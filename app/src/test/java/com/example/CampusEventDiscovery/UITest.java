package com.example.CampusEventDiscovery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.CampusEventDiscovery.ui.profile.AccountSettingsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * UITest.java  —  Local UI tests using Robolectric and Espresso.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {30})
public class UITest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.print("RUNNING UI TEST: " + description.getMethodName() + " ... ");
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("PASS");
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("FAIL (" + e.getMessage() + ")");
        }
    };

    @Rule
    public ActivityScenarioRule<SignInActivity> signInRule =
            new ActivityScenarioRule<>(SignInActivity.class);

    // ─── SIGN-IN SCREEN ──────────────────────────────────────────────────────

    @Test
    public void signInScreen_essentialElementsVisible() {
        onView(withId(R.id.etEmail)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.etPassword)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.btnSignIn)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    @Test
    public void signInScreen_loginInteraction() {
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnSignIn)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    // ─── ACCOUNT SETTINGS SCREEN ─────────────────────────────────────────────

    @Test
    public void accountSettings_fieldsVisible() {
        try (var scenario = androidx.test.core.app.ActivityScenario.launch(AccountSettingsActivity.class)) {
            // Verify fields are present in the layout. Use withEffectiveVisibility to avoid rect-check issues in Robolectric.
            onView(withId(R.id.etFullName)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.etUniversity)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.btnSaveSettings)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }
}
