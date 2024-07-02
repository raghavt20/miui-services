package com.android.server.display.statistics;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class OneTrackFoldStateHelper {
    private static final String APP_FOLDED_UNFOLDED_AVG_TIME = "app_folded_unfolded_avg_time";
    private static final int APP_FOLD_SCREEN_AVG_TIME = 1;
    private static final String APP_ID = "31000401594";
    private static final int APP_UNFOLD_SCREEN_AVG_TIME = 0;
    private static final String APP_USE_SCREEN_TIME = "app_use_screen_time";
    private static final boolean DEBUG;
    private static final long DEBUG_REPORT_TIME_DURATION = 120000;
    private static final String DEVICE_REGION;
    private static final int DEVICE_STATE_CLOSE = 0;
    private static final long DEVICE_STATE_DEBOUNCE_TIME = 1000;
    private static final int DEVICE_STATE_HALF_OPEN = 2;
    private static final int DEVICE_STATE_INVALID = -1;
    private static final int DEVICE_STATE_OPEN = 3;
    private static final int DEVICE_STATE_OPEN_PRESENTATION = 5;
    private static final int DEVICE_STATE_OPEN_REVERSE = 4;
    private static final int DEVICE_STATE_OPEN_REVERSE_PRESENTATION = 6;
    private static final int DEVICE_STATE_TENT = 1;
    private static final String DEVICE_STATE_TIME = "device_state_time";
    private static final String EVENT_NAME = "fold_statistic";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String FOLDED_AVG_TIME = "folded_avg_time";
    private static final String FOLD_TIMES = "fold_times";
    private static final boolean IS_FOLDABLE_DEVICE;
    private static final boolean IS_INTERNATIONAL_BUILD;
    private static final int MSG_DEVICE_STATE_CHANGED = 3;
    private static final int MSG_DEVICE_STATE_DEBOUNCE = 4;
    private static final int MSG_DISPLAY_SWAP_FINISHED = 6;
    private static final int MSG_INTERACTIVE_CHANGED = 5;
    private static final int MSG_ON_FOCUS_PACKAGE_CHANGED = 8;
    private static final int MSG_ON_FOLD_CHANGED = 1;
    private static final int MSG_ON_FOREGROUND_APP_CHANGED = 2;
    private static final int MSG_RESET_PENDING_DISPLAY_SWAP = 7;
    private static final float ONE_SECOND = 1000.0f;
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONE_TRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String PACKAGE = "android";
    private static final int RECORD_REASON_DEVICE_STATE = 0;
    private static final int RECORD_REASON_FOREGROUND_APP = 2;
    private static final int RECORD_REASON_INTERACTIVE = 1;
    private static final int RECORD_REASON_REPORT_DATA = 3;
    private static final int SCREEN_TYPE_INVALID = -1;
    private static final int SCREEN_TYPE_LARGE = 0;
    private static final int SCREEN_TYPE_SMALL = 1;
    private static final String TAG = "OneTrackFoldStateHelper";
    private static final long TIMEOUT_PENDING_DISPLAY_SWAP_MILLIS = 5000;
    private static final String UNFOLDED_AVG_TIME = "unfolded_avg_time";
    private static volatile OneTrackFoldStateHelper sInstance;
    private String mFocusPackageName;
    private int mFoldCountSum;
    private float mFoldTimeSum;
    private boolean mFolded;
    private String mForegroundAppName;
    private int mLastEventReason;
    private long mLastEventTime;
    private String mLastFocusPackageName;
    private long mLastFoldChangeTime;
    private boolean mPendingWaitForDisplaySwapFinished;
    private int mUnfoldCountSum;
    private float mUnfoldTimeSum;
    private final Map<Integer, HashMap<String, Long>> mDeviceStateUsageDetail = new HashMap();
    private final Map<String, HashMap<Integer, List<Long>>> mAppFoldAvgTimeMap = new HashMap();
    private final List<Integer> mFoldedStateList = new ArrayList<Integer>() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper.1
        {
            add(0);
            add(1);
            add(4);
            add(6);
        }
    };
    private final List<Integer> mUnfoldedStateList = new ArrayList<Integer>() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper.2
        {
            add(2);
            add(3);
            add(5);
        }
    };
    private int mFoldTimes = 0;
    private boolean mInteractive = true;
    private boolean mPreInteractive = true;
    private int mCurrentDeviceState = -1;
    private int mPreviousDeviceState = -1;
    private int mPendingDeviceState = -1;
    private long mPendingDeviceStateDebounceTime = -1;
    private final AlarmManager.OnAlarmListener mOnAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper$$ExternalSyntheticLambda0
        @Override // android.app.AlarmManager.OnAlarmListener
        public final void onAlarm() {
            OneTrackFoldStateHelper.this.lambda$new$1();
        }
    };
    private final IForegroundWindowListener mWindowListener = new IForegroundWindowListener.Stub() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper.3
        public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
            if (OneTrackFoldStateHelper.DEBUG) {
                Slog.d(OneTrackFoldStateHelper.TAG, "onForegroundWindowChanged: pkgName: " + foregroundInfo.mForegroundPackageName);
            }
            OneTrackFoldStateHelper.this.mHandler.removeMessages(2);
            Message msg = OneTrackFoldStateHelper.this.mHandler.obtainMessage(2, foregroundInfo);
            OneTrackFoldStateHelper.this.mHandler.sendMessage(msg);
        }
    };
    private Context mApplicationContext = ActivityThread.currentActivityThread().getApplication();
    private Handler mHandler = new OneTrackFoldStateHelperHandler(MiuiBgThread.get().getLooper());
    private ActivityManager mActivityManager = (ActivityManager) this.mApplicationContext.getSystemService("activity");
    private AlarmManager mAlarmManager = (AlarmManager) this.mApplicationContext.getSystemService("alarm");

    static {
        DEBUG = SystemProperties.getInt("debug.miui.power.fold.dgb", 0) != 0;
        IS_FOLDABLE_DEVICE = SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2;
        IS_INTERNATIONAL_BUILD = SystemProperties.get("ro.product.mod_device", "").contains("_global");
        DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    }

    private OneTrackFoldStateHelper() {
        if (IS_FOLDABLE_DEVICE) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    OneTrackFoldStateHelper.this.lambda$new$0();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        registerForegroundWindowListener();
        setReportScheduleEventAlarm();
    }

    public static OneTrackFoldStateHelper getInstance() {
        if (sInstance == null) {
            synchronized (OneTrackFoldStateHelper.class) {
                if (sInstance == null) {
                    sInstance = new OneTrackFoldStateHelper();
                }
            }
        }
        return sInstance;
    }

    private void setReportScheduleEventAlarm() {
        long now = SystemClock.elapsedRealtime();
        boolean z = DEBUG;
        long duration = z ? DEBUG_REPORT_TIME_DURATION : 86400000L;
        long nextTime = now + duration;
        if (z) {
            Slog.d(TAG, "setReportSwitchStatAlarm: next time: " + TimeUtils.formatDuration(duration));
        }
        AlarmManager alarmManager = this.mAlarmManager;
        if (alarmManager != null) {
            alarmManager.setExact(2, nextTime, TAG, this.mOnAlarmListener, this.mHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1() {
        reportScheduleEvents();
        setReportScheduleEventAlarm();
    }

    private void reportScheduleEvents() {
        if (DEVICE_REGION.equals("IN")) {
            return;
        }
        recordDeviceStateDetails(3);
        updateLastEventInfo(SystemClock.uptimeMillis(), 3);
        reportFoldInfoToOneTrack();
        resetFoldInfoAfterReported();
    }

    public void oneTrackFoldState(boolean folded) {
        if (DEBUG) {
            Slog.d(TAG, "oneTrackFoldState: folded: " + folded);
        }
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1, Boolean.valueOf(folded));
        this.mHandler.sendMessage(msg);
    }

    private void registerForegroundWindowListener() {
        ProcessManager.registerForegroundWindowListener(this.mWindowListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFoldChanged(boolean folded) {
        if (this.mFolded == folded) {
            if (DEBUG) {
                Slog.d(TAG, "handleFoldChanged: no change mFolded: " + this.mFolded);
            }
        } else {
            this.mFolded = folded;
            handleDisplaySwapStarted();
            this.mFoldTimes++;
            if (DEBUG) {
                Slog.d(TAG, "handleFoldChanged: mFoldTimes: " + this.mFoldTimes);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWindowChanged(ForegroundInfo foregroundInfo) {
        String packageName = foregroundInfo.mForegroundPackageName;
        if (DEBUG) {
            Slog.d(TAG, "handleWindowChanged: pkgName: " + packageName + ", mForegroundAppName: " + this.mForegroundAppName);
        }
        if (!TextUtils.equals(this.mForegroundAppName, packageName)) {
            recordDeviceStateDetails(2);
            updateLastEventInfo(SystemClock.uptimeMillis(), 2);
            this.mForegroundAppName = packageName;
        }
    }

    private void reportFoldInfoToOneTrack() {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics").putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
        addDeviceStateUsageTimeToIntent(intent);
        addAppFoldInfoToIntent(intent);
        addAppUsageTimeInScreenToIntent(intent);
        if (!IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        Slog.d(TAG, "reportOneTrack: data: " + intent.getExtras());
        try {
            this.mApplicationContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "reportOneTrack Failed to upload!");
        }
    }

    private void resetFoldInfoAfterReported() {
        this.mFoldTimes = 0;
        this.mDeviceStateUsageDetail.clear();
        this.mAppFoldAvgTimeMap.clear();
        this.mFoldTimeSum = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mFoldCountSum = 0;
        this.mUnfoldTimeSum = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mUnfoldCountSum = 0;
    }

    private void addDeviceStateUsageTimeToIntent(Intent intent) {
        Map<Integer, Float> deviceStateUsageMap = new HashMap<>();
        for (Integer deviceState : this.mDeviceStateUsageDetail.keySet()) {
            long stateSumTime = 0;
            Iterator<Long> it = this.mDeviceStateUsageDetail.get(deviceState).values().iterator();
            while (it.hasNext()) {
                long usageTime = it.next().longValue();
                stateSumTime += usageTime;
            }
            float deviceStateTime = ((float) stateSumTime) / ONE_SECOND;
            deviceStateUsageMap.put(deviceState, Float.valueOf(deviceStateTime));
        }
        if (!deviceStateUsageMap.isEmpty()) {
            intent.putExtra(DEVICE_STATE_TIME, deviceStateUsageMap.toString());
        }
    }

    private void addAppFoldInfoToIntent(Intent intent) {
        int i;
        int i2;
        if (!this.mAppFoldAvgTimeMap.isEmpty()) {
            Map<String, HashMap<Integer, Float>> appFoldAvgTimeMap = new HashMap<>();
            for (String appName : this.mAppFoldAvgTimeMap.keySet()) {
                HashMap<Integer, Float> tempMap = new HashMap<>();
                for (Map.Entry<Integer, List<Long>> foldEntry : this.mAppFoldAvgTimeMap.get(appName).entrySet()) {
                    float totalTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                    Integer foldType = foldEntry.getKey();
                    List<Long> timeList = foldEntry.getValue();
                    Iterator<Long> it = timeList.iterator();
                    while (it.hasNext()) {
                        long time = it.next().longValue();
                        totalTime += (float) time;
                    }
                    float avgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                    if (timeList.size() != 0) {
                        avgTime = totalTime / timeList.size();
                    }
                    tempMap.put(foldType, Float.valueOf(avgTime));
                }
                appFoldAvgTimeMap.put(appName, tempMap);
            }
            intent.putExtra(APP_FOLDED_UNFOLDED_AVG_TIME, appFoldAvgTimeMap.toString());
        }
        float f = this.mFoldTimeSum;
        if (f != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && (i2 = this.mFoldCountSum) != 0) {
            intent.putExtra(FOLDED_AVG_TIME, f / i2);
        }
        float foldAvgTime = this.mUnfoldTimeSum;
        if (foldAvgTime != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && (i = this.mUnfoldCountSum) != 0) {
            float unfoldAvgTime = foldAvgTime / i;
            intent.putExtra(UNFOLDED_AVG_TIME, unfoldAvgTime);
        }
        intent.putExtra(FOLD_TIMES, this.mFoldTimes);
    }

    private void addAppUsageTimeInScreenToIntent(Intent intent) {
        int stateType = -1;
        Map<String, Map<Integer, Float>> deviceStateTimeMap = new HashMap<>();
        for (Integer deviceState : this.mDeviceStateUsageDetail.keySet()) {
            if (this.mFoldedStateList.contains(deviceState)) {
                stateType = 1;
            } else if (this.mUnfoldedStateList.contains(deviceState)) {
                stateType = 0;
            }
            for (Map.Entry<String, Long> appUsageEntry : this.mDeviceStateUsageDetail.get(deviceState).entrySet()) {
                String appName = appUsageEntry.getKey();
                long appUseTime = appUsageEntry.getValue().longValue();
                Map<Integer, Float> appUseMap = deviceStateTimeMap.getOrDefault(appName, new HashMap<>());
                float duration = appUseMap.getOrDefault(Integer.valueOf(stateType), Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)).floatValue();
                float newDuration = (((float) appUseTime) + duration) / ONE_SECOND;
                appUseMap.put(Integer.valueOf(stateType), Float.valueOf(newDuration));
                deviceStateTimeMap.put(appName, appUseMap);
            }
        }
        if (!deviceStateTimeMap.isEmpty()) {
            intent.putExtra(APP_USE_SCREEN_TIME, deviceStateTimeMap.toString());
        }
    }

    private String getTopAppName() {
        try {
            if (this.mActivityManager.getRunningTasks(1) == null || this.mActivityManager.getRunningTasks(1).get(0) == null) {
                return "";
            }
            ComponentName cn = this.mActivityManager.getRunningTasks(1).get(0).topActivity;
            String packageName = cn.getPackageName();
            return packageName;
        } catch (IndexOutOfBoundsException e) {
            Slog.e(TAG, "getTopAppName: exception:" + e.toString());
            return "";
        } catch (Exception e2) {
            Slog.e(TAG, "getTopAppName: exception:" + e2.toString());
            return "";
        }
    }

    /* loaded from: classes.dex */
    private final class OneTrackFoldStateHelperHandler extends Handler {
        public OneTrackFoldStateHelperHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    OneTrackFoldStateHelper.this.handleFoldChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 2:
                    OneTrackFoldStateHelper.this.handleWindowChanged((ForegroundInfo) msg.obj);
                    return;
                case 3:
                    OneTrackFoldStateHelper.this.handleDeviceStateChanged(msg.arg1, ((Long) msg.obj).longValue());
                    return;
                case 4:
                    OneTrackFoldStateHelper.this.debounceDeviceState(((Long) msg.obj).longValue());
                    return;
                case 5:
                    OneTrackFoldStateHelper.this.handleInteractiveChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 6:
                    OneTrackFoldStateHelper.this.handleDisplaySwapFinished(((Long) msg.obj).longValue());
                    return;
                case 7:
                    OneTrackFoldStateHelper.this.handleDisplaySwapFinished(SystemClock.uptimeMillis());
                    return;
                case 8:
                    OneTrackFoldStateHelper.this.handleFocusedWindowChanged((String) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private void handleDisplaySwapStarted() {
        if (DEBUG) {
            Slog.d(TAG, "handleDisplaySwapStarted: mInteractive: " + this.mInteractive);
        }
        if (this.mInteractive && this.mPreInteractive) {
            this.mLastFoldChangeTime = SystemClock.uptimeMillis();
            this.mLastFocusPackageName = this.mFocusPackageName;
            this.mPendingWaitForDisplaySwapFinished = true;
            this.mHandler.removeMessages(7);
            Message msg = this.mHandler.obtainMessage(7);
            this.mHandler.sendMessageDelayed(msg, TIMEOUT_PENDING_DISPLAY_SWAP_MILLIS);
        }
        this.mPreInteractive = this.mInteractive;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDisplaySwapFinished(long j) {
        if (this.mPendingWaitForDisplaySwapFinished && this.mLastFocusPackageName != null) {
            long j2 = j - this.mLastFoldChangeTime;
            if (DEBUG) {
                Slog.i(TAG, "handleDisplaySwapFinished: duration: " + j2);
            }
            Integer valueOf = Integer.valueOf(!this.mFolded ? 1 : 0);
            HashMap<Integer, List<Long>> orDefault = this.mAppFoldAvgTimeMap.getOrDefault(this.mLastFocusPackageName, new HashMap<>());
            List<Long> orDefault2 = orDefault.getOrDefault(valueOf, new ArrayList());
            orDefault2.add(Long.valueOf(j2));
            orDefault.put(valueOf, orDefault2);
            if (this.mFolded) {
                this.mFoldTimeSum += (float) j2;
                this.mFoldCountSum++;
            } else {
                this.mUnfoldTimeSum += (float) j2;
                this.mUnfoldCountSum++;
            }
            this.mAppFoldAvgTimeMap.put(this.mLastFocusPackageName, orDefault);
            this.mPendingWaitForDisplaySwapFinished = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFocusedWindowChanged(String focusPackageName) {
        if (DEBUG) {
            Slog.d(TAG, "handleFocusedWindowChanged: focusPackageName: " + focusPackageName);
        }
        if (focusPackageName != null && !focusPackageName.equals(this.mFocusPackageName)) {
            this.mFocusPackageName = focusPackageName;
        }
    }

    public void onEarlyInteractivityChange(boolean interactive) {
        this.mHandler.removeMessages(5);
        Message msg = this.mHandler.obtainMessage(5, Boolean.valueOf(interactive));
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDeviceStateChanged(int toState, long eventTime) {
        if (DEBUG) {
            Slog.d(TAG, "handleDeviceStateChanged: toState: " + toState);
        }
        this.mPendingDeviceState = toState;
        long now = SystemClock.uptimeMillis();
        this.mPendingDeviceStateDebounceTime = 1000 + now;
        debounceDeviceState(eventTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void debounceDeviceState(long eventTime) {
        if (this.mPendingDeviceState != -1 && this.mPendingDeviceStateDebounceTime >= 0) {
            long now = SystemClock.uptimeMillis();
            if (this.mPendingDeviceStateDebounceTime <= now) {
                if (this.mCurrentDeviceState != this.mPendingDeviceState) {
                    recordDeviceStateDetails(0);
                    this.mLastEventTime = now;
                    this.mPreviousDeviceState = this.mCurrentDeviceState;
                    this.mCurrentDeviceState = this.mPendingDeviceState;
                }
                this.mPendingDeviceState = -1;
                this.mPendingDeviceStateDebounceTime = -1L;
                return;
            }
            this.mHandler.removeMessages(4);
            Message msg = this.mHandler.obtainMessage(4, Long.valueOf(eventTime));
            this.mHandler.sendMessageAtTime(msg, this.mPendingDeviceStateDebounceTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInteractiveChanged(boolean interactive) {
        if (DEBUG) {
            Slog.d(TAG, "handleInteractiveChanged: interactive: " + interactive);
        }
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            if (interactive) {
                this.mForegroundAppName = getTopAppName();
                updateLastEventInfo(SystemClock.uptimeMillis(), 1);
            } else {
                if (this.mPendingWaitForDisplaySwapFinished) {
                    this.mPendingWaitForDisplaySwapFinished = false;
                    this.mHandler.removeMessages(7);
                }
                recordDeviceStateDetails(1);
            }
        }
    }

    private void updateLastEventInfo(long eventTime, int reason) {
        this.mLastEventTime = eventTime;
        this.mLastEventReason = reason;
    }

    private void recordDeviceStateDetails(int reason) {
        if ((!this.mInteractive && (reason == 0 || reason == 3)) || this.mCurrentDeviceState == -1) {
            return;
        }
        if (DEBUG) {
            Slog.i(TAG, "recordDeviceStateDetails: start tracking device state: " + this.mCurrentDeviceState + ", reason: " + reason);
        }
        HashMap<String, Long> deviceStateMaps = this.mDeviceStateUsageDetail.getOrDefault(Integer.valueOf(this.mCurrentDeviceState), new HashMap<>());
        long duration = deviceStateMaps.getOrDefault(this.mForegroundAppName, 0L).longValue();
        long now = SystemClock.uptimeMillis();
        long newDuration = (now - this.mLastEventTime) + duration;
        deviceStateMaps.put(this.mForegroundAppName, Long.valueOf(newDuration));
        this.mDeviceStateUsageDetail.put(Integer.valueOf(this.mCurrentDeviceState), deviceStateMaps);
    }

    public void notifyDeviceStateChanged(int newState) {
        long now = SystemClock.uptimeMillis();
        this.mHandler.removeMessages(3);
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = newState;
        msg.obj = Long.valueOf(now);
        this.mHandler.sendMessage(msg);
    }

    public void notifyDisplaySwapFinished() {
        this.mHandler.removeMessages(6);
        long now = SystemClock.uptimeMillis();
        Message msg = this.mHandler.obtainMessage(6, Long.valueOf(now));
        this.mHandler.sendMessage(msg);
    }

    public void notifyFocusedWindowChanged(String focusedPackageName) {
        this.mHandler.removeMessages(8);
        Message msg = this.mHandler.obtainMessage(8);
        msg.obj = focusedPackageName;
        this.mHandler.sendMessage(msg);
    }
}
