package com.sujay.apps.todolist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class NotificationHelper {

    public static final String CHANNEL_ID = "reminder_channel"; // Unique channel ID
    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel(context);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Channel"; // Channel name
            String description = "Channel for reminders"; // Channel description
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // Channel importance
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification(String title, String taskId) {
        int uniqueNotificationId = taskId.hashCode();

        Intent intent = new Intent(context, MainActivity.class); // Change to the activity you want to open
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, uniqueNotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ... (channel creation)

        Intent completeIntent = new Intent(context, NotificationReceiver.class);
        completeIntent.setAction("ACTION_COMPLETE");
        completeIntent.putExtra("taskId", taskId);
        completeIntent.putExtra("taskTitle", title);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context, uniqueNotificationId * 10 + 1, completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("taskId", taskId);
        snoozeIntent.putExtra("taskTitle", title);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, uniqueNotificationId * 10 + 2, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(context, NotificationReceiver.class);
        cancelIntent.setAction("ACTION_CANCEL");
        cancelIntent.putExtra("taskId", taskId);
        cancelIntent.putExtra("taskTitle", title);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                context, uniqueNotificationId * 10 + 3, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.todo2) // Replace with your icon
                .setContentTitle(title) // Display title
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.complete, "Complete", completePendingIntent)
                .addAction(R.drawable.complete, "Snooze", snoozePendingIntent)
                .addAction(R.drawable.complete, "Cancel", cancelPendingIntent);
//                .setStyle(new MediaStyle()
//                        .setShowActionsInCompactView(0, 1, 2));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(uniqueNotificationId, builder.build());
    }

    public void cancelNotification(String taskId) {
        int uniqueNotificationId = taskId.hashCode();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(uniqueNotificationId);
    }
}
