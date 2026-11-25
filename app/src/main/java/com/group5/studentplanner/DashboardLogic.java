package com.group5.studentplanner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * The calculations behind the Today dashboard: which class is next, how urgent each task is,
 * and how the deadline is described ("Due tomorrow", "Overdue by 2 days", ...).
 * It uses plain Java only, so it can be checked without a phone.
 */
public final class DashboardLogic {

    /** How many tasks the "Most urgent deadlines" list shows. */
    public static final int URGENT_LIMIT = 5;

    private static final long DAY = 24L * 60 * 60 * 1000;

    private DashboardLogic() {
    }

    /** Calendar days from "now" to "time": 0 = today, 1 = tomorrow, -1 = yesterday. */
    public static int daysBetween(long now, long time) {
        return (int) Math.round((startOfDay(time) - startOfDay(now)) / (double) DAY);
    }

    private static long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Unfinished tasks ordered by urgency: overdue tasks first, then by the day they are due.
     * Tasks due on the same day are ordered by priority (High first), then by time.
     */
    public static List<Task> mostUrgent(List<Task> tasks, long now, int limit) {
        List<Task> pending = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.completed) {
                pending.add(t);
            }
        }
        Collections.sort(pending, (a, b) -> {
            boolean aLate = a.dueDate < now;
            boolean bLate = b.dueDate < now;
            if (aLate != bLate) {
                return aLate ? -1 : 1;
            }
            int byDay = Integer.compare(daysBetween(now, a.dueDate), daysBetween(now, b.dueDate));
            if (byDay != 0) {
                return byDay;
            }
            int byPriority = Integer.compare(b.priority, a.priority);
            if (byPriority != 0) {
                return byPriority;
            }
            return Long.compare(a.dueDate, b.dueDate);
        });
        return pending.size() > limit ? new ArrayList<>(pending.subList(0, limit)) : pending;
    }

    /** Number of unfinished tasks whose deadline has passed. */
    public static int countOverdue(List<Task> tasks, long now) {
        int count = 0;
        for (Task t : tasks) {
            if (!t.completed && t.dueDate < now) {
                count++;
            }
        }
        return count;
    }

    /** Short description of a deadline, e.g. "Due today, 11:59 PM" or "Overdue by 2 days". */
    public static String describeDeadline(long now, long due) {
        int days = daysBetween(now, due);
        String time = FormatUtils.formatTime(due);
        if (due < now) {
            if (days == 0) {
                return "Overdue since " + time;
            }
            int late = -days;
            return "Overdue by " + late + (late == 1 ? " day" : " days");
        }
        if (days == 0) {
            return "Due today, " + time;
        }
        if (days == 1) {
            return "Due tomorrow, " + time;
        }
        if (days < 7) {
            return "Due in " + days + " days";
        }
        return "Due " + FormatUtils.formatDate(due);
    }

    /**
     * The class that is happening now or starts next today, or null if today's classes are over.
     * The list must be today's classes ordered by start time.
     */
    public static ClassSession nextClass(List<ClassSession> todaysClasses, int nowMinutes) {
        for (ClassSession s : todaysClasses) {
            if (nowMinutes < s.endMinutes) {
                return s;
            }
        }
        return null;
    }

    /** "Now – ends at 8:30 PM" or "Starts in 1 h 25 min". */
    public static String describeNextClass(ClassSession s, int nowMinutes) {
        if (nowMinutes >= s.startMinutes) {
            return "Now – ends at " + FormatUtils.formatMinutes(s.endMinutes);
        }
        int wait = s.startMinutes - nowMinutes;
        if (wait < 60) {
            return "Starts in " + wait + " min";
        }
        int minutes = wait % 60;
        return "Starts in " + (wait / 60) + " h" + (minutes == 0 ? "" : " " + minutes + " min");
    }

    /** Minutes after midnight for the given time. */
    public static int minutesOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }
}
