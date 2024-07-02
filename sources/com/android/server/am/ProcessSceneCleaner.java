package com.android.server.am;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.wm.MiuiSoScManagerStub;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessUtils;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.process.ProcessConfig;

/* loaded from: classes.dex */
public class ProcessSceneCleaner extends ProcessCleanerBase {
    private static final String TAG = "ProcessSceneCleaner";
    private ActivityManagerService mAMS;
    private Context mContext;
    private H mHandler;
    private ProcessManagerService mPMS;
    private ProcessPolicy mProcessPolicy;
    private SystemPressureController mSysPressureCtrl;

    public ProcessSceneCleaner(ActivityManagerService ams) {
        super(ams);
        this.mAMS = ams;
    }

    public void systemReady(ProcessManagerService pms, Looper myLooper, Context context) {
        super.systemReady(context, pms);
        this.mContext = context;
        this.mPMS = pms;
        this.mProcessPolicy = pms.getProcessPolicy();
        this.mSysPressureCtrl = SystemPressureController.getInstance();
        this.mHandler = new H(myLooper);
    }

    public boolean sceneKillProcess(ProcessConfig config) {
        if (this.mHandler == null || config == null) {
            return false;
        }
        switch (config.getPolicy()) {
            case 1:
            case 2:
            case 4:
            case 5:
                H h = this.mHandler;
                h.sendMessage(createMessage(1, config, h));
                return true;
            case 3:
            case 6:
            case 10:
                if (config.getKillingPackageMaps() == null) {
                    return false;
                }
                H h2 = this.mHandler;
                h2.sendMessage(createMessage(3, config, h2));
                return true;
            case 7:
                if (TextUtils.isEmpty(config.getKillingPackage())) {
                    return false;
                }
                H h3 = this.mHandler;
                h3.sendMessage(createMessage(2, config, h3));
                return true;
            case 8:
            case 9:
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final int KILL_ALL_EVENT = 1;
        public static final int KILL_ANY_EVENT = 3;
        public static final int SWIPE_KILL_EVENT = 2;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ProcessConfig config = (ProcessConfig) msg.obj;
            switch (msg.what) {
                case 1:
                    Trace.traceBegin(524288L, "SceneKillAll:" + ProcessSceneCleaner.this.getKillReason(config.getPolicy()));
                    ProcessSceneCleaner.this.handleKillAll(config);
                    Trace.traceEnd(524288L);
                    return;
                case 2:
                    Trace.traceBegin(524288L, "SceneSwipeKill" + ProcessSceneCleaner.this.getKillReason(config.getPolicy()));
                    ProcessSceneCleaner.this.handleSwipeKill(config);
                    Trace.traceEnd(524288L);
                    return;
                case 3:
                    Trace.traceBegin(524288L, "SceneKillAny" + ProcessSceneCleaner.this.getKillReason(config.getPolicy()));
                    ProcessSceneCleaner.this.handleKillAny(config);
                    Trace.traceEnd(524288L);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKillAll(ProcessConfig config) {
        boolean isForceStop;
        boolean isForceStop2;
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
        if (policy == 2) {
            removeAllTasks(UserHandle.getCallingUserId(), this.mPMS);
        } else {
            removeTasksIfNeeded(config, this.mProcessPolicy, whiteList, fgTaskPackageMap);
        }
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses();
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningProcess = it.next();
            ProcessRecord app = runningProcess.getProcessRecord();
            if (config.getPolicy() == 1) {
                if (!MiuiSoScManagerStub.get().isInSoScSingleMode(runningProcess.getPackageName())) {
                    if (this.mProcessPolicy.getOneKeyCleanWhiteList().containsKey(runningProcess.getPackageName())) {
                        if (!this.mProcessPolicy.getOneKeyCleanWhiteList().get(runningProcess.getPackageName()).contains(runningProcess.getProcessName())) {
                            isForceStop = false;
                            isForceStop2 = isCurrentProcessInBackup(runningProcess);
                            if (!isForceStop2 && !whiteList.contains(runningProcess.getPackageName())) {
                                killOnce(app, policy, reason, isForceStop, this.mPMS, TAG, this.mHandler, this.mContext);
                            }
                        }
                    }
                }
            }
            isForceStop = true;
            isForceStop2 = isCurrentProcessInBackup(runningProcess);
            if (!isForceStop2) {
                killOnce(app, policy, reason, isForceStop, this.mPMS, TAG, this.mHandler, this.mContext);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleSwipeKill(ProcessConfig config) {
        if (!config.isUserIdInvalid() && !config.isTaskIdInvalid()) {
            String packageName = config.getKillingPackage();
            int taskId = config.getTaskId();
            String killReason = getKillReason(config.getPolicy());
            if (config.isRemoveTaskNeeded()) {
                removeTaskIfNeeded(config.getTaskId());
            }
            List<ProcessRecord> infoList = this.mPMS.getProcessRecordList(packageName, config.getUserId());
            if (infoList != null && !infoList.isEmpty()) {
                if (isAppHasOtherTask(infoList, taskId)) {
                    return killAppForHasOtherTask(taskId, config);
                }
                for (ProcessRecord proc : infoList) {
                    if (!Build.IS_INTERNATIONAL_BUILD || !proc.mServices.hasForegroundServices()) {
                        if (!isCurrentProcessInBackup(proc.info.packageName, proc.processName)) {
                            killOnce(proc, config.getPolicy(), killReason, true, this.mPMS, TAG, this.mHandler, this.mContext);
                        }
                    }
                }
                return true;
            }
            return false;
        }
        Slog.w(TAG, "userId:" + config.getUserId() + " or taskId:" + config.getTaskId() + " is invalid");
        return false;
    }

    private boolean isAppHasOtherTask(List<ProcessRecord> infoList, int taskId) {
        boolean appHasOtherTask = false;
        for (ProcessRecord proc : infoList) {
            if (WindowProcessUtils.isProcessHasActivityInOtherTaskLocked(proc.getWindowProcessController(), taskId)) {
                appHasOtherTask = true;
            }
        }
        return appHasOtherTask;
    }

    private boolean killAppForHasOtherTask(int taskId, ProcessConfig config) {
        ProcessRecord taskTopApp = null;
        WindowProcessController wpc = WindowProcessUtils.getTaskTopApp(taskId);
        if (wpc != null) {
            taskTopApp = (ProcessRecord) wpc.mOwner;
        }
        if (taskTopApp != null) {
            boolean processHasOtherTask = WindowProcessUtils.isProcessHasActivityInOtherTaskLocked(taskTopApp.getWindowProcessController(), taskId);
            if (!processHasOtherTask) {
                if ((!Build.IS_INTERNATIONAL_BUILD || !taskTopApp.mServices.hasForegroundServices()) && !isCurrentProcessInBackup(taskTopApp.info.packageName, taskTopApp.processName)) {
                    killOnce(taskTopApp, config.getPolicy(), getKillReason(config.getPolicy()), false, this.mPMS, TAG, this.mHandler, this.mContext);
                    return true;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKillAny(ProcessConfig config) {
        if (this.mPMS == null) {
            return;
        }
        if (config.isUserIdInvalid()) {
            Slog.w(TAG, "userId:" + config.getUserId() + " is invalid");
            return;
        }
        int policy = config.getPolicy();
        ArrayMap<Integer, List<String>> killingPackageMaps = config.getKillingPackageMaps();
        removeTasksByPackages(killingPackageMaps, config);
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses();
        Map<String, Integer> killPackages = getKillPackageList(killingPackageMaps);
        Set<String> killPkg = killPackages.keySet();
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningApp = it.next();
            ProcessRecord app = runningApp.getProcessRecord();
            String pkgName = runningApp.getPackageName();
            if (killPkg.contains(pkgName) && runningApp.getAdj() > 0 && !isCurrentProcessInBackup(runningApp)) {
                int killLevel = killPackages.get(pkgName).intValue();
                if (killLevel != 100) {
                    killOnce(app, getKillReason(policy), killLevel, this.mHandler, this.mContext);
                } else {
                    killOnce(app, config.getPolicy(), getKillReason(policy), true, this.mPMS, TAG, this.mHandler, this.mContext);
                }
            }
        }
    }

    private Map<String, Integer> getKillPackageList(ArrayMap<Integer, List<String>> killingPackageMaps) {
        Map<String, Integer> killPackage = new HashMap<>();
        for (int i = 0; i < killingPackageMaps.size(); i++) {
            int killLevel = killingPackageMaps.keyAt(i).intValue();
            List<String> killingPackages = killingPackageMaps.valueAt(i);
            if (killingPackages != null && killingPackages.size() > 0) {
                for (String pkg : killingPackages) {
                    killPackage.put(pkg, Integer.valueOf(killLevel));
                }
            }
        }
        return killPackage;
    }

    private void removeTasksByPackages(ArrayMap<Integer, List<String>> packageMaps, ProcessConfig config) {
        List<String> killedPackages;
        if (config.isRemoveTaskNeeded()) {
            List<String> removedTasksInPackages = new ArrayList<>();
            for (int i = 0; i < packageMaps.size(); i++) {
                int killLevel = packageMaps.keyAt(i).intValue();
                if (killLevel != 101 && (killedPackages = packageMaps.get(Integer.valueOf(killLevel))) != null && !killedPackages.isEmpty()) {
                    removedTasksInPackages.addAll(killedPackages);
                }
            }
            Iterator pkgIterator = removedTasksInPackages.iterator();
            while (pkgIterator.hasNext()) {
                String pkg = pkgIterator.next();
                if (!TextUtils.isEmpty(pkg) && !isTrimMemoryEnable(pkg, this.mPMS)) {
                    pkgIterator.remove();
                }
            }
            removeTasksInPackages(removedTasksInPackages, config.getUserId(), this.mProcessPolicy);
        }
    }

    Message createMessage(int event, ProcessConfig config, Handler handler) {
        Message msg = handler.obtainMessage(event);
        msg.obj = config;
        return msg;
    }
}
