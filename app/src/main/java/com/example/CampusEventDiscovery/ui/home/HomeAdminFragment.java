package com.example.CampusEventDiscovery.ui.home;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.OrganizerPendingAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.Vendor;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.event.OrganizerProposalDetailActivity;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeAdminFragment.java
 *
 * Administrator dashboard for reviewing and approving event proposals and vendor requests.
 */
public class HomeAdminFragment extends Fragment {

    private RecyclerView rvAdminApprovals;
    private ProgressBar progressBarAdmin;
    private TextView tvEmptyAdmin;
    private MaterialButtonToggleGroup toggleApprovalStatus;
    private MaterialButton btnCreateEvent;
    private MaterialButton btnManageEvents;
    private MaterialButton btnScanTickets;
    private MaterialButton btnSosDashboard;

    // Vendor Request Fields
    private RecyclerView rvVendorRequests;
    private TextView tvNoVendorRequests;
    private ProgressBar progressBarVendors;
    private AdminVendorAdapter vendorAdapter;
    private final List<Vendor> vendorList = new ArrayList<>();

    private OrganizerPendingAdapter approvalAdapter;
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
        toggleApprovalStatus = view.findViewById(R.id.toggleApprovalStatus);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnManageEvents = view.findViewById(R.id.btnManageEvents);
        btnScanTickets = view.findViewById(R.id.btnScanTickets);
        btnSosDashboard = view.findViewById(R.id.btnSosDashboard);

        // Initialize Vendor Request Views
        rvVendorRequests = view.findViewById(R.id.rvVendorRequests);
        tvNoVendorRequests = view.findViewById(R.id.tvNoVendorRequests);
        progressBarVendors = view.findViewById(R.id.progressBarVendors);

