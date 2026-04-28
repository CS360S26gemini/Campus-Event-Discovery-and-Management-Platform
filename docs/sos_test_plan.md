# SOS Emergency Feature — Manual Test Plan (Sprint 2)

Each scenario below is formatted as a GitHub issue template so it can be copy-pasted into the project tracker.

---
## Issue 1: SOS button visible to authenticated attendee only

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Confirms the red SOS button on EventDetailActivity appears only for a signed-in user whose role is `attendee`.

### Preconditions
- App installed and launched on a physical device or emulator.
- A Firestore user exists with `role = "attendee"`.
- The attendee is signed in.
- At least one published event is available.

### Steps
1. Open the Events list.
2. Tap any event to open EventDetailActivity.
3. Scroll to the ticket/registration area.

### Expected Result
The SOS button is visible above the ticket row with a red background and white label.

### Edge Case Risk
If role resolution fails silently, the button may never appear for a legitimate attendee — blocking the entire feature.

---
## Issue 2: SOS button hidden for organizer role

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Verifies that users whose role is `organizer` do not see the SOS button on the event detail screen.

### Preconditions
- A user signed in with `role = "organizer"`.
- Any event loaded in EventDetailActivity.

### Steps
1. Sign in as an organizer.
2. Open EventDetailActivity for any event.
3. Inspect the area above the ticket row.

### Expected Result
The SOS button is hidden (`View.GONE`). No space is reserved.

### Edge Case Risk
Organizers accidentally sending SOS alerts about their own events would poison the dashboard with self-reports.

---
## Issue 3: SOS button hidden for admin role

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Verifies admins do not see the attendee-only SOS trigger button.

### Preconditions
- A user signed in with `role = "admin"`.
- EventDetailActivity open.

### Steps
1. Sign in as an admin.
2. Open any event's detail screen.

### Expected Result
The SOS button is not rendered.

### Edge Case Risk
Admins self-triggering SOS would flood the dashboard and trigger unnecessary full-screen alarms on other admin devices.

---
## Issue 4: SOS button hidden when not logged in

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Verifies that anonymous or signed-out users cannot see or trigger SOS.

### Preconditions
- App is launched in signed-out state.
- No DevSessionManager bypass user is active.

### Steps
1. Ensure no user is signed in (sign out if needed).
2. Browse to an event detail screen.

### Expected Result
The SOS button is hidden. Registration CTA reads "Sign in to register".

### Edge Case Risk
Anonymous SOS writes would violate Firestore security rules and could be abused to spam the alert dashboard.

---
## Issue 5: Confirmation dialog appears on SOS tap

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Tapping the SOS button must first display a confirmation dialog — no silent sends.

### Preconditions
- Signed in as attendee.
- EventDetailActivity showing an event.

### Steps
1. Tap the red SOS button.

### Expected Result
An AlertDialog appears titled "Send SOS Alert" with the body explaining that location will be shared. Two buttons: "Send Alert" and "Cancel". Dialog is non-cancelable by tapping outside.

### Edge Case Risk
Missing confirmation → accidental taps fire real alerts and wake all admins.

---
## Issue 6: Cancelling dialog does not send alert

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Verifies that tapping Cancel aborts the SOS flow with zero Firestore writes.

### Preconditions
- Confirmation dialog is displayed.

### Steps
1. Tap "Cancel" on the dialog.

### Expected Result
SosActivity finishes immediately. No `sos_alerts` document is created (verify in Firestore console). User is returned to the event detail screen.

### Edge Case Risk
If cancellation still triggered the write path, the cooldown would also engage and lock the user out unnecessarily.

---
## Issue 7: Alert writes to sos_alerts Firestore collection

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Confirms a new document is created in `sos_alerts` after the user confirms.

### Preconditions
- Signed in as attendee with location permission granted.
- Firestore console open on `sos_alerts`.

### Steps
1. Tap SOS → Send Alert.
2. Wait for the success screen.
3. Refresh the Firestore console.

