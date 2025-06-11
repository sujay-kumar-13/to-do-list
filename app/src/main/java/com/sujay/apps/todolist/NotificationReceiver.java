package com.sujay.apps.todolist;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationReceiver extends BroadcastReceiver {
    private final String TAG = "NotificationReceiver";
    private String userId;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        String taskId = bundle.getString("taskId");
        String taskTitle = bundle.getString("taskTitle");
        int notificationId = taskId.hashCode();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Clear the notification when any action is clicked
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        if(action != null) {
            switch (action) {
                case "ACTION_COMPLETE" :
                    completeTask(context, taskId);
                    break;

                case "ACTION_SNOOZE" :
                    ReminderWorker.snoozeReminder(context, taskId, taskTitle);
                    Toast.makeText(context, "Snoozed", Toast.LENGTH_SHORT).show();
                    break;

                case "ACTION_CANCEL" :
                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void completeTask(Context context, String taskId) {
        DocumentReference taskRef = FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("tasks").document(taskId);

        taskRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                boolean repeatDaily = Boolean.TRUE.equals(documentSnapshot.getBoolean("isRepeating"));

                // Mark current task as done
                taskRef.update("taskDone", true, "isRepeating", false).addOnSuccessListener(aVoid -> {
                    ReminderWorker.cancelReminder(context, taskId);
                    Log.d(TAG, "Task status updated successfully");
                    if (repeatDaily) {
                        createNextTask(userId, documentSnapshot, context);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update task status", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating task completion", e);
                });
            }
        });
    }

    private void createNextTask(String userId, DocumentSnapshot oldTask, Context context) {
        // Get old task details
        String taskTitle = oldTask.getString("taskName");
        String time = oldTask.getString("dueTime");
        String date = oldTask.getString("dueDate");
        String nextDate;

        // Calculate the next day's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try{
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            nextDate = dateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            Log.e("TaskListFragment", "Error adding date", e);
            nextDate = date;
        }

        // Create new task data
        Map<String, Object> newTask = new HashMap<>();
        newTask.put("taskName", taskTitle);
        newTask.put("dueTime", time);
        newTask.put("dueDate", nextDate);
        newTask.put("taskDone", false);
        newTask.put("isRepeating", true);

        // Save the new task in Firestore
        String finalNextDate = nextDate;
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("tasks")
                .add(newTask)
                .addOnCompleteListener(taskAdd -> {
                    String newTaskId = taskAdd.getResult().getId();
                    Log.d(TAG, "New repeating task created for next day");
                    ReminderWorker.scheduleReminder(context, newTaskId, taskTitle, time, finalNextDate, true);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create next repeating task", e));
    }
}
