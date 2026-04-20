package com.example.CampusEventDiscovery.util;

import com.example.CampusEventDiscovery.model.Payment;
import java.util.UUID;

/**
 * MockPaymentService.java
 *
 * A demo service that simulates a payment process.
 */
public class MockPaymentService {

    /**
     * Simulates a payment processing and returns a successful Payment object.
     *
     * @param userId  The ID of the user making the payment.
     * @param eventId The ID of the event being paid for.
     * @param amount  The amount to pay.
     * @return A Payment object with status "SUCCESS".
     */
    public static Payment processPayment(String userId, String eventId, double amount) {
        String transactionId = UUID.randomUUID().toString();
        return new Payment(
                null,
                userId,
                eventId,
                amount,
                "SUCCESS",
                transactionId,
                System.currentTimeMillis()
        );
    }
}
