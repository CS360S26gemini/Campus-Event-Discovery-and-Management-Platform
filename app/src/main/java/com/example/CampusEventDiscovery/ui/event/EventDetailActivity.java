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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.TicketTierOptionAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.TicketTier;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.EventShareHelper;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
public class EventDetailActivity extends AppCompatActivity {

    private ImageView ivBanner;
    private MaterialToolbar toolbarEventDetail;
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
    private TextView tvTicketTierLabel;
    private RecyclerView rvTicketTiers;
    private com.google.android.material.button.MaterialButton btnTickets;
    private com.google.android.material.button.MaterialButton btnViewTicket;

    private EventRepository repository;
    private String eventId;
    private String currentUserId;
    private String currentUserRole = "";
    private Event currentEvent;
    private Rsvp currentRsvp;
    private final List<TicketTier> ticketTiers = new ArrayList<>();
    private TicketTierOptionAdapter ticketTierPreviewAdapter;
    private final Set<String> savedEventIds = new HashSet<>();
    private boolean walkthroughMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        updateRegistrationCta();

        eventId = resolveEventId(getIntent());
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, getString(R.string.event_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupStaticListeners();
        if (walkthroughMode) {
            currentUserRole = UserRoles.ATTENDEE;
            currentEvent = WalkthroughManager.getDemoEvent();
            bindEvent(currentEvent);
            bindTicketTiers(new ArrayList<>());
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "event_detail");
        } else {
            loadSavedState();
            loadCurrentUserRole();
            loadEventDetails();
            checkRsvpStatus();
        }
    }

    private void bindViews() {
        ivBanner = findViewById(R.id.ivBanner);
        toolbarEventDetail = findViewById(R.id.toolbarEventDetail);
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
        tvTicketTierLabel = findViewById(R.id.tvTicketTierLabel);
        rvTicketTiers = findViewById(R.id.rvTicketTiers);
        btnTickets = findViewById(R.id.btnTickets);
        ticketTierPreviewAdapter = new TicketTierOptionAdapter(false, null);
        rvTicketTiers.setLayoutManager(new LinearLayoutManager(this));
        rvTicketTiers.setNestedScrollingEnabled(false);
        rvTicketTiers.setAdapter(ticketTierPreviewAdapter);
        
        View rawBtn = findViewById(R.id.btnViewTicket);
        if (rawBtn instanceof com.google.android.material.button.MaterialButton) {
            btnViewTicket = (com.google.android.material.button.MaterialButton) rawBtn;
        }
    }

    private void setupStaticListeners() {
        toolbarEventDetail.setNavigationOnClickListener(v -> finish());

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

            EventShareHelper.showEventShareOptions(this, currentEvent);
        });

        tvAddToCalendar.setOnClickListener(v -> addToCalendar());

        tvViewOnMap.setOnClickListener(v -> openMap());

        btnTickets.setOnClickListener(v -> {
            if (walkthroughMode) {
                Toast.makeText(this, "Walkthrough mode: no RSVP was created.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentEvent == null || !UserRoles.isAttendee(currentUserRole)) {
                Toast.makeText(this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                return;
            }

            long remaining = getEffectiveRemainingSpots();
            if (remaining <= 0) {
                Toast.makeText(this,
                        hasVisibleTiers() ? getString(R.string.ticket_tier_sold_out_message) : getString(R.string.sold_out_toast),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (hasVisibleTiers()) {
                Intent intent = new Intent(EventDetailActivity.this, BuyTicketActivity.class);
                intent.putExtra("eventId", currentEvent.getEventId());
                intent.putExtra("eventTitle", safeText(currentEvent.getTitle(), getString(R.string.app_name)));
                intent.putExtra("eventDateMillis", currentEvent.getDate() != null
                        ? currentEvent.getDate().toDate().getTime() : -1L);
                intent.putExtra("eventVenue", safeText(resolveDisplayLocation(currentEvent), ""));
                intent.putExtra("eventCapacity", currentEvent.getCapacity());
                intent.putExtra("eventRsvpCount", currentEvent.getRsvpCount());
                intent.putExtra("eventTicketPrice", currentEvent.getTicketPrice());
                startActivity(intent);
            } else {
                Intent intent = new Intent(EventDetailActivity.this, CheckoutActivity.class);
                intent.putExtra("eventId", currentEvent.getEventId());
                intent.putExtra("eventTitle", safeText(currentEvent.getTitle(), getString(R.string.app_name)));
                intent.putExtra("totalPrice", currentEvent.getTicketPrice());
                intent.putExtra("eventDateMillis", currentEvent.getDate() != null
                        ? currentEvent.getDate().toDate().getTime() : -1L);
                intent.putExtra("eventVenue", safeText(resolveDisplayLocation(currentEvent), ""));
                startActivity(intent);
            }
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
                if (btnTickets != null) {
                    btnTickets.setEnabled(false);
                    btnTickets.setAlpha(0.6f);
                }
                loadTicketTiers();
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
        tvVenue.setText(resolveDisplayLocation(event));
        tvRefundPolicy.setText(getString(R.string.refund_policy_body));
        tvDescription.setText(safeText(event.getDescription(), getString(R.string.placeholder_description)));

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

        bindTicketPricingSummary();
        updateHeartIcon();
        updateRegistrationCta();
    }

    private void loadTicketTiers() {
        repository.getTiersForEvent(eventId, new EventRepository.TicketTierListCallback() {
            @Override
            public void onSuccess(List<TicketTier> tiers) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                bindTicketTiers(tiers);
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                bindTicketTiers(new ArrayList<>());
            }
        });
    }

    private void bindTicketTiers(List<TicketTier> tiers) {
        ticketTiers.clear();
        if (tiers != null) {
            ticketTiers.addAll(tiers);
        }
        ticketTierPreviewAdapter.submitList(ticketTiers);
        boolean hasTiers = hasVisibleTiers();
        tvTicketTierLabel.setVisibility(hasTiers ? View.VISIBLE : View.GONE);
        rvTicketTiers.setVisibility(hasTiers ? View.VISIBLE : View.GONE);
        bindTicketPricingSummary();
        updateRegistrationCta();
    }

    private void bindTicketPricingSummary() {
        if (currentEvent == null) {
            return;
        }

        if (hasVisibleTiers()) {
            double lowestPrice = Double.MAX_VALUE;
            long totalRemaining = 0L;
            boolean hasAvailableTier = false;

            for (TicketTier tier : ticketTiers) {
                lowestPrice = Math.min(lowestPrice, tier.getPrice());
                totalRemaining += tier.getRemainingCapacity();
                hasAvailableTier = hasAvailableTier || !tier.isSoldOut();
            }

            tvPrice.setText(lowestPrice <= 0.0
                    ? getString(R.string.price_free)
                    : getString(R.string.ticket_tier_preview_price, lowestPrice));
            if (!hasAvailableTier) {
                tvSpotsRemaining.setText(getString(R.string.sold_out));
            } else {
                tvSpotsRemaining.setText(getString(R.string.ticket_tier_spots_remaining_long, (int) totalRemaining));
            }
        } else {
            if (currentEvent.getTicketPrice() == 0) {
                tvPrice.setText(getString(R.string.price_free));
            } else {
                tvPrice.setText(String.format(Locale.getDefault(), "PKR %.2f", currentEvent.getTicketPrice()));
            }

            long remaining = Math.max(0L, currentEvent.getCapacity() - currentEvent.getRsvpCount());
            if (remaining <= 0) {
                tvSpotsRemaining.setText(getString(R.string.sold_out));
            } else {
                tvSpotsRemaining.setText(getString(R.string.spots_remaining, (int) remaining));
            }
        }
    }

    private void updateHeartIcon() {
        if (currentEvent == null || currentEvent.getEventId() == null) {
            btnHeart.setImageResource(R.drawable.ic_heart_outline);
            return;
        }

        boolean isSaved = savedEventIds.contains(currentEvent.getEventId());
        btnHeart.setImageResource(isSaved ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnHeart.setColorFilter(isSaved
                ? ThemeManager.getAccentColor(this)
                : ContextCompat.getColor(this, R.color.colorOnSurfaceVariant));
    }

    private void addToCalendar() {
        if (currentEvent == null) {
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, safeText(currentEvent.getTitle(), getString(R.string.app_name)))
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, tvVenue.getText().toString());

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
        if (currentEvent == null) {
            return;
        }

        // Internal Map Overriding Google Maps redirect
        String locationKey = currentEvent.getLocationKey();
        if (!TextUtils.isEmpty(locationKey)) {
            Intent intent = new Intent(this, CampusMapActivity.class);
            intent.putExtra("locationKey", locationKey);
            startActivity(intent);
        } else {
            // Fallback for events that don't have a specific key yet - try to match from location string
            String matchedKey = null;
            String location = currentEvent.getLocation() != null ? currentEvent.getLocation().toUpperCase() : "";
            
            if (location.contains(Constants.MAP_LOC_SSE)) matchedKey = Constants.MAP_LOC_SSE;
            else if (location.contains(Constants.MAP_LOC_HSS)) matchedKey = Constants.MAP_LOC_HSS;
            else if (location.contains(Constants.MAP_LOC_SAHSOL)) matchedKey = Constants.MAP_LOC_SAHSOL;
            else if (location.contains("SPORTS") || location.contains("COMPLEX")) matchedKey = Constants.MAP_LOC_SPORTS_COMPLEX;
            else if (location.contains("PARKING")) matchedKey = Constants.MAP_LOC_PARKING_LOT;
            else if (location.contains("REDC")) matchedKey = Constants.MAP_LOC_REDC;
            else if (location.contains("CRICKET") || location.contains("GROUND")) matchedKey = Constants.MAP_LOC_CRICKET_GROUND;
            else if (location.contains(Constants.MAP_LOC_SDSB)) matchedKey = Constants.MAP_LOC_SDSB;
            else if (location.contains(Constants.MAP_LOC_IST)) matchedKey = Constants.MAP_LOC_IST;
            else if (location.contains("MASJID")) matchedKey = Constants.MAP_LOC_MASJID;

            if (matchedKey != null) {
                Intent intent = new Intent(this, CampusMapActivity.class);
                intent.putExtra("locationKey", matchedKey);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Internal campus map not available for this location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void trackRecentlyViewed(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }

        String prefsName = Constants.PREFS_RECENTLY_VIEWED;
        String key = Constants.PREFS_RECENTLY_VIEWED_KEY;

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

    private String resolveEventId(Intent intent) {
        if (intent == null) {
            return "";
        }

        String extraEventId = intent.getStringExtra("eventId");
        if (!TextUtils.isEmpty(extraEventId)) {
            return extraEventId;
        }

        return EventShareHelper.eventIdFromUri(intent.getData());
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
        btnTickets.setText(hasVisibleTiers() ? R.string.choose_ticket_tier : R.string.tickets_button);
    }

    private long getEffectiveRemainingSpots() {
        if (hasVisibleTiers()) {
            long remaining = 0L;
            for (TicketTier tier : ticketTiers) {
                remaining += tier.getRemainingCapacity();
            }
            return remaining;
        }
        return Math.max(0L, currentEvent != null ? currentEvent.getCapacity() - currentEvent.getRsvpCount() : 0L);
    }

    private boolean hasVisibleTiers() {
        return !ticketTiers.isEmpty();
    }

    private String resolveDisplayLocation(Event event) {
        if (event == null) {
            return getString(R.string.placeholder_venue);
        }

        String displayLocation = event.getLocation();
        if (!TextUtils.isEmpty(event.getLocationDescription()) && !TextUtils.isEmpty(event.getLocationKey())) {
            displayLocation = event.getLocationDescription() + ", " + event.getLocationKey();
        } else if (!TextUtils.isEmpty(event.getLocationKey())) {
            displayLocation = event.getLocationKey();
        }
        return safeText(displayLocation, getString(R.string.placeholder_venue));
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
