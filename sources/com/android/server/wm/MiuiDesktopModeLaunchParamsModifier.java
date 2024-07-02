package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.android.server.wm.ActivityStarter;
import com.android.server.wm.LaunchParamsController;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class MiuiDesktopModeLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {
    private static final boolean DEBUG = true;
    private static final String TAG = "MiuiDesktopModeLaunchParamsModifier";
    private StringBuilder mLogBuilder;

    public int onCalculate(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, ActivityStarter.Request request, int phase, LaunchParamsController.LaunchParams currentParams, LaunchParamsController.LaunchParams outParams) {
        initLogBuilder(task, activity);
        int result = calculate(task, layout, activity, source, options, request, phase, currentParams, outParams);
        outputLog();
        return result;
    }

    private int calculate(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, ActivityStarter.Request request, int phase, LaunchParamsController.LaunchParams currentParams, LaunchParamsController.LaunchParams outParams) {
        String packageName;
        if (task == null) {
            appendLog("task null, skipping", new Object[0]);
            return 0;
        }
        Context context = task.mAtmService.mContext;
        boolean isDesktopModeActive = MiuiDesktopModeUtils.isActive(context);
        if (!isDesktopModeActive) {
            return 0;
        }
        if (phase != 3) {
            appendLog("not in bounds phase, skipping", new Object[0]);
            return 0;
        }
        if (!task.isActivityTypeStandard()) {
            appendLog("not standard activity type, skipping", new Object[0]);
            return 0;
        }
        if (!currentParams.mBounds.isEmpty()) {
            appendLog("currentParams has bounds set, not overriding", new Object[0]);
            return 0;
        }
        outParams.set(currentParams);
        if (isDesktopModeActive && !task.isVisible() && (task.inFreeformWindowingMode() || (options != null && options.getLaunchWindowingMode() == 5))) {
            if (task.origActivity != null) {
                packageName = task.origActivity.getPackageName();
            } else if (task.realActivity != null) {
                packageName = task.realActivity.getPackageName();
            } else {
                packageName = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : "unknown";
            }
            ActivityOptions freeformLaunchOption = MiuiMultiWindowUtils.getActivityOptions(context, packageName, true, false, isDesktopModeActive);
            if (freeformLaunchOption != null) {
                outParams.mBounds.set(freeformLaunchOption.getLaunchBounds());
                outParams.mWindowingMode = 5;
                if (options != null) {
                    try {
                        Method method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", (Object[]) null);
                        if (method != null) {
                            MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "setFreeformScale", new Object[]{Float.valueOf(((Float) MiuiMultiWindowUtils.invoke(method.invoke(freeformLaunchOption, new Object[0]), "getFreeformScale", new Object[0])).floatValue())});
                            MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "setNormalFreeForm", new Object[]{Boolean.valueOf(((Boolean) MiuiMultiWindowUtils.invoke(method.invoke(freeformLaunchOption, new Object[0]), "isNormalFreeForm", new Object[0])).booleanValue())});
                            options.setLaunchBounds(freeformLaunchOption.getLaunchBounds());
                            options.setLaunchWindowingMode(5);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        appendLog("setting desktop mode task bounds to %s", outParams.mBounds);
        return 1;
    }

    private void initLogBuilder(Task task, ActivityRecord activity) {
        this.mLogBuilder = new StringBuilder("MiuiDesktopModeLaunchParamsModifier: task=" + task + " activity=" + activity);
    }

    private void appendLog(String format, Object... args) {
        this.mLogBuilder.append(" ").append(String.format(format, args));
    }

    private void outputLog() {
        Slog.d(TAG, this.mLogBuilder.toString());
    }
}
