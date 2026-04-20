# Front-End Change Notes

Compared local project snapshot against GitHub branch `integration/level2-final` at commit `a982be00d8d5b3ada37c55ee3ebe67491dc6b31f`.

## Local Differences To Describe Later

- Local has all files tracked by the remote branch; no tracked remote files are missing locally.
- Local has 49 real content changes after ignoring line-ending differences.
- Local has many raw file differences caused only by LF/CRLF line endings. These should not be described as feature changes.
- Local includes generated or machine-local artifacts that should not be treated as meaningful product changes: `.gradle/`, `.idea/`, `app/build/`, root `build/`, and `local.properties`.

## Local-Only Files

- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/AuthScreensInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/NavigationSurfacesInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/UtilityScreensInstrumentedTest.java`
- `app/src/main/res/drawable/bg_sos_status_chip.xml`
- `app/src/test/java/com/example/CampusEventDiscovery/ui/LayoutStyleContractTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ui/ThemeStyleContractTest.java`

## Local Feature/UI Changes

- Added Android instrumented test dependencies in `app/build.gradle.kts`: `androidx.test:core` and `androidx.test:rules`.
- Registered `PaymentConfirmationActivity` in `AndroidManifest.xml`.
- Added organizer create-event access through an extended floating action button in `MainActivity`.
- Updated sign-in and sign-up screens to use `MaterialToolbar` navigation instead of separate image back buttons.
- Added sign-up terms and privacy interaction: checkbox validation, terms/privacy links, and policy dialogs.
- Added search/filter behavior to `MyEventsFragment`.
- Added search/filter behavior to `ManageEventsActivity`.
- Added organizer quick actions on organizer home: create event, manage events, scan tickets, and SOS dashboard.
- Added organizer navigation rows on profile: create event, scan tickets, SOS dashboard, and manage events.
- Improved account settings flow by separating change email and change password controls, revealing password fields only when needed.
- Added or adjusted layout resources for the auth flow, profile, organizer home, manage events, my events, payment, SOS, ticketing, and event detail screens.
- Added string and theme resources to support the updated UI.
- Added front-end contract tests around layout and theme styling.

## Current UI/HCI Assessment

- The app uses a simple bottom navigation model with five main destinations: Home, Search, Action, Favourites, and Profile.
- The middle action adapts by role: attendees see My Events, admins see Approvals, and organizers get a separate Create Event floating action button.
- Role-specific shortcuts now exist in organizer home and profile, which improves discoverability for organizer workflows.
- Search exists globally and within My Events/Manage Events, which helps users recover from long lists.
- Most secondary screens use toolbars, but a few event-related screens still use custom image back buttons. This creates an inconsistent navigation pattern.
- Profile is currently a long mixed list of personal settings, attendee tools, organizer tools, calendar, notifications, help, and logout. It works, but the information architecture is crowded.
- Organizer home duplicates some actions already available from the floating action button and profile. This is useful for discoverability, but the labels and visual hierarchy should make the primary next action obvious.

## Recommended Front-End-Only Improvements

- Standardize top navigation by replacing remaining custom `ImageButton` back controls with `MaterialToolbar` navigation on event detail, event approval, organizer event detail, who-is-coming, ticket, payment confirmation, and SOS screens.
- Group the profile page into clear sections: Account, Attendee Tools, Organizer Tools, App Preferences, Support. This keeps the same actions but reduces cognitive load.
- Keep Create Event as the organizer primary action, but avoid making users choose between too many equal-weight buttons. Use one prominent Create Event button and secondary outlined buttons for Manage Events, Scan Tickets, and SOS Dashboard.
- Add short subtitles to important profile and organizer rows, for example "View RSVPs and saved events" under My Events or "Check in attendees by QR" under Scan Tickets. This improves recognition without backend changes.
- Add consistent empty states with an icon, one helpful sentence, and a direct action where possible. Examples: no saved events should point to Search; no managed events should point to Create Event.
- Add a visible "Clear search" affordance to Search, My Events, and Manage Events. Users should not need to manually delete text to recover full results.
- Add loading and disabled states consistently for all submit actions, especially payment, checkout, create event, account settings, sign-in, and sign-up. Some screens already disable buttons; make the pattern universal.
- Improve touch targets and accessibility labels for icon-only controls such as favourite, share, calendar month arrows, and SOS close. Aim for at least 48dp touch targets.
- Use role-aware labels consistently. If an organizer is viewing events they created, prefer "Manage Events" over "My Events" in that context to avoid ambiguity.
- Keep SOS visible but visually distinct from normal navigation. It should be easy to find, but not look like an ordinary secondary action.
- Add confirmation dialogs for destructive or high-impact actions such as blacklist attendee, logout, reject proposal, and SOS-related dismissal if not already present.
- Use snackbars for reversible or low-risk feedback and dialogs only for decisions or errors requiring attention. This keeps interaction lighter and more human-friendly.

## Backend Constraint

All recommendations above are front-end-only. They can be implemented through XML layout changes, resource strings/styles, adapters, activity/fragment click handling, and client-side filtering/navigation. No Firestore schema, repository behavior, cloud functions, Supabase functions, or payment backend changes are required.

## Implemented Front-End Improvements

- Standardized remaining custom back navigation on key screens by introducing `MaterialToolbar` navigation on event detail, event approval, organizer event detail, who-is-coming, ticket, payment confirmation, SOS, and SOS dashboard screens.
- Preserved the emergency full-screen SOS alert design, but added a confirmation dialog before dismissing the alert.
- Grouped the profile page into clearer sections and hid attendee/organizer tool sections when they do not apply to the current role.
- Added subtitles to important profile rows so users can understand actions before tapping: My Events, Memories, Manage Events, Create Event, Scan Tickets, SOS Dashboard, Calendar, Notifications, Account Settings, and Dark Mode.
- Rebalanced organizer home actions so Create Event is the single primary action, while Manage Events, Scan Tickets, and SOS Dashboard are secondary actions.
- Added visible clear-text affordances to Search, My Events, Manage Events, and Who Is Coming search fields.
- Improved empty states with icons, centered copy, and more helpful text for search, My Events, Manage Events, payments, participants, and SOS dashboard.
- Improved icon-only touch targets and accessibility labels on event detail, event approval, organizer event detail, and calendar month navigation controls.
- Added loading/disabled action handling to event approval and rejection actions to avoid duplicate submissions.
- Kept all changes within front-end layouts, strings/styles, and activity/fragment UI handling.

## Verification

- Ran `.\gradlew.bat test --rerun-tasks`.
- Result: build successful, including resource compilation and unit/style contract tests.

## Implemented Admin And Safety Improvements

- Admins now receive organizer-style home capabilities: create event, manage events, scan tickets, and open the SOS dashboard.
- Admins can manage all active events, while organizers continue to manage only their own active events.
- Admin home now includes a clear switch between pending approvals and rejected proposals.
- Admins and organizers can delete events from the organizer event detail screen. The implementation soft-deletes by setting `status = deleted`, with `deletedAt` and `deletedBy`, so active-event queries stop showing the event without orphaning subcollection data.
- Admins inherit organizer tools in Profile, including Manage Events, Create Event, Scan Tickets, and SOS Dashboard.
- Admins can blacklist attendees from any event they can open through all-event management. Organizers can blacklist attendees from their own event management flow.
- Organizer event detail now has a Blacklisted People button that opens the event blacklist list.
- Scanner now checks `events/{eventId}/blacklist/{userId}` before displaying or accepting a scanned ticket. If the attendee is blacklisted, it shows that the named attendee has been blacklisted and should not be allowed to enter.
- Manual QR-token check-in also checks blacklist records by stored `qrToken` when the attendee has already been removed from the active attendee list.
- Blacklisting stores the attendee `qrToken` in the blacklist record, cancels the user RSVP, removes the attendee from active attendees, and adjusts event RSVP/checked-in counts in a transaction.
- Event approval now guards against duplicate event creation by reading the proposal inside a transaction and only creating the published event if the proposal is still pending.
- The profile image picker now uses `ACTION_OPEN_DOCUMENT` with persistable read permissions, which is more reliable on physical Android devices for selecting and uploading profile images.

## Data Duplication Notes

- RSVP creation already uses a Firestore transaction to prevent duplicate attendee/RSVP records and to enforce capacity from the server-side event document.
- Event approval now uses a transaction to prevent double-tapping or repeated approval from creating duplicate published events for the same proposal.
- Check-in uses a transaction to prevent duplicate scans from incrementing attendance more than once.
- Blacklisting uses a transaction to keep blacklist, attendee removal, RSVP cancellation, and event counts consistent.
- SOS alert creation remains a single batch write that fans out notifications to the organizer and all admins passed by `SosActivity`; no extra duplicate SOS alert documents are created.

## Implemented Navigation, Sharing, Search, And Accent Improvements

- Ticket scanning now uses a custom ZXing capture activity with an explicit 48dp back button inside the live camera screen, so organizers can leave scanning without relying on system navigation.
- Event detail and organizer event detail share buttons now open a share-options dialog instead of immediately launching the Android chooser.
- Event sharing now generates an app deep link in the format `campusevent://events/{eventId}` and registers `EventDetailActivity` to open those links directly to the event page.
- The share dialog gives users clear options to copy the event link, share only the link, or share event details with title, date, venue, and link.
- Search now supports client-side sorting by relevance, date, popularity based on registration count, ticket price high-to-low, and ticket price low-to-high.
- The Search sort control now has a visible 48dp touch target and keeps the selected sort label visible.
- Profile settings now include an Accent Color option alongside Dark Mode, with Purple, Blue, Green, and Orange choices stored locally.
- The selected accent updates the visible accent preview, Search sort control, main bottom navigation selected state, and Create Event floating action button without backend changes.
- Accent switching now applies through a centralized activity lifecycle pass that retints common Material buttons, outlined buttons, chips, text inputs, progress indicators, compound buttons, purple text/icon tints, bottom navigation, and floating action buttons when they are using the original purple accent.
- Calendar day highlights and legend markers now use the selected accent instead of reading the static purple resource directly.
- RecyclerView-bound event accents now use the selected accent when rows are bound, including saved-heart icons and active/approved status chips.
- Accent switching now uses selected theme variants before activity layout inflation, so XML-defined buttons, links, chips, vector hearts, chip backgrounds, scanner laser color, calendar arrows, and color state lists resolve to the selected accent instead of the static purple resource.
- Each accent has light and dark shade resources for primary, on-primary, primary-container, on-primary-container, and ripple/feedback use so hierarchy and contrast are preserved across dark mode and light mode.
- The bottom navigation background was intentionally not changed yet. Recommended alternatives were documented in conversation: keep a neutral surface with accent active state, or use a very light/dark accent container background if the team explicitly wants the whole bar colored.
