package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.SignupValidator;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * SignUpActivity.java
 *
 * Screen for new users to register an account.
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String POLICY_VERSION = "2026-04-18";

    private EditText etFullName, etEmail, etPassword, etRepeatPassword;
    private AutoCompleteTextView actvCampus;
    private MultiAutoCompleteTextView etInterests;
    private MaterialButtonToggleGroup toggleUserType;
    private MaterialButton btnSignUp;
    private CheckBox cbAcceptPolicies;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        actvCampus = findViewById(R.id.actvCampus);
        etInterests = findViewById(R.id.etInterests);
        toggleUserType = findViewById(R.id.toggleUserType);
        cbAcceptPolicies = findViewById(R.id.cbAcceptPolicies);
        btnSignUp = findViewById(R.id.btnSignUp);
        MaterialButton btnViewTerms = findViewById(R.id.btnViewTerms);
        MaterialButton btnViewPrivacy = findViewById(R.id.btnViewPrivacy);
        MaterialButton btnDevBypass = findViewById(R.id.btnDevBypass);

        setupDropdowns();

        btnBack.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> signUpUser());
        btnViewTerms.setOnClickListener(v -> showPolicyDialog(
                getString(R.string.terms_title),
                getString(R.string.terms_body)
        ));
        btnViewPrivacy.setOnClickListener(v -> showPolicyDialog(
                getString(R.string.privacy_policy_title),
                getString(R.string.privacy_policy_body)
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
        String interestsRaw = etInterests.getText().toString().trim();

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
        if (validationError == null) {
            validationError = SignupValidator.validatePolicyAcceptance(cbAcceptPolicies.isChecked());
        }
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean darkMode = ThemeManager.isDarkModeEnabled(this);
        ArrayList<String> interests = parseInterests(interestsRaw);

        btnSignUp.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        btnSignUp.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Auth error: User is null", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String uid = authResult.getUser().getUid();
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
                    newUser.setAcceptedTerms(true);
                    newUser.setAcceptedPolicyVersion(POLICY_VERSION);
                    newUser.setAcceptedTermsAt(Timestamp.now());

                    db.collection("users").document(uid).set(newUser)
                            .addOnSuccessListener(unused -> {
                                ThemeManager.applyThemePreference(SignUpActivity.this, darkMode);
                                Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSignUp.setEnabled(true);
                                Toast.makeText(SignUpActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSignUp.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, buildAuthErrorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    private void showPolicyDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
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

    private ArrayList<String> parseInterests(String raw) {
        ArrayList<String> interests = new ArrayList<>();
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
}
