package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class PerformanceSmokeTest {

    @Test
    public void walkthroughDemoData_generationStaysFast() {
        long start = System.nanoTime();
        int notificationCount = 0;
        for (int i = 0; i < 500; i++) {
            notificationCount += WalkthroughManager.getDemoNotifications().size();
            notificationCount += WalkthroughManager.getDemoEvents().size();
            notificationCount += WalkthroughManager.getDemoVendorProposals().size();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(2500, notificationCount);
        assertTrue("Walkthrough demo data generation should stay comfortably under 2s", elapsedMs < 2000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void memoriesAggregation_mediumDatasetStaysFast() throws Exception {
        MemoriesActivity activity = Robolectric.buildActivity(MemoriesActivity.class).setup().get();

        Field registeredRsvpsField = MemoriesActivity.class.getDeclaredField("registeredRsvps");
        registeredRsvpsField.setAccessible(true);
        List<Rsvp> rsvps = (List<Rsvp>) registeredRsvpsField.get(activity);

        for (int i = 0; i < 120; i++) {
            Rsvp rsvp = new Rsvp();
            rsvp.setEventId("event-" + i);
            rsvp.setTitle("Event " + i);
            rsvp.setDate(WalkthroughManager.getDemoEvent().getDate());
            rsvps.add(rsvp);
        }

        List<Memory> stored = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            List<String> photos = new ArrayList<>();
            photos.add("photo-" + i + "-a");
            photos.add("photo-" + i + "-b");
            stored.add(new Memory("event-" + i, "", photos, null, i % 5));
        }

        Method buildMemoryAlbums = MemoriesActivity.class.getDeclaredMethod("buildMemoryAlbums", List.class);
        buildMemoryAlbums.setAccessible(true);

        long start = System.nanoTime();
        List<Memory> result = (List<Memory>) buildMemoryAlbums.invoke(activity, stored);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(120, result.size());
        assertTrue("Memory aggregation should stay comfortably under 2s", elapsedMs < 2000L);
    }

    @Test
    public void roleSanitization_hotLoopStaysFast() {
        long start = System.nanoTime();
        String last = "";
        for (int i = 0; i < 100_000; i++) {
            last = UserRoles.sanitize(i % 2 == 0 ? " ADMIN " : "unknown-role");
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals("", last);
        assertTrue("Role sanitization loop should stay comfortably under 2s", elapsedMs < 2000L);
    }
}
