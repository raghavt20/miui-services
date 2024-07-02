package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.LocalServices;
import com.android.server.wm.WindowProcessUtils;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.process.ProcessConfig;

/* loaded from: classes.dex */
public class ProcessPowerCleaner extends ProcessCleanerBase {
    private static final float ACTIVE_PROCESS_MIX_THRESHOLD = PressureStateSettings.SCREEN_OFF_PROCESS_ACTIVE_MIX_THRESHOLD / 1.0f;
    private static final float ACTIVE_PROCESS_THRESHOLD_RATE = 0.75f;
    private static final double BACKGROUND_PROCESS_SINGLE_CPU_RATIO = 25.0d;
    private static final int CHECK_CPU_GROWTH_RATE = 2;
    private static final int CHECK_CPU_MAX_TIME_MS = 3600000;
    private static final String DESK_CLOCK_PROCESS_NAME = "com.android.deskclock";
    private static final int SCREEN_OFF_DELAYED_TIME = 300000;
    private static final int SCREEN_OFF_FROZEN_DELAYED_TIME = 180000;
    private static final String SCREEN_OFF_FROZEN_REASON = "lock off frozen";
    private static final int SCREEN_OFF_START_CPU_CHECK_TIME = 1500000;
    private static final String TAG = "ProcessPowerCleaner";
    private static final int THERMAL_KILL_ALL_PROCESS_MINADJ = 200;
    private ActivityManagerService mAMS;
    private float mActiveProcessThreshold;
    private int mCheckCPUTime;
    private Context mContext;
    private H mHandler;
    private boolean mIsScreenOffState;
    private boolean mLockOffCleanTestEnable;
    private float mLockOffCleanTestThreshold;
    private ProcessManagerService mPMS;
    private ProcessPolicy mProcessPolicy;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private SystemPressureController mSysPressureCtrl;

    public ProcessPowerCleaner(ActivityManagerService ams) {
        super(ams);
        this.mActiveProcessThreshold = PressureStateSettings.SCREEN_OFF_PROCESS_ACTIVE_THRESHOLD / 1.0f;
        this.mLockOffCleanTestThreshold = 0.001f;
        this.mCheckCPUTime = 600000;
        this.mIsScreenOffState = false;
        this.mLockOffCleanTestEnable = false;
        this.mAMS = ams;
    }

