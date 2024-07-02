package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/* loaded from: classes.dex */
public class DarkModeTimeModeHelper {
    private static final String TAG = "DarkModeTimeModeHelper";

    public static boolean isInNight(Context context) {
        return getNowTime() >= getSunSetTime(context) || getNowTime() <= getSunRiseTime(context);
    }

    public static int getNowTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        return (hour * 60) + minute;
    }

    public static long getNowTimeInMills() {
        return System.currentTimeMillis();
    }

    public static String getTimeInString(int time) {
        return (time / 60) + ": " + (time % 60);
    }

    public static boolean isDarkModeOpen(Context context) {
        if (isDarkModeEnable(context)) {
            Slog.i(TAG, "darkModeEnable = true");
            return true;
        }
        if (isDarkModeTimeEnable(context)) {
            Slog.i(TAG, "darkModeTimeEnable = true");
            return true;
        }
        return false;
    }

    public static void setDarkModeSuggestCount(Context context, int count) {
        Settings.System.putInt(context.getContentResolver(), "dark_mode_suggest_notification_count", count);
    }

    public static int getDarkModeSuggestCount(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "dark_mode_suggest_notification_count", 0);
    }

    public static boolean isOnHome(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        return getHomeApplicationList(context).contains(runningTaskInfos.get(0).topActivity.getPackageName());
    }

    private static List<String> getHomeApplicationList(Context context) {
        List<String> names = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 65536);
        for (ResolveInfo resolveInfo : resolveInfos) {
            names.add(resolveInfo.activityInfo.packageName);
        }
        return names;
    }

    public static boolean isDarkModeEnable(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), DarkModeTimeModeManager.DARK_MODE_ENABLE, 0, 0) == 1;
    }

    public static boolean isDarkModeTimeEnable(Context context) {
        return MiuiSettings.System.getBoolean(context.getContentResolver(), "dark_mode_time_enable", false);
    }

    public static void setDarkModeAutoTimeEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_auto_time_enable", enable);
    }

    public static void setSunRiseSunSetMode(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_sun_time_mode_enable", enable);
    }

    public static void setDarkModeTimeEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_time_enable", enable);
    }

    public static void setLastSuggestTime(Context ctx, long lastTime) {
        Settings.System.putLong(ctx.getContentResolver(), "dark_mode_last_time_of_suggest", lastTime);
    }

    public static void setDarkModeSuggestEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_has_get_suggest_from_cloud", enable);
    }

    public static int getDarkModeStartTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_start", 1140);
    }

    public static int getDarkModeEndTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_end", 420);
    }

    public static int getSunRiseTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "auto_night_end_time", 420);
    }

    public static int getSunSetTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "auto_night_start_time", 1200);
    }

    public static int getDarkModeTimeType(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_type", 2);
    }

    public static long getLastSuggestTime(Context ctx) {
        return Settings.System.getLong(ctx.getContentResolver(), "dark_mode_last_time_of_suggest", -10080L);
    }

    public static boolean isDarkModeSuggestEnable(Context ctx) {
        return MiuiSettings.System.getBoolean(ctx.getContentResolver(), "dark_mode_has_get_suggest_from_cloud", false);
    }

    public static void setDarkModeTimeType(Context context, int type) {
        Settings.System.putInt(context.getContentResolver(), "dark_mode_time_type", type);
    }

    public static boolean isSuntimeType(Context context) {
        return 2 == getDarkModeTimeType(context);
    }

    public static boolean isDarkModeContrastEnable(Context ctx) {
        return MiuiSettings.System.getBoolean(ctx.getContentResolver(), "dark_mode_contrast_enable", false);
    }

    public static void setSunTimeFromCloud(Context context, boolean z) {
        Settings.System.putInt(context.getContentResolver(), "get_sun_time_from_cloud", z ? 1 : 0);
    }

    public static boolean isSunTimeFromCloud(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "get_sun_time_from_cloud", 0) != 0;
    }
}
