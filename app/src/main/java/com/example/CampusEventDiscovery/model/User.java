/**
 * User.java
 *
 * Model class representing a registered user in the Firestore database.
 * Maps directly to documents in the 'users' collection.
 * Document ID matches the Firebase Auth UID assigned on signup.
 *
 */
package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Represents a single registered user.
 * All fields correspond to the Firestore 'users' collection schema
 * as defined in the project database design document.
 */
public class User {

    private String userId;
    private String fullName;
    private String email;
    private String role;
    private String university;
    private String profilePicUrl;
    private List<String> interests;
    private boolean darkMode;
    private String fcmToken;
    private Timestamp createdAt;

    /**
     * Required no-argument constructor for Firestore deserialization.
     */
    public User() {}

    /**
     * Full constructor for creating a new User object programmatically.
     *
     * @param userId       Firebase Auth UID — same as Firestore document ID.
     * @param fullName     Full name of the user.
     * @param email        Email address of the user.
     * @param role         Role of the user: attendee, organizer, or admin.
     * @param university   University the user belongs to e.g. LUMS.
     * @param profilePicUrl Firebase Storage URL for the user's profile photo.
     * @param interests    List of interest tags for personalized suggestions.
     * @param darkMode     True if user has dark mode enabled.
     * @param fcmToken     Firebase Cloud Messaging token for push notifications.
     * @param createdAt    Timestamp of when the user account was created.
     */
    public User(String userId, String fullName, String email, String role,
                String university, String profilePicUrl, List<String> interests,
                boolean darkMode, String fcmToken, Timestamp createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.university = university;
        this.profilePicUrl = profilePicUrl;
        this.interests = interests;
        this.darkMode = darkMode;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
    }

    /** @return the Firebase Auth UID for this user. */
    public String getUserId() { return userId; }

    /** @param userId The Firebase Auth UID to set. */
    public void setUserId(String userId) { this.userId = userId; }

    /** @return the full name of the user. */
    public String getFullName() { return fullName; }

    /** @param fullName The full name to set. */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return the email address of the user. */
    public String getEmail() { return email; }

    /** @param email The email address to set. */
    public void setEmail(String email) { this.email = email; }

    /**
     * @return the role of the user.
     * Possible values: attendee, organizer, admin.
     */
    public String getRole() { return role; }

    /** @param role The role to set — attendee, organizer, or admin. */
    public void setRole(String role) { this.role = role; }

    /** @return the university the user belongs to. */
    public String getUniversity() { return university; }

    /** @param university The university name to set. */
    public void setUniversity(String university) { this.university = university; }

    /** @return the Firebase Storage URL for the user's profile photo. */
    public String getProfilePicUrl() { return profilePicUrl; }

    /** @param profilePicUrl The profile photo URL to set. */
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    /**
     * @return list of interest tags used for personalized event suggestions.
     * Matched against event tags in the events collection.
     */
    public List<String> getInterests() { return interests; }

    /** @param interests The list of interest tags to set. */
    public void setInterests(List<String> interests) { this.interests = interests; }

    /** @return true if the user has dark mode enabled. */
    public boolean isDarkMode() { return darkMode; }

    /** @param darkMode The dark mode preference to set. */
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    /** @return the FCM token for sending push notifications to this user. */
    public String getFcmToken() { return fcmToken; }

    /** @param fcmToken The FCM token to set. */
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    /** @return the timestamp of when this user account was created. */
    public Timestamp getCreatedAt() { return createdAt; }

    /** @param createdAt The creation timestamp to set. */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
