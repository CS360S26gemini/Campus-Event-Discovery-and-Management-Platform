package com.example.CampusEventDiscovery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.R;

import org.junit.Test;

public class WalkthroughManagerContractTest {

    @Test
    public void createEventSubmitStep_requiresAutoScroll() {
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", R.id.btnSubmitEvent));
        assertFalse(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", R.id.etEventTitle));
        assertFalse(WalkthroughManager.shouldAutoScrollBeforeOverlay("search", R.id.btnSubmitEvent));
    }
}
