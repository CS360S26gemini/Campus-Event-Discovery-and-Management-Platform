package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Event.java
 *
 * Model class representing an approved and active event in Firestore.
 */
public class Event {

    @Exclude
    private String eventId;

    private String title;
    private String description;
    private String category;
    private List<String> tags = new ArrayList<>();
    private Timestamp date;
    private Timestamp endTime;
    private String location;
    private long capacity;
    private long rsvpCount;
    private long checkedInCount;
    private String thumbnailUrl;
    private String trailerUrl;
    private List<String> sponsors = new ArrayList<>();
    private List<String> foodStalls = new ArrayList<>();
    private String organizerId;
    private String organizerName;
    private boolean verified;
    private double averageRating;
    private long ratingCount;
    private String status = "pending"; // Default status
    private Timestamp createdAt;
    private double ticketPrice;
    private String imageUrl; // Added for Cloudinary integration

    public Event() {
    }

    @Exclude
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }

    public long getRsvpCount() { return rsvpCount; }
    public void setRsvpCount(long rsvpCount) { this.rsvpCount = rsvpCount; }

    public long getCheckedInCount() { return checkedInCount; }
    public void setCheckedInCount(long checkedInCount) { this.checkedInCount = checkedInCount; }

    @Exclude
    public long getAttendedCount() { return checkedInCount; }
    public void setAttendedCount(long attendedCount) { this.checkedInCount = attendedCount; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public List<String> getSponsors() { return sponsors; }
    public void setSponsors(List<String> sponsors) { this.sponsors = sponsors; }

    public List<String> getFoodStalls() { return foodStalls; }
    public void setFoodStalls(List<String> foodStalls) { this.foodStalls = foodStalls; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getRatingCount() { return ratingCount; }
    public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(double ticketPrice) { this.ticketPrice = ticketPrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
