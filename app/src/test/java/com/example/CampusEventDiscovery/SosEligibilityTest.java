package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.util.EventTimeUtils;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Calendar;

public class SosEligibilityTest {

    @Test
    public void sosAllowed_checkedInBeforeOneHourAfterEnd() {
        long now = System.currentTimeMillis();
        Timestamp start = new Timestamp(new java.util.Date(now - 2L * 60L * 60L * 1000L));
        Timestamp end = new Timestamp(new java.util.Date(now - 30L * 60L * 1000L));

        assertTrue(EventTimeUtils.isSosAllowed(true, start, end, now));
    }

    @Test
    public void sosBlocked_whenNotCheckedIn() {
        long now = System.currentTimeMillis();
        Timestamp start = new Timestamp(new java.util.Date(now - 30L * 60L * 1000L));
        Timestamp end = new Timestamp(new java.util.Date(now + 30L * 60L * 1000L));

        assertFalse(EventTimeUtils.isSosAllowed(false, start, end, now));
    }

    @Test
    public void sosBlocked_afterOneHourGracePeriod() {
        long now = System.currentTimeMillis();
        Timestamp start = new Timestamp(new java.util.Date(now - 4L * 60L * 60L * 1000L));
        Timestamp end = new Timestamp(new java.util.Date(now - 61L * 60L * 1000L));

        assertFalse(EventTimeUtils.isSosAllowed(true, start, end, now));
    }

    @Test
    public void defaultEndTime_isTenPmSameDayWhenAfterStart() {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2026, Calendar.MAY, 10, 18, 30, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        Timestamp end = EventTimeUtils.defaultEndTimeForStart(new Timestamp(startCalendar.getTime()));

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(end.toDate());
        assertTrue(endCalendar.get(Calendar.YEAR) == 2026);
        assertTrue(endCalendar.get(Calendar.MONTH) == Calendar.MAY);
        assertTrue(endCalendar.get(Calendar.DAY_OF_MONTH) == 10);
        assertTrue(endCalendar.get(Calendar.HOUR_OF_DAY) == 22);
        assertTrue(endCalendar.get(Calendar.MINUTE) == 0);
    }

    @Test
    public void defaultEndTime_movesToNextDayWhenStartAfterTenPm() {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2026, Calendar.MAY, 10, 23, 15, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        Timestamp end = EventTimeUtils.defaultEndTimeForStart(new Timestamp(startCalendar.getTime()));

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(end.toDate());
        assertTrue(endCalendar.get(Calendar.YEAR) == 2026);
        assertTrue(endCalendar.get(Calendar.MONTH) == Calendar.MAY);
        assertTrue(endCalendar.get(Calendar.DAY_OF_MONTH) == 11);
        assertTrue(endCalendar.get(Calendar.HOUR_OF_DAY) == 22);
        assertTrue(endCalendar.get(Calendar.MINUTE) == 0);
    }
}
