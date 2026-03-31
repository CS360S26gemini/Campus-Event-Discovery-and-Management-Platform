package com.example.campuseventdiscovery.ui.event;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * EventApprovalActivity.java
 *
 * Detailed administrative review page for inspecting and approving/rejecting submitted events.
 */
public class EventApprovalActivity extends AppCompatActivity {

    private ImageView ivBanner;
    private ImageButton btnBack;
    private TextView tvTitle, tvDateTime, tvVenue, tvDescription;
    private com.google.android.material.button.MaterialButton btnApprove, btnReject;

    private String proposalId;
    private EventProposal currentProposal;
    private EventRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_approval);

        repository = new EventRepository();
        proposalId = getIntent().getStringExtra("proposalId");

        bindViews();
        setupListeners();
        loadProposalDetails();
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvDescription = findViewById(R.id.tvDescription);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnApprove.setOnClickListener(v -> {
            if (currentProposal == null) return;
            repository.approveProposal(proposalId, currentProposal, new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EventApprovalActivity.this, "Event Approved and Created", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(EventApprovalActivity.this, "Approval failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnReject.setOnClickListener(v -> {
            if (currentProposal == null) return;
            repository.rejectProposal(proposalId, "Rejected by admin", new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EventApprovalActivity.this, "Proposal Rejected", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(EventApprovalActivity.this, "Rejection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadProposalDetails() {
        if (TextUtils.isEmpty(proposalId)) {
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("event_proposals").document(proposalId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentProposal = documentSnapshot.toObject(EventProposal.class);
                    if (currentProposal != null) {
                        tvTitle.setText(currentProposal.getTitle());
                        tvDateTime.setText(formatDateTime(currentProposal.getDate()));
                        tvVenue.setText(currentProposal.getLocation());
                        tvDescription.setText(currentProposal.getDescription());
                        ivBanner.setImageResource(R.drawable.bg_placeholder_image);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading proposal", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "Date TBD";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}
