package com.group5.studentplanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database with two tables: "tasks" and "classes".
 * All create / read / update / delete (CRUD) operations of the app go through this class.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "student_planner.db";
    private static final int DATABASE_VERSION = 1;

    // Table: tasks
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_DUE_DATE = "due_date";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_COMPLETED = "completed";
    public static final String COL_REMINDER = "reminder_minutes";

    // Table: classes
    public static final String TABLE_CLASSES = "classes";
    public static final String COL_COURSE = "course_name";
    public static final String COL_LOCATION = "location";
    public static final String COL_INSTRUCTOR = "instructor";
    public static final String COL_DAY = "day_of_week";
    public static final String COL_START = "start_minutes";
    public static final String COL_END = "end_minutes";

    private static DatabaseHelper instance;

    /** One shared helper for the whole app. */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        appContext = context;
    }

    /** Called after every change, so the home-screen widget always shows the latest data. */
    private void dataChanged() {
        TodayWidgetProvider.refresh(appContext);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TASKS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_DUE_DATE + " INTEGER NOT NULL, "
                + COL_PRIORITY + " INTEGER NOT NULL DEFAULT 1, "
                + COL_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_REMINDER + " INTEGER NOT NULL DEFAULT -1)");

        db.execSQL("CREATE TABLE " + TABLE_CLASSES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_COURSE + " TEXT NOT NULL, "
                + COL_LOCATION + " TEXT, "
                + COL_INSTRUCTOR + " TEXT, "
                + COL_DAY + " INTEGER NOT NULL, "
                + COL_START + " INTEGER NOT NULL, "
                + COL_END + " INTEGER NOT NULL, "
                + COL_REMINDER + " INTEGER NOT NULL DEFAULT -1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first release, so there is nothing to migrate yet.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSES);
        onCreate(db);
    }

    // ---------------------------------------------------------------- Tasks

    private ContentValues taskValues(Task task) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, task.title);
        values.put(COL_DESCRIPTION, task.description);
        values.put(COL_DUE_DATE, task.dueDate);
        values.put(COL_PRIORITY, task.priority);
        values.put(COL_COMPLETED, task.completed ? 1 : 0);
        values.put(COL_REMINDER, task.reminderMinutes);
        return values;
    }

    /** Inserts a new task and returns its new id. */
    public long insertTask(Task task) {
        task.id = getWritableDatabase().insert(TABLE_TASKS, null, taskValues(task));
        dataChanged();
        return task.id;
    }

    public void updateTask(Task task) {
        getWritableDatabase().update(TABLE_TASKS, taskValues(task),
                COL_ID + " = ?", new String[]{String.valueOf(task.id)});
        dataChanged();
    }

    public void setTaskCompleted(long id, boolean completed) {
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETED, completed ? 1 : 0);
        getWritableDatabase().update(TABLE_TASKS, values,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        dataChanged();
    }

    public void deleteTask(long id) {
        getWritableDatabase().delete(TABLE_TASKS, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        dataChanged();
    }

    public Task getTask(long id) {
        List<Task> list = queryTasks(COL_ID + " = ?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    /** All tasks ordered by deadline (unfinished first). */
    public List<Task> getAllTasks(boolean includeCompleted) {
        String where = includeCompleted ? null : COL_COMPLETED + " = 0";
        return queryTasks(where, null);
    }

    private List<Task> queryTasks(String where, String[] args) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_TASKS, null, where, args,
                null, null, COL_COMPLETED + " ASC, " + COL_DUE_DATE + " ASC");
        try {
            while (c.moveToNext()) {
                Task t = new Task();
                t.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                t.title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                t.description = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION));
                t.dueDate = c.getLong(c.getColumnIndexOrThrow(COL_DUE_DATE));
                t.priority = c.getInt(c.getColumnIndexOrThrow(COL_PRIORITY));
                t.completed = c.getInt(c.getColumnIndexOrThrow(COL_COMPLETED)) == 1;
                t.reminderMinutes = c.getInt(c.getColumnIndexOrThrow(COL_REMINDER));
                list.add(t);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // -------------------------------------------------------------- Classes

    private ContentValues classValues(ClassSession s) {
        ContentValues values = new ContentValues();
        values.put(COL_COURSE, s.courseName);
        values.put(COL_LOCATION, s.location);
        values.put(COL_INSTRUCTOR, s.instructor);
        values.put(COL_DAY, s.dayOfWeek);
        values.put(COL_START, s.startMinutes);
        values.put(COL_END, s.endMinutes);
        values.put(COL_REMINDER, s.reminderMinutes);
        return values;
    }

    public long insertClass(ClassSession s) {
        s.id = getWritableDatabase().insert(TABLE_CLASSES, null, classValues(s));
        dataChanged();
        return s.id;
    }

    public void updateClass(ClassSession s) {
        getWritableDatabase().update(TABLE_CLASSES, classValues(s),
                COL_ID + " = ?", new String[]{String.valueOf(s.id)});
        dataChanged();
    }

    public void deleteClass(long id) {
        getWritableDatabase().delete(TABLE_CLASSES, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        dataChanged();
    }

    public ClassSession getClassSession(long id) {
        List<ClassSession> list = queryClasses(COL_ID + " = ?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    /** The whole week ordered by day (Sunday first) and start time. */
    public List<ClassSession> getAllClasses() {
        return queryClasses(null, null);
    }

    public List<ClassSession> getClassesForDay(int dayOfWeek) {
        return queryClasses(COL_DAY + " = ?", new String[]{String.valueOf(dayOfWeek)});
    }

    private List<ClassSession> queryClasses(String where, String[] args) {
        List<ClassSession> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_CLASSES, null, where, args,
                null, null, COL_DAY + " ASC, " + COL_START + " ASC");
        try {
            while (c.moveToNext()) {
                ClassSession s = new ClassSession();
                s.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                s.courseName = c.getString(c.getColumnIndexOrThrow(COL_COURSE));
                s.location = c.getString(c.getColumnIndexOrThrow(COL_LOCATION));
                s.instructor = c.getString(c.getColumnIndexOrThrow(COL_INSTRUCTOR));
                s.dayOfWeek = c.getInt(c.getColumnIndexOrThrow(COL_DAY));
                s.startMinutes = c.getInt(c.getColumnIndexOrThrow(COL_START));
                s.endMinutes = c.getInt(c.getColumnIndexOrThrow(COL_END));
                s.reminderMinutes = c.getInt(c.getColumnIndexOrThrow(COL_REMINDER));
                list.add(s);
            }
        } finally {
            c.close();
        }
        return list;
    }
}
