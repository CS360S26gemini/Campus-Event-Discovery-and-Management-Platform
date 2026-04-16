package com.example.CampusEventDiscovery.repository;

import android.text.TextUtils;

import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.SosAlert;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

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
}
