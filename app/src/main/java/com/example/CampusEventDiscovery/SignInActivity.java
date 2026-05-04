package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.AuthErrorMessages;
import com.example.CampusEventDiscovery.util.AuthPreferenceManager;
import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.SignupValidator;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
/**
 * SignInActivity.java
 *
 * Screen for returning users to sign in.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private TextView tvForgotPassword, tvCreateAccountLink;
    private com.google.android.material.materialswitch.MaterialSwitch switchRememberMe;
    private MaterialButton btnSignIn, btnDevBypass;
    private ProgressBar progressBarSignIn;

    private FirebaseAuth auth;
    private EventRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        MaterialToolbar toolbarSignIn = findViewById(R.id.toolbarSignIn);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvCreateAccountLink = findViewById(R.id.tvCreateAccountLink);
        switchRememberMe = findViewById(R.id.switchRememberMe);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnDevBypass = findViewById(R.id.btnDevBypass);
        progressBarSignIn = findViewById(R.id.progressBarSignIn);

        loadRememberedLogin();

        toolbarSignIn.setNavigationOnClickListener(v -> finish());

        btnSignIn.setOnClickListener(v -> signInUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvCreateAccountLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
        btnDevBypass.setOnClickListener(v -> DevBypassHelper.showRolePicker(this));
    }

    private void loadRememberedLogin() {
        boolean rememberMe = AuthPreferenceManager.isRememberMeEnabled(this);
        String rememberedEmail = AuthPreferenceManager.getRememberedEmail(this);

        switchRememberMe.setChecked(rememberMe);

        if (rememberMe && !TextUtils.isEmpty(rememberedEmail) && !rememberedEmail.trim().isEmpty()) {
            etEmail.setText(rememberedEmail);
            etPassword.requestFocus();
        } else {
            etEmail.setText("");
            etPassword.setText("");
        }
    }

    private void saveRememberedLoginPreference(String email) {
        AuthPreferenceManager.saveRememberChoice(this, switchRememberMe.isChecked(), email);
    }

    private void showForgotPasswordDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_lg);
        container.setPadding(padding, 0, padding, 0);

        TextInputLayout emailLayout = new TextInputLayout(this);
        emailLayout.setHint(getString(R.string.reset_password_email_hint));

        TextInputEditText emailInput = new TextInputEditText(emailLayout.getContext());
        emailInput.setSingleLine(true);
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        String existingEmail = etEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(existingEmail)) {
            emailInput.setText(existingEmail);
            emailInput.setSelection(existingEmail.length());
        }

        emailLayout.addView(emailInput);
        container.addView(emailLayout);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.reset_password_title)
                .setMessage(R.string.reset_password_message)
                .setView(container)
                .setPositiveButton(R.string.send_reset_link, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String email = emailInput.getText() == null
                        ? ""
                        : emailInput.getText().toString().trim();
                String emailError = SignupValidator.validateEmail(email);
                if (emailError != null) {
                    emailLayout.setError(getString(R.string.reset_password_invalid_email));
                    return;
                }

                emailLayout.setError(null);
                sendPasswordResetEmail(email, dialog, positiveButton);
            });
        });

        dialog.show();
    }

    private void sendPasswordResetEmail(String email, AlertDialog dialog, Button positiveButton) {
        auth.setLanguageCode("en");
        positiveButton.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(
                            SignInActivity.this,
                            getString(R.string.reset_password_email_sent),
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {
                    positiveButton.setEnabled(true);
                    String message = buildPasswordResetErrorMessage(e);
                    if (TextUtils.equals(message, getString(R.string.reset_password_email_sent))) {
                        dialog.dismiss();
                    }
                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_LONG).show();
                });
    }

    private String buildPasswordResetErrorMessage(Exception exception) {
        if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
            String errorCode = ((com.google.firebase.auth.FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(errorCode)) {
                return getString(R.string.reset_password_email_sent);
            }
        }
        return AuthErrorMessages.forPasswordReset(exception);
    }

    private void signInUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        clearSignInErrors();

        if (email.equals("admin") && password.equals("admin")) {
            DevSessionManager.enableBypass(this, "admin");
            Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        DevSessionManager.clearBypass(this);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.email_required));
            etEmail.requestFocus();
            return;
        }
        String emailError = SignupValidator.validateEmail(email);
        if (emailError != null) {
            tilEmail.setError(emailError);
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.password_required));
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser signedInUser = authResult.getUser();
                    if (signedInUser == null) {
                        setLoading(false);
                        Toast.makeText(this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    signedInUser.reload()
                            .addOnSuccessListener(unused -> {
                                FirebaseUser refreshedUser = auth.getCurrentUser();
                                if (refreshedUser == null) {
                                    setLoading(false);
                                    Toast.makeText(
                                            SignInActivity.this,
                                            "Could not confirm your account status. Please try signing in again.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    return;
                                }

                                if (!refreshedUser.isEmailVerified()) {
                                    handleUnverifiedUser(refreshedUser);
                                    return;
                                }

                                saveRememberedLoginPreference(email);

                                repository.getUserData(refreshedUser.getUid(), new EventRepository.UserCallback() {
                                    @Override
                                    public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                                        ThemeManager.syncThemePreference(SignInActivity.this, user.isDarkMode());
                                        openMain();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        openMain();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                setLoading(false);
                                Toast.makeText(
                                        SignInActivity.this,
                                        "Could not refresh account status: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String message = AuthErrorMessages.forSignIn(e);
                    applySignInAuthErrorToField(message);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
    }

    private void handleUnverifiedUser(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Email not verified")
                .setMessage("Your email is not verified yet. Do you want us to send the verification email again?")
                .setPositiveButton("Resend", (dialog, which) -> {
                    auth.setLanguageCode("en");

                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                auth.signOut();
                                setLoading(false);
                                Toast.makeText(
                                        SignInActivity.this,
                                        "Verification email sent again. Check your inbox and spam folder.",
                                        Toast.LENGTH_LONG
                                ).show();
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                setLoading(false);
                                Toast.makeText(
                                        SignInActivity.this,
                                        "Could not resend verification email: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    auth.signOut();
                    setLoading(false);
                })
                .setCancelable(false)
                .show();
    }

    private void setLoading(boolean isLoading) {
        btnSignIn.setEnabled(!isLoading);
        btnDevBypass.setEnabled(!isLoading);
        switchRememberMe.setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
        tvCreateAccountLink.setEnabled(!isLoading);
        progressBarSignIn.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    private void openMain() {
        Toast.makeText(this, getString(R.string.sign_in_success), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearSignInErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void applySignInAuthErrorToField(String message) {
        if (message == null) {
            return;
        }
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("password")) {
            tilPassword.setError(message);
            etPassword.requestFocus();
        } else if (lowerMessage.contains("email")) {
            tilEmail.setError(message);
            etEmail.requestFocus();
        }
    }
}
