package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.CachedAppOptimizer;
import com.android.server.am.ProcessPolicy;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.server.AccessController;
import com.miui.server.security.AccessControlImpl;
import com.miui.server.smartpower.IAppState;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class MiuiMemReclaimer {
    public static final long ANON_RSS_LIMIT_KB;
    private static final String COMPACTION_PROC_NAME_CAMERA = "com.android.camera";
    private static final String COMPACTION_PROC_NAME_WALLPAPER = "com.miui.miwallpaper";
    private static final String COMPACTION_PROC_NAME_WECHAT = "com.tencent.mm";
    private static final ArrayList<String> COMPACTION_PROC_NAME_WHITE_LIST;
    private static final String COMPACT_ACTION_ANON = "anon";
    private static final String COMPACT_ACTION_FILE = "file";
    private static final String COMPACT_ACTION_FULL = "all";
    private static final long COMPACT_ALL_MIN_INTERVAL;
    private static final int COMPACT_GLOBALLY_MSG = 100;
    private static final int COMPACT_PROCESSES_MSG = 102;
    private static final int COMPACT_PROCESS_MSG = 101;
    private static final long COMPACT_PROC_MIN_INTERVAL;
    private static final int EVENT_TAG = 80800;
    private static final long FILE_RSS_LIMIT_KB;
    private static final String RECLAIM_EVENT_NODE = "/sys/kernel/mi_reclaim/event";
    private static final boolean RECLAIM_IF_NEEDED;
    private static final String TAG = "MiuiMemoryService";
    private static final long TOTAL_MEMORY;
    private static final boolean USE_LEGACY_COMPACTION;
    private static final int VMPRESS_CRITICAL_RECLAIM_SIZE_MB = 100;
    private static final int VMPRESS_LOW_RECLAIM_SIZE_MB = 30;
    private static final int VMPRESS_MEDIUM_RECLAIM_SIZE_MB = 50;
    private static final int VM_ANON = 2;
    private static final int VM_FILE = 1;
    private static final int VM_RSS = 0;
    private static final int VM_SWAP = 3;
    private static volatile ProcessManagerService sPms;
    private final int MAX_COMPACTION_COUNT;
    private boolean mBatteryLow;
    private final BroadcastReceiver mBatteryReceiver;
    private final Map<Integer, CompactProcInfo> mCompactionStats;
    private final Handler mCompactorHandler;
    private final HandlerThread mCompactorThread;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private long mLastCompactionTimeMillis;
    private ProcessPolicy mProcessPolicy;
    private SmartPowerServiceInternal mSmartPowerService;
    private boolean DEBUG = MiuiMemoryService.DEBUG;
    private boolean mInterruptNeeded = false;
    private CachedAppOptimizer.DefaultProcessDependencies mProcessDependencies = new CachedAppOptimizer.DefaultProcessDependencies();

    static {
        long totalMemory = Process.getTotalMemory() >> 30;
        TOTAL_MEMORY = totalMemory;
        RECLAIM_IF_NEEDED = totalMemory < 8;
        USE_LEGACY_COMPACTION = SystemProperties.getBoolean("persist.sys.mms.use_legacy", false);
        ANON_RSS_LIMIT_KB = SystemProperties.getLong("persist.sys.mms.anon_rss", 5000L);
        FILE_RSS_LIMIT_KB = SystemProperties.getLong("persist.sys.mms.file_rss", ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION);
        COMPACT_ALL_MIN_INTERVAL = SystemProperties.getLong("persist.sys.mms.compact_min_interval", AccessControlImpl.LOCK_TIME_OUT);
        COMPACT_PROC_MIN_INTERVAL = SystemProperties.getLong("persist.sys.mms.compact_proc_min_interval", 10000L);
        ArrayList<String> arrayList = new ArrayList<>();
        COMPACTION_PROC_NAME_WHITE_LIST = arrayList;
        arrayList.add("com.miui.home");
        arrayList.add(AccessController.PACKAGE_SYSTEMUI);
        arrayList.add("com.miui.screenrecorder");
    }

    /* loaded from: classes.dex */
    private class CompactorHandler extends Handler {
        public CompactorHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    MiuiMemReclaimer.reclaimPage(msg.arg1);
                    return;
                case 101:
                    MiuiMemReclaimer.this.performCompactProcess((IAppState.IRunningProcess) msg.obj, msg.arg1);
                    return;
                case 102:
                    MiuiMemReclaimer.this.performCompactProcesses(msg.arg1);
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    private boolean isLowBattery() {
        return this.mBatteryLow;
    }

    public MiuiMemReclaimer(Context context) {
        this.MAX_COMPACTION_COUNT = this.DEBUG ? 1000 : MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
        this.mCompactionStats = new LinkedHashMap<Integer, CompactProcInfo>() { // from class: com.android.server.am.MiuiMemReclaimer.1
            @Override // java.util.LinkedHashMap
            protected boolean removeEldestEntry(Map.Entry<Integer, CompactProcInfo> entry) {
                return size() > MiuiMemReclaimer.this.MAX_COMPACTION_COUNT;
            }
        };
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.am.MiuiMemReclaimer.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                    MiuiMemReclaimer.this.mBatteryLow = intent.getBooleanExtra("battery_low", false);
                }
            }
        };
        this.mBatteryReceiver = broadcastReceiver;
        HandlerThread handlerThread = new HandlerThread("MiuiMemoryService_Compactor");
        this.mCompactorThread = handlerThread;
        handlerThread.start();
        this.mCompactorHandler = new CompactorHandler(handlerThread.getLooper());
        Process.setThreadGroupAndCpuset(handlerThread.getThreadId(), 2);
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        this.mContext = context;
        this.mBatteryLow = false;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        this.mIntentFilter = intentFilter;
        this.mContext.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void reclaimPage(int reclaimSizeMB) {
        if (RECLAIM_IF_NEEDED) {
            writeToNode(RECLAIM_EVENT_NODE, reclaimSizeMB);
        }
    }

    public static void reclaimPage() {
        if (RECLAIM_IF_NEEDED) {
            writeToNode(RECLAIM_EVENT_NODE, 1);
        }
    }

    public static void enterSt() {
        if (RECLAIM_IF_NEEDED) {
            writeToNode(RECLAIM_EVENT_NODE, 2);
        }
    }

    public static void cancelSt() {
        if (RECLAIM_IF_NEEDED) {
            writeToNode(RECLAIM_EVENT_NODE, 0);
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:20:0x0066 -> B:9:0x0077). Please report as a decompilation issue!!! */
    public static void writeToNode(String node, int value) {
        FileWriter writer = null;
        File file = new File(node);
        String commMsg = " " + node + ":" + value;
        String errMsg = "error" + commMsg;
        String str = "success" + commMsg;
        try {
            try {
            } catch (IOException e) {
                Slog.e("MiuiMemoryService", errMsg, e);
            }
            if (!file.exists()) {
                return;
            }
            try {
                writer = new FileWriter(file);
                writer.write(String.valueOf(value));
                writer.close();
            } catch (IOException e2) {
                Slog.e("MiuiMemoryService", errMsg, e2);
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    Slog.e("MiuiMemoryService", errMsg, e3);
                }
            }
            throw th;
        }
    }

    public void dumpCompactionStats(PrintWriter pw) {
        if (this.mCompactionStats.size() == 0) {
            pw.println("Compaction never done!");
            return;
        }
        int num = 0;
        pw.println("Compaction Statistics:");
        synchronized (this.mCompactionStats) {
            for (Map.Entry<Integer, CompactProcInfo> entry : this.mCompactionStats.entrySet()) {
                num++;
                pw.println("No." + num + ": " + entry.getValue().toString());
            }
        }
    }

    public void runGlobalCompaction(int vmPressureLevel) {
        int reclaimSizeMB = -1;
        switch (vmPressureLevel) {
            case 0:
                reclaimSizeMB = 30;
                break;
            case 1:
                reclaimSizeMB = 50;
                break;
            case 2:
            case 3:
                reclaimSizeMB = 100;
                break;
        }
        if (reclaimSizeMB < 0) {
            return;
        }
        this.mCompactorHandler.sendMessage(generateMessage(100, reclaimSizeMB, null));
        if (this.DEBUG) {
            Slog.d("MiuiMemoryService", "Global reclaim " + reclaimSizeMB + "MB.");
        }
    }

    public void runProcCompaction(IAppState.IRunningProcess proc, int mode) {
        if (proc == null || proc.getPid() <= 0 || proc.isSystemApp() || !isCompactNeeded(proc, mode)) {
            return;
        }
        this.mCompactorHandler.sendMessage(generateMessage(101, mode, proc));
    }

    public void runProcsCompaction(int mode) {
        synchronized (this) {
            this.mInterruptNeeded = false;
        }
        this.mCompactorHandler.sendMessage(generateMessage(102, mode, null));
    }

    public synchronized void interruptProcsCompaction() {
        this.mInterruptNeeded = true;
    }

    public synchronized void interruptProcCompaction(int pid) {
        if (CachedAppOptimizer.DefaultProcessDependencies.mPidCompacting == pid) {
            this.mProcessDependencies.interruptProcCompaction();
        }
    }

    public void setAppStartingMode(boolean appStarting) {
        this.mProcessDependencies.setAppStartingMode(appStarting);
    }

    private Message generateMessage(int what, int arg1, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        if (arg1 != -1) {
            msg.arg1 = arg1;
        }
        if (obj != null) {
            msg.obj = obj;
        }
        return msg;
    }

    private boolean isProtectProcess(int mode, int uid, String pkgName, String procName) {
        if (this.mSmartPowerService.isProcessPerceptible(uid, procName) || this.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, pkgName, procName)) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performCompactProcess(IAppState.IRunningProcess proc, int mode) {
        if (proc == null || proc.getPid() <= 0 || proc.isKilled()) {
            return;
        }
        CompactProcInfo info = new CompactProcInfo(proc, mode);
        if (!isCompactSatisfied(info)) {
            if (this.DEBUG) {
                Slog.d("MiuiMemoryService", "Proc " + info.getPid() + " isn't compact satisfied");
                return;
            }
            return;
        }
        if (info.proc.isKilled() || !isAdjInCompactRange(info.proc.getAdj(), mode) || !isCompactNeeded(proc, mode)) {
            return;
        }
        if (SystemPressureController.getInstance().isStartingApp()) {
            synchronized (SystemPressureController.getInstance().mStartingAppLock) {
                if (SystemPressureController.getInstance().isStartingApp()) {
                    try {
                        SystemPressureController.getInstance().mStartingAppLock.wait(2000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        long startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(1L, "MiuiCompact:" + info.action + " " + info.getPid() + ":" + info.getProcessName() + "-" + info.proc.getAdj());
        performCompaction(info.action, info.getPid());
        Trace.traceEnd(1L);
        info.lastCompactTimeMillis = System.currentTimeMillis();
        info.compactDurationMillis = SystemClock.uptimeMillis() - startTime;
        info.computeRssDiff(true);
        synchronized (this.mCompactionStats) {
            this.mCompactionStats.remove(Integer.valueOf(info.getPid()));
            this.mCompactionStats.put(Integer.valueOf(info.getPid()), info);
        }
        EventLog.writeEvent(80800, "Compacted proc " + info.toString());
        if (this.DEBUG) {
            Slog.d("MiuiMemoryService", "Compacted proc " + info.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performCompactProcesses(int mode) {
        if (mode != 1 && SystemClock.uptimeMillis() - this.mLastCompactionTimeMillis < COMPACT_ALL_MIN_INTERVAL) {
            if (this.DEBUG) {
                Slog.d("MiuiMemoryService", "Skip compaction for frequently");
                return;
            }
            return;
        }
        if (this.mInterruptNeeded) {
            Slog.w("MiuiMemoryService", "Compact processes skipped");
            return;
        }
        List<CompactProcInfo> compactTargetProcs = filterCompactionProcs(mode);
        if (compactTargetProcs.isEmpty()) {
            Slog.w("MiuiMemoryService", "No process can compact.");
            return;
        }
        for (CompactProcInfo info : compactTargetProcs) {
            if (this.mInterruptNeeded) {
                Slog.w("MiuiMemoryService", "Compact processes interrupted");
                return;
            }
            if (info.proc.isKilled() || !isAdjInCompactRange(info.proc.getAmsProcState(), mode)) {
                Slog.w("MiuiMemoryService", info.getProcessName() + " changed, compaction skipped.");
            } else {
                long startTime = SystemClock.uptimeMillis();
                Trace.traceBegin(1L, "MiuiCompact:" + info.action + " " + info.getPid() + ":" + info.getProcessName() + "-" + info.proc.getAdj());
                performCompaction(info.action, info.getPid());
                Trace.traceEnd(1L);
                info.lastCompactTimeMillis = System.currentTimeMillis();
                info.compactDurationMillis = SystemClock.uptimeMillis() - startTime;
                info.computeRssDiff(true);
                synchronized (this.mCompactionStats) {
                    this.mCompactionStats.remove(Integer.valueOf(info.getPid()));
                    this.mCompactionStats.put(Integer.valueOf(info.getPid()), info);
                }
                EventLog.writeEvent(80800, "Compacted proc " + info.toString());
                if (this.DEBUG) {
                    Slog.d("MiuiMemoryService", "Compacted proc " + info.toString());
                }
            }
        }
        this.mLastCompactionTimeMillis = SystemClock.uptimeMillis();
        Slog.i("MiuiMemoryService", "Compact processes success! Compact mode: " + mode);
    }

    private List<CompactProcInfo> filterCompactionProcs(int mode) {
        List<CompactProcInfo> targetProcs = new ArrayList<>();
        ArrayList<IAppState> appStateList = this.mSmartPowerService.getAllAppState();
        Iterator<IAppState> it = appStateList.iterator();
        while (it.hasNext()) {
            IAppState appState = it.next();
            if (!appState.isVsible()) {
                Iterator it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    IAppState.IRunningProcess runningProc = (IAppState.IRunningProcess) it2.next();
                    if (runningProc.getAdj() > 0 && isCompactNeeded(runningProc, mode)) {
                        CompactProcInfo info = new CompactProcInfo(runningProc, mode);
                        if (isCompactSatisfied(info)) {
                            targetProcs.add(info);
                        }
                    }
                }
            }
        }
        if (!targetProcs.isEmpty()) {
            sortProcsByRss(targetProcs, COMPACT_ACTION_FULL);
            logTargetProcsDetails(targetProcs, COMPACT_ACTION_FULL);
        }
        return targetProcs;
    }

    private Set<Integer> getActiveUidSet() {
        Set<Integer> activeUidSet = new HashSet<>();
        if (this.mProcessPolicy == null) {
            if (getProcessManagerService() == null) {
                return activeUidSet;
            }
            this.mProcessPolicy = getProcessManagerService().getProcessPolicy();
        }
        List<ProcessPolicy.ActiveUidRecord> activeUidRecords = this.mProcessPolicy.getActiveUidRecordList(3);
        for (ProcessPolicy.ActiveUidRecord record : activeUidRecords) {
            activeUidSet.add(Integer.valueOf(record.uid));
        }
        return activeUidSet;
    }

    private static ProcessManagerService getProcessManagerService() {
        if (sPms == null) {
            sPms = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        }
        return sPms;
    }

    private void sortProcsByRss(List<CompactProcInfo> infoList, final String action) {
        if (infoList == null || infoList.isEmpty()) {
            return;
        }
        Collections.sort(infoList, new Comparator<CompactProcInfo>() { // from class: com.android.server.am.MiuiMemReclaimer.3
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // java.util.Comparator
            public int compare(CompactProcInfo t1, CompactProcInfo t2) {
                char c;
                String str = action;
                switch (str.hashCode()) {
                    case 96673:
                        if (str.equals(MiuiMemReclaimer.COMPACT_ACTION_FULL)) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2998988:
                        if (str.equals(MiuiMemReclaimer.COMPACT_ACTION_ANON)) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3143036:
                        if (str.equals(MiuiMemReclaimer.COMPACT_ACTION_FILE)) {
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
                        return Long.compare(t2.rss[0], t1.rss[0]);
                    case 1:
                        return Long.compare(t2.rss[1], t1.rss[1]);
                    case 2:
                        return Long.compare(t2.rss[2], t1.rss[2]);
                    default:
                        return Long.compare(t2.rss[0], t1.rss[0]);
                }
            }
        });
    }

    private void logTargetProcsDetails(List<CompactProcInfo> infoList, String action) {
        if (infoList != null && !infoList.isEmpty() && this.DEBUG) {
            int num = 0;
            for (CompactProcInfo info : infoList) {
                if (info.getPid() != 0) {
                    num++;
                    Slog.d("MiuiMemoryService", String.format("No.%s Proc %s:%s action=%s rss[%s, %s, %s, %s]", Integer.valueOf(num), Integer.valueOf(info.getPid()), info.getProcessName(), action, Long.valueOf(info.rss[0]), Long.valueOf(info.rss[1]), Long.valueOf(info.rss[2]), Long.valueOf(info.rss[3])));
                }
            }
        }
    }

    private boolean isCameraMode() {
        return "com.android.camera".equals(SystemPressureController.getInstance().getForegroundPackageName());
    }

    public boolean isCompactNeeded(IAppState.IRunningProcess proc, int mode) {
        String procName = proc.getProcessName();
        String pkgName = proc.getPackageName();
        int uid = proc.getUid();
        int adj = proc.getAdj();
        if ((mode == 4 && (ProcessListStub.get().isGameMode() || isCameraMode() || isLowBattery())) || COMPACTION_PROC_NAME_WHITE_LIST.contains(procName) || procName.startsWith(COMPACTION_PROC_NAME_WALLPAPER) || ((mode == 0 && TextUtils.equals("com.android.camera", procName)) || ((mode != 2 && TextUtils.equals(COMPACTION_PROC_NAME_WECHAT, procName)) || ((mode == 3 && !TextUtils.equals(pkgName, procName)) || uid <= 1000 || !isAdjInCompactRange(adj, mode) || isProtectProcess(mode, uid, pkgName, procName))))) {
            return false;
        }
        if (mode == 4) {
            long nowTime = System.currentTimeMillis();
            CompactProcInfo procInfo = this.mCompactionStats.get(Integer.valueOf(proc.getPid()));
            if ((procInfo != null && nowTime - procInfo.lastCompactTimeMillis < COMPACT_PROC_MIN_INTERVAL && procInfo.action.equals(CompactProcInfo.genCompactAction(proc.getAmsProcState(), mode))) || this.mSmartPowerService.isUidVisible(uid) || OomAdjusterImpl.ADJ_TYPE_CAMERA_IMPROVE.equals(proc.getAdjType())) {
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean isAdjInCompactRange(int adj, int mode) {
        switch (mode) {
            case 0:
                return adj > 100 && adj < 900;
            case 1:
            case 2:
            default:
                return adj >= 100;
            case 3:
                return adj > 200 && adj < 800;
            case 4:
                return adj >= 100 && adj <= 950;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0041, code lost:
    
        if (r0.equals(com.android.server.am.MiuiMemReclaimer.COMPACT_ACTION_FULL) != false) goto L20;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isCompactSatisfied(com.android.server.am.MiuiMemReclaimer.CompactProcInfo r15) {
        /*
            Method dump skipped, instructions count: 292
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.MiuiMemReclaimer.isCompactSatisfied(com.android.server.am.MiuiMemReclaimer$CompactProcInfo):boolean");
    }

    public void performCompaction(String action, int pid) {
        if (USE_LEGACY_COMPACTION) {
            performCompactionLegacy(action, pid);
        } else {
            performCompactionNew(action, pid);
        }
    }

    private static void performCompactionLegacy(String action, int pid) {
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = new FileOutputStream("/proc/" + pid + "/reclaim");
                    fos.write(action.getBytes());
                    fos.close();
                } catch (IOException e) {
                    Slog.e("MiuiMemoryService", "Compaction failed: pid " + pid);
                    if (fos != null) {
                        fos.close();
                    }
                }
            } catch (Throwable th) {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:6:0x0027. Please report as an issue. */
    private void performCompactionNew(String action, int pid) {
        char c;
        try {
            switch (action.hashCode()) {
                case 96673:
                    if (action.equals(COMPACT_ACTION_FULL)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 2998988:
                    if (action.equals(COMPACT_ACTION_ANON)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 3143036:
                    if (action.equals(COMPACT_ACTION_FILE)) {
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
                    this.mProcessDependencies.performCompaction(CachedAppOptimizer.CompactProfile.FULL, pid);
                case 1:
                    this.mProcessDependencies.performCompaction(CachedAppOptimizer.CompactProfile.SOME, pid);
                case 2:
                    this.mProcessDependencies.performCompaction(CachedAppOptimizer.CompactProfile.ANON, pid);
                    return;
                default:
                    return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CompactProcInfo {
        public final String action;
        public final IAppState.IRunningProcess proc;
        public long[] rss;
        public long[] rssAfter;
        public long[] rssDiff = {0, 0, 0, 0};
        public long lastCompactTimeMillis = -1;
        public long compactDurationMillis = -1;

        public CompactProcInfo(IAppState.IRunningProcess proc, int mode) {
            this.proc = proc;
            this.action = genCompactAction(proc.getAmsProcState(), mode);
            if (proc.getPid() > 0) {
                this.rss = Process.getRss(proc.getPid());
            } else {
                this.rss = new long[]{0, 0, 0, 0};
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getPid() {
            return this.proc.getPid();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getProcessName() {
            return this.proc.getProcessName();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String genCompactAction(int procState, int mode) {
            if (OomAdjusterImpl.isCacheProcessState(procState)) {
                return MiuiMemReclaimer.COMPACT_ACTION_FULL;
            }
            if (mode == 2 && procState == 15) {
                return MiuiMemReclaimer.COMPACT_ACTION_FULL;
            }
            return MiuiMemReclaimer.COMPACT_ACTION_ANON;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isRssValid() {
            long[] jArr = this.rss;
            if (jArr == null) {
                return false;
            }
            return (jArr[0] == 0 && jArr[1] == 0 && jArr[2] == 0 && jArr[3] == 0) ? false : true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void computeRssDiff(boolean isCompacted) {
            if (!isCompacted) {
                this.rssAfter = this.rss;
                return;
            }
            long[] rss = Process.getRss(this.proc.getPid());
            this.rssAfter = rss;
            long[] jArr = this.rssDiff;
            long[] jArr2 = this.rss;
            jArr[0] = jArr2[0] - rss[0];
            jArr[1] = jArr2[1] - rss[1];
            jArr[2] = jArr2[2] - rss[2];
            jArr[3] = jArr2[3] - rss[3];
            this.proc.updatePss();
        }

        public String toString() {
            String date = null;
            if (this.lastCompactTimeMillis > 0) {
                SimpleDateFormat dateformat = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SS");
                date = dateformat.format(Long.valueOf(this.lastCompactTimeMillis));
            }
            return String.format("%d %s uid:%d adj:%d rss[%d, %d, %d, %d] rssAfter[%d, %d, %d, %d] rssDiff[%d, %d, %d, %d] lastCompactTime:%s compactDuration:%dms action:%s", Integer.valueOf(this.proc.getPid()), this.proc.getProcessName(), Integer.valueOf(this.proc.getUid()), Integer.valueOf(this.proc.getAdj()), Long.valueOf(this.rss[0]), Long.valueOf(this.rss[1]), Long.valueOf(this.rss[2]), Long.valueOf(this.rss[3]), Long.valueOf(this.rssAfter[0]), Long.valueOf(this.rssAfter[1]), Long.valueOf(this.rssAfter[2]), Long.valueOf(this.rssAfter[3]), Long.valueOf(this.rssDiff[0]), Long.valueOf(this.rssDiff[1]), Long.valueOf(this.rssDiff[2]), Long.valueOf(this.rssDiff[3]), date, Long.valueOf(this.compactDurationMillis), this.action);
        }
    }
}
