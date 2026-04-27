# Project Change Log

This file tracks the work done on this project from this point forward. Each entry records why the change was needed, what was changed, which files were updated, and how the change was verified.

## How This Log Will Be Maintained

- Add a new entry after every implemented feature, UI update, bug fix, or meaningful project change.
- Keep entries in reverse chronological order, with the newest work at the top.
- Include the reason for the change, the implementation details, changed files, verification steps, and any follow-up notes.
- Mention commits when a change has already been committed or pushed.

## 2026-04-27 - Organizer Bottom Navigation Create Event Action

### Why This Was Needed

Organizers needed faster access to the event creation form from the bottom navigation, without removing attendee-specific My Events or admin-specific vendor management behavior.

### What Was Done

- Reused the existing `nav_action` bottom navigation slot as an organizer-only create event action.
- Shows the action as a plus icon with the `Create Event` label for organizer accounts.
- Opens the same `CreateEventActivity` used by the Organizer Dashboard `Create a New Event` button.
- Keeps the action hidden for admins.
- Keeps the attendee action unchanged as `My Events`.
- Added and updated tests to cover the organizer create action in bottom navigation.

### Files Changed

- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
  - Shows `nav_action` for organizers with `ic_add` and `Create Event`.
  - Routes organizer `nav_action` clicks to `CreateEventActivity`.
- `app/src/test/java/com/example/CampusEventDiscovery/ui/LayoutStyleContractTest.java`
  - Updates the main navigation contract to assert the bottom-nav action slot exists.
- `app/src/test/java/com/example/CampusEventDiscovery/ui/VendorManagementContractTest.java`
  - Adds a contract assertion for organizer bottom-nav create action wiring.
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/NavigationSurfacesInstrumentedTest.java`
  - Asserts the organizer bottom nav shows `Create Event`.
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/VendorManagementInstrumentedTest.java`
  - Adds an instrumentation test that selects the plus action and verifies the create event form opens.

### Verification

- Ran `sh gradlew :app:assembleDebug`.
- Ran focused unit contracts:
  - `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.ui.VendorManagementContractTest --tests com.example.CampusEventDiscovery.ui.LayoutStyleContractTest`
- Compiled app and Android UI tests:
  - `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`
- Ran focused vendor/create-event UI instrumentation tests on `Medium_Phone_API_36.1(AVD) - 16`:
  - `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`
  - 3 tests completed successfully.

### Commit

- Not committed yet.

## 2026-04-27 - Memories Cloudinary Upload Alignment

### Why This Was Needed

Event creation already uploads images through the shared `CloudinaryHelper` flow. Memories were still uploading selected photos through Firebase Storage, which created inconsistent image upload behavior and storage paths across the app.

### What Was Done

- Updated Memories photo uploads to use `CloudinaryHelper.uploadImage`.
- Updated Event Feedback photo uploads, which also save into Memories, to use `CloudinaryHelper.uploadImage`.
- Kept the existing Firestore memory album storage unchanged: uploaded Cloudinary URLs are still saved into `photoUrls`.
- Preserved the existing sequential multi-photo upload behavior, loading states, and success/failure UI messages.

### Files Changed

- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoriesActivity.java`
  - Replaced Firebase Storage upload logic with Cloudinary helper uploads.
  - Removed Firebase Storage and UUID imports that were only needed for Storage paths.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventFeedbackActivity.java`
  - Replaced Firebase Storage upload logic with Cloudinary helper uploads before saving feedback memory photos.
  - Removed Firebase Storage and UUID imports.

### Verification

- Ran `sh gradlew :app:assembleDebug`.
- Build completed successfully.

### Commit

- Included in the vendor management batch commit.

## 2026-04-27 - Vendor And Cloudinary Test Coverage

### Why This Was Needed

Before committing the vendor management and memory upload work, the new behavior needed focused coverage for the data model, repository contracts, role-based vendor UI, and Cloudinary upload usage.

### What Was Done

- Added unit tests for `VendorProposal` default state and field round-tripping.
- Added source/layout contract tests for:
  - Vendor management screen structure.
  - Admin dashboard Vendor Requests entry point.
  - Vendor proposal item review actions.
  - Admin/organizer bottom navigation replacing Favourites with Vendors.
  - Organizer event-selection-first vendor flow.
  - Admin unread vendor proposal count wiring.
  - Vendor repository methods and expected Firestore fields.
  - Memories and feedback image uploads using `CloudinaryHelper`.
