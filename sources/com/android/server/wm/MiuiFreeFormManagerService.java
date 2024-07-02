package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.widget.Toast;
import android.window.TransitionInfo;
import android.window.WindowContainerTransaction;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.xiaomi.freeform.MiuiFreeformStub;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.app.IFreeformCallback;
import miui.app.IMiuiFreeFormManager;
import miui.app.IMiuiFreeformModeControl;
import miui.app.MiuiFreeFormManager;

/* loaded from: classes.dex */
public class MiuiFreeFormManagerService extends IMiuiFreeFormManager.Stub implements MiuiFreeFormManagerServiceStub {
    private static final String TAG = "MiuiFreeFormManagerService";
    ActivityTaskManagerService mActivityTaskManagerService;
    private String mApplicationUsedInFreeform;
    MiuiFreeFormCameraStrategy mFreeFormCameraStrategy;
    MiuiFreeFormGestureController mFreeFormGestureController;
    MiuiFreeFormStackDisplayStrategy mFreeFormStackDisplayStrategy;
    private IMiuiFreeformModeControl mFreeformModeControl;
    Handler mHandler;
    private int mImeHeight;
    private boolean mIsImeShowing;
    final ConcurrentHashMap<Integer, MiuiFreeFormActivityStack> mFreeFormActivityStacks = new ConcurrentHashMap<>();
    private final RemoteCallbackList<IFreeformCallback> mCallbacks = new RemoteCallbackList<>();
    private boolean mAutoLayoutModeOn = false;
    private Map<Integer, Float> mCorrectScaleList = new HashMap();
    private ArraySet<Integer> mAppBehindHome = new ArraySet<>();
    private final IBinder.DeathRecipient mFreeformModeControlDeath = new IBinder.DeathRecipient() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda2
        @Override // android.os.IBinder.DeathRecipient
        public final void binderDied() {
            MiuiFreeFormManagerService.this.lambda$new$0();
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreeFormManagerService> {

        /* compiled from: MiuiFreeFormManagerService$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreeFormManagerService INSTANCE = new MiuiFreeFormManagerService();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiFreeFormManagerService m2533provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiFreeFormManagerService m2532provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.wm.MiuiFreeFormManagerService is marked as singleton");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            detachFreeformModeControl();
            this.mFreeFormGestureController.clearAllFreeFormForProcessReboot();
        }
    }

    public void registerMiuiFreeformModeControl(IMiuiFreeformModeControl freeformModeControl) {
        try {
            IMiuiFreeformModeControl iMiuiFreeformModeControl = this.mFreeformModeControl;
            if (iMiuiFreeformModeControl != null) {
                if (iMiuiFreeformModeControl.asBinder() != null) {
                    this.mFreeformModeControl.asBinder().unlinkToDeath(this.mFreeformModeControlDeath, 0);
                }
                detachFreeformModeControl();
            }
            if (freeformModeControl.asBinder() != null) {
                freeformModeControl.asBinder().linkToDeath(this.mFreeformModeControlDeath, 0);
            }
            this.mFreeformModeControl = freeformModeControl;
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to set transition player");
        }
    }

    private void detachFreeformModeControl() {
        if (this.mFreeformModeControl == null) {
            return;
        }
        this.mFreeformModeControl = null;
    }

    public boolean isAutoLayoutModeOn() {
        return this.mAutoLayoutModeOn;
    }

    public void updateAutoLayoutModeStatus(boolean isOn) {
        this.mAutoLayoutModeOn = isOn;
    }

    public void setAdjustedForIme(boolean adjustedForIme, int imeHeight, boolean adjustedForRotation) {
        boolean imeShowing = adjustedForIme && imeHeight > 0;
        int imeHeight2 = imeShowing ? imeHeight : 0;
        if (imeShowing == this.mIsImeShowing && imeHeight2 == this.mImeHeight) {
            return;
        }
        this.mIsImeShowing = imeShowing;
        this.mImeHeight = imeHeight2;
        notifyImeVisibilityChanged(imeShowing, imeHeight2, adjustedForRotation);
    }

    public void init(ActivityTaskManagerService ams) {
        this.mActivityTaskManagerService = ams;
        this.mFreeFormStackDisplayStrategy = new MiuiFreeFormStackDisplayStrategy(this);
        this.mFreeFormCameraStrategy = new MiuiFreeFormCameraStrategy(this);
        HandlerThread handlerThread = new HandlerThread(TAG, -4);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        this.mFreeFormGestureController = new MiuiFreeFormGestureController(this.mActivityTaskManagerService, this, this.mHandler);
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getMiuiFreeFormStackInfo(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return null;
        }
        MiuiFreeFormManager.MiuiFreeFormStackInfo res = mffas.getMiuiFreeFormStackInfo();
        return res;
    }

    public boolean adjustMovedToTopIfNeed(boolean alreadyResumed, Task topFocusableTask, boolean movedToTop, Task focusTask) {
        if (movedToTop && topFocusableTask != null && topFocusableTask.inFreeformWindowingMode() && topFocusableTask.isAlwaysOnTop() && !focusTask.isAlwaysOnTop() && alreadyResumed) {
            Slog.d(TAG, "adjustMovedToTop false topFocusableTask: " + topFocusableTask);
            return false;
        }
        return movedToTop;
    }

    public boolean inPinMode(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        boolean res = mffas.inPinMode();
        return res;
    }

    public void setFreeformForceHideIfNeed(List<WindowContainerTransaction.HierarchyOp> hops, int hopSize, Map.Entry<IBinder, WindowContainerTransaction.Change> entry, WindowContainer wc) {
        boolean reorderToBack = false;
        boolean unsetAlwaysOnTop = false;
        boolean isFreeformDisplayArea = wc.getTaskDisplayArea() != null && wc.getTaskDisplayArea().inFreeformWindowingMode();
        if (wc.asTask() != null && wc.inFreeformWindowingMode() && entry.getValue().getWindowingMode() == 0 && !isFreeformDisplayArea) {
            for (int i = 0; i < hopSize; i++) {
                WindowContainerTransaction.HierarchyOp hop = hops.get(i);
                if (hop.getType() == 1) {
                    WindowContainer hopWc = WindowContainer.fromBinder(hop.getContainer());
                    if (wc.equals(hopWc)) {
                        reorderToBack = !hop.getToTop();
                    }
                }
            }
            for (int i2 = 0; i2 < hopSize; i2++) {
                WindowContainerTransaction.HierarchyOp hop2 = hops.get(i2);
                if (hop2.getType() == 12) {
                    WindowContainer hopWc2 = WindowContainer.fromBinder(hop2.getContainer());
                    if (wc.equals(hopWc2)) {
                        unsetAlwaysOnTop = !hop2.isAlwaysOnTop();
                    }
                }
            }
        }
        if (reorderToBack && unsetAlwaysOnTop) {
            Slog.d(TAG, "setforceHiden wc: " + wc + " hops: " + hops);
            wc.asTask().setForceHidden(65536, true);
        }
    }

    public void unsetFreeformForceHideIfNeed(WindowContainerTransaction.HierarchyOp hop, WindowContainer container) {
        if (container.asTask() != null && container.asTask().isForceHidden() && !hop.isAlwaysOnTop()) {
            Slog.d(TAG, "unsetforceHiden wc: " + container);
            container.asTask().setForceHidden(65536, false);
        }
    }

    public void setMiuiFreeformScale(int taskId, float miuiFreeformScale) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null && Math.abs(mffas.getFreeFormScale() - miuiFreeformScale) >= 0.001f) {
            mffas.setFreeformScale(miuiFreeformScale);
        }
    }

    public void setMiuiFreeformOrientation(int taskId, boolean isLandscape) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.mIsLandcapeFreeform = isLandscape;
    }

    public void setMiuiFreeformPinPos(int taskId, Rect pinPos) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.mPinFloatingWindowPos.set(pinPos);
    }

    public void setMiuiFreeformPinedActiveTime(int taskId, long activeTime) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.pinActiveTime = activeTime;
    }

    public void setMiuiFreeformIsForegroundPin(int taskId, boolean isForegroundPin) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.mIsForegroundPin = isForegroundPin;
    }

    public void setMiuiFreeformIsNeedAnimation(int taskId, boolean needAnimation) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.setNeedAnimation(needAnimation);
    }

    public boolean getMiuiFreeformIsForegroundPin(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        return mffas.mIsForegroundPin;
    }

    public float getFreeformScale(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return 1.0f;
        }
        float freeformScale = mffas.getFreeFormScale();
        return freeformScale;
    }

    public void setMiuiFreeformMode(int taskId, int miuiFreeformMode) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            mffas.setStackFreeFormMode(miuiFreeformMode);
        }
    }

    public int getMiuiFreeformMode(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return -1;
        }
        int freeformMode = mffas.getMiuiFreeFromWindowMode();
        return freeformMode;
    }

    public boolean isInMiniFreeFormMode(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        boolean res = mffas.isInMiniFreeFormMode();
        return res;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateDataFromSetting() {
        this.mApplicationUsedInFreeform = getApplicationUsedInFreeform(this.mActivityTaskManagerService.mContext);
    }

    public boolean shouldAddingToTask(String shortComponentName) {
        return MiuiMultiWindowAdapter.getLaunchInTaskList().contains(shortComponentName);
    }

    public boolean isSupportPin() {
        return true;
    }

    public boolean hasMiuiFreeformOnShellFeature() {
        return true;
    }

    public int getCurrentMiuiFreeFormNum() {
        return this.mFreeFormActivityStacks.size();
    }

    public void addFreeFormActivityStack(Task rootTask) {
        addFreeFormActivityStack(rootTask, 0, null);
    }

    public void addFreeFormActivityStack(Task rootTask, String reason) {
        addFreeFormActivityStack(rootTask, 0, reason);
    }

    public void addFreeFormActivityStack(Task rootTask, int miuiFreeFromWindowMode) {
        addFreeFormActivityStack(rootTask, miuiFreeFromWindowMode, null);
    }

    public void addFreeFormActivityStack(Task rootTask, int miuiFreeFromWindowMode, String reason) {
        if (rootTask.isActivityTypeHome()) {
            return;
        }
        logd(TAG, "addFreeFormActivityStack reason=" + reason);
        if (this.mFreeFormActivityStacks.containsKey(Integer.valueOf(rootTask.mTaskId))) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(Integer.valueOf(rootTask.mTaskId));
            if (MiuiDesktopModeUtils.isDesktopActive() && !this.mCorrectScaleList.isEmpty() && this.mCorrectScaleList.get(Integer.valueOf(rootTask.mTaskId)) != null) {
                mffas.mFreeFormScale = this.mCorrectScaleList.get(Integer.valueOf(rootTask.mTaskId)).floatValue();
                this.mCorrectScaleList.remove(Integer.valueOf(rootTask.mTaskId));
            }
            int maxStackCount = this.mFreeFormStackDisplayStrategy.getMaxMiuiFreeFormStackCount(mffas.getStackPackageName(), mffas);
            boolean isAddingTopGame = MiuiMultiWindowAdapter.isInTopGameList(mffas.getStackPackageName());
            if (this.mFreeFormActivityStacks.size() <= maxStackCount && !isAddingTopGame) {
                logd(TAG, "addFreeFormActivityStack as = " + rootTask + " mffas.isInFreeFormMode()=" + mffas.isInFreeFormMode() + ", miuiFreeFromWindowMode=" + miuiFreeFromWindowMode + ",maxStackCount=" + maxStackCount + ", mFreeFormActivityStacks.size()=" + this.mFreeFormActivityStacks.size() + ", isAddingTopGame=" + isAddingTopGame);
                return;
            }
            if (MiuiDesktopModeUtils.isDesktopActive() && mffas.isFrontFreeFormStackInfo() && rootTask.hasActivity()) {
                int frontSize = getFrontFreeformNum(mffas);
                logd(TAG, "addFreeFormActivityStack as = " + rootTask + " mffas.isInFreeFormMode()=" + mffas.isInFreeFormMode() + ", isAddingTopGame=" + isAddingTopGame + ",frontSize=" + frontSize);
                if (mffas.isInFreeFormMode()) {
                    if (frontSize >= maxStackCount || isAddingTopGame) {
                        this.mFreeFormStackDisplayStrategy.onMiuiFreeFormStasckAdded(this.mFreeFormActivityStacks, this.mFreeFormGestureController, mffas);
                        return;
                    }
                    return;
                }
                return;
            }
            return;
        }
        MiuiFreeFormActivityStack mffas2 = new MiuiFreeFormActivityStack(rootTask, miuiFreeFromWindowMode);
        logd(TAG, "addFreeFormActivityStack as = " + rootTask + " mffas.isInFreeFormMode()=" + mffas2.isInFreeFormMode() + ", miuiFreeFromWindowMode=" + miuiFreeFromWindowMode);
        if (MiuiDesktopModeUtils.isDesktopActive() && !this.mCorrectScaleList.isEmpty() && this.mCorrectScaleList.get(Integer.valueOf(rootTask.mTaskId)) != null) {
            logd(TAG, "addFreeFormActivityStack  mCorrectScale  " + this.mCorrectScaleList.get(Integer.valueOf(rootTask.mTaskId)));
            mffas2.mFreeFormScale = this.mCorrectScaleList.get(Integer.valueOf(rootTask.mTaskId)).floatValue();
            this.mCorrectScaleList.remove(Integer.valueOf(rootTask.mTaskId));
        }
        this.mFreeFormActivityStacks.put(Integer.valueOf(rootTask.mTaskId), mffas2);
        onMiuiFreeFormStasckAdded(mffas2);
        if (mffas2.getStackPackageName() != null) {
            if (mffas2.isInFreeFormMode()) {
                dispatchFreeFormStackModeChanged(0, mffas2);
                return;
            } else {
                if (mffas2.isInMiniFreeFormMode()) {
                    dispatchFreeFormStackModeChanged(1, mffas2);
                    return;
                }
                return;
            }
        }
        mffas2.mShouldDelayDispatchFreeFormStackModeChanged = true;
    }

    private boolean isHomeTopTask() {
        Task currentFullRootTask = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
        if (currentFullRootTask != null && currentFullRootTask.isActivityTypeHomeOrRecents()) {
            return true;
        }
        return false;
    }

    private void showFreeformIfNeeded(int taskId) {
        if (MiuiDesktopModeUtils.isDesktopActive() && this.mFreeformModeControl != null) {
            try {
                Slog.i(TAG, "show hidden task  ...." + taskId);
                if (isHomeTopTask()) {
                    this.mFreeformModeControl.showFreeform(taskId);
                } else {
                    this.mFreeformModeControl.fromFulltoFreeform(taskId);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Error showFreeformIfNeeded.", e);
            }
        }
    }

    public void onStartActivity(Task as, ActivityOptions options, Intent intent) {
        MiuiFreeFormActivityStack mffas;
        Method method;
        if (as == null || !as.isActivityTypeHome()) {
            logd(TAG, "onStartActivityInner as = " + as + " op = " + options);
            String oriPkg = intent != null ? intent.getStringExtra("android.intent.extra.shortcut.NAME") : null;
            int oriUid = intent != null ? intent.getIntExtra("originating_uid", 0) : 0;
            if (oriPkg != null && oriPkg.length() > 0 && as != null) {
                if (as.inMultiWindowMode()) {
                    as.behindAppLockPkg = oriPkg;
                    as.originatingUid = oriUid;
                } else {
                    Task task = as.getRootTask();
                    if (task != null) {
                        task.behindAppLockPkg = oriPkg;
                        task.originatingUid = oriUid;
                    }
                }
            }
            if (options != null && options.getLaunchWindowingMode() == 5 && as != null && as.mAtmService.mSupportsFreeformWindowManagement && (method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0])) != null) {
                try {
                    boolean needAnimation = ((Boolean) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformAnimation", new Object[0])).booleanValue();
                    addFreeFormActivityStack(as.inMultiWindowMode() ? as : as.getRootTask(), "onStartActivity");
                    MiuiFreeFormActivityStack mffas2 = getMiuiFreeFormActivityStackForMiuiFB(as.getRootTaskId());
                    mffas2.isNormalFreeForm = ((Boolean) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "isNormalFreeForm", new Object[0])).booleanValue();
                    mffas2.setNeedAnimation(needAnimation);
                    float scaleInOptions = ((Float) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformScale", new Object[0])).floatValue();
                    mffas2.mFreeFormScale = scaleInOptions != -1.0f ? scaleInOptions : MiuiMultiWindowUtils.getOriFreeformScale(this.mActivityTaskManagerService.mContext, mffas2.mIsLandcapeFreeform, mffas2.isNormalFreeForm, MiuiDesktopModeUtils.isActive(this.mActivityTaskManagerService.mContext), mffas2.getStackPackageName());
                    if (options.getLaunchFromTaskId() != 0) {
                        mffas2.mFreeFormLaunchFromTaskId = options.getLaunchFromTaskId();
                    }
                    logd(TAG, "onStartActivityInner as = " + as + " op = " + options + " options.getLaunchFromTaskId()= " + options.getLaunchFromTaskId());
                } catch (Exception e) {
                }
            }
            if (MiuiDesktopModeUtils.isDesktopActive() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(as.getRootTaskId())) != null) {
                if ((!as.isVisible() || !mffas.isFrontFreeFormStackInfo()) && mffas.getStackPackageName() != null && as.mTaskSupervisor.getLaunchParamsController().hasFreeformDesktopMemory(as)) {
                    float lastScale = as.mTaskSupervisor.getLaunchParamsController().getFreeformLastScale(mffas.mTask);
                    setMiuiFreeformScale(as.getRootTaskId(), lastScale);
                    Rect lastBounds = as.mTaskSupervisor.getLaunchParamsController().getFreeformLastPosition(mffas.mTask);
                    if (lastBounds != null) {
                        mffas.mIsLandcapeFreeform = lastBounds.width() > lastBounds.height();
                    }
                    mffas.isNormalFreeForm = isNormalFreeForm(as, getStackPackageName(as));
                }
            }
        }
    }

    public void onShowToastIfNeeded(Task as, ActivityOptions options) {
        if ((as == null || !as.isActivityTypeHome()) && options != null && options.getLaunchWindowingMode() == 5 && as != null && as.mAtmService.mSupportsFreeformWindowManagement) {
            MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(as.inMultiWindowMode() ? as.mTaskId : as.getRootTaskId());
            showPropotionalFreeformToast(mffas);
        }
    }

    public void onFirstWindowDrawn(ActivityRecord activityRecord) {
        this.mFreeFormGestureController.mMiuiMultiWindowRecommendHelper.onFirstWindowDrawn(activityRecord);
    }

    public int startSmallFreeformFromNotification() {
        return this.mFreeFormGestureController.mMiuiMultiWindowRecommendHelper.startSmallFreeformFromNotification();
    }

    private void showPropotionalFreeformToast(MiuiFreeFormActivityStack mffas) {
        if (mffas != null && !mffas.isNormalFreeForm && mffas.getStackPackageName() != null) {
            String str = this.mApplicationUsedInFreeform;
            if (str != null) {
                String[] applications = str.split(",");
                List<String> applicationsList = Arrays.asList(applications);
                if (!applicationsList.contains(mffas.getStackPackageName())) {
                    String str2 = this.mApplicationUsedInFreeform + "," + mffas.getStackPackageName();
                    this.mApplicationUsedInFreeform = str2;
                    showPropotionalFreeformToast(str2);
                    return;
                }
                return;
            }
            String stackPackageName = mffas.getStackPackageName();
            this.mApplicationUsedInFreeform = stackPackageName;
            showPropotionalFreeformToast(stackPackageName);
        }
    }

    public void showPropotionalFreeformToast(final String applicationUsedInFreeform) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                Toast toast = Toast.makeText(MiuiFreeFormManagerService.this.mActivityTaskManagerService.mContext, MiuiFreeFormManagerService.this.mActivityTaskManagerService.mContext.getString(286196472), 1);
                toast.show();
                MiuiFreeFormManagerService.this.setApplicationUsedInFreeform(applicationUsedInFreeform);
                Slog.d(MiuiFreeFormManagerService.TAG, "show propotional freeform toast");
            }
        });
    }

    private void onMiuiFreeFormStasckAdded(MiuiFreeFormActivityStack stack) {
        this.mFreeFormStackDisplayStrategy.onMiuiFreeFormStasckAdded(this.mFreeFormActivityStacks, this.mFreeFormGestureController, stack);
        setCameraRotationIfNeeded();
    }

    public void setCameraRotationIfNeeded() {
        this.mFreeFormCameraStrategy.rotateCameraIfNeeded();
    }

    public void removeFreeFormActivityStack(Task task, boolean stackRemoved) {
        MiuiFreeFormActivityStack stack;
        if (task == null || !this.mFreeFormActivityStacks.containsKey(Integer.valueOf(task.getRootTaskId())) || (stack = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) == null) {
            return;
        }
        Slog.d(TAG, "removeFreeFormActivityStack as = " + stack);
        task.setAlwaysOnTop(false);
        if (task.getDisplayArea() != null) {
            task.getDisplayArea().positionChildAt(Integer.MIN_VALUE, task, false);
        }
        int windowMode = stack.getMiuiFreeFromWindowMode();
        stack.setStackFreeFormMode(-1);
        if (windowMode == 0) {
            dispatchFreeFormStackModeChanged(3, stack);
        } else if (windowMode == 1) {
            dispatchFreeFormStackModeChanged(5, stack);
        } else if (windowMode == 2) {
            dispatchFreeFormStackModeChanged(16, stack);
        } else if (windowMode == 3) {
            dispatchFreeFormStackModeChanged(17, stack);
        }
        this.mFreeFormActivityStacks.remove(Integer.valueOf(task.getRootTaskId()));
        setCameraRotationIfNeeded();
    }

    public void onActivityStackWindowModeSet(Task rootTask, int mode) {
        if (mode == 5) {
            addFreeFormActivityStack(rootTask, "onActivityStackWindowModeSet");
        } else {
            removeFreeFormActivityStack(rootTask, false);
        }
    }

    public void onActivityStackConfigurationChanged(Task task, int prevWindowingMode, int overrideWindowingMode) {
        if (task.inFreeformWindowingMode()) {
            addFreeFormActivityStack(task, "onActivityStackConfigurationChanged");
        } else if (prevWindowingMode == 5 && overrideWindowingMode != 5) {
            removeFreeFormActivityStack(task, false);
        }
    }

    public MiuiFreeFormActivityStackStub getMiuiFreeFormActivityStack(int taskId) {
        return this.mFreeFormActivityStacks.get(Integer.valueOf(taskId));
    }

    public MiuiFreeFormActivityStack getMiuiFreeFormActivityStackForMiuiFB(int taskId) {
        return this.mFreeFormActivityStacks.get(Integer.valueOf(taskId));
    }

    public MiuiFreeFormActivityStackStub getMiuiFreeFormActivityStack(String packageName, int userId) {
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (mffas != null && mffas.getStackPackageName() != null && mffas.getStackPackageName().equals(packageName) && mffas.mTask.mUserId == userId) {
                return mffas;
            }
        }
        return null;
    }

    public List<MiuiFreeFormActivityStack> getAllMiuiFreeFormActivityStack() {
        List<MiuiFreeFormActivityStack> stackList = new ArrayList<>();
        for (MiuiFreeFormActivityStack stack : this.mFreeFormActivityStacks.values()) {
            stackList.add(stack);
        }
        return stackList;
    }

    public boolean isInFreeForm(String packageName) {
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (mffas != null && mffas.getStackPackageName() != null && mffas.getStackPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public int getFreeFormWindowMode(int rootStackId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(rootStackId);
        if (mffas == null) {
            return -1;
        }
        return mffas.mMiuiFreeFromWindowMode;
    }

    public boolean isAppBehindHome(int rootStackId) {
        return this.mAppBehindHome.contains(Integer.valueOf(rootStackId));
    }

    public MiuiFreeFormActivityStack getReplaceFreeForm(MiuiFreeFormActivityStack addingTask) {
        if (unlockingAppLock(addingTask)) {
            return null;
        }
        boolean hasFreeformModeTask = false;
        boolean hasPinModeTask = false;
        boolean hasMiniFreeformModeTask = false;
        for (MiuiFreeFormActivityStack stack : this.mFreeFormActivityStacks.values()) {
            if (stack != addingTask && stack.getMiuiFreeFromWindowMode() == 0 && stack.isFrontFreeFormStackInfo() && !isAppBehindHome(stack.mTask.mTaskId)) {
                hasFreeformModeTask |= true;
            }
            if (stack != addingTask && stack.inPinMode()) {
                hasPinModeTask |= true;
            }
            if (stack != addingTask && stack.getMiuiFreeFromWindowMode() == 1) {
                hasMiniFreeformModeTask |= true;
            }
        }
        Slog.i(TAG, "getReplaceFreeForm stacks = " + this.mFreeFormActivityStacks.values() + " addingTask: " + addingTask + " hasFreeformModeTask: " + hasFreeformModeTask + " hasPinModeTask: " + hasPinModeTask + " hasMiniFreeformModeTask: " + hasMiniFreeformModeTask);
        Task replacedTask = null;
        if (hasFreeformModeTask) {
            replacedTask = getBottomFreeformTask(addingTask, true, false);
            Slog.i(TAG, "getReplaceFreeForm getBottomFreeformTask  replacedTask = " + replacedTask);
        } else if (hasPinModeTask) {
            replacedTask = getReplacePinModeTask(addingTask);
            Slog.i(TAG, "getReplaceFreeForm getReplacePinModeTask  replacedTask = " + replacedTask);
        } else if (hasMiniFreeformModeTask) {
            replacedTask = getBottomFreeformTask(addingTask, false, true);
        }
        Slog.i(TAG, "getReplaceFreeForm replacedTask = " + replacedTask + ", hasFreeformModeTask=" + hasFreeformModeTask + ", hasPinModeTask=" + hasPinModeTask + ", hasMiniFreeformModeTask=" + hasMiniFreeformModeTask);
        if (replacedTask == null) {
            return null;
        }
        return getMiuiFreeFormActivityStackForMiuiFB(replacedTask.getRootTaskId());
    }

    public int getFrontFreeformNum(MiuiFreeFormActivityStack addingStack) {
        int addingTaskId = addingStack != null ? addingStack.mTask.getRootTaskId() : -1;
        if (addingStack == null || !addingStack.isFrontFreeFormStackInfo() || addingStack.isHideStackFromFullScreen() || isAppBehindHome(addingTaskId)) {
            return -1;
        }
        return getFrontFreeformNum(addingTaskId);
    }

    public int getFrontFreeformNum(int addingTaskId) {
        if (!MiuiDesktopModeUtils.isActive(this.mActivityTaskManagerService.mContext)) {
            return this.mFreeFormActivityStacks.size();
        }
        int frontSize = 0;
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas_temp = this.mFreeFormActivityStacks.get(taskId);
            if (mffas_temp != null && mffas_temp.isFrontFreeFormStackInfo() && !isAppBehindHome(taskId.intValue()) && taskId.intValue() != addingTaskId) {
                frontSize++;
            }
        }
        Slog.d(TAG, "current FrontFreeformNum = " + frontSize + ", addingTaskId=" + addingTaskId);
        return frontSize;
    }

    private MiuiFreeFormActivityStack getFreeformTaskHasAppLock(Task addingTask, String pkg) {
        Task lastVisibleFreeformTask;
        if (addingTask == null || (lastVisibleFreeformTask = getLastVisibleFreeformTask(addingTask)) == null || !applockMatched(lastVisibleFreeformTask, pkg)) {
            return null;
        }
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(lastVisibleFreeformTask.getRootTaskId());
        logd(false, TAG, "get freeformTask with applock: " + mffas);
        return mffas;
    }

    private boolean unlockingAppLock(MiuiFreeFormActivityStack addingTask) {
        Task lastFreeformTask = getLastFreeformTask(addingTask, true, false);
        if (lastFreeformTask == null || addingTask == null) {
            return false;
        }
        return applockMatched(lastFreeformTask, addingTask.getStackPackageName());
    }

    private boolean applockMatched(Task lastFreeformTask, String pkg) {
        ActivityRecord lastFreeformActivity;
        if (lastFreeformTask != null) {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                lastFreeformActivity = lastFreeformTask.getTopActivity(true, true);
            }
            ComponentName lastFreeformComponentName = lastFreeformActivity != null ? lastFreeformActivity.mActivityComponent : null;
            if (lastFreeformComponentName != null && AccessController.APP_LOCK_CLASSNAME.contains(lastFreeformComponentName.flattenToString()) && lastFreeformTask.behindAppLockPkg != null && lastFreeformTask.behindAppLockPkg.equals(pkg)) {
                logd(false, TAG, "unlock: " + lastFreeformTask + " to addingTaskPkg:" + pkg);
                return true;
            }
        }
        return false;
    }

    private Task getLastFreeformTask(final MiuiFreeFormActivityStack addingTask, final boolean findFreeformMode, final boolean findMini) {
        Task lastTask;
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            lastTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda6
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean lambda$getLastFreeformTask$1;
                    lambda$getLastFreeformTask$1 = MiuiFreeFormManagerService.this.lambda$getLastFreeformTask$1(addingTask, findFreeformMode, findMini, (Task) obj);
                    return lambda$getLastFreeformTask$1;
                }
            }, true);
        }
        return lastTask;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$getLastFreeformTask$1(MiuiFreeFormActivityStack addingTask, boolean findFreeformMode, boolean findMini, Task t) {
        MiuiFreeFormActivityStack formActivityStack;
        if (t.inFreeformWindowingMode() && (formActivityStack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()))) != null && formActivityStack != addingTask) {
            if (!formActivityStack.isInFreeFormMode() || !findFreeformMode) {
                if (formActivityStack.isInMiniFreeFormMode() && findMini) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private Task getLastVisibleFreeformTask(final Task addingTask) {
        Task lastVisibleTask;
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            lastVisibleTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda9
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean lambda$getLastVisibleFreeformTask$2;
                    lambda$getLastVisibleFreeformTask$2 = MiuiFreeFormManagerService.this.lambda$getLastVisibleFreeformTask$2(addingTask, (Task) obj);
                    return lambda$getLastVisibleFreeformTask$2;
                }
            }, true);
        }
        return lastVisibleTask;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$getLastVisibleFreeformTask$2(Task addingTask, Task t) {
        MiuiFreeFormActivityStack formActivityStack;
        if (t.inFreeformWindowingMode() && (formActivityStack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()))) != null && isCandidateForAutoLayout(formActivityStack) && formActivityStack.mTask.getRootTaskId() != addingTask.mTaskId) {
            return true;
        }
        return false;
    }

    private Task getBottomFreeformTask(final MiuiFreeFormActivityStack addingTask, final boolean findFreeformMode, final boolean findMini) {
        Task replacedTask;
        Slog.i(TAG, "getBottomFreeformTask addingTask = " + addingTask + " findFreeformMode: " + findFreeformMode + " findMini: " + findMini);
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            replacedTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda12
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean lambda$getBottomFreeformTask$3;
                    lambda$getBottomFreeformTask$3 = MiuiFreeFormManagerService.this.lambda$getBottomFreeformTask$3(addingTask, findFreeformMode, findMini, (Task) obj);
                    return lambda$getBottomFreeformTask$3;
                }
            }, false);
        }
        return replacedTask;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$getBottomFreeformTask$3(MiuiFreeFormActivityStack addingTask, boolean findFreeformMode, boolean findMini, Task t) {
        if (t.inFreeformWindowingMode()) {
            MiuiFreeFormActivityStack formActivityStack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()));
            Slog.i(TAG, "getBottomFreeformTask formActivityStack = " + formActivityStack);
            if (formActivityStack != null && formActivityStack != addingTask && formActivityStack.isFrontFreeFormStackInfo() && !this.mAppBehindHome.contains(Integer.valueOf(t.getRootTaskId()))) {
                if (!formActivityStack.isInFreeFormMode() || !findFreeformMode) {
                    if (formActivityStack.isInMiniFreeFormMode() && findMini) {
                        return true;
                    }
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private Task getReplacePinModeTask(MiuiFreeFormActivityStack addingTask) {
        MiuiFreeFormActivityStack replacedPinTask = null;
        for (MiuiFreeFormActivityStack stack : this.mFreeFormActivityStacks.values()) {
            if (stack.inPinMode() && stack != addingTask && (replacedPinTask == null || replacedPinTask.pinActiveTime > stack.pinActiveTime)) {
                replacedPinTask = stack;
            }
        }
        if (replacedPinTask == null) {
            return null;
        }
        return replacedPinTask.mTask;
    }

    public int getGameFreeFormCount(MiuiFreeFormActivityStack addingStack) {
        int existingGameFreeform = 0;
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (taskId.intValue() != addingStack.mTask.mTaskId && MiuiMultiWindowAdapter.isInTopGameList(mffas.getStackPackageName()) && mffas.mIsFrontFreeFormStackInfo && mffas.isFrontFreeFormStackInfo() && !this.mAppBehindHome.contains(taskId)) {
                existingGameFreeform++;
            }
        }
        Slog.i(TAG, "existingGameFreeform=" + existingGameFreeform);
        return existingGameFreeform;
    }

    public MiuiFreeFormActivityStack getBottomGameFreeFormActivityStack(final MiuiFreeFormActivityStack addingStack) {
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            Task bottomTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda10
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean lambda$getBottomGameFreeFormActivityStack$4;
                    lambda$getBottomGameFreeFormActivityStack$4 = MiuiFreeFormManagerService.this.lambda$getBottomGameFreeFormActivityStack$4(addingStack, (Task) obj);
                    return lambda$getBottomGameFreeFormActivityStack$4;
                }
            }, false);
            if (bottomTask == null) {
                return null;
            }
            return getMiuiFreeFormActivityStackForMiuiFB(bottomTask.getRootTaskId());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$getBottomGameFreeFormActivityStack$4(MiuiFreeFormActivityStack addingStack, Task t) {
        MiuiFreeFormActivityStack curFreeFormActivityStack;
        return t.inFreeformWindowingMode() && (curFreeFormActivityStack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()))) != null && curFreeFormActivityStack != addingStack && MiuiMultiWindowAdapter.isInTopGameList(curFreeFormActivityStack.getStackPackageName()) && curFreeFormActivityStack.isFrontFreeFormStackInfo() && !this.mAppBehindHome.contains(Integer.valueOf(t.getRootTaskId()));
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getAllFreeFormStackInfosOnDisplay(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
            if (-1 == displayId) {
                for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
                    MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
                    if (mffas != null && MiuiFreeFormManager.isFrontFreeFormStackInfo(taskId.intValue()) && !MiuiFreeFormManager.isHideStackFromFullScreen(taskId.intValue()) && !this.mAppBehindHome.contains(taskId)) {
                        list.add(mffas.getMiuiFreeFormStackInfo());
                    }
                }
                logd(TAG, "getAllFreeFormStackInfosOnDisplay INVALID_DISPLAY list=" + list);
                return list;
            }
            for (Integer taskId2 : this.mFreeFormActivityStacks.keySet()) {
                MiuiFreeFormActivityStack mffas2 = this.mFreeFormActivityStacks.get(taskId2);
                if (mffas2 != null && mffas2.mTask.getDisplayId() == displayId && MiuiFreeFormManager.isFrontFreeFormStackInfo(taskId2.intValue()) && !MiuiFreeFormManager.isHideStackFromFullScreen(taskId2.intValue()) && !this.mAppBehindHome.contains(taskId2)) {
                    list.add(mffas2.getMiuiFreeFormStackInfo());
                }
            }
            logd(TAG, "getAllFreeFormStackInfosOnDisplay list=" + list.size());
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getAllFrontFreeFormStackInfosOnDesktopMode(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
            if (-1 == displayId) {
                for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
                    MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
                    if (mffas != null && MiuiFreeFormManager.isFrontFreeFormStackInfo(taskId.intValue()) && !MiuiFreeFormManager.isHideStackFromFullScreen(taskId.intValue())) {
                        list.add(mffas.getMiuiFreeFormStackInfo());
                    }
                }
                return list;
            }
            for (Integer taskId2 : this.mFreeFormActivityStacks.keySet()) {
                MiuiFreeFormActivityStack mffas2 = this.mFreeFormActivityStacks.get(taskId2);
                if (mffas2 != null && mffas2.mTask.getDisplayId() == displayId && MiuiFreeFormManager.isFrontFreeFormStackInfo(taskId2.intValue()) && !MiuiFreeFormManager.isHideStackFromFullScreen(taskId2.intValue())) {
                    list.add(mffas2.getMiuiFreeFormStackInfo());
                }
            }
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getAllPinedFreeFormStackInfosOnDisplay(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
            if (-1 == displayId) {
                for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
                    MiuiFreeFormActivityStack mffas = entry.getValue();
                    if (mffas != null && mffas.inPinMode()) {
                        list.add(mffas.getMiuiFreeFormStackInfo());
                    }
                }
                return list;
            }
            for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry2 : this.mFreeFormActivityStacks.entrySet()) {
                MiuiFreeFormActivityStack mffas2 = entry2.getValue();
                if (mffas2 != null && mffas2.mTask.getDisplayId() == displayId && mffas2.inPinMode()) {
                    list.add(mffas2.getMiuiFreeFormStackInfo());
                }
            }
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByActivity(IBinder token) {
        MiuiFreeFormActivityStack mffas;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null && (mffas = getMiuiFreeFormActivityStackForMiuiFB(r.getRootTaskId())) != null) {
                    return mffas.getMiuiFreeFormStackInfo();
                }
                Binder.restoreCallingIdentity(ident);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByWindow(IBinder wtoken) {
        MiuiFreeFormActivityStack mffas;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                WindowState win = this.mActivityTaskManagerService.mWindowManager.windowForClientLocked((Session) null, wtoken, false);
                if (win == null || win.mActivityRecord == null || (mffas = getMiuiFreeFormActivityStackForMiuiFB(win.mActivityRecord.getRootTaskId())) == null) {
                    return null;
                }
                return mffas.getMiuiFreeFormStackInfo();
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByStackId(int stackId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(stackId);
                if (mffas != null) {
                    return mffas.getMiuiFreeFormStackInfo();
                }
                Binder.restoreCallingIdentity(ident);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void dispatchFreeFormStackModeChanged(int action, int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        for (int i = 0; i <= 21; i++) {
            int mask = 1 << i;
            if ((action & mask) != 0) {
                dispatchFreeFormStackModeChanged(i, mffas);
            }
        }
    }

    public void dispatchFreeFormStackModeChanged(final int action, final MiuiFreeFormActivityStack mffas) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$dispatchFreeFormStackModeChanged$6(mffas, action);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$dispatchFreeFormStackModeChanged$6(MiuiFreeFormActivityStack mffas, final int action) {
        if (mffas == null) {
            return;
        }
        final MiuiFreeFormManager.MiuiFreeFormStackInfo freeFormASInfo = mffas.getMiuiFreeFormStackInfo();
        logd(true, TAG, "action: " + action + " dispatchFreeFormStackModeChanged freeFormASInfo = " + freeFormASInfo);
        if (mffas.timestamp == 0) {
            mffas.setFreeformTimestamp(System.currentTimeMillis());
        }
        synchronized (this.mCallbacks) {
            int callbacksCount = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacksCount; i++) {
                IFreeformCallback freeformCallback = this.mCallbacks.getBroadcastItem(i);
                try {
                    freeformCallback.dispatchFreeFormStackModeChanged(action, freeFormASInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mCallbacks.finishBroadcast();
        }
        if (mffas.mTask != null) {
            synchronized (mffas.mTask.mWmService.mGlobalLock) {
                mffas.mTask.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).dispatchFreeFormStackModeChanged(action, freeFormASInfo);
                    }
                }, true);
            }
            if (action == 0) {
                this.mFreeFormGestureController.deliverDestroyItemToAlipay(mffas);
            }
        }
    }

    public void registerFreeformCallback(IFreeformCallback freeformCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.register(freeformCallback);
        }
    }

    public void unregisterFreeformCallback(IFreeformCallback freeformCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.unregister(freeformCallback);
        }
    }

    public String getApplicationUsedInFreeform(Context context) {
        return Settings.Secure.getStringForUser(context.getContentResolver(), "application_used_freeform", -2);
    }

    public void setApplicationUsedInFreeform(String applicationUsedInFreeform) {
        long origId = Binder.clearCallingIdentity();
        try {
            setApplicationUsedInFreeform(applicationUsedInFreeform, this.mActivityTaskManagerService.mWindowManager.mContext);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setApplicationUsedInFreeform(String applicationUsedInFreeform, Context context) {
        Settings.Secure.putStringForUser(context.getContentResolver(), "application_used_freeform", applicationUsedInFreeform, -2);
    }

    public void moveTaskToFront(Task task, ActivityOptions options) {
        MiuiFreeFormActivityStack mffas;
        Rect bounds;
        if (task != null && task.inFreeformWindowingMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) != null) {
            removeFullScreenTasksBehindHome(task.mTaskId);
            if ((!task.isVisible() || !mffas.isFrontFreeFormStackInfo()) && !mffas.inPinMode() && options != null && (bounds = options.getLaunchBounds()) != null && !bounds.isEmpty()) {
                task.setBounds(bounds);
            }
            mffas.setIsFrontFreeFormStackInfo(true);
            if (mffas.isHideStackFromFullScreen()) {
                showFreeformIfNeeded(task.getRootTaskId());
            }
        }
    }

    public void unPinFloatingWindowForActive(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$unPinFloatingWindowForActive$7(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$unPinFloatingWindowForActive$7(int taskId) {
        try {
            this.mFreeformModeControl.unPinFloatingWindowForActive(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restoreMiniToFreeformMode(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$restoreMiniToFreeformMode$8(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreMiniToFreeformMode$8(int taskId) {
        try {
            this.mFreeformModeControl.restoreMiniToFreeformMode(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void allFreeformFormFreefromToMini() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$allFreeformFormFreefromToMini$9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$allFreeformFormFreefromToMini$9() {
        try {
            this.mFreeformModeControl.allFreeformFormFreefromToMini();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fromFreefromToMini(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$fromFreefromToMini$10(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$fromFreefromToMini$10(int taskId) {
        try {
            this.mFreeformModeControl.fromFreefromToMini(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onExitFreeform(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$onExitFreeform$11(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onExitFreeform$11(int taskId) {
        try {
            this.mFreeformModeControl.onExitFreeform(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRequestedOrientation(final int requestedOrientation, final Task task, boolean noAnimation) {
        Slog.d(TAG, "setRequestedOrientation task: " + task + " requestedOrientation: " + requestedOrientation);
        if (this.mHandler == null || task == null) {
            return;
        }
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas != null) {
            mffas.mIsLandcapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(requestedOrientation);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$setRequestedOrientation$12(task, requestedOrientation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setRequestedOrientation$12(Task task, int requestedOrientation) {
        try {
            this.mFreeformModeControl.setRequestedOrientation(task.getRootTaskId(), requestedOrientation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyImeVisibilityChanged(boolean imeVisible, int imeHeight, boolean adjustedForRotation) {
        IMiuiFreeformModeControl iMiuiFreeformModeControl = this.mFreeformModeControl;
        if (iMiuiFreeformModeControl != null) {
            try {
                iMiuiFreeformModeControl.onImeVisibilityChanged(imeVisible, imeHeight, adjustedForRotation);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering bounds changed event.", e);
            }
        }
    }

    public void setMiuiFreeFormTouchExcludeRegion(Region region, Region region2) {
        IMiuiFreeformModeControl iMiuiFreeformModeControl = this.mFreeformModeControl;
        if (iMiuiFreeformModeControl != null) {
            try {
                iMiuiFreeformModeControl.setMiuiFreeFormTouchExcludeRegion(region, region2);
            } catch (RemoteException e) {
                Slog.e(TAG, "setMiuiFreeFormTouchExcludeRegion.", e);
            }
        }
    }

    public void fromSoscToFreeform(int taskId) {
        IMiuiFreeformModeControl iMiuiFreeformModeControl = this.mFreeformModeControl;
        if (iMiuiFreeformModeControl != null) {
            try {
                iMiuiFreeformModeControl.fromSoscToFreeform(taskId);
            } catch (RemoteException e) {
                Slog.e(TAG, "fromSoscToFreeform.", e);
            }
        }
    }

    public boolean skipStartActivity(Task reusedTask, ActivityOptions activityOption, ActivityRecord activityRecord, ActivityRecord sourceRecord, TaskDisplayArea taskDisplayArea) {
        if (reusedTask != null && MiuiSoScManagerStub.get().isInSoScMode(reusedTask) && activityOption != null && activityOption.getLaunchWindowingMode() == 5) {
            fromSoscToFreeform(reusedTask.mTaskId);
            Slog.i(TAG, "Skip start activity from sosc to freeform.");
            return true;
        }
        Task sourceTask = sourceRecord != null ? sourceRecord.getTask() : null;
        if (reusedTask != null && reusedTask.inFreeformWindowingMode() && reusedTask.isMiuiFreeformExiting() && activityOption != null && MiuiSoScManagerStub.get().validateWindowingModeForSoSc(activityOption.getLaunchWindowingMode(), activityRecord, sourceTask, taskDisplayArea) == 6) {
            Slog.i(TAG, "Skip start activity via freeform is exiting");
            return true;
        }
        return activeOrFullscreenFreeformTaskIfNeed(reusedTask, activityOption, sourceRecord, activityRecord);
    }

    public Task adjustedReuseableFreeformTask(ActivityRecord startActivity) {
        ActivityRecord r;
        String pkgBehindIntentActivity;
        List<Task> list = new ArrayList<>();
        for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
            MiuiFreeFormActivityStack mffas = entry.getValue();
            if (mffas != null && mffas.inPinMode() && mffas.mTask != null) {
                ComponentName cls = startActivity.intent.getComponent();
                if (!ConfigurationContainer.isCompatibleActivityType(startActivity.mAnimationType, mffas.mTask.getActivityType()) || mffas.mTask.mUserId != startActivity.mUserId || (r = mffas.mTask.getTopNonFinishingActivity(false)) == null || r.finishing || r.mUserId != startActivity.mUserId || r.launchMode == 3 || !ConfigurationContainer.isCompatibleActivityType(r.getActivityType(), startActivity.mAnimationType)) {
                    break;
                }
                if ((mffas.mTask.realActivity != null && mffas.mTask.realActivity.compareTo(cls) == 0) || ((mffas.mTask.affinityIntent != null && mffas.mTask.affinityIntent.getComponent() != null && mffas.mTask.affinityIntent.getComponent().compareTo(cls) == 0) || (mffas.mTask.rootAffinity != null && mffas.mTask.rootAffinity.equals(startActivity.taskAffinity)))) {
                    if (!MiuiMultiWindowAdapter.isAppLockActivity(startActivity.mActivityComponent, cls) || (pkgBehindIntentActivity = startActivity.intent.getStringExtra("android.intent.extra.shortcut.NAME")) == null || pkgBehindIntentActivity.equals(mffas.getStackPackageName())) {
                        list.add(mffas.mTask);
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /* renamed from: getSchemeLauncherTask, reason: merged with bridge method [inline-methods] */
    public MiuiFreeFormActivityStack m2531getSchemeLauncherTask() {
        for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
            MiuiFreeFormActivityStack mffas = entry.getValue();
            if (mffas != null && mffas.mTask != null && mffas.mTask.intent != null && mffas.mTask.intent.getComponent() != null && mffas.mTask.intent.getComponent().getShortClassName().equals("com.alipay.mobile.quinox.SchemeLauncherActivity")) {
                return mffas;
            }
        }
        return null;
    }

    public String getStackPackageName(Task targetTask) {
        if (targetTask == null) {
            return null;
        }
        if (targetTask.origActivity != null) {
            return targetTask.origActivity.getPackageName();
        }
        if (targetTask.realActivity != null) {
            return targetTask.realActivity.getPackageName();
        }
        if (targetTask.getTopActivity(false, true) == null) {
            return null;
        }
        return targetTask.getTopActivity(false, true).packageName;
    }

    public boolean shouldForegroundPin(int taskId) {
        MiuiFreeFormActivityStack stack = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (stack == null || stack.mTask == null) {
            return false;
        }
        stack.mIsForegroundPin = this.mFreeFormGestureController.needForegroundPin(stack);
        return stack.mIsForegroundPin;
    }

    public void notifyCameraStateChanged(String packageName, int cameraState) {
        MiuiFreeFormCameraStrategy miuiFreeFormCameraStrategy = this.mFreeFormCameraStrategy;
        int i = 1;
        if (cameraState != 1) {
            i = 0;
        }
        miuiFreeFormCameraStrategy.onCameraStateChanged(packageName, i);
    }

    public boolean openCameraInFreeForm(String packageName) {
        return this.mFreeFormCameraStrategy.openCameraInFreeForm(packageName);
    }

    static void logd(String tag, String string) {
        logd(true, tag, string);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logd(boolean enable, String tag, String string) {
        if (MiuiFreeFormGestureController.DEBUG || enable) {
            Slog.d(tag, string);
        }
    }

    public void setCameraOrientation(int newRotation) {
        this.mFreeFormCameraStrategy.setCameraOrientation(newRotation);
    }

    public void hideCallingTaskIfAddSpecificChild(WindowContainer child) {
        if (child != null && child.asActivityRecord() != null && child.asActivityRecord().getTask() != null && child.asActivityRecord().intent != null && child.asActivityRecord().intent.getComponent() != null && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(child.asActivityRecord().intent.getComponent().getClassName())) {
            for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
                if (ffas != null && ffas.mTask != null && ffas.mTask.intent != null && ffas.mTask.intent.getComponent() != null && ffas.mTask.intent.getComponent().getPackageName() != null && ffas.mTask.intent.getComponent().getPackageName().equals(child.asActivityRecord().getTask().mCallingPackage) && ffas.mTask.getResumedActivity() != null && ffas.mTask.getResumedActivity().intent != null && ffas.mTask.getResumedActivity().intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(ffas.mTask.getResumedActivity().intent.getComponent().getClassName())) {
                    ffas.mTask.getSyncTransaction().hide(ffas.mTask.getSurfaceControl());
                }
            }
        }
    }

    public void showHiddenTaskIfRemoveSpecificChild(WindowContainer child) {
        if (child != null && child.asActivityRecord() != null && child.asActivityRecord().getTask() != null && child.asActivityRecord().intent != null && child.asActivityRecord().intent.getComponent() != null && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(child.asActivityRecord().intent.getComponent().getClassName())) {
            for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
                if (ffas != null && ffas.mTask != null && ffas.mTask.intent != null && ffas.mTask.intent.getComponent() != null && ffas.mTask.intent.getComponent().getPackageName() != null && ffas.mTask.intent.getComponent().getPackageName().equals(child.asActivityRecord().getTask().mCallingPackage) && ffas.mTask.getTopNonFinishingActivity() != null && ffas.mTask.getTopNonFinishingActivity().intent != null && ffas.mTask.getTopNonFinishingActivity().intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(ffas.mTask.getTopNonFinishingActivity().intent.getComponent().getClassName())) {
                    ffas.mTask.getSyncTransaction().show(ffas.mTask.getSurfaceControl());
                }
            }
        }
    }

