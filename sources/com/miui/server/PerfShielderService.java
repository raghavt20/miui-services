package com.miui.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.statistics.E2EScenario;
import android.os.statistics.E2EScenarioPayload;
import android.os.statistics.E2EScenarioSettings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.app.IMiuiSysUser;
import com.android.internal.app.IPerfShielder;
import com.android.internal.app.LaunchTimeRecord;
import com.android.internal.app.QuickAppResolveInfo;
import com.android.server.LocalServices;
import com.android.server.MiuiBgThread;
import com.android.server.ScoutHelper;
import com.android.server.SystemService;
import com.android.server.am.IProcessPolicy;
import com.android.server.am.MiuiMemoryInfoStub;
import com.android.server.am.MiuiSysUserServiceHelper;
import com.android.server.am.ProcessUtils;
import com.miui.app.SpeedTestModeServiceInternal;
import com.miui.daemon.performance.server.IMiuiPerfService;
import com.miui.hybrid.hook.CallingPkgHook;
import com.miui.hybrid.hook.FilterInfoInjector;
import com.miui.hybrid.hook.HapLinksInjector;
import com.miui.hybrid.hook.IntentHook;
import com.miui.hybrid.hook.PermissionChecker;
import com.miui.hybrid.hook.PkgInfoHook;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.security.AccessControlImpl;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import miui.process.ProcessManagerNative;

