# README Content Inventory

This file captures the current documentation and codebase signals that should drive the final project README.

## Repository Shape

- Platform: Android app in Java with XML layouts
- Primary app module: `app/`
- Backend integrations: Firebase Auth, Firestore, Storage, Messaging, App Check
- Supporting backend code: Firebase Functions in `functions/`
- Additional payment backend helper: Supabase Edge Function in `supabase/functions/create-payment-intent/`
- Production source files: about `94` Java files
- Layout files: about `61`
- Local unit/contract/integration-style tests: `44`
- Instrumented Android tests: `8`

## Canonical Sources For Final README

- Product and architecture baseline:
  - `README.md`
  - `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
  - `app/src/main/AndroidManifest.xml`
  - `app/build.gradle.kts`
- Feature-specific implementation notes:
  - `README_AUTH_AND_PERSONALISED_CHANGES.md`
  - `README_EVENT_REMINDERS_YAHYA.md`
  - `README_REMINDERS_AND_REFUNDS.md`
  - `README_SOS.md`
  - `STRIPE_SETUP.md`
  - `REFUND_FIX_README.md`
- Database and testing references:
  - `docs/db/initial_db.txt`
  - `docs/testing_coverage_report.md`
  - `docs/sos_test_plan.md`
- Generated API reference:
  - `docs/javadoc/`

## Existing Documentation Buckets

- Main project overview:
  - `README.md`
- Branch/change logs and feature notes:
  - `README_AUTH_AND_PERSONALISED_CHANGES.md`
  - `README_BUG.md`
  - `README_DIFF.md`
  - `README_EVENT_REMINDERS_YAHYA.md`
  - `README_PERSONALISED_RECOMMENDATIONS.md`
  - `README_PROJECT_COMPARISON.md`
  - `README_REMINDERS_AND_REFUNDS.md`
  - `README_SOS.md`
  - `FRONTEND_CHANGE_NOTES.md`
  - `REFUND_FIX_README.md`
  - `PROJECT_CHANGELOG.md`
  - `MEMORY_MAP.md`
  - `Important_improvements.md`
- Team coordination and merge notes:
  - `docs/conflicts_nausherwan.md`
  - `docs/conflicts_yahya.md`
  - `docs/github_4_stage_breakdown.md`
- Technical deep references:
  - `docs/db/initial_db.txt`
  - `docs/testing_coverage_report.md`
  - `docs/javadoc/`

## Implemented Product Areas Confirmed In Code

### User roles and entry flow

- Welcome, sign in, sign up, splash, maintenance flow
- Developer bypass for attendee, organizer, and admin role simulation
- Role-aware main navigation and role-specific home/profile surfaces

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/SplashActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/WelcomeActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/SignInActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/SignUpActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/DevSessionManager.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/DevBypassHelper.java`

### Attendee experience

- Home feed with featured events and personalised recommendations
- Search and filtering
- Favourites
- My Events
- Calendar
- Event detail page
- Ticket purchase and checkout
- QR ticket viewing
- Event feedback
- Memories and memory albums
- Notification center
- Campus map viewer

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/search/SearchFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/favourites/FavouritesFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/myevents/MyEventsFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/calendar/EventCalendarFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/BuyTicketActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CheckoutActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/TicketActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventFeedbackActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/NotificationCenterActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoriesActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoryAlbumActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoryPhotoViewerActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CampusMapActivity.java`

### Organizer experience

- Event proposal creation
- Approved/pending/rejected event management
- Proposal detail review for organizer
- Organizer event detail view
- Attendee management
- QR scanning and check-in
- Ticket capture flow
- Attendee blacklisting
- Vendor proposal submission and event vendor management

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/CreateEventActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/ManageEventsActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/OrganizerProposalDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/OrganizerEventDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/WhoIsComingActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/ScannerActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/TicketCaptureActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/vendors/VendorManagementFragment.java`

### Admin experience

- Pending proposal review
- Event approval/rejection
- Admin home dashboard
- Vendor request review
- Maintenance mode handling

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeAdminFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventApprovalActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/admin/AdminHomeActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/MaintenanceActivity.java`

### SOS and safety

- SOS trigger tied to event attendance conditions
- Location-aware SOS submission
- Full-screen alert surface
- SOS dashboard for staff/admin users
- Backend fan-out via Firebase Functions

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/sos/SosActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/sos/SOSAlertActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/sos/SOSDashboardActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/SosRepository.java`
- `functions/index.js`

