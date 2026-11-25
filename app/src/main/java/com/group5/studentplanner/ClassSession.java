package com.group5.studentplanner;

/**
 * A weekly class in the student's timetable (one row of the "classes" table).
 */
public class ClassSession {

    public long id = -1;
    public String courseName = "";
    public String location = "";
    public String instructor = "";
    /** Day of the week using java.util.Calendar constants (SUNDAY = 1 ... SATURDAY = 7). */
    public int dayOfWeek;
    /** Start time as minutes after midnight (e.g. 8:30 AM = 510). */
    public int startMinutes;
    /** End time as minutes after midnight. */
    public int endMinutes;
    /** How many minutes before the class to remind the user, or Task.NO_REMINDER. */
    public int reminderMinutes = Task.NO_REMINDER;
}
