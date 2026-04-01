# Campus Event Discovery and Management Platform

This README is a developer guide for the entire repository. It is written for someone opening the project for the first time and trying to answer four questions quickly:

1. What does this app do?
2. How is it structured?
3. Which file owns which behavior?
4. What is finished, what is partial, and what should I be careful with?

The codebase is an Android app written in Java using classic Activities, Fragments, RecyclerView, Firebase Auth, Firestore, Firebase Storage, Material 3, and Glide. There is no backend service in this repository; the Android client talks directly to Firebase.

## Remaining Work

The core demo loop is implemented, but several features are still partial or missing. A new developer should know this before reading the rest of the guide.

### App-side features still left

- QR check-in is not finished. RSVP QR tokens exist, but there is no scanner/verification flow yet.
- Notifications are only partially done. Approval/rejection writes exist, but there is no in-app notification center screen yet.
- Memories/photos and ratings exist at the repository/model level, but not as full user-facing flows.
- Organizer attendee management is only partly done. Attendee loading works, but blacklist/moderation is still a placeholder.
- `ManageEventsActivity` is still basically a placeholder.
- The organizer proposal form is still simplified and does not collect the full richer event metadata you may eventually want.
- Payment is not real. The checkout flow simulates payment and then performs RSVP.

### Backend and infrastructure work still left

- Proposal approval is still client-driven, not Cloud-Function-driven.
- Google Calendar integration is partial and currently uses Android calendar intents, not full Google OAuth/calendar sync.
- `maintenanceMode` from `app_config/settings` is not enforced anywhere yet.
- Real Firebase sign-in/up still depends on Firebase console configuration; the screens exist, but the easiest testing path is still the local bypass flow.

## Current Product Shape

The app supports three roles:

- `attendee`: browse events, search, save favourites, RSVP, view calendar, manage profile
- `organizer`: browse events like an attendee, submit event proposals, inspect approved events
- `admin`: review and approve/reject organizer proposals

The app currently also supports a local role-bypass mode so the UI can be tested without Firebase sign-in.

## Tech Stack

- Language: Java 11 source/target
- UI: XML layouts, Material 3, RecyclerView
- Navigation: Activity launches plus fragment swaps inside `MainActivity`
- Data: Firebase Authentication, Firestore, Firebase Storage
- Imaging: Glide, CircleImageView
- Build: Gradle Kotlin DSL, Android Gradle Plugin 9.0.1, Gradle 9.1.0

## Quick Start

### Requirements

- Android Studio
- Android SDK
- Java runtime; the project has been run successfully with Android Studio's bundled JBR
- A valid Firebase project if you want real auth and live backend writes

### Environment setup

- `set-java-path.ps1` writes `JAVA_HOME` and appends the Android Studio JBR `bin` directory to the Windows user `PATH`
- `app/google-services.json` contains the Firebase Android app configuration used by this project
- `local.properties` is machine-specific and should point at your local Android SDK

### Build

From the repo root:

```bat
gradlew.bat :app:assembleDebug
```

If Java is not on `PATH`, run the PowerShell helper:

```powershell
.\set-java-path.ps1
```

### Run modes

- Real auth mode: uses `SignInActivity` / `SignUpActivity` and Firebase Auth
- Test mode: uses `TempLoginActivity` to enter as attendee, organizer, or admin without Firebase Auth

At the moment, the easiest way to exercise the app is Test Mode.

## Architecture At A Glance

### Startup flow

`SplashActivity` is the launcher activity. After a short delay it decides whether to open:

- `MainActivity` if a Firebase user already exists or local bypass mode is enabled
- `TempLoginActivity` otherwise

`WelcomeActivity`, `SignInActivity`, and `SignUpActivity` still exist, but the current unauthenticated default path goes through `TempLoginActivity`.

### App shell

`MainActivity` owns:

- the bottom navigation bar
- role resolution
- fragment caching
- role-specific home/action behavior

Primary fragments hosted by `MainActivity`:

- `HomeFragment`
- `HomeOrganizerFragment`
- `HomeAdminFragment`
- `SearchFragment`
- `FavouritesFragment`
- `ProfileFragment`
- `MyEventsFragment`
- `EventCalendarFragment`

### Data access pattern

There is one main repository: `EventRepository`.

It is responsible for:

- reading events and featured events
- search
- save/unsave
- RSVP/cancel RSVP
- organizer proposals
- admin approval/rejection
- profile reads and some profile updates
- notifications, ratings, memories, attendees, SOS writes

