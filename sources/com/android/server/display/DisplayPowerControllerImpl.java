package com.android.server.display;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.display.BrightnessConfiguration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.view.IRotationWatcher;
import android.widget.Toast;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.server.display.AutomaticBrightnessControllerImpl;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.DisplayPowerControllerStub;
import com.android.server.display.HighBrightnessModeController;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.display.RampAnimator;
import com.android.server.display.SunlightController;
import com.android.server.display.ThermalBrightnessController;
import com.android.server.display.aiautobrt.CustomBrightnessModeController;
import com.android.server.display.brightness.BrightnessReason;
import com.android.server.display.statistics.BrightnessDataProcessor;
import com.android.server.display.statistics.OneTrackFoldStateHelper;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class DisplayPowerControllerImpl implements DisplayPowerControllerStub, SunlightController.Callback, MiuiDisplayCloudController.Callback {
    private static boolean BCBC_ENABLE = SystemProperties.getBoolean("ro.vendor.bcbc.enable", false);
    private static final int BCBC_STATE_DISABLE = 0;
    private static final int BCBC_STATE_ENABLE = 1;
    private static final float COEFFICIENT = 0.5f;
    private static final int CURRENT_GRAYSCALE_UPDATE_DISABLED = 0;
    private static final int CURRENT_GRAYSCALE_UPDATE_ENABLED = 1;
    private static final float[] DATA_D;
    private static final long DELAY_TIME = 4000;
    private static final int DISPLAY_DIM_STATE = 262;
    private static final float DOLBY_PREVIEW_DEFAULT_BOOST_RATIO = 1.0f;
    private static final float DOLBY_PREVIEW_MAX_BOOST_RATIO = 10.0f;
    private static final float DOZE_HBM_NIT_DEFAULT = 60.0f;
    private static final float DOZE_LBM_NIT_DEFAULT = 5.0f;
    private static final int DOZE_LIGHT_LOW = 1;
    public static final int EPSILON = 3;
    private static final float HBM_MINIMUM_LUX = 6000.0f;
    private static final boolean IS_FOLDABLE_DEVICE;
    private static final String KEY_CURTAIN_ANIM_ENABLED = "curtain_anim_enabled";
    private static final String KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW = "is_dynamic_lockscreen_shown";
    private static final String KEY_SUNLIGHT_MODE_AVAILABLE = "config_sunlight_mode_available";
    private static final float MAX_A;
    private static final float MAX_DIFF;
    public static final float MAX_GALLERY_HDR_FACTOR = 2.25f;
    private static final float MAX_HBM_BRIGHTNESS_FOR_PEAK;
    private static final float MAX_POWER_SAVE_MODE_NIT;
    public static final float MIN_GALLERY_HDR_FACTOR = 1.0f;
    private static final int MSG_UPDATE_CURRENT_GRAY_SCALE = 6;
    private static final int MSG_UPDATE_DOLBY_STATE = 2;
    private static final int MSG_UPDATE_FOREGROUND_APP = 4;
    private static final int MSG_UPDATE_FOREGROUND_APP_SYNC = 3;
    private static final int MSG_UPDATE_GRAY_SCALE = 1;
    private static final int MSG_UPDATE_ROTATION = 7;
    private static final int MSG_UPDATE_THERMAL_MAX_BRIGHTNESS = 5;
    private static final String PACKAGE_DIM_SYSTEM = "system";
    private static final int PEAK_BRIGHTNESS_AMBIENT_LUX_THRESHOLD;
    private static final int PEAK_BRIGHTNESS_GRAY_SCALE_THRESHOLD;
    private static final boolean SUPPORT_BCBC_BY_AMBIENT_LUX;
    private static final boolean SUPPORT_IDLE_DIM;
    private static final boolean SUPPORT_MULTIPLE_AOD_BRIGHTNESS;
    private static final Resources SYSTEM_RESOURCES;
    private static final String TAG = "DisplayPowerControllerImpl";
    private static final int TRANSACTION_NOTIFY_BRIGHTNESS = 31104;
    private static final int TRANSACTION_NOTIFY_DIM = 31107;
    private static final float V1;
    private static final float V2;
    private static final float[] mBCBCLuxThreshold;
    private static final float[] mBCBCNitDecreaseThreshold;
    private static final boolean mSupportGalleryHdr;
    private final boolean SUPPORT_DOLBY_VERSION_BRIGHTEN;
    private final boolean SUPPORT_HDR_HBM_BRIGHTEN;
    private final boolean SUPPORT_MANUAL_BRIGHTNESS_BOOST;
    private final boolean SUPPORT_MANUAL_DIMMING;
    private IActivityTaskManager mActivityTaskManager;
    private float mActualScreenOnBrightness;
    private boolean mAppliedBcbc;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private float mAppliedMaxOprBrightness;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedSunlightMode;
    private boolean mAutoBrightnessEnable;
    private AutomaticBrightnessControllerImpl mAutomaticBrightnessControllerImpl;
    private int mBCBCState;
    private float mBasedBrightness;
    private float mBasedSdrBrightness;
    private BatteryReceiver mBatteryReceiver;
    private float mBcbcBrightness;
    private boolean mBootCompleted;
    private BrightnessABTester mBrightnessABTester;
    private BrightnessDataProcessor mBrightnessDataProcessor;
    private BrightnessMappingStrategy mBrightnessMapper;
    private RampAnimator.DualRampAnimator<DisplayPowerState> mBrightnessRampAnimator;
    private CustomBrightnessModeController mCbmController;
    private AutomaticBrightnessControllerImpl.CloudControllerListener mCloudListener;
    private boolean mColorInversionEnabled;
    private Context mContext;
    private float mCurrentBrightness;
    private int mCurrentConditionId;
    private int mCurrentDisplayPolicy;
    private float mCurrentGalleryHdrBoostFactor;
    private float mCurrentGrayScale;
    private float mCurrentSdrBrightness;
    private float mCurrentTemperature;
    private boolean mCurtainAnimationAvailable;
    private boolean mCurtainAnimationEnabled;
    private boolean mDebug;
    private float mDesiredBrightness;
    private int mDesiredBrightnessInt;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private DisplayFeatureManagerServiceImpl mDisplayFeatureManagerServicImpl;
    private int mDisplayId;
    private DisplayManagerServiceImpl mDisplayMangerServiceImpl;
    private DisplayPowerController mDisplayPowerController;
    private boolean mDolbyPreviewBoostAvailable;
    private float mDolbyPreviewBoostRatio;
    private boolean mDolbyPreviewEnable;
    private boolean mDolbyStateEnable;
    private boolean mDozeInLowBrightness;
    private float mDozeScreenBrightness = -1.0f;
    private Boolean mFolded;
    private String mForegroundAppPackageName;
    private final IForegroundWindowListener mForegroundWindowListener;
    private boolean mGalleryHdrThrottled;
    private float mGrayBrightnessFactor;
    private float mGrayScale;
    private DisplayPowerControllerImplHandler mHandler;
    private HighBrightnessModeController mHbmController;
    private HighBrightnessModeController.HdrStateListener mHdrStateListener;
    private IBinder mISurfaceFlinger;
    private boolean mInitialBCBCParameters;
    private Injector mInjector;
    private boolean mIsDynamicLockScreenShowing;
    private boolean mIsGalleryHdrEnable;
    private boolean mIsScreenOn;
    private boolean mIsSunlightModeEnable;
    private boolean mIsSupportManualBoostApp;
    private float mK1;
    private float mK2;
    private float mK3;
    private float mK4;
    private boolean mLastAnimating;
    private int mLastDisplayState;
    private boolean mLastFoldedState;
    private float mLastManualBoostBrightness;
    private float mLastMaxBrightness;
    private float mLastSettingsBrightnessBeforeApplySunlight;
    private boolean mLastSlowChange;
    private float mLastTemperature;
    private float mLastTemporaryBrightness;
    private LogicalDisplay mLogicalDisplay;
    private int[] mLowBatteryLevelBrightnessThreshold;
    private int[] mLowBatteryLevelThreshold;
    private float mLowBatteryLimitBrightness;
    private boolean mManualBrightnessBoostEnable;
    private float mMaxDozeBrightnessFloat;
    private float mMaxManualBoostBrightness;
    private float mMaxPowerSaveModeBrightness;
    private float mMinDozeBrightnessFloat;
    protected MiuiDisplayCloudController mMiuiDisplayCloudController;
    private int mOprAmbientLuxThreshold;
    private boolean mOprBrightnessControlAvailable;
    private int[] mOprGrayscaleThreshold;
    private int mOprMaxNitThreshold;
    private int[] mOprNitThreshold;
    private int mOrientation;
    private boolean mOutDoorHighTempState;
    private String mPendingForegroundAppPackageName;
    private boolean mPendingResetGrayscaleStateForOpr;
    private boolean mPendingShowCurtainAnimation;
    private boolean mPendingUpdateBrightness;
    private WindowManagerPolicy mPolicy;
    private PowerManager mPowerManager;
    private int mPreviousDisplayPolicy;
    private RampRateController mRampRateController;
    private float[] mRealtimeArrayD;
    private float mRealtimeMaxA;
    private float mRealtimeMaxDiff;
    private boolean mRotationListenerEnabled;
    private RotationWatcher mRotationWatcher;
    private float mScreenBrightnessRangeMaximum;
    private float mScreenBrightnessRangeMinimum;
    private boolean mScreenGrayscaleState;
    private SettingsObserver mSettingsObserver;
    private boolean mShouldDimming;
    private boolean mShowOutdoorHighTempToast;
    private SunlightController mSunlightController;
    private boolean mSunlightModeActive;
    private boolean mSunlightModeAvailable;
    private DisplayPowerControllerStub.SunlightStateChangedListener mSunlightStateListener;
    private boolean mSupportCustomBrightness;
    private boolean mSupportIndividualBrightness;
    private float mTargetBrightness;
    private float mTargetSdrBrightness;
    private TaskStackListenerImpl mTaskStackListener;
    private ThermalBrightnessController.Callback mThermalBrightnessCallback;
    private boolean mThermalBrightnessControlAvailable;
    private ThermalBrightnessController mThermalBrightnessController;
    private float mThermalMaxBrightness;
    private ThermalObserver mThermalObserver;
    private boolean mUpdateBrightnessAnimInfoEnable;
    private WindowManagerService mWms;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayPowerControllerImpl> {

        /* compiled from: DisplayPowerControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayPowerControllerImpl INSTANCE = new DisplayPowerControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayPowerControllerImpl m1108provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayPowerControllerImpl m1107provideNewInstance() {
            return new DisplayPowerControllerImpl();
        }
    }

    public DisplayPowerControllerImpl() {
        float[] fArr = DATA_D;
        this.mRealtimeArrayD = Arrays.copyOf(fArr, fArr.length);
        this.mRealtimeMaxA = MAX_A;
        this.mRealtimeMaxDiff = MAX_DIFF;
        this.mGrayBrightnessFactor = 1.0f;
        this.mGrayScale = Float.NaN;
        this.mBcbcBrightness = Float.NaN;
        this.SUPPORT_DOLBY_VERSION_BRIGHTEN = FeatureParser.getBoolean("support_dolby_version_brighten", false);
        this.SUPPORT_HDR_HBM_BRIGHTEN = FeatureParser.getBoolean("support_hdr_hbm_brighten", false);
        this.SUPPORT_MANUAL_DIMMING = FeatureParser.getBoolean("support_manual_dimming", false);
        this.SUPPORT_MANUAL_BRIGHTNESS_BOOST = FeatureParser.getBoolean("support_manual_brightness_boost", false);
        this.mLastDisplayState = 2;
        this.mLastMaxBrightness = -1.0f;
        this.mCurrentGrayScale = Float.NaN;
        this.mCurrentGalleryHdrBoostFactor = 1.0f;
        this.mThermalMaxBrightness = Float.NaN;
        this.mLastTemporaryBrightness = Float.NaN;
        this.mBasedSdrBrightness = Float.NaN;
        this.mBasedBrightness = Float.NaN;
        this.mLowBatteryLimitBrightness = 1.0f;
        this.mAppliedMaxOprBrightness = Float.NaN;
        this.mMaxPowerSaveModeBrightness = Float.NaN;
        this.mForegroundWindowListener = new IForegroundWindowListener.Stub() { // from class: com.android.server.display.DisplayPowerControllerImpl.1
            public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
                DisplayPowerControllerImpl.this.mHandler.removeMessages(4);
                DisplayPowerControllerImpl.this.mHandler.sendEmptyMessage(4);
                DisplayPowerControllerImpl.this.notifyUpdateForegroundApp();
            }
        };
        this.mDolbyPreviewBoostRatio = Float.NaN;
        this.mThermalBrightnessCallback = new ThermalBrightnessController.Callback() { // from class: com.android.server.display.DisplayPowerControllerImpl.3
            @Override // com.android.server.display.ThermalBrightnessController.Callback
            public void onThermalBrightnessChanged(float thermalBrightness) {
                DisplayPowerControllerImpl.this.mHandler.removeMessages(5);
                Message msg = DisplayPowerControllerImpl.this.mHandler.obtainMessage(5, Integer.valueOf(Float.floatToIntBits(thermalBrightness)));
                msg.sendToTarget();
            }
        };
        this.mCloudListener = new AutomaticBrightnessControllerImpl.CloudControllerListener() { // from class: com.android.server.display.DisplayPowerControllerImpl.5
            @Override // com.android.server.display.AutomaticBrightnessControllerImpl.CloudControllerListener
            public boolean isAutoBrightnessStatisticsEventEnable() {
                if (DisplayPowerControllerImpl.this.mMiuiDisplayCloudController != null) {
                    return DisplayPowerControllerImpl.this.mMiuiDisplayCloudController.isAutoBrightnessStatisticsEventEnable();
                }
                return false;
            }
        };
    }

    static {
        IS_FOLDABLE_DEVICE = SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2;
        SUPPORT_MULTIPLE_AOD_BRIGHTNESS = SystemProperties.getBoolean("ro.vendor.aod.brightness.cust", false);
        mBCBCLuxThreshold = new float[]{DOLBY_PREVIEW_MAX_BOOST_RATIO, 100.0f};
        mBCBCNitDecreaseThreshold = new float[]{DOZE_LBM_NIT_DEFAULT, 12.0f};
        Resources system = Resources.getSystem();
        SYSTEM_RESOURCES = system;
        DATA_D = getFloatArray(system.obtainTypedArray(285409330));
        V1 = system.getFloat(285671461);
        V2 = system.getFloat(285671460);
        MAX_DIFF = system.getFloat(285671456);
        MAX_A = system.getFloat(285671458);
        MAX_POWER_SAVE_MODE_NIT = system.getFloat(285671472);
        PEAK_BRIGHTNESS_GRAY_SCALE_THRESHOLD = system.getInteger(285933594);
        PEAK_BRIGHTNESS_AMBIENT_LUX_THRESHOLD = system.getInteger(285933577);
        MAX_HBM_BRIGHTNESS_FOR_PEAK = system.getFloat(285671470);
        SUPPORT_BCBC_BY_AMBIENT_LUX = FeatureParser.getBoolean("support_bcbc_by_ambient_lux", false);
        SUPPORT_IDLE_DIM = FeatureParser.getBoolean("support_idle_dim", false);
        mSupportGalleryHdr = FeatureParser.getBoolean("support_gallery_hdr", false);
    }

    /* JADX WARN: Code restructure failed: missing block: B:12:0x004b, code lost:
    
        if (miui.util.FeatureParser.getBoolean(com.android.server.display.DisplayPowerControllerImpl.KEY_SUNLIGHT_MODE_AVAILABLE, true) != false) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void init(com.android.server.display.DisplayPowerController r20, android.content.Context r21, android.os.Looper r22, int r23, float r24, float r25, com.android.server.display.DisplayDeviceConfig r26, com.android.server.display.LogicalDisplay r27, com.android.server.display.HighBrightnessModeController r28) {
        /*
            Method dump skipped, instructions count: 630
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DisplayPowerControllerImpl.init(com.android.server.display.DisplayPowerController, android.content.Context, android.os.Looper, int, float, float, com.android.server.display.DisplayDeviceConfig, com.android.server.display.LogicalDisplay, com.android.server.display.HighBrightnessModeController):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$1(Boolean folded) {
        setDeviceFolded(folded.booleanValue());
    }

    private void setDeviceFolded(boolean folded) {
        Boolean bool = this.mFolded;
        if (bool != null && bool.booleanValue() == folded) {
            return;
        }
        this.mFolded = Boolean.valueOf(folded);
        OneTrackFoldStateHelper.getInstance().oneTrackFoldState(folded);
        if (this.mDebug) {
            Slog.d(TAG, "mFolded: " + this.mFolded);
        }
        boolean isInteractive = this.mPowerManager.isInteractive();
        if (this.mCurtainAnimationAvailable && this.mCurtainAnimationEnabled && !this.mIsDynamicLockScreenShowing && !isInteractive && !isFolded() && !this.mPendingShowCurtainAnimation) {
            this.mPendingShowCurtainAnimation = true;
        }
    }

    public boolean isCurtainAnimationNeeded() {
        return this.mPendingShowCurtainAnimation;
    }

    public void onCurtainAnimationFinished() {
        if (this.mPendingShowCurtainAnimation) {
            this.mPendingShowCurtainAnimation = false;
        }
    }

    public boolean isFolded() {
        Boolean bool = this.mFolded;
        if (bool != null) {
            return bool.booleanValue();
        }
        return false;
    }

    private void registerSettingsObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_display_inversion_enabled"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_CURTAIN_ANIM_ENABLED), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW), false, this.mSettingsObserver, -1);
        loadSettings();
    }

    public void mayBeReportUserDisableSunlightTemporary(float tempBrightness) {
        if (this.mSunlightModeAvailable && this.mAppliedSunlightMode && !Float.isNaN(tempBrightness)) {
            this.mSunlightController.setSunlightModeDisabledByUserTemporary();
        }
    }

    private void updateAmbientLightSensor(Sensor lightSensor) {
        SunlightController sunlightController = this.mSunlightController;
        if (sunlightController != null) {
            sunlightController.updateAmbientLightSensor(lightSensor);
        }
    }

    public void setSunlightListener(DisplayPowerControllerStub.SunlightStateChangedListener listener) {
        this.mSunlightStateListener = listener;
    }

    private boolean isAllowedUseSunlightMode() {
        return this.mSunlightModeActive && !isSunlightModeDisabledByUser();
    }

    public float canApplyingSunlightBrightness(float currentScreenBrightness, float brightness, BrightnessReason reason) {
        DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener;
        int reasonModifier = reason.getModifier();
        if ((reasonModifier & 1) != 0 || this.mDisplayId != 0) {
            return brightness;
        }
        float tempBrightness = brightness;
        if (this.SUPPORT_MANUAL_BRIGHTNESS_BOOST) {
            canApplyManualBrightnessBoost(currentScreenBrightness);
        }
        if (this.mSunlightModeAvailable && isAllowedUseSunlightMode()) {
            if (this.mDisplayDeviceConfig.getHighBrightnessModeData() != null) {
                if (this.SUPPORT_MANUAL_BRIGHTNESS_BOOST && shouldManualBoostForCurrentApp() && this.mMaxManualBoostBrightness != -1.0f) {
                    tempBrightness = this.mMaxManualBoostBrightness;
                } else {
                    tempBrightness = this.mDisplayDeviceConfig.getHighBrightnessModeData().transitionPoint;
                }
            } else {
                tempBrightness = 1.0f;
            }
            if (!this.mAppliedSunlightMode) {
                this.mLastSettingsBrightnessBeforeApplySunlight = currentScreenBrightness;
            }
            this.mAppliedSunlightMode = true;
            DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener2 = this.mSunlightStateListener;
            if (sunlightStateChangedListener2 != null) {
                sunlightStateChangedListener2.updateScreenBrightnessSettingDueToSunlight(tempBrightness);
            }
            reason.setReason(11);
            if (this.mDebug) {
                Slog.d(TAG, "updatePowerState: appling sunlight mode brightness.last brightness:" + this.mLastSettingsBrightnessBeforeApplySunlight);
            }
        } else if (this.mAppliedSunlightMode) {
            this.mAppliedSunlightMode = false;
            if (!isSunlightModeDisabledByUser() && (sunlightStateChangedListener = this.mSunlightStateListener) != null) {
                sunlightStateChangedListener.updateScreenBrightnessSettingDueToSunlight(this.mLastSettingsBrightnessBeforeApplySunlight);
            }
            if (this.mDebug) {
                Slog.d(TAG, "updatePowerState: exit sunlight mode brightness. reset brightness: " + this.mLastSettingsBrightnessBeforeApplySunlight);
            }
        }
        return tempBrightness;
    }

    private boolean isSunlightModeDisabledByUser() {
        return this.mSunlightController.isSunlightModeDisabledByUser();
    }

    @Override // com.android.server.display.SunlightController.Callback
    public void notifySunlightStateChange(boolean active) {
        if (this.mDebug) {
            Slog.d(TAG, "notifySunlightStateChange: " + active);
        }
        this.mSunlightModeActive = active;
        DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener = this.mSunlightStateListener;
        if (sunlightStateChangedListener != null) {
            sunlightStateChangedListener.onSunlightStateChange();
        }
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            thermalBrightnessController.setSunlightState(active);
        }
    }

    @Override // com.android.server.display.SunlightController.Callback
    public float convertBrightnessToNit(float brightness) {
        DisplayDeviceConfig displayDeviceConfig = this.mDisplayDeviceConfig;
        if (displayDeviceConfig == null) {
            return Float.NaN;
        }
        return displayDeviceConfig.getNitsFromBacklight(brightness);
    }

    @Override // com.android.server.display.SunlightController.Callback
    public void notifySunlightModeChanged(boolean isSunlightModeEnable) {
        if (this.SUPPORT_MANUAL_BRIGHTNESS_BOOST && this.mIsSunlightModeEnable != isSunlightModeEnable) {
            if (!this.mBootCompleted && isSunlightModeEnable) {
                this.mManualBrightnessBoostEnable = true;
            }
            this.mIsSunlightModeEnable = isSunlightModeEnable;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public boolean isSupportManualBrightnessBoost() {
        return this.SUPPORT_MANUAL_BRIGHTNESS_BOOST;
    }

    public float getMaxManualBrightnessBoost() {
        if (!this.mAutoBrightnessEnable && shouldManualBoostForCurrentApp()) {
            float f = this.mMaxManualBoostBrightness;
            if (f != -1.0f) {
                return f;
            }
        }
        return -1.0f;
    }

    private boolean isManualBrightnessBoostAppEnable() {
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null) {
            return miuiDisplayCloudController.isManualBoostAppEnable();
        }
        return false;
    }

    private boolean shouldManualBoostForCurrentApp() {
        if (isManualBrightnessBoostAppEnable()) {
            return this.mIsSupportManualBoostApp && this.mIsSunlightModeEnable;
        }
        return this.mIsSunlightModeEnable;
    }

    private void updateManualBrightnessBoostState() {
        boolean isSupportManualBoostApp;
        if (this.mMiuiDisplayCloudController != null && isManualBrightnessBoostAppEnable() && (!this.mMiuiDisplayCloudController.getManualBoostDisableAppList().contains(this.mForegroundAppPackageName)) != this.mIsSupportManualBoostApp) {
            this.mIsSupportManualBoostApp = isSupportManualBoostApp;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    private void canApplyManualBrightnessBoost(float brightness) {
        if (this.mDisplayDeviceConfig.getHighBrightnessModeData() == null || !this.mBootCompleted || this.mMaxManualBoostBrightness == -1.0f) {
            Slog.w(TAG, "Don't apply manual brightness boost because current device status is invalid.");
            return;
        }
        float tempBrightness = brightness;
        if (!this.mManualBrightnessBoostEnable && shouldManualBoostForCurrentApp()) {
            this.mManualBrightnessBoostEnable = true;
            tempBrightness = calculateBrightnessForManualBoost(tempBrightness, true);
            this.mSunlightStateListener.updateScreenBrightnessSettingDueToSunlight(tempBrightness);
            Slog.d(TAG, "Enter manual brightness boost.");
        } else if (this.mManualBrightnessBoostEnable && !shouldManualBoostForCurrentApp()) {
            this.mManualBrightnessBoostEnable = false;
            tempBrightness = calculateBrightnessForManualBoost(tempBrightness, false);
            this.mSunlightStateListener.updateScreenBrightnessSettingDueToSunlight(tempBrightness);
            Slog.d(TAG, "Exit manual brightness boost.");
        }
        if (this.mIsSunlightModeEnable && this.mManualBrightnessBoostEnable) {
            this.mLastManualBoostBrightness = tempBrightness;
        } else {
            this.mLastManualBoostBrightness = Float.NaN;
        }
    }

    private float calculateBrightnessForManualBoost(float brightness, boolean isBoostEntering) {
        float transitionPoint = this.mDisplayDeviceConfig.getHighBrightnessModeData().transitionPoint;
        if (isBoostEntering) {
            float f = this.mScreenBrightnessRangeMinimum;
            float tempBrightness = (((brightness - f) / (transitionPoint - f)) * (this.mMaxManualBoostBrightness - transitionPoint)) + brightness;
            return tempBrightness;
        }
        float f2 = this.mLastManualBoostBrightness;
        float f3 = this.mScreenBrightnessRangeMinimum;
        float f4 = this.mMaxManualBoostBrightness;
        float tempBrightness2 = ((f2 * (transitionPoint - f3)) + ((f4 - transitionPoint) * f3)) / (f4 - f3);
        return tempBrightness2;
    }

    private void sendSurfaceFlingerActualBrightness(int brightness) {
        if (this.mDebug) {
            Slog.d(TAG, "sendSurfaceFlingerActualBrightness, brightness = " + brightness);
        }
        if (this.mISurfaceFlinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(brightness);
            try {
                try {
                    this.mISurfaceFlinger.transact(TRANSACTION_NOTIFY_BRIGHTNESS, data, null, 1);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to send brightness to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void sendBrightnessToSurfaceFlingerIfNeeded(float target, float dozeBrightness, boolean isDimming) {
        float f;
        switch (this.mDisplayPowerController.getDisplayPowerState().getScreenState()) {
            case 1:
            case 3:
                if (!BrightnessSynchronizer.floatEquals(dozeBrightness, this.mDozeScreenBrightness)) {
                    this.mDozeScreenBrightness = target;
                    this.mDozeInLowBrightness = BrightnessSynchronizer.brightnessFloatToInt(dozeBrightness) == 1;
                }
                float pendingBrightness = this.mDozeInLowBrightness ? this.mDozeScreenBrightness : this.mActualScreenOnBrightness;
                if (pendingBrightness != this.mDesiredBrightness) {
                    this.mDesiredBrightness = pendingBrightness;
                    int brightnessFloatToInt = BrightnessSynchronizer.brightnessFloatToInt(pendingBrightness);
                    this.mDesiredBrightnessInt = brightnessFloatToInt;
                    sendSurfaceFlingerActualBrightness(brightnessFloatToInt);
                    return;
                }
                return;
            case 2:
                if (target != this.mActualScreenOnBrightness && !isDimming) {
                    if (this.mSunlightModeActive) {
                        f = BrightnessSynchronizer.brightnessFloatToInt(this.mLastSettingsBrightnessBeforeApplySunlight);
                    } else {
                        f = target;
                    }
                    this.mActualScreenOnBrightness = f;
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void notifyBrightnessChangeIfNeeded(boolean screenOn, float brightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, float ambientLux, float userDataPoint, boolean defaultConfig, float actualBrightness) {
        if (this.mBrightnessDataProcessor != null && this.mAutomaticBrightnessControllerImpl != null) {
            HighBrightnessModeController highBrightnessModeController = this.mHbmController;
            boolean isHdrLayer = highBrightnessModeController != null && (highBrightnessModeController.getHighBrightnessMode() == 2 || this.mHbmController.isDolbyEnable());
            boolean isDimmingChanged = this.mAppliedDimming || (this.mPreviousDisplayPolicy == 2 && this.mCurrentDisplayPolicy == 3);
            this.mBrightnessDataProcessor.notifyBrightnessEventIfNeeded(screenOn, brightness, actualBrightness, userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, lowPowerMode, ambientLux, userDataPoint, defaultConfig, this.mSunlightModeActive, isHdrLayer, isDimmingChanged, this.mAutomaticBrightnessControllerImpl.getMainFastAmbientLux(), this.mAutomaticBrightnessControllerImpl.getAssistFastAmbientLux(), getSceneState());
        }
    }

    public float adjustSdrBrightness(float brightness, boolean useAutoBrightness, BrightnessReason reason, boolean appliedAutoBrightness) {
        float newBrightness = brightness;
        if (this.mDisplayPowerController.getDisplayPowerState().getScreenState() == 2) {
            if (useAutoBrightness) {
                if (this.mOprBrightnessControlAvailable && !isHdrScene()) {
                    newBrightness = adjustBrightnessByOpr(newBrightness, reason);
                }
                if (!isKeyguardOn()) {
                    newBrightness = adjustBrightnessByBcbc(newBrightness, reason);
                }
            } else {
                newBrightness = canApplyingSunlightBrightness(this.mDisplayPowerController.getScreenBrightnessSetting(), brightness, reason);
            }
            if ((reason.getModifier() & 2) != 0) {
                newBrightness = adjustBrightnessByPowerSaveMode(newBrightness, reason);
            }
            float newBrightness2 = adjustBrightnessToPeak(newBrightness, false, reason);
            if (this.mThermalBrightnessControlAvailable) {
                newBrightness2 = adjustBrightnessByThermal(newBrightness2, false, reason);
                if (appliedAutoBrightness && newBrightness2 != brightness) {
                    updateFastRateStatus(newBrightness2);
                }
            }
            newBrightness = adjustBrightnessByBattery(newBrightness2, reason);
        }
        if (!BrightnessSynchronizer.floatEquals(brightness, this.mBasedSdrBrightness)) {
            this.mBasedSdrBrightness = brightness;
            resetRateModifierOnAnimateValueChanged();
            onBrightnessChanged(true);
        } else {
            onBrightnessChanged(false);
        }
        resetBcbcRateModifier(appliedAutoBrightness);
        return newBrightness;
    }

    public float adjustBrightness(float brightness, BrightnessReason reason) {
        float newBrightness = adjustBrightnessToPeak(brightness, true, reason);
        if (this.mThermalBrightnessControlAvailable) {
            newBrightness = adjustBrightnessByThermal(newBrightness, true, reason);
        }
        return adjustBrightnessByBattery(newBrightness, reason);
    }

    private float adjustBrightnessByThermal(float brightness, boolean isHdr, BrightnessReason reason) {
        float newBrightness = brightness;
        if (this.mThermalBrightnessControlAvailable) {
            if ((!isHdr && !isHdrScene()) || (isHdr && isHdrScene())) {
                float f = this.mThermalMaxBrightness;
                if (f != -1.0f && !Float.isNaN(f)) {
                    newBrightness = MathUtils.min(this.mThermalMaxBrightness, brightness);
                }
                if (newBrightness != brightness) {
                    reason.addModifier(64);
                }
            }
            if (brightness > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && (isHdr || !isHdrScene())) {
                startFullSceneThermalUsageStats(brightness, this.mThermalMaxBrightness, this.mCurrentConditionId, this.mCurrentTemperature, this.mOutDoorHighTempState);
            }
        }
        return newBrightness;
    }

    private float adjustBrightnessByBcbc(float brightness, BrightnessReason reason) {
        float newBrightness = brightness;
        if (isGrayScaleLegal(this.mGrayScale)) {
            newBrightness = calculateBrightnessBCBC(brightness, this.mGrayScale);
            if (brightness != newBrightness) {
                this.mAppliedBcbc = true;
                this.mBcbcBrightness = newBrightness;
                reason.addModifier(16);
                if (this.mDebug) {
                    Slog.d(TAG, "Apply bcbc brightness.");
                }
            } else if (this.mAppliedBcbc && brightness == newBrightness) {
                this.mAppliedBcbc = false;
                this.mBcbcBrightness = Float.NaN;
                resetBcbcRateModifier(false);
                if (this.mDebug) {
                    Slog.d(TAG, "Exit bcbc brightness.");
                }
            }
        }
        return newBrightness;
    }

    private float adjustBrightnessToPeak(float brightness, boolean isHdrBrightness, BrightnessReason reason) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl;
        float currentMaxBrightness = 1.0f;
        if (isSupportPeakBrightness() && (automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl) != null) {
            float currentLux = automaticBrightnessControllerImpl.getCurrentAmbientLux();
            currentMaxBrightness = getMaxHbmBrightnessForPeak();
            boolean shouldApplyPeakBrightness = isHdrVideo() && this.mAutoBrightnessEnable && currentLux > ((float) PEAK_BRIGHTNESS_AMBIENT_LUX_THRESHOLD) && isGrayScaleLegal(this.mCurrentGrayScale) && this.mCurrentGrayScale <= ((float) PEAK_BRIGHTNESS_GRAY_SCALE_THRESHOLD);
            if (shouldApplyPeakBrightness) {
                if (isHdrBrightness) {
                    currentMaxBrightness = 1.0f;
                    reason.addModifier(256);
                    if (this.mDebug) {
                        Slog.d(TAG, "Apply peak brightness, currentLux: " + currentLux + ", current gray scale: " + this.mCurrentGrayScale);
                    }
                }
            } else if (this.mLastMaxBrightness > currentMaxBrightness) {
                if (this.mDebug) {
                    Slog.d(TAG, "Exit peak brightness, currentLux: " + currentLux + ", current gray scale: " + this.mCurrentGrayScale);
                }
                this.mCurrentGrayScale = Float.NaN;
            }
            this.mLastMaxBrightness = currentMaxBrightness;
        }
        return MathUtils.min(brightness, currentMaxBrightness);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetBCBCState() {
        this.mGrayScale = Float.NaN;
        this.mBcbcBrightness = Float.NaN;
        this.mAppliedBcbc = false;
        updateBCBCStateIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetScreenGrayscaleState() {
        this.mCurrentGrayScale = Float.NaN;
        this.mLastMaxBrightness = -1.0f;
        this.mAppliedMaxOprBrightness = Float.NaN;
        this.mPendingUpdateBrightness = false;
        updateScreenGrayscaleStateIfNeeded();
    }

    private boolean isGrayScaleLegal(float grayScale) {
        return !Float.isNaN(grayScale) && grayScale > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    private boolean isHdrVideo() {
        HighBrightnessModeController highBrightnessModeController = this.mHbmController;
        return highBrightnessModeController != null && (highBrightnessModeController.isHdrLayerPresent() || this.mHbmController.isDolbyEnable());
    }

    private boolean isHdrScene() {
        HighBrightnessModeController highBrightnessModeController;
        return this.mDolbyStateEnable || ((highBrightnessModeController = this.mHbmController) != null && highBrightnessModeController.getHighBrightnessMode() == 2);
    }

    public void updateBrightnessChangeStatus(boolean animating, int displayState, boolean slowChange, boolean appliedDimming, boolean appliedLowPower, float currentBrightness, float currentSdrBrightness, float targetBrightness, float targetSdrBrightness, int displayPolicy, BrightnessReason reason, BrightnessReason reasonTemp) {
        int i;
        if (currentSdrBrightness != this.mCurrentSdrBrightness || targetBrightness != this.mTargetBrightness) {
            Slog.d(TAG, "updateBrightnessChangeStatus: animating: " + animating + ", displayState: " + displayState + ", slowChange: " + slowChange + ", appliedDimming: " + appliedDimming + ", appliedLowPower: " + appliedLowPower + ", currentBrightness: " + currentBrightness + ", currentSdrBrightness: " + currentSdrBrightness + ", targetBrightness: " + targetBrightness + ", targetSdrBrightness: " + targetSdrBrightness + ", previousDisplayPolicy: " + this.mCurrentDisplayPolicy + ", currentDisplayPolicy: " + displayPolicy);
        }
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.updateBrightnessState(isAnimating(), animating, slowChange, currentBrightness, currentSdrBrightness, targetBrightness, targetSdrBrightness, displayPolicy, this.mCurrentDisplayPolicy, this.mLastDisplayState, reasonTemp, reason);
        }
        this.mLastAnimating = animating;
        this.mLastDisplayState = displayState;
        this.mCurrentBrightness = currentBrightness;
        this.mCurrentSdrBrightness = currentSdrBrightness;
        this.mLastSlowChange = slowChange;
        this.mAppliedDimming = appliedDimming;
        this.mAppliedLowPower = appliedLowPower;
        this.mTargetBrightness = targetBrightness;
        this.mTargetSdrBrightness = targetSdrBrightness;
        this.mPreviousDisplayPolicy = this.mCurrentDisplayPolicy;
        this.mCurrentDisplayPolicy = displayPolicy;
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            i = displayPolicy;
            automaticBrightnessControllerImpl.updateSlowChangeStatus(slowChange, appliedDimming, appliedLowPower, currentBrightness, currentSdrBrightness);
        } else {
            i = displayPolicy;
        }
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            thermalBrightnessController.updateScreenState(displayState, i);
        }
    }

    private float calculateBrightnessBCBC(float brightness, float grayScale) {
        float ratio = brightness / this.mScreenBrightnessRangeMaximum;
        float f = V2;
        if (grayScale > f) {
            float[] fArr = this.mRealtimeArrayD;
            float f2 = fArr[4];
            if (ratio > f2 && ratio <= fArr[5]) {
                this.mGrayBrightnessFactor = (this.mK3 * (ratio - f2) * (grayScale - f)) + 1.0f;
            } else if (ratio > fArr[5] && ratio <= fArr[6]) {
                this.mGrayBrightnessFactor = 1.0f - ((this.mRealtimeMaxDiff * (grayScale - f)) / ((1.0f - f) * ratio));
            } else {
                if (ratio > fArr[6]) {
                    float f3 = fArr[7];
                    if (ratio < f3) {
                        this.mGrayBrightnessFactor = (this.mK4 * (ratio - f3) * (grayScale - f)) + 1.0f;
                    }
                }
                this.mGrayBrightnessFactor = 1.0f;
            }
        } else {
            if (grayScale > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                float f4 = V1;
                if (grayScale < f4) {
                    float[] fArr2 = this.mRealtimeArrayD;
                    float f5 = fArr2[0];
                    if (ratio > f5 && ratio <= fArr2[1]) {
                        this.mGrayBrightnessFactor = (this.mK1 * (ratio - f5) * (grayScale - f4)) + 1.0f;
                    } else if (ratio > fArr2[1] && ratio <= fArr2[2]) {
                        float f6 = this.mRealtimeMaxA;
                        this.mGrayBrightnessFactor = (f6 + 1.0f) - ((f6 / f4) * grayScale);
                    } else {
                        if (ratio > fArr2[2]) {
                            float f7 = fArr2[3];
                            if (ratio < f7) {
                                this.mGrayBrightnessFactor = (this.mK2 * (ratio - f7) * (grayScale - f4)) + 1.0f;
                            }
                        }
                        this.mGrayBrightnessFactor = 1.0f;
                    }
                }
            }
            this.mGrayBrightnessFactor = 1.0f;
        }
        float f8 = this.mGrayBrightnessFactor;
        float outBrightness = brightness * f8;
        if (SUPPORT_BCBC_BY_AMBIENT_LUX && f8 < 1.0f) {
            outBrightness = adjustBrightnessByLux(brightness, outBrightness);
        }
        if (this.mDebug) {
            Slog.d(TAG, " grayScale = " + grayScale + " factor = " + this.mGrayBrightnessFactor + " inBrightness = " + brightness + " outBrightness = " + outBrightness);
        }
        return outBrightness;
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x005c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private float adjustBrightnessByLux(float r9, float r10) {
        /*
            r8 = this;
            r0 = r10
            com.android.server.display.BrightnessMappingStrategy r1 = r8.mBrightnessMapper
            if (r1 == 0) goto L93
            com.android.server.display.AutomaticBrightnessControllerImpl r2 = r8.mAutomaticBrightnessControllerImpl
            if (r2 != 0) goto Lb
            goto L93
        Lb:
            float r1 = r1.convertToNits(r9)
            com.android.server.display.BrightnessMappingStrategy r2 = r8.mBrightnessMapper
            float r2 = r2.convertToNits(r10)
            com.android.server.display.AutomaticBrightnessControllerImpl r3 = r8.mAutomaticBrightnessControllerImpl
            float r3 = r3.getCurrentAmbientLux()
            float[] r4 = com.android.server.display.DisplayPowerControllerImpl.mBCBCLuxThreshold
            r5 = 0
            r6 = r4[r5]
            int r6 = (r3 > r6 ? 1 : (r3 == r6 ? 0 : -1))
            if (r6 >= 0) goto L3b
            int r6 = (r1 > r2 ? 1 : (r1 == r2 ? 0 : -1))
            if (r6 <= 0) goto L3b
            float r6 = r1 - r2
            float[] r7 = com.android.server.display.DisplayPowerControllerImpl.mBCBCNitDecreaseThreshold
            r5 = r7[r5]
            int r6 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
            if (r6 <= 0) goto L3b
            com.android.server.display.BrightnessMappingStrategy r4 = r8.mBrightnessMapper
            float r5 = r1 - r5
            float r0 = r4.convertToBrightness(r5)
            goto L58
        L3b:
            r5 = 1
            r4 = r4[r5]
            int r4 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r4 >= 0) goto L58
            int r4 = (r1 > r2 ? 1 : (r1 == r2 ? 0 : -1))
            if (r4 <= 0) goto L58
            float r4 = r1 - r2
            float[] r6 = com.android.server.display.DisplayPowerControllerImpl.mBCBCNitDecreaseThreshold
            r5 = r6[r5]
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 <= 0) goto L58
            com.android.server.display.BrightnessMappingStrategy r4 = r8.mBrightnessMapper
            float r5 = r1 - r5
            float r0 = r4.convertToBrightness(r5)
        L58:
            boolean r4 = r8.mDebug
            if (r4 == 0) goto L92
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "adjustBrightnessByLux: currentLux: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r3)
            java.lang.String r5 = ", preNit: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r1)
            java.lang.String r5 = ", currentNit: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r2)
            java.lang.String r5 = ", currentBrightness: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r10)
            java.lang.String r4 = r4.toString()
            java.lang.String r5 = "DisplayPowerControllerImpl"
            android.util.Slog.d(r5, r4)
        L92:
            return r0
        L93:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DisplayPowerControllerImpl.adjustBrightnessByLux(float, float):float");
    }

    float getGrayBrightnessFactor() {
        return this.mGrayBrightnessFactor;
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }

    private void computeBCBCAdjustmentParams() {
        float f = this.mRealtimeMaxA;
        float f2 = V1;
        float[] fArr = this.mRealtimeArrayD;
        this.mK1 = (-f) / ((fArr[1] - fArr[0]) * f2);
        this.mK2 = f / (f2 * (fArr[3] - fArr[2]));
        float f3 = this.mRealtimeMaxDiff;
        float f4 = fArr[5];
        float f5 = V2;
        this.mK3 = (-f3) / (((1.0f - f5) * f4) * (f4 - fArr[4]));
        float f6 = fArr[6];
        this.mK4 = f3 / (((1.0f - f5) * f6) * (fArr[7] - f6));
    }

    public boolean isColorInversionEnabled() {
        return this.mColorInversionEnabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateColorInversionEnabled() {
        this.mColorInversionEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, -2) != 0;
    }

    public void updateDolbyPreviewStateLocked(boolean enable, float dolbyPreviewBoostRatio) {
        if (this.mDolbyPreviewBoostAvailable) {
            if (this.mDolbyPreviewEnable != enable || this.mDolbyPreviewBoostRatio != dolbyPreviewBoostRatio) {
                this.mDolbyPreviewEnable = enable;
                this.mDolbyPreviewBoostRatio = dolbyPreviewBoostRatio;
                this.mDisplayPowerController.updateBrightness();
            }
        }
    }

    public boolean isDolbyPreviewEnable() {
        return this.mDolbyPreviewEnable;
    }

    public float getDolbyPreviewBoostRatio() {
        float f = this.mDolbyPreviewBoostRatio;
        if (f < 1.0f || f > DOLBY_PREVIEW_MAX_BOOST_RATIO) {
            Slog.e(TAG, "getDolbyPreviewBoostRatio: current dolby preview boost ratio is invalid.");
            return Float.NaN;
        }
        return f;
    }

    public void updateGalleryHdrState(boolean enable) {
        if (this.mIsGalleryHdrEnable != enable) {
            this.mIsGalleryHdrEnable = enable;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public void updateGalleryHdrThermalThrottler(boolean throttled) {
        if (this.mGalleryHdrThrottled != throttled) {
            this.mGalleryHdrThrottled = throttled;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public boolean isGalleryHdrEnable() {
        return mSupportGalleryHdr && this.mIsGalleryHdrEnable && !this.mGalleryHdrThrottled;
    }

    protected boolean isEnterGalleryHdr() {
        if (isGalleryHdrEnable() && !this.mLastSlowChange) {
            float f = this.mTargetBrightness;
            if (f > this.mTargetSdrBrightness && !this.mAppliedDimming && f > this.mCurrentBrightness) {
                return true;
            }
        }
        return false;
    }

    protected boolean isExitGalleryHdr() {
        return (this.mCurrentBrightness == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || isGalleryHdrEnable() || this.mCurrentGalleryHdrBoostFactor <= 1.0f) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getGalleryHdrBoostFactor(float sdrBacklight, float hdrBacklight) {
        if (!isGalleryHdrEnable() && this.mCurrentGalleryHdrBoostFactor == 1.0f) {
            return 1.0f;
        }
        String tempReason = "null";
        float hdrNit = this.mDisplayDeviceConfig.getNitsFromBacklight(hdrBacklight);
        float sdrNit = this.mDisplayDeviceConfig.getNitsFromBacklight(sdrBacklight);
        float factor = calculateGalleryHdrBoostFactor(hdrNit, sdrNit);
        if (isEnterGalleryHdr()) {
            tempReason = "enter_gallery_hdr_boost";
        } else if (isExitGalleryHdr()) {
            tempReason = "exit_gallery_hdr_boost";
        } else if (this.mAppliedDimming && isGalleryHdrEnable()) {
            tempReason = "enter_dim_state";
        }
        this.mCurrentGalleryHdrBoostFactor = factor;
        if (this.mDebug) {
            Slog.d(TAG, "getGalleryHdrBoostFactor: reason:" + tempReason + ", hdrBrightness: " + hdrBacklight + ", sdrBrightness: " + sdrBacklight + ", mCurrentBrightness: " + this.mCurrentBrightness + ", mCurrentSdrBrightness: " + this.mCurrentSdrBrightness + ", hdrNit: " + hdrNit + ", sdrNit: " + sdrNit + ", factor: " + factor);
        }
        return factor;
    }

    private float calculateGalleryHdrBoostFactor(float hdrNit, float sdrNit) {
        if (hdrNit == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || sdrNit == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || Float.isNaN(sdrNit) || Float.isNaN(hdrNit)) {
            return 1.0f;
        }
        float factor = BigDecimal.valueOf(hdrNit / sdrNit).setScale(3, RoundingMode.HALF_UP).floatValue();
        return MathUtils.constrain(factor, 1.0f, 2.25f);
    }

    public void temporaryBrightnessStartChange(float brightness) {
        if (isOverrideBrightnessPolicyEnable() && this.mAppliedScreenBrightnessOverride) {
            WindowManagerServiceStub.get().notifySystemBrightnessChange();
            this.mAppliedScreenBrightnessOverride = false;
        }
        if (this.mThermalBrightnessControlAvailable && this.mDisplayId == 0 && isInOutdoorCriticalTemperature() && !this.mShowOutdoorHighTempToast && brightness > this.mLastTemporaryBrightness) {
            this.mShowOutdoorHighTempToast = true;
            this.mInjector.showOutdoorHighTempToast();
        }
        this.mLastTemporaryBrightness = brightness;
    }

    public void setAppliedScreenBrightnessOverride(boolean isApplied) {
        if (this.mAppliedScreenBrightnessOverride != isApplied) {
            if (this.mAutoBrightnessEnable && !isApplied) {
                resetBCBCState();
            }
            this.mAppliedScreenBrightnessOverride = isApplied;
            RampRateController rampRateController = this.mRampRateController;
            if (rampRateController != null) {
                rampRateController.addOverrideRateModifier(isApplied);
            }
        }
    }

    private boolean isOverrideBrightnessPolicyEnable() {
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null) {
            return miuiDisplayCloudController.isOverrideBrightnessPolicyEnable();
        }
        return false;
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.Callback
    public void notifyDisableResetShortTermModel(boolean enable) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.notifyDisableResetShortTermModel(enable);
        }
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.Callback
    public void notifyThresholdSunlightNitChanged(float thresholdSunlightNit) {
        SunlightController sunlightController = this.mSunlightController;
        if (sunlightController != null) {
            sunlightController.updateThresholdSunlightNit(Float.valueOf(thresholdSunlightNit));
        }
    }

    public void settingBrightnessStartChange(float brightness) {
        if (this.mShowOutdoorHighTempToast) {
            this.mShowOutdoorHighTempToast = false;
            this.mLastTemporaryBrightness = Float.NaN;
        }
    }

    private boolean isInOutdoorCriticalTemperature() {
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            return thermalBrightnessController.isInOutdoorCriticalTemperature();
        }
        return false;
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
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -551230169:
                    if (lastPathSegment.equals("accessibility_display_inversion_enabled")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -267787360:
                    if (lastPathSegment.equals(DisplayPowerControllerImpl.KEY_CURTAIN_ANIM_ENABLED)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1108440478:
                    if (lastPathSegment.equals(DisplayPowerControllerImpl.KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW)) {
                        c = 3;
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
                    DisplayPowerControllerImpl.this.updateColorInversionEnabled();
                    return;
                case 1:
                    DisplayPowerControllerImpl.this.updateAutoBrightnessMode();
                    return;
                case 2:
                case 3:
                    DisplayPowerControllerImpl.this.updateCurtainAnimationEnabled();
                    return;
                default:
                    return;
            }
        }
    }

    public void receiveNoticeFromDisplayPowerController(int code, Bundle bundle) {
        switch (code) {
            case 1:
                updateGrayScale(bundle);
                return;
            case 2:
            case 4:
            default:
                return;
            case 3:
                updateDolbyState(bundle);
                return;
            case 5:
                updateCurrentGrayScale(bundle);
                return;
        }
    }

    public void updateGrayScale(Bundle bundle) {
        Message msg = this.mHandler.obtainMessage(1);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setGrayScale(float grayScale) {
        this.mGrayScale = grayScale;
        this.mBrightnessDataProcessor.updateGrayScale(grayScale);
        this.mDisplayPowerController.updateBrightness();
    }

    public void updateCurrentGrayScale(Bundle bundle) {
        this.mHandler.removeMessages(6);
        Message msg = this.mHandler.obtainMessage(6);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCurrentGrayScale(float grayScale) {
        if (isGrayScaleLegal(grayScale) && this.mCurrentGrayScale != grayScale) {
            updateBrightnessForOpr(grayScale);
            updateBrightnessForPeak(grayScale);
        }
    }

    private void updateBrightnessForPeak(float grayScale) {
        this.mCurrentGrayScale = grayScale;
        boolean updateBrightnessForPeak = isHdrVideo() && grayScale <= ((float) PEAK_BRIGHTNESS_GRAY_SCALE_THRESHOLD);
        if (this.mPendingUpdateBrightness != updateBrightnessForPeak) {
            this.mPendingUpdateBrightness = updateBrightnessForPeak;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    private void updateBrightnessForOpr(float grayScale) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl;
        boolean updateBrightnessForOpr = (isHdrScene() || (automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl) == null || automaticBrightnessControllerImpl.getCurrentAmbientLux() <= ((float) this.mOprAmbientLuxThreshold)) ? false : true;
        if (updateBrightnessForOpr) {
            float currentRestrictedBrightness = getRestrictedOprBrightness(this.mCurrentGrayScale);
            float pendingRestrictedOprBrightness = getRestrictedOprBrightness(grayScale);
            this.mCurrentGrayScale = grayScale;
            if (currentRestrictedBrightness != pendingRestrictedOprBrightness) {
                this.mDisplayPowerController.updateBrightness();
            }
        }
    }

    private void updateDolbyState(Bundle bundle) {
        Message msg = this.mHandler.obtainMessage(2);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayPowerControllerImplHandler extends Handler {
        public DisplayPowerControllerImplHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPowerControllerImpl.this.setGrayScale(msg.getData().getFloat("gray_scale"));
                    return;
                case 2:
                    DisplayPowerControllerImpl.this.updateDolbyBrightnessIfNeeded(msg.getData().getBoolean("dolby_version_state", false));
                    return;
                case 3:
                    DisplayPowerControllerImpl.this.updateForegroundAppSync();
                    return;
                case 4:
                    DisplayPowerControllerImpl.this.updateForegroundApp();
                    return;
                case 5:
                    DisplayPowerControllerImpl.this.updateThermalBrightness(Float.intBitsToFloat(((Integer) msg.obj).intValue()));
                    return;
                case 6:
                    DisplayPowerControllerImpl.this.setCurrentGrayScale(msg.getData().getFloat("current_gray_scale"));
                    return;
                case 7:
                    DisplayPowerControllerImpl.this.mOrientation = ((Integer) msg.obj).intValue();
                    if (DisplayPowerControllerImpl.this.mCbmController != null && DisplayPowerControllerImpl.this.mAutoBrightnessEnable) {
                        DisplayPowerControllerImpl.this.mCbmController.updateCustomSceneState(DisplayPowerControllerImpl.this.mForegroundAppPackageName, DisplayPowerControllerImpl.this.mOrientation);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateThermalBrightness(float thermalNit) {
        DisplayDeviceConfig displayDeviceConfig = this.mDisplayDeviceConfig;
        if (displayDeviceConfig == null) {
            Slog.e(TAG, "updateThermalBrightness: no valid display device config!");
            return;
        }
        float thermalBrightness = displayDeviceConfig.getBrightnessFromNit(thermalNit);
        if (thermalBrightness != this.mThermalMaxBrightness) {
            this.mThermalMaxBrightness = thermalBrightness;
            if (Float.isNaN(thermalBrightness)) {
                resetThermalRateModifier();
            }
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public void setUpAutoBrightness(BrightnessMappingStrategy brightnessMapper, AutomaticBrightnessControllerStub stub, DisplayDeviceConfig displayDeviceConfig, Sensor lightSensor) {
        this.mBrightnessMapper = brightnessMapper;
        this.mDisplayDeviceConfig = displayDeviceConfig;
        this.mAutomaticBrightnessControllerImpl = (AutomaticBrightnessControllerImpl) stub;
        brightnessMapper.setDisplayPowerControllerImpl(this);
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.setUpAutoBrightness(this, this.mBrightnessMapper, this.mDisplayDeviceConfig, this.mLogicalDisplay);
            BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
            if (brightnessDataProcessor != null) {
                brightnessDataProcessor.setUpCloudControllerListener(this.mCloudListener);
                this.mBrightnessDataProcessor.setDisplayDeviceConfig(this.mDisplayDeviceConfig);
                this.mBrightnessDataProcessor.setBrightnessMapper(this.mBrightnessMapper);
            }
            CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
            if (customBrightnessModeController != null) {
                customBrightnessModeController.setAutoBrightnessComponent(this.mBrightnessMapper);
            }
            DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = this.mDisplayDeviceConfig.getHighBrightnessModeData();
            if (highBrightnessModeData != null) {
                recalculationForBCBC(0.5f);
                this.mHbmController.supportHdrBrightenHbm(this.SUPPORT_HDR_HBM_BRIGHTEN);
                this.mHbmController.registerListener(this.mHdrStateListener);
            }
            BrightnessABTester brightnessABTester = this.mBrightnessABTester;
            if (brightnessABTester != null) {
                brightnessABTester.setAutomaticBrightnessControllerImpl(this.mAutomaticBrightnessControllerImpl);
            }
            BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
            if (brightnessMappingStrategy != null) {
                float f = MAX_POWER_SAVE_MODE_NIT;
                if (f != -1.0f) {
                    this.mMaxPowerSaveModeBrightness = brightnessMappingStrategy.convertToBrightness(f);
                }
            }
        }
        updateAmbientLightSensor(lightSensor);
    }

    public void setRampAnimator(RampAnimator.DualRampAnimator<DisplayPowerState> rampAnimator) {
        this.mBrightnessRampAnimator = rampAnimator;
    }

    private void recalculationForBCBC(float coefficient) {
        if (this.mInitialBCBCParameters) {
            return;
        }
        this.mInitialBCBCParameters = true;
        int i = 0;
        while (true) {
            float[] fArr = this.mRealtimeArrayD;
            if (i < fArr.length) {
                fArr[i] = BigDecimal.valueOf(DATA_D[i] * coefficient).setScale(6, 4).floatValue();
                i++;
            } else {
                this.mRealtimeMaxDiff = BigDecimal.valueOf(MAX_DIFF * coefficient).setScale(6, 4).floatValue();
                computeBCBCAdjustmentParams();
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAutoBrightnessMode() {
        this.mAutoBrightnessEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2) == 1;
        resetBCBCState();
        resetScreenGrayscaleState();
        if (!this.mAutoBrightnessEnable) {
            updateBrightnessAnimInfoIfNeeded(false);
        } else {
            this.mManualBrightnessBoostEnable = false;
        }
    }

    private void loadSettings() {
        updateColorInversionEnabled();
        updateAutoBrightnessMode();
        updateCurtainAnimationEnabled();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: registerForegroundAppUpdater, reason: merged with bridge method [inline-methods] */
    public void lambda$init$0() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            ProcessManager.registerForegroundWindowListener(this.mForegroundWindowListener);
            updateForegroundApp();
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() {
            DisplayPowerControllerImpl.this.mHandler.removeMessages(4);
            DisplayPowerControllerImpl.this.mHandler.sendEmptyMessage(4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApp() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl.4
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = DisplayPowerControllerImpl.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null && info.getWindowingMode() != 5 && info.getWindowingMode() != 6 && !DisplayPowerControllerImpl.this.mActivityTaskManager.isInSplitScreenWindowingMode()) {
                        String packageName = info.topActivity.getPackageName();
                        if (packageName != null && packageName.equals(DisplayPowerControllerImpl.this.mForegroundAppPackageName)) {
                            return;
                        }
                        DisplayPowerControllerImpl.this.mPendingForegroundAppPackageName = packageName;
                        DisplayPowerControllerImpl.this.mHandler.sendEmptyMessage(3);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundAppSync() {
        String str = this.mPendingForegroundAppPackageName;
        this.mForegroundAppPackageName = str;
        this.mPendingForegroundAppPackageName = null;
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null && this.mAutoBrightnessEnable) {
            customBrightnessModeController.updateCustomSceneState(str, this.mOrientation);
        }
        if (this.mAutoBrightnessEnable) {
            updateBCBCStateIfNeeded();
            updateGameSceneEnable();
        } else {
            updateManualBrightnessBoostState();
        }
        updateThermalBrightnessBoostState();
    }

    public void updateBCBCStateIfNeeded() {
        MiuiDisplayCloudController miuiDisplayCloudController;
        if (BCBC_ENABLE && (miuiDisplayCloudController = this.mMiuiDisplayCloudController) != null) {
            int state = (miuiDisplayCloudController.getBCBCAppList().contains(this.mForegroundAppPackageName) && this.mAutoBrightnessEnable && !isHdrScene() && this.mIsScreenOn) ? 1 : 0;
            if (state != this.mBCBCState) {
                this.mBCBCState = state;
                this.mDisplayFeatureManagerServicImpl.updateBCBCState(state);
                if (this.mDebug) {
                    Slog.d(TAG, (state == 1 ? "Enter " : "Exit ") + "BCBC State, mForegroundAppPackageName = " + this.mForegroundAppPackageName + ", mAutoBrightnessEnable = " + this.mAutoBrightnessEnable);
                    return;
                }
                return;
            }
            if (this.mDebug) {
                Slog.d(TAG, "Skip BCBC State, mBCBCState = " + this.mBCBCState);
            }
        }
    }

    public void updateScreenGrayscaleStateIfNeeded() {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl;
        boolean peakState = isSupportPeakBrightness() && isHdrVideo();
        boolean oprState = this.mOprBrightnessControlAvailable && !isHdrScene() && (automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl) != null && automaticBrightnessControllerImpl.getCurrentAmbientLux() > ((float) this.mOprAmbientLuxThreshold);
        boolean state = this.mAutoBrightnessEnable && this.mIsScreenOn && (peakState || oprState);
        if (state != this.mScreenGrayscaleState) {
            Slog.d(TAG, (state ? "Starting" : "Ending") + " update screen gray scale.");
            this.mScreenGrayscaleState = state;
            this.mDisplayFeatureManagerServicImpl.updateScreenGrayscaleState(state ? 1 : 0);
        }
    }

    public boolean isSupportPeakBrightness() {
        return MAX_HBM_BRIGHTNESS_FOR_PEAK != 1.0f;
    }

    public float getMaxHbmBrightnessForPeak() {
        return MAX_HBM_BRIGHTNESS_FOR_PEAK;
    }

    public float getClampedBrightnessForPeak(float brightnessValue) {
        HighBrightnessModeController highBrightnessModeController;
        if (isSupportPeakBrightness() && (highBrightnessModeController = this.mHbmController) != null) {
            return MathUtils.min(brightnessValue, highBrightnessModeController.getCurrentBrightnessMax());
        }
        return brightnessValue;
    }

    private boolean isKeyguardOn() {
        return this.mPolicy.isKeyguardShowing() || this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public void updateFastRateStatus(float brightness) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.updateFastRateStatus(brightness);
        }
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.Callback
    public void updateOutdoorThermalAppCategoryList(final List<String> outdoorThermalAppList) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerControllerImpl.this.lambda$updateOutdoorThermalAppCategoryList$2(outdoorThermalAppList);
            }
        }, DELAY_TIME);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateOutdoorThermalAppCategoryList$2(List outdoorThermalAppList) {
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            thermalBrightnessController.updateOutdoorThermalAppCategoryList(outdoorThermalAppList);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCurtainAnimationEnabled() {
        this.mCurtainAnimationEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_CURTAIN_ANIM_ENABLED, 1, -2) == 1;
        this.mIsDynamicLockScreenShowing = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW, 0, -2) == 1;
    }

    private float adjustBrightnessByOpr(float brightness, BrightnessReason reason) {
        if (this.mDisplayDeviceConfig == null) {
            Slog.e(TAG, "adjustBrightnessByOpr: no valid display device config!");
            return brightness;
        }
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        boolean shouldResetScreenGrayscaleState = automaticBrightnessControllerImpl != null && automaticBrightnessControllerImpl.getCurrentAmbientLux() > ((float) this.mOprAmbientLuxThreshold);
        if (this.mPendingResetGrayscaleStateForOpr != shouldResetScreenGrayscaleState) {
            this.mPendingResetGrayscaleStateForOpr = shouldResetScreenGrayscaleState;
            resetScreenGrayscaleState();
        }
        float maxOprBrightness = this.mDisplayDeviceConfig.getBrightnessFromNit(this.mOprMaxNitThreshold);
        if (maxOprBrightness == -1.0f) {
            return brightness;
        }
        if (!this.mPendingResetGrayscaleStateForOpr && brightness > maxOprBrightness) {
            if (this.mDebug) {
                Slog.d(TAG, "adjustBrightnessByOpr: constrain brightnesswhen current lux is below opr ambient lux threshold.");
            }
            reason.addModifier(512);
            return maxOprBrightness;
        }
        float restrictedOprBrightness = getRestrictedOprBrightness(this.mCurrentGrayScale);
        if (Float.isNaN(restrictedOprBrightness)) {
            return brightness;
        }
        if (restrictedOprBrightness != this.mAppliedMaxOprBrightness) {
            this.mAppliedMaxOprBrightness = restrictedOprBrightness;
        }
        if (brightness > this.mAppliedMaxOprBrightness) {
            if (this.mDebug) {
                Slog.d(TAG, "adjustBrightnessByOpr: current brightness: " + brightness + " is constrained to target brightness: " + this.mAppliedMaxOprBrightness);
            }
            reason.addModifier(512);
            return this.mAppliedMaxOprBrightness;
        }
        return brightness;
    }

    private float getRestrictedOprBrightness(float grayScale) {
        int[] iArr;
        int[] iArr2;
        if (!isGrayScaleLegal(grayScale) || this.mDisplayDeviceConfig == null || (iArr = this.mOprGrayscaleThreshold) == null || (iArr2 = this.mOprNitThreshold) == null || iArr.length == 0 || iArr2.length == 0) {
            return Float.NaN;
        }
        int index = 0;
        while (true) {
            if (index >= this.mOprGrayscaleThreshold.length || grayScale <= r1[index]) {
                break;
            }
            index++;
        }
        return this.mDisplayDeviceConfig.getBrightnessFromNit(this.mOprNitThreshold[index]);
    }

    private float adjustBrightnessByPowerSaveMode(float brightness, BrightnessReason reason) {
        if (!Float.isNaN(this.mMaxPowerSaveModeBrightness)) {
            float newBrightness = MathUtils.min(this.mMaxPowerSaveModeBrightness, brightness);
            if (newBrightness != brightness) {
                reason.addModifier(1024);
                return newBrightness;
            }
        }
        return brightness;
    }

    private void updateThermalBrightnessBoostState() {
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            thermalBrightnessController.updateForegroundApp(this.mForegroundAppPackageName);
        }
    }

    public void dump(PrintWriter pw) {
        if (this.mSunlightModeAvailable) {
            this.mSunlightController.dump(pw);
            pw.println("  mAppliedSunlightMode=" + this.mAppliedSunlightMode);
            pw.println("  mLastSettingsBrightnessBeforeApplySunlight=" + this.mLastSettingsBrightnessBeforeApplySunlight);
            pw.println("  SUPPORT_MANUAL_BRIGHTNESS_BOOST=" + this.SUPPORT_MANUAL_BRIGHTNESS_BOOST);
            pw.println("  mMaxManualBoostBrightness=" + this.mMaxManualBoostBrightness);
            pw.println("  mManualBrightnessBoostEnable=" + this.mManualBrightnessBoostEnable);
            pw.println("  mLastManualBoostBrightness=" + this.mLastManualBoostBrightness);
            pw.println("  mIsSupportManualBoostApp=" + this.mIsSupportManualBoostApp);
        }
        pw.println("");
        pw.println("BCBC_ENABLE=" + BCBC_ENABLE);
        pw.println("  mGrayScale=" + this.mGrayScale);
        pw.println("  mAppliedBcbc=" + this.mAppliedBcbc);
        pw.println("  mBcbcBrightness=" + this.mBcbcBrightness);
        pw.println("  mCurrentGrayScale=" + this.mCurrentGrayScale);
        pw.println("  mColorInversionEnabled=" + this.mColorInversionEnabled);
        if (this.mCurtainAnimationAvailable) {
            pw.println("");
            pw.println("Curtain animation state: ");
            pw.println("  mCurtainAnimationAvailable=" + this.mCurtainAnimationAvailable);
            pw.println("  mCurtainAnimationEnabled=" + this.mCurtainAnimationEnabled);
            pw.println("  mIsDynamicLockScreenShowing=" + this.mIsDynamicLockScreenShowing);
            pw.println("  mPendingShowCurtainAnimation=" + this.mPendingShowCurtainAnimation);
        }
        boolean z = mSupportGalleryHdr;
        if (z) {
            pw.println("");
            pw.println("Gallery Hdr Boost:");
            pw.println("  mSupportGalleryHdr=" + z);
            pw.println("  mIsGalleryHdrEnable=" + this.mIsGalleryHdrEnable);
            pw.println("  mGalleryHdrThrottled=" + this.mGalleryHdrThrottled);
            pw.println("  mCurrentGalleryHdrBoostFactor=" + this.mCurrentGalleryHdrBoostFactor);
            ThermalObserver thermalObserver = this.mThermalObserver;
            if (thermalObserver != null) {
                thermalObserver.dump(pw);
            }
        }
        if (this.mDolbyPreviewBoostAvailable) {
            pw.println("");
            pw.println("Dolby Preview Brightness Boost:");
            pw.println("  mDolbyPreviewBoostAvailable=" + this.mDolbyPreviewBoostAvailable);
            pw.println("  mDolbyPreviewEnable=" + this.mDolbyPreviewEnable);
            pw.println("  mDolbyPreviewBoostRatio=" + this.mDolbyPreviewBoostRatio);
        }
        ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
        if (thermalBrightnessController != null) {
            thermalBrightnessController.dump(pw);
            pw.println("mThermalMaxBrightness=" + this.mThermalMaxBrightness);
            pw.println("mBasedSdrBrightness=" + this.mBasedSdrBrightness);
            pw.println("mBasedBrightness=" + this.mBasedBrightness);
        }
        if (this.mLowBatteryLevelThreshold.length != 0) {
            pw.println("");
            pw.println("Low Battery Level:");
            pw.println("  mLowBatteryLevelThreshold=" + Arrays.toString(this.mLowBatteryLevelThreshold));
            pw.println("  mLowBatteryLevelBrightnessThreshold=" + Arrays.toString(this.mLowBatteryLevelBrightnessThreshold));
        }
        if (this.mOprBrightnessControlAvailable) {
            pw.println("");
            pw.println("Opr Brightness Control:");
            pw.println("  mOprBrightnessControlAvailable=" + this.mOprBrightnessControlAvailable);
            pw.println("  mOprGrayscaleThreshold=" + Arrays.toString(this.mOprGrayscaleThreshold));
            pw.println("  mOprNitThreshold=" + Arrays.toString(this.mOprNitThreshold));
            pw.println("  mOprAmbientLuxThreshold=" + this.mOprAmbientLuxThreshold);
            pw.println("  mAppliedMaxOprBrightness=" + this.mAppliedMaxOprBrightness);
            pw.println("  mOprMaxNitThreshold=" + this.mOprMaxNitThreshold);
        }
        if (this.mCbmController != null) {
            pw.println("");
            pw.println("Cbm Config: ");
            this.mCbmController.dump(pw);
        }
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.dump(pw);
        }
        this.mDebug = DisplayDebugConfig.DEBUG_DPC;
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null) {
            miuiDisplayCloudController.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDolbyBrightnessIfNeeded(boolean enable) {
        if (this.SUPPORT_DOLBY_VERSION_BRIGHTEN && this.mHbmController != null && this.mDolbyStateEnable != enable) {
            this.mDolbyStateEnable = enable;
            RampRateController rampRateController = this.mRampRateController;
            if (rampRateController != null) {
                rampRateController.addHdrRateModifier(enable);
            }
            this.mHbmController.setDolbyEnable(this.mDolbyStateEnable);
            if (this.mAutoBrightnessEnable) {
                resetBCBCState();
                resetScreenGrayscaleState();
            }
            this.mDisplayPowerController.updateBrightness();
            ThermalBrightnessController thermalBrightnessController = this.mThermalBrightnessController;
            if (thermalBrightnessController != null) {
                thermalBrightnessController.setDolbyEnabled(this.mDolbyStateEnable);
            }
        }
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
        BatteryReceiver batteryReceiver = this.mBatteryReceiver;
        if (batteryReceiver != null) {
            this.mContext.unregisterReceiver(batteryReceiver);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerControllerImpl.this.lambda$stop$3();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$stop$3() {
        ProcessManager.unregisterForegroundWindowListener(this.mForegroundWindowListener);
    }

    public void showTouchCoverProtectionRect(boolean isShow) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.showTouchCoverProtectionRect(isShow);
        }
    }

    public void notifyUpdateBrightnessAnimInfo(float currentBrightnessAnim, float brightnessAnim, float targetBrightnessAnim) {
        if (this.mBrightnessDataProcessor != null && this.mUpdateBrightnessAnimInfoEnable && !isTemporaryDimmingEnabled()) {
            this.mBrightnessDataProcessor.notifyUpdateBrightnessAnimInfo(currentBrightnessAnim, brightnessAnim, targetBrightnessAnim);
        }
    }

    public void notifyUpdateTempBrightnessTimeStamp(boolean enable) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.notifyUpdateTempBrightnessTimeStampIfNeeded(enable);
        }
    }

    public void updateBrightnessAnimInfoIfNeeded(boolean enable) {
        BrightnessDataProcessor brightnessDataProcessor;
        if (enable != this.mUpdateBrightnessAnimInfoEnable && (brightnessDataProcessor = this.mBrightnessDataProcessor) != null) {
            this.mUpdateBrightnessAnimInfoEnable = enable;
            if (!enable) {
                brightnessDataProcessor.notifyResetBrightnessAnimInfo();
            }
        }
    }

    private void updateGameSceneEnable() {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.updateGameSceneEnable(this.mMiuiDisplayCloudController.getTouchCoverProtectionGameList().contains(this.mForegroundAppPackageName));
        }
    }

    public void onBootCompleted() {
        this.mBootCompleted = true;
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.onBootCompleted();
        }
    }

    public void sendDimStateToSurfaceFlinger(boolean z) {
        if (!SUPPORT_IDLE_DIM || this.mDisplayId != 0) {
            return;
        }
        if (this.mDebug) {
            Slog.d(TAG, "sendDimStateToSurfaceFlinger is dim " + z);
        }
        if (this.mISurfaceFlinger != null) {
            Parcel obtain = Parcel.obtain();
            obtain.writeInterfaceToken("android.ui.ISurfaceComposer");
            obtain.writeInt(DISPLAY_DIM_STATE);
            obtain.writeInt(z ? 1 : 0);
            obtain.writeString(PACKAGE_DIM_SYSTEM);
            try {
                try {
                    this.mISurfaceFlinger.transact(TRANSACTION_NOTIFY_DIM, obtain, null, 1);
                } catch (RemoteException | SecurityException e) {
                    Slog.e(TAG, "Failed to send brightness to SurfaceFlinger", e);
                }
            } finally {
                obtain.recycle();
            }
        }
    }

    private float adjustBrightnessByBattery(float brightness, BrightnessReason reason) {
        float newBrightness = Math.min(brightness, this.mLowBatteryLimitBrightness);
        if (newBrightness != brightness) {
            reason.addModifier(128);
        }
        return newBrightness;
    }

    private void registerBroadcastsReceiver() {
        if (this.mLowBatteryLevelThreshold.length != 0 && this.mLowBatteryLevelBrightnessThreshold.length != 0) {
            this.mBatteryReceiver = new BatteryReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.setPriority(1000);
            this.mContext.registerReceiver(this.mBatteryReceiver, filter, null, this.mHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBatteryBrightness(int batteryLevel) {
        int index = 0;
        float lowBatteryBrightness = 1.0f;
        while (true) {
            int[] iArr = this.mLowBatteryLevelThreshold;
            if (index >= iArr.length || batteryLevel <= iArr[index]) {
                break;
            } else {
                index++;
            }
        }
        if (index < this.mLowBatteryLevelBrightnessThreshold.length) {
            lowBatteryBrightness = this.mDisplayDeviceConfig.getBrightnessFromNit(r2[index]);
        }
        if (lowBatteryBrightness != this.mLowBatteryLimitBrightness) {
            this.mLowBatteryLimitBrightness = lowBatteryBrightness;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BatteryReceiver extends BroadcastReceiver {
        private BatteryReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            DisplayPowerControllerImpl.this.updateBatteryBrightness(intent.getIntExtra("level", 0));
        }
    }

    /* loaded from: classes.dex */
    static class Injector {
        private Toast mOutdoorHighTempToast;

        public Injector(Context context) {
            this.mOutdoorHighTempToast = Toast.makeText(context, context.getString(286196274), 0);
        }

        public void showOutdoorHighTempToast() {
            this.mOutdoorHighTempToast.cancel();
            this.mOutdoorHighTempToast.show();
        }
    }

    public void updateScreenState(float brightnessState, int policy) {
        boolean state = brightnessState > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && this.mDisplayPowerController.getDisplayPowerState().getScreenState() == 2 && (policy == 3 || policy == 2);
        if (this.mIsScreenOn != state) {
            this.mIsScreenOn = state;
            startUpdateThermalStats(brightnessState, state);
            resetScreenGrayscaleState();
            resetBCBCState();
        }
    }

    private void startUpdateThermalStats(float brightnessState, boolean isScreenOn) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            float f = this.mCurrentTemperature;
            brightnessDataProcessor.updateThermalStats(brightnessState, isScreenOn, f, this.mLastTemperature != f);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyOutDoorHighTempState$4(boolean changed) {
        this.mOutDoorHighTempState = changed;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyOutDoorHighTempState(final boolean changed) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerControllerImpl.this.lambda$notifyOutDoorHighTempState$4(changed);
            }
        });
        if (this.mDebug) {
            Slog.d(TAG, "notifyOutDoorState: mOutDoorHighTempState: " + changed);
        }
    }

    private void startFullSceneThermalUsageStats(float brightness, float thermalBrightness, int currentConditionId, float temperature, boolean outdoorHighTempState) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.noteFullSceneThermalUsageStats(brightness, thermalBrightness, currentConditionId, temperature, outdoorHighTempState);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void startDetailThermalUsageStatsOnThermalChanged(final int conditionId, final float temperature, final boolean brightnessChanged) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerControllerImpl.this.lambda$startDetailThermalUsageStatsOnThermalChanged$5(conditionId, temperature, brightnessChanged);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startDetailThermalUsageStatsOnThermalChanged$5(int conditionId, float temperature, boolean brightnessChanged) {
        this.mCurrentConditionId = conditionId;
        float f = this.mCurrentTemperature;
        this.mLastTemperature = f;
        this.mCurrentTemperature = temperature;
        startAverageTemperatureStats(temperature, f != temperature);
        if (!brightnessChanged) {
            startDetailsThermalUsageStats(conditionId, temperature);
        }
    }

    private void startDetailsThermalUsageStats(int conditionId, float temperature) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.noteDetailThermalUsage(conditionId, temperature);
        }
    }

    private void startAverageTemperatureStats(float temperature, boolean needComputed) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.noteAverageTemperature(temperature, needComputed);
        }
    }

    protected boolean isTemporaryDimmingEnabled() {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController == null) {
            return false;
        }
        boolean enable = rampRateController.isTemporaryDimming();
        return enable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RotationWatcher extends IRotationWatcher.Stub {
        private RotationWatcher() {
        }

        public void onRotationChanged(int rotation) throws RemoteException {
            Message msg = Message.obtain(DisplayPowerControllerImpl.this.mHandler, 7, Integer.valueOf(rotation));
            msg.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void registerRotationWatcher(boolean enable) {
        RotationWatcher rotationWatcher;
        if ((this.mSupportIndividualBrightness || this.mSupportCustomBrightness) && (rotationWatcher = this.mRotationWatcher) != null) {
            if (enable) {
                if (!this.mRotationListenerEnabled) {
                    this.mRotationListenerEnabled = true;
                    this.mWms.watchRotation(rotationWatcher, 0);
                    Slog.d(TAG, "Register rotation listener.");
                    return;
                }
                return;
            }
            if (this.mRotationListenerEnabled) {
                this.mRotationListenerEnabled = false;
                this.mWms.removeRotationWatcher(rotationWatcher);
                Slog.d(TAG, "Unregister rotation listener.");
            }
        }
    }

    public void updateAutoBrightness() {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.update();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getCustomBrightness(float lux, String packageName, int category, float oldAutoBrightness, float newAutoBrightness, boolean isManuallySet) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            return customBrightnessModeController.getCustomBrightness(lux, packageName, category, oldAutoBrightness, newAutoBrightness, this.mOrientation, isManuallySet);
        }
        return newAutoBrightness;
    }

    public void setScreenBrightnessByUser(float lux, float brightness, String packageName) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        float unAdjustedBrightness = brightnessMappingStrategy != null ? brightnessMappingStrategy.getBrightness(lux) : -1.0f;
        boolean isBrightening = unAdjustedBrightness < brightness;
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.setScreenBrightnessByUser(lux, isBrightening, packageName, this.mOrientation);
        }
    }

    public void resetShortTermModel(boolean manually) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.resetShortTermModel(manually);
        }
    }

    public void setBrightnessConfiguration(BrightnessConfiguration config) {
        this.mDisplayPowerController.setBrightnessConfiguration(config, false);
    }

    public float getHbmDataMinimumLux() {
        HighBrightnessModeController highBrightnessModeController = this.mHbmController;
        if (highBrightnessModeController == null || !highBrightnessModeController.deviceSupportsHbm()) {
            return HBM_MINIMUM_LUX;
        }
        float minimumLux = this.mHbmController.getHbmData().minimumLux;
        return minimumLux;
    }

    public float getNormalMaxBrightness() {
        HighBrightnessModeController highBrightnessModeController = this.mHbmController;
        if (highBrightnessModeController != null && this.mDisplayDeviceConfig != null) {
            return highBrightnessModeController.getNormalBrightnessMax();
        }
        return 1.0f;
    }

    public float getMinBrightness() {
        HighBrightnessModeController highBrightnessModeController = this.mHbmController;
        if (highBrightnessModeController != null && this.mDisplayDeviceConfig != null) {
            return highBrightnessModeController.getCurrentBrightnessMin();
        }
        return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void startCbmStatsJob() {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.startCbmStatsJob();
        }
    }

    public void setCustomCurveEnabledOnCommand(boolean enable) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.setCustomCurveEnabledOnCommand(enable);
        }
    }

    public void setIndividualModelEnabledOnCommand(boolean enable) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.setIndividualModelEnabledOnCommand(enable);
        }
    }

    public void setForceTrainEnabledOnCommand(boolean enable) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.setForceTrainEnabledOnCommand(enable);
        }
    }

    private int getSceneState() {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController == null) {
            return -1;
        }
        int state = customBrightnessModeController.getCurrentSceneState();
        return state;
    }

    public boolean isAnimating() {
        RampAnimator.DualRampAnimator<DisplayPowerState> dualRampAnimator = this.mBrightnessRampAnimator;
        if (dualRampAnimator != null) {
            return dualRampAnimator.isAnimating();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyUpdateForegroundApp() {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.notifyUpdateForegroundApp();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateCustomSceneState(String packageName) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.updateCustomSceneState(packageName, this.mOrientation);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateCbmState(boolean autoBrightnessEnabled) {
        CustomBrightnessModeController customBrightnessModeController = this.mCbmController;
        if (customBrightnessModeController != null) {
            customBrightnessModeController.updateCbmState(autoBrightnessEnabled);
        }
    }

    public boolean isBrightnessCurveOptimizePolicyDisable() {
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        return miuiDisplayCloudController != null && miuiDisplayCloudController.isBrightnessCurveOptimizePolicyDisable();
    }

    public float clampDozeBrightness(float dozeBrightness) {
        if (Float.isNaN(dozeBrightness)) {
            return dozeBrightness;
        }
        float newDozeBrightness = dozeBrightness;
        if (!SUPPORT_MULTIPLE_AOD_BRIGHTNESS) {
            if (dozeBrightness > this.mScreenBrightnessRangeMinimum) {
                newDozeBrightness = this.mMaxDozeBrightnessFloat;
            } else {
                newDozeBrightness = this.mMinDozeBrightnessFloat;
            }
        }
        if (this.mDebug) {
            Slog.i(TAG, "clamp doze brightness: " + dozeBrightness + "->" + newDozeBrightness);
        }
        return newDozeBrightness;
    }

    public float[] getDozeBrightnessThreshold() {
        return new float[]{this.mMaxDozeBrightnessFloat, this.mMinDozeBrightnessFloat};
    }

    private void onBrightnessChanged(boolean enable) {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.onBrightnessChanged(enable, isAnimating());
        }
    }

    private void onAnimateValueChanged(boolean changed) {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.onAnimateValueChanged(changed, isAnimating());
        }
    }

    public float updateRampRate(String name, float currentBrightness, float targetBrightness, float rate) {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            return rampRateController.updateBrightnessRate(name, currentBrightness, targetBrightness, rate);
        }
        return rate;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean appliedFastRate(float currentBrightness, float targetBrightness) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            return automaticBrightnessControllerImpl.shouldUseFastRate(currentBrightness, targetBrightness);
        }
        return false;
    }

    private void resetBcbcRateModifier(boolean appliedAutoBrightness) {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null && !appliedAutoBrightness) {
            rampRateController.clearBcbcModifier();
        }
    }

    private void resetThermalRateModifier() {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.clearThermalModifier();
        }
    }

    private void resetRateModifierOnAnimateValueChanged() {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.clearBcbcModifier();
            this.mRampRateController.clearThermalModifier();
        }
    }

    public void onAnimationEnd() {
        RampRateController rampRateController = this.mRampRateController;
        if (rampRateController != null) {
            rampRateController.clearAllRateModifier();
        }
    }

    public void onAnimateValueChanged(float animateValue) {
        if (BrightnessSynchronizer.floatEquals(animateValue, this.mBasedBrightness)) {
            onAnimateValueChanged(false);
            return;
        }
        this.mBasedBrightness = animateValue;
        resetRateModifierOnAnimateValueChanged();
        onAnimateValueChanged(true);
    }

    public void notifyAonFlareEvents(int type, float preLux) {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.notifyAonFlareEvents(type, preLux);
        }
    }

    public void notifyUpdateBrightness() {
        BrightnessDataProcessor brightnessDataProcessor = this.mBrightnessDataProcessor;
        if (brightnessDataProcessor != null) {
            brightnessDataProcessor.notifyUpdateBrightness();
        }
    }

    public int getProximityNegativeDebounceDelay(int delay) {
        return 0;
    }

    public void notifyFocusedWindowChanged(String focusedPackageName) {
        OneTrackFoldStateHelper.getInstance().notifyFocusedWindowChanged(focusedPackageName);
    }

    public void notifyDisplaySwapFinished() {
        boolean foldedState = isFolded();
        if (IS_FOLDABLE_DEVICE && foldedState != this.mLastFoldedState) {
            OneTrackFoldStateHelper.getInstance().notifyDisplaySwapFinished();
            this.mLastFoldedState = foldedState;
        }
    }
}
