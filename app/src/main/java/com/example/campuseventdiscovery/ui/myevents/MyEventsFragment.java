package com.example.campuseventdiscovery.ui.myevents;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.EventAdapter;
import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.event.EventDetailActivity;
import com.example.campuseventdiscovery.ui.organizer.OrganizerEventDetailActivity;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.example.campuseventdiscovery.util.UserRoles;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MyEventsFragment.java
 *
 * Displays personal events based on user role.
 * Attendee: RSVPs and Recently Viewed.
 * Organizer: Approved, Pending, and Rejected events created by them.
 */
public class MyEventsFragment extends Fragment {

    private MaterialToolbar toolbarMyEvents;
    
    private TextView tvSection1Header, tvSection2Header, tvSection3Header;
    private TextView tvEmptySection1, tvEmptySection2, tvEmptySection3;
    private ProgressBar progressBarSection1, progressBarSection2, progressBarSection3;
    private RecyclerView rvSection1, rvSection2, rvSection3;
    private View divider1, divider2;

    private EventRepository repository;

    private EventAdapter adapter1, adapter2, adapter3;

    private final List<Event> list1 = new ArrayList<>();
    private final List<Event> list2 = new ArrayList<>();
    private final List<Event> list3 = new ArrayList<>();
    
    private final Set<String> ids1 = new HashSet<>();
    private final Set<String> ids2 = new HashSet<>();
    private final Set<String> ids3 = new HashSet<>();

    private String currentUserId;
    private String userRole = UserRoles.ATTENDEE;

    public MyEventsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(requireContext());

        bindViews(view);
        setupToolbar();
        setupRecyclerViews();
        
