package com.example.CampusEventDiscovery.util;

/**
 * Constants.java
 *
 * Shared constants — mainly Firestore collection names so we
 * don't hardcode strings all over the place.
 */
public final class Constants {

    private Constants() {}

    public static final String COLLECTION_USERS       = "users";
    public static final String COLLECTION_EVENTS      = "events";
    public static final String COLLECTION_RSVPS       = "rsvps";
    public static final String COLLECTION_PAYMENTS    = "payments";
    public static final String COLLECTION_SOS_ALERTS  = "sos_alerts";

    // RSVP payment statuses
    public static final String PAYMENT_PENDING   = "PENDING";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_REJECTED  = "REJECTED";
}
