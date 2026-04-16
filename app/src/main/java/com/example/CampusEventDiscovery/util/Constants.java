package com.example.CampusEventDiscovery.util;

/** Central source of truth for Firestore collection names used across the app. */
public final class Constants {

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_RSVPS = "rsvps";
    public static final String COLLECTION_SOS_ALERTS = "sos_alerts";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    public static final String SUBCOLLECTION_MESSAGES = "messages";

    private Constants() {
    }
}
