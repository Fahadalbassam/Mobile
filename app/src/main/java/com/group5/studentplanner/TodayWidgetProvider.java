package com.group5.studentplanner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;

/**
 * Home-screen widget: a small version of the Today dashboard.
 * Shows the next class, the most urgent task and how many tasks are pending / overdue.
 * Tapping the widget opens the app.
 */
public class TodayWidgetProvider extends AppWidgetProvider {

    /** Called by Android when the widget is added and then every 30 minutes (see today_widget_info.xml). */
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        RemoteViews views = buildViews(context);
        for (int id : widgetIds) {
            manager.updateAppWidget(id, views);
        }
    }

    /** Refreshes every Student Planner widget on the home screen. Called whenever the data changes. */
    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        if (ids.length > 0) {
            manager.updateAppWidget(ids, buildViews(context));
        }
    }

    private static RemoteViews buildViews(Context context) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        long now = System.currentTimeMillis();
        int nowMinutes = DashboardLogic.minutesOfDay(now);
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);

        // Next class
        ClassSession next = DashboardLogic.nextClass(db.getClassesForDay(today), nowMinutes);
        if (next == null) {
            views.setTextViewText(R.id.widgetNextClass, context.getString(R.string.no_more_classes));
        } else {
            views.setTextViewText(R.id.widgetNextClass, next.courseName + " · "
                    + DashboardLogic.describeNextClass(next, nowMinutes));
        }

        // Most urgent task
        List<Task> pending = db.getAllTasks(false);
        List<Task> urgent = DashboardLogic.mostUrgent(pending, now, 1);
        if (urgent.isEmpty()) {
            views.setTextViewText(R.id.widgetUrgentTask, context.getString(R.string.no_pending_tasks));
        } else {
            Task t = urgent.get(0);
            views.setTextViewText(R.id.widgetUrgentTask, t.title + " · "
                    + DashboardLogic.describeDeadline(now, t.dueDate));
        }

        // Counts
        views.setTextViewText(R.id.widgetCounts, context.getString(R.string.widget_counts,
                pending.size(), DashboardLogic.countOverdue(pending, now)));

        // Tapping the widget opens the Today dashboard (explicit intent wrapped in a PendingIntent).
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pi);
        return views;
    }
}
