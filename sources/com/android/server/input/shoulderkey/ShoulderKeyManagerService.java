package com.android.server.input.shoulderkey;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.KeyguardManager;
import android.app.TaskStackListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.SystemService;
import com.android.server.am.ProcessUtils;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.shoulderkey.ShoulderKeyManagerService;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import miui.hardware.shoulderkey.IShoulderKeyManager;
import miui.hardware.shoulderkey.ITouchMotionEventListener;
import miui.hardware.shoulderkey.ShoulderKey;
import miui.hardware.shoulderkey.ShoulderKeyManager;
import miui.hardware.shoulderkey.ShoulderKeyMap;
import miui.os.Build;

/* loaded from: classes.dex */
public class ShoulderKeyManagerService extends IShoulderKeyManager.Stub {
    private static final long GAMEBOOSTER_DEBOUNCE_DELAY_MILLS = 150;
    private static final String GAME_BOOSTER_SWITCH = "shoulder_quick_star_gameturbo";
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int NONUI_SENSOR_ID = 33171027;
    public static final String SHOULDEKEY_SOUND_TYPE = "shoulderkey_sound_type";
    private static final int SHOULDERKEY_POSITION_LEFT = 0;
    private static final int SHOULDERKEY_POSITION_RIGHT = 1;
    public static final String SHOULDERKEY_SOUND_SWITCH = "shoulderkey_sound_switch";
    private static final String TAG = "ShoulderKeyManager";
    private IActivityTaskManager mActivityTaskManager;
    private boolean mBoosterSwitch;
    private Context mContext;
    private String mCurrentForegroundAppLabel;
    private String mCurrentForegroundPkg;
    private DisplayInfo mDisplayInfo;
    private DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    private long mDownTime;
    private H mHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsGameMode;
    private boolean mIsPocketMode;
    private boolean mIsScreenOn;
    private KeyguardManager mKeyguardManager;
    private boolean mLeftShoulderKeySwitchStatus;
    private boolean mLeftShoulderKeySwitchTriggered;
    private ArrayMap<ShoulderKey, ShoulderKeyMap> mLifeKeyMapper;
    private LocalService mLocalService;
    private final MiuiInputManagerInternal mMiuiInputManagerInternal;
    private MiuiShoulderKeyShortcutListener mMiuiShoulderKeyShortcutListener;
    private Sensor mNonUISensor;
    private PackageManager mPackageManager;
    private boolean mRecordEventStatus;
    private boolean mRegisteredNonUI;
    private boolean mRightShoulderKeySwitchStatus;
    private boolean mRightShoulderKeySwitchTriggered;
    private MiuiSettingsObserver mSettingsObserver;
    private boolean mShoulderKeySoundSwitch;
    private String mShoulderKeySoundType;
    private SensorManager mSm;
    private boolean mSupportShoulderKey;
    private TaskStackListenerImpl mTaskStackListener;
    private boolean DEBUG = false;
    private HashSet<Integer> mInjectEventPids = new HashSet<>();
    private final HashSet<String> mInjcetEeventPackages = new HashSet<>();
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId != 0) {
                return;
            }
            ShoulderKeyManagerService shoulderKeyManagerService = ShoulderKeyManagerService.this;
            shoulderKeyManagerService.mDisplayInfo = shoulderKeyManagerService.mDisplayManagerInternal.getDisplayInfo(0);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }
    };
    private SensorEventListener mNonUIListener = new SensorEventListener() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == ShoulderKeyManagerService.NONUI_SENSOR_ID) {
                if (event.values[0] != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                    ShoulderKeyManagerService.this.mIsPocketMode = true;
                } else if (event.values[0] == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                    ShoulderKeyManagerService.this.mIsPocketMode = false;
                }
                Slog.d(ShoulderKeyManagerService.TAG, "NonUIEventListener onSensorChanged,mIsPocketMode = " + ShoulderKeyManagerService.this.mIsPocketMode);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mTouchMotionEventLock = new Object();
    private final SparseArray<TouchMotionEventListenerRecord> mTouchMotionEventListeners = new SparseArray<>();
    private final List<TouchMotionEventListenerRecord> mTempTouchMotionEventListenersToNotify = new ArrayList();

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final ShoulderKeyManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ShoulderKeyManagerService(context);
        }

        public void onStart() {
            publishBinderService("shoulderkey", this.mService);
        }
    }

    public ShoulderKeyManagerService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("shoulderKey");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(this.mHandlerThread.getLooper());
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        LocalServices.addService(ShoulderKeyManagerInternal.class, localService);
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mLifeKeyMapper = new ArrayMap<>();
        this.mSupportShoulderKey = ShoulderKeyManager.SUPPORT_SHOULDERKEY;
        this.mMiuiShoulderKeyShortcutListener = new MiuiShoulderKeyShortcutListener(this.mContext);
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        init();
    }

    private void init() {
        if (this.mSupportShoulderKey) {
            this.mLeftShoulderKeySwitchStatus = ShoulderKeyUtil.getShoulderKeySwitchStatus(0);
            this.mRightShoulderKeySwitchStatus = ShoulderKeyUtil.getShoulderKeySwitchStatus(1);
        }
        this.mInjectEventPids.clear();
        this.mRecordEventStatus = false;
        this.mInjcetEeventPackages.add("com.xiaomi.macro");
        this.mInjcetEeventPackages.add("com.xiaomi.migameservice");
        this.mInjcetEeventPackages.add("com.xiaomi.joyose");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void systemReadyInternal() {
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(MiuiStylusShortcutManager.SCENE_KEYGUARD);
        registerForegroundAppUpdater();
        if (this.mSupportShoulderKey) {
            SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            this.mSm = sensorManager;
            this.mNonUISensor = sensorManager.getDefaultSensor(NONUI_SENSOR_ID, true);
        }
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
        this.mDisplayManager = displayManager;
        displayManager.registerDisplayListener(this.mDisplayListener, null);
        DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mDisplayInfo = displayManagerInternal.getDisplayInfo(0);
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register foreground app updater: " + e);
        }
    }

    public void loadLiftKeyMap(Map mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("lift key mapper is null");
        }
        if (mapper.size() == 0) {
            throw new IllegalArgumentException("lift key mapper is empty");
        }
        Slog.d(TAG, "loadLiftKeyMap, mapper.size() = " + mapper.size());
        Message msg = this.mHandler.obtainMessage(0);
        msg.obj = mapper;
        this.mHandler.sendMessage(msg);
    }

    public void unloadLiftKeyMap() {
        Slog.d(TAG, "unloadLiftKeyMap");
        this.mHandler.sendEmptyMessage(1);
    }

    public boolean getShoulderKeySwitchStatus(int position) {
        if (position == 0) {
            return this.mLeftShoulderKeySwitchStatus;
        }
        if (position == 1) {
            return this.mRightShoulderKeySwitchStatus;
        }
        return false;
    }

    public void setInjectMotionEventStatus(boolean enable) {
        if (!checkInjectEventsPermission()) {
            Slog.d(TAG, Binder.getCallingPid() + "process Not have INJECT_EVENTS permission!");
            return;
        }
        Slog.d(TAG, ProcessUtils.getProcessNameByPid(Binder.getCallingPid()) + " setInjectMotionEventStatus " + enable);
        int pid = Binder.getCallingPid();
        if (enable) {
            this.mInjectEventPids.add(Integer.valueOf(pid));
        } else {
            this.mInjectEventPids.remove(Integer.valueOf(pid));
        }
        this.mHandler.sendEmptyMessage(5);
    }

    public void injectTouchMotionEvent(MotionEvent event) {
        if (!checkInjectEventsPermission()) {
            Slog.d(TAG, Binder.getCallingPid() + " Not have INJECT_EVENTS permission!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(2);
        msg.obj = event;
        msg.arg1 = 2;
        msg.arg2 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            if (args.length == 2) {
                if ("debuglog".equals(args[0])) {
                    this.DEBUG = Integer.parseInt(args[1]) == 1;
                }
            }
            dumpInternal(pw);
        }
    }

    private boolean checkInjectEventsPermission() {
        int pid = Binder.getCallingPid();
        return this.mInjcetEeventPackages.contains(ProcessUtils.getProcessNameByPid(pid));
    }

    public void dumpInternal(PrintWriter pw) {
        pw.println("SHOULDERKEY MANAGER (dumpsys shoulderkey)\n");
        pw.println("    DEBUG=" + this.DEBUG);
        pw.println("    mIsGameMode=" + this.mIsGameMode);
        pw.println("    mSupportShoulderKey=" + this.mSupportShoulderKey);
        pw.println("    mRecordEventStatus=" + this.mRecordEventStatus);
        pw.println("    mInjectEventPids=" + this.mInjectEventPids);
        if (this.mSupportShoulderKey) {
            pw.println("    mBoosterSwitch=" + this.mBoosterSwitch);
            pw.println("    mLeftShoulderKeySwitchStatus=" + this.mLeftShoulderKeySwitchStatus);
            pw.println("    mRightShoulderKeySwitchStatus=" + this.mRightShoulderKeySwitchStatus);
            pw.println("    mShoulderKeySoundSwitch=" + this.mShoulderKeySoundSwitch);
            pw.println("    mShoulderKeySoundType=" + this.mShoulderKeySoundType);
            pw.println("    DisplayInfo { rotation=" + this.mDisplayInfo.rotation + " width=" + this.mDisplayInfo.logicalWidth + " height=" + this.mDisplayInfo.logicalHeight + " }");
        }
    }

    private void handleShoulderKeyToMotionEvent(KeyEvent event, int position) {
        ShoulderKeyMap keymap;
        float centerX;
        float centerY;
        int action = event.getAction();
        if (action != 0 && action != 1) {
            return;
        }
        int productId = event.getDevice().getProductId();
        int keycode = event.getKeyCode();
        int i = 0;
        while (true) {
            if (i >= this.mLifeKeyMapper.size()) {
                keymap = null;
                break;
            } else if (!this.mLifeKeyMapper.keyAt(i).equals(productId, keycode)) {
                i++;
            } else {
                ShoulderKeyMap keymap2 = this.mLifeKeyMapper.valueAt(i);
                keymap = keymap2;
                break;
            }
        }
        if (keymap == null) {
            return;
        }
        if (action == 0) {
            this.mDownTime = SystemClock.uptimeMillis();
        }
        if (keymap.isIsSeparateMapping()) {
            if (action == 0) {
                centerX = keymap.getDownCenterX();
                centerY = keymap.getDownCenterY();
            } else {
                centerX = keymap.getUpCenterX();
                centerY = keymap.getUpCenterY();
            }
            float f = centerX;
            float f2 = centerY;
            MotionEvent downEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 0, f, f2, 0);
            this.mHandler.obtainMessage(2, position, Process.myPid(), downEvent).sendToTarget();
            MotionEvent upEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 1, f, f2, 0);
            this.mHandler.obtainMessage(2, position, Process.myPid(), upEvent).sendToTarget();
            return;
        }
        MotionEvent motionEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), action, keymap.getCenterX(), keymap.getCenterY(), 0);
        this.mHandler.obtainMessage(2, position, Process.myPid(), motionEvent).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transformMotionEventForInjection(MotionEvent motionEvent) {
        float ratio;
        if (this.DEBUG) {
            Slog.d(TAG, "before transform for injection: " + motionEvent.toString());
        }
        int rotation = this.mDisplayInfo.rotation;
        int width = this.mDisplayInfo.logicalWidth;
        int height = this.mDisplayInfo.logicalHeight;
        Display.Mode mode = this.mDisplayInfo.getMode();
        int physicalWidth = mode.getPhysicalWidth();
        Matrix matrix = new Matrix();
        switch (rotation) {
            case 1:
                ratio = height != 0 ? physicalWidth / height : 1.0f;
                matrix = MotionEvent.createRotateMatrix(3, height, width);
                break;
            case 2:
                ratio = width != 0 ? physicalWidth / width : 1.0f;
                matrix = MotionEvent.createRotateMatrix(2, width, height);
                break;
            case 3:
                ratio = height != 0 ? physicalWidth / height : 1.0f;
                matrix = MotionEvent.createRotateMatrix(1, height, width);
                break;
            default:
                ratio = width != 0 ? physicalWidth / width : 1.0f;
                break;
        }
        matrix.postScale(ratio, ratio);
        motionEvent.applyTransform(matrix);
        if (this.DEBUG) {
            Slog.d(TAG, "after transform for injection: " + motionEvent.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transformMotionEventForDeliver(MotionEvent motionEvent) {
        float ratio;
        if (this.DEBUG) {
            Slog.d(TAG, "before transform for deliver: " + motionEvent.toString());
        }
        int rotation = this.mDisplayInfo.rotation;
        int width = this.mDisplayInfo.logicalWidth;
        int height = this.mDisplayInfo.logicalHeight;
        Display.Mode mode = this.mDisplayInfo.getMode();
        int physicalWidth = mode.getPhysicalWidth();
        int physicalHeight = mode.getPhysicalHeight();
        Matrix matrix = new Matrix();
        switch (rotation) {
            case 1:
                ratio = physicalWidth != 0 ? height / physicalWidth : 1.0f;
                matrix = MotionEvent.createRotateMatrix(1, physicalHeight, physicalWidth);
                break;
            case 2:
                ratio = physicalWidth != 0 ? width / physicalWidth : 1.0f;
                matrix = MotionEvent.createRotateMatrix(2, physicalWidth, physicalHeight);
                break;
            case 3:
                ratio = physicalWidth != 0 ? height / physicalWidth : 1.0f;
                matrix = MotionEvent.createRotateMatrix(3, physicalHeight, physicalWidth);
                break;
            default:
                ratio = physicalWidth != 0 ? width / physicalWidth : 1.0f;
                break;
        }
        matrix.postScale(ratio, ratio);
        motionEvent.applyTransform(matrix);
        if (this.DEBUG) {
            Slog.d(TAG, "after transform for deliver: " + motionEvent.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleShoulderKeyEventInternal(KeyEvent event) {
        if (isShoulderKeyCanShortCut(event.getKeyCode())) {
            this.mMiuiShoulderKeyShortcutListener.handleShoulderKeyShortcut(event);
        }
        int position = -1;
        if (event.getKeyCode() == 131) {
            position = 0;
        } else if (event.getKeyCode() == 132) {
            position = 1;
        }
        if (position != -1) {
            sendShoulderKeyEventBroadcast(1, position, event.getAction());
            if (this.mIsGameMode && !this.mLifeKeyMapper.isEmpty()) {
                handleShoulderKeyToMotionEvent(event, position);
            }
        }
        if (event.getAction() == 0) {
            if (event.getKeyCode() == 133) {
                setShoulderKeySwitchStatusInternal(0, true);
                return;
            }
            if (event.getKeyCode() == 134) {
                setShoulderKeySwitchStatusInternal(0, false);
            } else if (event.getKeyCode() == 135) {
                setShoulderKeySwitchStatusInternal(1, true);
            } else if (event.getKeyCode() == 136) {
                setShoulderKeySwitchStatusInternal(1, false);
            }
        }
    }

    public boolean isShoulderKeyCanShortCut(int keyCode) {
        return (keyCode == 131 || keyCode == 132) && ShoulderKeyManager.SUPPORT_SHOULDERKEY_MORE && !this.mIsPocketMode && !this.mIsGameMode && isUserSetupComplete();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setShoulderKeySwitchStatusInternal(int i, boolean z) {
        if (i == 0) {
            this.mLeftShoulderKeySwitchStatus = z;
            this.mLeftShoulderKeySwitchTriggered = z;
        } else if (i == 1) {
            this.mRightShoulderKeySwitchStatus = z;
            this.mRightShoulderKeySwitchTriggered = z;
        } else {
            return;
        }
        if (z) {
            this.mHandler.sendEmptyMessageDelayed(4, GAMEBOOSTER_DEBOUNCE_DELAY_MILLS);
        }
        sendShoulderKeyEventBroadcast(0, i, z ? 1 : 0);
        interceptGameBooster();
        playSoundIfNeeded(this.mShoulderKeySoundType + "-" + i + "-" + (z ? 1 : 0));
        if ((this.mLeftShoulderKeySwitchStatus || this.mRightShoulderKeySwitchStatus) && !this.mIsScreenOn) {
            registerNonUIListener();
        } else {
            unregisterNonUIListener();
        }
    }

    private void playSoundIfNeeded(String soundId) {
        if (this.mShoulderKeySoundSwitch && this.mIsScreenOn) {
            ShoulderKeyUtil.playSound(soundId, false);
        }
    }

    private void interceptGameBooster() {
        boolean isKeyguardShown = this.mKeyguardManager.isKeyguardLocked();
        if (!this.mIsGameMode && this.mIsScreenOn && this.mBoosterSwitch && !isKeyguardShown && this.mLeftShoulderKeySwitchTriggered && this.mRightShoulderKeySwitchTriggered && isUserSetupComplete()) {
            launchGameBooster();
        }
    }

    private void registerNonUIListener() {
        Sensor sensor;
        if (this.mRegisteredNonUI) {
            return;
        }
        SensorManager sensorManager = this.mSm;
        if (sensorManager != null && (sensor = this.mNonUISensor) != null) {
            sensorManager.registerListener(this.mNonUIListener, sensor, 3, this.mHandler);
            this.mRegisteredNonUI = true;
            Slog.w(TAG, " register NonUISensorListener");
            return;
        }
        Slog.w(TAG, " mNonUISensor is null");
    }

    private void unregisterNonUIListener() {
        SensorManager sensorManager;
        if (this.mRegisteredNonUI && (sensorManager = this.mSm) != null) {
            sensorManager.unregisterListener(this.mNonUIListener);
            this.mRegisteredNonUI = false;
            this.mIsPocketMode = false;
            Slog.w(TAG, " unregister NonUISensorListener,mIsPocketMode = false");
        }
    }

    private void launchGameBooster() {
        Intent intent = new Intent("com.miui.gamebooster.action.ACCESS_MAINACTIVITY");
        intent.putExtra("jump_target", "gamebox");
        intent.addFlags(268435456);
        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private void sendShoulderKeyEventBroadcast(int type, int position, int action) {
        ShoulderKeyOneTrack.reportShoulderKeyActionOneTrack(this.mContext, type, position, action, this.mIsGameMode, this.mCurrentForegroundAppLabel);
        Intent intent = new Intent("com.miui.shoulderkey");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, type);
        intent.putExtra("position", position);
        intent.putExtra("action", action);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadSoundResourceIfNeeded() {
        if (this.mShoulderKeySoundSwitch) {
            ShoulderKeyUtil.loadSoundResource(this.mContext);
        } else {
            ShoulderKeyUtil.releaseSoundResource();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScreenStateInternal(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
        if ((this.mLeftShoulderKeySwitchStatus || this.mRightShoulderKeySwitchStatus) && !isScreenOn) {
            registerNonUIListener();
        } else {
            unregisterNonUIListener();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyForegroundAppChanged() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = ShoulderKeyManagerService.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null) {
                        String packageName = info.topActivity.getPackageName();
                        if (ShoulderKeyManagerService.this.mCurrentForegroundPkg != null && ShoulderKeyManagerService.this.mCurrentForegroundPkg.equals(packageName)) {
                            return;
                        }
                        ShoulderKeyManagerService.this.mCurrentForegroundPkg = packageName;
                        ShoulderKeyManagerService shoulderKeyManagerService = ShoulderKeyManagerService.this;
                        shoulderKeyManagerService.mCurrentForegroundAppLabel = shoulderKeyManagerService.getAppLabelByPkgName(packageName);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getAppLabelByPkgName(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = this.mPackageManager.getApplicationInfo(packageName, 64);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (ai == null) {
            return "";
        }
        String label = ai.loadLabel(this.mPackageManager).toString();
        return label;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TouchMotionEventListenerRecord implements IBinder.DeathRecipient {
        private final ITouchMotionEventListener mListener;
        private final int mPid;

        public TouchMotionEventListenerRecord(int pid, ITouchMotionEventListener listener) {
            this.mPid = pid;
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (ShoulderKeyManagerService.this.DEBUG) {
                Slog.d(ShoulderKeyManagerService.TAG, "Touch MoitonEvent listener for pid " + this.mPid + " died.");
            }
            ShoulderKeyManagerService.this.onTouchMotionEventListenerDied(this.mPid);
        }

        public void notifyTouchMotionEvent(MotionEvent event) {
            try {
                this.mListener.onTouchMotionEvent(event);
            } catch (RemoteException ex) {
                Slog.w(ShoulderKeyManagerService.TAG, "Failed to notify process " + this.mPid + " that Touch MotionEvent, assuming it died.", ex);
                binderDied();
            }
        }
    }

    public void registerTouchMotionEventListener(ITouchMotionEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTouchMotionEventLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mTouchMotionEventListeners.get(callingPid) != null) {
                throw new IllegalStateException("The calling process has already registered a TabletModeChangedListener.");
            }
            TouchMotionEventListenerRecord record = new TouchMotionEventListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mTouchMotionEventListeners.put(callingPid, record);
                this.mHandler.obtainMessage(6, true).sendToTarget();
            } catch (RemoteException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public void unregisterTouchMotionEventListener() {
        synchronized (this.mTouchMotionEventLock) {
            int callingPid = Binder.getCallingPid();
            onTouchMotionEventListenerDied(callingPid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTouchMotionEventListenerDied(int pid) {
        synchronized (this.mTouchMotionEventLock) {
            this.mTouchMotionEventListeners.remove(pid);
            this.mHandler.obtainMessage(6, Boolean.valueOf(this.mTouchMotionEventListeners.size() != 0)).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverTouchMotionEvent(MotionEvent event) {
        int numListeners;
        if (this.DEBUG) {
            Slog.d(TAG, "deliverTouchMotionEvent " + event.toString());
        }
        this.mTempTouchMotionEventListenersToNotify.clear();
        synchronized (this.mTouchMotionEventLock) {
            numListeners = this.mTouchMotionEventListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mTempTouchMotionEventListenersToNotify.add(this.mTouchMotionEventListeners.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mTempTouchMotionEventListenersToNotify.get(i2).notifyTouchMotionEvent(event);
        }
    }

    /* loaded from: classes.dex */
    class LocalService implements ShoulderKeyManagerInternal {
        LocalService() {
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void systemReady() {
            ShoulderKeyManagerService.this.systemReadyInternal();
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void handleShoulderKeyEvent(KeyEvent event) {
            ShoulderKeyManagerService.this.handleShoulderKeyEventInternal(event);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void updateScreenState(boolean isScreenOn) {
            ShoulderKeyManagerService.this.updateScreenStateInternal(isScreenOn);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void setShoulderKeySwitchStatus(int position, boolean isPopup) {
            ShoulderKeyManagerService.this.setShoulderKeySwitchStatusInternal(position, isPopup);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void notifyTouchMotionEvent(MotionEvent event) {
            Message msg = ShoulderKeyManagerService.this.mHandler.obtainMessage(3, event);
            ShoulderKeyManagerService.this.mHandler.sendMessage(msg);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void onUserSwitch() {
            ShoulderKeyManagerService.this.mSettingsObserver.update();
            ShoulderKeyManagerService.this.mMiuiShoulderKeyShortcutListener.updateSettings();
        }
    }

    /* loaded from: classes.dex */
    class MiuiSettingsObserver extends ContentObserver {
        MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = ShoulderKeyManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE), false, this, -1);
            update();
        }

        void update() {
            onChange(false, Settings.Secure.getUriFor("gb_boosting"));
            onChange(false, Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH));
            onChange(false, Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH));
            onChange(false, Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChanged, Uri uri) {
            ContentResolver resolver = ShoulderKeyManagerService.this.mContext.getContentResolver();
            if (Settings.Secure.getUriFor("gb_boosting").equals(uri)) {
                ShoulderKeyManagerService.this.mIsGameMode = Settings.Secure.getIntForUser(resolver, "gb_boosting", 0, -2) == 1;
                if (!ShoulderKeyManagerService.this.mIsGameMode) {
                    ShoulderKeyManagerService.this.mHandler.obtainMessage(7).sendToTarget();
                    return;
                }
                return;
            }
            if (Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH).equals(uri)) {
                ShoulderKeyManagerService.this.mBoosterSwitch = Settings.Secure.getIntForUser(resolver, ShoulderKeyManagerService.GAME_BOOSTER_SWITCH, 1, -2) == 1;
                return;
            }
            if (Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH).equals(uri)) {
                ShoulderKeyManagerService.this.mShoulderKeySoundSwitch = Settings.System.getIntForUser(resolver, ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH, 0, -2) == 1;
                ShoulderKeyManagerService.this.loadSoundResourceIfNeeded();
            } else if (Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE).equals(uri)) {
                ShoulderKeyManagerService.this.mShoulderKeySoundType = Settings.System.getStringForUser(resolver, ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE, -2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() {
            ShoulderKeyManagerService.this.notifyForegroundAppChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ShoulderKeyOneTrack {
        private static final String EXTRA_APP_ID = "31000000481";
        private static final String EXTRA_EVENT_NAME = "shoulderkey";
        private static final String EXTRA_PACKAGE_NAME = "com.xiaomi.shoulderkey";
        private static final int FLAG_NON_ANONYMOUS = 2;
        private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
        private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
        private static final String KEY_ACTION = "action";
        private static final String KEY_EVENT_TYPE = "event_type";
        private static final String KEY_GAME_NAME = "game_name";
        private static final String KEY_IS_GAMEBOOSTER = "is_gamebooster";
        private static final String KEY_POSITION = "position";
        private static final String TAG = "ShoulderKeyOneTrack";

        private ShoulderKeyOneTrack() {
        }

        public static void reportShoulderKeyActionOneTrack(final Context context, final int eventType, final int position, final int action, final boolean isGameMode, final String pkgLabel) {
            if (context == null || Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService$ShoulderKeyOneTrack$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ShoulderKeyManagerService.ShoulderKeyOneTrack.lambda$reportShoulderKeyActionOneTrack$0(eventType, position, action, isGameMode, pkgLabel, context);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$reportShoulderKeyActionOneTrack$0(int eventType, int position, int action, boolean isGameMode, String pkgLabel, Context context) {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, EXTRA_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EXTRA_EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, EXTRA_PACKAGE_NAME);
            intent.putExtra(KEY_EVENT_TYPE, eventType);
            intent.putExtra(KEY_POSITION, position);
            intent.putExtra(KEY_ACTION, action);
            intent.putExtra(KEY_IS_GAMEBOOSTER, isGameMode);
            if (isGameMode) {
                intent.putExtra(KEY_GAME_NAME, pkgLabel);
            }
            intent.setFlags(2);
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.w(TAG, "Failed to upload ShoulderKey event.");
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to start service.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final int MSG_DELIVER_TOUCH_MOTIONEVENT = 3;
        private static final int MSG_EXIT_GAMEMODE = 7;
        private static final int MSG_INJECT_EVENT_STATUS = 5;
        private static final int MSG_INJECT_MOTIONEVENT = 2;
        private static final int MSG_LOAD_LIFTKEYMAP = 0;
        private static final int MSG_RECORD_EVENT_STATUS = 6;
        private static final int MSG_RESET_SHOULDERKEY_TRIGGER_STATE = 4;
        private static final int MSG_UNLOAD_LIFTKEYMAP = 1;

        public H(Looper looper) {
            super(looper);
        }

        private void updateInjectEventStatus() {
            InputCommonConfig.getInstance().setInjectEventStatus(ShoulderKeyManagerService.this.mInjectEventPids.size() != 0);
            InputCommonConfig.getInstance().flushToNative();
        }

        private void updateRecordEventStatus(Boolean enable) {
            ShoulderKeyManagerService.this.mRecordEventStatus = enable.booleanValue();
            InputCommonConfig.getInstance().setRecordEventStatus(ShoulderKeyManagerService.this.mRecordEventStatus);
            InputCommonConfig.getInstance().flushToNative();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Map mapper = (Map) msg.obj;
                    ShoulderKeyManagerService.this.mInjectEventPids.add(Integer.valueOf(Process.myPid()));
                    updateInjectEventStatus();
                    if (mapper != null) {
                        ShoulderKeyManagerService.this.mLifeKeyMapper.clear();
                        ShoulderKeyManagerService.this.mLifeKeyMapper.putAll(mapper);
                        Slog.d(ShoulderKeyManagerService.TAG, "loadLiftKeyMap " + ShoulderKeyManagerService.this.mLifeKeyMapper);
                        return;
                    }
                    Slog.d(ShoulderKeyManagerService.TAG, "loadLiftKeyMap : null");
                    return;
                case 1:
                    ShoulderKeyManagerService.this.mInjectEventPids.remove(Integer.valueOf(Process.myPid()));
                    updateInjectEventStatus();
                    ShoulderKeyManagerService.this.mLifeKeyMapper.clear();
                    return;
                case 2:
                    MotionEvent event = (MotionEvent) msg.obj;
                    ShoulderKeyManagerService.this.transformMotionEventForInjection(event);
                    ShoulderKeyManagerService.this.mMiuiInputManagerInternal.injectMotionEvent(event, msg.arg1);
                    if (ShoulderKeyManagerService.this.DEBUG) {
                        Slog.d(ShoulderKeyManagerService.TAG, ProcessUtils.getProcessNameByPid(msg.arg2) + " inject motionEvent " + msg.arg1 + " " + event.toString());
                        return;
                    } else {
                        if (event.getActionMasked() != 2) {
                            Slog.d(ShoulderKeyManagerService.TAG, ProcessUtils.getProcessNameByPid(msg.arg2) + " inject motionEvent " + msg.arg1 + " " + MotionEvent.actionToString(event.getAction()));
                            return;
                        }
                        return;
                    }
                case 3:
                    MotionEvent event2 = (MotionEvent) msg.obj;
                    ShoulderKeyManagerService.this.transformMotionEventForDeliver(event2);
                    ShoulderKeyManagerService.this.deliverTouchMotionEvent(event2);
                    return;
                case 4:
                    ShoulderKeyManagerService.this.mLeftShoulderKeySwitchTriggered = false;
                    ShoulderKeyManagerService.this.mRightShoulderKeySwitchTriggered = false;
                    return;
                case 5:
                    updateInjectEventStatus();
                    return;
                case 6:
                    updateRecordEventStatus((Boolean) msg.obj);
                    return;
                case 7:
                    if (ShoulderKeyManagerService.this.DEBUG) {
                        Slog.d(ShoulderKeyManagerService.TAG, "EXIT GAMEMODE");
                    }
                    ShoulderKeyManagerService.this.mInjectEventPids.clear();
                    updateInjectEventStatus();
                    updateRecordEventStatus(false);
                    return;
                default:
                    return;
            }
        }
    }
}
