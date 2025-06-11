package com.sujay.apps.todolist;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ReminderWorker extends Worker {
    private Context context;
    private static final String TAG = "ReminderWorker";
    public static final String KEY_TASK_TITLE = "task_title";
    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_REPEATING_DAILY = "repeating_daily";
    public static final String KEY_TIME = "time";
    public static final String KEY_NOTIFICATION_ID = "notification_id"; // Add a unique ID

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void scheduleReminder(Context context, String taskId, String taskTitle, String time, String date, boolean isRepeating) {
//        if(!time.isEmpty() && !date.isEmpty()) {
            long triggerTimeMillis = calculateTriggerTime(time, date);

            if (triggerTimeMillis <= System.currentTimeMillis()) {
                scheduleReminderForNextDay(context, taskId, taskTitle, time);
//                Log.d(TAG, "Time for reminder is in the past. Not scheduling.");
                return;
            }

            // Cancel reminder for previous task (if exists)
            cancelReminder(context, taskId);

            // Add the new reminder or update the existing one
            Data inputData = new Data.Builder()
                    .putString(KEY_TASK_TITLE, taskTitle)
                    .putString(KEY_TASK_ID, taskId)
                    .putString(KEY_TIME, time)
                    .putBoolean(KEY_REPEATING_DAILY, isRepeating)
//                    .putInt(KEY_NOTIFICATION_ID, notificationId)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(triggerTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag(taskId)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);

            Log.d(TAG, "Reminder scheduled for: " + taskTitle + " at " + time + " on " + date);
//        }
    }

    public static void snoozeReminder(Context context, String taskId, String taskTitle) {
        long snoozeTimeMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30); // 30 minutes from now

        // Cancel existing reminder if needed
        cancelReminder(context, taskId);

        // Create input data for WorkManager
        Data inputData = new Data.Builder()
                .putString(KEY_TASK_TITLE, taskTitle)
                .putString(KEY_TASK_ID, taskId)
                .build();

        // Schedule a new WorkManager task with a 30-minute delay
        OneTimeWorkRequest snoozeWorkRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag(taskId) // Ensures this task can be managed later
                .build();

        WorkManager.getInstance(context).enqueue(snoozeWorkRequest);

        Log.d(TAG, "Task snoozed for 30 minutes: " + taskTitle);
    }


    private static long calculateTriggerTime(String time, String date) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String combinedDateTime = date + " " + time;
            Date dateObj = dateTimeFormat.parse(combinedDateTime);
            return dateObj.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date/time: " + e.getMessage());
            return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1); // Default to 1 minute from now
        }
    }

    private static void scheduleReminderForNextDay(Context context, String taskId, String taskTitle, String time) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1); // Move to next day

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String nextDate = dateFormat.format(calendar.getTime());

        scheduleReminder(context, taskId, taskTitle, time, nextDate, true);
        Log.d(TAG, "Reminder set for next day for: " + taskTitle);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String taskTitle = inputData.getString(KEY_TASK_TITLE);
        String taskId = inputData.getString(KEY_TASK_ID);
        String time = inputData.getString(KEY_TIME);
        boolean isRepeating = inputData.getBoolean(KEY_REPEATING_DAILY, false);

        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        notificationHelper.showNotification(taskTitle, taskId);

        Log.d(TAG, "Reminder delivered for: " + taskTitle);

        if(isRepeating) {
            scheduleReminderForNextDay(getApplicationContext(), taskId, taskTitle, time);
        }

        //updates the ui if app is running
        // Send broadcast to notify UI
        Intent intent = new Intent("TASKS_UPDATED");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        return Result.success();
    }

    public static void cancelReminder(Context context, String taskId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(taskId);
        Log.d(TAG, "Reminder canceled for: " + taskId);
    }
}