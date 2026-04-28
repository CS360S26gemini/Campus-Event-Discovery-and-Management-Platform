# Sprint Documentation: Nausherwan

This file documents the changes made during the sprint to facilitate integration and manage potential merge conflicts.

### 📋 Files Modified
* **`EventRepository.java`**:
    * Fixed missing `Nullable` import.
    * Added `getTiersForEvent` method to fetch ticket tiers from Firestore.
    * Updated `rsvpEvent` to support atomic tier capacity checks and increments.
* **`CreateEventActivity.java`**:
    * Implemented the **Tier Builder UI** logic (adding/removing tiers dynamically).
    * Integrated **Campus Location Selection** (Building dropdown and extra detail field).
    * Updated submission logic to save `locationKey` and `locationDescription`.
* **`EventDetailActivity.java`**:
    * Removed external Google Maps redirection.
    * Implemented internal redirection to `CampusMapActivity` using building keys.
    * Formatted the location string display as `{Description}, {Key}`.
* **`CheckoutActivity.java`**:
    * Integrated the **Ticket Tier Selection** RecyclerView directly above payment options.
    * Updated total price calculation and RSVP transaction to include selected tier data.
* **`Event.java` & `EventProposal.java`**:
    * Added `locationKey` and `locationDescription` fields to the models.
* **`Constants.java`**:
    * Added shared keys for all 10 campus buildings (`MAP_LOC_SSE`, `MAP_LOC_HSS`, etc.).
* **`strings.xml`**:
    * Added UI labels and error messages for Tiers and Campus Map features.
* **Layouts**:
    * `activity_create_event.xml`: Added building dropdown and Tier Builder containers.
    * `activity_checkout.xml`: Added tiers list for selection.
    * `activity_buy_ticket.xml`: Added loading states.
* **`TicketTierAdapter.java`**:
    * Updated to handle `tierId` and refined selection logic for checkout synchronization.
* **`AndroidManifest.xml`**:
    * Registered `CampusMapActivity`.

### 🆕 New Files Added
* **`app/src/main/java/com/example/CampusEventDiscovery/ui/event/CampusMapActivity.java`**: Handles zoomable/pannable campus map displays.
* **`app/src/main/res/layout/activity_campus_map.xml`**: Layout for the internal map viewer.
* **`app/src/main/res/layout/item_ticket_tier_builder.xml`**: Individual tier row for organizers.
* **`app/src/main/res/drawable/ic_delete.xml`**: Trash icon asset.

### 🔑 Constants Added
* `MAP_LOC_HSS`, `MAP_LOC_SSE`, `MAP_LOC_SAHSOL`, `MAP_LOC_SPORTS_COMPLEX`, `MAP_LOC_PARKING_LOT`, `MAP_LOC_REDC`, `MAP_LOC_CRICKET_GROUND`, `MAP_LOC_SDSB`, `MAP_LOC_IST`, `MAP_LOC_MASJID`.

### 🗄️ Firestore Collections or Fields Modified
* **`events` / `event_proposals`**: Added `locationKey` (String) and `locationDescription` (String).
* **`event_proposals`**: Added `tiers` (Array of Maps).

### 🛠️ Methods Added/Modified in `EventRepository`
* `public void getTiersForEvent(String eventId, TierListCallback cb)` (Added)
* `public void rsvpEvent(String userId, Event event, String fullName, @Nullable Map<String, Object> selectedTier, ActionCallback cb)` (Modified)

### 📦 External Dependencies Added
* *None* (Implemented zooming using native Android `Matrix` and `GestureDetector`).

### ⚠️ Known Conflicts
* **`EventDetailActivity.java`**: High probability of conflict as this file is central to attendee actions.
* **`Constants.java`**: Shared resource file.
* **`strings.xml`**: Shared resource file.

### ⏳ Merge Order Dependency
* My `EventDetailActivity` changes (specifically the location rendering) must be merged **before** Saad implements the blacklist gate to ensure building-level validation works as intended.
