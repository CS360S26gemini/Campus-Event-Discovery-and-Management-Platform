package com.example.CampusEventDiscovery.ui.myevents;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.EventAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.ui.event.EventFeedbackActivity;
import com.example.CampusEventDiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.CampusEventDiscovery.ui.event.TicketActivity;
import com.example.CampusEventDiscovery.ui.organizer.OrganizerEventDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * MyEventsFragment.java
 *
 * Displays personal events based on user role.
 * Attendee: RSVPs and Recently Viewed.
 * Organizer: Approved, Pending, and Rejected events created by them.
 */
public class MyEventsFragment extends Fragment {

    private static final String ARG_SHOW_BACK_BUTTON = "show_back_button";

    private MaterialToolbar toolbarMyEvents;
    private TextInputEditText etMyEventsSearch;
    
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
    private String searchQuery = "";

    public MyEventsFragment() {
        // Required empty public constructor
    }

    public static MyEventsFragment newInstance(boolean showBackButton) {
        MyEventsFragment fragment = new MyEventsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_BACK_BUTTON, showBackButton);
        fragment.setArguments(args);
        return fragment;
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
        setupSearch();
        setupRecyclerViews();
        
        loadUserRoleAndData();
        WalkthroughManager.maybeShow(requireActivity(), view, "my_events");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadUserRoleAndData();
            WalkthroughManager.maybeShow(requireActivity(), getView(), "my_events");
        }
    }

    private void bindViews(View view) {
        toolbarMyEvents = view.findViewById(R.id.toolbarMyEvents);
        etMyEventsSearch = view.findViewById(R.id.etMyEventsSearch);
        
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
            boolean showBackButton = getArguments() != null
                    && getArguments().getBoolean(ARG_SHOW_BACK_BUTTON, false);
            if (showBackButton) {
                toolbarMyEvents.setNavigationIcon(R.drawable.ic_back);
                toolbarMyEvents.setNavigationOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            } else {
                toolbarMyEvents.setNavigationIcon(null);
                toolbarMyEvents.setNavigationOnClickListener(null);
            }
        }
    }

    private void setupSearch() {
        etMyEventsSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                applyFilters();
            }
        });
    }

    private void setupRecyclerViews() {
        adapter1 = new EventAdapter(
                list1,
                ids1,
                null,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        if (event == null || event.getEventId() == null || !canUseView()) return;
                        // For attendee RSVPs: if a QR ticket exists, open it directly
                        if (UserRoles.isAttendee(userRole)) {
                            openTicketOrDetail(event);
                        } else {
                            openEventDetail(event);
                        }
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        // no-op
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        if (UserRoles.isAttendee(userRole)) {
                            showAttendeeActionsDialog(event);
                        }
                    }
                }
        );
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
                null,
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
                            showAttendeeActionsDialog(event);
                        }
                    }
                }
        );
    }

    private void loadUserRoleAndData() {
        if (!canUseView()) {
            return;
        }

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
                if (!canUseView()) {
                    return;
                }

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
                if (!canUseView()) {
                    return;
                }

                loadAttendeeData();
            }
        });
    }

    private void loadAttendeeData() {
        if (!canUseView()) {
            return;
        }

        if (toolbarMyEvents != null) {
            toolbarMyEvents.setTitle(R.string.my_events_header);
        }
        tvSection1Header.setText(R.string.my_events_section);
        tvSection2Header.setText(R.string.recently_viewed_section);
        tvEmptySection1.setText(R.string.no_saved_events_help);
        tvEmptySection2.setText(R.string.no_recent_events);
        
        tvSection3Header.setVisibility(View.GONE);
        rvSection3.setVisibility(View.GONE);
        divider2.setVisibility(View.GONE);
        
        loadRsvps();
        loadRecentEvents();
    }

    private void loadOrganizerData() {
        if (!canUseView()) {
            return;
        }

        if (toolbarMyEvents != null) {
            toolbarMyEvents.setTitle(R.string.manage_events);
        }
        tvSection1Header.setText(R.string.approved_events_label);
        tvSection2Header.setText(R.string.pending_events_label);
        tvSection3Header.setText(R.string.rejected_proposals_label);
        tvEmptySection1.setText(R.string.no_managed_events_help);
        tvEmptySection2.setText(R.string.no_pending_events);
        tvEmptySection3.setText(R.string.no_rejected_proposals);
        
        tvSection3Header.setVisibility(View.VISIBLE);
        divider2.setVisibility(View.VISIBLE);
        
        loadApprovedEvents();
        loadProposals();
    }

    private void loadRsvps() {
        showLoading(progressBarSection1, true);
        if (WalkthroughManager.isActive()) {
            updateList(list1, ids1, adapter1, WalkthroughManager.getDemoEvents(), tvEmptySection1, progressBarSection1);
            return;
        }
        if (currentUserId == null) {
            updateList(list1, ids1, adapter1, new ArrayList<>(), tvEmptySection1, progressBarSection1);
            return;
        }

        repository.getRsvps(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!canUseView()) {
                    return;
                }

                updateList(list1, ids1, adapter1, events, tvEmptySection1, progressBarSection1);
            }

            @Override
            public void onError(Exception e) {
                if (!canUseView()) {
                    return;
                }

                updateList(list1, ids1, adapter1, new ArrayList<>(), tvEmptySection1, progressBarSection1);
            }
        });
    }

    private void loadRecentEvents() {
        if (!canUseView()) {
            return;
        }

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
                    if (!canUseView()) {
                        return;
                    }

                    if (event != null) recentEvents.add(event);
                    checkFinish();
                }

                @Override
                public void onError(Exception e) {
                    if (!canUseView()) {
                        return;
                    }

                    checkFinish();
                }

                private void checkFinish() {
                    if (!canUseView()) {
                        return;
                    }

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
                if (!canUseView()) {
                    return;
                }

                updateList(list1, ids1, adapter1, events, tvEmptySection1, progressBarSection1);
            }

            @Override
            public void onError(Exception e) {
                if (!canUseView()) {
                    return;
                }

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
                if (!canUseView()) {
                    return;
                }

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
                if (!canUseView()) {
                    return;
                }

                updateList(list2, ids2, adapter2, new ArrayList<>(), tvEmptySection2, progressBarSection2);
                updateList(list3, ids3, adapter3, new ArrayList<>(), tvEmptySection3, progressBarSection3);
            }
        });
    }

    private void updateList(List<Event> targetList, Set<String> targetIds, EventAdapter adapter, 
                            List<Event> newData, TextView emptyView, ProgressBar progressBar) {
        if (!canUseView()) {
            return;
        }

        targetList.clear();
        targetIds.clear();
        if (newData != null) {
            targetList.addAll(newData);
            for (Event e : newData) {
                if (e.getEventId() != null) targetIds.add(e.getEventId());
            }
        }
        adapter.updateSavedIds(targetIds);
        List<Event> filtered = filterEvents(targetList);
        adapter.updateData(filtered);
        
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(View.GONE);
        
        // Handle visibility of the whole section if needed
        if (rvSection1.getAdapter() == adapter) rvSection1.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        if (rvSection2.getAdapter() == adapter) rvSection2.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        if (rvSection3.getAdapter() == adapter) rvSection3.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void applyFilters() {
        if (adapter1 == null || adapter2 == null || adapter3 == null) {
            return;
        }

        List<Event> filtered1 = filterEvents(list1);
        List<Event> filtered2 = filterEvents(list2);
        List<Event> filtered3 = filterEvents(list3);

        adapter1.updateData(filtered1);
        adapter2.updateData(filtered2);
        adapter3.updateData(filtered3);

        tvEmptySection1.setVisibility(filtered1.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection1.setVisibility(filtered1.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptySection2.setVisibility(filtered2.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection2.setVisibility(filtered2.isEmpty() ? View.GONE : View.VISIBLE);

        boolean showThirdSection = tvSection3Header.getVisibility() == View.VISIBLE;
        tvEmptySection3.setVisibility(showThirdSection && filtered3.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection3.setVisibility(showThirdSection && !filtered3.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<Event> filterEvents(List<Event> source) {
        if (TextUtils.isEmpty(searchQuery)) {
            return new ArrayList<>(source);
        }

        String needle = searchQuery.toLowerCase();
        List<Event> filtered = new ArrayList<>();
        for (Event event : source) {
            if (event == null) {
                continue;
            }

            String haystack = (safeText(event.getTitle())
                    + " " + safeText(event.getLocation())
                    + " " + safeText(event.getCategory())
                    + " " + safeText(event.getStatus())).toLowerCase();
            if (haystack.contains(needle)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
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
        if (!canUseView()) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.cancel_rsvp))
                .setMessage(getString(R.string.cancel_rsvp_message))
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    if (!canUseView()) {
                        return;
                    }

                    repository.cancelRsvp(currentUserId, event.getEventId(), new EventRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (!canUseView()) {
                                return;
                            }

                            loadRsvps();
                            Toast.makeText(requireContext(), getString(R.string.rsvp_cancelled), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!canUseView()) {
                                return;
                            }

                            Toast.makeText(requireContext(), "Failed to cancel RSVP", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAttendeeActionsDialog(Event event) {
        if (event == null || TextUtils.isEmpty(event.getEventId()) || TextUtils.isEmpty(currentUserId) || !canUseView()) {
            return;
        }

        List<String> options = new ArrayList<>();
        options.add(getString(R.string.view_check_in_code)); // This will now launch TicketActivity
        if (isPastEvent(event)) {
            options.add(getString(R.string.leave_feedback));
        }
        options.add(getString(R.string.cancel_rsvp));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.attendee_actions_title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (!canUseView()) {
                        return;
                    }

                    if (which == 0) {
                        // Open TicketActivity
                        FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                                .collection("rsvps").document(event.getEventId())
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (!canUseView()) {
                                        return;
                                    }

                                    if (doc.exists()) {
                                        Rsvp rsvp = doc.toObject(Rsvp.class);
                                        if (rsvp != null) {
                                            Intent intent = new Intent(requireContext(), TicketActivity.class);
                                            intent.putExtra("rsvpId", event.getEventId());
                                            intent.putExtra("eventName", event.getTitle());
                                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
                                            intent.putExtra("eventDate", event.getDate() != null ? sdf.format(event.getDate().toDate()) : "Date TBD");
                                            intent.putExtra("transactionId", rsvp.getTransactionId());
                                            intent.putExtra("qrPayload", rsvp.getQrPayload());
                                            startActivity(intent);
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Ticket not found.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else if (isPastEvent(event) && which == 1) {
                        Intent intent = new Intent(requireContext(), EventFeedbackActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        intent.putExtra("eventTitle", event.getTitle());
                        startActivity(intent);
                    } else {
                        showCancelRsvpDialog(event);
                    }
                })
                .show();
    }

    private boolean isPastEvent(Event event) {
        return event != null
                && event.getDate() != null
                && event.getDate().toDate().before(new java.util.Date());
    }

    private void showLoading(ProgressBar pb, boolean isLoading) {
        if (pb != null) pb.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    /**
     * For attendee RSVP cards: checks Firestore for an existing QR payload.
     * If one exists, opens TicketActivity to show the QR code directly.
     * Falls back to EventDetailActivity if no RSVP ticket is found.
     *
     * @param event The event whose ticket or detail screen should be opened.
     */
    private void openTicketOrDetail(Event event) {
        if (!canUseView()) {
            return;
        }

        if (currentUserId == null || event.getEventId() == null) {
            openEventDetail(event);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(currentUserId)
                .collection("rsvps").document(event.getEventId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!canUseView()) {
                        return;
                    }

                    if (snapshot.exists()) {
                        Rsvp rsvp = snapshot.toObject(Rsvp.class);
                        if (rsvp != null
                                && rsvp.getQrPayload() != null
                                && !rsvp.getQrPayload().isEmpty()) {
                            String formattedDate = "Date TBD";
                            if (rsvp.getDate() != null) {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                        "EEE, dd MMM • hh:mm a", java.util.Locale.getDefault());
                                formattedDate = sdf.format(rsvp.getDate().toDate());
                            }
                            Intent intent = new Intent(requireContext(),
                                    com.example.CampusEventDiscovery.ui.event.TicketActivity.class);
                            intent.putExtra("rsvpId", event.getEventId());
                            intent.putExtra("eventName", rsvp.getTitle() != null
                                    ? rsvp.getTitle() : event.getTitle());
                            intent.putExtra("eventDate", formattedDate);
                            intent.putExtra("transactionId", rsvp.getTransactionId());
                            intent.putExtra("qrPayload", rsvp.getQrPayload());
                            startActivity(intent);
                            return;
                        }
                    }
                    // No ticket found — open event detail as normal
                    openEventDetail(event);
                })
                .addOnFailureListener(e -> {
                    if (canUseView()) {
                        openEventDetail(event);
                    }
                });
    }

    private void openEventDetail(Event event) {
        if (event == null || event.getEventId() == null || !canUseView()) return;

        if (UserRoles.isOrganizer(userRole)) {
            String status = event.getStatus() == null ? "" : event.getStatus().trim().toLowerCase();
            if (!"active".equals(status)) {
                Intent intent = new Intent(requireContext(), OrganizerProposalDetailActivity.class);
                intent.putExtra("proposalId", event.getEventId());
                startActivity(intent);
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

    private boolean canUseView() {
        return isAdded() && getView() != null;
    }
}
