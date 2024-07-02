package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.Signature;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.app.IPerfShielder;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.am.ProcessPolicy;
import com.android.server.camera.CameraOpt;
import com.android.server.display.DisplayManagerServiceStub;
import com.android.server.power.stats.BatteryStatsManagerStub;
import com.android.server.wm.FgActivityChangedInfo;
import com.android.server.wm.FgWindowChangedInfo;
import com.android.server.wm.ForegroundInfoManager;
import com.android.server.wm.RealTimeModeControllerStub;
import com.android.server.wm.WindowProcessUtils;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.PerfShielderService;
import com.miui.server.migard.MiGardInternal;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.rtboost.SchedBoostManagerInternal;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.app.backup.BackupManager;
import miui.enterprise.ApplicationHelperStub;
import miui.enterprise.EnterpriseManagerStub;
import miui.os.Build;
import miui.process.ActiveUidInfo;
import miui.process.ForegroundInfo;
import miui.process.IActivityChangeListener;
import miui.process.IForegroundInfoListener;
import miui.process.IForegroundWindowListener;
import miui.process.IMiuiApplicationThread;
import miui.process.IPreloadCallback;
import miui.process.LifecycleConfig;
import miui.process.PreloadProcessData;
import miui.process.ProcessCloudData;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.process.ProcessManagerNative;
import miui.process.RunningProcessInfo;

