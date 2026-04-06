package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.util.UserRoles;

import org.junit.Test;

/**
 * ExampleUnitTest.java
 *
 * Sanity-check tests that run purely on the JVM (no Android framework needed).
 * Kept minimal — detailed coverage lives in UserRolesTest and SignupValidatorTest.
 */
public class ExampleUnitTest {

    @Test
    public void userRoles_areSanitizedCorrectly() {
        assertEquals(UserRoles.ATTENDEE,  UserRoles.sanitize(" attendee "));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("ORGANIZER"));
        assertEquals(UserRoles.ADMIN,     UserRoles.sanitize("admin"));
        assertEquals("", UserRoles.sanitize("guest"));
        assertEquals("", UserRoles.sanitize(null));
    }

    @Test
    public void userRoles_isOrganizer_caseInsensitive() {
        assertTrue(UserRoles.isOrganizer("Organizer"));
        assertTrue(UserRoles.isOrganizer("ORGANIZER"));
        assertFalse(UserRoles.isOrganizer("attendee"));
    }

    @Test
    public void userRoles_isAdmin_trueForAdminOnly() {
        assertTrue(UserRoles.isAdmin("admin"));
        assertFalse(UserRoles.isAdmin("organizer"));
        assertFalse(UserRoles.isAdmin(null));
    }
}
