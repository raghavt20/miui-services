package com.android.server.wm;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import miui.os.Build;

/* loaded from: classes.dex */
public class OneTrackVulkanHelper {
    public static String APP_CRASH_INFO = null;
    private static final String APP_ID = "31000401696";
    private static final String EVENT_NAME = "app_use_vulkan";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String PACKAGE_NAME = "android";
    private static final String TAG = OneTrackVulkanHelper.class.getSimpleName();
    static Context mContext;
    private static OneTrackVulkanHelper sInstance;

    public static synchronized OneTrackVulkanHelper getInstance() {
        OneTrackVulkanHelper oneTrackVulkanHelper;
        synchronized (OneTrackVulkanHelper.class) {
            if (sInstance == null) {
                sInstance = new OneTrackVulkanHelper();
            }
            Application application = ActivityThread.currentActivityThread().getApplication();
            mContext = application;
            if (application == null) {
                Slog.e(TAG, "init OneTrackVulkanHelper mContext = null");
            }
            oneTrackVulkanHelper = sInstance;
        }
        return oneTrackVulkanHelper;
    }

    public void getCrashInfo(String trace) {
        APP_CRASH_INFO = trace;
    }

    public void reportOneTrack(String packageName) {
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
            Bundle params = new Bundle();
            intent.putExtras(params);
            intent.putExtra("vulkan_package_name", packageName);
            intent.putExtra("vulkan_crash_info", APP_CRASH_INFO);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(3);
            }
            mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            Slog.i(TAG, "reportOneTrack");
        } catch (Exception e) {
            Slog.e(TAG, "Upload use VulkanApp exception! " + packageName, e);
        }
    }
}
