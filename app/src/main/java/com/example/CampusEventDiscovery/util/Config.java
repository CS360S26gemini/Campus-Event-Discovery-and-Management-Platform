package com.example.CampusEventDiscovery.util;

/**
 * Config.java
 *
 * App-wide third-party config values. Keep test keys only —
 * never commit live keys.
 */
public final class Config {

    private Config() {}

    // Stripe test-mode publishable key (safe to commit — test mode only)
    public static final String STRIPE_PUBLISHABLE_KEY_TEST =
            "pk_test_TYooMQauvdEDq54NiTphI7jx";

    // Stripe test currency for PKR events → charged as USD in test mode
    public static final String STRIPE_CURRENCY = "usd";
}
