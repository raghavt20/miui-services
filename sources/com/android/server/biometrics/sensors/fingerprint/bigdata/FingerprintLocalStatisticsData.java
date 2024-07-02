package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import miui.os.Build;
import miui.util.ITouchFeature;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintLocalStatisticsData {
    private static final String APP_ID = "31000000080";
    private static final String EVENT_NAME = "FingerprintUnlockInfo";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final boolean FP_LOCAL_STATISTICS_DEBUG = false;
    private static final String FP_LOCAL_STATISTICS_DIR = "fingerprint";
    private static final String FP_LOCAL_STATISTICS_PREFS_FILE = "fingerprint_track";
    private static final String KEY_FP_ENROLLED_COUNT = "key_fp_enrolled_count";
    private static final String KEY_FP_OPTION = "key_fp_option";
    private static final String KEY_FP_TOUCH_FILM_MODE = "key_fp_touch_film_mode";
    private static final String KEY_FP_TYPE = "key_fp_type";
    private static final String KEY_FP_VENDOR = "key_fp_vendor";
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String TAG = "FingerprintLocalStatisticsData";
    private static volatile FingerprintLocalStatisticsData sInstance;
    private FingerprintAuthTimeData authTimeInstance;
    private FingerprintFailReasonData failReasonInstance;
    private FingerprintHalAuthData halAuthInstance;
    private FingerprintHalEnrollData halEnrollInstance;
    private Context mContext;
    private SharedPreferences mFpLocalSharedPreferences;
    private File mFpPrefsFile;
    private SharedPreferences.Editor mFpSharedPreferencesEditor;
    private FingerprintUnlockRateData unlockRateInstance;
    private static boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    private static boolean IS_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private static String FP_VENDOR = SystemProperties.get("persist.vendor.sys.fp.vendor", "");
    private static final boolean FP_LOCAL_STATISTICS_ENABLED = SystemProperties.getBoolean("persist.vendor.sys.fp.onetrack.enable", true);
    private static final long FP_LOCAL_STATISTICS_UPLOAD_PERIOD = SystemProperties.getLong("ro.hardware.fp.onetrack.period", 86400000);
    private List<String> LocalStatisticsKeyList = Arrays.asList("unlock_rate_info", "fail_reason_info", "auth_time_info", "unlock_hal_info");
    private Map<String, JSONObject> JsonMap = new LinkedHashMap();
    private Map<String, FingerprintBigData> InstanceMap = new LinkedHashMap();
    private int mUnlockOption = -1;
    private int mEnrolledCount = 0;
    private int mScreenStatus = -1;
    private int filmMode = -1;
    private long mLocalStatisticsUploadTime = 0;
    private boolean mLocalStatisticsInit = false;

    public static FingerprintLocalStatisticsData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintLocalStatisticsData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintLocalStatisticsData();
                }
            }
        }
        return sInstance;
    }

    private void initInstanceMap() {
        this.unlockRateInstance = FingerprintUnlockRateData.getInstance();
        this.failReasonInstance = FingerprintFailReasonData.getInstance();
        this.authTimeInstance = FingerprintAuthTimeData.getInstance();
        this.halAuthInstance = FingerprintHalAuthData.getInstance();
        this.halEnrollInstance = FingerprintHalEnrollData.getInstance();
        this.InstanceMap.put("unlock_rate_info", this.unlockRateInstance);
        this.InstanceMap.put("fail_reason_info", this.failReasonInstance);
        this.InstanceMap.put("auth_time_info", this.authTimeInstance);
        this.InstanceMap.put("unlock_hal_info", this.halAuthInstance);
        this.InstanceMap.put("enroll_hal_info", this.halEnrollInstance);
    }

    public synchronized void initLocalStatistics(Context context) {
        this.mContext = context;
        if (context == null) {
            return;
        }
        if (!this.mLocalStatisticsInit) {
            Slog.d(TAG, "initLocalStatistics");
            initInstanceMap();
            File prefsDir = new File(Environment.getDataSystemCeDirectory(0), FP_LOCAL_STATISTICS_DIR);
            File file = new File(prefsDir, FP_LOCAL_STATISTICS_PREFS_FILE);
            this.mFpPrefsFile = file;
            try {
                SharedPreferences sharedPreferences = this.mContext.getSharedPreferences(file, 32768);
                this.mFpLocalSharedPreferences = sharedPreferences;
                this.mFpSharedPreferencesEditor = sharedPreferences.edit();
                this.mLocalStatisticsUploadTime = this.mFpLocalSharedPreferences.getLong("reporttimestamp", 0L);
                try {
                    for (String key : this.LocalStatisticsKeyList) {
                        String sharedStr = this.mFpLocalSharedPreferences.getString(key, "");
                        if (!sharedStr.isEmpty()) {
                            Slog.w(TAG, "mLocalStatisticsInit: get: " + key + ", from SP: " + sharedStr.toString());
                            this.JsonMap.put(key, new JSONObject(sharedStr));
                            JSONObject targetJson = this.JsonMap.get(key);
                            if (this.InstanceMap.get(key) == null) {
                                Slog.w(TAG, "get key of instance fail: " + key);
                                removeTargetLocalStatistics(key);
                            } else {
                                boolean result = this.InstanceMap.get(key).updateJsonToData(targetJson);
                                if (!result) {
                                    Slog.e(TAG, "initLocalStatistics unlockRateInfo exception");
                                    removeTargetLocalStatistics(key);
                                }
                            }
                        } else {
                            this.JsonMap.put(key, new JSONObject());
                            FingerprintBigData bigdata = this.InstanceMap.get(key);
                            if (bigdata == null) {
                                initInstanceMap();
                            }
                            resetTargetInstanceData(key);
                        }
                    }
                    this.mLocalStatisticsInit = true;
                } catch (JSONException e) {
                    Slog.e(TAG, "initLocalStatistics exception", e);
                }
            } catch (IllegalStateException e2) {
                Slog.e(TAG, "initLocalStatistics IllegalStateException", e2);
                resetLocalStatisticsData();
            }
        }
    }

    public void updataLocalStatistics(String targetStr) {
        Slog.w(TAG, "updataLocalStatistics, target:  " + targetStr);
        if (this.mFpSharedPreferencesEditor != null) {
            try {
                if (this.InstanceMap.get(targetStr) == null) {
                    Slog.w(TAG, "updataLocalStatistics, target instance is null !");
                    return;
                }
                if (this.JsonMap.get(targetStr) == null) {
                    this.JsonMap.put(targetStr, new JSONObject());
                }
                FingerprintBigData bigdata = this.InstanceMap.get(targetStr);
                JSONObject targetJson = this.JsonMap.get(targetStr);
                boolean result = bigdata.updateDataToJson(targetJson);
                if (!result) {
                    Slog.e(TAG, "updataLocalStatistics, update: " + targetStr + " fail!");
                    bigdata.resetLocalInfo();
                    bigdata.updateDataToJson(targetJson);
                }
                this.mFpSharedPreferencesEditor.putString(targetStr, targetJson.toString());
                this.mFpSharedPreferencesEditor.apply();
            } catch (IllegalStateException e) {
                Slog.e(TAG, "updataLocalStatistics, updata " + targetStr + " IllegalStateException!", e);
            }
        }
    }

    public void resetTargetInstanceData(String targetStr) {
        Slog.i(TAG, "resetTargetInstanceData");
        if (this.InstanceMap.get(targetStr) != null) {
            FingerprintBigData bigdata = this.InstanceMap.get(targetStr);
            bigdata.resetLocalInfo();
        }
        updataLocalStatistics(targetStr);
    }

    public void removeTargetLocalStatistics(String targetStr) {
        SharedPreferences.Editor editor = this.mFpSharedPreferencesEditor;
        if (editor != null) {
            editor.remove(targetStr);
            this.mFpSharedPreferencesEditor.commit();
            this.mFpSharedPreferencesEditor.putLong("reporttimestamp", this.mLocalStatisticsUploadTime);
            this.mFpSharedPreferencesEditor.apply();
        }
        resetTargetInstanceData(targetStr);
        updataLocalStatistics(targetStr);
    }

    public void resetLocalStatisticsData() {
        this.mLocalStatisticsUploadTime = Calendar.getInstance().getTimeInMillis();
        SharedPreferences.Editor editor = this.mFpSharedPreferencesEditor;
        if (editor != null) {
            editor.clear();
            this.mFpSharedPreferencesEditor.commit();
            this.mFpSharedPreferencesEditor.putLong("reporttimestamp", this.mLocalStatisticsUploadTime);
            this.mFpSharedPreferencesEditor.apply();
        }
        for (String targetStr : this.LocalStatisticsKeyList) {
            Slog.w(TAG, "resetLocalStatisticsData, target: " + targetStr);
            resetTargetInstanceData(targetStr);
        }
    }

    public void recordFpTypeAndEnrolledCount(int unlockOption, int enrolledCount) {
    }

    public boolean getModeCurValue() {
        try {
            ITouchFeature touchFeature = ITouchFeature.getInstance();
            if (touchFeature != null) {
                String touchMode = touchFeature.getModeCurValueString(0, 1015);
                switch (touchMode.charAt(0)) {
                    case '0':
                        this.filmMode = 0;
                        break;
                    case '1':
                        this.filmMode = 1;
                        break;
                    case UsbKeyboardUtil.COMMAND_MIAUTH_STEP3_TYPE1 /* 50 */:
                        this.filmMode = 2;
                        break;
                }
                Slog.e(TAG, "getModeCurValue, touchMode:  " + touchMode + ", filmMode: " + this.filmMode);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startLocalStatisticsOneTrackUpload() {
        Slog.i(TAG, "startLocalStatisticsOneTrackUpload");
        if (!this.mLocalStatisticsInit) {
            Slog.e(TAG, "mLocalStatisticsInit failed, skip OneTrackUpload");
            return;
        }
        long nowTime = Calendar.getInstance().getTimeInMillis();
        if (this.mLocalStatisticsUploadTime == 0) {
            this.mLocalStatisticsUploadTime = nowTime;
        }
        if (nowTime - this.mLocalStatisticsUploadTime < FP_LOCAL_STATISTICS_UPLOAD_PERIOD) {
            return;
        }
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, this.mContext.getPackageName());
        String fp_type = IS_POWERFP ? "powerFP" : "backFP";
        intent.putExtra(KEY_FP_TYPE, IS_FOD ? "fod" : fp_type).putExtra(KEY_FP_VENDOR, FP_VENDOR).putExtra(KEY_FP_OPTION, IS_POWERFP ? String.valueOf(this.mUnlockOption) : "null").putExtra(KEY_FP_ENROLLED_COUNT, this.mEnrolledCount);
        if (getModeCurValue()) {
            intent.putExtra(KEY_FP_TOUCH_FILM_MODE, this.filmMode);
        }
        SharedPreferences sharedPreferences = this.mFpLocalSharedPreferences;
        if (sharedPreferences != null) {
            Map<String, ?> fpLocalSaveKeyValue = sharedPreferences.getAll();
            try {
                for (String key : fpLocalSaveKeyValue.keySet()) {
                    if (!key.equals("reporttimestamp")) {
                        Slog.d(TAG, key + ": " + String.valueOf(fpLocalSaveKeyValue.get(key)));
                        intent.putExtra(key, String.valueOf(fpLocalSaveKeyValue.get(key)));
                    }
                }
                if (!Build.IS_INTERNATIONAL_BUILD) {
                    intent.setFlags(2);
                }
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.e(TAG, "Failed to upload FingerprintService event.");
            } catch (SecurityException e2) {
                Slog.e(TAG, "Unable to start service.");
            } catch (Exception e3) {
                Slog.e(TAG, "initLocalStatistics unlockRateInfo exception", e3);
                resetLocalStatisticsData();
                return;
            }
            resetLocalStatisticsData();
        }
    }
}
