package com.example.CampusEventDiscovery.util;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuthException;

public final class AuthErrorMessages {

    private AuthErrorMessages() {
    }

    public static String forSignIn(Exception exception) {
        String configurationMessage = configurationMessage(exception);
        if (configurationMessage != null) {
            return configurationMessage;
        }

        String code = getFirebaseErrorCode(exception);
        return forSignInCode(code);
    }

    public static String forSignInCode(String code) {
        if ("ERROR_INVALID_EMAIL".equals(code)) {
            return "Enter a valid email address.";
        }
        if ("ERROR_WRONG_PASSWORD".equals(code) || "ERROR_INVALID_PASSWORD".equals(code)) {
            return "Incorrect password. Please try again.";
        }
        if ("ERROR_INVALID_CREDENTIAL".equals(code)) {
            return "Email or password is incorrect. Please check both and try again.";
        }
        if ("ERROR_USER_NOT_FOUND".equals(code)) {
            return "No account found for this email address.";
        }
        if ("ERROR_USER_DISABLED".equals(code)) {
            return "This account has been disabled. Contact support for help.";
        }
        if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
            return "Too many sign-in attempts. Please wait a few minutes and try again.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
            return "Network error. Check your connection and try again.";
        }

        return "Sign in failed. Please check your details and try again.";
    }

    public static String forSignUp(Exception exception) {
        String configurationMessage = configurationMessage(exception);
        if (configurationMessage != null) {
            return configurationMessage;
        }

        String code = getFirebaseErrorCode(exception);
        return forSignUpCode(code);
    }

    public static String forSignUpCode(String code) {
        if ("ERROR_INVALID_EMAIL".equals(code)) {
            return "Enter a valid email address.";
        }
        if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
            return "An account already exists with this email. Try signing in instead.";
        }
        if ("ERROR_WEAK_PASSWORD".equals(code)) {
            return SignupValidator.PASSWORD_REQUIREMENTS_MESSAGE;
        }
        if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
            return "Email/password sign-up is not enabled right now.";
        }
        if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
            return "Too many sign-up attempts. Please wait a few minutes and try again.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
            return "Network error. Check your connection and try again.";
        }

        return "Sign up failed. Please review your details and try again.";
    }

    public static String forPasswordReset(Exception exception) {
        String code = getFirebaseErrorCode(exception);
        return forPasswordResetCode(code);
    }

    public static String forPasswordResetCode(String code) {
        if ("ERROR_INVALID_EMAIL".equals(code)) {
            return "Enter a valid email address.";
        }
        if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
            return "Too many reset attempts. Please wait a few minutes and try again.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
            return "Network error. Check your connection and try again.";
        }
        return "Could not send the reset email. Please try again.";
    }

    public static String forAccountUpdate(Exception exception) {
        String configurationMessage = configurationMessage(exception);
        if (configurationMessage != null) {
            return configurationMessage;
        }

        String code = getFirebaseErrorCode(exception);
        String mappedMessage = forAccountUpdateCode(code);
        if (mappedMessage != null) {
            return mappedMessage;
        }

        String message = exception == null ? null : exception.getMessage();
        return TextUtils.isEmpty(message) ? "Update failed. Please try again." : message;
    }

    public static String forAccountUpdateCode(String code) {
        if ("ERROR_WRONG_PASSWORD".equals(code) || "ERROR_INVALID_PASSWORD".equals(code)) {
            return "Current password is incorrect.";
        }
        if ("ERROR_INVALID_CREDENTIAL".equals(code)) {
            return "Current password is incorrect. Please try again.";
        }
        if ("ERROR_INVALID_EMAIL".equals(code)) {
            return "Enter a valid email address.";
        }
        if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
            return "That email is already used by another account.";
        }
        if ("ERROR_WEAK_PASSWORD".equals(code)) {
            return SignupValidator.PASSWORD_REQUIREMENTS_MESSAGE;
        }
        if ("ERROR_REQUIRES_RECENT_LOGIN".equals(code)) {
            return "For security, please sign in again before changing email or password.";
        }
        if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
            return "Too many attempts. Please wait a few minutes and try again.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
            return "Network error. Check your connection and try again.";
        }
        return null;
    }

    private static String getFirebaseErrorCode(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            return ((FirebaseAuthException) exception).getErrorCode();
        }
        return null;
    }

    private static String configurationMessage(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        if (message != null && message.contains("CONFIGURATION_NOT_FOUND")) {
            return "Firebase Authentication is not fully configured. Enable Email/Password in Firebase Console.";
        }
        return null;
    }
}
