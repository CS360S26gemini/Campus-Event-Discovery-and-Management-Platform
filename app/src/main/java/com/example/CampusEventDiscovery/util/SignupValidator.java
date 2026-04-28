package com.example.CampusEventDiscovery.util;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.List;

public class SignupValidator {

    public static final String PASSWORD_REQUIREMENTS_MESSAGE =
            "Password must be at least 8 characters and include 1 uppercase letter, 1 number, and 1 special character.";

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
        if (password == null || password.trim().isEmpty()) {
            return "Password is required";
        }

        boolean hasUppercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) {
                hasUppercase = true;
            }
            if (Character.isDigit(ch)) {
                hasNumber = true;
            }
            if (!Character.isLetterOrDigit(ch)) {
                hasSpecial = true;
            }
        }

        if (password.length() < 8 || !hasUppercase || !hasNumber || !hasSpecial) {
            return PASSWORD_REQUIREMENTS_MESSAGE;
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

    public static boolean hasMinimumSelectedInterests(List<String> interests) {
        return interests != null && interests.size() >= 3;
    }
}