    public void showOpenMiuiOptimizationToast() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService.2
            @Override // java.lang.Runnable
            public void run() {
                Toast toast = Toast.makeText(MiuiFreeFormManagerService.this.mActivityTaskManagerService.mContext, MiuiFreeFormManagerService.this.mActivityTaskManagerService.mContext.getString(286196427), 0);
                toast.show();
            }
        });
    }

    public void traverseTask(Task task, Consumer<ActivityRecord> consumer) {
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            if (task == null) {
                return;
            }
            int candidate = -2;
            for (int activityIndex = task.getChildCount() - 1; activityIndex >= 0; activityIndex--) {
                ActivityRecord activityRecord = task.getChildAt(activityIndex).asActivityRecord();
                if (activityRecord != null) {
                    int orientation = activityRecord.getOrientation(candidate);
                    candidate = orientation;
                    if (orientation != -1 && orientation != 13 && orientation != 10 && orientation != 4 && orientation != 2 && orientation != 14) {
                        if (orientation != 3 && orientation != -2) {
                            consumer.accept(activityRecord);
                            return;
                        }
                    }
                    return;
                }
            }
        }
    }

    public boolean isWpsPreStartActivity(String shortComponentName) {
        return "cn.wps.moffice_eng/cn.wps.moffice.documentmanager.PreStartActivity".equals(shortComponentName);
    }

    public boolean isWpsSameActivity(ComponentName componentName, ComponentName otherComponentName) {
        return MiuiMultiWindowAdapter.isWpsSameActivity(componentName, otherComponentName);
    }

    public boolean disableSplashScreenForWpsInFreeForm(ComponentName componentName, int userId) {
        if (componentName == null || !"cn.wps.moffice_eng/cn.wps.moffice.documentmanager.PreStartActivity".equals(componentName.flattenToShortString()) || getMiuiFreeFormActivityStack(componentName.getPackageName(), userId) == null) {
            return false;
        }
        return true;
    }

    public void addFreeFormActivityStackFromStartSmallFreeform(Task task, int cornerPosition, String reason, Rect fromRect) {
        addFreeFormActivityStack(task, 1, "FromStartSmallFreeform");
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        mffas.setCornerPosition(cornerPosition);
        mffas.setEnterMiniFreeformReason(reason);
        mffas.setEnterMiniFreeformRect(fromRect);
        if (!mffas.mTask.mTaskSupervisor.getLaunchParamsController().hasFreeformDesktopMemory(task)) {
            mffas.mFreeFormScale = MiuiMultiWindowUtils.getOriFreeformScale(task.mAtmService.mContext, mffas.mIsLandcapeFreeform, mffas.isNormalFreeForm, MiuiDesktopModeUtils.isActive(task.mAtmService.mContext), mffas.getStackPackageName());
        } else {
            mffas.mFreeFormScale = mffas.mTask.mTaskSupervisor.getLaunchParamsController().getFreeformLastScale(task);
        }
        task.setAlwaysOnTop(true);
    }

    public void onATMSSystemReady() {
        this.mFreeFormGestureController.onATMSSystemReady();
    }

    public Rect getMiniFreeformBounds(int taskId, Rect stableBounds, boolean rotated) {
        int toPosX;
        int toPosY;
        Rect miniFreeformBounds = new Rect();
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            RectF possibleBounds = MiuiFreeformStub.getInstance().getPossibleBounds(this.mActivityTaskManagerService.mContext, !rotated, mffas.mIsLandcapeFreeform, mffas.getStackPackageName(), mffas.isNormalFreeForm);
            miniFreeformBounds.set((int) possibleBounds.left, (int) possibleBounds.top, (int) possibleBounds.right, (int) possibleBounds.bottom);
            float miniScale = MiuiMultiWindowUtils.getMiniFreeformScale(this.mActivityTaskManagerService.mContext, mffas.isLandcapeFreeform(), miniFreeformBounds, mffas.getStackPackageName());
            int margin = (int) (MiuiMultiWindowUtils.isPadScreen(this.mActivityTaskManagerService.mContext) ? MiuiMultiWindowUtils.ACCESSABLE_MARGIN_DIP_PAD : MiuiMultiWindowUtils.ACCESSABLE_MARGIN_DIP);
            stableBounds.inset(margin + 20, 20);
            if (mffas.mCornerPosition == 1) {
                toPosX = stableBounds.left;
                toPosY = stableBounds.top;
            } else {
                int toPosX2 = stableBounds.right;
                toPosX = toPosX2 - ((int) (miniFreeformBounds.width() * miniScale));
                toPosY = stableBounds.top;
            }
            miniFreeformBounds.offsetTo(toPosX, toPosY);
        }
        Slog.d(TAG, " getMiniFreeformBounds taskId: " + taskId + " stableBounds: " + stableBounds + " rotated: " + rotated + " miniFreeformBounds: " + miniFreeformBounds);
        return miniFreeformBounds;
    }

    public void onActivityStackFirstActivityRecordAdded(int rootId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(rootId);
        if (mffas == null) {
            return;
        }
        if (mffas.mShouldDelayDispatchFreeFormStackModeChanged) {
            mffas.mShouldDelayDispatchFreeFormStackModeChanged = false;
            dispatchFreeFormStackModeChanged(mffas.isInFreeFormMode() ? 0 : 1, mffas);
        }
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null) {
            this.mFreeFormStackDisplayStrategy.onMiuiFreeFormStasckAdded(this.mFreeFormActivityStacks, miuiFreeFormGestureController, mffas);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackToAvoid(final int displayId, String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
                if (ffas.isInMiniFreeFormMode() || ffas.inPinMode()) {
                    return null;
                }
            }
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                Task topTaskToAvoid = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean lambda$getFreeFormStackToAvoid$13;
                        lambda$getFreeFormStackToAvoid$13 = MiuiFreeFormManagerService.this.lambda$getFreeFormStackToAvoid$13(displayId, (Task) obj);
                        return lambda$getFreeFormStackToAvoid$13;
                    }
                }, true);
                if (topTaskToAvoid == null) {
                    return null;
                }
                return getMiuiFreeFormActivityStackForMiuiFB(topTaskToAvoid.getRootTaskId()).getMiuiFreeFormStackInfo();
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$getFreeFormStackToAvoid$13(int displayId, Task t) {
        if (t.inFreeformWindowingMode()) {
            MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId());
            boolean notAvoid = (mffas == null || mffas.mTask == null || mffas.mTask.realActivity == null || !MiuiMultiWindowAdapter.NOT_AVOID_LAUNCH_OTHER_FREEFORM_LIST.contains(mffas.mTask.realActivity.getClassName()) || AccessController.APP_LOCK_CLASSNAME.equals(mffas.mTask.realActivity.flattenToShortString())) ? false : true;
            if (mffas != null && ((-1 == displayId || mffas.mTask.getDisplayId() == displayId) && !mffas.inPinMode() && !notAvoid)) {
                return true;
            }
        }
        return false;
    }

    public int autoLayoutFreeFormStackIfNeed(Rect outBounds, float scale, Rect accessibleArea, Task excludedTask, String pkg, MiuiFreeFormActivityStack freeformTaskHasAppLock) {
        boolean isDisplayLandscape = accessibleArea.width() > accessibleArea.height();
        int remainingSpace = isDisplayLandscape ? accessibleArea.width() : accessibleArea.height();
        int gap = (int) (MiuiMultiWindowUtils.applyDip2Px(20.0f) + 0.5f);
        int frontNum = 0;
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            if (taskId.intValue() != excludedTask.mTaskId) {
                MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
                if (isCandidateForAutoLayout(mffas)) {
                    remainingSpace = (remainingSpace - MiuiMultiWindowUtils.getVisualWidthOrHeight(isDisplayLandscape, mffas.mTask.getBounds(), mffas.mFreeFormScale)) - gap;
                    frontNum++;
                    Slog.d(TAG, "auto layout: CandidateForAutoLayout: " + taskId + " remainingSpace: " + remainingSpace);
                }
            }
        }
        int freeformNum = getFrontFreeformNum(excludedTask.mTaskId);
        boolean replaceFreeForm = freeformNum >= 4;
        if (frontNum == 1 && replaceFreeForm) {
            Slog.d(TAG, "auto layout: There is only one freeform at the front desk and it will be replaced");
            frontNum--;
        }
        if (frontNum == 0 || isAutoLayoutModeOn()) {
            if (freeformTaskHasAppLock != null) {
                remainingSpace = remainingSpace + gap + MiuiMultiWindowUtils.getVisualWidthOrHeight(isDisplayLandscape, freeformTaskHasAppLock.mTask.getBounds(), freeformTaskHasAppLock.mFreeFormScale);
                removeFreeformParamsForAutoLayout(freeformTaskHasAppLock.mTask.getRootTaskId());
                Slog.d(TAG, "auto layout: found app lock is going to exit freeform: increase left space to " + remainingSpace);
            } else if (replaceFreeForm) {
                MiuiFreeFormActivityStack excludedMffas = getMiuiFreeFormActivityStackForMiuiFB(excludedTask.mTaskId);
                MiuiFreeFormActivityStack subMffas = getReplaceFreeForm(excludedMffas);
                if (isCandidateForAutoLayout(subMffas)) {
                    remainingSpace = remainingSpace + gap + MiuiMultiWindowUtils.getVisualWidthOrHeight(isDisplayLandscape, subMffas.mTask.getBounds(), subMffas.mFreeFormScale);
                    removeFreeformParamsForAutoLayout(subMffas.mTask.getRootTaskId());
                    Slog.d(TAG, "auto layout: found one is going to exit freeform: increase left space to " + remainingSpace);
                }
            }
            int freeformPlusGapSpace = isDisplayLandscape ? accessibleArea.width() - remainingSpace : accessibleArea.height() - remainingSpace;
            int remainingSpace2 = remainingSpace - MiuiMultiWindowUtils.getVisualWidthOrHeight(isDisplayLandscape, outBounds, scale);
            Slog.d(TAG, "auto layout: left space = " + remainingSpace2 + " freeformPlusGapSpace: " + freeformPlusGapSpace + " outBounds: " + outBounds);
            if (remainingSpace2 >= 0) {
                int margin = (int) ((remainingSpace2 / 2) + 0.5f);
                int startPoint = (isDisplayLandscape ? accessibleArea.left + margin : accessibleArea.top + margin) + freeformPlusGapSpace;
                updateAutoLayoutModeStatus(true);
                Slog.d(TAG, "auto layout: left start point = " + startPoint);
                return startPoint;
            }
            return -1;
        }
        return -1;
    }

    private boolean isCandidateForAutoLayout(MiuiFreeFormActivityStack mffas) {
        return mffas != null && mffas.isFrontFreeFormStackInfo() && mffas.mTask.inFreeformWindowingMode() && mffas.isInFreeFormMode() && !isAppBehindHome(mffas.mTask.getRootTaskId());
    }

    private void centerRect(Rect outBounds, Rect accessibleArea, boolean isDisplayLandscape, int startPoint) {
        if (isDisplayLandscape) {
            outBounds.offset(0, accessibleArea.centerY() - outBounds.centerY());
            outBounds.offsetTo(startPoint, outBounds.top);
        } else {
            outBounds.offset(accessibleArea.centerX() - outBounds.centerX(), 0);
            outBounds.offsetTo(outBounds.left, startPoint);
        }
        Slog.d(TAG, "centerRect to (" + outBounds.left + ", " + outBounds.top + ")");
    }

    private void removeFreeformParamsForAutoLayout(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$removeFreeformParamsForAutoLayout$14(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeFreeformParamsForAutoLayout$14(int taskId) {
        try {
            this.mFreeformModeControl.removeFreeformParamsForAutoLayout(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float scaleDownIfNeeded(float scale, Rect bounds, Rect stableBounds) {
        float currentVisualWidth = bounds.width() * scale;
        float currentVisualHeight = bounds.height() * scale;
        if (currentVisualWidth <= stableBounds.width() && currentVisualHeight <= stableBounds.height()) {
            return scale;
        }
        float scaleDown = Math.min(stableBounds.width() / currentVisualWidth, stableBounds.height() / currentVisualHeight);
        Slog.d(TAG, "scaleDownIfNeeded scaleDown: " + scaleDown + " currentVisualWidth: " + currentVisualWidth + " currentVisualHeight: " + currentVisualHeight + " stableBounds: " + stableBounds + " scale: " + scale);
        float finalScale = scale * scaleDown;
        return finalScale;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x00c8  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0112  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x0152  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x015f  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0171  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x0188  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public float getFreeformRectDesktop(android.graphics.Rect r20, float r21, int r22, boolean r23) {
        /*
            Method dump skipped, instructions count: 487
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormManagerService.getFreeformRectDesktop(android.graphics.Rect, float, int, boolean):float");
    }

    public void displayConfigurationChange(DisplayContent displayContent, Configuration configuration) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null && miuiFreeFormGestureController.mMiuiMultiWindowRecommendHelper != null) {
            this.mFreeFormGestureController.mMiuiMultiWindowRecommendHelper.displayConfigurationChange(displayContent, configuration);
            this.mFreeFormGestureController.displayConfigurationChange(displayContent, configuration);
        }
    }

    public void adjustFreeformTouchRegion(Rect inOutRect, int taskId) {
        if (MiuiFreeformUtilStub.getInstance().isPadScreen(this.mActivityTaskManagerService.mContext)) {
            float freefromScale = getFreeformScale(taskId);
            if (getFreeFormWindowMode(taskId) == 0) {
                inOutRect.inset(((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceBottomOffsetPad() / freefromScale)) * (-1));
                return;
            } else {
                if (getFreeFormWindowMode(taskId) == 1) {
                    inOutRect.inset(((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale)) * (-1), 0, ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceBottomOffsetPad() / freefromScale)) * (-1));
                    return;
                }
                return;
            }
        }
        float freefromScale2 = getFreeformScale(taskId);
        if (getFreeFormWindowMode(taskId) == 0) {
            inOutRect.inset(((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale2)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale2)) * (-1));
        } else if (getFreeFormWindowMode(taskId) == 1) {
            inOutRect.inset(((int) Math.ceil((MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() + MiuiFreeformUtilStub.getInstance().getMiniFreeformPaddingStroke()) / freefromScale2)) * (-1), 0, ((int) Math.ceil((MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() + MiuiFreeformUtilStub.getInstance().getMiniFreeformPaddingStroke()) / freefromScale2)) * (-1), ((int) Math.ceil(MiuiFreeformUtilStub.getInstance().getHotSpaceOffset() / freefromScale2)) * (-1));
        }
    }

    public void activeFreeformTask(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            if (mffas.inPinMode()) {
                mffas.setStackFreeFormMode(0);
                unPinFloatingWindowForActive(taskId);
            } else if (mffas.isInMiniFreeFormMode()) {
                mffas.setStackFreeFormMode(0);
                restoreMiniToFreeformMode(taskId);
            }
        }
    }

    public void fullscreenFreeformTask(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$fullscreenFreeformTask$15(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$fullscreenFreeformTask$15(int taskId) {
        try {
            this.mFreeformModeControl.fullscreenFreeformTask(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void freeformFullscreenTask(final int taskId) {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$freeformFullscreenTask$16(taskId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$freeformFullscreenTask$16(int taskId) {
        try {
            this.mFreeformModeControl.freeformFullscreenTask(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void freeformKillAll() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormManagerService.this.lambda$freeformKillAll$17();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$freeformKillAll$17() {
        try {
            this.mFreeformModeControl.freeformKillAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean activeOrFullscreenFreeformTaskIfNeed(Task reusedTask, ActivityOptions options, ActivityRecord sourceRecord, ActivityRecord startingAr) {
        boolean startForSosc = false;
        boolean startForFullscreen = false;
        boolean startDefaultAr = false;
        if (options != null && isLaunchForSplitMode(Task.fromWindowContainerToken(options.getLaunchRootTask()))) {
            startForSosc = true;
        }
        if (options != null && options.getLaunchWindowingMode() == 1) {
            startForFullscreen = true;
        }
        if (startingAr != null && startingAr.intent != null) {
            startDefaultAr = ActivityRecord.isMainIntent(startingAr.intent);
            Slog.d(TAG, "activeOrFullscreenFreeformTaskIfNeed startDefaultAr:" + startDefaultAr);
        }
        if (!startForSosc && reusedTask != null && reusedTask.inFreeformWindowingMode()) {
            if (sourceRecord == null || !sourceRecord.inFreeformWindowingMode()) {
                dispatchFreeFormStackModeChanged(1048576, reusedTask.mTaskId);
                if ((inPinMode(reusedTask.mTaskId) || isInMiniFreeFormMode(reusedTask.mTaskId)) && !startForFullscreen && startDefaultAr) {
                    activeFreeformTask(reusedTask.mTaskId);
                    Slog.i(TAG, "activFreeformTask " + reusedTask.getPackageName() + " is in pinmode or minifreeform, so just active it!");
                    return true;
                }
                if (startForFullscreen) {
                    fullscreenFreeformTask(reusedTask.mTaskId);
                    Slog.i(TAG, "fullscreenFreeformTask " + reusedTask.getPackageName() + " from freeform to fullscreen for options.getLaunchWindowingMode() == WINDOWING_MODE_FULLSCREEN");
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public boolean activeFreeformTaskIfNeed(int taskId, ActivityOptions activityOptions, RootWindowContainer rootWindowContainer) {
        boolean startForFullscreen = false;
        Task preFindTask = rootWindowContainer.anyTaskForId(taskId, 0);
        boolean startForSosc = false;
        if (activityOptions != null && isLaunchForSplitMode(Task.fromWindowContainerToken(activityOptions.getLaunchRootTask()))) {
            startForSosc = true;
        }
        if (activityOptions != null && activityOptions.getLaunchWindowingMode() == 1) {
            startForFullscreen = true;
        }
        if (preFindTask == null || !preFindTask.inFreeformWindowingMode() || startForSosc) {
            return false;
        }
        if (startForFullscreen) {
            fullscreenFreeformTask(taskId);
            Slog.i(TAG, "fullscreenFreeformTask " + preFindTask.getPackageName() + " from freeform to fullscreen for options.getLaunchWindowingMode() == WINDOWING_MODE_FULLSCREEN");
            return true;
        }
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        if (!mffas.inPinMode() && !mffas.isInMiniFreeFormMode()) {
            return false;
        }
        activeFreeformTask(taskId);
        Slog.d(TAG, "activeFreeformTaskIfNeed taskId: " + taskId + " activityOptions: " + activityOptions + " startForSosc: " + startForSosc);
        return true;
    }

    public void exitFreeformIfEnterSplitScreen(Task candidateTask, Task candidateRoot) {
        if (candidateTask != null && candidateTask.inFreeformWindowingMode() && isLaunchForSplitMode(candidateRoot)) {
            Slog.d(TAG, "exitFreeformEnterSplitScreen candidateTask: " + candidateTask + " candidateRoot: " + candidateRoot);
            Configuration c = new Configuration(candidateTask.getRequestedOverrideConfiguration());
            c.windowConfiguration.setAlwaysOnTop(false);
            c.windowConfiguration.setWindowingMode(0);
            c.windowConfiguration.setBounds((Rect) null);
            candidateTask.onRequestedOverrideConfigurationChanged(c);
        }
    }

    private boolean isLaunchForSplitMode(Task task) {
        Task rootTask;
        if (task == null || (rootTask = task.getRootTask()) == null) {
            return false;
        }
        return rootTask.mSoScRoot || (rootTask.mCreatedByOrganizer && rootTask.getWindowingMode() == 1 && rootTask.getChildCount() == 2 && task.getWindowingMode() == 6);
    }

    public float getFreeformLastScale(int taskId) {
        Task task = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
        return this.mActivityTaskManagerService.mTaskSupervisor.getLaunchParamsController().getFreeformLastScale(task);
    }

    public Rect getFreeformLastPosition(int taskId) {
        Task task = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
        return this.mActivityTaskManagerService.mTaskSupervisor.getLaunchParamsController().getFreeformLastPosition(task);
    }

    public boolean hasFreeformDesktopMemory(int taskId) {
        Task task = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
        return this.mActivityTaskManagerService.mTaskSupervisor.getLaunchParamsController().hasFreeformDesktopMemory(task);
    }

    public void setHideStackFromFullScreen(int taskId, boolean hidden) {
        Task nextFocusdTask;
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            Slog.d(TAG, " setHideStackFromFullScreen taskId=" + taskId + ", hiden=" + hidden + ", mffas=" + mffas);
            mffas.setHideStackFromFullScreen(hidden);
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                Task curentTask = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
                if (curentTask != null && curentTask.isFocused() && hidden && (nextFocusdTask = curentTask.getNextFocusableTask(false)) != null) {
                    this.mActivityTaskManagerService.setFocusedRootTask(nextFocusdTask.getRootTaskId());
                }
            }
        }
    }

    public boolean isHideStackFromFullScreen(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            return mffas.isHideStackFromFullScreen();
        }
        return false;
    }

    public void setFrontFreeFormStackInfo(int taskId, boolean isFront) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            Slog.d(TAG, " setFrontFreeFormStackInfo taskId=" + taskId + ", isFront=" + isFront + ", mffas=" + mffas);
            mffas.setIsFrontFreeFormStackInfo(isFront);
        }
    }

    public boolean isFrontFreeFormStackInfo(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            return mffas.isFrontFreeFormStackInfo();
        }
        return false;
    }

    public void addFullScreenTasksBehindHome(int taskId) {
        this.mAppBehindHome.add(Integer.valueOf(taskId));
    }

    public void removeFullScreenTasksBehindHome(int taskId) {
        this.mAppBehindHome.remove(Integer.valueOf(taskId));
    }

    public void clearFullScreenTasksBehindHome() {
        this.mAppBehindHome.clear();
    }

    public boolean isFullScreenStrategyNeededInDesktopMode(int taskId) {
        if (!MiuiDesktopModeUtils.isDesktopActive()) {
            return false;
        }
        Task task = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
        ActivityRecord ar = task.getRootActivity();
        return (ar != null && (MiuiFreeformUtilStub.getInstance().getDesktopModeLaunchFullscreenAppList().contains(ar.packageName) || MiuiFreeformUtilStub.getInstance().getDesktopModeLaunchFullscreenAppList().contains(ar.shortComponentName))) || MiuiFreeformUtilStub.getInstance().isAppSetAutoUiInDesktopMode(ar, 0, this.mActivityTaskManagerService.mContext);
    }

    public boolean isSplitRootTask(int taskId) {
        Task task = this.mActivityTaskManagerService.mRootWindowContainer.anyTaskForId(taskId, 0);
        if (task != null) {
            return task.isSplitScreenRootTask();
        }
        return false;
    }

    public boolean isInVideoOrGameScene() {
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null) {
            return miuiFreeFormGestureController.isInVideoOrGameScene();
        }
        return false;
    }

    public void deliverResultForFinishActivity(ActivityRecord resultTo, ActivityRecord resultFrom, Intent intent) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null) {
            miuiFreeFormGestureController.deliverResultForFinishActivity(resultTo, resultFrom, intent);
        }
    }

    public void deliverResultForResumeActivityInFreeform(ActivityRecord resultTo) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null) {
            miuiFreeFormGestureController.deliverResultForResumeActivityInFreeform(resultTo);
        }
    }

    public boolean ignoreDeliverResultForFreeForm(ActivityRecord resultTo, ActivityRecord resultFrom) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = this.mFreeFormGestureController;
        if (miuiFreeFormGestureController != null) {
            return miuiFreeFormGestureController.ignoreDeliverResultForFreeForm(resultTo, resultFrom);
        }
        return false;
    }

    public void freeformToFullscreenByBottomCaption(int taskId) {
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            if (this.mFreeFormGestureController != null) {
                MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) getMiuiFreeFormActivityStack(taskId);
                Slog.d(TAG, " freeformToFullscreenByBottomCaption: taskId= " + taskId);
                if (mffas != null) {
                    this.mFreeFormGestureController.deliverResultForExitFreeform(mffas);
                }
            }
        }
    }

    public void miniFreeformToFullscreenByBottomCaption(int taskId) {
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            if (this.mFreeFormGestureController != null) {
                MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) getMiuiFreeFormActivityStack(taskId);
                Slog.d(TAG, " miniFreeformToFullscreenByBottomCaption: taskId= " + taskId);
                if (mffas != null) {
                    this.mFreeFormGestureController.deliverResultForExitFreeform(mffas);
                }
            }
        }
    }

    public void launchFullscreenInDesktopModeIfNeeded(ActivityRecord r, Task reusedTask) {
        if (!MiuiDesktopModeUtils.isDesktopActive() || r == null || reusedTask == null) {
            return;
        }
        List<String> launchFullscreenAppList = MiuiMultiWindowUtils.getDesktopModeLaunchFullscreenAppList();
        for (int i = 0; i < launchFullscreenAppList.size(); i++) {
            if (launchFullscreenAppList.get(i).equals(r.shortComponentName)) {
                MiuiFreeFormActivityStackStub mffas = getMiuiFreeFormActivityStack(reusedTask.mTaskId);
                if (mffas != null) {
                    fullscreenFreeformTask(reusedTask.mTaskId);
                    return;
                }
            }
        }
    }

    public Rect getMiuiFreeformBounds(int taskId, Rect originalBounds) {
        Rect bounds = new Rect(originalBounds);
        bounds.scale(getFreeformScale(taskId));
        bounds.offsetTo(originalBounds.left, originalBounds.top);
        return bounds;
    }

    public float getMiuiFreeformCornerRadius(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        if (mffas.isInFreeFormMode()) {
            float cornerRadius = MiuiMultiWindowUtils.FREEFORM_ROUND_CORNER_DIP_MIUI15;
            return cornerRadius;
        }
        if (!mffas.isInMiniFreeFormMode()) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        float cornerRadius2 = MiuiMultiWindowUtils.MINI_FREEFORM_ROUND_CORNER_DIP_MIUI15;
        return cornerRadius2;
    }

    public boolean disableIgnoreOrientationRequest(TransitionController transitionController) {
        return transitionController.getCollectingTransitionType() == 2147483543;
    }

    public boolean adjustBoundsAndScaleIfNeeded(int taskId) {
        try {
            return this.mFreeformModeControl.adjustBoundsAndScaleIfNeeded(taskId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getFreeformStackBounds(Task task, int oldRotation, int newRotation, Rect preBounds, Rect newBounds) {
        MiuiFreeFormActivityStack ffas = (MiuiFreeFormActivityStack) getMiuiFreeFormActivityStack(task.getRootTaskId());
        if (ffas != null && ffas.mTask != null) {
            Task topRootTask = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(5);
            MiuiMultiWindowUtils.getFreeformRect(this.mActivityTaskManagerService.mWindowManager.mContext, false, false, false, ffas.mIsLandcapeFreeform, newBounds, ffas.getStackPackageName(), true, (topRootTask == null || topRootTask.mTaskId == task.getRootTaskId()) ? false : true, 0, 0, ffas.isNormalFreeForm, MiuiDesktopModeUtils.isActive(this.mActivityTaskManagerService.mContext));
            ffas.setFreeformScale(MiuiMultiWindowUtils.sScale);
            logd(TAG, " preBounds= " + preBounds + " newBounds= " + newBounds + " newRotation= " + newRotation + " scale= " + MiuiMultiWindowUtils.sScale);
            return true;
        }
        return false;
    }

    public void moveToFront(Task task, String reason) {
        IMiuiFreeformModeControl iMiuiFreeformModeControl;
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas != null) {
            if ((mffas.isInMiniFreeFormMode() || task.isVisible()) && (iMiuiFreeformModeControl = this.mFreeformModeControl) != null) {
                try {
                    iMiuiFreeformModeControl.moveToFront(task.getRootTaskId(), reason);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error moveToFront.", e);
                }
            }
        }
    }

    public boolean isNormalFreeForm(Task task, String pkg) {
        if (task == null) {
            return false;
        }
        if (MiuiMultiWindowAdapter.inFreeformWhiteList(pkg) || (task.realActivity != null && MiuiMultiWindowAdapter.getFreeformResizeableWhiteList().contains(task.realActivity.flattenToString()))) {
            Slog.d(TAG, "Task " + task.mTaskId + " is Normal freeform because pkg or activity is in freeform white list");
            return true;
        }
        if (MiuiMultiWindowAdapter.getFreeformBlackList().contains(pkg)) {
            Slog.d(TAG, "Task " + task.mTaskId + " is Abnormal freeform because pkg is in freeform black list");
            return false;
        }
        return ActivityInfo.isResizeableMode(task.mResizeMode);
    }

    public boolean unSupportedFreeformInDesktop(final int taskId) {
        Task target = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getRootTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MiuiFreeFormManagerService.lambda$unSupportedFreeformInDesktop$18(taskId, (Task) obj);
            }
        });
        return !MiuiFreeformUtilStub.getInstance().isSupportFreeFormInDesktopMode(target);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$unSupportedFreeformInDesktop$18(int taskId, Task task) {
        return task.mTaskId == taskId;
    }

    public void moveTaskToBack(Task task) {
        MiuiFreeFormActivityStack mffas;
        if (task != null && task.inFreeformWindowingMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) != null && !mffas.inPinMode() && !mffas.isInMiniFreeFormMode()) {
            this.mFreeFormGestureController.startExitApplication(mffas);
        }
    }

    public void exitAllFreeform() {
        List<MiuiFreeFormActivityStack> miuiFreeFormActivityStackList = getAllMiuiFreeFormActivityStack();
        for (MiuiFreeFormActivityStack mffas : miuiFreeFormActivityStackList) {
            if (mffas != null) {
                this.mFreeFormGestureController.startExitApplication(mffas);
            }
        }
    }

    public boolean isInInfiniteDragTaskResizeAnim(int taskId) {
        try {
            return this.mFreeformModeControl.isInInfiniteDragTaskResizeAnim(taskId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean appLockAlreadyInFront(Task targetRootTask, ActivityRecord startActivity) {
        return targetRootTask != null && startActivity != null && targetRootTask.inFreeformWindowingMode() && isFrontFreeFormStackInfo(targetRootTask.mTaskId) && startActivity.mActivityComponent != null && AccessController.APP_LOCK_CLASSNAME.contains(startActivity.mActivityComponent.flattenToShortString());
    }

    public void autoLayoutOthersIfNeed(int taskId) {
        try {
            this.mFreeformModeControl.autoLayoutOthersIfNeed(taskId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean shouldAdjustFreeformLayer(int type, TransitionInfo info) {
        WindowContainer wc;
        boolean shouldAdjustLayer = false;
        if (info != null) {
            int i = info.getChanges().size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                TransitionInfo.Change c = (TransitionInfo.Change) info.getChanges().get(i);
                if (c.getContainer() == null || (wc = WindowContainer.fromBinder(c.getContainer().asBinder())) == null || !wc.inFreeformWindowingMode()) {
                    i--;
                } else {
                    shouldAdjustLayer = true;
                    break;
                }
            }
        }
        if ((info != null && info.getChanges().isEmpty()) || ((type == 1 || type == 3) && shouldAdjustLayer)) {
            return true;
        }
        return false;
    }
}
