package com.android.server.display;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.display.AutomaticBrightnessControllerStub;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class DualSensorPolicy {
    private static final int ASSISTANT_LIGHT_SENSOR_TYPE = 33171055;
    private static boolean DEBUG = false;
    private static final int MSG_UPDATE_ASSISTANT_LIGHT_SENSOR_AMBIENT_LUX = 0;
    private static final String TAG = "DualSensorPolicy";
    private int mAmbientLightHorizonLong;
    private int mAmbientLightHorizonShort;
    private boolean mAssistAmbientLuxValid;
    private Sensor mAssistLightSensor;
    private long mAssistLightSensorBrighteningDebounce;
    private long mAssistLightSensorDarkeningDebounce;
    private boolean mAssistLightSensorEnable;
    private long mAssistLightSensorEnableTime;
    private AmbientLightRingBuffer mAssistLightSensorRingBuffer;
    private int mAssistLightSensorWarmUpTime;
    private AutomaticBrightnessControllerImpl mBrightnessControllerImpl;
    private HysteresisLevelsStub mHysteresisLevelsImpl;
    private int mLightSensorRate;
    private AutomaticBrightnessControllerStub.DualSensorPolicyListener mListener;
    private Handler mPolicyHandler;
    private SceneDetector mSceneDetector;
    private SensorManager mSensorManager;
    private int mUseLightSensorFlag;
    private int mCurrentAssistLightSensorRate = -1;
    private float mMainFastAmbientLux = -1.0f;
    private float mMainSlowAmbientLux = -1.0f;
    private float mAssistFastAmbientLux = -1.0f;
    private float mAssistSlowAmbientLux = -1.0f;
    private float mAssistBrighteningThreshold = -1.0f;
    private float mAssistDarkeningThreshold = -1.0f;
    private SensorEventListener mSensorListener = new SensorEventListener() { // from class: com.android.server.display.DualSensorPolicy.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            long time = SystemClock.uptimeMillis();
            float lux = event.values[0];
            DualSensorPolicy.this.handleAssistLightSensorEvent(time, lux);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public DualSensorPolicy(Looper looper, SensorManager sensormanager, int lightSensorWarmUpTime, int lightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, int ambientLightHorizonLong, int ambientLightHorizonShort, HysteresisLevelsStub hysteresisLevelsImpl, AutomaticBrightnessControllerStub.DualSensorPolicyListener listener, AutomaticBrightnessControllerImpl brightnessControllerImpl) {
        this.mPolicyHandler = new DualSensorPolicyHandler(looper);
        this.mSensorManager = sensormanager;
        this.mAssistLightSensor = sensormanager.getDefaultSensor(ASSISTANT_LIGHT_SENSOR_TYPE);
        this.mListener = listener;
        this.mAssistLightSensorWarmUpTime = lightSensorWarmUpTime;
        this.mLightSensorRate = lightSensorRate;
        this.mAssistLightSensorBrighteningDebounce = brighteningLightDebounceConfig;
        this.mAssistLightSensorDarkeningDebounce = darkeningLightDebounceConfig;
        this.mAmbientLightHorizonLong = ambientLightHorizonLong;
        this.mAmbientLightHorizonShort = ambientLightHorizonShort;
        this.mHysteresisLevelsImpl = hysteresisLevelsImpl;
        this.mAssistLightSensorRingBuffer = new AmbientLightRingBuffer(lightSensorRate, ambientLightHorizonLong);
        setUpDebounceConfig(this.mAssistLightSensorBrighteningDebounce, this.mAssistLightSensorDarkeningDebounce);
        this.mBrightnessControllerImpl = brightnessControllerImpl;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSceneDetector(SceneDetector sceneDetector) {
        this.mSceneDetector = sceneDetector;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAssistLightSensorEvent(long time, float lux) {
        int i;
        this.mPolicyHandler.removeMessages(0);
        if (this.mAssistLightSensorRingBuffer.size() == 0 && (i = this.mLightSensorRate) != this.mCurrentAssistLightSensorRate) {
            this.mCurrentAssistLightSensorRate = i;
            this.mSensorManager.unregisterListener(this.mSensorListener);
            this.mSensorManager.registerListener(this.mSensorListener, this.mAssistLightSensor, this.mCurrentAssistLightSensorRate * 1000, this.mPolicyHandler);
        }
        if (this.mBrightnessControllerImpl.getIsTorchOpen() || !this.mBrightnessControllerImpl.checkAssistSensorValid()) {
            Slog.d(TAG, "handleAssistantLightSensorEvent: drop assistant light sensor lux due to flash events or within one second of turning off the torch.");
            return;
        }
        this.mAssistLightSensorRingBuffer.prune(time - this.mAmbientLightHorizonLong);
        this.mAssistLightSensorRingBuffer.push(time, lux);
        updateAssistLightSensorAmbientLux(time);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setSensorEnabled(boolean enable) {
        if (enable && !this.mAssistLightSensorEnable) {
            Slog.i(TAG, "setSensorEnabled: register the assist light sensor.");
            this.mAssistLightSensorEnable = true;
            this.mAssistLightSensorEnableTime = SystemClock.uptimeMillis();
            int i = this.mLightSensorRate;
            this.mCurrentAssistLightSensorRate = i;
            this.mSensorManager.registerListener(this.mSensorListener, this.mAssistLightSensor, i * 1000, this.mPolicyHandler);
            return;
        }
        if (!enable && this.mAssistLightSensorEnable) {
            Slog.i(TAG, "setSensorEnabled: unregister the assist light sensor.");
            this.mAssistLightSensorEnable = false;
            this.mAssistAmbientLuxValid = false;
            this.mAssistLightSensorRingBuffer.clear();
            this.mCurrentAssistLightSensorRate = -1;
            this.mUseLightSensorFlag = AutomaticBrightnessControllerStub.DUAL_SENSOR_LUX_INVALID;
            this.mMainFastAmbientLux = -1.0f;
            this.mMainSlowAmbientLux = -1.0f;
            this.mAssistFastAmbientLux = -1.0f;
            this.mAssistSlowAmbientLux = -1.0f;
            this.mPolicyHandler.removeMessages(0);
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mAssistLightSensor);
        }
    }

    private void setUpDebounceConfig(long brighteningDebounce, long darkeningDebounce) {
        this.mAssistLightSensorRingBuffer.setBrighteningDebounce(brighteningDebounce);
        this.mAssistLightSensorRingBuffer.setDarkeningDebounce(darkeningDebounce);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAssistLightSensorAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAssistLightSensorRingBuffer.prune(time - this.mAmbientLightHorizonLong);
        updateAssistLightSensorAmbientLux(time);
    }

    private void updateAssistLightSensorAmbientLux(long time) {
        if (!this.mAssistAmbientLuxValid) {
            long timeWhenAssistSensorWarmedUp = this.mAssistLightSensorWarmUpTime + this.mAssistLightSensorEnableTime;
            if (time < timeWhenAssistSensorWarmedUp) {
                this.mPolicyHandler.sendEmptyMessageAtTime(0, timeWhenAssistSensorWarmedUp);
                return;
            } else {
                this.mAssistFastAmbientLux = this.mAssistLightSensorRingBuffer.calculateAmbientLux(time, this.mAmbientLightHorizonShort);
                setAmbientLuxWhenInvalid(AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT, this.mAssistFastAmbientLux);
                this.mAssistAmbientLuxValid = true;
            }
        }
        updateDualSensorPolicy(time, AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT);
    }

    /* loaded from: classes.dex */
    private final class DualSensorPolicyHandler extends Handler {
        public DualSensorPolicyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    DualSensorPolicy.this.updateAssistLightSensorAmbientLux();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getAmbientLux(float preLux, float updateLux, boolean needUpdateLux) {
        return needUpdateLux ? updateLux : preLux;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getMainAmbientLux() {
        return this.mMainFastAmbientLux;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean updateMainLightSensorAmbientThreshold(int event) {
        return event == AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT || (this.mUseLightSensorFlag == AutomaticBrightnessControllerStub.USE_MAIN_LIGHT_SENSOR && event == AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean updateBrightnessUsingMainLightSensor() {
        return this.mUseLightSensorFlag == AutomaticBrightnessControllerStub.USE_MAIN_LIGHT_SENSOR;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x033c, code lost:
    
        if (r9 <= r27) goto L80;
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x0340, code lost:
    
        if (r13 != false) goto L80;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x015b, code lost:
    
        if (r12 <= r27) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0329, code lost:
    
        if (r4 <= r27) goto L80;
     */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x0071, code lost:
    
        if (r10 <= r27) goto L13;
     */
    /* JADX WARN: Removed duplicated region for block: B:19:0x02a4  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x02b4  */
    /* JADX WARN: Removed duplicated region for block: B:26:0x02a9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean updateDualSensorPolicy(long r27, int r29) {
        /*
            Method dump skipped, instructions count: 1182
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DualSensorPolicy.updateDualSensorPolicy(long, int):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x008a, code lost:
    
        if (r3 > r5.mMainFastAmbientLux) goto L30;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setAmbientLuxWhenInvalid(int r6, float r7) {
        /*
            r5 = this;
            int r0 = com.android.server.display.AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT
            r1 = 1
            if (r6 != r0) goto L50
            int r0 = r5.mUseLightSensorFlag
            int r2 = com.android.server.display.AutomaticBrightnessControllerStub.USE_MAIN_LIGHT_SENSOR
            r3 = 0
            if (r0 == r2) goto L42
            int r0 = r5.mUseLightSensorFlag
            int r2 = com.android.server.display.AutomaticBrightnessControllerStub.DUAL_SENSOR_LUX_INVALID
            if (r0 == r2) goto L42
            int r0 = r5.mUseLightSensorFlag
            int r2 = com.android.server.display.AutomaticBrightnessControllerStub.USE_ASSIST_LIGHT_SENSOR
            if (r0 != r2) goto L1f
            float r0 = r5.mAssistFastAmbientLux
            int r0 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1))
            if (r0 < 0) goto L1f
            goto L42
        L1f:
            r5.mMainSlowAmbientLux = r7
            r5.mMainFastAmbientLux = r7
            com.android.server.display.AutomaticBrightnessControllerStub$DualSensorPolicyListener r0 = r5.mListener
            r0.updateAmbientLux(r6, r7, r3, r3)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "setAmbientLuxWhenInvalid: update brightness using assist light sensor in process, mMainFastAmbientLux: "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r7)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "DualSensorPolicy"
            android.util.Slog.d(r1, r0)
            goto Lab
        L42:
            int r0 = com.android.server.display.AutomaticBrightnessControllerStub.USE_MAIN_LIGHT_SENSOR
            r5.mUseLightSensorFlag = r0
            r5.mMainSlowAmbientLux = r7
            r5.mMainFastAmbientLux = r7
            com.android.server.display.AutomaticBrightnessControllerStub$DualSensorPolicyListener r0 = r5.mListener
            r0.updateAmbientLux(r6, r7, r1, r3)
            goto Lab
        L50:
            int r0 = com.android.server.display.AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT
            if (r6 != r0) goto Lab
            r5.mAssistFastAmbientLux = r7
            r5.mAssistSlowAmbientLux = r7
            com.android.server.display.AutomaticBrightnessControllerStub$DualSensorPolicyListener r0 = r5.mListener
            float r0 = r0.getBrighteningThreshold()
            com.android.server.display.AutomaticBrightnessControllerStub$DualSensorPolicyListener r2 = r5.mListener
            boolean r2 = r2.useDaemonSensorPolicyInProgress()
            int r3 = r5.mUseLightSensorFlag
            int r4 = com.android.server.display.AutomaticBrightnessControllerStub.DUAL_SENSOR_LUX_INVALID
            if (r3 != r4) goto L6c
            if (r2 != 0) goto L8c
        L6c:
            int r3 = r5.mUseLightSensorFlag
            int r4 = com.android.server.display.AutomaticBrightnessControllerStub.USE_MAIN_LIGHT_SENSOR
            if (r3 != r4) goto L78
            float r3 = r5.mAssistFastAmbientLux
            int r3 = (r3 > r0 ? 1 : (r3 == r0 ? 0 : -1))
            if (r3 > 0) goto L8c
        L78:
            int r3 = r5.mUseLightSensorFlag
            int r4 = com.android.server.display.AutomaticBrightnessControllerStub.USE_ASSIST_LIGHT_SENSOR
            if (r3 != r4) goto L97
            float r3 = r5.mAssistFastAmbientLux
            float r4 = r5.mAssistBrighteningThreshold
            int r4 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r4 < 0) goto L97
            float r4 = r5.mMainFastAmbientLux
            int r3 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r3 <= 0) goto L97
        L8c:
            int r3 = com.android.server.display.AutomaticBrightnessControllerStub.USE_ASSIST_LIGHT_SENSOR
            r5.mUseLightSensorFlag = r3
            com.android.server.display.AutomaticBrightnessControllerStub$DualSensorPolicyListener r3 = r5.mListener
            r4 = r2 ^ 1
            r3.updateAmbientLux(r6, r7, r1, r4)
        L97:
            com.android.server.display.HysteresisLevelsStub r1 = r5.mHysteresisLevelsImpl
            float r3 = r5.mAssistFastAmbientLux
            float r1 = r1.getBrighteningThreshold(r3)
            r5.mAssistBrighteningThreshold = r1
            com.android.server.display.HysteresisLevelsStub r1 = r5.mHysteresisLevelsImpl
            float r3 = r5.mAssistFastAmbientLux
            float r1 = r1.getDarkeningThreshold(r3)
            r5.mAssistDarkeningThreshold = r1
        Lab:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DualSensorPolicy.setAmbientLuxWhenInvalid(int, float):void");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Dual Sensor Policy State:");
        pw.println("  mUseLightSensorFlag=" + this.mUseLightSensorFlag);
        pw.println("  mAssistAmbientLuxValid=" + this.mAssistAmbientLuxValid);
        pw.println("  mAssistFastAmbientLux=" + this.mAssistFastAmbientLux);
        pw.println("  mAssistSlowAmbientLux=" + this.mAssistSlowAmbientLux);
        pw.println("  mAssistBrighteningThreshold=" + this.mAssistBrighteningThreshold);
        pw.println("  mAssistDarkeningThreshold=" + this.mAssistDarkeningThreshold);
        pw.println("  mAssistLightSensorRingBuffer=" + this.mAssistLightSensorRingBuffer);
        pw.println("Dual Sensor Policy Configuration:");
        pw.println("  mAssistLightSensorBrighteningDebounce=" + this.mAssistLightSensorBrighteningDebounce);
        pw.println("  mAssistLightSensorDarkeningDebounce=" + this.mAssistLightSensorDarkeningDebounce);
        DEBUG = DisplayDebugConfig.DEBUG_ABC;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Sensor getAssistLightSensor() {
        return this.mAssistLightSensor;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getAssistFastAmbientLux() {
        return this.mAssistFastAmbientLux;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateMainLuxStatus(float currentLux) {
        this.mUseLightSensorFlag = AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT;
        this.mMainSlowAmbientLux = currentLux;
        this.mMainFastAmbientLux = currentLux;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float getMainFastAmbientLux() {
        return this.mMainFastAmbientLux;
    }

    private boolean isSkipAmbientBrighteningThreshold(float currentAmbientLux) {
        boolean isAnimating = this.mBrightnessControllerImpl.isAnimating();
        if (!isAnimating) {
            return false;
        }
        float preAmbientLux = this.mListener.getAmbientLux();
        boolean isBrighteningDirection = this.mBrightnessControllerImpl.isBrighteningDirection();
        if (currentAmbientLux <= preAmbientLux || !isBrighteningDirection) {
            return false;
        }
        Slog.d(TAG, "isSkipAmbientBrighteningThreshold: currentAmbientLux: " + currentAmbientLux + ", preAmbientLux: " + preAmbientLux);
        return true;
    }

    private boolean isSkipAmbientDarkeningThreshold(float currentAmbientLux) {
        boolean isAnimating = this.mBrightnessControllerImpl.isAnimating();
        if (!isAnimating) {
            return false;
        }
        float preAmbientLux = this.mListener.getAmbientLux();
        boolean isBrighteningDirection = this.mBrightnessControllerImpl.isBrighteningDirection();
        if (currentAmbientLux >= preAmbientLux || isBrighteningDirection) {
            return false;
        }
        Slog.d(TAG, "isSkipAmbientDarkeningThreshold: currentAmbientLux: " + currentAmbientLux + ", preAmbientLux: " + preAmbientLux);
        return true;
    }
}
