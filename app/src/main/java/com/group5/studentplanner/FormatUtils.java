package com.group5.studentplanner;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Small helpers for showing dates, times and day names.
 */
public final class FormatUtils {

    private FormatUtils() {
    }

    /** e.g. "Tue, 24 Nov 2026" */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    /** e.g. "10:30 AM" */
    public static String formatTime(long millis) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        return formatDate(millis) + "  " + formatTime(millis);
    }

    /** Formats minutes after midnight, e.g. 510 -> "8:30 AM". */
    public static String formatMinutes(int minutesOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60);
        cal.set(Calendar.MINUTE, minutesOfDay % 60);
        return formatTime(cal.getTimeInMillis());
    }

    /** Day name for a Calendar day constant (SUNDAY = 1). */
    public static String dayName(Context context, int dayOfWeek) {
        return context.getResources().getStringArray(R.array.days)[dayOfWeek - 1];
    }

    public static String priorityName(Context context, int priority) {
        return context.getResources().getStringArray(R.array.priorities)[priority];
    }

    /** Colour resource for a priority: green = Low, orange = Medium, red = High. */
    public static int priorityColor(int priority) {
        if (priority == Task.PRIORITY_HIGH) {
            return R.color.priority_high;
        } else if (priority == Task.PRIORITY_MEDIUM) {
            return R.color.priority_medium;
        }
        return R.color.priority_low;
    }
}
