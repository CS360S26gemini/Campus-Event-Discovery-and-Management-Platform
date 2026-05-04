package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

/**
 * RefundPolicyTest.java
 *
 * Pure policy tests for the in-app refund window.
 */
public class RefundPolicyTest {

    @Test
    public void attendeeCancellationMoreThanThreeDaysAway_isRefundEligible() {
        Timestamp eventDate = new Timestamp(new Date(System.currentTimeMillis() + 4L * 24L * 60L * 60L * 1000L));
        Timestamp cancellation = Timestamp.now();

        assertTrue(EventRepository.isRefundEligible(eventDate, cancellation, false));
    }

    @Test
    public void attendeeCancellationWithinThreeDays_isNotRefundEligible() {
        Timestamp eventDate = new Timestamp(new Date(System.currentTimeMillis() + 2L * 24L * 60L * 60L * 1000L));
        Timestamp cancellation = Timestamp.now();

        assertFalse(EventRepository.isRefundEligible(eventDate, cancellation, false));
    }

    @Test
    public void organizerCancellationAtLeastFiveDaysAway_isAllowed() {
        Timestamp eventDate = new Timestamp(new Date(System.currentTimeMillis() + 6L * 24L * 60L * 60L * 1000L));

        assertTrue(EventRepository.isRefundEligible(eventDate, Timestamp.now(), true));
    }

    @Test
    public void organizerCancellationWithinFiveDays_isNotAllowed() {
        Timestamp eventDate = new Timestamp(new Date(System.currentTimeMillis() + 4L * 24L * 60L * 60L * 1000L));

        assertFalse(EventRepository.isRefundEligible(eventDate, Timestamp.now(), true));
    }

    @Test
    public void normalizeRefundAmount_neverReturnsNegative() {
        assertTrue(EventRepository.normalizeRefundAmount(-250.0) == 0.0);
    }

    @Test
    public void resolveRefundAmount_prefersTierPriceWhenPresent() {
        double resolved = EventRepository.resolveRefundAmount(1800.0, 1200.0, 900.0);
        assertTrue(resolved == 1800.0);
    }

    @Test
    public void resolveRefundAmount_fallsBackToAmountThenTicketPrice() {
        double fromAmount = EventRepository.resolveRefundAmount(null, 1200.0, 900.0);
        double fromTicketPrice = EventRepository.resolveRefundAmount(null, null, 900.0);

        assertTrue(fromAmount == 1200.0);
        assertTrue(fromTicketPrice == 900.0);
    }

    @Test
    public void applyRefundPenalty_reducesRefundByConfiguredPercent() {
        assertTrue(EventRepository.applyRefundPenalty(2000.0, 25.0) == 1500.0);
    }

    @Test
    public void sanitizeRefundPenalty_clampsToAllowedRange() {
        assertTrue(EventRepository.sanitizeRefundPenaltyPercent(-5.0) == 0.0);
        assertTrue(EventRepository.sanitizeRefundPenaltyPercent(75.0) == 50.0);
    }
}
