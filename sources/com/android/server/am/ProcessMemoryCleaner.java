package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ProcessMemoryCleaner extends ProcessCleanerBase {
    private static final int AUDIO_PROC_PAUSE_PROTECT_TIME = 300000;
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    private static final double COMM_USED_PSS_LIMIT_HIGH_FACTOR = 1.33d;
    private static final double COMM_USED_PSS_LIMIT_LITE_FACTOR = 0.67d;
    private static final double COMM_USED_PSS_LIMIT_USUAL_FACTOR = 1.0d;
    private static final int DEF_MIN_KILL_PROC_ADJ = 200;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    private static final String KILLING_PROC_REASON = "MiuiMemoryService";
    private static final int KILL_PROC_COUNT_LIMIT = 10;
    private static final double MEM_EXCEPTION_TH_HIGHH_FACTOR = 2.0d;
    private static final double MEM_EXCEPTION_TH_HIGHL_FACTOR = 1.25d;
    private static final double MEM_EXCEPTION_TH_HIGHM_FACTOR = 1.5d;
    private static final double MEM_EXCEPTION_TH_LITE_FACTOR = 0.625d;
    private static final double MEM_EXCEPTION_TH_MID_FACTOR = 0.75d;
    private static final double MEM_EXCEPTION_TH_USUAL_FACTOR = 1.0d;
    private static final int MSG_APP_SWITCH_BG_EXCEPTION = 1;
    private static final int MSG_PAD_SMALL_WINDOW_CLEAN = 3;
    private static final int MSG_PROCESS_BG_COMPACT = 4;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 2;
    private static final long PAD_SMALL_WINDOW_CLEAN_TIME = 5000;
    private static final String PERF_PROC_THRESHOLD_CLOUD_KEY = "perf_proc_threshold";
    private static final String PERF_PROC_THRESHOLD_CLOUD_MOUDLE = "perf_process";
    private static final int PROCESS_PRIORITY_FACTOR = 1000;
    private static final int PROC_BG_COMPACT_DELAY_TIME = 3000;
    private static final String PROC_COMM_PSS_LIMIT_CLOUD = "perf_proc_common_pss_limit";
    private static final String PROC_MEM_EXCEPTION_THRESHOLD_CLOUD = "perf_proc_mem_exception_threshold";
    private static final String PROC_PROTECT_LIST_CLOUD = "perf_proc_protect_list";
    private static final String PROC_SWITCH_BG_DELAY_TIME_CLOUD = "perf_proc_switch_Bg_time";
    private static final long RAM_SIZE_1GB = 1073741824;
    private static final String REASON_PAD_SMALL_WINDOW_CLEAN = "PadSmallWindowClean";
    private static final String SUB_PROCESS_ADJ_BIND_LIST_CLOUD = "perf_proc_adj_bind_list";
    public static final String TAG = "ProcessMemoryCleaner";
    private ActivityManagerService mAMS;
    private int mAppSwitchBgExceptDelayTime;
    private long mCommonUsedPssLimitKB;
    private Context mContext;
    private String mForegroundPkg;
    private int mForegroundUid;
    private H mHandler;
    private HandlerThread mHandlerTh;
    private long mLastPadSmallWindowUpdateTime;
    private long mMemExceptionThresholdKB;
    private MiuiMemoryServiceInternal mMiuiMemoryService;
    private ProcessManagerService mPMS;
    private ProcessPolicy mProcessPolicy;
    private ProcMemCleanerStatistics mStatistics;
    public static final boolean DEBUG = PressureStateSettings.DEBUG_ALL;
    private static final long TOTAL_MEMEORY_GB = Process.getTotalMemory() / 1073741824;

    public ProcessMemoryCleaner(ActivityManagerService ams) {
        super(ams);
        this.mAppSwitchBgExceptDelayTime = 30000;
        this.mCommonUsedPssLimitKB = PressureStateSettings.PROC_COMMON_USED_PSS_LIMIT_KB;
        this.mHandlerTh = new HandlerThread("ProcessMemoryCleanerTh", -2);
        this.mMemExceptionThresholdKB = PressureStateSettings.PROC_MEM_EXCEPTION_PSS_LIMIT_KB;
        this.mForegroundPkg = "";
        this.mForegroundUid = -1;
        this.mLastPadSmallWindowUpdateTime = 0L;
        this.mAMS = ams;
    }

    @Override // com.android.server.am.ProcessCleanerBase
    public void systemReady(Context context, ProcessManagerService pms) {
        super.systemReady(context, pms);
        if (DEBUG) {
            Slog.d(TAG, "ProcessesCleaner init");
        }
        this.mContext = context;
        this.mPMS = pms;
        this.mHandlerTh.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
        this.mProcessPolicy = this.mPMS.getProcessPolicy();
        this.mMiuiMemoryService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
        this.mStatistics = ProcMemCleanerStatistics.getInstance();
        computeMemExceptionThreshold(PressureStateSettings.PROC_MEM_EXCEPTION_PSS_LIMIT_KB);
        computeCommonUsedAppPssThreshold(this.mCommonUsedPssLimitKB);
    }

    public void onBootPhase() {
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessage(msg);
    }

    public static int getProcPriority(IAppState.IRunningProcess runningProc) {
        return (runningProc.getAdj() * 1000) + runningProc.getPriorityScore();
    }

    public boolean scanProcessAndCleanUpMemory(long targetReleaseMem) {
        if (this.mContext == null) {
            return false;
        }
        if (DEBUG) {
            Slog.d(TAG, "Start clean up memory.....");
        }
        List<IAppState.IRunningProcess> runningProcList = new ArrayList<>();
        ArrayList<IAppState> appStateList = this.mSmartPowerService.getAllAppState();
        Iterator<IAppState> it = appStateList.iterator();
        while (it.hasNext()) {
            IAppState appState = it.next();
            if (!appState.isVsible()) {
                Iterator it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    IAppState.IRunningProcess runningProc = (IAppState.IRunningProcess) it2.next();
                    if (runningProc.getAdj() > 0 && (runningProc.getAdj() < DEF_MIN_KILL_PROC_ADJ || runningProc.getAdj() > 250)) {
                        if (!isIsolatedByAdj(runningProc) && !isImportantSubProc(runningProc.getPackageName(), runningProc.getProcessName()) && !runningProc.isProcessPerceptible() && !runningProc.hasForegrundService() && !isSystemHighPrioProc(runningProc) && !isLastMusicPlayProcess(runningProc.getPid()) && !containInWhiteList(runningProc) && !isAutoStartProcess(appState, runningProc) && !this.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, runningProc.getPackageName(), runningProc.getProcessName())) {
                            runningProcList.add(runningProc);
                        }
                    }
                }
            }
        }
        return cleanUpMemory(runningProcList, targetReleaseMem);
    }

    private boolean isAutoStartProcess(IAppState appState, IAppState.IRunningProcess runningProc) {
        return appState.isAutoStartApp() && runningProc.getAdj() <= 800;
    }

    public void onApplyOomAdjLocked(ProcessRecord app) {
        if (app.mState.getSetAdj() <= 0) {
            this.mMiuiMemoryService.interruptProcCompaction(app.mPid);
        }
    }

    private boolean isIsolatedByAdj(IAppState.IRunningProcess runningProc) {
        return isIsolatedProcess(runningProc);
    }

    private boolean isIsolatedProcess(IAppState.IRunningProcess runningProc) {
        return Process.isIsolated(runningProc.getUid()) || runningProc.getProcessName().startsWith(new StringBuilder().append(runningProc.getPackageName()).append(":sandboxed_").toString());
    }

    private boolean isImportantSubProc(String pkgName, String procName) {
        return OomAdjusterImpl.getInstance().isImportantSubProc(pkgName, procName);
    }

    private boolean isSystemHighPrioProc(IAppState.IRunningProcess runningProc) {
        if (runningProc.getAdj() <= DEF_MIN_KILL_PROC_ADJ && runningProc.isSystemApp()) {
            return true;
        }
        return false;
    }

    private boolean isLastMusicPlayProcess(int pid) {
        long lastMusicPlayTime = this.mSmartPowerService.getLastMusicPlayTimeStamp(pid);
        if (SystemClock.uptimeMillis() - lastMusicPlayTime <= 300000) {
            return true;
        }
        return false;
    }

    private boolean cleanUpMemory(List<IAppState.IRunningProcess> runningProcList, long targetReleaseMem) {
        Collections.sort(runningProcList, new Comparator<IAppState.IRunningProcess>() { // from class: com.android.server.am.ProcessMemoryCleaner.1
            @Override // java.util.Comparator
            public int compare(IAppState.IRunningProcess o1, IAppState.IRunningProcess o2) {
                boolean isHav1 = o1.hasActivity();
                boolean isHav2 = o2.hasActivity();
                if ((isHav1 && isHav2) || (!isHav1 && !isHav2)) {
                    return ProcessMemoryCleaner.getProcPriority(o2) - ProcessMemoryCleaner.getProcPriority(o1);
                }
                if (isHav1) {
                    return 1;
                }
                return -1;
            }
        });
        long nowTime = System.currentTimeMillis();
        this.mStatistics.checkLastKillTime(nowTime);
        if (DEBUG) {
            debugAppGroupToString(runningProcList);
        }
        long releasePssByProcClean = 0;
        int killProcCount = 0;
        for (IAppState.IRunningProcess runningProc : runningProcList) {
            if (!this.mStatistics.isLastKillProcess(runningProc.getProcessName(), runningProc.getUid(), nowTime) && (runningProc.getAdj() < 900 || isIsolatedProcess(runningProc))) {
                if (runningProc.hasActivity()) {
                    return true;
                }
                long pss = killProcess(runningProc, 0, ProcMemCleanerStatistics.REASON_CLEAN_UP_MEM);
                if (targetReleaseMem <= 0) {
                    continue;
                } else {
                    if (PressureStateSettings.ONLY_KILL_ONE_PKG && runningProc.hasActivity()) {
                        if (DEBUG) {
                            Slog.d(TAG, "----skip kill: " + processToString(runningProc));
                        }
                        return true;
                    }
                    releasePssByProcClean += pss;
                    if (releasePssByProcClean >= targetReleaseMem || (killProcCount = killProcCount + 1) >= 10) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void compactProcess(IAppState.IRunningProcess processInfo) {
        this.mMiuiMemoryService.runProcCompaction(processInfo, 4);
    }

    private boolean containInWhiteList(IAppState.IRunningProcess proc) {
        return this.mPMS.isInWhiteList(proc.getProcessRecord(), 0, 21) || ProcessManagerInternal.checkCtsProcess(proc.getProcessName());
    }

    private List<IAppState.IRunningProcess> getProcessGroup(String packageName, int uid) {
        ArrayList<IAppState.IRunningProcess> procs = new ArrayList<>();
        synchronized (this.mAMS) {
            ProcessList procList = this.mAMS.mProcessList;
            int NP = procList.getProcessNamesLOSP().getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) procList.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    boolean isDep = app.getPkgDeps() != null && app.getPkgDeps().contains(packageName);
                    if (UserHandle.getAppId(app.uid) == UserHandle.getAppId(uid) && (app.getPkgList().containsKey(packageName) || isDep)) {
                        procs.add(this.mSmartPowerService.getRunningProcess(app.uid, app.processName));
                    }
                }
            }
        }
        return procs;
    }

    private boolean checkRunningProcess(IAppState.IRunningProcess runningProc, int minAdj) {
        return (runningProc == null || checkProcessDied(runningProc.getProcessRecord()) || runningProc.getAdj() <= minAdj) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.ProcessMemoryCleaner.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri())) {
                    ProcessMemoryCleaner.this.updateCloudControlData();
                }
            }
        };
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudControlData() {
        List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), PERF_PROC_THRESHOLD_CLOUD_MOUDLE);
        if (cloudDataList == null) {
            return;
        }
        String data = "";
        for (int i = 0; i < cloudDataList.size(); i++) {
            String cd = cloudDataList.get(i).getString(PERF_PROC_THRESHOLD_CLOUD_KEY, "");
            if (!TextUtils.isEmpty(cd)) {
                data = cd;
                break;
            }
        }
        try {
            JSONObject jsonObject = new JSONObject(data);
            updateAppSwitchBgDelayTime(jsonObject);
            updateCommonUsedPssLimitKB(jsonObject);
            updateMemExceptionThresholdKB(jsonObject);
            updateProtectProcessList(jsonObject);
            updateProcessAdjBindList(jsonObject);
        } catch (JSONException e) {
            Slog.e(TAG, "updateCloudData error :", e);
        }
    }

    private void updateProcessAdjBindList(JSONObject jsonObject) {
        String procString = jsonObject.optString(SUB_PROCESS_ADJ_BIND_LIST_CLOUD);
        OomAdjusterImpl.getInstance().updateProcessAdjBindList(procString);
        if (DEBUG) {
            logCloudControlParas(SUB_PROCESS_ADJ_BIND_LIST_CLOUD, procString);
        }
    }

    private void updateAppSwitchBgDelayTime(JSONObject jsonObject) {
        String SwitchBgTime = jsonObject.optString(PROC_SWITCH_BG_DELAY_TIME_CLOUD);
        if (!TextUtils.isEmpty(SwitchBgTime)) {
            this.mAppSwitchBgExceptDelayTime = Integer.parseInt(SwitchBgTime);
            if (DEBUG) {
                logCloudControlParas(PROC_SWITCH_BG_DELAY_TIME_CLOUD, SwitchBgTime);
            }
        }
    }

    private void updateCommonUsedPssLimitKB(JSONObject jsonObject) {
        String pssLimitKB = jsonObject.optString(PROC_COMM_PSS_LIMIT_CLOUD);
        if (!TextUtils.isEmpty(pssLimitKB)) {
            computeCommonUsedAppPssThreshold(Long.parseLong(pssLimitKB));
            if (DEBUG) {
                logCloudControlParas(PROC_COMM_PSS_LIMIT_CLOUD, pssLimitKB);
            }
        }
    }

    private void updateMemExceptionThresholdKB(JSONObject jsonObject) {
        String memExceptionThresholdKB = jsonObject.optString(PROC_MEM_EXCEPTION_THRESHOLD_CLOUD);
        if (!TextUtils.isEmpty(memExceptionThresholdKB)) {
            computeMemExceptionThreshold(Long.parseLong(memExceptionThresholdKB));
            if (DEBUG) {
                logCloudControlParas(PROC_MEM_EXCEPTION_THRESHOLD_CLOUD, memExceptionThresholdKB);
            }
        }
    }

    private void updateProtectProcessList(JSONObject jsonObject) {
        String processString = jsonObject.optString(PROC_PROTECT_LIST_CLOUD);
        if (!TextUtils.isEmpty(processString)) {
            String[] processArray = processString.split(",");
            for (String processName : processArray) {
                this.mProcessPolicy.updateSystemCleanWhiteList(processName);
            }
            if (DEBUG) {
                logCloudControlParas(PROC_PROTECT_LIST_CLOUD, processString);
            }
        }
    }

    private void logCloudControlParas(String key, String data) {
        Slog.d(TAG, "sync cloud control " + key + " " + data);
    }

    public long killPackage(IAppState.IRunningProcess runningProc, String reason) {
        return killPackage(runningProc, DEF_MIN_KILL_PROC_ADJ, reason);
    }

    public long killPackage(IAppState.IRunningProcess proc, int minAdj, String reason) {
        if (proc.getProcessRecord() == null) {
            return 0L;
        }
        long appCurPss = 0;
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses(proc.getUid(), proc.getPackageName());
        IAppState.IRunningProcess mainProc = null;
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningProc = it.next();
            appCurPss += runningProc.getPss();
            if (!checkRunningProcess(runningProc, minAdj)) {
                return 0L;
            }
            if (runningProc.getPackageName().equals(runningProc.getProcessName())) {
                mainProc = runningProc;
            }
        }
        if (mainProc == null || !checkRunningProcess(mainProc, minAdj)) {
            return 0L;
        }
        forceStopPackage(mainProc.getPackageName(), mainProc.getUserId(), getKillReason(mainProc));
        this.mStatistics.reportEvent(2, mainProc, appCurPss, reason);
        return appCurPss;
    }

    private String getKillReason(IAppState.IRunningProcess proc) {
        return "MiuiMemoryService(" + proc.getAdjType() + ")";
    }

    public long killProcess(IAppState.IRunningProcess runningProc, String reason) {
        return killProcess(runningProc, DEF_MIN_KILL_PROC_ADJ, reason);
    }

    public long killProcess(IAppState.IRunningProcess runningProc, int minAdj, String reason) {
        if (isCurrentProcessInBackup(runningProc)) {
            return 0L;
        }
        synchronized (this.mAMS) {
            ProcessRecord proc = runningProc.getProcessRecord();
            if (!checkRunningProcess(runningProc, minAdj) || isRunningComponent(proc)) {
                return 0L;
            }
            killApplicationLock(proc, getKillReason(runningProc));
            this.mStatistics.reportEvent(1, runningProc, runningProc.getPss(), reason);
            return runningProc.getPss();
        }
    }

    private boolean isRunningComponent(ProcessRecord proc) {
        if (proc.mServices.numberOfExecutingServices() > 0 || proc.mReceivers.numberOfCurReceivers() > 0) {
            return true;
        }
        return false;
    }

    public void KillProcessForPadSmallWindowMode(String pkgName) {
        long nowTime = SystemClock.uptimeMillis();
        if (this.mHandler.hasMessages(3) || nowTime - this.mLastPadSmallWindowUpdateTime < PAD_SMALL_WINDOW_CLEAN_TIME) {
            return;
        }
        this.mLastPadSmallWindowUpdateTime = nowTime;
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = pkgName;
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void killProcessByMinAdj(int minAdj, String reason, List<String> whiteList) {
        if (minAdj <= 300) {
            minAdj = MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
        }
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses();
        List<IAppState.IRunningProcess> canForcePkg = new ArrayList<>();
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningApp = it.next();
            if (!whiteList.contains(runningApp.getPackageName()) && checkRunningProcess(runningApp, minAdj)) {
                if (runningApp.getProcessName().equals(runningApp.getPackageName())) {
                    canForcePkg.add(runningApp);
                } else {
                    synchronized (this.mAMS) {
                        killApplicationLock(runningApp.getProcessRecord(), reason);
                    }
                }
            }
        }
        for (IAppState.IRunningProcess runningProc : canForcePkg) {
            if (SystemPressureController.getInstance().isForceStopEnable(runningProc.getProcessRecord(), 0)) {
                forceStopPackage(runningProc.getPackageName(), runningProc.getUserId(), reason);
            } else {
                synchronized (this.mAMS) {
                    killApplicationLock(runningProc.getProcessRecord(), reason);
                }
            }
        }
    }

    public ProcMemCleanerStatistics getProcMemStat() {
        return this.mStatistics;
    }

    private void debugAppGroupToString(List<IAppState.IRunningProcess> runningProcList) {
        for (IAppState.IRunningProcess runningProc : runningProcList) {
            String deg = processToString(runningProc);
            Slog.d(TAG, "ProcessInfo: " + deg);
        }
    }

    private String processToString(IAppState.IRunningProcess runningProc) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("pck=");
        sb.append(runningProc.getPackageName());
        sb.append(", prcName=");
        sb.append(runningProc.getProcessName());
        sb.append(", priority=");
        sb.append(getProcPriority(runningProc));
        sb.append(", hasAct=");
        sb.append(runningProc.hasActivity());
        sb.append(", pss=");
        sb.append(runningProc.getPss());
        return sb.toString();
    }

    private boolean isLaunchCameraForThirdApp(ControllerActivityInfo info) {
        if (info.fromPkg != null && info.launchPkg.equals("com.android.camera") && !isSystemApp(info.formUid)) {
            return true;
        }
        return false;
    }

    private boolean isSystemApp(int uid) {
        IAppState appState = this.mSmartPowerService.getAppState(uid);
        if (appState != null) {
            return appState.isSystemApp();
        }
        return false;
    }

    public void foregroundActivityChanged(ControllerActivityInfo info) {
        if (this.mHandler == null) {
            return;
        }
        this.mMiuiMemoryService.interruptProcCompaction(info.launchPid);
        sendAppSwitchBgExceptionMsg(info);
        sendGlobalCompactMsg(info);
        this.mForegroundPkg = info.launchPkg;
        this.mForegroundUid = info.launchUid;
    }

    public void compactBackgroundProcess(int uid, String processName) {
        IAppState.IRunningProcess proc = this.mSmartPowerService.getRunningProcess(uid, processName);
        compactBackgroundProcess(proc, false);
    }

    public void compactBackgroundProcess(IAppState.IRunningProcess proc, boolean isDelayed) {
        if (isNeedCompact(proc)) {
            if (!isDelayed) {
                this.mHandler.removeMessages(4, proc);
                Message msg = this.mHandler.obtainMessage(4, proc);
                msg.obj = proc;
                this.mHandler.sendMessage(msg);
                return;
            }
            if (!this.mHandler.hasMessages(4, proc)) {
                Message msg2 = this.mHandler.obtainMessage(4, proc);
                msg2.obj = proc;
                this.mHandler.sendMessageDelayed(msg2, 3000L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkBackgroundProcCompact(IAppState.IRunningProcess proc) {
        if (proc.getCurrentState() >= 4 && checkRunningProcess(proc, 100) && isNeedCompact(proc) && this.mMiuiMemoryService.isCompactNeeded(proc, 4)) {
            if (DEBUG) {
                Slog.d(TAG, "Compact memory: " + proc + " pss:" + proc.getPss() + " swap:" + proc.getSwapPss());
            }
            compactProcess(proc);
        }
    }

    private boolean isNeedCompact(IAppState.IRunningProcess proc) {
        return proc != null && proc.getUid() > 1000 && (proc.hasActivity() || isInWhiteListLock(proc.getProcessRecord(), proc.getUserId(), 2, this.mPMS)) && proc.getPss() - proc.getSwapPss() >= MiuiMemReclaimer.ANON_RSS_LIMIT_KB;
    }

    private void sendAppSwitchBgExceptionMsg(ControllerActivityInfo info) {
        if (this.mHandler.hasEqualMessages(1, info.launchPkg)) {
            this.mHandler.removeMessages(1, info.launchPkg);
        }
        ProcessPolicy processPolicy = this.mProcessPolicy;
        List<String> packageWhiteList = processPolicy.getWhiteList(processPolicy.getPolicyFlags(21));
        if (!packageWhiteList.contains(this.mForegroundPkg) && !this.mProcessPolicy.isInExtraPackageList(this.mForegroundPkg) && !isLaunchCameraForThirdApp(info) && !SystemPressureController.getInstance().isGameApp(this.mForegroundPkg)) {
            Message msg = this.mHandler.obtainMessage(1, this.mForegroundPkg);
            msg.obj = this.mForegroundPkg;
            msg.arg1 = this.mForegroundUid;
            this.mHandler.sendMessageDelayed(msg, this.mAppSwitchBgExceptDelayTime);
        }
    }

    private void sendGlobalCompactMsg(ControllerActivityInfo info) {
        MiuiMemoryServiceInternal miuiMemoryServiceInternal;
        if (info.launchPkg.equals("com.miui.home") && (miuiMemoryServiceInternal = this.mMiuiMemoryService) != null) {
            miuiMemoryServiceInternal.runGlobalCompaction(1);
        }
    }

    private void computeMemExceptionThreshold(long threshold) {
        long j = TOTAL_MEMEORY_GB;
        if (j < 5) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_LITE_FACTOR);
            return;
        }
        if (j <= 6) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_MID_FACTOR);
            return;
        }
        if (j <= 8) {
            this.mMemExceptionThresholdKB = (long) (threshold * 1.0d);
            return;
        }
        if (j <= 12) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_HIGHL_FACTOR);
        } else if (j <= 16) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_HIGHM_FACTOR);
        } else {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_HIGHH_FACTOR);
        }
    }

    private void computeCommonUsedAppPssThreshold(long threshold) {
        long j = TOTAL_MEMEORY_GB;
        if (j <= 6) {
            this.mCommonUsedPssLimitKB = (long) (threshold * COMM_USED_PSS_LIMIT_LITE_FACTOR);
        } else if (j <= 8) {
            this.mCommonUsedPssLimitKB = (long) (threshold * 1.0d);
        } else {
            this.mCommonUsedPssLimitKB = (long) (threshold * COMM_USED_PSS_LIMIT_HIGH_FACTOR);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int checkBackgroundAppException(String packageName, int uid) {
        int killType = 0;
        if (SystemPressureController.getInstance().isGameApp(packageName)) {
            return 0;
        }
        long mainPss = 0;
        long subPss = 0;
        long subPssWithActivity = 0;
        IAppState.IRunningProcess mainProc = null;
        List<IAppState.IRunningProcess> procGroup = getProcessGroup(packageName, uid);
        for (IAppState.IRunningProcess proc : procGroup) {
            proc.updatePss();
            if (proc.getPackageName().equals(proc.getProcessName())) {
                mainProc = proc;
                mainPss = proc.getPss();
            } else if (proc.hasActivity()) {
                subPssWithActivity += proc.getPss();
            } else {
                subPss += proc.getPss();
            }
        }
        if (mainProc == null) {
            return 0;
        }
        if (ProcMemCleanerStatistics.isCommonUsedApp(mainProc.getPackageName())) {
            if (mainPss >= this.mCommonUsedPssLimitKB) {
                killPackage(mainProc, ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
                killType = 2;
                if (DEBUG) {
                    Slog.d(TAG, "common used app takes up too much memory: " + mainProc.getPackageName());
                }
            }
            return killType;
        }
        long j = mainPss + subPss + subPssWithActivity;
        long subPssWithActivity2 = this.mMemExceptionThresholdKB;
        if (j > subPssWithActivity2) {
            killPackage(this.mSmartPowerService.getRunningProcess(mainProc.getUid(), mainProc.getProcessName()), ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
            killType = 2;
            if (DEBUG) {
                Slog.d(TAG, "app takes up too much memory: " + mainProc.getPackageName() + ". pss:" + mainPss + " sub:" + subPss);
            }
        } else if (subPss >= SmartPowerSettings.PROC_MEM_LVL1_PSS_LIMIT_KB) {
            for (IAppState.IRunningProcess proc2 : procGroup) {
                if (!proc2.getPackageName().equals(proc2.getProcessName()) && !proc2.hasActivity()) {
                    killProcess(this.mSmartPowerService.getRunningProcess(proc2.getUid(), proc2.getProcessName()), ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
                }
            }
            killType = 1;
            if (DEBUG) {
                Slog.d(TAG, "subprocess takes up too much memory: " + mainProc.getPackageName() + ". sub:" + subPss);
            }
        }
        return killType;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private List<String> mPadSmallWindowWhiteList;

        public H(Looper looper) {
            super(looper);
            this.mPadSmallWindowWhiteList = new ArrayList();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    try {
                        ProcessMemoryCleaner.this.checkBackgroundAppException((String) msg.obj, msg.arg1);
                        return;
                    } catch (Exception e) {
                        Slog.d(ProcessMemoryCleaner.TAG, "checkBackgroundAppException failed: " + e.toString());
                        return;
                    }
                case 2:
                    ProcessMemoryCleaner processMemoryCleaner = ProcessMemoryCleaner.this;
                    processMemoryCleaner.registerCloudObserver(processMemoryCleaner.mContext);
                    ProcessMemoryCleaner.this.updateCloudControlData();
                    return;
                case 3:
                    this.mPadSmallWindowWhiteList.add((String) msg.obj);
                    ProcessMemoryCleaner.this.killProcessByMinAdj(799, ProcessMemoryCleaner.REASON_PAD_SMALL_WINDOW_CLEAN, this.mPadSmallWindowWhiteList);
                    this.mPadSmallWindowWhiteList.clear();
                    return;
                case 4:
                    try {
                        ProcessMemoryCleaner.this.checkBackgroundProcCompact((IAppState.IRunningProcess) msg.obj);
                        return;
                    } catch (Exception e2) {
                        Slog.d(ProcessMemoryCleaner.TAG, "checkBackgroundProcCompact failed: " + e2.toString());
                        return;
                    }
                default:
                    return;
            }
        }
    }
}
