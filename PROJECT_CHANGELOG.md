# Project Change Log

This file tracks the work done on this project from this point forward. Each entry records why the change was needed, what was changed, which files were updated, and how the change was verified.

## How This Log Will Be Maintained

- Add a new entry after every implemented feature, UI update, bug fix, or meaningful project change.
- Keep entries in reverse chronological order, with the newest work at the top.
- Include the reason for the change, the implementation details, changed files, verification steps, and any follow-up notes.
- Mention commits when a change has already been committed or pushed.

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