There is no dependency injection framework, no ViewModel layer, and no use-case layer. Screens create and call `EventRepository` directly.

### State management

State is a mix of:

- Firestore documents and subcollections
- local `SharedPreferences`
- in-memory screen state inside Activities and Fragments

Important local preference stores:

- `app_preferences`: managed by `ThemeManager`; stores dark mode
- `dev_session`: managed by `DevSessionManager`; stores bypass role
- `recently_viewed`: written by `EventDetailActivity`; stores recent event IDs for `MyEventsFragment`

### UI pattern

- XML layout per screen
- `findViewById` everywhere; no ViewBinding or DataBinding
- RecyclerView adapters for event cards and ticket tiers
- shared theme styles in `values/themes.xml` and `values-night/themes.xml`
- shared motion via window animation style plus `NavigationTransitions`

## How Classes Relate

This project is easiest to understand as five layers:

1. app startup and shell
2. role-specific screens
3. shared feature screens
4. repository and model layer
5. local utility/state helpers

### Startup and shell relations

- `CampusEventDiscoveryApp` initializes theme state before any screen is shown.
- `SplashActivity` decides whether the app enters authenticated flow or local test flow.
- `TempLoginActivity` writes the bypass identity through `DevSessionManager`.
- `WelcomeActivity`, `SignInActivity`, and `SignUpActivity` are the real-auth entry points.
- `MainActivity` is the runtime shell that decides which fragments exist for the current role and what the action tab should do.

### Screen-to-repository relations

- `HomeFragment`, `HomeOrganizerFragment`, `SearchFragment`, `FavouritesFragment`, `MyEventsFragment`, and `EventCalendarFragment` all read event data through `EventRepository`.
- `EventDetailActivity` reads one event through `EventRepository`, updates save state through `EventRepository`, and launches the ticket flow.
- `BuyTicketActivity` is mostly UI-only and launches `CheckoutActivity`.
- `CheckoutActivity` performs the actual call to `EventRepository.rsvpEvent(...)`.
- `CreateEventActivity` creates `EventProposal` objects and writes them through `EventRepository.proposeEvent(...)`.
- `HomeAdminFragment` reads pending proposals through `EventRepository.getAllPendingProposals(...)`.
- `EventApprovalActivity` reads one proposal through `EventRepository.getProposalById(...)` and resolves it through `approveProposal(...)` / `rejectProposal(...)`.
- `WhoIsComingActivity` reads `events/{eventId}/attendees` through `EventRepository.getEventAttendees(...)`.
- `ProfileFragment` and `AccountSettingsActivity` use `EventRepository` for user reads and profile updates.

### Adapter-to-model relations

- `EventAdapter` renders `Event` objects and is reused across home, search, favourites, my-events, and calendar flows.
- `OrganizerPendingAdapter` renders `EventProposal` objects for admin pending-approval lists.
- `TicketTierAdapter` renders its own local `TicketTier` model because ticket tiers are UI-defined, not Firestore-defined.
- `AttendeeAdapter` renders `EventAttendee` objects for organizer attendee management.

### Utility relations

- `UserRoles` centralizes role strings and role checks and is used across `MainActivity`, `ProfileFragment`, `EventDetailActivity`, `BuyTicketActivity`, and `CheckoutActivity`.
- `ThemeManager` is used by `CampusEventDiscoveryApp`, `ProfileFragment`, `SignInActivity`, and `SignUpActivity`.
- `DevSessionManager` is used anywhere the app needs to behave like a signed-in user without Firebase Auth, especially `SplashActivity`, `TempLoginActivity`, `MainActivity`, `ProfileFragment`, and the registration flow.
- `NavigationTransitions` is used where fragment replacements should feel visually consistent with shared activity animations.

### Firebase ownership boundaries

- `EventRepository` should be considered the owner of Firestore event/proposal/RSVP/attendee/report/notification operations.
- `SignUpActivity` still creates the initial Firestore user document directly because it must create the Firebase Auth user first.
- `ProfileFragment` owns the Firebase Storage upload for profile pictures, then delegates the Firestore `profilePicUrl` update back into `EventRepository`.

## Role-Based Behavior

### Attendee

- sees attendee home
- can search and favourite events
- can RSVP through Buy Ticket -> Checkout
- can access My Events and Event Calendar

### Organizer

