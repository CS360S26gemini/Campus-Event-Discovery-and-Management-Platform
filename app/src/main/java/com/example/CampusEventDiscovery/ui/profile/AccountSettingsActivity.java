package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.SignupValidator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.EmailAuthProvider;
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
    private EditText etEmail;
    private AutoCompleteTextView etUniversity;
    private EditText etLocation;
    private MultiAutoCompleteTextView etInterests;
    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
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
        setupBackNavigation();
        setupDropdowns();

        if (currentUser != null) {
            loadUserData();
        } else if (DevSessionManager.shouldUseBypass(this)) {
            loadDevProfile();
        } else {
            etEmail.setText(getString(R.string.unknown_email));
            etFullName.setText(getString(R.string.guest_user));
            btnSaveSettings.setEnabled(false);
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }

        setupSaveButton();
    }

    private void bindViews() {
        toolbarSettings = findViewById(R.id.toolbarSettings);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etUniversity = findViewById(R.id.etUniversity);
        etLocation = findViewById(R.id.etLocation);
        etInterests = findViewById(R.id.etInterests);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        progressBarSettings = findViewById(R.id.progressBarSettings);
    }

    private void setupToolbar() {
        toolbarSettings.setNavigationOnClickListener(v -> navigateBackToProfile());
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToProfile();
            }
        });
    }

    private void setupDropdowns() {
        String[] campusOptions = getResources().getStringArray(R.array.campus_options);
        ArrayAdapter<String> campusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                campusOptions
        );
        etUniversity.setAdapter(campusAdapter);
        etUniversity.setOnClickListener(v -> etUniversity.showDropDown());
        etUniversity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etUniversity.showDropDown();
            }
        });
        if (campusOptions.length > 0) {
            etUniversity.setText(campusOptions[0], false);
        }

        String[] interestOptions = getResources().getStringArray(R.array.interest_options);
        ArrayAdapter<String> interestsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                interestOptions
        );
        etInterests.setAdapter(interestsAdapter);
        etInterests.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        etInterests.setThreshold(1);
        etInterests.setOnClickListener(v -> etInterests.showDropDown());
        etInterests.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etInterests.showDropDown();
            }
        });
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
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showLoading(false);
                if (user == null) {
                    return;
                }

                etFullName.setText(user.getFullName());
                etEmail.setText(user.getEmail());
                etUniversity.setText(
                        TextUtils.isEmpty(user.getUniversity()) ? getDefaultCampus() : user.getUniversity(),
                        false
                );
                etLocation.setText(user.getLocation());
                etInterests.setText(joinInterests(user.getInterests()));
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showLoading(false);
                Toast.makeText(AccountSettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDevProfile() {
        etFullName.setText(DevSessionManager.getDisplayName(this));
        etEmail.setText(DevSessionManager.getDisplayEmail(this));
        etUniversity.setText(getDefaultCampus(), false);
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
        String email = etEmail.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        List<String> interests = parseInterests(etInterests.getText().toString());

        String validationError = SignupValidator.validateName(fullName);
        if (validationError == null) {
            validationError = SignupValidator.validateEmail(email);
        }
        if (validationError == null) {
            validationError = SignupValidator.validateCampus(university);
        }

        boolean passwordChangeRequested = !TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword);
        if (validationError == null && passwordChangeRequested) {
            validationError = SignupValidator.validatePassword(newPassword);
        }
        if (validationError == null && passwordChangeRequested) {
            validationError = SignupValidator.validatePasswordConfirmation(newPassword, confirmPassword);
        }

        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean emailChanged = currentUser != null && !TextUtils.equals(email, currentUser.getEmail());
        boolean requiresAuthUpdate = currentUser != null && (emailChanged || passwordChangeRequested);

        if (requiresAuthUpdate && TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this,
                    getString(R.string.current_password_required_for_auth_updates),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (!requiresAuthUpdate) {
            persistProfile(fullName, email, university, location, interests, null);
            return;
        }

        currentUser.reauthenticate(EmailAuthProvider.getCredential(
                        currentUser.getEmail() == null ? email : currentUser.getEmail(),
                        currentPassword
                ))
                .addOnSuccessListener(unused -> applyAuthUpdates(
                        currentUser,
                        email,
                        newPassword,
                        emailChanged,
                        passwordChangeRequested,
                        fullName,
                        university,
                        location,
                        interests
                ))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(AccountSettingsActivity.this,
                            getString(R.string.reauthentication_failed, safeMessage(e.getMessage())),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void applyAuthUpdates(FirebaseUser currentUser,
                                  String email,
                                  String newPassword,
                                  boolean emailChanged,
                                  boolean passwordChangeRequested,
                                  String fullName,
                                  String university,
                                  String location,
                                  List<String> interests) {
        if (emailChanged) {
            currentUser.updateEmail(email)
                    .addOnSuccessListener(unused -> updatePasswordIfNeeded(
                            currentUser,
                            newPassword,
                            passwordChangeRequested,
                            fullName,
                            email,
                            university,
                            location,
                            interests
                    ))
                    .addOnFailureListener(e -> persistProfileAfterAuthFailure(
                            fullName,
                            currentUser.getEmail(),
                            university,
                            location,
                            interests,
                            e.getMessage()
                    ));
            return;
        }

        updatePasswordIfNeeded(
                currentUser,
                newPassword,
                passwordChangeRequested,
                fullName,
                email,
                university,
                location,
                interests
        );
    }

    private void updatePasswordIfNeeded(FirebaseUser currentUser,
                                        String newPassword,
                                        boolean passwordChangeRequested,
                                        String fullName,
                                        String email,
                                        String university,
                                        String location,
                                        List<String> interests) {
        if (!passwordChangeRequested) {
            persistProfile(fullName, email, university, location, interests, null);
            return;
        }

        currentUser.updatePassword(newPassword)
                .addOnSuccessListener(unused -> persistProfile(
                        fullName,
                        email,
                        university,
                        location,
                        interests,
                        null
                ))
                .addOnFailureListener(e -> persistProfileAfterAuthFailure(
                        fullName,
                        currentUser.getEmail(),
                        university,
                        location,
                        interests,
                        e.getMessage()
                ));
    }

    private void persistProfileAfterAuthFailure(String fullName,
                                                String email,
                                                String university,
                                                String location,
                                                List<String> interests,
                                                String authErrorMessage) {
        String safeEmail = TextUtils.isEmpty(email) ? etEmail.getText().toString().trim() : email;
        persistProfile(
                fullName,
                safeEmail,
                university,
                location,
                interests,
                getString(R.string.profile_updated_but_auth_failed, safeMessage(authErrorMessage))
        );
    }

    private void persistProfile(String fullName,
                                String email,
                                String university,
                                String location,
                                List<String> interests,
                                String successMessageOverride) {
        repository.updateUserProfile(
                currentUserId,
                fullName,
                email,
                university,
                location,
                interests,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(
                                AccountSettingsActivity.this,
                                successMessageOverride != null
                                        ? successMessageOverride
                                        : getString(R.string.account_updated),
                                Toast.LENGTH_LONG
                        ).show();
                        navigateBackToProfile();
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        Toast.makeText(
                                AccountSettingsActivity.this,
                                getString(R.string.account_update_failed, safeMessage(e != null ? e.getMessage() : null)),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
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

    private String getDefaultCampus() {
        String[] options = getResources().getStringArray(R.array.campus_options);
        return options.length > 0 ? options[0] : "";
    }

    private String safeMessage(String message) {
        return TextUtils.isEmpty(message) ? getString(R.string.unknown_error) : message;
    }

    private void showLoading(boolean isLoading) {
        progressBarSettings.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveSettings.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
