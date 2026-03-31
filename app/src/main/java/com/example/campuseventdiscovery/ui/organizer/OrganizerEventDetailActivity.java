package com.example.campuseventdiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

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
            startActivity(intent);
        });
        btnAnnouncement.setOnClickListener(v -> 
            Toast.makeText(this, "Announcement feature coming soon", Toast.LENGTH_SHORT).show());
    }

    private void loadEventDetails() {
        if (TextUtils.isEmpty(eventId)) {
            finish();
            return;
        }

        repository.getEventById(eventId, new EventRepository.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                tvTitle.setText(event.getTitle());
                tvDateTime.setText(formatDateTime(event.getDate()));
                tvVenue.setText(event.getLocation());
                
                long rsvp = event.getRsvpCount();
                long capacity = event.getCapacity();
                tvRegCount.setText(rsvp + "/" + capacity);
                
                if (capacity > 0) {
                    pbRegistrations.setProgress((int) ((rsvp * 100) / capacity));
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

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "Date TBD";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}