package com.example.CampusEventDiscovery.ui.organizer;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.PaymentConfirmationActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.EventShareHelper;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private MaterialToolbar toolbarOrganizerEventDetail;
    private ImageButton btnShare;
    private TextView tvTitle, tvDateTime, tvVenue, tvRegCount;
    private ProgressBar pbRegistrations, progressBarDeleteEvent;
    private MaterialButton btnWhoIsComing, btnAnnouncement, btnPayments, btnBlacklisted, btnDeleteEvent;

    private EventRepository repository;
    private String eventId;
    private String currentUserId;
    private String currentRole = "";
    private Event currentEvent;
    private ListenerRegistration eventListener;
    private boolean walkthroughMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_detail);

        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();
        eventId = getIntent().getStringExtra("eventId");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : DevSessionManager.getEffectiveUserId(this);
        currentRole = DevSessionManager.shouldUseBypass(this) ? DevSessionManager.getBypassRole(this) : "";

        bindViews();
        setupListeners();
        loadCurrentRole();
        loadEventDetails();
        if (walkthroughMode) {
            currentEvent = WalkthroughManager.getDemoEvent();
            bindEvent(currentEvent);
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "organizer_event_detail");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (walkthroughMode) {
            return;
        }
        startEventListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopEventListener();
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        toolbarOrganizerEventDetail = findViewById(R.id.toolbarOrganizerEventDetail);
        btnShare = findViewById(R.id.btnShare);
        tvTitle = findViewById(R.id.tvTitle);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvRegCount = findViewById(R.id.tvRegCount);
        pbRegistrations = findViewById(R.id.pbRegistrations);
        progressBarDeleteEvent = findViewById(R.id.progressBarDeleteEvent);
        btnWhoIsComing = findViewById(R.id.btnWhoIsComing);
        btnAnnouncement = findViewById(R.id.btnAnnouncement);
        btnPayments = findViewById(R.id.btnPayments);
        btnBlacklisted = findViewById(R.id.btnBlacklisted);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
    }

    private void setupListeners() {
        toolbarOrganizerEventDetail.setNavigationOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> {
            if (currentEvent != null) {
                EventShareHelper.showEventShareOptions(this, currentEvent);
            }
        });
        btnWhoIsComing.setOnClickListener(v -> {
            Intent intent = new Intent(this, WhoIsComingActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", currentEvent != null ? currentEvent.getTitle() : "");
            startActivity(intent);
        });
        btnAnnouncement.setOnClickListener(v -> showAnnouncementDialog());
        btnPayments.setOnClickListener(v -> {
            if (walkthroughMode) {
                Toast.makeText(this, "Walkthrough mode: payment records were not opened.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PaymentConfirmationActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", currentEvent != null ? currentEvent.getTitle() : "");
            startActivity(intent);
        });
        btnBlacklisted.setOnClickListener(v -> {
            Intent intent = new Intent(this, WhoIsComingActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", currentEvent != null ? currentEvent.getTitle() : "");
            intent.putExtra("showBlacklisted", true);
            startActivity(intent);
        });
        btnDeleteEvent.setOnClickListener(v -> confirmDeleteEvent());
    }

    private void loadCurrentRole() {
        if (walkthroughMode) {
            currentRole = UserRoles.ORGANIZER;
            bindDeleteVisibility();
            return;
        }

        if (DevSessionManager.shouldUseBypass(this)) {
            currentRole = DevSessionManager.getBypassRole(this);
            bindDeleteVisibility();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            bindDeleteVisibility();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                currentRole = user == null ? "" : UserRoles.sanitize(user.getRole());
                bindDeleteVisibility();
            }

            @Override
            public void onError(Exception e) {
                currentRole = "";
                bindDeleteVisibility();
            }
        });
    }

    private void bindDeleteVisibility() {
        boolean canDelete = UserRoles.canManageEvents(currentRole);
        btnDeleteEvent.setVisibility(canDelete ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void confirmDeleteEvent() {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_event_confirm_title)
                .setMessage(R.string.delete_event_confirm_message)
                .setPositiveButton(R.string.delete_event, (dialog, which) -> deleteEvent())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteEvent() {
        if (walkthroughMode) {
            Toast.makeText(this, "Walkthrough mode: event was not deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        setDeleteLoading(true);
        repository.deleteEvent(eventId, currentUserId, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrganizerEventDetailActivity.this, R.string.event_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setDeleteLoading(false);
                String message = e != null && !TextUtils.isEmpty(e.getMessage())
                        ? e.getMessage()
                        : getString(R.string.event_delete_failed);
                Toast.makeText(OrganizerEventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDeleteLoading(boolean isLoading) {
        btnDeleteEvent.setEnabled(!isLoading);
        progressBarDeleteEvent.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    private void loadEventDetails() {
        if (walkthroughMode) {
            return;
        }

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
                bindEvent(event);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OrganizerEventDetailActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindEvent(Event event) {
        if (event == null) {
            return;
        }
        tvTitle.setText(event.getTitle());
        tvDateTime.setText(formatDateTime(event.getDate()));

        String displayLocation = event.getLocation();
        if (!TextUtils.isEmpty(event.getLocationDescription()) && !TextUtils.isEmpty(event.getLocationKey())) {
            displayLocation = event.getLocationDescription() + ", " + event.getLocationKey();
        } else if (!TextUtils.isEmpty(event.getLocationKey())) {
            displayLocation = event.getLocationKey();
        }
        tvVenue.setText(displayLocation);

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

    private void stopEventListener() {
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    private void showAnnouncementDialog() {
        if (walkthroughMode) {
            Toast.makeText(this, "Walkthrough mode: announcement was not sent.", Toast.LENGTH_SHORT).show();
            return;
        }

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
