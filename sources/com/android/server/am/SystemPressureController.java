package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.spc.MemoryCleanInfo;
import android.os.spc.PressureStateSettings;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.app.SpeedTestModeServiceInternal;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.smartpower.IAppState;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.process.RunningProcessInfo;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.am.SystemPressureControllerStub$$")
/* loaded from: classes.dex */
public final class SystemPressureController extends SystemPressureControllerStub {
    private static final boolean IS_ENABLE_RECLAIM;
    private static final int MSG_CLEAN_UP_MEMLVL_PROCESS = 1;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 3;
    private static final int MSG_RESET_STAERTING_APP_STATE = 2;
    private static final int MSG_UPDATE_SCREEN_STATE = 4;
    private static final boolean PAD_SMALL_WINDOW_CLEAN_ENABLE;
    private static final String PREF_SHIELDER_PROC = "perf_shielder_Proc";
    private static final String PREF_SHIELDER_PROC_CLOUD_MOUDLE = "perf_process";
    private static final String PROC_GAME_LIST_CLOUD = "perf_proc_game_List";
    private static final String PROC_GAME_OOM_CLOUD_ENABLE = "perf_game_oom_enable";
    private static final String RECLAIM_EVENT_NODE = "/sys/kernel/mi_reclaim/event";
    public static final int STAERT_APP_TIMEOUT = 2000;
    public static final String TAG = "SystemPressureControl";
    public static final int THREAD_GROUP_FOREGROUND = 1;
    private static final long TOTAL_MEMORY;
    private static List<String> mKillPkgPermList;
    private static List<String> mKillProcPermList;
    private static List<String> sGameAppList;
    private static SystemPressureController sInstance;
    private ActivityManagerService mAms;
    private Context mContext;
    private H mHandler;
    private long mLastProcessCleanTimeMillis;
    private ProcessManagerService mPms;
    ProcessPowerCleaner mPowerCleaner;
    ProcessMemoryCleaner mProcessCleaner;
    ProcessGameCleaner mProcessGameCleaner;
    ProcessSceneCleaner mSceneCleaner;
    private SmartPowerServiceInternal mSmartPowerService;
    private HandlerThread mHandlerTh = new HandlerThread("SystemPressureControlTh", -2);
    private HandlerThread mProcessCleanHandlerTh = new HandlerThread("ProcessCleanTh", -2);
    private String mForegroundPackageName = "";
    private boolean mGameOomEnable = true;
    private final ArrayList<ThermalTempListener> mThermalTempListeners = new ArrayList<>();
    private boolean mSystemReady = false;
    public Object mStartingAppLock = new Object();
    private boolean mIsStartingApp = false;

    private static native void nDumpCpuLimit(PrintWriter printWriter);

    private native void nEndPressureMonitor();

    private static native String nGetBackgroundCpuPolicy();

    private static native long nGetCpuUsage(int i, String str);

    private native void nInit();