/* loaded from: classes.dex */
public class ProcessManagerService extends ProcessManagerNative {
    private static final String BINDER_MONITOR_FD_PATH = "/proc/mi_log/binder_delay";
    private static final boolean DEBUG = true;
    public static final String DEVICE = Build.DEVICE.toLowerCase();
    static final int MAX_PROCESS_CONFIG_HISTORY = 30;
    static final int RESTORE_AI_PROCESSES_INFO_MSG = 1;
    static final int SKIP_PRELOAD_COUNT_LIMIT = 2;
    private static final String TAG = "ProcessManager";
    static final int USER_OWNER = 0;
    static final int USER_XSPACE = 999;
    private AccessibilityManager mAccessibilityManager;
    private AppOpsManager mAppOpsManager;
    private FileWriter mBinderDelayWriter;
    private Context mContext;
    private DisplayManagerInternal mDisplayManagerInternal;
    private ForegroundInfoManager mForegroundInfoManager;
    final MainHandler mHandler;
    private final ProcessManagerInternal mInternal;
    private MiuiApplicationThreadManager mMiuiApplicationThreadManager;
    private IPerfShielder mPerfService;
    private PackageManager mPkms;
    private PreloadAppControllerImpl mPreloadAppController;
    private ProcessKiller mProcessKiller;
    private ProcessPolicy mProcessPolicy;
    private ProcessStarter mProcessStarter;
    private SchedBoostManagerInternal mSchedBoostService;
    final ServiceThread mServiceThread;
    private SmartPowerServiceInternal mSmartPowerService;
    private Set<Signature> mSystemSignatures;
    final ProcessConfig[] mProcessConfigHistory = new ProcessConfig[30];
    int mHistoryNext = -1;
    private INotificationManager mNotificationManager = NotificationManager.getService();
    private ActivityManagerService mActivityManagerService = ActivityManagerNative.getDefault();
    private ArrayList<ProcessRecord> mLruProcesses = this.mActivityManagerService.mProcessList.getLruProcessesLOSP();

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final ProcessManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ProcessManagerService(context);
        }

        public void onStart() {
            publishBinderService("ProcessManager", this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 1000) {
                SystemPressureController.getInstance().mProcessCleaner.onBootPhase();
            }
        }
    }

    public ProcessManagerService(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mPkms = this.mContext.getPackageManager();
        ServiceThread serviceThread = new ServiceThread("ProcessManager", 0, false);
        this.mServiceThread = serviceThread;
        serviceThread.start();
        MainHandler mainHandler = new MainHandler(serviceThread.getLooper());
        this.mHandler = mainHandler;
        PreloadAppControllerImpl preloadAppControllerImpl = PreloadAppControllerImpl.getInstance();
        this.mPreloadAppController = preloadAppControllerImpl;
        preloadAppControllerImpl.init(this.mActivityManagerService, this, serviceThread);
        this.mProcessKiller = new ProcessKiller(this.mActivityManagerService);
        this.mProcessPolicy = new ProcessPolicy(this, this.mActivityManagerService, this.mAccessibilityManager, serviceThread);
        this.mProcessStarter = new ProcessStarter(this, this.mActivityManagerService, mainHandler);
        BatteryStatsManagerStub.getInstance().setActiveCallback(this.mProcessPolicy);
        this.mMiuiApplicationThreadManager = new MiuiApplicationThreadManager(this.mActivityManagerService);
        this.mForegroundInfoManager = new ForegroundInfoManager(this);
        systemReady();
        LocalService localService = new LocalService();
        this.mInternal = localService;
        LocalServices.addService(ProcessManagerInternal.class, localService);
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        probeCgroupVersion();
    }

    public ProcessManagerInternal getInternal() {
        return this.mInternal;
    }

    protected void systemReady() {
        PerfShielderService asInterface = IPerfShielder.Stub.asInterface(ServiceManager.getService(PerfShielderService.SERVICE_NAME));
        this.mPerfService = asInterface;
        if (asInterface != null) {
            asInterface.systemReady();
        }
        this.mProcessPolicy.systemReady(this.mContext);
        this.mProcessStarter.systemReady();
        MiProcessTracker.getInstance().systemReady(this.mContext);
        try {
            this.mBinderDelayWriter = new FileWriter(BINDER_MONITOR_FD_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (ProcessManagerService.this.mActivityManagerService) {
                        ProcessManagerService.this.mProcessStarter.restoreLastProcessesInfoLocked(msg.arg1);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void shutdown() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessPolicy getProcessPolicy() {
        return this.mProcessPolicy;
    }

    public ProcessKiller getProcessKiller() {
        return this.mProcessKiller;
    }

    public boolean kill(ProcessConfig config) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.kill() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            return false;
        }
        addConfigToHistory(config);
        this.mProcessPolicy.resetWhiteList(this.mContext, UserHandle.getCallingUserId());
        boolean success = SystemPressureController.getInstance().kill(config);
        ProcessRecordStub.get().reportAppPss();
        return success;
    }

    public boolean killAllBackgroundExceptLocked(ProcessConfig config) {
        if (config.isPriorityInvalid()) {
            String msg = "priority:" + config.getPriority() + " is invalid";
            Slog.w("ProcessManager", msg);
            return false;
        }
        int maxProcState = config.getPriority();
        String killReason = TextUtils.isEmpty(config.getReason()) ? getKillReason(config.getPolicy()) : config.getReason();
        synchronized (this.mActivityManagerService) {
            ArrayList<ProcessRecord> procs = new ArrayList<>();
            ProcessList processList = this.mActivityManagerService.mProcessList;
            int NP = processList.getProcessNamesLOSP().getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) processList.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (app.isRemoved() || (((maxProcState < 0 || app.mState.getSetProcState() > maxProcState) && app.mState.hasShownUi()) || DisplayManagerServiceStub.getInstance().isInResolutionSwitchBlackList(app.processName))) {
                        procs.add(app);
                    }
                }
            }
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ProcessRecord app2 = procs.get(i);
                if (!DisplayManagerServiceStub.getInstance().isInResolutionSwitchProtectList(app2.processName)) {
                    this.mProcessKiller.forceStopPackage(app2, killReason, true);
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInWhiteList(ProcessRecord app, int userId, int policy) {
        if (app.uid == UserHandle.getAppId(1002) || app.info.packageName.contains("com.android.cts") || app.info.packageName.contains("android.devicepolicy.cts")) {
            return true;
        }
        int flags = this.mProcessPolicy.getPolicyFlags(policy);
        if (EnterpriseSettings.ENTERPRISE_ACTIVATED || EnterpriseManagerStub.ENTERPRISE_ACTIVATED) {
            flags |= 4096;
        }
        switch (policy) {
            case 1:
            case 3:
            case 4:
            case 5:
            case 6:
            case 14:
            case 16:
            case 22:
                flags = 5;
                if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
                    flags = 5 | 4096;
                }
                if (isPackageInList(app.info.packageName, flags) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isLockedApplication(app.info.packageName, userId) || this.mProcessPolicy.isProcessImportant(app) || this.mProcessPolicy.isFastBootEnable(app.info.packageName, app.info.uid, true)) {
                    return true;
                }
                break;
            case 2:
            case 19:
            case 20:
                break;
            case 7:
                int flags2 = 5;
                if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
                    flags2 = 5 | 4096;
                }
                return isPackageInList(app.info.packageName, flags2) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isProcessImportant(app) || this.mProcessPolicy.isFastBootEnable(app.info.packageName, app.info.uid, true);
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 15:
            case 17:
            default:
                return false;
            case 18:
                return this.mProcessPolicy.isInDisplaySizeWhiteList(app.processName);
            case 21:
                return isPackageInList(app.info.packageName, flags) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isProcessImportant(app) || this.mProcessPolicy.isInExtraPackageList(app.info.packageName) || this.mProcessPolicy.isInSystemCleanWhiteList(app.processName);
        }
        return isPackageInList(app.info.packageName, flags) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isProcessImportant(app);
    }

    protected String getKillReason(ProcessConfig config) {
        int policy = config.getPolicy();
        if (policy == 10 && !TextUtils.isEmpty(config.getReason())) {
            return config.getReason();
        }
        return getKillReason(policy);
    }

    private String getKillReason(int policy) {
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
            case 15:
                return IProcessPolicy.REASON_AUTO_LOCK_OFF_CLEAN;
            case 16:
                return IProcessPolicy.REASON_AUTO_SYSTEM_ABNORMAL_CLEAN;
            case 17:
                return IProcessPolicy.REASON_AUTO_LOCK_OFF_CLEAN_BY_PRIORITY;
            case 18:
                return IProcessPolicy.REASON_DISPLAY_SIZE_CHANGED;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkPermission() {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        if (callingUid < 10000) {
            return true;
        }
        ProcessRecord app = getProcessRecordByPid(callingPid);
        return isSystemApp(app);
    }

    boolean checkSystemSignature() {
        int callingUid = Binder.getCallingUid();
        return this.mSmartPowerService.checkSystemSignature(callingUid);
    }

    public void updateApplicationLockedState(String packageName, int userId, boolean isLocked) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateApplicationLockedState() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mProcessPolicy.updateApplicationLockedState(this.mContext, userId, packageName, isLocked);
    }

    public List<String> getLockedApplication(int userId) {
        return this.mProcessPolicy.getLockedApplication(userId);
    }

    public boolean isLockedApplication(String packageName, int userId) throws RemoteException {
        return this.mProcessPolicy.isLockedApplication(packageName, userId);
    }

    public boolean skipCurrentProcessInBackup(ProcessRecord app, String packageName, int userId) {
        BackupManager backupManager = BackupManager.getBackupManager(this.mContext);
        if (backupManager.getState() != 0) {
            String curRunningPkg = backupManager.getCurrentRunningPackage();
            if ((!TextUtils.isEmpty(packageName) && packageName.equals(curRunningPkg)) || (app != null && app.getThread() != null && app.getPkgList().containsKey(curRunningPkg) && app.userId == userId)) {
                Log.i("ProcessManager", "skip kill:" + (app != null ? app.processName : packageName) + " for Backup app");
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isForceStopEnable(ProcessRecord app, int policy) {
        if (policy == 13) {
            return true;
        }
        return (Build.IS_INTERNATIONAL_BUILD || isSystemApp(app) || isAllowAutoStart(app.info.packageName, app.info.uid) || isPackageInList(app.info.packageName, 34)) ? false : true;
    }

    public boolean isTrimMemoryEnable(String packageName) {
        return !isPackageInList(packageName, 16);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAllowAutoStart(String packageName, int uid) {
        int mode = this.mAppOpsManager.checkOpNoThrow(10008, uid, packageName);
        return mode == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAppDisabledForPkms(String packageName) {
        boolean isSuspend = this.mPkms.isPackageSuspendedForUser(packageName, UserHandle.myUserId());
        int mode = this.mPkms.getApplicationEnabledSetting(packageName);
        return isSuspend || mode == 2 || mode == 3 || mode == 4;
    }

    private boolean isPackageInList(String packageName, int flags) {
        if (packageName == null) {
            return false;
        }
        if (ApplicationHelperStub.getInstance().isKeepLiveWhiteApp(packageName)) {
            return true;
        }
        List<String> whiteList = this.mProcessPolicy.getWhiteList(flags);
        for (String item : whiteList) {
            if (packageName.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSystemApp(int pid) {
        ProcessRecord processRecord = getProcessRecordByPid(pid);
        if (processRecord != null) {
            return isSystemApp(processRecord);
        }
        return false;
    }

    private boolean isSystemApp(ProcessRecord app) {
        return (app == null || app.info == null || (app.info.flags & 129) == 0) ? false : true;
    }

    private boolean isUidSystem(int uid) {
        return uid % 100000 < 10000;
    }

    private String getPackageNameByPid(int pid) {
        ProcessRecord processRecord = getProcessRecordByPid(pid);
        if (processRecord != null) {
            return processRecord.info.packageName;
        }
        return null;
    }

    public ProcessRecord getProcessRecordByPid(int pid) {
        ProcessRecord processRecord;
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            processRecord = this.mActivityManagerService.mPidsSelfLocked.get(pid);
        }
        return processRecord;
    }

    public ProcessRecord getProcessRecord(String processName, int userId) {
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.processName.equals(processName) && app.userId == userId) {
                    return app;
                }
            }
            return null;
        }
    }

    public ProcessRecord getProcessRecord(String processName) {
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.processName.equals(processName)) {
                    return app;
                }
            }
            return null;
        }
    }

    public List<ProcessRecord> getProcessRecordList(String packageName, int userId) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.getPkgList().containsKey(packageName) && app.userId == userId) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    public List<ProcessRecord> getProcessRecordListByPackageAndUid(String packageName, int uid) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.getPkgList().containsKey(packageName) && app.info.uid == uid) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    public List<ProcessRecord> getProcessRecordByUid(int uid) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.info.uid == uid) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAppHasForegroundServices(ProcessRecord processRecord) {
        boolean hasForegroundServices;
        synchronized (this.mActivityManagerService) {
            hasForegroundServices = processRecord.mServices.hasForegroundServices();
        }
        return hasForegroundServices;
    }

    private void increaseRecordCount(String processName, Map<String, Integer> recordMap) {
        Integer expCount = recordMap.get(processName);
        if (expCount == null) {
            expCount = 0;
        }
        recordMap.put(processName, Integer.valueOf(expCount.intValue() + 1));
    }

    private void reduceRecordCountDelay(final String processName, final Map<String, Integer> recordMap, long delay) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                Integer count = (Integer) recordMap.get(processName);
                if (count == null || count.intValue() <= 0) {
                    return;
                }
                Integer count2 = Integer.valueOf(count.intValue() - 1);
                if (count2.intValue() <= 0) {
                    recordMap.remove(processName);
                } else {
                    recordMap.put(processName, count2);
                }
            }
        }, delay);
    }

    public void updateConfig(ProcessConfig config) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateConfig() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
    }

    public int startProcesses(List<PreloadProcessData> dataList, int startProcessCount, boolean ignoreMemory, int userId, int flag) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.startMutiProcesses() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (dataList == null || dataList.size() == 0) {
            throw new IllegalArgumentException("packageNames cannot be null!");
        }
        if (dataList.size() < startProcessCount) {
            throw new IllegalArgumentException("illegal start number!");
        }
        if (!ignoreMemory && ProcessUtils.isLowMemory()) {
            Slog.w("ProcessManager", "low memory! skip start process!");
            return 0;
        }
        if (startProcessCount <= 0) {
            Slog.w("ProcessManager", "startProcessCount <= 0, skip start process!");
            return 0;
        }
        if (!this.mProcessStarter.isAllowPreloadProcess(dataList, flag)) {
            return 0;
        }
        return this.mProcessStarter.startProcesses(dataList, startProcessCount, ignoreMemory, userId, flag);
    }

    public void foregroundInfoChanged(String foregroundPackageName, ComponentName component, String resultToProc) {
        this.mProcessStarter.foregroundActivityChanged(foregroundPackageName);
        OomAdjusterImpl.getInstance().foregroundInfoChanged(foregroundPackageName, component, resultToProc);
    }

    public boolean protectCurrentProcess(boolean isProtected, int timeout) throws RemoteException {
        final ProcessRecord app = getProcessRecordByPid(Binder.getCallingPid());
        if (app == null || !this.mProcessPolicy.isInAppProtectList(app.info.packageName)) {
            String msg = "Permission Denial: ProcessManager.protectCurrentProcess() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        boolean success = this.mProcessPolicy.protectCurrentProcess(app, isProtected);
        if (isProtected && timeout > 0) {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessManagerService.2
                @Override // java.lang.Runnable
                public void run() {
                    ProcessManagerService.this.mProcessPolicy.protectCurrentProcess(app, false);
                }
            }, timeout);
        }
        return success;
    }

    public void setProcessMaxAdjLock(int userId, ProcessRecord app, int maxAdj, int maxProcState) {
        if (app == null) {
            return;
        }
        if (maxAdj > ProcessManager.LOCKED_MAX_ADJ && this.mProcessPolicy.isLockedApplication(app.info.packageName, userId)) {
            maxAdj = ProcessManager.LOCKED_MAX_ADJ;
            maxProcState = ProcessManager.LOCKED_MAX_PROCESS_STATE;
        }
        app.mState.setMaxAdj(maxAdj);
        IProcessPolicy.setAppMaxProcState(app, maxProcState);
    }

    public void updateCloudData(ProcessCloudData cloudData) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateCloudWhiteList() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (cloudData == null) {
            throw new IllegalArgumentException("cloudData cannot be null!");
        }
        this.mProcessPolicy.updateCloudData(cloudData);
    }

    public void registerForegroundInfoListener(IForegroundInfoListener listener) throws RemoteException {
        if (!checkPermission() && !checkSystemSignature()) {
            String msg = "Permission Denial: ProcessManager.registerForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.i("ProcessManager", "registerForegroundInfoListener, caller=" + Binder.getCallingPid() + ", listener=" + listener);
        this.mForegroundInfoManager.registerForegroundInfoListener(listener);
    }

    public void unregisterForegroundInfoListener(IForegroundInfoListener listener) throws RemoteException {
        if (!checkPermission() && !checkSystemSignature()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterForegroundInfoListener(listener);
    }

    public void registerForegroundWindowListener(IForegroundWindowListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.registerForegroundWindowListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.i("ProcessManager", "registerForegroundWindowListener, caller=" + Binder.getCallingPid() + ", listener=" + listener);
        this.mForegroundInfoManager.registerForegroundWindowListener(listener);
    }

    public void unregisterForegroundWindowListener(IForegroundWindowListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundWindowListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterForegroundWindowListener(listener);
    }

    public void registerActivityChangeListener(List<String> targetPackages, List<String> targetActivities, IActivityChangeListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.registerActivityChangeListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.registerActivityChangeListener(targetPackages, targetActivities, listener);
    }

    public void unregisterActivityChangeListener(IActivityChangeListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterActivityChangeListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterActivityChangeListener(listener);
    }

    public ForegroundInfo getForegroundInfo() throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        return this.mForegroundInfoManager.getForegroundInfo();
    }

    public void addMiuiApplicationThread(IMiuiApplicationThread applicationThread, int pid) throws RemoteException {
        if (Binder.getCallingPid() != pid) {
            String msg = "Permission Denial: ProcessManager.addMiuiApplicationThread() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mMiuiApplicationThreadManager.addMiuiApplicationThread(applicationThread, pid);
    }

    public IMiuiApplicationThread getForegroundApplicationThread() throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getForegroundApplicationThread() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        int pid = WindowProcessUtils.getTopRunningPidLocked();
        return this.mMiuiApplicationThreadManager.getMiuiApplicationThread(pid);
    }

    public void notifyForegroundInfoChanged(final FgActivityChangedInfo fgInfo) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                ProcessManagerService.this.mForegroundInfoManager.notifyForegroundInfoChanged(fgInfo);
            }
        });
    }

    public void notifyForegroundWindowChanged(final FgWindowChangedInfo fgInfo) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                ProcessManagerService.this.mForegroundInfoManager.notifyForegroundWindowChanged(fgInfo);
            }
        });
    }

    public void notifyActivityChanged(final ComponentName curActivityComponent) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                ProcessManagerService.this.mForegroundInfoManager.notifyActivityChanged(curActivityComponent);
                CameraOpt.callMethod("notifyActivityChanged", curActivityComponent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyProcessStarted(ProcessRecord app) {
        MiGardInternal migard = (MiGardInternal) LocalServices.getService(MiGardInternal.class);
        migard.onProcessStart(app.uid, app.getPid(), app.info.packageName, app.callerPackage);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAmsProcessKill(ProcessRecord app, String reason) {
        CameraOpt.callMethod("notifyProcessDied", Integer.valueOf(app.getPid()), Integer.valueOf(app.info.uid), app.info.packageName, app.processName);
        String processName = app.processName;
        if (TextUtils.isEmpty(reason) || TextUtils.isEmpty(processName)) {
            return;
        }
        this.mPreloadAppController.onProcessKilled(reason, processName);
        this.mProcessStarter.recordKillProcessIfNeeded(processName, reason);
        MiGardInternal migard = (MiGardInternal) LocalServices.getService(MiGardInternal.class);
        migard.onProcessKilled(app.uid, app.getPid(), app.info.packageName, reason);
        MiProcessTracker.getInstance().recordAmKillProcess(app, reason);
    }

    public void enableBinderMonitor(final int pid, final int enable) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.6
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ProcessManagerService.this.mBinderDelayWriter.write(pid + " " + enable);
                    ProcessManagerService.this.mBinderDelayWriter.flush();
                } catch (Exception e) {
                    Slog.e("ProcessManager", "error write exception log");
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyProcessDied(ProcessRecord app) {
        if (app == null || app.info == null || app.isolated) {
            return;
        }
        String packageName = app.info.packageName;
        int uid = app.uid;
        String processName = app.processName;
        OomAdjusterImpl.getInstance().notifyProcessDied(app);
        this.mProcessStarter.restartCameraIfNeeded(packageName, processName, uid);
        this.mProcessStarter.recordDiedProcessIfNeeded(packageName, processName, uid);
        MiGardInternal miGardInternal = (MiGardInternal) LocalServices.getService(MiGardInternal.class);
        if (miGardInternal != null) {
            miGardInternal.notifyProcessDied(app.getPid());
        }
        enableBinderMonitor(app.mPid, 0);
        CameraOpt.callMethod("notifyProcessDied", Integer.valueOf(app.getPid()), Integer.valueOf(app.info.uid), app.info.packageName, app.processName);
    }

    public List<RunningProcessInfo> getRunningProcessInfo(int pid, int uid, String packageName, String processName, int userId) throws RemoteException {
        int userId2;
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getRunningProcessInfo() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (userId > 0) {
            userId2 = userId;
        } else {
            userId2 = UserHandle.getCallingUserId();
        }
        List<RunningProcessInfo> processInfoList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            if (pid > 0) {
                ProcessRecord app = getProcessRecordByPid(pid);
                fillRunningProcessInfoList(processInfoList, app);
                return processInfoList;
            }
            if (!TextUtils.isEmpty(processName)) {
                ProcessRecord app2 = getProcessRecord(processName, userId2);
                fillRunningProcessInfoList(processInfoList, app2);
                return processInfoList;
            }
            if (!TextUtils.isEmpty(packageName) && uid > 0) {
                List<ProcessRecord> appList = getProcessRecordListByPackageAndUid(packageName, uid);
                for (ProcessRecord app3 : appList) {
                    fillRunningProcessInfoList(processInfoList, app3);
                }
                return processInfoList;
            }
            if (!TextUtils.isEmpty(packageName)) {
                List<ProcessRecord> appList2 = getProcessRecordList(packageName, userId2);
                for (ProcessRecord app4 : appList2) {
                    fillRunningProcessInfoList(processInfoList, app4);
                }
            }
            if (uid > 0) {
                List<ProcessRecord> appList3 = getProcessRecordByUid(uid);
                for (ProcessRecord app5 : appList3) {
                    fillRunningProcessInfoList(processInfoList, app5);
                }
            }
            return processInfoList;
        }
    }

    private void fillRunningProcessInfoList(List<RunningProcessInfo> infoList, ProcessRecord app) {
        RunningProcessInfo info = generateRunningProcessInfo(app);
        if (info != null && !infoList.contains(info)) {
            infoList.add(info);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RunningProcessInfo generateRunningProcessInfo(ProcessRecord app) {
        RunningProcessInfo info = null;
        if (app != null && app.getThread() != null && !app.mErrorState.isCrashing() && !app.mErrorState.isNotResponding()) {
            info = new RunningProcessInfo();
            info.mProcessName = app.processName;
            info.mPid = app.getPid();
            info.mUid = app.uid;
            info.mAdj = app.mState.getCurAdj();
            info.mProcState = app.mState.getCurProcState();
            info.mHasForegroundActivities = app.mState.hasForegroundActivities();
            info.mHasForegroundServices = app.mServices.hasForegroundServices();
            info.mPkgList = app.getPackageList();
            info.mLocationForeground = info.mHasForegroundServices || (app.mServices.getForegroundServiceTypes() & 8) != 0;
        }
        return info;
    }

    private List<StatusBarNotification> getAppNotificationWithFlag(String packageName, int uid, int flags) {
        List<StatusBarNotification> notifications;
        List<StatusBarNotification> notificationList = new ArrayList<>();
        try {
            ParceledListSlice<StatusBarNotification> notificaionList = this.mNotificationManager.getAppActiveNotifications(packageName, UserHandle.getUserId(uid));
            notifications = notificaionList.getList();
        } catch (RemoteException e) {
        }
        if (notifications != null && !notifications.isEmpty()) {
            for (StatusBarNotification statusBarNotification : notifications) {
                if (statusBarNotification != null && statusBarNotification.getNotification() != null) {
                    Notification notification = statusBarNotification.getNotification();
                    if ((notification.flags & flags) != 0) {
                        notificationList.add(statusBarNotification);
                    }
                }
            }
            return notificationList;
        }
        return notificationList;
    }

    public List<ActiveUidInfo> getActiveUidInfo(int flag) throws RemoteException {
        List<ActiveUidInfo> activeUidInfoList;
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getActiveUidInfo() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        List<ProcessPolicy.ActiveUidRecord> activeUidRecords = this.mProcessPolicy.getActiveUidRecordList(flag);
        SparseArray<ActiveUidInfo> activeUidInfos = new SparseArray<>();
        synchronized (this.mActivityManagerService) {
            for (ProcessPolicy.ActiveUidRecord r : activeUidRecords) {
                List<ProcessRecord> records = getProcessRecordByUid(r.uid);
                for (ProcessRecord app : records) {
                    ActiveUidInfo activeUidInfo = activeUidInfos.get(r.uid);
                    if (activeUidInfo != null) {
                        if (app.mState.getCurAdj() < activeUidInfo.curAdj) {
                            activeUidInfo.curAdj = app.mState.getCurAdj();
                        }
                        if (app.mState.getCurProcState() < activeUidInfo.curProcState) {
                            activeUidInfo.curProcState = app.mState.getCurProcState();
                        }
                    } else {
                        ActiveUidInfo activeUidInfo2 = generateActiveUidInfoLocked(app, r);
                        if (activeUidInfo2 != null) {
                            activeUidInfos.put(r.uid, activeUidInfo2);
                        }
                    }
                }
            }
            activeUidInfoList = new ArrayList<>();
            for (int i = 0; i < activeUidInfos.size(); i++) {
                activeUidInfoList.add(activeUidInfos.valueAt(i));
            }
        }
        return activeUidInfoList;
    }

    private ActiveUidInfo generateActiveUidInfoLocked(ProcessRecord app, ProcessPolicy.ActiveUidRecord activeUidRecord) {
        if (app == null || app.getThread() == null || app.mErrorState.isCrashing() || app.mErrorState.isNotResponding()) {
            return null;
        }
        ActiveUidInfo info = new ActiveUidInfo();
        info.packageName = app.info.packageName;
        info.uid = activeUidRecord.uid;
        info.flag = activeUidRecord.flag;
        info.curAdj = app.mState.getCurAdj();
        info.curProcState = app.mState.getCurProcState();
        info.foregroundServices = app.mServices.hasForegroundServices();
        info.lastBackgroundTime = app.getUidRecord().getLastBackgroundTime();
        info.numProcs = app.getUidRecord().getNumOfProcs();
        info.pkgList = app.getPackageList();
        return info;
    }

    public void registerPreloadCallback(IPreloadCallback callback, int type) {
        this.mPreloadAppController.registerPreloadCallback(callback, type);
    }

    public int startPreloadApp(String packageName, boolean ignoreMemory, boolean sync, LifecycleConfig config) {
        this.mPreloadAppController.preloadAppEnqueue(packageName, ignoreMemory, config);
        return 500;
    }

    public int killPreloadApp(String packageName) {
        return this.mPreloadAppController.killPreloadApp(packageName);
    }

    public boolean frequentlyKilledForPreload(String packageName) {
        return this.mProcessStarter.frequentlyKilledForPreload(packageName);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new ProcessMangaerShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    class ProcessMangaerShellCommand extends ShellCommand {
        ProcessMangaerShellCommand() {
        }

        public int onCommand(String cmd) {
            boolean z;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case 3580:
                        if (cmd.equals("pl")) {
                            z = false;
                            break;
                        }
                    default:
                        z = -1;
                        break;
                }
            } catch (Exception e) {
                pw.println(e);
            }
            switch (z) {
                case false:
                    ProcessManagerService.this.startPreloadApp(getNextArgRequired(), Boolean.parseBoolean(getNextArg()), true, LifecycleConfig.create(Integer.parseInt(getNextArg())));
                    return -1;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Process manager commands:");
            pw.println("  pl PACKAGENAME");
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends ProcessManagerInternal {
        private LocalService() {
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyForegroundInfoChanged(FgActivityChangedInfo info) {
            ProcessManagerService.this.notifyForegroundInfoChanged(info);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isForegroundApp(String pkgName, int uid) {
            return ProcessManagerService.this.mForegroundInfoManager.isForegroundApp(pkgName, uid);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyForegroundWindowChanged(FgWindowChangedInfo info) {
            ProcessManagerService.this.notifyForegroundWindowChanged(info);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyActivityChanged(ComponentName curComponentActivity) {
            ProcessManagerService.this.notifyActivityChanged(curComponentActivity);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyProcessStarted(ProcessRecord app) {
            ProcessManagerService.this.notifyProcessStarted(app);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyAmsProcessKill(ProcessRecord app, String reason) {
            ProcessManagerService.this.notifyAmsProcessKill(app, reason);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyProcessDied(ProcessRecord app) {
            ProcessManagerService.this.notifyProcessDied(app);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void forceStopPackage(String packageName, int userId, String reason) {
            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).forceStopPackage(packageName, userId, reason);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void updateEnterpriseWhiteList(String packageName, boolean isAdd) {
            List<String> whiteList = ProcessManagerService.this.mProcessPolicy.getWhiteList(4096);
            if (isAdd) {
                if (!whiteList.contains(packageName)) {
                    whiteList.add(packageName);
                    ProcessManagerService.this.mProcessPolicy.addWhiteList(4096, whiteList, false);
                    return;
                }
                return;
            }
            if (whiteList.contains(packageName)) {
                whiteList.remove(packageName);
                ProcessManagerService.this.mProcessPolicy.addWhiteList(4096, whiteList, false);
            }
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ApplicationInfo getMultiWindowForegroundAppInfoLocked() {
            return WindowProcessUtils.getMultiWindowForegroundAppInfoLocked(ProcessManagerService.this.mActivityManagerService.mActivityTaskManager);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public IMiuiApplicationThread getMiuiApplicationThread(int pid) {
            return ProcessManagerService.this.mMiuiApplicationThreadManager.getMiuiApplicationThread(pid);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isPackageFastBootEnable(String packageName, int uid, boolean checkPass) {
            return ProcessManagerService.this.mProcessPolicy.isFastBootEnable(packageName, uid, checkPass);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public List<RunningProcessInfo> getAllRunningProcessInfo() {
            List<RunningProcessInfo> processInfoList = new ArrayList<>();
            synchronized (ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked) {
                for (int i = ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked.size() - 1; i >= 0; i--) {
                    ProcessRecord proc = ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked.valueAt(i);
                    if (proc != null) {
                        processInfoList.add(ProcessManagerService.this.generateRunningProcessInfo(proc));
                    }
                }
            }
            return processInfoList;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void setSpeedTestState(boolean state) {
            ProcessManagerService.this.mPreloadAppController.setSpeedTestState(state);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean checkAppFgServices(int pid) {
            ProcessRecord app = getProcessRecordByPid(pid);
            if (app != null) {
                return ProcessManagerService.this.isAppHasForegroundServices(app);
            }
            return false;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isAllowRestartProcessLock(String processName, int flag, int uid, String pkgName, String callerPackage, HostingRecord hostingRecord) {
            return ProcessManagerService.this.mProcessStarter.isAllowRestartProcessLock(processName, flag, uid, pkgName, callerPackage, hostingRecord);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean restartDiedAppOrNot(ProcessRecord app, boolean isHomeApp, boolean allowRestart, boolean fromBinderDied) {
            if (fromBinderDied) {
                return ProcessManagerService.this.mProcessStarter.restartDiedAppOrNot(app, isHomeApp, allowRestart);
            }
            return allowRestart;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyLmkProcessKill(int uid, int oomScore, long rssInBytes, int freeMemKb, int freeSwapKb, int killReason, int thrashing, String processName) {
            MiProcessTracker.getInstance().recordLmkKillProcess(uid, oomScore, rssInBytes, freeMemKb, freeSwapKb, killReason, thrashing, processName);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isTrimMemoryEnable(String packageName) {
            return ProcessManagerService.this.isTrimMemoryEnable(packageName);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isLockedApplication(String packageName, int userId) throws RemoteException {
            return ProcessManagerService.this.isLockedApplication(packageName, userId);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ForegroundInfo getForegroundInfo() throws RemoteException {
            return ProcessManagerService.this.getForegroundInfo();
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ProcessRecord getProcessRecord(String processName, int userId) {
            return ProcessManagerService.this.getProcessRecord(processName, userId);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ProcessRecord getProcessRecord(String processName) {
            return ProcessManagerService.this.getProcessRecord(processName);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ProcessRecord getProcessRecordByPid(int pid) {
            return ProcessManagerService.this.getProcessRecordByPid(pid);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void setProcessMaxAdjLock(int userId, ProcessRecord app, int maxAdj, int maxProcState) {
            ProcessManagerService.this.setProcessMaxAdjLock(userId, app, maxAdj, maxProcState);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void forceStopPackage(ProcessRecord app, String reason, boolean evenForeground) {
            ProcessManagerService.this.getProcessKiller().forceStopPackage(app, reason, evenForeground);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void trimMemory(ProcessRecord app, boolean evenForeground) {
            ProcessManagerService.this.getProcessKiller().trimMemory(app, evenForeground);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void killApplication(ProcessRecord app, String reason, boolean evenForeground) {
            ProcessManagerService.this.getProcessKiller().killApplication(app, reason, evenForeground);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isInWhiteList(ProcessRecord app, int userId, int policy) {
            return ProcessManagerService.this.isInWhiteList(app, userId, policy);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public IProcessPolicy getProcessPolicy() {
            return ProcessManagerService.this.getProcessPolicy();
        }
    }

    private final int ringAdvance(int x, int increment, int ringSize) {
        int x2 = x + increment;
        if (x2 < 0) {
            return ringSize - 1;
        }
        if (x2 >= ringSize) {
            return 0;
        }
        return x2;
    }

    private void addConfigToHistory(ProcessConfig config) {
        config.setKillingClockTime(System.currentTimeMillis());
        int ringAdvance = ringAdvance(this.mHistoryNext, 1, 30);
        this.mHistoryNext = ringAdvance;
        this.mProcessConfigHistory[ringAdvance] = config;
    }

    public void enableHomeSchedBoost(boolean enable) throws RemoteException {
        if (!checkPermission()) {
            Slog.e("ProcessManager", "Permission Denial: can't enable Home Sched Boost");
        }
        SchedBoostManagerInternal si = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        if (si != null) {
            si.enableSchedBoost(enable);
        }
    }

    public void beginSchedThreads(int[] tids, long duration, int pid, int mode) throws RemoteException {
        if (!checkPermission() && !checkSystemSignature() && mode != 3) {
            String msg = "Permission Denial: ProcessManager.beginSchedThreads() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.d("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (this.mSchedBoostService == null) {
            this.mSchedBoostService = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        }
        if (this.mSchedBoostService == null || tids == null || tids.length < 1) {
            return;
        }
        if (pid < 0) {
            pid = Binder.getCallingPid();
        }
        String pkgName = getPackageNameByPid(pid);
        boolean isEnabled = RealTimeModeControllerStub.get().checkCallerPermission(pkgName);
        if (!isEnabled) {
            Slog.d("ProcessManager", "beginSchedThreads is not Enabled");
        } else {
            this.mSchedBoostService.beginSchedThreads(tids, duration, pkgName, mode);
        }
    }

    public void reportGameScene(String packageName, int gameScene, int appState) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.reportGameScene() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.d("ProcessManager", "pms : packageName = " + packageName + " gameScene =" + gameScene + " appState = " + appState);
        OomAdjusterImpl.getInstance().updateGameSceneRecordMap(packageName, gameScene, appState);
    }

    public void notifyBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, String pkg, int data) throws RemoteException {
        this.mSmartPowerService.onBluetoothEvent(isConnect, bleType, uid, pid, pkg, data);
    }

    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) throws RemoteException {
        this.mSmartPowerService.reportTrackStatus(uid, pid, sessionId, isMuted);
    }

    public int getRenderThreadTidByPid(int pid) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateConfig() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Slog.i("ProcessManager", "getRenderThreadTidByPid, caller=" + Binder.getCallingPid());
        if (pid == Process.myPid()) {
            return ProcessListStubImpl.getInstance().getSystemRenderThreadTid();
        }
        ProcessRecord proc = getProcessRecordByPid(pid);
        if (proc != null) {
            return proc.getRenderThreadTid();
        }
        return 0;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ProcessConfig config;
        if (!checkPermission()) {
            pw.println("Permission Denial: can't dump ProcessManager from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Process Config:");
        int lastIndex = this.mHistoryNext;
        int ringIndex = this.mHistoryNext;
        int i = 0;
        while (ringIndex != -1 && (config = this.mProcessConfigHistory[ringIndex]) != null) {
            pw.print("  #");
            pw.print(i);
            pw.print(": ");
            pw.println(config.toString());
            ringIndex = ringAdvance(ringIndex, -1, 30);
            i++;
            if (ringIndex == lastIndex) {
                break;
            }
        }
        this.mForegroundInfoManager.dump(pw, "    ");
        this.mProcessPolicy.dump(pw, "    ");
    }
}
