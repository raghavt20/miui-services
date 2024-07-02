package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.PrivacyTestModeStub;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.app.IPerfShielder;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.os.TransferPipe;
import com.android.internal.os.anr.AnrLatencyTracker;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.android.server.clipboard.ClipboardServiceStub;
import com.android.server.input.padkeyboard.iic.NanoSocketCallback;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.uri.GrantUri;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.ActivityTaskManagerServiceImpl;
import com.android.server.wm.ActivityTaskManagerServiceStub;
import com.android.server.wm.AppResurrectionServiceStub;
import com.android.server.wm.MiuiFreezeStub;
import com.android.server.wm.WindowProcessUtils;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.AccessController;
import com.miui.server.greeze.GreezeManagerInternal;
import com.miui.server.security.AccessControlImpl;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import com.miui.server.stability.StabilityLocalServiceInternal;
import com.miui.server.xspace.XSpaceManagerServiceStub;
import com.miui.whetstone.PowerKeeperPolicy;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import com.xiaomi.vkmode.service.MiuiForceVkServiceInternal;
import database.SlaDbSchema.SlaDbSchema;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import miui.app.StorageRestrictedPathManager;
import miui.drm.DrmBroadcast;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.BootEventManager;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.security.CallerInfo;
import miui.security.SecurityManagerInternal;
import miui.security.WakePathChecker;
import miui.util.font.SymlinkUtils;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;

