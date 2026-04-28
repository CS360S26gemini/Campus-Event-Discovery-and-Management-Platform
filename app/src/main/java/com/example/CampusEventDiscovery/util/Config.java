package com.example.CampusEventDiscovery.util;

/**
 * Config.java
 *
 * App-wide third-party config values. Keep test keys only.
 */
public final class Config {

    private Config() {}

    // Stripe test-mode publishable key (safe to commit — test mode only)
    public static final String STRIPE_PUBLISHABLE_KEY_TEST =
            "pk_test_TYooMQauvdEDq54NiTphI7jx";

    // Stripe test currency for PKR events → charged as USD in test mode
    public static final String STRIPE_CURRENCY = "usd";

    // Cloudinary unsigned upload config used for event images and payment proofs.
    public static final String CLOUDINARY_CLOUD_NAME = "dcxablsft";
    public static final String CLOUDINARY_UPLOAD_PRESET = "campus_event_discovery";
}
