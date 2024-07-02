package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.miui.server.AccessController;
import java.lang.reflect.Method;
import java.util.List;

/* loaded from: classes.dex */
class ActivityStarterInjector {
    public static final int FLAG_ASSOCIATED_SETTINGS_AV = 134217728;
    private static final String TAG = "ActivityStarter";

    ActivityStarterInjector() {
    }

    public static void checkFreeformSupport(ActivityTaskManagerService service, ActivityOptions options) {
        if (!service.mSupportsFreeformWindowManagement) {
            if ((options != null && options.getLaunchWindowingMode() == 5) || MiuiDesktopModeUtils.isActive(service.mContext)) {
                service.mMiuiFreeFormManagerService.showOpenMiuiOptimizationToast();
            }
        }
    }

    private static ActivityOptions modifyLaunchActivityOptionIfNeed(RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, int userId, Intent intent, boolean pcRun, ActivityRecord sourceRecord) {
        String startPackageName;
        ActivityOptions options2;
        Task task;
        String startPackageName2 = null;
        MiuiFreeFormActivityStack sourceFfas = null;
        ActivityRecord sourceFreeFormActivity = sourceRecord;
        if (intent != null) {
            if (intent.getStringExtra("android.intent.extra.shortcut.NAME") != null) {
                startPackageName2 = intent.getStringExtra("android.intent.extra.shortcut.NAME");
            } else if (intent.getComponent() != null) {
                startPackageName2 = intent.getComponent().getPackageName();
            }
            if (startPackageName2 != null) {
                startPackageName = startPackageName2;
            } else {
                String startPackageName3 = intent.getPackage();
                startPackageName = startPackageName3;
            }
        } else {
            startPackageName = null;
        }
        if (sourceFreeFormActivity == null && callerApp != null) {
            List<ActivityRecord> callerActivities = callerApp.getActivities();
            List<ActivityRecord> inactiveActivities = callerApp.getInactiveActivities();
            sourceFreeFormActivity = getLastFreeFormActivityRecord(callerActivities) != null ? getLastFreeFormActivityRecord(callerActivities) : getLastFreeFormActivityRecord(inactiveActivities);
        }
        if (sourceFreeFormActivity != null) {
            sourceFfas = (MiuiFreeFormActivityStack) sourceFreeFormActivity.mWmService.mAtmService.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(sourceFreeFormActivity.getRootTaskId());
        }
        if (intent != null && intent.getComponent() != null) {
            if (MiuiMultiWindowAdapter.getCanStartActivityFromFullscreenToFreefromList().contains(intent.getComponent().getClassName())) {
                sourceFfas = (MiuiFreeFormActivityStack) root.mWmService.mAtmService.mMiuiFreeFormManagerService.getSchemeLauncherTask();
            }
        }
        if (sourceFfas != null && startPackageName != null && ((startPackageName.equals(sourceFfas.getStackPackageName()) || (sourceFreeFormActivity != null && sourceFreeFormActivity.mActivityComponent != null && "com.miui.securitycore/com.miui.xspace.ui.activity.XSpaceResolveActivity".equals(sourceFreeFormActivity.mActivityComponent.flattenToString()))) && sourceFreeFormActivity != null && sourceFreeFormActivity.mActivityComponent != null && !AccessController.APP_LOCK_CLASSNAME.equals(sourceFreeFormActivity.mActivityComponent.flattenToString()))) {
            options2 = MiuiMultiWindowUtils.getActivityOptions(sourceFreeFormActivity.mWmService.mContext, startPackageName, true);
            Task sourceTask = sourceFfas.mTask;
            if (sourceTask != null && MiuiDesktopModeUtils.isDesktopActive() && sourceTask.mTaskSupervisor.getLaunchParamsController().hasFreeformDesktopMemory(sourceTask)) {
                Rect launchBounds = sourceTask.mTaskSupervisor.getLaunchParamsController().getFreeformLastPosition(sourceTask);
                options2.setLaunchBounds(launchBounds);
                float scale = sourceTask.mTaskSupervisor.getLaunchParamsController().getFreeformLastScale(sourceTask);
                Slog.d(TAG, "ActivityStarterInjector::modifyLaunchActivityOptionIfNeed:: modify bounds and scale in dkt  launchBounds: " + launchBounds + " scale: " + scale);
                try {
                    Method method = MiuiMultiWindowUtils.isMethodExist(options2, "getActivityOptionsInjector", new Object[0]);
                    if (method != null && scale != -1.0f) {
                        MiuiMultiWindowUtils.invoke(method.invoke(options2, new Object[0]), "setFreeformScale", new Object[]{Float.valueOf(scale)});
                    }
                } catch (Exception e) {
                }
            }
            if (sourceFfas.getFreeFormLaunchFromTaskId() != 0) {
                options2.setLaunchFromTaskId(sourceFfas.getFreeFormLaunchFromTaskId());
            }
            Slog.d(TAG, "ActivityStarterInjector::modifyLaunchActivityOptionIfNeed:: options = " + options2 + " sourceFfas: " + sourceFfas + " sourceFreeFormActivity: " + sourceFreeFormActivity);
        } else {
            options2 = options;
        }
        if (sourceFfas != null && startPackageName != null && (task = sourceFfas.mTask) != null && task.realActivity != null && MiuiMultiWindowAdapter.NOT_AVOID_LAUNCH_OTHER_FREEFORM_LIST.contains(task.realActivity.getClassName())) {
            Rect currentRect = task != null ? task.getBounds() : sourceFreeFormActivity.getBounds();
            ActivityOptions targetOptions = MiuiMultiWindowUtils.getActivityOptions(sourceFreeFormActivity.mWmService.mContext, startPackageName, true, currentRect != null ? currentRect.left : -1, currentRect != null ? currentRect.top : -1);
            if (targetOptions != null) {
                if (options2 != null && options2.getForceLaunchNewTask()) {
                    targetOptions.setForceLaunchNewTask();
                }
                if (sourceFfas.getFreeFormLaunchFromTaskId() != 0) {
                    targetOptions.setLaunchFromTaskId(sourceFfas.getFreeFormLaunchFromTaskId());
                }
                options2 = options2 != null ? targetOptions : null;
                Slog.d(TAG, "ActivityStarterInjector::modifyLaunchActivityOptionIfNeed::due to application confirm lock");
            }
        }
        if (MiuiDesktopModeUtils.isDesktopActive()) {
            if (options2 == null) {
                options2 = ActivityOptions.makeBasic();
            }
            if (intent != null && intent.getComponent() != null && "com.android.camera/.OneShotCamera".equals(intent.getComponent().flattenToShortString())) {
                options2.setForceLaunchNewTask();
            }
        }
        return options2;
    }

