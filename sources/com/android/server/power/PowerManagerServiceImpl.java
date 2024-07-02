package com.android.server.power;

import android.R;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.widget.Toast;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.policy.DisplayTurnoverManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerService;
import com.android.server.power.statistic.MiuiPowerStatisticTracker;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.whetstone.PowerKeeperPolicy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import miui.app.IFreeformCallback;
import miui.app.MiuiFreeFormManager;
import miui.greeze.IGreezeManager;
import miui.os.DeviceFeature;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;
import miui.util.FeatureParser;

@MiuiStubHead(manifestName = "com.android.server.power.PowerManagerServiceStub$$")
/* loaded from: classes.dex */
public class PowerManagerServiceImpl extends PowerManagerServiceStub {
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY = 15000;
    private static final int DEFAULT_SUBSCREEN_SUPER_POWER_SAVE_MODE = 0;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int FLAG_BOOST_BRIGHTNESS = 2;
    private static final int FLAG_KEEP_SECONDARY_ALWAYS_ON = 1;
    private static final int FLAG_SCREEN_PROJECTION = 0;
    private static final boolean IS_FOLDABLE_DEVICE;
    private static final String KEY_DEVICE_LOCK = "com.xiaomi.system.devicelock.locked";
    private static final String KEY_SECONDARY_SCREEN_ENABLE = "subscreen_switch";
    private static final int MSG_UPDATE_FOREGROUND_APP = 1;
    private static final int PICK_UP_SENSOR_TYPE = 33171036;
    private static final String POWER_SAVE_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    private static final String REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL = "CAMERA_CALL";
    private static final String SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY = "subscreen_display_time";
    private static final String SUBSCREEN_SUPER_POWER_SAVE_MODE = "subscreen_super_power_save_mode";
    private static final int SUB_DISPLAY_GROUP_ID = 1;
    private static final String TAG = "PowerManagerServiceImpl";
    private static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    private static final int WAKE_LOCK_SCREEN_DIM = 4;
    private static final int WAKE_LOCK_STAY_AWAKE = 32;
    public static final String WAKING_UP_DETAILS_FINGERPRINT = "android.policy:FINGERPRINT";
    private static final float mBrightnessDecreaseRatio;
    private boolean isDeviceLock;
    private IActivityTaskManager mActivityTaskManager;
    private boolean mAdaptiveSleepEnabled;
    private boolean mAlwaysWakeUp;
    private boolean mAonScreenOffEnabled;
    private MiuiAttentionDetector mAttentionDetector;
    private boolean mBootCompleted;
    private boolean mBrightnessBoostForSoftLightInProgress;
    private Context mContext;
    private boolean mDimEnable;
    private long mDisplayStateChangeStateTime;
    private int mDriveMode;
    private Boolean mFolded;
    private String mForegroundAppPackageName;
    private IGreezeManager mGreezeManager;
    private Handler mHandler;
    private boolean mHangUpEnabled;
    private boolean mIsBrightWakeLock;
    private volatile boolean mIsFreeFormOrSplitWindowMode;
    private boolean mIsPowerSaveModeEnabled;
    private boolean mIsUserSetupComplete;
    private long mLastRequestDimmingTime;
    private String mLastSecondaryDisplayGoToSleepReason;
    private long mLastSecondaryDisplayGoToSleepTime;
    private long mLastSecondaryDisplayUserActivityTime;
    private String mLastSecondaryDisplayWakeUpReason;
    private long mLastSecondaryDisplayWakeUpTime;
    private Object mLock;
    private MiuiInputManagerInternal mMiuiInputManagerInternal;
    private MiuiPowerStatisticTracker mMiuiPowerStatisticTracker;
    private boolean mNoSupprotInnerScreenProximitySensor;
    private NotifierInjector mNotifierInjector;
    private String mPendingForegroundAppPackageName;
    private boolean mPendingSetDirtyDueToRequestDimming;
    private boolean mPendingStopBrightnessBoost;
    private boolean mPickUpGestureWakeUpEnabled;
    private Sensor mPickUpSensor;
    private boolean mPickUpSensorEnabled;
    private WindowManagerPolicy mPolicy;
    private SparseArray<PowerGroup> mPowerGroups;
    private PowerKeeperPolicy mPowerKeeperPolicy;
    private PowerManager mPowerManager;
    private PowerManagerService mPowerManagerService;
    private int mPreWakefulness;
    private ContentResolver mResolver;
    private boolean mScreenProjectionEnabled;
    private boolean mSecondaryDisplayEnabled;
    private long mSecondaryDisplayScreenOffTimeout;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mSituatedDimmingDueToAttention;
    private boolean mSituatedDimmingDueToSynergy;
    private boolean mSupportAdaptiveSleep;
    private boolean mSynergyModeEnable;
    private boolean mSystemReady;
    private boolean mTouchInteractiveUnavailable;
    private int mUserActivitySecondaryDisplaySummary;
    private ArrayList<PowerManagerService.WakeLock> mWakeLocks;
    private int mWakefulness;
    private static final boolean SUPPORT_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    public static final String WAKING_UP_DETAILS_GOTO_UNLOCK = "com.android.systemui:GOTO_UNLOCK";
    public static final String WAKING_UP_DETAILS_BIOMETRIC = "android.policy:BIOMETRIC";
    private static final List<String> SKIP_TRANSITION_WAKE_UP_DETAILS = Arrays.asList("android.policy:FINGERPRINT", WAKING_UP_DETAILS_GOTO_UNLOCK, WAKING_UP_DETAILS_BIOMETRIC);
    private static boolean DEBUG = false;
    private static final boolean OPTIMIZE_WAKELOCK_ENABLED = FeatureParser.getBoolean("optimize_wakelock_enabled", true);
    private final List<Integer> mVisibleWindowUids = Collections.synchronizedList(new ArrayList());
    private long mUserActivityTimeoutOverrideFromMirrorManager = -1;
    private boolean mIsKeyguardHasShownWhenExitHangup = true;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks = new HashMap<>();
    private long mBrightWaitTime = 0;
    private int mSubScreenSuperPowerSaveMode = 0;
    private final List<String> mScreenWakeLockWhitelists = new ArrayList();
    private final Runnable mUpdateForegroundAppRunnable = new Runnable() { // from class: com.android.server.power.PowerManagerServiceImpl$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            PowerManagerServiceImpl.this.updateForegroundApp();
        }
    };
    private final TaskStackListener mTaskStackListener = new TaskStackListener() { // from class: com.android.server.power.PowerManagerServiceImpl.1
        public void onTaskStackChanged() {
            BackgroundThread.getHandler().removeCallbacks(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
            BackgroundThread.getHandler().post(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
        }
    };
    private final IForegroundWindowListener mWindowListener = new IForegroundWindowListener.Stub() { // from class: com.android.server.power.PowerManagerServiceImpl.2
        public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
            BackgroundThread.getHandler().removeCallbacks(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
            BackgroundThread.getHandler().post(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
        }
    };
    private final IFreeformCallback mFreeformCallBack = new IFreeformCallback.Stub() { // from class: com.android.server.power.PowerManagerServiceImpl.3
        public void dispatchFreeFormStackModeChanged(int action, MiuiFreeFormManager.MiuiFreeFormStackInfo stackInfo) {
            switch (action) {
                case 3:
                case 5:
                    BackgroundThread.getHandler().removeCallbacks(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
                    BackgroundThread.getHandler().post(PowerManagerServiceImpl.this.mUpdateForegroundAppRunnable);
                    return;
                case 4:
                default:
                    return;
            }
        }
    };
    private final SensorEventListener mPickUpSensorListener = new SensorEventListener() { // from class: com.android.server.power.PowerManagerServiceImpl.5
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.values[0] == 1.0f) {
                PowerManagerServiceImpl.this.updateUserActivityLocked();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PowerManagerServiceImpl> {

        /* compiled from: PowerManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PowerManagerServiceImpl INSTANCE = new PowerManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PowerManagerServiceImpl m2277provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PowerManagerServiceImpl m2276provideNewInstance() {
            return new PowerManagerServiceImpl();
        }
    }

    static {
        IS_FOLDABLE_DEVICE = SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2;
        mBrightnessDecreaseRatio = FeatureParser.getFloat("brightness_decrease_ratio", 0.9f).floatValue();
    }

    public void init(PowerManagerService powerManagerService, ArrayList<PowerManagerService.WakeLock> allWakeLocks, Object lock, Context context, Looper looper, SparseArray<PowerGroup> powerGroup) {
        this.mContext = context;
        this.mPowerManagerService = powerManagerService;
        this.mWakeLocks = allWakeLocks;
        this.mLock = lock;
        this.mHandler = new PowerManagerStubHandler(looper);
        this.mPowerKeeperPolicy = PowerKeeperPolicy.getInstance();
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSystemReady = true;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mNotifierInjector = new NotifierInjector(this.mContext);
        this.mPowerGroups = powerGroup;
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mPickUpSensor = sensorManager.getDefaultSensor(PICK_UP_SENSOR_TYPE, true);
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        systemReady();
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = MiuiPowerStatisticTracker.getInstance();
        this.mMiuiPowerStatisticTracker = miuiPowerStatisticTracker;
        miuiPowerStatisticTracker.init(this.mContext);
        if (MiuiAttentionDetector.AON_SCREEN_OFF_SUPPORTED || MiuiAttentionDetector.AON_SCREEN_ON_SUPPORTED) {
            this.mAttentionDetector = new MiuiAttentionDetector(context, this.mPowerManager, this, this.mPolicy);
        }
    }

    private void systemReady() {
        this.mSupportAdaptiveSleep = this.mContext.getResources().getBoolean(R.bool.config_allowStartActivityForLongPressOnPowerInSetup);
        String[] whitelist = this.mContext.getResources().getStringArray(285409359);
        this.mScreenWakeLockWhitelists.addAll(Arrays.asList(whitelist));
        this.mNoSupprotInnerScreenProximitySensor = this.mContext.getResources().getBoolean(285540459);
        registerForegroundAppObserver();
        resetScreenProjectionSettingsLocked();
        loadSettingsLocked();
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("screen_project_in_screening"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("screen_project_hang_up_on"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor(KEY_DEVICE_LOCK), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(SUBSCREEN_SUPER_POWER_SAVE_MODE), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("drive_mode_drive_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("pick_up_gesture_wakeup_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("adaptive_sleep"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("gaze_lock_screen_setting"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(POWER_SAVE_MODE_OPEN), false, this.mSettingsObserver, -1);
        if (IS_FOLDABLE_DEVICE) {
            DeviceStateManager deviceStateManager = (DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class);
            deviceStateManager.registerCallback(new HandlerExecutor(this.mHandler), new DeviceStateManager.FoldStateListener(this.mContext, new Consumer() { // from class: com.android.server.power.PowerManagerServiceImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PowerManagerServiceImpl.this.onFoldStateChanged(((Boolean) obj).booleanValue());
                }
            }));
        }
    }

    private void loadSettingsLocked() {
        updateDriveModeLocked();
        updateSecondaryDisplayScreenOffTimeoutLocked();
        updateUserSetupCompleteLocked();
        updatePickUpGestureWakeUpModeLocked();
        updateAdaptiveSleepLocked();
        updateAonScreenOffConfig();
        updatePowerSaveMode();
    }

    private void updateDriveModeLocked() {
        this.mDriveMode = Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", 0, -2);
    }

    private void updatePickUpGestureWakeUpModeLocked() {
        this.mPickUpGestureWakeUpEnabled = Settings.System.getIntForUser(this.mResolver, "pick_up_gesture_wakeup_mode", 0, -2) == 1;
    }

    private void updateAdaptiveSleepLocked() {
        this.mAdaptiveSleepEnabled = Settings.Secure.getIntForUser(this.mResolver, "adaptive_sleep", 0, -2) == 1;
    }

    private void updateAonScreenOffConfig() {
        this.mAonScreenOffEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gaze_lock_screen_setting", 0, -2) != 0;
    }

    public void setPickUpSensorEnable(boolean isDimming) {
        if ((this.mSupportAdaptiveSleep || MiuiAttentionDetector.AON_SCREEN_OFF_SUPPORTED) && ((this.mAdaptiveSleepEnabled || this.mAonScreenOffEnabled) && this.mPickUpGestureWakeUpEnabled && !this.mPolicy.isKeyguardShowingAndNotOccluded() && isDimming && !this.mPickUpSensorEnabled)) {
            this.mPickUpSensorEnabled = true;
            this.mSensorManager.registerListener(this.mPickUpSensorListener, this.mPickUpSensor, 3);
        } else if (this.mPickUpSensorEnabled && !isDimming) {
            this.mPickUpSensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mPickUpSensorListener);
        }
    }

    private void registerForegroundAppObserver() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            MiuiFreeFormManager.registerFreeformCallback(this.mFreeformCallBack);
            ProcessManager.registerForegroundWindowListener(this.mWindowListener);
            updateForegroundApp();
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApp() {
        boolean z;
        try {
            ActivityTaskManager.RootTaskInfo info = this.mActivityTaskManager.getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null) {
                if (info.getWindowingMode() != 5 && !this.mActivityTaskManager.isInSplitScreenWindowingMode()) {
                    z = false;
                    this.mIsFreeFormOrSplitWindowMode = z;
                    String packageName = info.topActivity.getPackageName();
                    this.mPendingForegroundAppPackageName = packageName;
                    this.mHandler.removeMessages(1);
                    this.mHandler.sendEmptyMessage(1);
                }
                z = true;
                this.mIsFreeFormOrSplitWindowMode = z;
                String packageName2 = info.topActivity.getPackageName();
                this.mPendingForegroundAppPackageName = packageName2;
                this.mHandler.removeMessages(1);
                this.mHandler.sendEmptyMessage(1);
            }
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundAppPackageName() {
        this.mForegroundAppPackageName = this.mPendingForegroundAppPackageName;
        this.mPendingForegroundAppPackageName = null;
        updateProximityWakeLockDisabledState();
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = this.mMiuiPowerStatisticTracker;
        if (miuiPowerStatisticTracker != null) {
            miuiPowerStatisticTracker.notifyForegroundAppChanged(this.mForegroundAppPackageName);
        }
    }

    private void updateProximityWakeLockDisabledState() {
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                setProximityWakeLockDisabledStateLocked(wakeLock, true);
            }
        }
    }

    protected void setProximityWakeLockDisabledStateLocked(PowerManagerService.WakeLock wakeLock, boolean notAcquired) {
        if (OPTIMIZE_WAKELOCK_ENABLED && UserHandle.isApp(wakeLock.mOwnerUid) && (wakeLock.mFlags & 65535) == 32) {
            boolean isInForeground = wakeLock.mPackageName.equals(this.mForegroundAppPackageName);
            boolean isUnfolded = IS_FOLDABLE_DEVICE && !this.mFolded.booleanValue();
            boolean isDisabledProximitySensor = isUnfolded && this.mNoSupprotInnerScreenProximitySensor;
            if (notAcquired) {
                boolean shouldDisabled = !isInForeground || this.mIsFreeFormOrSplitWindowMode || isDisabledProximitySensor;
                if (shouldDisabled != wakeLock.mDisabled) {
                    wakeLock.setDisabled(shouldDisabled);
                    printProximityWakeLockStatusChange(wakeLock, shouldDisabled);
                    if (wakeLock.mDisabled) {
                        this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
                    } else {
                        this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                    }
                    this.mPowerManagerService.setWakeLockDirtyLocked();
                    this.mPowerManagerService.updatePowerStateLocked();
                    return;
                }
                return;
            }
            if ((!this.mPolicy.isKeyguardOccluded() && !isInForeground) || this.mIsFreeFormOrSplitWindowMode || isDisabledProximitySensor) {
                wakeLock.setDisabled(true);
                printProximityWakeLockStatusChange(wakeLock, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFoldStateChanged(boolean folded) {
        Boolean bool = this.mFolded;
        if (bool != null && bool.booleanValue() == folded) {
            return;
        }
        this.mFolded = Boolean.valueOf(folded);
        updateProximityWakeLockDisabledState();
    }

    public void addVisibleWindowUids(int uid) {
        if (!this.mVisibleWindowUids.contains(Integer.valueOf(uid))) {
            this.mVisibleWindowUids.add(Integer.valueOf(uid));
        }
    }

    public void clearVisibleWindowUids() {
        if (!this.mVisibleWindowUids.isEmpty()) {
            this.mVisibleWindowUids.clear();
        }
    }

    public void checkScreenWakeLockDisabledStateLocked() {
        boolean state;
        boolean changed = false;
        Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
        while (it.hasNext()) {
            PowerManagerService.WakeLock wakeLock = it.next();
            if (PowerManagerService.isScreenLock(wakeLock) && UserHandle.isApp(wakeLock.mOwnerUid) && !this.mScreenWakeLockWhitelists.contains(wakeLock.mPackageName) && (state = this.mVisibleWindowUids.contains(Integer.valueOf(wakeLock.mOwnerUid))) == wakeLock.mDisabled) {
                changed = true;
                wakeLock.mDisabled = !state;
                if (wakeLock.mDisabled) {
                    this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
                } else {
                    this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                }
                Slog.d(TAG, "screen wakeLock:[" + wakeLock.toString() + "]" + (wakeLock.mDisabled ? "disabled" : "enabled"));
            }
        }
        if (changed) {
            this.mPowerManagerService.setWakeLockDirtyLocked();
        }
    }

    protected void printWakeLockDetails(PowerManagerService.WakeLock wakeLock, boolean isAcquired) {
        switch (wakeLock.mFlags & 65535) {
            case 6:
            case 10:
            case 26:
            case 32:
                StringBuilder sb = new StringBuilder();
                sb.append(isAcquired ? "Acquire wakelock: " : "Release wakelock: ");
                sb.append(wakeLock);
                Slog.i(TAG, sb.toString());
                return;
            default:
                return;
        }
    }

    private void printProximityWakeLockStatusChange(PowerManagerService.WakeLock wakeLock, boolean disabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("Proximity wakelock:[").append(wakeLock).append("]").append(", disabled: ").append(disabled).append(", reason: ");
        if (disabled) {
            if (IS_FOLDABLE_DEVICE && !this.mFolded.booleanValue() && this.mNoSupprotInnerScreenProximitySensor) {
                sb.append("in unfold state");
            } else {
                sb.append(this.mIsFreeFormOrSplitWindowMode ? "in freeform or split window mode" : "in background");
            }
        } else if (IS_FOLDABLE_DEVICE && !this.mFolded.booleanValue() && !this.mNoSupprotInnerScreenProximitySensor) {
            sb.append("making a call from the internal screen");
        } else {
            sb.append("in foreground");
        }
        Slog.i(TAG, sb.toString());
    }

    public void printPartialWakeLockDisabledIfNeeded(PowerManagerService.WakeLock wakeLock, boolean disabled, String reason) {
        if (wakeLock.mDisabled != disabled) {
            Slog.i(TAG, "Partial wakeLock:[" + wakeLock + "], disabled: " + disabled + ", procState: " + wakeLock.mUidState.mProcState + ", reason: " + reason);
        }
    }

    public void setTouchInteractive(boolean isDimming) {
        if (isDimming && !this.mTouchInteractiveUnavailable) {
            this.mTouchInteractiveUnavailable = true;
            this.mMiuiInputManagerInternal.setDimState(true);
        } else if (!isDimming && this.mTouchInteractiveUnavailable) {
            this.mTouchInteractiveUnavailable = false;
            this.mMiuiInputManagerInternal.setDimState(false);
        }
    }

    private int[] getRealOwners(PowerManagerService.WakeLock wakeLock) {
        int[] iArr = new int[0];
        if (wakeLock.mWorkSource == null) {
            return new int[]{wakeLock.mOwnerUid};
        }
        int N = wakeLock.mWorkSource.size();
        int[] realOwners = new int[N];
        for (int i = 0; i < N; i++) {
            realOwners[i] = wakeLock.mWorkSource.get(i);
        }
        return realOwners;
    }

    public int getPartialWakeLockHoldByUid(int uid) {
        int wakeLockNum = 0;
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                WorkSource ws = wakeLock.mWorkSource;
                if (ws != null || wakeLock.mOwnerUid == uid) {
                    if (ws == null || ws.get(0) == uid) {
                        int wakeLockType = wakeLock.mFlags & 65535;
                        if (wakeLockType == 1) {
                            wakeLockNum++;
                        }
                    }
                }
            }
        }
        return wakeLockNum;
    }

    public int getScreenWakeLockHoldByUid(int uid) {
        int wakeLockNum = 0;
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                WorkSource ws = wakeLock.mWorkSource;
                if (ws != null || wakeLock.mOwnerUid == uid) {
                    if (ws == null || ws.get(0) == uid) {
                        int wakeLockType = wakeLock.mFlags & 65535;
                        switch (wakeLockType) {
                            case 6:
                            case 10:
                            case 26:
                                wakeLockNum++;
                                break;
                        }
                    }
                }
            }
        }
        return wakeLockNum;
    }

    public boolean isAllowWakeUpList(int uid) {
        IGreezeManager iGreezeManager = this.mGreezeManager;
        if (iGreezeManager == null) {
            return false;
        }
        try {
            return iGreezeManager.isAllowWakeUpList(uid);
        } catch (Exception e) {
            Slog.e(TAG, "isAllowWakeUpList error ", e);
            return false;
        }
    }

    public boolean isWakelockDisabledByPolicy(PowerManagerService.WakeLock wakeLock) {
        boolean disabled = false;
        int[] realOwners = getRealOwners(wakeLock);
        if (this.mPowerKeeperPolicy == null || this.mGreezeManager == null) {
            return false;
        }
        try {
            for (int realOwner : realOwners) {
                if (this.mPowerKeeperPolicy.isWakelockDisabledByPolicy(wakeLock.mTag, realOwner)) {
                    disabled = true;
                    printPartialWakeLockDisabledIfNeeded(wakeLock, true, "PowerKeeper");
                    break;
                }
                if (UserHandle.isApp(realOwner) && this.mGreezeManager.isUidFrozen(realOwner)) {
                    disabled = true;
                    printPartialWakeLockDisabledIfNeeded(wakeLock, true, "UidFrozen");
                    break;
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "isWakelockDisabledByPolicy error ", e);
        }
        return disabled;
    }

    private boolean setWakeLockDisabledStateLocked(PowerManagerService.WakeLock wakeLock, boolean disabled) {
        if (wakeLock.mDisabled == disabled) {
            return false;
        }
        wakeLock.mDisabled = disabled;
        return true;
    }

    public void updateAllPartialWakeLockDisableState() {
        synchronized (this.mLock) {
            boolean changed = false;
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        boolean disabled = isWakelockDisabledByPolicy(wakeLock);
                        changed |= setWakeLockDisabledStateLocked(wakeLock, disabled);
                        break;
                }
            }
            if (changed) {
                this.mPowerManagerService.setWakeLockDirtyLocked();
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void setUidPartialWakeLockDisabledState(int uid, String tag, boolean disabled, String reason) {
        synchronized (this.mLock) {
            boolean ret = false;
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                boolean changed = false;
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        int[] realOwners = getRealOwners(wakeLock);
                        for (int realOwner : realOwners) {
                            if (realOwner == uid && ((tag == null || tag.equals(wakeLock.mTag)) && wakeLock.mDisabled != disabled)) {
                                printPartialWakeLockDisabledIfNeeded(wakeLock, disabled, reason);
                                wakeLock.mDisabled = disabled;
                                changed = true;
                            }
                        }
                        break;
                }
                if (changed) {
                    if (wakeLock.mDisabled) {
                        this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
                    } else {
                        this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                    }
                    ret = true;
                }
            }
            if (ret) {
                this.mPowerManagerService.setWakeLockDirtyLocked();
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void setUidPartialWakeLockDisabledState(int uid, String tag, boolean disabled) {
        if (tag == null && !UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("can not disable all wakelock for uid " + uid);
        }
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                boolean changed = false;
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        int[] realOwners = getRealOwners(wakeLock);
                        for (int realOwner : realOwners) {
                            if (realOwner == uid && (tag == null || tag.equals(wakeLock.mTag))) {
                                changed = setWakeLockDisabledStateLocked(wakeLock, disabled);
                                break;
                            }
                        }
                        break;
                }
                if (changed) {
                    if (wakeLock.mDisabled) {
                        this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
                    } else {
                        this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                    }
                    printPartialWakeLockDisabledIfNeeded(wakeLock, disabled, "unknown");
                    this.mPowerManagerService.setWakeLockDirtyLocked();
                    this.mPowerManagerService.updatePowerStateLocked();
                }
            }
        }
    }

    public boolean isShutdownOrRebootPermitted(boolean shutdown, boolean confirm, String reason, boolean wait) {
        if (shutdown && this.isDeviceLock) {
            Handler h = UiThread.getHandler();
            if (h != null) {
                h.post(new Runnable() { // from class: com.android.server.power.PowerManagerServiceImpl.4
                    @Override // java.lang.Runnable
                    public void run() {
                        Toast.makeText(PowerManagerServiceImpl.this.mContext, 286196379, 1).show();
                    }
                });
                return false;
            }
            return false;
        }
        return true;
    }

    public void recordShutDownTime() {
        File last_utime = new File("/cache/recovery/last_utime");
        if (!last_utime.exists()) {
            Slog.e(TAG, "last_utime doesn't exist");
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(last_utime));
            String str = reader.readLine();
            if (str != null) {
                long usrConfirmTime = Long.parseLong(str);
                if (usrConfirmTime <= 0) {
                    Slog.e(TAG, "last_utime has invalid content");
                    reader.close();
                    return;
                }
                reader.close();
                File last_ShutdownTime = new File("/cache/recovery/last_shutdowntime");
                BufferedWriter writer = new BufferedWriter(new FileWriter(last_ShutdownTime));
                String buf = Long.toString(System.currentTimeMillis());
                writer.write(buf);
                writer.flush();
                writer.close();
                if (!last_ShutdownTime.setReadable(true, false)) {
                    last_ShutdownTime.delete();
                    Slog.e(TAG, "set last_shutdowntime readable failed");
                    return;
                }
                return;
            }
            Slog.e(TAG, "last_utime is blank");
            reader.close();
        } catch (IOException ex) {
            Slog.e(TAG, ex.getMessage());
        }
    }

    private void resetScreenProjectionSettingsLocked() {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_in_screening", 0);
        setHangUpModeLocked(false);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "synergy_mode", 0);
    }

    private void setHangUpModeLocked(boolean z) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_hang_up_on", z ? 1 : 0);
    }

    public boolean hangUpNoUpdateLocked(boolean hangUp) {
        if (!this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        if (hangUp && this.mWakefulness == 4) {
            return false;
        }
        if (!hangUp && this.mWakefulness != 4) {
            return false;
        }
        Slog.d(TAG, "hangUpNoUpdateLocked: " + (hangUp ? "enter" : "exit") + " hang up mode...");
        long eventTime = SystemClock.uptimeMillis();
        if (!hangUp) {
            updateUserActivityLocked();
        }
        this.mPowerManagerService.setWakefulnessLocked(0, hangUp ? 4 : 1, eventTime, 1000, 0, 0, (String) null, "hangup");
        this.mNotifierInjector.onWakefulnessInHangUp(hangUp, this.mWakefulness);
        return true;
    }

    public void setBootPhase(int phase) {
        if (phase == 1000) {
            this.mBootCompleted = true;
            this.mHandler.post(new Runnable() { // from class: com.android.server.power.PowerManagerServiceImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiPowerStatisticTracker.getInstance().bootCompleted();
                }
            });
        }
    }

    public boolean shouldRequestDisplayHangupLocked() {
        boolean z = this.mSynergyModeEnable;
        if (!z || this.mPreWakefulness != 4 || this.mWakefulness != 1 || this.mIsKeyguardHasShownWhenExitHangup) {
            return (this.mScreenProjectionEnabled || z) && this.mWakefulness == 4;
        }
        if (WindowManagerServiceStub.get().isKeyGuardShowing()) {
            this.mIsKeyguardHasShownWhenExitHangup = true;
        }
        return true;
    }

    public boolean isInHangUpState() {
        return (this.mScreenProjectionEnabled || this.mSynergyModeEnable) && this.mWakefulness == 4;
    }

    public boolean isSituatedDimmingDueToSynergy() {
        return (this.mScreenProjectionEnabled || this.mSynergyModeEnable) && this.mSituatedDimmingDueToSynergy;
    }

    public boolean exitHangupWakefulnessLocked() {
        return this.mPreWakefulness == 4 && this.mWakefulness != 4;
    }

    protected void finishWakefulnessChangeIfNeededLocked() {
        if ((!this.mScreenProjectionEnabled && !this.mHangUpEnabled && !this.mSynergyModeEnable) || this.mWakefulness != 4) {
            this.mNotifierInjector.onWakefulnessInHangUp(false, this.mWakefulness);
        }
    }

    public boolean shouldEnterHangupLocked() {
        return this.mScreenProjectionEnabled || this.mSynergyModeEnable;
    }

    protected long shouldOverrideUserActivityTimeoutLocked(long timeout, long maximumScreenDimDurationConfig, float maximumScreenDimRatioConfig) {
        if (this.mSynergyModeEnable) {
            long j = this.mUserActivityTimeoutOverrideFromMirrorManager;
            if (j >= 0) {
                timeout = Math.min(timeout, j);
            }
        }
        if (this.mSituatedDimmingDueToAttention) {
            return Math.min(maximumScreenDimDurationConfig, ((float) timeout) * maximumScreenDimRatioConfig);
        }
        return timeout;
    }

    private boolean updateScreenProjectionLocked() {
        boolean screenProjectingEnabled = Settings.Secure.getInt(this.mResolver, "screen_project_in_screening", 0) == 1;
        boolean hangUpEnabled = Settings.Secure.getInt(this.mResolver, "screen_project_hang_up_on", 0) == 1;
        boolean synergyModeEnable = Settings.Secure.getInt(this.mResolver, "synergy_mode", 0) == 1;
        if (screenProjectingEnabled == this.mScreenProjectionEnabled && hangUpEnabled == this.mHangUpEnabled && synergyModeEnable == this.mSynergyModeEnable) {
            return false;
        }
        if (!hangUpEnabled && this.mHangUpEnabled) {
            this.mHangUpEnabled = false;
            return true;
        }
        this.mScreenProjectionEnabled = screenProjectingEnabled;
        this.mHangUpEnabled = hangUpEnabled;
        this.mSynergyModeEnable = synergyModeEnable;
        if ((screenProjectingEnabled || synergyModeEnable) && hangUpEnabled) {
            hangUpNoUpdateLocked(true);
        } else if (!screenProjectingEnabled && !synergyModeEnable) {
            hangUpNoUpdateLocked(false);
        }
        if (this.mHangUpEnabled) {
            setHangUpModeLocked(false);
        }
        return true;
    }

    protected int adjustWakeLockDueToHangUpLocked(int wakeLockSummary) {
        this.mIsBrightWakeLock = false;
        if ((wakeLockSummary & 38) != 0 && (this.mScreenProjectionEnabled || this.mSynergyModeEnable)) {
            return wakeLockSummary & (-39);
        }
        if (this.mDimEnable && (wakeLockSummary & 2) != 0) {
            this.mIsBrightWakeLock = true;
            return (wakeLockSummary & (-3)) | 4;
        }
        return wakeLockSummary;
    }

    public void updateWakefulnessLocked(int wakefulness) {
        this.mPreWakefulness = this.mWakefulness;
        this.mWakefulness = wakefulness;
        MiuiAttentionDetector miuiAttentionDetector = this.mAttentionDetector;
        if (miuiAttentionDetector != null) {
            miuiAttentionDetector.notifyInteractiveChange(PowerManagerInternal.isInteractive(wakefulness));
        }
    }

    public void requestDimmingRightNowDueToAttention() {
        synchronized (this.mLock) {
            boolean isDimming = (this.mPowerGroups.get(0).getUserActivitySummaryLocked() & 2) != 0;
            if (!this.mSituatedDimmingDueToAttention && !isDimming) {
                updateUserActivityLocked();
                this.mLastRequestDimmingTime = SystemClock.uptimeMillis();
                this.mSituatedDimmingDueToAttention = true;
                this.mPendingSetDirtyDueToRequestDimming = true;
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    protected void requestDimmingRightNowInternal(long timeMillis) {
        synchronized (this.mLock) {
            if (!this.mSituatedDimmingDueToSynergy) {
                if (DEBUG) {
                    Slog.d(TAG, "requestDimmingRightNowInternal: timeout: " + timeMillis);
                }
                updateUserActivityLocked();
                this.mLastRequestDimmingTime = SystemClock.uptimeMillis();
                this.mSituatedDimmingDueToSynergy = true;
                this.mPendingSetDirtyDueToRequestDimming = true;
                this.mUserActivityTimeoutOverrideFromMirrorManager = timeMillis;
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void clearRequestDimmingParamsLocked() {
        if (this.mSituatedDimmingDueToSynergy) {
            this.mSituatedDimmingDueToSynergy = false;
            this.mUserActivityTimeoutOverrideFromMirrorManager = -1L;
            this.mLastRequestDimmingTime = -1L;
            this.mPendingSetDirtyDueToRequestDimming = false;
        }
        if (this.mSituatedDimmingDueToAttention) {
            this.mSituatedDimmingDueToAttention = false;
            this.mLastRequestDimmingTime = -1L;
            this.mPendingSetDirtyDueToRequestDimming = false;
        }
    }

    protected long updateNextDimTimeoutIfNeededLocked(long nextTimeout, long lastUserActivityTime) {
        if (this.mSynergyModeEnable && this.mSituatedDimmingDueToSynergy && this.mUserActivityTimeoutOverrideFromMirrorManager >= 0 && this.mLastRequestDimmingTime > 0) {
            return this.mLastRequestDimmingTime;
        }
        if (this.mDimEnable && this.mIsBrightWakeLock) {
            return lastUserActivityTime + this.mBrightWaitTime;
        }
        if (!this.mSituatedDimmingDueToAttention) {
            return nextTimeout;
        }
        long j = this.mLastRequestDimmingTime;
        if (j > 0) {
            return Math.min(nextTimeout, j);
        }
        return nextTimeout;
    }

    protected void updateUserActivityLocked() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
    }

    protected int adjustDirtyIfNeededLocked(int mDirty) {
        if (this.mSynergyModeEnable && this.mSituatedDimmingDueToSynergy && this.mPendingSetDirtyDueToRequestDimming) {
            mDirty |= 32;
            this.mPendingSetDirtyDueToRequestDimming = false;
        }
        if (this.mPendingStopBrightnessBoost) {
            mDirty |= 2048;
        }
        if (this.mSituatedDimmingDueToAttention && this.mPendingSetDirtyDueToRequestDimming) {
            int mDirty2 = mDirty | 32;
            this.mPendingSetDirtyDueToRequestDimming = false;
            return mDirty2;
        }
        return mDirty;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleSettingsChangedLocked(Uri uri) {
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        switch (lastPathSegment.hashCode()) {
            case -1848054051:
                if (lastPathSegment.equals(SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1475410818:
                if (lastPathSegment.equals(KEY_DEVICE_LOCK)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1438362181:
                if (lastPathSegment.equals("synergy_mode")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1187891250:
                if (lastPathSegment.equals("adaptive_sleep")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -974080081:
                if (lastPathSegment.equals("user_setup_complete")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -901388401:
                if (lastPathSegment.equals("screen_project_hang_up_on")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -889914649:
                if (lastPathSegment.equals("pick_up_gesture_wakeup_mode")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -445165996:
                if (lastPathSegment.equals(SUBSCREEN_SUPER_POWER_SAVE_MODE)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 81099118:
                if (lastPathSegment.equals("accelerometer_rotation")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1404599639:
                if (lastPathSegment.equals("gaze_lock_screen_setting")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 1455506878:
                if (lastPathSegment.equals(POWER_SAVE_MODE_OPEN)) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1497178783:
                if (lastPathSegment.equals("drive_mode_drive_mode")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 2030129845:
                if (lastPathSegment.equals("screen_project_in_screening")) {
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
            case 1:
            case 2:
                if (updateScreenProjectionLocked()) {
                    this.mPowerManagerService.updatePowerStateLocked();
                    return;
                }
                return;
            case 3:
                this.mNotifierInjector.updateAccelerometerRotationLocked();
                return;
            case 4:
                updateDriveModeLocked();
                return;
            case 5:
                updateDeviceLockState();
                return;
            case 6:
                updateSecondaryDisplayScreenOffTimeoutLocked();
                return;
            case 7:
                updateUserSetupCompleteLocked();
                return;
            case '\b':
                updateSubScreenSuperPowerSaveMode();
                return;
            case '\t':
                updatePickUpGestureWakeUpModeLocked();
                return;
            case '\n':
                updateAdaptiveSleepLocked();
                return;
            case 11:
                updateAonScreenOffConfig();
                return;
            case '\f':
                updatePowerSaveMode();
                this.mPowerManagerService.updatePowerStateLocked();
                return;
            default:
                return;
        }
    }

    public void onUserSwitching() {
        updateAonScreenOffConfig();
    }

    public void updateDimState(boolean dimEnable, long brightWaitTime) {
        this.mIsBrightWakeLock = false;
        this.mDimEnable = dimEnable;
        this.mBrightWaitTime = brightWaitTime;
        synchronized (this.mLock) {
            this.mPowerManagerService.updatePowerStateLocked();
        }
    }

    protected void sendBroadcastRestoreBrightnessIfNeededLocked() {
        if (this.mDimEnable && this.mIsBrightWakeLock) {
            this.mNotifierInjector.sendBroadcastRestoreBrightness();
        }
    }

    private void updateDeviceLockState() {
        this.isDeviceLock = Settings.Global.getInt(this.mResolver, KEY_DEVICE_LOCK, 0) != 0;
    }

    private void requestHangUpWhenScreenProjectInternal(IBinder token, boolean hangup) {
        synchronized (this.mLock) {
            setDeathCallbackLocked(token, 0, hangup);
            setHangUpModeLocked(hangup);
        }
    }

    private void setDeathCallbackLocked(IBinder token, int flag, boolean register) {
        synchronized (this.mLock) {
            if (register) {
                registerDeathCallbackLocked(token, flag);
            } else {
                unregisterDeathCallbackLocked(token);
            }
        }
    }

    protected void registerDeathCallbackLocked(IBinder token, int flag) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.d(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token, flag));
        }
    }

    protected void registerDeathCallbackLocked(IBinder token) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.d(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(this, token));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        ClientDeathCallback deathCallback;
        if (token != null && (deathCallback = this.mClientDeathCallbacks.remove(token)) != null) {
            token.unlinkToDeath(deathCallback, 0);
        }
    }

    private void doDieLocked() {
        clearRequestDimmingParamsLocked();
        resetScreenProjectionSettingsLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doDieLocked(int flag) {
        if ((flag & 1) != 0 && this.mAlwaysWakeUp) {
            this.mAlwaysWakeUp = false;
            this.mPowerManagerService.updatePowerStateLocked();
        } else if (flag == 0) {
            clearRequestDimmingParamsLocked();
            resetScreenProjectionSettingsLocked();
        } else if (flag == 2) {
            stopBoostingBrightnessLocked(1000);
        }
    }

    protected void requestDimmingRightNow(long timeMillis) {
        long ident = Binder.clearCallingIdentity();
        if (timeMillis > 0) {
            try {
                requestDimmingRightNowInternal(timeMillis);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void requestHangUpWhenScreenProject(IBinder token, boolean hangup) {
        long ident = Binder.clearCallingIdentity();
        if (token != null) {
            try {
                requestHangUpWhenScreenProjectInternal(token, hangup);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void boostScreenBrightnessInternal(IBinder token, boolean enabled, int uid) {
        synchronized (this.mLock) {
            if (this.mBrightnessBoostForSoftLightInProgress != enabled) {
                setDeathCallbackLocked(token, 2, enabled);
                this.mBrightnessBoostForSoftLightInProgress = enabled;
                if (enabled) {
                    Slog.d(TAG, "Start boosting screen brightness: uid = " + uid);
                    this.mPowerManager.boostScreenBrightness(SystemClock.uptimeMillis());
                } else {
                    stopBoostingBrightnessLocked(uid);
                }
            }
        }
    }

    private void stopBoostingBrightnessLocked(int uid) {
        Slog.d(TAG, "stop boosting screen brightness: uid = " + uid);
        if (this.mBrightnessBoostForSoftLightInProgress) {
            this.mBrightnessBoostForSoftLightInProgress = false;
        }
        this.mPendingStopBrightnessBoost = true;
        this.mPowerManagerService.updatePowerStateLocked();
    }

    /* loaded from: classes.dex */
    private class PowerManagerStubHandler extends Handler {
        public PowerManagerStubHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PowerManagerServiceImpl.this.updateForegroundAppPackageName();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private int mFlag;
        private IBinder mToken;

        public ClientDeathCallback(PowerManagerServiceImpl this$0, IBinder token) {
            this(token, 0);
        }

        public ClientDeathCallback(IBinder token, int flag) {
            this.mToken = token;
            this.mFlag = flag;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(PowerManagerServiceImpl.TAG, "binderDied: flag: " + this.mFlag);
            synchronized (PowerManagerServiceImpl.this.mLock) {
                PowerManagerServiceImpl.this.doDieLocked(this.mFlag);
            }
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
            synchronized (PowerManagerServiceImpl.this.mLock) {
                PowerManagerServiceImpl.this.handleSettingsChangedLocked(uri);
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        try {
            switch (code) {
                case 16777207:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token3 = data.readStrongBinder();
                    boolean enabled = data.readBoolean();
                    boostScreenBrightness(token3, enabled);
                    return true;
                case 16777208:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token2 = data.readStrongBinder();
                    int flag2 = data.readInt();
                    clearWakeUpFlags(token2, flag2);
                    return true;
                case 16777209:
                    data.enforceInterface("android.os.IPowerManager");
                    reply.writeInt(getSecondaryDisplayWakefulnessLocked());
                    return true;
                case 16777210:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token1 = data.readStrongBinder();
                    long wakeUpTime1 = data.readLong();
                    int flag = data.readInt();
                    String wakeUpReason1 = data.readString();
                    wakeUpSecondaryDisplay(token1, wakeUpTime1, flag, wakeUpReason1);
                    return true;
                case DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY /* 16777211 */:
                    data.enforceInterface("android.os.IPowerManager");
                    long goToSleepTime = data.readLong();
                    String goToSleepReason = data.readString();
                    goToSleepSecondaryDisplay(goToSleepTime, goToSleepReason);
                    return true;
                case DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY /* 16777212 */:
                    data.enforceInterface("android.os.IPowerManager");
                    long wakeUpTime = data.readLong();
                    String wakeUpReason = data.readString();
                    wakeUpSecondaryDisplay(wakeUpTime, wakeUpReason);
                    return true;
                case 16777213:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token = data.readStrongBinder();
                    boolean hangup = data.readBoolean();
                    requestHangUpWhenScreenProject(token, hangup);
                    return true;
                case 16777214:
                    data.enforceInterface("android.os.IPowerManager");
                    long duration = data.readLong();
                    requestDimmingRightNow(duration);
                    return true;
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private void boostScreenBrightness(IBinder token, boolean enabled) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            boostScreenBrightnessInternal(token, enabled, uid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void wakeUpSecondaryDisplay(long eventTime, String reason) {
        wakeUpSecondaryDisplay(null, eventTime, 0, reason);
    }

    private void wakeUpSecondaryDisplay(IBinder token, long eventTime, int flag, String reason) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    if (eventTime >= this.mLastSecondaryDisplayWakeUpTime) {
                        if ((getSecondaryDisplayWakefulnessLocked() != 1 || REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL.equals(reason)) && this.mSystemReady) {
                            this.mLastSecondaryDisplayWakeUpTime = eventTime;
                            this.mLastSecondaryDisplayUserActivityTime = eventTime;
                            this.mLastSecondaryDisplayWakeUpReason = reason;
                            updateAlwaysWakeUpIfNeededLocked(token, flag);
                            Slog.d(TAG, "Waking up secondary display from: " + uid + ", reason: " + reason);
                            this.mPowerManagerService.wakeUpSecondaryDisplay(this.mPowerGroups.get(1), eventTime, 0, "sub-display", uid);
                            return;
                        }
                    }
                    Slog.d(TAG, "wakeUpSecondaryDisplay: return");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void goToSleepSecondaryDisplay(long eventTime, String reason) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    if (eventTime >= this.mLastSecondaryDisplayWakeUpTime && PowerManagerInternal.isInteractive(getSecondaryDisplayWakefulnessLocked()) && this.mSystemReady && this.mBootCompleted) {
                        if (shouldGoToSleepWhileAlwaysOn(reason)) {
                            Slog.d(TAG, "Ignore " + reason + " to apply secondary display sleep while mAlwaysWakeUp");
                            return;
                        }
                        this.mLastSecondaryDisplayGoToSleepTime = eventTime;
                        this.mLastSecondaryDisplayGoToSleepReason = reason;
                        this.mAlwaysWakeUp = false;
                        Slog.d(TAG, "Going to sleep secondary display from: " + uid + ", reason: " + reason);
                        this.mPowerManagerService.goToSleepSecondaryDisplay(this.mPowerGroups.get(1), eventTime, 0, uid);
                        return;
                    }
                    Slog.d(TAG, "goToSleepSecondaryDisplay: return");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void clearWakeUpFlags(IBinder token, int flag) {
        long ident = Binder.clearCallingIdentity();
        try {
            clearWakeUpFlagsInternal(token, flag);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void updateAlwaysWakeUpIfNeededLocked(IBinder token, int flag) {
        if (token != null && (flag & 1) != 0) {
            this.mAlwaysWakeUp = true;
            setDeathCallbackLocked(token, flag, true);
        }
    }

    public boolean isSubScreenSuperPowerSaveModeOpen() {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && this.mSubScreenSuperPowerSaveMode != 0) {
            Slog.i(TAG, "wilderness subscreen_super_power_save_mode open");
            return true;
        }
        return false;
    }

    private void updateSubScreenSuperPowerSaveMode() {
        this.mSubScreenSuperPowerSaveMode = Settings.System.getIntForUser(this.mResolver, SUBSCREEN_SUPER_POWER_SAVE_MODE, 0, -2);
    }

    private void updateSecondaryDisplayScreenOffTimeoutLocked() {
        this.mSecondaryDisplayScreenOffTimeout = Settings.System.getInt(this.mResolver, SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY, 15000);
    }

    private void updateUserSetupCompleteLocked() {
        this.mIsUserSetupComplete = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    protected int getSecondaryDisplayWakefulnessLocked() {
        return this.mPowerGroups.get(1).getWakefulnessLocked();
    }

    private boolean shouldGoToSleepWhileAlwaysOn(String reason) {
        return this.mAlwaysWakeUp && !REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL.equals(reason);
    }

    private void clearWakeUpFlagsInternal(IBinder token, int flag) {
        synchronized (this.mLock) {
            if (DeviceFeature.IS_SUBSCREEN_DEVICE && (flag & 1) != 0) {
                this.mAlwaysWakeUp = false;
                unregisterDeathCallbackLocked(token);
                long now = SystemClock.uptimeMillis();
                this.mPowerManagerService.userActivitySecondaryDisplay(2, now, 0, 0, 1000);
            }
        }
    }

    private void updatePowerSaveMode() {
        boolean isPowerSaveModeEnabled = Settings.System.getIntForUser(this.mResolver, POWER_SAVE_MODE_OPEN, 0, -2) != 0;
        this.mIsPowerSaveModeEnabled = isPowerSaveModeEnabled;
        Slog.i(TAG, "updatePowerSaveMode: mIsPowerSaveModeEnabled: " + this.mIsPowerSaveModeEnabled);
    }

    public void updateLowPowerModeIfNeeded(DisplayManagerInternal.DisplayPowerRequest displayPowerRequest) {
        if (!this.mIsPowerSaveModeEnabled) {
            return;
        }
        displayPowerRequest.lowPowerMode = true;
        displayPowerRequest.screenLowPowerBrightnessFactor = mBrightnessDecreaseRatio;
    }

    public long adjustScreenOffTimeoutIfNeededLocked(PowerGroup powerGroup, long timeout) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && powerGroup.getGroupId() == 1) {
            return this.mSecondaryDisplayScreenOffTimeout;
        }
        return timeout;
    }

    public boolean shouldAlwaysWakeUpSecondaryDisplay() {
        return this.mAlwaysWakeUp;
    }

    public long getDimDurationExtraTime(long extraTimeMillis) {
        if (this.mDriveMode != 1 || extraTimeMillis <= 0) {
            return 0L;
        }
        return extraTimeMillis;
    }

    public boolean isBrightnessBoostForSoftLightInProgress() {
        return this.mBrightnessBoostForSoftLightInProgress;
    }

    public boolean isPendingStopBrightnessBoost() {
        return this.mPendingStopBrightnessBoost;
    }

    public void resetBoostBrightnessIfNeededLocked() {
        if (this.mPendingStopBrightnessBoost) {
            this.mPendingStopBrightnessBoost = false;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x002d, code lost:
    
        r3 = r6.processName;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.String reassignRebootReason(int r9, int r10) {
        /*
            r8 = this;
            r0 = 0
            java.lang.String r1 = "Unexpected reboot or shutdown reason is null, the reason will be reassign by uid and processname"
            java.lang.String r2 = "PowerManagerServiceImpl"
            android.util.Slog.w(r2, r1)
            android.content.Context r1 = r8.mContext
            java.lang.String r3 = "activity"
            java.lang.Object r1 = r1.getSystemService(r3)
            android.app.ActivityManager r1 = (android.app.ActivityManager) r1
            r3 = 0
            java.util.List r4 = r1.getRunningAppProcesses()     // Catch: java.lang.Exception -> L31
            java.util.Iterator r5 = r4.iterator()     // Catch: java.lang.Exception -> L31
        L1b:
            boolean r6 = r5.hasNext()     // Catch: java.lang.Exception -> L31
            if (r6 == 0) goto L30
            java.lang.Object r6 = r5.next()     // Catch: java.lang.Exception -> L31
            android.app.ActivityManager$RunningAppProcessInfo r6 = (android.app.ActivityManager.RunningAppProcessInfo) r6     // Catch: java.lang.Exception -> L31
            int r7 = r6.pid     // Catch: java.lang.Exception -> L31
            if (r7 != r10) goto L2f
            java.lang.String r2 = r6.processName     // Catch: java.lang.Exception -> L31
            r3 = r2
            goto L30
        L2f:
            goto L1b
        L30:
            goto L37
        L31:
            r4 = move-exception
            java.lang.String r5 = "Failed to get running app processes from ActivityManager"
            android.util.Slog.e(r2, r5, r4)
        L37:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r9)
            java.lang.String r4 = ","
            java.lang.StringBuilder r2 = r2.append(r4)
            if (r3 != 0) goto L4d
            java.lang.Integer r4 = java.lang.Integer.valueOf(r10)
            goto L4e
        L4d:
            r4 = r3
        L4e:
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.String r0 = r2.toString()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.power.PowerManagerServiceImpl.reassignRebootReason(int, int):java.lang.String");
    }

    public void lockNowIfNeededLocked(int reason) {
        if (this.mSynergyModeEnable && reason == 1 && this.mWakefulness == 4 && !WindowManagerServiceStub.get().isKeyGuardShowing()) {
            this.mPolicy.lockNow((Bundle) null);
            this.mIsKeyguardHasShownWhenExitHangup = false;
        }
    }

    public boolean isAcquiredScreenBrightWakeLock(int groupId) {
        boolean screenBrightWakeLock;
        synchronized (this.mLock) {
            int wakeLockSummary = this.mPowerGroups.get(groupId).getWakeLockSummaryLocked();
            screenBrightWakeLock = (wakeLockSummary & 6) != 0;
        }
        return screenBrightWakeLock;
    }

    public long getDefaultDisplayLastUserActivity() {
        long lastUserActivityTimeLocked;
        synchronized (this.mLock) {
            lastUserActivityTimeLocked = this.mPowerGroups.get(0).getLastUserActivityTimeLocked();
        }
        return lastUserActivityTimeLocked;
    }

    public void notifyWakefulnessChangedLocked(int groupId, int wakefulness, long eventTime, int reason, String details) {
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = this.mMiuiPowerStatisticTracker;
        if (miuiPowerStatisticTracker == null || groupId != 0) {
            return;
        }
        miuiPowerStatisticTracker.notifyWakefulnessChangedLocked(wakefulness, eventTime, reason);
        WindowManagerServiceStub.get().trackScreenData(wakefulness, details);
    }

    public void notifyWakefulnessCompletedLocked(int groupId) {
        if (this.mMiuiPowerStatisticTracker == null || groupId != 0) {
            return;
        }
        long wakefulnessCompletedTime = SystemClock.uptimeMillis();
        this.mMiuiPowerStatisticTracker.notifyWakefulnessCompletedLocked(wakefulnessCompletedTime);
    }

    public void notifyScreenOnUnBlocker(int displayId, long screenOnBlockStartRealTime, int delayMiles) {
        if (this.mMiuiPowerStatisticTracker == null || displayId != 0) {
            return;
        }
        int delay = (int) (SystemClock.elapsedRealtime() - screenOnBlockStartRealTime);
        this.mMiuiPowerStatisticTracker.notifyScreenOnUnBlocker(delay, delayMiles);
    }

    public void notifyDisplayStateChangeStartLocked(boolean isFirstDisplay) {
        if (!isFirstDisplay) {
            return;
        }
        this.mDisplayStateChangeStateTime = SystemClock.uptimeMillis();
    }

    public void notifyDisplayStateChangeEndLocked(boolean isFirstDisplay) {
        if (this.mMiuiPowerStatisticTracker == null || !isFirstDisplay) {
            return;
        }
        int latencyMs = (int) (SystemClock.uptimeMillis() - this.mDisplayStateChangeStateTime);
        this.mMiuiPowerStatisticTracker.notifyDisplayStateChangedLatencyLocked(latencyMs);
    }

    public void notifyUserAttentionChanged(long startTimeStamp, int result) {
        if (this.mMiuiPowerStatisticTracker == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        int latencyMs = (int) (now - startTimeStamp);
        this.mMiuiPowerStatisticTracker.notifyUserAttentionChanged(latencyMs, result);
    }

    public void notifyTofPowerState(boolean wakeup) {
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = this.mMiuiPowerStatisticTracker;
        if (miuiPowerStatisticTracker == null) {
            return;
        }
        miuiPowerStatisticTracker.notifyTofPowerState(wakeup);
    }

    public void notifyGestureEvent(String pkg, boolean success, int label) {
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = this.mMiuiPowerStatisticTracker;
        if (miuiPowerStatisticTracker == null) {
            return;
        }
        miuiPowerStatisticTracker.notifyGestureEvent(pkg, success, label);
    }

    public void notifyAonScreenOnOffEvent(boolean isScreenOn, boolean hasFace, long startTime) {
        if (this.mMiuiPowerStatisticTracker == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long latencyMs = now - startTime;
        this.mMiuiPowerStatisticTracker.notifyAonScreenOnOffEvent(isScreenOn, hasFace, latencyMs);
    }

    public void notifyDisplayPortConnectStateChanged(long physicalDisplayId, boolean isConnected, String productName, int frameRate, String resolution) {
        MiuiPowerStatisticTracker miuiPowerStatisticTracker = this.mMiuiPowerStatisticTracker;
        if (miuiPowerStatisticTracker == null) {
            return;
        }
        miuiPowerStatisticTracker.notifyDisplayPortConnectStateChanged(physicalDisplayId, isConnected, productName, frameRate, resolution);
    }

    public void notifyUserActivityTimeChanged() {
        MiuiAttentionDetector miuiAttentionDetector = this.mAttentionDetector;
        if (miuiAttentionDetector != null) {
            miuiAttentionDetector.onUserActivityChanged();
        }
    }

    public void notifyStayOnChanged(boolean stayOn) {
        MiuiAttentionDetector miuiAttentionDetector = this.mAttentionDetector;
        if (miuiAttentionDetector != null) {
            miuiAttentionDetector.notifyStayOnChanged(stayOn);
        }
    }

    public boolean recalculateGlobalWakefulnessSkipPowerGroup(PowerGroup powerGroup) {
        return powerGroup.getGroupId() != 0;
    }

    public boolean shouldWakeUpWhenPluggedOrUnpluggedSkipByMiui() {
        return true;
    }

    public void notifyDimStateChanged(int pendingRequestPolicy, int requestPolicy) {
        if (this.mMiuiPowerStatisticTracker == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (2 == requestPolicy || 2 == pendingRequestPolicy) {
            boolean isEnterDim = 2 == requestPolicy;
            this.mMiuiPowerStatisticTracker.notifyDimStateChanged(now, isEnterDim);
        }
    }

    public void updateBlockUntilScreenOnReady(PowerGroup powerGroup, DisplayManagerInternal.DisplayPowerRequest displayPowerRequest) {
        displayPowerRequest.setBrightnessUntilScreenOnReady = powerGroup.getGroupId() == 0 && powerGroup.isPoweringOnLocked() && displayPowerRequest.policy == 3 && !(SUPPORT_FOD && SKIP_TRANSITION_WAKE_UP_DETAILS.contains(powerGroup.getLastWakeReasonDetails()));
    }

    public void dumpLocal(PrintWriter pw) {
        DEBUG = PowerDebugConfig.DEBUG_PMS;
        pw.println("mScreenProjectionEnabled: " + this.mScreenProjectionEnabled);
        pw.println("mHangUpEnabled: " + this.mHangUpEnabled);
        pw.println("mSynergyModeEnable: " + this.mSynergyModeEnable);
        pw.println("mIsKeyguardHasShownWhenExitHangup: " + this.mIsKeyguardHasShownWhenExitHangup);
        pw.println("mSituatedDimmingDueToSynergy: " + this.mSituatedDimmingDueToSynergy);
        pw.println("mLastRequestDimmingTime: " + this.mLastRequestDimmingTime);
        pw.println("mPendingSetDirtyDueToRequestDimming: " + this.mPendingSetDirtyDueToRequestDimming);
        pw.println("mUserActivityTimeoutOverrideFromMirrorManager: " + this.mUserActivityTimeoutOverrideFromMirrorManager);
        pw.println("mSituatedDimmingDueToAttention: " + this.mSituatedDimmingDueToAttention);
        pw.println("mIsPowerSaveModeEnabled: " + this.mIsPowerSaveModeEnabled);
        pw.println("mTouchInteractiveUnavailable: " + this.mTouchInteractiveUnavailable);
        this.mNotifierInjector.dump(pw);
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            pw.println();
            pw.println("Secondary display power state: ");
            long now = SystemClock.uptimeMillis();
            long lastUserActivityDuration = now - this.mLastSecondaryDisplayUserActivityTime;
            long lastGoToSleepDuration = now - this.mLastSecondaryDisplayGoToSleepTime;
            long lastWakeUpDuration = now - this.mLastSecondaryDisplayWakeUpTime;
            pw.println("    mLastSecondaryDisplayUserActivityTime: " + TimeUtils.formatDuration(lastUserActivityDuration) + " ago.");
            pw.println("    mLastSecondaryDisplayWakeUpTime: " + TimeUtils.formatDuration(lastWakeUpDuration) + " ago.");
            pw.println("    mLastSecondaryDisplayGoToSleepTime: " + TimeUtils.formatDuration(lastGoToSleepDuration) + " ago.");
            pw.println("    mUserActivitySecondaryDisplaySummary: 0x" + Integer.toHexString(this.mUserActivitySecondaryDisplaySummary));
            pw.println("    mLastSecondaryDisplayWakeUpReason: " + this.mLastSecondaryDisplayWakeUpReason);
            pw.println("    mLastSecondaryDisplayGoToSleepReason: " + this.mLastSecondaryDisplayGoToSleepReason);
            pw.println("    mSecondaryDisplayScreenOffTimeout: " + this.mSecondaryDisplayScreenOffTimeout + " ms.");
            pw.println("    mAlwaysWakeUp: " + this.mAlwaysWakeUp);
        }
        if (OPTIMIZE_WAKELOCK_ENABLED) {
            pw.println("");
            pw.println("mIsFreeFormOrSplitWindowMode: " + this.mIsFreeFormOrSplitWindowMode);
        }
        MiuiAttentionDetector miuiAttentionDetector = this.mAttentionDetector;
        if (miuiAttentionDetector != null) {
            miuiAttentionDetector.dump(pw);
        }
    }

    public void updatePowerGroupPolicy(int groupId, int policy) {
        if (groupId == 0) {
            boolean isDimming = 2 == policy;
            setPickUpSensorEnable(isDimming);
            setTouchInteractive(isDimming);
        }
    }
}
