package com.example.CampusEventDiscovery.ui.vendors;

import android.os.Bundle;
import android.content.DialogInterface;
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

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.VendorEventAdapter;
import com.example.CampusEventDiscovery.adapter.VendorProposalAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.model.VendorProposal;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VendorManagementFragment extends Fragment {

    private static final String ARG_ROLE = "role";

    private TextView tvSubtitle;
    private TextView tvEventPrompt;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private MaterialToolbar toolbarVendorManagement;
    private RecyclerView rvEvents;
    private RecyclerView rvProposals;
    private MaterialCardView cardVendorToggle;
    private MaterialButton btnVendorApproved;
    private MaterialButton btnVendorPending;
    private MaterialButton btnVendorRejected;
    private FloatingActionButton fabAddVendor;

    private EventRepository repository;
    private VendorEventAdapter eventAdapter;
    private VendorProposalAdapter proposalAdapter;

    private final List<Event> organizerEvents = new ArrayList<>();
    private final List<VendorProposal> allProposals = new ArrayList<>();

    private String role = UserRoles.ATTENDEE;
    private String currentUserId;
    private String currentUserName;
    private String selectedStatus = "approved";
    private Event selectedEvent;
    private boolean showingOrganizerEventDetail;
    private boolean walkthroughMode;

    public static VendorManagementFragment newInstance(String role) {
        VendorManagementFragment fragment = new VendorManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vendor_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isActive();
        role = UserRoles.sanitize(getArguments() == null ? null : getArguments().getString(ARG_ROLE));

        tvSubtitle = view.findViewById(R.id.tvVendorSubtitle);
        tvEventPrompt = view.findViewById(R.id.tvVendorEventPrompt);
        tvEmpty = view.findViewById(R.id.tvVendorEmpty);
        progressBar = view.findViewById(R.id.progressVendor);
        toolbarVendorManagement = view.findViewById(R.id.toolbarVendorManagement);
        rvEvents = view.findViewById(R.id.rvVendorEvents);
        rvProposals = view.findViewById(R.id.rvVendorProposals);
        cardVendorToggle = view.findViewById(R.id.cardVendorToggle);
        btnVendorApproved = view.findViewById(R.id.btnVendorApproved);
        btnVendorPending = view.findViewById(R.id.btnVendorPending);
        btnVendorRejected = view.findViewById(R.id.btnVendorRejected);
        fabAddVendor = view.findViewById(R.id.fabAddVendor);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : DevSessionManager.getEffectiveUserId(requireContext());
        currentUserName = DevSessionManager.getDisplayName(requireContext());

        setupLists();
        setupToggle();
        setupRoleUi();
        loadCurrentUserName();
        loadData();
        WalkthroughManager.maybeShow(requireActivity(), view, "vendors");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroyView() {
        if (rvEvents != null) {
            rvEvents.setAdapter(null);
        }
        if (rvProposals != null) {
            rvProposals.setAdapter(null);
        }
        if (fabAddVendor != null) {
            fabAddVendor.setOnClickListener(null);
        }
        eventAdapter = null;
        proposalAdapter = null;
        repository = null;
        super.onDestroyView();
    }

    private void setupLists() {
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventAdapter = new VendorEventAdapter(organizerEvents, event -> {
            openOrganizerEventDetail(event);
        });
        rvEvents.setAdapter(eventAdapter);

        rvProposals.setLayoutManager(new LinearLayoutManager(requireContext()));
        proposalAdapter = new VendorProposalAdapter(allProposals, UserRoles.isAdmin(role), new VendorProposalAdapter.OnVendorActionListener() {
            @Override
            public void onApprove(VendorProposal proposal) {
                reviewVendor(proposal, true);
            }

            @Override
            public void onReject(VendorProposal proposal) {
                reviewVendor(proposal, false);
            }
        });
        rvProposals.setAdapter(proposalAdapter);
    }

    private void setupToggle() {
        btnVendorApproved.setOnClickListener(v -> selectVendorStatus("approved"));
        btnVendorPending.setOnClickListener(v -> selectVendorStatus("pending"));
        btnVendorRejected.setOnClickListener(v -> selectVendorStatus("rejected"));
        applyVendorStatusState();
    }

    private void selectVendorStatus(String status) {
        if (TextUtils.equals(selectedStatus, status)) {
            return;
        }
        selectedStatus = status;
        applyVendorStatusState();
        filterAndShowProposals();
    }

    private void applyVendorStatusState() {
        if (!isAdded()) {
            return;
        }

        styleVendorButton(btnVendorApproved, "approved".equals(selectedStatus));
        styleVendorButton(btnVendorPending, "pending".equals(selectedStatus));
        styleVendorButton(btnVendorRejected, "rejected".equals(selectedStatus));
    }

    private void styleVendorButton(MaterialButton button, boolean selected) {
        if (button == null) {
            return;
        }

        button.setSelected(selected);
        button.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        button.refreshDrawableState();
    }

    private void setupRoleUi() {
        boolean admin = UserRoles.isAdmin(role);
        tvSubtitle.setText(admin ? R.string.admin_vendor_requests_subtitle : R.string.vendor_management_subtitle);
        toolbarVendorManagement.setTitle(admin ? R.string.vendor_requests : R.string.vendor_management);
        toolbarVendorManagement.setNavigationIcon(null);
        toolbarVendorManagement.setNavigationOnClickListener(v -> {
            if (showingOrganizerEventDetail) {
                showOrganizerEventSelection();
            }
        });
        tvEventPrompt.setVisibility(admin ? View.GONE : View.VISIBLE);
        rvEvents.setVisibility(admin ? View.GONE : View.VISIBLE);
        rvProposals.setVisibility(admin ? View.VISIBLE : View.GONE);
        cardVendorToggle.setVisibility(admin ? View.VISIBLE : View.GONE);
        fabAddVendor.setVisibility(View.GONE);
        fabAddVendor.setOnClickListener(v -> showAddVendorDialog());
        selectedStatus = admin ? "pending" : "approved";
        applyVendorStatusState();
    }

    private void loadCurrentUserName() {
        if (TextUtils.isEmpty(currentUserId)) {
            return;
        }
        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded() || user == null || TextUtils.isEmpty(user.getFullName())) return;
                currentUserName = user.getFullName();
            }

            @Override
            public void onError(Exception e) {
                // Keep dev/session fallback name.
            }
        });
    }

    private void loadData() {
        if (!isAdded() || repository == null) {
            return;
        }
        if (walkthroughMode) {
            bindWalkthroughData();
            return;
        }
        if (UserRoles.isAdmin(role)) {
            loadAdminProposals();
        } else {
            loadOrganizerEvents();
        }
    }

    private void bindWalkthroughData() {
        showLoading(false);
        allProposals.clear();
        allProposals.addAll(WalkthroughManager.getDemoVendorProposals());

        if (UserRoles.isAdmin(role)) {
            filterAndShowProposals();
            WalkthroughManager.maybeShow(requireActivity(), getView(), "vendors");
            return;
        }

        organizerEvents.clear();
        organizerEvents.addAll(WalkthroughManager.getDemoEvents());
        eventAdapter.updateData(new ArrayList<>(organizerEvents));
        selectedEvent = WalkthroughManager.getDemoEvent();
        showingOrganizerEventDetail = true;
        eventAdapter.setSelectedEventId(selectedEvent.getEventId());
        String title = TextUtils.isEmpty(selectedEvent.getTitle()) ? getString(R.string.vendor_management) : selectedEvent.getTitle();
        toolbarVendorManagement.setTitle(title);
        toolbarVendorManagement.setNavigationIcon(R.drawable.ic_back);
        tvSubtitle.setText(TextUtils.isEmpty(selectedEvent.getLocation()) ? getString(R.string.vendor_management_subtitle) : selectedEvent.getLocation());
        tvEventPrompt.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        rvProposals.setVisibility(View.VISIBLE);
        cardVendorToggle.setVisibility(View.VISIBLE);
        fabAddVendor.setVisibility(View.VISIBLE);
        filterAndShowProposals();
        WalkthroughManager.maybeShow(requireActivity(), getView(), "vendors");
    }

    private void loadOrganizerEvents() {
        if (TextUtils.isEmpty(currentUserId)) {
            showEmpty(getString(R.string.no_approved_events_for_vendors));
            return;
        }
        showLoading(true);
        repository.getOrganizerEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) return;
                showLoading(false);
                organizerEvents.clear();
                if (events != null) {
                    organizerEvents.addAll(events);
                }
                eventAdapter.updateData(new ArrayList<>(organizerEvents));

                if (organizerEvents.isEmpty()) {
                    selectedEvent = null;
                    proposalAdapter.updateData(new ArrayList<>());
                    showOrganizerEventSelection();
                    showEmpty(getString(R.string.no_approved_events_for_vendors));
                    return;
                }

                if (showingOrganizerEventDetail && selectedEvent != null && containsEvent(selectedEvent.getEventId())) {
                    eventAdapter.setSelectedEventId(selectedEvent.getEventId());
                    loadProposalsForSelectedEvent();
                } else {
                    selectedEvent = null;
                    showOrganizerEventSelection();
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                showEmpty(getString(R.string.no_approved_events_for_vendors));
            }
        });
    }

    private void loadProposalsForSelectedEvent() {
        if (selectedEvent == null || TextUtils.isEmpty(selectedEvent.getEventId())) {
            showEmpty(getString(R.string.no_vendors_for_event));
            return;
        }
        showLoading(true);
        repository.getVendorProposalsForEvent(selectedEvent.getEventId(), new EventRepository.VendorProposalListCallback() {
            @Override
            public void onSuccess(List<VendorProposal> proposals) {
                if (!isAdded()) return;
                showLoading(false);
                allProposals.clear();
                if (proposals != null) {
                    allProposals.addAll(proposals);
                }
                filterAndShowProposals();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                showEmpty(getString(R.string.no_vendors_for_event));
            }
        });
    }

    private void openOrganizerEventDetail(Event event) {
        if (event == null) {
            return;
        }
        selectedEvent = event;
        showingOrganizerEventDetail = true;
        eventAdapter.setSelectedEventId(event.getEventId());
        String title = TextUtils.isEmpty(event.getTitle()) ? getString(R.string.vendor_management) : event.getTitle();
        toolbarVendorManagement.setTitle(title);
        toolbarVendorManagement.setNavigationIcon(R.drawable.ic_back);
        tvSubtitle.setText(TextUtils.isEmpty(event.getLocation()) ? getString(R.string.vendor_management_subtitle) : event.getLocation());
        tvEventPrompt.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        rvProposals.setVisibility(View.VISIBLE);
        cardVendorToggle.setVisibility(View.VISIBLE);
        fabAddVendor.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        loadProposalsForSelectedEvent();
    }

    private void showOrganizerEventSelection() {
        if (UserRoles.isAdmin(role)) {
            return;
        }
        showingOrganizerEventDetail = false;
        selectedEvent = null;
        eventAdapter.setSelectedEventId(null);
        proposalAdapter.updateData(new ArrayList<>());
        allProposals.clear();
        toolbarVendorManagement.setTitle(R.string.vendor_management);
        toolbarVendorManagement.setNavigationIcon(null);
        tvSubtitle.setText(R.string.select_event_for_vendors_help);
        tvEventPrompt.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.VISIBLE);
        rvProposals.setVisibility(View.GONE);
        cardVendorToggle.setVisibility(View.GONE);
        fabAddVendor.setVisibility(View.GONE);
        tvEmpty.setVisibility(organizerEvents.isEmpty() ? View.VISIBLE : View.GONE);
        selectedStatus = "approved";
        applyVendorStatusState();
    }

    private void loadAdminProposals() {
        showLoading(true);
        repository.markPendingVendorProposalsRead(null);
        repository.getAllVendorProposals(new EventRepository.VendorProposalListCallback() {
            @Override
            public void onSuccess(List<VendorProposal> proposals) {
                if (!isAdded()) return;
                showLoading(false);
                allProposals.clear();
                if (proposals != null) {
                    allProposals.addAll(proposals);
                }
                filterAndShowProposals();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                showEmpty(getString(R.string.no_vendor_requests));
            }
        });
    }

    private void filterAndShowProposals() {
        List<VendorProposal> filtered = new ArrayList<>();
        for (VendorProposal proposal : allProposals) {
            if (proposal == null) {
                continue;
            }
            String status = proposal.getStatus() == null ? "pending" : proposal.getStatus().toLowerCase(Locale.getDefault());
            if (selectedStatus.equals(status)) {
                filtered.add(proposal);
            }
        }
        proposalAdapter.updateData(filtered);
        if (filtered.isEmpty()) {
            showEmpty(UserRoles.isAdmin(role) ? getString(R.string.no_vendor_requests) : getString(R.string.no_vendors_for_event));
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showAddVendorDialog() {
        if (selectedEvent == null || TextUtils.isEmpty(selectedEvent.getEventId())) {
            Toast.makeText(requireContext(), R.string.select_event_for_vendors, Toast.LENGTH_SHORT).show();
            return;
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vendor, null, false);
        TextInputLayout layoutName = content.findViewById(R.id.layoutVendorName);
        TextInputLayout layoutDescription = content.findViewById(R.id.layoutVendorDescription);
        TextInputLayout layoutPhone = content.findViewById(R.id.layoutVendorPhone);
        TextInputEditText etName = content.findViewById(R.id.etVendorName);
        TextInputEditText etDescription = content.findViewById(R.id.etVendorDescription);
        TextInputEditText etPhone = content.findViewById(R.id.etVendorPhone);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_vendor)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.submit_vendor_request, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = valueOf(etName);
            String description = valueOf(etDescription);
            String phone = valueOf(etPhone);
            layoutName.setError(TextUtils.isEmpty(name) ? getString(R.string.required_field) : null);
            layoutDescription.setError(TextUtils.isEmpty(description) ? getString(R.string.required_field) : null);
            layoutPhone.setError(TextUtils.isEmpty(phone) ? getString(R.string.required_field) : null);
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(phone)) {
                return;
            }

            VendorProposal proposal = new VendorProposal();
            proposal.setVendorName(name);
            proposal.setDescription(description);
            proposal.setPhone(phone);
            proposal.setEventId(selectedEvent.getEventId());
            proposal.setEventTitle(selectedEvent.getTitle());
            proposal.setOrganizerId(currentUserId);
            proposal.setOrganizerName(currentUserName);
            submitVendor(proposal, dialog);
        }));
        dialog.show();
    }

    private void submitVendor(VendorProposal proposal, androidx.appcompat.app.AlertDialog dialog) {
        if (walkthroughMode) {
            Toast.makeText(requireContext(), "Walkthrough mode: vendor request was not submitted.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        repository.proposeVendor(proposal, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                dialog.dismiss();
                Toast.makeText(requireContext(), R.string.vendor_request_submitted, Toast.LENGTH_SHORT).show();
                selectedStatus = "pending";
                applyVendorStatusState();
                loadProposalsForSelectedEvent();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.vendor_request_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reviewVendor(VendorProposal proposal, boolean approve) {
        if (walkthroughMode) {
            Toast.makeText(requireContext(), "Walkthrough mode: vendor request was not reviewed.", Toast.LENGTH_SHORT).show();
            return;
        }

        EventRepository.ActionCallback callback = new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), approve ? R.string.vendor_approved : R.string.vendor_rejected, Toast.LENGTH_SHORT).show();
                loadAdminProposals();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.vendor_review_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };

        if (approve) {
            repository.approveVendorProposal(proposal, callback);
        } else {
            repository.rejectVendorProposal(proposal, "", callback);
        }
    }

    private boolean containsEvent(String eventId) {
        for (Event event : organizerEvents) {
            if (event != null && event.getEventId() != null && event.getEventId().equals(eventId)) {
                return true;
            }
        }
        return false;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
