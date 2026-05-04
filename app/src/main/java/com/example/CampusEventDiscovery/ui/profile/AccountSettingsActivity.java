package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.AuthErrorMessages;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.SignupValidator;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ChipGroup chipGroupInterests;
    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private LinearLayout layoutPasswordFields;
    private TextInputLayout tilEmail;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private MaterialButton btnChangeEmail;
    private MaterialButton btnChangePassword;
    private MaterialButton btnSaveSettings;
    private ProgressBar progressBarSettings;
    private TextView tvUserCreditBalance;

    private EventRepository repository;
    private String currentUserId;
    private boolean walkthroughMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        repository = new EventRepository();
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupBackNavigation();
        setupDropdowns();
        setupSecurityButtons();

        if (walkthroughMode) {
            loadWalkthroughProfile();
        } else if (currentUser != null) {
            loadUserData();
        } else if (DevSessionManager.shouldUseBypass(this)) {
            loadDevProfile();
        } else {
            etEmail.setText(getString(R.string.unknown_email));
            etFullName.setText(getString(R.string.guest_user));
            btnSaveSettings.setEnabled(false);
            btnChangeEmail.setEnabled(false);
            btnChangePassword.setEnabled(false);
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }

        setupSaveButton();
        if (walkthroughMode) {
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "account_settings");
        }
    }

    private void bindViews() {
        toolbarSettings = findViewById(R.id.toolbarSettings);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etUniversity = findViewById(R.id.etUniversity);
        etLocation = findViewById(R.id.etLocation);
        chipGroupInterests = findViewById(R.id.chipGroupInterests);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        layoutPasswordFields = findViewById(R.id.layoutPasswordFields);
        tilEmail = findViewById(R.id.tilEmail);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        progressBarSettings = findViewById(R.id.progressBarSettings);
        tvUserCreditBalance = findViewById(R.id.tvUserCreditBalance);
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
    }

    private void setupSecurityButtons() {
        btnChangeEmail.setOnClickListener(v -> {
            etEmail.setEnabled(true);
            etEmail.requestFocus();
            layoutPasswordFields.setVisibility(View.VISIBLE);
            tilNewPassword.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            tilEmail.setHelperText(getString(R.string.change_email_helper));
        });

        btnChangePassword.setOnClickListener(v -> {
            layoutPasswordFields.setVisibility(View.VISIBLE);
            tilNewPassword.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            etCurrentPassword.requestFocus();
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
                precheckInterestChips(user.getInterests());

                if (tvUserCreditBalance != null) {
                    tvUserCreditBalance.setText(getString(R.string.credit_balance_format, user.getCreditBalance()));
                }
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
        precheckInterestChips(new ArrayList<>());
        btnSaveSettings.setEnabled(false);
        btnChangeEmail.setEnabled(false);
        btnChangePassword.setEnabled(false);
    }

    private void loadWalkthroughProfile() {
        etFullName.setText("Demo Attendee");
        etEmail.setText("demo.attendee@campus.edu");
        etUniversity.setText(getDefaultCampus(), false);
        etLocation.setText("Main Campus");
        List<String> interests = new ArrayList<>();
        interests.add("Music");
        interests.add("Sports");
        interests.add("Academic");
        precheckInterestChips(interests);
        if (tvUserCreditBalance != null) {
            tvUserCreditBalance.setText(getString(R.string.credit_balance_format, 1200.0));
        }
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
        if (walkthroughMode) {
            Toast.makeText(this, "Walkthrough mode: account settings were not saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        List<String> interests = getSelectedInterestsFromChips();

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

        if (!SignupValidator.hasMinimumSelectedInterests(interests)) {
            Toast.makeText(this, getString(R.string.select_at_least_three_interests), Toast.LENGTH_SHORT).show();
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
                            getString(R.string.reauthentication_failed, AuthErrorMessages.forAccountUpdate(e)),
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
                            AuthErrorMessages.forAccountUpdate(e)
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
                        AuthErrorMessages.forAccountUpdate(e)
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

    private List<String> getSelectedInterestsFromChips() {
        List<String> interests = new ArrayList<>();
        if (chipGroupInterests == null) {
            return interests;
        }

        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
            if (chipGroupInterests.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) chipGroupInterests.getChildAt(i);
                if (chip.isChecked()) {
                    interests.add(chip.getText().toString());
                }
            }
        }
        return interests;
    }

    private void precheckInterestChips(List<String> interests) {
        if (chipGroupInterests == null) {
            return;
        }

        Set<String> selected = new HashSet<>();
        if (interests != null) {
            for (String interest : interests) {
                selected.add(normalizeInterest(interest));
            }
        }

        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
            if (chipGroupInterests.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) chipGroupInterests.getChildAt(i);
                chip.setChecked(selected.contains(normalizeInterest(chip.getText().toString())));
            }
        }
    }

    private String normalizeInterest(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        String value = raw.trim();
        return value.equalsIgnoreCase("Education") ? "Academic" : value;
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
        btnChangeEmail.setEnabled(!isLoading);
        btnChangePassword.setEnabled(!isLoading);
    }
}
