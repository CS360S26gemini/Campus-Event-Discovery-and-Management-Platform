/**
 * Rsvp.java
 *
 * Model class representing a user's RSVP to a campus event.
 * Stored as a subcollection under users/{userId}/rsvps/{eventId}
 * and referenced in events/{eventId}/attendees/{userId}.
 *
 */
package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;

/**
 * Represents a single RSVP record linking a user to an event.
 * All fields correspond to the Firestore rsvps subcollection schema.
 */
public class Rsvp {

    private String eventId;
    private String title;
    private Timestamp date;
    private String status;
    private boolean checkedIn;
    private String qrCodeToken;
    private boolean addedToCalendar;
    private String gcalEventId;
    private Timestamp rsvpAt;

    /**
     * Required no-argument constructor for Firestore deserialization.
     */
    public Rsvp() {}

    /**
     * Full constructor for creating a new RSVP record.
     *
     * @param eventId        Firestore document ID of the event being RSVPed to.
     * @param title          Event title copied for fast display without extra query.
     * @param date           Event date copied for fast display.
     * @param qrCodeToken    Unique token encoded in the QR code for check-in validation.
     */
    public Rsvp(String eventId, String title, Timestamp date, String qrCodeToken) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.status = "confirmed";
        this.checkedIn = false;
        this.qrCodeToken = qrCodeToken;
        this.addedToCalendar = false;
        this.gcalEventId = null;
        this.rsvpAt = Timestamp.now();
    }

    /** @return the Firestore document ID of the event. */
    public String getEventId() { return eventId; }

    /** @param eventId The event document ID to set. */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return the event title copied for fast display. */
    public String getTitle() { return title; }

    /** @param title The event title to set. */
    public void setTitle(String title) { this.title = title; }

    /** @return the event date copied for fast display. */
    public Timestamp getDate() { return date; }

    /** @param date The event date to set. */
    public void setDate(Timestamp date) { this.date = date; }

    /**
     * @return the current status of this RSVP.
     * Possible values: confirmed, cancelled, attended.
     */
    public String getStatus() { return status; }

    /** @param status The RSVP status to set. */
    public void setStatus(String status) { this.status = status; }

    /** @return true if the user has physically checked in at the event via QR scan. */
    public boolean isCheckedIn() { return checkedIn; }

    /** @param checkedIn The check-in status to set. */
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    /**
     * @return the unique QR code token for this RSVP.
     * Must match the qrToken stored in events/{eventId}/attendees/{userId}.
     */
    public String getQrCodeToken() { return qrCodeToken; }

    /** @param qrCodeToken The QR code token to set. */
    public void setQrCodeToken(String qrCodeToken) { this.qrCodeToken = qrCodeToken; }

    /** @return true if this event has been added to the user's Google Calendar. */
    public boolean isAddedToCalendar() { return addedToCalendar; }

    /** @param addedToCalendar The calendar sync status to set. */
    public void setAddedToCalendar(boolean addedToCalendar) { this.addedToCalendar = addedToCalendar; }

    /**
     * @return the Google Calendar event ID for this RSVP.
     * Used to delete the calendar entry if the user un-RSVPs.
     */
    public String getGcalEventId() { return gcalEventId; }

    /** @param gcalEventId The Google Calendar event ID to set. */
    public void setGcalEventId(String gcalEventId) { this.gcalEventId = gcalEventId; }

    /** @return the timestamp of when this RSVP was created. */
    public Timestamp getRsvpAt() { return rsvpAt; }

    /** @param rsvpAt The RSVP creation timestamp to set. */
    public void setRsvpAt(Timestamp rsvpAt) { this.rsvpAt = rsvpAt; }
}

