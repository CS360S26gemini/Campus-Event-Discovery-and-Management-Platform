package com.example.campuseventdiscovery;

import android.app.Application;

import com.example.campuseventdiscovery.util.ThemeManager;

/**
 * Applies persisted UI preferences before any screen is shown.
 */
public class CampusEventDiscoveryApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyStoredTheme(this);
    }
}
