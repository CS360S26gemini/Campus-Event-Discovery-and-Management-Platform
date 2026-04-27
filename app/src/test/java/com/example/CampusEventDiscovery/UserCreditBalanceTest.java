package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;

import com.example.CampusEventDiscovery.model.User;

import org.junit.Test;

import java.util.ArrayList;

/**
 * UserCreditBalanceTest.java
 *
 * Ensures the new credit balance field behaves like a normal Firestore POJO field.
 */
public class UserCreditBalanceTest {

    @Test
    public void emptyConstructor_creditBalanceDefaultsToZero() {
        User user = new User();
        assertEquals(0.0, user.getCreditBalance(), 0.0);
    }

    @Test
    public void creditBalance_roundTripsThroughSetter() {
        User user = new User("Name", "a@example.com", "attendee", "LUMS", "Lahore", "", new ArrayList<>(), false);
        user.setCreditBalance(1250.0);
        assertEquals(1250.0, user.getCreditBalance(), 0.0);
    }
}