- sees organizer home
- can browse events like attendee
- can create proposals through `CreateEventActivity`
- can open approved organizer events in `OrganizerEventDetailActivity`
- cannot register for events; attendee-only registration is enforced in event detail, buy-ticket, and checkout flows

### Admin

- sees admin home
- reviews pending proposals in `EventApprovalActivity`
- can approve or reject proposals

## Firestore Model

The intended Firestore design is documented in `docs/db/initial_db.txt`.

The main collections are:

- `users`
- `events`
- `event_proposals`
- `reports`
- `notifications`
- `app_config/settings`

The code now follows this structure closely for:

- user profiles
- event browsing
- saved events
- RSVPs and attendee documents
- proposals and approval review metadata
- reports
- notifications
- featured event configuration

Not everything from the design is fully surfaced in UI yet. The remaining gaps are mostly around backend-only behavior, Google Calendar token handling, maintenance mode, memories UI, and ratings UI.

### Database Alignment Notes

Some Firestore behavior in the app is intentionally pragmatic and should be understood before making backend changes:

- proposal approval is still materialized on the client in `EventRepository.approveProposal(...)`; there is no Cloud Function in this repo
- RSVP cancellation keeps the user RSVP document and changes its `status` to `cancelled` instead of deleting it
- calendar integration updates `addedToCalendar`, but the current Android intent flow cannot provide a real Google Calendar event ID
- SOS/report writes follow the expected map shape, but location values may be placeholders until a real location-permission flow exists
- organizer approval/rejection notifications are written directly into Firestore under `notifications/{userId}/messages`

## Main User Flows

### 1. App launch and role resolution

`SplashActivity` -> `MainActivity` or `TempLoginActivity`

- `TempLoginActivity` sets bypass role through `DevSessionManager`
- `MainActivity` loads the correct role-specific home fragment
- `ThemeManager` applies the current dark/light theme as early as possible

### 2. Attendee discovery flow

`HomeFragment` / `SearchFragment` / `FavouritesFragment` / `EventCalendarFragment` -> `EventDetailActivity`

- events are rendered through `EventAdapter`
- hearts save/unsave through `EventRepository`
- featured event cards use `item_event_card_large.xml`
- regular lists use `item_event_card.xml`

### 3. RSVP / ticket flow

`EventDetailActivity` -> `BuyTicketActivity` -> `CheckoutActivity`

- `BuyTicketActivity` builds a list of static ticket tiers
- `CheckoutActivity` collects fake payment/contact info
- successful checkout actually calls `EventRepository.rsvpEvent(...)`
- this is an RSVP flow dressed like a payment flow; it is not a real payment integration

### 4. Organizer proposal flow

`HomeOrganizerFragment` or bottom-nav action -> `CreateEventActivity`

- organizer enters title, date, category, venue, description, capacity, optional trailer URL
- `CreateEventActivity` creates an `EventProposal`
- repository writes the proposal into `event_proposals` with `status = pending`

### 5. Admin approval flow

`HomeAdminFragment` -> `EventApprovalActivity`

- admin loads the proposal through `EventRepository.getProposalById(...)`
- approve: repository marks proposal approved, creates a new `events` document on the client, and writes an organizer notification
- reject: repository updates proposal status/admin note and writes an organizer notification

### 6. Organizer attendee-management flow

`OrganizerEventDetailActivity` -> `WhoIsComingActivity`

- organizer opens attendee management from an approved event
- `WhoIsComingActivity` reads `events/{eventId}/attendees`
- attendee rows are rendered by `AttendeeAdapter` using the `EventAttendee` model
- blacklist selection is still a placeholder action; the attendee list itself is now live

### 7. Profile/settings flow

`ProfileFragment`

- loads user name/email/photo
- toggles dark mode through `ThemeManager`
- opens `MyEventsFragment`
- opens `EventCalendarFragment`
- opens `AccountSettingsActivity`
- logs out to `TempLoginActivity` or `WelcomeActivity`

## Known Architectural Realities

- This is not MVVM. Screen classes contain UI logic and call the repository directly.
- `EventRepository` is the center of gravity for backend logic.
- Firebase writes happen in the client.
- Several planned features are scaffolded but not wired to UI.
- Test mode is important because Firebase Auth can require extra console configuration.

## File-By-File Repository Tour

## Root Build and Tooling Files

