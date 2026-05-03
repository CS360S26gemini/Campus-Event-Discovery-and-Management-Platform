package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.profile.AccountSettingsActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminProfileInstrumentedTest {

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
    public void adminMainScreen_showsApprovalTogglesAndVendorRequests() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                org.junit.Assert.assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.tvAdminHeader).getVisibility());
                org.junit.Assert.assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.btnPendingApprovals).getVisibility());
                org.junit.Assert.assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.btnRejectedEvents).getVisibility());
                org.junit.Assert.assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.btnVendorRequests).getVisibility());
                org.junit.Assert.assertNotNull(activity.findViewById(R.id.rvAdminApprovals));
            });
        }
    }

    @Test
    public void adminMainScreen_toggleButtonsSwitchSelectionState() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnPendingApprovals)).check(matches(isSelected()));
            onView(withId(R.id.btnRejectedEvents)).check(matches(not(isSelected())));

            onView(withId(R.id.btnRejectedEvents)).perform(click());
            onView(withId(R.id.btnRejectedEvents)).check(matches(isSelected()));
            onView(withId(R.id.btnPendingApprovals)).check(matches(not(isSelected())));

            onView(withId(R.id.btnPendingApprovals)).perform(click());
            onView(withId(R.id.btnPendingApprovals)).check(matches(isSelected()));
            onView(withId(R.id.btnRejectedEvents)).check(matches(not(isSelected())));
        }
    }

    @Test
    public void adminVendorRequestsButton_opensVendorManagementSurface() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnVendorRequests)).perform(click());
            onView(withId(R.id.toolbarVendorManagement)).check(matches(isDisplayed()));
            onView(withId(R.id.btnVendorApproved)).check(matches(isDisplayed()));
            onView(withId(R.id.btnVendorPending)).check(matches(isDisplayed()));
            onView(withId(R.id.btnVendorRejected)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminApprovalReviewWalkthrough_showsReviewActions() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);
        Intent intent = new Intent(context, EventApprovalActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("proposalId", WalkthroughManager.getDemoProposal().getProposalId());

        try (ActivityScenario<EventApprovalActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarEventApproval)).check(matches(isDisplayed()));
            onView(withId(R.id.btnApprove)).check(matches(isDisplayed()));
            onView(withId(R.id.btnReject)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void attendeeProfileScreen_showsCoreNavigationRows() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.profileScrollView)).check(matches(isDisplayed()));
            onView(withId(R.id.rowMemories)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.rowCalendar)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.rowNotifications)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.rowAccountSettings)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void attendeeProfileCalendarRow_opensCalendarSurface() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rowCalendar)).perform(scrollTo(), click());
            onView(withId(R.id.tvMonthLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.gridCalendarDays)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void attendeeProfileRows_launchMemoriesNotificationsAndSettings() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);

        verifyProfileRowLaunchesActivity(R.id.rowMemories, MemoriesActivity.class);
        verifyProfileRowLaunchesActivity(R.id.rowNotifications, NotificationCenterActivity.class);
        verifyProfileRowLaunchesActivity(R.id.rowAccountSettings, AccountSettingsActivity.class);
    }

    private void verifyProfileRowLaunchesActivity(int rowId, Class<? extends Activity> targetActivity) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                targetActivity.getName(),
                null,
                false
        );

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(rowId)).perform(scrollTo(), click());
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
