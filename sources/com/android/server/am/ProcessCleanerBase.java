package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowProcessUtils;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.server.greeze.AurogonImmobulusMode;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.process.ProcessConfig;

/* loaded from: classes.dex */
public abstract class ProcessCleanerBase {
    private static final String DESK_CLOCK_PROCESS_NAME = "com.android.deskclock";
    private static final String HOME_PROCESS_NAME = "com.miui.home";
    private static final String RADIO_PROCESS_NAME = "com.miui.fmservice:remote";
    private static final String RADIO_TURN_OFF_INTENT = "miui.intent.action.TURN_OFF";
    public static final int SMART_POWER_PROTECT_APP_FLAGS = 472;
    private static final String TAG = "ProcessCleanerBase";
    protected ActivityManagerService mAMS;
    protected SmartPowerServiceInternal mSmartPowerService;

    public ProcessCleanerBase(ActivityManagerService ams) {
        this.mAMS = ams;
    }

    public void systemReady(Context context, ProcessManagerService pms) {
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
    }

    public static boolean isSystemApp(ProcessRecord app) {
        if (app == null || app.info == null) {
            return false;
        }
        return (app.info.flags & 129) != 0 || 1000 == app.uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0077  */
    /* JADX WARN: Removed duplicated region for block: B:52:0x007f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void killOnce(com.android.server.am.ProcessRecord r15, int r16, java.lang.String r17, boolean r18, com.android.server.am.ProcessManagerService r19, java.lang.String r20, android.os.Handler r21, android.content.Context r22) {
        /*
            Method dump skipped, instructions count: 193
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ProcessCleanerBase.killOnce(com.android.server.am.ProcessRecord, int, java.lang.String, boolean, com.android.server.am.ProcessManagerService, java.lang.String, android.os.Handler, android.content.Context):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killOnce(ProcessRecord app, String reason, int killLevel, Handler handler, Context context) {
        try {
            if (checkProcessDied(app) || checkKillFMApp(app, handler, context)) {
                return;
            }
            if (killLevel == 101) {
                trimMemory(app);
                return;
            }
            if (killLevel == 102) {
                killBackgroundApplication(app, reason);
                return;
            }
            if (killLevel == 103) {
                synchronized (this.mAMS) {
                    killApplicationLock(app, reason);
                }
            } else if (killLevel == 104) {
                forceStopPackage(app.info.packageName, app.userId, reason);
            }
        } catch (Exception e) {
            Slog.d(TAG, "killOnce:reason " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean checkProcessDied(ProcessRecord proc) {
        boolean z;
        synchronized (this.mAMS) {
            if (proc != null) {
                try {
                    z = proc.getThread() == null || proc.isKilled();
                } finally {
                }
            }
        }
        return z;
    }

    private void printKillLog(ProcessRecord app, int killLevel, String reason, String killTag) {
        StringBuilder append;
        String str;
        StringBuilder append2 = new StringBuilder().append(killLevelToString(killLevel));
        if (killLevel == 104) {
            append = new StringBuilder().append(" pkgName=");
            str = app.info.packageName;
        } else {
            append = new StringBuilder().append(" procName=");
            str = app.processName;
        }
        String levelString = append2.append(append.append(str).toString()).toString();
        String info = String.format("AS:%d%d", Integer.valueOf(app.mState.getCurAdj()), Integer.valueOf(app.mState.getCurProcState()));
        Slog.i(killTag, reason + ": " + levelString + " info=" + info);
    }

    boolean checkKillFMApp(final ProcessRecord proc, Handler handler, final Context context) {
        if (handler != null && proc.processName.equals(RADIO_PROCESS_NAME)) {
            final Intent intent = new Intent(RADIO_TURN_OFF_INTENT);
            intent.addFlags(268435456);
            handler.post(new Runnable() { // from class: com.android.server.am.ProcessCleanerBase.1
                @Override // java.lang.Runnable
                public void run() {
                    context.sendBroadcastAsUser(intent, new UserHandle(proc.userId));
                }
            });
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllTasks(int userId, ProcessManagerService pms) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeAllTasks(pms.getInternal(), userId, this.mAMS.mActivityTaskManager);
            if (userId == 0) {
                WindowProcessUtils.removeAllTasks(pms.getInternal(), 999, this.mAMS.mActivityTaskManager);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void removeTasksIfNeeded(List<Integer> taskIdList, Set<Integer> whiteTaskSet, List<String> whiteList, List<Integer> whiteListTaskId, ProcessPolicy procPolicy) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTasks(taskIdList, whiteTaskSet, procPolicy, this.mAMS.mActivityTaskManager, whiteList, whiteListTaskId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTaskIfNeeded(int taskId) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTask(taskId, this.mAMS.mActivityTaskManager);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTasksInPackages(List<String> packages, int userId, ProcessPolicy procPolicy) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTasksInPackages(packages, userId, procPolicy, this.mAMS.mActivityTaskManager);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentProcessInBackup(IAppState.IRunningProcess runningProc) {
        return isCurrentProcessInBackup(runningProc.getPackageName(), runningProc.getProcessName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentProcessInBackup(String packageName, String processName) {
        if (this.mSmartPowerService.isProcessWhiteList(64, packageName, processName)) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTrimMemoryEnable(String packageName, ProcessManagerService pms) {
        return pms.isTrimMemoryEnable(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInWhiteListLock(ProcessRecord app, int userId, int policy, ProcessManagerService pms) {
        return pms.isInWhiteList(app, userId, policy);
    }

    boolean isForceStopEnable(ProcessRecord app, int policy, ProcessManagerService pms) {
        return pms.isForceStopEnable(app, policy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAudioOrGPSApp(int uid) {
        return SystemPressureController.getInstance().isAudioOrGPSApp(uid);
    }

    boolean isAudioOrGPSProc(int uid, int pid) {
        return SystemPressureController.getInstance().isAudioOrGPSProc(uid, pid);
    }

    void trimMemory(ProcessRecord app) {
        if (app.getWindowProcessController().isInterestingToUser()) {
            return;
        }
        if (app.info.packageName.equals("android")) {
            scheduleTrimMemory(app, 60);
        } else {
            scheduleTrimMemory(app, 80);
        }
    }

    void scheduleTrimMemory(ProcessRecord app, int level) {
        synchronized (this.mAMS) {
            if (!checkProcessDied(app)) {
                try {
                    app.getThread().scheduleTrimMemory(level);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceStopPackage(String packageName, int userId, String reason) {
        Trace.traceBegin(64L, "forceStopPackage pck:" + packageName + " r:" + reason);
        ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).forceStopPackage(packageName, userId, reason);
        Trace.traceEnd(64L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killApplicationLock(ProcessRecord app, String reason) {
        Trace.traceBegin(64L, "killLocked pid:" + app.getPid() + " r:" + reason);
        app.killLocked(reason, 13, true);
        Trace.traceEnd(64L);
    }

    void killBackgroundApplication(ProcessRecord app, String reason) {
        Trace.traceBegin(64L, "killBackgroundProcesses pid:" + app.getPid() + " r:" + reason);
        this.mAMS.killBackgroundProcesses(app.info.packageName, app.userId, reason);
        Trace.traceEnd(64L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getProcessPolicyWhiteList(ProcessConfig config, ProcessPolicy mProcessPolicy) {
        int policy = config.getPolicy();
        List<String> whiteList = config.getWhiteList();
        if (whiteList == null) {
            whiteList = new ArrayList();
        }
        if (policy == 14 || policy == 16 || policy == 15) {
            whiteList.add(DESK_CLOCK_PROCESS_NAME);
        } else if (policy == 1) {
            whiteList.add("com.miui.home");
        }
        printWhiteList(config, whiteList);
        return whiteList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTasksIfNeeded(ProcessConfig config, ProcessPolicy mProcessPolicy, List<String> policyWhiteList, Map<Integer, String> fgTaskPackageMap) {
        List<Integer> whiteListTaskId = config.getWhiteListTaskId();
        if (config.isRemoveTaskNeeded() && config.getRemovingTaskIdList() != null) {
            Set<Integer> fgTaskIdSet = fgTaskPackageMap != null ? fgTaskPackageMap.keySet() : null;
            if (config.getPolicy() == 1) {
                List<Integer> taskList = WindowProcessUtils.getAllTaskIdList(this.mAMS.mActivityTaskManager);
                removeTasksIfNeeded(taskList, fgTaskIdSet, policyWhiteList, whiteListTaskId, mProcessPolicy);
            } else {
                removeTasksIfNeeded(config.getRemovingTaskIdList(), fgTaskIdSet, policyWhiteList, whiteListTaskId, mProcessPolicy);
            }
        }
    }

    void printWhiteList(ProcessConfig config, List<String> whiteList) {
        if (!PressureStateSettings.DEBUG_ALL) {
            return;
        }
        String info = "reason=" + getKillReason(config.getPolicy()) + " whiteList=";
        for (int i = 0; i < whiteList.size(); i++) {
            info = info + whiteList.get(i) + " ";
        }
        Slog.d(TAG, info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getKillReason(int policy) {
        switch (policy) {
            case 1:
                return IProcessPolicy.REASON_ONE_KEY_CLEAN;
            case 2:
                return IProcessPolicy.REASON_FORCE_CLEAN;
            case 3:
                return IProcessPolicy.REASON_LOCK_SCREEN_CLEAN;
            case 4:
                return IProcessPolicy.REASON_GAME_CLEAN;
            case 5:
                return IProcessPolicy.REASON_OPTIMIZATION_CLEAN;
            case 6:
                return IProcessPolicy.REASON_GARBAGE_CLEAN;
            case 7:
                return IProcessPolicy.REASON_SWIPE_UP_CLEAN;
            case 8:
            case 9:
            case 15:
            case 17:
            case 18:
            case 21:
            default:
                return IProcessPolicy.REASON_UNKNOWN;
            case 10:
                return IProcessPolicy.REASON_USER_DEFINED;
            case 11:
                return IProcessPolicy.REASON_AUTO_POWER_KILL;
            case 12:
                return IProcessPolicy.REASON_AUTO_THERMAL_KILL;
            case 13:
                return IProcessPolicy.REASON_AUTO_IDLE_KILL;
            case 14:
                return IProcessPolicy.REASON_AUTO_SLEEP_CLEAN;
            case 16:
                return IProcessPolicy.REASON_AUTO_SYSTEM_ABNORMAL_CLEAN;
            case 19:
                return IProcessPolicy.REASON_AUTO_THERMAL_KILL_ALL_LEVEL_1;
            case 20:
                return IProcessPolicy.REASON_AUTO_THERMAL_KILL_ALL_LEVEL_2;
            case 22:
                return IProcessPolicy.REASON_SCREEN_OFF_CPU_CHECK_KILL;
        }
    }

    String killLevelToString(int level) {
        switch (level) {
            case 100:
                return "none";
            case 101:
                return "trim-memory";
            case 102:
                return "kill-background";
            case 103:
                return "kill";
            case AurogonImmobulusMode.MSG_REMOVE_ALL_MESSAGE /* 104 */:
                return "force-stop";
            default:
                return "";
        }
    }
}
