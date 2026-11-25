package com.group5.studentplanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * Schedules and cancels reminder alarms with AlarmManager.
 * When an alarm goes off, Android sends a broadcast Intent to ReminderReceiver.
 */
public final class ReminderScheduler {

    public static final String EXTRA_TYPE = "reminder_type";
    public static final String EXTRA_ID = "reminder_id";
    public static final int TYPE_TASK = 0;
    public static final int TYPE_CLASS = 1;

    /** How long the "Snooze" button on a notification waits. */
    public static final int SNOOZE_MINUTES = 10;

    private static final long MINUTE = 60_000L;
    private static final long WEEK = 7L * 24 * 60 * MINUTE;
    /** Added to the request code of snoozed alarms so they never replace the normal reminder. */
    private static final int SNOOZE_OFFSET = 1_000_000;

    private ReminderScheduler() {
    }

    // ---------------------------------------------------------------- Tasks

    public static void scheduleTask(Context context, Task task) {
        cancelTask(context, task.id);
        if (task.completed || task.reminderMinutes == Task.NO_REMINDER) {
            return;
        }
        long when = task.dueDate - task.reminderMinutes * MINUTE;
        if (when <= System.currentTimeMillis()) {
            return; // reminder time already passed
        }
        setAlarm(context, when, pendingIntent(context, TYPE_TASK, task.id, false));
    }

    public static void cancelTask(Context context, long taskId) {
        cancel(context, pendingIntent(context, TYPE_TASK, taskId, false));
        cancel(context, pendingIntent(context, TYPE_TASK, taskId, true));
    }

    // -------------------------------------------------------------- Classes

    public static void scheduleClass(Context context, ClassSession session) {
        cancelClass(context, session.id);
        if (session.reminderMinutes == Task.NO_REMINDER) {
            return;
        }
        long when = nextClassReminderTime(session, System.currentTimeMillis());
        setAlarm(context, when, pendingIntent(context, TYPE_CLASS, session.id, false));
    }

    public static void cancelClass(Context context, long classId) {
        cancel(context, pendingIntent(context, TYPE_CLASS, classId, false));
        cancel(context, pendingIntent(context, TYPE_CLASS, classId, true));
    }

    // -------------------------------------------------------------- Snooze

    /** Shows the same reminder again in SNOOZE_MINUTES minutes (used by the notification button). */
    public static void snooze(Context context, int type, long id) {
        long when = System.currentTimeMillis() + SNOOZE_MINUTES * MINUTE;
        setAlarm(context, when, pendingIntent(context, type, id, true));
    }

    /**
     * Finds the next time (after "now") that the reminder for a weekly class should fire.
     */
    static long nextClassReminderTime(ClassSession session, long now) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        int daysAhead = (session.dayOfWeek - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        cal.add(Calendar.DAY_OF_YEAR, daysAhead);
        cal.set(Calendar.HOUR_OF_DAY, session.startMinutes / 60);
        cal.set(Calendar.MINUTE, session.startMinutes % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long when = cal.getTimeInMillis() - session.reminderMinutes * MINUTE;
        while (when <= now) {
            when += WEEK;
        }
        return when;
    }

    // -------------------------------------------------------------- Helpers

    private static PendingIntent pendingIntent(Context context, int type, long id, boolean snoozed) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_ID, id);
        // A different request code for every task/class so their alarms do not replace each other.
        // A snoozed reminder gets its own code, so snoozing a class keeps next week's reminder.
        int requestCode = (int) (id * 2 + type) + (snoozed ? SNOOZE_OFFSET : 0);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(Context context, long when, PendingIntent pi) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // The user has not allowed exact alarms: use an inexact alarm instead
            // (it may arrive a few minutes late).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    private static void cancel(Context context, PendingIntent pi) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
        }
    }
}
