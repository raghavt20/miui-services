package com.android.server.wm;

import android.app.ActivityClient;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Slog;
import android.widget.Toast;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MiuiFreeFormKeyCombinationHelper {
    private static final String TAG = "MiuiFreeFormKeyCombinationHelper";
    private MiuiFreeFormGestureController mGestureController;
    private boolean mIsHandleKeyCombination = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiuiFreeFormKeyCombinationHelper(MiuiFreeFormGestureController gestureController) {
        this.mGestureController = gestureController;
    }

    public void freeFormAndFullScreenToggleByKeyCombination(boolean isStartFreeForm) {
        Task focusedTask = null;
        if (this.mGestureController.mDisplayContent.mFocusedApp != null) {
            focusedTask = this.mGestureController.mDisplayContent.mFocusedApp.getTask();
        }
        if (focusedTask == null) {
            Slog.d(TAG, "no task focused, do noting");
            return;
        }
        if (focusedTask.isActivityTypeHomeOrRecents()) {
            Slog.d(TAG, "task isActivityTypeHomeOrRecents");
            return;
        }
        if (focusedTask.getActivity(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormKeyCombinationHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAnimating;
                isAnimating = ((ActivityRecord) obj).isAnimating(3);
                return isAnimating;
            }
        }) != null) {
            Slog.d(TAG, "topFocusedStack is in animating");
            return;
        }
        int windowingMode = focusedTask.getWindowingMode();
        Slog.d(TAG, "topFocusedStack.getRootTaskId()= " + focusedTask.getRootTaskId() + " islaunchFreeform= " + isStartFreeForm + " windowingMode= " + windowingMode + " mIsHandleKeyCombination= " + this.mIsHandleKeyCombination);
        if (this.mIsHandleKeyCombination) {
            return;
        }
        this.mIsHandleKeyCombination = true;
        try {
            try {
                if (isStartFreeForm && windowingMode == 1) {
                    if (!this.mGestureController.mService.getTaskResizeableForFreeform(focusedTask.getRootTaskId())) {
                        ActivityRecord ar = focusedTask.getTopActivity(false, true);
                        if (ar != null) {
                            showNotSupportToast(this.mGestureController.mService.mContext, getActivityName(this.mGestureController.mService.mContext, ar));
                        }
                    } else {
                        this.mGestureController.mMiuiFreeFormManagerService.freeformFullscreenTask(focusedTask.getRootTaskId());
                    }
                } else if (!isStartFreeForm && windowingMode == 5) {
                    MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(focusedTask.getRootTaskId());
                    if (mffas != null && mffas.isInFreeFormMode()) {
                        this.mGestureController.mMiuiFreeFormManagerService.fullscreenFreeformTask(focusedTask.getRootTaskId());
                    }
                } else if (isStartFreeForm && windowingMode == 6) {
                    if (!this.mGestureController.mService.getTaskResizeableForFreeform(focusedTask.getRootTaskId())) {
                        ActivityRecord ar2 = focusedTask.getTopActivity(false, true);
                        if (ar2 != null) {
                            showNotSupportToast(this.mGestureController.mService.mContext, getActivityName(this.mGestureController.mService.mContext, ar2));
                        }
                    } else if (!focusedTask.mTransitionController.isCollecting() && !focusedTask.mTransitionController.isPlaying()) {
                        this.mGestureController.mMiuiFreeFormManagerService.fromSoscToFreeform(focusedTask.mTaskId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            this.mIsHandleKeyCombination = false;
        }
    }

    private String getActivityName(Context context, ActivityRecord activityRecord) {
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            return activityRecord.info.applicationInfo.loadLabel(pm).toString();
        }
        return "";
    }

    private void showNotSupportToast(final Context context, final String activityName) {
        this.mGestureController.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormKeyCombinationHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormKeyCombinationHelper.lambda$showNotSupportToast$1(context, activityName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showNotSupportToast$1(Context context, String activityName) {
        String tips = context.getResources().getString(286196382, activityName);
        Toast.makeText(context, tips, 0).show();
    }

    public void exitFreeFormByKeyCombination(Task task) {
        MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(task.getRootTaskId());
        if (mffas != null && mffas.isInMiniFreeFormMode()) {
            return;
        }
        ActivityRecord ar = task.getTopActivity(true, true);
        if (ar != null) {
            task.setAlwaysOnTop(false);
            ActivityClient.getInstance().moveActivityTaskToBack(ar.token, true);
        } else {
            task.setAlwaysOnTop(false);
            task.moveTaskToBack(task);
        }
        if (MiuiDesktopModeUtils.isDesktopActive()) {
            this.mGestureController.mMiuiFreeFormManagerService.autoLayoutOthersIfNeed(task.getRootTaskId());
        }
    }
}
