package com.example.CampusEventDiscovery.ui.organizer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * ScannerActivity.java
 *
 * Allows organizers to scan attendee QR codes and mark them as attended.
 * Once an attendee is marked as attended, the QR code is expired
 * (qrExpired = true) making it one-time use only.
 */
public class ScannerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private MaterialCardView cardResult;
    private TextView tvAttendeeName;
    private TextView tvEventName;
    private TextView tvPaymentStatus;
    private TextView tvCheckInStatus;
    private TextView tvError;
    private MaterialButton btnMarkAttended;
    private MaterialButton btnStartScanner;

    private FirebaseFirestore db;
    private Rsvp currentRsvp;
    private String currentAttendeeName;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startScanning();
                } else {
                    Toast.makeText(this,
                            getString(R.string.scanner_camera_permission_required),
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupUI();
    }

    private void bindViews() {
        toolbar          = findViewById(R.id.toolbarScanner);
        cardResult       = findViewById(R.id.cardScannerResult);
        tvAttendeeName   = findViewById(R.id.tvScannerAttendeeName);
        tvEventName      = findViewById(R.id.tvScannerEventName);
        tvPaymentStatus  = findViewById(R.id.tvScannerPaymentStatus);
        tvCheckInStatus  = findViewById(R.id.tvScannerCheckInStatus);
        tvError          = findViewById(R.id.tvScannerError);
        btnMarkAttended  = findViewById(R.id.btnMarkAsAttended);
        btnStartScanner  = findViewById(R.id.btnStartScanner);
    }

    private void setupUI() {
        toolbar.setNavigationOnClickListener(v -> finish());
        btnStartScanner.setOnClickListener(v -> checkCameraPermission());
        btnMarkAttended.setOnClickListener(v -> markAsAttended());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanning() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.scanner_prompt));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, getString(R.string.scanner_cancelled), Toast.LENGTH_LONG).show();
            } else {
                processScanResult(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Parses the scanned QR payload JSON and looks up the matching RSVP.
     *
     * @param payload The raw string content scanned from the QR code.
     */
    private void processScanResult(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String transactionId = json.getString("transactionId");
            String userId        = json.getString("userId");
            String eventId       = json.getString("eventId");

            lookupRsvp(transactionId, userId, eventId);
        } catch (JSONException e) {
            showError(getString(R.string.scanner_invalid_qr));
        }
    }

    /**
     * Fetches the RSVP document from Firestore and verifies the transaction ID.
     *
     * @param txnId   Transaction ID from the scanned QR payload.
     * @param userId  User ID from the scanned QR payload.
     * @param eventId Event ID from the scanned QR payload.
     */
    private void lookupRsvp(String txnId, String userId, String eventId) {
        db.collection("users").document(userId)
                .collection("rsvps").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentRsvp = documentSnapshot.toObject(Rsvp.class);
                        if (currentRsvp != null) {
                            // Fix: Explicitly set userId and eventId since they may not be fields in the doc
                            currentRsvp.setUserId(userId);
                            currentRsvp.setEventId(eventId);

                            if (txnId.equals(currentRsvp.getTransactionId())) {
                                fetchAttendeeNameAndDisplay(userId);
                            } else {
                                showError(getString(R.string.scanner_rsvp_mismatch));
                            }
                        }
                    } else {
                        showError(getString(R.string.scanner_rsvp_not_found));
                    }
                })
                .addOnFailureListener(e -> showError(getString(R.string.scanner_error_fetching, e.getMessage())));
    }

    /**
     * Fetches the attendee's display name from Firestore before showing results.
     *
     * @param userId The user ID whose name to look up.
     */
    private void fetchAttendeeNameAndDisplay(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    currentAttendeeName = (user != null && user.getFullName() != null)
                            ? user.getFullName()
                            : getString(R.string.scanner_unknown_user);
                    displayResult();
                })
                .addOnFailureListener(e -> {
                    currentAttendeeName = getString(R.string.scanner_unknown_user);
                    displayResult();
                });
    }

    /**
     * Displays the scan result card. If the QR code has already been used
     * (qrExpired = true), shows a "TICKET ALREADY USED" warning in red
     * and hides the mark-as-attended button.
     */
    private void displayResult() {
        tvError.setVisibility(View.GONE);
        cardResult.setVisibility(View.VISIBLE);

        tvAttendeeName.setText(getString(R.string.scanner_label_attendee, currentAttendeeName));
        tvEventName.setText(getString(R.string.scanner_label_event, currentRsvp.getTitle()));
        tvPaymentStatus.setText(getString(R.string.scanner_label_payment, currentRsvp.getPaymentStatus()));

        if (currentRsvp.isQrExpired()) {
            // QR has already been used — block re-entry
            tvCheckInStatus.setText(getString(R.string.scanner_ticket_already_used));
            tvCheckInStatus.setTextColor(Color.RED);
            btnMarkAttended.setVisibility(View.GONE);
        } else if (currentRsvp.isCheckedIn()) {
            tvCheckInStatus.setText(getString(R.string.scanner_checked_in_yes));
            btnMarkAttended.setVisibility(View.GONE);
        } else {
            tvCheckInStatus.setText(getString(R.string.scanner_checked_in_no));
            btnMarkAttended.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Marks the attendee as attended by setting checkedIn = true and
     * qrExpired = true in a single Firestore update, making the QR
     * code one-time use only.
     */
    private void markAsAttended() {
        if (currentRsvp == null || currentRsvp.getUserId() == null || currentRsvp.getEventId() == null) return;

        db.collection("users").document(currentRsvp.getUserId())
                .collection("rsvps").document(currentRsvp.getEventId())
                .update(
                        "checkedIn", true,
                        "qrExpired", true,
                        "checkedInAt", Timestamp.now()
                )
                .addOnSuccessListener(aVoid -> {
                    currentRsvp.setCheckedIn(true);
                    currentRsvp.setQrExpired(true);

                    tvCheckInStatus.setText(getString(R.string.scanner_checked_in_yes));
                    btnMarkAttended.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.scanner_marked_attended),
                            Toast.LENGTH_SHORT).show();

                    // Mirror check-in state on the event's attendees sub-collection
                    db.collection("events").document(currentRsvp.getEventId())
                            .collection("attendees").document(currentRsvp.getUserId())
                            .update("checkedIn", true, "checkedInAt", Timestamp.now());
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.scanner_mark_failed),
                        Toast.LENGTH_SHORT).show());
    }

    private void showError(String message) {
        cardResult.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }
}
