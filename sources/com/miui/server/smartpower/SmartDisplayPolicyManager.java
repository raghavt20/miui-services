package com.miui.server.smartpower;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.TransitionInfo;
import com.android.server.am.SmartPowerService;
import com.android.server.am.SystemPressureControllerStub;
import com.android.server.am.ThermalTempListener;
import com.android.server.display.DisplayManagerServiceStub;
import com.android.server.display.mode.DisplayModeDirector;
import com.android.server.display.mode.DisplayModeDirectorStub;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.RealTimeModeControllerStub;
import com.android.server.wm.SchedBoostGesturesEvent;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.AccessController;
import com.miui.server.smartpower.SmartScenarioManager;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.smartpower.MultiTaskActionManager;

/* loaded from: classes.dex */
public class SmartDisplayPolicyManager {
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    private static final int INSET_IME_HIDE = 0;
    private static final int INSET_IME_SHOW = 1;
    private static final int INTERACT_APP_TRANSITION = 2;
    private static final int INTERACT_BASE_TYPE = 1;
    public static final int INTERACT_CVW_GESTURE = 14;
    private static final int INTERACT_FOCUS_WINDOW_CHANGE = 7;
    public static final int INTERACT_FREE_FORM = 12;
    private static final int INTERACT_INPUT_DOWN = 8;
    private static final int INTERACT_INPUT_FLING = 10;
    private static final int INTERACT_INPUT_MOVE = 9;
    private static final int INTERACT_INSET_ANIMATION = 4;
    public static final int INTERACT_MULTI_WINDOW = 15;
    private static final int INTERACT_NONE_TYPE = 0;
    private static final int INTERACT_RECENT_ANIMATION = 5;
    private static final int INTERACT_REMOTE_ANIMATION = 6;
    public static final int INTERACT_SPLIT_SCREEN = 13;
    private static final int INTERACT_STOP_BASE_TYPE = -100;
    private static final int INTERACT_STOP_CAMERA = -99;
    public static final int INTERACT_TRANSITION = 16;
    private static final int INTERACT_WINDOW_ANIMATION = 3;
    private static final int INTERACT_WINDOW_VISIBLE_CHANGED = 11;
    private static final int INVALID_APP_TRANSITION_DUR = 100;
    private static final int INVALID_DEFAULT_DUR = 100;
    private static final int INVALID_SF_MODE_ID = -1;
    private static final int INVALID_WINDOW_ANIMATION_DUR = 200;
    private static final int MAX_INPUT_FLING_DURATION = 3000;
    private static final int MODULE_SF_INTERACTION = 268;
    private static final int MSG_DOWN_REFRESHRATE = 2;
    private static final int MSG_UPDATE_DISPLAY_DEVICE = 3;
    private static final int MSG_UP_REFRESHRATE = 1;
    private static final int SCENARIO_TYPE_EXTERNAL_CASTING = 16384;
    private static final int SCENARIO_TYPE_TOPAPP = 2;
    private static final int SF_INTERACTION_END = 0;
    private static final int SF_INTERACTION_START = 1;
    private static final String SF_PERF_INTERACTION = "perf_interaction";
    public static final String TAG = "SmartPower.DisplayPolicy";
    private static final int TRANSACTION_SF_DISPLAY_FEATURE_IDLE = 31107;
    private boolean isTempWarning;
    private final Context mContext;
    private int mCurrentTemp;
    private final Object mDisplayLock;
    private final DisplayManagerServiceStub mDisplayManagerServiceStub;
    private final DisplayModeDirectorStub mDisplayModeDirectorStub;
    private int mDisplayModeHeight;
    private int mDisplayModeWidth;
    private boolean mExternalDisplayConnected;
    private String mFrameInsertScene;
    private boolean mFrameInsertingForGame;
    private boolean mFrameInsertingForVideo;
    private final FrameInsertionObserver mFrameInsertionObserver;
    private final H mHandler;
    private final HandlerThread mHandlerTh;
    private boolean mIsCameraInForeground;
    private String mLastFocusPackage;
    private int mLastInputMethodStatus;
    private RefreshRatePolicyController mRefreshRatePolicyController;
    private SchedBoostGesturesEvent mSchedBoostGesturesEvent;
    private SmartScenarioManager mSmartScenarioManager;
    private SmartWindowPolicyManager mSmartWindowPolicyManager;
    private SurfaceControl.DisplayMode[] mSupportedDisplayModes;
    private final IBinder mSurfaceFlinger;
    private final WindowManagerService mWMS;
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    private static final boolean ENABLE = SmartPowerSettings.DISPLAY_POLICY_ENABLE;
    private static final boolean CAMERA_REFRESH_RATE_ENABLE = SmartPowerSettings.PROP_CAMERA_REFRESH_RATE_ENABLE;
    private static final int THERMAL_TEMP_THRESHOLD = SmartPowerSettings.PROP_THERMAL_TEMP_THRESHOLD;

    public SmartDisplayPolicyManager(Context context, WindowManagerService wms) {
        HandlerThread handlerThread = new HandlerThread("SmartPowerDisplayTh", -2);
        this.mHandlerTh = handlerThread;
        this.mDisplayLock = new Object();
        this.mRefreshRatePolicyController = null;
        this.mSchedBoostGesturesEvent = null;
        this.mLastInputMethodStatus = 0;
        this.mLastFocusPackage = "";
        this.mExternalDisplayConnected = false;
        this.mFrameInsertingForGame = false;
        this.mFrameInsertingForVideo = false;
        this.mFrameInsertScene = "";
        this.mIsCameraInForeground = false;
        this.isTempWarning = false;
        this.mCurrentTemp = 0;
        this.mContext = context;
        this.mWMS = wms;
        this.mSurfaceFlinger = ServiceManager.getService(DumpSysInfoUtil.SURFACEFLINGER);
        this.mDisplayManagerServiceStub = DisplayManagerServiceStub.getInstance();
        this.mDisplayModeDirectorStub = DisplayModeDirectorStub.getInstance();
        handlerThread.start();
        H h = new H(handlerThread.getLooper());
        this.mHandler = h;
        Process.setThreadGroupAndCpuset(handlerThread.getThreadId(), 1);
        this.mFrameInsertionObserver = new FrameInsertionObserver(context, h);
        this.mRefreshRatePolicyController = new RefreshRatePolicyController(context, h);
    }

