package com.android.server.am;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.os.BackgroundThread;
import com.android.server.ServiceThread;
import com.android.server.power.stats.BatteryStatsManagerStub;
import com.miui.enterprise.settings.EnterpriseSettings;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.process.ProcessCloudData;
import miui.process.ProcessManager;
import miui.util.DeviceLevel;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ProcessPolicy implements BatteryStatsManagerStub.ActiveCallback, IProcessPolicy {
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_ACTIVE = true;
    private static final long DEFAULT_FASTBOOT_THRESHOLDKB = 524288000;
    private static final boolean DYNAMIC_LIST_CHECK_ADJ = true;
    private static final String JSON_KEY_PACKAGE_NAMES = "pkgs";
    private static final String JSON_KEY_USER_ID = "u";
    private static final int MSG_UPDATE_AUDIO_OFF = 1;
    private static final int MSG_UPDATE_STOP_GPS = 2;
    private static final int PERCEPTIBLE_APP_ADJ;
    private static final String PKG_MIUI_CONTENT_CATCHER = "com.miui.contentcatcher";
    private static final String PKG_MIUI_VOICETRIGGER = "com.miui.voicetrigger";
    private static final int PRIORITY_LEVEL_HEAVY_WEIGHT = 3;
    private static final int PRIORITY_LEVEL_PERCEPTIBLE = 2;
    private static final int PRIORITY_LEVEL_UNKNOWN = -1;
    private static final int PRIORITY_LEVEL_VISIBLE = 1;
    public static final String TAG = "ProcessManager";
    private static final long UPDATE_AUDIO_OFF_DELAY = 600;
    private static final long UPDATE_STOP_GPS_DELAY = 1000;
    private static SparseArray<ActiveUidRecord> sActiveUidList;
    private static Map<String, Integer> sAppProtectMap;
    private static Map<String, String> sBoundFgServiceProtectMap;
    private static List<String> sDisableForceStopList;
    private static List<String> sDisableTrimList;
    private static List<String> sDisplaySizeBlackList;
    private static List<String> sDisplaySizeProtectList;
    private static List<String> sEnableCallProtectList;
    private static List<String> sEnterpriseAppList;
    private static List<String> sExtraPackageWhiteList;
    private static Map<String, Long> sFastBootAppMap;
    private static List<String> sFgServiceCheckList;
    private static final Object sLock;
    private static HashMap<Integer, Set<String>> sLockedApplicationList;
    static List<String> sLowMemKillProcReasons;
    private static List<String> sNeedTraceList;
    private static SparseArray<Integer> sPolicyFlagsList;
    public static final SparseArray<Pair<Integer, Integer>> sProcessPriorityMap;
    private static List<String> sSecretlyProtectAppList;
    private static List<String> sSystemCleanWhiteList;
    private static SparseArray<ActiveUidRecord> sTempInactiveAudioList;
    private static SparseArray<ActiveUidRecord> sTempInactiveGPSList;
    static List<String> sUserKillProcReasons;
    private static List<String> sUserRequestCleanWhiteList;
    private AccessibilityManager mAccessibilityManager;
    private ActiveUpdateHandler mActiveUpdateHandler;
    private ActivityManagerService mActivityManagerService;
    private Context mContext;
    private ProcessManagerService mProcessManagerService;
    private final ContentObserver mProcessPolicyObserver;
    private static final String SETTINGS_MIUI_VOICETRIGGER_ENABLE = "voice_trigger_enabled";
    private static final Uri SETTINGS_MIUI_VOICETRIGGER_ENABLE_URL = Settings.Global.getUriFor(SETTINGS_MIUI_VOICETRIGGER_ENABLE);
    private static final String SETTINGS_MIUI_CCONTENT_CATCHER_ENABLE = "open_content_extension_clipboard_mode";
    private static final Uri SETTINGS_MIUI_CCONTENT_CATCHER_URL = Settings.System.getUriFor(SETTINGS_MIUI_CCONTENT_CATCHER_ENABLE);
    private static List<String> sStaticWhiteList = new ArrayList();
    private static List<String> sProcessStaticWhiteList = new ArrayList();
    private static HashMap<String, Boolean> sDynamicWhiteList = new HashMap<>();
    private static List<String> sCloudWhiteList = new ArrayList();
    private static List<String> sHeapBlackList = new ArrayList();
    private static HashMap<String, List<String>> sOneKeyCleanWhiteList = new HashMap<>();

    static {
        sHeapBlackList.add("com.ss.android.ugc.aweme");
        sOneKeyCleanWhiteList.put("com.xiaomi.mslgrdp", new ArrayList(Arrays.asList("com.xiaomi.mslgrdp:multiwindow.rdp")));
        sLockedApplicationList = new HashMap<>();
        sDisableTrimList = new ArrayList();
        sDisableForceStopList = new ArrayList();
        sEnableCallProtectList = new ArrayList();
        sNeedTraceList = new ArrayList();
        sSecretlyProtectAppList = new ArrayList();
        sUserKillProcReasons = new ArrayList();
        sLowMemKillProcReasons = new ArrayList();
        sEnterpriseAppList = new ArrayList();
        sFgServiceCheckList = new ArrayList();
        sBoundFgServiceProtectMap = new HashMap();
        sDisplaySizeProtectList = new ArrayList();
        sDisplaySizeBlackList = new ArrayList();
        sUserRequestCleanWhiteList = new ArrayList();
        sSystemCleanWhiteList = new ArrayList();
        sExtraPackageWhiteList = new ArrayList();
        sActiveUidList = new SparseArray<>();
        sTempInactiveAudioList = new SparseArray<>();
        sTempInactiveGPSList = new SparseArray<>();
        sPolicyFlagsList = new SparseArray<>();
        sFastBootAppMap = new HashMap();
        sAppProtectMap = new HashMap();
        sLock = new Object();
        SparseArray<Pair<Integer, Integer>> sparseArray = new SparseArray<>();
        sProcessPriorityMap = sparseArray;
        PERCEPTIBLE_APP_ADJ = 200;
        sUserKillProcReasons.add(IProcessPolicy.REASON_ONE_KEY_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_FORCE_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_GARBAGE_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_GAME_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_OPTIMIZATION_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_SWIPE_UP_CLEAN);
        sUserKillProcReasons.add(IProcessPolicy.REASON_USER_DEFINED);
        sLowMemKillProcReasons.add(IProcessPolicy.REASON_ANR);
        sLowMemKillProcReasons.add(IProcessPolicy.REASON_CRASH);
        sLowMemKillProcReasons.add(IProcessPolicy.REASON_MIUI_MEMO_SERVICE);
        sLowMemKillProcReasons.add(IProcessPolicy.REASON_LOW_MEMO);
        long memoryThreshold = IProcessPolicy.getMemoryThresholdForFastBooApp().longValue();
        sFastBootAppMap.put("com.tencent.mm", Long.valueOf(memoryThreshold));
        sFastBootAppMap.put("com.tencent.mobileqq", Long.valueOf(memoryThreshold));
        sparseArray.put(-1, ProcessUtils.PRIORITY_UNKNOW);
        sparseArray.put(1, ProcessUtils.PRIORITY_VISIBLE);
        sparseArray.put(2, ProcessUtils.PRIORITY_PERCEPTIBLE);
        sparseArray.put(3, ProcessUtils.PRIORITY_HEAVY);
        sAppProtectMap.put("com.miui.bugreport", 3);
        sAppProtectMap.put("com.miui.virtualsim", 3);
        sAppProtectMap.put("com.miui.touchassistant", 3);
        sAppProtectMap.put("com.xiaomi.joyose", 3);
        sAppProtectMap.put("com.miui.tsmclient", 3);
        sAppProtectMap.put("com.miui.powerkeeper", 3);
        sBoundFgServiceProtectMap.put("com.milink.service", "com.xiaomi.miplay_client");
        sPolicyFlagsList.put(1, 8197);
        sPolicyFlagsList.put(7, 8197);
        sPolicyFlagsList.put(4, 5);
        sPolicyFlagsList.put(5, 5);
        sPolicyFlagsList.put(14, 5);
        sPolicyFlagsList.put(3, 5);
        sPolicyFlagsList.put(6, 5);
        sPolicyFlagsList.put(16, 5);
        sPolicyFlagsList.put(22, 5);
        sPolicyFlagsList.put(19, 5);
        sPolicyFlagsList.put(20, 5);
        sPolicyFlagsList.put(2, 5);
        sPolicyFlagsList.put(21, 53);
    }

    /* loaded from: classes.dex */
    class ActiveUpdateHandler extends Handler {
        public ActiveUpdateHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ActiveUidRecord uidRecord = (ActiveUidRecord) msg.obj;
            switch (msg.what) {
                case 1:
                    checkRemoveActiveUid(uidRecord, 1);
                    return;
                case 2:
                    checkRemoveActiveUid(uidRecord, 2);
                    return;
                default:
                    return;
            }
        }

        private void checkRemoveActiveUid(ActiveUidRecord uidRecord, int flag) {
            if (uidRecord != null) {
                synchronized (ProcessPolicy.sLock) {
                    try {
                        switch (flag) {
                            case 1:
                                ProcessPolicy.sTempInactiveAudioList.remove(uidRecord.uid);
                                break;
                            case 2:
                                ProcessPolicy.sTempInactiveGPSList.remove(uidRecord.uid);
                                break;
                            default:
                                return;
                        }
                        uidRecord.flag &= ~flag;
                        if (uidRecord.flag == 0) {
                            ProcessPolicy.sActiveUidList.remove(uidRecord.uid);
                            Slog.d("ProcessManager", "real remove inactive uid : " + uidRecord.uid + " flag : " + flag);
                        }
                    } finally {
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class ActiveUidRecord {
        static final int ACTIVE_AUDIO = 1;
        static final int ACTIVE_GPS = 2;
        static final int NO_ACTIVE = 0;
        public int flag;
        public int uid;

        public ActiveUidRecord(int _uid) {
            this.uid = _uid;
        }

        private void makeActiveString(StringBuilder sb) {
            sb.append("flag :");
            sb.append(this.flag);
            sb.append(' ');
            boolean printed = false;
            int i = this.flag;
            if (i == 0) {
                sb.append("NONE");
                return;
            }
            if ((i & 1) != 0) {
                printed = true;
                sb.append("A");
            }
            if ((this.flag & 2) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("G");
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActiveUidRecord{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            UserHandle.formatUid(sb, this.uid);
            sb.append(' ');
            makeActiveString(sb);
            sb.append("}");
            return sb.toString();
        }
    }

    public ProcessPolicy(ProcessManagerService processManagerService, ActivityManagerService ams, AccessibilityManager accessibilityManager, ServiceThread thread) {
        this.mProcessPolicyObserver = new ContentObserver(this.mActiveUpdateHandler) { // from class: com.android.server.am.ProcessPolicy.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (uri.equals(ProcessPolicy.SETTINGS_MIUI_VOICETRIGGER_ENABLE_URL)) {
                    ProcessPolicy.this.updateVoiceTriggerWhitelist();
                } else if (uri.equals(ProcessPolicy.SETTINGS_MIUI_CCONTENT_CATCHER_URL)) {
                    ProcessPolicy.this.updateContentCatcherWhitelist();
                }
            }
        };
        this.mProcessManagerService = processManagerService;
        this.mActivityManagerService = ams;
        this.mAccessibilityManager = accessibilityManager;
        this.mActiveUpdateHandler = new ActiveUpdateHandler(thread.getLooper());
    }

    public void systemReady(Context context) {
        String[] pckWhiteList;
        String[] prcWhiteList;
        String[] pckWhiteList2;
        this.mContext = context;
        synchronized (sLock) {
            sStaticWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409454)));
            sProcessStaticWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409468)));
            if (DeviceLevel.IS_MIUI_LITE_VERSION) {
                pckWhiteList = context.getResources().getStringArray(285409455);
                prcWhiteList = context.getResources().getStringArray(285409469);
            } else {
                pckWhiteList = context.getResources().getStringArray(285409453);
                prcWhiteList = context.getResources().getStringArray(285409467);
            }
            if (pckWhiteList != null) {
                sStaticWhiteList.addAll(new ArrayList(Arrays.asList(pckWhiteList)));
            }
            if (prcWhiteList != null) {
                sProcessStaticWhiteList.addAll(new ArrayList(Arrays.asList(prcWhiteList)));
            }
            sDisableTrimList = Arrays.asList(context.getResources().getStringArray(285409462));
            sDisableForceStopList = Arrays.asList(context.getResources().getStringArray(285409461));
            sNeedTraceList = Arrays.asList(context.getResources().getStringArray(285409447));
            sSecretlyProtectAppList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409466)));
            sFgServiceCheckList = Arrays.asList(context.getResources().getStringArray(285409465));
            sDisplaySizeProtectList = Arrays.asList(context.getResources().getStringArray(285409464));
            sDisplaySizeBlackList = Arrays.asList(context.getResources().getStringArray(285409463));
            sUserRequestCleanWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409456)));
            sSystemCleanWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409470)));
        }
        loadLockedAppFromSettings(context);
        updateApplicationLockedState("com.jeejen.family.miui", -100, true);
        if (!TextUtils.isEmpty(PressureStateSettings.WHITE_LIST_PKG) && (pckWhiteList2 = PressureStateSettings.WHITE_LIST_PKG.split(",")) != null && pckWhiteList2.length > 0) {
            sExtraPackageWhiteList = Arrays.asList(pckWhiteList2);
        }
        registerContentObserver();
    }

    private static boolean isLiteOrMiddle() {
        return DeviceLevel.IS_MIUI_LITE_VERSION || DeviceLevel.IS_MIUI_MIDDLE_VERSION;
    }

    private void registerContentObserver() {
        updateVoiceTriggerWhitelist();
        this.mContext.getContentResolver().registerContentObserver(SETTINGS_MIUI_VOICETRIGGER_ENABLE_URL, true, this.mProcessPolicyObserver);
        updateContentCatcherWhitelist();
        this.mContext.getContentResolver().registerContentObserver(SETTINGS_MIUI_CCONTENT_CATCHER_URL, true, this.mProcessPolicyObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVoiceTriggerWhitelist() {
        if (Build.IS_INTERNATIONAL_BUILD || isLiteOrMiddle()) {
            Slog.i("ProcessManager", "update voicetrigger whitelist error, build " + Build.IS_INTERNATIONAL_BUILD + " isProtect " + (true ^ isLiteOrMiddle()));
            return;
        }
        boolean isEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), SETTINGS_MIUI_VOICETRIGGER_ENABLE, 0) == 1;
        sProcessStaticWhiteList.remove(PKG_MIUI_VOICETRIGGER);
        if (isEnabled) {
            sProcessStaticWhiteList.add(PKG_MIUI_VOICETRIGGER);
        }
        Slog.i("ProcessManager", "update voicetrigger whitelist, enabled " + isEnabled);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateContentCatcherWhitelist() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            Slog.i("ProcessManager", "update taplus clipboard whitelist error, build international");
            return;
        }
        boolean isEnabled = Settings.System.getInt(this.mContext.getContentResolver(), SETTINGS_MIUI_CCONTENT_CATCHER_ENABLE, 0) == 1;
        sProcessStaticWhiteList.remove(PKG_MIUI_CONTENT_CATCHER);
        if (isEnabled) {
            sProcessStaticWhiteList.add(PKG_MIUI_CONTENT_CATCHER);
        }
    }

    public List<String> getWhiteList(int flags) {
        List<String> whiteList = new ArrayList<>();
        synchronized (sLock) {
            if ((flags & 1) != 0) {
                try {
                    whiteList.addAll(sStaticWhiteList);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((flags & 2) != 0) {
                whiteList.addAll(sDynamicWhiteList.keySet());
            }
            if ((flags & 4) != 0) {
                whiteList.addAll(sCloudWhiteList);
            }
            if ((flags & 16) != 0) {
                whiteList.addAll(sDisableTrimList);
            }
            if ((flags & 32) != 0) {
                whiteList.addAll(sDisableForceStopList);
            }
            if ((flags & 64) != 0) {
                whiteList.addAll(sEnableCallProtectList);
            }
            if ((flags & 128) != 0) {
                whiteList.addAll(sNeedTraceList);
            }
            if ((flags & 1024) != 0) {
                whiteList.addAll(sSecretlyProtectAppList);
            }
            if ((flags & 2048) != 0) {
                whiteList.addAll(sFastBootAppMap.keySet());
            }
            if ((flags & 8192) != 0) {
                whiteList.addAll(sUserRequestCleanWhiteList);
            }
            if (EnterpriseSettings.ENTERPRISE_ACTIVATED && (flags & 4096) != 0) {
                whiteList.addAll(sEnterpriseAppList);
            }
        }
        return whiteList;
    }

    public void addWhiteList(int flag, List<String> whiteList, boolean append) {
        List<String> targetWhiteList;
        synchronized (sLock) {
            try {
                if ((flag & 1) != 0) {
                    targetWhiteList = sStaticWhiteList;
                } else if ((flag & 4) != 0) {
                    targetWhiteList = sCloudWhiteList;
                } else if ((flag & 16) != 0) {
                    targetWhiteList = sDisableTrimList;
                } else if ((flag & 32) != 0) {
                    targetWhiteList = sDisableForceStopList;
                } else if (EnterpriseSettings.ENTERPRISE_ACTIVATED && (flag & 4096) != 0) {
                    targetWhiteList = sEnterpriseAppList;
                } else {
                    targetWhiteList = new ArrayList<>();
                    Slog.e("ProcessManager", "addWhiteList with unknown flag=" + flag);
                }
                if (!append) {
                    targetWhiteList.clear();
                }
                targetWhiteList.addAll(whiteList);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public HashMap<String, Boolean> updateDynamicWhiteList(Context context, int userId) {
        Iterator<AccessibilityServiceInfo> it;
        HashMap<String, Boolean> activeWhiteList = new HashMap<>();
        String wallpaperPkg = ProcessUtils.getActiveWallpaperPackage(context);
        if (wallpaperPkg != null) {
            activeWhiteList.put(wallpaperPkg, true);
        }
        String inputMethodPkg = ProcessUtils.getDefaultInputMethod(context);
        if (inputMethodPkg != null) {
            activeWhiteList.put(inputMethodPkg, true);
        }
        String ttsEngine = ProcessUtils.getActiveTtsEngine(context);
        if (ttsEngine != null) {
            activeWhiteList.put(ttsEngine, true);
        }
        if (ProcessUtils.isPhoneWorking()) {
            TelecomManager telecomm = (TelecomManager) context.getSystemService("telecom");
            if (telecomm != null && !telecomm.getDefaultDialerPackage().equals("com.android.contacts")) {
                activeWhiteList.put("com.qualcomm.qtil.btdsda", false);
                activeWhiteList.put(telecomm.getDefaultDialerPackage(), true);
            } else {
                activeWhiteList.put("com.qualcomm.qtil.btdsda", false);
                activeWhiteList.put("com.android.incallui", true);
            }
        }
        activeWhiteList.put("com.miui.voip", true);
        synchronized (this.mActivityManagerService) {
            try {
                for (String packageName : sFgServiceCheckList) {
                    try {
                        List<ProcessRecord> appList = this.mProcessManagerService.getProcessRecordList(packageName, userId);
                        Iterator<ProcessRecord> it2 = appList.iterator();
                        while (true) {
                            if (!it2.hasNext()) {
                                break;
                            }
                            ProcessRecord app = it2.next();
                            if (app != null && app.mServices.hasForegroundServices()) {
                                activeWhiteList.put(packageName, true);
                                if (sBoundFgServiceProtectMap.containsKey(packageName)) {
                                    activeWhiteList.put(sBoundFgServiceProtectMap.get(packageName), false);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                List<AccessibilityServiceInfo> accessibilityList = this.mAccessibilityManager.getEnabledAccessibilityServiceList(-1);
                if (accessibilityList != null && !accessibilityList.isEmpty()) {
                    Iterator<AccessibilityServiceInfo> it3 = accessibilityList.iterator();
                    while (it3.hasNext()) {
                        AccessibilityServiceInfo info = it3.next();
                        if (info == null || TextUtils.isEmpty(info.getId())) {
                            it = it3;
                        } else {
                            ComponentName componentName = ComponentName.unflattenFromString(info.getId());
                            String pkg = componentName != null ? componentName.getPackageName() : null;
                            if (pkg == null || activeWhiteList.containsKey(pkg)) {
                                it = it3;
                            } else {
                                ApplicationInfo app2 = info.getResolveInfo().serviceInfo.applicationInfo;
                                boolean isSystemApp = app2.isSystemApp() || app2.isUpdatedSystemApp();
                                if (isSystemApp) {
                                    it = it3;
                                    activeWhiteList.put(pkg, false);
                                } else {
                                    it = it3;
                                    if ((info.feedbackType & 7) != 0) {
                                        activeWhiteList.put(componentName.getPackageName(), true);
                                    }
                                }
                            }
                        }
                        it3 = it;
                    }
                }
                Log.d("ProcessManager", "update DY:" + Arrays.toString(activeWhiteList.keySet().toArray()));
                synchronized (sLock) {
                    sDynamicWhiteList = activeWhiteList;
                }
                return activeWhiteList;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void resetWhiteList(Context context, int userId) {
        updateDynamicWhiteList(context, userId);
    }

    public void updateApplicationLockedState(final Context context, int userId, String packageName, boolean isLocked) {
        updateApplicationLockedState(packageName, userId, isLocked);
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessPolicy.2
            @Override // java.lang.Runnable
            public void run() {
                ProcessPolicy.this.saveLockedAppIntoSettings(context);
            }
        });
        ProcessRecord targetApp = this.mProcessManagerService.getProcessRecord(packageName, userId);
        if (targetApp != null) {
            promoteLockedApp(targetApp);
        }
    }

    private void updateApplicationLockedState(String packageName, int userId, boolean isLocked) {
        synchronized (sLock) {
            Set<String> lockedApplication = sLockedApplicationList.get(Integer.valueOf(userId));
            if (lockedApplication == null) {
                lockedApplication = new HashSet();
                sLockedApplicationList.put(Integer.valueOf(userId), lockedApplication);
            }
            if (isLocked) {
                lockedApplication.add(packageName);
            } else {
                lockedApplication.remove(packageName);
                removeDefaultLockedAppIfExists(packageName);
            }
        }
    }

    private void removeDefaultLockedAppIfExists(String packageName) {
        Set<String> defaultLockedApps = sLockedApplicationList.get(-100);
        if (defaultLockedApps != null && defaultLockedApps.contains(packageName)) {
            defaultLockedApps.remove(packageName);
        }
    }

    protected void promoteLockedApp(ProcessRecord app) {
        if (app.isPersistent() || isInSecretlyProtectList(app.processName)) {
            Log.d("ProcessManager", "do not promote " + app.processName);
            return;
        }
        boolean isLocked = isLockedApplication(app.processName, app.userId);
        int targetMaxAdj = isLocked ? ProcessManager.LOCKED_MAX_ADJ : 1001;
        int targetMaxProcState = isLocked ? ProcessManager.LOCKED_MAX_PROCESS_STATE : 20;
        updateMaxAdjLocked(app, targetMaxAdj, isLocked);
        updateMaxProcStateLocked(app, targetMaxProcState, isLocked);
        Slog.d("ProcessManager", "promoteLockedApp:" + isLocked + ", set " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(IProcessPolicy.getAppMaxProcState(app)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveLockedAppIntoSettings(Context context) {
        synchronized (sLock) {
            JSONArray userSpaceArray = new JSONArray();
            try {
                for (Integer userId : sLockedApplicationList.keySet()) {
                    JSONObject userSpaceObject = new JSONObject();
                    userSpaceObject.put(JSON_KEY_USER_ID, userId);
                    userSpaceObject.put(JSON_KEY_PACKAGE_NAMES, new JSONArray((Collection) sLockedApplicationList.get(userId)));
                    userSpaceArray.put(userSpaceObject);
                }
                Log.d("ProcessManager", "saveLockedAppIntoSettings:" + userSpaceArray.toString());
            } catch (Exception e) {
                Log.d("ProcessManager", "saveLockedAppIntoSettings failed: " + e.toString());
                e.printStackTrace();
            }
            MiuiSettings.System.putString(context.getContentResolver(), "locked_apps", userSpaceArray.toString());
        }
    }

    private void loadLockedAppFromSettings(Context context) {
        synchronized (sLock) {
            String jsonFormatText = MiuiSettings.System.getString(context.getContentResolver(), "locked_apps");
            if (TextUtils.isEmpty(jsonFormatText)) {
                return;
            }
            try {
                JSONArray userSpaceArray = new JSONArray(jsonFormatText);
                for (int spaceIndex = 0; spaceIndex < userSpaceArray.length(); spaceIndex++) {
                    JSONObject userSpaceObject = (JSONObject) userSpaceArray.get(spaceIndex);
                    int userId = userSpaceObject.getInt(JSON_KEY_USER_ID);
                    JSONArray packageNameArray = userSpaceObject.getJSONArray(JSON_KEY_PACKAGE_NAMES);
                    Set<String> packageNameSet = new HashSet<>();
                    for (int pkgIndex = 0; pkgIndex < packageNameArray.length(); pkgIndex++) {
                        packageNameSet.add(packageNameArray.getString(pkgIndex));
                    }
                    sLockedApplicationList.put(Integer.valueOf(userId), packageNameSet);
                    Log.d("ProcessManager", "loadLockedAppFromSettings userId:" + userId + "-pkgNames:" + Arrays.toString(packageNameSet.toArray()));
                }
            } catch (Exception e) {
                Log.d("ProcessManager", "loadLockedApp failed: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override // com.android.server.am.IProcessPolicy
    public boolean isLockedApplication(String packageName, int userId) {
        synchronized (sLock) {
            if (!isLockedApplicationForUserId(packageName, userId) && !isLockedApplicationForUserId(packageName, -100)) {
                return false;
            }
            return true;
        }
    }

    private boolean isLockedApplicationForUserId(String packageName, int userId) {
        Set<String> lockedApplication;
        if (packageName != null && (lockedApplication = sLockedApplicationList.get(Integer.valueOf(userId))) != null) {
            for (String item : lockedApplication) {
                if (item.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getLockedApplication(int userId) {
        List<String> lockedApps = new ArrayList<>();
        Set<String> userApps = sLockedApplicationList.get(Integer.valueOf(userId));
        if (userApps != null && userApps.size() > 0) {
            lockedApps.addAll(userApps);
        }
        lockedApps.addAll(sLockedApplicationList.get(-100));
        return lockedApps;
    }

    public void updateCloudData(ProcessCloudData cloudData) {
        updateCloudWhiteList(cloudData);
        updateAppProtectMap(cloudData);
        updateFastBootList(cloudData);
        updateSecretlyProtectAppList(cloudData);
    }

    private void updateCloudWhiteList(ProcessCloudData cloudData) {
        List<String> cloudWhiteList = cloudData.getCloudWhiteList();
        synchronized (sLock) {
            if (cloudWhiteList != null) {
                try {
                    if (!cloudWhiteList.isEmpty() && !cloudWhiteList.equals(sCloudWhiteList)) {
                        sCloudWhiteList.clear();
                        sCloudWhiteList.addAll(cloudWhiteList);
                        Log.d("ProcessManager", "update CL:" + Arrays.toString(sCloudWhiteList.toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((cloudWhiteList == null || cloudWhiteList.isEmpty()) && !sCloudWhiteList.isEmpty()) {
                sCloudWhiteList.clear();
                Log.d("ProcessManager", "update CL:" + Arrays.toString(sCloudWhiteList.toArray()));
            }
        }
    }

    private void updateAppProtectMap(ProcessCloudData cloudData) {
        Map<String, Integer> appProtectMap = cloudData.getAppProtectMap();
        synchronized (sLock) {
            if (appProtectMap != null) {
                try {
                    if (!appProtectMap.isEmpty() && !appProtectMap.equals(sAppProtectMap)) {
                        sAppProtectMap.clear();
                        sAppProtectMap.putAll(appProtectMap);
                        Log.d("ProcessManager", "update AP:" + Arrays.toString(sAppProtectMap.keySet().toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((appProtectMap == null || appProtectMap.isEmpty()) && !sAppProtectMap.isEmpty()) {
                sAppProtectMap.clear();
                Log.d("ProcessManager", "update AP:" + Arrays.toString(sAppProtectMap.keySet().toArray()));
            }
        }
    }

    private void updateFastBootList(ProcessCloudData cloudData) {
        Set<String> oldFastBootSet;
        List<String> fastBootList = cloudData.getFastBootList();
        Object obj = sLock;
        synchronized (obj) {
            oldFastBootSet = sFastBootAppMap.keySet();
        }
        if (fastBootList != null && !fastBootList.isEmpty() && !oldFastBootSet.equals(new HashSet(fastBootList))) {
            synchronized (obj) {
                Map<String, Long> temp = new HashMap<>();
                for (String packageName : fastBootList) {
                    long thresholdKb = sFastBootAppMap.get(packageName).longValue();
                    temp.put(packageName, Long.valueOf(thresholdKb > 0 ? thresholdKb : DEFAULT_FASTBOOT_THRESHOLDKB));
                }
                sFastBootAppMap.clear();
                sFastBootAppMap.putAll(temp);
            }
            Log.d("ProcessManager", "update FA:" + Arrays.toString(sFastBootAppMap.keySet().toArray()));
            return;
        }
        if ((fastBootList == null || fastBootList.isEmpty()) && !sFastBootAppMap.isEmpty()) {
            synchronized (obj) {
                sFastBootAppMap.clear();
            }
            Log.d("ProcessManager", "update FA:" + Arrays.toString(sFastBootAppMap.keySet().toArray()));
        }
    }

    private void updateSecretlyProtectAppList(ProcessCloudData cloudData) {
        List<String> secretlyProtectAppList = cloudData.getSecretlyProtectAppList();
        synchronized (sLock) {
            if (secretlyProtectAppList != null) {
                try {
                    if (!secretlyProtectAppList.isEmpty() && !secretlyProtectAppList.equals(sSecretlyProtectAppList)) {
                        sSecretlyProtectAppList.clear();
                        sSecretlyProtectAppList.addAll(secretlyProtectAppList);
                        Log.d("ProcessManager", "update SPAL:" + Arrays.toString(sSecretlyProtectAppList.toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((secretlyProtectAppList == null || secretlyProtectAppList.isEmpty()) && !sSecretlyProtectAppList.isEmpty()) {
                sSecretlyProtectAppList.clear();
                Log.d("ProcessManager", "update SPAL:" + Arrays.toString(sSecretlyProtectAppList.toArray()));
            }
        }
    }

    public boolean isProcessImportant(ProcessRecord app) {
        Pair<Boolean, Boolean> isInDynamicPair = isInDynamicList(app);
        return ((Boolean) isInDynamicPair.first).booleanValue() && (!((Boolean) isInDynamicPair.second).booleanValue() || app.mServices.hasForegroundServices() || app.mState.getCurAdj() <= PERCEPTIBLE_APP_ADJ);
    }

    public Pair<Boolean, Boolean> isInDynamicList(ProcessRecord app) {
        if (app != null) {
            synchronized (sLock) {
                String packageName = app.info.packageName;
                if (sDynamicWhiteList.keySet().contains(packageName)) {
                    return new Pair<>(Boolean.TRUE, sDynamicWhiteList.get(packageName));
                }
            }
        }
        return new Pair<>(Boolean.FALSE, Boolean.FALSE);
    }

    public boolean isFastBootEnable(String packageName, int uid, boolean checkPss) {
        return uid > 0 && isInFastBootList(packageName, uid, checkPss) && this.mProcessManagerService.isAllowAutoStart(packageName, uid);
    }

    public boolean isInFastBootList(String packageName, int uid, boolean checkPss) {
        boolean res;
        long pss = checkPss ? ProcessUtils.getPackageLastPss(packageName, UserHandle.getUserId(uid)) : 0L;
        synchronized (sLock) {
            res = sFastBootAppMap.keySet().contains(packageName);
            if (res && checkPss && pss > sFastBootAppMap.get(packageName).longValue()) {
                Log.w("ProcessManager", "ignore fast boot, caused pkg:" + packageName + " is too large, with pss:" + pss);
                res = false;
            }
        }
        return res;
    }

    public HashMap<String, List<String>> getOneKeyCleanWhiteList() {
        return sOneKeyCleanWhiteList;
    }

    public boolean isInAppProtectList(String packageName) {
        boolean contains;
        synchronized (sLock) {
            contains = sAppProtectMap.keySet().contains(packageName);
        }
        return contains;
    }

    public boolean isInProcessStaticWhiteList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sProcessStaticWhiteList.contains(processName);
        }
        return contains;
    }

    public boolean isInDisplaySizeWhiteList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sDisplaySizeProtectList.contains(processName);
        }
        return contains;
    }

    public boolean isInDisplaySizeBlackList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sDisplaySizeBlackList.contains(processName);
        }
        return contains;
    }

    public boolean isInSecretlyProtectList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sSecretlyProtectAppList.contains(processName);
        }
        return contains;
    }

    public boolean isInSystemCleanWhiteList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sSystemCleanWhiteList.contains(processName);
        }
        return contains;
    }

    public boolean isInExtraPackageList(String packageName) {
        boolean contains;
        synchronized (sLock) {
            contains = sExtraPackageWhiteList.contains(packageName);
        }
        return contains;
    }

    public void updateSystemCleanWhiteList(String processName) {
        synchronized (sLock) {
            if (!sSystemCleanWhiteList.contains(processName)) {
                sSystemCleanWhiteList.add(processName);
            }
        }
    }

    public boolean protectCurrentProcess(ProcessRecord app, boolean isProtected) {
        Integer priorityLevel;
        if (app == null || app.info == null) {
            return false;
        }
        synchronized (sLock) {
            priorityLevel = sAppProtectMap.get(app.info.packageName);
        }
        if (priorityLevel == null) {
            return false;
        }
        updateProcessPriority(app, priorityLevel.intValue(), isProtected);
        Slog.d("ProcessManager", "protectCurrentProcess:" + isProtected + ", set " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(IProcessPolicy.getAppMaxProcState(app)));
        return true;
    }

    private void updateProcessPriority(ProcessRecord app, int priorityLevel, boolean protect) {
        Pair<Integer, Integer> priorityPair = sProcessPriorityMap.get(priorityLevel);
        if (priorityPair != null) {
            int targetMaxAdj = protect ? ((Integer) priorityPair.first).intValue() : 1001;
            updateMaxAdjLocked(app, targetMaxAdj, protect);
            int targetMaxProcState = protect ? ((Integer) priorityPair.second).intValue() : 20;
            updateMaxProcStateLocked(app, targetMaxProcState, protect);
        }
    }

    private void updateMaxAdjLocked(ProcessRecord app, int targetMaxAdj, boolean protect) {
        if (app.isPersistent()) {
            return;
        }
        if (protect && app.mState.getMaxAdj() > targetMaxAdj) {
            app.mState.setMaxAdj(targetMaxAdj);
        } else if (!protect && app.mState.getMaxAdj() < targetMaxAdj) {
            app.mState.setMaxAdj(targetMaxAdj);
        }
    }

    private void updateMaxProcStateLocked(ProcessRecord app, int targetMaxProcState, boolean protect) {
        if (protect && IProcessPolicy.getAppMaxProcState(app) > targetMaxProcState) {
            IProcessPolicy.setAppMaxProcState(app, targetMaxProcState);
        } else if (!protect && IProcessPolicy.getAppMaxProcState(app) < targetMaxProcState) {
            IProcessPolicy.setAppMaxProcState(app, targetMaxProcState);
        }
    }

    public List<ActiveUidRecord> getActiveUidRecordList(int flag) {
        List<ActiveUidRecord> records;
        synchronized (sLock) {
            records = new ArrayList<>();
            for (int i = sActiveUidList.size() - 1; i >= 0; i--) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                if ((r.flag & flag) != 0) {
                    records.add(r);
                }
            }
        }
        return records;
    }

    @Override // com.android.server.am.IProcessPolicy
    public List<Integer> getActiveUidList(int flag) {
        List<Integer> records;
        synchronized (sLock) {
            records = new ArrayList<>();
            for (int i = sActiveUidList.size() - 1; i >= 0; i--) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                if ((r.flag & flag) != 0) {
                    records.add(Integer.valueOf(r.uid));
                }
            }
        }
        return records;
    }

    public int getPolicyFlags(int policy) {
        if (sPolicyFlagsList.contains(policy)) {
            return sPolicyFlagsList.get(policy).intValue();
        }
        return -1;
    }

    public void noteAudioOnLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord temp = sTempInactiveAudioList.get(uid);
                if (temp != null) {
                    this.mActiveUpdateHandler.removeMessages(1, temp);
                    sTempInactiveAudioList.remove(uid);
                    Slog.d("ProcessManager", "remove temp audio active uid : " + uid);
                } else {
                    ActiveUidRecord r = sActiveUidList.get(uid);
                    if (r == null) {
                        r = new ActiveUidRecord(uid);
                    }
                    r.flag = 1 | r.flag;
                    sActiveUidList.put(uid, r);
                    Slog.d("ProcessManager", "add audio active uid : " + uid);
                }
            }
        }
    }

    public void noteAudioOffLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord r = sActiveUidList.get(uid);
                if (r != null) {
                    sTempInactiveAudioList.put(uid, r);
                    Message msg = this.mActiveUpdateHandler.obtainMessage(1, r);
                    this.mActiveUpdateHandler.sendMessageDelayed(msg, UPDATE_AUDIO_OFF_DELAY);
                    Slog.d("ProcessManager", "add temp remove audio inactive uid : " + uid);
                }
            }
        }
    }

    public void noteResetAudioLocked() {
        synchronized (sLock) {
            List<ActiveUidRecord> removed = new ArrayList<>();
            int N = sActiveUidList.size();
            for (int i = 0; i < N; i++) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                r.flag &= -2;
                if (r.flag == 0) {
                    removed.add(r);
                }
            }
            Iterator<ActiveUidRecord> it = removed.iterator();
            while (it.hasNext()) {
                sActiveUidList.remove(it.next().uid);
            }
            Slog.d("ProcessManager", " noteResetAudioLocked removed ActiveUids : " + Arrays.toString(removed.toArray()));
        }
    }

    public void noteStartGpsLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord temp = sTempInactiveGPSList.get(uid);
                if (temp != null) {
                    this.mActiveUpdateHandler.removeMessages(2, temp);
                    sTempInactiveGPSList.remove(uid);
                    Slog.d("ProcessManager", "remove temp gps active uid : " + uid);
                } else {
                    ActiveUidRecord r = sActiveUidList.get(uid);
                    if (r == null) {
                        r = new ActiveUidRecord(uid);
                    }
                    r.flag = 2 | r.flag;
                    sActiveUidList.put(uid, r);
                    Slog.d("ProcessManager", "add gps active uid : " + uid);
                }
            }
        }
    }

    public void noteStopGpsLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord r = sActiveUidList.get(uid);
                if (r != null) {
                    sTempInactiveGPSList.put(uid, r);
                    Message msg = this.mActiveUpdateHandler.obtainMessage(2, r);
                    this.mActiveUpdateHandler.sendMessageDelayed(msg, 1000L);
                    Slog.d("ProcessManager", "add temp remove gps inactive uid : " + uid);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.println("Process Policy:");
        if (sDynamicWhiteList.size() > 0) {
            pw.println("DY:");
            Set<String> dynamic = sDynamicWhiteList.keySet();
            for (String pkg : dynamic) {
                pw.print(prefix);
                pw.println(pkg + " : " + sDynamicWhiteList.get(pkg));
            }
        }
        if (sCloudWhiteList.size() > 0) {
            pw.println("CL:");
            for (int i = 0; i < sCloudWhiteList.size(); i++) {
                pw.print(prefix);
                pw.println(sCloudWhiteList.get(i));
            }
        }
        if (sLockedApplicationList.size() > 0) {
            pw.println("LO:");
            Set<Integer> userIds = sLockedApplicationList.keySet();
            for (Integer userId : userIds) {
                Set<String> lockedApplication = sLockedApplicationList.get(userId);
                pw.print(prefix);
                pw.println("userId=" + userId);
                for (String app : lockedApplication) {
                    pw.print(prefix);
                    pw.println(app);
                }
            }
        }
        if (sFastBootAppMap.size() > 0) {
            pw.println("FA:");
            for (String protectedPackage : sFastBootAppMap.keySet()) {
                pw.print(prefix);
                pw.println(protectedPackage);
            }
        }
        if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
            pw.println("EP Activated: true");
            if (sEnterpriseAppList.size() > 0) {
                for (int i2 = 0; i2 < sEnterpriseAppList.size(); i2++) {
                    pw.print(prefix);
                    pw.println(sEnterpriseAppList.get(i2));
                }
            }
        }
        if (sSecretlyProtectAppList.size() > 0) {
            pw.println("SPAL:");
            for (int i3 = 0; i3 < sSecretlyProtectAppList.size(); i3++) {
                pw.print(prefix);
                pw.println(sSecretlyProtectAppList.get(i3));
            }
        }
        if (sActiveUidList.size() > 0) {
            pw.println("ACU:");
            for (int i4 = 0; i4 < sActiveUidList.size(); i4++) {
                pw.print(prefix);
                pw.println(sActiveUidList.valueAt(i4));
            }
        }
        OomAdjusterImpl.getInstance().dumpPreviousBackgroundApps(pw, prefix);
        OomAdjusterImpl.getInstance().dumpPreviousResidentApps(pw, prefix);
    }
}
