# Campus Event Discovery & Management Platform

**Yahya's Branch — Team Gemini, CS360 Android Project (Level 2, Sprint 2)**
LUMS Computer Science, Spring 2026

---

## What I Built

This branch contains my complete contribution to the Campus Event Discovery app. My core ownership was the **payment system** — taking it from a non-functional stub to a real, working integration — plus the organiser payment management screen, real-time event listeners, QR check-in transaction logic, and the full unit test suite.

Here is everything I worked on, in detail:

---

## 1. Stripe Payment Integration (Credit Card & Debit Card)

### The Problem

The previous branch had a `MockPaymentService` that returned a fake transaction ID and never actually processed any money. For the sprint demo, we needed real Stripe payments to show up on the Stripe dashboard and update Firestore.

### What I Built

I replaced `MockPaymentService` with `StripePaymentService.java`, which makes a real HTTP POST to a Supabase Edge Function. The Edge Function creates a Stripe `PaymentIntent` server-side and returns the `pi_...` ID back to the app. The app then saves that ID as `paymentRef` on both the Payment document and the RSVP.

**Why Supabase and not calling Stripe directly from the app?**
Stripe requires the secret key to create a PaymentIntent. You can never put a secret key in an Android APK — it can be extracted by decompiling the app. Supabase Edge Functions run in the cloud (Deno/TypeScript runtime), so the secret key stays server-side and is never shipped to any device.

### Supabase Edge Function

The function lives at `supabase/functions/create-payment-intent/index.ts`. It:
1. Receives a JSON body with `amount` (in cents) and `currency`
2. Reads `STRIPE_SECRET_KEY` from Supabase Secrets (environment variable — never hard-coded)
3. Calls the Stripe API to create a PaymentIntent
4. Returns the `clientSecret` and `paymentIntentId` to the Android app

**The function is already deployed.** Its live URL is:
```
https://jhvujgiusemenimbzmil.supabase.co/functions/v1/create-payment-intent
```

No teammate needs to deploy it again. The app just calls this URL over HTTPS.

### Does anyone else need to run `npx supabase` commands?

**No.** The Edge Function is live in the cloud. When you clone the repo and run the app, it automatically calls the deployed function. The Stripe secret key is already set as a Supabase Secret on the server.

You only need the CLI commands if you are starting from a brand new Supabase project. Steps for that (one person only, one time):

```powershell
# Install and log in to Supabase CLI
npx supabase login

# Link to the Supabase project
npx supabase link --project-ref jhvujgiusemenimbzmil

# Set the Stripe secret key as a server-side secret
npx supabase secrets set STRIPE_SECRET_KEY=sk_test_XXXX...

# Deploy the Edge Function
npx supabase functions deploy create-payment-intent --no-verify-jwt
```

### The Secret Key Issue (GitHub Push Protection)

At one point the Stripe secret key was accidentally hard-coded in `index.ts` and GitHub blocked the push with a "secret detected" error. I fixed this by:

1. Replacing the hard-coded value with `Deno.env.get("STRIPE_SECRET_KEY") ?? ""`
2. Setting the actual key in Supabase Secrets via the CLI
3. Amending the commit and force-pushing to remove the key from Git history:

```bash
git add supabase/functions/create-payment-intent/index.ts
git commit --amend --no-edit
git push origin HEAD:yahya --force
```

The key is now completely absent from the codebase and Git history.

### Stripe Publishable Key (in the app)

The **publishable** key is safe to include in the app — it identifies your Stripe account but cannot be used to charge anyone. It is in `CampusEventDiscoveryApp.java`:

```java
PaymentConfiguration.init(getApplicationContext(),
    "pk_test_51TMwPhDiIoFXfkxZWuPsxeutgfnanoDHM6NjWz5AtDHFwrppmRw98E6ds4tIugEvssLeeYErZI9yBpt5CRDYOyGp00443vZS1G");
```

### Testing Stripe Payments

