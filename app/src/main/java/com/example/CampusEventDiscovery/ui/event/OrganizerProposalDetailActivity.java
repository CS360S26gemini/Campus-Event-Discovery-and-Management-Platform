package com.example.CampusEventDiscovery.ui.event;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Read-only organizer view for pending and rejected event proposals.
 */
public class OrganizerProposalDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbarProposalDetail;
    private TextView tvProposalTitle;
    private TextView tvProposalStatus;
    private TextView tvProposalSubmittedAt;
    private TextView tvProposalDateTime;
    private TextView tvProposalVenue;
    private TextView tvProposalDescription;
    private TextView tvProposalTags;
    private TextView tvProposalSponsors;
    private TextView tvProposalFoodStalls;
    private TextView tvProposalTrailer;
    private TextView tvProposalAdminNote;

    private EventRepository repository;
    private String proposalId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_proposal_detail);

        repository = new EventRepository();
        proposalId = getIntent().getStringExtra("proposalId");

        bindViews();
        setupToolbar();
        loadProposal();
    }

    private void bindViews() {
        toolbarProposalDetail = findViewById(R.id.toolbarProposalDetail);
        tvProposalTitle = findViewById(R.id.tvProposalTitle);
        tvProposalStatus = findViewById(R.id.tvProposalStatus);
        tvProposalSubmittedAt = findViewById(R.id.tvProposalSubmittedAt);
        tvProposalDateTime = findViewById(R.id.tvProposalDateTime);
        tvProposalVenue = findViewById(R.id.tvProposalVenue);
        tvProposalDescription = findViewById(R.id.tvProposalDescription);
        tvProposalTags = findViewById(R.id.tvProposalTags);
        tvProposalSponsors = findViewById(R.id.tvProposalSponsors);
        tvProposalFoodStalls = findViewById(R.id.tvProposalFoodStalls);
        tvProposalTrailer = findViewById(R.id.tvProposalTrailer);
        tvProposalAdminNote = findViewById(R.id.tvProposalAdminNote);
    }

    private void setupToolbar() {
        toolbarProposalDetail.setNavigationOnClickListener(v -> finish());
        tvProposalTrailer.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void loadProposal() {
        if (TextUtils.isEmpty(proposalId)) {
            finish();
            return;
        }

        repository.getProposalById(proposalId, new EventRepository.ProposalCallback() {
            @Override
            public void onSuccess(EventProposal proposal) {
                bindProposal(proposal);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        OrganizerProposalDetailActivity.this,
                        getString(R.string.error_loading_proposal),
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        });
    }

    private void bindProposal(EventProposal proposal) {
        if (proposal == null) {
            finish();
            return;
        }

        String status = proposal.getStatus() == null ? "" : proposal.getStatus().trim().toLowerCase(Locale.getDefault());

        tvProposalTitle.setText(safeText(proposal.getTitle(), getString(R.string.app_name)));
        tvProposalStatus.setText(formatStatus(status));
        tvProposalSubmittedAt.setText(getString(R.string.proposal_submitted_at, formatDateTime(proposal.getSubmittedAt())));
        tvProposalDateTime.setText(formatDateTime(proposal.getDate()));
        tvProposalVenue.setText(safeText(proposal.getLocation(), getString(R.string.placeholder_venue)));
        tvProposalDescription.setText(safeText(proposal.getDescription(), getString(R.string.placeholder_description)));
        tvProposalTags.setText(formatList(proposal.getTags()));
        tvProposalSponsors.setText(formatList(proposal.getSponsors()));
        tvProposalFoodStalls.setText(formatList(proposal.getFoodStalls()));
        tvProposalTrailer.setText(safeText(proposal.getTrailerUrl(), getString(R.string.none_list_value)));

        String note = proposal.getAdminNote();
        if (TextUtils.isEmpty(note)) {
            note = "pending".equals(status)
                    ? getString(R.string.proposal_note_pending)
                    : getString(R.string.no_review_note_yet);
        }
        tvProposalAdminNote.setText(note);
    }

    private String formatStatus(String status) {
        if ("pending".equals(status)) {
            return getString(R.string.proposal_status_pending);
        }
        if ("rejected".equals(status)) {
            return getString(R.string.proposal_status_rejected);
        }
        if ("approved".equals(status)) {
            return getString(R.string.proposal_status_approved);
        }
        return getString(R.string.proposal_status_unknown);
    }

    private String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return getString(R.string.none_list_value);
        }
        return TextUtils.join(", ", items);
    }

    private String formatDateTime(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return getString(R.string.placeholder_date);
        }
        return new SimpleDateFormat("EEE, dd MMM - hh:mm a", Locale.getDefault())
                .format(timestamp.toDate());
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
