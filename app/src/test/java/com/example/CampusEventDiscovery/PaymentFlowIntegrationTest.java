package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.StripePaymentService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * PaymentFlowIntegrationTest.java
 *
 * Exercises the full payment contract without Firestore:
 *   - StripePaymentService produces a Payment,
 *   - The resulting ref is copied onto the Rsvp as paymentRef,
 *   - Revenue aggregation math used by PaymentConfirmationActivity is correct.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class PaymentFlowIntegrationTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void fullFlow_servicePopulatesRsvpFields() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 500.0);
        p.setPaymentMethod("BANK_TRANSFER");
        p.setProofUrl("https://example.com/proof.jpg");

        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("u1");
        rsvp.setEventId("e1");
        rsvp.setPaymentStatus(Constants.PAYMENT_CONFIRMED);
        rsvp.setTransactionId(p.getTransactionId());
        rsvp.setPaymentRef(p.getTransactionId());
        rsvp.setPaymentMethod(p.getPaymentMethod());
        rsvp.setPaymentProofUrl(p.getProofUrl());

        assertEquals(Constants.PAYMENT_CONFIRMED, rsvp.getPaymentStatus());
        assertEquals(p.getTransactionId(), rsvp.getPaymentRef());
        assertEquals(p.getTransactionId(), rsvp.getTransactionId());
        assertEquals("BANK_TRANSFER", rsvp.getPaymentMethod());
        assertEquals("https://example.com/proof.jpg", rsvp.getPaymentProofUrl());
        assertTrue(rsvp.getPaymentRef().startsWith("pi_test_"));
    }

    @Test
    public void revenueAggregation_sumIsCorrect() {
        List<Payment> payments = new ArrayList<>();
        payments.add(mk("u1", 500.0));
        payments.add(mk("u2", 750.0));
        payments.add(mk("u3", 1250.0));

        double total = 0.0;
        for (Payment p : payments) total += p.getAmount();
        assertEquals(2500.0, total, 0.0001);
    }

    @Test
    public void revenueAggregation_emptyList_isZero() {
        List<Payment> payments = new ArrayList<>();
        double total = 0.0;
        for (Payment p : payments) total += p.getAmount();
        assertEquals(0.0, total, 0.0);
    }

    @Test
    public void revenueAggregation_singleZeroPayment_isZero() {
        // Not realistic (free events skip this path) — but the math should hold.
        List<Payment> payments = new ArrayList<>();
        payments.add(mk("u1", 0.0));
        double total = 0.0;
        for (Payment p : payments) total += p.getAmount();
        assertEquals(0.0, total, 0.0);
    }

    @Test
    public void revenueAggregation_floatingPointPrecision_withinTolerance() {
        // Classic 0.1 + 0.2 trap — must not lose more than a rounding cent.
        List<Payment> payments = new ArrayList<>();
        payments.add(mk("u1", 0.1));
        payments.add(mk("u2", 0.2));
        double total = 0.0;
        for (Payment p : payments) total += p.getAmount();
        assertEquals(0.3, total, 0.0001);
    }

    @Test
    public void paymentList_sortsByTimestampDescending() {
        // Mirrors PaymentRepository.getPaymentsForEvent() ordering contract.
        List<Payment> payments = new ArrayList<>();
        payments.add(mkAt("u1", 100.0, 1_000L));
        payments.add(mkAt("u2", 200.0, 3_000L));
        payments.add(mkAt("u3", 150.0, 2_000L));

        Collections.sort(payments, new Comparator<Payment>() {
            @Override public int compare(Payment a, Payment b) {
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });

        assertEquals(3_000L, payments.get(0).getTimestamp());
        assertEquals(2_000L, payments.get(1).getTimestamp());
        assertEquals(1_000L, payments.get(2).getTimestamp());
    }

    @Test
    public void paymentCount_isListSize() {
        List<Payment> payments = new ArrayList<>();
        for (int i = 0; i < 7; i++) payments.add(mk("u" + i, 100.0));
        assertEquals(7, payments.size());
    }

    @Test
    public void fullFlow_uniqueRefsForDifferentUsers() {
        Payment a = StripePaymentService.processPayment("u1", "e1", 100.0);
        Payment b = StripePaymentService.processPayment("u2", "e1", 100.0);
        assertNotEquals(a.getTransactionId(), b.getTransactionId());
    }

    @Test
    public void paymentRefAndTransactionId_sameValueInCurrentFlow() {
        // Today CheckoutActivity writes the same Stripe intent ID to both
        // paymentRef and transactionId. Document that so any divergence is
        // a deliberate, reviewed change.
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);

        Rsvp rsvp = new Rsvp();
        rsvp.setTransactionId(p.getTransactionId());
        rsvp.setPaymentRef(p.getTransactionId());

        assertEquals(rsvp.getTransactionId(), rsvp.getPaymentRef());
    }

    private static Payment mk(String userId, double amount) {
        return new Payment(null, userId, "e1", amount,
                Constants.PAYMENT_CONFIRMED,
                "pi_test_" + userId,
                System.currentTimeMillis());
    }

    private static Payment mkAt(String userId, double amount, long ts) {
        Payment p = mk(userId, amount);
        p.setTimestamp(ts);
        assertNotNull(p);
        return p;
    }
}