Use the Stripe test card: **4242 4242 4242 4242**, any future expiry date, any 3-digit CVV. After checkout, the transaction appears in the [Stripe test dashboard](https://dashboard.stripe.com/test/payments) and the RSVP in Firestore gets `paymentStatus = "CONFIRMED"` and a real `paymentRef = "pi_..."`.

---

## 2. Bank Transfer Payment Method

### What I Removed and Why

I removed **JazzCash** and **Apple Pay** from the checkout screen. Both were non-functional radio buttons — selecting them showed nothing useful and they never processed any payment. Leaving broken UI in a demo is worse than not having it.

### What I Added Instead

**Bank Transfer** — a real, trackable flow:

1. User selects Bank Transfer at checkout
2. A card appears showing the account details to transfer to:
   - **Bank:** HBL
   - **Account Name:** Campus Events Society
   - **Account Number:** 0123-4567890-001
   - **IBAN:** PK36HABB0000123456789001
3. User transfers money via their own banking app externally
4. User taps **Upload Screenshot** and picks their payment receipt from the gallery
5. The screenshot uploads to Cloudinary (anonymous unsigned upload — no API key in the app)
6. The Cloudinary URL is saved to the `Payment` document in Firestore (`proofUrl` field) and also onto the RSVP (`paymentProofUrl` field)
7. A unique transaction reference is generated (`bank_` + UUID, e.g. `bank_a3f91c2d`) and saved to both documents
8. The user is taken to the ticket screen which shows a receipt card — the uploaded screenshot and the transaction reference number displayed prominently (like an EasyPaisa/JazzCash confirmation screen)

### The Receipt Screen (EasyPaisa/JazzCash Style)

After Bank Transfer checkout completes, `TicketActivity` detects `paymentMethod = "BANK_TRANSFER"` and shows a receipt card below the QR code containing:
- The uploaded proof screenshot (loaded from Cloudinary via Glide)
- "Transaction Reference" label
- The `bank_XXXXXXXX` ID in large bold monospace — this is the number the user keeps for their records
- A note: "Keep this reference for your records. The organiser will confirm your payment."

For Credit/Debit Card payments, this card is hidden — only the QR code is shown.

### Organiser View

The organiser opens the event → taps **View Payments** → sees `PaymentConfirmationActivity`, which lists every payment for the event with the proof screenshot loaded via Glide. This is how the organiser verifies Bank Transfer payments before manually confirming them.

---

## 3. Cloudinary Image Upload

Screenshots (and event thumbnails from `CreateEventActivity`) are uploaded using Cloudinary's **unsigned upload** API. This means no API key or secret is stored in the app — only two public values:

```java
// Config.java
public static final String CLOUDINARY_CLOUD_NAME    = "dcxablsft";
public static final String CLOUDINARY_UPLOAD_PRESET = "campus_event_discovery";
```

The upload goes directly from the device to Cloudinary's CDN. The returned HTTPS URL is what gets stored in Firestore.

**If the upload fails with "Upload preset not found" or "Must supply api_key":**
1. Log in at https://cloudinary.com/console
2. Go to **Settings → Upload → Upload Presets**
3. Find `campus_event_discovery` and confirm its **Signing Mode** is **Unsigned**
4. If it is set to Signed, change it to Unsigned and save

---

## 4. Payment Data Model

### New fields I added to `Rsvp.java`

```java
private String paymentStatus;   // "CONFIRMED", "PENDING", or "REJECTED"
private String paymentRef;      // Stripe pi_... ID or bank_XXXXXXXX
private String paymentMethod;   // "CREDIT_CARD", "DEBIT_CARD", or "BANK_TRANSFER"
private String paymentProofUrl; // Cloudinary URL (Bank Transfer only)
```

### New fields I added to `Payment.java`

```java
private String paymentMethod; // "CREDIT_CARD", "DEBIT_CARD", or "BANK_TRANSFER"
private String proofUrl;      // Cloudinary URL (Bank Transfer only)
```

### Constants I added (`Constants.java`)

```java
public static final String PAYMENT_CONFIRMED = "CONFIRMED";
public static final String PAYMENT_PENDING   = "PENDING";
public static final String PAYMENT_REJECTED  = "REJECTED";
public static final String COLLECTION_PAYMENTS = "payments";
```

### Firestore structure

| Collection | Document | Key fields |
|---|---|---|
| `payments` | auto-ID | `userId`, `eventId`, `amount`, `status`, `transactionId`, `paymentMethod`, `proofUrl`, `timestamp` |
| `users/{uid}/rsvps` | `{eventId}` | `paymentStatus`, `transactionId`, `paymentRef`, `paymentMethod`, `paymentProofUrl`, `qrPayload`, `qrExpired` |
| `events` | `{eventId}` | `title`, `date`, `location`, `capacity`, `rsvpCount`, `checkedInCount` |
| `events/{eventId}/attendees` | `{uid}` | `fullName`, `checkedIn`, `transactionId`, `qrExpired`, `status` |

---

## 5. Real-Time Event Listener (`observeEventById`)

I added `observeEventById()` to `EventRepository.java`. This attaches a Firestore real-time snapshot listener instead of a one-shot `get()`, so `OrganizerEventDetailActivity` updates automatically whenever the event document changes — for example, when an attendee checks in on a different device while the organiser is watching the screen.

```java
public ListenerRegistration observeEventById(String eventId, SingleEventCallback cb) {
    return db.collection(COLLECTION_EVENTS).document(eventId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) { cb.onError(error); return; }
                if (snapshot != null && snapshot.exists()) cb.onSuccess(documentToEvent(snapshot));
                else cb.onError(new Exception("Event not found."));
            });
}
```

`OrganizerEventDetailActivity` starts the listener in `onStart()` and removes it in `onStop()` so there are no memory leaks.

The registration counter on that screen now shows:
```
X attended • Y/Z
```
where X = `checkedInCount`, Y = `rsvpCount`, Z = `capacity`.

---

## 6. QR Check-In Transaction Logic (`ScannerActivity`)

I rewrote the check-in logic in `ScannerActivity` and `EventRepository` to use a proper **atomic Firestore transaction** instead of a bare `update()` call. The transaction:

1. Verifies the event document exists
2. Verifies the attendee subcollection document exists
3. Verifies the RSVP document exists
4. Checks that the scanned `transactionId` matches the one stored on the RSVP
5. Guards against double check-in (returns early if `checkedIn == true`)
6. Atomically writes `checkedIn = true`, `qrExpired = true`, `status = "attended"` to both the attendee doc and the RSVP doc
7. Increments `checkedInCount` on the event document using `FieldValue.increment(1)` (race-condition safe)

I also added an event ID mismatch check at the scan stage — if someone tries to use a QR code from a different event, it fails immediately with a clear error before touching Firestore.

Also upgraded `cancelRsvp()` to track whether the user was checked in, and decrement `checkedInCount` if they cancel after having already checked in.

---

## 7. Pulling Nausherwan's Branch

Nausherwan's branch (`nausher-final-fix`) was a separate working branch. Rather than doing a GitHub merge (which would have required resolving conflicts on the remote and risked overwriting either person's work), I did a **local manual merge**:

