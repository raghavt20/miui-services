package com.miui.server.greeze;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IActivityTaskManager;
import android.app.IAlarmManager;
import android.app.IProcessObserver;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Singleton;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.app.ProcessMap;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.location.mnlutils.MnlConfigUtils$$ExternalSyntheticLambda0;
import com.android.server.power.PowerManagerServiceStub;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.server.AccessController;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.xiaomi.abtest.d.d;
import database.SlaDbSchema.SlaDbSchema;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.greeze.IGreezeCallback;
import miui.greeze.IGreezeManager;
import miui.greeze.IMonitorToken;
import miui.process.ActiveUidInfo;
import miui.process.ForegroundInfo;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class GreezeManagerService extends IGreezeManager.Stub {
    public static final int BINDER_STATE_IN_BUSY = 1;
    public static final int BINDER_STATE_IN_IDLE = 0;
    public static final int BINDER_STATE_IN_TRANSACTION = 4;
    public static final int BINDER_STATE_PROC_IN_BUSY = 3;
    public static final int BINDER_STATE_THREAD_IN_BUSY = 2;
    private static final String CLOUD_AUROGON_ALARM_ALLOW_LIST = "cloud_aurogon_alarm_allow_list";
    private static final String CLOUD_GREEZER_ENABLE = "cloud_greezer_enable";
    private static final int CMD_ADJ_ADD = 0;
    private static final int CMD_ADJ_REMOVE = 1;
    private static final int CMD_ADJ_THAWALL = 2;
    private static final int DUMPSYS_HISTORY_DURATION = 14400000;
    private static final int HISTORY_SIZE;
    private static final Singleton<IGreezeManager> IGreezeManagerSingleton;
    private static final long LOOPONCE_DELAY_TIME = 5000;
    private static final int MAX_HISTORY_ITEMS = 4096;
    private static final long MILLET_DELAY_CEILING = 10000;
    private static final long MILLET_DELAY_THRASHOLD = 50;
    private static final int MILLET_MONITOR_ALL = 7;
    private static final int MILLET_MONITOR_BINDER = 1;
    private static final int MILLET_MONITOR_NET = 4;
    private static final int MILLET_MONITOR_SIGNAL = 2;
    public static final String SERVICE_NAME = "greezer";
    public static final String TAG = "GreezeManager";
    private static final String TIME_FORMAT_PATTERN = "HH:mm:ss.SSS";
    static HashMap<Integer, IGreezeCallback> callbacks;
    private static List<String> mAllowBroadcast;
    private static List<String> mCNDeferBroadcast;
    private static List<String> mMiuiDeferBroadcast;
    private static List<String> mNeedCachedBroadcast;
    private AlarmManager am;
    private ConnectivityManager cm;
    private boolean isBarExpand;
    private ActivityManager mActivityManager;
    private final ActivityManagerService mActivityManagerService;
    IActivityTaskManager mActivityTaskManager;
    IAlarmManager mAlarmManager;
    private List<String> mAurogonAlarmAllowList;
    private List<String> mAurogonAlarmForbidList;
    public String mBGOrderBroadcastAction;
    public int mBGOrderBroadcastAppUid;
    private boolean mBroadcastCtrlCloud;
    private Map<String, String> mBroadcastCtrlMap;
    public List<String> mBroadcastIntentDenyList;
    public Map<Integer, List<Intent>> mCachedBCList;
    final ContentObserver mCloudAurogonAlarmListObserver;
    private Context mContext;
    DisplayManager mDisplayManager;
    DisplayManagerInternal mDisplayManagerInternal;
    public List<Integer> mExcuteServiceList;
    public String mFGOrderBroadcastAction;
    public int mFGOrderBroadcastAppUid;
    public List<String> mFreeformSmallWinList;
    private Method mGetCastPid;
    public Handler mHandler;
    private LocalLog mHistoryLog;
    public AurogonImmobulusMode mImmobulusMode;
    public boolean mInFreeformSmallWinMode;
    private PackageManager mPm;
    private final IProcessObserver mProcessObserver;
    public List<Integer> mRecentLaunchAppList;
    private int mRegisteredMonitor;
    public boolean mScreenOnOff;
    private int mSystemUiPid;
    private ServiceThread mThread;
    public String mTopAppPackageName;
    public int mTopAppUid;
    private final IUidObserver mUidObserver;
    public IWindowManager mWindowManager;
    private Object powerManagerServiceImpl;
    private List<String> ENABLE_LAUNCH_MODE_DEVICE = new ArrayList(Arrays.asList("thor", "dagu", "zizhan", "unicorn", "mayfly", "cupid", "zeus", "nuwa", "fuxi", "socrates", "marble", "liuqin", "pipa", "ishtar", "daumier", "matisse", "rubens", "xaga", "yuechu", "sky", "pearl", "babylon", "selene", "veux", "earth", "evergo", "corot", "yudi", "xun", "river", "shennong", "houji", "manet", "vermeer", "duchamp", "mondrian", "sheng", "aurora", "ruyi", "chenfeng", "garnet", "zircon", "dizi", "goku", "peridot", "breeze", "ruan"));
    public List<String> DISABLE_IMMOB_MODE_DEVICE = new ArrayList(Arrays.asList("sky", "river", "breeze"));
    private List<String> ENABLE_VIDEO_MODE_DEVICE = new ArrayList(Arrays.asList("liuqin", "pipa", "fuxi", "yuechu", "yudi", "shennong", "houji", "sheng", "aurora", "chenfeng"));
    private final SparseArray<FrozenInfo> mFrozenPids = new SparseArray<>();
    private String mAudioZeroPkgs = "com.tencent.lolm, com.netease.dwrg, com.tencent.tmgp.pubgm, com.tencent.tmgp.dwrg, com.tencent.tmgp.pubgmhd, com.tencent.tmgp.sgame, com.netease.dwrg.mi, com.duowan.kiwi";
    private Map<String, List<String>> mImmobulusModeWhiteList = new HashMap();
    private List<String> mActivityCtrlBlackList = new ArrayList(Arrays.asList("com.tencent.android.qqdownloader"));
    private Map<String, List<String>> mBroadcastTargetWhiteList = new HashMap();
    private Map<String, List<String>> mBroadcastCallerWhiteList = new HashMap();
    private FrozenInfo[] mFrozenHistory = new FrozenInfo[HISTORY_SIZE];
    private int mHistoryIndexNext = 0;
    public Object mAurogonLock = new Object();
    private boolean mInited = true;
    private boolean mFsgNavBar = true;
    private final SparseArray<IMonitorToken> mMonitorTokens = new SparseArray<>();
    private AlarmListener mAlarmLoop = new AlarmListener();
    private int mLoopCount = 0;
    private SparseArray<List<Integer>> isoPids = new SparseArray<>();
    private boolean mInitCtsStatused = false;
    private Set<Integer> mUidAdjs = new HashSet();
    private INetworkManagementService mNms = null;

    private static native void nAddConcernedUid(int i);

    private static native void nClearConcernedUid();

    private static native void nDelConcernedUid(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nLoopOnce();

    private static native void nQueryBinder(int i);

    static {
        HISTORY_SIZE = GreezeManagerDebugConfig.DEBUG_MONKEY ? 16384 : 4096;
        mNeedCachedBroadcast = new ArrayList(Arrays.asList("android.intent.action.SCREEN_OFF", "android.intent.action.SCREEN_ON", "android.intent.action.ACTION_POWER_DISCONNECTED", "android.intent.action.ACTION_POWER_CONNECTED", "android.os.action.DEVICE_IDLE_MODE_CHANGED", "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED", "com.ss.android.ugc.aweme.search.widget.APPWIDGET_RESET_ALARM", "com.tencent.mm.TrafficStatsReceiver", "com.xiaomi.mipush.MESSAGE_ARRIVED", "com.tencent.mm.plugin.report.service.KVCommCrossProcessReceiver", "android.intent.action.wutao"));
        mAllowBroadcast = new ArrayList(Arrays.asList("android.intent.action.MY_PACKAGE_REPLACED"));
        mMiuiDeferBroadcast = new ArrayList(Arrays.asList("android.intent.action.BATTERY_CHANGED"));
        mCNDeferBroadcast = new ArrayList(Arrays.asList("com.google.android.intent.action.GCM_RECONNECT", "com.google.android.gcm.DISCONNECTED", "com.google.android.gcm.CONNECTED", "com.google.android.gms.gcm.HEARTBEAT_ALARM"));
        IGreezeManagerSingleton = new Singleton<IGreezeManager>() { // from class: com.miui.server.greeze.GreezeManagerService.2
            /* JADX INFO: Access modifiers changed from: protected */
            public IGreezeManager create() {
                IBinder b = ServiceManager.getService(GreezeManagerService.SERVICE_NAME);
                IGreezeManager service = IGreezeManager.Stub.asInterface(b);
                return service;
            }
        };
        callbacks = new HashMap<>();
    }

    private void registerCloudObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.miui.server.greeze.GreezeManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri == null) {
                    return;
                }
                if (uri.equals(Settings.System.getUriFor(GreezeManagerService.CLOUD_GREEZER_ENABLE))) {
                    GreezeManagerDebugConfig.sEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), GreezeManagerService.CLOUD_GREEZER_ENABLE, -2));
                    Slog.w(GreezeManagerService.TAG, "cloud control set received :" + GreezeManagerDebugConfig.sEnable);
                } else if (uri.equals(Settings.Global.getUriFor("zeropkgs"))) {
                    GreezeManagerService.this.mAudioZeroPkgs = Settings.Global.getStringForUser(context.getContentResolver(), "zeropkgs", -2);
                    Slog.w(GreezeManagerService.TAG, "mAudioZeroPkgs :" + GreezeManagerService.this.mAudioZeroPkgs);
                } else if (uri.equals(Settings.Global.getUriFor("force_fsg_nav_bar"))) {
                    GreezeManagerService.this.getNavBarInfo(context);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_GREEZER_ENABLE), false, observer, -2);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zeropkgs"), false, observer, -2);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, observer, -2);
        String data = Settings.Global.getStringForUser(context.getContentResolver(), "zeropkgs", -2);
        if (data != null && data.length() > 3) {
            this.mAudioZeroPkgs = data;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getNavBarInfo(Context context) {
        String val = Settings.Global.getStringForUser(context.getContentResolver(), "force_fsg_nav_bar", -2);
        if ("0".equals(val)) {
            this.mFsgNavBar = false;
            Slog.w(TAG, "mFsgNavBar :" + this.mFsgNavBar);
            this.mSystemUiPid = getSystemUiPid();
            return;
        }
        this.mFsgNavBar = true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public GreezeManagerService(Context context) {
        Object[] objArr = 0;
        this.mImmobulusMode = null;
        this.cm = null;
        this.am = null;
        IUidObserver.Stub stub = new IUidObserver.Stub() { // from class: com.miui.server.greeze.GreezeManagerService.8
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            }

            public void onUidActive(final int uid) {
                GreezeManagerService.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.8.1
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mImmobulusMode.notifyAppActive(uid);
                    }
                });
            }

            public void onUidGone(int uid, boolean disabled) {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid, int adj) {
            }
        };
        this.mUidObserver = stub;
        IProcessObserver.Stub stub2 = new IProcessObserver.Stub() { // from class: com.miui.server.greeze.GreezeManagerService.9
            public void onForegroundActivitiesChanged(int pid, final int uid, boolean foregroundActivities) {
                if (GreezeManagerService.this.mInited) {
                    if (foregroundActivities) {
                        Slog.d("Aurogon", " uid = " + uid + " switch to FG");
                        if (GreezeManagerService.this.isUidFrozen(uid)) {
                            GreezeManagerService.this.thawUid(uid, 1000, "FG activity");
                        }
                        GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.1
                            @Override // java.lang.Runnable
                            public void run() {
                                GreezeManagerService.this.updateAurogonUidRule(uid, false);
                                AurogonDownloadFilter.getInstance().setMoveToFgApp(uid);
                                String packageName = GreezeManagerService.this.getPackageNameFromUid(uid);
                                if (packageName == null) {
                                    return;
                                }
                                if ((!GreezeManagerService.this.mImmobulusMode.isSystemOrMiuiImportantApp(packageName) || "com.xiaomi.market".equals(packageName)) && UserHandle.isApp(uid)) {
                                    AurogonDownloadFilter.getInstance().addAppNetCheckList(uid, packageName);
                                }
                                if (GreezeManagerService.this.mImmobulusMode.mEnterIMCamera && uid != GreezeManagerService.this.mImmobulusMode.mCameraUid) {
                                    GreezeManagerService.this.mImmobulusMode.quitImmobulusMode();
                                    GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = false;
                                }
                                if (GreezeManagerService.this.ENABLE_VIDEO_MODE_DEVICE.contains(Build.DEVICE)) {
                                    if (GreezeManagerService.this.mImmobulusMode.mEnterIMVideo) {
                                        GreezeManagerService.this.mImmobulusMode.triggerVideoMode(false, packageName, uid);
                                    }
                                    if (GreezeManagerService.this.mImmobulusMode.mImmobulusModeEnabled && GreezeManagerService.this.mImmobulusMode.mVideoAppList.contains(packageName)) {
                                        GreezeManagerService.this.mImmobulusMode.triggerVideoMode(true, packageName, uid);
                                    }
                                }
                            }
                        });
                        return;
                    }
                    Slog.d("Aurogon", " uid = " + uid + " switch to BG");
                    GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.2
                        @Override // java.lang.Runnable
                        public void run() {
                            String packageName;
                            if (GreezeManagerService.this.mScreenOnOff && (packageName = GreezeManagerService.this.getPackageNameFromUid(uid)) != null) {
                                GreezeManagerService.this.updateFreeformSmallWinList(packageName, false);
                            }
                            GreezeManagerService.this.mImmobulusMode.notifyAppSwitchToBg(uid);
                        }
                    });
                }
            }

            public void onForegroundServicesChanged(final int pid, final int uid, int serviceTypes) {
                GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.3
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mImmobulusMode.notifyFgServicesChanged(pid, uid);
                    }
                });
            }

            public void onProcessDied(int pid, int uid) {
            }
        };
        this.mProcessObserver = stub2;
        this.mFreeformSmallWinList = new ArrayList();
        this.mInFreeformSmallWinMode = false;
        this.mFGOrderBroadcastAction = "";
        this.mBGOrderBroadcastAction = "";
        this.mFGOrderBroadcastAppUid = -1;
        this.mBGOrderBroadcastAppUid = -1;
        this.mHistoryLog = new LocalLog(4096);
        this.mTopAppUid = -1;
        this.mTopAppPackageName = "";
        this.mPm = null;
        this.mAurogonAlarmAllowList = new ArrayList(Arrays.asList("com.tencent.mm", "com.tencent.mobileqq"));
        this.mAurogonAlarmForbidList = new ArrayList(Arrays.asList("com.miui.player"));
        this.mRecentLaunchAppList = new ArrayList();
        this.mAlarmManager = null;
        this.mWindowManager = null;
        this.mScreenOnOff = true;
        this.mDisplayManagerInternal = null;
        this.mBroadcastIntentDenyList = new ArrayList(Arrays.asList("android.intent.action.BATTERY_CHANGED", "android.net.wifi.STATE_CHANGE", "android.intent.action.DROPBOX_ENTRY_ADDED", "android.net.wifi.RSSI_CHANGED", "android.net.wifi.supplicant.STATE_CHANGE", "com.android.server.action.NETWORK_STATS_UPDATED", "android.intent.action.CLOSE_SYSTEM_DIALOGS", "android.intent.action.TIME_TICK", "android.net.conn.CONNECTIVITY_CHANGE", "android.net.wifi.WIFI_STATE_CHANGED", "android.net.wifi.SCAN_RESULTS"));
        this.mCloudAurogonAlarmListObserver = new ContentObserver(this.mHandler) { // from class: com.miui.server.greeze.GreezeManagerService.13
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                String str;
                if (uri != null && uri.equals(Settings.System.getUriFor(GreezeManagerService.CLOUD_AUROGON_ALARM_ALLOW_LIST)) && (str = Settings.System.getStringForUser(GreezeManagerService.this.mContext.getContentResolver(), GreezeManagerService.CLOUD_AUROGON_ALARM_ALLOW_LIST, -2)) != null) {
                    String[] pkgName = str.split(",");
                    if (pkgName.length > 0) {
                        GreezeManagerService.this.mAurogonAlarmAllowList.clear();
                        for (String name : pkgName) {
                            GreezeManagerService.this.mAurogonAlarmAllowList.add(name);
                        }
                    }
                }
            }
        };
        this.mExcuteServiceList = new ArrayList();
        this.isBarExpand = false;
        this.mSystemUiPid = -1;
        this.mBroadcastCtrlCloud = true;
        this.mBroadcastCtrlMap = new HashMap();
        this.mCachedBCList = new HashMap();
        this.mContext = context;
        this.mThread = GreezeThread.getInstance();
        H h = new H(this.mThread.getLooper());
        this.mHandler = h;
        h.sendEmptyMessage(3);
        ActivityManagerService service = ActivityManager.getService();
        this.mActivityManagerService = service;
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (GreezeManagerDebugConfig.sEnable) {
            registerCloudObserver(context);
            if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_GREEZER_ENABLE, -2) != null) {
                GreezeManagerDebugConfig.sEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), CLOUD_GREEZER_ENABLE, -2));
            }
        }
        registerObserverForAurogon();
        getWindowManagerService();
        getAlarmManagerService();
        try {
            service.registerProcessObserver(stub2);
            service.registerUidObserver(stub, 14, -1, (String) null);
        } catch (Exception e) {
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        new AurogonBroadcastReceiver();
        this.mImmobulusMode = new AurogonImmobulusMode(this.mContext, this.mThread, this);
        this.cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        LocalServices.addService(GreezeManagerInternal.class, new LocalService());
        initArgs();
        this.am = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        registerDisplayChanageListener();
        if (GreezeManagerDebugConfig.mPowerMilletEnable) {
            SystemProperties.set("ctl.start", "millet_binder");
            SystemProperties.set("ctl.start", "millet_pkg");
            SystemProperties.set("ctl.start", "millet_sig");
        }
    }

    private void initArgs() {
        List<String> actions = new ArrayList<>();
        actions.add("android.net.wifi.STATE_CHANGE");
        actions.add("android.net.conn.CONNECTIVITY_CHANGE");
        for (int i = 0; i < AurogonImmobulusMode.mMessageApp.size(); i++) {
            String pkgName = AurogonImmobulusMode.mMessageApp.get(i);
            this.mBroadcastTargetWhiteList.put(pkgName, actions);
        }
        List<String> callerAction = new ArrayList<>();
        callerAction.add("android.appwidget.action.APPWIDGET_UPDATE");
        callerAction.add("android.appwidget.action.APPWIDGET_ENABLED");
        this.mBroadcastCallerWhiteList.put("android", callerAction);
        List<String> immobulusModeActions = new ArrayList<>();
        immobulusModeActions.add("*");
        this.mImmobulusModeWhiteList.put("com.xiaomi.metoknlp", immobulusModeActions);
        getNavBarInfo(this.mContext);
    }

    private void sendMilletLoop() {
        this.mHandler.sendEmptyMessageDelayed(4, LOOPONCE_DELAY_TIME);
    }

    public static GreezeManagerService getService() {
        return (GreezeManagerService) IGreezeManagerSingleton.get();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void startService() {
        if (getService() != null && SystemProperties.getBoolean("persist.sys.millet.handshake", false)) {
            getService().sendMilletLoop();
            GreezeManagerDebugConfig.mCgroupV1Flag = SystemProperties.getBoolean("persist.sys.millet.cgroup", false);
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final GreezeManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new GreezeManagerService(context);
        }

        public void onStart() {
            publishBinderService(GreezeManagerService.SERVICE_NAME, this.mService);
            GreezeManagerService.startService();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPermission() {
        int uid = Binder.getCallingUid();
        if (!UserHandle.isApp(uid)) {
        } else {
            throw new SecurityException("Uid " + uid + " does not have permission to greezer");
        }
    }

    /* loaded from: classes.dex */
    class MonitorDeathRecipient implements IBinder.DeathRecipient {
        IMonitorToken mMonitorToken;
        int mType;

        MonitorDeathRecipient(IMonitorToken token, int type) {
            this.mMonitorToken = token;
            this.mType = type;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (GreezeManagerService.this.mMonitorTokens) {
                GreezeManagerService.this.mMonitorTokens.remove(this.mType);
            }
            GreezeManagerService.this.mHandler.sendEmptyMessage(3);
            int i = this.mType;
            if (i == 1) {
                GreezeManagerService.this.mRegisteredMonitor &= -2;
            } else if (i == 2) {
                GreezeManagerService.this.mRegisteredMonitor &= -3;
            } else if (i == 3) {
                GreezeManagerService.this.mRegisteredMonitor &= -5;
            }
            ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).powerFrozenServiceReady(false);
            if ((GreezeManagerService.this.mRegisteredMonitor & 7) != 7) {
                GreezeManagerDebugConfig.milletEnable = false;
                for (IGreezeCallback callback : GreezeManagerService.callbacks.values()) {
                    try {
                        callback.serviceReady(GreezeManagerDebugConfig.milletEnable);
                    } catch (RemoteException e) {
                    }
                }
            }
            Slog.w(GreezeManagerService.TAG, "Monitor (type " + this.mType + ") died, gz stop");
            if (this.mType == 2) {
                GreezeManagerService.this.setLoopAlarm();
                GreezeManagerService.this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.MonitorDeathRecipient.1
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mHandler.sendEmptyMessage(4);
                    }
                }, 600L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLoopAlarm() {
        if (this.am == null) {
            Slog.d(TAG, "setLoopAlarm am == null");
        } else {
            Slog.d(TAG, "setLoopAlarm am cnt = " + this.mLoopCount);
            this.am.setExact(3, 300000 + SystemClock.elapsedRealtime(), "monitorloop", this.mAlarmLoop, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AlarmListener implements AlarmManager.OnAlarmListener {
        private AlarmListener() {
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            if ((GreezeManagerService.this.mRegisteredMonitor & 7) == 7) {
                Slog.d(GreezeManagerService.TAG, "loop handshake restore");
                return;
            }
            GreezeManagerService.this.mLoopCount++;
            if (GreezeManagerService.this.mLoopCount % 2 == 0) {
                SystemProperties.set("sys.millet.monitor", "1");
            } else {
                GreezeManagerService.this.mHandler.sendEmptyMessage(4);
            }
            GreezeManagerService.this.setLoopAlarm();
        }
    }

    public boolean registerMonitor(IMonitorToken token, int type) throws RemoteException {
        checkPermission();
        Slog.i(TAG, "Monitor registered, type " + type + " pid " + getCallingPid());
        this.mHandler.sendEmptyMessage(3);
        synchronized (this.mMonitorTokens) {
            this.mMonitorTokens.put(type, token);
            token.asBinder().linkToDeath(new MonitorDeathRecipient(token, type), 0);
        }
        if (type == 1) {
            this.mRegisteredMonitor |= 1;
        } else if (type == 2) {
            this.mRegisteredMonitor |= 2;
        } else if (type == 3) {
            this.mRegisteredMonitor |= 4;
        }
        if ((this.mRegisteredMonitor & 7) == 7) {
            AlarmManager alarmManager = this.am;
            if (alarmManager != null) {
                alarmManager.cancel(this.mAlarmLoop);
            }
            this.mLoopCount = 0;
            Slog.i(TAG, "All monitors registered, about to loop once");
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public FrozenInfo getFrozenInfo(int uid) {
        FrozenInfo info = null;
        synchronized (this.mFrozenPids) {
            int i = 0;
            while (true) {
                if (i < this.mFrozenPids.size()) {
                    FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                    if (frozen == null || frozen.uid != uid) {
                        i++;
                    } else {
                        info = frozen;
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return info;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getInfoUid(int pid) {
        int infoUid = -1;
        synchronized (this.isoPids) {
            int i = this.isoPids.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                List<Integer> pids = this.isoPids.valueAt(i);
                if (pids != null && pids.contains(Integer.valueOf(pid))) {
                    infoUid = this.isoPids.keyAt(i);
                    break;
                }
                i--;
            }
        }
        return infoUid;
    }

    public void reportSignal(final int uid, final int pid, long now) {
        checkPermission();
        ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).reportSignal(uid, pid, now);
        long delay = SystemClock.uptimeMillis() - now;
        String msg = "Receive frozen signal: uid=" + uid + " pid=" + pid + " delay=" + delay + "ms";
        if (GreezeManagerDebugConfig.DEBUG_MILLET) {
            Slog.i(TAG, msg);
        }
        if (delay > MILLET_DELAY_THRASHOLD && delay < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg);
        }
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                boolean isIsoUid = Process.isIsolated(uid);
                int infoUid = -1;
                if (isIsoUid) {
                    infoUid = GreezeManagerService.this.getInfoUid(pid);
                    if (GreezeManagerDebugConfig.DEBUG_MILLET) {
                        Slog.d(GreezeManagerService.TAG, "reportSig infoUid = " + infoUid);
                    }
                }
                synchronized (GreezeManagerService.this.mFrozenPids) {
                    if (GreezeManagerService.this.mFrozenPids.get(Integer.valueOf(pid).intValue()) == null) {
                        Slog.d(GreezeManagerService.TAG, " report signal uid = " + uid + " pid = " + pid + "is not frozen.");
                        return;
                    }
                    if (isIsoUid && infoUid > 0) {
                        GreezeManagerService.this.thawUidAsync(infoUid, 1000, "Signal iso");
                    }
                    if (GreezeManagerService.this.readPidStatus(pid)) {
                        Slog.d(GreezeManagerService.TAG, " report signal uid = " + uid + " pid = " + pid + "is not died.");
                        GreezeManagerService.this.thawUidAsync(uid, 1000, "Signal other");
                        return;
                    }
                    GreezeManagerService.this.mFrozenPids.remove(Integer.valueOf(pid).intValue());
                    FrozenInfo info = GreezeManagerService.this.getFrozenInfo(uid);
                    if (info == null) {
                        String isolatedPid = "";
                        if (GreezeManagerDebugConfig.mCgroupV1Flag && UserHandle.isApp(uid)) {
                            FreezeUtils.thawPid(pid);
                            isolatedPid = " isolatedPid = " + pid;
                        }
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.w(GreezeManagerService.TAG, "reportSignal null uid = " + uid + isolatedPid);
                        }
                        GreezeManagerService.this.removePendingBroadcast(uid);
                        StringBuilder log = new StringBuilder();
                        log.append("Died uid = " + uid);
                        GreezeManagerService.this.mHistoryLog.log(log.toString());
                    }
                }
            }
        }, 200L);
    }

    public void reportNet(int uid, long now) {
        checkPermission();
        ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).reportNet(uid, now);
        long delay = SystemClock.uptimeMillis() - now;
        String msg = "Receive frozen pkg net: uid=" + uid + " delay=" + delay + "ms";
        if (GreezeManagerDebugConfig.DEBUG_MILLET) {
            Slog.i(TAG, msg);
        }
        if (delay > MILLET_DELAY_THRASHOLD && delay < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg);
        }
        synchronized (this.mFrozenPids) {
            FrozenInfo info = getFrozenInfo(uid);
            if (info == null) {
                try {
                    if (this.mTopAppUid == uid) {
                        callbacks.get(1).reportNet(uid, now);
                    }
                } catch (RemoteException e) {
                }
                if (GreezeManagerDebugConfig.DEBUG) {
                    Slog.w(TAG, "reportNet null uid = " + uid);
                }
                return;
            }
            info.getOwner();
            if (GreezeManagerDebugConfig.mPowerMilletEnable) {
                if (info.isFrozenByLaunchMode) {
                    this.mImmobulusMode.addLaunchModeQiutList(uid);
                } else {
                    thawUid(uid, 1000, "PACKET");
                }
            }
        }
    }

    public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, int buffer) throws RemoteException {
        int owner;
        FrozenInfo info;
        String msg;
        long delay;
        checkPermission();
        ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, buffer);
        long delay2 = SystemClock.uptimeMillis() - now;
        String msg2 = "Receive frozen binder trans: dstUid=" + dstUid + " dstPid=" + dstPid + " callerUid=" + callerUid + " callerPid=" + callerPid + " callerTid=" + callerTid + " delay=" + delay2 + "ms oneway=" + isOneway;
        if (GreezeManagerDebugConfig.DEBUG_MILLET) {
            Slog.i(TAG, msg2);
        }
        if (delay2 > MILLET_DELAY_THRASHOLD && delay2 < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg2);
        }
        synchronized (this.mFrozenPids) {
            try {
                FrozenInfo info2 = getFrozenInfo(dstUid);
                if (info2 == null) {
                    try {
                        if (GreezeManagerDebugConfig.DEBUG) {
                            Slog.w(TAG, "reportBinderTrans null uid = " + dstUid);
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                    }
                } else {
                    try {
                        int owner2 = info2.getOwner();
                        try {
                            if (callerPid == this.mSystemUiPid && (this.isBarExpand || !this.mFsgNavBar)) {
                                thawUid(info2.uid, 1000, "UI bar");
                                return;
                            }
                            int i = buffer;
                            if (i == 0 && GreezeManagerDebugConfig.mPowerMilletEnable) {
                                thawUid(info2.uid, 1000, "BF");
                                return;
                            }
                            if (!isOneway && GreezeManagerDebugConfig.mPowerMilletEnable) {
                                thawUid(info2.uid, 1000, "Sync Binder");
                                return;
                            }
                            for (Map.Entry<Integer, IGreezeCallback> item : callbacks.entrySet()) {
                                try {
                                    if (item.getKey().intValue() == owner2) {
                                        owner = owner2;
                                        info = info2;
                                        msg = msg2;
                                        delay = delay2;
                                        try {
                                            item.getValue().reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, i);
                                            return;
                                        } catch (RemoteException e) {
                                        }
                                    } else {
                                        owner = owner2;
                                        info = info2;
                                        msg = msg2;
                                        delay = delay2;
                                    }
                                } catch (RemoteException e2) {
                                    owner = owner2;
                                    info = info2;
                                    msg = msg2;
                                    delay = delay2;
                                }
                                i = buffer;
                                owner2 = owner;
                                info2 = info;
                                msg2 = msg;
                                delay2 = delay;
                            }
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
            }
            while (true) {
                try {
                    break;
                } catch (Throwable th5) {
                    th = th5;
                }
            }
            throw th;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:60:0x014a
        	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1166)
        	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:1022)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:55)
        */
    public void reportBinderState(int r19, int r20, int r21, int r22, long r23) {
        /*
            Method dump skipped, instructions count: 333
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.greeze.GreezeManagerService.reportBinderState(int, int, int, int, long):void");
    }

    public void reportLoopOnce() {
        checkPermission();
        ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).powerFrozenServiceReady(true);
        if (GreezeManagerDebugConfig.DEBUG_MILLET) {
            Slog.i(TAG, "Receive millet loop once msg");
        }
        if ((this.mRegisteredMonitor & 7) == 7) {
            if (!this.mInitCtsStatused) {
                this.mImmobulusMode.initCtsStatus();
                this.mInitCtsStatused = true;
            }
            GreezeManagerDebugConfig.milletEnable = true;
            Slog.i(TAG, "Receive millet loop once, gz begin to work");
            for (IGreezeCallback callback : callbacks.values()) {
                try {
                    callback.serviceReady(GreezeManagerDebugConfig.milletEnable);
                } catch (RemoteException e) {
                }
            }
            return;
        }
        Slog.i(TAG, "Receive millet loop once, but monitor not ready");
    }

    public boolean registerCallback(IGreezeCallback callback, int module) throws RemoteException {
        checkPermission();
        if (Binder.getCallingUid() != 1000) {
            return false;
        }
        if (callbacks.getOrDefault(Integer.valueOf(module), null) != null) {
            Slog.i(TAG, "Already registed callback for module: " + module);
        }
        if (module == 4 || module == 3) {
            return false;
        }
        callbacks.put(Integer.valueOf(module), callback);
        callback.asBinder().linkToDeath(new CallbackDeathRecipient(module), 0);
        callback.serviceReady(GreezeManagerDebugConfig.milletEnable);
        return true;
    }

    public long getLastThawedTime(int uid, int module) {
        List<FrozenInfo> historyInfos = getHistoryInfos(System.currentTimeMillis() - 14400000);
        for (FrozenInfo info : historyInfos) {
            if (info.uid == uid) {
                return info.mThawUptime;
            }
        }
        return -1L;
    }

    public boolean isUidFrozen(int uid) {
        synchronized (this.mFrozenPids) {
            int[] frozenUids = getFrozenUids(9999);
            for (int frozenuid : frozenUids) {
                if (frozenuid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isAllowWakeUpList(int uid) {
        String packageName = getPackageNameFromUid(uid);
        if (packageName != null) {
            return this.mImmobulusMode.isAllowWakeUpList(packageName);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CallbackDeathRecipient implements IBinder.DeathRecipient {
        int module;

        CallbackDeathRecipient(int module) {
            this.module = module;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            GreezeManagerService.callbacks.remove(Integer.valueOf(this.module));
            int[] frozenUids = GreezeManagerService.this.getFrozenUids(this.module);
            for (int frozenuid : frozenUids) {
                GreezeManagerService.this.updateAurogonUidRule(frozenuid, false);
            }
            Slog.i(GreezeManagerService.TAG, "module: " + this.module + " has died!");
            GreezeManagerService greezeManagerService = GreezeManagerService.this;
            int i = this.module;
            greezeManagerService.thawAll(i, i, "module died");
        }
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "BINDER_IN_IDLE";
            case 1:
                return "BINDER_IN_BUSY";
            case 2:
                return "BINDER_THREAD_IN_BUSY";
            case 3:
                return "BINDER_PROC_IN_BUSY";
            case 4:
                return "BINDER_IN_TRANSACTION";
            default:
                return Integer.toString(state);
        }
    }

    private void setWakeLockState(List<Integer> ls, boolean disable) {
        for (Integer uid : ls) {
            if (!Process.isIsolated(uid.intValue())) {
                try {
                    PowerManagerServiceStub.get().setUidPartialWakeLockDisabledState(uid.intValue(), (String) null, disable, "greeze");
                } catch (Exception e) {
                    Log.e(TAG, "updateWakelockBlockedUid", e);
                }
            }
        }
    }

    public void monitorNet(int uid) {
        nAddConcernedUid(uid);
    }

    public void clearMonitorNet(int uid) {
        nDelConcernedUid(uid);
    }

    public void clearMonitorNet() {
        nClearConcernedUid();
    }

    public void queryBinderState(int uid) {
        nQueryBinder(uid);
    }

    List<RunningProcess> getProcessByUid(int uid) {
        List<RunningProcess> procs = GreezeServiceUtils.getUidMap().get(uid);
        if (procs != null) {
            return procs;
        }
        return new ArrayList();
    }

    RunningProcess getProcessByPid(int pid) {
        List<RunningProcess> procs = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procs) {
            if (pid == proc.pid) {
                return proc;
            }
        }
        return null;
    }

    ProcessMap<List<RunningProcess>> getPkgMap() {
        ProcessMap<List<RunningProcess>> map = new ProcessMap<>();
        List<RunningProcess> procList = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procList) {
            int uid = proc.uid;
            if (proc.pkgList != null) {
                for (String packageName : proc.pkgList) {
                    List<RunningProcess> procs = (List) map.get(packageName, uid);
                    if (procs == null) {
                        procs = new ArrayList<>();
                        map.put(packageName, uid, procs);
                    }
                    procs.add(proc);
                }
            }
        }
        return map;
    }

    public boolean isUidActive(int uid) {
        try {
            List<ActiveUidInfo> infos = ProcessManager.getActiveUidInfo(3);
            for (ActiveUidInfo info : infos) {
                if (info.uid == uid) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get active audio info. Going to freeze uid" + uid + " regardless of whether it using audio", e);
            return true;
        }
    }

    public void freezeThread(int tid) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            FreezeUtils.freezeTid(tid);
        }
    }

    public boolean freezeProcess(RunningProcess proc, long timeout, int fromWho, String reason) {
        boolean done;
        int pid = proc.pid;
        if (pid <= 0 || Process.myPid() == pid || Process.getUidForPid(pid) != proc.uid) {
            return false;
        }
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            done = FreezeUtils.freezePid(pid);
        } else {
            done = FreezeUtils.freezePid(pid, proc.uid);
        }
        ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).addFrozenPid(proc.uid, pid);
        synchronized (this.mFrozenPids) {
            FrozenInfo info = this.mFrozenPids.get(pid);
            if (info != null && info.uid != proc.uid) {
                Slog.d(TAG, "freeze uid-pid mismatch " + info.toString());
                this.mFrozenPids.remove(pid);
            }
            FrozenInfo info2 = new FrozenInfo(proc);
            this.mFrozenPids.put(pid, info2);
            info2.addFreezeInfo(System.currentTimeMillis(), fromWho, reason);
            if (this.mHandler.hasMessages(1, info2)) {
                this.mHandler.removeMessages(1, info2);
            }
            if (timeout != 0) {
                Message msg = this.mHandler.obtainMessage(1, info2);
                msg.arg1 = pid;
                msg.arg2 = fromWho;
                this.mHandler.sendMessageDelayed(msg, timeout);
            }
        }
        return done;
    }

    public List<Integer> freezePids(int[] pids, long timeout, int fromWho, String reason) {
        List<RunningProcess> procs;
        int i;
        int[] iArr = pids;
        checkPermission();
        if (GreezeManagerDebugConfig.DEBUG_AIDL) {
            Slog.d(TAG, "AIDL freezePids(" + Arrays.toString(pids) + ", " + timeout + ", " + fromWho + ", " + reason + ")");
        }
        if (iArr == null) {
            return new ArrayList();
        }
        List<RunningProcess> procs2 = GreezeServiceUtils.getProcessList();
        List<Integer> result = new ArrayList<>();
        int length = iArr.length;
        int i2 = 0;
        while (i2 < length) {
            int pid = iArr[i2];
            RunningProcess target = null;
            for (RunningProcess proc : procs2) {
                if (pid == proc.pid) {
                    target = proc;
                }
            }
            if (target == null) {
                Slog.w(TAG, "Failed to freeze invalid pid " + pid);
                i = i2;
                procs = procs2;
            } else {
                RunningProcess target2 = target;
                procs = procs2;
                i = i2;
                if (!freezeProcess(target, timeout, fromWho, reason)) {
                    if (GreezeManagerDebugConfig.DEBUG_AIDL) {
                        Slog.d(TAG, "AIDL freezePid(" + pid + ", " + timeout + ", " + fromWho + ", " + reason + ") failed!");
                    }
                } else {
                    result.add(Integer.valueOf(target2.pid));
                }
            }
            i2 = i + 1;
            iArr = pids;
            procs2 = procs;
        }
        if (GreezeManagerDebugConfig.DEBUG_AIDL && GreezeManagerDebugConfig.mCgroupV1Flag) {
            Slog.d(TAG, "AIDL freezePids result: frozen " + FreezeUtils.getFrozenPids());
        }
        return result;
    }

    /* JADX WARN: Code restructure failed: missing block: B:190:0x056d, code lost:
    
        if (r25.mImmobulusMode.isNeedRestictNetworkPolicy(r14) == false) goto L170;
     */
    /* JADX WARN: Code restructure failed: missing block: B:191:0x056f, code lost:
    
        monitorNet(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:192:0x0572, code lost:
    
        checkAndFreezeIsolated(r14, true);
        r11 = r31;
        r7 = r25;
        r4 = true;
        r14 = r20;
        r5 = r21;
        r6 = r22;
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:113:? -> B:110:0x05c6). Please report as a decompilation issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.util.List<java.lang.Integer> freezeUids(int[] r26, long r27, int r29, java.lang.String r30, boolean r31) {
        /*
            Method dump skipped, instructions count: 1582
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.greeze.GreezeManagerService.freezeUids(int[], long, int, java.lang.String, boolean):java.util.List");
    }

    public void dealAdjSet(final int uid, final int cmd) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                switch (cmd) {
                    case 0:
                        GreezeManagerService.this.mUidAdjs.add(Integer.valueOf(uid));
                        return;
                    case 1:
                        GreezeManagerService.this.mUidAdjs.remove(Integer.valueOf(uid));
                        return;
                    case 2:
                        Iterator it = GreezeManagerService.this.mUidAdjs.iterator();
                        while (it.hasNext()) {
                            int uu = ((Integer) it.next()).intValue();
                            GreezeManagerService.this.thawUid(uu, 1000, "adj");
                        }
                        return;
                    default:
                        return;
                }
            }
        });
    }

    private boolean checkStateForScrOff(int uid) {
        return !this.mScreenOnOff && UserHandle.isApp(uid) && uid <= 19999;
    }

    public boolean freezeAction(int uid, int fromWho, String reason, boolean isNeedCompact) {
        Iterator<Integer> it;
        List<Integer> pidList;
        Set<Integer> gameUids;
        if (!this.mImmobulusMode.isModeReason(reason) && isAppShowOnWindows(uid)) {
            return false;
        }
        StringBuilder log = new StringBuilder();
        boolean done = false;
        synchronized (this.mAurogonLock) {
            if (isUidFrozen(uid)) {
                if (GreezeManagerDebugConfig.DEBUG) {
                    Slog.d("Greeze", "uid = " + uid + "has be freeze");
                }
                return false;
            }
            log.append("FZ uid = " + uid);
            List<Integer> pidList2 = readPidFromCgroup(uid);
            if (pidList2.size() == 0) {
                return false;
            }
            Set<Integer> gameUids2 = GreezeServiceUtils.getGameUids();
            if (gameUids2.contains(Integer.valueOf(uid))) {
                Handler handler = this.mHandler;
                handler.sendMessage(handler.obtainMessage(7, 0, uid));
            }
            log.append(" pid = [ ");
            boolean isCgroupPidError = true;
            try {
                synchronized (this.mFrozenPids) {
                    try {
                        Iterator<Integer> it2 = pidList2.iterator();
                        while (it2.hasNext()) {
                            int pid = it2.next().intValue();
                            if (readPidStatus(uid, pid)) {
                                isCgroupPidError = false;
                                done = FreezeUtils.freezePid(pid, uid);
                                if (done) {
                                    FrozenInfo info = this.mFrozenPids.get(pid);
                                    if (info == null) {
                                        try {
                                            info = new FrozenInfo(uid, pid);
                                            this.mFrozenPids.put(pid, info);
                                            it = it2;
                                        } catch (Throwable th) {
                                            th = th;
                                            throw th;
                                        }
                                    } else if (uid == info.uid) {
                                        it = it2;
                                    } else {
                                        it = it2;
                                        Slog.d(TAG, "uid-pid mismatch old uid = " + info.uid + " pid = " + pid + " new uid = " + uid);
                                        info.uid = uid;
                                    }
                                    pidList = pidList2;
                                    gameUids = gameUids2;
                                    info.addFreezeInfo(System.currentTimeMillis(), fromWho, reason);
                                    if (this.mImmobulusMode.mEnterImmobulusMode) {
                                        info.isFrozenByImmobulus = true;
                                    } else if ((fromWho & 16) != 0) {
                                        info.isFrozenByLaunchMode = true;
                                    }
                                    log.append(pid + " ");
                                } else {
                                    it = it2;
                                    pidList = pidList2;
                                    gameUids = gameUids2;
                                    Slog.d(AurogonImmobulusMode.TAG, " Freeze uid = " + uid + " pid = " + pid + " error !");
                                }
                                pidList2 = pidList;
                                gameUids2 = gameUids;
                                it2 = it;
                            }
                        }
                        if (isCgroupPidError) {
                            Slog.d(AurogonImmobulusMode.TAG, " Freeze uid = " + uid + " error due pid-uid mismatch");
                            return true;
                        }
                        log.append("] reason : " + reason + " caller : " + fromWho);
                        if (!this.mImmobulusMode.isModeReason(reason) || GreezeManagerDebugConfig.PID_DEBUG) {
                            this.mHistoryLog.log(log.toString());
                        }
                        if (this.mImmobulusMode.mExtremeMode) {
                            closeSocketForAurogon(uid);
                            updateAurogonUidRule(uid, true);
                        } else if (this.mImmobulusMode.isNeedRestictNetworkPolicy(uid)) {
                            monitorNet(uid);
                        }
                        queryBinderState(uid);
                        checkAndFreezeIsolated(uid, true);
                        return done;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void resetCgroupUidStatus(int uid) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            return;
        }
        List<Integer> pidList = readPidFromCgroup(uid);
        StringBuilder log = new StringBuilder();
        log.append("Reset uid = " + uid + " pid = [ ");
        Iterator<Integer> it = pidList.iterator();
        while (it.hasNext()) {
            int pid = it.next().intValue();
            FreezeUtils.thawPid(pid, uid);
            log.append(pid + " ");
        }
        log.append("]");
        Slog.d(TAG, log.toString());
    }

    public boolean isAppShowOnWindows(int uid) {
        IWindowManager iWindowManager = this.mWindowManager;
        if (iWindowManager != null) {
            try {
                if (iWindowManager.checkAppOnWindowsStatus(uid)) {
                    Slog.d(TAG, "Uid " + uid + " was show on screen, skip it");
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public List<Integer> readPidFromCgroup(int uid) {
        String path = "/sys/fs/cgroup/uid_" + uid;
        List<Integer> pidList = new ArrayList<>();
        File file = new File(path);
        File[] tempList = file.listFiles();
        if (tempList == null) {
            return pidList;
        }
        for (int i = 0; i < tempList.length; i++) {
            String temp = tempList[i].toString();
            if (temp.contains("pid_")) {
                String[] Str = tempList[i].toString().split(d.h);
                if (Str.length > 2 && Str != null && Str[2] != null) {
                    pidList.add(Integer.valueOf(Integer.parseInt(Str[2])));
                }
            }
        }
        return pidList;
    }

    public boolean readPidStatus(int uid, int pid) {
        int tempUid = Process.getUidForPid(pid);
        return tempUid == uid;
    }

    public boolean readPidStatus(int pid) {
        String path = "/proc/" + pid + "/status";
        File file = new File(path);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public void updateFrozenInfoForImmobulus(int uid, int type) {
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                if ((type & 8) != 0) {
                    frozen.isFrozenByImmobulus = true;
                } else if ((type & 16) != 0) {
                    frozen.isFrozenByLaunchMode = true;
                }
            }
        }
    }

    public void resetStatusForImmobulus(int type) {
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                if ((type & 8) != 0) {
                    if (frozen.isFrozenByImmobulus) {
                        frozen.isFrozenByImmobulus = false;
                    }
                } else if ((type & 16) != 0 && frozen.isFrozenByLaunchMode) {
                    frozen.isFrozenByLaunchMode = false;
                }
            }
        }
    }

    public void triggerLaunchMode(String processName, int uid) {
        if (!this.mImmobulusMode.mLaunchModeEnabled || !GreezeManagerDebugConfig.milletEnable) {
            return;
        }
        if ("com.miui.home".equals(processName)) {
            if (!this.mInited) {
                this.mInited = true;
            }
            if (this.mImmobulusMode.isRunningLaunchMode()) {
                return;
            }
        }
        if (!this.ENABLE_LAUNCH_MODE_DEVICE.contains(Build.DEVICE) && !AccessController.PACKAGE_CAMERA.equals(processName) && this.mImmobulusMode.mEnabledLMCamera) {
            return;
        }
        if (AccessController.PACKAGE_CAMERA.equals(processName) && this.mImmobulusMode.mEnabledLMCamera) {
            this.mImmobulusMode.triggerImmobulusMode(uid, "Camera");
        }
        if (!UserHandle.isApp(uid)) {
            return;
        }
        if (isUidFrozen(uid)) {
            Slog.d("Aurogon", "Thaw uid = " + uid + " Activity Start!");
            thawUidAsync(uid, 1000, "Activity Start");
        }
        this.mImmobulusMode.triggerLaunchMode(processName, uid);
    }

    public void finishLaunchMode(String processName, int uid) {
    }

    public void addToDumpHistory(String log) {
        this.mHistoryLog.log(log);
    }

    public void thawThread(int tid) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            FreezeUtils.thawTid(tid);
        }
    }

    private void checkAndFreezeIsolated(final int uid, final boolean freeze) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                List<Integer> pids = GreezeManagerService.this.mActivityManagerService.mInternal.getIsolatedProcesses(uid);
                if (!freeze) {
                    synchronized (GreezeManagerService.this.isoPids) {
                        GreezeManagerService.this.isoPids.remove(Integer.valueOf(uid).intValue());
                    }
                }
                if (pids == null) {
                    return;
                }
                ArrayList arrayList = new ArrayList();
                Iterator<Integer> iterator = pids.iterator();
                while (iterator.hasNext()) {
                    int p = iterator.next().intValue();
                    int isouid = Process.getUidForPid(p);
                    if (isouid != -1 && Process.isIsolated(isouid)) {
                        arrayList.add(Integer.valueOf(p));
                    }
                }
                if (arrayList.size() == 0) {
                    return;
                }
                int[] rst = arrayList.stream().mapToInt(new MnlConfigUtils$$ExternalSyntheticLambda0()).toArray();
                if (freeze) {
                    GreezeManagerService.this.freezePids(rst, 0L, 1000, "iso");
                    synchronized (GreezeManagerService.this.isoPids) {
                        GreezeManagerService.this.isoPids.put(uid, arrayList);
                    }
                } else {
                    GreezeManagerService.this.thawPids(rst, 1000, "iso");
                }
                Slog.d(GreezeManagerService.TAG, "iso uid:" + uid + " p:" + arrayList.toString() + " :" + freeze);
            }
        });
    }

    public void notifyOtherModule(FrozenInfo info, final int fromWho) {
        int i = info.state;
        final int uid = info.uid;
        final int pid = info.pid;
        if (!UserHandle.isApp(uid)) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.6
            @Override // java.lang.Runnable
            public void run() {
                ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).thawedByOther(uid, pid);
                for (int i2 = 1; i2 < 5; i2++) {
                    if (i2 != fromWho && GreezeManagerService.callbacks.get(Integer.valueOf(i2)) != null) {
                        try {
                            GreezeManagerService.callbacks.get(Integer.valueOf(i2)).thawedByOther(uid, pid, fromWho);
                        } catch (Exception e) {
                            if (GreezeManagerDebugConfig.DEBUG) {
                                Slog.e(GreezeManagerService.TAG, "notify other module fail uid : " + uid + " from :" + fromWho);
                            }
                        }
                    }
                }
            }
        });
    }

    public boolean thawProcess(int pid, int fromWho, String reason) {
        boolean done;
        synchronized (this.mFrozenPids) {
            FrozenInfo info = this.mFrozenPids.get(pid);
            if (info == null) {
                if (GreezeManagerDebugConfig.DEBUG) {
                    Slog.w(TAG, "Thawing a non-frozen process (pid=" + pid + "), won't add into history, reason " + reason);
                }
                return false;
            }
            if (GreezeManagerDebugConfig.mCgroupV1Flag) {
                done = FreezeUtils.thawPid(pid);
            } else {
                done = FreezeUtils.thawPid(pid, info.uid);
            }
            Set<Integer> gameUids = GreezeServiceUtils.getGameUids();
            if (gameUids.contains(Integer.valueOf(info.uid))) {
                Handler handler = this.mHandler;
                handler.sendMessage(handler.obtainMessage(8, 0, info.uid));
            }
            ((SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class)).removeFrozenPid(info.uid, pid);
            info.mThawTime = System.currentTimeMillis();
            info.mThawUptime = SystemClock.uptimeMillis();
            info.mThawReason = reason;
            this.mFrozenPids.remove(pid);
            addHistoryInfo(info);
            if (this.mHandler.hasMessages(1, info)) {
                this.mHandler.removeMessages(1, info);
            }
            return done;
        }
    }

    public List<Integer> thawPids(int[] pids, int fromWho, String reason) {
        checkPermission();
        if (GreezeManagerDebugConfig.DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawPids(" + Arrays.toString(pids) + ", " + fromWho + ", " + reason + ")");
        }
        List<Integer> result = new ArrayList<>();
        for (int pid : pids) {
            if (!thawProcess(pid, fromWho, reason)) {
                if (GreezeManagerDebugConfig.DEBUG_AIDL) {
                    Slog.d(TAG, "AIDL thawPid(" + pid + ", " + fromWho + ", " + reason + ") failed");
                }
            } else {
                result.add(Integer.valueOf(pid));
            }
        }
        return result;
    }

    public boolean thawUid(final int uid, int fromWho, String reason) {
        int i;
        boolean z;
        HashSet<Integer> toThawPids;
        if (this.mImmobulusMode.mExtremeMode && "alarm".equals(reason)) {
            return false;
        }
        boolean allDone = true;
        List<FrozenInfo> toThaw = new ArrayList<>();
        synchronized (this.mAurogonLock) {
            synchronized (this.mFrozenPids) {
                HashSet<Integer> toThawPids2 = new HashSet<>();
                for (int i2 = 0; i2 < this.mFrozenPids.size(); i2++) {
                    FrozenInfo frozen = this.mFrozenPids.valueAt(i2);
                    if (frozen.uid == uid) {
                        toThaw.add(frozen);
                        toThawPids2.add(Integer.valueOf(frozen.pid));
                    }
                }
                int i3 = toThaw.size();
                if (i3 == 0) {
                    return true;
                }
                if (GreezeManagerDebugConfig.mCgroupV1Flag) {
                    List<Integer> curPids = getFrozenNewPids();
                    List<Integer> allPids = FreezeUtils.getFrozenPids();
                    allPids.removeAll(curPids);
                    for (Integer item : allPids) {
                        int ppid = Process.getParentPid(item.intValue());
                        if (!toThawPids2.contains(Integer.valueOf(ppid))) {
                            toThawPids = toThawPids2;
                        } else {
                            FreezeUtils.thawPid(item.intValue());
                            toThawPids = toThawPids2;
                            Slog.d(TAG, "isolated pid: " + item + ", ppid: " + ppid);
                        }
                        toThawPids2 = toThawPids;
                    }
                }
                StringBuilder success = new StringBuilder();
                StringBuilder failed = new StringBuilder();
                StringBuilder log = new StringBuilder();
                log.append("THAW uid = " + uid);
                for (FrozenInfo frozen2 : toThaw) {
                    if (!thawProcess(frozen2.pid, fromWho, reason)) {
                        failed.append(frozen2.pid + " ");
                        allDone = false;
                    } else {
                        success.append(frozen2.pid + " ");
                    }
                }
                dealAdjSet(uid, 1);
                log.append(" pid = [ " + success.toString() + "] ");
                if (!"".equals(failed.toString())) {
                    log.append("failed = [ " + failed.toString() + "]");
                }
                log.append(" reason : " + reason + " caller : " + fromWho);
                if (!this.mImmobulusMode.isModeReason(reason) || !allDone) {
                    Slog.d(TAG, log.toString());
                    this.mHistoryLog.log(log.toString());
                }
                if (!this.mImmobulusMode.isModeReason(reason) || this.mImmobulusMode.isNeedNotifyAppStatus(uid)) {
                    i = 1;
                    if (fromWho != 1) {
                        notifyOtherModule(toThaw.get(0), fromWho);
                    }
                } else {
                    i = 1;
                }
                if (fromWho == i || fromWho == 1000) {
                    List<Integer> uids = new ArrayList<>();
                    uids.add(Integer.valueOf(uid));
                    z = false;
                    setWakeLockState(uids, false);
                } else {
                    z = false;
                }
                checkAndFreezeIsolated(uid, z);
                sendPendingAlarmForAurogon(uid);
                sendPendingBroadcastForAurogon(uid);
                this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.7
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.updateAurogonUidRule(uid, false);
                        if (GreezeManagerService.this.mImmobulusMode.mEnterImmobulusMode) {
                            AurogonImmobulusMode aurogonImmobulusMode = GreezeManagerService.this.mImmobulusMode;
                            int i4 = uid;
                            AurogonImmobulusMode aurogonImmobulusMode2 = GreezeManagerService.this.mImmobulusMode;
                            aurogonImmobulusMode.repeatCheckAppForImmobulusMode(i4, 2000);
                            return;
                        }
                        if (GreezeManagerService.this.mImmobulusMode.isRunningLaunchMode()) {
                            AurogonImmobulusMode aurogonImmobulusMode3 = GreezeManagerService.this.mImmobulusMode;
                            int i5 = uid;
                            AurogonImmobulusMode aurogonImmobulusMode4 = GreezeManagerService.this.mImmobulusMode;
                            aurogonImmobulusMode3.repeatCheckAppForImmobulusMode(i5, 500);
                        }
                    }
                });
                return allDone;
            }
        }
    }

    public List<Integer> thawUids(int[] uids, int fromWho, String reason) {
        checkPermission();
        if (GreezeManagerDebugConfig.DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawUids(" + Arrays.toString(uids) + ", " + fromWho + ", " + reason + ")");
        }
        List<Integer> result = new ArrayList<>();
        for (int uid : uids) {
            if (!thawUid(uid, fromWho, reason)) {
                if (GreezeManagerDebugConfig.DEBUG_AIDL) {
                    Slog.d(TAG, "AIDL thawUid(" + uid + ", " + fromWho + ", " + reason + ") failed");
                }
            } else {
                result.add(Integer.valueOf(uid));
            }
        }
        return result;
    }

    private List<Integer> getFrozenNewPids() {
        List<Integer> pids = new ArrayList<>();
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                pids.add(Integer.valueOf(frozen.pid));
            }
        }
        return pids;
    }

    public boolean thawAll(String reason) {
        thawUids(getFrozenUids(9999), 1000, reason + "-thawAll");
        this.mHandler.removeMessages(1);
        if (!GreezeManagerDebugConfig.mCgroupV1Flag) {
            return true;
        }
        List<Integer> tids = FreezeUtils.getFrozonTids();
        Iterator<Integer> it = tids.iterator();
        while (it.hasNext()) {
            int tid = it.next().intValue();
            FreezeUtils.thawTid(tid);
        }
        return FreezeUtils.getFrozenPids().size() == 0;
    }

    public void thawuidsAll(String reason) {
        List<Integer> list = new ArrayList<>();
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                if (!list.contains(Integer.valueOf(frozen.uid))) {
                    list.add(Integer.valueOf(frozen.uid));
                }
            }
        }
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            int uid = it.next().intValue();
            thawUid(uid, 1000, reason);
        }
    }

    public List<Integer> thawAll(int module, int fromWho, String reason) {
        checkPermission();
        if (GreezeManagerDebugConfig.DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawAll(" + module + ", " + fromWho + ", " + reason + ")");
        }
        return thawUids(getFrozenUids(9999), fromWho, reason + "-thawAll from " + module);
    }

    public int[] getFrozenPids(int module) {
        List<Integer> frozens;
        if (GreezeManagerDebugConfig.DEBUG_AIDL) {
            Slog.d(TAG, "AIDL getFrozenPids(" + module + ")");
        }
        switch (module) {
            case 0:
            case 1:
            case 2:
                List<Integer> pids = new ArrayList<>();
                synchronized (this.mFrozenPids) {
                    for (int i = 0; i < this.mFrozenPids.size(); i++) {
                        FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                        if (frozen.mFromWho.size() != 0 && frozen.getOwner() == module) {
                            pids.add(Integer.valueOf(frozen.pid));
                        }
                    }
                }
                return toArray(pids);
            case 9999:
                if (GreezeManagerDebugConfig.mCgroupV1Flag) {
                    frozens = FreezeUtils.getFrozenPids();
                } else {
                    frozens = getFrozenNewPids();
                }
                return toArray(frozens);
            default:
                return new int[0];
        }
    }

    public int[] getFrozenUids(int module) {
        switch (module) {
            case 0:
            case 1:
            case 2:
            case 9999:
                HashSet<Integer> uids = new HashSet<>();
                synchronized (this.mFrozenPids) {
                    for (int i = 0; i < this.mFrozenPids.size(); i++) {
                        FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                        if (module == 9999 || (frozen.mFromWho.size() != 0 && frozen.getOwner() == module)) {
                            uids.add(Integer.valueOf(frozen.uid));
                        }
                    }
                }
                int[] rst = new int[uids.size()];
                int index = 0;
                Iterator<Integer> iterator = uids.iterator();
                while (iterator.hasNext()) {
                    rst[index] = iterator.next().intValue();
                    index++;
                }
                return rst;
            default:
                return new int[0];
        }
    }

    public void updateAurogonUidRule(int uid, boolean allow) {
        if (!GreezeManagerDebugConfig.mPowerMilletEnable || miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        try {
            if (this.cm != null) {
                Class clazz = Class.forName("android.net.ConnectivityManager");
                Method method = clazz.getDeclaredMethod("updateAurogonUidRule", Integer.TYPE, Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(this.cm, Integer.valueOf(uid), Boolean.valueOf(allow));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateAurogonUidRule error ", e);
        }
    }

    public void closeSocketForAurogon(int[] uids) {
        try {
            if (getNmsService() != null) {
                this.mNms.closeSocketForAurogon(uids);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.d(TAG, "failed to close socket for aurogon!");
        }
    }

    public void closeSocketForAurogon(int uid) {
        if (GreezeManagerDebugConfig.DEBUG) {
            Slog.d(AurogonImmobulusMode.TAG, "closeSocketForAurogon uid = " + uid);
        }
        int[] uids = {uid};
        closeSocketForAurogon(uids);
    }

    private INetworkManagementService getNmsService() {
        if (this.mNms == null) {
            this.mNms = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        }
        return this.mNms;
    }

    public boolean isNeedCachedAlarmForAurogon(int uid) {
        return isNeedCachedAlarmForAurogonInner(uid);
    }

    public boolean isAppRunning(int uid) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            return true;
        }
        List<Integer> list = readPidFromCgroup(uid);
        if (list.size() == 0) {
            return false;
        }
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            int pid = it.next().intValue();
            if (readPidStatus(uid, pid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAppRunningInFg(int uid) {
        return uid == this.mTopAppUid;
    }

    public void forceStopPackage(String packageName, int userId, String reason) {
    }

    public void notifyMovetoFront(int uid, boolean inFreeformSmallWinMode) {
        if (GreezeManagerDebugConfig.DEBUG) {
            Slog.d("Aurogon", "notifyMovetoFront uid = " + uid + " inFreeformSmallWinMode = " + inFreeformSmallWinMode);
        }
        if (inFreeformSmallWinMode) {
            this.mInFreeformSmallWinMode = inFreeformSmallWinMode;
            String packageName = getPackageNameFromUid(uid);
            if (packageName != null) {
                if (!this.mImmobulusMode.isRunningLaunchMode() && !checkFreeformSmallWin(packageName)) {
                    triggerLaunchMode(packageName, uid);
                }
                updateFreeformSmallWinList(packageName, true);
            }
            if (this.mImmobulusMode.mEnterImmobulusMode) {
                this.mImmobulusMode.quitImmobulusMode();
            }
        }
        thawUidAsync(uid, 1000, "Activity Front");
        synchronized (this.mRecentLaunchAppList) {
            if (!this.mRecentLaunchAppList.contains(Integer.valueOf(uid))) {
                this.mRecentLaunchAppList.add(Integer.valueOf(uid));
                removetLaunchAppListDelay(uid);
            }
        }
    }

    public void updateFreeformSmallWinList(String packageName, boolean allow) {
        synchronized (this.mFreeformSmallWinList) {
            if (allow) {
                if (!this.mFreeformSmallWinList.contains(packageName)) {
                    this.mFreeformSmallWinList.add(packageName);
                }
            } else {
                this.mFreeformSmallWinList.remove(packageName);
            }
        }
    }

    public void notifyFreeformModeFocus(String string, int mode) {
        boolean z;
        boolean z2;
        String[] str = string.split(":");
        if (str.length == 2) {
            String packageName = str[1];
            int uid = Integer.parseInt(str[0]);
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d("Aurogon", " notifyFreeformModeFocus packageName = " + packageName + " uid = " + uid + " mode = " + mode);
            }
            if (mode == 0) {
                z = true;
            } else {
                z = false;
            }
            if (mode == 1) {
                z2 = true;
            } else {
                z2 = false;
            }
            if (z | z2) {
                thawUidAsync(uid, 1000, "FreeformMode");
                updateFreeformSmallWinList(packageName, true);
            } else {
                updateFreeformSmallWinList(packageName, false);
            }
        }
    }

    public boolean checkFreeformSmallWin(String packageName) {
        boolean contains;
        synchronized (this.mFreeformSmallWinList) {
            contains = this.mFreeformSmallWinList.contains(packageName);
        }
        return contains;
    }

    public boolean checkFreeformSmallWin(int uid) {
        boolean contains;
        String packageName = getPackageNameFromUid(uid);
        if (packageName == null) {
            return false;
        }
        synchronized (this.mFreeformSmallWinList) {
            contains = this.mFreeformSmallWinList.contains(packageName);
        }
        return contains;
    }

    public void notifyMultitaskLaunch(int uid, String packageName) {
        if (GreezeManagerDebugConfig.DEBUG) {
            Slog.d("Aurogon", "notifyMultitaskLaunch uid = " + uid + " packageName = " + packageName);
        }
        thawUidAsync(uid, 1000, "Multitask");
    }

    public boolean checkRecentLuanchedApp(int uid) {
        boolean contains;
        synchronized (this.mRecentLaunchAppList) {
            contains = this.mRecentLaunchAppList.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    public void removetLaunchAppListDelay(final int uid) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.10
            @Override // java.lang.Runnable
            public void run() {
                synchronized (GreezeManagerService.this.mRecentLaunchAppList) {
                    GreezeManagerService.this.mRecentLaunchAppList.remove(Integer.valueOf(uid));
                }
            }
        }, 1000L);
    }

    public void notifyResumeTopActivity(int uid, String packageName, boolean inMultiWindowMode) {
        if (GreezeManagerDebugConfig.DEBUG) {
            Slog.d(TAG, "notifyResumeTopActivity uid = " + uid + " packageName = " + packageName + "  inMultiWindowMode = " + inMultiWindowMode);
        }
        if (uid == this.mTopAppUid || !UserHandle.isApp(uid)) {
            return;
        }
        thawUidAsync(uid, 1000, "Activity Resume");
        synchronized (this.mRecentLaunchAppList) {
            if (!this.mRecentLaunchAppList.contains(Integer.valueOf(uid))) {
                this.mRecentLaunchAppList.add(Integer.valueOf(uid));
                removetLaunchAppListDelay(uid);
            }
        }
        if (!this.mTopAppPackageName.equals(packageName)) {
            if (UserHandle.isApp(this.mTopAppUid)) {
                this.mImmobulusMode.reOrderTargetList(this.mTopAppUid);
            }
            this.mTopAppPackageName = packageName;
            this.mTopAppUid = uid;
            if (inMultiWindowMode) {
                this.mImmobulusMode.addTempMutiWindowsApp(uid);
            } else if (this.mInFreeformSmallWinMode) {
                this.mInFreeformSmallWinMode = false;
            } else {
                triggerLaunchMode(packageName, uid);
            }
        }
    }

    public void thawUidAsync(final int uid, int caller, final String reason) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.11
            @Override // java.lang.Runnable
            public void run() {
                if (GreezeManagerService.this.isUidFrozen(uid)) {
                    GreezeManagerService.this.thawUid(uid, 1000, reason);
                }
            }
        });
    }

    public void updateOrderBCStatus(String intentAction, int uid, boolean isforeground, boolean allow) {
        if (intentAction == null) {
            return;
        }
        if (allow) {
            if (isforeground) {
                this.mFGOrderBroadcastAction = intentAction;
                this.mFGOrderBroadcastAppUid = uid;
                return;
            } else {
                this.mBGOrderBroadcastAction = intentAction;
                this.mBGOrderBroadcastAppUid = uid;
                return;
            }
        }
        if (isforeground) {
            if (this.mFGOrderBroadcastAction.equals(intentAction)) {
                this.mFGOrderBroadcastAction = "";
                this.mFGOrderBroadcastAppUid = -1;
                return;
            }
            return;
        }
        if (this.mBGOrderBroadcastAction.equals(intentAction)) {
            this.mBGOrderBroadcastAction = "";
            this.mBGOrderBroadcastAppUid = -1;
        }
    }

    public boolean checkOrderBCRecivingApp(int uid) {
        if (uid == this.mFGOrderBroadcastAppUid) {
            this.mFGOrderBroadcastAction = "";
            this.mFGOrderBroadcastAppUid = -1;
            return true;
        }
        if (uid == this.mBGOrderBroadcastAppUid) {
            this.mBGOrderBroadcastAction = "";
            this.mBGOrderBroadcastAppUid = -1;
            return true;
        }
        return false;
    }

    private void dumpFreezeAction(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            pw.println("Frozen processes:" + FreezeUtils.getFrozenPids().toString());
        } else {
            pw.println("Frozen processes:" + Arrays.toString(getFrozenPids(9999)));
        }
        pw.println("Greezer History : ");
        this.mHistoryLog.dump(fd, pw, args);
        this.mImmobulusMode.dump(fd, pw, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getAlarmManagerService() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        }
    }

    private void getWindowManagerService() {
        if (this.mWindowManager == null) {
            this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        }
    }

    public boolean isNeedCachedAlarmForAurogonInner(int uid) {
        String packageName;
        if (!isUidFrozen(uid) || (packageName = getPackageNameFromUid(uid)) == null || !UserHandle.isApp(uid) || packageName.contains("xiaomi") || ((packageName.contains("miui") && !this.mAurogonAlarmForbidList.contains(packageName)) || this.mAurogonAlarmAllowList.contains(packageName))) {
            return false;
        }
        Slog.d(TAG, "cached alarm!");
        return true;
    }

    public void sendPendingAlarmForAurogon(final int uid) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.12
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (GreezeManagerService.this.mAlarmManager == null) {
                        GreezeManagerService.this.getAlarmManagerService();
                    }
                    if (GreezeManagerService.this.mAlarmManager != null) {
                        GreezeManagerService.this.mAlarmManager.sendPendingAlarmByAurogon(uid);
                    }
                } catch (RemoteException e) {
                }
            }
        }, 500L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageNameFromUid(int uid) {
        String packName = null;
        if (this.mPm == null) {
            this.mPm = this.mContext.getPackageManager();
        }
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packName = packageManager.getNameForUid(uid);
        }
        if (packName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packName;
    }

    private void registerObserverForAurogon() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_AUROGON_ALARM_ALLOW_LIST), false, this.mCloudAurogonAlarmListObserver, -2);
    }

    public void notifyExcuteServices(final int uid) {
        if (!UserHandle.isApp(uid) || uid > 19999) {
            return;
        }
        synchronized (this.mExcuteServiceList) {
            if (!this.mExcuteServiceList.contains(Integer.valueOf(uid)) && !isAppRunningInFg(uid)) {
                this.mExcuteServiceList.add(Integer.valueOf(uid));
            }
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.14
            @Override // java.lang.Runnable
            public void run() {
                if (!GreezeManagerService.this.mImmobulusMode.isRunningLaunchMode()) {
                    GreezeManagerService.this.thawUid(uid, 1000, "Excute Service");
                }
            }
        });
    }

    public void notifyDumpAppInfo(final int uid, int pid) {
        Slog.d(TAG, "notify dump uid " + uid + " pid " + pid);
        if (isUidFrozen(uid)) {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.15
                @Override // java.lang.Runnable
                public void run() {
                    GreezeManagerService.this.thawUid(uid, 1000, "Dumping");
                }
            });
        }
    }

    public void notifyDumpAllInfo() {
        Slog.d(TAG, "notify dump all info");
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.16
            @Override // java.lang.Runnable
            public void run() {
                GreezeManagerService.this.thawAll(1000, 1000, "Dumping");
            }
        });
    }

    /* loaded from: classes.dex */
    public class AurogonBroadcastReceiver extends BroadcastReceiver {
        public String actionUI = "com.android.systemui.fsgesture";

        public AurogonBroadcastReceiver() {
            IntentFilter intent = new IntentFilter();
            intent.addAction("android.intent.action.SCREEN_ON");
            intent.addAction("android.intent.action.SCREEN_OFF");
            intent.addAction(this.actionUI);
            GreezeManagerService.this.mContext.registerReceiver(this, intent);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                GreezeManagerService.this.mScreenOnOff = true;
                int[] uids = new int[GreezeManagerService.this.mFreeformSmallWinList.size()];
                synchronized (GreezeManagerService.this.mFreeformSmallWinList) {
                    if (GreezeManagerService.this.mFreeformSmallWinList.size() > 0) {
                        int i = 0;
                        for (String name : GreezeManagerService.this.mFreeformSmallWinList) {
                            int uid = GreezeManagerService.this.getUidByPackageName(name);
                            uids[i] = uid;
                            i++;
                        }
                    }
                }
                if (uids.length > 0) {
                    GreezeManagerService.this.thawUids(uids, 1000, "smallw");
                }
                GreezeManagerService.this.dealAdjSet(0, 2);
                GreezeManagerService.this.mHistoryLog.log("SCREEN ON!");
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                GreezeManagerService.this.mScreenOnOff = false;
                GreezeManagerService.this.mHistoryLog.log("SCREEN OFF!");
                GreezeManagerService.this.mImmobulusMode.finishLaunchMode();
                if (GreezeManagerService.this.mImmobulusMode.mEnterImmobulusMode && !GreezeManagerService.this.mImmobulusMode.mExtremeMode) {
                    GreezeManagerService.this.mImmobulusMode.quitImmobulusMode();
                    return;
                }
                return;
            }
            if (this.actionUI.equals(action)) {
                String type = intent.getStringExtra("typeFrom");
                boolean isEnter = intent.getBooleanExtra("isEnter", false);
                if (type != null && "typefrom_status_bar_expansion".equals(type)) {
                    GreezeManagerService.this.isBarExpand = isEnter;
                    if (isEnter) {
                        GreezeManagerService.this.mHandler.sendEmptyMessage(5);
                        if (GreezeManagerService.this.mImmobulusMode.mEnterIMCamera || GreezeManagerService.this.mImmobulusMode.mEnterImmobulusMode) {
                            GreezeManagerService.this.mImmobulusMode.quitImmobulusMode();
                            GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = false;
                            GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus = true;
                            return;
                        }
                        return;
                    }
                    String ret = "";
                    if (GreezeManagerService.this.mTopAppUid == GreezeManagerService.this.mImmobulusMode.mCameraUid) {
                        GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = true;
                        ret = "Camera";
                    }
                    if (GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus) {
                        GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus = false;
                        ret = ret + "-BarExpand";
                    }
                    if (ret.contains("Camera") || ret.contains("BarExpand")) {
                        GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(GreezeManagerService.this.mTopAppUid, ret);
                    }
                }
            }
        }
    }

    public boolean getBroadcastCtrl() {
        return this.mBroadcastCtrlCloud;
    }

    public void setBroadcastCtrl(boolean val) {
        this.mBroadcastCtrlCloud = val;
        Slog.d(TAG, "setBroadcastCtrl " + val);
    }

    public Map<String, String> getBroadcastConfig() {
        return this.mBroadcastCtrlMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getSystemUiPid() {
        List<RunningProcess> procList = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procList) {
            if (proc != null && AccessController.PACKAGE_SYSTEMUI.equals(proc.processName)) {
                return proc.pid;
            }
        }
        return -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0070, code lost:
    
        if (r5.equals("provider") != false) goto L24;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean isRestrictBackgroundAction(java.lang.String r5, int r6, java.lang.String r7, int r8, java.lang.String r9) {
        /*
            r4 = this;
            r0 = 0
            boolean r1 = r4.isUidFrozen(r8)
            r2 = 1
            if (r1 != 0) goto L9
            return r2
        L9:
            boolean r1 = com.miui.server.greeze.GreezeManagerDebugConfig.DEBUG
            if (r1 == 0) goto L4d
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "localhost = "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r5)
            java.lang.String r3 = " callerUid = "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r6)
            java.lang.String r3 = " callerPkgName = "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r7)
            java.lang.String r3 = " calleeUid = "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r8)
            java.lang.String r3 = " calleePkgName = "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r9)
            java.lang.String r1 = r1.toString()
            java.lang.String r3 = "GreezeManager"
            android.util.Slog.d(r3, r1)
        L4d:
            int r1 = r5.hashCode()
            java.lang.String r3 = "provider"
            switch(r1) {
                case -1618876223: goto L73;
                case -987494927: goto L6c;
                case -246623272: goto L62;
                case 185053203: goto L57;
                default: goto L56;
            }
        L56:
            goto L7d
        L57:
            java.lang.String r1 = "startservice"
            boolean r1 = r5.equals(r1)
            if (r1 == 0) goto L56
            r2 = 3
            goto L7e
        L62:
            java.lang.String r1 = "bindservice"
            boolean r1 = r5.equals(r1)
            if (r1 == 0) goto L56
            r2 = 2
            goto L7e
        L6c:
            boolean r1 = r5.equals(r3)
            if (r1 == 0) goto L56
            goto L7e
        L73:
            java.lang.String r1 = "broadcast"
            boolean r1 = r5.equals(r1)
            if (r1 == 0) goto L56
            r2 = 0
            goto L7e
        L7d:
            r2 = -1
        L7e:
            switch(r2) {
                case 0: goto L89;
                case 1: goto L87;
                case 2: goto L82;
                case 3: goto L82;
                default: goto L81;
            }
        L81:
            goto L8e
        L82:
            boolean r0 = r4.isNeedAllowRequest(r6, r7, r8)
            goto L8e
        L87:
            r0 = 1
            goto L8e
        L89:
            boolean r0 = r4.isNeedAllowRequest(r6, r7, r8)
        L8e:
            if (r0 == 0) goto La9
            com.miui.server.greeze.AurogonImmobulusMode r1 = r4.mImmobulusMode
            boolean r1 = r1.isRunningLaunchMode()
            if (r1 == 0) goto La4
            boolean r1 = r3.equals(r5)
            if (r1 != 0) goto La4
            com.miui.server.greeze.AurogonImmobulusMode r1 = r4.mImmobulusMode
            r1.addLaunchModeQiutList(r8)
            return r0
        La4:
            r1 = 1000(0x3e8, float:1.401E-42)
            r4.thawUidAsync(r8, r1, r5)
        La9:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.greeze.GreezeManagerService.isRestrictBackgroundAction(java.lang.String, int, java.lang.String, int, java.lang.String):boolean");
    }

    boolean isNeedAllowRequest(int callerUid, String callerPkgName, int calleeUid) {
        return !this.mScreenOnOff ? callerUid == 1027 || callerUid == 1002 : isAppRunningInFg(callerUid) || callerUid == calleeUid || "com.xiaomi.xmsf".equals(callerPkgName);
    }

    public boolean isNeedCachedBroadcast(Intent intent, int uid, String packageName) {
        if (!isUidFrozen(uid) || checkAurogonIntentDenyList(intent.getAction())) {
            return false;
        }
        if (intent.getSelector() != null) {
            Slog.w(TAG, "drop bc, selector:" + intent.getSelector().toString());
            return false;
        }
        if (mNeedCachedBroadcast.contains(intent.getAction())) {
            intent.setPackage(packageName);
            CachedBroadcasForAurogon(intent, uid);
            return true;
        }
        return false;
    }

    public boolean isRestrictReceiver(Intent intent, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
        if (!this.mBroadcastCtrlCloud) {
            thawUidAsync(calleeUid, 1000, "broadcast_cl");
            return false;
        }
        if (!isUidFrozen(calleeUid)) {
            return false;
        }
        String action = intent.getAction();
        if (action == null) {
            thawUidAsync(calleeUid, 1000, "bc_action");
            Slog.d(TAG, "isNeedCachedBroadcast white action =" + action);
            return false;
        }
        if (GreezeManagerDebugConfig.DEBUG) {
            Slog.d(TAG, "Broadcast callerUid = " + callerUid + " callerPkgName = " + callerPkgName + " calleeUid = " + calleeUid + " calleePkgName = " + calleePkgName + " action = " + action);
        }
        boolean ret = !isAllowBroadcast(callerUid, callerPkgName, calleeUid, calleePkgName, action);
        if (!ret) {
            if (this.mImmobulusMode.isRunningLaunchMode() && checkImmobulusModeRestrict(calleePkgName, action)) {
                this.mImmobulusMode.addLaunchModeQiutList(calleeUid);
            } else {
                thawUidAsync(calleeUid, 1000, "broadcast");
            }
        } else {
            isNeedCachedBroadcast(intent, calleeUid, calleePkgName);
        }
        return ret;
    }

    private boolean deferBroadcastForMiui(String action) {
        if (!this.mScreenOnOff && mMiuiDeferBroadcast.contains(action)) {
            return true;
        }
        if (!this.mScreenOnOff && AurogonImmobulusMode.CN_MODEL && mCNDeferBroadcast.contains(action)) {
            return true;
        }
        return this.mImmobulusMode.isRunningImmobulusMode() && mMiuiDeferBroadcast.contains(action);
    }

    boolean isAllowBroadcast(int callerUid, String callerPkgName, int calleeUid, String calleePkgName, String action) {
        synchronized (this.mBroadcastTargetWhiteList) {
            if (this.mBroadcastTargetWhiteList.containsKey(calleePkgName)) {
                List<String> actions = this.mBroadcastTargetWhiteList.get(calleePkgName);
                if (actions.contains(action) || actions.contains("*")) {
                    return true;
                }
            }
            synchronized (this.mBroadcastCallerWhiteList) {
                if (this.mBroadcastCallerWhiteList.containsKey(callerPkgName)) {
                    List<String> actions2 = this.mBroadcastCallerWhiteList.get(callerPkgName);
                    if (actions2.contains(action) || actions2.contains("*")) {
                        return true;
                    }
                }
                synchronized (this.mBroadcastCtrlMap) {
                    if (this.mBroadcastCtrlMap.containsKey(calleePkgName) && (this.mBroadcastCtrlMap.get(calleePkgName).equals("ALL") || this.mBroadcastCtrlMap.get(calleePkgName).contains(action))) {
                        return true;
                    }
                    if (this.mImmobulusMode.isSystemOrMiuiImportantApp(calleePkgName) || mAllowBroadcast.contains(action)) {
                        return (!calleePkgName.contains("com.google.android.gms") || this.mScreenOnOff) && !deferBroadcastForMiui(action);
                    }
                    if (checkAurogonIntentDenyList(action)) {
                        return false;
                    }
                    if (callerUid == 1027 || callerUid == 1002) {
                        return true;
                    }
                    return this.mScreenOnOff && (isAppRunningInFg(callerUid) || callerUid == calleeUid || "com.xiaomi.xmsf".equals(callerPkgName));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppZygoteStart(ApplicationInfo info, boolean start) {
        if (info != null && start) {
            thawUidAsync(info.uid, 1, "AppZygote");
        }
    }

    boolean checkImmobulusModeRestrict(String targetPkgName, String action) {
        synchronized (this.mImmobulusModeWhiteList) {
            if (this.mImmobulusModeWhiteList.containsKey(targetPkgName)) {
                List<String> list = this.mImmobulusModeWhiteList.get(targetPkgName);
                if (list.contains(action) || list.contains("*")) {
                    return false;
                }
            }
            return true;
        }
    }

    public void CachedBroadcasForAurogon(Intent intent, int uid) {
        synchronized (this.mCachedBCList) {
            List<Intent> intentList = this.mCachedBCList.get(Integer.valueOf(uid));
            if (intentList == null) {
                intentList = new ArrayList();
                this.mCachedBCList.put(Integer.valueOf(uid), intentList);
            }
            for (Intent old : intentList) {
                if (old.getAction().equals(intent.getAction())) {
                    return;
                }
            }
            intentList.add(intent);
        }
    }

    public void sendPendingBroadcastForAurogon(int uid) {
        synchronized (this.mCachedBCList) {
            List<Intent> intentList = this.mCachedBCList.get(Integer.valueOf(uid));
            if (intentList != null && intentList.size() != 0) {
                for (final Intent intent : intentList) {
                    if (this.mScreenOnOff) {
                        if (!"android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                            if (!"android.intent.action.PACKAGE_REPLACED".equals(intent.getAction()) && !"android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                                this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.17
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        GreezeManagerService.this.mContext.sendOrderedBroadcast(intent, null, null, null, 0, null, null);
                                    }
                                });
                            }
                        }
                    } else if (!"android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                        if (!"android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
                            this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.17
                                @Override // java.lang.Runnable
                                public void run() {
                                    GreezeManagerService.this.mContext.sendOrderedBroadcast(intent, null, null, null, 0, null, null);
                                }
                            });
                        }
                    }
                }
                this.mCachedBCList.remove(Integer.valueOf(uid));
            }
        }
    }

    public void removePendingBroadcast(int uid) {
        synchronized (this.mCachedBCList) {
            this.mCachedBCList.remove(Integer.valueOf(uid));
        }
    }

    public boolean checkAurogonIntentDenyList(String action) {
        if (this.mBroadcastIntentDenyList.contains(action)) {
            return true;
        }
        return false;
    }

    static int[] toArray(List<Integer> lst) {
        if (lst == null) {
            return new int[0];
        }
        int[] arr = new int[lst.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = lst.get(i).intValue();
        }
        return arr;
    }

    public void notifyBackup(int i, boolean z) {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(6, i, z ? 1 : 0));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GreezeThread extends ServiceThread {
        private static GreezeThread sInstance;

        private GreezeThread() {
            super("Greezer", -2, true);
        }

        private static void ensureThreadLocked() {
            if (sInstance == null) {
                GreezeThread greezeThread = new GreezeThread();
                sInstance = greezeThread;
                greezeThread.start();
            }
        }

        public static GreezeThread getInstance() {
            GreezeThread greezeThread;
            synchronized (GreezeThread.class) {
                ensureThreadLocked();
                greezeThread = sInstance;
            }
            return greezeThread;
        }
    }

    /* loaded from: classes.dex */
    class H extends Handler {
        static final int MSG_BACKUP_OP = 6;
        static final int MSG_GET_SYSTEM_PID = 5;
        static final int MSG_LAUNCH_BOOST = 2;
        static final int MSG_MILLET_LOOPONCE = 4;
        static final int MSG_RECEIVE_FZ = 7;
        static final int MSG_RECEIVE_THAW = 8;
        static final int MSG_REPORT_FZ = 9;
        static final int MSG_THAW_ALL = 3;
        static final int MSG_THAW_PID = 1;
        Set<Integer> delayUids;
        Set<Integer> freezeingUids;
        Set<Integer> thawingUids;

        public H(Looper looper) {
            super(looper);
            this.freezeingUids = new HashSet();
            this.thawingUids = new HashSet();
            this.delayUids = new HashSet();
        }

        private void reportFz() {
            if (this.delayUids.size() > 0 && this.freezeingUids.size() > 0) {
                this.delayUids.removeAll(this.freezeingUids);
            }
            if (this.delayUids.size() > 0 && this.thawingUids.size() > 0) {
                this.delayUids.removeAll(this.thawingUids);
            }
            HashSet<Integer> tmp = new HashSet<>();
            tmp.addAll(this.freezeingUids);
            tmp.retainAll(this.thawingUids);
            String result = "";
            this.thawingUids.removeAll(tmp);
            this.freezeingUids.removeAll(tmp);
            Iterator<Integer> it = this.delayUids.iterator();
            while (it.hasNext()) {
                int u = it.next().intValue();
                if (!GreezeManagerService.this.isUidFrozen(u)) {
                    this.thawingUids.add(Integer.valueOf(u));
                    it.remove();
                }
            }
            if (this.thawingUids.size() > 0) {
                result = "thaw:" + this.thawingUids.toString().substring(1, this.thawingUids.toString().length() - 1);
            }
            this.delayUids.addAll(this.freezeingUids);
            if (this.delayUids.size() > 0) {
                result = result + ";freeze:" + this.delayUids.toString().substring(1, this.delayUids.toString().length() - 1);
            }
            this.delayUids.clear();
            this.delayUids.addAll(tmp);
            if (this.delayUids.size() > 0 && !hasMessages(9)) {
                sendEmptyMessageDelayed(9, 1000L);
            }
            if (GreezeManagerDebugConfig.DEBUG) {
                Slog.d(GreezeManagerService.TAG, " settings result:" + result);
            }
            if (result.length() > 2) {
                Settings.Secure.putString(GreezeManagerService.this.mContext.getContentResolver(), "miui_freeze", result);
            }
            this.freezeingUids.clear();
            this.thawingUids.clear();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.arg1 != 0) {
                        int pid = msg.arg1;
                        GreezeManagerService.this.thawProcess(pid, msg.arg2, "Timeout pid " + pid);
                        return;
                    }
                    return;
                case 2:
                default:
                    return;
                case 3:
                    GreezeManagerService.this.thawAll("from msg");
                    return;
                case 4:
                    GreezeManagerService.nLoopOnce();
                    return;
                case 5:
                    GreezeManagerService greezeManagerService = GreezeManagerService.this;
                    greezeManagerService.mSystemUiPid = greezeManagerService.getSystemUiPid();
                    return;
                case 6:
                    if (msg.arg2 == 1 && GreezeManagerService.this.isUidFrozen(msg.arg1)) {
                        GreezeManagerService.this.thawUid(msg.arg1, 1000, "backing");
                        return;
                    } else {
                        if (msg.arg2 == 0 && !GreezeManagerService.this.isUidFrozen(msg.arg1)) {
                            int[] uids = {msg.arg1};
                            GreezeManagerService.this.freezeUids(uids, 0L, 1000, "backingE", true);
                            return;
                        }
                        return;
                    }
                case 7:
                    if (msg.arg1 == 0) {
                        this.freezeingUids.add(Integer.valueOf(msg.arg2));
                    }
                    if (!hasMessages(9)) {
                        sendEmptyMessageDelayed(9, 1000L);
                        return;
                    }
                    return;
                case 8:
                    if (msg.arg1 == 0) {
                        this.thawingUids.add(Integer.valueOf(msg.arg2));
                    }
                    if (!hasMessages(9)) {
                        sendEmptyMessageDelayed(9, 1000L);
                        return;
                    }
                    return;
                case 9:
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d(GreezeManagerService.TAG, "delayUids:" + this.delayUids.toString() + " freezeingUids:" + this.freezeingUids.toString() + " thawingUids:" + this.thawingUids.toString());
                    }
                    reportFz();
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class FrozenInfo {
        String mThawReason;
        long mThawTime;
        long mThawUptime;
        int pid;
        String processName;
        int state;
        int uid;
        List<Integer> mFromWho = new ArrayList(16);
        List<Long> mFreezeTimes = new ArrayList(16);
        List<String> mFreezeReasons = new ArrayList(16);
        boolean isFrozenByImmobulus = false;
        boolean isFrozenByLaunchMode = false;
        boolean isAudioZero = false;

        FrozenInfo(RunningProcess processRecord) {
            this.uid = processRecord.uid;
            this.pid = processRecord.pid;
            this.processName = processRecord.processName;
        }

        FrozenInfo(int uid, int pid) {
            this.uid = uid;
            this.pid = pid;
        }

        void addFreezeInfo(long curTime, int fromWho, String reason) {
            this.mFreezeTimes.add(Long.valueOf(curTime));
            this.mFromWho.add(Integer.valueOf(fromWho));
            this.mFreezeReasons.add(reason);
            this.state |= 1 << fromWho;
        }

        long getStartTime() {
            if (this.mFreezeTimes.size() == 0) {
                return 0L;
            }
            return this.mFreezeTimes.get(0).longValue();
        }

        long getEndTime() {
            return this.mThawTime;
        }

        long getFrozenDuration() {
            if (getStartTime() < getEndTime()) {
                return getEndTime() - getStartTime();
            }
            return 0L;
        }

        int getOwner() {
            if (this.mFromWho.size() == 0) {
                return 0;
            }
            return this.mFromWho.get(0).intValue();
        }

        public String toString() {
            return this.uid + " " + this.pid + " " + this.processName;
        }
    }

    private static int ringAdvance(int origin, int increment, int size) {
        int index = (origin + increment) % size;
        return index < 0 ? index + size : index;
    }

    private void addHistoryInfo(FrozenInfo info) {
        FrozenInfo[] frozenInfoArr = this.mFrozenHistory;
        int i = this.mHistoryIndexNext;
        frozenInfoArr[i] = info;
        this.mHistoryIndexNext = ringAdvance(i, 1, HISTORY_SIZE);
    }

    private List<FrozenInfo> getHistoryInfos(long sinceUptime) {
        FrozenInfo frozenInfo;
        List<FrozenInfo> ret = new ArrayList<>();
        int index = ringAdvance(this.mHistoryIndexNext, -1, HISTORY_SIZE);
        int i = 0;
        while (true) {
            int i2 = HISTORY_SIZE;
            if (i >= i2 || (frozenInfo = this.mFrozenHistory[index]) == null || frozenInfo.mThawTime < sinceUptime) {
                break;
            }
            ret.add(this.mFrozenHistory[index]);
            index = ringAdvance(index, -1, i2);
            i++;
        }
        return ret;
    }

    void dumpHistory(String prefix, FileDescriptor fd, PrintWriter pw) {
        pw.println("Frozen processes in history:");
        List<FrozenInfo> infos = getHistoryInfos(SystemClock.uptimeMillis() - 14400000);
        int index = 1;
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
        for (FrozenInfo info : infos) {
            pw.print(prefix + "  ");
            int index2 = index + 1;
            pw.print("#" + index);
            pw.print(" " + formater.format(new Date(info.mThawTime)));
            if (info.uid != 0) {
                pw.print(" " + info.uid);
            }
            pw.print(" " + info.pid);
            if (!TextUtils.isEmpty(info.processName)) {
                pw.print(" " + info.processName);
            }
            pw.println(" " + info.getFrozenDuration() + "ms");
            for (int i = 0; i < info.mFreezeTimes.size(); i++) {
                pw.print(prefix + "    ");
                pw.print("fz: ");
                pw.print(formater.format(new Date(info.mFreezeTimes.get(i).longValue())));
                pw.print(" " + info.mFreezeReasons.get(i));
                pw.println(" from " + info.mFromWho.get(i));
            }
            pw.print(prefix + "    ");
            pw.print("th: ");
            pw.print(formater.format(new Date(info.mThawTime)));
            pw.println(" " + info.mThawReason);
            index = index2;
        }
    }

    void dumpSettings(String prefix, FileDescriptor fd, PrintWriter pw) {
        pw.println(prefix + "Settings:");
        pw.println(prefix + "  enable=" + (GreezeManagerDebugConfig.milletEnable && GreezeManagerDebugConfig.mPowerMilletEnable) + " (persist.sys.powmillet.enable)");
        pw.println(prefix + "  debug=" + GreezeManagerDebugConfig.DEBUG + " (persist.sys.gz.debug)");
        pw.println(prefix + "  monkey=" + GreezeManagerDebugConfig.DEBUG_MONKEY + " (persist.sys.gz.monkey)");
        pw.println(prefix + "  fz_timeout=" + GreezeManagerDebugConfig.LAUNCH_FZ_TIMEOUT + " (persist.sys.gz.fztimeout)");
        pw.println(prefix + "  monitor=" + GreezeManagerDebugConfig.milletEnable + " (" + this.mRegisteredMonitor + ")");
        synchronized (this.mBroadcastTargetWhiteList) {
            pw.println(prefix + "  mBroadcastTargetWhiteList=" + this.mBroadcastTargetWhiteList.toString());
        }
    }

    void dumpFrozen(String prefix, FileDescriptor fd, PrintWriter pw) {
        List<Integer> tids;
        if (GreezeManagerDebugConfig.mCgroupV1Flag) {
            List<Integer> tids2 = FreezeUtils.getFrozonTids();
            pw.println(prefix + "Frozen tids: " + tids2);
            tids = FreezeUtils.getFrozenPids();
        } else {
            tids = getFrozenNewPids();
        }
        pw.println(prefix + "Frozen pids: " + tids);
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
        pw.println(prefix + "Frozen processes:");
        synchronized (this.mFrozenPids) {
            int n = this.mFrozenPids.size();
            for (int i = 0; i < n; i++) {
                FrozenInfo info = this.mFrozenPids.valueAt(i);
                pw.print(prefix + "  ");
                pw.print("#" + (i + 1));
                pw.println(" pid=" + info.pid);
                for (int index = 0; index < info.mFreezeTimes.size(); index++) {
                    pw.print(prefix + "    ");
                    pw.print("fz: ");
                    pw.print(formater.format(new Date(info.mFreezeTimes.get(index).longValue())));
                    pw.print(" " + info.mFreezeReasons.get(index));
                    pw.println(" from " + info.mFromWho.get(index));
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            dumpSettings("", fd, pw);
            dumpFreezeAction(fd, pw, args);
            if (args.length != 0 && "old".equals(args[0])) {
                dumpHistory("", fd, pw);
            }
            for (String temp : this.mFreeformSmallWinList) {
                pw.println(" FreeformSmallWin uid = " + temp);
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new GreezeMangaerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class GreezeMangaerShellCommand extends ShellCommand {
        GreezeManagerService mService;

        GreezeMangaerShellCommand(GreezeManagerService service) {
            this.mService = service;
        }

        private void runDumpHistory() {
            this.mService.dumpHistory("", getOutFileDescriptor(), getOutPrintWriter());
        }

        private void runListProcesses() {
            PrintWriter pw = getOutPrintWriter();
            List<RunningProcess> list = GreezeServiceUtils.getProcessList();
            pw.println("process total " + list.size());
            for (int i = 0; i < list.size(); i++) {
                RunningProcess proc = list.get(i);
                pw.printf("  #%d %s", Integer.valueOf(i + 1), proc.toString());
                pw.println();
            }
        }

        private void runDumpPackages() {
            PrintWriter pw = getOutPrintWriter();
            ProcessMap<List<RunningProcess>> procMap = this.mService.getPkgMap();
            for (String pkgName : procMap.getMap().keySet()) {
                pw.println("pkg " + pkgName);
                SparseArray<List<RunningProcess>> uids = (SparseArray) procMap.getMap().get(pkgName);
                for (int i = 0; i < uids.size(); i++) {
                    uids.keyAt(i);
                    List<RunningProcess> procs = uids.valueAt(i);
                    if (procs != null) {
                        for (RunningProcess proc : procs) {
                            pw.println("  " + proc.toString());
                        }
                    }
                }
            }
        }

        private void runDumpUids() {
            PrintWriter pw = getOutPrintWriter();
            SparseArray<List<RunningProcess>> uidMap = GreezeServiceUtils.getUidMap();
            int N = uidMap.size();
            pw.println("uid total " + N);
            for (int i = 0; i < N; i++) {
                int uid = uidMap.keyAt(i);
                pw.printf("#%d uid %d", Integer.valueOf(i + 1), Integer.valueOf(uid));
                pw.println();
                List<RunningProcess> procs = uidMap.valueAt(i);
                for (RunningProcess proc : procs) {
                    pw.println("  " + proc.toString());
                }
            }
        }

        private void dumpSkipUid() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("audio uid: " + GreezeServiceUtils.getAudioUid());
            pw.println("ime uid: " + GreezeServiceUtils.getIMEUid());
            try {
                ForegroundInfo foregroundInfo = ProcessManager.getForegroundInfo();
                pw.println("foreground uid: " + foregroundInfo.mForegroundUid);
                pw.println("multi window uid: " + foregroundInfo.mMultiWindowForegroundUid);
            } catch (Exception e) {
                Slog.w("ShellCommand", "Failed to get foreground info from ProcessManager", e);
            }
            List<RunningProcess> procs = GreezeServiceUtils.getProcessList();
            Set<Integer> foreActs = new ArraySet<>();
            Set<Integer> foreSvcs = new ArraySet<>();
            for (RunningProcess proc : procs) {
                if (proc.hasForegroundActivities) {
                    foreActs.add(Integer.valueOf(proc.uid));
                }
                if (proc.hasForegroundServices) {
                    foreSvcs.add(Integer.valueOf(proc.uid));
                }
            }
            pw.println("fore act uid: " + foreActs);
            pw.println("fore svc uid: " + foreSvcs);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Failed to find 'out' block for switch in B:12:0x011a. Please report as an issue. */
        public int onCommand(String cmd) {
            char c;
            this.mService.checkPermission();
            PrintWriter pw = getOutPrintWriter();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                switch (cmd.hashCode()) {
                    case -1346846559:
                        if (cmd.equals("unmonitor")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1298848381:
                        if (cmd.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case -452147603:
                        if (cmd.equals("clearmonitor")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case -172220347:
                        if (cmd.equals("callback")) {
                            c = 20;
                            break;
                        }
                        c = 65535;
                        break;
                    case -75092327:
                        if (cmd.equals("getUids")) {
                            c = 19;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3463:
                        if (cmd.equals("ls")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case 3587:
                        if (cmd.equals(ScoutHelper.ACTION_PS)) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case 104581:
                        if (cmd.equals("iso")) {
                            c = 21;
                            break;
                        }
                        c = 65535;
                        break;
                    case 111052:
                        if (cmd.equals("pkg")) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 115792:
                        if (cmd.equals(SlaDbSchema.SlaTable.Uidlist.UID)) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3327652:
                        if (cmd.equals("loop")) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3331227:
                        if (cmd.equals("lsfz")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case 3532159:
                        if (cmd.equals("skip")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3558826:
                        if (cmd.equals("thaw")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 95458899:
                        if (cmd.equals("debug")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 97944631:
                        if (cmd.equals("fzpid")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 97949436:
                        if (cmd.equals("fzuid")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 107944136:
                        if (cmd.equals("query")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case 110337687:
                        if (cmd.equals("thpid")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 110342492:
                        if (cmd.equals("thuid")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 926934164:
                        if (cmd.equals("history")) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1236319578:
                        if (cmd.equals("monitor")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
            } catch (Exception e) {
                pw.println(e);
            }
            switch (c) {
                case 0:
                    this.mService.freezeUids(new int[]{Integer.parseInt(getNextArgRequired())}, 0L, Integer.parseInt(getNextArgRequired()), "cmd: fzuid", false);
                    return 0;
                case 1:
                    this.mService.freezePids(new int[]{Integer.parseInt(getNextArgRequired())}, 0L, Integer.parseInt(getNextArgRequired()), "cmd: fzpid");
                    return 0;
                case 2:
                    this.mService.thawUids(new int[]{Integer.parseInt(getNextArgRequired())}, Integer.parseInt(getNextArgRequired()), "cmd: thuid");
                    return 0;
                case 3:
                    this.mService.thawPids(new int[]{Integer.parseInt(getNextArgRequired())}, Integer.parseInt(getNextArgRequired()), "cmd: thpid");
                    return 0;
                case 4:
                    this.mService.thawAll(9999, 9999, "ShellCommand: thaw all");
                    return 0;
                case 5:
                    this.mService.monitorNet(Integer.parseInt(getNextArgRequired()));
                    return 0;
                case 6:
                    this.mService.clearMonitorNet(Integer.parseInt(getNextArgRequired()));
                    return 0;
                case 7:
                    this.mService.clearMonitorNet();
                    return 0;
                case '\b':
                    this.mService.queryBinderState(Integer.parseInt(getNextArgRequired()));
                    return 0;
                case '\t':
                    this.mService.dumpSettings("", getOutFileDescriptor(), getOutPrintWriter());
                    this.mService.dumpFrozen("", getOutFileDescriptor(), getOutPrintWriter());
                    return 0;
                case '\n':
                    getOutPrintWriter().println(Arrays.toString(this.mService.getFrozenPids(Integer.parseInt(getNextArgRequired()))));
                    return 0;
                case 11:
                    runDumpUids();
                    return 0;
                case '\f':
                    runListProcesses();
                    return 0;
                case '\r':
                    runDumpPackages();
                    return 0;
                case 14:
                    boolean enable = Boolean.parseBoolean(getNextArgRequired());
                    GreezeManagerDebugConfig.sEnable = enable;
                    pw.println("launch freeze enabled " + enable);
                    return 0;
                case 15:
                    dumpSkipUid();
                    return 0;
                case 16:
                    boolean debug = Boolean.parseBoolean(getNextArgRequired());
                    GreezeManagerDebugConfig.DEBUG_MILLET = debug;
                    GreezeManagerDebugConfig.DEBUG_LAUNCH_FROM_HOME = debug;
                    GreezeManagerDebugConfig.DEBUG_AIDL = debug;
                    GreezeManagerDebugConfig.DEBUG = debug;
                    GreezeManagerDebugConfig.DEBUG = debug;
                    pw.println("launch debug log enabled " + debug);
                    return 0;
                case 17:
                    runDumpHistory();
                    return 0;
                case 18:
                    GreezeManagerService.nLoopOnce();
                    return 0;
                case 19:
                    int module = Integer.parseInt(getNextArgRequired());
                    int[] rst = this.mService.getFrozenUids(module);
                    pw.println("Frozen uids : " + Arrays.toString(rst));
                    return -1;
                case 20:
                    int tmp_module = Integer.parseInt(getNextArgRequired());
                    this.mService.registerCallback(new TmpCallback(tmp_module), tmp_module);
                    return -1;
                case 21:
                    synchronized (this.mService.isoPids) {
                        for (int i = this.mService.isoPids.size() - 1; i >= 0; i--) {
                            List<Integer> pids = (List) this.mService.isoPids.valueAt(i);
                            if (pids != null) {
                                pw.println(" uid= " + this.mService.isoPids.keyAt(i) + " pids:" + pids.toString());
                            }
                        }
                    }
                    return -1;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Greeze manager (greezer) commands:");
            pw.println();
            pw.println("  ls lsfz");
            pw.println("  history");
            pw.println();
            pw.println("  fzpid PID");
            pw.println("  fzuid UID");
            pw.println();
            pw.println("  thpid PID");
            pw.println("  thuid UID");
            pw.println("  thaw");
            pw.println();
            pw.println("  monitor/unmonitor UID");
            pw.println("  clearmonitor");
            pw.println();
            pw.println("  query UID");
            pw.println("    Query binder state in all processes of UID");
            pw.println();
            pw.println("  uid pkg ps");
            pw.println();
            pw.println("  enable true/false");
        }
    }

    /* loaded from: classes.dex */
    static class TmpCallback extends IGreezeCallback.Stub {
        int module;

        public TmpCallback(int module) {
            this.module = module;
        }

        public void reportSignal(int uid, int pid, long now) {
        }

        public void reportNet(int uid, long now) {
        }

        public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) {
        }

        public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
        }

        public void serviceReady(boolean ready) {
        }

        public void thawedByOther(int uid, int pid, int module) {
            Log.e(GreezeManagerService.TAG, this.module + ": thawed uid:" + uid + " by:" + module);
        }
    }

    /* loaded from: classes.dex */
    private class LocalService extends GreezeManagerInternal {
        private LocalService() {
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean isRestrictBackgroundAction(String localhost, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
            return GreezeManagerService.this.isRestrictBackgroundAction(localhost, callerUid, callerPkgName, calleeUid, calleePkgName);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean isUidFrozen(int uid) {
            return GreezeManagerService.this.isUidFrozen(uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public int[] getFrozenUids(int module) {
            return GreezeManagerService.this.getFrozenUids(module);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public int[] getFrozenPids(int module) {
            return GreezeManagerService.this.getFrozenPids(module);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyExcuteServices(int uid) {
            GreezeManagerService.this.notifyExcuteServices(uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyBackup(int uid, boolean start) {
            GreezeManagerService.this.notifyBackup(uid, start);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyDumpAppInfo(int uid, int pid) {
            GreezeManagerService.this.notifyDumpAppInfo(uid, pid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyDumpAllInfo() {
            GreezeManagerService.this.notifyDumpAllInfo();
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public long getLastThawedTime(int uid, int module) {
            return GreezeManagerService.this.getLastThawedTime(uid, module);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyResumeTopActivity(int uid, String packageName, boolean inMultiWindowMode) {
            GreezeManagerService.this.notifyResumeTopActivity(uid, packageName, inMultiWindowMode);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyMovetoFront(int uid, boolean inFreeformSmallWinMode) {
            GreezeManagerService.this.notifyMovetoFront(uid, inFreeformSmallWinMode);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyMultitaskLaunch(int uid, String packageName) {
            GreezeManagerService.this.notifyMultitaskLaunch(uid, packageName);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void notifyFreeformModeFocus(String packageName, int mode) {
            GreezeManagerService.this.notifyFreeformModeFocus(packageName, mode);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean registerCallback(IGreezeCallback callback, int module) throws RemoteException {
            return GreezeManagerService.this.registerCallback(callback, module);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean thawUid(int uid, int fromWho, String reason) {
            return GreezeManagerService.this.thawUid(uid, fromWho, reason);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public List<Integer> thawUids(int[] uids, int fromWho, String reason) {
            return GreezeManagerService.this.thawUids(uids, fromWho, reason);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public List<Integer> thawPids(int[] pids, int fromWho, String reason) {
            return GreezeManagerService.this.thawPids(pids, fromWho, reason);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public List<Integer> freezeUids(int[] uids, long timeout, int fromWho, String reason, boolean checkAudioGps) {
            return GreezeManagerService.this.freezeUids(uids, timeout, fromWho, reason, checkAudioGps);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public List<Integer> freezePids(int[] pids, long timeout, int fromWho, String reason) {
            return GreezeManagerService.this.freezePids(pids, timeout, fromWho, reason);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void queryBinderState(int uid) {
            GreezeManagerService.this.queryBinderState(uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void finishLaunchMode(String processName, int uid) {
            GreezeManagerService.this.finishLaunchMode(processName, uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void triggerLaunchMode(String processName, int uid) {
            GreezeManagerService.this.triggerLaunchMode(processName, uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean freezePid(int pid) {
            return FreezeUtils.freezePid(pid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean freezePid(int pid, int uid) {
            return FreezeUtils.freezePid(pid, uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean thawPid(int pid) {
            return FreezeUtils.thawPid(pid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean thawPid(int pid, int uid) {
            return FreezeUtils.thawPid(pid, uid);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void updateOrderBCStatus(String intentAction, int uid, boolean isforeground, boolean allow) {
            GreezeManagerService.this.updateOrderBCStatus(intentAction, uid, isforeground, allow);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean checkAurogonIntentDenyList(String action) {
            return GreezeManagerService.this.checkAurogonIntentDenyList(action);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean isNeedCachedBroadcast(Intent intent, int uid, String packageName, boolean force) {
            return GreezeManagerService.this.isNeedCachedBroadcast(intent, uid, packageName);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public boolean isRestrictReceiver(Intent intent, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
            return GreezeManagerService.this.isRestrictReceiver(intent, callerUid, callerPkgName, calleeUid, calleePkgName);
        }

        @Override // com.miui.server.greeze.GreezeManagerInternal
        public void handleAppZygoteStart(ApplicationInfo info, boolean start) {
            GreezeManagerService.this.handleAppZygoteStart(info, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyDisplayListener implements DisplayManager.DisplayListener {
        private MyDisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            ComponentName componentName;
            ArraySet<Integer> uids = new ArraySet<>();
            Display display = GreezeManagerService.this.mDisplayManager.getDisplay(displayId);
            if (display == null) {
                return;
            }
            int state = display.getState();
            if (state == 2 && displayId != 0) {
                try {
                    List<ActivityTaskManager.RootTaskInfo> rootTaskInfos = GreezeManagerService.this.mActivityTaskManager.getAllRootTaskInfosOnDisplay(displayId);
                    for (ActivityTaskManager.RootTaskInfo rootTaskInfo : rootTaskInfos) {
                        if (rootTaskInfo != null && rootTaskInfo.visible && rootTaskInfo.childTaskNames.length > 0 && (componentName = ComponentName.unflattenFromString(rootTaskInfo.childTaskNames[0])) != null && componentName.getPackageName() != null) {
                            int uid = GreezeManagerService.this.getUidByPackageName(componentName.getPackageName());
                            uids.add(Integer.valueOf(uid));
                        }
                    }
                } catch (Exception e) {
                    Slog.e(GreezeManagerService.TAG, e.toString());
                }
                Iterator<Integer> it = uids.iterator();
                while (it.hasNext()) {
                    int needUnFreezeUid = it.next().intValue();
                    if (GreezeManagerService.this.isUidFrozen(needUnFreezeUid)) {
                        GreezeManagerService.this.thawUid(needUnFreezeUid, 1000, "Display_On");
                    }
                }
            }
        }
    }

    private void registerDisplayChanageListener() {
        this.mDisplayManager.registerDisplayListener(new MyDisplayListener(), this.mHandler);
    }

    public int getUidByPackageName(String packageName) {
        Context context = this.mContext;
        if (context == null || context.getPackageManager() == null) {
            return 0;
        }
        try {
            int uid = this.mContext.getPackageManager().getPackageUid(packageName, 0);
            return uid;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can not found the app for " + packageName + "#0");
            return 0;
        }
    }
}
