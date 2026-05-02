package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.NotificationAdapter;
import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays in-app notifications stored in Firestore.
 */
public class NotificationCenterActivity extends AppCompatActivity {

    private MaterialToolbar toolbarNotifications;
    private RecyclerView rvNotifications;
    private ProgressBar progressBarNotifications;
    private TextView tvEmptyNotifications;

    private EventRepository repository;
    private NotificationAdapter adapter;
    private String currentUserId;
    private final List<Notification> notifications = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        loadNotifications();
        WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "notifications");
    }

    private void bindViews() {
        toolbarNotifications = findViewById(R.id.toolbarNotifications);
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBarNotifications = findViewById(R.id.progressBarNotifications);
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications);
    }

    private void setupToolbar() {
        toolbarNotifications.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notifications, notification -> {
            if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
                Toast.makeText(this, "Walkthrough mode: notification was not opened.", Toast.LENGTH_SHORT).show();
                return;
            }
            repository.markNotificationRead(currentUserId, notification.getNotificationId());
            notification.setRead(true);
            adapter.updateData(new ArrayList<>(notifications));

            if (!TextUtils.isEmpty(notification.getEventId())) {
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra("eventId", notification.getEventId());
                startActivity(intent);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            bindNotifications(WalkthroughManager.getDemoNotifications());
            return;
        }

        setLoading(true);

        if (TextUtils.isEmpty(currentUserId)) {
            bindNotifications(new ArrayList<>());
            return;
        }

        repository.getNotifications(currentUserId, new EventRepository.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                bindNotifications(notifications);
            }

            @Override
            public void onError(Exception e) {
                bindNotifications(new ArrayList<>());
            }
        });
    }

    private void bindNotifications(List<Notification> items) {
        notifications.clear();
        if (items != null) {
            notifications.addAll(items);
        }
        adapter.updateData(new ArrayList<>(notifications));
        tvEmptyNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        rvNotifications.setVisibility(notifications.isEmpty() ? View.GONE : View.VISIBLE);
        setLoading(false);
    }

    private void setLoading(boolean isLoading) {
        progressBarNotifications.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
