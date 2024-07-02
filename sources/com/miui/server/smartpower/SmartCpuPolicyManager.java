package com.miui.server.smartpower;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.util.IntArray;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppStateManager;
import com.android.server.am.ProcessCleanerBase;
import com.android.server.am.SystemPressureControllerStub;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class SmartCpuPolicyManager {
    private static final int CPU_EXCEPTION_HANDLE_THRESHOLD = 30000;
    private static final String CPU_EXCEPTION_KILL_REASON = "cpu exception";
    public static final boolean DEBUG = PressureStateSettings.DEBUG_ALL;
    public static final int DEFAULT_BACKGROUND_CPU_CORE_NUM = 4;
    private static final int MONITOR_CPU_MIN_TIME = 10000;
    static final boolean MONITOR_THREAD_CPU_USAGE = true;
    private static final int MSG_CPU_EXCEPTION = 7;
    public static final String TAG = "SmartPower.CpuPolicy";
    private ActivityManagerService mAMS;
    private H mHandler;
    private SmartPowerPolicyManager mSmartPowerPolicyManager;
    private AppStateManager mAppStateManager = null;
    private HandlerThread mHandlerTh = new HandlerThread("CpuPolicyTh", -2);
    private int mBackgroundCpuCoreNum = 4;
    private long mLashHandleCpuException = 0;
    private final AtomicLong mLastUpdateCpuTime = new AtomicLong(0);
    private final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(true);

    public SmartCpuPolicyManager(Context context, ActivityManagerService ams) {
        this.mAMS = ams;
    }

    public void init(AppStateManager appStateManager, SmartPowerPolicyManager smartPowerPolicyManager) {
        this.mAppStateManager = appStateManager;
        this.mSmartPowerPolicyManager = smartPowerPolicyManager;
        this.mHandlerTh.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
    }

    public static boolean isEnableCpuLimit() {
        return false;
    }

    public int getBackgroundCpuCoreNum() {
        return this.mBackgroundCpuCoreNum;
    }

    public void updateBackgroundCpuCoreNum() {
        String bgCpu = SystemPressureControllerStub.getInstance().getBackgroundCpuPolicy().trim();
        IntArray cpuCores = parseCpuCores(bgCpu);
        if (cpuCores == null || cpuCores.size() == 0) {
            Slog.i(TAG, "Failed to parse CPU cores from " + bgCpu);
        } else {
            this.mBackgroundCpuCoreNum = cpuCores.size();
        }
    }

    public static IntArray parseCpuCores(String cpuContent) {
        IntArray cpuCores = new IntArray(0);
        try {
            String[] pairs = cpuContent.contains(",") ? cpuContent.trim().split(",") : cpuContent.trim().split(" ");
            for (int j = 0; j < pairs.length; j++) {
                String[] minMaxPairs = pairs[j].split("-");
                if (minMaxPairs.length >= 2) {
                    int min = Integer.parseInt(minMaxPairs[0]);
                    int max = Integer.parseInt(minMaxPairs[1]);
                    if (min <= max) {
                        for (int id = min; id <= max; id++) {
                            cpuCores.add(id);
                        }
                    }
                } else if (minMaxPairs.length != 1) {
                    Slog.i(TAG, "Invalid CPU core range format " + pairs[j]);
                } else {
                    cpuCores.add(Integer.parseInt(minMaxPairs[0]));
                }
            }
        } catch (Exception e) {
            Slog.i(TAG, "Failed to parse CPU cores from " + cpuContent);
        }
        return cpuCores;
    }

    public void cpuPressureEvents(int level) {
    }

    public void cpuExceptionEvents(int type) {
        H h;
        if (PressureStateSettings.PROC_CPU_EXCEPTION_ENABLE && SystemClock.uptimeMillis() - this.mLashHandleCpuException > 30000 && (h = this.mHandler) != null && !h.hasMessages(7)) {
            Message msg = this.mHandler.obtainMessage(7);
            msg.arg1 = type;
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLimitCpuException(int type) {
        boolean z;
        SmartCpuPolicyManager smartCpuPolicyManager = this;
        smartCpuPolicyManager.mLashHandleCpuException = SystemClock.uptimeMillis();
        boolean z2 = false;
        long uptimeSince = smartCpuPolicyManager.updateCpuStatsNow(false);
        long j = 0;
        if (uptimeSince <= 0) {
            return;
        }
        Slog.d(TAG, "HandleLimitCpuException: type=" + type + " bgcpu=" + smartCpuPolicyManager.mBackgroundCpuCoreNum + " over:" + TimeUtils.formatDuration(uptimeSince));
        int flag = ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS;
        ArrayList<IAppState> appStateList = smartCpuPolicyManager.mAppStateManager.getAllAppState();
        Iterator<IAppState> it = appStateList.iterator();
        while (it.hasNext()) {
            IAppState appState = it.next();
            long backgroundUpdateTime = smartCpuPolicyManager.mLashHandleCpuException - appState.getLastTopTime();
            if (appState.isVsible()) {
                smartCpuPolicyManager = this;
                uptimeSince = uptimeSince;
                flag = flag;
                j = 0;
            } else if (backgroundUpdateTime >= 30010) {
                Iterator it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    IAppState.IRunningProcess runningProc = (IAppState.IRunningProcess) it2.next();
                    long uptimeSince2 = uptimeSince;
                    long curCpuTime = runningProc.getCurCpuTime();
                    long lastCpuTime = runningProc.getLastCpuTime();
                    runningProc.setLastCpuTime(curCpuTime);
                    if (lastCpuTime <= j) {
                        smartCpuPolicyManager = this;
                        z2 = false;
                        uptimeSince = uptimeSince2;
                        flag = flag;
                        j = 0;
                    } else if (!smartCpuPolicyManager.checkProcessRecord(runningProc)) {
                        smartCpuPolicyManager = this;
                        z2 = false;
                        uptimeSince = uptimeSince2;
                        flag = flag;
                        j = 0;
                    } else if (runningProc.isProcessPerceptible()) {
                        smartCpuPolicyManager = this;
                        z2 = false;
                        uptimeSince = uptimeSince2;
                        flag = flag;
                        j = 0;
                    } else if (smartCpuPolicyManager.mSmartPowerPolicyManager.isProcessWhiteList(flag, runningProc.getPackageName(), runningProc.getProcessName())) {
                        uptimeSince = uptimeSince2;
                        z2 = false;
                        j = 0;
                    } else {
                        int flag2 = flag;
                        long cpuTimeUsed = (curCpuTime - lastCpuTime) / smartCpuPolicyManager.mBackgroundCpuCoreNum;
                        if ((((float) cpuTimeUsed) * 100.0f) / ((float) uptimeSince2) >= PressureStateSettings.PROC_CPU_EXCEPTION_THRESHOLD) {
                            String reason = "cpu exception over " + TimeUtils.formatDuration(uptimeSince2) + " used " + TimeUtils.formatDuration(cpuTimeUsed) + " (" + ((((float) cpuTimeUsed) * 100.0f) / ((float) uptimeSince2)) + "%)";
                            z = false;
                            SystemPressureControllerStub.getInstance().killProcess(runningProc.getProcessRecord(), 0, reason);
                        } else {
                            z = false;
                        }
                        runningProc.setLastCpuTime(curCpuTime);
                        smartCpuPolicyManager = this;
                        z2 = z;
                        uptimeSince = uptimeSince2;
                        flag = flag2;
                        j = 0;
                    }
                }
                smartCpuPolicyManager = this;
                uptimeSince = uptimeSince;
                flag = flag;
                j = 0;
            }
        }
    }

    private void forAllCpuStats(Consumer<ProcessCpuTracker.Stats> consumer) {
        synchronized (this.mProcessCpuTracker) {
            int numOfStats = this.mProcessCpuTracker.countStats();
            for (int i = 0; i < numOfStats; i++) {
                consumer.accept(this.mProcessCpuTracker.getStats(i));
            }
        }
    }

    public long updateCpuStatsNow(final boolean isReset) {
        synchronized (this.mProcessCpuTracker) {
            long nowTime = SystemClock.uptimeMillis();
            if (nowTime - this.mLastUpdateCpuTime.get() < 10000) {
                return 0L;
            }
            Slog.d(TAG, "update process cpu time");
            updateBackgroundCpuCoreNum();
            this.mProcessCpuTracker.update();
            if (!this.mProcessCpuTracker.hasGoodLastStats()) {
                return 0L;
            }
            long uptimeSince = nowTime - this.mLastUpdateCpuTime.get();
            this.mLastUpdateCpuTime.set(nowTime);
            forAllCpuStats(new Consumer() { // from class: com.miui.server.smartpower.SmartCpuPolicyManager$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SmartCpuPolicyManager.this.lambda$updateCpuStatsNow$0(isReset, (ProcessCpuTracker.Stats) obj);
                }
            });
            return uptimeSince;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateCpuStatsNow$0(boolean isReset, ProcessCpuTracker.Stats st) {
        AppStateManager.AppState.RunningProcess proc;
        if (!st.working || st.uid <= 1000 || st.pid <= 0 || (proc = this.mAppStateManager.getRunningProcess(st.uid, st.pid)) == null || proc.isKilled()) {
            return;
        }
        long curCpuTime = proc.addAndGetCurCpuTime(st.rel_utime + st.rel_stime);
        if (isReset) {
            proc.setLastCpuTime(curCpuTime);
        } else {
            proc.compareAndSetLastCpuTime(0L, curCpuTime);
        }
    }

    private boolean checkProcessRecord(IAppState.IRunningProcess proc) {
        return proc != null && proc.getAdj() > 200 && proc.getPid() > 0 && UserHandle.isApp(proc.getUid()) && !proc.isKilled();
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
                case 7:
                    try {
                        SmartCpuPolicyManager.this.handleLimitCpuException(msg.arg1);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                default:
                    return;
            }
        }
    }
}
