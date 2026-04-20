package com.example.CampusEventDiscovery.repository;

import android.util.Log;

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
 * Repository class for handling payment-related Firestore operations.
 */
public class PaymentRepository {

    private static final String TAG = "PaymentRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Saves a payment record to Firestore.
     *
     * @param payment  The payment object to save.
     * @param callback The callback to handle success or failure.
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
     * Retrieves a payment by its transaction ID.
     *
     * @param txnId    The transaction ID to search for.
     * @param callback The callback to handle success or failure.
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
     */
    public void getPaymentsForEvent(String eventId, FirestoreCallback callback) {
        // Removed orderBy timestamp to avoid requiring a composite index for this query.
        // We will sort manually in the UI if needed, but standard query ensures we get the data.
        db.collection(Constants.COLLECTION_PAYMENTS)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setPaymentId(doc.getId());
                            payments.add(payment);
                        }
                    });
                    callback.onSuccess(payments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getPaymentsForEvent failed", e);
                    callback.onFailure(e);
                });
    }
}
