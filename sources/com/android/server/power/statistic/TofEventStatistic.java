package com.android.server.power.statistic;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class TofEventStatistic {
    private static final String AON_GESTURE_CUE_TONE = "miui_aon_gesture_cue_tone";
    public static final String FEATURE_AON_GESTURE_SUPPORT = "config_aon_gesture_available";
    private static final String FEATURE_TOF_GESTURE_SUPPORT = "config_tof_gesture_available";
    private static final String FEATURE_TOF_PROXIMITY_SUPPORT = "config_tof_proximity_available";
    private static final String KEY_AON_APP_USAGE_FREQUENCY = "aon_app_usage_frequency";
    private static final String KEY_AON_GESTURE_CUE_TONE_ENABLE = "aon_gesture_cue_tone_enable";
    private static final String KEY_AON_GESTURE_ENABLE = "aon_gesture_enable";
    private static final String KEY_AON_GESTURE_USAGE_COUNT = "aon_gesture_count";
    private static final String KEY_TOF_APP_USAGE_FREQUENCY = "tof_app_usage_frequency";
    private static final String KEY_TOF_GESTURE_CUE_TONE_ENABLE = "tof_gesture_cue_tone_enable";
    private static final String KEY_TOF_GESTURE_ENABLE = "tof_gesture_enable";
    private static final String KEY_TOF_GESTURE_USAGE_COUNT = "tof_gesture_count";
    private static final String KEY_TOF_SCREEN_OFF_ENABLE = "tof_screen_off_enable";
    private static final String KEY_TOF_SCREEN_OFF_USAGE_COUNT = "tof_screen_off_count";
    private static final String KEY_TOF_SCREEN_ON_ENABLE = "tof_screen_on_enable";
    private static final String KEY_TOF_SCREEN_ON_USAGE_COUNT = "tof_screen_on_count";
    public static final String TAG = "MiuiTofStatisticTracker";
    public static final String TOF_EVENT_NAME = "tof_statistic";
    private static final String TOF_GESTURE_CUE_TONE = "miui_tof_gesture_cue_tone";
    public static final int TOF_GESTURE_DOUBLE_PRESS = 7;
    public static final int TOF_GESTURE_DOWN = 3;
    public static final int TOF_GESTURE_DRAW_CIRCLE = 8;
    public static final int TOF_GESTURE_LEFT = 1;
    public static final int TOF_GESTURE_RIGHT = 2;
    public static final int TOF_GESTURE_UP = 4;
    private boolean mAonGestureCueToneEnable;
    private boolean mAonGestureEnable;
    private String mAppId;
    private Context mContext;
    private OneTrackerHelper mOneTrackerHelper;
    private Map<String, AppUsage> mTofAppUsageEventsMap = new HashMap();
    private boolean mTofGestureCueToneEnable;
    private boolean mTofGestureEnable;
    private int mTofGestureUsageCount;
    private boolean mTofScreenOffEnable;
    private int mTofScreenOffUsageCount;
    private boolean mTofScreenOnEnable;
    private int mTofScreenOnUsageCount;
    private static final boolean SUPPORT_TOF_PROXIMITY = FeatureParser.getBoolean("config_tof_proximity_available", false);
    private static final boolean SUPPORT_TOF_GESTURE = FeatureParser.getBoolean("config_tof_gesture_available", false);
    private static final boolean SUPPORT_AON_GESTURE = FeatureParser.getBoolean("config_aon_gesture_available", false);

    public TofEventStatistic(String appId, Context context, OneTrackerHelper oneTrackerHelper) {
        this.mAppId = appId;
        this.mContext = context;
        this.mOneTrackerHelper = oneTrackerHelper;
    }

    public void reportTofEventByIntent() {
        boolean z = SUPPORT_TOF_GESTURE;
        if (!z && !SUPPORT_TOF_PROXIMITY && !SUPPORT_AON_GESTURE) {
            return;
        }
        this.mTofScreenOnEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_screen_on", 0, -2) != 0;
        this.mTofScreenOffEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_screen_off", 0, -2) != 0;
        this.mTofGestureEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_gesture", 0, -2) != 0;
        this.mAonGestureEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_aon_gesture", 0, -2) != 0;
        this.mTofGestureCueToneEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), TOF_GESTURE_CUE_TONE, -1, -2) != 0;
        this.mAonGestureCueToneEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), AON_GESTURE_CUE_TONE, -1, -2) != 0;
        Intent intent = this.mOneTrackerHelper.getTrackEventIntent(this.mAppId, TOF_EVENT_NAME, this.mContext.getPackageName());
        if (SUPPORT_TOF_PROXIMITY) {
            intent.putExtra(KEY_TOF_SCREEN_ON_ENABLE, this.mTofScreenOnEnable).putExtra(KEY_TOF_SCREEN_OFF_ENABLE, this.mTofScreenOffEnable).putExtra(KEY_TOF_SCREEN_ON_USAGE_COUNT, this.mTofScreenOnUsageCount).putExtra(KEY_TOF_SCREEN_OFF_USAGE_COUNT, this.mTofScreenOffUsageCount);
        }
        if (z) {
            intent.putExtra(KEY_TOF_GESTURE_ENABLE, this.mTofGestureEnable).putExtra(KEY_TOF_GESTURE_USAGE_COUNT, this.mTofGestureUsageCount).putExtra(KEY_TOF_GESTURE_CUE_TONE_ENABLE, this.mTofGestureCueToneEnable).putExtra(KEY_TOF_APP_USAGE_FREQUENCY, getTofAppUsageEventsString());
        }
        if (SUPPORT_AON_GESTURE) {
            intent.putExtra(KEY_AON_GESTURE_ENABLE, this.mAonGestureEnable).putExtra(KEY_AON_GESTURE_USAGE_COUNT, this.mTofGestureUsageCount).putExtra(KEY_AON_GESTURE_CUE_TONE_ENABLE, this.mAonGestureCueToneEnable).putExtra(KEY_AON_APP_USAGE_FREQUENCY, getTofAppUsageEventsString());
        }
        try {
            Slog.d(TAG, "reportGestureEventByIntent: intent:" + intent.getExtras());
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportTofEvent fail! " + e);
        }
        resetTofEventData();
    }

    private void resetTofEventData() {
        this.mTofScreenOnEnable = false;
        this.mTofScreenOffEnable = false;
        this.mTofGestureEnable = false;
        this.mTofGestureCueToneEnable = true;
        this.mAonGestureEnable = false;
        this.mAonGestureCueToneEnable = true;
        this.mTofGestureUsageCount = 0;
        this.mTofScreenOnUsageCount = 0;
        this.mTofScreenOffUsageCount = 0;
        this.mTofAppUsageEventsMap.clear();
    }

    private String getTofAppUsageEventsString() {
        List appUsageList = getTofAppUsageEvents(this.mTofAppUsageEventsMap);
        if (appUsageList.size() <= 0) {
            return "";
        }
        String appUsage = appUsageList.toString();
        return appUsage;
    }

    private List getTofAppUsageEvents(Map<String, AppUsage> appUsageEvents) {
        List<AppUsage> list = new ArrayList<>();
        for (Map.Entry<String, AppUsage> entry : appUsageEvents.entrySet()) {
            AppUsage appUsage = entry.getValue();
            list.add(appUsage);
        }
        return list;
    }

    public void notifyGestureEvent(String pkg, boolean success, int label) {
        this.mTofGestureUsageCount++;
        if (pkg == null) {
            return;
        }
        if (!this.mTofAppUsageEventsMap.containsKey(pkg)) {
            AppUsage appUsage = new AppUsage();
            appUsage.setPackageName(pkg);
            this.mTofAppUsageEventsMap.put(pkg, appUsage);
        }
        this.mTofAppUsageEventsMap.get(pkg).setGestureEvent(success, label);
    }

    public void notifyTofPowerState(boolean wakeup) {
        if (wakeup) {
            this.mTofScreenOnUsageCount++;
        } else {
            this.mTofScreenOffUsageCount++;
        }
    }

    public void dumpLocal() {
        StringBuffer stringBuffer = new StringBuffer("MiuiToFStaticTracker:");
        stringBuffer.append("\nScreenOnEnable:" + this.mTofScreenOnEnable);
        stringBuffer.append("\nScreenOffEnable:" + this.mTofScreenOffEnable);
        stringBuffer.append("\nGestureEnable:" + this.mTofGestureEnable);
        stringBuffer.append("\nScreenOnCount:" + this.mTofScreenOnUsageCount);
        stringBuffer.append("\nScreenOffCount:" + this.mTofScreenOffUsageCount);
        stringBuffer.append("\nGestureEventCount:" + this.mTofGestureUsageCount);
        stringBuffer.append("\nappUsage:" + getTofAppUsageEvents(this.mTofAppUsageEventsMap).toString());
        Slog.i(TAG, stringBuffer.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppUsage {
        private int mFailCircleNum;
        private int mFailDoubleTapNum;
        private int mFailDownNum;
        private int mFailLeftNum;
        private int mFailRightNum;
        private int mFailUpNum;
        private String mPackageName;
        private int mSuccessCircleNum;
        private int mSuccessDoubleTapNum;
        private int mSuccessDownNum;
        private int mSuccessLeftNum;
        private int mSuccessRightNum;
        private int mSuccessUpNum;
        private String mUsageCount;

        public AppUsage() {
        }

        public AppUsage(String mPackageName, String mUsageCount) {
            this.mPackageName = mPackageName;
            this.mUsageCount = mUsageCount;
        }

        public int getSuccessUpNum() {
            return this.mSuccessUpNum;
        }

        public void setSuccessUpNum(int mSuccessUpNum) {
            this.mSuccessUpNum = mSuccessUpNum;
        }

        public int getSuccessDownNum() {
            return this.mSuccessDownNum;
        }

        public void setSuccessDownNum(int mSuccessDownNum) {
            this.mSuccessDownNum = mSuccessDownNum;
        }

        public int getSuccessLeftNum() {
            return this.mSuccessLeftNum;
        }

        public void setSuccessLeftNum(int mSuccessLeftNum) {
            this.mSuccessLeftNum = mSuccessLeftNum;
        }

        public int getSuccessRightNum() {
            return this.mSuccessRightNum;
        }

        public void setSuccessRightNum(int mSuccessRightNum) {
            this.mSuccessRightNum = mSuccessRightNum;
        }

        public int getSuccessCircleNum() {
            return this.mSuccessCircleNum;
        }

        public void setSuccessCircleNum(int mSuccessCircleNum) {
            this.mSuccessCircleNum = mSuccessCircleNum;
        }

        public int getSuccessDoubleTapNum() {
            return this.mSuccessDoubleTapNum;
        }

        public void setSuccessDoubleTapNum(int mSuccessDoubleTapNum) {
            this.mSuccessDoubleTapNum = mSuccessDoubleTapNum;
        }

        public int getFailUpNum() {
            return this.mFailUpNum;
        }

        public void setFailUpNum(int mFailUpNum) {
            this.mFailUpNum = mFailUpNum;
        }

        public int getFailDownNum() {
            return this.mFailDownNum;
        }

        public void setFailDownNum(int mFailDownNum) {
            this.mFailDownNum = mFailDownNum;
        }

        public int getFailLeftNum() {
            return this.mFailLeftNum;
        }

        public void setFailLeftNum(int mFailLeftNum) {
            this.mFailLeftNum = mFailLeftNum;
        }

        public int getFailRightNum() {
            return this.mFailRightNum;
        }

        public void setFailRightNum(int mFailRightNum) {
            this.mFailRightNum = mFailRightNum;
        }

        public int getFailCircleNum() {
            return this.mFailCircleNum;
        }

        public void setFailCircleNum(int mFailCircleNum) {
            this.mFailCircleNum = mFailCircleNum;
        }

        public int getFailDoubleTapNum() {
            return this.mFailDoubleTapNum;
        }

        public void setFailDoubleTapNum(int mFailDoubleTapNum) {
            this.mFailDoubleTapNum = mFailDoubleTapNum;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public void setPackageName(String mPackageName) {
            this.mPackageName = mPackageName;
        }

        public String getUsageCount() {
            return this.mUsageCount;
        }

        public void setUsageCount(String mUsageCount) {
            this.mUsageCount = mUsageCount;
        }

        public void reset() {
            this.mSuccessUpNum = 0;
            this.mSuccessDownNum = 0;
            this.mSuccessLeftNum = 0;
            this.mSuccessRightNum = 0;
            this.mSuccessCircleNum = 0;
            this.mSuccessDoubleTapNum = 0;
            this.mFailUpNum = 0;
            this.mFailDownNum = 0;
            this.mFailLeftNum = 0;
            this.mFailRightNum = 0;
            this.mFailCircleNum = 0;
            this.mFailDoubleTapNum = 0;
        }

        public void setGestureEvent(boolean success, int label) {
            switch (label) {
                case 1:
                    if (success) {
                        this.mSuccessLeftNum++;
                        return;
                    } else {
                        this.mFailLeftNum++;
                        return;
                    }
                case 2:
                    if (success) {
                        this.mSuccessRightNum++;
                        return;
                    } else {
                        this.mFailRightNum++;
                        return;
                    }
                case 3:
                    if (success) {
                        this.mSuccessDownNum++;
                        return;
                    } else {
                        this.mFailDownNum++;
                        return;
                    }
                case 4:
                    if (success) {
                        this.mSuccessUpNum++;
                        return;
                    } else {
                        this.mFailUpNum++;
                        return;
                    }
                case 5:
                case 6:
                default:
                    return;
                case 7:
                    if (success) {
                        this.mSuccessDoubleTapNum++;
                        return;
                    } else {
                        this.mFailDoubleTapNum++;
                        return;
                    }
                case 8:
                    if (success) {
                        this.mSuccessCircleNum++;
                        return;
                    } else {
                        this.mFailCircleNum++;
                        return;
                    }
            }
        }

        public String toString() {
            return "{app:" + this.mPackageName + ",success:{up:" + this.mSuccessUpNum + ",down:" + this.mSuccessDownNum + ",left:" + this.mSuccessLeftNum + ",right:" + this.mSuccessRightNum + ",circle:" + this.mSuccessCircleNum + ",doubletap:" + this.mSuccessDoubleTapNum + "},fail:{up:" + this.mFailUpNum + ",down:" + this.mFailDownNum + ",left:" + this.mFailLeftNum + ",right:" + this.mFailRightNum + ",circle:" + this.mFailCircleNum + ",doubletap:" + this.mFailDoubleTapNum + "}}";
        }
    }
}
