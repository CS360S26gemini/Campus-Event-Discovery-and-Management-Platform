# Testing Coverage Report

## Scope

This report reviews the implemented app features in the current codebase and maps them against the existing automated tests under:

- `app/src/test/java`
- `app/src/androidTest/java`

It also proposes a plan for expanding coverage across:

- Functional tests
- Unit tests
- Integration tests
- UI tests
- System tests

Date of review: `2026-05-03`

---

## Current Test Setup

### Frameworks already in use

- `JUnit 4`
- `Robolectric`
- `Mockito`
- `Espresso`
- Android instrumentation tests via `AndroidJUnitRunner`

### Current test inventory

- Local tests in `app/src/test/java`: `264`
- Instrumented tests in `app/src/androidTest/java`: `18`

### Baseline run

Command used:

```bash
./gradlew testDebugUnitTest
```

Observed result:

- `264` tests completed
- `15` failed
- `6` skipped

Current failing areas from that run:

- `BlacklistTest`
- `UITest`
- `EventRepositoryPersonalisationTest`
- `LayoutStyleContractTest`
- `VendorManagementContractTest`

This means the existing local suite is substantial, but it is not currently green.

---

## Feature Coverage Matrix

Legend:

- `Covered`: feature has direct automated tests
- `Partial`: feature has only structural, limited, or indirect tests
- `Missing`: no meaningful automated coverage found

| Feature Area | Current Status | Existing Tests |
|---|---|---|
| Welcome, sign-in, sign-up, auth validation | Covered | `AuthRepositoryTest`, `SignupValidatorTest`, `AuthPreferenceManagerTest`, `AuthScreensInstrumentedTest`, `UITest` |
| Role handling and role-aware navigation | Partial | `UserRolesTest`, `NavigationSurfacesInstrumentedTest`, `VendorManagementInstrumentedTest` |
| Event repository core operations | Partial | `EventRepositoryTest` |
| RSVP model and RSVP payment fields | Covered | `RsvpManagerTest`, `RsvpPaymentFieldsTest` |
| Event discovery personalisation / recommendations | Covered, but unstable | `EventRepositoryPersonalisationTest` |
| Payment model, repository, Stripe flow, bank transfer, refunds | Covered | `PaymentModelTest`, `PaymentRepositoryTest`, `StripePaymentServiceTest`, `BankTransferPaymentTest`, `PaymentFlowIntegrationTest`, `RefundPolicyTest`, `TicketTierFieldsTest`, `UserCreditBalanceTest` |
| SOS model, eligibility, repository, dashboard surface | Covered | `SosAlertTest`, `SosEligibilityTest`, `SosRepositoryTest`, `UtilityScreensInstrumentedTest`, `docs/sos_test_plan.md` |
| Vendor proposal and vendor management | Covered, mostly contract-heavy | `VendorProposalTest`, `VendorRepositoryContractTest`, `VendorManagementContractTest`, `VendorManagementInstrumentedTest` |
| Memory album feature | Partial | `MemoryAlbumContractTest`, `UtilityScreensInstrumentedTest` |
| Cloudinary upload usage | Partial | `CloudinaryUploadContractTest` |
| Walkthrough/help onboarding | Covered | `WalkthroughManagerContractTest`, `WalkthroughManagerInstrumentedTest` |
| Firebase reminder notification helper | Partial | `MyFirebaseMessagingServiceTest` |
| Theme, layout, navigation string/style contracts | Covered | `LayoutStyleContractTest`, `ThemeStyleContractTest` |
| Config, constants, avatar config | Covered | `ConfigTest`, `ConstantsTest`, `AvatarConfigTest` |
| Splash routing | Missing | None found |
| Main attendee home behavior | Missing | None found |
| Search filters and sort behavior in `SearchFragment` | Missing | None found |
| Favourites behavior in `FavouritesFragment` | Missing | None found |
| My Events flows in `MyEventsFragment` | Missing | None found |
| Calendar behavior in `EventCalendarFragment` | Missing | None found |
| Event detail behavior | Missing | None found |
| Buy ticket screen behavior | Missing | None found |
| Checkout UI behavior | Missing | None found |
| Ticket screen and refund UI behavior | Missing | None found |
| Event feedback flow | Missing | None found |
| Campus map behavior | Missing | None found |
| Create event form validation and submission flow | Missing | None found |
| Organizer event detail actions | Missing | None found |
| Manage events behavior beyond surface checks | Partial | `NavigationSurfacesInstrumentedTest` |
| QR scanner camera permission and scan result flow | Partial | `UtilityScreensInstrumentedTest` |
| Who Is Coming attendee actions | Partial | `NavigationSurfacesInstrumentedTest` |
| Event approval screen actions | Missing | None found |
| Admin home behavior beyond visible controls | Partial | `VendorManagementInstrumentedTest` |
| Profile behavior, avatar save, theme toggle, account save | Partial | `UITest`, `MemoryAlbumContractTest`, `ThemeStyleContractTest` |
| Notification center behavior | Missing | None found |
| Memories list behavior | Missing | None found |
| Memory photo viewer behavior | Missing | None found |
| Maintenance mode flow | Missing | None found |

