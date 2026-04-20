package com.example.CampusEventDiscovery.util;

import java.util.Arrays;
import java.util.List;

/**
 * EventValidator.java
 *
 * Utility class to validate event proposal data before submission.
 */
public class EventValidator {

    private static final List<String> VALID_CATEGORIES = Arrays.asList(
            "Music", "Sports", "Career", "Academic", "Arts", "Business", "Food & Bev", "Social"
    );

    /**
     * Validates event fields and returns an error message if invalid, or null if valid.
     */
    public static String validate(
            String title,
            String description,
            String location,
            long timestamp,
            long capacity,
            String category,
            String organizerId
    ) {
        if (title == null || title.trim().isEmpty()) {
            return "Event title is required";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Event description is required";
        }
        if (location == null || location.trim().isEmpty()) {
            return "Event location is required";
        }
        if (timestamp <= System.currentTimeMillis()) {
            return "Event date must be in the future";
        }
        if (capacity < 1) {
            return "Capacity must be at least 1";
        }
        if (category == null || category.trim().isEmpty()) {
            return "Event category is required";
        }
        if (!VALID_CATEGORIES.contains(category)) {
            return "Invalid event category";
        }
        if (organizerId == null || organizerId.trim().isEmpty()) {
            return "Organizer ID is required";
        }

        return null;
    }
}
