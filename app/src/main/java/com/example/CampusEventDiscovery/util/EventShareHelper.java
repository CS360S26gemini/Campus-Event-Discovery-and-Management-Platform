package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Builds shareable event links and keeps share/copy behavior consistent.
 */
public final class EventShareHelper {

    private static final String EVENT_SCHEME = "campusevent";
    private static final String EVENT_HOST = "events";

    private EventShareHelper() {
    }

    public static Uri buildEventUri(String eventId) {
        return new Uri.Builder()
                .scheme(EVENT_SCHEME)
                .authority(EVENT_HOST)
                .appendPath(eventId)
                .build();
    }

    public static String eventIdFromUri(Uri uri) {
        if (uri == null
                || !EVENT_SCHEME.equalsIgnoreCase(uri.getScheme())
                || !EVENT_HOST.equalsIgnoreCase(uri.getHost())
                || uri.getPathSegments().isEmpty()) {
            return "";
        }
        return uri.getPathSegments().get(0);
    }

    public static void showEventShareOptions(Activity activity, Event event) {
        if (activity == null || event == null || TextUtils.isEmpty(event.getEventId())) {
            return;
        }

        String link = buildEventUri(event.getEventId()).toString();
        String details = buildShareDetails(activity, event, link);
        String[] options = {
                activity.getString(R.string.copy_event_link),
                activity.getString(R.string.share_event_link),
                activity.getString(R.string.share_event_details)
        };

        new AlertDialog.Builder(activity)
                .setTitle(R.string.share_event_title)
                .setMessage(activity.getString(R.string.share_event_message, link))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        copyLink(activity, link);
                    } else if (which == 1) {
                        shareText(activity, link);
                    } else {
                        shareText(activity, details);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void copyLink(Activity activity, String link) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.event_link_label), link));
            Toast.makeText(activity, R.string.event_link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareText(Activity activity, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.share)));
    }

    private static String buildShareDetails(Activity activity, Event event, String link) {
        return safeText(event.getTitle(), activity.getString(R.string.app_name))
                + "\n"
                + formatDateTime(activity, event.getDate())
                + "\n"
                + safeText(event.getLocation(), activity.getString(R.string.placeholder_venue))
                + "\n"
                + link;
    }

    private static String formatDateTime(Activity activity, Timestamp timestamp) {
        if (timestamp == null) {
            return activity.getString(R.string.placeholder_date);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM - hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private static String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }
}
