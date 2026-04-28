package com.example.CampusEventDiscovery.model;

import com.google.firebase.firestore.Exclude;

/**
 * TicketTier.java
 *
 * Model class representing a ticket tier in the ticket_tiers subcollection.
 */
public class TicketTier {

    @Exclude
    private String tierId;

    private String name;
    private double price;
    private long capacity;
    private long rsvpCount;
    private String description;

    public TicketTier() {
    }

    public TicketTier(String tierId, String name, double price, long capacity, long rsvpCount, String description) {
        this.tierId = tierId;
        this.name = name;
        this.price = price;
        this.capacity = capacity;
        this.rsvpCount = rsvpCount;
        this.description = description;
    }

    @Exclude
    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }

    public long getRsvpCount() { return rsvpCount; }
    public void setRsvpCount(long rsvpCount) { this.rsvpCount = rsvpCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Exclude
    public long getRemainingCapacity() {
        return Math.max(0, capacity - rsvpCount);
    }

    @Exclude
    public boolean isSoldOut() {
        return rsvpCount >= capacity;
    }
}
