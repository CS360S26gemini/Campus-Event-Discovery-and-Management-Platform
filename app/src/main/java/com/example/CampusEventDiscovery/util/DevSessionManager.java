package com.example.CampusEventDiscovery.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Stores a local-only developer bypass session used when no Firebase user is signed in.
 */
public final class DevSessionManager {

    private static final String PREFS_NAME = "dev_session";
    private static final String KEY_BYPASS_ENABLED = "bypass_enabled";
    private static final String KEY_ROLE = "role";

    private DevSessionManager() {
    }

    public static void enableBypass(Context context, String role) {
        getPrefs(context).edit()
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
        return sanitizeRole(getPrefs(context).getString(KEY_ROLE, UserRoles.ATTENDEE));
    }

    public static String getEffectiveUserId(Context context) {
        if (!shouldUseBypass(context)) {
            return null;
        }

        String role = getBypassRole(context);
        if (UserRoles.isOrganizer(role)) {
            return "demo-organizer-user";
        }
        if (UserRoles.isAdmin(role)) {
            return "demo-admin-user";
        }
        return "demo-attendee-user";
    }

    public static String getDisplayName(Context context) {
        String role = getBypassRole(context);
        if (UserRoles.isOrganizer(role)) {
            return "Test Organizer";
        }
        if (UserRoles.isAdmin(role)) {
            return "Test Admin";
        }
        return "Test Attendee";
    }

    public static String getDisplayEmail(Context context) {
        String role = getBypassRole(context);
        if (UserRoles.isOrganizer(role)) {
            return "organizer@test.local";
        }
        if (UserRoles.isAdmin(role)) {
            return "admin@test.local";
        }
        return "attendee@test.local";
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String sanitizeRole(String role) {
        String sanitizedRole = UserRoles.sanitize(role);
        return sanitizedRole.isEmpty() ? UserRoles.ATTENDEE : sanitizedRole;
    }
}
