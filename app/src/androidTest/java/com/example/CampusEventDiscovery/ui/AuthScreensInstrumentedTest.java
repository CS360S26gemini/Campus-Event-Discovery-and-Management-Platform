package com.example.CampusEventDiscovery.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.SignInActivity;
import com.example.CampusEventDiscovery.SignUpActivity;
import com.example.CampusEventDiscovery.WelcomeActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthScreensInstrumentedTest {

    @Test
    public void welcomeScreen_showsPrimaryEntryActions() {
        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withId(R.id.btnSignIn)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCreateAccount)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDevBypass)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void signInScreen_showsM3FormControls() {
        try (ActivityScenario<SignInActivity> ignored = ActivityScenario.launch(SignInActivity.class)) {
            onView(withId(R.id.toolbarSignIn)).check(matches(isDisplayed()));
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.switchRememberMe)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSignIn)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDevBypass)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void signInScreen_acceptsTypingWithoutLayoutBreakage() {
        try (ActivityScenario<SignInActivity> ignored = ActivityScenario.launch(SignInActivity.class)) {
            onView(withId(R.id.etEmail)).perform(typeText("student@test.local"), closeSoftKeyboard());
            onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
            onView(withId(R.id.btnSignIn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void signUpScreen_showsTermsAndPrivacyDialogs() {
        try (ActivityScenario<SignUpActivity> ignored = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.toolbarSignUp)).check(matches(isDisplayed()));
            onView(withId(R.id.etFullName)).check(matches(isDisplayed()));
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.etRepeatPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.actvCampus)).check(matches(isDisplayed()));
            onView(withId(R.id.cbTerms)).check(matches(not(isChecked())));

            onView(withId(R.id.tvTermsLink)).perform(scrollTo(), click());
            onView(withText(R.string.terms_and_conditions)).check(matches(isDisplayed()));
            onView(withText(R.string.ok)).perform(click());

            onView(withId(R.id.tvPrivacyLink)).perform(scrollTo(), click());
            onView(withText(R.string.privacy_policy)).check(matches(isDisplayed()));
            onView(withText(R.string.ok)).perform(click());
        }
    }
}
