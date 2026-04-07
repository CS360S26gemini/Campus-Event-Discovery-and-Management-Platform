# Campus Event Discovery and Management Platform

> A centralised Android application enabling students to discover, filter, and RSVP to campus events while providing organisers with proposal, attendee management, and QR-based check-in tools, and administrators with a full approval and moderation dashboard.

**Platform:** Android (Java) · **Backend:** Firebase (Auth, Firestore, Storage, App Check) · **Min SDK:** 24 (Android 7.0) · **Target SDK:** 34 · **Compile SDK:** 36

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Prerequisites](#2-prerequisites)
3. [Installation & Setup](#3-installation--setup)
   - 3.1 [Clone the Repository](#31-clone-the-repository)
   - 3.2 [Firebase Configuration](#32-firebase-configuration)
   - 3.3 [Build & Run](#33-build--run)
4. [UML Class Diagrams](#4-uml-class-diagrams)
   - 4.1 [Authentication & Event Model](#41-authentication--event-model)
   - 4.2 [Utility Classes](#42-utility-classes)
   - 4.3 [Admin Layer](#43-admin-layer)
   - 4.4 [Favourites Fragment](#44-favourites-fragment)
   - 4.5 [My Events Fragment](#45-my-events-fragment)
   - 4.6 [Search Fragment](#46-search-fragment)
   - 4.7 [Profile Layer](#47-profile-layer)
   - 4.8 [Event Ticket Purchase Flow](#48-event-ticket-purchase-flow)
   - 4.9 [Event Calendar Fragment Flow](#49-event-calendar-fragment-flow)
   - 4.10 [Event Management Home Flow](#410-event-management-home-flow)
   - 4.11 [Event Proposal Creation Flow](#411-event-proposal-creation-flow)
5. [CRC Cards](#5-crc-cards)
6. [Application Architecture](#6-application-architecture)
   - 6.1 [Package Structure](#61-package-structure)
   - 6.2 [Layer Overview](#62-layer-overview)
7. [Firebase & Firestore Database Design](#7-firebase--firestore-database-design)
   - 7.1 [Collection Tree](#71-collection-tree)
   - 7.2 [users / {userId}](#72-users--userid)
   - 7.3 [events / {eventId}](#73-events--eventid)
   - 7.4 [event_proposals / {proposalId}](#74-event_proposals--proposalid)
   - 7.5 [payments / {paymentId}](#75-payments--paymentid)
   - 7.6 [reports / {reportId}](#76-reports--reportid)
   - 7.7 [notifications / {userId} / messages / {notificationId}](#77-notifications--userid--messages--notificationid)
   - 7.8 [app_config / settings](#78-app_config--settings)
8. [Data Models](#8-data-models)
   - 8.1 [User.java](#81-userjava)
   - 8.2 [Event.java](#82-eventjava)
   - 8.3 [Rsvp.java](#83-rsvpjava)
   - 8.4 [EventProposal.java](#84-eventproposaljava)
   - 8.5 [EventAttendee.java](#85-eventattendeejava)
   - 8.6 [Payment.java](#86-paymentjava)
   - 8.7 [Notification.java](#87-notificationjava)
   - 8.8 [Memory.java](#88-memoryjava)
9. [Authentication System](#9-authentication-system)
   - 9.1 [AuthRepository Interface](#91-authrepository-interface)
   - 9.2 [FirebaseAuthRepository](#92-firebaseauthrepository)
   - 9.3 [MockAuthRepository](#93-mockauthrepository)
   - 9.4 [AuthCallback Interface](#94-authcallback-interface)
   - 9.5 [SignupValidator](#95-signupvalidator)
   - 9.6 [DevSessionManager](#96-devsessionmanager)
   - 9.7 [DevBypassHelper](#97-devbypasshelper)
   - 9.8 [UserRoles](#98-userroles)
   - 9.9 [Sign-In Flow (SignInActivity)](#99-sign-in-flow-signinactivity)
   - 9.10 [Sign-Up Flow (SignUpActivity)](#910-sign-up-flow-signupactivity)
10. [Payment Demo Pipeline](#10-payment-demo-pipeline)
    - 10.1 [Flow Diagram](#101-flow-diagram)
    - 10.2 [BuyTicketActivity](#102-buyticketactivity)
    - 10.3 [CheckoutActivity](#103-checkoutactivity)
    - 10.4 [MockPaymentService](#104-mockpaymentservice)
    - 10.5 [PaymentRepository](#105-paymentrepository)
11. [QR Code Ticketing System](#11-qr-code-ticketing-system)
    - 11.1 [Token Generation Strategy](#111-token-generation-strategy)
    - 11.2 [QRCodeHelper](#112-qrcodehelper)
    - 11.3 [TicketActivity](#113-ticketactivity)
12. [QR Code Check-In & Organiser Scanner](#12-qr-code-check-in--organiser-scanner)
    - 12.1 [ScannerActivity — Camera-Based Scan](#121-scanneractivity--camera-based-scan)
    - 12.2 [WhoIsComingActivity — Attendee Management](#122-whoiscomingactivity--attendee-management)
    - 12.3 [One-Time-Use Enforcement](#123-one-time-use-enforcement)
    - 12.4 [Blacklist System](#124-blacklist-system)
13. [EventRepository — Core Data Layer](#13-eventrepository--core-data-layer)
    - 13.1 [Callback Interfaces](#131-callback-interfaces)
    - 13.2 [Event Fetching Methods](#132-event-fetching-methods)
    - 13.3 [RSVP Transaction (Atomic)](#133-rsvp-transaction-atomic)
    - 13.4 [Cancel RSVP Transaction](#134-cancel-rsvp-transaction)
    - 13.5 [Organiser & Admin Operations](#135-organiser--admin-operations)
    - 13.6 [Notification Dispatch](#136-notification-dispatch)
    - 13.7 [Rating System](#137-rating-system)
    - 13.8 [SOS Reports](#138-sos-reports)
14. [Application Entry Points & Navigation](#14-application-entry-points--navigation)
    - 14.1 [CampusEventDiscoveryApp](#141-campuseventdiscoveryapp)
    - 14.2 [SplashActivity](#142-splashactivity)
    - 14.3 [MainActivity & Role-Aware Navigation](#143-mainactivity--role-aware-navigation)
15. [Utility Classes](#15-utility-classes)
    - 15.1 [ThemeManager](#151-thememanager)
    - 15.2 [EventValidator](#152-eventvalidator)
    - 15.3 [NavigationTransitions](#153-navigationtransitions)
16. [UI Layer — Activities & Fragments](#16-ui-layer--activities--fragments)
17. [RecyclerView Adapters](#17-recyclerview-adapters)
18. [Test Suite](#18-test-suite)
19. [Build Configuration & Dependencies](#19-build-configuration--dependencies)
20. [Permissions](#20-permissions)
21. [Screenshots](#21-screenshots)
22. [Known Limitations & Future Work](#22-known-limitations--future-work)
23. [Team](#23-team)

---

## 1. Project Overview

The **Campus Event Discovery and Management Platform** is a full-stack Android prototype built for LUMS (CS360 Software Engineering, Spring 2026). It targets three distinct user roles operating on a shared Firebase backend:

| Role | Primary Capabilities |
|---|---|
| **Attendee** | Browse/search events, RSVP, purchase tickets, QR check-in, save favourites, view calendar, submit feedback, access memories, SOS reporting |
| **Organiser** | Submit event proposals, manage approved events, view attendee lists, scan QR check-ins, send announcements, blacklist attendees |
| **Admin** | Review and approve/reject proposals, manage app maintenance mode, configure featured events, oversee all platform activity |

The application is written entirely in **Java**, uses **XML-based layouts**, and communicates directly with **Firebase Authentication**, **Cloud Firestore**, **Firebase Storage**, and **Firebase App Check**.

---

## 2. Prerequisites

Before building or running the project, ensure the following are installed and configured:

| Requirement | Version / Notes |
|---|---|
| Android Studio | Ladybug (2024.2.1) or later recommended |
| JDK | JDK 11 (bundled with Android Studio; set `JAVA_HOME` to `/Applications/Android Studio.app/Contents/jbr/Contents/Home` on macOS) |
| Android SDK | API 34 (targetSdk); API 36 (compileSdk) |
| Gradle | 9.0.1 (via Gradle Wrapper — no separate installation required) |
| Firebase Project | A configured Firebase project with Authentication (Email/Password), Firestore, Storage, and App Check enabled |
| `google-services.json` | Placed at `app/google-services.json` — **not committed to the repository; obtain from the team** |
| Git | Any recent version |

---

## 3. Installation & Setup

### 3.1 Clone the Repository

```bash
git clone https://github.com/CS360S26gemini/Campus-Event-Discovery-and-Management-Platform.git
cd Campus-Event-Discovery-and-Management-Platform
```

To work from the latest stable branch:

```bash
git fetch origin
git checkout nausher-final-fix
git pull origin nausher-final-fix
```

### 3.2 Firebase Configuration

1. Obtain `google-services.json` from a team member (available via WhatsApp or direct share — never committed to the repository for security reasons).
2. Place the file at the following path within the project:
   ```
   app/google-services.json
   ```
3. Verify the `applicationId` in `app/build.gradle.kts` matches the package name registered in your Firebase project:
   ```kotlin
   applicationId = "com.example.CampusEventDiscovery"
   ```
4. In the Firebase Console, ensure the following services are enabled:
   - **Authentication** → Sign-in method → Email/Password: **Enabled**
   - **Cloud Firestore** → Database created in production or test mode
   - **Firebase Storage** → Default bucket configured
   - **App Check** → Play Integrity (release) and Debug (debug builds)

**Developer Bypass (no Firebase account required):** The application includes a built-in developer bypass accessible from both `SignInActivity` and `SignUpActivity` via the "Dev Bypass" button. This allows testing all three role experiences (`attendee`, `organiser`, `admin`) without creating Firebase accounts. The bypass stores a role flag in `SharedPreferences` via `DevSessionManager` and routes directly to `MainActivity`.

### 3.3 Build & Run

**Via Android Studio:**
1. Open the project root directory in Android Studio.
2. Wait for Gradle sync to complete.
3. Select **Run → Edit Configurations**, set the **Module** to `app`, and click **Apply**.
4. Connect a physical device or start an emulator (API 24+).
5. Click **Run ▶**.

**Via command line:**
```bash
# macOS/Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

**ADB note (macOS):** `adb` is not on `$PATH` by default. Use the full path:
```bash
~/Library/Android/sdk/platform-tools/adb devices
```

Or add it permanently to your shell profile:
```bash
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
```

---

## 4. UML Class Diagrams

All UML diagrams are stored in the repository under `docs/images/uml/`. Each diagram was derived directly from the implemented source code and reflects the actual class structure, relationships, and method signatures present in the codebase.

> **To add these images to the repository**, place the PNG files at the paths shown under each diagram. GitHub will render them inline.

---

### 4.1 Authentication & Event Model

**File:** `docs/images/uml/Authentication and Event-2026-04-07-132810.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/repository/AuthRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/FirebaseAuthRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/MockAuthRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/User.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Rsvp.java`
- `app/src/main/java/com/example/CampusEventDiscovery/callback/AuthCallback.java`

![Authentication & Event Model UML](docs/images/uml/Authentication%20and%20Event-2026-04-07-132810.png)

---

### 4.2 Utility Classes

**File:** `docs/images/uml/Untitled diagram-2026-04-07-133119.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/util/MockPaymentService.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/EventValidator.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/DevBypassHelper.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/DevSessionManager.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/NavigationTransitions.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/QRCodeHelper.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/SignupValidator.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/ThemeManager.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/UserRoles.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Payment.java`

![Utility Classes UML](docs/images/uml/Untitled%20diagram-2026-04-07-133119.png)

---

### 4.3 Admin Layer

**File:** `docs/images/uml/Untitled diagram-2026-04-07-134105.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/admin/AdminHomeActivity.java`

![AdminHomeActivity UML](docs/images/uml/Untitled%20diagram-2026-04-07-134105.png)

---

### 4.4 Favourites Fragment

**File:** `docs/images/uml/Untitled diagram-2026-04-07-134935.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/favourites/FavouritesFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/EventAdapter.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`

![FavouritesFragment UML](docs/images/uml/Untitled%20diagram-2026-04-07-134935.png)

---

### 4.5 My Events Fragment

**File:** `docs/images/uml/Untitled diagram-2026-04-07-135041.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/myevents/MyEventsFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/EventAdapter.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/EventProposal.java`

![MyEventsFragment UML](docs/images/uml/Untitled%20diagram-2026-04-07-135041.png)

---

### 4.6 Search Fragment

**File:** `docs/images/uml/Untitled diagram-2026-04-07-135535.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/search/SearchFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/EventAdapter.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`

![SearchFragment UML](docs/images/uml/Untitled%20diagram-2026-04-07-135535.png)

---

### 4.7 Profile Layer

**File:** `docs/images/uml/Untitled diagram-2026-04-07-135643.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/ProfileFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/AccountSettingsActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoriesActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/NotificationCenterActivity.java`

![Profile Layer UML](docs/images/uml/Untitled%20diagram-2026-04-07-135643.png)

---

### 4.8 Event Ticket Purchase Flow

**File:** `docs/images/uml/Event Ticket Purchase Flow-2026-04-07-134638.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/BuyTicketActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/CheckoutActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/TicketActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/MockPaymentService.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/QRCodeHelper.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/PaymentRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`

![Event Ticket Purchase Flow UML](docs/images/uml/Event%20Ticket%20Purchase%20Flow-2026-04-07-134638.png)

---

### 4.9 Event Calendar Fragment Flow

**File:** `docs/images/uml/Event Calendar Fragment Flow-2026-04-07-134506.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/calendar/EventCalendarFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/Rsvp.java`

![Event Calendar Fragment Flow UML](docs/images/uml/Event%20Calendar%20Fragment%20Flow-2026-04-07-134506.png)

---

### 4.10 Event Management Home Flow

**File:** `docs/images/uml/Event Management Home Flow-2026-04-07-135206.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeOrganizerFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeAdminFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`

![Event Management Home Flow UML](docs/images/uml/Event%20Management%20Home%20Flow-2026-04-07-135206.png)

---

### 4.11 Event Proposal Creation Flow

**File:** `docs/images/uml/Event Proposal Creation Flow-2026-04-07-135437.png`  
**Source classes:**
- `app/src/main/java/com/example/CampusEventDiscovery/ui/organizer/CreateEventActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventApprovalActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/OrganizerProposalDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/model/EventProposal.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/EventValidator.java`

![Event Proposal Creation Flow UML](docs/images/uml/Event%20Proposal%20Creation%20Flow-2026-04-07-135437.png)

---

## 5. CRC Cards

CRC (Class–Responsibility–Collaborator) cards describe the high-level design intent for the core domain classes. These were produced during the design phase and served as the basis for the implemented model and repository classes.

**Source file:** `docs/CRC_cards`

---

```
+------------------------------------------------------+
|                      USER                            |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Browse upcoming events    | Event                    |
| Request event search      | EventRepository          |
| Request event filtering   | SearchFilter             |
| View event details        | BookmarkManager          |
| Request bookmarking       | NotificationManager      |
| of events                 |                          |
| View saved/bookmarked     |                          |
| events                    |                          |
| Receive event reminders   |                          |
| and notifications         |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `app/src/main/java/com/example/CampusEventDiscovery/model/User.java`

---

```
+------------------------------------------------------+
|                      EVENT                           |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Maintain event            | Category                 |
| information               | Venue                    |
| Provide title,            | EventRepository          |
| description, date, time   | BookmarkManager          |
| Provide category info     | NotificationManager      |
| Provide venue/location    |                          |
| information               |                          |
| Provide full event        |                          |
| details for display       |                          |
| Indicate whether event    |                          |
| is upcoming               |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`

---

```
+------------------------------------------------------+
|                 EVENT REPOSITORY                     |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Store and retrieve        | Event                    |
| event data                | Category                 |
| Retrieve upcoming events  | Venue                    |
| Sort events by date/time  | SearchFilter             |
| Fetch event details       |                          |
| Provide event lists       |                          |
| Return filtered/searched  |                          |
| event data                |                          |
| Handle missing/unavailable|                          |
| event data gracefully     |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`

---

```
+------------------------------------------------------+
|                     CATEGORY                         |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Maintain category         | Event                    |
| information               | EventRepository          |
| Classify events by type   | SearchFilter             |
| Support category-based    |                          |
| filtering                 |                          |
| Organize events into      |                          |
| groups for browsing       |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `category` field on `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java`; validated against allowed values in `app/src/main/java/com/example/CampusEventDiscovery/util/EventValidator.java`

---

```
+------------------------------------------------------+
|                 BOOKMARK MANAGER                     |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Add events to bookmarks   | User                     |
| Remove events from        | Event                    |
| bookmarks                 |                          |
| Retrieve bookmarked       |                          |
| events                    |                          |
| Maintain user's saved     |                          |
| events list               |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `saveEvent()`, `unsaveEvent()`, `getSavedEvents()` in `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`; persistence in `users/{uid}/saved_events/` Firestore subcollection

---

```
+------------------------------------------------------+
|               NOTIFICATION MANAGER                   |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Schedule event reminders  | User                     |
| Send event notifications  | Event                    |
| Manage notification       |                          |
| settings                  |                          |
| Trigger reminders before  |                          |
| event time                |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `getNotifications()`, `markNotificationRead()`, `sendAnnouncementToAttendees()` in `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`; model at `app/src/main/java/com/example/CampusEventDiscovery/model/Notification.java`; UI at `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/NotificationCenterActivity.java`

---

```
+------------------------------------------------------+
|                       VENUE                          |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Maintain venue/location   | Event                    |
| information               | EventRepository          |
| Provide venue name        | SearchFilter             |
| and details               |                          |
| Link events to locations  |                          |
| Support venue/location    |                          |
| based filtering           |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `location` field on `app/src/main/java/com/example/CampusEventDiscovery/model/Event.java` and `app/src/main/java/com/example/CampusEventDiscovery/model/EventProposal.java`

---

```
+------------------------------------------------------+
|                   SEARCH FILTER                      |
+---------------------------+--------------------------+
| Responsibilities          | Collaborators            |
+---------------------------+--------------------------+
| Process search queries    | User                     |
| Filter events by category | EventRepository          |
| Filter events by date     | Event                    |
| Filter events by venue    | Category                 |
| Return matching event     | Venue                    |
| results                   |                          |
+---------------------------+--------------------------+
```

*Implemented as:* `searchEvents(query, category, cb)` in `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`; UI at `app/src/main/java/com/example/CampusEventDiscovery/ui/search/SearchFragment.java`

---

## 6. Application Architecture

### 6.1 Package Structure

```
com.example.CampusEventDiscovery/
│
├── CampusEventDiscoveryApp.java          # Application class — Firebase init, App Check, theme
├── MainActivity.java                     # Host activity — bottom nav, role routing
├── SplashActivity.java                   # Entry point — maintenance check, auth routing
├── SignInActivity.java                   # Login screen
├── SignUpActivity.java                   # Registration screen
├── WelcomeActivity.java                  # Unauthenticated landing screen
├── MaintenanceActivity.java              # Maintenance mode screen
│
├── adapter/                              # RecyclerView adapters
│   ├── AttendeeAdapter.java
│   ├── EventAdapter.java
│   ├── MemoryAdapter.java
│   ├── NotificationAdapter.java
│   ├── OrganizerPendingAdapter.java
│   └── TicketTierAdapter.java
│
├── callback/                             # Async callback interfaces
│   ├── AuthCallback.java
│   └── FirestoreCallback.java
│
├── model/                                # Firestore-mapped POJOs
│   ├── Event.java
│   ├── EventAttendee.java
│   ├── EventProposal.java
│   ├── Memory.java
│   ├── Notification.java
│   ├── Payment.java
│   ├── Rsvp.java
│   └── User.java
│
├── repository/                           # Data access layer
│   ├── AuthRepository.java               # Interface
│   ├── EventRepository.java              # Primary Firestore operations class
│   ├── FirebaseAuthRepository.java       # Firebase implementation
│   ├── MockAuthRepository.java           # In-memory test stub
│   └── PaymentRepository.java            # Payment Firestore operations
│
├── ui/
│   ├── admin/
│   │   └── AdminHomeActivity.java
│   ├── calendar/
│   │   └── EventCalendarFragment.java
│   ├── event/
│   │   ├── BuyTicketActivity.java
│   │   ├── CheckoutActivity.java
│   │   ├── EventApprovalActivity.java
│   │   ├── EventDetailActivity.java
│   │   ├── EventFeedbackActivity.java
│   │   ├── OrganizerProposalDetailActivity.java
│   │   └── TicketActivity.java
│   ├── favourites/
│   │   └── FavouritesFragment.java
│   ├── home/
│   │   ├── HomeAdminFragment.java
│   │   ├── HomeFragment.java
│   │   └── HomeOrganizerFragment.java
│   ├── myevents/
│   │   └── MyEventsFragment.java
│   ├── organizer/
│   │   ├── CreateEventActivity.java
│   │   ├── ManageEventsActivity.java
│   │   ├── OrganizerEventDetailActivity.java
│   │   ├── ScannerActivity.java
│   │   └── WhoIsComingActivity.java
│   ├── profile/
│   │   ├── AccountSettingsActivity.java
│   │   ├── MemoriesActivity.java
│   │   ├── NotificationCenterActivity.java
│   │   └── ProfileFragment.java
│   └── search/
│       └── SearchFragment.java
│
└── util/                                 # Utility and helper classes
    ├── DevBypassHelper.java
    ├── DevSessionManager.java
    ├── EventValidator.java
    ├── MockPaymentService.java
    ├── NavigationTransitions.java
    ├── QRCodeHelper.java
    ├── SignupValidator.java
    ├── ThemeManager.java
    └── UserRoles.java
```

### 6.2 Layer Overview

The project follows a lightweight **Repository pattern**:

```
UI Layer (Activities / Fragments)
        │
        ▼
Repository Layer (EventRepository, FirebaseAuthRepository, PaymentRepository)
        │
        ▼
Firebase SDK (FirebaseFirestore, FirebaseAuth, FirebaseStorage)
        │
        ▼
Google Cloud (Firestore, Authentication, Storage, App Check)
```

No ViewModel or LiveData layer is used in this prototype; Firestore async callbacks are dispatched directly to UI components. All Firestore transactions run on the calling thread and callbacks are delivered on the main thread by the Firebase SDK.

---

## 7. Firebase & Firestore Database Design

### 5.1 Collection Tree

```
FIRESTORE ROOT
│
├── users/
│   └── {userId}/
│       ├── [fields]
│       ├── saved_events/     {eventId}
│       ├── rsvps/            {eventId}
│       └── memories/         {memoryId}
│
├── events/
│   └── {eventId}/
│       ├── [fields]
│       ├── ratings/          {userId}
│       ├── attendees/        {userId}
│       └── blacklist/        {userId}
│
├── event_proposals/
│   └── {proposalId}/         [fields]
│
├── payments/
│   └── {paymentId}/          [fields]
│
├── reports/
│   └── {reportId}/           [fields]
│
├── notifications/
│   └── {userId}/
│       └── messages/         {notificationId}
│
└── app_config/
    └── settings              [single document]
```

### 5.2 users / {userId}

The document ID is the Firebase Authentication UID, assigned automatically at registration.

| Field | Type | Description |
|---|---|---|
| `fullName` | `string` | User's full display name |
| `email` | `string` | Registered email address |
| `role` | `string` | `"attendee"` / `"organizer"` / `"admin"` |
| `university` | `string` | e.g. `"LUMS"` |
| `location` | `string` | e.g. `"Lahore"` |
| `profilePicUrl` | `string` | Firebase Storage download URL |
| `interests` | `array<string>` | e.g. `["Music", "Sports"]` — used for personalised event feed |
| `darkMode` | `boolean` | `true` = dark theme active |
| `fcmToken` | `string` | Firebase Cloud Messaging device token for push notifications |
| `googleCalendarToken` | `map` | `{ accessToken, refreshToken, expiresAt }` |
| `createdAt` | `timestamp` | Server-set registration timestamp |

**Subcollection: `saved_events / {eventId}`**

| Field | Type | Description |
|---|---|---|
| `eventId` | `string` | Denormalised reference |
| `title` | `string` | Copied for fast list rendering |
| `date` | `timestamp` | Event date |
| `category` | `string` | Event category |
| `thumbnailUrl` | `string` | Banner image URL |
| `savedAt` | `timestamp` | When the user saved the event |

**Subcollection: `rsvps / {eventId}`**

| Field | Type | Description |
|---|---|---|
| `eventId` | `string` | Denormalised reference |
| `title` | `string` | Copied for fast list rendering |
| `date` | `timestamp` | Event date |
| `status` | `string` | `"confirmed"` / `"cancelled"` / `"attended"` |
| `checkedIn` | `boolean` | Set to `true` after QR scan |
| `qrCodeToken` | `string` | UUID generated at RSVP creation — encoded in the attendee's QR ticket |
| `paymentStatus` | `string` | `"SUCCESS"` (merged from `CheckoutActivity`) |
| `transactionId` | `string` | UUID from `MockPaymentService` |
| `qrPayload` | `string` | JSON string `{userId, eventId, transactionId, timestamp}` — encoded in the QR bitmap |
| `qrExpired` | `boolean` | Set to `true` after successful organiser scan — enforces one-time use |
| `addedToCalendar` | `boolean` | Calendar integration tracking |
| `gcalEventId` | `string` | Google Calendar event ID for removal on RSVP cancellation |
| `rsvpAt` | `timestamp` | RSVP creation timestamp |
| `checkedInAt` | `timestamp` | Timestamp of check-in |

**Subcollection: `memories / {memoryId}`**

| Field | Type | Description |
|---|---|---|
| `eventId` | `string` | Reference to attended event |
| `eventTitle` | `string` | Denormalised title |
| `photoUrls` | `array<string>` | Firebase Storage URLs of uploaded photos |
| `attendedAt` | `timestamp` | When the memory was created |
| `rating` | `number` | 1–5 star rating given by the user |

### 5.3 events / {eventId}

Document ID is auto-generated by Firestore at approval time.

| Field | Type | Description |
|---|---|---|
| `title` | `string` | Event name |
| `description` | `string` | Full event description |
| `category` | `string` | e.g. `"Music"` / `"Career"` / `"Sports"` |
| `tags` | `array<string>` | e.g. `["Live", "Concert"]` — matched against user interests |
| `date` | `timestamp` | Start date/time |
| `endTime` | `timestamp` | End date/time (auto-computed as start + 2 hours on approval) |
| `location` | `string` | Venue description |
| `capacity` | `number` | Maximum attendees |
| `rsvpCount` | `number` | Live count — incremented/decremented atomically via Firestore transactions |
| `checkedInCount` | `number` | Incremented on each QR check-in |
| `thumbnailUrl` | `string` | Firebase Storage banner image URL |
| `trailerUrl` | `string` | Firebase Storage video URL (optional) |
| `sponsors` | `array<string>` | Sponsor names |
| `foodStalls` | `array<string>` | Food stall names |
| `organizerId` | `string` | References `users/{userId}` |
| `organizerName` | `string` | Denormalised for fast rendering |
| `verified` | `boolean` | Admin-set badge flag |
| `averageRating` | `number` | Rolling average — updated transactionally on each rating write |
| `ratingCount` | `number` | Total ratings received |
| `status` | `string` | `"active"` / `"pending"` / `"cancelled"` / `"completed"` |
| `ticketPrice` | `number` | Price in PKR; `0.0` for free events |
| `createdAt` | `timestamp` | Inherited from the approved proposal |

**Subcollection: `ratings / {userId}`**

| Field | Type | Description |
|---|---|---|
| `userId` | `string` | Reviewer's UID |
| `stars` | `number` | 1–5 |
| `review` | `string` | Optional text review |
| `ratedAt` | `timestamp` | Rating timestamp |

**Subcollection: `attendees / {userId}`**

| Field | Type | Description |
|---|---|---|
| `userId` | `string` | Attendee's UID |
| `fullName` | `string` | Denormalised for organiser view |
| `qrToken` | `string` | Must match `users/{uid}/rsvps/{eid}.qrCodeToken` — lookup key for scanner |
| `checkedIn` | `boolean` | Mirrored from RSVP |
| `checkedInAt` | `timestamp` | Mirrored from RSVP |

**Subcollection: `blacklist / {userId}`**

| Field | Type | Description |
|---|---|---|
| `userId` | `string` | Blacklisted user's UID |
| `fullName` | `string` | Name at time of blacklist |
| `blacklistedAt` | `timestamp` | Blacklist timestamp |

### 5.4 event_proposals / {proposalId}

Contains the same fields as an approved event document, plus:

| Field | Type | Description |
|---|---|---|
| `status` | `string` | `"pending"` / `"approved"` / `"rejected"` |
| `adminNote` | `string` | Rejection reason provided by admin |
| `submittedAt` | `timestamp` | Proposal submission timestamp |
| `reviewedAt` | `timestamp` | Admin review timestamp |
| `ticketPrice` | `number` | Organiser-set price (0.0 = free) |

### 5.5 payments / {paymentId}

| Field | Type | Description |
|---|---|---|
| `userId` | `string` | Paying user's UID |
| `eventId` | `string` | Event being paid for |
| `amount` | `number` | Amount in PKR |
| `status` | `string` | `"SUCCESS"` / `"FAILED"` |
| `transactionId` | `string` | UUID generated by `MockPaymentService` |
| `timestamp` | `number` | `System.currentTimeMillis()` at payment time |

### 5.6 reports / {reportId}

| Field | Type | Description |
|---|---|---|
| `reporterId` | `string` | Null if anonymous |
| `reporterName` | `string` | Null if anonymous |
| `isAnonymous` | `boolean` | Whether reporter chose anonymity |
| `description` | `string` | Incident description |
| `location` | `map` | `{ lat: number, lng: number }` — device GPS coordinates |
| `status` | `string` | `"open"` / `"reviewed"` / `"resolved"` |
| `adminNote` | `string` | Admin response note |
| `submittedAt` | `timestamp` | Report submission timestamp |
| `resolvedAt` | `timestamp` | Resolution timestamp |

### 5.7 notifications / {userId} / messages / {notificationId}

| Field | Type | Description |
|---|---|---|
| `title` | `string` | Notification headline |
| `body` | `string` | Notification body text |
| `type` | `string` | `"event_approved"` / `"event_rejected"` / `"event_announcement"` |
| `eventId` | `string` | Related event ID (nullable) |
| `isRead` | `boolean` | Read state |
| `createdAt` | `timestamp` | Notification creation timestamp |

### 5.8 app_config / settings

| Field | Type | Description |
|---|---|---|
| `maintenanceMode` | `boolean` | When `true`, non-admin users are redirected to `MaintenanceActivity` |
| `featuredEventIds` | `array<string>` | Ordered list of event IDs displayed in the featured carousel |

---

## 8. Data Models

All model classes reside in `com.example.CampusEventDiscovery.model`. Each class includes a no-argument constructor (required by the Firestore SDK for automatic deserialization via `DocumentSnapshot.toObject()`), a full parameterised constructor, and complete getters and setters.

Fields annotated with `@com.google.firebase.firestore.Exclude` are excluded from Firestore serialization — they exist only as runtime helpers (typically to carry the document ID back into the object after a fetch).

### 6.1 User.java

Maps to the `users/{userId}` document.

```
Fields: fullName, email, role, university, location, profilePicUrl,
        interests (List<String>), darkMode, fcmToken,
        googleCalendarToken (Map<String,Object>), createdAt
```

The `interests` list is the basis for personalised event filtering in `EventRepository.getPersonalisedEvents()`. The `darkMode` flag is read on sign-in and applied to the app theme via `ThemeManager`.

### 6.2 Event.java

Maps to the `events/{eventId}` document.

```
Fields: @Exclude eventId, title, description, category, tags (List<String>),
        date (Timestamp), endTime (Timestamp), location, capacity (long),
        rsvpCount (long), checkedInCount (long), thumbnailUrl, trailerUrl,
        sponsors (List<String>), foodStalls (List<String>),
        organizerId, organizerName, verified (boolean),
        averageRating (double), ratingCount (long),
        status (default "pending"), ticketPrice (double), createdAt
```

`eventId` is excluded from serialization and is populated by `EventRepository.documentToEvent()` immediately after a Firestore fetch, using the `DocumentSnapshot.getId()` value.

### 6.3 Rsvp.java

Maps to the `users/{userId}/rsvps/{eventId}` subcollection document.

```
Fields: @Exclude rsvpId, userId, eventId, title, date (Timestamp),
        status, paymentStatus, transactionId, qrPayload,
        checkedIn (boolean), qrExpired (boolean),
        rsvpAt (Timestamp), checkedInAt (Timestamp)

@Exclude helpers: getCheckInTimestamp(), setCheckInTimestamp(long)
```

The `qrPayload` field stores a JSON string containing `{userId, eventId, transactionId, timestamp}` — this is the content encoded into the QR bitmap rendered in `TicketActivity`. The `qrExpired` flag is set to `true` by `ScannerActivity` on first successful scan.

### 6.4 EventProposal.java

Maps to the `event_proposals/{proposalId}` document.

```
Fields: @Exclude proposalId, title, description, category,
        tags (List<String>), date (Timestamp), location,
        capacity (long), sponsors (List<String>), foodStalls (List<String>),
        trailerUrl, thumbnailUrl, organizerId, organizerName,
        status, adminNote, submittedAt (Timestamp),
        reviewedAt (Timestamp), ticketPrice (double)
```

When an admin approves a proposal, `EventRepository.approveProposal()` uses a `WriteBatch` to atomically update the proposal status, create a new document in the `events` collection (via `proposalToApprovedEvent()`), and dispatch a notification to the organiser.

### 6.5 EventAttendee.java

Maps to the `events/{eventId}/attendees/{userId}` subcollection document.

```
Fields: userId, fullName, qrToken, checkedIn (boolean), checkedInAt (Timestamp)
```

This document is written inside the `rsvpEvent()` transaction alongside the user's RSVP document. It is the lookup target for QR check-in: `ScannerActivity` and `WhoIsComingActivity` query this subcollection by `qrToken`.

### 6.6 Payment.java

Maps to the `payments/{paymentId}` document.

```
Fields: @Exclude paymentId, userId, eventId,
        amount (double), status, transactionId, timestamp (long)
```

Payment documents are written by `PaymentRepository.savePayment()` immediately after `MockPaymentService.processPayment()` returns. The `transactionId` is also stored on the RSVP and encoded in the QR payload for cross-referencing during scanner lookup.

### 6.7 Notification.java

Maps to the `notifications/{userId}/messages/{notificationId}` document.

```
Fields: @Exclude notificationId, title, body, type, eventId,
        isRead (boolean), createdAt (Timestamp)
```

Notifications are created via `WriteBatch` operations inside `EventRepository.approveProposal()`, `rejectProposal()`, and `sendAnnouncementToAttendees()`.

### 6.8 Memory.java

Maps to the `users/{userId}/memories/{memoryId}` subcollection document.

```
Fields: eventId, eventTitle, photoUrls (List<String>),
        attendedAt (Timestamp), rating (int)
```

---

## 9. Authentication System

### 7.1 AuthRepository Interface

`com.example.CampusEventDiscovery.repository.AuthRepository`

```java
public interface AuthRepository {
    void signup(String name, String email, String password, String role, AuthCallback callback);
    void login(String email, String password, AuthCallback callback);
    void logout();
    boolean isLoggedIn();
}
```

The interface decouples authentication logic from the UI, enabling the `MockAuthRepository` to replace `FirebaseAuthRepository` in unit tests without any Firebase SDK involvement.

### 7.2 FirebaseAuthRepository

`com.example.CampusEventDiscovery.repository.FirebaseAuthRepository`

The production implementation of `AuthRepository`. It holds references to `FirebaseAuth` and `FirebaseFirestore` instances, both obtained as singletons via their respective `getInstance()` calls.

**`signup()` flow:**
1. Calls `FirebaseAuth.createUserWithEmailAndPassword(email, password)`.
2. On success, obtains the UID from `auth.getCurrentUser().getUid()`.
3. Constructs a `Map<String, Object>` containing `uid`, `fullName`, `email`, `role`, and `createdAt` (server timestamp).
4. Writes to `db.collection("users").document(uid)`.
5. On Firestore write success, constructs a `User` object and delivers it to `callback.onSuccess(user)`.

**`login()` flow:**
1. Calls `FirebaseAuth.signInWithEmailAndPassword(email, password)`.
2. On success, fetches the user's Firestore document at `users/{uid}`.
3. Constructs a `User` from the document fields and delivers it to `callback.onSuccess(user)`.
4. Role resolution (`user.getRole()`) drives navigation in the calling activity.

### 7.3 MockAuthRepository

`com.example.CampusEventDiscovery.repository.MockAuthRepository`

An in-memory stub pre-populated with a test user (`test@lums.edu.pk` / `attendee`). Used exclusively in the unit test suite. It also handles the hardcoded admin credential `admin / admin123` for convenience. Any email not already in the internal map is accepted by `signup()`, and `login()` rejects only if the password is literally `"wrongpass"`.

### 7.4 AuthCallback Interface

`com.example.CampusEventDiscovery.callback.AuthCallback`

```java
public interface AuthCallback {
    void onSuccess(User user);
    void onFailure(String errorMessage);
}
```

Both `FirebaseAuthRepository` and `MockAuthRepository` deliver results through this interface. The calling `Activity` inspects `user.getRole()` in `onSuccess()` to route to the correct home screen.

### 7.5 SignupValidator

`com.example.CampusEventDiscovery.util.SignupValidator`

A stateless utility with a single static method `validate(name, email, password, confirmPassword, role)` that returns either an error message string or `null` if all fields are valid.

Validation rules enforced:
- `name` must be non-empty.
- `email` must match `android.util.Patterns.EMAIL_ADDRESS`.
- `password` must be at least 8 characters, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.
- `confirmPassword` must match `password`.
- `role` must be either `"attendee"` or `"organizer"` (admin accounts are not self-registerable).

### 7.6 DevSessionManager

`com.example.CampusEventDiscovery.util.DevSessionManager`

A final, non-instantiable class that manages a local developer bypass session stored in `SharedPreferences` under the name `"dev_session"`.

| Method | Description |
|---|---|
| `enableBypass(Context, String role)` | Sets `bypass_enabled = true` and stores the given role |
| `clearBypass(Context)` | Clears all `dev_session` preferences |
| `isBypassEnabled(Context)` | Returns `true` if bypass flag is set |
| `shouldUseBypass(Context)` | Returns `true` if bypass is enabled AND `FirebaseAuth.getCurrentUser()` is null |
| `getBypassRole(Context)` | Returns the stored role string, defaulting to `"attendee"` |
| `getEffectiveUserId(Context)` | Returns `"demo-organizer-user"`, `"demo-admin-user"`, or `"demo-attendee-user"` based on stored role |
| `getDisplayName(Context)` | Returns `"Test Organizer"`, `"Test Admin"`, or `"Test Attendee"` |
| `getDisplayEmail(Context)` | Returns the appropriate `@test.local` address |

The `shouldUseBypass()` guard is important: it only activates the bypass when there is no real Firebase session. This prevents accidental bypass activation for signed-in users.

### 7.7 DevBypassHelper

`com.example.CampusEventDiscovery.util.DevBypassHelper`

A UI helper that inflates the `dialog_dev_bypass_role_picker` layout and presents a `MaterialAlertDialog` with three role selection buttons (Attendee, Organiser, Admin). On selection, it signs out any current Firebase session, calls `DevSessionManager.enableBypass()`, and launches `MainActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` to reset the back stack.

### 7.8 UserRoles

`com.example.CampusEventDiscovery.util.UserRoles`

Defines the three role constants and provides safe comparison utilities:

```java
public static final String ATTENDEE  = "attendee";
public static final String ORGANIZER = "organizer";
public static final String ADMIN     = "admin";

public static String sanitize(String role)   // normalises case; returns "" for unknowns
public static boolean isAttendee(String role)
public static boolean isOrganizer(String role)
public static boolean isAdmin(String role)
```

All role comparisons in the codebase go through `UserRoles` to avoid raw string equality checks.

### 7.9 Sign-In Flow (SignInActivity)

Located at `com.example.CampusEventDiscovery.SignInActivity`.

The activity presents email, password, a "Remember Me" switch, a Sign In button, and a Dev Bypass button.

On sign-in click:
1. Checks for the hardcoded dev admin shortcut (`email == "admin"`, `password == "admin"`). If matched, calls `DevSessionManager.enableBypass(context, "admin")` and launches `MainActivity` directly, bypassing Firebase.
2. Clears any existing bypass session.
3. Validates that email and password fields are non-empty.
4. Disables the sign-in button to prevent double submission.
5. Calls `FirebaseAuth.signInWithEmailAndPassword()`.
6. On success, fetches the user's Firestore profile via `EventRepository.getUserData()` to sync the `darkMode` preference via `ThemeManager`.
7. Launches `MainActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
8. On failure, re-enables the button and displays a descriptive error via `buildSignInErrorMessage()`, which special-cases `CONFIGURATION_NOT_FOUND` to guide Firebase Console setup.

### 7.10 Sign-Up Flow (SignUpActivity)

Located at `com.example.CampusEventDiscovery.SignUpActivity`.

Collects full name, email, password, repeat password, comma-separated interests, and a role toggle (`Attendee` / `Organiser`).

On registration click:
1. Clears any existing bypass session.
2. Parses interests input into a deduplicated `ArrayList<String>` via `parseInterests()`.
3. Runs `SignupValidator.validate()` — aborts with a Toast on any validation failure.
4. Calls `FirebaseAuth.createUserWithEmailAndPassword()`.
5. On success, constructs a `User` object with all fields and writes it to `db.collection("users").document(uid)`.
6. On Firestore success, launches `MainActivity`.

---

## 10. Payment Demo Pipeline

The payment system is an intentional demonstration prototype. It simulates the full checkout experience without connecting to a real payment gateway.

### 8.1 Flow Diagram

```
EventDetailActivity
        │
        ▼ (RSVP / Buy Ticket button)
BuyTicketActivity
   [Ticket Tier Selection]
        │
        ▼ (user selects tier & confirms total)
CheckoutActivity
   [Name, Payment Method, Card Number]
        │
        ├── Pre-check: Firestore query for existing RSVP
        │       If found → navigateToTicket(existingRsvp)
        │
        ├── validateForm()
        │
        ▼ (all checks pass)
MockPaymentService.processPayment()
        │  returns Payment(SUCCESS, UUID transactionId)
        │
PaymentRepository.savePayment()
        │  writes to payments/{paymentId}
        │
EventRepository.rsvpEvent()    ← Firestore TRANSACTION
        │  · checks blacklist
        │  · checks duplicate RSVP
        │  · checks capacity (rsvpCount < capacity)
        │  · writes users/{uid}/rsvps/{eid} (qrCodeToken = UUID)
        │  · writes events/{eid}/attendees/{uid}
        │  · increments events/{eid}.rsvpCount
        │
mergePaymentOntoRsvp()
        │  adds paymentStatus, transactionId, qrPayload, qrExpired
        │
        ▼
TicketActivity
   [QR Code Bitmap rendered from qrPayload JSON]
```

### 8.2 BuyTicketActivity

`com.example.CampusEventDiscovery.ui.event.BuyTicketActivity`

Receives event metadata via Intent extras (`eventId`, `eventTitle`, `eventDateMillis`, `eventVenue`, `eventCapacity`, `eventRsvpCount`) from `EventDetailActivity`. Displays three hardcoded ticket tiers via `TicketTierAdapter`:

| Tier | Price (PKR) |
|---|---|
| Early Bird | 2,500 |
| VIP | 10,000 |
| General Admission | 3,000 |

Each tier has a quantity selector. The running total is updated live via an `onTotalChanged` callback from the adapter. On "Buy" click, validates that at least one ticket is selected, then launches `CheckoutActivity` with the total price via `ActivityResultLauncher`. `BuyTicketActivity` finishes itself when `CheckoutActivity` returns `RESULT_OK` to clean the back stack.

**Access control:** Both `BuyTicketActivity` and `CheckoutActivity` call `enforceAttendeeAccess()`, which fetches the user's Firestore role and immediately `finish()`es if the role is not `"attendee"`. This prevents organisers and admins from purchasing tickets.

### 8.3 CheckoutActivity

`com.example.CampusEventDiscovery.ui.event.CheckoutActivity`

The central payment orchestration class. Receives all event data and `totalPrice` as Intent extras.

**Form fields:**
- First name / Last name (`EditText`)
- Payment method: JazzCash, Credit Card, Debit Card, Apple Pay (`RadioGroup`)
- Card number (`EditText`) — visible only when Credit Card or Debit Card is selected; requires exactly 16 digits

**`validateForm()`:**
- Ensures first and last name are non-empty.
- Ensures a payment method is selected.
- For Credit Card and Debit Card, validates card number length = 16.

**Duplicate RSVP pre-check:**
Before calling `MockPaymentService`, `CheckoutActivity` queries `users/{uid}/rsvps/{eventId}` directly. If a document exists with a non-`"cancelled"` status and a non-empty `qrPayload`, the user is shown a Toast and redirected to their existing `TicketActivity` without incurring a new charge.

**`processPayment()`:**
Calls `MockPaymentService.processPayment(userId, eventId, totalPrice)` synchronously (no network call), then passes the returned `Payment` object to `PaymentRepository.savePayment()`.

**`runRsvpTransaction(Payment)`:**
Constructs a minimal `Event` shell containing only `eventId`, `title`, and `date`, then delegates to `EventRepository.rsvpEvent()`, which runs the full atomic Firestore transaction.

**`mergePaymentOntoRsvp(Payment)`:**
After the transaction succeeds, constructs the JSON QR payload:
```json
{
  "userId": "...",
  "eventId": "...",
  "transactionId": "...",
  "timestamp": 1234567890
}
```
Then calls `db.collection("users").document(uid).collection("rsvps").document(eventId).update(paymentFields)` to merge `paymentStatus`, `transactionId`, `qrPayload`, and `qrExpired = false` onto the RSVP document created by the transaction.

On success, launches `TicketActivity`.

### 8.4 MockPaymentService

`com.example.CampusEventDiscovery.util.MockPaymentService`

A stateless final class with a single static method:

```java
public static Payment processPayment(String userId, String eventId, double amount) {
    String transactionId = UUID.randomUUID().toString();
    return new Payment(null, userId, eventId, amount, "SUCCESS", transactionId,
                       System.currentTimeMillis());
}
```

Always returns `"SUCCESS"`. The `paymentId` field is `null` at this point; it is set by `PaymentRepository.savePayment()` after Firestore assigns a document ID.

### 8.5 PaymentRepository

`com.example.CampusEventDiscovery.repository.PaymentRepository`

| Method | Description |
|---|---|
| `savePayment(Payment, FirestoreCallback)` | Adds the payment to `payments/`; sets `payment.paymentId` from the generated document reference |
| `getPaymentByTransactionId(String txnId, FirestoreCallback)` | Queries `payments/` where `transactionId == txnId`; limit 1 |

Uses `FirestoreCallback` (not `AuthCallback`) since it returns generic `Object` results.

---

## 11. QR Code Ticketing System

### 9.1 Token Generation Strategy

Two tokens are generated per RSVP:

1. **`qrCodeToken`** — a `UUID.randomUUID().toString()` generated inside the `rsvpEvent()` Firestore transaction and stored in:
   - `users/{uid}/rsvps/{eid}.qrCodeToken`
   - `events/{eid}/attendees/{uid}.qrToken`

   This is the lookup key used by `WhoIsComingActivity` (manual entry) and `EventRepository.checkInAttendeeByQrToken()`. It identifies the specific RSVP in a Firestore `whereEqualTo("qrToken", token)` query.

2. **`qrPayload`** — a JSON string containing `{userId, eventId, transactionId, timestamp}` merged onto the RSVP document by `CheckoutActivity.mergePaymentOntoRsvp()`. This is the string encoded into the visual QR bitmap rendered in `TicketActivity`. It is parsed by `ScannerActivity` during camera-based scanning.

The dual-token design allows the two scanning paths (camera scan parses JSON payload, manual entry uses the plain UUID token) to operate independently.

### 9.2 QRCodeHelper

`com.example.CampusEventDiscovery.util.QRCodeHelper`

```java
public static Bitmap generateQRCode(String content, int widthPx, int heightPx)
```

Uses ZXing's `MultiFormatWriter` to encode the content string as a `QR_CODE` format `BitMatrix`, then converts it to a `Bitmap` via `BarcodeEncoder.createBitmap()`. Returns `null` on `WriterException` (e.g. empty content).

Both `TicketActivity` and any future in-app ticket display screens call this method. The bitmap is rendered at 800×800 pixels to ensure adequate resolution across device densities.

**Dependencies:**
```kotlin
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
implementation("com.google.zxing:core:3.5.3")
```

Both must be declared; `zxing-android-embedded` provides the Android-specific scanning UI layer and camera integrations, while `zxing:core` provides the `MultiFormatWriter`, `BitMatrix`, and `BarcodeFormat` encoding classes.

### 9.3 TicketActivity

`com.example.CampusEventDiscovery.ui.event.TicketActivity`

Receives the following via Intent extras from `CheckoutActivity`:

| Extra Key | Type | Description |
|---|---|---|
| `rsvpId` | `String` | The event ID (used as the RSVP document ID) |
| `eventName` | `String` | Display name |
| `eventDate` | `String` | Pre-formatted date string |
| `transactionId` | `String` | For display |
| `qrPayload` | `String` | JSON string to encode into the QR bitmap |

On `onCreate()`, calls `QRCodeHelper.generateQRCode(qrPayload, 800, 800)` and sets the result on the `ivTicketQrCode` `ImageView`. The "Done" button launches `MainActivity` with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK` and finishes `TicketActivity`.

---

## 12. QR Code Check-In & Organiser Scanner

### 10.1 ScannerActivity — Camera-Based Scan

`com.example.CampusEventDiscovery.ui.organizer.ScannerActivity`

Provides the primary QR check-in experience for organiser use. Launched from `WhoIsComingActivity` via the "Scan QR" button.

**Camera permission flow:**
Uses `ActivityResultLauncher<String>` with `RequestPermission` contract. If `CAMERA` permission is granted, proceeds to `startScanning()`. Otherwise, shows a Toast and returns.

**Scan initiation:**
```java
IntentIntegrator integrator = new IntentIntegrator(this);
integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
integrator.setBeepEnabled(true);
integrator.initiateScan();
```

**`processScanResult(String payload)`:**
Parses the scanned string as a JSON object and extracts `transactionId`, `userId`, and `eventId`. Calls `lookupRsvp()`.

**`lookupRsvp(txnId, userId, eventId)`:**
Fetches `users/{userId}/rsvps/{eventId}` from Firestore. Verifies that the `transactionId` in the document matches the scanned payload to prevent replay attacks using copied payload strings. On mismatch, shows error card; on match, calls `fetchAttendeeNameAndDisplay()` to resolve the user's full name.

**`displayResult()`:**
Renders the result card with attendee name, event name, payment status, and check-in status. Checks `rsvp.isQrExpired()`:
- If `true`: displays "TICKET ALREADY USED" in red; hides the Mark Attended button. This is the one-time-use enforcement at the display layer.
- If `false` and `checkedIn == false`: shows the Mark Attended button.
- If `false` and `checkedIn == true`: shows "Checked In: Yes"; hides button.

**`markAsAttended()`:**
Calls a single Firestore `update()` on `users/{userId}/rsvps/{eventId}` setting:
```java
"checkedIn"    → true
"qrExpired"    → true          // prevents re-use of this QR code
"checkedInAt"  → Timestamp.now()
```
Then mirrors `checkedIn = true` and `checkedInAt` onto `events/{eventId}/attendees/{userId}` to keep the organiser's attendee list consistent.

### 10.2 WhoIsComingActivity — Attendee Management

`com.example.CampusEventDiscovery.ui.organizer.WhoIsComingActivity`

Provides the full attendee management screen for organisers. Displays a live list of all registered attendees with check-in status, search, QR scanning, manual check-in, and blacklisting.

**Real-time listener:**
Uses a Firestore `addSnapshotListener()` on `events/{eventId}/attendees`. The listener is registered in `onResume()` and removed in `onPause()`. Every write to the attendees subcollection (including check-ins from `ScannerActivity`) automatically refreshes the list without any manual polling or refresh button.

```java
attendeeListener = FirebaseFirestore.getInstance()
    .collection("events").document(eventId)
    .collection("attendees")
    .addSnapshotListener((snapshots, error) -> {
        // rebuild adapter list from snapshot documents
    });
```

**Search:** A `TextWatcher` on `etSearchParticipants` calls `adapter.filter(query)` on every character change, enabling live attendee name filtering.

**Manual check-in:** The "Check In" button presents a `MaterialAlertDialog` with an `EditText` for the organiser to type the attendee's `qrCodeToken` directly. On confirm, calls `EventRepository.checkInAttendeeByQrToken()`.

### 10.3 One-Time-Use Enforcement

The one-time-use guarantee is enforced at two independent levels:

1. **`ScannerActivity.markAsAttended()`** — sets `qrExpired = true` on the RSVP document. Subsequent scans of the same QR will call `displayResult()`, which checks `rsvp.isQrExpired()` and shows the "TICKET ALREADY USED" error in red, blocking re-entry.

2. **`EventRepository.checkInAttendeeByQrToken()`** — runs a Firestore transaction that reads the `attendees/{userId}` document and throws `FirebaseFirestoreException(ABORTED)` if `checkedIn == true`. This prevents the manual entry path from re-checking-in an already-attended user.

### 10.4 Blacklist System

`WhoIsComingActivity` enables multi-select on the attendee list via `AttendeeAdapter`. The "Blacklist" button is enabled only when at least one attendee is selected (callback from adapter via `selectedCount > 0`).

On confirm, calls `EventRepository.blacklistAttendees(eventId, selectedAttendees, callback)`, which runs a Firestore transaction that for each blacklisted attendee:
1. Creates `events/{eid}/blacklist/{uid}` with name and timestamp.
2. Deletes `events/{eid}/attendees/{uid}`.
3. Cancels the user's RSVP at `users/{uid}/rsvps/{eid}`.
4. Decrements `events/{eid}.rsvpCount` by the number of removed attendees.

When a blacklisted user subsequently attempts to RSVP, the `rsvpEvent()` transaction reads their blacklist document and throws `FirebaseFirestoreException(PERMISSION_DENIED)`, preventing registration.

---

## 13. EventRepository — Core Data Layer

`com.example.CampusEventDiscovery.repository.EventRepository`

The primary data access class for all Firestore operations. Holds a single `FirebaseFirestore db` instance. Accepts a `FirebaseFirestore` constructor parameter for dependency injection in unit tests; the no-arg constructor calls `FirebaseFirestore.getInstance()`.

Collection name constants are declared as private static final strings to prevent typos across all query paths.

### 11.1 Callback Interfaces

All async operations deliver results through inner functional interfaces:

| Interface | Signature |
|---|---|
| `EventListCallback` | `onSuccess(List<Event>)` / `onError(Exception)` |
| `SingleEventCallback` | `onSuccess(Event)` / `onError(Exception)` |
| `ProposalListCallback` | `onSuccess(List<EventProposal>)` / `onError(Exception)` |
| `ProposalCallback` | `onSuccess(EventProposal)` / `onError(Exception)` |
| `UserCallback` | `onSuccess(User)` / `onError(Exception)` |
| `NotificationListCallback` | `onSuccess(List<Notification>)` / `onError(Exception)` |
| `MemoryListCallback` | `onSuccess(List<Memory>)` / `onError(Exception)` |
| `AttendeeListCallback` | `onSuccess(List<EventAttendee>)` / `onError(Exception)` |
| `FeaturedEventIdsCallback` | `onSuccess(List<String>)` / `onError(Exception)` |
| `BooleanCallback` | `onSuccess(boolean)` / `onError(Exception)` |
| `StringCallback` | `onSuccess(String)` / `onError(Exception)` |
| `ActionCallback` | `onSuccess()` / `onError(Exception)` |

### 11.2 Event Fetching Methods

| Method | Query | Sort |
|---|---|---|
| `getUpcomingEvents(cb)` | `events` where `status == "active"` | Ascending by `date` |
| `getFeaturedEventIds(cb)` | `app_config/settings.featuredEventIds` | As stored |
| `getFeaturedEvents(ids, cb)` | Chunked `whereIn` on document IDs | Preserves input order |
| `getPersonalisedEvents(interests, cb)` | `status == "active"` + `tags arrayContainsAny interests` (max 10) | Ascending by `date` |
| `getEventById(eventId, cb)` | Single document fetch | N/A |
| `searchEvents(query, category, cb)` | All active events; client-side filter on title + category | Ascending by `date` |
| `getOrganizerEvents(organizerId, cb)` | `organizerId == x` + `status == "active"` | Ascending by `date` |
| `getPendingEvents(cb)` | `status == "pending"` | None |

**Chunk fetching:** `fetchEventsByIdsPreserveOrder()` batches IDs into groups of 10 (Firestore `whereIn` limit), fires parallel `Tasks.whenAllSuccess()` across chunks, and reconstructs results in the original input order.

### 11.3 RSVP Transaction (Atomic)

`rsvpEvent(userId, event, fullName, ActionCallback)` runs a `db.runTransaction()` that atomically:

1. Reads the event document and throws `NOT_FOUND` if it does not exist.
2. Reads `users/{uid}/rsvps/{eid}` and `events/{eid}/attendees/{uid}`.
3. Reads `events/{eid}/blacklist/{uid}` and throws `PERMISSION_DENIED` if the user is blacklisted.
4. Throws `ABORTED` with message `"Already registered"` if either attendee or a non-cancelled RSVP document already exists.
5. Reads `capacity` and `rsvpCount` and throws `ABORTED` with message `"Event full"` if `rsvpCount >= capacity`.
6. Generates `qrToken = UUID.randomUUID().toString()`.
7. Writes `users/{uid}/rsvps/{eid}` with fields: `eventId`, `title`, `date`, `status = "confirmed"`, `checkedIn = false`, `qrCodeToken`, `addedToCalendar = false`, `gcalEventId = ""`, `rsvpAt`.
8. Writes `events/{eid}/attendees/{uid}` with fields: `userId`, `fullName`, `qrToken`, `checkedIn = false`, `checkedInAt = null`.
9. Increments `events/{eid}.rsvpCount` by 1 via `FieldValue.increment(1)`.

All eight writes occur in a single transaction, ensuring complete atomicity. No partial state can be written to Firestore.

### 11.4 Cancel RSVP Transaction

`cancelRsvp(userId, eventId, ActionCallback)` runs a transaction that:
1. Reads the event, user RSVP, and attendee documents.
2. If a non-cancelled RSVP or attendee document exists, marks `hadActiveRsvp = true`.
3. Merges `status = "cancelled"` onto the RSVP document.
4. Deletes the attendee document.
5. Decrements `rsvpCount` by 1 only if `hadActiveRsvp` is true, using `Math.max(0L, count - 1L)` as a safety floor.

### 11.5 Organiser & Admin Operations

| Method | Description |
|---|---|
| `proposeEvent(EventProposal, ActionCallback)` | Adds a new document to `event_proposals/` |
| `getOrganizerProposals(organizerId, cb)` | Fetches all proposals for a given organiser, sorted descending by `submittedAt` |
| `getAllPendingProposals(cb)` | One-time fetch of all `status == "pending"` proposals |
| `observeAllPendingProposals(cb)` | Real-time `addSnapshotListener()` for admin dashboard live updates |
| `approveProposal(proposalId, EventProposal, cb)` | `WriteBatch`: updates proposal status, creates event doc, sends organiser notification |
| `rejectProposal(proposalId, note, cb)` | `WriteBatch`: updates proposal status with rejection note, sends organiser notification |
| `getEventAttendees(eventId, cb)` | Fetches `events/{eid}/attendees` sorted alphabetically by `fullName` |
| `blacklistAttendees(eventId, attendees, cb)` | Transaction: creates blacklist docs, deletes attendee docs, cancels RSVPs, decrements count |
| `checkInAttendeeByQrToken(eventId, qrToken, cb)` | Transaction: queries attendees by `qrToken`, checks `checkedIn` flag, marks attendance, increments `checkedInCount` |
| `sendAnnouncementToAttendees(eventId, title, message, cb)` | Fetches all attendees, creates notification documents via `WriteBatch` |
| `sendSosReport(reporterId, reporterName, description, lat, lng, cb)` | Creates a new document in `reports/` |
| `getRsvpQrToken(userId, eventId, cb)` | Fetches `users/{uid}/rsvps/{eid}` and returns `qrCodeToken` for ticket display |

### 11.6 Notification Dispatch

Notifications are created via `buildNotification(title, body, type, eventId)` which constructs a `Notification` object with `isRead = false` and `createdAt = Timestamp.now()`. The document is written to `notifications/{recipientId}/messages/{auto-id}` within batch operations so that notification writes are atomic with their triggering operations.

`markNotificationRead(userId, notificationId)` updates both `read` and `isRead` fields for compatibility with older document schemas.

### 11.7 Rating System

`addRating(eventId, userId, stars, review, ActionCallback)` runs a Firestore transaction that:
1. Reads the current `averageRating` and `ratingCount` from the event document.
2. Reads the user's existing rating document (if any) to extract `previousStars`.
3. Computes the new rolling average:
   - If updating: `newAvg = ((avg * count) - previousStars + newStars) / count`
   - If new: `newAvg = ((avg * count) + newStars) / (count + 1)`
4. Atomically updates `ratingRef` and the event's `averageRating` / `ratingCount`.

### 11.8 SOS Reports

`sendSosReport()` writes a document to `reports/` with the reporter's identity (or `isAnonymous = true` if `reporterId` is empty), description, and a `location` map containing `lat` and `lng` coordinates from the device GPS.

---

## 14. Application Entry Points & Navigation

### 12.1 CampusEventDiscoveryApp

`com.example.CampusEventDiscovery.CampusEventDiscoveryApp`

The custom `Application` class declared in `AndroidManifest.xml`. Its `onCreate()` performs three operations before any Activity starts:

1. **`FirebaseApp.initializeApp(this)`** — initialises all Firebase services.
2. **Firebase App Check configuration** — checks `ApplicationInfo.FLAG_DEBUGGABLE`:
   - Debug builds: installs `DebugAppCheckProviderFactory` (outputs a debug token to Logcat for Firebase Console registration).
   - Release builds: installs `PlayIntegrityAppCheckProviderFactory`.
   - Calls `setTokenAutoRefreshEnabled(true)`.
3. **`ThemeManager.applyStoredTheme(this)`** — applies the stored light/dark preference before the first Activity renders, preventing a flash of the wrong theme.

### 12.2 SplashActivity

`com.example.CampusEventDiscovery.SplashActivity`

The application entry point (`LAUNCHER` intent filter). Displays the splash layout for 1,500 ms via `Handler.postDelayed()`, then calls `routeFromSplash()`.

**Routing logic:**
1. If `FirebaseAuth.getCurrentUser() == null` AND `DevSessionManager.shouldUseBypass()` is true → launch `MainActivity` directly (developer bypass path).
2. Otherwise, query `EventRepository.getMaintenanceMode()`:
   - Maintenance mode OFF → `openDefaultDestination()`: authenticated users go to `MainActivity`, unauthenticated go to `WelcomeActivity`.
   - Maintenance mode ON:
     - No current user → `MaintenanceActivity`.
     - Current user is admin (role check via Firestore) → `MainActivity`.
     - Current user is non-admin → `MaintenanceActivity`.

### 12.3 MainActivity & Role-Aware Navigation

`com.example.CampusEventDiscovery.MainActivity`

The host activity for all authenticated screens. Contains a `BottomNavigationView` and a `fragmentContainer` `FrameLayout`.

**Role resolution:** On `onStart()`, checks for bypass session or Firebase user, fetches the user's Firestore role via `EventRepository.getUserData()`, and stores it in `currentRole`.

**Bottom navigation item mutation:** `updateBottomNavMenu()` changes the centre navigation item's icon and label based on role:
- Attendee: pin icon → "My Events"
- Organiser: add icon → "Create Event" (launches `CreateEventActivity` directly)
- Admin: verified icon → "Approvals" (loads `HomeAdminFragment`)

**Fragment caching:** All fragments are stored in a `HashMap<String, Fragment> fragmentCache`. A fragment is created only once per `MainActivity` instance; subsequent tab switches reuse the cached instance, preserving scroll position and loaded data.

**Navigation transitions:** All fragment swaps use `NavigationTransitions.replace()` with custom slide animations (`screen_enter`, `screen_exit`, `screen_pop_enter`, `screen_pop_exit`) applied only when a fragment is already visible (i.e., not on the initial load).

---

## 15. Utility Classes

### 13.1 ThemeManager

`com.example.CampusEventDiscovery.util.ThemeManager`

Manages the application's light/dark mode preference using `SharedPreferences` (`"app_preferences"` / `"dark_mode_enabled"`). Default is `true` (dark mode).

| Method | Description |
|---|---|
| `applyStoredTheme(Context)` | Called from `CampusEventDiscoveryApp.onCreate()` — applies stored preference before first Activity |
| `applyThemePreference(Context, boolean)` | Saves and applies theme; returns `true` if `recreate()` is needed |
| `syncThemePreference(Context, boolean)` | Called after sign-in to sync Firestore `darkMode` field with local preference |
| `isDarkModeEnabled(Context)` | Returns stored preference |

Uses `AppCompatDelegate.setDefaultNightMode()` with `MODE_NIGHT_YES` / `MODE_NIGHT_NO`. Returns `false` if the target mode is already set, avoiding unnecessary `recreate()` calls.

### 13.2 EventValidator

`com.example.CampusEventDiscovery.util.EventValidator`

Stateless validator for event proposal data. `validate(title, description, location, timestamp, capacity, category, organizerId)` returns an error string or `null`.

Valid categories: `Music`, `Sports`, `Career`, `Academic`, `Arts`, `Business`, `Food & Bev`, `Social`.

Validation rules include: non-empty fields, `timestamp > System.currentTimeMillis()` (future date), `capacity >= 1`, and `category` membership in the allowed set.

### 13.3 NavigationTransitions

`com.example.CampusEventDiscovery.util.NavigationTransitions`

Single static method `replace(FragmentManager, containerId, Fragment, addToBackStack, animate)`. When `animate = true`, applies `setCustomAnimations()` using the four slide animation resources. Used by `MainActivity.loadFragment()`.

---

## 16. UI Layer — Activities & Fragments

| Class | Role | Key Behaviour |
|---|---|---|
| `WelcomeActivity` | Unauthenticated landing | Offers "Sign In" and "Sign Up" navigation |
| `MaintenanceActivity` | Maintenance screen | Shown to non-admin users when `maintenanceMode == true` |
| `HomeFragment` | Attendee home | Featured events carousel + personalised event feed; uses `getPersonalisedEvents()` |
| `HomeOrganizerFragment` | Organiser home | Organiser's event list + pending proposal status cards |
| `HomeAdminFragment` | Admin home | Real-time pending proposals list via `observeAllPendingProposals()` |
| `SearchFragment` | Event search | Debounced search with category filter chips |
| `FavouritesFragment` | Saved events | Loads `getSavedEvents()`; swipe-to-unsave |
| `MyEventsFragment` | RSVP list | Loads `getRsvps()`; tap to view ticket |
| `EventCalendarFragment` | Personal calendar | Displays RSVP'd events on a calendar view |
| `ProfileFragment` | User profile | Shows user data; navigation to settings, notifications, memories |
| `EventDetailActivity` | Event detail | Full event info; RSVP / Buy Ticket / Save / Rate / Feedback |
| `BuyTicketActivity` | Ticket selection | Tier picker → CheckoutActivity |
| `CheckoutActivity` | Payment form | Payment method selection, duplicate check, RSVP transaction, QR generation |
| `TicketActivity` | QR ticket display | Renders QR bitmap; displays transaction ID |
| `EventFeedbackActivity` | Post-event feedback | Star rating + text review submission |
| `EventApprovalActivity` | Admin approval | Approve / reject a single event document |
| `OrganizerProposalDetailActivity` | Organiser view | View proposal status and admin note |
| `CreateEventActivity` | Event proposal | Organiser submits a new event proposal with image upload |
| `ManageEventsActivity` | Organiser events | List of organiser's active events |
| `OrganizerEventDetailActivity` | Organiser event | Event stats, attendee count, announcements, who-is-coming |
| `WhoIsComingActivity` | Attendee management | Real-time list, search, scan, manual check-in, blacklist |
| `ScannerActivity` | QR scanner | Camera scan, JSON parse, Firestore lookup, mark attended |
| `AdminHomeActivity` | Admin dashboard | Proposal queue, maintenance toggle, featured events |
| `AccountSettingsActivity` | Profile editing | Update name, university, location, interests, profile picture |
| `NotificationCenterActivity` | Notifications | In-app notification list; mark read |
| `MemoriesActivity` | Memories | Post-event photo + rating memories |

---

## 17. RecyclerView Adapters

| Class | Used In | Description |
|---|---|---|
| `EventAdapter` | `HomeFragment`, `SearchFragment`, `FavouritesFragment`, `MyEventsFragment` | Renders event cards; uses Glide for image loading |
| `AttendeeAdapter` | `WhoIsComingActivity` | Multi-select attendee list; `filter(query)` for live search; check-in badge display |
| `TicketTierAdapter` | `BuyTicketActivity` | Ticket tier cards with quantity selectors; reports total via `onTotalChanged` callback |
| `NotificationAdapter` | `NotificationCenterActivity` | Notification list with read/unread state |
| `MemoryAdapter` | `MemoriesActivity` | Memory cards with photo grid and star rating |
| `OrganizerPendingAdapter` | `HomeAdminFragment` | Pending proposal cards with approve/reject action buttons |

---

## 18. Test Suite

All unit tests are located under `app/src/test/java/com/example/CampusEventDiscovery/`. The test framework uses JUnit 4, Mockito 5, and Robolectric 4.11.1.

| Test Class | Coverage |
|---|---|
| `AuthRepositoryTest` | Tests `MockAuthRepository`: valid signup, duplicate email, login success, wrong password, admin shortcut |
| `SignupValidatorTest` | Exhaustive validation rule coverage: empty fields, invalid email, short password, missing uppercase/lowercase/digit/special character, password mismatch, invalid role |
| `EventValidatorTest` | Tests all `EventValidator` rules: empty title/description/location, past date, zero capacity, invalid/missing category, empty organiser ID |
| `UserRolesTest` | Tests `UserRoles.sanitize()`, `isAttendee()`, `isOrganizer()`, `isAdmin()` for all valid roles, null, empty string, mixed case |
| `EventRepositoryTest` | Tests `EventRepository` with injected mock `FirebaseFirestore` via constructor |
| `RsvpManagerTest` | Tests RSVP creation, cancellation, and duplicate prevention logic |
| `UITest` | Robolectric-based Activity tests for login/signup screen rendering |
| `ExampleUnitTest` | Placeholder — basic arithmetic assertion |

**Test dependencies:**
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("org.mockito:mockito-core:5.11.0")
testImplementation("org.mockito:mockito-inline:5.2.0")
testImplementation("androidx.test:core:1.6.1")
```

`isIncludeAndroidResources = true` is set in `testOptions` to allow Robolectric tests to access `R` resources.

---

## 19. Build Configuration & Dependencies

**`app/build.gradle.kts`:**

```kotlin
android {
    namespace       = "com.example.CampusEventDiscovery"
    compileSdk      = 36
    defaultConfig {
        applicationId = "com.example.CampusEventDiscovery"
        minSdk        = 24
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

**Runtime dependencies:**

| Dependency | Version | Purpose |
|---|---|---|
| `firebase-bom` | 34.11.0 | Firebase Bill of Materials — manages all Firebase SDK versions |
| `firebase-auth` | (BOM managed) | Email/password authentication |
| `firebase-firestore` | (BOM managed) | NoSQL document database |
| `firebase-storage` | (BOM managed) | Profile picture and event image uploads |
| `firebase-appcheck-playintegrity` | (BOM managed) | Production App Check provider |
| `firebase-appcheck-debug` | (BOM managed) | Debug App Check provider |
| `androidx.appcompat` | 1.7.1 | AppCompatActivity, AppCompatDelegate |
| `com.google.android.material` | 1.13.0 | MaterialButton, BottomNavigationView, MaterialToolbar, MaterialCardView |
| `androidx.recyclerview` | 1.3.2 | RecyclerView for all list screens |
| `androidx.cardview` | 1.0.0 | Card containers |
| `com.github.bumptech.glide` | 4.16.0 | Async image loading and caching |
| `de.hdodenhof:circleimageview` | 3.1.0 | Circular profile picture views |
| `com.journeyapps:zxing-android-embedded` | 4.3.0 | ZXing camera scanner UI for `ScannerActivity` |
| `com.google.zxing:core` | 3.5.3 | QR encoding engine for `QRCodeHelper` |

---

## 20. Permissions

Declared in `AndroidManifest.xml`:

| Permission | Reason |
|---|---|
| `INTERNET` | All Firebase SDK network communication |
| `ACCESS_COARSE_LOCATION` | SOS report location (approximate) |
| `ACCESS_FINE_LOCATION` | SOS report location (precise GPS) |
| `CAMERA` | `ScannerActivity` — QR code camera scanning |
| `READ_EXTERNAL_STORAGE` (maxSdkVersion 32) | Firebase Storage `putFile(Uri)` on Android ≤ 12 |
| `READ_MEDIA_IMAGES` | Firebase Storage `putFile(Uri)` on Android 13+ |

The `android.hardware.camera` feature is declared with `required="false"` to allow installation on devices without a rear camera (manual entry fallback is available in `WhoIsComingActivity`).

The `enableOnBackInvokedCallback="true"` application attribute opts into the Android 13+ predictive back gesture.

---

## 21. Screenshots

> **Note to reviewer:** Screenshots are to be added manually. Suggested captures are listed below.

| Screen | Suggested File Name |
|---|---|
| Splash screen | `screenshot_splash.png` |
| Welcome / landing screen | `screenshot_welcome.png` |
| Sign-in screen | `screenshot_signin.png` |
| Sign-up screen | `screenshot_signup.png` |
| Attendee home (featured + personalised feed) | `screenshot_home_attendee.png` |
| Event detail screen | `screenshot_event_detail.png` |
| Ticket tier selection (`BuyTicketActivity`) | `screenshot_buy_ticket.png` |
| Checkout form (`CheckoutActivity`) | `screenshot_checkout.png` |
| QR code ticket (`TicketActivity`) | `screenshot_ticket_qr.png` |
| Organiser home | `screenshot_home_organiser.png` |
| Who Is Coming (attendee list) | `screenshot_who_is_coming.png` |
| QR scanner result (`ScannerActivity`) | `screenshot_scanner_result.png` |
| Admin home (pending proposals) | `screenshot_home_admin.png` |
| Profile screen | `screenshot_profile.png` |

Place screenshots in `docs/images/` and reference them here as:
```markdown
![Screen Name](docs/images/screenshot_name.png)
```

---

## 22. Known Limitations & Future Work

| Area | Current State | Production Requirement |
|---|---|---|
| **Payment Gateway** | `MockPaymentService` always returns `SUCCESS`; no real money moves | Integrate Stripe, Braintree, or JazzCash REST API |
| **QR Scanner** | `ScannerActivity` uses ZXing camera scan; `WhoIsComingActivity` also provides manual token entry | Camera scan fully functional for demo; production may add faster scan modes |
| **Check-in Atomicity** | `ScannerActivity.markAsAttended()` uses a single `update()` call; `checkInAttendeeByQrToken()` uses a transaction | Camera-based scan path should also wrap in a transaction to handle high-concurrency events |
| **Push Notifications** | Notification documents are written to Firestore; no FCM dispatch implemented | Firebase Cloud Functions required to send FCM messages from `fcmToken` on document write |
| **Google Calendar Integration** | `addedToCalendar` and `gcalEventId` fields present; UI option visible | Requires Google Sign-In OAuth scope and Calendar REST API integration |
| **`google-services.json`** | Excluded from version control; shared out-of-band | CI/CD pipeline secret injection (GitHub Actions secrets or Firebase App Distribution) |
| **Firebase App Check (debug)** | Debug token logged to Logcat; must be registered in Firebase Console | Automated debug token rotation for team members |
| **Profile Picture Upload** | `AccountSettingsActivity` and `CreateEventActivity` support image selection | Firebase Storage security rules must be tightened for production |
| **Ticket Tier Pricing** | Hardcoded in `BuyTicketActivity`; not read from Firestore | `ticketPrice` field on `Event` document should drive tier generation dynamically |
| **SOS Location** | GPS coordinates captured; no map view or admin console for reports | Admin report screen with map pins required |

---

## 23. Team

**CS360 Software Engineering — Spring 2026, LUMS**

| Member | Role | Primary Responsibilities |
|---|---|---|
| Saad | Scrum Master / Integration Lead | Firebase setup, authentication system, payment pipeline, QR generation & check-in, branch integration |
| Ammar | Project Owner / Firebase Lead | Core architecture, base branch maintenance |
| Hussain | Event Model / RSVP | Event model, RSVP flows, auth patterns |
| Yahya | Event Browsing / Search | Event discovery, search, `EventRepository`, database schema |
| Nausherwan | Registration / Organiser Features | Registration system, organiser dashboard, extended testing |

**Supervisors:** Dr. Suleman Shahid · Dr. Abdul Ali Bangash

**Repository:** [https://github.com/CS360S26gemini/Campus-Event-Discovery-and-Management-Platform](https://github.com/CS360S26gemini/Campus-Event-Discovery-and-Management-Platform)

---

*Last updated: April 7, 2026 — Branch: `nausher-final-fix`*
