package com.example.CampusEventDiscovery.ui.event;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

/**
 * EventDetailActivity.java
 *
 * Shows full event details for a selected event.
 * Updated to show "View Ticket" if already registered.
 */

// export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
//export PATH="$JAVA_HOME/bin:$PATH"
public class EventDetailActivity extends AppCompatActivity {

    private ImageView ivBanner;
    private ImageButton btnBack;
    private ImageButton btnHeart;
    private ImageButton btnShare;
    private TextView tvTitle;
    private TextView tvDateTime;
    private TextView tvAddToCalendar;
    private TextView tvVenue;
    private TextView tvViewOnMap;
    private TextView tvRefundPolicy;
    private TextView tvDescription;
    private TextView tvSpotsRemaining;
    private TextView tvPrice;
    private com.google.android.material.button.MaterialButton btnTickets;
    private com.google.android.material.button.MaterialButton btnViewTicket;

    private EventRepository repository;
    private String eventId;
    private String currentUserId;
    private String currentUserRole = "";
    private Event currentEvent;
    private Rsvp currentRsvp;
    private final Set<String> savedEventIds = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        updateRegistrationCta();

        eventId = getIntent().getStringExtra("eventId");
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, getString(R.string.event_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupStaticListeners();
        loadSavedState();
        loadCurrentUserRole();
        loadEventDetails();
        checkRsvpStatus();
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        btnBack = findViewById(R.id.btnBack);
        btnHeart = findViewById(R.id.btnHeart);
        btnShare = findViewById(R.id.btnShare);
        tvTitle = findViewById(R.id.tvTitle);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvAddToCalendar = findViewById(R.id.tvAddToCalendar);
        tvVenue = findViewById(R.id.tvVenue);
        tvViewOnMap = findViewById(R.id.tvViewOnMap);
        tvRefundPolicy = findViewById(R.id.tvRefundPolicy);
        tvDescription = findViewById(R.id.tvDescription);
        tvSpotsRemaining = findViewById(R.id.tvSpotsRemaining);
        tvPrice = findViewById(R.id.tvPrice);
        btnTickets = findViewById(R.id.btnTickets);
        
        // Add dynamic View Ticket button if not in layout, or bind it
//        btnViewTicket = findViewById(R.id.btnViewTicket);
        // btnViewTicket is optional — only bind if present in layout
        View rawBtn = findViewById(R.id.btnViewTicket);
        if (rawBtn instanceof com.google.android.material.button.MaterialButton) {
            btnViewTicket = (com.google.android.material.button.MaterialButton) rawBtn;
        }
    }

