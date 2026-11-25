package com.group5.studentplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Unit tests for the Today dashboard calculations.
 * "Now" is fixed to Wednesday 25 November 2026, 10:00 AM so the results are always the same.
 */
public class DashboardLogicTest {

    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;
    private static final long NOW = at(2026, Calendar.NOVEMBER, 25, 10, 0);

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, hour, minute);
        return cal.getTimeInMillis();
    }

    private static Task task(String title, long due, int priority, boolean completed) {
        Task t = new Task();
        t.title = title;
        t.dueDate = due;
        t.priority = priority;
        t.completed = completed;
        return t;
    }

    private static ClassSession session(String name, int start, int end) {
        ClassSession s = new ClassSession();
        s.courseName = name;
        s.startMinutes = start;
        s.endMinutes = end;
        return s;
    }

    // ------------------------------------------------------------ days and deadlines

    @Test
    public void daysBetween_countsCalendarDays() {
        assertEquals(0, DashboardLogic.daysBetween(NOW, NOW + 13 * HOUR));  // 11 PM today
        assertEquals(1, DashboardLogic.daysBetween(NOW, NOW + 15 * HOUR));  // 1 AM tomorrow
        assertEquals(-1, DashboardLogic.daysBetween(NOW, NOW - 11 * HOUR)); // 11 PM yesterday
    }

    @Test
    public void describeDeadline_usesFriendlyWords() {
        assertTrue(DashboardLogic.describeDeadline(NOW, NOW + 2 * HOUR).startsWith("Due today, "));
        assertTrue(DashboardLogic.describeDeadline(NOW, NOW + DAY).startsWith("Due tomorrow, "));
        assertEquals("Due in 3 days", DashboardLogic.describeDeadline(NOW, NOW + 3 * DAY));
        assertTrue(DashboardLogic.describeDeadline(NOW, NOW - HOUR).startsWith("Overdue since "));
        assertEquals("Overdue by 1 day", DashboardLogic.describeDeadline(NOW, NOW - DAY));
        assertEquals("Overdue by 2 days", DashboardLogic.describeDeadline(NOW, NOW - 2 * DAY));
    }

    // ------------------------------------------------------------ most urgent deadlines

    @Test
    public void mostUrgent_putsOverdueFirstThenSoonest() {
        Task nextWeek = task("next week", NOW + 6 * DAY, Task.PRIORITY_HIGH, false);
        Task tomorrow = task("tomorrow", NOW + DAY, Task.PRIORITY_LOW, false);
        Task overdue = task("overdue", NOW - DAY, Task.PRIORITY_LOW, false);
        List<Task> result = DashboardLogic.mostUrgent(Arrays.asList(nextWeek, tomorrow, overdue), NOW, 5);
        assertEquals(Arrays.asList(overdue, tomorrow, nextWeek), result);
    }

    @Test
    public void mostUrgent_sameDay_highPriorityFirst() {
        Task lowEarly = task("low", NOW + 2 * HOUR, Task.PRIORITY_LOW, false);
        Task highLate = task("high", NOW + 8 * HOUR, Task.PRIORITY_HIGH, false);
        List<Task> result = DashboardLogic.mostUrgent(Arrays.asList(lowEarly, highLate), NOW, 5);
        assertEquals(highLate, result.get(0));
    }

    @Test
    public void mostUrgent_skipsCompletedAndRespectsLimit() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task("done", NOW + HOUR, Task.PRIORITY_HIGH, true));
        for (int i = 1; i <= 7; i++) {
            tasks.add(task("task " + i, NOW + i * DAY, Task.PRIORITY_MEDIUM, false));
        }
        List<Task> result = DashboardLogic.mostUrgent(tasks, NOW, DashboardLogic.URGENT_LIMIT);
        assertEquals(5, result.size());
        assertEquals("task 1", result.get(0).title);
    }

    @Test
    public void countOverdue_ignoresCompletedTasks() {
        List<Task> tasks = Arrays.asList(
                task("late", NOW - HOUR, Task.PRIORITY_LOW, false),
                task("late but done", NOW - HOUR, Task.PRIORITY_LOW, true),
                task("future", NOW + HOUR, Task.PRIORITY_LOW, false));
        assertEquals(1, DashboardLogic.countOverdue(tasks, NOW));
    }

    // ------------------------------------------------------------ next class

    @Test
    public void nextClass_findsCurrentOrNextClass() {
        ClassSession morning = session("morning", 8 * 60, 9 * 60 + 15);
        ClassSession noon = session("noon", 12 * 60, 13 * 60);
        List<ClassSession> today = Arrays.asList(morning, noon);

        assertEquals(morning, DashboardLogic.nextClass(today, 7 * 60));      // before both
        assertEquals(morning, DashboardLogic.nextClass(today, 8 * 60 + 30)); // during the first
        assertEquals(noon, DashboardLogic.nextClass(today, 10 * 60));        // between them
        assertNull(DashboardLogic.nextClass(today, 14 * 60));                // all finished
    }

    @Test
    public void describeNextClass_showsCountdownOrNow() {
        ClassSession s = session("CS 526", 12 * 60, 13 * 60 + 15);
        assertEquals("Starts in 45 min", DashboardLogic.describeNextClass(s, 11 * 60 + 15));
        assertEquals("Starts in 1 h 25 min", DashboardLogic.describeNextClass(s, 10 * 60 + 35));
        assertEquals("Starts in 2 h", DashboardLogic.describeNextClass(s, 10 * 60));
        assertTrue(DashboardLogic.describeNextClass(s, 12 * 60 + 5).startsWith("Now – ends at "));
    }
}
