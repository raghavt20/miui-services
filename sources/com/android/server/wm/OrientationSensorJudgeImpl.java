package com.android.server.wm;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.wm.WindowOrientationListener;
import com.miui.base.MiuiStubRegistry;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class OrientationSensorJudgeImpl extends OrientationSensorJudgeStub {
    private static final int MSG_UPDATE_FOREGROUND_APP = 2;
    private static final int MSG_UPDATE_FOREGROUND_APP_SYNC = 1;
    private static final String TAG = "OrientationSensorJudgeImpl";
    private IActivityTaskManager mActivityTaskManager;
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private String mForegroundAppPackageName;
    private Handler mHandler;
    private WindowOrientationListener.OrientationSensorJudge mOrientationSensorJudge;
    private String mPendingForegroundAppPackageName;
    private TaskStackListener mTaskStackListener;
    private static final boolean LOG = SystemProperties.getBoolean("debug.orientation.log", false);
    private static final boolean mSupportUIOrientationV2 = FeatureParser.getBoolean("support_ui_orientation_v2", false);
    private String mLastPackageName = null;
    private int mDesiredRotation = -1;
    private int mLastReportedRotation = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<OrientationSensorJudgeImpl> {

        /* compiled from: OrientationSensorJudgeImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final OrientationSensorJudgeImpl INSTANCE = new OrientationSensorJudgeImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public OrientationSensorJudgeImpl m2746provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public OrientationSensorJudgeImpl m2745provideNewInstance() {
            return new OrientationSensorJudgeImpl();
        }
    }

    public void initialize(Context context, Looper looper, WindowOrientationListener.OrientationSensorJudge judge) {
        if (!mSupportUIOrientationV2) {
            return;
        }
        this.mHandler = new OrientationSensorJudgeImplHandler(looper);
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mOrientationSensorJudge = judge;
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.OrientationSensorJudgeImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                OrientationSensorJudgeImpl.this.lambda$initialize$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: registerForegroundAppUpdater, reason: merged with bridge method [inline-methods] */
    public void lambda$initialize$0() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            updateForegroundApp();
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApp() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.OrientationSensorJudgeImpl.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = OrientationSensorJudgeImpl.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null) {
                        if (info.getWindowingMode() != 5 && info.getWindowingMode() != 6) {
                            String packageName = info.topActivity.getPackageName();
                            if (packageName != null && packageName.equals(OrientationSensorJudgeImpl.this.mForegroundAppPackageName)) {
                                if (OrientationSensorJudgeImpl.LOG) {
                                    Slog.w(OrientationSensorJudgeImpl.TAG, "updateForegroundApp, app didn't change, nothing to do!");
                                    return;
                                }
                                return;
                            } else {
                                OrientationSensorJudgeImpl.this.mPendingForegroundAppPackageName = packageName;
                                if (OrientationSensorJudgeImpl.LOG) {
                                    Slog.w(OrientationSensorJudgeImpl.TAG, "updateForegroundApp, packageName = " + packageName);
                                }
                                OrientationSensorJudgeImpl.this.mHandler.sendEmptyMessage(1);
                                return;
                            }
                        }
                        if (OrientationSensorJudgeImpl.LOG) {
                            Slog.w(OrientationSensorJudgeImpl.TAG, "updateForegroundApp, WindowingMode = " + info.getWindowingMode());
                        }
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundAppSync() {
        this.mForegroundAppPackageName = this.mPendingForegroundAppPackageName;
        this.mPendingForegroundAppPackageName = null;
        if (LOG) {
            Slog.d(TAG, "updateForegroundAppSync, mPendingPackage = " + this.mForegroundAppPackageName);
        }
        if (this.mLastReportedRotation == 4) {
            this.mOrientationSensorJudge.callFinalizeRotation(4);
        }
    }

    public int finalizeRotation(int reportedRotation) {
        int newRotation;
        String packageName = getTopAppPackageName();
        this.mLastReportedRotation = reportedRotation;
        if (reportedRotation >= 0 && reportedRotation <= 3) {
            this.mDesiredRotation = reportedRotation;
            this.mOrientationSensorJudge.updateDesiredRotation(reportedRotation);
            newRotation = this.mOrientationSensorJudge.evaluateRotationChangeLocked();
        } else if (reportedRotation == 4) {
            if (packageName != null && !packageName.equals(this.mLastPackageName)) {
                this.mDesiredRotation = 0;
                this.mOrientationSensorJudge.updateDesiredRotation(0);
                newRotation = this.mOrientationSensorJudge.evaluateRotationChangeLocked();
            } else {
                newRotation = -1;
            }
        } else {
            newRotation = -1;
        }
        this.mLastPackageName = packageName;
        if (LOG) {
            Slog.d(TAG, "finalizeRotation: reportedRotation = " + reportedRotation + " mDesiredRotation = " + this.mDesiredRotation + " newRotation = " + newRotation + " mLastPackageName = " + this.mLastPackageName);
        }
        return newRotation;
    }

    private String getTopAppPackageName() {
        WindowProcessController controller;
        ActivityTaskManagerInternal activityTaskManagerInternal = this.mActivityTaskManagerInternal;
        if (activityTaskManagerInternal == null || (controller = activityTaskManagerInternal.getTopApp()) == null || controller.mInfo == null || controller.mInfo.packageName == null) {
            return null;
        }
        String packageName = controller.mInfo.packageName;
        return packageName;
    }

    public void updateDesiredRotation(int desiredRotation) {
        this.mDesiredRotation = desiredRotation;
    }

    public boolean isSupportUIOrientationV2() {
        return mSupportUIOrientationV2;
    }

    /* loaded from: classes.dex */
    class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() {
            if (OrientationSensorJudgeImpl.LOG) {
                Slog.d(OrientationSensorJudgeImpl.TAG, "onTaskStackChanged!");
            }
            OrientationSensorJudgeImpl.this.mHandler.sendEmptyMessage(2);
        }
    }

    /* loaded from: classes.dex */
    private final class OrientationSensorJudgeImplHandler extends Handler {
        public OrientationSensorJudgeImplHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    OrientationSensorJudgeImpl.this.updateForegroundAppSync();
                    return;
                case 2:
                    OrientationSensorJudgeImpl.this.updateForegroundApp();
                    return;
                default:
                    return;
            }
        }
    }
}
