package com.example.CampusEventDiscovery.util;

import java.util.Locale;

/**
 * Shared helpers for role-based access decisions.
 */
public final class UserRoles {

    public static final String ATTENDEE = "attendee";
    public static final String ORGANIZER = "organizer";
    public static final String ADMIN = "admin";

    private UserRoles() {
    }

    public static String sanitize(String role) {
        if (role == null) {
            return "";
        }

        String normalized = role.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case ATTENDEE:
            case ORGANIZER:
            case ADMIN:
                return normalized;
            default:
                return "";
        }
    }

    public static boolean isAttendee(String role) {
        return ATTENDEE.equals(sanitize(role));
    }

    public static boolean isOrganizer(String role) {
        return ORGANIZER.equals(sanitize(role));
    }

    public static boolean isAdmin(String role) {
        return ADMIN.equals(sanitize(role));
    }

    public static boolean canManageEvents(String role) {
        String safeRole = sanitize(role);
        return ORGANIZER.equals(safeRole) || ADMIN.equals(safeRole);
    }

    public static boolean canUseAdminPowers(String role) {
        return ADMIN.equals(sanitize(role));
    }
}
