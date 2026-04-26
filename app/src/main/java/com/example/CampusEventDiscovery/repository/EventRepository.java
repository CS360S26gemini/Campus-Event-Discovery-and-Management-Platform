package com.example.CampusEventDiscovery.repository;

import android.text.TextUtils;
import android.util.Log;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventAttendee;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Date;

/**
 * EventRepository.java
 *
 * Repository class responsible for all Firestore operations.
 */
public class EventRepository {

    private static final String TAG = EventRepository.class.getSimpleName();

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_EVENTS = "events";
    private static final String COLLECTION_EVENT_PROPOSALS = "event_proposals";
    private static final String COLLECTION_REPORTS = "reports";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_APP_CONFIG = "app_config";

    private static final String SUBCOLLECTION_SAVED_EVENTS = "saved_events";
    private static final String SUBCOLLECTION_RSVPS = "rsvps";
    private static final String SUBCOLLECTION_MEMORIES = "memories";
    private static final String SUBCOLLECTION_RATINGS = "ratings";
    private static final String SUBCOLLECTION_ATTENDEES = "attendees";
    private static final String SUBCOLLECTION_MESSAGES = "messages";
    private static final String SUBCOLLECTION_BLACKLIST = "blacklist";
    private static final String DOCUMENT_SETTINGS = "settings";
    private static final int REFUND_WINDOW_DAYS = 3;
    private static final long REFUND_WINDOW_MILLIS = REFUND_WINDOW_DAYS * 24L * 60L * 60L * 1000L;

    private static final int FIRESTORE_WHERE_IN_LIMIT = 10;

    private final FirebaseFirestore db;

    /**
     * Default constructor for production use.
     */
    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor for testing to allow dependency injection.
     */
    public EventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface EventListCallback {
        void onSuccess(List<Event> events);
        void onError(Exception e);
    }

    public interface SingleEventCallback {
        void onSuccess(Event event);
        void onError(Exception e);
    }

    public interface ProposalListCallback {
        void onSuccess(List<EventProposal> proposals);
        void onError(Exception e);
    }

    public interface ProposalCallback {
        void onSuccess(EventProposal proposal);
        void onError(Exception e);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onError(Exception e);
    }

    public interface MemoryListCallback {
        void onSuccess(List<Memory> memories);
        void onError(Exception e);
    }

    public interface AttendeeListCallback {
        void onSuccess(List<EventAttendee> attendees);
        void onError(Exception e);
    }

    public interface FeaturedEventIdsCallback {
        void onSuccess(List<String> featuredIds);
        void onError(Exception e);
    }

    public interface BooleanCallback {
        void onSuccess(boolean value);
        void onError(Exception e);
    }

    public interface StringCallback {
        void onSuccess(String value);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static boolean isRefundEligible(Timestamp eventDate, Timestamp cancellationTime, boolean organizerInitiated) {
        if (organizerInitiated) {
            return true;
        }
        if (eventDate == null || cancellationTime == null) {
            return false;
        }
        long remaining = eventDate.toDate().getTime() - cancellationTime.toDate().getTime();
        return remaining >= REFUND_WINDOW_MILLIS;
    }

    public static double normalizeRefundAmount(double amount) {
        return Math.max(0.0, amount);
    }

    private double resolveCancellationAmount(DocumentSnapshot eventSnap, DocumentSnapshot userRsvpSnap) {
        Double storedAmount = null;
        if (userRsvpSnap != null) {
            storedAmount = userRsvpSnap.getDouble("amount");
            if (storedAmount == null) {
                storedAmount = userRsvpSnap.getDouble("ticketPrice");
            }
        }

        if (storedAmount == null && eventSnap != null) {
            storedAmount = eventSnap.getDouble("ticketPrice");
        }

        return normalizeRefundAmount(storedAmount != null ? storedAmount : 0.0);
    }

