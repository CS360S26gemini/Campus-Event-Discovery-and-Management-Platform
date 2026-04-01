# Campus Event Discovery Prototype

This repository contains an Android prototype for discovering campus events, submitting organizer proposals, approving events, registering attendees, and managing profile, notification, memory, and SOS flows.

The app is written in Java with XML layouts and talks directly to Firebase Authentication, Firestore, Firebase Storage, and Firebase App Check. This README documents the code as it exists in this repository on March 31, 2026.

## Prototype Status

### What this prototype already has

- A single Android app module with real Firebase Auth sign-in/sign-up plus a local developer-bypass mode for attendee, organizer, and admin roles.
- Startup routing through `SplashActivity`, including maintenance-mode checks from `app_config/settings`.
- A role-aware shell in `MainActivity` with bottom navigation and separate attendee, organizer, and admin home experiences.
- Attendee flows for featured/upcoming/personalized discovery, search, favourites, event detail, RSVP, recent events, notifications, memories, feedback, profile management, and SOS reporting.
- Organizer flows for event proposal submission, viewing approved/pending/rejected items, opening approved event details, managing attendees, checking people in by code, blacklisting attendees, and sending announcements.
- Admin flows for viewing pending proposals and approving or rejecting them with a note.
- Firestore-backed repository logic for events, proposals, saved events, RSVPs, attendee records, check-in, blacklist records, ratings, memories, notifications, SOS reports, and profile updates.
- Firebase Storage uploads for profile pictures and attendee memory photos.
- Theme persistence through local preferences plus Firestore dark-mode sync for signed-in users.
- Shared adapters, model classes, and XML resources for the main UI flows.

### What still has to be implemented before this becomes a final working product

Some of the items below are direct code gaps, and some are production-readiness work implied by the current architecture.

#### Missing or incomplete product features

- Real payments and ticketing are not implemented. `BuyTicketActivity` uses hard-coded ticket tiers and `CheckoutActivity` simply collects form data and creates an RSVP.
- Real QR code generation and scanning are not implemented. Attendees currently see a raw token string, and organizers manually type that token into `WhoIsComingActivity`.
- Event media management is incomplete. `CreateEventActivity` collects metadata only, and approved events are created with an empty `thumbnailUrl`.
- Event editing, cancellation, archival, completion, and richer organizer lifecycle management do not exist yet.
- Event start/end scheduling is incomplete. Organizers only choose a date, and `EventRepository` auto-generates a default 2-hour end time when approving a proposal.
- The calendar experience is not a true account sync. `EventCalendarFragment` shows all upcoming active events and uses Android calendar insert intents rather than real Google Calendar synchronization.
- Admin functionality is narrow. Admins can review proposals, but there is no admin UI for SOS reports, featured-event curation, maintenance toggles, report resolution, or broader moderation.
- Push notifications and reminders are not implemented. The current notification center reads Firestore documents only.
- Search and recommendations are still prototype-grade. Search fetches active events and filters client-side, and personalization only uses up to 10 interest tags because of Firestore query limits.
- Feedback and memory submission are UI-driven flows; stronger backend validation is still needed if only verified attendees should be allowed to submit them.

#### Backend, security, and integration work still needed

- Privileged operations should be moved behind strict Firebase Security Rules and/or Cloud Functions. Proposal approval, rejection, blacklist, announcements, and attendee check-in are currently initiated by the Android client.
- Production Firestore indexes and security rules need to be defined, tested, and documented.
- FCM token collection and a server-side notification pipeline still need to be added if real push notifications are required.
- If real Google Calendar sync is a requirement, OAuth, token refresh, and event update/removal handling still need to be built.
- Development and production Firebase environments should be separated more cleanly.

#### Quality and release-readiness work still needed

- Automated testing is extremely light. The repo currently has one simple unit test and one simple instrumented test.
- CI/CD, linting, static analysis, release signing, versioning, and deployment automation still need to be added or hardened.
- Accessibility, localization, error handling, loading-state polish, analytics, and crash reporting still need more work.
- The app currently uses direct screen-to-repository calls with no ViewModel or DI layer. That is workable for a prototype, but a larger product will need stronger state-management and dependency boundaries.