        loadUserRoleAndData();
    }

    private void bindViews(View view) {
        toolbarMyEvents = view.findViewById(R.id.toolbarMyEvents);
        
        tvSection1Header = view.findViewById(R.id.tvSection1Header);
        tvSection2Header = view.findViewById(R.id.tvSection2Header);
        tvSection3Header = view.findViewById(R.id.tvSection3Header);
        
        tvEmptySection1 = view.findViewById(R.id.tvEmptySection1);
        tvEmptySection2 = view.findViewById(R.id.tvEmptySection2);
        tvEmptySection3 = view.findViewById(R.id.tvEmptySection3);
        
        progressBarSection1 = view.findViewById(R.id.progressBarSection1);
        progressBarSection2 = view.findViewById(R.id.progressBarSection2);
        progressBarSection3 = view.findViewById(R.id.progressBarSection3);
        
        rvSection1 = view.findViewById(R.id.rvSection1);
        rvSection2 = view.findViewById(R.id.rvSection2);
        rvSection3 = view.findViewById(R.id.rvSection3);
        
        divider1 = view.findViewById(R.id.divider1);
        divider2 = view.findViewById(R.id.divider2);
    }

    private void setupToolbar() {
        if (toolbarMyEvents != null) {
            toolbarMyEvents.setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupRecyclerViews() {
        adapter1 = createAdapter(list1, ids1, true);
        adapter2 = createAdapter(list2, ids2, false);
        adapter3 = createAdapter(list3, ids3, false);

        rvSection1.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSection1.setNestedScrollingEnabled(false);
        rvSection1.setAdapter(adapter1);

        rvSection2.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSection2.setNestedScrollingEnabled(false);
        rvSection2.setAdapter(adapter2);
        
        rvSection3.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSection3.setNestedScrollingEnabled(false);
        rvSection3.setAdapter(adapter3);
    }
    
    private EventAdapter createAdapter(List<Event> list, Set<String> ids, boolean allowLongClick) {
        return new EventAdapter(
                list,
                ids,
                currentUserId,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        openEventDetail(event);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        // no-op here
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        if (allowLongClick && UserRoles.isAttendee(userRole)) {
                            showCancelRsvpDialog(event);
                        }
                    }
                }
        );
    }

    private void loadUserRoleAndData() {
        if (DevSessionManager.shouldUseBypass(requireContext())) {
            userRole = DevSessionManager.getBypassRole(requireContext());
            if (UserRoles.isOrganizer(userRole)) {
                loadOrganizerData();
            } else {
                loadAttendeeData();
            }
            return;
        }

        if (currentUserId == null) {
            loadAttendeeData();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userRole = user == null ? UserRoles.ATTENDEE : UserRoles.sanitize(user.getRole());
                if (TextUtils.isEmpty(userRole)) userRole = UserRoles.ATTENDEE;
                
                if (UserRoles.isOrganizer(userRole)) {
                    loadOrganizerData();
                } else {
                    loadAttendeeData();
                }
            }

            @Override
            public void onError(Exception e) {
                loadAttendeeData();
            }
        });
    }

    private void loadAttendeeData() {
        if (toolbarMyEvents != null) {
            toolbarMyEvents.setTitle(R.string.my_events_header);
        }
        tvSection1Header.setText(R.string.my_events_section);
        tvSection2Header.setText(R.string.recently_viewed_section);
        
        tvSection3Header.setVisibility(View.GONE);
        rvSection3.setVisibility(View.GONE);
        divider2.setVisibility(View.GONE);
        
        loadRsvps();
        loadRecentEvents();
    }

    private void loadOrganizerData() {
        if (toolbarMyEvents != null) {
            toolbarMyEvents.setTitle(R.string.manage_events);
        }
        tvSection1Header.setText(R.string.approved_events_label);
        tvSection2Header.setText(R.string.pending_events_label);
        tvSection3Header.setText(R.string.rejected_proposals_label);
        
        tvSection3Header.setVisibility(View.VISIBLE);
        divider2.setVisibility(View.VISIBLE);
        
        loadApprovedEvents();
        loadProposals();
    }

    private void loadRsvps() {
        showLoading(progressBarSection1, true);
        if (currentUserId == null) {
            updateList(list1, ids1, adapter1, new ArrayList<>(), tvEmptySection1, progressBarSection1);
            return;
        }

        repository.getRsvps(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                updateList(list1, ids1, adapter1, events, tvEmptySection1, progressBarSection1);
            }

            @Override
            public void onError(Exception e) {
                updateList(list1, ids1, adapter1, new ArrayList<>(), tvEmptySection1, progressBarSection1);
            }
        });
    }

    private void loadRecentEvents() {
        showLoading(progressBarSection2, true);
        String stored = requireContext()
                .getSharedPreferences("recently_viewed", android.content.Context.MODE_PRIVATE)
                .getString("event_ids", "");

        if (TextUtils.isEmpty(stored)) {
            updateList(list2, ids2, adapter2, new ArrayList<>(), tvEmptySection2, progressBarSection2);
            return;
        }

        String[] splitIds = stored.split(",");
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        for (String id : splitIds) {
            if (!TextUtils.isEmpty(id)) orderedIds.add(id.trim());
        }

        if (orderedIds.isEmpty()) {
            updateList(list2, ids2, adapter2, new ArrayList<>(), tvEmptySection2, progressBarSection2);
            return;
        }

        List<Event> recentEvents = new ArrayList<>();
        final int total = orderedIds.size();
        final int[] completed = {0};

        for (String id : orderedIds) {
            repository.getEventById(id, new EventRepository.SingleEventCallback() {
                @Override
                public void onSuccess(Event event) {
                    if (event != null) recentEvents.add(event);
                    checkFinish();
                }

                @Override
                public void onError(Exception e) {
                    checkFinish();
                }

                private void checkFinish() {
                    completed[0]++;
                    if (completed[0] == total) {
                        updateList(list2, ids2, adapter2, recentEvents, tvEmptySection2, progressBarSection2);
                    }
                }
            });
        }
    }

    private void loadApprovedEvents() {
        showLoading(progressBarSection1, true);
        repository.getOrganizerEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                updateList(list1, ids1, adapter1, events, tvEmptySection1, progressBarSection1);
            }

            @Override
            public void onError(Exception e) {
                updateList(list1, ids1, adapter1, new ArrayList<>(), tvEmptySection1, progressBarSection1);
            }
        });
    }

    private void loadProposals() {
        showLoading(progressBarSection2, true);
        showLoading(progressBarSection3, true);
        repository.getOrganizerProposals(currentUserId, new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                List<Event> pending = new ArrayList<>();
                List<Event> rejected = new ArrayList<>();
                
                for (EventProposal p : proposals) {
                    Event e = proposalToEvent(p);
                    if ("pending".equalsIgnoreCase(p.getStatus())) {
                        pending.add(e);
                    } else if ("rejected".equalsIgnoreCase(p.getStatus())) {
                        rejected.add(e);
                    }
                }
                
                updateList(list2, ids2, adapter2, pending, tvEmptySection2, progressBarSection2);
                updateList(list3, ids3, adapter3, rejected, tvEmptySection3, progressBarSection3);
            }

            @Override
            public void onError(Exception e) {
                updateList(list2, ids2, adapter2, new ArrayList<>(), tvEmptySection2, progressBarSection2);
                updateList(list3, ids3, adapter3, new ArrayList<>(), tvEmptySection3, progressBarSection3);
            }
        });
    }

    private void updateList(List<Event> targetList, Set<String> targetIds, EventAdapter adapter, 
                            List<Event> newData, TextView emptyView, ProgressBar progressBar) {
        targetList.clear();
        targetIds.clear();
        if (newData != null) {
            targetList.addAll(newData);
            for (Event e : newData) {
                if (e.getEventId() != null) targetIds.add(e.getEventId());
            }
        }
        adapter.updateSavedIds(targetIds);
        adapter.updateData(new ArrayList<>(targetList));
        
        emptyView.setVisibility(targetList.isEmpty() ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(View.GONE);
        
        // Handle visibility of the whole section if needed
        if (rvSection1.getAdapter() == adapter) rvSection1.setVisibility(targetList.isEmpty() ? View.GONE : View.VISIBLE);
        if (rvSection2.getAdapter() == adapter) rvSection2.setVisibility(targetList.isEmpty() ? View.GONE : View.VISIBLE);
        if (rvSection3.getAdapter() == adapter) rvSection3.setVisibility(targetList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private Event proposalToEvent(EventProposal p) {
        Event e = new Event();
        e.setEventId(p.getProposalId());
        e.setTitle(p.getTitle());
        e.setDescription(p.getDescription());
        e.setCategory(p.getCategory());
        e.setDate(p.getDate());
        e.setLocation(p.getLocation());
        e.setCapacity(p.getCapacity());
        e.setOrganizerId(p.getOrganizerId());
        e.setOrganizerName(p.getOrganizerName());
        e.setStatus(p.getStatus());
        e.setTrailerUrl(p.getTrailerUrl());
        return e;
    }

    private void showCancelRsvpDialog(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.cancel_rsvp))
                .setMessage(getString(R.string.cancel_rsvp_message))
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    repository.cancelRsvp(currentUserId, event.getEventId(), () -> {
                        loadRsvps();
                        Toast.makeText(requireContext(), getString(R.string.rsvp_cancelled), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showLoading(ProgressBar pb, boolean isLoading) {
        if (pb != null) pb.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void openEventDetail(Event event) {
        if (event == null || event.getEventId() == null) return;

        if (UserRoles.isOrganizer(userRole)) {
            String status = event.getStatus() == null ? "" : event.getStatus().trim().toLowerCase();
            if (!"active".equals(status)) {
                int messageRes = "rejected".equals(status)
                        ? R.string.proposal_rejected_message
                        : R.string.proposal_pending_message;
                Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(requireContext(), OrganizerEventDetailActivity.class);
            intent.putExtra("eventId", event.getEventId());
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
