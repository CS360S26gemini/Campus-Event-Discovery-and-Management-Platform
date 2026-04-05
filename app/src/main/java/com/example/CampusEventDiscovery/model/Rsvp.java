package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

/**
 * Rsvp.java
 *
 * Model class representing an RSVP document in Firestore.
 */
public class Rsvp {

    @Exclude
    private String rsvpId;

    private String userId;
    private String eventId;
    private String title;
    private Timestamp date;
    private String status;
    private String paymentStatus;
    private String transactionId;
    private String qrPayload;
    private boolean checkedIn;
    private boolean qrExpired;
    private Timestamp rsvpAt;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Rsvp() {
    }

    /**
     * Full constructor for creating Rsvp objects.
     *
     * @param rsvpId        Helper field for Firestore document ID.
     * @param userId        User ID of the attendee.
     * @param eventId       Event ID.
     * @param title         Event title.
     * @param date          Event date.
     * @param status        RSVP status (e.g., "CONFIRMED").
     * @param paymentStatus Payment status (e.g., "SUCCESS").
     * @param transactionId Transaction ID from the payment.
     * @param qrPayload     JSON payload for QR code.
     * @param checkedIn     Whether the attendee has been checked in.
     * @param qrExpired     Whether the QR code has been used and expired.
     * @param rsvpAt        Timestamp of RSVP creation.
     */
    public Rsvp(String rsvpId, String userId, String eventId, String title, Timestamp date,
                String status, String paymentStatus, String transactionId, String qrPayload,
                boolean checkedIn, boolean qrExpired, Timestamp rsvpAt) {
        this.rsvpId = rsvpId;
        this.userId = userId;
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.qrPayload = qrPayload;
        this.checkedIn = checkedIn;
        this.qrExpired = qrExpired;
        this.rsvpAt = rsvpAt;
    }

    @Exclude
    public String getRsvpId() { return rsvpId; }
    public void setRsvpId(String rsvpId) { this.rsvpId = rsvpId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    /**
     * Returns whether this QR code has been used and is no longer valid.
     *
     * @return true if the QR code has expired after a successful check-in.
     */
    public boolean isQrExpired() { return qrExpired; }

    /**
     * Marks the QR code as expired. Called after organizer scans and
     * confirms attendance, making the ticket one-time use only.
     *
     * @param qrExpired true to invalidate this QR code.
     */
    public void setQrExpired(boolean qrExpired) { this.qrExpired = qrExpired; }

    public Timestamp getRsvpAt() { return rsvpAt; }
    public void setRsvpAt(Timestamp rsvpAt) { this.rsvpAt = rsvpAt; }
}
