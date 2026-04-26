# Event Reminders & In-App Credit Refund System
**Branch:** `reminders-and-refunds-in-app-credits`  
**Developer:** Yahya (27100218)  
**Sprint:** Sprint 2  
**Status:** ✅ Implemented, tested on physical device, verified on Firestore console

---

## 1. Overview

This branch implements two complete features:

1. **Event Reminders** — automated push notifications sent to attendees 3 days before their registered event
2. **In-App Credit Refund System** — attendees receive in-app credits when they cancel an eligible RSVP or when an organizer cancels an event; credits can be used at checkout instead of a payment method

---

## 2. Event Reminders

### What was implemented

| File | Change |
|------|--------|
| `functions/index.js` | `sendEventReminders` Cloud Function — queries active events within 3 days, reads attendees, fetches FCM tokens, sends data payload |
| `firestore.indexes.json` | Composite index on `events` collection: `status ASC + date ASC` |
| `firebase.json` | Firebase deployment configuration |
| `CampusEventDiscoveryApp.java` | `REMINDER_CHANNEL_ID = "event_reminders"` registered with `IMPORTANCE_DEFAULT` |
| `MyFirebaseMessagingService.java` | `EVENT_REMINDER` branch in `onMessageReceived` — builds and shows notification |
| `MainActivity.java` | Reads `destinationTab` extra on `onCreate` and `onNewIntent`, routes to calendar tab if value equals `calendar` |

### FCM Payload

| Key | Value |
|-----|-------|
| `type` | `EVENT_REMINDER` |
| `eventId` | Firestore event document ID |
| `title` | Notification title |
| `body` | Notification body |
| `destinationTab` | `calendar` |

### Notification text logic
- If event is **today**: "Your event {title} is today at {time}!"
- If event is **N days away**: "Your event {title} is in N day(s)!"

### Scheduler — deployment note
The original spec required Firebase Cloud Scheduler (daily 8:00 AM PKT / 03:00 UTC). This was blocked because the Firebase project `campuseventdiscovery-87f4f` is on the **Spark plan** which does not allow Cloud Functions deployment. 

**Workaround:** GitHub Actions cron job configured to trigger the reminder sender daily at 03:00 UTC. The reminder logic itself is identical to the spec — only the runtime trigger differs.

### Tests written
- `ConstantsTest.java` — reminder channel ID and constants
- `MyFirebaseMessagingServiceTest.java` — reminder notification routing

### Verified on device
- Notification displayed correctly on physical Android device
- Tap on notification opened MainActivity and routed to calendar tab
- `destinationTab = calendar` confirmed via intent inspection

---

## 3. In-App Credit Refund System

### What was implemented

| File | Change |
|------|--------|
| `User.java` | Added `creditBalance` (double, default 0.0) with getter/setter |
| `Constants.java` | Added `COLLECTION_CREDIT_TRANSACTIONS`, `PAYMENT_REFUNDED`, `PAYMENT_METHOD_IN_APP_CREDIT`, `CREDIT_TRANSACTION_REFUND`, `CREDIT_TRANSACTION_USED`, payment method string constants |
| `EventRepository.java` | `isRefundEligible()`, `normalizeRefundAmount()`, `resolveCancellationAmount()`, extended `cancelRsvp()`, extended `deleteEvent()`, new `rsvpEventWithCredit()` |
| `CheckoutActivity.java` | Loads `creditBalance` from user profile, shows balance label, added `rbInAppCredit` radio button, routes to `processCreditPayment()` |
| `activity_checkout.xml` | Added `tvCreditBalance` TextView and `rbInAppCredit` RadioButton |
| `activity_account_settings.xml` | Added In-App Credits row showing current balance |
| `AccountSettingsActivity.java` | Binds `tvUserCreditBalance` and populates it from `getUserData()` |
| `strings.xml` | Updated refund policy text, added credit-related strings |

### Refund Policy

| Scenario | Refund |
|----------|--------|
| Attendee cancels > 3 days before event | 100% credit issued |
| Attendee cancels ≤ 3 days before event | No credit issued |
| Organizer deletes event (any time) | 100% credit issued to all attendees |
| Free event cancelled | No credit (nothing was paid) |

### Credit rules
- 1 PKR = 1 credit
- Credits can only be gained via refund — there is no way to purchase credits
- Credits are deducted atomically at checkout via Firestore transaction
- No Stripe or bank refunds are issued — in-app credit only

