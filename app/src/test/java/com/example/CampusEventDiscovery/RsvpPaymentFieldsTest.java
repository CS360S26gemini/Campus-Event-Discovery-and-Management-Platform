package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.util.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * RsvpPaymentFieldsTest.java
 *
 * Ensures the new paymentRef field behaves correctly and is independent
 * of existing fields. Required by Sprint Manual §5.3.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class RsvpPaymentFieldsTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Test
    public void emptyConstructor_paymentRefIsNull() {
        Rsvp rsvp = new Rsvp();
        assertNull("paymentRef must default to null for legacy docs", rsvp.getPaymentRef());
    }

    @Test
    public void setPaymentRef_roundTripsValue() {
        Rsvp rsvp = new Rsvp();
        rsvp.setPaymentRef("pi_test_abcdef0123456789abcdef01");
        assertEquals("pi_test_abcdef0123456789abcdef01", rsvp.getPaymentRef());
    }

    @Test
    public void setPaymentRef_null_isStoredAsNull() {
        Rsvp rsvp = new Rsvp();
        rsvp.setPaymentRef("pi_test_something");
        rsvp.setPaymentRef(null);
        assertNull(rsvp.getPaymentRef());
    }

    @Test
    public void setPaymentRef_emptyString_isStored() {
        Rsvp rsvp = new Rsvp();
        rsvp.setPaymentRef("");
        assertEquals("", rsvp.getPaymentRef());
    }

    @Test
    public void paymentRef_independentFromTransactionId() {
        // paymentRef and transactionId happen to be the same today, but
        // the fields are structurally independent.
        Rsvp rsvp = new Rsvp();
        rsvp.setTransactionId("txn_old");
        rsvp.setPaymentRef("pi_test_new");
        assertEquals("txn_old", rsvp.getTransactionId());
        assertEquals("pi_test_new", rsvp.getPaymentRef());
        assertNotEquals(rsvp.getTransactionId(), rsvp.getPaymentRef());
    }

    @Test
    public void paymentStatus_acceptsAllThreeEnumValues() {
        Rsvp rsvp = new Rsvp();

        rsvp.setPaymentStatus(Constants.PAYMENT_PENDING);
        assertEquals("PENDING", rsvp.getPaymentStatus());

        rsvp.setPaymentStatus(Constants.PAYMENT_CONFIRMED);
        assertEquals("CONFIRMED", rsvp.getPaymentStatus());

        rsvp.setPaymentStatus(Constants.PAYMENT_REJECTED);
        assertEquals("REJECTED", rsvp.getPaymentStatus());
    }

    @Test
    public void paymentFields_settingDoesNotAffectOtherFields() {
        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("u1");
        rsvp.setEventId("e1");
        rsvp.setStatus("confirmed");
        rsvp.setCheckedIn(false);

        rsvp.setPaymentRef("pi_test_foo");
        rsvp.setPaymentStatus(Constants.PAYMENT_CONFIRMED);

        assertEquals("u1", rsvp.getUserId());
        assertEquals("e1", rsvp.getEventId());
        assertEquals("confirmed", rsvp.getStatus());
        assertEquals(false, rsvp.isCheckedIn());
    }

    @Test
    public void fullConstructor_stillWorksWithoutPaymentRef() {
        // The existing constructor signature is untouched — legacy call
        // sites continue to compile and produce an Rsvp with paymentRef null.
        Rsvp rsvp = new Rsvp(
                "rsvp1", "u1", "e1", "Title", null,
                "confirmed", Constants.PAYMENT_CONFIRMED, "txn1", "{}",
                false, false, null
        );
        assertNull(rsvp.getPaymentRef());
        assertEquals("txn1", rsvp.getTransactionId());
    }
}
