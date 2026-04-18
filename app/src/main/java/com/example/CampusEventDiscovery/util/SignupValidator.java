package com.example.CampusEventDiscovery.util;

import android.text.TextUtils;
import android.util.Patterns;

public class SignupValidator {

    public static String validate(String name, String email, String password,
                                  String confirmPassword, String role) {
        String error = validateName(name);
        if (error != null) return error;

        error = validateEmail(email);
        if (error != null) return error;

        error = validatePassword(password);
        if (error != null) return error;

        error = validatePasswordConfirmation(password, confirmPassword);
        if (error != null) return error;

        error = validateRole(role);
        if (error != null) return error;

        return null;
    }

    public static String validatePolicyAcceptance(boolean accepted) {
        return accepted ? null : "Please accept the Terms and Conditions and Privacy Policy to continue.";
    }

    public static String validateName(String name) {
        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            return "Name is required";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return "Email is required";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return "Password is required";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return "Password must contain at least one special character";
        }
        return null;
    }

    public static String validatePasswordConfirmation(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            return "Please confirm your password";
        }
        if (!TextUtils.equals(password, confirmPassword)) {
            return "Passwords do not match";
        }
        return null;
    }

    public static String validateRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return "Please select a role";
        }
        if (!role.equals("attendee") && !role.equals("organizer")) {
            return "Invalid role selected";
        }
        return null;
    }

    public static String validateCampus(String campus) {
        if (TextUtils.isEmpty(campus) || campus.trim().isEmpty()) {
            return "Please select a campus";
        }
        return null;
    }
}