    public void init(SmartScenarioManager smartScenarioManager, SmartWindowPolicyManager smartWindowPolicyManager) {
        this.mSmartScenarioManager = smartScenarioManager;
        this.mSmartWindowPolicyManager = smartWindowPolicyManager;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void systemReady() {
        SchedBoostGesturesEvent schedBoostGesturesEvent = new SchedBoostGesturesEvent(this.mHandlerTh.getLooper());
        this.mSchedBoostGesturesEvent = schedBoostGesturesEvent;
        schedBoostGesturesEvent.init(this.mContext);
        this.mSchedBoostGesturesEvent.setGesturesEventListener(new GesturesEventListenerCallback());
        SmartScenarioManager.ClientConfig createClientConfig = this.mSmartScenarioManager.createClientConfig(TAG, new SmartScenarioCallback());
        createClientConfig.addMainScenarioIdConfig(2L, "com.android.camera", 2);
        createClientConfig.addAdditionalScenarioIdConfig(16384L, null, 16384);
        this.mSmartScenarioManager.registClientConfig(createClientConfig);
        SystemPressureControllerStub.getInstance().registerThermalTempListener(new ThermalTempListenerCallback());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GesturesEventListenerCallback implements SchedBoostGesturesEvent.GesturesEventListener {
        private GesturesEventListenerCallback() {
        }

        @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
        public void onFling(float velocityX, float velocityY, int durationMs) {
            if (isVerticalFling((int) velocityX, (int) velocityY)) {
                SmartDisplayPolicyManager.this.notifyInputEventReceived(10, Math.min(durationMs, SmartDisplayPolicyManager.MAX_INPUT_FLING_DURATION));
            }
        }

        @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
        public void onScroll(boolean started) {
        }

        @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
        public void onDown() {
            SmartDisplayPolicyManager.this.notifyInputEventReceived(8);
        }

        @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
        public void onMove() {
            SmartDisplayPolicyManager.this.notifyInputEventReceived(9);
        }

        private boolean isVerticalFling(int velocityX, int velocityY) {
            return Math.abs(velocityY) - Math.abs(velocityX) > 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SmartScenarioCallback implements SmartScenarioManager.ISmartScenarioCallback {
        private SmartScenarioCallback() {
        }

        @Override // com.miui.server.smartpower.SmartScenarioManager.ISmartScenarioCallback
        public void onCurrentScenarioChanged(long mainSenarioId, long additionalSenarioId) {
            SmartDisplayPolicyManager.this.mExternalDisplayConnected = (16384 & additionalSenarioId) != 0;
            boolean cameraIsInForeground = (2 & mainSenarioId) != 0;
            SmartDisplayPolicyManager.this.updateCameraInForegroundStatus(cameraIsInForeground);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ThermalTempListenerCallback implements ThermalTempListener {
        private ThermalTempListenerCallback() {
        }

        public void onThermalTempChange(int temp) {
            SmartDisplayPolicyManager.this.mCurrentTemp = temp;
            SmartDisplayPolicyManager.this.isTempWarning = temp > SmartDisplayPolicyManager.THERMAL_TEMP_THRESHOLD;
            if (SmartDisplayPolicyManager.DEBUG && SmartDisplayPolicyManager.this.isTempWarning) {
                Slog.d(SmartDisplayPolicyManager.TAG, "onThermalTempChange, warning temp: " + temp);
            }
        }
    }

    private void setDisplaySize(int width, int height) {
        synchronized (this.mDisplayLock) {
            this.mDisplayModeWidth = width;
            this.mDisplayModeHeight = height;
            if (DEBUG) {
                Slog.d(TAG, "update display size: " + this.mDisplayModeWidth + " * " + this.mDisplayModeHeight);
            }
        }
    }

    private void resetDisplaySize() {
        synchronized (this.mDisplayLock) {
            this.mDisplayModeWidth = 0;
            this.mDisplayModeHeight = 0;
        }
    }

    private void updateSupportDisplayModes() {
        SurfaceControl.DisplayMode[] displayModes = getSupportDisplayModes();
        synchronized (this.mDisplayLock) {
            this.mSupportedDisplayModes = displayModes;
            updatePolicyControllerLocked();
        }
    }

    private SurfaceControl.DisplayMode[] getSupportDisplayModes() {
        SurfaceControl.DynamicDisplayInfo dynamicDisplayInfo = this.mDisplayManagerServiceStub.updateDefaultDisplaySupportMode();
        if (dynamicDisplayInfo == null) {
            Slog.e(TAG, "get dynamic display info error");
            return null;
        }
        SurfaceControl.DisplayMode[] supportedDisplayModes = dynamicDisplayInfo.supportedDisplayModes;
        if (supportedDisplayModes != null && DEBUG) {
            Slog.d(TAG, "update display modes: " + Arrays.toString(supportedDisplayModes));
        }
        return supportedDisplayModes;
    }

    private void updatePolicyControllerLocked() {
        this.mRefreshRatePolicyController.updateControllerLocked();
    }

    public void notifyActivityStartUnchecked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart) {
        if ("com.android.camera".equals(packageName)) {
            updateCameraInForegroundStatus(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCameraInForegroundStatus(boolean isInForeground) {
        if (!CAMERA_REFRESH_RATE_ENABLE) {
            return;
        }
        this.mIsCameraInForeground = isInForeground;
        if (isInForeground) {
            endControlRefreshRatePolicy(INTERACT_STOP_CAMERA);
        }
    }

    public void notifyAppTransitionStartLocked(long animDuration) {
        if (!isSupportSmartDisplayPolicy() || animDuration <= 100) {
            return;
        }
        long animDuration2 = animDuration + getDurationForInteraction(2);
        if (DEBUG) {
            Slog.d(TAG, "notifyAppTransitionStart animDuration: " + animDuration2);
        }
        startControlRefreshRatePolicy(2, animDuration2);
    }

    public void notifyWindowAnimationStartLocked(long animDuration, SurfaceControl animationLeash) {
        if (!isSupportSmartDisplayPolicy() || animDuration <= 200) {
            return;
        }
        long animDuration2 = animDuration + getDurationForInteraction(3);
        if (DEBUG) {
            Slog.d(TAG, "notifyWindowAnimationStart animDuration: " + animDuration2);
        }
        startControlRefreshRatePolicy(3, animDuration2);
    }

    public void notifyInsetAnimationShow(int types) {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyInsetAnimationShow");
            }
            if ((WindowInsets.Type.ime() & types) != 0) {
                notifyInputMethodAnimationStart(1);
            }
        }
    }

    public void notifyInsetAnimationHide(int types) {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyInsetAnimationHide");
            }
            if ((WindowInsets.Type.ime() & types) != 0) {
                notifyInputMethodAnimationStart(0);
            }
        }
    }

    private void notifyInputMethodAnimationStart(int status) {
        if (status == 1 && this.mLastInputMethodStatus == 1) {
            return;
        }
        if (status == 1 || (status == 0 && this.mLastFocusPackage.equals(getCurrentFocusPackageName()))) {
            startControlRefreshRatePolicy(4);
        }
        this.mLastInputMethodStatus = status;
        this.mLastFocusPackage = getCurrentFocusPackageName();
    }

    public void notifyRecentsAnimationStart(ActivityRecord targetActivity) {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyRecentsAnimationStart");
            }
            startControlRefreshRatePolicy(5);
        }
    }

    public void notifyRecentsAnimationEnd() {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyRecentsAnimationEnd");
            }
            endControlRefreshRatePolicy(5);
        }
    }

