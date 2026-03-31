package com.example.campuseventdiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.util.DevSessionManager;
import com.example.campuseventdiscovery.util.UserRoles;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * TempLoginActivity.java
 *
 * Local role picker used to bypass sign-in during development and QA.
 */
public class TempLoginActivity extends AppCompatActivity {

    private MaterialButton btnContinueAttendee;
    private MaterialButton btnContinueOrganizer;
    private MaterialButton btnContinueAdmin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_login);

        btnContinueAttendee = findViewById(R.id.btnContinueAttendee);
        btnContinueOrganizer = findViewById(R.id.btnContinueOrganizer);
        btnContinueAdmin = findViewById(R.id.btnContinueAdmin);

        btnContinueAttendee.setOnClickListener(v -> continueInTestMode(UserRoles.ATTENDEE));
        btnContinueOrganizer.setOnClickListener(v -> continueInTestMode(UserRoles.ORGANIZER));
        btnContinueAdmin.setOnClickListener(v -> continueInTestMode(UserRoles.ADMIN));
    }

    private void continueInTestMode(String role) {
        FirebaseAuth.getInstance().signOut();
        DevSessionManager.enableBypass(this, role);

        Toast.makeText(
                this,
                getString(R.string.test_mode_enabled, DevSessionManager.getDisplayName(this)),
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
