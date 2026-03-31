package com.example.campuseventdiscovery.repository;

import android.util.Log;

import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.model.Notification;
import com.example.campuseventdiscovery.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final String SUBCOLLECTION_SAVED_EVENTS = "saved_events";
    private static final String SUBCOLLECTION_RSVPS = "rsvps";
    private static final String SUBCOLLECTION_MEMORIES = "memories";
    private static final String SUBCOLLECTION_RATINGS = "ratings";
    private static final String SUBCOLLECTION_ATTENDEES = "attendees";
    private static final String SUBCOLLECTION_MESSAGES = "messages";

    private static final int FIRESTORE_WHERE_IN_LIMIT = 10;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onError(Exception e);
    }

    public interface ParticipantListCallback {
        void onSuccess(List<Map<String, Object>> participants);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // --- EVENT FETCHING ---

    public void getUpcomingEvents(EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getUpcomingEvents failed", error);
                        cb.onError(error);
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            events.add(documentToEvent(doc));
                        }
                    }
                    cb.onSuccess(events);
                });
    }

    public void getFeaturedEvents(List<String> ids, EventListCallback cb) {
        if (ids == null || ids.isEmpty()) {
            cb.onSuccess(new ArrayList<>());
            return;
        }
        fetchEventsByIdsPreserveOrder(ids, null, cb);
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
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    cb.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getPersonalisedEvents failed", e);
                    cb.onError(e);
                });
    }

    public void getEventById(String eventId, SingleEventCallback cb) {
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

    public void searchEvents(String query, String category, EventListCallback cb) {
        final String safeQuery = query == null ? "" : query.trim().toLowerCase();
        final String safeCategory = category == null ? "All" : category.trim();

        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("status", "active")
                .orderBy("date", Query.Direction.ASCENDING)
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

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(event.getEventId());
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_RSVPS).document(event.getEventId());
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);

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
            rsvpData.put("qrCodeToken", qrToken);
            rsvpData.put("addedToCalendar", false);
            rsvpData.put("rsvpAt", Timestamp.now());
            transaction.set(userRsvpRef, rsvpData);

            Map<String, Object> attendeeData = new HashMap<>();
            attendeeData.put("userId", userId);
            attendeeData.put("fullName", fullName);
            attendeeData.put("qrToken", qrToken);
            attendeeData.put("checkedIn", false);
            transaction.set(eventAttendeeRef, attendeeData);

            transaction.update(eventRef, "rsvpCount", FieldValue.increment(1));

            return null;
        }).addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void cancelRsvp(String userId, String eventId, Runnable onSuccess) {
        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference userRsvpRef = db.collection(COLLECTION_USERS).document(userId).collection(SUBCOLLECTION_RSVPS).document(eventId);
        DocumentReference eventAttendeeRef = eventRef.collection(SUBCOLLECTION_ATTENDEES).document(userId);

        db.runTransaction(transaction -> {
            transaction.delete(userRsvpRef);
            transaction.delete(eventAttendeeRef);
            transaction.update(eventRef, "rsvpCount", FieldValue.increment(-1));
            return null;
        }).addOnSuccessListener(unused -> runIfNotNull(onSuccess));
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
        Map<String, Object> memory = new HashMap<>();
        memory.put("eventId", eventId);
        memory.put("eventTitle", eventTitle);
        memory.put("photoUrls", photoUrls);
        memory.put("attendedAt", Timestamp.now());
        memory.put("rating", rating);

        db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_MEMORIES)
                .add(memory);
    }

    public void addRating(String eventId, String userId, int stars, String review) {
        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document(eventId);
        DocumentReference ratingRef = eventRef.collection(SUBCOLLECTION_RATINGS).document(userId);

        Map<String, Object> rating = new HashMap<>();
        rating.put("userId", userId);
        rating.put("stars", stars);
        rating.put("review", review);
        rating.put("ratedAt", Timestamp.now());

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            Long countL = eventSnap.getLong("ratingCount");
            long count = countL != null ? countL : 0L;
            Double avgD = eventSnap.getDouble("averageRating");
            double avg = avgD != null ? avgD : 0.0;

            double newAvg = ((avg * count) + stars) / (count + 1);

            transaction.set(ratingRef, rating);
            transaction.update(eventRef, "averageRating", newAvg, "ratingCount", count + 1);
            return null;
        });
    }

    // --- ORGANIZER & ADMIN ---

    public void proposeEvent(EventProposal proposal, ActionCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .add(proposal)
                .addOnSuccessListener(ref -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getOrganizerEvents(String organizerId, EventListCallback cb) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("organizerId", organizerId)
                .whereEqualTo("status", "active")
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        events.add(documentToEvent(doc));
                    }
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getOrganizerProposals(String organizerId, ProposalListCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("organizerId", organizerId)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
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
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllPendingProposals(ProposalListCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS)
                .whereEqualTo("status", "pending")
                .orderBy("submittedAt", Query.Direction.ASCENDING)
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
                    cb.onSuccess(proposals);
                })
                .addOnFailureListener(cb::onError);
    }

    public void approveProposal(String proposalId, EventProposal proposal, ActionCallback cb) {
        WriteBatch batch = db.batch();

        DocumentReference proposalRef = db.collection(COLLECTION_EVENT_PROPOSALS).document(proposalId);
        batch.update(proposalRef, "status", "approved", "reviewedAt", Timestamp.now());

        DocumentReference eventRef = db.collection(COLLECTION_EVENTS).document();
        Event event = new Event();
        event.setTitle(proposal.getTitle());
        event.setDescription(proposal.getDescription());
        event.setCategory(proposal.getCategory());
        event.setTags(proposal.getTags());
        event.setDate(proposal.getDate());
        event.setLocation(proposal.getLocation());
        event.setCapacity(proposal.getCapacity());
        event.setOrganizerId(proposal.getOrganizerId());
        event.setOrganizerName(proposal.getOrganizerName());
        event.setStatus("active");
        event.setCreatedAt(Timestamp.now());
        event.setThumbnailUrl("");
        event.setTrailerUrl(proposal.getTrailerUrl());
        
        batch.set(eventRef, event);

        batch.commit().addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void rejectProposal(String proposalId, String note, ActionCallback cb) {
        db.collection(COLLECTION_EVENT_PROPOSALS).document(proposalId)
                .update("status", "rejected", "adminNote", note, "reviewedAt", Timestamp.now())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    public void getEventAttendees(String eventId, ParticipantListCallback cb) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .collection(SUBCOLLECTION_ATTENDEES)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        list.add(doc.getData());
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // --- SOS REPORTS ---

    public void sendSosReport(String reporterId, String reporterName, String description, double lat, double lng) {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reporterName", reporterName);
        report.put("isAnonymous", reporterId == null);
        report.put("description", description);
        
        Map<String, Object> location = new HashMap<>();
        location.put("lat", lat);
        location.put("lng", lng);
        report.put("location", location);
        
        report.put("status", "open");
        report.put("submittedAt", Timestamp.now());

        db.collection(COLLECTION_REPORTS).add(report);
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

    public void updateProfilePic(String userId, String url, ActionCallback cb) {
        db.collection(COLLECTION_USERS).document(userId)
                .update("profilePicUrl", url)
                .addOnSuccessListener(unused -> { if (cb != null) cb.onSuccess(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
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

    private void runIfNotNull(Runnable r) { if (r != null) r.run(); }
}