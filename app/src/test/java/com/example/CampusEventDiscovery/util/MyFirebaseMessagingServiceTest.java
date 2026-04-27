package com.example.CampusEventDiscovery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * MyFirebaseMessagingServiceTest.java
 *
 * Pure JVM coverage for the reminder copy and deep-link payload used by the
 * FCM reminder branch.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class MyFirebaseMessagingServiceTest {

    @Test
    public void buildReminderTitle_threeDaysRemaining_matchesSpec() {
        assertEquals("3 Days left to Hack Night",
                MyFirebaseMessagingService.buildReminderTitle("Hack Night", 3, "8:00 AM"));
    }

    @Test
    public void buildReminderBody_dayOf_includesStartTime() {
        assertEquals("Hack Night commencing at 8:00 AM",
                MyFirebaseMessagingService.buildReminderBody("Hack Night", 0, "8:00 AM"));
    }

    @Test
    public void createReminderIntent_routesToCalendarTab() {
        Intent intent = MyFirebaseMessagingService.createReminderIntent(
                org.robolectric.RuntimeEnvironment.getApplication());

        assertEquals(Constants.DESTINATION_TAB_CALENDAR,
                intent.getStringExtra(Constants.EXTRA_DESTINATION_TAB));
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
    }
}
