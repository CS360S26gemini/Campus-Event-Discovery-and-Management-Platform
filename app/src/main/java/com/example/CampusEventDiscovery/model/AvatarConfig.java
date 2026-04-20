package com.example.CampusEventDiscovery.model;

import com.example.CampusEventDiscovery.util.UserRoles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Serializable avatar settings stored on user profiles.
 */
public class AvatarConfig {

    public static final int SKIN_TONE_COUNT = 5;
    public static final int HAIR_STYLE_COUNT = 5;
    public static final int HAIR_COLOR_COUNT = 5;
    public static final int BACKGROUND_COUNT = 5;
    public static final int ACCESSORY_COUNT = 4;
    public static final int SHIRT_COLOR_COUNT = 5;

    private static final String KEY_SKIN_TONE = "skinTone";
    private static final String KEY_HAIR_STYLE = "hairStyle";
    private static final String KEY_HAIR_COLOR = "hairColor";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_ACCESSORY = "accessory";
    private static final String KEY_SHIRT_COLOR = "shirtColor";
    private static final String KEY_INITIALS = "initials";

    private int skinTone;
    private int hairStyle;
    private int hairColor;
    private int background;
    private int accessory;
    private int shirtColor;
    private String initials;

    public AvatarConfig() {
        this("CE", 0, 0, 0, 0, 0, 0);
    }

    public AvatarConfig(String initials,
                        int skinTone,
                        int hairStyle,
                        int hairColor,
                        int background,
                        int accessory,
                        int shirtColor) {
        this.initials = normalizeInitials(initials, "Campus Event");
        this.skinTone = clamp(skinTone, SKIN_TONE_COUNT);
        this.hairStyle = clamp(hairStyle, HAIR_STYLE_COUNT);
        this.hairColor = clamp(hairColor, HAIR_COLOR_COUNT);
        this.background = clamp(background, BACKGROUND_COUNT);
        this.accessory = clamp(accessory, ACCESSORY_COUNT);
        this.shirtColor = clamp(shirtColor, SHIRT_COLOR_COUNT);
    }

    public static AvatarConfig defaultsFor(String displayName, String role) {
        String safeRole = UserRoles.sanitize(role);
        int backgroundIndex = 0;
        int shirtIndex = 0;
        if (UserRoles.isOrganizer(safeRole)) {
            backgroundIndex = 2;
            shirtIndex = 2;
        } else if (UserRoles.isAdmin(safeRole)) {
            backgroundIndex = 3;
            shirtIndex = 3;
        }

        return new AvatarConfig(
                buildInitials(displayName),
                Math.abs(safeHash(displayName)) % SKIN_TONE_COUNT,
                Math.abs(safeHash(displayName + "hair")) % HAIR_STYLE_COUNT,
                Math.abs(safeHash(displayName + "color")) % HAIR_COLOR_COUNT,
                backgroundIndex,
                0,
                shirtIndex
        );
    }

    public static AvatarConfig fromMap(Map<String, Object> source, String displayName, String role) {
        AvatarConfig defaults = defaultsFor(displayName, role);
        if (source == null || source.isEmpty()) {
            return defaults;
        }

        defaults.setSkinTone(readIndex(source.get(KEY_SKIN_TONE), defaults.getSkinTone(), SKIN_TONE_COUNT));
        defaults.setHairStyle(readIndex(source.get(KEY_HAIR_STYLE), defaults.getHairStyle(), HAIR_STYLE_COUNT));
        defaults.setHairColor(readIndex(source.get(KEY_HAIR_COLOR), defaults.getHairColor(), HAIR_COLOR_COUNT));
        defaults.setBackground(readIndex(source.get(KEY_BACKGROUND), defaults.getBackground(), BACKGROUND_COUNT));
        defaults.setAccessory(readIndex(source.get(KEY_ACCESSORY), defaults.getAccessory(), ACCESSORY_COUNT));
        defaults.setShirtColor(readIndex(source.get(KEY_SHIRT_COLOR), defaults.getShirtColor(), SHIRT_COLOR_COUNT));
        defaults.setInitials(normalizeInitials(asString(source.get(KEY_INITIALS)), displayName));
        return defaults;
    }

    public AvatarConfig copy() {
        return new AvatarConfig(initials, skinTone, hairStyle, hairColor, background, accessory, shirtColor);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        values.put(KEY_SKIN_TONE, skinTone);
        values.put(KEY_HAIR_STYLE, hairStyle);
        values.put(KEY_HAIR_COLOR, hairColor);
        values.put(KEY_BACKGROUND, background);
        values.put(KEY_ACCESSORY, accessory);
        values.put(KEY_SHIRT_COLOR, shirtColor);
        values.put(KEY_INITIALS, initials);
        return values;
    }

    public int getSkinTone() {
        return skinTone;
    }

    public void setSkinTone(int skinTone) {
        this.skinTone = clamp(skinTone, SKIN_TONE_COUNT);
    }

    public int getHairStyle() {
        return hairStyle;
    }

    public void setHairStyle(int hairStyle) {
        this.hairStyle = clamp(hairStyle, HAIR_STYLE_COUNT);
    }

    public int getHairColor() {
        return hairColor;
    }

    public void setHairColor(int hairColor) {
        this.hairColor = clamp(hairColor, HAIR_COLOR_COUNT);
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = clamp(background, BACKGROUND_COUNT);
    }

    public int getAccessory() {
        return accessory;
    }

    public void setAccessory(int accessory) {
        this.accessory = clamp(accessory, ACCESSORY_COUNT);
    }

    public int getShirtColor() {
        return shirtColor;
    }

    public void setShirtColor(int shirtColor) {
        this.shirtColor = clamp(shirtColor, SHIRT_COLOR_COUNT);
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = normalizeInitials(initials, "Campus Event");
    }

    private static int readIndex(Object value, int fallback, int count) {
        if (value instanceof Number) {
            return clamp(((Number) value).intValue(), count);
        }
        if (value instanceof String) {
            try {
                return clamp(Integer.parseInt((String) value), count);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int clamp(int value, int count) {
        if (value < 0) {
            return 0;
        }
        if (value >= count) {
            return count - 1;
        }
        return value;
    }

    private static String normalizeInitials(String raw, String displayName) {
        String value = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9]", "").trim();
        if (value.isEmpty()) {
            value = buildInitials(displayName);
        }
        if (value.length() > 2) {
            value = value.substring(0, 2);
        }
        return value.toUpperCase(Locale.getDefault());
    }

    private static String buildInitials(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "CE";
        }

        String[] parts = displayName.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                builder.append(part.charAt(0));
            }
            if (builder.length() == 2) {
                break;
            }
        }

        if (builder.length() == 0) {
            return "CE";
        }
        return builder.toString().toUpperCase(Locale.getDefault());
    }

    private static int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