---

## What The Existing Tests Actually Cover

### Strongest current areas

1. Authentication and validation
2. Payment domain logic
3. SOS repository/domain logic
4. Static UI contracts for layouts and vendor surfaces
5. Vendor role navigation surfaces

### Areas with only shallow coverage

These features have tests, but the tests are mostly structural or indirect:

- `Search`, `calendar`, `favourites`, and `my events` navigation are not behavior-tested
- `Vendor` and `memory` features rely heavily on contract tests that inspect code/layout structure instead of user workflows
- `Scanner`, `SOS dashboard`, and `payment confirmation` are checked mostly for visible widgets, not end-to-end behavior
- `Cloudinary` is validated by source-level contract tests, not actual upload integration

### Areas with no meaningful coverage

- Attendee browsing workflows
- Event detail actions
- Full ticket purchase UI flow
- Event creation and approval flow
- Notification center
- Splash and maintenance routing
- Real multi-role end-to-end scenarios

---

## Recommended Functional Test Coverage

Functional tests should verify that complete user-facing features behave correctly, not just that views exist.

### Highest-priority functional scenarios to add

1. `SplashActivity` routes correctly for signed-out, attendee, organizer, admin, and maintenance-mode cases.
2. Attendee can browse from `HomeFragment` to `EventDetailActivity`.
3. Search by keyword and category in `SearchFragment` filters results correctly.
4. Saving and unsaving an event updates `FavouritesFragment`.
5. Attendee RSVP flow works for free events.
6. Paid event flow moves from `EventDetailActivity` to `BuyTicketActivity` to `CheckoutActivity` to `TicketActivity`.
7. Ticket cancellation/refund path behaves correctly inside `TicketActivity`.
8. Organizer can create an event proposal from `CreateEventActivity`.
9. Admin can approve and reject proposals from `EventApprovalActivity`.
10. Organizer can open `ManageEventsActivity`, drill into `OrganizerEventDetailActivity`, and access attendee/payment actions.
11. Organizer can mark attendance from `WhoIsComingActivity` and via `ScannerActivity`.
12. Attendee can submit event feedback with and without photos.
13. User can open `NotificationCenterActivity` and navigate from a notification to the target event.
14. User can open calendar events from `EventCalendarFragment`.
15. User can create and manage memories and album photos.

### Recommended location

- Add new behavior-driven tests primarily under `app/src/androidTest/java`
- Use Espresso for screen flow verification
- Use developer bypass / fake intent inputs where possible to avoid external Firebase dependence

---

## Recommended Unit Test Expansion

Unit tests should target isolated logic classes and pure decision-making code.

### Good candidates for more unit coverage

1. `EventTimeUtils`
2. `EventShareHelper`
3. `QRCodeHelper`
4. `ThemeManager`
5. `WalkthroughManager` edge cases
6. `AvatarRenderer`
7. `DevSessionManager`
8. `DevBypassHelper`
9. `EventValidator` edge cases for dates, ticket tiers, and paid/free combinations
10. `AccountSettingsActivity` helper methods extracted into testable units

### Unit gaps visible today

- Time and date handling around event start/end
- QR token generation and parsing contracts
- Share payload formatting
- Theme persistence behavior
- Role/session utility edge cases

---

## Recommended Integration Test Expansion

