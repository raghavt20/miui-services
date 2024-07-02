package com.miui.server.input.stylus;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.ActivityThread;
import android.app.ContextImpl;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.ReflectionUtils;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityTaskManagerService;
import com.miui.server.AccessController;
import com.miui.server.input.MiuiInputSettingsConnection;
import com.miui.server.input.stylus.laser.LaserPointerController;
import com.miui.server.input.util.MiuiInputShellCommand;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class MiuiStylusShortcutManager {
    private static final int HALL_CLOSE = 0;
    private static final int HALL_FAR = 1;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final String KEY_STYLUS_SCREEN_OFF_QUICK_NOTE = "stylus_quick_note_screen_off";
    private static final int LONG_PRESS_TIME_OUT = 380;
    private static final String POWER_SUPPLY_PEN_HALL3_EVENT = "POWER_SUPPLY_PEN_HALL3";
    private static final String POWER_SUPPLY_PEN_HALL4_EVENT = "POWER_SUPPLY_PEN_HALL4";
    public static final String SCENE_APP = "app";
    public static final String SCENE_HOME = "home";
    public static final String SCENE_KEYGUARD = "keyguard";
    public static final String SCENE_OFF_SCREEN = "off_screen";
    private static final String STYLUS_HALL_STATUS = "stylus_hall_status";
    private static final String STYLUS_PAGE_KEY_BW_CONFIG = "STYLUS_PAGE_KEY_BLACK_WHITE_LIST";
    private static final float SWIPE_HEIGHT_DOWN_PERCENTAGE = 0.25f;
    private static final float SWIPE_HEIGHT_UP_PERCENTAGE = 0.75f;
    private static final float SWIPE_WIDTH_PERCENTAGE = 0.65f;
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "MiuiStylusShortcutManager";
    private static volatile MiuiStylusShortcutManager sInstance;
    private final ActivityTaskManagerService mAtms;
    private final Context mContext;
    private WindowManagerPolicy.WindowState mFocusedWindow;
    private final Handler mHandler;
    private final MiuiInputSettingsConnection mInputSettingsConnection;
    private boolean mIsGameMode;
    private boolean mIsRequestMaskShow;
    private boolean mIsScreenOffQuickNoteOn;
    private boolean mIsScreenOn;
    private boolean mIsUserSetupComplete;
    private LaserPointerController mLaserPointerController;
    private final MiuiInputManagerInternal mMiuiInputManagerInternal;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private final MiuiStylusDeviceListener mMiuiStylusDeviceListener;
    private boolean mNeedDispatchLaserKey;
    private volatile boolean mQuickNoteKeyFunctionTriggered;
    private String mScene;
    private volatile boolean mScreenShotKeyFunctionTriggered;
    private final UEventObserver mStylusHallObserver;
    private StylusPageKeyConfig mStylusPageKeyConfig;
    private static final Set<String> NOTE_PACKAGE_NAME = Set.of("com.miui.notes", "com.miui.pen.demo", "com.miui.creation");
    private static final Set<String> NEED_LASER_KEY_APP = Set.of(AccessController.PACKAGE_CAMERA);
    private static final boolean STYLUS_SCREEN_OFF_QUICK_NOTE_DEFAULT = SystemProperties.getBoolean("persist.sys.quick.note.enable.default", false);
    private boolean mLidOpen = true;
    private int mHallStatus = 1;

    private MiuiStylusShortcutManager() {
        UEventObserver uEventObserver = new UEventObserver() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager.1
            public void onUEvent(UEventObserver.UEvent event) {
                String hall3_str = event.get(MiuiStylusShortcutManager.POWER_SUPPLY_PEN_HALL3_EVENT);
                String hall4_str = event.get(MiuiStylusShortcutManager.POWER_SUPPLY_PEN_HALL4_EVENT);
                if (hall3_str != null && hall4_str != null) {
                    int hall3 = MiuiStylusShortcutManager.this.parseInt(hall3_str);
                    int hall4 = MiuiStylusShortcutManager.this.parseInt(hall4_str);
                    if ((hall3 == 0 || hall4 == 0) && MiuiStylusShortcutManager.this.mHallStatus == 1) {
                        MiuiStylusShortcutManager.this.mHallStatus = 0;
                        MiuiStylusShortcutManager.this.setHallStatus();
                    } else if (hall3 == 1 && hall4 == 1 && MiuiStylusShortcutManager.this.mHallStatus == 0) {
                        MiuiStylusShortcutManager.this.mHallStatus = 1;
                        MiuiStylusShortcutManager.this.setHallStatus();
                    }
                }
            }
        };
        this.mStylusHallObserver = uEventObserver;
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mContext = systemContext;
        this.mHandler = new H(MiuiInputThread.getHandler().getLooper());
        this.mIsUserSetupComplete = isUserSetUp();
        this.mMiuiStylusDeviceListener = new MiuiStylusDeviceListener(systemContext);
        MiuiInputSettingsConnection miuiInputSettingsConnection = MiuiInputSettingsConnection.getInstance();
        this.mInputSettingsConnection = miuiInputSettingsConnection;
        miuiInputSettingsConnection.registerCallbackListener(1, new Consumer() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiStylusShortcutManager.this.lambda$new$0((Message) obj);
            }
        });
        this.mAtms = ActivityTaskManager.getService();
        uEventObserver.startObserving(POWER_SUPPLY_PEN_HALL3_EVENT);
        uEventObserver.startObserving(POWER_SUPPLY_PEN_HALL4_EVENT);
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(Message value) {
        triggerQuickNoteKeyFunction();
    }

    public static MiuiStylusShortcutManager getInstance() {
        if (sInstance == null) {
            synchronized (MiuiStylusShortcutManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiStylusShortcutManager();
                }
            }
        }
        return sInstance;
    }

    public void onSystemBooted() {
        initStylusPageKeyConfig();
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
    }

    private void initStylusPageKeyConfig() {
        this.mStylusPageKeyConfig = new StylusPageKeyConfig();
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiStylusShortcutManager.this.lambda$initStylusPageKeyConfig$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initStylusPageKeyConfig$1() {
        this.mStylusPageKeyConfig = MiuiStylusUtils.getDefaultStylusPageKeyConfig();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudConfig() {
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), STYLUS_PAGE_KEY_BW_CONFIG, (String) null, (String) null, false);
        if (cloudData == null || cloudData.json() == null) {
            Slog.w(TAG, "Cloud config is null, use default local config");
            return;
        }
        StylusPageKeyConfig cloudConfig = MiuiStylusUtils.parseJsonToStylusPageKeyConfig(cloudData.json());
        if (cloudConfig.getVersion() < this.mStylusPageKeyConfig.getVersion()) {
            Slog.w(TAG, "Do not use cloud config, because cloud config version: " + cloudConfig.getVersion() + " < current config version: " + this.mStylusPageKeyConfig.getVersion());
        } else {
            this.mStylusPageKeyConfig = cloudConfig;
            Slog.w(TAG, "Cloud config has updated, use cloud config");
        }
    }

    private boolean isSupportStylusRemoteControl() {
        if (this.mFocusedWindow == null || !this.mStylusPageKeyConfig.isEnable()) {
            return false;
        }
        String name = this.mFocusedWindow.getOwningPackage();
        if (this.mStylusPageKeyConfig.getAppWhiteSet().contains(name)) {
            if (this.mStylusPageKeyConfig.getActivityBlackSet().isEmpty()) {
                Slog.w(TAG, "Current activity supports short video remote control");
                return true;
            }
            String activityName = getActivityName();
            if (!TextUtils.isEmpty(activityName) && !this.mStylusPageKeyConfig.getActivityBlackSet().contains(activityName)) {
                Slog.w(TAG, "Current activity " + activityName + " supports short video remote control");
                return true;
            }
        }
        return false;
    }

    private String getActivityName() {
        WindowManager.LayoutParams attrs;
        String className;
        int index;
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(windowState, "getAttrs", new Object[0])) == null || (index = (className = attrs.getTitle().toString()).lastIndexOf(47)) < 0 || index + 1 >= className.length()) {
            return "";
        }
        String activityName = className.substring(index + 1);
        return activityName;
    }

    public long getDelayTime(KeyEvent keyEvent) {
        if (!isScreenShotKey(keyEvent) && !isQuickNoteKey(keyEvent)) {
            return 0L;
        }
        boolean isTriggered = isQuickNoteKey(keyEvent) ? this.mQuickNoteKeyFunctionTriggered : this.mScreenShotKeyFunctionTriggered;
        if (isTriggered) {
            int repeatCount = keyEvent.getRepeatCount();
            if (repeatCount == 0) {
                Slog.i(TAG, "Shortcut triggered, so intercept " + KeyEvent.keyCodeToString(keyEvent.getKeyCode()));
            }
            return -1L;
        }
        if (!this.mHandler.hasMessages(1) && !this.mHandler.hasMessages(2)) {
            return interceptKeyByRemoteControl(keyEvent) ? -1L : 0L;
        }
        long now = SystemClock.uptimeMillis();
        long timeoutTime = keyEvent.getDownTime() + 380 + 20;
        if (now < timeoutTime) {
            return timeoutTime - now;
        }
        return 0L;
    }

    private boolean interceptKeyByRemoteControl(KeyEvent keyEvent) {
        if (this.mIsScreenOn && keyEvent.getAction() == 0) {
            int keyCode = keyEvent.getKeyCode();
            if (keyCode == 93) {
                StylusOneTrackHelper stylusOneTrackHelper = StylusOneTrackHelper.getInstance(this.mContext);
                String keyCodeToString = KeyEvent.keyCodeToString(keyCode);
                WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
                stylusOneTrackHelper.trackStylusKeyPress(keyCodeToString, windowState != null ? windowState.getOwningPackage() : "");
            } else if (keyCode == 92) {
                StylusOneTrackHelper stylusOneTrackHelper2 = StylusOneTrackHelper.getInstance(this.mContext);
                String keyCodeToString2 = KeyEvent.keyCodeToString(keyCode);
                WindowManagerPolicy.WindowState windowState2 = this.mFocusedWindow;
                stylusOneTrackHelper2.trackStylusKeyPress(keyCodeToString2, windowState2 != null ? windowState2.getOwningPackage() : "");
            }
        }
        InputDevice device = keyEvent.getDevice();
        if (device == null || device.isXiaomiStylus() < 3 || !isSupportStylusRemoteControl()) {
            return false;
        }
        if (keyEvent.getAction() == 0) {
            int keyCode2 = keyEvent.getKeyCode();
            if (keyCode2 == 93) {
                this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiStylusShortcutManager.this.next();
                    }
                });
                return true;
            }
            if (keyCode2 == 92) {
                this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiStylusShortcutManager.this.previous();
                    }
                });
                return true;
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void previous() {
        if (isSplitOrFreeFormMode()) {
            return;
        }
        int[] swipeInfo = getWindowSwipeInfo();
        Slog.w(TAG, "swipePositionInfo: downX: " + swipeInfo[0] + " downY: " + swipeInfo[1] + " upX: " + swipeInfo[0] + " upY: " + swipeInfo[2]);
        MiuiInputShellCommand.getInstance().swipeGenerator(swipeInfo[0], swipeInfo[1], swipeInfo[0], swipeInfo[2], 60);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void next() {
        if (isSplitOrFreeFormMode()) {
            return;
        }
        int[] swipeInfo = getWindowSwipeInfo();
        Slog.w(TAG, "swipePositionInfo: downX: " + swipeInfo[0] + " downY: " + swipeInfo[2] + " upX: " + swipeInfo[0] + " upY: " + swipeInfo[1]);
        MiuiInputShellCommand.getInstance().swipeGenerator(swipeInfo[0], swipeInfo[2], swipeInfo[0], swipeInfo[1], 60);
    }

    private int[] getWindowSwipeInfo() {
        int heightPixels = this.mContext.getResources().getDisplayMetrics().heightPixels;
        int widthPixels = this.mContext.getResources().getDisplayMetrics().widthPixels;
        int[] swipeInfo = {(int) (widthPixels * SWIPE_WIDTH_PERCENTAGE), (int) (heightPixels * SWIPE_HEIGHT_DOWN_PERCENTAGE), (int) (heightPixels * 0.75f)};
        return swipeInfo;
    }

    private boolean isSplitOrFreeFormMode() {
        if (this.mAtms.isInSplitScreenWindowingMode()) {
            Slog.w(TAG, "current activity is split screen windowing mode， not support short video remote control");
            return true;
        }
        List<ActivityManager.RunningTaskInfo> runningTasks = this.mAtms.getTasks(1);
        if (!runningTasks.isEmpty() && runningTasks.get(0).getWindowingMode() != 5) {
            return false;
        }
        Slog.w(TAG, "current activity is free form windowing mode， not support short video remote control");
        return true;
    }

    public boolean shouldInterceptKey(KeyEvent event) {
        if (!isUserSetupComplete() || this.mIsGameMode) {
            return false;
        }
        boolean isScreenShotKey = isScreenShotKey(event);
        boolean isQuickNoteKey = isQuickNoteKey(event);
        if ((isScreenShotKey || isQuickNoteKey) && event.getAction() == 0) {
            fadeLaserAndResetPosition();
        }
        if (isScreenShotKey) {
            return interceptScreenShotKey(event);
        }
        if (isQuickNoteKey) {
            return interceptQuickNoteKey(event);
        }
        if (isLaserKey(event)) {
            return interceptLaserKey(event);
        }
        return false;
    }

    private boolean interceptLaserKey(KeyEvent event) {
        if (event.getAction() == 0) {
            WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
            this.mNeedDispatchLaserKey = windowState != null && NEED_LASER_KEY_APP.contains(windowState.getOwningPackage());
        }
        if (this.mNeedDispatchLaserKey) {
            return false;
        }
        if (this.mLaserPointerController == null) {
            this.mLaserPointerController = (LaserPointerController) this.mMiuiInputManagerInternal.obtainLaserPointerController();
        }
        return this.mLaserPointerController.interceptLaserKey(event);
    }

    private void fadeLaserAndResetPosition() {
        LaserPointerController laserPointerController = this.mLaserPointerController;
        if (laserPointerController == null) {
            return;
        }
        laserPointerController.fade(0);
        this.mLaserPointerController.resetPosition();
    }

    private boolean interceptQuickNoteKey(KeyEvent event) {
        boolean isUp = event.getAction() == 1;
        if (isUp) {
            this.mHandler.sendEmptyMessage(4);
            removeMessageIfHas(1);
            return this.mQuickNoteKeyFunctionTriggered;
        }
        this.mQuickNoteKeyFunctionTriggered = false;
        if (this.mIsScreenOn) {
            return interceptQuickNoteKeyDownWhenScreenOn();
        }
        return interceptQuickNoteKeyDownWhenScreenOff();
    }

    private boolean interceptQuickNoteKeyDownWhenScreenOff() {
        return false;
    }

    private boolean interceptQuickNoteKeyDownWhenScreenOn() {
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState == null) {
            Slog.i(TAG, "focus window is null");
            return false;
        }
        if (NOTE_PACKAGE_NAME.contains(windowState.getOwningPackage())) {
            Slog.i(TAG, "focus window is notes, so long press page down disable");
            return false;
        }
        this.mHandler.sendEmptyMessageDelayed(1, 380L);
        return false;
    }

    private boolean interceptScreenShotKey(KeyEvent event) {
        boolean isUp = event.getAction() == 1;
        if (isUp) {
            this.mHandler.removeMessages(2);
            return this.mScreenShotKeyFunctionTriggered;
        }
        this.mScreenShotKeyFunctionTriggered = false;
        this.mHandler.sendEmptyMessageDelayed(2, 380L);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addStylusMaskWindow() {
        if (!this.mIsScreenOn) {
            Slog.w(TAG, "Can't add stylus mask window because screen is not on");
            return;
        }
        this.mScene = SCENE_APP;
        String owningPackage = null;
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState != null) {
            owningPackage = windowState.getOwningPackage();
        }
        if (((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isKeyguardLocked() && "com.android.systemui".equals(owningPackage)) {
            this.mScene = SCENE_KEYGUARD;
        } else if ("com.miui.home".equals(owningPackage)) {
            this.mScene = SCENE_HOME;
        }
        Slog.i(TAG, "request add stylus mask");
        this.mInputSettingsConnection.sendMessageToInputSettings(1);
        this.mIsRequestMaskShow = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeStylusMaskWindow(boolean z) {
        Slog.i(TAG, "request remove stylus mask, mIsRequestMaskShow = " + this.mIsRequestMaskShow);
        if (!this.mIsRequestMaskShow) {
            return;
        }
        this.mInputSettingsConnection.sendMessageToInputSettings(2, z ? 1 : 0);
        this.mIsRequestMaskShow = false;
    }

    private void removeMessageIfHas(int what) {
        if (this.mHandler.hasMessages(what)) {
            this.mHandler.removeMessages(what);
        }
    }

    private boolean isUserSetupComplete() {
        if (!this.mIsUserSetupComplete) {
            boolean isUserSetUp = isUserSetUp();
            this.mIsUserSetupComplete = isUserSetUp;
            return isUserSetUp;
        }
        return true;
    }

    public boolean needInterceptBeforeDispatching(KeyEvent event) {
        return isUserSetupComplete() && !this.mIsGameMode && (isScreenShotKey(event) || isQuickNoteKey(event));
    }

    public void updateScreenState(boolean screenOn) {
        this.mIsScreenOn = screenOn;
        if (screenOn) {
            return;
        }
        this.mHandler.sendEmptyMessage(3);
    }

    public void onDefaultDisplayFocusChangedLw(WindowManagerPolicy.WindowState newFocus) {
        this.mFocusedWindow = newFocus;
    }

    public void processMotionEventForQuickNote(MotionEvent event) {
        if (!this.mIsScreenOffQuickNoteOn) {
            Slog.w(TAG, "Stylus screen off quick note function is off.");
            return;
        }
        if (this.mIsScreenOn || !this.mLidOpen) {
            Slog.w(TAG, "Not support quick note for offscreen or lid not open state. screenOn = " + this.mIsScreenOn + ", lidOpen = " + this.mLidOpen);
            return;
        }
        boolean z = false;
        if (event.isFromSource(16386) && event.getToolType(0) == 2) {
            z = true;
        }
        boolean eventFromStylus = z;
        if (!eventFromStylus) {
            Slog.w(TAG, "Event not from stylus, event = " + event);
        } else {
            if (event.getActionMasked() != 0) {
                return;
            }
            this.mHandler.sendEmptyMessage(5);
        }
    }

    private boolean isUserSetUp() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private void takePartialScreenshot() {
        if (!this.mIsScreenOn) {
            Slog.w(TAG, "Can't take screenshot because screen is not on");
        } else {
            ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("stylus_partial_screenshot", "long_press_page_up_key", null, false);
        }
    }

    private void launchNote() {
        Bundle bundle = new Bundle();
        bundle.putString("scene", this.mScene);
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("note", "stylus", bundle, true);
    }

    private void triggerQuickNoteKeyFunction() {
        launchNote();
        StylusOneTrackHelper stylusOneTrackHelper = StylusOneTrackHelper.getInstance(this.mContext);
        String keyCodeToString = KeyEvent.keyCodeToString(93);
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        stylusOneTrackHelper.trackStylusKeyLongPress("note", keyCodeToString, windowState != null ? windowState.getOwningPackage() : "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerScreenShotKeyFunction() {
        takePartialScreenshot();
        StylusOneTrackHelper stylusOneTrackHelper = StylusOneTrackHelper.getInstance(this.mContext);
        String keyCodeToString = KeyEvent.keyCodeToString(92);
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        stylusOneTrackHelper.trackStylusKeyLongPress("stylus_partial_screenshot", keyCodeToString, windowState != null ? windowState.getOwningPackage() : "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerNoteWhenScreenOff() {
        this.mScene = SCENE_OFF_SCREEN;
        launchNote();
        StylusOneTrackHelper.getInstance(this.mContext).trackStylusShortHandTrigger();
    }

    private static boolean isQuickNoteKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputDevice device = event.getDevice();
        return keyCode == 93 && device != null && device.isXiaomiStylus() > 0;
    }

    private static boolean isScreenShotKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputDevice device = event.getDevice();
        return keyCode == 92 && device != null && device.isXiaomiStylus() > 0;
    }

    private static boolean isLaserKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputDevice device = event.getDevice();
        return keyCode == 193 && device != null && device.isXiaomiStylus() == 3;
    }

    public void notifyLidSwitchChanged(boolean lidOpen) {
        this.mLidOpen = lidOpen;
        Slog.d(TAG, "notify lidSwitch change to : " + lidOpen);
    }

    public void onUserSwitch(int newUserId) {
        updateGameModeSettings();
        if (MiuiStylusUtils.isSupportOffScreenQuickNote()) {
            updateStylusScreenOffQuickNoteSettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateStylusScreenOffQuickNoteSettings() {
        this.mIsScreenOffQuickNoteOn = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), KEY_STYLUS_SCREEN_OFF_QUICK_NOTE, STYLUS_SCREEN_OFF_QUICK_NOTE_DEFAULT, -2);
        Slog.i(TAG, "Update stylus screen off quick note settings = " + this.mIsScreenOffQuickNoteOn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameModeSettings() {
        this.mIsGameMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "gb_boosting", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHallStatus() {
        Slog.d(TAG, "Stylus hall event " + (this.mHallStatus == 0 ? "close" : "far"));
        removeMessageIfHas(6);
        Message msg = Message.obtain();
        msg.setWhat(6);
        msg.arg1 = this.mHallStatus;
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void putHallSettings(int hall) {
        Settings.System.putInt(this.mContext.getContentResolver(), STYLUS_HALL_STATUS, hall);
    }

    public int parseInt(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            Slog.d(TAG, "Invalid integer argument " + argument);
            return -1;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        pw.print("mScene=");
        pw.println(this.mScene);
        pw.print(prefix);
        pw.print("mLidOpen=");
        pw.println(this.mLidOpen);
        pw.print(prefix);
        pw.println("mStylusPageKeyConfig");
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mEnable = ");
        pw.println(this.mStylusPageKeyConfig.isEnable());
        pw.print(prefix2);
        pw.print("mAppWhiteSet.size = ");
        pw.println(this.mStylusPageKeyConfig.getAppWhiteSet().size());
        pw.print(prefix2);
        pw.print("mActivityBlackSet.size = ");
        pw.println(this.mStylusPageKeyConfig.getActivityBlackSet().size());
        pw.print(prefix2);
        pw.print("StylusScreenOffQuickNoteDefault = ");
        pw.println(STYLUS_SCREEN_OFF_QUICK_NOTE_DEFAULT);
        pw.print(prefix2);
        pw.print("IsSupportScreenOffQuickNote = ");
        pw.println(MiuiStylusUtils.isSupportOffScreenQuickNote());
        pw.print(prefix2);
        pw.print("mIsScreenOffQuickNoteOn = ");
        pw.println(this.mIsScreenOffQuickNoteOn);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        private static final int MSG_HALL = 6;
        private static final int MSG_KEY_DISMISS_WITHOUT_ANIMATION = 3;
        private static final int MSG_KEY_DISMISS_WITH_ANIMATION = 4;
        private static final int MSG_OFF_SCREEN_QUICK_NOTE = 5;
        private static final int MSG_QUICK_NOTE_KEY_LONG_PRESS_ACTION = 1;
        private static final int MSG_SCREEN_SHOT_KEY_LONG_PRESS_ACTION = 2;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiStylusShortcutManager.this.mQuickNoteKeyFunctionTriggered = true;
                    MiuiStylusShortcutManager.this.addStylusMaskWindow();
                    return;
                case 2:
                    MiuiStylusShortcutManager.this.mScreenShotKeyFunctionTriggered = true;
                    MiuiStylusShortcutManager.this.triggerScreenShotKeyFunction();
                    return;
                case 3:
                    MiuiStylusShortcutManager.this.removeStylusMaskWindow(false);
                    return;
                case 4:
                    MiuiStylusShortcutManager.this.removeStylusMaskWindow(true);
                    return;
                case 5:
                    MiuiStylusShortcutManager.this.triggerNoteWhenScreenOff();
                    return;
                case 6:
                    int hall = msg.arg1;
                    MiuiStylusShortcutManager.this.putHallSettings(hall);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class MiuiSettingsObserver extends ContentObserver {
        MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            MiuiStylusShortcutManager.this.updateGameModeSettings();
            Handler handler = MiuiStylusShortcutManager.this.mHandler;
            final MiuiStylusShortcutManager miuiStylusShortcutManager = MiuiStylusShortcutManager.this;
            handler.post(new Runnable() { // from class: com.miui.server.input.stylus.MiuiStylusShortcutManager$MiuiSettingsObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiStylusShortcutManager.this.updateCloudConfig();
                }
            });
            ContentResolver resolver = MiuiStylusShortcutManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), false, this, -1);
            resolver.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, this, -1);
            if (MiuiStylusUtils.isSupportOffScreenQuickNote()) {
                resolver.registerContentObserver(Settings.System.getUriFor(MiuiStylusShortcutManager.KEY_STYLUS_SCREEN_OFF_QUICK_NOTE), true, this, -1);
                MiuiStylusShortcutManager.this.updateStylusScreenOffQuickNoteSettings();
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Secure.getUriFor("gb_boosting").equals(uri)) {
                MiuiStylusShortcutManager.this.updateGameModeSettings();
                Slog.d(MiuiStylusShortcutManager.TAG, "game_booster changed, isGameMode = " + MiuiStylusShortcutManager.this.mIsGameMode);
            } else if (MiuiSettings.SettingsCloudData.getCloudDataNotifyUri().equals(uri)) {
                MiuiStylusShortcutManager.this.updateCloudConfig();
            } else if (Settings.System.getUriFor(MiuiStylusShortcutManager.KEY_STYLUS_SCREEN_OFF_QUICK_NOTE).equals(uri)) {
                MiuiStylusShortcutManager.this.updateStylusScreenOffQuickNoteSettings();
            }
        }
    }
}
