package com.android.server.am;

import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.ApplicationPackageManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.JsonReader;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.android.server.am.ProcessPolicy;
import com.android.server.audio.AudioService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.security.AccessControlImpl;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.util.DeviceLevel;

/* loaded from: classes.dex */
public class MemoryStandardProcessControl implements MemoryStandardProcessControlStub {
    private static final int ADJ_WATERLINE = 250;
    private static final String DEFAULT_CONFIG_FILE_PATH = "/product/etc/mspc.default.json";
    public static final int EVENT_TAGS = 80900;
    private static final long HANDLE_MEM_PRESSURE_INTERVAL = 120000;
    private static final long KILLING_DURATION_THRESHOLD = 86400000;
    private static final int MAX_CONTINUE_DETECT_EXCEPTION = 3;
    private static final int MAX_PROCESS_KILL_TIMES = 3;
    private static final String MEMORY_SCREENOFF_STRATEGY = "persist.sys.memory_standard.strategy";
    private static final String MEMORY_STANDARD_APPHEAP_ENABLE_REOP = "persist.sys.memory_standard.appheap.enable";
    private static final String MEMORY_STANDARD_ENABLE_PROP = "persist.sys.memory_standard.enable";
    private static final int MEM_NO_PRESSURE = -1;
    private static final int MEM_PRESSURE_COUNT = 3;
    private static final int MEM_PRESSURE_CRITICAL = 2;
    private static final int MEM_PRESSURE_LOW = 0;
    private static final int MEM_PRESSURE_MIN = 1;
    private static final int MSG_DUMP_STANDARD = 6;
    private static final int MSG_DUMP_WARNED_APP = 5;
    private static final int MSG_KILL_HIGH_PRIORITY_PROCESSES = 3;
    private static final int MSG_PERIODIC_DETECTION = 1;
    private static final int MSG_PERIODIC_KILL = 2;
    private static final int MSG_REPORT_APP_HEAP = 7;
    private static final int MSG_REPORT_PRESSURE = 4;
    private static final int SCREEN_STATE_OFF = 2;
    private static final int SCREEN_STATE_ON = 1;
    private static final int SCREEN_STATE_UNKOWN = 3;
    private static final String TAG = "MemoryStandardProcessControl";
    private static long[] sDefaultMemoryStandardArray = {FormatBytesUtil.MB, FormatBytesUtil.MB, 1572864, 1572864, 2097152, 2097152, 2097152};
    private static final int[][] sDefaultCacheLevel = {new int[]{450000, 370000, 290000}, new int[]{550000, 470000, 390000}, new int[]{650000, 570000, 490000}, new int[]{1250000, 1170000, 1090000}, new int[]{1650000, 1570000, 1490000}, new int[]{2050000, 1970000, 1890000}, new int[]{2450000, 2370000, 2290000}};
    private static final String MEMORY_STANDARD_DEBUG_PROP = "persist.sys.memory_standard.debug";
    public static boolean DEBUG = SystemProperties.getBoolean(MEMORY_STANDARD_DEBUG_PROP, false);
    public volatile boolean mEnable = SystemProperties.getBoolean(MEMORY_STANDARD_ENABLE_PROP, false);
    public volatile boolean mAppHeapEnable = SystemProperties.getBoolean(MEMORY_STANDARD_APPHEAP_ENABLE_REOP, false);
    private volatile boolean mScreenDetection = true;
    public volatile int mScreenOffStrategy = SystemProperties.getInt(MEMORY_SCREENOFF_STRATEGY, 0);
    private HandlerThread mHandlerThread = new HandlerThread(TAG);
    public volatile int mScreenState = 3;
    private int[] mPressureCacheThreshold = new int[3];
    private boolean mIsInit = false;
    private boolean mTriggerKill = false;
    private long mDefaultMemoryStandard = 2097152;
    private long mNeedReleaseMemSize = 0;
    public volatile long mAppHeapHandleTime = 1200000;
    private long mLastMemPressureTime = 0;
    private boolean mLastMemDetectionHasKilled = false;
    public MemoryStandardMap mMemoryStandardMap = new MemoryStandardMap();
    public HashMap<String, AppInfo> mWarnedAppMap = new HashMap<>();
    public HashMap<String, Integer> mAppHeapStandard = new HashMap<>();
    private Context mContext = null;
    public MyHandler mHandler = null;
    private ActivityManagerService mAMS = null;
    private ActivityTaskManagerService mATMS = null;
    private ProcessManagerService mPMS = null;
    private AudioService mAudioService = null;
    private ApplicationPackageManager mPackageManager = null;
    private WindowManagerInternal mWMS = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.am.MemoryStandardProcessControl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                MemoryStandardProcessControl.this.mScreenState = 1;
                if (MemoryStandardProcessControl.this.mScreenOffStrategy == 1) {
                    MemoryStandardProcessControl.this.mHandler.removeMessages(2);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                MemoryStandardProcessControl.this.mScreenState = 2;
                if (MemoryStandardProcessControl.this.mScreenOffStrategy == 1) {
                    MemoryStandardProcessControl.this.mHandler.sendEmptyMessageDelayed(3, AccessControlImpl.LOCK_TIME_OUT);
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MemoryStandardProcessControl> {

        /* compiled from: MemoryStandardProcessControl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MemoryStandardProcessControl INSTANCE = new MemoryStandardProcessControl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MemoryStandardProcessControl m504provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MemoryStandardProcessControl m503provideNewInstance() {
            return new MemoryStandardProcessControl();
        }
    }

    public boolean isEnable() {
        return this.mEnable && this.mIsInit;
    }

    public boolean init(Context context, ActivityManagerService ams) {
        int index;
        if (!this.mEnable || this.mIsInit) {
            return false;
        }
        if (!loadDefaultConfigFromFile()) {
            this.mEnable = false;
            return false;
        }
        this.mContext = context;
        int totalMemGB = (int) ((Process.getTotalMemory() / FormatBytesUtil.GB) + 1);
        if (totalMemGB > 12) {
            index = sDefaultMemoryStandardArray.length - 1;
        } else if (totalMemGB > 4) {
            index = totalMemGB / 2;
        } else {
            index = totalMemGB - 2;
        }
        initGlobalParams(context, ams, index);
        MemoryControlCloud cloudControl = new MemoryControlCloud();
        cloudControl.initCloud(context, this);
        if (this.mScreenDetection) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            context.registerReceiver(this.mReceiver, filter);
        }
        if (DEBUG) {
            debugModifyKillStrategy();
        }
        MemoryControlJobService.detectionSchedule(this.mContext);
        MemoryControlJobService.killSchedule(this.mContext);
        ServiceManager.addService("mspc", new BinderService());
        if (DEBUG) {
            Slog.d(TAG, "init mspc2.0 success");
        }
        this.mIsInit = true;
        return true;
    }

    private void initGlobalParams(Context context, ActivityManagerService ams, int index) {
        this.mDefaultMemoryStandard = sDefaultMemoryStandardArray[index];
        loadDefCacheLevelConfig(index);
        this.mHandlerThread.start();
        this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
        this.mAMS = ams;
        this.mATMS = ams.mActivityTaskManager;
        this.mPMS = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        this.mPackageManager = context.getApplicationContext().getPackageManager();
        this.mWMS = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mAudioService = ServiceManager.getService("audio");
    }

    private void loadDefCacheLevelConfig(int memIndex) {
        int[] iArr = this.mPressureCacheThreshold;
        int[] iArr2 = sDefaultCacheLevel[memIndex];
        iArr[0] = iArr2[0];
        iArr[1] = iArr2[1];
        iArr[2] = iArr2[2];
    }

    private int getMemPressureLevel() {
        int level;
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        this.mNeedReleaseMemSize = 0L;
        long[] rawInfo = minfo.getRawInfo();
        long otherFile = rawInfo[3] + rawInfo[26] + rawInfo[2];
        long needRemovedFile = rawInfo[4] + rawInfo[18] + rawInfo[26];
        if (otherFile > needRemovedFile) {
            otherFile -= needRemovedFile;
        }
        int[] iArr = this.mPressureCacheThreshold;
        if (otherFile >= iArr[0]) {
            return -1;
        }
        if (otherFile >= iArr[1]) {
            level = 0;
        } else {
            int level2 = iArr[2];
            if (otherFile >= level2) {
                level = 1;
            } else {
                this.mNeedReleaseMemSize = level2 - otherFile;
                level = 2;
            }
        }
        if (DEBUG) {
            Slog.i(TAG, "Other File: " + otherFile + "KB. Mem Pressure Level: " + level);
        }
        return level;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void debugModifyKillStrategy() {
        this.mEnable = true;
        this.mScreenOffStrategy = 1;
    }

    public void periodicMemoryDetection() {
        AppInfo info;
        String packageName;
        MemoryStandard standard;
        String processName;
        AppInfo info2;
        if (!isEnable() || this.mMemoryStandardMap.isEmpty()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start memory detection");
        }
        this.mLastMemDetectionHasKilled = false;
        for (AppInfo info3 : this.mWarnedAppMap.values()) {
            info3.mProcessMap.clear();
            info3.mAdj = 1001;
        }
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        synchronized (this.mAMS) {
            try {
                ArrayList<ProcessRecord> lruProcs = this.mAMS.mProcessList.getLruProcessesLOSP();
                int N = lruProcs.size();
                for (int i = N - 1; i >= 0; i--) {
                    try {
                        procs.add(lruProcs.get(i));
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                for (int i2 = N - 1; i2 >= 0; i2--) {
                    ProcessRecord app = procs.get(i2);
                    if (app.mState.getCurAdj() > 0 && !app.isKilledByAm() && !app.isKilled() && app.getThread() != null && !app.isolated && (standard = this.mMemoryStandardMap.getStandard((packageName = app.info.packageName))) != null) {
                        String processName2 = app.processName;
                        if (standard.mProcesses.contains(processName2)) {
                            AppInfo info4 = this.mWarnedAppMap.get(packageName);
                            if (info4 != null) {
                                processName = processName2;
                                info2 = info4;
                            } else {
                                processName = processName2;
                                info2 = new AppInfo(packageName, app.uid, app.userId, app.mState.getCurAdj());
                                this.mWarnedAppMap.put(packageName, info2);
                            }
                            long appPss = Debug.getPss(app.mPid, null, null) / FormatBytesUtil.KB;
                            info2.mProcessMap.put(processName, Long.valueOf(appPss));
                            int newAdj = app.mState.getCurAdj();
                            if (info2.mAdj > newAdj) {
                                info2.mAdj = newAdj;
                            }
                            if (DEBUG) {
                                Slog.d(TAG, "package = " + packageName + ", process = " + processName + ", adj = " + info2.mAdj);
                            }
                        }
                    }
                }
                for (String cmd : this.mMemoryStandardMap.getNativeProcessCmds()) {
                    int[] pids = Process.getPidsForCommands(new String[]{cmd});
                    if (pids != null && pids.length > 0) {
                        int pid = pids[0];
                        long appPss2 = Debug.getPss(pid, null, null) / FormatBytesUtil.KB;
                        AppInfo info5 = this.mWarnedAppMap.get(cmd);
                        if (info5 != null) {
                            info = info5;
                        } else {
                            info = new AppInfo(cmd, 1000, 0, ScoutHelper.OOM_SCORE_ADJ_MIN);
                            this.mWarnedAppMap.put(cmd, info);
                        }
                        info.mAdj = ScoutHelper.OOM_SCORE_ADJ_MIN;
                        info.mProcessMap.put(cmd, Long.valueOf(appPss2));
                        if (DEBUG) {
                            Slog.d(TAG, "package = " + cmd + ", process = " + cmd + ", adj = " + info.mAdj);
                        }
                    }
                }
                long now = System.currentTimeMillis();
                for (AppInfo info6 : this.mWarnedAppMap.values()) {
                    String packageName2 = info6.mPackageName;
                    if (info6.mKillTimes > 3 && !DEBUG) {
                        Slog.d(TAG, packageName2 + " frequently killed, skip");
                    } else {
                        long totalPss = info6.getTotalPss();
                        long standardPss = this.mMemoryStandardMap.getPss(packageName2);
                        if (totalPss > standardPss) {
                            info6.mMemExcelTime++;
                            if (info6.mMemExcelTime >= 3) {
                                this.mTriggerKill = true;
                                info6.mKillTimes++;
                                info6.mLastKillTime = now;
                            }
                            Slog.d(TAG, packageName2 + " is out of standard, pss is " + totalPss);
                        } else if (info6.mMemExcelTime > 0) {
                            info6.mMemExcelTime--;
                        }
                    }
                }
                if (this.mTriggerKill) {
                    killProcess(false);
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void killProcess(boolean canKillhighPriorityProcess) {
        if (!isEnable()) {
            return;
        }
        Slog.d(TAG, "canKillhighPriorityProcess: " + canKillhighPriorityProcess);
        if (canKillhighPriorityProcess) {
            if (this.mScreenState != 2) {
                Slog.d(TAG, "screen on when detect, skip");
                return;
            }
        } else {
            this.mTriggerKill = false;
        }
        List<Integer> activeUids = getActiveUids();
        List<Integer> visibleUids = getVisibleWindowOwner();
        Set<String> deletePackageNameSet = new HashSet<>();
        for (AppInfo info : this.mWarnedAppMap.values()) {
            if (info.mMemExcelTime >= 3) {
                String packageName = info.mPackageName;
                if (canKill(canKillhighPriorityProcess, info, activeUids, visibleUids)) {
                    if (DEBUG) {
                        Slog.d(TAG, "add to the kill list, packageName:" + info.mPackageName + "; mMemExcelTime:" + info.mMemExcelTime);
                    }
                    deletePackageNameSet.add(packageName);
                }
            }
        }
        if (deletePackageNameSet.size() > 0) {
            doClean(deletePackageNameSet, canKillhighPriorityProcess);
        }
        resetRecordDataIfNeeded();
    }

    private void doClean(Set<String> deletePackageNameSet, boolean canKillhighPriorityProcess) {
        List<ProcessRecord> records;
        if (getMemPressureLevel() < 2 && !DEBUG) {
            return;
        }
        List<RestartProcessInfo> recordRestartProcs = new ArrayList<>();
        long memoryReleased = 0;
        try {
            synchronized (this.mAMS) {
                List<ProcessRecord> records2 = this.mAMS.mProcessList.getLruProcessesLOSP();
                int N = records2.size();
                int i = N - 1;
                while (i >= 0) {
                    ProcessRecord app = records2.get(i);
                    int adj = app.mState.getCurAdj();
                    int i2 = app.mPid;
                    String processName = app.processName;
                    String packageName = app.info.packageName;
                    if (packageName == null || !deletePackageNameSet.contains(packageName) || app.isKilledByAm()) {
                        records = records2;
                    } else if (app.isKilled() || app.getThread() == null) {
                        records = records2;
                    } else if (app.isolated) {
                        records = records2;
                    } else if (adj >= ADJ_WATERLINE || canKillhighPriorityProcess) {
                        records = records2;
                        MemoryStandard standard = this.mMemoryStandardMap.getStandard(packageName);
                        if (standard == null || standard.mProcesses.contains(processName)) {
                            long procPss = Debug.getPss(app.mPid, null, null) / FormatBytesUtil.KB;
                            memoryReleased += procPss;
                            AppProfilerStub.getInstance().reportMemoryStandardProcessControlKillMessage(processName, app.mPid, app.uid, procPss);
                            if (adj < ADJ_WATERLINE) {
                                recordRestartProcs.add(new RestartProcessInfo(packageName, processName, app.userId));
                            }
                            this.mLastMemDetectionHasKilled = true;
                            app.killLocked("MemoryStandardProcessContral", 13, true);
                            EventLog.writeEvent(EVENT_TAGS, "standardkill:" + killedReason(app));
                            Slog.d(TAG, "kill process, processName:" + processName + "; pid:" + app.mPid);
                            deletePackageNameSet.remove(packageName);
                            AppInfo killedAppInfo = this.mWarnedAppMap.get(packageName);
                            killedAppInfo.mMemExcelTime = 0;
                            killedAppInfo.mAdj = 1001;
                            if (memoryReleased > this.mNeedReleaseMemSize && !DEBUG) {
                                break;
                            }
                        }
                    } else {
                        records = records2;
                        Slog.d(TAG, "Process adj has a high priority and cannot be killed:" + processName + "; adj:" + adj);
                    }
                    i--;
                    records2 = records;
                }
            }
            for (int i3 = 0; i3 < recordRestartProcs.size(); i3++) {
                RestartProcessInfo procInfo = recordRestartProcs.get(i3);
                ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(procInfo.mPackageName, FormatBytesUtil.KB, procInfo.mUserId);
                synchronized (this.mAMS) {
                    this.mAMS.startProcessLocked(procInfo.mProcessName, info, false, 0, new HostingRecord("MemoryStandardPullUp", procInfo.mProcessName), 1, false, false, "android");
                }
            }
            Set<String> nativeCmds = this.mMemoryStandardMap.getNativeProcessCmds();
            if (nativeCmds.size() == 0) {
                return;
            }
            for (String proc : deletePackageNameSet) {
                if (memoryReleased <= this.mNeedReleaseMemSize || DEBUG) {
                    if (nativeCmds.contains(proc)) {
                        int[] pids = Process.getPidsForCommands(new String[]{proc});
                        if (pids != null && pids.length > 0) {
                            int pid = pids[0];
                            long procPss2 = Debug.getPss(pid, null, null) / FormatBytesUtil.KB;
                            this.mLastMemDetectionHasKilled = true;
                            Process.killProcess(pid);
                            memoryReleased += procPss2;
                            Slog.d(TAG, "kill native process, processName:" + proc + "; pid:" + pid);
                            AppInfo killedAppInfo2 = this.mWarnedAppMap.get(proc);
                            killedAppInfo2.mMemExcelTime = 0;
                            killedAppInfo2.mAdj = ScoutHelper.OOM_SCORE_ADJ_MIN;
                        }
                    }
                } else {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isForegroundApp(String packageName) {
        ActivityTaskManager.RootTaskInfo info;
        try {
            info = this.mATMS.getFocusedRootTaskInfo();
        } catch (RemoteException e) {
        }
        if (info != null && info.topActivity != null) {
            String topPackageName = info.topActivity.getPackageName();
            return packageName.equals(topPackageName);
        }
        Slog.e(TAG, "get getFocusedStackInfo error.");
        return false;
    }

    private void resetRecordDataIfNeeded() {
        long now = System.currentTimeMillis();
        for (AppInfo info : this.mWarnedAppMap.values()) {
            if (now - info.mLastKillTime > KILLING_DURATION_THRESHOLD && info.mKillTimes > 3) {
                info.mKillTimes = 0;
                if (DEBUG) {
                    Slog.d(TAG, info.mPackageName + " long time never killed, reset kill times");
                }
            }
        }
    }

    private List<Integer> getVisibleWindowOwner() {
        List<Integer> list = this.mWMS.getVisibleWindowOwner();
        if (DEBUG && list.size() > 0) {
            int[] debugUids = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                debugUids[i] = list.get(i).intValue();
            }
            String[] pkgs = this.mPackageManager.getNamesForUids(debugUids);
            Slog.d(TAG, "VisibleWindowPkgs: " + Arrays.asList(pkgs));
        }
        return list;
    }

    private List<Integer> getActiveUids() {
        List<Integer> uids = new ArrayList<>();
        List<ProcessPolicy.ActiveUidRecord> activeUids = this.mPMS.getProcessPolicy().getActiveUidRecordList(3);
        if (activeUids != null) {
            for (ProcessPolicy.ActiveUidRecord r : activeUids) {
                int uid = r.uid;
                if (!uids.contains(Integer.valueOf(uid))) {
                    uids.add(Integer.valueOf(uid));
                }
            }
        }
        if (DEBUG && uids.size() > 0) {
            int[] debugUids = new int[uids.size()];
            for (int i = 0; i < uids.size(); i++) {
                debugUids[i] = uids.get(i).intValue();
            }
            String[] pkgs = this.mPackageManager.getNamesForUids(debugUids);
            Slog.d(TAG, "LocationActivePkgs: " + Arrays.asList(pkgs));
        }
        return uids;
    }

    private boolean canKill(boolean canKillhighPriorityProcess, AppInfo info, List<Integer> activeUids, List<Integer> visibleUids) {
        int adj = info.mAdj;
        if (canKillhighPriorityProcess) {
            if (adj > ADJ_WATERLINE) {
                return false;
            }
        } else if (adj <= ADJ_WATERLINE) {
            return false;
        }
        String packageName = info.mPackageName;
        if (this.mMemoryStandardMap.mNativeProcessCmds.contains(packageName)) {
            MemoryStandard standard = this.mMemoryStandardMap.getStandard(packageName);
            if (standard == null || standard.mParent == null) {
                return true;
            }
            return !isForegroundApp(standard.mParent);
        }
        int uid = info.mUid;
        boolean foreground = isForegroundApp(packageName);
        boolean active = activeUids.contains(Integer.valueOf(uid));
        boolean visible = visibleUids.contains(Integer.valueOf(uid));
        if (DEBUG) {
            Slog.d(TAG, "packageName: " + packageName);
            Slog.d(TAG, "isForegroundApp(packageName): " + foreground);
            Slog.d(TAG, "activeUids.contains(uid): " + active);
            Slog.d(TAG, "visibleUids.contains(uid): " + visible);
        }
        return (foreground || active || visible) ? false : true;
    }

    private String killedReason(ProcessRecord pr) {
        return String.format("#%s n:%s(%d) u:%d pss:%d r:%s", "KillProc", pr.processName, Integer.valueOf(pr.mPid), Integer.valueOf(pr.uid), Long.valueOf(pr.mProfile.getLastPss()), "OutOfMemoryStandard");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMemPressure(int pressureState) {
        if (DEBUG) {
            Slog.d(TAG, "current pressure is " + pressureState);
        }
        if (getMemPressureLevel() < 2 && !DEBUG) {
            return;
        }
        periodicMemoryDetection();
        if (this.mLastMemDetectionHasKilled) {
            MemoryControlJobService.detectionSchedule(this.mContext);
            if (DEBUG) {
                Slog.d(TAG, "app killed by memory pressure, reschedule detection job");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppHeapWhenBackground(int pid, String pkgName) {
        int i;
        int userId;
        boolean finded;
        Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
        if (!Debug.getMemoryInfo(pid, memInfo)) {
            return;
        }
        int appHeap = memInfo.getSummaryJavaHeap() / 1024;
        Slog.d(TAG, "handle app heap when background, appHeap is " + appHeap);
        synchronized (this.mAMS) {
            try {
                List<ProcessRecord> records = this.mAMS.mProcessList.getLruProcessesLOSP();
                int N = records.size();
                int i2 = N - 1;
                while (true) {
                    if (i2 < 0) {
                        i = -1;
                        userId = -1;
                        finded = false;
                        break;
                    }
                    ProcessRecord app = records.get(i2);
                    String processName = app.processName;
                    if (!pkgName.equals(processName)) {
                        i2--;
                    } else {
                        int uid = app.uid;
                        int userId2 = app.userId;
                        if (app.mPid == pid && !app.isKilledByAm() && !app.isKilled() && app.getThread() != null && !app.isolated) {
                            i = uid;
                            userId = userId2;
                            finded = true;
                        }
                        return;
                    }
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                List<Integer> visibleUids = getVisibleWindowOwner();
                List<Integer> activeUids = getActiveUids();
                if (finded && appHeap > this.mAppHeapStandard.get(pkgName).intValue()) {
                    if (!isForegroundApp(pkgName) && !visibleUids.contains(Integer.valueOf(i)) && !activeUids.contains(Integer.valueOf(i))) {
                        this.mAMS.forceStopPackage(pkgName, userId, 0, "MemoryStandardProcessControl: OutOfHeapStandard");
                        String killReason = String.format("#%s n:%s(%d) u:%d pss:%d r:%s", "KillProc", pkgName, Integer.valueOf(pid), Integer.valueOf(i), Integer.valueOf(appHeap), "OutOfHeapStandard");
                        EventLog.writeEvent(EVENT_TAGS, "overHeapkill:" + killReason);
                        Slog.d(TAG, "over heap kill, processName:" + pkgName + "; pid:" + pid);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void reportMemPressure(int pressureState) {
        if (!isEnable() || this.mScreenState != 1) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - this.mLastMemPressureTime <= HANDLE_MEM_PRESSURE_INTERVAL || pressureState < 3) && !DEBUG) {
            return;
        }
        this.mLastMemPressureTime = now;
        Message msg = this.mHandler.obtainMessage(4, pressureState, 0);
        this.mHandler.sendMessageAtFrontOfQueue(msg);
    }

    public void reportAppStopped(int pid, String packageName) {
        if (isEnable() && this.mAppHeapEnable && packageName != null && this.mAppHeapStandard.containsKey(packageName)) {
            this.mHandler.removeEqualMessages(7, packageName);
            Message msg = this.mHandler.obtainMessage(7, pid, 0, packageName);
            this.mHandler.sendMessageDelayed(msg, DEBUG ? 5000L : this.mAppHeapHandleTime);
        }
    }

    public void reportAppResumed(int pid, String packageName) {
        if (isEnable() && this.mAppHeapEnable && packageName != null && this.mAppHeapStandard.containsKey(packageName)) {
            this.mHandler.removeEqualMessages(7, packageName);
        }
    }

    public void callPeriodicDetection() {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    public void callKillProcess(boolean killHighPriority) {
        int msgId = killHighPriority ? 3 : 2;
        this.mHandler.removeMessages(msgId);
        this.mHandler.obtainMessage(msgId).sendToTarget();
    }

    public void callUpdateCloudConfig(String cloudStr) {
        loadConfigFromCloud(cloudStr);
    }

    public void callUpdateCloudAppHeapConfig(String cloudStr) {
        loadAppHeapConfigFromCloud(cloudStr);
    }

    public void callRunnable(Runnable callback) {
        this.mHandler.post(callback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MemoryStandardProcessControl.this.periodicMemoryDetection();
                    return;
                case 2:
                    MemoryStandardProcessControl.this.killProcess(false);
                    return;
                case 3:
                    MemoryStandardProcessControl.this.killProcess(true);
                    return;
                case 4:
                    MemoryStandardProcessControl.this.handleMemPressure(msg.arg1);
                    return;
                case 5:
                    for (AppInfo info : MemoryStandardProcessControl.this.mWarnedAppMap.values()) {
                        Slog.d(MemoryStandardProcessControl.TAG, info.toString());
                    }
                    return;
                case 6:
                    Slog.i(MemoryStandardProcessControl.TAG, MemoryStandardProcessControl.this.mMemoryStandardMap.toString());
                    Slog.i(MemoryStandardProcessControl.TAG, MemoryStandardProcessControl.this.mAppHeapStandard.toString());
                    return;
                case 7:
                    MemoryStandardProcessControl.this.handleAppHeapWhenBackground(msg.arg1, (String) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private void parseConfigAging(JsonReader jsonReader, String systemType) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            String packageName = null;
            String parent = null;
            long pss = 0;
            boolean isNative = false;
            List<String> processes = new ArrayList<>();
            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                if (key.equals("name")) {
                    packageName = jsonReader.nextString();
                } else if (key.equals("processes")) {
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        processes.add(jsonReader.nextString());
                    }
                    jsonReader.endArray();
                } else if (key.equals(systemType)) {
                    pss = jsonReader.nextLong();
                } else if (key.equals("parent")) {
                    parent = jsonReader.nextString();
                } else if (key.equals("native")) {
                    isNative = jsonReader.nextBoolean();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if (pss != 0 && packageName != null) {
                if (isNative) {
                    processes.add(packageName);
                    this.mMemoryStandardMap.updateNativeMemoryStandard(pss, packageName, processes, parent);
                } else {
                    this.mMemoryStandardMap.updateMemoryStandard(pss, packageName, processes, parent);
                }
            }
        }
        jsonReader.endArray();
    }

    private void parseConfigCommon(JsonReader jsonReader, String systemType) throws IOException {
        jsonReader.beginObject();
        long pss = 0;
        while (jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if (key.equals(systemType)) {
                pss = jsonReader.nextLong();
            } else if (key.equals("apps")) {
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    jsonReader.beginObject();
                    List<String> processes = new ArrayList<>();
                    String packageName = null;
                    String parent = null;
                    boolean isNative = false;
                    while (jsonReader.hasNext()) {
                        String pKey = jsonReader.nextName();
                        if (pKey.equals("name")) {
                            packageName = jsonReader.nextString();
                        } else if (pKey.equals("processes")) {
                            jsonReader.beginArray();
                            while (jsonReader.hasNext()) {
                                processes.add(jsonReader.nextString());
                            }
                            jsonReader.endArray();
                        } else if (pKey.equals("native")) {
                            isNative = jsonReader.nextBoolean();
                        } else if (pKey.equals("parent")) {
                            parent = jsonReader.nextString();
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    if (pss != 0 && packageName != null) {
                        if (isNative) {
                            processes.add(packageName);
                            this.mMemoryStandardMap.updateNativeMemoryStandard(pss, packageName, processes, parent);
                        } else {
                            this.mMemoryStandardMap.updateMemoryStandard(pss, packageName, processes, parent);
                        }
                        jsonReader.endObject();
                    }
                }
                jsonReader.endArray();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }

    private void parseConfigHeap(JsonReader jsonReader, String systemType) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            String packageName = null;
            int appHeap = 0;
            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                if (key.equals("name")) {
                    packageName = jsonReader.nextString();
                } else if (key.equals(systemType)) {
                    appHeap = jsonReader.nextInt();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if (appHeap != 0 && packageName != null) {
                this.mAppHeapStandard.put(packageName, Integer.valueOf(appHeap));
            }
        }
        jsonReader.endArray();
    }

    private void parseConfigRemove(JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            String packageName = jsonReader.nextString();
            this.mMemoryStandardMap.removeMemoryStandard(packageName);
            this.mWarnedAppMap.remove(packageName);
        }
        jsonReader.endArray();
    }

    private boolean loadDefaultConfigFromFile() {
        File file = new File(DEFAULT_CONFIG_FILE_PATH);
        if (!file.exists()) {
            return false;
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        JsonReader jsonReader = null;
        this.mMemoryStandardMap.clear();
        String systemType = DeviceLevel.IS_MIUI_LITE_VERSION ? "lite" : "miui";
        try {
            try {
                fileInputStream = new FileInputStream(file);
                inputStreamReader = new InputStreamReader(fileInputStream);
                jsonReader = new JsonReader(inputStreamReader);
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("aging")) {
                        parseConfigAging(jsonReader, systemType);
                    } else if (name.equals("common")) {
                        parseConfigCommon(jsonReader, systemType);
                    } else if (name.equals("heap")) {
                        parseConfigHeap(jsonReader, systemType);
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                closeIO(jsonReader);
                closeIO(inputStreamReader);
                closeIO(fileInputStream);
                return true;
            } catch (IOException e) {
                if (DEBUG) {
                    Slog.e(TAG, "load config from default config file failed, invalid config");
                }
                closeIO(jsonReader);
                closeIO(inputStreamReader);
                closeIO(fileInputStream);
                return false;
            }
        } catch (Throwable th) {
            closeIO(jsonReader);
            closeIO(inputStreamReader);
            closeIO(fileInputStream);
            throw th;
        }
    }

    public void loadConfigFromCloud(String str) {
        StringReader stringReader = null;
        JsonReader jsonReader = null;
        String systemType = DeviceLevel.IS_MIUI_LITE_VERSION ? "lite" : "miui";
        try {
            try {
                stringReader = new StringReader(str);
                jsonReader = new JsonReader(stringReader);
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("aging")) {
                        parseConfigAging(jsonReader, systemType);
                    } else if (name.equals("common")) {
                        parseConfigCommon(jsonReader, systemType);
                    } else if (name.equals("remove")) {
                        parseConfigRemove(jsonReader);
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } catch (IOException e) {
                if (DEBUG) {
                    Slog.e(TAG, "load config from cloud or database failed, invalid config");
                }
                this.mEnable = false;
            }
        } finally {
            closeIO(jsonReader);
            closeIO(stringReader);
        }
    }

    public void loadAppHeapConfigFromCloud(String str) {
        StringReader stringReader = null;
        JsonReader jsonReader = null;
        String systemType = DeviceLevel.IS_MIUI_LITE_VERSION ? "lite" : "miui";
        this.mAppHeapStandard.clear();
        this.mHandler.removeMessages(7);
        try {
            try {
                stringReader = new StringReader(str);
                jsonReader = new JsonReader(stringReader);
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("heap")) {
                        parseConfigHeap(jsonReader, systemType);
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } catch (IOException e) {
                if (DEBUG) {
                    Slog.e(TAG, "load app heap config from cloud or database failed, invalid config");
                }
                this.mAppHeapEnable = false;
            }
        } finally {
            closeIO(jsonReader);
            closeIO(stringReader);
        }
    }

    private <T extends Closeable> void closeIO(T io) {
        if (io != null) {
            try {
                io.close();
            } catch (IOException e) {
                Slog.e(TAG, "close io failed: " + e.getMessage());
            }
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new MemoryStandardProcessControlCmd().exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private class MemoryStandardProcessControlCmd extends ShellCommand {
        private MemoryStandardProcessControlCmd() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Failed to find 'out' block for switch in B:12:0x0050. Please report as an issue. */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case -1298848381:
                        if (cmd.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1276242363:
                        if (cmd.equals("pressure")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3095028:
                        if (cmd.equals("dump")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 95458899:
                        if (cmd.equals("debug")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1312628413:
                        if (cmd.equals("standard")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
            } catch (Exception e) {
                pw.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e("ShellCommand", "Error running shell command! " + e);
            }
            switch (c) {
                case 0:
                    MemoryStandardProcessControl.this.mHandler.sendMessage(MemoryStandardProcessControl.this.mHandler.obtainMessage(5));
                    return 0;
                case 1:
                    try {
                        int presssureState = Integer.parseInt(getNextArgRequired());
                        MemoryStandardProcessControl.this.reportMemPressure(presssureState);
                    } catch (NumberFormatException e2) {
                        pw.println("pressure is invalid");
                    }
                    return 0;
                case 2:
                    String mode = getNextArgRequired();
                    if (mode.equals("list")) {
                        MemoryStandardProcessControl.this.mHandler.sendMessage(MemoryStandardProcessControl.this.mHandler.obtainMessage(6));
                        pw.println("please check logcat");
                    } else if (mode.equals("add")) {
                        String[] strs = getNextArgRequired().split(",");
                        String packageName = strs[0];
                        long pss = Long.parseLong(strs[1]);
                        List<String> processes = new ArrayList<>();
                        processes.add(strs[2]);
                        MemoryStandardProcessControl.this.mMemoryStandardMap.updateMemoryStandard(pss, packageName, processes, null);
                    } else if (mode.equals("add-native")) {
                        String[] strs2 = getNextArgRequired().split(",");
                        String packageName2 = strs2[0];
                        long pss2 = Long.parseLong(strs2[1]);
                        List<String> processes2 = new ArrayList<>();
                        processes2.add(strs2[2]);
                        String parent = null;
                        if (strs2.length > 3) {
                            parent = strs2[3];
                        }
                        MemoryStandardProcessControl.this.mMemoryStandardMap.updateNativeMemoryStandard(pss2, packageName2, processes2, parent);
                    }
                    return 0;
                case 3:
                    String str = getNextArgRequired();
                    if ("true".equals(str)) {
                        MemoryStandardProcessControl.this.mEnable = true;
                        pw.println("mspc enabled");
                    } else if ("false".equals(str)) {
                        MemoryStandardProcessControl.this.mEnable = false;
                        pw.println("mspc disabled");
                    }
                    return 0;
                case 4:
                    String str2 = getNextArgRequired();
                    if ("true".equals(str2)) {
                        MemoryStandardProcessControl.DEBUG = true;
                        MemoryStandardProcessControl.this.debugModifyKillStrategy();
                        pw.println("debug on, enable mspc and screen off kill");
                    } else if ("false".equals(str2)) {
                        MemoryStandardProcessControl.DEBUG = false;
                        MemoryStandardProcessControl.this.mScreenOffStrategy = 0;
                        pw.println("debug off, disable mspc and screen off kill");
                    }
                    return 0;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Memory Standard Process Control commands:");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppInfo {
        public int mAdj;
        public final String mPackageName;
        public final int mUid;
        public final int mUserId;
        public final Map<String, Long> mProcessMap = new HashMap();
        public int mKillTimes = 0;
        public long mLastKillTime = 0;
        public int mMemExcelTime = 1;

        public AppInfo(String packageName, int uid, int userId, int adj) {
            this.mPackageName = packageName;
            this.mUid = uid;
            this.mUserId = userId;
            this.mAdj = adj;
        }

        public int getTotalPss() {
            int pss = 0;
            for (Map.Entry<String, Long> e : this.mProcessMap.entrySet()) {
                pss = (int) (pss + e.getValue().longValue());
            }
            return pss;
        }

        public String toString() {
            return String.format("{ package: %s, processes: %s, uid: %d, userId: %d, adj: %d }", this.mPackageName, this.mProcessMap.toString(), Integer.valueOf(this.mUid), Integer.valueOf(this.mUserId), Integer.valueOf(this.mAdj));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RestartProcessInfo {
        public String mPackageName;
        public String mProcessName;
        public int mUserId;

        public RestartProcessInfo(String packageName, String processName, int userId) {
            this.mPackageName = packageName;
            this.mProcessName = processName;
            this.mUserId = userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MemoryStandard {
        public final String mParent;
        public final Set<String> mProcesses;
        public final long mPss;

        public MemoryStandard(long pss, List<String> processes, String parent) {
            HashSet hashSet = new HashSet();
            this.mProcesses = hashSet;
            this.mPss = pss;
            this.mParent = parent;
            hashSet.addAll(processes);
        }

        public String toString() {
            return String.format("{ pss: %d, processes: %s, parent: %s }", Long.valueOf(this.mPss), this.mProcesses.toString(), this.mParent);
        }
    }

    /* loaded from: classes.dex */
    public class MemoryStandardMap {
        private Map<String, MemoryStandard> mStandardMap = new HashMap();
        private Set<String> mNativeProcessCmds = new HashSet();

        public MemoryStandardMap() {
        }

        public void updateMemoryStandard(long pss, String packageName, List<String> processes, String parent) {
            MemoryStandard standard = new MemoryStandard(pss, processes, parent);
            this.mStandardMap.put(packageName, standard);
        }

        public void updateNativeMemoryStandard(long pss, String packageName, List<String> processes, String parent) {
            updateMemoryStandard(pss, packageName, processes, parent);
            if (!this.mNativeProcessCmds.contains(packageName)) {
                this.mNativeProcessCmds.add(packageName);
            }
        }

        public void removeMemoryStandard(String packageName) {
            MemoryStandard standard = this.mStandardMap.get(packageName);
            if (standard == null) {
                return;
            }
            if (this.mNativeProcessCmds.contains(packageName)) {
                this.mNativeProcessCmds.remove(packageName);
            }
            this.mStandardMap.remove(packageName);
        }

        public Set<String> getNativeProcessCmds() {
            return this.mNativeProcessCmds;
        }

        public MemoryStandard getStandard(String packageName) {
            return this.mStandardMap.get(packageName);
        }

        public long getPss(String packageName) {
            MemoryStandard memoryStandard = this.mStandardMap.get(packageName);
            if (memoryStandard != null) {
                return memoryStandard.mPss;
            }
            return 0L;
        }

        public boolean isEmpty() {
            return this.mStandardMap.isEmpty();
        }

        public boolean containsPackage(String packageName) {
            return this.mStandardMap.containsKey(packageName);
        }

        public void clear() {
            this.mStandardMap.clear();
            this.mNativeProcessCmds.clear();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Memory Standard:\n");
            for (Map.Entry<String, MemoryStandard> e : this.mStandardMap.entrySet()) {
                sb.append(e.getKey() + ": " + e.getValue().toString() + "\n");
            }
            sb.append("Native Procs:\n");
            Iterator<String> nativeProcIterator = this.mNativeProcessCmds.iterator();
            while (nativeProcIterator.hasNext()) {
                sb.append(nativeProcIterator.next() + "\n");
            }
            return sb.toString();
        }
    }
}