    public void incrementAttendeeCount(String eventId, ActionCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection(COLLECTION_EVENTS).document(eventId)
                .update("rsvpCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    // --- EVENT FETCHING ---

    public void getUpcomingEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    sortEventsByDateAscending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "getUpcomingEvents failed", error);
                    cb.onError(error);
                });
    }

    public void getFeaturedEvents(List<String> ids, EventListCallback cb) {
        if (ids == null || ids.isEmpty()) {
            cb.onSuccess(new ArrayList<>());
            return;
        }
        fetchEventsByIdsPreserveOrder(ids, null, cb);
    }

    public void getFeaturedEventIds(FeaturedEventIdsCallback cb) {
        db.collection(COLLECTION_APP_CONFIG)
                .document(DOCUMENT_SETTINGS)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> featuredIds = (List<String>) documentSnapshot.get("featuredEventIds");
                    cb.onSuccess(featuredIds != null ? new ArrayList<>(featuredIds) : new ArrayList<>());
                })
                .addOnFailureListener(cb::onError);
    }

    public void getMaintenanceMode(BooleanCallback cb) {
        db.collection(COLLECTION_APP_CONFIG)
                .document(DOCUMENT_SETTINGS)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean maintenanceMode = documentSnapshot.getBoolean("maintenanceMode");
                    cb.onSuccess(maintenanceMode != null && maintenanceMode);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getPersonalisedEvents(List<String> interests, EventListCallback cb) {
        if (interests == null || interests.isEmpty()) {
            getUpcomingEvents(cb);
            return;
        }
        List<String> safeInterests = new ArrayList<>(interests);
        if (safeInterests.size() > FIRESTORE_WHERE_IN_LIMIT) {
            safeInterests = safeInterests.subList(0, FIRESTORE_WHERE_IN_LIMIT);
        }
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .whereArrayContainsAny("tags", safeInterests)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    sortEventsByDateAscending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getPersonalisedEvents failed", e);
                    cb.onError(e);
                });
    }

    public void getEventById(String eventId, SingleEventCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            cb.onError(new Exception("Event ID is empty"));
            return;
        }
        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cb.onSuccess(documentToEvent(documentSnapshot));
                    } else {
                        cb.onError(new Exception("Event not found."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getEventById failed", e);
                    cb.onError(e);
                });
    }

    public ListenerRegistration observeEventById(String eventId, SingleEventCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new Exception("Event ID is empty"));
            return null;
        }

        return db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "observeEventById failed", error);
                        if (cb != null) cb.onError(error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        if (cb != null) cb.onSuccess(documentToEvent(documentSnapshot));
                    } else {
                        if (cb != null) cb.onError(new Exception("Event not found."));
                    }
                });
    }

    public void searchEvents(String query, String category, EventListCallback cb) {
        final String safeQuery = query == null ? "" : query.trim().toLowerCase();
        final String safeCategory = category == null ? "All" : category.trim();

        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> filtered = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Event event = documentToEvent(doc);
                        boolean matchesTitle = safeQuery.isEmpty() || (event.getTitle() != null && event.getTitle().toLowerCase().contains(safeQuery));
                        boolean matchesCategory = safeCategory.equalsIgnoreCase("All") || (event.getCategory() != null && event.getCategory().equalsIgnoreCase(safeCategory));
                        if (matchesTitle && matchesCategory) {
                            filtered.add(event);
                        }
                    }
                    sortEventsByDateAscending(filtered);
                    cb.onSuccess(filtered);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "searchEvents failed", e);
                    cb.onError(e);
                });
    }

    // --- USER ACTIONS: SAVE & RSVP ---

    public void saveEvent(String userId, Event event, ActionCallback cb) {
        if (userId == null || event == null || event.getEventId() == null) {
            if (cb != null) cb.onError(new Exception("Invalid data"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", event.getTitle());
        data.put("date", event.getDate());
        data.put("category", event.getCategory());
        data.put("thumbnailUrl", event.getThumbnailUrl());
        data.put("savedAt", Timestamp.now());

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_SAVED_EVENTS).document(event.getEventId())
                .set(data)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void unsaveEvent(String userId, String eventId, Runnable onSuccess) {
        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_SAVED_EVENTS).document(eventId)
                .delete()
                .addOnSuccessListener(unused -> runIfNotNull(onSuccess));
    }

    public void getSavedEvents(String userId, EventListCallback cb) {
        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_SAVED_EVENTS)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(savedSnapshots -> {
                    if (savedSnapshots.isEmpty()) {
                        cb.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<String> orderedIds = new ArrayList<>();
                    Map<String, Event> fallbackMap = new HashMap<>();
                    for (DocumentSnapshot doc : savedSnapshots.getDocuments()) {
                        String id = doc.getString("eventId");
                        if (id != null) {
                            orderedIds.add(id);
                            fallbackMap.put(id, savedDocumentToFallbackEvent(doc));
                        }
                    }
                    fetchEventsByIdsPreserveOrder(orderedIds, fallbackMap, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    public void rsvpEvent(String userId, Event event, String fullName, ActionCallback cb) {
        if (userId == null || event == null || event.getEventId() == null) {
            if (cb != null) cb.onError(new Exception("Invalid data"));
            return;
        }

        // Removed local capacity check to rely on Firestore source of truth inside transaction

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(event.getEventId());
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_RSVPS).document(event.getEventId());
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);
        DocumentReference blacklistRef = eventRef.collection(SUBCOLLECTION_BLACKLIST).document(userId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    if (!eventSnap.exists()) throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);

                    DocumentSnapshot existingRsvpSnap = transaction.get(userRsvpRef);
                    DocumentSnapshot existingAttendeeSnap = transaction.get(eventAttendeeRef);
                    DocumentSnapshot blacklistSnap = transaction.get(blacklistRef);

                    if (blacklistSnap.exists()) {
                        throw new FirebaseFirestoreException("You are not allowed to register for this event", FirebaseFirestoreException.Code.PERMISSION_DENIED);
                    }

                    boolean alreadyRegistered = existingAttendeeSnap.exists()
                            || (existingRsvpSnap.exists()
                            && !"cancelled".equalsIgnoreCase(existingRsvpSnap.getString("status")));
                    if (alreadyRegistered) {
                        throw new FirebaseFirestoreException("Already registered", FirebaseFirestoreException.Code.ABORTED);
                    }

                    Long cap = eventSnap.getLong("capacity");
                    long capacity = cap != null ? cap : 0L;
                    Long rsvp = eventSnap.getLong("rsvpCount");
                    long rsvpCount = rsvp != null ? rsvp : 0L;

                    if (rsvpCount >= capacity) throw new FirebaseFirestoreException("Event full", FirebaseFirestoreException.Code.ABORTED);

                    String qrToken = UUID.randomUUID().toString();

                    Map<String, Object> rsvpData = new HashMap<>();
                    rsvpData.put("eventId", event.getEventId());
                    rsvpData.put("title", event.getTitle());
                    rsvpData.put("date", event.getDate());
                    rsvpData.put("status", "confirmed");
                    rsvpData.put("checkedIn", false);
                    rsvpData.put("checkedInAt", null);
                    rsvpData.put("qrExpired", false);
                    rsvpData.put("qrCodeToken", qrToken);
                    rsvpData.put("addedToCalendar", false);
                    rsvpData.put("gcalEventId", "");
                    rsvpData.put("rsvpAt", Timestamp.now());
                    transaction.set(userRsvpRef, rsvpData);

                    Map<String, Object> attendeeData = new HashMap<>();
                    attendeeData.put("userId", userId);
                    attendeeData.put("fullName", TextUtils.isEmpty(fullName) ? "Attendee" : fullName);
                    attendeeData.put("qrToken", qrToken);
                    attendeeData.put("checkedIn", false);
                    attendeeData.put("checkedInAt", null);
                    transaction.set(eventAttendeeRef, attendeeData);

                    transaction.update(eventRef, "rsvpCount", FieldValue.increment(1));

                    return null;
                }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void rsvpEventWithCredit(String userId, Event event, String fullName, double amount, ActionCallback cb) {
        if (userId == null || event == null || event.getEventId() == null) {
            if (cb != null) cb.onError(new Exception("Invalid data"));
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(event.getEventId());
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        DocumentReference userRsvpRef = userRef.collection(SUBCOLLECTION_RSVPS).document(event.getEventId());
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);
        DocumentReference blacklistRef = eventRef.collection(SUBCOLLECTION_BLACKLIST).document(userId);
        DocumentReference paymentRef = db.collection(Constants.COLLECTION_PAYMENTS).document();
        DocumentReference creditTransactionRef = db.collection(Constants.COLLECTION_CREDIT_TRANSACTIONS).document();

        double safeAmount = normalizeRefundAmount(amount);

        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    if (!eventSnap.exists()) {
                        throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    DocumentSnapshot existingRsvpSnap = transaction.get(userRsvpRef);
                    DocumentSnapshot existingAttendeeSnap = transaction.get(eventAttendeeRef);
                    DocumentSnapshot blacklistSnap = transaction.get(blacklistRef);
                    DocumentSnapshot userSnap = transaction.get(userRef);

                    if (blacklistSnap.exists()) {
                        throw new FirebaseFirestoreException("You are not allowed to register for this event", FirebaseFirestoreException.Code.PERMISSION_DENIED);
                    }

                    boolean alreadyRegistered = existingAttendeeSnap.exists()
                            || (existingRsvpSnap.exists()
                            && !"cancelled".equalsIgnoreCase(existingRsvpSnap.getString("status")));
                    if (alreadyRegistered) {
                        throw new FirebaseFirestoreException("Already registered", FirebaseFirestoreException.Code.ABORTED);
                    }

                    double availableCredit = 0.0;
                    if (userSnap.exists()) {
                        Double currentCredit = userSnap.getDouble("creditBalance");
                        availableCredit = currentCredit != null ? currentCredit : 0.0;
                    }

                    if (availableCredit + 0.0001 < safeAmount) {
                        throw new FirebaseFirestoreException("Insufficient credit balance", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

                    Long cap = eventSnap.getLong("capacity");
                    long capacity = cap != null ? cap : 0L;
                    Long rsvp = eventSnap.getLong("rsvpCount");
                    long rsvpCount = rsvp != null ? rsvp : 0L;

                    if (rsvpCount >= capacity) {
                        throw new FirebaseFirestoreException("Event full", FirebaseFirestoreException.Code.ABORTED);
                    }

                    String transactionId = "credit_" + UUID.randomUUID().toString().replace("-", "");
                    String qrToken = UUID.randomUUID().toString();
                    Timestamp now = Timestamp.now();

                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("userId", userId);
                    paymentData.put("eventId", event.getEventId());
                    paymentData.put("amount", safeAmount);
                    paymentData.put("status", Constants.PAYMENT_CONFIRMED);
                    paymentData.put("transactionId", transactionId);
                    paymentData.put("timestamp", now.toDate().getTime());
                    paymentData.put("paymentMethod", Constants.PAYMENT_METHOD_IN_APP_CREDIT);
                    paymentData.put("proofUrl", "");
                    transaction.set(paymentRef, paymentData);

                    Map<String, Object> creditTransaction = new HashMap<>();
                    creditTransaction.put("userId", userId);
                    creditTransaction.put("type", Constants.CREDIT_TRANSACTION_USED);
                    creditTransaction.put("amount", safeAmount);
                    creditTransaction.put("eventId", event.getEventId());
                    creditTransaction.put("eventTitle", event.getTitle());
                    creditTransaction.put("originalAmount", safeAmount);
                    creditTransaction.put("createdAt", now);
                    creditTransaction.put("paymentTransactionId", transactionId);
                    transaction.set(creditTransactionRef, creditTransaction);

                    Map<String, Object> rsvpData = new HashMap<>();
                    rsvpData.put("eventId", event.getEventId());
                    rsvpData.put("title", event.getTitle());
                    rsvpData.put("date", event.getDate());
                    rsvpData.put("status", "confirmed");
                    rsvpData.put("paymentStatus", Constants.PAYMENT_CONFIRMED);
                    rsvpData.put("transactionId", transactionId);
                    rsvpData.put("paymentRef", transactionId);
                    rsvpData.put("paymentMethod", Constants.PAYMENT_METHOD_IN_APP_CREDIT);
                    rsvpData.put("paymentProofUrl", "");
                    rsvpData.put("checkedIn", false);
                    rsvpData.put("checkedInAt", null);
                    rsvpData.put("qrExpired", false);
                    rsvpData.put("qrCodeToken", qrToken);
                    rsvpData.put("addedToCalendar", false);
                    rsvpData.put("gcalEventId", "");
                    rsvpData.put("rsvpAt", now);
                    transaction.set(userRsvpRef, rsvpData);

                    Map<String, Object> attendeeData = new HashMap<>();
                    attendeeData.put("userId", userId);
                    attendeeData.put("fullName", TextUtils.isEmpty(fullName) ? "Attendee" : fullName);
                    attendeeData.put("qrToken", qrToken);
                    attendeeData.put("checkedIn", false);
                    attendeeData.put("checkedInAt", null);
                    transaction.set(eventAttendeeRef, attendeeData);

                    transaction.set(userRef, Collections.singletonMap("creditBalance", FieldValue.increment(-safeAmount)), SetOptions.merge());
                    transaction.update(eventRef, "rsvpCount", FieldValue.increment(1));

                    return null;
                }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void cancelRsvp(String userId, String eventId, ActionCallback cb) {
        cancelRsvp(userId, eventId, false, cb);
    }

    public void cancelRsvp(String userId, String eventId, boolean organizerInitiated, ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new Exception("Invalid user or event ID"));
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        DocumentReference userRsvpRef = userRef.collection(SUBCOLLECTION_RSVPS).document(eventId);
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);
        DocumentReference creditTransactionRef = db.collection(Constants.COLLECTION_CREDIT_TRANSACTIONS).document();

        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    DocumentSnapshot userRsvpSnap = transaction.get(userRsvpRef);
                    DocumentSnapshot attendeeSnap = transaction.get(eventAttendeeRef);
                    DocumentSnapshot userSnap = transaction.get(userRef);

                    boolean hadActiveRsvp = attendeeSnap.exists();
                    boolean wasCheckedIn = false;
                    if (userRsvpSnap.exists()) {
                        String currentStatus = userRsvpSnap.getString("status");
                        hadActiveRsvp = hadActiveRsvp || !"cancelled".equalsIgnoreCase(currentStatus);

                        Boolean rsvpCheckedIn = userRsvpSnap.getBoolean("checkedIn");
                        wasCheckedIn = rsvpCheckedIn != null && rsvpCheckedIn;

                        Map<String, Object> cancelledRsvpData = new HashMap<>();
                        cancelledRsvpData.put("status", "cancelled");
                        cancelledRsvpData.put("addedToCalendar", false);
                        cancelledRsvpData.put("gcalEventId", "");
                        cancelledRsvpData.put("paymentStatus", organizerInitiated ? Constants.PAYMENT_REFUNDED : userRsvpSnap.getString("paymentStatus"));
                        transaction.set(userRsvpRef, cancelledRsvpData, SetOptions.merge());
                    }

                    if (attendeeSnap.exists()) {
                        Boolean attendeeCheckedIn = attendeeSnap.getBoolean("checkedIn");
                        wasCheckedIn = wasCheckedIn || (attendeeCheckedIn != null && attendeeCheckedIn);
                        transaction.delete(eventAttendeeRef);
                    }

                    double refundAmount = 0.0;
                    if (hadActiveRsvp && eventSnap.exists()) {
                        double candidateAmount = resolveCancellationAmount(eventSnap, userRsvpSnap);
                        if (isRefundEligible(eventSnap.getTimestamp("date"), Timestamp.now(), organizerInitiated)) {
                            refundAmount = candidateAmount;
                        }
                    }

                    if (refundAmount > 0.0) {
                        transaction.set(userRsvpRef,
                                Collections.singletonMap("paymentStatus", Constants.PAYMENT_REFUNDED),
                                SetOptions.merge());

                        Map<String, Object> creditTransaction = new HashMap<>();
                        creditTransaction.put("userId", userId);
                        creditTransaction.put("type", Constants.CREDIT_TRANSACTION_REFUND);
                        creditTransaction.put("amount", refundAmount);
                        creditTransaction.put("eventId", eventId);
                        creditTransaction.put("eventTitle", eventSnap.exists() ? eventSnap.getString("title") : "");
                        creditTransaction.put("originalAmount", refundAmount);
                        creditTransaction.put("createdAt", Timestamp.now());
                        creditTransaction.put("reason", organizerInitiated ? "ORGANIZER_CANCELLED" : "ATTENDEE_CANCELLED");
                        transaction.set(creditTransactionRef, creditTransaction);
                        transaction.set(userRef, Collections.singletonMap("creditBalance", FieldValue.increment(refundAmount)), SetOptions.merge());
                    }

                    if (hadActiveRsvp && eventSnap.exists()) {
                        Long currentCount = eventSnap.getLong("rsvpCount");
                        long safeCount = Math.max(0L, currentCount != null ? currentCount : 0L);
                        Map<String, Object> eventUpdates = new HashMap<>();
                        eventUpdates.put("rsvpCount", Math.max(0L, safeCount - 1L));

                        if (wasCheckedIn) {
                            Long currentCheckedInCount = eventSnap.getLong("checkedInCount");
                            long safeCheckedInCount = Math.max(0L, currentCheckedInCount != null ? currentCheckedInCount : 0L);
                            eventUpdates.put("checkedInCount", Math.max(0L, safeCheckedInCount - 1L));
                        }

                        transaction.update(eventRef, eventUpdates);
                    }
                    return null;
                }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getRsvps(String userId, EventListCallback cb) {
        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_RSVPS)
                .orderBy("rsvpAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(rsvpSnapshots -> {
                    List<String> orderedIds = new ArrayList<>();
                    for (DocumentSnapshot doc : rsvpSnapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"cancelled".equalsIgnoreCase(status)) {
                            orderedIds.add(doc.getString("eventId"));
                        }
                    }
                    fetchEventsByIdsPreserveOrder(orderedIds, null, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    // --- MEMORIES & RATINGS ---

    public void addMemory(String userId, String eventId, String eventTitle, List<String> photoUrls, int rating) {
        addMemory(userId, eventId, eventTitle, photoUrls, rating, null);
    }

    public void addMemory(String userId,
                          String eventId,
                          String eventTitle,
                          List<String> photoUrls,
                          int rating,
                          ActionCallback cb) {
        Map<String, Object> memory = new HashMap<>();
        memory.put("eventId", eventId);
        memory.put("eventTitle", eventTitle);
        memory.put("photoUrls", photoUrls);
        memory.put("attendedAt", Timestamp.now());
        memory.put("rating", rating);

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .add(memory)
                .addOnSuccessListener(unused -> {
                    if (cb != null) cb.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void getMemories(String userId, MemoryListCallback cb) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .orderBy("attendedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Memory> memories = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Memory memory = doc.toObject(Memory.class);
                        if (memory != null) {
                            memories.add(memory);
                        }
                    }
                    cb.onSuccess(memories);
                })
                .addOnFailureListener(cb::onError);
    }

    public void addRating(String eventId, String userId, int stars, String review) {
        addRating(eventId, userId, stars, review, null);
    }

    public void addRating(String eventId,
                          String userId,
                          int stars,
                          String review,
                          ActionCallback cb) {
        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference ratingRef = eventRef.collection(SUBCOLLECTION_RATINGS).document(userId);

        Map<String, Object> rating = new HashMap<>();
        rating.put("userId", userId);
        rating.put("stars", stars);
        rating.put("review", review);
        rating.put("ratedAt", Timestamp.now());

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            DocumentSnapshot existingRatingSnap = transaction.get(ratingRef);
            Long countL = eventSnap.getLong("ratingCount");
            long count = countL != null ? countL : 0L;
            Double avgD = eventSnap.getDouble("averageRating");
            double avg = avgD != null ? avgD : 0.0;

            boolean hasExistingRating = existingRatingSnap.exists();
            long newCount = hasExistingRating ? Math.max(1L, count) : count + 1L;
            long previousStars = 0L;
            if (hasExistingRating) {
                Long previousStarsLong = existingRatingSnap.getLong("stars");
                previousStars = previousStarsLong != null ? previousStarsLong : 0L;
            }

            double totalStars = (avg * count) - previousStars + stars;
            if (!hasExistingRating) {
                totalStars = (avg * count) + stars;
            }
            double newAvg = newCount > 0 ? totalStars / newCount : 0.0;

            transaction.set(ratingRef, rating);
            transaction.update(eventRef, "averageRating", newAvg, "ratingCount", newCount);
            return null;
        }).addOnSuccessListener(unused -> {
            if (cb != null) cb.onSuccess();
        }).addOnFailureListener(e -> {
            if (cb != null) cb.onError(e);
        });
    }

    // --- ORGANIZER & ADMIN ---

    public void proposeEvent(EventProposal proposal, ActionCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .add(proposal)
                .addOnSuccessListener(ref -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void createEvent(Event event, ActionCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .document(event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId())
                .set(event)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getProposalById(String proposalId, ProposalCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .document(proposalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        cb.onError(new Exception("Proposal not found."));
                        return;
                    }
                    EventProposal proposal = documentSnapshot.toObject(EventProposal.class);
                    if (proposal == null) {
                        cb.onError(new Exception("Proposal not found."));
                        return;
                    }
                    proposal.setProposalId(documentSnapshot.getId());
                    cb.onSuccess(proposal);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getRsvpQrToken(String userId, String eventId, StringCallback cb) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_RSVPS)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        cb.onError(new Exception("RSVP not found."));
                        return;
                    }

                    String status = documentSnapshot.getString("status");
                    if ("cancelled".equalsIgnoreCase(status)) {
                        cb.onError(new Exception("RSVP is cancelled."));
                        return;
                    }

                    String qrCodeToken = documentSnapshot.getString("qrCodeToken");
                    if (TextUtils.isEmpty(qrCodeToken)) {
                        cb.onError(new Exception("Check-in code unavailable."));
                        return;
                    }

                    cb.onSuccess(qrCodeToken);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getOrganizerEvents(String organizerId, EventListCallback cb) {
        if (TextUtils.isEmpty(organizerId)) {
            cb.onError(new Exception("Organizer ID is empty"));
            return;
        }
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("organizerId", organizerId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    sortEventsByDateAscending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllActiveEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    sortEventsByDateAscending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getOrganizerProposals(String organizerId, ProposalListCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<EventProposal> proposals = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        EventProposal p = doc.toObject(EventProposal.class);
                        if (p != null) {
                            p.setProposalId(doc.getId());
                            proposals.add(p);
                        }
                    }
                    sortProposalsBySubmittedAtDescending(proposals);
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public ListenerRegistration observeAllPendingProposals(ProposalListCallback cb) {
        return db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("status", "pending")
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "observeAllPendingProposals failed", error);
                        cb.onError(error);
                        return;
                    }
                    List<EventProposal> proposals = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            EventProposal p = doc.toObject(EventProposal.class);
                            if (p != null) {
                                p.setProposalId(doc.getId());
                                proposals.add(p);
                            }
                        }
                    }
                    cb.onSuccess(proposals);
                });
    }

    public void getAllPendingProposals(ProposalListCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<EventProposal> proposals = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        EventProposal p = doc.toObject(EventProposal.class);
                        if (p != null) {
                            p.setProposalId(doc.getId());
                            proposals.add(p);
                        }
                    }
                    sortProposalsBySubmittedAtAscending(proposals);
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllProposalsByStatus(String status, ProposalListCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<EventProposal> proposals = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        EventProposal p = doc.toObject(EventProposal.class);
                        if (p != null) {
                            p.setProposalId(doc.getId());
                            proposals.add(p);
                        }
                    }
                    sortProposalsBySubmittedAtDescending(proposals);
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getPendingEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void approveProposal(String proposalId, EventProposal proposal, ActionCallback cb) {
        if (TextUtils.isEmpty(proposalId) || proposal == null) {
            if (cb != null) cb.onError(new Exception("Invalid proposal"));
            return;
        }
        DocumentReference proposalRef = db.collection(COLLECTION_EVENT_PROPOSALS).document(proposalId);
        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document();

        db.runTransaction(transaction -> {
                    DocumentSnapshot proposalSnap = transaction.get(proposalRef);
                    if (!proposalSnap.exists()) {
                        throw new FirebaseFirestoreException("Proposal not found", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    String status = proposalSnap.getString("status");
                    if (!TextUtils.isEmpty(status) && !"pending".equalsIgnoreCase(status)) {
                        throw new FirebaseFirestoreException("Proposal has already been reviewed", FirebaseFirestoreException.Code.ABORTED);
                    }

                    transaction.update(proposalRef,
                            "status", "approved",
                            "approvedEventId", eventRef.getId(),
                            "adminNote", "",
                            "reviewedAt", Timestamp.now());
                    transaction.set(eventRef, proposalToApprovedEvent(proposal));

                    if (!TextUtils.isEmpty(proposal.getOrganizerId())) {
                        DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS)
                                .document(proposal.getOrganizerId())
                                .collection(SUBCOLLECTION_MESSAGES)
                                .document();
                        transaction.set(notificationRef, buildNotification(
                                "Your event was approved",
                                proposal.getTitle() + " is now live.",
                                "event_approved",
                                eventRef.getId()
                        ));
                    }

                    return null;
                })
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void approveEvent(String eventId, ActionCallback cb) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .update("status", "active")
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void rejectProposal(String proposalId, String note, ActionCallback cb) {
        rejectProposal(proposalId, null, note, cb);
    }

    public void rejectProposal(String proposalId, EventProposal proposal, String note, ActionCallback cb) {
        WriteBatch batch = db.batch();
        DocumentReference proposalRef = db.collection(COLLECTION_EVENT_PROPOSALS).document(proposalId);

        batch.update(proposalRef,
                "status", "rejected",
                "adminNote", note,
                "reviewedAt", Timestamp.now());

        if (proposal != null && !TextUtils.isEmpty(proposal.getOrganizerId())) {
            DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS)
                    .document(proposal.getOrganizerId())
                    .collection(SUBCOLLECTION_MESSAGES)
                    .document();
            batch.set(notificationRef, buildNotification(
                    "Your event was rejected",
                    proposal.getTitle() + " needs changes before it can go live.",
                    "event_rejected",
                    null
            ));
        }

        batch.commit()
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void rejectEvent(String eventId, ActionCallback cb) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .update("status", "rejected")
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void deleteEvent(String eventId, String deletedByUserId, ActionCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new Exception("Event ID is empty"));
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);

        eventRef.get()
                .addOnSuccessListener(eventSnap -> eventRef.collection(SUBCOLLECTION_ATTENDEES).get()
                        .addOnSuccessListener(attendeeSnapshots -> {
                            double refundAmount = 0.0;
                            if (eventSnap.exists()) {
                                Double ticketPrice = eventSnap.getDouble("ticketPrice");
                                refundAmount = normalizeRefundAmount(ticketPrice != null ? ticketPrice : 0.0);
                            }

                            WriteBatch batch = db.batch();
                            for (DocumentSnapshot attendeeDoc : attendeeSnapshots.getDocuments()) {
                                String attendeeUserId = attendeeDoc.getString("userId");
                                if (TextUtils.isEmpty(attendeeUserId)) {
                                    attendeeUserId = attendeeDoc.getId();
                                }
                                if (TextUtils.isEmpty(attendeeUserId)) {
                                    continue;
                                }

                                DocumentReference userRef = db.collection(COLLECTION_USERS).document(attendeeUserId);
                                DocumentReference userRsvpRef = userRef.collection(SUBCOLLECTION_RSVPS).document(eventId);
                                DocumentReference creditTransactionRef = db.collection(Constants.COLLECTION_CREDIT_TRANSACTIONS).document();

                                Map<String, Object> cancelledRsvpData = new HashMap<>();
                                cancelledRsvpData.put("status", "cancelled");
                                cancelledRsvpData.put("addedToCalendar", false);
                                cancelledRsvpData.put("gcalEventId", "");
                                cancelledRsvpData.put("paymentStatus", Constants.PAYMENT_REFUNDED);
                                batch.set(userRsvpRef, cancelledRsvpData, SetOptions.merge());
                                batch.delete(eventRef.collection(SUBCOLLECTION_ATTENDEES).document(attendeeUserId));

                                if (refundAmount > 0.0) {
                                    Map<String, Object> creditTransaction = new HashMap<>();
                                    creditTransaction.put("userId", attendeeUserId);
                                    creditTransaction.put("type", Constants.CREDIT_TRANSACTION_REFUND);
                                    creditTransaction.put("amount", refundAmount);
                                    creditTransaction.put("eventId", eventId);
                                    creditTransaction.put("eventTitle", eventSnap.exists() ? eventSnap.getString("title") : "");
                                    creditTransaction.put("originalAmount", refundAmount);
                                    creditTransaction.put("createdAt", Timestamp.now());
                                    creditTransaction.put("reason", "ORGANIZER_CANCELLED");
                                    batch.set(creditTransactionRef, creditTransaction);
                                    batch.set(userRef, Collections.singletonMap("creditBalance", FieldValue.increment(refundAmount)), SetOptions.merge());
                                }
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "deleted");
                            updates.put("deletedAt", Timestamp.now());
                            updates.put("deletedBy", deletedByUserId == null ? "" : deletedByUserId);
                            updates.put("rsvpCount", 0L);
                            updates.put("checkedInCount", 0L);
                            batch.set(eventRef, updates, SetOptions.merge());

                            batch.commit()
                                    .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                                    .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
                        })
                        .addOnFailureListener(e -> { if (cb != null) cb.onError(e); }))
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getEventAttendees(String eventId, AttendeeListCallback cb) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .collection(SUBCOLLECTION_ATTENDEES)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<EventAttendee> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        EventAttendee attendee = doc.toObject(EventAttendee.class);
                        if (attendee != null) {
                            if (TextUtils.isEmpty(attendee.getUserId())) {
                                attendee.setUserId(doc.getId());
                            }
                            list.add(attendee);
                        }
                    }
                    Collections.sort(list, Comparator.comparing(
                            attendee -> attendee.getFullName() == null ? "" : attendee.getFullName().toLowerCase()
                    ));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void blacklistAttendees(String eventId, List<EventAttendee> attendees, ActionCallback cb) {
        if (TextUtils.isEmpty(eventId) || attendees == null || attendees.isEmpty()) {
            if (cb != null) cb.onSuccess();
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            Long currentCountLong = eventSnap.getLong("rsvpCount");
            long currentCount = currentCountLong != null ? currentCountLong : 0L;
            Long currentCheckedInCountLong = eventSnap.getLong("checkedInCount");
            long currentCheckedInCount = currentCheckedInCountLong != null ? currentCheckedInCountLong : 0L;
            long removedCount = 0L;
            long removedCheckedInCount = 0L;

            for (EventAttendee attendee : attendees) {
                if (attendee == null || TextUtils.isEmpty(attendee.getUserId())) {
                    continue;
                }

                String attendeeUserId = attendee.getUserId();
                DocumentReference attendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(attendeeUserId);
                DocumentReference blacklistRef = eventRef.collection(SUBCOLLECTION_BLACKLIST).document(attendeeUserId);
                DocumentReference userRsvpRef = db.collection(COLLECTION_USERS)
                        .document(attendeeUserId)
                        .collection(SUBCOLLECTION_RSVPS)
                        .document(eventId);

                Map<String, Object> blacklistData = new HashMap<>();
                blacklistData.put("userId", attendeeUserId);
                blacklistData.put("fullName", attendee.getFullName());
                blacklistData.put("qrToken", attendee.getQrToken());
                blacklistData.put("blacklistedAt", Timestamp.now());
                transaction.set(blacklistRef, blacklistData);

                DocumentSnapshot attendeeSnap = transaction.get(attendeeRef);
                if (attendeeSnap.exists()) {
                    Boolean attendeeCheckedIn = attendeeSnap.getBoolean("checkedIn");
                    if (attendeeCheckedIn != null && attendeeCheckedIn) {
                        removedCheckedInCount++;
                    }
                    transaction.delete(attendeeRef);
                    removedCount++;
                }

                Map<String, Object> cancelledRsvpData = new HashMap<>();
                cancelledRsvpData.put("status", "cancelled");
                cancelledRsvpData.put("addedToCalendar", false);
                cancelledRsvpData.put("gcalEventId", "");
                transaction.set(userRsvpRef, cancelledRsvpData, SetOptions.merge());
            }

            if (removedCount > 0L && eventSnap.exists()) {
                Map<String, Object> eventUpdates = new HashMap<>();
                eventUpdates.put("rsvpCount", Math.max(0L, currentCount - removedCount));
                if (removedCheckedInCount > 0L) {
                    eventUpdates.put("checkedInCount", Math.max(0L, currentCheckedInCount - removedCheckedInCount));
                }
                transaction.update(eventRef, eventUpdates);
            }

            return null;
        }).addOnSuccessListener(unused -> {
            if (cb != null) cb.onSuccess();
        }).addOnFailureListener(e -> {
            if (cb != null) cb.onError(e);
        });
    }

    public void getBlacklistedAttendees(String eventId, AttendeeListCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            cb.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_EVENTS).document(eventId)
                .collection(SUBCOLLECTION_BLACKLIST)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<EventAttendee> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        EventAttendee attendee = doc.toObject(EventAttendee.class);
                        if (attendee != null) {
                            if (TextUtils.isEmpty(attendee.getUserId())) {
                                attendee.setUserId(doc.getId());
                            }
                            attendee.setBlacklisted(true);
                            list.add(attendee);
                        }
                    }
                    Collections.sort(list, Comparator.comparing(
                            attendee -> attendee.getFullName() == null ? "" : attendee.getFullName().toLowerCase()
                    ));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void checkInAttendeeByQrToken(String eventId, String qrToken, ActionCallback cb) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(qrToken)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Event ID and check-in code are required"));
            return;
        }

        db.collection(COLLECTION_EVENTS).document(eventId)
                .collection(SUBCOLLECTION_ATTENDEES)
                .whereEqualTo("qrToken", qrToken.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        db.collection(COLLECTION_EVENTS).document(eventId)
                                .collection(SUBCOLLECTION_BLACKLIST)
                                .whereEqualTo("qrToken", qrToken.trim())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(blacklistSnapshots -> {
                                    if (blacklistSnapshots != null && !blacklistSnapshots.isEmpty()) {
                                        String name = blacklistSnapshots.getDocuments().get(0).getString("fullName");
                                        if (TextUtils.isEmpty(name)) {
                                            name = "This attendee";
                                        }
                                        if (cb != null) cb.onError(new Exception(name + " has been blacklisted and should not be allowed to enter"));
                                    } else if (cb != null) {
                                        cb.onError(new Exception("Check-in code not found"));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (cb != null) cb.onError(e);
                                });
                        return;
                    }

                    DocumentSnapshot match = queryDocumentSnapshots.getDocuments().get(0);
                    String attendeeUserId = match.getString("userId");
                    if (TextUtils.isEmpty(attendeeUserId)) {
                        attendeeUserId = match.getId();
                    }

                    checkInAttendanceTransaction(eventId, attendeeUserId, null, cb);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void checkInAttendeeByScan(String eventId, String userId, String transactionId, ActionCallback cb) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(transactionId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Event ID, user ID, and transaction ID are required"));
            return;
        }

        checkInAttendanceTransaction(eventId.trim(), userId.trim(), transactionId.trim(), cb);
    }

    private void checkInAttendanceTransaction(String eventId, String attendeeUserId, String expectedTransactionId, ActionCallback cb) {
        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference attendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(attendeeUserId);
        DocumentReference blacklistRef = eventRef.collection(SUBCOLLECTION_BLACKLIST).document(attendeeUserId);
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS)
                .document(attendeeUserId)
                .collection(SUBCOLLECTION_RSVPS)
                .document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            if (!eventSnapshot.exists()) {
                throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentSnapshot attendeeSnapshot = transaction.get(attendeeRef);
            DocumentSnapshot blacklistSnapshot = transaction.get(blacklistRef);
            if (blacklistSnapshot.exists()) {
                String name = blacklistSnapshot.getString("fullName");
                if (TextUtils.isEmpty(name)) {
                    name = attendeeSnapshot.exists() ? attendeeSnapshot.getString("fullName") : "This attendee";
                }
                throw new FirebaseFirestoreException(name + " has been blacklisted and should not be allowed to enter", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            if (!attendeeSnapshot.exists()) {
                throw new FirebaseFirestoreException("Attendee not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentSnapshot rsvpSnapshot = transaction.get(userRsvpRef);
            if (!rsvpSnapshot.exists()) {
                throw new FirebaseFirestoreException("RSVP not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String status = rsvpSnapshot.getString("status");
            if ("cancelled".equalsIgnoreCase(status)) {
                throw new FirebaseFirestoreException("Cancelled RSVP cannot be checked in", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            if (!TextUtils.isEmpty(expectedTransactionId)) {
                String storedTransactionId = rsvpSnapshot.getString("transactionId");
                if (!expectedTransactionId.equals(storedTransactionId)) {
                    throw new FirebaseFirestoreException("QR does not match RSVP", FirebaseFirestoreException.Code.PERMISSION_DENIED);
                }
            }

            Boolean attendeeCheckedIn = attendeeSnapshot.getBoolean("checkedIn");
            Boolean rsvpCheckedIn = rsvpSnapshot.getBoolean("checkedIn");
            Boolean qrExpired = rsvpSnapshot.getBoolean("qrExpired");
            boolean alreadyCheckedIn = (attendeeCheckedIn != null && attendeeCheckedIn)
                    || (rsvpCheckedIn != null && rsvpCheckedIn)
                    || (qrExpired != null && qrExpired);

            if (alreadyCheckedIn) {
                throw new FirebaseFirestoreException("Attendee already checked in", FirebaseFirestoreException.Code.ABORTED);
            }

            Timestamp now = Timestamp.now();

            Map<String, Object> attendeeUpdates = new HashMap<>();
            attendeeUpdates.put("checkedIn", true);
            attendeeUpdates.put("checkedInAt", now);
            transaction.set(attendeeRef, attendeeUpdates, SetOptions.merge());

            Map<String, Object> rsvpUpdates = new HashMap<>();
            rsvpUpdates.put("checkedIn", true);
            rsvpUpdates.put("qrExpired", true);
            rsvpUpdates.put("checkedInAt", now);
            rsvpUpdates.put("status", "attended");
            transaction.set(userRsvpRef, rsvpUpdates, SetOptions.merge());

            transaction.update(eventRef, "checkedInCount", FieldValue.increment(1));
            return null;
        }).addOnSuccessListener(unused -> {
            if (cb != null) cb.onSuccess();
        }).addOnFailureListener(e -> {
            if (cb != null) cb.onError(e);
        });
    }

    // --- SOS REPORTS ---

    public void sendSosReport(String reporterId, String reporterName, String description, double lat, double lng) {
        sendSosReport(reporterId, reporterName, description, lat, lng, null);
    }

    public void sendSosReport(String reporterId,
                              String reporterName,
                              String description,
                              double lat,
                              double lng,
                              ActionCallback cb) {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reporterName", reporterName);
        report.put("isAnonymous", TextUtils.isEmpty(reporterId));
        report.put("description", description);

        Map<String, Object> location = new HashMap<>();
        location.put("lat", lat);
        location.put("lng", lng);
        report.put("location", location);

        report.put("status", "open");
        report.put("adminNote", null);
        report.put("submittedAt", Timestamp.now());
        report.put("resolvedAt", null);

        db.collection(COLLECTION_REPORTS)
                .add(report)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    // --- NOTIFICATIONS ---

    public void getNotifications(String userId, NotificationListCallback cb) {
        db.collection(COLLECTION_NOTIFICATIONS).document(userId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Notification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setNotificationId(doc.getId());
                            list.add(n);
                        }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void markNotificationRead(String userId, String notificationId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(notificationId)) {
            return;
        }

        db.collection(COLLECTION_NOTIFICATIONS)
                .document(userId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(notificationId)
                .update("read", true, "isRead", true)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to mark notification as read", e));
    }

    public void sendAnnouncementToAttendees(String eventId,
                                            String eventTitle,
                                            String message,
                                            ActionCallback cb) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(message)) {
            if (cb != null) cb.onError(new Exception("Announcement message is required"));
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(SUBCOLLECTION_ATTENDEES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (cb != null) cb.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot attendeeDoc : queryDocumentSnapshots.getDocuments()) {
                        String attendeeUserId = attendeeDoc.getString("userId");
                        if (TextUtils.isEmpty(attendeeUserId)) {
                            attendeeUserId = attendeeDoc.getId();
                        }
                        if (TextUtils.isEmpty(attendeeUserId)) {
                            continue;
                        }

                        DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS)
                                .document(attendeeUserId)
                                .collection(SUBCOLLECTION_MESSAGES)
                                .document();
                        batch.set(notificationRef, buildNotification(
                                TextUtils.isEmpty(eventTitle) ? "Event announcement" : "Update for " + eventTitle,
                                message,
                                "event_announcement",
                                eventId
                        ));
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (cb != null) cb.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (cb != null) cb.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    // --- USER PROFILE ---

    public void getUserData(String userId, UserCallback cb) {
        db.collection(COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        cb.onSuccess(doc.toObject(User.class));
                    } else {
                        cb.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void updateDarkMode(String userId, boolean darkMode) {
        db.collection(COLLECTION_USERS).document(userId).update("darkMode", darkMode);
    }

    public void updateUserProfile(String userId,
                                  String fullName,
                                  String university,
                                  String location,
                                  ActionCallback cb) {
        updateUserProfile(userId, fullName, null, university, location, null, cb);
    }

    public void updateUserProfile(String userId,
                                  String fullName,
                                  String university,
                                  String location,
                                  List<String> interests,
                                  ActionCallback cb) {
        updateUserProfile(userId, fullName, null, university, location, interests, cb);
    }

    public void updateUserProfile(String userId,
                                  String fullName,
                                  String email,
                                  String university,
                                  String location,
                                  List<String> interests,
                                  ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        if (email != null) {
            updates.put("email", email);
        }
        updates.put("university", university);
        updates.put("location", location);
        if (interests != null) {
            updates.put("interests", interests);
        }

        db.collection(COLLECTION_USERS).document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void updateProfilePic(String userId, String url, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePicUrl", url);
        updates.put("avatarEnabled", false);

        db.collection(COLLECTION_USERS).document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void updateProfileAvatar(String userId, Map<String, Object> avatarConfig, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarEnabled", true);
        updates.put("avatarConfig", avatarConfig == null ? new HashMap<>() : avatarConfig);

        db.collection(COLLECTION_USERS).document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void updateProfileVisualPreference(String userId, boolean avatarEnabled, ActionCallback cb) {
        db.collection(COLLECTION_USERS).document(userId)
                .set(Collections.singletonMap("avatarEnabled", avatarEnabled), SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void markRsvpAddedToCalendar(String userId, String eventId, String gcalEventId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            return;
        }

        DocumentReference rsvpRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_RSVPS)
                .document(eventId);

        rsvpRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("addedToCalendar", true);
                    updates.put("gcalEventId", gcalEventId == null ? "" : gcalEventId);
                    rsvpRef.update(updates)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to update RSVP calendar metadata", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to inspect RSVP before calendar update", e));
    }

    // --- HELPERS ---

    private Event documentToEvent(DocumentSnapshot doc) {
        Event event = doc.toObject(Event.class);
        if (event == null) event = new Event();
        event.setEventId(doc.getId());
        return event;
    }

    private Event savedDocumentToFallbackEvent(DocumentSnapshot doc) {
        Event event = new Event();
        event.setEventId(doc.getString("eventId"));
        event.setTitle(doc.getString("title"));
        event.setDate(doc.getTimestamp("date"));
        event.setCategory(doc.getString("category"));
        event.setThumbnailUrl(doc.getString("thumbnailUrl"));
        event.setStatus("active");
        return event;
    }

    private Event proposalToApprovedEvent(EventProposal proposal) {
        Event event = new Event();
        Timestamp startTime = proposal.getDate();
        Timestamp endTime = null;
        if (startTime != null) {
            endTime = new Timestamp(new Date(startTime.toDate().getTime() + 2L * 60L * 60L * 1000L));
        }
        event.setTitle(proposal.getTitle());
        event.setDescription(proposal.getDescription());
        event.setCategory(proposal.getCategory());
        event.setTags(proposal.getTags() != null ? proposal.getTags() : new ArrayList<>());
        event.setDate(startTime);
        event.setEndTime(endTime);
        event.setLocation(proposal.getLocation());
        event.setCapacity(proposal.getCapacity());
        event.setRsvpCount(0L);
        event.setCheckedInCount(0L);
        event.setThumbnailUrl(proposal.getThumbnailUrl() != null ? proposal.getThumbnailUrl() : "");
        event.setTrailerUrl(proposal.getTrailerUrl());
        event.setSponsors(proposal.getSponsors() != null ? proposal.getSponsors() : new ArrayList<>());
        event.setFoodStalls(proposal.getFoodStalls() != null ? proposal.getFoodStalls() : new ArrayList<>());
        event.setOrganizerId(proposal.getOrganizerId());
        event.setOrganizerName(proposal.getOrganizerName());
        event.setVerified(true);
        event.setAverageRating(0.0);
        event.setRatingCount(0L);
        event.setStatus("active");
        event.setCreatedAt(proposal.getSubmittedAt() != null ? proposal.getSubmittedAt() : Timestamp.now());
        event.setTicketPrice(proposal.getTicketPrice());
        return event;
    }

    private Notification buildNotification(String title, String body, String type, String eventId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setEventId(eventId);
        notification.setRead(false);
        notification.setCreatedAt(Timestamp.now());
        return notification;
    }

    private void fetchEventsByIdsPreserveOrder(List<String> orderedIds, Map<String, Event> fallbacks, EventListCallback cb) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            cb.onSuccess(new ArrayList<>());
            return;
        }
        List<List<String>> chunks = chunkList(orderedIds, FIRESTORE_WHERE_IN_LIMIT);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (List<String> chunk : chunks) {
            tasks.add(db.collection(COLLECTION_EVENTS).whereIn(FieldPath.documentId(), chunk).get());
        }
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Map<String, Event> fetchedMap = new HashMap<>();
            for (Object res : results) {
                for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                    Event e = documentToEvent(doc);
                    fetchedMap.put(e.getEventId(), e);
                }
            }
            List<Event> ordered = new ArrayList<>();
            for (String id : orderedIds) {
                if (fetchedMap.containsKey(id)) ordered.add(fetchedMap.get(id));
                else if (fallbacks != null && fallbacks.containsKey(id)) ordered.add(fallbacks.get(id));
            }
            cb.onSuccess(ordered);
        }).addOnFailureListener(cb::onError);
    }

    private List<List<String>> chunkList(List<String> source, int size) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            chunks.add(new ArrayList<>(source.subList(i, Math.min(source.size(), i + size))));
        }
        return chunks;
    }

    private void sortEventsByDateAscending(List<Event> events) {
        Collections.sort(events, Comparator.comparing(
                Event::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
    }

    private void sortProposalsBySubmittedAtDescending(List<EventProposal> proposals) {
        Collections.sort(proposals, Comparator.comparing(
                EventProposal::getSubmittedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
    }

    private void sortProposalsBySubmittedAtAscending(List<EventProposal> proposals) {
        Collections.sort(proposals, Comparator.comparing(
                EventProposal::getSubmittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
    }

    private void runIfNotNull(Runnable r) { if (r != null) r.run(); }
}
