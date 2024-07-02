package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class MiuiFoldPolicy {
    private static final String CLOSE_LID_DISPLAY_SETTING = "close_lid_display_setting";
    private static final Boolean DEBUG = false;
    private static final int FOLD_GESTURE_ANGLE_THRESHOLD = 82;
    private static final String MIUI_OPTIMIZATION = "miui_optimization";
    private static final int MSG_RELEASE_WINDOW_BY_SCREEN_OFF = 2;
    private static final int MSG_SCREEN_TURNING_OFF = 6;
    private static final int MSG_SCREEN_TURNING_ON = 5;
    private static final int MSG_SHOW_OR_RELEASE_SWIPE_UP_WINDOW = 1;
    private static final int MSG_UPDATE_DEVICE_STATE = 3;
    private static final int MSG_USER_SWITCH = 4;
    private static final int SETTING_EVENT_INVALID = -1;
    private static final int SETTING_EVENT_KEEP_ON = 2;
    private static final int SETTING_EVENT_SCREEN_OFF = 0;
    private static final int SETTING_EVENT_SMART = 3;
    private static final int SETTING_EVENT_SWIPE_UP = 1;
    public static final String TAG = "MiuiFoldPolicy";
    public static final int TYPE_HINGE_STATE = 33171087;
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    private AudioManager mAudioManager;
    private CameraManager mCameraManager;
    private Context mContext;
    private Handler mHandler;
    private boolean mIsCtsMode;
    private boolean mIsDeviceProvisioned;
    private Sensor mMiHingeAngleSensor;
    private boolean mMiHingeAngleSensorEnabled;
    private boolean mNeedOffDueToFoldGesture;
    private boolean mNeedReleaseByScreenTurningOn;
    private boolean mNeedReleaseSwipeUpWindow;
    private boolean mNeedShowSwipeUpWindow;
    private int mScreenStateAfterFold;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private final int[] mStrictFoldedDeviceStates;
    private SwipeUpWindow mSwipeUpWindow;
    private TelecomManager mTelecomManager;
    private final int[] mTentDeviceStates;
    private WindowManagerPolicy mWindowManagerPolicy;
    private int mPreState = -1;
    private int mState = -1;
    private final Set<Integer> mOpeningCameraID = new HashSet();
    private int mFoldGestureAngleThreshold = 82;
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.display.MiuiFoldPolicy.1
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            MiuiFoldPolicy.this.mOpeningCameraID.remove(Integer.valueOf(id));
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            MiuiFoldPolicy.this.mOpeningCameraID.add(Integer.valueOf(id));
        }
    };
    private final SensorEventListener mMiHingeAngleSensorListener = new SensorEventListener() { // from class: com.android.server.display.MiuiFoldPolicy.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            MiuiFoldPolicy.this.mNeedOffDueToFoldGesture = false;
            if (event.values[4] == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && event.values[11] < MiuiFoldPolicy.this.mFoldGestureAngleThreshold) {
                MiuiFoldPolicy.this.mNeedOffDueToFoldGesture = true;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public MiuiFoldPolicy(Context context) {
        this.mContext = context;
        this.mStrictFoldedDeviceStates = context.getResources().getIntArray(17236155);
        this.mTentDeviceStates = this.mContext.getResources().getIntArray(285409366);
    }

    public void initMiuiFoldPolicy() {
        ServiceThread handlerThread = new ServiceThread(TAG, -4, false);
        handlerThread.start();
        this.mHandler = new MiuiFoldPolicyHandler(handlerThread.getLooper());
        this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
        this.mMiHingeAngleSensor = this.mSensorManager.getDefaultSensor(TYPE_HINGE_STATE);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSwipeUpWindow = new SwipeUpWindow(this.mContext, handlerThread.getLooper());
        registerContentObserver();
        this.mContext.registerReceiver(new UserSwitchReceiver(), new IntentFilter("android.intent.action.USER_SWITCHED"));
    }

    public void dealDisplayTransition() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void setDeviceStateLocked(int state) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        Message msg = handler.obtainMessage(3);
        msg.arg1 = state;
        this.mHandler.sendMessage(msg);
        if (DEBUG.booleanValue()) {
            Slog.i(TAG, "setDeviceStateLocked: " + state);
        }
    }

    public void handleDeviceStateChanged(int state) {
        int i = this.mState;
        if (state == i) {
            Slog.i(TAG, "the new state is equal the old(" + this.mState + ") skip");
            return;
        }
        this.mPreState = i;
        this.mState = state;
        if (!isFoldedOrTent(state) || isKeepScreenOnAfterFolded() || isCtsScene() || !this.mIsDeviceProvisioned) {
            this.mSwipeUpWindow.cancelScreenOffDelay();
            this.mNeedReleaseSwipeUpWindow = true;
            this.mNeedShowSwipeUpWindow = false;
            return;
        }
        if (isFolded(this.mState) && !isTent(this.mPreState)) {
            int i2 = this.mScreenStateAfterFold;
            if (i2 == 0 && this.mPreState != -1) {
                screenTurnOff();
                return;
            }
            if (i2 == 3) {
                if (this.mNeedOffDueToFoldGesture) {
                    screenTurnOff();
                }
            } else if (i2 == 1) {
                this.mNeedShowSwipeUpWindow = true;
                this.mNeedReleaseSwipeUpWindow = false;
            }
        }
    }

    private boolean isKeepScreenOnAfterFolded() {
        switch (this.mScreenStateAfterFold) {
            case 0:
                boolean isKeepScreenOn = isHoldScreenOn();
                return isKeepScreenOn;
            case 1:
                boolean isKeepScreenOn2 = isHoldScreenOn() || this.mWindowManagerPolicy.isKeyguardShowing();
                return isKeepScreenOn2;
            case 2:
                return true;
            default:
                return false;
        }
    }

    private boolean isFolded(int state) {
        return ArrayUtils.contains(this.mStrictFoldedDeviceStates, state);
    }

    private boolean isTent(int state) {
        return ArrayUtils.contains(this.mTentDeviceStates, state);
    }

    private boolean isFoldedOrTent(int state) {
        return isFolded(state) || isTent(state);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showOrReleaseSwipeUpWindow() {
        if (DEBUG.booleanValue()) {
            Slog.i(TAG, "showOrReleaseSwipeUpWindow: fold?" + (!isFoldedOrTent(this.mState)) + ", mNeedShowSwipeUpWindow:" + this.mNeedShowSwipeUpWindow);
        }
        this.mNeedReleaseByScreenTurningOn = false;
        if (this.mNeedReleaseSwipeUpWindow || !isFoldedOrTent(this.mState)) {
            this.mNeedReleaseSwipeUpWindow = false;
            releaseSwipeUpWindow("device state");
        } else if (this.mNeedShowSwipeUpWindow) {
            this.mNeedShowSwipeUpWindow = false;
            this.mSwipeUpWindow.showSwipeUpWindow();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseSwipeUpWindow(String reason) {
        this.mSwipeUpWindow.releaseSwipeWindow(reason);
    }

    /* loaded from: classes.dex */
    private final class MiuiFoldPolicyHandler extends Handler {
        public MiuiFoldPolicyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiFoldPolicy.this.showOrReleaseSwipeUpWindow();
                    return;
                case 2:
                    MiuiFoldPolicy.this.releaseSwipeUpWindow("screen off");
                    return;
                case 3:
                    MiuiFoldPolicy.this.handleDeviceStateChanged(msg.arg1);
                    return;
                case 4:
                    MiuiFoldPolicy.this.updateSettings();
                    return;
                case 5:
                    MiuiFoldPolicy.this.handleScreenTurningOn();
                    return;
                case 6:
                    MiuiFoldPolicy.this.handleScreenTurningOff();
                    return;
                default:
                    return;
            }
        }
    }

    public void screenTurningOn() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.sendEmptyMessage(5);
    }

    public void screenTurningOff() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.sendEmptyMessage(6);
    }

    public void notifyFinishedGoingToSleep() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.sendEmptyMessage(2);
    }

    public void handleScreenTurningOn() {
        if (this.mNeedReleaseByScreenTurningOn) {
            releaseSwipeUpWindow("screen on");
            this.mNeedReleaseByScreenTurningOn = false;
        }
    }

    public void handleScreenTurningOff() {
        this.mNeedReleaseByScreenTurningOn = true;
    }

    private void screenTurnOff() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        powerManager.goToSleep(SystemClock.uptimeMillis(), 3, 0);
    }

    private boolean isHoldScreenOn() {
        String reason = "";
        boolean isHoldScreenOn = false;
        if (this.mTelecomManager.isInCall()) {
            isHoldScreenOn = true;
            reason = "in call";
        } else if (!this.mOpeningCameraID.isEmpty()) {
            isHoldScreenOn = true;
            reason = "camera using";
        } else if (this.mAudioManager.getMode() == 2 || this.mAudioManager.getMode() == 3) {
            isHoldScreenOn = true;
            reason = "audio using";
        }
        if (isHoldScreenOn) {
            Slog.i(TAG, "hold screen on reason : " + reason);
        }
        return isHoldScreenOn;
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
                case -1346041141:
                    if (lastPathSegment.equals("fold_gesture_angle_threshold")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -746666980:
                    if (lastPathSegment.equals("miui_optimization")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1230812500:
                    if (lastPathSegment.equals(MiuiFoldPolicy.CLOSE_LID_DISPLAY_SETTING)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1384083403:
                    if (lastPathSegment.equals("device_provisioned")) {
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
                    MiuiFoldPolicy.this.updateFoldGestureAngleThreshold();
                    return;
                case 1:
                    MiuiFoldPolicy.this.updateScreenStateAfterFold();
                    return;
                case 2:
                    MiuiFoldPolicy.this.updateCtsMode();
                    return;
                case 3:
                    MiuiFoldPolicy.this.updateDeviceProVisioned();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSettings() {
        updateScreenStateAfterFold();
        updateFoldGestureAngleThreshold();
        updateCtsMode();
        updateDeviceProVisioned();
    }

    private void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOSE_LID_DISPLAY_SETTING), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("fold_gesture_angle_threshold"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_optimization"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mSettingsObserver, -1);
        updateSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFoldGestureAngleThreshold() {
        this.mFoldGestureAngleThreshold = Settings.System.getIntForUser(this.mContext.getContentResolver(), "fold_gesture_angle_threshold", 82, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScreenStateAfterFold() {
        int intForUser = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOSE_LID_DISPLAY_SETTING, -1, -2);
        this.mScreenStateAfterFold = intForUser;
        if (intForUser == -1) {
            if ("cetus".equals(Build.DEVICE)) {
                this.mScreenStateAfterFold = 2;
            } else {
                this.mScreenStateAfterFold = 1;
            }
            Settings.System.putIntForUser(this.mContext.getContentResolver(), CLOSE_LID_DISPLAY_SETTING, this.mScreenStateAfterFold, -2);
        }
        registerMiHingeAngleSensorListener(this.mScreenStateAfterFold == 3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCtsMode() {
        this.mIsCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    /* JADX WARN: Code restructure failed: missing block: B:6:0x000a, code lost:
    
        if (r0 != 3) goto L9;
     */
    /* JADX WARN: Removed duplicated region for block: B:9:0x0011  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isCtsScene() {
        /*
            r3 = this;
            boolean r0 = r3.mIsCtsMode
            if (r0 == 0) goto Ld
            int r0 = r3.mScreenStateAfterFold
            r1 = 1
            if (r0 == r1) goto Lc
            r2 = 3
            if (r0 != r2) goto Ld
        Lc:
            goto Le
        Ld:
            r1 = 0
        Le:
            r0 = r1
            if (r0 == 0) goto L18
            java.lang.String r1 = "MiuiFoldPolicy"
            java.lang.String r2 = "running cts, skip fold policy."
            android.util.Slog.i(r1, r2)
        L18:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.MiuiFoldPolicy.isCtsScene():boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeviceProVisioned() {
        this.mIsDeviceProvisioned = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) != 0;
    }

    /* loaded from: classes.dex */
    private final class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiFoldPolicy.this.mHandler.sendEmptyMessage(4);
        }
    }

    private void registerMiHingeAngleSensorListener(boolean enable) {
        if (enable) {
            if (!this.mMiHingeAngleSensorEnabled) {
                this.mMiHingeAngleSensorEnabled = true;
                this.mSensorManager.registerListener(this.mMiHingeAngleSensorListener, this.mMiHingeAngleSensor, 0);
                return;
            }
            return;
        }
        if (this.mMiHingeAngleSensorEnabled) {
            this.mMiHingeAngleSensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mMiHingeAngleSensorListener);
        }
    }
}
