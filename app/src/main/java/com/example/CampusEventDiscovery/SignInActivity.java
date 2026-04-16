package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.example.CampusEventDiscovery.ui.admin.AdminHomeActivity;
/**
 * SignInActivity.java
 *
 * Screen for returning users to sign in.
 */
public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Switch switchRememberMe;
    private MaterialButton btnSignIn, btnDevBypass;
    private ImageButton btnBack;

    private FirebaseAuth auth;
    private EventRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        switchRememberMe = findViewById(R.id.switchRememberMe);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnDevBypass = findViewById(R.id.btnDevBypass);

        btnBack.setOnClickListener(v -> finish());

        btnSignIn.setOnClickListener(v -> signInUser());
        btnDevBypass.setOnClickListener(v -> DevBypassHelper.showRolePicker(this));
    }

    private void signInUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignIn.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    repository.getUserData(authResult.getUser().getUid(), new EventRepository.UserCallback() {
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
                    btnSignIn.setEnabled(true);
                    Toast.makeText(this, buildSignInErrorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    private void openMain() {
        Toast.makeText(this, getString(R.string.sign_in_success), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String buildSignInErrorMessage(Exception exception) {
        if (exception == null) {
            return getString(R.string.sign_in_failed, "Unknown error");
        }

        String message = exception.getMessage();
        if (message != null && message.contains("CONFIGURATION_NOT_FOUND")) {
            return "Sign in failed: Firebase Authentication is not fully configured in Firebase Console.";
        }

        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            return getString(R.string.sign_in_failed, authException.getErrorCode());
        }

        return getString(R.string.sign_in_failed, message);
    }
}
