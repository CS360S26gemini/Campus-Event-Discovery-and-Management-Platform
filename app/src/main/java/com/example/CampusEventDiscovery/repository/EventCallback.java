/**
 * EventCallback.java
 *
 * Callback interface for single Event Firestore operations.
 * Used by EventRepository to return asynchronous results to the UI layer.
 *
 */
package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.model.Event;

/**
 * Callback interface for Firestore operations that return a single Event.
 */
public interface EventCallback {

    /**
     * Called when the Firestore operation completes successfully.
     * @param event The Event object returned from Firestore.
     */
    void onSuccess(Event event);

    /**
     * Called when the Firestore operation fails.
     * @param e The exception describing the failure reason.
     */
    void onFailure(Exception e);
}
