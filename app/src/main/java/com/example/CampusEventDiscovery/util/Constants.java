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
    public static final String COLLECTION_CREDIT_TRANSACTIONS = "credit_transactions";
    public static final String COLLECTION_SOS_ALERTS    = "sos_alerts";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    public static final String SUBCOLLECTION_MESSAGES = "messages";

    public static final String EXTRA_DESTINATION_TAB = "destinationTab";
    public static final String DESTINATION_TAB_CALENDAR = "calendar";

    // RSVP payment statuses
    public static final String PAYMENT_PENDING   = "PENDING";
    public static final String PAYMENT_CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_REJECTED  = "REJECTED";
    public static final String PAYMENT_REFUNDED  = "REFUNDED";

    public static final String PAYMENT_METHOD_JAZZCASH = "JAZZ_CASH";
    public static final String PAYMENT_METHOD_CREDIT_CARD = "CREDIT_CARD";
    public static final String PAYMENT_METHOD_DEBIT_CARD = "DEBIT_CARD";
    public static final String PAYMENT_METHOD_APPLE_PAY = "APPLE_PAY";
    public static final String PAYMENT_METHOD_IN_APP_CREDIT = "IN_APP_CREDIT";
    public static final String CREDIT_TRANSACTION_REFUND = "REFUND";
    public static final String CREDIT_TRANSACTION_USED = "USED";
}
