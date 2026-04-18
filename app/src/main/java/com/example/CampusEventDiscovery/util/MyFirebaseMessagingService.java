package com.example.CampusEventDiscovery.util;

import android.app.PendingIntent;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.CampusEventDiscovery.CampusEventDiscoveryApp;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.ui.sos.SOSAlertActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";
    private static final String TYPE_SOS_ALERT = "SOS_ALERT";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().isEmpty()) {
            return;
        }

        String type = remoteMessage.getData().get("type");
        if (TYPE_SOS_ALERT.equals(type)) {
            String reporter = remoteMessage.getData().get("reporter");
            String description = remoteMessage.getData().get("description");
            String eventName = remoteMessage.getData().get("eventName");
            String eventId = remoteMessage.getData().get("eventId");
            String mapsUrl = remoteMessage.getData().get("mapsUrl");
            showSosNotification(reporter, description, eventName, eventId, mapsUrl);
        }
    }

    private void showSosNotification(String reporter,
                                     String description,
                                     String eventName,
                                     String eventId,
                                     String mapsUrl) {
        Intent fullScreenIntent = new Intent(this, SOSAlertActivity.class);
        fullScreenIntent.putExtra("reporter", reporter);
        fullScreenIntent.putExtra("description", description);
        fullScreenIntent.putExtra("eventName", eventName);
        fullScreenIntent.putExtra("eventId", eventId);
        fullScreenIntent.putExtra("mapsUrl", mapsUrl);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        int reqCode = (int) System.currentTimeMillis();

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                reqCode,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = TextUtils.isEmpty(reporter)
                ? "An attendee has triggered an SOS alert."
                : "SOS from " + reporter
                        + (TextUtils.isEmpty(eventName) ? "" : " at " + eventName);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CampusEventDiscoveryApp.SOS_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_sos)
                        .setContentTitle("EMERGENCY ALERT")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(TextUtils.isEmpty(description) ? contentText : description))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(fullScreenPendingIntent)
                        .setFullScreenIntent(fullScreenPendingIntent, true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(reqCode, notificationBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        persistFcmToken(token);
    }

    private void persistFcmToken(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No signed-in user; skipping FCM token persistence.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(unused -> Log.d(TAG, "FCM token updated for " + currentUser.getUid()))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update FCM token", e));
    }
}
