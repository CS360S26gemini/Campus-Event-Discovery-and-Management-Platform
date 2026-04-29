package com.example.CampusEventDiscovery.util;

/**
 * Implemented by screens that need custom state cleanup before being scrolled
 * back to the top from bottom navigation.
 */
public interface ScrollResettable {
    void resetScrollToTop();
}
