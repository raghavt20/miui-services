package com.android.server.wm;

import android.appcompat.ApplicationCompatUtilsStub;
import android.os.Build;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.wm.TaskStub;
import com.android.server.wm.Transition;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerInternal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class TaskStubImpl implements TaskStub {
    private static final String TAG = "TaskStubImpl";
    private static final List<String> mNeedFinishAct;
    private static final List<String> mVtCameraNeedFinishAct;
    private boolean isHierarchyBottom = false;

    /* loaded from: classes.dex */
    public static final class MutableTaskStubImpl implements TaskStub.MutableTaskStub {
        private boolean mIsCirculatedToVirtual;
        private boolean mIsSplitMode;
        private int mLastDisplayId;
        Task mTask;

        /* loaded from: classes.dex */
        public final class Provider implements MiuiStubRegistry.ImplProvider<MutableTaskStubImpl> {

            /* compiled from: TaskStubImpl$MutableTaskStubImpl$Provider.java */
            /* loaded from: classes.dex */
            public static final class SINGLETON {
                public static final MutableTaskStubImpl INSTANCE = new MutableTaskStubImpl();
            }

            /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
            public MutableTaskStubImpl m2791provideSingleton() {
                return SINGLETON.INSTANCE;
            }

            /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
            public MutableTaskStubImpl m2790provideNewInstance() {
                return new MutableTaskStubImpl();
            }
        }

        public int getLastDisplayId() {
            return this.mLastDisplayId;
        }

        public void setLastDisplayId(int lastDisplayId) {
            this.mLastDisplayId = lastDisplayId;
        }

        public boolean getIsCirculatedToVirtual() {
            return this.mIsCirculatedToVirtual;
        }

        public void setIsCirculatedToVirtual(boolean isCirculatedToVirtual) {
            this.mIsCirculatedToVirtual = isCirculatedToVirtual;
        }

        public void init(Task task) {
            this.mTask = task;
            this.mIsSplitMode = false;
        }

        public boolean isSplitMode() {
            return this.mIsSplitMode;
        }

        public void setSplitMode(boolean isSplitMode) {
            this.mIsSplitMode = isSplitMode;
        }
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TaskStubImpl> {

        /* compiled from: TaskStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TaskStubImpl INSTANCE = new TaskStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public TaskStubImpl m2793provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public TaskStubImpl m2792provideNewInstance() {
            return new TaskStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        mNeedFinishAct = arrayList;
        arrayList.add("com.miui.securitycenter/com.miui.permcenter.permissions.SystemAppPermissionDialogActivity");
        arrayList.add("com.xiaomi.account/.ui.SystemAccountAuthDialogActivity");
        ArrayList arrayList2 = new ArrayList();
        mVtCameraNeedFinishAct = arrayList2;
        arrayList2.add("com.milink.service/com.xiaomi.vtcamera.activities.CameraServerActivity");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isSplitScreenModeDismissed$0(Task root, Task task) {
        return task != root && task.hasChild();
    }

    public boolean isSplitScreenModeDismissed(final Task root) {
        return root != null && root.getTask(new Predicate() { // from class: com.android.server.wm.TaskStubImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskStubImpl.lambda$isSplitScreenModeDismissed$0(root, (Task) obj);
            }
        }) == null;
    }

    public void onSplitScreenParentChanged(WindowContainer<?> oldParent, WindowContainer<?> newParent) {
        ActivityRecord topActivity;
        long start = SystemClock.uptimeMillis();
        boolean newInFullScreenMode = false;
        boolean oldInSpliScreenMode = (oldParent == null || oldParent.asTask() == null || !oldParent.inSplitScreenWindowingMode()) ? false : true;
        if (newParent != null && newParent.asTaskDisplayArea() != null && newParent.getWindowingMode() == 1) {
            newInFullScreenMode = true;
        }
        if (oldInSpliScreenMode && newInFullScreenMode && isSplitScreenModeDismissed(oldParent.asTask().getRootTask())) {
            Task task = newParent.asTaskDisplayArea().getTopRootTaskInWindowingMode(1);
            if (task == null || (topActivity = task.topRunningActivityLocked()) == null) {
                return;
            }
            if (ApplicationCompatUtilsStub.get().isContinuityEnabled()) {
                AppContinuityRouterStub.get().onSplitToFullScreenChanged(topActivity);
            }
            ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity);
            PassivePenAppWhiteListImpl.getInstance().onSplitScreenExit();
        }
        long took = SystemClock.uptimeMillis() - start;
        if (took > 50) {
            Slog.d(TAG, "onSplitScreenParentChanged took " + took + "ms");
        }
    }

    public boolean isHierarchyBottom() {
        return this.isHierarchyBottom;
    }

    public void setHierarchy(boolean bottom) {
        this.isHierarchyBottom = bottom;
    }

    public void updateForegroundActivityInAppPair(Task task, boolean isInAppPair) {
        Task topStack;
        ActivityRecord topActivity;
        if (!isInAppPair) {
            TaskDisplayArea displayArea = task.getDisplayArea();
            if (displayArea == null || (topStack = displayArea.getTopRootTaskInWindowingMode(1)) == null || (topActivity = topStack.topRunningActivityLocked()) == null) {
                return;
            }
            ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity);
            return;
        }
        ActivityRecord topActivity2 = task.topRunningActivityLocked();
        if (topActivity2 == null) {
            return;
        }
        ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity2);
    }

    public void notifyMovetoFront(int uid, boolean inFreeformSmallWinMode) {
        GreezeManagerInternal.getInstance().notifyMovetoFront(uid, inFreeformSmallWinMode);
    }

    public void notifyFreeformModeFocus(String packageName, int mode) {
        GreezeManagerInternal.getInstance().notifyFreeformModeFocus(packageName, mode);
    }

    public void notifyMultitaskLaunch(int uid, String packageName) {
        GreezeManagerInternal.getInstance().notifyMultitaskLaunch(uid, packageName);
    }

    public List<ActivityRecord> getActivities(Predicate<ActivityRecord> callback, boolean traverseTopToBottom, ActivityRecord boundary, WindowList<? extends WindowContainer> children) {
        List<ActivityRecord> activityRecordList = new LinkedList<>();
        if (traverseTopToBottom) {
            int i = children.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                WindowContainer wc = (WindowContainer) children.get(i);
                if (wc == boundary) {
                    activityRecordList.add(boundary);
                    break;
                }
                ActivityRecord r = wc.getActivity(callback, traverseTopToBottom, boundary);
                if (r != null) {
                    activityRecordList.add(r);
                }
                i--;
            }
        } else {
            int count = children.size();
            int i2 = 0;
            while (true) {
                if (i2 >= count) {
                    break;
                }
                WindowContainer wc2 = (WindowContainer) children.get(i2);
                if (wc2 == boundary) {
                    activityRecordList.add(boundary);
                    break;
                }
                ActivityRecord r2 = wc2.getActivity(callback, traverseTopToBottom, boundary);
                if (r2 != null) {
                    activityRecordList.add(r2);
                }
                i2++;
            }
        }
        return activityRecordList;
    }

    public int getLayoutInDisplayCutoutMode(WindowManager.LayoutParams attrs) {
        if (attrs == null) {
            return 0;
        }
        return attrs.layoutInDisplayCutoutMode;
    }

    public void clearRootProcess(WindowProcessController app, Task task) {
        ActivityRecord top;
        if (app == null || task == null || !app.isRemoved() || task.getNonFinishingActivityCount() != 1) {
            return;
        }
        if ((task.affinity == null || task.affinity.contains("com.xiaomi.vipaccount")) && (top = task.getTopNonFinishingActivity()) != null && top.app != app && mNeedFinishAct.contains(top.shortComponentName)) {
            Slog.d(TAG, "Only left " + top.shortComponentName + " in task " + task.mTaskId + " and finish it.");
            top.finishIfPossible("clearNonAppAct", false);
        }
    }

    public void clearVirtualCamera(WindowProcessController app, Task task) {
        ActivityRecord top;
        if (app == null || task == null) {
            return;
        }
        if ((task.affinity == null || task.affinity.contains("com.xiaomi.vtcamera")) && (top = task.getTopNonFinishingActivity()) != null && top.app != app && mVtCameraNeedFinishAct.contains(top.shortComponentName)) {
            Slog.d(TAG, "Only left " + top.shortComponentName + " in task " + task.mTaskId + " and finish it.");
            top.finishIfPossible("clearVtCameraAct", false);
        }
    }

    public boolean inSplitScreenWindowingMode(Task task) {
        synchronized (task.mAtmService.mGlobalLock) {
            boolean z = false;
            if (task.isRootTask()) {
                if (task.mCreatedByOrganizer && task.getWindowingMode() == 1 && task.hasChild() && task.getTopChild() != null && task.getTopChild().asTask() != null && task.getTopChild().getWindowingMode() == 6) {
                    z = true;
                }
                return z;
            }
            Task rootTask = task.getRootTask();
            if (rootTask != null && rootTask.mCreatedByOrganizer && rootTask.hasChild() && rootTask.getWindowingMode() == 1 && rootTask.getTopChild() != null && rootTask.getTopChild().asTask() != null && rootTask.getTopChild().getWindowingMode() == 6) {
                z = true;
            }
            return z;
        }
    }

    public void clearSizeCompatInSplitScreen(int prevWinMode, int newWinMode, WindowContainer wc) {
        if (Build.IS_MIUI && !miui.os.Build.IS_TABLET) {
            Task task = wc != null ? wc.asTask() : null;
            if (newWinMode == 6 && task != null && task.getWindowingMode() == 6 && task.getParent() != null && task.getParent().getWindowingMode() == 6) {
                task.forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskStubImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TaskStubImpl.lambda$clearSizeCompatInSplitScreen$1((ActivityRecord) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$clearSizeCompatInSplitScreen$1(ActivityRecord r) {
        if (r.hasSizeCompatBounds() || r.getCompatDisplayInsets() != null) {
            r.clearSizeCompatMode();
        }
    }

    public void onHoverModeTaskPrepareSurfaces(Task task) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onHoverModeTaskPrepareSurfaces(task);
        }
    }

    public void notifyActivityPipModeChangedForHoverMode(boolean inPip, ActivityRecord ar) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onActivityPipModeChangedForHoverMode(inPip, ar);
        }
    }

    public void onHoverModeTaskParentChanged(Task task, WindowContainer newParent) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onHoverModeTaskParentChanged(task, newParent);
        }
    }

    public void onTaskConfigurationChanged(int prevWindowingMode, int overrideWindowingMode) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onTaskConfigurationChanged(prevWindowingMode, overrideWindowingMode);
        }
    }

    public boolean skipSplitScreenTaskIfNeeded(WindowContainer container, ArraySet<WindowContainer> participants) {
        if (container.asTask() == null || !container.inSplitScreenWindowingMode()) {
            return false;
        }
        boolean taskChildInParticipants = false;
        int i = container.mChildren.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            if (participants.contains(container.getChildAt(i))) {
                taskChildInParticipants = true;
                break;
            }
            i--;
        }
        if (taskChildInParticipants) {
            return false;
        }
        boolean otherTaskChildInParticipants = false;
        for (int i2 = participants.size() - 1; i2 >= 0; i2--) {
            WindowContainer<?> wc = participants.valueAt(i2);
            if (!wc.equals(container) && wc.asTask() != null && wc.inSplitScreenWindowingMode()) {
                for (int j = wc.mChildren.size() - 1; j >= 0; j--) {
                    WindowContainer child = (WindowContainer) wc.mChildren.get(j);
                    if (participants.contains(child)) {
                        otherTaskChildInParticipants = true;
                        if (container.equals(child)) {
                            return false;
                        }
                    }
                }
            }
        }
        return otherTaskChildInParticipants;
    }

    public boolean skipPinnedTaskIfNeeded(WindowContainer container, Transition.ChangeInfo changeInfo, ArraySet<WindowContainer> participants) {
        if (!container.inPinnedWindowingMode() || container.isVisibleRequested() || !changeInfo.mVisible || container.mTransitionController.getCollectingTransitionType() != 1) {
            return false;
        }
        boolean activitySwitchInFreeform = false;
        for (int i = participants.size() - 1; i >= 0; i--) {
            WindowContainer<?> wc = participants.valueAt(i);
            if (!wc.equals(container) && (!wc.inPinnedWindowingMode() || wc.isVisibleRequested())) {
                if (wc.asActivityRecord() == null || !wc.inFreeformWindowingMode()) {
                    return false;
                }
                activitySwitchInFreeform = true;
            }
        }
        if (activitySwitchInFreeform) {
            Slog.i(TAG, "skip pip task " + container + " for activity switch in freeform task");
        }
        return activitySwitchInFreeform;
    }

    public boolean disallowEnterPip(ActivityRecord toFrontActivity) {
        if (toFrontActivity != null && toFrontActivity.toString().contains("com.tencent.mm/.plugin.base.stub.WXEntryActivity")) {
            Slog.i(TAG, "Skip pip enter request for transient activity : " + toFrontActivity);
            return true;
        }
        return false;
    }

    public boolean addInvisiblePipTaskToTransition(boolean inVisibleState, WindowContainer wc, int windowingMode) {
        if (wc.asTask() == null || wc.mDisplayContent == null || !wc.mDisplayContent.isSleeping() || !inVisibleState || !wc.isOrganized() || windowingMode == 0 || windowingMode == wc.getWindowingMode() || !wc.inPinnedWindowingMode()) {
            return false;
        }
        Slog.i(TAG, "Add invisible pip task to transition, taskId : " + wc.asTask().mTaskId);
        return true;
    }

    public void checkFreeFormActivityRecordIfNeeded(ArrayList<Transition.ChangeInfo> targetList) {
        for (int i = targetList.size() - 1; i >= 0; i--) {
            Transition.ChangeInfo targetChange = targetList.get(i);
            WindowContainer<?> target = targetChange.mContainer;
            if (target.asActivityRecord() != null && target.inFreeformWindowingMode() && shouldSkipFreeFormActivityRecord(targetList, target.asActivityRecord())) {
                targetList.remove(i);
                Slog.d(TAG, " skipFreeFormActivityRecordIfNeeded: target= " + target);
            }
        }
    }

    public boolean shouldSkipFreeFormActivityRecord(ArrayList<Transition.ChangeInfo> targetList, ActivityRecord freeformActivityRecord) {
        if (freeformActivityRecord == null || !freeformActivityRecord.inFreeformWindowingMode()) {
            return false;
        }
        for (int i = targetList.size() - 1; i >= 0; i--) {
            Transition.ChangeInfo targetChange = targetList.get(i);
            WindowContainer<?> target = targetChange.mContainer;
            if (!target.equals(freeformActivityRecord) && target.asWallpaperToken() == null && (target.asActivityRecord() == null || target.getParent() == null || !target.getParent().equals(freeformActivityRecord.getTask()))) {
                return true;
            }
        }
        return false;
    }
}
