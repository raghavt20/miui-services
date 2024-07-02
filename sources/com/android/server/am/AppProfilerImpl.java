package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.incremental.IncrementalMetrics;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.app.IPerfShielder;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.PsiParser;
import com.android.server.ResourcePressureUtil;
import com.android.server.ScoutHelper;
import com.android.server.ScoutSystemMonitor;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.utils.PriorityDump;
import com.miui.base.MiuiStubRegistry;
import com.miui.daemon.performance.PerfShielderManager;
import com.miui.server.sentinel.MiuiSentinelMemoryManager;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import com.miui.server.stability.ScoutMemoryUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.util.DeviceLevel;

/* loaded from: classes.dex */
public class AppProfilerImpl implements AppProfilerStub {
    private static final String CMD_DUMPSYS = "dumpsys";
    private static final long DEFAULT_DUMP_CPU_TIMEOUT_MILLISECONDS = 3000;
    private static final String DUMP_CPU_TIMEOUT_PROPERTY = "persist.sys.stability.dump_cpu.timeout";
    private static final int DUMP_PROCS_MEM_INTERVAL_MILLIS = 300000;
    private static final int LOW_MEM_FACTOR = 10;
    private static final int MAX_LOW_MEM_FILE_NUM = 5;
    private static final String NATIVE_MEM_INFO = "native";
    private static final String OOM_MEM_INFO = "OomMeminfo";
    private static final int PSS_NORMAL_THRESHOLD = 307200;
    static final int STATE_START = 1;
    static final int STATE_STOP = 2;
    private static final String TAG = "AppProfilerImpl";
    private long mLastMemUsageReportTime;
    private ActivityManagerService mService;
    private HashMap<String, Integer> pssThresholdMap;
    private static final int[] MEMINFO_FORMAT = {288, 8224, 10, 288, 8224, 10, 288, 8224, 10};
    private static boolean testTrimMemActivityBg_workaround = SystemProperties.getBoolean("persist.sys.testTrimMemActivityBg.wk.enable", false);
    public static final int STALL_RATIO_SOME = SystemProperties.getInt("persist.sys.stall_ratio_some", 75);
    public static final int STALL_RATIO_FULL = SystemProperties.getInt("persist.sys.stall_ratio_full", 50);
    private int mMinOomScore = 1001;
    private long mLastDumpProcsMemTime = -240000;
    private final long REPORT_OOM_MEMINFO_INTERVAL_MILLIS = SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED;
    private long mLastReportOomMemTime = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppProfilerImpl> {

        /* compiled from: AppProfilerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppProfilerImpl INSTANCE = new AppProfilerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AppProfilerImpl m312provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AppProfilerImpl m311provideNewInstance() {
            return new AppProfilerImpl();
        }
    }

    public void init(ActivityManagerService ams) {
        this.mService = ams;
        MiuiSentinelMemoryManager.getInstance().init(ams, ams.mContext);
        ScoutDisplayMemoryManager.getInstance().init(ams, ams.mContext);
        initPssThresholdMap();
    }

    public void reportScoutLowMemory(ScoutMeminfo scoutMeminfo) {
        ScoutDisplayMemoryManager.getInstance().checkScoutLowMemory(scoutMeminfo);
    }

    public void reportScoutLowMemory(int adj) {
        if (adj < this.mMinOomScore) {
            this.mMinOomScore = adj;
        }
        if (this.mService == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (adj >= 0 && adj <= 600 && now > this.mLastDumpProcsMemTime + 300000 && isSystemLowMemPsi()) {
            Slog.e("MIUIScout Memory", "Scout dump proc mem, current killed adj= " + adj);
            ScoutSystemMonitor.getInstance().setWorkMessage(1);
            this.mLastDumpProcsMemTime = now;
        }
        if (ScoutDisplayMemoryManager.getInstance().isEnableScoutMemory() && adj >= 0 && adj <= 600 && now > this.mLastMemUsageReportTime + 180000 && isSystemLowMemPsi()) {
            this.mLastMemUsageReportTime = now;
            Slog.e("MIUIScout Memory", "Scout report Low memory,currently killed process adj= " + adj);
            ScoutSystemMonitor.getInstance().setWorkMessage(0);
        }
    }

    public void reportScoutLowMemoryState(int state) {
        if (state == 2) {
            if (this.mMinOomScore <= 600) {
                long timestamp = SystemClock.elapsedRealtime();
                MQSEventManagerDelegate.getInstance().reportLmkKill(this.mMinOomScore, timestamp);
            }
            this.mMinOomScore = 1001;
        }
    }

    public void reportPssRecord(String processName, String packageName, long pss) {
        PackageInfo pi;
        String versionName = "1.0";
        int versionCode = 1;
        try {
            if (!OOM_MEM_INFO.equals(packageName) && !NATIVE_MEM_INFO.equals(packageName)) {
                IPackageManager pm = AppGlobals.getPackageManager();
                if (pm == null || (pi = pm.getPackageInfo(packageName, 0L, ActivityManager.getCurrentUser())) == null) {
                    return;
                }
                versionName = pi.versionName;
                versionCode = pi.versionCode;
            }
            PerfShielderManager.getService().reportPssRecord(processName, packageName, pss, versionName, versionCode);
        } catch (RemoteException e) {
        }
    }

    public void reportMemoryStandardProcessControlKillMessage(String processName, int pid, int uid, long pss) {
        try {
            IPerfShielder perfShielder = PerfShielderManager.getService();
            if (perfShielder != null) {
                perfShielder.reportKillMessage(processName, pid, uid, pss);
            }
        } catch (RemoteException e) {
        }
    }

    public void reportOomMemRecordIfNeeded(ActivityManagerService ams, ProcessCpuTracker processCpuTracker) {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastReportOomMemTime < SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED) {
            return;
        }
        this.mLastReportOomMemTime = now;
        SparseIntArray procs = new SparseIntArray();
        synchronized (ams) {
            ArrayList<ProcessRecord> lruProcs = ams.mProcessList.getLruProcessesLOSP();
            for (int i = lruProcs.size() - 1; i >= 0; i--) {
                ProcessRecord r = lruProcs.get(i);
                procs.put(r.mPid, r.mState.getSetAdjWithServices());
            }
        }
        SparseArray<String> nativeProcs = new SparseArray<>();
        ams.updateCpuStatsNow();
        synchronized (processCpuTracker) {
            int N = processCpuTracker.countStats();
            for (int i2 = 0; i2 < N; i2++) {
                ProcessCpuTracker.Stats st = processCpuTracker.getStats(i2);
                if (st.vsize > 0 && procs.indexOfKey(st.pid) < 0) {
                    nativeProcs.put(st.pid, st.name);
                }
            }
        }
        long[] oomPss = new long[ActivityManagerService.DUMP_MEM_OOM_LABEL.length];
        for (int i3 = 0; i3 < procs.size(); i3++) {
            int pid = procs.keyAt(i3);
            int oomAdj = procs.get(pid);
            long myTotalPss = Debug.getPss(pid, null, null);
            for (int oomIndex = 0; oomIndex < oomPss.length; oomIndex++) {
                if (oomIndex == oomPss.length - 1 || (oomAdj >= ActivityManagerService.DUMP_MEM_OOM_ADJ[oomIndex] && oomAdj < ActivityManagerService.DUMP_MEM_OOM_ADJ[oomIndex + 1])) {
                    oomPss[oomIndex] = oomPss[oomIndex] + myTotalPss;
                    break;
                }
            }
        }
        for (int i4 = 0; i4 < nativeProcs.size(); i4++) {
            int pid2 = nativeProcs.keyAt(i4);
            String name = nativeProcs.get(pid2);
            long myTotalPss2 = Debug.getPss(pid2, null, null);
            if (myTotalPss2 != 0) {
                oomPss[0] = oomPss[0] + myTotalPss2;
                reportPssRecord(name, NATIVE_MEM_INFO, myTotalPss2);
            }
        }
        for (int i5 = 0; i5 < ActivityManagerService.DUMP_MEM_OOM_LABEL.length; i5++) {
            if (oomPss[i5] != 0) {
                reportPssRecord("OomMeminfo." + ActivityManagerService.DUMP_MEM_OOM_LABEL[i5], OOM_MEM_INFO, oomPss[i5]);
            }
        }
    }

    public void reportPackagePss(ActivityManagerService ams, String packageName) {
        long pkgPss = 0;
        synchronized (ams) {
            ArrayList<ProcessRecord> lruProcs = ams.mProcessList.getLruProcessesLOSP();
            for (int i = lruProcs.size() - 1; i >= 0; i--) {
                ProcessRecord r = lruProcs.get(i);
                if (r.info.packageName.equals(packageName)) {
                    if (r.mProfile.getLastPss() == 0) {
                        return;
                    } else {
                        pkgPss += r.mProfile.getLastPss();
                    }
                }
            }
            reportPssRecordIfNeeded("Package." + packageName, packageName, pkgPss);
        }
    }

    private void initPssThresholdMap() {
        String listName;
        Slog.d(TAG, "Init Memory ThresholdMap");
        if (this.pssThresholdMap == null) {
            this.pssThresholdMap = new HashMap<>();
            if (DeviceLevel.IS_MIUI_LITE_VERSION) {
                listName = "package_pss_threshold_lite";
            } else {
                listName = "package_pss_threshold";
            }
            try {
                int pssThresholdId = this.mService.mContext.getResources().getIdentifier(listName, "array", "android.miui");
                if (pssThresholdId > 0) {
                    String[] pssThresholdList = this.mService.mContext.getResources().getStringArray(pssThresholdId);
                    for (String str : pssThresholdList) {
                        List<String> packageThreshold = Arrays.asList(str.split(","));
                        int packagePssThreshold = Integer.parseInt(packageThreshold.get(1)) * 1024;
                        this.pssThresholdMap.put(packageThreshold.get(0), Integer.valueOf(packagePssThreshold));
                    }
                    Slog.d(TAG, listName + " size:" + pssThresholdList.length);
                    return;
                }
                Slog.d(TAG, "No Memory Threshold Id");
            } catch (Exception e) {
            }
        }
    }

    private void reportPssRecordIfNeeded(String processName, String packageName, long pss) {
        HashMap<String, Integer> hashMap = this.pssThresholdMap;
        if (hashMap == null || !hashMap.containsKey(packageName)) {
            if (pss > 307200) {
                reportPssRecord("Package." + packageName, packageName, pss);
            }
        } else if (pss > this.pssThresholdMap.get(packageName).intValue()) {
            reportPssRecord("Package." + packageName, packageName, pss);
        }
    }

    public boolean isSystemLowMem() {
        long[] longs = new long[3];
        if (!Process.readProcFile("/proc/meminfo", MEMINFO_FORMAT, null, longs, null)) {
            Slog.e(TAG, "Read file /proc/meminfo failed!");
            return false;
        }
        long memAvailableKb = longs[2];
        if (memAvailableKb == 0) {
            Slog.e(TAG, "MemAvailable is 0! This should never happen!");
            return false;
        }
        long memTotalKb = longs[0];
        return memTotalKb / memAvailableKb >= 10;
    }

    public void updateCameraForegroundState(boolean isCameraForeground) {
        ScoutDisplayMemoryManager.getInstance().updateCameraForegroundState(isCameraForeground);
    }

    public boolean testTrimMemActivityBgWk(String processName) {
        if (testTrimMemActivityBg_workaround && processName.contains("com.android.app1:android.app.stubs.TrimMemService:trimmem_")) {
            Slog.i(TAG, "special case.");
            return true;
        }
        return false;
    }

    public void reportMemUsage(ArrayList<ProcessMemInfo> memInfos) {
        ScoutMeminfo scoutInfo = new ScoutMeminfo();
        MemUsageBuilder builder = new MemUsageBuilder(this.mService.mAppProfiler, memInfos, "Low on memory");
        builder.setScoutInfo(scoutInfo);
        builder.buildAll();
        Slog.i(TAG, "Low on memory:");
        Slog.i(TAG, builder.shortNative);
        Slog.i(TAG, builder.fullJava);
        Slog.i(TAG, builder.summary);
        scoutInfo.setTotalPss(builder.totalPss);
        scoutInfo.setTotalSwapPss(builder.totalSwapPss);
        scoutInfo.setCachedPss(builder.cachedPss);
        AppProfilerStub.getInstance().reportScoutLowMemory(scoutInfo);
        String dropboxStr = buildMemInfo(builder, true);
        FrameworkStatsLog.write(81);
        this.mService.addErrorToDropBox("lowmem", (ProcessRecord) null, "system_server", (String) null, (String) null, (ProcessRecord) null, builder.subject, dropboxStr, (File) null, (ApplicationErrorReport.CrashInfo) null, (Float) null, (IncrementalMetrics) null, (UUID) null);
        synchronized (this.mService) {
            long now = SystemClock.uptimeMillis();
            if (this.mLastMemUsageReportTime < now) {
                this.mLastMemUsageReportTime = now;
            }
        }
    }

    public void dumpProcsMemInfo() {
        String dropboxStr = getMemInfo(true);
        File exFile = ScoutMemoryUtils.getExceptionFile("scout_procs", 3);
        boolean suc = ScoutMemoryUtils.dumpInfoToFile(dropboxStr, exFile);
        Slog.w(TAG, "Dump processes memory to file: " + exFile + ". Succeeded? " + suc);
        ScoutMemoryUtils.deleteOldFiles(3, 5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getMemInfo$0(ProcessRecord rec) {
        return rec.getThread() != null;
    }

    public String getMemInfo(boolean isDropbox) {
        ArrayList<ProcessMemInfo> memInfos = collectProcessMemInfos(new Predicate() { // from class: com.android.server.am.AppProfilerImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppProfilerImpl.lambda$getMemInfo$0((ProcessRecord) obj);
            }
        });
        MemUsageBuilder builder = new MemUsageBuilder(this.mService.mAppProfiler, memInfos, "Scout low memory");
        builder.buildAll();
        return buildMemInfo(builder, isDropbox);
    }

    public boolean isSystemLowMemPsi() {
        try {
            PsiParser.Psi psi = PsiParser.currentParsedPsiState(PsiParser.ResourceType.MEM);
            if (psi.full.avg10 <= 2.0f) {
                if (psi.some.avg10 <= 10.0f) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Get psi failed: " + e + ". Use isSystemLowMem instead.");
            return isSystemLowMem();
        }
    }

    public boolean isSystemLowMemPsiCritical() {
        try {
            PsiParser.Psi psi = PsiParser.currentParsedPsiState(PsiParser.ResourceType.MEM);
            float f = psi.some.avg10;
            int i = STALL_RATIO_SOME;
            if (f < i && psi.some.avg60 < i) {
                float f2 = psi.full.avg10;
                int i2 = STALL_RATIO_FULL;
                if (f2 < i2) {
                    if (psi.full.avg60 < i2) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Get psi failed: " + e + ". Use isSystemLowMem instead.");
            return isSystemLowMem();
        }
    }

    public boolean dumpCpuInfo(final PriorityDump.PriorityDumper dumper, final FileDescriptor fd, final PrintWriter pw, final String[] args) {
        FutureTask<?> task = new FutureTask<>(new Callable<Void>() { // from class: com.android.server.am.AppProfilerImpl.1
            @Override // java.util.concurrent.Callable
            public Void call() throws Exception {
                PriorityDump.dump(dumper, fd, pw, args);
                return null;
            }
        });
        Executor executor = new Executor() { // from class: com.android.server.am.AppProfilerImpl$$ExternalSyntheticLambda2
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                new Thread(runnable, "CPU-Dumper").start();
            }
        };
        executor.execute(task);
        try {
            task.get(getDumpCpuTimeoutThreshold(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e2) {
            e2.printStackTrace();
        } catch (TimeoutException e3) {
            Slog.w(TAG, "dump cpuinfo timeout, interrupted!");
            task.cancel(true);
            int callingPid = Binder.getCallingPid();
            String procName = ScoutHelper.getProcessCmdline(callingPid);
            if (!TextUtils.isEmpty(procName) && procName.contains("dumpsys")) {
                Process.killProcess(callingPid);
            }
        }
        return true;
    }

    private long getDumpCpuTimeoutThreshold() {
        return SystemProperties.getLong(DUMP_CPU_TIMEOUT_PROPERTY, 3000L);
    }

    private String buildMemInfo(MemUsageBuilder builder, boolean isDropbox) {
        StringBuilder dropBuilder = new StringBuilder(1024);
        if (isDropbox) {
            String psi = ResourcePressureUtil.currentPsiState();
            dropBuilder.append("Subject: ").append(builder.subject).append('\n');
            dropBuilder.append("Build: ").append(Build.FINGERPRINT).append("\n\n");
            dropBuilder.append(builder.title).append(":");
            dropBuilder.append(psi);
        }
        String psi2 = builder.stack;
        dropBuilder.append(psi2).append("\n\n");
        dropBuilder.append(builder.topProcs).append("\n");
        dropBuilder.append(builder.fullNative);
        dropBuilder.append(builder.fullJava).append('\n');
        dropBuilder.append(builder.summary).append('\n');
        if (isDropbox) {
            StringWriter catSw = new StringWriter();
            synchronized (this.mService) {
                FastPrintWriter fastPrintWriter = new FastPrintWriter(catSw, false, 256);
                String[] emptyArgs = new String[0];
                fastPrintWriter.println();
                synchronized (this.mService.mProcLock) {
                    this.mService.mProcessList.dumpProcessesLSP((FileDescriptor) null, fastPrintWriter, emptyArgs, 0, false, (String) null, -1);
                }
                fastPrintWriter.println();
                this.mService.mServices.newServiceDumperLocked((FileDescriptor) null, fastPrintWriter, emptyArgs, 0, false, (String) null).dumpLocked();
                fastPrintWriter.println();
                this.mService.mAtmInternal.dump("activities", (FileDescriptor) null, fastPrintWriter, emptyArgs, 0, false, false, (String) null, -1);
                fastPrintWriter.flush();
            }
            dropBuilder.append(catSw);
        }
        return dropBuilder.toString();
    }

    private ArrayList<ProcessMemInfo> collectProcessMemInfos(final Predicate<ProcessRecord> predicate) {
        final ArrayList<ProcessMemInfo> memInfos;
        synchronized (this.mService) {
            int lruSize = this.mService.mProcessList.getLruSizeLOSP();
            memInfos = new ArrayList<>(lruSize);
            this.mService.mProcessList.forEachLruProcessesLOSP(false, new Consumer() { // from class: com.android.server.am.AppProfilerImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppProfilerImpl.lambda$collectProcessMemInfos$2(predicate, memInfos, (ProcessRecord) obj);
                }
            });
        }
        return memInfos;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$collectProcessMemInfos$2(Predicate predicate, ArrayList memInfos, ProcessRecord rec) {
        if (predicate.test(rec)) {
            ProcessStateRecord state = rec.mState;
            memInfos.add(new ProcessMemInfo(rec.processName, rec.getPid(), state.getSetAdj(), state.getSetProcState(), state.getAdjType(), state.makeAdjReason()));
        }
    }

    public void checkMemoryPsi(boolean watchdog) {
        if (!isSystemLowMemPsiCritical()) {
            return;
        }
        if (watchdog) {
            ScoutHelper.doSysRqInterface('m');
            return;
        }
        ScoutHelper.doSysRqInterface('w');
        ScoutHelper.doSysRqInterface('l');
        String meminfo = getMemInfo(false);
        Slog.i(TAG, "Critical memory pressure, dumping memory info \n" + meminfo);
    }
}
