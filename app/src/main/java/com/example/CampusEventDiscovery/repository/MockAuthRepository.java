package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.AuthCallback;
import com.example.CampusEventDiscovery.model.User;

import java.util.HashMap;
import java.util.Map;

public class MockAuthRepository implements AuthRepository {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void registerUser(String name, String email, String password, String role, AuthCallback callback) {
        if (users.containsKey(email)) {
            callback.onFailure("Email already registered");
            return;
        }
        User user = new User(name, email, role, "", "", "", null, false);
        users.put(email, user);
        callback.onSuccess(user);
    }

    @Override
    public void loginUser(String email, String password, AuthCallback callback) {
        if (email.contains("organizer")) {
            callback.onSuccess(new User("Organizer", email, "organizer", "", "", "", null, false));
        } else if (email.contains("admin")) {
            callback.onSuccess(new User("Admin", email, "admin", "", "", "", null, false));
        } else {
            callback.onSuccess(new User("Student", email, "attendee", "", "", "", null, false));
        }
    }
}