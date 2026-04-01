package com.example.campuseventdiscovery.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.OrganizerPendingAdapter;
import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.event.EventApprovalActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeAdminFragment.java
 *
 * Administrator dashboard for reviewing and approving event proposals.
 */
public class HomeAdminFragment extends Fragment {

    private RecyclerView rvPendingApprovals;
    private ProgressBar progressBarAdmin;
    private TextView tvEmptyAdmin;
    
    private OrganizerPendingAdapter adapter;
    private EventRepository repository;
    private final List<Event> pendingEvents = new ArrayList<>();

    public HomeAdminFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new EventRepository();
        
        rvPendingApprovals = view.findViewById(R.id.rvPendingApprovals);
        progressBarAdmin = view.findViewById(R.id.progressBarAdmin);
        tvEmptyAdmin = view.findViewById(R.id.tvEmptyAdmin);

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPendingProposals();
    }

    private void setupRecyclerView() {
        adapter = new OrganizerPendingAdapter(pendingEvents, event -> {
            Intent intent = new Intent(requireContext(), EventApprovalActivity.class);
            intent.putExtra("proposalId", event.getEventId());
            startActivity(intent);
        });
        rvPendingApprovals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPendingApprovals.setAdapter(adapter);
    }

    private void loadPendingProposals() {
        showLoading(true);
        repository.getAllPendingProposals(new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                showLoading(false);
                pendingEvents.clear();
                for (EventProposal p : proposals) {
                    Event e = new Event();
                    e.setEventId(p.getProposalId());
                    e.setTitle(p.getTitle());
                    e.setDate(p.getDate());
                    e.setLocation(p.getLocation());
                    e.setStatus(p.getStatus());
                    pendingEvents.add(e);
                }
                adapter.updateData(new ArrayList<>(pendingEvents));
                tvEmptyAdmin.setVisibility(pendingEvents.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                tvEmptyAdmin.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBarAdmin != null) {
            progressBarAdmin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}