- `README.md`: this developer guide.
- `build.gradle.kts`: root Gradle plugin declarations.
- `settings.gradle.kts`: repository and plugin resolution plus `:app` module inclusion; note the root project name is still the generic `My Application`.
- `gradle.properties`: standard Gradle and AndroidX flags.
- `gradlew`: Unix shell Gradle wrapper launcher.
- `gradlew.bat`: Windows Gradle wrapper launcher.
- `gradle/libs.versions.toml`: version catalog for AGP and a few Android test/support dependencies.
- `gradle/wrapper/gradle-wrapper.jar`: Gradle wrapper bootstrap JAR.
- `gradle/wrapper/gradle-wrapper.properties`: wrapper configuration; currently points at Gradle `9.1.0`.
- `set-java-path.ps1`: Windows helper that sets user `JAVA_HOME` and appends Android Studio JBR to user `PATH`.

## Documentation Files

- `docs/db/initial_db.txt`: intended Firestore schema and storage folder design; use this to compare implementation against the original data design.
- `docs/CRC_cards`: CRC analysis notes from the course/project planning phase.
- `docs/images/project_board_overview.png`: screenshot of the GitHub project board.
- `docs/images/open_issues.png`: screenshot of open issues.
- `docs/images/closed_issues.png`: screenshot of closed issues.
- `docs/images/Screenshot_2026-03-10_111125.png`: screenshot used for the user-story template in docs.
- `docs/images/Screens(1).png`: UI mockup / storyboard image set 1.
- `docs/images/Screens(2).png`: UI mockup / storyboard image set 2.
- `docs/images/Screens(3).png`: UI mockup / storyboard image set 3.
- `docs/images/RSVP.png`: RSVP storyboard image.
- `docs/images/create_event.png`: organizer create-event storyboard image.

## App Module Build and Config Files

- `app/build.gradle.kts`: Android app module config; sets namespace/application ID, SDK versions, Java version, Firebase dependencies, Material, RecyclerView, Glide, and CircleImageView.
- `app/google-services.json`: Firebase Android app configuration file; should match the Firebase project used for the build.
- `app/proguard-rules.pro`: default placeholder ProGuard/R8 rules; no app-specific keep rules are currently defined.
- `app/src/main/AndroidManifest.xml`: app manifest; registers `CampusEventDiscoveryApp`, all Activities, and sets `SplashActivity` as launcher.

## Java Source: App Entry and Shell

- `app/src/main/java/com/example/campuseventdiscovery/CampusEventDiscoveryApp.java`: application class; applies stored theme preference before any screen is shown.
- `app/src/main/java/com/example/campuseventdiscovery/SplashActivity.java`: launcher/splash activity; routes to `MainActivity` or `TempLoginActivity`.
- `app/src/main/java/com/example/campuseventdiscovery/TempLoginActivity.java`: developer/test bypass screen; lets you continue as attendee, organizer, or admin.
- `app/src/main/java/com/example/campuseventdiscovery/WelcomeActivity.java`: legacy welcome screen with Sign In and Create Account buttons.
- `app/src/main/java/com/example/campuseventdiscovery/SignInActivity.java`: Firebase email/password sign-in screen; syncs theme from Firestore after login.
- `app/src/main/java/com/example/campuseventdiscovery/SignUpActivity.java`: Firebase sign-up screen; creates the initial Firestore `users/{uid}` document.
- `app/src/main/java/com/example/campuseventdiscovery/MainActivity.java`: main shell; hosts bottom navigation, resolves user role, caches fragments, and routes the role-sensitive action tab.

## Java Source: Repository

- `app/src/main/java/com/example/campuseventdiscovery/repository/EventRepository.java`: single Firestore repository used across the app.

Repository responsibilities by method group:

- Event fetch: `getUpcomingEvents`, `getFeaturedEventIds`, `getFeaturedEvents`, `getPersonalisedEvents`, `getEventById`, `searchEvents`
- Save/RSVP: `saveEvent`, `unsaveEvent`, `getSavedEvents`, `rsvpEvent`, `cancelRsvp`, `getRsvps`, `markRsvpAddedToCalendar`
- Memories/ratings: `addMemory`, `addRating`
- Organizer/admin: `proposeEvent`, `getProposalById`, `getOrganizerEvents`, `getOrganizerProposals`, `getAllPendingProposals`, `approveProposal`, `rejectProposal`, `getEventAttendees`
- Reports/notifications: `sendSosReport`, `getNotifications`
- User profile: `getUserData`, `updateDarkMode`, `updateUserProfile`, `updateProfilePic`
- Internal helpers: document mapping, saved-event fallback mapping, ID chunking, ordered fetch reconstruction, and proposal-to-event conversion

