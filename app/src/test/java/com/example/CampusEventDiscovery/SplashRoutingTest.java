package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class SplashRoutingTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        DevSessionManager.clearBypass(context);
    }

    @Test
    public void splash_withDeveloperBypass_routesToMainActivity() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);

        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).setup().get();
        ShadowLooper.idleMainLooper(1600, TimeUnit.MILLISECONDS);

        Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(MainActivity.class.getName(), nextIntent.getComponent().getClassName());
    }
}
