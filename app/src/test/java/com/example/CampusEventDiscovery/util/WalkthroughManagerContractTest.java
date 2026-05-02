package com.example.CampusEventDiscovery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.R;

import java.util.List;

import org.junit.Test;

public class WalkthroughManagerContractTest {

    @Test
    public void walkthroughTargets_requestRevealBeforeOverlay() {
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", R.id.btnSubmitEvent));
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", R.id.etEventTitle));
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("search", R.id.btnSubmitEvent));
        assertFalse(WalkthroughManager.shouldAutoScrollBeforeOverlay("search", 0));
    }

    @Test
    public void guideRegistry_includesRoleSpecificFunctionality() {
        List<String> guideIds = WalkthroughManager.getGuideIds();

        assertTrue(guideIds.contains("attendee_favourites"));
        assertTrue(guideIds.contains("attendee_calendar"));
        assertTrue(guideIds.contains("attendee_feedback"));
        assertTrue(guideIds.contains("profile_settings"));
        assertTrue(guideIds.contains("organizer_attendees"));
        assertTrue(guideIds.contains("organizer_vendors"));
        assertTrue(guideIds.contains("admin_events"));
        assertTrue(guideIds.contains("admin_vendors"));
    }

    @Test
    public void allRegisteredGuides_haveRoutableTeachingSteps() {
        for (String guideId : WalkthroughManager.getGuideIds()) {
            List<WalkthroughManager.StepInfo> steps = WalkthroughManager.getStepInfoForGuide(guideId);
            assertFalse("Guide has no steps: " + guideId, steps.isEmpty());
            for (WalkthroughManager.StepInfo step : steps) {
                assertTrue("Unroutable screen in " + guideId + ": " + step.screen,
                        WalkthroughManager.canRouteScreen(step.screen));
                assertTrue("Missing target in " + guideId + " / " + step.screen,
                        step.targetId != 0);
                assertFalse("Missing title in " + guideId + " / " + step.screen,
                        step.title == null || step.title.trim().isEmpty());
                assertFalse("Missing body in " + guideId + " / " + step.screen,
                        step.body == null || step.body.trim().isEmpty());
            }
        }
    }
}
