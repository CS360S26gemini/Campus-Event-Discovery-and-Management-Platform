package com.example.CampusEventDiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.AttendeeAdapter;
import com.example.CampusEventDiscovery.model.EventAttendee;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * WhoIsComingActivity.java
 *
 * Screen for organizers to view attendees, mark them as attended via
 * QR scanner or manual code entry, and blacklist attendees. Uses a
 * real-time Firestore snapshot listener so the list updates instantly
 * after a scan without flicker or stale data.
 */
public class WhoIsComingActivity extends AppCompatActivity {

    private MaterialToolbar toolbarWhoIsComing;
    private TextView tvTitle;
    private TextView tvEventContext;
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
    private boolean showBlacklisted;
    private ListenerRegistration attendeeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_who_is_coming);

        repository = new EventRepository();
        eventId    = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");
        showBlacklisted = getIntent().getBooleanExtra("showBlacklisted", false);

        bindViews();
        toolbarWhoIsComing.setNavigationOnClickListener(v -> finish());
        tvTitle.setText(showBlacklisted ? R.string.blacklisted_attendees : R.string.who_is_coming);
        if (TextUtils.isEmpty(eventTitle)) {
            tvEventContext.setVisibility(View.GONE);
        } else {
            tvEventContext.setText(getString(R.string.event_context_label, eventTitle));
            tvEventContext.setVisibility(View.VISIBLE);
        }

        setupRecyclerView();
        setupSearch();
        setupScanQrAction();
        setupCheckInAction();
        setupBlacklistAction();
        bindMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showBlacklisted) {
            loadBlacklistedAttendees();
        } else {
            startAttendeesListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAttendeesListener();
    }

    private void bindViews() {
        toolbarWhoIsComing   = findViewById(R.id.toolbarWhoIsComing);
        tvTitle              = findViewById(R.id.tvTitle);
        tvEventContext       = findViewById(R.id.tvEventContext);
        etSearchParticipants = findViewById(R.id.etSearchParticipants);
        rvParticipants       = findViewById(R.id.rvParticipants);
        tvEmptyParticipants  = findViewById(R.id.tvEmptyParticipants);
        btnScanQr            = findViewById(R.id.btnScanQr);
        btnCheckIn           = findViewById(R.id.btnCheckIn);
        btnBlacklist         = findViewById(R.id.btnBlacklist);
    }

    private void bindMode() {
        btnScanQr.setVisibility(showBlacklisted ? View.GONE : View.VISIBLE);
        btnCheckIn.setVisibility(showBlacklisted ? View.GONE : View.VISIBLE);
        btnBlacklist.setVisibility(showBlacklisted ? View.GONE : View.VISIBLE);
        tvEmptyParticipants.setText(showBlacklisted
                ? R.string.no_blacklisted_attendees
                : R.string.no_participants_yet);
    }

    private void setupRecyclerView() {
        adapter = new AttendeeAdapter(
                selectedCount -> btnBlacklist.setEnabled(selectedCount > 0));
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        rvParticipants.setAdapter(adapter);
        btnBlacklist.setEnabled(false);
    }

    /**
     * Starts a real-time Firestore snapshot listener on the event's
     * attendees subcollection. The list updates automatically whenever
     * any attendee document changes — no manual refresh needed, no flicker.
     */
    private void startAttendeesListener() {
        if (TextUtils.isEmpty(eventId)) return;
        stopAttendeesListener();

        attendeeListener = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("attendees")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<EventAttendee> attendees = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        EventAttendee a = doc.toObject(EventAttendee.class);
                        if (a != null) {
                            if (TextUtils.isEmpty(a.getUserId())) {
                                a.setUserId(doc.getId());
                            }
                            attendees.add(a);
                        }
                    }

                    Collections.sort(attendees, Comparator.comparing(
                            a -> a.getFullName() == null
                                    ? "" : a.getFullName().toLowerCase()));

                    adapter.updateData(attendees);
                    updateEmptyState();
                });
    }

    private void loadBlacklistedAttendees() {
        stopAttendeesListener();
        repository.getBlacklistedAttendees(eventId, new EventRepository.AttendeeListCallback() {
            @Override
            public void onSuccess(List<EventAttendee> attendees) {
                adapter.updateData(attendees);
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                adapter.updateData(new ArrayList<>());
                updateEmptyState();
            }
        });
    }

    private void stopAttendeesListener() {
        if (attendeeListener != null) {
            attendeeListener.remove();
            attendeeListener = null;
        }
    }

    private void setupSearch() {
        etSearchParticipants.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyState();
            }
        });
    }

    /**
     * Opens ScannerActivity which uses the ZXing camera to scan the
     * attendee's QR code and mark them as attended.
     */
    private void setupScanQrAction() {
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", eventTitle);
            startActivity(intent);
        });
    }

    /**
     * Manual fallback — allows the organizer to type an attendee's
     * qrCodeToken directly if the camera is unavailable.
     */
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
                            Toast.makeText(this,
                                    getString(R.string.check_in_requires_code),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        performManualAttendance(code);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    /**
     * Marks an attendee as attended using their plain qrCodeToken.
     * The attendees list updates automatically via the snapshot listener.
     *
     * @param code The attendee's qrCodeToken string.
     */
    private void performManualAttendance(String code) {
        repository.checkInAttendeeByQrToken(eventId, code,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WhoIsComingActivity.this,
                                getString(R.string.check_in_success),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(WhoIsComingActivity.this,
                                e.getMessage() == null
                                        ? getString(R.string.check_in_failed)
                                        : e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBlacklistAction() {
        btnBlacklist.setOnClickListener(v -> {
            List<EventAttendee> selected = adapter.getSelectedAttendees();
            if (selected.isEmpty()) return;

            new AlertDialog.Builder(this)
                    .setTitle(R.string.blacklist_confirm_title)
                    .setMessage(getString(
                            R.string.blacklist_confirm_message, selected.size()))
                    .setPositiveButton(R.string.blacklist_selected, (dialog, which) ->
                            repository.blacklistAttendees(eventId, selected,
                                    new EventRepository.ActionCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(WhoIsComingActivity.this,
                                                    getString(R.string.blacklist_success),
                                                    Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Toast.makeText(WhoIsComingActivity.this,
                                                    getString(R.string.blacklist_failed),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        tvEmptyParticipants.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvParticipants.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
