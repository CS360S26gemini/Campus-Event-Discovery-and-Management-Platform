package com.example.CampusEventDiscovery.repository;

import android.text.TextUtils;
import android.util.Log;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventAttendee;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.TicketTier;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.model.VendorProposal;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.EventTimeUtils;
import com.example.CampusEventDiscovery.util.UserRoles;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String COLLECTION_VENDOR_PROPOSALS = "vendorProposals";

    private static final int ATTENDEE_CANCELLATION_WINDOW_DAYS = 3;
    private static final int ORGANIZER_CANCELLATION_WINDOW_DAYS = 5;
    private static final long ATTENDEE_CANCELLATION_WINDOW_MILLIS =
            ATTENDEE_CANCELLATION_WINDOW_DAYS * 24L * 60L * 60L * 1000L;
    private static final long ORGANIZER_CANCELLATION_WINDOW_MILLIS =
            ORGANIZER_CANCELLATION_WINDOW_DAYS * 24L * 60L * 60L * 1000L;

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

    public interface RecommendationCallback {
        void onSuccess(List<Event> events, boolean trendingFallback, String topCategory);
        void onError(Exception e);
    }

    public interface RsvpListCallback {
        void onSuccess(List<Rsvp> rsvps);
        void onError(Exception e);
    }

    public interface VendorProposalListCallback {
        void onSuccess(List<VendorProposal> proposals);
        void onError(Exception e);
    }

    public interface IntegerCallback {
        void onSuccess(int value);
        void onError(Exception e);
    }

    public interface TicketTierListCallback {
        void onSuccess(List<TicketTier> tiers);
        void onError(Exception e);
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
                    events = filterVisibleLiveEvents(events);
                    sortEventsByCreatedAtDescending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "getUpcomingEvents failed", error);
                    cb.onError(error);
                });
    }

    public void getRecentEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    events = filterVisibleLiveEvents(events);
                    sortEventsByCreatedAtDescending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "getRecentEvents failed", error);
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

    public void getEventsByIds(List<String> ids, EventListCallback cb) {
        if (cb == null) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            cb.onSuccess(new ArrayList<>());
            return;
        }
        fetchEventsByIdsPreserveOrder(new ArrayList<>(new LinkedHashSet<>(ids)), null, cb);
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
                    events = filterVisibleLiveEvents(events);
                    sortEventsByCreatedAtDescending(events);
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

    public void getTiersForEvent(String eventId, TicketTierListCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(Constants.SUBCOLLECTION_TICKET_TIERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TicketTier> tiers = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        TicketTier tier = doc.toObject(TicketTier.class);
                        if (tier != null) {
                            tier.setTierId(doc.getId());
                            tiers.add(tier);
                        }
                    }
                    Collections.sort(tiers, Comparator.comparingDouble(TicketTier::getPrice));
                    if (cb != null) cb.onSuccess(tiers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTiersForEvent failed", e);
                    if (cb != null) cb.onError(e);
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
                    filtered = filterVisibleLiveEvents(filtered);
                    sortEventsByCreatedAtDescending(filtered);
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
                    fetchEventsByIdsPreserveOrder(orderedIds, fallbackMap, new EventListCallback() {
                        @Override
                        public void onSuccess(List<Event> events) {
                            events = filterVisibleLiveEvents(events);
                            sortEventsByCreatedAtDescending(events);
                            cb.onSuccess(events);
                        }

                        @Override
                        public void onError(Exception e) {
                            cb.onError(e);
                        }
                    });
                })
                .addOnFailureListener(cb::onError);
    }

    public void rsvpEvent(String userId, Event event, String fullName, ActionCallback cb) {
        rsvpEvent(userId, event, fullName, null, null, null, cb);
    }

    public void rsvpEvent(String userId,
                          Event event,
                          String fullName,
                          String tierId,
                          String tierName,
                          Double tierPrice,
                          ActionCallback cb) {
        if (userId == null || event == null || event.getEventId() == null) {
            if (cb != null) cb.onError(new Exception("Invalid data"));
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(event.getEventId());
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_RSVPS).document(event.getEventId());
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);
        DocumentReference blacklistRef = eventRef.collection(SUBCOLLECTION_BLACKLIST).document(userId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    if (!eventSnap.exists()) throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);
                    if (!"active".equalsIgnoreCase(eventSnap.getString("status"))) {
                        throw new FirebaseFirestoreException("Event is not active", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

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

                    if (rsvpCount >= capacity) {
                        throw new FirebaseFirestoreException("Event full", FirebaseFirestoreException.Code.ABORTED);
                    }

                    boolean hasTicketTiers = eventHasTicketTiers(eventSnap);
                    DocumentReference tierRef = null;
                    String effectiveTierId = null;
                    String effectiveTierName = null;
                    Double effectiveTierPrice = null;

                    if (hasTicketTiers) {
                        if (TextUtils.isEmpty(tierId)) {
                            throw new FirebaseFirestoreException("Ticket tier required", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }

                        tierRef = eventRef.collection(Constants.SUBCOLLECTION_TICKET_TIERS).document(tierId);
                        DocumentSnapshot tierSnap = transaction.get(tierRef);
                        if (!tierSnap.exists()) {
                            throw new FirebaseFirestoreException("Ticket tier not found", FirebaseFirestoreException.Code.NOT_FOUND);
                        }

                        long tierCapacity = getLongValue(tierSnap.getLong("capacity"));
                        long tierRsvpCount = getLongValue(tierSnap.getLong("rsvpCount"));
                        if (tierRsvpCount >= tierCapacity) {
                            throw new FirebaseFirestoreException("Selected tier sold out", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }

                        effectiveTierId = tierRef.getId();
                        effectiveTierName = !TextUtils.isEmpty(tierName) ? tierName : tierSnap.getString("name");
                        effectiveTierPrice = tierPrice != null ? tierPrice : tierSnap.getDouble("price");
                    }

                    String qrToken = UUID.randomUUID().toString();
                    double chargedAmount = hasTicketTiers
                            ? resolveRefundAmount(effectiveTierPrice, null, eventSnap.getDouble("ticketPrice"))
                            : resolveRefundAmount(null, null, eventSnap.getDouble("ticketPrice"));

                    Map<String, Object> rsvpData = buildRsvpData(
                            event,
                            qrToken,
                            chargedAmount,
                            effectiveTierId,
                            effectiveTierName,
                            effectiveTierPrice,
                            Timestamp.now()
                    );
                    transaction.set(userRsvpRef, rsvpData);

                    Map<String, Object> attendeeData = buildAttendeeData(
                            userId,
                            fullName,
                            qrToken,
                            effectiveTierId,
                            effectiveTierName
                    );
                    transaction.set(eventAttendeeRef, attendeeData);

                    if (tierRef != null) {
                        transaction.update(tierRef, "rsvpCount", FieldValue.increment(1));
                    }
                    transaction.update(eventRef, "rsvpCount", FieldValue.increment(1));

                    return null;
                }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void cancelRsvp(String userId, String eventId, ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new Exception("Invalid user or event ID"));
            return;
        }

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS).document(userId).collection(SUBCOLLECTION_RSVPS).document(eventId);
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    DocumentSnapshot userSnap = transaction.get(userRef);
                    DocumentSnapshot userRsvpSnap = transaction.get(userRsvpRef);
                    DocumentSnapshot attendeeSnap = transaction.get(eventAttendeeRef);

                    boolean hadActiveRsvp = attendeeSnap.exists();
                    boolean wasCheckedIn = false;
                    String tierId = null;
                    String tierName = null;
                    DocumentReference tierRef = null;
                    DocumentSnapshot tierSnap = null;
                    boolean shouldRefund = false;
                    double refundAmount = 0.0;
                    Timestamp cancelledAt = Timestamp.now();

                    if (userRsvpSnap.exists()) {
                        String currentStatus = userRsvpSnap.getString("status");
                        hadActiveRsvp = hadActiveRsvp || !"cancelled".equalsIgnoreCase(currentStatus);

                        Boolean rsvpCheckedIn = userRsvpSnap.getBoolean("checkedIn");
                        wasCheckedIn = rsvpCheckedIn != null && rsvpCheckedIn;

                        tierId = userRsvpSnap.getString("tierId");
                        tierName = userRsvpSnap.getString("tierName");
                        if (hadActiveRsvp && !TextUtils.isEmpty(tierId)) {
                            tierRef = eventRef.collection(Constants.SUBCOLLECTION_TICKET_TIERS).document(tierId);
                            tierSnap = transaction.get(tierRef);
                        }

                        Timestamp eventDate = resolveEventDate(eventSnap, userRsvpSnap);
                        if (hadActiveRsvp && !isAttendeeCancellationAllowed(eventDate, cancelledAt)) {
                            throw new FirebaseFirestoreException(
                                    "Tickets can only be cancelled at least 3 days before the event.",
                                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }

                        refundAmount = resolveCancellationAmount(eventSnap, userRsvpSnap);
                        // Refund if the ticket was paid, the user never checked in, and the
                        // cancellation is at least 3 days before the event.
                        shouldRefund = hadActiveRsvp
                                && !wasCheckedIn
                                && refundAmount > 0.0
                                && isRefundEligible(eventDate, cancelledAt, false);

                        Map<String, Object> cancelledRsvpData = new HashMap<>();
                        cancelledRsvpData.put("status", "cancelled");
                        cancelledRsvpData.put("addedToCalendar", false);
                        cancelledRsvpData.put("gcalEventId", "");
                        cancelledRsvpData.put("cancelledAt", cancelledAt);
                        if (shouldRefund) {
                            cancelledRsvpData.put("paymentStatus", Constants.PAYMENT_REFUNDED);
                            cancelledRsvpData.put("refundAmount", refundAmount);
                        }
                        transaction.set(userRsvpRef, cancelledRsvpData, SetOptions.merge());
                    }

                    if (attendeeSnap.exists()) {
                        Boolean attendeeCheckedIn = attendeeSnap.getBoolean("checkedIn");
                        wasCheckedIn = wasCheckedIn || (attendeeCheckedIn != null && attendeeCheckedIn);
                        transaction.delete(eventAttendeeRef);
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

                    if (hadActiveRsvp && tierRef != null && tierSnap != null) {
                        if (tierSnap.exists()) {
                            long currentTierCount = getLongValue(tierSnap.getLong("rsvpCount"));
                            transaction.update(tierRef, "rsvpCount", Math.max(0L, currentTierCount - 1L));
                        }
                    }

                    if (shouldRefund) {
                        DocumentReference refundRef = db.collection(Constants.COLLECTION_CREDIT_TRANSACTIONS).document();
                        double currentCreditBalance = userSnap != null && userSnap.exists()
                                ? normalizeRefundAmount(userSnap.getDouble("creditBalance") != null
                                ? userSnap.getDouble("creditBalance") : 0.0)
                                : 0.0;
                        double updatedCreditBalance = currentCreditBalance + refundAmount;

                        Map<String, Object> creditTransaction = new HashMap<>();
                        creditTransaction.put("userId", userId);
                        creditTransaction.put("type", Constants.CREDIT_TRANSACTION_REFUND);
                        creditTransaction.put("amount", refundAmount);
                        creditTransaction.put("eventId", eventId);
                        creditTransaction.put("eventTitle", userRsvpSnap.getString("title"));
                        creditTransaction.put("tierId", tierId);
                        creditTransaction.put("tierName", tierName);
                        creditTransaction.put("createdAt", cancelledAt);
                        creditTransaction.put("originalPaymentMethod", userRsvpSnap.getString("paymentMethod"));
                        creditTransaction.put("paymentTransactionId", userRsvpSnap.getString("transactionId"));
                        creditTransaction.put("creditBalanceBefore", currentCreditBalance);
                        creditTransaction.put("creditBalanceAfter", updatedCreditBalance);
                        transaction.set(refundRef, creditTransaction);

                        Map<String, Object> userCreditUpdate = new HashMap<>();
                        userCreditUpdate.put("creditBalance", updatedCreditBalance);
                        transaction.set(userRef, userCreditUpdate, SetOptions.merge());
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
                    fetchEventsByIdsPreserveOrder(orderedIds, null, new EventListCallback() {
                        @Override
                        public void onSuccess(List<Event> events) {
                            events = filterVisibleLiveEvents(events);
                            sortEventsByCreatedAtDescending(events);
                            cb.onSuccess(events);
                        }

                        @Override
                        public void onError(Exception e) {
                            cb.onError(e);
                        }
                    });
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
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Invalid memory data"));
            return;
        }

        Map<String, Object> memory = new HashMap<>();
        memory.put("eventId", eventId);
        memory.put("eventTitle", eventTitle);
        if (photoUrls != null && !photoUrls.isEmpty()) {
            memory.put("photoUrls", FieldValue.arrayUnion(photoUrls.toArray()));
        }
        memory.put("attendedAt", Timestamp.now());
        memory.put("updatedAt", Timestamp.now());
        memory.put("rating", rating);

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .document(eventId)
                .set(memory, SetOptions.merge())
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
                    events = filterVisibleLiveEvents(events);
                    sortEventsByCreatedAtDescending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getOrganizerPastEvents(String organizerId, EventListCallback cb) {
        if (TextUtils.isEmpty(organizerId)) {
            cb.onSuccess(new ArrayList<>());
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
                    events = filterPastEvents(events);
                    sortEventsByCreatedAtDescending(events);
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
                    events = filterVisibleLiveEvents(events);
                    sortEventsByCreatedAtDescending(events);
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllPastEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    events = filterPastEvents(events);
                    sortEventsByCreatedAtDescending(events);
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
                    List<Map<String, Object>> normalizedTiers = normalizeProposalTiers(proposal.getTiers());
                    transaction.set(eventRef, proposalToApprovedEvent(proposal, normalizedTiers));
                    transaction.set(eventRef, buildTierMetadata(normalizedTiers), SetOptions.merge());
                    for (Map<String, Object> tierMap : normalizedTiers) {
                        DocumentReference tierRef = eventRef.collection(Constants.SUBCOLLECTION_TICKET_TIERS).document();
                        transaction.set(tierRef, tierMap);
                    }

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

        eventRef.collection(SUBCOLLECTION_ATTENDEES)
                .get()
                .addOnSuccessListener(attendeeSnaps -> db.runTransaction(transaction -> {
            Timestamp cancelledAt = Timestamp.now();
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) {
                throw new FirebaseFirestoreException(
                        "Event not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = eventSnap.getString("status");
            if ("cancelled".equalsIgnoreCase(currentStatus) || "deleted".equalsIgnoreCase(currentStatus)) {
                throw new FirebaseFirestoreException(
                        "Event is already cancelled.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            String organizerId = eventSnap.getString("organizerId");
            boolean actorIsAdmin = false;
            if (!TextUtils.isEmpty(organizerId)
                    && !TextUtils.isEmpty(deletedByUserId)
                    && !TextUtils.equals(organizerId, deletedByUserId)) {
                DocumentSnapshot actorSnap = transaction.get(db.collection(COLLECTION_USERS).document(deletedByUserId));
                String role = actorSnap.exists() ? actorSnap.getString("role") : "";
                actorIsAdmin = UserRoles.isAdmin(role) || isDeveloperBypassAdminUserId(deletedByUserId);
                if (!actorIsAdmin) {
                    throw new FirebaseFirestoreException(
                            "Only the event organizer or an admin can cancel this event.",
                            FirebaseFirestoreException.Code.PERMISSION_DENIED);
                }
            }

            Timestamp eventDate = eventSnap.getTimestamp("date");
            if (!actorIsAdmin && !isOrganizerCancellationAllowed(eventDate, cancelledAt)) {
                throw new FirebaseFirestoreException(
                        "Events can only be cancelled at least 5 days before the event.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            List<OrganizerCancellationRecord> records = new ArrayList<>();
            List<DocumentReference> staleAttendeeRefs = new ArrayList<>();
            Set<String> tierIds = new HashSet<>();

            for (DocumentSnapshot attendeeSnap : attendeeSnaps.getDocuments()) {
                String attendeeUserId = attendeeSnap.getString("userId");
                if (TextUtils.isEmpty(attendeeUserId)) {
                    attendeeUserId = attendeeSnap.getId();
                }
                if (TextUtils.isEmpty(attendeeUserId)) {
                    continue;
                }

                DocumentReference attendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(attendeeSnap.getId());
                DocumentReference userRef = db.collection(COLLECTION_USERS).document(attendeeUserId);
                DocumentReference userRsvpRef = userRef.collection(SUBCOLLECTION_RSVPS).document(eventId);
                DocumentSnapshot userSnap = transaction.get(userRef);
                DocumentSnapshot userRsvpSnap = transaction.get(userRsvpRef);

                if (userRsvpSnap.exists()) {
                    String rsvpStatus = userRsvpSnap.getString("status");
                    if ("cancelled".equalsIgnoreCase(rsvpStatus)) {
                        staleAttendeeRefs.add(attendeeRef);
                        continue;
                    }
                }

                double refundAmount = userRsvpSnap.exists()
                        ? resolveCancellationAmount(eventSnap, userRsvpSnap)
                        : 0.0;
                String tierId = userRsvpSnap.exists() ? userRsvpSnap.getString("tierId") : null;
                if (!TextUtils.isEmpty(tierId)) {
                    tierIds.add(tierId);
                }

                records.add(new OrganizerCancellationRecord(
                        attendeeUserId,
                        attendeeRef,
                        userRef,
                        userRsvpRef,
                        userSnap,
                        userRsvpSnap,
                        refundAmount,
                        tierId,
                        userRsvpSnap.exists() ? userRsvpSnap.getString("tierName") : null
                ));
            }

            Map<String, Object> eventUpdates = new HashMap<>();
            eventUpdates.put("status", "cancelled");
            eventUpdates.put("cancelledAt", cancelledAt);
            eventUpdates.put("cancelledBy", deletedByUserId == null ? "" : deletedByUserId);
            eventUpdates.put("deletedAt", cancelledAt);
            eventUpdates.put("deletedBy", deletedByUserId == null ? "" : deletedByUserId);
            eventUpdates.put("rsvpCount", 0L);
            eventUpdates.put("checkedInCount", 0L);
            transaction.set(eventRef, eventUpdates, SetOptions.merge());

            for (DocumentReference staleAttendeeRef : staleAttendeeRefs) {
                transaction.delete(staleAttendeeRef);
            }

            String eventTitle = eventSnap.getString("title");
            for (OrganizerCancellationRecord record : records) {
                Map<String, Object> cancelledRsvpData = new HashMap<>();
                cancelledRsvpData.put("status", "cancelled");
                cancelledRsvpData.put("addedToCalendar", false);
                cancelledRsvpData.put("gcalEventId", "");
                cancelledRsvpData.put("cancelledAt", cancelledAt);
                cancelledRsvpData.put("cancelledByOrganizer", true);
                cancelledRsvpData.put("qrExpired", true);

                if (record.refundAmount > 0.0) {
                    cancelledRsvpData.put("paymentStatus", Constants.PAYMENT_REFUNDED);
                    cancelledRsvpData.put("refundAmount", record.refundAmount);
                }
                transaction.set(record.userRsvpRef, cancelledRsvpData, SetOptions.merge());
                transaction.delete(record.attendeeRef);

                if (record.refundAmount > 0.0) {
                    double currentCreditBalance = record.userSnap != null && record.userSnap.exists()
                            ? normalizeRefundAmount(record.userSnap.getDouble("creditBalance") != null
                            ? record.userSnap.getDouble("creditBalance") : 0.0)
                            : 0.0;
                    double updatedCreditBalance = currentCreditBalance + record.refundAmount;

                    DocumentReference refundRef = db.collection(Constants.COLLECTION_CREDIT_TRANSACTIONS).document();
                    Map<String, Object> creditTransaction = new HashMap<>();
                    creditTransaction.put("userId", record.userId);
                    creditTransaction.put("type", Constants.CREDIT_TRANSACTION_REFUND);
                    creditTransaction.put("amount", record.refundAmount);
                    creditTransaction.put("eventId", eventId);
                    creditTransaction.put("eventTitle", TextUtils.isEmpty(eventTitle) ? record.userRsvpSnap.getString("title") : eventTitle);
                    creditTransaction.put("tierId", record.tierId);
                    creditTransaction.put("tierName", record.tierName);
                    creditTransaction.put("createdAt", cancelledAt);
                    creditTransaction.put("reason", "ORGANIZER_EVENT_CANCELLED");
                    creditTransaction.put("originalPaymentMethod", record.userRsvpSnap.getString("paymentMethod"));
                    creditTransaction.put("paymentTransactionId", record.userRsvpSnap.getString("transactionId"));
                    creditTransaction.put("creditBalanceBefore", currentCreditBalance);
                    creditTransaction.put("creditBalanceAfter", updatedCreditBalance);
                    transaction.set(refundRef, creditTransaction);

                    Map<String, Object> userCreditUpdate = new HashMap<>();
                    userCreditUpdate.put("creditBalance", updatedCreditBalance);
                    transaction.set(record.userRef, userCreditUpdate, SetOptions.merge());
                }

                DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS)
                        .document(record.userId)
                        .collection(SUBCOLLECTION_MESSAGES)
                        .document();
                transaction.set(notificationRef, buildNotification(
                        "Event cancelled",
                        (TextUtils.isEmpty(eventTitle) ? "This event" : eventTitle)
                                + " was cancelled by the organizer. Paid tickets were refunded as in-app credit.",
                        "event_cancelled",
                        eventId
                ));
            }

            for (String tierId : tierIds) {
                transaction.set(
                        eventRef.collection(Constants.SUBCOLLECTION_TICKET_TIERS).document(tierId),
                        Collections.singletonMap("rsvpCount", 0L),
                        SetOptions.merge());
            }

            return null;
        }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
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

    public void isUserBlacklisted(String eventId, String userId, BooleanCallback cb) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(userId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Event ID and user ID are required"));
            return;
        }

        DocumentReference eventBlacklistRef = db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(SUBCOLLECTION_BLACKLIST)
                .document(userId);
        DocumentReference platformBlacklistRef = db.collection(Constants.COLLECTION_PLATFORM_BLACKLIST)
                .document(userId);

        eventBlacklistRef.get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc.exists()) {
                        if (cb != null) cb.onSuccess(true);
                        return;
                    }

                    platformBlacklistRef.get()
                            .addOnSuccessListener(platformDoc -> {
                                if (cb != null) cb.onSuccess(platformDoc.exists());
                            })
                            .addOnFailureListener(e -> {
                                if (cb != null) cb.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void blacklistUserByEmail(String eventId,
                                     String email,
                                     String organizerId,
                                     String reason,
                                     ActionCallback cb) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(email)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Event ID and email are required"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (cb != null) cb.onError(new Exception("User not found"));
                        return;
                    }

                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String userId = userDoc.getId();
                    Map<String, Object> blacklistData = new HashMap<>();
                    blacklistData.put("userId", userId);
                    blacklistData.put("email", email.trim());
                    blacklistData.put("organizerId", organizerId == null ? "" : organizerId);
                    blacklistData.put("reason", reason == null ? "" : reason);
                    blacklistData.put("blacklistedAt", Timestamp.now());

                    db.collection(COLLECTION_EVENTS)
                            .document(eventId)
                            .collection(SUBCOLLECTION_BLACKLIST)
                            .document(userId)
                            .set(blacklistData)
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
                              double lat, double lng,
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

        db.collection(Constants.COLLECTION_SOS_ALERTS)
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
        return proposalToApprovedEvent(proposal, normalizeProposalTiers(proposal.getTiers()));
    }

    private Event proposalToApprovedEvent(EventProposal proposal, List<Map<String, Object>> normalizedTiers) {
        Event event = new Event();
        Timestamp startTime = proposal.getDate();
        Timestamp endTime = EventTimeUtils.resolveEndTime(startTime, proposal.getEndTime());
        event.setTitle(proposal.getTitle());
        event.setDescription(proposal.getDescription());
        event.setCategory(proposal.getCategory());
        event.setTags(proposal.getTags() != null ? proposal.getTags() : new ArrayList<>());
        event.setDate(startTime);
        event.setEndTime(endTime);
        event.setLocation(proposal.getLocation());
        event.setLocationKey(proposal.getLocationKey());
        event.setLocationDescription(proposal.getLocationDescription());
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
        event.setTicketPrice(resolveEventSummaryTicketPrice(proposal, normalizedTiers));
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

    private void sortEventsByCreatedAtDescending(List<Event> events) {
        Collections.sort(events, Comparator.comparing(
                Event::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
    }

    private List<Event> filterVisibleLiveEvents(List<Event> source) {
        List<Event> filtered = new ArrayList<>();
        if (source == null) {
            return filtered;
        }
        for (Event event : source) {
            if (event == null) {
                continue;
            }
            if (isPastEvent(event)) {
                continue;
            }
            String status = event.getStatus();
            if ("cancelled".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) {
                continue;
            }
            filtered.add(event);
        }
        return filtered;
    }

    private List<Event> filterPastEvents(List<Event> source) {
        List<Event> filtered = new ArrayList<>();
        if (source == null) {
            return filtered;
        }
        for (Event event : source) {
            if (event != null && isPastEvent(event)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private boolean isPastEvent(Event event) {
        if (event == null || event.getDate() == null) {
            return false;
        }
        Timestamp resolvedEndTime = EventTimeUtils.resolveEndTime(event.getDate(), event.getEndTime());
        Timestamp cutoff = resolvedEndTime != null ? resolvedEndTime : event.getDate();
        return cutoff != null && cutoff.toDate().before(new Date());
    }

    private boolean isDeveloperBypassAdminUserId(String userId) {
        return "demo-admin-user".equals(userId);
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

    // ─── PERSONALISED RECOMMENDATIONS (Hussain) ──────────────────────────────

    public void getScoredRecommendations(
            List<String> interests,
            List<String> recentlyViewedIds,
            RecommendationCallback cb
    ) {
        long nowMillis = System.currentTimeMillis();
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(activeSnapshots -> {
                    List<Event> activeUpcoming = new ArrayList<>();
                    for (DocumentSnapshot doc : activeSnapshots.getDocuments()) {
                        Event event = documentToEvent(doc);
                        if (isUpcoming(event, nowMillis)) {
                            activeUpcoming.add(event);
                        }
                    }

                    List<String> recentIds = sanitizeRecentIds(recentlyViewedIds);
                    if (recentIds.isEmpty()) {
                        completeRecommendations(activeUpcoming, interests, new HashSet<>(), nowMillis, cb);
                        return;
                    }

                    db.collection(COLLECTION_EVENTS)
                            .whereIn(FieldPath.documentId(), recentIds)
                            .get()
                            .addOnSuccessListener(recentSnapshots -> {
                                Set<String> recentCategories = new HashSet<>();
                                for (DocumentSnapshot doc : recentSnapshots.getDocuments()) {
                                    String category = normalizeCategory(documentToEvent(doc).getCategory());
                                    if (!TextUtils.isEmpty(category)) {
                                        recentCategories.add(category);
                                    }
                                }
                                completeRecommendations(activeUpcoming, interests, recentCategories, nowMillis, cb);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getScoredRecommendations recent events failed", e);
                                cb.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getScoredRecommendations active events failed", e);
                    cb.onError(e);
                });
    }

    private void completeRecommendations(List<Event> activeUpcoming,
                                         List<String> interests,
                                         Set<String> recentCategories,
                                         long nowMillis,
                                         RecommendationCallback cb) {
        RankedRecommendations ranked = rankRecommendations(activeUpcoming, interests, recentCategories, nowMillis);
        cb.onSuccess(
                ranked.events,
                ranked.trendingFallback,
                resolveTopCategory(interests, ranked.events)
        );
    }

    private static List<String> sanitizeRecentIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null) continue;
            String trimmed = id.trim();
            if (!TextUtils.isEmpty(trimmed)) {
                deduped.add(trimmed);
            }
            if (deduped.size() == 5) break;
        }
        return new ArrayList<>(deduped);
    }

    private static RankedRecommendations rankRecommendations(List<Event> events,
                                                             List<String> interests,
                                                             Set<String> recentCategories,
                                                             long nowMillis) {
        List<Event> upcoming = new ArrayList<>();
        if (events != null) {
            for (Event event : events) {
                if (isUpcoming(event, nowMillis)) upcoming.add(event);
            }
        }

        Map<Event, Integer> scoreByEvent = new HashMap<>();
        boolean hasPositiveScore = false;
        for (Event event : upcoming) {
            int score = scoreEvent(event, interests, recentCategories, nowMillis);
            scoreByEvent.put(event, score);
            if (score > 0) hasPositiveScore = true;
        }

        if (hasPositiveScore) {
            upcoming.sort((first, second) -> compareScoredEvents(first, second, scoreByEvent));
            return new RankedRecommendations(limitToFive(upcoming), false);
        }

        upcoming.sort(EventRepository::compareTrendingEvents);
        return new RankedRecommendations(limitToFive(upcoming), true);
    }

    static List<Event> rankRecommendationsForTesting(List<Event> events,
                                                     List<String> interests,
                                                     Set<String> recentCategories,
                                                     long nowMillis) {
        return rankRecommendations(events, interests, recentCategories, nowMillis).events;
    }

    static boolean usesTrendingFallbackForTesting(List<Event> events,
                                                  List<String> interests,
                                                  Set<String> recentCategories,
                                                  long nowMillis) {
        return rankRecommendations(events, interests, recentCategories, nowMillis).trendingFallback;
    }

    static int scoreEvent(Event event, List<String> interests, Set<String> recentCategories, long nowMillis) {
        if (event == null) return 0;

        int score = 0;
        String eventCategory = normalizeCategory(event.getCategory());
        if (!TextUtils.isEmpty(eventCategory) && normalizedInterestSet(interests).contains(eventCategory)) {
            score += categoryWeight(eventCategory);
        }

        Set<String> normalizedRecentCategories = normalizedCategorySet(recentCategories);
        if (!TextUtils.isEmpty(eventCategory) && normalizedRecentCategories.contains(eventCategory)) {
            score += 6;
        }

        score += Math.min(5, (int) (Math.max(0L, event.getRsvpCount()) / 10L));
        score += dateProximityScore(event, nowMillis);
        return score;
    }

    static String normalizeCategory(String raw) {
        if (TextUtils.isEmpty(raw)) return "";
        String value = raw.trim();
        if (value.equalsIgnoreCase("Education")) return "Academic";
        if (value.equalsIgnoreCase("Music")) return "Music";
        if (value.equalsIgnoreCase("Sports")) return "Sports";
        if (value.equalsIgnoreCase("Career")) return "Career";
        if (value.equalsIgnoreCase("Academic")) return "Academic";
        if (value.equalsIgnoreCase("Arts")) return "Arts";
        if (value.equalsIgnoreCase("Business")) return "Business";
        if (value.equalsIgnoreCase("Food & Bev") || value.equalsIgnoreCase("Food &amp; Bev")) return "Food & Bev";
        if (value.equalsIgnoreCase("Social")) return "Social";
        return value;
    }

    static int categoryWeight(String category) {
        String normalized = normalizeCategory(category);
        switch (normalized) {
            case "Sports": return 10;
            case "Music": return 9;
            case "Career": case "Business": return 8;
            case "Arts": case "Social": return 7;
            case "Food & Bev": return 6;
            case "Academic": return 5;
            default: return 0;
        }
    }

    static boolean isUpcoming(Event event, long nowMillis) {
        if (event == null || event.getDate() == null) return false;
        return event.getDate().toDate().getTime() >= nowMillis;
    }

    static int dateProximityScore(Event event, long nowMillis) {
        if (event == null || event.getDate() == null) return 0;
        long diffMillis = event.getDate().toDate().getTime() - nowMillis;
        if (diffMillis < 0L) return 0;
        long sevenDays = 7L * 24L * 60L * 60L * 1000L;
        long thirtyDays = 30L * 24L * 60L * 60L * 1000L;
        if (diffMillis <= sevenDays) return 4;
        if (diffMillis <= thirtyDays) return 2;
        return 0;
    }

    private static Set<String> normalizedInterestSet(List<String> interests) {
        Set<String> normalized = new HashSet<>();
        if (interests == null) return normalized;
        for (String interest : interests) {
            String category = normalizeCategory(interest);
            if (!TextUtils.isEmpty(category)) normalized.add(category);
        }
        return normalized;
    }

    private static Set<String> normalizedCategorySet(Set<String> categories) {
        Set<String> normalized = new HashSet<>();
        if (categories == null) return normalized;
        for (String category : categories) {
            String value = normalizeCategory(category);
            if (!TextUtils.isEmpty(value)) normalized.add(value);
        }
        return normalized;
    }

    private static int compareScoredEvents(Event first, Event second, Map<Event, Integer> scoreByEvent) {
        int byScore = Integer.compare(scoreByEvent.getOrDefault(second, 0), scoreByEvent.getOrDefault(first, 0));
        if (byScore != 0) return byScore;
        return compareRecommendationTieBreakers(first, second);
    }

    private static int compareRecommendationTieBreakers(Event first, Event second) {
        int byDate = Comparator.comparing(
                Event::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).compare(first, second);
        if (byDate != 0) return byDate;
        int byRsvp = Long.compare(second == null ? 0L : second.getRsvpCount(), first == null ? 0L : first.getRsvpCount());
        if (byRsvp != 0) return byRsvp;
        String firstTitle = first == null || first.getTitle() == null ? "" : first.getTitle();
        String secondTitle = second == null || second.getTitle() == null ? "" : second.getTitle();
        return firstTitle.compareToIgnoreCase(secondTitle);
    }

    private static int compareTrendingEvents(Event first, Event second) {
        int byRsvp = Long.compare(second == null ? 0L : second.getRsvpCount(), first == null ? 0L : first.getRsvpCount());
        if (byRsvp != 0) return byRsvp;
        return compareRecommendationTieBreakers(first, second);
    }

    private static List<Event> limitToFive(List<Event> events) {
        if (events == null || events.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(events.subList(0, Math.min(5, events.size())));
    }

    private static String resolveTopCategory(List<String> interests, List<Event> returnedEvents) {
        String strongestInterest = "";
        int strongestWeight = 0;
        if (interests != null) {
            for (String interest : interests) {
                String category = normalizeCategory(interest);
                int weight = categoryWeight(category);
                if (weight > strongestWeight) {
                    strongestWeight = weight;
                    strongestInterest = category;
                }
            }
        }
        if (!TextUtils.isEmpty(strongestInterest)) return strongestInterest;
        if (returnedEvents != null && !returnedEvents.isEmpty()) {
            return normalizeCategory(returnedEvents.get(0).getCategory());
        }
        return "";
    }

    private static class RankedRecommendations {
        final List<Event> events;
        final boolean trendingFallback;

        RankedRecommendations(List<Event> events, boolean trendingFallback) {
            this.events = events;
            this.trendingFallback = trendingFallback;
        }
    }

    // ─── REFUND HELPERS (Yahya) ───────────────────────────────────────────────

    public static boolean isRefundEligible(Timestamp eventDate, Timestamp cancellationTime, boolean organizerInitiated) {
        return organizerInitiated
                ? isOrganizerCancellationAllowed(eventDate, cancellationTime)
                : isAttendeeCancellationAllowed(eventDate, cancellationTime);
    }

    public static boolean isAttendeeCancellationAllowed(Timestamp eventDate, Timestamp cancellationTime) {
        if (eventDate == null || cancellationTime == null) {
            return false;
        }
        long remaining = eventDate.toDate().getTime() - cancellationTime.toDate().getTime();
        return remaining >= ATTENDEE_CANCELLATION_WINDOW_MILLIS;
    }

    public static boolean isOrganizerCancellationAllowed(Timestamp eventDate, Timestamp cancellationTime) {
        if (eventDate == null || cancellationTime == null) {
            return false;
        }
        long remaining = eventDate.toDate().getTime() - cancellationTime.toDate().getTime();
        return remaining >= ORGANIZER_CANCELLATION_WINDOW_MILLIS;
    }

    /**
     * Returns true when the ticket was purchased (rsvpAt) within the 3-day refund window.
     * If rsvpAt is missing (legacy records) we allow the refund so old tickets are not
     * silently broken.
     */
    public static boolean isPurchasedWithin3Days(Timestamp purchasedAt, Timestamp now) {
        if (purchasedAt == null || now == null) {
            // Missing timestamp — allow refund rather than silently blocking valid requests
            return true;
        }
        long elapsed = now.toDate().getTime() - purchasedAt.toDate().getTime();
        return elapsed >= 0 && elapsed <= ATTENDEE_CANCELLATION_WINDOW_MILLIS;
    }

    public static double normalizeRefundAmount(double amount) {
        return Math.max(0.0, amount);
    }

    public static double resolveRefundAmount(Double tierPrice, Double amount, Double ticketPrice) {
        Double storedAmount = tierPrice;
        if (storedAmount == null) {
            storedAmount = amount;
        }
        if (storedAmount == null) {
            storedAmount = ticketPrice;
        }
        return normalizeRefundAmount(storedAmount != null ? storedAmount : 0.0);
    }

    private double resolveCancellationAmount(DocumentSnapshot eventSnap, DocumentSnapshot userRsvpSnap) {
        Double tierPrice = userRsvpSnap != null ? userRsvpSnap.getDouble("tierPrice") : null;
        Double amount = userRsvpSnap != null ? userRsvpSnap.getDouble("amount") : null;
        Double ticketPrice = userRsvpSnap != null ? userRsvpSnap.getDouble("ticketPrice") : null;
        if (ticketPrice == null && eventSnap != null) {
            ticketPrice = eventSnap.getDouble("ticketPrice");
        }
        return resolveRefundAmount(tierPrice, amount, ticketPrice);
    }

    private Timestamp resolveEventDate(DocumentSnapshot eventSnap, DocumentSnapshot userRsvpSnap) {
        Timestamp eventDate = eventSnap != null && eventSnap.exists() ? eventSnap.getTimestamp("date") : null;
        if (eventDate == null && userRsvpSnap != null && userRsvpSnap.exists()) {
            eventDate = userRsvpSnap.getTimestamp("date");
        }
        return eventDate;
    }

    private static class OrganizerCancellationRecord {
        final String userId;
        final DocumentReference attendeeRef;
        final DocumentReference userRef;
        final DocumentReference userRsvpRef;
        final DocumentSnapshot userSnap;
        final DocumentSnapshot userRsvpSnap;
        final double refundAmount;
        final String tierId;
        final String tierName;

        OrganizerCancellationRecord(String userId,
                                    DocumentReference attendeeRef,
                                    DocumentReference userRef,
                                    DocumentReference userRsvpRef,
                                    DocumentSnapshot userSnap,
                                    DocumentSnapshot userRsvpSnap,
                                    double refundAmount,
                                    String tierId,
                                    String tierName) {
            this.userId = userId;
            this.attendeeRef = attendeeRef;
            this.userRef = userRef;
            this.userRsvpRef = userRsvpRef;
            this.userSnap = userSnap;
            this.userRsvpSnap = userRsvpSnap;
            this.refundAmount = refundAmount;
            this.tierId = tierId;
            this.tierName = tierName;
        }
    }

    // ─── RSVPS FOR MEMORIES (ui-update) ──────────────────────────────────────

    public void getAttendedRsvpsForMemories(String userId, RsvpListCallback cb) {
        if (TextUtils.isEmpty(userId)) {
            cb.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_RSVPS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Rsvp> rsvps = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Rsvp rsvp = doc.toObject(Rsvp.class);
                        if (rsvp == null) {
                            continue;
                        }
                        rsvp.setRsvpId(doc.getId());
                        String status = rsvp.getStatus();
                        if ("cancelled".equalsIgnoreCase(status)) {
                            continue;
                        }

                        if (rsvp.isCheckedIn()) {
                            rsvps.add(rsvp);
                        }
                    }

                    Collections.sort(rsvps, (a, b) -> {
                        Date aDate = a.getDate() != null ? a.getDate().toDate() : new Date(0L);
                        Date bDate = b.getDate() != null ? b.getDate().toDate() : new Date(0L);
                        return bDate.compareTo(aDate);
                    });
                    cb.onSuccess(rsvps);
                })
                .addOnFailureListener(cb::onError);
    }

    // ─── MEMORY PHOTOS (ui-update) ───────────────────────────────────────────

    public void getRegisteredRsvpsForMemories(String userId, RsvpListCallback cb) {
        if (TextUtils.isEmpty(userId)) {
            cb.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_RSVPS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Rsvp> rsvps = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Rsvp rsvp = doc.toObject(Rsvp.class);
                        if (rsvp == null) {
                            continue;
                        }

                        rsvp.setRsvpId(doc.getId());
                        String eventId = TextUtils.isEmpty(rsvp.getEventId())
                                ? doc.getId()
                                : rsvp.getEventId();
                        rsvp.setEventId(eventId);

                        String status = rsvp.getStatus();
                        if (TextUtils.isEmpty(eventId) || "cancelled".equalsIgnoreCase(status)) {
                            continue;
                        }

                        rsvps.add(rsvp);
                    }

                    Collections.sort(rsvps, (a, b) -> {
                        Date aDate = a.getDate() != null ? a.getDate().toDate() : new Date(0L);
                        Date bDate = b.getDate() != null ? b.getDate().toDate() : new Date(0L);
                        return bDate.compareTo(aDate);
                    });
                    cb.onSuccess(rsvps);
                })
                .addOnFailureListener(cb::onError);
    }

    public void createMemoryAlbum(String userId,
                                  String eventId,
                                  String eventTitle,
                                  Timestamp attendedAt,
                                  ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Invalid memory album data"));
            return;
        }

        Map<String, Object> memory = new HashMap<>();
        memory.put("eventId", eventId);
        memory.put("eventTitle", eventTitle);
        memory.put("attendedAt", attendedAt != null ? attendedAt : Timestamp.now());
        memory.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .document(eventId)
                .set(memory, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (cb != null) cb.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    public void addMemoryPhotos(String userId,
                                String eventId,
                                String eventTitle,
                                Timestamp attendedAt,
                                List<String> photoUrls,
                                ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)
                || photoUrls == null || photoUrls.isEmpty()) {
            if (cb != null) cb.onError(new IllegalArgumentException("Invalid memory photo data"));
            return;
        }

        Map<String, Object> memory = new HashMap<>();
        memory.put("eventId", eventId);
        memory.put("eventTitle", eventTitle);
        if (attendedAt != null) {
            memory.put("attendedAt", attendedAt);
        }
        memory.put("updatedAt", Timestamp.now());
        memory.put("photoUrls", FieldValue.arrayUnion(photoUrls.toArray()));

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .document(eventId)
                .set(memory, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (cb != null) cb.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onError(e);
                });
    }

    // ─── IN-APP CREDIT RSVP (Yahya) ──────────────────────────────────────────

    public void removeMemoryPhoto(String userId,
                                  String eventId,
                                  String photoUrl,
                                  ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId) || TextUtils.isEmpty(photoUrl)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Invalid memory photo removal data"));
            return;
        }

        Map<String, Object> memory = new HashMap<>();
        memory.put("photoUrls", FieldValue.arrayRemove(photoUrl));
        memory.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        db.collection(COLLECTION_USERS).document(userId)
                                .collection(SUBCOLLECTION_MEMORIES)
                                .document(eventId)
                                .set(memory, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    if (cb != null) cb.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (cb != null) cb.onError(e);
                                });
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.set(doc.getReference(), memory, SetOptions.merge());
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

    public void deleteMemory(String userId,
                             String eventId,
                             ActionCallback cb) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(eventId)) {
            if (cb != null) cb.onError(new IllegalArgumentException("Invalid memory delete data"));
            return;
        }

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    DocumentReference canonicalRef = db.collection(COLLECTION_USERS).document(userId)
                            .collection(SUBCOLLECTION_MEMORIES)
                            .document(eventId);
                    batch.delete(canonicalRef);
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            if (!doc.getReference().getPath().equals(canonicalRef.getPath())) {
                                batch.delete(doc.getReference());
                            }
                        }
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

    public void rsvpEventWithCredit(String userId, Event event, String fullName, double amount, ActionCallback cb) {
        rsvpEventWithCredit(userId, event, fullName, amount, null, null, null, cb);
    }

    public void rsvpEventWithCredit(String userId,
                                    Event event,
                                    String fullName,
                                    double amount,
                                    String tierId,
                                    String tierName,
                                    Double tierPrice,
                                    ActionCallback cb) {
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
                    if (!"active".equalsIgnoreCase(eventSnap.getString("status"))) {
                        throw new FirebaseFirestoreException("Event is not active", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
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

                    boolean hasTicketTiers = eventHasTicketTiers(eventSnap);
                    DocumentReference tierRef = null;
                    String effectiveTierId = null;
                    String effectiveTierName = null;
                    Double effectiveTierPrice = null;

                    if (hasTicketTiers) {
                        if (TextUtils.isEmpty(tierId)) {
                            throw new FirebaseFirestoreException("Ticket tier required", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }

                        tierRef = eventRef.collection(Constants.SUBCOLLECTION_TICKET_TIERS).document(tierId);
                        DocumentSnapshot tierSnap = transaction.get(tierRef);
                        if (!tierSnap.exists()) {
                            throw new FirebaseFirestoreException("Ticket tier not found", FirebaseFirestoreException.Code.NOT_FOUND);
                        }

                        long tierCapacity = getLongValue(tierSnap.getLong("capacity"));
                        long tierRsvpCount = getLongValue(tierSnap.getLong("rsvpCount"));
                        if (tierRsvpCount >= tierCapacity) {
                            throw new FirebaseFirestoreException("Selected tier sold out", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }

                        effectiveTierId = tierRef.getId();
                        effectiveTierName = !TextUtils.isEmpty(tierName) ? tierName : tierSnap.getString("name");
                        effectiveTierPrice = tierPrice != null ? tierPrice : tierSnap.getDouble("price");
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
                    paymentData.put("tierId", effectiveTierId);
                    paymentData.put("tierName", effectiveTierName);
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
                    creditTransaction.put("tierId", effectiveTierId);
                    creditTransaction.put("tierName", effectiveTierName);
                    transaction.set(creditTransactionRef, creditTransaction);

                    Map<String, Object> rsvpData = buildRsvpData(
                            event,
                            qrToken,
                            safeAmount,
                            effectiveTierId,
                            effectiveTierName,
                            effectiveTierPrice,
                            now
                    );
                    rsvpData.put("paymentStatus", Constants.PAYMENT_CONFIRMED);
                    rsvpData.put("transactionId", transactionId);
                    rsvpData.put("paymentRef", transactionId);
                    rsvpData.put("paymentMethod", Constants.PAYMENT_METHOD_IN_APP_CREDIT);
                    rsvpData.put("paymentProofUrl", "");
                    transaction.set(userRsvpRef, rsvpData);

                    Map<String, Object> attendeeData = buildAttendeeData(
                            userId,
                            fullName,
                            qrToken,
                            effectiveTierId,
                            effectiveTierName
                    );
                    transaction.set(eventAttendeeRef, attendeeData);

                    transaction.set(userRef, Collections.singletonMap("creditBalance", FieldValue.increment(-safeAmount)), SetOptions.merge());
                    if (tierRef != null) {
                        transaction.update(tierRef, "rsvpCount", FieldValue.increment(1));
                    }
                    transaction.update(eventRef, "rsvpCount", FieldValue.increment(1));

                    return null;
                }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    private boolean eventHasTicketTiers(DocumentSnapshot eventSnap) {
        return eventSnap != null && Boolean.TRUE.equals(eventSnap.getBoolean("hasTicketTiers"));
    }

    private long getLongValue(Long value) {
        return value != null ? value : 0L;
    }

    private Map<String, Object> buildRsvpData(Event event,
                                              String qrToken,
                                              double chargedAmount,
                                              String tierId,
                                              String tierName,
                                              Double tierPrice,
                                              Timestamp now) {
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
        rsvpData.put("rsvpAt", now);
        rsvpData.put("amount", chargedAmount);
        rsvpData.put("ticketPrice", chargedAmount);
        rsvpData.put("tierId", tierId);
        rsvpData.put("tierName", tierName);
        rsvpData.put("tierPrice", tierPrice);
        return rsvpData;
    }

    private Map<String, Object> buildAttendeeData(String userId,
                                                  String fullName,
                                                  String qrToken,
                                                  String tierId,
                                                  String tierName) {
        Map<String, Object> attendeeData = new HashMap<>();
        attendeeData.put("userId", userId);
        attendeeData.put("fullName", TextUtils.isEmpty(fullName) ? "Attendee" : fullName);
        attendeeData.put("qrToken", qrToken);
        attendeeData.put("checkedIn", false);
        attendeeData.put("checkedInAt", null);
        attendeeData.put("tierId", tierId);
        attendeeData.put("tierName", tierName);
        return attendeeData;
    }

    private Map<String, Object> buildTierMetadata(EventProposal proposal) {
        return buildTierMetadata(normalizeProposalTiers(proposal.getTiers()));
    }

    private Map<String, Object> buildTierMetadata(List<Map<String, Object>> tiers) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hasTicketTiers", !tiers.isEmpty());
        metadata.put("ticketTierCount", tiers.size());
        return metadata;
    }

    private double resolveEventSummaryTicketPrice(EventProposal proposal, List<Map<String, Object>> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return proposal.getTicketPrice();
        }

        double lowestPrice = Double.MAX_VALUE;
        for (Map<String, Object> tier : tiers) {
            if (tier == null) {
                continue;
            }
            lowestPrice = Math.min(lowestPrice, getDoubleValue(tier.get("price")));
        }

        return lowestPrice == Double.MAX_VALUE ? proposal.getTicketPrice() : lowestPrice;
    }

    private List<Map<String, Object>> normalizeProposalTiers(List<Map<String, Object>> tiers) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        if (tiers == null) {
            return normalized;
        }

        for (Map<String, Object> tier : tiers) {
            if (tier == null) {
                continue;
            }

            String name = tier.get("name") instanceof String ? ((String) tier.get("name")).trim() : "";
            if (TextUtils.isEmpty(name)) {
                continue;
            }

            Map<String, Object> tierData = new HashMap<>();
            tierData.put("name", name);
            tierData.put("price", getDoubleValue(tier.get("price")));
            tierData.put("capacity", getLongObjectValue(tier.get("capacity")));
            tierData.put("rsvpCount", 0L);
            tierData.put("description", tier.get("description") instanceof String ? ((String) tier.get("description")).trim() : "");
            normalized.add(tierData);
        }

        return normalized;
    }

    private long getLongObjectValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // ─── VENDOR PROPOSALS (ui-update) ────────────────────────────────────────

    public void proposeVendor(VendorProposal proposal, ActionCallback cb) {
        if (proposal == null
                || TextUtils.isEmpty(proposal.getVendorName())
                || TextUtils.isEmpty(proposal.getEventId())
                || TextUtils.isEmpty(proposal.getOrganizerId())) {
            if (cb != null) cb.onError(new IllegalArgumentException("Vendor, event, and organizer are required"));
            return;
        }

        proposal.setStatus("pending");
        proposal.setReadByAdmin(false);
        proposal.setAdminNote("");
        proposal.setCreatedAt(Timestamp.now());
        proposal.setReviewedAt(null);

        db.collection(COLLECTION_VENDOR_PROPOSALS)
                .add(proposal)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getVendorProposalsForEvent(String eventId, VendorProposalListCallback cb) {
        if (TextUtils.isEmpty(eventId)) {
            cb.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_VENDOR_PROPOSALS)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<VendorProposal> proposals = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        VendorProposal vp = doc.toObject(VendorProposal.class);
                        if (vp != null) {
                            vp.setProposalId(doc.getId());
                            proposals.add(vp);
                        }
                    }
                    sortVendorProposalsByCreatedAtDescending(proposals);
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllVendorProposals(VendorProposalListCallback cb) {
        db.collection(COLLECTION_VENDOR_PROPOSALS)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<VendorProposal> proposals = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        VendorProposal vp = doc.toObject(VendorProposal.class);
                        if (vp != null) {
                            vp.setProposalId(doc.getId());
                            proposals.add(vp);
                        }
                    }
                    sortVendorProposalsByCreatedAtDescending(proposals);
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public ListenerRegistration observeUnreadPendingVendorProposalCount(IntegerCallback cb) {
        return db.collection(COLLECTION_VENDOR_PROPOSALS)
                .whereEqualTo("status", "pending")
                .whereEqualTo("readByAdmin", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "observeUnreadPendingVendorProposalCount failed", error);
                        cb.onError(error);
                        return;
                    }
                    cb.onSuccess(snapshots == null ? 0 : snapshots.size());
                });
    }

    public void markPendingVendorProposalsRead(ActionCallback cb) {
        db.collection(COLLECTION_VENDOR_PROPOSALS)
                .whereEqualTo("status", "pending")
                .whereEqualTo("readByAdmin", false)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        if (cb != null) cb.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        batch.update(doc.getReference(), "readByAdmin", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                            .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
                })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void approveVendorProposal(VendorProposal proposal, ActionCallback cb) {
        reviewVendorProposal(proposal, "approved", "", cb);
    }

    public void rejectVendorProposal(VendorProposal proposal, String note, ActionCallback cb) {
        reviewVendorProposal(proposal, "rejected", note, cb);
    }

    private void reviewVendorProposal(VendorProposal proposal, String status, String note, ActionCallback cb) {
        if (proposal == null || TextUtils.isEmpty(proposal.getProposalId())) {
            if (cb != null) cb.onError(new IllegalArgumentException("Vendor proposal is required"));
            return;
        }

        DocumentReference proposalRef = db.collection(COLLECTION_VENDOR_PROPOSALS).document(proposal.getProposalId());
        WriteBatch batch = db.batch();
        batch.update(proposalRef,
                "status", status,
                "adminNote", note == null ? "" : note,
                "readByAdmin", true,
                "reviewedAt", Timestamp.now());

        if (!TextUtils.isEmpty(proposal.getOrganizerId())) {
            DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS)
                    .document(proposal.getOrganizerId())
                    .collection(SUBCOLLECTION_MESSAGES)
                    .document();
            String title = "approved".equals(status) ? "Vendor approved" : "Vendor rejected";
            String body = proposal.getVendorName() + " for " + proposal.getEventTitle()
                    + ("approved".equals(status) ? " was approved." : " was rejected.");
            batch.set(notificationRef, buildNotification(title, body, "vendor_" + status, proposal.getEventId()));
        }

        batch.commit()
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    private void sortVendorProposalsByCreatedAtDescending(List<VendorProposal> proposals) {
        Collections.sort(proposals, Comparator.comparing(
                VendorProposal::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
    }
}
