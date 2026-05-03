# MEMORY_MAP

Purpose: compact repo memory for future work. Read this first, not the full README, unless you need setup or design-history detail.

## 1. Project Identity

- Project: `Campus Event Discovery and Management Platform`
- Platform: Android app in Java/XML
- Primary backend: Firebase Auth, Firestore, Storage, Messaging, App Check
- Secondary backend piece: Supabase edge function for Stripe payment intent creation
- Build system: Gradle Kotlin DSL
- Android config: `minSdk 24`, `targetSdk 34`, `compileSdk 36`, Java 11

## 2. Repo Shape

- `app/`: main Android application code and tests
- `functions/`: Firebase Cloud Functions for SOS fan-out and scheduled reminders
- `supabase/`: Supabase edge function for Stripe payment intents
- `docs/`: diagrams, Javadocs, plans, issue notes, generated/reference material
- `gradle/`, root `build.gradle.kts`, `settings.gradle.kts`: build wiring
- Root `README*.md` files: feature-specific notes and change history; useful for archaeology, not first-pass navigation

## 3. Runtime Entry Flow

Start here for app behavior:

1. `app/src/main/java/com/example/CampusEventDiscovery/CampusEventDiscoveryApp.java`
   Initializes Firebase, App Check, theme lifecycle, Cloudinary, SOS/reminder notification channels.
2. `app/src/main/java/com/example/CampusEventDiscovery/SplashActivity.java`
   Launcher activity from manifest.
