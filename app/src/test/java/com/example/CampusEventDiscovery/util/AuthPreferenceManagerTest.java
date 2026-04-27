package com.example.CampusEventDiscovery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class AuthPreferenceManagerTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void saveRememberChoice_enabled_storesEmailAndTrueFlag() {
        AuthPreferenceManager.saveRememberChoice(context, true, "student@example.com");

        assertTrue(AuthPreferenceManager.isRememberMeEnabled(context));
        assertEquals("student@example.com", AuthPreferenceManager.getRememberedEmail(context));
    }

    @Test
    public void saveRememberChoice_disabled_clearsEmailAndStoresFalseFlag() {
        AuthPreferenceManager.saveRememberChoice(context, true, "student@example.com");
        AuthPreferenceManager.saveRememberChoice(context, false, "student@example.com");

        assertFalse(AuthPreferenceManager.isRememberMeEnabled(context));
        assertEquals("", AuthPreferenceManager.getRememberedEmail(context));
    }

    @Test
    public void getRememberedEmail_withoutSavedEmail_returnsEmptyString() {
        assertEquals("", AuthPreferenceManager.getRememberedEmail(context));
    }

    @Test
    public void saveRememberChoice_doesNotStorePassword() {
        AuthPreferenceManager.saveRememberChoice(context, true, "student@example.com");

        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        assertFalse(prefs.contains("password"));
        assertFalse(prefs.contains("remembered_password"));
    }
}
