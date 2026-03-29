/**
 * Event.java
 *
 * Model class representing a campus event in the Firestore database.
 * Maps directly to documents in the 'events' collection.
 * Follows the Repository pattern — data access is handled by EventRepository.
 *
 * Outstanding issues: None.
 */
package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Represents a single campus event.
 * All fields correspond to the Firestore 'events' collection schema
 * as defined in the project database design document.
 */
public class Event {

    private String eventId;
    private String title;
    private String description;
    private String organizerId;
    private String organizerName;
    private String venue;
    private Timestamp dateTime;
    private Timestamp endTime;
    private String category;
    private List<String> tags;
    private int capacity;
    private int registeredCount;
    private String imageUrl;
    private boolean isApproved;
    private boolean isVerified;
    private double averageRating;
    private int ratingCount;
    private String status;
    private Timestamp createdAt;

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
     * @param dateTime        Start date and time of the event.
     * @param endTime         End date and time of the event.
     * @param category        Event category e.g. academic, social, sports.
     * @param tags            List of tags matched against user interests for personalization.
     * @param capacity        Maximum number of attendees allowed.
     * @param registeredCount Current number of RSVPs.
     * @param imageUrl        Optional URL for event banner image in Firebase Storage.
     * @param isApproved      True if admin has approved this event for listing.
     * @param isVerified      True if admin has granted verified badge to this event.
     * @param averageRating   Average star rating calculated from ratings subcollection.
     * @param ratingCount     Total number of ratings submitted for this event.
     * @param status          Current status — active, cancelled, or completed.
     * @param createdAt       Timestamp of when this event was created.
     */
    public Event(String eventId, String title, String description,
                 String organizerId, String organizerName, String venue,
                 Timestamp dateTime, Timestamp endTime, String category,
                 List<String> tags, int capacity, int registeredCount,
                 String imageUrl, boolean isApproved, boolean isVerified,
                 double averageRating, int ratingCount, String status,
                 Timestamp createdAt) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.venue = venue;
        this.dateTime = dateTime;
        this.endTime = endTime;
        this.category = category;
        this.tags = tags;
        this.capacity = capacity;
        this.registeredCount = registeredCount;
        this.imageUrl = imageUrl;
        this.isApproved = isApproved;
        this.isVerified = isVerified;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.status = status;
        this.createdAt = createdAt;
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

    /** @return the start date and time of the event as a Firestore Timestamp. */
    public Timestamp getDateTime() { return dateTime; }

    /** @param dateTime The event start date and time to set. */
    public void setDateTime(Timestamp dateTime) { this.dateTime = dateTime; }

    /** @return the end date and time of the event as a Firestore Timestamp. */
    public Timestamp getEndTime() { return endTime; }

    /** @param endTime The event end date and time to set. */
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    /** @return the category of the event e.g. academic, social, sports. */
    public String getCategory() { return category; }

    /** @param category The event category to set. */
    public void setCategory(String category) { this.category = category; }

    /**
     * @return list of tags used for personalized event suggestions.
     * Matched against user interests stored in the users collection.
     */
    public List<String> getTags() { return tags; }

    /** @param tags The list of interest tags to set for this event. */
    public void setTags(List<String> tags) { this.tags = tags; }

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

    /** @return true if admin has approved this event for public listing. */
    public boolean isApproved() { return isApproved; }

    /** @param approved The approval status to set. */
    public void setApproved(boolean approved) { isApproved = approved; }

    /**
     * @return true if this event has been granted a verified badge by an admin.
     * Verified events display a checkmark badge on event cards and detail screens.
     */
    public boolean isVerified() { return isVerified; }

    /** @param verified The verified status to set. */
    public void setVerified(boolean verified) { isVerified = verified; }

    /**
     * @return the average star rating for this event calculated from
     * the ratings subcollection under this event document.
     */
    public double getAverageRating() { return averageRating; }

    /** @param averageRating The calculated average rating to set. */
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    /** @return the total number of ratings submitted for this event. */
    public int getRatingCount() { return ratingCount; }

    /** @param ratingCount The total rating count to set. */
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    /** @return the current status of the event — active, cancelled, or completed. */
    public String getStatus() { return status; }

    /** @param status The event status to set. */
    public void setStatus(String status) { this.status = status; }

    /** @return the timestamp of when this event document was created in Firestore. */
    public Timestamp getCreatedAt() { return createdAt; }

    /** @param createdAt The creation timestamp to set. */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}