@MiuiStubHead(manifestName = "com.android.server.am.ActivityManagerServiceStub$$")
/* loaded from: classes.dex */
public class ActivityManagerServiceImpl extends ActivityManagerServiceStub {
    public static final long BOOST_DURATION = 3000;
    private static final String BOOST_TAG = "Boost";
    private static final String CARLINK = "com.miui.carlink";
    private static final String CARLINK_CARLIFE = "com.baidu.carlife.xiaomi";
    private static final String CHROME_PKG = "com.android.chrome";
    private static List<String> JOB_ANRS = null;
    public static final long KEEP_FOREGROUND_DURATION = 20000;
    public static final String MIUI_APP_TAG = "MIUIScout App";
    private static final String MIUI_NOTIFICATION = "com.miui.notification";
    private static final String MIUI_VOICE = "com.miui.voiceassist";
    private static final String MI_PUSH = "com.xiaomi.mipush.sdk.PushMessageHandler";
    private static final String MI_VOICE = "com.miui.voiceassist/com.xiaomi.voiceassistant.VoiceService";
    private static final int PHONE_CARWITH_CASTING = 1;
    private static final String PROP_DISABLE_AUTORESTART_APP_PREFIX = "sys.rescuepartyplus.disable_autorestart.";
    static final int PUSH_SERVICE_WHITELIST_TIMEOUT = 60000;
    public static final int SIGNAL_QUIT = 3;
    private static final String TAG = "ActivityManagerServiceImpl";
    private static final String UCAR_CASTING_STATE = "ucar_casting_state";
    private static final String WEIXIN = "com.tencent.mm";
    public static final List<String> WIDGET_PROVIDER_WHITE_LIST;
    private static final String XIAOMI_BLUETOOTH = "com.xiaomi.bluetooth";
    private static final String XMSF = "com.xiaomi.xmsf";
    private static final HashSet<String> mIgnoreAuthorityList;
    private static Map<String, List<String>> splitDecouplePkgList;
    ActivityManagerService mAmService;
    Context mContext;
    private MiuiForceVkServiceInternal mForceVkInternal;
    private Intent mLastSplitIntent;
    private PackageManagerInternal mPackageManager;
    private IPerfShielder mPerfService;
    private SecurityManagerInternal mSecurityInternal;
    protected SmartPowerServiceInternal mSmartPowerService;
    private Map<Integer, Stack<IBinder>> mSplitActivityEntryStack;
    private Bundle mSplitExtras;
    private StabilityLocalServiceInternal mStabilityLocalServiceInternal;
    boolean mSystemReady;
    private static ArrayList<String> dumpTraceRequestList = new ArrayList<>();
    private static AtomicInteger requestDumpTraceCount = new AtomicInteger(0);
    private static AtomicBoolean dumpFlag = new AtomicBoolean(false);
    private Map<Integer, Intent> mCurrentSplitIntent = new HashMap();
    private final Object splitEntryStackLock = new Object();
    private GreezeManagerInternal greezer = null;
    private ArrayList<Integer> mBackupingList = new ArrayList<>();
    private int mInstrUid = -1;
    private Set<Integer> mUsingVibratorUids = new HashSet();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityManagerServiceImpl> {

        /* compiled from: ActivityManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityManagerServiceImpl INSTANCE = new ActivityManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActivityManagerServiceImpl m303provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActivityManagerServiceImpl m302provideNewInstance() {
            return new ActivityManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        WIDGET_PROVIDER_WHITE_LIST = arrayList;
        arrayList.add("com.android.calendar");
        splitDecouplePkgList = new HashMap<String, List<String>>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1
            {
                put("com.miui.video", new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.1
                    {
                        add("c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8");
                        add("c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025");
                    }
                });
                put(AccessController.PACKAGE_GALLERY, new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.2
                    {
                        add("c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8");
                        add("c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025");
                    }
                });
                put("com.android.deskclock", new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.3
                    {
                        add("c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8");
                        add("c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025");
                    }
                });
                put("com.android.soundrecorder", new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.4
                    {
                        add("c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8");
                        add("c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025");
                    }
                });
                put("com.baidu.input_mi", new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.5
                    {
                        add("a6ef817bfd6c083442a149856e51036f6912c2db6b6009db8127cdd641e295a9");
                    }
                });
                put("com.iflytek.inputmethod.miui", new ArrayList<String>() { // from class: com.android.server.am.ActivityManagerServiceImpl.1.6
                    {
                        add("8e28b81d19efb0065cf017a31f1e86af48c652e4b88cc51e27f592fd3b2c4fc3");
                    }
                });
            }
        };
        HashSet<String> hashSet = new HashSet<>();
        mIgnoreAuthorityList = hashSet;
        hashSet.add("com.miui.securitycenter.zman.fileProvider");
        hashSet.add("com.xiaomi.misettings.FileProvider");
        hashSet.add("com.xiaomi.mirror.remoteprovider");
        hashSet.add("com.xiaomi.aiasst.service.fileProvider");
        hashSet.add("com.miui.bugreport.fileprovider");
        hashSet.add("com.miui.cleanmaster.fileProvider");
        JOB_ANRS = new ArrayList(Arrays.asList("Timed out while trying to bind", "No response to onStartJob", "No response to onStopJob", "required notification not provided"));
    }

    public static ActivityManagerServiceImpl getInstance() {
        return (ActivityManagerServiceImpl) MiuiStubUtil.getImpl(ActivityManagerServiceStub.class);
    }

    void init(ActivityManagerService ams, Context context) {
        this.mAmService = ams;
        this.mContext = context;
        MiuiWarnings.getInstance().init(context);
        BroadcastQueueModernStubImpl.getInstance().init(this.mAmService, this.mContext);
        DumpScoutTraceThread dumpScoutTraceThread = new DumpScoutTraceThread(DumpScoutTraceThread.TAG, this);
        dumpScoutTraceThread.start();
        Slog.i(TAG, "DumpScoutTraceThread begin running.");
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.block_system", false)) {
            Slog.w(TAG, "boot monitor system_watchdog...");
            SystemClock.sleep(Long.MAX_VALUE);
        }
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        this.mStabilityLocalServiceInternal = (StabilityLocalServiceInternal) LocalServices.getService(StabilityLocalServiceInternal.class);
    }

    void onSystemReady() {
        BootEventManager.getInstance().setAmsReady(SystemClock.uptimeMillis());
        this.mSystemReady = true;
        this.mPackageManager = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        try {
            ensureDeviceProvisioned(this.mContext);
        } catch (Exception e) {
            Log.e(TAG, "ensureDeviceProvisioned occurs Exception.", e);
        }
        MemoryStandardProcessControlStub.getInstance().init(this.mContext, this.mAmService);
        MemoryFreezeStub.getInstance().init(this.mContext, this.mAmService);
        ProcessProphetStub.getInstance().init(this.mContext, this.mAmService);
        MiuiFreezeStub.getInstance().init(this.mAmService.mActivityTaskManager);
    }

    private static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private static void ensureDeviceProvisioned(Context context) {
        ComponentName checkEnableName;
        if (!isDeviceProvisioned(context)) {
            PackageManager pm = context.getPackageManager();
            if (!Build.IS_INTERNATIONAL_BUILD) {
                checkEnableName = new ComponentName("com.android.provision", "com.android.provision.activities.DefaultActivity");
            } else {
                checkEnableName = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
            }
            if (pm != null && pm.getComponentEnabledSetting(checkEnableName) == 2) {
                Log.e(TAG, "The device provisioned state is inconsistent,try to restore.");
                Settings.Secure.putInt(context.getContentResolver(), "device_provisioned", 1);
                if (!Build.IS_INTERNATIONAL_BUILD) {
                    ComponentName name = new ComponentName("com.android.provision", "com.android.provision.activities.DefaultActivity");
                    pm.setComponentEnabledSetting(name, 1, 1);
                    Intent intent = new Intent("android.intent.action.MAIN");
                    intent.setComponent(name);
                    intent.addFlags(268435456);
                    intent.addCategory("android.intent.category.HOME");
                    context.startActivity(intent);
                    return;
                }
                Settings.Secure.putInt(context.getContentResolver(), "user_setup_complete", 1);
            }
        }
    }

    public boolean isRestrictBackgroundAction(String localhost, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
        if (getGreezeService() != null) {
            return getGreezeService().isRestrictBackgroundAction(localhost, callerUid, callerPkgName, calleeUid, calleePkgName);
        }
        return true;
    }

    void finishBooting() {
        long bootCompleteTime = SystemClock.uptimeMillis();
        BootEventManager.getInstance().setUIReady(bootCompleteTime);
        BootEventManager.getInstance().setBootComplete(bootCompleteTime);
        XSpaceManagerServiceStub.getInstance().init(this.mContext);
        AppResurrectionServiceStub.getInstance().init(this.mContext);
        DrmBroadcast.getInstance(this.mContext).broadcast();
        Intent intent = new Intent("miui.intent.action.FINISH_BOOTING");
        intent.setFlags(268435456);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
        ActivityTaskManagerServiceImpl.getInstance().updateResizeBlackList(this.mContext);
        if (!ScoutUtils.REBOOT_COREDUMP) {
            ScoutUtils.isLibraryTest();
        }
        StabilityLocalServiceInternal stabilityLocalServiceInternal = this.mStabilityLocalServiceInternal;
        if (stabilityLocalServiceInternal != null) {
            stabilityLocalServiceInternal.initContext(this.mContext);
            this.mStabilityLocalServiceInternal.startMemoryMonitor();
        }
        SystemPressureControllerStub.getInstance().finishBooting();
    }

    void markAmsReady() {
        BootEventManager.getInstance().setAmsReady(SystemClock.uptimeMillis());
    }

    void markUIReady() {
        long bootCompleteTime = SystemClock.uptimeMillis();
        BootEventManager.getInstance().setUIReady(bootCompleteTime);
        BootEventManager.getInstance().setBootComplete(bootCompleteTime);
    }

    void reportBootEvent() {
        BootEventManager.getInstance();
        BootEventManager.reportBootEvent();
    }

    public BroadcastProcessQueue getBroadcastProcessQueue(String processName, int uid) {
        if (this.mAmService.mEnableModernQueue) {
            BroadcastQueue bc = this.mAmService.broadcastQueueForFlags(0);
            return bc.getBroadcastProcessQueue(processName, uid);
        }
        return null;
    }

    public int getAppStartMode(int uid, int defMode, int callingPid, String callingPackage) {
        if (XMSF.equalsIgnoreCase(callingPackage) || MIUI_VOICE.equalsIgnoreCase(callingPackage) || CARLINK.equalsIgnoreCase(callingPackage) || ((CARLINK_CARLIFE.equalsIgnoreCase(callingPackage) && 1 == Settings.Global.getInt(this.mContext.getContentResolver(), UCAR_CASTING_STATE, 0)) || MIUI_NOTIFICATION.equalsIgnoreCase(callingPackage) || XIAOMI_BLUETOOTH.equalsIgnoreCase(callingPackage))) {
            UidRecord record = this.mAmService.mProcessList.getUidRecordLOSP(uid);
            if (record != null && record.isIdle() && !record.isCurAllowListed()) {
                synchronized (this.mAmService.mPidsSelfLocked) {
                    try {
                        try {
                            ProcessRecord proc = this.mAmService.mPidsSelfLocked.get(callingPid);
                            int callingUid = proc != null ? proc.uid : Binder.getCallingUid();
                            this.mAmService.tempAllowlistUidLocked(record.getUid(), AccessControlImpl.LOCK_TIME_OUT, 101, "push-service-launch", 0, callingUid);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
            return 0;
        }
        return defMode;
    }

    public void startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr) {
        UidRecord record;
        String callerPackage = app.callerPackage;
        if ((XMSF.equalsIgnoreCase(callerPackage) || MIUI_NOTIFICATION.equalsIgnoreCase(callerPackage)) && (record = this.mAmService.mProcessList.getUidRecordLOSP(app.uid)) != null && record.isIdle() && !record.isCurAllowListed()) {
            int callingUid = Binder.getCallingUid();
            this.mAmService.tempAllowlistUidLocked(record.getUid(), AccessControlImpl.LOCK_TIME_OUT, 101, "push-service-launch", 0, callingUid);
        }
    }

    public String getProcessNameByPid(int pid) {
        return ProcessUtils.getProcessNameByPid(pid);
    }

    public String getPackageNameByPid(int pid) {
        return ProcessUtils.getPackageNameByPid(pid);
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        if (!this.mSystemReady) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        return checkServiceWakePath(service, resolvedType, callerInfo, userId);
    }

    public boolean checkRunningCompatibility(Intent service, String resolvedType, int callingUid, int callingPid, int userId) {
        ProcessRecord record;
        if (!this.mSystemReady) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, callingPid, callingUid);
        if (callerInfo == null && (record = ProcessUtils.getProcessRecordByPid(callingPid)) != null) {
            callerInfo = new CallerInfo();
            callerInfo.callerUid = callingUid;
            callerInfo.callerPkg = record.info.packageName;
            callerInfo.callerPid = callingPid;
            callerInfo.callerProcessName = record.processName;
        }
        return checkServiceWakePath(service, resolvedType, callerInfo, userId);
    }

    private boolean checkServiceWakePath(Intent service, String resolvedType, CallerInfo callerInfo, int userId) {
        try {
            ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, resolvedType, FormatBytesUtil.KB, userId);
            ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
            if (!((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).shouldInterceptService(service, callerInfo, sInfo)) {
                if (!checkWakePath(this.mAmService, callerInfo, null, service, sInfo, 8, userId)) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (RemoteException e) {
            return true;
        }
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, ActivityInfo info, Intent intent, int userId, String callingPackage) {
        SecurityManagerInternal securityManagerInternal;
        if (info == null) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        if (callerInfo != null) {
            PrivacyTestModeStub.get().collectPrivacyTestModeInfo(callerInfo.callerProcessName, info, intent, callingPackage, this.mContext);
        }
        if (this.mSecurityInternal == null) {
            this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        if (Build.IS_INTERNATIONAL_BUILD && TextUtils.equals(callingPackage, CHROME_PKG) && !TextUtils.equals(CHROME_PKG, info.applicationInfo.packageName) && (securityManagerInternal = this.mSecurityInternal) != null && !securityManagerInternal.isAllowedDeviceProvision()) {
            return false;
        }
        return checkWakePath(this.mAmService, callerInfo, callingPackage, intent, info, 1, userId);
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, int callingUid, ContentProviderRecord record, int userId) {
        String callerPkg;
        if (!this.mSystemReady || record == null || record.name == null) {
            return true;
        }
        Intent intent = new Intent();
        intent.setClassName(record.name.getPackageName(), record.name.getClassName());
        intent.putExtra("android.intent.extra.UID", callingUid);
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        if (callerInfo == null) {
            AndroidPackage aPackage = this.mPackageManager.getPackage(callingUid);
            if (aPackage == null) {
                return true;
            }
            String callerPkg2 = aPackage.getPackageName();
            callerPkg = callerPkg2;
        } else {
            String callerPkg3 = callerInfo.callerPkg;
            callerPkg = callerPkg3;
        }
        if (ClipboardServiceStub.get().checkProviderWakePathForClipboard(callerPkg, callingUid, record.info, userId)) {
            return true;
        }
        return checkWakePath(this.mAmService, callerInfo, null, intent, record.info, 4, userId);
    }

    /* JADX WARN: Code restructure failed: missing block: B:44:0x00d3, code lost:
    
        if (com.android.server.wm.WindowProcessUtils.isPackageRunning(r16.mAmService.mActivityTaskManager, r10, r0.processName, r0.uid) != false) goto L42;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean checkRunningCompatibility(android.content.ComponentName r17, int r18, int r19, int r20) {
        /*
            Method dump skipped, instructions count: 300
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityManagerServiceImpl.checkRunningCompatibility(android.content.ComponentName, int, int, int):boolean");
    }

    static boolean checkWakePath(ActivityManagerService ams, CallerInfo callerInfo, String callingPackage, Intent intent, ComponentInfo info, int wakeType, int userId) {
        long startTime;
        String callerPkg;
        int calleeUid;
        int calleeUid2;
        String callerPkg2;
        WakePathChecker checker;
        String callerPkg3;
        String calleePkg;
        String calleePkg2;
        boolean abort;
        boolean z = true;
        if (ams == null || intent == null) {
            return true;
        }
        if (info == null) {
            return true;
        }
        WakePathChecker checker2 = WakePathChecker.getInstance();
        checker2.updatePath(intent, info, wakeType, userId);
        long startTime2 = SystemClock.elapsedRealtime();
        int callerUid = -1;
        String callerPkg4 = "";
        if (callerInfo != null) {
            String widgetProcessName = callerInfo.callerPkg + ":widgetProvider";
            if (WIDGET_PROVIDER_WHITE_LIST.contains(callerInfo.callerPkg)) {
                startTime = startTime2;
            } else if (widgetProcessName.equals(callerInfo.callerProcessName)) {
                if (wakeType == 1) {
                    abort = !PendingIntentRecordImpl.containsPendingIntent(callerInfo.callerPkg);
                } else {
                    abort = !WindowProcessUtils.isProcessRunning(ams.mActivityTaskManager, info.processName, info.applicationInfo.uid);
                }
                if (abort) {
                    Slog.i(TAG, "MIUILOG- Reject widget call from " + callerInfo.callerPkg);
                    WakePathChecker.getInstance().recordWakePathCall(callerInfo.callerPkg, info.packageName, wakeType, UserHandle.getUserId(callerInfo.callerUid), UserHandle.getUserId(info.applicationInfo.uid), false);
                    return false;
                }
                startTime = startTime2;
            } else {
                startTime = startTime2;
            }
            String callerPkg5 = callerInfo.callerPkg;
            callerUid = callerInfo.callerUid;
            callerPkg = callerPkg5;
        } else {
            startTime = startTime2;
            if (TextUtils.isEmpty(callingPackage) && wakeType == 4) {
                int callerUid2 = intent.getIntExtra("android.intent.extra.UID", -1);
                if (callerUid2 != -1) {
                    try {
                        String[] pkgs = ams.getPackageManager().getPackagesForUid(callerUid2);
                        if (pkgs != null && pkgs.length != 0) {
                            callerPkg4 = pkgs[0];
                        }
                        callerUid = callerUid2;
                        callerPkg = callerPkg4;
                    } catch (Exception e) {
                        Log.e(TAG, "getPackagesFor uid exception!", e);
                        callerPkg = "android";
                        callerUid = callerUid2;
                    }
                } else {
                    callerPkg = "android";
                    callerUid = callerUid2;
                }
            } else {
                callerPkg = callingPackage;
            }
        }
        String calleePkg3 = info.packageName;
        String className = info.name;
        String action = intent.getAction();
        if (info.applicationInfo == null) {
            calleeUid = -1;
        } else {
            int calleeUid3 = info.applicationInfo.uid;
            calleeUid = calleeUid3;
        }
        if (TextUtils.isEmpty(calleePkg3) || TextUtils.equals(callerPkg, calleePkg3)) {
            return true;
        }
        if (calleeUid < 0) {
            calleeUid2 = calleeUid;
            callerPkg2 = callerPkg;
            checker = checker2;
            callerPkg3 = className;
            calleePkg = calleePkg3;
            calleePkg2 = action;
        } else if (!WindowProcessUtils.isPackageRunning(ams.mActivityTaskManager, calleePkg3, info.processName, calleeUid)) {
            calleeUid2 = calleeUid;
            callerPkg2 = callerPkg;
            checker = checker2;
            z = true;
            callerPkg3 = className;
            calleePkg = calleePkg3;
            calleePkg2 = action;
        } else {
            String callerPkg6 = callerPkg;
            boolean isAllow = !checker2.calleeAliveMatchBlackRule(action, className, callerPkg, calleePkg3, userId, wakeType << 12, true);
            if (!isAllow) {
                Slog.i(TAG, "MIUILOG-Reject alive wakepath call " + userId + " caller= " + callerPkg6 + " callee= " + calleePkg3 + " classname=" + className + " action=" + action + " wakeType=" + wakeType);
            }
            return isAllow;
        }
        boolean ret = !checker.matchWakePathRule(calleePkg2, callerPkg3, callerPkg2, calleePkg, callerUid, calleeUid2, wakeType, userId);
        checkTime(startTime, "checkWakePath");
        return ret;
    }

    private static void checkTime(long startTime, String where) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 1000) {
            Slog.w(TAG, "MIUILOG-checkTime:Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    public boolean ignoreSpecifiedAuthority(String authority) {
        return mIgnoreAuthorityList.contains(authority);
    }

    boolean isStartWithBackupRestriction(Context context, String backupPkgName, ProcessRecord app) {
        if (app.getActiveInstrumentation() != null) {
            ApplicationInfo applicationInfo = app.getActiveInstrumentation().mTargetInfo;
            return false;
        }
        ApplicationInfo applicationInfo2 = app.info;
        return false;
    }

    public void killProcessDueToResolutionChanged() {
        ProcessConfig config = new ProcessConfig(18);
        config.setPriority(6);
        ProcessManager.kill(config);
    }

    public void finishBootingAsUser(int userId) {
        ActivityTaskManagerServiceImpl.getInstance().restartSubScreenUiIfNeeded(userId, "finishBooting");
    }

    public void moveUserToForeground(int newUserId) {
        ActivityTaskManagerServiceImpl.getInstance().restartSubScreenUiIfNeeded(newUserId, "moveUserToForeground");
    }

    public boolean isAllowedOperatorGetPhoneNumber(ActivityManagerService ams, String permission) {
        String[] content = permission.split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
        if (content.length != 4) {
            return true;
        }
        int pid = Integer.parseInt(content[3]);
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        if (TextUtils.isEmpty(packageName)) {
            return true;
        }
        int op = Integer.parseInt(content[1]);
        int uid = Integer.parseInt(content[2]);
        return ams.mAppOpsService.noteOperation(op, uid, packageName, (String) null, false, "ActivityManagerServiceImpl#isAllowedOperatorGetPhoneNumber", false).getOpMode() == 0;
    }

    public boolean onTransact(ActivityManagerService activityManagerService, int i, Parcel parcel, Parcel parcel2, int i2) {
        ApplicationInfo applicationInfo;
        ApplicationInfo applicationInfo2;
        if (i == 16777214) {
            return setPackageHoldOn(activityManagerService, parcel, parcel2);
        }
        if (i == 16777213) {
            return getPackageHoldOn(parcel, parcel2);
        }
        if (i == 16777212) {
            parcel.enforceInterface("android.app.IActivityManager");
            ActivityInfo lastResumedActivityInfo = ActivityTaskManagerServiceImpl.getInstance().getLastResumedActivityInfo();
            parcel2.writeNoException();
            parcel2.writeParcelable(lastResumedActivityInfo, 0);
            return true;
        }
        if (i == 16776609) {
            parcel.enforceInterface("android.app.IActivityManager");
            boolean isTopSplitActivity = isTopSplitActivity(parcel.readInt(), parcel.readStrongBinder());
            parcel2.writeNoException();
            parcel2.writeInt(isTopSplitActivity ? 1 : 0);
            return true;
        }
        if (i == 16776608) {
            int callingUid = Binder.getCallingUid();
            String packageNameByPid = getPackageNameByPid(Binder.getCallingPid());
            if (UserHandle.isApp(callingUid) && ((applicationInfo2 = this.mPackageManager.getApplicationInfo(packageNameByPid, 0L, callingUid, UserHandle.getUserId(callingUid))) == null || (!applicationInfo2.isSystemApp() && !applicationInfo2.isProduct()))) {
                long uptimeMillis = SystemClock.uptimeMillis();
                if (splitDecouplePkgList.containsKey(packageNameByPid)) {
                    if (!splitDecouplePkgList.get(packageNameByPid).contains(sha256(this.mPackageManager.getPackageInfo(packageNameByPid, 64L, callingUid, UserHandle.getUserId(callingUid)).signatures[0].toByteArray()))) {
                        Slog.e(TAG, "Permission Denial: not set package " + packageNameByPid + ", security check took: " + (SystemClock.uptimeMillis() - uptimeMillis) + "ms");
                        return false;
                    }
                    Slog.d(TAG, "security check took " + (SystemClock.uptimeMillis() - uptimeMillis) + "ms.");
                } else {
                    Slog.e(TAG, "Permission Denial: not from uid " + callingUid + ", security check took: " + (SystemClock.uptimeMillis() - uptimeMillis) + "ms");
                    return false;
                }
            }
            parcel.enforceInterface("android.app.IActivityManager");
            IBinder topSplitActivity = getTopSplitActivity(parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeStrongBinder(topSplitActivity);
            return true;
        }
        if (i == 16776610) {
            parcel.enforceInterface("android.app.IActivityManager");
            removeFromEntryStack(parcel.readInt(), parcel.readStrongBinder());
            return true;
        }
        if (i == 16776611) {
            parcel.enforceInterface("android.app.IActivityManager");
            clearEntryStack(parcel.readInt(), parcel.readStrongBinder());
            return true;
        }
        if (i == 16776612) {
            parcel.enforceInterface("android.app.IActivityManager");
            addToEntryStack(parcel.readInt(), parcel.readStrongBinder(), parcel.readInt(), (Intent) Intent.CREATOR.createFromParcel(parcel));
            parcel2.writeNoException();
            return true;
        }
        if (i == 16776613) {
            parcel.enforceInterface("android.app.IActivityManager");
            Parcelable[] intentInfo = getIntentInfo(parcel.readInt(), parcel.readInt() > 0);
            parcel2.writeNoException();
            parcel2.writeParcelableArray(intentInfo, 0);
            return true;
        }
        if (i == 16776614) {
            int callingUid2 = Binder.getCallingUid();
            String packageNameByPid2 = getPackageNameByPid(Binder.getCallingPid());
            if (UserHandle.isApp(callingUid2) && ((applicationInfo = this.mPackageManager.getApplicationInfo(packageNameByPid2, 0L, callingUid2, UserHandle.getUserId(callingUid2))) == null || (!applicationInfo.isSystemApp() && !applicationInfo.isProduct()))) {
                long uptimeMillis2 = SystemClock.uptimeMillis();
                if (splitDecouplePkgList.containsKey(packageNameByPid2)) {
                    if (!splitDecouplePkgList.get(packageNameByPid2).contains(sha256(this.mPackageManager.getPackageInfo(packageNameByPid2, 64L, callingUid2, UserHandle.getUserId(callingUid2)).signatures[0].toByteArray()))) {
                        Slog.e(TAG, "Permission Denial: not set package: " + packageNameByPid2 + " security check took: " + (SystemClock.uptimeMillis() - uptimeMillis2) + "ms");
                        return false;
                    }
                    Slog.d(TAG, "security check took: " + (SystemClock.uptimeMillis() - uptimeMillis2) + "ms");
                } else {
                    Slog.e(TAG, "Permission Denial: not from uid: " + callingUid2 + " security check took: " + (SystemClock.uptimeMillis() - uptimeMillis2) + "ms");
                    return false;
                }
            }
            boolean z = false;
            parcel.enforceInterface("android.app.IActivityManager");
            Intent intent = (Intent) parcel.readParcelable(Intent.class.getClassLoader());
            int readInt = parcel.readInt();
            Bundle readBundle = parcel.readBundle();
            if (parcel.readInt() > 0) {
                z = true;
            }
            setIntentInfo(intent, readInt, readBundle, z);
            parcel2.writeNoException();
            return true;
        }
        if (i != 16776607) {
            return false;
        }
        parcel.enforceInterface("android.app.IActivityManager");
        ActivityManager.RunningTaskInfo splitTaskInfo = ActivityTaskManagerServiceStub.get().getSplitTaskInfo(parcel.readStrongBinder());
        parcel2.writeNoException();
        parcel2.writeParcelable(splitTaskInfo, 0);
        return true;
    }

    public boolean getPackageHoldOn(Parcel data, Parcel reply) {
        data.enforceInterface("android.app.IActivityManager");
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        reply.writeNoException();
        try {
            if (UserHandle.getAppId(callingUid) != 1000) {
                reply.writeString("");
                Slog.e(TAG, "Permission Denial: getPackageHoldOn() not from system " + callingUid);
            } else {
                reply.writeString(ActivityTaskManagerServiceStub.get().getPackageHoldOn());
            }
            Binder.restoreCallingIdentity(ident);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public boolean setPackageHoldOn(ActivityManagerService service, Parcel data, Parcel reply) {
        data.enforceInterface("android.app.IActivityManager");
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (UserHandle.getAppId(callingUid) == 1000) {
                ActivityTaskManagerServiceStub.get().setPackageHoldOn(service.mActivityTaskManager, data.readString());
            } else {
                Slog.e(TAG, "Permission Denial: setPackageHoldOn() not from system uid " + callingUid);
            }
            Binder.restoreCallingIdentity(ident);
            reply.writeNoException();
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public static boolean isSystemPackage(String packageName, int userId) {
        try {
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, userId);
            if (applicationInfo == null) {
                return true;
            }
            int flags = applicationInfo.flags;
            return ((flags & 1) == 0 && (flags & 128) == 0) ? false : true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean isBoostNeeded(ProcessRecord app, String hostingType, String hostingName) {
        String callerPackage = app.callerPackage;
        boolean isSystem = isSystemPackage(callerPackage, 0);
        boolean isNeeded = "service".equals(hostingType) && hostingName.endsWith(MI_PUSH) && XMSF.equals(callerPackage) && isSystem;
        boolean isNeeded2 = MI_VOICE.equals(hostingName) || isNeeded;
        boolean isNeeded3 = MIUI_NOTIFICATION.equals(callerPackage) || isNeeded2;
        if (WEIXIN.equals(app.processName)) {
            isNeeded3 = true;
        }
        Slog.d(BOOST_TAG, "hostingType=" + hostingType + ", hostingName=" + hostingName + ", callerPackage=" + callerPackage + ", isSystem=" + isSystem + ", isBoostNeeded=" + isNeeded3 + ".");
        return isNeeded3;
    }

    public boolean doBoostEx(ProcessRecord app, long beginTime) {
        boolean boostNeededNext = false | doTopAppBoost(app, beginTime);
        if (WEIXIN.equals(app.processName)) {
            return boostNeededNext | doForegroundBoost(app, beginTime);
        }
        return boostNeededNext;
    }

    public String dumpMiuiStackTraces(int[] pids) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("Only the system process can call dumpMiuiStackTraces, received request from uid: " + callingUid);
        }
        if (pids.length < 1) {
            return null;
        }
        ArrayList<Integer> javapids = new ArrayList<>(3);
        ArrayList<Integer> nativePids = new ArrayList<>(3);
        for (int i = 0; i < pids.length; i++) {
            int adj = ScoutHelper.getOomAdjOfPid(TAG, pids[i]);
            int isJavaOrNativeProcess = ScoutHelper.checkIsJavaOrNativeProcess(adj);
            if (isJavaOrNativeProcess != 0) {
                if (isJavaOrNativeProcess == 1) {
                    javapids.add(Integer.valueOf(pids[i]));
                } else if (isJavaOrNativeProcess == 2) {
                    nativePids.add(Integer.valueOf(pids[i]));
                }
            }
        }
        File mTraceFile = StackTracesDumpHelper.dumpStackTraces(javapids, (ProcessCpuTracker) null, (SparseBooleanArray) null, CompletableFuture.completedFuture(nativePids), (StringWriter) null, "App Scout Exception", (String) null, (Executor) null, (AnrLatencyTracker) null);
        if (mTraceFile != null) {
            return mTraceFile.getAbsolutePath();
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x00c4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void dumpMiuiStackTracesForCmdlines(java.lang.String[] r18, java.lang.String r19, java.lang.String r20) {
        /*
            Method dump skipped, instructions count: 283
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityManagerServiceImpl.dumpMiuiStackTracesForCmdlines(java.lang.String[], java.lang.String, java.lang.String):void");
    }

    public File dumpAppStackTraces(ArrayList<Integer> firstPids, SparseArray<Boolean> lastPids, ArrayList<Integer> nativePids, String subject, String path) {
        Slog.i(MIUI_APP_TAG, "dumpStackTraces pids=" + lastPids + " nativepids=" + nativePids);
        File tracesFile = new File(path);
        try {
            if (tracesFile.createNewFile()) {
                FileUtils.setPermissions(tracesFile.getAbsolutePath(), 384, -1, -1);
            }
            if (subject != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(tracesFile, true);
                    try {
                        String header = "Subject: " + subject + "\n";
                        fos.write(header.getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    } finally {
                    }
                } catch (IOException e) {
                    Slog.w(MIUI_APP_TAG, "Exception writing subject to scout dump file:", e);
                }
            }
            StackTracesDumpHelper.dumpStackTraces(tracesFile.getAbsolutePath(), firstPids, CompletableFuture.completedFuture(nativePids), CompletableFuture.completedFuture(null), (Future) null, (AnrLatencyTracker) null);
            return tracesFile;
        } catch (IOException e2) {
            Slog.w(MIUI_APP_TAG, "Exception creating scout dump file:", e2);
            return null;
        }
    }

    public File dumpOneProcessTraces(int pid, String path, String subject) {
        ArrayList<Integer> firstPids = new ArrayList<>();
        ArrayList<Integer> nativePids = new ArrayList<>();
        int adj = ScoutHelper.getOomAdjOfPid(TAG, pid);
        int isJavaOrNativeProcess = ScoutHelper.checkIsJavaOrNativeProcess(adj);
        if (isJavaOrNativeProcess == 1) {
            firstPids.add(Integer.valueOf(pid));
        } else if (isJavaOrNativeProcess == 2) {
            nativePids.add(Integer.valueOf(pid));
        } else {
            Slog.w(MIUI_APP_TAG, "can not distinguish for this process's adj" + adj);
            return null;
        }
        File traceFile = new File(path);
        try {
            if (traceFile.createNewFile()) {
                FileUtils.setPermissions(traceFile.getAbsolutePath(), 384, -1, -1);
            }
            if (subject != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(traceFile, true);
                    try {
                        String header = "Subject: " + subject + "\n";
                        fos.write(header.getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    } finally {
                    }
                } catch (IOException e) {
                    Slog.w(MIUI_APP_TAG, "Exception writing subject to scout dump file:", e);
                }
            }
            StackTracesDumpHelper.dumpStackTraces(traceFile.getAbsolutePath(), firstPids, CompletableFuture.completedFuture(nativePids), (Future) null, (Future) null, (AnrLatencyTracker) null);
            return traceFile;
        } catch (IOException e2) {
            Slog.w(MIUI_APP_TAG, "Exception creating scout dump file:", e2);
            return null;
        }
    }

    public void dumpSystemTraces(final String path) {
        requestDumpTraceCount.getAndIncrement();
        if (requestDumpTraceCount.get() > 0 && !dumpFlag.get()) {
            requestDumpTraceCount.getAndDecrement();
            dumpFlag.set(true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                try {
                    executor.execute(new Runnable() { // from class: com.android.server.am.ActivityManagerServiceImpl$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            ActivityManagerServiceImpl.this.lambda$dumpSystemTraces$0(path);
                        }
                    });
                    Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                    if (executor == null) {
                        return;
                    }
                } catch (Exception e) {
                    Slog.w(MIUI_APP_TAG, "Exception occurs while dumping system scout trace file:", e);
                    Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                    if (executor == null) {
                        return;
                    }
                }
                executor.shutdown();
                return;
            } catch (Throwable th) {
                Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                if (executor != null) {
                    executor.shutdown();
                }
                throw th;
            }
        }
        synchronized (dumpTraceRequestList) {
            dumpTraceRequestList.add(path);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r1v9, types: [com.android.server.am.ActivityManagerServiceImpl$2] */
    public /* synthetic */ void lambda$dumpSystemTraces$0(String path) {
        ScoutHelper.CheckDState(MIUI_APP_TAG, ActivityManagerService.MY_PID);
        Slog.i(MIUI_APP_TAG, "Start dumping system_server trace ...");
        final File systemTraceFile = dumpOneProcessTraces(ActivityManagerService.MY_PID, path, "App Scout Exception");
        if (systemTraceFile != null) {
            Slog.d(MIUI_APP_TAG, "Dump scout system trace file successfully!");
            final ArrayList<String> tempArray = new ArrayList<>();
            synchronized (dumpTraceRequestList) {
                Iterator<String> it = dumpTraceRequestList.iterator();
                while (it.hasNext()) {
                    String filePath = it.next();
                    tempArray.add(filePath);
                }
                dumpTraceRequestList.clear();
            }
            dumpFlag.set(false);
            Slog.d(MIUI_APP_TAG, "starting copying file");
            if (requestDumpTraceCount.get() > 0 && tempArray.size() > 0) {
                new Thread() { // from class: com.android.server.am.ActivityManagerServiceImpl.2
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        Iterator it2 = tempArray.iterator();
                        while (it2.hasNext()) {
                            String dumpPath = (String) it2.next();
                            File dumpFile = new File(dumpPath);
                            ActivityManagerServiceImpl.requestDumpTraceCount.getAndDecrement();
                            Slog.d(ActivityManagerServiceImpl.MIUI_APP_TAG, "requestDumpTraceCount delete one, now is " + ActivityManagerServiceImpl.requestDumpTraceCount.toString());
                            try {
                                if (dumpFile.createNewFile()) {
                                    FileUtils.setPermissions(dumpFile.getAbsolutePath(), 384, -1, -1);
                                    if (FileUtils.copyFile(systemTraceFile, dumpFile)) {
                                        Slog.i(ActivityManagerServiceImpl.MIUI_APP_TAG, "Success copying system_server trace to path" + dumpPath);
                                    } else {
                                        Slog.w(ActivityManagerServiceImpl.MIUI_APP_TAG, "Fail to copy system_server trace to path" + dumpPath);
                                    }
                                }
                            } catch (IOException e) {
                                Slog.w(ActivityManagerServiceImpl.MIUI_APP_TAG, "Exception occurs while copying system scout trace file:", e);
                            }
                        }
                    }
                }.start();
                return;
            }
            return;
        }
        Slog.w(MIUI_APP_TAG, "Dump scout system trace file fail!");
        dumpFlag.set(false);
    }

