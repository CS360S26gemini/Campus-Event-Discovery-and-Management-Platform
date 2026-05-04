package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;

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
        shareEventLink(activity, event);
    }

    public static void shareEventLink(Context context, Event event) {
        if (context == null || event == null || TextUtils.isEmpty(event.getEventId())) {
            return;
        }

        String link = buildEventUri(event.getEventId()).toString();
        shareText(context, link);
    }

    private static void shareText(Context context, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        Intent chooser = Intent.createChooser(shareIntent, context.getString(R.string.share));
        if (!(context instanceof Activity)) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(chooser);
    }
}