Integration tests should verify collaboration between app layers such as repository + model + helper logic.

### Highest-value integration targets

1. `EventRepository` plus RSVP plus attendee count updates
2. `CheckoutActivity` payment completion plus RSVP generation plus ticket navigation
3. `SosRepository` plus notification fan-out behavior
4. Event recommendation generation using real in-memory event lists
5. Event creation flow combining validation plus repository write preparation
6. Vendor proposal review plus organizer notification generation
7. Memory photo upload metadata preparation plus Cloudinary helper integration seam

### Recommended approach

- Keep these under `app/src/test/java`
- Prefer mocked Firestore boundaries with richer state assertions
- Consider Firebase Emulator based tests later for repository write/query verification

---

## Recommended UI Test Expansion

UI tests should validate rendering, interaction, navigation, and visible state changes.

### Highest-priority UI screens to add

1. `MainActivity` bottom navigation behavior for all roles
2. `HomeFragment` cards, share button, SOS entry, and featured event behavior
3. `SearchFragment` chips, sorting dialog, and result tap behavior
4. `FavouritesFragment` empty and populated states
5. `MyEventsFragment` attendee and organizer states
6. `EventDetailActivity` ticket CTA, map CTA, calendar CTA, favourite toggle, and SOS visibility
7. `BuyTicketActivity` tier selection behavior
8. `CheckoutActivity` form validation and loading/error states
9. `TicketActivity` QR visibility and refund/cancel actions
10. `CreateEventActivity` dynamic ticket tier UI
11. `AccountSettingsActivity` save flow and validation errors
12. `NotificationCenterActivity` unread/read interaction

### Notes

- Current UI coverage is concentrated on screen existence and control visibility
- The biggest missing piece is user interaction with state changes

---

## Recommended System Test Expansion

System tests should validate complete multi-screen workflows across roles. If by "systematic" you meant "system", this is the category that best fits that request.

### Recommended system scenarios

1. Attendee discovers an event, purchases a ticket, views the QR ticket, and later requests a refund.
2. Organizer creates a proposal, admin approves it, attendee discovers it, then organizer manages attendance.
3. Attendee triggers SOS at an event, organizer/admin dashboard receives the alert, and the alert can be reviewed.
4. Organizer adds a vendor proposal, admin reviews it, and organizer sees the updated vendor state.
5. User signs in, updates profile/settings, receives a reminder notification, and navigates into the event from the notification.

### Tooling recommendation

- Keep system tests separate from unit tests
- Long term, use Firebase Emulator Suite for backend state
- Seed deterministic users/events for attendee, organizer, and admin journeys

---

## Proposed Priority Plan

### Phase 1: Stabilize what already exists

1. Fix the `15` currently failing local tests.
2. Separate brittle contract tests from behavior tests in reporting.
3. Add CI commands for local tests and instrumentation tests.

### Phase 2: Build missing functional coverage for core user value

1. Auth and splash routing
2. Home and search
3. Event detail and favourites
4. Free RSVP and paid ticket checkout
5. Organizer create/manage/check-in
6. Admin approval flow

### Phase 3: Expand secondary product areas

1. Calendar
2. Memories
3. Notifications
4. Profile/settings
5. Vendor workflows

### Phase 4: Add system-level flows

1. End-to-end attendee journey
2. End-to-end organizer journey
3. End-to-end admin moderation journey
4. SOS emergency journey

---

## Suggested New Test Files

These would be a clean next step if implementation starts immediately:

- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/SplashRoutingInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/HomeFragmentInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/SearchFlowInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/FavouritesFlowInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/EventDetailInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/CheckoutFlowInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/CreateEventFlowInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/EventApprovalFlowInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/NotificationCenterInstrumentedTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/QRCodeHelperTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/EventTimeUtilsTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/EventShareHelperTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/ThemeManagerTest.java`

---

## Bottom Line

The project already has a respectable automated test base, especially for auth validation, payments, SOS, and contract-level UI checks. The biggest gap is feature-level behavior testing for the main attendee, organizer, and admin journeys. The best next move is to stabilize the failing local suite first, then add Espresso-based functional tests for splash, search, event detail, checkout, event creation, and approval flows.