Important note: `approveProposal(...)` currently copies a proposal into `events` directly from the Android client instead of using a Cloud Function. That matches current app behavior, but it is still an architectural limitation.

## Java Source: Models

- `app/src/main/java/com/example/campuseventdiscovery/model/User.java`: Firestore user model; includes profile, role, interests, dark mode, FCM token, Google Calendar token, and created-at.
- `app/src/main/java/com/example/campuseventdiscovery/model/Event.java`: Firestore event model; includes document ID helper, event metadata, capacity counts, organizer identity, verification, rating stats, and status.
- `app/src/main/java/com/example/campuseventdiscovery/model/EventProposal.java`: Firestore event proposal model; used before admin approval.
- `app/src/main/java/com/example/campuseventdiscovery/model/EventAttendee.java`: Firestore attendee model for `events/{eventId}/attendees/{userId}`.
- `app/src/main/java/com/example/campuseventdiscovery/model/Memory.java`: model for post-event memories/photos and rating; repository support exists but no dedicated screen currently uses it.
- `app/src/main/java/com/example/campuseventdiscovery/model/Notification.java`: model for per-user notifications; repository support exists but no notification UI is currently present.

## Java Source: Utilities

- `app/src/main/java/com/example/campuseventdiscovery/util/UserRoles.java`: central role constants and sanitization helpers.
- `app/src/main/java/com/example/campuseventdiscovery/util/ThemeManager.java`: stores and applies dark mode using `SharedPreferences` plus `AppCompatDelegate`.
- `app/src/main/java/com/example/campuseventdiscovery/util/DevSessionManager.java`: stores local bypass role and demo identity strings for test mode.
- `app/src/main/java/com/example/campuseventdiscovery/util/NavigationTransitions.java`: shared helper for fragment replace animations.

## Java Source: RecyclerView Adapters

- `app/src/main/java/com/example/campuseventdiscovery/adapter/EventAdapter.java`: generic event-card adapter used across home, search, favourites, calendar, and my-events screens. Also defines `OnEventClickListener` and `EventViewHolder`.
- `app/src/main/java/com/example/campuseventdiscovery/adapter/OrganizerPendingAdapter.java`: simplified adapter for pending proposal cards used on the admin home screen. Also defines `OnPendingClickListener` and `PendingViewHolder`.
- `app/src/main/java/com/example/campuseventdiscovery/adapter/TicketTierAdapter.java`: adapter for checkout tiers. Also defines `OnTotalChangedListener`, `TicketTier`, and `TierViewHolder`.
- `app/src/main/java/com/example/campuseventdiscovery/adapter/AttendeeAdapter.java`: organizer attendee-list adapter with search/filter support and local selection state for future moderation actions.

## Java Source: Attendee and Shared UI