## Quick Start

### Requirements

- Android Studio
- Android SDK
- Java runtime
- A Firebase project if you want to use real authentication and live backend data

### Important setup files

- `app/google-services.json`: Firebase Android app configuration for this project
- `local.properties`: local Android SDK path
- `set-java-path.ps1`: helper script that sets `JAVA_HOME` and updates the user `PATH` for Android Studio's bundled JBR on Windows

### Build

From the repo root:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat test
```

If Java is not on `PATH`, run:

```powershell
.\set-java-path.ps1
```

### Run modes

- Real auth mode: use `WelcomeActivity`, `SignInActivity`, and `SignUpActivity` with Firebase Auth
- Developer-bypass mode: choose attendee, organizer, or admin from the role picker and enter the app without Firebase sign-in

## Codespace / Repository Map

```text
Prototype/
|- app/                         Android application module
|  |- build.gradle.kts          App-level Android and Firebase dependencies
|  \- src/
|     |- main/
|     |  |- java/com/example/campuseventdiscovery/
|     |  |  |- adapter/         RecyclerView adapters
|     |  |  |- model/           Firestore/domain models
|     |  |  |- repository/      Firestore access layer
|     |  |  |- ui/              Fragments and activities grouped by feature
|     |  |  \- util/            Theme, role, bypass, and navigation helpers
|     |  |- res/                Layouts, drawables, strings, arrays, themes, animations
|     |  \- AndroidManifest.xml Activity declarations and launcher setup
|     |- test/                  Local unit tests
|     \- androidTest/           Instrumented tests
|- docs/                        Supporting docs, screenshots, schema notes, CRC cards
|- .github/ISSUE_TEMPLATE/      GitHub issue template(s)
|- gradle/                      Wrapper and version catalog
|- build.gradle.kts             Root Gradle config
|- settings.gradle.kts          Project settings
|- set-java-path.ps1            Windows Java helper
\- README.md                    This file
```

Generated or local-only folders such as `build/`, `.gradle/`, and `.idea/` are not the source of truth for the app logic.

## Tech Stack and Architecture

### Stack

- Language: Java 11
- UI: XML layouts, Material Components, RecyclerView, CardView
- Navigation: Activities plus fragment swaps inside `MainActivity`
- Backend services: Firebase Auth, Firestore, Storage, App Check
- Image loading: Glide
- Misc UI dependency: CircleImageView
- Build: Gradle Kotlin DSL, Android Gradle Plugin 9.0.1

### Architecture style

- One Android app module
- `Activity` and `Fragment` based UI
- `findViewById` rather than ViewBinding/DataBinding
- One main repository class: `EventRepository`
- No ViewModel layer
- No dependency injection framework
- Local state in `SharedPreferences` plus in-memory screen state

### Important local state stores

- `app_preferences`: dark-mode preference, owned by `ThemeManager`
- `dev_session`: developer bypass role/session, owned by `DevSessionManager`
- `recently_viewed`: comma-separated event IDs written by `EventDetailActivity`

## Runtime Flow

### Startup and auth flow

1. `CampusEventDiscoveryApp` initializes Firebase, configures App Check, and applies the stored theme.
2. `SplashActivity` waits briefly, checks developer bypass, then checks maintenance mode from Firestore.
3. If maintenance mode is off, the app goes to `MainActivity` for signed-in users or `WelcomeActivity` for signed-out users.
4. If maintenance mode is on, admins can still enter `MainActivity`; non-admins are routed to `MaintenanceActivity`.
5. Signed-out users can sign in, sign up, or use developer bypass from `WelcomeActivity`, `SignInActivity`, or `SignUpActivity`.

### Main shell

`MainActivity` is the runtime shell for the app. It:

- resolves the current role from Firestore or developer bypass
- updates the bottom-nav action tab based on that role
- caches fragments in memory
- swaps fragments with `NavigationTransitions`
- supports reopening the profile tab via the `OPEN_TAB` intent extra

### Role behavior

| Role | Main behavior |
| --- | --- |
| Attendee | Browse featured/upcoming events, search, favourite, RSVP, view recent events, submit feedback, open memories and notifications, send SOS |
| Organizer | Browse events, create proposals, view approved events, view proposal status, manage attendees, check people in, blacklist, announce |
| Admin | Review pending proposals and approve or reject them |

## Firebase Data and Storage Model

The canonical schema notes live in `docs/db/initial_db.txt`. The current code actively uses the following collections and subcollections.

| Location | Purpose |
| --- | --- |
| `users/{userId}` | User profile, role, preferences, interests, optional profile picture URL |
| `users/{userId}/saved_events` | Saved/favourited events |
| `users/{userId}/rsvps` | RSVP status, QR token, calendar metadata |
| `users/{userId}/memories` | Post-event memories and ratings snapshot |
| `events/{eventId}` | Live approved events |
| `events/{eventId}/ratings` | Per-user event ratings and reviews |
| `events/{eventId}/attendees` | Organizer-facing attendee records and check-in state |
| `events/{eventId}/blacklist` | Per-event blacklist created by organizers |
| `event_proposals/{proposalId}` | Organizer-submitted proposals pending admin review |
| `reports/{reportId}` | SOS reports |
| `notifications/{userId}/messages` | In-app notifications shown in the notification center |
| `app_config/settings` | Global flags such as `maintenanceMode` and `featuredEventIds` |

### Firebase Storage usage in current code

- `profile_pictures/{userId}.jpg`: uploaded from `ProfileFragment`
- `memories/{userId}/{eventId}/{uuid}.jpg`: uploaded from `EventFeedbackActivity`

The schema document also describes `event_thumbnails/` and `event_trailers/`, but this prototype does not yet have organizer upload flows that write those folders.

## Package and Class Reference

### Core app package

| Class | Type | Responsibility |
| --- | --- | --- |
| `CampusEventDiscoveryApp` | `Application` | Initializes Firebase, configures App Check for debug/release, and applies the saved theme before any screen loads. |
| `SplashActivity` | Activity | Launcher activity; checks developer bypass, maintenance mode, auth state, and routes to the next screen. |
| `WelcomeActivity` | Activity | Signed-out landing screen with sign-in, sign-up, and developer-bypass entry points. |
| `SignInActivity` | Activity | Firebase email/password sign-in flow, then theme sync and navigation into `MainActivity`. |
| `SignUpActivity` | Activity | Creates Firebase Auth users and their Firestore `users/{uid}` documents. |
| `MainActivity` | Activity | Bottom-nav host, fragment cache, role-aware home selection, and action-tab behavior. |
| `MaintenanceActivity` | Activity | Minimal maintenance screen with a retry button back to `SplashActivity`. |

### Utility package

| Class | Type | Responsibility |
| --- | --- | --- |
| `UserRoles` | Utility class | Central role constants and sanitization/check helpers for attendee, organizer, and admin. |
| `ThemeManager` | Utility class | Stores and applies dark-mode preference using `SharedPreferences` and `AppCompatDelegate`. |
| `DevSessionManager` | Utility class | Stores local developer-bypass state, role, fake user ID, and display identity. |
| `DevBypassHelper` | Utility class | Opens the developer role-picker dialog and launches `MainActivity` in bypass mode. |
| `NavigationTransitions` | Utility class | Wraps fragment replacement and shared custom animations. |

### Model package

| Class | Type | Responsibility |
| --- | --- | --- |
| `Event` | Model | Firestore-backed event document model with metadata, counts, ratings, and organizer info. |
| `EventProposal` | Model | Firestore-backed organizer proposal model used before an event becomes live. |
| `EventAttendee` | Model | Organizer-facing attendee record with user ID, name, QR token, and check-in state. |
| `User` | Model | Firestore user profile model with role, profile fields, interests, dark mode, and optional calendar token map. |
| `Memory` | Model | Post-event memory record with title, photo URLs, attended timestamp, and rating. |
| `Notification` | Model | In-app notification model with title, body, type, event link, read flag, and timestamp. |

### Repository package

| Class | Type | Responsibility |
| --- | --- | --- |
| `EventRepository` | Repository/service | Central Firestore access layer. Handles event reads, featured events, maintenance mode, search, save/unsave, RSVP/cancel RSVP, memories, ratings, organizer proposals, admin approval/rejection, attendee management, SOS reports, notifications, profile updates, and calendar metadata. |

Important `EventRepository` method groups:

- Event discovery: `getUpcomingEvents`, `getFeaturedEventIds`, `getFeaturedEvents`, `getPersonalisedEvents`, `getEventById`, `searchEvents`
- User actions: `saveEvent`, `unsaveEvent`, `getSavedEvents`, `rsvpEvent`, `cancelRsvp`, `getRsvps`, `getRsvpQrToken`
- Memories and ratings: `addMemory`, `getMemories`, `addRating`
- Organizer/admin flows: `proposeEvent`, `getProposalById`, `getOrganizerEvents`, `getOrganizerProposals`, `getAllPendingProposals`, `approveProposal`, `rejectProposal`, `getEventAttendees`, `blacklistAttendees`, `checkInAttendeeByQrToken`, `sendAnnouncementToAttendees`
- Profile/support flows: `getUserData`, `updateDarkMode`, `updateUserProfile`, `updateProfilePic`, `markRsvpAddedToCalendar`, `sendSosReport`, `getNotifications`, `markNotificationRead`

### Adapter package

| Class | Type | Responsibility |
| --- | --- | --- |
| `EventAdapter` | `RecyclerView.Adapter` | Shared event-card adapter used across home, search, favourites, my events, and calendar screens. Contains `EventViewHolder`. |
| `OrganizerPendingAdapter` | `RecyclerView.Adapter` | Reuses the event-card layout for pending/rejected/approved organizer proposal cards. Contains `PendingViewHolder`. |
| `TicketTierAdapter` | `RecyclerView.Adapter` | Checkout ticket-tier selector with quantity controls and total-price callbacks. Contains nested `TicketTier` and `TierViewHolder`. |
| `AttendeeAdapter` | `RecyclerView.Adapter` | Organizer attendee list adapter with filtering and multi-select for blacklist actions. Contains `AttendeeViewHolder`. |
| `MemoryAdapter` | `RecyclerView.Adapter` | Renders attendee memories with cover image, rating, and photo count. Contains `MemoryViewHolder`. |
| `NotificationAdapter` | `RecyclerView.Adapter` | Renders in-app notifications and unread state. Contains `NotificationViewHolder`. |

### Discovery and role-home UI

| Class | Type | Responsibility |
| --- | --- | --- |
| `HomeFragment` | Fragment | Attendee home screen with welcome header, featured event banner, personalized/upcoming feed, save/share actions, and SOS reporting. |
| `HomeOrganizerFragment` | Fragment | Organizer home screen with the same discovery/feed ideas plus a shortcut into event creation. |
| `HomeAdminFragment` | Fragment | Admin dashboard that lists pending proposals and opens `EventApprovalActivity`. |
| `SearchFragment` | Fragment | Debounced text search with category chips and save/unsave support. |
| `FavouritesFragment` | Fragment | Loads and displays the current user's saved events. |
| `MyEventsFragment` | Fragment | Role-dependent screen. Attendees see RSVPs and recently viewed events; organizers see approved events plus pending/rejected proposals. |
| `EventCalendarFragment` | Fragment | Month-based calendar of upcoming active events with per-day filtering and Android calendar insertion on long-press. |

### Event and approval UI

| Class | Type | Responsibility |
| --- | --- | --- |
| `EventDetailActivity` | Activity | Full event detail screen with save/share, map intent, calendar intent, recently-viewed tracking, and attendee-only registration CTA. |
| `BuyTicketActivity` | Activity | Shows hard-coded ticket tiers, computes totals, and launches checkout. |
| `CheckoutActivity` | Activity | Collects attendee name and fake payment selection, then creates the RSVP through `EventRepository`. |
| `EventFeedbackActivity` | Activity | Lets attendees submit a star rating, review text, and optional photos; saves both rating and memory records. |
| `EventApprovalActivity` | Activity | Admin review page for a proposal with approve/reject actions. |
| `OrganizerProposalDetailActivity` | Activity | Read-only organizer view for pending, approved, or rejected proposal details and admin notes. |

### Organizer management UI

| Class | Type | Responsibility |
| --- | --- | --- |
| `CreateEventActivity` | Activity | Organizer proposal form for title, date, category, venue, description, capacity, tags, sponsors, food stalls, and trailer URL. |
| `ManageEventsActivity` | Activity | Organizer screen that groups approved events, pending proposals, and rejected proposals into separate lists. |
| `OrganizerEventDetailActivity` | Activity | Organizer-facing live-event dashboard with registration counts, attendee access, and announcement sending. |
| `WhoIsComingActivity` | Activity | Organizer attendee-management screen with participant search, manual check-in by token, and blacklist actions. |

### Profile and account UI

| Class | Type | Responsibility |
| --- | --- | --- |
| `ProfileFragment` | Fragment | Role-aware profile hub with theme toggle, profile photo upload, logout, and navigation to related account screens. |
| `NotificationCenterActivity` | Activity | Lists Firestore-backed in-app notifications and opens related event details when tapped. |
| `MemoriesActivity` | Activity | Lists the current user's saved memory records. |
| `AccountSettingsActivity` | Activity | Lets signed-in users update profile fields such as full name, university, location, and interests. |

### Test classes

| Class | Type | Responsibility |
| --- | --- | --- |
| `ExampleUnitTest` | Local unit test | Verifies basic `UserRoles` sanitization and role checks. |
| `ExampleInstrumentedTest` | Instrumented test | Verifies the package name from app context. |

## Resource and XML Overview

The Java classes above map closely to XML resources under `app/src/main/res`.

- `layout/`: one XML screen per activity/fragment plus reusable list items like `item_event_card`, `item_notification`, `item_memory`, `item_attendee`, and `item_ticket_tier`
- `menu/bottom_nav_menu.xml`: bottom navigation for `MainActivity`
- `anim/`: shared fragment/activity transition animations
- `values/strings.xml`: nearly all UI copy
- `values/arrays.xml`: event category list used by `CreateEventActivity`
- `values/colors.xml`, `values/themes.xml`, `values-night/`: theming
- `drawable/`: icons, chips, badges, cards, and placeholder graphics

## Other Source Files Worth Knowing

| Path | Why it matters |
| --- | --- |
| `docs/db/initial_db.txt` | Most complete written description of the intended Firestore schema |
| `docs/github_4_stage_breakdown.md` | High-level staging/history summary for the major feature batches |
| `docs/images/` | Screenshots and project-board images |
| `docs/CRC_cards/` | Design documentation artifact(s) for CRC analysis |
| `docs/meeting-minutes/` | Placeholder folder for meeting notes |
| `.github/ISSUE_TEMPLATE/user_story.md` | GitHub issue template |
| `gradle/libs.versions.toml` | Central dependency/version catalog |
| `settings.gradle.kts` | Project/module settings |
| `build.gradle.kts` and `app/build.gradle.kts` | Build configuration and dependency definitions |

## Key Observations About The Current Codebase

- The app is already beyond a wireframe. Most major screens exist and are connected to Firebase-backed data.
- The repository layer is the real center of the project. Almost every screen calls `EventRepository` directly.
- The attendee experience is the broadest completed slice.
- Organizer and admin flows work, but they still rely heavily on client-side trust and prototype shortcuts.
- The biggest gap between "prototype" and "final product" is not the number of screens. It is the missing production-grade backend rules, integrations, validation, and test coverage.
