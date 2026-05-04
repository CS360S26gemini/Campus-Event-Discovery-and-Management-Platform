package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;

/**
 * EventAttendee.java
 *
 * Model class representing an attendee document under events/{eventId}/attendees/{userId}.
 */
public class EventAttendee {

    private String userId;
    private String fullName;
    private String cnic;
    private String countryCode;
    private String phone;
    private String phoneNumber;
    private String qrToken;
    private boolean checkedIn;
    private Timestamp checkedInAt;
    private boolean blacklisted;
    private Timestamp blacklistedAt;

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

    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

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

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public Timestamp getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Timestamp blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }
}
