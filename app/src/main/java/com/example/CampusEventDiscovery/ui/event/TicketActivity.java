package com.example.CampusEventDiscovery.ui.event;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.QRCodeHelper;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity for displaying the generated QR code ticket to the attendee.
 *
 * Refund policy: tickets can be cancelled at least 3 days before the event.
 * Paid tickets are refunded as in-app credit.
 */
public class TicketActivity extends AppCompatActivity {

    private String rsvpId;
    private String eventName;
    private String eventDate;
    private String transactionId;
    private String qrPayload;
    private String currentUserId;

    private ImageView ivQrCode;
    private MaterialToolbar toolbarTicket;
    private TextView tvEventName;
    private TextView tvEventDate;
    private TextView tvTxnId;
    private TextView tvRefundStatus;
    private ProgressBar progressBarTicket;
    private MaterialButton btnCancelRefund;
    private MaterialButton btnAddToCalendar;
    private MaterialButton btnViewOnMap;
    private MaterialButton btnDone;

    private EventRepository repository;
    private Rsvp currentRsvp;
    private Event currentEvent;
    private boolean refundEligible;
    private boolean cancellationAllowedByEventDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        repository = new EventRepository();

        Intent intent = getIntent();
        rsvpId        = intent.getStringExtra("rsvpId");
        eventName     = intent.getStringExtra("eventName");
        eventDate     = intent.getStringExtra("eventDate");
        transactionId = intent.getStringExtra("transactionId");
        qrPayload     = intent.getStringExtra("qrPayload");

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null
                ? firebaseUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupUI();
        loadRefundState();
        WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "ticket");
    }

    private void bindViews() {
        toolbarTicket  = findViewById(R.id.toolbarTicket);
        ivQrCode       = findViewById(R.id.ivTicketQrCode);
        tvEventName    = findViewById(R.id.tvTicketEventName);
        tvEventDate    = findViewById(R.id.tvTicketEventDate);
        tvTxnId        = findViewById(R.id.tvTicketTxnId);
        tvRefundStatus = findViewById(R.id.tvTicketRefundStatus);
        progressBarTicket = findViewById(R.id.progressBarTicket);
        btnCancelRefund = findViewById(R.id.btnTicketCancelRefund);
        btnAddToCalendar = findViewById(R.id.btnTicketAddToCalendar);
        btnViewOnMap = findViewById(R.id.btnTicketViewOnMap);
        btnDone        = findViewById(R.id.btnTicketDone);
    }

    private void setupUI() {
        tvEventName.setText(eventName);
        tvEventDate.setText(eventDate);
        tvTxnId.setText("Txn ID: " + transactionId);
        tvRefundStatus.setText(getString(R.string.refund_loading_status));
        renderQrCode();

        toolbarTicket.setNavigationOnClickListener(v -> finish());

        btnCancelRefund.setOnClickListener(v -> showCancelDialog());
        btnAddToCalendar.setOnClickListener(v -> addToCalendar());
        btnViewOnMap.setOnClickListener(v -> openMap());
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadRefundState() {
        setLoading(true);
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(rsvpId)) {
            tvRefundStatus.setText(getString(R.string.refund_not_signed_in_status));
            setLoading(false);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("rsvps")
                .document(rsvpId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentRsvp = documentSnapshot.toObject(Rsvp.class);
                    if (currentRsvp != null && TextUtils.isEmpty(qrPayload)) {
                        qrPayload = currentRsvp.getQrPayload();
                        if (TextUtils.isEmpty(qrPayload)) {
                            qrPayload = buildFallbackQrPayload(currentRsvp);
                        }
                        renderQrCode();
                    }

                    Timestamp eventTimestamp = documentSnapshot.getTimestamp("date");
                    cancellationAllowedByEventDate = EventRepository.isAttendeeCancellationAllowed(
                            eventTimestamp,
                            Timestamp.now());

                    updateRefundUi();
                    loadTicketEventDetails();
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    tvRefundStatus.setText(getString(R.string.refund_unavailable_status));
                    refundEligible = false;
                    updateActionState(false);
                    setLoading(false);
                });
    }

    private void loadTicketEventDetails() {
        String eventId = currentRsvp != null && !TextUtils.isEmpty(currentRsvp.getEventId())
                ? currentRsvp.getEventId()
                : rsvpId;
        if (TextUtils.isEmpty(eventId)) {
            updateTicketActionState(false);
            return;
        }

        repository.getEventById(eventId, new EventRepository.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                if (currentEvent != null) {
                    if (TextUtils.isEmpty(eventName)) {
                        eventName = currentEvent.getTitle();
                        tvEventName.setText(eventName);
                    }
                    if (currentEvent.getDate() != null) {
                        tvEventDate.setText(eventDate);
                    }
                }
                updateTicketActionState(false);
            }

            @Override
            public void onError(Exception e) {
                updateTicketActionState(false);
            }
        });
    }

    private void updateRefundUi() {
        if (currentRsvp == null) {
            tvRefundStatus.setText(getString(R.string.refund_unavailable_status));
            refundEligible = false;
            updateActionState(false);
            return;
        }

        boolean checkedIn = currentRsvp.isCheckedIn();
        boolean cancelled = "cancelled".equalsIgnoreCase(currentRsvp.getStatus());
        double  amount    = currentRsvp.getAmount();
        boolean isPaid    = amount > 0;

        refundEligible = !checkedIn && !cancelled && cancellationAllowedByEventDate;

        if (checkedIn) {
            tvRefundStatus.setText(getString(R.string.refund_checked_in_status));
        } else if (cancelled) {
            tvRefundStatus.setText(getString(R.string.rsvp_cancelled));
        } else if (!cancellationAllowedByEventDate) {
            tvRefundStatus.setText(getString(R.string.refund_window_expired_status));
        } else if (!isPaid) {
            tvRefundStatus.setText(getString(R.string.free_ticket_cancel_status));
        } else {
            tvRefundStatus.setText(getString(R.string.refund_available_status));
        }

        updateActionState(false);
    }

    private void showCancelDialog() {
        if (!refundEligible || currentRsvp == null) {
            return;
        }

        double amount  = currentRsvp.getAmount();
        boolean isPaid = amount > 0;

        // Show different dialog messages for paid vs free tickets
        int messageRes = isPaid
                ? R.string.refund_cancel_confirm_message
                : R.string.free_ticket_cancel_confirm_message;

        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_rsvp)
                .setMessage(messageRes)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    setLoading(true);
                    repository.cancelRsvp(currentUserId, rsvpId, new EventRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            String msg = isPaid
                                    ? getString(R.string.refund_cancel_success)
                                    : getString(R.string.free_ticket_cancel_success);
                            Toast.makeText(TicketActivity.this, msg, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(TicketActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            setLoading(false);
                            Toast.makeText(TicketActivity.this,
                                    getString(R.string.refund_cancel_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        progressBarTicket.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        btnDone.setEnabled(!isLoading);
        updateActionState(isLoading);
        updateTicketActionState(isLoading);
    }

    private void updateActionState(boolean isLoading) {
        boolean cancelEnabled = !isLoading && refundEligible;
        btnCancelRefund.setEnabled(cancelEnabled);
        btnCancelRefund.setAlpha(cancelEnabled ? 1f : 0.6f);
    }

    private void updateTicketActionState(boolean isLoading) {
        boolean eventLoaded = currentEvent != null;
        btnAddToCalendar.setEnabled(!isLoading);
        btnAddToCalendar.setAlpha(!isLoading ? 1f : 0.6f);
        btnViewOnMap.setEnabled(!isLoading && eventLoaded);
        btnViewOnMap.setAlpha(!isLoading && eventLoaded ? 1f : 0.6f);
    }

    private void addToCalendar() {
        if (currentEvent == null && currentRsvp == null) {
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, resolveCalendarTitle())
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, resolveDisplayLocation());

            Timestamp start = currentEvent != null && currentEvent.getDate() != null
                    ? currentEvent.getDate()
                    : currentRsvp != null ? currentRsvp.getDate() : null;
            if (start != null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toDate().getTime());
            }

            long endMillis = resolveEventEndMillis();
            if (endMillis > 0L) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            }

            startActivity(intent);
            if (!TextUtils.isEmpty(currentUserId) && !TextUtils.isEmpty(rsvpId)) {
                repository.markRsvpAddedToCalendar(currentUserId, rsvpId, "");
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.calendar_add_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void openMap() {
        if (currentEvent == null) {
            return;
        }

        String locationKey = currentEvent.getLocationKey();
        if (TextUtils.isEmpty(locationKey)) {
            locationKey = matchLocationKey(currentEvent.getLocation());
        }

        if (!TextUtils.isEmpty(locationKey)) {
            Intent intent = new Intent(this, CampusMapActivity.class);
            intent.putExtra("locationKey", locationKey);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Internal campus map not available for this location.", Toast.LENGTH_SHORT).show();
        }
    }

    private String resolveCalendarTitle() {
        if (currentEvent != null && !TextUtils.isEmpty(currentEvent.getTitle())) {
            return currentEvent.getTitle();
        }
        if (!TextUtils.isEmpty(eventName)) {
            return eventName;
        }
        return getString(R.string.app_name);
    }

    private String resolveDisplayLocation() {
        if (currentEvent == null) {
            return "";
        }
        if (!TextUtils.isEmpty(currentEvent.getLocationDescription())
                && !TextUtils.isEmpty(currentEvent.getLocationKey())) {
            return currentEvent.getLocationDescription() + ", " + currentEvent.getLocationKey();
        }
        if (!TextUtils.isEmpty(currentEvent.getLocationKey())) {
            return currentEvent.getLocationKey();
        }
        return currentEvent.getLocation() == null ? "" : currentEvent.getLocation();
    }

    private String matchLocationKey(String rawLocation) {
        String location = rawLocation != null ? rawLocation.toUpperCase() : "";
        if (location.contains(Constants.MAP_LOC_SSE)) return Constants.MAP_LOC_SSE;
        if (location.contains(Constants.MAP_LOC_HSS)) return Constants.MAP_LOC_HSS;
        if (location.contains(Constants.MAP_LOC_SAHSOL)) return Constants.MAP_LOC_SAHSOL;
        if (location.contains("SPORTS") || location.contains("COMPLEX")) return Constants.MAP_LOC_SPORTS_COMPLEX;
        if (location.contains("PARKING")) return Constants.MAP_LOC_PARKING_LOT;
        if (location.contains("REDC")) return Constants.MAP_LOC_REDC;
        if (location.contains("CRICKET") || location.contains("GROUND")) return Constants.MAP_LOC_CRICKET_GROUND;
        if (location.contains(Constants.MAP_LOC_SDSB)) return Constants.MAP_LOC_SDSB;
        if (location.contains(Constants.MAP_LOC_IST)) return Constants.MAP_LOC_IST;
        if (location.contains("MASJID")) return Constants.MAP_LOC_MASJID;
        return null;
    }

    private long resolveEventEndMillis() {
        if (currentEvent != null && currentEvent.getEndTime() != null) {
            return currentEvent.getEndTime().toDate().getTime();
        }

        Timestamp start = currentEvent != null && currentEvent.getDate() != null
                ? currentEvent.getDate()
                : currentRsvp != null ? currentRsvp.getDate() : null;
        return start != null ? start.toDate().getTime() + 2L * 60L * 60L * 1000L : 0L;
    }

    private void renderQrCode() {
        if (TextUtils.isEmpty(qrPayload)) {
            ivQrCode.setImageDrawable(null);
            return;
        }

        Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 800, 800);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        } else {
            ivQrCode.setImageDrawable(null);
        }
    }

    private String buildFallbackQrPayload(Rsvp rsvp) {
        if (rsvp == null || TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(rsvp.getEventId())
                || TextUtils.isEmpty(rsvp.getTransactionId())) {
            return null;
        }

        JSONObject qrJson = new JSONObject();
        try {
            qrJson.put("userId", currentUserId);
            qrJson.put("eventId", rsvp.getEventId());
            qrJson.put("transactionId", rsvp.getTransactionId());
            qrJson.put("timestamp", rsvp.getRsvpAt() != null ? rsvp.getRsvpAt().toDate().getTime() : System.currentTimeMillis());
            return qrJson.toString();
        } catch (JSONException ignored) {
            return null;
        }
    }
}
