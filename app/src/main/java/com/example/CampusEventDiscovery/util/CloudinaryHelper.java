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
 * Helper class for initializing and uploading images to Cloudinary.
 * Strictly configured for the UNSIGNED approach to avoid "Must supply api_key" errors.
 */
public class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";

    public interface CloudinaryCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    public static void init(Context context) {
        try {
            Map<String, Object> config = new HashMap<>();
            // Use the correct Cloud Name from your dashboard (e.g., "Root")
            config.put("cloud_name", Config.CLOUDINARY_CLOUD_NAME.trim());
            config.put("secure", true);
            
            // IMPORTANT: For Unsigned uploads, we initialize MediaManager with 
            // ONLY the cloud_name. This prevents the SDK from defaulting to 
            // Signed mode and asking for an API Key.
            MediaManager.init(context, config);
            Log.d(TAG, "Cloudinary initialized for UNSIGNED mode with cloud: " + Config.CLOUDINARY_CLOUD_NAME);
        } catch (IllegalStateException e) {
            // Already initialized. Changes to Config.java require a Force Stop of the app.
            Log.w(TAG, "Cloudinary already initialized. Restart app to apply config changes.");
        }
    }

    public static void uploadImage(Uri imageUri, CloudinaryCallback callback) {
        String preset = Config.CLOUDINARY_UPLOAD_PRESET.trim();
        Log.d(TAG, "Starting Unsigned Upload. Preset: " + preset);
        
        // We use the 'unsigned' dispatch method to bypass authentication checks
        MediaManager.get().upload(imageUri)
                .unsigned(preset)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload request started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload SUCCESS: " + url);
                        if (callback != null) {
                            callback.onSuccess(url);
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        String errorMsg = "Unsigned Failure (Code " + error.getCode() + "): " + error.getDescription();
                        Log.e(TAG, errorMsg);
                        if (callback != null) {
                            callback.onError(errorMsg);
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                }).dispatch();
    }
}
