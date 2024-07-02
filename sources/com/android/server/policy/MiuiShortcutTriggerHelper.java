package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.input.MiuiInputThread;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import miui.hardware.input.InputFeature;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiuiShortcutTriggerHelper {
    public static final String ACTION_KEYBOARD = "keyboard:";
    private static final String DEVICE_REGION_RUSSIA = "ru";
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_CLOSE = 0;
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_LAUNCH_CAMERA = 1;
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_LAUNCH_CAMERA_AND_TAKE_PHOTO = 2;
    public static final int FUNCTION_DEFAULT = -1;
    public static final int FUNCTION_DISABLE = 0;
    public static final int FUNCTION_ENABLE = 1;
    private static final String KEY_IS_IN_MIUI_SOS_MODE = "key_is_in_miui_sos_mode";
    private static final String KEY_MIUI_SOS_ENABLE = "key_miui_sos_enable";
    private static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    private static final int LONG_PRESS_POWER_NOTHING = 0;
    public static final String PACKAGE_SMART_HOME = "com.miui.smarthomeplus";
    private static final String TAG = "MiuiShortcutTriggerHelp";
    public static final int TYPE_SOS_GOOGLE = 0;
    public static final int TYPE_SOS_MI = 1;
    public static final int VOICE_ASSIST_GUIDE_MAX_COUNT = 2;
    private static volatile MiuiShortcutTriggerHelper sMiuiShortcutTriggerHelper;
    private boolean mAOSPAssistantLongPressHomeEnabled;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId;
    public int mDefaultLongPressTimeOut;
    private int mFingerPrintNavCenterAction;
    private String mFivePressPowerLaunchGoogleSos;
    private boolean mFivePressPowerLaunchSos;
    private final Handler mHandler;
    private boolean mIsCtsMode;
    private boolean mIsOnSosMode;
    public boolean mLongPressPowerKeyLaunchSmartHome;
    public boolean mLongPressPowerKeyLaunchXiaoAi;
    private final PowerManager mPowerManager;
    private boolean mPressToAppSwitch;
    private final ShortcutSettingsObserver mShortcutSettingsObserver;
    private boolean mSingleKeyUse;
    private final boolean mSupportGoogleSos;
    private boolean mTorchEnabled;
    private final WindowManagerPolicy mWindowManagerPolicy;
    public static final String CURRENT_DEVICE_REGION = SystemProperties.get("ro.miui.build.region", "CN");
    public static final String CURRENT_DEVICE_CUSTOMIZED_REGION = SystemProperties.get("ro.miui.customized.region", "");
    public int mXiaoaiPowerGuideFlag = -1;
    public int mDoubleTapOnHomeBehavior = 0;
    private int mRSAGuideStatus = -1;
    private int mHandledKeyCodeByLongPress = -1;

    private MiuiShortcutTriggerHelper(Context context) {
        this.mContext = context;
        Handler handler = MiuiInputThread.getHandler();
        this.mHandler = handler;
        ContentResolver contentResolver = context.getContentResolver();
        this.mContentResolver = contentResolver;
        this.mSupportGoogleSos = context.getResources().getBoolean(17891647);
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        Settings.Secure.putInt(contentResolver, "support_gesture_shortcut_settings", 1);
        ShortcutSettingsObserver shortcutSettingsObserver = new ShortcutSettingsObserver(handler);
        this.mShortcutSettingsObserver = shortcutSettingsObserver;
        shortcutSettingsObserver.initShortcutSettingsObserver();
    }

    public static MiuiShortcutTriggerHelper getInstance(Context context) {
        if (sMiuiShortcutTriggerHelper == null) {
            synchronized (MiuiShortcutTriggerHelper.class) {
                if (sMiuiShortcutTriggerHelper == null) {
                    sMiuiShortcutTriggerHelper = new MiuiShortcutTriggerHelper(context);
                }
            }
        }
        return sMiuiShortcutTriggerHelper;
    }

    public boolean supportRSARegion() {
        return Build.IS_INTERNATIONAL_BUILD && !DEVICE_REGION_RUSSIA.equals(CURRENT_DEVICE_REGION);
    }

    public boolean supportAOSPTriggerFunction(int keyCode) {
        if (3 == keyCode) {
            return this.mAOSPAssistantLongPressHomeEnabled;
        }
        return true;
    }

    public int getRSAGuideStatus() {
        return this.mRSAGuideStatus;
    }

    public boolean isPressToAppSwitch() {
        return this.mPressToAppSwitch;
    }

    public void setPressToAppSwitch(boolean pressToAppSwitch) {
        this.mPressToAppSwitch = pressToAppSwitch;
    }

    public boolean isSingleKeyUse() {
        return this.mSingleKeyUse;
    }

    public int getFingerPrintNavCenterAction() {
        return this.mFingerPrintNavCenterAction;
    }

    public boolean isSupportLongPressPowerGuide() {
        int longPressPowerGuideStatus = -1;
        if (CURRENT_DEVICE_REGION.equals("cn")) {
            longPressPowerGuideStatus = this.mXiaoaiPowerGuideFlag;
        } else if (supportRSARegion()) {
            longPressPowerGuideStatus = this.mRSAGuideStatus;
        }
        return isUserSetUpComplete() && longPressPowerGuideStatus == 1;
    }

    public String getFivePressPowerLaunchGoogleSos() {
        return this.mFivePressPowerLaunchGoogleSos;
    }

    public boolean shouldLaunchSosByType(int type) {
        String str;
        switch (type) {
            case 0:
                return this.mSupportGoogleSos && ((str = this.mFivePressPowerLaunchGoogleSos) == null || "1".equals(str));
            case 1:
                return this.mFivePressPowerLaunchSos && !this.mIsOnSosMode;
            default:
                return false;
        }
    }

    public PowerManager getPowerManager() {
        return this.mPowerManager;
    }

    public static String getDoubleVolumeDownKeyFunction(String doubleVolumeDownStatus) {
        if (TextUtils.isEmpty(doubleVolumeDownStatus)) {
            return null;
        }
        int status = -1;
        try {
            status = Integer.parseInt(doubleVolumeDownStatus);
        } catch (NumberFormatException e) {
            Slog.d(TAG, "status is invalid,status=-1");
        }
        if (status == 1) {
            return "launch_camera";
        }
        if (status == 2) {
            return "launch_camera_and_take_photo";
        }
        return "none";
    }

    public boolean isTorchEnabled() {
        return this.mTorchEnabled;
    }

    public boolean isCtsMode() {
        return this.mIsCtsMode;
    }

    public int getDefaultLongPressTimeOut() {
        return this.mDefaultLongPressTimeOut;
    }

    public boolean shouldLaunchSos() {
        return shouldLaunchSosByType(0) || shouldLaunchSosByType(1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ShortcutSettingsObserver extends ContentObserver {
        public ShortcutSettingsObserver(Handler handler) {
            super(handler);
        }

        public void initShortcutSettingsObserver() {
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_xiaoai"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_smarthome"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("screen_key_press_app_switch"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("fingerprint_nav_center_action"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("single_key_use_enable"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_menu_key_when_lock"), false, this, -1);
            if (!Build.IS_GLOBAL_BUILD) {
                MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("xiaoai_power_guide"), false, this, -1);
                onChange(false, Settings.System.getUriFor("xiaoai_power_guide"));
            }
            if (MiuiShortcutTriggerHelper.this.supportRSARegion()) {
                MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("global_power_guide"), false, this, -1);
                onChange(false, Settings.System.getUriFor("global_power_guide"));
            }
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("pc_mode_open"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_smarthome"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("emergency_gesture_enabled"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_MIUI_SOS_ENABLE), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_IS_IN_MIUI_SOS_MODE), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("imperceptible_press_power_key"), false, this);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_MIUI_SOS_ENABLE), false, this);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_IS_IN_MIUI_SOS_MODE), false, this);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("long_press_timeout"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("torch_state"), false, this);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("assist_long_press_home_enabled"), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, this, -1);
            MiuiShortcutTriggerHelper.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            onChange(false);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper.mLongPressPowerKeyLaunchXiaoAi = Settings.System.getIntForUser(miuiShortcutTriggerHelper.mContentResolver, "long_press_power_launch_xiaoai", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper2 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper2.mPressToAppSwitch = Settings.System.getIntForUser(miuiShortcutTriggerHelper2.mContentResolver, "screen_key_press_app_switch", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId) != 0;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper3 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper3.mFingerPrintNavCenterAction = Settings.System.getIntForUser(miuiShortcutTriggerHelper3.mContentResolver, "fingerprint_nav_center_action", -1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper4 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper4.mSingleKeyUse = Settings.System.getIntForUser(miuiShortcutTriggerHelper4.mContentResolver, "single_key_use_enable", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper5 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper5.mDoubleTapOnHomeBehavior = miuiShortcutTriggerHelper5.mSingleKeyUse ? 1 : 0;
            if (!Build.IS_GLOBAL_BUILD) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper6 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper6.mXiaoaiPowerGuideFlag = Settings.System.getIntForUser(miuiShortcutTriggerHelper6.mContentResolver, "xiaoai_power_guide", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            }
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper7 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper7.mRSAGuideStatus = Settings.System.getIntForUser(miuiShortcutTriggerHelper7.mContentResolver, "global_power_guide", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper8 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper8.mFivePressPowerLaunchGoogleSos = Settings.Secure.getStringForUser(miuiShortcutTriggerHelper8.mContentResolver, "emergency_gesture_enabled", MiuiShortcutTriggerHelper.this.mCurrentUserId);
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper9 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper9.mFivePressPowerLaunchSos = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper9.mContentResolver, MiuiShortcutTriggerHelper.KEY_MIUI_SOS_ENABLE, 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper10 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper10.mIsOnSosMode = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper10.mContentResolver, MiuiShortcutTriggerHelper.KEY_IS_IN_MIUI_SOS_MODE, 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper11 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper11.mDefaultLongPressTimeOut = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper11.mContentResolver, "long_press_timeout", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper12 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper12.mTorchEnabled = Settings.Global.getInt(miuiShortcutTriggerHelper12.mContentResolver, "torch_state", 0) != 0;
            MiuiShortcutTriggerHelper miuiShortcutTriggerHelper13 = MiuiShortcutTriggerHelper.this;
            miuiShortcutTriggerHelper13.mAOSPAssistantLongPressHomeEnabled = Settings.Secure.getInt(miuiShortcutTriggerHelper13.mContentResolver, "assist_long_press_home_enabled", 1) == 1;
            MiuiShortcutTriggerHelper.this.mIsCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            if (Settings.System.getUriFor("long_press_power_launch_xiaoai").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper.mLongPressPowerKeyLaunchXiaoAi = Settings.System.getIntForUser(miuiShortcutTriggerHelper.mContentResolver, "long_press_power_launch_xiaoai", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
                if (Build.IS_GLOBAL_BUILD) {
                    Settings.System.putStringForUser(MiuiShortcutTriggerHelper.this.mContentResolver, "long_press_power_key", MiuiShortcutTriggerHelper.this.mLongPressPowerKeyLaunchXiaoAi ? "launch_google_search" : "none", MiuiShortcutTriggerHelper.this.mCurrentUserId);
                } else if (MiuiShortcutTriggerHelper.this.mLongPressPowerKeyLaunchXiaoAi) {
                    Settings.System.putStringForUser(MiuiShortcutTriggerHelper.this.mContentResolver, "long_press_power_key", "launch_voice_assistant", MiuiShortcutTriggerHelper.this.mCurrentUserId);
                }
            } else if (Settings.System.getUriFor("long_press_power_launch_smarthome").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper2 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper2.mLongPressPowerKeyLaunchSmartHome = Settings.System.getIntForUser(miuiShortcutTriggerHelper2.mContentResolver, "long_press_power_launch_smarthome", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
                if (MiuiShortcutTriggerHelper.this.mLongPressPowerKeyLaunchSmartHome) {
                    Settings.System.putStringForUser(MiuiShortcutTriggerHelper.this.mContentResolver, "long_press_power_key", "launch_smarthome", MiuiShortcutTriggerHelper.this.mCurrentUserId);
                }
            } else if (Settings.System.getUriFor("screen_key_press_app_switch").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper3 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper3.mPressToAppSwitch = Settings.System.getIntForUser(miuiShortcutTriggerHelper3.mContentResolver, "screen_key_press_app_switch", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId) != 0;
            } else if (Settings.System.getUriFor("fingerprint_nav_center_action").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper4 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper4.mFingerPrintNavCenterAction = Settings.System.getIntForUser(miuiShortcutTriggerHelper4.mContentResolver, "fingerprint_nav_center_action", -1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            } else if (Settings.System.getUriFor("single_key_use_enable").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper5 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper5.mSingleKeyUse = Settings.System.getIntForUser(miuiShortcutTriggerHelper5.mContentResolver, "single_key_use_enable", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper6 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper6.mDoubleTapOnHomeBehavior = miuiShortcutTriggerHelper6.mSingleKeyUse ? 1 : 0;
            } else if (Settings.System.getUriFor("xiaoai_power_guide").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper7 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper7.mXiaoaiPowerGuideFlag = Settings.System.getIntForUser(miuiShortcutTriggerHelper7.mContext.getContentResolver(), "xiaoai_power_guide", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor("emergency_gesture_enabled").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper8 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper8.mFivePressPowerLaunchGoogleSos = Settings.Secure.getStringForUser(miuiShortcutTriggerHelper8.mContentResolver, "emergency_gesture_enabled", MiuiShortcutTriggerHelper.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_MIUI_SOS_ENABLE).equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper9 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper9.mFivePressPowerLaunchSos = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper9.mContentResolver, MiuiShortcutTriggerHelper.KEY_MIUI_SOS_ENABLE, 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            } else if (Settings.Secure.getUriFor(MiuiShortcutTriggerHelper.KEY_IS_IN_MIUI_SOS_MODE).equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper10 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper10.mIsOnSosMode = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper10.mContentResolver, MiuiShortcutTriggerHelper.KEY_IS_IN_MIUI_SOS_MODE, 0, MiuiShortcutTriggerHelper.this.mCurrentUserId) == 1;
            } else if (Settings.System.getUriFor("global_power_guide").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper11 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper11.mRSAGuideStatus = Settings.System.getIntForUser(miuiShortcutTriggerHelper11.mContentResolver, "global_power_guide", 1, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor("long_press_timeout").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper12 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper12.mDefaultLongPressTimeOut = Settings.Secure.getIntForUser(miuiShortcutTriggerHelper12.mContentResolver, "long_press_timeout", 0, MiuiShortcutTriggerHelper.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor("assist_long_press_home_enabled").equals(uri)) {
                MiuiShortcutTriggerHelper miuiShortcutTriggerHelper13 = MiuiShortcutTriggerHelper.this;
                miuiShortcutTriggerHelper13.mAOSPAssistantLongPressHomeEnabled = Settings.Secure.getInt(miuiShortcutTriggerHelper13.mContentResolver, "assist_long_press_home_enabled", -1) == 1;
            } else if (Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION).equals(uri)) {
                MiuiShortcutTriggerHelper.this.mIsCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
            } else if (Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                MiuiShortcutTriggerHelper.this.setVeryLongPressPowerBehavior();
            }
            super.onChange(z, uri);
        }
    }

    private int getVeryLongPressPowerBehavior() {
        if (!isUserSetUpComplete()) {
            return 0;
        }
        String stringForUser = Settings.System.getStringForUser(this.mContentResolver, "long_press_power_key", this.mCurrentUserId);
        if (isSupportLongPressPowerGuide() || !(TextUtils.isEmpty(stringForUser) || "none".equals(stringForUser))) {
            return !InputFeature.IS_SUPPORT_KDDI_POWER_GUIDE ? 1 : 0;
        }
        return 0;
    }

    public void setVeryLongPressPowerBehavior() {
        int behavior = getVeryLongPressPowerBehavior();
        Slog.d(TAG, "set veryLongPressBehavior=" + behavior);
        Settings.Global.putInt(this.mContentResolver, "power_button_very_long_press", behavior);
    }

    public void setLongPressPowerBehavior(String function) {
        if (isUserSetUpComplete() && InputFeature.IS_SUPPORT_KDDI_POWER_GUIDE) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "power_button_long_press", (TextUtils.isEmpty(function) || "none".equals(function)) ? 1 : 0);
        }
    }

    public boolean isUserSetUpComplete() {
        WindowManagerPolicy windowManagerPolicy = this.mWindowManagerPolicy;
        return windowManagerPolicy != null && windowManagerPolicy.isUserSetupComplete();
    }

    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mCurrentUserId = currentUserId;
        this.mShortcutSettingsObserver.onChange(false);
        setupMenuFunction();
        if (isNewUser) {
            resetMiuiShortcutSettings();
        }
        Settings.Secure.putIntForUser(this.mContentResolver, "support_gesture_shortcut_settings", 1, currentUserId);
    }

    private void setupMenuFunction() {
        String menuKeyCurrentFunction = Settings.System.getStringForUser(this.mContentResolver, "screen_key_press_app_switch", this.mCurrentUserId);
        if (menuKeyCurrentFunction != null) {
            return;
        }
        int i = this.mCurrentUserId;
        boolean isAppSwitch = i == 0;
        Settings.System.putIntForUser(this.mContentResolver, "screen_key_press_app_switch", isAppSwitch ? 1 : 0, i);
    }

    public boolean isLegalData(int global, String region) {
        if (TextUtils.isEmpty(region)) {
            if (Build.IS_INTERNATIONAL_BUILD || global != 0) {
                return (1 == global && Build.IS_INTERNATIONAL_BUILD) || 2 == global;
            }
            return true;
        }
        List<String> regionList = Arrays.asList(region.split(","));
        return regionList.contains(CURRENT_DEVICE_REGION);
    }

    public void resetMiuiShortcutSettings() {
        this.mShortcutSettingsObserver.onChange(false);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mPressToAppSwitch=");
        pw.println(this.mPressToAppSwitch);
        pw.print(prefix2);
        pw.print("isMiuiSosCanBeTrigger=");
        pw.println(shouldLaunchSosByType(1));
        pw.print(prefix2);
        pw.print("isGoogleSosEnable=");
        pw.println(shouldLaunchSosByType(0));
        pw.print(prefix2);
        pw.print("mFingerPrintNavCenterAction=");
        pw.println(this.mFingerPrintNavCenterAction);
        pw.print(prefix2);
        pw.print("mXiaoaiPowerGuideFlag=");
        pw.println(this.mXiaoaiPowerGuideFlag);
        pw.print(prefix2);
        pw.print("mDefaultLongPressTimeOut=");
        pw.println(this.mDefaultLongPressTimeOut);
        pw.print(prefix2);
        pw.print("mTorchEnabled=");
        pw.println(this.mTorchEnabled);
        pw.print(prefix2);
        pw.print("mAOSPAssistantLongPressHomeEnabled=");
        pw.println(this.mAOSPAssistantLongPressHomeEnabled);
        pw.print(prefix2);
        pw.print("mIsCtsMode=");
        pw.println(this.mIsCtsMode);
        pw.print(prefix2);
        pw.print("mRSAGuideStatus=");
        pw.println(this.mRSAGuideStatus);
        pw.print(prefix2);
        pw.print("mHandledKeyCodeByLongPress=");
        pw.println(this.mHandledKeyCodeByLongPress);
    }

    public boolean skipKeyGesutre(KeyEvent event) {
        if (event.getRepeatCount() != 0 || (event.getFlags() & BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT) != 0) {
            return true;
        }
        return false;
    }

    public void notifyLongPressed(int keyCode) {
        this.mHandledKeyCodeByLongPress = keyCode;
    }

    public void resetKeyCodeByLongPress() {
        this.mHandledKeyCodeByLongPress = -1;
    }

    public boolean interceptKeyByLongPress(KeyEvent event) {
        if (this.mHandledKeyCodeByLongPress != event.getKeyCode() || (event.getFlags() & 1024) != 0) {
            return false;
        }
        return true;
    }
}
