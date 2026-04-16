package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.util.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * PaymentModelTest.java
 *
 * POJO contract tests for Payment. Firestore deserialises via the
 * empty constructor + setters, so those must all work.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class PaymentModelTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void emptyConstructor_allFieldsDefault() {
        Payment p = new Payment();
        assertNull(p.getPaymentId());
        assertNull(p.getUserId());
        assertNull(p.getEventId());
        assertEquals(0.0, p.getAmount(), 0.0);
        assertNull(p.getStatus());
        assertNull(p.getTransactionId());
        assertEquals(0L, p.getTimestamp());
    }

    @Test
    public void fullConstructor_setsEveryField() {
        Payment p = new Payment(
                "pay1", "u1", "e1", 500.0,
                Constants.PAYMENT_CONFIRMED,
                "pi_test_abcdef0123456789abcdef01",
                1_713_000_000_000L
        );
        assertEquals("pay1", p.getPaymentId());
        assertEquals("u1", p.getUserId());
        assertEquals("e1", p.getEventId());
        assertEquals(500.0, p.getAmount(), 0.0);
        assertEquals("CONFIRMED", p.getStatus());
        assertEquals("pi_test_abcdef0123456789abcdef01", p.getTransactionId());
        assertEquals(1_713_000_000_000L, p.getTimestamp());
    }

    @Test
    public void setters_roundTripEveryField() {
        Payment p = new Payment();
        p.setPaymentId("p1");
        p.setUserId("u1");
        p.setEventId("e1");
        p.setAmount(1999.99);
        p.setStatus(Constants.PAYMENT_REJECTED);
        p.setTransactionId("pi_test_ref");
        p.setTimestamp(1234L);

        assertEquals("p1", p.getPaymentId());
        assertEquals("u1", p.getUserId());
        assertEquals("e1", p.getEventId());
        assertEquals(1999.99, p.getAmount(), 0.0001);
        assertEquals("REJECTED", p.getStatus());
        assertEquals("pi_test_ref", p.getTransactionId());
        assertEquals(1234L, p.getTimestamp());
    }

    @Test
    public void setAmount_negativeStillStored() {
        // Domain-layer (CheckoutActivity) enforces positivity.
        Payment p = new Payment();
        p.setAmount(-10.0);
        assertEquals(-10.0, p.getAmount(), 0.0001);
    }

    @Test
    public void setAmount_zeroAllowed() {
        Payment p = new Payment();
        p.setAmount(0.0);
        assertEquals(0.0, p.getAmount(), 0.0);
    }
}
