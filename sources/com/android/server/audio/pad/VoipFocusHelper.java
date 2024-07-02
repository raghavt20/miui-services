package com.android.server.audio.pad;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.ICameraFaceListener;
import android.hardware.ICameraService;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.xiaomi.aon.IMiAON;
import com.xiaomi.aon.IMiAONListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public class VoipFocusHelper {
    private static final ComponentName AON_SERVICE_COMPONENT = new ComponentName("com.xiaomi.aon", "com.xiaomi.aon.AONService");
    private static final int[] AON_VIEW_REGION = {0, 0, 320, 240};
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    private static final int FACE_DETECT_IME_OUT_MS = 3000;
    private static final int FACE_MONITOR_AON = 1;
    private static final int FACE_MONITOR_CAMERA = 2;
    private static final int FACE_MONITOR_NONE = 0;
    private static final int MSG_FACE_DETECT_TIME_OUT = 3;
    private static final int MSG_FACE_INFO_UPDATE = 2;
    private static final int MSG_FACE_SRC_UPDATE = 1;
    private static final String TAG = "PadAdapter.VoipFocusHelper";
    private static final String VOIP_FOCUS_MODE_OFF = "voip_focus_mode=off";
    private static final String VOIP_FOCUS_MODE_ON = "voip_focus_mode=on";
    private IMiAON mAonService;
    private CameraManager mCameraManager;
    private ICameraService mCameraService;
    private Context mContext;
    private boolean mFaceDetected;
    private H mHandler;
    private boolean mOrientationListening;
    private SensorManager mSensorManager;
    private boolean mSettingOn;
    private volatile boolean mVoipFocusMode;
    private HandlerThread mWorkerThread;
    private int mAudioMode = 0;
    private volatile int mOrientation = -1;
    private HashSet<String> mOpenedCameras = new HashSet<>();
    private int mFaceMonitorState = 0;
    private Object mHandlerLock = new Object();
    private Object mCameraStateLock = new Object();
    private Object mAonServiceLock = new Object();
    private SensorEventListener mOrientationListener = new SensorEventListener() { // from class: com.android.server.audio.pad.VoipFocusHelper.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            VoipFocusHelper.this.onOrientationUpdate((int) event.values[0]);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(VoipFocusHelper.TAG, "onAccuracyChanged() sensor : " + sensor + ", accuracy : " + accuracy);
        }
    };
    private CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.audio.pad.VoipFocusHelper.2
        public void onCameraOpened(String cameraId, String packageId) {
            Log.d(VoipFocusHelper.TAG, "onCameraOpened() cameraId : " + cameraId + ", packageId : " + packageId);
            synchronized (VoipFocusHelper.this.mCameraStateLock) {
                VoipFocusHelper.this.mOpenedCameras.add(cameraId);
            }
            VoipFocusHelper.this.updateFaceInfoSrc();
        }

        public void onCameraClosed(String cameraId) {
            Log.d(VoipFocusHelper.TAG, "onCameraClosed() cameraId : " + cameraId);
            synchronized (VoipFocusHelper.this.mCameraStateLock) {
                VoipFocusHelper.this.mOpenedCameras.remove(cameraId);
            }
            VoipFocusHelper.this.updateFaceInfoSrc();
        }
    };
    private ServiceConnection mAonConnection = new ServiceConnection() { // from class: com.android.server.audio.pad.VoipFocusHelper.3
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(VoipFocusHelper.TAG, "AonService connected !");
            synchronized (VoipFocusHelper.this.mAonServiceLock) {
                VoipFocusHelper.this.mAonService = IMiAON.Stub.asInterface(service);
                VoipFocusHelper.this.mAonServiceLock.notifyAll();
                VoipFocusHelper.this.updateFaceInfoSrc();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.d(VoipFocusHelper.TAG, "AonService disconnected !");
            synchronized (VoipFocusHelper.this.mAonServiceLock) {
                VoipFocusHelper.this.mAonService = null;
            }
        }
    };
    private IMiAONListener mAonListener = new IMiAONListener.Stub() { // from class: com.android.server.audio.pad.VoipFocusHelper.4
        public void onCallbackListener(int Type, int[] data) {
            Log.d(VoipFocusHelper.TAG, "onCallbackListener() data : " + Arrays.toString(data));
            FaceInfo faceInfo = FaceInfo.build(1, VoipFocusHelper.this.mOrientation, VoipFocusHelper.AON_VIEW_REGION, data);
            if (faceInfo != null) {
                VoipFocusHelper.this.postUpdateFaceInfo(faceInfo);
            }
        }

        public void onImageAvailiable(int frame_id) {
        }
    };
    private ICameraFaceListener mCameraFaceListener = new ICameraFaceListener.Stub() { // from class: com.android.server.audio.pad.VoipFocusHelper.5
        public void onFaceUpdate(int[] viewRegion, int[] faceRects) {
            Log.d(VoipFocusHelper.TAG, "[camera] face detected, viewRegion : " + Arrays.toString(viewRegion) + ", faceRects : " + Arrays.toString(faceRects));
            FaceInfo faceInfo = FaceInfo.build(2, VoipFocusHelper.this.mOrientation, viewRegion, faceRects);
            if (faceInfo != null) {
                VoipFocusHelper.this.postUpdateFaceInfo(faceInfo);
            }
        }
    };

    public VoipFocusHelper(Context context) {
        Log.d(TAG, "VoipFocusHelper Construct ...");
        this.mContext = context;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
    }

    public void handleAudioModeUpdate(int mode) {
        Log.d(TAG, "audio mode update : " + mode);
        this.mAudioMode = mode;
        if (mode == 0) {
            this.mSettingOn = false;
            disableOrientationListener();
            stopVoipFocusModeIfNeed("exit call mode");
        }
        if (this.mAudioMode == 3) {
            enableOrientationListener();
            startVoipFocusModeIfNeed();
        }
    }

    public void handleMeetingModeUpdate(String parameter) {
        if (parameter == null || !parameter.contains("remote_record_mode")) {
            Log.d(TAG, "invalid parameter ...");
            return;
        }
        if (parameter.contains("remote_record_mode=single_on")) {
            this.mSettingOn = true;
            startVoipFocusModeIfNeed();
        } else if (parameter.contains("remote_record_mode=multi_on") || parameter.contains("remote_record_mode=surround_on") || parameter.contains("remote_record_mode=off")) {
            this.mSettingOn = false;
            stopVoipFocusModeIfNeed("exit meeting mode");
        }
    }

    private void startVoipFocusModeIfNeed() {
        if (!this.mVoipFocusMode && this.mSettingOn && this.mAudioMode == 3) {
            Log.d(TAG, "start voip focus mode ...");
            this.mVoipFocusMode = true;
            synchronized (this.mHandlerLock) {
                HandlerThread handlerThread = new HandlerThread(TAG);
                this.mWorkerThread = handlerThread;
                handlerThread.start();
                this.mHandler = new H(this.mWorkerThread.getLooper());
            }
            bindAonService();
            enableCameraStateMonitor();
            startFaceMonitor();
        }
    }

    private void stopVoipFocusModeIfNeed(String reason) {
        if (this.mVoipFocusMode) {
            Log.d(TAG, "stop voip focus by reason : " + reason);
            this.mVoipFocusMode = false;
            this.mFaceDetected = false;
            disableCameraStateMonitor();
            stopFaceMonitor();
            unbindAonService();
            synchronized (this.mHandlerLock) {
                this.mHandler = null;
                this.mWorkerThread.quitSafely();
                this.mWorkerThread = null;
            }
        }
    }

    private void enableOrientationListener() {
        Sensor orientationSensor;
        if (!this.mOrientationListening && (orientationSensor = this.mSensorManager.getDefaultSensor(27, true)) != null) {
            Log.d(TAG, "register device_orientation sensor ...");
            this.mSensorManager.registerListener(this.mOrientationListener, orientationSensor, 2);
            this.mOrientationListening = true;
        }
    }

    private void disableOrientationListener() {
        if (this.mOrientationListening) {
            this.mSensorManager.unregisterListener(this.mOrientationListener);
            this.mOrientationListening = false;
            this.mOrientation = -1;
        }
    }

    private void enableCameraStateMonitor() {
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
    }

    private void disableCameraStateMonitor() {
        this.mCameraManager.unregisterAvailabilityCallback(this.mAvailabilityCallback);
        this.mOpenedCameras.clear();
    }

    private void bindAonService() {
        Intent intent = new Intent();
        intent.setComponent(AON_SERVICE_COMPONENT);
        try {
            if (!this.mContext.bindService(intent, this.mAonConnection, 1)) {
                Log.d(TAG, "Failed to bind to AonService !");
                return;
            }
        } catch (SecurityException e) {
            Log.d(TAG, "Forbidden to bind to AonService !");
        }
        synchronized (this.mAonServiceLock) {
            try {
                this.mAonServiceLock.wait(10000L);
            } catch (InterruptedException exception) {
                Log.d(TAG, "interrupted exception : " + exception);
            }
        }
    }

    private void unbindAonService() {
        synchronized (this.mAonServiceLock) {
            this.mAonService = null;
            this.mContext.unbindService(this.mAonConnection);
        }
    }

    private void startFaceMonitor() {
        Log.d(TAG, "start face monitor ...");
        updateFaceInfoSrc();
    }

    private void stopFaceMonitor() {
        Log.d(TAG, "stop face monitor ...");
        unregisterMiAonListener();
        unregisterCameraFaceListener();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postUpdateFaceInfo(FaceInfo info) {
        synchronized (this.mHandlerLock) {
            H h = this.mHandler;
            if (h != null) {
                h.removeMessages(2);
                this.mHandler.removeMessages(3);
                this.mHandler.obtainMessage(2, info).sendToTarget();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFaceInfoSrc() {
        synchronized (this.mHandlerLock) {
            H h = this.mHandler;
            if (h != null) {
                h.obtainMessage(1).sendToTarget();
            }
        }
    }

    private void enableAonFaceMonitor() {
        Log.d(TAG, "enableAonFaceMonitor() ...");
        unregisterCameraFaceListener();
        registerMiAonListener();
    }

    private void enableCameraFaceMonitor() {
        Log.d(TAG, "enableCameraFaceMonitor() ...");
        unregisterMiAonListener();
        registerCameraFaceListener();
    }

    private void registerMiAonListener() {
        synchronized (this.mAonServiceLock) {
            IMiAON iMiAON = this.mAonService;
            if (iMiAON != null) {
                try {
                    iMiAON.registerListener(2, 10.0f, 600000, this.mAonListener);
                } catch (RemoteException exception) {
                    Log.e(TAG, "register aon listener remote exception : " + exception);
                } catch (Exception exception2) {
                    Log.e(TAG, "register aon listener exception : " + exception2);
                }
            }
        }
    }

    private void unregisterMiAonListener() {
        synchronized (this.mAonServiceLock) {
            IMiAON iMiAON = this.mAonService;
            if (iMiAON != null) {
                try {
                    iMiAON.unregisterListener(2, this.mAonListener);
                } catch (RemoteException exception) {
                    Log.e(TAG, "unregister aon listener remote exception : " + exception);
                } catch (Exception exception2) {
                    Log.e(TAG, "unregister aon listener exception : " + exception2);
                }
            }
        }
    }

    private void registerCameraFaceListener() {
        if (this.mCameraService == null) {
            IBinder binder = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
            if (binder == null) {
                Log.d(TAG, "CAMERA_SERVICE not supported !");
                return;
            }
            ICameraService asInterface = ICameraService.Stub.asInterface(binder);
            this.mCameraService = asInterface;
            try {
                asInterface.addFaceListener(this.mCameraFaceListener);
            } catch (RemoteException exception) {
                Log.e(TAG, "registerCameraFaceListener exception : " + exception);
            }
        }
    }

    private void unregisterCameraFaceListener() {
        ICameraService iCameraService = this.mCameraService;
        if (iCameraService != null) {
            try {
                iCameraService.removeFaceListener(this.mCameraFaceListener);
            } catch (RemoteException exception) {
                Log.e(TAG, "unregisterCameraFaceListener exception : " + exception);
            }
            this.mCameraService = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFaceSrcUpdate() {
        CameraCharacteristics characteristics;
        synchronized (this.mCameraStateLock) {
            if (this.mOpenedCameras.size() == 0) {
                this.mFaceMonitorState = 1;
                enableAonFaceMonitor();
                return;
            }
            Iterator<String> iterator = this.mOpenedCameras.iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                try {
                    characteristics = this.mCameraManager.getCameraCharacteristics(id);
                } catch (CameraAccessException exception) {
                    Log.d(TAG, "camera access exception : " + exception);
                }
                if (((Integer) characteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
                    Log.d(TAG, "camera facing front ...");
                    this.mFaceMonitorState = 2;
                    enableCameraFaceMonitor();
                    return;
                }
            }
            this.mFaceMonitorState = 0;
            stopFaceMonitor();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFaceInfoUpdate(FaceInfo info) {
        Log.d(TAG, "onFaceInfoUpdate() faceInfo : " + info);
        if (info != null) {
            if (!this.mFaceDetected) {
                faceDetectedStateChanged(true);
            }
            AudioSystem.setParameters(info.mParamViewRegion);
            AudioSystem.setParameters(info.mParamFocusRegion);
            synchronized (this.mHandlerLock) {
                H h = this.mHandler;
                if (h != null) {
                    h.sendEmptyMessageDelayed(3, 3000L);
                }
            }
            return;
        }
        if (this.mFaceDetected) {
            faceDetectedStateChanged(false);
        }
    }

    private void faceDetectedStateChanged(boolean detected) {
        Log.d(TAG, "face detected : " + detected);
        this.mFaceDetected = detected;
        AudioSystem.setParameters(detected ? VOIP_FOCUS_MODE_ON : VOIP_FOCUS_MODE_OFF);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onOrientationUpdate(int orientation) {
        Log.d(TAG, "onOrientationUpdate : " + orientation);
        this.mOrientation = orientation;
        AudioSystem.setParameters("voip_tx_rotation=" + this.mOrientation);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VoipFocusHelper.this.onFaceSrcUpdate();
                    return;
                case 2:
                    VoipFocusHelper.this.onFaceInfoUpdate((FaceInfo) msg.obj);
                    return;
                case 3:
                    VoipFocusHelper.this.onFaceInfoUpdate(null);
                    return;
                default:
                    return;
            }
        }
    }
}
