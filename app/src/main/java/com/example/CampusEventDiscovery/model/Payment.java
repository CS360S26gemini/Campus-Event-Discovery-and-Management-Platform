package com.example.CampusEventDiscovery.model;

import com.google.firebase.firestore.Exclude;

/**
 * Payment.java
 *
 * Model class representing a payment record in Firestore.
 */
public class Payment {

    @Exclude
    private String paymentId;

    private String userId;
    private String eventId;
    private double amount;
    private String status; // "SUCCESS" or "FAILED"
    private String transactionId;
    private long timestamp;
    private String paymentMethod;
    private String proofUrl;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Payment() {
    }

    /**
     * Full constructor for creating Payment objects.
     *
     * @param paymentId Helper field for Firestore document ID.
     * @param userId User who made the payment.
     * @param eventId Event being paid for.
     * @param amount Amount paid.
     * @param status Payment status.
     * @param transactionId Randomly generated transaction ID.
     * @param timestamp Payment timestamp.
     */
    public Payment(String paymentId, String userId, String eventId, double amount, String status, String transactionId, long timestamp) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.eventId = eventId;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    @Exclude
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }
}