/* loaded from: classes.dex */
public final class PerfShielderService extends IPerfShielder.Stub {
    private static final long ACTIVITY_BATCH_MAX_INTERVAL = 60000;
    private static final int ACTIVITY_BATCH_MAX_SIZE = 10;
    private static final long BIND_FAIL_RETRY_TIME = 60000;
    private static final long BIND_RETRY_TIME_BASE = 60000;
    private static final long BIND_RETRY_TIME_MAX = 3600000;
    private static final long BIND_SYSOPT_SERVICE_FIRST = 1500;
    private static final boolean DEBUG = true;
    private static final long DELAY_TIME = 300000;
    private static final int LAUNCH_TYPE_DEFAULT = 0;
    private static final int LAUNCH_TYPE_FROM_HOME = 1;
    private static final String MIUI_SYS_USER_CLASS = "com.miui.daemon.performance.SysoptService";
    private static final String MIUI_SYS_USER_PACKAHE = "com.miui.daemon";
    static final int MSG_BIND_MIUI_SYS_USER = 2;
    static final int MSG_REBIND = 1;
    private static final int NATIVE_ADJ;
    private static final String PERFORMANCE_CLASS = "com.miui.daemon.performance.MiuiPerfService";
    private static final String PERFORMANCE_PACKAGE = "com.miui.daemon";
    private static final int SELF_CAUSE_ANR = 7;
    private static final String[] SELF_CAUSE_NAMES;
    public static final String SERVICE_NAME = "perfshielder";
    private static final String SYSTEM_SERVER = "system_server";
    public static final String TAG = "PerfShielderService";
    private static ArrayList<String> WINDOW_NAME_WHITE_LIST;
    private Context mContext;
    protected IMiuiPerfService mPerfService;
    private WMServiceConnection mWMServiceConnection;
    private static long mLastRetryTime = AccessControlImpl.LOCK_TIME_OUT;
    private static Pattern WINDOW_NAME_REX = Pattern.compile("(\\w+\\.)+(\\w+)\\/\\.?(\\w+\\.)*(\\w+)");
    private final Object mPerfEventSocketFdLock = new Object();
    private final AtomicReference<ParcelFileDescriptor> mPerfEventSocketFd = new AtomicReference<>();
    private List<LaunchTimeRecord> mLaunchTimes = new ArrayList();
    private MiuiSysUserServiceConnection mMiuiSysUserConnection = new MiuiSysUserServiceConnection();
    private final ServiceConnection mPerformanceConnection = new ServiceConnection() { // from class: com.miui.server.PerfShielderService.2
        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            Slog.v(PerfShielderService.TAG, "Miui performance service disconnected!");
            PerfShielderService.this.mPerfService = null;
            if (PerfShielderService.this.mContext != null) {
                PerfShielderService.this.mContext.unbindService(PerfShielderService.this.mPerformanceConnection);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            PerfShielderService.this.mPerfService = IMiuiPerfService.Stub.asInterface(arg1);
            PerfShielderService.this.mHandler.removeMessages(1);
            try {
                Slog.v(PerfShielderService.TAG, "Miui performance service connected!");
                PerfShielderService.this.mPerfService.asBinder().linkToDeath(PerfShielderService.this.mDeathHandler, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    IBinder.DeathRecipient mMiuiSysUserDeathHandler = new IBinder.DeathRecipient() { // from class: com.miui.server.PerfShielderService.3
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiSysUserServiceHelper.setMiuiSysUser(null);
            Slog.v(PerfShielderService.TAG, "MiuiSysUser service binderDied!");
            PerfShielderService.this.mHandler.removeMessages(2);
            PerfShielderService.this.sendBindMiuiSysUserMsg(PerfShielderService.mLastRetryTime);
        }
    };
    IBinder.DeathRecipient mDeathHandler = new IBinder.DeathRecipient() { // from class: com.miui.server.PerfShielderService.4
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.v(PerfShielderService.TAG, "Miui performance service binderDied!");
            PerfShielderService.this.sendRebindServiceMsg(PerfShielderService.DELAY_TIME);
        }
    };
    private BindServiceHandler mHandler = new BindServiceHandler(MiuiBgThread.get().getLooper());
    private Method mReflectGetPssMethod = reflectDebugGetPssMethod();

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        WINDOW_NAME_WHITE_LIST = arrayList;
        arrayList.add("Keyguard");
        WINDOW_NAME_WHITE_LIST.add("StatusBar");
        WINDOW_NAME_WHITE_LIST.add("RecentsPanel");
        WINDOW_NAME_WHITE_LIST.add("InputMethod");
        WINDOW_NAME_WHITE_LIST.add("Volume Control");
        WINDOW_NAME_WHITE_LIST.add("GestureStubBottom");
        WINDOW_NAME_WHITE_LIST.add("GestureStub");
        WINDOW_NAME_WHITE_LIST.add("GestureAnywhereView");
        WINDOW_NAME_WHITE_LIST.add("NavigationBar");
        NATIVE_ADJ = ScoutHelper.OOM_SCORE_ADJ_MIN;
        SELF_CAUSE_NAMES = new String[]{"Slow main thread", "Slow handle input", "Slow handle animation", "Slow handle traversal", "Slow bitmap uploads", "Slow issue draw commands", "Slow swap buffers", "ANR"};
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final PerfShielderService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new PerfShielderService(context);
        }

        public void onStart() {
            publishBinderService(PerfShielderService.SERVICE_NAME, this.mService);
        }

        public void onBootPhase(int phase) {
            SpeedTestModeServiceInternal speedTestModeService;
            if (phase == 1000 && (speedTestModeService = (SpeedTestModeServiceInternal) LocalServices.getService(SpeedTestModeServiceInternal.class)) != null) {
                speedTestModeService.onBootPhase();
            }
        }
    }

    public PerfShielderService(Context context) {
        this.mContext = context;
        this.mWMServiceConnection = new WMServiceConnection(context);
        SpeedTestModeServiceInternal speedTestModeService = (SpeedTestModeServiceInternal) LocalServices.getService(SpeedTestModeServiceInternal.class);
        if (speedTestModeService != null) {
            speedTestModeService.init(context);
        }
    }

