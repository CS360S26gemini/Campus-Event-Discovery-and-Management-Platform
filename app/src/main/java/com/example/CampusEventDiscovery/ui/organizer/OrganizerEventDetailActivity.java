package com.example.CampusEventDiscovery.ui.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * OrganizerEventDetailActivity.java
 *
 * Organizer-facing event management screen showing registration counts and actions.
 */
public class OrganizerEventDetailActivity extends AppCompatActivity {

    private ImageView ivBanner;
    private ImageButton btnBack, btnShare;
    private TextView tvTitle, tvDateTime, tvVenue, tvRegCount;
    private ProgressBar pbRegistrations;
    private MaterialButton btnWhoIsComing, btnAnnouncement;

    private EventRepository repository;
    private String eventId;
    private Event currentEvent;
    private ListenerRegistration eventListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_detail);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra("eventId");

        bindViews();
        setupListeners();
        loadEventDetails();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startEventListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopEventListener();
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        tvTitle = findViewById(R.id.tvTitle);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvRegCount = findViewById(R.id.tvRegCount);
        pbRegistrations = findViewById(R.id.pbRegistrations);
        btnWhoIsComing = findViewById(R.id.btnWhoIsComing);
        btnAnnouncement = findViewById(R.id.btnAnnouncement);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnWhoIsComing.setOnClickListener(v -> {
            Intent intent = new Intent(this, WhoIsComingActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", currentEvent != null ? currentEvent.getTitle() : "");
            startActivity(intent);
        });
        btnAnnouncement.setOnClickListener(v -> showAnnouncementDialog());
    }

    private void loadEventDetails() {
        if (TextUtils.isEmpty(eventId)) {
            finish();
        }
    }

    private void startEventListener() {
        if (TextUtils.isEmpty(eventId)) {
            finish();
            return;
        }

        stopEventListener();
        eventListener = repository.observeEventById(eventId, new EventRepository.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                tvTitle.setText(event.getTitle());
                tvDateTime.setText(formatDateTime(event.getDate()));
                tvVenue.setText(event.getLocation());

                long rsvp = event.getRsvpCount();
                long capacity = event.getCapacity();
                long checkedIn = event.getCheckedInCount();
                tvRegCount.setText(checkedIn + " attended • " + rsvp + "/" + capacity);

                if (capacity > 0) {
                    pbRegistrations.setProgress((int) ((rsvp * 100) / capacity));
                } else {
                    pbRegistrations.setProgress(0);
                }

                if (!TextUtils.isEmpty(event.getThumbnailUrl())) {
                    Glide.with(OrganizerEventDetailActivity.this)
                            .load(event.getThumbnailUrl())
                            .placeholder(R.drawable.bg_placeholder_image)
                            .into(ivBanner);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OrganizerEventDetailActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopEventListener() {
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    private void showAnnouncementDialog() {
        if (currentEvent == null || TextUtils.isEmpty(currentEvent.getEventId())) {
            return;
        }

        EditText input = new EditText(this);
        input.setHint(R.string.announcement_hint);
        input.setMinLines(3);
        input.setMaxLines(5);
        input.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
                .setTitle(R.string.announcement_title)
                .setView(input)
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (TextUtils.isEmpty(message)) {
                        Toast.makeText(this, getString(R.string.announcement_requires_message), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    repository.sendAnnouncementToAttendees(
                            currentEvent.getEventId(),
                            currentEvent.getTitle(),
                            message,
                            new EventRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(
                                            OrganizerEventDetailActivity.this,
                                            getString(R.string.announcement_sent),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(
                                            OrganizerEventDetailActivity.this,
                                            getString(R.string.announcement_failed),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                    );
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
