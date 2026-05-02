package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.SignupValidator;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * SignUpActivity.java
 *
 * Screen for new users to register an account.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etRepeatPassword;
    private AutoCompleteTextView actvCampus;
    private ChipGroup chipGroupInterests;
    private MaterialButtonToggleGroup toggleUserType;
    private CheckBox cbTerms;
    private MaterialButton btnSignUp;
    private MaterialButton btnDevBypass;
    private ProgressBar progressBarSignUp;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbarSignUp = findViewById(R.id.toolbarSignUp);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        actvCampus = findViewById(R.id.actvCampus);
        chipGroupInterests = findViewById(R.id.chipGroupInterests);
        toggleUserType = findViewById(R.id.toggleUserType);
        cbTerms = findViewById(R.id.cbTerms);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBarSignUp = findViewById(R.id.progressBarSignUp);
        TextView tvTermsLink = findViewById(R.id.tvTermsLink);
        TextView tvPrivacyLink = findViewById(R.id.tvPrivacyLink);
        btnDevBypass = findViewById(R.id.btnDevBypass);

        setupDropdowns();

        toolbarSignUp.setNavigationOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> signUpUser());
        tvTermsLink.setOnClickListener(v -> showPolicyDialog(
                getString(R.string.terms_and_conditions),
                getString(R.string.terms_body)
        ));
        tvPrivacyLink.setOnClickListener(v -> showPolicyDialog(
                getString(R.string.privacy_policy),
                getString(R.string.privacy_body)
        ));
        btnDevBypass.setOnClickListener(v -> DevBypassHelper.showRolePicker(this));
    }

    private void signUpUser() {
        DevSessionManager.clearBypass(this);

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();
        String campus = actvCampus.getText().toString().trim();

        String role = toggleUserType.getCheckedButtonId() == R.id.btnOrganizer ? "organizer" : "attendee";

        String validationError = SignupValidator.validateName(fullName);
        if (validationError == null) {
            validationError = SignupValidator.validateEmail(email);
        }
        if (validationError == null) {
            validationError = SignupValidator.validatePassword(password);
        }
        if (validationError == null) {
            validationError = SignupValidator.validatePasswordConfirmation(password, repeatPassword);
        }
        if (validationError == null) {
            validationError = SignupValidator.validateRole(role);
        }
        if (validationError == null) {
            validationError = SignupValidator.validateCampus(campus);
        }
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, getString(R.string.terms_required), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean darkMode = ThemeManager.isDarkModeEnabled(this);
        ArrayList<String> interests = getSelectedInterestsFromChips();
        if (!SignupValidator.hasMinimumSelectedInterests(interests)) {
            Toast.makeText(this, getString(R.string.select_at_least_three_interests), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(SignUpActivity.this, "Auth error: User is null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    User newUser = new User(
                            fullName,
                            email,
                            role,
                            campus,
                            "", // location
                            "", // profilePicUrl
                            interests,
                            darkMode
                    );

                    db.collection("users").document(uid).set(newUser)
                            .addOnSuccessListener(unused -> {
                                auth.setLanguageCode("en");

                                firebaseUser.sendEmailVerification()
                                        .addOnSuccessListener(emailUnused -> {
                                            auth.signOut();
                                            redirectToSignIn(
                                                    "Account created. Verification email sent. Please verify your email before signing in.",
                                                    darkMode
                                            );
                                        })
                                        .addOnFailureListener(e -> {
                                            auth.signOut();
                                            redirectToSignIn(
                                                    "Account created, but the verification email could not be sent right now. Please sign in again to resend verification.",
                                                    darkMode
                                            );
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(
                                        SignUpActivity.this,
                                        "Database error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(SignUpActivity.this, buildAuthErrorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    private void redirectToSignIn(String message, boolean darkMode) {
        ThemeManager.applyThemePreference(this, darkMode);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupDropdowns() {
        String[] campusOptions = getResources().getStringArray(R.array.campus_options);
        ArrayAdapter<String> campusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                campusOptions
        );
        actvCampus.setAdapter(campusAdapter);
        actvCampus.setOnClickListener(v -> actvCampus.showDropDown());
        actvCampus.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvCampus.showDropDown();
            }
        });
        if (campusOptions.length > 0) {
            actvCampus.setText(campusOptions[0], false);
        }
    }

    private void showPolicyDialog(String title, String body) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private ArrayList<String> getSelectedInterestsFromChips() {
        ArrayList<String> interests = new ArrayList<>();
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

    private String buildAuthErrorMessage(Exception exception) {
        if (exception == null) {
            return "Auth error: sign up failed.";
        }

        String message = exception.getMessage();
        if (message != null && message.contains("CONFIGURATION_NOT_FOUND")) {
            return "Auth error: Firebase Authentication is not fully configured. Enable Email/Password and finish Auth/App Check setup in Firebase Console.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            return "Auth error: " + authException.getErrorCode();
        }

        return "Auth error: " + message;
    }

    private void setLoading(boolean isLoading) {
        btnSignUp.setEnabled(!isLoading);
        btnDevBypass.setEnabled(!isLoading);
        cbTerms.setEnabled(!isLoading);
        toggleUserType.setEnabled(!isLoading);
        actvCampus.setEnabled(!isLoading);
        progressBarSignUp.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }
}
