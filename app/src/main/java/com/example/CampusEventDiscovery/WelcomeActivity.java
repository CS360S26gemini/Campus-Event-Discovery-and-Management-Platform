package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.util.DevBypassHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.button.MaterialButton;

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

        MaterialButton btnLogIn = findViewById(R.id.btnLogIn);
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);
        MaterialButton btnDevBypass = findViewById(R.id.btnDevBypass);

        btnLogIn.setOnClickListener(v -> openAuthScreen(SignInActivity.class));
        btnSignUp.setOnClickListener(v -> openAuthScreen(SignUpActivity.class));

        btnDevBypass.setOnClickListener(v -> DevBypassHelper.showRolePicker(this));
    }

    private void openAuthScreen(Class<?> destinationActivity) {
        DevSessionManager.clearBypass(this);

        Intent intent = new Intent(this, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
