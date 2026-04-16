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
    private String paymentRef;
    private String qrPayload;
    private boolean checkedIn;
    private boolean qrExpired;
    private Timestamp rsvpAt;
    private Timestamp checkedInAt;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Rsvp() {
    }

    /**
     * Full constructor for creating Rsvp objects.
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

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public boolean isQrExpired() { return qrExpired; }
    public void setQrExpired(boolean qrExpired) { this.qrExpired = qrExpired; }

    public Timestamp getRsvpAt() { return rsvpAt; }
    public void setRsvpAt(Timestamp rsvpAt) { this.rsvpAt = rsvpAt; }

    public Timestamp getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Timestamp checkedInAt) { this.checkedInAt = checkedInAt; }

    @Exclude
    public long getCheckInTimestamp() {
        return checkedInAt != null ? checkedInAt.toDate().getTime() : 0L;
    }

    @Exclude
    public void setCheckInTimestamp(long timestamp) {
        this.checkedInAt = new Timestamp(new java.util.Date(timestamp));
    }
}
