package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * AccountSettingsActivity.java
 *
 * Screen for users to update their profile information.
 */
public class AccountSettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbarSettings;
    private EditText etFullName;
    private TextView tvEmail;
    private EditText etUniversity;
    private EditText etLocation;
    private EditText etInterests;
    private MaterialButton btnSaveSettings;
    private ProgressBar progressBarSettings;

    private EventRepository repository;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        repository = new EventRepository();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        
        if (currentUser != null) {
            loadUserData();
        } else if (DevSessionManager.shouldUseBypass(this)) {
            loadDevProfile();
        } else {
            tvEmail.setText(getString(R.string.unknown_email));
            etFullName.setText(getString(R.string.guest_user));
            btnSaveSettings.setEnabled(false);
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }
        
        setupSaveButton();
    }

    private void bindViews() {
        toolbarSettings = findViewById(R.id.toolbarSettings);
        etFullName = findViewById(R.id.etFullName);
        tvEmail = findViewById(R.id.tvEmail);
        etUniversity = findViewById(R.id.etUniversity);
        etLocation = findViewById(R.id.etLocation);
        etInterests = findViewById(R.id.etInterests);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        progressBarSettings = findViewById(R.id.progressBarSettings);
    }

    private void setupToolbar() {
        toolbarSettings.setNavigationOnClickListener(v -> navigateBackToProfile());
    }

    @Override
    public void onBackPressed() {
        navigateBackToProfile();
    }

    private void navigateBackToProfile() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
        showLoading(true);
        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                showLoading(false);
                if (user != null) {
                    etFullName.setText(user.getFullName());
                    tvEmail.setText(user.getEmail());
                    etUniversity.setText(user.getUniversity());
                    etLocation.setText(user.getLocation());
                    etInterests.setText(joinInterests(user.getInterests()));
                }
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(AccountSettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDevProfile() {
        etFullName.setText(DevSessionManager.getDisplayName(this));
        tvEmail.setText(DevSessionManager.getDisplayEmail(this));
        etUniversity.setText("");
        etLocation.setText("");
        etInterests.setText("");
        btnSaveSettings.setEnabled(false);
    }

    private void setupSaveButton() {
        btnSaveSettings.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            saveSettings();
        });
    }

    private void saveSettings() {
        String fullName = etFullName.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        List<String> interests = parseInterests(etInterests.getText().toString());

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        repository.updateUserProfile(currentUserId, fullName, university, location, interests, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                    showLoading(false);
                    Toast.makeText(AccountSettingsActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    navigateBackToProfile();
            }

            @Override
            public void onError(Exception e) {
                    showLoading(false);
                    Toast.makeText(AccountSettingsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String joinInterests(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return "";
        }
        return TextUtils.join(", ", interests);
    }

    private List<String> parseInterests(String raw) {
        List<String> interests = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return interests;
        }

        String[] tokens = raw.split(",");
        for (String token : tokens) {
            String value = token == null ? "" : token.trim();
            if (!TextUtils.isEmpty(value) && !interests.contains(value)) {
                interests.add(value);
            }
        }
        return interests;
    }

    private void showLoading(boolean isLoading) {
        progressBarSettings.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveSettings.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
