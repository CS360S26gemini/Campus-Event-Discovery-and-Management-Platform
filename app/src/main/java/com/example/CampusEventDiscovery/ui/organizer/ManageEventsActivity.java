package com.example.CampusEventDiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public class ManageEventsActivity extends AppCompatActivity {

    private enum ManageFilter {
        ALL,
        APPROVED,
        PENDING,
        REJECTED,
        PAST
    }

    private MaterialToolbar toolbarManageEvents;
    private TextInputEditText etManageEventsSearch;
    private AutoCompleteTextView actManageEventsFilter;
    private TextView tvManageEventsSubtitle;
    private TextView tvEmptyManageEvents;
    private ProgressBar progressBarManageEvents;
    private RecyclerView rvManageEventCards;

    private EventRepository repository;
    private String currentUserId;
    private String currentRole = UserRoles.ORGANIZER;
    private boolean walkthroughMode;
    private String searchQuery = "";
    private ManageFilter selectedFilter = ManageFilter.APPROVED;

    private EventAdapter eventAdapter;

    private final List<Event> approvedEvents = new ArrayList<>();
    private final List<Event> pastEvents = new ArrayList<>();
    private final List<Event> pendingEvents = new ArrayList<>();
    private final List<Event> rejectedEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();

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
        setupRecyclerView();
        setupFilterSelector();

        if (walkthroughMode) {
            bindWalkthroughData();
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "manage_events");
        } else {
            loadRoleAndData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (walkthroughMode) {
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "manage_events");
            return;
        }
        loadRoleAndData();
    }

    private void bindViews() {
        toolbarManageEvents = findViewById(R.id.toolbarManageEvents);
        etManageEventsSearch = findViewById(R.id.etManageEventsSearch);
        actManageEventsFilter = findViewById(R.id.actManageEventsFilter);
        tvManageEventsSubtitle = findViewById(R.id.tvManageEventsSubtitle);
        tvEmptyManageEvents = findViewById(R.id.tvEmptyManageEvents);
        progressBarManageEvents = findViewById(R.id.progressBarManageEvents);
        rvManageEventCards = findViewById(R.id.rvManageEventCards);
    }

    private void setupToolbar() {
        toolbarManageEvents.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        etManageEventsSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                renderSelectedFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(
                new ArrayList<>(),
                Collections.emptySet(),
                null,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        if (event == null || TextUtils.isEmpty(event.getEventId())) {
                            return;
                        }

                        String status = event.getStatus();
                        if ("pending".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)) {
                            openProposalDetail(event);
                        } else {
                            Intent intent = new Intent(ManageEventsActivity.this, OrganizerEventDetailActivity.class);
                            intent.putExtra("eventId", event.getEventId());
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                    }
                }
        );

        rvManageEventCards.setLayoutManager(new LinearLayoutManager(this));
        rvManageEventCards.setAdapter(eventAdapter);
    }

    private void setupFilterSelector() {
        String[] items = new String[] {
                getString(R.string.manage_events_tab_all),
                getString(R.string.manage_events_tab_approved),
                getString(R.string.manage_events_tab_pending),
                getString(R.string.manage_events_tab_rejected),
                getString(R.string.manage_events_tab_past)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                items
        );
        actManageEventsFilter.setAdapter(adapter);
        actManageEventsFilter.setText(getString(R.string.manage_events_tab_approved), false);
        actManageEventsFilter.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    selectFilter(ManageFilter.ALL);
                    break;
                case 1:
                    selectFilter(ManageFilter.APPROVED);
                    break;
                case 2:
                    selectFilter(ManageFilter.PENDING);
                    break;
                case 3:
                    selectFilter(ManageFilter.REJECTED);
                    break;
                default:
                    selectFilter(ManageFilter.PAST);
                    break;
            }
        });
    }

    private void bindWalkthroughData() {
        approvedEvents.clear();
        approvedEvents.addAll(WalkthroughManager.getDemoEvents());
        pendingEvents.clear();
        rejectedEvents.clear();
        pastEvents.clear();
        showLoading(false);
        renderSelectedFilter();
    }

    private void loadRoleAndData() {
        if (DevSessionManager.shouldUseBypass(this)) {
            currentRole = DevSessionManager.getBypassRole(this);
            loadAllData();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            loadAllData();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentRole = user == null ? UserRoles.ORGANIZER : UserRoles.sanitize(user.getRole());
                if (TextUtils.isEmpty(currentRole)) {
                    currentRole = UserRoles.ORGANIZER;
                }
                loadAllData();
            }

            @Override
            public void onError(Exception e) {
                loadAllData();
            }
        });
    }

    private void loadAllData() {
        toolbarManageEvents.setTitle(UserRoles.isAdmin(currentRole)
                ? getString(R.string.admin_all_events)
                : getString(R.string.manage_events));
        showLoading(true);

        loadApprovedEvents(new Runnable() {
            @Override
            public void run() {
                loadPastEvents(new Runnable() {
                    @Override
                    public void run() {
                        loadProposals(new Runnable() {
                            @Override
                            public void run() {
                                showLoading(false);
                                renderSelectedFilter();
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadApprovedEvents(Runnable next) {
        EventRepository.EventListCallback callback = new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                approvedEvents.clear();
                if (events != null) {
                    approvedEvents.addAll(events);
                }
                next.run();
            }

            @Override
            public void onError(Exception e) {
                approvedEvents.clear();
                next.run();
            }
        };

        if (UserRoles.isAdmin(currentRole)) {
            repository.getAllActiveEvents(callback);
        } else if (TextUtils.isEmpty(currentUserId)) {
            approvedEvents.clear();
            next.run();
        } else {
            repository.getOrganizerEvents(currentUserId, callback);
        }
    }

    private void loadPastEvents(Runnable next) {
        EventRepository.EventListCallback callback = new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                pastEvents.clear();
                if (events != null) {
                    pastEvents.addAll(events);
                }
                next.run();
            }

            @Override
            public void onError(Exception e) {
                pastEvents.clear();
                next.run();
            }
        };

        if (UserRoles.isAdmin(currentRole)) {
            repository.getAllPastEvents(callback);
        } else if (TextUtils.isEmpty(currentUserId)) {
            pastEvents.clear();
            next.run();
        } else {
            repository.getOrganizerPastEvents(currentUserId, callback);
        }
    }

    private void loadProposals(Runnable next) {
        EventRepository.ProposalListCallback callback = new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                bindProposalSections(proposals);
                next.run();
            }

            @Override
            public void onError(Exception e) {
                bindProposalSections(new ArrayList<>());
                next.run();
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
        } else if (TextUtils.isEmpty(currentUserId)) {
            bindProposalSections(new ArrayList<>());
            next.run();
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

        Comparator<Event> newestFirst = Comparator.comparing(
                Event::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        pendingEvents.sort(newestFirst);
        rejectedEvents.sort(newestFirst);
    }

    private Event proposalToEvent(EventProposal proposal) {
        Event event = new Event();
        event.setEventId(proposal.getProposalId());
        event.setTitle(proposal.getTitle());
        event.setDescription(proposal.getDescription());
        event.setDate(proposal.getDate());
        event.setEndTime(proposal.getEndTime());
        event.setLocation(proposal.getLocation());
        event.setCapacity(proposal.getCapacity());
        event.setStatus(proposal.getStatus());
        event.setOrganizerId(proposal.getOrganizerId());
        event.setOrganizerName(proposal.getOrganizerName());
        event.setOrganizerEmail(proposal.getOrganizerEmail());
        event.setThumbnailUrl(proposal.getThumbnailUrl());
        event.setCreatedAt(proposal.getSubmittedAt());
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

    private void selectFilter(ManageFilter filter) {
        if (selectedFilter == filter) {
            return;
        }
        selectedFilter = filter;
        updateSubtitle();
        renderSelectedFilter();
    }

    private void updateSubtitle() {
        tvManageEventsSubtitle.setText(getFilterTitleRes(selectedFilter));
    }

    private void renderSelectedFilter() {
        List<Event> filtered = filterEvents(getSourceForFilter(selectedFilter));
        eventAdapter.updateData(filtered);
        rvManageEventCards.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyManageEvents.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyManageEvents.setText(getEmptyTextRes(selectedFilter));
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

    private List<Event> getSourceForFilter(ManageFilter filter) {
        if (filter == ManageFilter.ALL) {
            List<Event> combined = new ArrayList<>();
            combined.addAll(approvedEvents);
            combined.addAll(pendingEvents);
            combined.addAll(rejectedEvents);
            combined.addAll(pastEvents);
            combined.sort(Comparator.comparing(
                    Event::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));
            return combined;
        }
        if (filter == ManageFilter.PENDING) {
            return new ArrayList<>(pendingEvents);
        }
        if (filter == ManageFilter.REJECTED) {
            return new ArrayList<>(rejectedEvents);
        }
        if (filter == ManageFilter.PAST) {
            return new ArrayList<>(pastEvents);
        }
        return new ArrayList<>(approvedEvents);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private int getFilterTitleRes(ManageFilter filter) {
        switch (filter) {
            case ALL:
                return R.string.manage_events_tab_all;
            case PENDING:
                return R.string.manage_events_tab_pending;
            case REJECTED:
                return R.string.manage_events_tab_rejected;
            case PAST:
                return R.string.manage_events_tab_past;
            default:
                return R.string.manage_events_tab_approved;
        }
    }

    private int getEmptyTextRes(ManageFilter filter) {
        switch (filter) {
            case ALL:
                return R.string.no_events_found;
            case PENDING:
                return R.string.no_pending_events;
            case REJECTED:
                return R.string.no_rejected_proposals;
            case PAST:
                return R.string.no_past_events;
            default:
                return UserRoles.isAdmin(currentRole)
                        ? R.string.no_active_events
                        : R.string.no_managed_events_help;
        }
    }

    private void showLoading(boolean isLoading) {
        progressBarManageEvents.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            rvManageEventCards.setVisibility(View.GONE);
            tvEmptyManageEvents.setVisibility(View.GONE);
        }
    }
}
