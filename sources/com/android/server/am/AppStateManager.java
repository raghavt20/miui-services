package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.miui.AppOpsUtils;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.WindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowProcessController;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.smartpower.AppPowerResource;
import com.miui.server.smartpower.AppPowerResourceManager;
import com.miui.server.smartpower.IAppState;
import com.miui.server.smartpower.PowerFrozenManager;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.miui.server.smartpower.SmartScenarioManager;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import database.SlaDbSchema.SlaDbSchema;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public class AppStateManager {
    private static final int APP_STATE_BACKGROUND = 2;
    private static final int APP_STATE_DIED = 0;
    private static final int APP_STATE_FOREGROUND = 1;
    private static final int APP_STATE_HIBERNATION = 6;
    private static final int APP_STATE_IDLE = 5;
    private static final int APP_STATE_INACTIVE = 3;
    private static final int APP_STATE_MAINTENANCE = 4;
    private static final int APP_STATE_NONE = -1;
    public static final boolean DEBUG;
    public static final boolean DEBUG_PROC;
    private static final int HISTORY_SIZE;
    private static final int MSG_CLEAR_APPSTATES_BY_USERID = 3;
    private static final int MSG_HIBERNATE_ALL = 1;
    private static final int MSG_HIBERNATE_ALL_INACTIVE = 2;
    private static final int PROCESS_STATE_BACKGROUND = 3;
    private static final int PROCESS_STATE_DIED = 0;
    private static final int PROCESS_STATE_HIBERNATION = 7;
    private static final int PROCESS_STATE_IDLE = 6;
    private static final int PROCESS_STATE_INACTIVE = 4;
    private static final int PROCESS_STATE_INVISIBLE = 2;
    private static final int PROCESS_STATE_MAINTENANCE = 5;
    private static final int PROCESS_STATE_NONE = -1;
    private static final int PROCESS_STATE_VISIBLE = 1;
    public static final String REASON_ADD_TO_WHITELIST = "add to whitelist";
    public static final String REASON_ADJ_ABOVE_FG = "adj above foreground";
    public static final String REASON_ADJ_BELOW_VISIBLE = "adj below visible";
    public static final String REASON_ALARM_END = "alarm end";
    public static final String REASON_ALARM_START = "alarm start";
    public static final String REASON_APP_START = "app start";
    public static final String REASON_BACKUP_END = "backup end";
    public static final String REASON_BACKUP_SERVICE_CONNECTED = "backup service connected";
    public static final String REASON_BACKUP_SERVICE_DISCONNECTED = "backup service disconnected";
    public static final String REASON_BACKUP_START = "backup start";
    public static final String REASON_BECOME_BACKGROUND = "become background";
    public static final String REASON_BECOME_FOREGROUND = "become foreground";
    public static final String REASON_BECOME_INVISIBLE = "become invisible";
    public static final String REASON_BECOME_VISIBLE = "become visible";
    public static final String REASON_BROADCAST_END = "broadcast end ";
    public static final String REASON_BROADCAST_START = "broadcast start ";
    public static final String REASON_CONTENT_PROVIDER_END = "content provider end ";
    public static final String REASON_CONTENT_PROVIDER_START = "content provider start ";
    public static final String REASON_HIBERNATE_ALL = "hibernate all ";
    public static final String REASON_PENDING_INTENT = "pending intent ";
    public static final String REASON_PERIODIC_ACTIVE = "periodic active";
    public static final String REASON_PRESS_MEDIA_KEY = "press media key";
    public static final String REASON_PROCESS_DIED = "process died ";
    public static final String REASON_PROCESS_START = "process start ";
    public static final String REASON_RECEIVE_BINDER_STATE = "receive binder state";
    public static final String REASON_RECEIVE_BINDER_TRANS = "receive binder trans";
    public static final String REASON_RECEIVE_KILL_SIGNAL = "receive kill signal";
    public static final String REASON_RECEIVE_NET_REQUEST = "receive net request";
    public static final String REASON_RECEIVE_OTHER_THAW_REQUEST = "receive other thaw request";
    public static final String REASON_REMOVE_FROM_WHITELIST = "remove from whitelist";
    public static final String REASON_RESOURCE_ACTIVE = "resource active ";
    public static final String REASON_RESOURCE_INACTIVE = "resource inactive ";
    public static final String REASON_SERVICE_CEANGE = "service ";
    public static final String REASON_SHOW_INPUTMETHOD = "show input method";
    public static final String REASON_START_ACTIVITY = "start activity ";
    public static final String TAG = "SmartPower";
    private static final ArraySet<Integer> mStatesNeedToRecord;
    public static boolean sEnable;
    private ActivityManagerService mAMS;
    private AppPowerResourceManager mAppPowerResourceManager;
    private Context mContext;
    private final BroadcastReceiver mIntentReceiver;
    private Looper mLooper;
    private MainHandler mMainHandler;
    private PackageManager mPkms;
    private ProcessManagerInternal mPmInternal;
    private PowerFrozenManager mPowerFrozenManager;
    private IProcessPolicy mProcessPolicy;
    private SmartPowerPolicyManager mSmartPowerPolicyManager;
    private SmartScenarioManager mSmartScenarioManager;
    private final ArraySet<String> mWhiteListVideoActivities;
    private final ActiveApps mActiveApps = new ActiveApps();
    private final Object mAppLock = new Object();
    private final ArrayList<IProcessStateCallback> mProcessStateCallbacks = new ArrayList<>();
    private final SparseArray<Long> mPendingInteractiveUids = new SparseArray<>();
    private final CurrentTaskStack mCurrentTaskStack = new CurrentTaskStack();
    private int mHoldScreenUid = -1;

    /* loaded from: classes.dex */
    public interface IProcessStateCallback {
        void onAppStateChanged(AppState appState, int i, int i2, boolean z, boolean z2, String str);

        void onProcessStateChanged(AppState.RunningProcess runningProcess, int i, int i2, String str);
    }

    static {
        boolean z = SmartPowerSettings.DEBUG_ALL;
        DEBUG = z;
        sEnable = SmartPowerSettings.APP_STATE_ENABLE;
        DEBUG_PROC = z && SmartPowerSettings.DEBUG_PROC;
        HISTORY_SIZE = SmartPowerSettings.MAX_HISTORY_REPORT_SIZE;
        mStatesNeedToRecord = new ArraySet<>();
    }

    public static String processStateToString(int state) {
        switch (state) {
            case 0:
                return "died";
            case 1:
                return "visible";
            case 2:
                return "invisible";
            case 3:
                return "background";
            case 4:
                return "inactive";
            case 5:
                return "maintenance";
            case 6:
                return "idle";
            case 7:
                return "hibernation";
            default:
                return Integer.toString(state);
        }
    }

    public static String appStateToString(int state) {
        switch (state) {
            case 0:
                return "died";
            case 1:
                return "foreground";
            case 2:
                return "background";
            case 3:
                return "inactive";
            case 4:
                return "maintenance";
            case 5:
                return "idle";
            case 6:
                return "hibernation";
            default:
                return Integer.toString(state);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppStateManager(Context context, Looper looper, ActivityManagerService ams, PowerFrozenManager powerFrozenManager) {
        ArraySet<String> arraySet = new ArraySet<>();
        this.mWhiteListVideoActivities = arraySet;
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.am.AppStateManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int userId;
                String action = intent.getAction();
                if (action.equals("android.intent.action.USER_REMOVED") && (userId = intent.getIntExtra("android.intent.extra.user_handle", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION)) != -10000) {
                    Message msg = AppStateManager.this.mMainHandler.obtainMessage(3, Integer.valueOf(userId));
                    AppStateManager.this.mMainHandler.sendMessage(msg);
                }
            }
        };
        this.mLooper = looper;
        this.mMainHandler = new MainHandler(this.mLooper);
        this.mContext = context;
        this.mAMS = ams;
        this.mPowerFrozenManager = powerFrozenManager;
        powerFrozenManager.registerFrozenCallback(new PowerFrozenCallback());
        ArraySet<Integer> arraySet2 = mStatesNeedToRecord;
        arraySet2.add(0);
        arraySet2.add(1);
        arraySet2.add(2);
        arraySet2.add(3);
        arraySet2.add(6);
        if (SmartPowerSettings.PROP_FROZEN_ENABLE || DEBUG) {
            arraySet2.add(7);
        }
        String[] activities = context.getResources().getStringArray(285409490);
        arraySet.addAll(Arrays.asList(activities));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(AppPowerResourceManager appPowerResourceManager, SmartPowerPolicyManager smartPowerPolicyManager, SmartScenarioManager smartScenarioManager) {
        this.mAppPowerResourceManager = appPowerResourceManager;
        this.mSmartPowerPolicyManager = smartPowerPolicyManager;
        this.mSmartScenarioManager = smartScenarioManager;
        this.mPmInternal = ProcessManagerInternal.getInstance();
        this.mPkms = this.mContext.getPackageManager();
        this.mProcessPolicy = this.mPmInternal.getProcessPolicy();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, null);
    }

    public boolean isUidIdle(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isIdle();
        }
        return false;
    }

    public boolean isUidInactive(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isInactive();
        }
        return false;
    }

    public boolean isUidVisible(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isVsible();
        }
        return false;
    }

    public boolean isProcessIdle(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isProcessIdle(pid);
        }
        return false;
    }

    public boolean isProcessPerceptible(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isProcessPerceptible(pid);
        }
        return false;
    }

    public boolean isProcessPerceptible(int uid, String processName) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isProcessPerceptible(processName);
        }
        return false;
    }

    public boolean isProcessInTaskStack(int uid, int pid) {
        return this.mCurrentTaskStack.isProcessInTaskStack(uid, pid);
    }

    public boolean isProcessInTaskStack(String procName) {
        return this.mCurrentTaskStack.isProcessInTaskStack(procName);
    }

    public boolean isProcessKilled(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isProcessKilled(pid);
        }
        return true;
    }

    public ArrayList<IAppState> getAllAppState() {
        ArrayList<IAppState> appList = new ArrayList<>();
        synchronized (this.mAppLock) {
            for (int i = 0; i < this.mActiveApps.size(); i++) {
                AppState app = this.mActiveApps.valueAt(i);
                appList.add(app);
            }
        }
        return appList;
    }

    public AppState getAppState(int uid) {
        AppState appState;
        synchronized (this.mAppLock) {
            appState = this.mActiveApps.get(uid);
        }
        return appState;
    }

    public List<AppState> getAppState(String packageName) {
        List<AppState> apps = new ArrayList<>();
        synchronized (this.mAppLock) {
            for (int i = 0; i < this.mActiveApps.size(); i++) {
                AppState app = this.mActiveApps.valueAt(i);
                if (app.getPackageName().equals(packageName)) {
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    public int getProcessState(int uid, String processName) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.getProcessState(processName);
        }
        return -1;
    }

    public AppState.RunningProcess getRunningProcess(int uid, String processName) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.getRunningProcess(processName);
        }
        return null;
    }

    public AppState.RunningProcess getRunningProcess(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.getRunningProcess(pid);
        }
        return null;
    }

    public ArrayList<IAppState> updateAllAppUsageStats() {
        ArrayList<IAppState> appList = new ArrayList<>();
        synchronized (this.mAppLock) {
            for (int i = 0; i < this.mActiveApps.size(); i++) {
                IAppState app = this.mActiveApps.valueAt(i);
                SmartPowerPolicyManager.UsageStatsInfo usageStats = this.mSmartPowerPolicyManager.getUsageStatsInfo(app.getPackageName());
                if (usageStats != null) {
                    app.setLaunchCount(usageStats.getAppLaunchCount());
                    app.setTotalTimeInForeground(usageStats.getTotalTimeInForeground());
                }
                appList.add(app);
            }
        }
        return appList;
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        synchronized (this.mAppLock) {
            try {
                if (opti + 1 < args.length) {
                    String parm = args[opti];
                    int dpUid = 0;
                    if (parm.contains(SlaDbSchema.SlaTable.Uidlist.UID) || parm.contains("-u")) {
                        String uidStr = args[opti + 1];
                        dpUid = Integer.parseInt(uidStr);
                    }
                    this.mActiveApps.dump(pw, args, opti, dpUid);
                } else {
                    this.mActiveApps.dump(pw, args, opti, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void dumpStack(PrintWriter pw, String[] args, int opti) {
        this.mCurrentTaskStack.dump(pw, args, opti);
    }

    public void processStartedLocked(ProcessRecord record, String packageName) {
        AppState appState;
        synchronized (this.mAppLock) {
            appState = this.mActiveApps.get(record.uid);
            if (appState == null) {
                appState = new AppState(record.uid, packageName, this.mContext);
                this.mActiveApps.put(record.uid, appState);
            }
        }
        appState.processStartedLocked(record);
    }

    public void processKilledLocked(ProcessRecord record) {
        AppState appState = getAppState(record.uid);
        if (appState != null) {
            appState.processKilledLocked(record);
            removeAppIfNeeded(appState);
        }
    }

    public void updateProcessLocked(ProcessRecord record) {
        AppState appState = getAppState(record.uid);
        if (appState != null) {
            appState.updateProcessLocked(record);
            removeAppIfNeeded(appState);
        }
    }

    public void setProcessPss(ProcessRecord record, long pss, long swapPss) {
        AppState.RunningProcess runProc = getRunningProcess(record.uid, record.processName);
        if (runProc != null) {
            runProc.setPss(pss, swapPss);
        }
    }

    private void removeAppIfNeeded(AppState appState) {
        if (Process.isIsolated(appState.getUid()) && !appState.isAlive()) {
            synchronized (this.mAppLock) {
                this.mActiveApps.remove(appState.getUid());
            }
        }
    }

    public void componentStartLocked(ProcessRecord processRecord, String content) {
        AppState appState = getAppState(processRecord.uid);
        if (appState != null) {
            appState.componentStart(processRecord, content);
        }
    }

    public void componentEndLocked(ProcessRecord processRecord, String content) {
        AppState appState = getAppState(processRecord.uid);
        if (appState != null) {
            appState.componentEnd(processRecord, content);
        }
    }

    public void componentStartLocked(int uid, String content) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.componentStart(content);
        }
    }

    public void componentEndLocked(int uid, String content) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.componentEnd(content);
        }
    }

    public void onSendPendingIntent(int uid, String typeName) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onSendPendingIntent(typeName);
            return;
        }
        synchronized (this.mPendingInteractiveUids) {
            this.mPendingInteractiveUids.append(uid, Long.valueOf(SystemClock.uptimeMillis()));
        }
    }

    public void serviceConnectionChangedLocked(ProcessRecord client, ProcessRecord service, boolean isConnect, long flags) {
        AppState appState = getAppState(client.uid);
        if (appState != null) {
            appState.updateServiceDepends(client, service, isConnect, flags);
        }
    }

    public void providerConnectionChangedLocked(ProcessRecord client, ProcessRecord host, boolean isConnect) {
        AppState appState;
        if (!this.mSmartPowerPolicyManager.isProcessHibernationWhiteList(host.uid, host.mPid, host.info.packageName, host.processName) && (appState = getAppState(client.uid)) != null) {
            appState.updateProviderDepends(client, host, isConnect);
        }
    }

    public void onAddToWhiteList(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onAddToWhiteList();
        }
    }

    public void onAddToWhiteList(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onAddToWhiteList(pid);
        }
    }

    public void onRemoveFromWhiteList(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onRemoveFromWhiteList();
        }
    }

    public void onRemoveFromWhiteList(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onRemoveFromWhiteList(pid);
        }
    }

    public void onMediaKey(int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onMediaKey(pid);
        }
    }

    public void onMediaKey(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onMediaKey();
        }
    }

    public void onInputMethodShow(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onInputMethodShow();
        }
    }

    public void onBackupChanged(boolean active, ProcessRecord app) {
        AppState appState = getAppState(app.uid);
        if (appState != null) {
            appState.onBackupChanged(active, app);
        }
    }

    public void onBackupServiceAppChanged(boolean active, int uid, int pid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onBackupServiceAppChanged(active, pid);
        }
    }

    public void onApplyOomAdjLocked(ProcessRecord app) {
        AppState appState = getAppState(app.uid);
        if (appState != null) {
            appState.onApplyOomAdjLocked(app);
        }
    }

    public void setActivityVisible(int uid, String processName, ActivityRecord record, boolean visible) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.setActivityVisible(processName, record, visible);
        }
    }

    public void setWindowVisible(int uid, int pid, WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.setWindowVisible(pid, win, attrs, visible);
        }
    }

    public void onHoldScreenUidChanged(int uid, boolean hold) {
        AppState appState;
        if (hold) {
            appState = getAppState(uid);
        } else {
            appState = getAppState(this.mHoldScreenUid);
        }
        this.mHoldScreenUid = uid;
        if (appState != null) {
            appState.updateCurrentResourceBehavier();
        }
    }

    public void activityStartBeforeLocked(String name, int fromPid, int toUid, String packageName, boolean isColdStart) {
        boolean startByHome = fromPid > 0 && getHomeProcessPidLocked() == fromPid;
        AppState appState = getAppState(toUid);
        if (appState == null && startByHome) {
            synchronized (this.mAppLock) {
                appState = new AppState(toUid, packageName, this.mContext);
                this.mActiveApps.put(toUid, appState);
            }
        }
        if (appState != null) {
            appState.onActivityForegroundLocked(name);
        }
        if (startByHome) {
            hibernateAllInactive(appState, REASON_APP_START);
        }
    }

    public int getHomeProcessPidLocked() {
        WindowProcessController homeProcess = this.mAMS.mAtmInternal.getHomeProcess();
        if (homeProcess != null) {
            return homeProcess.getPid();
        }
        return -1;
    }

    public boolean isHomeProcessTopLocked() {
        ProcessRecord proc;
        int homePid = getHomeProcessPidLocked();
        if (homePid > 0) {
            synchronized (this.mAMS.mPidsSelfLocked) {
                proc = this.mAMS.mPidsSelfLocked.get(homePid);
            }
            return proc != null && proc.mState.getCurProcState() == 2;
        }
        return false;
    }

    public void onForegroundActivityChangedLocked(int fromPid, int toUid, int toPid, String name, int taskId, int callingUid, String callingPkg) {
        AppState appState = getAppState(toUid);
        if (appState != null) {
            appState.onActivityForegroundLocked(name);
            AppState.RunningProcess proc = appState.getRunningProcess(toPid);
            if (proc != null) {
                this.mCurrentTaskStack.updateIfNeeded(taskId, proc, callingUid, callingPkg);
                proc.mLastTaskId = taskId;
            }
        }
    }

    public void updateVisibilityLocked(ArraySet<Integer> uids) {
        Iterator<Integer> it = uids.iterator();
        while (it.hasNext()) {
            int uid = it.next().intValue();
            AppState appState = getAppState(uid);
            if (appState != null) {
                appState.updateVisibility();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onReportPowerFrozenSignal(int uid, String reason) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onReportPowerFrozenSignal(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onReportPowerFrozenSignal(int uid, int pid, String reason) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            appState.onReportPowerFrozenSignal(pid, reason);
        }
    }

    public boolean isRecentThawed(int uid, long sinceUptime) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            ArrayList<IAppState.IRunningProcess> runningProcessList = appState.getRunningProcessList();
            Iterator<IAppState.IRunningProcess> it = runningProcessList.iterator();
            while (it.hasNext()) {
                IAppState.IRunningProcess process = it.next();
                ArrayList<AppState.RunningProcess.StateChangeRecord> history = ((AppState.RunningProcess) process).getHistoryInfos(sinceUptime);
                if (history.size() > 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<IAppState.IRunningProcess> getLruProcesses() {
        ArrayList<IAppState.IRunningProcess> lruProcesses = new ArrayList<>();
        synchronized (this.mAppLock) {
            for (int i = 0; i < this.mActiveApps.size(); i++) {
                AppState app = this.mActiveApps.valueAt(i);
                lruProcesses.addAll(app.getRunningProcessList());
            }
        }
        return lruProcesses;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<IAppState.IRunningProcess> getLruProcesses(int uid, String packageName) {
        ArrayList<IAppState.IRunningProcess> processes = new ArrayList<>();
        synchronized (this.mAppLock) {
            AppState app = this.mActiveApps.get(uid);
            if (app != null) {
                processes.addAll(app.getRunningProcessList(packageName));
            }
        }
        return processes;
    }

    public static boolean isSystemApp(ProcessRecord app) {
        if (app == null || app.info == null) {
            return false;
        }
        return (app.info.flags & 129) != 0 || 1000 == app.uid;
    }

    public boolean isSystemApp(int uid) {
        AppState appState = getAppState(uid);
        if (appState != null) {
            return appState.isSystemApp();
        }
        return false;
    }

    public void hibernateAllIfNeeded(String reason) {
        Message msg = this.mMainHandler.obtainMessage(1, reason);
        this.mMainHandler.sendMessage(msg);
    }

    public void hibernateAllInactive(AppState startingApp, String reason) {
        this.mMainHandler.removeMessages(2);
        Message msg = this.mMainHandler.obtainMessage(2, startingApp);
        Bundle data = new Bundle();
        data.putString(EdgeSuppressionManager.EdgeSuppressionHandler.MSG_DATA_REASON, reason);
        msg.setData(data);
        this.mMainHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ActiveApps {
        private final SparseArray<AppState> mActiveApps = new SparseArray<>();

        ActiveApps() {
        }

        void put(int uid, AppState value) {
            this.mActiveApps.put(uid, value);
        }

        void remove(int uid) {
            this.mActiveApps.remove(uid);
        }

        AppState get(int uid) {
            return this.mActiveApps.get(uid);
        }

        void clear() {
            this.mActiveApps.clear();
        }

        int size() {
            return this.mActiveApps.size();
        }

        AppState valueAt(int index) {
            return this.mActiveApps.valueAt(index);
        }

        int keyAt(int index) {
            return this.mActiveApps.keyAt(index);
        }

        int indexOfKey(int uid) {
            return this.mActiveApps.indexOfKey(uid);
        }

        void dump(PrintWriter pw, String[] args, int opti, int dpUid) {
            for (int i = 0; i < this.mActiveApps.size(); i++) {
                int uid = this.mActiveApps.keyAt(i);
                AppState app = this.mActiveApps.valueAt(i);
                if (dpUid != 0) {
                    if (uid == dpUid) {
                        app.dump(pw, args, opti);
                    }
                } else {
                    app.dump(pw, args, opti);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int ringAdvance(int origin, int increment, int size) {
        int index = (origin + increment) % size;
        return index < 0 ? index + size : index;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static List<Integer> parseScenatioActions(int scenarioAction) {
        List<Integer> actions = new ArrayList<>();
        for (int action = 1; action <= scenarioAction; action <<= 1) {
            if ((action & scenarioAction) != 0) {
                actions.add(Integer.valueOf(action));
            }
        }
        return actions;
    }

    /* loaded from: classes.dex */
    public class AppState extends IAppState {
        private static final int MSG_STEP_APP_STATE = 1;
        private static final int MSG_UPDATE_APPSTATE = 2;
        private int mAppState;
        private int mAppSwitchFgCount;
        private boolean mForeground;
        private int mFreeFormCount;
        private MyHandler mHandler;
        private boolean mHasFgService;
        private boolean mHasProtectProcess;
        private boolean mInSplitMode;
        private boolean mIs64BitApp;
        private boolean mIsAutoStartApp;
        private boolean mIsBackupServiceApp;
        private boolean mIsLockedApp;
        private long mLastTopTime;
        private long mLastUidInteractiveTimeMills;
        private long mLastVideoPlayTimeStamp;
        private int mLaunchCount;
        private int mMainProcOomAdj;
        private long mMainProcStartTime;
        private int mMinAmsProcState;
        private int mMinOomAdj;
        private int mMinPriorityScore;
        private int mMinUsageLevel;
        private int mOverlayCount;
        private String mPackageName;
        private final Object mProcLock;
        private int mResourceBehavier;
        private final ArrayMap<String, RunningProcess> mRunningProcs;
        private int mScenarioActions;
        private boolean mSystemApp;
        private boolean mSystemSignature;
        private int mTopAppCount;
        private long mTotalTimeInForeground;
        private final int mUid;
        private boolean mWhiteListVideoPlaying;

        private AppState(int uid, String packageName, Context context) {
            this.mScenarioActions = 0;
            this.mResourceBehavier = 0;
            this.mAppSwitchFgCount = 0;
            this.mForeground = false;
            this.mIsBackupServiceApp = false;
            this.mHasProtectProcess = false;
            this.mIsAutoStartApp = false;
            this.mIsLockedApp = false;
            this.mHasFgService = false;
            this.mTotalTimeInForeground = 0L;
            this.mLastTopTime = 0L;
            this.mLastUidInteractiveTimeMills = 0L;
            this.mMainProcStartTime = 0L;
            this.mAppState = -1;
            this.mTopAppCount = 0;
            this.mFreeFormCount = 0;
            this.mOverlayCount = 0;
            this.mLaunchCount = 0;
            this.mInSplitMode = false;
            this.mWhiteListVideoPlaying = false;
            this.mLastVideoPlayTimeStamp = 0L;
            this.mRunningProcs = new ArrayMap<>();
            this.mProcLock = new Object();
            this.mSystemApp = false;
            this.mSystemSignature = false;
            this.mIs64BitApp = true;
            this.mUid = uid;
            this.mPackageName = packageName;
            this.mHandler = new MyHandler(AppStateManager.this.mLooper);
            this.mSystemSignature = checkSystemSignature();
            synchronized (AppStateManager.this.mPendingInteractiveUids) {
                if (AppStateManager.this.mPendingInteractiveUids.contains(uid)) {
                    this.mLastUidInteractiveTimeMills = ((Long) AppStateManager.this.mPendingInteractiveUids.get(uid)).longValue();
                    AppStateManager.this.mPendingInteractiveUids.remove(uid);
                }
            }
        }

        private boolean checkSystemSignature() {
            if (AppStateManager.this.mPkms == null) {
                AppStateManager appStateManager = AppStateManager.this;
                appStateManager.mPkms = appStateManager.mContext.getPackageManager();
            }
            return AppStateManager.this.mPkms != null && AppStateManager.this.mPkms.checkSignatures("android", this.mPackageName) == 0;
        }

        public long getActions() {
            return this.mScenarioActions;
        }

        private void updateCurrentScenarioAction(final int action, final boolean active) {
            int currentActions;
            int currentActions2 = this.mScenarioActions;
            if (active) {
                currentActions = currentActions2 | action;
            } else {
                currentActions = currentActions2 & (~action);
            }
            if (currentActions != this.mScenarioActions) {
                this.mScenarioActions = currentActions;
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AppStateManager.this.mSmartScenarioManager.onAppActionChanged(action, AppState.this, active);
                        if (AppStateManager.DEBUG) {
                            Slog.i("SmartPower", AppState.this + " actions(" + SmartScenarioManager.actionTypeToString(AppState.this.mScenarioActions) + ")");
                        }
                        if (action == 1024) {
                            AppState.this.recordVideoPlayIfNeeded(active);
                        }
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordVideoPlayIfNeeded(boolean active) {
            if (active) {
                synchronized (this.mProcLock) {
                    boolean whiteListVideoVisible = false;
                    for (RunningProcess proc : this.mRunningProcs.values()) {
                        whiteListVideoVisible |= proc.whiteListVideoVisible();
                    }
                    this.mWhiteListVideoPlaying = whiteListVideoVisible;
                }
                if (AppStateManager.DEBUG && this.mWhiteListVideoPlaying) {
                    Slog.d("SmartPower", this + " WhiteListVideoPlaying " + this.mWhiteListVideoPlaying);
                    return;
                }
                return;
            }
            if (this.mWhiteListVideoPlaying) {
                this.mLastVideoPlayTimeStamp = SystemClock.uptimeMillis();
                this.mWhiteListVideoPlaying = false;
                if (AppStateManager.DEBUG) {
                    Slog.d("SmartPower", this + " WhiteListVideoPlaying " + this.mWhiteListVideoPlaying);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateCurrentResourceBehavier() {
            int currentBehavier = 0;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    currentBehavier = currentBehavier | proc.mAudioBehavier | proc.mGpsBehavier | proc.mNetBehavier | proc.mCamBehavier | proc.mDisplayBehavier;
                }
            }
            if (AppStateManager.this.mHoldScreenUid == this.mUid) {
                currentBehavier |= 1024;
            }
            if (currentBehavier != this.mResourceBehavier) {
                onResourceBehavierChanged(currentBehavier);
            }
        }

        private void onResourceBehavierChanged(int resourceBehavier) {
            int curAction = SmartScenarioManager.calculateScenarioAction(resourceBehavier);
            int preAction = SmartScenarioManager.calculateScenarioAction(this.mResourceBehavier);
            if (curAction != preAction) {
                List<Integer> removedActions = AppStateManager.parseScenatioActions((~curAction) & preAction);
                Iterator<Integer> it = removedActions.iterator();
                while (it.hasNext()) {
                    int action = it.next().intValue();
                    updateCurrentScenarioAction(action, false);
                }
                List<Integer> addedActions = AppStateManager.parseScenatioActions((~preAction) & curAction);
                Iterator<Integer> it2 = addedActions.iterator();
                while (it2.hasNext()) {
                    int action2 = it2.next().intValue();
                    updateCurrentScenarioAction(action2, true);
                }
            }
            this.mResourceBehavier = resourceBehavier;
        }

        public boolean hasForegroundService() {
            return this.mHasFgService;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public int getPriorityScore() {
            return this.mMinPriorityScore;
        }

        public int getUsageLevel() {
            return this.mMinUsageLevel;
        }

        public int getAdj() {
            return this.mMinOomAdj;
        }

        public int getMainProcAdj() {
            return this.mMainProcOomAdj;
        }

        public int getAppSwitchFgCount() {
            return this.mAppSwitchFgCount;
        }

        public void setLaunchCount(int launchCount) {
            this.mLaunchCount = launchCount;
        }

        public int getLaunchCount() {
            return this.mLaunchCount;
        }

        public void setTotalTimeInForeground(long totalTime) {
            this.mTotalTimeInForeground = totalTime;
        }

        public long getTotalTimeInForeground() {
            return this.mTotalTimeInForeground;
        }

        public boolean isAlive() {
            boolean z;
            synchronized (this.mProcLock) {
                z = this.mAppState > 0 && this.mRunningProcs.size() > 0;
            }
            return z;
        }

        public boolean isIdle() {
            return this.mAppState >= 5;
        }

        public boolean isInactive() {
            return this.mAppState >= 3;
        }

        public boolean isProcessPerceptible() {
            boolean isPerceptible = false;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    isPerceptible |= proc.isProcessPerceptible();
                }
            }
            return isPerceptible;
        }

        public boolean isSystemApp() {
            return this.mSystemApp;
        }

        public boolean isSystemSignature() {
            return this.mSystemSignature;
        }

        public boolean isIs64BitApp() {
            return this.mIs64BitApp;
        }

        public boolean isVsible() {
            return this.mForeground;
        }

        public boolean isAutoStartApp() {
            return this.mIsAutoStartApp;
        }

        public boolean isLockedApp() {
            return this.mIsLockedApp;
        }

        public boolean hasProtectProcess() {
            return this.mHasProtectProcess;
        }

        public int getMinAmsProcState() {
            return this.mMinAmsProcState;
        }

        int getProcessState(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                return proc.getCurrentState();
            }
            return -1;
        }

        public boolean hasActivityApp() {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    if (proc.getCurrentState() > 0 && proc.hasActivity()) {
                        return true;
                    }
                }
                return false;
            }
        }

        public int getProcessState(String processName) {
            RunningProcess proc = getRunningProcess(processName);
            if (proc != null) {
                return proc.getCurrentState();
            }
            return -1;
        }

        int getCurrentState() {
            return this.mAppState;
        }

        public ArrayList<IAppState.IRunningProcess> getRunningProcessList() {
            ArrayList<IAppState.IRunningProcess> runningProcList = new ArrayList<>();
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    if (proc.getCurrentState() > 0) {
                        runningProcList.add(proc);
                    }
                }
            }
            return runningProcList;
        }

        ArrayList<RunningProcess> getRunningProcessList(String packageName) {
            ArrayList<RunningProcess> runningProcList = new ArrayList<>();
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    if (proc.getCurrentState() > 0 && proc.mPackageName.equals(packageName)) {
                        runningProcList.add(proc);
                    }
                }
            }
            return runningProcList;
        }

        RunningProcess getRunningProcess(String processName) {
            RunningProcess runningProcess;
            synchronized (this.mProcLock) {
                runningProcess = this.mRunningProcs.get(processName);
            }
            return runningProcess;
        }

        RunningProcess getRunningProcess(int pid) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    if (proc.getPid() == pid) {
                        return proc;
                    }
                }
                return null;
            }
        }

        boolean isProcessKilled(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            return proc == null || proc.getCurrentState() <= 0 || proc.isKilled();
        }

        public boolean isProcessIdle(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            return proc != null && proc.getCurrentState() > 5;
        }

        public boolean isProcessPerceptible(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                return proc.isProcessPerceptible();
            }
            return false;
        }

        public boolean isProcessPerceptible(String processName) {
            RunningProcess proc = getRunningProcess(processName);
            if (proc != null) {
                return proc.isProcessPerceptible();
            }
            return false;
        }

        public ArrayMap<String, DependProcInfo> getDependsProcess() {
            ArrayMap<String, DependProcInfo> dependsList = new ArrayMap<>();
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    if (proc.mServiceDependProcs.size() > 0) {
                        dependsList.putAll(proc.mServiceDependProcs);
                    }
                    if (proc.mProviderDependProcs.size() > 0) {
                        dependsList.putAll(proc.mProviderDependProcs);
                    }
                }
            }
            return dependsList;
        }

        public long getLastTopTime() {
            return this.mLastTopTime;
        }

        public long getLastVideoPlayTimeStamp() {
            return this.mLastVideoPlayTimeStamp;
        }

        public long getMainProcStartTime() {
            return this.mMainProcStartTime;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void processStartedLocked(ProcessRecord record) {
            RunningProcess proc = getRunningProcess(record.processName);
            if (proc == null) {
                RunningProcess proc2 = new RunningProcess(record);
                synchronized (this.mProcLock) {
                    if (this.mRunningProcs.isEmpty()) {
                        this.mSystemApp = AppStateManager.isSystemApp(record);
                    }
                    this.mRunningProcs.put(record.processName, proc2);
                }
            } else {
                proc.updateProcessInfo(record);
            }
            synchronized (this.mProcLock) {
                updatePkgInfoLocked();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void processKilledLocked(ProcessRecord record) {
            RunningProcess proc = getRunningProcess(record.processName);
            if (proc != null) {
                proc.processKilledLocked();
                removeProcessIfNeeded(proc);
                if (isAlive()) {
                    synchronized (this.mProcLock) {
                        updatePkgInfoLocked();
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateProcessLocked(ProcessRecord record) {
            synchronized (this.mProcLock) {
                RunningProcess proc = getRunningProcess(record.processName);
                if (proc != null) {
                    proc.updateProcessInfo(record);
                    removeProcessIfNeeded(proc);
                } else if (!record.isKilled()) {
                    processStartedLocked(record);
                }
            }
        }

        private void removeProcessIfNeeded(RunningProcess proc) {
            if (Process.isIsolated(proc.getUid()) && proc.isKilled()) {
                synchronized (this.mProcLock) {
                    this.mRunningProcs.remove(proc.getProcessName());
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setActivityVisible(String processName, ActivityRecord record, boolean visible) {
            RunningProcess process = getRunningProcess(processName);
            if (process != null) {
                process.setActivityVisible(record, visible);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setWindowVisible(int pid, WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                proc.setWindowVisible(win, attrs, visible);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onApplyOomAdjLocked(ProcessRecord record) {
            updateProcessLocked(record);
            if (isAlive()) {
                synchronized (this.mProcLock) {
                    updatePkgInfoLocked();
                }
            }
        }

        private void updatePkgInfoLocked() {
            if (this.mRunningProcs.size() == 0) {
                return;
            }
            int minAdj = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
            int minAmsProcState = 20;
            int minPriorityScore = 1000;
            int minUsageLevel = 9;
            boolean hasFgService = false;
            boolean is32BitApp = false;
            for (RunningProcess r : this.mRunningProcs.values()) {
                if (r.getCurrentState() != 0 && r.mAdj != -10000) {
                    if (minAdj > r.mAdj || minAdj == -10000) {
                        minAdj = r.mAdj;
                    }
                    if (minAmsProcState > r.mAmsProcState) {
                        minAmsProcState = r.mAmsProcState;
                    }
                    if (minPriorityScore > r.getPriorityScore()) {
                        minPriorityScore = r.getPriorityScore();
                    }
                    if (minUsageLevel > r.getUsageLevel()) {
                        minUsageLevel = r.getUsageLevel();
                    }
                    if (r.hasForegrundService()) {
                        hasFgService = true;
                    }
                    if (r.isMainProc()) {
                        this.mMainProcOomAdj = r.mAdj;
                    }
                    if (!r.mIs64BitProcess) {
                        is32BitApp = true;
                    }
                }
            }
            if (minAdj != this.mMinOomAdj && minAdj != -10000) {
                this.mMinOomAdj = minAdj;
            }
            if (minPriorityScore != this.mMinPriorityScore && minPriorityScore != 1000) {
                this.mMinPriorityScore = minPriorityScore;
            }
            this.mIs64BitApp = !is32BitApp;
            this.mMinUsageLevel = minUsageLevel;
            this.mMinAmsProcState = minAmsProcState;
            this.mHasFgService = hasFgService;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateVisibility() {
            boolean foreground = false;
            boolean inSplitMode = false;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.updateVisibility();
                    boolean foreground2 = foreground | proc.isVisible();
                    inSplitMode |= proc.inSplitMode();
                    foreground = foreground2 | proc.inCurrentStack();
                }
                if (foreground != this.mForeground) {
                    setForegroundLocked(foreground);
                }
                if (this.mTopAppCount > 0) {
                    updateCurrentScenarioAction(2, true);
                } else {
                    updateCurrentScenarioAction(2, false);
                }
                if (this.mFreeFormCount > 0) {
                    updateCurrentScenarioAction(16, true);
                } else {
                    updateCurrentScenarioAction(16, false);
                }
                if (this.mOverlayCount > 0) {
                    updateCurrentScenarioAction(32, true);
                } else {
                    updateCurrentScenarioAction(32, false);
                }
                if (foreground) {
                    updateCurrentScenarioAction(8, false);
                } else {
                    updateCurrentScenarioAction(8, true);
                }
                if (inSplitMode) {
                    updateCurrentScenarioAction(32768, true);
                } else {
                    updateCurrentScenarioAction(32768, false);
                }
            }
        }

        private void setForegroundLocked(boolean foreground) {
            for (RunningProcess proc : this.mRunningProcs.values()) {
                proc.setForeground(foreground);
            }
            this.mForeground = foreground;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void hibernateAllIfNeeded(String reason) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.becomeInactiveIfNeeded(reason);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void hibernateAllInactive(String reason) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    synchronized (proc.mStateLock) {
                        if (proc.getCurrentState() >= 4 && proc.getCurrentState() <= 6 && proc.getAdj() > 0) {
                            proc.moveToStateLocked(7, reason);
                        }
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onAddToWhiteList() {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onAddToWhiteList();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onAddToWhiteList(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                proc.onAddToWhiteList();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onRemoveFromWhiteList() {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onRemoveFromWhiteList();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onRemoveFromWhiteList(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                proc.onRemoveFromWhiteList();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onMediaKey(int pid) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                proc.onMediaKey(SystemClock.uptimeMillis());
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onMediaKey() {
            this.mLastUidInteractiveTimeMills = SystemClock.uptimeMillis();
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onMediaKey(this.mLastUidInteractiveTimeMills);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onInputMethodShow() {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onInputMethodShow();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onBackupChanged(boolean active, ProcessRecord app) {
            RunningProcess proc = getRunningProcess(app.mPid);
            if (proc != null) {
                proc.onBackupChanged(active);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onBackupServiceAppChanged(boolean active, int pid) {
            this.mIsBackupServiceApp = active;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onBackupServiceAppChanged(active);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void componentStart(String content) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.componentStart(content);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onSendPendingIntent(String typeName) {
            String content = AppStateManager.REASON_PENDING_INTENT + typeName;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onSendPendingIntent(content);
                }
            }
            this.mLastUidInteractiveTimeMills = SystemClock.uptimeMillis();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void componentEnd(String content) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.componentEnd(content);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void componentStart(ProcessRecord processRecord, String content) {
            RunningProcess proc = getRunningProcess(processRecord.mPid);
            if (proc != null) {
                proc.componentStart(content);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void componentEnd(ProcessRecord processRecord, String content) {
            RunningProcess proc = getRunningProcess(processRecord.mPid);
            if (proc != null) {
                proc.componentEnd(content);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateServiceDepends(ProcessRecord client, ProcessRecord service, boolean isConnect, long flags) {
            RunningProcess proc = getRunningProcess(client.mPid);
            boolean fgService = (flags & 67108864) != 0;
            if (proc == null) {
                if (client.mPid == Process.myPid() && service.uid != this.mUid) {
                    boolean isJobService = (flags & 32768) != 0;
                    updateSystemServiceDepends(client, service, isConnect, fgService, isJobService);
                    return;
                }
                return;
            }
            proc.updateServiceDepends(service, isConnect, fgService);
        }

        private void updateSystemServiceDepends(ProcessRecord client, ProcessRecord service, boolean isConnect, boolean fgService, boolean jobService) {
            if (!isConnect || fgService || !jobService) {
                AppStateManager.this.mSmartPowerPolicyManager.onDependChanged(isConnect, client.processName, service.processName, service.uid, service.mPid);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateProviderDepends(ProcessRecord client, ProcessRecord host, boolean isConnect) {
            RunningProcess proc = getRunningProcess(client.mPid);
            if (proc != null) {
                proc.updateProviderDepends(host, isConnect);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onReportPowerFrozenSignal(int pid, String reason) {
            RunningProcess proc = getRunningProcess(pid);
            if (proc != null) {
                proc.onReportPowerFrozenSignal(reason);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onReportPowerFrozenSignal(String reason) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    proc.onReportPowerFrozenSignal(reason);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onActivityForegroundLocked(String name) {
            synchronized (this.mProcLock) {
                if (!this.mForeground) {
                    setForegroundLocked(true);
                    for (RunningProcess proc : this.mRunningProcs.values()) {
                        proc.onActivityForegroundLocked(name);
                    }
                }
            }
        }

        public void dump(PrintWriter pw, String[] args, int opti) {
            synchronized (this.mProcLock) {
                pw.println("#" + this.mPackageName + "(" + this.mUid + ") state=" + AppStateManager.appStateToString(this.mAppState));
                pw.println("    minAdj=" + this.mMinOomAdj + " minPriority=" + this.mMinPriorityScore + " hdur=" + AppStateManager.this.mSmartPowerPolicyManager.getHibernateDuration(this));
                pw.println("    proc size=" + this.mRunningProcs.size());
                pw.println("    noRestrict=" + AppStateManager.this.mSmartPowerPolicyManager.isNoRestrictApp(this.mPackageName) + " locked=" + this.mIsLockedApp + " autoStart=" + this.mIsAutoStartApp + " sysSign=" + this.mSystemSignature + " sysApp=" + this.mSystemApp + " is64Bit=" + this.mIs64BitApp);
                for (int i = 0; i < this.mRunningProcs.size(); i++) {
                    RunningProcess proc = this.mRunningProcs.valueAt(i);
                    proc.dump(pw, args, opti);
                }
            }
        }

        public String toString() {
            return this.mPackageName + "/" + this.mUid + " state=" + AppStateManager.appStateToString(this.mAppState) + " adj=" + this.mMinOomAdj + " proc size=" + this.mRunningProcs.size();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateAppState(String reason) {
            int minProcessState;
            int minProcessState2 = -1;
            int minProcessStateExceptProtect = -1;
            boolean hasProtectProcess = false;
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    boolean canHibernate = proc.canHibernate();
                    int state = proc.getCurrentState();
                    hasProtectProcess |= !canHibernate;
                    if ((state > 0 && (state < minProcessState2 || minProcessState2 == 0)) || minProcessState2 == -1) {
                        minProcessState2 = state;
                    }
                    if (canHibernate && (state < minProcessStateExceptProtect || minProcessStateExceptProtect == 0 || minProcessStateExceptProtect == -1)) {
                        minProcessStateExceptProtect = state;
                        if (!this.mForeground && state == 3) {
                            proc.sendBecomeInactiveMsg(reason);
                        }
                    }
                }
                minProcessState = Math.max(minProcessState2, minProcessStateExceptProtect);
            }
            int appState = -1;
            switch (minProcessState) {
                case -1:
                    appState = -1;
                    break;
                case 0:
                    appState = 0;
                    break;
                case 1:
                    appState = 1;
                    break;
                case 2:
                case 3:
                    appState = 2;
                    break;
                case 4:
                    appState = 3;
                    break;
                case 5:
                    appState = 4;
                    break;
                case 6:
                    appState = 5;
                    break;
                case 7:
                    appState = 6;
                    break;
            }
            if (appState != this.mAppState || this.mHasProtectProcess != hasProtectProcess) {
                changeAppState(appState, hasProtectProcess, reason);
            }
        }

        private void changeAppState(int state, boolean hasProtectProcess, String reason) {
            if (state <= 3) {
                this.mHandler.removeMessages(1);
            }
            if (state == 6 && this.mAppState != state && reason.startsWith(AppStateManager.REASON_HIBERNATE_ALL)) {
                this.mHandler.removeMessages(1);
                long delay = AppStateManager.this.mSmartPowerPolicyManager.getHibernateDuration(this);
                if (delay > 0) {
                    Message msg = this.mHandler.obtainMessage(1, AppStateManager.REASON_PERIODIC_ACTIVE);
                    this.mHandler.sendMessageDelayed(msg, delay);
                }
            }
            if (state == 3 && !AppStateManager.this.mSmartPowerPolicyManager.isAppHibernationWhiteList(this)) {
                Message msg2 = this.mHandler.obtainMessage(1, reason);
                this.mHandler.sendMessageDelayed(msg2, AppStateManager.this.mSmartPowerPolicyManager.getInactiveDuration(this.mPackageName, this.mUid));
            }
            if (this.mAppState == 1 && state > 1) {
                this.mLastTopTime = SystemClock.uptimeMillis();
            }
            synchronized (AppStateManager.this.mProcessStateCallbacks) {
                Iterator it = AppStateManager.this.mProcessStateCallbacks.iterator();
                while (it.hasNext()) {
                    IProcessStateCallback callback = (IProcessStateCallback) it.next();
                    callback.onAppStateChanged(this, this.mAppState, state, this.mHasProtectProcess, hasProtectProcess, reason);
                }
            }
            if (AppStateManager.DEBUG) {
                Slog.i("SmartPower", this + " move to " + AppStateManager.appStateToString(state) + " " + reason);
            }
            if (this.mAppState != state && state == 1) {
                this.mAppSwitchFgCount++;
            }
            this.mAppState = state;
            this.mHasProtectProcess = hasProtectProcess;
        }

        private void changeProcessState(int state, String reason) {
            synchronized (this.mProcLock) {
                for (RunningProcess proc : this.mRunningProcs.values()) {
                    synchronized (proc.mStateLock) {
                        if (proc.getCurrentState() > 0 && proc.canHibernate() && (state == 5 || proc.getCurrentState() < state)) {
                            proc.moveToStateLocked(state, reason);
                        }
                    }
                }
            }
        }

        private boolean isAppRunningComponentLock(ProcessRecord proc) {
            return proc != null && (proc.mServices.numberOfExecutingServices() > 0 || proc.mReceivers.numberOfCurReceivers() > 0);
        }

        private boolean isAppRunningComponent() {
            synchronized (AppStateManager.this.mAMS) {
                for (RunningProcess r : this.mRunningProcs.values()) {
                    if (isAppRunningComponentLock(r.getProcessRecord())) {
                        return true;
                    }
                }
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void stepAppState(String reason) {
            this.mHandler.removeMessages(1);
            long nextStepDuration = 0;
            Message msg = this.mHandler.obtainMessage(1, reason);
            switch (this.mAppState) {
                case 3:
                case 4:
                    if (this.mMinOomAdj <= 0 && isAppRunningComponent()) {
                        this.mHandler.sendMessageDelayed(msg, 1000L);
                        return;
                    } else {
                        changeProcessState(6, reason);
                        nextStepDuration = AppStateManager.this.mSmartPowerPolicyManager.getIdleDur(this.mPackageName);
                        break;
                    }
                    break;
                case 5:
                    if (this.mMinOomAdj <= 0 && (this.mMinAmsProcState < 6 || isAppRunningComponent())) {
                        this.mHandler.sendMessageDelayed(msg, 1000L);
                        return;
                    }
                    changeProcessState(7, reason);
                    msg = this.mHandler.obtainMessage(1, AppStateManager.REASON_PERIODIC_ACTIVE);
                    nextStepDuration = AppStateManager.this.mSmartPowerPolicyManager.getHibernateDuration(this);
                    break;
                    break;
                case 6:
                    changeProcessState(5, reason);
                    nextStepDuration = AppStateManager.this.mSmartPowerPolicyManager.getMaintenanceDur(this.mPackageName);
                    break;
            }
            if (nextStepDuration > 0) {
                this.mHandler.sendMessageDelayed(msg, nextStepDuration);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class MyHandler extends Handler {
            MyHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        AppState.this.stepAppState((String) msg.obj);
                        return;
                    case 2:
                        AppState.this.updateAppState((String) msg.obj);
                        return;
                    default:
                        return;
                }
            }
        }

        /* loaded from: classes.dex */
        public class RunningProcess extends IAppState.IRunningProcess {
            private String LOG_TAG;
            private int mAdj;
            private String mAdjType;
            private int mAmsProcState;
            private boolean mAudioActive;
            private int mAudioBehavier;
            private long mBackgroundCount;
            private long mBackgroundDuration;
            private boolean mBleActive;
            private int mCamBehavier;
            private boolean mCameraActive;
            public final AtomicLong mCurCpuTime;
            private volatile boolean mDependencyProtected;
            private boolean mDisplayActive;
            private int mDisplayBehavier;
            private long mForegroundCount;
            private long mForegroundDuration;
            private boolean mGpsActive;
            private int mGpsBehavier;
            private boolean mHasActivity;
            private boolean mHasFgService;
            private long mHibernatonCount;
            private long mHibernatonDuration;
            private int mHistoryIndexNext;
            private long mIdleCount;
            private long mIdleDuration;
            private boolean mIs64BitProcess;
            private boolean mIsKilled;
            private boolean mIsMainProc;
            public final AtomicLong mLastCpuTime;
            private long mLastInteractiveTimeMills;
            private long mLastPss;
            private int mLastRecordState;
            private long mLastStateTimeMills;
            private long mLastSwapPss;
            private int mLastTaskId;
            private boolean mNetActive;
            private int mNetBehavier;
            private String mPackageName;
            private int mPid;
            private ProcessPriorityScore mProcPriorityScore;
            private String mProcessName;
            private ProcessRecord mProcessRecord;
            private final ArrayMap<String, DependProcInfo> mProviderDependProcs;
            private AppPowerResourceCallback mResourcesCallback;
            private final ArrayMap<String, DependProcInfo> mServiceDependProcs;
            private int mState;
            private StateChangeRecord[] mStateChangeHistory;
            private final Object mStateLock;
            private int mUid;
            private int mUserId;
            private final ArraySet<WeakReference<ActivityRecord>> mVisibleActivities;
            private final ArraySet<WeakReference<WindowManagerPolicy.WindowState>> mVisibleWindows;

            private RunningProcess(ProcessRecord record) {
                super(AppState.this);
                this.mUid = -1;
                this.mUserId = -1;
                this.mPid = -1;
                this.mLastPss = -1L;
                this.mLastSwapPss = -1L;
                this.LOG_TAG = "SmartPower";
                this.mAdj = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
                this.mAmsProcState = 20;
                this.mHasActivity = false;
                this.mHasFgService = false;
                this.mAudioActive = false;
                this.mGpsActive = false;
                this.mNetActive = false;
                this.mBleActive = false;
                this.mCameraActive = false;
                this.mDisplayActive = false;
                this.mIsMainProc = false;
                this.mAudioBehavier = 0;
                this.mGpsBehavier = 0;
                this.mNetBehavier = 0;
                this.mCamBehavier = 0;
                this.mDependencyProtected = false;
                this.mDisplayBehavier = 0;
                this.mIs64BitProcess = true;
                this.mState = -1;
                this.mProcPriorityScore = new ProcessPriorityScore();
                this.mIsKilled = false;
                this.mStateLock = new Object();
                this.mServiceDependProcs = new ArrayMap<>();
                this.mProviderDependProcs = new ArrayMap<>();
                this.mResourcesCallback = new AppPowerResourceCallback();
                this.mVisibleWindows = new ArraySet<>();
                this.mVisibleActivities = new ArraySet<>();
                this.mStateChangeHistory = new StateChangeRecord[AppStateManager.HISTORY_SIZE];
                this.mLastRecordState = -1;
                this.mLastStateTimeMills = SystemClock.uptimeMillis();
                this.mHistoryIndexNext = 0;
                this.mBackgroundDuration = 0L;
                this.mForegroundDuration = 0L;
                this.mIdleDuration = 0L;
                this.mHibernatonDuration = 0L;
                this.mBackgroundCount = 0L;
                this.mForegroundCount = 0L;
                this.mIdleCount = 0L;
                this.mHibernatonCount = 0L;
                this.mLastInteractiveTimeMills = 0L;
                this.mLastTaskId = 0;
                this.mLastCpuTime = new AtomicLong(0L);
                this.mCurCpuTime = new AtomicLong(0L);
                updateProcessInfo(record);
                if (AppState.this.mForeground) {
                    setForeground(true);
                } else {
                    setForeground(false);
                }
                this.mLastRecordState = this.mState;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void updateProcessInfo(final ProcessRecord record) {
                this.mUserId = record.userId;
                this.mUid = record.uid;
                this.mProcessName = record.processName;
                this.mPackageName = record.info.packageName;
                this.mProcessRecord = record;
                this.mAdj = record.mState.getSetAdj();
                this.mAmsProcState = record.mState.getSetProcState();
                this.mLastPss = record.mProfile.getLastPss();
                this.mLastSwapPss = record.mProfile.getLastSwapPss();
                this.mIsKilled = record.isKilled();
                this.mAdjType = record.mState.getAdjType();
                this.mHasActivity = record.hasActivities();
                this.mHasFgService = record.mServices.hasForegroundServices();
                this.mIsMainProc = record.info.packageName.equals(record.processName);
                this.mIs64BitProcess = !is32BitProcess(record);
                if (this.mPid != record.mPid) {
                    this.mPid = record.mPid;
                    if (this.mIsMainProc) {
                        AppState.this.mMainProcStartTime = SystemClock.uptimeMillis();
                    }
                    this.LOG_TAG = "SmartPower: " + this.mProcessName + "/" + this.mUid + "(" + this.mPid + ")";
                    if (this.mPid > 0 && !this.mIsKilled) {
                        synchronized (this.mStateLock) {
                            if (AppState.this.mForeground && this.mState != 1) {
                                moveToStateLocked(2, AppStateManager.REASON_PROCESS_START);
                            } else if (!AppState.this.mForeground) {
                                moveToStateLocked(3, AppStateManager.REASON_PROCESS_START);
                            }
                        }
                        AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppState.this.mIsAutoStartApp = AppOpsUtils.getApplicationAutoStart(AppStateManager.this.mContext.getApplicationContext(), RunningProcess.this.mPackageName, record.info.uid) == 0;
                                AppState.this.mIsLockedApp = AppStateManager.this.mProcessPolicy.isLockedApplication(RunningProcess.this.mPackageName, RunningProcess.this.mUserId);
                                AppStateManager.this.mAppPowerResourceManager.registerCallback(1, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                                AppStateManager.this.mAppPowerResourceManager.registerCallback(5, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                                AppStateManager.this.mAppPowerResourceManager.registerCallback(3, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                                AppStateManager.this.mAppPowerResourceManager.registerCallback(6, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                                AppStateManager.this.mAppPowerResourceManager.registerCallback(7, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                            }
                        });
                    }
                }
                if (this.mIsKilled) {
                    processKilledLocked();
                } else {
                    this.mProcPriorityScore.updatePriorityScore();
                }
                int i = this.mAdj;
                if (i >= 100) {
                    becomeInactiveIfNeeded(AppStateManager.REASON_ADJ_BELOW_VISIBLE);
                } else if (i < 0) {
                    becomeActiveIfNeeded(AppStateManager.REASON_ADJ_ABOVE_FG);
                }
            }

            private boolean is32BitProcess(ProcessRecord proc) {
                String primaryCpuAbi = proc.info.primaryCpuAbi;
                String secondaryCpuAbi = proc.info.secondaryCpuAbi;
                if (primaryCpuAbi == null && secondaryCpuAbi == null) {
                    return false;
                }
                if (primaryCpuAbi != null && primaryCpuAbi.startsWith("arm64")) {
                    return false;
                }
                if (secondaryCpuAbi != null && secondaryCpuAbi.startsWith("arm64")) {
                    return false;
                }
                return true;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void processKilledLocked() {
                synchronized (this.mStateLock) {
                    moveToStateLocked(0, AppStateManager.REASON_PROCESS_DIED);
                }
                if (this.mDependencyProtected) {
                    synchronized (this.mServiceDependProcs) {
                        for (DependProcInfo processInfo : this.mServiceDependProcs.values()) {
                            AppStateManager.this.mSmartPowerPolicyManager.onDependChanged(false, this.mProcessName, processInfo.mProcessName, processInfo.mUid, processInfo.mPid);
                        }
                    }
                }
                reset();
            }

            private void reset() {
                synchronized (this.mVisibleWindows) {
                    this.mVisibleWindows.clear();
                }
                synchronized (this.mVisibleActivities) {
                    this.mVisibleActivities.clear();
                }
                this.mAudioActive = false;
                this.mGpsActive = false;
                this.mNetActive = false;
                this.mDisplayActive = false;
                this.mCameraActive = false;
                this.mAudioBehavier = 0;
                this.mGpsBehavier = 0;
                this.mNetBehavier = 0;
                this.mCamBehavier = 0;
                this.mLastCpuTime.set(0L);
                this.mCurCpuTime.set(0L);
                this.mDisplayBehavier = 0;
                AppState.this.mTopAppCount = 0;
                AppState.this.mFreeFormCount = 0;
                AppState.this.mOverlayCount = 0;
                this.mLastPss = 0L;
                this.mLastSwapPss = 0L;
                if (isMainProc()) {
                    AppState.this.mAppSwitchFgCount = 0;
                }
                AppState.this.updateCurrentResourceBehavier();
                synchronized (this.mServiceDependProcs) {
                    this.mServiceDependProcs.clear();
                }
                synchronized (this.mProviderDependProcs) {
                    this.mProviderDependProcs.clear();
                }
                AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.2
                    @Override // java.lang.Runnable
                    public void run() {
                        if (RunningProcess.this.mState != 0) {
                            return;
                        }
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(2, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(1, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(5, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(3, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(6, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid, RunningProcess.this.mPid);
                        AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(7, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void setActivityVisible(ActivityRecord record, boolean visible) {
                if (AppStateManager.DEBUG_PROC) {
                    Slog.d(this.LOG_TAG, record.toString() + " onActivityVisibilityChanged " + visible);
                }
                synchronized (this.mVisibleActivities) {
                    WeakReference<ActivityRecord> recordRef = null;
                    int i = 0;
                    while (true) {
                        if (i >= this.mVisibleActivities.size()) {
                            break;
                        }
                        WeakReference<ActivityRecord> ref = this.mVisibleActivities.valueAt(i);
                        if (ref.get() != record) {
                            i++;
                        } else {
                            recordRef = ref;
                            break;
                        }
                    }
                    if (visible && recordRef == null) {
                        this.mVisibleActivities.add(new WeakReference<>(record));
                        if (record.inFreeformWindowingMode()) {
                            AppState.this.mFreeFormCount++;
                        } else {
                            AppState.this.mTopAppCount++;
                        }
                    } else if (!visible && recordRef != null) {
                        this.mVisibleActivities.remove(recordRef);
                        if (record.inFreeformWindowingMode()) {
                            AppState appState = AppState.this;
                            appState.mFreeFormCount--;
                        } else {
                            AppState appState2 = AppState.this;
                            appState2.mTopAppCount--;
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void setWindowVisible(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
                if (AppStateManager.DEBUG_PROC) {
                    Slog.d(this.LOG_TAG, win.toString() + " onWindowVisibilityChanged " + visible);
                }
                synchronized (this.mVisibleWindows) {
                    WeakReference<WindowManagerPolicy.WindowState> winRef = null;
                    int i = 0;
                    while (true) {
                        if (i >= this.mVisibleWindows.size()) {
                            break;
                        }
                        WeakReference<WindowManagerPolicy.WindowState> ref = this.mVisibleWindows.valueAt(i);
                        if (ref.get() != win) {
                            i++;
                        } else {
                            winRef = ref;
                            break;
                        }
                    }
                    if (visible && winRef == null) {
                        this.mVisibleWindows.add(new WeakReference<>(win));
                        if (attrs.type == 2038 && !attrs.isFullscreen()) {
                            AppState.this.mOverlayCount++;
                        }
                    } else if (!visible && winRef != null) {
                        this.mVisibleWindows.remove(winRef);
                        if (attrs.type == 2038 && !attrs.isFullscreen()) {
                            AppState appState = AppState.this;
                            appState.mOverlayCount--;
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void updateVisibility() {
                updateActivityVisiblity();
                updateWindowVisiblity();
                synchronized (this.mStateLock) {
                    if (!hasVisibleWindow() && !hasVisibleActivity()) {
                        int i = this.mState;
                        if (i != 0 && i < 2) {
                            moveToStateLocked(2, AppStateManager.REASON_BECOME_INVISIBLE);
                        }
                    }
                    if (this.mState != 1) {
                        moveToStateLocked(1, AppStateManager.REASON_BECOME_VISIBLE);
                    }
                }
            }

            private void reportDependChangedIfNeeded() {
                boolean isConnected = AppState.this.mIsBackupServiceApp || (isForeground() && (this.mUid != 1000 || hasVisibleActivity()));
                if (isConnected != this.mDependencyProtected) {
                    synchronized (this.mServiceDependProcs) {
                        if (isConnected != this.mDependencyProtected) {
                            for (DependProcInfo processInfo : this.mServiceDependProcs.values()) {
                                AppStateManager.this.mSmartPowerPolicyManager.onDependChanged(isConnected, this.mProcessName, processInfo.mProcessName, processInfo.mUid, processInfo.mPid);
                            }
                            this.mDependencyProtected = isConnected;
                        }
                    }
                }
            }

            private void updateWindowVisiblity() {
                synchronized (this.mVisibleWindows) {
                    if (this.mVisibleWindows.size() > 0) {
                        int i = 0;
                        while (i < this.mVisibleWindows.size()) {
                            WindowManagerPolicy.WindowState win = this.mVisibleWindows.valueAt(i).get();
                            if (win == null) {
                                this.mVisibleWindows.removeAt(i);
                                i--;
                            }
                            i++;
                        }
                    }
                }
            }

            private void updateActivityVisiblity() {
                boolean inSplitMode = false;
                synchronized (this.mVisibleActivities) {
                    if (this.mVisibleActivities.size() > 0) {
                        int i = 0;
                        while (i < this.mVisibleActivities.size()) {
                            ActivityRecord activity = this.mVisibleActivities.valueAt(i).get();
                            if (activity == null) {
                                this.mVisibleActivities.removeAt(i);
                                i--;
                            } else if (activity.inSplitScreenWindowingMode()) {
                                inSplitMode = true;
                            }
                            i++;
                        }
                        AppState.this.mInSplitMode = inSplitMode;
                    }
                }
                AppState.this.mInSplitMode = inSplitMode;
            }

            private boolean hasVisibleWindow() {
                boolean z;
                synchronized (this.mVisibleWindows) {
                    z = this.mVisibleWindows.size() > 0;
                }
                return z;
            }

            private boolean hasVisibleActivity() {
                boolean z;
                synchronized (this.mVisibleActivities) {
                    z = this.mVisibleActivities.size() > 0;
                }
                return z;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean whiteListVideoVisible() {
                synchronized (this.mVisibleActivities) {
                    for (int i = 0; i < this.mVisibleActivities.size(); i++) {
                        ActivityRecord activity = this.mVisibleActivities.valueAt(i).get();
                        if (activity != null) {
                            Iterator it = AppStateManager.this.mWhiteListVideoActivities.iterator();
                            while (it.hasNext()) {
                                String videoActivity = (String) it.next();
                                if (activity.toString().contains(videoActivity)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void setForeground(boolean foreground) {
                synchronized (this.mStateLock) {
                    if (this.mState == 0) {
                        return;
                    }
                    if (AppStateManager.DEBUG_PROC) {
                        Slog.d(this.LOG_TAG, "set foreground " + foreground);
                    }
                    if (!foreground && this.mState < 3) {
                        moveToStateLocked(3, AppStateManager.REASON_BECOME_BACKGROUND);
                        becomeInactiveIfNeeded(AppStateManager.REASON_BECOME_BACKGROUND);
                    } else if (foreground && this.mState > 2) {
                        moveToStateLocked(2, AppStateManager.REASON_BECOME_FOREGROUND);
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onMediaKey(long timeStamp) {
                Slog.d(this.LOG_TAG, AppStateManager.REASON_PRESS_MEDIA_KEY);
                this.mLastInteractiveTimeMills = timeStamp;
                becomeActiveIfNeeded(AppStateManager.REASON_PRESS_MEDIA_KEY);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onInputMethodShow() {
                Slog.d(this.LOG_TAG, AppStateManager.REASON_SHOW_INPUTMETHOD);
                synchronized (this.mStateLock) {
                    if (!isKilled()) {
                        moveToStateLocked(1, AppStateManager.REASON_SHOW_INPUTMETHOD);
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onBackupChanged(boolean active) {
                if (active) {
                    becomeActiveIfNeeded(AppStateManager.REASON_BACKUP_START);
                } else {
                    sendBecomeInactiveMsg(AppStateManager.REASON_BACKUP_END);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onBackupServiceAppChanged(boolean active) {
                if (active) {
                    becomeActiveIfNeeded(AppStateManager.REASON_BACKUP_SERVICE_CONNECTED);
                } else {
                    sendBecomeInactiveMsg(AppStateManager.REASON_BACKUP_SERVICE_DISCONNECTED);
                }
                reportDependChangedIfNeeded();
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onSendPendingIntent(String content) {
                if (this.mState == 7) {
                    becomeActiveIfNeeded(content);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void componentStart(String content) {
                if (this.mState == 7) {
                    becomeActiveIfNeeded(content);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void componentEnd(String content) {
                sendBecomeInactiveMsg(content);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onAddToWhiteList() {
                if (AppStateManager.DEBUG) {
                    Slog.w(this.LOG_TAG, AppStateManager.REASON_ADD_TO_WHITELIST);
                }
                becomeActiveIfNeeded(AppStateManager.REASON_ADD_TO_WHITELIST);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onRemoveFromWhiteList() {
                if (AppStateManager.DEBUG) {
                    Slog.w(this.LOG_TAG, AppStateManager.REASON_REMOVE_FROM_WHITELIST);
                }
                sendBecomeInactiveMsg(AppStateManager.REASON_REMOVE_FROM_WHITELIST);
            }

            public boolean isDependsServiceProcess(String dependsProcName) {
                boolean containsKey;
                synchronized (this.mServiceDependProcs) {
                    containsKey = this.mServiceDependProcs.containsKey(dependsProcName);
                }
                return containsKey;
            }

            public boolean isDependsProvidersProcess(String dependsProcName) {
                return this.mProviderDependProcs.containsKey(dependsProcName);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void sendBecomeInactiveMsg(final String resson) {
                AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.3
                    @Override // java.lang.Runnable
                    public void run() {
                        RunningProcess.this.becomeInactiveIfNeeded(resson);
                    }
                });
            }

            private void sendBecomeActiveMsg(final String resson) {
                AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.4
                    @Override // java.lang.Runnable
                    public void run() {
                        RunningProcess.this.becomeActiveIfNeeded(resson);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onReportPowerFrozenSignal(String reason) {
                if (this.mState == 7) {
                    sendBecomeActiveMsg(reason);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onActivityForegroundLocked(String name) {
                String reason = AppStateManager.REASON_START_ACTIVITY + name;
                becomeActiveIfNeeded(reason);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void updateServiceDepends(ProcessRecord service, boolean isConnect, boolean fgService) {
                if (service.uid != this.mUid) {
                    synchronized (this.mServiceDependProcs) {
                        if (isConnect) {
                            this.mServiceDependProcs.put(service.processName, new DependProcInfo(service.uid, service.mPid, service.processName));
                        } else {
                            this.mServiceDependProcs.remove(service.processName);
                        }
                    }
                    if (!this.mDependencyProtected) {
                        if (this.mUid != 1000) {
                            return;
                        }
                        if (!fgService && isConnect) {
                            return;
                        }
                    }
                    AppStateManager.this.mSmartPowerPolicyManager.onDependChanged(isConnect, this.mProcessName, service.processName, service.uid, service.mPid);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void updateProviderDepends(ProcessRecord host, boolean isConnect) {
                if (host.uid != this.mUid) {
                    synchronized (this.mProviderDependProcs) {
                        if (isConnect) {
                            this.mProviderDependProcs.put(host.processName, new DependProcInfo(host.uid, host.mPid, host.processName));
                        } else {
                            this.mProviderDependProcs.remove(host.processName);
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void becomeActiveIfNeeded(String reason) {
                synchronized (this.mStateLock) {
                    if (AppStateManager.sEnable && this.mState > 3) {
                        moveToStateLocked(3, reason);
                        if (!this.mNetActive) {
                            AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.5
                                @Override // java.lang.Runnable
                                public void run() {
                                    AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(2, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                                }
                            });
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void becomeInactiveIfNeeded(String reason) {
                synchronized (this.mStateLock) {
                    if (AppStateManager.sEnable && this.mState == 3 && this.mAdj >= 0) {
                        if (this.mAudioActive) {
                            if (AppStateManager.DEBUG) {
                                Slog.d(this.LOG_TAG, "skip inactive for audio active");
                            }
                            return;
                        }
                        if (this.mBleActive) {
                            if (AppStateManager.DEBUG) {
                                Slog.d(this.LOG_TAG, "skip inactive for bluetooth active");
                            }
                            return;
                        }
                        if (this.mGpsActive) {
                            if (AppStateManager.DEBUG) {
                                Slog.d(this.LOG_TAG, "skip inactive for GPS active");
                            }
                        } else if (this.mNetActive) {
                            if (AppStateManager.DEBUG) {
                                Slog.d(this.LOG_TAG, "skip inactive for net active");
                            }
                        } else if (AppState.this.mIsBackupServiceApp) {
                            if (AppStateManager.DEBUG) {
                                Slog.d(this.LOG_TAG, "skip inactive for backing up");
                            }
                        } else {
                            if (AppStateManager.this.mSmartPowerPolicyManager.needCheckNet(this.mPackageName, this.mUid)) {
                                AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.6
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        AppStateManager.this.mAppPowerResourceManager.registerCallback(2, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                                    }
                                });
                            }
                            moveToStateLocked(4, reason);
                        }
                    } else if (AppStateManager.sEnable && this.mState == 3 && AppStateManager.DEBUG) {
                        Slog.d(this.LOG_TAG, "skip inactive state(" + AppStateManager.processStateToString(this.mState) + ") adj(" + this.mAdj + ") R(" + reason + ")");
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void moveToStateLocked(int targetState, String reason) {
                if (!AppStateManager.sEnable || targetState == this.mState) {
                    return;
                }
                boolean isWhiteList = AppStateManager.this.mSmartPowerPolicyManager.isProcessHibernationWhiteList(this);
                if (targetState == 7 && isWhiteList) {
                    return;
                }
                Trace.traceBegin(131072L, "moveToStateLocked");
                synchronized (AppStateManager.this.mProcessStateCallbacks) {
                    Iterator it = AppStateManager.this.mProcessStateCallbacks.iterator();
                    while (it.hasNext()) {
                        IProcessStateCallback callback = (IProcessStateCallback) it.next();
                        callback.onProcessStateChanged(this, this.mState, targetState, reason);
                    }
                }
                stateChangeToRecord(targetState, reason);
                this.mState = targetState;
                AppState.this.mHandler.removeMessages(2);
                Message msg = AppState.this.mHandler.obtainMessage(2, reason);
                AppState.this.mHandler.sendMessage(msg);
                reportDependChangedIfNeeded();
                Trace.traceEnd(131072L);
            }

            private void stateChangeToRecord(int targetState, String reason) {
                if (AppStateManager.DEBUG) {
                    Slog.i(this.LOG_TAG, new StateChangeRecord(this.mState, targetState, 0L, reason, this.mAdj).toString());
                }
                if (targetState != this.mLastRecordState && AppStateManager.mStatesNeedToRecord.contains(Integer.valueOf(targetState))) {
                    long now = SystemClock.uptimeMillis();
                    long interval = now - this.mLastStateTimeMills;
                    StateChangeRecord record = new StateChangeRecord(this.mLastRecordState, targetState, interval, reason, this.mAdj);
                    addToHistory(record);
                    this.mLastStateTimeMills = now;
                    this.mLastRecordState = targetState;
                }
            }

            private void addToHistory(StateChangeRecord record) {
                StateChangeRecord[] stateChangeRecordArr = this.mStateChangeHistory;
                int i = this.mHistoryIndexNext;
                stateChangeRecordArr[i] = record;
                this.mHistoryIndexNext = AppStateManager.ringAdvance(i, 1, AppStateManager.HISTORY_SIZE);
                if (!AppStateManager.DEBUG && record.mNewState != 0) {
                    Slog.i(this.LOG_TAG, record.toString());
                }
                if (record.mOldState == 6) {
                    this.mIdleDuration += record.mInterval;
                } else if (record.mOldState == 7) {
                    this.mHibernatonDuration += record.mInterval;
                } else if (record.mOldState > 0 && record.mOldState < 3) {
                    this.mForegroundDuration += record.mInterval;
                } else if (record.mOldState >= 3) {
                    this.mBackgroundDuration += record.mInterval;
                }
                switch (record.mNewState) {
                    case 2:
                        this.mForegroundCount++;
                        return;
                    case 3:
                        this.mBackgroundCount++;
                        return;
                    case 4:
                    case 5:
                    default:
                        return;
                    case 6:
                        this.mIdleCount++;
                        return;
                    case 7:
                        this.mHibernatonCount++;
                        return;
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public ArrayList<StateChangeRecord> getHistoryInfos(long sinceUptime) {
                StateChangeRecord stateChangeRecord;
                ArrayList<StateChangeRecord> ret = new ArrayList<>();
                int index = AppStateManager.ringAdvance(this.mHistoryIndexNext, -1, AppStateManager.HISTORY_SIZE);
                for (int i = 0; i < AppStateManager.HISTORY_SIZE && (stateChangeRecord = this.mStateChangeHistory[index]) != null && stateChangeRecord.mTimeStamp >= sinceUptime; i++) {
                    ret.add(this.mStateChangeHistory[index]);
                    index = AppStateManager.ringAdvance(index, -1, AppStateManager.HISTORY_SIZE);
                }
                return ret;
            }

            public boolean isMainProc() {
                return this.mIsMainProc;
            }

            public int getUserId() {
                return this.mUserId;
            }

            public int getUid() {
                return this.mUid;
            }

            public int getPid() {
                return this.mPid;
            }

            public String getProcessName() {
                return this.mProcessName;
            }

            public String getPackageName() {
                return this.mPackageName;
            }

            public ProcessRecord getProcessRecord() {
                return this.mProcessRecord;
            }

            public int getAdj() {
                return this.mAdj;
            }

            public int getCurrentState() {
                return this.mState;
            }

            public int getAmsProcState() {
                return this.mAmsProcState;
            }

            public String getAdjType() {
                return this.mAdjType;
            }

            public int getLastTaskId() {
                return this.mLastTaskId;
            }

            public boolean isLastInterActive() {
                long now = SystemClock.uptimeMillis();
                return now - this.mLastInteractiveTimeMills < SmartPowerSettings.DEF_LAST_INTER_ACTIVE_DURATION || now - AppState.this.mLastUidInteractiveTimeMills < SmartPowerSettings.DEF_LAST_INTER_ACTIVE_DURATION;
            }

            public boolean isVisible() {
                return this.mState == 1;
            }

            public boolean inSplitMode() {
                return AppState.this.mInSplitMode;
            }

            public boolean inCurrentStack() {
                return false;
            }

            public boolean isForeground() {
                return AppState.this.mForeground;
            }

            public boolean isSystemApp() {
                return AppState.this.mSystemApp;
            }

            public boolean is64BitProcess() {
                return this.mIs64BitProcess;
            }

            public boolean isSystemSignature() {
                return AppState.this.mSystemSignature;
            }

            public boolean isAutoStartApp() {
                return AppState.this.mIsAutoStartApp;
            }

            public boolean isIdle() {
                return this.mState > 5;
            }

            public boolean isHibernation() {
                return this.mState == 7;
            }

            public boolean isKilled() {
                return this.mIsKilled || this.mState == 0;
            }

            public boolean isProcessPerceptible() {
                int i = this.mState;
                return i == 1 || i == 2 || this.mAudioActive || this.mGpsActive || this.mNetActive || this.mBleActive || AppState.this.mIsBackupServiceApp || isLastInterActive();
            }

            public boolean hasActivity() {
                return this.mHasActivity;
            }

            public boolean hasForegrundService() {
                return this.mHasFgService;
            }

            public void updatePss() {
                long[] swapTmp = new long[3];
                this.mLastPss = Debug.getPss(this.mPid, swapTmp, null);
                this.mLastSwapPss = swapTmp[1];
            }

            public void setPss(long pss, long swapPss) {
                this.mLastPss = pss;
                this.mLastSwapPss = swapPss;
            }

            public long getPss() {
                if (this.mLastPss == 0) {
                    updatePss();
                }
                return this.mLastPss;
            }

            public long getSwapPss() {
                if (this.mLastPss == 0) {
                    updatePss();
                }
                return this.mLastSwapPss;
            }

            public int getPriorityScore() {
                return this.mProcPriorityScore.mPriorityScore;
            }

            public int getUsageLevel() {
                return this.mProcPriorityScore.mUsageLevel;
            }

            public boolean canHibernate() {
                return (isProcessPerceptible() || AppStateManager.this.mSmartPowerPolicyManager.isProcessHibernationWhiteList(this)) ? false : true;
            }

            public String toString() {
                return this.mProcessName + "/" + this.mUid + "(" + this.mPid + ") state=" + AppStateManager.processStateToString(this.mState) + " adj=" + this.mAdj + " pri=" + getPriorityScore() + " usage=" + getUsageLevel() + " isKilled=" + this.mIsKilled + " is64Bit=" + this.mIs64BitProcess;
            }

            public long addAndGetCurCpuTime(long cpuTime) {
                return this.mCurCpuTime.addAndGet(cpuTime);
            }

            public boolean compareAndSetLastCpuTime(long oldTime, long cpuTime) {
                return this.mLastCpuTime.compareAndSet(oldTime, cpuTime);
            }

            public void setLastCpuTime(long cpuTime) {
                this.mLastCpuTime.set(cpuTime);
            }

            public long getCurCpuTime() {
                return this.mCurCpuTime.get();
            }

            public long getLastCpuTime() {
                return this.mLastCpuTime.get();
            }

            public void dump(PrintWriter pw, String[] args, int opti) {
                long currentDuration;
                pw.println("        name=" + this.mProcessName + "(" + this.mPid + ")  state=" + AppStateManager.processStateToString(this.mState));
                pw.println("            pkg=" + this.mPackageName + " adj=" + this.mAdj + " pri=" + getPriorityScore() + " usage=" + getUsageLevel());
                pw.println("            audioActive=" + this.mAudioActive + " gpsActive=" + this.mGpsActive + " bleActive=" + this.mBleActive + " netActive=" + this.mNetActive);
                long currentDuration2 = (SystemClock.uptimeMillis() - this.mLastStateTimeMills) / 1000;
                long foregroundDuration = this.mForegroundDuration / 1000;
                long backgroundDuration = this.mBackgroundDuration / 1000;
                long idleDuration = this.mIdleDuration / 1000;
                long hibernationDuration = this.mHibernatonDuration / 1000;
                int i = this.mState;
                if (i == 6) {
                    idleDuration += currentDuration2;
                } else if (i == 7) {
                    hibernationDuration += currentDuration2;
                } else if (i != 0 && i < 3) {
                    foregroundDuration += currentDuration2;
                } else if (i >= 3) {
                    backgroundDuration += currentDuration2;
                }
                pw.println("            f(" + foregroundDuration + ":" + this.mForegroundCount + ") b(" + backgroundDuration + ":" + this.mBackgroundCount + ") i(" + idleDuration + ":" + this.mIdleCount + ") h(" + hibernationDuration + ":" + this.mHibernatonCount + ")");
                synchronized (this.mVisibleActivities) {
                    try {
                        if (this.mVisibleActivities.size() > 0) {
                            pw.println("            visible activities size=" + this.mVisibleActivities.size());
                            int i2 = 0;
                            while (i2 < this.mVisibleActivities.size()) {
                                ActivityRecord record = this.mVisibleActivities.valueAt(i2).get();
                                if (record == null) {
                                    currentDuration = currentDuration2;
                                } else {
                                    currentDuration = currentDuration2;
                                    try {
                                        pw.println("            " + record);
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                i2++;
                                currentDuration2 = currentDuration;
                            }
                        }
                        synchronized (this.mVisibleWindows) {
                            if (this.mVisibleWindows.size() > 0) {
                                pw.println("            visible windows size=" + this.mVisibleWindows.size());
                                for (int i3 = 0; i3 < this.mVisibleWindows.size(); i3++) {
                                    WindowManagerPolicy.WindowState win = this.mVisibleWindows.valueAt(i3).get();
                                    if (win != null) {
                                        pw.println("            " + win);
                                    }
                                }
                            }
                        }
                        SimpleDateFormat formater = new SimpleDateFormat(SmartPowerSettings.TIME_FORMAT_PATTERN);
                        ArrayList<StateChangeRecord> history = getHistoryInfos(System.currentTimeMillis() - SmartPowerSettings.MAX_HISTORY_REPORT_DURATION);
                        pw.println("            state change history:" + history.size());
                        for (Iterator<StateChangeRecord> it = history.iterator(); it.hasNext(); it = it) {
                            StateChangeRecord record2 = it.next();
                            pw.println("            " + formater.format(new Date(record2.getTimeStamp())) + ": " + record2);
                            history = history;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes.dex */
            public class StateChangeRecord {
                private int mAdj;
                private String mChangeReason;
                private long mInterval;
                private int mNewState;
                private int mOldState;
                private long mTimeStamp;

                private StateChangeRecord(int oldState, int newState, long intervalMills, String reson, int adj) {
                    this.mOldState = oldState;
                    this.mNewState = newState;
                    this.mInterval = intervalMills;
                    this.mChangeReason = reson;
                    this.mTimeStamp = System.currentTimeMillis();
                    this.mAdj = adj;
                }

                /* JADX INFO: Access modifiers changed from: private */
                public long getTimeStamp() {
                    return this.mTimeStamp;
                }

                public String toString() {
                    return String.format("%s->%s(%dms) R(%s) adj=%s.", AppStateManager.processStateToString(this.mOldState), AppStateManager.processStateToString(this.mNewState), Long.valueOf(this.mInterval), this.mChangeReason, Integer.valueOf(this.mAdj));
                }
            }

            /* loaded from: classes.dex */
            private class AppPowerResourceCallback implements AppPowerResource.IAppPowerResourceCallback {
                private AppPowerResourceCallback() {
                }

                @Override // com.miui.server.smartpower.AppPowerResource.IAppPowerResourceCallback
                public void noteResourceActive(int type, int behavier) {
                    String reason = "resource active  uid:" + RunningProcess.this.mUid + " pid:" + RunningProcess.this.mPid + " " + AppPowerResourceManager.typeToString(type);
                    if (AppStateManager.DEBUG) {
                        Slog.d(RunningProcess.this.LOG_TAG, reason);
                    }
                    if (type == 1) {
                        RunningProcess.this.mAudioActive = true;
                        RunningProcess.this.mAudioBehavier = behavier;
                    } else if (type == 3) {
                        RunningProcess.this.mGpsActive = true;
                        RunningProcess.this.mGpsBehavier = behavier;
                    } else if (type == 2) {
                        RunningProcess.this.mNetActive = true;
                        RunningProcess.this.mNetBehavier = behavier;
                    } else if (type == 5) {
                        RunningProcess.this.mBleActive = true;
                    } else if (type == 6) {
                        RunningProcess.this.mCameraActive = true;
                        RunningProcess.this.mCamBehavier = behavier;
                    } else if (type == 7) {
                        RunningProcess.this.mDisplayActive = true;
                        RunningProcess.this.mDisplayBehavier = behavier;
                    }
                    AppState.this.updateCurrentResourceBehavier();
                    RunningProcess.this.becomeActiveIfNeeded(reason);
                }

                @Override // com.miui.server.smartpower.AppPowerResource.IAppPowerResourceCallback
                public void noteResourceInactive(int type, int behavier) {
                    String reason = "resource inactive  uid:" + RunningProcess.this.mUid + " pid:" + RunningProcess.this.mPid + " " + AppPowerResourceManager.typeToString(type);
                    if (AppStateManager.DEBUG) {
                        Slog.d(RunningProcess.this.LOG_TAG, reason);
                    }
                    if (type == 1) {
                        RunningProcess.this.mAudioActive = false;
                        RunningProcess.this.mAudioBehavier = 0;
                    } else if (type == 3) {
                        RunningProcess.this.mGpsActive = false;
                        RunningProcess.this.mGpsBehavier = 0;
                    } else if (type == 5) {
                        RunningProcess.this.mBleActive = false;
                    } else if (type == 2) {
                        AppState.this.mHandler.post(new Runnable() { // from class: com.android.server.am.AppStateManager.AppState.RunningProcess.AppPowerResourceCallback.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppStateManager.this.mAppPowerResourceManager.unRegisterCallback(2, RunningProcess.this.mResourcesCallback, RunningProcess.this.mUid);
                            }
                        });
                        RunningProcess.this.mNetActive = false;
                        RunningProcess.this.mNetBehavier = 0;
                    } else if (type == 6) {
                        RunningProcess.this.mCameraActive = false;
                        RunningProcess.this.mCamBehavier = 0;
                    } else if (type == 7) {
                        RunningProcess.this.mDisplayActive = false;
                        RunningProcess.this.mDisplayBehavier = 0;
                    }
                    AppState.this.updateCurrentResourceBehavier();
                    RunningProcess.this.sendBecomeInactiveMsg(reason);
                }
            }

            /* loaded from: classes.dex */
            public class ProcessPriorityScore {
                private static final long APP_ACTIVE_THRESHOLD_HIGH = 600000;
                private static final long APP_ACTIVE_THRESHOLD_LOW = 3600000;
                private static final long APP_ACTIVE_THRESHOLD_MODERATE = 1800000;
                private static final int MAX_APP_WEEK_LAUNCH_COUNT = 21;
                private static final int MINIMUM_MEMORY_GROWTH_THRESHOLD = 102400;
                private static final long MIN_APP_FOREGROUND_EVENT_DURATION = 5000;
                private static final int PROCESS_ACTIVE_LEVEL_CRITICAL = 1;
                private static final int PROCESS_ACTIVE_LEVEL_HEAVY = 0;
                private static final int PROCESS_ACTIVE_LEVEL_LOW = 3;
                private static final int PROCESS_ACTIVE_LEVEL_MODERATE = 2;
                private static final int PROCESS_MEM_LEVEL_CRITICAL = 3;
                private static final int PROCESS_MEM_LEVEL_HIGH = 4;
                private static final int PROCESS_MEM_LEVEL_LOW = 1;
                private static final int PROCESS_MEM_LEVEL_MODERATE = 2;
                public static final int PROCESS_PRIORITY_ACTIVE_USAGE_FACTOR = 100;
                public static final int PROCESS_PRIORITY_LEVEL_UNKNOWN = 1000;
                public static final int PROCESS_PRIORITY_MEM_FACTOR = 1;
                public static final int PROCESS_PRIORITY_TYPE_FACTOR = 10;
                private static final int PROCESS_TYPE_LEVEL_MAINPROC_HAS_ACTIVITY = 1;
                private static final int PROCESS_TYPE_LEVEL_NO_ACTIVITY = 3;
                private static final int PROCESS_TYPE_LEVEL_SUBPROC_HAS_ACTIVITY = 2;
                private static final int PROCESS_USAGE_ACTIVE_LEVEL_DEFAULT = 9;
                private static final int PROCESS_USAGE_LEVEL_AUTOSTART = 4;
                public static final int PROCESS_USAGE_LEVEL_CRITICAL = 5;
                public static final int PROCESS_USAGE_LEVEL_LOW = 7;
                public static final int PROCESS_USAGE_LEVEL_MODERATE = 6;
                private final long MEMORY_GROWTH_THRESHOLD;
                private final long TOTAL_MEMEORY_KB;
                private int mPriorityScore;
                private int mUsageLevel;

                public ProcessPriorityScore() {
                    long totalMemory = Process.getTotalMemory() / FormatBytesUtil.KB;
                    this.TOTAL_MEMEORY_KB = totalMemory;
                    this.MEMORY_GROWTH_THRESHOLD = Math.max(totalMemory / 100, 102400L);
                }

                /* JADX INFO: Access modifiers changed from: private */
                public void updatePriorityScore() {
                    int typeLevel = computeTypeLevel();
                    int memLevel = computeMemUsageLevel();
                    int activeUsageLevel = computeActiveAndUsageStatLevel();
                    this.mPriorityScore = (memLevel * 1) + (typeLevel * 10) + (activeUsageLevel * 100);
                }

                private int computeActiveAndUsageStatLevel() {
                    this.mUsageLevel = computeUsageStatLevel();
                    if (AppState.this.mIsLockedApp || AppStateManager.this.mSmartPowerPolicyManager.isDependedProcess(RunningProcess.this.mPid)) {
                        return 0;
                    }
                    int activeLevel = computeAppActiveLevel();
                    return Math.min(this.mUsageLevel, activeLevel);
                }

                private int computeAppActiveLevel() {
                    if (AppState.this.mIsLockedApp || AppStateManager.this.mSmartPowerPolicyManager.isDependedProcess(RunningProcess.this.mPid)) {
                        return 0;
                    }
                    long lastTopTime = RunningProcess.this.mProcessRecord.mState.getLastTopTime();
                    long appTopDuration = lastTopTime - RunningProcess.this.mProcessRecord.mState.getLastSwitchToTopTime();
                    long backgroundTime = SystemClock.uptimeMillis() - lastTopTime;
                    int activeLevel = 9;
                    if (appTopDuration > MIN_APP_FOREGROUND_EVENT_DURATION) {
                        if (backgroundTime <= 600000) {
                            activeLevel = 1;
                        } else if (backgroundTime <= 1800000) {
                            activeLevel = 2;
                        } else if (backgroundTime <= 3600000) {
                            activeLevel = 3;
                        }
                    }
                    if (activeLevel == 9 && AppState.this.mIsAutoStartApp) {
                        return 4;
                    }
                    return activeLevel;
                }

                private int computeUsageStatLevel() {
                    SmartPowerPolicyManager.UsageStatsInfo usageStats = AppStateManager.this.mSmartPowerPolicyManager.getUsageStatsInfo(RunningProcess.this.mPackageName);
                    if (usageStats == null) {
                        return 9;
                    }
                    long lastTimeVisible = Math.max(RunningProcess.this.mProcessRecord.mState.getLastTopTime(), usageStats.getLastTimeVisible());
                    boolean recentlyLunch = System.currentTimeMillis() - lastTimeVisible < 86400000;
                    boolean recentlyUsage = usageStats.getAppLaunchCount() > 21;
                    if (recentlyLunch && recentlyUsage) {
                        return 5;
                    }
                    if (recentlyLunch) {
                        return 6;
                    }
                    if (!recentlyUsage) {
                        return 9;
                    }
                    return 7;
                }

                private int computeTypeLevel() {
                    if (!RunningProcess.this.mProcessRecord.hasActivities()) {
                        return 3;
                    }
                    if (!RunningProcess.this.mProcessName.equals(RunningProcess.this.mPackageName)) {
                        return 2;
                    }
                    return 1;
                }

                private int computeMemUsageLevel() {
                    int memLevel;
                    if (RunningProcess.this.mLastPss >= SmartPowerSettings.PROC_MEM_LVL3_PSS_LIMIT_KB) {
                        memLevel = 4;
                    } else if (RunningProcess.this.mLastPss >= SmartPowerSettings.PROC_MEM_LVL2_PSS_LIMIT_KB) {
                        memLevel = 3;
                    } else if (RunningProcess.this.mLastPss >= SmartPowerSettings.PROC_MEM_LVL1_PSS_LIMIT_KB) {
                        memLevel = 2;
                    } else {
                        memLevel = 1;
                    }
                    long initialIdlePss = RunningProcess.this.mProcessRecord.mProfile.getInitialIdlePss();
                    if (memLevel == 1 && initialIdlePss != 0 && RunningProcess.this.mLastPss > (3 * initialIdlePss) / 2 && RunningProcess.this.mLastPss > this.MEMORY_GROWTH_THRESHOLD + initialIdlePss) {
                        return 3;
                    }
                    return memLevel;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class DependProcInfo {
            private int mPid;
            private String mProcessName;
            private int mUid;

            private DependProcInfo(int uid, int pid, String processName) {
                this.mUid = -1;
                this.mPid = -1;
                this.mUid = uid;
                this.mPid = pid;
                this.mProcessName = processName;
            }

            public String toString() {
                return this.mProcessName + "(" + this.mUid + "/" + this.mPid + ")";
            }
        }
    }

    /* loaded from: classes.dex */
    private class PowerFrozenCallback implements PowerFrozenManager.IFrozenReportCallback {
        private PowerFrozenCallback() {
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void reportSignal(int uid, int pid, long now) {
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void reportNet(int uid, long now) {
            String reason = "receive net request: uid[" + uid + "]";
            AppStateManager.this.onReportPowerFrozenSignal(uid, reason);
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) {
            if (isOneway && !AppStateManager.this.isProcessPerceptible(callerUid, callerPid)) {
                return;
            }
            String reason = "receive binder trans: dstUid[" + dstUid + "] dstPid[" + dstPid + "] callerUid[" + callerUid + "] callerPid[" + callerPid + "] callerTid[" + callerTid + "] isOneway[" + isOneway + "] code[" + buffer + "]";
            AppStateManager.this.onReportPowerFrozenSignal(dstUid, dstPid, reason);
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void serviceReady(boolean ready) {
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
            switch (binderState) {
                case 0:
                case 1:
                default:
                    return;
                case 2:
                case 3:
                case 4:
                    String reason = "receive binder state: uid[" + uid + "] pid[" + pid + "] tid[" + tid + "]";
                    AppStateManager.this.onReportPowerFrozenSignal(uid, pid, reason);
                    return;
            }
        }

        @Override // com.miui.server.smartpower.PowerFrozenManager.IFrozenReportCallback
        public void thawedByOther(int uid, int pid) {
            String reason = "receive other thaw request: uid[" + uid + "]";
            AppStateManager.this.onReportPowerFrozenSignal(uid, pid, reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String reason = (String) msg.obj;
                    synchronized (AppStateManager.this.mAppLock) {
                        for (int i = 0; i < AppStateManager.this.mActiveApps.size(); i++) {
                            AppState app = AppStateManager.this.mActiveApps.valueAt(i);
                            app.hibernateAllIfNeeded(AppStateManager.REASON_HIBERNATE_ALL + reason);
                        }
                    }
                    return;
                case 2:
                    AppState fromApp = (AppState) msg.obj;
                    Bundle bundle = msg.getData();
                    String reason2 = bundle.getString(EdgeSuppressionManager.EdgeSuppressionHandler.MSG_DATA_REASON);
                    ArrayList<AppState> targetApps = new ArrayList<>();
                    synchronized (AppStateManager.this.mAppLock) {
                        for (int i2 = 0; i2 < AppStateManager.this.mActiveApps.size(); i2++) {
                            AppState app2 = AppStateManager.this.mActiveApps.valueAt(i2);
                            if (app2 != fromApp && !Process.isCoreUid(app2.mUid) && app2.getCurrentState() >= 3 && app2.getCurrentState() <= 5) {
                                targetApps.add(app2);
                            }
                        }
                    }
                    Iterator<AppState> it = targetApps.iterator();
                    while (it.hasNext()) {
                        AppState app3 = it.next();
                        app3.hibernateAllInactive(AppStateManager.REASON_HIBERNATE_ALL + reason2);
                    }
                    return;
                case 3:
                    int userId = ((Integer) msg.obj).intValue();
                    synchronized (AppStateManager.this.mAppLock) {
                        ArrayList<Integer> targerUids = new ArrayList<>();
                        for (int i3 = 0; i3 < AppStateManager.this.mActiveApps.size(); i3++) {
                            AppState app4 = AppStateManager.this.mActiveApps.valueAt(i3);
                            if (UserHandle.getUserId(app4.mUid) == userId && !app4.isAlive()) {
                                targerUids.add(Integer.valueOf(app4.mUid));
                            }
                        }
                        Iterator<Integer> it2 = targerUids.iterator();
                        while (it2.hasNext()) {
                            int uid = it2.next().intValue();
                            AppStateManager.this.mActiveApps.remove(uid);
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private boolean isFrozenForUid(int uid) {
        return this.mPowerFrozenManager.isFrozenForUid(uid);
    }

    public boolean registerAppStateListener(IProcessStateCallback callback) {
        synchronized (this.mProcessStateCallbacks) {
            if (this.mProcessStateCallbacks.contains(callback)) {
                return false;
            }
            this.mProcessStateCallbacks.add(callback);
            return true;
        }
    }

    public void unRegisterAppStateListener(IProcessStateCallback callback) {
        synchronized (this.mProcessStateCallbacks) {
            this.mProcessStateCallbacks.remove(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskRecord {
        int mLastUid;
        int mTaskId;
        String mLastPkg = "";
        final ArraySet<AppState.RunningProcess> mProcesses = new ArraySet<>();

        TaskRecord(int taskId) {
            this.mTaskId = taskId;
        }

        int getTaskId() {
            return this.mTaskId;
        }

        void addProcess(AppState.RunningProcess process) {
            this.mProcesses.add(process);
            this.mLastUid = process.getUid();
            this.mLastPkg = process.getProcessName();
        }

        boolean isProcessInTask(int uid, int pid) {
            Iterator<AppState.RunningProcess> it = this.mProcesses.iterator();
            while (it.hasNext()) {
                AppState.RunningProcess proc = it.next();
                if (proc.getPid() == pid) {
                    return true;
                }
            }
            return false;
        }

        boolean isProcessInTask(String prcName) {
            Iterator<AppState.RunningProcess> it = this.mProcesses.iterator();
            while (it.hasNext()) {
                AppState.RunningProcess proc = it.next();
                if (proc.getProcessName().equals(prcName)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return "TaskRecord{" + this.mTaskId + " " + this.mLastUid + " " + this.mLastPkg + " " + this.mProcesses;
        }

        void dump(PrintWriter pw, String[] args, int opti) {
            pw.println("    Task:" + this.mTaskId + " " + this.mLastUid + " " + this.mLastPkg);
            Iterator<AppState.RunningProcess> it = this.mProcesses.iterator();
            while (it.hasNext()) {
                AppState.RunningProcess proc = it.next();
                pw.println("        " + proc);
            }
        }
    }

    /* loaded from: classes.dex */
    class CurrentTaskStack {
        private final Stack<TaskRecord> mTaskRecords = new Stack<>();
        private TaskRecord mTopTask;

        CurrentTaskStack() {
        }

        void updateIfNeeded(int taskId, AppState.RunningProcess proc, int launchedFromUid, String launchedFromPkg) {
            synchronized (this.mTaskRecords) {
                TaskRecord taskRecord = this.mTopTask;
                if (taskRecord == null || taskRecord.mTaskId != taskId) {
                    TaskRecord taskRecord2 = this.mTopTask;
                    if (taskRecord2 != null && launchedFromUid != taskRecord2.mLastUid && !this.mTopTask.mLastPkg.equals(launchedFromPkg)) {
                        this.mTaskRecords.clear();
                    }
                    TaskRecord taskRecord3 = new TaskRecord(taskId);
                    this.mTopTask = taskRecord3;
                    this.mTaskRecords.push(taskRecord3);
                }
                TaskRecord taskRecord4 = this.mTopTask;
                if (taskRecord4 != null) {
                    taskRecord4.addProcess(proc);
                }
            }
        }

        boolean isProcessInTaskStack(int uid, int pid) {
            synchronized (this.mTaskRecords) {
                Iterator<TaskRecord> it = this.mTaskRecords.iterator();
                while (it.hasNext()) {
                    TaskRecord task = it.next();
                    if (task.isProcessInTask(uid, pid)) {
                        return true;
                    }
                }
                return false;
            }
        }

        boolean isProcessInTaskStack(String prcName) {
            synchronized (this.mTaskRecords) {
                Iterator<TaskRecord> it = this.mTaskRecords.iterator();
                while (it.hasNext()) {
                    TaskRecord task = it.next();
                    if (task.isProcessInTask(prcName)) {
                        return true;
                    }
                }
                return false;
            }
        }

        void dump(PrintWriter pw, String[] args, int opti) {
            pw.println("#tasks");
            synchronized (this.mTaskRecords) {
                Iterator<TaskRecord> it = this.mTaskRecords.iterator();
                while (it.hasNext()) {
                    TaskRecord task = it.next();
                    task.dump(pw, args, opti);
                }
            }
        }
    }
}