### Firestore schema additions

**`credit_transactions/{docId}`**
| Field | Type | Notes |
|-------|------|-------|
| `userId` | String | |
| `type` | String | `REFUND` or `USED` |
| `amount` | Number | PKR amount |
| `eventId` | String | |
| `eventTitle` | String | |
| `originalAmount` | Number | |
| `createdAt` | Timestamp | |
| `reason` | String | `ORGANIZER_CANCELLED` or `ATTENDEE_CANCELLED` (REFUND only) |
| `paymentTransactionId` | String | (USED only) |

**`users/{userId}`** — new field:
| Field | Type | Default |
|-------|------|---------|
| `creditBalance` | Number | 0 |

### Key repository methods

**`cancelRsvp(userId, eventId, organizerInitiated, cb)`**
- Runs Firestore transaction
- Checks refund eligibility via `isRefundEligible()`
- If eligible: writes `credit_transactions` doc, increments `creditBalance` via `FieldValue.increment`
- Marks RSVP `paymentStatus = REFUNDED`
- Decrements `rsvpCount` on event

**`deleteEvent(eventId, deletedByUserId, cb)`**
- Reads all attendees from `events/{eventId}/attendees`
- For each attendee: cancels RSVP, writes credit transaction, increments their `creditBalance`
- Marks event `status = deleted`, resets `rsvpCount = 0`
- All writes in a single batch commit

**`rsvpEventWithCredit(userId, event, fullName, amount, cb)`**
- Runs Firestore transaction
- Reads `creditBalance` from user doc
- Throws `FAILED_PRECONDITION` if balance insufficient
- Checks capacity, blacklist, duplicate registration
- Deducts `creditBalance` via `FieldValue.increment(-amount)`
- Writes RSVP, attendee doc, payment doc, credit transaction doc atomically

### Tests written
- `RefundPolicyTest.java`
  - Attendee cancel > 3 days → eligible
  - Attendee cancel ≤ 3 days → not eligible
  - Organizer cancel → always eligible
  - `normalizeRefundAmount` never returns negative
- `UserCreditBalanceTest.java`
  - Default constructor → `creditBalance = 0`
  - Setter/getter round trip

### Verified on device
- `creditBalance` set to 1000 manually in Firestore console
- Paid for a 500 PKR ticket using In-App Credit → `creditBalance` dropped to 9500 ✅
- Credit balance displayed correctly on Account Settings screen ✅
- Insufficient credit toast shown when balance is zero ✅
- RSVP cancelled on event > 3 days away → credit returned to Firestore ✅

---

## 4. How to test

### Prerequisites
- Sign in with a real Firebase Auth account (not developer bypass)
- Go to Firestore console → `users/{yourUID}` → set `creditBalance = 1000` (type: number)

### Test credit payment
1. Open any paid event (price ≤ 1000 PKR)
2. Tap Get Tickets → Checkout
3. Select **In-App Credit** from payment options
4. Tap Pay
5. Verify ticket shown and `creditBalance` deducted in Firestore
6. Verify new doc in `credit_transactions` with `type: USED`

### Test attendee refund (eligible)
1. RSVP to a paid event dated 4+ days from now
2. Go to My Events → Cancel RSVP
3. Verify `creditBalance` increased in Firestore
4. Verify new doc in `credit_transactions` with `type: REFUND`

### Test attendee refund (not eligible)
1. RSVP to a paid event dated 1-2 days from now
2. Cancel RSVP
3. Verify `creditBalance` unchanged and no new `credit_transactions` doc

### Test organizer cancellation refund
1. Log in as organizer
2. Delete an event that has paid attendees
3. Verify each attendee's `creditBalance` increased in Firestore

### Test insufficient credits
1. Set `creditBalance = 0` in Firestore
2. Open a paid event → Checkout → select In-App Credit
3. Verify toast: "Insufficient credits to complete this purchase"

---

## 5. Known limitations

- Firebase Cloud Scheduler deployment blocked by Spark plan — replaced with GitHub Actions cron
- Some backend reminder test cases (cancelled RSVP exclusion, 3-day window exclusion) are manually verified rather than fully automated
- Credit balance on checkout screen is loaded once on activity start — does not live-update if balance changes in another session simultaneously

---

## 6. Commit reference

```
2ffbcde — feature added: inapp credit refund system, credit payment at checkout, 
credit balance on profile verified on firestore console, backend working, 
updated UI to show available credits on payment page
```