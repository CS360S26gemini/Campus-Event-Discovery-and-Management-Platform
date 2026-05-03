package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.event.CheckoutActivity;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.OrganizerEventDetailActivity;
import com.example.CampusEventDiscovery.ui.organizer.WhoIsComingActivity;
import com.example.CampusEventDiscovery.ui.profile.AccountSettingsActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoryAlbumActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.util.WalkthroughManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {30})
public class WalkthroughFunctionalTest {

    @Test
    public void eventDetailWalkthrough_bindsDemoEventAndActionButtons() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("eventId", WalkthroughManager.getDemoEvent().getEventId());

        try (ActivityScenario<EventDetailActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(withText("Demo Music Night")));
            onView(withId(R.id.btnTickets)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.tvViewOnMap)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnShare)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void checkoutWalkthrough_showsDemoSummaryAndPrimaryAction() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CheckoutActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("eventId", WalkthroughManager.getDemoEvent().getEventId());
        intent.putExtra("eventTitle", WalkthroughManager.getDemoEvent().getTitle());
        intent.putExtra("totalPrice", WalkthroughManager.getDemoEvent().getTicketPrice());
        intent.putExtra("eventDateMillis", WalkthroughManager.getDemoEvent().getDate().toDate().getTime());
        intent.putExtra("eventVenue", WalkthroughManager.getDemoEvent().getLocation());

        try (ActivityScenario<CheckoutActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvCheckoutEventTitle)).check(matches(withText("Demo Music Night")));
            onView(withId(R.id.tvCheckoutTotal)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnPay)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void createEventWalkthrough_prefillsDemoProposalAndShowsSubmit() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etEventTitle)).check(matches(withText("Demo Cultural Night")));
            onView(withId(R.id.btnSelectImage)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnSubmitEvent)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void eventApprovalWalkthrough_showsProposalReviewActions() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventApprovalActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("proposalId", WalkthroughManager.getDemoProposal().getProposalId());

        try (ActivityScenario<EventApprovalActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(withText("Demo Cultural Night")));
            onView(withId(R.id.btnApprove)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnReject)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void notificationCenterWalkthrough_showsDemoNotifications() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), NotificationCenterActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<NotificationCenterActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rvNotifications)).check(matches(isDisplayed()));
            onView(withText("Demo event approved")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void whoIsComingWalkthrough_showsAttendeeToolsAndDemoList() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WhoIsComingActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("eventId", WalkthroughManager.getDemoEvent().getEventId());
        intent.putExtra("eventTitle", WalkthroughManager.getDemoEvent().getTitle());

        try (ActivityScenario<WhoIsComingActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(withText(R.string.who_is_coming)));
            onView(withId(R.id.tvEventContext)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.etSearchParticipants)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rvParticipants)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnScanQr)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnCheckIn)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnBlacklist)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withText("Demo Checked-In Attendee")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void whoIsComingBlacklistedMode_hidesAttendeeActionButtons() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WhoIsComingActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("eventId", WalkthroughManager.getDemoEvent().getEventId());
        intent.putExtra("eventTitle", WalkthroughManager.getDemoEvent().getTitle());
        intent.putExtra("showBlacklisted", true);

        try (ActivityScenario<WhoIsComingActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(withText(R.string.blacklisted_attendees)));
            onView(withId(R.id.btnScanQr)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.btnCheckIn)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.btnBlacklist)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }
    }

    @Test
    public void organizerEventDetailWalkthrough_showsManagementActions() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerEventDetailActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("eventId", WalkthroughManager.getDemoEvent().getEventId());

        try (ActivityScenario<OrganizerEventDetailActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvTitle)).check(matches(withText("Demo Music Night")));
            onView(withId(R.id.tvRegCount)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnWhoIsComing)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnAnnouncement)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnPayments)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnBlacklisted)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnDeleteEvent)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void manageEventsWalkthrough_showsFilterAndManagedEventList() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventsActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<ManageEventsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarManageEvents)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.actManageEventsFilter)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rvManageEventCards)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.tvManageEventsSubtitle)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void accountSettingsWalkthrough_showsProfileAndSecurityControls() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AccountSettingsActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<AccountSettingsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etFullName)).check(matches(withText("Demo Attendee")));
            onView(withId(R.id.etUniversity)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.chipGroupInterests)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnChangeEmail)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnChangePassword)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnSaveSettings)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void memoriesWalkthrough_showsMemoryActionsAndListSurface() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MemoriesActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        try (ActivityScenario<MemoriesActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnCreateMemory)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.rvMemories)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void memoryAlbumWalkthrough_showsAlbumControlsAndGrid() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MemoryAlbumActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_ID, WalkthroughManager.getDemoEvent().getEventId());
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_TITLE, WalkthroughManager.getDemoEvent().getTitle());
        intent.putStringArrayListExtra(MemoryAlbumActivity.EXTRA_PHOTO_URLS, new java.util.ArrayList<>());

        try (ActivityScenario<MemoryAlbumActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.toolbarMemoryAlbum)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnAddAlbumPhotos)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnSelectAlbumPhotos)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.tvEmptyMemoryAlbum)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }
}
