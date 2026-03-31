/**
 * AuthRepository.java
 *
 * Repository class responsible for all Firebase Authentication operations.
 * Handles user signup, login, logout, and current user retrieval.
 * On signup, creates a corresponding user document in Firestore.
 *
 */
package com.example.CampusEventDiscovery.repository;

import android.util.Log;

import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.util.FirebaseManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * Handles all Firebase Authentication and user document operations.
 * All methods are asynchronous — results delivered via AuthCallback interface.
 */
public class AuthRepository {

    private static final String TAG = AuthRepository.class.getSimpleName();
    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth = FirebaseManager.getInstance().getAuth();
    private final FirebaseFirestore db = FirebaseManager.getInstance().getDb();

    /**
     * Callback interface for authentication operations.
     */
    public interface AuthCallback {
        /**
         * Called when the auth operation completes successfully.
         * @param user The FirebaseUser returned after successful auth.
         */
        void onSuccess(FirebaseUser user);

        /**
         * Called when the auth operation fails.
         * @param e The exception describing the failure reason.
         */
        void onFailure(Exception e);
    }

    /**
     * Registers a new user with email and password.
     * On success, creates a user document in Firestore under users/{uid}.
     *
     * @param fullName Display name of the new user.
     * @param email    Email address for the new account.
     * @param password Password for the new account.
     * @param role     Role of the user — attendee or organizer.
     * @param callback Called with onSuccess(user) if signup succeeds,
     *                 onFailure(e) if signup fails.
     */
    public void signUp(String fullName, String email, String password,
                       String role, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure(new Exception("User creation failed"));
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    User user = new User(
                            uid, fullName, email, role,
                            "LUMS", null, new ArrayList<>(),
                            true, null, Timestamp.now()
                    );
                    db.collection(USERS_COLLECTION)
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> callback.onSuccess(firebaseUser))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create user document", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Signup failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email    Email address of the user.
     * @param password Password of the user.
     * @param callback Called with onSuccess(user) if login succeeds,
     *                 onFailure(e) if login fails.
     */
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sign in failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Signs out the currently authenticated user.
     * Clears the local Firebase Auth session.
     */
    public void signOut() {
        auth.signOut();
    }

    /**
     * Returns the currently signed in Firebase user, or null if not signed in.
     * @return FirebaseUser if authenticated, null otherwise.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
}
