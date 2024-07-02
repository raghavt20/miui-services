package com.android.server.wm;

import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import java.util.concurrent.ConcurrentHashMap;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiuiFreeFormStackDisplayStrategy {
    private final String TAG = "MiuiFreeFormStackDisplayStrategy";
    private int mDefaultMaxFreeformCount = 2;
    private int mDefaultMaxGameFreeformCount = 2;
    private MiuiFreeFormManagerService mFreeFormManagerService;

    public MiuiFreeFormStackDisplayStrategy(MiuiFreeFormManagerService service) {
        this.mFreeFormManagerService = service;
    }

    private boolean isSplitScreenMode() {
        return this.mFreeFormManagerService.mActivityTaskManagerService.isInSplitScreenWindowingMode();
    }

    private boolean isInEmbeddedWindowingMode(MiuiFreeFormActivityStack stack) {
        Object activityEmbedded;
        ActivityRecord topActivity = stack.mTask.getDisplayContent().getDefaultTaskDisplayArea().getTopActivity(false, true);
        if (topActivity == null || topActivity.getRootTaskId() == stack.mTask.getRootTaskId() || (activityEmbedded = MiuiMultiWindowUtils.invoke(topActivity, "isActivityEmbedded", new Object[]{false})) == null) {
            return false;
        }
        return ((Boolean) activityEmbedded).booleanValue();
    }

    public void onMiuiFreeFormStasckAdded(ConcurrentHashMap<Integer, MiuiFreeFormActivityStack> freeFormActivityStacks, MiuiFreeFormGestureController miuiFreeFormGestureController, MiuiFreeFormActivityStack addingStack) {
        MiuiFreeFormActivityStack mffas;
        MiuiFreeFormActivityStack mffas2;
        if (addingStack.getStackPackageName() != null) {
            if (!MiuiDesktopModeUtils.isDesktopActive() && addingStack.mHasHadStackAdded) {
                return;
            }
            addingStack.mHasHadStackAdded = true;
            boolean isAddingTopGame = MiuiMultiWindowAdapter.isInTopGameList(addingStack.getStackPackageName());
            StringBuilder log = new StringBuilder();
            log.append("onMiuiFreeFormStasckAdded  isAddingTopGame= ");
            log.append(isAddingTopGame);
            log.append(" addingStack= ");
            log.append(addingStack.getStackPackageName());
            MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", log.toString());
            if (isAddingTopGame) {
                if (!MiuiDesktopModeUtils.isDesktopActive()) {
                    for (Integer taskId : freeFormActivityStacks.keySet()) {
                        MiuiFreeFormActivityStack mffas3 = freeFormActivityStacks.get(taskId);
                        if (taskId.intValue() != addingStack.mTask.getRootTaskId() && MiuiMultiWindowAdapter.isInTopGameList(mffas3.getStackPackageName())) {
                            miuiFreeFormGestureController.startExitApplication(mffas3);
                            MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded Max TOP GAME FreeForm Window Num reached!");
                            return;
                        }
                    }
                } else if (this.mFreeFormManagerService.getGameFreeFormCount(addingStack) >= this.mDefaultMaxGameFreeformCount && (mffas2 = this.mFreeFormManagerService.getBottomGameFreeFormActivityStack(addingStack)) != null) {
                    miuiFreeFormGestureController.startExitApplication(mffas2);
                    MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "MiuiDesktopModeStatus isActive, onMiuiFreeFormStasckAdded Max TOP GAME FreeForm Window Num reached!");
                }
            }
            int maxStackCount = getMaxMiuiFreeFormStackCount(addingStack.getStackPackageName(), addingStack);
            int size = freeFormActivityStacks.size();
            log.delete(0, log.length());
            log.append("onMiuiFreeFormStasckAdded size = ");
            log.append(size);
            log.append(" getMaxMiuiFreeFormStackCount() = ");
            log.append(maxStackCount);
            MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", log.toString());
            if (size > maxStackCount && (mffas = this.mFreeFormManagerService.getReplaceFreeForm(addingStack)) != null) {
                if (mffas.mTask != null && mffas.mTask.getResumedActivity() != null && addingStack != null && addingStack.mTask != null && addingStack.mTask.intent != null && addingStack.mTask.intent.getComponent() != null && MiuiMultiWindowAdapter.sNotExitFreeFormWhenAddOtherFreeFormTask.get(mffas.mTask.getResumedActivity().shortComponentName) != null && ((String) MiuiMultiWindowAdapter.sNotExitFreeFormWhenAddOtherFreeFormTask.get(mffas.mTask.getResumedActivity().shortComponentName)).equals(addingStack.mTask.intent.getComponent().flattenToShortString())) {
                    return;
                }
                if (mffas.mTask != null && mffas.mTask.getResumedActivity() != null && mffas.mTask.getResumedActivity().intent != null && mffas.mTask.getResumedActivity().intent.getComponent() != null && addingStack.mTask != null && addingStack.mTask.intent != null && addingStack.mTask.intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(mffas.mTask.getResumedActivity().intent.getComponent().getClassName()) && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(addingStack.mTask.intent.getComponent().getClassName())) {
                    MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded before startExitApplication ");
                    return;
                }
                if (MiuiDesktopModeUtils.isActive(this.mFreeFormManagerService.mActivityTaskManagerService.mContext)) {
                    int frontSize = this.mFreeFormManagerService.getFrontFreeformNum(addingStack);
                    if (frontSize < maxStackCount) {
                        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded frontSize： " + frontSize + ", maxStackCount=" + maxStackCount);
                        return;
                    } else {
                        miuiFreeFormGestureController.startExitApplication(mffas);
                        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded Max FreeForm Window Num reached! exit mffas： " + mffas);
                        return;
                    }
                }
                miuiFreeFormGestureController.startExitApplication(mffas);
                MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded Max FreeForm Window Num reached! exit mffas： " + mffas);
            }
        }
    }

    public int getMaxMiuiFreeFormStackCount(String packageName, MiuiFreeFormActivityStack stack) {
        if (MiuiDesktopModeUtils.isActive(this.mFreeFormManagerService.mActivityTaskManagerService.mContext)) {
            return 4;
        }
        if (!MiuiMultiWindowUtils.multiFreeFormSupported(this.mFreeFormManagerService.mActivityTaskManagerService.mContext)) {
            return 1;
        }
        if (stack == null) {
            return 0;
        }
        int totalMemory = Build.TOTAL_RAM;
        if (totalMemory >= 7) {
            if (isInEmbeddedWindowingMode(stack) || isSplitScreenMode()) {
                return 2;
            }
            return this.mDefaultMaxFreeformCount;
        }
        if (totalMemory < 5) {
            return totalMemory >= 3 ? 1 : 0;
        }
        if (isInEmbeddedWindowingMode(stack) || isSplitScreenMode()) {
            return 1;
        }
        return this.mDefaultMaxFreeformCount;
    }
}