        setupToolActions();
        setupRecyclerViews();
        setupApprovalToggle();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
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
    }

    private void setupRecyclerViews() {
        // Proposals Adapter
        approvalAdapter = new OrganizerPendingAdapter(approvalEvents, event -> {
            Intent intent = new Intent(requireContext(),
                    showingRejected ? OrganizerProposalDetailActivity.class : EventApprovalActivity.class);
            intent.putExtra("proposalId", event.getEventId());
            startActivity(intent);
        });
        rvAdminApprovals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminApprovals.setAdapter(approvalAdapter);

        // Vendor Adapter
        vendorAdapter = new AdminVendorAdapter(vendorList);
        rvVendorRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVendorRequests.setNestedScrollingEnabled(false);
        rvVendorRequests.setAdapter(vendorAdapter);
    }

    private void setupApprovalToggle() {
        toggleApprovalStatus.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showingRejected = checkedId == R.id.btnRejectedEvents;
            loadDashboardData();
        });
    }

    private void loadDashboardData() {
        showLoading(true);

        Task<QuerySnapshot> proposalsTask = repository.getProposalsTask(showingRejected ? "rejected" : "pending");
        Task<QuerySnapshot> vendorsTask = repository.getPendingVendorsTask();

        Tasks.whenAllComplete(proposalsTask, vendorsTask).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            showLoading(false);

            if (proposalsTask.isSuccessful()) {
                updateProposalsUI(proposalsTask.getResult());
            }
            if (vendorsTask.isSuccessful()) {
                updateVendorsUI(vendorsTask.getResult());
            }
        });
    }

    private void updateProposalsUI(QuerySnapshot snapshots) {
        approvalEvents.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            EventProposal p = doc.toObject(EventProposal.class);
            p.setProposalId(doc.getId());
            Event e = new Event();
            e.setEventId(p.getProposalId());
            e.setTitle(p.getTitle());
            e.setDate(p.getDate());
            e.setLocation(p.getLocation());
            e.setStatus(p.getStatus());
            approvalEvents.add(e);
        }
        approvalAdapter.updateData(new ArrayList<>(approvalEvents));
        tvEmptyAdmin.setText(showingRejected ? R.string.no_rejected_proposals : R.string.no_pending_approvals);
        tvEmptyAdmin.setVisibility(approvalEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateVendorsUI(QuerySnapshot snapshots) {
        vendorList.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            Vendor v = doc.toObject(Vendor.class);
            v.setVendorId(doc.getId());
            vendorList.add(v);
        }
        vendorAdapter.notifyDataSetChanged();
        progressBarVendors.setVisibility(View.GONE);
        if (vendorList.isEmpty()) {
            tvNoVendorRequests.setVisibility(View.VISIBLE);
            rvVendorRequests.setVisibility(View.GONE);
        } else {
            tvNoVendorRequests.setVisibility(View.GONE);
            rvVendorRequests.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBarAdmin != null) {
            progressBarAdmin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            progressBarVendors.setVisibility(View.VISIBLE);
            rvVendorRequests.setVisibility(View.GONE);
            tvNoVendorRequests.setVisibility(View.GONE);
        }
    }

    private class AdminVendorAdapter extends RecyclerView.Adapter<AdminVendorAdapter.ViewHolder> {
        private final List<Vendor> list;

        AdminVendorAdapter(List<Vendor> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            container.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvEvent = new TextView(parent.getContext());
            tvEvent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvOrganizer = new TextView(parent.getContext());
            tvOrganizer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvPhone = new TextView(parent.getContext());
            tvPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvType = new TextView(parent.getContext());
            tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            LinearLayout buttonContainer = new LinearLayout(parent.getContext());
            buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
            buttonContainer.setPadding(0, dpToPx(8), 0, 0);

            Button btnApprove = new Button(parent.getContext());
            btnApprove.setText("Approve");
            btnApprove.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button btnReject = new Button(parent.getContext(), null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
            btnReject.setText("Reject");
            btnReject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnReject.getLayoutParams();
            lp.setMarginStart(dpToPx(8));

            buttonContainer.addView(btnApprove);
            buttonContainer.addView(btnReject);

            container.addView(tvName);
            container.addView(tvEvent);
            container.addView(tvOrganizer);
            container.addView(tvPhone);
            container.addView(tvType);
            container.addView(buttonContainer);

            return new ViewHolder(container, tvName, tvEvent, tvOrganizer, tvPhone, tvType, btnApprove, btnReject);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Vendor vendor = list.get(position);
            holder.tvName.setText(vendor.getVendorName());
            holder.tvEvent.setText("Event: " + vendor.getEventTitle());
            holder.tvOrganizer.setText("Organizer: " + vendor.getOrganizerName());
            holder.tvPhone.setText("Phone: " + vendor.getPhoneNumber());

            if (TextUtils.isEmpty(vendor.getVendorType())) {
                holder.tvType.setVisibility(View.GONE);
            } else {
                holder.tvType.setVisibility(View.VISIBLE);
                holder.tvType.setText("Type: " + vendor.getVendorType());
            }

            holder.btnApprove.setOnClickListener(v -> repository.approveVendorRequest(vendor.getVendorId(), new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        list.remove(pos);
                        notifyItemRemoved(pos);
                        Toast.makeText(getContext(), "Vendor approved", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(Exception e) { Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
            }));

            holder.btnReject.setOnClickListener(v -> repository.rejectVendorRequest(vendor.getVendorId(), new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        list.remove(pos);
                        notifyItemRemoved(pos);
                        Toast.makeText(getContext(), "Vendor rejected", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(Exception e) { Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
            }));
        }

        @Override
        public int getItemCount() { return list.size(); }

        private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvName, tvEvent, tvOrganizer, tvPhone, tvType;
            final Button btnApprove, btnReject;

            ViewHolder(@NonNull View itemView, TextView tvName, TextView tvEvent, TextView tvOrganizer,
                       TextView tvPhone, TextView tvType, Button btnApprove, Button btnReject) {
                super(itemView);
                this.tvName = tvName;
                this.tvEvent = tvEvent;
                this.tvOrganizer = tvOrganizer;
                this.tvPhone = tvPhone;
                this.tvType = tvType;
                this.btnApprove = btnApprove;
                this.btnReject = btnReject;
            }
        }
    }
}
