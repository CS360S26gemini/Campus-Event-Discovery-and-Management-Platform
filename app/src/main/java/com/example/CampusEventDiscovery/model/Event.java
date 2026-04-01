package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Event.java
 *
 * Model class representing a single event document from the Firestore
 * events collection.
 *
 * Firestore-backed fields follow Yahya's shared schema. The helper field
 * eventId is marked with @Exclude because it is typically taken from the
 * Firestore document ID rather than stored as a normal field.
 */
public class Event {

    @Exclude
    private String eventId;

    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private Timestamp date;
    private Timestamp endTime;
    private String location;
    private long capacity;
    private long rsvpCount;
    private long checkedInCount;
    private String thumbnailUrl;
    private String trailerUrl;
    private List<String> sponsors;
    private List<String> foodStalls;
    private String organizerId;
    private String organizerName;
    private boolean isVerified;
    private double averageRating;
    private long ratingCount;
    private String status;
    private Timestamp createdAt;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Event() {
        this.tags = new ArrayList<>();
        this.sponsors = new ArrayList<>();
        this.foodStalls = new ArrayList<>();
    }

    /**
     * Full constructor for creating Event objects in code.
     *
     * @param eventId Helper field for the Firestore document ID.
     * @param title Event title.
     * @param description Event description.
     * @param category Event category.
     * @param tags Event tags for personalization/search.
     * @param date Event start date/time.
     * @param endTime Event end date/time.
     * @param location Event venue/location.
     * @param capacity Maximum event capacity.
     * @param rsvpCount Current RSVP count.
     * @param checkedInCount Current checked-in attendee count.
     * @param thumbnailUrl Event thumbnail/banner image URL.
     * @param trailerUrl Event trailer video URL.
     * @param sponsors List of sponsors.
     * @param foodStalls List of food stalls.
     * @param organizerId Organizer user ID.
     * @param organizerName Organizer display name.
     * @param isVerified Whether the event is admin-verified.
     * @param averageRating Average event rating.
     * @param ratingCount Number of ratings.
     * @param status Event status.
     * @param createdAt Event creation timestamp.
     */
    public Event(String eventId,
                 String title,
                 String description,
                 String category,
                 List<String> tags,
                 Timestamp date,
                 Timestamp endTime,
                 String location,
                 long capacity,
                 long rsvpCount,
                 long checkedInCount,
                 String thumbnailUrl,
                 String trailerUrl,
                 List<String> sponsors,
                 List<String> foodStalls,
                 String organizerId,
                 String organizerName,
                 boolean isVerified,
                 double averageRating,
                 long ratingCount,
                 String status,
                 Timestamp createdAt) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.date = date;
        this.endTime = endTime;
        this.location = location;
        this.capacity = capacity;
        this.rsvpCount = rsvpCount;
        this.checkedInCount = checkedInCount;
        this.thumbnailUrl = thumbnailUrl;
        this.trailerUrl = trailerUrl;
        this.sponsors = sponsors != null ? sponsors : new ArrayList<>();
        this.foodStalls = foodStalls != null ? foodStalls : new ArrayList<>();
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.isVerified = isVerified;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Gets the helper Firestore document ID.
     *
     * @return Event document ID.
     */
    @Exclude
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the helper Firestore document ID.
     *
     * @param eventId Event document ID.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the event title.
     *
     * @return Event title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the event title.
     *
     * @param title Event title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the event description.
     *
     * @return Event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     *
     * @param description Event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the event category.
     *
     * @return Event category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the event category.
     *
     * @param category Event category.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the event tags.
     *
     * @return Event tags list.
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the event tags.
     *
     * @param tags Event tags list.
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the event start timestamp.
     *
     * @return Event start timestamp.
     */
    public Timestamp getDate() {
        return date;
    }

    /**
     * Sets the event start timestamp.
     *
     * @param date Event start timestamp.
     */
    public void setDate(Timestamp date) {
        this.date = date;
    }

    /**
     * Gets the event end timestamp.
     *
     * @return Event end timestamp.
     */
    public Timestamp getEndTime() {
        return endTime;
    }

    /**
     * Sets the event end timestamp.
     *
     * @param endTime Event end timestamp.
     */
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the event location.
     *
     * @return Event location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the event location.
     *
     * @param location Event location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the event capacity.
     *
     * @return Event capacity.
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Sets the event capacity.
     *
     * @param capacity Event capacity.
     */
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    /**
     * Gets the current RSVP count.
     *
     * @return RSVP count.
     */
    public long getRsvpCount() {
        return rsvpCount;
    }

    /**
     * Sets the current RSVP count.
     *
     * @param rsvpCount RSVP count.
     */
    public void setRsvpCount(long rsvpCount) {
        this.rsvpCount = rsvpCount;
    }

    /**
     * Gets the checked-in attendee count.
     *
     * @return Checked-in count.
     */
    public long getCheckedInCount() {
        return checkedInCount;
    }

    /**
     * Sets the checked-in attendee count.
     *
     * @param checkedInCount Checked-in count.
     */
    public void setCheckedInCount(long checkedInCount) {
        this.checkedInCount = checkedInCount;
    }

    /**
     * Gets the thumbnail URL.
     *
     * @return Thumbnail URL.
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Sets the thumbnail URL.
     *
     * @param thumbnailUrl Thumbnail URL.
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Gets the trailer URL.
     *
     * @return Trailer URL.
     */
    public String getTrailerUrl() {
        return trailerUrl;
    }

    /**
     * Sets the trailer URL.
     *
     * @param trailerUrl Trailer URL.
     */
    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    /**
     * Gets the sponsors list.
     *
     * @return Sponsors list.
     */
    public List<String> getSponsors() {
        return sponsors;
    }

    /**
     * Sets the sponsors list.
     *
     * @param sponsors Sponsors list.
     */
    public void setSponsors(List<String> sponsors) {
        this.sponsors = sponsors;
    }

    /**
     * Gets the food stalls list.
     *
     * @return Food stalls list.
     */
    public List<String> getFoodStalls() {
        return foodStalls;
    }

    /**
     * Sets the food stalls list.
     *
     * @param foodStalls Food stalls list.
     */
    public void setFoodStalls(List<String> foodStalls) {
        this.foodStalls = foodStalls;
    }

    /**
     * Gets the organizer user ID.
     *
     * @return Organizer ID.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer user ID.
     *
     * @param organizerId Organizer ID.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the organizer name.
     *
     * @return Organizer name.
     */
    public String getOrganizerName() {
        return organizerName;
    }

    /**
     * Sets the organizer name.
     *
     * @param organizerName Organizer name.
     */
    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    /**
     * Returns whether the event is verified.
     *
     * @return True if verified, otherwise false.
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Sets whether the event is verified.
     *
     * @param verified Verification flag.
     */
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    /**
     * Gets the average rating.
     *
     * @return Average rating.
     */
    public double getAverageRating() {
        return averageRating;
    }

    /**
     * Sets the average rating.
     *
     * @param averageRating Average rating.
     */
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    /**
     * Gets the rating count.
     *
     * @return Rating count.
     */
    public long getRatingCount() {
        return ratingCount;
    }

    /**
     * Sets the rating count.
     *
     * @param ratingCount Rating count.
     */
    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }

    /**
     * Gets the event status.
     *
     * @return Event status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the event status.
     *
     * @param status Event status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the created-at timestamp.
     *
     * @return Created-at timestamp.
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created-at timestamp.
     *
     * @param createdAt Created-at timestamp.
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}