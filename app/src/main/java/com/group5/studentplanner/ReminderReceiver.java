package com.group5.studentplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Called by AlarmManager when a reminder is due. Reads the task or class from SQLite
 * and shows a notification.
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(ReminderScheduler.EXTRA_TYPE, -1);
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1);
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        int notificationId = (int) (id * 2 + type);

        if (type == ReminderScheduler.TYPE_TASK) {
            Task task = db.getTask(id);
            if (task == null || task.completed) {
                return; // deleted or already done
            }
            NotificationHelper.show(context, notificationId,
                    "Task due: " + task.title,
                    "Deadline: " + FormatUtils.formatDateTime(task.dueDate), type, id);

        } else if (type == ReminderScheduler.TYPE_CLASS) {
            ClassSession session = db.getClassSession(id);
            if (session == null) {
                return; // class was deleted
            }
            String text = "Starts at " + FormatUtils.formatMinutes(session.startMinutes);
            if (session.location != null && !session.location.isEmpty()) {
                text += " in " + session.location;
            }
            NotificationHelper.show(context, notificationId, "Class: " + session.courseName, text, type, id);

            // Classes repeat every week, so schedule next week's reminder.
            ReminderScheduler.scheduleClass(context, session);
        }
    }
}
