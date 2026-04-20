package com.example.CampusEventDiscovery;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ApplicationInfo;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;

/**
 * Applies persisted UI preferences and initializes system-wide components.
 */
public class CampusEventDiscoveryApp extends Application {

    public static final String SOS_CHANNEL_ID = "SOS_ALERTS_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        initAppCheck();
        createSosNotificationChannel();
        ThemeManager.applyStoredTheme(this);
        ThemeManager.installAccentLifecycle(this);
        CloudinaryHelper.init(this);
    }

    private void initAppCheck() {
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebuggable) {
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
        firebaseAppCheck.setTokenAutoRefreshEnabled(true);
    }

    private void createSosNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SOS Alerts";
            String description = "High priority emergency alerts for campus events";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(SOS_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            Uri defaultSoundUri = Settings.System.DEFAULT_RINGTONE_URI;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            channel.setSound(defaultSoundUri, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
