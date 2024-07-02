package com.android.server.display.aiautobrt;

import android.R;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.hardware.display.BrightnessConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.os.BackgroundThread;
import com.android.server.display.BrightnessMappingStrategy;
import com.android.server.display.DisplayDebugConfig;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.DisplayPowerControllerImpl;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.display.aiautobrt.IndividualBrightnessEngine;
import com.android.server.display.statistics.BrightnessDataProcessor;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.xiaomi.aiautobrt.IndividualModelEvent;
import com.xiaomi.aiautobrt.IndividualTrainEvent;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class CustomBrightnessModeController implements BrightnessDataProcessor.ModelEventCallback, MiuiDisplayCloudController.CloudListener, IndividualBrightnessEngine.EngineCallback {
    private static final float BRIGHT_ROOM_AMBIENT_LUX = 600.0f;
    public static final String CUSTOM_BRIGHTNESS_CURVE_BRIGHTENING = "custom_brightness_curve_brightening";
    public static final String CUSTOM_BRIGHTNESS_CURVE_DARKENING = "custom_brightness_curve_darkening";
    public static final String CUSTOM_BRIGHTNESS_CURVE_DEFAULT = "custom_brightness_curve_default";
    public static final int CUSTOM_BRIGHTNESS_MODE_CUSTOM = 1;
    public static final int CUSTOM_BRIGHTNESS_MODE_INDIVIDUAL = 2;
    private static final int CUSTOM_BRIGHTNESS_MODE_INVALID = -1;
    public static final int CUSTOM_BRIGHTNESS_MODE_OFF = 0;
    protected static final String CUSTOM_CURVE_STATE_REASON_BACKUP = "backup";
    protected static final String CUSTOM_CURVE_STATE_REASON_BEST_INDICATOR = "best_indicator";
    protected static final String CUSTOM_CURVE_STATE_REASON_DEFAULT = "default";
    protected static final String CUSTOM_CURVE_STATE_REASON_FORCED = "forced_operate";
    protected static final String CUSTOM_CURVE_STATE_REASON_USER = "user_operate";
    private static final float DARK_ROOM_AMBIENT_LUX = 100.0f;
    private static final int EXPERIMENT_FLAG_CUSTOM_CURVE = 1;
    private static final int EXPERIMENT_FLAG_INDIVIDUAL_MODEL = 2;
    private static final int EXPERIMENT_FLAG_INVALID = -1;
    private static final int FORCE_TRAIN_DISABLE_VALUE = 0;
    private static final int FORCE_TRAIN_ENABLE_VALUE = 1;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final String KEY_CUSTOM_BRIGHTNESS_MODE = "custom_brightness_mode";
    private static final String KEY_FORCE_TRAIN_ENABLE = "force_train_enable";
    private static final float LUX_GRAD_SMOOTHING = 0.25f;
    public static final int MAX_CUSTOM_CURVE_VALID_ADJ_TIMES = 5;
    private static final float MAX_GRAD = 1.0f;
    private static final float MIN_PERMISSIBLE_INCREASE = 0.004f;
    private static final float MIN_SCOPE_DELTA_NIT = 100.0f;
    private static final int MSG_CLEAR_PREDICT_PENDING = 1;
    public static final int SCENE_STATE_BRIGHTENING = 1;
    public static final int SCENE_STATE_DARKENING = 2;
    public static final int SCENE_STATE_DEFAULT = 0;
    public static final int SCENE_STATE_INVALID = -1;
    private static final float SHORT_TERM_MODEL_THRESHOLD_RATIO = 0.6f;
    protected static final String TAG = "CbmController";
    private static final int WAIT_FOR_PREDICT_TIMEOUT = 200;
    protected static boolean sDebug;
    private final AppClassifier mAppClassifier;
    private boolean mAutoBrightnessModeEnabled;
    private final Handler mBgHandler;
    private BrightnessConfiguration mBrighteningConfiguration;
    private final BrightnessDataProcessor mBrightnessDataProcessor;
    private BrightnessMappingStrategy mBrightnessMapper;
    private int mCachedCategoryId;
    private int mCachedLuxIndex;
    private final CbmStateTracker mCbmStateTracker;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private boolean mCustomCurveCloudDisable;
    private boolean mCustomCurveDisabledByUserChange;
    private boolean mCustomCurveValid;
    private BrightnessConfiguration mDarkeningConfiguration;
    private final CustomPersistentDataStore mDataStore;
    private BrightnessConfiguration mDefaultConfiguration;
    private final float[] mDefaultLuxLevels;
    private final float[] mDefaultNitsLevels;
    private final DisplayDeviceConfig mDisplayDeviceConfig;
    private final DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private boolean mForcedCustomCurveEnabled;
    private boolean mForcedIndividualBrightnessEnabled;
    private final CbmHandler mHandler;
    private final float mHbmMinimumLux;
    private IndividualBrightnessEngine mIndividualBrightnessEngine;
    private final ComponentName mIndividualComponentName;
    private Spline mIndividualDefaultSpline;
    private boolean mIndividualDisabledByUserChange;
    private Spline mIndividualGameSpline;
    private boolean mIndividualModelCloudDisable;
    private Spline mIndividualVideoSpline;
    private boolean mIsCustomCurveValidReady;
    private long mLastModelTrainTimeStamp;
    private final float mMinBrightness;
    private Spline mMinimumBrightnessSpline;
    private long mModelTrainTotalTimes;
    private final float mNormalMaxBrightness;
    private final SettingsObserver mSettingsObserver;
    private final long mShortTermModelTimeout;
    private boolean mSupportCustomBrightness;
    private boolean mSupportIndividualBrightness;
    private final Map<Integer, float[]> mBrightnessValidationMapper = new HashMap();
    private int mCbmState = -1;
    private int mCurrentCustomSceneState = -1;
    private float mPendingIndividualBrightness = Float.NaN;
    private String mCustomCurveValidStateReason = "default";
    private volatile boolean mIndividualModelExperimentEnable = true;
    private volatile boolean mCustomCurveExperimentEnable = true;
    private Runnable mNotifyModelVerificationRunnable = new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            CustomBrightnessModeController.this.lambda$new$0();
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        this.mCachedCategoryId = 0;
        this.mCachedLuxIndex = 0;
        this.mIndividualBrightnessEngine.completeModelValidation();
        Slog.e(TAG, "Model cannot complete verification due to predict time out.");
    }

    public CustomBrightnessModeController(Context context, Looper looper, DisplayDeviceConfig config, DisplayPowerControllerImpl dpcImpl, BrightnessDataProcessor processor, float hbmMinimumLux, float normalMaxBrightness, float minBrightness) {
        this.mContext = context;
        this.mDisplayDeviceConfig = config;
        this.mDisplayPowerControllerImpl = dpcImpl;
        this.mBrightnessDataProcessor = processor;
        processor.setModelEventCallback(this);
        this.mHbmMinimumLux = hbmMinimumLux;
        this.mNormalMaxBrightness = normalMaxBrightness;
        this.mMinBrightness = minBrightness;
        CbmHandler cbmHandler = new CbmHandler(looper);
        this.mHandler = cbmHandler;
        Handler handler = new Handler(BackgroundThread.getHandler().getLooper());
        this.mBgHandler = handler;
        this.mAppClassifier = AppClassifier.getInstance();
        this.mCbmStateTracker = new CbmStateTracker(context, handler, cbmHandler, this);
        Resources resources = context.getResources();
        String componentName = resources.getString(286195878);
        float[] luxLevels = getLuxLevels(resources.getIntArray(R.array.config_biometric_sensors));
        this.mDefaultLuxLevels = luxLevels;
        this.mDefaultNitsLevels = getFloatArray(resources.obtainTypedArray(R.array.config_autoBrightnessLevels));
        float[] customDefaultLuxLevels = getLuxLevels(resources.getIntArray(285409338));
        float[] customDefaultNitsLevels = getFloatArray(resources.obtainTypedArray(285409339));
        float[] brighteningNitsLevels = getFloatArray(resources.obtainTypedArray(285409332));
        float[] darkeningNitsLevels = getFloatArray(resources.obtainTypedArray(285409337));
        float[] minLuxLevel = getFloatArray(resources.obtainTypedArray(285409345));
        float[] minNitsLevel = getFloatArray(resources.obtainTypedArray(285409346));
        this.mShortTermModelTimeout = resources.getInteger(R.integer.config_burnInProtectionMaxRadius);
        this.mSupportIndividualBrightness = context.getResources().getBoolean(285540403);
        this.mSupportCustomBrightness = context.getResources().getBoolean(285540375);
        if (isValidMapping(minLuxLevel, minNitsLevel)) {
            this.mMinimumBrightnessSpline = Spline.createSpline(minLuxLevel, minNitsLevel);
        }
        if (isValidMapping(customDefaultLuxLevels, customDefaultNitsLevels) && isValidMapping(customDefaultLuxLevels, brighteningNitsLevels) && isValidMapping(customDefaultLuxLevels, darkeningNitsLevels)) {
            this.mDefaultConfiguration = build(customDefaultLuxLevels, customDefaultNitsLevels, CUSTOM_BRIGHTNESS_CURVE_DEFAULT);
            this.mBrighteningConfiguration = build(customDefaultLuxLevels, brighteningNitsLevels, CUSTOM_BRIGHTNESS_CURVE_BRIGHTENING);
            this.mDarkeningConfiguration = build(customDefaultLuxLevels, darkeningNitsLevels, CUSTOM_BRIGHTNESS_CURVE_DARKENING);
        }
        processor.setBrightnessConfiguration(this.mDefaultConfiguration, this.mDarkeningConfiguration, this.mBrighteningConfiguration);
        ComponentName individualComponent = getIndividualComponent(componentName);
        this.mIndividualComponentName = individualComponent;
        this.mIndividualBrightnessEngine = new IndividualBrightnessEngine(context, processor.mIndividualEventNormalizer, looper, individualComponent, this, handler);
        this.mSettingsObserver = new SettingsObserver(cbmHandler);
        this.mContentResolver = context.getContentResolver();
        CustomPersistentDataStore customPersistentDataStore = new CustomPersistentDataStore(this, luxLevels);
        this.mDataStore = customPersistentDataStore;
        this.mIndividualBrightnessEngine.setDataStore(customPersistentDataStore);
        registerSettingsObserver();
        loadSettings();
    }

    private void registerSettingsObserver() {
        this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_CUSTOM_BRIGHTNESS_MODE), false, this.mSettingsObserver, -1);
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
    }

    private void loadSettings() {
        updateCustomBrightnessEnabled();
        updateScreenBrightnessMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCustomBrightnessEnabled() {
        int value = Settings.Secure.getIntForUser(this.mContentResolver, KEY_CUSTOM_BRIGHTNESS_MODE, 0, -2);
        boolean forcedEnableCustomCurve = value == 1;
        if (this.mForcedCustomCurveEnabled != forcedEnableCustomCurve) {
            this.mForcedCustomCurveEnabled = forcedEnableCustomCurve;
            setCustomCurveValid(forcedEnableCustomCurve, CUSTOM_CURVE_STATE_REASON_FORCED);
        }
        boolean forcedEnableModel = value == 2;
        if (this.mForcedIndividualBrightnessEnabled != forcedEnableModel) {
            this.mForcedIndividualBrightnessEnabled = forcedEnableModel;
            this.mIndividualBrightnessEngine.setModelValid(forcedEnableModel, CUSTOM_CURVE_STATE_REASON_FORCED);
        }
        if (forcedEnableCustomCurve || forcedEnableModel) {
            resetCustomCurveValidConditions();
        }
        if (forcedEnableModel) {
            disableIndividualEngine(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScreenBrightnessMode() {
        this.mAutoBrightnessModeEnabled = Settings.System.getIntForUser(this.mContentResolver, "screen_brightness_mode", 0, -2) == 1;
    }

    private ComponentName getIndividualComponent(String name) {
        if (name != null) {
            return ComponentName.unflattenFromString(name);
        }
        return null;
    }

    public void onBootCompleted() {
        this.mIndividualBrightnessEngine.onBootCompleted();
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                CustomBrightnessModeController.this.lambda$onBootCompleted$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBootCompleted$1() {
        UserBrightnessStatsJob.scheduleJob(this.mContext);
    }

    public float getCustomBrightness(float lux, String packageName, int category, float oldAutoBrightness, float newAutoBrightness, int orientation, boolean isManuallySet) {
        int newCbmState;
        float customBrightness = newAutoBrightness;
        int i = this.mCbmState;
        if (isIndividualAllowed()) {
            customBrightness = getIndividualBrightness(packageName, lux);
            newCbmState = 2;
        } else if (isCustomAllowed()) {
            customBrightness = this.mBrightnessMapper.getBrightness(lux, packageName, category);
            newCbmState = 1;
        } else {
            newCbmState = 0;
        }
        if (newCbmState != this.mCbmState) {
            updateCbmState(newCbmState);
        }
        if (0 == 0 && !isManuallySet && customBrightness != oldAutoBrightness && !Float.isNaN(oldAutoBrightness)) {
            this.mCbmStateTracker.noteAutoAdjustmentTimes(newCbmState);
            final int finalNewCbmState = newCbmState;
            this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    CustomBrightnessModeController.this.lambda$getCustomBrightness$2(finalNewCbmState);
                }
            });
        }
        if (sDebug || (customBrightness != newAutoBrightness && customBrightness != oldAutoBrightness)) {
            Slog.i(TAG, "getCustomBrightness: previous: " + oldAutoBrightness + ", config: " + newAutoBrightness + ", custom: " + customBrightness + ", packageName: " + packageName + ", orientation: " + orientation);
        }
        return customBrightness;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getCustomBrightness$2(int finalNewCbmState) {
        noteBrightnessAdjustTimesToAggregate(finalNewCbmState, false);
    }

    private void updateCbmState(int cbmState) {
        int oldState = this.mCbmState;
        this.mCbmState = cbmState;
        long now = SystemClock.elapsedRealtime();
        if (oldState != -1) {
            this.mCbmStateTracker.noteStopCbmStateTracking(oldState, now);
        }
        if (this.mCbmState != -1) {
            this.mCbmStateTracker.noteStartCbmStateTracking(cbmState, now);
        }
    }

    private boolean isCustomAllowed() {
        return this.mSupportCustomBrightness && !this.mCustomCurveDisabledByUserChange && this.mBrightnessMapper != null && this.mCustomCurveValid && !this.mCustomCurveCloudDisable && this.mCustomCurveExperimentEnable;
    }

    private boolean isIndividualAllowed() {
        return this.mSupportIndividualBrightness && this.mIndividualComponentName != null && !this.mIndividualDisabledByUserChange && this.mIndividualBrightnessEngine.isModelValid() && !this.mIndividualModelCloudDisable && this.mIndividualModelExperimentEnable && isValidIndividualSpline();
    }

    public void updateCustomSceneState(String packageName, int orientation) {
        BrightnessConfiguration config;
        if (isCustomAllowed()) {
            boolean changed = hasSceneStateChanged(packageName, orientation);
            if (changed) {
                int i = this.mCurrentCustomSceneState;
                if (i == 1) {
                    config = this.mBrighteningConfiguration;
                } else if (i == 2) {
                    config = this.mDarkeningConfiguration;
                } else {
                    config = this.mDefaultConfiguration;
                }
                this.mDisplayPowerControllerImpl.setBrightnessConfiguration(config);
                Slog.d(TAG, "updateCustomSceneState: config changed, config: " + config.toString());
            }
        }
    }

    private boolean hasSceneStateChanged(String packageName, int orientation) {
        int state = getSceneState(packageName, orientation);
        if (this.mCurrentCustomSceneState != state) {
            this.mCurrentCustomSceneState = state;
            return true;
        }
        return false;
    }

    private int getSceneState(String packageName, int orientation) {
        int clock = LocalDateTime.now().getHour();
        int category = this.mAppClassifier.getAppCategoryId(packageName);
        int scene = 0;
        if (isBrighteningScene(category, orientation, clock)) {
            scene = 1;
        } else if (isDarkeningScene(category, orientation, clock)) {
            scene = 2;
        }
        if (this.mCurrentCustomSceneState != scene) {
            Slog.d(TAG, "getSceneState: category: " + AppClassifier.categoryToString(category) + ", scene state: " + scene + ", packageName: " + packageName + ", clock: " + clock);
        }
        return scene;
    }

    private boolean isBrighteningScene(int category, int orientation, int clock) {
        if (category == 7) {
            return true;
        }
        if (isNoonTime(clock) || isDayTime(clock)) {
            if (isHorizontalScreen(orientation) && (category == 1 || category == 9)) {
                return true;
            }
            if (isVerticalScreen(orientation) && category == 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isDarkeningScene(int category, int orientation, int clock) {
        return category == 4 || (isVerticalScreen(orientation) && ((isNightTime(clock) && (category == 5 || category == 9 || category == 3)) || ((isEarlyMorningTime(clock) && (category == 9 || category == 3)) || (isNoonTime(clock) && category == 9))));
    }

    public void setAutoBrightnessComponent(BrightnessMappingStrategy mapper) {
        this.mBrightnessMapper = mapper;
    }

    protected void disableCustomCurve(boolean disable) {
        boolean changed = false;
        if (disable && !this.mCustomCurveDisabledByUserChange) {
            this.mCustomCurveDisabledByUserChange = true;
            this.mDisplayPowerControllerImpl.setBrightnessConfiguration(null);
            changed = true;
        } else if (!disable && this.mCustomCurveDisabledByUserChange) {
            this.mCustomCurveDisabledByUserChange = false;
            changed = true;
        }
        if (changed) {
            Slog.d(TAG, (this.mCustomCurveDisabledByUserChange ? "disable " : "enable ") + "custom curve.");
        }
    }

    protected void disableIndividualEngine(boolean disable) {
        boolean changed = false;
        if (disable && !this.mIndividualDisabledByUserChange) {
            this.mIndividualDisabledByUserChange = true;
            changed = true;
        } else if (!disable && this.mIndividualDisabledByUserChange) {
            this.mIndividualDisabledByUserChange = false;
            changed = true;
        }
        if (changed) {
            Slog.d(TAG, (this.mIndividualDisabledByUserChange ? "disable " : "enable ") + "individual engine.");
        }
    }

    public void setScreenBrightnessByUser(float userDataPoint, boolean isBrightening, String packageName, int orientation) {
        if (this.mSupportIndividualBrightness) {
            this.mIndividualBrightnessEngine.bindServiceDueToBrightnessAdjust(true);
        }
        if (this.mIndividualBrightnessEngine.isModelValid() || this.mForcedIndividualBrightnessEnabled) {
            disableIndividualEngine(true);
        }
        this.mCbmStateTracker.noteManualAdjustmentTimes(this.mCbmState);
        final int finalCbmState = this.mCbmState;
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                CustomBrightnessModeController.this.lambda$setScreenBrightnessByUser$3(finalCbmState);
            }
        });
        if (!this.mIndividualBrightnessEngine.isModelValid() && this.mCbmState == 0 && !this.mCustomCurveValid && !this.mIsCustomCurveValidReady && this.mCustomCurveExperimentEnable) {
            if (isDarkRoomAdjustment(userDataPoint, isBrightening)) {
                this.mCbmStateTracker.noteManualAdjustmentTimes(this.mCbmState, 1);
                this.mCbmStateTracker.startEvaluateCustomCurve();
            } else if (isBrightRoomAdjustment(userDataPoint, packageName, orientation, isBrightening)) {
                this.mCbmStateTracker.noteManualAdjustmentTimes(this.mCbmState, 2);
                this.mCbmStateTracker.startEvaluateCustomCurve();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setScreenBrightnessByUser$3(int finalCbmState) {
        noteBrightnessAdjustTimesToAggregate(finalCbmState, true);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void customCurveConditionsSatisfied() {
        if (!this.mIsCustomCurveValidReady) {
            this.mIsCustomCurveValidReady = true;
            Slog.d(TAG, "Satisfy valid conditions of custom curve.");
        }
    }

    public void resetShortTermModel(boolean manually) {
        if (!this.mCustomCurveValid && this.mIsCustomCurveValidReady && !this.mIndividualBrightnessEngine.isModelValid() && !manually) {
            setCustomCurveValid(true, CUSTOM_CURVE_STATE_REASON_USER);
            resetCustomCurveValidConditions();
            this.mDataStore.startWrite();
        } else if (manually) {
            if (this.mCustomCurveValid || this.mIndividualBrightnessEngine.isModelValid()) {
                setCustomCurveValid(false, CUSTOM_CURVE_STATE_REASON_USER);
                resetCustomCurveValidConditions();
                this.mIndividualBrightnessEngine.setModelValid(false, CUSTOM_CURVE_STATE_REASON_USER);
                this.mDataStore.startWrite();
            }
        }
    }

    @Override // com.android.server.display.statistics.BrightnessDataProcessor.ModelEventCallback
    public void onBrightnessModelEvent(IndividualModelEvent modelEvent) {
        if (this.mSupportIndividualBrightness && this.mAutoBrightnessModeEnabled && this.mIndividualComponentName != null) {
            this.mIndividualBrightnessEngine.uploadBrightnessModelEvent(modelEvent, this.mIndividualModelExperimentEnable);
        }
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void onPredictFinished(float lux, int appId, float brightness) {
        if (Float.isNaN(brightness) || brightness < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            Slog.e(TAG, "Predict error: lux: " + lux + ", appId: " + appId + ", brt: " + brightness);
            return;
        }
        this.mHandler.removeMessages(1);
        float dbv = this.mDisplayDeviceConfig.getBrightnessFromNit(brightness);
        Slog.d(TAG, "onPredictFinished: lux: " + lux + ", appId: " + appId + ", brt: " + brightness + ", dbv: " + dbv);
        this.mCbmStateTracker.noteIndividualResult(lux, appId, dbv);
        this.mCbmStateTracker.noteStopPredictTracking(SystemClock.elapsedRealtime());
        updateAutoBrightness(dbv);
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void validateModelMonotonicity() {
        this.mCachedCategoryId = 0;
        this.mCachedLuxIndex = 0;
        this.mBrightnessValidationMapper.clear();
        uploadValidatedEventToModel();
        this.mLastModelTrainTimeStamp = System.currentTimeMillis();
        this.mModelTrainTotalTimes++;
        this.mBrightnessDataProcessor.aggregateIndividualModelTrainTimes();
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void onValidatedBrightness(float brightness) {
        float[] fArr;
        int i;
        this.mBgHandler.removeCallbacks(this.mNotifyModelVerificationRunnable);
        if (!this.mIndividualBrightnessEngine.isVerificationInProgress() || (fArr = this.mDefaultLuxLevels) == null || fArr.length == 0) {
            return;
        }
        float[] nitsArray = new float[fArr.length];
        if (this.mBrightnessValidationMapper.containsKey(Integer.valueOf(this.mCachedCategoryId))) {
            float[] nitsArray2 = this.mBrightnessValidationMapper.get(Integer.valueOf(this.mCachedCategoryId));
            nitsArray = nitsArray2;
        }
        nitsArray[this.mCachedLuxIndex] = brightness;
        this.mBrightnessValidationMapper.put(Integer.valueOf(this.mCachedCategoryId), nitsArray);
        int i2 = this.mCachedLuxIndex + 1;
        this.mCachedLuxIndex = i2;
        float[] fArr2 = this.mDefaultLuxLevels;
        if (i2 < fArr2.length) {
            uploadValidatedEventToModel();
            return;
        }
        if (i2 == fArr2.length && (i = this.mCachedCategoryId) < 2) {
            this.mCachedLuxIndex = 0;
            this.mCachedCategoryId = i + 1;
            uploadValidatedEventToModel();
        } else if (i2 == fArr2.length && this.mCachedCategoryId == 2) {
            if (isMonotonicModel()) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        CustomBrightnessModeController.this.lambda$onValidatedBrightness$4();
                    }
                });
                this.mBrightnessDataProcessor.aggregateIndividualModelTrainTimes(true);
            } else {
                this.mBrightnessDataProcessor.aggregateIndividualModelTrainTimes(false);
                Slog.e(TAG, "Model cannot complete verification due to non-monotonicity.");
            }
            this.mIndividualBrightnessEngine.completeModelValidation();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onValidatedBrightness$4() {
        this.mIndividualBrightnessEngine.setModelValid(true, "train_finished");
        resetCustomCurveValidConditions();
        setCustomCurveValid(false, CUSTOM_CURVE_STATE_REASON_USER);
        disableIndividualEngine(false);
        this.mDataStore.startWrite();
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void onExperimentUpdated(int expId, boolean enable) {
        this.mIndividualModelExperimentEnable = enable;
        this.mBrightnessDataProcessor.updateExpId(expId);
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void onTrainIndicatorsFinished(IndividualTrainEvent event) {
        this.mBrightnessDataProcessor.aggregateModelTrainIndicators(event);
    }

    @Override // com.android.server.display.aiautobrt.IndividualBrightnessEngine.EngineCallback
    public void onAbTestExperimentUpdated(int expId, int flag) {
        if (flag == -1) {
            this.mCustomCurveExperimentEnable = true;
            this.mIndividualModelExperimentEnable = true;
        } else {
            this.mCustomCurveExperimentEnable = flag == 1;
            this.mIndividualModelExperimentEnable = flag == 2;
        }
        this.mBrightnessDataProcessor.updateExpId(expId);
    }

    private void uploadValidatedEventToModel() {
        int i;
        float[] fArr = this.mDefaultLuxLevels;
        if (fArr != null && (i = this.mCachedLuxIndex) < fArr.length) {
            float lux = fArr[i];
            IndividualModelEvent event = this.mBrightnessDataProcessor.createModelEvent(lux, this.mCachedCategoryId, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 0, -1);
            if (event.isValidRawEvent() && this.mSupportIndividualBrightness) {
                this.mIndividualBrightnessEngine.preparePredictBrightness(event);
            }
            this.mBgHandler.postDelayed(this.mNotifyModelVerificationRunnable, 200L);
        }
    }

    private boolean isMonotonicModel() {
        for (Map.Entry<Integer, float[]> entry : this.mBrightnessValidationMapper.entrySet()) {
            final int key = entry.getKey().intValue();
            float[] array = entry.getValue();
            final float[] newArray = smoothCurve(array);
            if (newArray == null) {
                return false;
            }
            if (!isMonotonic(newArray)) {
                Slog.e(TAG, "Model is not monotonic, brightness spline: " + Arrays.toString(newArray));
                return false;
            }
            if (isBelowMinimumSpline(newArray)) {
                Slog.e(TAG, "Model brightness spline is below the minimum brightness spline, brightness spline: " + Arrays.toString(newArray));
                return false;
            }
            if (isBelowMinimumSlope(newArray)) {
                Slog.e(TAG, "Model brightness spline is below the minimum scope, brightness spline: " + Arrays.toString(newArray));
                return true;
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    CustomBrightnessModeController.this.lambda$isMonotonicModel$5(key, newArray);
                }
            });
            this.mDataStore.storeIndividualSpline(key, newArray);
        }
        return true;
    }

    private float[] smoothCurve(float[] nitsArray) {
        DisplayDeviceConfig displayDeviceConfig;
        float[] fArr = this.mDefaultLuxLevels;
        if (fArr.length != this.mDefaultNitsLevels.length || (displayDeviceConfig = this.mDisplayDeviceConfig) == null) {
            Slog.e(TAG, "Can not smooth individual curve!");
            return null;
        }
        float[] newNitsArray = new float[nitsArray.length];
        float preLux = fArr[0];
        float preBrightness = MathUtils.constrain(displayDeviceConfig.getBrightnessFromNit(nitsArray[0]), this.mMinBrightness, this.mNormalMaxBrightness);
        newNitsArray[0] = this.mDisplayDeviceConfig.getNitsFromBacklight(preBrightness);
        int i = 1;
        while (true) {
            float[] fArr2 = this.mDefaultLuxLevels;
            if (i < fArr2.length) {
                if (fArr2[i] <= this.mHbmMinimumLux - 1.0f) {
                    float currentLux = fArr2[i];
                    float currentBrightness = this.mDisplayDeviceConfig.getBrightnessFromNit(nitsArray[i]);
                    float maxBrightness = MathUtils.min(MathUtils.max(permissibleRatio(currentLux, preLux) * preBrightness, MIN_PERMISSIBLE_INCREASE + preBrightness), this.mNormalMaxBrightness);
                    float newBrightness = MathUtils.constrain(currentBrightness, preBrightness, maxBrightness);
                    preLux = currentLux;
                    preBrightness = newBrightness;
                    newNitsArray[i] = this.mDisplayDeviceConfig.getNitsFromBacklight(newBrightness);
                } else {
                    newNitsArray[i] = this.mDefaultNitsLevels[i];
                }
                i++;
            } else {
                return newNitsArray;
            }
        }
    }

    private float permissibleRatio(float currLux, float prevLux) {
        return MathUtils.pow((currLux + LUX_GRAD_SMOOTHING) / (LUX_GRAD_SMOOTHING + prevLux), 1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: buildIndividualSpline, reason: merged with bridge method [inline-methods] */
    public void lambda$isMonotonicModel$5(int category, float[] nitsArray) {
        Spline spline = Spline.createSpline(this.mDefaultLuxLevels, nitsArray);
        if (category == 0) {
            this.mIndividualDefaultSpline = spline;
        } else if (category == 1) {
            this.mIndividualGameSpline = spline;
        } else if (category == 2) {
            this.mIndividualVideoSpline = spline;
        }
    }

    private float getIndividualBrightness(String packageName, float lux) {
        Spline spline;
        int category = this.mAppClassifier.getAppCategoryId(packageName);
        if (category == 1) {
            spline = this.mIndividualGameSpline;
        } else if (category == 2) {
            spline = this.mIndividualVideoSpline;
        } else {
            spline = this.mIndividualDefaultSpline;
        }
        float individualBrt = this.mDisplayDeviceConfig.getBrightnessFromNit(spline.interpolate(lux));
        Slog.d(TAG, "getIndividualBrightness: category: " + category + ", lux: " + lux + ", individualBrt: " + individualBrt);
        return individualBrt;
    }

    private boolean isValidIndividualSpline() {
        return (this.mIndividualDefaultSpline == null || this.mIndividualGameSpline == null || this.mIndividualVideoSpline == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAutoBrightness(float brightness) {
        this.mPendingIndividualBrightness = brightness;
        this.mDisplayPowerControllerImpl.updateAutoBrightness();
    }

    private void setPendingToWaitPredict(float newAutoBrightness) {
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1, Integer.valueOf(Float.floatToIntBits(newAutoBrightness)));
        this.mHandler.sendMessageDelayed(msg, 200L);
    }

    private BrightnessConfiguration build(float[] luxLevels, float[] brightnessLevelsNits, String description) {
        BrightnessConfiguration.Builder builder = new BrightnessConfiguration.Builder(luxLevels, brightnessLevelsNits);
        builder.setShortTermModelTimeoutMillis(this.mShortTermModelTimeout);
        builder.setShortTermModelLowerLuxMultiplier(0.6f);
        builder.setShortTermModelUpperLuxMultiplier(0.6f);
        builder.setDescription(description);
        return builder.build();
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.CloudListener
    public void onCloudUpdated(long summary, Map<String, Object> data) {
        if ((8 & summary) != 0) {
            this.mAppClassifier.loadAppCategoryConfig();
        }
        this.mCustomCurveCloudDisable = (16 & summary) != 0;
        this.mIndividualModelCloudDisable = (32 & summary) != 0;
    }

    private boolean isVerticalScreen(int orientation) {
        return orientation == 0 || orientation == 2;
    }

    private boolean isHorizontalScreen(int orientation) {
        return orientation == 1 || orientation == 3;
    }

    private boolean isNightTime(int clock) {
        return (clock >= 0 && clock <= 4) || (clock > 20 && clock <= 23);
    }

    private boolean isEarlyMorningTime(int clock) {
        return clock > 4 && clock <= 8;
    }

    private boolean isNoonTime(int clock) {
        return clock > 10 && clock <= 12;
    }

    private boolean isDayTime(int clock) {
        return (clock > 8 && clock <= 10) || (clock > 12 && clock <= 20);
    }

    private boolean isValidMapping(float[] x, float[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length != y.length) {
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        float prevY = y[0];
        if (prevX < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || prevY < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || Float.isNaN(prevX) || Float.isNaN(prevY)) {
            return false;
        }
        for (int i = 1; i < N; i++) {
            if (prevX >= x[i] || prevY > y[i] || Float.isNaN(x[i]) || Float.isNaN(y[i])) {
                return false;
            }
            prevX = x[i];
            prevY = y[i];
        }
        return true;
    }

    private float[] getFloatArray(TypedArray array) {
        int N = array.length();
        float[] values = new float[N];
        for (int i = 0; i < N; i++) {
            values[i] = array.getFloat(i, -1.0f);
        }
        array.recycle();
        return values;
    }

    private float[] getLuxLevels(int[] lux) {
        float[] levels = new float[lux.length + 1];
        for (int i = 0; i < lux.length; i++) {
            levels[i + 1] = lux[i];
        }
        return levels;
    }

    public void updateCbmState(boolean autoBrightnessEnabled) {
        if (!autoBrightnessEnabled) {
            updateCbmState(-1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CbmHandler extends Handler {
        public CbmHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Slog.d(CustomBrightnessModeController.TAG, "Predict timeout.");
                    CustomBrightnessModeController.this.updateAutoBrightness(Float.intBitsToFloat(msg.arg1));
                    CustomBrightnessModeController.this.mBrightnessDataProcessor.aggregateModelPredictTimeoutTimes();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -1564692477:
                    if (lastPathSegment.equals(CustomBrightnessModeController.KEY_CUSTOM_BRIGHTNESS_MODE)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
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
                    CustomBrightnessModeController.this.updateCustomBrightnessEnabled();
                    return;
                case 1:
                    CustomBrightnessModeController.this.updateScreenBrightnessMode();
                    return;
                default:
                    return;
            }
        }
    }

    private boolean isMonotonic(float[] x) {
        if (x.length < 2) {
            return false;
        }
        float prev = x[0];
        for (int i = 1; i < x.length; i++) {
            float curr = x[i];
            if (curr < prev || prev <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                return false;
            }
            prev = curr;
        }
        return true;
    }

    private boolean isBelowMinimumSpline(float[] nits) {
        float[] fArr;
        if (this.mMinimumBrightnessSpline == null || (fArr = this.mDefaultLuxLevels) == null || nits.length != fArr.length) {
            return true;
        }
        int i = 0;
        while (true) {
            float[] fArr2 = this.mDefaultLuxLevels;
            if (i >= fArr2.length) {
                return false;
            }
            if (nits[i] < this.mMinimumBrightnessSpline.interpolate(fArr2[i])) {
                return true;
            }
            i++;
        }
    }

    private boolean isBelowMinimumSlope(float[] nits) {
        float[] fArr;
        float[] fArr2 = this.mDefaultLuxLevels;
        if (fArr2 == null || nits.length != fArr2.length) {
            return true;
        }
        int index = 0;
        int i = 0;
        while (true) {
            fArr = this.mDefaultLuxLevels;
            if (i >= fArr.length) {
                break;
            }
            index = i;
            if (fArr[i] > this.mHbmMinimumLux - 1.0f) {
                break;
            }
            i++;
        }
        if (index > 1) {
            float f = fArr[index - 1];
            float f2 = fArr[0];
            if (f != f2) {
                float minScope = (nits[index - 1] - nits[0]) / (fArr[index - 1] - f2);
                float permittedMinScope = 100.0f / (this.mHbmMinimumLux - 1.0f);
                Slog.d(TAG, "isBelowMinimumSlope: minScope: " + minScope + ", permittedMinScope: " + permittedMinScope);
                return minScope <= permittedMinScope;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateModelValid(int type, float ratio) {
        boolean customCurveValid = type == 1;
        boolean IndividualModelValid = type == 2;
        boolean defaultValid = type == 0;
        setCustomCurveValid(customCurveValid, CUSTOM_CURVE_STATE_REASON_BEST_INDICATOR);
        this.mIndividualBrightnessEngine.setModelValid(IndividualModelValid, CUSTOM_CURVE_STATE_REASON_BEST_INDICATOR);
        if (customCurveValid || IndividualModelValid) {
            resetCustomCurveValidConditions();
        }
        if (IndividualModelValid) {
            disableIndividualEngine(false);
        }
        this.mDataStore.startWrite();
        Slog.d(TAG, "updateModelValid: custom valid: " + customCurveValid + ", individual model valid: " + IndividualModelValid + ", default valid: " + defaultValid + ", best ratio: " + ratio);
    }

    protected void setCustomCurveValid(boolean enable, String reason) {
        if (enable && !this.mCustomCurveValid) {
            this.mCustomCurveValid = true;
            this.mCustomCurveValidStateReason = reason;
            this.mDataStore.storeCustomCurveEnabled(true);
            Slog.d(TAG, "setCustomCurveValid: custom curve is valid due to " + reason);
            return;
        }
        if (!enable && this.mCustomCurveValid) {
            this.mCustomCurveValid = false;
            this.mCurrentCustomSceneState = -1;
            this.mCustomCurveValidStateReason = reason;
            this.mDisplayPowerControllerImpl.setBrightnessConfiguration(null);
            this.mDataStore.storeCustomCurveEnabled(false);
            Slog.d(TAG, "setCustomCurveValid: custom curve is invalid due to " + reason);
        }
    }

    public void startCbmStatsJob() {
        Handler handler = this.mBgHandler;
        final CbmStateTracker cbmStateTracker = this.mCbmStateTracker;
        Objects.requireNonNull(cbmStateTracker);
        handler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.startCbmStats();
            }
        });
    }

    public void setCustomCurveEnabledOnCommand(boolean z) {
        Settings.Secure.putInt(this.mContentResolver, KEY_CUSTOM_BRIGHTNESS_MODE, z ? 1 : 0);
    }

    public void setIndividualModelEnabledOnCommand(boolean enable) {
        Settings.Secure.putInt(this.mContentResolver, KEY_CUSTOM_BRIGHTNESS_MODE, enable ? 2 : 0);
    }

    public void setForceTrainEnabledOnCommand(boolean z) {
        Settings.Secure.putInt(this.mContentResolver, KEY_FORCE_TRAIN_ENABLE, z ? 1 : 0);
    }

    private boolean isDarkRoomAdjustment(float lux, boolean isBrightening) {
        return lux <= 100.0f && isBrightening;
    }

    private boolean isBrightRoomAdjustment(float lux, String packageName, int orientation, boolean isBrightening) {
        int clock = LocalDateTime.now().getHour();
        int category = this.mAppClassifier.getAppCategoryId(packageName);
        return isBrightening && lux >= BRIGHT_ROOM_AMBIENT_LUX && isBrighteningScene(category, orientation, clock);
    }

    public int getCurrentSceneState() {
        return this.mCurrentCustomSceneState;
    }

    private void resetCustomCurveValidConditions() {
        this.mIsCustomCurveValidReady = false;
        this.mCbmStateTracker.resetBrtAdjSceneCount();
    }

    private void noteBrightnessAdjustTimesToAggregate(int cbmState, boolean isManuallySet) {
        if (this.mCbmStateTracker.isBrightnessAdjustNoted(cbmState)) {
            this.mBrightnessDataProcessor.aggregateCbmBrightnessAdjustTimes(cbmState, isManuallySet);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteBrightnessUsageToAggregate(float duration, int cbmState) {
        this.mBrightnessDataProcessor.aggregateCbmBrightnessUsageDuration(duration, cbmState);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notePredictDurationToAggregate(long duration) {
        this.mBrightnessDataProcessor.aggregateModelAvgPredictDuration((float) duration);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void buildConfigurationFromXml(int category, float[] nits) {
        if (isMonotonic(nits)) {
            lambda$isMonotonicModel$5(category, nits);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setCustomCurveEnabledFromXml(boolean enabled) {
        this.mCustomCurveValid = enabled;
        this.mCustomCurveValidStateReason = CUSTOM_CURVE_STATE_REASON_BACKUP;
        Slog.d(TAG, "setCustomCurveEnabledFromXml: custom curve is" + (this.mCustomCurveValid ? " valid" : " invalid") + " due to: " + this.mCustomCurveValidStateReason);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setIndividualModelEnabledFromXml(boolean enabled) {
        this.mIndividualBrightnessEngine.setModeValidFromXml(enabled);
    }

    public void dump(final PrintWriter pw) {
        boolean z = DisplayDebugConfig.DEBUG_CBM;
        sDebug = z;
        this.mBrightnessDataProcessor.forceReportTrainDataEnabled(z);
        if (this.mSupportCustomBrightness) {
            pw.println("  mCustomCurveDisabledByUserChange=" + this.mCustomCurveDisabledByUserChange);
            pw.println("  mForcedCustomCurveEnabled=" + this.mForcedCustomCurveEnabled);
            pw.println("  mCurrentSceneState=" + this.mCurrentCustomSceneState);
            pw.println("  mDefaultConfiguration=" + this.mDefaultConfiguration);
            pw.println("  mBrighteningConfiguration=" + this.mBrighteningConfiguration);
            pw.println("  mDarkeningConfiguration=" + this.mDarkeningConfiguration);
            pw.println("  mCustomCurveValidStateReason=" + this.mCustomCurveValidStateReason);
            pw.println("  mCustomCurveValid=" + this.mCustomCurveValid);
            pw.println("  mIsCustomCurveValidReady=" + this.mIsCustomCurveValidReady);
            pw.println("  mCustomCurveExperimentEnable=" + this.mCustomCurveExperimentEnable);
        }
        if (this.mSupportIndividualBrightness) {
            pw.println("  mForcedIndividualBrightnessEnabled=" + this.mForcedIndividualBrightnessEnabled);
            pw.println("  mIndividualDisabledByUserChange=" + this.mIndividualDisabledByUserChange);
            pw.println("  mPendingIndividualBrightness=" + this.mPendingIndividualBrightness);
            pw.println("  mMinimumBrightnessSpline=" + this.mMinimumBrightnessSpline);
            pw.println("  mIndividualModelExperimentEnable=" + this.mIndividualModelExperimentEnable);
            pw.println("  mLastModelTrainTimeStamp=" + FORMAT.format(new Date(this.mLastModelTrainTimeStamp)));
            pw.println("  mModelTrainTotalTimes=" + this.mModelTrainTotalTimes);
            this.mIndividualBrightnessEngine.dump(pw);
            this.mCbmStateTracker.dump(pw);
            if (!this.mBrightnessValidationMapper.isEmpty()) {
                pw.println("  Brt validation set:");
                this.mBrightnessValidationMapper.forEach(new BiConsumer() { // from class: com.android.server.display.aiautobrt.CustomBrightnessModeController$$ExternalSyntheticLambda6
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        pw.println("{cateId=" + ((Integer) obj) + ", brtSpl=" + Arrays.toString((float[]) obj2) + "}");
                    }
                });
            }
            pw.println(" Individual Model Spline:");
            pw.println("  mIndividualDefaultSpline=" + this.mIndividualDefaultSpline);
            pw.println("  mIndividualGameSpline=" + this.mIndividualGameSpline);
            pw.println("  mIndividualVideoSpline=" + this.mIndividualVideoSpline);
            this.mDataStore.dump(pw);
        }
    }
}
