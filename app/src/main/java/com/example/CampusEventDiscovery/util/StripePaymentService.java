package com.example.CampusEventDiscovery.util;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.util.Constants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * StripePaymentService.java
 *
 * Calls the Supabase Edge Function which creates a real Stripe PaymentIntent.
 * Transactions appear in the Stripe sandbox dashboard.
 */
public class StripePaymentService {

    private static final String SUPABASE_FUNCTION_URL =
            "https://jhvujgiusemenimbzmil.supabase.co/functions/v1/create-payment-intent";

    private static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpodnVqZ2l1c2VtZW5pbWJ6bWlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNTcxNDQsImV4cCI6MjA5MTkzMzE0NH0.1zvB_GmPZBphRK79pyB1PoYen-8g3yThTRWAI9wq29g";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private StripePaymentService() {}

    /**
     * Callback interface for async payment result.
     */
    public interface PaymentCallback {
        void onSuccess(Payment payment);
        void onFailure(Exception e);
    }

    /**
     * Lightweight synchronous helper kept for unit tests and legacy callers.
     * The app flow uses the async overload below.
     */
    public static Payment processPayment(String userId, String eventId, double amount) {
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return new Payment(
                null,
                userId,
                eventId,
                amount,
                Constants.PAYMENT_CONFIRMED,
                "pi_test_" + hex,
                System.currentTimeMillis()
        );
    }

    /**
     * Calls the Supabase Edge Function to create a real Stripe PaymentIntent,
     * then returns a Payment object with the real PaymentIntent ID as transactionId.
     * Runs on a background thread; callbacks are invoked on that same thread —
     * callers must post to main thread if updating UI.
     */
    public static void processPayment(String userId, String eventId, double amount,
                                      PaymentCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("amount", amount);
                body.put("currency", "pkr");
                body.put("userId", userId != null ? userId : "");
                body.put("eventId", eventId != null ? eventId : "");

                RequestBody requestBody = RequestBody.create(body.toString(), JSON);

                Request request = new Request.Builder()
                        .url(SUPABASE_FUNCTION_URL)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null
                            ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        callback.onFailure(new IOException(
                                "Payment server error " + response.code() + ": " + responseBody));
                        return;
                    }

                    JSONObject json = new JSONObject(responseBody);
                    String paymentIntentId = json.getString("paymentIntentId");

                    Payment payment = new Payment(
                            null,
                            userId,
                            eventId,
                            amount,
                            Constants.PAYMENT_CONFIRMED,
                            paymentIntentId,
                            System.currentTimeMillis()
                    );

                    callback.onSuccess(payment);
                }

            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
