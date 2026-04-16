package com.example.CampusEventDiscovery.ui.sos;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.SosAlert;
import com.example.CampusEventDiscovery.repository.SosRepository;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * SosActivity captures GPS coordinates and writes an SOS alert to Firestore
 * so campus security can be notified of an attendee emergency at an event.
 */
public class SosActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final long LOCATION_TIMEOUT_MS = 10_000L;

    private String eventId;
    private String eventName;
    private String organizerId;
    private String currentUserId;

    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private ProgressDialog progressDialog;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean locationResolved = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        organizerId = getIntent().getStringExtra("organizerId");
        if (organizerId == null) {
            organizerId = "";
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null
                ? firebaseUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Hide the success UI until the alert is actually sent.
        findViewById(R.id.tvSosHeader).setVisibility(View.GONE);
        findViewById(R.id.tvSosMessage).setVisibility(View.GONE);
        findViewById(R.id.tvMapsUrl).setVisibility(View.GONE);
        findViewById(R.id.btnSosClose).setVisibility(View.GONE);

        showConfirmationDialog();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Send SOS Alert")
                .setMessage("This will share your current location with the event organizer and campus administrators. Send alert?")
                .setCancelable(false)
                .setPositiveButton("Send Alert", (dialog, which) -> sendSosAlert())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void sendSosAlert() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Getting your location...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
        cancellationTokenSource = new CancellationTokenSource();
        locationResolved = false;

        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = () -> {
            if (locationResolved) return;
            locationResolved = true;
            if (cancellationTokenSource != null) {
                cancellationTokenSource.cancel();
            }
            fetchUserDisplayName(0.0, 0.0);
        };
        timeoutHandler.postDelayed(timeoutRunnable, LOCATION_TIMEOUT_MS);

        try {
            fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (locationResolved) return;
                        locationResolved = true;
                        cancelTimeout();
                        if (location != null) {
                            fetchUserDisplayName(location.getLatitude(), location.getLongitude());
                        } else {
                            fetchUserDisplayName(0.0, 0.0);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (locationResolved) return;
                        locationResolved = true;
                        cancelTimeout();
                        fetchUserDisplayName(0.0, 0.0);
                    });
        } catch (SecurityException e) {
            if (locationResolved) return;
            locationResolved = true;
            cancelTimeout();
            fetchUserDisplayName(0.0, 0.0);
        }
    }

    private void cancelTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private void fetchUserDisplayName(double latitude, double longitude) {
        if (TextUtils.isEmpty(currentUserId)) {
            fetchAdminsAndSend(latitude, longitude, "Unknown User");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String displayName = "Unknown User";
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String fetched = documentSnapshot.getString("fullName");
                        if (!TextUtils.isEmpty(fetched)) {
                            displayName = fetched;
                        }
                    }
                    fetchAdminsAndSend(latitude, longitude, displayName);
                })
                .addOnFailureListener(e -> fetchAdminsAndSend(latitude, longitude, "Unknown User"));
    }

    private void fetchAdminsAndSend(double lat, double lng, String displayName) {
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .whereEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> adminIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        adminIds.add(doc.getId());
                    }
                    writeAlert(lat, lng, displayName, adminIds);
                })
                .addOnFailureListener(e -> writeAlert(lat, lng, displayName, new ArrayList<>()));
    }

    private void writeAlert(double lat, double lng, String displayName, List<String> adminIds) {
        String mapsUrl = "https://maps.google.com/?q=" + lat + "," + lng;

        SosAlert alert = new SosAlert(
                currentUserId == null ? "" : currentUserId,
                displayName,
                eventId == null ? "" : eventId,
                eventName == null ? "" : eventName,
                organizerId == null ? "" : organizerId,
                lat,
                lng,
                mapsUrl,
                System.currentTimeMillis(),
                "ACTIVE"
        );

        new SosRepository().sendSosAlert(alert, organizerId, adminIds, new SosRepository.SosCallback() {
            @Override
            public void onSuccess() {
                getSharedPreferences("sos_prefs", MODE_PRIVATE)
                        .edit()
                        .putLong("last_sent_ts", System.currentTimeMillis())
                        .apply();
                dismissProgress();
                showSuccessUi(mapsUrl);
            }

            @Override
            public void onFailure(Exception e) {
                dismissProgress();
                Toast.makeText(SosActivity.this,
                        "Failed to send SOS. Please call security directly.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showSuccessUi(String mapsUrl) {
        findViewById(R.id.tvSosHeader).setVisibility(View.VISIBLE);
        TextView tvMessage = findViewById(R.id.tvSosMessage);
        tvMessage.setText("Help is on the way. The event organizer and administrators have been notified.");
        tvMessage.setVisibility(View.VISIBLE);

        TextView tvMapsUrl = findViewById(R.id.tvMapsUrl);
        tvMapsUrl.setText(mapsUrl);
        tvMapsUrl.setVisibility(View.VISIBLE);
        tvMapsUrl.setOnClickListener(v -> {
            try {
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                startActivity(browse);
            } catch (Exception ignored) {
            }
        });

        MaterialButton btnClose = findViewById(R.id.btnSosClose);
        btnClose.setVisibility(View.VISIBLE);
        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSION) return;

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCurrentLocation();
        } else {
            dismissProgress();
            Toast.makeText(this,
                    "Location permission required to send SOS alert.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        cancelTimeout();
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
        dismissProgress();
        super.onDestroy();
    }
}
