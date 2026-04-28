package com.example.CampusEventDiscovery.ui.sos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.SosAlert;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lists SOS alerts. Admins see every alert; organizers see only alerts
 * tied to events they created (filtered by sos_alerts.organizerId).
 */
public class SOSDashboardActivity extends AppCompatActivity {

    private static final String TAG = "SOSDashboardActivity";
    private RecyclerView rvAlerts;
    private TextView tvEmpty;
    private ProgressBar pbLoading;
    private SosAlertAdapter adapter;

    private String currentUserId;
    private String currentUserRole = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbarSos);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAlerts = findViewById(R.id.rvSosAlerts);
        tvEmpty = findViewById(R.id.tvSosEmpty);
        pbLoading = findViewById(R.id.pbSosLoading);

        adapter = new SosAlertAdapter();
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null
                ? firebaseUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            pbLoading.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
            adapter.setAlerts(Collections.singletonList(createDemoAlert()));
            rvAlerts.postDelayed(() ->
                    WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "sos_dashboard"), 260L);
        } else {
            resolveRoleAndLoad();
        }
    }

    private SosAlert createDemoAlert() {
        return new SosAlert(
                "walkthrough_attendee",
                "Demo Attendee",
                "walkthrough_event",
                "Demo Music Night",
                currentUserId,
                0.0,
                0.0,
                "",
                System.currentTimeMillis() - 5 * 60 * 1000L,
                getString(R.string.sos_status_active)
        );
    }

    private void resolveRoleAndLoad() {
        if (DevSessionManager.shouldUseBypass(this)) {
            currentUserRole = DevSessionManager.getBypassRole(this);
            loadAlerts();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Sign in to view SOS alerts.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    currentUserRole = UserRoles.sanitize(doc != null ? doc.getString("role") : "");
                    loadAlerts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to resolve role", e);
                    Toast.makeText(SOSDashboardActivity.this,
                            "Failed to verify your role.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadAlerts() {
        if (!UserRoles.isAdmin(currentUserRole) && !UserRoles.isOrganizer(currentUserRole)) {
            Toast.makeText(this, "Access restricted to admins and organizers.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Query query = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_SOS_ALERTS);

        if (UserRoles.isOrganizer(currentUserRole)) {
            query = query.whereEqualTo("organizerId", currentUserId);
        }

        // Removed orderBy temporarily to rule out missing index issues. 
        // If results load after this, a Firestore index is required.
        query.get()
                .addOnSuccessListener(snapshots -> {
                    List<SosAlert> alerts = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        SosAlert alert = doc.toObject(SosAlert.class);
                        if (alert != null) alerts.add(alert);
                    }
                    // Sort manually to avoid index requirement
                    alerts.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    renderAlerts(alerts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load alerts", e);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(SOSDashboardActivity.this,
                            "Failed to load alerts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void renderAlerts(List<SosAlert> alerts) {
        pbLoading.setVisibility(View.GONE);
        adapter.setAlerts(alerts);
        if (alerts.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        }
    }
}
