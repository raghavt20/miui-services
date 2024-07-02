package com.miui.server.input.util;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.MiuiMultiWindowUtils;
import android.widget.Toast;
import com.android.internal.accessibility.util.AccessibilityUtils;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.util.ScreenshotRequest;
import com.android.server.LocalServices;
import com.android.server.camera.CameraOpt;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.shortcut.ShortcutOneTrackHelper;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.FindDevicePowerOffLocateManager;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.ActivityTaskManagerServiceImpl;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.server.AccessController;
import com.miui.server.input.custom.InputMiuiDesktopMode;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import com.miui.server.stability.StabilityLocalServiceInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import miui.hardware.input.InputFeature;
import miui.os.Build;
import miui.securityspace.CrossUserUtils;
import miui.util.AudioManagerHelper;
import miui.util.HapticFeedbackUtil;

/* loaded from: classes.dex */
public class ShortCutActionsUtils {
    private static final String ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION = "com.miui.accessibility.environment.sound.recognition.TransparentEsrSettings";
    private static final String ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION_ENABLED = "com.miui.accessibility.environment.sound.recognition.EnvSoundRecognitionService";
    private static final String ACCESSIBILITY_CLASS_NAME_HEAR_SOUND = "com.miui.accessibility.asr.component.message.MessageActivity";
    private static final String ACCESSIBILITY_CLASS_NAME_HEAR_SOUND_SUBTITLE = "com.miui.accessibility.asr.component.floatwindow.TransparentCaptionActivity";
    private static final String ACCESSIBILITY_CLASS_NAME_TALK_BACK = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL = "com.miui.accessibility.voiceaccess.settings.TransparentVoiceAccessSettings";
    private static final String ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL_ENABLED = "com.miui.accessibility.voiceaccess.VoiceAccessAccessibilityService";
    private static final String ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_DEFAULT = "startAiSubtitlesWindow";
    private static final String ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_FULL_SCREEN = "startAiSubtitlesFullscreen";
    private static final String ACCESSIBILITY_PACKAGE_NAME = "com.miui.accessibility";
    public static final String ACCESSIBILITY_TALKBACK_STATUS = "talkback_status";
    private static final int ACCESSIBLE_MODE_LARGE_DENSITY = 461;
    private static final int ACCESSIBLE_MODE_SMALL_DENSITY = 352;
    private static final String ACTION_GLOBAL_POWER_GUIDE = "com.miui.miinput.action.GLOBAL_POWER_GUIDE";
    private static final String ACTION_INTENT_CLASS_NAME = "className";
    private static final String ACTION_INTENT_CONTENT = "content";
    private static final String ACTION_INTENT_DUAL = "dual";
    private static final String ACTION_INTENT_KEY = "packageName";
    private static final String ACTION_INTENT_SHORTCUT = "shortcut";
    private static final String ACTION_INTENT_TITLE = "title";
    private static final String ACTION_KDDI_GLOBAL_POWER_GUIDE = "com.miui.miinput.action.KDDI_GLOBAL_POWER_GUIDE";
    public static final String ACTION_PANEL_OPERATION = "action_panels_operation";
    public static final String ACTION_POWER_UP = "power_up";
    private static final String ACTION_ROTATION_FOLLOWS_SENSOR_GLOBAL_POWER_GUIDE = "com.miui.miinput.action.ROTATION_FOLLOWS_SENSOR_GLOBAL_POWER_GUIDE";
    private static final String ACTION_SMALL_SCREEN_GLOBAL_POWER_GUIDE = "com.miui.miinput.action.SMALL_SCREEN_GLOBAL_POWER_GUIDE";
    private static final String DEVICE_TYPE_FOLD = "fold";
    private static final String DEVICE_TYPE_PAD = "pad";
    private static final String DEVICE_TYPE_PHONE = "phone";
    private static final String DEVICE_TYPE_SMALL_SCREEN = "small_screen";
    static final int DIRECTION_LEFT = 0;
    static final int DIRECTION_RIGHT = 1;
    public static final String DISABLE_SHORTCUT_TRACK = "disable_shortcut_track";
    public static final int EVENT_LEICA_MOMENT = 65537;
    public static final String EXTRA_ACTION_SOURCE = "event_source";
    public static final String EXTRA_KEY_ACTION = "extra_key_action";
    public static final String EXTRA_KEY_EVENT_TIME = "extra_key_event_time";
    public static final String EXTRA_LONG_PRESS_POWER_FUNCTION = "extra_long_press_power_function";
    public static final String EXTRA_POWER_GUIDE = "powerGuide";
    public static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    public static final String EXTRA_SKIP_TELECOM_CHECK = "skip_telecom_check";
    public static final String EXTRA_TORCH_ENABLED = "extra_torch_enabled";
    private static final boolean IS_CETUS = "cetus".equals(Build.DEVICE);
    private static final String KEY_ACTION = "key_action";
    public static final String KEY_OPERATION = "operation";
    public static final String NOTES_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/text_note";
    private static final String PACKAGE_INPUT_SETTINGS = "com.miui.securitycore";
    private static final String PACKAGE_MI_CREATION = "com.miui.creation";
    private static final String PACKAGE_NOTES = "com.miui.notes";
    public static final String PACKAGE_SMART_HOME = "com.miui.smarthomeplus";
    public static final String PARTIAL_SCREENSHOT_POINTS = "partial.screenshot.points";
    public static final String REASON_OF_DOUBLE_CLICK_HOME_KEY = "double_click_home";
    public static final String REASON_OF_DOUBLE_CLICK_VOLUME_DOWN = "double_click_volume_down";
    public static final String REASON_OF_KEYBOARD = "keyboard";
    public static final String REASON_OF_KEYBOARD_FN = "keyboard_fn";
    public static final String REASON_OF_LONG_PRESS_CAMERA_KEY = "long_press_camera_key";
    public static final String REASON_OF_TRIGGERED_BY_AI_KEY = "ai_key";
    public static final String REASON_OF_TRIGGERED_BY_PROXIMITY_SENSOR = "proximity_sensor";
    public static final String REASON_OF_TRIGGERED_BY_STABILIZER = "stabilizer";
    public static final String REASON_OF_TRIGGERED_TORCH = "triggered_by_runnable";
    public static final String REASON_OF_TRIGGERED_TORCH_BY_POWER = "triggered_by_power";
    public static final String REVERSE_NOTIFICATION_PANEL = "reverse_notifications_panel";
    public static final String REVERSE_QUICK_SETTINGS_PANEL = "reverse_quick_settings_panel";
    private static final String SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY = "com.android.settings.KeyDownloadDialogActivity";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "ShortCutActionsUtils";
    public static final int TAKE_PARTIAL_SCREENSHOT = 100;
    public static final int TAKE_PARTIAL_SCREENSHOT_WITH_STYLUS = 98;
    public static final int TAKE_SCREENSHOT_WITHOUT_ANIM = 99;
    public static final String TYPE_PHONE_SHORTCUT = "phone_shortcut";
    private static final int XIAO_POWER_GUIDE_VERSIONCODE = 3;
    private static ShortCutActionsUtils shortCutActionsUtils;
    private Context mContext;
    private Handler mHandler;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private boolean mHasCameraFlash;
    private boolean mIsFolded;
    private boolean mIsKidMode;
    private final boolean mIsVoiceCapable;
    private PowerManager mPowerManager;
    private final ScreenshotHelper mScreenshotHelper;
    private final ShortcutOneTrackHelper mShortcutOneTrackHelper;
    private StabilityLocalServiceInternal mStabilityLocalServiceInternal;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private boolean mSupportAccessibilitySubtitle;
    private boolean mWifiOnly;
    private WindowManagerPolicy mWindowManagerPolicy;
    private List<String> mGAGuideCustomizedRegionList = new ArrayList();
    private boolean mIsSystemReady = false;

