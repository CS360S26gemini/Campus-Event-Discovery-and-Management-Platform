package com.example.campuseventdiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.example.campuseventdiscovery.util.UserRoles;
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
    private final EventRepository repository = new EventRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeFromSplash, SPLASH_DELAY_MILLIS);
    }

    private void routeFromSplash() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null && DevSessionManager.shouldUseBypass(this)) {
            openActivity(MainActivity.class);
            return;
        }

        repository.getMaintenanceMode(new EventRepository.BooleanCallback() {
            @Override
            public void onSuccess(boolean maintenanceMode) {
                if (!maintenanceMode) {
                    openDefaultDestination(currentUser);
                    return;
                }

                if (currentUser == null) {
                    openActivity(MaintenanceActivity.class);
                    return;
                }

                repository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
                    @Override
                    public void onSuccess(com.example.campuseventdiscovery.model.User user) {
                        if (user != null && UserRoles.isAdmin(user.getRole())) {
                            openActivity(MainActivity.class);
                        } else {
                            openActivity(MaintenanceActivity.class);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        openActivity(MaintenanceActivity.class);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                openDefaultDestination(currentUser);
            }
        });
    }

    private void openDefaultDestination(FirebaseUser currentUser) {
        if (currentUser != null) {
            openActivity(MainActivity.class);
        } else {
            openActivity(WelcomeActivity.class);
        }
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}
