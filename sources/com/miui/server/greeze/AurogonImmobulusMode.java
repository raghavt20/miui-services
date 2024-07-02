package com.miui.server.greeze;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.am.ActivityManagerServiceStub;
import com.android.server.appwidget.MiuiAppWidgetServiceImpl;
import com.android.server.wm.ActivityTaskManagerServiceStub;
import com.miui.server.AccessController;
import com.miui.server.SplashScreenServiceDelegate;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.abtest.d.d;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.app.StorageRestrictedPathManager;

/* loaded from: classes.dex */
public class AurogonImmobulusMode {
    private static final String AUROGON_ENABLE = "aurogon_enable";
    private static final String BLUETOOTH_GLOBAL_SETTING = "RECORD_BLE_APPNAME";
    public static final String BROADCAST_SATELLITE = "com.android.app.action.SATELLITE_STATE_CHANGE";
    private static final String IMMOBULUS_GAME_CONTROLLER = "com.xiaomi.joyose.action.GAME_STATUS_UPDATE";
    private static final String IMMOBULUS_GAME_KEY_ALLOW_LIST = "com.xiaomi.joyose.key.BACKGROUND_FREEZE_WHITELIST";
    private static final String IMMOBULUS_GAME_KEY_STATUS = "com.xiaomi.joyose.key.GAME_STATUS";
    private static final int IMMOBULUS_GAME_VALUE_QUIT = 2;
    private static final int IMMOBULUS_GAME_VALUE_TRIGGER = 1;
    public static final int IMMOBULUS_LAUNCH_MODE_REPEAT_TIME = 500;
    private static final int IMMOBULUS_LEVEL_FORCESTOP = 3;
    public static final int IMMOBULUS_REPEAT_TIME = 2000;
    private static final String IMMOBULUS_SWITCH_SECURE_SETTING = "immobulus_mode_switch_restrict";
    public static final int MSG_IMMOBULUS_MODE_QUIT_ACTION = 102;
    public static final int MSG_IMMOBULUS_MODE_TRIGGER_ACTION = 101;
    public static final int MSG_LAUNCH_MODE_QUIT_ACTION = 106;
    public static final int MSG_LAUNCH_MODE_TRIGGER_ACTION = 105;
    public static final int MSG_REMOVE_ALL_MESSAGE = 104;
    public static final int MSG_REPEAT_FREEZE_APP = 103;
    private static final String PC_SECURITY_CENTER_EXTREME_MODE = "pc_security_center_extreme_mode_enter";
    public static final String SATELLITE_STATE = "satellite_state";
    public static final String TAG = "AurogonImmobulusMode";
    private static final long TIME_FREEZE_DURATION_UNUSEFUL = 20000;
    private static final long TIME_FREEZE_DURATION_USEFUL = 60000;
    private static final long TIME_LAUNCH_MODE_TIMEOUT = 3000;
    public static final int TYPE_MODE_IMMOBULUS = 8;
    public static final int TYPE_MODE_LAUNCH = 16;
    public Context mContext;
    public GreezeManagerService mGreezeService;
    public ImmobulusHandler mHandler;
    public boolean mImmobulusModeEnabled;
    public boolean mLaunchModeEnabled;
    private PackageManager mPm;
    public ProcessManagerInternal mProcessManagerInternal;
    private SettingsObserver mSettingsObserver;
    private static String KEY_NO_RESTRICT_APP = "MILLET_NO_RESTRICT_APP";
    private static final String AUROGON_IMMOBULUS_SWITCH_PROPERTY = "persist.sys.aurogon.immobulus";
    public static final boolean IMMOBULUS_ENABLED = SystemProperties.getBoolean(AUROGON_IMMOBULUS_SWITCH_PROPERTY, true);
    public static final boolean CN_MODEL = "CN".equals(SystemProperties.get("ro.miui.region", "unknown"));
    private static List<String> REASON_FREEZE = new ArrayList(Arrays.asList("IMMOBULUS", "LAUNCH_MODE"));
    private static List<String> REASON_UNFREEZE = new ArrayList(Arrays.asList("SYNC_BINDER", "ASYNC_BINDER", "PACKET", "BINDER_SERVICE", "SIGNAL", "BROADCAST"));
    private static List<String> ENABLE_DOUBLE_APP = new ArrayList(Arrays.asList("nuwa", "ishtar", "babylon", "fuxi"));
    public static List<String> mMessageApp = new ArrayList(Arrays.asList("com.tencent.mobileqq", "com.tencent.mm", "com.tencent.tim", "com.alibaba.android.rimet", "com.ss.android.lark", "com.ss.android.lark.kami", "com.tencent.wework"));
    public List<String> mImmobulusAllowList = null;
    public List<AurogonAppInfo> mImmobulusTargetList = null;
    public Map<Integer, List<Integer>> mFgServiceAppList = null;
    public List<Integer> mVPNAppList = null;
    public List<AurogonAppInfo> mQuitImmobulusList = null;
    public List<AurogonAppInfo> mQuitLaunchModeList = null;
    public List<String> mBluetoothUsingList = null;
    public boolean mEnterImmobulusMode = false;
    public boolean mLastBarExpandIMStatus = false;
    public boolean mVpnConnect = false;
    public boolean mEnabledLMCamera = true;
    public boolean mEnterIMCamera = false;
    public boolean mExtremeMode = false;
    private boolean mExtremeModeCloud = true;
    private boolean mDoubleAppCtrlCloud = true;
    public int mCameraUid = -1;
    public boolean mEnterIMVideo = true;
    private boolean mEnterIMVideoCloud = true;
    public String mCurrentVideoPacageName = null;
    public boolean mBroadcastCtrlCloud = true;
    public boolean mCtsModeOn = false;
    public ConnectivityManager mConnMgr = null;
    public String mCurrentIMEPacageName = "";
    public int mCurrentIMEUid = -1;
    public String mLastPackageName = "";
    public String mWallPaperPackageName = "";
    public List<String> mCurrentWidgetPackages = new ArrayList();
    private List<String> mLocalAllowList = new ArrayList(Arrays.asList("android.uid.shared", SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE, "com.android.providers.media.module", "com.google.android.webview", "com.miui.voicetrigger", "com.miui.voiceassist", "com.dewmobile.kuaiya", "com.android.permissioncontroller", "com.android.htmlviewer", "com.google.android.providers.media.module", "com.android.incallui", "org.codeaurora.ims", "com.android.providers.contacts", "com.xiaomi.xaee", "com.android.calllogbackup", "com.android.providers.blockednumber", "com.android.providers.userdictionary", "com.xiaomi.aireco", "com.miui.securityinputmethod", "com.miui.home", "com.miui.newhome", "com.miui.screenshot", "com.lbe.security.miui", "org.ifaa.aidl.manager", "com.xiaomi.macro", "com.miui.rom", "com.miui.personalassistant", MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE, "com.miui.mediaviewer", "com.xiaomi.gamecenter.sdk.service", "com.xiaomi.scanner", "com.android.cameraextensions", "com.xiaomi.xmsf", "com.xiaomi.account", "com.xiaomi.metoknlp", "com.duokan.reader", "com.android.mms", "com.xiaomi.mibrain.speech", "com.baidu.carlife.xiaomi", "com.xiaomi.miralink", "com.miui.core"));
    public List<String> mVideoAppList = new ArrayList(Arrays.asList("com.ss.android.ugc.aweme.lite", "com.ss.android.ugc.aweme", "com.kuaishou.nebula", "com.smile.gifmaker", "tv.danmaku.bili", "com.tencent.qqlive", "com.ss.android.article.video", "com.qiyi.video", "com.youku.phone", "com.miui.video", "com.duowan.kiwi", "air.tv.douyu.android", "com.le123.ysdq", "com.hunantv.imgo.activity"));
    private Set<String> mNoRestrictAppSet = new HashSet();
    private List<String> mCloudAllowList = null;
    private List<String> mAllowList = new ArrayList();
    private List<String> mImportantAppList = new ArrayList(Arrays.asList("com.miui.home", AccessController.PACKAGE_CAMERA, "com.goodix.fingerprint.setting", AccessController.PACKAGE_GALLERY));
    private List<String> CTS_NAME = new ArrayList(Arrays.asList("android.net.cts", "com.android.cts.verifier"));
    public List<Integer> mOnWindowsAppList = new ArrayList();
    public List<Integer> mMutiWindowsApp = new ArrayList();
    List<String> mAllowWakeUpPackageNameList = new ArrayList(Arrays.asList("com.tencent.mm", "com.google.android.gms"));

