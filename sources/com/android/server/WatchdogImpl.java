package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.SurfaceControlImpl;
import android.view.SurfaceControlStub;
import com.android.internal.os.BackgroundThread;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppProfilerStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.WatchdogEvent;

@MiuiStubHead(manifestName = "com.android.server.WatchdogStub$$")
/* loaded from: classes.dex */
public class WatchdogImpl extends WatchdogStub {
    private static final String APP_ID = "APP_ID";
    private static final long CHECK_LAYER_TIMEOUT = 150000;
    public static final boolean DEBUG = false;
    private static final String EMPTY_BINDER = "Here are no Binder-related exception messages available.";
    private static final String EMPTY_MESSAGE = "";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String EXTRA_APP_ID = "31000401706";
    private static final String EXTRA_EVENT_NAME = "native_hang";
    private static final String EXTRA_PACKAGE_NAME = "android";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final long GB = 1073741824;
    private static final String HEAP_MONITOR_TAG = "HeapUsage Monitor";
    private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
    private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
    private static final String INVAILD_FORMAT_BINDER = "regex match failed";
    private static final long KB = 1024;
    private static final int MAX_TRACES = 3;
    private static final long MB = 1048576;
    private static final String NATIVE_HANG_ENABLE = "persist.sys.stability.nativehang.enable";
    private static final String NATIVE_HANG_PROCESS_NAME = "sys.stability.nativehang.processname";
    private static final int NATIVE_HANG_THRESHOLD = 2;
    private static final String PACKAGE = "PACKAGE";
    private static final String PID = "native_hang_pid";
    private static final String PROC_NAME = "naitve_hang_packageName";
    private static final String PROP_PRESERVE_LAYER_LEAK_CRIME_SCENE = "persist.sys.debug.preserve_scout_memory_leak_scene";
    private static final String REBOOT_MIUITEST = "persist.reboot.miuitest";
    private static final String REGEX_PATTERN = "\\bfrom((?:\\s+)?\\d+)\\(((?:\\s+)?\\S+)\\):.+to((?:\\s+)?\\d+)\\(((?:\\s+)?\\S+)\\):.+elapsed:((?:\\s+)?\\d*\\.?\\d*).\\b";
    private static final String SYSTEM_NATIVE_HANG_COUNT = "sys.stability.nativehang.count";
    private static final String SYSTEM_SERVER = "system_server";
    private static final String SYSTEM_SERVER_START_COUNT = "sys.system_server.start_count";
    private static final String TAG = "Watchdog";
    private static final String WATCHDOG_DIR = "/data/miuilog/stability/scout/watchdog";
    private static final int WATCHDOG_THRESHOLD = 60;
    private Context mContext;
    private static final boolean OOM_CRASH_ON_WATCHDOG = SystemProperties.getBoolean("persist.sys.oom_crash_on_watchdog", false);
    private static final int HEAP_MONITOR_THRESHOLD = SystemProperties.getInt("persist.sys.oom_crash_on_watchdog_size", 500);
    private static boolean isHalfOom = false;
    private static final List<String> diableThreadList = Arrays.asList("PackageManager");
    public static final Set<String> nativeHangProcList = new HashSet(Arrays.asList("/system/bin/surfaceflinger", "/system/bin/keystore2/data/misc/keystore"));
    String mToProcess = null;
    int mPendingTime = 0;
    int mToPid = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WatchdogImpl> {

        /* compiled from: WatchdogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WatchdogImpl INSTANCE = new WatchdogImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WatchdogImpl m278provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WatchdogImpl m277provideNewInstance() {
            return new WatchdogImpl();
        }
    }

    WatchdogImpl() {
        if (ScoutUtils.isLibraryTest()) {
            scheduleLayerLeakCheck();
            SurfaceControlImpl.enableRenovationForLibraryTests();
        }
    }

    private static void scheduleLayerLeakCheck() {
        final Handler h = BackgroundThread.getHandler();
        h.postDelayed(new Runnable() { // from class: com.android.server.WatchdogImpl.1
            @Override // java.lang.Runnable
            public void run() {
                if (SurfaceControl.checkLayersLeaked()) {
                    String leakedLayers = SurfaceControl.getLeakedLayers();
                    Slog.w(WatchdogImpl.TAG, "Leaked layers: " + leakedLayers);
                    SurfaceControlStub.getInstance().reportOORException(false);
                }
                h.postDelayed(this, WatchdogImpl.CHECK_LAYER_TIMEOUT);
            }
        }, CHECK_LAYER_TIMEOUT);
    }

    void onHalfWatchdog(String subject, File trace, List<Watchdog.HandlerChecker> blockedCheckers, String binderTransInfo, String mUuid) {
        if (Debug.isDebuggerConnected()) {
            return;
        }
        if (blockedCheckers != null && blockedCheckers.size() == 1) {
            String name = blockedCheckers.get(0).getName();
            if (diableThreadList.contains(name)) {
                return;
            }
        }
        String psi = ResourcePressureUtil.currentPsiState();
        Slog.w("MIUIScout Watchdog", "Enter HALF_WATCHDOG \n" + psi);
        ScoutSystemMonitor.getInstance().setWorkMessage(2);
        reportEvent(384, subject, saveWatchdogTrace(true, subject, binderTransInfo, trace), blockedCheckers, binderTransInfo, mUuid);
    }

    void onWatchdog(String subject, File trace, List<Watchdog.HandlerChecker> blockedCheckers, String binderTransInfo, String mUuid) {
        if (Debug.isDebuggerConnected()) {
            return;
        }
        String psi = ResourcePressureUtil.currentPsiState();
        Slog.w("MIUIScout Watchdog", "Enter WATCHDOG \n" + psi);
        AppProfilerStub.getInstance().checkMemoryPsi(true);
        int eventType = trace != null ? 2 : 385;
        reportEvent(eventType, subject, saveWatchdogTrace(false, subject, binderTransInfo, trace), blockedCheckers, binderTransInfo, mUuid);
    }

    public void setWatchdogPropTimestamp(long anrtime) {
        try {
            SystemProperties.set("sys.service.watchdog.timestamp", Long.toString(anrtime));
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Failed to set watchdog.timestamp property", e);
        }
    }

    public void checkOOMState() {
        if (!OOM_CRASH_ON_WATCHDOG) {
            return;
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            long dalvikMax = runtime.totalMemory();
            long dalvikFree = runtime.freeMemory();
            long allocHeapSpace = dalvikMax - dalvikFree;
            StringBuilder append = new StringBuilder().append("HeapAllocSize : ").append(prettySize(allocHeapSpace)).append("; monitorSize : ");
            int i = HEAP_MONITOR_THRESHOLD;
            Slog.d(HEAP_MONITOR_TAG, append.append(i).append("MB").toString());
            if (allocHeapSpace >= i * 1048576) {
                runtime.gc();
                long dalvikMax2 = runtime.totalMemory();
                long dalvikFree2 = runtime.freeMemory();
                long allocHeapSpace2 = dalvikMax2 - dalvikFree2;
                if (allocHeapSpace2 < i * 1048576) {
                    Slog.d(HEAP_MONITOR_TAG, "After performing Gc, HeapAllocSize : " + prettySize(allocHeapSpace2));
                    isHalfOom = false;
                    return;
                } else if (!isHalfOom) {
                    Slog.d(HEAP_MONITOR_TAG, "Half Oom, Heap of System_Server has allocated " + prettySize(allocHeapSpace2));
                    isHalfOom = true;
                    return;
                } else {
                    String msg = "Heap of System_Server has allocated " + prettySize(allocHeapSpace2) + " , So trigger OutOfMemoryError crash";
                    throw new OutOfMemoryError(msg);
                }
            }
            isHalfOom = false;
        } catch (Exception e) {
            Slog.e(HEAP_MONITOR_TAG, "checkOOMState:" + e.toString());
        }
    }

    private static File saveWatchdogTrace(boolean halfWatchdog, String subject, String binderTransInfo, File file) {
        File tracesDir = getWatchdogDir();
        if (tracesDir == null) {
            Slog.w(TAG, "Failed to get watchdog dir");
            return file;
        }
        final String prefix = halfWatchdog ? "pre_watchdog_pid_" : "watchdog_pid_";
        final TreeSet<File> existingTraces = new TreeSet<>();
        tracesDir.listFiles(new FileFilter() { // from class: com.android.server.WatchdogImpl$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file2) {
                return WatchdogImpl.lambda$saveWatchdogTrace$0(prefix, existingTraces, file2);
            }
        });
        if (existingTraces.size() >= 3) {
            for (int i = 0; i < 2; i++) {
                existingTraces.pollLast();
            }
            Iterator<File> it = existingTraces.iterator();
            while (it.hasNext()) {
                File trace = it.next();
                trace.delete();
            }
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        String fileName = prefix + Process.myPid() + d.h + dateFormat.format(new Date());
        File watchdogFile = new File(tracesDir, fileName);
        FileOutputStream fout = null;
        FileInputStream fin = null;
        if (file != null) {
            try {
                try {
                    if (file.length() > 0) {
                        fin = new FileInputStream(file);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to save watchdog trace: " + e.getMessage());
                }
            } finally {
                IoUtils.closeQuietly(fin);
                IoUtils.closeQuietly(fout);
            }
        }
        fout = new FileOutputStream(watchdogFile);
        if (fin == null) {
            fout.write("Subject".getBytes(StandardCharsets.UTF_8));
            fout.write(10);
            fout.write(10);
            if (binderTransInfo != null) {
                fout.write(binderTransInfo.getBytes(StandardCharsets.UTF_8));
                fout.write(10);
                fout.write(10);
            }
        } else {
            FileUtils.copyInternalUserspace(fin, fout, null, null, null);
        }
        if (!halfWatchdog) {
            fout.write(10);
            fout.write("------ ps info ------".getBytes());
            fout.write(ScoutHelper.getPsInfo(0).getBytes());
        }
        FileUtils.setPermissions(watchdogFile, 420, -1, -1);
        return watchdogFile;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$saveWatchdogTrace$0(String prefix, TreeSet existingTraces, File pathname) {
        if (pathname.getName().startsWith(prefix)) {
            existingTraces.add(pathname);
            return false;
        }
        return false;
    }

    private static File getWatchdogDir() {
        File tracesDir = new File(WATCHDOG_DIR);
        if (!tracesDir.exists() && !tracesDir.mkdirs()) {
            return null;
        }
        FileUtils.setPermissions(tracesDir, 493, -1, -1);
        return tracesDir;
    }

    public static String prettySize(long byte_count) {
        long[] kUnitThresholds = {0, 10240, 10485760, 10737418240L};
        long[] kBytesPerUnit = {1, 1024, 1048576, 1073741824};
        String[] kUnitStrings = {"B", "KB", "MB", "GB"};
        String negative_str = "";
        if (byte_count < 0) {
            negative_str = "-";
            byte_count = -byte_count;
        }
        int i = kUnitThresholds.length;
        do {
            i--;
            if (i <= 0) {
                break;
            }
        } while (byte_count < kUnitThresholds[i]);
        return negative_str + (byte_count / kBytesPerUnit[i]) + kUnitStrings[i];
    }

    private static void reportEvent(int type, String subject, File trace, List<Watchdog.HandlerChecker> handlerCheckers, String mBinderInfo, String mUuid) {
        final WatchdogEvent event = new WatchdogEvent();
        event.setType(type);
        event.setPid(Process.myPid());
        event.setProcessName(SYSTEM_SERVER);
        event.setPackageName(SYSTEM_SERVER);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSystem(true);
        event.setSummary(subject);
        event.setDetails(subject);
        event.setBinderTransactionInfo(mBinderInfo);
        event.setUuid(mUuid);
        event.setZygotePid(SystemProperties.get("persist.sys.zygote.start_pid"));
        if (trace != null) {
            event.setLogName(trace.getAbsolutePath());
        }
        if (handlerCheckers != null) {
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < handlerCheckers.size(); i++) {
                StackTraceElement[] st = handlerCheckers.get(i).getThread().getStackTrace();
                for (StackTraceElement element : st) {
                    details.append("    at ").append(element).append("\n");
                }
                details.append("\n\n");
            }
            event.setDetails(details.toString());
        }
        if (2 == type) {
            event.setEnsureReport(true);
        }
        if (!ScoutUtils.isLibraryTest()) {
            MQSEventManagerDelegate.runtimeWithTimeout(new Runnable() { // from class: com.android.server.WatchdogImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MQSEventManagerDelegate.getInstance().reportWatchdogEvent(event);
                }
            });
        } else {
            MQSEventManagerDelegate.getInstance().reportWatchdogEvent(event);
        }
    }

    public boolean retryDumpMyStacktrace(String fileName, long timeStartMs, long totalTimeoutMs) {
        long duration = SystemClock.elapsedRealtime() - timeStartMs;
        int retry = 3;
        long timeoutMs = totalTimeoutMs;
        while (duration < 2000) {
            int retry2 = retry - 1;
            if (retry <= 0 || timeoutMs < 5000) {
                break;
            }
            Slog.i(TAG, "retryDumpMyStacktrace: timeoutMs=" + timeoutMs + ", prevDumpDuration=" + duration);
            SystemClock.sleep(1000L);
            long timeStartMs2 = SystemClock.elapsedRealtime();
            long timeoutMs2 = timeoutMs - 1000;
            if (Debug.dumpJavaBacktraceToFileTimeout(Process.myPid(), fileName, (int) (timeoutMs2 / 1000))) {
                Slog.i(TAG, "retryDumpMyStacktrace succeeded");
                return true;
            }
            long dumpDuration = SystemClock.elapsedRealtime() - timeStartMs2;
            timeoutMs = timeoutMs2 - dumpDuration;
            retry = retry2;
        }
        Slog.i(TAG, "retryDumpMyStacktrace failed: timeoutMs=" + timeoutMs + ", prevDumpDuration=" + duration);
        return false;
    }

    public void init(Context context, ActivityManagerService activity) {
        this.mContext = context;
    }

    private void reportNativeHang(int pid, String packageName) {
        if (this.mContext == null) {
            return;
        }
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra("APP_ID", EXTRA_APP_ID);
            intent.putExtra("EVENT_NAME", EXTRA_EVENT_NAME);
            intent.putExtra("PACKAGE", EXTRA_PACKAGE_NAME);
            intent.putExtra(PID, pid);
            intent.putExtra(PROC_NAME, packageName);
            intent.setFlags(3);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
            Slog.e("MIUIScout Watchdog", "Upload onetrack exception!", e);
        }
    }

    private boolean checkNativeHangEnable() {
        return SystemProperties.getBoolean(NATIVE_HANG_ENABLE, false);
    }

    public int getWatchdogCount() {
        return SystemProperties.getInt(SYSTEM_NATIVE_HANG_COUNT, 0);
    }

    private void setWatchdogCount(int count) {
        SystemProperties.set(SYSTEM_NATIVE_HANG_COUNT, String.valueOf(count));
    }

    private boolean checkIsMiuiMtbf() {
        return SystemProperties.getBoolean(REBOOT_MIUITEST, false);
    }

    private boolean checkLabTestStatus() {
        if (ScoutUtils.isLibraryTest() && !checkIsMiuiMtbf()) {
            return false;
        }
        return true;
    }

    private boolean checkNativeHang(String binderTransInfo) {
        if (!checkNativeHangEnable()) {
            Slog.w("MIUIScout Watchdog", "naitve hang feature disable.");
            return false;
        }
        if (binderTransInfo.equals("") || binderTransInfo.equals(EMPTY_BINDER) || binderTransInfo.contains(INVAILD_FORMAT_BINDER)) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile(REGEX_PATTERN);
            Matcher matcher = pattern.matcher(binderTransInfo);
            if (!matcher.find()) {
                Slog.w("MIUIScout Watchdog", "nativeHang (" + binderTransInfo + ") regex match failed");
                return false;
            }
            int fromPid = Integer.parseInt(matcher.group(1).trim());
            String fromProcess = matcher.group(2).trim();
            this.mToPid = Integer.parseInt(matcher.group(3).trim());
            this.mToProcess = matcher.group(4).trim();
            double timeout = Double.valueOf(matcher.group(5).trim()).doubleValue();
            this.mPendingTime = (int) Math.round(timeout);
            Slog.w("MIUIScout Watchdog", "fromPid=" + fromPid + ", fromProcess=" + fromProcess + ", mToPid=" + this.mToPid + ", mToProcess=" + this.mToProcess + ", mPendingTime=" + this.mPendingTime);
            if (Process.myPid() != fromPid || !nativeHangProcList.contains(this.mToProcess) || this.mPendingTime < 60) {
                return false;
            }
            Slog.e("MIUIScout Watchdog", "Occur native hang");
            return true;
        } catch (Exception e) {
            Slog.w("MIUIScout Watchdog", "checkNativeHang process Error: ", e);
            return false;
        }
    }

    public boolean checkAndSaveStatus(boolean waitHalf, String binderTransInfo) {
        if (!checkLabTestStatus()) {
            Slog.w("MIUIScout Watchdog", "only checkNativeHang when MIUI MTBF& non-laboratory test scenarios");
            return false;
        }
        if (waitHalf) {
            Slog.w("MIUIScout Watchdog", "only checkNativeHang when watchdog");
            return false;
        }
        if (!checkNativeHang(binderTransInfo)) {
            Slog.w("MIUIScout Watchdog", "not native hang process");
            return false;
        }
        int oldCount = getWatchdogCount();
        if (oldCount == 0) {
            setWatchdogCount(oldCount + 1);
            SystemProperties.set(NATIVE_HANG_PROCESS_NAME, this.mToProcess);
            return true;
        }
        if (oldCount == 1 && SystemProperties.get(NATIVE_HANG_PROCESS_NAME).equals(this.mToProcess)) {
            setWatchdogCount(oldCount + 1);
            reportNativeHang(this.mToPid, this.mToProcess);
            Slog.e("MIUIScout Watchdog", "Occur native hang, processName=" + this.mToProcess + ", mPendTime=" + this.mPendingTime + "s.");
            Slog.e("MIUIScout Watchdog", "Native process hang reboot time larger than 2 time, reboot device!");
            return true;
        }
        setWatchdogCount(0);
        SystemProperties.set(NATIVE_HANG_PROCESS_NAME, "null");
        return false;
    }

    public boolean isNativeHang() {
        return getWatchdogCount() >= 2;
    }

    public boolean needRebootSystem() {
        if (!isNativeHang()) {
            return false;
        }
        return true;
    }
}
