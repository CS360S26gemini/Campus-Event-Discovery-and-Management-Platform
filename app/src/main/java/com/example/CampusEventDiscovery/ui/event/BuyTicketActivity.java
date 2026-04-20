package com.example.CampusEventDiscovery.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.TicketTierAdapter;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * BuyTicketActivity.java
 *
 * Allows the user to choose ticket tiers and proceed to checkout.
 */
public class BuyTicketActivity extends AppCompatActivity {

    private MaterialToolbar toolbarBuyTicket;
    private TextView tvEventName;
    private TextView tvEventDate;
    private TextView tvEventVenue;
    private RecyclerView rvTiers;
    private TextView tvTotal;
    private MaterialButton btnBuy;

    private TicketTierAdapter adapter;
    private EventRepository repository;

    private String eventId;
    private String eventTitle;
    private long eventDateMillis;
    private String eventVenue;
    private long eventCapacity;
    private long eventRsvpCount;

    private final ActivityResultLauncher<Intent> checkoutLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_ticket);

        repository = new EventRepository();

        bindViews();
        readIntentExtras();
        setupToolbar();
        bindEventSummary();
        setupTicketTiers();
        enforceAttendeeAccess();
        setupBuyButton();
    }

    private void bindViews() {
        toolbarBuyTicket = findViewById(R.id.toolbarBuyTicket);
        tvEventName = findViewById(R.id.tvEventName);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventVenue = findViewById(R.id.tvEventVenue);
        rvTiers = findViewById(R.id.rvTiers);
        tvTotal = findViewById(R.id.tvTotal);
        btnBuy = findViewById(R.id.btnBuy);
    }

    private void readIntentExtras() {
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");
        eventDateMillis = getIntent().getLongExtra("eventDateMillis", -1L);
        eventVenue = getIntent().getStringExtra("eventVenue");
        eventCapacity = getIntent().getLongExtra("eventCapacity", 0L);
        eventRsvpCount = getIntent().getLongExtra("eventRsvpCount", 0L);
    }

    private void setupToolbar() {
        toolbarBuyTicket.setNavigationOnClickListener(v -> finish());
    }

    private void bindEventSummary() {
        tvEventName.setText(eventTitle != null ? eventTitle : getString(R.string.app_name));

        if (eventDateMillis > 0L) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("EEE, dd MMM • hh:mm a", java.util.Locale.getDefault());
            tvEventDate.setText(sdf.format(new java.util.Date(eventDateMillis)));
        } else {
            tvEventDate.setText(getString(R.string.placeholder_date));
        }

        tvEventVenue.setText(eventVenue != null ? eventVenue : getString(R.string.placeholder_venue));
        tvTotal.setText(getString(R.string.total_label, 0));
    }

    private void setupTicketTiers() {
        List<TicketTierAdapter.TicketTier> tiers = new ArrayList<>();
        tiers.add(new TicketTierAdapter.TicketTier(
                getString(R.string.ticket_tier_early_bird),
                getString(R.string.ticket_desc_early_bird),
                getString(R.string.ticket_date_range),
                2500,
                0
        ));
        tiers.add(new TicketTierAdapter.TicketTier(
                getString(R.string.ticket_tier_vip),
                getString(R.string.ticket_desc_vip),
                getString(R.string.ticket_date_range),
                10000,
                0
        ));
        tiers.add(new TicketTierAdapter.TicketTier(
                getString(R.string.ticket_tier_general),
                getString(R.string.ticket_desc_general),
                getString(R.string.ticket_date_range),
                3000,
                0
        ));

        adapter = new TicketTierAdapter(tiers, total ->
                tvTotal.setText(getString(R.string.total_label, total)));

        rvTiers.setLayoutManager(new LinearLayoutManager(this));
        rvTiers.setAdapter(adapter);
    }

    private void setupBuyButton() {
        btnBuy.setOnClickListener(v -> {
            int total = adapter.getTotalPrice();

            if (total == 0) {
                Toast.makeText(this, getString(R.string.please_select_ticket), Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(BuyTicketActivity.this, CheckoutActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", eventTitle);
            intent.putExtra("eventDateMillis", eventDateMillis);
            intent.putExtra("eventVenue", eventVenue);
            intent.putExtra("eventCapacity", eventCapacity);
            intent.putExtra("eventRsvpCount", eventRsvpCount);
            intent.putExtra("totalPrice", total);

            checkoutLauncher.launch(intent);
        });
    }

    private void enforceAttendeeAccess() {
        if (DevSessionManager.shouldUseBypass(this)) {
            if (!UserRoles.isAttendee(DevSessionManager.getBypassRole(this))) {
                Toast.makeText(this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (user == null || !UserRoles.isAttendee(user.getRole())) {
                    Toast.makeText(BuyTicketActivity.this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(BuyTicketActivity.this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
