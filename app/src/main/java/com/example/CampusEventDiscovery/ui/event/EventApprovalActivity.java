package com.example.CampusEventDiscovery.ui.event;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * EventApprovalActivity.java
 *
 * Detailed administrative review page for inspecting and approving/rejecting submitted events.
 */
public class EventApprovalActivity extends AppCompatActivity {

    private ImageView ivBanner;
    private MaterialToolbar toolbarEventApproval;
    private TextView tvTitle, tvDateTime, tvVenue, tvDescription, tvPriceInfo;
    private com.google.android.material.button.MaterialButton btnApprove, btnReject;

    private String proposalId;
    private EventProposal currentProposal;
    private EventRepository repository;
    private boolean walkthroughMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_approval);

        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();
        proposalId = getIntent().getStringExtra("proposalId");

        bindViews();
        setupListeners();
        if (walkthroughMode) {
            currentProposal = WalkthroughManager.getDemoProposal();
            bindProposal(currentProposal);
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "approval");
        } else {
            loadProposalDetails();
        }
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        toolbarEventApproval = findViewById(R.id.toolbarEventApproval);
        tvTitle = findViewById(R.id.tvTitle);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvDescription = findViewById(R.id.tvDescription);
        tvPriceInfo = findViewById(R.id.tvPriceInfo);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
    }

    private void setupListeners() {
        toolbarEventApproval.setNavigationOnClickListener(v -> finish());
        
        btnApprove.setOnClickListener(v -> {
            if (currentProposal == null) return;
            if (walkthroughMode) {
                Toast.makeText(this, "Walkthrough mode: proposal was not approved.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.approve)
                    .setMessage(R.string.approve_proposal_confirm_message)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> approveProposal())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        btnReject.setOnClickListener(v -> {
            if (currentProposal == null) return;
            if (walkthroughMode) {
                Toast.makeText(this, "Walkthrough mode: proposal was not rejected.", Toast.LENGTH_SHORT).show();
                return;
            }
            showRejectDialog();
        });
    }

    private void approveProposal() {
        setActionLoading(true);
        repository.approveProposal(proposalId, currentProposal, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventApprovalActivity.this, getString(R.string.proposal_approved_success), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                String message = e.getMessage() == null ? getString(R.string.app_name) : e.getMessage();
                Toast.makeText(EventApprovalActivity.this, getString(R.string.approval_failed, message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setActionLoading(boolean isLoading) {
        btnApprove.setEnabled(!isLoading);
        btnReject.setEnabled(!isLoading);
    }

    private void loadProposalDetails() {
        if (TextUtils.isEmpty(proposalId)) {
            finish();
            return;
        }

        repository.getProposalById(proposalId, new EventRepository.ProposalCallback() {
            @Override
            public void onSuccess(EventProposal proposal) {
                currentProposal = proposal;
                bindProposal(currentProposal);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventApprovalActivity.this, getString(R.string.error_loading_proposal), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindProposal(EventProposal proposal) {
        tvTitle.setText(proposal.getTitle());
        tvDateTime.setText(formatDateTime(proposal.getDate()));
        tvVenue.setText(proposal.getLocation());
        tvDescription.setText(proposal.getDescription());

        if (tvPriceInfo != null) {
            if (proposal.getTicketPrice() == 0) {
                tvPriceInfo.setText(R.string.price_free);
            } else {
                tvPriceInfo.setText(String.format(Locale.getDefault(), "PKR %.2f", proposal.getTicketPrice()));
            }
        }

        ivBanner.setImageResource(R.drawable.bg_placeholder_image);
    }

    private void showRejectDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.rejection_note_hint);
        input.setMinLines(3);
        input.setMaxLines(5);
        input.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
                .setTitle(R.string.rejection_note_title)
                .setView(input)
                .setPositiveButton(R.string.reject, (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    if (TextUtils.isEmpty(note)) {
                        Toast.makeText(this, getString(R.string.rejection_requires_note), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    setActionLoading(true);
                    repository.rejectProposal(proposalId, currentProposal, note, new EventRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(EventApprovalActivity.this, getString(R.string.proposal_rejected_success), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            setActionLoading(false);
                            String message = e.getMessage() == null ? getString(R.string.app_name) : e.getMessage();
                            Toast.makeText(EventApprovalActivity.this, getString(R.string.rejection_failed, message), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "Date TBD";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}
