package com.android.server.wm;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityController;
import android.app.TaskSnapshotHelperStub;
import android.appcompat.ApplicationCompatUtilsStub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.window.ScreenCapture;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.server.LocalServices;
import com.android.server.MiuiFgThread;
import com.android.server.PinnerServiceStub;
import com.android.server.appcacheopt.AppCacheOptimizerStub;
import com.android.server.camera.CameraOpt;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.MiuiOrientationImpl;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.hybrid.hook.HookClient;
import com.miui.server.greeze.GreezeManagerInternal;
import com.miui.server.process.ProcessManagerInternal;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.PackageForegroundEvent;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.util.FeatureParser;
import miui.util.MiuiMultiDisplayTypeInfo;
import org.json.JSONArray;

/* loaded from: classes.dex */
public class ActivityTaskManagerServiceImpl extends ActivityTaskManagerServiceStub {
    private static final int CONTINUITY_NOTIFICATION_ID = 9990;
    private static final String EXPAND_DOCK = "expand_dock";
    private static final String EXPAND_OTHER = "expand_other";
    public static final String FLIP_HOME_CLASS = "com.miui.fliphome.FlipLauncher";
    public static final String FLIP_HOME_PACKAGE = "com.miui.fliphome";
    public static final String FLIP_HOME_SHORT_COMPONENT = "com.miui.fliphome/.FlipLauncher";
    private static final String GESTURE_LEFT_TO_RIGHT = "gesture_left_to_right";
    private static final String GESTURE_RIGHT_TO_LEFT = "gesture_right_to_left";
    public static final String KEY_MULTI_FINGER_SLIDE = "multi_finger_slide";
    private static final String MANAGED_PROFILE_NOT_SNAPSHOT_ACTIVITY = "com.android.cts.verifier/.managedprovisioning.RecentsRedactionActivity";
    private static final String MIUI_THEMEMANAGER_PKG = "com.android.thememanager";
    private static final int PACKAGE_FORE_BUFFER_SIZE;
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PRESS_META_KEY_AND_W = "press_meta_key_and_w";
    private static final String SNAP_TO_LEFT = "snap_to_left";
    private static final String SNAP_TO_RIGHT = "snap_to_right";
    private static final String SPLIT_SCREEN_BLACK_LIST = "miui_resize_black_list";
    private static final String SPLIT_SCREEN_BLACK_LIST_FOR_FOLD = "miui_resize_black_list_for_fold";
    private static final String SPLIT_SCREEN_BLACK_LIST_FOR_PAD = "miui_resize_black_list_for_pad";
    private static final String SPLIT_SCREEN_MODULE_NAME = "split_screen_applist";
    private static final String SPLIT_SCREEN_MULTI_TASK_WHITE_LIST = "miui_multi_task_white_list";
    private static final String SUBSCREEN_MAIN_ACTIVITY = "com.xiaomi.misubscreenui.SubScreenMainActivity";
    private static final String SUBSCREEN_PKG = "com.xiaomi.misubscreenui";
    private static final boolean SUPPORT_MULTIPLE_TASK = true;
    private static final String TAG = "ATMSImpl";
    private static final String THREE_GESTURE_DOCK_TASK = "three_gesture_dock_task";
    private static final String UPDATE_SPLIT_SNAP_TARGET = "update_split_snap_target";
    private static final int WIDE_SCREEN_SIZE = 600;
    private static String lastForegroundPkg;
    private static ApplicationInfo lastMultiWindowAppInfo;
    private static final HashSet<String> mIgnoreUriCheckPkg;
    private static int mLastMainDisplayTopTaskId;
    private static final List<PackageForegroundEvent> sCachedForegroundPackageList;
    private static boolean sSystemBootCompleted;
    private AppCompatTask mAppCompatTask;
    public ActivityTaskManagerService mAtmService;
    public Context mContext;
    private FoldablePackagePolicy mFoldablePackagePolicy;
    public MiuiOrientationImpl.FullScreenPackageManager mFullScreenPackageManager;
    private volatile boolean mInAnimationOut;
    private MiuiSizeCompatInternal mMiuiSizeCompatIn;
    public PackageConfigurationController mPackageConfigurationController;
    String mPackageHoldOn;
    private PackageManagerService.IPackageManagerImpl mPackageManager;
    public PackageSettingsManager mPackageSettingsManager;
    ProcessManagerInternal mProcessManagerIn;
    private int mRestartingTaskId;
    private SurfaceControl mScreenshotLayer;
    private SmartPowerServiceInternal mSmartPowerService;
    private String mTargePackageName;
    private ActivityTaskSupervisor mTaskSupervisor;
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private static final Interpolator FAST_OUT_SLOW_IN_REVERSE = new PathInterpolator(0.8f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 0.6f, 1.0f);
    private static final ArraySet<String> BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO = new ArraySet<>();
    private boolean mAppContinuityIsUnfocused = false;
    private HashSet<String> mResizeBlackList = new HashSet<>();
    private HashSet<String> mMultipleTaskWhiteList = new HashSet<>();
    private final boolean SUPPORT_DECLINE_BACKGROUND_COLOR = FeatureParser.getBoolean("support_decline_background_color", false);
    private List<String> mDeclineColorActivity = new ArrayList();
    private HashSet<Integer> mTransitionSyncIdList = new HashSet<>();
    private List<Integer> mAdvanceTaskIds = new ArrayList();
    private boolean isHasActivityControl = false;
    private int mActivityControlUid = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityTaskManagerServiceImpl> {

        /* compiled from: ActivityTaskManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityTaskManagerServiceImpl INSTANCE = new ActivityTaskManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActivityTaskManagerServiceImpl m2429provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActivityTaskManagerServiceImpl m2428provideNewInstance() {
            return new ActivityTaskManagerServiceImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        mIgnoreUriCheckPkg = hashSet;
        hashSet.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        hashSet.add("com.miui.mishare.connectivity");
        hashSet.add("com.miui.securitycore");
        sCachedForegroundPackageList = new ArrayList();
        PACKAGE_FORE_BUFFER_SIZE = SystemProperties.getInt("sys.proc.fore_pkg_buffer", 15);
        lastForegroundPkg = null;
        lastMultiWindowAppInfo = null;
    }

    public static ActivityTaskManagerServiceImpl getInstance() {
        return (ActivityTaskManagerServiceImpl) ActivityTaskManagerServiceStub.get();
    }

    public MiuiActivityController getMiuiActivityController() {
        return MiuiActivityControllerImpl.INSTANCE;
    }

    void init(ActivityTaskManagerService atms, Context context) {
        this.mAtmService = atms;
        this.mTaskSupervisor = atms.mTaskSupervisor;
        this.mContext = context;
        MiuiOrientationImpl.getInstance().init(this.mContext, this, atms);
        ActivityStarterImpl.getInstance().init(atms, this.mContext);
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        PassivePenAppWhiteListImpl.getInstance().init(atms, this.mContext);
    }

    void onSystemReady() {
        MiuiSizeCompatInternal miuiSizeCompatInternal = (MiuiSizeCompatInternal) LocalServices.getService(MiuiSizeCompatInternal.class);
        this.mMiuiSizeCompatIn = miuiSizeCompatInternal;
        if (miuiSizeCompatInternal != null) {
            miuiSizeCompatInternal.onSystemReady(this.mAtmService);
        }
        this.mFullScreenPackageManager = MiuiOrientationImpl.getInstance().getPackageManager();
        MiuiOrientationImpl.getInstance().onSystemReady();
        this.mPackageSettingsManager = new PackageSettingsManager(this);
        this.mPackageConfigurationController = new PackageConfigurationController(this.mAtmService);
        FoldablePackagePolicy foldablePackagePolicy = new FoldablePackagePolicy(this);
        this.mFoldablePackagePolicy = foldablePackagePolicy;
        this.mPackageConfigurationController.registerPolicy(foldablePackagePolicy);
        this.mPackageConfigurationController.startThread();
        this.mPackageManager = ServiceManager.getService("package");
        this.mProcessManagerIn = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        ActivityStarterImpl.getInstance().onSystemReady();
        this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerServiceImpl.this.lambda$onSystemReady$0();
            }
        });
        PassivePenAppWhiteListImpl.getInstance().onSystemReady();
    }

    public MiuiSizeCompatInternal getMiuiSizeCompatIn() {
        return this.mMiuiSizeCompatIn;
    }

    public List<String> getContinuityList(String policyName) {
        return this.mFoldablePackagePolicy.getContinuityList(policyName);
    }

    public long getContinuityVersion() {
        return this.mFoldablePackagePolicy.getContinuityVersion();
    }

    public boolean inMiuiGameSizeCompat(String pkgName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.inMiuiGameSizeCompat(pkgName);
    }

    public Intent hookStartActivity(Intent intent, String callingPackage) {
        Intent redirectedIntent = HookClient.redirectStartActivity(intent, callingPackage);
        CameraOpt.callMethod("notify", intent);
        return redirectedIntent;
    }

    public String hookGetCallingPkg(IBinder token, String originCallingPkg) {
        String hostApp = null;
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
            if (r != null) {
                hostApp = r.packageName;
            }
        }
        return HookClient.hookGetCallingPkg(hostApp, originCallingPkg);
    }

    boolean isGetTasksOpAllowed(String caller, int pid, int uid) {
        if (AppOpsUtils.isXOptMode() || !"getRunningAppProcesses".equals(caller)) {
            return false;
        }
        String packageName = null;
        synchronized (this.mAtmService.mGlobalLock) {
            WindowProcessController wpc = this.mAtmService.mProcessMap.getProcess(pid);
            if (wpc != null && wpc.mInfo != null) {
                packageName = wpc.mInfo.packageName;
            }
        }
        if (packageName == null) {
            return false;
        }
        AppOpsManager opsManager = this.mAtmService.getAppOpsManager();
        return opsManager.checkOp(10019, uid, packageName) == 0;
    }

    public void onConfigurationChanged(int displayId) {
    }

    boolean ignoreSpecifiedSource(String pkg) {
        return mIgnoreUriCheckPkg.contains(pkg);
    }

    public void onFreeFormToFullScreen(final ActivityRecord r) {
        if (r == null || r.app == null) {
            return;
        }
        final ActivityRecord.State state = r.getState();
        final int pid = r.app.getPid();
        MiuiFgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                ActivityTaskManagerServiceImpl.this.onForegroundActivityChanged(r, state, pid, null);
            }
        });
    }

    public void updateTopActivity(ActivityRecord r) {
        if (r == null || r.app == null || skipReportForegroundActivityChange(r)) {
            return;
        }
        reportForegroundActivityChange(r);
    }

    private void reportForegroundActivityChange(final ActivityRecord r) {
        final ApplicationInfo multiWindowAppInfo = this.mProcessManagerIn.getMultiWindowForegroundAppInfoLocked();
        final ActivityRecord.State state = r.getState();
        final int pid = r.app.getPid();
        MiuiFgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.2
            @Override // java.lang.Runnable
            public void run() {
                ActivityTaskManagerServiceImpl.this.onForegroundActivityChanged(r, state, pid, multiWindowAppInfo);
            }
        });
    }

    public void onForegroundActivityChangedLocked(ActivityRecord r) {
        if (r.app == null || r.getRootTask() == null) {
            return;
        }
        if (r.getRootTask().getWindowingMode() == 5) {
            Slog.i(TAG, "do not report freeform event");
            return;
        }
        this.mSmartPowerService.onForegroundActivityChangedLocked(r.info.name, r.getUid(), r.getPid(), r.packageName, r.launchedFromUid, r.launchedFromPid, r.launchedFromPackage, r.mActivityRecordStub.getIsColdStart(), r.getRootTask().mTaskId, r.getRootTask().mCallingUid, r.getRootTask().mCallingPackage);
        PinnerServiceStub.get().onActivityChanged(r.packageName);
        if (skipReportForegroundActivityChange(r)) {
            return;
        }
        if (GreezeManagerInternal.getInstance() != null) {
            GreezeManagerInternal.getInstance().notifyResumeTopActivity(r.getUid(), r.packageName, r.inMultiWindowMode());
        }
        reportForegroundActivityChange(r);
        if (ApplicationCompatUtilsStub.get().isContinuityEnabled()) {
            AppContinuityRouterStub.get().onForegroundActivityChangedLocked(r);
        }
    }

    private boolean skipReportForegroundActivityChange(ActivityRecord r) {
        if (PreloadStateManagerStub.get().isPreloadDisplayId(r.getDisplayId())) {
            Slog.i(TAG, "do not report preloadApp event");
            return true;
        }
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && 2 == r.getDisplayId()) {
            Slog.i(TAG, "do not report subscreen event");
            return true;
        }
        return false;
    }

    void onForegroundActivityChanged(ActivityRecord record, ActivityRecord.State state, int pid, ApplicationInfo multiWindowAppInfo) {
        if (record == null || record.app == null || TextUtils.isEmpty(record.packageName)) {
            Slog.w(TAG, "next or next process is null, skip report!");
            return;
        }
        OneTrackRotationHelper.getInstance().reportPackageForeground(record.packageName);
        boolean skipReportForeground = false;
        synchronized (record.mAtmService.mGlobalLock) {
            if (!record.isTopRunningActivity() && this.mAtmService.isInSplitScreenWindowingMode()) {
                Slog.w(TAG, "Don't report foreground because " + record.shortComponentName + " is not top running.");
                skipReportForeground = true;
            }
        }
        if ((!TextUtils.equals(record.packageName, lastForegroundPkg) || lastMultiWindowAppInfo != multiWindowAppInfo) && !skipReportForeground) {
            this.mProcessManagerIn.notifyForegroundInfoChanged(new FgActivityChangedInfo(record, state, pid, multiWindowAppInfo));
            reportPackageForeground(record, pid, lastForegroundPkg);
            lastForegroundPkg = record.packageName;
            lastMultiWindowAppInfo = multiWindowAppInfo;
        }
        MiuiMiPerfStub.getInstance().onAfterActivityResumed(record);
        AppCacheOptimizerStub.getInstance().onForegroundActivityChanged(record.packageName);
        this.mProcessManagerIn.notifyActivityChanged(record.mActivityComponent);
    }

    void updateTopResumedActivity(ActivityRecord prev, ActivityRecord top) {
        if (top == null || top.getRootTask() == null) {
            return;
        }
        TransitionController tc = top.mTransitionController;
        if (tc != null && tc.isTransientLaunch(prev) && tc.isTransientHide(top.getRootTask()) && prev != null && prev.isActivityTypeHomeOrRecents()) {
            boolean isTopEnterMini = tc.getCollectingTransitionType() == 2147483546 && (tc.inCollectingTransition(top) || tc.getCollectingTransition().mParticipants.size() == 0);
            if (isTopEnterMini) {
                Slog.i(TAG, "updateTopResumedActivity do not report freeform event");
                return;
            } else {
                onForegroundActivityChangedLocked(top);
                return;
            }
        }
        if (!top.inMultiWindowMode()) {
            updateTopActivity(top);
        }
    }

    public static void onForegroundWindowChanged(WindowProcessController app, ActivityInfo info, ActivityRecord record, ActivityRecord.State state) {
        if (app != null && info != null) {
            ((ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class)).notifyForegroundWindowChanged(new FgWindowChangedInfo(record, info.applicationInfo, app.getPid()));
        }
    }

    private static void reportPackageForeground(ActivityRecord record, int pid, String lastPkgName) {
        PackageForegroundEvent event = new PackageForegroundEvent();
        event.setPackageName(record.packageName);
        event.setComponentName(record.shortComponentName);
        event.setIdentity(System.identityHashCode(record));
        event.setPid(pid);
        event.setForegroundTime(SystemClock.uptimeMillis());
        event.setColdStart(record.mActivityRecordStub.getIsColdStart());
        event.setLastPackageName(lastPkgName);
        List<PackageForegroundEvent> list = sCachedForegroundPackageList;
        list.add(event);
        if (list.size() >= PACKAGE_FORE_BUFFER_SIZE && isSystemBootCompleted()) {
            Slog.d(TAG, "Begin to report package foreground events...");
            List<PackageForegroundEvent> events = new ArrayList<>();
            events.addAll(list);
            list.clear();
            reportPackageForegroundEvents(events);
        }
    }

    private static void reportPackageForegroundEvents(List<PackageForegroundEvent> events) {
        final ParceledListSlice<PackageForegroundEvent> reportEvents = new ParceledListSlice<>(events);
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                MQSEventManagerDelegate.getInstance().reportPackageForegroundEvents(reportEvents);
            }
        });
    }

    private static boolean isSystemBootCompleted() {
        if (!sSystemBootCompleted) {
            sSystemBootCompleted = "1".equals(SystemProperties.get("sys.boot_completed"));
        }
        return sSystemBootCompleted;
    }

    public float getAspectRatio(String packageName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal == null) {
            return -1.0f;
        }
        return miuiSizeCompatInternal.getAspectRatioByPackage(packageName);
    }

    public int getScaleMode(String packageName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal == null) {
            return 0;
        }
        return miuiSizeCompatInternal.getScaleModeByPackage(packageName);
    }

    public int getAspectGravity(String packageName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal != null) {
            return miuiSizeCompatInternal.getAspectGravityByPackage(packageName);
        }
        return 17;
    }

    public int getPolicy(String packageName) {
        return this.mPackageSettingsManager.mDisplayCompatPackages.getPolicy(packageName);
    }

    public String getPackageHoldOn() {
        return this.mPackageHoldOn;
    }

    public void setPackageHoldOn(ActivityTaskManagerService atms, String packageName) {
        Task stack;
        if (!TextUtils.isEmpty(packageName)) {
            List<ActivityTaskManager.RootTaskInfo> stackList = this.mAtmService.getAllRootTaskInfos();
            for (ActivityTaskManager.RootTaskInfo info : stackList) {
                if (info.topActivity != null && info.topActivity.getPackageName().equals(packageName) && (stack = atms.mRootWindowContainer.getRootTask(info.taskId)) != null && stack.getTopActivity(false, true) != null) {
                    this.mPackageHoldOn = packageName;
                    WindowManagerInternal wm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                    wm.setHoldOn(stack.getTopActivity(false, true).token, true);
                    PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
                    pm.goToSleep(SystemClock.uptimeMillis());
                    Slog.i(TAG, "Going to sleep and hold on, package name in hold on: " + this.mPackageHoldOn);
                    return;
                }
            }
            return;
        }
        this.mPackageHoldOn = null;
    }

    public void onFreeFormToFullScreen(Task task) {
        Slog.i(TAG, "onFreeFormToFullScreen task: " + task);
        Task rootTask = task.getRootTask();
        if (rootTask != null) {
            onFreeFormToFullScreen(rootTask.topRunningActivityLocked());
        }
    }

    public static int handleFreeformModeRequst(IBinder token, int cmd, Context mContext) {
        int result = -1;
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityRecord r = ActivityRecord.forTokenLocked(token);
            int i = 0;
            switch (cmd) {
                case 0:
                    if (r != null) {
                        i = r.getWindowingMode();
                    }
                    result = i;
                    break;
                case 1:
                    Settings.Secure.putString(mContext.getContentResolver(), "gamebox_stick", r.getTask().getBaseIntent().getComponent().flattenToShortString());
                    break;
                case 2:
                    Settings.Secure.putString(mContext.getContentResolver(), "gamebox_stick", "");
                    break;
                case 3:
                    String component = Settings.Secure.getString(mContext.getContentResolver(), "gamebox_stick");
                    if (r != null && r.getTask().getBaseIntent().getComponent().flattenToShortString().equals(component)) {
                        i = 1;
                    }
                    result = i;
                    break;
            }
            return result;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateResizeBlackList(Context context) {
        String data;
        updateMultiTaskWhiteList(context);
        try {
            if (Build.IS_TABLET) {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST_FOR_PAD, (String) null);
            } else if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST_FOR_FOLD, (String) null);
            } else {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST, (String) null);
            }
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                for (int i = 0; i < apps.length(); i++) {
                    this.mResizeBlackList.add(apps.getString(i));
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Get splitscreen blacklist from xml: ", e);
        }
    }

    private void updateMultiTaskWhiteList(Context context) {
        try {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_MULTI_TASK_WHITE_LIST, (String) null);
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                for (int i = 0; i < apps.length(); i++) {
                    this.mMultipleTaskWhiteList.add(apps.getString(i));
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Get multi_task whitelist from xml: ", e);
        }
    }

    private void getSplitScreenBlackListFromXml() {
        if (Build.IS_TABLET) {
            this.mResizeBlackList.add("com.android.settings");
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409445)));
        } else if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            this.mResizeBlackList.add("com.android.settings");
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409444)));
        } else {
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409443)));
        }
        if (Build.IS_TABLET) {
            this.mMultipleTaskWhiteList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409441)));
        } else {
            this.mMultipleTaskWhiteList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409440)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: registerObserver, reason: merged with bridge method [inline-methods] */
    public void lambda$onSystemReady$0() {
        getSplitScreenBlackListFromXml();
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                ActivityTaskManagerServiceImpl activityTaskManagerServiceImpl = ActivityTaskManagerServiceImpl.this;
                activityTaskManagerServiceImpl.updateResizeBlackList(activityTaskManagerServiceImpl.mContext);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(URI_CLOUD_ALL_DATA_NOTIFY, false, observer, -1);
        observer.onChange(false);
    }

    public boolean inResizeBlackList(String packageName) {
        return this.mResizeBlackList.contains(packageName);
    }

    public boolean inMultipleTaskWhiteList(String packageName) {
        if ("android".equals(packageName)) {
            return true;
        }
        return this.mMultipleTaskWhiteList.contains(packageName);
    }

    public boolean forceLaunchNewTaskForMultipleTask(ActivityRecord sourceRecord, ActivityRecord intentActivity, int launchFlag, ActivityOptions options) {
        Task launchRootTask;
        if (sourceRecord != null || intentActivity == null || options == null || !inMultipleTaskWhiteList(intentActivity.packageName) || options.getLaunchRootTask() == null || (134217728 & launchFlag) == 0 || (launchRootTask = Task.fromWindowContainerToken(options.getLaunchRootTask())) == null || !launchRootTask.mCreatedByOrganizer || launchRootTask.getWindowingMode() != 6) {
            return false;
        }
        Slog.d(TAG, "Use new task for split, intentActivity: " + intentActivity);
        return true;
    }

    public void restartSubScreenUiIfNeeded(int userId, String reason) {
        try {
            ApplicationInfo aInfo = AppGlobals.getPackageManager().getApplicationInfo(SUBSCREEN_PKG, FormatBytesUtil.KB, userId);
            if (aInfo == null) {
                Slog.d(TAG, "aInfo is null when start subscreenui for user " + userId);
            } else {
                restartSubScreenUiIfNeeded(aInfo, reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restartSubScreenUiIfNeeded(final ApplicationInfo info, final String reason) {
        if (info == null || !SUBSCREEN_PKG.equals(info.packageName) || isCTS() || this.mAtmService == null) {
            return;
        }
        BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManagerServiceImpl.this.startSubScreenUi(info, reason);
                } catch (Exception e) {
                    Slog.e(ActivityTaskManagerServiceImpl.TAG, e.toString());
                }
            }
        }, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startSubScreenUi(ApplicationInfo info, String reason) {
        try {
            int userId = UserHandle.getUserId(info.uid);
            if (userId == this.mAtmService.getCurrentUserId() && isSubScreenFeatureOn(this.mAtmService.mContext, userId) && !shouldNotStartSubscreen()) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchWindowingMode(1);
                options.setLaunchDisplayId(2);
                Intent intent = new Intent("android.intent.action.MAIN");
                ComponentName componentName = new ComponentName(SUBSCREEN_PKG, SUBSCREEN_MAIN_ACTIVITY);
                intent.setComponent(componentName);
                intent.setFlags(268435456);
                ActivityInfo aInfo = AppGlobals.getPackageManager().getActivityInfo(componentName, FormatBytesUtil.KB, userId);
                Slog.d(TAG, "starSubScreenActivity: " + reason + " for user " + userId);
                this.mAtmService.getActivityStartController().obtainStarter(intent, "starSubScreenActivity: " + reason).setCallingUid(0).setUserId(userId).setActivityInfo(aInfo).setActivityOptions(options.toBundle()).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isCTS() {
        return !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    private boolean isSubScreenFeatureOn(Context context, int userId) {
        return DeviceFeature.IS_SUBSCREEN_DEVICE && context != null && MiuiSettings.System.getBooleanForUser(context.getContentResolver(), "subscreen_switch", false, userId);
    }

    private boolean shouldNotStartSubscreen() {
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDisplayContent(2);
        ActivityRecord topRunningActivity = null;
        boolean switchUser = false;
        if (display != null) {
            topRunningActivity = display.topRunningActivity();
            switchUser = (topRunningActivity == null || topRunningActivity.mUserId == this.mAtmService.getCurrentUserId()) ? false : true;
            Slog.d(TAG, "shouldNotStartSubscreen topRunningActivity=" + topRunningActivity + ", switchUser=" + switchUser);
        }
        if (display != null) {
            return display.isSleeping() && topRunningActivity != null && topRunningActivity.getPid() > 0 && !switchUser;
        }
        return true;
    }

    void registerSubScreenSwitchObserver(Context context) {
        if (!DeviceFeature.IS_SUBSCREEN_DEVICE || context == null) {
            return;
        }
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("subscreen_switch"), false, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri, int flags) {
                super.onChange(selfChange, uri, flags);
                ActivityTaskManagerServiceImpl.this.restartSubScreenUiIfNeeded(flags, "subscreen feature switch on");
            }
        }, -1);
    }

    Configuration getGlobalConfigurationForMiui(ActivityTaskManagerService atms, WindowProcessController app) {
        if (Build.IS_MIUI && atms != null && app != null && MIUI_THEMEMANAGER_PKG.equals(app.mInfo.packageName) && app.getWindowingMode() == 6) {
            return atms.getGlobalConfiguration();
        }
        return null;
    }

    public ActivityInfo getLastResumedActivityInfo() {
        int uid = UserHandle.getAppId(Binder.getCallingUid());
        int pid = Binder.getCallingPid();
        ActivityInfo activityInfo = null;
        if (uid != 1000 && ActivityTaskManagerService.checkPermission("android.permission.REAL_GET_TASKS", pid, uid) != 0) {
            Slog.d(TAG, "permission denied for, callingPid:" + pid + " , callingUid:" + uid + ", requires: android.Manifest.permission.REAL_GET_TASKS");
            return null;
        }
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord activity = this.mAtmService.mLastResumedActivity;
            if (activity != null) {
                activityInfo = activity.info;
            }
        }
        return activityInfo;
    }

    public boolean shouldExcludeTaskFromRecents(Task task) {
        if (task == null || task.getBaseIntent() == null || task.getBaseIntent().getComponent() == null || !DeviceFeature.IS_SUBSCREEN_DEVICE) {
            return false;
        }
        String packageName = task.getBaseIntent().getComponent().getPackageName();
        return SUBSCREEN_PKG.equals(packageName) && 2 == task.getDisplayId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasMetaData(String packageName, String metaDataKey) {
        Bundle metaData;
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            if (applicationInfo != null && (metaData = applicationInfo.metaData) != null) {
                return metaData.get(metaDataKey) != null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getMetaDataBoolean(String packageName, String metaDataKey) {
        Bundle metaData;
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            if (applicationInfo != null && (metaData = applicationInfo.metaData) != null && metaData.get(metaDataKey) != null) {
                return metaData.getBoolean(metaDataKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getMetaDataFloat(String packageName, String metaDataKey) {
        Bundle metaData;
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            if (applicationInfo != null && (metaData = applicationInfo.metaData) != null && metaData.get(metaDataKey) != null) {
                return metaData.getFloat(metaDataKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    boolean executeShellCommand(String command, String[] args, PrintWriter pw) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal != null && miuiSizeCompatInternal.executeShellCommand(command, args, pw)) {
            return true;
        }
        if (ApplicationCompatUtilsStub.get().isContinuityEnabled() && AppContinuityRouterStub.get().executeShellCommand(command, args, pw)) {
            return true;
        }
        return this.mPackageConfigurationController.executeShellCommand(command, args, pw);
    }

    public ActivityManager.RunningTaskInfo getSplitTaskInfo(IBinder token) {
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord record = ActivityRecord.forTokenLocked(token);
            if (record != null && record.getTask() != null && (record.getTask().mTaskStub.isSplitMode() || record.getTask().inFreeformWindowingMode())) {
                return record.getTask().getTaskInfo();
            }
            return null;
        }
    }

    public boolean isFixedAspectRatioPackage(String packageName, int userId) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.getAspectRatioByPackage(packageName) > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args == null || args.length <= 1) {
            pw.println("dump nothing for ext!");
            return;
        }
        String extCmd = args[1];
        if ("packages".equals(extCmd)) {
            this.mPackageSettingsManager.dump(pw, "");
        }
    }

    public boolean setSplitScreenDirection(int direction) {
        ActivityTaskManagerService activityTaskManagerService;
        if (MiuiSoScManagerStub.get().isSoScSupported()) {
            return MiuiSoScManagerStub.get().setSplitScreenDirection(direction);
        }
        String str = "";
        if (direction == 0) {
            str = SNAP_TO_LEFT;
        }
        if (direction == 1) {
            str = SNAP_TO_RIGHT;
        }
        if (TextUtils.isEmpty(str) || (activityTaskManagerService = this.mAtmService) == null) {
            return false;
        }
        Task topFocusedStack = activityTaskManagerService.getTopDisplayFocusedRootTask();
        if (topFocusedStack == null) {
            Slog.d(TAG, "no stack focued, do noting.");
            return false;
        }
        if (isInSystemSplitScreen(topFocusedStack)) {
            MiuiSettings.System.putString(this.mAtmService.getUiContext().getContentResolver(), UPDATE_SPLIT_SNAP_TARGET, str);
        } else {
            boolean isLTR = true;
            if (direction == 0) {
                isLTR = false;
            }
            if (direction == 1) {
                isLTR = true;
            }
            toggleSplitByGesture(isLTR);
        }
        return true;
    }

    public void onMetaKeyCombination() {
        Object result;
        ActivityTaskManagerService activityTaskManagerService = this.mAtmService;
        if (activityTaskManagerService == null) {
            return;
        }
        synchronized (activityTaskManagerService.mGlobalLock) {
            Task topFocusedStack = this.mAtmService.getTopDisplayFocusedRootTask();
            if (topFocusedStack == null) {
                Slog.d(TAG, "no stack focued, do noting.");
                return;
            }
            int windowingMode = topFocusedStack.getWindowingMode();
            switch (windowingMode) {
                case 1:
                    if (!exitSplitScreenIfNeed(topFocusedStack) && (((result = invoke(MiuiSoScManagerStub.get(), "onMetaKeyCombination", new Object[0])) == null || !((Boolean) result).booleanValue()) && !topFocusedStack.isActivityTypeHomeOrRecents())) {
                        topFocusedStack.moveTaskToBack(topFocusedStack);
                        break;
                    }
                    break;
                case 5:
                    MiuiFreeFormManagerService miuiFreeFormManagerService = (MiuiFreeFormManagerService) this.mAtmService.mMiuiFreeFormManagerService;
                    miuiFreeFormManagerService.mFreeFormGestureController.exitFreeFormByKeyCombination(topFocusedStack);
                    break;
                default:
                    return;
            }
        }
    }

    private boolean exitSplitScreenIfNeed(WindowContainer wc) {
        if (!isVerticalSplit() || !isInSystemSplitScreen(wc)) {
            return false;
        }
        Task rootTask = wc.asTask().getRootTask();
        Task topLeafTask = rootTask.getTopLeafTask();
        boolean isExpandDock = topLeafTask.getBounds().left == 0;
        exitSplitScreen(isExpandDock);
        return true;
    }

    private void exitSplitScreen(boolean isExpandDock) {
        String str = isExpandDock ? EXPAND_DOCK : EXPAND_OTHER;
        MiuiSettings.System.putString(this.mAtmService.getUiContext().getContentResolver(), PRESS_META_KEY_AND_W, str);
    }

    public boolean isFreeFormExit(String packageName, int userId) {
        MiuiFreeFormManagerService miuiFreeFormManagerService = (MiuiFreeFormManagerService) this.mAtmService.mMiuiFreeFormManagerService;
        MiuiFreeFormActivityStack miuiFreeFormActivityStack = (MiuiFreeFormActivityStack) miuiFreeFormManagerService.getMiuiFreeFormActivityStack(packageName, userId);
        return MiuiDesktopModeUtils.isDesktopActive() ? (miuiFreeFormActivityStack == null || !miuiFreeFormActivityStack.isFrontFreeFormStackInfo() || miuiFreeFormActivityStack.isHideStackFromFullScreen() || miuiFreeFormManagerService.isAppBehindHome(miuiFreeFormActivityStack.mTask.mTaskId)) ? false : true : miuiFreeFormActivityStack != null;
    }

    public boolean isSplitExist(String packageName, int userId) {
        if (!this.mAtmService.isInSplitScreenWindowingMode()) {
            return false;
        }
        ActivityTaskManager.RootTaskInfo rootTaskInfo = retrieveFirstSplitWindowRootTaskInfo();
        boolean isInSplitWindow = false;
        if (rootTaskInfo != null) {
            ComponentName[] componentNames = getChildComponentNames(rootTaskInfo);
            int[] childTaskUserIds = getChildTaskUserIds(rootTaskInfo);
            Slog.d(TAG, "isInSplitWindow: childTaskNames = " + Arrays.toString(componentNames) + " childTaskUserIds =" + Arrays.toString(childTaskUserIds));
            int i = 0;
            while (true) {
                if (i >= childTaskUserIds.length || i >= componentNames.length) {
                    break;
                }
                if (componentNames[i] == null || !TextUtils.equals(packageName, componentNames[i].getPackageName()) || childTaskUserIds[i] != userId) {
                    i++;
                } else {
                    isInSplitWindow = true;
                    break;
                }
            }
        }
        Slog.d(TAG, "isInSplitWindow: " + isInSplitWindow);
        return isInSplitWindow;
    }

    public ActivityTaskManager.RootTaskInfo retrieveFirstSplitWindowRootTaskInfo() {
        List<ActivityTaskManager.RootTaskInfo> taskInfos = this.mAtmService.getAllRootTaskInfos();
        for (ActivityTaskManager.RootTaskInfo taskInfo : taskInfos) {
            int windowMode = taskInfo.getWindowingMode();
            boolean visible = taskInfo.visible;
            Slog.d(TAG, "retrieveFirstSplitWindowRootTaskInfo: windowMode = " + windowMode + " visible = " + visible);
            if (visible && windowMode == 1) {
                return taskInfo;
            }
        }
        return null;
    }

    public static ComponentName[] getChildComponentNames(ActivityTaskManager.RootTaskInfo rootTaskInfo) {
        String[] childTaskNames;
        ComponentName[] componentNames = new ComponentName[0];
        if (rootTaskInfo != null && (childTaskNames = rootTaskInfo.childTaskNames) != null) {
            int childTaskCount = childTaskNames.length;
            componentNames = new ComponentName[childTaskCount];
            for (int i = 0; i < childTaskCount; i++) {
                componentNames[i] = ComponentName.unflattenFromString(childTaskNames[i]);
            }
        }
        return componentNames;
    }

    public static int[] getChildTaskUserIds(ActivityTaskManager.RootTaskInfo rootTaskInfo) {
        int[] childUserIds;
        int[] childTaskUserIds = new int[0];
        if (rootTaskInfo != null && (childUserIds = rootTaskInfo.childTaskUserIds) != null) {
            int childTaskCount = childUserIds.length;
            int[] childTaskUserIds2 = new int[childTaskCount];
            System.arraycopy(childUserIds, 0, childTaskUserIds2, 0, childTaskCount);
            return childTaskUserIds2;
        }
        return childTaskUserIds;
    }

    public boolean isCameraForeground() {
        return TextUtils.equals(lastForegroundPkg, "com.android.camera");
    }

    public void notifyActivityResumed(ActivityRecord r, WindowManagerService wms) {
        if (r == null) {
            if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.i(TAG, "NotifyActivityResumed failed, activity = null");
            }
        } else {
            if (r.inMultiWindowMode()) {
                if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                    Slog.i(TAG, "In MultiWindowMode, wont do snapshot, return !....");
                    return;
                }
                return;
            }
            this.mSmartPowerService.onActivityReusmeUnchecked(r.info.name, r.getUid(), r.getPid(), r.packageName, r.launchedFromUid, r.launchedFromPid, r.launchedFromPackage, r.mActivityRecordStub.getIsColdStart());
            if (TaskSnapshotControllerInjectorStub.get().canTakeSnapshot(r)) {
                wms.mTaskSnapshotController.notifyAppResumed(r, true);
            } else if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.i(TAG, "No snapshot since not start by launcher, activity=" + r.mActivityComponent.flattenToShortString());
            }
        }
    }

    public void handleQSOnConfigureChanged(int userId, int change) {
        TaskSnapshotHelperStub.get().destroyQS(userId);
    }

    public boolean isAppSizeCompatRestarting(String pkgName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.isAppSizeCompatRestarting(pkgName);
    }

    public void splitTaskIfNeed(ActivityOptions activityOptions, Task task) {
        if (task == null || activityOptions == null || this.mAtmService == null) {
            return;
        }
        Object invoke = invoke(activityOptions, "getEnterAppPair", new Object[0]);
        Object invoke2 = invoke(activityOptions, "getAppPairPrimary", new Object[0]);
        if (invoke != null && ((Boolean) invoke).booleanValue()) {
            Task topRootTaskInWindowingMode = task.getDisplayArea().getTopRootTaskInWindowingMode(1);
            if ((topRootTaskInWindowingMode == null || !topRootTaskInWindowingMode.supportsMultiWindow() || !task.supportsMultiWindow()) && (topRootTaskInWindowingMode.getBaseIntent() == null || topRootTaskInWindowingMode.getBaseIntent().getComponent() == null || !skipTaskForMultiWindow(topRootTaskInWindowingMode.getBaseIntent().getComponent().getPackageName()))) {
                Slog.w(TAG, "The top app or drag's app is not supports multi window.");
                return;
            }
            if (topRootTaskInWindowingMode != task) {
                Slog.i(TAG, "pair t1:" + topRootTaskInWindowingMode + " t2:" + task + " isPrimary:" + invoke2);
                if (MiuiSoScManagerStub.get().isSoScSupported()) {
                    startSoSc(task, 1 ^ (((Boolean) invoke2).booleanValue() ? 1 : 0));
                    return;
                }
                if (topRootTaskInWindowingMode.isActivityTypeHomeOrRecents()) {
                    Slog.w(TAG, "The top app or drag's app is not supports multi window.");
                    return;
                }
                showScreenShotForSplitTask();
                task.setHasBeenVisible(true);
                task.sendTaskAppeared();
                this.mAtmService.mTaskOrganizerController.dispatchPendingEvents();
                invoke(this.mAtmService, "enterSplitScreen", Integer.valueOf(task.mTaskId), (Boolean) invoke2);
                return;
            }
            Slog.w(TAG, "The task has been front.");
        }
    }

    private void startSoSc(Task task, int position) {
        try {
            showScreenShotForSplitTask();
            task.setHasBeenVisible(true);
            task.sendTaskAppeared();
            this.mAtmService.mTaskOrganizerController.dispatchPendingEvents();
            MiuiSoScManagerStub.get().startTaskInSoSc(task.mTaskId, position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* renamed from: removeSplitTaskShotIfNeed, reason: merged with bridge method [inline-methods] */
    public boolean lambda$showScreenShotForSplitTask$2() {
        if (this.mScreenshotLayer != null && !this.mInAnimationOut) {
            this.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskManagerServiceImpl.this.animationOut();
                }
            });
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void animationOut() {
        if (this.mInAnimationOut) {
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        final SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ActivityTaskManagerServiceImpl.this.lambda$animationOut$1(t, valueAnimator);
            }
        });
        anim.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.7
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (ActivityTaskManagerServiceImpl.this.mScreenshotLayer != null) {
                    t.remove(ActivityTaskManagerServiceImpl.this.mScreenshotLayer).apply();
                    ActivityTaskManagerServiceImpl.this.mScreenshotLayer = null;
                }
                ActivityTaskManagerServiceImpl.this.mInAnimationOut = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                if (ActivityTaskManagerServiceImpl.this.mScreenshotLayer != null) {
                    t.remove(ActivityTaskManagerServiceImpl.this.mScreenshotLayer).apply();
                    ActivityTaskManagerServiceImpl.this.mScreenshotLayer = null;
                }
                ActivityTaskManagerServiceImpl.this.mInAnimationOut = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }
        });
        anim.setDuration(300L);
        anim.setStartDelay(200L);
        anim.setInterpolator(FAST_OUT_SLOW_IN_REVERSE);
        anim.start();
        this.mInAnimationOut = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animationOut$1(SurfaceControl.Transaction t, ValueAnimator animation) {
        float alpha = ((Float) animation.getAnimatedValue()).floatValue();
        SurfaceControl surfaceControl = this.mScreenshotLayer;
        if (surfaceControl != null) {
            t.setAlpha(surfaceControl, alpha).apply();
        }
    }

    public void showScreenShotForSplitTask() {
        if (this.mScreenshotLayer != null) {
            return;
        }
        DisplayContent dc = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        DisplayInfo info = dc.getDisplayInfo();
        Rect bounds = new Rect(0, 0, info.logicalWidth, info.logicalHeight);
        ScreenCapture.LayerCaptureArgs displayCaptureArgs = new ScreenCapture.LayerCaptureArgs.Builder(dc.getSurfaceControl()).setCaptureSecureLayers(true).setAllowProtected(true).setSourceCrop(bounds).build();
        ScreenCapture.ScreenshotHardwareBuffer screenshotHardwareBuffer = ScreenCapture.captureLayers(displayCaptureArgs);
        if (screenshotHardwareBuffer == null) {
            return;
        }
        GraphicBuffer buffer = GraphicBuffer.createFromHardwareBuffer(screenshotHardwareBuffer.getHardwareBuffer());
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        SurfaceControl build = dc.makeOverlay().setName("SplitTaskScreenShot").setOpaque(true).setSecure(false).setCallsite("SplitTaskScreenShot").setBLASTLayer().build();
        this.mScreenshotLayer = build;
        t.setLayer(build, 2000000);
        t.setBuffer(this.mScreenshotLayer, buffer);
        t.setColorSpace(this.mScreenshotLayer, screenshotHardwareBuffer.getColorSpace());
        t.show(this.mScreenshotLayer);
        t.apply();
        this.mAtmService.mUiHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerServiceImpl.this.lambda$showScreenShotForSplitTask$2();
            }
        }, 1000L);
        Slog.i(TAG, "show split task screen shot layer.");
    }

    public int getOrientation(Task task) {
        if ((task == null || task.getDisplayContent() == null || task.getDisplayContent().getConfiguration().smallestScreenWidthDp < WIDE_SCREEN_SIZE) && task != null) {
            Task rootTask = task.getRootTask();
            if (rootTask.mSoScRoot && MiuiSoScManagerStub.get().isInSoScSingleMode(rootTask) && (!rootTask.mTransitionController.isCollecting() || !rootTask.isFocusable())) {
                return 1;
            }
            if (!this.mTransitionSyncIdList.isEmpty() && task.getDisplayContent() != null && task.getDisplayContent().getRotation() == 0 && rootTask.mCreatedByOrganizer && rootTask.hasChild() && rootTask.getTopChild().getWindowingMode() == 6) {
                return 1;
            }
        }
        return -1;
    }

    public boolean isVerticalSplit() {
        return this.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getConfiguration().smallestScreenWidthDp >= WIDE_SCREEN_SIZE && !WindowManagerServiceImpl.getInstance().isCtsModeEnabled();
    }

    public boolean toggleSplitByGesture(boolean isLTR) {
        if (MiuiSoScManagerStub.get().isSoScSupported()) {
            MiuiSplitInputMethodStub.getInstance().updateImeVisiblityIfNeed(false);
            return MiuiSoScManagerStub.get().toggleSplitByGesture(isLTR);
        }
        if (!isVerticalSplit() || this.mAtmService == null) {
            return false;
        }
        if (Build.IS_TABLET && Settings.Global.getInt(this.mAtmService.getUiContext().getContentResolver(), KEY_MULTI_FINGER_SLIDE, 0) == 1) {
            WindowState statusBar = this.mAtmService.mRootWindowContainer.getDefaultDisplay().getDisplayPolicy().getStatusBar();
            boolean ignoreMultiFingerFlide = false;
            if (MiuiSoScManagerStub.get().isSoScSupported() && MiuiSoScManagerStub.get().isSingleModeActivated()) {
                ignoreMultiFingerFlide = true;
                Slog.i(TAG, "Ignore MULTI_FINGER_SLIDE.");
            }
            if (statusBar != null && !statusBar.isVisible() && !ignoreMultiFingerFlide) {
                Slog.i(TAG, "Ignore because MULTI_FINGER_SLIDE is setted and StatusBar is hidden.");
                return false;
            }
        }
        if (this.mAtmService.mWindowManager.mPolicy.isKeyguardLocked() || !this.mAtmService.mWindowManager.mPolicy.okToAnimate(false)) {
            Slog.i(TAG, "Ignore because keyguard is showing or turning screen off.");
            return false;
        }
        if (this.mAtmService.getLockTaskModeState() == 2) {
            return false;
        }
        Task focusedRootTask = this.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getFocusedRootTask();
        if (focusedRootTask == null) {
            Slog.i(TAG, "Ignore because get none focused task.");
            return false;
        }
        if (!focusedRootTask.supportsMultiWindow() && !isInSystemSplitScreen(focusedRootTask)) {
            Slog.i(TAG, "Ignore because the task not support SplitScreen mode.");
            this.mAtmService.getTaskChangeNotificationController().notifyActivityDismissingDockedRootTask();
            return false;
        }
        if (focusedRootTask.inFreeformWindowingMode()) {
            Slog.i(TAG, "Ignore because the task at FreeForm mode.");
            return false;
        }
        if (isInSystemSplitScreen(focusedRootTask)) {
            exitSplitScreen(isLTR);
            return false;
        }
        String threeGestureDockTask = MiuiSettings.System.getString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, "");
        if (!TextUtils.isEmpty(threeGestureDockTask)) {
            MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, "");
        }
        if (isLTR) {
            boolean triggered = MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, GESTURE_LEFT_TO_RIGHT);
            return triggered;
        }
        boolean triggered2 = MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, GESTURE_RIGHT_TO_LEFT);
        return triggered2;
    }

    public boolean isInSystemSplitScreen(WindowContainer wc) {
        if (wc == null) {
            return false;
        }
        Task task = null;
        if (wc instanceof Task) {
            task = wc.asTask();
        } else if (wc instanceof ActivityRecord) {
            task = ((ActivityRecord) wc).getTask();
        } else if (wc instanceof WindowState) {
            task = ((WindowState) wc).getTask();
        }
        if (task != null && skipTaskForMultiWindow(task.getPackageName())) {
            task = wc.getTaskDisplayArea().getNextFocusableRootTask(task, true);
        }
        if (task == null) {
            return false;
        }
        boolean isRoot = task.isRootTask();
        if (isRoot) {
            WindowContainer container = task.getTopChild();
            if (!task.mCreatedByOrganizer || task.getChildCount() != 2 || task.getWindowingMode() != 1 || container == null || container.asTask() == null || container.getWindowingMode() != 6 || !container.hasChild()) {
                return false;
            }
            return true;
        }
        Task rootTask = task.getRootTask();
        if (rootTask == null) {
            return false;
        }
        WindowContainer container2 = rootTask.getTopChild();
        if (!rootTask.mCreatedByOrganizer || !rootTask.hasChild() || rootTask.getWindowingMode() != 1 || container2 == null || container2.asTask() == null || container2.getWindowingMode() != 6) {
            return false;
        }
        return true;
    }

    public void setActivityController(IActivityController control) {
        Slog.d(TAG, "setActivityController callingUid:" + Binder.getCallingUid() + " control=" + (control != null));
        if (control != null) {
            this.mActivityControlUid = Binder.getCallingUid();
        } else {
            this.mActivityControlUid = -1;
        }
    }

    public int getActivityControllerUid() {
        return this.mActivityControlUid;
    }

    public List<Intent> getTopTaskVisibleActivities(Task mainStack) {
        if (mainStack == null) {
            return Collections.emptyList();
        }
        List<ActivityRecord> activityRecordList = mainStack.topRunningActivitiesLocked();
        if (activityRecordList == null) {
            return Collections.emptyList();
        }
        return (List) activityRecordList.stream().filter(new Predicate() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isState;
                isState = ((ActivityRecord) obj).isState(ActivityRecord.State.RESUMED);
                return isState;
            }
        }).map(new Function() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ActivityTaskManagerServiceImpl.lambda$getTopTaskVisibleActivities$4((ActivityRecord) obj);
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Intent lambda$getTopTaskVisibleActivities$4(ActivityRecord activityRecord) {
        Intent intent = new Intent();
        intent.setComponent(activityRecord.mActivityComponent);
        return intent;
    }

    public boolean freeFormAndFullScreenToggleByKeyCombination(boolean isStartFreeForm) {
        Slog.i(TAG, "freeFormAndFullScreenToggleByKeyCombination " + isStartFreeForm);
        MiuiFreeFormManagerService miuiFreeFormManagerService = (MiuiFreeFormManagerService) this.mAtmService.mMiuiFreeFormManagerService;
        miuiFreeFormManagerService.mFreeFormGestureController.freeFormAndFullScreenToggleByKeyCombination(isStartFreeForm);
        return true;
    }

    public boolean skipTaskForMultiWindow(String pkgName) {
        return Build.IS_TABLET && "com.android.quicksearchbox".equals(pkgName);
    }

    public void addTransitionSyncId(int syncId) {
        this.mTransitionSyncIdList.add(Integer.valueOf(syncId));
    }

    public void removeTransitionSyncId(int syncId) {
        this.mTransitionSyncIdList.remove(Integer.valueOf(syncId));
    }

    public void onLastResumedActivityChange(ActivityRecord r) {
        WindowManagerServiceStub.get().checkFlipFullScreenChange(r);
        if (!r.mActivityRecordStub.isInitialColor()) {
            WindowManagerServiceStub.get().saveNavigationBarColor(r.mActivityRecordStub.getNavigationBarColor());
        }
    }

    public boolean hideLockedProfile(ActivityRecord mLastResumedActivity) {
        if (mLastResumedActivity == null) {
            return true;
        }
        return true ^ MANAGED_PROFILE_NOT_SNAPSHOT_ACTIVITY.equals(mLastResumedActivity.intent.getComponent().flattenToShortString());
    }

    public static Object invoke(Object obj, String methodName, Object... values) {
        try {
            Class clazz = obj.getClass();
            if (values == null) {
                Method method = clazz.getDeclaredMethod(methodName, null);
                method.setAccessible(true);
                return method.invoke(obj, new Object[0]);
            }
            Class<?>[] argsClass = new Class[values.length];
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    argsClass[i] = Integer.TYPE;
                } else if (values[i] instanceof Boolean) {
                    argsClass[i] = Boolean.TYPE;
                } else if (values[i] instanceof Float) {
                    argsClass[i] = Float.TYPE;
                } else {
                    argsClass[i] = values[i].getClass();
                }
            }
            Method method2 = clazz.getDeclaredMethod(methodName, argsClass);
            method2.setAccessible(true);
            return method2.invoke(obj, values);
        } catch (Exception e) {
            Slog.d(TAG, "getDeclaredMethod:" + e.toString());
            return null;
        }
    }

    public boolean isControllerAMonkey() {
        ActivityTaskManagerService activityTaskManagerService;
        return Build.isDebuggable() && (activityTaskManagerService = this.mAtmService) != null && activityTaskManagerService.mController != null && this.mAtmService.mControllerIsAMonkey;
    }

    public void setPauseAdvancedForTask(int[] taskIds, boolean userLeaving) {
        Task task;
        ActivityRecord topNonFinishingActivity;
        for (int taskId : taskIds) {
            if (taskId > -1 && (task = this.mAtmService.mRootWindowContainer.anyTaskForId(taskId)) != null && (topNonFinishingActivity = task.getTopNonFinishingActivity()) != null) {
                TaskFragment taskFragment = topNonFinishingActivity.getTaskFragment();
                if (taskFragment == null) {
                    Slog.d(TAG, "setPauseAdvance: Task " + taskId + " not found.");
                } else {
                    this.mAdvanceTaskIds.add(Integer.valueOf(taskId));
                    taskFragment.mPauseAdvance = true;
                    userLeaving = taskFragment.mTransitionController.isTransientHide(task) && userLeaving;
                    if (canEnterPipOnTaskSwitch(topNonFinishingActivity, task, userLeaving)) {
                        topNonFinishingActivity.supportsEnterPipOnTaskSwitch = true;
                    }
                    taskFragment.startPausing(userLeaving, false, (ActivityRecord) null, "setPauseAdvanced");
                    Slog.d(TAG, "setPauseAdvanced: userLeaving= " + userLeaving + ",Task=" + task + ",resume=" + topNonFinishingActivity.shortComponentName);
                }
            }
        }
    }

    private boolean canEnterPipOnTaskSwitch(ActivityRecord top, Task task, boolean userLeaving) {
        Task rootTask;
        return (top == null || task == null || top.getWindowingMode() == 2 || (rootTask = task.getRootTask()) == null || rootTask.isActivityTypeAssistant() || !userLeaving) ? false : true;
    }

    public void unSetPauseAdvancedInner(boolean resume) {
        ActivityRecord topNonFinishingActivity;
        if (this.mAdvanceTaskIds.size() == 0) {
            return;
        }
        Iterator<Integer> it = this.mAdvanceTaskIds.iterator();
        while (it.hasNext()) {
            int taskId = it.next().intValue();
            Task task = this.mAtmService.mRootWindowContainer.anyTaskForId(taskId);
            if (task != null && (topNonFinishingActivity = task.getTopNonFinishingActivity()) != null) {
                TaskFragment taskFragment = topNonFinishingActivity.getTaskFragment();
                if (taskFragment == null) {
                    Slog.d(TAG, "unsetPauseAdvance: Task " + taskId + " not found.");
                } else {
                    if (taskFragment.mEnterPipDuringPauseAdvance != null) {
                        this.mAtmService.mH.post(taskFragment.mEnterPipDuringPauseAdvance);
                    }
                    taskFragment.mPauseAdvance = false;
                    taskFragment.mEnterPipDuringPauseAdvance = null;
                    if (resume) {
                        task.resumeTopActivityUncheckedLocked((ActivityRecord) null, (ActivityOptions) null);
                    }
                    Slog.d(TAG, "unSetPauseAdvance: Task=" + task + ",resume=" + resume);
                }
            }
        }
        this.mAdvanceTaskIds.clear();
    }

    public void addFlipActivityFullScreen(String setFlipActivityFullScreen) {
        ArraySet<String> arraySet = BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO;
        if (!arraySet.contains(setFlipActivityFullScreen)) {
            arraySet.add(setFlipActivityFullScreen);
        }
    }

    public void removeFlipActivityFullScreen(String removeFlipActivityFullScreen) {
        ArraySet<String> arraySet = BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO;
        if (arraySet.contains(removeFlipActivityFullScreen)) {
            arraySet.remove(removeFlipActivityFullScreen);
        }
    }

    public ArraySet<String> getAllFlipActivityFullScreen() {
        return BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO;
    }

    public boolean shouldNotApplyAspectRatio(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            return false;
        }
        return shouldNotApplyAspectRatio(activityRecord.mActivityComponent.toShortString().replaceAll("[{}]", "")) || shouldNotApplyAspectRatio(activityRecord.packageName);
    }

    public boolean shouldNotApplyAspectRatio(String name) {
        if (name == null) {
            return false;
        }
        if (MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            Slog.d(TAG, " shouldNotApplyAspectRatio = " + name + " ? " + BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO.contains(name));
        }
        return BLACK_LIST_SHOULD_NOT_APPLY_ASPECT_RATIO.contains(name);
    }

    public Intent updateHomeIntent(Intent intent) {
        if (ApplicationCompatUtilsStub.get().isDialogContinuityEnabled()) {
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
                return intent;
            }
            if (ApplicationCompatRouterStub.get().getConfigDisplayID() == 5) {
                intent.setComponent(new ComponentName(FLIP_HOME_PACKAGE, FLIP_HOME_CLASS));
            }
            return intent;
        }
        return intent;
    }

    public ComponentName loadFlipComponent() {
        if (!MiuiMultiDisplayTypeInfo.isFlipDevice()) {
            return null;
        }
        return ComponentName.unflattenFromString(FLIP_HOME_SHORT_COMPONENT);
    }

    public boolean isCallerRecents(ActivityTaskManagerService service, int uid, ComponentName componentName) {
        return service.isCallerRecents(uid) || service.isFlipComponent(componentName);
    }
}
