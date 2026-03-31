package com.example.campuseventdiscovery.model;

import com.google.firebase.Timestamp;

/**
 * EventAttendee.java
 *
 * Model class representing an attendee document under events/{eventId}/attendees/{userId}.
 */
public class EventAttendee {

    private String userId;
    private String fullName;
    private String qrToken;
    private boolean checkedIn;
    private Timestamp checkedInAt;

    public EventAttendee() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public Timestamp getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Timestamp checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
