package com.example.CampusEventDiscovery.util;

/**
 * Constants.java
 *
 * Shared constants — mainly Firestore collection names so we
 * don't hardcode strings all over the place.
 */
public final class Constants {

    private Constants() {}

    public static final String COLLECTION_USERS         = "users";
    public static final String COLLECTION_EVENTS        = "events";
    public static final String COLLECTION_RSVPS         = "rsvps";
    public static final String COLLECTION_PAYMENTS      = "payments";
    public static final String COLLECTION_SOS_ALERTS    = "sos_alerts";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    public static final String SUBCOLLECTION_MESSAGES = "messages";

    public static final String EXTRA_DESTINATION_TAB = "destinationTab";
    public static final String DESTINATION_TAB_CALENDAR = "calendar";

    // RSVP payment statuses
    public static final String PAYMENT_PENDING   = "PENDING";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_REJECTED  = "REJECTED";

    // Map Location Keys
    public static final String MAP_LOC_HSS            = "HSS";
    public static final String MAP_LOC_SSE            = "SSE";
    public static final String MAP_LOC_SAHSOL         = "SAHSOL";
    public static final String MAP_LOC_SPORTS_COMPLEX = "Sports Complex";
    public static final String MAP_LOC_PARKING_LOT    = "Parking Lot";
    public static final String MAP_LOC_REDC           = "REDC";
    public static final String MAP_LOC_CRICKET_GROUND = "Cricket Ground";
    public static final String MAP_LOC_SDSB           = "SDSB";
    public static final String MAP_LOC_IST            = "IST";
    public static final String MAP_LOC_MASJID         = "Masjid";
}
