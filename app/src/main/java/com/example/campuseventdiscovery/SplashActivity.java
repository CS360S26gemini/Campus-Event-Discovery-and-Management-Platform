package com.example.campuseventdiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.util.DevSessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity.java
 *
 * Simple splash screen shown when the app launches.
 * Routes users into the authenticated app or the welcome flow.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MILLIS = 1500L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null || DevSessionManager.shouldUseBypass(SplashActivity.this)) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, TempLoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MILLIS);
    }
}