1. Downloaded Nausher's branch as a zip from GitHub
2. Did a side-by-side diff of every Java file and resource file between the two branches
3. Identified what Nausher changed vs what I changed, and which version was better for each file
4. Copied Nausher's improvements into my branch's files by hand

**What I took from Nausher's branch:**
- `CloudinaryHelper.java` — his version made the class `final` with a private constructor (utility class pattern), and had cleaner error logging. I merged this into my version since I had added `uploadImage()` on top of his structural changes.
- `bg_icon_button_circle.xml` — his version used 50% opacity (`#80000000`) vs my 20% (`#33000000`). Took his.
- Real-time listener pattern in `OrganizerEventDetailActivity` — his version used `addSnapshotListener` which I then built on top of with my `observeEventById()` method.

**What I kept from my branch (did not overwrite):**
- All payment files (`CheckoutActivity`, `PaymentConfirmationActivity`, `StripePaymentService`, `PaymentRepository`, `Payment`, `Rsvp` with payment fields)
- `Constants.java`
- All unit tests
- `CreateEventActivity` with Cloudinary upload

The final merged code is on my branch (`yahya`) on GitHub and is the single source of truth.

---

## 8. Unit Tests

All tests are in `app/src/test/java/com/example/CampusEventDiscovery/` and run on the **JVM without a device or emulator** using Robolectric + Mockito.

To run all tests:
```bash
./gradlew test
```

To run a specific test class:
```bash
./gradlew test --tests "com.example.CampusEventDiscovery.BankTransferPaymentTest"
```

### Test coverage

