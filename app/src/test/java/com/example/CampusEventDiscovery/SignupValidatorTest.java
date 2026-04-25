package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.CampusEventDiscovery.util.SignupValidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/**
 * SignupValidatorTest.java
 *
 * Comprehensive tests for SignupValidator.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class SignupValidatorTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void validate_validAttendee_returnsNull() {
        assertNull(SignupValidator.validate(
                "John Doe", "john@example.com", "Password123!", "Password123!", "attendee"));
    }

    @Test
    public void validate_validOrganizer_returnsNull() {
        assertNull(SignupValidator.validate(
                "Jane Smith", "jane@example.com", "Secure@99", "Secure@99", "organizer"));
    }

    @Test
    public void validate_emptyName_returnsError() {
        assertNotNull("Should return error for empty name",
                SignupValidator.validate("", "john@example.com", "Password123!", "Password123!", "attendee"));
    }

    @Test
    public void validate_invalidEmail_returnsError() {
        assertNotNull("Should return error for invalid email",
                SignupValidator.validate("John", "invalidemail", "Password123!", "Password123!", "attendee"));
    }

    @Test
    public void validate_shortPassword_returnsError() {
        assertNotNull("Should return error for short password",
                SignupValidator.validate("John", "john@example.com", "Pass1!", "Pass1!", "attendee"));
    }

    @Test
    public void validate_missingSpecialChar_returnsError() {
        assertNotNull("Should return error for missing special char",
                SignupValidator.validate("John", "john@example.com", "Password123", "Password123", "attendee"));
    }

    @Test
    public void validate_mismatchedPasswords_returnsError() {
        assertNotNull("Should return error for mismatched passwords",
                SignupValidator.validate("John", "john@example.com", "Password123!", "Password456!", "attendee"));
    }

    @Test
    public void validate_emptyRole_returnsError() {
        assertNotNull("Should return error for empty role",
                SignupValidator.validate("John", "john@example.com", "Password123!", "Password123!", ""));
    }

    @Test
    public void validate_invalidRole_returnsError() {
        assertNotNull("Should return error for invalid role",
                SignupValidator.validate("John", "john@example.com", "Password123!", "Password123!", "superuser"));
    }

    @Test
    public void validate_adminRole_notAllowedViaSignup_returnsError() {
        assertNotNull("Should return error for admin role signup",
                SignupValidator.validate("Admin", "admin@example.com", "Password123!", "Password123!", "admin"));
    }

    @Test
    public void validateInterests_fewerThanThreeRejected() {
        assertEquals(false, SignupValidator.hasMinimumSelectedInterests(Arrays.asList("Music", "Sports")));
    }

    @Test
    public void validateInterests_exactlyThreeAccepted() {
        assertEquals(true, SignupValidator.hasMinimumSelectedInterests(Arrays.asList("Music", "Sports", "Career")));
    }
}
