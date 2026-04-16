package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.FirestoreCallback;
import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentRepository.java
 *
 * Firestore ops for payments. Collection name comes from Constants.
 */
public class PaymentRepository {

    private final FirebaseFirestore db;

    public PaymentRepository() {
        this(FirebaseFirestore.getInstance());
    }

    // Visible for testing — lets tests inject a mocked Firestore.
    public PaymentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Saves a payment record.
     */
    public void savePayment(Payment payment, FirestoreCallback callback) {
        db.collection(Constants.COLLECTION_PAYMENTS)
                .add(payment)
                .addOnSuccessListener(documentReference -> {
                    payment.setPaymentId(documentReference.getId());
                    callback.onSuccess(payment);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Looks up a payment by its Stripe transaction / intent reference.
     */
    public void getPaymentByTransactionId(String txnId, FirestoreCallback callback) {
        db.collection(Constants.COLLECTION_PAYMENTS)
                .whereEqualTo("transactionId", txnId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Payment payment = queryDocumentSnapshots.getDocuments().get(0).toObject(Payment.class);
                        if (payment != null) {
                            payment.setPaymentId(queryDocumentSnapshots.getDocuments().get(0).getId());
                        }
                        callback.onSuccess(payment);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns all payments for a given event, newest first.
     * Used by the organizer PaymentConfirmationActivity.
     */
    public void getPaymentsForEvent(String eventId, FirestoreCallback callback) {
        db.collection(Constants.COLLECTION_PAYMENTS)
                .whereEqualTo("eventId", eventId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        Payment p = doc.toObject(Payment.class);
                        if (p != null) {
                            p.setPaymentId(doc.getId());
                            payments.add(p);
                        }
                    });
                    callback.onSuccess(payments);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
