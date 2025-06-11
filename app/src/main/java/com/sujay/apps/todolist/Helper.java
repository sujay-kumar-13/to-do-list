package com.sujay.apps.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Helper {
    private static final String USERNAME = "username";
    private static final String USER_KEY = "user_key";
    private static final String NOTIFICATIONS = "notifications";
    private static final String NOTIFICATION_KEY = "notification_key";
    private static final String TIME = "default_time";
    private static final String TIME_KEY = "time_key";

    public static void saveUserTime(Context context, String userTime) {
        SharedPreferences prefs = context.getSharedPreferences(TIME, Context.MODE_PRIVATE);
        Log.d("Helper", "Default Notification Time Changed");
        prefs.edit().putString(TIME_KEY, userTime).apply();
    }

    public static String getUserTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TIME, Context.MODE_PRIVATE);
        return prefs.getString(TIME_KEY, "08:00 AM");
    }

    public static void saveUser(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(USERNAME, Context.MODE_PRIVATE);
        prefs.edit().putString(USER_KEY, username).apply();
    }

    public static String getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(USERNAME, Context.MODE_PRIVATE);
        return prefs.getString(USER_KEY, "Not Signed-in");
    }
}
