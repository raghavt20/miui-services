package com.android.server.am;

import android.content.Context;
import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.spc.PressureStateSettings;
import android.util.Slog;
import com.android.internal.app.IPerfShielder;
import com.android.server.LocalServices;
import com.android.server.am.MiProcessTracker;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.daemon.performance.PerfShielderManager;
import com.miui.server.smartpower.IAppState;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
public class MiProcessTracker {
    private static final int COMPACTION = 8;
    private static final int CONT_LOW_WMARK = 3;
    private static final int CRITICAL_KILL = 1;
    private static final int DIRECT_RECL_AND_LOW_MEM = 10;
    private static final int DIRECT_RECL_AND_THRASHING = 5;
    private static final int DIRECT_RECL_AND_THROT = 9;
    private static final String LMKD_COMPACTION = "lmkdCompaction";
    private static final String LMKD_CONT_LOW_WMARK = "lmkdContLowLwmark";
    private static final String LMKD_CRITICAL_KILL = "lmkdCriticalKill";
    private static final String LMKD_DIRECT_RECL_AND_LOW_MEM = "lmkdDirectReclAndLowMem";
    private static final String LMKD_DIRECT_RECL_AND_THRASHING = "lmkdDirectReclAndThrashing";
    private static final String LMKD_DIRECT_RECL_AND_THROT = "lmkdDirectReclAndThrot";
    private static final String LMKD_KILL_REASON_CAMERA = "cameraKill";
    private static final String LMKD_KILL_REASON_UNKNOWN = "lmkdUnknown";
    private static final String LMKD_LOW_FILECACHE_AFTER_THRASHING = "lmkdLowFileCacheAfterThrashing";
    private static final String LMKD_LOW_MEM_AND_SWAP = "lmkdLowMemAndSwap";
    private static final String LMKD_LOW_MEM_AND_SWAP_UTIL = "lmkdLowMemAndSwapUtil";
    private static final String LMKD_LOW_MEM_AND_THRASHING = "lmkdLowMemAndThrashing";
    private static final String LMKD_LOW_SWAP_AND_LOW_FILE = "lmkdLowSwapAndLowFile";
    private static final String LMKD_LOW_SWAP_AND_THRASHING = "lmkdLowSwapAndThrashing";
    private static final String LMKD_PERCEPTIBLE_PROTECT = "lmkdPerceptibleProtect";
    private static final String LMKD_PRESSURE_AFTER_KILL = "lmkdPressureAfterKill";
    private static final String LMKD_WILL_THROTTLED = "lmkdWillThrottled";
    private static final int LOW_FILECACHE_AFTER_THRASHING = 7;
    private static final int LOW_MEM_AND_SWAP = 3;
    private static final int LOW_MEM_AND_SWAP_UTIL = 6;
    private static final int LOW_MEM_AND_THRASHING = 4;
    private static final int LOW_SWAP_AND_LOW_FILE = 11;
    private static final int LOW_SWAP_AND_THRASHING = 2;
    private static final int MINFREE_MODE = 13;
    private static final int MI_NEW_KILL_REASON_BASE = 50;
    private static final int MI_UPDATE_KILL_REASON_BASE = 200;
    private static final int PERCEPTIBLE_PROTECT = 12;
    private static final int PRESSURE_AFTER_KILL = 0;
    public static final String TAG = "MiProcessTracker";
    private static final int WILL_THROTTLED = 4;
    public static MiProcessTracker sInstance;
    private IPerfShielder mPerfShielder;
    private SmartPowerServiceInternal mSmartPowerService;
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.process.tracker.enable", false);
    public static boolean sEnable = PressureStateSettings.PROCESS_TRACKER_ENABLE;
    public static HashSet<String> trackerPkgWhiteList = new HashSet<>();
    private boolean mSystemReady = false;
    private final AppStartRecordController mAppStartRecordController = new AppStartRecordController();
    private final ProcessRecordController mProcessRecordController = new ProcessRecordController();

    /* JADX WARN: Multi-variable type inference failed */
    private MiProcessTracker() {
    }

