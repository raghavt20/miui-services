package com.android.server.app;

import android.app.ActivityManager;
import android.compat.Compatibility;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.app.GmsLimitLogger;
import com.android.server.compat.PlatformCompat;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.wm.ActivityTaskManagerServiceStub;
import com.android.server.wm.MiuiEmbeddingWindowServiceStub;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.input.edgesuppression.EdgeSuppressionFactory;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import miui.os.Build;
import miui.util.FeatureParser;
import miui.util.MiuiMultiDisplayTypeInfo;
import org.json.JSONArray;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.app.GameManagerServiceStub$$")
/* loaded from: classes.dex */
public class GameManagerServiceStubImpl extends GameManagerServiceStub {
    private static final String CLOUD_ALL_DATA_CHANGE_URI = "content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify";
    private static final String DEBUG_DOWNSCALE_POWER_SAVE = "persist.sys.downscale_power_save";
    private static final String DEBUG_PROP_KEY = "persist.sys.miui_downscale";
    private static final String DOWNSCALE_APP_SETTINGS_FILE_PATH = "system/etc/DownscaleAppSettings.json";
    private static final String DOWNSCALE_DISABLE = "disable";
    private static final String DOWNSCALE_ENABLE = "enable";
    private static final boolean FOLD_DEVICE = MiuiMultiDisplayTypeInfo.isFoldDeviceInside();
    private static final String KEY_POWER_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    public static final int MIUI_PAD_SCREEN_COMPAT_VERSION = 1;
    public static final String MIUI_RESOLUTION = "persist.sys.miui_resolution";
    private static final String MIUI_SCREEN_COMPAT = "miui_screen_compat";
    public static final int MIUI_SCREEN_COMPAT_VERSION = 3;
    static final int MIUI_SCREEN_COMPAT_WIDTH = 1080;
    private static final String MODULE_NAME_DOWNSCALE = "tdownscale";
    public static final int MSG_CLOUD_DATA_CHANGE = 111114;
    public static final int MSG_DOWNSCALE_APP_DIED = 111111;
    public static final int MSG_EXTREME_MODE = 111116;
    public static final int MSG_INTELLIGENT_POWERSAVE = 111115;
    public static final int MSG_PROCESS_POWERSAVE_ACTION = 111112;
    public static final int MSG_REBOOT_COMPLETED_ACTION = 111113;
    private static final String PC_SECURITY_CENTER_EXTREME_MODE = "pc_security_center_extreme_mode_enter";
    public static final float SCALE_MIN = 0.6f;
    static final String TAG = "GameManagerServiceStub";
    private Context mContext;
    private DownscaleCloudData mDownscaleCloudData;
    private InnerHandler mHandler;
    private GMSObserver mObserver;
    private volatile boolean mPowerSaving;
    private ActivityManagerService mService;
    WindowManagerService mWindowManager;
    public final ServiceThread mHandlerThread = new ServiceThread(TAG, -2, false);
    private HashMap<String, Boolean> mDownscaleApps = new HashMap<>();
    private String mProEnable = "enable";
    private boolean isDebugPowerSave = false;
    private boolean intelligentPowerSavingEnable = false;
    private int currentWidth = MIUI_SCREEN_COMPAT_WIDTH;
    private HashSet<String> mSizeCompatApps = new HashSet<>();
    private HashSet<String> mShellCmdDownscalePackageNames = new HashSet<>();
    private boolean mExtremeModeEnable = false;
    private boolean fromCloud = false;
    private String cloudDataStr = "";
    private HashMap<String, AppItem> mAppStates = new HashMap<>();
    private GmsLimitLogger limitLogger = new GmsLimitLogger(200);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GameManagerServiceStubImpl> {

        /* compiled from: GameManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GameManagerServiceStubImpl INSTANCE = new GameManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GameManagerServiceStubImpl m708provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GameManagerServiceStubImpl m707provideNewInstance() {
            return new GameManagerServiceStubImpl();
        }
    }

    public void registerDownscaleObserver(Context context) {
        Slog.d(TAG, "downscale register");
        this.mContext = context;
        if (this.mHandler == null) {
            if (!this.mHandlerThread.isAlive()) {
                this.mHandlerThread.start();
            }
            this.mHandler = new InnerHandler(this.mHandlerThread.getLooper());
        }
        this.mObserver = new GMSObserver(this.mHandler);
        this.mWindowManager = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        this.mPowerSaving = Settings.System.getInt(context.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1;
        this.isDebugPowerSave = Settings.Global.getInt(context.getContentResolver(), DEBUG_DOWNSCALE_POWER_SAVE, 0) == 1;
        this.intelligentPowerSavingEnable = Settings.System.getInt(context.getContentResolver(), MIUI_SCREEN_COMPAT, !Build.IS_TABLET ? 1 : 0) == 1;
        String string = Settings.Global.getString(context.getContentResolver(), DEBUG_PROP_KEY);
        if (!TextUtils.isEmpty(string) && TextUtils.equals(DOWNSCALE_DISABLE, string)) {
            this.mProEnable = string;
        }
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(DEBUG_PROP_KEY), false, this.mObserver);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_POWER_MODE_OPEN), true, this.mObserver);
        context.registerReceiverForAllUsers(new GMSBroadcastReceiver(), new IntentFilter("android.intent.action.BOOT_COMPLETED"), null, null);
        context.getContentResolver().registerContentObserver(Uri.parse(CLOUD_ALL_DATA_CHANGE_URI), false, this.mObserver);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(MIUI_SCREEN_COMPAT), false, this.mObserver);
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(PC_SECURITY_CENTER_EXTREME_MODE), false, this.mObserver);
        if (this.mDownscaleCloudData == null) {
            this.mDownscaleCloudData = new DownscaleCloudData(initLocalDownscaleAppList());
            this.fromCloud = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processCloudData() {
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), MODULE_NAME_DOWNSCALE, MODULE_NAME_DOWNSCALE, (String) null, false);
        if (cloudData == null || cloudData.json() == null) {
            return;
        }
        JSONObject jsonAll = cloudData.json();
        this.cloudDataStr = jsonAll.toString();
        long version = -1;
        if (jsonAll != null) {
            version = jsonAll.optLong(AmapExtraCommand.VERSION_KEY);
        }
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData == null) {
            DownscaleCloudData downscaleCloudData2 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData2;
            toggleDownscale(null);
            this.fromCloud = true;
            return;
        }
        if (downscaleCloudData.version != version && version != -1) {
            DownscaleCloudData downscaleCloudData3 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData3;
            toggleDownscale(null);
            this.fromCloud = true;
        }
    }

    private void updateLocalDataBootCompleted() {
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), MODULE_NAME_DOWNSCALE, MODULE_NAME_DOWNSCALE, (String) null, false);
        if (cloudData == null || cloudData.json() == null) {
            return;
        }
        JSONObject jsonAll = cloudData.json();
        this.cloudDataStr = jsonAll.toString();
        long version = -1;
        if (jsonAll != null) {
            version = jsonAll.optLong(AmapExtraCommand.VERSION_KEY);
        }
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData == null) {
            DownscaleCloudData downscaleCloudData2 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData2;
            Slog.v(TAG, "BOOT_COMPLETED updateLocalDataBootCompleted use cloudData");
            this.fromCloud = true;
            return;
        }
        if (downscaleCloudData.version != version && version != -1) {
            DownscaleCloudData downscaleCloudData3 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData3;
            Slog.v(TAG, "BOOT_COMPLETED updateLocalDataBootCompleted use cloudData different version");
            this.fromCloud = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processBootCompletedAction() {
        boolean powerModeOpen = Settings.System.getInt(this.mContext.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1;
        this.mExtremeModeEnable = Settings.Secure.getInt(this.mContext.getContentResolver(), PC_SECURITY_CENTER_EXTREME_MODE, 0) == 1;
        Slog.v(TAG, "ACTION_BOOT_COMPLETED --- powerModeOpen= " + powerModeOpen + " mPowerSaving = " + this.mPowerSaving);
        updateLocalDataBootCompleted();
        if (powerModeOpen || this.mExtremeModeEnable) {
            this.mPowerSaving = true;
        }
        this.limitLogger.log(new GmsLimitLogger.StringEvent("---rebootCompleted---"));
        toggleDownscale(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processPowerSaveAction() {
        this.mPowerSaving = Settings.System.getInt(this.mContext.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1;
        if (DOWNSCALE_DISABLE.equals(this.mProEnable)) {
            return;
        }
        toggleDownscale(null);
    }

    public void toggleDownscale(String packageNameParam) {
        if (!GmsUtil.checkValidApp(packageNameParam, this.mDownscaleCloudData)) {
            return;
        }
        executeDownscale(packageNameParam, true);
    }

    public void downscaleForSwitchResolution() {
        toggleDownscale(null);
    }

    public void downscaleWithPackageNameAndRatio(String packageName, String ratio) {
    }

    public void dynamicRefresh(String ratio, String packageName) {
    }

    public void addSizeCompatApps(String packageName) {
        if (FOLD_DEVICE && GmsUtil.checkValidApp(packageName, this.mDownscaleCloudData)) {
            this.mSizeCompatApps.add(packageName);
        }
    }

    public void toggleDownscaleForStopedApp(String packageName) {
        if (!FOLD_DEVICE || !GmsUtil.checkValidApp(packageName, this.mDownscaleCloudData) || runningApp(packageName)) {
            return;
        }
        executeDownscale(packageName, true);
    }

    public void shellCmd(String ratio, String packageName) {
        Set<Long> disabled;
        final long changeId = getCompatChangeId(ratio);
        Set<Long> enabled = new ArraySet<>();
        if (changeId == 0) {
            disabled = this.DOWNSCALE_CHANGE_IDS;
        } else {
            enabled.add(168419799L);
            enabled.add(Long.valueOf(changeId));
            disabled = (Set) this.DOWNSCALE_CHANGE_IDS.stream().filter(new Predicate() { // from class: com.android.server.app.GameManagerServiceStubImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return GameManagerServiceStubImpl.lambda$shellCmd$0(changeId, (Long) obj);
                }
            }).collect(Collectors.toSet());
        }
        if (TextUtils.equals(ratio, DOWNSCALE_DISABLE)) {
            if (this.mShellCmdDownscalePackageNames.contains(packageName)) {
                this.mShellCmdDownscalePackageNames.remove(packageName);
            }
        } else if (this.mShellCmdDownscalePackageNames.size() <= 50) {
            this.mShellCmdDownscalePackageNames.add(packageName);
        } else {
            Slog.v(TAG, "commands enable no more than 50");
            return;
        }
        PlatformCompat platformCompat = ServiceManager.getService("platform_compat");
        CompatibilityChangeConfig overrides = new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
        platformCompat.setOverrides(overrides, packageName);
        String logStr = "shellCmd--- " + packageName + "," + ratio;
        Slog.v(TAG, logStr);
        this.limitLogger.log(new GmsLimitLogger.StringEvent(logStr));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$shellCmd$0(long changeId, Long it) {
        return (it.longValue() == 168419799 || it.longValue() == changeId) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void executeDownscale(String packageNameParam, boolean ignoreRunningApps) {
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData != null) {
            appsDownscale(packageNameParam, ignoreRunningApps);
            return;
        }
        if (downscaleCloudData == null) {
            JSONObject jsonAll = initLocalDownscaleAppList();
            this.mDownscaleCloudData = new DownscaleCloudData(jsonAll);
            this.fromCloud = false;
        }
        appsDownscale(packageNameParam, ignoreRunningApps);
    }

    private void appsDownscale(String packageNameParam, boolean ignoreRunningApps) {
        AppItem app;
        HashMap<String, AppItem> apps = this.mDownscaleCloudData.apps;
        String currentResolution = SystemProperties.get(MIUI_RESOLUTION);
        if (!TextUtils.isEmpty(currentResolution)) {
            String[] resolutionArr = currentResolution.split(",");
            if (resolutionArr != null && resolutionArr.length > 0) {
                try {
                    this.currentWidth = Integer.parseInt(resolutionArr[0]);
                } catch (Exception e) {
                }
            }
        } else {
            Point point = new Point();
            this.mWindowManager.getBaseDisplaySize(0, point);
            int baseDisplayWidth = point.x;
            Slog.v(TAG, "baseDisplayWidth = " + baseDisplayWidth);
            if (baseDisplayWidth > 0) {
                this.currentWidth = baseDisplayWidth;
            }
        }
        if (TextUtils.isEmpty(packageNameParam)) {
            if (apps != null) {
                for (Map.Entry<String, AppItem> appItemEntry : apps.entrySet()) {
                    downscaleApp(appItemEntry.getKey(), appItemEntry.getValue(), ignoreRunningApps);
                }
                return;
            }
            return;
        }
        if (apps != null && (app = apps.get(packageNameParam)) != null) {
            downscaleApp(app.packageName, app, ignoreRunningApps);
        }
    }

    private int getTargetWidth(AppItem appItem) {
        int mode;
        int systemVersion;
        if (!"enable".equals(this.mProEnable) || !this.mDownscaleCloudData.enable || (mode = appItem.mode) == 0 || (systemVersion = appItem.systemVersion) == 0) {
            return -1;
        }
        int appVersion = appItem.appVersion;
        if (appVersion > 3) {
            return -1;
        }
        boolean normalEnable = (mode & 1) != 0;
        boolean saveBatteryEnable = (mode & 2) != 0;
        boolean containDevice = this.isDebugPowerSave || GmsUtil.isContainDevice(this.mDownscaleCloudData.devices);
        boolean preVersionEnable = (systemVersion & 1) != 0;
        boolean devVersionEnable = (systemVersion & 2) != 0;
        boolean stableVersionEnable = (systemVersion & 4) != 0;
        if ((!preVersionEnable || !Build.IS_PRE_VERSION) && ((!devVersionEnable || !Build.IS_DEV_VERSION) && (!stableVersionEnable || !Build.IS_STABLE_VERSION))) {
            return -1;
        }
        if (this.mExtremeModeEnable) {
            return this.mDownscaleCloudData.scenes.saveBattery;
        }
        if (this.mPowerSaving && saveBatteryEnable && containDevice) {
            if (this.currentWidth == 720) {
                return 540;
            }
            return this.mDownscaleCloudData.scenes.saveBattery;
        }
        int[] resolutionArray = FeatureParser.getIntArray("screen_resolution_supported");
        boolean screenCompatSupported = FeatureParser.getBoolean("screen_compat_supported", false);
        if (!Build.IS_TABLET && this.intelligentPowerSavingEnable && normalEnable && this.currentWidth > MIUI_SCREEN_COMPAT_WIDTH && (resolutionArray != null || screenCompatSupported)) {
            return this.mDownscaleCloudData.scenes.normal;
        }
        if (Build.IS_TABLET && this.intelligentPowerSavingEnable && normalEnable) {
            return this.mDownscaleCloudData.scenes.normal;
        }
        return -1;
    }

    private void downscaleApp(String packageName, AppItem appItem, boolean ignoreRunningApps) {
        Set<Long> disabled;
        if ((this.isDebugPowerSave || GmsUtil.needDownscale(packageName, this.mDownscaleCloudData, this.currentWidth)) && !TextUtils.isEmpty(packageName) && appItem != null) {
            if ("enable".equals(this.mProEnable) && ignoreRunningApps && runningApp(packageName)) {
                Slog.v(TAG, "packageName = " + packageName + "is running do not downscale");
                return;
            }
            String ratioResult = DOWNSCALE_DISABLE;
            if (!Build.IS_TABLET) {
                int targetWidth = getTargetWidth(appItem);
                if (targetWidth != -1) {
                    ratioResult = GmsUtil.calcuRatio(targetWidth, this.currentWidth);
                }
            } else if ("enable".equals(this.mProEnable) && this.mDownscaleCloudData.enable) {
                ratioResult = GmsUtil.getTargetRatioForPad(appItem, this.mPowerSaving, this.mDownscaleCloudData, this.isDebugPowerSave);
            }
            if (FOLD_DEVICE && (ActivityTaskManagerServiceStub.get().getAspectRatio(packageName) > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || MiuiEmbeddingWindowServiceStub.get().isEmbeddingEnabledForPackage(packageName))) {
                ratioResult = DOWNSCALE_DISABLE;
            }
            float currentScale = WindowManagerServiceStub.get().getCompatScale(packageName, 0);
            Slog.v(TAG, "downscale currentScale = " + currentScale);
            if (currentScale != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                float currentScale2 = 1.0f / currentScale;
                if (ratioResult.equals(String.valueOf(currentScale2)) || (TextUtils.equals(ratioResult, DOWNSCALE_DISABLE) && currentScale2 == 1.0f)) {
                    Slog.v(TAG, "downscale " + packageName + " not change !!");
                    appItem.ratio = ratioResult;
                    this.mAppStates.put(packageName, appItem);
                    this.limitLogger.log(new GmsLimitLogger.StringEvent(packageName + "," + ratioResult));
                    return;
                }
            }
            final long changeId = getCompatChangeId(ratioResult);
            Set<Long> enabled = new ArraySet<>();
            if (changeId == 0) {
                disabled = this.DOWNSCALE_CHANGE_IDS;
            } else {
                enabled.add(168419799L);
                enabled.add(Long.valueOf(changeId));
                disabled = (Set) this.DOWNSCALE_CHANGE_IDS.stream().filter(new Predicate() { // from class: com.android.server.app.GameManagerServiceStubImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return GameManagerServiceStubImpl.lambda$downscaleApp$1(changeId, (Long) obj);
                    }
                }).collect(Collectors.toSet());
            }
            PlatformCompat platformCompat = ServiceManager.getService("platform_compat");
            CompatibilityChangeConfig overrides = new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
            platformCompat.setOverridesForDownscale(overrides, packageName);
            Slog.v(TAG, "downscale enable packageName = " + packageName + " ratio = " + ratioResult);
            appItem.ratio = ratioResult;
            this.mAppStates.put(packageName, appItem);
            this.limitLogger.log(new GmsLimitLogger.StringEvent(packageName + "," + ratioResult));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$downscaleApp$1(long changeId, Long it) {
        return (it.longValue() == 168419799 || it.longValue() == changeId) ? false : true;
    }

    private JSONObject initLocalDownscaleAppList() {
        FileInputStream f;
        String jsonString = "";
        try {
            try {
                f = new FileInputStream(DOWNSCALE_APP_SETTINGS_FILE_PATH);
                try {
                    BufferedReader bis = new BufferedReader(new InputStreamReader(f));
                    while (true) {
                        try {
                            String line = bis.readLine();
                            if (line == null) {
                                break;
                            }
                            jsonString = jsonString + line;
                        } catch (Throwable th) {
                            try {
                                bis.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                        }
                    }
                    bis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                f.close();
            } catch (Exception e2) {
                Slog.v(TAG, "system/etc/DownscaleAppSettings.json not find");
            }
            try {
                JSONObject appConfigJSON = new JSONObject(jsonString);
                return appConfigJSON;
            } catch (Exception e3) {
                return null;
            }
        } catch (Throwable th3) {
            try {
                f.close();
            } catch (Throwable th4) {
                th3.addSuppressed(th4);
            }
            throw th3;
        }
    }

    public void handleAppDied(String packageName) {
        if (this.mHandler != null) {
            if (TextUtils.equals(packageName, "com.tencent.mm")) {
                Slog.v(TAG, "com.tencent.mm handleAppDied");
            } else {
                if (!this.isDebugPowerSave && !GmsUtil.needDownscale(packageName, this.mDownscaleCloudData, this.currentWidth)) {
                    return;
                }
                Message msg = this.mHandler.obtainMessage(MSG_DOWNSCALE_APP_DIED);
                msg.obj = packageName;
                this.mHandler.sendMessage(msg);
            }
        }
    }

    public void setActivityManagerService(ActivityManagerService service) {
        this.mService = service;
        if (this.mHandler == null) {
            if (!this.mHandlerThread.isAlive()) {
                this.mHandlerThread.start();
            }
            this.mHandler = new InnerHandler(this.mHandlerThread.getLooper());
        }
    }

    private boolean runningApp(String packageName) {
        ActivityManagerService activityManagerService;
        if (TextUtils.isEmpty(packageName) || (activityManagerService = this.mService) == null) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManagerService.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            if (packageName.equals(runningAppProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    public void dump(PrintWriter pw) {
        StringBuilder sb = new StringBuilder();
        sb.append("WindowDownscale Service").append("\n");
        sb.append("IS_TABLET = " + Build.IS_TABLET).append("\n");
        sb.append("WQHD = " + GmsUtil.isWQHD(this.currentWidth)).append("\n");
        sb.append("IntelligentPowerSavingEnable = " + this.intelligentPowerSavingEnable).append("\n");
        sb.append("PowerSaving = " + this.mPowerSaving).append("\n");
        sb.append("ExtremeModeEnable = " + this.mExtremeModeEnable).append("\n");
        sb.append("FromCloud = " + this.fromCloud).append("\n");
        sb.append("CloudData = " + this.cloudDataStr).append("\n").append("\n");
        sb.append("--- Apps: begin ---").append("\n");
        for (Map.Entry<String, AppItem> entry : this.mAppStates.entrySet()) {
            sb.append(entry.getValue().toString()).append(" ");
        }
        sb.append("\n");
        sb.append("--- Apps: end").append("\n").append("\n");
        sb.append("--- Cmd app begin ---").append("\n");
        Iterator<String> it = this.mShellCmdDownscalePackageNames.iterator();
        while (it.hasNext()) {
            String item = it.next();
            sb.append("{packageName = " + item).append("} ");
        }
        sb.append("\n");
        sb.append("--- Cmd end").append("\n").append("\n");
        sb.append("--- trace ---");
        pw.println(sb);
        this.limitLogger.dump(pw);
    }

    /* loaded from: classes.dex */
    final class InnerHandler extends Handler {
        public InnerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GameManagerServiceStubImpl.MSG_DOWNSCALE_APP_DIED /* 111111 */:
                    String processName = (String) msg.obj;
                    if (GameManagerServiceStubImpl.FOLD_DEVICE && !GameManagerServiceStubImpl.this.mSizeCompatApps.isEmpty() && GameManagerServiceStubImpl.this.mSizeCompatApps.contains(processName)) {
                        GameManagerServiceStubImpl.this.mSizeCompatApps.remove(processName);
                        return;
                    } else {
                        if (!GameManagerServiceStubImpl.this.mShellCmdDownscalePackageNames.contains(processName)) {
                            GameManagerServiceStubImpl.this.toggleDownscale(processName);
                            return;
                        }
                        return;
                    }
                case GameManagerServiceStubImpl.MSG_PROCESS_POWERSAVE_ACTION /* 111112 */:
                    GameManagerServiceStubImpl.this.processPowerSaveAction();
                    return;
                case GameManagerServiceStubImpl.MSG_REBOOT_COMPLETED_ACTION /* 111113 */:
                    GameManagerServiceStubImpl.this.processBootCompletedAction();
                    return;
                case GameManagerServiceStubImpl.MSG_CLOUD_DATA_CHANGE /* 111114 */:
                    GameManagerServiceStubImpl.this.processCloudData();
                    return;
                case GameManagerServiceStubImpl.MSG_INTELLIGENT_POWERSAVE /* 111115 */:
                    GameManagerServiceStubImpl.this.processIntelligentPowerSave();
                    return;
                case GameManagerServiceStubImpl.MSG_EXTREME_MODE /* 111116 */:
                    GameManagerServiceStubImpl.this.executeDownscale(null, false);
                    return;
                default:
                    Slog.d(GameManagerServiceStubImpl.TAG, "default case");
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processIntelligentPowerSave() {
        if (DOWNSCALE_DISABLE.equals(this.mProEnable)) {
            return;
        }
        executeDownscale(null, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DownscaleCloudData {
        boolean enable;
        Scenes scenes;
        long version;
        HashSet<String> devices = new HashSet<>();
        HashMap<String, AppItem> apps = new HashMap<>();
        HashSet<String> offLocalDevices = new HashSet<>();

        public DownscaleCloudData(JSONObject cloudData) {
            if (cloudData != null) {
                this.enable = cloudData.optBoolean("enable", false);
                this.version = cloudData.optLong(AmapExtraCommand.VERSION_KEY, -1L);
                JSONArray appsJson = null;
                JSONArray devicesJson = null;
                JSONObject scenesJson = null;
                if (!Build.IS_TABLET) {
                    appsJson = cloudData.optJSONArray("apps");
                    devicesJson = cloudData.optJSONArray("devices");
                    scenesJson = cloudData.optJSONObject("scenes");
                } else {
                    JSONObject padJson = cloudData.optJSONObject(MiuiFreeformTrackManager.CommonTrackConstants.DEVICE_TYPE_PAD);
                    if (padJson == null) {
                        return;
                    }
                    this.enable = padJson.optBoolean("enable", false);
                    if (padJson != null) {
                        appsJson = padJson.optJSONArray("apps");
                        devicesJson = padJson.optJSONArray("devices");
                        scenesJson = padJson.optJSONObject("scenes");
                    }
                }
                parseApps(appsJson);
                parseOffLocalDevices(cloudData.optJSONArray("off_devices"));
                parseDevices(devicesJson);
                parseScenes(scenesJson);
            }
        }

        private void parseApps(JSONArray appsJson) {
            if (appsJson != null && appsJson.length() > 0) {
                for (int i = 0; i < appsJson.length(); i++) {
                    AppItem appItem = new AppItem(appsJson.optJSONObject(i));
                    this.apps.put(appItem.packageName, appItem);
                }
            }
        }

        private void parseDevices(JSONArray devicesJson) {
            if (devicesJson != null && devicesJson.length() > 0) {
                for (int i = 0; i < devicesJson.length(); i++) {
                    DeviceItem deviceItem = new DeviceItem(devicesJson.optJSONObject(i));
                    this.devices.add(deviceItem.name);
                }
                GmsSettings.getLocalDevicesForPowerSaving(this.devices, this.offLocalDevices);
            }
        }

        private void parseScenes(JSONObject scenesJson) {
            if (scenesJson != null) {
                this.scenes = new Scenes(scenesJson);
            }
        }

        private void parseOffLocalDevices(JSONArray offLocalDevicesJson) {
            if (offLocalDevicesJson != null && offLocalDevicesJson.length() > 0) {
                for (int i = 0; i < offLocalDevicesJson.length(); i++) {
                    DeviceItem deviceItem = new DeviceItem(offLocalDevicesJson.optJSONObject(i));
                    this.offLocalDevices.add(deviceItem.name);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AppItem {
        int appVersion;
        int mode;
        String packageName;
        String ratio = "";
        int systemVersion;

        public AppItem(JSONObject cloudDataApps) {
            if (cloudDataApps != null) {
                this.packageName = cloudDataApps.optString("packageName");
                this.systemVersion = cloudDataApps.optInt("systemVersion", 7);
                this.appVersion = cloudDataApps.optInt(AmapExtraCommand.VERSION_KEY, 0);
                this.mode = cloudDataApps.optInt("mode", 0);
            }
        }

        public String toString() {
            return "{packageName='" + this.packageName + "', appVersion=" + this.appVersion + ", mode=" + this.mode + ", ratio='" + this.ratio + "'}";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DeviceItem {
        String name;

        public DeviceItem(JSONObject cloudDataDevice) {
            if (cloudDataDevice != null) {
                this.name = cloudDataDevice.optString("name");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Scenes {
        int normal;
        String padSaveBattery;
        int saveBattery;

        public Scenes(JSONObject jsonObject) {
            if (jsonObject != null) {
                this.normal = jsonObject.optInt(EdgeSuppressionFactory.TYPE_NORMAL, GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT_WIDTH);
                this.saveBattery = jsonObject.optInt("save_battery", 720);
                this.padSaveBattery = jsonObject.optString("save_battery", "0.85");
            }
        }
    }

    /* loaded from: classes.dex */
    class GMSObserver extends ContentObserver {
        public GMSObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null) {
                return;
            }
            if (uri.equals(Settings.Global.getUriFor(GameManagerServiceStubImpl.DEBUG_PROP_KEY))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "DEBUG_PROP_KEY");
                String proEnable = Settings.Global.getString(GameManagerServiceStubImpl.this.mContext.getContentResolver(), GameManagerServiceStubImpl.DEBUG_PROP_KEY);
                if (TextUtils.equals(proEnable, GameManagerServiceStubImpl.this.mProEnable)) {
                    return;
                }
                GameManagerServiceStubImpl.this.mProEnable = proEnable;
                if (!GameManagerServiceStubImpl.this.mPowerSaving && !GameManagerServiceStubImpl.this.intelligentPowerSavingEnable) {
                    return;
                }
                GameManagerServiceStubImpl.this.toggleDownscale(null);
                return;
            }
            if (uri.equals(Settings.System.getUriFor(GameManagerServiceStubImpl.KEY_POWER_MODE_OPEN))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "KEY_POWER_MODE_OPEN");
                if (GameManagerServiceStubImpl.this.mHandler != null) {
                    GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_PROCESS_POWERSAVE_ACTION);
                    return;
                }
                return;
            }
            if (uri.equals(Uri.parse(GameManagerServiceStubImpl.CLOUD_ALL_DATA_CHANGE_URI))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "CLOUD_ALL_DATA_CHANGE_URI");
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_CLOUD_DATA_CHANGE);
                return;
            }
            if (uri.equals(Settings.System.getUriFor(GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "MIUI_SCREEN_COMPAT");
                GameManagerServiceStubImpl gameManagerServiceStubImpl = GameManagerServiceStubImpl.this;
                gameManagerServiceStubImpl.intelligentPowerSavingEnable = Settings.System.getInt(gameManagerServiceStubImpl.mContext.getContentResolver(), GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT, 1) == 1;
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_INTELLIGENT_POWERSAVE);
                return;
            }
            if (uri.equals(Settings.Secure.getUriFor(GameManagerServiceStubImpl.PC_SECURITY_CENTER_EXTREME_MODE))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "PC_SECURITY_CENTER_EXTREME_MODE");
                GameManagerServiceStubImpl gameManagerServiceStubImpl2 = GameManagerServiceStubImpl.this;
                gameManagerServiceStubImpl2.mExtremeModeEnable = Settings.Secure.getInt(gameManagerServiceStubImpl2.mContext.getContentResolver(), GameManagerServiceStubImpl.PC_SECURITY_CENTER_EXTREME_MODE, 0) == 1;
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_EXTREME_MODE);
            }
        }
    }

    /* loaded from: classes.dex */
    class GMSBroadcastReceiver extends BroadcastReceiver {
        GMSBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.d(GameManagerServiceStubImpl.TAG, "ACTION_BOOT_COMPLETED");
            if (intent != null && intent.getAction() == "android.intent.action.BOOT_COMPLETED" && GameManagerServiceStubImpl.this.mHandler != null) {
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_REBOOT_COMPLETED_ACTION);
            }
        }
    }
}
