package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.location.ILocationListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.window.TransitionInfo;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.AppStateManager;
import com.android.server.am.PendingIntentRecord;
import com.android.server.display.mode.DisplayModeDirector;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.smartpower.AppPowerResourceManager;
import com.miui.server.smartpower.ComponentsManager;
import com.miui.server.smartpower.IAppState;
import com.miui.server.smartpower.PowerFrozenManager;
import com.miui.server.smartpower.SmartBoostPolicyManager;
import com.miui.server.smartpower.SmartCpuPolicyManager;
import com.miui.server.smartpower.SmartDisplayPolicyManager;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.miui.server.smartpower.SmartScenarioManager;
import com.miui.server.smartpower.SmartThermalPolicyManager;
import com.miui.server.smartpower.SmartWindowPolicyManager;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.security.CallerInfo;
import miui.smartpower.IScenarioCallback;
import miui.smartpower.ISmartPowerManager;
import miui.smartpower.MultiTaskActionManager;
import miui.smartpower.SmartPowerManager;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.miui.server.smartpower.SmartPowerServiceStub$$")
/* loaded from: classes.dex */
public class SmartPowerService extends ISmartPowerManager.Stub implements SmartPowerServiceStub, SmartPowerServiceInternal {
    private static final String CLOUD_ALARM_WHITE_LIST = "power_alarm_white_list";
    private static final String CLOUD_BROADCAST_WHITE_LIST = "power_broadcast_white_list";
    private static final String CLOUD_CONTROL_KEY = "perf_shielder_smartpower";
    private static final String CLOUD_CONTROL_MOUDLE = "perf_smartpower";
    private static final String CLOUD_DISPLAY_ENABLE = "perf_power_display_enable";
    private static final String CLOUD_ENABLE = "perf_power_appState_enable";
    private static final String CLOUD_FROZEN_ENABLE = "perf_power_freeze_enable";
    private static final String CLOUD_GAME_PAY_PROTECT_ENABLE = "perf_game_pay_protect_enable";
    private static final String CLOUD_INTERCEPT_ENABLE = "perf_power_intercept_enable";
    private static final String CLOUD_PKG_WHITE_LIST = "power_pkg_white_list";
    private static final String CLOUD_PROC_WHITE_LIST = "power_proc_white_list";
    private static final String CLOUD_PROVIDER_WHITE_LIST = "power_provider_white_list";
    private static final String CLOUD_SCREENON_WHITE_LIST = "power_screenon_white_list";
    private static final String CLOUD_SERVICE_WHITE_LIST = "power_service_white_list";
    private static final String CLOUD_WINDOW_ENABLE = "perf_power_window_enable";
    public static final boolean DEBUG = SmartPowerSettings.DEBUG_ALL;
    public static final String SERVICE_NAME = "smartpower";
    public static final String TAG = "SmartPower";
    public static final int THREAD_GROUP_FOREGROUND = 1;
    private ActivityManagerService mAMS;
    private ActivityTaskManagerService mATMS;
    private Context mContext;
    private MyHandler mHandler;
    private PackageManagerInternal mPackageManager;
    private SmartCpuPolicyManager mSmartCpuPolicyManager;
    private WindowManagerService mWMS;
    private PowerFrozenManager mPowerFrozenManager = null;
    private AppStateManager mAppStateManager = null;
    private SmartPowerPolicyManager mSmartPowerPolicyManager = null;
    private AppPowerResourceManager mAppPowerResourceManager = null;
    private ComponentsManager mComponentsManager = null;
    private SmartScenarioManager mSmartScenarioManager = null;
    private SmartThermalPolicyManager mSmartThermalPolicyManager = null;
    private SmartDisplayPolicyManager mSmartDisplayPolicyManager = null;
    private SmartWindowPolicyManager mSmartWindowPolicyManager = null;
    private SmartBoostPolicyManager mSmartBoostPolicyManager = null;
    private HandlerThread mHandlerTh = new HandlerThread(SmartPowerPolicyManager.REASON, -2);
    private boolean mSystemReady = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SmartPowerService> {

        /* compiled from: SmartPowerService$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SmartPowerService INSTANCE = new SmartPowerService();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SmartPowerService m682provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SmartPowerService m681provideNewInstance() {
            return new SmartPowerService();
        }
    }

    SmartPowerService() {
        LocalServices.addService(SmartPowerServiceInternal.class, this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
        }
    }

    void systemReady(Context context) {
        this.mContext = context;
        this.mHandlerTh.start();
        this.mAMS = ActivityManager.getService();
        this.mATMS = ActivityTaskManager.getService();
        this.mWMS = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        this.mPackageManager = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mHandler = new MyHandler(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
        this.mPowerFrozenManager = new PowerFrozenManager();
        this.mSmartPowerPolicyManager = new SmartPowerPolicyManager(context, this.mHandlerTh.getLooper(), this.mAMS, this.mPowerFrozenManager);
        this.mAppPowerResourceManager = new AppPowerResourceManager(context, this.mHandlerTh.getLooper());
        this.mAppStateManager = new AppStateManager(context, this.mHandlerTh.getLooper(), this.mAMS, this.mPowerFrozenManager);
        this.mComponentsManager = new ComponentsManager(context, this.mHandlerTh.getLooper());
        this.mSmartCpuPolicyManager = new SmartCpuPolicyManager(context, this.mAMS);
        SmartScenarioManager smartScenarioManager = new SmartScenarioManager(context, this.mHandlerTh.getLooper());
        this.mSmartScenarioManager = smartScenarioManager;
        this.mSmartThermalPolicyManager = new SmartThermalPolicyManager(smartScenarioManager);
        this.mSmartDisplayPolicyManager = new SmartDisplayPolicyManager(context, this.mWMS);
        this.mSmartWindowPolicyManager = new SmartWindowPolicyManager(context, this.mHandlerTh.getLooper(), this.mWMS);
        this.mSmartBoostPolicyManager = new SmartBoostPolicyManager(context, this.mHandlerTh.getLooper());
        this.mAppStateManager.init(this.mAppPowerResourceManager, this.mSmartPowerPolicyManager, this.mSmartScenarioManager);
        this.mAppPowerResourceManager.init();
        this.mSmartPowerPolicyManager.init(this.mAppStateManager, this.mAppPowerResourceManager);
        this.mComponentsManager.init(this.mAppStateManager, this.mSmartPowerPolicyManager);
        this.mSmartCpuPolicyManager.init(this.mAppStateManager, this.mSmartPowerPolicyManager);
        this.mSmartDisplayPolicyManager.init(this.mSmartScenarioManager, this.mSmartWindowPolicyManager);
        this.mSmartBoostPolicyManager.init();
        this.mSmartWindowPolicyManager.init(this.mSmartDisplayPolicyManager, this.mSmartBoostPolicyManager);
        registerCloudObserver(this.mContext);
        this.mSystemReady = true;
        this.mSmartDisplayPolicyManager.systemReady();
        this.mSmartBoostPolicyManager.systemReady();
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final SmartPowerService mService;

        public Lifecycle(Context context) {
            super(context);
            SmartPowerService smartPowerService = (SmartPowerService) SmartPowerServiceStub.getInstance();
            this.mService = smartPowerService;
            smartPowerService.systemReady(context);
        }

        public void onStart() {
            publishBinderService(SmartPowerService.SERVICE_NAME, this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 1000) {
                this.mService.updateCloudControlParas();
            }
        }
    }

    private boolean checkPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid < 10000) {
            return true;
        }
        return this.mAppStateManager.isSystemApp(callingUid);
    }

    public void registThermalScenarioCallback(IScenarioCallback callback) {
        if (this.mSystemReady) {
            if (!checkPermission()) {
                String msg = "Permission Denial: registThermalScenarioCallback from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w("SmartPower", msg);
            } else {
                this.mSmartThermalPolicyManager.registThermalScenarioCallback(callback);
            }
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isUidIdle(int uid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isUidIdle(uid);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void playbackStateChanged(int uid, int pid, int oldState, int newState) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.playbackStateChanged(uid, pid, oldState, newState);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void recordAudioFocus(int uid, int pid, String clientId, boolean request) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.recordAudioFocus(uid, pid, clientId, request);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void recordAudioFocusLoss(int uid, String clientId, int focusLoss) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.recordAudioFocusLoss(uid, clientId, focusLoss);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onPlayerTrack(int uid, int pid, int piid, int sessionId) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerTrack(uid, pid, piid, sessionId);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onPlayerRlease(int uid, int pid, int piid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerRlease(uid, pid, piid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onPlayerEvent(int uid, int pid, int piid, int event) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerEvent(uid, pid, piid, event);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRecorderTrack(int uid, int pid, int riid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderTrack(uid, pid, riid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRecorderRlease(int uid, int riid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderRlease(uid, riid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRecorderEvent(int uid, int riid, int event) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderEvent(uid, riid, event);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onExternalAudioRegister(int uid, int pid) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onExternalAudioRegister(uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void uidAudioStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.uidAudioStatusChanged(uid, active);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void uidVideoStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.uidVideoStatusChanged(uid, active);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onProcessStart(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.processStartedLocked(r, r.info.packageName);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onProcessKill(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.processKilledLocked(r);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void updateProcess(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.updateProcessLocked(r);
        }
    }

    public void setProcessPss(ProcessRecord r, long pss, long swapPss) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.setProcessPss(r, pss, swapPss);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onActivityStartUnchecked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart) {
        if (this.mSystemReady) {
            this.mComponentsManager.activityStartBeforeLocked(name, launchedFromPid, uid, packageName, isColdStart);
            SystemPressureControllerStub.getInstance().activityStartBeforeLocked(new ControllerActivityInfo(uid, pid, packageName, launchedFromUid, launchedFromPid, launchedFromPackage, isColdStart));
            this.mSmartDisplayPolicyManager.notifyActivityStartUnchecked(name, uid, pid, packageName, launchedFromUid, launchedFromPid, launchedFromPackage, isColdStart);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onForegroundActivityChangedLocked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart, int taskId, int callingUid, String callingPkg) {
        if (this.mSystemReady) {
            this.mComponentsManager.onForegroundActivityChangedLocked(launchedFromPid, uid, pid, name, taskId, callingUid, callingPkg);
            SystemPressureControllerStub.getInstance().foregroundActivityChangedLocked(new ControllerActivityInfo(uid, pid, packageName, launchedFromUid, launchedFromPid, launchedFromPackage, isColdStart));
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onActivityReusmeUnchecked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart) {
        if (this.mSystemReady) {
            SystemPressureControllerStub.getInstance().activityResumeUnchecked(new ControllerActivityInfo(uid, pid, packageName, launchedFromUid, launchedFromPid, launchedFromPackage, isColdStart));
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onBroadcastStatusChanged(ProcessRecord processRecord, boolean active, Intent intent) {
        if (this.mSystemReady && processRecord != null) {
            this.mComponentsManager.broadcastStatusChangedLocked(processRecord, active, intent);
        }
    }

    public void onServiceStatusChanged(ProcessRecord processRecord, boolean active, ServiceRecord record, int execution) {
        if (this.mSystemReady && processRecord != null) {
            if (active && execution == 2) {
                for (ArrayList<ConnectionRecord> connList : record.getConnections().values()) {
                    Iterator<ConnectionRecord> it = connList.iterator();
                    while (it.hasNext()) {
                        ConnectionRecord conn = it.next();
                        onServiceConnectionChanged(conn.binding, true);
                    }
                }
            }
            this.mComponentsManager.serviceStatusChangedLocked(processRecord, active, record.shortInstanceName, execution);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean shouldInterceptProvider(int callingUid, ProcessRecord callingProc, ProcessRecord hostingProc, boolean isRunning) {
        if (this.mSystemReady && hostingProc != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptProviderLocked(callingUid, callingProc, hostingProc, isRunning);
        }
        return false;
    }

    public void onContentProviderStatusChanged(int callingUid, ProcessRecord callingProc, ProcessRecord hostingProc, ContentProviderRecord record, boolean isRunning) {
        if (this.mSystemReady && hostingProc != null) {
            this.mComponentsManager.contentProviderStatusChangedLocked(hostingProc, record.toString());
        }
    }

    public void onServiceConnectionChanged(AppBindRecord bindRecord, boolean isConnect) {
        if (this.mSystemReady && bindRecord != null && bindRecord.client != null && bindRecord.service != null && bindRecord.intent != null) {
            if (!isConnect && bindRecord.intent.apps.containsKey(bindRecord.client)) {
                return;
            }
            ProcessRecord serviceApp = bindRecord.service.app;
            if (serviceApp == null) {
                int serviceFlag = bindRecord.service.serviceInfo.flags;
                if ((serviceFlag & 2) != 0) {
                    serviceApp = bindRecord.service.isolationHostProc;
                } else if (bindRecord.service.appInfo != null) {
                    serviceApp = this.mAMS.getProcessRecordLocked(bindRecord.service.processName, bindRecord.service.appInfo.uid);
                }
            }
            if (serviceApp != null && serviceApp.mPid > 0) {
                this.mComponentsManager.serviceConnectionChangedLocked(bindRecord.client, serviceApp, isConnect, bindRecord.intent.collectFlags());
            }
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onProviderConnectionChanged(ContentProviderConnection conn, boolean isConnect) {
        if (this.mSystemReady && conn != null && conn.client != null && conn.provider != null && conn.provider.proc != null) {
            this.mComponentsManager.providerConnectionChangedLocked(conn.client, conn.provider.proc, isConnect);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onAlarmStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mComponentsManager.alarmStatusChangedLocked(uid, active);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean shouldInterceptAlarm(int uid, int type) {
        if (this.mSystemReady) {
            return this.mSmartPowerPolicyManager.shouldInterceptAlarmLocked(uid, type);
        }
        return false;
    }

    public boolean shouldInterceptBroadcast(ProcessRecord recProc, BroadcastRecord record) {
        if (this.mSystemReady && recProc != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptBroadcastLocked(record.callingUid, record.callingPid, record.callerPackage, recProc, record.intent, record.ordered, record.sticky);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean skipFrozenAppAnr(ApplicationInfo info, int uid, String report) {
        if (this.mSystemReady && info != null) {
            return this.mSmartPowerPolicyManager.skipFrozenAppAnr(info, uid, report);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean shouldInterceptService(Intent service, CallerInfo callingInfo, ServiceInfo serviceInfo) {
        if (this.mSystemReady && callingInfo != null && serviceInfo != null && serviceInfo.applicationInfo != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptService(service, callingInfo, serviceInfo);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void updateActivitiesVisibility() {
        if (this.mSystemReady) {
            this.mComponentsManager.updateActivitiesVisibilityLocked();
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void powerFrozenServiceReady(boolean ready) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.serviceReady(ready);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void reportSignal(int uid, int pid, long now) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.reportSignal(uid, pid, now);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void reportNet(int uid, long now) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.reportNet(uid, now);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, buffer);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.reportBinderState(uid, pid, tid, binderState, now);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void addFrozenPid(int uid, int pid) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.addFrozenPid(uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void thawedByOther(int uid, int pid) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.thawedByOther(uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void removeFrozenPid(int uid, int pid) {
        if (this.mSystemReady) {
            this.mPowerFrozenManager.removeFrozenPid(uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onActivityVisibilityChanged(int uid, String processName, ActivityRecord record, boolean visible) {
        if (this.mSystemReady) {
            this.mComponentsManager.activityVisibilityChangedLocked(uid, processName, record, visible);
        }
    }

    public void onActivityLaunched(int uid, String processName, ActivityRecord record, int launchState) {
        MiProcessTracker.getInstance().onActivityLaunched(processName, record.packageName, uid, launchState);
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onWindowVisibilityChanged(int uid, int pid, WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
        if (this.mSystemReady) {
            this.mComponentsManager.windowVisibilityChangedLocked(uid, pid, win, attrs, visible);
            this.mSmartDisplayPolicyManager.notifyWindowVisibilityChangedLocked(win, attrs, visible);
        }
    }

    public void onHoldScreenUidChanged(int uid, boolean hold) {
        if (this.mSystemReady) {
            this.mAppStateManager.onHoldScreenUidChanged(uid, hold);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void applyOomAdjLocked(ProcessRecord app) {
        if (this.mSystemReady && app != null) {
            this.mComponentsManager.onApplyOomAdjLocked(app);
            SystemPressureControllerStub.getInstance().onApplyOomAdjLocked(app);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onSendPendingIntent(PendingIntentRecord.Key key, String callingPkg, String targetPkg, Intent intent) {
        if (this.mSystemReady) {
            int uid = this.mPackageManager.getPackageUid(targetPkg, 0L, key.userId);
            this.mComponentsManager.onSendPendingIntent(uid, key.typeName());
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onPendingPackagesExempt(String packageName) {
        List<AppStateManager.AppState> appStates = this.mAppStateManager.getAppState(packageName);
        for (AppStateManager.AppState appState : appStates) {
            this.mComponentsManager.onSendPendingIntent(appState.getUid(), "exempt");
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onMediaKey(int uid, int pid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onMediaKey(uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onMediaKey(int uid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onMediaKey(uid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onWallpaperComponentChanged(boolean active, int uid, String packageName) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onWallpaperComponentChangedLocked(active, uid, packageName);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onVpnChanged(boolean active, int uid, String packageName) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onVpnChanged(active, uid, packageName);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onBackupChanged(boolean active, ProcessRecord proc) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onBackupChanged(active, proc.uid, proc.info.packageName);
            this.mAppStateManager.onBackupChanged(active, proc);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onBackupServiceAppChanged(boolean active, int uid, int pid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onBackupServiceAppChanged(active, uid, pid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onUsbStateChanged(boolean isConnected, long functions, Intent intent) {
        if (this.mSystemReady) {
            boolean isUsbDataTrans = isUsbDataTransferActive(functions);
            this.mSmartPowerPolicyManager.onUsbStateChanged(isConnected, isUsbDataTrans);
        }
    }

    protected static boolean isUsbDataTransferActive(long functions) {
        return ((4 & functions) == 0 && (16 & functions) == 0) ? false : true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onInputMethodShow(int uid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onInputMethodShow(uid);
        }
    }

    public void onAcquireLocation(int uid, int pid, ILocationListener listener) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onAquireLocation(uid, pid, listener);
        }
    }

    public void onReleaseLocation(int uid, int pid, ILocationListener listener) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onReleaseLocation(uid, pid, listener);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onAcquireWakelock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onAcquireWakelock(lock, flags, tag, ownerUid, ownerPid);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onReleaseWakelock(IBinder lock, int flags) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onReleaseWakelock(lock, flags);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, String pkg, int data) {
        if (this.mSystemReady) {
            if (uid > 0) {
                if (pid > 0) {
                    this.mAppPowerResourceManager.onBluetoothEvent(isConnect, bleType, uid, pid, data);
                    return;
                }
                AppStateManager.AppState appState = this.mAppStateManager.getAppState(uid);
                if (appState == null) {
                    return;
                }
                ArrayList<IAppState.IRunningProcess> runningProcs = appState.getRunningProcessList();
                for (int i = 0; i < runningProcs.size(); i++) {
                    this.mAppPowerResourceManager.onBluetoothEvent(isConnect, bleType, uid, runningProcs.get(i).getPid(), data);
                }
                return;
            }
            if (!TextUtils.isEmpty(pkg)) {
                List<AppStateManager.AppState> appStates = this.mAppStateManager.getAppState(pkg);
                for (AppStateManager.AppState appState2 : appStates) {
                    int realUid = appState2.getUid();
                    ArrayList<IAppState.IRunningProcess> runningProcs2 = appState2.getRunningProcessList();
                    for (int i2 = 0; i2 < runningProcs2.size(); i2++) {
                        this.mAppPowerResourceManager.onBluetoothEvent(isConnect, bleType, realUid, runningProcs2.get(i2).getPid(), data);
                    }
                }
            }
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.reportTrackStatus(uid, pid, sessionId, isMuted);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void notifyCameraForegroundState(String cameraId, boolean isForeground, String caller, int callerUid, int callerPid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.notifyCameraForegroundState(cameraId, isForeground, caller, callerUid, callerPid);
        }
    }

    private void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.SmartPowerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri())) {
                    SmartPowerService.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudControlParas() {
        String data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUD_CONTROL_MOUDLE, CLOUD_CONTROL_KEY, "");
        if (TextUtils.isEmpty(data)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(data);
            if (jsonObject.has(CLOUD_FROZEN_ENABLE)) {
                boolean frozenEnable = jsonObject.optBoolean(CLOUD_FROZEN_ENABLE);
                this.mPowerFrozenManager.syncCloudControlSettings(frozenEnable);
                try {
                    SystemProperties.set(SmartPowerSettings.FROZEN_PROP, String.valueOf(frozenEnable));
                    logCloudControlParas(CLOUD_FROZEN_ENABLE, Boolean.valueOf(frozenEnable));
                } catch (JSONException e) {
                    e = e;
                    Slog.e("SmartPower", "updateCloudData error :", e);
                    return;
                }
            }
            if (jsonObject.has(CLOUD_ENABLE)) {
                String appStateEnable = jsonObject.optString(CLOUD_ENABLE);
                SystemProperties.set(SmartPowerSettings.APP_STATE_PROP, appStateEnable);
                logCloudControlParas(CLOUD_ENABLE, appStateEnable);
            }
            if (jsonObject.has(CLOUD_INTERCEPT_ENABLE)) {
                String interceptEnable = jsonObject.optString(CLOUD_INTERCEPT_ENABLE);
                SystemProperties.set(SmartPowerSettings.INTERCEPT_PROP, interceptEnable);
                SmartPowerSettings.PROP_INTERCEPT_ENABLE = Boolean.parseBoolean(interceptEnable);
                logCloudControlParas(CLOUD_INTERCEPT_ENABLE, interceptEnable);
            }
            if (jsonObject.has(CLOUD_DISPLAY_ENABLE)) {
                String dipslayEnable = jsonObject.optString(CLOUD_DISPLAY_ENABLE);
                SystemProperties.set(SmartPowerSettings.DISPLAY_POLICY_PROP, dipslayEnable);
                logCloudControlParas(CLOUD_DISPLAY_ENABLE, dipslayEnable);
            }
            if (jsonObject.has(CLOUD_WINDOW_ENABLE)) {
                String windowEnable = jsonObject.optString(CLOUD_WINDOW_ENABLE);
                SystemProperties.set(SmartPowerSettings.WINDOW_POLICY_PROP, windowEnable);
                logCloudControlParas(CLOUD_WINDOW_ENABLE, windowEnable);
            }
            if (jsonObject.has(CLOUD_GAME_PAY_PROTECT_ENABLE)) {
                String gamePayProtectEnable = jsonObject.optString(CLOUD_GAME_PAY_PROTECT_ENABLE);
                SystemProperties.set(SmartPowerSettings.GAME_PAY_PROTECT, gamePayProtectEnable);
                SmartPowerSettings.GAME_PAY_PROTECT_ENABLED = Boolean.parseBoolean(gamePayProtectEnable);
                logCloudControlParas(CLOUD_GAME_PAY_PROTECT_ENABLE, gamePayProtectEnable);
            }
            String pkgWhiteListString = jsonObject.optString(CLOUD_PKG_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudPackageWhiteList(pkgWhiteListString);
            logCloudControlParas(CLOUD_PKG_WHITE_LIST, pkgWhiteListString);
            String procWhiteListString = jsonObject.optString(CLOUD_PROC_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudProcessWhiteList(procWhiteListString);
            logCloudControlParas(CLOUD_PROC_WHITE_LIST, procWhiteListString);
            String screenonWhiteListString = jsonObject.optString(CLOUD_SCREENON_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudSreenonWhiteList(screenonWhiteListString);
            logCloudControlParas(CLOUD_SCREENON_WHITE_LIST, screenonWhiteListString);
            String broadcastWhiteListString = jsonObject.optString(CLOUD_BROADCAST_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudBroadcastWhiteLit(broadcastWhiteListString);
            logCloudControlParas(CLOUD_BROADCAST_WHITE_LIST, broadcastWhiteListString);
            String alarmWhiteListString = jsonObject.optString(CLOUD_ALARM_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudAlarmWhiteLit(alarmWhiteListString);
            logCloudControlParas(CLOUD_ALARM_WHITE_LIST, alarmWhiteListString);
            String providerWhiteListString = jsonObject.optString(CLOUD_PROVIDER_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudProviderWhiteLit(providerWhiteListString);
            logCloudControlParas(CLOUD_PROVIDER_WHITE_LIST, providerWhiteListString);
            String serviceWhiteListString = jsonObject.optString(CLOUD_SERVICE_WHITE_LIST);
            this.mSmartPowerPolicyManager.updateCloudServiceWhiteLit(serviceWhiteListString);
            logCloudControlParas(CLOUD_SERVICE_WHITE_LIST, serviceWhiteListString);
        } catch (JSONException e2) {
            e = e2;
        }
    }

    private void logCloudControlParas(String key, Object data) {
        Slog.d("SmartPower", "sync cloud control " + key + " " + data);
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public ArrayList<IAppState> getAllAppState() {
        if (this.mSystemReady) {
            return this.mAppStateManager.getAllAppState();
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public ArrayList<IAppState> updateAllAppUsageStats() {
        if (this.mSystemReady) {
            return this.mAppStateManager.updateAllAppUsageStats();
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public IAppState getAppState(int uid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.getAppState(uid);
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public ArrayList<IAppState.IRunningProcess> getLruProcesses() {
        if (this.mSystemReady) {
            return this.mAppStateManager.getLruProcesses();
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public ArrayList<IAppState.IRunningProcess> getLruProcesses(int uid, String packageName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.getLruProcesses(uid, packageName);
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public IAppState.IRunningProcess getRunningProcess(int uid, String processName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.getRunningProcess(uid, processName);
        }
        return null;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void hibernateAllIfNeeded(String reason) {
        if (this.mSystemReady) {
            this.mAppStateManager.hibernateAllIfNeeded(reason);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isUidVisible(int uid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isUidVisible(uid);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isProcessPerceptible(int uid, String procName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isProcessPerceptible(uid, procName);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isProcessInTaskStack(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isProcessInTaskStack(uid, pid);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isProcessInTaskStack(String prcName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isProcessInTaskStack(prcName);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isProcessWhiteList(int flag, String packageName, String procName) {
        if (this.mSystemReady) {
            return this.mSmartPowerPolicyManager.isProcessWhiteList(flag, packageName, procName);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isAppAudioActive(int uid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, 1);
        }
        return true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isAppGpsActive(int uid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, 3);
        }
        return true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isAppAudioActive(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, pid, 1);
        }
        return true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public long getLastMusicPlayTimeStamp(int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.getLastMusicPlayTimeStamp(pid);
        }
        return 0L;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean isAppGpsActive(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, pid, 3);
        }
        return true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public long updateCpuStatsNow(boolean isReset) {
        if (this.mSystemReady) {
            return this.mSmartCpuPolicyManager.updateCpuStatsNow(isReset);
        }
        return 0L;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public int getBackgroundCpuCoreNum() {
        if (this.mSystemReady) {
            return this.mSmartCpuPolicyManager.getBackgroundCpuCoreNum();
        }
        return 4;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean checkSystemSignature(int uid) {
        AppStateManager.AppState appState = this.mAppStateManager.getAppState(uid);
        if (appState != null) {
            return appState.isSystemSignature();
        }
        return false;
    }

    protected void registerAppStateListener(AppStateManager.IProcessStateCallback callback) {
        if (this.mSystemReady) {
            this.mAppStateManager.registerAppStateListener(callback);
        }
    }

    protected void unRegisterAppStateListener(AppStateManager.IProcessStateCallback callback) {
        if (this.mSystemReady) {
            this.mAppStateManager.unRegisterAppStateListener(callback);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onCpuPressureEvents(int level) {
        if (this.mSystemReady) {
            this.mSmartCpuPolicyManager.cpuPressureEvents(level);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onCpuExceptionEvents(int type) {
        if (this.mSystemReady) {
            this.mSmartCpuPolicyManager.cpuExceptionEvents(type);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void notifyMultiSenceEnable(boolean enable) {
        if (this.mSystemReady) {
            this.mSmartBoostPolicyManager.setMultiSenceEnable(enable);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onAppTransitionStartLocked(long animDuration) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyAppTransitionStartLocked(animDuration);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onWindowAnimationStartLocked(long animDuration, SurfaceControl animationLeash) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyWindowAnimationStartLocked(animDuration, animationLeash);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onInsetAnimationShow(int type) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyInsetAnimationShow(type);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onInsetAnimationHide(int type) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyInsetAnimationHide(type);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRecentsAnimationStart(ActivityRecord targetActivity) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRecentsAnimationStart(targetActivity);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRecentsAnimationEnd() {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRecentsAnimationEnd();
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRemoteAnimationStart(RemoteAnimationTarget[] appTargets) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRemoteAnimationStart(appTargets);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onRemoteAnimationEnd() {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRemoteAnimationEnd();
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onFocusedWindowChangeLocked(String oldPackage, String newPackage) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyFocusedWindowChangeLocked(oldPackage, newPackage);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onDisplayDeviceStateChangeLocked(int deviceState) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyDisplayDeviceStateChangeLocked(deviceState);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public void onDisplaySwitchResolutionLocked(int width, int height, int density) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyDisplaySwitchResolutionLocked(width, height, density);
        }
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean shouldInterceptUpdateDisplayModeSpecs(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
        if (this.mSystemReady) {
            return this.mSmartDisplayPolicyManager.shouldInterceptUpdateDisplayModeSpecs(displayId, modeSpecs);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean registerMultiTaskActionListener(int listenerFlag, Handler handler, SmartPowerServiceInternal.MultiTaskActionListener listener) {
        if (this.mSystemReady) {
            return this.mSmartWindowPolicyManager.registerMultiTaskActionListener(listenerFlag, handler, listener);
        }
        return false;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal
    public boolean unregisterMultiTaskActionListener(int listenerFlag, SmartPowerServiceInternal.MultiTaskActionListener listener) {
        if (this.mSystemReady) {
            return this.mSmartWindowPolicyManager.unregisterMultiTaskActionListener(listenerFlag, listener);
        }
        return false;
    }

    public void onMultiTaskActionStart(MultiTaskActionManager.ActionInfo actionInfo) {
        if (this.mSystemReady) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            if (!checkPermission()) {
                String msg = "Permission Denial: onMultiTaskActionStart from pid=" + callingPid + ", uid=" + callingUid;
                Slog.w("SmartPower", msg);
            } else {
                this.mSmartWindowPolicyManager.onMultiTaskActionStart(actionInfo, callingPid);
            }
        }
    }

    public void onMultiTaskActionEnd(MultiTaskActionManager.ActionInfo actionInfo) {
        if (this.mSystemReady) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            if (!checkPermission()) {
                String msg = "Permission Denial: onMultiTaskActionEnd from pid=" + callingPid + ", uid=" + callingUid;
                Slog.w("SmartPower", msg);
            } else {
                this.mSmartWindowPolicyManager.onMultiTaskActionEnd(actionInfo, callingPid);
            }
        }
    }

    public void onTransitionAnimateStart(int type, TransitionInfo info) {
        if (this.mSystemReady) {
            if (!checkPermission()) {
                String msg = "Permission Denial: onTransitionAnimateStart from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w("SmartPower", msg);
            } else {
                String typeString = SmartPowerManager.transitionAnimateTypeToString(type);
                Trace.asyncTraceBegin(32L, "Transit: " + typeString, 0);
            }
        }
    }

    public void onTransitionAnimateEnd(int type) {
        if (this.mSystemReady) {
            if (!checkPermission()) {
                String msg = "Permission Denial: onTransitionAnimateEnd from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w("SmartPower", msg);
            } else {
                String typeString = SmartPowerManager.transitionAnimateTypeToString(type);
                Trace.asyncTraceEnd(32L, "Transit: " + typeString, 0);
            }
        }
    }

    public void onTransitionStart(TransitionInfo info) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyTransitionStart(info);
        }
    }

    public void onTransitionEnd(int syncId, int type, int flags) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyTransitionEnd(syncId, type, flags);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, "SmartPower", pw)) {
            pw.println("smart power (smartpower):");
            try {
                if (0 < args.length) {
                    String parm = args[0];
                    int opti = 0 + 1;
                    if (parm.contains("policy")) {
                        this.mSmartPowerPolicyManager.dump(pw, args, opti);
                    } else if (parm.contains("appstate")) {
                        this.mAppStateManager.dump(pw, args, opti);
                    } else if (parm.contains("fz")) {
                        this.mPowerFrozenManager.dump(pw, args, opti);
                    } else if (parm.contains("scene")) {
                        this.mSmartScenarioManager.dump(pw, args, opti);
                    } else if (parm.contains("display")) {
                        this.mSmartDisplayPolicyManager.dump(pw, args, opti);
                    } else if (parm.contains(DumpSysInfoUtil.WINDOW)) {
                        this.mSmartWindowPolicyManager.dump(pw, args, opti);
                    } else if (parm.equals("set")) {
                        dumpSettings(pw, args, opti);
                        dumpConfig(pw);
                    } else if (parm.equals("stack")) {
                        this.mAppStateManager.dumpStack(pw, args, opti);
                    } else if (parm.equals("-a")) {
                        dumpConfig(pw);
                        this.mSmartPowerPolicyManager.dump(pw, args, opti);
                        pw.println("");
                        this.mAppStateManager.dump(pw, args, opti);
                        pw.println("");
                        this.mSmartScenarioManager.dump(pw, args, opti);
                        pw.println("");
                        this.mPowerFrozenManager.dump(pw, args, opti);
                        pw.println("");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void dumpSettings(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 < args.length) {
            int opti2 = opti + 1;
            String key = args[opti];
            int i = opti2 + 1;
            String value = args[opti2];
            if (key.contains("fz")) {
                if (value.equals("true") || value.equals("false")) {
                    SystemProperties.set(SmartPowerSettings.FROZEN_PROP, value);
                }
            } else if (key.contains("intercept") && (value.equals("true") || value.equals("false"))) {
                SystemProperties.set(SmartPowerSettings.INTERCEPT_PROP, value);
                SmartPowerSettings.PROP_INTERCEPT_ENABLE = Boolean.parseBoolean(value);
            }
        }
    }

    private void dumpConfig(PrintWriter pw) {
        pw.println("smartpower config:");
        pw.println("  appstate(" + SmartPowerSettings.APP_STATE_ENABLE + ") fz(" + SmartPowerSettings.PROP_FROZEN_ENABLE + ") intercept(" + SmartPowerSettings.PROP_INTERCEPT_ENABLE + ")");
        pw.println("");
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new SmartPowerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class SmartPowerShellCommand extends ShellCommand {
        SmartPowerService mService;

        SmartPowerShellCommand(SmartPowerService service) {
            this.mService = service;
        }

        public int onCommand(String cmd) {
            FileDescriptor fd = getOutFileDescriptor();
            PrintWriter pw = getOutPrintWriter();
            String[] args = getAllArgs();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                this.mService.dump(fd, pw, args);
                return -1;
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("smart power (smartpower) commands:");
            pw.println();
            pw.println("  policy");
            pw.println("  appstate");
            pw.println("  fz");
            pw.println();
        }
    }
}
