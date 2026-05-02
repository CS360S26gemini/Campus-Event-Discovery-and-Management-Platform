package com.example.CampusEventDiscovery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WalkthroughManagerInstrumentedTest {

    @Test
    public void walkthroughCompletion_routesToHelpAndSupport() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = WalkthroughManager.createCompletionIntent(context);

        assertTrue(intent.getBooleanExtra("OPEN_HELP", false));
        assertFalse(intent.hasExtra("OPEN_TAB"));
    }

    @Test
    public void allWalkthroughGuides_haveSteps_andBackActionExists() throws Exception {
        Method buildSteps = WalkthroughManager.class.getDeclaredMethod("buildSteps", String.class);
        buildSteps.setAccessible(true);

        for (String guideId : WalkthroughManager.getGuideIds()) {
            @SuppressWarnings("unchecked")
            List<?> steps = (List<?>) buildSteps.invoke(null, guideId);
            assertTrue("Guide " + guideId + " should contain at least one step", steps != null && !steps.isEmpty());
        }

        assertTrue(WalkthroughManager.shouldShowBackAction(0));
        assertTrue(WalkthroughManager.shouldShowBackAction(1));
        assertTrue("First step should use exit label",
                WalkthroughManager.resolveBackButtonLabel(ApplicationProvider.getApplicationContext(), 0)
                        .equals(ApplicationProvider.getApplicationContext().getString(com.example.CampusEventDiscovery.R.string.walkthrough_exit)));
        assertTrue("Subsequent steps should use back label",
                WalkthroughManager.resolveBackButtonLabel(ApplicationProvider.getApplicationContext(), 1)
                        .equals(ApplicationProvider.getApplicationContext().getString(com.example.CampusEventDiscovery.R.string.walkthrough_back)));
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", com.example.CampusEventDiscovery.R.id.btnSubmitEvent));
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("create_event", com.example.CampusEventDiscovery.R.id.etEventTitle));
        assertTrue(WalkthroughManager.shouldAutoScrollBeforeOverlay("search", com.example.CampusEventDiscovery.R.id.btnSubmitEvent));
        assertFalse(WalkthroughManager.shouldAutoScrollBeforeOverlay("search", 0));
    }
}
