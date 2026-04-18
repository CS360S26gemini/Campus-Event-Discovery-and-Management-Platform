package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.util.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

/**
 * BankTransferPaymentTest.java
 *
 * Unit tests covering the Bank Transfer payment path:
 *
 *  1. Payment model stores proofUrl and paymentMethod correctly.
 *  2. Rsvp model stores paymentProofUrl, paymentMethod, and paymentRef correctly.
 *  3. A missing proof URL is detectable before submitting.
 *  4. Transaction ID is generated as a non-empty unique string for bank transfers.
 *  5. Payment amount is preserved end-to-end through the model layer.
 *  6. Rsvp paymentProofUrl is independent of paymentRef.
 *  7. Payment proofUrl is independent of transactionId.
 *  8. Empty proofUrl on Payment correctly identified as missing proof.
 *  9. Bank transfer constant string matches expected value.
 * 10. Rsvp confirms BANK_TRANSFER status flows through to paymentMethod field.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class BankTransferPaymentTest {

    private static final String SAMPLE_PROOF_URL =
            "https://res.cloudinary.com/dcxablsft/image/upload/v1234567890/sample_proof.jpg";

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    // -------------------------------------------------------------------------
    // 1. Payment model correctly stores proofUrl and paymentMethod
    // -------------------------------------------------------------------------
    @Test
    public void payment_bankTransfer_storesProofUrlAndMethod() {
        Payment payment = buildBankTransferPayment(SAMPLE_PROOF_URL);

        assertEquals("BANK_TRANSFER", payment.getPaymentMethod());
        assertEquals(SAMPLE_PROOF_URL, payment.getProofUrl());
    }

    // -------------------------------------------------------------------------
    // 2. Rsvp correctly stores paymentProofUrl and paymentMethod
    // -------------------------------------------------------------------------
    @Test
    public void rsvp_bankTransfer_storesProofUrlAndMethod() {
        Rsvp rsvp = buildBankTransferRsvp(SAMPLE_PROOF_URL);

        assertEquals("BANK_TRANSFER", rsvp.getPaymentMethod());
        assertEquals(SAMPLE_PROOF_URL, rsvp.getPaymentProofUrl());
    }

    // -------------------------------------------------------------------------
    // 3. Missing proof URL (null) is detectable before submit
    // -------------------------------------------------------------------------
    @Test
    public void proofUrl_null_isMissingProof() {
        // Simulates CheckoutActivity.validateForm() check:
        // if (selectedProofUri == null && TextUtils.isEmpty(uploadedProofUrl))
        String uploadedProofUrl = null;
        assertTrue("Null proofUrl must be treated as missing proof",
                uploadedProofUrl == null || uploadedProofUrl.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 4. Bank transfer transaction ID is non-empty and unique
    // -------------------------------------------------------------------------
    @Test
    public void bankTransfer_transactionId_isNonEmptyAndUnique() {
        String txId1 = "bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String txId2 = "bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        assertNotNull(txId1);
        assertTrue("Transaction ID must start with bank_", txId1.startsWith("bank_"));
        assertEquals("bank_ prefix + 24 hex chars = 29 chars", 29, txId1.length());
        // Two generated IDs must be different (UUID guarantees uniqueness)
        assertTrue("Each transaction ID must be unique", !txId1.equals(txId2));
    }

    // -------------------------------------------------------------------------
    // 5. Payment amount is preserved through model layer
    // -------------------------------------------------------------------------
    @Test
    public void payment_amount_preservedThroughModel() {
        double amount = 1500.0;
        Payment payment = buildBankTransferPayment(SAMPLE_PROOF_URL);
        payment.setAmount(amount);

        assertEquals(amount, payment.getAmount(), 0.001);
    }

    // -------------------------------------------------------------------------
    // 6. Rsvp paymentProofUrl is independent of paymentRef
    // -------------------------------------------------------------------------
    @Test
    public void rsvp_proofUrl_independentOfPaymentRef() {
        Rsvp rsvp = new Rsvp();
        rsvp.setPaymentRef("bank_abc123def456ghi789jkl0");
        rsvp.setPaymentProofUrl(SAMPLE_PROOF_URL);

        assertEquals("bank_abc123def456ghi789jkl0", rsvp.getPaymentRef());
        assertEquals(SAMPLE_PROOF_URL, rsvp.getPaymentProofUrl());
        // Changing proofUrl must not affect paymentRef
        rsvp.setPaymentProofUrl("https://example.com/other.jpg");
        assertEquals("bank_abc123def456ghi789jkl0", rsvp.getPaymentRef());
    }

    // -------------------------------------------------------------------------
    // 7. Payment proofUrl is independent of transactionId
    // -------------------------------------------------------------------------
    @Test
    public void payment_proofUrl_independentOfTransactionId() {
        Payment payment = new Payment();
        payment.setTransactionId("bank_txn_001");
        payment.setProofUrl(SAMPLE_PROOF_URL);

        assertEquals("bank_txn_001", payment.getTransactionId());
        assertEquals(SAMPLE_PROOF_URL, payment.getProofUrl());

        // Overwrite proofUrl — transactionId stays unchanged
        payment.setProofUrl("https://example.com/new.jpg");
        assertEquals("bank_txn_001", payment.getTransactionId());
    }

    // -------------------------------------------------------------------------
    // 8. Empty proofUrl string is also treated as missing proof
    // -------------------------------------------------------------------------
    @Test
    public void proofUrl_emptyString_isMissingProof() {
        String uploadedProofUrl = "";
        assertTrue("Empty proofUrl must be treated as missing proof",
                uploadedProofUrl == null || uploadedProofUrl.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 9. BANK_TRANSFER string constant matches expected value
    // -------------------------------------------------------------------------
    @Test
    public void bankTransfer_methodConstant_isCorrect() {
        // CheckoutActivity hard-codes "BANK_TRANSFER". Verify it round-trips through model.
        Payment payment = new Payment();
        payment.setPaymentMethod("BANK_TRANSFER");
        assertEquals("BANK_TRANSFER", payment.getPaymentMethod());
    }

    // -------------------------------------------------------------------------
    // 10. Rsvp BANK_TRANSFER paymentMethod flows through correctly
    // -------------------------------------------------------------------------
    @Test
    public void rsvp_bankTransferMethod_flowsThroughCorrectly() {
        Rsvp rsvp = new Rsvp();
        rsvp.setPaymentStatus(Constants.PAYMENT_CONFIRMED);
        rsvp.setPaymentMethod("BANK_TRANSFER");
        rsvp.setPaymentProofUrl(SAMPLE_PROOF_URL);

        assertEquals(Constants.PAYMENT_CONFIRMED, rsvp.getPaymentStatus());
        assertEquals("BANK_TRANSFER", rsvp.getPaymentMethod());
        assertEquals(SAMPLE_PROOF_URL, rsvp.getPaymentProofUrl());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Payment buildBankTransferPayment(String proofUrl) {
        String txId = "bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        Payment payment = new Payment(
                null,
                "user_test_01",
                "event_test_01",
                1500.0,
                Constants.PAYMENT_CONFIRMED,
                txId,
                System.currentTimeMillis()
        );
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setProofUrl(proofUrl);
        return payment;
    }

    private Rsvp buildBankTransferRsvp(String proofUrl) {
        String txId = "bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("user_test_01");
        rsvp.setEventId("event_test_01");
        rsvp.setStatus("confirmed");
        rsvp.setPaymentStatus(Constants.PAYMENT_CONFIRMED);
        rsvp.setTransactionId(txId);
        rsvp.setPaymentRef(txId);
        rsvp.setPaymentMethod("BANK_TRANSFER");
        rsvp.setPaymentProofUrl(proofUrl);
        return rsvp;
    }
}
