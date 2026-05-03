package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoryAlbumActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.util.WalkthroughManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class ReliabilitySmokeTest {

    @Test
    public void notificationCenterWalkthrough_bindsNonEmptyStateReliably() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), NotificationCenterActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        NotificationCenterActivity activity = Robolectric.buildActivity(NotificationCenterActivity.class, intent).setup().get();
        RecyclerView recyclerView = activity.findViewById(R.id.rvNotifications);
        View empty = activity.findViewById(R.id.tvEmptyNotifications);

        assertTrue(recyclerView.getAdapter() != null);
        assertEquals(2, recyclerView.getAdapter().getItemCount());
        assertEquals(View.VISIBLE, recyclerView.getVisibility());
        assertEquals(View.GONE, empty.getVisibility());
    }

    @Test
    public void memoriesWalkthrough_bindsDemoAlbumAndHidesEmptyState() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MemoriesActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        MemoriesActivity activity = Robolectric.buildActivity(MemoriesActivity.class, intent).setup().get();
        RecyclerView recyclerView = activity.findViewById(R.id.rvMemories);
        View empty = activity.findViewById(R.id.tvEmptyMemories);

        assertTrue(recyclerView.getAdapter() != null);
        assertEquals(1, recyclerView.getAdapter().getItemCount());
        assertEquals(View.VISIBLE, recyclerView.getVisibility());
        assertEquals(View.GONE, empty.getVisibility());
    }

    @Test
    public void memoryAlbumWalkthrough_emptyAlbumShowsStableEmptySurface() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MemoryAlbumActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_ID, WalkthroughManager.getDemoEvent().getEventId());
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_TITLE, WalkthroughManager.getDemoEvent().getTitle());
        intent.putStringArrayListExtra(MemoryAlbumActivity.EXTRA_PHOTO_URLS, new ArrayList<>());

        MemoryAlbumActivity activity = Robolectric.buildActivity(MemoryAlbumActivity.class, intent).setup().get();
        RecyclerView recyclerView = activity.findViewById(R.id.rvMemoryAlbumPhotos);
        View empty = activity.findViewById(R.id.tvEmptyMemoryAlbum);

        assertTrue(recyclerView.getAdapter() != null);
        assertEquals(0, recyclerView.getAdapter().getItemCount());
        assertEquals(View.GONE, recyclerView.getVisibility());
        assertEquals(View.VISIBLE, empty.getVisibility());
    }

    @Test
    public void eventApprovalWalkthrough_loadsReviewSurfaceWithoutBackend() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventApprovalActivity.class);
        intent.putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);
        intent.putExtra("proposalId", WalkthroughManager.getDemoProposal().getProposalId());

        EventApprovalActivity activity = Robolectric.buildActivity(EventApprovalActivity.class, intent).setup().get();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.btnApprove).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.btnReject).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.toolbarEventApproval).getVisibility());
    }
}
