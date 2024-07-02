package com.android.server.tof;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.IMiuiActivityObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerServiceStub;
import com.android.server.tof.ContactlessGestureController;
import com.android.server.wm.WindowManagerService;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.server.stability.DumpSysInfoUtil;
import com.miui.tof.gesture.TofGestureAppData;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public abstract class ContactlessGestureController {
    public static final int AON_GESTURE_DOWN_STATE = 25;
    public static final int AON_GESTURE_LEFT_STATE = 26;
    public static final int AON_GESTURE_RIGHT_STATE = 27;
    public static final int AON_GESTURE_TRIGGER_DOWN = 21;
    public static final int AON_GESTURE_TRIGGER_LEFT = 22;
    public static final int AON_GESTURE_TRIGGER_RIGHT = 23;
    public static final int AON_GESTURE_TRIGGER_UP = 20;
    public static final int AON_GESTURE_UP_STATE = 24;
    private static final int CHECK_TOP_ACTIVITY_DELAY = 900;
    private static final int DELAY = 500;
    private static final int FLAG_GESTURE_DOWN_STATE_FEATURE = -10;
    private static final int FLAG_GESTURE_INVALIDATE_SCENARIO_FEATURE = -3;
    private static final int FLAG_GESTURE_LEFT_STATE_FEATURE = -12;
    private static final int FLAG_GESTURE_RIGHT_STATE_FEATURE = -11;
    private static final int FLAG_GESTURE_TRIGGER_DOWN_FEATURE = -6;
    private static final int FLAG_GESTURE_TRIGGER_LEFT_FEATURE = -8;
    private static final int FLAG_GESTURE_TRIGGER_RIGHT_FEATURE = -7;
    private static final int FLAG_GESTURE_TRIGGER_UP_FEATURE = -5;
    private static final int FLAG_GESTURE_UN_TRIGGER_FEATURE = -2;
    private static final int FLAG_GESTURE_UP_STATE_FEATURE = -9;
    private static final int FLAG_TOF_GESTURE_TRIGGER_FEATURE = -1;
    private static final int FLAG_TOF_GESTURE_WORKING_FEATURE = -4;
    public static final int GESTURE_GESTURE_WORKING = 14;
    public static final int GESTURE_INVALIDATE_SCENARIO = 13;
    private static final int SCENE_ACTIVITY_FOCUS_CHANGE = 2;
    private static final int SCENE_CALL_STATE_CHANGE = 4;
    private static final int SCENE_DISPLAY_ROTATION_CHANGE = 3;
    private static final int SCENE_DISPLAY_STATE_CHANGE = 5;
    public static final int SCENE_OTHER_CHANGE = 6;
    private static final int SCENE_TOP_ACTIVITY_CHANGE = 1;
    private static final long SERVICE_BIND_AWAIT_MILLIS = 1000;
    private static final String TAG = "ContactlessGestureController";
    public static final int TOF_GESTURE_DOUBLE_PRESS = 7;
    public static final int TOF_GESTURE_DOWN = 3;
    public static final int TOF_GESTURE_DRAW_CIRCLE = 8;
    public static final int TOF_GESTURE_LEFT = 1;
    public static final int TOF_GESTURE_RIGHT = 2;
    public static final int TOF_GESTURE_TRIGGER = 10;
    public static final int TOF_GESTURE_UN_TRIGGER = 12;
    public static final int TOF_GESTURE_UP = 4;
    private AppFeatureHelper mAppFeatureHelper;
    protected ComponentName mComponentName;
    private ContactlessGestureService mContactlessGestureService;
    protected Context mContext;
    private DisplayManager mDisplayManager;
    protected int mDisplayRotation;
    private String mFocusedPackage;
    protected Handler mHandler;
    private boolean mIsDeskTopMode;
    private boolean mIsSplitScreenMode;
    protected boolean mIsTrigger;
    private int mLastTriggerLabel;
    private boolean mRegisterGesture;
    private volatile boolean mServiceIsBinding;
    boolean mShouldUpdateTriggerView;
    private TelephonyManager mTelephonyManager;
    private ComponentName mTopActivity;
    private boolean mTriggerUiSuccess;
    private int mTofGestureLeftSupportFeature = 81940;
    private int mTofGestureRightSupportFeature = 139274;
    private int mTofGestureDownSupportFeature = 5184;
    private int mTofGestureUpSupportFeature = 2592;
    private int mTofGestureDoublePressSupportFeature = 1;
    private int mTofGestureDrawCircleSupportFeature = 384;
    private boolean mInteractive = true;
    private boolean mTopActivityFullScreenOrNotOccluded = true;
    private Runnable mRegisterGestureRunnable = new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            ContactlessGestureController.this.lambda$new$3();
        }
    };
    private Runnable mUpdateTopActivityIfNeededRunnable = new Runnable() { // from class: com.android.server.tof.ContactlessGestureController.1
        @Override // java.lang.Runnable
        public void run() {
            boolean splitScreenModeChange = false;
            ComponentName topActivity = null;
            boolean topActivityFullScreenOrNotOccluded = true;
            try {
                boolean splitScreenMode = ActivityTaskManager.getService().isInSplitScreenWindowingMode();
                if (splitScreenMode != ContactlessGestureController.this.mIsSplitScreenMode) {
                    ContactlessGestureController.this.mIsSplitScreenMode = splitScreenMode;
                    splitScreenModeChange = true;
                }
                List<ActivityTaskManager.RootTaskInfo> infos = ContactlessGestureController.this.mActivityManager.getAllRootTaskInfos();
                Iterator<ActivityTaskManager.RootTaskInfo> it = infos.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ActivityTaskManager.RootTaskInfo info = it.next();
                    if (info.displayId == 0 && info.visible && !Objects.isNull(info.topActivity)) {
                        int windoingMode = info.getWindowingMode();
                        if (!ContactlessGestureController.this.mIsDeskTopMode) {
                            topActivity = info.topActivity;
                            topActivityFullScreenOrNotOccluded = windoingMode == 1;
                        } else if (windoingMode == 1) {
                            topActivity = info.topActivity;
                            break;
                        } else if (topActivityFullScreenOrNotOccluded) {
                            topActivityFullScreenOrNotOccluded = windoingMode == 5;
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.e(ContactlessGestureController.TAG, "cannot getTasks", e);
            }
            if (topActivityFullScreenOrNotOccluded != ContactlessGestureController.this.mTopActivityFullScreenOrNotOccluded || splitScreenModeChange || (topActivity != null && !topActivity.equals(ContactlessGestureController.this.mTopActivity))) {
                if (ContactlessGestureService.mDebug) {
                    Slog.i(ContactlessGestureController.TAG, "top activity status changed, mTopActivityFullScreenOrNotOccluded:" + ContactlessGestureController.this.mTopActivityFullScreenOrNotOccluded + ", mIsDeskTopMode:" + ContactlessGestureController.this.mIsDeskTopMode + ", mTopActivity:" + topActivity + ", mIsSplitScreenMode:" + ContactlessGestureController.this.mIsSplitScreenMode);
                }
                ContactlessGestureController.this.mTopActivity = topActivity;
                ContactlessGestureController.this.mTopActivityFullScreenOrNotOccluded = topActivityFullScreenOrNotOccluded;
                ContactlessGestureController.this.onSceneChanged(1);
            }
        }
    };
    IMiuiActivityObserver mActivityStateObserver = new IMiuiActivityObserver.Stub() { // from class: com.android.server.tof.ContactlessGestureController.3
        public void activityIdle(Intent intent) {
        }

        public void activityResumed(Intent intent) throws RemoteException {
            ContactlessGestureController.this.checkTopActivities();
        }

        public void activityPaused(Intent intent) throws RemoteException {
        }

        public void activityStopped(Intent intent) throws RemoteException {
            ContactlessGestureController.this.checkTopActivities();
        }

        public void activityDestroyed(Intent intent) throws RemoteException {
            ContactlessGestureController.this.checkTopActivities();
        }

        /* JADX WARN: Multi-variable type inference failed */
        public IBinder asBinder() {
            return this;
        }
    };
    private TofClientConnecton mTofClientConnection = new TofClientConnecton();
    private InputHelper mInputHelper = new InputHelper();
    private IActivityManager mActivityManager = ActivityManager.getService();
    private WindowManagerService mWindowManagerService = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
    private WindowManagerPolicy mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    private final TelephonyCallback mTelephonyCallback = new AccessibilityTelephonyCallback();
    private BroadcastReceiver mScreenReceiver = new ScreenStateReceiver();
    private PowerManagerServiceStub mPowerManagerServiceImpl = PowerManagerServiceStub.get();
    protected CountDownLatch mServiceBindingLatch = new CountDownLatch(1);

    public abstract String getAction();

    public abstract ComponentName getTofClientComponent();

    public abstract boolean isServiceInit();

    public abstract void notifyRotationChanged(int i);

    public abstract void onGestureServiceConnected(ComponentName componentName, IBinder iBinder);

    public abstract void registerGestureListenerIfNeeded(boolean z);

    public abstract void restContactlessGestureService();

    public abstract void showGestureNotification();

    public abstract boolean updateGestureHint(int i);

    /* JADX WARN: Multi-variable type inference failed */
    public ContactlessGestureController(Context context, Handler handler, ContactlessGestureService contactlessGestureService) {
        this.mContext = context;
        this.mHandler = new Handler(handler.getLooper());
        this.mContactlessGestureService = contactlessGestureService;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        initTofGestureConfig();
        AppFeatureHelper appFeatureHelper = AppFeatureHelper.getInstance();
        this.mAppFeatureHelper = appFeatureHelper;
        appFeatureHelper.initTofComponentConfig(this.mContext);
        this.mComponentName = getTofClientComponent();
        registerListenerIfNeeded();
    }

    private void initTofGestureConfig() {
        this.mTofGestureUpSupportFeature = this.mContext.getResources().getInteger(285933615);
        this.mTofGestureDownSupportFeature = this.mContext.getResources().getInteger(285933611);
        this.mTofGestureLeftSupportFeature = this.mContext.getResources().getInteger(285933613);
        this.mTofGestureRightSupportFeature = this.mContext.getResources().getInteger(285933614);
        this.mTofGestureDoublePressSupportFeature = this.mContext.getResources().getInteger(285933610);
        this.mTofGestureDrawCircleSupportFeature = this.mContext.getResources().getInteger(285933612);
    }

    public void bindService() {
        bindService(this.mComponentName);
        awaitServiceBinding();
    }

    public void unBindService() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ContactlessGestureController.this.lambda$unBindService$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$unBindService$0() {
        this.mContext.unbindService(this.mTofClientConnection);
    }

    public void bindService(ComponentName name) {
        if (!isServiceInit() && name != null && !this.mServiceIsBinding) {
            Slog.i(TAG, "bindService");
            this.mServiceIsBinding = true;
            Intent intent = new Intent(getAction());
            intent.setComponent(name);
            intent.addFlags(SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX);
            try {
                if (!this.mContext.bindServiceAsUser(intent, this.mTofClientConnection, 67108865, UserHandle.CURRENT)) {
                    Slog.e(TAG, "unable to bind tof service: " + intent);
                }
            } catch (SecurityException ex) {
                Slog.e(TAG, "unable to bind tof service: " + intent, ex);
            }
        }
    }

    private void registerListenerIfNeeded() {
        monitorActivityChanges();
        registerCallStateChange();
        registerDisplayListener();
        registerScreenReceiver();
        updateDefaultDisplayRotation();
    }

    public void onSceneChanged(int scene) {
        if (!this.mInteractive || ((this.mContactlessGestureService.isSupportTofGestureFeature() && this.mDisplayRotation != 1) || this.mContactlessGestureService.isScreenCastMode() || this.mContactlessGestureService.isTalkBackEnabled() || this.mContactlessGestureService.isOneHandedModeEnabled())) {
            registerListenerDelayIfNeeded(false);
            return;
        }
        int callState = this.mTelephonyManager.getCallState();
        if (callState == 1 || callState == 2) {
            registerListenerDelayIfNeeded(true, true);
            return;
        }
        if (this.mIsSplitScreenMode || !this.mTopActivityFullScreenOrNotOccluded) {
            registerListenerDelayIfNeeded(false);
            return;
        }
        AppFeatureHelper appFeatureHelper = this.mAppFeatureHelper;
        ComponentName componentName = this.mTopActivity;
        int i = this.mDisplayRotation;
        boolean isSupported = appFeatureHelper.getSupportFeature(componentName, i == 0 || i == 2) > 0;
        if (ContactlessGestureService.mDebug) {
            Slog.i(TAG, "isSupported:" + isSupported + (this.mTopActivity != null ? ", component: " + this.mTopActivity.toString() : ""));
        }
        registerListenerDelayIfNeeded(isSupported);
        if (isSupported && getCurrentSupportFeature() != 0) {
            if (this.mContactlessGestureService.mAonGestureEnabled) {
                this.mContactlessGestureService.startGestureClientIfNeeded();
                updateGestureHint(14);
                if (scene != 1 && scene != 3) {
                    showLastTriggerViewIfNeeded();
                    this.mShouldUpdateTriggerView = false;
                    return;
                }
                return;
            }
            return;
        }
        updateGestureHint(13);
        this.mShouldUpdateTriggerView = true;
    }

    private void showLastTriggerViewIfNeeded() {
        if (this.mIsTrigger && this.mShouldUpdateTriggerView && isStateOrTriggerLabelSupport(this.mLastTriggerLabel, getCurrentSupportFeature())) {
            Slog.i(TAG, "show last trigger view, mLastTriggerLabel:" + this.mLastTriggerLabel);
            updateGestureHint(this.mLastTriggerLabel);
        }
    }

    public void updateDeskTopMode(boolean enable) {
        this.mIsDeskTopMode = enable;
    }

    public void updateFocusedPackage(String packageName) {
        this.mFocusedPackage = packageName;
        if (!isTopActivityFocused()) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureController.this.lambda$updateFocusedPackage$1();
                }
            });
        } else {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureController.this.lambda$updateFocusedPackage$2();
                }
            }, 1800L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateFocusedPackage$1() {
        onSceneChanged(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateFocusedPackage$2() {
        this.mUpdateTopActivityIfNeededRunnable.run();
        onSceneChanged(2);
    }

    private boolean isTopActivityFocused() {
        ComponentName componentName;
        String str = this.mFocusedPackage;
        if (str == null || (componentName = this.mTopActivity) == null) {
            return true;
        }
        boolean topActivityFocused = str.equals(componentName.getPackageName());
        return topActivityFocused;
    }

    private boolean isKeyguardShowing() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDefaultDisplayRotation() {
        int currentRotation = this.mWindowManagerService.getDefaultDisplayRotation();
        if (currentRotation != this.mDisplayRotation) {
            this.mDisplayRotation = currentRotation;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$3() {
        this.mContactlessGestureService.registerContactlessGestureListenerIfNeeded(this.mRegisterGesture);
    }

    private void registerListenerDelayIfNeeded(boolean register) {
        registerListenerDelayIfNeeded(register, false);
    }

    private void registerListenerDelayIfNeeded(boolean register, boolean immediate) {
        this.mRegisterGesture = register;
        if (!register) {
            updateGestureHint(13);
        }
        this.mHandler.removeCallbacks(this.mRegisterGestureRunnable);
        if (immediate) {
            this.mHandler.post(this.mRegisterGestureRunnable);
        } else {
            this.mHandler.postDelayed(this.mRegisterGestureRunnable, 500L);
        }
    }

    public void handleGestureEvent(int label) {
        Slog.i(TAG, "sensorEvent:" + gestureLabelToString(label) + " mIsTrigger:" + this.mIsTrigger);
        dismissTriggerViewIfNeeded(label);
        if (!this.mRegisterGesture) {
            return;
        }
        int appSupportFeature = getCurrentSupportFeature();
        if (label == 12) {
            this.mIsTrigger = false;
        } else if (isGestureStateLabel(label) || isTriggerLabel(label)) {
            this.mLastTriggerLabel = label;
            if (appSupportFeature == 0 && isTriggerLabel(label)) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContactlessGestureController.this.lambda$handleGestureEvent$4();
                    }
                });
            }
        }
        if (appSupportFeature != 0) {
            int feature = 0;
            switch (label) {
                case 1:
                    feature = appSupportFeature & this.mTofGestureLeftSupportFeature;
                    break;
                case 2:
                    feature = appSupportFeature & this.mTofGestureRightSupportFeature;
                    break;
                case 3:
                    feature = appSupportFeature & this.mTofGestureDownSupportFeature;
                    break;
                case 4:
                    feature = appSupportFeature & this.mTofGestureUpSupportFeature;
                    break;
                case 7:
                    feature = appSupportFeature & this.mTofGestureDoublePressSupportFeature;
                    break;
                case 8:
                    feature = appSupportFeature & this.mTofGestureDrawCircleSupportFeature;
                    break;
                case 10:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                    showGestureViewIfNeeded(label, appSupportFeature);
                    break;
                case 12:
                    updateGestureHint(label);
                    break;
            }
            if (label != 12) {
                ComponentName componentName = this.mTopActivity;
                String pkg = componentName == null ? null : componentName.getPackageName();
                boolean success = feature > 0;
                this.mPowerManagerServiceImpl.notifyGestureEvent(pkg, success, label);
            }
            boolean success2 = this.mIsTrigger;
            if (success2 && feature > 0) {
                updateGestureHint(label);
                this.mInputHelper.injectSupportInputEvent(feature);
                Slog.i(TAG, "handleTofGesture feature:0x" + Integer.toHexString(feature) + ",mIsTrigger:" + this.mIsTrigger);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$handleGestureEvent$4() {
        registerGestureListenerIfNeeded(false);
    }

    private void showGestureViewIfNeeded(int label, int currentSupportFeature) {
        if (!showTrigger(label, currentSupportFeature) && !showFakeTrigger(label, currentSupportFeature)) {
            showGestureState(label, currentSupportFeature);
        }
    }

    private void showGestureState(int label, int currentSupportFeature) {
        if (isGestureStateLabel(label) && isStateOrTriggerLabelSupport(label, currentSupportFeature)) {
            updateGestureHint(label);
        }
    }

    private boolean isStateOrTriggerLabelSupport(int label, int currentSupportFeature) {
        int feature = 0;
        switch (label) {
            case 10:
            case 20:
            case 24:
                feature = currentSupportFeature & this.mTofGestureDownSupportFeature;
                if (currentSupportFeature == 128 || currentSupportFeature == 256) {
                    feature = currentSupportFeature;
                    break;
                }
                break;
            case 21:
            case 25:
                feature = currentSupportFeature & this.mTofGestureUpSupportFeature;
                break;
            case 22:
            case 26:
                feature = currentSupportFeature & this.mTofGestureRightSupportFeature;
                break;
            case 23:
            case 27:
                feature = currentSupportFeature & this.mTofGestureLeftSupportFeature;
                break;
        }
        return feature > 0;
    }

    private boolean showFakeTrigger(int label, int currentSupportFeature) {
        int fakeLabel = -1;
        if (!this.mIsTrigger) {
            switch (label) {
                case 24:
                    fakeLabel = 20;
                    break;
                case 25:
                    fakeLabel = 21;
                    break;
                case 26:
                    fakeLabel = 22;
                    break;
                case 27:
                    fakeLabel = 23;
                    break;
            }
            if (fakeLabel > 0) {
                return showTrigger(fakeLabel, currentSupportFeature);
            }
            return false;
        }
        return false;
    }

    private boolean showTrigger(int label, int currentSupportFeature) {
        if (isTriggerLabel(label) && isStateOrTriggerLabelSupport(label, currentSupportFeature)) {
            this.mIsTrigger = updateGestureHint(label);
            return true;
        }
        return false;
    }

    private boolean isGestureStateLabel(int label) {
        if (label == 24 || label == 25 || label == 26 || label == 27) {
            return true;
        }
        return false;
    }

    private boolean isTriggerLabel(int label) {
        if (label == 10 || label == 20 || label == 21 || label == 22 || label == 23) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentSupportFeature() {
        TofGestureComponent component;
        switch (this.mTelephonyManager.getCallState()) {
            case 1:
                return 128;
            case 2:
                return 256;
            default:
                if (isKeyguardShowing() || !isTopActivityFocused() || (component = this.mAppFeatureHelper.getSupportComponentFromConfig(this.mTopActivity)) == null) {
                    return 0;
                }
                int i = this.mDisplayRotation;
                int appSupportFeature = (i == 1 || i == 3) ? component.getLandscapeFeature() : component.getPortraitFeature();
                return appSupportFeature;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentSupportGesture() {
        int gestureSupport = 0;
        int appSupportFeature = getCurrentSupportFeature();
        if ((this.mTofGestureUpSupportFeature & appSupportFeature) != 0 || (this.mTofGestureDownSupportFeature & appSupportFeature) != 0) {
            gestureSupport = 0 | 1;
        }
        if ((this.mTofGestureLeftSupportFeature & appSupportFeature) != 0 || (this.mTofGestureRightSupportFeature & appSupportFeature) != 0) {
            gestureSupport |= 2;
        }
        if ((this.mTofGestureDoublePressSupportFeature & appSupportFeature) != 0) {
            gestureSupport |= 4;
        }
        if ((this.mTofGestureDrawCircleSupportFeature & appSupportFeature) != 0) {
            return gestureSupport | 8;
        }
        return gestureSupport;
    }

    public String getCurrentPkg() {
        ComponentName componentName = this.mTopActivity;
        if (componentName != null) {
            return componentName.getPackageName();
        }
        return null;
    }

    public int getCurrentAppType() {
        TofGestureComponent component = this.mAppFeatureHelper.getSupportComponentFromConfig(this.mTopActivity);
        if (component == null) {
            return 0;
        }
        return component.getCategory();
    }

    private void dismissTriggerViewIfNeeded(int label) {
        if (label == 12) {
            updateGestureHint(label);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkTopActivities() {
        this.mHandler.removeCallbacks(this.mUpdateTopActivityIfNeededRunnable);
        this.mHandler.postDelayed(this.mUpdateTopActivityIfNeededRunnable, 900L);
    }

    private void monitorActivityChanges() {
        try {
            this.mActivityManager.registerActivityObserver(this.mActivityStateObserver, new Intent());
        } catch (RemoteException e) {
            Slog.e(TAG, "cannot register activity monitoring", e);
        }
        checkTopActivities();
    }

    private void registerCallStateChange() {
        this.mTelephonyManager.registerTelephonyCallback(new HandlerExecutor(this.mHandler), this.mTelephonyCallback);
    }

    private void registerScreenReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.setPriority(1000);
        this.mContext.registerReceiver(this.mScreenReceiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.tof.ContactlessGestureController$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements DisplayManager.DisplayListener {
        AnonymousClass2() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            ContactlessGestureController.this.updateDefaultDisplayRotation();
            ContactlessGestureController.this.checkTopActivities();
            ContactlessGestureController.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureController.AnonymousClass2.this.lambda$onDisplayChanged$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDisplayChanged$0() {
            ContactlessGestureController.this.onSceneChanged(3);
        }
    }

    private void registerDisplayListener() {
        this.mDisplayManager.registerDisplayListener(new AnonymousClass2(), this.mHandler);
    }

    public void parseGestureComponentFromCloud(List<String> componentFeatureList) {
        this.mAppFeatureHelper.parseContactlessGestureComponentFromCloud(componentFeatureList);
    }

    public boolean changeAppConfigWithPackageName(String pkg, boolean enable) {
        return this.mAppFeatureHelper.changeTofGestureAppConfig(pkg, enable);
    }

    public TofGestureAppData getTofGestureAppData() {
        return this.mAppFeatureHelper.getTofGestureAppData();
    }

    /* loaded from: classes.dex */
    private final class AccessibilityTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        private AccessibilityTelephonyCallback() {
        }

        @Override // android.telephony.TelephonyCallback.CallStateListener
        public void onCallStateChanged(int state) {
            Slog.i(ContactlessGestureController.TAG, " call state changed, state:" + state);
            ContactlessGestureController.this.onSceneChanged(4);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFeatureFromLabel(int label) {
        switch (label) {
            case 1:
                int labelFeature = this.mTofGestureLeftSupportFeature;
                return labelFeature;
            case 2:
                int labelFeature2 = this.mTofGestureRightSupportFeature;
                return labelFeature2;
            case 3:
                int labelFeature3 = this.mTofGestureDownSupportFeature;
                return labelFeature3;
            case 4:
                int labelFeature4 = this.mTofGestureUpSupportFeature;
                return labelFeature4;
            case 5:
            case 6:
            case 9:
            case 11:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            default:
                return -3;
            case 7:
                int labelFeature5 = this.mTofGestureDoublePressSupportFeature;
                return labelFeature5;
            case 8:
                int labelFeature6 = this.mTofGestureDrawCircleSupportFeature;
                return labelFeature6;
            case 10:
                return -1;
            case 12:
                return -2;
            case 13:
                return -3;
            case 14:
                return -4;
            case 20:
                return -5;
            case 21:
                return -6;
            case 22:
                return FLAG_GESTURE_TRIGGER_LEFT_FEATURE;
            case 23:
                return FLAG_GESTURE_TRIGGER_RIGHT_FEATURE;
            case 24:
                return FLAG_GESTURE_UP_STATE_FEATURE;
            case 25:
                return FLAG_GESTURE_DOWN_STATE_FEATURE;
            case 26:
                return FLAG_GESTURE_LEFT_STATE_FEATURE;
            case 27:
                return FLAG_GESTURE_RIGHT_STATE_FEATURE;
        }
    }

    public String gestureLabelToString(int label) {
        switch (label) {
            case 1:
                return "GESTURE_LEFT";
            case 2:
                return "GESTURE_RIGHT";
            case 3:
                return "GESTURE_DOWN";
            case 4:
                return "GESTURE_UP";
            case 5:
            case 6:
            case 9:
            case 11:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            default:
                String gesture = "UNKNOWN label:" + label;
                return gesture;
            case 7:
                return "GESTURE_DOUBLE_PRESS";
            case 8:
                return "GESTURE_DRAW_CIRCLE";
            case 10:
                return "GESTURE_TRIGGER";
            case 12:
                return "GESTURE_UN_TRIGGER";
            case 13:
                return "INVALIDATE_SCENARIO";
            case 14:
                return "GESTURE_GESTURE_WORKING";
            case 20:
                return "GESTURE_TRIGGER_UP";
            case 21:
                return "GESTURE_TRIGGER_DOWN";
            case 22:
                return "GESTURE_TRIGGER_LEFT";
            case 23:
                return "GESTURE_TRIGGER_RIGHT";
            case 24:
                return "GESTURE_UP_STATE";
            case 25:
                return "GESTURE_DOWN_STATE";
            case 26:
                return "GESTURE_LEFT_STATE";
            case 27:
                return "GESTURE_RIGHT_STATE";
        }
    }

    private void awaitServiceBinding() {
        try {
            this.mServiceBindingLatch.await(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.e(TAG, "Interrupted while waiting to bind contactless gesture client Service.", e);
        }
        this.mServiceIsBinding = false;
    }

    public void dump(PrintWriter pw) {
        pw.println("Contactless Gesture Controller:");
        pw.println("    gesture client: " + getTofClientComponent());
        pw.println("    gesture client connected: " + (isServiceInit() ? "true" : "false"));
        pw.println("    register: " + this.mRegisterGesture);
        pw.println("    interactive: " + this.mInteractive);
        pw.println("    isTrigger: " + this.mIsTrigger);
        pw.println("    mLastTriggerLabel: " + this.mLastTriggerLabel);
        StringBuilder append = new StringBuilder().append("    focused package: ");
        String str = this.mFocusedPackage;
        if (str == null) {
            str = "";
        }
        pw.println(append.append(str).toString());
        pw.println("    display rotation: " + this.mDisplayRotation);
        pw.println("    desk top mode enable: " + this.mIsDeskTopMode);
        pw.println("    split screen mode: " + this.mIsSplitScreenMode);
        pw.println("    isScreenCastMode: " + this.mContactlessGestureService.isScreenCastMode());
        StringBuilder append2 = new StringBuilder().append("    top activity: ");
        ComponentName componentName = this.mTopActivity;
        pw.println(append2.append(componentName != null ? componentName.toString() : "").toString());
        pw.println("    resConfig: " + this.mAppFeatureHelper.getResGestureConfig());
        pw.println("    cloudConfig: " + this.mAppFeatureHelper.getCloudGestureConfig());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ScreenStateReceiver extends BroadcastReceiver {
        private ScreenStateReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                ContactlessGestureController.this.mInteractive = false;
            } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                ContactlessGestureController.this.mInteractive = true;
            }
            ContactlessGestureController.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ContactlessGestureController$ScreenStateReceiver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContactlessGestureController.ScreenStateReceiver.this.lambda$onReceive$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            ContactlessGestureController.this.onSceneChanged(5);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TofClientConnecton implements IBinder.DeathRecipient, ServiceConnection {
        private TofClientConnecton() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (ContactlessGestureController.this.mTofClientConnection == this) {
                ContactlessGestureController.this.onGestureServiceConnected(name, service);
                ContactlessGestureController.this.mServiceIsBinding = false;
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(ContactlessGestureController.TAG, "onServiceDisconnected");
            ContactlessGestureController.this.restContactlessGestureService();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.i(ContactlessGestureController.TAG, "binder died");
            ContactlessGestureController.this.restContactlessGestureService();
        }
    }
}
