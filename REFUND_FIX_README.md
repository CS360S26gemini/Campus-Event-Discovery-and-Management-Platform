# Refund System Fix — Campus Event Discovery Platform
**Author: Yahya**
**Date: April 29, 2026**

---

## Overview

This document details the investigation, root cause analysis, and complete fix for the in-app credit refund system in the Campus Event Discovery and Management Platform. The issue was that ticket cancellations were visually succeeding (RSVP status updated to `cancelled`) but in-app credit balances were never being updated in Firestore. A 3-day purchase window refund policy was also designed and implemented on top of the fix.

---

## The Problem

### Symptom
When a user purchased a ticket using in-app credits and then cancelled that ticket:
- The RSVP document in Firestore would correctly update its `status` field to `"cancelled"` ✅
- The user's `creditBalance` field in Firestore was **never updated** ❌
- No `credit_transactions` refund document was ever created ❌
- The cancel button on the ticket screen was sometimes **disabled** even for tickets that should have been cancellable ❌

### What Was Already Working (Before This Fix)
- Adding in-app credits manually via Firestore worked correctly
- Purchasing tickets using in-app credits deducted the balance correctly (`creditBalance` decremented atomically in a Firestore transaction)
- The RSVP document itself was being created with all the correct fields (`amount`, `ticketPrice`, `tierPrice`, `paymentMethod`, `rsvpAt`, etc.)

---

## Root Cause Analysis

### Tracing the `cancelRsvp` transaction in `EventRepository.java`

The `cancelRsvp` method inside `EventRepository.java` runs a Firestore transaction that:
1. Reads the RSVP, event, user, and tier documents
2. Computes `refundAmount` via `resolveCancellationAmount()`
3. Evaluates a `shouldRefund` boolean
4. If `shouldRefund == true`: creates a `credit_transactions` doc and updates `creditBalance`

The critical line was the `shouldRefund` evaluation:

```java
// BROKEN (original code)
shouldRefund = hadActiveRsvp
        && refundAmount > 0.0
        && isRefundEligible(
                eventSnap != null ? eventSnap.getTimestamp("date") : null,
                cancelledAt,
                false
        );
```

### What `isRefundEligible` Actually Does

```java
public static boolean isRefundEligible(Timestamp eventDate, Timestamp cancellationTime, boolean organizerInitiated) {
    // ...
    long remaining = eventDate.toDate().getTime() - cancellationTime.toDate().getTime();
    return remaining >= REFUND_WINDOW_MILLIS; // REFUND_WINDOW_MILLIS = 3 days
}
```

This method checks whether the **event date** is at least 3 days in the **future** from the time of cancellation. This was a pre-event cancellation window check — not a purchase window check.

### Why Credits Never Updated

During development and testing, events were created with near-future dates (same day, next day, etc.). For any event happening within 3 days of the cancellation time:
- `isRefundEligible()` returned `false`
- `shouldRefund` was `false`
- The entire credit block was skipped
- The RSVP status was still set to `"cancelled"` (that part ran unconditionally)
- Result: cancel "worked" visually, but no money came back

### The Secondary Bug — UI Button Also Blocked

In `TicketActivity.java`, the cancel button's enabled state was also gated behind the same check:

```java
// BROKEN (original code)
refundEligible = !checkedIn
        && !cancelled
        && EventRepository.isRefundEligible(eventTimestamp, Timestamp.now(), false);
```

So for near-future test events, the cancel button was entirely greyed out — users couldn't even attempt a cancellation.

---

## The Fix

### Design Decision — Switch to Purchase-Window Policy

Instead of checking "is the event more than 3 days away", the policy was redesigned to check "was this ticket purchased within the last 3 days". This is a much more user-friendly and standard refund policy (similar to how most ticketing platforms work), and it also correctly handled the test scenario.

**Old policy:** refund only if `eventDate - now >= 3 days`
**New policy:** refund only if `now - rsvpAt <= 3 days`

The constant `REFUND_WINDOW_MILLIS = 3 * 24 * 60 * 60 * 1000` was already defined and was reused — only the logic changed.

---

### File 1: `EventRepository.java`

**Change 1 — Fixed `shouldRefund` in `cancelRsvp()`:**

```java
// FIXED
shouldRefund = hadActiveRsvp
        && !wasCheckedIn
        && refundAmount > 0.0
        && isPurchasedWithin3Days(
                userRsvpSnap.getTimestamp("rsvpAt"),
                cancelledAt);
```

The `!wasCheckedIn` guard replaces the old time check — if a user scanned in at the event, no refund is issued. The `isPurchasedWithin3Days` call enforces the 3-day purchase window on the backend side.

