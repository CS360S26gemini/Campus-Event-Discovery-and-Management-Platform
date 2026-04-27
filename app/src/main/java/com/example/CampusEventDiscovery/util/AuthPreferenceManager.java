package com.example.CampusEventDiscovery.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class AuthPreferenceManager {

    private AuthPreferenceManager() {}

    public static void saveRememberedEmail(Context context, String email) {
        getAuthPrefs(context)
                .edit()
                .putBoolean(Constants.PREF_REMEMBER_ME, true)
                .putString(Constants.PREF_REMEMBERED_EMAIL, email == null ? "" : email.trim())
                .apply();
    }

    public static void clearRememberedEmail(Context context) {
        getAuthPrefs(context)
                .edit()
                .putBoolean(Constants.PREF_REMEMBER_ME, false)
                .remove(Constants.PREF_REMEMBERED_EMAIL)
                .apply();
    }

    public static boolean isRememberMeEnabled(Context context) {
        return getAuthPrefs(context).getBoolean(Constants.PREF_REMEMBER_ME, false);
    }

    public static String getRememberedEmail(Context context) {
        return getAuthPrefs(context).getString(Constants.PREF_REMEMBERED_EMAIL, "");
    }

    public static void saveRememberChoice(Context context, boolean rememberMe, String email) {
        if (rememberMe && !TextUtils.isEmpty(email) && !email.trim().isEmpty()) {
            saveRememberedEmail(context, email);
            return;
        }

        clearRememberedEmail(context);
    }

    private static SharedPreferences getAuthPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
    }
}
