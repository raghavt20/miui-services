package com.android.server.power.statistic;

import android.R;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.os.BackgroundThread;
import com.android.server.power.MiuiAttentionDetector;
import com.android.server.power.statistic.DimEventStatistic;
import com.android.server.power.statistic.DisplayPortEventStatistic;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.util.ArrayList;
import java.util.Iterator;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiPowerStatisticTracker {
    private static final String ALL_SCREEN_ON = "all_screen_on_time";
    public static final String AON_EVENT_NAME = "aon_statistic";
    private static final String APP_ID = "31000401594";
    private static final int ARRAY_CAPACITY = 10;
    private static final String BLOCK_AVG_TIME = "block_avg_time";
    private static final boolean DEBUG;
    private static final String FAILURE_MILLIS = "failure_millis";
    private static final String FAILURE_REASONS = "failure_reasons";
    private static final String FAILURE_TIMES = "failure_times";
    private static final String FEATURE_AON_PROXIMITY_SUPPORT = "config_aon_proximity_available";
    private static final String KEY_AON_SCREEN_OFF_ENABLE = "aon_screen_off_enable";
    private static final String KEY_AON_SCREEN_ON_ENABLE = "aon_screen_on_enable";
    private static final String KEY_SCREEN_OFF_CHECK_ATTENTION_MILLIS = "screen_off_check_attention_millis";
    private static final String KEY_SCREEN_OFF_HAS_FACE_COUNT = "screen_off_has_face_count";
    private static final String KEY_SCREEN_ON_CHECK_ATTENTION_MILLIS = "screen_on_check_attention_millis";
    private static final String KEY_SCREEN_ON_HAS_FACE_COUNT = "screen_on_has_face_count";
    private static final String KEY_SCREEN_ON_NO_FACE_COUNT = "screen_on_no_face_count";
    private static final int MSG_CONNECTED_STATE_CHANGED = 8;
    private static final int MSG_DIM_STATE_CHANGE = 11;
    private static final int MSG_DUMP = 10;
    private static final int MSG_FOREGROUND_APP_CHANGED = 9;
    private static final int MSG_POWER_STATE_CHANGE = 4;
    private static final int MSG_TOF_GESTURE_CHANGE = 6;
    private static final int MSG_TOF_PROXIMITY_CHANGE = 7;
    private static final int MSG_UNBLOCK_SCREEN = 3;
    private static final int MSG_USER_ATTENTION_CHANGE = 5;
    private static final int MSG_WAKEFULNESS_CHANGE = 1;
    private static final int MSG_WAKEFULNESS_CHANGE_COMPLETE = 2;
    private static final String OFF_AVG_TIME = "off_avg_time";
    private static final String OFF_REASON = "off_reason";
    private static final String ON_AVG_TIME = "on_avg_time";
    private static final String ON_REASON = "on_reason";
    public static final String POWER_EVENT_NAME = "power_on_off_statistic";
    private static final String STATE_OFF_AVG_TIME = "state_off_avg_time";
    private static final String STATE_ON_AVG_TIME = "state_on_avg_time";
    private static final String SUCCESS_MILLIS = "success_millis";
    private static final String SUCCESS_TIMES = "success_times";
    public static final String TAG = "MiuiPowerStatisticTracker";
    private static volatile MiuiPowerStatisticTracker sInstance;
    private AlarmManager mAlarmManager;
    private long mAllScreenOnTime;
    private boolean mAonProximitySupport;
    private boolean mAonScreenOffEnabled;
    private boolean mAonScreenOnEnabled;
    private Handler mBackgroundHandler;
    private Context mContext;
    private DimEventStatistic mDimEventStatistic;
    private DisplayPortEventStatistic mDisplayPortEventStatistic;
    private long mFailureMillis;
    private int mFailureTimes;
    private long mLastWakeTime;
    private OneTrackerHelper mOneTrackerHelper;
    private float mPowerStateOffAvgTime;
    private float mPowerStateOnAvgTime;
    private float mScreenOffAvgTime;
    private long mScreenOffCheckAttentionMillis;
    private int mScreenOffHasFaceCount;
    private float mScreenOnAvgTime;
    private float mScreenOnBlockerAvgTime;
    private long mScreenOnCheckAttentionMillis;
    private int mScreenOnHasFaceCount;
    private int mScreenOnNoFaceCount;
    private long mSuccessMillis;
    private int mSuccessTimes;
    private boolean mSupportAdaptiveSleep;
    private TofEventStatistic mTofEventStatistic;
    private boolean mWakefulnessChanging;
    private final long DEBUG_REPORT_TIME_DURATION = 120000;
    private SparseArray<Long> mScreenOnReasons = new SparseArray<>();
    private SparseArray<Long> mScreenOffReasons = new SparseArray<>();
    private ArrayList<PowerEvent> mScreenOnLatencyList = new ArrayList<>(10);
    private ArrayList<PowerEvent> mScreenOffLatencyList = new ArrayList<>(10);
    private int mWakefulness = 1;
    private final SparseArray<Integer> mFailureReasons = new SparseArray<>();
    private PowerEvent mPowerEvent = new PowerEvent();
    private final AlarmManager.OnAlarmListener mOnAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.power.statistic.MiuiPowerStatisticTracker$$ExternalSyntheticLambda0
        @Override // android.app.AlarmManager.OnAlarmListener
        public final void onAlarm() {
            MiuiPowerStatisticTracker.this.lambda$new$0();
        }
    };

    static {
        DEBUG = SystemProperties.getInt("debug.miui.power.statistic.dbg", 0) != 0;
    }

    public static MiuiPowerStatisticTracker getInstance() {
        if (sInstance == null) {
            synchronized (MiuiPowerStatisticTracker.class) {
                if (sInstance == null) {
                    sInstance = new MiuiPowerStatisticTracker();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        this.mContext = context;
        this.mBackgroundHandler = new PowerStatisticHandler(BackgroundThread.getHandler().getLooper());
        this.mLastWakeTime = SystemClock.uptimeMillis();
        this.mOneTrackerHelper = new OneTrackerHelper(this.mContext);
        this.mSupportAdaptiveSleep = this.mContext.getResources().getBoolean(R.bool.config_allowStartActivityForLongPressOnPowerInSetup);
        this.mAonProximitySupport = FeatureParser.getBoolean(FEATURE_AON_PROXIMITY_SUPPORT, false);
        this.mTofEventStatistic = new TofEventStatistic(APP_ID, context, this.mOneTrackerHelper);
        this.mDisplayPortEventStatistic = new DisplayPortEventStatistic(APP_ID, context, this.mOneTrackerHelper);
        this.mDimEventStatistic = new DimEventStatistic(APP_ID, context, this.mOneTrackerHelper);
    }

    public void bootCompleted() {
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        setReportScheduleEventAlarm();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        if (DEBUG) {
            Slog.i(TAG, "It's time to report");
        }
        reportScheduleEvent();
        setReportScheduleEventAlarm();
    }

    public void notifyWakefulnessChangedLocked(int wakefulness, long eventTime, int reason) {
        PowerEvent powerEvent = new PowerEvent(wakefulness, reason, eventTime);
        Message message = this.mBackgroundHandler.obtainMessage(1);
        message.obj = powerEvent;
        this.mBackgroundHandler.sendMessage(message);
    }

    public void handleWakefulnessChanged(PowerEvent powerEvent) {
        boolean z = DEBUG;
        if (z) {
            Slog.i(TAG, "handleWakefulnessChanged. powerEvent=" + powerEvent.toString());
        }
        if (this.mWakefulnessChanging) {
            powerEvent.setQuickBreak(true);
        }
        if (powerEvent.getWakefulness() == 1) {
            this.mWakefulnessChanging = true;
            this.mLastWakeTime = powerEvent.getEventTime();
            updateScreenReasons(this.mScreenOnReasons, powerEvent.getReason());
        } else if (PowerManagerInternal.isInteractive(this.mWakefulness) && powerEvent.isScreenOff()) {
            this.mWakefulnessChanging = true;
            this.mAllScreenOnTime += SystemClock.uptimeMillis() - this.mLastWakeTime;
            if (z) {
                Slog.i(TAG, "update all screen time on to " + this.mAllScreenOnTime);
            }
            updateScreenReasons(this.mScreenOffReasons, powerEvent.getReason());
        } else {
            if (z) {
                Slog.i(TAG, "skip: old wakefulness=" + this.mWakefulness + ", new wakefulness=" + powerEvent.getWakefulness());
                return;
            }
            return;
        }
        if (this.mWakefulnessChanging) {
            this.mPowerEvent.copyFrom(powerEvent);
            int wakefulness = powerEvent.getWakefulness();
            this.mWakefulness = wakefulness;
            this.mDisplayPortEventStatistic.handleInteractiveChanged(PowerManagerInternal.isInteractive(wakefulness));
        }
    }

    public void notifyScreenOnUnBlocker(int screenOnBlockTime, int delayMiles) {
        Message message = this.mBackgroundHandler.obtainMessage(3);
        message.arg1 = screenOnBlockTime;
        message.arg2 = delayMiles;
        this.mBackgroundHandler.sendMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOnUnBlocker(int screenOnBlockTime, int delayMiles) {
        if (!this.mWakefulnessChanging || !this.mPowerEvent.isScreenOn()) {
            return;
        }
        if (DEBUG) {
            Slog.i(TAG, "handleScreenOnUnBlocker: screenOnBlockStartRealTime=" + screenOnBlockTime + ", delayMiles=" + delayMiles);
        }
        this.mPowerEvent.setDelayByFace(delayMiles > 0);
        this.mPowerEvent.setBlockedScreenLatency(screenOnBlockTime);
    }

    public void notifyDisplayStateChangedLatencyLocked(int latencyMs) {
        Message message = this.mBackgroundHandler.obtainMessage(4);
        message.arg1 = latencyMs;
        this.mBackgroundHandler.sendMessage(message);
    }

    public void handleDisplayStateChangedLatencyLocked(int latencyMs) {
        if (!this.mWakefulnessChanging) {
            return;
        }
        this.mPowerEvent.setDisplayStateChangedLatency(latencyMs);
    }

    public void notifyWakefulnessCompletedLocked(long wakefulnessCompletedTime) {
        Message message = this.mBackgroundHandler.obtainMessage(2);
        message.obj = new Long(wakefulnessCompletedTime);
        this.mBackgroundHandler.sendMessage(message);
    }

    public void handleWakefulnessCompleted(long wakefulnessCompletedTime) {
        if (!this.mWakefulnessChanging) {
            return;
        }
        int interactiveChangeLatency = (int) (wakefulnessCompletedTime - this.mPowerEvent.getEventTime());
        this.mPowerEvent.setWakefulnessChangedLatency(interactiveChangeLatency);
        this.mWakefulnessChanging = false;
        if (DEBUG) {
            Slog.i(TAG, "handleWakefulnessCompleted: " + this.mPowerEvent.toString());
        }
        if (this.mPowerEvent.isValid()) {
            if (this.mPowerEvent.isScreenOn()) {
                collectLatencyInfo(this.mScreenOnLatencyList, this.mPowerEvent);
            } else if (this.mPowerEvent.isScreenOff()) {
                collectLatencyInfo(this.mScreenOffLatencyList, this.mPowerEvent);
            }
        }
        this.mPowerEvent.clear();
    }

    public void notifyUserAttentionChanged(int latencyMs, int result) {
        Message message = this.mBackgroundHandler.obtainMessage(5);
        message.arg1 = latencyMs;
        message.arg2 = result;
        this.mBackgroundHandler.sendMessage(message);
    }

    public void handleUserAttentionChanged(int latencyMs, int result) {
        if (DEBUG) {
            Slog.i(TAG, "handleUserAttentionChanged, attention used:" + latencyMs + ", result:" + result);
        }
        if (result != 1 && result != 0) {
            this.mFailureTimes++;
            this.mFailureMillis += latencyMs;
            int curNum = this.mFailureReasons.get(result, 0).intValue();
            this.mFailureReasons.put(result, Integer.valueOf(curNum + 1));
            return;
        }
        int curNum2 = this.mSuccessTimes;
        this.mSuccessTimes = curNum2 + 1;
        this.mSuccessMillis += latencyMs;
    }

    /* loaded from: classes.dex */
    private class PowerStatisticHandler extends Handler {
        public PowerStatisticHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiPowerStatisticTracker.this.handleWakefulnessChanged((PowerEvent) msg.obj);
                    return;
                case 2:
                    MiuiPowerStatisticTracker.this.handleWakefulnessCompleted(((Long) msg.obj).longValue());
                    return;
                case 3:
                    MiuiPowerStatisticTracker.this.handleScreenOnUnBlocker(msg.arg1, msg.arg2);
                    return;
                case 4:
                    MiuiPowerStatisticTracker.this.handleDisplayStateChangedLatencyLocked(msg.arg1);
                    return;
                case 5:
                    MiuiPowerStatisticTracker.this.handleUserAttentionChanged(msg.arg1, msg.arg2);
                    return;
                case 6:
                    MiuiPowerStatisticTracker.this.mTofEventStatistic.notifyGestureEvent((String) msg.obj, msg.arg1 == 1, msg.arg2);
                    return;
                case 7:
                    MiuiPowerStatisticTracker.this.mTofEventStatistic.notifyTofPowerState(((Boolean) msg.obj).booleanValue());
                    return;
                case 8:
                    MiuiPowerStatisticTracker.this.mDisplayPortEventStatistic.handleConnectedStateChanged((DisplayPortEventStatistic.DisplayPortEvent) msg.obj);
                    return;
                case 9:
                    MiuiPowerStatisticTracker.this.mDisplayPortEventStatistic.handleForegroundAppChanged((String) msg.obj);
                    return;
                case 10:
                    MiuiPowerStatisticTracker.this.dumpLocal();
                    MiuiPowerStatisticTracker.this.mTofEventStatistic.dumpLocal();
                    return;
                case 11:
                    MiuiPowerStatisticTracker.this.mDimEventStatistic.notifyDimStateChanged((DimEventStatistic.DimEvent) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private void collectLatencyInfo(ArrayList<PowerEvent> list, PowerEvent powerEvent) {
        if (list.size() > 10) {
            recalculateLatencyInfo(list);
        }
        if (powerEvent.isValid()) {
            list.add(new PowerEvent(powerEvent));
        }
    }

    private void recalculateLatencyInfo(ArrayList<PowerEvent> latencyInfoList) {
        if (latencyInfoList.isEmpty()) {
            return;
        }
        float sumWakefulnessChangedLatency = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        float sumBlockedScreenLatency = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        float sumDisplayStateLatency = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        int wakefulnessChangedSize = 0;
        int blockedScreenSize = 0;
        int DisplayStateChangedSize = 0;
        Iterator<PowerEvent> it = latencyInfoList.iterator();
        while (it.hasNext()) {
            PowerEvent powerEvent = it.next();
            if (powerEvent.getWakefulnessChangedLatency() > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                sumWakefulnessChangedLatency += powerEvent.getWakefulnessChangedLatency();
                wakefulnessChangedSize++;
            }
            if (powerEvent.getBlockedScreenLatency() > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                sumBlockedScreenLatency += powerEvent.getBlockedScreenLatency();
                blockedScreenSize++;
            }
            if (powerEvent.getDisplayStateChangedLatency() > 0) {
                sumDisplayStateLatency += powerEvent.getDisplayStateChangedLatency();
                DisplayStateChangedSize++;
            }
        }
        if (latencyInfoList.get(0).isScreenOn()) {
            this.mScreenOnAvgTime = updateAvgTime(this.mScreenOnAvgTime, sumWakefulnessChangedLatency, wakefulnessChangedSize);
            this.mScreenOnBlockerAvgTime = updateAvgTime(this.mScreenOnBlockerAvgTime, sumBlockedScreenLatency, blockedScreenSize);
            this.mPowerStateOnAvgTime = updateAvgTime(this.mPowerStateOnAvgTime, sumDisplayStateLatency, DisplayStateChangedSize);
        } else if (latencyInfoList.get(0).isScreenOff()) {
            this.mScreenOffAvgTime = updateAvgTime(this.mScreenOffAvgTime, sumWakefulnessChangedLatency, wakefulnessChangedSize);
            this.mPowerStateOffAvgTime = updateAvgTime(this.mPowerStateOffAvgTime, sumDisplayStateLatency, DisplayStateChangedSize);
        }
        latencyInfoList.clear();
    }

    private void setReportScheduleEventAlarm() {
        long now = SystemClock.elapsedRealtime();
        boolean z = DEBUG;
        long duration = z ? 120000L : 86400000L;
        long nextTime = now + duration;
        if (z) {
            Slog.d(TAG, "setReportScheduleEventAlarm: next time: " + TimeUtils.formatDuration(duration));
        }
        AlarmManager alarmManager = this.mAlarmManager;
        if (alarmManager != null) {
            alarmManager.setExact(2, nextTime, "report_power_statistic", this.mOnAlarmListener, this.mBackgroundHandler);
        }
    }

    private void reportScheduleEvent() {
        reportScreenOnOffEventByIntent();
        reportAttentionEventByIntent();
        reportAonScreenOnOffEventByIntent();
        this.mTofEventStatistic.reportTofEventByIntent();
        this.mDisplayPortEventStatistic.reportDisplayPortEventByIntent();
        this.mDimEventStatistic.reportDimStateByIntent();
    }

    private void reportAonScreenOnOffEventByIntent() {
        if (!MiuiAttentionDetector.AON_SCREEN_OFF_SUPPORTED && !MiuiAttentionDetector.AON_SCREEN_ON_SUPPORTED) {
            return;
        }
        this.mAonScreenOnEnabled = MiuiAttentionDetector.AON_SCREEN_ON_SUPPORTED && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_people_near_screen_on", 0, -2) != 0;
        this.mAonScreenOffEnabled = MiuiAttentionDetector.AON_SCREEN_OFF_SUPPORTED && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gaze_lock_screen_setting", 0, -2) != 0;
        Intent intent = this.mOneTrackerHelper.getTrackEventIntent(APP_ID, AON_EVENT_NAME, this.mContext.getPackageName());
        intent.putExtra(KEY_AON_SCREEN_ON_ENABLE, this.mAonScreenOnEnabled).putExtra(KEY_AON_SCREEN_OFF_ENABLE, this.mAonScreenOffEnabled);
        if (this.mAonScreenOffEnabled) {
            intent.putExtra(KEY_SCREEN_ON_NO_FACE_COUNT, this.mScreenOnNoFaceCount).putExtra(KEY_SCREEN_ON_HAS_FACE_COUNT, this.mScreenOnHasFaceCount).putExtra(KEY_SCREEN_ON_CHECK_ATTENTION_MILLIS, this.mScreenOnCheckAttentionMillis);
        }
        if (this.mAonScreenOnEnabled) {
            intent.putExtra(KEY_SCREEN_OFF_HAS_FACE_COUNT, this.mScreenOffHasFaceCount).putExtra(KEY_SCREEN_OFF_CHECK_ATTENTION_MILLIS, this.mScreenOffCheckAttentionMillis);
        }
        Slog.d(TAG, "reportAonScreenOnOffEventByIntent: intent:" + intent.getExtras());
        try {
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportAttentionEvent fail! " + e);
        }
        resetAonScreenOnOffData();
    }

    private void resetAonScreenOnOffData() {
        this.mAonScreenOnEnabled = false;
        this.mAonScreenOffEnabled = false;
        this.mScreenOnNoFaceCount = 0;
        this.mScreenOnHasFaceCount = 0;
        this.mScreenOnCheckAttentionMillis = 0L;
        this.mScreenOffCheckAttentionMillis = 0L;
        this.mScreenOffHasFaceCount = 0;
    }

    public void notifyAonScreenOnOffEvent(final boolean isScreenOn, final boolean hasFace, final long checkTime) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.power.statistic.MiuiPowerStatisticTracker$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiPowerStatisticTracker.this.lambda$notifyAonScreenOnOffEvent$1(isScreenOn, checkTime, hasFace);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyAonScreenOnOffEvent$1(boolean isScreenOn, long checkTime, boolean hasFace) {
        if (isScreenOn) {
            this.mScreenOnCheckAttentionMillis += checkTime;
            if (hasFace) {
                this.mScreenOnHasFaceCount++;
                return;
            } else {
                this.mScreenOnNoFaceCount++;
                return;
            }
        }
        this.mScreenOffCheckAttentionMillis += checkTime;
        this.mScreenOffHasFaceCount++;
    }

    public void notifyGestureEvent(String str, boolean z, int i) {
        Message obtainMessage = this.mBackgroundHandler.obtainMessage(6, str);
        obtainMessage.arg1 = z ? 1 : 0;
        obtainMessage.arg2 = i;
        this.mBackgroundHandler.sendMessage(obtainMessage);
    }

    public void notifyTofPowerState(boolean wakeup) {
        Message message = this.mBackgroundHandler.obtainMessage(7, new Boolean(wakeup));
        this.mBackgroundHandler.sendMessage(message);
    }

    public void notifyDisplayPortConnectStateChanged(long physicalDisplayId, boolean isConnected, String productName, int frameRate, String resolution) {
        Message message = this.mBackgroundHandler.obtainMessage(8);
        message.obj = new DisplayPortEventStatistic.DisplayPortEvent(physicalDisplayId, isConnected, productName, frameRate, resolution);
        this.mBackgroundHandler.sendMessage(message);
    }

    public void notifyForegroundAppChanged(String foregroundAppName) {
        Message message = this.mBackgroundHandler.obtainMessage(9, foregroundAppName);
        this.mBackgroundHandler.sendMessage(message);
    }

    public void notifyDimStateChanged(long now, boolean isEnterDim) {
        Message message = this.mBackgroundHandler.obtainMessage(11, new DimEventStatistic.DimEvent(now, isEnterDim));
        this.mBackgroundHandler.sendMessage(message);
    }

    private void reportScreenOnOffEventByIntent() {
        recalculateLatencyInfo(this.mScreenOnLatencyList);
        recalculateLatencyInfo(this.mScreenOffLatencyList);
        try {
            Intent intent = this.mOneTrackerHelper.getTrackEventIntent(APP_ID, POWER_EVENT_NAME, this.mContext.getPackageName());
            intent.putExtra(ON_REASON, this.mScreenOnReasons.toString()).putExtra(OFF_REASON, this.mScreenOffReasons.toString()).putExtra(ON_AVG_TIME, this.mScreenOnAvgTime).putExtra(OFF_AVG_TIME, this.mScreenOffAvgTime).putExtra(BLOCK_AVG_TIME, this.mScreenOnBlockerAvgTime).putExtra(STATE_ON_AVG_TIME, this.mPowerStateOnAvgTime).putExtra(STATE_OFF_AVG_TIME, this.mPowerStateOffAvgTime).putExtra(ALL_SCREEN_ON, this.mAllScreenOnTime);
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportScreenOnOffEventByIntent fail! " + e);
        }
        resetRecordInfo();
    }

    private void resetRecordInfo() {
        this.mScreenOnReasons.clear();
        this.mScreenOffReasons.clear();
        this.mScreenOnLatencyList.clear();
        this.mScreenOffLatencyList.clear();
        resetAvgTime();
    }

    private void resetAvgTime() {
        this.mScreenOnAvgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mScreenOffAvgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mScreenOnBlockerAvgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mPowerStateOnAvgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mPowerStateOffAvgTime = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mAllScreenOnTime = 0L;
    }

    private float updateAvgTime(float curAvg, float newSum, int size) {
        return (newSum + curAvg) / (size + 1);
    }

    private void updateScreenReasons(SparseArray<Long> array, int reason) {
        Long curNum = array.get(reason, 0L);
        array.put(reason, Long.valueOf(curNum.longValue() + 1));
    }

    private void reportAttentionEventByIntent() {
        if (!this.mSupportAdaptiveSleep) {
            return;
        }
        try {
            Intent intent = this.mOneTrackerHelper.getTrackEventIntent(APP_ID, AON_EVENT_NAME, this.mContext.getPackageName());
            intent.putExtra(SUCCESS_TIMES, this.mSuccessTimes).putExtra(FAILURE_TIMES, this.mFailureTimes).putExtra(SUCCESS_MILLIS, this.mSuccessMillis).putExtra(FAILURE_MILLIS, this.mFailureMillis).putExtra(FAILURE_REASONS, this.mFailureReasons.toString());
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportAttentionEvent fail! " + e);
        }
        resetAttentionData();
    }

    private void resetAttentionData() {
        this.mSuccessTimes = 0;
        this.mFailureTimes = 0;
        this.mSuccessMillis = 0L;
        this.mFailureMillis = 0L;
        this.mFailureReasons.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerEvent {
        int mBlockedScreenLatency;
        int mDisplayStateChangedLatency;
        long mEventTime;
        boolean mIsDelayByFace;
        boolean mIsQuickBreak;
        int mReason;
        int mWakefulness;
        int mWakefulnessChangedLatency;

        PowerEvent() {
        }

        PowerEvent(PowerEvent powerEvent) {
            copyFrom(powerEvent);
        }

        PowerEvent(int wakefulness, int reason, long eventTime) {
            this.mWakefulness = wakefulness;
            this.mReason = reason;
            this.mEventTime = eventTime;
        }

        public boolean isValid() {
            return (this.mIsDelayByFace || this.mWakefulnessChangedLatency <= 0 || this.mIsQuickBreak) ? false : true;
        }

        public boolean isScreenOn() {
            return this.mWakefulness == 1;
        }

        public boolean isScreenOff() {
            int i = this.mWakefulness;
            return i == 0 || i == 3;
        }

        public float getWakefulnessChangedLatency() {
            return this.mWakefulnessChangedLatency;
        }

        public void setWakefulnessChangedLatency(int latency) {
            this.mWakefulnessChangedLatency = latency;
        }

        public void setWakefulness(int wakefulness) {
            this.mWakefulness = wakefulness;
        }

        public int getWakefulness() {
            return this.mWakefulness;
        }

        public long getEventTime() {
            return this.mEventTime;
        }

        public void setEventTime(long eventTime) {
            this.mEventTime = eventTime;
        }

        public int getReason() {
            return this.mReason;
        }

        public void setReason(int reason) {
            this.mReason = reason;
        }

        public boolean isQuickBreak() {
            return this.mIsQuickBreak;
        }

        public void setQuickBreak(boolean isQuickBreak) {
            this.mIsQuickBreak = isQuickBreak;
        }

        public float getBlockedScreenLatency() {
            return this.mBlockedScreenLatency;
        }

        public void setBlockedScreenLatency(int latency) {
            this.mBlockedScreenLatency = latency;
        }

        public void setDisplayStateChangedLatency(int latency) {
            this.mDisplayStateChangedLatency = latency;
        }

        public int getDisplayStateChangedLatency() {
            return this.mDisplayStateChangedLatency;
        }

        public boolean isDelayByFace() {
            return this.mIsDelayByFace;
        }

        public void setDelayByFace(boolean delayByFace) {
            this.mIsDelayByFace = delayByFace;
        }

        public void clear() {
            this.mWakefulness = -1;
            this.mWakefulnessChangedLatency = 0;
            this.mBlockedScreenLatency = 0;
            this.mDisplayStateChangedLatency = 0;
            this.mIsDelayByFace = false;
            this.mIsQuickBreak = false;
            this.mEventTime = 0L;
            this.mReason = -1;
        }

        public void copyFrom(PowerEvent other) {
            this.mWakefulness = other.mWakefulness;
            this.mWakefulnessChangedLatency = other.mWakefulnessChangedLatency;
            this.mBlockedScreenLatency = other.mBlockedScreenLatency;
            this.mDisplayStateChangedLatency = other.mDisplayStateChangedLatency;
            this.mIsDelayByFace = other.mIsDelayByFace;
            this.mIsQuickBreak = other.mIsQuickBreak;
            this.mEventTime = other.mEventTime;
            this.mReason = other.mReason;
        }

        public String toString() {
            return "powerEvent:[wakefulness=" + this.mWakefulness + "; blockedScreen=" + this.mBlockedScreenLatency + "; powerStateChanged=" + this.mDisplayStateChangedLatency + "; wakefulnessChanged=" + this.mWakefulnessChangedLatency + "; isDelayByFace=" + this.mIsDelayByFace + "; isQuickBreak=" + this.mIsQuickBreak + "; reason=" + this.mReason + "; eventTime=" + this.mEventTime + "]";
        }
    }

    public void dump() {
        if (DEBUG) {
            this.mBackgroundHandler.sendEmptyMessage(10);
        }
    }

    public void dumpLocal() {
        if (DEBUG) {
            StringBuffer stringBuffer = new StringBuffer("MiuiPowerStaticTracker:");
            stringBuffer.append("\nonReasons:" + this.mScreenOnReasons);
            stringBuffer.append("\noffReasons:" + this.mScreenOffReasons);
            stringBuffer.append("\nscreenOnList:" + this.mScreenOnLatencyList);
            stringBuffer.append("\nscreenOffList:" + this.mScreenOffLatencyList);
            stringBuffer.append("\nscreenOnAvgTime:" + this.mScreenOnAvgTime);
            stringBuffer.append("\nscreenOffAvgTime:" + this.mScreenOffAvgTime);
            stringBuffer.append("\nscreenOnBlockerAvgTime:" + this.mScreenOnBlockerAvgTime);
            stringBuffer.append("\npowerStateOnAvgTime:" + this.mPowerStateOnAvgTime);
            stringBuffer.append("\npowerStateOffAvgTime:" + this.mPowerStateOffAvgTime);
            stringBuffer.append("\nallScreenOnTime:" + this.mAllScreenOnTime);
            Slog.i(TAG, stringBuffer.toString());
        }
    }
}
