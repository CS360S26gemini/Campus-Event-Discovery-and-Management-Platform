package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * EventProposal.java
 *
 * Model class representing a proposal document from the
 * event_proposals collection in Firestore.
 */
public class EventProposal {

    @Exclude
    private String proposalId;

    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private Timestamp date;
    private String location;
    private long capacity;
    private List<String> sponsors;
    private List<String> foodStalls;
    private String trailerUrl;
    private String thumbnailUrl;
    private String imageUrl; // Added for Cloudinary integration
    private String organizerId;
    private String organizerName;
    private String status;
    private String adminNote;
    private Timestamp submittedAt;
    private Timestamp reviewedAt;
    private double ticketPrice;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public EventProposal() {
        this.tags = new ArrayList<>();
        this.sponsors = new ArrayList<>();
        this.foodStalls = new ArrayList<>();
    }

    /**
     * Full constructor for creating EventProposal objects in code.
     */
    public EventProposal(String proposalId,
                         String title,
                         String description,
                         String category,
                         List<String> tags,
                         Timestamp date,
                         String location,
                         long capacity,
                         List<String> sponsors,
                         List<String> foodStalls,
                         String trailerUrl,
                         String organizerId,
                         String organizerName,
                         String status,
                         String adminNote,
                         Timestamp submittedAt,
                         Timestamp reviewedAt,
                         double ticketPrice) {
        this.proposalId = proposalId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.date = date;
        this.location = location;
        this.capacity = capacity;
        this.sponsors = sponsors != null ? sponsors : new ArrayList<>();
        this.foodStalls = foodStalls != null ? foodStalls : new ArrayList<>();
        this.trailerUrl = trailerUrl;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.status = status;
        this.adminNote = adminNote;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.ticketPrice = ticketPrice;
    }

    @Exclude
    public String getProposalId() { return proposalId; }
    public void setProposalId(String proposalId) { this.proposalId = proposalId; }

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

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }

    public List<String> getSponsors() { return sponsors; }
    public void setSponsors(List<String> sponsors) { this.sponsors = sponsors; }

    public List<String> getFoodStalls() { return foodStalls; }
    public void setFoodStalls(List<String> foodStalls) { this.foodStalls = foodStalls; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

    public Timestamp getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }

    public double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(double ticketPrice) { this.ticketPrice = ticketPrice; }
}