    public static MiProcessTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MiProcessTracker();
        }
        return sInstance;
    }

    public void systemReady(Context context) {
        if (!sEnable) {
            return;
        }
        this.mPerfShielder = PerfShielderManager.getService();
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        trackerPkgWhiteList.addAll(Arrays.asList(context.getResources().getStringArray(285409454)));
        trackerPkgWhiteList.addAll(Arrays.asList(context.getResources().getStringArray(285409468)));
        trackerPkgWhiteList.addAll(Arrays.asList(context.getResources().getStringArray(285409453)));
        trackerPkgWhiteList.addAll(Arrays.asList(context.getResources().getStringArray(285409467)));
        this.mSystemReady = true;
    }

    public void onActivityLaunched(String processName, String packageName, int uid, int launchState) {
        if (!this.mSystemReady) {
            return;
        }
        this.mAppStartRecordController.onActivityLaunched(processName, packageName, uid, launchState);
    }

    public void recordAmKillProcess(ProcessRecord app, String reason) {
        if (this.mSystemReady && app != null) {
            String processName = app.processName;
            String packageName = app.info.packageName;
            int uid = app.info.uid;
            int adj = app.mState.getSetAdj();
            long pss = app.mProfile.getLastPss();
            this.mProcessRecordController.recordKillProcessIfNeed(packageName, processName, uid, reason, adj, pss, 0L);
        }
    }

    public void recordLmkKillProcess(int uid, int oomScore, long rssInBytes, int freeMemKb, int freeSwapKb, int killReason, int thrashing, String processName) {
        IAppState.IRunningProcess runningProcess;
        if (this.mSystemReady && (runningProcess = this.mSmartPowerService.getRunningProcess(uid, processName)) != null) {
            String packageName = runningProcess.getPackageName();
            String reason = getLmkdKillReason(killReason);
            this.mProcessRecordController.recordKillProcessIfNeed(packageName, processName, uid, reason, oomScore, 0L, rssInBytes / FormatBytesUtil.KB);
        }
    }

    /* loaded from: classes.dex */
    class AppStartRecordController {
        private static final int APP_COLD_START_MODE = 1;
        private static final int APP_HOT_START_MODE = 2;
        private static final int APP_WARM_START_MODE = 3;
        private static final long INTERVAL_TIME = 3000;
        private String mForegroundPackageName;

        private AppStartRecordController() {
            this.mForegroundPackageName = "";
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onActivityLaunched(String processName, String packageName, int uid, int launchState) {
            IAppState app;
            if (launchState == 0 || this.mForegroundPackageName.equals(packageName)) {
                return;
            }
            this.mForegroundPackageName = packageName;
            if (uid == 1000 || !processName.equals(packageName) || (app = MiProcessTracker.this.mSmartPowerService.getAppState(uid)) == null || MiProcessTracker.trackerPkgWhiteList.contains(packageName)) {
                return;
            }
            switch (launchState) {
                case 1:
                    if (!MiProcessTracker.this.mProcessRecordController.isAppFirstStart(app)) {
                        reportAppStart(packageName, 1);
                        break;
                    }
                    break;
                case 2:
                    reportAppStart(packageName, 3);
                    break;
                case 3:
                    reportAppStart(packageName, 2);
                    break;
            }
            if (MiProcessTracker.DEBUG) {
                Slog.d(MiProcessTracker.TAG, "onActivityLaunched packageName = " + packageName + " launchState = " + launchState);
            }
        }

        private void reportAppStart(String packageName, int flag) {
            if (MiProcessTracker.this.mPerfShielder != null) {
                List<IAppState> residentAppList = (List) MiProcessTracker.this.mSmartPowerService.getAllAppState().stream().filter(new Predicate() { // from class: com.android.server.am.MiProcessTracker$AppStartRecordController$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return MiProcessTracker.AppStartRecordController.lambda$reportAppStart$0((IAppState) obj);
                    }
                }).collect(Collectors.toList());
                String[] residentPkgNameList = (String[]) ((List) residentAppList.stream().map(new Function() { // from class: com.android.server.am.MiProcessTracker$AppStartRecordController$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        String packageName2;
                        packageName2 = ((IAppState) obj).getPackageName();
                        return packageName2;
                    }
                }).collect(Collectors.toList())).toArray(new IntFunction() { // from class: com.android.server.am.MiProcessTracker$AppStartRecordController$$ExternalSyntheticLambda2
                    @Override // java.util.function.IntFunction
                    public final Object apply(int i) {
                        return MiProcessTracker.AppStartRecordController.lambda$reportAppStart$1(i);
                    }
                });
                if (MiProcessTracker.DEBUG) {
                    Slog.d(MiProcessTracker.TAG, "reportAppStart packageName = " + packageName + " flag = " + flag + " residentAppList size = " + residentAppList.size() + " residentAppList = " + Arrays.toString(residentPkgNameList));
                }
                try {
                    MiProcessTracker.this.mPerfShielder.reportApplicationStart(packageName, flag, residentAppList.size(), residentPkgNameList);
                } catch (RemoteException e) {
                    Slog.e(MiProcessTracker.TAG, "mPerfShielder is dead", e);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$reportAppStart$0(IAppState app) {
            return app.getUid() != 1000 && !MiProcessTracker.trackerPkgWhiteList.contains(app.getPackageName()) && app.getAppSwitchFgCount() > 0 && app.isAlive();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ String[] lambda$reportAppStart$1(int x$0) {
            return new String[x$0];
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ProcessRecordController {
        private static final long APP_MAX_SWITCH_BACKGROUND_TIME = 86400000;
        private HashMap<String, String> mLastTimeKillReason;
        private List<String> mRetentionFilterKillReasonList;

        private ProcessRecordController() {
            this.mLastTimeKillReason = new HashMap<>();
            this.mRetentionFilterKillReasonList = List.of(IProcessPolicy.REASON_ONE_KEY_CLEAN, IProcessPolicy.REASON_FORCE_CLEAN, IProcessPolicy.REASON_GARBAGE_CLEAN, IProcessPolicy.REASON_LOCK_SCREEN_CLEAN, IProcessPolicy.REASON_GAME_CLEAN, IProcessPolicy.REASON_OPTIMIZATION_CLEAN, IProcessPolicy.REASON_SWIPE_UP_CLEAN);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordKillProcessIfNeed(String packageName, String processName, int uid, String reason, int adj, long pss, long rss) {
            IAppState appState;
            long residentDuration;
            IAppState.IRunningProcess runningProcess = MiProcessTracker.this.mSmartPowerService.getRunningProcess(uid, processName);
            boolean hasActivities = runningProcess != null && runningProcess.hasActivity();
            boolean hasForegroundServices = runningProcess != null && runningProcess.hasForegrundService();
            if ((processName.equals(packageName) || hasActivities) && (appState = MiProcessTracker.this.mSmartPowerService.getAppState(uid)) != null) {
                long backgroundTime = appState.getLastTopTime();
                if (backgroundTime == 0) {
                    residentDuration = 0;
                } else {
                    long residentDuration2 = SystemClock.uptimeMillis() - backgroundTime;
                    residentDuration = residentDuration2;
                }
                long[] memInfo = new long[27];
                Debug.getMemInfo(memInfo);
                long memAvail = memInfo[19];
                if (MiProcessTracker.DEBUG) {
                    Slog.i(MiProcessTracker.TAG, "reportKillProcessIfNeed packageName = " + packageName + " processName = " + processName + " reason = " + reason + " adj:" + adj + " pss:" + pss + " rss:" + rss + " memAvail:" + memAvail + " residentDuration:" + residentDuration + " AppSwitchFgCount:" + appState.getAppSwitchFgCount() + " hasActivity:" + hasActivities + " hasForegroundService:" + hasForegroundServices);
                }
                if (!needFilterReason(reason) && (appState.getAppSwitchFgCount() > 0 || hasActivities || hasForegroundServices)) {
                    reportKillProcessEvent(processName, packageName, reason, adj, pss, rss, memAvail, residentDuration, hasActivities, hasForegroundServices);
                }
                this.mLastTimeKillReason.put(packageName, reason);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isAppFirstStart(IAppState appState) {
            if (!this.mLastTimeKillReason.containsKey(appState.getPackageName())) {
                return true;
            }
            long backgroundTime = SystemClock.uptimeMillis() - appState.getLastTopTime();
            if (backgroundTime > APP_MAX_SWITCH_BACKGROUND_TIME) {
                return true;
            }
            return needFilterReason(this.mLastTimeKillReason.get(appState.getPackageName()));
        }

        private boolean needFilterReason(String reason) {
            if (reason != null) {
                for (String filterReason : this.mRetentionFilterKillReasonList) {
                    if (reason.contains(filterReason)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private void reportKillProcessEvent(String processName, String packageName, String reason, int adj, long pss, long rss, long memAvail, long residentDuration, boolean hasActivities, boolean hasForegroundServices) {
            if (MiProcessTracker.this.mPerfShielder != null) {
                try {
                    MiProcessTracker.this.mPerfShielder.reportKillProcessEvent(processName, packageName, reason, adj, pss, rss, memAvail, residentDuration, hasActivities, hasForegroundServices);
                } catch (RemoteException e) {
                    Slog.e(MiProcessTracker.TAG, "mPerfShielder is dead", e);
                }
            }
        }
    }

    private String getLmkdKillReason(int reason) {
        if (reason > 250) {
            switch (reason - 250) {
                case 3:
                    return LMKD_CONT_LOW_WMARK;
                case 4:
                    return LMKD_WILL_THROTTLED;
                default:
                    return LMKD_KILL_REASON_UNKNOWN;
            }
        }
        if (reason > MI_UPDATE_KILL_REASON_BASE) {
            reason -= 200;
        }
        switch (reason) {
            case 0:
                return LMKD_PRESSURE_AFTER_KILL;
            case 1:
                return LMKD_CRITICAL_KILL;
            case 2:
                return LMKD_LOW_SWAP_AND_THRASHING;
            case 3:
                return LMKD_LOW_MEM_AND_SWAP;
            case 4:
                return LMKD_LOW_MEM_AND_THRASHING;
            case 5:
                return LMKD_DIRECT_RECL_AND_THRASHING;
            case 6:
                return LMKD_LOW_MEM_AND_SWAP_UTIL;
            case 7:
                return LMKD_LOW_FILECACHE_AFTER_THRASHING;
            case 8:
                return LMKD_COMPACTION;
            case 9:
                return LMKD_DIRECT_RECL_AND_THROT;
            case 10:
                return LMKD_DIRECT_RECL_AND_LOW_MEM;
            case 11:
                return LMKD_LOW_SWAP_AND_LOW_FILE;
            case 12:
                return LMKD_PERCEPTIBLE_PROTECT;
            case 13:
                return LMKD_KILL_REASON_CAMERA;
            default:
                return LMKD_KILL_REASON_UNKNOWN;
        }
    }
}
