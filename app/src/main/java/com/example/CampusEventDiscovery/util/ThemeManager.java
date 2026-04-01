package com.example.CampusEventDiscovery.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores and applies the app's light/dark preference locally.
 */
public final class ThemeManager {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ThemeManager() {
    }

    public static void applyStoredTheme(Context context) {
        applyThemeMode(isDarkModeEnabled(context));
    }

    public static boolean applyThemePreference(Context context, boolean darkMode) {
        saveThemePreference(context, darkMode);
        return applyThemeMode(darkMode);
    }

    public static boolean syncThemePreference(Context context, boolean darkMode) {
        saveThemePreference(context, darkMode);
        return applyThemeMode(darkMode);
    }

    public static boolean isDarkModeEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, true);
    }

    private static void saveThemePreference(Context context, boolean darkMode) {
        getPrefs(context)
                .edit()
                .putBoolean(KEY_DARK_MODE, darkMode)
                .apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static boolean applyThemeMode(boolean darkMode) {
        int targetMode = darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;

        if (AppCompatDelegate.getDefaultNightMode() == targetMode) {
            return false;
        }

        AppCompatDelegate.setDefaultNightMode(targetMode);
        return true;
    }
}
