package com.example.CampusEventDiscovery.callback;

/**
 * FirestoreCallback.java
 *
 * Generic callback interface for Firestore operations.
 */
public interface FirestoreCallback {
    /**
     * Called when the Firestore operation is successful.
     *
     * @param result The result of the operation.
     */
    void onSuccess(Object result);

    /**
     * Called when the Firestore operation fails.
     *
     * @param e The exception that occurred.
     */
    void onFailure(Exception e);
}
