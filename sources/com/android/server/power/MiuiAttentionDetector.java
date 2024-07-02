package com.android.server.power;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.ServiceThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.MiuiAttentionDetector;
import com.xiaomi.aon.IMiAON;
import com.xiaomi.aon.IMiAONListener;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiAttentionDetector {
    private static final int ALWAYS_CHECK_ATTENTION_TIMEOUT = 600000;
    public static final boolean AON_SCREEN_OFF_SUPPORTED;
    public static final boolean AON_SCREEN_ON_SUPPORTED;
    public static final String AON_SERVICE_CLASS = "com.xiaomi.aon.AONService";
    public static final String AON_SERVICE_PACKAGE = "com.xiaomi.aon";
    private static final long CONNECTION_TTL_MILLIS = 60000;
    private static final int DIM_DURATION_CHECK_ATTENTION_TIMEOUT = 20000;
    private static final int EXTERNAL_DISPLAY_CONNECTED = 1;
    private static final long NO_ACTIVITE_CHECK_ATTENTION_MILLIS = 8000;
    private static final int NO_ACTIVITY_CHECK_ATTENTION_TIMEOUT = 2000;
    public static final String REASON_DIM_DURATION_CHECK_ATTENTION = "dim duration";
    public static final String REASON_INTERACTIVE_CHANGE_CHECK_ATTENTION = "interactive change";
    public static final String REASON_NO_USER_ACTIVITY_CHECK_ATTENTION = "no user activity";
    private static final long SERVICE_BIND_AWAIT_MILLIS = 1000;
    private static final int SYNERGY_MODE_ON = 1;
    private static final int TYPE_CHECK_ATTENTION_EYE = 4;
    private static final int TYPE_CHECK_ATTENTION_FACE = 1;
    private boolean mAonScreenOffEnabled;
    private boolean mAonScreenOnEnabled;
    private IMiAON mAonService;
    private final AttentionHandler mAttentionHandler;
    private int mCheckTimeout;
    private ComponentName mComponentName;
    private final Context mContext;
    private boolean mExternalDisplayConnected;
    private int mFps;
    private boolean mIsChecked;
    private Sensor mLightSensor;
    private boolean mLightSensorEnabled;
    private float mLux;
    private WindowManagerPolicy mPolicy;
    private PowerManager mPowerManager;
    private final PowerManagerServiceImpl mPowerManagerServiceImpl;
    private String mReason;
    private boolean mScreenProjectInScreen;
    private SensorManager mSensorManager;
    private CountDownLatch mServiceBindingLatch;
    private SettingsObserver mSettingsObserver;
    private long mStartTimeStamp;
    private boolean mStayOn;
    private boolean mSynergyModeEnable;
    private int mType;
    private static final String TAG = MiuiAttentionDetector.class.getSimpleName();
    private static final int FPS_CHECK_ATTENTION_SCREEN_OFF = FeatureParser.getInteger("aon_screen_off_fps", 10);
    private static final int FPS_CHECK_ATTENTION_SCREEN_ON = FeatureParser.getInteger("aon_screen_on_fps", 15);
    private static final float AON_CHECK_INCORRECT_LUX = FeatureParser.getFloat("config_aon_check_incorrect_lux", 6.0f).floatValue();
    private static final int TYPE_CHECK_ATTENTION = FeatureParser.getInteger("check_attention_type", 1);
    private boolean mInteractive = true;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() { // from class: com.android.server.power.MiuiAttentionDetector.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            MiuiAttentionDetector.this.mLux = event.values[0];
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final IMiAONListener mAttentionListener = new IMiAONListener.Stub() { // from class: com.android.server.power.MiuiAttentionDetector.2
        public void onCallbackListener(int type, int[] data) {
            if (data == null || data.length < 12) {
                Slog.w(MiuiAttentionDetector.TAG, "The aon data is valid!");
                return;
            }
            if (type == 1) {
                if (data[4] == 0 && data[5] == 0) {
                    MiuiAttentionDetector.this.handleAttentionResult(0);
                    return;
                } else {
                    MiuiAttentionDetector.this.handleAttentionResult(1);
                    return;
                }
            }
            if (type == 4) {
                if (data[5] != 1 && data[11] != 1) {
                    MiuiAttentionDetector.this.handleAttentionResult(0);
                } else {
                    MiuiAttentionDetector.this.handleAttentionResult(1);
                }
            }
        }

        public void onImageAvailiable(int frame) {
        }
    };
    private final ServiceConnection mConnection = new AnonymousClass3();

    static {
        boolean z = true;
        AON_SCREEN_ON_SUPPORTED = FeatureParser.getBoolean("config_aon_proximity_available", false) || FeatureParser.getBoolean("config_aon_screen_on_available", false);
        if (!FeatureParser.getBoolean("config_aon_proximity_available", false) && !FeatureParser.getBoolean("config_aon_screen_off_available", false)) {
            z = false;
        }
        AON_SCREEN_OFF_SUPPORTED = z;
    }

    public MiuiAttentionDetector(Context context, PowerManager powerManager, PowerManagerServiceImpl impl, WindowManagerPolicy policy) {
        this.mContext = context;
        this.mPowerManagerServiceImpl = impl;
        this.mPolicy = policy;
        ServiceThread handlerThread = new ServiceThread("MiuiAttentionDetector", -4, false);
        handlerThread.start();
        AttentionHandler attentionHandler = new AttentionHandler(handlerThread.getLooper());
        this.mAttentionHandler = attentionHandler;
        this.mPowerManager = powerManager;
        this.mServiceBindingLatch = new CountDownLatch(1);
        this.mSettingsObserver = new SettingsObserver(attentionHandler);
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mLightSensor = sensorManager.getDefaultSensor(5);
        updateConfigState();
        registerContentObserver();
        try {
            UserSwitchObserver observer = new UserSwitchObserver();
            ActivityManager.getService().registerUserSwitchObserver(observer, TAG);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAttentionResult(int result) {
        if (isScreenCastMode()) {
            Slog.i(TAG, "ignore aon event in hangup mode");
            return;
        }
        long time = SystemClock.uptimeMillis();
        String action = "";
        boolean screenWakeLock = false;
        if (this.mInteractive) {
            screenWakeLock = this.mPowerManagerServiceImpl.isAcquiredScreenBrightWakeLock(0);
            if (result == 0 && this.mLightSensorEnabled && this.mLux < AON_CHECK_INCORRECT_LUX) {
                action = "low lux,ignore";
                onUserActivityChanged();
            } else if (!screenWakeLock && !this.mStayOn && result == 0) {
                action = "dim";
                this.mPowerManagerServiceImpl.requestDimmingRightNowDueToAttention();
                this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.power.MiuiAttentionDetector$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiAttentionDetector.this.lambda$handleAttentionResult$0();
                    }
                });
            } else {
                action = "userActivity";
                this.mPowerManager.userActivity(time, 0, 1024);
            }
        } else if (result == 1) {
            action = "wakeUp";
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "aon");
        }
        if (!TextUtils.isEmpty(action)) {
            this.mPowerManagerServiceImpl.notifyAonScreenOnOffEvent(this.mInteractive, result == 1, this.mStartTimeStamp);
        }
        Slog.i(TAG, "onSuccess: mInteractive:" + this.mInteractive + " screenWakelock:" + screenWakeLock + " result:" + result + " action:" + action + " lux:" + this.mLux);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$handleAttentionResult$0() {
        cancelAttentionCheck();
        checkAttention(TYPE_CHECK_ATTENTION, FPS_CHECK_ATTENTION_SCREEN_OFF, REASON_DIM_DURATION_CHECK_ATTENTION, DIM_DURATION_CHECK_ATTENTION_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.power.MiuiAttentionDetector$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements ServiceConnection {
        AnonymousClass3() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.i(MiuiAttentionDetector.TAG, "Attention service connected success, service:" + service);
            MiuiAttentionDetector.this.mAonService = IMiAON.Stub.asInterface(Binder.allowBlocking(service));
            MiuiAttentionDetector.this.mServiceBindingLatch.countDown();
            MiuiAttentionDetector.this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.power.MiuiAttentionDetector$3$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiAttentionDetector.AnonymousClass3.this.lambda$onServiceConnected$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceConnected$0() {
            MiuiAttentionDetector.this.handlePendingCallback();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName className) {
            Slog.i(MiuiAttentionDetector.TAG, "Attention service connected failure");
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            cleanupService();
        }

        public void cleanupService() {
            MiuiAttentionDetector.this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.power.MiuiAttentionDetector$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiAttentionDetector.AnonymousClass3.this.lambda$cleanupService$1();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$cleanupService$1() {
            MiuiAttentionDetector.this.mAonService = null;
            MiuiAttentionDetector.this.mServiceBindingLatch = new CountDownLatch(1);
            MiuiAttentionDetector.this.mIsChecked = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePendingCallback() {
        if (!this.mIsChecked) {
            checkAttention(this.mType, this.mFps, this.mReason, this.mCheckTimeout);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConfigState() {
        updateAonScreenOnConfig();
        updateAonScreenOffConfig();
        updateScreenProjectConfig();
        updateSynergyModeConfig();
        updateExternalDisplayConnectConfig();
    }

    private void registerContentObserver() {
        ContentResolver cr = this.mContext.getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor("miui_people_near_screen_on"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("gaze_lock_screen_setting"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("screen_project_in_screening"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.System.getUriFor("external_display_connected"), false, this.mSettingsObserver, -1);
    }

    private void bindAonService() {
        if (this.mAonService != null) {
            return;
        }
        if (this.mComponentName == null) {
            this.mComponentName = new ComponentName("com.xiaomi.aon", "com.xiaomi.aon.AONService");
        }
        Intent serviceIntent = new Intent("com.xiaomi.aon.AONService").setComponent(this.mComponentName);
        this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, 67108865, UserHandle.CURRENT);
    }

    public void cancelAndUnbind() {
        if (this.mAonService == null) {
            return;
        }
        cancelAttentionCheck();
        this.mContext.unbindService(this.mConnection);
        this.mAonService = null;
        this.mServiceBindingLatch = new CountDownLatch(1);
    }

    private void awaitServiceBinding() {
        try {
            this.mServiceBindingLatch.await(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.e(TAG, "Interrupted while waiting to bind Attention Service.", e);
        }
    }

    public void checkAttention(int type, int fps, String reason, int timeout) {
        if (this.mInteractive) {
            if (this.mPolicy.isKeyguardShowingAndNotOccluded()) {
                return;
            } else {
                freeIfInactive();
            }
        }
        bindAonService();
        awaitServiceBinding();
        this.mType = type;
        this.mFps = fps;
        this.mReason = reason;
        this.mCheckTimeout = timeout;
        Slog.i(TAG, "check attention, the reason is " + reason + ", attentionService is " + this.mAonService + " mIsChecked:" + this.mIsChecked);
        try {
            if (this.mAonService != null && !this.mIsChecked) {
                this.mStartTimeStamp = SystemClock.uptimeMillis();
                this.mAonService.registerListener(type, fps, timeout, this.mAttentionListener);
                this.mIsChecked = true;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "checkAttention: error " + e);
        }
    }

    public void cancelAttentionCheck() {
        try {
            if (this.mIsChecked && this.mAonService != null) {
                Slog.i(TAG, "Attention check cancel, callers: " + Debug.getCallers(3));
                this.mAonService.unregisterListener(this.mType, this.mAttentionListener);
                this.mIsChecked = false;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "cancelAttentionCheck: error " + e);
        }
    }

    private void freeIfInactive() {
        this.mAttentionHandler.removeMessages(1);
        this.mAttentionHandler.sendEmptyMessageDelayed(1, 60000L);
    }

    public void notifyInteractiveChange(final boolean interactive) {
        if ((!this.mAonScreenOnEnabled && !this.mAonScreenOffEnabled) || isScreenCastMode()) {
            return;
        }
        this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.power.MiuiAttentionDetector$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiAttentionDetector.this.lambda$notifyInteractiveChange$1(interactive);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyInteractiveChange$1(boolean interactive) {
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            boolean z = false;
            setLightSensorEnable(false);
            if (!this.mInteractive) {
                this.mAttentionHandler.removeMessages(1);
                this.mAttentionHandler.removeMessages(2);
            }
            if (!interactive && this.mAonScreenOnEnabled) {
                z = true;
            }
            registerAttentionListenerIfNeeded(z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLightSensorEnable(boolean enabled) {
        if (enabled == this.mLightSensorEnabled) {
            return;
        }
        if (enabled) {
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 3);
        } else {
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        }
        this.mLightSensorEnabled = enabled;
    }

    private void registerAttentionListenerIfNeeded(boolean register) {
        if (register) {
            if (this.mIsChecked) {
                cancelAttentionCheck();
            }
            checkAttention(TYPE_CHECK_ATTENTION, FPS_CHECK_ATTENTION_SCREEN_ON, REASON_INTERACTIVE_CHANGE_CHECK_ATTENTION, ALWAYS_CHECK_ATTENTION_TIMEOUT);
            return;
        }
        cancelAndUnbind();
    }

    public void onUserActivityChanged() {
        if (!this.mAonScreenOffEnabled || isScreenCastMode()) {
            return;
        }
        this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.power.MiuiAttentionDetector$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiAttentionDetector.this.lambda$onUserActivityChanged$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onUserActivityChanged$2() {
        setLightSensorEnable(false);
        cancelAttentionCheck();
        this.mAttentionHandler.removeMessages(2);
        this.mAttentionHandler.sendEmptyMessageDelayed(2, NO_ACTIVITE_CHECK_ATTENTION_MILLIS);
    }

    public void notifyStayOnChanged(boolean stayOn) {
        this.mStayOn = stayOn;
    }

    private boolean isScreenCastMode() {
        return this.mScreenProjectInScreen || this.mSynergyModeEnable || this.mExternalDisplayConnected;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAonScreenOnConfig() {
        boolean z = false;
        if (AON_SCREEN_ON_SUPPORTED && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_people_near_screen_on", 0, -2) != 0) {
            z = true;
        }
        this.mAonScreenOnEnabled = z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScreenProjectConfig() {
        this.mScreenProjectInScreen = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_project_in_screening", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSynergyModeConfig() {
        this.mSynergyModeEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "synergy_mode", 0, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateExternalDisplayConnectConfig() {
        this.mExternalDisplayConnected = Settings.System.getIntForUser(this.mContext.getContentResolver(), "external_display_connected", 0, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAonScreenOffConfig() {
        boolean z = false;
        if (AON_SCREEN_OFF_SUPPORTED && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gaze_lock_screen_setting", 0, -2) != 0) {
            z = true;
        }
        this.mAonScreenOffEnabled = z;
        if (!z) {
            cancelAttentionCheck();
            this.mAttentionHandler.removeMessages(2);
        } else {
            onUserActivityChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AttentionHandler extends Handler {
        private static final int CHECK_ATTENTION_NO_USER_ACTIVITY = 2;
        private static final int CHECK_CONNECTION_EXPIRATION = 1;

        AttentionHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiAttentionDetector.this.cancelAndUnbind();
                    return;
                case 2:
                    MiuiAttentionDetector.this.checkAttention(MiuiAttentionDetector.TYPE_CHECK_ATTENTION, MiuiAttentionDetector.FPS_CHECK_ATTENTION_SCREEN_OFF, MiuiAttentionDetector.REASON_NO_USER_ACTIVITY_CHECK_ATTENTION, 2000);
                    MiuiAttentionDetector.this.setLightSensorEnable(true);
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
                case -1753838504:
                    if (lastPathSegment.equals("external_display_connected")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -1438362181:
                    if (lastPathSegment.equals("synergy_mode")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 117213468:
                    if (lastPathSegment.equals("miui_people_near_screen_on")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1404599639:
                    if (lastPathSegment.equals("gaze_lock_screen_setting")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 2030129845:
                    if (lastPathSegment.equals("screen_project_in_screening")) {
                        c = 2;
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
                    MiuiAttentionDetector.this.updateAonScreenOnConfig();
                    return;
                case 1:
                    MiuiAttentionDetector.this.updateAonScreenOffConfig();
                    return;
                case 2:
                case 3:
                case 4:
                    MiuiAttentionDetector.this.updateScreenProjectConfig();
                    MiuiAttentionDetector.this.updateSynergyModeConfig();
                    MiuiAttentionDetector.this.updateExternalDisplayConnectConfig();
                    return;
                default:
                    return;
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("MiuiAttentionDetector:");
        pw.println(" mComponentName=" + this.mComponentName);
        pw.println(" mAonScreenOnEnabled=" + this.mAonScreenOnEnabled);
        pw.println(" mAonScreenOffEnabled=" + this.mAonScreenOffEnabled);
        pw.println(" mIsChecked=" + this.mIsChecked);
        pw.println(" isScreenCastMode=" + isScreenCastMode());
    }

    /* loaded from: classes.dex */
    private final class UserSwitchObserver extends SynchronousUserSwitchObserver {
        private UserSwitchObserver() {
        }

        public void onUserSwitching(int newUserId) throws RemoteException {
            MiuiAttentionDetector.this.updateConfigState();
        }
    }
}
