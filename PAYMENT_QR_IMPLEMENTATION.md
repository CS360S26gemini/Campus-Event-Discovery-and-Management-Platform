# Payment Gateway & QR Code Ticketing — Implementation Notes

**Branch:** `saad`  
**Feature:** Demo Payment Gateway + QR Code Ticketing System  
**Author:** Saad Jamshaid Khan  
**Sprint:** Part 3 (Half-Way Checkpoint)

---

## Table of Contents

1. [Overview](#overview)
2. [Complete User Flow](#complete-user-flow)
3. [Architecture](#architecture)
4. [New Files Created](#new-files-created)
5. [Modified Files](#modified-files)
6. [Class Reference](#class-reference)
7. [Firestore Data Model](#firestore-data-model)
8. [Payment Method Logic](#payment-method-logic)
9. [QR Code System](#qr-code-system)
10. [One-Time Use QR Enforcement](#one-time-use-qr-enforcement)
11. [Gradle Dependencies Added](#gradle-dependencies-added)
12. [String Resources Added](#string-resources-added)
13. [Known Limitations](#known-limitations)

---

## Overview

This feature implements a complete end-to-end ticketing flow for the Campus Event Discovery platform:

- Organizers set a ticket price when creating an event
- Attendees browse events and see the price on the event detail screen
- Attendees go through a checkout form with demo payment methods (JazzCash, Credit Card, Debit Card, Apple Pay)
- On successful payment, a unique QR code is generated and stored in Firestore
- The QR code is displayed as the attendee's ticket
- Registered events appear in the attendee's "My Events" section — tapping them reopens the QR ticket
- Organizers open a scanner screen, scan the attendee's QR code, verify the ticket against Firestore, and mark the attendee as attended
- Once scanned and marked, the QR code is expired and cannot be reused

---

## Complete User Flow

### Organizer Side

```
CreateEventActivity
    └── Organizer fills in event details including ticket price (etTicketPrice)
    └── On submit → EventProposal saved to Firestore with ticketPrice field
            ↓
AdminHomeActivity / EventApprovalActivity
    └── Admin sees proposal including ticket price
    └── Admin approves → Event document created in events/ collection
            ↓
ScannerActivity (at event day)
    └── Organizer opens scanner from their home screen
    └── Camera opens via ZXing IntentIntegrator
    └── Scans attendee QR code
    └── JSON payload decoded → RSVP looked up in Firestore
    └── Attendee name, event, payment status displayed
    └── "Mark as Attended" button → sets checkedIn=true, qrExpired=true
```

### Attendee Side

```
HomeFragment / SearchFragment
    └── Attendee browses events
            ↓
EventDetailActivity
    └── Event details shown including price (tvPrice)
    └── Price shown as "Free" or "PKR X.XX"
    └── "Tickets" button (btnTickets) clicked
            ↓
CheckoutActivity
    └── Form shown: Full Name, Last Name, Payment Method, Card Number (conditional)
    └── Duplicate RSVP check runs against Firestore before any payment
    └── If already registered → TicketActivity opened with existing QR
    └── If not registered → MockPaymentService.processPayment() called
    └── Payment saved to payments/ collection
    └── RSVP created at users/{userId}/rsvps/{eventId}
    └── attendeeCount incremented on event document
            ↓
TicketActivity
    └── QR code generated from qrPayload string
    └── Event name, date, transaction ID displayed
    └── Attendee can show this screen to organizer
            ↓
MyEventsFragment (later visits)
    └── Section 1 shows RSVPd events
    └── Tapping a registered event → Firestore check for qrPayload
    └── If qrPayload exists → TicketActivity opened directly
    └── If no qrPayload → EventDetailActivity opened
```

---

## Architecture

```
model/
├── Payment.java          ← NEW: payment record model
├── Rsvp.java             ← MODIFIED: added qrExpired, transactionId, qrPayload, paymentStatus
└── Event.java            ← unchanged (ticketPrice field already existed)

repository/
└── PaymentRepository.java  ← NEW: Firestore CRUD for payments/

util/
├── MockPaymentService.java  ← NEW: simulates payment, returns Payment object
└── QRCodeHelper.java        ← NEW: generates QR Bitmap from string payload

ui/event/
├── CheckoutActivity.java    ← REPLACED: full overhaul of payment flow
├── EventDetailActivity.java ← MODIFIED: price display + launches CheckoutActivity
├── TicketActivity.java      ← unchanged (already correct)
└── PaymentActivity.java     ← DELETED: was a duplicate of CheckoutActivity

ui/organizer/
└── ScannerActivity.java     ← MODIFIED: added qrExpired enforcement

ui/myevents/
└── MyEventsFragment.java    ← MODIFIED: RSVP tap opens TicketActivity

res/layout/
└── activity_checkout.xml    ← REPLACED: new layout with 4 payment methods

res/values/
└── strings.xml              ← MODIFIED: new strings added, duplicates removed

AndroidManifest.xml          ← MODIFIED: PaymentActivity entry removed

gradle/
├── libs.versions.toml       ← MODIFIED: ZXing entries added
└── app/build.gradle.kts     ← MODIFIED: ZXing dependencies added
```

---

## New Files Created

### `Payment.java`
**Package:** `com.example.CampusEventDiscovery.model`

Model class representing a single payment transaction stored in Firestore.

| Field | Type | Description |
|-------|------|-------------|
| `paymentId` | `String` | Firestore document ID (`@Exclude` — not stored as field) |
| `userId` | `String` | UID of the user who paid |
| `eventId` | `String` | ID of the event being paid for |
| `amount` | `double` | Amount paid (matches event's `ticketPrice`) |
| `status` | `String` | `"SUCCESS"` or `"FAILED"` |
| `transactionId` | `String` | UUID generated at payment time |
| `timestamp` | `long` | `System.currentTimeMillis()` at payment time |

**Firestore path:** `payments/{paymentId}`

---

### `PaymentRepository.java`
**Package:** `com.example.CampusEventDiscovery.repository`

Handles all Firestore read/write operations for the `payments/` collection.

#### Methods

```java
void savePayment(Payment payment, FirestoreCallback callback)
```
- Uses `db.collection("payments").add(payment)` to create a new document
- On success: sets `payment.paymentId` from the generated document ID, calls `callback.onSuccess(payment)`
- On failure: calls `callback.onFailure(e)`

```java
void getPaymentByTransactionId(String txnId, FirestoreCallback callback)
```
- Queries `payments/` where `transactionId == txnId`, limit 1
- Returns the `Payment` object or `null` if not found

---

### `MockPaymentService.java`
**Package:** `com.example.CampusEventDiscovery.util`

Simulates a payment gateway for demo purposes. No real money moves.

```java
public static Payment processPayment(String userId, String eventId, double amount)
```
- Generates a random `transactionId` using `UUID.randomUUID().toString()`
- Returns a `Payment` object with `status = "SUCCESS"` and `timestamp = System.currentTimeMillis()`
- Always succeeds — this is intentional for demo use

---

### `QRCodeHelper.java`
**Package:** `com.example.CampusEventDiscovery.util`

Generates QR code bitmaps from string content using the ZXing library.

```java
public static Bitmap generateQRCode(String content, int widthPx, int heightPx)
```
- Uses `com.google.zxing.MultiFormatWriter` with `BarcodeFormat.QR_CODE`
- Encodes the content string into a `BitMatrix`
- Converts to an Android `Bitmap` using `BarcodeEncoder`
- Returns `null` if encoding fails (handled gracefully in `TicketActivity`)

**Called from:** `TicketActivity.setupUI()` with `widthPx = 800, heightPx = 800`

---

## Modified Files

### `Rsvp.java`
**Package:** `com.example.CampusEventDiscovery.model`

**Fields added:**

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `paymentStatus` | `String` | — | `"SUCCESS"` after confirmed payment |
| `transactionId` | `String` | — | Links to the `Payment` document |
| `qrPayload` | `String` | — | JSON string encoded in the QR code |
| `checkedIn` | `boolean` | `false` | Set to `true` by organizer at event |
| `qrExpired` | `boolean` | `false` | Set to `true` after first successful scan |

**Full constructor signature (updated):**
```java
public Rsvp(String rsvpId, String userId, String eventId, String title,
            Timestamp date, String status, String paymentStatus,
            String transactionId, String qrPayload,
            boolean checkedIn, boolean qrExpired, Timestamp rsvpAt)
```

**Firestore path:** `users/{userId}/rsvps/{eventId}`

Note: `eventId` is used as the document ID deliberately — this enforces one RSVP per user per event at the database level.

---

### `CheckoutActivity.java` (full replacement)
**Package:** `com.example.CampusEventDiscovery.ui.event`

The original `CheckoutActivity` and the now-deleted `PaymentActivity` have been merged into a single, complete checkout flow.

#### Intent Extras Received

| Key | Type | Source |
|-----|------|--------|
| `"eventId"` | `String` | `EventDetailActivity` |
| `"eventTitle"` | `String` | `EventDetailActivity` |
| `"eventDateMillis"` | `long` | `EventDetailActivity` (`-1L` if unknown) |
| `"eventVenue"` | `String` | `EventDetailActivity` |
| `"totalPrice"` | `double` | `EventDetailActivity` (`event.getTicketPrice()`) |

#### Key Fields

```java
private String eventId;
private String eventTitle;
private long eventDateMillis;
private String eventVenue;
private double totalPrice;
private String effectiveUserId;       // resolved from FirebaseAuth or DevSessionManager

private MaterialToolbar toolbarCheckout;
private TextView tvCheckoutEventTitle;
private TextView tvCheckoutSubtitle;
private EditText etFullName;          // R.id.etFirstName
private EditText etLastName;          // R.id.etLastName
private RadioGroup radioGroupPayment; // R.id.radioGroupPayment
private EditText etCardNumber;        // R.id.etCardNumber — shown/hidden based on selection
private TextView tvCheckoutTotal;
private ProgressBar progressBarCheckout;
private MaterialButton btnPay;
```

#### Payment Method Radio Button IDs

| ID | Label | Card Number Required |
|----|-------|---------------------|
| `R.id.rbJazzCash` | JazzCash | No |
| `R.id.rbCreditCard` | Credit Card | Yes (16 digits) |
| `R.id.rbDebitCard` | Debit Card | Yes (16 digits) |
| `R.id.rbApplePay` | Apple Pay | No |

#### Method Flow

```
btnPay.onClick()
    └── validateForm()
            ├── checks fullName and lastName not empty
            ├── checks a payment method is selected
            └── if Credit Card or Debit Card: checks cardNumber.length() == 16
    └── showLoading(true)
    └── Firestore: users/{userId}/rsvps/{eventId}.get()
            ├── EXISTS with non-empty qrPayload
            │       └── Toast "already registered"
            │       └── navigateToTicket(existingRsvp)   ← no new payment
            └── NOT EXISTS
                    └── processPayment()
                            └── MockPaymentService.processPayment(userId, eventId, totalPrice)
                            └── paymentRepository.savePayment(payment, callback)
                                    └── createRsvpWithQr(payment)
                                            └── builds qrPayload JSON
                                            └── builds Rsvp object
                                            └── Firestore: users/{userId}/rsvps/{eventId}.set(rsvp)
                                            └── eventRepository.incrementAttendeeCount(eventId, null)
                                            └── navigateToTicket(rsvp)
```

#### QR Payload JSON structure

```json
{
  "userId": "firebase_uid_string",
  "eventId": "firestore_event_doc_id",
  "transactionId": "uuid-v4-string",
  "timestamp": 1712345678901
}
```

---

### `EventDetailActivity.java`
**Package:** `com.example.CampusEventDiscovery.ui.event`

**Changes:**

1. **Price display** — in `loadEventDetails()`, `tvPrice` is now set as:
   ```java
   if (event.getTicketPrice() == 0.0) {
       tvPrice.setText(getString(R.string.price_free));   // "Free"
   } else {
       tvPrice.setText(String.format(Locale.getDefault(), "PKR %.2f", event.getTicketPrice()));
   }
   ```

2. **Tickets button** — `btnTickets` now launches `CheckoutActivity` instead of the deleted `PaymentActivity`:
   ```java
   Intent intent = new Intent(EventDetailActivity.this, CheckoutActivity.class);
   intent.putExtra("eventId", currentEvent.getEventId());
   intent.putExtra("eventTitle", safeText(currentEvent.getTitle(), getString(R.string.app_name)));
   intent.putExtra("totalPrice", currentEvent.getTicketPrice());
   intent.putExtra("eventDateMillis", currentEvent.getDate() != null
           ? currentEvent.getDate().toDate().getTime() : -1L);
   intent.putExtra("eventVenue", safeText(currentEvent.getLocation(), ""));
   ```

3. **`btnViewTicket` null safety** — bound with an `instanceof` check since the view ID may not exist in all layout variants.

---

### `ScannerActivity.java`
**Package:** `com.example.CampusEventDiscovery.ui.organizer`

**Changes:**

1. **`markAsAttended()`** — now updates three fields in a single Firestore call:
   ```java
   db.collection("users").document(currentRsvp.getUserId())
       .collection("rsvps").document(currentRsvp.getEventId())
       .update(
           "checkedIn", true,
           "qrExpired", true,
           "checkedInAt", Timestamp.now()
       )
   ```
   After success, also mirrors `checkedIn = true` on `events/{eventId}/attendees/{userId}` for consistency.

2. **`displayResult()`** — now checks `qrExpired` before showing the mark-as-attended button:
   ```java
   if (currentRsvp.isQrExpired()) {
       tvCheckInStatus.setText(getString(R.string.scanner_ticket_already_used));
       tvCheckInStatus.setTextColor(Color.RED);
       btnMarkAttended.setVisibility(View.GONE);
   } else if (currentRsvp.isCheckedIn()) {
       tvCheckInStatus.setText(getString(R.string.scanner_checked_in_yes));
       btnMarkAttended.setVisibility(View.GONE);
   } else {
       tvCheckInStatus.setText(getString(R.string.scanner_checked_in_no));
       btnMarkAttended.setVisibility(View.VISIBLE);
   }
   ```

3. **Camera permission** — handled via `ActivityResultLauncher<String>` with `RequestPermission` contract. Camera opens only after permission is granted.

4. **All user-facing strings** moved to `strings.xml` — no hardcoded text remains.

---

### `MyEventsFragment.java`
**Package:** `com.example.CampusEventDiscovery.ui.myevents`

**Changes:**

1. **Section 1 adapter** (RSVPs / Registered Events) replaced `createAdapter()` call with an inline adapter that intercepts `onItemClick` for attendees:

   ```java
   adapter1 = new EventAdapter(list1, ids1, null,
       new EventAdapter.OnEventClickListener() {
           @Override
           public void onItemClick(Event event) {
               if (UserRoles.isAttendee(userRole)) {
                   openTicketOrDetail(event);   // ← new method
               } else {
                   openEventDetail(event);
               }
           }
           // ...
       }
   );
   ```

2. **`openTicketOrDetail(Event event)`** — new private method:
   - Fetches `users/{userId}/rsvps/{eventId}` from Firestore
   - If document exists and `qrPayload` is non-empty → launches `TicketActivity` with all required extras
   - Otherwise → falls back to `openEventDetail(event)`

   ```java
   Intent intent = new Intent(requireContext(), TicketActivity.class);
   intent.putExtra("rsvpId", event.getEventId());
   intent.putExtra("eventName", rsvp.getTitle());
   intent.putExtra("eventDate", formattedDate);
   intent.putExtra("transactionId", rsvp.getTransactionId());
   intent.putExtra("qrPayload", rsvp.getQrPayload());
   startActivity(intent);
   ```

---

### `activity_checkout.xml` (full replacement)
**Path:** `app/src/main/res/layout/`

New layout built with `CoordinatorLayout` + `NestedScrollView`. Key view IDs:

| View ID | Type | Purpose |
|---------|------|---------|
| `toolbarCheckout` | `MaterialToolbar` | Back navigation |
| `tvCheckoutEventTitle` | `TextView` | Event name |
| `tvCheckoutSubtitle` | `TextView` | Subtitle text |
| `etFirstName` | `TextInputEditText` | Full name input |
| `etLastName` | `TextInputEditText` | Last name input |
| `radioGroupPayment` | `RadioGroup` | Payment method selection |
| `rbJazzCash` | `RadioButton` | JazzCash option |
| `rbCreditCard` | `RadioButton` | Credit Card option |
| `rbDebitCard` | `RadioButton` | Debit Card option |
| `rbApplePay` | `RadioButton` | Apple Pay option |
| `etCardNumber` | `TextInputEditText` | Card number (16 digits, hidden by default) |
| `tvCheckoutTotal` | `TextView` | Displays total: "Free" or "PKR X.XX" |
| `progressBarCheckout` | `ProgressBar` | Shown during payment processing |
| `btnPay` | `MaterialButton` | "Pay Now" or "Register Free" |

---

## Firestore Data Model

### `payments/{paymentId}`
```
{
  userId:        string,
  eventId:       string,
  amount:        number,
  status:        "SUCCESS",
  transactionId: string (UUID),
  timestamp:     number (epoch millis)
}
```

### `users/{userId}/rsvps/{eventId}`
```
{
  userId:        string,
  eventId:       string,
  title:         string,
  date:          Timestamp,
  status:        "CONFIRMED",
  paymentStatus: "SUCCESS",
  transactionId: string (UUID),
  qrPayload:     string (JSON),
  checkedIn:     boolean,
  qrExpired:     boolean,
  rsvpAt:        Timestamp
}
```

### `events/{eventId}` (relevant fields)
```
{
  ticketPrice:   number,
  rsvpCount:     number,   ← incremented on confirmed RSVP
  ...
}
```

---

## Payment Method Logic

| Method | Card Number Field | Validation |
|--------|------------------|------------|
| JazzCash | Hidden | None |
| Credit Card | Shown | Must be exactly 16 digits |
| Debit Card | Shown | Must be exactly 16 digits |
| Apple Pay | Hidden | None |

The card number field (`etCardNumber`) is toggled by `radioGroupPayment.setOnCheckedChangeListener()`. When JazzCash or Apple Pay is selected, the field is hidden and its content is cleared. No actual card processing occurs — this is a demo UI only.

---

## QR Code System

### Payload Format
The QR code encodes a JSON string with exactly 4 fields:
```json
{
  "userId": "abc123",
  "eventId": "xyz789",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1712345678901
}
```

### Generation
- Called in `TicketActivity.setupUI()`
- `QRCodeHelper.generateQRCode(qrPayload, 800, 800)` returns a `Bitmap`
- Bitmap set on `ivQrCode` (`ImageView`, 400x400dp in layout)

### Verification (Scanner side)
1. ZXing scans the QR and returns the raw string
2. `processScanResult()` parses the JSON to extract `transactionId`, `userId`, `eventId`
3. `lookupRsvp()` fetches `users/{userId}/rsvps/{eventId}` from Firestore
4. Verifies that `transactionId` in the QR matches `rsvp.getTransactionId()` in Firestore
5. Fetches user's display name from `users/{userId}`
6. Displays result card

---

## One-Time Use QR Enforcement

The `qrExpired` boolean field on the `Rsvp` model is the mechanism that prevents QR reuse.

**Set to `true`:** When organizer taps "Mark as Attended" in `ScannerActivity.markAsAttended()` — updates `checkedIn`, `qrExpired`, and `checkedInAt` in one atomic Firestore call.

**Checked in:** `ScannerActivity.displayResult()` — if `qrExpired == true`, shows "TICKET ALREADY USED" in red and hides the mark-as-attended button, preventing double entry.

**Not checked on attendee side:** The `TicketActivity` still displays the QR even after it's expired. This is intentional — the attendee needs to see their ticket history. Only the organizer scanner enforces the expiry.

---

## Gradle Dependencies Added

### `app/build.gradle.kts`
```kotlin
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
implementation("com.google.zxing:core:3.5.3")
```

### `gradle/libs.versions.toml`
```toml
[libraries]
zxing = { group = "com.journeyapps", name = "zxing-android-embedded", version = "4.3.0" }
zxing-core = { group = "com.google.zxing", name = "core", version = "3.5.3" }
```

`zxing-android-embedded` provides the `IntentIntegrator` scanner and `BarcodeEncoder` for QR generation.  
`zxing-core` provides `MultiFormatWriter` and `BarcodeFormat`.

---

## String Resources Added

All new strings added to `res/values/strings.xml`:

```xml
<!-- Checkout -->
<string name="checkout_section_personal">Personal details</string>
<string name="checkout_section_payment">Payment method</string>
<string name="checkout_hint_full_name">Full name</string>
<string name="checkout_hint_last_name">Last name</string>
<string name="checkout_hint_card_number">Card / account number (16 digits)</string>
<string name="checkout_order_total_label">Order total</string>
<string name="checkout_total_free">Free</string>
<string name="checkout_total_pkr">PKR %1$.2f</string>
<string name="payment_method_jazzcash">JazzCash</string>
<string name="payment_method_credit_card">Credit Card</string>
<string name="payment_method_debit_card">Debit Card</string>
<string name="payment_method_apple_pay">Apple Pay</string>
<string name="invalid_card_number">Card number must be exactly 16 digits</string>
<string name="already_registered_for_event">You are already registered for this event. Opening your ticket.</string>
<string name="checkout_rsvp_check_failed">Could not verify registration status. Please try again.</string>
<string name="payment_failed_message">Payment failed: %1$s</string>
<string name="rsvp_failed_message">Registration failed: %1$s</string>
<string name="price_free">Free</string>

<!-- Scanner -->
<string name="scanner_camera_permission_required">Camera permission is required to scan QR codes</string>
<string name="scanner_prompt">Scan attendee ticket QR code</string>
<string name="scanner_cancelled">Scan cancelled</string>
<string name="scanner_invalid_qr">Invalid QR code format</string>
<string name="scanner_rsvp_mismatch">Ticket data does not match our records</string>
<string name="scanner_rsvp_not_found">No registration found for this ticket</string>
<string name="scanner_error_fetching">Error fetching registration: %1$s</string>
<string name="scanner_unknown_user">Unknown attendee</string>
<string name="scanner_label_attendee">Attendee: %1$s</string>
<string name="scanner_label_event">Event: %1$s</string>
<string name="scanner_label_payment">Payment: %1$s</string>
<string name="scanner_checked_in_yes">Checked in: YES</string>
<string name="scanner_checked_in_no">Checked in: NO</string>
<string name="scanner_ticket_already_used">TICKET ALREADY USED</string>
<string name="scanner_marked_attended">Attendee marked as attended!</string>
<string name="scanner_mark_failed">Failed to update check-in status</string>
```

---

## Known Limitations

| Limitation | Reason | Acceptable for Demo |
|------------|--------|---------------------|
| Payment always succeeds | `MockPaymentService` returns `SUCCESS` unconditionally | Yes — graders understand this is a simulation |
| No real card validation | Card number is checked for length only, never processed | Yes |
| QR screenshot bypass | A screenshot of the QR before expiry could be shown again | Yes — noted as known gap |
| Single device demo | Organizer scanner needs to be on a separate device from the attendee ticket | Plan accordingly for demo — show on two phones or explain the flow |
| No refund flow | `MockPaymentService` has a `simulateRefund()` stub but it is not wired to any UI | Out of scope for this sprint |
