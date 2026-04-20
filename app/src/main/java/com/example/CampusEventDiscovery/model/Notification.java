package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

/**
 * Notification.java
 *
 * Model class representing a user notification message.
 */
public class Notification {

    @Exclude
    private String notificationId;

    private String title;
    private String body;
    private String type;
    private String eventId;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {}

    public Notification(String title, String body, String type, String eventId) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.eventId = eventId;
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    @Exclude
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}