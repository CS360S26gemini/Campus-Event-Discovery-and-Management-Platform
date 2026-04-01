package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.AuthCallback;

public interface AuthRepository {
    void registerUser(String name, String email, String password, String role, AuthCallback callback);
    void loginUser(String email, String password, AuthCallback callback);
}