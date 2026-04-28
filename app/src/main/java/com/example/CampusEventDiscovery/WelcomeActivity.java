package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.google.android.material.button.MaterialButton;
import com.example.CampusEventDiscovery.util.DevSessionManager;

/**
 * WelcomeActivity.java
 *
 * Entry screen for new and returning users to choose between Sign In and Sign Up.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        MaterialButton btnDevBypass = findViewById(R.id.btnDevBypass);

        btnSignIn.setOnClickListener(v -> {
            DevSessionManager.clearBypass(this);
            Intent intent = new Intent(WelcomeActivity.this, SignInActivity.class);
            startActivity(intent);
        });

        btnCreateAccount.setOnClickListener(v -> {
            DevSessionManager.clearBypass(this);
            Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        btnDevBypass.setOnClickListener(v -> DevBypassHelper.showRolePicker(this));
    }
}