**Change 2 — New helper method `isPurchasedWithin3Days()`:**

```java
public static boolean isPurchasedWithin3Days(Timestamp purchasedAt, Timestamp now) {
    if (purchasedAt == null || now == null) {
        // Missing timestamp — allow refund rather than silently blocking valid requests
        return true;
    }
    long elapsed = now.toDate().getTime() - purchasedAt.toDate().getTime();
    return elapsed >= 0 && elapsed <= REFUND_WINDOW_MILLIS;
}
```

The null-safe fallback ensures legacy RSVP documents (created before `rsvpAt` was stored) are not silently denied refunds.

---

### File 2: `TicketActivity.java`

**Rewrote `updateRefundUi()` to use the purchase-window policy:**

```java
// FIXED
refundEligible = !checkedIn && !cancelled && (!isPaid || purchasedWithin3Days);
```

`purchasedWithin3Days` is computed from the `rsvpAt` Firestore field read directly off the document snapshot in `loadRefundState()`:

```java
Timestamp rsvpAt = documentSnapshot.getTimestamp("rsvpAt");
if (rsvpAt != null) {
    long elapsed = System.currentTimeMillis() - rsvpAt.toDate().getTime();
    purchasedWithin3Days = elapsed >= 0 && elapsed <= REFUND_WINDOW_MILLIS;
} else {
    purchasedWithin3Days = true; // legacy ticket — allow refund
}
```

Added separate UI states for:
- Paid ticket, inside 3-day window → "Refund available" + button enabled
- Paid ticket, outside 3-day window → "Refund window closed" + button disabled
- Free ticket → "Free ticket — cancelling removes registration" + button enabled
- Checked-in ticket → "Checked-in tickets cannot be cancelled" + button disabled

Also added separate dialog messages and toast messages for paid vs. free ticket cancellations.

---

### File 3: `strings.xml`

Updated and added the following string resources:

| Key | Purpose |
|-----|---------|
| `refund_available_status` | Updated to mention the 3-day window |
| `refund_window_expired_status` | **New** — shown for expired paid tickets |
| `free_ticket_cancel_confirm_message` | **New** — dialog for free ticket cancel |
| `free_ticket_cancel_success` | **New** — toast after free ticket cancel |

---

## Test Results

### Test 1 — Basic paid-ticket refund (within 3-day window) ✅ PASSED

**Setup:**
- User `creditBalance` set to `2000` via Firestore
- Paid event with `amount: 1000`
- Ticket purchased using In-App Credit payment method

**Steps:**
1. Opened the ticket QR screen after purchase
2. Status showed "Refund available — cancel within 3 days of purchase for a full credit refund."
3. Cancel button was enabled
4. Tapped Cancel Ticket → Confirmed

**Verified in Firestore:**
- `users/{uid}/rsvps/{eventId}` → `status: "cancelled"`, `paymentStatus: "refunded"`, `refundAmount: 1000` ✅
- `credit_transactions` → new document with `type: "refund"`, `amount: 1000` ✅
- `users/{uid}` → `creditBalance` updated back to `2000` ✅
- In-app credit balance reflected correctly inside the app ✅

---

### Test 2 — Refund window expired (simulated old purchase) ✅ PASSED

**Setup:**
- Same ticket from Test 1 flow, but `rsvpAt` field in Firestore was manually edited to a date 4 days in the past (April 25, 2026)

**Steps:**
1. Force-closed and reopened the app
2. Navigated to the ticket screen

**Results:**
- Status showed "Refund window closed — tickets can only be refunded within 3 days of purchase." ✅
- Cancel button was greyed out and non-interactive ✅
- Credit balance was completely unchanged ✅
- No transaction documents were created ✅

---

## Files Modified

| File | Type of Change |
|------|----------------|
| `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java` | Backend refund logic — `shouldRefund` fix + new `isPurchasedWithin3Days()` helper |
| `app/src/main/java/com/example/CampusEventDiscovery/ui/event/TicketActivity.java` | UI refund state — purchase-window check, 4 UI states, separate dialogs/toasts |
| `app/src/main/res/values/strings.xml` | String resources — updated `refund_available_status`, added 3 new strings |

---

## How to Test Going Forward

To simulate an expired refund window without waiting 3 real days:
1. Purchase a ticket
2. Go to Firestore → `users/{uid}/rsvps/{eventId}` → edit the `rsvpAt` field to 4+ days ago
3. Force-close and reopen the app → ticket screen should show the expired state

To add the event-date-based 3-day policy on top later:
- In `EventRepository.cancelRsvp()`, add `&& isRefundEligible(...)` to the `shouldRefund` line
- In `TicketActivity.updateRefundUi()`, add the corresponding UI check to `refundEligible`