- Added Android instrumentation UI tests for:
  - Organizer Vendors tab opening the approved-event selection screen.
  - Admin Vendors tab opening the vendor requests screen.
- Updated stale instrumentation expectations that referenced removed UI IDs.

### Files Changed

- `app/src/test/java/com/example/CampusEventDiscovery/VendorProposalTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ui/VendorManagementContractTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ui/VendorRepositoryContractTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/ui/CloudinaryUploadContractTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/VendorManagementInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/NavigationSurfacesInstrumentedTest.java`
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/UtilityScreensInstrumentedTest.java`

### Verification

- Ran focused unit tests:
  - `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.VendorProposalTest --tests com.example.CampusEventDiscovery.ui.VendorManagementContractTest --tests com.example.CampusEventDiscovery.ui.CloudinaryUploadContractTest --tests com.example.CampusEventDiscovery.ui.VendorRepositoryContractTest`
  - Build completed successfully.
- Compiled app and Android UI tests:
  - `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`
  - Build completed successfully.
- Ran focused vendor UI instrumentation tests on `Medium_Phone_API_36.1(AVD) - 16`:
  - `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`
  - 2 tests completed successfully.

### Notes

- The full legacy unit suite still has pre-existing Robolectric/Mockito bytecode failures unrelated to this feature work under the current Java/runtime setup, so the new coverage was run as focused tests.

## 2026-04-27 - Vendor Management System

### Why This Was Needed

Organizers needed a structured way to request vendors for approved events, and admins needed a clear review workflow for approving or rejecting those vendor proposals. The previous admin and organizer bottom navigation still exposed Favourites, which is attendee-focused and not part of the admin/organizer workflow.

### What Was Done

- Added a `VendorProposal` model backed by a new Firestore collection named `vendorProposals`.
- Added repository methods to submit vendor proposals, fetch proposals by event, fetch all admin proposals, approve/reject vendor proposals, mark proposals as read by admin, and observe unread pending proposal counts.
- Replaced the admin/organizer bottom navigation Favourites tab with Vendors while leaving attendee Favourites unchanged.
- Added an organizer vendor management flow:
  - First shows only the organizer's approved event cards using the compact event-row style used elsewhere in the app.
  - Opens the selected event's vendor detail view when an organizer taps an event card.
  - Shows approved, pending, and rejected vendors for the selected event in separate status tabs.
  - Lets the organizer submit a new vendor request for admin approval.
- Added an admin vendor request flow:
  - Shows vendor proposals grouped by approved, pending, and rejected status.
  - Allows admins to approve or reject pending vendor proposals.
  - Marks pending unread vendor proposals as read when the admin opens the vendor screen.
- Added an Admin Dashboard Vendor Requests button that displays the unread pending vendor proposal count.
- Removed organizer-facing favourite controls from organizer event cards and the organizer featured event card. Attendee favourites remain unchanged.
- Used existing Material styling, app surface colors, neutral outlines, and the current accent color for selected states.

### Files Changed

- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`
  - Converts the Favourites bottom-nav item into Vendors for admin and organizer roles.
  - Routes admin/organizer vendor tab clicks to `VendorManagementFragment`.
  - Adds `openVendorManagement()` for dashboard navigation.
- `app/src/main/java/com/example/CampusEventDiscovery/model/VendorProposal.java`
  - Adds the Firestore model for vendor requests.
- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
  - Adds vendor proposal creation, fetching, unread count observation, read marking, and approve/reject review methods.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/vendors/VendorManagementFragment.java`
  - Adds role-aware organizer/admin vendor management UI logic.
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/VendorEventAdapter.java`
  - Adds selectable approved event cards for organizer vendor management using the existing compact event row layout.
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/VendorProposalAdapter.java`
  - Adds vendor proposal cards with status chips and admin review actions.
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/EventAdapter.java`
  - Adds a flag to hide favourite actions for non-attendee usage.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeOrganizerFragment.java`
  - Disables organizer-facing favourite behavior and hides the featured-card favourite control.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeAdminFragment.java`
  - Adds vendor request count observation and dashboard navigation.
- `app/src/main/res/layout/fragment_vendor_management.xml`
  - Adds the main vendor management screen.
- `app/src/main/res/layout/item_vendor_proposal.xml`
  - Adds vendor proposal display and admin action rows.
