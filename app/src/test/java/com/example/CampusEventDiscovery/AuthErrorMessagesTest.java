package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;

import com.example.CampusEventDiscovery.util.AuthErrorMessages;
import com.example.CampusEventDiscovery.util.SignupValidator;

import org.junit.Test;

public class AuthErrorMessagesTest {

    @Test
    public void signIn_wrongPasswordShowsPasswordMessage() {
        assertEquals(
                "Incorrect password. Please try again.",
                AuthErrorMessages.forSignInCode("ERROR_WRONG_PASSWORD")
        );
    }

    @Test
    public void signIn_invalidCredentialDoesNotBlameOnlyEmail() {
        assertEquals(
                "Email or password is incorrect. Please check both and try again.",
                AuthErrorMessages.forSignInCode("ERROR_INVALID_CREDENTIAL")
        );
    }

    @Test
    public void signUp_weakPasswordShowsRequirements() {
        assertEquals(
                SignupValidator.PASSWORD_REQUIREMENTS_MESSAGE,
                AuthErrorMessages.forSignUpCode("ERROR_WEAK_PASSWORD")
        );
    }

    @Test
    public void accountUpdate_invalidCredentialShowsCurrentPasswordMessage() {
        assertEquals(
                "Current password is incorrect. Please try again.",
                AuthErrorMessages.forAccountUpdateCode("ERROR_INVALID_CREDENTIAL")
        );
    }
}