    private void setupStaticListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnHeart.setOnClickListener(v -> {
            if (currentEvent == null || currentUserId == null || currentEvent.getEventId() == null) {
                return;
            }

            boolean isSaved = savedEventIds.contains(currentEvent.getEventId());

            if (isSaved) {
                repository.unsaveEvent(currentUserId, currentEvent.getEventId(), () -> {
                    savedEventIds.remove(currentEvent.getEventId());
                    updateHeartIcon();
                });
            } else {
                repository.saveEvent(currentUserId, currentEvent, new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        savedEventIds.add(currentEvent.getEventId());
                        updateHeartIcon();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(EventDetailActivity.this, "Failed to save event", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnShare.setOnClickListener(v -> {
            if (currentEvent == null) {
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    safeText(currentEvent.getTitle(), getString(R.string.app_name))
                            + "\n"
                            + formatDateTime(currentEvent.getDate())
                            + "\n"
                            + safeText(currentEvent.getLocation(), getString(R.string.placeholder_venue)));

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
        });

        tvAddToCalendar.setOnClickListener(v -> addToCalendar());

        tvViewOnMap.setOnClickListener(v -> openMap());

        btnTickets.setOnClickListener(v -> {
            if (currentEvent == null || !UserRoles.isAttendee(currentUserRole)) {
                Toast.makeText(this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                return;
            }

            long remaining = Math.max(0L, currentEvent.getCapacity() - currentEvent.getRsvpCount());
            if (remaining <= 0) {
                Toast.makeText(this, getString(R.string.sold_out_toast), Toast.LENGTH_SHORT).show();
                return;
            }

            // Launch CheckoutActivity for payment and QR ticket generation
            Intent intent = new Intent(EventDetailActivity.this, CheckoutActivity.class);
            intent.putExtra("eventId", currentEvent.getEventId());
            intent.putExtra("eventTitle", safeText(currentEvent.getTitle(), getString(R.string.app_name)));
            intent.putExtra("totalPrice", currentEvent.getTicketPrice());
            intent.putExtra("eventDateMillis", currentEvent.getDate() != null
                    ? currentEvent.getDate().toDate().getTime() : -1L);
            intent.putExtra("eventVenue", safeText(currentEvent.getLocation(), ""));

            startActivity(intent);
        });

        if (btnViewTicket != null) {
            btnViewTicket.setOnClickListener(v -> {
                if (currentRsvp != null) {
                    Intent intent = new Intent(this, TicketActivity.class);
                    intent.putExtra("rsvpId", eventId);
                    intent.putExtra("eventName", currentEvent.getTitle());
                    intent.putExtra("eventDate", formatDateTime(currentEvent.getDate()));
                    intent.putExtra("transactionId", currentRsvp.getTransactionId());
                    intent.putExtra("qrPayload", currentRsvp.getQrPayload());
                    startActivity(intent);
                }
            });
        }
    }

    private void checkRsvpStatus() {
        if (currentUserId == null || eventId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .collection("rsvps").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentRsvp = doc.toObject(Rsvp.class);
                        if (currentRsvp != null && !"cancelled".equalsIgnoreCase(currentRsvp.getStatus())) {
                            showViewTicketButton();
                        }
                    }
                });
    }

    private void showViewTicketButton() {
        if (btnTickets != null) btnTickets.setVisibility(View.GONE);
        if (btnViewTicket != null) {
            btnViewTicket.setVisibility(View.VISIBLE);
            btnViewTicket.setText(R.string.view_ticket);
        }
    }

    private void loadCurrentUserRole() {
        if (DevSessionManager.shouldUseBypass(this)) {
            currentUserRole = DevSessionManager.getBypassRole(this);
            updateRegistrationCta();
            return;
        }

        if (currentUserId == null) {
            currentUserRole = "";
            updateRegistrationCta();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                currentUserRole = user == null ? "" : UserRoles.sanitize(user.getRole());
                updateRegistrationCta();
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                currentUserRole = "";
                updateRegistrationCta();
            }
        });
    }

    private void loadSavedState() {
        if (currentUserId == null) {
            updateHeartIcon();
            return;
        }

        repository.getSavedEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                savedEventIds.clear();
                for (Event event : events) {
                    if (event.getEventId() != null) {
                        savedEventIds.add(event.getEventId());
                    }
                }
                updateHeartIcon();
            }

            @Override
            public void onError(Exception e) {
                savedEventIds.clear();
                updateHeartIcon();
            }
        });
    }

    private void loadEventDetails() {
        repository.getEventById(eventId, new EventRepository.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                bindEvent(event);
                trackRecentlyViewed(event.getEventId());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailActivity.this, getString(R.string.failed_to_load_event), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindEvent(Event event) {
        tvTitle.setText(safeText(event.getTitle(), getString(R.string.app_name)));
        tvDateTime.setText(formatDateTime(event.getDate()));
        tvVenue.setText(safeText(event.getLocation(), getString(R.string.placeholder_venue)));
        tvRefundPolicy.setText(getString(R.string.refund_policy_body));
        tvDescription.setText(safeText(event.getDescription(), getString(R.string.placeholder_description)));
        
        if (event.getTicketPrice() == 0) {
            tvPrice.setText(getString(R.string.price_free));
        } else {
            tvPrice.setText(String.format(Locale.getDefault(), "PKR %.2f", event.getTicketPrice()));
        }

        long remaining = Math.max(0L, event.getCapacity() - event.getRsvpCount());
        if (remaining <= 0) {
            tvSpotsRemaining.setText(getString(R.string.sold_out));
        } else {
            tvSpotsRemaining.setText(getString(R.string.spots_remaining, (int) remaining));
        }

        if (!TextUtils.isEmpty(event.getThumbnailUrl())) {
            Glide.with(this)
                    .load(event.getThumbnailUrl())
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(ivBanner);
        } else {
            ivBanner.setImageResource(0);
            ivBanner.setBackgroundResource(R.drawable.bg_placeholder_image);
        }

        updateHeartIcon();
        updateRegistrationCta();
    }

    private void updateHeartIcon() {
        if (currentEvent == null || currentEvent.getEventId() == null) {
            btnHeart.setImageResource(R.drawable.ic_heart_outline);
            return;
        }

        boolean isSaved = savedEventIds.contains(currentEvent.getEventId());
        btnHeart.setImageResource(isSaved ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void addToCalendar() {
        if (currentEvent == null) {
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, safeText(currentEvent.getTitle(), getString(R.string.app_name)))
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, safeText(currentEvent.getLocation(), getString(R.string.placeholder_venue)));

            Timestamp start = currentEvent.getDate();

            if (start != null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toDate().getTime());
            }

            long endMillis = resolveEventEndMillis(currentEvent);
            if (endMillis > 0L) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            }

            startActivity(intent);
            repository.markRsvpAddedToCalendar(currentUserId, currentEvent.getEventId(), "");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.calendar_add_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void openMap() {
        if (currentEvent == null || TextUtils.isEmpty(currentEvent.getLocation())) {
            return;
        }

        try {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(currentEvent.getLocation()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.open_map_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void trackRecentlyViewed(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }

        String prefsName = "recently_viewed";
        String key = "event_ids";

        String existing = getSharedPreferences(prefsName, MODE_PRIVATE).getString(key, "");
        LinkedList<String> ids = new LinkedList<>();

        if (!TextUtils.isEmpty(existing)) {
            String[] split = existing.split(",");
            for (String id : split) {
                if (!TextUtils.isEmpty(id) && !id.equals(eventId) && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }

        ids.addFirst(eventId);

        while (ids.size() > 5) {
            ids.removeLast();
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            builder.append(ids.get(i));
            if (i < ids.size() - 1) {
                builder.append(",");
            }
        }

        getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .putString(key, builder.toString())
                .apply();
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return getString(R.string.placeholder_date);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private void updateRegistrationCta() {
        if (btnTickets == null) {
            return;
        }

        if (currentUserId == null) {
            btnTickets.setEnabled(false);
            btnTickets.setAlpha(0.6f);
            btnTickets.setText(R.string.sign_in_to_register);
            return;
        }

        if (!UserRoles.isAttendee(currentUserRole)) {
            btnTickets.setEnabled(false);
            btnTickets.setAlpha(0.6f);
            btnTickets.setText(R.string.attendee_only_button);
            return;
        }

        btnTickets.setEnabled(true);
        btnTickets.setAlpha(1f);
        btnTickets.setText(R.string.tickets_button);
    }

    private long resolveEventEndMillis(Event event) {
        if (event == null) {
            return 0L;
        }

        Timestamp end = event.getEndTime();
        if (end != null) {
            return end.toDate().getTime();
        }

        Timestamp start = event.getDate();
        if (start == null) {
            return 0L;
        }

        return start.toDate().getTime() + 2L * 60L * 60L * 1000L;
    }
}
