package com.miui.server.stability;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.Os;
import android.util.Slog;
import android.view.SurfaceControlImpl;
import android.view.SurfaceControlStub;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.util.ArrayUtils;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.ScoutHelper;
import com.android.server.UiThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BaseErrorDialog;
import com.android.server.am.ScoutMeminfo;
import com.android.server.am.ScoutMemoryError;
import com.android.server.content.SyncManagerStubImpl;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.ExceptionEvent;
import miui.mqsas.sdk.event.GeneralExceptionEvent;

/* loaded from: classes.dex */
public class ScoutDisplayMemoryManager {
    private static final String APP_ID = "31000000454";
    private static final int DISPLAY_DMABUF = 1;
    private static final int DISPLAY_GPU_MEMORY = 2;
    private static final String DMABUF_EVENT_NAME = "dmabuf_leak";
    private static final int DMABUF_LEAK_SF_THRESHOLD;
    private static final int DMABUF_LEAK_THRESHOLD;
    private static final boolean ENABLE_SCOUT_MEMORY_MONITOR;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final long GBTOKB = 1048576;
    private static final String GPU_MEMORY_EVENT_NAME = "kgsl_leak";
    private static final int GPU_MEMORY_LEAK_THRESHOLD;
    private static final long MBTOKB = 1024;
    private static final int MEM_DISABLE_REPORT_INTERVAL = 600000;
    private static final int MEM_ERROR_DIALOG_TIMEOUT = 300000;
    private static final int MEM_REPORT_INTERVAL = 3600000;
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String PACKAGE = "android";
    private static final double PROC_PROPORTIONAL_THRESHOLD = 0.6d;
    public static final int RESUME_ACTION_CANCLE = 5;
    public static final int RESUME_ACTION_CONFIRM = 4;
    public static final int RESUME_ACTION_CRASH = 2;
    public static final int RESUME_ACTION_DIALOG = 3;
    public static final int RESUME_ACTION_FAIL = 0;
    public static final int RESUME_ACTION_KILL = 1;
    private static final boolean SCOUT_MEMORY_DISABLE_DMABUF;
    private static final boolean SCOUT_MEMORY_DISABLE_GPU;
    private static final String SYSPROP_DMABUF_LEAK_THRESHOLD = "persist.sys.debug.scout_memory_dmabuf_threshold";
    private static final String SYSPROP_ENABLE_SCOUT_MEMORY_MONITOR = "persist.sys.debug.enable_scout_memory_monitor";
    private static final String SYSPROP_ENABLE_SCOUT_MEMORY_RESUME = "persist.sys.debug.enable_scout_memory_resume";
    private static final String SYSPROP_GPU_MEMORY_LEAK_THRESHOLD = "persist.sys.debug.scout_memory_gpu_threshold";
    private static final String SYSPROP_PRESERVE_CRIME_SCENE = "persist.sys.debug.preserve_scout_memory_leak_scene";
    private static final String SYSPROP_SCOUT_MEMORY_DISABLE_DMABUF = "persist.sys.debug.scout_memory_disable_dmabuf";
    private static final String SYSPROP_SCOUT_MEMORY_DISABLE_GPU = "persist.sys.debug.scout_memory_disable_gpu";
    private static final String TAG = "ScoutDisplayMemoryManager";
    private static boolean debug;
    private static ScoutDisplayMemoryManager displayMemoryManager;
    private int gpuType;
    private boolean isCameraForeground;
    private Context mContext;
    private int mCrashTimes;
    private int mLastCrashPid;
    private String mLastCrashProc;
    private ActivityManagerService mService;
    private volatile long mShowDialogTime;
    private final AtomicBoolean isBusy = new AtomicBoolean(false);
    private final AtomicBoolean mIsShowDialog = new AtomicBoolean(false);
    private final AtomicBoolean mDisableState = new AtomicBoolean(false);
    private int totalRam = 0;
    private volatile long mLastReportTime = 0;