- `app/src/main/res/layout/dialog_add_vendor.xml`
  - Adds the organizer vendor request form.
- `app/src/main/res/layout/fragment_home_admin.xml`
  - Adds the Vendor Requests dashboard button.
- `app/src/main/res/values/strings.xml`
  - Adds vendor management labels, messages, and validation strings.

### Verification

- Ran `sh gradlew :app:assembleDebug`.
- Build completed successfully.

### Commit

- Included in the vendor management batch commit.

### Notes

- Firestore queries were kept simple and sorted client-side where needed to avoid requiring new composite indexes.
- Attendee Favourites remains available and unchanged.

## 2026-04-27 - Admin Approval Tabs Background Fix

### Why This Was Needed

The Admin Dashboard approval tabs had a visual issue where the entire segmented tab bar appeared purple. The intended behavior was for only the selected tab segment to use the current accent color, while the rest of the control stayed neutral. This made the selected state clearer and prevented the inactive tab area from looking active.

### What Was Done

- Updated the approval tabs container on the Admin Dashboard to use the app surface color instead of the secondary container color.
- Added a neutral outline around the segmented control container so the tab bar still has a clear boundary.
- Kept the existing selected-state behavior intact through the `admin_toggle_background` selector, which highlights only the checked tab with `?attr/colorPrimary`.
- Left the inactive tab background transparent, so inactive segments no longer appear highlighted.

### Files Changed

- `app/src/main/res/layout/fragment_home_admin.xml`
  - Changed the approval toggle wrapper background from `@color/colorSecondaryContainer` to `@color/colorSurface`.
  - Added `app:strokeColor="@color/colorOutlineVariant"`.
  - Added `app:strokeWidth="1dp"`.
  - Removed an unnecessary implementation comment above the segmented toggle.

### Verification

- Ran `sh gradlew :app:assembleDebug`.
- Build completed successfully.

### Commit

- `81fb492 Fix admin approval tab background`
- Pushed to `origin/ui-update`.

### Notes

- The local `.DS_Store` modification was intentionally excluded from the commit because it was unrelated to the UI change.

## 2026-04-27 - Memory Album Viewer

### Why This Was Needed

The memory cards already supported adding photos, but there was no dedicated way to inspect the full album for an event. A separate album view makes the memory flow consistent with the rest of the app and gives users a gallery-style screen for browsing all photos attached to an event memory.

### What Was Done

- Added a `View Memories` action to each memory card alongside the existing `Add Photos` action.
- Wired the new action to open a dedicated memory album screen for the selected event.
- Added a new album activity that displays all photos for the event in a three-column grid view.
- Kept the empty state for albums with no photos.
- Passed the selected event title and filtered photo URLs into the album screen through intent extras.
- Styled the album screen and grid items to match the app's current Material design system and accent behavior.
- Added focused unit and Android UI coverage for the new album screen, card actions, and activity registration.

### Files Changed

- `app/src/main/res/layout/item_memory.xml`
  - Added the `View Memories` button next to `Add Photos`.
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/MemoryAdapter.java`
  - Added a second action callback for opening the album view.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoriesActivity.java`
  - Added navigation to the album activity and passed album data through intent extras.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/MemoryAlbumActivity.java`
  - New album screen with toolbar, empty state, and grid layout.
- `app/src/main/java/com/example/CampusEventDiscovery/adapter/MemoryPhotoGridAdapter.java`
  - New RecyclerView adapter for the album grid.
- `app/src/main/res/layout/activity_memory_album.xml`
  - New album screen layout.
- `app/src/main/res/layout/item_memory_album_photo.xml`
  - New grid cell layout for album photos.
- `app/src/main/res/values/strings.xml`
  - Added album labels and empty-state strings.
- `app/src/main/AndroidManifest.xml`
  - Registered `MemoryAlbumActivity`.
- `app/src/test/java/com/example/CampusEventDiscovery/ui/MemoryAlbumContractTest.java`
  - Added layout and manifest contract coverage.
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/UtilityScreensInstrumentedTest.java`
  - Added an instrumentation check for the album screen rendering.

### Verification

- Ran `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.ui.MemoryAlbumContractTest --tests com.example.CampusEventDiscovery.ui.LayoutStyleContractTest`.
- Ran `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.UtilityScreensInstrumentedTest`.
- All of the above completed successfully.

