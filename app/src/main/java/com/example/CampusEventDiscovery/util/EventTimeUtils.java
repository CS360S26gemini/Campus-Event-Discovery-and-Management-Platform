package com.example.CampusEventDiscovery.util;

import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

/** Shared event time helpers used by event creation, display, and SOS eligibility. */
public final class EventTimeUtils {

    public static final int DEFAULT_END_HOUR_OF_DAY = 22;
    public static final int DEFAULT_END_MINUTE = 0;
    public static final long SOS_GRACE_PERIOD_MS = 60L * 60L * 1000L;

    private EventTimeUtils() {
    }

    public static Timestamp defaultEndTimeForStart(Timestamp startTime) {
        if (startTime == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime.toDate());
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_END_HOUR_OF_DAY);
        calendar.set(Calendar.MINUTE, DEFAULT_END_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (!calendar.getTime().after(startTime.toDate())) {
            calendar.add(Calendar.DATE, 1);
        }

        return new Timestamp(calendar.getTime());
    }

    public static Timestamp resolveEndTime(Timestamp startTime, Timestamp explicitEndTime) {
        return explicitEndTime != null ? explicitEndTime : defaultEndTimeForStart(startTime);
    }

    public static boolean isSosAllowed(boolean checkedIn, Timestamp startTime, Timestamp endTime, long nowMillis) {
        Timestamp resolvedEnd = resolveEndTime(startTime, endTime);
        if (!checkedIn || resolvedEnd == null) {
            return false;
        }
        long cutoffMillis = resolvedEnd.toDate().getTime() + SOS_GRACE_PERIOD_MS;
        return nowMillis <= cutoffMillis;
    }

    public static Date addSosGracePeriod(Date endTime) {
        if (endTime == null) {
            return null;
        }
        return new Date(endTime.getTime() + SOS_GRACE_PERIOD_MS);
    }
}
