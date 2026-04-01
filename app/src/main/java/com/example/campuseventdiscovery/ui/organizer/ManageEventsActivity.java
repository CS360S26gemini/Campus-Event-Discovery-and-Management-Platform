package com.example.campuseventdiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.EventAdapter;
import com.example.campuseventdiscovery.adapter.OrganizerPendingAdapter;
import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
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
    private RecyclerView rvSection1;
    private RecyclerView rvSection2;
    private RecyclerView rvSection3;

    private EventRepository repository;
    private String currentUserId;

    private EventAdapter approvedAdapter;
    private OrganizerPendingAdapter pendingAdapter;
    private OrganizerPendingAdapter rejectedAdapter;

    private final List<Event> approvedEvents = new ArrayList<>();
    private final List<Event> pendingEvents = new ArrayList<>();
    private final List<Event> rejectedEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupRecyclerViews();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void bindViews() {
        toolbarManageEvents = findViewById(R.id.toolbarManageEvents);
        progressBarSection1 = findViewById(R.id.progressBarSection1);
        progressBarSection2 = findViewById(R.id.progressBarSection2);
        progressBarSection3 = findViewById(R.id.progressBarSection3);
        tvEmptySection1 = findViewById(R.id.tvEmptySection1);
        tvEmptySection2 = findViewById(R.id.tvEmptySection2);
        tvEmptySection3 = findViewById(R.id.tvEmptySection3);
        rvSection1 = findViewById(R.id.rvSection1);
        rvSection2 = findViewById(R.id.rvSection2);
        rvSection3 = findViewById(R.id.rvSection3);
    }

    private void setupToolbar() {
        toolbarManageEvents.setNavigationOnClickListener(v -> finish());
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

    private void loadData() {
        loadApprovedEvents();
        loadProposals();
    }

    private void loadApprovedEvents() {
        setLoading(progressBarSection1, true);

        if (TextUtils.isEmpty(currentUserId)) {
            bindApprovedEvents(new ArrayList<>());
            return;
        }

        repository.getOrganizerEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                bindApprovedEvents(events);
            }

            @Override
            public void onError(Exception e) {
                bindApprovedEvents(new ArrayList<>());
            }
        });
    }

    private void bindApprovedEvents(List<Event> events) {
        approvedEvents.clear();
        if (events != null) {
            approvedEvents.addAll(events);
        }
        approvedAdapter.updateData(new ArrayList<>(approvedEvents));
        tvEmptySection1.setVisibility(approvedEvents.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection1.setVisibility(approvedEvents.isEmpty() ? View.GONE : View.VISIBLE);
        setLoading(progressBarSection1, false);
    }

    private void loadProposals() {
        setLoading(progressBarSection2, true);
        setLoading(progressBarSection3, true);

        if (TextUtils.isEmpty(currentUserId)) {
            bindProposalSections(new ArrayList<>());
            return;
        }

        repository.getOrganizerProposals(currentUserId, new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                bindProposalSections(proposals);
            }

            @Override
            public void onError(Exception e) {
                bindProposalSections(new ArrayList<>());
            }
        });
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

        pendingAdapter.updateData(new ArrayList<>(pendingEvents));
        rejectedAdapter.updateData(new ArrayList<>(rejectedEvents));

        tvEmptySection2.setVisibility(pendingEvents.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection2.setVisibility(pendingEvents.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptySection3.setVisibility(rejectedEvents.isEmpty() ? View.VISIBLE : View.GONE);
        rvSection3.setVisibility(rejectedEvents.isEmpty() ? View.GONE : View.VISIBLE);

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

    private void setLoading(ProgressBar progressBar, boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
