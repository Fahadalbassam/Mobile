package com.group5.studentplanner;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Creates the notification channel and shows reminder notifications.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "reminders";

    private NotificationHelper() {
    }

    /** Android 8.0+ requires a channel before any notification can be shown. */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.channel_desc));
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Shows a reminder notification. Task reminders get "Done" and "Snooze" buttons;
     * class reminders get a "Snooze" button.
     */
    public static void show(Context context, int notificationId, String title, String text,
                            int type, long id) {
        // On Android 13+ the user must grant the notification permission first.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(context);

        // Tapping the notification opens the app's home screen.
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        if (type == ReminderScheduler.TYPE_TASK) {
            builder.addAction(0, context.getString(R.string.action_done),
                    actionIntent(context, NotificationActionReceiver.ACTION_DONE, notificationId, type, id));
        }
        builder.addAction(0, context.getString(R.string.action_snooze),
                actionIntent(context, NotificationActionReceiver.ACTION_SNOOZE, notificationId, type, id));

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    /** Explicit broadcast to NotificationActionReceiver when a notification button is tapped. */
    private static PendingIntent actionIntent(Context context, String action, int notificationId,
                                              int type, long id) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(ReminderScheduler.EXTRA_TYPE, type);
        intent.putExtra(ReminderScheduler.EXTRA_ID, id);
        intent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        // Different request codes so the Done and Snooze buttons of every notification stay separate.
        int requestCode = notificationId * 2 + (NotificationActionReceiver.ACTION_DONE.equals(action) ? 0 : 1);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
