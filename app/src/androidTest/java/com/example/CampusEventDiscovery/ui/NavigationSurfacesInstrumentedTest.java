package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.WhoIsComingActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NavigationSurfacesInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);
    }

    @After
    public void tearDown() {
        DevSessionManager.clearBypass(context);
    }

    @Test
    public void organizerMainScreen_showsVendorTabAndShortcuts() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottomNavigationView);
                CharSequence vendorsTitle = bottomNavigationView
                        .getMenu()
                        .findItem(R.id.nav_favourites)
                        .getTitle();
                org.junit.Assert.assertEquals(
                        activity.getString(R.string.vendors),
                        String.valueOf(vendorsTitle)
                );
            });
            onView(withText(R.string.organizer_tools)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCreateEvent)).check(matches(isDisplayed()));
            onView(withId(R.id.btnManageEvents)).check(matches(isDisplayed()));
            onView(withId(R.id.btnScanTickets)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSosDashboard)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void manageEventsScreen_showsSearchAndSections() {
        try (ActivityScenario<ManageEventsActivity> ignored = ActivityScenario.launch(ManageEventsActivity.class)) {
            onView(withId(R.id.toolbarManageEvents)).check(matches(isDisplayed()));
            onView(withId(R.id.etManageEventsSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.actManageEventsFilter)).check(matches(isDisplayed()));
            onView(withId(R.id.tvManageEventsSubtitle)).check(matches(isDisplayed()));
            onView(withId(R.id.progressBarManageEvents)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void whoIsComingScreen_showsEventContextSearchAndActions() {
        Intent intent = new Intent(context, WhoIsComingActivity.class);
        intent.putExtra("eventId", "ui-test-event");
        intent.putExtra("eventTitle", "UI Test Event");

        try (ActivityScenario<WhoIsComingActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEventContext)).check(matches(isDisplayed()));
            onView(withId(R.id.etSearchParticipants)).check(matches(isDisplayed()));
            onView(withId(R.id.btnScanQr)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCheckIn)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBlacklist)).check(matches(isDisplayed()));
        }
    }
}
