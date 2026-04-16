# GitHub 4-Stage Breakdown

Use this if you want to present the current work as four clear stages on GitHub.

Current recommendation:
- show these as `4 commits`, or
- show these as `4 milestone sections` in your README / progress update

## Stage 1: Auth, Startup, and Developer Access

Suggested commit title:
`feat: improve auth startup flow, maintenance handling, and developer bypass`

What to say:
- Added Firebase App Check initialization for debug and release builds.
- Improved startup routing through splash and maintenance mode checks.
- Improved sign-in and sign-up error handling for Firebase/Auth setup issues.
- Added developer bypass flow with role selection for attendee, organizer, and admin.
- Added dev bypass entry points on welcome, sign-in, and sign-up screens.

Main files:
- `app/src/main/java/com/example/campuseventdiscovery/CampusEventDiscoveryApp.java`
- `app/src/main/java/com/example/campuseventdiscovery/SplashActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/MainActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/WelcomeActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/SignInActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/SignUpActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/MaintenanceActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/util/DevSessionManager.java`
- `app/src/main/java/com/example/campuseventdiscovery/util/DevBypassHelper.java`
- `app/src/main/res/layout/activity_welcome.xml`
- `app/src/main/res/layout/activity_sign_in.xml`
- `app/src/main/res/layout/activity_sign_up.xml`
- `app/src/main/res/layout/activity_maintenance.xml`
- `app/src/main/res/layout/dialog_dev_bypass_role_picker.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

Good demo points:
- Launch app
- Show maintenance / splash routing
- Show developer bypass role picker
- Show login / signup error messaging

## Stage 2: Organizer Event Management

Suggested commit title:
`feat: add organizer event creation, proposal tracking, and attendee management`

What to say:
- Expanded event proposal form with category, capacity, tags, sponsors, food stalls, and trailer URL.
- Added a full Manage Events screen for approved, pending, and rejected organizer items.
- Added organizer proposal detail screen.
- Added organizer event detail screen.
- Added attendee management with attendee search, check-in, blacklisting, and announcements.

Main files:
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/CreateEventActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/ManageEventsActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/OrganizerEventDetailActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/organizer/WhoIsComingActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/OrganizerProposalDetailActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/AttendeeAdapter.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/OrganizerPendingAdapter.java`
- `app/src/main/res/layout/activity_create_event.xml`
- `app/src/main/res/layout/activity_manage_events.xml`
- `app/src/main/res/layout/activity_who_is_coming.xml`
- `app/src/main/res/layout/activity_organizer_proposal_detail.xml`
- `app/src/main/res/drawable/bg_badge_dark.xml`
- `app/src/main/res/values/strings.xml`

Good demo points:
- Create a proposal
- Show pending / rejected / approved sections
- Open proposal details
- Open organizer event details
- Show attendee search, check-in, blacklist, and announcement flow

## Stage 3: Attendee Experience and Profile Features

Suggested commit title:
`feat: add attendee notifications, memories, feedback, and profile improvements`

What to say:
- Added Notification Center screen.
- Added Memories screen.
- Added event feedback flow with optional photo uploads and saved memories.
- Improved profile screen with role-based rows and account settings support.
- Improved attendee home and event detail flows.
- Added better SOS handling with location permission and best-known coordinates.
- Improved calendar flow with usable event end times.

Main files:
- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/NotificationCenterActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/MemoriesActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventFeedbackActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/ProfileFragment.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/profile/AccountSettingsActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeFragment.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/home/HomeOrganizerFragment.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventDetailActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/calendar/EventCalendarFragment.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/EventAdapter.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/MemoryAdapter.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/NotificationAdapter.java`
- `app/src/main/res/layout/activity_event_feedback.xml`
- `app/src/main/res/layout/activity_memories.xml`
- `app/src/main/res/layout/activity_notification_center.xml`
- `app/src/main/res/layout/fragment_profile.xml`
- `app/src/main/res/layout/item_memory.xml`
- `app/src/main/res/layout/item_notification.xml`
- `app/src/main/res/layout/activity_account_settings.xml`
- `app/src/main/res/values/strings.xml`

Good demo points:
- Open notifications
- Open memories
- Leave feedback with rating and photos
- Show profile rows and account settings
- Show event detail and calendar add flow
- Show SOS dialog and location-enabled behavior

## Stage 4: Admin Review, Repository Logic, and Final Polish

Suggested commit title:
`feat: complete admin approval flow and repository support for advanced event actions`

What to say:
- Added detailed admin approval and rejection flow.
- Rejection now requires a real note.
- Improved proposal/event status handling and organizer/admin card behavior.
- Extended repository logic to support announcements, blacklisting, attendee check-in, memories, ratings, notifications, proposal review, and default event end time behavior.
- Added test and manifest cleanup needed for the updated app flow.

Main files:
- `app/src/main/java/com/example/campuseventdiscovery/ui/event/EventApprovalActivity.java`
- `app/src/main/java/com/example/campuseventdiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/campuseventdiscovery/ui/myevents/MyEventsFragment.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/OrganizerPendingAdapter.java`
- `app/src/main/java/com/example/campuseventdiscovery/adapter/EventAdapter.java`
- `app/src/test/java/com/example/campuseventdiscovery/ExampleUnitTest.java`
- `app/src/androidTest/java/com/example/campusevent/ExampleInstrumentedTest.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`

Good demo points:
- Show admin proposal approval
- Show rejection note flow
- Explain repository support added for notifications, memories, attendee actions, and proposal transitions

## Best Way To Show This On GitHub

If you want the cleanest GitHub history, make 4 commits in this order:

1. Stage 1: auth, startup, maintenance, developer bypass
2. Stage 2: organizer creation and management flows
3. Stage 3: attendee/profile/notification/memories/feedback flows
4. Stage 4: admin approval flow and repository polish

If you want, I can do the next step and split the current uncommitted work into exactly these 4 commits for you.
