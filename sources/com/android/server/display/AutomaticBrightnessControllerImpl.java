package com.android.server.display;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.display.AutomaticBrightnessControllerStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.security.AccessControlImpl;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class AutomaticBrightnessControllerImpl extends AutomaticBrightnessControllerStub {
    private static final int BRIGHTENING = 1;
    private static final String[] CAMERA_ROLE_IDS;
    private static final int DARKENING = 0;
    private static final int DO_NOT_REDUCE_BRIGHTNESS_INTERVAL = 60000;
    private static final boolean IS_LIGHT_FOV_OPTIMIZATION_POLICY_ENABLE;
    private static final int MSG_RESET_FAST_RATE = 2;
    private static final int MSG_UPDATE_OUT_PACKET_TIME = 1;
    private static final long NON_UI_FAST_UPDATE_BRIGHTNESS_TIME = 2000;
    private static final float NON_UI_NOT_IN_POCKET = 0.0f;
    private static final int PARALLEL_VIRTUAL_ROLE_ID = 102;
    private static final int SENSOR_TYPE_ASSIST = 33171055;
    private static final int SENSOR_TYPE_LIGHT_FOV = 33172111;
    private static final int SENSOR_TYPE_NON_UI = 33171027;
    private static final String TAG = "AutomaticBrightnessControllerImpl";
    private static final int TORCH_CLOSE_DELAY = 1800;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final int VIRTUAL_BACK_ROLE_ID = 100;
    private static final int VIRTUAL_FRONT_ROLE_ID = 101;
    private float mAllowFastRateRatio;
    private int mAllowFastRateTime;
    private float mAllowFastRateValue;
    private float mAmbientLux;
    private boolean mAppliedDimming;
    private boolean mApplyingFastRate;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private boolean mAutomaticBrightnessEnable;
    private BrightnessMappingStrategy mBrightnessMapper;
    private CameraManager mCameraManager;
    private Context mContext;
    private DaemonSensorPolicy mDaemonSensorPolicy;
    private boolean mDaemonSensorPolicyEnabled;
    private boolean mDebug;
    private boolean mDisableResetShortTermModel;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private DualSensorPolicy mDualSensorPolicy;
    private long mEnterGameTime;
    private String[] mExitFacingCameraIds;
    private boolean mFrontFlashAvailable;
    private Handler mHandler;
    private HysteresisLevelsImpl mHysteresisLevelsImpl;
    private boolean mIsAnimatePolicyDisable;
    private boolean mIsGameSceneEnable;
    private boolean mIsTorchOpen;
    private float mLastBrightness;
    private Sensor mLightFovSensor;
    private long mLightSensorEnableTime;
    private float mNeedUseFastRateBrightness;
    private Sensor mNonUiSensor;
    private float mNonUiSensorData;
    private boolean mNonUiSensorEnabled;
    private boolean mPendingUseFastRateDueToFirstAutoBrightness;
    private boolean mProximityPositive;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private float mProximityThreshold;
    private int mResetFastRateTime;
    private SceneDetector mSceneDetector;
    private SensorManager mSensorManager;
    private float mSkipTransitionLuxValue;
    private boolean mSlowChange;
    private int mState;
    private long mTorchCloseTime;
    private TouchCoverProtectionHelper mTouchAreaHelper;
    private boolean mUseAonFlareEnabled;
    private boolean mUseAssistSensorEnabled;
    private boolean mUseFastRateForVirtualSensor;
    private boolean mUseNonUiEnabled;
    private boolean mUseProximityEnabled;
    private float mStartBrightness = -1.0f;
    private float mStartSdrBrightness = -1.0f;
    private long mNotInPocketTime = -1;
    private int mAmbientLuxDirection = 0;
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.1
        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeUnavailable(String cameraId) {
            if (AutomaticBrightnessControllerImpl.this.mExitFacingCameraIds != null && cameraId != null) {
                for (String id : AutomaticBrightnessControllerImpl.this.mExitFacingCameraIds) {
                    if (cameraId.equals(id)) {
                        return;
                    }
                }
            }
            AutomaticBrightnessControllerImpl.this.mIsTorchOpen = true;
        }

        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            try {
                CameraCharacteristics cameraCharacteristics = AutomaticBrightnessControllerImpl.this.mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = (Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing.intValue() == 0) {
                    AutomaticBrightnessControllerImpl.this.mFrontFlashAvailable = ((Boolean) cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)).booleanValue();
                }
                if (AutomaticBrightnessControllerImpl.this.mFrontFlashAvailable && AutomaticBrightnessControllerImpl.this.mExitFacingCameraIds != null && cameraId != null) {
                    for (String id : AutomaticBrightnessControllerImpl.this.mExitFacingCameraIds) {
                        if (cameraId.equals(id)) {
                            return;
                        }
                    }
                }
                Integer[] roleIds = null;
                for (String cameraRoleId : AutomaticBrightnessControllerImpl.CAMERA_ROLE_IDS) {
                    roleIds = getRoleIds(cameraCharacteristics, cameraRoleId);
                    if (roleIds != null) {
                        break;
                    }
                }
                if (roleIds != null) {
                    List<Integer> list = Arrays.asList(roleIds);
                    if (!list.contains(100) && !list.contains(101)) {
                        if (list.contains(102)) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            } catch (CameraAccessException e) {
                Slog.e(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged: can't get characteristics for camera " + cameraId, e);
            } catch (IllegalArgumentException e2) {
                Slog.e(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged: can't get camera characteristics key", e2);
            }
            if (enabled) {
                AutomaticBrightnessControllerImpl.this.mIsTorchOpen = true;
            } else {
                if (AutomaticBrightnessControllerImpl.this.mIsTorchOpen) {
                    AutomaticBrightnessControllerImpl.this.mTorchCloseTime = System.currentTimeMillis();
                }
                AutomaticBrightnessControllerImpl.this.mIsTorchOpen = false;
            }
            Slog.i(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged, mIsTorchOpen=" + AutomaticBrightnessControllerImpl.this.mIsTorchOpen);
        }

        private Integer[] getRoleIds(CameraCharacteristics cameraCharacteristics, String cameraRoleId) {
            try {
                return (Integer[]) cameraCharacteristics.get(new CameraCharacteristics.Key(cameraRoleId, Integer[].class));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    };
    private SensorEventListener mSensorListener = new SensorEventListener() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case 8:
                    AutomaticBrightnessControllerImpl.this.onProximitySensorChanged(event);
                    return;
                case AutomaticBrightnessControllerImpl.SENSOR_TYPE_NON_UI /* 33171027 */:
                    AutomaticBrightnessControllerImpl.this.onNonUiSensorChanged(event);
                    return;
                default:
                    return;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /* loaded from: classes.dex */
    public interface CloudControllerListener {
        boolean isAutoBrightnessStatisticsEventEnable();
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AutomaticBrightnessControllerImpl> {

        /* compiled from: AutomaticBrightnessControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AutomaticBrightnessControllerImpl INSTANCE = new AutomaticBrightnessControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AutomaticBrightnessControllerImpl m985provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AutomaticBrightnessControllerImpl m984provideNewInstance() {
            return new AutomaticBrightnessControllerImpl();
        }
    }

    static {
        IS_LIGHT_FOV_OPTIMIZATION_POLICY_ENABLE = SystemProperties.getInt("ro.miui.support.light.fov", 0) == 1;
        CAMERA_ROLE_IDS = new String[]{"com.xiaomi.cameraid.role.cameraIds", "com.xiaomi.cameraid.role.cameraId"};
    }

    public void initialize(SensorManager sensorManager, Context context, Looper looper, Sensor lightSensor, int lightSensorWarmUpTime, int lightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, int ambientLightHorizonLong, int ambientLightHorizonShort, HysteresisLevelsStub hysteresisLevelsImpl, AutomaticBrightnessControllerStub.DualSensorPolicyListener listener, AutomaticBrightnessController controller) {
        this.mContext = context;
        this.mSensorManager = sensorManager;
        this.mAutomaticBrightnessController = controller;
        loadConfiguration();
        this.mHandler = new AutomaticBrightnessControllerImplHandler(looper);
        this.mDaemonSensorPolicy = new DaemonSensorPolicy(this.mContext, sensorManager, looper, this, lightSensor);
        this.mTouchAreaHelper = new TouchCoverProtectionHelper(this.mContext, looper);
        this.mDualSensorPolicy = new DualSensorPolicy(looper, sensorManager, lightSensorWarmUpTime, lightSensorRate, brighteningLightDebounceConfig, darkeningLightDebounceConfig, ambientLightHorizonLong, ambientLightHorizonShort, hysteresisLevelsImpl, listener, this);
        if (this.mUseAonFlareEnabled) {
            SceneDetector sceneDetector = new SceneDetector(listener, this, this.mHandler, context);
            this.mSceneDetector = sceneDetector;
            this.mDualSensorPolicy.setSceneDetector(sceneDetector);
        }
        this.mLightFovSensor = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_LIGHT_FOV);
        this.mHysteresisLevelsImpl = (HysteresisLevelsImpl) hysteresisLevelsImpl;
        if (this.mUseProximityEnabled) {
            Sensor defaultSensor = this.mSensorManager.getDefaultSensor(8);
            this.mProximitySensor = defaultSensor;
            if (defaultSensor != null) {
                this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), TYPICAL_PROXIMITY_THRESHOLD);
            }
        }
        if (this.mUseNonUiEnabled) {
            Sensor defaultSensor2 = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_NON_UI);
            this.mNonUiSensor = defaultSensor2;
            if (defaultSensor2 == null) {
                this.mNonUiSensor = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_NON_UI, true);
            }
        }
        CameraManager cameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mCameraManager = cameraManager;
        cameraManager.registerTorchCallback(this.mTorchCallback, this.mHandler);
    }

    private void loadConfiguration() {
        Resources resources = this.mContext.getResources();
        this.mUseProximityEnabled = resources.getBoolean(285540371);
        this.mAllowFastRateTime = resources.getInteger(285933576);
        this.mResetFastRateTime = resources.getInteger(285933600);
        this.mAllowFastRateRatio = resources.getFloat(285671454);
        this.mAllowFastRateValue = resources.getFloat(285671455);
        this.mUseNonUiEnabled = resources.getBoolean(285540370);
        this.mUseAonFlareEnabled = resources.getBoolean(285540368);
        this.mUseAssistSensorEnabled = resources.getBoolean(285540369);
        this.mSkipTransitionLuxValue = resources.getFloat(285671476);
        this.mDaemonSensorPolicyEnabled = FeatureParser.getBoolean("use_daemon_sensor_policy", true);
        this.mUseFastRateForVirtualSensor = resources.getBoolean(285540489);
        this.mExitFacingCameraIds = resources.getStringArray(285409342);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setUpAutoBrightness(DisplayPowerControllerImpl dpcImpl, BrightnessMappingStrategy brightnessMapper, DisplayDeviceConfig deviceConfig, LogicalDisplay logicalDisplay) {
        this.mDisplayPowerControllerImpl = dpcImpl;
        this.mBrightnessMapper = brightnessMapper;
        setUpDisplayDeviceConfig(deviceConfig);
        setUpLogicalDisplay(logicalDisplay);
    }

    private void setUpDisplayDeviceConfig(DisplayDeviceConfig deviceConfig) {
        this.mDisplayDeviceConfig = deviceConfig;
    }

    private void setUpLogicalDisplay(LogicalDisplay logicalDisplay) {
        TouchCoverProtectionHelper touchCoverProtectionHelper = this.mTouchAreaHelper;
        if (touchCoverProtectionHelper != null) {
            touchCoverProtectionHelper.setUpLogicalDisplay(logicalDisplay);
        }
    }

    public void configure(int state, float screenAutoBrightness, int displayPolicy) {
        boolean enable = state == 1 && displayPolicy != 1;
        setSensorEnabled(enable);
        if (this.mDaemonSensorPolicyEnabled) {
            this.mDaemonSensorPolicy.notifyRegisterDaemonLightSensor(state, displayPolicy);
        }
        if (this.mUseAonFlareEnabled) {
            this.mSceneDetector.configure(enable);
        }
        this.mTouchAreaHelper.configure(enable);
        setRotationListenerEnable(enable);
        if (!enable && this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = false;
            this.mApplyingFastRate = false;
            this.mPendingUseFastRateDueToFirstAutoBrightness = false;
            this.mHandler.removeMessages(2);
            updateCbmState(false);
        } else if (enable && !this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = true;
            int oldState = this.mState;
            if (oldState == 2 || this.mUseFastRateForVirtualSensor) {
                this.mPendingUseFastRateDueToFirstAutoBrightness = true;
            }
        }
        this.mState = state;
    }

    private void setSensorEnabled(boolean enable) {
        if (this.mUseProximityEnabled) {
            setProximitySensorEnabled(enable);
        }
        if (this.mUseAssistSensorEnabled) {
            this.mDualSensorPolicy.setSensorEnabled(enable);
        }
        if (this.mUseNonUiEnabled) {
            setNonUiSensorEnabled(enable);
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable && !this.mProximitySensorEnabled) {
            Slog.i(TAG, "setProximitySensorEnabled enable");
            this.mProximitySensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, this.mProximitySensor, 3);
        } else if (!enable && this.mProximitySensorEnabled) {
            Slog.i(TAG, "setProximitySensorEnabled disable");
            this.mProximitySensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mProximitySensor);
        }
    }

    private void setNonUiSensorEnabled(boolean enable) {
        Sensor sensor;
        if (enable && !this.mNonUiSensorEnabled && (sensor = this.mNonUiSensor) != null) {
            this.mNonUiSensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, sensor, 3);
            Slog.i(TAG, "setNonUiSensorEnabled enable");
        } else if (!enable && this.mNonUiSensorEnabled) {
            this.mNonUiSensorEnabled = false;
            this.mNotInPocketTime = -1L;
            this.mNonUiSensorData = 0.0f;
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mNonUiSensor);
            Slog.i(TAG, "setNonUiSensorEnabled disable");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProximitySensorChanged(SensorEvent event) {
        if (this.mProximitySensorEnabled) {
            boolean z = false;
            float distance = event.values[0];
            if (distance >= 0.0f && distance < this.mProximityThreshold) {
                z = true;
            }
            this.mProximityPositive = z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNonUiSensorChanged(SensorEvent event) {
        if (this.mNonUiSensorEnabled && event.values[0] != this.mNonUiSensorData) {
            this.mNonUiSensorData = event.values[0];
            if (event.values[0] == 0.0f) {
                this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    public boolean dropAmbientLuxIfNeeded() {
        if (this.mTouchAreaHelper.isTouchCoverProtectionActive()) {
            Slog.d(TAG, "drop the ambient lux due to touch events.");
            return true;
        }
        if (this.mUseProximityEnabled && this.mProximityPositive) {
            Slog.d(TAG, "drop the ambient lux due to proximity events.");
            return true;
        }
        return false;
    }

    public boolean dropDecreaseLuxIfNeeded() {
        long now = SystemClock.uptimeMillis();
        if (!this.mUseProximityEnabled && this.mIsGameSceneEnable) {
            if (now - this.mEnterGameTime <= AccessControlImpl.LOCK_TIME_OUT || this.mTouchAreaHelper.isGameSceneWithinTouchTime()) {
                Slog.d(TAG, "drop the ambient lux due to game scene enable.");
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateGameSceneEnable(boolean enable) {
        this.mIsGameSceneEnable = enable;
        this.mEnterGameTime = enable ? SystemClock.uptimeMillis() : this.mEnterGameTime;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean checkAssistSensorValid() {
        if (this.mUseAssistSensorEnabled && !this.mIsTorchOpen && System.currentTimeMillis() - this.mTorchCloseTime > 1800) {
            return true;
        }
        if (this.mDebug) {
            Slog.d(TAG, "drop assist light data due to within 1s of turning off the torch.");
            return false;
        }
        return false;
    }

    public void updateFastRateStatus(float brightness) {
        this.mLastBrightness = brightness;
        if (this.mPendingUseFastRateDueToFirstAutoBrightness) {
            this.mApplyingFastRate = true;
            this.mNeedUseFastRateBrightness = brightness;
            this.mPendingUseFastRateDueToFirstAutoBrightness = false;
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessageDelayed(2, this.mResetFastRateTime);
            Slog.i(TAG, "Use fast rate due to first auto brightness.");
        }
    }

    public boolean shouldSkipBrighteningTransition(long sensorEnableTime, float currentLux, float ambientLux, float brighteningThreshold) {
        this.mLightSensorEnableTime = sensorEnableTime;
        if (!checkFastRateStatus() || currentLux < brighteningThreshold || currentLux < this.mSkipTransitionLuxValue + ambientLux || currentLux < (this.mAllowFastRateRatio * ambientLux) + ambientLux) {
            return false;
        }
        if (supportDualSensorPolicy()) {
            if (!this.mDualSensorPolicy.updateBrightnessUsingMainLightSensor() && currentLux < this.mDualSensorPolicy.getAssistFastAmbientLux()) {
                return false;
            }
            this.mDualSensorPolicy.updateMainLuxStatus(currentLux);
        }
        Slog.i(TAG, "Skip brightening transition, currentLux:" + currentLux + ", ambientLux:" + ambientLux);
        if (this.mUseAonFlareEnabled) {
            this.mSceneDetector.updateAmbientLux(HANDLE_MAIN_LUX_EVENT, currentLux, false);
            return false;
        }
        return true;
    }

    protected boolean checkFastRateStatus() {
        long currentTime = SystemClock.uptimeMillis();
        return currentTime <= this.mLightSensorEnableTime + ((long) this.mAllowFastRateTime) || (this.mUseNonUiEnabled && currentTime <= this.mNotInPocketTime + NON_UI_FAST_UPDATE_BRIGHTNESS_TIME);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean shouldUseFastRate(float currBrightness, float tgtBrightness) {
        float nit = convertToNit(this.mLastBrightness);
        float currentNit = convertToNit(currBrightness);
        if (!this.mApplyingFastRate && checkFastRateStatus() && nit >= (this.mAllowFastRateRatio * currentNit) + currentNit && nit >= this.mAllowFastRateValue + currentNit) {
            this.mApplyingFastRate = true;
            this.mNeedUseFastRateBrightness = this.mLastBrightness;
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessageDelayed(2, this.mResetFastRateTime);
            Slog.i(TAG, "Use fast rate due to large change in brightness, mLastBrightness:" + this.mLastBrightness + ", currBrightness:" + currBrightness);
        }
        if (this.mApplyingFastRate) {
            float f = this.mLastBrightness;
            float f2 = this.mNeedUseFastRateBrightness;
            if (f < f2 || (f > f2 && currBrightness >= f2)) {
                Slog.i(TAG, "shouldUseFastRate: mLastBrightness: " + this.mLastBrightness + ", tgtBrightness: " + tgtBrightness + ", currBrightness: " + currBrightness + ", mNeedUseFastRateBrightness: " + this.mNeedUseFastRateBrightness);
                this.mApplyingFastRate = false;
                this.mNeedUseFastRateBrightness = 0.0f;
            }
        }
        return this.mApplyingFastRate;
    }

    public void updateSlowChangeStatus(boolean slowChange, boolean appliedDimming, boolean appliedLowPower, float startBrightness, float startSdrBrightness) {
        this.mSlowChange = (!appliedDimming) & slowChange;
        this.mAppliedDimming = appliedDimming;
        this.mStartBrightness = startBrightness;
        this.mStartSdrBrightness = startSdrBrightness;
        if (this.mDebug) {
            Slog.d(TAG, "updateSlowChangeStatus: mSlowChange: " + this.mSlowChange + ", appliedDimming: " + this.mAppliedDimming + ", appliedLowPower: " + appliedLowPower + ", startBrightness: " + this.mStartBrightness + ", mStartSdrBrightness: " + this.mStartSdrBrightness);
        }
    }

    private float convertToNit(float brightness) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToNits(brightness);
        }
        return Float.NaN;
    }

    private float convertToBrightness(float nit) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToBrightness(nit);
        }
        return Float.NaN;
    }

    public void dump(PrintWriter pw) {
        pw.println("  mUseProximityEnabled=" + this.mUseProximityEnabled);
        pw.println("  mLightFovSensor=" + this.mLightFovSensor);
        pw.println("  mUseNonUiEnabled=" + this.mUseNonUiEnabled);
        pw.println("  mUseAonFlareEnabled=" + this.mUseAonFlareEnabled);
        pw.println("  mIsGameSceneEnable=" + this.mIsGameSceneEnable);
        this.mTouchAreaHelper.dump(pw);
        this.mDaemonSensorPolicy.dump(pw);
        if (supportDualSensorPolicy()) {
            this.mDualSensorPolicy.dump(pw);
        }
        if (this.mUseAonFlareEnabled) {
            this.mSceneDetector.dump(pw);
        }
        this.mHysteresisLevelsImpl.dump(pw);
        this.mDebug = DisplayDebugConfig.DEBUG_ABC;
    }

    public SparseArray<Float> fillInLuxFromDaemonSensor() {
        SparseArray<Float> daemonSensorArray = new SparseArray<Float>() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.3
            {
                int i = AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT;
                Float valueOf = Float.valueOf(Float.NaN);
                put(i, valueOf);
                put(AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT, valueOf);
            }
        };
        if (!this.mDaemonSensorPolicyEnabled) {
            return daemonSensorArray;
        }
        float mainLux = this.mDaemonSensorPolicy.getMainLightSensorLux();
        float assistLux = this.mDaemonSensorPolicy.getDaemonSensorValue(SENSOR_TYPE_ASSIST);
        if (this.mUseAssistSensorEnabled && checkAssistSensorValid() && assistLux > mainLux) {
            daemonSensorArray.put(HANDLE_ASSIST_LUX_EVENT, Float.valueOf(assistLux));
        } else {
            daemonSensorArray.put(HANDLE_MAIN_LUX_EVENT, Float.valueOf(mainLux));
        }
        if (this.mDebug) {
            Slog.d(TAG, "fillInLuxFromDaemonSensor: mainLux: " + mainLux + ", assistLux: " + assistLux);
        }
        return daemonSensorArray;
    }

    public void stop() {
        setSensorEnabled(false);
        this.mDaemonSensorPolicy.stop();
        CameraManager cameraManager = this.mCameraManager;
        if (cameraManager != null) {
            cameraManager.unregisterTorchCallback(this.mTorchCallback);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AutomaticBrightnessControllerImpl.this.lambda$stop$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$stop$0() {
        this.mTouchAreaHelper.stop();
        setRotationListenerEnable(false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean getIsTorchOpen() {
        return this.mIsTorchOpen;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isAonFlareEnabled() {
        return this.mUseAonFlareEnabled;
    }

    public void showTouchCoverProtectionRect(boolean isShow) {
        TouchCoverProtectionHelper touchCoverProtectionHelper = this.mTouchAreaHelper;
        if (touchCoverProtectionHelper != null) {
            touchCoverProtectionHelper.showTouchCoverProtectionRect(isShow);
        }
    }

    public boolean supportDualSensorPolicy() {
        return this.mUseAssistSensorEnabled && this.mDualSensorPolicy.getAssistLightSensor() != null;
    }

    public float getAmbientLux(int event, float preLux, float updateLux, boolean needUpdateLux) {
        if (needUpdateLux) {
            this.mAmbientLux = updateLux;
        }
        return this.mDualSensorPolicy.getAmbientLux(preLux, updateLux, needUpdateLux);
    }

    public boolean updateMainLightSensorAmbientThreshold(int event) {
        return this.mDualSensorPolicy.updateMainLightSensorAmbientThreshold(event);
    }

    public boolean updateBrightnessUsingMainLightSensor() {
        return this.mDualSensorPolicy.updateBrightnessUsingMainLightSensor();
    }

    public boolean updateDualSensorPolicy(long time, int event) {
        return this.mDualSensorPolicy.updateDualSensorPolicy(time, event);
    }

    public float getCurrentAmbientLux() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            return automaticBrightnessController.getAmbientLux();
        }
        return this.mAmbientLux;
    }

    public float getMainAmbientLux() {
        return this.mDualSensorPolicy.getMainAmbientLux();
    }

    /* loaded from: classes.dex */
    private final class AutomaticBrightnessControllerImplHandler extends Handler {
        public AutomaticBrightnessControllerImplHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessControllerImpl.this.mNotInPocketTime = SystemClock.uptimeMillis();
                    Slog.i(AutomaticBrightnessControllerImpl.TAG, "take phone out of pocket at the current time!");
                    return;
                case 2:
                    if (AutomaticBrightnessControllerImpl.this.mApplyingFastRate) {
                        AutomaticBrightnessControllerImpl.this.mApplyingFastRate = false;
                        AutomaticBrightnessControllerImpl.this.mNeedUseFastRateBrightness = 0.0f;
                        Slog.i(AutomaticBrightnessControllerImpl.TAG, "Reset apply fast rate.");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void notifyUnregisterDaemonSensor() {
        this.mDaemonSensorPolicy.setDaemonLightSensorsEnabled(false);
    }

    public void setAmbientLuxWhenInvalid(int event, float lux) {
        this.mDualSensorPolicy.setAmbientLuxWhenInvalid(event, lux);
    }

    public Sensor switchLightSensor(Sensor sensor) {
        Sensor sensor2;
        if (IS_LIGHT_FOV_OPTIMIZATION_POLICY_ENABLE && (sensor2 = this.mLightFovSensor) != null) {
            return sensor2;
        }
        return sensor;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyDisableResetShortTermModel(boolean enable) {
        this.mDisableResetShortTermModel = enable;
    }

    public boolean isDisableResetShortTermModel() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.resetShortTermModel(false);
        }
        return this.mDisableResetShortTermModel;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getMainFastAmbientLux() {
        return this.mDualSensorPolicy.getMainFastAmbientLux();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getAssistFastAmbientLux() {
        return this.mDualSensorPolicy.getAssistFastAmbientLux();
    }

    private void setRotationListenerEnable(boolean enable) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.registerRotationWatcher(enable);
        }
    }

    public void update() {
        this.mAutomaticBrightnessController.update();
    }

    public float getCustomBrightness(float lux, String packageName, int category, float oldAutoBrightness, float newAutoBrightness, boolean isManuallySet) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getCustomBrightness(lux, packageName, category, oldAutoBrightness, newAutoBrightness, isManuallySet);
        }
        return newAutoBrightness;
    }

    public void setScreenBrightnessByUser(float lux, float brightness, String packageName) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.setScreenBrightnessByUser(lux, brightness, packageName);
        }
    }

    public void updateAmbientLuxDirection(boolean needUpdateBrightness, float currentAmbientLux, float preAmbientLux) {
        if (needUpdateBrightness) {
            if (currentAmbientLux > preAmbientLux) {
                this.mAmbientLuxDirection = 1;
            }
            if (currentAmbientLux < preAmbientLux) {
                this.mAmbientLuxDirection = 0;
            }
        }
    }

    public boolean isBrighteningDirection() {
        return this.mAmbientLuxDirection == 1;
    }

    public boolean isAnimating() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        return (displayPowerControllerImpl == null || this.mIsAnimatePolicyDisable || !displayPowerControllerImpl.isAnimating() || this.mApplyingFastRate) ? false : true;
    }

    public void setAnimationPolicyDisable(boolean isDisable) {
        this.mIsAnimatePolicyDisable = isDisable;
    }

    public HysteresisLevelsStub getHysteresisLevelsImpl() {
        return this.mHysteresisLevelsImpl;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyUpdateForegroundApp() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.updateForegroundAppWindowChanged();
        }
    }

    public void updateCustomSceneState(String packageName) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.updateCustomSceneState(packageName);
        }
    }

    private void updateCbmState(boolean autoBrightnessEnabled) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.updateCbmState(autoBrightnessEnabled);
        }
    }

    public void notifyAonFlareEvents(int type, float preLux) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.notifyAonFlareEvents(type, preLux);
        }
    }

    public void notifyUpdateBrightness() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.notifyUpdateBrightness();
        }
    }
}
