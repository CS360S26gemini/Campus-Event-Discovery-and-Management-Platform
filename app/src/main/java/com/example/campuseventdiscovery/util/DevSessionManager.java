package com.example.campuseventdiscovery.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Stores a local role-based bypass session so the app can be exercised
 * without Firebase Authentication during development.
 */
public final class DevSessionManager {

    private static final String PREFS_NAME = "dev_session";
    private static final String KEY_BYPASS_ENABLED = "bypass_enabled";
    private static final String KEY_ROLE = "role";

    private DevSessionManager() {
    }

    public static void enableBypass(Context context, String role) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putBoolean(KEY_BYPASS_ENABLED, true)
                .putString(KEY_ROLE, sanitizeRole(role))
                .apply();
    }

    public static void clearBypass(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static boolean isBypassEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_BYPASS_ENABLED, false);
    }

    public static boolean shouldUseBypass(Context context) {
        return isBypassEnabled(context) && FirebaseAuth.getInstance().getCurrentUser() == null;
    }

    public static String getBypassRole(Context context) {
        String role = getPrefs(context).getString(KEY_ROLE, UserRoles.ATTENDEE);
        return sanitizeRole(role);
    }

    public static String getEffectiveUserId(Context context) {
        if (!shouldUseBypass(context)) {
            return null;
        }

        if (UserRoles.isOrganizer(getBypassRole(context))) {
            return "demo-organizer-user";
        }

        if (UserRoles.isAdmin(getBypassRole(context))) {
            return "demo-admin-user";
        }

        return "demo-attendee-user";
    }

    public static String getDisplayName(Context context) {
        if (UserRoles.isOrganizer(getBypassRole(context))) {
            return "Test Organizer";
        }

        if (UserRoles.isAdmin(getBypassRole(context))) {
            return "Test Admin";
        }

        return "Test Attendee";
    }

    public static String getDisplayEmail(Context context) {
        if (UserRoles.isOrganizer(getBypassRole(context))) {
            return "organizer@test.local";
        }

        if (UserRoles.isAdmin(getBypassRole(context))) {
            return "admin@test.local";
        }

        return "attendee@test.local";
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String sanitizeRole(String role) {
        String safeRole = UserRoles.sanitize(role);
        return TextUtils.isEmpty(safeRole) ? UserRoles.ATTENDEE : safeRole;
    }
}
