package com.android.server.wm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.MiuiProcess;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.MiuiProcessStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.miui.server.rtboost.SchedBoostManagerInternal;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import miui.process.ProcessManager;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class RealTimeModeControllerImpl implements RealTimeModeControllerStub {
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 1;
    private static final String PERF_SHIELDER_GESTURE_KEY = "perf_shielder_GESTURE";
    private static final String PERF_SHIELDER_RTMODE_KEY = "perf_shielder_RTMODE";
    private static final String PERF_SHIELDER_RTMODE_MOUDLE = "perf_rtmode";
    private static final String RT_ENABLE_CLOUD = "perf_rt_enable";
    private static final String RT_ENABLE_TEMPLIMIT_CLOUD = "rt_enable_templimit";
    private static final String RT_GESTURE_ENABLE_CLOUD = "perf_rt_gesture_enable";
    public static final HashSet<String> RT_GESTURE_WHITE_LIST;
    private static final String RT_GESTURE_WHITE_LIST_CLOUD = "rt_gesture_white_list";
    private static final String RT_PKG_BLACK_LIST_CLOUD = "rt_pkg_black_list";
    private static final String RT_PKG_WHITE_LIST_CLOUD = "rt_pkg_white_list";
    private static final String RT_TEMPLIMIT_BOTTOM_CLOUD = "rt_templimit_bottom";
    private static final String RT_TEMPLIMIT_CEILING_CLOUD = "rt_templimit_ceiling";
    public static final String TAG = "RTMode";
    private SchedBoostManagerInternal mBoostInternal;
    private WindowState mCurrentFocus;
    private Handler mH;
    private HandlerThread mHandlerThread;
    private WindowManagerService mService;
    private static Context mContext = null;
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug_rtmode", false);
    public static boolean ENABLE_RT_MODE = SystemProperties.getBoolean("persist.sys.enable_rtmode", true);
    public static boolean IGNORE_CLOUD_ENABLE = SystemProperties.getBoolean("persist.sys.enable_ignorecloud_rtmode", false);
    public static boolean ENABLE_RT_GESTURE = SystemProperties.getBoolean("persist.sys.enable_sched_gesture", true);
    public static boolean ENABLE_TEMP_LIMIT_ENABLE = SystemProperties.getBoolean("persist.sys.enable_templimit", false);
    public static int RT_TEMPLIMIT_BOTTOM = SystemProperties.getInt("persist.sys.rtmode_templimit_bottom", 42);
    public static int RT_TEMPLIMIT_CEILING = SystemProperties.getInt("persist.sys.rtmode_templimit_ceiling", 45);
    public static final HashSet<String> RT_PKG_WHITE_LIST = new HashSet<>();
    public static final HashSet<String> RT_PKG_BLACK_LIST = new HashSet<>();
    private boolean isInit = false;
    private PackageManager mPm = null;
    private final HashMap<Integer, Boolean> mUidToAppBitType = new HashMap<>();
    private final Object mLock = new Object();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<RealTimeModeControllerImpl> {

        /* compiled from: RealTimeModeControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final RealTimeModeControllerImpl INSTANCE = new RealTimeModeControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public RealTimeModeControllerImpl m2769provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public RealTimeModeControllerImpl m2768provideNewInstance() {
            return new RealTimeModeControllerImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        RT_GESTURE_WHITE_LIST = hashSet;
        hashSet.add("com.miui.home");
        hashSet.add(AccessController.PACKAGE_SYSTEMUI);
        hashSet.add("com.mi.android.globallauncher");
        hashSet.add("com.android.provision");
        hashSet.add("com.miui.miwallpaper.snowmountain");
        hashSet.add("com.miui.miwallpaper.earth");
        hashSet.add("com.miui.miwallpaper.geometry");
        hashSet.add("com.miui.miwallpaper.saturn");
        hashSet.add("com.miui.miwallpaper.mars");
        hashSet.add(ActivityTaskManagerServiceImpl.FLIP_HOME_PACKAGE);
        hashSet.add("com.android.quicksearchbox");
    }

    public static RealTimeModeControllerImpl get() {
        return (RealTimeModeControllerImpl) RealTimeModeControllerStub.get();
    }

    public void init(Context context) {
        if (this.isInit) {
            return;
        }
        mContext = context;
        Resources r = context.getResources();
        String[] whiteList = r.getStringArray(285409471);
        RT_PKG_WHITE_LIST.addAll(Arrays.asList(whiteList));
        HandlerThread handlerThread = new HandlerThread("RealTimeModeControllerTh");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Process.setThreadGroupAndCpuset(this.mHandlerThread.getThreadId(), 2);
        this.mH = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.wm.RealTimeModeControllerImpl.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        RealTimeModeControllerImpl.this.registerCloudObserver(RealTimeModeControllerImpl.mContext);
                        RealTimeModeControllerImpl.this.updateCloudControlParas();
                        RealTimeModeControllerImpl.updateGestureCloudControlParas();
                        return;
                    default:
                        return;
                }
            }
        };
        this.isInit = true;
        Slog.d(TAG, "init RealTimeModeController");
    }

    public void onBootPhase() {
        Handler handler = this.mH;
        if (handler != null) {
            Message msg = handler.obtainMessage(1);
            this.mH.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.RealTimeModeControllerImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri())) {
                    RealTimeModeControllerImpl.this.updateCloudControlParas();
                    RealTimeModeControllerImpl.updateGestureCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateGestureCloudControlParas() {
        if (!IGNORE_CLOUD_ENABLE) {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(mContext.getContentResolver(), PERF_SHIELDER_RTMODE_MOUDLE, PERF_SHIELDER_GESTURE_KEY, "");
            if (TextUtils.isEmpty(data)) {
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(data);
                String rtGestureEnable = jsonObject.optString(RT_GESTURE_ENABLE_CLOUD);
                if (rtGestureEnable != null) {
                    ENABLE_RT_GESTURE = Boolean.parseBoolean(rtGestureEnable);
                    if (DEBUG) {
                        Slog.d(TAG, "rtGestureEnable cloud control set received : " + ENABLE_RT_GESTURE);
                    }
                }
                String rtGestureList = jsonObject.optString(RT_GESTURE_WHITE_LIST_CLOUD);
                if (!TextUtils.isEmpty(rtGestureList)) {
                    RT_GESTURE_WHITE_LIST.addAll(Arrays.asList(rtGestureList.split(",")));
                    if (DEBUG) {
                        Slog.d(TAG, "rtGestureList cloud control set received : " + rtGestureList);
                    }
                }
            } catch (JSONException e) {
                Slog.e(TAG, "updateCloudData error :", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudControlParas() {
        if (!IGNORE_CLOUD_ENABLE) {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(mContext.getContentResolver(), PERF_SHIELDER_RTMODE_MOUDLE, PERF_SHIELDER_RTMODE_KEY, "");
            if (TextUtils.isEmpty(data)) {
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(data);
                String rtModeEnable = jsonObject.optString(RT_ENABLE_CLOUD);
                if (rtModeEnable != null) {
                    ENABLE_RT_MODE = Boolean.parseBoolean(rtModeEnable);
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode enable cloud control set received : " + ENABLE_RT_MODE);
                    }
                }
                String rtWhiteList = jsonObject.optString(RT_PKG_WHITE_LIST_CLOUD);
                if (!TextUtils.isEmpty(rtWhiteList)) {
                    HashSet<String> hashSet = RT_PKG_WHITE_LIST;
                    hashSet.clear();
                    Resources r = mContext.getResources();
                    String[] whiteList = r.getStringArray(285409471);
                    hashSet.addAll(Arrays.asList(whiteList));
                    hashSet.addAll(Arrays.asList(rtWhiteList.split(",")));
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode rtWhiteList cloud control set received : " + rtWhiteList);
                    }
                }
                String rtBlackList = jsonObject.optString(RT_PKG_BLACK_LIST_CLOUD);
                if (!TextUtils.isEmpty(rtBlackList)) {
                    HashSet<String> hashSet2 = RT_PKG_BLACK_LIST;
                    hashSet2.clear();
                    hashSet2.addAll(Arrays.asList(rtBlackList.split(",")));
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode rtBlackList cloud control set received : " + rtBlackList);
                    }
                }
                String rtTempLimitBottom = jsonObject.optString(RT_TEMPLIMIT_BOTTOM_CLOUD);
                if (rtTempLimitBottom != null) {
                    RT_TEMPLIMIT_BOTTOM = Integer.parseInt(rtTempLimitBottom);
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode Temp Limit bottom cloud control set received : " + RT_TEMPLIMIT_BOTTOM);
                    }
                }
                String rtTempLimitCeiling = jsonObject.optString(RT_TEMPLIMIT_CEILING_CLOUD);
                if (rtTempLimitCeiling != null) {
                    RT_TEMPLIMIT_CEILING = Integer.parseInt(rtTempLimitCeiling);
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode Temp Limit ceiling cloud control set received : " + RT_TEMPLIMIT_CEILING);
                    }
                }
                String rtEnableTempLimit = jsonObject.optString(RT_ENABLE_TEMPLIMIT_CLOUD);
                if (rtEnableTempLimit != null) {
                    ENABLE_TEMP_LIMIT_ENABLE = Boolean.parseBoolean(rtEnableTempLimit);
                    if (DEBUG) {
                        Slog.d(TAG, "RTMode Temp Limit enable cloud control set received : " + ENABLE_TEMP_LIMIT_ENABLE);
                    }
                }
            } catch (JSONException e) {
                Slog.e(TAG, "updateCloudData error :", e);
            }
        }
    }

    public boolean couldBoostTopAppProcess(String currentPackage) {
        if (!ENABLE_RT_MODE) {
            if (DEBUG) {
                Slog.d(TAG, "ENABLE_RT_MODE : " + ENABLE_RT_MODE);
            }
            return false;
        }
        if (!RT_PKG_WHITE_LIST.contains(currentPackage) || RT_PKG_BLACK_LIST.contains(currentPackage)) {
            if (DEBUG) {
                Slog.d(TAG, currentPackage + "is not in whitelist or in blacklist!");
            }
            return false;
        }
        if (isCurrentApp32Bit()) {
            if (DEBUG) {
                Slog.d(TAG, currentPackage + "is a 32bit app, skip boost!");
            }
            return false;
        }
        return true;
    }

    public String getAppPackageName() {
        try {
            DisplayContent dc = this.mService.mRoot.getTopFocusedDisplayContent();
            if (dc != null) {
                synchronized (this.mLock) {
                    WindowState windowState = dc.mCurrentFocus;
                    this.mCurrentFocus = windowState;
                    if (windowState != null) {
                        return windowState.mSession.mPackageName;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "failed to getAppPackageName", e);
            return null;
        }
    }

    public WindowProcessController getWindowProcessController() {
        try {
            synchronized (this.mLock) {
                WindowState windowState = this.mCurrentFocus;
                if (windowState == null) {
                    return null;
                }
                ActivityRecord activityRecord = windowState.mActivityRecord;
                int pid = this.mCurrentFocus.mSession.mPid;
                WindowProcessController mHomeProcess = this.mService.mAtmService.mHomeProcess;
                if (activityRecord == null) {
                    return this.mService.mAtmService.mProcessMap.getProcess(pid);
                }
                if (mHomeProcess != null && mHomeProcess.mName == activityRecord.packageName && mHomeProcess.getPid() != pid) {
                    return this.mService.mAtmService.mProcessMap.getProcess(pid);
                }
                return activityRecord.app;
            }
        } catch (Exception e) {
            Slog.e(TAG, "failed to getWindowProcessController", e);
            return null;
        }
    }

    public int getCurrentFocusProcessPid() {
        synchronized (this.mLock) {
            WindowState windowState = this.mCurrentFocus;
            if (windowState == null) {
                return -1;
            }
            int pid = windowState.mSession.mPid;
            return pid;
        }
    }

    public static boolean isHomeProcess(WindowProcessController wpc) {
        return wpc.isHomeProcess();
    }

    public static boolean isSystemUIProcess(WindowProcessController wpc) {
        return TextUtils.equals(wpc.mName, AccessController.PACKAGE_SYSTEMUI);
    }

    public boolean checkCallerPermission(String pkgName) {
        if (!ENABLE_RT_GESTURE || !ENABLE_RT_MODE) {
            if (DEBUG) {
                Slog.d(TAG, "ENABLE_RT_MODE : " + ENABLE_RT_MODE + "ENABLE_RT_GESTURE : " + ENABLE_RT_GESTURE);
            }
            return false;
        }
        if (pkgName != null && RT_GESTURE_WHITE_LIST.contains(pkgName)) {
            return true;
        }
        if (DEBUG) {
            Slog.d(TAG, pkgName + "pkgName is null or has no permission");
        }
        return false;
    }

    public void setWindowManager(WindowManagerService mService) {
        this.mService = mService;
    }

    public void onFling(int durationMs) {
    }

    public void onScroll(boolean started) {
        if (started) {
            boostTopApp(MiuiProcessStub.getInstance().getSchedModeNormal(), MiuiProcessStub.getInstance().getScrollRtSchedDurationMs());
        }
    }

    public void onDown() {
        boostTopApp(MiuiProcessStub.getInstance().getSchedModeNormal(), MiuiProcessStub.getInstance().getTouchRtSchedDurationMs());
    }

    public void onMove() {
        boostTopApp(MiuiProcessStub.getInstance().getSchedModeNormal(), MiuiProcessStub.getInstance().getTouchRtSchedDurationMs());
    }

    private void boostTopApp(int mode, long durationMs) {
        String focusedPackage = getAppPackageName();
        if (focusedPackage == null) {
            Slog.e(TAG, "Error: package name null");
            return;
        }
        boolean couldBoost = couldBoostTopAppProcess(focusedPackage);
        if (couldBoost) {
            int pid = getCurrentFocusProcessPid();
            int renderThreadTid = ProcessManager.getRenderThreadTidByPid(pid);
            getSchedBoostService().beginSchedThreads(new int[]{pid, renderThreadTid}, durationMs, focusedPackage, mode);
            return;
        }
        getSchedBoostService().stopCurrentSchedBoost();
    }

    private boolean isCurrentApp32Bit() {
        synchronized (this.mLock) {
            WindowState windowState = this.mCurrentFocus;
            if (windowState == null) {
                return false;
            }
            int currentUid = windowState.mOwnerUid;
            String packageName = this.mCurrentFocus.mSession.mPackageName;
            if (currentUid < 10000) {
                return false;
            }
            if (!this.mUidToAppBitType.containsKey(Integer.valueOf(currentUid))) {
                boolean is32Bit = MiuiProcess.is32BitApp(mContext, packageName);
                this.mUidToAppBitType.put(Integer.valueOf(currentUid), Boolean.valueOf(is32Bit));
            }
            return this.mUidToAppBitType.get(Integer.valueOf(currentUid)).booleanValue();
        }
    }

    private SchedBoostManagerInternal getSchedBoostService() {
        if (this.mBoostInternal == null) {
            this.mBoostInternal = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        }
        return this.mBoostInternal;
    }

    public boolean checkThreadBoost(int tid) {
        if (this.isInit && ENABLE_RT_GESTURE && ENABLE_RT_MODE) {
            return getSchedBoostService().checkThreadBoost(tid);
        }
        return false;
    }

    public void setThreadSavedPriority(int[] tid, int prio) {
        if (!this.isInit || !ENABLE_RT_GESTURE || !ENABLE_RT_MODE) {
            return;
        }
        getSchedBoostService().setThreadSavedPriority(tid, prio);
    }

    public static void dump(PrintWriter pw, String[] args) {
        pw.println("persist.sys.debug_rtmode: " + DEBUG);
        pw.println("persist.sys.enable_rtmode: " + ENABLE_RT_MODE);
        pw.println("persist.sys.enable_ignorecloud_rtmode: " + IGNORE_CLOUD_ENABLE);
        pw.println("persist.sys.enable_sched_gesture: " + ENABLE_RT_GESTURE);
        pw.println("persist.sys.enable_templimit: " + ENABLE_TEMP_LIMIT_ENABLE);
        pw.println("rt_templimit_bottom: " + RT_TEMPLIMIT_BOTTOM);
        pw.println("rt_templimit_ceiling: " + RT_TEMPLIMIT_CEILING);
        pw.println("RT_PKG_WHITE_LIST: ");
        HashSet<String> hashSet = RT_PKG_WHITE_LIST;
        synchronized (hashSet) {
            Iterator<String> it = hashSet.iterator();
            while (it.hasNext()) {
                String white = it.next();
                pw.println("    " + white);
            }
        }
        pw.println("RT_PKG_BLACK_LIST: ");
        HashSet<String> hashSet2 = RT_PKG_BLACK_LIST;
        synchronized (hashSet2) {
            Iterator<String> it2 = hashSet2.iterator();
            while (it2.hasNext()) {
                String black = it2.next();
                pw.println("    " + black);
            }
        }
        pw.println("RT_GESTURE_WHITE_LIST: ");
        HashSet<String> hashSet3 = RT_GESTURE_WHITE_LIST;
        synchronized (hashSet3) {
            Iterator<String> it3 = hashSet3.iterator();
            while (it3.hasNext()) {
                String gest_white = it3.next();
                pw.println("    " + gest_white);
            }
        }
    }
}
