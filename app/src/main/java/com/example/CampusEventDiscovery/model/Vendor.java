package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;

/**
 * Vendor.java
 *
 * Model class representing a vendor request in Firestore.
 */
public class Vendor {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private String vendorId;
    private String vendorName;
    private String phoneNumber;
    private String vendorType;
    private String eventId;
    private String eventTitle;
    private String organizerId;
    private String organizerName;
    private String status = STATUS_PENDING;
    private Timestamp createdAt;

    public Vendor() {
    }

    public Vendor(String vendorId, String vendorName, String phoneNumber, String vendorType,
                  String eventId, String eventTitle, String organizerId, String organizerName,
                  String status, Timestamp createdAt) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.phoneNumber = phoneNumber;
        this.vendorType = vendorType;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getVendorType() { return vendorType; }
    public void setVendorType(String vendorType) { this.vendorType = vendorType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

}
