package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.CampusEventDiscovery.model.AvatarConfig;
import com.example.CampusEventDiscovery.util.UserRoles;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AvatarConfigTest {

    @Test
    public void defaultsFor_usesDisplayNameInitials() {
        AvatarConfig config = AvatarConfig.defaultsFor("Ammar Khan", UserRoles.ATTENDEE);

        assertEquals("AK", config.getInitials());
    }

    @Test
    public void fromMap_clampsInvalidIndexes() {
        Map<String, Object> values = new HashMap<>();
        values.put("skinTone", 99);
        values.put("hairStyle", -4);
        values.put("hairColor", 8);
        values.put("background", 42);
        values.put("accessory", 12);
        values.put("shirtColor", 9);
        values.put("initials", "campus");

        AvatarConfig config = AvatarConfig.fromMap(values, "Campus Event", UserRoles.ADMIN);

        assertEquals(AvatarConfig.SKIN_TONE_COUNT - 1, config.getSkinTone());
        assertEquals(0, config.getHairStyle());
        assertEquals(AvatarConfig.HAIR_COLOR_COUNT - 1, config.getHairColor());
        assertEquals(AvatarConfig.BACKGROUND_COUNT - 1, config.getBackground());
        assertEquals(AvatarConfig.ACCESSORY_COUNT - 1, config.getAccessory());
        assertEquals(AvatarConfig.SHIRT_COLOR_COUNT - 1, config.getShirtColor());
        assertEquals("CA", config.getInitials());
    }

    @Test
    public void toMap_roundTripsValues() {
        AvatarConfig original = new AvatarConfig("ZA", 1, 2, 3, 4, 1, 2);

        AvatarConfig copy = AvatarConfig.fromMap(original.toMap(), "Fallback", UserRoles.ORGANIZER);

        assertNotNull(copy.toMap());
        assertEquals(original.getInitials(), copy.getInitials());
        assertEquals(original.getSkinTone(), copy.getSkinTone());
        assertEquals(original.getHairStyle(), copy.getHairStyle());
        assertEquals(original.getHairColor(), copy.getHairColor());
        assertEquals(original.getBackground(), copy.getBackground());
        assertEquals(original.getAccessory(), copy.getAccessory());
        assertEquals(original.getShirtColor(), copy.getShirtColor());
    }
}
