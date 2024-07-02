package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import java.util.ArrayList;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.GeneralExceptionEvent;

/* loaded from: classes.dex */
public class ActivityTaskSupervisorImpl extends ActivityTaskSupervisorStub {
    private static final int ACTIVITY_RESUME_TIMEOUT = 5000;
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
    private static final String INCALL_PACKAGE_NAME = "com.android.incallui";
    private static final String INCALL_UI_NAME = "com.android.incallui.InCallActivity";
    private static final int MAX_SWITCH_INTERVAL = 1000;
    public static final String MIUI_APP_LOCK_ACTION = "miui.intent.action.CHECK_ACCESS_CONTROL";
    public static final String MIUI_APP_LOCK_ACTIVITY_NAME = "com.miui.applicationlock.ConfirmAccessControl";
    public static final String MIUI_APP_LOCK_PACKAGE_NAME = "com.miui.securitycenter";
    public static final int MIUI_APP_LOCK_REQUEST_CODE = -1001;
    private static final String TAG = "ActivityTaskSupervisor";
    private static long mLastIncallUiLaunchTime;
    private static int sActivityRequestId;
    static final ArrayList<String> sSupportsMultiTaskInDockList;
    private ActivityTaskManagerService mAtmService;
    private Context mContext;
    private volatile long mTimestamp = 0;
    private volatile boolean mFromKeyguard = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityTaskSupervisorImpl> {

        /* compiled from: ActivityTaskSupervisorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityTaskSupervisorImpl INSTANCE = new ActivityTaskSupervisorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActivityTaskSupervisorImpl m2432provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActivityTaskSupervisorImpl m2431provideNewInstance() {
            return new ActivityTaskSupervisorImpl();
        }
    }

    void init(ActivityTaskManagerService atms, Looper looper) {
        this.mAtmService = atms;
        this.mContext = atms.mContext;
    }

    void startActivityFromRecentsFlag(Intent intent) {
        ComponentName component;
        String targetPkg = intent.getPackage();
        if (targetPkg == null && (component = intent.getComponent()) != null) {
            targetPkg = component.getPackageName();
        }
        if (targetPkg == null) {
            PackageManager pm = this.mContext.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                targetPkg = resolveInfo.resolvePackageName;
            }
        }
        if (!TextUtils.isEmpty(targetPkg)) {
            PendingIntentRecordImpl.exemptTemporarily(targetPkg, true);
        }
    }

    int isAppLockActivity(ActivityRecord sourceRecord, Intent intent, ActivityInfo aInfo, int requestCode) {
        return (sourceRecord.getTask() != null && sourceRecord.inMultiWindowMode() && requestCode == -1 && intent != null && aInfo != null && MIUI_APP_LOCK_PACKAGE_NAME.equals(intent.getPackage()) && MIUI_APP_LOCK_ACTION.equals(intent.getAction()) && MIUI_APP_LOCK_ACTIVITY_NAME.equals(aInfo.name)) ? MIUI_APP_LOCK_REQUEST_CODE : requestCode;
    }

    void acquireLaunchWakelock() {
    }

    void startPausingResumedActivity() {
        this.mTimestamp = SystemClock.uptimeMillis();
    }

    void keyguardGoingAway(int flags) {
        this.mTimestamp = SystemClock.uptimeMillis();
        this.mFromKeyguard = true;
    }

    void activityIdle(final ActivityInfo info) {
        if (this.mTimestamp == 0) {
            return;
        }
        long timestamp = this.mTimestamp;
        final boolean fromKeyguard = this.mFromKeyguard;
        this.mTimestamp = 0L;
        this.mFromKeyguard = false;
        final long durationMillis = SystemClock.uptimeMillis() - timestamp;
        if (durationMillis > 5000) {
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskSupervisorImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskSupervisorImpl.this.lambda$activityIdle$0(info, durationMillis, fromKeyguard);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: trackActivityResumeTimeout, reason: merged with bridge method [inline-methods] */
    public void lambda$activityIdle$0(ActivityInfo info, long durationMillis, boolean fromKeyguard) {
        try {
            GeneralExceptionEvent event = new GeneralExceptionEvent();
            event.setType(433);
            event.setPackageName(info.packageName);
            event.setTimeStamp(System.currentTimeMillis());
            event.setSummary("activity resume timeout");
            event.setDetails("activity=" + info.name + " duration=" + durationMillis + "ms fromKeyguard=" + fromKeyguard);
            event.setPid((int) durationMillis);
            MQSEventManagerDelegate.getInstance().reportGeneralException(event);
            MiEvent miEvent = new MiEvent(901003004);
            miEvent.addStr("PackageName", info.packageName);
            miEvent.addStr("ActivityName", info.name);
            miEvent.addInt("Duration", (int) durationMillis);
            miEvent.addBool("FromKeyguard", fromKeyguard);
            MiSight.sendEvent(miEvent);
        } catch (Exception e) {
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sSupportsMultiTaskInDockList = arrayList;
        arrayList.add("com.miui.hybrid");
        mLastIncallUiLaunchTime = -1L;
    }

    static void updateInfoBeforeRealStartActivity(Task stack, IApplicationThread caller, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo, IBinder resultTo, int requestCode, int userId) {
        MiuiMultiTaskManagerStub.get().updateMultiTaskInfoIfNeed(stack, aInfo, intent);
    }

    static boolean isAllowedAppSwitch(Task stack, String callingPackageName, ActivityInfo aInfo, long lastTime) {
        return isAllowedAppSwitch(stack, callingPackageName, aInfo);
    }

    static boolean isAllowedAppSwitch(Task stack, String callingPackageName, ActivityInfo aInfo) {
        if (stack == null) {
            return false;
        }
        ActivityRecord topr = stack.topRunningNonDelayedActivityLocked((ActivityRecord) null);
        if (topr != null && topr.info != null && INCALL_UI_NAME.equals(topr.info.name) && !INCALL_PACKAGE_NAME.equals(callingPackageName) && aInfo != null && !INCALL_UI_NAME.equals(aInfo.name) && mLastIncallUiLaunchTime + 1000 > System.currentTimeMillis()) {
            Slog.w("ActivityManager", "app switch:" + aInfo.name + " stopped for " + INCALL_UI_NAME + "in 1000 ms.Try later.");
            return false;
        }
        if (aInfo != null && INCALL_UI_NAME.equals(aInfo.name)) {
            mLastIncallUiLaunchTime = System.currentTimeMillis();
            return true;
        }
        return true;
    }

    public static boolean supportsMultiTaskInDock(String packageName) {
        return sSupportsMultiTaskInDockList.contains(packageName);
    }

    private static ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId) {
        try {
            return AppGlobals.getPackageManager().resolveIntent(intent, resolvedType, 66560L, userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    private static int getNextRequestIdLocked() {
        if (sActivityRequestId >= Integer.MAX_VALUE) {
            sActivityRequestId = 0;
        }
        int i = sActivityRequestId + 1;
        sActivityRequestId = i;
        return i;
    }

    public static boolean notPauseAtFreeformMode(Task focusStack, Task curStack) {
        if (supportsFreeform()) {
            return (focusStack.getWindowingMode() == 1 && curStack.getWindowingMode() == 5) || focusStack.getWindowingMode() == 5;
        }
        return false;
    }

    public static boolean supportsFreeform() {
        return SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, "1".equals(SystemProperties.get("ro.miui.cts")) ^ true) && MiuiMultiWindowUtils.isForceResizeable();
    }

    public static Task exitfreeformIfNeeded(Task task, int taskId, int windowMode, ActivityTaskSupervisor supervisor) {
        if (task == null || task.getWindowingMode() != 5 || windowMode == 5) {
            return task;
        }
        ActivityOptions op = ActivityOptions.makeBasic();
        op.setLaunchWindowingMode(1);
        Task tTask = supervisor.mRootWindowContainer.anyTaskForId(taskId, 2, op, true);
        return tTask;
    }

    public static void updateApplicationConfiguration(ActivityTaskSupervisor stackSupervisor, Configuration globalConfiguration, String packageName) {
        Task topStack;
        ActivityRecord topActivity;
        synchronized (stackSupervisor.mService.mGlobalLock) {
            topStack = stackSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
        }
        if (topStack != null) {
            synchronized (stackSupervisor.mService.mGlobalLock) {
                topActivity = topStack.topRunningActivityLocked();
            }
            if (topActivity != null && topActivity.getWindowingMode() == 5 && packageName.equals(topActivity.packageName)) {
                Rect rect = topActivity.getConfiguration().windowConfiguration.getBounds();
                globalConfiguration.orientation = rect.height() > rect.width() ? 1 : 2;
                globalConfiguration.windowConfiguration.setWindowingMode(5);
                globalConfiguration.windowConfiguration.setBounds(topActivity.getConfiguration().windowConfiguration.getBounds());
            }
        }
    }

    void addSender(ActivityTaskManagerService service, Intent intent, int callingPid, int filterCallingUid) {
        synchronized (service.mGlobalLock) {
            WindowProcessController wpc = service.getProcessController(callingPid, filterCallingUid);
            if (wpc != null && wpc.mInfo != null) {
                intent.setSender(wpc.mInfo.packageName);
            } else {
                intent.setSender(null);
            }
        }
    }
}
