/**
 * Event.java
 *
 * Model class representing a campus event in the Firestore database.
 * Maps directly to documents in the 'events' collection.
 * Follows the Repository pattern — data access is handled by EventRepository.
 *
 */
package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;

/**
 * Represents a single campus event.
 * All fields correspond to the Firestore 'events' collection schema.
 */
public class Event {

    private String eventId;
    private String title;
    private String description;
    private String organizerId;
    private String organizerName;
    private String venue;
    private Timestamp dateTime;
    private String category;
    private int capacity;
    private int registeredCount;
    private String imageUrl;
    private boolean isApproved;

    /**
     * Required no-argument constructor for Firestore deserialization.
     * Firestore uses this to reconstruct Event objects from documents.
     */
    public Event() {}

    /**
     * Full constructor for creating a new Event object programmatically.
     *
     * @param eventId         Auto-generated Firestore document ID.
     * @param title           Name of the event.
     * @param description     Full description of the event.
     * @param organizerId     Firebase Auth UID of the organizer.
     * @param organizerName   Display name of the organizer.
     * @param venue           Location or venue name.
     * @param dateTime        Date and time of the event.
     * @param category        Event category e.g. academic, social, sports.
     * @param capacity        Maximum number of attendees allowed.
     * @param registeredCount Current number of RSVPs.
     * @param imageUrl        Optional URL for event banner image.
     * @param isApproved      True if admin has approved this event.
     */
    public Event(String eventId, String title, String description,
                 String organizerId, String organizerName, String venue,
                 Timestamp dateTime, String category, int capacity,
                 int registeredCount, String imageUrl, boolean isApproved) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.venue = venue;
        this.dateTime = dateTime;
        this.category = category;
        this.capacity = capacity;
        this.registeredCount = registeredCount;
        this.imageUrl = imageUrl;
        this.isApproved = isApproved;
    }

    /** @return the Firestore document ID for this event. */
    public String getEventId() { return eventId; }

    /** @param eventId The Firestore document ID to set. */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return the title of the event. */
    public String getTitle() { return title; }

    /** @param title The event title to set. */
    public void setTitle(String title) { this.title = title; }

    /** @return the full description of the event. */
    public String getDescription() { return description; }

    /** @param description The event description to set. */
    public void setDescription(String description) { this.description = description; }

    /** @return the Firebase Auth UID of the organizer. */
    public String getOrganizerId() { return organizerId; }

    /** @param organizerId The organizer UID to set. */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /** @return the display name of the organizer. */
    public String getOrganizerName() { return organizerName; }

    /** @param organizerName The organizer display name to set. */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /** @return the venue or location of the event. */
    public String getVenue() { return venue; }

    /** @param venue The venue name to set. */
    public void setVenue(String venue) { this.venue = venue; }

    /** @return the date and time of the event as a Firestore Timestamp. */
    public Timestamp getDateTime() { return dateTime; }

    /** @param dateTime The event date and time to set. */
    public void setDateTime(Timestamp dateTime) { this.dateTime = dateTime; }

    /** @return the category of the event e.g. academic, social, sports. */
    public String getCategory() { return category; }

    /** @param category The event category to set. */
    public void setCategory(String category) { this.category = category; }

    /** @return the maximum number of attendees allowed. */
    public int getCapacity() { return capacity; }

    /** @param capacity The maximum capacity to set. */
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** @return the current number of confirmed RSVPs. */
    public int getRegisteredCount() { return registeredCount; }

    /** @param registeredCount The current RSVP count to set. */
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }

    /** @return the Firebase Storage URL for the event banner image, or null if not set. */
    public String getImageUrl() { return imageUrl; }

    /** @param imageUrl The Firebase Storage image URL to set. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** @return true if admin has approved this event, false if pending. */
    public boolean isApproved() { return isApproved; }

    /** @param approved The approval status to set. */
    public void setApproved(boolean approved) { isApproved = approved; }
}