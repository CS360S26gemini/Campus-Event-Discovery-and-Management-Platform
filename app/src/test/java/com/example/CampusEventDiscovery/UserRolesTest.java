package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.util.UserRoles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class UserRolesTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.print("RUNNING: " + description.getMethodName() + " ... ");
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("PASS");
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("FAIL (" + e.getMessage() + ")");
        }
    };

    @Test
    public void testSanitize() {
        assertEquals(UserRoles.ATTENDEE, UserRoles.sanitize("ATTENDEE"));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("  organizer  "));
        assertEquals(UserRoles.ADMIN, UserRoles.sanitize("Admin"));
        assertEquals("", UserRoles.sanitize(null));
        assertEquals("", UserRoles.sanitize("unknown"));
    }

    @Test
    public void testIsAttendee() {
        assertTrue(UserRoles.isAttendee("attendee"));
        assertTrue(UserRoles.isAttendee(" ATTENDEE "));
        assertFalse(UserRoles.isAttendee("organizer"));
        assertFalse(UserRoles.isAttendee(null));
    }

    @Test
    public void testIsOrganizer() {
        assertTrue(UserRoles.isOrganizer("organizer"));
        assertTrue(UserRoles.isOrganizer("ORGANIZER"));
        assertFalse(UserRoles.isOrganizer("admin"));
    }

    @Test
    public void testIsAdmin() {
        assertTrue(UserRoles.isAdmin("admin"));
        assertTrue(UserRoles.isAdmin(" ADMIN "));
        assertFalse(UserRoles.isAdmin("attendee"));
    }
}
