package com.example.CampusEventDiscovery.model;

/** Model class representing an SOS alert document in the sos_alerts Firestore collection. */
public class SosAlert {

    private String userId;
    private String displayName;
    private String eventId;
    private String eventName;
    private String organizerId;
    private double latitude;
    private double longitude;
    private String mapsUrl;
    private long timestamp;
    private String status;

    public SosAlert() {
    }

    public SosAlert(String userId,
                    String displayName,
                    String eventId,
                    String eventName,
                    String organizerId,
                    double latitude,
                    double longitude,
                    String mapsUrl,
                    long timestamp,
                    String status) {
        this.userId = userId;
        this.displayName = displayName;
        this.eventId = eventId;
        this.eventName = eventName;
        this.organizerId = organizerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapsUrl = mapsUrl;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getMapsUrl() { return mapsUrl; }
    public void setMapsUrl(String mapsUrl) { this.mapsUrl = mapsUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
