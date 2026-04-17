package com.example.CampusEventDiscovery;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.example.CampusEventDiscovery.util.ThemeManager;

/**
 * Applies persisted UI preferences before any screen is shown.
 */
public class CampusEventDiscoveryApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        ThemeManager.applyStoredTheme(this);
    }
}
