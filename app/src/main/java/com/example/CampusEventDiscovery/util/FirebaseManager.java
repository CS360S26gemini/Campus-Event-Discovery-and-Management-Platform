/**
 * FirebaseManager.java
 *
 * Singleton utility class that provides a single point of access
 * to Firebase Authentication and Firestore instances throughout the app.
 * All Firebase access should go through this class.
 *
 */
package com.example.CampusEventDiscovery.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Singleton wrapper for Firebase service instances.
 * Ensures only one instance of FirebaseAuth and FirebaseFirestore
 * exists throughout the application lifecycle.
 */
public class FirebaseManager {

    private static final String TAG = FirebaseManager.class.getSimpleName();
    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    /**
     * Private constructor — initializes Firebase Auth and Firestore instances.
     */
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the singleton instance of FirebaseManager.
     * Creates it if it does not yet exist.
     *
     * @return the single FirebaseManager instance.
     */
    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Returns the FirebaseAuth instance for authentication operations.
     * @return FirebaseAuth instance.
     */
    public FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * Returns the FirebaseFirestore instance for database operations.
     * @return FirebaseFirestore instance.
     */
    public FirebaseFirestore getDb() {
        return db;
    }
}