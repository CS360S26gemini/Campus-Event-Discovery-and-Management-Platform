package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.SosAlert;

import org.junit.Before;
import org.junit.Test;

/**
 * SosAlertTest.java
 *
 * Pure-JVM unit tests for the {@link SosAlert} model. No Android framework or
 * Firebase dependencies are touched — only getters, setters, and constructors
 * are exercised.
 */
public class SosAlertTest {

    private static final double DELTA = 0.000001;

    private SosAlert alert;

    @Before
    public void setUp() {
        alert = new SosAlert();
    }

    /** Verifies the no-arg constructor leaves every field at its Java default. */
    @Test
    public void testDefaultConstructor_allFieldsNullOrZero() {
        SosAlert fresh = new SosAlert();
        assertNull(fresh.getUserId());
        assertNull(fresh.getDisplayName());
        assertNull(fresh.getEventId());
        assertNull(fresh.getEventName());
        assertNull(fresh.getOrganizerId());
        assertNull(fresh.getMapsUrl());
        assertNull(fresh.getStatus());
        assertEquals(0.0, fresh.getLatitude(), DELTA);
        assertEquals(0.0, fresh.getLongitude(), DELTA);
        assertEquals(0L, fresh.getTimestamp());
    }

    /** Verifies the full constructor assigns every argument to the matching field. */
    @Test
    public void testFullConstructor_allFieldsSet() {
        long ts = 1_700_000_000_000L;
        SosAlert full = new SosAlert(
                "uid-1",
                "Jane Doe",
                "event-42",
                "Spring Gala",
                "org-7",
                31.4808,
                74.3035,
                "https://maps.google.com/?q=31.4808,74.3035",
                ts,
                "ACTIVE");

        assertEquals("uid-1", full.getUserId());
        assertEquals("Jane Doe", full.getDisplayName());
        assertEquals("event-42", full.getEventId());
        assertEquals("Spring Gala", full.getEventName());
        assertEquals("org-7", full.getOrganizerId());
        assertEquals(31.4808, full.getLatitude(), DELTA);
        assertEquals(74.3035, full.getLongitude(), DELTA);
        assertEquals("https://maps.google.com/?q=31.4808,74.3035", full.getMapsUrl());
        assertEquals(ts, full.getTimestamp());
        assertEquals("ACTIVE", full.getStatus());
    }

    /** Verifies every setter/getter pair round-trips the written value unchanged. */
    @Test
    public void testSettersAndGetters_roundTrip() {
        alert.setUserId("uid-99");
        alert.setDisplayName("Ali Khan");
        alert.setEventId("event-xyz");
        alert.setEventName("Tech Fest");
        alert.setOrganizerId("organizer-55");
        alert.setLatitude(12.9716);
        alert.setLongitude(77.5946);
        alert.setMapsUrl("https://maps.google.com/?q=12.9716,77.5946");
        alert.setTimestamp(1_650_000_000_000L);
        alert.setStatus("ACTIVE");

        assertEquals("uid-99", alert.getUserId());
        assertEquals("Ali Khan", alert.getDisplayName());
        assertEquals("event-xyz", alert.getEventId());
        assertEquals("Tech Fest", alert.getEventName());
        assertEquals("organizer-55", alert.getOrganizerId());
        assertEquals(12.9716, alert.getLatitude(), DELTA);
        assertEquals(77.5946, alert.getLongitude(), DELTA);
        assertEquals("https://maps.google.com/?q=12.9716,77.5946", alert.getMapsUrl());
        assertEquals(1_650_000_000_000L, alert.getTimestamp());
        assertEquals("ACTIVE", alert.getStatus());
    }

    /** Verifies a stored mapsUrl preserves both the latitude and longitude strings. */
    @Test
    public void testMapsUrl_format() {
        alert.setLatitude(31.4808);
        alert.setLongitude(74.3035);
        alert.setMapsUrl("https://maps.google.com/?q=31.4808,74.3035");

        String url = alert.getMapsUrl();
        assertTrue("mapsUrl should contain latitude", url.contains("31.4808"));
        assertTrue("mapsUrl should contain longitude", url.contains("74.3035"));
    }

    /** Verifies ACTIVE is stored verbatim — this is the status written on creation. */
    @Test
    public void testStatus_activeValue() {
        alert.setStatus("ACTIVE");
        assertTrue(alert.getStatus().equals("ACTIVE"));
    }

    /** Verifies RESOLVED is stored verbatim — the terminal status set by responders. */
    @Test
    public void testStatus_resolvedValue() {
        alert.setStatus("RESOLVED");
        assertTrue(alert.getStatus().equals("RESOLVED"));
    }

    /** Verifies the Unknown User fallback survives a setter round-trip. */
    @Test
    public void testDisplayName_unknownUserFallback() {
        alert.setDisplayName("Unknown User");
        assertTrue(alert.getDisplayName().equals("Unknown User"));
    }

    /** Verifies a real-wall-clock timestamp round-trips as a positive long. */
    @Test
    public void testTimestamp_isPositive() {
        alert.setTimestamp(System.currentTimeMillis());
        assertTrue("timestamp should be positive", alert.getTimestamp() > 0L);
    }

    /** Verifies 0.0/0.0 — the GPS-unavailable fallback — survives setter round-trip. */
    @Test
    public void testCoordinates_zeroFallback() {
        alert.setLatitude(0.0);
        alert.setLongitude(0.0);
        assertEquals(0.0, alert.getLatitude(), DELTA);
        assertEquals(0.0, alert.getLongitude(), DELTA);
    }

    /** Verifies an empty-string organizerId is valid — used when no organizer is found. */
    @Test
    public void testOrganizerId_emptyString() {
        alert.setOrganizerId("");
        assertTrue(alert.getOrganizerId().equals(""));
    }
}
