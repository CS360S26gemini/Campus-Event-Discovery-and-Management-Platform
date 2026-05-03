package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.event.PaymentConfirmationActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.profile.AccountSettingsActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoryAlbumActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.firebase.auth.FirebaseAuth;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UtilityScreensInstrumentedTest {

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
    public void scannerScreen_showsResultCardPromptAndCameraAction() {
        Intent intent = new Intent(context, ScannerActivity.class);
        intent.putExtra("eventId", "ui-test-event");
        intent.putExtra("eventTitle", "UI Test Event");

        try (ActivityScenario<ScannerActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarScanner)).check(matches(isDisplayed()));
            onView(withId(R.id.tvScannerError)).check(matches(isDisplayed()));
            onView(withId(R.id.btnStartScanner)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void sosDashboardScreen_showsToolbarAndStateViews() {
        try (ActivityScenario<SOSDashboardActivity> ignored = ActivityScenario.launch(SOSDashboardActivity.class)) {
            onView(withId(R.id.toolbarSos)).check(matches(isDisplayed()));
            onView(withId(R.id.rvSosAlerts)).check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    @Test
    public void paymentConfirmationScreen_showsContextAndListSurfaces() {
        Intent intent = new Intent(context, PaymentConfirmationActivity.class);
        intent.putExtra("eventId", "ui-test-event");
        intent.putExtra("eventTitle", "UI Test Event");

        try (ActivityScenario<PaymentConfirmationActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarPayments)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.rvPayments)).check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    @Test
    public void memoryAlbumScreen_showsToolbarAndPhotoGrid() {
        Intent intent = new Intent(context, MemoryAlbumActivity.class);
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_TITLE, "UI Test Event");
        intent.putStringArrayListExtra(
                MemoryAlbumActivity.EXTRA_PHOTO_URLS,
                new java.util.ArrayList<>(java.util.Collections.singletonList("https://example.com/photo.jpg"))
        );

        try (ActivityScenario<MemoryAlbumActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarMemoryAlbum)).check(matches(isDisplayed()));
            onView(withId(R.id.rvMemoryAlbumPhotos)).check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    @Test
    public void notificationCenterWalkthrough_showsToolbarAndNotificationList() {
        Intent intent = new Intent(context, NotificationCenterActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<NotificationCenterActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarNotifications)).check(matches(isDisplayed()));
            onView(withId(R.id.rvNotifications)).check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    @Test
    public void memoriesWalkthrough_showsToolbarCreateButtonAndList() {
        Intent intent = new Intent(context, MemoriesActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<MemoriesActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarMemories)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCreateMemory)).check(matches(isDisplayed()));
            onView(withId(R.id.rvMemories)).check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    @Test
    public void accountSettingsWalkthrough_showsEditableSectionsAndSaveAction() {
        Intent intent = new Intent(context, AccountSettingsActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<AccountSettingsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarSettings)).check(matches(isDisplayed()));
            onView(withId(R.id.etFullName)).check(matches(isDisplayed()));
            onView(withId(R.id.etUniversity)).check(matches(isDisplayed()));
            onView(withId(R.id.btnChangePassword)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSaveSettings)).check(matches(isDisplayed()));
        }
    }
}
