# Important Improvements

This document tracks release-blocking vulnerabilities, crash risks, UI consistency issues, and performance improvements found during the project review. It should be used as a prioritized checklist before delivering the app.

## Critical Findings

### 1. Remove or strictly gate development bypass login

**Why this is needed:**  
The app currently exposes development-only login paths inside the auth flow. If this reaches a production build, anyone who discovers the shortcut can enter privileged roles without real authentication.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/SignInActivity.java`
  - `btnDevBypass` is wired to `DevBypassHelper.showRolePicker(this)`.
  - The hardcoded `admin/admin` shortcut enables admin bypass and opens `MainActivity`.
- `app/src/main/java/com/example/CampusEventDiscovery/SignUpActivity.java`
  - Development bypass is also available from sign up.

**Recommended fix:**
- Remove these controls from release layouts and release code paths.
- If still needed locally, gate them behind `BuildConfig.DEBUG`.
- Ensure `DevSessionManager` cannot affect release builds.

### 2. Replace mock checkout for paid events

**Why this is needed:**  
Paid checkout currently completes using `MockPaymentService`. That means a user can receive a confirmed RSVP/ticket without verified real payment.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CheckoutActivity.java`
  - `processPayment()` calls `MockPaymentService.processPayment(...)`.
- `app/src/main/java/com/example/CampusEventDiscovery/util/MockPaymentService.java`

**Recommended fix:**
- Use the real Stripe/Supabase payment flow for paid tickets.
- Keep mock payments only for debug/demo builds.
- Block ticket creation until the backend verifies payment success.

### 3. Make payment and RSVP creation atomic

**Why this is needed:**  
Checkout saves the payment first and then creates the RSVP. If RSVP creation fails because the event is full, the user is blacklisted, or the user is already registered, the payment record can remain confirmed without a valid ticket.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CheckoutActivity.java`
  - `paymentRepository.savePayment(...)` runs before `eventRepository.rsvpEvent(...)`.

**Recommended fix:**
- Move paid checkout finalization to a backend function.
- Validate capacity, duplicate RSVP status, blacklist state, and payment success in one server-controlled flow.
- Only write the confirmed RSVP/ticket after all checks pass.

### 4. Verify backend enforcement for privileged actions

**Why this is needed:**  
Admin and organizer actions are called directly from the client repository. This is safe only if Firestore rules or backend functions enforce role permissions correctly.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
  - Event approval/rejection.
  - Event deletion.
  - Vendor proposal approval/rejection.
  - RSVP/check-in related writes.

**Recommended fix:**
- Add or audit Firestore security rules.
- Ensure only admins can approve/reject events and vendor proposals.
- Ensure organizers can only manage their own events.
- Ensure attendees cannot forge payment, RSVP, ticket, or check-in records.

### 5. Harden release build settings

**Why this is needed:**  
Release builds currently do not use code shrinking or obfuscation, making the APK easier to inspect and increasing app size.

**Where it was found:**
- `app/build.gradle.kts`
  - `release { isMinifyEnabled = false }`

**Recommended fix:**
- Enable `isMinifyEnabled = true` for release.
- Enable resource shrinking if compatible.
- Add keep rules for Firebase, Glide, Cloudinary, Stripe/Supabase, and model classes as needed.

### 6. Review Android backup behavior

**Why this is needed:**  
Android backup is enabled. Local state such as session preferences, theme settings, and any development bypass state should not be restored insecurely on another device.

**Where it was found:**
- `app/src/main/AndroidManifest.xml`
  - `android:allowBackup="true"`

**Recommended fix:**
- Disable backup for release or define strict backup rules.
- Exclude sensitive SharedPreferences and local session state.

## High Priority Crash And Stability Risks

### 1. Add lifecycle guards to async callbacks

**Why this is needed:**  
Several screens start Firebase, repository, location, or delayed tasks and then update UI in callbacks. If the user leaves the screen before the callback returns, the app can crash or show stale UI.

**Where it was found:**
- `SignInActivity.java`
- `SignUpActivity.java`
- `MainActivity.java`
- `ui/event/CheckoutActivity.java`
- `ui/sos/SosActivity.java`
- `ui/organizer/WhoIsComingActivity.java`
- `ui/search/SearchFragment.java`
- `ui/vendors/VendorManagementFragment.java`

**Recommended fix:**
- Add `canUpdateUi()` checks before touching views, showing dialogs/toasts, or navigating.
- Remove listeners in `onDestroyView()` or `onDestroy()`.
- Detach RecyclerView adapters in fragment `onDestroyView()`.

### 2. Store and remove delayed splash callbacks

**Why this is needed:**  
The splash screen posts a delayed route runnable without storing/removing it. This can keep the activity alive longer than needed.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/SplashActivity.java`

**Recommended fix:**
- Store the `Handler` and `Runnable`.
- Remove the runnable in `onDestroy()`.

### 3. Avoid misleading SOS alerts without location

**Why this is needed:**  
When location fails, fallback flows can send coordinates like `0.0,0.0`. That creates a misleading Maps location.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/sos/SosActivity.java`

**Recommended fix:**
- Send `locationAvailable = false` when no real location exists.
- Do not generate a Maps URL for missing coordinates.
- Allow emergency reporting without location instead of dead-ending the user.

## UI Performance Improvements

### 1. Stop full collection reads for search

**Why this is needed:**  
Search currently fetches every active event and filters on the client. This will become slow, expensive, and unstable as event volume grows.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
  - `searchEvents(...)`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/search/SearchFragment.java`

**Recommended fix:**
- Add indexed lowercase searchable fields.
- Query by category/status/date where possible.
- Add pagination.
- Consider Algolia, Typesense, or a backend search endpoint for richer search.

### 2. Add query limits and pagination to event lists