### Expected Result
A new document appears in `sos_alerts` with an auto-generated ID.

### Edge Case Risk
WriteBatch failure would silently lose the alert — the user thinks help is coming but nothing was persisted.

---
## Issue 8: All required fields present in sos_alerts document

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Every alert document must contain the full schema: userId, displayName, eventId, eventName, organizerId, latitude, longitude, mapsUrl, timestamp, status.

### Preconditions
- An alert document has just been written (see Issue 7).

### Steps
1. Open the newly-created `sos_alerts/{id}` document in Firestore console.
2. Inspect each field.

### Expected Result
All 10 fields are present with correct types (String, double, long).

### Edge Case Risk
A missing `organizerId` breaks the organizer-scoped dashboard query; a missing `timestamp` breaks `orderBy` sorting.

---
## Issue 9: Organizer receives notification in Firestore

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
The event's organizer must receive a notification doc under `notifications/{organizerId}/messages/{auto-id}`.

### Preconditions
- The event being tested has a valid `organizerId` field.
- Alert has just been sent.

### Steps
1. Note the event's organizerId.
2. Navigate to `notifications/{organizerId}/messages` in Firestore.

### Expected Result
A new message document exists with title "SOS Alert at Your Event" and type `sos_alert`.

### Edge Case Risk
Fan-out failure would mean the organizer never learns about the emergency at their own event.

---
## Issue 10: Admin receives notification in Firestore

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Every user with `role = "admin"` should receive a notification document for the alert.

### Preconditions
- At least two admin users exist in the `users` collection.
- A fresh alert has been sent.

### Steps
1. For each admin UID, open `notifications/{adminId}/messages`.

### Expected Result
Each admin has a newly-created message document titled "SOS Alert" with type `sos_alert` pointing to the event.

### Edge Case Risk
Partial fan-out (e.g. one admin missed) defeats redundancy — the alert might never be seen if the primary admin is offline.

---
## Issue 11: mapsUrl opens in browser when tapped

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
The success screen shows the mapsUrl as a tappable link that opens Google Maps in the browser.

### Preconditions
- Alert send succeeded, success screen is showing.

### Steps
1. Tap the `https://maps.google.com/?q=...` text.

### Expected Result
External browser or Google Maps app opens with a pin on the reported coordinates.

### Edge Case Risk
If ACTION_VIEW is not handled (no browser / no maps app), the app must not crash — the catch block swallows the exception.

---
## Issue 12: Success screen shows correct message

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
After a successful send, the user sees reassurance text that the organizer and admins have been notified.

### Preconditions
- Alert sent successfully.

### Steps
1. Wait for the progress dialog to dismiss.

### Expected Result
Visible text reads: "Help is on the way. The event organizer and administrators have been notified." along with the mapsUrl and a Close button.

### Edge Case Risk
Wrong copy could lead the user to believe no one was notified and call 911 unnecessarily — or worse, to doubt that help is coming.

---
## Issue 13: SOS button disabled for 60 seconds after send

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
After a successful send, returning to EventDetailActivity shows the SOS button greyed out for the cooldown window.

### Preconditions
- An alert has just been sent (timestamp saved under `sos_prefs.last_sent_ts`).

### Steps
1. Close SosActivity.
2. Observe the SOS button on EventDetailActivity.

### Expected Result
Button alpha is 0.6 and `setEnabled(false)`. After 60 seconds elapse it re-enables automatically.

### Edge Case Risk
If cooldown does not re-arm after 60s the button stays dead for the entire session, blocking a legitimate second emergency.

---
## Issue 14: Second SOS tap within 60 seconds is blocked

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Even if the user finds a way to tap the button during cooldown, the tap must be rejected with a Toast.

### Preconditions
- Within 60s of a prior successful send.

### Steps
1. Tap the (still-visible) SOS button rapidly.

