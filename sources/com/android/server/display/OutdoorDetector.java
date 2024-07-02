package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.BackgroundThread;

/* loaded from: classes.dex */
public class OutdoorDetector {
    private static final int DEBOUNCE_ENTER_OUTDOOR_DURATION = 3000;
    private static final int DEBOUNCE_EXIT_OUTDOOR_DURATION = 3000;
    private static boolean DEBUG = ThermalBrightnessController.DEBUG;
    private static final int MSG_UPDATE_OUTDOOR_STATE = 1;
    private static final int OUTDOOR_AMBIENT_LIGHT_HORIZON = 3000;
    private static final String TAG = "ThermalBrightnessController.OutdoorDetector";
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private float mCurrentAmbientLux;
    private Handler mHandler;
    private boolean mIsInOutDoor;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private Sensor mLightSensor;
    private boolean mSensorEnabled;
    private SensorEventListener mSensorListener = new SensorEventListener() { // from class: com.android.server.display.OutdoorDetector.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            long time = SystemClock.uptimeMillis();
            float lux = event.values[0];
            OutdoorDetector.this.handleLightSensorEvent(time, lux);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorManager mSensorManager;
    private ThermalBrightnessController mThermalBrightnessController;
    private final float mThresholdLuxEnterOutdoor;

    public OutdoorDetector(Context context, ThermalBrightnessController thermalController, float threshold) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
        this.mSensorManager = sensorManager;
        this.mLightSensor = sensorManager.getDefaultSensor(5, false);
        this.mHandler = new MyHandler(BackgroundThread.getHandler().getLooper());
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer(250L, 3000);
        this.mThermalBrightnessController = thermalController;
        this.mThresholdLuxEnterOutdoor = threshold;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean detect(boolean enable) {
        if (enable && !this.mSensorEnabled) {
            this.mSensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, this.mLightSensor, 0, this.mHandler);
        } else if (!enable && this.mSensorEnabled) {
            this.mSensorEnabled = false;
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mSensorListener);
            setOutdoorActive(false);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLightSensorEvent(long time, float lux) {
        if (DEBUG) {
            Slog.d(TAG, "handleLightSensorEvent: lux = " + lux);
        }
        this.mHandler.removeMessages(1);
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mAmbientLightRingBuffer.prune(time - 3000);
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - 3000);
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        this.mCurrentAmbientLux = this.mAmbientLightRingBuffer.calculateAmbientLux(time, 3000L);
        long nextEnterOutdoorModeTime = nextEnterOutdoorModeTransition(time);
        long nextExitOutdoorModeTime = nextExitOutdoorModeTransition(time);
        float f = this.mCurrentAmbientLux;
        float f2 = this.mThresholdLuxEnterOutdoor;
        if (f >= f2 && nextEnterOutdoorModeTime <= time && !this.mIsInOutDoor) {
            setOutdoorActive(true);
        } else if (f < f2 && nextExitOutdoorModeTime <= time) {
            setOutdoorActive(false);
        }
        long nextTransitionTime = Math.min(nextEnterOutdoorModeTime, nextExitOutdoorModeTime);
        long nextTransitionTime2 = nextTransitionTime > time ? nextTransitionTime : 1000 + time;
        if (DEBUG) {
            Slog.d(TAG, "updateAmbientLux: Scheduling lux update for " + TimeUtils.formatUptime(nextTransitionTime2) + ", mAmbientLightRingBuffer: ");
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime2);
    }

    private void setOutdoorActive(boolean active) {
        if (active != this.mIsInOutDoor) {
            Slog.d(TAG, "setOutDoorActive: active: " + active);
            this.mIsInOutDoor = active;
            this.mThermalBrightnessController.outDoorStateChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isOutdoorState() {
        return this.mIsInOutDoor;
    }

    private long nextEnterOutdoorModeTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > this.mThresholdLuxEnterOutdoor; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return 3000 + earliestValidTime;
    }

    private long nextExitOutdoorModeTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < this.mThresholdLuxEnterOutdoor; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return 3000 + earliestValidTime;
    }

    /* loaded from: classes.dex */
    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    OutdoorDetector.this.updateAmbientLux();
                    return;
                default:
                    return;
            }
        }
    }
}
