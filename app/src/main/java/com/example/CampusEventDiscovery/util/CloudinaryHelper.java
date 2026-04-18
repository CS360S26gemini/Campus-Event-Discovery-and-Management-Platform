package com.example.CampusEventDiscovery.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * CloudinaryHelper.java
 *
 * Helper for unsigned Cloudinary uploads.
 */
public final class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";

    private CloudinaryHelper() {
    }

    public interface CloudinaryCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    public static void init(Context context) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", Config.CLOUDINARY_CLOUD_NAME.trim());
            config.put("secure", true);
            MediaManager.init(context, config);
            Log.d(TAG, "Cloudinary initialized");
        } catch (IllegalStateException e) {
            Log.d(TAG, "Cloudinary already initialized");
        }
    }

    public static void uploadImage(Uri imageUri, CloudinaryCallback callback) {
        String preset = Config.CLOUDINARY_UPLOAD_PRESET.trim();

        MediaManager.get().upload(imageUri)
                .unsigned(preset)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if (callback != null) {
                            callback.onSuccess(url);
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (callback != null) {
                            callback.onError(error != null ? error.getDescription() : "Upload failed");
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                })
                .dispatch();
    }
}
