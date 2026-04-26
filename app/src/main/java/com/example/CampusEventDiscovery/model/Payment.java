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

    // Added for Ticket Tiers support
    private String tierId;
    private String tierName;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Payment() {
    }

    /**
     * Full constructor for creating Payment objects.
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
<<<<<<< Updated upstream
=======

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

    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
>>>>>>> Stashed changes
}
