package com.example.campuseventdiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.campuseventdiscovery.util.UserRoles;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void userRoles_areSanitizedCorrectly() {
        assertEquals(UserRoles.ATTENDEE, UserRoles.sanitize(" attendee "));
        assertEquals(UserRoles.ORGANIZER, UserRoles.sanitize("ORGANIZER"));
        assertEquals(UserRoles.ADMIN, UserRoles.sanitize("admin"));
        assertEquals("", UserRoles.sanitize("guest"));
        assertTrue(UserRoles.isOrganizer("Organizer"));
    }
}
