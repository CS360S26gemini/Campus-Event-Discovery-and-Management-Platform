# Technical Audit: Bugs, Redundancies, and Inefficiencies

This report identifies current technical debt, performance bottlenecks, and architectural weaknesses in the Campus Event Discovery application.

---

## 1. Bugs & Functional Issues

### 1.1 Redundant Data Loading (HomeFragment)
*   **Description:** `loadHomeData()` is invoked in both `onViewCreated` and `onResume`. 
*   **Why:** When the fragment is first created, `onViewCreated` triggers a fetch. Immediately after, `onResume` triggers the exact same set of Firestore queries.
*   **Impact:** Double network usage on app start and every time a user returns to the Home tab. This increases Firestore read costs and causes UI "flicker" as lists refresh twice.

### 1.2 Fragment Overlap in MainActivity
*   **Description:** `MainActivity` uses a manual `Map<String, Fragment> fragmentCache`. 
*   **Why:** While intended to save state, Android's `FragmentManager` already handles fragment restoration on configuration changes (like screen rotation). By creating a new cache and not checking if the `FragmentManager` already has a fragment with that tag, the app risks having "ghost" fragments or inconsistent UI states.
*   **Impact:** Potential memory leaks and UI glitches during screen rotation or theme switching.

### 1.3 Callback Hell (SosActivity)
*   **Description:** The SOS triggering logic is deeply nested: `requestCurrentLocation` -> `fetchUserDisplayName` -> `fetchAdminsAndSend` -> `writeAlert`.
*   **Why:** Sequential asynchronous calls are handled via nested anonymous listeners rather than using Tasks/Coroutines or a flat reactive chain.
*   **Impact:** High maintainability risk. If one step in the chain fails (e.g., location timeout), the user might be left on a loading screen indefinitely.

### 1.4 Deprecated UI Components (SosActivity)
*   **Description:** Usage of `ProgressDialog`.
*   **Why:** `ProgressDialog` was deprecated in API 26 as it prevents users from interacting with the app in a way that modern Android guidelines discourage.
*   **Impact:** Visual inconsistency with the rest of the Material 3/Material Design components and potential issues with accessibility services.

---

## 2. Redundancies & Performance Inefficiencies

### 2.1 Waterfall Fetching (HomeFragment)
*   **Description:** The app fetches user data, *then* fetches upcoming events, *then* fetches featured event IDs, *then* fetches featured event details.
*   **Why:** Data dependencies are handled sequentially.
*   **Impact:** Significant latency. The user sees a blank screen for several seconds as 4-5 network round-trips happen one after another instead of in parallel.

### 2.2 Client-Side Notification Fan-out (SosRepository)
*   **Description:** When an SOS is sent, the Android app iterates through a list of Admin IDs and writes a notification document for *each* admin manually.
*   **Why:** The routing logic is implemented on the client side.
*   **Impact:** If there are 10 admins, the app makes 11 Firestore writes (1 alert + 10 notifications). This is slow, battery-intensive, and will fail if the user loses connection mid-loop.

### 2.3 Redundant FCM Token Updates
*   **Description:** `MyFirebaseMessagingService.onNewToken` updates Firestore every time a token is generated, but `MainActivity` also fetches user data every start.
*   **Why:** Lack of a centralized sync strategy for device metadata.
*   **Impact:** Minor, but creates unnecessary write operations if the token hasn't actually changed.

---

## 3. Architectural Redundancy

### 3.1 Lack of ViewModel Layer
*   **Description:** Most UI logic (filtering, string formatting, data mapping) resides directly in the `Fragment` or `Activity`.
*   **Why:** Traditional "Activity-as-God-Object" pattern.
*   **Impact:** Data is lost on screen rotation. If a user is half-way through a search and rotates the phone, the query and results are often wiped and re-fetched.

### 3.2 String Literal Concatenation
*   **Description:** Multiple instances of `tvReporter.setText("Reporter: " + reporter)`.
*   **Why:** Bypassing the Android resource system.
*   **Impact:** Breaks Internationalization (i18n). The app cannot be easily translated to other languages because the labels are hardcoded in Java logic.

---

## 4. Recommended Improvements

### 1. Implement ViewModels & LiveData
*   Migrate `EventRepository` calls to `ViewModel` classes.
*   Use `LiveData` or `StateFlow` to observe Firestore changes so the UI updates automatically without manual "refresh" buttons.

### 2. Parallelize Network Requests
*   Use `Tasks.whenAll(...)` in the repository to fetch user data and upcoming events simultaneously, reducing initial load time by ~50%.

### 3. Move Logic to Cloud Functions
*   The `SosRepository` fan-out should be moved to a Firestore Trigger. The app should only write *one* document to `alerts`, and the backend should handle notifying the 10+ admins.

### 4. Implement Local Caching
*   Enable Firestore Offline Persistence or implement a local Room database. This allows the app to show the "last known" events immediately while the network fetches updates in the background.

### 5. Centralize Constants
*   Move strings like `"SOS_ALERT"`, `"admin"`, and collection names into a single `Constants` utility class to prevent typos and make schema changes easier.
