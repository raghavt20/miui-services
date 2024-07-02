package com.android.server.policy;

import android.R;
import android.app.ActivityManager;
import android.app.BroadcastOptions;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Slog;
import android.view.IDisplayFoldListener;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.LocalServices;
import com.android.server.cameracovered.CameraBlackCoveredManager;
import com.android.server.contentcatcher.WakeUpContentCatcherManager;
import com.android.server.display.DisplayManagerServiceStub;
import com.android.server.display.DisplayPowerControllerStub;
import com.android.server.input.InputManagerServiceStub;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.KeyboardCombinationManagerStubImpl;
import com.android.server.input.MiInputPhotoHandleManager;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.TouchWakeUpFeatureManager;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProvider;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.input.pocketmode.MiuiPocketModeManager;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.android.server.input.shortcut.ShortcutOneTrackHelper;
import com.android.server.input.shoulderkey.ShoulderKeyManagerInternal;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.tof.TofManagerInternal;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowState;
import com.android.server.wm.WindowStateStubImpl;
import com.miui.server.AccessController;
import com.miui.server.input.AutoDisableScreenButtonsManager;
import com.miui.server.input.MiuiBackTapGestureService;
import com.miui.server.input.MiuiFingerPrintTapListener;
import com.miui.server.input.PadManager;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.knock.MiuiKnockGestureService;
import com.miui.server.input.magicpointer.MiuiMagicPointerServiceInternal;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import com.miui.server.input.stylus.MiuiStylusUtils;
import com.miui.server.input.stylus.blocker.MiuiEventBlockerManager;
import com.miui.server.input.time.MiuiTimeFloatingWindow;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.input.util.ShortCutActionsUtils;
import com.miui.server.stability.StabilityLocalServiceInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import miui.android.animation.internal.AnimTask;
import miui.hardware.input.InputFeature;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.provider.SettingsStringUtil;
import miui.util.FeatureParser;
import miui.util.HapticFeedbackUtil;
import miui.util.ITouchFeature;
import miui.util.MiuiMultiDisplayTypeInfo;
import miui.util.SmartCoverManager;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public abstract class BaseMiuiPhoneWindowManager extends PhoneWindowManager {
    private static final int ACCESSIBLE_MODE_LARGE_DENSITY = 461;
    private static final int ACCESSIBLE_MODE_SMALL_DENSITY = 352;
    private static final int ACTION_DOUBLE_CLICK = 1;
    private static final String ACTION_DUMPSYS = "dumpsys_by_power";
    private static final String ACTION_FACTORY_RESET = "key_long_press_volume_up";
    private static final int ACTION_LONGPRESS = 2;
    private static final String ACTION_PARTIAL_SCREENSHOT = "android.intent.action.CAPTURE_PARTIAL_SCREENSHOT";
    private static final int ACTION_SINGLE_CLICK = 0;
    private static final int BTN_MOUSE = 272;
    private static final int COMBINE_VOLUME_KEY_DELAY_TIME = 150;
    private static final boolean DEBUG = false;
    private static final int DOUBLE_CLICK_AI_KEY_TIME = 300;
    private static final int ENABLE_HOME_KEY_DOUBLE_TAP_INTERVAL = 300;
    private static final int ENABLE_VOLUME_KEY_PRESS_COUNTS = 2;
    private static final int ENABLE_VOLUME_KEY_PRESS_INTERVAL = 300;
    public static final int FLAG_INJECTED_FROM_SHORTCUT = 16777216;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    protected static final int INTERCEPT_EXPECTED_RESULT_GO_TO_SLEEP = -1;
    protected static final int INTERCEPT_EXPECTED_RESULT_NONE = 0;
    protected static final int INTERCEPT_EXPECTED_RESULT_WAKE_UP = 1;
    public static final String IS_CUSTOM_SHORTCUTS_EFFECTIVE = "is_custom_shortcut_effective";
    private static final String IS_MI_INPUT_EVENT_TIME_LINE_ENABLE = "is_mi_input_event_time_line_enable";
    private static final int KEYCODE_AI = 689;
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int KEY_LONG_PRESS_TIMEOUT_DELAY_FOR_CAMERA_KEY = 300;
    private static final int KEY_LONG_PRESS_TIMEOUT_DELAY_FOR_SHOW_MENU = 50;
    private static final String KID_MODE = "kid_mode_status";
    private static final int KID_MODE_STATUE_EXIT = 0;
    private static final int KID_MODE_STATUS_IN = 1;
    private static final String KID_SPACE_ID = "kid_user_id";
    private static final String LAST_POWER_UP_EVENT = "power_up_event";
    private static final String LAST_POWER_UP_POLICY = "power_up_policy";
    private static final String LAST_POWER_UP_SCREEN_STATE = "power_up_screen_state";
    private static final String LONG_LONG_PRESS_POWER_KEY = "long_press_power_key_four_second";
    private static final int LONG_PRESS_AI_KEY_TIME = 500;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_NONE = 0;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_PAY = 2;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_STREET_SNAP = 1;
    private static final int META_KEY_PRESS_INTERVAL = 300;
    private static final int MSG_COMBINE_VOLUME_KEY_DELAY_TIME = 3000;
    private static final String PERMISSION_INTERNAL_GENERAL_API = "miui.permission.USE_INTERNAL_GENERAL_API";
    protected static final String REASON_FP_DPAD_CENTER_WAKEUP = "miui.policy:FINGERPRINT_DPAD_CENTER";
    private static final String SYNERGY_MODE = "synergy_mode";
    private static final String SYSTEM_SETTINGS_VR_MODE = "vr_mode";
    static final ArrayList<Integer> sScreenRecorderKeyEventList;
    static final ArrayList<Integer> sVoiceAssistKeyEventList;
    boolean mAccessibilityShortcutOnLockScreen;
    private SettingsStringUtil.SettingStringHelper mAccessibilityShortcutSetting;
    private PowerManager.WakeLock mAiKeyWakeLock;
    private AudioManager mAudioManager;
    private AutoDisableScreenButtonsManager mAutoDisableScreenButtonsManager;
    private ProgressBar mBootProgress;
    private String[] mBootText;
    private TextView mBootTextView;
    boolean mCameraKeyWakeScreen;
    private int mCurrentUserId;
    private boolean mDoubleClickAiKeyIsConsumed;
    boolean mDpadCenterDown;
    private EdgeSuppressionManager mEdgeSuppressionManager;
    protected WindowManagerPolicy.WindowState mFocusedWindow;
    private boolean mFolded;
    private boolean mForbidFullScreen;
    protected boolean mFrontFingerprintSensor;
    protected Handler mHandler;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private boolean mHasWatchedRotation;
    boolean mHaveBankCard;
    boolean mHaveTranksCard;
    private PowerManager.WakeLock mHelpKeyWakeLock;
    boolean mHomeConsumed;
    boolean mHomeDoubleClickPending;
    private final Runnable mHomeDoubleClickTimeoutRunnable;
    boolean mHomeDoubleTapPending;
    private final Runnable mHomeDoubleTapTimeoutRunnable;
    boolean mHomeDownAfterDpCenter;
    private String mImperceptiblePowerKey;
    private InputFeature mInputFeature;
    private int mInputMethodWindowVisibleHeight;
    private BroadcastReceiver mInternalBroadcastReceiver;
    private boolean mIsFoldChanged;
    private boolean mIsKidMode;
    protected boolean mIsStatusBarVisibleInFullscreen;
    private boolean mIsSupportGloablTounchDirection;
    private boolean mIsSynergyMode;
    private boolean mIsVRMode;
    protected int mKeyLongPressTimeout;
    private boolean mLongPressAiKeyIsConsumed;
    boolean mMikeymodeEnabled;
    private MiuiBackTapGestureService mMiuiBackTapGestureService;
    private Dialog mMiuiBootMsgDialog;
    private MiuiFingerPrintTapListener mMiuiFingerPrintTapListener;
    private MiuiInputManagerInternal mMiuiInputManagerInternal;
    private MiuiKeyInterceptExtend mMiuiKeyInterceptExtend;
    protected MiuiKeyShortcutRuleManager mMiuiKeyShortcutRuleManager;
    protected MiuiKeyguardServiceDelegate mMiuiKeyguardDelegate;
    private MiuiKnockGestureService mMiuiKnockGestureService;
    private MiuiPadKeyboardManager mMiuiPadKeyboardManager;
    private MiuiPocketModeManager mMiuiPocketModeManager;
    protected MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    protected MiuiStylusShortcutManager mMiuiStylusShortcutManager;
    protected MiuiMultiFingerGestureManager mMiuiThreeGestureListener;
    private MiuiTimeFloatingWindow mMiuiTimeFloatingWindow;
    int mNavBarHeight;
    int mNavBarHeightLand;
    int mNavBarWidth;
    private MiuiScreenOnProximityLock mProximitySensor;
    private RotationWatcher mRotationWatcher;
    protected int mScreenOffReason;
    BroadcastReceiver mScreenRecordeEnablekKeyEventReceiver;
    private boolean mScreenRecorderEnabled;
    private MiuiSettingsObserver mSettingsObserver;
    private ShortcutOneTrackHelper mShortcutOneTrackHelper;
    boolean mShortcutServiceIsTalkBack;
    private ShoulderKeyManagerInternal mShoulderKeyManagerInternal;
    private StabilityLocalServiceInternal mStabilityLocalServiceInternal;
    protected boolean mSupportTapFingerprintSensorToHome;
    private HashSet<String> mSystemKeyPackages;
    boolean mTalkBackIsOpened;
    boolean mTestModeEnabled;
    private TofManagerInternal mTofManagerInternal;
    private int mTrackDumpLogKeyCodeLastKeyCode;
    private boolean mTrackDumpLogKeyCodePengding;
    private long mTrackDumpLogKeyCodeStartTime;
    private int mTrackDumpLogKeyCodeTimeOut;
    private int mTrackDumpLogKeyCodeVolumeDownTimes;
    boolean mTrackballWakeScreen;
    private volatile long mUpdateWakeUpDetailTime;
    private boolean mVoiceAssistEnabled;
    private long mVolumeButtonPrePressedTime;
    private long mVolumeButtonPressedCount;
    private boolean mVolumeDownKeyConsumed;
    private boolean mVolumeDownKeyPressed;
    private long mVolumeDownKeyTime;
    private PowerManager.WakeLock mVolumeKeyUpWakeLock;
    private PowerManager.WakeLock mVolumeKeyWakeLock;
    private boolean mVolumeUpKeyConsumed;
    private boolean mVolumeUpKeyPressed;
    private long mVolumeUpKeyTime;
    private WakeUpContentCatcherManager mWakeUpContentCatcherManager;
    private volatile String mWakeUpDetail;
    private volatile String mWakeUpReason;
    private boolean mWifiOnly;
    private WindowManagerPolicy.WindowState mWin;
    private static final boolean IS_FOLD_DEVICE = MiuiMultiDisplayTypeInfo.isFoldDevice();
    private static final boolean IS_FLIP_DEVICE = MiuiMultiDisplayTypeInfo.isFlipDevice();
    private static PhoneWindowManagerFeatureImpl phoneWindowManagerFeature = new PhoneWindowManagerFeatureImpl();
    private static final int SHORTCUT_HOME_POWER = getKeyBitmask(3) | getKeyBitmask(26);
    private static final int SHORTCUT_BACK_POWER = getKeyBitmask(4) | getKeyBitmask(26);
    private static final int SHORTCUT_MENU_POWER = getKeyBitmask(187) | getKeyBitmask(26);
    private static final int SHORTCUT_SCREENSHOT_ANDROID = getKeyBitmask(26) | getKeyBitmask(25);
    private static final int SHORTCUT_SCREENSHOT_MIUI = getKeyBitmask(187) | getKeyBitmask(25);
    private static final int SHORTCUT_SCREENSHOT_SINGLE_KEY = getKeyBitmask(3) | getKeyBitmask(25);
    private static final int SHORTCUT_UNLOCK = getKeyBitmask(4) | getKeyBitmask(24);
    protected static final boolean SUPPORT_EDGE_TOUCH_VOLUME = FeatureParser.getBoolean("support_edge_touch_volume", false);
    protected static final boolean SUPPORT_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private static final boolean IS_CETUS = "cetus".equals(Build.DEVICE);
    private static final List<ComponentName> TALK_BACK_SERVICE_LIST = new ArrayList(Arrays.asList(new ComponentName("com.google.android.marvin.talkback", "com.google.android.marvin.talkback.TalkBackService"), new ComponentName("com.google.android.marvin.talkback", ".TalkBackService")));
    private static final String mPhoneProduct = SystemProperties.get("ro.product.device", "null");
    private int mDoubleClickAiKeyCount = 0;
    private long mLastClickAiKeyTime = 0;
    private int mLongPressVolumeDownBehavior = 0;
    Runnable mPowerLongPressOriginal = phoneWindowManagerFeature.getPowerLongPress(this);
    BroadcastReceiver mBootCompleteReceiver = new AnonymousClass2();
    private final IDisplayFoldListener mIDisplayFoldListener = new IDisplayFoldListener.Stub() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.3
        public void onDisplayFoldChanged(int displayId, boolean folded) {
            if (BaseMiuiPhoneWindowManager.this.mFolded != folded) {
                BaseMiuiPhoneWindowManager.this.mFolded = folded;
                BaseMiuiPhoneWindowManager.this.mIsFoldChanged = true;
                BaseMiuiPhoneWindowManager.this.setTouchFeatureRotation();
            }
            MiuiInputLog.defaults("display changed,display=" + displayId + " fold=" + folded + " mIsFoldChanged=" + BaseMiuiPhoneWindowManager.this.mIsFoldChanged);
            if (BaseMiuiPhoneWindowManager.this.mMiuiBackTapGestureService != null) {
                BaseMiuiPhoneWindowManager.this.mMiuiBackTapGestureService.notifyFoldStatus(folded);
            }
            if (BaseMiuiPhoneWindowManager.this.mProximitySensor != null) {
                BaseMiuiPhoneWindowManager.this.mProximitySensor.release(true);
            }
            if (BaseMiuiPhoneWindowManager.this.mMiuiThreeGestureListener != null) {
                BaseMiuiPhoneWindowManager.this.mMiuiThreeGestureListener.notifyFoldStatus(folded);
            }
            ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).notifyFoldStatus(folded);
        }
    };
    private SmartCoverManager mSmartCoverManager = new SmartCoverManager();
    private Runnable mSingleClickAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.4
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            MiuiInputLog.detail("single click ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_single_click_ai_button_settings");
        }
    };
    private Runnable mDoubleClickAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.5
        @Override // java.lang.Runnable
        public void run() {
            MiuiInputLog.detail("double click ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_double_click_ai_button_settings");
        }
    };
    private Runnable mLongPressDownAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.6
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            BaseMiuiPhoneWindowManager.this.mLongPressAiKeyIsConsumed = true;
            MiuiInputLog.detail("long press down ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_long_press_down_ai_button_settings");
        }
    };
    private Runnable mLongPressUpAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.7
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            MiuiInputLog.detail("long press up ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_long_press_up_ai_button_settings");
        }
    };
    private final MiuiPocketModeSensorWrapper.ProximitySensorChangeListener mWakeUpKeySensorListener = new MiuiPocketModeSensorWrapper.ProximitySensorChangeListener() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.8
        @Override // com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper.ProximitySensorChangeListener
        public void onSensorChanged(boolean tooClose) {
            BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager.unregisterListener();
            if (tooClose) {
                Slog.w("BaseMiuiPhoneWindowManager", "Going to sleep due to KEYCODE_WAKEUP/KEYCODE_DPAD_CENTER: proximity sensor too close");
                ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("go_to_sleep", ShortCutActionsUtils.REASON_OF_TRIGGERED_BY_PROXIMITY_SENSOR, null, false);
            }
        }
    };
    private int mMetaKeyAction = 0;
    private boolean mMetaKeyConsume = true;
    private boolean mSystemShortcutsMenuShown = false;
    private int mKeyBoardDeviceId = -1;
    private final Runnable mMetaKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.10
        @Override // java.lang.Runnable
        public void run() {
            if (!BaseMiuiPhoneWindowManager.this.mMetaKeyConsume) {
                BaseMiuiPhoneWindowManager.this.mMetaKeyConsume = true;
                MiuiInputLog.major(" mMetaKeyAction : " + BaseMiuiPhoneWindowManager.this.mMetaKeyAction);
                Intent intent = null;
                if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 0) {
                    intent = new Intent("com.miui.home.action.SINGLE_CLICK");
                    intent.setPackage("com.miui.home");
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardShortcut(MiuiKeyboardUtil.KeyBoardShortcut.SHOW_DOCK);
                } else if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 1) {
                    intent = new Intent("com.miui.home.action.DOUBLE_CLICK");
                    intent.setPackage("com.miui.home");
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardShortcut(MiuiKeyboardUtil.KeyBoardShortcut.TOGGLE_RECENT_APP);
                } else if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 2) {
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.showKeyboardShortcutsMenu(baseMiuiPhoneWindowManager.mKeyBoardDeviceId, true, true);
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardShortcut(MiuiKeyboardUtil.KeyBoardShortcut.SHOW_SHORTCUT_LIST);
                }
                if (intent != null) {
                    BaseMiuiPhoneWindowManager.this.sendAsyncBroadcast(intent, "miui.permission.USE_INTERNAL_GENERAL_API");
                }
            }
        }
    };
    protected List<String> mFpNavEventNameList = null;
    private boolean mKeyguardOnWhenHomeDown = false;
    private Binder mBinder = new Binder();
    BroadcastReceiver mStatusBarExitFullscreenReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.11
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.setStatusBarInFullscreen(false);
        }
    };
    BroadcastReceiver mScreenshotReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.12
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.takeScreenshot(intent, "screen_shot");
        }
    };
    BroadcastReceiver mPartialScreenshotReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.13
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.takeScreenshot(intent, "partial_screen_shot");
        }
    };

    protected abstract int callSuperInterceptKeyBeforeQueueing(KeyEvent keyEvent, int i, boolean z);

    protected abstract void cancelPreloadRecentAppsInternal();

    protected abstract void finishActivityInternal(IBinder iBinder, int i, Intent intent) throws RemoteException;

    protected abstract void forceStopPackage(String str, int i, String str2);

    protected abstract WindowManagerPolicy.WindowState getKeyguardWindowState();

    protected abstract int getWakePolicyFlag();

    protected abstract boolean interceptPowerKeyByFingerPrintKey();

    protected abstract boolean isFingerPrintKey(KeyEvent keyEvent);

    protected abstract boolean isScreenOnInternal();

    protected abstract void launchAssistActionInternal(String str, Bundle bundle);

    protected abstract void launchRecentPanelInternal();

    protected abstract void onStatusBarPanelRevealed(IStatusBarService iStatusBarService);

    protected abstract void preloadRecentAppsInternal();

    protected abstract int processFingerprintNavigationEvent(KeyEvent keyEvent, boolean z);

    protected abstract boolean screenOffBecauseOfProxSensor();

    protected abstract void toggleSplitScreenInternal();

    public BaseMiuiPhoneWindowManager() {
        HashSet<String> hashSet = new HashSet<>();
        this.mSystemKeyPackages = hashSet;
        hashSet.add("android");
        this.mSystemKeyPackages.add(AccessController.PACKAGE_SYSTEMUI);
        this.mSystemKeyPackages.add("com.android.phone");
        this.mSystemKeyPackages.add("com.android.mms");
        this.mSystemKeyPackages.add("com.android.contacts");
        this.mSystemKeyPackages.add("com.miui.home");
        this.mSystemKeyPackages.add("com.jeejen.family.miui");
        this.mSystemKeyPackages.add("com.android.incallui");
        this.mSystemKeyPackages.add("com.miui.backup");
        this.mSystemKeyPackages.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        this.mSystemKeyPackages.add("com.xiaomi.mihomemanager");
        this.mSystemKeyPackages.add("com.miui.securityadd");
        this.mHomeDoubleTapTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.18
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mHomeDoubleTapPending) {
                    BaseMiuiPhoneWindowManager.this.mHomeDoubleTapPending = false;
                    BaseMiuiPhoneWindowManager.this.launchHomeFromHotKey(0);
                }
            }
        };
        this.mHomeDoubleClickTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.19
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mHomeDoubleClickPending) {
                    BaseMiuiPhoneWindowManager.this.mHomeDoubleClickPending = false;
                }
            }
        };
        this.mScreenRecorderEnabled = false;
        this.mScreenRecordeEnablekKeyEventReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.20
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean enable = intent.getBooleanExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, false);
                Slog.i("WindowManager", "mScreenRecordeEnablekKeyEventReceiver enable=" + enable);
                BaseMiuiPhoneWindowManager.this.setScreenRecorderEnabled(enable);
            }
        };
        this.mTrackDumpLogKeyCodePengding = false;
        this.mTrackDumpLogKeyCodeStartTime = 0L;
        this.mTrackDumpLogKeyCodeLastKeyCode = 25;
        this.mTrackDumpLogKeyCodeTimeOut = 2000;
        this.mTrackDumpLogKeyCodeVolumeDownTimes = 0;
        this.mVoiceAssistEnabled = false;
        this.mInternalBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.21
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode".equals(action)) {
                    BaseMiuiPhoneWindowManager.this.mForbidFullScreen = true;
                } else if ("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode".equals(action)) {
                    BaseMiuiPhoneWindowManager.this.mForbidFullScreen = false;
                }
            }
        };
    }

    static {
        ArrayList<Integer> arrayList = new ArrayList<>();
        sScreenRecorderKeyEventList = arrayList;
        arrayList.add(3);
        arrayList.add(4);
        arrayList.add(82);
        arrayList.add(187);
        arrayList.add(24);
        arrayList.add(25);
        arrayList.add(26);
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        sVoiceAssistKeyEventList = arrayList2;
        arrayList2.add(4);
    }

    private static int getKeyBitmask(int keycode) {
        switch (keycode) {
            case 3:
                return 8;
            case 4:
                return 16;
            case 24:
                return 128;
            case 25:
                return 64;
            case 26:
                return 32;
            case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                return 2;
            case 187:
                return 4;
            default:
                return 1;
        }
    }

    private int getCodeByKeyPressed(int keyPressed) {
        switch (keyPressed) {
            case 2:
                return 82;
            case 4:
                return 187;
            case 8:
                return 3;
            case 16:
                return 4;
            case 32:
                return 26;
            case 64:
                return 25;
            case 128:
                return 24;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void initInternal(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, IWindowManager windowManager) {
        Resources res = context.getResources();
        this.mNavBarWidth = res.getDimensionPixelSize(R.dimen.preference_child_padding_side);
        this.mNavBarHeight = res.getDimensionPixelSize(R.dimen.pip_minimized_visible_size);
        this.mNavBarHeightLand = res.getDimensionPixelSize(R.dimen.preference_breadcrumb_paddingRight);
        boolean hasSupportGlobalTouchDirection = ITouchFeature.getInstance().hasSupportGlobalTouchDirection();
        this.mIsSupportGloablTounchDirection = hasSupportGlobalTouchDirection;
        if (hasSupportGlobalTouchDirection || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
            handleTouchFeatureRotationWatcher();
        }
        this.mHandler = new H();
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        InputFeature inputFeature = InputFeature.getInstance(context);
        this.mInputFeature = inputFeature;
        inputFeature.init();
        this.mSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mEdgeSuppressionManager = EdgeSuppressionManager.getInstance(context);
        this.mMiuiShortcutTriggerHelper = MiuiShortcutTriggerHelper.getInstance(context);
        this.mMiuiKeyShortcutRuleManager = MiuiKeyShortcutRuleManager.getInstance(this.mContext, this.mSingleKeyGestureDetector, this.mKeyCombinationManager, this.mMiuiShortcutTriggerHelper);
        this.mShortcutOneTrackHelper = ShortcutOneTrackHelper.getInstance(this.mContext);
        if (MiuiStylusUtils.supportStylusGesture()) {
            this.mMiuiStylusShortcutManager = MiuiStylusShortcutManager.getInstance();
        }
        this.mMiuiKeyInterceptExtend = MiuiKeyInterceptExtend.getInstance(this.mContext);
        this.mSettingsObserver.observe();
        phoneWindowManagerFeature.setPowerLongPress(this, new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.1
            @Override // java.lang.Runnable
            public void run() {
                BaseMiuiPhoneWindowManager.this.mPowerLongPressOriginal.run();
            }
        });
        this.mVolumeKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mVolumeKeyWakeLock");
        this.mAiKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mAiKeyWakeLock");
        this.mHelpKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mHelpKeyWakeLock");
        this.mVolumeKeyUpWakeLock = this.mPowerManager.newWakeLock(805306394, "Reset");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CAPTURE_SCREENSHOT");
        context.registerReceiverAsUser(this.mScreenshotReceiver, UserHandle.ALL, filter, "miui.permission.USE_INTERNAL_GENERAL_API", null, 2);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_PARTIAL_SCREENSHOT);
        context.registerReceiverAsUser(this.mPartialScreenshotReceiver, UserHandle.ALL, filter2, "miui.permission.USE_INTERNAL_GENERAL_API", null, 2);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("com.miui.app.ExtraStatusBarManager.EXIT_FULLSCREEN");
        context.registerReceiver(this.mStatusBarExitFullscreenReceiver, filter3, 2);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction("miui.intent.SCREEN_RECORDER_ENABLE_KEYEVENT");
        context.registerReceiver(this.mScreenRecordeEnablekKeyEventReceiver, filter4, 2);
        IntentFilter filter5 = new IntentFilter();
        filter5.addAction("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode");
        filter5.addAction("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode");
        context.registerReceiverAsUser(this.mInternalBroadcastReceiver, UserHandle.ALL, filter5, "miui.permission.USE_INTERNAL_GENERAL_API", this.mHandler, 2);
        IntentFilter filter6 = new IntentFilter();
        filter6.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBootCompleteReceiver, filter6, 2);
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, false);
        this.mAutoDisableScreenButtonsManager = new AutoDisableScreenButtonsManager(context, this.mHandler);
        this.mSmartCoverManager.init(context, this.mPowerManager, this.mHandler);
        saveWindowTypeLayer(context);
        this.mMiuiTimeFloatingWindow = new MiuiTimeFloatingWindow(this.mContext);
        this.mWakeUpContentCatcherManager = new WakeUpContentCatcherManager(this.mContext);
    }

    private void saveWindowTypeLayer(Context context) {
        JSONObject typeLayers = new JSONObject();
        int[] types = {2000, 2001, 2013};
        for (int type : types) {
            int layer = getWindowLayerFromTypeLw(type);
            if (layer != 2) {
                try {
                    typeLayers.put(Integer.toString(type), layer);
                } catch (JSONException ex) {
                    Slog.e("WindowManager", "JSONException", ex);
                }
            }
        }
        MiuiSettings.System.putString(context.getContentResolver(), "window_type_layer", typeLayers.toString());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void systemReadyInternal() {
        this.mMiuiFingerPrintTapListener = new MiuiFingerPrintTapListener(this.mContext);
        MiuiMultiFingerGestureManager miuiMultiFingerGestureManager = new MiuiMultiFingerGestureManager(this.mContext, this.mHandler);
        this.mMiuiThreeGestureListener = miuiMultiFingerGestureManager;
        miuiMultiFingerGestureManager.initKeyguardActiveFunction(new BooleanSupplier() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda3
            @Override // java.util.function.BooleanSupplier
            public final boolean getAsBoolean() {
                return BaseMiuiPhoneWindowManager.this.getKeyguardActive();
            }
        });
        this.mMiuiKnockGestureService = new MiuiKnockGestureService(this.mContext);
        this.mMiuiBackTapGestureService = new MiuiBackTapGestureService(this.mContext);
        this.mStabilityLocalServiceInternal = (StabilityLocalServiceInternal) LocalServices.getService(StabilityLocalServiceInternal.class);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = (ShoulderKeyManagerInternal) LocalServices.getService(ShoulderKeyManagerInternal.class);
        this.mShoulderKeyManagerInternal = shoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.systemReady();
        }
        PackageManager pm = this.mContext.getPackageManager();
        boolean configAntiMisTouchDisabled = FeatureParser.getBoolean("config_antiMistouchDisabled", false);
        if (MiuiPocketModeManager.isSupportInertialAndLightSensor(this.mContext) || (pm != null && pm.hasSystemFeature("android.hardware.sensor.proximity") && !DeviceFeature.hasSupportAudioPromity() && !configAntiMisTouchDisabled)) {
            this.mProximitySensor = new MiuiScreenOnProximityLock(this.mContext, this.mMiuiKeyguardDelegate, this.mHandler.getLooper());
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "torch_state", 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), "auto_test_mode_on", 0);
        this.mIsVRMode = false;
        Settings.System.putInt(this.mContext.getContentResolver(), SYSTEM_SETTINGS_VR_MODE, 0);
        this.mFrontFingerprintSensor = FeatureParser.getBoolean("front_fingerprint_sensor", false);
        this.mSupportTapFingerprintSensorToHome = FeatureParser.getBoolean("support_tap_fingerprint_sensor_to_home", false);
        this.mFpNavEventNameList = new ArrayList();
        String[] strArray = FeatureParser.getStringArray("fp_nav_event_name_list");
        if (strArray != null) {
            for (String str : strArray) {
                this.mFpNavEventNameList.add(str);
            }
        }
        Settings.Global.putStringForUser(this.mContext.getContentResolver(), "policy_control", "immersive.preconfirms=*", -2);
        if (Settings.System.getInt(this.mContext.getContentResolver(), "persist.camera.snap.enable", 0) == 1) {
            Settings.System.putInt(this.mContext.getContentResolver(), "persist.camera.snap.enable", 0);
            if (!this.mHaveTranksCard) {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "key_long_press_volume_down", "Street-snap", this.mCurrentUserId);
            } else {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "key_long_press_volume_down", "none", this.mCurrentUserId);
            }
        }
        this.mSettingsObserver.onChange(false);
        this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_CONFIGURATION);
        MiuiPadKeyboardManager keyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        this.mMiuiPadKeyboardManager = keyboardManager;
        if (keyboardManager != null) {
            LocalServices.addService(MiuiPadKeyboardManager.class, keyboardManager);
            PadManager.getInstance().registerPointerEventListener();
        }
        if (IS_FOLD_DEVICE || IS_FLIP_DEVICE) {
            registerDisplayFoldListener(this.mIDisplayFoldListener);
        }
        this.mTofManagerInternal = (TofManagerInternal) LocalServices.getService(TofManagerInternal.class);
        if (InputFeature.supportPhotoHandle()) {
            MiInputPhotoHandleManager.getInstance(this.mContext).init();
        }
    }

    public void startedWakingUp(int displayGroupId, int why) {
        super.startedWakingUp(displayGroupId, why);
        if (displayGroupId != 0) {
            return;
        }
        if (this.mMiuiPocketModeManager == null) {
            this.mMiuiPocketModeManager = new MiuiPocketModeManager(this.mContext);
        }
        if (this.mProximitySensor != null && isDeviceProvisioned() && !MiuiSettings.System.isInSmallWindowMode(this.mContext) && this.mMiuiPocketModeManager.getPocketModeEnableSettings() && !this.mIsVRMode && !this.mIsSynergyMode && !skipPocketModeAquireByWakeUpInfo(this.mWakeUpDetail, this.mWakeUpReason)) {
            this.mProximitySensor.aquire();
        }
        this.mMiuiKnockGestureService.updateScreenState(true);
        this.mMiuiThreeGestureListener.updateScreenState(true);
        this.mMiuiTimeFloatingWindow.updateScreenState(true);
        this.mMiuiBackTapGestureService.notifyScreenOn();
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.updateScreenState(true);
        }
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.updateScreenState(true);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyScreenState(true);
        }
        this.mMiuiInputManagerInternal.setScreenState(true);
        this.mMiuiKeyInterceptExtend.setScreenState(true);
    }

    public void setWakeUpInfo(String detail, String reason) {
        this.mWakeUpDetail = detail;
        this.mWakeUpReason = reason;
        this.mUpdateWakeUpDetailTime = System.currentTimeMillis();
    }

    private boolean skipPocketModeAquireByWakeUpInfo(String detail, String wakeUpReason) {
        long currentTime = System.currentTimeMillis();
        boolean validData = currentTime > this.mUpdateWakeUpDetailTime && currentTime - this.mUpdateWakeUpDetailTime < 100;
        if ("android.policy:FINGERPRINT".equals(detail)) {
            return validData;
        }
        if (MiuiScreenOnProximityLock.SKIP_AQUIRE_UNFOLD_WAKE_UP_DETAIL.equals(detail)) {
            return validData && PowerManager.wakeReasonToString(12).equals(wakeUpReason);
        }
        return false;
    }

    public void screenTurningOn(int displayId, WindowManagerPolicy.ScreenOnListener screenOnListener) {
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate;
        super.screenTurningOn(displayId, screenOnListener);
        if (screenOnListener == null && (miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate) != null) {
            miuiKeyguardServiceDelegate.onScreenTurnedOnWithoutListener();
        }
        if (MiuiSettings.System.IS_FOLD_DEVICE && displayId == 0) {
            DisplayManagerServiceStub.getInstance().screenTurningOn();
        }
    }

    public void screenTurningOff(int displayId, WindowManagerPolicy.ScreenOffListener screenOffListener) {
        super.screenTurningOff(displayId, screenOffListener);
        if (MiuiSettings.System.IS_FOLD_DEVICE && displayId == 0) {
            DisplayManagerServiceStub.getInstance().screenTurningOff();
        }
    }

    public void screenTurnedOff(int displayId, boolean isSwappingDisplay) {
        super.screenTurnedOff(displayId, isSwappingDisplay);
        startCameraProcess();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.policy.BaseMiuiPhoneWindowManager$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.AnonymousClass2.this.lambda$onReceive$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).bootComplete();
        }
    }

    private void startCameraProcess() {
        try {
            Intent cameraIntent = new Intent("miui.action.CAMERA_EMPTY_SERVICE");
            cameraIntent.setPackage(AccessController.PACKAGE_CAMERA);
            this.mContext.startServiceAsUser(cameraIntent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e("WindowManager", "IllegalAccessException", e);
        }
    }

    public void startedGoingToSleep(int displayGroupId, int why) {
        super.startedGoingToSleep(displayGroupId, why);
        if (displayGroupId != 0) {
            return;
        }
        this.mMiuiKnockGestureService.updateScreenState(false);
        this.mMiuiThreeGestureListener.updateScreenState(false);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.updateScreenState(false);
        }
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.updateScreenState(false);
        }
        this.mMiuiTimeFloatingWindow.updateScreenState(false);
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyScreenState(false);
        }
        this.mMiuiInputManagerInternal.setScreenState(false);
        this.mMiuiKeyInterceptExtend.setScreenState(false);
    }

    public void finishedGoingToSleep(int displayGroupId, int why) {
        screenTurnedOffInternal(why);
        this.mMiuiBackTapGestureService.notifyScreenOff();
        this.mEdgeSuppressionManager.finishedGoingToSleep();
        releaseScreenOnProximitySensor(true);
        if (MiuiSettings.System.IS_FOLD_DEVICE && displayGroupId == 0) {
            DisplayManagerServiceStub.getInstance().finishedGoingToSleep();
        }
        super.finishedGoingToSleep(displayGroupId, why);
    }

    public void finishedWakingUp(int displayGroupId, int why) {
        super.finishedWakingUp(displayGroupId, why);
        this.mEdgeSuppressionManager.finishedWakingUp();
    }

    protected void screenTurnedOffInternal(int why) {
        this.mScreenOffReason = why;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        this.mSmartCoverManager.notifyLidSwitchChanged(lidOpen, this.mFolded);
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyLidSwitchChanged(lidOpen);
        }
        PadManager.getInstance().setIsLidOpen(lidOpen);
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.notifyLidSwitchChanged(lidOpen);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseScreenOnProximitySensor(boolean isNowRelease) {
        MiuiScreenOnProximityLock miuiScreenOnProximityLock = this.mProximitySensor;
        if (miuiScreenOnProximityLock != null) {
            miuiScreenOnProximityLock.release(isNowRelease);
        }
    }

    public long interceptKeyBeforeDispatching(IBinder focusedToken, KeyEvent event, int policyFlags) {
        boolean cancelKeyFunction;
        int type;
        int repeatCount = event.getRepeatCount();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate;
        boolean keyguardActive = miuiKeyguardServiceDelegate != null && miuiKeyguardServiceDelegate.isShowingAndNotHidden();
        if (!this.mKeyCombinationManager.isKeyConsumed(event) || this.mMiuiShortcutTriggerHelper.skipKeyGesutre(event)) {
            int interceptType = this.mMiuiKeyInterceptExtend.getKeyInterceptTypeBeforeDispatching(event, policyFlags, this.mFocusedWindow);
            if (interceptType == 1) {
                MiuiInputLog.defaults("pass the key to app!");
                return 0L;
            }
            if (interceptType == 2) {
                MiuiInputLog.defaults("send the key to aosp!");
                return super.interceptKeyBeforeDispatching(focusedToken, event, policyFlags);
            }
            if (interceptType == 4) {
                return -1L;
            }
            WindowState winStateFromInputWinMap = WindowStateStubImpl.getWinStateFromInputWinMap(this.mWindowManager, focusedToken);
            if (down && repeatCount == 0) {
                this.mWin = winStateFromInputWinMap;
            }
            if ((!this.mMiuiShortcutTriggerHelper.interceptKeyByLongPress(event) || (event.getFlags() & FLAG_INJECTED_FROM_SHORTCUT) != 0) && !canceled) {
                cancelKeyFunction = false;
            } else {
                cancelKeyFunction = true;
            }
            if (event.getAction() == 1 && (event.getFlags() & FLAG_INJECTED_FROM_SHORTCUT) == 0) {
                this.mMiuiShortcutTriggerHelper.resetKeyCodeByLongPress();
            }
            int keyCode = event.getKeyCode();
            if (keyCode == 82) {
                if (this.mTestModeEnabled) {
                    MiuiInputLog.major("Ignoring MENU because mTestModeEnabled = " + this.mTestModeEnabled);
                    return 0L;
                }
                if (isLockDeviceWindow(winStateFromInputWinMap)) {
                    MiuiInputLog.major("device locked, pass MENU to lock window");
                    return 0L;
                }
                if (!this.mMiuiShortcutTriggerHelper.isPressToAppSwitch()) {
                    MiuiInputLog.major("is show menu");
                    return 0L;
                }
                if (!cancelKeyFunction) {
                    if (this.mMiuiShortcutTriggerHelper.isPressToAppSwitch() && (event.getFlags() & FLAG_INJECTED_FROM_SHORTCUT) == 0) {
                        if (!keyguardOn()) {
                            if (down) {
                                sendFullScreenStateToTaskSnapshot();
                                preloadRecentApps();
                                return -1L;
                            }
                            if (repeatCount == 0) {
                                this.mShortcutOneTrackHelper.trackShortcutEventTrigger("screen_key_press_app_switch", "launch_recents");
                                launchRecentPanel();
                                return -1L;
                            }
                            cancelPreloadRecentAppsInternal();
                            return -1L;
                        }
                        return -1L;
                    }
                    MiuiInputLog.major("is show menu");
                    return 0L;
                }
                if (!keyguardOn() && this.mMiuiShortcutTriggerHelper.isPressToAppSwitch()) {
                    cancelPreloadRecentAppsInternal();
                }
                MiuiInputLog.error("Ignoring MENU; event canceled or intercepted by long press");
                return -1L;
            }
            if (keyCode == 3) {
                if (this.mTestModeEnabled) {
                    MiuiInputLog.major("Ignoring HOME because mTestModeEnabled = " + this.mTestModeEnabled);
                    return 0L;
                }
                if (isLockDeviceWindow(winStateFromInputWinMap)) {
                    MiuiInputLog.major("device locked, pass HOME to lock window");
                    return 0L;
                }
                if (cancelKeyFunction) {
                    MiuiInputLog.error("Ignoring HOME; event canceled or intercepted by long press.");
                    return -1L;
                }
                if (!down) {
                    if (this.mHomeConsumed) {
                        this.mHomeConsumed = false;
                        return -1L;
                    }
                    if (phoneWindowManagerFeature.isScreenOnFully(this)) {
                        if (keyguardActive) {
                            StatusBarManager sbm = (StatusBarManager) this.mContext.getSystemService("statusbar");
                            sbm.collapsePanels();
                        }
                        if (this.mKeyguardOnWhenHomeDown) {
                            MiuiInputLog.major("Ignoring HOME; keyguard is on when first Home down");
                        } else {
                            if (this.mMiuiShortcutTriggerHelper.mDoubleTapOnHomeBehavior != 0 && !keyguardActive) {
                                this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                                this.mHomeDoubleTapPending = true;
                                this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, 300L);
                                return -1L;
                            }
                            MiuiInputLog.error("before go to home");
                            Intent intent = new Intent(CameraBlackCoveredManager.ACTION_LAUNCH_HOME_FROM_HOTKEY);
                            sendAsyncBroadcast(intent, "miui.permission.USE_INTERNAL_GENERAL_API");
                            launchHomeFromHotKey(0);
                        }
                    }
                } else {
                    sendFullScreenStateToHome();
                    WindowManager.LayoutParams attrs = winStateFromInputWinMap != null ? (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(winStateFromInputWinMap, "getAttrs", new Object[0]) : null;
                    if (attrs != null && ((type = attrs.type) == 2004 || type == 2009)) {
                        return 0L;
                    }
                }
                if (repeatCount == 0) {
                    if (this.mHomeDoubleTapPending) {
                        this.mHomeDoubleTapPending = false;
                        this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                        handleDoubleTapOnHome();
                        return -1L;
                    }
                    if (this.mMiuiShortcutTriggerHelper.mDoubleTapOnHomeBehavior == 1) {
                        preloadRecentApps();
                        return -1L;
                    }
                    return -1L;
                }
                return -1L;
            }
            if (keyCode == 4 && cancelKeyFunction) {
                MiuiInputLog.error("Ignoring BACK; event canceled or intercepted by long press.");
                return -1L;
            }
            if (keyCode == 25 && this.mVolumeDownKeyConsumed) {
                if (!down) {
                    this.mVolumeDownKeyConsumed = false;
                    return -1L;
                }
                return -1L;
            }
            if (keyCode == 24 && this.mVolumeUpKeyConsumed) {
                if (!down) {
                    this.mVolumeUpKeyConsumed = false;
                    return -1L;
                }
                return -1L;
            }
            MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
            if (miuiStylusShortcutManager != null && miuiStylusShortcutManager.needInterceptBeforeDispatching(event)) {
                return this.mMiuiStylusShortcutManager.getDelayTime(event);
            }
            if (interceptType == 3) {
                MiuiInputLog.defaults("Skip aosp key's logic");
                return 0L;
            }
            return super.interceptKeyBeforeDispatching(focusedToken, event, policyFlags);
        }
        MiuiInputLog.defaults("is key consumed,keyCode=" + event.getKeyCode() + " downTime=" + event.getDownTime());
        return -1L;
    }

    private boolean isInCallScreenShowing() {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        return "com.android.phone.MiuiInCallScreen".equals(runningActivity) || "com.android.incallui.InCallActivity".equals(runningActivity);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markShortcutTriggered() {
        phoneWindowManagerFeature.callInterceptPowerKeyUp(this, false);
    }

    public static void sendRecordCountEvent(Context context, String category, String event) {
        Intent intent = new Intent("com.miui.gallery.intent.action.SEND_STAT");
        intent.setPackage(AccessController.PACKAGE_GALLERY);
        intent.putExtra("stat_type", "count_event");
        intent.putExtra("category", category);
        intent.putExtra("event", event);
        context.sendBroadcast(intent);
    }

    private Toast makeAllUserToastAndShow(String text, int duration) {
        Toast toast = Toast.makeText(this.mContext, text, duration);
        toast.show();
        return toast;
    }

    protected boolean stopLockTaskMode() {
        return false;
    }

    protected boolean isInLockTaskMode() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startAiKeyService(String pressType) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.ai.AidaemonService"));
            intent.putExtra("key_ai_button_settings", pressType);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e("WindowManager", e.toString());
        }
    }

    private void handleAiKeyEvent(KeyEvent event, boolean down) {
        if (down) {
            long keyDownTime = SystemClock.uptimeMillis();
            if (event.getRepeatCount() == 0) {
                this.mDoubleClickAiKeyIsConsumed = false;
                this.mLongPressAiKeyIsConsumed = false;
                this.mDoubleClickAiKeyCount++;
                this.mHandler.postDelayed(this.mLongPressDownAiKeyRunnable, 500L);
            }
            if (keyDownTime - this.mLastClickAiKeyTime < 300 && this.mDoubleClickAiKeyCount == 2) {
                this.mHandler.post(this.mDoubleClickAiKeyRunnable);
                this.mDoubleClickAiKeyIsConsumed = true;
                this.mDoubleClickAiKeyCount = 0;
                this.mHandler.removeCallbacks(this.mSingleClickAiKeyRunnable);
                this.mHandler.removeCallbacks(this.mLongPressDownAiKeyRunnable);
            }
            this.mLastClickAiKeyTime = keyDownTime;
            return;
        }
        if (this.mLongPressAiKeyIsConsumed) {
            this.mHandler.post(this.mLongPressUpAiKeyRunnable);
        } else if (!this.mDoubleClickAiKeyIsConsumed) {
            this.mHandler.postDelayed(this.mSingleClickAiKeyRunnable, 300L);
            this.mHandler.removeCallbacks(this.mLongPressDownAiKeyRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int intercept(KeyEvent event, int policyFlags, boolean isScreenOn, int expectedResult) {
        boolean down = event.getAction() == 0;
        if (!down) {
            cancelEventAndCallSuperQueueing(event, policyFlags, isScreenOn);
        }
        return expectedResult;
    }

    protected void registerProximitySensor() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.9
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager == null) {
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.mMiuiPocketModeManager = new MiuiPocketModeManager(baseMiuiPhoneWindowManager.mContext);
                }
                BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager.registerListener(BaseMiuiPhoneWindowManager.this.mWakeUpKeySensorListener);
            }
        });
    }

    private boolean shouldInterceptKey(KeyEvent event, int policyFlags, boolean isScreenOn) {
        int keyCode = event.getKeyCode();
        if (this.mIsVRMode) {
            MiuiInputLog.major("VR mode drop all keys.");
            return true;
        }
        if (SystemProperties.getInt("sys.in_shutdown_progress", 0) == 1) {
            MiuiInputLog.major("this device is being shut down, ignore key event.");
            return true;
        }
        if (!isScreenOn && (4 == keyCode || 82 == keyCode)) {
            MiuiInputLog.major("Cancel back or menu key when screen is off");
            cancelEventAndCallSuperQueueing(event, policyFlags, false);
            return true;
        }
        InputDevice inputDevice = event.getDevice();
        if (inputDevice == null || inputDevice.getProductId() != 1576) {
            return false;
        }
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.handleShoulderKeyEvent(event);
        }
        return true;
    }

    private void cancelEventAndCallSuperQueueing(KeyEvent event, int policyFlags, boolean isScreenOn) {
        callSuperInterceptKeyBeforeQueueing(KeyEvent.changeFlags(event, event.getFlags() | 32), policyFlags, isScreenOn);
    }

    private void notifyPowerKeeperKeyEvent(KeyEvent event) {
    }

    private void handleMetaKey(KeyEvent event) {
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        if (KeyEvent.isMetaKey(keyCode)) {
            if (!isUserSetupComplete() || !PadManager.getInstance().isPad()) {
                return;
            }
            if (down) {
                this.mHandler.removeCallbacks(this.mMetaKeyRunnable);
                this.mKeyBoardDeviceId = event.getDeviceId();
                if (this.mMetaKeyConsume) {
                    this.mMetaKeyConsume = false;
                    this.mMetaKeyAction = 2;
                    this.mHandler.postDelayed(this.mMetaKeyRunnable, 300L);
                    return;
                } else {
                    this.mMetaKeyAction = 1;
                    this.mHandler.post(this.mMetaKeyRunnable);
                    return;
                }
            }
            if (!this.mMetaKeyConsume && this.mMetaKeyAction != 1) {
                this.mMetaKeyAction = 0;
                return;
            } else {
                if (this.mSystemShortcutsMenuShown) {
                    showKeyboardShortcutsMenu(this.mKeyBoardDeviceId, true, false);
                    return;
                }
                return;
            }
        }
        if (!this.mMetaKeyConsume) {
            this.mMetaKeyConsume = true;
        } else if (this.mSystemShortcutsMenuShown) {
            showKeyboardShortcutsMenu(this.mKeyBoardDeviceId, true, false);
        }
    }

    private boolean isMiPad() {
        return PadManager.getInstance().isPad();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showRecentApps(boolean triggeredFromAltTab) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showRecentApps(triggeredFromAltTab);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showKeyboardShortcutsMenu(int deviceId, boolean system, boolean show) {
        MiuiInputLog.major("toggleKeyboardShortcutsMenu ,deviceId: " + deviceId + "  system:" + system + " show:" + show);
        if (system) {
            this.mSystemShortcutsMenuShown = show;
        }
        Intent intent = new Intent("com.miui.systemui.action.KEYBOARD_SHORTCUTS");
        intent.setPackage(AccessController.PACKAGE_SYSTEMUI);
        intent.putExtra("system", system);
        intent.putExtra("show", show);
        intent.putExtra("deviceId", deviceId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Removed duplicated region for block: B:182:0x03cf A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:183:0x03d0  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public int interceptKeyBeforeQueueingInternal(android.view.KeyEvent r23, int r24, boolean r25) {
        /*
            Method dump skipped, instructions count: 1096
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.BaseMiuiPhoneWindowManager.interceptKeyBeforeQueueingInternal(android.view.KeyEvent, int, boolean):int");
    }

    private void removePowerLongPress() {
        removeKeyFunction(ACTION_DUMPSYS);
        removeKeyFunction(this.mImperceptiblePowerKey);
    }

    private void postPowerLongPress() {
        String longPressPowerFunction = this.mMiuiKeyShortcutRuleManager.getFunction("long_press_power_key");
        postKeyFunction(ACTION_DUMPSYS, AnimTask.MAX_SINGLE_TASK_SIZE, LONG_LONG_PRESS_POWER_KEY);
        if (TextUtils.isEmpty(longPressPowerFunction) || "none".equals(longPressPowerFunction)) {
            postKeyFunction(this.mImperceptiblePowerKey, (int) getImperceptiblePowerKeyTimeOut(), "imperceptible_press_power_key");
        }
    }

    private void removeVolumeUpLongPress() {
        if ("zhuque".equals(Build.DEVICE)) {
            removeKeyFunction(ACTION_FACTORY_RESET);
        }
    }

    private void postVolumeUpLongPress() {
        postKeyFunction(ACTION_FACTORY_RESET, 5000, "");
    }

    private long getImperceptiblePowerKeyTimeOut() {
        return SystemProperties.getLong("ro.miui.imp_power_time", this.mMiuiKeyShortcutRuleManager.getSingleKeyRule(26).getMiuiLongPressTimeoutMs());
    }

    private void postKeyFunction(String action, int delay, String shortcut) {
        if (TextUtils.isEmpty(action)) {
            MiuiInputLog.defaults("action is null");
            return;
        }
        Message message = this.mHandler.obtainMessage(1, action);
        if (action == ACTION_DUMPSYS) {
            message.setAsynchronous(true);
        }
        Bundle bundle = new Bundle();
        bundle.putString("shortcut", shortcut);
        message.setData(bundle);
        this.mHandler.sendMessageDelayed(message, delay);
    }

    private void removeKeyFunction(String action) {
        this.mHandler.removeMessages(1, action);
    }

    private void interceptAccessibilityShortcutChord(boolean keyguardActive) {
        if (this.mVolumeDownKeyPressed && this.mVolumeUpKeyPressed && this.mTalkBackIsOpened) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mVolumeDownKeyTime + 150 && now <= this.mVolumeUpKeyTime + 150) {
                this.mVolumeDownKeyConsumed = true;
                this.mVolumeUpKeyConsumed = true;
                if (!this.mShortcutServiceIsTalkBack || (keyguardActive && !this.mAccessibilityShortcutOnLockScreen)) {
                    Handler handler = this.mHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(1, "close_talkback"), 3000L);
                }
            }
        }
    }

    private void cancelPendingAccessibilityShortcutAction() {
        this.mHandler.removeMessages(1, "close_talkback");
    }

    public PhoneWindowManager.PowerKeyRule getOriginalPowerKeyRule() {
        return this.mPowerKeyRule;
    }

    private void streetSnap(boolean isScreenOn, int keyCode, boolean down, KeyEvent event) {
        if (!isScreenOn && this.mLongPressVolumeDownBehavior == 1) {
            Intent keyIntent = null;
            if (keyCode == 24 || keyCode == 25) {
                keyIntent = new Intent("miui.intent.action.CAMERA_KEY_BUTTON");
            } else if (down && keyCode == 26) {
                keyIntent = new Intent("android.intent.action.KEYCODE_POWER_UP");
            }
            if (keyIntent != null) {
                keyIntent.setClassName(AccessController.PACKAGE_CAMERA, "com.android.camera.snap.SnapKeyReceiver");
                keyIntent.putExtra("key_code", keyCode);
                keyIntent.putExtra("key_action", event.getAction());
                keyIntent.putExtra("key_event_time", event.getEventTime());
                this.mContext.sendBroadcastAsUser(keyIntent, UserHandle.CURRENT);
            }
        }
    }

    private boolean shouldInterceptHeadSetHookKey(int keyCode, KeyEvent event) {
        if (this.mMikeymodeEnabled && keyCode == 79) {
            Intent mikeyIntent = new Intent("miui.intent.action.MIKEY_BUTTON");
            mikeyIntent.setPackage("com.xiaomi.miclick");
            mikeyIntent.putExtra("key_action", event.getAction());
            mikeyIntent.putExtra("key_event_time", event.getEventTime());
            this.mContext.sendBroadcast(mikeyIntent);
            return true;
        }
        return false;
    }

    private boolean sendOthersBroadcast(boolean down, boolean isScreenOn, boolean keyguardActive, int keyCode, KeyEvent event) {
        IStatusBarService statusBarService;
        if (down) {
            if (isScreenOn && !keyguardActive && (keyCode == 26 || keyCode == 25 || keyCode == 24 || keyCode == 164 || keyCode == 85 || keyCode == 79)) {
                Intent i = new Intent("miui.intent.action.KEYCODE_EXTERNAL");
                i.putExtra("android.intent.extra.KEY_EVENT", event);
                i.addFlags(1073741824);
                sendAsyncBroadcast(i, true);
            }
            boolean stopNotification = keyCode == 26;
            if (!stopNotification && keyguardActive && (keyCode == 25 || keyCode == 24 || keyCode == 164)) {
                stopNotification = true;
            }
            if (stopNotification && this.mSystemReady && (statusBarService = getStatusBarService()) != null) {
                onStatusBarPanelRevealed(statusBarService);
            }
            if (keyCode == 25 || keyCode == 24) {
                ContentResolver cr = this.mContext.getContentResolver();
                String proc = Settings.System.getString(cr, "remote_control_proc_name");
                String pkg = Settings.System.getString(cr, "remote_control_pkg_name");
                if (proc != null && pkg != null) {
                    SystemClock.uptimeMillis();
                    boolean running = checkProcessRunning(proc);
                    if (!running) {
                        Settings.System.putString(cr, "remote_control_proc_name", null);
                        Settings.System.putString(cr, "remote_control_pkg_name", null);
                    } else {
                        Intent i2 = new Intent("miui.intent.action.REMOTE_CONTROL");
                        i2.setPackage(pkg);
                        i2.addFlags(1073741824);
                        i2.putExtra("android.intent.extra.KEY_EVENT", event);
                        sendAsyncBroadcast(i2);
                        return true;
                    }
                }
            }
        } else if (keyCode == 26) {
            sendAsyncBroadcast(new Intent("android.intent.action.KEYCODE_POWER_UP"));
        }
        return false;
    }

    private boolean inFingerprintEnrolling() {
        String topClassName;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        try {
            topClassName = am.getRunningTasks(1).get(0).topActivity.getClassName();
        } catch (Exception e) {
            MiuiInputLog.error("Exception", e);
        }
        return "com.android.settings.NewFingerprintInternalActivity".equals(topClassName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStatusBarInFullscreen(boolean show) {
        this.mIsStatusBarVisibleInFullscreen = show;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.disable(show ? Integer.MIN_VALUE : 0, this.mBinder, "android");
            }
        } catch (RemoteException e) {
            Slog.e("WindowManager", "RemoteException", e);
            this.mStatusBarService = null;
        }
    }

    protected void registerStatusBarInputEventReceiver() {
    }

    protected void unregisterStatusBarInputEventReceiver() {
    }

    /* loaded from: classes.dex */
    protected class StatusBarPointEventTracker {
        private float mDownX = -1.0f;
        private float mDownY = -1.0f;

        public StatusBarPointEventTracker() {
        }

        protected void onTrack(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                    float statusBarExpandHeight = motionEvent.getRawX();
                    this.mDownX = statusBarExpandHeight;
                    this.mDownY = motionEvent.getRawY();
                    return;
                case 1:
                case 2:
                case 3:
                    float statusBarExpandHeight2 = BaseMiuiPhoneWindowManager.this.mContext.getResources().getFraction(285802496, 0, 0);
                    float f = this.mDownY;
                    if (statusBarExpandHeight2 >= f && f != -1.0f) {
                        float distanceX = Math.abs(this.mDownX - motionEvent.getRawX());
                        float distanceY = Math.abs(this.mDownY - motionEvent.getRawY());
                        if (2.0f * distanceX <= distanceY && MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X <= distanceY) {
                            BaseMiuiPhoneWindowManager.this.setStatusBarInFullscreen(true);
                            this.mDownY = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void takeScreenshot(final Intent intent, final String function) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BaseMiuiPhoneWindowManager.this.lambda$takeScreenshot$0(function, intent);
            }
        }, intent.getLongExtra("capture_delay", 0L));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$takeScreenshot$0(String function, Intent intent) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, intent.getStringExtra("shortcut"), null, false);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        static final int MSG_DISPATCH_SHOW_RECENTS = 3;
        static final int MSG_KEY_DELAY_POWER = 2;
        static final int MSG_KEY_FUNCTION = 1;

        private H() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (BaseMiuiPhoneWindowManager.LONG_LONG_PRESS_POWER_KEY.equals(msg.getData().getString("shortcut"))) {
                MiuiInputLog.defaults("trigger dump power log");
                if (BaseMiuiPhoneWindowManager.this.mStabilityLocalServiceInternal != null) {
                    BaseMiuiPhoneWindowManager.this.mStabilityLocalServiceInternal.crawlLogsByPower();
                    return;
                }
                return;
            }
            if (BaseMiuiPhoneWindowManager.this.mIsKidMode) {
                MiuiInputLog.defaults("Children's space does not trigger shortcut gestures");
                return;
            }
            if (msg.what == 2) {
                BaseMiuiPhoneWindowManager.this.callSuperInterceptKeyBeforeQueueing((KeyEvent) msg.getData().getParcelable(BaseMiuiPhoneWindowManager.LAST_POWER_UP_EVENT), msg.getData().getInt(BaseMiuiPhoneWindowManager.LAST_POWER_UP_POLICY), msg.getData().getBoolean(BaseMiuiPhoneWindowManager.LAST_POWER_UP_SCREEN_STATE));
            } else if (msg.what == 1) {
                if (!BaseMiuiPhoneWindowManager.this.mPowerManager.isScreenOn()) {
                    return;
                }
                String shortcut = msg.getData().getString("shortcut");
                boolean triggered = false;
                String action = (String) msg.obj;
                if (action == null) {
                    return;
                }
                if (!BaseMiuiPhoneWindowManager.this.isUserSetupComplete() && !"dump_log".equals(action) && !"find_device_locate".equals(action) && !"close_talkback".equals(action)) {
                    MiuiInputLog.defaults("user setup not complete, does not trigger shortcut");
                    return;
                }
                if ("launch_camera".equals(action)) {
                    triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_camera", shortcut, null, true);
                } else {
                    boolean z = false;
                    if ("screen_shot".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("screen_shot", shortcut, null, false);
                        BaseMiuiPhoneWindowManager.sendRecordCountEvent(BaseMiuiPhoneWindowManager.this.mContext, "screenshot", "key_shortcut");
                    } else if ("partial_screen_shot".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("partial_screen_shot", shortcut, null, false);
                    } else if ("go_to_sleep".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("go_to_sleep", shortcut, null, true);
                    } else if ("turn_on_torch".equals(action)) {
                        TelecomManager telecomManager = BaseMiuiPhoneWindowManager.this.getTelecommService();
                        if (BaseMiuiPhoneWindowManager.this.mWifiOnly || (telecomManager != null && telecomManager.getCallState() == 0)) {
                            z = true;
                        }
                        boolean phoneIdle = z;
                        if (phoneIdle) {
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("turn_on_torch", shortcut, null, true);
                        }
                    } else if ("close_app".equals(action)) {
                        "close_app".equals(BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutRuleManager.getFunction("long_press_back_key"));
                        BaseMiuiPhoneWindowManager.this.mShortcutOneTrackHelper.trackShortcutEventTrigger(shortcut, "close_app");
                        triggered = BaseMiuiPhoneWindowManager.this.triggerCloseApp(action);
                    } else if ("show_menu".equals(action)) {
                        BaseMiuiPhoneWindowManager.this.mShortcutOneTrackHelper.trackShortcutEventTrigger(shortcut, "show_menu");
                        triggered = BaseMiuiPhoneWindowManager.this.showMenu();
                    } else if ("dump_log".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("dump_log", shortcut, null, true);
                    } else if ("launch_recents".equals(action)) {
                        BaseMiuiPhoneWindowManager.this.mShortcutOneTrackHelper.trackShortcutEventTrigger(shortcut, "launch_recents");
                        triggered = BaseMiuiPhoneWindowManager.this.triggerLaunchRecent(action);
                        BaseMiuiPhoneWindowManager.this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
                    } else if ("close_talkback".equals(action)) {
                        BaseMiuiPhoneWindowManager.this.mShortcutOneTrackHelper.trackShortcutEventTrigger(shortcut, "close_talkback");
                        BaseMiuiPhoneWindowManager.this.closeTalkBack();
                        triggered = true;
                    } else if ("find_device_locate".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("find_device_locate", shortcut, null, false);
                    } else if (BaseMiuiPhoneWindowManager.ACTION_FACTORY_RESET.equals(action)) {
                        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
                        intent.setPackage("android");
                        intent.addFlags(268435456);
                        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true);
                        intent.putExtra("com.android.internal.intent.extra.WIPE_ESIMS", true);
                        BaseMiuiPhoneWindowManager.this.mContext.sendBroadcast(intent);
                    }
                }
                if (triggered) {
                    BaseMiuiPhoneWindowManager.this.markShortcutTriggered();
                }
            } else if (msg.what == 3) {
                boolean triggeredFromAltTab = ((Boolean) msg.obj).booleanValue();
                BaseMiuiPhoneWindowManager.this.showRecentApps(triggeredFromAltTab);
            }
            super.handleMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeTalkBack() {
        if (this.mTalkBackIsOpened) {
            ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("miui_talkback", "key_combination_volume_up_volume_down", null, true);
        }
    }

    public void onDefaultDisplayFocusChangedLw(WindowManagerPolicy.WindowState newFocus) {
        TofManagerInternal tofManagerInternal;
        super.onDefaultDisplayFocusChangedLw(newFocus);
        String packageName = newFocus != null ? newFocus.getOwningPackage() : null;
        if (this.mSystemReady && (tofManagerInternal = this.mTofManagerInternal) != null) {
            tofManagerInternal.onDefaultDisplayFocusChanged(packageName);
        }
        this.mFocusedWindow = newFocus;
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.onDefaultDisplayFocusChangedLw(newFocus);
            MiuiEventBlockerManager.getInstance().onFocusedWindowChanged(newFocus);
        }
        MiuiMultiFingerGestureManager miuiMultiFingerGestureManager = this.mMiuiThreeGestureListener;
        if (miuiMultiFingerGestureManager != null) {
            miuiMultiFingerGestureManager.onFocusedWindowChanged(newFocus);
        }
        if (newFocus != null && MiuiSettings.System.IS_FOLD_DEVICE) {
            String focusedPackageName = newFocus.getOwningPackage();
            if (DisplayPowerControllerStub.getInstance() != null) {
                DisplayPowerControllerStub.getInstance().notifyFocusedWindowChanged(focusedPackageName);
            }
        }
        MiuiMagicPointerServiceInternal service = (MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class);
        if (service != null) {
            service.onFocusedWindowChanged(newFocus);
        }
    }

    public boolean triggerCloseApp(String action) {
        MiuiInputLog.defaults("trigger close app reason:" + action);
        return closeApp();
    }

    private boolean closeApp() {
        WindowManager.LayoutParams attrs;
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(windowState, "getAttrs", new Object[0])) == null) {
            return false;
        }
        int type = attrs.type;
        if ((type < 1 || type > 99) && (type < 1000 || type > 1999)) {
            return false;
        }
        String title = null;
        String packageName = attrs.packageName;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            String className = attrs.getTitle().toString();
            int index = className.lastIndexOf(47);
            if (index >= 0) {
                ComponentName componentName = new ComponentName(packageName, (String) className.subSequence(index + 1, className.length()));
                ActivityInfo activityInfo = pm.getActivityInfo(componentName, 0);
                title = activityInfo.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            MiuiInputLog.error("NameNotFoundException", e);
        }
        try {
            if (TextUtils.isEmpty(title)) {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
                title = applicationInfo.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e2) {
            MiuiInputLog.error("NameNotFoundException", e2);
        }
        if (TextUtils.isEmpty(title)) {
            title = packageName;
        }
        if (packageName.equals("com.miui.home")) {
            MiuiInputLog.major("The current window is the home,not need to close app");
            return true;
        }
        if (this.mSystemKeyPackages.contains(packageName)) {
            makeAllUserToastAndShow(this.mContext.getString(286196216, title), 0);
            MiuiInputLog.major("The current window is the system window,not need to close app");
            return true;
        }
        try {
            finishActivityInternal(attrs.token, 0, null);
        } catch (RemoteException e3) {
            MiuiInputLog.error("RemoteException", e3);
        }
        forceStopPackage(packageName, -2, "key shortcut");
        MiuiInputLog.major("The 'close app' interface was called successfully");
        makeAllUserToastAndShow(this.mContext.getString(286196215, title), 0);
        return true;
    }

    public boolean launchAssistAction(String hint, Bundle args) {
        sendCloseSystemWindows("assist");
        if (!isUserSetupComplete()) {
            return false;
        }
        if ((this.mContext.getResources().getConfiguration().uiMode & 15) == 4) {
            ((SearchManager) this.mContext.getSystemService("search")).launchAssist(args);
            return true;
        }
        launchAssistActionInternal(hint, args);
        return true;
    }

    public boolean launchRecents() {
        return launchRecentPanel();
    }

    public boolean launchHome() {
        launchHomeFromHotKey(0);
        return true;
    }

    public boolean launchRecentPanel() {
        sendCloseSystemWindows("recentapps");
        if (keyguardOn()) {
            return false;
        }
        launchRecentPanelInternal();
        return true;
    }

    private void preloadRecentApps() {
        preloadRecentAppsInternal();
    }

    public boolean triggerLaunchRecent(String action) {
        MiuiInputLog.defaults("trigger launch recent,reason:" + action);
        preloadRecentApps();
        return launchRecentPanel();
    }

    public boolean triggerShowMenu(String action) {
        MiuiInputLog.defaults("trigger show menu,reason:" + action);
        return showMenu();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean showMenu() {
        this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
        markShortcutTriggered();
        injectEventFromShortcut(82);
        MiuiInputLog.major("show menu,the inject event is KEYCODE_MENU ");
        return false;
    }

    private void injectEvent(int injectKeyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent homeDown = new KeyEvent(now, now, 0, injectKeyCode, 0, 0, -1, 0);
        KeyEvent homeUp = new KeyEvent(now, now, 1, injectKeyCode, 0, 0, -1, 0);
        InputManager.getInstance().injectInputEvent(homeDown, 0);
        InputManager.getInstance().injectInputEvent(homeUp, 0);
    }

    private void injectEventFromShortcut(int injectKeyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, 0, injectKeyCode, 0, 0, -1, 0);
        down.setFlags(FLAG_INJECTED_FROM_SHORTCUT);
        KeyEvent up = new KeyEvent(now, now, 1, injectKeyCode, 0, 0, -1, 0);
        up.setFlags(FLAG_INJECTED_FROM_SHORTCUT);
        InputManager.getInstance().injectInputEvent(down, 0);
        InputManager.getInstance().injectInputEvent(up, 0);
    }

    private AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        return this.mAudioManager;
    }

    private void playSoundEffect(int policyFlags, int keyCode, boolean down, int repeatCount) {
        if (down && (policyFlags & 2) != 0 && repeatCount == 0 && !this.mVibrator.hasVibrator() && !hasNavigationBar()) {
            switch (keyCode) {
                case 3:
                case 4:
                case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                case 84:
                case 187:
                    playSoundEffect();
                    return;
                default:
                    return;
            }
        }
    }

    private boolean playSoundEffect() {
        AudioManager audioManager = getAudioManager();
        if (audioManager == null) {
            return false;
        }
        audioManager.playSoundEffect(0);
        return true;
    }

    public boolean performHapticFeedback(int uid, String packageName, int effectId, boolean always, String reason) {
        if (this.mHapticFeedbackUtil.isSupportedEffect(effectId)) {
            return this.mHapticFeedbackUtil.performHapticFeedback(packageName, effectId, always);
        }
        return super.performHapticFeedback(uid, packageName, effectId, always, reason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGameMode() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "gb_boosting", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTouchFeatureRotation() {
        int rotation = this.mContext.getDisplay().getRotation();
        Slog.d("WindowManager", "set rotation = " + rotation);
        int targetId = 0;
        if (MiuiSettings.System.IS_FOLD_DEVICE && this.mFolded) {
            targetId = 1;
        }
        ITouchFeature.getInstance().setTouchMode(targetId, 8, rotation);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTouchFeatureRotationWatcher() {
        if (this.mRotationWatcher == null) {
            this.mRotationWatcher = new RotationWatcher();
        }
        if (this.mIsSupportGloablTounchDirection || isGameMode() || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
            setTouchFeatureRotation();
            if (!this.mHasWatchedRotation) {
                try {
                    this.mWindowManager.watchRotation(this.mRotationWatcher, this.mContext.getDisplay().getDisplayId());
                    this.mHasWatchedRotation = true;
                    return;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return;
                }
            }
            return;
        }
        try {
            this.mWindowManager.removeRotationWatcher(this.mRotationWatcher);
            this.mHasWatchedRotation = false;
        } catch (RemoteException e2) {
            e2.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyKidSpaceChanged(boolean isInKidMode) {
        this.mIsKidMode = isInKidMode;
        this.mMiuiKeyInterceptExtend.setKidSpaceMode(isInKidMode);
        ShortCutActionsUtils.getInstance(this.mContext).notifyKidSpaceChanged(isInKidMode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RotationWatcher extends IRotationWatcher.Stub {
        RotationWatcher() {
        }

        public void onRotationChanged(int i) throws RemoteException {
            Slog.d("WindowManager", "rotation changed = " + i);
            int targetId = 0;
            if (MiuiSettings.System.IS_FOLD_DEVICE && BaseMiuiPhoneWindowManager.this.mFolded) {
                targetId = 1;
            }
            ITouchFeature.getInstance().setTouchMode(targetId, 8, i);
            if (!BaseMiuiPhoneWindowManager.this.isGameMode() && EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
                BaseMiuiPhoneWindowManager.this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_ROTATION);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("trackball_wake_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("camera_key_preferred_action_type"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("camera_key_preferred_action_shortcut_id"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("volumekey_wake_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_long_press_volume_down"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("auto_test_mode_on"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_bank_card_in_ese"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_trans_card_in_ese"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(BaseMiuiPhoneWindowManager.SYSTEM_SETTINGS_VR_MODE), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enable_mikey_mode"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_accessibility_services"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_target_service"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_on_lock_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("three_gesture_down"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("three_gesture_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("send_back_when_xiaoai_appear"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("long_press_timeout"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor(BaseMiuiPhoneWindowManager.KID_MODE), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.KID_SPACE_ID), false, this, -1);
            if ((!BaseMiuiPhoneWindowManager.this.mIsSupportGloablTounchDirection || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) && DeviceFeature.SUPPORT_GAME_MODE) {
                resolver.registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), false, this, -1);
            }
            resolver.registerContentObserver(Settings.System.getUriFor(BaseMiuiPhoneWindowManager.IS_CUSTOM_SHORTCUTS_EFFECTIVE), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.SYNERGY_MODE), false, this, -1);
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$MiuiSettingsObserver$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.MiuiSettingsObserver.this.lambda$observe$0();
                }
            });
            resolver.registerContentObserver(Settings.System.getUriFor(BaseMiuiPhoneWindowManager.IS_MI_INPUT_EVENT_TIME_LINE_ENABLE), false, this, -1);
            onChange(false, Settings.Global.getUriFor("imperceptible_press_power_key"));
            onChange(false, Settings.Secure.getUriFor("long_press_timeout"));
            onChange(false, Settings.System.getUriFor(BaseMiuiPhoneWindowManager.IS_CUSTOM_SHORTCUTS_EFFECTIVE));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$observe$0() {
            Settings.System.putIntForUser(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), BaseMiuiPhoneWindowManager.IS_MI_INPUT_EVENT_TIME_LINE_ENABLE, 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            if (Settings.Secure.getUriFor("gb_boosting").equals(uri)) {
                if (EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE && !BaseMiuiPhoneWindowManager.this.isGameMode()) {
                    BaseMiuiPhoneWindowManager.this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_GAMEBOOSTER);
                    return;
                } else {
                    BaseMiuiPhoneWindowManager.this.handleTouchFeatureRotationWatcher();
                    return;
                }
            }
            if (Settings.Global.getUriFor("imperceptible_press_power_key").equals(uri)) {
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager.mImperceptiblePowerKey = MiuiSettings.Key.getKeyAndGestureShortcutFunction(baseMiuiPhoneWindowManager.mContext, "imperceptible_press_power_key");
                return;
            }
            if (Settings.Secure.getUriFor("long_press_timeout").equals(uri)) {
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager2 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager2.mKeyLongPressTimeout = Settings.Secure.getIntForUser(resolver, "long_press_timeout", 0, baseMiuiPhoneWindowManager2.mCurrentUserId);
                return;
            }
            if (Settings.Global.getUriFor(BaseMiuiPhoneWindowManager.KID_MODE).equals(uri) || Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.KID_SPACE_ID).equals(uri)) {
                boolean isKidMode = Settings.Global.getInt(resolver, BaseMiuiPhoneWindowManager.KID_MODE, 0) == 1;
                if (!isKidMode) {
                    int kidSpaceId = Settings.Secure.getIntForUser(resolver, BaseMiuiPhoneWindowManager.KID_SPACE_ID, ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
                    if (kidSpaceId == BaseMiuiPhoneWindowManager.this.mCurrentUserId) {
                        isKidMode = true;
                    }
                }
                BaseMiuiPhoneWindowManager.this.notifyKidSpaceChanged(isKidMode);
                return;
            }
            if (Settings.System.getUriFor(BaseMiuiPhoneWindowManager.IS_CUSTOM_SHORTCUTS_EFFECTIVE).equals(uri)) {
                boolean enable = Settings.System.getIntForUser(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), BaseMiuiPhoneWindowManager.IS_CUSTOM_SHORTCUTS_EFFECTIVE, 1, -2) == 1;
                MiuiKeyInterceptExtend.getInstance(BaseMiuiPhoneWindowManager.this.mContext).setKeyboardShortcutEnable(enable);
                return;
            }
            if (Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.SYNERGY_MODE).equals(uri)) {
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager3 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager3.mIsSynergyMode = Settings.Secure.getInt(baseMiuiPhoneWindowManager3.mContext.getContentResolver(), BaseMiuiPhoneWindowManager.SYNERGY_MODE, 0) == 1;
                if (BaseMiuiPhoneWindowManager.this.mIsSynergyMode && BaseMiuiPhoneWindowManager.this.mProximitySensor != null && BaseMiuiPhoneWindowManager.this.mProximitySensor.isHeld()) {
                    BaseMiuiPhoneWindowManager.this.releaseScreenOnProximitySensor(true);
                    return;
                }
                return;
            }
            if (Settings.System.getUriFor(BaseMiuiPhoneWindowManager.IS_MI_INPUT_EVENT_TIME_LINE_ENABLE).equals(uri)) {
                MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$MiuiSettingsObserver$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BaseMiuiPhoneWindowManager.MiuiSettingsObserver.this.lambda$onChange$1();
                    }
                });
            } else {
                super.onChange(selfChange, uri);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$1() {
            boolean enable = Settings.System.getIntForUser(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), BaseMiuiPhoneWindowManager.IS_MI_INPUT_EVENT_TIME_LINE_ENABLE, 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId) == 1;
            InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
            inputCommonConfig.setMiInputEventTimeLineMode(enable);
            inputCommonConfig.flushToNative();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            Object lock = BaseMiuiPhoneWindowManager.phoneWindowManagerFeature.getLock(BaseMiuiPhoneWindowManager.this);
            synchronized (lock) {
                MiuiSettings.Key.updateOldKeyFunctionToNew(BaseMiuiPhoneWindowManager.this.mContext);
                boolean z = true;
                BaseMiuiPhoneWindowManager.this.mMiuiShortcutTriggerHelper.setPressToAppSwitch(Settings.System.getIntForUser(resolver, "screen_key_press_app_switch", 1, BaseMiuiPhoneWindowManager.this.mCurrentUserId) != 0);
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager.mTrackballWakeScreen = Settings.System.getIntForUser(resolver, "trackball_wake_screen", 0, baseMiuiPhoneWindowManager.mCurrentUserId) == 1;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager2 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager2.mMikeymodeEnabled = Settings.Secure.getIntForUser(resolver, "enable_mikey_mode", 0, baseMiuiPhoneWindowManager2.mCurrentUserId) != 0;
                int cameraKeyActionType = Settings.System.getIntForUser(resolver, "camera_key_preferred_action_type", 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager3 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager3.mCameraKeyWakeScreen = 1 == cameraKeyActionType && 4 == Settings.System.getIntForUser(resolver, "camera_key_preferred_action_shortcut_id", -1, baseMiuiPhoneWindowManager3.mCurrentUserId);
                BaseMiuiPhoneWindowManager.this.mTestModeEnabled = Settings.Global.getInt(resolver, "auto_test_mode_on", 0) != 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager4 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager4.mVoiceAssistEnabled = Settings.System.getIntForUser(resolver, "send_back_when_xiaoai_appear", 0, baseMiuiPhoneWindowManager4.mCurrentUserId) != 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager5 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager5.mHaveBankCard = Settings.Secure.getIntForUser(resolver, "key_bank_card_in_ese", 0, baseMiuiPhoneWindowManager5.mCurrentUserId) > 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager6 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager6.mHaveTranksCard = Settings.Secure.getIntForUser(resolver, "key_trans_card_in_ese", 0, baseMiuiPhoneWindowManager6.mCurrentUserId) > 0;
                String action = Settings.Secure.getStringForUser(resolver, "key_long_press_volume_down", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                if (action != null) {
                    if (!"Street-snap".equals(action) && !"Street-snap-picture".equals(action) && !"Street-snap-movie".equals(action)) {
                        if ("public_transportation_shortcuts".equals(action)) {
                            BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 2;
                        } else {
                            BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 0;
                        }
                    }
                    BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 1;
                } else {
                    Settings.Secure.putStringForUser(resolver, "key_long_press_volume_down", "none", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                }
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager7 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager7.mIsVRMode = Settings.System.getIntForUser(resolver, BaseMiuiPhoneWindowManager.SYSTEM_SETTINGS_VR_MODE, 0, baseMiuiPhoneWindowManager7.mCurrentUserId) == 1;
                if (BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting == null) {
                    BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting = new SettingsStringUtil.SettingStringHelper(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), "enabled_accessibility_services", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                }
                String shortcutService = BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting.read();
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager8 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager8.mTalkBackIsOpened = baseMiuiPhoneWindowManager8.hasTalkbackService(shortcutService);
                String accessibilityShortcut = Settings.Secure.getStringForUser(resolver, "accessibility_shortcut_target_service", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                if (accessibilityShortcut == null) {
                    accessibilityShortcut = BaseMiuiPhoneWindowManager.this.mContext.getString(R.string.config_usbPermissionActivity);
                }
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager9 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager9.mShortcutServiceIsTalkBack = 0 != 0 && baseMiuiPhoneWindowManager9.isTalkBackService(accessibilityShortcut);
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager10 = BaseMiuiPhoneWindowManager.this;
                if (Settings.Secure.getIntForUser(resolver, "accessibility_shortcut_on_lock_screen", 0, baseMiuiPhoneWindowManager10.mCurrentUserId) != 1) {
                    z = false;
                }
                baseMiuiPhoneWindowManager10.mAccessibilityShortcutOnLockScreen = z;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTalkBackService(String accessibilityShortcut) {
        ComponentName componentName;
        return (TextUtils.isEmpty(accessibilityShortcut) || (componentName = ComponentName.unflattenFromString(accessibilityShortcut)) == null || !TALK_BACK_SERVICE_LIST.contains(componentName)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasTalkbackService(String accessibilityShortcut) {
        if (TextUtils.isEmpty(accessibilityShortcut)) {
            return false;
        }
        for (ComponentName componentName : TALK_BACK_SERVICE_LIST) {
            if (SettingsStringUtil.ComponentNameSet.contains(accessibilityShortcut, componentName)) {
                return true;
            }
        }
        return false;
    }

    public void setCurrentUserLw(int newUserId) {
        super.setCurrentUserLw(newUserId);
        this.mCurrentUserId = newUserId;
        this.mAutoDisableScreenButtonsManager.onUserSwitch(newUserId);
        this.mSmartCoverManager.onUserSwitch(newUserId);
        this.mAccessibilityShortcutSetting.setUserId(newUserId);
        this.mSettingsObserver.onChange(false);
        this.mMiuiBackTapGestureService.onUserSwitch(newUserId);
        this.mMiuiKnockGestureService.onUserSwitch(newUserId);
        this.mMiuiThreeGestureListener.onUserSwitch(newUserId);
        this.mMiuiKeyShortcutRuleManager.onUserSwitch(newUserId);
        this.mMiuiFingerPrintTapListener.onUserSwitch(newUserId);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.onUserSwitch();
        }
        MiuiEventBlockerManager.getInstance().onUserSwitch(newUserId);
        ScrollerOptimizationConfigProvider.getInstance().onUserSwitch(newUserId);
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.onUserSwitch(newUserId);
        }
        MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                BaseMiuiPhoneWindowManager.this.lambda$setCurrentUserLw$1();
            }
        });
        MiuiMagicPointerServiceInternal service = (MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class);
        if (service != null) {
            service.onUserChanged(newUserId);
        }
        TouchWakeUpFeatureManager.getInstance().onUserSwitch(newUserId);
        InputManagerServiceStub.getInstance().updateFromUserSwitch();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setCurrentUserLw$1() {
        Settings.System.putIntForUser(this.mContext.getContentResolver(), IS_MI_INPUT_EVENT_TIME_LINE_ENABLE, 0, this.mCurrentUserId);
    }

    public void showBootMessage(final CharSequence msg, boolean always) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.14
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog == null) {
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog = new Dialog(BaseMiuiPhoneWindowManager.this.mContext, 286261258) { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.14.1
                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTouchEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTrackballEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                            return true;
                        }
                    };
                    View view = LayoutInflater.from(BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getContext()).inflate(285999114, (ViewGroup) null);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.setContentView(view);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setType(2021);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().addFlags(1282);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams lp = BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = 5;
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setAttributes(lp);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.setCancelable(false);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.show();
                    ImageView bootLogo = (ImageView) view.findViewById(285868062);
                    bootLogo.setVisibility(0);
                    if ("beryllium".equals(Build.DEVICE)) {
                        String hwc = SystemProperties.get("ro.boot.hwc", "");
                        if (hwc.contains("INDIA")) {
                            bootLogo.setImageResource(285737320);
                        } else if (hwc.contains("GLOBAL")) {
                            bootLogo.setImageResource(285737319);
                        }
                    }
                    BaseMiuiPhoneWindowManager.this.mBootProgress = (ProgressBar) view.findViewById(285868063);
                    BaseMiuiPhoneWindowManager.this.mBootProgress.setVisibility(4);
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.mBootText = baseMiuiPhoneWindowManager.mContext.getResources().getStringArray(285409305);
                    if (BaseMiuiPhoneWindowManager.this.mBootText != null && BaseMiuiPhoneWindowManager.this.mBootText.length > 0) {
                        BaseMiuiPhoneWindowManager.this.mBootTextView = (TextView) view.findViewById(285868064);
                        BaseMiuiPhoneWindowManager.this.mBootTextView.setVisibility(4);
                    }
                }
                List<String> parseList = new ArrayList<>();
                CharSequence charSequence = msg;
                if (charSequence != null) {
                    for (String sp : String.valueOf(charSequence).replaceAll("[^0-9]", ",").split(",")) {
                        if (sp.length() > 0) {
                            parseList.add(sp);
                        }
                    }
                }
                if (parseList.size() == 2) {
                    int progress = Integer.parseInt(parseList.get(0));
                    int total = Integer.parseInt(parseList.get(1));
                    if (progress > total) {
                        progress = total;
                        total = progress;
                    }
                    if (total > 3) {
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setVisibility(0);
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setMax(total);
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setProgress(progress);
                        if (BaseMiuiPhoneWindowManager.this.mBootTextView != null && BaseMiuiPhoneWindowManager.this.mBootText != null) {
                            BaseMiuiPhoneWindowManager.this.mBootTextView.setVisibility(0);
                            int pos = (BaseMiuiPhoneWindowManager.this.mBootText.length * progress) / total;
                            if (pos >= BaseMiuiPhoneWindowManager.this.mBootText.length) {
                                pos = BaseMiuiPhoneWindowManager.this.mBootText.length - 1;
                            }
                            BaseMiuiPhoneWindowManager.this.mBootTextView.setText(BaseMiuiPhoneWindowManager.this.mBootText[pos]);
                        }
                    }
                }
            }
        });
    }

    public void hideBootMessages() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.15
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog != null) {
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.dismiss();
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog = null;
                    BaseMiuiPhoneWindowManager.this.mBootProgress = null;
                    BaseMiuiPhoneWindowManager.this.mBootTextView = null;
                    BaseMiuiPhoneWindowManager.this.mBootText = null;
                }
            }
        });
    }

    boolean checkProcessRunning(String processName) {
        List<ActivityManager.RunningAppProcessInfo> procs;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        if (am == null || (procs = am.getRunningAppProcesses()) == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo info : procs) {
            if (processName.equalsIgnoreCase(info.processName)) {
                return true;
            }
        }
        return false;
    }

    void sendAsyncBroadcast(Intent intent) {
        sendAsyncBroadcast(intent, false);
    }

    void sendAsyncBroadcast(final Intent intent, boolean isInteractive) {
        if (this.mSystemReady) {
            if (isInteractive) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        BaseMiuiPhoneWindowManager.this.lambda$sendAsyncBroadcast$2(intent);
                    }
                });
            } else {
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        BaseMiuiPhoneWindowManager.this.lambda$sendAsyncBroadcast$3(intent);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendAsyncBroadcast$2(Intent intent) {
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, null, BroadcastOptions.makeBasic().setInteractive(true).toBundle());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendAsyncBroadcast$3(Intent intent) {
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendAsyncBroadcast(final Intent intent, final String receiverPermission) {
        if (this.mSystemReady) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.16
                @Override // java.lang.Runnable
                public void run() {
                    BaseMiuiPhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, receiverPermission);
                }
            });
        }
    }

    void sendAsyncBroadcastForAllUser(final Intent intent) {
        if (this.mSystemReady) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.17
                @Override // java.lang.Runnable
                public void run() {
                    BaseMiuiPhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, new UserHandle(-1));
                }
            });
        }
    }

    public void enableScreenAfterBoot() {
        super.enableScreenAfterBoot();
        this.mWifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        ShortCutActionsUtils.getInstance(this.mContext).setWifiOnly(this.mWifiOnly);
        this.mSmartCoverManager.enableLidAfterBoot();
    }

    public void finishLayoutLw(DisplayFrames displayFrames, Rect inputMethodRegion, int displayId) {
        final int inputMethodHeight = inputMethodRegion.bottom - inputMethodRegion.top;
        if (this.mInputMethodWindowVisibleHeight != inputMethodHeight) {
            this.mInputMethodWindowVisibleHeight = inputMethodHeight;
            Slog.i("WindowManager", "input method visible height changed " + inputMethodHeight);
            this.mMiuiKnockGestureService.setInputMethodRect(inputMethodRegion);
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.lambda$finishLayoutLw$4(inputMethodHeight);
                }
            });
            Intent intent = new Intent("miui.intent.action.INPUT_METHOD_VISIBLE_HEIGHT_CHANGED");
            intent.putExtra("miui.intent.extra.input_method_visible_height", this.mInputMethodWindowVisibleHeight);
            sendAsyncBroadcast(intent, "miui.permission.USE_INTERNAL_GENERAL_API");
        }
        int displayWidth = Math.min(displayFrames.mWidth, displayFrames.mHeight) - 1;
        if (this.mIsFoldChanged && displayId == 0 && displayWidth != this.mEdgeSuppressionManager.getScreenWidth()) {
            this.mIsFoldChanged = false;
            this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_CONFIGURATION, this.mFolded, displayFrames);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setInputMethodModeToTouch, reason: merged with bridge method [inline-methods] */
    public void lambda$finishLayoutLw$4(int inputMethodHeight) {
        if (inputMethodHeight != 0) {
            ITouchFeature.getInstance().setTouchMode(0, 25, 1);
        } else {
            ITouchFeature.getInstance().setTouchMode(0, 25, 0);
        }
    }

    protected boolean getForbidFullScreenFlag() {
        return this.mForbidFullScreen;
    }

    private void handleDoubleTapOnHome() {
        if (this.mMiuiShortcutTriggerHelper.mDoubleTapOnHomeBehavior == 1) {
            this.mHomeConsumed = true;
            launchRecentPanel();
        }
    }

    private boolean isNfcEnable(boolean ishomeclick) {
        if (!ishomeclick) {
            return this.mLongPressVolumeDownBehavior == 2 && this.mHaveTranksCard;
        }
        if ("sagit".equals(Build.DEVICE) || "jason".equals(Build.DEVICE)) {
            return false;
        }
        return this.mHaveBankCard || this.mHaveTranksCard;
    }

    public boolean getKeyguardActive() {
        return this.mMiuiKeyguardDelegate != null && (!this.mPowerManager.isScreenOn() ? !this.mMiuiKeyguardDelegate.isShowing() : !this.mMiuiKeyguardDelegate.isShowingAndNotHidden());
    }

    public boolean isKeyGuardNotActive() {
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate;
        return (miuiKeyguardServiceDelegate == null || miuiKeyguardServiceDelegate.isShowingAndNotHidden()) ? false : true;
    }

    public WindowManagerPolicy.WindowState getFocusedWindow() {
        return this.mFocusedWindow;
    }

    private boolean isAudioActive() {
        boolean active = false;
        int mode = getAudioManager().getMode();
        if (mode > 0 && mode < 7) {
            MiuiInputLog.major("isAudioActive():true");
            return true;
        }
        int size = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < size && (1 == i || !(active = AudioSystem.isStreamActive(i, 0))); i++) {
        }
        if (active) {
            MiuiInputLog.major("isAudioActive():" + active);
        }
        return active;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenRecorderEnabled(boolean enable) {
        this.mScreenRecorderEnabled = enable;
    }

    private boolean isTrackInputEvenForScreenRecorder(KeyEvent event) {
        if (this.mScreenRecorderEnabled && sScreenRecorderKeyEventList.contains(Integer.valueOf(event.getKeyCode()))) {
            return true;
        }
        return false;
    }

    private void sendKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.SCREEN_RECORDER_TRACK_KEYEVENT");
        intent.setPackage("com.miui.screenrecorder");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_KEYCODE, event.getKeyCode());
        intent.putExtra("isdown", event.getAction() == 0);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void trackDumpLogKeyCode(KeyEvent event) {
        int code = event.getKeyCode();
        if (code != 25 && code != 24) {
            this.mTrackDumpLogKeyCodePengding = false;
            return;
        }
        InputDevice inputDevice = event.getDevice();
        if (inputDevice != null && inputDevice.isExternal()) {
            this.mTrackDumpLogKeyCodePengding = false;
            return;
        }
        boolean z = this.mTrackDumpLogKeyCodePengding;
        if (!z && code == 24) {
            this.mTrackDumpLogKeyCodePengding = true;
            this.mTrackDumpLogKeyCodeStartTime = event.getEventTime();
            this.mTrackDumpLogKeyCodeLastKeyCode = 24;
            this.mTrackDumpLogKeyCodeVolumeDownTimes = 0;
            return;
        }
        if (z) {
            long timeDelta = event.getEventTime() - this.mTrackDumpLogKeyCodeStartTime;
            if (timeDelta >= this.mTrackDumpLogKeyCodeTimeOut || code == this.mTrackDumpLogKeyCodeLastKeyCode) {
                this.mTrackDumpLogKeyCodePengding = false;
                if (code == 24) {
                    this.mTrackDumpLogKeyCodePengding = true;
                    this.mTrackDumpLogKeyCodeStartTime = event.getEventTime();
                    this.mTrackDumpLogKeyCodeLastKeyCode = 24;
                    this.mTrackDumpLogKeyCodeVolumeDownTimes = 0;
                    return;
                }
                return;
            }
            this.mTrackDumpLogKeyCodeLastKeyCode = code;
            if (code == 25) {
                this.mTrackDumpLogKeyCodeVolumeDownTimes++;
            }
            if (this.mTrackDumpLogKeyCodeVolumeDownTimes == 3) {
                this.mTrackDumpLogKeyCodePengding = false;
                MiuiInputLog.defaults("DumpLog triggered");
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BaseMiuiPhoneWindowManager.this.lambda$trackDumpLogKeyCode$5();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackDumpLogKeyCode$5() {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("dump_log_or_secret_code", "volume_down_up_three_time", null, false);
    }

    private boolean isTrackInputEventForVoiceAssist(KeyEvent event) {
        if (this.mVoiceAssistEnabled && sVoiceAssistKeyEventList.contains(Integer.valueOf(event.getKeyCode()))) {
            return true;
        }
        return false;
    }

    private void sendVoiceAssistKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.VOICE_ASSIST_TRACK_KEYEVENT");
        intent.setPackage("com.miui.voiceassist");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_KEYCODE, event.getKeyCode());
        intent.putExtra("isdown", event.getAction() == 0);
        sendAsyncBroadcast(intent);
    }

    private void sendFullScreenStateToTaskSnapshot() {
        Intent intent = new Intent(CameraBlackCoveredManager.ACTION_FULLSCREEN_STATE_CHANGE);
        intent.putExtra("state", "taskSnapshot");
        sendAsyncBroadcast(intent);
    }

    private void sendFullScreenStateToHome() {
        Intent intent = new Intent(CameraBlackCoveredManager.ACTION_FULLSCREEN_STATE_CHANGE);
        intent.putExtra("state", CameraBlackCoveredManager.STATE_TO_HOME);
        sendAsyncBroadcastForAllUser(intent);
    }

    private void sendBackKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.KEYCODE_BACK");
        intent.putExtra("android.intent.extra.KEY_EVENT", event);
        sendAsyncBroadcast(intent);
    }

    private boolean isLockDeviceWindow(WindowManagerPolicy.WindowState win) {
        WindowManager.LayoutParams lp;
        return (win == null || (lp = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(win, "getAttrs", new Object[0])) == null || (lp.extraFlags & 2048) == 0) ? false : true;
    }

    public static boolean isLargeScreen(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        return configuration.densityDpi == ACCESSIBLE_MODE_SMALL_DENSITY ? smallestScreenWidthDp > 400 : configuration.densityDpi == ACCESSIBLE_MODE_LARGE_DENSITY ? smallestScreenWidthDp > 305 : smallestScreenWidthDp > 320;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        super.dump(prefix, pw, args);
        pw.print(prefix);
        pw.println("BaseMiuiPhoneWindowManager");
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mInputMethodWindowVisibleHeight=");
        pw.println(this.mInputMethodWindowVisibleHeight);
        pw.print(prefix2);
        pw.print("mFrontFingerprintSensor=");
        pw.println(this.mFrontFingerprintSensor);
        pw.print(prefix2);
        pw.print("mSupportTapFingerprintSensorToHome=");
        pw.println(this.mSupportTapFingerprintSensorToHome);
        pw.print(prefix2);
        pw.print("mScreenOffReason=");
        pw.println(this.mScreenOffReason);
        pw.print(prefix2);
        pw.print("mIsStatusBarVisibleInFullscreen=");
        pw.println(this.mIsStatusBarVisibleInFullscreen);
        pw.print(prefix2);
        pw.print("mScreenRecorderEnabled=");
        pw.println(this.mScreenRecorderEnabled);
        pw.print(prefix2);
        pw.print("mVoiceAssistEnabled=");
        pw.println(this.mVoiceAssistEnabled);
        pw.print(prefix2);
        pw.print("mWifiOnly=");
        pw.println(this.mWifiOnly);
        pw.print(prefix2);
        pw.print("KEYCODE_MENU KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(82)));
        pw.print(prefix2);
        pw.print("KEYCODE_APP_SWITCH KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(187)));
        pw.print(prefix2);
        pw.print("KEYCODE_HOME KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(3)));
        pw.print(prefix2);
        pw.print("KEYCODE_BACK KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(4)));
        pw.print(prefix2);
        pw.print("KEYCODE_POWER KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(26)));
        pw.print(prefix2);
        pw.print("KEYCODE_VOLUME_DOWN KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(25)));
        pw.print(prefix2);
        pw.print("KEYCODE_VOLUME_UP KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(24)));
        pw.print(prefix2);
        pw.print("ElSE KEYCODE KeyBitmask=");
        pw.println(Integer.toBinaryString(1));
        pw.print(prefix2);
        pw.print("SHORTCUT_HOME_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_HOME_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_BACK_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_BACK_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_MENU_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_MENU_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_SCREENSHOT_ANDROID=");
        pw.println(Integer.toBinaryString(SHORTCUT_SCREENSHOT_ANDROID));
        pw.print(prefix2);
        pw.print("SHORTCUT_SCREENSHOT_MIUI=");
        pw.println(Integer.toBinaryString(SHORTCUT_SCREENSHOT_MIUI));
        pw.print(prefix2);
        pw.print("SHORTCUT_UNLOCK=");
        pw.println(Integer.toBinaryString(SHORTCUT_UNLOCK));
        pw.print(prefix2);
        pw.print("mDpadCenterDown=");
        pw.println(this.mDpadCenterDown);
        pw.print(prefix2);
        pw.print("mHomeDownAfterDpCenter=");
        pw.println(this.mHomeDownAfterDpCenter);
        pw.print("    ");
        pw.println("KeyResponseSetting");
        pw.print(prefix2);
        pw.print("mCurrentUserId=");
        pw.println(this.mCurrentUserId);
        pw.print(prefix2);
        pw.print("mMikeymodeEnabled=");
        pw.println(this.mMikeymodeEnabled);
        pw.print(prefix2);
        pw.print("mCameraKeyWakeScreen=");
        pw.println(this.mCameraKeyWakeScreen);
        pw.print(prefix2);
        pw.print("mTrackballWakeScreen=");
        pw.println(this.mTrackballWakeScreen);
        pw.print(prefix2);
        pw.print("mTestModeEnabled=");
        pw.println(this.mTestModeEnabled);
        pw.print(prefix2);
        pw.print("mScreenButtonsDisabled=");
        pw.println(this.mAutoDisableScreenButtonsManager.isScreenButtonsDisabled());
        pw.print(prefix2);
        pw.print("mVolumeButtonPrePressedTime=");
        pw.println(this.mVolumeButtonPrePressedTime);
        pw.print(prefix2);
        pw.print("mVolumeButtonPressedCount=");
        pw.println(this.mVolumeButtonPressedCount);
        pw.print(prefix2);
        pw.print("mHaveBankCard=");
        pw.println(this.mHaveBankCard);
        pw.print(prefix2);
        pw.print("mHaveTranksCard=");
        pw.println(this.mHaveTranksCard);
        pw.print(prefix2);
        pw.print("mLongPressVolumeDownBehavior=");
        pw.println(this.mLongPressVolumeDownBehavior);
        pw.print(prefix2);
        pw.print("mIsVRMode=");
        pw.println(this.mIsVRMode);
        pw.print(prefix2);
        pw.print("mTalkBackIsOpened=");
        pw.println(this.mTalkBackIsOpened);
        pw.print(prefix2);
        pw.print("mShortcutServiceIsTalkBack=");
        pw.println(this.mShortcutServiceIsTalkBack);
        pw.print(prefix2);
        pw.print("mImperceptiblePowerKey=");
        pw.println(this.mImperceptiblePowerKey);
        this.mSmartCoverManager.dump(prefix2, pw);
        this.mMiuiThreeGestureListener.dump(prefix2, pw);
        this.mMiuiKnockGestureService.dump(prefix2, pw);
        this.mMiuiBackTapGestureService.dump(prefix2, pw);
        this.mMiuiKeyShortcutRuleManager.dump(prefix2, pw);
        this.mMiuiFingerPrintTapListener.dump(prefix2, pw);
        this.mMiuiTimeFloatingWindow.dump(prefix2, pw);
        if (isMiPad()) {
            MiuiCustomizeShortCutUtils.getInstance(this.mContext).dump(prefix2, pw);
            KeyboardCombinationManagerStubImpl.getInstance().dump(prefix2, pw);
        }
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.dump(prefix2, pw);
            MiuiEventBlockerManager.getInstance().dump(prefix2, pw);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.dump(prefix2, pw);
        }
        ScrollerOptimizationConfigProvider.getInstance().dump(prefix2, pw);
    }
}
