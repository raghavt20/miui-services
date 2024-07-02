package com.android.server.wm;

import android.app.TaskSnapshotHelperImpl;
import android.app.TaskSnapshotHelperStub;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.server.LocalServices;
import com.android.server.am.IProcessPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.BaseAppSnapshotPersister;
import com.miui.base.MiuiStubRegistry;
import java.io.File;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TaskSnapshotControllerInjectorImpl extends TaskSnapshotControllerInjectorStub {
    private static final String TAG = "TaskSnapshot_CInjector";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TaskSnapshotControllerInjectorImpl> {

        /* compiled from: TaskSnapshotControllerInjectorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TaskSnapshotControllerInjectorImpl INSTANCE = new TaskSnapshotControllerInjectorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public TaskSnapshotControllerInjectorImpl m2787provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public TaskSnapshotControllerInjectorImpl m2786provideNewInstance() {
            return new TaskSnapshotControllerInjectorImpl();
        }
    }

    TaskSnapshotControllerInjectorImpl() {
    }

    public void doSnapshotQS(TaskSnapshotController controller, TaskSnapshotPersister persister, Handler handler, ActivityRecord r, boolean visible) {
        doSnapshotQS(controller, persister, handler, r, visible, true);
    }

    public void doSnapshotQS(final TaskSnapshotController controller, final TaskSnapshotPersister persister, Handler handler, final ActivityRecord r, final boolean visible, final boolean forceUpdate) {
        if (controller == null || persister == null || handler == null || r == null) {
            return;
        }
        if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
            Slog.w(TAG, "doSnapshotQS()...ago! activity=" + r.mActivityComponent.flattenToShortString() + ", state=" + toState(r.getState()) + ", lastVisibleTime=" + r.lastVisibleTime);
        }
        handler.postDelayed(new Runnable() { // from class: com.android.server.wm.TaskSnapshotControllerInjectorImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TaskSnapshotControllerInjectorImpl.this.lambda$doSnapshotQS$0(r, forceUpdate, controller, persister, visible);
            }
        }, TaskSnapshotHelperStub.get().delayTime(r.packageName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$doSnapshotQS$0(ActivityRecord r, boolean forceUpdate, TaskSnapshotController controller, TaskSnapshotPersister persister, boolean visible) {
        try {
            if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.w(TAG, "doSnapshotQS()...later! state=" + toState(r.getState()));
            }
            if ((r.getState() != ActivityRecord.State.RESUMED || !canTakeSnapshot(r) || !isTop(r)) && !forceUpdate) {
                Slog.w(TAG, "doSnapshotQS()... launchedBy=" + r.launchedFromPackage);
            } else {
                doSnapshotQSInternal(controller, persister, r, visible, forceUpdate);
            }
        } catch (Exception e) {
            Slog.e(TAG, "exception:", e);
        }
    }

    private void doSnapshotQSInternal(TaskSnapshotController controller, TaskSnapshotPersister persister, ActivityRecord r, boolean visible, boolean forceUpdate) {
        TaskSnapshot snapshot;
        Task task = r.getTask();
        if ((task == null || !task.isVisible()) && !forceUpdate) {
            return;
        }
        if (task.isActivityTypeHome()) {
            Slog.w(TAG, "doSnapshotQSInternal()...return as isActivityTypeHome!");
            return;
        }
        try {
            int windowType = task.mDisplayContent.mCurrentFocus.getWindowInfo().type;
            if (windowType != 1) {
                Slog.w(TAG, "doSnapshotQSInternal()... return as window type is incorrect, type=" + windowType);
                return;
            }
            if (!TaskSnapshotPersisterInjectorStub.get().couldPersist(new BaseAppSnapshotPersister.DirectoryResolver() { // from class: com.android.server.wm.TaskSnapshotControllerInjectorImpl$$ExternalSyntheticLambda0
                public final File getSystemDirectoryForUser(int i) {
                    return Environment.getDataSystemCeDirectory(i);
                }
            }, r)) {
                Slog.w(TAG, "couldPersist()... false.");
                return;
            }
            switch (controller.getSnapshotMode(task)) {
                case 0:
                    snapshot = controller.snapshot(task, true);
                    break;
                default:
                    Slog.w(TAG, "doSnapshotQSInternal()...snapshot mode NOT ok!");
                    snapshot = null;
                    break;
            }
            if (snapshot == null && forceUpdate) {
                if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                    Slog.d(TAG, "snapshot is null and use snapshot if needed. current state " + r.getState().name());
                }
                if (!r.isState(ActivityRecord.State.RESUMED)) {
                    snapshot = controller.getSnapshot(task.mTaskId, task.mUserId, false, false);
                }
            }
            if (snapshot != null && TaskSnapshotHelperImpl.QUICK_START_NAME_WITH_ACTIVITY_LIST.contains(r.packageName)) {
                snapshot.setClassNameQS(r.intent.getComponent().getClassName());
            }
            saveSnapshot(snapshot, task, persister);
        } catch (Exception e) {
            Slog.d(TAG, "exception:" + e.getMessage());
        }
    }

    private void saveSnapshot(TaskSnapshot snapshot, Task task, TaskSnapshotPersister persister) {
        if (snapshot == null) {
            Slog.w(TAG, "doSnapshotQSInternal()...snapshot is null!");
            return;
        }
        GraphicBuffer buffer = snapshot.getSnapshot();
        if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
            buffer.destroy();
            Slog.e(TAG, "Invalid task snapshot dimensions " + buffer.getWidth() + "*" + buffer.getHeight());
        } else {
            persister.persistSnapshotQS(task.mTaskId, task.mUserId, snapshot);
        }
    }

    public boolean isTop(ActivityRecord r) {
        Task task = r.getTask();
        if (task != null) {
            String current = r.mActivityComponent.flattenToShortString();
            String top = task.topRunningActivityLocked().mActivityComponent.flattenToShortString();
            if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.w(TAG, "isTop()...current=" + current + ", topActivity=" + top);
            }
            return current.equals(top);
        }
        return false;
    }

    public static String toState(ActivityRecord.State state) {
        if (state == ActivityRecord.State.DESTROYED) {
            return "DESTROYED";
        }
        if (state == ActivityRecord.State.DESTROYING) {
            return "DESTROYING";
        }
        if (state == ActivityRecord.State.FINISHING) {
            return "FINISHING";
        }
        if (state == ActivityRecord.State.INITIALIZING) {
            return "INITIALIZING";
        }
        if (state == ActivityRecord.State.PAUSED) {
            return "PAUSED";
        }
        if (state == ActivityRecord.State.PAUSING) {
            return "PAUSING";
        }
        if (state == ActivityRecord.State.RESTARTING_PROCESS) {
            return "RESTARTING_PROCESS";
        }
        if (state == ActivityRecord.State.RESUMED) {
            return "RESUMED";
        }
        if (state == ActivityRecord.State.STARTED) {
            return "STARTED";
        }
        if (state == ActivityRecord.State.STOPPED) {
            return "STOPPED";
        }
        if (state == ActivityRecord.State.STOPPING) {
            return "STOPPING";
        }
        return IProcessPolicy.REASON_UNKNOWN;
    }

    public boolean canTakeSnapshot(ActivityRecord r) {
        return TaskSnapshotHelperImpl.QUICK_START_HOME_PACKAGE_NAME_LIST.contains(r.launchedFromPackage) || TaskSnapshotHelperImpl.QUICK_START_SPECIAL_PACKAGE_NAME_LIST.contains(r.packageName);
    }

    public void adaptLetterboxInsets(ActivityRecord activity, Rect letterboxInsets) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.adaptLetterboxInsets(activity, letterboxInsets);
        }
    }
}