### Payments, refunds, and ticketing

- Stripe-oriented payment flow on Android
- Payment record persistence in Firestore
- Paid RSVP handling
- Credit/refund-related constants and tests
- QR payload generation and check-in

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/util/StripePaymentService.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/PaymentRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CheckoutActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/PaymentConfirmationActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/QRCodeHelper.java`
- `supabase/functions/create-payment-intent/index.ts`

### Notifications and reminders

- Firebase Cloud Messaging service in app
- Reminder routing to calendar tab
- Scheduled reminder backend function

Key files:

- `app/src/main/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingService.java`
- `app/src/main/java/com/example/CampusEventDiscovery/CampusEventDiscoveryApp.java`
- `functions/index.js`

## Core Data Layer Summary

`EventRepository.java` is the central business/data layer and covers:

- Event loading, search, featured events, recommendations
- Saved events and RSVP lifecycle
- Ticket tiers
- Memories and ratings
- Proposal creation and approval
- Organizer event operations
- Attendee retrieval, blacklisting, and check-in
- SOS reporting
- Notifications and announcements
- User profile updates
- Vendor proposal approval/rejection

This file should be treated as the strongest single source for feature claims in the final README.

## Data Model Surface

Models confirmed:

- `User`
- `Event`
- `Rsvp`
- `EventProposal`
- `EventAttendee`
- `Payment`
- `Notification`
- `Memory`
- `SosAlert`
- `TicketTier`
- `VendorProposal`
- `AvatarConfig`

## Tech Stack Confirmed

### Android

- Java 11
- AndroidX
- Material Components
- RecyclerView
- CardView
- Glide
- ZXing embedded scanner
- Google Play Services Location
- Cloudinary Android SDK

### Firebase

- Firebase Auth
- Firestore
- Storage
- Messaging
- App Check
- Firebase Functions

### Testing

- JUnit 4
- Robolectric
- Mockito
- Espresso
- AndroidJUnitRunner

## Test Story

The current repository contains substantial automated coverage, but the coverage report says the suite is not fully green.

Useful sources:

- `docs/testing_coverage_report.md`
- `app/src/test/java/`
- `app/src/androidTest/java/`

Instrumented test classes currently include:

- `AdminProfileInstrumentedTest`
- `AuthScreensInstrumentedTest`
- `NavigationSurfacesInstrumentedTest`
- `SystemJourneyInstrumentedTest`
- `UtilityScreensInstrumentedTest`
- `VendorManagementInstrumentedTest`
- `WalkthroughManagerInstrumentedTest`

## UML Assets

In-repo UML and diagram assets already exist under:

- `docs/images/uml/`
- `docs/images/`

User-supplied UML bundle extracted for review to:

- `/private/tmp/codex-umls/UMLs/`

Bundle contents:

- `application architecture.png`
- `Authentication and roles based navigation flow.png`
- `Event proposal,approval and publishing flow.png`
- `highLevelUML.png`
- `QR checkin and attendee management flow.png`
- `RSVP payment  and QR ticketing flow.png`
- `SOS and vendor management flow.png`

## Documentation Issues To Resolve In Final README

- The current `README.md` is comprehensive, but it mixes durable architecture with branch-history details.
- Several README files are change logs rather than end-user or reviewer-friendly documentation.
- Some docs use different terminology for the same concepts, so the final README should normalize naming.
- The final README should separate:
  - product overview
  - architecture
  - setup
  - feature walkthrough
  - testing
  - screenshots/UML
  - known limitations

## Recommended Final README Structure

1. Executive summary
2. Problem statement and target users
3. Feature overview by role
4. Product walkthrough with key flows
5. System architecture
6. Tech stack
7. Database and backend design
8. Setup and run instructions
9. Testing strategy and current coverage
10. UML diagrams and screenshots
11. Team contributions
12. Known limitations and next steps

## Caution Notes For Drafting

- Prefer code-verified claims over branch-note claims.
- Use `EventRepository.java`, manifest entries, layouts, and test files to verify behavior.
- Treat audit-style docs like `README_BUG.md` as supporting material, not the main product narrative.
- Keep reminder, refund, vendor, and SOS sections aligned with both Android code and `functions/index.js`.
