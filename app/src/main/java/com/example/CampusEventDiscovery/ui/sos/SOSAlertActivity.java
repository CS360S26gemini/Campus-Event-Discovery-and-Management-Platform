package com.example.CampusEventDiscovery.ui.sos;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;

/**
 * Full-screen, lock-screen-bypassing alert shown to admins/organizers
 * when an attendee triggers an SOS. Plays a loud alarm and vibrates
 * continuously until the user taps Dismiss.
 */
public class SOSAlertActivity extends AppCompatActivity {

    private Ringtone alarmRingtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_sos_alert);

        TextView tvReporter = findViewById(R.id.tvSosReporter);
        TextView tvDescription = findViewById(R.id.tvSosDescription);
        Button btnDismiss = findViewById(R.id.btnDismiss);

        String reporter = getIntent().getStringExtra("reporter");
        String description = getIntent().getStringExtra("description");
        String eventName = getIntent().getStringExtra("eventName");

        if (!TextUtils.isEmpty(reporter)) {
            tvReporter.setText("Reporter: " + reporter);
        }

        StringBuilder body = new StringBuilder();
        if (!TextUtils.isEmpty(eventName)) {
            body.append("Event: ").append(eventName).append("\n");
        }
        if (!TextUtils.isEmpty(description)) {
            body.append(description);
        }
        if (body.length() > 0) {
            tvDescription.setText(body.toString());
        }

        btnDismiss.setOnClickListener(v -> finish());

        startAlarm();
    }

    private void startAlarm() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            alarmRingtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            if (alarmRingtone != null) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                alarmRingtone.setAudioAttributes(attrs);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    alarmRingtone.setLooping(true);
                    alarmRingtone.setVolume(1.0f);
                }
                alarmRingtone.play();
            }
        } catch (Exception ignored) {
        }

        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 500, 1000, 500, 1000};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void stopAlarm() {
        if (alarmRingtone != null) {
            try {
                alarmRingtone.stop();
            } catch (Exception ignored) {
            }
            alarmRingtone = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
            vibrator = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