    public static ActivityOptions modifyLaunchActivityOptionIfNeed(ActivityTaskManagerService service, RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, Intent intent, int userId, ActivityInfo aInfo, ActivityRecord sourceRecord) {
        ActivityRecord ar;
        Task topTask;
        if (isStartedInMiuiSetttingVirtualDispaly(root, intent, aInfo) && options != null && options.getLaunchDisplayId() == -1) {
            options.setLaunchDisplayId(0);
        }
        if (0 == 0 && intent != null && intent.getComponent() != null && MiuiMultiWindowAdapter.START_FROM_FREEFORM_BLACK_LIST_ACTIVITY.contains(intent.getComponent().flattenToShortString())) {
            return options;
        }
        if (root.getDefaultTaskDisplayArea() != null && (topTask = root.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1)) != null) {
            ActivityRecord ar2 = topTask.getTopActivity(false, true);
            ar = ar2;
        } else {
            ar = null;
        }
        if (options != null && options.getLaunchWindowingMode() == 5 && ar != null && ar.mActivityComponent != null && MiuiMultiWindowAdapter.LIST_ABOUT_LOCK_MODE_ACTIVITY.contains(ar.mActivityComponent.flattenToString())) {
            options.setLaunchWindowingMode(0);
            return options;
        }
        if (intent != null && intent.getComponent() != null && MiuiFreeformUtilStub.getInstance().isFullScreenStrategyNeededInDesktopMode(intent.getComponent().flattenToShortString())) {
            Slog.d(TAG, "ActivityStarterInjector::modifyLaunchActivityOptionIfNeed skip due to desktop Full-screen strategy");
            return options;
        }
        return modifyLaunchActivityOptionIfNeed(root, callingPackgae, options, callerApp, userId, intent, false, sourceRecord);
    }

    public static boolean getLastFrame(String name) {
        if (name.contains("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity") || name.contains("com.tencent.mm/.plugin.voip.ui.VideoActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity") || name.contains("com.android.incallui/.InCallActivity") || name.contains("com.google.android.dialer/com.android.incallui.InCallActivity") || name.contains("voipcalling.VoipActivityV2")) {
            return true;
        }
        return false;
    }

    public static void startActivityUncheckedBefore(ActivityRecord r, boolean isFromHome) {
    }

    private static boolean isStartedInMiuiSetttingVirtualDispaly(RootWindowContainer root, Intent intent, ActivityInfo aInfo) {
        ActivityRecord result;
        if (aInfo == null || (result = root.findActivity(intent, aInfo, false)) == null || result.intent == null || (result.intent.getMiuiFlags() & FLAG_ASSOCIATED_SETTINGS_AV) == 0) {
            return false;
        }
        return true;
    }

    private static ActivityRecord getLastFreeFormActivityRecord(List<ActivityRecord> activities) {
        if (activities != null && !activities.isEmpty() && activities.get(activities.size() - 1) != null && activities.get(activities.size() - 1).getWindowingMode() == 5) {
            return activities.get(activities.size() - 1);
        }
        return null;
    }
}
