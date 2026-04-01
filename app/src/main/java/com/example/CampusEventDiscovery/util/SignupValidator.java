package com.example.CampusEventDiscovery.util;

import android.text.TextUtils;
import android.util.Patterns;

public class SignupValidator {

    public static String validate(String name, String email, String password,
                                  String confirmPassword, String role) {
        if (TextUtils.isEmpty(name))
            return "Name is required";
        if (TextUtils.isEmpty(email))
            return "Email is required";
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Please enter a valid email address";
        if (TextUtils.isEmpty(password))
            return "Password is required";
        if (password.length() < 8)
            return "Password must be at least 8 characters";
        if (!password.matches(".*[A-Z].*"))
            return "Password must contain at least one uppercase letter";
        if (!password.matches(".*[a-z].*"))
            return "Password must contain at least one lowercase letter";
        if (!password.matches(".*\\d.*"))
            return "Password must contain at least one number";
        if (!password.matches(".*[^a-zA-Z0-9].*"))
            return "Password must contain at least one special character";
        if (TextUtils.isEmpty(confirmPassword))
            return "Please confirm your password";
        if (!password.equals(confirmPassword))
            return "Passwords do not match";
        if (TextUtils.isEmpty(role))
            return "Please select a role";
        return null;
    }
}