- `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeFragment.java`: attendee home; loads welcome text, featured event, personalized/upcoming events, save state, and SOS action.
- `app/src/main/java/com/example/campuseventdiscovery/ui/search/SearchFragment.java`: text search plus category chips with debounce; uses repository search and event cards.
- `app/src/main/java/com/example/campuseventdiscovery/ui/favourites/FavouritesFragment.java`: loads saved events for the current user and lets the user unsave directly from the list.
- `app/src/main/java/com/example/campuseventdiscovery/ui/myevents/MyEventsFragment.java`: role-sensitive event bucket screen. Attendee sees RSVPs and recently viewed; organizer sees approved, pending, and rejected items.
- `app/src/main/java/com/example/campuseventdiscovery/ui/calendar/EventCalendarFragment.java`: calendar screen; loads upcoming events, filters them by selected day, and supports long-press add-to-calendar via Android calendar intent.
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventDetailActivity.java`: shared attendee-facing event detail screen; handles save/unsave, share, map, add-to-calendar, recent-history tracking, and attendee-only registration CTA logic.

## Java Source: Ticket and Approval UI

- `app/src/main/java/com/example/campuseventdiscovery/ui/event/BuyTicketActivity.java`: static ticket-tier picker; enforces attendee-only access and launches checkout.
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/CheckoutActivity.java`: contact/payment form; simulates checkout and confirms RSVP by calling the repository.
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventApprovalActivity.java`: admin proposal review screen with approve/reject actions.

## Java Source: Organizer UI

- `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeOrganizerFragment.java`: organizer home; mirrors attendee browsing behavior but adds a Create Event button.
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/CreateEventActivity.java`: organizer proposal form; creates pending `EventProposal` documents.
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/OrganizerEventDetailActivity.java`: organizer-facing event detail/management screen showing registration count and actions.
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/WhoIsComingActivity.java`: organizer attendee-list screen; loads attendee documents, supports local search/filter, and exposes a placeholder selection-based moderation action.
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/ManageEventsActivity.java`: placeholder activity with toolbar/back handling only; currently not the main organizer management flow.

## Java Source: Admin UI

- `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeAdminFragment.java`: admin dashboard showing pending proposals through `OrganizerPendingAdapter`.

## Java Source: Profile UI

- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/ProfileFragment.java`: profile screen; loads user identity, profile photo, dark mode, my-events/calendar/account settings shortcuts, and logout. Also handles test mode behavior and ripple-delay navigation.
- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/AccountSettingsActivity.java`: editable profile settings screen; loads and updates full name, university, and location.

## Layout Files

- `app/src/main/res/layout/activity_main.xml`: main app shell containing the fragment container and bottom navigation bar.
- `app/src/main/res/layout/activity_splash.xml`: splash screen layout.
- `app/src/main/res/layout/activity_temp_login.xml`: local test-mode role picker layout.
- `app/src/main/res/layout/activity_welcome.xml`: welcome/sign-in-or-create-account layout.
- `app/src/main/res/layout/activity_sign_in.xml`: sign-in form layout.
- `app/src/main/res/layout/activity_sign_up.xml`: sign-up form layout.
- `app/src/main/res/layout/fragment_home.xml`: attendee home fragment layout.
- `app/src/main/res/layout/fragment_home_organizer.xml`: organizer home fragment layout.
- `app/src/main/res/layout/fragment_home_admin.xml`: admin home fragment layout.
- `app/src/main/res/layout/fragment_search.xml`: search screen layout with chips and results list.
- `app/src/main/res/layout/fragment_favourites.xml`: favourites list layout.
- `app/src/main/res/layout/fragment_my_events.xml`: multi-section my-events/manage-events layout.
- `app/src/main/res/layout/fragment_event_calendar.xml`: calendar screen layout.
- `app/src/main/res/layout/fragment_profile.xml`: profile/settings screen layout.
- `app/src/main/res/layout/fragment_placeholder.xml`: generic placeholder fragment layout; currently not part of active navigation.
- `app/src/main/res/layout/activity_event_detail.xml`: attendee event detail layout.
- `app/src/main/res/layout/activity_buy_ticket.xml`: ticket-tier selection layout.
- `app/src/main/res/layout/activity_checkout.xml`: checkout form layout.
- `app/src/main/res/layout/activity_event_approval.xml`: admin proposal approval layout.
- `app/src/main/res/layout/activity_create_event.xml`: organizer create-event form layout.
- `app/src/main/res/layout/activity_organizer_event_detail.xml`: organizer event-management detail layout.
- `app/src/main/res/layout/activity_who_is_coming.xml`: organizer attendee list layout.
- `app/src/main/res/layout/activity_manage_events.xml`: placeholder manage-events layout.
- `app/src/main/res/layout/activity_account_settings.xml`: profile/account settings layout.
- `app/src/main/res/layout/item_event_card.xml`: standard event list card used by `EventAdapter` and `OrganizerPendingAdapter`.
- `app/src/main/res/layout/item_event_card_large.xml`: featured event card layout used on home screens.
- `app/src/main/res/layout/item_ticket_tier.xml`: row layout for ticket tiers in `BuyTicketActivity`.
- `app/src/main/res/layout/item_attendee.xml`: attendee row layout used by `AttendeeAdapter`.

## Theme, String, Menu, and Shared Resource Files

- `app/src/main/res/values/strings.xml`: all user-facing copy; many strings still reflect the app's prototype history.
- `app/src/main/res/values/arrays.xml`: category list used in organizer proposal creation.
- `app/src/main/res/values/colors.xml`: light-mode color tokens.
- `app/src/main/res/values-night/colors.xml`: dark-mode color tokens.
- `app/src/main/res/values/themes.xml`: main light/day-night theme, shared button/card styles, and shared activity window animations.
- `app/src/main/res/values-night/themes.xml`: night variant of the theme and component styles.
- `app/src/main/res/menu/bottom_nav_menu.xml`: bottom navigation items and base icons.
- `app/src/main/res/color/nav_item_color.xml`: selected/unselected color state list for bottom-nav items.
- `app/src/main/res/xml/backup_rules.xml`: default backup rules template; currently mostly untouched.
- `app/src/main/res/xml/data_extraction_rules.xml`: default Android 12+ backup/data extraction template.

## Animation Files

- `app/src/main/res/anim/screen_enter.xml`: shared forward-enter animation.
- `app/src/main/res/anim/screen_exit.xml`: shared forward-exit animation.
- `app/src/main/res/anim/screen_pop_enter.xml`: shared backward-enter animation.
- `app/src/main/res/anim/screen_pop_exit.xml`: shared backward-exit animation.

## Drawable Files

- `app/src/main/res/drawable/bg_badge_new.xml`: rounded background for NEW/PENDING badges.
- `app/src/main/res/drawable/bg_button_purple.xml`: older purple rounded button background.
- `app/src/main/res/drawable/bg_card_rounded.xml`: rounded surface card background.
- `app/src/main/res/drawable/bg_chip_selected.xml`: selected chip background.
- `app/src/main/res/drawable/bg_chip_unselected.xml`: unselected chip background.
- `app/src/main/res/drawable/bg_featured_overlay.xml`: gradient overlay used on featured cards.
- `app/src/main/res/drawable/bg_placeholder_image.xml`: neutral placeholder block used when event images are missing.
- `app/src/main/res/drawable/bg_search_bar.xml`: search field background.
- `app/src/main/res/drawable/ic_add.xml`: add/create icon.
- `app/src/main/res/drawable/ic_back.xml`: back arrow icon.
- `app/src/main/res/drawable/ic_calendar.xml`: calendar icon.
- `app/src/main/res/drawable/ic_heart_filled.xml`: filled heart icon.
- `app/src/main/res/drawable/ic_heart_outline.xml`: outlined heart icon.
- `app/src/main/res/drawable/ic_home.xml`: home icon for bottom nav.
- `app/src/main/res/drawable/ic_launcher_background.xml`: launcher icon background vector.
- `app/src/main/res/drawable/ic_launcher_foreground.xml`: launcher icon foreground vector.
- `app/src/main/res/drawable/ic_location.xml`: location icon.
- `app/src/main/res/drawable/ic_location_pin.xml`: map pin icon.
- `app/src/main/res/drawable/ic_person.xml`: profile/person icon.
- `app/src/main/res/drawable/ic_pin.xml`: pin/action icon.
- `app/src/main/res/drawable/ic_search.xml`: search icon.
- `app/src/main/res/drawable/ic_share.xml`: share icon.
- `app/src/main/res/drawable/ic_verified.xml`: verified/admin icon.

## Launcher Icon Assets

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: adaptive launcher icon definition.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`: adaptive round launcher icon definition.
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`: mdpi launcher icon.
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`: mdpi round launcher icon.
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`: hdpi launcher icon.
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`: hdpi round launcher icon.
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`: xhdpi launcher icon.
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`: xhdpi round launcher icon.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`: xxhdpi launcher icon.
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`: xxhdpi round launcher icon.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`: xxxhdpi launcher icon.
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`: xxxhdpi round launcher icon.

