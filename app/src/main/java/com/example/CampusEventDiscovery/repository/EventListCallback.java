/**
 * EventListCallback.java
 *
 * Callback interface for Firestore operations that return a list of Events.
 * Used by EventRepository.getEvents() to deliver results asynchronously.
 *
 */
package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.model.Event;
import java.util.List;

/**
 * Callback interface for Firestore operations that return multiple Events.
 */
public interface EventListCallback {

    /**
     * Called when the Firestore query completes successfully.
     * @param events List of Event objects returned from Firestore.
     */
    void onSuccess(List<Event> events);

    /**
     * Called when the Firestore query fails.
     * @param e The exception describing the failure reason.
     */
    void onFailure(Exception e);
}