3. `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
   Main shell after auth; role-aware bottom navigation and fragment host.

Auth/front-door screens:

- `WelcomeActivity.java`
- `SignInActivity.java`
- `SignUpActivity.java`

## 4. High-Value Packages

### Core data/repositories

- `repository/EventRepository.java`
  Main Firestore gateway. Central place for events, proposals, RSVP, favourites, notifications, memories, attendees, recommendations, maintenance mode, featured events, vendor proposals.
- `repository/FirebaseAuthRepository.java`
  Signup/login + user profile creation/read.
- `repository/PaymentRepository.java`
  Firestore payment persistence/query.
- `repository/SosRepository.java`
  Writes SOS alerts and related notification fan-out records.
- `repository/MockAuthRepository.java`
  Test/dev auth alternative.

### UI feature areas

- `ui/home/`
  Role-specific home fragments: attendee, organizer, admin.
- `ui/search/SearchFragment.java`
  Search/browse surface.
- `ui/event/`
  Event details, ticket purchase, checkout, approvals, feedback, map.
- `ui/organizer/`
  Event creation, management, QR scan/check-in, attendee list.
- `ui/profile/`
  Profile, memories, notifications, settings, help.
- `ui/calendar/EventCalendarFragment.java`
  Calendar flow.
- `ui/favourites/FavouritesFragment.java`
  Saved events.
- `ui/myevents/MyEventsFragment.java`
  RSVP'd/owned event lists depending on context.
- `ui/admin/AdminHomeActivity.java`
  Admin surface.
- `ui/sos/`
  SOS trigger/dashboard/alert UI.
- `ui/vendors/VendorManagementFragment.java`
  Vendor proposal/management flow.

### Models

`model/` contains the app's Firestore/domain objects:

- `Event`
- `User`
- `Rsvp`
- `Payment`
- `TicketTier`
- `EventProposal`
- `EventAttendee`
- `Notification`
- `Memory`
- `SosAlert`
- `VendorProposal`
- `AvatarConfig`

### Adapters

`adapter/` holds RecyclerView adapters for events, attendees, ticket tiers, memories, notifications, vendors, organizer pending items.

### Utilities

Most touched utility files:

- `util/Constants.java`: shared collection names, payment statuses, prefs keys, map constants
- `util/ThemeManager.java`: theme/accent behavior
- `util/EventValidator.java`: event form validation
- `util/EventTimeUtils.java`: time-window logic, including SOS/event timing
- `util/StripePaymentService.java`: Stripe-related client-side flow
- `util/MyFirebaseMessagingService.java`: FCM handling
- `util/CloudinaryHelper.java`: media upload integration
- `util/DevSessionManager.java` and `util/DevBypassHelper.java`: local bypass/test role flow
- `util/WalkthroughManager.java`: guided demo/walkthrough mode
- `util/AuthPreferenceManager.java`: remember-me behavior
- `util/QRCodeHelper.java`: ticket QR generation/parsing
- `util/UserRoles.java`: role normalization/checks

## 5. Main Navigation Mental Model

`MainActivity.java` is the navigation brain.

- Attendee home key: `home_attendee`
- Organizer home key: `home_organizer`
- Admin home key: `home_admin`
- Shared tabs/targets: `search`, `favourites`, `profile`, `my_events`, `vendors`, `calendar`, `help`
- Bottom nav changes by role:
  - attendee: action tab opens `MyEvents`
  - organizer: action tab launches `CreateEventActivity`
  - admin: action tab hidden
- Fragment replacement uses `NavigationTransitions`

If a task mentions "why is a tab/fragment behaving oddly", inspect `MainActivity` first.

## 6. Firestore Mental Model

Canonical collection names are spread across `EventRepository.java` and `util/Constants.java`.

Primary collections:

- `users`
- `events`
- `event_proposals`
- `payments`
- `notifications`
- `reports`
- `sos_alerts`
- `app_config`
- `vendorProposals`

Common subcollections:

- `saved_events`
- `rsvps`
- `attendees`
- `memories`
- `ratings`
- `messages`
- `blacklist`

Important note:

- There is some naming duplication/drift between repository-local constants and `Constants.java`. If changing schema-related code, check both places before assuming a single source of truth.

## 7. Feature Ownership Shortcuts

When a task is about:

- Auth/login/signup/dev bypass: `SignInActivity`, `SignUpActivity`, `FirebaseAuthRepository`, `DevSessionManager`
- Event feed/search/recommendations: `HomeFragment`, `SearchFragment`, `EventRepository`
- Event details/save/share/map/calendar: `EventDetailActivity`
- Organizer proposal creation: `CreateEventActivity`, `EventValidator`, `CloudinaryHelper`, `EventRepository`
- Ticket buying/checkout/payment records: `BuyTicketActivity`, `CheckoutActivity`, `PaymentRepository`, `StripePaymentService`, `supabase/functions/create-payment-intent/index.ts`
- QR tickets/check-in: `TicketActivity`, `ScannerActivity`, `TicketCaptureActivity`, `QRCodeHelper`
- SOS flow: `ui/sos/*`, `SosRepository`, `functions/index.js`, `MyFirebaseMessagingService`
- Notifications/reminders: `NotificationCenterActivity`, `MyFirebaseMessagingService`, `functions/index.js`
- Theme/styling regressions: `ThemeManager`, `res/values/themes.xml`, `res/values/colors.xml`, layout XMLs
- Vendor feature: `ui/vendors/VendorManagementFragment`, `model/VendorProposal`, vendor tests
- Memories/profile assets: `MemoriesActivity`, `MemoryAlbumActivity`, `MemoryAdapter`, `MemoryPhotoGridAdapter`

## 8. Backend Touchpoints

### Firebase Cloud Functions

File: `functions/index.js`

Contains:

- `onSosAlertCreated`
  Firestore trigger on `sos_alerts/{alertId}`, sends high-priority FCM data notifications to organizer + admins, prunes invalid tokens.
- `sendEventReminders`
  Scheduled daily reminder job, timezone set to `Asia/Karachi`, looks ahead 3 days and sends FCM reminder data messages.

### Supabase Edge Function

File: `supabase/functions/create-payment-intent/index.ts`

- Creates Stripe payment intents
- Expects `amount`, optional `currency`, `userId`, `eventId`
- Uses `STRIPE_SECRET_KEY` from Supabase env
- This is separate from Firebase and only covers payment-intent creation

## 9. Resources and UI Assets

- Layout XMLs: `app/src/main/res/layout/`
- Drawables/icons/maps: `app/src/main/res/drawable*`
- Strings/colors/themes: `app/src/main/res/values/`
- Nav graph: `app/src/main/res/navigation/nav_graph.xml`
- Manifest permissions cover camera, location, notifications, media read, internet

If a bug is visual, pair the activity/fragment Java file with its matching `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, or `dialog_*.xml`.

## 10. Tests Map

Unit tests: `app/src/test/java/com/example/CampusEventDiscovery/`

Good signal areas:

- repository contracts and personalization
- payment flow
- SOS eligibility/repository/alert behavior
- theme/layout contract tests
- vendor management contracts
- walkthrough/auth preference tests

Instrumentation tests: `app/src/androidTest/java/com/example/CampusEventDiscovery/`

- auth screens
- navigation surfaces
- utility screens
- vendor management
- walkthrough flow

When changing logic, search tests first; many features already have contract coverage.

## 11. Files Usually Worth Ignoring Initially

- `docs/javadoc/`: generated output, not source of truth
- `build.zip`, `app/src.zip`: archive artifacts, not live source
- most root `README_*.md` files: useful for history or feature context, but expensive for first-pass reading
- image-heavy `docs/images/`: documentation assets only

## 12. Likely Friction Points

- Schema constants are not perfectly centralized.
- Repo mixes production flows, demo/walkthrough flows, and dev bypass behavior.
- Firebase and Supabase both exist; payment behavior spans both.
- Some README/history docs may describe intent more than current code reality.
- `EventRepository.java` is large and acts as a "god repository"; many unrelated behaviors converge there.

## 13. Minimal Future Reading Strategy

For most tasks, read in this order:

1. `MEMORY_MAP.md`
2. Specific activity/fragment mentioned by the task
3. Matching repository class
4. Matching model/util class
5. Matching layout XML
6. Relevant tests

For cross-cutting issues:

1. `MainActivity.java`
2. `EventRepository.java`
3. `Constants.java`
4. `ThemeManager.java` or `MyFirebaseMessagingService.java` depending on feature

## 14. Current Workspace Note

- `git status --short` was clean when this map was created.