## Test Files

- `app/src/test/java/com/example/campuseventdiscovery/ExampleUnitTest.java`: default sample local unit test; not meaningful to app behavior.
- `app/src/androidTest/java/com/example/campusevent/ExampleInstrumentedTest.java`: default sample instrumented test, but it is stale and still asserts the wrong package name.

## How The Major Screens Work Together

### `MainActivity` fragment map

- home tab -> attendee home, organizer home, or admin home
- search tab -> `SearchFragment`
- action tab -> organizer create-event screen, admin approvals, or attendee my-events
- favourites tab -> `FavouritesFragment`
- profile tab -> `ProfileFragment`

### Event browsing screens

The shared event-browsing stack is:

- `HomeFragment`
- `HomeOrganizerFragment`
- `SearchFragment`
- `FavouritesFragment`
- `MyEventsFragment`
- `EventCalendarFragment`

All of these either use `EventAdapter` directly or open `EventDetailActivity` for event details.

### Organizer-specific screens

The organizer-specific stack is:

- `HomeOrganizerFragment`
- `CreateEventActivity`
- `MyEventsFragment` in organizer mode
- `OrganizerEventDetailActivity`
- `WhoIsComingActivity`

### Admin-specific screens

The admin-specific stack is:

- `HomeAdminFragment`
- `EventApprovalActivity`

