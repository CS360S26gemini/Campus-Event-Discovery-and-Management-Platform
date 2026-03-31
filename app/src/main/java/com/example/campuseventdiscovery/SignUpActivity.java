package com.example.campuseventdiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * SignUpActivity.java
 *
 * Screen for new users to register an account.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etRepeatPassword;
    private MaterialButtonToggleGroup toggleUserType;
    private MaterialButton btnSignUp;
    private ImageButton btnBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        toggleUserType = findViewById(R.id.toggleUserType);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnBack.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> signUpUser());
    }

    private void signUpUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = toggleUserType.getCheckedButtonId() == R.id.btnOrganizer ? "organizer" : "attendee";
        boolean darkMode = ThemeManager.isDarkModeEnabled(this);

        btnSignUp.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    User newUser = new User(
                            fullName,
                            email,
                            role,
                            "", // university
                            "", // location
                            "", // profilePicUrl
                            new ArrayList<>(),
                            darkMode
                    );

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
                    Toast.makeText(SignUpActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
