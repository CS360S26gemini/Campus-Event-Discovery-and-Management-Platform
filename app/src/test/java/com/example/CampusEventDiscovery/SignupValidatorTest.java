package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.CampusEventDiscovery.util.SignupValidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class SignupValidatorTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.print("RUNNING: " + description.getMethodName() + " ... ");
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

    @Test
    public void validate_validInput_returnsNull() {
        String result = SignupValidator.validate(
                "John Doe",
                "john@example.com",
                "Password123!",
                "Password123!",
                "attendee"
        );
        assertNull(result);
    }

    @Test
    public void validate_emptyName_returnsError() {
        String result = SignupValidator.validate(
                "",
                "john@example.com",
                "Password123!",
                "Password123!",
                "attendee"
        );
        assertEquals("Name is required", result);
    }

    @Test
    public void validate_invalidEmail_returnsError() {
        String result = SignupValidator.validate(
                "John Doe",
                "invalid-email",
                "Password123!",
                "Password123!",
                "attendee"
        );
        assertEquals("Please enter a valid email address", result);
    }

    @Test
    public void validate_shortPassword_returnsError() {
        String result = SignupValidator.validate(
                "John Doe",
                "john@example.com",
                "Pass1!",
                "Pass1!",
                "attendee"
        );
        assertEquals("Password must be at least 8 characters", result);
    }

    @Test
    public void validate_mismatchedPasswords_returnsError() {
        String result = SignupValidator.validate(
                "John Doe",
                "john@example.com",
                "Password123!",
                "Password456!",
                "attendee"
        );
        assertEquals("Passwords do not match", result);
    }

    @Test
    public void validate_missingSpecialChar_returnsError() {
        String result = SignupValidator.validate(
                "John Doe",
                "john@example.com",
                "Password123",
                "Password123",
                "attendee"
        );
        assertEquals("Password must contain at least one special character", result);
    }
}
