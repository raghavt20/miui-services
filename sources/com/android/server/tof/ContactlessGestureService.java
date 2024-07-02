package com.android.server.tof;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.util.DumpUtils;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.power.PowerManagerServiceStub;
import com.android.server.tof.ContactlessGestureService;
import com.miui.tof.gesture.TofGestureAppData;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class ContactlessGestureService extends SystemService {
    private static final int DEBOUNCE_TIME = 5000;
    private static final int EXTERNAL_DISPLAY_CONNECTED = 1;
    public static final String FEATURE_AON_GESTURE_SUPPORT = "config_aon_gesture_available";
    public static final String FEATURE_TOF_GESTURE_SUPPORT = "config_tof_gesture_available";
    public static final String FEATURE_TOF_PROXIMITY_SUPPORT = "config_tof_proximity_available";
    private static final String GESTURE_GUIDE_DISMISS = "gesture_guide_dismiss";
    private static final String GESTURE_THREAD_NAME = "miui.gesture";
    private static final String MIUI_DESK_TOP_MODE = "miui_dkt_mode";
    private static final String SERVICE_NAME = "tof";
    private static final int SYNERGY_MODE_ON = 1;
    private static final int TOF_GESTURE_SENSOR_TYPE = 33171200;
    private static final int TOF_PERSON_LEAVE_SENSOR_TYPE = 33171204;
    private static final int TOF_PERSON_NEAR_SENSOR_TYPE = 33171203;
    private AccessibilityManager mAccessibilityManager;
    public boolean mAonGestureEnabled;
    private boolean mAonGestureSupport;
    private List<String> mComponentFeatureList;
    private ContactlessGestureController mContactlessGestureController;
    private final Context mContext;
    private boolean mDeskTopModeEnabled;
    private boolean mExternalDisplayConnected;
    private Handler mHandler;
    private boolean mIsInteractive;
    private boolean mOneHandedModeEnable;
    private PowerManager mPowerManager;
    private PowerManagerServiceStub mPowerManagerServiceImpl;
    private boolean mScreenProjectInScreen;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mSynergyModeEnable;
    private boolean mTofGestureEnabled;
    private boolean mTofGestureRegistered;
    private Sensor mTofGestureSensor;
    private boolean mTofGestureSupport;
    private boolean mTofPersonLeaveRegistered;
    private Sensor mTofPersonLeaveSensor;
    private boolean mTofPersonNearRegistered;
    private Sensor mTofPersonNearSensor;
    private boolean mTofProximity;
    private boolean mTofProximitySupport;
    private boolean mTofScreenOffEnabled;
    private boolean mTofScreenOnEnabled;
    private final SensorEventListener mTofSensorListener;
    private static final String TAG = ContactlessGestureService.class.getSimpleName();
    public static boolean mDebug = false;

    public ContactlessGestureService(Context context) {
        super(context);
        this.mIsInteractive = true;
        this.mTofSensorListener = new SensorEventListener() { // from class: com.android.server.tof.ContactlessGestureService.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent sensorEvent) {
                float value = sensorEvent.values[0];
                int sensorType = sensorEvent.sensor.getType();
                Slog.i(ContactlessGestureService.TAG, "onSensorChanged, sensor:" + sensorEvent.sensor.getName() + ", value:" + value);
                switch (sensorType) {
                    case ContactlessGestureService.TOF_GESTURE_SENSOR_TYPE /* 33171200 */:
                        ContactlessGestureService.this.handleGestureSensorChanged((int) value);
                        return;
                    case 33171201:
                    case 33171202:
                    default:
                        Slog.e(ContactlessGestureService.TAG, "unknown sensor");
                        return;
                    case ContactlessGestureService.TOF_PERSON_NEAR_SENSOR_TYPE /* 33171203 */:
                        ContactlessGestureService.this.handleTofProximitySensorChanged(value == 1.0f);
                        return;
                    case ContactlessGestureService.TOF_PERSON_LEAVE_SENSOR_TYPE /* 33171204 */:
                        ContactlessGestureService.this.handleTofProximitySensorChanged(value != 1.0f);
                        return;
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        this.mContext = context;
        ServiceThread handlerThread = new ServiceThread(GESTURE_THREAD_NAME, -4, false);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        checkFeatureSupport();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onStart() {
        publishBinderService(SERVICE_NAME, new BinderService());
        Slog.i(TAG, "publish contactless gesture Service");
        publishLocalService(TofManagerInternal.class, new LocalService());
    }

    public void onBootPhase(int phase) {
        if (phase == 1000) {
            systemReady();
        }
    }

    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        Slog.i(TAG, "from user:" + from.getUserHandle().toString() + " getUserIdentifier:" + from.getUserIdentifier() + ", to:" + to.getUserHandle().toString() + " getUserIdentifier:" + to.getUserIdentifier());
        updateConfigState();
        updateTofServiceIfNeeded();
    }

    private void systemReady() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManagerServiceImpl = PowerManagerServiceStub.get();
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mTofPersonNearSensor = sensorManager.getDefaultSensor(TOF_PERSON_NEAR_SENSOR_TYPE, true);
        this.mTofPersonLeaveSensor = this.mSensorManager.getDefaultSensor(TOF_PERSON_LEAVE_SENSOR_TYPE);
        this.mTofGestureSensor = this.mSensorManager.getDefaultSensor(TOF_GESTURE_SENSOR_TYPE);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        if (this.mAonGestureSupport) {
            this.mContactlessGestureController = new AONGestureController(this.mContext, this.mHandler, this);
        } else {
            this.mContactlessGestureController = new ToFGestureController(this.mContext, this.mHandler, this);
        }
        this.mContactlessGestureController.parseGestureComponentFromCloud(this.mComponentFeatureList);
        updateConfigState();
        registerContentObserver();
    }

    private void updateConfigState() {
        updateTofScreenOnConfig();
        updateTofScreenOffConfig();
        updateTofGestureConfig();
        updateAonGestureConfig();
        updateDeskTopModeConfig();
        updateScreenProjectConfig();
        updateSynergyModeConfig();
        updateExternalDisplayConnectConfig();
        handleOneHandedModeEnableChanged();
        registerTofProximityListenerIfNeeded();
    }

    private void registerContentObserver() {
        ContentResolver cr = this.mContext.getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor("miui_tof_screen_on"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("miui_tof_screen_off"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.System.getUriFor(MIUI_DESK_TOP_MODE), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("miui_tof_gesture"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("miui_aon_gesture"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("screen_project_in_screening"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("one_handed_mode_activated"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.System.getUriFor("external_display_connected"), false, this.mSettingsObserver, -1);
        cr.registerContentObserver(Settings.Global.getUriFor(GESTURE_GUIDE_DISMISS), false, this.mSettingsObserver, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerTofProximityListenerIfNeeded() {
        if (this.mTofProximitySupport) {
            registerPersonLeaveListenerIfNeeded(this.mIsInteractive && this.mTofScreenOffEnabled);
            registerPersonNearListenerIfNeeded(!this.mIsInteractive && this.mTofScreenOnEnabled);
        }
    }

    private void registerPersonLeaveListenerIfNeeded(boolean register) {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        if (register && !this.mTofPersonLeaveRegistered) {
            this.mTofPersonLeaveRegistered = sensorManager.registerListener(this.mTofSensorListener, this.mTofPersonLeaveSensor, 3);
            Slog.i(TAG, "registerPersonLeaveListener: " + this.mTofPersonLeaveRegistered);
        } else if (!register && this.mTofPersonLeaveRegistered) {
            sensorManager.unregisterListener(this.mTofSensorListener, this.mTofPersonLeaveSensor);
            this.mTofPersonLeaveRegistered = false;
        }
    }

    private void registerPersonNearListenerIfNeeded(boolean register) {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        if (register && !this.mTofPersonNearRegistered) {
            this.mTofPersonNearRegistered = sensorManager.registerListener(this.mTofSensorListener, this.mTofPersonNearSensor, 3);
            Slog.i(TAG, "registerPersonNearListener: " + this.mTofPersonNearRegistered);
        } else if (!register && this.mTofPersonNearRegistered) {
            sensorManager.unregisterListener(this.mTofSensorListener, this.mTofPersonNearSensor);
            this.mTofPersonNearRegistered = false;
        }
    }

    public void registerContactlessGestureListenerIfNeeded(boolean register) {
        if (register) {
            startGestureClientIfNeeded();
            showTofGestureNotificationIfNeeded();
        }
        if (this.mTofGestureSupport && this.mTofGestureEnabled) {
            registerTofGestureListenerIfNeeded(register);
        } else if (this.mAonGestureSupport && this.mAonGestureEnabled) {
            this.mContactlessGestureController.registerGestureListenerIfNeeded(register);
            Slog.i(TAG, "registerGestureListener:" + register);
        }
    }

    private void registerTofGestureListenerIfNeeded(boolean register) {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            Slog.e(TAG, "SensorManager is not ready, reject register tof sensor.");
            return;
        }
        if (register && !this.mTofGestureRegistered) {
            this.mTofGestureRegistered = sensorManager.registerListener(this.mTofSensorListener, this.mTofGestureSensor, 3);
        } else if (!register && this.mTofGestureRegistered) {
            sensorManager.unregisterListener(this.mTofSensorListener, this.mTofGestureSensor);
            this.mTofGestureRegistered = false;
        }
        Slog.i(TAG, "registerTofGestureListener: " + register + ",mTofGestureRegistered:" + this.mTofGestureRegistered);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGestureSensorChanged(int label) {
        if (!this.mContactlessGestureController.isServiceInit()) {
            this.mContactlessGestureController.bindService();
        }
        this.mContactlessGestureController.handleGestureEvent(label);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTofProximitySensorChanged(boolean proximity) {
        this.mTofProximity = proximity;
        boolean hangupMode = isScreenCastMode();
        boolean screenWakeLock = false;
        long lastUserActivityTime = 0;
        if (hangupMode) {
            Slog.i(TAG, "ignore Tof sensor in hangup mode");
            return;
        }
        if (this.mTofProximity) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, SERVICE_NAME);
            this.mPowerManagerServiceImpl.notifyTofPowerState(true);
        } else {
            screenWakeLock = this.mPowerManagerServiceImpl.isAcquiredScreenBrightWakeLock(0);
            lastUserActivityTime = this.mPowerManagerServiceImpl.getDefaultDisplayLastUserActivity();
            long time = SystemClock.uptimeMillis();
            if (!screenWakeLock && time - lastUserActivityTime > 5000) {
                this.mPowerManager.goToSleep(time, 0, 0);
                this.mPowerManagerServiceImpl.notifyTofPowerState(false);
            }
        }
        Slog.i(TAG, this.mTofProximity ? "wake up" : "go to sleep, screenWakeLock: " + screenWakeLock + ", lastUserActivity: " + TimeUtils.formatUptime(lastUserActivityTime));
    }

    private void handleDeskTopModeChanged() {
        this.mContactlessGestureController.updateDeskTopMode(this.mDeskTopModeEnabled);
    }

    private void handleScreenCastModeChanged() {
        if (this.mTofGestureSupport && this.mTofGestureEnabled) {
            this.mContactlessGestureController.onSceneChanged(6);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAonGestureSupport() {
        return true;
    }

    private void checkFeatureSupport() {
        this.mTofProximitySupport = FeatureParser.getBoolean(FEATURE_TOF_PROXIMITY_SUPPORT, false);
        this.mTofGestureSupport = FeatureParser.getBoolean(FEATURE_TOF_GESTURE_SUPPORT, false);
        this.mAonGestureSupport = FeatureParser.getBoolean("config_aon_gesture_available", false);
        Slog.i(TAG, "check feature support mTofProximitySupport:" + this.mTofProximitySupport + ", mTofGestureSupport:" + this.mTofGestureSupport + ", mAonGestureSupport:" + this.mAonGestureSupport);
    }

    public boolean isSupportTofPersonProximityFeature() {
        return this.mTofProximitySupport;
    }

    public boolean isSupportTofGestureFeature() {
        return this.mTofGestureSupport;
    }

    public boolean isScreenCastMode() {
        return this.mScreenProjectInScreen || this.mSynergyModeEnable || this.mExternalDisplayConnected;
    }

    public boolean isTalkBackEnabled() {
        AccessibilityManager accessibilityManager = this.mAccessibilityManager;
        return accessibilityManager != null && accessibilityManager.isEnabled() && this.mAccessibilityManager.isTouchExplorationEnabled();
    }

    public boolean isOneHandedModeEnabled() {
        return this.mOneHandedModeEnable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int setDebug(boolean enable) {
        mDebug = enable;
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleSettingsChanged(Uri uri) {
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        switch (lastPathSegment.hashCode()) {
            case -1753838504:
                if (lastPathSegment.equals("external_display_connected")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1438362181:
                if (lastPathSegment.equals("synergy_mode")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -839612986:
                if (lastPathSegment.equals("miui_tof_gesture")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -401108015:
                if (lastPathSegment.equals(GESTURE_GUIDE_DISMISS)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -396480129:
                if (lastPathSegment.equals("miui_tof_screen_off")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -151337009:
                if (lastPathSegment.equals("miui_tof_screen_on")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 657459245:
                if (lastPathSegment.equals("one_handed_mode_activated")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1662957947:
                if (lastPathSegment.equals("miui_aon_gesture")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1967212548:
                if (lastPathSegment.equals(MIUI_DESK_TOP_MODE)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 2030129845:
                if (lastPathSegment.equals("screen_project_in_screening")) {
                    c = 4;
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
            case 1:
                updateTofScreenOnConfig();
                updateTofScreenOffConfig();
                registerTofProximityListenerIfNeeded();
                return;
            case 2:
                updateTofGestureConfig();
                startGestureClientIfNeeded();
                return;
            case 3:
                updateAonGestureConfig();
                startGestureClientIfNeeded();
                return;
            case 4:
            case 5:
            case 6:
                updateScreenProjectConfig();
                updateSynergyModeConfig();
                updateExternalDisplayConnectConfig();
                handleScreenCastModeChanged();
                return;
            case 7:
                handleOneHandedModeEnableChanged();
                return;
            case '\b':
                updateDeskTopModeConfig();
                return;
            case '\t':
                handleGestureGuideDismiss();
                return;
            default:
                return;
        }
    }

    private void handleGestureGuideDismiss() {
        boolean guideDismiss = Settings.Global.getInt(this.mContext.getContentResolver(), GESTURE_GUIDE_DISMISS, 0) == 1;
        if (this.mAonGestureSupport && this.mAonGestureEnabled && guideDismiss) {
            Slog.i(TAG, "the guide dismiss,register aon listener anew.");
            this.mContactlessGestureController.registerGestureListenerIfNeeded(false);
            this.mContactlessGestureController.onSceneChanged(6);
            Settings.Global.putInt(this.mContext.getContentResolver(), GESTURE_GUIDE_DISMISS, 0);
        }
    }

    private void handleOneHandedModeEnableChanged() {
        this.mOneHandedModeEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "one_handed_mode_activated", 0, -2) != 0;
        if (this.mAonGestureSupport && this.mAonGestureEnabled) {
            this.mContactlessGestureController.onSceneChanged(6);
        }
    }

    private void updateScreenProjectConfig() {
        this.mScreenProjectInScreen = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_project_in_screening", 0, -2) != 0;
    }

    private void updateSynergyModeConfig() {
        this.mSynergyModeEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "synergy_mode", 0, -2) == 1;
    }

    private void updateExternalDisplayConnectConfig() {
        this.mExternalDisplayConnected = Settings.System.getIntForUser(this.mContext.getContentResolver(), "external_display_connected", 0, -2) == 1;
    }

    private void updateDeskTopModeConfig() {
        this.mDeskTopModeEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), MIUI_DESK_TOP_MODE, 0, -2) != 0;
        handleDeskTopModeChanged();
    }

    private void updateTofScreenOnConfig() {
        this.mTofScreenOnEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_screen_on", 0, -2) != 0;
    }

    private void updateTofScreenOffConfig() {
        this.mTofScreenOffEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_screen_off", 0, -2) != 0;
    }

    private void updateTofGestureConfig() {
        this.mTofGestureEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_tof_gesture", 0, -2) != 0;
    }

    private void updateAonGestureConfig() {
        this.mAonGestureEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "miui_aon_gesture", 0, -2) != 0;
    }

    public void showTofGestureNotificationIfNeeded() {
        if ((this.mTofGestureSupport && !this.mTofGestureEnabled) || (this.mAonGestureSupport && !this.mAonGestureEnabled)) {
            this.mContactlessGestureController.showGestureNotification();
        }
    }

    private void updateTofServiceIfNeeded() {
        ContactlessGestureController contactlessGestureController;
        if (this.mTofGestureSupport && (contactlessGestureController = this.mContactlessGestureController) != null && contactlessGestureController.isServiceInit()) {
            this.mContactlessGestureController.unBindService();
        }
    }

    public void startGestureClientIfNeeded() {
        if ((this.mTofGestureSupport || this.mAonGestureSupport) && !this.mContactlessGestureController.isServiceInit()) {
            Slog.i(TAG, "start bind contactless gesture client.");
            this.mContactlessGestureController.bindService();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("ContactlessGesture service status ");
        pw.println("   mTofProximitySupport:" + this.mTofProximitySupport);
        if (this.mTofProximitySupport) {
            pw.println("       mTofProximity:" + this.mTofProximity);
            pw.println("       mTofScreenOnEnabled:" + this.mTofScreenOnEnabled);
            pw.println("       mTofScreenOffEnabled:" + this.mTofScreenOffEnabled);
            pw.println("       mTofPersonNearRegistered:" + this.mTofPersonNearRegistered);
            pw.println("       mTofPersonLeaveRegistered:" + this.mTofPersonLeaveRegistered);
        }
        pw.println("   mTofGestureSupport:" + this.mTofGestureSupport);
        if (this.mTofGestureSupport) {
            pw.println("       mTofGestureEnabled:" + this.mTofGestureEnabled);
            pw.println("       mTofGestureRegistered:" + this.mTofGestureRegistered);
        }
        pw.println("   mAonGestureSupport:" + this.mAonGestureSupport);
        if (this.mTofGestureSupport) {
            pw.println("       mAonGestureEnabled:" + this.mAonGestureEnabled);
        }
        ContactlessGestureController contactlessGestureController = this.mContactlessGestureController;
        if (contactlessGestureController != null) {
            contactlessGestureController.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContactlessGestureService.this.handleSettingsChanged(uri);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends TofManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.tof.TofManagerInternal
        public void onEarlyInteractivityChange(boolean interactive) {
            if (ContactlessGestureService.this.mTofProximitySupport) {
                ContactlessGestureService.this.mIsInteractive = interactive;
                ContactlessGestureService.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureService$LocalService$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContactlessGestureService.LocalService.this.lambda$onEarlyInteractivityChange$0();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onEarlyInteractivityChange$0() {
            ContactlessGestureService.this.registerTofProximityListenerIfNeeded();
        }

        @Override // com.android.server.tof.TofManagerInternal
        public void onDefaultDisplayFocusChanged(final String packageName) {
            ContactlessGestureService.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureService$LocalService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureService.LocalService.this.lambda$onDefaultDisplayFocusChanged$1(packageName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDefaultDisplayFocusChanged$1(String packageName) {
            if ((ContactlessGestureService.this.mTofGestureEnabled || ContactlessGestureService.this.mAonGestureEnabled) && ContactlessGestureService.this.mContactlessGestureController != null) {
                ContactlessGestureService.this.mContactlessGestureController.updateFocusedPackage(packageName);
            }
        }

        @Override // com.android.server.tof.TofManagerInternal
        public void updateGestureAppConfig(final List<String> componentFeatureList) {
            ContactlessGestureService.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureService$LocalService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureService.LocalService.this.lambda$updateGestureAppConfig$2(componentFeatureList);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$updateGestureAppConfig$2(List componentFeatureList) {
            if (!ContactlessGestureService.this.isAonGestureSupport()) {
                return;
            }
            if (ContactlessGestureService.this.mContactlessGestureController != null) {
                Slog.i(ContactlessGestureService.TAG, "updateGestureAppConfig");
                ContactlessGestureService.this.mContactlessGestureController.parseGestureComponentFromCloud(componentFeatureList);
                ContactlessGestureService.this.mComponentFeatureList = null;
                return;
            }
            ContactlessGestureService.this.mComponentFeatureList = componentFeatureList;
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        TofServiceShellCommand mTofServiceShellCommand;

        private BinderService() {
            this.mTofServiceShellCommand = new TofServiceShellCommand();
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (!ContactlessGestureService.this.mTofGestureSupport && !ContactlessGestureService.this.mAonGestureSupport) {
                return super.onTransact(code, data, reply, flags);
            }
            switch (code) {
                case 16777213:
                    getTofGestureAppData(data, reply);
                    return true;
                case 16777214:
                    setTofGestureConfig(data);
                    Binder.getCallingUserHandle();
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private void setTofGestureConfig(Parcel data) {
            long token = Binder.clearCallingIdentity();
            try {
                Binder.getCallingUserHandle();
                data.enforceInterface("com.miui.tof.TofManager");
                String packageName = data.readString();
                boolean enable = data.readBoolean();
                ContactlessGestureService.this.mContactlessGestureController.changeAppConfigWithPackageName(packageName, enable);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        private void getTofGestureAppData(Parcel data, Parcel reply) {
            long token = Binder.clearCallingIdentity();
            try {
                data.enforceInterface("com.miui.tof.TofManager");
                TofGestureAppData appData = ContactlessGestureService.this.mContactlessGestureController.getTofGestureAppData();
                reply.writeNoException();
                reply.writeParcelable(appData, 0);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            this.mTofServiceShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(ContactlessGestureService.this.mContext, ContactlessGestureService.TAG, pw)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                ContactlessGestureService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class TofServiceShellCommand extends ShellCommand {
        private TofServiceShellCommand() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            String component;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter err = getErrPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case -75080375:
                        if (cmd.equals("gesture")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 134257820:
                        if (cmd.equals("tof-pro-sensor")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 269792944:
                        if (cmd.equals("tof-start-gesture-app")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1040450170:
                        if (cmd.equals("logging-disable")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1728842673:
                        if (cmd.equals("logging-enable")) {
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
                        return ContactlessGestureService.this.setDebug(true);
                    case 1:
                        return ContactlessGestureService.this.setDebug(false);
                    case 2:
                        String powerOn = Build.TYPE;
                        if (!powerOn.equals("user") && (component = getNextArgRequired()) != null) {
                            ContactlessGestureService.this.mContactlessGestureController.bindService(ComponentName.unflattenFromString(component));
                        }
                        return 0;
                    case 3:
                        String powerOn2 = getNextArgRequired();
                        ContactlessGestureService.this.handleTofProximitySensorChanged("true".equals(powerOn2));
                        return 0;
                    case 4:
                        if (ContactlessGestureService.mDebug || !Build.TYPE.equals("user")) {
                            String label = getNextArgRequired();
                            if (!TextUtils.isEmpty(label)) {
                                ContactlessGestureService.this.mContactlessGestureController.handleGestureEvent(Integer.valueOf(label).intValue());
                            }
                        }
                        return 0;
                    default:
                        dumpHelp();
                        return 0;
                }
            } catch (IllegalArgumentException e) {
                err.println("Error: " + e.getMessage());
                return -1;
            }
        }

        public void onHelp() {
            dumpHelp();
        }

        private void dumpHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Contactless gesture commands: ");
            pw.println("  --help|-h");
            pw.println("    Print this help text.");
            pw.println("  logging-enable");
            pw.println("    Enable logging.");
            pw.println("  logging-disable");
            pw.println("    Disable logging.");
            pw.println("  tof-start-gesture-app");
            pw.println("    Launch the Tof gesture app.");
            pw.println("  tof-pro-sensor");
            pw.println("    Simulating the proximity sensor state.");
            pw.println("  gesture");
            pw.println("    Simulating the gesture sensor events.");
        }
    }
}