    private native void nStartPressureMonitor();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SystemPressureController> {

        /* compiled from: SystemPressureController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SystemPressureController INSTANCE = new SystemPressureController();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SystemPressureController m690provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SystemPressureController m689provideNewInstance() {
            return new SystemPressureController();
        }
    }

    static {
        long totalMemory = Process.getTotalMemory() >> 30;
        TOTAL_MEMORY = totalMemory;
        sGameAppList = new ArrayList();
        ArrayList arrayList = new ArrayList();
        mKillProcPermList = arrayList;
        arrayList.add("com.miui.powerkeeper:ui");
        mKillProcPermList.add("com.miui.powerkeeper");
        mKillProcPermList.add("com.miui.cleanmaster");
        mKillProcPermList.add("com.miui.voiceassist");
        mKillProcPermList.add("system");
        ArrayList arrayList2 = new ArrayList();
        mKillPkgPermList = arrayList2;
        arrayList2.add("com.miui.home");
        mKillPkgPermList.add("com.mi.android.globallauncher");
        mKillPkgPermList.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        mKillPkgPermList.add("com.miui.cleaner");
        IS_ENABLE_RECLAIM = totalMemory < 8;
        PAD_SMALL_WINDOW_CLEAN_ENABLE = totalMemory < 6;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    SystemPressureController.this.cleanUpMemory(((Long) msg.obj).longValue());
                    return;
                case 2:
                    SystemPressureController.this.resetStartingAppState();
                    return;
                case 3:
                    SystemPressureController systemPressureController = SystemPressureController.this;
                    systemPressureController.registerCloudObserver(systemPressureController.mContext);
                    SystemPressureController.this.updateCloudControlData();
                    return;
                case 4:
                    SystemPressureController.this.updateScreenState(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    public static SystemPressureController getInstance() {
        return (SystemPressureController) MiuiStubUtil.getImpl(SystemPressureControllerStub.class);
    }

    public void init(Context context, ActivityManagerService ams) {
        this.mContext = context;
        this.mAms = ams;
        this.mHandlerTh.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
        this.mProcessCleanHandlerTh.start();
        Process.setThreadGroupAndCpuset(this.mProcessCleanHandlerTh.getThreadId(), 1);
        this.mProcessCleaner = new ProcessMemoryCleaner(this.mAms);
        this.mProcessGameCleaner = new ProcessGameCleaner(this.mAms);
        if (PressureStateSettings.DEBUG_ALL) {
            Slog.d(TAG, "init SystemPressureController");
        }
    }

    public void onSystemReady() {
        this.mPms = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        OomAdjusterImpl.getInstance().onSystemReady();
        this.mProcessCleaner.systemReady(this.mContext, this.mPms);
        this.mProcessGameCleaner.onSystemReady(this.mPms, this.mProcessCleanHandlerTh.getLooper());
        try {
            nInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        ProcessPowerCleaner processPowerCleaner = new ProcessPowerCleaner(this.mAms);
        this.mPowerCleaner = processPowerCleaner;
        processPowerCleaner.systemReady(this.mContext, this.mPms, this.mProcessCleanHandlerTh.getLooper());
        ProcessSceneCleaner processSceneCleaner = new ProcessSceneCleaner(this.mAms);
        this.mSceneCleaner = processSceneCleaner;
        processSceneCleaner.systemReady(this.mPms, this.mProcessCleanHandlerTh.getLooper(), this.mContext);
        nStartPressureMonitor();
        registerScreenStateReceiver();
        this.mSystemReady = true;
    }

    public void finishBooting() {
        Message msg = this.mHandler.obtainMessage(3);
        this.mHandler.sendMessage(msg);
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        pw.println("system pressure controller:");
        try {
            if (opti < args.length) {
                String parm = args[opti];
                if (parm.contains("cleanproc")) {
                    cleanUpMemory(PressureStateSettings.PROC_CLEAN_PSS_KB);
                    return;
                }
                if (parm.contains("configs")) {
                    dumpConfigs(pw);
                    return;
                }
                if (parm.contains("cleanrecord") && this.mSystemReady) {
                    this.mProcessCleaner.getProcMemStat().dumpKillInfo(pw);
                    return;
                }
                if (parm.contains("thermalcleanlevel1")) {
                    ProcessConfig config = new ProcessConfig(19);
                    powerKillProcess(config);
                    return;
                }
                if (parm.contains("thermalcleanlevel2")) {
                    ProcessConfig config2 = new ProcessConfig(20);
                    powerKillProcess(config2);
                    return;
                }
                if (parm.contains("lockoffclean")) {
                    ProcessPowerCleaner processPowerCleaner = this.mPowerCleaner;
                    if (processPowerCleaner != null) {
                        processPowerCleaner.setLockOffCleanTestEnable(true);
                        this.mPowerCleaner.handleAutoLockOff();
                        return;
                    }
                    return;
                }
                if (parm.startsWith("idlecleanapp")) {
                    String cleanString = parm.substring(parm.indexOf(d.h) + 1);
                    String[] cleanArray = cleanString.split(",");
                    for (int i = 0; i < cleanArray.length; i++) {
                        List<RunningProcessInfo> infoList = ProcessManager.getRunningProcessInfoByPackageName(cleanArray[i]);
                        if (infoList != null && !infoList.isEmpty()) {
                            int uid = infoList.get(0).mUid;
                            ProcessConfig config3 = new ProcessConfig(13, cleanArray[i], uid);
                            powerKillProcess(config3);
                        }
                    }
                    return;
                }
                if (parm.contains("systemabnormal")) {
                    ProcessConfig config4 = new ProcessConfig(16);
                    powerKillProcess(config4);
                    return;
                }
                if (parm.contains("forceclean")) {
                    ProcessConfig config5 = new ProcessConfig(2);
                    sceneKillProcess(config5);
                } else if (parm.startsWith("removePerm")) {
                    String name = parm.substring(parm.indexOf(d.h) + 1);
                    if (mKillPkgPermList.contains(name)) {
                        mKillPkgPermList.remove(name);
                    } else if (mKillProcPermList.contains(name)) {
                        mKillProcPermList.remove(name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getForegroundPackageName() {
        return this.mForegroundPackageName;
    }

    protected void dumpConfigs(PrintWriter pw) {
        pw.println("DEBUG_ALL=" + PressureStateSettings.DEBUG_ALL);
        pw.println("-----------start of proc cleaner configs-----------");
        pw.println("PROCESS_CLEANER_ENABLED = ");
        pw.println(PressureStateSettings.PROCESS_CLEANER_ENABLED);
        pw.println("PROC_CLEAN_MIN_INTERVAL_MS = ");
        pw.println(PressureStateSettings.PROC_CLEAN_MIN_INTERVAL_MS);
        pw.println("-----------end of proc cleaner configs-----------");
    }

    public void triggerProcessClean() {
        if (PressureStateSettings.PROCESS_CLEANER_ENABLED && !this.mHandler.hasMessages(1) && SystemClock.elapsedRealtime() - this.mLastProcessCleanTimeMillis > PressureStateSettings.PROC_CLEAN_MIN_INTERVAL_MS) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.obj = Long.valueOf(PressureStateSettings.PROC_CLEAN_PSS_KB);
            this.mHandler.sendMessage(msg);
            this.mLastProcessCleanTimeMillis = SystemClock.elapsedRealtime();
        }
    }

    public void cleanAllSubProcess() {
        if (PressureStateSettings.PROCESS_CLEANER_ENABLED && !this.mHandler.hasMessages(1) && SystemClock.elapsedRealtime() - this.mLastProcessCleanTimeMillis > PressureStateSettings.PROC_CLEAN_MIN_INTERVAL_MS) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.obj = 0L;
            this.mHandler.sendMessage(msg);
            this.mLastProcessCleanTimeMillis = SystemClock.elapsedRealtime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanUpMemory(long targetReleaseMem) {
        if (!this.mSystemReady) {
            return;
        }
        long[] meminfo = new long[27];
        Debug.getMemInfo(meminfo);
        MemoryCleanInfo cInfo = new MemoryCleanInfo();
        cInfo.time = SystemClock.elapsedRealtime();
        cInfo.beforeMemFree = meminfo[1];
        cInfo.beforeMemAvail = meminfo[3] + meminfo[1];
        cInfo.memTotal = meminfo[0];
        cInfo.swapTotal = meminfo[8];
        try {
            this.mProcessCleaner.scanProcessAndCleanUpMemory(targetReleaseMem);
        } catch (Exception e) {
            Slog.d(TAG, "scanProcessAndCleanUpMemory failed: " + e.toString());
        }
        Debug.getMemInfo(meminfo);
        cInfo.afterMemFree = meminfo[1];
        cInfo.afterMemAvail = meminfo[3] + meminfo[1];
        try {
            if (ProcessMemoryCleaner.DEBUG) {
                Slog.d(ProcessMemoryCleaner.TAG, "memory clean info: " + cInfo.toString());
            }
            EventLog.writeEvent(ProcMemCleanerStatistics.EVENT_TAGS, Long.valueOf(cInfo.memTotal), Long.valueOf(cInfo.swapTotal), Long.valueOf(cInfo.beforeMemFree), Long.valueOf(cInfo.beforeMemAvail), Long.valueOf(cInfo.afterMemFree), Long.valueOf(cInfo.afterMemAvail), Long.valueOf(targetReleaseMem));
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.SystemPressureController.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri())) {
                    SystemPressureController.this.updateCloudControlData();
                }
            }
        };
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudControlData() {
        List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), PREF_SHIELDER_PROC_CLOUD_MOUDLE);
        if (cloudDataList == null) {
            return;
        }
        String data = "";
        for (int i = 0; i < cloudDataList.size(); i++) {
            String cd = cloudDataList.get(i).getString(PREF_SHIELDER_PROC, "");
            if (!TextUtils.isEmpty(cd)) {
                data = cd;
                break;
            }
        }
        try {
            JSONObject jsonObject = new JSONObject(data);
            sGameAppList.clear();
            sGameAppList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409511)));
            String gameString = jsonObject.optString(PROC_GAME_LIST_CLOUD);
            if (!TextUtils.isEmpty(gameString)) {
                String[] appArray = gameString.split(",");
                for (String appPackageName : appArray) {
                    if (!sGameAppList.contains(appPackageName)) {
                        sGameAppList.add(appPackageName);
                        if (PressureStateSettings.DEBUG_ALL) {
                            Slog.d(TAG, "sGameList cloud set received: " + appPackageName);
                        }
                    }
                }
            }
            String gameOomString = jsonObject.optString(PROC_GAME_OOM_CLOUD_ENABLE);
            if (!TextUtils.isEmpty(gameOomString)) {
                this.mGameOomEnable = Boolean.parseBoolean(gameOomString);
                if (PressureStateSettings.DEBUG_ALL) {
                    Slog.d(TAG, "mGameOomEnable cloud set received: " + gameOomString);
                }
            }
        } catch (JSONException e) {
            Slog.e(TAG, "updateCloudData error :", e);
        }
    }

    public boolean getGameOomEnable() {
        return this.mGameOomEnable;
    }

    public void registerThermalTempListener(ThermalTempListener listener) {
        this.mThermalTempListeners.add(listener);
    }

    public void unRegisterThermalTemListener(ThermalTempListener listener) {
        this.mThermalTempListeners.add(listener);
    }

    public void KillProcessForPadSmallWindowMode(String pkgName) {
        if (PAD_SMALL_WINDOW_CLEAN_ENABLE && this.mSystemReady) {
            cleanAllSubProcess();
            this.mProcessCleaner.KillProcessForPadSmallWindowMode(pkgName);
        }
    }

    private long killProcess(ProcessRecord proc, String reason) {
        if (this.mSystemReady) {
            return this.mProcessCleaner.killProcess(this.mSmartPowerService.getRunningProcess(proc.info.uid, proc.processName), reason);
        }
        return 0L;
    }

    public long killProcess(ProcessRecord proc, int minAdj, String reason) {
        if (this.mSystemReady) {
            return this.mProcessCleaner.killProcess(this.mSmartPowerService.getRunningProcess(proc.info.uid, proc.processName), minAdj, reason);
        }
        return 0L;
    }

    public void reclaimPage() {
        reclaimPage(1);
    }

    public void reclaimPage(int reclaimSizeMB) {
        if (IS_ENABLE_RECLAIM) {
            writeToNode(RECLAIM_EVENT_NODE, reclaimSizeMB);
        }
    }

    public void switchSPTMReclaim(boolean isEnable) {
        if (IS_ENABLE_RECLAIM) {
            writeToNode(RECLAIM_EVENT_NODE, isEnable ? 2 : 0);
        }
    }

    public void compactBackgroundProcess(int uid, String processName) {
        if (this.mSystemReady) {
            this.mProcessCleaner.compactBackgroundProcess(uid, processName);
        }
    }

    public void compactBackgroundProcess(IAppState.IRunningProcess proc, boolean isDelayed) {
        if (this.mSystemReady) {
            this.mProcessCleaner.compactBackgroundProcess(proc, isDelayed);
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:28:0x003f -> B:9:0x0050). Please report as a decompilation issue!!! */
    private void writeToNode(String node, int value) {
        FileWriter writer = null;
        File file = new File(node);
        String errMsg = "error " + node + ":" + value;
        try {
            try {
            } catch (Throwable th) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Slog.e(TAG, errMsg, e);
                    }
                }
                throw th;
            }
        } catch (IOException e2) {
            Slog.e(TAG, errMsg, e2);
        }
        if (!file.exists()) {
            return;
        }
        try {
            writer = new FileWriter(file);
            writer.write(String.valueOf(value));
            writer.close();
        } catch (IOException e3) {
            Slog.e(TAG, errMsg, e3);
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void performCompaction(String action, int pid) {
        MiuiMemoryServiceInternal memService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
        if (memService == null) {
            return;
        }
        memService.performCompaction(action, pid);
    }

    public void setAppStartingMode(boolean appStarting) {
        this.mHandler.removeMessages(2);
        this.mIsStartingApp = appStarting;
        if (appStarting) {
            this.mHandler.sendEmptyMessageDelayed(2, 2000L);
        }
        MiuiMemoryServiceInternal memService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
        if (memService != null) {
            memService.setAppStartingMode(appStarting);
        }
    }

    public boolean isCtsModeEnable() {
        return SmartPowerPolicyConstants.TESTSUITSPECIFIC;
    }

    public boolean powerKillProcess(ProcessConfig config) {
        if (this.mSystemReady) {
            return this.mPowerCleaner.powerKillProcess(config);
        }
        return false;
    }

    public boolean sceneKillProcess(ProcessConfig config) {
        if (this.mSystemReady) {
            return this.mSceneCleaner.sceneKillProcess(config);
        }
        return false;
    }

    public boolean isGameApp(String pckName) {
        return sGameAppList.contains(pckName);
    }

    public boolean isStartingApp() {
        return this.mIsStartingApp;
    }

    public void activityStartBeforeLocked(ControllerActivityInfo activityInfo) {
        if (!activityInfo.launchPkg.equals(this.mForegroundPackageName) && activityInfo.isCloudStart) {
            setAppStartingMode(true);
        }
    }

    public void foregroundActivityChangedLocked(ControllerActivityInfo activityInfo) {
        if (activityInfo.launchPkg.equals(this.mForegroundPackageName)) {
            return;
        }
        this.mForegroundPackageName = activityInfo.launchPkg;
        if (PressureStateSettings.PROCESS_CLEANER_ENABLED && this.mSystemReady) {
            this.mProcessCleaner.foregroundActivityChanged(activityInfo);
        }
        this.mProcessGameCleaner.foregroundInfoChanged(activityInfo.launchPkg, activityInfo.launchPid);
    }

    public void activityResumeUnchecked(ControllerActivityInfo activityInfo) {
        if (this.mIsStartingApp) {
            resetStartingAppState();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStartingAppState() {
        setAppStartingMode(false);
        synchronized (this.mStartingAppLock) {
            this.mStartingAppLock.notifyAll();
        }
    }

    public boolean isAudioOrGPSApp(int uid) {
        if (this.mSystemReady) {
            return this.mSmartPowerService.isAppAudioActive(uid) || this.mSmartPowerService.isAppGpsActive(uid);
        }
        return false;
    }

    public boolean isAudioOrGPSProc(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mSmartPowerService.isAppAudioActive(uid, pid) || this.mSmartPowerService.isAppGpsActive(uid, pid);
        }
        return false;
    }

    public boolean isForceStopEnable(ProcessRecord app, int policy) {
        return this.mPms.isForceStopEnable(app, policy);
    }

    public void onApplyOomAdjLocked(ProcessRecord app) {
        if (this.mSystemReady) {
            this.mProcessCleaner.onApplyOomAdjLocked(app);
        }
    }

    public boolean kill(ProcessConfig config) {
        if (!this.mSystemReady) {
            Slog.w(TAG, "The system is not ready");
            return false;
        }
        int callingPid = Binder.getCallingPid();
        if (!checkKillProcPermission(callingPid)) {
            String msg = "Permission Denial: ProcessManager.kill() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", procName=" + this.mPms.getProcessRecordByPid(callingPid).processName + ", pkgName=" + this.mPms.getProcessRecordByPid(callingPid).info.packageName;
            Slog.w(TAG, msg);
            return false;
        }
        int policy = config.getPolicy();
        switch (policy) {
            case 1:
                ((SpeedTestModeServiceInternal) LocalServices.getService(SpeedTestModeServiceInternal.class)).reportOneKeyCleanEvent();
                boolean success = sceneKillProcess(config);
                return success;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 10:
                boolean success2 = sceneKillProcess(config);
                return success2;
            case 8:
            case 9:
            case 15:
            case 17:
            default:
                Slog.w(TAG, "unKnown policy");
                return false;
            case 11:
            case 12:
            case 13:
            case 14:
            case 16:
            case 19:
            case 20:
                boolean success3 = powerKillProcess(config);
                return success3;
            case 18:
                boolean success4 = this.mPms.killAllBackgroundExceptLocked(config);
                return success4;
        }
    }

    private boolean checkKillProcPermission(int callingPid) {
        ProcessRecord app = this.mPms.getProcessRecordByPid(callingPid);
        if (mKillProcPermList.contains(app.processName) || mKillPkgPermList.contains(app.info.packageName)) {
            return true;
        }
        return false;
    }

    private long killPackage(ProcessRecord proc, String reason) {
        if (this.mSystemReady) {
            return this.mProcessCleaner.killPackage(this.mSmartPowerService.getRunningProcess(proc.info.uid, proc.processName), reason);
        }
        return 0L;
    }

    private long killPackage(ProcessRecord proc, int minAdj, String reason) {
        if (this.mSystemReady) {
            return this.mProcessCleaner.killPackage(this.mSmartPowerService.getRunningProcess(proc.info.uid, proc.processName), minAdj, reason);
        }
        return 0L;
    }

    public void notifyCpuPressureEvents(int level) {
        if (this.mSystemReady) {
            this.mSmartPowerService.onCpuPressureEvents(level);
        }
    }

    public void notifyCpuExceptionEvents(int type) {
        if (this.mSystemReady) {
            this.mSmartPowerService.onCpuExceptionEvents(type);
        }
    }

    public void notifyThermalTempChange(int temp) {
        Slog.d(TAG, "thermal temperature " + temp);
        Iterator<ThermalTempListener> it = this.mThermalTempListeners.iterator();
        while (it.hasNext()) {
            ThermalTempListener listener = it.next();
            listener.onThermalTempChange(temp);
        }
    }

    public long getCpuUsage(int pid, String processName) {
        return nGetCpuUsage(pid, processName);
    }

    public String getBackgroundCpuPolicy() {
        return nGetBackgroundCpuPolicy();
    }

    public void updateScreenState(boolean screenOn) {
        if (screenOn) {
            nStartPressureMonitor();
        } else {
            nEndPressureMonitor();
        }
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new ScreenStateReceiver(), filter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ScreenStateReceiver extends BroadcastReceiver {
        ScreenStateReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            boolean screenOn;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 0;
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
                    screenOn = true;
                    break;
                default:
                    screenOn = false;
                    break;
            }
            Message msg = Message.obtain(SystemPressureController.this.mHandler, 4, Boolean.valueOf(screenOn));
            msg.sendToTarget();
        }
    }
}
