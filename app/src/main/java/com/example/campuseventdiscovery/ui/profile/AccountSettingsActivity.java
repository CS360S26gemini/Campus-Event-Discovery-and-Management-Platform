package com.example.campuseventdiscovery.ui.profile;

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

import com.example.campuseventdiscovery.MainActivity;
import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
            tvEmail.setText(DevSessionManager.getDisplayEmail(this));
            etFullName.setText(DevSessionManager.getDisplayName(this));
            btnSaveSettings.setEnabled(false);
            Toast.makeText(this, "Test mode active - Save disabled", Toast.LENGTH_SHORT).show();
        } else {
            tvEmail.setText("dev.user@example.com");
            etFullName.setText("Dev User");
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
                }
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(AccountSettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
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

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        repository.updateUserProfile(currentUserId, fullName, university, location, new EventRepository.ActionCallback() {
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

    private void showLoading(boolean isLoading) {
        progressBarSettings.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveSettings.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
