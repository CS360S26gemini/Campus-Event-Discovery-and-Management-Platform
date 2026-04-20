package com.example.CampusEventDiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.EventAdapter;
import com.example.CampusEventDiscovery.adapter.OrganizerPendingAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Organizer-focused event management screen showing approved, pending,
 * and rejected items in one place.
 */
public class ManageEventsActivity extends AppCompatActivity {

    private MaterialToolbar toolbarManageEvents;
    private ProgressBar progressBarSection1;
    private ProgressBar progressBarSection2;
    private ProgressBar progressBarSection3;
    private TextView tvEmptySection1;
    private TextView tvEmptySection2;
    private TextView tvEmptySection3;
    private TextInputEditText etManageEventsSearch;
    private RecyclerView rvSection1;
    private RecyclerView rvSection2;
    private RecyclerView rvSection3;

    private EventRepository repository;
    private String currentUserId;
    private String currentRole = UserRoles.ORGANIZER;

    private EventAdapter approvedAdapter;
    private OrganizerPendingAdapter pendingAdapter;
    private OrganizerPendingAdapter rejectedAdapter;

    private final List<Event> approvedEvents = new ArrayList<>();
    private final List<Event> pendingEvents = new ArrayList<>();
    private final List<Event> rejectedEvents = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);
        currentRole = DevSessionManager.shouldUseBypass(this)
                ? DevSessionManager.getBypassRole(this)
                : UserRoles.ORGANIZER;

        bindViews();
        setupToolbar();
        setupSearch();
        setupRecyclerViews();
        loadRoleAndData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoleAndData();
    }

    private void bindViews() {
        toolbarManageEvents = findViewById(R.id.toolbarManageEvents);
        progressBarSection1 = findViewById(R.id.progressBarSection1);
        progressBarSection2 = findViewById(R.id.progressBarSection2);
        progressBarSection3 = findViewById(R.id.progressBarSection3);
        tvEmptySection1 = findViewById(R.id.tvEmptySection1);
        tvEmptySection2 = findViewById(R.id.tvEmptySection2);
        tvEmptySection3 = findViewById(R.id.tvEmptySection3);
        etManageEventsSearch = findViewById(R.id.etManageEventsSearch);
        rvSection1 = findViewById(R.id.rvSection1);
        rvSection2 = findViewById(R.id.rvSection2);
        rvSection3 = findViewById(R.id.rvSection3);
    }

    private void setupToolbar() {
        toolbarManageEvents.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        etManageEventsSearch.addTextChangedListener(new TextWatcher() {
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
        approvedAdapter = new EventAdapter(
                approvedEvents,
                Collections.emptySet(),
                null,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        if (event == null || TextUtils.isEmpty(event.getEventId())) {
                            return;
                        }

                        Intent intent = new Intent(ManageEventsActivity.this, OrganizerEventDetailActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        startActivity(intent);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        // no-op for organizer management
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        // no-op for organizer management
                    }
                }
        );

        pendingAdapter = new OrganizerPendingAdapter(pendingEvents, this::openProposalDetail);
        rejectedAdapter = new OrganizerPendingAdapter(rejectedEvents, this::openProposalDetail);

        rvSection1.setLayoutManager(new LinearLayoutManager(this));
        rvSection1.setNestedScrollingEnabled(false);
        rvSection1.setAdapter(approvedAdapter);

        rvSection2.setLayoutManager(new LinearLayoutManager(this));
        rvSection2.setNestedScrollingEnabled(false);
        rvSection2.setAdapter(pendingAdapter);

        rvSection3.setLayoutManager(new LinearLayoutManager(this));
        rvSection3.setNestedScrollingEnabled(false);
        rvSection3.setAdapter(rejectedAdapter);
    }

    private void loadRoleAndData() {
        if (DevSessionManager.shouldUseBypass(this)) {
            currentRole = DevSessionManager.getBypassRole(this);
            loadData();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            loadData();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                currentRole = user == null ? UserRoles.ORGANIZER : UserRoles.sanitize(user.getRole());
                if (TextUtils.isEmpty(currentRole)) {
                    currentRole = UserRoles.ORGANIZER;
                }
                loadData();
            }

            @Override
            public void onError(Exception e) {
                loadData();
            }
        });
    }

    private void loadData() {
        if (UserRoles.isAdmin(currentRole)) {
            toolbarManageEvents.setTitle(R.string.admin_all_active_events);
        } else {
            toolbarManageEvents.setTitle(R.string.manage_events);
        }
        loadApprovedEvents();
        loadProposals();
    }

    private void loadApprovedEvents() {
        setLoading(progressBarSection1, true);

        if (!UserRoles.isAdmin(currentRole) && TextUtils.isEmpty(currentUserId)) {
            bindApprovedEvents(new ArrayList<>());
            return;
        }

        EventRepository.EventListCallback callback = new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                bindApprovedEvents(events);
            }

            @Override
            public void onError(Exception e) {
                bindApprovedEvents(new ArrayList<>());
            }
        };

        if (UserRoles.isAdmin(currentRole)) {
            repository.getAllActiveEvents(callback);
        } else {
            repository.getOrganizerEvents(currentUserId, callback);
        }
    }

    private void bindApprovedEvents(List<Event> events) {
        approvedEvents.clear();
        if (events != null) {
            approvedEvents.addAll(events);
        }
        applyFilters();
        setLoading(progressBarSection1, false);
    }

    private void loadProposals() {
        setLoading(progressBarSection2, true);
        setLoading(progressBarSection3, true);

        if (!UserRoles.isAdmin(currentRole) && TextUtils.isEmpty(currentUserId)) {
            bindProposalSections(new ArrayList<>());
            return;
        }

        EventRepository.ProposalListCallback callback = new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                bindProposalSections(proposals);
            }

            @Override
            public void onError(Exception e) {
                bindProposalSections(new ArrayList<>());
            }
        };

        if (UserRoles.isAdmin(currentRole)) {
            repository.getAllProposalsByStatus("pending", new EventRepository.ProposalListCallback() {
                @Override
                public void onSuccess(List<EventProposal> pending) {
                    repository.getAllProposalsByStatus("rejected", new EventRepository.ProposalListCallback() {
                        @Override
                        public void onSuccess(List<EventProposal> rejected) {
                            List<EventProposal> combined = new ArrayList<>();
                            combined.addAll(pending);
                            combined.addAll(rejected);
                            callback.onSuccess(combined);
                        }

                        @Override
                        public void onError(Exception e) {
                            callback.onError(e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            repository.getOrganizerProposals(currentUserId, callback);
        }
    }

    private void bindProposalSections(List<EventProposal> proposals) {
        pendingEvents.clear();
        rejectedEvents.clear();

        if (proposals != null) {
            for (EventProposal proposal : proposals) {
                Event event = proposalToEvent(proposal);
                if ("pending".equalsIgnoreCase(proposal.getStatus())) {
                    pendingEvents.add(event);
                } else if ("rejected".equalsIgnoreCase(proposal.getStatus())) {
                    rejectedEvents.add(event);
                }
            }
        }

        applyFilters();

        setLoading(progressBarSection2, false);
        setLoading(progressBarSection3, false);
    }

    private Event proposalToEvent(EventProposal proposal) {
        Event event = new Event();
        event.setEventId(proposal.getProposalId());
        event.setTitle(proposal.getTitle());
        event.setDescription(proposal.getDescription());
        event.setDate(proposal.getDate());
        event.setLocation(proposal.getLocation());
        event.setCapacity(proposal.getCapacity());
        event.setStatus(proposal.getStatus());
        return event;
    }

    private void openProposalDetail(Event event) {
        if (event == null || TextUtils.isEmpty(event.getEventId())) {
            return;
        }

        Intent intent = new Intent(this, OrganizerProposalDetailActivity.class);
        intent.putExtra("proposalId", event.getEventId());
        startActivity(intent);
    }

    private void applyFilters() {
        List<Event> filteredApproved = filterEvents(approvedEvents);
        List<Event> filteredPending = filterEvents(pendingEvents);
        List<Event> filteredRejected = filterEvents(rejectedEvents);

        approvedAdapter.updateData(filteredApproved);
        pendingAdapter.updateData(filteredPending);
        rejectedAdapter.updateData(filteredRejected);

        tvEmptySection1.setVisibility(filteredApproved.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection1.setVisibility(filteredApproved.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptySection2.setVisibility(filteredPending.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection2.setVisibility(filteredPending.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptySection3.setVisibility(filteredRejected.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection3.setVisibility(filteredRejected.isEmpty() ? View.GONE : View.VISIBLE);
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

    private void setLoading(ProgressBar progressBar, boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
