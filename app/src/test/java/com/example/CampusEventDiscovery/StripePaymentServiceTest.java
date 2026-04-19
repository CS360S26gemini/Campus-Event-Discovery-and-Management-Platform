package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.StripePaymentService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * StripePaymentServiceTest.java
 *
 * Payment-shim tests. Covers ref shape, status, amount propagation,
 * uniqueness under load, and null/empty id handling. Pure unit tests —
 * no Firestore, no network.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class StripePaymentServiceTest {

    private static final Pattern PI_TEST_SHAPE =
            Pattern.compile("^pi_test_[a-f0-9]{24}$");

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void processPayment_returnsNonNullPayment() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 500.0);
        assertNotNull("Payment must not be null for valid inputs", p);
    }

    @Test
    public void processPayment_refMatchesStripePaymentIntentShape() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        assertNotNull(p.getTransactionId());
        assertTrue("Ref must look like pi_test_<24 hex>: " + p.getTransactionId(),
                PI_TEST_SHAPE.matcher(p.getTransactionId()).matches());
    }

    @Test
    public void processPayment_statusIsConfirmed() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        assertEquals(Constants.PAYMENT_CONFIRMED, p.getStatus());
    }

    @Test
    public void processPayment_amountIsPreservedExactly() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 1234.56);
        assertEquals(1234.56, p.getAmount(), 0.0001);
    }

    @Test
    public void processPayment_zeroAmountStillProducesPayment() {
        // Free events never reach this path in CheckoutActivity, but the
        // service itself must not crash on zero.
        Payment p = StripePaymentService.processPayment("u1", "e1", 0.0);
        assertNotNull(p);
        assertEquals(0.0, p.getAmount(), 0.0001);
    }

    @Test
    public void processPayment_veryLargeAmountAccepted() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 999_999_999.99);
        assertEquals(999_999_999.99, p.getAmount(), 0.01);
    }

    @Test
    public void processPayment_negativeAmountStillReturnsPayment() {
        // Service doesn't validate — caller (CheckoutActivity) does.
        // Still document behaviour: no NPE, echoes amount as-is.
        Payment p = StripePaymentService.processPayment("u1", "e1", -50.0);
        assertEquals(-50.0, p.getAmount(), 0.0001);
    }

    @Test
    public void processPayment_userIdPreserved() {
        Payment p = StripePaymentService.processPayment("user_abc123", "e1", 100.0);
        assertEquals("user_abc123", p.getUserId());
    }

    @Test
    public void processPayment_eventIdPreserved() {
        Payment p = StripePaymentService.processPayment("u1", "event_xyz", 100.0);
        assertEquals("event_xyz", p.getEventId());
    }

    @Test
    public void processPayment_timestampIsRecent() {
        long before = System.currentTimeMillis();
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        long after = System.currentTimeMillis();
        assertTrue("timestamp >= before", p.getTimestamp() >= before);
        assertTrue("timestamp <= after",  p.getTimestamp() <= after);
    }

    @Test
    public void processPayment_paymentIdIsNullBeforePersist() {
        // paymentId is the Firestore doc id, assigned by PaymentRepository.savePayment.
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        assertNull("paymentId must be null before savePayment runs", p.getPaymentId());
    }

    @Test
    public void processPayment_refsUniqueAcross1000Calls() {
        Set<String> refs = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
            refs.add(p.getTransactionId());
        }
        assertEquals("All 1000 refs must be unique", 1000, refs.size());
    }

    @Test
    public void processPayment_twoSuccessiveCallsHaveDifferentRefs() {
        Payment a = StripePaymentService.processPayment("u1", "e1", 100.0);
        Payment b = StripePaymentService.processPayment("u1", "e1", 100.0);
        assertNotEquals(a.getTransactionId(), b.getTransactionId());
    }

    @Test
    public void processPayment_nullUserIdTolerated() {
        // Defensive — CheckoutActivity guards against this, but the shim
        // itself must not throw.
        Payment p = StripePaymentService.processPayment(null, "e1", 100.0);
        assertNotNull(p);
        assertNull(p.getUserId());
    }

    @Test
    public void processPayment_nullEventIdTolerated() {
        Payment p = StripePaymentService.processPayment("u1", null, 100.0);
        assertNotNull(p);
        assertNull(p.getEventId());
    }

    @Test
    public void processPayment_emptyIdsTolerated() {
        Payment p = StripePaymentService.processPayment("", "", 100.0);
        assertNotNull(p);
        assertEquals("", p.getUserId());
        assertEquals("", p.getEventId());
    }

    @Test
    public void processPayment_refIsLowercaseHex() {
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        String hex = p.getTransactionId().substring("pi_test_".length());
        assertFalse("Ref must be lowercase", !hex.equals(hex.toLowerCase()));
    }

    @Test
    public void processPayment_refLengthExactly32Chars() {
        // "pi_test_" (8) + 24 hex = 32
        Payment p = StripePaymentService.processPayment("u1", "e1", 100.0);
        assertEquals(32, p.getTransactionId().length());
    }
}
