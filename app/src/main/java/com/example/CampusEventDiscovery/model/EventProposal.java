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
     *
     * @param proposalId Helper field for Firestore document ID.
     * @param title Proposal title.
     * @param description Proposal description.
     * @param category Proposal category.
     * @param tags Proposal tags.
     * @param date Proposed event date.
     * @param location Proposed event location.
     * @param capacity Proposed event capacity.
     * @param sponsors Proposal sponsors.
     * @param foodStalls Proposal food stalls.
     * @param trailerUrl Proposal trailer URL.
     * @param organizerId Organizer user ID.
     * @param organizerName Organizer name.
     * @param status Proposal status.
     * @param adminNote Admin review note.
     * @param submittedAt Submission timestamp.
     * @param reviewedAt Review timestamp.
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

    /**
     * Gets the helper Firestore proposal document ID.
     *
     * @return Proposal document ID.
     */
    @Exclude
    public String getProposalId() {
        return proposalId;
    }

    /**
     * Sets the helper Firestore proposal document ID.
     *
     * @param proposalId Proposal document ID.
     */
    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }

    /**
     * Gets the proposal title.
     *
     * @return Proposal title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the proposal title.
     *
     * @param title Proposal title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the proposal description.
     *
     * @return Proposal description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the proposal description.
     *
     * @param description Proposal description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the proposal category.
     *
     * @return Proposal category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the proposal category.
     *
     * @param category Proposal category.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the proposal tags.
     *
     * @return Proposal tags list.
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the proposal tags.
     *
     * @param tags Proposal tags list.
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the proposed event date.
     *
     * @return Proposed date timestamp.
     */
    public Timestamp getDate() {
        return date;
    }

    /**
     * Sets the proposed event date.
     *
     * @param date Proposed date timestamp.
     */
    public void setDate(Timestamp date) {
        this.date = date;
    }

    /**
     * Gets the proposed location.
     *
     * @return Proposed location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the proposed location.
     *
     * @param location Proposed location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the proposed capacity.
     *
     * @return Proposed capacity.
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Sets the proposed capacity.
     *
     * @param capacity Proposed capacity.
     */
    public void setCapacity(long capacity) {
        this.capacity = capacity;
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
     * Gets the thumbnail/banner image URL for this event proposal.
     *
     * @return Thumbnail URL (Firebase Storage download URL).
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Sets the thumbnail/banner image URL for this event proposal.
     *
     * @param thumbnailUrl Firebase Storage download URL.
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /** Alias for {@link #getThumbnailUrl()} — kept for compatibility with Nausher's branch. */
    public String getImageUrl() { return thumbnailUrl; }

    /** Sets imageUrl and keeps thumbnailUrl in sync. */
    public void setImageUrl(String imageUrl) {
        this.thumbnailUrl = imageUrl;
    }

    /**
     * Gets the organizer ID.
     *
     * @return Organizer ID.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer ID.
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
     * Gets the proposal status.
     *
     * @return Proposal status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the proposal status.
     *
     * @param status Proposal status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the admin note.
     *
     * @return Admin note.
     */
    public String getAdminNote() {
        return adminNote;
    }

    /**
     * Sets the admin note.
     *
     * @param adminNote Admin note.
     */
    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    /**
     * Gets the submitted-at timestamp.
     *
     * @return Submission timestamp.
     */
    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    /**
     * Sets the submitted-at timestamp.
     *
     * @param submittedAt Submission timestamp.
     */
    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    /**
     * Gets the reviewed-at timestamp.
     *
     * @return Review timestamp.
     */
    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    /**
     * Sets the reviewed-at timestamp.
     *
     * @param reviewedAt Review timestamp.
     */
    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    /**
     * Gets the ticket price set by the organizer.
     *
     * @return Ticket price (0.0 for free events).
     */
    public double getTicketPrice() {
        return ticketPrice;
    }

    /**
     * Sets the ticket price for the event.
     *
     * @param ticketPrice Ticket price (0.0 for free events).
     */
    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}