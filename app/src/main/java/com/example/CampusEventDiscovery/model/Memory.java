package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory.java
 *
 * Model class representing a post-event memory saved by a user.
 */
public class Memory {
    private String eventId;
    private String eventTitle;
    private List<String> photoUrls;
    private Timestamp attendedAt;
    private int rating;

    public Memory() {
        this.photoUrls = new ArrayList<>();
    }

    public Memory(String eventId, String eventTitle, List<String> photoUrls, Timestamp attendedAt, int rating) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
        this.attendedAt = attendedAt;
        this.rating = rating;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public Timestamp getAttendedAt() { return attendedAt; }
    public void setAttendedAt(Timestamp attendedAt) { this.attendedAt = attendedAt; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}