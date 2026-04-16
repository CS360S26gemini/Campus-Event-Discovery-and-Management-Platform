package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.AuthCallback;
import com.example.CampusEventDiscovery.model.User;

import java.util.HashMap;
import java.util.Map;

public class MockAuthRepository implements AuthRepository {

    private final Map<String, User> users = new HashMap<>();
    private User currentUser = null;

    public MockAuthRepository() {
        // Pre-populate with some test users if needed, or rely on signup/login in tests
        User testUser = new User("Test User", "test@lums.edu.pk", "attendee", "", "", "", null, false);
        users.put(testUser.getEmail(), testUser);
    }

    @Override
    public void signup(String name, String email, String password, String role, AuthCallback callback) {
        if (name == null || name.trim().isEmpty()) {
            callback.onFailure("Name is required");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            callback.onFailure("Email is required");
            return;
        }
        if (users.containsKey(email)) {
            callback.onFailure("Email already registered");
            return;
        }
        User user = new User(name, email, role, "", "", "", null, false);
        users.put(email, user);
        currentUser = user;
        callback.onSuccess(user);
    }

    @Override
    public void login(String email, String password, AuthCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onFailure("Email is required");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            callback.onFailure("Password is required");
            return;
        }

        if ("admin".equals(email) && "admin123".equals(password)) {
            User admin = new User("Admin", email, "admin", "", "", "", null, false);
            currentUser = admin;
            callback.onSuccess(admin);
            return;
        }

        User user = users.get(email);
        if (user != null) {
            // Simple password check for test purposes
            if ("wrongpass".equals(password)) {
                callback.onFailure("Invalid password");
            } else {
                currentUser = user;
                callback.onSuccess(user);
            }
        } else {
            callback.onFailure("User not found");
        }
    }

    @Override
    public void logout() {
        currentUser = null;
    }

    @Override
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
