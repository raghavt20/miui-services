package com.android.server.display.statistics;

import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Spline;
import android.util.TimeUtils;
import android.view.IRotationWatcher;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.display.AutomaticBrightnessControllerImpl;
import com.android.server.display.BrightnessMappingStrategy;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.display.ThermalBrightnessController;
import com.android.server.display.aiautobrt.AppClassifier;
import com.android.server.display.aiautobrt.IndividualEventNormalizer;
import com.android.server.display.statistics.AggregationEvent;
import com.android.server.display.statistics.BrightnessEvent;
import com.android.server.display.thermalbrightnesscondition.config.TemperatureBrightnessPair;
import com.android.server.display.thermalbrightnesscondition.config.ThermalConditionItem;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowManagerService;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.aiautobrt.IndividualModelEvent;
import com.xiaomi.aiautobrt.IndividualTrainEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class BrightnessDataProcessor implements MiuiDisplayCloudController.CloudListener, ThermalBrightnessController.ThermalListener {
    private static final long AON_FLARE_EFFECTIVE_TIME = 60000;
    private static final int DEBOUNCE_REPORT_BRIGHTNESS = 3000;
    private static final long DEBOUNCE_REPORT_DISABLE_AUTO_BRIGHTNESS_EVENT = 300000;
    private static final boolean DEBUG;
    private static final long DEBUG_REPORT_TIME_DURATION = 120000;
    private static final String DISABLE_SECURITY_BY_MI_SHOW = "disable_security_by_mishow";
    private static final int FACTOR_DARK_MODE = 4;
    private static final int FACTOR_DC_MODE = 2;
    private static final int FACTOR_READING_MODE = 1;
    private static final int LUX_SPLIT_HIGH = 3500;
    private static final int LUX_SPLIT_LOW = 30;
    private static final int LUX_SPLIT_MEDIUM = 900;
    private static final int LUX_SPLIT_SUPREME_FIVE = 100000;
    private static final int LUX_SPLIT_SUPREME_FOUR = 65000;
    private static final int LUX_SPLIT_SUPREME_ONE = 10000;
    private static final int LUX_SPLIT_SUPREME_THREE = 35000;
    private static final int LUX_SPLIT_SUPREME_TWO = 15000;
    private static final int MAX_NIT_SPAN = 42;
    private static final int MAX_SPAN_INDEX = 44;
    private static final int MSG_BRIGHTNESS_REPORT_DEBOUNCE = 3;
    private static final int MSG_HANDLE_EVENT_CHANGED = 1;
    private static final int MSG_REPORT_DISABLE_AUTO_BRIGHTNESS_EVENT = 13;
    private static final int MSG_RESET_BRIGHTNESS_ANIMATION_INFO = 12;
    private static final int MSG_RESET_FLARE_NOT_SUPPRESS_DARKEN_STATUS = 15;
    private static final int MSG_RESET_FLARE_SUPPRESS_DARKEN_STATUS = 14;
    private static final int MSG_ROTATION_CHANGED = 6;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_DURATION = 10;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_INFO = 8;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_TARGET = 9;
    private static final int MSG_UPDATE_BRIGHTNESS_STATISTICS_DATA = 7;
    private static final int MSG_UPDATE_FOREGROUND_APP = 5;
    private static final int MSG_UPDATE_MOTION_EVENT = 2;
    private static final int MSG_UPDATE_SCREEN_STATE = 4;
    private static final int MSG_UPDATE_TEMPORARY_BRIGHTNESS_TIME_STAMP = 11;
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_REPORT = "report";
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_SCREEN_OFF = "screen off";
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_SPAN_CHANGE = "brightness span change";
    private static final float SCREEN_NIT_SPAN_FIVE = 500.0f;
    private static final float SCREEN_NIT_SPAN_FOUR = 80.0f;
    private static final float SCREEN_NIT_SPAN_ONE = 3.5f;
    private static final float SCREEN_NIT_SPAN_SIX = 1000.0f;
    private static final float SCREEN_NIT_SPAN_THREE = 50.0f;
    private static final float SCREEN_NIT_SPAN_TWO = 8.0f;
    private static final int SENSOR_TYPE_LIGHT_FOV = 33172111;
    private static final int SPAN_LUX_STEP_HIGH = 200;
    private static final int SPAN_LUX_STEP_LOW = 5;
    private static final int SPAN_LUX_STEP_MEDIUM = 100;
    private static final int SPAN_LUX_STEP_SUPREME = 500;
    private static final float SPAN_SCREEN_NIT_STEP_FOUR = 50.0f;
    private static final float SPAN_SCREEN_NIT_STEP_ONE = 7.0f;
    private static final float SPAN_SCREEN_NIT_STEP_THREE = 20.0f;
    private static final float SPAN_SCREEN_NIT_STEP_TWO = 10.0f;
    protected static final String TAG = "BrightnessDataProcessor";
    private static final int THERMAL_STATUS_INVALID_THERMAL_UNRESTRICTED_BRIGHTNESS = 1;
    private static final int THERMAL_STATUS_VALID_THERMAL_RESTRICTED_BRIGHTNESS = 3;
    private static final int THERMAL_STATUS_VALID_THERMAL_UNRESTRICTED_BRIGHTNESS = 2;
    public static final int TYPE_IN_FLARE_SCENE = 1;
    public static final int TYPE_NOT_IN_FLARE_SCENE = 2;
    public static final int TYPE_NOT_REPORT_IN_TIME = 3;
    private Sensor mAccSensor;
    private boolean mAccSensorEnabled;
    private final IActivityTaskManager mActivityTaskManager;
    private float mActualNit;
    private final AlarmManager mAlarmManager;
    private final AppClassifier mAppClassifier;
    private float mAutoBrightnessDuration;
    private boolean mAutoBrightnessEnable;
    private float mAutoBrightnessIntegral;
    private boolean mAutoBrightnessModeChanged;
    private final Handler mBackgroundHandler;
    private Spline mBrighteningSceneSpline;
    private boolean mBrightnessAnimStart;
    private long mBrightnessAnimStartTime;
    private BrightnessMappingStrategy mBrightnessMapper;
    private final float mBrightnessMin;
    private AutomaticBrightnessControllerImpl.CloudControllerListener mCloudControllerListener;
    private ContentResolver mContentResolver;
    private final Context mContext;
    private float mCurrentBrightnessAnimValue;
    private Spline mDarkeningSceneSpline;
    private Spline mDefaultSceneSpline;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private int mExpId;
    private boolean mForcedReportTrainDataEnabled;
    private String mForegroundAppPackageName;
    private float mGrayScale;
    private boolean mHaveValidMotionForWindowBrightness;
    private boolean mHaveValidWindowBrightness;
    private final float mHbmMinLux;
    private String mHdrAppPackageName;
    public final IndividualEventNormalizer mIndividualEventNormalizer;
    private boolean mIsAonSuppressDarken;
    private boolean mIsHdrLayer;
    private boolean mIsMiShow;
    private boolean mIsNotAonSuppressDarken;
    private boolean mIsTemporaryBrightnessAdjustment;
    private boolean mIsValidResetAutoBrightnessMode;
    private boolean mLastAutoBrightnessEnable;
    private BrightnessChangeItem mLastBrightnessChangeItem;
    private float mLastBrightnessOverrideFromWindow;
    private float mLastBrightnessRestrictedTimeStamp;
    private int mLastConditionId;
    private long mLastDetailThermalUsageTimeStamp;
    private long mLastHdrEnableTimeStamp;
    private long mLastOutDoorHighTemTimeStamp;
    private boolean mLastOutDoorHighTempState;
    private long mLastResetBrightnessModeTime;
    private float mLastRestrictedBrightness;
    private float mLastScreenBrightness;
    private long mLastScreenOnTimeStamp;
    private float mLastTemperature;
    private long mLastThermalStatusTimeStamp;
    private float mLastUnrestrictedBrightness;
    private long mLatestDraggingChangedTime;
    private Sensor mLightFovSensor;
    private boolean mLightFovSensorEnabled;
    private Sensor mLightSensor;
    private boolean mLightSensorEnabled;
    private float mManualBrightnessDuration;
    private float mManualBrightnessIntegral;
    private float mMinOutDoorHighTempNit;
    private ModelEventCallback mModelEventCallback;
    private final float mNormalBrightnessMax;
    private Spline mOriSpline;
    private int mOrientation;
    private float mOriginalNit;
    private boolean mPendingAnimationStart;
    private BrightnessChangeItem mPendingBrightnessChangeItem;
    private float mPendingTargetBrightnessAnimValue;
    private WindowOverlayEventListener mPointerEventListener;
    private boolean mPointerEventListenerEnabled;
    private final PowerManager mPowerManager;
    private volatile boolean mReportBrightnessEventsEnable;
    private boolean mRotationListenerEnabled;
    private RotationWatcher mRotationWatcher;
    private boolean mScreenOn;
    private SensorListener mSensorListener;
    private final SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private long mStartTimeStamp;
    private SwitchStatsHelper mSwitchStatsHelper;
    private float mTargetBrightnessAnimValue;
    private TaskStackListenerImpl mTaskStackListener;
    private long mTemporaryBrightnessTimeStamp;
    private int mThermalStatus;
    private boolean mUserDragging;
    private boolean mWindowOverrideBrightnessChanging;
    private final WindowManagerService mWms;
    private int mCurrentUserId = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
    private float mLastStoreBrightness = Float.NaN;
    private float mLastAutoBrightness = Float.NaN;
    private float mLastManualBrightness = Float.NaN;
    private int mBrightnessChangedState = -1;
    private int mPendingBrightnessChangedState = -1;
    private float mLongTermModelSplineError = -1.0f;
    private float mDefaultSplineError = -1.0f;
    private Map<String, AggregationEvent> mBrightnessEventsMap = new HashMap();
    private Map<String, AggregationEvent> mAdvancedBrightnessEventsMap = new HashMap();
    private Map<String, AggregationEvent> mThermalEventsMap = new HashMap();
    private final SparseArray<SparseArray<Float>> mDetailThermalUnrestrictedUsage = new SparseArray<>();
    private final SparseArray<SparseArray<Float>> mDetailThermalRestrictedUsage = new SparseArray<>();
    private final List<Float> mTemperatureSpan = new ArrayList();
    private final SparseArray<SparseArray<Float>> mThermalBrightnessRestrictedUsage = new SparseArray<>();
    private float mLastAmbientLux = -1.0f;
    private float mLastMainAmbientLux = -1.0f;
    private float mLastAssistAmbientLux = -1.0f;
    private final IForegroundWindowListener mForegroundWindowListener = new IForegroundWindowListener.Stub() { // from class: com.android.server.display.statistics.BrightnessDataProcessor.1
        public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
            BrightnessDataProcessor.this.mBackgroundHandler.removeMessages(5);
            BrightnessDataProcessor.this.mBackgroundHandler.sendEmptyMessage(5);
        }
    };
    private Map<String, AggregationEvent> mCbmEventsMap = new HashMap();
    private List<Map<String, Object>> mModelTrainIndicatorsList = new ArrayList();
    private Map<String, AggregationEvent> mAonFlareEventsMap = new HashMap();
    private final AlarmManager.OnAlarmListener mOnAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda5
        @Override // android.app.AlarmManager.OnAlarmListener
        public final void onAlarm() {
            BrightnessDataProcessor.this.lambda$new$0();
        }
    };
    private final WindowManagerPolicy mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    /* loaded from: classes.dex */
    public interface ModelEventCallback {
        void onBrightnessModelEvent(IndividualModelEvent individualModelEvent);
    }

    static {
        DEBUG = SystemProperties.getInt("debug.miui.display.dgb", 0) != 0;
    }

    public BrightnessDataProcessor(Context context, DisplayDeviceConfig displayDeviceConfig, float min, float normalMax, float hbmMinLux) {
        this.mContext = context;
        this.mDisplayDeviceConfig = displayDeviceConfig;
        this.mBrightnessMin = min;
        this.mNormalBrightnessMax = normalMax;
        this.mHbmMinLux = hbmMinLux;
        BrightnessChangeHandler brightnessChangeHandler = new BrightnessChangeHandler(BackgroundThread.getHandler().getLooper());
        this.mBackgroundHandler = brightnessChangeHandler;
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mWms = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mAppClassifier = AppClassifier.getInstance();
        this.mIndividualEventNormalizer = new IndividualEventNormalizer(this.mDisplayDeviceConfig.getNitsFromBacklight(min), this.mDisplayDeviceConfig.getNitsFromBacklight(normalMax), MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, hbmMinLux - 1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, getBrightnessSpanByNit(normalMax), MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, getAmbientLuxSpanIndex(hbmMinLux - 1.0f));
        brightnessChangeHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.start();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void start() {
        this.mPointerEventListener = new WindowOverlayEventListener();
        this.mSwitchStatsHelper = SwitchStatsHelper.getInstance(this.mContext);
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mRotationWatcher = new RotationWatcher();
        this.mMinOutDoorHighTempNit = this.mContext.getResources().getFloat(285671477);
        this.mStartTimeStamp = SystemClock.elapsedRealtime();
        setRotationListener(true);
        setPointerEventListener(true);
        registerForegroundAppUpdater();
        registerScreenStateReceiver();
        setReportScheduleEventAlarm(true);
        this.mSettingsObserver = new SettingsObserver(this.mBackgroundHandler);
        this.mContentResolver = this.mContext.getContentResolver();
        registerSettingsObserver();
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        this.mAccSensor = this.mSensorManager.getDefaultSensor(1);
        this.mLightFovSensor = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_LIGHT_FOV);
        this.mSensorListener = new SensorListener();
    }

    private void setReportScheduleEventAlarm(boolean init) {
        long duration;
        long now = SystemClock.elapsedRealtime();
        boolean z = DEBUG;
        if (z) {
            duration = DEBUG_REPORT_TIME_DURATION;
        } else {
            duration = init ? 43200000L : 86400000L;
        }
        long nextTime = now + duration;
        if (z) {
            Slog.d(TAG, "setReportSwitchStatAlarm: next time: " + TimeUtils.formatDuration(duration));
        }
        this.mAlarmManager.setExact(2, nextTime, "report_switch_stats", this.mOnAlarmListener, this.mBackgroundHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        reportScheduleEvent();
        setReportScheduleEventAlarm(false);
    }

    private void reportScheduleEvent() {
        reportAggregatedBrightnessEvents();
        reportAdvancedBrightnessEvents();
        reportThermalEvents();
        reportCustomBrightnessEvents();
        reportAonFlareEvents();
    }

    public void notifyBrightnessEventIfNeeded(boolean screenOn, float originalBrightness, float actualBrightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, float ambientLux, float userDataPoint, boolean defaultConfig, boolean sunlightActive, boolean isHdrLayer, boolean isDimmingChanged, float mainFastAmbientLux, float assistFastAmbientLux, int sceneState) {
        float ambientLux2;
        int type = getBrightnessType(userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, sunlightActive);
        if (type != 2 && type != 1) {
            ambientLux2 = Float.NaN;
        } else {
            ambientLux2 = ambientLux;
        }
        float originalNit = BigDecimal.valueOf(this.mDisplayDeviceConfig.getNitsFromBacklight(clampBrightness(originalBrightness))).setScale(2, RoundingMode.HALF_UP).floatValue();
        float actualNit = BigDecimal.valueOf(this.mDisplayDeviceConfig.getNitsFromBacklight(clampBrightness(actualBrightness))).setScale(2, RoundingMode.HALF_UP).floatValue();
        scheduleUpdateBrightnessStatisticsData(screenOn, actualBrightness);
        startHdyUsageStats(isHdrLayer);
        updateScreenNits(originalNit, actualNit);
        BrightnessChangeItem brightnessChangeItem = this.mLastBrightnessChangeItem;
        if ((brightnessChangeItem == null || !BrightnessSynchronizer.floatEquals(brightnessChangeItem.originalBrightness, originalBrightness)) && !isDimmingChanged && !this.mPolicy.isKeyguardShowingAndNotOccluded() && !this.mPolicy.isKeyguardShowing() && screenOn) {
            updateInterruptBrightnessAnimDurationIfNeeded(type, actualBrightness);
            BrightnessChangeItem item = new BrightnessChangeItem(originalBrightness, actualBrightness, userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, lowPowerMode, sunlightActive, ambientLux2, userDataPoint, defaultConfig, type, this.mForegroundAppPackageName, mainFastAmbientLux, assistFastAmbientLux, originalNit, actualNit, this.mOrientation, sceneState);
            this.mLastBrightnessChangeItem = item;
            Message msg = Message.obtain(this.mBackgroundHandler, 1, item);
            msg.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBrightnessChangeEvent(BrightnessChangeItem item) {
        boolean windowOverrideApplying = !Float.isNaN(item.brightnessOverrideFromWindow);
        boolean windowOverrideChanging = windowOverrideApplying && item.brightnessOverrideFromWindow != this.mLastBrightnessOverrideFromWindow;
        if (windowOverrideChanging != this.mWindowOverrideBrightnessChanging) {
            this.mWindowOverrideBrightnessChanging = windowOverrideChanging;
        }
        if (windowOverrideChanging) {
            long now = SystemClock.elapsedRealtime();
            if (this.mUserDragging || (this.mHaveValidMotionForWindowBrightness && now - this.mLatestDraggingChangedTime < 50)) {
                this.mLastBrightnessOverrideFromWindow = item.originalBrightness;
                this.mHaveValidWindowBrightness = true;
                if (DEBUG) {
                    Slog.d(TAG, "Brightness from window is changing: " + item.originalBrightness);
                }
            }
        }
        if (windowOverrideApplying && !this.mHaveValidWindowBrightness) {
            return;
        }
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null && brightnessChangeItem.type != item.type) {
            registerSensorByBrightnessType(this.mPendingBrightnessChangeItem.type, false);
        }
        this.mPendingBrightnessChangeItem = item;
        if (this.mReportBrightnessEventsEnable) {
            registerSensorByBrightnessType(item.type, true);
        }
        debounceBrightnessEvent(3000L);
    }

    private void debounceBrightnessEvent(long debounceTime) {
        long debounceTime2 = this.mForcedReportTrainDataEnabled ? 0L : debounceTime;
        this.mBackgroundHandler.removeMessages(3);
        Message msg = this.mBackgroundHandler.obtainMessage(3);
        this.mBackgroundHandler.sendMessageDelayed(msg, debounceTime2);
    }

    private void setPointerEventListener(boolean enable) {
        WindowManagerService windowManagerService = this.mWms;
        if (windowManagerService == null) {
            return;
        }
        if (enable) {
            if (!this.mPointerEventListenerEnabled) {
                windowManagerService.registerPointerEventListener(this.mPointerEventListener, 0);
                this.mPointerEventListenerEnabled = true;
                if (DEBUG) {
                    Slog.d(TAG, "register pointer event listener.");
                    return;
                }
                return;
            }
            return;
        }
        if (this.mPointerEventListenerEnabled) {
            windowManagerService.unregisterPointerEventListener(this.mPointerEventListener, 0);
            this.mPointerEventListenerEnabled = false;
            if (DEBUG) {
                Slog.d(TAG, "unregister pointer event listener.");
            }
        }
    }

    private void setRotationListener(boolean enable) {
        if (enable) {
            if (!this.mRotationListenerEnabled) {
                this.mRotationListenerEnabled = true;
                this.mWms.watchRotation(this.mRotationWatcher, 0);
                if (DEBUG) {
                    Slog.d(TAG, "register rotation listener.");
                    return;
                }
                return;
            }
            return;
        }
        if (this.mRotationListenerEnabled) {
            this.mRotationListenerEnabled = false;
            this.mWms.removeRotationWatcher(this.mRotationWatcher);
            if (DEBUG) {
                Slog.d(TAG, "unregister rotation listener.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readyToReportEvent() {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null) {
            if (this.mHaveValidWindowBrightness && this.mUserDragging) {
                return;
            }
            reportBrightnessEvent(brightnessChangeItem);
            reportIndividualModelEvent(this.mPendingBrightnessChangeItem);
        }
    }

    private void reportBrightnessEvent(BrightnessChangeItem item) {
        if (DEBUG) {
            Slog.d(TAG, "brightness changed, let's make a recode: " + item.toString() + ", foregroundApps: " + this.mForegroundAppPackageName);
        }
        long now = System.currentTimeMillis();
        int curSpanIndex = getBrightnessSpanByNit(item.originalBrightness);
        int preSpanIndex = getBrightnessSpanByNit(this.mLastScreenBrightness);
        boolean brightnessRestricted = this.mThermalStatus == 3;
        if ((item.type == 1 && this.mCloudControllerListener.isAutoBrightnessStatisticsEventEnable()) || (item.type == 2 && this.mReportBrightnessEventsEnable)) {
            BrightnessEvent event = new BrightnessEvent();
            event.setEventType(item.type).setCurBrightnessSpanIndex(curSpanIndex).setPreBrightnessSpanIndex(preSpanIndex).setTimeStamp(now).setScreenBrightness(item.originalBrightness).setPreviousBrightness(this.mLastScreenBrightness).setAmbientLux(item.ambientLux).setUserDataPoint(item.userDataPoint).setForegroundPackage(item.packageName).setLowPowerModeFlag(item.lowPowerMode).setIsDefaultConfig(item.defaultConfig).setAffectFactorFlag(convergeAffectFactors()).setLuxSpanIndex(getAmbientLuxSpanIndex(item.ambientLux)).setOrientation(this.mOrientation).setDisplayGrayScale(this.mGrayScale).setUserId(this.mCurrentUserId).setOriginalNit(item.originalNit).setActualNit(item.actualNit).setHdrLayerEnable(this.mIsHdrLayer).setBrightnessRestrictedEnable(brightnessRestricted).setMainAmbientLux(item.mainLux).setAssistAmbientLux(item.assistLux).setLastMainAmbientLux(this.mLastMainAmbientLux).setLastAssistAmbientLux(this.mLastAssistAmbientLux).setAccValues(item.accValues).setIsUseLightFovOptimization(item.isUseLightFovOptimization).setSwitchStats(this.mSwitchStatsHelper.getAllSwitchStats());
            reportEventToServer(event);
        } else if (item.type != 1 && this.mReportBrightnessEventsEnable) {
            BrightnessEvent event2 = new BrightnessEvent();
            event2.setEventType(item.type).setCurBrightnessSpanIndex(curSpanIndex).setPreBrightnessSpanIndex(preSpanIndex).setTimeStamp(now).setPreviousBrightness(this.mLastScreenBrightness).setForegroundPackage(item.packageName).setLowPowerModeFlag(item.lowPowerMode).setAffectFactorFlag(convergeAffectFactors()).setOrientation(this.mOrientation).setUserId(this.mCurrentUserId).setOriginalNit(item.originalNit).setActualNit(item.actualNit).setHdrLayerEnable(this.mIsHdrLayer).setBrightnessRestrictedEnable(brightnessRestricted).setSwitchStats(this.mSwitchStatsHelper.getAllSwitchStats());
            if (item.type == 0) {
                event2.setAmbientLux(item.ambientLux).setLuxSpanIndex(getAmbientLuxSpanIndex(item.ambientLux));
            }
            reportEventToServer(event2);
        }
        updateBrightnessUsageIfNeeded(curSpanIndex, preSpanIndex);
    }

    private void reportIndividualModelEvent(BrightnessChangeItem item) {
        if ((item.type != 1 && item.type != 2) || Float.isNaN(item.ambientLux) || this.mModelEventCallback == null) {
            return;
        }
        IndividualModelEvent event = createModelEvent(item);
        if (event.isValidRawEvent()) {
            this.mModelEventCallback.onBrightnessModelEvent(event);
        }
    }

    private IndividualModelEvent createModelEvent(BrightnessChangeItem item) {
        return createModelEvent(item.ambientLux, item.packageName, item.originalBrightness, item.orientation, item.sceneState);
    }

    public IndividualModelEvent createModelEvent(float lux, String packageName, float newBrightness, int orientation, int sceneState) {
        int categoryId = this.mAppClassifier.getAppCategoryId(packageName);
        return createModelEvent(lux, categoryId, newBrightness, orientation, sceneState);
    }

    public IndividualModelEvent createModelEvent(float lux, int categoryId, float newBrightness, int orientation, int sceneState) {
        float currentNit = this.mDisplayDeviceConfig.getNitsFromBacklight(newBrightness);
        float currentNitSpan = getBrightnessSpanByNit(newBrightness);
        float defaultConfigNit = getConfigBrightness(lux, sceneState);
        float currentLuxSpan = getAmbientLuxSpanIndex(lux);
        float mixedOrientationApp = this.mIndividualEventNormalizer.getMixedOrientApp(categoryId, orientation);
        IndividualModelEvent.Builder builder = new IndividualModelEvent.Builder();
        builder.setCurrentBrightness(currentNit).setCurrentBrightnessSpan(currentNitSpan).setAmbientLux(lux).setAmbientLuxSpan(currentLuxSpan).setDefaultConfigBrightness(defaultConfigNit).setAppCategoryId(categoryId).setOrientation(orientation).setMixedOrientationAppId(mixedOrientationApp).setTimeStamp(System.currentTimeMillis());
        return builder.build();
    }

    public void setModelEventCallback(ModelEventCallback callback) {
        this.mModelEventCallback = callback;
    }

    private float getConfigBrightness(float lux, int sceneState) {
        Spline spline;
        Spline spline2;
        Spline spline3;
        Spline spline4;
        if (sceneState == -1 && (spline4 = this.mOriSpline) != null) {
            return spline4.interpolate(lux);
        }
        if (sceneState == 0 && (spline3 = this.mDefaultSceneSpline) != null) {
            return spline3.interpolate(lux);
        }
        if (sceneState == 2 && (spline2 = this.mDarkeningSceneSpline) != null) {
            return spline2.interpolate(lux);
        }
        if (sceneState == 1 && (spline = this.mBrighteningSceneSpline) != null) {
            return spline.interpolate(lux);
        }
        return Float.NaN;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateGrayScale$1(float grayScale) {
        this.mGrayScale = grayScale;
    }

    public void updateGrayScale(final float grayScale) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$updateGrayScale$1(grayScale);
            }
        });
    }

    private void aggregateBrightnessUsageDuration() {
        updateBrightnessUsage(getBrightnessSpanByNit(this.mLastScreenBrightness), false, REASON_UPDATE_BRIGHTNESS_USAGE_REPORT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEventToServer(BrightnessEvent event) {
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportToOneTrack(this.mContext, event);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append(event.toString());
                Slog.d(TAG, "reportBrightnessEventToServer: , event:" + ((Object) sb));
            }
        }
    }

    private void reportEventToServer(AdvancedEvent event) {
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportToOneTrack(this.mContext, event);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append(event.convertToString());
                Slog.d(TAG, "reportAdvancedEventToServer:, event:" + ((Object) sb));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetPendingParams() {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null) {
            int type = brightnessChangeItem.type;
            this.mLastScreenBrightness = this.mPendingBrightnessChangeItem.originalBrightness;
            this.mLastAmbientLux = this.mPendingBrightnessChangeItem.ambientLux;
            this.mLastMainAmbientLux = this.mPendingBrightnessChangeItem.mainLux;
            this.mLastAssistAmbientLux = this.mPendingBrightnessChangeItem.assistLux;
            registerSensorByBrightnessType(type, false);
            this.mPendingBrightnessChangeItem = null;
        }
        resetWindowOverrideParams();
    }

    private void resetWindowOverrideParams() {
        this.mHaveValidWindowBrightness = false;
        this.mLastBrightnessOverrideFromWindow = -1.0f;
        this.mWindowOverrideBrightnessChanging = false;
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            ProcessManager.registerForegroundWindowListener(this.mForegroundWindowListener);
            updateForegroundApps();
        } catch (RemoteException e) {
            if (DEBUG) {
                Slog.e(TAG, "Failed to register foreground app updater: " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApps() {
        try {
            ActivityTaskManager.RootTaskInfo info = this.mActivityTaskManager.getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null && info.getWindowingMode() != 5 && info.getWindowingMode() != 6 && !this.mActivityTaskManager.isInSplitScreenWindowingMode()) {
                String packageName = info.topActivity.getPackageName();
                String str = this.mForegroundAppPackageName;
                if (str != null && str.equals(packageName)) {
                    return;
                }
                this.mCurrentUserId = info.userId;
                this.mForegroundAppPackageName = packageName;
            }
        } catch (RemoteException e) {
        }
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ScreenStateReceiver(), filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePointerEventMotionState(boolean dragging, int distanceX, int distanceY) {
        if (this.mUserDragging != dragging) {
            this.mUserDragging = dragging;
            if (!dragging && checkIsValidMotionForWindowBrightness(distanceX, distanceY)) {
                this.mLatestDraggingChangedTime = SystemClock.elapsedRealtime();
                if (this.mHaveValidWindowBrightness) {
                    debounceBrightnessEvent(3000L);
                }
            }
        }
    }

    private boolean checkIsValidMotionForWindowBrightness(int distanceX, int distanceY) {
        boolean isValid = true;
        if (distanceY <= 10 || (distanceX != 0 && distanceY / distanceX < 2)) {
            isValid = false;
            if (DEBUG) {
                Slog.d(TAG, "checkIsValidMotionForWindowBrightness: invalid and return.");
            }
        }
        this.mHaveValidMotionForWindowBrightness = isValid;
        return isValid;
    }

    private void updateBrightnessUsageIfNeeded(int curSpanIndex, int preSpanIndex) {
        if (curSpanIndex != preSpanIndex && isValidStartTimeStamp()) {
            updateBrightnessUsage(preSpanIndex, false, REASON_UPDATE_BRIGHTNESS_USAGE_SPAN_CHANGE);
        }
    }

    private void updateBrightnessUsage(int spanIndex, boolean dueToScreenOff, String reason) {
        if (isValidStartTimeStamp()) {
            long now = SystemClock.elapsedRealtime();
            float duration = ((float) (now - this.mStartTimeStamp)) / SCREEN_NIT_SPAN_SIX;
            aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_BRIGHTNESS_USAGE, Integer.valueOf(spanIndex), Float.valueOf(duration));
            noteHbmUsage(spanIndex, duration);
            if (dueToScreenOff) {
                this.mStartTimeStamp = 0L;
            } else {
                this.mStartTimeStamp = now;
            }
            if (DEBUG) {
                Slog.d(TAG, "updateBrightnessUsage: reason: " + reason + ", span: " + spanIndex + ", append duration: " + duration + "s");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScreenStateChanged(boolean screenOn) {
        updateStartTimeStamp(screenOn);
        setPointerEventListener(screenOn);
        setRotationListener(screenOn);
    }

    private void updateStartTimeStamp(boolean screenOn) {
        if (DEBUG) {
            Slog.d(TAG, "updateStartTimeStamp: screenOn: " + screenOn);
        }
        if (screenOn && !isValidStartTimeStamp()) {
            this.mStartTimeStamp = SystemClock.elapsedRealtime();
        } else if (!screenOn) {
            updateBrightnessUsage(getBrightnessSpanByNit(this.mLastScreenBrightness), true, REASON_UPDATE_BRIGHTNESS_USAGE_SCREEN_OFF);
        }
    }

    private int getBrightnessType(boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean sunlightActive) {
        if (!Float.isNaN(brightnessOverrideFromWindow)) {
            return 3;
        }
        if (useAutoBrightness && userInitiatedChange) {
            return 2;
        }
        if (useAutoBrightness) {
            return 1;
        }
        if (!sunlightActive) {
            return 0;
        }
        return 4;
    }

    private boolean isValidStartTimeStamp() {
        return this.mStartTimeStamp > 0;
    }

    private int getAmbientLuxSpanIndex(float lux) {
        int index;
        if (lux < 30.0f) {
            index = (int) (lux / 5.0f);
        } else if (lux < 900.0f) {
            index = (int) ((lux / 100.0f) + 6.0f);
        } else if (lux < 3500.0f) {
            index = (int) (((lux - 900.0f) / 200.0f) + 15.0f);
        } else if (lux < 10000.0f) {
            index = (int) (((lux - 3500.0f) / SCREEN_NIT_SPAN_FIVE) + 28.0f);
        } else if (lux < 15000.0f) {
            index = 41;
        } else if (lux < 35000.0f) {
            index = 42;
        } else if (lux < 65000.0f) {
            index = 43;
        } else if (lux < 100000.0f) {
            index = MAX_SPAN_INDEX;
        } else {
            index = MAX_SPAN_INDEX;
        }
        if (DEBUG) {
            Slog.d(TAG, "lux = " + lux + ", index = " + index);
        }
        return index;
    }

    private int getBrightnessSpanByNit(float brightness) {
        float screenNit = Math.round(this.mDisplayDeviceConfig.getNitsFromBacklight(brightness));
        int index = 0;
        if (screenNit >= SCREEN_NIT_SPAN_ONE && screenNit < SCREEN_NIT_SPAN_TWO) {
            index = 1;
        } else if (screenNit >= SCREEN_NIT_SPAN_TWO && screenNit < 50.0f) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_TWO) / SPAN_SCREEN_NIT_STEP_ONE)) + 2;
        } else if (screenNit >= 50.0f && screenNit < SCREEN_NIT_SPAN_FOUR) {
            index = ((int) ((screenNit - 50.0f) / SPAN_SCREEN_NIT_STEP_TWO)) + 2 + 6;
        } else if (screenNit >= SCREEN_NIT_SPAN_FOUR && screenNit < SCREEN_NIT_SPAN_FIVE) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_FOUR) / 20.0f)) + 2 + 9;
        } else if (screenNit >= SCREEN_NIT_SPAN_FIVE && screenNit < SCREEN_NIT_SPAN_SIX) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_FIVE) / 50.0f)) + 2 + 30;
        } else if (screenNit > SCREEN_NIT_SPAN_SIX) {
            index = 42;
        }
        if (DEBUG) {
            Slog.d(TAG, "brightness = " + brightness + ", screenNit = " + screenNit + ", index = " + index);
        }
        return index;
    }

    private int convergeAffectFactors() {
        int factor = 0;
        if (this.mSwitchStatsHelper.isReadModeSettingsEnable()) {
            factor = 0 | 1;
        }
        if (this.mSwitchStatsHelper.isDcBacklightSettingsEnable()) {
            factor |= 2;
        }
        if (this.mSwitchStatsHelper.isDarkModeSettingsEnable()) {
            return factor | 4;
        }
        return factor;
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.CloudListener
    public void onCloudUpdated(long summary, Map<String, Object> data) {
        this.mReportBrightnessEventsEnable = (1 & summary) != 0;
    }

    @Override // com.android.server.display.ThermalBrightnessController.ThermalListener
    public void thermalConfigChanged(List<ThermalConditionItem> item) {
        for (ThermalConditionItem condition : item) {
            List<TemperatureBrightnessPair> pairs = condition.getTemperatureBrightnessPair();
            for (TemperatureBrightnessPair pair : pairs) {
                float minTemp = pair.getMinInclusive();
                float maxTemp = pair.getMaxExclusive();
                if (!this.mTemperatureSpan.contains(Float.valueOf(minTemp))) {
                    this.mTemperatureSpan.add(Float.valueOf(minTemp));
                }
                if (!this.mTemperatureSpan.contains(Float.valueOf(maxTemp))) {
                    this.mTemperatureSpan.add(Float.valueOf(maxTemp));
                }
            }
        }
        if (this.mTemperatureSpan.size() != 0) {
            Collections.sort(this.mTemperatureSpan);
        }
        if (DEBUG) {
            Slog.d(TAG, "thermalConfigChanged: mTemperatureSpan: " + this.mTemperatureSpan.toString());
        }
    }

    private int getTemperatureSpan(float temperature) {
        if (Float.isNaN(temperature)) {
            return 0;
        }
        for (int index = 0; index < this.mTemperatureSpan.size(); index++) {
            if (index == this.mTemperatureSpan.size() - 1 && temperature == this.mTemperatureSpan.get(index).floatValue()) {
                int temperatureSpan = index;
                return temperatureSpan;
            }
            if (index == this.mTemperatureSpan.size() - 1 && temperature > this.mTemperatureSpan.get(index).floatValue()) {
                int temperatureSpan2 = index + 1;
                return temperatureSpan2;
            }
            if (this.mTemperatureSpan.get(index).floatValue() <= temperature && temperature < this.mTemperatureSpan.get(index + 1).floatValue()) {
                int temperatureSpan3 = index;
                return temperatureSpan3;
            }
        }
        return 0;
    }

    /* loaded from: classes.dex */
    private class BrightnessChangeHandler extends Handler {
        public BrightnessChangeHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BrightnessDataProcessor.this.handleBrightnessChangeEvent((BrightnessChangeItem) msg.obj);
                    return;
                case 2:
                    BrightnessDataProcessor.this.updatePointerEventMotionState(((Boolean) msg.obj).booleanValue(), msg.arg1, msg.arg2);
                    return;
                case 3:
                    BrightnessDataProcessor.this.readyToReportEvent();
                    BrightnessDataProcessor.this.brightnessChangedTriggerAggregation();
                    BrightnessDataProcessor.this.resetPendingParams();
                    return;
                case 4:
                    BrightnessDataProcessor.this.updateScreenStateChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 5:
                    BrightnessDataProcessor.this.updateForegroundApps();
                    return;
                case 6:
                    BrightnessDataProcessor.this.mOrientation = ((Integer) msg.obj).intValue();
                    return;
                case 7:
                    SomeArgs args = (SomeArgs) msg.obj;
                    BrightnessDataProcessor.this.updateBrightnessStatisticsData(((Boolean) args.arg1).booleanValue(), ((Float) args.arg2).floatValue());
                    return;
                case 8:
                    SomeArgs someArgs = (SomeArgs) msg.obj;
                    BrightnessDataProcessor.this.updateBrightnessAnimInfo(((Float) someArgs.arg1).floatValue(), ((Float) someArgs.arg2).floatValue(), ((Boolean) someArgs.arg3).booleanValue());
                    return;
                case 9:
                    BrightnessDataProcessor.this.mTargetBrightnessAnimValue = ((Float) msg.obj).floatValue();
                    return;
                case 10:
                    BrightnessDataProcessor.this.updateInterruptBrightnessAnimDuration(msg.arg1, ((Float) msg.obj).floatValue());
                    return;
                case 11:
                    BrightnessDataProcessor.this.mTemporaryBrightnessTimeStamp = SystemClock.elapsedRealtime();
                    return;
                case 12:
                    BrightnessDataProcessor.this.resetBrightnessAnimInfo();
                    return;
                case 13:
                    BrightnessEvent event = (BrightnessEvent) msg.obj;
                    BrightnessDataProcessor.this.reportEventToServer(event);
                    return;
                case 14:
                    BrightnessDataProcessor.this.mIsAonSuppressDarken = false;
                    return;
                case 15:
                    BrightnessDataProcessor.this.mIsNotAonSuppressDarken = false;
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class BrightnessChangeItem {
        float[] accValues = new float[3];
        float actualBrightness;
        float actualNit;
        float ambientLux;
        float assistLux;
        float brightnessOverrideFromWindow;
        boolean defaultConfig;
        boolean isUseLightFovOptimization;
        boolean lowPowerMode;
        float mainLux;
        int orientation;
        float originalBrightness;
        float originalNit;
        String packageName;
        int sceneState;
        boolean sunlightActive;
        int type;
        boolean useAutoBrightness;
        float userDataPoint;
        boolean userInitiatedChange;

        public BrightnessChangeItem(float originalBrightness, float actualBrightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, boolean sunlightActive, float ambientLux, float userDataPoint, boolean defaultConfig, int type, String packageName, float mainFastAmbientLux, float assistFastAmbientLux, float originalNit, float actualNit, int orientation, int sceneState) {
            this.originalBrightness = originalBrightness;
            this.actualBrightness = actualBrightness;
            this.brightnessOverrideFromWindow = brightnessOverrideFromWindow;
            this.userInitiatedChange = userInitiatedChange;
            this.useAutoBrightness = useAutoBrightness;
            this.lowPowerMode = lowPowerMode;
            this.sunlightActive = sunlightActive;
            this.ambientLux = ambientLux;
            this.userDataPoint = userDataPoint;
            this.defaultConfig = defaultConfig;
            this.type = type;
            this.packageName = packageName;
            this.mainLux = mainFastAmbientLux;
            this.assistLux = assistFastAmbientLux;
            this.originalNit = originalNit;
            this.actualNit = actualNit;
            this.orientation = orientation;
            this.sceneState = sceneState;
        }

        public boolean equals(Object o) {
            if (!(o instanceof BrightnessChangeItem)) {
                return false;
            }
            BrightnessChangeItem value = (BrightnessChangeItem) o;
            return this.originalBrightness == value.originalBrightness && this.type == value.type && this.userInitiatedChange == value.userInitiatedChange && this.useAutoBrightness == value.useAutoBrightness && this.brightnessOverrideFromWindow == value.brightnessOverrideFromWindow && this.lowPowerMode == value.lowPowerMode && this.ambientLux == value.ambientLux && this.userDataPoint == value.userDataPoint && this.packageName.equals(value.packageName) && this.mainLux == value.mainLux && this.assistLux == value.assistLux && Arrays.equals(this.accValues, value.accValues) && this.originalNit == value.originalNit && this.actualNit == value.actualNit && this.orientation == value.orientation && this.sceneState == value.sceneState;
        }

        public String toString() {
            return "BrightnessChangeItem{type=" + typeToString(this.type) + ", originalBrightness=" + this.originalBrightness + ", actualBrightness=" + this.actualBrightness + ", originalNit=" + this.originalNit + ", actualNit=" + this.actualNit + ", brightnessOverrideFromWindow=" + this.brightnessOverrideFromWindow + ", userInitiatedChange=" + this.userInitiatedChange + ", useAutoBrightness=" + this.useAutoBrightness + ", lowPowerMode=" + this.lowPowerMode + ", sunlightActive=" + this.sunlightActive + ", ambientLux=" + this.ambientLux + ", userDataPoint=" + this.userDataPoint + ", defaultConfig=" + this.defaultConfig + ", packageName=" + this.packageName + ", mainLux=" + this.mainLux + ", assistLux=" + this.assistLux + ", accValues=" + Arrays.toString(this.accValues) + ", orientation=" + this.orientation + ", sceneState=" + this.sceneState + '}';
        }

        private String typeToString(int type) {
            switch (type) {
                case 0:
                    return "manual_brightness";
                case 1:
                    return "auto_brightness";
                case 2:
                    return "auto_manual_brightness";
                case 3:
                    return "window_override_brightness";
                case 4:
                    return "sunlight_brightness";
                default:
                    return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ScreenStateReceiver extends BroadcastReceiver {
        ScreenStateReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            boolean screenOn = false;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    screenOn = true;
                    break;
            }
            Message msg = Message.obtain(BrightnessDataProcessor.this.mBackgroundHandler, 4, Boolean.valueOf(screenOn));
            msg.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() {
            BrightnessDataProcessor.this.mBackgroundHandler.removeMessages(5);
            BrightnessDataProcessor.this.mBackgroundHandler.sendEmptyMessage(5);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RotationWatcher extends IRotationWatcher.Stub {
        RotationWatcher() {
        }

        public void onRotationChanged(int rotation) throws RemoteException {
            Message msg = Message.obtain(BrightnessDataProcessor.this.mBackgroundHandler, 6, Integer.valueOf(rotation));
            msg.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WindowOverlayEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        int distance_x;
        int distance_y;
        boolean userDragging;
        float eventXDown = -1.0f;
        float eventYDown = -1.0f;
        float eventXUp = -1.0f;
        float eventYUp = -1.0f;

        WindowOverlayEventListener() {
        }

        public void onPointerEvent(MotionEvent event) {
            switch (event.getAction()) {
                case 0:
                    this.eventXDown = event.getX();
                    this.eventYDown = event.getY();
                    break;
                case 1:
                    this.eventXUp = event.getX();
                    this.eventYUp = event.getY();
                    this.userDragging = false;
                    break;
                case 2:
                    this.userDragging = true;
                    break;
            }
            if (this.userDragging != BrightnessDataProcessor.this.mUserDragging) {
                BrightnessDataProcessor.this.mBackgroundHandler.removeMessages(2);
                if (!this.userDragging) {
                    this.distance_x = (int) MathUtils.abs(this.eventXUp - this.eventXDown);
                    this.distance_y = (int) MathUtils.abs(this.eventYUp - this.eventYDown);
                    if (BrightnessDataProcessor.DEBUG) {
                        Slog.d(BrightnessDataProcessor.TAG, "onPointerEvent: x_down: " + this.eventXDown + ", y_down: " + this.eventYDown + ", x_up: " + this.eventXUp + ", y_up: " + this.eventYUp + ", distance_x: " + this.distance_x + ", distance_y: " + this.distance_y);
                    }
                }
                Message message = Message.obtain(BrightnessDataProcessor.this.mBackgroundHandler, 2, this.distance_x, this.distance_y, Boolean.valueOf(this.userDragging));
                message.sendToTarget();
            }
        }
    }

    private void scheduleUpdateBrightnessStatisticsData(boolean screenOn, float brightness) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = Boolean.valueOf(screenOn);
        args.arg2 = Float.valueOf(brightness);
        this.mBackgroundHandler.obtainMessage(7, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBrightnessStatisticsData(boolean screenOn, float brightness) {
        long now = SystemClock.elapsedRealtime();
        if (this.mScreenOn != screenOn) {
            this.mScreenOn = screenOn;
            if (screenOn) {
                this.mLastScreenOnTimeStamp = 0L;
                if (this.mLastScreenBrightness == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                    this.mLastScreenBrightness = brightness;
                }
            } else {
                this.mLastStoreBrightness = Float.NaN;
                computeLastAverageBrightness(now);
            }
        }
        if (screenOn) {
            computeAverageBrightnessIfNeeded(brightness);
        }
    }

    private void computeAverageBrightnessIfNeeded(float brightness) {
        if (this.mLastStoreBrightness == brightness && !this.mAutoBrightnessModeChanged) {
            return;
        }
        this.mLastStoreBrightness = brightness;
        long now = SystemClock.elapsedRealtime();
        if (this.mLastScreenOnTimeStamp != 0) {
            computeLastAverageBrightness(now);
        }
        this.mLastAutoBrightness = Float.NaN;
        this.mLastManualBrightness = Float.NaN;
        if (this.mAutoBrightnessEnable) {
            this.mLastAutoBrightness = brightness;
        } else {
            this.mLastManualBrightness = brightness;
        }
        this.mLastScreenOnTimeStamp = now;
        if (this.mAutoBrightnessModeChanged) {
            this.mAutoBrightnessModeChanged = false;
        }
        if (DEBUG) {
            Slog.d(TAG, "computeAverageBrightnessIfNeeded: current brightness: " + brightness + ", mAutoBrightnessEnable: " + this.mAutoBrightnessEnable + ", time: " + this.mLastScreenOnTimeStamp);
        }
    }

    private void computeLastAverageBrightness(long now) {
        float timeDuration = ((float) (now - this.mLastScreenOnTimeStamp)) * 0.001f;
        if (timeDuration != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            if (!Float.isNaN(this.mLastAutoBrightness)) {
                this.mAutoBrightnessDuration += timeDuration;
                float nitsFromBacklight = this.mAutoBrightnessIntegral + (this.mDisplayDeviceConfig.getNitsFromBacklight(this.mLastAutoBrightness) * timeDuration);
                this.mAutoBrightnessIntegral = nitsFromBacklight;
                float avgNits = nitsFromBacklight / this.mAutoBrightnessDuration;
                aggregateBrightnessEventsSync(AggregationEvent.BrightnessAggregationEvent.EVENT_AVERAGE_BRIGHTNESS, 1, Float.valueOf(avgNits));
                if (DEBUG) {
                    Slog.d(TAG, "computeLastAverageBrightness: compute last auto average brightness, timeDuration: " + timeDuration + "s, mAutoBrightnessDuration: " + this.mAutoBrightnessDuration + "s, mAutoBrightnessIntegral: " + this.mAutoBrightnessIntegral + ", avgNits: " + avgNits);
                    return;
                }
                return;
            }
            this.mManualBrightnessDuration += timeDuration;
            float nitsFromBacklight2 = this.mManualBrightnessIntegral + (this.mDisplayDeviceConfig.getNitsFromBacklight(BrightnessSynchronizer.brightnessIntToFloat((int) this.mLastManualBrightness)) * timeDuration);
            this.mManualBrightnessIntegral = nitsFromBacklight2;
            float avgNits2 = nitsFromBacklight2 / this.mManualBrightnessDuration;
            aggregateBrightnessEventsSync(AggregationEvent.BrightnessAggregationEvent.EVENT_AVERAGE_BRIGHTNESS, 0, Float.valueOf(avgNits2));
            if (DEBUG) {
                Slog.d(TAG, "computeLastAverageBrightness: compute last manual average brightness, timeDuration: " + timeDuration + "s, mManualBrightnessDuration: " + this.mManualBrightnessDuration + "s, mManualBrightnessIntegral: " + this.mManualBrightnessIntegral + ", avgNits: " + avgNits2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            if (selfChange) {
                return;
            }
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1571092473:
                    if (lastPathSegment.equals(BrightnessDataProcessor.DISABLE_SECURITY_BY_MI_SHOW)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    BrightnessDataProcessor brightnessDataProcessor = BrightnessDataProcessor.this;
                    brightnessDataProcessor.mAutoBrightnessEnable = Settings.System.getIntForUser(brightnessDataProcessor.mContentResolver, "screen_brightness_mode", 0, -2) == 1;
                    BrightnessDataProcessor.this.mAutoBrightnessModeChanged = true;
                    if (BrightnessDataProcessor.this.mReportBrightnessEventsEnable) {
                        BrightnessDataProcessor.this.reportDisabledAutoBrightnessEvent();
                    }
                    BrightnessDataProcessor.this.updateUserResetAutoBrightnessModeTimes();
                    return;
                case 1:
                    BrightnessDataProcessor brightnessDataProcessor2 = BrightnessDataProcessor.this;
                    brightnessDataProcessor2.mIsMiShow = Settings.System.getInt(brightnessDataProcessor2.mContentResolver, BrightnessDataProcessor.DISABLE_SECURITY_BY_MI_SHOW, 0) == 1;
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUserResetAutoBrightnessModeTimes() {
        if (this.mAutoBrightnessEnable == this.mLastAutoBrightnessEnable) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (this.mAutoBrightnessEnable && this.mIsValidResetAutoBrightnessMode) {
            long j = this.mLastResetBrightnessModeTime;
            if (j != 0 && now - j <= 3000) {
                aggregateAdvancedBrightnessEventsSum(AggregationEvent.AdvancedAggregationEvent.EVENT_RESET_BRIGHTNESS_MODE_TIMES, AggregationEvent.AdvancedAggregationEvent.KEY_RESET_TIMES, 1);
                flareStatisticalResetBrightnessModeTimes();
            }
        }
        this.mIsValidResetAutoBrightnessMode = this.mLastAutoBrightnessEnable && !this.mAutoBrightnessEnable;
        this.mLastAutoBrightnessEnable = this.mAutoBrightnessEnable;
        this.mLastResetBrightnessModeTime = now;
        if (DEBUG) {
            Slog.d(TAG, "updateUserResetAutoBrightnessModeTimes: mAdvancedBrightnessEventsMap: " + this.mAdvancedBrightnessEventsMap.toString());
        }
    }

    private void registerSettingsObserver() {
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        boolean z = Settings.System.getIntForUser(this.mContentResolver, "screen_brightness_mode", 0, -2) == 1;
        this.mAutoBrightnessEnable = z;
        this.mLastAutoBrightnessEnable = z;
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor(DISABLE_SECURITY_BY_MI_SHOW), false, this.mSettingsObserver, -1);
        this.mIsMiShow = Settings.System.getInt(this.mContentResolver, DISABLE_SECURITY_BY_MI_SHOW, 0) == 1;
    }

    private void resetAverageBrightnessInfo() {
        this.mAutoBrightnessDuration = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mAutoBrightnessIntegral = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mManualBrightnessDuration = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mManualBrightnessIntegral = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    public void notifyUpdateTempBrightnessTimeStampIfNeeded(boolean enable) {
        if (enable != this.mIsTemporaryBrightnessAdjustment) {
            this.mIsTemporaryBrightnessAdjustment = enable;
            if (enable) {
                this.mPendingAnimationStart = false;
                this.mBackgroundHandler.obtainMessage(11).sendToTarget();
            }
        }
    }

    public void notifyUpdateBrightnessAnimInfo(float currentBrightnessAnim, float brightnessAnim, float targetBrightnessAnim) {
        int i;
        int i2;
        boolean begin = brightnessAnim != targetBrightnessAnim;
        int state = getBrightnessChangedState(currentBrightnessAnim, targetBrightnessAnim);
        if (begin != this.mPendingAnimationStart || (begin && (i2 = this.mPendingBrightnessChangedState) != 2 && i2 != 3 && state != 2 && state != 3 && state != i2)) {
            this.mPendingAnimationStart = begin;
            this.mPendingBrightnessChangedState = state;
            this.mPendingTargetBrightnessAnimValue = targetBrightnessAnim;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Float.valueOf(currentBrightnessAnim);
            args.arg2 = Float.valueOf(targetBrightnessAnim);
            args.arg3 = Boolean.valueOf(begin);
            this.mBackgroundHandler.obtainMessage(8, args).sendToTarget();
            return;
        }
        if (begin && (i = this.mPendingBrightnessChangedState) != 2 && i != 3 && state != 2 && state != 3 && state == i && targetBrightnessAnim != this.mPendingTargetBrightnessAnimValue) {
            this.mPendingTargetBrightnessAnimValue = targetBrightnessAnim;
            Message msg = this.mBackgroundHandler.obtainMessage(9);
            msg.obj = Float.valueOf(targetBrightnessAnim);
            msg.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBrightnessAnimInfo(float currentBrightnessAnim, float targetBrightnessAnim, boolean begin) {
        if (this.mAutoBrightnessEnable) {
            this.mCurrentBrightnessAnimValue = currentBrightnessAnim;
            this.mTargetBrightnessAnimValue = targetBrightnessAnim;
            this.mBrightnessAnimStart = begin;
            this.mBrightnessAnimStartTime = SystemClock.elapsedRealtime();
            this.mBrightnessChangedState = getBrightnessChangedState(currentBrightnessAnim, targetBrightnessAnim);
            if (DEBUG) {
                Slog.d(TAG, "updateBrightnessAnimInfo: mCurrentAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mCurrentBrightnessAnimValue) + ", mTargetAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mTargetBrightnessAnimValue) + ", mAnimationStart:" + this.mBrightnessAnimStart + ", mAnimationStartTime:" + this.mBrightnessAnimStartTime + ", mBrightnessChangedState = " + this.mBrightnessChangedState);
            }
        }
    }

    private void updateInterruptBrightnessAnimDurationIfNeeded(int type, float brightness) {
        Message msg = this.mBackgroundHandler.obtainMessage(10);
        msg.arg1 = type;
        msg.obj = Float.valueOf(brightness);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInterruptBrightnessAnimDuration(int type, float brightness) {
        boolean isSameAdjustment = (this.mBrightnessChangedState == 0 && getBrightnessChangedState(this.mCurrentBrightnessAnimValue, brightness) == 0) || (this.mBrightnessChangedState == 1 && getBrightnessChangedState(this.mCurrentBrightnessAnimValue, brightness) == 1);
        if (type == 2) {
            if (this.mBrightnessAnimStart && isSameAdjustment) {
                long j = this.mTemporaryBrightnessTimeStamp;
                if (j != 0) {
                    long j2 = this.mBrightnessAnimStartTime;
                    if (j2 != 0 && j > j2) {
                        long duration = j - j2;
                        aggregateAdvancedBrightnessEventsSum(AggregationEvent.AdvancedAggregationEvent.EVENT_INTERRUPT_ANIMATION_TIMES, AggregationEvent.AdvancedAggregationEvent.KEY_INTERRUPT_TIMES, 1);
                        boolean brightnessRestricted = this.mThermalStatus == 3;
                        AdvancedEvent event = new AdvancedEvent();
                        event.setEventType(1).setAutoBrightnessAnimationDuration(((float) duration) * 0.001f).setCurrentAnimateValue(BrightnessSynchronizer.brightnessFloatToInt(this.mCurrentBrightnessAnimValue)).setTargetAnimateValue(BrightnessSynchronizer.brightnessFloatToInt(this.mTargetBrightnessAnimValue)).setUserBrightness(BrightnessSynchronizer.brightnessFloatToInt(brightness)).setBrightnessChangedState(this.mBrightnessChangedState).setTimeStamp(System.currentTimeMillis()).setBrightnessRestrictedEnable(brightnessRestricted);
                        reportEventToServer(event);
                        if (DEBUG) {
                            Slog.d(TAG, "updateInterruptBrightnessAnimDuration: duration:" + (((float) duration) * 0.001f) + ", currentAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mCurrentBrightnessAnimValue) + ", targetAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mTargetBrightnessAnimValue) + ", userBrightness:" + BrightnessSynchronizer.brightnessFloatToInt(brightness) + ", mBrightnessChangedState:" + this.mBrightnessChangedState);
                        }
                    }
                }
            }
            this.mBrightnessAnimStart = false;
            this.mTemporaryBrightnessTimeStamp = 0L;
        }
    }

    private int getBrightnessChangedState(float currentValue, float targetValue) {
        if (currentValue == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || targetValue == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            return 3;
        }
        if (targetValue > currentValue) {
            return 0;
        }
        if (targetValue < currentValue) {
            return 1;
        }
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetBrightnessAnimInfo() {
        this.mCurrentBrightnessAnimValue = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mTargetBrightnessAnimValue = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        this.mBrightnessAnimStart = false;
        this.mBrightnessAnimStartTime = 0L;
        this.mBrightnessChangedState = -1;
        this.mTemporaryBrightnessTimeStamp = 0L;
    }

    public void notifyResetBrightnessAnimInfo() {
        this.mPendingAnimationStart = false;
        this.mBackgroundHandler.obtainMessage(12).sendToTarget();
    }

    public void setUpCloudControllerListener(AutomaticBrightnessControllerImpl.CloudControllerListener listener) {
        this.mCloudControllerListener = listener;
    }

    public void setDisplayDeviceConfig(DisplayDeviceConfig config) {
        this.mDisplayDeviceConfig = config;
    }

    public void setBrightnessMapper(BrightnessMappingStrategy brightnessMapper) {
        this.mBrightnessMapper = brightnessMapper;
        if (brightnessMapper != null) {
            BrightnessConfiguration config = brightnessMapper.getDefaultConfig();
            this.mOriSpline = getSpline(config);
        }
    }

    private Spline getSpline(BrightnessConfiguration config) {
        if (config == null) {
            return null;
        }
        Pair<float[], float[]> defaultCurve = config.getCurve();
        Spline spline = Spline.createSpline((float[]) defaultCurve.first, (float[]) defaultCurve.second);
        return spline;
    }

    public void setBrightnessConfiguration(BrightnessConfiguration defaultSceneConfig, BrightnessConfiguration darkeningSceneConfig, BrightnessConfiguration brighteningSceneConfig) {
        this.mDefaultSceneSpline = getSpline(defaultSceneConfig);
        this.mDarkeningSceneSpline = getSpline(darkeningSceneConfig);
        this.mBrighteningSceneSpline = getSpline(brighteningSceneConfig);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateExpId$2(int expId) {
        this.mExpId = expId;
    }

    public void updateExpId(final int expId) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$updateExpId$2(expId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void brightnessChangedTriggerAggregation() {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null) {
            int type = brightnessChangeItem.type;
            String pkg = this.mPendingBrightnessChangeItem.packageName;
            int factor = convergeAffectFactors();
            int luxSpan = getAmbientLuxSpanIndex(this.mPendingBrightnessChangeItem.ambientLux);
            int preBrightnessSpan = getBrightnessSpanByNit(this.mLastScreenBrightness);
            int curBrightnessSpan = getBrightnessSpanByNit(this.mPendingBrightnessChangeItem.originalBrightness);
            float brightness = this.mPendingBrightnessChangeItem.originalBrightness;
            float curNit = Math.round(this.mDisplayDeviceConfig.getNitsFromBacklight(brightness));
            aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_BRIGHTNESS_ADJUST_TIMES, Integer.valueOf(type), 1);
            if (type == 3) {
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_OVERRIDE_ADJUST_APP_RANKING, pkg, 1);
            } else if (type == 2) {
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_APP_RANKING, pkg, 1);
                aggregateBrightnessEventsAvg(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_AVG_NITS_LUX_SPAN, Integer.valueOf(luxSpan), Float.valueOf(curNit));
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_DISPLAY_MODE, Integer.valueOf(factor), 1);
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_LUX_SPAN, Integer.valueOf(luxSpan), 1);
                if (curBrightnessSpan < preBrightnessSpan) {
                    aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_LOW_LUX_SPAN, Integer.valueOf(luxSpan), 1);
                    flareStatisticalManualAdjustTimes(false);
                }
                if (curBrightnessSpan > preBrightnessSpan) {
                    aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_HIGH_LUX_SPAN, Integer.valueOf(luxSpan), 1);
                    flareStatisticalManualAdjustTimes(true);
                }
            }
            noteAdjHighTimesOnBrightnessRestricted(type, brightness);
            if (DEBUG) {
                Slog.d(TAG, "brightnessChangedTriggerAggregation: type: " + type + ", mLastScreenBrightness: " + this.mLastScreenBrightness + ", brightness: " + brightness + ", curNit: " + curNit + ", preBrightnessSpan: " + preBrightnessSpan + ", curBrightnessSpan: " + curBrightnessSpan + ", mBrightnessEventsMap: " + this.mBrightnessEventsMap.toString());
            }
        }
    }

    private void aggregateBrightnessEventsSum(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getBrightnessAggregationEvent(keySubEvent);
        statsSummary(event.getQuotaEvents(), key, value);
        this.mBrightnessEventsMap.put(keySubEvent, event);
    }

    private void aggregateBrightnessEventsAvg(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getBrightnessAggregationEvent(keySubEvent);
        statsAverageValues(event, key, (Float) value);
        this.mBrightnessEventsMap.put(keySubEvent, event);
    }

    private void aggregateBrightnessEventsSync(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getBrightnessAggregationEvent(keySubEvent);
        event.getQuotaEvents().put(key, value);
        this.mBrightnessEventsMap.put(keySubEvent, event);
    }

    private void aggregateAdvancedBrightnessEventsSum(String keySubEvent, Object key, Object value) {
        AggregationEvent event = this.mAdvancedBrightnessEventsMap.get(keySubEvent);
        if (event == null) {
            event = new AggregationEvent.AdvancedAggregationEvent();
        }
        statsSummary(event.getQuotaEvents(), key, value);
        this.mAdvancedBrightnessEventsMap.put(keySubEvent, event);
    }

    private AggregationEvent getBrightnessAggregationEvent(String keySubEvent) {
        AggregationEvent event = this.mBrightnessEventsMap.get(keySubEvent);
        if (event == null) {
            return new AggregationEvent.BrightnessAggregationEvent();
        }
        return event;
    }

    private void statsSummary(Map<Object, Object> map, Object key, Object value) {
        Object totalValues = map.get(key);
        if (totalValues != null) {
            value = getSumValues(totalValues, value);
        }
        map.put(key, value);
    }

    private Object getSumValues(Object value, Object increment) {
        if ((value instanceof Integer) && (increment instanceof Integer)) {
            return Integer.valueOf(((Integer) value).intValue() + ((Integer) increment).intValue());
        }
        if ((value instanceof Long) && (increment instanceof Long)) {
            return Long.valueOf(((Long) value).longValue() + ((Long) increment).longValue());
        }
        if ((value instanceof Float) && (increment instanceof Float)) {
            return Float.valueOf(((Float) value).floatValue() + ((Float) increment).floatValue());
        }
        return increment;
    }

    private void statsAverageValues(AggregationEvent event, Object key, Float value) {
        Map<Object, Object> quotaEvents = event.getQuotaEvents();
        Map<Object, List<Float>> map = event.getCacheDataMap();
        List<Float> list = map.get(key);
        float sum = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        if (list == null) {
            list = new ArrayList();
        }
        list.add(value);
        map.put(key, list);
        Iterator<Float> it = list.iterator();
        while (it.hasNext()) {
            float v = it.next().floatValue();
            sum += v;
        }
        quotaEvents.put(key, Float.valueOf(sum / list.size()));
    }

    private void aggregateSwitchEvents() {
        List<BrightnessEvent.SwitchStatEntry> allSwitchStats = this.mSwitchStatsHelper.getAllSwitchStats();
        if (DEBUG) {
            Slog.d(TAG, "aggregateSwitchEvents: allSwitchStats:" + allSwitchStats);
        }
        aggregateBrightnessEventsSync(AggregationEvent.BrightnessAggregationEvent.EVENT_SWITCH_STATS, AggregationEvent.BrightnessAggregationEvent.KEY_SWITCH_STATS_DETAILS, allSwitchStats);
    }

    private void reportAggregatedBrightnessEvents() {
        aggregateSwitchEvents();
        aggregateBrightnessUsageDuration();
        noteHdrUsageBeforeReported();
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportAggregatedEventsToServer(this.mContext, this.mBrightnessEventsMap, OneTrackUploaderHelper.BRIGHTNESS_QUOTA_AGGREGATION_EVENT_NAME, this.mExpId);
            if (DEBUG) {
                Slog.d(TAG, "reportAggregatedBrightnessEvents: mBrightnessEventsMap: " + this.mBrightnessEventsMap.toString());
            }
        }
        resetStatsCache();
    }

    private void resetStatsCache() {
        resetAverageBrightnessInfo();
        this.mBrightnessEventsMap.clear();
    }

    private void reportAdvancedBrightnessEvents() {
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportAggregatedEventsToServer(this.mContext, this.mAdvancedBrightnessEventsMap, OneTrackUploaderHelper.ADVANCED_BRIGHTNESS_AGGREGATION_EVENT_NAME, this.mExpId);
            if (DEBUG) {
                Slog.d(TAG, "reportAdvancedBrightnessEvents: mAdvancedBrightnessEventsMap: " + this.mAdvancedBrightnessEventsMap.toString());
            }
        }
        this.mAdvancedBrightnessEventsMap.clear();
    }

    private AggregationEvent getThermalAggregationEvent(String keySubEvent) {
        AggregationEvent event = this.mThermalEventsMap.get(keySubEvent);
        if (event == null) {
            return new AggregationEvent.ThermalAggregationEvent();
        }
        return event;
    }

    private void aggregateThermalEventsSync(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getThermalAggregationEvent(keySubEvent);
        event.getQuotaEvents().put(key, value);
        this.mThermalEventsMap.put(keySubEvent, event);
    }

    private void aggregateThermalEventsSum(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getThermalAggregationEvent(keySubEvent);
        statsSummary(event.getQuotaEvents(), key, value);
        this.mThermalEventsMap.put(keySubEvent, event);
    }

    private void aggregateThermalEventsAvg(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getThermalAggregationEvent(keySubEvent);
        statsAverageValues(event, key, (Float) value);
        this.mThermalEventsMap.put(keySubEvent, event);
    }

    public void noteFullSceneThermalUsageStats(final float brightness, final float thermalBrightness, final int conditionId, final float temperature, final boolean outdoorHighTemState) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$noteFullSceneThermalUsageStats$3(thermalBrightness, brightness, outdoorHighTemState, conditionId, temperature);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteFullSceneThermalUsageStats$3(float thermalBrightness, float brightness, boolean outdoorHighTemState, int conditionId, float temperature) {
        long now = SystemClock.elapsedRealtime();
        noteOutDoorHighTempUsage(thermalBrightness, brightness, now, outdoorHighTemState, false);
        noteThermalBrightnessRestrictedUsage(thermalBrightness, brightness, now, true);
        if (Float.isNaN(thermalBrightness)) {
            noteDetailThermalUsage(1, conditionId, temperature, now, true);
            noteThermalUsage(1, now, true);
        } else if (brightness > thermalBrightness) {
            noteDetailThermalUsage(3, conditionId, temperature, now, true);
            noteThermalUsage(3, now, true);
        } else {
            noteDetailThermalUsage(2, conditionId, temperature, now, true);
            noteThermalUsage(2, now, true);
        }
    }

    private void noteThermalUsage(int status, long now, boolean isScreenOn) {
        if (this.mLastThermalStatusTimeStamp == 0) {
            this.mThermalStatus = status;
            this.mLastThermalStatusTimeStamp = now;
        }
        int i = this.mThermalStatus;
        if (i != status && isScreenOn) {
            float duration = ((float) (now - this.mLastThermalStatusTimeStamp)) / SCREEN_NIT_SPAN_SIX;
            aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_USAGE, Integer.valueOf(i), Float.valueOf(duration));
            this.mThermalStatus = status;
            this.mLastThermalStatusTimeStamp = now;
            if (DEBUG) {
                Slog.d(TAG, "noteThermalUsage: status: " + status + ", duration: " + duration + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
                return;
            }
            return;
        }
        if (!isScreenOn) {
            float duration2 = ((float) (now - this.mLastThermalStatusTimeStamp)) / SCREEN_NIT_SPAN_SIX;
            aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_USAGE, Integer.valueOf(i), Float.valueOf(duration2));
            this.mThermalStatus = status;
            this.mLastThermalStatusTimeStamp = now;
            if (DEBUG) {
                Slog.d(TAG, "noteThermalUsage: status: " + status + ", duration: " + duration2 + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SensorListener implements SensorEventListener {
        private SensorListener() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case 1:
                    BrightnessDataProcessor.this.handleAccSensor(event);
                    return;
                case 5:
                    BrightnessDataProcessor.this.handleLightSensor(event);
                    return;
                case BrightnessDataProcessor.SENSOR_TYPE_LIGHT_FOV /* 33172111 */:
                    BrightnessDataProcessor.this.handleLightFovSensor(event);
                    return;
                default:
                    return;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLightSensor(SensorEvent event) {
        int type;
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null && (type = brightnessChangeItem.type) == 0) {
            this.mPendingBrightnessChangeItem.ambientLux = event.values[0];
            registerSensorByBrightnessType(type, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAccSensor(SensorEvent event) {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null && brightnessChangeItem.type == 2 && event.values.length == 3) {
            this.mPendingBrightnessChangeItem.accValues = event.values;
            registerAccSensor(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLightFovSensor(SensorEvent event) {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null && brightnessChangeItem.type == 2) {
            this.mPendingBrightnessChangeItem.isUseLightFovOptimization = ((int) event.values[1]) != 0;
            registerLightFovSensor(false);
        }
    }

    private void registerSensorByBrightnessType(int type, boolean enable) {
        if (type == 2) {
            registerAccSensor(enable);
            registerLightFovSensor(enable);
        } else if (type == 0) {
            registerLightSensor(enable);
        }
    }

    private void noteDetailThermalUsage(int status, int conditionId, float temperature, long now, boolean isScreenOn) {
        if (this.mLastDetailThermalUsageTimeStamp == 0) {
            this.mLastConditionId = conditionId;
            this.mLastTemperature = temperature;
            this.mLastDetailThermalUsageTimeStamp = now;
        }
        if (isScreenOn) {
            int i = this.mLastConditionId;
            if (i != conditionId) {
                int i2 = this.mThermalStatus;
                if (i2 == 2) {
                    float duration = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                    countThermalUsage(duration, this.mDetailThermalUnrestrictedUsage, i, getTemperatureSpan(this.mLastTemperature));
                    this.mLastConditionId = conditionId;
                    this.mLastDetailThermalUsageTimeStamp = now;
                    aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
                } else if (i2 == 3) {
                    float duration2 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                    countThermalUsage(duration2, this.mDetailThermalRestrictedUsage, i, getTemperatureSpan(this.mLastTemperature));
                    this.mLastConditionId = conditionId;
                    this.mLastDetailThermalUsageTimeStamp = now;
                    aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
                } else if (i2 == 1) {
                    this.mLastConditionId = conditionId;
                    this.mLastDetailThermalUsageTimeStamp = now;
                }
            } else {
                float f = this.mLastTemperature;
                if (f != temperature) {
                    int i3 = this.mThermalStatus;
                    if (i3 == 2) {
                        float duration3 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                        countThermalUsage(duration3, this.mDetailThermalUnrestrictedUsage, i, getTemperatureSpan(f));
                        this.mLastTemperature = temperature;
                        this.mLastDetailThermalUsageTimeStamp = now;
                        aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
                    } else if (i3 == 3) {
                        float duration4 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                        countThermalUsage(duration4, this.mDetailThermalRestrictedUsage, i, getTemperatureSpan(f));
                        this.mLastTemperature = temperature;
                        this.mLastDetailThermalUsageTimeStamp = now;
                        aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
                    } else if (i3 == 1) {
                        this.mLastTemperature = temperature;
                        this.mLastDetailThermalUsageTimeStamp = now;
                    }
                } else {
                    int i4 = this.mThermalStatus;
                    if (i4 != status) {
                        if (i4 == 2) {
                            float duration5 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                            countThermalUsage(duration5, this.mDetailThermalUnrestrictedUsage, i, getTemperatureSpan(f));
                            this.mLastConditionId = conditionId;
                            this.mLastDetailThermalUsageTimeStamp = now;
                            aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
                        } else if (i4 == 3) {
                            float duration6 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                            countThermalUsage(duration6, this.mDetailThermalRestrictedUsage, i, getTemperatureSpan(f));
                            this.mLastConditionId = conditionId;
                            this.mLastDetailThermalUsageTimeStamp = now;
                            aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
                        } else if (i4 == 1) {
                            this.mLastDetailThermalUsageTimeStamp = now;
                        }
                    } else if (i4 == 1) {
                        this.mLastDetailThermalUsageTimeStamp = now;
                    }
                }
            }
        } else {
            int i5 = this.mThermalStatus;
            if (i5 == 2) {
                float duration7 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                countThermalUsage(duration7, this.mDetailThermalUnrestrictedUsage, this.mLastConditionId, getTemperatureSpan(this.mLastTemperature));
                this.mLastDetailThermalUsageTimeStamp = now;
                aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
            } else if (i5 == 3) {
                float duration8 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                countThermalUsage(duration8, this.mDetailThermalRestrictedUsage, this.mLastConditionId, getTemperatureSpan(this.mLastTemperature));
                this.mLastDetailThermalUsageTimeStamp = now;
                aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "noteDetailThermalUsage: status: " + status + ", mThermalStatus: " + this.mThermalStatus + ", conditionId: " + conditionId + ", temperature: " + temperature + ", mLastTemperature: " + this.mLastTemperature + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
        }
    }

    public void noteDetailThermalUsage(final int conditionId, final float temperature) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$noteDetailThermalUsage$4(conditionId, temperature);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteDetailThermalUsage$4(int conditionId, float temperature) {
        if (!this.mScreenOn) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        int i = this.mLastConditionId;
        if (i != conditionId) {
            int i2 = this.mThermalStatus;
            if (i2 == 2) {
                float duration = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                countThermalUsage(duration, this.mDetailThermalUnrestrictedUsage, i, getTemperatureSpan(this.mLastTemperature));
                this.mLastConditionId = conditionId;
                this.mLastDetailThermalUsageTimeStamp = now;
                aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
            } else if (i2 == 3) {
                float duration2 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                countThermalUsage(duration2, this.mDetailThermalRestrictedUsage, i, getTemperatureSpan(this.mLastTemperature));
                this.mLastConditionId = conditionId;
                this.mLastDetailThermalUsageTimeStamp = now;
                aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
            } else if (i2 == 1) {
                this.mLastConditionId = conditionId;
                this.mLastDetailThermalUsageTimeStamp = now;
            }
        } else {
            float f = this.mLastTemperature;
            if (f != temperature) {
                int i3 = this.mThermalStatus;
                if (i3 == 2) {
                    float duration3 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                    countThermalUsage(duration3, this.mDetailThermalUnrestrictedUsage, i, getTemperatureSpan(f));
                    this.mLastTemperature = temperature;
                    this.mLastDetailThermalUsageTimeStamp = now;
                    aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, this.mDetailThermalUnrestrictedUsage);
                } else if (i3 == 3) {
                    float duration4 = ((float) (now - this.mLastDetailThermalUsageTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                    countThermalUsage(duration4, this.mDetailThermalRestrictedUsage, i, getTemperatureSpan(f));
                    this.mLastTemperature = temperature;
                    this.mLastDetailThermalUsageTimeStamp = now;
                    aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_DETAIL_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mDetailThermalRestrictedUsage);
                } else if (i3 == 1) {
                    this.mLastTemperature = temperature;
                    this.mLastDetailThermalUsageTimeStamp = now;
                }
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "noteDetailThermalUsage: thermal state changed, mThermalStatus: " + this.mThermalStatus + ", conditionId: " + conditionId + ", temperature: " + temperature + ", mLastTemperature: " + this.mLastTemperature + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
        }
    }

    private void noteThermalBrightnessRestrictedUsage(float restrictedBrightness, float unrestrictedBrightness, long now, boolean isScreenOn) {
        if (this.mLastBrightnessRestrictedTimeStamp == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            this.mLastUnrestrictedBrightness = unrestrictedBrightness;
            this.mLastRestrictedBrightness = restrictedBrightness;
            this.mLastBrightnessRestrictedTimeStamp = (float) now;
        }
        float f = this.mLastRestrictedBrightness;
        if (f != restrictedBrightness || this.mLastUnrestrictedBrightness != unrestrictedBrightness || !isScreenOn) {
            int i = this.mThermalStatus;
            if (i == 3) {
                float duration = (((float) now) - this.mLastBrightnessRestrictedTimeStamp) / SCREEN_NIT_SPAN_SIX;
                int nit = Math.round(this.mDisplayDeviceConfig.getNitsFromBacklight(f));
                int brightnessSpan = getBrightnessSpanByNit(BrightnessSynchronizer.brightnessFloatToInt(this.mLastUnrestrictedBrightness));
                countThermalBrightnessRestrictedUsage(duration, this.mThermalBrightnessRestrictedUsage, nit, brightnessSpan);
                aggregateThermalEventsSync(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_BRIGHTNESS_RESTRICTED_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, this.mThermalBrightnessRestrictedUsage);
                this.mLastUnrestrictedBrightness = unrestrictedBrightness;
                this.mLastRestrictedBrightness = restrictedBrightness;
                this.mLastBrightnessRestrictedTimeStamp = (float) now;
            } else if (i == 2 || i == 1) {
                this.mLastUnrestrictedBrightness = unrestrictedBrightness;
                this.mLastRestrictedBrightness = restrictedBrightness;
                this.mLastBrightnessRestrictedTimeStamp = (float) now;
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "noteThermalBrightnessRestrictedUsage: restrictedBrightness: " + restrictedBrightness + ", unrestrictedBrightness: " + unrestrictedBrightness + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
        }
    }

    private void countThermalUsage(float duration, SparseArray<SparseArray<Float>> usage, int conditionId, int temperatureSpan) {
        SparseArray<Float> sparseArray = usage.get(conditionId);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
        }
        if (sparseArray.contains(temperatureSpan)) {
            duration += sparseArray.get(temperatureSpan).floatValue();
        }
        sparseArray.put(temperatureSpan, Float.valueOf(duration));
        usage.put(conditionId, sparseArray);
        if (DEBUG) {
            Slog.d(TAG, "countThermalUsage: conditionId: " + conditionId + ", temperatureSpan: " + temperatureSpan + ", duration: " + duration);
        }
    }

    private void countThermalBrightnessRestrictedUsage(float duration, SparseArray<SparseArray<Float>> usage, int nit, int brightnessSpan) {
        SparseArray<Float> sparseArray = usage.get(nit);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
        }
        if (sparseArray.contains(brightnessSpan)) {
            duration += sparseArray.get(brightnessSpan).floatValue();
        }
        sparseArray.put(brightnessSpan, Float.valueOf(duration));
        usage.put(nit, sparseArray);
        if (DEBUG) {
            Slog.d(TAG, "countThermalBrightnessRestrictedUsage: nit: " + nit + ", brightnessSpan: " + brightnessSpan + ", duration: " + duration);
        }
    }

    private void noteOutDoorHighTempUsage(float restrictedBrightness, float brightness, long now, boolean outDoorHighTempState, boolean isPendingReported) {
        if (this.mLastOutDoorHighTemTimeStamp == 0) {
            this.mLastOutDoorHighTempState = outDoorHighTempState;
            this.mLastOutDoorHighTemTimeStamp = now;
        }
        boolean isNaN = Float.isNaN(this.mLastUnrestrictedBrightness);
        float lastRestrictedNit = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        float lastUnrestrictedNit = isNaN ? 0.0f : Math.round(this.mDisplayDeviceConfig.getNitsFromBacklight(this.mLastUnrestrictedBrightness));
        if (!Float.isNaN(this.mLastRestrictedBrightness)) {
            lastRestrictedNit = Math.round(this.mDisplayDeviceConfig.getNitsFromBacklight(this.mLastRestrictedBrightness));
        }
        float lastRestrictedNitByThermal = MathUtils.min(lastUnrestrictedNit, lastRestrictedNit);
        boolean z = this.mLastOutDoorHighTempState;
        if (z != outDoorHighTempState) {
            if (z && lastRestrictedNitByThermal == this.mMinOutDoorHighTempNit) {
                aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_OUTDOOR_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, Float.valueOf(((float) (now - this.mLastOutDoorHighTemTimeStamp)) / SCREEN_NIT_SPAN_SIX));
            } else if (z) {
                float f = this.mMinOutDoorHighTempNit;
                if (lastRestrictedNitByThermal < f && lastRestrictedNit == f) {
                    aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_OUTDOOR_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, Float.valueOf(((float) (now - this.mLastOutDoorHighTemTimeStamp)) / SCREEN_NIT_SPAN_SIX));
                }
            }
            this.mLastOutDoorHighTempState = isPendingReported ? this.mLastOutDoorHighTempState : outDoorHighTempState;
            this.mLastOutDoorHighTemTimeStamp = now;
        } else if (z && (brightness != this.mLastUnrestrictedBrightness || restrictedBrightness != this.mLastRestrictedBrightness)) {
            float duration = this.mMinOutDoorHighTempNit;
            if (lastRestrictedNitByThermal == duration) {
                aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_OUTDOOR_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_RESTRICTED_USAGE_VALUE, Float.valueOf(((float) (now - this.mLastOutDoorHighTemTimeStamp)) / SCREEN_NIT_SPAN_SIX));
            } else if (lastRestrictedNitByThermal < duration && lastRestrictedNit == duration) {
                aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_OUTDOOR_USAGE, AggregationEvent.ThermalAggregationEvent.KEY_UNRESTRICTED_USAGE_VALUE, Float.valueOf((((float) now) - this.mLastBrightnessRestrictedTimeStamp) / SCREEN_NIT_SPAN_SIX));
            }
            this.mLastOutDoorHighTemTimeStamp = now;
        }
        if (DEBUG) {
            Slog.d(TAG, "noteOutDoorHighTempUsage: brightness: " + brightness + ", lastRestrictedNit: " + lastRestrictedNit + ", mLastUnrestrictedBrightness: " + this.mLastUnrestrictedBrightness + ", lastRestrictedNitByThermal: " + lastRestrictedNitByThermal + ", outDoorHighTempState: " + outDoorHighTempState + ", mLastOutDoorHighTempState: " + this.mLastOutDoorHighTempState + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
        }
    }

    public void updateThermalStats(final float brightnessState, final boolean isScreenOn, final float temperature, final boolean needComputed) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$updateThermalStats$5(isScreenOn, temperature, needComputed, brightnessState);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateThermalStats$5(boolean isScreenOn, float temperature, boolean needComputed, float brightnessState) {
        if (this.mLastThermalStatusTimeStamp != 0) {
            long now = SystemClock.elapsedRealtime();
            if (isScreenOn) {
                this.mLastThermalStatusTimeStamp = now;
                this.mLastDetailThermalUsageTimeStamp = now;
                this.mLastBrightnessRestrictedTimeStamp = (float) now;
                this.mLastOutDoorHighTemTimeStamp = now;
                noteAverageTemperatureScreenOn(temperature, needComputed);
                return;
            }
            noteOutDoorHighTempUsage(this.mLastRestrictedBrightness, brightnessState, now, false, false);
            noteThermalBrightnessRestrictedUsage(this.mLastRestrictedBrightness, this.mLastUnrestrictedBrightness, now, false);
            noteDetailThermalUsage(3, this.mLastConditionId, this.mLastTemperature, now, false);
            noteThermalUsage(this.mThermalStatus, now, false);
        }
    }

    public void noteAverageTemperature(final float temperature, final boolean needComputed) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$noteAverageTemperature$6(needComputed, temperature);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteAverageTemperature$6(boolean needComputed, float temperature) {
        if (this.mScreenOn && needComputed) {
            aggregateThermalEventsAvg(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_AVERAGE_TEMPERATURE, AggregationEvent.ThermalAggregationEvent.KEY_AVERAGE_VALUE, Float.valueOf(temperature));
            if (DEBUG) {
                Slog.d(TAG, "noteAverageTemperature: mThermalEventsMap: " + this.mThermalEventsMap.toString());
            }
        }
    }

    private void noteAverageTemperatureScreenOn(float temperature, boolean needComputed) {
        if (needComputed) {
            aggregateThermalEventsAvg(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_AVERAGE_TEMPERATURE, AggregationEvent.ThermalAggregationEvent.KEY_AVERAGE_VALUE, Float.valueOf(temperature));
            if (DEBUG) {
                Slog.d(TAG, "noteAverageTemperature: mThermalEventsMap: " + this.mThermalEventsMap.toString());
            }
        }
    }

    private void startHdyUsageStats(final boolean isHdrLayer) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$startHdyUsageStats$7(isHdrLayer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: noteHdrUsage, reason: merged with bridge method [inline-methods] */
    public void lambda$startHdyUsageStats$7(boolean isHdrLayer) {
        if (this.mIsHdrLayer != isHdrLayer) {
            this.mIsHdrLayer = isHdrLayer;
            long now = SystemClock.elapsedRealtime();
            if (!this.mIsHdrLayer && this.mHdrAppPackageName != null) {
                float duration = ((float) (now - this.mLastHdrEnableTimeStamp)) / SCREEN_NIT_SPAN_SIX;
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_HDR_USAGE, AggregationEvent.BrightnessAggregationEvent.KEY_USAGE_VALUE, Float.valueOf(duration));
                noteHdrAppUsage(duration);
                this.mHdrAppPackageName = null;
                this.mLastHdrEnableTimeStamp = 0L;
                if (DEBUG) {
                    Slog.d(TAG, "noteHdrUsage: mBrightnessEventsMap: " + this.mBrightnessEventsMap.toString());
                    return;
                }
                return;
            }
            this.mHdrAppPackageName = this.mForegroundAppPackageName;
            this.mLastHdrEnableTimeStamp = now;
        }
    }

    private void noteHdrAppUsage(float duration) {
        String str = this.mHdrAppPackageName;
        if (str != null) {
            aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_HDR_USAGE_APP_USAGE, str, Float.valueOf(duration));
        }
    }

    private void noteHbmUsage(float spanIndex, float duration) {
        float transitionBrightness = Float.NaN;
        float[] brightness = this.mDisplayDeviceConfig.getBrightness();
        if (brightness != null) {
            if (brightness.length > 2) {
                transitionBrightness = brightness[brightness.length - 2];
            } else {
                transitionBrightness = brightness[brightness.length - 1];
            }
        }
        if (!Float.isNaN(transitionBrightness)) {
            float spanTransition = getBrightnessSpanByNit(BrightnessSynchronizer.brightnessFloatToInt(transitionBrightness));
            if (spanIndex > spanTransition) {
                aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_HBM_USAGE, Float.valueOf(spanIndex), Float.valueOf(duration));
                if (DEBUG) {
                    Slog.d(TAG, "noteHbmUsage: spanIndex: " + spanIndex + ", spanTransition: " + spanTransition + ", mBrightnessEventsMap: " + this.mBrightnessEventsMap.toString());
                }
            }
        }
    }

    private void noteAdjHighTimesOnBrightnessRestricted(int type, float brightness) {
        if ((type == 0 || type == 1 || type == 2 || type == 3 || type == 4) && this.mThermalStatus == 3 && BrightnessSynchronizer.brightnessFloatToInt(brightness) > this.mLastScreenBrightness) {
            aggregateThermalEventsSum(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_BRIGHTNESS_RESTRICTED_ADJUST_HIGH_TIMES, Integer.valueOf(type), 1);
        }
        if (DEBUG) {
            Slog.d(TAG, "noteAdjHighTimesOnBrightnessRestricted: type: " + type + ", brightness: " + BrightnessSynchronizer.brightnessFloatToInt(brightness) + ", mLastScreenBrightness: " + this.mLastScreenBrightness + ", mThermalEventsMap: " + this.mThermalEventsMap.toString());
        }
    }

    private void updateThermalStatsBeforeReported() {
        if (this.mLastThermalStatusTimeStamp != 0) {
            long now = SystemClock.elapsedRealtime();
            noteOutDoorHighTempUsage(this.mLastRestrictedBrightness, -1.0f, now, this.mLastOutDoorHighTempState, true);
            noteThermalBrightnessRestrictedUsage(this.mLastRestrictedBrightness, this.mLastUnrestrictedBrightness, now, false);
            noteDetailThermalUsage(1, this.mLastConditionId, this.mLastTemperature, now, false);
            noteThermalUsage(this.mThermalStatus, now, false);
            if (!this.mThermalEventsMap.containsKey(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_AVERAGE_TEMPERATURE)) {
                aggregateThermalEventsAvg(AggregationEvent.ThermalAggregationEvent.EVENT_THERMAL_AVERAGE_TEMPERATURE, AggregationEvent.ThermalAggregationEvent.KEY_AVERAGE_VALUE, Float.valueOf(this.mLastTemperature));
            }
            if (DEBUG) {
                Slog.d(TAG, "updateThermalStatsBeforeReported: update stats before reporting stats.");
            }
        }
    }

    private void reportThermalEvents() {
        updateThermalStatsBeforeReported();
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportAggregatedEventsToServer(this.mContext, this.mThermalEventsMap, OneTrackUploaderHelper.THERMAL_AGGREGATION_EVENT_NAME, this.mExpId);
            if (DEBUG) {
                Slog.d(TAG, "reportThermalEvents: mThermalEventsMap: " + this.mThermalEventsMap.toString());
            }
        }
        resetThermalEventsCache();
    }

    private void resetThermalEventsCache() {
        this.mThermalEventsMap.clear();
        this.mDetailThermalUnrestrictedUsage.clear();
        this.mDetailThermalRestrictedUsage.clear();
        this.mThermalBrightnessRestrictedUsage.clear();
    }

    private void noteHdrUsageBeforeReported() {
        long now = SystemClock.elapsedRealtime();
        if (this.mIsHdrLayer && this.mHdrAppPackageName != null) {
            float duration = ((float) (now - this.mLastHdrEnableTimeStamp)) / SCREEN_NIT_SPAN_SIX;
            aggregateBrightnessEventsSum(AggregationEvent.BrightnessAggregationEvent.EVENT_HDR_USAGE, AggregationEvent.BrightnessAggregationEvent.KEY_USAGE_VALUE, Float.valueOf(duration));
            noteHdrAppUsage(duration);
            this.mLastHdrEnableTimeStamp = now;
            if (DEBUG) {
                Slog.d(TAG, "noteHdrUsageBeforeReported: mBrightnessEventsMap: " + this.mBrightnessEventsMap.toString());
            }
        }
    }

    private void registerLightSensor(boolean enable) {
        boolean z = this.mLightSensorEnabled;
        if (!z && enable) {
            this.mSensorManager.registerListener(this.mSensorListener, this.mLightSensor, 3, this.mBackgroundHandler);
            this.mLightSensorEnabled = true;
            if (DEBUG) {
                Slog.d(TAG, "registerLightSensor: register light sensor.");
                return;
            }
            return;
        }
        if (z && !enable) {
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mLightSensor);
            this.mLightSensorEnabled = false;
            if (DEBUG) {
                Slog.d(TAG, "registerLightSensor: unregister light sensor.");
            }
        }
    }

    private void registerAccSensor(boolean enable) {
        boolean z = this.mAccSensorEnabled;
        if (!z && enable) {
            this.mSensorManager.registerListener(this.mSensorListener, this.mAccSensor, 3, this.mBackgroundHandler);
            this.mAccSensorEnabled = true;
            if (DEBUG) {
                Slog.d(TAG, "registerAccSensor: register acc sensor.");
                return;
            }
            return;
        }
        if (z && !enable) {
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mAccSensor);
            this.mAccSensorEnabled = false;
            if (DEBUG) {
                Slog.d(TAG, "registerAccSensor: unregister acc sensor.");
            }
        }
    }

    private void registerLightFovSensor(boolean enable) {
        Sensor sensor = this.mLightFovSensor;
        if (sensor != null && !this.mLightFovSensorEnabled && enable) {
            this.mSensorManager.registerListener(this.mSensorListener, sensor, 3, this.mBackgroundHandler);
            this.mLightFovSensorEnabled = true;
            if (DEBUG) {
                Slog.d(TAG, "registerLightFovSensor: register light fov sensor.");
                return;
            }
            return;
        }
        if (sensor != null && this.mLightFovSensorEnabled && !enable) {
            this.mSensorManager.unregisterListener(this.mSensorListener, sensor);
            this.mLightFovSensorEnabled = false;
            if (DEBUG) {
                Slog.d(TAG, "registerLightFovSensor: unregister light fov sensor.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportDisabledAutoBrightnessEvent() {
        if (this.mScreenOn) {
            boolean z = this.mLastAutoBrightnessEnable;
            if (z && !this.mAutoBrightnessEnable) {
                this.mBackgroundHandler.removeMessages(13);
                BrightnessEvent event = new BrightnessEvent();
                long now = System.currentTimeMillis();
                boolean brightnessRestricted = this.mThermalStatus == 3;
                event.setEventType(8).setTimeStamp(now).setActualNit(this.mActualNit).setOriginalNit(this.mOriginalNit).setForegroundPackage(this.mForegroundAppPackageName).setAffectFactorFlag(convergeAffectFactors()).setOrientation(this.mOrientation).setUserId(this.mCurrentUserId).setHdrLayerEnable(this.mIsHdrLayer).setBrightnessRestrictedEnable(brightnessRestricted).setAmbientLux(this.mLastAmbientLux).setLuxSpanIndex(getAmbientLuxSpanIndex(this.mLastAmbientLux)).setLastMainAmbientLux(this.mLastMainAmbientLux).setLastAssistAmbientLux(this.mLastAssistAmbientLux).setSwitchStats(this.mSwitchStatsHelper.getAllSwitchStats());
                Message msg = this.mBackgroundHandler.obtainMessage(13);
                msg.obj = event;
                this.mBackgroundHandler.sendMessageDelayed(msg, DEBOUNCE_REPORT_DISABLE_AUTO_BRIGHTNESS_EVENT);
                if (DEBUG) {
                    Slog.d(TAG, "reportDisabledAutoBrightnessEvent: event: " + event.toString());
                    return;
                }
                return;
            }
            if (!z && this.mAutoBrightnessEnable && this.mBackgroundHandler.hasMessages(13)) {
                this.mBackgroundHandler.removeMessages(13);
                if (DEBUG) {
                    Slog.d(TAG, "reportDisabledAutoBrightnessEvent: remove message due to enabled auto brightness.");
                }
            }
        }
    }

    public void setExpId(int id) {
        this.mExpId = id;
    }

    private void updateScreenNits(final float originalNit, final float actualNit) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$updateScreenNits$8(originalNit, actualNit);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateScreenNits$8(float originalNit, float actualNit) {
        this.mOriginalNit = originalNit;
        this.mActualNit = actualNit;
        if (DEBUG) {
            Slog.d(TAG, "updateScreenNits: mOriginalNit: " + this.mOriginalNit + ", mActualNit: " + this.mActualNit);
        }
    }

    private float clampBrightness(float brightness) {
        if (Float.isNaN(brightness)) {
            return -1.0f;
        }
        return brightness;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$forceReportTrainDataEnabled$9(boolean enable) {
        this.mForcedReportTrainDataEnabled = enable;
    }

    public void forceReportTrainDataEnabled(final boolean enable) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$forceReportTrainDataEnabled$9(enable);
            }
        });
    }

    private AggregationEvent getCbmAggregationEvent(String keySubEvent) {
        AggregationEvent event = this.mCbmEventsMap.get(keySubEvent);
        if (event == null) {
            return new AggregationEvent.CbmAggregationEvent();
        }
        return event;
    }

    private void aggregateCbmEventsSync(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getCbmAggregationEvent(keySubEvent);
        event.getQuotaEvents().put(key, value);
        this.mCbmEventsMap.put(keySubEvent, event);
    }

    private void aggregateCbmEventsSum(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getCbmAggregationEvent(keySubEvent);
        statsSummary(event.getQuotaEvents(), key, value);
        this.mCbmEventsMap.put(keySubEvent, event);
    }

    private void aggregateCbmEventsAvg(String keySubEvent, Object key, Object value) {
        AggregationEvent event = getCbmAggregationEvent(keySubEvent);
        statsAverageValues(event, key, (Float) value);
        this.mCbmEventsMap.put(keySubEvent, event);
    }

    public void aggregateCbmBrightnessAdjustTimes(int cbmState, boolean isManuallySet) {
        String name = AggregationEvent.CbmAggregationEvent.getCbmAutoAdjustQuotaName(cbmState, isManuallySet);
        aggregateCbmEventsSum("custom_brightness_adj", name, 1);
        if (DEBUG) {
            Slog.d(TAG, "aggregateCbmBrightnessAdjustTimes: mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateCbmBrightnessUsageDuration(float duration, int cbmSate) {
        float duration2 = duration / SCREEN_NIT_SPAN_SIX;
        String name = AggregationEvent.CbmAggregationEvent.getCbmBrtUsageQuotaName(cbmSate);
        aggregateCbmEventsSum("custom_brightness_usage", name, Float.valueOf(duration2));
        if (DEBUG) {
            Slog.d(TAG, "aggregateCbmBrightnessUsageDuration: duration: " + duration2 + ", mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateIndividualModelTrainTimes() {
        aggregateCbmEventsSum("individual_model_train", "model_train_count", 1);
        if (DEBUG) {
            Slog.d(TAG, "aggregateIndividualModelTrainTimes: mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateIndividualModelTrainTimes(final boolean isSuccessful) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$aggregateIndividualModelTrainTimes$10(isSuccessful);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$aggregateIndividualModelTrainTimes$10(boolean isSuccessful) {
        String name = isSuccessful ? "model_validation_success_count" : "model_validation_fail_count";
        aggregateCbmEventsSum("individual_model_train", name, 1);
        if (DEBUG) {
            Slog.d(TAG, "aggregateIndividualModelTrainTimes: isSuccessful: " + isSuccessful + ", mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateModelPredictTimeoutTimes() {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.BrightnessDataProcessor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessDataProcessor.this.lambda$aggregateModelPredictTimeoutTimes$11();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$aggregateModelPredictTimeoutTimes$11() {
        aggregateCbmEventsSum("individual_model_predict", "model_predict_timeout_count", 1);
        if (DEBUG) {
            Slog.d(TAG, "aggregateModelPredictTimeoutTimes: mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateModelAvgPredictDuration(float duration) {
        aggregateCbmEventsAvg("individual_model_predict", "model_predict_average_duration", Float.valueOf(duration));
        if (DEBUG) {
            Slog.d(TAG, "aggregateModelAvgPredictDuration: duration: " + duration + ", mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    public void aggregateModelTrainIndicators(IndividualTrainEvent event) {
        Map<String, Object> indicatorsMap = new HashMap<>();
        long now = System.currentTimeMillis();
        indicatorsMap.put("sample_num", Integer.valueOf(event.getTrainSampleNum()));
        indicatorsMap.put("train_loss", Float.valueOf(event.getModelTrainLoss()));
        indicatorsMap.put("pre_train_mae", Float.valueOf(event.getPreModelMAE()));
        indicatorsMap.put("cur_train_mae", Float.valueOf(event.getCurModelMAE()));
        indicatorsMap.put("pre_train_mape", Float.valueOf(event.getPreModelMAPE()));
        indicatorsMap.put("cur_train_mape", Float.valueOf(event.getCurModelMAPE()));
        indicatorsMap.put("timestamp", BrightnessEvent.timestamp2String(now));
        this.mModelTrainIndicatorsList.add(indicatorsMap);
        aggregateCbmEventsSync("individual_model_train", "model_train_indicators", this.mModelTrainIndicatorsList);
        if (DEBUG) {
            Slog.d(TAG, "aggregateModelTrainIndicators: mCbmEventsMap: " + this.mCbmEventsMap);
        }
    }

    private void reportCustomBrightnessEvents() {
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportAggregatedEventsToServer(this.mContext, this.mCbmEventsMap, OneTrackUploaderHelper.CUSTOM_BRIGHTNESS_AGGREGATION_EVENT_NAME, this.mExpId);
            if (DEBUG) {
                Slog.d(TAG, "reportCustomBrightnessEvents: mCbmEventsMap: " + this.mCbmEventsMap.toString());
            }
        }
        this.mCbmEventsMap.clear();
        this.mModelTrainIndicatorsList.clear();
    }

    public void notifyAonFlareEvents(int type, float preLux) {
        switch (type) {
            case 1:
                if (DEBUG) {
                    Slog.i(TAG, "notifyAonFlareEvents: preLux: " + preLux);
                }
                aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_SCENE_CHECK_TIMES, "1", 1);
                aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_SUPPRESS_DARKEN_LUX_SPAN, Integer.valueOf(getAmbientLuxSpanIndex(preLux)), 1);
                aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_SUPPRESS_DARKEN_HOUR, Integer.valueOf(getHourFromTimestamp(System.currentTimeMillis())), 1);
                this.mIsAonSuppressDarken = true;
                this.mBackgroundHandler.removeMessages(14);
                this.mBackgroundHandler.sendEmptyMessageDelayed(14, 60000L);
                return;
            case 2:
                aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_SCENE_CHECK_TIMES, "2", 1);
                this.mIsNotAonSuppressDarken = true;
                this.mBackgroundHandler.removeMessages(15);
                this.mBackgroundHandler.sendEmptyMessageDelayed(15, 60000L);
                return;
            case 3:
                aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_SCENE_CHECK_TIMES, "3", 1);
                this.mIsNotAonSuppressDarken = true;
                this.mBackgroundHandler.removeMessages(15);
                this.mBackgroundHandler.sendEmptyMessageDelayed(15, 60000L);
                return;
            default:
                return;
        }
    }

    public void notifyUpdateBrightness() {
        this.mIsAonSuppressDarken = false;
        this.mIsNotAonSuppressDarken = false;
    }

    private void aggregateAonFlareEventsSum(String keySubEvent, Object key, Object value) {
        AggregationEvent event = this.mAonFlareEventsMap.get(keySubEvent);
        if (event == null) {
            event = new AggregationEvent.AonFlareAggregationEvent();
        }
        statsSummary(event.getQuotaEvents(), key, value);
        this.mAonFlareEventsMap.put(keySubEvent, event);
    }

    private void reportAonFlareEvents() {
        if (!this.mIsMiShow) {
            OneTrackUploaderHelper.reportAggregatedEventsToServer(this.mContext, this.mAonFlareEventsMap, OneTrackUploaderHelper.AON_FLARE_AGGREGATION_EVENT_NAME, this.mExpId);
            if (DEBUG) {
                Slog.d(TAG, "reportAonFlareEvents: mAonFlareEventsMap: " + this.mAonFlareEventsMap.toString());
            }
        }
        this.mAonFlareEventsMap.clear();
    }

    private int getHourFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.get(11);
    }

    private void flareStatisticalManualAdjustTimes(boolean isIncrease) {
        String str;
        String str2;
        if (DEBUG) {
            Slog.i(TAG, "manual adjust brightness: mIsAonSuppressDarken: " + this.mIsAonSuppressDarken + ", mIsNotAonSuppressDarken: " + this.mIsNotAonSuppressDarken + ", isIncrease:" + isIncrease);
        }
        if (this.mIsAonSuppressDarken) {
            this.mIsAonSuppressDarken = false;
            if (isIncrease) {
                str2 = "2";
            } else {
                str2 = "1";
            }
            aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_MANUAL_ADJUST_TIMES, str2, 1);
        }
        if (this.mIsNotAonSuppressDarken) {
            this.mIsNotAonSuppressDarken = false;
            if (isIncrease) {
                str = "4";
            } else {
                str = "3";
            }
            aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_MANUAL_ADJUST_TIMES, str, 1);
        }
    }

    private void flareStatisticalResetBrightnessModeTimes() {
        if (DEBUG) {
            Slog.i(TAG, "reset brightness mode: mIsAonSuppressDarken: " + this.mIsAonSuppressDarken + ", mIsNotAonSuppressDarken: " + this.mIsNotAonSuppressDarken);
        }
        if (this.mIsAonSuppressDarken) {
            this.mIsAonSuppressDarken = false;
            aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_USER_RESET_BRIGHTNESS_MODE_TIMES, "1", 1);
        }
        if (this.mIsNotAonSuppressDarken) {
            this.mIsNotAonSuppressDarken = false;
            aggregateAonFlareEventsSum(AggregationEvent.AonFlareAggregationEvent.EVENT_FLARE_USER_RESET_BRIGHTNESS_MODE_TIMES, "2", 1);
        }
    }
}