| Test class | What is tested |
|---|---|
| `AuthRepositoryTest` | Firebase Auth mock: login success, logout, error handling |
| `BankTransferPaymentTest` | Proof URL stored on Payment model, proof URL on Rsvp, null/empty proof detection, unique transaction ID generation, amount preservation, field independence between proofUrl / paymentRef / transactionId, BANK_TRANSFER constant, Rsvp flow-through |
| `ConfigTest` | `CLOUDINARY_CLOUD_NAME` and `CLOUDINARY_UPLOAD_PRESET` are non-empty strings |
| `ConstantsTest` | `PAYMENT_CONFIRMED = "CONFIRMED"`, `PAYMENT_PENDING = "PENDING"`, `PAYMENT_REJECTED = "REJECTED"` exact values |
| `EventRepositoryTest` | Firestore event CRUD mocked with Mockito |
| `EventValidatorTest` | Title required, description required, venue required, date must be future, capacity must be positive, organizer ID required |
| `PaymentFlowIntegrationTest` | StripePaymentService → PaymentRepository → Rsvp end-to-end flow |
| `PaymentModelTest` | Payment POJO: empty constructor, full constructor, getter/setter round-trips |
| `PaymentRepositoryTest` | `savePayment`, `getPaymentsForEvent` with mocked Firestore |
| `RsvpManagerTest` | Duplicate RSVP prevention, capacity enforcement |
| `RsvpPaymentFieldsTest` | `paymentRef`, `paymentMethod`, `paymentProofUrl` fields exist and round-trip correctly |
| `SignupValidatorTest` | Email format, password length, display name not empty |
| `StripePaymentServiceTest` | Synchronous stub returns Payment with correct amount, currency, and pi_... ID shape |
| `UITest` | Basic smoke test — app entry point does not crash |
| `UserRolesTest` | `isAttendee`, `isOrganiser`, `isAdmin` return correct values for each role string |

---

## 9. Project Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11+
- Physical Android device or emulator running API 24+ (Android 7.0+)

### Steps

1. **Clone the repo and check out my branch:**
   ```bash
   git clone <repo-url>
   git checkout yahya
   ```

2. **Open in Android Studio:** `File → Open` → select the project root folder.

3. **Sync Gradle:** Android Studio prompts automatically. If not: `File → Sync Project with Gradle Files`.

4. **`google-services.json`** is already at `app/google-services.json`. Do not replace it unless you are connecting a different Firebase project.

5. **Run on device:** Press ▶. The app connects to the live Firebase, Stripe (via Supabase), and Cloudinary automatically. No local server needed.

6. **Run tests:**
   ```bash
   ./gradlew test
   ```

---

## 10. Key Files I Wrote or Modified

```
app/src/main/java/.../
├── ui/event/
│   ├── CheckoutActivity.java              — Full payment flow: Stripe + Bank Transfer + Cloudinary upload
│   ├── PaymentConfirmationActivity.java   — Organiser screen: payments list + proof screenshots
│   └── TicketActivity.java                — QR ticket + Bank Transfer receipt card (EasyPaisa-style)
├── ui/organizer/
│   ├── OrganizerEventDetailActivity.java  — Real-time listener for registration counts
│   ├── ScannerActivity.java               — QR scan + atomic Firestore check-in transaction
│   └── CreateEventActivity.java           — Event proposal form with Cloudinary thumbnail upload
├── util/
│   ├── StripePaymentService.java          — HTTP call to Supabase Edge Function → Stripe pi_... ID
│   ├── CloudinaryHelper.java              — Unsigned image upload helper (merged with Nausher's refactor)
│   ├── Config.java                        — Cloudinary cloud name + upload preset
│   └── Constants.java                     — PAYMENT_CONFIRMED / PAYMENT_PENDING / PAYMENT_REJECTED
├── model/
│   ├── Payment.java                       — Added paymentMethod + proofUrl fields
│   ├── Rsvp.java                          — Added paymentStatus, paymentRef, paymentMethod, paymentProofUrl
│   ├── Event.java                         — Added imageUrl alias + attendedCount alias (Nausher compat)
│   └── EventProposal.java                 — Added imageUrl alias (Nausher compat)
└── repository/
    ├── EventRepository.java               — Added observeEventById(), upgraded cancelRsvp(), added checkInAttendeeByScan()
    └── PaymentRepository.java             — savePayment, getPaymentsForEvent

supabase/functions/create-payment-intent/
└── index.ts                               — Deno Edge Function (already deployed to Supabase cloud)

app/src/test/java/.../
└── (15 test classes — see Section 8)

app/src/main/res/layout/
├── activity_checkout.xml                  — Bank Transfer account info card, removed JazzCash/Apple Pay
└── activity_ticket.xml                    — Bank Transfer receipt card (screenshot + TID)
```

---

*Yahya — Team Gemini, LUMS CS360, Spring 2026*