### Expected Result
Toast shown: "Please wait Ns before sending another SOS." No confirmation dialog. No Firestore write.

### Edge Case Risk
Without this guard, a panicked user mashing the button could fan out duplicate alerts and drain cooldown logic on the backend.

---
## Issue 15: Location unavailable — alert still sends with 0.0 coordinates

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
If FusedLocationProviderClient returns null (or times out after 10s), the alert must still fire with lat/lng 0.0.

### Preconditions
- Enable airplane mode or disable GPS on the device.
- Location permission previously granted.

### Steps
1. Tap SOS → Send Alert.
2. Wait for timeout (~10s).

### Expected Result
Alert is written with `latitude = 0.0`, `longitude = 0.0`, and the mapsUrl points to `0.0,0.0`. Success screen still appears.

### Edge Case Risk
Blocking the send on GPS unavailability defeats the whole purpose — the attendee needs help *especially* when their device is struggling.

---
## Issue 16: Location permission denied — graceful error shown

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Denying the runtime permission prompt must not crash and must inform the user.

### Preconditions
- App has never been granted `ACCESS_FINE_LOCATION`.

### Steps
1. Tap SOS → Send Alert.
2. Tap "Deny" on the permission dialog.

### Expected Result
Toast shown: "Location permission required to send SOS alert." SosActivity finishes. No Firestore write.

### Edge Case Risk
A crash here would scare the user during an emergency. An infinite spinner would be equally bad.

---
## Issue 17: No internet — failure Toast shown, no crash

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
If the device is offline, the WriteBatch commit fails and the user must be told.

### Preconditions
- Enable airplane mode (Wi-Fi and cellular both off).

### Steps
1. Tap SOS → Send Alert → (optionally grant location).
2. Wait for commit to fail.

### Expected Result
Toast shown: "Failed to send SOS. Please call security directly." No success screen. App remains stable.

### Edge Case Risk
The user must know to fall back to a phone call — silent failure here is dangerous.

---
## Issue 18: Empty organizerId — no organizer notification written

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
If the event doc has no `organizerId`, the repository must skip the organizer notification write but still write the alert and admin notifications.

### Preconditions
- An event exists whose `organizerId` field is absent or empty.

### Steps
1. Trigger SOS on that event.
2. Check `sos_alerts` (should have new doc).
3. Check `notifications/` — no organizer entry should appear.
4. Check each admin's messages subcollection.

### Expected Result
`sos_alerts` document created. No organizer notification. All admins still notified.

### Edge Case Risk
Attempting to write to `notifications/""/messages/...` would throw a Firestore IllegalArgumentException and abort the whole batch.

---
## Issue 19: SOS alert status field equals ACTIVE

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
Every newly-written alert document must carry `status = "ACTIVE"` so the dashboard can filter unresolved alerts.

### Preconditions
- Fresh alert just written.

### Steps
1. Open the `sos_alerts/{id}` document.
2. Read the `status` field.

### Expected Result
Exact value is the string `ACTIVE`.

### Edge Case Risk
Wrong casing (`active`) or missing status breaks any future filter query that uses `whereEqualTo("status", "ACTIVE")`.

---
## Issue 20: displayName populated from Firestore users collection

**Labels:** `testing`, `sos`, `sprint-2`
**Assignee:** Saad

### Description
The `displayName` in the alert must be sourced from `users/{uid}.fullName`, falling back to "Unknown User".

### Preconditions
- Signed-in attendee has a `fullName` field set in `users/{uid}`.

### Steps
1. Verify `users/{uid}.fullName` is populated (e.g. "Jane Doe").
2. Trigger SOS.
3. Inspect the new `sos_alerts` doc.

### Expected Result
`displayName == "Jane Doe"`.
If `fullName` is missing or the users doc lookup fails, the alert contains `displayName == "Unknown User"` (no crash).

### Edge Case Risk
An anonymous-looking alert ("Unknown User") when a full name is available makes triage harder — responders can't call the person by name.

---
