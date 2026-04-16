package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.CampusEventDiscovery.util.EventValidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * EventValidatorTest.java
 *
 * Unit tests for EventValidator.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class EventValidatorTest {

    private static final long FUTURE_TIME = System.currentTimeMillis() + 86_400_000L;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void validate_allValid_returnsNull() {
        String result = EventValidator.validate(
                "Career Fair 2025",
                "Annual career fair for LUMS students",
                "LUMS SSE Atrium",
                FUTURE_TIME,
                200,
                "Academic",
                "org123"
        );
        assertNull(result);
    }

    @Test
    public void validate_emptyTitle_returnsError() {
        String result = EventValidator.validate("", "Desc", "Loc", FUTURE_TIME, 50, "Social", "org123");
        assertNotNull("Should return error for empty title", result);
    }

    @Test
    public void validate_emptyDescription_returnsError() {
        String result = EventValidator.validate("Title", "", "Loc", FUTURE_TIME, 30, "Academic", "org123");
        assertNotNull("Should return error for empty description", result);
    }

    @Test
    public void validate_emptyLocation_returnsError() {
        String result = EventValidator.validate("Title", "Desc", "", FUTURE_TIME, 30, "Academic", "org123");
        assertNotNull("Should return error for empty location", result);
    }

    @Test
    public void validate_pastDate_returnsError() {
        long yesterday = System.currentTimeMillis() - 86_400_000L;
        String result = EventValidator.validate("Title", "Desc", "Loc", yesterday, 100, "Academic", "org123");
        assertNotNull("Should return error for past date", result);
    }

    @Test
    public void validate_zeroCapacity_returnsError() {
        String result = EventValidator.validate("Title", "Desc", "Loc", FUTURE_TIME, 0, "Social", "org123");
        assertNotNull("Should return error for zero capacity", result);
    }

    @Test
    public void validate_emptyCategory_returnsError() {
        String result = EventValidator.validate("Title", "Desc", "Loc", FUTURE_TIME, 50, "", "org123");
        assertNotNull("Should return error for empty category", result);
    }

    @Test
    public void validate_invalidCategory_returnsError() {
        String result = EventValidator.validate("Title", "Desc", "Loc", FUTURE_TIME, 50, "Invalid", "org123");
        assertNotNull("Should return error for invalid category", result);
    }

    @Test
    public void validate_missingOrganizerId_returnsError() {
        String result = EventValidator.validate("Title", "Desc", "Loc", FUTURE_TIME, 50, "Academic", "");
        assertNotNull("Should return error for missing organizer ID", result);
    }
}
