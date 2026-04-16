package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.util.UserRoles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * UserRolesTest.java
 *
 * Tests for UserRoles utility: role constants, sanitization (trim + lowercase),
 * and role-check helpers (isAttendee, isOrganizer, isAdmin).
 */
public class UserRolesTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    // ─── SANITIZE ────────────────────────────────────────────────────────────

    @Test
    public void testSanitize_uppercase() {
        assertEquals(UserRoles.ATTENDEE,  UserRoles.sanitize("ATTENDEE"));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("ORGANIZER"));
        assertEquals(UserRoles.ADMIN,     UserRoles.sanitize("ADMIN"));
    }

    @Test
    public void testSanitize_mixedCase() {
        assertEquals(UserRoles.ATTENDEE,  UserRoles.sanitize("Attendee"));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("Organizer"));
        assertEquals(UserRoles.ADMIN,     UserRoles.sanitize("Admin"));
    }

    @Test
    public void testSanitize_withWhitespace() {
        assertEquals(UserRoles.ATTENDEE,  UserRoles.sanitize("  attendee  "));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("  organizer  "));
        assertEquals(UserRoles.ADMIN,     UserRoles.sanitize("  admin  "));
    }

    @Test
    public void testSanitize_null_returnsEmpty() {
        assertEquals("", UserRoles.sanitize(null));
    }

    @Test
    public void testSanitize_unknown_returnsEmpty() {
        assertEquals("", UserRoles.sanitize("guest"));
        assertEquals("", UserRoles.sanitize("superuser"));
        assertEquals("", UserRoles.sanitize(""));
    }

    @Test
    public void testSanitize_emptyString_returnsEmpty() {
        assertEquals("", UserRoles.sanitize(""));
    }

    // ─── IS ATTENDEE ─────────────────────────────────────────────────────────

    @Test
    public void testIsAttendee_lowercase() {
        assertTrue(UserRoles.isAttendee("attendee"));
    }

    @Test
    public void testIsAttendee_uppercase() {
        assertTrue(UserRoles.isAttendee("ATTENDEE"));
    }

    @Test
    public void testIsAttendee_withSpaces() {
        assertTrue(UserRoles.isAttendee(" ATTENDEE "));
    }

    @Test
    public void testIsAttendee_organizerRole_returnsFalse() {
        assertFalse(UserRoles.isAttendee("organizer"));
    }

    @Test
    public void testIsAttendee_null_returnsFalse() {
        assertFalse(UserRoles.isAttendee(null));
    }

    @Test
    public void testIsAttendee_emptyString_returnsFalse() {
        assertFalse(UserRoles.isAttendee(""));
    }

    // ─── IS ORGANIZER ────────────────────────────────────────────────────────

    @Test
    public void testIsOrganizer_lowercase() {
        assertTrue(UserRoles.isOrganizer("organizer"));
    }

    @Test
    public void testIsOrganizer_uppercase() {
        assertTrue(UserRoles.isOrganizer("ORGANIZER"));
    }

    @Test
    public void testIsOrganizer_adminRole_returnsFalse() {
        assertFalse(UserRoles.isOrganizer("admin"));
    }

    @Test
    public void testIsOrganizer_attendeeRole_returnsFalse() {
        assertFalse(UserRoles.isOrganizer("attendee"));
    }

    @Test
    public void testIsOrganizer_null_returnsFalse() {
        assertFalse(UserRoles.isOrganizer(null));
    }

    // ─── IS ADMIN ────────────────────────────────────────────────────────────

    @Test
    public void testIsAdmin_lowercase() {
        assertTrue(UserRoles.isAdmin("admin"));
    }

    @Test
    public void testIsAdmin_uppercaseWithSpaces() {
        assertTrue(UserRoles.isAdmin(" ADMIN "));
    }

    @Test
    public void testIsAdmin_attendeeRole_returnsFalse() {
        assertFalse(UserRoles.isAdmin("attendee"));
    }

    @Test
    public void testIsAdmin_organizerRole_returnsFalse() {
        assertFalse(UserRoles.isAdmin("organizer"));
    }

    @Test
    public void testIsAdmin_null_returnsFalse() {
        assertFalse(UserRoles.isAdmin(null));
    }

    // ─── ROLE CONSTANTS ──────────────────────────────────────────────────────

    @Test
    public void testRoleConstants_areCorrectStrings() {
        assertEquals("attendee",  UserRoles.ATTENDEE);
        assertEquals("organizer", UserRoles.ORGANIZER);
        assertEquals("admin",     UserRoles.ADMIN);
    }

    // ─── MUTUAL EXCLUSIVITY ──────────────────────────────────────────────────

    /** No role string should satisfy more than one is-check at once. */
    @Test
    public void testRoles_areMutuallyExclusive() {
        assertFalse(UserRoles.isOrganizer(UserRoles.ATTENDEE));
        assertFalse(UserRoles.isAdmin(UserRoles.ATTENDEE));
        assertFalse(UserRoles.isAttendee(UserRoles.ORGANIZER));
        assertFalse(UserRoles.isAdmin(UserRoles.ORGANIZER));
        assertFalse(UserRoles.isAttendee(UserRoles.ADMIN));
        assertFalse(UserRoles.isOrganizer(UserRoles.ADMIN));
    }
}
