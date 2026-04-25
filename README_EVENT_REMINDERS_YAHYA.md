# Event Reminders Implementation Notes

## Branch / Commit Context

- Branch: `event-reminders-yahya`
- Core reminder feature commit: `aff427a`
- GitHub Actions scheduler follow-up: `77b423b`

## What Was Implemented

This work implements the Event Reminders feature from the sprint manual.

### Android app changes

- Added a dedicated reminder notification channel in `CampusEventDiscoveryApp.java`
- Added `EVENT_REMINDER` handling in `MyFirebaseMessagingService.java`
- Built reminder notifications from incoming FCM data payloads
- Routed reminder notification taps into the calendar flow through `MainActivity.java`
- Added reminder routing constants in `Constants.java`

### Backend / scheduling changes

- Added reminder sender logic in `functions/index.js`
- Added Firebase deployment config in `firebase.json`
- Added Firestore composite index definition in `firestore.indexes.json`
- Added GitHub Actions scheduler in `.github/workflows/send-reminders.yml`

### Coordination changes

- Added `docs/conflicts_yahya.md` for merge visibility

## Tests And Verification

### Unit tests added

- `app/src/test/java/com/example/CampusEventDiscovery/ConstantsTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingServiceTest.java`

### Build verification completed

- `.\gradlew.bat :app:assembleDebug`
- targeted reminder-related unit tests were run during implementation

### Physical device verification completed

The reminder flow was tested on a real Android device.

Confirmed:

- FCM payload reached the app
- notification appeared visibly on the phone
- tapping the notification routed into the calendar flow

Observed logs:

- `FCM_SERVICE: Reminder received eventId=test-event-id, destinationTab=calendar, title=3 Days left to Hack Night`
- `MAIN_INTENT: destinationTab=calendar`

## Original Deployment Blocker

The original plan was to deploy `sendEventReminders` as a scheduled Firebase Cloud Function using:

`firebase deploy --only functions:sendEventReminders`

This was blocked because the Firebase project `campuseventdiscovery-87f4f` is on the Spark plan, while Cloud Functions deployment requires Blaze billing in order to enable:

- `cloudfunctions.googleapis.com`
- `cloudbuild.googleapis.com`
- `artifactregistry.googleapis.com`

Without a workaround, the code existed and manual FCM testing worked, but the automatic daily scheduled reminder job could not go live.

## Deployment Workaround Used

To avoid the Blaze-plan blocker, the reminder scheduler was moved to GitHub Actions.

### Why GitHub Actions

- free for the repository workflow being used
- no Firebase billing upgrade required
- can run on a daily cron schedule
- can run the same Firebase Admin SDK reminder logic already written in `functions/index.js`

### Workflow file

`.github/workflows/send-reminders.yml`

Current schedule:

- `0 3 * * *` UTC
- equivalent to `8:00 AM PKT`

The workflow also supports `workflow_dispatch` for manual test runs.

### Authentication

GitHub Actions uses the repository secret:

- `FIREBASE_SERVICE_ACCOUNT`

This secret contains the Firebase service account JSON required for Firestore reads and FCM sends.

## Final Deployment State

The reminder system is now designed to run as follows:

1. GitHub Actions cron triggers every day at `03:00 UTC`
2. the runner checks out the repository
3. Node is set up
4. dependencies are installed in `functions/`
5. `node functions/index.js` runs
6. the script authenticates using `FIREBASE_SERVICE_ACCOUNT`
7. Firestore is queried for active events within the next 3 days
8. attendee subcollections are read
9. user FCM tokens are read
10. `EVENT_REMINDER` data payloads are sent through FCM
11. the Android app receives the payload and opens the calendar flow when tapped

## Files Relevant To This Feature

- `app/src/main/java/com/example/CampusEventDiscovery/CampusEventDiscoveryApp.java`
- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/Constants.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingService.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ConstantsTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingServiceTest.java`
- `.github/workflows/send-reminders.yml`
- `docs/conflicts_yahya.md`
- `firebase.json`
- `firestore.indexes.json`
- `functions/index.js`
- `functions/package.json`

## Current Shortcoming

The original Firebase scheduled-function deployment is still not possible on the current Spark-plan project. The deployed automatic scheduler now depends on GitHub Actions and the repository secret `FIREBASE_SERVICE_ACCOUNT` being present and valid on the default branch. This works around the billing blocker, but it means reminder automation is no longer hosted inside Firebase Cloud Functions itself.
