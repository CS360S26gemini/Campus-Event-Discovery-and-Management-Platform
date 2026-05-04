package com.example.CampusEventDiscovery.ui.home;

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

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.adapter.OrganizerPendingAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeAdminFragment.java
 *
 * Administrator dashboard for reviewing and approving event proposals.
 */
public class HomeAdminFragment extends Fragment {

    private RecyclerView rvAdminApprovals;
    private ProgressBar progressBarAdmin;
    private TextView tvEmptyAdmin;
    private MaterialButton btnPendingApprovals;
    private MaterialButton btnRejectedEvents;
    private MaterialButton btnCreateEvent;
    private MaterialButton btnManageEvents;
    private MaterialButton btnScanTickets;
    private MaterialButton btnSosDashboard;
    private MaterialButton btnVendorRequests;
    private ListenerRegistration vendorCountRegistration;

    private OrganizerPendingAdapter adapter;
    private EventRepository repository;
    private final List<Event> approvalEvents = new ArrayList<>();
    private boolean showingRejected;

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

        rvAdminApprovals = view.findViewById(R.id.rvAdminApprovals);
        progressBarAdmin = view.findViewById(R.id.progressBarAdmin);
        tvEmptyAdmin = view.findViewById(R.id.tvEmptyAdmin);
        btnPendingApprovals = view.findViewById(R.id.btnPendingApprovals);
        btnRejectedEvents = view.findViewById(R.id.btnRejectedEvents);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnManageEvents = view.findViewById(R.id.btnManageEvents);
        btnScanTickets = view.findViewById(R.id.btnScanTickets);
        btnSosDashboard = view.findViewById(R.id.btnSosDashboard);
        btnVendorRequests = view.findViewById(R.id.btnVendorRequests);

        setupToolActions();
        setupRecyclerView();
        setupApprovalToggle();
        observeVendorRequestCount();
        if (WalkthroughManager.isActive()) {
            approvalEvents.clear();
            approvalEvents.add(WalkthroughManager.getDemoEvent());
            adapter.updateData(new ArrayList<>(approvalEvents));
            tvEmptyAdmin.setVisibility(View.GONE);
        }
        WalkthroughManager.maybeShow(requireActivity(), view, "home_admin");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (WalkthroughManager.isActive()) {
            WalkthroughManager.maybeShow(requireActivity(), getView(), "home_admin");
            return;
        }
        loadApprovals();
        if (getView() != null) {
            WalkthroughManager.maybeShow(requireActivity(), getView(), "home_admin");
        }
    }

    @Override
    public void onDestroyView() {
        if (vendorCountRegistration != null) {
            vendorCountRegistration.remove();
            vendorCountRegistration = null;
        }
        if (rvAdminApprovals != null) {
            rvAdminApprovals.setAdapter(null);
        }
        adapter = null;
        repository = null;
        super.onDestroyView();
    }

    private void setupToolActions() {
        btnCreateEvent.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateEventActivity.class)));
        btnManageEvents.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageEventsActivity.class)));
        btnScanTickets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ScannerActivity.class)));
        btnSosDashboard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SOSDashboardActivity.class)));
        btnVendorRequests.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openVendorManagement();
            }
        });
    }

    private void observeVendorRequestCount() {
        vendorCountRegistration = repository.observeUnreadPendingVendorProposalCount(new EventRepository.IntegerCallback() {
            @Override
            public void onSuccess(int value) {
                if (!isAdded() || btnVendorRequests == null) return;
                btnVendorRequests.setText(value > 0
                        ? getString(R.string.vendor_requests_with_count, value)
                        : getString(R.string.vendor_requests));
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || btnVendorRequests == null) return;
                btnVendorRequests.setText(R.string.vendor_requests);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new OrganizerPendingAdapter(approvalEvents, event -> {
            Intent intent = new Intent(requireContext(),
                    showingRejected ? OrganizerProposalDetailActivity.class : EventApprovalActivity.class);
            intent.putExtra("proposalId", event.getEventId());
            startActivity(intent);
        });
        rvAdminApprovals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminApprovals.setAdapter(adapter);
    }

    private void setupApprovalToggle() {
        btnPendingApprovals.setOnClickListener(v -> selectApprovalStatus(false));
        btnRejectedEvents.setOnClickListener(v -> selectApprovalStatus(true));
        applyApprovalToggleState();
    }

    private void selectApprovalStatus(boolean rejected) {
        if (showingRejected == rejected) {
            return;
        }
        showingRejected = rejected;
        applyApprovalToggleState();
        loadApprovals();
    }

    private void applyApprovalToggleState() {
        if (!isAdded()) {
            return;
        }

        styleApprovalButton(btnPendingApprovals, !showingRejected);
        styleApprovalButton(btnRejectedEvents, showingRejected);
    }

    private void styleApprovalButton(MaterialButton button, boolean selected) {
        if (button == null) {
            return;
        }

        button.setSelected(selected);
        button.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        button.refreshDrawableState();
    }

    private void loadApprovals() {
        showLoading(true);

        String status = showingRejected ? "rejected" : "pending";
        repository.getAllProposalsByStatus(status, new EventRepository.ProposalListCallback() {
            @Override
            public void onSuccess(List<EventProposal> proposals) {
                if (!isAdded()) return;
                showLoading(false);
                approvalEvents.clear();
                for (EventProposal p : proposals) {
                    Event e = new Event();
                    e.setEventId(p.getProposalId());
                    e.setTitle(p.getTitle());
                    e.setDate(p.getDate());
                    e.setLocation(p.getLocation());
                    e.setOrganizerId(p.getOrganizerId());
                    e.setOrganizerName(p.getOrganizerName());
                    e.setOrganizerEmail(p.getOrganizerEmail());
                    e.setStatus(p.getStatus());
                    String imageUrl = p.getThumbnailUrl();
                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        imageUrl = p.getImageUrl();
                    }
                    e.setThumbnailUrl(imageUrl == null ? "" : imageUrl);
                    approvalEvents.add(e);
                }
                adapter.updateData(new ArrayList<>(approvalEvents));
                tvEmptyAdmin.setText(showingRejected ? R.string.no_rejected_proposals : R.string.no_pending_approvals);
                tvEmptyAdmin.setVisibility(approvalEvents.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                tvEmptyAdmin.setText(showingRejected ? R.string.no_rejected_proposals : R.string.no_pending_approvals);
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
