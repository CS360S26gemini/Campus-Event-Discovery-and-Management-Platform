package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {30})
public class AdminProfileFunctionalTest {

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
    public void adminHomeScreen_showsApprovalAndVendorSurfaces() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnPendingApprovals)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnRejectedEvents)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnVendorRequests)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rvAdminApprovals)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void attendeeProfileScreen_showsProfileNavigationRows() {
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottomNavigationView);
                nav.setSelectedItemId(R.id.nav_profile);
            });

            onView(withId(R.id.profileScrollView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowMemories)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowCalendar)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowNotifications)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowAccountSettings)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void organizerProfileScreen_showsOrganizerTools() {
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottomNavigationView);
                nav.setSelectedItemId(R.id.nav_profile);
            });

            onView(withId(R.id.sectionOrganizerTools)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowManageEvents)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowCreateEvent)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowScanTickets)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rowSosDashboard)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }
}
