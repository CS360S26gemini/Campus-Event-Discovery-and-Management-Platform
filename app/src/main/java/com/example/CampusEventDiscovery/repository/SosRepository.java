package com.example.CampusEventDiscovery.repository;

import android.text.TextUtils;

import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.SosAlert;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.EventTimeUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/** Repository responsible for writing SOS alerts and fan-out notifications to Firestore. */
public class SosRepository {

    private final FirebaseFirestore db;

    public SosRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public SosRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface SosCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void sendCheckedInEventSosAlert(SosAlert alert,
                                           String attendeeUserId,
                                           String organizerId,
                                           List<String> adminIds,
                                           SosCallback callback) {
        if (TextUtils.isEmpty(attendeeUserId) || alert == null || TextUtils.isEmpty(alert.getEventId())) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("SOS requires a checked-in attendee and event."));
            }
            return;
        }

        DocumentReference eventRef = db.collection(Constants.COLLECTION_EVENTS).document(alert.getEventId());
        DocumentReference attendeeRef = eventRef.collection(Constants.SUBCOLLECTION_ATTENDEES).document(attendeeUserId);
        DocumentReference rsvpRef = db.collection(Constants.COLLECTION_USERS)
                .document(attendeeUserId)
                .collection(Constants.SUBCOLLECTION_RSVPS)
                .document(alert.getEventId());
        DocumentReference sosRef = db.collection(Constants.COLLECTION_SOS_ALERTS).document();
        List<NotificationTarget> notificationTargets = buildNotificationTargets(alert, organizerId, adminIds);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) {
                throw new FirebaseFirestoreException("Event not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String eventStatus = eventSnap.getString("status");
            if (!TextUtils.isEmpty(eventStatus) && !"active".equalsIgnoreCase(eventStatus)) {
                throw new FirebaseFirestoreException("SOS is unavailable for inactive events.", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            DocumentSnapshot attendeeSnap = transaction.get(attendeeRef);
            DocumentSnapshot rsvpSnap = transaction.get(rsvpRef);
            boolean attendeeCheckedIn = attendeeSnap.exists() && Boolean.TRUE.equals(attendeeSnap.getBoolean("checkedIn"));
            boolean rsvpCheckedIn = rsvpSnap.exists() && Boolean.TRUE.equals(rsvpSnap.getBoolean("checkedIn"));
            String rsvpStatus = rsvpSnap.exists() ? rsvpSnap.getString("status") : "";
            boolean checkedIn = attendeeCheckedIn || rsvpCheckedIn || "attended".equalsIgnoreCase(rsvpStatus);
            if (!checkedIn) {
                throw new FirebaseFirestoreException("SOS is available only after attendee check-in.", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            Timestamp startTime = eventSnap.getTimestamp("date");
            Timestamp endTime = eventSnap.getTimestamp("endTime");
            if (!EventTimeUtils.isSosAllowed(true, startTime, endTime, System.currentTimeMillis())) {
                throw new FirebaseFirestoreException("SOS window has expired.", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            transaction.set(sosRef, alert, SetOptions.merge());
            for (NotificationTarget target : notificationTargets) {
                transaction.set(target.ref, target.notification);
            }
            return null;
        }).addOnSuccessListener(unused -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e);
        });
    }

    public void sendSosAlert(SosAlert alert,
                             String organizerId,
                             List<String> adminIds,
                             SosCallback callback) {
        WriteBatch batch = db.batch();

        DocumentReference sosRef = db.collection(Constants.COLLECTION_SOS_ALERTS).document();
        batch.set(sosRef, alert);

        if (!TextUtils.isEmpty(organizerId)) {
            Notification organizerNotification = new Notification(
                    "SOS Alert at Your Event",
                    "An attendee has triggered an SOS at " + alert.getEventName()
                            + ". Check their location immediately.",
                    "sos_alert",
                    alert.getEventId()
            );
            DocumentReference organizerNotifRef = db.collection(Constants.COLLECTION_NOTIFICATIONS)
                    .document(organizerId)
                    .collection(Constants.SUBCOLLECTION_MESSAGES)
                    .document();
            batch.set(organizerNotifRef, organizerNotification);
        }

        if (adminIds != null) {
            for (String adminId : adminIds) {
                if (TextUtils.isEmpty(adminId)) continue;
                Notification adminNotification = new Notification(
                        "SOS Alert",
                        "SOS triggered at " + alert.getEventName()
                                + " by " + alert.getDisplayName() + ".",
                        "sos_alert",
                        alert.getEventId()
                );
                DocumentReference adminNotifRef = db.collection(Constants.COLLECTION_NOTIFICATIONS)
                        .document(adminId)
                        .collection(Constants.SUBCOLLECTION_MESSAGES)
                        .document();
                batch.set(adminNotifRef, adminNotification);
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    private List<NotificationTarget> buildNotificationTargets(SosAlert alert, String organizerId, List<String> adminIds) {
        List<NotificationTarget> targets = new ArrayList<>();

        if (!TextUtils.isEmpty(organizerId)) {
            Notification organizerNotification = new Notification(
                    "SOS Alert at Your Event",
                    "An attendee has triggered an SOS at " + alert.getEventName()
                            + ". Check their location immediately.",
                    "sos_alert",
                    alert.getEventId()
            );
            DocumentReference organizerNotifRef = db.collection(Constants.COLLECTION_NOTIFICATIONS)
                    .document(organizerId)
                    .collection(Constants.SUBCOLLECTION_MESSAGES)
                    .document();
            targets.add(new NotificationTarget(organizerNotifRef, organizerNotification));
        }

        if (adminIds != null) {
            for (String adminId : adminIds) {
                if (TextUtils.isEmpty(adminId)) continue;
                Notification adminNotification = new Notification(
                        "SOS Alert",
                        "SOS triggered at " + alert.getEventName()
                                + " by " + alert.getDisplayName() + ".",
                        "sos_alert",
                        alert.getEventId()
                );
                DocumentReference adminNotifRef = db.collection(Constants.COLLECTION_NOTIFICATIONS)
                        .document(adminId)
                        .collection(Constants.SUBCOLLECTION_MESSAGES)
                        .document();
                targets.add(new NotificationTarget(adminNotifRef, adminNotification));
            }
        }

        return targets;
    }

    private static class NotificationTarget {
        private final DocumentReference ref;
        private final Notification notification;

        NotificationTarget(DocumentReference ref, Notification notification) {
            this.ref = ref;
            this.notification = notification;
        }
    }
}