### Notes

- The album screen filters out empty photo URLs before rendering.
- The local `.DS_Store` modification remains unrelated and was not touched.

## 2026-04-27 - Vendor Toggle and Back Button UI Consistency

### Why This Was Needed

The vendor and admin segmented controls were rendering as solid purple bars, which made the inactive state visually unclear. The organizer vendor detail screen also used a back affordance that did not match the rest of the app, so the vendor flow felt inconsistent with the other screens.

### What Was Done

- Updated the shared vendor/admin toggle background selector so unselected segments use the neutral surface color and only the active segment uses the current accent color.
- Kept the existing toggle behavior intact for both the Admin Dashboard approval tabs and the Vendor Management status tabs.
- Replaced the organizer vendor detail screen's old back button with a standard toolbar navigation icon pattern consistent with the rest of the app.
- Made the vendor detail toolbar switch between vendor-management mode and event-detail mode so the back action only appears when the organizer is inside a specific event's vendor view.

### Files Changed

- `app/src/main/res/color/admin_toggle_background.xml`
  - Changed the default toggle background from transparent to the surface color.
- `app/src/main/res/layout/fragment_vendor_management.xml`
  - Replaced the standalone back button with a `MaterialToolbar`.
  - Kept the subtitle and vendor content layout aligned with the rest of the app.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/vendors/VendorManagementFragment.java`
  - Wired the toolbar title and navigation icon to the current vendor view state.
  - Removed the old back button flow.
- `app/src/test/java/com/example/CampusEventDiscovery/ui/VendorManagementContractTest.java`
  - Updated the layout contract to assert the toolbar-based header instead of the removed button.

### Verification

- Ran `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.ui.VendorManagementContractTest --tests com.example.CampusEventDiscovery.ui.LayoutStyleContractTest`.
- Ran `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`.
- All of the above completed successfully.

### Notes

- The active toggle still uses the current theme accent, including role-based accent themes.
- The local `.DS_Store` modification remains unrelated and was not touched.

## 2026-04-27 - Purple Toggle Accent Fix

### Why This Was Needed

The purple accent theme was still flattening the vendor and approval segmented controls into a full purple bar. Other accent themes were rendering correctly, which pointed to a runtime theme application issue rather than a layout problem.

### What Was Done

- Updated the shared accent applicator so it does not override `MaterialButton` tinting for buttons inside a `MaterialButtonToggleGroup`.
- Kept the toggle-group styling driven by XML state selectors, so only the checked segment uses the accent color.
- Preserved the existing accent behavior for ordinary buttons elsewhere in the app.

### Files Changed

- `app/src/main/java/com/example/CampusEventDiscovery/util/ThemeManager.java`
  - Added a toggle-group ancestry check and skipped accent tint overrides for those buttons.

### Verification

- Ran `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.ui.VendorManagementContractTest --tests com.example.CampusEventDiscovery.ui.LayoutStyleContractTest`.
- Ran `sh gradlew clean :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`.
- All completed successfully after rerunning the connected test outside the clean build.

### Notes

- The earlier vendor toolbar back-button consistency fix remains in place.
- The local `.DS_Store` modification remains unrelated and was not touched.

## 2026-04-27 - Toggle Container Tint Guard

### Why This Was Needed

Even after skipping accent overrides for the toggle buttons themselves, the purple theme could still tint the `MaterialButtonToggleGroup` container and make the full bar read as a solid accent block.

### What Was Done

- Added a guard in the shared accent applicator so `MaterialButtonToggleGroup` containers are not retinted at runtime.
- Left the checked-state styling to the XML selector, which keeps only the active tab highlighted.

### Files Changed

- `app/src/main/java/com/example/CampusEventDiscovery/util/ThemeManager.java`
  - Prevented `MaterialButtonToggleGroup` containers from being treated like ordinary accent-tinted buttons.

### Verification

- Ran `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`.
- Both completed successfully.

### Notes

- This was a follow-up to the earlier purple toggle fix because the container itself still needed to be excluded from runtime tinting.
- The local `.DS_Store` modification remains unrelated and was not touched.

## 2026-04-27 - Rebuilt Vendor and Admin Segmented Buttons

### Why This Was Needed

The segmented approval/vendor controls still behaved like a theme-tinted toggle group in the purple accent path. Rather than keep patching the old widget, the control was rebuilt so the selected state is handled directly by the app and cannot collapse into a full purple bar.

### What Was Done

- Removed the `MaterialButtonToggleGroup` implementation from both the Admin Dashboard approval bar and the Vendor Management status bar.
- Replaced both with manual segmented button rows built from individual `MaterialButton` views.
- Added explicit selected and unselected styling in code so only the active segment is filled with the current accent color.
- Kept the button rows consistent with the app's Material surface, outline, and accent palette.
- Added device coverage for the admin home approval buttons and kept the vendor-screen coverage in place.

### Files Changed

- `app/src/main/res/layout/fragment_home_admin.xml`
  - Rebuilt the approval control as two standalone buttons inside a neutral card.
- `app/src/main/res/layout/fragment_vendor_management.xml`
  - Rebuilt the vendor status control as three standalone buttons inside a neutral card.
- `app/src/main/res/values/themes.xml`
  - Added `AppSegmentToggleButtonStyle` for the new segmented buttons.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeAdminFragment.java`
  - Switched from toggle-group listeners to manual button selection and styling.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/vendors/VendorManagementFragment.java`
  - Switched from toggle-group listeners to manual button selection and styling.
- `app/src/androidTest/java/com/example/CampusEventDiscovery/ui/VendorManagementInstrumentedTest.java`
  - Added admin home assertions for the new approval segment buttons.
- `app/src/test/java/com/example/CampusEventDiscovery/ui/VendorManagementContractTest.java`
  - Updated layout assertions to match the rebuilt button rows.

### Verification

- Ran `sh gradlew :app:testDebugUnitTest --tests com.example.CampusEventDiscovery.ui.VendorManagementContractTest --tests com.example.CampusEventDiscovery.ui.LayoutStyleContractTest`.
- Ran `sh gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`.
- All completed successfully.

### Notes

- The earlier theme-guard changes in `ThemeManager` remain, but the new button rows no longer depend on toggle-group behavior.
- The local `.DS_Store` modification remains unrelated and was not touched.

## 2026-04-27 - Segmented Button Label Visibility Fix

### Why This Was Needed

After rebuilding the segmented controls as manual button rows, the first selected button could still render with unreadable text on the purple accent path until the other option was touched. That made the control look broken even though the selection logic was working.

### What Was Done

- Moved the segmented controls to selected-state color resources so the active segment and label colors are resolved declaratively on first draw.
- Kept the selection logic in the fragments, but switched the visual state to `setSelected(...)` instead of imperative tint updates.
- Excluded the segmented buttons from the shared runtime accent walker so the theme pass cannot flatten their selected text color.
- Confirmed the admin and vendor segmented controls now show their labels immediately on initial render across the purple accent theme.

### Files Changed

- `app/src/main/res/layout/fragment_home_admin.xml`
  - Left the approval buttons as plain buttons using selected-state styling.
- `app/src/main/res/layout/fragment_vendor_management.xml`
  - Left the vendor status buttons as plain buttons using selected-state styling.
- `app/src/main/res/color/admin_toggle_background.xml`
  - Switched the selected-state selector from checked state to selected state.
- `app/src/main/res/color/admin_toggle_text.xml`
  - Switched the selected-state selector from checked state to selected state and kept the unselected text neutral.
- `app/src/main/res/color/admin_toggle_stroke.xml`
  - Added a dedicated stroke selector for selected and unselected states.
- `app/src/main/res/values/themes.xml`
  - Pointed the segmented control style at the state-based color resources.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeAdminFragment.java`
  - Switched approval buttons to `setSelected(...)` and kept typeface changes only.
- `app/src/main/java/com/example/CampusEventDiscovery/ui/vendors/VendorManagementFragment.java`
  - Switched vendor buttons to `setSelected(...)` and kept typeface changes only.
- `app/src/main/java/com/example/CampusEventDiscovery/util/ThemeManager.java`
  - Added an explicit exclusion for the segmented control buttons so the accent walker does not recolor them.

### Verification

- Ran `sh gradlew clean :app:assembleDebug :app:assembleDebugAndroidTest`.
- Ran `sh gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.CampusEventDiscovery.ui.VendorManagementInstrumentedTest`.
- Both completed successfully.

### Notes

- The segmented controls now behave like deterministic styled buttons instead of toggle widgets.
- The local `.DS_Store` modification remains unrelated and was not touched.