    private native long getTotalGpuMemory(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public native DmaBufUsageInfo readDmabufInfo();

    /* JADX INFO: Access modifiers changed from: private */
    public native GpuMemoryUsageInfo readGpuMemoryInfo(int i);

    static {
        debug = ScoutHelper.ENABLED_SCOUT_DEBUG || ScoutUtils.isLibraryTest();
        DMABUF_LEAK_THRESHOLD = SystemProperties.getInt(SYSPROP_DMABUF_LEAK_THRESHOLD, 2560);
        DMABUF_LEAK_SF_THRESHOLD = SystemProperties.getInt(SYSPROP_DMABUF_LEAK_THRESHOLD, 3000);
        GPU_MEMORY_LEAK_THRESHOLD = SystemProperties.getInt(SYSPROP_GPU_MEMORY_LEAK_THRESHOLD, 2560);
        ENABLE_SCOUT_MEMORY_MONITOR = SystemProperties.getBoolean(SYSPROP_ENABLE_SCOUT_MEMORY_MONITOR, false);
        SCOUT_MEMORY_DISABLE_GPU = SystemProperties.getBoolean(SYSPROP_SCOUT_MEMORY_DISABLE_GPU, false);
        SCOUT_MEMORY_DISABLE_DMABUF = SystemProperties.getBoolean(SYSPROP_SCOUT_MEMORY_DISABLE_DMABUF, false);
    }

    public static ScoutDisplayMemoryManager getInstance() {
        if (displayMemoryManager == null) {
            displayMemoryManager = new ScoutDisplayMemoryManager();
        }
        return displayMemoryManager;
    }

    private ScoutDisplayMemoryManager() {
    }

    private int initGpuType() {
        File kgslFile = new File(ScoutMemoryUtils.FILE_KGSL);
        if (kgslFile.exists()) {
            return 1;
        }
        File maliFile = new File(ScoutMemoryUtils.FILE_MALI);
        if (maliFile.exists()) {
            return 2;
        }
        return 0;
    }

    public void init(ActivityManagerService mService, Context mContext) {
        this.mService = mService;
        this.mContext = mContext;
        ScoutMemoryError.getInstance().init(mService, mContext);
        this.gpuType = initGpuType();
        Slog.d("MIUIScout Memory", "gpuType = " + this.gpuType);
    }

    public boolean isEnableScoutMemory() {
        return ENABLE_SCOUT_MEMORY_MONITOR;
    }

    public boolean isEnableResumeFeature() {
        return ScoutUtils.isLibraryTest() || ScoutUtils.isUnReleased() || SystemProperties.getBoolean(SYSPROP_ENABLE_SCOUT_MEMORY_RESUME, false);
    }

    public String getDmabufUsageInfo() {
        DmaBufUsageInfo dmabufInfo = readDmabufInfo();
        if (dmabufInfo != null) {
            ArrayList<DmaBufProcUsageInfo> infoList = dmabufInfo.getList();
            Collections.sort(infoList, new Comparator<DmaBufProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.1
                @Override // java.util.Comparator
                public int compare(DmaBufProcUsageInfo o1, DmaBufProcUsageInfo o2) {
                    if (o2.getRss() == o1.getRss()) {
                        return 0;
                    }
                    if (o2.getRss() > o1.getRss()) {
                        return 1;
                    }
                    return -1;
                }
            });
            return dmabufInfo.toString();
        }
        return null;
    }