    public AurogonImmobulusMode(Context context, HandlerThread ht, GreezeManagerService service) {
        this.mContext = null;
        this.mImmobulusModeEnabled = true;
        this.mLaunchModeEnabled = true;
        this.mHandler = null;
        this.mGreezeService = null;
        this.mSettingsObserver = null;
        this.mPm = null;
        this.mProcessManagerInternal = null;
        this.mContext = context;
        if (IMMOBULUS_ENABLED) {
            this.mHandler = new ImmobulusHandler(ht.getLooper());
            this.mPm = this.mContext.getPackageManager();
            this.mGreezeService = service;
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            this.mAllowList.addAll(this.mLocalAllowList);
            init();
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(BLUETOOTH_GLOBAL_SETTING), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(IMMOBULUS_SWITCH_SECURE_SETTING), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this.mSettingsObserver, -2);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, this.mSettingsObserver, -2);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(AUROGON_ENABLE), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_NO_RESTRICT_APP), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiAppWidgetServiceImpl.ENABLED_WIDGETS), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(PC_SECURITY_CENTER_EXTREME_MODE), false, this.mSettingsObserver, -2);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SATELLITE_STATE), false, this.mSettingsObserver, -2);
            this.mProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            new ImmobulusBroadcastReceiver();
            updateCloudAllowList();
            updateIMEAppStatus();
            updateWidgetPackages();
            getConnectivityManager();
            if (this.mGreezeService.DISABLE_IMMOB_MODE_DEVICE.contains(Build.DEVICE)) {
                this.mImmobulusModeEnabled = false;
            }
        } else {
            this.mImmobulusModeEnabled = false;
            this.mLaunchModeEnabled = false;
        }
        getNoRestrictApps();
    }

    private void updateCtsStatus() {
        String freezeEnable = Settings.Global.getString(this.mContext.getContentResolver(), "cached_apps_freezer");
        Boolean DEFAULT_USE_FREEZER = true;
        if (this.mCtsModeOn) {
            SystemProperties.set("persist.sys.powmillet.enable", "false");
            if ("disabled".equals(freezeEnable)) {
                Settings.Global.putString(this.mContext.getContentResolver(), "cached_apps_freezer", "enabled");
            }
            this.mGreezeService.thawAll("tsMode");
            return;
        }
        if ("enabled".equals(freezeEnable) || DeviceConfig.getBoolean("activity_manager_native_boot", "use_freezer", DEFAULT_USE_FREEZER.booleanValue())) {
            Settings.Global.putString(this.mContext.getContentResolver(), "cached_apps_freezer", "disabled");
        }
    }

    public void initCtsStatus() {
        if (GreezeManagerDebugConfig.mPowerMilletEnable) {
            updateCtsStatus();
        }
    }

    private void init() {
        this.mImmobulusAllowList = new ArrayList();
        this.mImmobulusTargetList = new ArrayList();
        this.mBluetoothUsingList = new ArrayList();
        this.mQuitImmobulusList = new ArrayList();
        this.mQuitLaunchModeList = new ArrayList();
        this.mCloudAllowList = new ArrayList();
        this.mFgServiceAppList = new HashMap();
        this.mVPNAppList = new ArrayList();
    }

    private boolean InStatusBarScene(String name) {
        if ("com.milink.service".equals(this.mGreezeService.mTopAppPackageName) && "com.xiaomi.smarthome".equals(name)) {
            return true;
        }
        return false;
    }

    public void reOrderTargetList(final int uid) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AurogonImmobulusMode.this.mImmobulusTargetList) {
                    AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(uid);
                    if (app == null) {
                        return;
                    }
                    AurogonImmobulusMode.this.mImmobulusTargetList.remove(app);
                    AurogonImmobulusMode.this.mImmobulusTargetList.add(app);
                }
            }
        });
    }

    public void TriggerImmobulusModeAction(int uid, String reason) {
        if (isRunningLaunchMode()) {
            sendMeesage(101, uid, -1, reason, 3500L);
            return;
        }
        StringBuilder log = new StringBuilder();
        log.append("IM " + reason + " FZ [" + uid + "] uid = [");
        updateAppsOnWindowsStatus();
        updateIMEAppStatus();
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (this.mImmobulusAllowList.contains(app.mPackageName)) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " is in ImmobulusAllowList, skip it!");
                    }
                } else if (this.mAllowList.contains(app.mPackageName)) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " in Allowlist, skip it!");
                    }
                } else if (!this.mExtremeMode && this.mNoRestrictAppSet.contains(app.mPackageName)) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " is in NoRestrictAppSet, skip it!");
                    }
                } else if (InStatusBarScene(app.mPackageName)) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " is in statusbar, skip it!");
                    }
                } else if (isWallPaperApp(app.mPackageName)) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " is WallPaperApp app, skip check!");
                    }
                } else if (!this.mGreezeService.isAppRunning(app.mUid) && this.mGreezeService.mScreenOnOff) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + " is not running, skip check!");
                    }
                } else if (this.mGreezeService.isAppRunningInFg(app.mUid)) {
                    Slog.d(TAG, " uid = " + app.mUid + "is running in FG, skip check!");
                } else if (this.mGreezeService.isUidFrozen(app.mUid)) {
                    this.mGreezeService.updateFrozenInfoForImmobulus(app.mUid, 8);
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(TAG, " uid = " + app.mUid + "updateFrozenInfoForImmobulus!");
                    }
                } else {
                    List<Integer> list = this.mOnWindowsAppList;
                    if (list != null && list.contains(Integer.valueOf(app.mUid))) {
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.d(TAG, " uid = " + app.mUid + " is show on screen, skip it!");
                        }
                    } else if (!app.hasIcon) {
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.d(TAG, " uid = " + app.mUid + " not has icon, skip it!");
                        }
                    } else if (!checkAppStatusForFreeze(app)) {
                        repeatCheckAppForImmobulusMode(app, 2000);
                    } else {
                        boolean success = freezeActionForImmobulus(app, "IMMOBULUS");
                        if (success) {
                            log.append(" " + app.mUid);
                        }
                    }
                }
            }
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
    }

    public void QuitImmobulusModeAction() {
        this.mEnterImmobulusMode = false;
        StringBuilder log = new StringBuilder();
        log.append("IM finish THAW uid = [");
        for (AurogonAppInfo app : this.mQuitImmobulusList) {
            boolean success = unFreezeActionForImmobulus(app, "IMMOBULUS");
            if (success) {
                log.append(" " + app.mUid);
            }
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
        this.mGreezeService.resetStatusForImmobulus(8);
        resetImmobulusModeStatus();
        removeAllMsg();
        this.mQuitImmobulusList.clear();
    }

    public void checkAppForImmobulusMode(AurogonAppInfo app) {
        if (!this.mEnterImmobulusMode || this.mGreezeService.isAppRunningInFg(app.mUid) || this.mGreezeService.isUidFrozen(app.mUid) || this.mImmobulusAllowList.contains(app.mPackageName) || !this.mGreezeService.isAppRunning(app.mUid)) {
            return;
        }
        if (!checkAppStatusForFreeze(app)) {
            if (isRunningLaunchMode()) {
                repeatCheckAppForImmobulusMode(app, 500);
                return;
            } else {
                repeatCheckAppForImmobulusMode(app, 2000);
                return;
            }
        }
        freezeActionForImmobulus(app, "repeat");
    }

    public void repeatCheckAppForImmobulusMode(int uid, int time) {
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null) {
            repeatCheckAppForImmobulusMode(app, time);
        }
    }

    public void repeatCheckAppForImmobulusMode(AurogonAppInfo app, int time) {
        if (app == null || this.mHandler.hasMessages(103, app) || !app.hasIcon) {
            return;
        }
        sendMeesage(103, -1, -1, app, time);
    }

    public boolean checkAppStatusForFreeze(AurogonAppInfo app) {
        if (this.mGreezeService.isAppRunningInFg(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + "is running in FG, skip check!");
            }
            return false;
        }
        if (this.mGreezeService.isUidActive(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using GPS/Audio/Vibrator!");
            }
            return false;
        }
        if (this.mGreezeService.checkFreeformSmallWin(app.mPackageName)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, "Uid " + app.mUid + " was Freeform small window, skip it");
            }
            return false;
        }
        if (this.mBluetoothUsingList.contains(app.mPackageName)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using BT!");
            }
            return false;
        }
        if (ActivityManagerServiceStub.get().isBackuping(app.mUid) || ActivityManagerServiceStub.get().isActiveInstruUid(app.mUid) || ActivityManagerServiceStub.get().isVibratorActive(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using backingg, activeIns or vibrator");
            }
            return false;
        }
        if (ActivityTaskManagerServiceStub.get().getActivityControllerUid() == app.mUid || this.mCurrentIMEPacageName.equals(app.mPackageName)) {
            return false;
        }
        if (this.mGreezeService.mScreenOnOff && isWidgetApp(app.mPackageName)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, "uid = " + app.mUid + " is widget app!");
            }
            return false;
        }
        if (this.mVpnConnect && isVpnApp(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, "uid = " + app.mUid + " is vpn app!");
            }
            return false;
        }
        if (checkMutiWindowsApp(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, "uid = " + app.mUid + " is in MutiWindows!");
            }
            return false;
        }
        if (this.mGreezeService.checkOrderBCRecivingApp(app.mUid)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, "freeze app was reciving broadcast! mUid = " + app.mUid);
            }
            return false;
        }
        if (isDownloadApp(app.mUid, app.mPackageName)) {
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is downloading.");
            }
            return false;
        }
        return true;
    }

    public boolean freezeActionForImmobulus(AurogonAppInfo app, String reason) {
        if (app.isSystemApp || checkAppFgService(app.mUid)) {
            addImmobulusModeQuitList(app);
        }
        boolean success = this.mGreezeService.freezeAction(app.mUid, 8, reason, false);
        if (success) {
            app.freezeTime = SystemClock.uptimeMillis();
        } else if (!this.mGreezeService.isUidFrozen(app.mUid)) {
            repeatCheckAppForImmobulusMode(app, 2000);
        }
        return success;
    }

    public boolean unFreezeActionForImmobulus(AurogonAppInfo app, String reason) {
        boolean success = this.mGreezeService.thawUid(app.mUid, 8, reason);
        if (success) {
            if (this.mEnterImmobulusMode) {
                long currentTime = SystemClock.uptimeMillis();
                long durration = currentTime - app.freezeTime;
                if (durration < 20000) {
                    app.level++;
                } else if (durration > 60000) {
                    app.level--;
                }
                app.unFreezeTime = currentTime;
                if (app.level >= 3) {
                    forceStopAppForImmobulus(app.mPackageName, "frequent wakeup");
                    app.level = 0;
                    return success;
                }
                repeatCheckAppForImmobulusMode(app, 2000);
            }
        } else {
            Slog.d(TAG, " unFreeze uid = " + app.mUid + " reason : " + reason + " failed!");
        }
        return success;
    }

    public void forceStopAppForImmobulus(String packageName, String reason) {
        this.mGreezeService.forceStopPackage(packageName, this.mContext.getUserId(), reason);
    }

    public boolean isSystemOrMiuiImportantApp(int uid) {
        String packageName = this.mGreezeService.getPackageNameFromUid(uid);
        if (packageName != null) {
            return isSystemOrMiuiImportantApp(packageName);
        }
        return false;
    }

    public boolean isSystemOrMiuiImportantApp(String packageName) {
        return packageName.contains(".miui") || packageName.contains(".xiaomi") || packageName.contains(".google") || packageName.contains("com.android") || packageName.contains("com.mi.") || this.mImportantAppList.contains(packageName) || isSystemApp(packageName);
    }

    public boolean isRunningLaunchMode() {
        return this.mHandler.hasMessages(106);
    }

    public boolean isRunningImmobulusMode() {
        if (!this.mImmobulusModeEnabled) {
            return false;
        }
        if (this.mEnterIMCamera || this.mExtremeMode || this.mEnterImmobulusMode) {
            return true;
        }
        return false;
    }

    public void sendMeesage(int what, int args1, int args2, Object obj, long delayTime) {
        Message msg = this.mHandler.obtainMessage(what);
        if (args1 != -1) {
            msg.arg1 = args1;
        }
        if (args2 != -1) {
            msg.arg2 = args2;
        }
        if (obj != null) {
            msg.obj = obj;
        }
        if (delayTime == -1) {
            this.mHandler.sendMessage(msg);
        } else {
            this.mHandler.sendMessageDelayed(msg, delayTime);
        }
    }

    public boolean checkAppFgService(int uid) {
        synchronized (this.mFgServiceAppList) {
            List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
            return list != null && list.size() > 0;
        }
    }

    public AurogonAppInfo getAurogonAppInfo(int uid) {
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (app.mUid == uid) {
                    return app;
                }
            }
            return null;
        }
    }

    public AurogonAppInfo getAurogonAppInfo(String packageName) {
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (app.mPackageName == packageName) {
                    return app;
                }
            }
            return null;
        }
    }

    public void notifyAppSwitchToBg(int uid) {
        if (IMMOBULUS_ENABLED) {
            String packageName = getPackageNameFromUid(uid);
            if (AccessController.PACKAGE_CAMERA.equals(packageName) && this.mEnterImmobulusMode) {
                quitImmobulusMode();
            }
            AurogonAppInfo app = getAurogonAppInfo(uid);
            updateTargetList(uid);
            if (this.mExtremeMode && app != null) {
                repeatCheckAppForImmobulusMode(app, 2000);
            }
        }
    }

    public void notifyAppActive(int uid) {
        if (IMMOBULUS_ENABLED) {
            updateTargetList(uid);
            AurogonAppInfo app = getAurogonAppInfo(uid);
            if (app != null && !this.mAllowList.contains(app.mPackageName) && this.mEnterImmobulusMode) {
                repeatCheckAppForImmobulusMode(app, 2000);
            }
        }
    }

    public void notifyFgServicesChanged(int pid, int uid) {
        if (this.mProcessManagerInternal == null) {
            Slog.d(TAG, " mProcessManagerInternal = null");
            this.mProcessManagerInternal = getProcessManagerInternal();
        }
        if (getAurogonAppInfo(uid) == null) {
            return;
        }
        checkFgServicesList();
        if (this.mProcessManagerInternal.checkAppFgServices(pid)) {
            updateFgServicesList(uid, pid, true);
        } else {
            updateFgServicesList(uid, pid, false);
        }
    }

    private void checkFgServicesList() {
        if (this.mProcessManagerInternal == null) {
            return;
        }
        synchronized (this.mFgServiceAppList) {
            List<Integer> tempUidList = new ArrayList<>();
            Iterator<Integer> it = this.mFgServiceAppList.keySet().iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
                if (list != null) {
                    List<Integer> tempList = new ArrayList<>();
                    Iterator<Integer> it2 = list.iterator();
                    while (it2.hasNext()) {
                        int pid = it2.next().intValue();
                        if (!this.mProcessManagerInternal.checkAppFgServices(pid)) {
                            tempList.add(Integer.valueOf(pid));
                        }
                    }
                    Iterator<Integer> it3 = tempList.iterator();
                    while (it3.hasNext()) {
                        int tempPid = it3.next().intValue();
                        list.remove(Integer.valueOf(tempPid));
                    }
                    if (list.size() == 0) {
                        tempUidList.add(Integer.valueOf(uid));
                    }
                }
            }
            Iterator<Integer> it4 = tempUidList.iterator();
            while (it4.hasNext()) {
                int tempUid = it4.next().intValue();
                this.mFgServiceAppList.remove(Integer.valueOf(tempUid));
            }
        }
    }

    public ProcessManagerInternal getProcessManagerInternal() {
        ProcessManagerInternal pmi = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        return pmi;
    }

    public void updateAppsOnWindowsStatus() {
        if (this.mGreezeService.mWindowManager != null) {
            try {
                List<String> list = this.mGreezeService.mWindowManager.getAppsOnWindowsStatus();
                if (list != null) {
                    this.mOnWindowsAppList.clear();
                    for (String uid : list) {
                        this.mOnWindowsAppList.add(Integer.valueOf(Integer.parseInt(uid)));
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.d(TAG, "Integer.parseInt(uid) = " + Integer.parseInt(uid));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public boolean isUidValid(int uid) {
        Slog.d("isUidValid", "uid = " + uid);
        if (uid < 10000 || !UserHandle.isApp(uid)) {
            return false;
        }
        if (uid > 19999) {
            return this.mDoubleAppCtrlCloud && ENABLE_DOUBLE_APP.contains(Build.DEVICE);
        }
        return true;
    }

    public void updateTargetList(int uid) {
        synchronized (this.mImmobulusTargetList) {
            if (isUidValid(uid)) {
                Iterator<AurogonAppInfo> it = this.mImmobulusTargetList.iterator();
                while (it.hasNext()) {
                    if (it.next().mUid == uid) {
                        return;
                    }
                }
                String packageName = getPackageNameFromUid(uid);
                if (packageName == null) {
                    return;
                }
                if (AccessController.PACKAGE_CAMERA.equals(packageName)) {
                    this.mCameraUid = uid;
                }
                String packageName2 = packageName.split(":")[0];
                if (!packageName2.contains(".qualcomm") && !packageName2.contains("com.qti")) {
                    if (isCtsApp(packageName2) && !packageName2.contains("ctsshim")) {
                        QuitLaunchModeAction(false);
                        this.mLaunchModeEnabled = false;
                        this.mCtsModeOn = true;
                        Slog.d(TAG, "ts " + packageName2);
                        updateCtsStatus();
                    }
                    AurogonAppInfo app = new AurogonAppInfo(uid, packageName2);
                    if (isAppHasIcon(packageName2)) {
                        app.hasIcon = true;
                    }
                    if (isSystemOrMiuiImportantApp(packageName2)) {
                        app.isSystemApp = true;
                    }
                    this.mImmobulusTargetList.add(app);
                }
            }
        }
    }

    public void addLaunchModeQiutList(final int uid) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.2
            @Override // java.lang.Runnable
            public void run() {
                AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(uid);
                synchronized (AurogonImmobulusMode.this.mQuitLaunchModeList) {
                    for (AurogonAppInfo temp : AurogonImmobulusMode.this.mQuitLaunchModeList) {
                        if (uid == temp.mUid) {
                            return;
                        }
                    }
                    if (app != null) {
                        AurogonImmobulusMode.this.mQuitLaunchModeList.add(app);
                    }
                }
            }
        });
    }

    public void addLaunchModeQiutList(final AurogonAppInfo app) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.3
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AurogonImmobulusMode.this.mQuitLaunchModeList) {
                    for (AurogonAppInfo temp : AurogonImmobulusMode.this.mQuitLaunchModeList) {
                        if (app.mUid == temp.mUid) {
                            return;
                        }
                    }
                    AurogonImmobulusMode.this.mQuitLaunchModeList.add(app);
                }
            }
        });
    }

    public void addImmobulusModeQuitList(final AurogonAppInfo app) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AurogonImmobulusMode.this.mQuitImmobulusList) {
                    for (AurogonAppInfo temp : AurogonImmobulusMode.this.mQuitImmobulusList) {
                        if (app.mUid == temp.mUid) {
                            return;
                        }
                    }
                    AurogonImmobulusMode.this.mQuitImmobulusList.add(app);
                }
            }
        });
    }

    public boolean isCtsApp(String packageName) {
        String[] str = packageName.split("\\.");
        if (str != null) {
            int val = 0;
            for (String temp : str) {
                if ("cts".equals(temp) || "gts".equals(temp)) {
                    val |= 1;
                } else if ("android".equals(temp) || "google".equals(temp)) {
                    val |= 2;
                }
                if (val == 3) {
                    return true;
                }
            }
        }
        return this.CTS_NAME.contains(packageName);
    }

    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo info = this.mPm.getApplicationInfo(packageName, 0);
            if (info != null) {
                return (info.flags & 1) != 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAppHasIcon(String packageName) {
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setAction("android.intent.action.MAIN");
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        List<ResolveInfo> resolveInfolist = this.mPm.queryIntentActivities(intent, 131072);
        if (resolveInfolist == null || resolveInfolist.size() <= 0) {
            return false;
        }
        return true;
    }

    public boolean isNeedNotifyAppStatus(int uid) {
        AurogonAppInfo app = getAurogonAppInfo(uid);
        return (app == null || app.isSystemApp || !app.hasIcon) ? false : true;
    }

    public void updateFgServicesList(int uid, int pid, boolean allow) {
        synchronized (this.mFgServiceAppList) {
            List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
            if (allow) {
                if (list == null) {
                    list = new ArrayList();
                    this.mFgServiceAppList.put(Integer.valueOf(uid), list);
                }
                if (!list.contains(Integer.valueOf(pid))) {
                    list.add(Integer.valueOf(pid));
                }
            } else if (list != null && list.contains(Integer.valueOf(pid))) {
                list.remove(Integer.valueOf(pid));
                if (list.size() == 0) {
                    this.mFgServiceAppList.remove(Integer.valueOf(uid));
                }
            }
        }
    }

    private String getPackageNameFromUid(int uid) {
        String packageName = null;
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packageName = packageManager.getNameForUid(uid);
        }
        if (packageName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packageName;
    }

    public boolean isDownloadApp(int uid, String pacakgeName) {
        if (AurogonDownloadFilter.getInstance().isDownloadApp(uid)) {
            return checkAppFgService(uid) || AurogonDownloadFilter.mImportantDownloadApp.contains(pacakgeName);
        }
        return false;
    }

    public void getConnectivityManager() {
        if (this.mConnMgr == null) {
            this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    public boolean isNeedRestictNetworkPolicy(int uid) {
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null && mMessageApp.contains(app.mPackageName)) {
            return true;
        }
        return false;
    }

    public void triggerLaunchModeAction(int uid) {
        AurogonAppInfo tempApp = getAurogonAppInfo(uid);
        if (tempApp == null || tempApp.hasIcon) {
            if (this.mHandler.hasMessages(106)) {
                this.mHandler.removeMessages(106);
                sendMeesage(106, -1, -1, null, 5000L);
            } else {
                sendMeesage(106, -1, -1, null, 3000L);
            }
            StringBuilder log = new StringBuilder();
            log.append("LM FZ [" + uid + "] uid = [");
            List<Integer> list = new ArrayList<>();
            updateAppsOnWindowsStatus();
            updateIMEAppStatus();
            synchronized (this.mImmobulusTargetList) {
                for (AurogonAppInfo app : this.mImmobulusTargetList) {
                    if (uid != app.mUid) {
                        if (this.mAllowList.contains(app.mPackageName)) {
                            if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is allow list app, skip check!");
                            }
                        } else if (this.mNoRestrictAppSet.contains(app.mPackageName)) {
                            if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is mNoRestrictAppSet list app, skip check!");
                            }
                        } else if (this.mGreezeService.checkRecentLuanchedApp(app.mUid)) {
                            if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is launched at same time with topp app, skip check!");
                            }
                        } else if (!this.mGreezeService.isAppRunning(app.mUid)) {
                            if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is not running app, skip check!");
                            }
                        } else {
                            List<Integer> list2 = this.mOnWindowsAppList;
                            if (list2 == null || !list2.contains(Integer.valueOf(app.mUid))) {
                                if (isWallPaperApp(app.mPackageName)) {
                                    if (GreezeManagerDebugConfig.DEBUG) {
                                        Slog.d(TAG, " uid = " + app.mUid + " is WallPaperApp app, skip check!");
                                    }
                                } else if (this.mGreezeService.isUidFrozen(app.mUid)) {
                                    if (GreezeManagerDebugConfig.DEBUG) {
                                        Slog.d(TAG, " uid = " + app.mUid + " is frozen app, skip check!");
                                    }
                                    this.mGreezeService.updateFrozenInfoForImmobulus(app.mUid, 16);
                                } else if (checkAppStatusForFreeze(app)) {
                                    if (GreezeManagerDebugConfig.mCgroupV1Flag) {
                                        list.add(Integer.valueOf(app.mUid));
                                    } else {
                                        boolean success = freezeActionForLaunchMode(app);
                                        if (success) {
                                            log.append(" " + app.mUid);
                                        }
                                    }
                                }
                            } else if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is show on screen, skip it!");
                            }
                        }
                    }
                }
            }
            if (GreezeManagerDebugConfig.mCgroupV1Flag && list.size() > 0) {
                int[] uids = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    uids[i] = list.get(i).intValue();
                }
                List<Integer> result = this.mGreezeService.freezeUids(uids, 0L, 16, "LAUNCH_MODE", false);
                Iterator<Integer> it = result.iterator();
                while (it.hasNext()) {
                    int temp = it.next().intValue();
                    log.append(" " + temp);
                    AurogonAppInfo app2 = getAurogonAppInfo(temp);
                    if (app2 != null && (app2.isSystemApp || !app2.hasIcon)) {
                        addLaunchModeQiutList(app2);
                    }
                }
            }
            log.append("]");
            this.mGreezeService.addToDumpHistory(log.toString());
        }
    }

    public void QuitLaunchModeAction(boolean timeout) {
        String reason;
        if (timeout) {
            reason = "LM timeout";
        } else if (this.mHandler.hasMessages(106)) {
            this.mHandler.removeMessages(106);
            reason = "LM finish";
        } else {
            return;
        }
        StringBuilder log = new StringBuilder();
        log.append(reason + " THAW uid = [");
        synchronized (this.mQuitLaunchModeList) {
            for (AurogonAppInfo app : this.mQuitLaunchModeList) {
                boolean success = this.mGreezeService.thawUid(app.mUid, 16, "LAUNCH_MODE");
                if (success) {
                    log.append(" " + app.mUid);
                } else {
                    this.mGreezeService.resetCgroupUidStatus(app.mUid);
                }
            }
            this.mQuitLaunchModeList.clear();
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
        this.mGreezeService.resetStatusForImmobulus(16);
        this.mLastPackageName = "";
        removeTempMutiWindowsApp();
    }

    public boolean freezeActionForLaunchMode(AurogonAppInfo app) {
        boolean isNeedCompact = false;
        if (app.isSystemApp || !app.hasIcon) {
            addLaunchModeQiutList(app);
        } else {
            isNeedCompact = true;
        }
        boolean success = this.mGreezeService.freezeAction(app.mUid, 16, "LAUNCH_MODE", isNeedCompact);
        return success;
    }

    public void triggerLaunchMode(String processName, int uid) {
        if (this.mLaunchModeEnabled) {
            Slog.d(TAG, "launch app processName = " + processName + " uid = " + uid);
            this.mLastPackageName = processName;
            if (AccessController.PACKAGE_CAMERA.equals(processName)) {
                this.mEnterIMCamera = true;
            }
            sendMeesage(MSG_LAUNCH_MODE_TRIGGER_ACTION, uid, -1, null, -1L);
        }
    }

    public void finishLaunchMode(String processName, int uid) {
        if (processName != this.mLastPackageName) {
            return;
        }
        sendMeesage(106, 1, -1, null, -1L);
    }

    public void finishLaunchMode() {
        if (this.mHandler.hasMessages(106)) {
            sendMeesage(106, 1, -1, null, -1L);
        }
    }

    public void triggerImmobulusMode(int uid, String mode) {
        if (this.mImmobulusModeEnabled) {
            if (!this.mGreezeService.mScreenOnOff && !"ExtremeM".equals(mode)) {
                if (GreezeManagerDebugConfig.DEBUG) {
                    Slog.d(TAG, " triggerImmobulusMode uid = " + uid + " mode = " + mode + " reject enter because of screen off!");
                }
            } else if (!this.mEnterImmobulusMode) {
                this.mEnterImmobulusMode = true;
                sendMeesage(101, uid, -1, mode, 3500L);
            }
        }
    }

    public void quitImmobulusMode() {
        if (this.mEnterImmobulusMode) {
            if (this.mHandler.hasMessages(101)) {
                this.mEnterImmobulusMode = false;
                this.mHandler.removeMessages(101);
            } else {
                this.mHandler.sendEmptyMessage(102);
            }
        }
    }

    public void triggerVideoMode(boolean allow, String packageName, int uid) {
        if (this.mImmobulusModeEnabled) {
            if (this.mGreezeService.mInFreeformSmallWinMode) {
                if (GreezeManagerDebugConfig.DEBUG) {
                    Slog.d(TAG, "SmallWindowMode skip videoMode!");
                }
            } else if (allow && this.mEnterIMVideoCloud) {
                triggerImmobulusMode(uid, "Video");
                this.mEnterIMVideo = true;
                this.mCurrentVideoPacageName = packageName;
            } else {
                if (!this.mEnterIMVideo || packageName == null || packageName.equals(this.mCurrentVideoPacageName)) {
                    return;
                }
                quitImmobulusMode();
                this.mEnterIMVideo = false;
                this.mCurrentVideoPacageName = "";
            }
        }
    }

    public void resetImmobulusModeStatus() {
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                app.level = 0;
            }
        }
    }

    public boolean isModeReason(String reason) {
        if (REASON_FREEZE.contains(reason)) {
            return true;
        }
        return false;
    }

    public void addTempMutiWindowsApp(int uid) {
        synchronized (this.mMutiWindowsApp) {
            this.mMutiWindowsApp.add(Integer.valueOf(uid));
        }
    }

    public void removeTempMutiWindowsApp() {
        synchronized (this.mMutiWindowsApp) {
            this.mMutiWindowsApp.clear();
        }
    }

    public boolean checkMutiWindowsApp(int uid) {
        boolean contains;
        synchronized (this.mMutiWindowsApp) {
            contains = this.mMutiWindowsApp.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    public boolean isWallPaperApp(String packageName) {
        if (this.mWallPaperPackageName.equals("")) {
            getWallpaperPackageName();
        }
        return packageName.equals(this.mWallPaperPackageName);
    }

    public boolean isVpnApp(int uid) {
        boolean z;
        synchronized (this.mVPNAppList) {
            z = this.mVPNAppList.contains(Integer.valueOf(uid));
        }
        return z;
    }

    public void getWallpaperPackageName() {
        try {
            WallpaperManager wm = (WallpaperManager) this.mContext.getSystemService("wallpaper");
            WallpaperInfo info = wm.getWallpaperInfo();
            if (info != null) {
                this.mWallPaperPackageName = info.getPackageName();
            } else {
                ComponentName componentName = WallpaperManager.getDefaultWallpaperComponent(this.mContext);
                if (componentName != null) {
                    this.mWallPaperPackageName = componentName.getPackageName();
                }
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ImmobulusHandler extends Handler {
        private ImmobulusHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 101:
                    int uid = msg.arg1;
                    String reason = (String) msg.obj;
                    AurogonImmobulusMode.this.TriggerImmobulusModeAction(uid, reason);
                    return;
                case 102:
                    AurogonImmobulusMode.this.QuitImmobulusModeAction();
                    return;
                case 103:
                    AurogonAppInfo app = (AurogonAppInfo) msg.obj;
                    AurogonImmobulusMode.this.checkAppForImmobulusMode(app);
                    return;
                case AurogonImmobulusMode.MSG_REMOVE_ALL_MESSAGE /* 104 */:
                    AurogonImmobulusMode.this.removeAllMsg();
                    return;
                case AurogonImmobulusMode.MSG_LAUNCH_MODE_TRIGGER_ACTION /* 105 */:
                    int flag = msg.arg1;
                    AurogonImmobulusMode.this.triggerLaunchModeAction(flag);
                    return;
                case 106:
                    int flag2 = msg.arg1;
                    if (flag2 != 1) {
                        AurogonImmobulusMode.this.QuitLaunchModeAction(true);
                        return;
                    } else {
                        AurogonImmobulusMode.this.QuitLaunchModeAction(false);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    public void removeAllMsg() {
        this.mHandler.removeMessages(101);
        this.mHandler.removeMessages(102);
        this.mHandler.removeMessages(103);
        this.mHandler.removeMessages(MSG_REMOVE_ALL_MESSAGE);
    }

    /* loaded from: classes.dex */
    public class ImmobulusBroadcastReceiver extends BroadcastReceiver {
        public ImmobulusBroadcastReceiver() {
            IntentFilter intent = new IntentFilter();
            intent.addAction(AurogonImmobulusMode.IMMOBULUS_GAME_CONTROLLER);
            intent.addAction("android.intent.action.WALLPAPER_CHANGED");
            intent.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            intent.addAction(AurogonImmobulusMode.BROADCAST_SATELLITE);
            AurogonImmobulusMode.this.mContext.registerReceiver(this, intent);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AurogonImmobulusMode.IMMOBULUS_GAME_CONTROLLER.equals(action)) {
                if (AurogonImmobulusMode.this.mImmobulusModeEnabled) {
                    int intExtra = intent.getIntExtra(AurogonImmobulusMode.IMMOBULUS_GAME_KEY_STATUS, 0);
                    String[] stringArrayExtra = intent.getStringArrayExtra(AurogonImmobulusMode.IMMOBULUS_GAME_KEY_ALLOW_LIST);
                    if (stringArrayExtra == null) {
                        return;
                    }
                    for (String str : stringArrayExtra) {
                        if (!AurogonImmobulusMode.this.mImmobulusAllowList.contains(str)) {
                            AurogonImmobulusMode.this.mImmobulusAllowList.add(str);
                        }
                    }
                    if (intExtra == 1) {
                        if (!AurogonImmobulusMode.this.mEnterImmobulusMode) {
                            AurogonImmobulusMode aurogonImmobulusMode = AurogonImmobulusMode.this;
                            aurogonImmobulusMode.triggerImmobulusMode(aurogonImmobulusMode.mGreezeService.mTopAppUid, "Game");
                            return;
                        }
                        return;
                    }
                    if (intExtra == 2 && AurogonImmobulusMode.this.mEnterImmobulusMode) {
                        AurogonImmobulusMode.this.quitImmobulusMode();
                        return;
                    }
                    return;
                }
                return;
            }
            if ("android.intent.action.WALLPAPER_CHANGED".equals(action)) {
                AurogonImmobulusMode.this.getWallpaperPackageName();
                return;
            }
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                if (AurogonImmobulusMode.this.mConnMgr != null) {
                    NetworkInfo networkInfo = AurogonImmobulusMode.this.mConnMgr.getNetworkInfo(17);
                    AurogonImmobulusMode.this.mVpnConnect = networkInfo != null ? networkInfo.isConnected() : false;
                    if (AurogonImmobulusMode.this.mVpnConnect) {
                        AurogonImmobulusMode.this.updateVpnStatus(networkInfo);
                    }
                }
                AurogonImmobulusMode.this.handleMessageAppStatus();
                return;
            }
            if (AurogonImmobulusMode.BROADCAST_SATELLITE.equals(action)) {
                boolean booleanExtra = intent.getBooleanExtra("is_enable", false);
                Slog.d("Aurogon", " BROADCAST_SATELLITE state = " + booleanExtra);
                if (booleanExtra) {
                    AurogonImmobulusMode.this.mExtremeMode = true;
                    AurogonImmobulusMode.this.triggerImmobulusMode(1000, "ExtremeM_SATELLITE");
                } else if (AurogonImmobulusMode.this.mExtremeMode) {
                    AurogonImmobulusMode.this.mExtremeMode = false;
                    AurogonImmobulusMode.this.quitImmobulusMode();
                    AurogonImmobulusMode.this.mGreezeService.thawuidsAll("SATELLITE");
                    AurogonImmobulusMode.this.simulateNetChange();
                }
            }
        }
    }

    public void simulateNetChange() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.5
            @Override // java.lang.Runnable
            public void run() {
                Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
                intent.setPackage("com.tencent.mm");
                AurogonImmobulusMode.this.mContext.sendBroadcast(intent);
            }
        });
    }

    public void updateVpnStatus(NetworkInfo networkInfo) {
        Network[] net = this.mConnMgr.getAllNetworks();
        this.mVPNAppList.clear();
        for (Network temp : net) {
            NetworkCapabilities nc = this.mConnMgr.getNetworkCapabilities(temp);
            if (nc != null) {
                int[] uids = nc.getAdministratorUids();
                synchronized (this.mVPNAppList) {
                    for (int uid : uids) {
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.d(TAG, " vpn uid = " + uid);
                        }
                        this.mVPNAppList.add(Integer.valueOf(uid));
                    }
                }
            }
        }
    }

    public void handleMessageAppStatus() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.6
            @Override // java.lang.Runnable
            public void run() {
                for (String name : AurogonImmobulusMode.mMessageApp) {
                    AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(name);
                    if (app != null && AurogonImmobulusMode.this.mGreezeService.isUidFrozen(app.mUid)) {
                        AurogonImmobulusMode.this.mGreezeService.thawUid(app.mUid, 1000, "Net Change");
                    }
                }
            }
        });
    }

    public boolean isAllowWakeUpList(String packageName) {
        if (this.mAllowWakeUpPackageNameList.contains(packageName)) {
            Slog.d("Aurogon", " packageName = " + packageName + " isAllowWakeUpList ");
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudAllowList() {
        if (!GreezeManagerDebugConfig.mPowerMilletEnable || this.mCtsModeOn || !CN_MODEL) {
            this.mLaunchModeEnabled = false;
            this.mImmobulusModeEnabled = false;
            if (!CN_MODEL) {
                this.mGreezeService.setBroadcastCtrl(false);
                return;
            }
            return;
        }
        String str = Settings.Secure.getString(this.mContext.getContentResolver(), IMMOBULUS_SWITCH_SECURE_SETTING);
        if (str != null) {
            Slog.d(TAG, "clound setting str = " + str);
            String[] temp = str.split(d.h);
            if (temp.length < 3) {
                return;
            }
            if (MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE.equals(temp[0])) {
                int enable = Integer.parseInt(temp[1]);
                if ((enable & 8) != 0) {
                    if (!this.mGreezeService.DISABLE_IMMOB_MODE_DEVICE.contains(Build.DEVICE)) {
                        this.mImmobulusModeEnabled = true;
                    }
                } else {
                    this.mImmobulusModeEnabled = false;
                }
                if ((enable & 16) != 0) {
                    this.mLaunchModeEnabled = true;
                } else {
                    this.mLaunchModeEnabled = false;
                }
            }
            if ("allowlist".equals(temp[2])) {
                synchronized (this.mAllowList) {
                    this.mAllowList.clear();
                    this.mAllowList.addAll(this.mLocalAllowList);
                    this.mCloudAllowList.clear();
                    int flag = 9999;
                    int i = 3;
                    while (true) {
                        if (i >= temp.length) {
                            break;
                        }
                        if ("wakeuplist".equals(temp[i])) {
                            flag = i;
                            break;
                        } else {
                            this.mCloudAllowList.add(temp[i]);
                            i++;
                        }
                    }
                    this.mAllowList.addAll(this.mCloudAllowList);
                    if (flag < temp.length - 1) {
                        this.mAllowWakeUpPackageNameList.clear();
                    }
                    for (int j = flag + 1; j < temp.length; j++) {
                        this.mAllowWakeUpPackageNameList.add(temp[j]);
                    }
                }
            }
        }
        parseAurogonEnable();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateIMEAppStatus() {
        String curImeId = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        if (curImeId != null) {
            String[] str = curImeId.split("/");
            if (str.length > 1 && !this.mCurrentIMEPacageName.equals(str[0])) {
                this.mCurrentIMEPacageName = str[0];
                Slog.d(TAG, " updateIMEAppStatus mCurrentIMEPacageName = " + this.mCurrentIMEPacageName);
                int uid = this.mGreezeService.getUidByPackageName(this.mCurrentIMEPacageName);
                this.mCurrentIMEUid = uid;
            }
        }
    }

    public boolean isIMEApp(int uid) {
        return uid == this.mCurrentIMEUid;
    }

    public void updateWidgetPackages() {
        String widgetPackages = Settings.Secure.getString(this.mContext.getContentResolver(), MiuiAppWidgetServiceImpl.ENABLED_WIDGETS);
        if (widgetPackages != null) {
            Slog.d(TAG, " widgetPackages = " + widgetPackages);
            String[] strs = widgetPackages.split(":");
            if (strs.length > 0) {
                synchronized (this.mCurrentWidgetPackages) {
                    this.mCurrentWidgetPackages.clear();
                    for (String str : strs) {
                        this.mCurrentWidgetPackages.add(str);
                    }
                }
            }
        }
    }

    public boolean isWidgetApp(int uid) {
        String packageName = this.mGreezeService.getPackageNameFromUid(uid);
        return isWidgetApp(packageName);
    }

    public boolean isWidgetApp(String pacakgeName) {
        boolean contains;
        synchronized (this.mCurrentWidgetPackages) {
            contains = this.mCurrentWidgetPackages.contains(pacakgeName);
        }
        return contains;
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (uri.equals(Settings.Global.getUriFor(AurogonImmobulusMode.BLUETOOTH_GLOBAL_SETTING))) {
                String str = Settings.Global.getString(AurogonImmobulusMode.this.mContext.getContentResolver(), AurogonImmobulusMode.BLUETOOTH_GLOBAL_SETTING);
                if (str != null) {
                    AurogonImmobulusMode.this.mBluetoothUsingList.clear();
                    String[] names = str.split("#");
                    for (String name : names) {
                        AurogonImmobulusMode.this.mBluetoothUsingList.add(name);
                        AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(name);
                        if (app != null && AurogonImmobulusMode.this.mGreezeService.isUidFrozen(app.mUid)) {
                            AurogonImmobulusMode.this.unFreezeActionForImmobulus(app, "BT connect");
                        }
                    }
                    return;
                }
                return;
            }
            if (uri.equals(Settings.Secure.getUriFor(AurogonImmobulusMode.IMMOBULUS_SWITCH_SECURE_SETTING))) {
                AurogonImmobulusMode.this.updateCloudAllowList();
                return;
            }
            if (uri.equals(Settings.Secure.getUriFor("default_input_method"))) {
                AurogonImmobulusMode.this.updateIMEAppStatus();
                return;
            }
            if (!uri.equals(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION))) {
                if (uri.equals(Settings.Secure.getUriFor(AurogonImmobulusMode.PC_SECURITY_CENTER_EXTREME_MODE))) {
                    if (AurogonImmobulusMode.this.mImmobulusModeEnabled) {
                        boolean temp = Settings.Secure.getInt(AurogonImmobulusMode.this.mContext.getContentResolver(), AurogonImmobulusMode.PC_SECURITY_CENTER_EXTREME_MODE, -1) == 1;
                        if (!temp) {
                            Slog.d(AurogonImmobulusMode.TAG, " quit Extreme Mode mode!");
                            AurogonImmobulusMode.this.mExtremeMode = false;
                            AurogonImmobulusMode.this.quitImmobulusMode();
                            return;
                        } else {
                            if (AurogonImmobulusMode.this.mExtremeMode || !AurogonImmobulusMode.this.mExtremeModeCloud) {
                                Slog.d(AurogonImmobulusMode.TAG, "Extreme Mode mode has enter!");
                                return;
                            }
                            Slog.d(AurogonImmobulusMode.TAG, " enter Extreme Mode mode!");
                            AurogonImmobulusMode.this.mExtremeMode = true;
                            AurogonImmobulusMode.this.triggerImmobulusMode(1000, "ExtremeM");
                            return;
                        }
                    }
                    return;
                }
                if (uri.equals(Settings.Global.getUriFor(AurogonImmobulusMode.AUROGON_ENABLE))) {
                    AurogonImmobulusMode.this.parseAurogonEnable();
                    return;
                }
                if (uri.equals(Settings.System.getUriFor(AurogonImmobulusMode.KEY_NO_RESTRICT_APP))) {
                    if (AurogonImmobulusMode.this.mLaunchModeEnabled || AurogonImmobulusMode.this.mImmobulusModeEnabled) {
                        AurogonImmobulusMode.this.mNoRestrictAppSet.clear();
                        AurogonImmobulusMode.this.getNoRestrictApps();
                        return;
                    }
                    return;
                }
                if (uri.equals(Settings.System.getUriFor(AurogonImmobulusMode.SATELLITE_STATE))) {
                    boolean state = Settings.System.getInt(AurogonImmobulusMode.this.mContext.getContentResolver(), AurogonImmobulusMode.SATELLITE_STATE, -1) == 1;
                    Slog.d("Aurogon", " SATELLITE_STATE state = " + state);
                    if (state) {
                        AurogonImmobulusMode.this.mExtremeMode = true;
                        AurogonImmobulusMode.this.triggerImmobulusMode(1000, "ExtremeM_SATELLITE");
                        return;
                    } else {
                        if (AurogonImmobulusMode.this.mExtremeMode) {
                            AurogonImmobulusMode.this.mExtremeMode = false;
                            AurogonImmobulusMode.this.quitImmobulusMode();
                            AurogonImmobulusMode.this.mGreezeService.thawuidsAll("SATELLITE");
                            AurogonImmobulusMode.this.simulateNetChange();
                            return;
                        }
                        return;
                    }
                }
                if (uri.equals(Settings.Secure.getUriFor(MiuiAppWidgetServiceImpl.ENABLED_WIDGETS))) {
                    AurogonImmobulusMode.this.updateWidgetPackages();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getNoRestrictApps() {
        String str = Settings.System.getString(this.mContext.getContentResolver(), KEY_NO_RESTRICT_APP);
        Slog.d(TAG, "getNoRestrictApps result=" + str);
        if (str == null || str.length() == 0) {
            this.mNoRestrictAppSet.clear();
            return;
        }
        String[] apps = str.split(",");
        for (String str2 : apps) {
            String app = str2.trim();
            if (app.length() != 0 && !this.mNoRestrictAppSet.contains(app)) {
                this.mNoRestrictAppSet.add(app);
            }
        }
        Slog.d(TAG, "mNoRestrictAppSet=" + this.mNoRestrictAppSet.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseAurogonEnable() {
        String str = Settings.Global.getString(this.mContext.getContentResolver(), AUROGON_ENABLE);
        if (str == null || str.isEmpty()) {
            return;
        }
        String[] cfs = str.split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
        for (String cf : cfs) {
            if (cf.contains("extrememode:")) {
                this.mExtremeModeCloud = cf.contains("true");
            } else if (cf.contains("enterimvideo:")) {
                this.mEnterIMVideoCloud = cf.contains("true");
            } else if (cf.contains("broadcastctrl:")) {
                if (this.mGreezeService == null) {
                    return;
                }
                String[] ls = cf.split(d.h);
                if (ls.length > 0) {
                    this.mGreezeService.setBroadcastCtrl(ls[0].contains("true"));
                }
                for (String ua : ls) {
                    if (ua.contains("/")) {
                        String[] pkgAction = ua.split("/");
                        if (pkgAction.length > 1) {
                            this.mGreezeService.getBroadcastConfig().put(pkgAction[0], pkgAction[1]);
                        }
                    }
                }
            } else if (cf.contains("doubleapp:")) {
                this.mDoubleAppCtrlCloud = !cf.contains("false");
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AurogonImmobulusMode : ");
        pw.println("mImmobulusModeEnabled : " + this.mImmobulusModeEnabled);
        if (GreezeManagerDebugConfig.DEBUG) {
            pw.println("ExtremeMode enabled : " + this.mExtremeMode + " cloud:" + this.mExtremeModeCloud);
            pw.println("VideoMode enabled : " + this.mEnterIMVideo + " cloud:" + this.mEnterIMVideoCloud);
            pw.println("Camera enabled : " + this.mEnterIMCamera + " doubleapp:" + this.mDoubleAppCtrlCloud);
        }
        pw.println("AurogonImmobulusMode LM : ");
        pw.println("LM enabled : " + this.mLaunchModeEnabled);
        pw.println("LM mCurrentIMEPacageName : " + this.mCurrentIMEPacageName);
        if (GreezeManagerDebugConfig.DEBUG) {
            pw.println("mImmobulusAllowList : ");
            for (String str : this.mImmobulusAllowList) {
                pw.println(" " + str);
            }
            pw.println("mImmobulusTargetList : ");
            synchronized (this.mImmobulusTargetList) {
                for (AurogonAppInfo app : this.mImmobulusTargetList) {
                    pw.println(" " + app.toString());
                }
            }
            synchronized (this.mFgServiceAppList) {
                pw.println("mFgServiceAppList : ");
                Iterator<Integer> it = this.mFgServiceAppList.keySet().iterator();
                while (it.hasNext()) {
                    int uid = it.next().intValue();
                    pw.println(" uid = " + uid);
                }
            }
            pw.println("mAllowList : ");
            for (String str1 : this.mAllowList) {
                pw.println(" " + str1);
            }
            pw.println("mWallPaperPackageName");
            pw.println(" " + this.mWallPaperPackageName);
            pw.println("mAllowWakeUpPackageNameList");
            for (String str2 : this.mAllowWakeUpPackageNameList) {
                pw.println("--" + str2 + "--");
            }
        }
        if (args.length == 0) {
            return;
        }
        if ("Immobulus".equals(args[0])) {
            if (MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE.equals(args[1])) {
                this.mImmobulusModeEnabled = true;
                this.mHandler.sendEmptyMessage(101);
            } else if ("disabled".equals(args[1])) {
                this.mImmobulusModeEnabled = false;
                this.mHandler.sendEmptyMessage(102);
            }
        }
        if (args.length == 3) {
            if ("LM".equals(args[0])) {
                if ("add".equals(args[1])) {
                    this.mAllowList.add(args[2]);
                    return;
                }
                if ("remove".equals(args[1])) {
                    if (this.mAllowList.contains(args[2])) {
                        this.mAllowList.remove(args[2]);
                        return;
                    }
                    return;
                } else {
                    if ("set".equals(args[1])) {
                        if ("true".equals(args[2])) {
                            this.mLaunchModeEnabled = true;
                            return;
                        } else {
                            if ("false".equals(args[2])) {
                                this.mLaunchModeEnabled = false;
                                return;
                            }
                            return;
                        }
                    }
                    if (DumpSysInfoUtil.WINDOW.equals(args[1])) {
                        updateAppsOnWindowsStatus();
                        return;
                    }
                    return;
                }
            }
            if ("IM".equals(args[0]) && "ExtremeMode".equals(args[1])) {
                if ("true".equals(args[2])) {
                    this.mExtremeMode = true;
                    triggerImmobulusMode(1000, "ExtremeM");
                } else if ("false".equals(args[2])) {
                    this.mExtremeMode = false;
                    quitImmobulusMode();
                    this.mGreezeService.thawAll("Sleep Mode");
                }
            }
        }
    }
}
