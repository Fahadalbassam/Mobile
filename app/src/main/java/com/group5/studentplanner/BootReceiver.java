package com.group5.studentplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives the system BOOT_COMPLETED broadcast and starts ReminderService
 * so that all reminders are scheduled again after the phone restarts.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startService(new Intent(context, ReminderService.class));
        }
    }
}