**Why this is needed:**  
Home and event lists fetch all active events and sort client-side. This increases load time, memory use, and Firestore reads.

**Where it was found:**
- `EventRepository.getUpcomingEvents(...)`
- `EventRepository.getPersonalisedEvents(...)`
- Organizer event/event approval list queries.

**Recommended fix:**
- Use Firestore `orderBy(...)`, `whereGreaterThan(...)`, and `limit(...)`.
- Load more results as the user scrolls.
- Keep home sections small and fast.

### 3. Paginate vendor proposals

**Why this is needed:**  
The admin vendor proposal screen fetches every proposal. This will slow down as proposal history grows.

**Where it was found:**
- `EventRepository.getAllVendorProposals(...)`

**Recommended fix:**
- Query by selected status tab.
- Use `orderBy("createdAt")` and `limit(...)`.
- Add pagination or incremental loading.

### 4. Improve vendor proposal read tracking

**Why this is needed:**  
Opening the admin vendor screen marks all pending unread proposals as read. This can be inaccurate if multiple admins use the system or if proposals were never actually viewed.

**Where it was found:**
- `EventRepository.markPendingVendorProposalsRead(...)`

**Recommended fix:**
- Track read state per admin.
- Mark only proposals that were loaded/visible.
- Preserve unread count until the admin actually views the relevant proposal list.

### 5. Replace broad list refreshes with DiffUtil

**Why this is needed:**  
Full adapter refreshes redraw entire lists, cause visual flicker, and make scrolling less stable.

**Where it was found:**
- Event adapters.
- Memory adapters.
- Vendor proposal adapters.
- Search/event list adapters.

**Recommended fix:**
- Move list-based RecyclerViews to `ListAdapter`.
- Add `DiffUtil.ItemCallback` per model.
- Submit immutable list snapshots.

## UI Consistency Improvements

### 1. Avoid broad runtime theme mutation

**Why this is needed:**  
The previous segmented button issue came from broad runtime accent logic touching views it should not mutate. This makes UI behavior unpredictable, especially across different accent colors.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/util/ThemeManager.java`
- Runtime accent application from `MainActivity`.

**Recommended fix:**
- Prefer explicit XML styles and color state selectors.
- Apply accent colors only to known controls.
- Keep segmented controls, bottom nav, buttons, and icons styled through predictable selectors.

### 2. Move high-churn screens to ViewBinding

**Why this is needed:**  
Many screens use large amounts of `findViewById`. This increases refactor risk and makes lifecycle mistakes easier.

**Where it was found:**
- `MainActivity`
- `SearchFragment`
- `VendorManagementFragment`
- `ProfileFragment`
- Organizer home/admin home screens.
- Checkout and event detail screens.

**Recommended fix:**
- Enable ViewBinding.
- Migrate fragments first because they have the highest lifecycle risk.
- Clear binding references in `onDestroyView()`.

### 3. Use explicit loading, empty, and error states

**Why this is needed:**  
Some sections disappear when there is no data. That makes the UI feel broken and makes debugging harder.

**Where it was found:**
- Popular Events sections.
- Vendor proposal/event lists.
- Search results.
- Memory album screens.

**Recommended fix:**
- Show a small empty state when no data exists.
- Show retry actions for failed network loads.
- Keep section headings stable unless the whole feature is unavailable.

### 4. Improve accessibility labels

**Why this is needed:**  
Some images and clickable views use generic content descriptions such as the app name. Screen reader users need action-specific labels.

**Where it was found:**
- Event cards.
- Search result rows.
- Memory album photos.
- Profile action rows.

**Recommended fix:**
- Decorative images should use `importantForAccessibility="no"`.
- Clickable icons/cards should have action-specific descriptions.
- Avoid using `@string/app_name` for event images or repeated decorative imagery.

## Backend And Data Integrity Improvements

### 1. Protect Cloudinary uploads

**Why this is needed:**  
The app uses Cloudinary unsigned uploads. If the preset is permissive, it can be abused to upload unwanted files.

**Where it was found:**
- `app/src/main/java/com/example/CampusEventDiscovery/util/Config.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/CloudinaryHelper.java`

**Recommended fix:**
- Restrict the unsigned preset by folder, file type, size, and moderation settings.
- Prefer signed upload generation from a backend function for production.

### 2. Validate event existence inside write transactions

**Why this is needed:**  
Some repository methods write subcollection data or update related records without consistently failing early when the parent event is missing.

**Where it was found:**
- Rating flow.
- Blacklist flow.
- RSVP/check-in related flows.

**Recommended fix:**
- Check parent document existence inside each transaction.
- Fail before writing any related subcollection data.
- Keep errors explicit so UI can show the right recovery path.

### 3. Sign or server-validate ticket QR payloads

**Why this is needed:**  
The QR flow validates against Firestore, which helps, but the payload itself is client-generated. A signed token or server-issued ticket would be stronger.

**Where it was found:**
- `CheckoutActivity`
- `ScannerActivity`
- `EventRepository.checkInAttendeeByQrToken(...)`

**Recommended fix:**
- Generate QR ticket tokens server-side.
- Include a signature/HMAC or opaque token.
- Verify scan/check-in through a server-controlled path or strict Firestore rules.

## Recommended Execution Order

1. Remove or gate dev bypass login.
2. Replace mock paid checkout and harden payment verification.
3. Verify Firestore security rules for admin, organizer, payment, RSVP, ticket, and vendor writes.
4. Fix payment/RSVP atomicity.
5. Add lifecycle guards to async-heavy screens.
6. Replace full collection reads with indexed and paginated queries.
7. Move high-risk fragments to ViewBinding.
8. Replace broad theme mutation with explicit selectors/styles.
9. Improve empty/error/loading states.
10. Improve accessibility labels and touch targets.

