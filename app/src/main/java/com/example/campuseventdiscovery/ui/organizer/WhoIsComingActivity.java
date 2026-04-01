package com.example.campuseventdiscovery.ui.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.AttendeeAdapter;
import com.example.campuseventdiscovery.model.EventAttendee;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Screen for organizers to view attendees, check them in (via code or mock scanner), and blacklist them.
 */
public class WhoIsComingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private EditText etSearchParticipants;
    private RecyclerView rvParticipants;
    private TextView tvEmptyParticipants;
    private MaterialButton btnScanQr;
    private MaterialButton btnCheckIn;
    private MaterialButton btnBlacklist;

    private EventRepository repository;
    private AttendeeAdapter adapter;
    private String eventId;
    private String eventTitle;

    // Mock scanner for the prototype
    private final ActivityResultLauncher<Intent> qrScannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedCode = result.getData().getStringExtra("scanned_code");
                    if (!TextUtils.isEmpty(scannedCode)) {
                        performCheckIn(scannedCode);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_who_is_coming);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");

        bindViews();
        btnBack.setOnClickListener(v -> finish());
        tvTitle.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.who_is_coming) : eventTitle);

        setupRecyclerView();
        setupSearch();
        setupScanQrAction();
        setupCheckInAction();
        setupBlacklistAction();
        loadParticipants();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        etSearchParticipants = findViewById(R.id.etSearchParticipants);
        rvParticipants = findViewById(R.id.rvParticipants);
        tvEmptyParticipants = findViewById(R.id.tvEmptyParticipants);
        btnScanQr = findViewById(R.id.btnScanQr);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnBlacklist = findViewById(R.id.btnBlacklist);
    }

    private void setupRecyclerView() {
        adapter = new AttendeeAdapter(selectedCount -> btnBlacklist.setEnabled(selectedCount > 0));
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        rvParticipants.setAdapter(adapter);
        btnBlacklist.setEnabled(false);
    }

    private void setupSearch() {
        etSearchParticipants.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }

    private void setupScanQrAction() {
        btnScanQr.setOnClickListener(v -> {
            // In a real product, this would launch a ZXing or CameraX scanner.
            // For the prototype, we show a dialog to "simulate" a scan.
            EditText input = new EditText(this);
            input.setHint("Paste QR code token here (Simulated Scan)");
            
            new AlertDialog.Builder(this)
                    .setTitle(R.string.scan_qr_code)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String code = input.getText().toString().trim();
                        if (!TextUtils.isEmpty(code)) {
                            performCheckIn(code);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void setupCheckInAction() {
        btnCheckIn.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint(R.string.check_in_code_hint);
            input.setPadding(32, 24, 32, 24);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.check_in_title)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String code = input.getText().toString().trim();
                        if (TextUtils.isEmpty(code)) {
                            Toast.makeText(this, getString(R.string.check_in_requires_code), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        performCheckIn(code);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void performCheckIn(String code) {
        repository.checkInAttendeeByQrToken(eventId, code, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(
                        WhoIsComingActivity.this,
                        getString(R.string.check_in_success),
                        Toast.LENGTH_SHORT
                ).show();
                loadParticipants();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        WhoIsComingActivity.this,
                        e.getMessage() == null ? getString(R.string.check_in_failed) : e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void setupBlacklistAction() {
        btnBlacklist.setOnClickListener(v -> {
            List<EventAttendee> selectedAttendees = adapter.getSelectedAttendees();
            if (selectedAttendees.isEmpty()) {
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.blacklist_confirm_title)
                    .setMessage(getString(R.string.blacklist_confirm_message, selectedAttendees.size()))
                    .setPositiveButton(R.string.blacklist_selected, (dialog, which) ->
                            repository.blacklistAttendees(eventId, selectedAttendees, new EventRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(
                                            WhoIsComingActivity.this,
                                            getString(R.string.blacklist_success),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    loadParticipants();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(
                                            WhoIsComingActivity.this,
                                            getString(R.string.blacklist_failed),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void loadParticipants() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, getString(R.string.error_loading_participants), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository.getEventAttendees(eventId, new EventRepository.AttendeeListCallback() {
            @Override
            public void onSuccess(List<EventAttendee> attendees) {
                adapter.updateData(attendees);
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        WhoIsComingActivity.this,
                        getString(R.string.error_loading_participants),
                        Toast.LENGTH_SHORT
                ).show();
                adapter.updateData(null);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        tvEmptyParticipants.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvParticipants.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
