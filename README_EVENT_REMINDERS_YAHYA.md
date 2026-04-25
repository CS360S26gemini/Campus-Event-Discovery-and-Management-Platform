# Event Reminders Implementation Notes

## Branch

`event-reminders-yahya`

## Commit

`aff427a`

## Scope Implemented

This branch implements the Event Reminders feature described in the sprint manual.

### Android app changes

- Added a dedicated reminder notification channel in `CampusEventDiscoveryApp.java`
- Added `EVENT_REMINDER` handling in `MyFirebaseMessagingService.java`
- Built reminder notifications from incoming FCM data payloads
- Routed reminder notification taps into the calendar flow through `MainActivity.java`
- Added reminder routing constants in `Constants.java`

### Backend changes

- Added scheduled reminder fan-out logic in `functions/index.js`
- Added Firebase deployment config in `firebase.json`
- Added Firestore composite index definition in `firestore.indexes.json`

### Test coverage

- Extended `ConstantsTest.java`
- Added `MyFirebaseMessagingServiceTest.java`

### Coordination / merge notes

- Added `docs/conflicts_yahya.md` for merge visibility

## Manual Verification Completed

The feature was manually verified on a physical Android device.

Verified behaviors:

- FCM reminder payload reached the app successfully
- Notification was displayed on the phone
- Tapping the notification routed into the calendar flow

Observed verification logs:

- `FCM_SERVICE: Reminder received eventId=test-event-id, destinationTab=calendar, title=3 Days left to Hack Night`
- `MAIN_INTENT: destinationTab=calendar`

## Files Included

- `app/src/main/java/com/example/CampusEventDiscovery/CampusEventDiscoveryApp.java`
- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/Constants.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingService.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ConstantsTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingServiceTest.java`
- `docs/conflicts_yahya.md`
- `firebase.json`
- `firestore.indexes.json`
- `functions/index.js`

## Shortcoming

The code implementation and device-level verification are complete, but automatic daily reminder deployment is currently blocked because the Firebase project `campuseventdiscovery-87f4f` is not yet on the Blaze plan. As a result, the scheduled Cloud Function cannot be deployed until billing is enabled.
