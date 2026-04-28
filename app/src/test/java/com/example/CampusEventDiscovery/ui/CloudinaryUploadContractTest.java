package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CloudinaryUploadContractTest {

    @Test
    public void eventCreationMemoryAlbumsAndFeedback_useCloudinaryHelperForImageUploads() throws Exception {
        String createEvent = readJava("ui/organizer/CreateEventActivity.java");
        String memories = readJava("ui/profile/MemoriesActivity.java");
        String feedback = readJava("ui/event/EventFeedbackActivity.java");

        assertTrue(createEvent.contains("CloudinaryHelper.uploadImage"));
        assertTrue(memories.contains("CloudinaryHelper.uploadImage"));
        assertTrue(feedback.contains("CloudinaryHelper.uploadImage"));
    }

    @Test
    public void memoriesAndFeedback_doNotUploadPhotosThroughFirebaseStorage() throws Exception {
        String memories = readJava("ui/profile/MemoriesActivity.java");
        String feedback = readJava("ui/event/EventFeedbackActivity.java");

        assertFalse(memories.contains("FirebaseStorage"));
        assertFalse(memories.contains(".putFile("));
        assertFalse(memories.contains("getDownloadUrl()"));

        assertFalse(feedback.contains("FirebaseStorage"));
        assertFalse(feedback.contains(".putFile("));
        assertFalse(feedback.contains("getDownloadUrl()"));
    }

    @Test
    public void remainingProfilePictureFirebaseUpload_isExplicitlyOutsideMemoryFlow() throws Exception {
        String profile = readJava("ui/profile/ProfileFragment.java");

        assertTrue(profile.contains("uploadProfilePicture(Uri uri)"));
        assertTrue(profile.contains("FirebaseStorage.getInstance()"));
    }

    private static String readJava(String name) throws Exception {
        Path fromRoot = Paths.get("app", "src", "main", "java", "com", "example", "CampusEventDiscovery", name);
        if (Files.exists(fromRoot)) {
            return new String(Files.readAllBytes(fromRoot), StandardCharsets.UTF_8);
        }
        Path fromModule = Paths.get("src", "main", "java", "com", "example", "CampusEventDiscovery", name);
        return new String(Files.readAllBytes(fromModule), StandardCharsets.UTF_8);
    }
}
