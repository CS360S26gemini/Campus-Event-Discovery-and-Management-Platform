package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VendorManagementInstrumentedTest {

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
    public void organizerVendorTab_opensApprovedEventSelectionScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottomNavigationView)).perform(selectBottomNavItem(R.id.nav_favourites));
            onView(withText(R.string.vendor_management)).check(matches(isDisplayed()));
            onView(withText(R.string.select_event_for_vendors)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminVendorTab_opensVendorRequestsScreen() {
        DevSessionManager.enableBypass(context, UserRoles.ADMIN);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottomNavigationView)).perform(selectBottomNavItem(R.id.nav_favourites));
            onView(withText(R.string.vendor_requests)).check(matches(isDisplayed()));
            onView(withText(R.string.pending_vendors)).check(matches(isDisplayed()));
        }
    }

    private static ViewAction selectBottomNavItem(int itemId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(BottomNavigationView.class);
            }

            @Override
            public String getDescription() {
                return "Select bottom navigation item " + itemId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((BottomNavigationView) view).setSelectedItemId(itemId);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
