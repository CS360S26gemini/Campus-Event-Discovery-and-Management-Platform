package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.VendorProposal;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.util.WalkthroughManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class WalkthroughIntegrationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void memoriesAggregation_mergesStoredPhotosAndBackfillsRsvpMetadata() throws Exception {
        MemoriesActivity activity = Robolectric.buildActivity(MemoriesActivity.class).setup().get();

        Field registeredRsvpsField = MemoriesActivity.class.getDeclaredField("registeredRsvps");
        registeredRsvpsField.setAccessible(true);
        List<Rsvp> registeredRsvps = (List<Rsvp>) registeredRsvpsField.get(activity);

        Event demoEvent = WalkthroughManager.getDemoEvent();
        Rsvp rsvp = new Rsvp();
        rsvp.setEventId(demoEvent.getEventId());
        rsvp.setTitle(demoEvent.getTitle());
        rsvp.setDate(demoEvent.getDate());
        registeredRsvps.clear();
        registeredRsvps.add(rsvp);

        Memory first = new Memory(demoEvent.getEventId(), "", new ArrayList<>(Arrays.asList("a.jpg", "b.jpg")), null, 0);
        Memory second = new Memory(demoEvent.getEventId(), "Legacy title", new ArrayList<>(Arrays.asList("b.jpg", "c.jpg")), null, 4);

        Method buildMemoryAlbums = MemoriesActivity.class.getDeclaredMethod("buildMemoryAlbums", List.class);
        buildMemoryAlbums.setAccessible(true);

        List<Memory> result = (List<Memory>) buildMemoryAlbums.invoke(activity, Arrays.asList(first, second));

        assertEquals(1, result.size());
        Memory album = result.get(0);
        assertEquals(demoEvent.getEventId(), album.getEventId());
        assertEquals(demoEvent.getTitle(), album.getEventTitle());
        assertEquals(demoEvent.getDate(), album.getAttendedAt());
        assertEquals(4, album.getRating());
        assertEquals(Arrays.asList("a.jpg", "b.jpg", "c.jpg"), album.getPhotoUrls());
    }

    @Test
    public void walkthroughNotifications_referenceDemoEventAndMixReadStates() {
        Event demoEvent = WalkthroughManager.getDemoEvent();
        List<Notification> notifications = WalkthroughManager.getDemoNotifications();

        assertEquals(2, notifications.size());
        assertEquals(demoEvent.getEventId(), notifications.get(0).getEventId());
        assertEquals(demoEvent.getEventId(), notifications.get(1).getEventId());
        assertFalse(notifications.get(0).isRead());
        assertTrue(notifications.get(1).isRead());
        assertEquals("event", notifications.get(0).getType());
        assertEquals("sos", notifications.get(1).getType());
    }

    @Test
    public void walkthroughVendorProposals_alignWithDemoEventContext() {
        Event demoEvent = WalkthroughManager.getDemoEvent();
        List<VendorProposal> proposals = WalkthroughManager.getDemoVendorProposals();

        assertEquals(2, proposals.size());
        for (VendorProposal proposal : proposals) {
            assertEquals(demoEvent.getEventId(), proposal.getEventId());
            assertEquals(demoEvent.getTitle(), proposal.getEventTitle());
            assertNotNull(proposal.getVendorName());
            assertNotNull(proposal.getOrganizerId());
            assertNotNull(proposal.getCreatedAt());
        }

        assertEquals("pending", proposals.get(0).getStatus());
        assertEquals("approved", proposals.get(1).getStatus());
    }
}
