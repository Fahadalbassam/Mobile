package com.group5.studentplanner;

/**
 * A study task, assignment or deadline (one row of the "tasks" table).
 */
public class Task {

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    /** Value of reminderMinutes when the user does not want a reminder. */
    public static final int NO_REMINDER = -1;

    public long id = -1;
    public String title = "";
    public String description = "";
    /** Deadline in milliseconds since epoch. */
    public long dueDate;
    public int priority = PRIORITY_MEDIUM;
    public boolean completed = false;
    /** How many minutes before the deadline to remind the user, or NO_REMINDER. */
    public int reminderMinutes = NO_REMINDER;
}
