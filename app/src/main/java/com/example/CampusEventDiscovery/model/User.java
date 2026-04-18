package com.example.CampusEventDiscovery.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User.java
 *
 * Model class representing a user document from the Firestore users collection.
 * Aligned with the database design in /docs/db/initial_db.txt.
 */
public class User {

    private String fullName;
    private String email;
    private String role;
    private String university;
    private String location;
    private String profilePicUrl;
    private boolean avatarEnabled;
    private Map<String, Object> avatarConfig;
    private List<String> interests;
    private boolean darkMode;
    private String fcmToken;
    private Map<String, Object> googleCalendarToken;
    private Timestamp createdAt;
    private boolean acceptedTerms;
    private String acceptedPolicyVersion;
    private Timestamp acceptedTermsAt;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public User() {
        this.interests = new ArrayList<>();
        this.avatarConfig = new HashMap<>();
        this.googleCalendarToken = new HashMap<>();
    }

    /**
     * Full constructor for creating User objects in code.
     */
    public User(String fullName,
                String email,
                String role,
                String university,
                String location,
                String profilePicUrl,
                List<String> interests,
                boolean darkMode) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.university = university;
        this.location = location;
        this.profilePicUrl = profilePicUrl;
        this.avatarEnabled = false;
        this.avatarConfig = new HashMap<>();
        this.interests = interests != null ? interests : new ArrayList<>();
        this.darkMode = darkMode;
        this.googleCalendarToken = new HashMap<>();
        this.createdAt = Timestamp.now();
        this.acceptedTerms = false;
    }

    // Getters and Setters

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public boolean isAvatarEnabled() {
        return avatarEnabled;
    }

    public void setAvatarEnabled(boolean avatarEnabled) {
        this.avatarEnabled = avatarEnabled;
    }

    public Map<String, Object> getAvatarConfig() {
        return avatarConfig;
    }

    public void setAvatarConfig(Map<String, Object> avatarConfig) {
        this.avatarConfig = avatarConfig != null ? avatarConfig : new HashMap<>();
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public Map<String, Object> getGoogleCalendarToken() {
        return googleCalendarToken;
    }

    public void setGoogleCalendarToken(Map<String, Object> googleCalendarToken) {
        this.googleCalendarToken = googleCalendarToken;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public String getAcceptedPolicyVersion() {
        return acceptedPolicyVersion;
    }

    public void setAcceptedPolicyVersion(String acceptedPolicyVersion) {
        this.acceptedPolicyVersion = acceptedPolicyVersion;
    }

    public Timestamp getAcceptedTermsAt() {
        return acceptedTermsAt;
    }

    public void setAcceptedTermsAt(Timestamp acceptedTermsAt) {
        this.acceptedTermsAt = acceptedTermsAt;
    }
}
