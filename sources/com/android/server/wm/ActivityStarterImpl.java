package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.android.internal.util.function.QuintPredicate;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.LocalServices;
import com.android.server.PowerConsumptionServiceInternal;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityStarter;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.SplashScreenServiceDelegate;
import com.miui.server.greeze.GreezeManagerInternal;
import com.miui.whetstone.server.IWhetstoneActivityManager;
import database.SlaDbSchema.SlaDbSchema;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import miui.app.ActivitySecurityHelper;
import miui.app.StorageRestrictedPathManager;
import miui.security.AppBehavior;
import miui.security.SecurityManagerInternal;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ActivityStarterImpl extends ActivityStarterStub {
    private static final String APPLICATION_LOCK_NAME = "com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl";
    private static final String CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS_ACTION = "not_support_in_mutil_display_activitys_action";
    private static final String EXTRA_ORIGINATING_UID = "originating_uid";
    public static final String MIBI_SDK_SIGN_DEDUCT_ACTIVITY = "com.mibi.sdk.deduct.ui.SignDeductActivity";
    public static final String PACKAGE_NAME_ALIPAY = "com.eg.android.AlipayGphone";
    private static final String SECURITY_CENTER = "com.miui.securitycenter";
    private static final String TAG = "ActivityStarterImpl";
    private ActivityTaskManagerService mAtmService;
    private Context mContext;
    private List<String> mDefaultHomePkgNames;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int mLastStartActivityUid;
    private PowerConsumptionServiceInternal mPowerConsumptionServiceInternal;
    private ActivitySecurityHelper mSecurityHelper;
    private SecurityManagerInternal mSecurityManagerInternal;
    private SmartPowerServiceInternal mSmartPowerService;
    private SplashScreenServiceDelegate mSplashScreenServiceDelegate;
    private boolean mSystemReady;
    private static final Set<String> CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS = new HashSet(Arrays.asList("com.autonavi.minimap", "com.baidu.BaiduMap", "com.sinyee.babybus.story"));
    private static final Set<String> CARLINK_VIRTUAL_DISPLAY_SET = new HashSet(Arrays.asList("com.miui.carlink", "com.xiaomi.ucar.minimap", "com.miui.car.launcher"));

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityStarterImpl> {

        /* compiled from: ActivityStarterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityStarterImpl INSTANCE = new ActivityStarterImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActivityStarterImpl m2423provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActivityStarterImpl m2422provideNewInstance() {
            return new ActivityStarterImpl();
        }
    }

    ActivityStarterImpl() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityTaskManagerService service, Context context) {
        this.mContext = context;
        this.mAtmService = service;
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        this.mSystemReady = true;
        this.mSplashScreenServiceDelegate = new SplashScreenServiceDelegate(this.mContext);
        this.mSecurityHelper = new ActivitySecurityHelper(this.mContext);
        this.mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        this.mPowerConsumptionServiceInternal = (PowerConsumptionServiceInternal) LocalServices.getService(PowerConsumptionServiceInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActivityStarterImpl getInstance() {
        return (ActivityStarterImpl) ActivityStarterStub.get();
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, ActivityInfo info, Intent intent, int userId, String callingPackage) {
        return ActivityManagerServiceImpl.getInstance().checkRunningCompatibility(caller, info, intent, userId, callingPackage);
    }

    private static void checkAndNotify(int uid) {
        try {
            GreezeManagerInternal gz = GreezeManagerInternal.getInstance();
            if (gz != null && gz.isUidFrozen(uid)) {
                IWhetstoneActivityManager ws = IWhetstoneActivityManager.Stub.asInterface(ServiceManager.getService("whetstone.activity"));
                Bundle b = new Bundle();
                b.putInt(SlaDbSchema.SlaTable.Uidlist.UID, uid);
                if (ws != null) {
                    ws.getPowerKeeperPolicy().notifyEvent(11, b);
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "checkAndNotify error uid = " + uid);
        }
    }

    public boolean carlinkJudge(Context context, Task targetRootTask, ActivityRecord record, RootWindowContainer rwc) {
        ActivityRecord activityRecord = findMapActivityInHistory(record, rwc);
        if (activityRecord == null || activityRecord.getRootTask() == null || targetRootTask == null) {
            return false;
        }
        DisplayContent preDisplayContent = activityRecord.getRootTask().mDisplayContent;
        int targetDisplayid = targetRootTask.getDisplayId();
        if (!isCarWithDisplay(preDisplayContent) || targetDisplayid != 0) {
            return false;
        }
        Slog.d(TAG, "activity is already started in carlink display");
        Intent srcIntent = record.intent;
        sendBroadCastToUcar(srcIntent, context);
        return true;
    }

    private ActivityRecord findMapActivityInHistory(ActivityRecord r, RootWindowContainer rwc) {
        if (r == null || r.intent == null || r.intent.getComponent() == null || r.info == null || !CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS.contains(r.intent.getComponent().getPackageName())) {
            return null;
        }
        ActivityRecord result = findActivityInSameApplication(r.intent, r.info, false, rwc);
        Slog.d(TAG, "findMapActivityInHistory result=" + result + " r=" + r.intent + "  info=" + r.info);
        return result;
    }

    private void sendBroadCastToUcar(Intent srcIntent, final Context context) {
        if (this.mHandlerThread == null) {
            HandlerThread handlerThread = new HandlerThread("carlink-workthread");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }
        if (context != null) {
            final Intent intent = new Intent();
            intent.setAction(CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS_ACTION);
            intent.setPackage("com.miui.carlink");
            Bundle bundle = new Bundle();
            bundle.putParcelable("src_intent", srcIntent);
            intent.putExtras(bundle);
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityStarterImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    context.sendBroadcast(intent, "miui.car.permission.MI_CARLINK_STATUS");
                }
            });
        }
    }

    public boolean isCarWithDisplay(DisplayContent dc) {
        if (dc != null && dc.getDisplay() != null) {
            String displayName = dc.getDisplay().getName();
            return CARLINK_VIRTUAL_DISPLAY_SET.contains(displayName);
        }
        return false;
    }

    private ActivityRecord findActivityInSameApplication(Intent intent, ActivityInfo info, boolean compareIntentFilters, RootWindowContainer rwc) {
        if (info.applicationInfo == null || rwc == null) {
            return null;
        }
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        PooledPredicate p = PooledLambda.obtainPredicate(new QuintPredicate() { // from class: com.android.server.wm.ActivityStarterImpl$$ExternalSyntheticLambda2
            public final boolean test(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                boolean matchesPackageName;
                matchesPackageName = ActivityStarterImpl.matchesPackageName((ActivityRecord) obj, ((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), (Intent) obj4, (ComponentName) obj5);
                return matchesPackageName;
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(userId), Boolean.valueOf(compareIntentFilters), intent, cls);
        if (p == null) {
            return null;
        }
        ActivityRecord r = rwc.getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean matchesPackageName(ActivityRecord r, int userId, boolean compareIntentFilters, Intent intent, ComponentName cls) {
        if (r == null || !r.canBeTopRunning() || r.mUserId != userId) {
            return false;
        }
        if (compareIntentFilters) {
            if (r.intent != null && r.intent.filterEquals(intent)) {
                return true;
            }
        } else {
            if (r.mActivityComponent == null || cls == null) {
                return false;
            }
            Slog.d(TAG, "matchesApplication r=" + r.mActivityComponent + " cls=" + cls);
            if (TextUtils.equals(r.mActivityComponent.getPackageName(), cls.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedStartActivity(int callingUid, int callingPid, String callingPackage) {
        ActivityRecord r;
        if (UserHandle.getAppId(callingUid) < 10000 || PendingIntentRecordImpl.containsPendingIntent(callingPackage) || callingUid == this.mLastStartActivityUid || this.mAtmService.hasUserVisibleWindow(callingUid)) {
            this.mLastStartActivityUid = callingUid;
            return true;
        }
        AppOpsManager appops = this.mAtmService.getAppOpsManager();
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        Task stack = display.getFocusedRootTask();
        if (stack == null || (r = stack.topRunningActivityLocked()) == null) {
            return true;
        }
        if (callingUid == r.info.applicationInfo.uid) {
            this.mLastStartActivityUid = callingUid;
            return true;
        }
        if (appops.checkOpNoThrow(10021, callingUid, callingPackage) == 0) {
            return true;
        }
        ArraySet<WindowProcessController> apps = this.mAtmService.mProcessMap.getProcesses(callingUid);
        if (apps != null && apps.size() != 0) {
            Iterator<WindowProcessController> it = apps.iterator();
            while (it.hasNext()) {
                WindowProcessController app = it.next();
                if (app != null && app.mUid == callingUid && app.hasForegroundActivities()) {
                    this.mLastStartActivityUid = callingUid;
                    return true;
                }
            }
        }
        Slog.d(TAG, "MIUILOG- Permission Denied Activity :  pkg : " + callingPackage + " uid : " + callingUid + " tuid : " + r.info.applicationInfo.uid);
        return false;
    }

    public boolean isAllowedStartActivity(Intent intent, int callingUid, int callingPid, String callingPackage, int realCallingUid, int realCallingPid, ActivityInfo aInfo) {
        int checkingUid;
        String callingPackage2;
        String str;
        checkAndNotify(aInfo.applicationInfo.uid);
        if (callingUid != realCallingUid && UserHandle.getAppId(realCallingUid) > 10000) {
            WindowProcessController realApp = this.mAtmService.getProcessController(realCallingPid, realCallingUid);
            if (realApp == null) {
                Slog.d(TAG, "MIUILOG- Permission Denied Activity : " + intent + " realPid : " + realCallingPid + " realUid : " + realCallingUid + " pid : " + callingPid + " uid : " + callingUid);
                return false;
            }
            String str2 = realApp.mInfo.packageName;
            checkingUid = realCallingUid;
            callingPackage2 = str2;
        } else {
            checkingUid = callingUid;
            callingPackage2 = callingPackage;
        }
        if (UserHandle.getAppId(checkingUid) < 10000 || PendingIntentRecordImpl.containsPendingIntent(callingPackage2) || PendingIntentRecordImpl.containsPendingIntent(aInfo.applicationInfo.packageName) || checkingUid == this.mLastStartActivityUid || this.mAtmService.hasUserVisibleWindow(checkingUid) || ("android.service.dreams.DreamActivity".equals(aInfo.name) && AppOpsUtils.isXOptMode())) {
            this.mLastStartActivityUid = aInfo.applicationInfo.uid;
            return true;
        }
        AppOpsManager appops = this.mAtmService.getAppOpsManager();
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        Task stack = display.getFocusedRootTask();
        if (stack == null) {
            return true;
        }
        if (this.mAtmService.mWindowManager.isKeyguardLocked()) {
            recordAppBehavior(36, callingPackage2, intent);
            str = " pkg : ";
            if (appops.noteOpNoThrow(10020, checkingUid, callingPackage2, (String) null, "ActivityTaskManagerServiceInjector#isAllowedStartActivity") != 0) {
                Slog.d(TAG, "MIUILOG- Permission Denied Activity KeyguardLocked: " + intent + str + callingPackage2 + " uid : " + checkingUid);
                return false;
            }
        } else {
            str = " pkg : ";
        }
        ActivityRecord r = stack.topRunningActivityLocked();
        if (r == null) {
            return true;
        }
        if (checkingUid == r.info.applicationInfo.uid) {
            this.mLastStartActivityUid = aInfo.applicationInfo.uid;
            return true;
        }
        recordAppBehavior(27, callingPackage2, intent);
        if (appops.checkOpNoThrow(10021, checkingUid, callingPackage2) == 0) {
            return true;
        }
        ArraySet<WindowProcessController> apps = this.mAtmService.mProcessMap.getProcesses(checkingUid);
        if (apps != null && apps.size() != 0) {
            Iterator<WindowProcessController> it = apps.iterator();
            while (it.hasNext()) {
                WindowProcessController app = it.next();
                if (app != null && app.mUid == checkingUid && app.hasForegroundActivities()) {
                    this.mLastStartActivityUid = aInfo.applicationInfo.uid;
                    return true;
                }
            }
        }
        appops.noteOpNoThrow(10021, checkingUid, callingPackage2, (String) null, "ActivityTaskManagerServiceInjector#isAllowedStartActivity");
        Slog.d(TAG, "MIUILOG- Permission Denied Activity : " + intent + str + callingPackage2 + " uid : " + checkingUid + " tuid : " + r.info.applicationInfo.uid);
        return false;
    }

    public IBinder finishActivity(IBinder token, int resultCode, Intent resultData) {
        return token;
    }

    public void triggerLaunchMode(String processName, int uid) {
    }

    public void finishLaunchMode(String processName, int uid) {
        GreezeManagerInternal.getInstance().finishLaunchMode(processName, uid);
    }

    void activityIdle(ActivityInfo aInfo) {
        if (this.mSystemReady) {
            if (aInfo == null) {
                Slog.w(TAG, "aInfo is null!");
            } else {
                this.mSplashScreenServiceDelegate.activityIdle(aInfo);
            }
        }
    }

    void destroyActivity(ActivityInfo aInfo) {
        if (this.mSystemReady) {
            if (aInfo == null) {
                Slog.w(TAG, "aInfo is null!");
            } else {
                this.mSplashScreenServiceDelegate.destroyActivity(aInfo);
            }
        }
    }

    Intent requestSplashScreen(Intent intent, ActivityInfo aInfo, SafeActivityOptions options, IApplicationThread caller) {
        ActivityOptions activityOptions;
        ActivityOptions checkedOptions;
        if (!this.mSystemReady) {
            return intent;
        }
        if (intent == null || aInfo == null) {
            Slog.w(TAG, "Intent or aInfo is null!");
            return intent;
        }
        synchronized (this.mAtmService.mGlobalLock) {
            WindowProcessController callerApp = this.mAtmService.getProcessController(caller);
            if (options != null) {
                activityOptions = options.getOptions(intent, aInfo, callerApp, this.mAtmService.mTaskSupervisor);
            } else {
                activityOptions = null;
            }
            checkedOptions = activityOptions;
        }
        if (checkedOptions != null && (checkedOptions.getLaunchWindowingMode() == 6 || checkedOptions.getLaunchWindowingMode() == 5)) {
            Slog.w(TAG, "The Activity is in freeForm|split windowing mode !");
            return intent;
        }
        return this.mSplashScreenServiceDelegate.requestSplashScreen(intent, aInfo);
    }

    ActivityInfo resolveSplashIntent(ActivityInfo aInfo, Intent intent, ProfilerInfo profilerInfo, int userId, int filterCallingUid, int callingPid) {
        if (intent == null) {
            return aInfo;
        }
        ComponentName component = intent.getComponent();
        if (component == null) {
            return aInfo;
        }
        if (SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE.equals(component.getPackageName()) && SplashScreenServiceDelegate.SPLASHSCREEN_ACTIVITY.equals(component.getClassName())) {
            return this.mAtmService.mTaskSupervisor.resolveActivity(intent, (String) null, 0, profilerInfo, userId, filterCallingUid, callingPid);
        }
        return aInfo;
    }

    public void updateLastStartActivityUid(String foregroundPackageName, int lastUid) {
        if (foregroundPackageName == null) {
            return;
        }
        if (this.mDefaultHomePkgNames == null) {
            ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                pm.getHomeActivities(homeActivities);
                if (homeActivities.size() > 0) {
                    Iterator<ResolveInfo> it = homeActivities.iterator();
                    while (it.hasNext()) {
                        ResolveInfo info = it.next();
                        if (this.mDefaultHomePkgNames == null) {
                            this.mDefaultHomePkgNames = new ArrayList();
                        }
                        this.mDefaultHomePkgNames.add(info.activityInfo.packageName);
                    }
                }
            } catch (Exception e) {
            }
        }
        List<String> list = this.mDefaultHomePkgNames;
        if (list != null && list.contains(foregroundPackageName)) {
            this.mLastStartActivityUid = lastUid;
        }
    }

    SafeActivityOptions checkStartActivityByFreeForm(IApplicationThread caller, ActivityInfo aInfo, Intent intent, String resolvedType, boolean ignoreTargetSecurity, int callingUid, String callingPackage, SafeActivityOptions bOptions) {
        ActivityOptions options;
        ActivityRecord behindActivity;
        if (aInfo != null) {
            String rawPackageName = getRawPackageName(aInfo, intent);
            String currentFullPackageName = null;
            ActivityRecord currentFullActivity = null;
            synchronized (this.mAtmService.mGlobalLock) {
                DisplayContent display = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
                if (display != null && display.topRunningActivityExcludeFreeform() != null && display.topRunningActivityExcludeFreeform().getTask() != null) {
                    currentFullPackageName = display.topRunningActivityExcludeFreeform().getTask().getPackageName();
                    currentFullActivity = display.topRunningActivityExcludeFreeform();
                }
            }
            if (startActivityByFreeForm(aInfo, intent) && rawPackageName != null && !rawPackageName.equals(currentFullPackageName)) {
                if ((currentFullActivity != null && currentFullActivity.mActivityComponent != null && MIBI_SDK_SIGN_DEDUCT_ACTIVITY.equals(currentFullActivity.mActivityComponent.getClassName()) && PACKAGE_NAME_ALIPAY.equals(rawPackageName)) || (options = MiuiMultiWindowUtils.getActivityOptions(this.mContext, rawPackageName, true)) == null) {
                    return bOptions;
                }
                if (this.mSecurityManagerInternal.isForceLaunchNewTask(rawPackageName, intent.getComponent().flattenToShortString()) || this.mSecurityManagerInternal.isApplicationLockActivity(intent.getComponent().flattenToShortString())) {
                    options.setForceLaunchNewTask();
                }
                if (currentFullActivity != null) {
                    if ("com.miui.securitycore/com.miui.xspace.ui.activity.XSpaceResolveActivity".equals(currentFullActivity.shortComponentName)) {
                        synchronized (this.mAtmService.mGlobalLock) {
                            behindActivity = this.mAtmService.mTaskSupervisor.mRootWindowContainer.getDisplayContent(0).getActivityBelow(currentFullActivity);
                        }
                        if (behindActivity != null) {
                            options.setLaunchFromTaskId(behindActivity.getRootTaskId());
                        }
                    } else {
                        options.setLaunchFromTaskId(currentFullActivity.getRootTaskId());
                    }
                }
                ActivityOptions options2 = bOptions != null ? bOptions.mergeActivityOptions(bOptions.getOriginalOptions(), options) : options;
                return SafeActivityOptions.fromBundle(options2 != null ? options2.toBundle() : null);
            }
        }
        return bOptions;
    }

    private String getRawPackageName(ActivityInfo aInfo, Intent intent) {
        Intent rawIntent;
        String rawPackageName = aInfo.packageName;
        if (intent.getComponent() != null && TextUtils.equals("com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl", intent.getComponent().flattenToShortString()) && (rawIntent = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT")) != null && rawIntent.getComponent() != null) {
            return rawIntent.getComponent().getPackageName();
        }
        return rawPackageName;
    }

    private boolean startActivityByFreeForm(ActivityInfo aInfo, Intent intent) {
        if (this.mSecurityManagerInternal == null) {
            this.mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        return this.mSecurityManagerInternal.checkGameBoosterPayPassAsUser(aInfo.packageName, intent, UserHandle.getUserId(aInfo.applicationInfo.uid));
    }

    boolean checkStartActivityPermission(ActivityStarter.Request request) {
        if (request.activityInfo != null) {
            if (!ActivityManagerServiceImpl.getInstance().checkRunningCompatibility(request.caller, request.activityInfo, request.intent, request.userId, request.callingPackage)) {
                return false;
            }
            int callingUid = request.realCallingUid == -1 ? Binder.getCallingUid() : request.realCallingUid;
            if (checkStorageRestricted(request.activityInfo, request.intent, callingUid, request.callingPackage, request.reason, request.resultTo) != null) {
                return true;
            }
            boolean calleeAlreadyStarted = WindowProcessUtils.isPackageRunning(this.mAtmService, request.activityInfo.packageName, request.activityInfo.processName, request.activityInfo.applicationInfo.uid);
            Bundle bOptions = null;
            if (request.activityOptions != null && request.activityOptions.getOriginalOptions() != null) {
                bOptions = request.activityOptions.getOriginalOptions().toBundle();
            }
            Intent checkIntent = this.mSecurityHelper.getCheckIntent(request.callingPackage, request.activityInfo.packageName, request.intent, request.resultTo != null, request.requestCode, calleeAlreadyStarted, UserHandle.getUserId(request.activityInfo.applicationInfo.uid), callingUid, bOptions);
            if (checkIntent != null) {
                request.intent = checkIntent;
            }
        }
        resolveCheckIntent(request);
        return true;
    }

    private Intent checkStorageRestricted(ActivityInfo aInfo, Intent intent, int callingUid, String callingPackage, String reason, IBinder resultTo) {
        if (TextUtils.equals("startActivityAsCaller", reason)) {
            Slog.i(TAG, "As caller, check special action!");
            if (aInfo != null && resultTo != null) {
                ActivityRecord record = ActivityRecord.forTokenLocked(resultTo);
                boolean fromWakePath = record != null && "com.miui.securitycenter".equals(record.packageName);
                if (!fromWakePath && (("android.intent.action.PICK".equals(intent.getAction()) && !TextUtils.equals(callingPackage, aInfo.packageName) && StorageRestrictedPathManager.isDenyAccessGallery(this.mContext, callingPackage, callingUid, intent)) || ("android.intent.action.SEND".equals(intent.getAction()) && !TextUtils.equals(callingPackage, aInfo.packageName) && StorageRestrictedPathManager.isDenyAccessGallery(this.mContext, aInfo.packageName, aInfo.applicationInfo.uid, intent)))) {
                    Slog.i(TAG, "startAsCaller to pick pictures, not skip!");
                    return null;
                }
                return intent;
            }
            return intent;
        }
        return null;
    }

    private ActivityInfo resolveCheckIntent(ActivityStarter.Request request) {
        int userId;
        boolean transform;
        Intent intent = request.intent;
        int userId2 = request.userId;
        if (intent != null && intent.getComponent() == null) {
            if (!ActivityTaskSupervisorImpl.MIUI_APP_LOCK_ACTION.equals(intent.getAction()) && !"android.app.action.CHECK_ACCESS_CONTROL_PAD".equals(intent.getAction()) && !"android.app.action.CHECK_ALLOW_START_ACTIVITY".equals(intent.getAction()) && !"android.app.action.CHECK_ALLOW_START_ACTIVITY_PAD".equals(intent.getAction()) && !"com.miui.gamebooster.action.ACCESS_WINDOWCALLACTIVITY".equals(intent.getAction()) && !this.mSecurityManagerInternal.isBlockActivity(intent)) {
                userId = userId2;
                transform = false;
            } else {
                if (userId2 == 999) {
                    userId2 = 0;
                }
                userId = userId2;
                transform = true;
            }
            if (transform) {
                request.activityInfo = this.mAtmService.mTaskSupervisor.resolveActivity(intent, (String) null, 0, request.profilerInfo, userId, request.filterCallingUid, request.callingPid);
            }
        }
        return request.activityInfo;
    }

    public void startActivityUncheckedBefore(ActivityRecord r) {
        PowerConsumptionServiceInternal powerConsumptionServiceInternal;
        if (r != null && (powerConsumptionServiceInternal = this.mPowerConsumptionServiceInternal) != null) {
            powerConsumptionServiceInternal.noteStartActivityForPowerConsumption(r.mActivityComponent.getClassName());
        }
        this.mSmartPowerService.onActivityStartUnchecked(r.info.name, r.getUid(), r.getPid(), r.packageName, r.launchedFromUid, r.launchedFromPid, r.launchedFromPackage, this.mAtmService.getProcessController(r.getProcessName(), r.getUid()) == null);
    }

    public boolean isCarWithDisplay(RootWindowContainer rootWindowContainer, ActivityOptions options) {
        if (rootWindowContainer == null) {
            return false;
        }
        int displayId = 0;
        if (options != null) {
            displayId = options.getLaunchDisplayId();
        } else {
            Task focusedRootTask = rootWindowContainer.getTopDisplayFocusedRootTask();
            if (focusedRootTask != null) {
                displayId = focusedRootTask.getDisplayId();
            }
        }
        DisplayContent displayContent = rootWindowContainer.getDisplayContent(displayId);
        return isCarWithDisplay(displayContent);
    }

    public void logStartActivityError(int err, Intent intent) {
        try {
            switch (err) {
                case -97:
                    Slog.e(TAG, "Error: Activity not started, voice control not allowed for: " + intent);
                    break;
                case -96:
                case -95:
                case -94:
                default:
                    Slog.e(TAG, "Error: Activity not started, unknown error code " + err);
                    break;
                case -93:
                    Slog.e(TAG, "Error: Activity not started, you requested to both forward and receive its result");
                    break;
                case -92:
                    Slog.e(TAG, "Error: Activity class " + intent.getComponent().toShortString() + " does not exist.");
                    break;
                case -91:
                    Slog.e(TAG, "Error: Activity not started, unable to resolve " + intent.toString());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean finishLaunchedFromActivityIfNeeded(final ActivityRecord starting) {
        ActivityRecord fromActivity;
        if (starting == null) {
            return false;
        }
        RootWindowContainer rwc = starting.mRootWindowContainer;
        if (starting.isActivityTypeHome()) {
            for (int displayNdx = rwc.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
                DisplayContent display = rwc.getChildAt(displayNdx);
                if (!display.isDefaultDisplay && (fromActivity = display.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityStarterImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return ActivityStarterImpl.lambda$finishLaunchedFromActivityIfNeeded$1(starting, (ActivityRecord) obj);
                    }
                })) != null) {
                    Slog.e(TAG, "Finish launch from activity " + fromActivity + " starting=" + starting + " launchedFromPid=" + starting.launchedFromPid + " launchedFromPackage=" + starting.launchedFromPackage);
                    fromActivity.finishIfPossible("finish-launch-from", true);
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$finishLaunchedFromActivityIfNeeded$1(ActivityRecord starting, ActivityRecord r) {
        return !r.finishing && r.getPid() == starting.launchedFromPid;
    }

    public String getRebootReason(Intent intent) {
        boolean userRequested = intent.getBooleanExtra("android.intent.extra.USER_REQUESTED_SHUTDOWN", false);
        if (userRequested) {
            return "userrequested";
        }
        String reason = intent.getStringExtra("android.intent.extra.REASON");
        return reason;
    }

    public boolean shouldRebootReasonCheckNull() {
        return SystemProperties.getBoolean("persist.sys.stability.rebootreason_check", true);
    }

    public int getTransitionType(ActivityRecord r, ActivityRecord sourceRecord) {
        if (r == null) {
            return 1;
        }
        if (r.mActivityRecordStub.isSplitMode() || (sourceRecord != null && sourceRecord.mActivityRecordStub.isSplitMode())) {
            if (this.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || (sourceRecord != null && sourceRecord.inFreeformWindowingMode())) {
                return 1;
            }
            return r.mActivityRecordStub.isSecondStateActivity() ? 2147483646 : 2147483644;
        }
        if (r.mActivityRecordStub.isKeyguardEditor()) {
            return 2147483627;
        }
        if (!MiuiEmbeddingWindowServiceStub.get().isForcePortraitActivity(r) && !MiuiEmbeddingWindowServiceStub.get().isForcePortraitActivity(sourceRecord)) {
            return 1;
        }
        if (r.mActivityRecordStub != null) {
            r.mActivityRecordStub.setForcePortraitActivity(true);
        }
        if (sourceRecord != null && sourceRecord.mActivityRecordStub != null) {
            sourceRecord.mActivityRecordStub.setForcePortraitActivity(true);
            return 2147483632;
        }
        return 2147483632;
    }

    public boolean moveToFrontForSplitScreen(boolean differentTopTask, boolean avoidMoveToFront, Task targetRootTask, ActivityRecord intentActivity, ActivityRecord sourceRecord) {
        return !differentTopTask && !avoidMoveToFront && targetRootTask.mCreatedByOrganizer && targetRootTask.getWindowingMode() == 6 && intentActivity.isState(ActivityRecord.State.RESUMED) && sourceRecord == null && !intentActivity.isDescendantOf(targetRootTask) && intentActivity.getWindowingMode() != 2;
    }

    public boolean skipForSplitScreen(Task targetTask, Task targetRootTask, Task topRootTask, ActivityRecord top, int launchFlags) {
        boolean startForSplit = (targetTask != null || targetRootTask == null || !targetRootTask.mCreatedByOrganizer || targetRootTask.getWindowingMode() != 6 || topRootTask == targetRootTask || top.isDescendantOf(targetRootTask) || (134217728 & launchFlags) == 0) ? false : true;
        if (startForSplit) {
            Slog.i(TAG, "skip for multiple task, top : " + top);
            return true;
        }
        boolean startForAE = MiuiEmbeddingWindowServiceStub.get().isEmbeddingEnabledForPackage(top.packageName) && ((536870912 & launchFlags) != 0 || 1 == top.launchMode);
        if (!startForAE) {
            return false;
        }
        Slog.i(TAG, "skip for single top activity, top : " + top);
        return true;
    }

    private void recordAppBehavior(int behavior, String caller, Intent data) {
        if (this.mSecurityManagerInternal == null) {
            this.mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        SecurityManagerInternal securityManagerInternal = this.mSecurityManagerInternal;
        if (securityManagerInternal != null) {
            securityManagerInternal.recordAppBehaviorAsync(behavior, caller, 1L, AppBehavior.parseIntent(data));
        }
    }

    public void recordNewIntent(Intent intent, String callingPackage) {
        List<String> mMapList = Arrays.asList("com.autonavi.minimap", "com.baidu.BaiduMap", "com.tencent.map");
        String pkg = "";
        if (intent.getComponent() != null) {
            pkg = intent.getComponent().getPackageName();
        }
        if (!mMapList.contains(pkg) || TextUtils.equals(callingPackage, pkg) || TextUtils.equals(callingPackage, "com.miui.carlink")) {
            return;
        }
        String dat = intent.getDataString();
        if (TextUtils.isEmpty(dat)) {
            return;
        }
        try {
            dat = URLDecoder.decode(dat, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Slog.d(TAG, "Ready To Send BroadCast To carlink");
        ActivityCarWithStarterImpl activityCarWithStarter = new ActivityCarWithStarterImpl();
        activityCarWithStarter.recordCarWithIntent(this.mContext, callingPackage, pkg, dat);
    }

    public boolean isAppLockActivity(ActivityRecord activity) {
        return (activity == null || activity.mActivityComponent == null || !"com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl".equals(activity.mActivityComponent.flattenToString())) ? false : true;
    }

    public boolean checkIntentActivityForAppLock(ActivityRecord intentActivity, ActivityRecord startActivity) {
        if (!isAppLockActivity(startActivity) || intentActivity == null || !"com.miui.securitycenter".equals(intentActivity.packageName)) {
            return false;
        }
        String str = null;
        String pkgBehindIntentActivity = (intentActivity == null || intentActivity.intent == null) ? null : intentActivity.intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (startActivity != null && startActivity.intent != null) {
            str = startActivity.intent.getStringExtra("android.intent.extra.shortcut.NAME");
        }
        String pkgBehindStartActivity = str;
        int intentActivityUid = (intentActivity == null || intentActivity.intent == null) ? 0 : intentActivity.intent.getIntExtra(EXTRA_ORIGINATING_UID, 0);
        int startActivityUid = (startActivity == null || startActivity.intent == null) ? 0 : startActivity.intent.getIntExtra(EXTRA_ORIGINATING_UID, 0);
        return ((pkgBehindIntentActivity == null || pkgBehindIntentActivity.equals(pkgBehindStartActivity)) && intentActivityUid == startActivityUid) ? false : true;
    }

    public boolean checkDefaultBrowser(Context context, Task task, Intent intent) {
        String rawPackageName;
        Uri uri;
        if (context == null || task == null || intent == null) {
            return false;
        }
        Parcelable targetParcelable = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (!(targetParcelable instanceof Intent)) {
            return false;
        }
        Intent rawIntent = (Intent) targetParcelable;
        if (rawIntent != null && rawIntent.getComponent() != null && TextUtils.equals("com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl", intent.getComponent().flattenToShortString())) {
            rawPackageName = rawIntent.getComponent().getPackageName();
        } else {
            rawPackageName = task.getPackageName();
            rawIntent = intent;
        }
        String defaultBrowserPackageName = context.getPackageManager().getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        boolean checkVGScene = this.mAtmService.mMiuiFreeFormManagerService.isInVideoOrGameScene();
        boolean isDefaultBrowser = TextUtils.equals(rawPackageName, defaultBrowserPackageName);
        if (!isDefaultBrowser || (uri = rawIntent.getData()) == null || uri.getHost() == null || uri.getScheme() == null || rawIntent.getAction() == null || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) {
            return false;
        }
        return checkVGScene & isDefaultBrowser;
    }
}
