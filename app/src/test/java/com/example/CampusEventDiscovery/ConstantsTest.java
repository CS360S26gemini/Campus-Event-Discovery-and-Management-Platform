package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.util.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * ConstantsTest.java
 *
 * Locks the Firestore collection names and payment-status enum values
 * so any accidental rename breaks the test suite immediately.
 * The existing Level 1 collection names (users/events/rsvps) are also
 * non-renameable per Sprint Manual §8.1.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class ConstantsTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void collectionUsers_isLowercaseUsers() {
        assertEquals("users", Constants.COLLECTION_USERS);
    }

    @Test
    public void collectionEvents_isLowercaseEvents() {
        assertEquals("events", Constants.COLLECTION_EVENTS);
    }

    @Test
    public void collectionRsvps_isLowercaseRsvps() {
        assertEquals("rsvps", Constants.COLLECTION_RSVPS);
    }

    @Test
    public void collectionPayments_isLowercasePayments() {
        assertEquals("payments", Constants.COLLECTION_PAYMENTS);
    }

    @Test
    public void collectionSosAlerts_isSnakeCase() {
        assertEquals("sos_alerts", Constants.COLLECTION_SOS_ALERTS);
    }

    @Test
    public void paymentStatuses_matchSpecExactly() {
        assertEquals("PENDING",   Constants.PAYMENT_PENDING);
        assertEquals("CONFIRMED", Constants.PAYMENT_CONFIRMED);
        assertEquals("REJECTED",  Constants.PAYMENT_REJECTED);
    }

    @Test
    public void paymentStatuses_areAllDistinct() {
        assertNotEquals(Constants.PAYMENT_PENDING, Constants.PAYMENT_CONFIRMED);
        assertNotEquals(Constants.PAYMENT_CONFIRMED, Constants.PAYMENT_REJECTED);
        assertNotEquals(Constants.PAYMENT_PENDING, Constants.PAYMENT_REJECTED);
    }

    @Test
    public void paymentStatuses_doNotLeakLegacySuccessValue() {
        assertFalse("SUCCESS".equals(Constants.PAYMENT_PENDING));
        assertFalse("SUCCESS".equals(Constants.PAYMENT_CONFIRMED));
        assertFalse("SUCCESS".equals(Constants.PAYMENT_REJECTED));
    }

    @Test
    public void reminderRoutingConstants_matchSpec() {
        assertEquals("destinationTab", Constants.EXTRA_DESTINATION_TAB);
        assertEquals("calendar", Constants.DESTINATION_TAB_CALENDAR);
    }

    @Test
    public void constantsClass_cannotBeInstantiated() throws Exception {
        Constructor<Constants> c = Constants.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", Modifier.isPrivate(c.getModifiers()));
        c.setAccessible(true);
        try {
            c.newInstance();
            // Allowed to succeed via reflection, but private modifier is the contract.
        } catch (Exception ignored) {
            // Fine â€” throwing is also acceptable.
        }
    }

    @Test
    public void constantsClass_isFinal() {
        assertTrue("Constants must be final",
                Modifier.isFinal(Constants.class.getModifiers()));
    }
}
