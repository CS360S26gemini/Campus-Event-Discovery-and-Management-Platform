## Files Modified
- `app/src/main/java/com/example/CampusEventDiscovery/util/Constants.java`: added reminder routing constants.
- `app/src/main/java/com/example/CampusEventDiscovery/CampusEventDiscoveryApp.java`: registered the reminder notification channel.
- `app/src/main/java/com/example/CampusEventDiscovery/util/MyFirebaseMessagingService.java`: added reminder notification handling and deep-link intent creation.
- `app/src/main/java/com/example/CampusEventDiscovery/MainActivity.java`: added reminder destination-tab routing.
- `functions/index.js`: added the scheduled reminder Cloud Function and helper utilities.

## New Files Added
- `firebase.json`
- `firestore.indexes.json`
- `docs/conflicts_yahya.md`

## Constants Added
- `EXTRA_DESTINATION_TAB = "destinationTab"`
- `DESTINATION_TAB_CALENDAR = "calendar"`

## Firestore Collections/Fields Modified
- `events` collection queried by `status` and `date` for reminder scheduling.
- No client-side schema fields were changed for this reminder slice.

## Methods Added or Removed from EventRepository
- None for this slice.

## External Dependencies Added
- None.

## Known Conflicts
- `MainActivity.java`
- `Constants.java`
- `CampusEventDiscoveryApp.java`
- `MyFirebaseMessagingService.java`
- `functions/index.js`

## Merge Order Dependency
- I must merge after the integration branch has settled any unrelated `Constants.java` or `MainActivity.java` changes, because this reminder work depends on those shared files staying stable.
