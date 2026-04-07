package com.example.CampusEventDiscovery.repository;

import com.example.CampusEventDiscovery.callback.AuthCallback;
import com.example.CampusEventDiscovery.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthRepository implements AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public FirebaseAuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void signup(String name, String email, String password, String role, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Registration failed");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("fullName", name);
                    userMap.put("email", email);
                    userMap.put("role", role);
                    userMap.put("createdAt", Timestamp.now());
                    db.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(new User(name, email, role, "", "", "", null, false)))
                            .addOnFailureListener(e ->
                                    callback.onFailure("Failed to save user profile: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Registration failed: " + e.getMessage()));
    }

    @Override
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Login failed");
                        return;
                    }
                    db.collection("users").document(firebaseUser.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    callback.onFailure("User profile not found");
                                    return;
                                }
                                callback.onSuccess(new User(
                                        doc.getString("fullName"),
                                        doc.getString("email"),
                                        doc.getString("role"),
                                        "", "", "", null, false));
                            })
                            .addOnFailureListener(e ->
                                    callback.onFailure("Failed to fetch user profile: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Login failed: " + e.getMessage()));
    }

    @Override
    public void logout() {
        auth.signOut();
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
