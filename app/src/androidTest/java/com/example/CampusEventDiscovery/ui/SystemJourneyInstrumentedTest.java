package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SystemJourneyInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
    }

    @After
    public void tearDown() {
        DevSessionManager.clearBypass(context);
    }

    @Test
    public void organizerJourney_manageEventsShortcut_launchesManagedEventsScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);
        verifyLaunchFromMain(
                new Intent(context, MainActivity.class),
                R.id.btnManageEvents,
                ManageEventsActivity.class
        );
    }

    @Test
    public void organizerJourney_scannerShortcut_launchesScannerScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);
        verifyLaunchFromMain(
                new Intent(context, MainActivity.class),
                R.id.btnScanTickets,
                ScannerActivity.class
        );
    }

    @Test
    public void adminJourney_createEventShortcut_launchesProposalScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);
        verifyLaunchFromMain(
                new Intent(context, MainActivity.class),
                R.id.btnCreateEvent,
                CreateEventActivity.class
        );
    }

    @Test
    public void attendeeJourney_profileToNotifications_launchesNotificationCenter() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");
        verifyLaunchFromMain(intent, R.id.rowNotifications, NotificationCenterActivity.class, true);
    }

    @Test
    public void attendeeJourney_profileToMemories_launchesMemoriesScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");
        verifyLaunchFromMain(intent, R.id.rowMemories, MemoriesActivity.class, true);
    }

    @Test
    public void attendeeJourney_profileToCalendar_opensCalendarSurfaceInsideMain() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rowCalendar)).perform(scrollTo(), click());
            onView(withId(R.id.tvMonthLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.gridCalendarDays)).check(matches(isDisplayed()));
        }
    }

    private void verifyLaunchFromMain(Intent launchIntent, int clickId, Class<? extends Activity> targetActivity) {
        verifyLaunchFromMain(launchIntent, clickId, targetActivity, false);
    }

    private void verifyLaunchFromMain(Intent launchIntent, int clickId, Class<? extends Activity> targetActivity, boolean scrollFirst) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(targetActivity.getName(), null, false);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(launchIntent)) {
            if (scrollFirst) {
                onView(withId(clickId)).perform(scrollTo(), click());
            } else {
                onView(withId(clickId)).perform(click());
            }
            Activity launched = instrumentation.waitForMonitorWithTimeout(monitor, 5000L);
            org.junit.Assert.assertNotNull("Expected " + targetActivity.getSimpleName() + " to launch", launched);
            if (launched != null) {
                launched.finish();
            }
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }
}