    public String getGpuMemoryUsageInfo() {
        GpuMemoryUsageInfo gpuInfo;
        int i = this.gpuType;
        if (i == 0 || (gpuInfo = readGpuMemoryInfo(i)) == null) {
            return null;
        }
        ArrayList<GpuMemoryProcUsageInfo> infoList = gpuInfo.getList();
        Collections.sort(infoList, new Comparator<GpuMemoryProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.2
            @Override // java.util.Comparator
            public int compare(GpuMemoryProcUsageInfo o1, GpuMemoryProcUsageInfo o2) {
                if (o2.getRss() == o1.getRss()) {
                    return 0;
                }
                if (o2.getRss() > o1.getRss()) {
                    return 1;
                }
                return -1;
            }
        });
        return gpuInfo.toString(this.gpuType);
    }

    public void updateCameraForegroundState(boolean isCameraForeground) {
        this.isCameraForeground = isCameraForeground;
    }

    public void updateLastReportTime() {
        this.mLastReportTime = SystemClock.uptimeMillis();
    }

    public void setDisableState(boolean state) {
        this.mDisableState.set(state);
    }

    public void setShowDialogState(boolean state) {
        this.mIsShowDialog.set(state);
        if (state) {
            this.mShowDialogTime = SystemClock.uptimeMillis();
        }
    }

    private int getTotalRam() {
        int i = this.totalRam;
        if (i > 0) {
            return i;
        }
        long[] infos = new long[27];
        Debug.getMemInfo(infos);
        this.totalRam = (int) (infos[0] / 1048576);
        if (debug) {
            Slog.d("MIUIScout Memory", "getTotalRam total memory " + infos[0] + " kB, " + this.totalRam + "GB");
        }
        return this.totalRam;
    }

    public void reportDisplayMemoryLeakEvent(final DiaplayMemoryErrorInfo errorInfo) {
        if (this.mContext == null || errorInfo == null) {
            return;
        }
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ScoutDisplayMemoryManager.this.lambda$reportDisplayMemoryLeakEvent$0(errorInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$reportDisplayMemoryLeakEvent$0(DiaplayMemoryErrorInfo errorInfo) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE);
        if (errorInfo.getType() == 1) {
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, DMABUF_EVENT_NAME);
            intent.putExtra("dmabuf_total_size", String.valueOf(errorInfo.getTotalSize()));
        } else if (errorInfo.getType() == 2) {
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, GPU_MEMORY_EVENT_NAME);
            intent.putExtra("kgsl_total_size", String.valueOf(errorInfo.getTotalSize()));
        } else {
            return;
        }
        intent.putExtra("memory_total_size", getTotalRam());
        intent.putExtra("memory_app_package", errorInfo.getName());
        intent.putExtra("memory_app_size", String.valueOf(errorInfo.getRss()));
        intent.putExtra("memory_app_adj", String.valueOf(errorInfo.getOomadj()));
        intent.putExtra("resume_action", String.valueOf(errorInfo.getAction()));
        intent.setFlags(1);
        if (debug) {
            Slog.d("MIUIScout Memory", "report type=" + errorInfo.getType() + " memory_total_size=" + getTotalRam() + " total_size=" + errorInfo.getTotalSize() + " memory_app_package=" + errorInfo.getName() + " memory_app_size =" + errorInfo.getRss() + " memory_app_adj=" + errorInfo.getOomadj() + " action = " + errorInfo.getAction());
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "Upload onetrack exception!", e);
        }
    }

    public void handleReportMqs(final DiaplayMemoryErrorInfo errorInfo, final String filePath) {
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ScoutDisplayMemoryManager.this.lambda$handleReportMqs$1(filePath, errorInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$handleReportMqs$1(String filePath, DiaplayMemoryErrorInfo errorInfo) {
        long thresholdSize;
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        String[] strArr = new String[1];
        strArr[0] = filePath != null ? filePath : "";
        List<String> files = Arrays.asList(strArr);
        String sum = errorInfo.getName() + ", occurring " + errorInfo.getReason() + " OOM";
        if (errorInfo.getType() == 1) {
            event.setType(424);
            thresholdSize = DMABUF_LEAK_THRESHOLD * 1024;
        } else if (errorInfo.getType() == 2) {
            event.setType(425);
            thresholdSize = GPU_MEMORY_LEAK_THRESHOLD * 1024;
        } else {
            return;
        }
        event.setSummary(sum);
        event.setTimeStamp(System.currentTimeMillis());
        event.setPackageName(errorInfo.getName());
        event.setExtraFiles(files);
        event.setDetails(sum + "\ntotal_threshold: " + thresholdSize + "(kB)\nmemory_app_package: " + errorInfo.getName() + "\napp_threshold: " + errorInfo.getThreshold() + "(kB)\n");
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
        handleReportMisight(errorInfo.getType(), event);
    }

    private void handleReportMisight(int errorType, ExceptionEvent event) {
        MiEvent miEvent;
        if (errorType == 1) {
            miEvent = new MiEvent(901004201);
            miEvent.addStr("DmaBuf", event.getDetails());
        } else if (errorType == 2) {
            miEvent = new MiEvent(901004202);
            miEvent.addStr("Gpu", event.getDetails());
        } else {
            Slog.w(TAG, "handleReportMisight: invalid errorType");
            return;
        }
        miEvent.addStr("Summary", event.getSummary()).addStr("PackageName", event.getPackageName()).addLong("CurrentTime", System.currentTimeMillis());
        MiSight.sendEvent(miEvent);
    }

    static String stringifySize(long size, int order) {
        Locale locale = Locale.US;
        switch (order) {
            case 1:
                return String.format(locale, "%,13d", Long.valueOf(size));
            case 1024:
                return String.format(locale, "%,9dkB", Long.valueOf(size / 1024));
            case 1048576:
                return String.format(locale, "%,5dMB", Long.valueOf((size / 1024) / 1024));
            case 1073741824:
                return String.format(locale, "%,1dGB", Long.valueOf(((size / 1024) / 1024) / 1024));
            default:
                throw new IllegalArgumentException("Invalid size order");
        }
    }

    static String stringifyKBSize(long size) {
        return stringifySize(1024 * size, 1024);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:39:0x01eb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void resumeMemLeak(com.miui.server.stability.ScoutDisplayMemoryManager.DiaplayMemoryErrorInfo r26) {
        /*
            Method dump skipped, instructions count: 721
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.stability.ScoutDisplayMemoryManager.resumeMemLeak(com.miui.server.stability.ScoutDisplayMemoryManager$DiaplayMemoryErrorInfo):void");
    }

    private void reportDmabufLeakException(final String reason, final long totalSizeKB) {
        if (this.isBusy.compareAndSet(false, true)) {
            new Thread(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.3
                @Override // java.lang.Runnable
                public void run() {
                    Slog.e("MIUIScout Memory", "Warring!!! dma-buf leak:" + ScoutDisplayMemoryManager.stringifyKBSize(totalSizeKB) + (ScoutDisplayMemoryManager.this.isCameraForeground ? " , camera in the foreground" : ""));
                    String dambufLog = "";
                    DmaBufUsageInfo dmabufInfo = ScoutDisplayMemoryManager.this.readDmabufInfo();
                    ArrayList<DmaBufProcUsageInfo> infoList = null;
                    if (dmabufInfo != null) {
                        infoList = dmabufInfo.getList();
                        Collections.sort(infoList, new Comparator<DmaBufProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.3.1
                            @Override // java.util.Comparator
                            public int compare(DmaBufProcUsageInfo o1, DmaBufProcUsageInfo o2) {
                                if (o2.getRss() == o1.getRss()) {
                                    return 0;
                                }
                                if (o2.getRss() > o1.getRss()) {
                                    return 1;
                                }
                                return -1;
                            }
                        });
                        dambufLog = dmabufInfo.toString();
                        Slog.e("MIUIScout Memory", dambufLog);
                        if (dmabufInfo.getTotalSize() < ScoutDisplayMemoryManager.DMABUF_LEAK_THRESHOLD * 1024) {
                            String failMsg = "TotalSize " + ScoutDisplayMemoryManager.stringifyKBSize(totalSizeKB) + " dma-buf, DmabufInfo totalSize " + dmabufInfo.getTotalSize() + "kB less than threshold, Skip";
                            Slog.e("MIUIScout Memory", failMsg);
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                    }
                    DmaBufProcUsageInfo topProc = null;
                    DmaBufProcUsageInfo sfProc = null;
                    if (infoList != null) {
                        HashSet<Integer> fdPids = new HashSet<>(5);
                        Iterator<DmaBufProcUsageInfo> it = infoList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            DmaBufProcUsageInfo procInfo = it.next();
                            String procName = procInfo.getName();
                            if ("/system/bin/surfaceflinger".equals(procName)) {
                                sfProc = procInfo;
                            }
                            if (ScoutMemoryUtils.skipIonProcList.contains(procName)) {
                                fdPids.add(Integer.valueOf(procInfo.getPid()));
                                Slog.e("MIUIScout Memory", "Skip " + procName + "(pid=" + procInfo.getPid() + " adj=" + procInfo.getOomadj() + ")");
                            } else {
                                topProc = procInfo;
                                fdPids.add(Integer.valueOf(topProc.getPid()));
                                Slog.e("MIUIScout Memory", "Most used process name=" + procName + " pid=" + procInfo.getPid() + " adj=" + procInfo.getOomadj() + " rss=" + procInfo.getRss());
                                break;
                            }
                        }
                        if (topProc != null) {
                            fdPids.add(Integer.valueOf(Process.myPid()));
                            String zipPath = ScoutMemoryUtils.captureIonLeakLog(dambufLog, reason, fdPids);
                            long thresholdSize = (long) (ScoutDisplayMemoryManager.DMABUF_LEAK_THRESHOLD * 1024 * ScoutDisplayMemoryManager.PROC_PROPORTIONAL_THRESHOLD);
                            DiaplayMemoryErrorInfo errorInfo = new DiaplayMemoryErrorInfo(topProc, thresholdSize, totalSizeKB);
                            if (topProc.getRss() > thresholdSize && !ScoutDisplayMemoryManager.preserveCrimeSceneIfNeed(errorInfo)) {
                                if (ScoutDisplayMemoryManager.this.isEnableResumeFeature()) {
                                    ScoutDisplayMemoryManager.this.resumeMemLeak(errorInfo);
                                } else {
                                    ScoutDisplayMemoryManager.this.reportDisplayMemoryLeakEvent(errorInfo);
                                }
                                ScoutDisplayMemoryManager.this.handleReportMqs(errorInfo, zipPath);
                            } else if (ScoutUtils.isLibraryTest() && sfProc != null) {
                                if (sfProc.getRss() > ScoutDisplayMemoryManager.DMABUF_LEAK_SF_THRESHOLD * 1024 && !ScoutDisplayMemoryManager.doPreserveCrimeSceneIfNeed("surfaceflinger", -1, totalSizeKB, "dmabuf")) {
                                    Slog.w("MIUIScout Memory", "surfaceflinger consumed " + totalSizeKB + "KB dmabuf memory totally, killing systemui and miui home");
                                    SurfaceControlImpl scImpl = SurfaceControlStub.getInstance();
                                    scImpl.startAsyncDumpIfNeed(false);
                                    scImpl.applyGenialRenovation();
                                    scImpl.increaseLeakLevel(true);
                                }
                            }
                            ScoutDisplayMemoryManager.this.updateLastReportTime();
                        }
                    }
                    ScoutDisplayMemoryManager.this.isBusy.set(false);
                }
            }, reason + "-" + stringifyKBSize(totalSizeKB)).start();
        } else {
            Slog.d("MIUIScout Memory", "Is Busy! skip report Dma-buf Leak Exception");
        }
    }

    private void reportGpuMemoryLeakException(final String reason, final long totalSizeKB) {
        if (this.isBusy.compareAndSet(false, true)) {
            new Thread(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.4
                @Override // java.lang.Runnable
                public void run() {
                    String gpuMemoryLog = "";
                    ScoutDisplayMemoryManager scoutDisplayMemoryManager = ScoutDisplayMemoryManager.this;
                    GpuMemoryUsageInfo gpuMemoryInfo = scoutDisplayMemoryManager.readGpuMemoryInfo(scoutDisplayMemoryManager.gpuType);
                    ArrayList<GpuMemoryProcUsageInfo> infoList = null;
                    if (gpuMemoryInfo != null) {
                        infoList = gpuMemoryInfo.getList();
                        Collections.sort(infoList, new Comparator<GpuMemoryProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.4.1
                            @Override // java.util.Comparator
                            public int compare(GpuMemoryProcUsageInfo o1, GpuMemoryProcUsageInfo o2) {
                                if (o2.getRss() == o1.getRss()) {
                                    return 0;
                                }
                                if (o2.getRss() > o1.getRss()) {
                                    return 1;
                                }
                                return -1;
                            }
                        });
                        gpuMemoryLog = gpuMemoryInfo.toString(ScoutDisplayMemoryManager.this.gpuType);
                        Slog.e("MIUIScout Memory", gpuMemoryLog);
                        if (gpuMemoryInfo.getTotalSize() < ScoutDisplayMemoryManager.GPU_MEMORY_LEAK_THRESHOLD * 1024) {
                            String failMsg = "TotalSize " + ScoutDisplayMemoryManager.stringifyKBSize(totalSizeKB) + " gpu memory, GpuMemoryInfo totalSize " + gpuMemoryInfo.getTotalSize() + "kB less than threshold, Skip";
                            Slog.e("MIUIScout Memory", failMsg);
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                    }
                    GpuMemoryProcUsageInfo sfProc = null;
                    if (gpuMemoryLog != null && infoList != null && infoList.size() > 0) {
                        GpuMemoryProcUsageInfo topProc = infoList.get(0);
                        Slog.e("MIUIScout Memory", "Most used process name=" + topProc.getName() + " pid=" + topProc.getPid() + " adj=" + topProc.getOomadj() + " size=" + topProc.getRss() + "kB");
                        if (ScoutMemoryUtils.gpuMemoryWhiteList.contains(topProc.getName())) {
                            Slog.e("MIUIScout Memory", topProc.getName() + " is white app, skip");
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                        if ("/system/bin/surfaceflinger".equals(topProc.getName())) {
                            sfProc = topProc;
                        }
                        String filePath = ScoutMemoryUtils.captureGpuMemoryLeakLog(gpuMemoryLog, reason, ScoutDisplayMemoryManager.this.gpuType);
                        long thresholdSize = (long) (ScoutDisplayMemoryManager.GPU_MEMORY_LEAK_THRESHOLD * 1024 * ScoutDisplayMemoryManager.PROC_PROPORTIONAL_THRESHOLD);
                        DiaplayMemoryErrorInfo errorInfo = new DiaplayMemoryErrorInfo(topProc, thresholdSize, totalSizeKB);
                        if (topProc.getRss() > thresholdSize && !ScoutDisplayMemoryManager.preserveCrimeSceneIfNeed(errorInfo)) {
                            if (ScoutDisplayMemoryManager.this.isEnableResumeFeature()) {
                                ScoutDisplayMemoryManager.this.resumeMemLeak(errorInfo);
                            } else {
                                ScoutDisplayMemoryManager.this.reportDisplayMemoryLeakEvent(errorInfo);
                            }
                            ScoutDisplayMemoryManager.this.handleReportMqs(errorInfo, filePath);
                        } else if (ScoutUtils.isLibraryTest() && sfProc != null && sfProc.getRss() > ScoutDisplayMemoryManager.GPU_MEMORY_LEAK_THRESHOLD * 1024 && !ScoutDisplayMemoryManager.doPreserveCrimeSceneIfNeed("surfaceflinger", -1, totalSizeKB, "GpuMemory")) {
                            Slog.w("MIUIScout Memory", "System consumed " + totalSizeKB + "KB GpuMemory totally");
                        }
                        ScoutDisplayMemoryManager.this.updateLastReportTime();
                    }
                    ScoutDisplayMemoryManager.this.isBusy.set(false);
                }
            }, reason + "-" + stringifyKBSize(totalSizeKB)).start();
        } else {
            Slog.d("MIUIScout Memory", "Is Busy! skip report Gpu Memory Leak Exception");
        }
    }

    private boolean checkDmaBufLeak(ScoutMeminfo scoutMeminfo) {
        long dmaBufTotal;
        if (SCOUT_MEMORY_DISABLE_DMABUF) {
            return false;
        }
        long ionHeap = Debug.getIonHeapsSizeKb();
        if (ionHeap >= 0) {
            dmaBufTotal = ionHeap;
        } else {
            dmaBufTotal = scoutMeminfo.getTotalExportedDmabuf();
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "checkDmaBufLeak(root) size:" + stringifyKBSize(dmaBufTotal) + " monitor threshold = " + (DMABUF_LEAK_THRESHOLD * 1024) + "kB");
        }
        if (dmaBufTotal <= DMABUF_LEAK_THRESHOLD * 1024) {
            return false;
        }
        reportDmabufLeakException(DMABUF_EVENT_NAME, dmaBufTotal);
        return true;
    }

    private boolean checkDmaBufLeak() {
        long dmaBufTotal;
        if (SCOUT_MEMORY_DISABLE_DMABUF) {
            return false;
        }
        long ionHeap = Debug.getIonHeapsSizeKb();
        if (ionHeap >= 0) {
            dmaBufTotal = ionHeap;
        } else {
            dmaBufTotal = Debug.getDmabufTotalExportedKb();
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "checkDmaBufLeak size:" + stringifyKBSize(dmaBufTotal) + " monitor threshold = " + (DMABUF_LEAK_THRESHOLD * 1024) + "kB");
        }
        if (dmaBufTotal <= DMABUF_LEAK_THRESHOLD * 1024) {
            return false;
        }
        reportDmabufLeakException(DMABUF_EVENT_NAME, dmaBufTotal);
        return true;
    }

    private boolean checkGpuMemoryLeak() {
        int i = this.gpuType;
        if (i < 1 || SCOUT_MEMORY_DISABLE_GPU) {
            return false;
        }
        long totalGpuMemoru = getTotalGpuMemory(i);
        if (debug) {
            Slog.d("MIUIScout Memory", "checkGpuMemoryLeak size:" + stringifyKBSize(totalGpuMemoru) + " monitor threshold = " + (GPU_MEMORY_LEAK_THRESHOLD * 1024) + "kB gpuType = " + this.gpuType);
        }
        if (totalGpuMemoru <= GPU_MEMORY_LEAK_THRESHOLD * 1024) {
            return false;
        }
        reportGpuMemoryLeakException("GpuMemory_leak", totalGpuMemoru);
        return true;
    }

    public void checkScoutLowMemory(ScoutMeminfo scoutMeminfo) {
        if (scoutMeminfo == null || !isEnableScoutMemory()) {
            return;
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "check MIUI Scout display memory(root)");
        }
        if (!checkGpuMemoryLeak()) {
            checkDmaBufLeak(scoutMeminfo);
        }
    }

    public void checkScoutLowMemory() {
        if (this.mService == null || !isEnableScoutMemory()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (this.mIsShowDialog.get()) {
            if (now < this.mShowDialogTime + 300000) {
                Slog.w("MIUIScout Memory", "skip check display memory leak, dialog show");
                return;
            }
            setShowDialogState(false);
        }
        if (this.mLastReportTime != 0 && ((this.mDisableState.get() && now < this.mLastReportTime + 600000) || (!isEnableResumeFeature() && now < this.mLastReportTime + SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED))) {
            if (debug) {
                Slog.d("MIUIScout Memory", "mLastReportTime = " + this.mLastReportTime + " mDisableState = " + this.mDisableState.get() + " now = " + now + " MEM_DISABLE_REPORT_INTERVAL = " + MEM_DISABLE_REPORT_INTERVAL + " MEM_REPORT_INTERVAL = " + MEM_REPORT_INTERVAL);
            }
            Slog.d("MIUIScout Memory", "skip check display memory leak, less than last check interval");
            return;
        }
        if (this.mDisableState.get()) {
            this.mDisableState.set(false);
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "check MIUI Scout display memory");
        }
        if (!checkGpuMemoryLeak()) {
            checkDmaBufLeak();
        }
    }

    static boolean preserveCrimeSceneIfNeed(DiaplayMemoryErrorInfo errorInfo) {
        String leakingProcess = errorInfo.getName();
        int pid = errorInfo.getPid();
        long rss = errorInfo.getRss();
        String leakType = errorInfo.getReason();
        return doPreserveCrimeSceneIfNeed(leakingProcess, pid, rss, leakType);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean doPreserveCrimeSceneIfNeed(String leakingProcess, int pid, long rssKB, String leakType) {
        if (!Build.IS_DEBUGGABLE) {
            return false;
        }
        String[] processList = SystemProperties.get(SYSPROP_PRESERVE_CRIME_SCENE).split(",");
        if (!ArrayUtils.containsAny(processList, new String[]{"any", leakingProcess})) {
            return false;
        }
        try {
            IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            pm.setApplicationEnabledSetting("com.phonetest.stresstest", 2, 0, 0, "MIUIScout Memory");
        } catch (Exception e) {
            Slog.d("MIUIScout Memory", "Failed to disable MTBF app", e);
        }
        int[] monkeyPids = Process.getPidsForCommands(new String[]{"com.android.commands.monkey"});
        if (monkeyPids != null) {
            for (int monkeyPid : monkeyPids) {
                Slog.d("MIUIScout Memory", "Killing monkey process: pid=" + monkeyPid);
                try {
                    Os.kill(monkeyPid, 9);
                } catch (Exception e2) {
                }
            }
        }
        String title = leakType.toUpperCase() + " MEMORY LEAKED";
        String msg = leakingProcess + ":" + pid + " consumed " + rssKB + "KB memory, please contact the engineers for help ASAP!";
        CrimeScenePreservedDialog.show(title, msg);
        return true;
    }

    /* loaded from: classes.dex */
    public static final class CrimeScenePreservedDialog {
        private static BaseErrorDialog sDialog = null;

        public static void show(final String title, final String msg) {
            Slog.w("MIUIScout Memory", title + ": " + msg);
            Runnable task = new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager$CrimeScenePreservedDialog$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ScoutDisplayMemoryManager.CrimeScenePreservedDialog.lambda$show$0(title, msg);
                }
            };
            UiThread.getHandler().postAtFrontOfQueue(task);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$show$0(String title, String msg) {
            BaseErrorDialog baseErrorDialog = sDialog;
            if (baseErrorDialog != null) {
                baseErrorDialog.dismiss();
                sDialog = null;
            }
            ContextImpl uiContext = ActivityThread.currentActivityThread().getSystemUiContext();
            BaseErrorDialog dialog = new BaseErrorDialog(uiContext) { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.CrimeScenePreservedDialog.1
                protected void closeDialog() {
                }
            };
            dialog.setTitle(title);
            dialog.setMessage(msg);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            Window window = dialog.getWindow();
            if (window == null) {
                Slog.w("MIUIScout Memory", "Cannot show dialog: no window");
                return;
            }
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.setTitle(title);
            dialog.show();
            sDialog = dialog;
        }
    }

    /* loaded from: classes.dex */
    public static class DiaplayMemoryErrorInfo {
        private int action;
        private int adj;
        private int pid;
        private String procName;
        private String reason;
        private long rss;
        private long threshold;
        private long totalSize;
        private int type;

        public DiaplayMemoryErrorInfo(GpuMemoryProcUsageInfo gpuMemoryInfo, long thresholdSize, long totalSize) {
            this.pid = gpuMemoryInfo.getPid();
            this.procName = gpuMemoryInfo.getName();
            this.rss = gpuMemoryInfo.getRss();
            this.adj = gpuMemoryInfo.getOomadj();
            this.threshold = thresholdSize;
            this.totalSize = totalSize;
            this.reason = "GpuMemory";
            this.type = 2;
        }

        public DiaplayMemoryErrorInfo(DmaBufProcUsageInfo dmabufInfo, long thresholdSize, long totalSize) {
            this.pid = dmabufInfo.getPid();
            this.procName = dmabufInfo.getName();
            this.rss = dmabufInfo.getRss();
            this.adj = dmabufInfo.getOomadj();
            this.threshold = thresholdSize;
            this.totalSize = totalSize;
            this.reason = "DMA-BUF";
            this.type = 1;
        }

        public void setName(String procName) {
            this.procName = procName;
        }

        public String getName() {
            return this.procName;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        public int getPid() {
            return this.pid;
        }

        public void setOomadj(int adj) {
            this.adj = adj;
        }

        public int getOomadj() {
            return this.adj;
        }

        public void setRss(long rss) {
            this.rss = rss;
        }

        public long getRss() {
            return this.rss;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }

        public long getThreshold() {
            return this.threshold;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public long getTotalSize() {
            return this.totalSize;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return this.reason;
        }

        public void setAction(int action) {
            this.action = action;
        }

        public int getAction() {
            return this.action;
        }

        public int getType() {
            return this.type;
        }
    }
}