    public void dumpMiuiJavaTrace(int pid) {
        if (Process.getThreadGroupLeader(pid) == pid && ScoutHelper.getOomAdjOfPid("MIUI ANR", pid) > -1000) {
            Process.sendSignal(pid, 3);
            Slog.w("MIUI ANR", "[Scout] Send SIGNAL_QUIT to generate java stack dump. Pid:" + pid);
        }
    }

    public int getOomAdjOfPid(int pid) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("Only the system process can call getOomAdjOfPid, received request from uid: " + callingUid);
        }
        return ScoutHelper.getOomAdjOfPid(TAG, pid);
    }

    private static boolean doTopAppBoost(ProcessRecord app, long beginTime) {
        if (SystemClock.uptimeMillis() - beginTime > 3000 || app.mState.getCurrentSchedulingGroup() == 3) {
            return false;
        }
        if (app.mState.getCurrentSchedulingGroup() < 3) {
            app.mState.setCurrentSchedulingGroup(3);
            Slog.d(BOOST_TAG, "Process is boosted to top app, processName=" + app.processName + ".");
            return true;
        }
        return true;
    }

    private static boolean doForegroundBoost(ProcessRecord app, long beginTime) {
        if (SystemClock.uptimeMillis() - beginTime > KEEP_FOREGROUND_DURATION) {
            return false;
        }
        if (app.mState.getCurrentSchedulingGroup() < 2) {
            app.mState.setCurrentSchedulingGroup(2);
            return true;
        }
        return true;
    }

    private static boolean checkThawTime(int uid, String report, GreezeManagerInternal greezer) {
        int timeout;
        Slog.d(TAG, "checkThawTime uid=" + uid + " report=" + report);
        if (TextUtils.isEmpty(report)) {
            return false;
        }
        if (report.startsWith("Broadcast of") || report.startsWith("executing service") || report.startsWith("ContentProvider not")) {
            timeout = 20000;
        } else if (report.startsWith("Input dispatching")) {
            timeout = SpeedTestModeServiceImpl.ENABLE_SPTM_MIN_MEMORY;
        } else if (report.startsWith("App requested: isolate_instructions:")) {
            timeout = 2000;
        } else {
            if (!JOB_ANRS.contains(report)) {
                return false;
            }
            timeout = 2000;
        }
        Slog.d(TAG, "checkThawTime thawTime=" + greezer.getLastThawedTime(uid, 1) + " now=" + SystemClock.uptimeMillis());
        long thawTime = greezer.getLastThawedTime(uid, 1);
        if (uid < 10000 || uid > 19999 || thawTime <= 0 || SystemClock.uptimeMillis() - thawTime >= timeout) {
            return false;
        }
        Slog.d(TAG, "matched " + report + " app time uid=" + uid);
        return true;
    }

    private GreezeManagerInternal getGreezeService() {
        if (this.greezer == null) {
            this.greezer = GreezeManagerInternal.getInstance();
        }
        return this.greezer;
    }

    public boolean skipFrozenAppAnr(ApplicationInfo info, int uid, String report) {
        GreezeManagerInternal greezer = getGreezeService();
        if (greezer == null) {
            return false;
        }
        int appid = UserHandle.getAppId(info.uid);
        if (uid != info.uid) {
            appid = UserHandle.getAppId(uid);
        }
        if (appid <= 1000) {
            return false;
        }
        if (((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).skipFrozenAppAnr(info, uid, report)) {
            return true;
        }
        if (greezer.isUidFrozen(appid)) {
            Slog.d(TAG, " matched appid is " + appid);
            return true;
        }
        if (checkThawTime(uid, report, greezer)) {
            return true;
        }
        int[] frozenUids = greezer.getFrozenUids(GreezeManagerInternal.GREEZER_MODULE_ALL);
        int[] frozenPids = greezer.getFrozenPids(GreezeManagerInternal.GREEZER_MODULE_ALL);
        if (frozenUids.length > 0) {
            Slog.d(TAG, "frozen uids:" + Arrays.toString(frozenUids));
        }
        StringBuilder inf = new StringBuilder();
        for (int i = 0; i < frozenPids.length; i++) {
            inf.append("pid: " + frozenPids[i] + "/uid:" + Process.getUidForPid(frozenPids[i]) + ",");
        }
        Slog.d(TAG, "frozen procs: " + ((Object) inf));
        return false;
    }

    public void cleanUpApplicationRecordLocked(ProcessRecord app) {
        super.cleanUpApplicationRecordLocked(app);
        if (DeviceFeature.SUPPORT_SPLIT_ACTIVITY) {
            Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
            if (map != null && map.containsKey(Integer.valueOf(app.mPid))) {
                Slog.w(TAG, "Split main entrance killed, clear sub activities for " + app.info.packageName + ", mPid " + app.mPid);
                clearEntryStack(app.mPid, null);
                this.mSplitActivityEntryStack.remove(Integer.valueOf(app.mPid));
            }
            Map<Integer, Intent> map2 = this.mCurrentSplitIntent;
            if (map2 != null) {
                map2.remove(Integer.valueOf(app.mPid));
            }
            Slog.d(TAG, "Cleaning tablet split stack.");
        }
    }

    public boolean isTopSplitActivity(int pid, IBinder token) {
        Stack<IBinder> stack;
        Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
        return (map == null || map.isEmpty() || token == null || (stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid))) == null || stack.empty() || !token.equals(stack.peek())) ? false : true;
    }

    public IBinder getTopSplitActivity(int pid) {
        Stack<IBinder> stack;
        Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
        if (map == null || map.isEmpty() || (stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid))) == null || stack.empty()) {
            return null;
        }
        return stack.peek();
    }

    public void removeFromEntryStack(int pid, IBinder token) {
        Stack<IBinder> stack;
        synchronized (this.splitEntryStackLock) {
            if (token != null) {
                Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
                if (map != null && (stack = map.get(Integer.valueOf(pid))) != null && !stack.empty()) {
                    stack.remove(token);
                    Slog.d(TAG, "SplitEntryStack: remove from stack, size=" + stack.size() + " pid=" + pid);
                }
            }
        }
    }

    public void clearEntryStack(int pid, IBinder selfToken) {
        synchronized (this.splitEntryStackLock) {
            Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
            if (map != null && !map.isEmpty()) {
                Stack<IBinder> stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
                if (stack != null && !stack.empty() && (selfToken == null || selfToken.equals(stack.peek()))) {
                    long ident = Binder.clearCallingIdentity();
                    while (!stack.empty()) {
                        IBinder token = stack.pop();
                        Slog.d(TAG, "SplitEntryStack: pop stack, size=" + stack.size() + " pid=" + pid);
                        if (token != null && !token.equals(selfToken)) {
                            this.mAmService.finishActivity(token, 0, (Intent) null, 0);
                        }
                    }
                    Binder.restoreCallingIdentity(ident);
                    if (selfToken != null) {
                        stack.push(selfToken);
                        Slog.d(TAG, "SplitEntryStack: push self to stack, size=" + stack.size() + " pid=" + pid);
                    }
                }
            }
        }
    }

    public void addToEntryStack(int pid, IBinder token, int resultCode, Intent resultData) {
        synchronized (this.splitEntryStackLock) {
            if (this.mSplitActivityEntryStack == null) {
                this.mSplitActivityEntryStack = new ConcurrentHashMap();
                Slog.d(TAG, "SplitEntryStack: init");
            }
            Stack<IBinder> stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
            if (stack == null) {
                stack = new Stack<>();
                this.mSplitActivityEntryStack.put(Integer.valueOf(pid), stack);
                Slog.d(TAG, "SplitEntryStack: create stack, pid=" + pid);
            }
            if (!stack.contains((Binder) token)) {
                stack.push(token);
                Slog.d(TAG, "SplitEntryStack: push to stack, size=" + stack.size() + " pid=" + pid);
            }
        }
    }

    private Parcelable[] getIntentInfo(int pid, boolean isForLast) {
        if (isForLast) {
            return new Parcelable[]{this.mLastSplitIntent, this.mSplitExtras};
        }
        return new Parcelable[]{this.mCurrentSplitIntent.get(Integer.valueOf(pid)), null};
    }

    private void setIntentInfo(Intent intent, int pid, Bundle bundle, boolean isForLast) {
        if (isForLast) {
            this.mLastSplitIntent = intent;
            this.mSplitExtras = bundle;
        } else {
            if (!this.mCurrentSplitIntent.containsKey(Integer.valueOf(pid))) {
                Log.e(TAG, "CRITICAL_LOG add intent info.");
            }
            this.mCurrentSplitIntent.put(Integer.valueOf(pid), intent);
        }
    }

    public boolean isKillProvider(ContentProviderRecord cpr, ProcessRecord proc, ProcessRecord capp) {
        if (capp.mState.getCurAdj() > 250 && !ProcessUtils.isHomeProcess(capp) && !isProtectProcess(capp.uid, capp.info.packageName, capp.processName) && !capp.getWindowProcessController().isPreviousProcess()) {
            return true;
        }
        Slog.w(TAG, "visible app " + capp.processName + " depends on provider " + cpr.name.flattenToShortString() + " in dying proc " + (proc != null ? proc.processName : "??") + " (adj " + (proc != null ? Integer.valueOf(proc.mState.getSetAdj()) : "??") + ")");
        return false;
    }

    private boolean isProtectProcess(int uid, String pkgName, String procName) {
        if (this.mSmartPowerService.isProcessPerceptible(uid, procName) || this.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, pkgName, procName)) {
            return true;
        }
        return false;
    }

    public boolean killPackageProcesses(String packageName, int appId, int userId, String reason) {
        boolean result = false;
        synchronized (this.mAmService) {
            ActivityManagerService.boostPriorityForLockedSection();
            try {
                result = this.mAmService.mProcessList.killPackageProcessesLSP(packageName, appId, userId, 0, false, true, true, false, true, false, 13, 0, reason);
            } catch (Exception e) {
                Slog.e(TAG, "invoke killPackageProcessesLocked error:", e);
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
        return result;
    }

    public void syncFontForWebView() {
        SymlinkUtils.onAttachApplication();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void enableAmsDebugConfig(String config, boolean enable) {
        char c;
        Slog.d(TAG, "enableAMSDebugConfig, config=:" + config + ", enable=:" + enable);
        switch (config.hashCode()) {
            case -2091566946:
                if (config.equals("DEBUG_VISIBILITY")) {
                    c = ' ';
                    break;
                }
                c = 65535;
                break;
            case -1705346631:
                if (config.equals("DEBUG_ANR")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1705346571:
                if (config.equals("DEBUG_APP")) {
                    c = '!';
                    break;
                }
                c = 65535;
                break;
            case -1705335933:
                if (config.equals("DEBUG_LRU")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1705332060:
                if (config.equals("DEBUG_PSS")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -1621031281:
                if (config.equals("DEBUG_FREEZER")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1462091039:
                if (config.equals("DEBUG_TRANSITION")) {
                    c = 31;
                    break;
                }
                c = 65535;
                break;
            case -1325909408:
                if (config.equals("DEBUG_IDLE")) {
                    c = '\"';
                    break;
                }
                c = 65535;
                break;
            case -1067959432:
                if (config.equals("DEBUG_ALLOWLISTS")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            case -866453022:
                if (config.equals("DEBUG_UID_OBSERVERS")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case -735231590:
                if (config.equals("DEBUG_OOM_ADJ_REASON")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -595903599:
                if (config.equals("DEBUG_COMPACTION")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -322538852:
                if (config.equals("DEBUG_SERVICE_EXECUTING")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case -274692833:
                if (config.equals("DEBUG_PERMISSIONS_REVIEW")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case -160480136:
                if (config.equals("DEBUG_CLEANUP")) {
                    c = '(';
                    break;
                }
                c = 65535;
                break;
            case -61428073:
                if (config.equals("DEBUG_METRICS")) {
                    c = ')';
                    break;
                }
                c = 65535;
                break;
            case -61191196:
                if (config.equals("DEBUG_RECENTS_TRIM_TASKS")) {
                    c = 28;
                    break;
                }
                c = 65535;
                break;
            case -44209236:
                if (config.equals("DEBUG_USER_LEAVING")) {
                    c = '$';
                    break;
                }
                c = 65535;
                break;
            case 52145557:
                if (config.equals("DEBUG_BROADCAST")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 53108793:
                if (config.equals("DEBUG_PERMISSIONS_REVIEW_ATMS")) {
                    c = '%';
                    break;
                }
                c = 65535;
                break;
            case 65041228:
                if (config.equals("DEBUG_RECENTS")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case 73340379:
                if (config.equals("DEBUG_RELEASE")) {
                    c = '#';
                    break;
                }
                c = 65535;
                break;
            case 80292298:
                if (config.equals("DEBUG_RESULTS")) {
                    c = '&';
                    break;
                }
                c = 65535;
                break;
            case 156502476:
                if (config.equals("DEBUG_BROADCAST_LIGHT")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 576262193:
                if (config.equals("DEBUG_PROCESSES")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 596380184:
                if (config.equals("DEBUG_BROADCAST_BACKGROUND")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 764822901:
                if (config.equals("DEBUG_ACTIVITY_STARTS")) {
                    c = '\'';
                    break;
                }
                c = 65535;
                break;
            case 826230786:
                if (config.equals("DEBUG_NETWORK")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 833576886:
                if (config.equals("DEBUG_ROOT_TASK")) {
                    c = 29;
                    break;
                }
                c = 65535;
                break;
            case 966898825:
                if (config.equals("DEBUG_SERVICE")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 1202911566:
                if (config.equals("DEBUG_BACKUP")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1222829889:
                if (config.equals("DEBUG_PROCESS_OBSERVERS")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case 1232431591:
                if (config.equals("DEBUG_POWER_QUICK")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1330462516:
                if (config.equals("DEBUG_MU")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 1388181797:
                if (config.equals("DEBUG_FOREGROUND_SERVICE")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 1441072931:
                if (config.equals("DEBUG_BACKGROUND_CHECK")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1700665397:
                if (config.equals("DEBUG_USAGE_STATS")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case 1710111424:
                if (config.equals("DEBUG_SWITCH")) {
                    c = 30;
                    break;
                }
                c = 65535;
                break;
            case 1837355645:
                if (config.equals("DEBUG_PROVIDER")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 1853284313:
                if (config.equals("DEBUG_POWER")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 1981739733:
                if (config.equals("DEBUG_BROADCAST_DEFERRAL")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1993785769:
                if (config.equals("DEBUG_OOM_ADJ")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                ActivityManagerDebugConfig.DEBUG_ANR = enable;
                return;
            case 1:
                ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK = enable;
                return;
            case 2:
                ActivityManagerDebugConfig.DEBUG_BACKUP = enable;
                return;
            case 3:
                ActivityManagerDebugConfig.DEBUG_BROADCAST = enable;
                return;
            case 4:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = enable;
                return;
            case 5:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = enable;
                return;
            case 6:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL = enable;
                return;
            case 7:
                ActivityManagerDebugConfig.DEBUG_COMPACTION = enable;
                return;
            case '\b':
                ActivityManagerDebugConfig.DEBUG_FREEZER = enable;
                return;
            case '\t':
                ActivityManagerDebugConfig.DEBUG_LRU = enable;
                return;
            case '\n':
                ActivityManagerDebugConfig.DEBUG_MU = enable;
                return;
            case 11:
                ActivityManagerDebugConfig.DEBUG_NETWORK = enable;
                return;
            case '\f':
                ActivityManagerDebugConfig.DEBUG_OOM_ADJ = enable;
                return;
            case '\r':
                ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON = enable;
                return;
            case 14:
                ActivityManagerDebugConfig.DEBUG_POWER = enable;
                return;
            case 15:
                ActivityManagerDebugConfig.DEBUG_POWER_QUICK = enable;
                return;
            case 16:
                ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = enable;
                return;
            case 17:
                ActivityManagerDebugConfig.DEBUG_PROCESSES = enable;
                return;
            case 18:
                ActivityManagerDebugConfig.DEBUG_PROVIDER = enable;
                return;
            case 19:
                ActivityManagerDebugConfig.DEBUG_PSS = enable;
                return;
            case 20:
                ActivityManagerDebugConfig.DEBUG_SERVICE = enable;
                return;
            case 21:
                ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE = enable;
                return;
            case 22:
                ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = enable;
                return;
            case 23:
                ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS = enable;
                return;
            case 24:
                ActivityManagerDebugConfig.DEBUG_USAGE_STATS = enable;
                return;
            case 25:
                ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = enable;
                return;
            case 26:
                ActivityManagerDebugConfig.DEBUG_ALLOWLISTS = enable;
                return;
            case 27:
                ActivityTaskManagerDebugConfig.DEBUG_RECENTS = enable;
                return;
            case 28:
                ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS = enable;
                return;
            case IResultValue.MISYS_ESPIPE /* 29 */:
                ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK = enable;
                return;
            case 30:
                ActivityTaskManagerDebugConfig.DEBUG_SWITCH = enable;
                return;
            case IResultValue.MISYS_EMLINK /* 31 */:
                ActivityTaskManagerDebugConfig.DEBUG_TRANSITION = enable;
                return;
            case ' ':
                ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY = enable;
                return;
            case UsbKeyboardUtil.COMMAND_TOUCH_PAD_ENABLE /* 33 */:
                ActivityTaskManagerDebugConfig.DEBUG_APP = enable;
                return;
            case '\"':
                ActivityTaskManagerDebugConfig.DEBUG_IDLE = enable;
                return;
            case UsbKeyboardUtil.COMMAND_BACK_LIGHT_ENABLE /* 35 */:
                ActivityTaskManagerDebugConfig.DEBUG_RELEASE = enable;
                return;
            case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING = enable;
                return;
            case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = enable;
                return;
            case '&':
                ActivityTaskManagerDebugConfig.DEBUG_RESULTS = enable;
                return;
            case '\'':
                ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS = enable;
                return;
            case NanoSocketCallback.CALLBACK_TYPE_TOUCHPAD /* 40 */:
                ActivityTaskManagerDebugConfig.DEBUG_CLEANUP = enable;
                return;
            case ')':
                ActivityTaskManagerDebugConfig.DEBUG_METRICS = enable;
                return;
            default:
                return;
        }
    }

    public boolean skipPruneOldTraces() {
        if (ScoutUtils.isLibraryTest()) {
            return true;
        }
        return false;
    }

    public boolean checkStartInputMethodSettingsActivity(IIntentSender target) {
        if (target instanceof PendingIntentRecord) {
            PendingIntentRecord pendingIntentRecord = (PendingIntentRecord) target;
            if (pendingIntentRecord.key != null && pendingIntentRecord.key.requestIntent != null && "android.settings.INPUT_METHOD_SETTINGS".equals(pendingIntentRecord.key.requestIntent.getAction())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean dump(String cmd, FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        if ("logging".equals(cmd)) {
            dumpLogText(pw);
            return true;
        }
        if ("app-logging".equals(cmd)) {
            return dumpAppLogText(fd, pw, args, opti);
        }
        return false;
    }

    private boolean dumpAppLogText(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (opti < args.length) {
            String dumpPackage = args[opti];
            int opti2 = opti + 1;
            if (opti2 < args.length) {
                try {
                    int uid = Integer.parseInt(args[opti2]);
                    synchronized (this.mAmService) {
                        ProcessRecord app = this.mAmService.getProcessRecordLocked(dumpPackage, uid);
                        if (app != null) {
                            pw.println("\n** APP LOGGIN in pid " + app.mPid + "[" + dumpPackage + "] **");
                            IApplicationThread thread = app.getThread();
                            if (thread != null) {
                                try {
                                    try {
                                        TransferPipe tp = new TransferPipe();
                                        try {
                                            thread.dumpLogText(tp.getWriteFd());
                                            tp.go(fd);
                                            return true;
                                        } finally {
                                            tp.kill();
                                        }
                                    } catch (IOException e) {
                                        pw.println("Got IoException! " + e);
                                        pw.flush();
                                    }
                                } catch (RemoteException e2) {
                                    pw.println("Got RemoteException! " + e2);
                                    pw.flush();
                                }
                            }
                        } else {
                            pw.println("app-logging: " + dumpPackage + "(" + uid + ") not running.");
                            return false;
                        }
                    }
                } catch (NumberFormatException e3) {
                    pw.println("app-logging: uid format is error, please input integer.");
                    return false;
                }
            } else {
                pw.println("app-logging: no uid specified.");
            }
            return false;
        }
        pw.println("app-logging: no process name specified");
        return false;
    }

    public void dumpLogText(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER LOGGING (dumpsys activity logging)");
        Pair<String, String> pair = new Pair<>("", "");
        Pair<String, String> pair2 = generateGroups((String) pair.first, (String) pair.second, ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK, "DEBUG_BACKGROUND_CHECK");
        Pair<String, String> pair3 = generateGroups((String) pair2.first, (String) pair2.second, ActivityManagerDebugConfig.DEBUG_BROADCAST, "DEBUG_BROADCAST");
        Pair<String, String> pair4 = generateGroups((String) pair3.first, (String) pair3.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND, "DEBUG_BROADCAST_BACKGROUND");
        Pair<String, String> pair5 = generateGroups((String) pair4.first, (String) pair4.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT, "DEBUG_BROADCAST_LIGHT");
        Pair<String, String> pair6 = generateGroups((String) pair5.first, (String) pair5.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL, "DEBUG_BROADCAST_DEFERRAL");
        Pair<String, String> pair7 = generateGroups((String) pair6.first, (String) pair6.second, ActivityManagerDebugConfig.DEBUG_PROVIDER, "DEBUG_PROVIDER");
        Pair<String, String> pair8 = generateGroups((String) pair7.first, (String) pair7.second, ActivityManagerDebugConfig.DEBUG_SERVICE, "DEBUG_SERVICE");
        Pair<String, String> pair9 = generateGroups((String) pair8.first, (String) pair8.second, ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE, "DEBUG_FOREGROUND_SERVICE");
        Pair<String, String> pair10 = generateGroups((String) pair9.first, (String) pair9.second, ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING, "DEBUG_SERVICE_EXECUTING");
        Pair<String, String> pair11 = generateGroups((String) pair10.first, (String) pair10.second, ActivityManagerDebugConfig.DEBUG_ALLOWLISTS, "DEBUG_ALLOWLISTS");
        Pair<String, String> pair12 = generateGroups((String) pair11.first, (String) pair11.second, ActivityTaskManagerDebugConfig.DEBUG_RECENTS, "DEBUG_RECENTS");
        Pair<String, String> pair13 = generateGroups((String) pair12.first, (String) pair12.second, ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS, "DEBUG_RECENTS_TRIM_TASKS");
        Pair<String, String> pair14 = generateGroups((String) pair13.first, (String) pair13.second, ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK, "DEBUG_ROOT_TASK");
        Pair<String, String> pair15 = generateGroups((String) pair14.first, (String) pair14.second, ActivityTaskManagerDebugConfig.DEBUG_SWITCH, "DEBUG_SWITCH");
        Pair<String, String> pair16 = generateGroups((String) pair15.first, (String) pair15.second, ActivityTaskManagerDebugConfig.DEBUG_TRANSITION, "DEBUG_TRANSITION");
        Pair<String, String> pair17 = generateGroups((String) pair16.first, (String) pair16.second, ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY, "DEBUG_VISIBILITY");
        Pair<String, String> pair18 = generateGroups((String) pair17.first, (String) pair17.second, ActivityTaskManagerDebugConfig.DEBUG_APP, "DEBUG_APP");
        Pair<String, String> pair19 = generateGroups((String) pair18.first, (String) pair18.second, ActivityTaskManagerDebugConfig.DEBUG_IDLE, "DEBUG_IDLE");
        Pair<String, String> pair20 = generateGroups((String) pair19.first, (String) pair19.second, ActivityTaskManagerDebugConfig.DEBUG_RELEASE, "DEBUG_RELEASE");
        Pair<String, String> pair21 = generateGroups((String) pair20.first, (String) pair20.second, ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING, "DEBUG_USER_LEAVING");
        Pair<String, String> pair22 = generateGroups((String) pair21.first, (String) pair21.second, ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW, "DEBUG_PERMISSIONS_REVIEW");
        Pair<String, String> pair23 = generateGroups((String) pair22.first, (String) pair22.second, ActivityTaskManagerDebugConfig.DEBUG_RESULTS, "DEBUG_RESULTS");
        Pair<String, String> pair24 = generateGroups((String) pair23.first, (String) pair23.second, ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS, "DEBUG_ACTIVITY_STARTS");
        Pair<String, String> pair25 = generateGroups((String) pair24.first, (String) pair24.second, ActivityTaskManagerDebugConfig.DEBUG_CLEANUP, "DEBUG_CLEANUP");
        Pair<String, String> pair26 = generateGroups((String) pair25.first, (String) pair25.second, ActivityTaskManagerDebugConfig.DEBUG_METRICS, "DEBUG_METRICS");
        pw.println("Enabled log groups:");
        pw.println((String) pair26.first);
        pw.println();
        pw.println("Disabled log groups:");
        pw.println((String) pair26.second);
    }

    private Pair<String, String> generateGroups(String enabled, String disabled, boolean config, String group) {
        if (config) {
            enabled = enabled + (TextUtils.isEmpty(enabled) ? group : " " + group);
        } else {
            disabled = disabled + (TextUtils.isEmpty(disabled) ? group : " " + group);
        }
        return new Pair<>(enabled, disabled);
    }

    public boolean checkAppDisableStatus(String packageName) {
        if (!SystemProperties.getBoolean(PROP_DISABLE_AUTORESTART_APP_PREFIX + packageName, false)) {
            return false;
        }
        Slog.w(TAG, "Disable App [" + packageName + "] auto start!");
        return true;
    }

    public boolean isSystemApp(int pid) {
        boolean z;
        synchronized (this.mAmService.mPidsSelfLocked) {
            ProcessRecord app = this.mAmService.mPidsSelfLocked.get(pid);
            z = (app == null || app.info == null || !app.info.isSystemApp()) ? false : true;
        }
        return z;
    }

    public String getPackageNameForPid(int pid) {
        String str;
        synchronized (this.mAmService.mPidsSelfLocked) {
            ProcessRecord app = this.mAmService.mPidsSelfLocked.get(pid);
            str = (app == null || app.info == null) ? null : app.info.packageName;
        }
        return str;
    }

    public void notifyExcuteServices(ProcessRecord app) {
        GreezeManagerInternal greezeManagerInternal = this.greezer;
        if (greezeManagerInternal != null) {
            greezeManagerInternal.notifyExcuteServices(app.uid);
        }
    }

    public void backupBind(int uid, boolean start) {
        if (this.greezer == null || uid < 10000 || uid > 19999) {
            return;
        }
        if (!this.mBackupingList.contains(Integer.valueOf(uid)) && start) {
            this.mBackupingList.add(Integer.valueOf(uid));
            this.greezer.notifyBackup(uid, true);
        } else if (this.mBackupingList.contains(Integer.valueOf(uid)) && !start) {
            this.mBackupingList.remove(Integer.valueOf(uid));
            Bundle b = new Bundle();
            b.putInt(SlaDbSchema.SlaTable.Uidlist.UID, uid);
            b.putBoolean("start", false);
            PowerKeeperPolicy.getInstance().notifyEvent(18, b);
        }
    }

    public boolean isBackuping(int uid) {
        return this.mBackupingList.contains(Integer.valueOf(uid));
    }

    public void setActiveInstrumentation(ComponentName instr) {
        if (instr != null) {
            String mInstrPkg = instr.getPackageName();
            if (mInstrPkg == null) {
                return;
            }
            try {
                PackageManager mPm = this.mContext.getPackageManager();
                ApplicationInfo info = mPm.getApplicationInfo(mInstrPkg, 0);
                this.mInstrUid = info != null ? info.uid : -1;
                return;
            } catch (Exception e) {
                return;
            }
        }
        this.mInstrUid = -1;
    }

    public boolean isActiveInstruUid(int uid) {
        return uid == this.mInstrUid;
    }

    public void setVibratorState(int uid, boolean active) {
        if (active) {
            this.mUsingVibratorUids.add(Integer.valueOf(uid));
        } else {
            this.mUsingVibratorUids.remove(Integer.valueOf(uid));
        }
    }

    public boolean isVibratorActive(int uid) {
        return this.mUsingVibratorUids.contains(Integer.valueOf(uid));
    }

    public void onGrantUriPermission(String sourcePkg, String targetPkg, int targetUid, GrantUri grantUri) {
        recordAppBehavior(sourcePkg, targetPkg, targetUid, grantUri.toString());
    }

    private void recordAppBehavior(String sourcePkg, String targetPkg, int targetUid, String grantUri) {
        if (UserHandle.getAppId(targetUid) >= 10000 && isSystem(sourcePkg, UserHandle.getUserId(Binder.getCallingUid())) && !isSystem(targetPkg, UserHandle.getUserId(targetUid))) {
            if (this.mSecurityInternal == null) {
                this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
            }
            SecurityManagerInternal securityManagerInternal = this.mSecurityInternal;
            if (securityManagerInternal != null) {
                securityManagerInternal.recordAppBehaviorAsync(30, targetPkg, 1L, sourcePkg + "@" + grantUri);
            }
        }
    }

    private static boolean isSystem(String packageName, int userId) {
        PackageManagerInternal internal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        ApplicationInfo applicationInfo = internal.getApplicationInfo(packageName, 0L, 1000, userId);
        return applicationInfo != null && (applicationInfo.isSystemApp() || UserHandle.getAppId(applicationInfo.uid) < 10000);
    }

    public void enableAppDebugConfig(ActivityManagerService activityManagerService, String config, boolean enable, String processName, int uid) {
        if (TextUtils.isEmpty(config) || TextUtils.isEmpty(processName)) {
            return;
        }
        int callerUid = Binder.getCallingUid();
        if (callerUid == 1000 || callerUid == 0 || callerUid == 2000) {
            synchronized (this) {
                ProcessRecord app = activityManagerService.getProcessRecordLocked(processName, uid);
                if (app != null) {
                    IApplicationThread thread = app.getThread();
                    if (thread != null) {
                        try {
                            thread.enableDebugConfig(config, enable);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
            return;
        }
        Slog.e(TAG, callerUid + " can not enable activity thread debug config.");
    }

    public void recordBroadcastLog(ProcessRecord callerApp, String action, String callerPackage, int callingUid) {
        String callers;
        if (callerApp != null) {
            StringBuilder append = new StringBuilder().append("Sending non-protected broadcast ").append(action).append(" from system ").append(callerApp.toShortString()).append(" pkg ").append(callerPackage).append(". Callers=");
            if (callerPackage == null || "android".equals(callerPackage)) {
                callers = Debug.getCallers(5);
            } else {
                callers = "";
            }
            Log.w(TAG, append.append(callers).toString());
            return;
        }
        Log.w(TAG, "Sending non-protected broadcast " + action + " from system uid " + UserHandle.formatUid(callingUid) + " pkg " + callerPackage + ". Callers=" + Debug.getCallers(5));
    }

    public int getDefaultMaxCachedProcesses() {
        if (SystemProperties.getBoolean("persist.sys.spc.enabled", false)) {
            return 1000;
        }
        long totalMemByte = Process.getTotalMemory() / FormatBytesUtil.GB;
        if (totalMemByte < 2) {
            return 12;
        }
        if (totalMemByte <= 3) {
            return 16;
        }
        if (totalMemByte <= 4) {
            return 24;
        }
        return 32;
    }

    public void notifyCrashToVkService(String packageName, boolean inNative, String stackTrace) {
        if (this.mForceVkInternal == null) {
            this.mForceVkInternal = (MiuiForceVkServiceInternal) LocalServices.getService(MiuiForceVkServiceInternal.class);
        }
        MiuiForceVkServiceInternal miuiForceVkServiceInternal = this.mForceVkInternal;
        if (miuiForceVkServiceInternal != null) {
            miuiForceVkServiceInternal.onAppCrashed(packageName, inNative, stackTrace);
        }
    }

    public boolean skipFrozenServiceTimeout(ProcessRecord app, boolean fg) {
        GreezeManagerInternal greezeManagerInternal = this.greezer;
        if (greezeManagerInternal != null && app != null && greezeManagerInternal.isUidFrozen(app.uid)) {
            Slog.d(TAG, "Skip Frozen Uid: " + app.uid + " service Timeout! fg: " + fg);
            return true;
        }
        return false;
    }

    public void notifyDumpAppInfo(int uid, int pid) {
        GreezeManagerInternal greezeManagerInternal = this.greezer;
        if (greezeManagerInternal != null) {
            greezeManagerInternal.notifyDumpAppInfo(uid, pid);
        }
    }

    public void notifyDumpAllInfo() {
        GreezeManagerInternal greezeManagerInternal = this.greezer;
        if (greezeManagerInternal != null) {
            greezeManagerInternal.notifyDumpAllInfo();
        }
    }

    public String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(b & 255);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
