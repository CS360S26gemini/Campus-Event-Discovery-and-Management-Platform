package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.FirestoreCallback;
import com.example.CampusEventDiscovery.model.Payment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * PaymentRepository.java
 *
 * Repository class for handling payment-related Firestore operations.
 */
public class PaymentRepository {

    private static final String COLLECTION_PAYMENTS = "payments";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Saves a payment record to Firestore.
     *
     * @param payment  The payment object to save.
     * @param callback The callback to handle success or failure.
     */
    public void savePayment(Payment payment, FirestoreCallback callback) {
        db.collection(COLLECTION_PAYMENTS)
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
        db.collection(COLLECTION_PAYMENTS)
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
}