    public void systemReady(Context context, ProcessManagerService pms, Looper myLooper) {
        super.systemReady(context, pms);
        this.mPMS = pms;
        this.mProcessPolicy = pms.getProcessPolicy();
        this.mSysPressureCtrl = SystemPressureController.getInstance();
        this.mContext = context;
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        this.mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatus = new IntentFilter();
        screenStatus.addAction("android.intent.action.SCREEN_ON");
        screenStatus.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mScreenStatusReceiver, screenStatus);
        registerCloudObserver(context);
        updateCloudControlParas(context);
        this.mHandler = new H(myLooper);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final int AUTO_LOCK_OFF_EVENT = 4;
        public static final int KILL_ALL_EVENT = 2;
        public static final int SCREEN_OFF_EVENT = 16;
        public static final int SCREEN_OFF_FROZEN_EVENT = 5;
        public static final int SCREEN_ON_EVENT = 15;
        public static final int THERMAL_KILL_ALL_EVENT = 1;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ProcessConfig config = (ProcessConfig) msg.obj;
            switch (msg.what) {
                case 1:
                    Trace.traceBegin(524288L, "PowerThermalKillProc:" + ProcessPowerCleaner.this.getKillReason(config.getPolicy()));
                    ProcessPowerCleaner.this.handleThermalKillProc(config);
                    Trace.traceEnd(524288L);
                    return;
                case 2:
                    Trace.traceBegin(524288L, "PowerKillAll:" + ProcessPowerCleaner.this.getKillReason(config.getPolicy()));
                    boolean isKillSystemProc = config.getPolicy() != 20;
                    ProcessPowerCleaner.this.handleKillAll(config, isKillSystemProc);
                    Trace.traceEnd(524288L);
                    return;
                case 4:
                    Trace.traceBegin(524288L, "PowerScreenOffKill:" + ProcessPowerCleaner.this.getKillReason(22));
                    ProcessPowerCleaner.this.handleAutoLockOff();
                    Trace.traceEnd(524288L);
                    return;
                case 5:
                    ProcessPowerCleaner.this.powerFrozenAll();
                    return;
                case 15:
                    ProcessPowerCleaner.this.resetLockOffConfig();
                    return;
                case 16:
                    ProcessPowerCleaner.this.handleScreenOffEvent();
                    return;
                default:
                    return;
            }
        }
    }

    public boolean powerKillProcess(ProcessConfig config) {
        if (this.mHandler == null || config == null) {
            return false;
        }
        switch (config.getPolicy()) {
            case 11:
            case 12:
            case 13:
                boolean success = handleKillApp(config);
                return success;
            case 14:
            case 16:
            case 20:
                H h = this.mHandler;
                h.sendMessage(createMessage(2, config, h));
                return false;
            case 15:
            case 17:
            case 18:
            default:
                return false;
            case 19:
                H h2 = this.mHandler;
                h2.sendMessage(createMessage(1, config, h2));
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKillAll(ProcessConfig config, boolean isKillSystemProc) {
        if (this.mPMS == null) {
            return;
        }
        int policy = config.getPolicy();
        String reason = getKillReason(config.getPolicy());
        Map<Integer, String> fgTaskPackageMap = WindowProcessUtils.getPerceptibleRecentAppList(this.mAMS.mActivityTaskManager);
        List<String> whiteList = getProcessPolicyWhiteList(config, this.mProcessPolicy);
        if (fgTaskPackageMap != null && whiteList != null) {
            whiteList.addAll(fgTaskPackageMap.values());
        }
        removeTasksIfNeeded(config, this.mProcessPolicy, whiteList, fgTaskPackageMap);
        ArrayList<IAppState> appList = this.mSmartPowerService.getAllAppState();
        if (appList == null || appList.isEmpty()) {
            return;
        }
        Iterator<IAppState> it = appList.iterator();
        while (it.hasNext()) {
            IAppState appState = it.next();
            if (!whiteList.contains(appState.getPackageName()) && (policy != 20 || (appState.getAdj() > 0 && !appState.isSystemApp()))) {
                List<IAppState.IRunningProcess> runningAppList = appState.getRunningProcessList();
                for (IAppState.IRunningProcess runningApp : runningAppList) {
                    ProcessRecord app = runningApp.getProcessRecord();
                    if (runningApp.getAdj() > 0 && !isCurrentProcessInBackup(runningApp)) {
                        killOnce(app, policy, reason, true, this.mPMS, TAG, this.mHandler, this.mContext);
                    }
                }
            }
        }
    }

    private boolean handleKillApp(ProcessConfig config) {
        if (config.isUidInvalid()) {
            Slog.w(TAG, "uid:" + config.getUserId() + " is invalid");
            return false;
        }
        String packageName = config.getKillingPackage();
        int uid = config.getUid();
        int policy = config.getPolicy();
        if (TextUtils.isEmpty(packageName) || !UserHandle.isApp(uid)) {
            return false;
        }
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses(uid, packageName);
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningProcess = it.next();
            ProcessRecord app = runningProcess.getProcessRecord();
            if (!isCurrentProcessInBackup(runningProcess)) {
                killOnce(app, policy, getKillReason(policy), true, this.mPMS, TAG, this.mHandler, this.mContext);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleThermalKillProc(ProcessConfig config) {
        ArrayList<IAppState> appList = this.mSmartPowerService.getAllAppState();
        if (appList == null || appList.isEmpty()) {
            return;
        }
        Iterator<IAppState> it = appList.iterator();
        while (it.hasNext()) {
            IAppState appState = it.next();
            if (!appState.isSystemApp() && appState.getAdj() > THERMAL_KILL_ALL_PROCESS_MINADJ) {
                List<IAppState.IRunningProcess> runningAppList = appState.getRunningProcessList();
                for (IAppState.IRunningProcess runningApp : runningAppList) {
                    if (!isCurrentProcessInBackup(runningApp)) {
                        killOnce(runningApp.getProcessRecord(), config.getPolicy(), getKillReason(config.getPolicy()), true, this.mPMS, TAG, this.mHandler, this.mContext);
                    }
                }
            }
        }
    }

    public void handleAutoLockOff() {
        Map<Integer, String> fgTaskPackageMap = WindowProcessUtils.getPerceptibleRecentAppList(this.mAMS.mActivityTaskManager);
        cleanAllSubProcess();
        List<String> whiteList = new ArrayList<>();
        whiteList.add(DESK_CLOCK_PROCESS_NAME);
        if (fgTaskPackageMap != null) {
            whiteList.addAll(fgTaskPackageMap.values());
        }
        killActiveProcess(this.mLockOffCleanTestEnable ? this.mLockOffCleanTestThreshold : this.mActiveProcessThreshold, 22, whiteList);
        if (this.mLockOffCleanTestEnable) {
            this.mLockOffCleanTestEnable = false;
            return;
        }
        this.mHandler.removeMessages(4);
        Message message = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessageDelayed(message, this.mCheckCPUTime);
        this.mActiveProcessThreshold = Math.max(this.mActiveProcessThreshold * 0.75f, ACTIVE_PROCESS_MIX_THRESHOLD);
        this.mCheckCPUTime = Math.min(this.mCheckCPUTime * 2, CHECK_CPU_MAX_TIME_MS);
    }

    private void killActiveProcess(float threshold, int policy, List<String> whiteList) {
        Iterator<IAppState> it;
        int i;
        ProcessPowerCleaner processPowerCleaner = this;
        boolean z = false;
        long uptimeSince = processPowerCleaner.mSmartPowerService.updateCpuStatsNow(false);
        if (uptimeSince <= 0) {
            return;
        }
        ArrayList<IAppState> appStates = processPowerCleaner.mSmartPowerService.getAllAppState();
        Iterator<IAppState> it2 = appStates.iterator();
        while (it2.hasNext()) {
            IAppState appInfo = it2.next();
            ArrayList<IAppState.IRunningProcess> procs = appInfo.getRunningProcessList();
            int adj = appInfo.getAdj();
            int i2 = THERMAL_KILL_ALL_PROCESS_MINADJ;
            boolean isForceStop = adj > THERMAL_KILL_ALL_PROCESS_MINADJ ? true : z;
            Iterator<IAppState.IRunningProcess> it3 = procs.iterator();
            while (it3.hasNext()) {
                IAppState.IRunningProcess procInfo = it3.next();
                long curCpuTime = procInfo.getCurCpuTime();
                long lastCpuTime = procInfo.getLastCpuTime();
                procInfo.setLastCpuTime(curCpuTime);
                synchronized (processPowerCleaner.mAMS) {
                    try {
                        if (processPowerCleaner.isInWhiteListLock(procInfo.getProcessRecord(), procInfo.getUid(), policy, processPowerCleaner.mPMS)) {
                            try {
                            } catch (Throwable th) {
                                th = th;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                if (processPowerCleaner.isAudioOrGPSApp(procInfo.getUid())) {
                    processPowerCleaner = this;
                    it2 = it2;
                } else if (whiteList.contains(procInfo.getPackageName())) {
                    processPowerCleaner = this;
                    it2 = it2;
                } else if (procInfo.getAdj() < i2) {
                    processPowerCleaner = this;
                    it2 = it2;
                } else if (processPowerCleaner.isCurrentProcessInBackup(procInfo)) {
                    processPowerCleaner = this;
                    it2 = it2;
                } else if (!procInfo.hasForegrundService()) {
                    long cpuUsedTime = (curCpuTime - lastCpuTime) / processPowerCleaner.mSmartPowerService.getBackgroundCpuCoreNum();
                    if ((((float) cpuUsedTime) * 100.0f) / ((float) uptimeSince) < threshold) {
                        it = it2;
                        i = THERMAL_KILL_ALL_PROCESS_MINADJ;
                    } else {
                        String reason = getKillReason(policy) + " over " + TimeUtils.formatDuration(uptimeSince) + " used " + TimeUtils.formatDuration(cpuUsedTime) + " (" + ((((float) cpuUsedTime) * 100.0f) / ((float) uptimeSince)) + "%) threshold " + threshold;
                        ProcessRecord processRecord = procInfo.getProcessRecord();
                        ProcessManagerService processManagerService = this.mPMS;
                        it = it2;
                        H h = this.mHandler;
                        Context context = this.mContext;
                        i = THERMAL_KILL_ALL_PROCESS_MINADJ;
                        killOnce(processRecord, policy, reason, isForceStop, processManagerService, TAG, h, context);
                    }
                    processPowerCleaner = this;
                    i2 = i;
                    it2 = it;
                }
            }
            z = false;
            processPowerCleaner = this;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOffEvent() {
        this.mSmartPowerService.updateCpuStatsNow(true);
        cleanAllSubProcess();
        powerFrozenAll();
        Message message = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessageDelayed(message, 1500000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void powerFrozenAll() {
        if (SmartPowerSettings.PROP_FROZEN_ENABLE && this.mSmartPowerService != null) {
            this.mSmartPowerService.hibernateAllIfNeeded(SCREEN_OFF_FROZEN_REASON);
            Slog.d(TAG, "Frozen: lock off frozen");
            Message message = this.mHandler.obtainMessage(5);
            this.mHandler.sendMessageDelayed(message, 180000L);
        }
    }

    Message createMessage(int event, ProcessConfig config, Handler handler) {
        Message msg = handler.obtainMessage(event);
        msg.obj = config;
        return msg;
    }

    public void setLockOffCleanTestEnable(boolean lockOffCleanTestEnable) {
        this.mLockOffCleanTestEnable = lockOffCleanTestEnable;
    }

    private void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.ProcessPowerCleaner.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.System.getUriFor("perf_proc_power"))) {
                    ProcessPowerCleaner processPowerCleaner = ProcessPowerCleaner.this;
                    processPowerCleaner.updateCloudControlParas(processPowerCleaner.mContext);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("perf_proc_power"), false, observer, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudControlParas(Context context) {
    }

    private void cleanAllSubProcess() {
        SystemPressureController systemPressureController = this.mSysPressureCtrl;
        if (systemPressureController != null) {
            systemPressureController.cleanAllSubProcess();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetLockOffConfig() {
        this.mHandler.removeMessages(16);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        this.mActiveProcessThreshold = 2.0f;
        this.mCheckCPUTime = 600000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ScreenStatusReceiver extends BroadcastReceiver {
        private String SCREEN_OFF;
        private String SCREEN_ON;

        private ScreenStatusReceiver() {
            this.SCREEN_ON = "android.intent.action.SCREEN_ON";
            this.SCREEN_OFF = "android.intent.action.SCREEN_OFF";
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (this.SCREEN_ON.equals(intent.getAction())) {
                ProcessPowerCleaner.this.mIsScreenOffState = false;
                Message msg = ProcessPowerCleaner.this.mHandler.obtainMessage(15);
                ProcessPowerCleaner.this.mHandler.sendMessage(msg);
            } else if (this.SCREEN_OFF.equals(intent.getAction()) && !ProcessPowerCleaner.this.mHandler.hasMessages(16)) {
                ProcessPowerCleaner.this.mIsScreenOffState = true;
                Message msg2 = ProcessPowerCleaner.this.mHandler.obtainMessage(16);
                ProcessPowerCleaner.this.mHandler.sendMessageDelayed(msg2, 300000L);
            }
        }
    }
}
