package com.group5.studentplanner;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Background service that reads every task and class from the database and
 * (re)schedules its reminder alarm. Android clears all alarms when the phone restarts,
 * so this service is started by BootReceiver after a reboot and by MainActivity on launch.
 */
public class ReminderService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Database work is done on a background thread, not the main (UI) thread.
        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            for (Task task : db.getAllTasks(false)) {
                ReminderScheduler.scheduleTask(this, task);
            }
            for (ClassSession session : db.getAllClasses()) {
                ReminderScheduler.scheduleClass(this, session);
            }
            stopSelf(startId);
        }).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // this is a started service, not a bound service
    }
}
