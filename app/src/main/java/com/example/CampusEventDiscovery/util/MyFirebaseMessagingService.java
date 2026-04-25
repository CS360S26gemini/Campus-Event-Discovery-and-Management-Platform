package com.example.CampusEventDiscovery.util;

import android.app.PendingIntent;
import android.content.Context;
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
    private static final String TYPE_EVENT_REMINDER = "EVENT_REMINDER";

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
            return;
        }

        if (TYPE_EVENT_REMINDER.equals(type)) {
            String eventId = remoteMessage.getData().get("eventId");
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            showEventReminderNotification(eventId, title, body);
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

    private void showEventReminderNotification(String eventId, String title, String body) {
        Intent intent = createReminderIntent(this);
        if (!TextUtils.isEmpty(eventId)) {
            intent.putExtra("eventId", eventId);
        }

        int reqCode = TextUtils.isEmpty(eventId) ? (int) System.currentTimeMillis() : eventId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String safeTitle = TextUtils.isEmpty(title) ? "Event reminder" : title;
        String safeBody = TextUtils.isEmpty(body) ? "Open the calendar for details." : body;

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CampusEventDiscoveryApp.REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_calendar)
                        .setContentTitle(safeTitle)
                        .setContentText(safeBody)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(reqCode, notificationBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    static Intent createReminderIntent(Context context) {
        Intent intent = new Intent(context, com.example.CampusEventDiscovery.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.EXTRA_DESTINATION_TAB, Constants.DESTINATION_TAB_CALENDAR);
        return intent;
    }

    public static String buildReminderTitle(String eventName, int daysRemaining, String startTime) {
        String safeEventName = TextUtils.isEmpty(eventName) ? "your event" : eventName;
        String safeStartTime = TextUtils.isEmpty(startTime) ? "scheduled time" : startTime;
        if (daysRemaining <= 0) {
            return safeEventName + " commencing at " + safeStartTime;
        }
        return daysRemaining + " Days left to " + safeEventName;
    }

    public static String buildReminderBody(String eventName, int daysRemaining, String startTime) {
        String safeEventName = TextUtils.isEmpty(eventName) ? "your event" : eventName;
        String safeStartTime = TextUtils.isEmpty(startTime) ? "scheduled time" : startTime;
        if (daysRemaining <= 0) {
            return safeEventName + " commencing at " + safeStartTime;
        }
        return "Reminder: " + daysRemaining + " Days left to " + safeEventName;
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
