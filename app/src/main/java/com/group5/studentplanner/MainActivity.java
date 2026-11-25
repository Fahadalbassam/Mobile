package com.group5.studentplanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Home screen: the Today dashboard.
 * Shows a short summary (classes today, pending tasks, overdue tasks), the next class,
 * all of today's classes and the most urgent deadlines in one place.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 1;

    private TextView textToday;
    private TextView textCountClasses;
    private TextView textCountPending;
    private TextView textCountOverdue;
    private View boxNextClass;
    private TextView textNextClass;
    private TextView textNextClassDetails;
    private TextView textNextClassStatus;
    private TextView textNoClasses;
    private TextView textNoTasks;
    private LinearLayout listClasses;
    private LinearLayout listTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InsetsHelper.apply(this);

        textToday = findViewById(R.id.textToday);
        textCountClasses = findViewById(R.id.textCountClasses);
        textCountPending = findViewById(R.id.textCountPending);
        textCountOverdue = findViewById(R.id.textCountOverdue);
        boxNextClass = findViewById(R.id.boxNextClass);
        textNextClass = findViewById(R.id.textNextClass);
        textNextClassDetails = findViewById(R.id.textNextClassDetails);
        textNextClassStatus = findViewById(R.id.textNextClassStatus);
        textNoClasses = findViewById(R.id.textNoClasses);
        textNoTasks = findViewById(R.id.textNoTasks);
        listClasses = findViewById(R.id.listClasses);
        listTasks = findViewById(R.id.listTasks);

        // Explicit intents to open the other screens
        findViewById(R.id.buttonTasks).setOnClickListener(
                v -> startActivity(new Intent(this, TaskListActivity.class)));
        findViewById(R.id.buttonSchedule).setOnClickListener(
                v -> startActivity(new Intent(this, ScheduleActivity.class)));

        NotificationHelper.createChannel(this);
        askNotificationPermission();

        // Make sure every saved reminder is scheduled.
        startService(new Intent(this, ReminderService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showDashboard(); // refresh every time the user comes back to this screen
        TodayWidgetProvider.refresh(this); // keep the home-screen widget's countdown up to date
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void showDashboard() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        long now = System.currentTimeMillis();
        int nowMinutes = DashboardLogic.minutesOfDay(now);
        textToday.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(new Date(now)));

        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        List<ClassSession> classes = db.getClassesForDay(today);
        List<Task> pending = db.getAllTasks(false);

        // 1. Summary numbers
        textCountClasses.setText(String.valueOf(classes.size()));
        textCountPending.setText(String.valueOf(pending.size()));
        int overdue = DashboardLogic.countOverdue(pending, now);
        textCountOverdue.setText(String.valueOf(overdue));
        textCountOverdue.setTextColor(ContextCompat.getColor(this,
                overdue > 0 ? R.color.priority_high : R.color.primary));

        // 2. Next class (the one happening now, or the next one today)
        ClassSession next = DashboardLogic.nextClass(classes, nowMinutes);
        if (next == null) {
            textNextClass.setText(R.string.no_more_classes);
            textNextClassDetails.setText(classes.isEmpty()
                    ? getString(R.string.no_classes_today) : getString(R.string.no_more_classes_details));
            textNextClassStatus.setVisibility(View.GONE);
            boxNextClass.setOnClickListener(null);
            boxNextClass.setClickable(false);
        } else {
            textNextClass.setText(next.courseName);
            textNextClassDetails.setText(classTimeAndPlace(next));
            textNextClassStatus.setText(DashboardLogic.describeNextClass(next, nowMinutes));
            textNextClassStatus.setVisibility(View.VISIBLE);
            boxNextClass.setOnClickListener(v -> openClass(next.id));
        }

        // 3. Today's classes (finished classes are greyed out)
        listClasses.removeAllViews();
        textNoClasses.setVisibility(classes.isEmpty() ? View.VISIBLE : View.GONE);
        for (ClassSession s : classes) {
            boolean finished = nowMinutes >= s.endMinutes;
            String subtitle = s.location == null || s.location.isEmpty() ? "" : s.location;
            if (finished) {
                subtitle = getString(R.string.class_finished) + (subtitle.isEmpty() ? "" : " · " + subtitle);
            }
            View row = addRow(listClasses, s.courseName, subtitle,
                    finished ? R.color.finished : R.color.primary);
            TextView left = row.findViewById(R.id.textLeft);
            left.setVisibility(View.VISIBLE);
            left.setText(FormatUtils.formatMinutes(s.startMinutes) + "\n" + FormatUtils.formatMinutes(s.endMinutes));
            if (finished) {
                ((TextView) row.findViewById(R.id.textRowTitle))
                        .setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            row.setOnClickListener(v -> openClass(s.id));
        }

        // 4. Most urgent deadlines (overdue first, then soonest, High priority first on the same day)
        listTasks.removeAllViews();
        List<Task> urgent = DashboardLogic.mostUrgent(pending, now, DashboardLogic.URGENT_LIMIT);
        textNoTasks.setVisibility(urgent.isEmpty() ? View.VISIBLE : View.GONE);
        for (Task t : urgent) {
            String subtitle = DashboardLogic.describeDeadline(now, t.dueDate)
                    + " · " + FormatUtils.priorityName(this, t.priority);
            View row = addRow(listTasks, t.title, subtitle, FormatUtils.priorityColor(t.priority));
            if (t.dueDate < now) {
                ((TextView) row.findViewById(R.id.textRowSubtitle))
                        .setTextColor(ContextCompat.getColor(this, R.color.priority_high));
            }
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, TaskEditActivity.class);
                intent.putExtra(TaskEditActivity.EXTRA_TASK_ID, t.id);
                startActivity(intent);
            });
        }
    }

    /** Adds one row (title, subtitle and a coloured bar) to the given list. */
    private View addRow(LinearLayout list, String title, String subtitle, int colorRes) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_dashboard_row, list, false);
        ((TextView) row.findViewById(R.id.textRowTitle)).setText(title);
        TextView sub = row.findViewById(R.id.textRowSubtitle);
        sub.setText(subtitle);
        sub.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        row.findViewById(R.id.colorBar).setBackgroundColor(ContextCompat.getColor(this, colorRes));
        list.addView(row);
        return row;
    }

    private String classTimeAndPlace(ClassSession s) {
        String text = FormatUtils.formatMinutes(s.startMinutes) + " - " + FormatUtils.formatMinutes(s.endMinutes);
        if (s.location != null && !s.location.isEmpty()) {
            text += " · " + s.location;
        }
        return text;
    }

    private void openClass(long classId) {
        Intent intent = new Intent(this, ClassEditActivity.class);
        intent.putExtra(ClassEditActivity.EXTRA_CLASS_ID, classId);
        startActivity(intent);
    }
}
