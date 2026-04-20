package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.AuthCallback;

public interface AuthRepository {
    void signup(String name, String email, String password, String role, AuthCallback callback);
    void login(String email, String password, AuthCallback callback);
    void logout();
    boolean isLoggedIn();
}