    public void systemReady() {
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.PerfShielderService.1
            @Override // java.lang.Runnable
            public void run() {
                PerfShielderService.this.bindService();
            }
        }, 10000L);
        sendBindMiuiSysUserMsg(BIND_SYSOPT_SERVICE_FIRST);
    }

    private boolean needToLimit(int pid, String processName) {
        boolean limit = false;
        String fileName = "/proc/" + pid + "/cmdline";
        BufferedReader reader = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                    String line = reader.readLine();
                    if (line != null) {
                        if (line.contains(processName)) {
                            limit = true;
                        }
                    }
                    reader.close();
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        return limit;
    }

    public void setForkedProcessGroup(int puid, int ppid, int group, String processName) {
        StrictMode.getThreadPolicyMask();
        String fileName = ProcessManagerNative.getCgroupFilePath(puid, ppid);
        BufferedReader reader = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        int subPid = Integer.parseInt(line);
                        if (subPid != ppid && (processName == null || needToLimit(subPid, processName))) {
                            Process.setProcessGroup(subPid, group);
                            Slog.i(TAG, "sFPG ppid:" + ppid + " grp:" + group + " forked:" + processName + " pid:" + subPid);
                        }
                    }
                    reader.close();
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
    }

    public int getMemoryTrimLevel() {
        return ProcessUtils.getMemoryTrimLevel();
    }

    public List<Bundle> updateProcessFullMemInfoByPids(int[] pids) {
        int pidSize = pids.length;
        List<Bundle> result = new ArrayList<>(pidSize);
        PidSwapGetter swapgetter = new PidSwapGetter();
        for (int i = 0; i < pidSize; i++) {
            Bundle bundle = new Bundle();
            bundle.putInt("pid", pids[i]);
            bundle.putLong("lastPssTime", SystemClock.uptimeMillis());
            bundle.putLong("lastPss", getProcessPss(pids[i]));
            bundle.putLong("lastRssTime", SystemClock.uptimeMillis());
            long[] pidStatus = getProcessStatusValues(pids[i]);
            bundle.putLong("swap", pidStatus[0]);
            int ppid = (int) pidStatus[1];
            bundle.putInt("ppid", ppid);
            bundle.putLong("pswap", swapgetter.get(ppid));
            bundle.putLong("rss", pidStatus[2]);
            result.add(bundle);
        }
        return result;
    }

    public List<Bundle> updateProcessPartialMemInfoByPids(int[] pids) {
        int pidSize = pids.length;
        List<Bundle> result = new ArrayList<>(pidSize);
        PidSwapGetter swapgetter = new PidSwapGetter();
        for (int i = 0; i < pidSize; i++) {
            Bundle bundle = new Bundle();
            bundle.putInt("pid", pids[i]);
            bundle.putLong("lastRssTime", SystemClock.uptimeMillis());
            long[] pidStatus = getProcessStatusValues(pids[i]);
            bundle.putLong("swap", pidStatus[0]);
            int ppid = (int) pidStatus[1];
            bundle.putInt("ppid", ppid);
            bundle.putLong("pswap", swapgetter.get(ppid));
            bundle.putLong("rss", pidStatus[2]);
            result.add(bundle);
        }
        return result;
    }

    /* loaded from: classes.dex */
    private class PidSwapGetter {
        Map<Integer, Long> pidSwapMap;

        private PidSwapGetter() {
            this.pidSwapMap = new HashMap();
        }

        public long get(int pid) {
            if (pid <= 0) {
                return 0L;
            }
            if (!this.pidSwapMap.containsKey(Integer.valueOf(pid))) {
                this.pidSwapMap.put(Integer.valueOf(pid), Long.valueOf(PerfShielderService.this.getProcessStatusValues(pid)[0]));
            }
            return this.pidSwapMap.get(Integer.valueOf(pid)).longValue();
        }
    }

    private Method reflectDebugGetPssMethod() {
        try {
            Method getPss = Debug.class.getDeclaredMethod("getPss", Integer.TYPE, long[].class, long[].class);
            return getPss;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private long getProcessPss(int pid) {
        if (this.mReflectGetPssMethod == null) {
            return 0L;
        }
        try {
            long pss = ((Long) this.mReflectGetPssMethod.invoke(null, Integer.valueOf(pid), null, null)).longValue();
            return pss;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } catch (Throwable th) {
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long[] getProcessStatusValues(int pid) {
        String[] procStatusLabels = {"VmSwap:", "PPid:", "VmRSS:"};
        long[] procStatusValues = {-1, -1, -1};
        Process.readProcLines("/proc/" + pid + "/status", procStatusLabels, procStatusValues);
        return procStatusValues;
    }

    public void reportPerceptibleJank(int callingPid, int renderThreadTid, String windowName, long totalDuration, long maxFrameDuration, long endTs, int appCause, long numFrames) {
        reportPerceptibleJank(callingPid, renderThreadTid, windowName, totalDuration, maxFrameDuration, endTs, appCause, numFrames, "");
    }

    public void reportAnr(int callingPid, String windowName, long totalDuration, long endTs, String cpuInfo) {
        reportPerceptibleJank(callingPid, -1, windowName, totalDuration, totalDuration, endTs, 7, 0L, cpuInfo);
    }

    public void reportPerceptibleJank(int callingPid, int renderThreadTid, String windowName, long totalDuration, long maxFrameDuration, long endTs, int appCause, long numFrames, String cpuInfo) {
        String strAppCause;
        String windowName2 = windowName;
        String callingPkg = ProcessUtils.getPackageNameByPid(callingPid);
        if (callingPkg == null) {
            return;
        }
        if (windowName2 != null && !WINDOW_NAME_WHITE_LIST.contains(windowName2) && !WINDOW_NAME_REX.matcher(windowName2).matches()) {
            String windowIdentityCode = (windowName.length() >= 3 ? windowName2.substring(0, 3) : windowName2) + windowName.hashCode();
            windowName2 = callingPkg + "-" + windowIdentityCode;
        }
        String packageVersion = new PackageVersionNameGetter().get(callingPkg);
        if (appCause >= 0) {
            String[] strArr = SELF_CAUSE_NAMES;
            if (appCause < strArr.length) {
                strAppCause = strArr[appCause];
                Slog.d(TAG, callingPkg + "|" + windowName2 + "|" + (totalDuration / 1000000) + "|" + endTs + "|" + (maxFrameDuration / 1000000) + "|" + appCause + "|" + numFrames);
                Bundle bundle = new Bundle();
                bundle.putInt("pid", callingPid);
                bundle.putInt("tid", renderThreadTid);
                bundle.putString("pkg", callingPkg);
                bundle.putString("pkgVersion", packageVersion);
                bundle.putString(DumpSysInfoUtil.WINDOW, windowName2);
                bundle.putLong("totalDuration", totalDuration);
                bundle.putLong("maxFrameDuration", maxFrameDuration);
                bundle.putLong("endTs", endTs);
                bundle.putString("appCause", strAppCause);
                bundle.putString("cpuInfo", cpuInfo);
                bundle.putLong("numFrames", numFrames);
                markPerceptibleJank(bundle);
            }
        }
        strAppCause = IProcessPolicy.REASON_UNKNOWN;
        Slog.d(TAG, callingPkg + "|" + windowName2 + "|" + (totalDuration / 1000000) + "|" + endTs + "|" + (maxFrameDuration / 1000000) + "|" + appCause + "|" + numFrames);
        Bundle bundle2 = new Bundle();
        bundle2.putInt("pid", callingPid);
        bundle2.putInt("tid", renderThreadTid);
        bundle2.putString("pkg", callingPkg);
        bundle2.putString("pkgVersion", packageVersion);
        bundle2.putString(DumpSysInfoUtil.WINDOW, windowName2);
        bundle2.putLong("totalDuration", totalDuration);
        bundle2.putLong("maxFrameDuration", maxFrameDuration);
        bundle2.putLong("endTs", endTs);
        bundle2.putString("appCause", strAppCause);
        bundle2.putString("cpuInfo", cpuInfo);
        bundle2.putLong("numFrames", numFrames);
        markPerceptibleJank(bundle2);
    }

    public void markPerceptibleJank(Bundle bundle) {
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.markPerceptibleJank(bundle);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addActivityLaunchTime(String str, String str2, long j, long j2, boolean z, boolean z2) {
        if (str == null) {
            return;
        }
        LaunchTimeRecord launchTimeRecord = new LaunchTimeRecord(str, str2, j, j2, z2);
        launchTimeRecord.setType(z ? 1 : 0);
        this.mLaunchTimes.add(launchTimeRecord);
        long launchStartTime = this.mLaunchTimes.get(0).getLaunchStartTime();
        long launchEndTime = this.mLaunchTimes.get(r5.size() - 1).getLaunchEndTime();
        if (z || this.mLaunchTimes.size() >= 10 || launchEndTime < launchStartTime || launchEndTime - launchStartTime >= AccessControlImpl.LOCK_TIME_OUT) {
            reportActivityLaunchRecords();
            this.mLaunchTimes.clear();
        }
    }

    private void reportActivityLaunchRecords() {
        try {
            if (this.mPerfService != null && this.mLaunchTimes.size() > 0) {
                PackageVersionNameGetter versionGetter = new PackageVersionNameGetter();
                List<Bundle> bundles = new ArrayList<>();
                for (int i = 0; i < this.mLaunchTimes.size(); i++) {
                    LaunchTimeRecord record = this.mLaunchTimes.get(i);
                    Bundle bundle = new Bundle();
                    bundle.putString("PackageName", record.getPackageName());
                    bundle.putString("PackageVersion", versionGetter.get(record.getPackageName()));
                    bundle.putString("Activity", record.getActivity());
                    bundle.putLong("LaunchStartTime", record.getLaunchStartTime());
                    bundle.putLong("LaunchEndTime", record.getLaunchEndTime());
                    bundle.putInt("Type", record.getType());
                    bundle.putBoolean("IsColdStart", record.isColdStart());
                    bundles.add(bundle);
                }
                this.mPerfService.reportActivityLaunchRecords(bundles);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportExcessiveCpuUsageRecords(List<Bundle> records) {
        try {
            if (this.mPerfService != null && records.size() > 0) {
                this.mPerfService.reportExcessiveCpuUsageRecords(records);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportProcessCleanEvent(Bundle bundle) {
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportProcessCleanEvent(bundle);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageVersionNameGetter {
        Map<String, String> packageVersionMap;

        private PackageVersionNameGetter() {
            this.packageVersionMap = new HashMap();
        }

        public String get(String packageName) {
            if (TextUtils.isEmpty(packageName)) {
                return "";
            }
            if (!this.packageVersionMap.containsKey(packageName)) {
                String packageVersion = "";
                try {
                    packageVersion = PerfShielderService.this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
                } catch (Exception e) {
                }
                this.packageVersionMap.put(packageName, packageVersion);
            }
            return this.packageVersionMap.get(packageName);
        }
    }

    public void setSchedFgPid(int pid) {
        if (pid <= 0) {
            return;
        }
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.setSchedFgPid(pid);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void dumpFromFile(PrintWriter pw, String path) {
        File file = new File(path);
        BufferedReader reader = null;
        if (!file.exists()) {
            return;
        }
        try {
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = reader.readLine();
                        if (line != null) {
                            pw.println(line);
                        } else {
                            reader.close();
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(pw);
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            String errMsg = "Permission Denial: can't dump perfshielder from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to missing android.permission.DUMP permission";
            Slog.v(TAG, errMsg);
            pw.println(errMsg);
            return;
        }
        pw.println("---- ION Memory Usage ----");
        dumpFromFile(pw, "/d/ion/heaps/system");
        dumpFromFile(pw, "/d/ion/ion_mm_heap");
        pw.println("---- End of ION Memory Usage ----\n");
        pw.println("---- minfree & adj ----");
        pw.print("minfree: ");
        dumpFromFile(pw, "/sys/module/lowmemorykiller/parameters/minfree");
        pw.print("    adj: ");
        dumpFromFile(pw, "/sys/module/lowmemorykiller/parameters/adj");
        pw.println("---- End of minfree & adj ----\n");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendRebindServiceMsg(long delayedTime) {
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1);
        this.mHandler.sendMessageDelayed(msg, delayedTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindService() {
        if (this.mContext != null && this.mPerfService == null) {
            Intent intent = new Intent();
            intent.setClassName("com.miui.daemon", PERFORMANCE_CLASS);
            if (!this.mContext.bindServiceAsUser(intent, this.mPerformanceConnection, 1, UserHandle.OWNER)) {
                Slog.v(TAG, "Miui performance: can't bind to com.miui.daemon.performance.MiuiPerfService");
                sendRebindServiceMsg(AccessControlImpl.LOCK_TIME_OUT);
            } else {
                Slog.v(TAG, "Miui performance service started");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MiuiSysUserServiceConnection implements ServiceConnection {
        private boolean isServiceDisconnected;

        private MiuiSysUserServiceConnection() {
            this.isServiceDisconnected = false;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            MiuiSysUserServiceHelper.setMiuiSysUser(null);
            Slog.v(PerfShielderService.TAG, "MiuiSysUser service disconnected!");
            this.isServiceDisconnected = false;
            if (PerfShielderService.this.mContext != null) {
                PerfShielderService.this.mContext.unbindService(PerfShielderService.this.mMiuiSysUserConnection);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName comp, IBinder iObj) {
            this.isServiceDisconnected = true;
            IMiuiSysUser sysOpt = IMiuiSysUser.Stub.asInterface(iObj);
            MiuiSysUserServiceHelper.setMiuiSysUser(sysOpt);
            PerfShielderService.this.mHandler.removeMessages(2);
            try {
                Slog.v(PerfShielderService.TAG, "MiuiSysUser service connected!");
                sysOpt.asBinder().linkToDeath(PerfShielderService.this.mMiuiSysUserDeathHandler, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBindMiuiSysUserMsg(long delayedTime) {
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessageDelayed(msg, delayedTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindMiuiSysUser() {
        if (this.mContext != null && !this.mMiuiSysUserConnection.isServiceDisconnected) {
            Intent intent = new Intent();
            intent.setClassName("com.miui.daemon", MIUI_SYS_USER_CLASS);
            if (!this.mContext.bindServiceAsUser(intent, this.mMiuiSysUserConnection, 1, UserHandle.OWNER)) {
                sendBindMiuiSysUserMsg(mLastRetryTime);
                long j = mLastRetryTime;
                mLastRetryTime = j < 3600000 ? j << 1 : 3600000L;
                Slog.v(TAG, "MiuiSysUser: can't bind to com.miui.daemon.performance.SysoptService, retry time == " + mLastRetryTime);
                return;
            }
            Slog.v(TAG, "MiuiSysUser service started");
        }
    }

    public boolean insertPackageInfo(PackageInfo pInfo) throws RemoteException {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("PkgInfoHook", "Check permission failed when insert PackageInfo.");
            return false;
        }
        return PkgInfoHook.getInstance().insert(pInfo);
    }

    public boolean deletePackageInfo(String pkgName) throws RemoteException {
        if (PermissionChecker.check(this.mContext)) {
            return PkgInfoHook.getInstance().delete(pkgName) != null;
        }
        Slog.e("PkgInfoHook", "Check permission failed when delete PackageInfo.");
        return false;
    }

    public boolean insertRedirectRule(String callingPkg, String destPkg, String redirectPkgname, Bundle clsNameMap) throws RemoteException {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("IntentHook", "Check permission failed when insert RedirectRule.");
            return false;
        }
        return IntentHook.getInstance().insert(callingPkg, destPkg, redirectPkgname, clsNameMap);
    }

    public boolean deleteRedirectRule(String callingPkg, String destPkg) throws RemoteException {
        if (PermissionChecker.check(this.mContext)) {
            return IntentHook.getInstance().delete(callingPkg, destPkg) != null;
        }
        Slog.e("IntentHook", "Check permission failed when delete RedirectRule.");
        return false;
    }

    public long getFreeMemory() throws RemoteException {
        return MiuiMemoryInfoStub.getInstance().getFreeMemory();
    }

    public ParcelFileDescriptor getPerfEventSocketFd() throws RemoteException {
        ParcelFileDescriptor fd = this.mPerfEventSocketFd.get();
        if (fd == null || fd.getFileDescriptor() == null || !fd.getFileDescriptor().valid() || Binder.getCallingPid() == Process.myPid()) {
            this.mPerfEventSocketFd.compareAndSet(fd, null);
        }
        obtainPerfEventSocketFd();
        ParcelFileDescriptor fd2 = this.mPerfEventSocketFd.get();
        if (fd2 == null || fd2.getFileDescriptor() == null || !fd2.getFileDescriptor().valid()) {
            return null;
        }
        try {
            ParcelFileDescriptor result = fd2.dup();
            return result;
        } catch (IOException e) {
            try {
                fd2.close();
            } catch (IOException e2) {
            }
            this.mPerfEventSocketFd.compareAndSet(fd2, null);
            obtainPerfEventSocketFd();
            ParcelFileDescriptor fd3 = this.mPerfEventSocketFd.get();
            if (fd3 == null || fd3.getFileDescriptor() == null || !fd3.getFileDescriptor().valid()) {
                return null;
            }
            try {
                ParcelFileDescriptor result2 = fd3.dup();
                return result2;
            } catch (IOException e3) {
                return null;
            }
        }
    }

    private void obtainPerfEventSocketFd() {
        IMiuiPerfService perfService = this.mPerfService;
        if (this.mPerfEventSocketFd.get() == null && perfService != null) {
            synchronized (this.mPerfEventSocketFdLock) {
                if (this.mPerfEventSocketFd.get() == null) {
                    try {
                        ParcelFileDescriptor fd = perfService.getPerfEventSocketFd();
                        this.mPerfEventSocketFd.set(fd);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BindServiceHandler extends Handler {
        public BindServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PerfShielderService.this.bindService();
                    return;
                case 2:
                    PerfShielderService.this.bindMiuiSysUser();
                    return;
                default:
                    return;
            }
        }
    }

    public boolean addCallingPkgHookRule(String hostApp, String originCallingPkg, String hookCallingPkg) throws RemoteException {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("CallingPkgHook", "Check permission failed when addCallingPkgHookRule.");
            return false;
        }
        return CallingPkgHook.getInstance().add(hostApp, originCallingPkg, hookCallingPkg);
    }

    public boolean removeCallingPkgHookRule(String hostApp, String originCallingPkg) throws RemoteException {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("CallingPkgHook", "Check permission failed when removeCallingPkgHookRule.");
            return false;
        }
        return CallingPkgHook.getInstance().remove(hostApp, originCallingPkg);
    }

    public Bundle beginScenario(E2EScenario scenario, E2EScenarioSettings settings, String tag, E2EScenarioPayload payload, int tid, long uptimeMillis, boolean needResultBundle) throws RemoteException {
        long uptimeMillis2;
        String processName;
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return null;
        }
        if (uptimeMillis != 0) {
            uptimeMillis2 = uptimeMillis;
        } else {
            uptimeMillis2 = SystemClock.uptimeMillis();
        }
        int pid = Binder.getCallingPid();
        if (pid == Process.myPid()) {
            processName = SYSTEM_SERVER;
        } else {
            processName = ProcessUtils.getProcessNameByPid(pid);
        }
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        return this.mPerfService.beginScenario(scenario, settings, tag, payload, uptimeMillis2, pid, tid, processName, packageName, needResultBundle);
    }

    public void abortMatchingScenario(E2EScenario scenario, String tag, int tid, long uptimeMillis) throws RemoteException {
        long uptimeMillis2;
        String processName;
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return;
        }
        if (uptimeMillis != 0) {
            uptimeMillis2 = uptimeMillis;
        } else {
            uptimeMillis2 = SystemClock.uptimeMillis();
        }
        int pid = Binder.getCallingPid();
        if (pid == Process.myPid()) {
            processName = SYSTEM_SERVER;
        } else {
            processName = ProcessUtils.getProcessNameByPid(pid);
        }
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        this.mPerfService.abortMatchingScenario(scenario, tag, uptimeMillis2, pid, tid, processName, packageName);
    }

    public void abortSpecificScenario(Bundle scenarioBundle, int tid, long uptimeMillis) throws RemoteException {
        String processName;
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return;
        }
        if (uptimeMillis == 0) {
            uptimeMillis = SystemClock.uptimeMillis();
        }
        int pid = Binder.getCallingPid();
        if (pid == Process.myPid()) {
            processName = SYSTEM_SERVER;
        } else {
            processName = ProcessUtils.getProcessNameByPid(pid);
        }
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        this.mPerfService.abortSpecificScenario(scenarioBundle, uptimeMillis, pid, tid, processName, packageName);
    }

    public void finishMatchingScenario(E2EScenario scenario, String tag, E2EScenarioPayload payload, int tid, long uptimeMillis) throws RemoteException {
        long uptimeMillis2;
        String processName;
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return;
        }
        if (uptimeMillis != 0) {
            uptimeMillis2 = uptimeMillis;
        } else {
            uptimeMillis2 = SystemClock.uptimeMillis();
        }
        int pid = Binder.getCallingPid();
        if (pid == Process.myPid()) {
            processName = SYSTEM_SERVER;
        } else {
            processName = ProcessUtils.getProcessNameByPid(pid);
        }
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        this.mPerfService.finishMatchingScenario(scenario, tag, payload, uptimeMillis2, pid, tid, processName, packageName);
    }

    public void finishSpecificScenario(Bundle scenarioBundle, E2EScenarioPayload payload, int tid, long uptimeMillis) throws RemoteException {
        long uptimeMillis2;
        String processName;
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return;
        }
        if (uptimeMillis != 0) {
            uptimeMillis2 = uptimeMillis;
        } else {
            uptimeMillis2 = SystemClock.uptimeMillis();
        }
        int pid = Binder.getCallingPid();
        if (pid == Process.myPid()) {
            processName = SYSTEM_SERVER;
        } else {
            processName = ProcessUtils.getProcessNameByPid(pid);
        }
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        this.mPerfService.finishSpecificScenario(scenarioBundle, payload, uptimeMillis2, pid, tid, processName, packageName);
    }

    public void reportNotificationClick(String postPackage, Intent intent, long uptimeMillis) throws RemoteException {
        IMiuiPerfService perfService = this.mPerfService;
        if (perfService == null) {
            return;
        }
        if (uptimeMillis == 0) {
            uptimeMillis = SystemClock.uptimeMillis();
        }
        this.mPerfService.reportNotificationClick(postPackage, intent, uptimeMillis);
    }

    public boolean insertFilterInfo(String packageName, String defaultLabel, Uri iconUri, List<Bundle> filterInfos) {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("CallingPkgHook", "Check permission failed when insertFilterInfo.");
            return false;
        }
        return FilterInfoInjector.getInstance().insertFilterInfo(packageName, defaultLabel, iconUri, filterInfos);
    }

    public boolean deleteFilterInfo(String packageName) {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("CallingPkgHook", "Check permission failed when deleteFilterInfo.");
            return false;
        }
        return FilterInfoInjector.getInstance().deleteFilterInfo(packageName);
    }

    public List<QuickAppResolveInfo> resolveQuickAppInfos(Intent targetIntent) {
        return FilterInfoInjector.getInstance().resolveAppInfos(this.mContext, targetIntent);
    }

    public void setHapLinks(Map data, ActivityInfo activityInfo) throws RemoteException {
        if (!PermissionChecker.check(this.mContext)) {
            Slog.e("CallingPkgHook", "Check permission failed when setHapLinks.");
        } else {
            HapLinksInjector.setData(data, activityInfo);
        }
    }

    public void reportPssRecord(String processName, String packageName, long pss, String versionName, int versionCode) {
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportPssRecord(processName, packageName, pss, versionName, versionCode);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportKillMessage(String processName, int pid, int uid, long pss) {
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportKillMessage(processName, pid, uid, pss);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportMiSpeedRecord(Bundle data) {
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportMiSpeedRecord(data);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportApplicationStart(String packageName, int startFlag, int residentPkg, String[] residentPkgNameList) {
        Bundle bundle = new Bundle();
        bundle.putString("packageName", packageName);
        bundle.putInt("startFlag", startFlag);
        bundle.putInt("residentPkg", residentPkg);
        bundle.putStringArray("residentPkgNameList", residentPkgNameList);
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportApplicationStart(bundle);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reportKillProcessEvent(String processName, String packageName, String reason, int adj, long pss, long rss, long memAvail, long residentDuration, boolean hasActivities, boolean hasForegroundServices) {
        Bundle bundle = new Bundle();
        bundle.putString("packageName", packageName);
        bundle.putString(EdgeSuppressionManager.EdgeSuppressionHandler.MSG_DATA_REASON, reason);
        bundle.putInt("adj", adj);
        bundle.putLong("pss", pss);
        bundle.putLong("rss", rss);
        bundle.putLong("memAvail", memAvail);
        bundle.putLong("residentDuration", residentDuration);
        bundle.putBoolean("hasActivities", hasActivities);
        bundle.putBoolean("hasForegroundServices", hasForegroundServices);
        try {
            IMiuiPerfService iMiuiPerfService = this.mPerfService;
            if (iMiuiPerfService != null) {
                iMiuiPerfService.reportKillProcessEvent(bundle);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
