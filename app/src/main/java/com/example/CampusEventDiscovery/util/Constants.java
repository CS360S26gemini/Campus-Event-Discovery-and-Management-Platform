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

    // Level 2 Sprint — New Constants
    public static final String SUBCOLLECTION_TICKET_TIERS        = "ticket_tiers";
    public static final String COLLECTION_CREDIT_TRANSACTIONS    = "credit_transactions";
    public static final String COLLECTION_APP_SETTINGS           = "app_settings";
    public static final String SETTINGS_DOC_GLOBAL               = "global";
    public static final String SUBCOLLECTION_VENDORS             = "vendors";
    public static final String SUBCOLLECTION_BLACKLIST           = "blacklist";
    public static final String COLLECTION_PLATFORM_BLACKLIST     = "platform_blacklist";
    public static final String EXTRA_DESTINATION_TAB             = "destinationTab";
    public static final String DESTINATION_TAB_CALENDAR          = "calendar";
    public static final String PREFS_RECENTLY_VIEWED             = "recently_viewed";
    public static final String PREFS_RECENTLY_VIEWED_KEY         = "event_ids";
    public static final String PREFS_AUTH                        = "auth_prefs";
    public static final String PREF_REMEMBER_ME                  = "remember_me";
    public static final String PREF_REMEMBERED_EMAIL             = "remembered_email";

    // RSVP payment statuses
    public static final String PAYMENT_PENDING   = "PENDING";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_REJECTED  = "REJECTED";

    // Campus map location keys (Nausher)
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
