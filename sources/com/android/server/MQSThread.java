package com.android.server;

import android.media.MiuiXlog;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;

/* loaded from: classes.dex */
public class MQSThread extends Thread {
    private static final String DYNAMIC_DAILY_USE_FORMAT = "{\"name\":\"dynamic_dailyuse\",\"audio_event\":{\"dynamic_package_name\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\" }";
    private static final String TAG = "MQSThread.vibrator";
    private static final String VIBRATION_DAILY_USE_FORMAT = "{\"name\":\"vibration_dailyuse\",\"audio_event\":{\"vibration_package_name\":\"%s\",\"vibration_effect_id\":\"%s\",\"vibration_count\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\" }";
    private final String EffectID;
    private final String PackageName;
    private final String count;
    private MiuiXlog mMiuiXlog;

    public MQSThread(String PKG) {
        this.mMiuiXlog = new MiuiXlog();
        this.PackageName = PKG;
        this.EffectID = null;
        this.count = null;
    }

    public MQSThread(String PKG, String ID, int nums) {
        this.mMiuiXlog = new MiuiXlog();
        this.PackageName = PKG;
        this.EffectID = ID;
        this.count = Integer.toString(nums);
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(0);
        try {
            String str = this.EffectID;
            if (str != null && !str.isEmpty()) {
                reportVibrationDailyUse();
            } else {
                reportDynamicDailyUse();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error for MQSThread");
        }
    }

    public void reportDynamicDailyUse() {
        if (!isAllowedRegion()) {
            return;
        }
        Slog.i(TAG, "reportDynamicDailyUse start.");
        String result = String.format(DYNAMIC_DAILY_USE_FORMAT, this.PackageName);
        Slog.d(TAG, "reportDynamicDailyUse:" + result);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Throwable th) {
            Slog.d(TAG, "can not use miuiXlogSend!!!");
        }
    }

    public void reportVibrationDailyUse() {
        if (!isAllowedRegion()) {
            return;
        }
        Slog.i(TAG, "reportVibrationDailyUse start.");
        String result = String.format(VIBRATION_DAILY_USE_FORMAT, this.PackageName, this.EffectID, this.count);
        Slog.d(TAG, "reportVibrationDailyUse:" + result);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Throwable th) {
            Slog.d(TAG, "can not use miuiXlogSend!!!");
        }
    }

    private boolean isAllowedRegion() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("CN");
    }
}
