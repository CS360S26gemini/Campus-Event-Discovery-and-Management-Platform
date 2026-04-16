# Campus Event Discovery Prototype

This repository contains an Android prototype for discovering campus events, submitting organizer proposals, approving events, registering attendees, and managing profile, notification, memory, and SOS flows.

The app is written in Java with XML layouts and talks directly to Firebase Authentication, Firestore, Firebase Storage, and Firebase App Check. This README documents the code as it exists in this repository.

## Prototype Status

### What this prototype already has

- A single Android app module with real Firebase Auth sign-in/sign-up plus a local developer-bypass mode for attendee, organizer, and admin roles.
- Startup routing through `SplashActivity`, including maintenance-mode checks from `app_config/settings`.
- A role-aware shell in `MainActivity` with bottom navigation and separate attendee, organizer, and admin home experiences.
- Attendee flows for featured/upcoming/personalized discovery, search, favourites, event detail, RSVP, recent events, notifications, memories, feedback, profile management, and SOS reporting.
- **Ticketing & Payments:** A complete flow from selecting ticket tiers in `BuyTicketActivity` to processing mock payments in `CheckoutActivity`.
- **Digital Tickets:** Generation of actual QR codes in `TicketActivity` using `QRCodeHelper` for event check-ins.
- **Organizer flows:** Event proposal submission with image selection, viewing approved/pending/rejected items, opening approved event details, managing attendees, checking people in via a simulated QR scanner (token entry), blacklisting attendees, and sending announcements.
- **Admin flows:** Viewing pending proposals and approving or rejecting them with a note.
- **Firestore-backed repository logic:** For events, proposals, saved events, RSVPs, attendee records, check-in, blacklist records, ratings, memories, notifications, SOS reports, and profile updates.
- **Firebase Storage uploads:** For profile pictures and attendee memory photos.
- **Theme persistence:** Through local preferences plus Firestore dark-mode sync for signed-in users.
- **Personalized Calendar:** `EventCalendarFragment` displays only the user's RSVP'd events, serving as a personal schedule.

### What still has to be implemented before this becomes a final working product

#### Remaining product gaps

- Real payment gateway integration (e.g., Stripe, Braintree) is not implemented. `CheckoutActivity` currently uses a `MockPaymentService`.
- Real-time QR scanning via camera is not implemented. Organizers currently "scan" by manually entering or pasting the attendee's token in `WhoIsComingActivity`.
- Advanced event media management. While `CreateEventActivity` allows image selection, a full media lifecycle (editing, multiple photos, video processing) is still needed.
- Event editing, cancellation, archival, and richer organizer lifecycle management.
- The calendar experience uses Android calendar insert intents for external sync; a deep, two-way Google Calendar API integration is not yet built.
- Admin functionality for SOS reports, featured-event curation, maintenance toggles, and report resolution is not yet fully exposed in the UI.
- Push notifications are currently Firestore-document based; FCM-based real push notifications still need to be integrated.

#### Backend, security, and integration work still needed

- **Firestore Security Rules:** Hardening for all collections (users, saved_events, rsvps, memories, notifications, proposals, etc.) to prevent unauthorized access or role escalation.
- **Firestore Indexes:** Required for complex query patterns (status, date, organizerId, etc.).
- **Cloud Functions:** Moving privileged operations (proposal approval, blacklist, announcements) to a trusted server environment.
- **FCM Token Pipeline:** Collection of tokens and a server-side notification trigger.
- **Validation:** Stronger server-side validation for ratings, memories, and attendee actions.

## Quick Start

### Requirements

- Android Studio
- Android SDK
- Java 17 (recommended for current build tools)
- A Firebase project with Auth, Firestore, and Storage enabled

### Important setup files

- `app/google-services.json`: Firebase Android app configuration
- `local.properties`: local Android SDK path
- `set-java-path.ps1`: helper script for Windows users to set `JAVA_HOME`

### Build & Run

From the repo root:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew test
```

## Codespace / Repository Map

```text
Prototype/
|- app/                         Android application module
|  |- src/
|     |- main/
|     |  |- java/com/example/campuseventdiscovery/
|     |  |  |- adapter/         RecyclerView adapters
|     |  |  |- model/           Firestore/domain models (Event, User, Rsvp, etc.)
|     |  |  |- repository/      Firestore access layer (EventRepository, PaymentRepository)
|     |  |  |- ui/              Features (Discovery, Organizer, Admin, Profile)
|     |  |  \- util/            Helpers (Theme, Role, QR, MockPayment)
|     |  |- res/                Layouts, themes, animations
|     |  \- AndroidManifest.xml Launcher and activity setup
|- docs/                        Schema notes, CRC cards, screenshots
|- README.md                    This file
```

## Tech Stack

- **Language:** Java 11/17
- **UI:** XML, Material Design, Glide (images), CircleImageView
- **Backend:** Firebase (Auth, Firestore, Storage, App Check)
- **Utilities:** ZXing (QR Code generation)

## Important Data Collections

| Collection | Purpose |
| --- | --- |
| `users` | Profiles, roles, and interests |
| `events` | Approved live events (includes `ticketPrice`) |
| `rsvps` | User registrations with `qrPayload` and `transactionId` |
| `event_proposals` | Draft events waiting for Admin review |
| `payments` | Records of (mock) transactions |
| `notifications` | In-app notification messages |
| `reports` | SOS alerts and security reports |
