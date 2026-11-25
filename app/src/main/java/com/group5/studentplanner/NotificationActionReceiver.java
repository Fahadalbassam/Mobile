package com.group5.studentplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

/**
 * Handles the buttons on a reminder notification:
 * "Done" marks the task as completed, and "Snooze 10 min" shows the reminder again 10 minutes later.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_DONE = "com.group5.studentplanner.ACTION_DONE";
    public static final String ACTION_SNOOZE = "com.group5.studentplanner.ACTION_SNOOZE";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(ReminderScheduler.EXTRA_TYPE, -1);
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

        // Either button closes the notification.
        NotificationManagerCompat.from(context).cancel(notificationId);

        if (ACTION_DONE.equals(intent.getAction()) && type == ReminderScheduler.TYPE_TASK) {
            DatabaseHelper.getInstance(context).setTaskCompleted(id, true);
            ReminderScheduler.cancelTask(context, id);
            Toast.makeText(context, R.string.marked_done, Toast.LENGTH_SHORT).show();
        } else if (ACTION_SNOOZE.equals(intent.getAction())) {
            ReminderScheduler.snooze(context, type, id);
            Toast.makeText(context, R.string.snoozed, Toast.LENGTH_SHORT).show();
        }
    }
}
