package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.example.CampusEventDiscovery.util.Config;

import org.junit.Test;

/**
 * ConfigTest.java
 *
 * Sanity checks for third-party config constants used by the payment and
 * Cloudinary upload flows.
 */
public class ConfigTest {

    @Test
    public void stripePublishableKey_isPresent() {
        assertNotNull(Config.STRIPE_PUBLISHABLE_KEY_TEST);
        assertFalse(Config.STRIPE_PUBLISHABLE_KEY_TEST.trim().isEmpty());
    }

    @Test
    public void cloudinaryConfig_isPresent() {
        assertNotNull(Config.CLOUDINARY_CLOUD_NAME);
        assertFalse(Config.CLOUDINARY_CLOUD_NAME.trim().isEmpty());
        assertNotNull(Config.CLOUDINARY_UPLOAD_PRESET);
        assertFalse(Config.CLOUDINARY_UPLOAD_PRESET.trim().isEmpty());
    }
}