    public void notifyRemoteAnimationStart(RemoteAnimationTarget[] appTargets) {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyRemoteAnimationStart");
            }
            startControlRefreshRatePolicy(6);
        }
    }

    public void notifyRemoteAnimationEnd() {
        if (isSupportSmartDisplayPolicy()) {
            if (DEBUG) {
                Slog.d(TAG, "notifyRemoteAnimationEnd");
            }
            endControlRefreshRatePolicy(6);
        }
    }

    public void notifyFocusedWindowChangeLocked(String oldFocusPackage, String newFocusPackage) {
        if (!isSupportSmartDisplayPolicy() || oldFocusPackage == null || newFocusPackage == null || oldFocusPackage.equals(newFocusPackage)) {
            return;
        }
        if (this.mRefreshRatePolicyController.isFocusedPackageWhiteList(newFocusPackage)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyFocusedWindowChanged, new: " + newFocusPackage);
            }
            startControlRefreshRatePolicy(7);
        } else if (this.mRefreshRatePolicyController.isFocusedPackageWhiteList(oldFocusPackage)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyFocusedWindowChanged, old: " + oldFocusPackage);
            }
            endControlRefreshRatePolicy(7);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInputEventReceived(int inputType) {
        notifyInputEventReceived(inputType, getDurationForInteraction(inputType));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInputEventReceived(int inputType, long durationMs) {
        if (isSupportSmartDisplayPolicy()) {
            String focusedPackage = getCurrentFocusPackageName();
            if (this.mRefreshRatePolicyController.isInputPackageWhiteList(focusedPackage)) {
                if (DEBUG) {
                    Slog.d(TAG, "notifyInputEventReceived, " + interactionTypeToString(inputType) + ", durationMs: " + durationMs + ", focus: " + focusedPackage);
                }
                startControlRefreshRatePolicy(inputType, durationMs);
            }
        }
    }

    public void notifyWindowVisibilityChangedLocked(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
        if (isSupportSmartDisplayPolicy() && visible && this.mRefreshRatePolicyController.isWindowVisibleWhiteList(win, attrs)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyWindowVisibilityChangedLocked");
            }
            startControlRefreshRatePolicy(11);
        }
    }

    public void notifyMultiTaskActionStart(MultiTaskActionManager.ActionInfo actionInfo) {
        if (!isSupportSmartDisplayPolicy() || actionInfo == null) {
            return;
        }
        int actionType = actionInfo.getType();
        if (DEBUG) {
            Slog.d(TAG, "notifyMultiTaskActionStart: " + MultiTaskActionManager.actionTypeToString(actionType));
        }
        int interactType = getInteractTypeForMultiTaskAction(actionType);
        if (interactType == 0) {
            return;
        }
        startControlRefreshRatePolicy(interactType);
    }

    public void notifyMultiTaskActionEnd(MultiTaskActionManager.ActionInfo actionInfo) {
        if (!isSupportSmartDisplayPolicy() || actionInfo == null) {
            return;
        }
        int actionType = actionInfo.getType();
        if (DEBUG) {
            Slog.d(TAG, "notifyMultiTaskActionEnd: " + MultiTaskActionManager.actionTypeToString(actionType));
        }
        int interactType = getInteractTypeForMultiTaskAction(actionType);
        if (interactType == 0) {
            return;
        }
        endControlRefreshRatePolicy(interactType);
    }

    private int getInteractTypeForMultiTaskAction(int type) {
        if (MultiTaskActionManager.isFreeFormAction(type)) {
            return 12;
        }
        if (MultiTaskActionManager.isSplictScreenAction(type)) {
            return 13;
        }
        if (MultiTaskActionManager.isCVWAction(type)) {
            return 14;
        }
        if (MultiTaskActionManager.isMultiWindowSwitchAction(type)) {
            return 15;
        }
        return 0;
    }

    public void notifyTransitionStart(TransitionInfo info) {
        if (!isSupportSmartDisplayPolicy() || info.getType() == 12) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyTransitionReady: " + info.toString());
        }
        startControlRefreshRatePolicy(16);
    }

    public void notifyTransitionEnd(int syncId, int type, int flags) {
        if (!isSupportSmartDisplayPolicy() || type == 12) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyTransitionFinish: " + WindowManager.transitTypeToString(type));
        }
        endControlRefreshRatePolicy(16);
    }

    public void notifyDisplayDeviceStateChangeLocked(int deviceState) {
        if (ENABLE && deviceState != -1) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            Message msg = this.mHandler.obtainMessage(3);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shouldUpdateDisplayDevice() {
        updateSupportDisplayModes();
        resetDisplaySize();
    }

    public void notifyDisplaySwitchResolutionLocked(int width, int height, int density) {
        if (ENABLE) {
            resetDisplaySize();
        }
    }

    public boolean shouldInterceptUpdateDisplayModeSpecs(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
        if (isSupportSmartDisplayPolicy() && displayId == 0) {
            return this.mRefreshRatePolicyController.shouldInterceptUpdateDisplayModeSpecs(modeSpecs);
        }
        return false;
    }

    private void startControlRefreshRatePolicy(int interactType) {
        startControlRefreshRatePolicy(interactType, getDurationForInteraction(interactType));
    }

    private void startControlRefreshRatePolicy(int interactType, long durationMs) {
        int maxRefreshRateInPolicy = this.mRefreshRatePolicyController.getMaxRefreshRateInPolicy();
        int minRefreshRateInPolicy = this.mRefreshRatePolicyController.getMinRefreshRateInPolicy();
        this.mRefreshRatePolicyController.notifyInteractionStart(new ComingInteraction(minRefreshRateInPolicy, maxRefreshRateInPolicy, interactType, durationMs));
    }

    private void endControlRefreshRatePolicy(int interactType) {
        int minRefreshRateInPolicy = this.mRefreshRatePolicyController.getMinRefreshRateInPolicy();
        long delayDownMs = getDelayDownTimeForInteraction(interactType);
        this.mRefreshRatePolicyController.notifyInteractionEnd(new ComingInteraction(minRefreshRateInPolicy, minRefreshRateInPolicy, interactType, delayDownMs));
    }

    private long getDurationForInteraction(int interactType) {
        return this.mRefreshRatePolicyController.getDurationForInteraction(interactType);
    }

    private long getDelayDownTimeForInteraction(int interactType) {
        return this.mRefreshRatePolicyController.getDelayDownTimeForInteraction(interactType);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shouldUpRefreshRate(ComingInteraction comingInteraction) {
        float maxUpRefreshRate = comingInteraction.mMaxRefreshRate;
        float minUpRefreshRate = comingInteraction.mMinRefreshRate;
        long interactDuration = comingInteraction.mDuration;
        int baseSfModeId = findBaseSfModeId(maxUpRefreshRate);
        if (baseSfModeId == -1 || isSwitchingResolution()) {
            this.mRefreshRatePolicyController.reset();
            if (DEBUG) {
                Slog.d(TAG, "invalid baseSfModeId or switching resolution now");
                return;
            }
            return;
        }
        realControlRefreshRate(baseSfModeId, false, minUpRefreshRate, maxUpRefreshRate);
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessageDelayed(msg, interactDuration);
        if (DEBUG) {
            Slog.d(TAG, "up refreshrate: " + maxUpRefreshRate);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shouldDownRefreshRate() {
        this.mHandler.removeMessages(2);
        this.mRefreshRatePolicyController.reset();
        notifyDisplayModeSpecsChanged();
        if (DEBUG) {
            Slog.d(TAG, "down refreshrate");
        }
    }

    private void realControlRefreshRate(int baseSfModeId, boolean allowGroupSwitching, float minUpRefreshRate, float maxUpRefreshRate) {
        notifySurfaceFlingerInteractionChanged(1);
        SurfaceControl.RefreshRateRange physical = buildRefreshRateRange(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, Float.POSITIVE_INFINITY);
        SurfaceControl.RefreshRateRange render = buildRefreshRateRange(minUpRefreshRate, maxUpRefreshRate);
        SurfaceControl.RefreshRateRanges primaryRanges = new SurfaceControl.RefreshRateRanges(physical, render);
        SurfaceControl.RefreshRateRanges appRequestRanges = new SurfaceControl.RefreshRateRanges(physical, render);
        SurfaceControl.DesiredDisplayModeSpecs desired = new SurfaceControl.DesiredDisplayModeSpecs(baseSfModeId, allowGroupSwitching, primaryRanges, appRequestRanges);
        this.mDisplayManagerServiceStub.shouldUpdateDisplayModeSpecs(desired);
    }

    private SurfaceControl.RefreshRateRange buildRefreshRateRange(float min, float max) {
        return new SurfaceControl.RefreshRateRange(min, max);
    }

    private void notifyDisplayModeSpecsChanged() {
        notifySurfaceFlingerInteractionChanged(0);
        this.mDisplayModeDirectorStub.notifyDisplayModeSpecsChanged();
    }

    private void notifySurfaceFlingerInteractionChanged(int actionType) {
        if (this.mSurfaceFlinger == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeInt(MODULE_SF_INTERACTION);
        data.writeInt(actionType);
        data.writeString(SF_PERF_INTERACTION);
        try {
            try {
                this.mSurfaceFlinger.transact(TRANSACTION_SF_DISPLAY_FEATURE_IDLE, data, null, 1);
            } catch (RemoteException | SecurityException ex) {
                Slog.e(TAG, "Failed to notify idle to SurfaceFlinger", ex);
            }
        } finally {
            data.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSupportSmartDisplayPolicy() {
        if (!ENABLE) {
            return false;
        }
        if (this.mSupportedDisplayModes == null) {
            updateSupportDisplayModes();
        }
        return this.mSupportedDisplayModes != null;
    }

    private String getCurrentFocusPackageName() {
        String focusedPackage = RealTimeModeControllerStub.get().getAppPackageName();
        return focusedPackage != null ? focusedPackage : "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getForegroundPackageName() {
        return SystemPressureControllerStub.getInstance().getForegroundPackageName();
    }

    private boolean isSwitchingResolution() {
        return WindowManagerServiceStub.get().isPendingSwitchResolution();
    }

    private int findBaseSfModeId(float upRefreshRate) {
        synchronized (this.mDisplayLock) {
            if (isSwitchingResolution()) {
                return -1;
            }
            if (this.mDisplayModeWidth == 0 || this.mDisplayModeHeight == 0) {
                Point displaySize = new Point();
                this.mWMS.getInitialDisplaySize(0, displaySize);
                setDisplaySize(displaySize.x, displaySize.y);
            }
            return findBaseSfModeIdLocked(upRefreshRate);
        }
    }

    private int findBaseSfModeIdLocked(float upRefreshRate) {
        for (SurfaceControl.DisplayMode mode : this.mSupportedDisplayModes) {
            if (upRefreshRate == mode.refreshRate && this.mDisplayModeWidth == mode.width && this.mDisplayModeHeight == mode.height) {
                if (DEBUG) {
                    Slog.d(TAG, "select mode: " + mode);
                }
                return mode.id;
            }
        }
        return -1;
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        pw.println("SmartDisplayPolicyManager");
        pw.println("enable: " + isSupportSmartDisplayPolicy());
        pw.println("supportedDisplayModes: " + Arrays.toString(this.mSupportedDisplayModes));
        pw.println("camera refresh rate enable: " + CAMERA_REFRESH_RATE_ENABLE);
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        if (refreshRatePolicyController != null) {
            refreshRatePolicyController.dump(pw, args, opti);
        }
    }

    /* loaded from: classes.dex */
    private class FrameInsertionObserver extends ContentObserver {
        private static final int IRIS_GAME_OFF = 0;
        private static final int IRIS_GAME_ON_EXTERNAL_FRAME = 3;
        private static final int IRIS_GAME_ON_SUPER_RESOLUTION = 2;
        private static final int IRIS_GAME_ON_WITHIN_FRAME = 1;
        private static final int IRIS_GAME_ON_WITHIN_FRAME_SUPER_RESOLUTION = 4;
        public static final String IRIS_GAME_STATUS = "game_iris_status";
        private static final int IRIS_VIDEO_OFF = 0;
        private static final int IRIS_VIDEO_ON = 1;
        public static final String IRIS_VIDEO_STATUS = "video_iris_status";
        private final ContentResolver mContentResolver;
        private final Context mContext;
        private final Uri mIrisGameStatusUri;
        private final Uri mIrisVideoStatusUri;

        public FrameInsertionObserver(Context context, Handler handler) {
            super(handler);
            Uri uriFor = Settings.System.getUriFor(IRIS_GAME_STATUS);
            this.mIrisGameStatusUri = uriFor;
            Uri uriFor2 = Settings.System.getUriFor(IRIS_VIDEO_STATUS);
            this.mIrisVideoStatusUri = uriFor2;
            this.mContext = context;
            ContentResolver contentResolver = context.getContentResolver();
            this.mContentResolver = contentResolver;
            contentResolver.registerContentObserver(uriFor, false, this, 0);
            contentResolver.registerContentObserver(uriFor2, false, this, 0);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int flags) {
            if (this.mIrisGameStatusUri.equals(uri)) {
                updateIrisGameStatus();
            } else if (this.mIrisVideoStatusUri.equals(uri)) {
                updateIrisVideoStatus();
            }
        }

        private void updateIrisGameStatus() {
            int gameStatus = Settings.System.getInt(this.mContext.getContentResolver(), IRIS_GAME_STATUS, 0);
            SmartDisplayPolicyManager.this.mFrameInsertingForGame = gameStatus == 1 || gameStatus == 3 || gameStatus == 4;
            updateFrameInsertScene();
        }

        private void updateIrisVideoStatus() {
            int videoStatus = Settings.System.getInt(this.mContext.getContentResolver(), IRIS_VIDEO_STATUS, 0);
            SmartDisplayPolicyManager.this.mFrameInsertingForVideo = videoStatus == 1;
            updateFrameInsertScene();
        }

        private void updateFrameInsertScene() {
            if (SmartDisplayPolicyManager.this.mFrameInsertingForGame) {
                SmartDisplayPolicyManager.this.mFrameInsertScene = "game";
            } else if (SmartDisplayPolicyManager.this.mFrameInsertingForVideo) {
                SmartDisplayPolicyManager.this.mFrameInsertScene = "video";
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ComingInteraction obj = (ComingInteraction) msg.obj;
                    SmartDisplayPolicyManager.this.shouldUpRefreshRate(obj);
                    return;
                case 2:
                    SmartDisplayPolicyManager.this.shouldDownRefreshRate();
                    return;
                case 3:
                    SmartDisplayPolicyManager.this.shouldUpdateDisplayDevice();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RefreshRatePolicyController {
        private static final int DEFAULT_MIN_REFRESH_RATE = 60;
        private static final long DELAY_DOWN_FOCUS_WINDOW_CHANGED_TIME = 500;
        private static final long DELAY_DOWN_MULTI_TASK_TIME = 500;
        private static final long DELAY_DOWN_RECENT_ANIMATION_TIME = 500;
        private static final long DELAY_DOWN_REMOTE_ANIMATION_TIME = 500;
        private static final long DELAY_DOWN_TIME = 500;
        private static final long INTERACT_APP_TRANSITION_DUR = 20;
        private static final long INTERACT_DEFAULT_DUR = 1000;
        private static final long INTERACT_INSET_ANIMATION_DUR = 300;
        private static final long INTERACT_MULTI_TASK_DUR = 30000;
        private static final long INTERACT_RECENT_ANIMATION_DUR = 30000;
        private static final long INTERACT_REMOTE_ANIMATION_DUR = 30000;
        private static final long INTERACT_WINDOW_ANIMATION_DUR = 20;
        private static final int INVALID_REFRESH_RATE = -1;
        private static final String MIUI_DEFAULT_REFRESH_RATE = "is_smart_fps";
        private static final String MIUI_REFRESH_RATE = "miui_refresh_rate";
        private static final String MIUI_THERMAL_LIMIT_REFRESH_RATE = "thermal_limit_refresh_rate";
        private static final String MIUI_USER_REFRESH_RATE = "user_refresh_rate";
        private static final int NORMAL_MAX_REFRESH_RATE = 120;
        private static final int SOFTWARE_REFRESH_RATE = 90;
        private final int LIMIT_MAX_REFRESH_RATE;
        private boolean isThermalWarning;
        private final Context mContext;
        private long mCurrentInteractDuration;
        private long mCurrentInteractEndTime;
        private int mCurrentInteractRefreshRate;
        private int mCurrentInteractType;
        private int mCurrentUserRefreshRate;
        private boolean mDefaultRefreshRateEnable;
        private final Set<String> mFocusedPackageWhiteList;
        private final Handler mHandler;
        private final Set<String> mInputPackageWhiteList;
        private final SparseArray<Long> mInteractDelayDownTimes;
        private final SparseArray<Long> mInteractDurations;
        private final Object mLock;
        private int mMaxRefreshRateInPolicy;
        private int mMaxRefreshRateSupport;
        private int mMinRefreshRateInPolicy;
        private int mMinRefreshRateSupport;
        private int mMiuiRefreshRate;
        private SettingRefreshRateObserver mSettingRefreshRateObserver;
        private final Set<Integer> mSupportRefreshRates;
        private final Set<String> mSurfaceTextureBlackList;
        private final ArrayMap<String, List<String>> mWindowVisibleWhiteList;

        private RefreshRatePolicyController(Context context, Handler handler) {
            this.mLock = new Object();
            this.LIMIT_MAX_REFRESH_RATE = SmartPowerSettings.PROP_LIMIT_MAX_REFRESH_RATE;
            HashSet hashSet = new HashSet();
            this.mFocusedPackageWhiteList = hashSet;
            HashSet hashSet2 = new HashSet();
            this.mInputPackageWhiteList = hashSet2;
            HashSet hashSet3 = new HashSet();
            this.mSurfaceTextureBlackList = hashSet3;
            ArrayMap<String, List<String>> arrayMap = new ArrayMap<>();
            this.mWindowVisibleWhiteList = arrayMap;
            HashSet hashSet4 = new HashSet();
            this.mSupportRefreshRates = hashSet4;
            SparseArray<Long> sparseArray = new SparseArray<>(8);
            this.mInteractDurations = sparseArray;
            SparseArray<Long> sparseArray2 = new SparseArray<>(4);
            this.mInteractDelayDownTimes = sparseArray2;
            this.mMaxRefreshRateSupport = 60;
            this.mMinRefreshRateSupport = 60;
            this.mMaxRefreshRateInPolicy = 60;
            this.mMinRefreshRateInPolicy = 0;
            this.mDefaultRefreshRateEnable = true;
            this.mCurrentUserRefreshRate = 60;
            this.mSettingRefreshRateObserver = null;
            this.mMiuiRefreshRate = 60;
            this.isThermalWarning = false;
            this.mContext = context;
            this.mHandler = handler;
            hashSet4.add(60);
            this.mSettingRefreshRateObserver = new SettingRefreshRateObserver(context, handler);
            hashSet.add(AccessController.PACKAGE_SYSTEMUI);
            hashSet2.add(AccessController.PACKAGE_SYSTEMUI);
            hashSet2.add("com.miui.home");
            hashSet2.add("com.mi.android.globallauncher");
            hashSet3.add("com.miui.mediaviewer");
            arrayMap.put("com.miui.screenshot", List.of("ScreenshotAnimation"));
            sparseArray.put(2, 20L);
            sparseArray.put(3, 20L);
            sparseArray.put(4, 300L);
            sparseArray.put(5, 30000L);
            sparseArray.put(6, 30000L);
            sparseArray.put(12, 30000L);
            sparseArray.put(13, 30000L);
            sparseArray.put(14, 30000L);
            sparseArray.put(15, 30000L);
            sparseArray2.put(5, 500L);
            sparseArray2.put(6, 500L);
            sparseArray2.put(7, 500L);
            sparseArray2.put(12, 500L);
            sparseArray2.put(13, 500L);
            sparseArray2.put(14, 500L);
            sparseArray2.put(15, 500L);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateControllerLocked() {
            synchronized (this.mLock) {
                resetControllerLocked();
                updateSupportRefreshRatesLocked();
                updateAppropriateRefreshRate();
            }
        }

        private void resetControllerLocked() {
            this.mSupportRefreshRates.clear();
            this.mSupportRefreshRates.add(60);
            this.mMaxRefreshRateSupport = 60;
            this.mMinRefreshRateSupport = 60;
            this.mMaxRefreshRateInPolicy = 60;
            this.mMinRefreshRateInPolicy = 0;
            this.mDefaultRefreshRateEnable = true;
            this.mCurrentUserRefreshRate = 60;
        }

        private void updateSupportRefreshRatesLocked() {
            if (SmartDisplayPolicyManager.this.mSupportedDisplayModes != null) {
                for (SurfaceControl.DisplayMode mode : SmartDisplayPolicyManager.this.mSupportedDisplayModes) {
                    int realRefreshRate = Math.round(mode.refreshRate * 10.0f) / 10;
                    mode.refreshRate = realRefreshRate;
                    this.mSupportRefreshRates.add(Integer.valueOf(realRefreshRate));
                    this.mMaxRefreshRateSupport = Math.max(this.mMaxRefreshRateSupport, realRefreshRate);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isInputPackageWhiteList(String packageName) {
            return this.mInputPackageWhiteList.contains(packageName);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isFocusedPackageWhiteList(String packageName) {
            return this.mFocusedPackageWhiteList.contains(packageName);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isWindowVisibleWhiteList(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs) {
            List<String> windowVisibleList = this.mWindowVisibleWhiteList.get(win.getOwningPackage());
            return windowVisibleList != null && windowVisibleList.contains(String.valueOf(attrs.getTitle()));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyInteractionStart(ComingInteraction comingInteraction) {
            synchronized (this.mLock) {
                if (isEnable() && !shouldInterceptAdjustRefreshRate(comingInteraction)) {
                    long comingInteractEndTime = System.currentTimeMillis() + comingInteraction.mDuration;
                    if (shouldInterceptComingInteractLocked(comingInteraction, comingInteractEndTime)) {
                        return;
                    }
                    updateCurrentInteractionLocked(comingInteraction, comingInteractEndTime);
                    dropCurrentInteraction();
                    Message msg = this.mHandler.obtainMessage(1, comingInteraction);
                    this.mHandler.sendMessage(msg);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyInteractionEnd(ComingInteraction comingInteraction) {
            synchronized (this.mLock) {
                if (isEnable()) {
                    long delayDuration = comingInteraction.mDuration;
                    long comingInteractEndTime = System.currentTimeMillis() + delayDuration;
                    updateCurrentInteractionLocked(comingInteraction, comingInteractEndTime);
                    Message msg = this.mHandler.obtainMessage(2);
                    this.mHandler.sendMessageDelayed(msg, delayDuration);
                }
            }
        }

        private boolean shouldInterceptComingInteractLocked(ComingInteraction comingInteraction, long comingInteractEndTime) {
            if (!isAniminting()) {
                return false;
            }
            if (comingInteractEndTime <= this.mCurrentInteractEndTime) {
                return true;
            }
            updateCurrentInteractionLocked(comingInteraction, comingInteractEndTime);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessageDelayed(2, comingInteraction.mDuration);
            return true;
        }

        private boolean shouldInterceptAdjustRefreshRate(ComingInteraction comingInteraction) {
            return shouldInterceptForDisplayStatus() || shouldInterceptForSoftwareRefreshRate(comingInteraction) || shouldInterceptForFrameInsertion(comingInteraction) || shouldInterceptForSurfaceTextureVideo(comingInteraction);
        }

        private boolean shouldInterceptForDisplayStatus() {
            if (SmartDisplayPolicyManager.this.mExternalDisplayConnected) {
                Slog.d(SmartDisplayPolicyManager.TAG, "should intercept for external display connecting");
                return true;
            }
            if (SmartDisplayPolicyManager.this.isTempWarning) {
                Slog.d(SmartDisplayPolicyManager.TAG, "should intercept for thermal temp warning: " + SmartDisplayPolicyManager.this.mCurrentTemp);
                return true;
            }
            if (SmartDisplayPolicyManager.this.mIsCameraInForeground) {
                Slog.d(SmartDisplayPolicyManager.TAG, "camera in foreground or activity starting");
                return true;
            }
            return false;
        }

        private boolean shouldInterceptForSoftwareRefreshRate(ComingInteraction comingInteraction) {
            boolean z = false;
            if (this.mMaxRefreshRateSupport != NORMAL_MAX_REFRESH_RATE) {
                return false;
            }
            if (this.mMiuiRefreshRate == SOFTWARE_REFRESH_RATE && comingInteraction.mInteractType != 5 && comingInteraction.mInteractType != 6) {
                z = true;
            }
            boolean intercept = z;
            if (intercept && SmartDisplayPolicyManager.DEBUG) {
                Slog.d(SmartDisplayPolicyManager.TAG, "should intercept for software refresh rate");
            }
            return intercept;
        }

        private boolean shouldInterceptForFrameInsertion(ComingInteraction comingInteraction) {
            boolean intercept = false;
            boolean isFrameInserting = SmartDisplayPolicyManager.this.mFrameInsertingForGame || SmartDisplayPolicyManager.this.mFrameInsertingForVideo;
            if (isFrameInserting && comingInteraction.mInteractType != 5 && comingInteraction.mInteractType != 6) {
                intercept = true;
            }
            if (intercept) {
                Slog.d(SmartDisplayPolicyManager.TAG, "should intercept for frame insertion in " + SmartDisplayPolicyManager.this.mFrameInsertScene);
            }
            return intercept;
        }

        private boolean shouldInterceptForSurfaceTextureVideo(ComingInteraction comingInteraction) {
            String packageName = SmartDisplayPolicyManager.this.getForegroundPackageName();
            boolean intercept = (!this.mSurfaceTextureBlackList.contains(packageName) || comingInteraction.mInteractType == 5 || comingInteraction.mInteractType == 6) ? false : true;
            if (intercept) {
                Slog.d(SmartDisplayPolicyManager.TAG, "should intercept for surface texture video");
            }
            return intercept;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean shouldInterceptUpdateDisplayModeSpecs(DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
            if (!isAniminting()) {
                return false;
            }
            float maxRefreshRate = Math.max(modeSpecs.primary.render.max, modeSpecs.appRequest.render.max);
            boolean interceptOrNot = maxRefreshRate < ((float) this.mMaxRefreshRateInPolicy);
            if (SmartDisplayPolicyManager.DEBUG) {
                Slog.d(SmartDisplayPolicyManager.TAG, "intercept update mode specs: " + interceptOrNot);
            }
            return interceptOrNot;
        }

        private void updateCurrentInteractionLocked(ComingInteraction comingInteraction, long comingInteractEndTime) {
            this.mCurrentInteractRefreshRate = comingInteraction.mMaxRefreshRate;
            this.mCurrentInteractType = comingInteraction.mInteractType;
            this.mCurrentInteractDuration = comingInteraction.mDuration;
            this.mCurrentInteractEndTime = comingInteractEndTime;
        }

        private boolean isAniminting() {
            return this.mCurrentInteractEndTime != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getDurationForInteraction(int interactType) {
            Long interactDuration = this.mInteractDurations.get(interactType);
            if (interactDuration != null) {
                return interactDuration.longValue();
            }
            return 1000L;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getDelayDownTimeForInteraction(int interactType) {
            Long interactDelayDownTime = this.mInteractDelayDownTimes.get(interactType);
            if (interactDelayDownTime != null) {
                return interactDelayDownTime.longValue();
            }
            return 0L;
        }

        private void dropCurrentInteraction() {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reset() {
            synchronized (this.mLock) {
                this.mCurrentInteractRefreshRate = 0;
                this.mCurrentInteractType = 0;
                this.mCurrentInteractDuration = 0L;
                this.mCurrentInteractEndTime = 0L;
            }
        }

        private boolean isEnable() {
            return SmartDisplayPolicyManager.ENABLE && this.mMaxRefreshRateInPolicy != this.mMinRefreshRateSupport;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getMaxRefreshRateInPolicy() {
            return this.mMaxRefreshRateInPolicy;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getMinRefreshRateInPolicy() {
            return this.mMinRefreshRateInPolicy;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateAppropriateRefreshRate() {
            synchronized (this.mLock) {
                this.mDefaultRefreshRateEnable = isDefaultRefreshRateEnable();
                int currentUserRefreshRate = getCurrentUserRefreshRate();
                this.mCurrentUserRefreshRate = currentUserRefreshRate;
                if (this.mDefaultRefreshRateEnable) {
                    this.mMaxRefreshRateInPolicy = this.mSupportRefreshRates.stream().max(new Comparator() { // from class: com.miui.server.smartpower.SmartDisplayPolicyManager$RefreshRatePolicyController$$ExternalSyntheticLambda0
                        @Override // java.util.Comparator
                        public final int compare(Object obj, Object obj2) {
                            return ((Integer) obj).compareTo((Integer) obj2);
                        }
                    }).get().intValue();
                    int i = this.LIMIT_MAX_REFRESH_RATE;
                    if (i != -1 && this.mSupportRefreshRates.contains(Integer.valueOf(i))) {
                        this.mMaxRefreshRateInPolicy = this.LIMIT_MAX_REFRESH_RATE;
                    }
                } else if (this.mSupportRefreshRates.contains(Integer.valueOf(currentUserRefreshRate))) {
                    this.mMaxRefreshRateInPolicy = this.mCurrentUserRefreshRate;
                }
                if (SmartDisplayPolicyManager.DEBUG) {
                    Slog.d(SmartDisplayPolicyManager.TAG, "default enable: " + this.mDefaultRefreshRateEnable + ", refresh rate: " + this.mMaxRefreshRateInPolicy);
                }
            }
        }

        public boolean isDefaultRefreshRateEnable() {
            return Settings.System.getInt(this.mContext.getContentResolver(), MIUI_DEFAULT_REFRESH_RATE, 0) == 1;
        }

        public int getCurrentUserRefreshRate() {
            return Settings.Secure.getInt(this.mContext.getContentResolver(), "user_refresh_rate", 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, String[] args, int opti) {
            pw.println("RefreshRatePolicyController");
            pw.println("status: " + isEnable());
            pw.println("refresh rate support: " + this.mSupportRefreshRates.toString());
            pw.println("default refresh rate: " + this.mDefaultRefreshRateEnable);
            pw.println("user refresh rate: " + this.mCurrentUserRefreshRate);
            pw.println("max refresh rate in policy: " + this.mMaxRefreshRateInPolicy);
            pw.println("min refresh rate in policy: " + this.mMinRefreshRateInPolicy);
            pw.println("current type: " + SmartDisplayPolicyManager.interactionTypeToString(this.mCurrentInteractType));
            pw.println("current interact refresh rate: " + this.mCurrentInteractRefreshRate);
            pw.println("current interact duration: " + this.mCurrentInteractDuration);
            pw.println("mCurrentInteractEndTime: " + this.mCurrentInteractEndTime);
        }

        /* loaded from: classes.dex */
        private class SettingRefreshRateObserver extends ContentObserver {
            private final ContentResolver mContentResolver;
            private final Context mContext;
            private final Uri mMiuiDefaultRefreshRateSetting;
            private final Uri mMiuiRefreshRateSetting;
            private final Uri mMiuiUserRefreshRateSetting;
            private final Uri mThermalRefreshRateSetting;

            public SettingRefreshRateObserver(Context context, Handler handler) {
                super(handler);
                Uri uriFor = Settings.System.getUriFor(RefreshRatePolicyController.MIUI_DEFAULT_REFRESH_RATE);
                this.mMiuiDefaultRefreshRateSetting = uriFor;
                Uri uriFor2 = Settings.Secure.getUriFor("user_refresh_rate");
                this.mMiuiUserRefreshRateSetting = uriFor2;
                Uri uriFor3 = Settings.Secure.getUriFor("miui_refresh_rate");
                this.mMiuiRefreshRateSetting = uriFor3;
                Uri uriFor4 = Settings.System.getUriFor("thermal_limit_refresh_rate");
                this.mThermalRefreshRateSetting = uriFor4;
                this.mContext = context;
                ContentResolver contentResolver = context.getContentResolver();
                this.mContentResolver = contentResolver;
                contentResolver.registerContentObserver(uriFor, false, this, 0);
                contentResolver.registerContentObserver(uriFor2, false, this, 0);
                contentResolver.registerContentObserver(uriFor3, false, this, 0);
                contentResolver.registerContentObserver(uriFor4, false, this, 0);
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (!SmartDisplayPolicyManager.this.isSupportSmartDisplayPolicy()) {
                    return;
                }
                if (this.mThermalRefreshRateSetting.equals(uri)) {
                    updateThermalRefreshRateStatus();
                    return;
                }
                if (this.mMiuiRefreshRateSetting.equals(uri)) {
                    updateMiuiRefreshRateStatus();
                } else if (this.mMiuiDefaultRefreshRateSetting.equals(uri) || this.mMiuiUserRefreshRateSetting.equals(uri)) {
                    updateSettingRefreshRateStatus();
                }
            }

            private void updateMiuiRefreshRateStatus() {
                RefreshRatePolicyController.this.mMiuiRefreshRate = Settings.Secure.getInt(this.mContentResolver, "miui_refresh_rate", 0);
            }

            private void updateThermalRefreshRateStatus() {
                int thermalRefreshRate = Settings.System.getInt(this.mContentResolver, "thermal_limit_refresh_rate", 0);
                RefreshRatePolicyController.this.isThermalWarning = thermalRefreshRate >= 60;
                if (SmartDisplayPolicyManager.DEBUG) {
                    Slog.d(SmartDisplayPolicyManager.TAG, "thermal warning changed: " + RefreshRatePolicyController.this.isThermalWarning);
                }
            }

            private void updateSettingRefreshRateStatus() {
                RefreshRatePolicyController.this.updateAppropriateRefreshRate();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ComingInteraction {
        private long mDuration;
        private int mInteractType;
        private int mMaxRefreshRate;
        private int mMinRefreshRate;

        private ComingInteraction(int mMinRefreshRate, int mMaxRefreshRate, int mInteractType, long mDuration) {
            this.mMinRefreshRate = mMinRefreshRate;
            this.mMaxRefreshRate = mMaxRefreshRate;
            this.mInteractType = mInteractType;
            this.mDuration = mDuration;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String interactionTypeToString(int type) {
        switch (type) {
            case 0:
                return "none";
            case 1:
                return "base";
            case 2:
                return "app transition";
            case 3:
                return "window animation";
            case 4:
            default:
                return String.valueOf(type);
            case 5:
                return "recent animation";
            case 6:
                return "remote animation";
            case 7:
                return "focus window changed";
            case 8:
                return "input: down";
            case 9:
                return "input: move";
            case 10:
                return "input: fling";
            case 11:
                return "window visible changed";
            case 12:
                return "multi task: free form";
            case 13:
                return "multi task: split screen";
            case 14:
                return "multi task: cvw gesture";
            case 15:
                return "multi task: multi window";
        }
    }
}
