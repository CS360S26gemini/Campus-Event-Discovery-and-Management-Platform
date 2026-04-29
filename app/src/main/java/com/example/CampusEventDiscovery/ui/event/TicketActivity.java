package com.example.CampusEventDiscovery.ui.event;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.QRCodeHelper;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private MaterialButton btnCancelRefund;
    private MaterialButton btnDone;

    private EventRepository repository;
    private Rsvp currentRsvp;
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
        btnCancelRefund = findViewById(R.id.btnTicketCancelRefund);
        btnDone        = findViewById(R.id.btnTicketDone);
    }

    private void setupUI() {
        tvEventName.setText(eventName);
        tvEventDate.setText(eventDate);
        tvTxnId.setText("Txn ID: " + transactionId);
        tvRefundStatus.setText(getString(R.string.refund_loading_status));

        if (qrPayload != null) {
            Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 800, 800);
            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap);
            }
        }

        toolbarTicket.setNavigationOnClickListener(v -> finish());

        btnCancelRefund.setOnClickListener(v -> showCancelDialog());
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadRefundState() {
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(rsvpId)) {
            tvRefundStatus.setText(getString(R.string.refund_not_signed_in_status));
            btnCancelRefund.setEnabled(false);
            btnCancelRefund.setAlpha(0.6f);
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

                    Timestamp eventTimestamp = documentSnapshot.getTimestamp("date");
                    cancellationAllowedByEventDate = EventRepository.isAttendeeCancellationAllowed(
                            eventTimestamp,
                            Timestamp.now());

                    updateRefundUi();
                })
                .addOnFailureListener(e -> {
                    btnCancelRefund.setEnabled(false);
                    btnCancelRefund.setAlpha(0.6f);
                    tvRefundStatus.setText(getString(R.string.refund_unavailable_status));
                });
    }

    private void updateRefundUi() {
        if (currentRsvp == null) {
            btnCancelRefund.setEnabled(false);
            btnCancelRefund.setAlpha(0.6f);
            tvRefundStatus.setText(getString(R.string.refund_unavailable_status));
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

        btnCancelRefund.setEnabled(refundEligible);
        btnCancelRefund.setAlpha(refundEligible ? 1f : 0.6f);
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
                            Toast.makeText(TicketActivity.this,
                                    getString(R.string.refund_cancel_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
