# SOS Emergency Alert System Implementation

This document details the architecture, implementation logic, and code structure for the High-Priority SOS Alert system within the Campus Event Discovery platform.

---

## 1. Architecture Overview

The SOS system is designed to provide immediate, "break-through" alerts to campus administrators and event organizers when an attendee triggers an emergency.

### High-Level Flow
1.  **Trigger:** An Attendee triggers an SOS from the `HomeFragment` or `SosActivity`.
2.  **Data Capture:** The app captures the user's GPS coordinates (Latitude/Longitude) and links the alert to the current event.
3.  **Persistence:** A document is written to the `alerts` collection in Firestore via `EventRepository.sendSosReport`.
4.  **Routing (Cloud Logic):** A Firebase Cloud Function (recommended) listens for new documents in `alerts`. It identifies the `organizerId` of the event and all users with the `admin` role.
5.  **Messaging:** The system sends a **High-Priority FCM Data Message** to the targeted recipients.
6.  **Reception:** The recipient's device (Organizer/Admin) receives the message through `MyFirebaseMessagingService`.
7.  **The "Big Alert":** The service triggers a `fullScreenIntent` which launches `SOSAlertActivity` immediately, even if the device is locked.

---

## 2. Implementation Details

### A. User Side (The Attendee)
*   **Location Capture:** Uses `FusedLocationProviderClient` to get the most accurate current location.
*   **Firestore Write:** `EventRepository.java` uses the `sendSosReport` method.
    *   **Collection:** `alerts`
    *   **Fields:** `reporterId`, `reporterName`, `eventId`, `description`, `location` (Geopoint/Lat-Lng), `status: "open"`, `submittedAt`.

### B. Recipient Side (Organizer & Admin)
To ensure the alert is seen immediately, we implement a "Big Alert" pattern:

1.  **Notification Channel:** Initialized in `CampusEventDiscoveryApp.java` with `IMPORTANCE_HIGH`, custom sound (ringtone), and a persistent vibration pattern (`1s on, 0.5s off`).
2.  **FCM Service (`MyFirebaseMessagingService.java`):**
    *   Listens for messages where `type == "SOS_ALERT"`.
    *   Builds a notification using `NotificationCompat.Builder`.
    *   **Key Property:** `.setFullScreenIntent(pendingIntent, true)`. This is what allows the Activity to pop up over other apps.
3.  **Alert Activity (`SOSAlertActivity.java`):**
    *   Displays high-contrast emergency UI (Red background).
    *   Uses `setShowWhenLocked(true)` and `setTurnScreenOn(true)` to bypass the keyguard.
    *   Provides a "Dismiss" button to close the alert.

---

## 3. Code Structure

### Core Classes
*   **`util/MyFirebaseMessagingService.java`**: The engine for receiving alerts. It parses the FCM data payload (reporter, description, mapsUrl) and generates the system notification.
*   **`ui/sos/SOSAlertActivity.java`**: The "Big Alert" UI. It handles the window flags required to wake the screen and show over the lock screen.
*   **`repository/EventRepository.java`**: Contains `sendSosReport(...)`, the entry point for persisting the emergency data.
*   **`CampusEventDiscoveryApp.java`**: Responsible for the one-time setup of the `SOS_ALERTS_CHANNEL`.

### Android Manifest Configuration
*   **Permissions:**
    *   `POST_NOTIFICATIONS`: Required for Android 13+.
    *   `USE_FULL_SCREEN_INTENT`: Required for the activity to pop up automatically.
    *   `ACCESS_FINE_LOCATION`: Required for the attendee to share coordinates.
*   **Service & Activity:**
    *   `MyFirebaseMessagingService` registered with `com.google.firebase.MESSAGING_EVENT`.
    *   `SOSAlertActivity` configured with `launchMode="singleInstance"` and `showOnLockScreen="true"`.

---

## 4. Testing Procedures

### Unit & Integration Testing
1.  **Repository Test:** Verify that calling `sendSosReport` creates a valid document in the `alerts` collection with a server timestamp.
2.  **Location Permission Test:** Ensure the SOS button correctly prompts for location permissions if not already granted.

### Manual "Big Alert" Test
To test the full implementation without a backend:
1.  **FCM Mocking:** Use the Firebase Console "Messaging" tool or a tool like Postman to send a **Data Message** (not a Notification Message) to a specific device token.
2.  **Payload:**
    ```json
    {
      "to": "<DEVICE_TOKEN>",
      "data": {
        "type": "SOS_ALERT",
        "reporter": "John Doe",
        "description": "Medical Emergency at Hall A",
        "eventName": "Tech Symposium"
      },
      "priority": "high"
    }
    ```
3.  **Verification:**
    *   Lock the recipient device.
    *   Send the payload.
    *   **Expected Result:** The screen should wake up, and `SOSAlertActivity` should appear immediately without requiring a swipe or unlock.

---

## 5. Security & Constraints
*   **Alert Routing:** Alerts are strictly restricted to the specific `organizerId` associated with the event and all users with `role: "admin"`.
*   **Channel Importance:** Users can manually downgrade notification importance in system settings; the app reinforces `IMPORTANCE_HIGH` on every launch.
