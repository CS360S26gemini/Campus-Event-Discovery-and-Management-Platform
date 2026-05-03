package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AccessibilityContractTest {

    @Test
    public void eventDetailLayout_exposesDescriptionsForPrimaryNavigationAndActions() throws Exception {
        String xml = readLayout("activity_event_detail.xml");

        assertTrue(xml.contains("app:navigationContentDescription=\"@string/back\""));
        assertTrue(xml.contains("android:id=\"@+id/btnHeart\""));
        assertTrue(xml.contains("android:contentDescription=\"@string/save\""));
        assertTrue(xml.contains("android:id=\"@+id/btnShare\""));
        assertTrue(xml.contains("android:contentDescription=\"@string/share\""));
    }

    @Test
    public void calendarAndProfileLayouts_exposeAccessibleNavigationLabels() throws Exception {
        String calendar = readLayout("fragment_event_calendar.xml");
        String profile = readLayout("fragment_profile.xml");

        assertTrue(calendar.contains("android:contentDescription=\"@string/previous_month\""));
        assertTrue(calendar.contains("android:contentDescription=\"@string/next_month\""));
        assertTrue(profile.contains("android:id=\"@+id/rowNotifications\""));
        assertTrue(profile.contains("android:contentDescription=\"@string/notifications_row\""));
        assertTrue(profile.contains("android:id=\"@+id/rowAccountSettings\""));
        assertTrue(profile.contains("android:contentDescription=\"@string/account_settings_row\""));
    }

    @Test
    public void scannerAndMemoryLayouts_keepActionSurfacesDiscoverable() throws Exception {
        String scanner = readLayout("activity_scanner.xml");
        String album = readLayout("activity_memory_album.xml");

        assertTrue(scanner.contains("android:id=\"@+id/btnStartScanner\""));
        assertTrue(scanner.contains("android:text=\"@string/open_camera\""));
        assertTrue(album.contains("android:id=\"@+id/btnAddAlbumPhotos\""));
        assertTrue(album.contains("android:id=\"@+id/btnSelectAlbumPhotos\""));
        assertTrue(album.contains("android:text=\"@string/memory_album_empty\""));
    }

    private static String readLayout(String name) throws Exception {
        Path root = Path.of(System.getProperty("user.dir"));
        Path file = root.resolve("src/main/res/layout").resolve(name);
        if (!Files.exists(file)) {
            file = root.resolve("app/src/main/res/layout").resolve(name);
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