    private ShortCutActionsUtils(final Context context) {
        this.mContext = context;
        this.mScreenshotHelper = new ScreenshotHelper(this.mContext);
        Handler handler = MiuiInputThread.getHandler();
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.input.util.ShortCutActionsUtils$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ShortCutActionsUtils.this.lambda$new$0(context);
            }
        });
        this.mIsVoiceCapable = context.getResources().getBoolean(17891898);
        this.mHasCameraFlash = Build.hasCameraFlash(this.mContext);
        this.mShortcutOneTrackHelper = ShortcutOneTrackHelper.getInstance(this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(Context context) {
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, false);
    }

    public static ShortCutActionsUtils getInstance(Context context) {
        if (shortCutActionsUtils == null) {
            shortCutActionsUtils = new ShortCutActionsUtils(context);
        }
        return shortCutActionsUtils;
    }

    public void systemReady() {
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        this.mStabilityLocalServiceInternal = (StabilityLocalServiceInternal) LocalServices.getService(StabilityLocalServiceInternal.class);
        this.mIsSystemReady = true;
    }

    public void bootComplete() {
        this.mSupportAccessibilitySubtitle = InputFeature.supportAccessibilityLiveSubtitles();
    }

    public boolean triggerFunction(String function, String action, Bundle extras, boolean hapticFeedback, String effectKey) {
        StatusBarManagerInternal statusBarManagerInternal;
        if (!this.mIsSystemReady) {
            MiuiInputLog.defaults("Failed to trigger shortcut function, system is not ready");
            return false;
        }
        if (this.mIsKidMode) {
            MiuiInputLog.defaults("Children's space does not trigger shortcut gestures");
            return false;
        }
        if (this.mShortcutOneTrackHelper != null && (extras == null || !extras.getBoolean(DISABLE_SHORTCUT_TRACK))) {
            this.mShortcutOneTrackHelper.trackShortcutEventTrigger(action, function);
        }
        boolean triggered = false;
        if (effectKey == null) {
            effectKey = "virtual_key_longpress";
        }
        if (!this.mWindowManagerPolicy.isUserSetupComplete()) {
            if ("dump_log".equals(function) || ("dump_log_or_secret_code".equals(function) && this.mIsVoiceCapable)) {
                boolean triggered2 = launchDumpLog();
                triggerHapticFeedback(triggered2, action, function, hapticFeedback, effectKey);
                return triggered2;
            }
            MiuiInputLog.major("user setup not complete");
            return false;
        }
        if ("screen_shot".equals(function)) {
            triggered = takeScreenshot(1);
            sendRecordCountEvent(this.mContext, "screenshot", "key_shortcut");
        } else if ("partial_screen_shot".equals(function)) {
            triggered = extras != null ? takeScreenshotWithBundle(extras.getFloatArray(PARTIAL_SCREENSHOT_POINTS)) : takeScreenshot(100);
        } else if ("screenshot_without_anim".equals(function)) {
            triggered = takeScreenshot(99);
        } else if ("stylus_partial_screenshot".equals(function)) {
            triggered = takeScreenshot(98);
        } else if ("launch_voice_assistant".equals(function)) {
            triggered = launchVoiceAssistant(action, extras);
            effectKey = "screen_button_voice_assist";
        } else if ("launch_ai_shortcut".equals(function)) {
            triggered = launchAiShortcut();
        } else if ("launch_alipay_scanner".equals(function)) {
            triggered = launchAlipayScanner(action);
        } else if ("launch_alipay_payment_code".equals(function)) {
            triggered = launchAlipayPaymentCode(action);
        } else if ("launch_alipay_health_code".equals(function)) {
            triggered = launchAlipayHealthCode(action);
        } else if ("launch_wechat_scanner".equals(function)) {
            triggered = launchWexinScanner(action);
        } else if ("launch_wechat_payment_code".equals(function)) {
            triggered = lauchWeixinPaymentCode(action);
        } else if ("turn_on_torch".equals(function)) {
            triggered = launchTorch(extras);
        } else if ("launch_calculator".equals(function)) {
            triggered = launchCalculator();
        } else if ("launch_camera".equals(function)) {
            triggered = launchCamera(action);
        } else if ("dump_log".equals(function)) {
            triggered = launchDumpLog();
        } else if ("launch_control_center".equals(function)) {
            triggered = launchControlCenter();
        } else if ("launch_notification_center".equals(function)) {
            triggered = launchNotificationCenter();
        } else if ("mute".equals(function)) {
            triggered = mute();
        } else if ("launch_google_search".equals(function)) {
            triggered = launchGoogleSearch(action);
            effectKey = "screen_button_voice_assist";
        } else if ("go_to_sleep".equals(function)) {
            triggered = goToSleep(action);
        } else if ("dump_log_or_secret_code".equals(function)) {
            triggered = launchDumpLogOrContact(action);
        } else if ("au_pay".equals(function)) {
            triggered = launchAuPay();
        } else if ("google_pay".equals(function)) {
            triggered = launchGooglePay();
        } else if ("mi_pay".equals(function)) {
            triggered = launchMiPay(action);
        } else if ("note".equals(function)) {
            triggered = launchMiNotes(action, extras);
        } else if ("launch_smarthome".equals(function)) {
            triggered = launchSmartHomeService(action, extras);
        } else if ("find_device_locate".equals(function)) {
            triggered = findDeviceLocate();
        } else if ("launch_sound_recorder".equals(function)) {
            triggered = launchSoundRecorder();
        } else if ("launch_camera_capture".equals(function)) {
            triggered = launchCamera(action, "CAPTURE");
        } else if ("launch_camera_video".equals(function)) {
            triggered = launchCamera(action, "VIDEO");
        } else if ("vibrate".equals(function)) {
            triggered = vibrate();
        } else if ("launch_screen_recorder".equals(function)) {
            triggered = launchScreenRecorder();
        } else if ("miui_talkback".equals(function)) {
            triggered = setAccessibilityTalkBackState(action);
        } else if ("voice_control".equals(function)) {
            triggered = launchAccessibilityVoiceControl();
        } else if ("environment_speech_recognition".equals(function)) {
            triggered = launchAccessibilityEnvironmentSpeechRecognition();
        } else if ("hear_sound".equals(function)) {
            triggered = launchAccessibilityHearSound(action, function);
        } else if ("hear_sound_subtitle".equals(function)) {
            triggered = launchAccessibilityHearSoundSubtitle(action, function);
        } else if ("launch_global_power_guide".equals(function)) {
            triggered = launchGlobalPowerGuide();
        } else if ("split_ltr".equals(function)) {
            ActivityTaskManagerServiceImpl.getInstance().toggleSplitByGesture(true);
        } else if ("split_rtl".equals(function)) {
            ActivityTaskManagerServiceImpl.getInstance().toggleSplitByGesture(false);
        } else if ("launch_home".equals(function)) {
            triggered = launchHome(action);
        } else if ("launch_recents".equals(function)) {
            triggered = launchRecents(action);
        } else if ("close_app".equals(function)) {
            if (extras != null && TYPE_PHONE_SHORTCUT.equals(extras.getString(EXTRA_SHORTCUT_TYPE))) {
                this.mWindowManagerPolicy.triggerCloseApp(action);
            } else {
                ActivityTaskManagerServiceImpl.getInstance().onMetaKeyCombination();
            }
        } else if ("launch_app_small_window".equals(function)) {
            triggered = ActivityTaskManagerServiceImpl.getInstance().freeFormAndFullScreenToggleByKeyCombination(true);
        } else if ("launch_app_full_window".equals(function)) {
            triggered = ActivityTaskManagerServiceImpl.getInstance().freeFormAndFullScreenToggleByKeyCombination(false);
        } else if ("launch_split_screen_to_left".equals(function)) {
            triggered = ActivityTaskManagerServiceImpl.getInstance().setSplitScreenDirection(0);
        } else if ("launch_split_screen_to_right".equals(function)) {
            triggered = ActivityTaskManagerServiceImpl.getInstance().setSplitScreenDirection(1);
        } else if ("launch_app".equals(function)) {
            triggered = launchAppSmallWindow(extras);
        } else if ("launch_camera_and_take_photo".equals(function)) {
            CameraOpt.callMethod("sendEvent", Integer.valueOf(EVENT_LEICA_MOMENT));
            triggered = launchCameraAndTakePhoto();
        } else if ("launch_sos".equals(function)) {
            triggered = launchMiuiSoS();
        } else if ("launch_xiaoai_guide".equals(function)) {
            triggered = launchApp(getPowerGuideIntent());
        } else if ("show_menu".equals(function)) {
            triggered = showMenu(action);
        } else if ("split_screen".equals(function)) {
            if (isSupportSpiltScreen() && (statusBarManagerInternal = this.mStatusBarManagerInternal) != null) {
                statusBarManagerInternal.toggleSplitScreen();
                triggered = true;
            }
        } else if ("live_subtitles_full_screen".equals(function)) {
            triggered = launchAccessibilityLiveSubtitle(action, ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_FULL_SCREEN);
        } else if ("live_subtitles".equals(function)) {
            triggered = launchAccessibilityLiveSubtitle(action, ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_DEFAULT);
        }
        triggerHapticFeedback(triggered, action, function, hapticFeedback, effectKey);
        return triggered;
    }

    private boolean redirectAccessibilityHearSoundFunction(String action, String function) {
        if (TextUtils.isEmpty(action)) {
            MiuiInputLog.defaults("redirect accessibility hear sound function fail! because action is null");
            return false;
        }
        if ("back_double_tap".equals(action) || "back_triple_tap".equals(action)) {
            String redirectFunction = null;
            if ("hear_sound".equals(function)) {
                redirectFunction = "live_subtitles_full_screen";
                launchAccessibilityLiveSubtitle(action, ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_FULL_SCREEN);
            } else if ("hear_sound_subtitle".equals(function)) {
                redirectFunction = "live_subtitles";
                launchAccessibilityLiveSubtitle(action, ACCESSIBILITY_LIVE_SUBTITLES_WINDOW_TYPE_DEFAULT);
            }
            if (!TextUtils.isEmpty(redirectFunction)) {
                Settings.System.putStringForUser(this.mContext.getContentResolver(), action, redirectFunction, -2);
                return true;
            }
        }
        return false;
    }

    private boolean launchAccessibilityLiveSubtitle(String action, String floatingWindowType) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xiaomi.aiasst.vision", "com.xiaomi.aiasst.vision.control.translation.AiTranslateService"));
        Bundle bundle = new Bundle();
        bundle.putString("from", action);
        bundle.putString("floatingWindowType", floatingWindowType);
        intent.putExtras(bundle);
        this.mContext.startService(intent);
        return true;
    }

    private boolean showMenu(String action) {
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            baseMiuiPhoneWindowManager.triggerShowMenu(action);
            return true;
        }
        return true;
    }

    private boolean isSupportSpiltScreen() {
        if (!IS_CETUS || isLargeScreen(this.mContext)) {
            return false;
        }
        MiuiInputLog.major("Ignore because the current window is the small screen");
        makeAllUserToastAndShow(this.mContext.getString(286196213), 0);
        return false;
    }

    private void makeAllUserToastAndShow(String text, int duration) {
        Toast toast = Toast.makeText(this.mContext, text, duration);
        toast.show();
    }

    private boolean isLargeScreen(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        return configuration.densityDpi == ACCESSIBLE_MODE_SMALL_DENSITY ? smallestScreenWidthDp > 400 : configuration.densityDpi == ACCESSIBLE_MODE_LARGE_DENSITY ? smallestScreenWidthDp > 305 : smallestScreenWidthDp > 320;
    }

    private boolean launchCameraAndTakePhoto() {
        return launchCamera("launch_camera_and_take_photo");
    }

    private boolean launchGlobalPowerGuide() {
        String actionName;
        Intent intent = new Intent();
        if ("pad".equals(getDeviceType()) || (DEVICE_TYPE_FOLD.equals(getDeviceType()) && isLargeScreen())) {
            actionName = ACTION_ROTATION_FOLLOWS_SENSOR_GLOBAL_POWER_GUIDE;
        } else if ("ruyi".equals(Build.DEVICE) && this.mIsFolded) {
            actionName = ACTION_SMALL_SCREEN_GLOBAL_POWER_GUIDE;
        } else {
            actionName = InputFeature.IS_SUPPORT_KDDI_POWER_GUIDE ? ACTION_KDDI_GLOBAL_POWER_GUIDE : ACTION_GLOBAL_POWER_GUIDE;
        }
        intent.setAction(actionName);
        intent.setPackage(PACKAGE_INPUT_SETTINGS);
        intent.setFlags(268468224);
        return launchApp(intent);
    }

    public void notifyFoldStatus(boolean folded) {
        this.mIsFolded = folded;
    }

    private String getDeviceType() {
        if (MiuiSettings.System.IS_FOLD_DEVICE) {
            return DEVICE_TYPE_FOLD;
        }
        if (Build.IS_TABLET) {
            return "pad";
        }
        return DEVICE_TYPE_PHONE;
    }

    private boolean isLargeScreen() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        int screenSize = configuration.screenLayout & 15;
        return screenSize == 3;
    }

    public boolean launchAppSmallWindow(Bundle extras) {
        if (extras == null) {
            return false;
        }
        String packageName = extras.getString("packageName");
        String className = extras.getString("className");
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
            return false;
        }
        Intent intent = getIntent(packageName, className);
        if (ActivityTaskManagerServiceImpl.getInstance().isFreeFormExit(packageName, UserHandle.myUserId())) {
            return false;
        }
        ActivityOptions options = MiuiMultiWindowUtils.getActivityOptions(this.mContext, packageName, true, false);
        if (options != null) {
            if (ActivityTaskManagerServiceImpl.getInstance().isSplitExist(packageName, UserHandle.myUserId())) {
                return false;
            }
            this.mContext.startActivityAsUser(intent, options.toBundle(), UserHandle.CURRENT);
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        return true;
    }

    public Intent getIntent(String packageName, String className) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setClassName(packageName, className);
        intent.setFlags(270532608);
        return intent;
    }

    private boolean launchHome(String shortcut) {
        if (InputMiuiDesktopMode.launchHome(this.mContext, shortcut)) {
            return true;
        }
        if (this.mWindowManagerPolicy == null) {
            this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            return baseMiuiPhoneWindowManager.launchHome();
        }
        return false;
    }

    private boolean launchRecents(String shortcut) {
        if (InputMiuiDesktopMode.launchRecents(this.mContext, shortcut)) {
            return true;
        }
        if (this.mWindowManagerPolicy == null) {
            this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
        StatusBarManagerInternal statusBarManagerInternal = this.mStatusBarManagerInternal;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.toggleRecentApps();
            return false;
        }
        return false;
    }

    private void triggerHapticFeedback(boolean triggered, String shortcut, String function, boolean hapticFeedback, String effectKey) {
        HapticFeedbackUtil hapticFeedbackUtil;
        if (triggered && hapticFeedback && (hapticFeedbackUtil = this.mHapticFeedbackUtil) != null) {
            hapticFeedbackUtil.performHapticFeedback(effectKey, false);
        }
        MiuiInputLog.defaults("shortcut:" + shortcut + " trigger function:" + function + " result:" + triggered);
    }

    private boolean setAccessibilityTalkBackState(String action) {
        ComponentName componentName = ComponentName.unflattenFromString(ACCESSIBILITY_CLASS_NAME_TALK_BACK);
        if ("key_combination_volume_up_volume_down".equals(action)) {
            AccessibilityUtils.setAccessibilityServiceState(this.mContext, componentName, false, -2);
            return true;
        }
        AccessibilityUtils.setAccessibilityServiceState(this.mContext, componentName, !isAccessibilityFunctionEnabled(componentName.getClassName()), -2);
        return true;
    }

    private boolean launchAccessibilityVoiceControl() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL);
        intent.putExtra("OPEN_VOICE_ACCESS", !isAccessibilityFunctionEnabled(ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL_ENABLED) ? "open" : "close");
        return launchApp(intent);
    }

    private boolean launchAccessibilityEnvironmentSpeechRecognition() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION);
        intent.putExtra("OPEN_ESR", !isAccessibilityFunctionEnabled(ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION_ENABLED) ? "open" : "close");
        return launchApp(intent);
    }

    private boolean launchAccessibilityHearSound(String action, String function) {
        if (this.mSupportAccessibilitySubtitle) {
            MiuiInputLog.defaults("redirect accessibility hear sound success, action=" + action);
            return redirectAccessibilityHearSoundFunction(action, function);
        }
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_HEAR_SOUND);
        return launchApp(intent);
    }

    private boolean launchAccessibilityHearSoundSubtitle(String action, String function) {
        if (this.mSupportAccessibilitySubtitle) {
            MiuiInputLog.defaults("redirect accessibility hear sound subtitle success, action=" + action);
            return redirectAccessibilityHearSoundFunction(action, function);
        }
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_HEAR_SOUND_SUBTITLE);
        return launchApp(intent);
    }

    private boolean isAccessibilityFunctionEnabled(String componentClassName) {
        boolean enabled = false;
        if (componentClassName == null) {
            MiuiInputLog.error("Accessibility function componentClassName is null");
            return false;
        }
        List<ComponentName> list = new ArrayList<>(AccessibilityUtils.getEnabledServicesFromSettings(this.mContext, -2));
        for (ComponentName component : list) {
            if (component != null && componentClassName.equals(component.getClassName())) {
                enabled = true;
            }
        }
        MiuiInputLog.major("Accessibility function componentClassName:" + componentClassName + " enabled=" + enabled);
        return enabled;
    }

    private boolean launchGoogleSearch(String action) {
        if (this.mWindowManagerPolicy == null) {
            this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
        if (this.mWindowManagerPolicy instanceof BaseMiuiPhoneWindowManager) {
            Bundle args = new Bundle();
            args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", -1);
            setGoogleSearchInvocationTypeKey(action, args);
            return this.mWindowManagerPolicy.launchAssistAction(null, args);
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void setGoogleSearchInvocationTypeKey(String action, Bundle args) {
        char c;
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action.hashCode()) {
            case -1259120794:
                if (action.equals("long_press_power_key")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1524450206:
                if (action.equals("long_press_home_key")) {
                    c = 1;
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
                args.putInt("invocation_type", 6);
                return;
            case 1:
                args.putInt("invocation_type", 5);
                return;
            default:
                args.putInt("invocation_type", 2);
                return;
        }
    }

    private boolean launchMiuiSoS() {
        Intent intent = new Intent("miui.intent.action.LAUNCH_SOS");
        intent.setPackage(SETTINGS_PACKAGE_NAME);
        return launchApp(intent);
    }

    private boolean findDeviceLocate() {
        FindDevicePowerOffLocateManager.sendFindDeviceLocateBroadcast(this.mContext, FindDevicePowerOffLocateManager.IMPERCEPTIBLE_POWER_PRESS);
        return true;
    }

    public boolean triggerFunction(String function, String action, Bundle extras, boolean hapticFeedback) {
        return triggerFunction(function, action, extras, hapticFeedback, "mesh_heavy");
    }

    private boolean launchSmartHomeService(String shortcut, Bundle extra) {
        Intent smartHomeIntent = new Intent();
        smartHomeIntent.setComponent(new ComponentName("com.miui.smarthomeplus", "com.miui.smarthomeplus.UWBEntryService"));
        if ("long_press_power_key".equals(shortcut) && extra != null && !TextUtils.isEmpty(extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION))) {
            smartHomeIntent = getLongPressPowerKeyFunctionIntent("com.miui.smarthomeplus", extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION));
        }
        smartHomeIntent.putExtra(EXTRA_ACTION_SOURCE, shortcut);
        try {
            this.mContext.startServiceAsUser(smartHomeIntent, UserHandle.CURRENT);
            return true;
        } catch (Exception e) {
            MiuiInputLog.error(e.toString());
            return true;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Intent getLongPressPowerKeyFunctionIntent(String intentName, String action) {
        boolean z;
        Intent intent = null;
        char c = 65535;
        switch (intentName.hashCode()) {
            case 298563857:
                if (intentName.equals("com.miui.smarthomeplus")) {
                    z = true;
                    break;
                }
                z = -1;
                break;
            case 1566545774:
                if (intentName.equals("android.intent.action.ASSIST")) {
                    z = false;
                    break;
                }
                z = -1;
                break;
            default:
                z = -1;
                break;
        }
        switch (z) {
            case false:
                intent = new Intent("android.intent.action.ASSIST");
                intent.setComponent(null);
                intent.putExtra("versionCode", 3);
                break;
            case true:
                intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.smarthomeplus", "com.miui.smarthomeplus.UWBEntryService"));
                break;
        }
        if (intent != null) {
            switch (action.hashCode()) {
                case -1259120794:
                    if (action.equals("long_press_power_key")) {
                        c = 0;
                        break;
                    }
                    break;
                case 858558485:
                    if (action.equals(ACTION_POWER_UP)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    intent.putExtra(KEY_ACTION, 0);
                    intent.putExtra("long_press_event_time", SystemClock.uptimeMillis());
                    break;
                case 1:
                    intent.putExtra(KEY_ACTION, 1);
                    intent.putExtra("key_event_time", SystemClock.uptimeMillis());
                    break;
            }
        }
        return intent;
    }

    public boolean launchControlCenter() {
        Intent intent = new Intent(ACTION_PANEL_OPERATION);
        intent.putExtra(KEY_OPERATION, REVERSE_QUICK_SETTINGS_PANEL);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    public boolean launchNotificationCenter() {
        Intent intent = new Intent(ACTION_PANEL_OPERATION);
        intent.putExtra(KEY_OPERATION, REVERSE_NOTIFICATION_PANEL);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    public boolean mute() {
        boolean isSilenceModeOn = MiuiSettings.SoundMode.isSilenceModeOn(this.mContext);
        MiuiSettings.SoundMode.setSilenceModeOn(this.mContext, !isSilenceModeOn);
        return true;
    }

    public boolean launchAiShortcut() {
        try {
            MiuiInputLog.major("knock launch ai shortcut");
            Intent intent = new Intent();
            intent.putExtra("is_show_dialog", "false");
            intent.putExtra("from", "knock");
            intent.setFlags(268435456);
            ComponentName componentName = ComponentName.unflattenFromString("com.miui.voiceassist/com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity");
            intent.setComponent(componentName);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return true;
        } catch (IllegalStateException e) {
            MiuiInputLog.error("IllegalStateException", e);
            return false;
        } catch (SecurityException e2) {
            MiuiInputLog.error("SecurityException", e2);
            return false;
        } catch (RuntimeException e3) {
            MiuiInputLog.error("RuntimeException", e3);
            return false;
        }
    }

    private boolean launchVoiceAssistant(String shortcut, Bundle extra) {
        try {
            Intent intent = new Intent("android.intent.action.ASSIST");
            if (REASON_OF_KEYBOARD.equals(shortcut)) {
                intent.putExtra("voice_assist_start_from_key", "external_keyboard_xiaoai_key");
            } else {
                if ("long_press_power_key".equals(shortcut) && extra != null && !TextUtils.isEmpty(extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION))) {
                    Intent intent2 = getLongPressPowerKeyFunctionIntent("android.intent.action.ASSIST", extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION));
                    intent2.putExtra(EXTRA_POWER_GUIDE, extra.getBoolean(EXTRA_POWER_GUIDE, false));
                    intent = intent2;
                } else if (extra != null && REASON_OF_TRIGGERED_BY_AI_KEY.equals(shortcut)) {
                    intent.putExtra(KEY_ACTION, extra.getInt(EXTRA_KEY_ACTION));
                    intent.putExtra("key_event_time", extra.getLong(EXTRA_KEY_EVENT_TIME));
                }
                intent.putExtra("voice_assist_start_from_key", shortcut);
                intent.putExtra("app.send.wakeup.command", System.currentTimeMillis());
            }
            intent.setPackage("com.miui.voiceassist");
            ComponentName componentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195883));
            intent.setComponent(componentName);
            MiuiInputLog.major("launchVoiceAssistant startForegroundServiceAsUser");
            return this.mContext.startForegroundServiceAsUser(intent, UserHandle.CURRENT) != null;
        } catch (Exception e) {
            MiuiInputLog.error("Exception", e);
            return false;
        }
    }

    private boolean launchMiNotes(String shortcut, Bundle extras) {
        ActivityOptions activityOptions;
        Intent intent = new Intent("com.miui.pad.notes.action.INSERT_OR_EDIT");
        intent.setType(NOTES_CONTENT_ITEM_TYPE);
        intent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        String packageName = PACKAGE_NOTES;
        intent.setPackage(PACKAGE_NOTES);
        if (extras != null) {
            String scene = extras.getString("scene", null);
            if (scene == null) {
                return launchAppSimple(intent, null);
            }
            intent.putExtra("scene", scene);
            if (isAppInstalled(PACKAGE_MI_CREATION)) {
                packageName = PACKAGE_MI_CREATION;
                intent.setPackage(PACKAGE_MI_CREATION);
                intent.setAction("com.miui.creation.action.INSERT_CREATION");
                intent.setType("vnd.android.cursor.item/create_creation");
                MiuiInputLog.defaults("com.miui.creation already installed");
            }
            intent.addFlags(335544320);
            if (MiuiStylusShortcutManager.SCENE_KEYGUARD.equals(scene) || MiuiStylusShortcutManager.SCENE_OFF_SCREEN.equals(scene)) {
                intent.putExtra("StartActivityWhenLocked", true);
                return launchAppSimple(intent, null);
            }
            Bundle bundle = null;
            if (MiuiStylusShortcutManager.SCENE_APP.equals(scene) && (activityOptions = MiuiMultiWindowUtils.getActivityOptions(this.mContext, packageName, true, false)) != null) {
                bundle = activityOptions.toBundle();
            }
            return launchAppSimple(intent, bundle);
        }
        return launchAppSimple(intent, null);
    }

    private boolean isAppInstalled(String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            MiuiInputLog.error(packageName + " don't install");
            packageInfo = null;
        }
        return packageInfo != null;
    }

    private boolean launchAppSimple(Intent intent, Bundle bundle) {
        try {
            this.mContext.startActivityAsUser(intent, bundle, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException e) {
            MiuiInputLog.error("launchAppSimple ActivityNotFoundException", e);
            return false;
        } catch (IllegalStateException e2) {
            MiuiInputLog.error("launchAppSimple IllegalStateException", e2);
            return false;
        }
    }

    TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    public void setWifiOnly(boolean wifiOnly) {
        this.mWifiOnly = wifiOnly;
    }

    private boolean launchMiPay(String eventSource) {
        Intent nfcIntent = new Intent();
        nfcIntent.setFlags(536870912);
        nfcIntent.putExtra("StartActivityWhenLocked", true);
        nfcIntent.setAction("com.miui.intent.action.DOUBLE_CLICK");
        nfcIntent.putExtra(EXTRA_ACTION_SOURCE, eventSource);
        nfcIntent.setPackage("com.miui.tsmclient");
        return launchApp(nfcIntent);
    }

    private boolean launchTorch(Bundle extra) {
        if (extra != null && !extra.getBoolean(EXTRA_SKIP_TELECOM_CHECK)) {
            TelecomManager telecomManager = getTelecommService();
            boolean phoneIdle = this.mWifiOnly || (telecomManager != null && telecomManager.getCallState() == 0);
            if (!phoneIdle) {
                MiuiInputLog.defaults("not launch torch,because telecom");
                return false;
            }
        }
        if (!this.mHasCameraFlash) {
            MiuiInputLog.major("not have camera flash");
            return false;
        }
        boolean isOpen = Settings.Global.getInt(this.mContext.getContentResolver(), "torch_state", 0) != 0;
        Intent intent = new Intent("miui.intent.action.TOGGLE_TORCH");
        intent.addFlags(268435456);
        if (extra != null) {
            intent.putExtra("miui.intent.extra.IS_ENABLE", extra.getBoolean(EXTRA_TORCH_ENABLED, false));
        } else {
            intent.putExtra("miui.intent.extra.IS_ENABLE", isOpen ? false : true);
        }
        this.mContext.sendBroadcast(intent);
        return true;
    }

    private boolean launchCamera(String shortcut) {
        if (REASON_OF_DOUBLE_CLICK_VOLUME_DOWN.equals(shortcut) || "launch_camera_and_take_photo".equals(shortcut)) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 5, shortcut);
        }
        Intent cameraIntent = new Intent();
        cameraIntent.setFlags(2097152);
        boolean z = true;
        cameraIntent.putExtra("ShowCameraWhenLocked", true);
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            if (!baseMiuiPhoneWindowManager.getKeyguardActive() && !"power_double_tap".equals(shortcut)) {
                z = false;
            }
            cameraIntent.putExtra("StartActivityWhenLocked", z);
        }
        cameraIntent.setAction("android.media.action.STILL_IMAGE_CAMERA");
        ComponentName mCameraComponentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195875));
        cameraIntent.setComponent(mCameraComponentName);
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", shortcut);
        return launchApp(cameraIntent);
    }

    private boolean launchCamera(String shortcut, String mode) {
        Intent cameraIntent = new Intent();
        cameraIntent.setFlags(SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX);
        cameraIntent.putExtra("ShowCameraWhenLocked", true);
        cameraIntent.putExtra("StartActivityWhenLocked", true);
        cameraIntent.setAction("android.media.action.VOICE_COMMAND");
        ComponentName mCameraComponentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195875));
        cameraIntent.setComponent(mCameraComponentName);
        cameraIntent.putExtra("android.intent.extra.CAMERA_MODE", mode);
        cameraIntent.putExtra("android.intent.extra.REFERRER", Uri.parse("android-app://com.android.camera"));
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", shortcut);
        return launchApp(cameraIntent);
    }

    private boolean launchScreenRecorder() {
        Intent intent = new Intent();
        intent.setAction("miui.intent.screenrecorder.RECORDER_SERVICE");
        intent.setPackage("com.miui.screenrecorder");
        intent.putExtra("is_start_immediately", false);
        this.mContext.startService(intent);
        return true;
    }

    public boolean vibrate() {
        AudioManagerHelper.toggleVibrateSetting(this.mContext);
        return true;
    }

    private boolean launchSoundRecorder() {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.android.soundrecorder", "com.android.soundrecorder.SoundRecorder");
        intent.setComponent(comp);
        return launchApp(intent);
    }

    private boolean launchMusicReader() {
        Intent intent = new Intent("android.intent.action.VIEW");
        ComponentName comp = new ComponentName("com.miui.player", "com.miui.player.ui.MusicBrowserActivity");
        intent.setData(Uri.parse("miui-music://radar?miref=com.miui.knock"));
        intent.setComponent(comp);
        return launchApp(intent);
    }

    private boolean launchWexinScanner(String shortcut) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.plugin.scanner.ui.BaseScanUI"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.tencent.mm");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195791));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195792));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayScanner(String shortcut) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setComponent(new ComponentName(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.mobile.scan.as.main.MainCaptureActivity"));
        intent.setFlags(268468224);
        Bundle bundle = new Bundle();
        bundle.putString("app_id", "10000007");
        Bundle bundleTemp = new Bundle();
        bundleTemp.putString("source", ACTION_INTENT_SHORTCUT);
        bundleTemp.putString("appId", "10000007");
        bundleTemp.putBoolean("REALLY_STARTAPP", true);
        bundleTemp.putString("showOthers", "YES");
        bundleTemp.putBoolean("startFromExternal", true);
        bundleTemp.putBoolean("REALLY_DOSTARTAPP", true);
        bundleTemp.putString("sourceId", ACTION_INTENT_SHORTCUT);
        bundleTemp.putString("ap_framework_sceneId", "20000001");
        bundle.putBundle("mExtras", bundleTemp);
        intent.putExtras(bundle);
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195787));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195792));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean lauchWeixinPaymentCode(String shortcut) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"));
        intent.putExtra("key_entry_scene", 2);
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.tencent.mm");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195790));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195792));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayPaymentCode(String shortcut) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.eg.android.AlipayGphone.FastStartActivity"));
        intent.setAction("android.intent.action.VIEW");
        intent.setFlags(343932928);
        intent.setData(Uri.parse("alipayss://platformapi/startapp?appId=20000056&source=shortcut"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195786));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195792));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayHealthCode(String shortcut) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setPackage(ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        intent.setFlags(343932928);
        intent.setData(Uri.parse("alipays://platformapi/startapp?appId=68687564&chInfo=ch_xiaomi_quick&sceneCode=KF_CHANGSHANG&shareUserId=2088831085791813&partnerId=ch_xiaomi_quick&pikshemo=YES"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195785));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195792));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchCalculator() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.calculator", "com.miui.calculator.cal.CalculatorActivity"));
        intent.setFlags(2097152);
        return launchApp(intent);
    }

    private boolean launchDumpLog() {
        Intent dumpLogIntent = new Intent();
        dumpLogIntent.setPackage("com.miui.bugreport");
        dumpLogIntent.setAction("com.miui.bugreport.service.action.DUMPLOG");
        dumpLogIntent.addFlags(32);
        this.mContext.sendBroadcastAsUser(dumpLogIntent, UserHandle.CURRENT);
        showToast(286196604, 0);
        StabilityLocalServiceInternal stabilityLocalServiceInternal = this.mStabilityLocalServiceInternal;
        if (stabilityLocalServiceInternal != null) {
            stabilityLocalServiceInternal.captureDumpLog();
            return true;
        }
        return true;
    }

    private void showToast(final int resourceId, final int length) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.util.ShortCutActionsUtils$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ShortCutActionsUtils.this.lambda$showToast$1(resourceId, length);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showToast$1(int resourceId, int length) {
        Context context = this.mContext;
        Toast.makeText(context, context.getString(resourceId), length).show();
    }

    private boolean launchDumpLogOrContact(String shortcut) {
        if (!this.mIsVoiceCapable) {
            MiuiInputLog.major("mIsVoiceCapable false, so lunch emergencyDialer");
            Intent intent = new Intent();
            ComponentName componentName = ComponentName.unflattenFromString("com.android.phone/com.android.phone.EmergencyDialer");
            intent.setComponent(componentName);
            intent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
            return launchApp(intent);
        }
        launchDumpLog();
        return true;
    }

    private boolean goToSleep(String shortcut) {
        long currentTime = SystemClock.uptimeMillis();
        if (this.mPowerManager != null) {
            MiuiInputLog.major("goToSleep, reason = " + shortcut);
            this.mPowerManager.goToSleep(currentTime);
            return true;
        }
        return false;
    }

    private boolean takeScreenshot(int type) {
        ScreenshotRequest request = new ScreenshotRequest.Builder(type).build();
        this.mScreenshotHelper.takeScreenshot(request, this.mHandler, (Consumer) null);
        return true;
    }

    private boolean launchAuPay() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setClassName("jp.auone.wallet", "jp.auone.wallet.ui.main.DeviceCredentialSchemeActivity");
        intent.putExtra("shortcut_start", "jp.auone.wallet.qr");
        intent.setFlags(536870912);
        if (!launchApp(intent)) {
            showToast(286195788, 0);
            return false;
        }
        return true;
    }

    private boolean takeScreenshotWithBundle(float[] pathList) {
        Bundle bundle = new Bundle();
        bundle.putFloatArray(PARTIAL_SCREENSHOT_POINTS, pathList);
        ScreenshotRequest request = new ScreenshotRequest.Builder(100).setExtraBundle(bundle).build();
        this.mScreenshotHelper.takeScreenshot(request, this.mHandler, (Consumer) null);
        return true;
    }

    private boolean launchGooglePay() {
        return false;
    }

    private Intent getPowerGuideIntent() {
        Intent powerGuideIntent = new Intent();
        powerGuideIntent.setClassName("com.miui.voiceassist", "com.xiaomi.voiceassistant.guidePage.PowerGuideDialogActivityV2");
        powerGuideIntent.putExtra("showSwitchNotice", true);
        powerGuideIntent.addFlags(805306368);
        return powerGuideIntent;
    }

    private boolean launchApp(Intent intent) {
        return launchApp(intent, null);
    }

    private boolean launchApp(Intent intent, Bundle bundle) {
        if ((intent.getFlags() & 2097152) != 0) {
            intent.addFlags(335544320);
        } else {
            intent.addFlags(343932928);
        }
        String packageName = null;
        if (!TextUtils.isEmpty(intent.getPackage())) {
            packageName = intent.getPackage();
        } else if (intent.getComponent() != null && !TextUtils.isEmpty(intent.getComponent().getPackageName())) {
            packageName = intent.getComponent().getPackageName();
        }
        if (packageName == null) {
            MiuiInputLog.major("package name is null");
            return false;
        }
        List<ResolveInfo> list = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, 851968, CrossUserUtils.getCurrentUserId());
        if (list != null && list.size() > 0) {
            try {
                this.mContext.startActivityAsUser(intent, bundle, UserHandle.CURRENT);
                return true;
            } catch (ActivityNotFoundException e) {
                MiuiInputLog.error("ActivityNotFoundException", e);
            } catch (IllegalStateException e2) {
                MiuiInputLog.error("IllegalStateException", e2);
            }
        } else {
            MiuiInputLog.major("launch app fail  package:" + packageName);
        }
        return false;
    }

    public void notifyKidSpaceChanged(boolean inKidSpace) {
        this.mIsKidMode = inKidSpace;
    }

    public static void sendRecordCountEvent(Context context, String category, String event) {
        Intent intent = new Intent("com.miui.gallery.intent.action.SEND_STAT");
        intent.setPackage(AccessController.PACKAGE_GALLERY);
        intent.putExtra("stat_type", "count_event");
        intent.putExtra("category", category);
        intent.putExtra("event", event);
        context.sendBroadcast(intent);
    }
}