## Important SharedPreferences Keys

- `app_preferences.dark_mode_enabled`: dark mode preference from `ThemeManager`
- `dev_session.bypass_enabled`: whether local test mode is enabled
- `dev_session.role`: current bypass role
- `recently_viewed.event_ids`: comma-separated recent event IDs

## Important Firebase Collections Used In Code

- `users/{uid}`
- `users/{uid}/saved_events/{eventId}`
- `users/{uid}/rsvps/{eventId}`
- `users/{uid}/memories/{memoryId}`
- `events/{eventId}`
- `events/{eventId}/ratings/{userId}`
- `events/{eventId}/attendees/{userId}`
- `event_proposals/{proposalId}`
- `reports/{reportId}`
- `notifications/{userId}/messages/{notificationId}`
- `app_config/settings`

## What Is Implemented vs What Is Scaffolded

### Implemented enough to demo

- role-based home screens
- event discovery and search
- save/favourite flow
- recent-history tracking
- RSVP flow
- organizer proposal submission
- admin proposal approval/rejection
- organizer attendee list loading
- approval/rejection notification writes
- schema-aligned report/attendee/RSVP writes
- dark mode persistence
- local test-mode role bypass

### Present in code but partial or placeholder

- `ManageEventsActivity`: placeholder only
- `Memory` model and `addMemory(...)`: no screen uses them
- `Notification` model and `getNotifications(...)`: no notification screen uses them
- `addRating(...)`: no rating UI currently calls it
- attendee moderation/blacklist action in `WhoIsComingActivity`: selection UI exists, but no real backend moderation flow is implemented
- Google Calendar token handling exists in the schema/model, but the UI still relies on Android calendar intents rather than Google Calendar API integration
- `app_config.settings.maintenanceMode`: documented in Firestore design but not consumed by the Android app yet

### Known rough edges

- Firebase email/password auth may require Firebase console configuration before it works reliably on device
- `SplashActivity` currently sends unauthenticated users to `TempLoginActivity`, not `WelcomeActivity`
- `CheckoutActivity` simulates payment but only performs RSVP registration
- `EventRepository.approveProposal(...)` performs event creation and organizer notification writes on the client rather than through backend automation
- the instrumented test package assertion is stale
- some strings still refer to older phase/placeholder naming
- `gcalEventId` is present in RSVP data, but current calendar integration cannot supply a real Google Calendar event ID

## Suggested Reading Order For New Developers

If you want to understand the app quickly, read files in this order:

1. `app/src/main/java/com/example/campuseventdiscovery/SplashActivity.java`
2. `app/src/main/java/com/example/campuseventdiscovery/TempLoginActivity.java`
3. `app/src/main/java/com/example/campuseventdiscovery/MainActivity.java`
4. `app/src/main/java/com/example/campuseventdiscovery/repository/EventRepository.java`
5. `app/src/main/java/com/example/campuseventdiscovery/model/Event.java`
6. `app/src/main/java/com/example/campuseventdiscovery/model/User.java`
7. `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeFragment.java`
8. `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventDetailActivity.java`
9. `app/src/main/java/com/example/campuseventdiscovery/ui/event/BuyTicketActivity.java`
10. `app/src/main/java/com/example/campuseventdiscovery/ui/event/CheckoutActivity.java`
11. `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/CreateEventActivity.java`
12. `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventApprovalActivity.java`
13. `app/src/main/java/com/example/campuseventdiscovery/ui/profile/ProfileFragment.java`

## Practical Debug Tips

- If auth is failing, use `TempLoginActivity` and test the rest of the app in bypass mode.
- If role-based behavior looks wrong, check `UserRoles`, `DevSessionManager`, and the role resolution in `MainActivity`.
- If a Firestore-backed screen is empty, start by checking the relevant repository method in `EventRepository`.
- If theme behavior looks wrong, start with `CampusEventDiscoveryApp`, `ThemeManager`, and the day/night `themes.xml` files.
- If a tap feels inconsistent, check both the layout ripple/background and whether the screen change happens too quickly to show feedback.

## Summary

This project is a single-module Android app with one repository class, a role-based main shell, and feature screens split across attendee, organizer, admin, event, calendar, and profile packages. The current code is good enough to demo the core product loop, but several advanced features are still stubbed or only partially wired. When in doubt, start from `MainActivity`, follow the fragment or activity launch path, and then trace backend operations through `EventRepository`.
