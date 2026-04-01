package com.example.CampusEventDiscovery.callback;

import com.example.CampusEventDiscovery.model.User;

public interface AuthCallback {
    void onSuccess(User user);
    void onFailure(String errorMessage);
}