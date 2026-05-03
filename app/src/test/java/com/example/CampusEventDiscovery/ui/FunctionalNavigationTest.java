package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
public class FunctionalNavigationTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
        DevSessionManager.enableBypass(context, UserRoles.ATTENDEE);
    }

    @After
    public void tearDown() {
        DevSessionManager.clearBypass(context);
    }

    @Test
    public void attendeeSearchScreen_showsSearchFiltersAndSort() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottomNavigationView);
                nav.setSelectedItemId(R.id.nav_search);
            });

            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.chipMusic)).check(matches(isDisplayed()));
            onView(withId(R.id.tvSortBy)).check(matches(isDisplayed()));
            onView(withId(R.id.rvResults)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void attendeeFavouritesScreen_showsSavedEventsSurface() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottomNavigationView);
                nav.setSelectedItemId(R.id.nav_favourites);
            });

            onView(withId(R.id.tvFavouritesBadge)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void calendarScreen_opensFromMainActivityAndShowsMonthControls() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(MainActivity::openCalendarScreen);

            onView(withId(R.id.tvMonthLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.gridCalendarDays)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnPrevMonth)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnNextMonth)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }
}
