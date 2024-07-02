package com.android.server.audio.foldable;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.List;

/* loaded from: classes.dex */
public class FoldStateHelper {
    private static final int FOLD_ANGLE_INIT = -1;
    private static final int FOLD_ANGLE_THRESHOLD = 90;
    private static final int MSG_HANDLE_LISTENER_UNREGISTER = 1;
    private static final String TAG = "FoldableAdapter.FoldStateHelper";
    private AudioManager mAudioManager;
    private boolean mCallMode;
    private Context mContext;
    private boolean mFoldedState;
    private H mHandler;
    private AngleChangedListener mListener;
    private boolean mMediaActive;
    private boolean mRegisterState;
    private SensorManager mSensorManager;
    private int mLastFoldAngle = -1;
    private SensorEventListener mHingeAngleListener = new SensorEventListener() { // from class: com.android.server.audio.foldable.FoldStateHelper.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            int hingeAngle = (int) event.values[0];
            FoldStateHelper.this.handleHingeAngleChanged(hingeAngle);
            if (FoldStateHelper.this.mListener != null) {
                FoldStateHelper.this.mListener.onAngleChanged(hingeAngle);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(FoldStateHelper.TAG, "onAccuracyChanged() sensor : " + sensor + ", accuracy : " + accuracy);
        }
    };

    /* loaded from: classes.dex */
    public interface AngleChangedListener {
        void onAngleChanged(int i);
    }

    public void setAngleChangedListener(AngleChangedListener listener) {
        this.mListener = listener;
    }

    public FoldStateHelper(Context context, Looper looper) {
        Log.d(TAG, "FoldStateHelper Construct ...");
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
    }

    public void onUpdateAudioMode(int mode) {
        this.mHandler.removeMessages(1);
        if ((mode == 2 || mode == 3) && !this.mCallMode) {
            Log.d(TAG, "enter call mode ...");
            this.mCallMode = true;
            if (!this.mRegisterState) {
                this.mRegisterState = true;
                Log.d(TAG, "onUpdateAudioMode registerHingeAngleListener");
                registerHingeAngleListener();
            }
        }
        if (mode == 0 && this.mCallMode) {
            Log.d(TAG, "exit call mode ...");
            this.mCallMode = false;
            if (this.mRegisterState && !this.mMediaActive) {
                Log.d(TAG, "onUpdateAudioMode sendMessageDelayed 10s");
                H h = this.mHandler;
                h.sendMessageDelayed(h.obtainMessage(1, true), 10000L);
            }
        }
    }

    public void onUpdateMediaState(boolean mediaActive) {
        this.mMediaActive = mediaActive;
        this.mHandler.removeMessages(1);
        if ((this.mMediaActive || this.mCallMode) && !this.mRegisterState) {
            this.mRegisterState = true;
            Log.d(TAG, "onUpdateMediaState registerHingeAngleListener");
            registerHingeAngleListener();
        }
        if (!this.mMediaActive && !this.mCallMode && this.mRegisterState) {
            Log.d(TAG, "onUpdateMediaState sendMessageDelayed 10s");
            H h = this.mHandler;
            h.sendMessageDelayed(h.obtainMessage(1, true), 10000L);
        }
    }

    public void onAudioServerDied() {
        Log.d(TAG, "onAudioServerDied ...");
        if (this.mCallMode || this.mMediaActive) {
            Log.d(TAG, "onAudioServerDied mLastFoldAngle = " + this.mLastFoldAngle);
            updateFoldedStateParam(this.mLastFoldAngle);
        }
    }

    private void registerHingeAngleListener() {
        Log.d(TAG, "registerHingeAngleListener() ...");
        List<Sensor> hingeAngleList = this.mSensorManager.getSensorList(36);
        if (hingeAngleList != null && hingeAngleList.size() > 0) {
            Sensor hingeAngleSensor = hingeAngleList.get(0);
            this.mSensorManager.registerListener(this.mHingeAngleListener, hingeAngleSensor, 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterHingeAngleListener() {
        this.mRegisterState = false;
        this.mLastFoldAngle = -1;
        Log.d(TAG, "unregisterHingeAngleListener() ...");
        this.mSensorManager.unregisterListener(this.mHingeAngleListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleHingeAngleChanged(int hingeAngle) {
        int i = this.mLastFoldAngle;
        if (i == -1 || ((i < FOLD_ANGLE_THRESHOLD && hingeAngle >= FOLD_ANGLE_THRESHOLD) || (i >= FOLD_ANGLE_THRESHOLD && hingeAngle < FOLD_ANGLE_THRESHOLD))) {
            updateFoldedStateParam(hingeAngle);
        }
        this.mLastFoldAngle = hingeAngle;
    }

    private void updateFoldedStateParam(int hingeAngle) {
        String parameter = "device_fold_status=" + (hingeAngle < FOLD_ANGLE_THRESHOLD ? "on" : "off");
        this.mAudioManager.setParameters(parameter);
        Log.d(TAG, "update audio param : " + parameter);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    FoldStateHelper.this.unregisterHingeAngleListener();
                    return;
                default:
                    return;
            }
        }
    }
}
