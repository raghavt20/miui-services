package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings;
import android.util.Slog;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.TransitionRequestInfo;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.server.LocalServices;
import com.android.server.MiuiBgThread;
import com.android.server.PowerConsumptionServiceInternal;
import com.android.server.camera.CameraOpt;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.DisplayContentStub;
import com.android.server.wm.WindowManagerService;
import com.miui.base.MiuiStubRegistry;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import com.miui.server.input.AutoDisableScreenButtonsManager;
import com.miui.server.input.knock.MiuiKnockGestureService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.ToIntFunction;
import miui.hardware.display.DisplayFeatureManager;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.GeneralExceptionEvent;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.util.ReflectionUtils;

/* loaded from: classes.dex */
public class DisplayContentStubImpl implements DisplayContentStub {
    private static final String DESCRIPTOR = "miui.systemui.keyguard.Wallpaper";
    private static final ArrayList<String> DISPLAY_DISABLE_SYSTEM_ANIMATION;
    private static final int FOCUSED_WINDOW_RETRY_TIME = 3000;
    private static final int FPS_COMMON = 60;
    private static final int NO_FOCUS_WINDOW_TIMEOUT = 5000;
    private static final String PROVISION_CONGRATULATION_ACTIVITY = "com.android.provision/.activities.CongratulationActivity";
    private static final String PROVISION_DEFAULT_ACTIVITY = "com.android.provision/.activities.DefaultActivity";
    private static final int SCREEN_DPI_MODE = 24;
    private static final ArrayList<String> WHITE_LIST_ALLOW_FIXED_ROTATION_FOR_NOT_OCCLUDES_PARENT;
    private int mBlur;
    private Context mContext;
    private PowerConsumptionServiceInternal mPowerConsumptionServiceInternal;
    public static int sLastUserRefreshRate = -1;
    public static int sCurrentRefreshRate = -1;
    private final String TAG = "DisplayContentStubImpl";
    private boolean mStatusBarVisible = true;
    private boolean mSkipVisualDisplayTransition = false;
    private final boolean blurEnabled = SystemProperties.getBoolean("persist.sys.background_blur_status_default", false);
    private final String KEY_BLUR_ENABLE = "background_blur_enable";
    private UpdateFocusRunnable mFocusRunnable = null;
    private boolean mSkipUpdateSurfaceRotation = false;
    private boolean mIsCameraRotating = false;
    private HashSet<WindowState> mHiddenWindowList = new HashSet<>();
    private long mTimestamp = 0;

    /* loaded from: classes.dex */
    public static final class MutableDisplayContentImpl implements DisplayContentStub.MutableDisplayContentStub {
        private DisplayContent displayContent;

        /* loaded from: classes.dex */
        public final class Provider implements MiuiStubRegistry.ImplProvider<MutableDisplayContentImpl> {

            /* compiled from: DisplayContentStubImpl$MutableDisplayContentImpl$Provider.java */
            /* loaded from: classes.dex */
            public static final class SINGLETON {
                public static final MutableDisplayContentImpl INSTANCE = new MutableDisplayContentImpl();
            }

            /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
            public MutableDisplayContentImpl m2463provideSingleton() {
                return SINGLETON.INSTANCE;
            }

            /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
            public MutableDisplayContentImpl m2462provideNewInstance() {
                return new MutableDisplayContentImpl();
            }
        }

        public void init(DisplayContent displayContent) {
            this.displayContent = displayContent;
        }

        public void cancelAppAnimationIfNeeded(ActivityRecord mFixedRotationLaunchingApp, AppTransition mAppTransition, String TAG) {
            if (mFixedRotationLaunchingApp.isAnimating(2, 1)) {
                Slog.d(TAG, "finishFixedRotation still animating " + mFixedRotationLaunchingApp);
                WindowContainer wc = mFixedRotationLaunchingApp.getAnimatingContainer(2, 1);
                if (mAppTransition.mLastOpeningAnimationTargets.contains(wc) || mAppTransition.mLastClosingAnimationTargets.contains(wc)) {
                    Iterator it = mAppTransition.mLastOpeningAnimationTargets.iterator();
                    while (it.hasNext()) {
                        WindowContainer animatingContainer = (WindowContainer) it.next();
                        animatingContainer.cancelAnimation();
                        if (ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                            Slog.d(TAG, "finishFixedRotation cancelAnimation " + animatingContainer);
                        }
                    }
                    Iterator it2 = mAppTransition.mLastClosingAnimationTargets.iterator();
                    while (it2.hasNext()) {
                        WindowContainer animatingContainer2 = (WindowContainer) it2.next();
                        animatingContainer2.cancelAnimation();
                        if (ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                            Slog.d(TAG, "finishFixedRotation cancelAnimation " + animatingContainer2);
                        }
                    }
                }
            }
        }

        public boolean allowFixedRotationForNotOccludesParent(ActivityRecord r, String TAG) {
            if (r != null && DisplayContentStubImpl.WHITE_LIST_ALLOW_FIXED_ROTATION_FOR_NOT_OCCLUDES_PARENT.contains(r.shortComponentName)) {
                return true;
            }
            return false;
        }

        public boolean isNoAnimation() {
            DisplayContent displayContent = this.displayContent;
            if (displayContent == null || displayContent.getDisplayInfo() == null) {
                return false;
            }
            return DisplayContentStubImpl.DISPLAY_DISABLE_SYSTEM_ANIMATION.contains(this.displayContent.getDisplayInfo().name) || (this.displayContent.getDisplayInfo().flags & 1048576) != 0;
        }
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayContentStubImpl> {

        /* compiled from: DisplayContentStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayContentStubImpl INSTANCE = new DisplayContentStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayContentStubImpl m2465provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayContentStubImpl m2464provideNewInstance() {
            return new DisplayContentStubImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        WHITE_LIST_ALLOW_FIXED_ROTATION_FOR_NOT_OCCLUDES_PARENT = arrayList;
        ArrayList<String> arrayList2 = new ArrayList<>();
        DISPLAY_DISABLE_SYSTEM_ANIMATION = arrayList2;
        arrayList.add("com.miui.mediaviewer/com.miui.video.gallery.galleryvideo.FrameLocalPlayActivity");
        arrayList.add("com.miui.gallery/.activity.InternalPhotoPageActivity");
        arrayList2.add("com.miui.carlink");
        arrayList2.add("com.xiaomi.ucar.minimap");
    }

    public void onSystemReady() {
        this.mPowerConsumptionServiceInternal = (PowerConsumptionServiceInternal) LocalServices.getService(PowerConsumptionServiceInternal.class);
    }

    public void displayReady(Context ctx) {
        this.mContext = ctx;
        computeBlurConfiguration(ctx);
    }

    public void registerSettingsObserver(Context ctx, WindowManagerService.SettingsObserver settingsObserver) {
        ContentResolver resolver = ctx.getContentResolver();
        resolver.registerContentObserver(Settings.Secure.getUriFor("background_blur_enable"), false, settingsObserver, -1);
    }

    public boolean onSettingsObserverChange(boolean selfChange, Uri uri) {
        if (uri != null && Settings.Secure.getUriFor("background_blur_enable").equals(uri)) {
            computeBlurConfiguration(this.mContext);
            return true;
        }
        return false;
    }

    private void computeBlurConfiguration(Context ctx) {
        if (ctx == null) {
            return;
        }
        int blur = Settings.Secure.getInt(ctx.getContentResolver(), "background_blur_enable", -1);
        if (blur == 1) {
            this.mBlur = 1;
        } else if (blur == 0) {
            this.mBlur = 2;
        } else if (this.blurEnabled) {
            this.mBlur = 1;
        } else {
            this.mBlur = 0;
        }
        Slog.d("DisplayContentStubImpl", "computeBlurConfiguration blurEnabled= " + this.blurEnabled + " blur= " + blur + " mBlur= " + this.mBlur);
    }

    public void setBlur(Configuration current, Configuration tmp) {
        if (current.blur == 0) {
            tmp.blur = this.mBlur;
        }
    }

    static int getFullScreenIndex(Task stack, WindowList<Task> children, int targetPosition) {
        if (stack.getWindowingMode() == 1) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Task tStack = (Task) it.next();
                if (tStack.getWindowingMode() == 5 && children.indexOf(tStack) > 0) {
                    return children.indexOf(tStack) - 1;
                }
            }
            return targetPosition;
        }
        return targetPosition;
    }

    static int getFullScreenIndex(boolean toTop, Task stack, WindowList<Task> children, int targetPosition, boolean adding) {
        if (toTop && stack.getWindowingMode() == 1) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Task tStack = (Task) it.next();
                if (tStack.getWindowingMode() == 5) {
                    int topChildPosition = children.indexOf(tStack);
                    return adding ? topChildPosition : topChildPosition > 0 ? topChildPosition - 1 : 0;
                }
            }
            return targetPosition;
        }
        return targetPosition;
    }

    static void updateRefreshRateIfNeed(boolean isInMultiWindow) {
        int currentFps = getCurrentRefreshRate();
        if (sCurrentRefreshRate != currentFps) {
            sCurrentRefreshRate = currentFps;
        }
        if (isInMultiWindow) {
            int i = sCurrentRefreshRate;
            if (i > 60) {
                sLastUserRefreshRate = i;
                setCurrentRefreshRate(60);
                return;
            }
            return;
        }
        int i2 = sLastUserRefreshRate;
        if (i2 >= 60) {
            setCurrentRefreshRate(i2);
            sLastUserRefreshRate = -1;
        }
    }

    private static int getCurrentRefreshRate() {
        int fps = SystemProperties.getInt("persist.vendor.dfps.level", 60);
        int powerFps = SystemProperties.getInt("persist.vendor.power.dfps.level", 0);
        if (powerFps != 0) {
            return powerFps;
        }
        return fps;
    }

    private static void setCurrentRefreshRate(int fps) {
        sCurrentRefreshRate = fps;
        DisplayFeatureManager.getInstance().setScreenEffect(24, fps);
    }

    public int compare(WindowToken token1, WindowToken token2) {
        try {
            if (token1.windowType == token2.windowType && token1.windowType == 2013) {
                if (token1.token != null && DESCRIPTOR.equals(token1.token.getInterfaceDescriptor())) {
                    return 1;
                }
                if (token2.token != null) {
                    if (DESCRIPTOR.equals(token2.token.getInterfaceDescriptor())) {
                        return -1;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.wm.DisplayContentStubImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int windowLayerFromType;
                windowLayerFromType = ((WindowToken) obj).getWindowLayerFromType();
                return windowLayerFromType;
            }
        }).compare(token1, token2);
    }

    public void finishLayoutLw(WindowManagerPolicy policy, DisplayContent displayContent, DisplayFrames displayFrames) {
        if (policy instanceof BaseMiuiPhoneWindowManager) {
            Rect inputMethodWindowRegion = getInputMethodWindowVisibleRegion(displayContent);
            ((BaseMiuiPhoneWindowManager) policy).finishLayoutLw(displayFrames, inputMethodWindowRegion, displayContent.mDisplayId);
            WindowState statusBar = displayContent.getDisplayPolicy().getStatusBar();
            if (statusBar != null && this.mStatusBarVisible != statusBar.isVisible()) {
                boolean isVisible = statusBar.isVisible();
                this.mStatusBarVisible = isVisible;
                AutoDisableScreenButtonsManager.onStatusBarVisibilityChangeStatic(isVisible);
                MiuiKnockGestureService.finishPostLayoutPolicyLw(this.mStatusBarVisible);
            }
        }
    }

    private Rect getInputMethodWindowVisibleRegion(DisplayContent displayContent) {
        InsetsState state = displayContent.getInsetsStateController().getRawInsetsState();
        InsetsSource imeSource = state.peekSource(InsetsSource.ID_IME);
        if (imeSource == null || !imeSource.isVisible()) {
            return new Rect(0, 0, 0, 0);
        }
        Rect imeFrame = imeSource.getVisibleFrame() != null ? imeSource.getVisibleFrame() : imeSource.getFrame();
        Rect dockFrame = (Rect) ReflectionUtils.tryGetObjectField(displayContent, "mTmpRect", Rect.class).get();
        dockFrame.set(state.getDisplayFrame());
        dockFrame.inset(state.calculateInsets(dockFrame, WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout(), false));
        return new Rect(dockFrame.left, imeFrame.top, dockFrame.right, dockFrame.bottom);
    }

    public boolean isSubDisplayOff(DisplayContent display) {
        return display != null && DeviceFeature.IS_SUBSCREEN_DEVICE && display.getDisplayId() == 2 && display.getDisplay().getState() == 1;
    }

    public boolean isSubDisplay(int displayId) {
        return DeviceFeature.IS_SUBSCREEN_DEVICE && displayId == 2;
    }

    public void focusChangedLw(int displayId, final WindowState focus) {
        if (focus != null) {
            CameraOpt.callMethod("notifyFocusWindowChanged", Integer.valueOf(focus.getPid()), Integer.valueOf(focus.getUid()), Integer.valueOf(displayId), focus.getOwningPackage());
        }
        if (displayId != 0) {
            return;
        }
        if (focus == null) {
            this.mTimestamp = SystemClock.uptimeMillis();
            return;
        }
        if (this.mTimestamp != 0) {
            long timestamp = this.mTimestamp;
            this.mTimestamp = 0L;
            final long durationMillis = SystemClock.uptimeMillis() - timestamp;
            if (durationMillis > 5000) {
                MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.DisplayContentStubImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        DisplayContentStubImpl.this.lambda$focusChangedLw$0(focus, durationMillis);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: trackNoFocusWindowTimeout, reason: merged with bridge method [inline-methods] */
    public void lambda$focusChangedLw$0(WindowState focus, long durationMillis) {
        try {
            String packageName = focus.getOwningPackage();
            String window = focus.getWindowTag().toString();
            if (!Objects.equals(window, packageName) && window.startsWith(packageName)) {
                window = window.substring(packageName.length() + 1);
            }
            GeneralExceptionEvent event = new GeneralExceptionEvent();
            event.setType(434);
            event.setPackageName(packageName);
            event.setTimeStamp(System.currentTimeMillis());
            event.setSummary("no focus window timeout");
            event.setDetails("window=" + window + " duration=" + durationMillis + "ms");
            event.setPid((int) durationMillis);
            MQSEventManagerDelegate.getInstance().reportGeneralException(event);
            MiEvent miEvent = new MiEvent(901003101);
            miEvent.addStr("PackageName", packageName);
            miEvent.addStr("WindowName", window);
            miEvent.addInt("Duration", (int) durationMillis);
            MiSight.sendEvent(miEvent);
        } catch (Exception e) {
        }
    }

    public void initOneTrackRotationHelper() {
        OneTrackRotationHelper.getInstance().init();
    }

    public void reportDisplayRotationChanged(int displayId, int rotation) {
        OneTrackRotationHelper.getInstance().reportRotationChanged(displayId, rotation);
    }

    public boolean attachToDisplayCompatMode(WindowState mImeLayeringTarget) {
        Task task;
        if (mImeLayeringTarget == null || mImeLayeringTarget.mActivityRecord == null || (task = mImeLayeringTarget.mActivityRecord.getTask()) == null || !task.mDisplayCompatAvailable) {
            return false;
        }
        return true;
    }

    public boolean needEnsureVisible(DisplayContent dc, Task task) {
        return dc == null || task == null || task.affinity == null || !isSubDisplayOff(dc) || !task.affinity.contains("com.xiaomi.misubscreenui");
    }

    public boolean shouldInterceptRelaunch(int vendorId, int productId) {
        return MiuiPadKeyboardManager.isXiaomiKeyboard(vendorId, productId);
    }

    public boolean isSupportedImeSnapShot() {
        return DeviceFeature.SUPPORT_IME_SNAPSHOT;
    }

    public boolean skipImeWindowsForMiui(DisplayContent display) {
        if (ActivityTaskManagerServiceImpl.getInstance().isVerticalSplit()) {
            return !display.mAtmService.isInSplitScreenWindowingMode();
        }
        return true;
    }

    public void noteFoucsChangeForPowerConsumptionIfNeeded(WindowState w) {
        PowerConsumptionServiceInternal powerConsumptionServiceInternal;
        WindowManager.LayoutParams mAttrs = w.mAttrs;
        if (mAttrs != null) {
            CharSequence tag = mAttrs.getTitle();
            if (tag.toString() != null && w.mMiuiWindowStateEx != null && (powerConsumptionServiceInternal = this.mPowerConsumptionServiceInternal) != null) {
                powerConsumptionServiceInternal.noteFoucsChangeForPowerConsumption(tag.toString(), w.mMiuiWindowStateEx);
            }
        }
    }

    public void onDisplayOverrideConfigUpdate(DisplayContent displayContent) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null) {
            miuiHoverIn.onDisplayOverrideConfigUpdate(displayContent);
        }
    }

    public boolean needSkipVisualDisplayTransition(int displayId) {
        if (displayId != 0) {
            return this.mSkipVisualDisplayTransition;
        }
        return false;
    }

    public void updateSkipVisualDisplayTransition(int displayId, String tokenTag, boolean shouldSkip) {
        if (displayId != 0 && tokenTag.equals("Display-off")) {
            if (shouldSkip) {
                this.mSkipVisualDisplayTransition = true;
            } else {
                this.mSkipVisualDisplayTransition = false;
            }
        }
    }

    public boolean forceEnableSeamless(WindowState w) {
        if (w != null && w.mAttrs != null && (w.mAttrs.extraFlags & 1073741824) != 0) {
            return true;
        }
        return false;
    }

    public void onFocusedWindowChanged(DisplayContent display) {
        if (!display.isDefaultDisplay || !ActivityTaskManagerServiceStub.get().isControllerAMonkey()) {
            return;
        }
        WindowManagerService wmService = display.mWmService;
        if (this.mFocusRunnable == null) {
            this.mFocusRunnable = new UpdateFocusRunnable(wmService);
        }
        this.mFocusRunnable.post(display.mCurrentFocus == null);
    }

    public boolean skipChangeTransition(int changes, TransitionRequestInfo.DisplayChange displayChange, String resumeActivity) {
        return displayChange == null && resumeActivity != null && (resumeActivity.contains(PROVISION_DEFAULT_ACTIVITY) || resumeActivity.contains(PROVISION_CONGRATULATION_ACTIVITY));
    }

    /* loaded from: classes.dex */
    private static class UpdateFocusRunnable implements Runnable {
        private boolean mPosted;
        private WindowManagerService mWmService;

        UpdateFocusRunnable(WindowManagerService wmService) {
            this.mWmService = wmService;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this.mWmService.mGlobalLock) {
                Slog.w("WindowManager", "retry update focused window after 3000ms");
                this.mWmService.updateFocusedWindowLocked(0, true);
            }
            this.mPosted = false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void post(boolean post) {
            if (post == this.mPosted) {
                return;
            }
            if (post) {
                this.mWmService.mH.postDelayed(this, 3000L);
            } else {
                this.mWmService.mH.removeCallbacks(this);
            }
            this.mPosted = post;
        }
    }

    public int getSmallScreenLayout(Configuration outConfig, DisplayPolicy mDisplayPolicy, float density, int dw, int dh) {
        if (Build.IS_TABLET && ActivityRecordStub.get().isCompatibilityMode()) {
            return getSmallScreenLayout(outConfig, mDisplayPolicy, density, dw <= dh, dw, dh);
        }
        return outConfig.screenLayout;
    }

    private int getSmallScreenLayout(Configuration outConfig, DisplayPolicy mDisplayPolicy, float density, boolean rotated, int dw, int dh) {
        int unrotDw;
        int unrotDh;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDw = dw;
            unrotDh = dh;
        }
        int sl = Configuration.resetScreenLayout(outConfig.screenLayout);
        return reduceConfigLayout(mDisplayPolicy, reduceConfigLayout(mDisplayPolicy, reduceConfigLayout(mDisplayPolicy, reduceConfigLayout(mDisplayPolicy, sl, 0, density, unrotDw, unrotDh), 1, density, unrotDh, unrotDw), 2, density, unrotDw, unrotDh), 3, density, unrotDh, unrotDw);
    }

    private int reduceConfigLayout(DisplayPolicy mDisplayPolicy, int curLayout, int rotation, float density, int dw, int dh) {
        Rect nonDecorSize = mDisplayPolicy.getDecorInsetsInfo(rotation, dw, dh).mNonDecorFrame;
        int w = nonDecorSize.width();
        int h = nonDecorSize.height();
        int longSize = w;
        int shortSize = h;
        if (longSize < shortSize) {
            longSize = shortSize;
            shortSize = longSize;
        }
        return Configuration.reduceScreenLayout(curLayout, (int) (longSize / density), (int) (shortSize / density));
    }

    public void setFixedRotationState(ActivityRecord prevActivity, ActivityRecord nowActivity) {
        if (prevActivity != null && nowActivity != null && prevActivity.packageName.equals(nowActivity.packageName)) {
            this.mSkipUpdateSurfaceRotation = true;
        } else {
            this.mSkipUpdateSurfaceRotation = false;
        }
    }

    public boolean needSkipUpdateSurfaceRotation() {
        return this.mSkipUpdateSurfaceRotation;
    }

    public void setCameraRotationState(boolean isCameraRotating) {
        if ((MiuiOrientationStub.IS_FOLD || MiuiOrientationStub.IS_TABLET) && this.mIsCameraRotating != isCameraRotating) {
            this.mIsCameraRotating = isCameraRotating;
        }
    }

    public boolean isCameraRotating() {
        if (MiuiOrientationStub.IS_FOLD || MiuiOrientationStub.IS_TABLET) {
            return this.mIsCameraRotating;
        }
        return false;
    }

    public void updateCurrentDisplayRotation(DisplayContent displayContent, boolean alwaysSendConfiguration, boolean forceRelayout) {
        Trace.traceBegin(32L, "updateRotation");
        try {
            synchronized (displayContent.mWmService.mGlobalLock) {
                boolean layoutNeeded = false;
                boolean rotationChanged = displayContent.updateRotationUnchecked();
                if (rotationChanged) {
                    displayContent.mAtmService.getTaskChangeNotificationController().notifyOnActivityRotation(displayContent.mDisplayId);
                }
                boolean pendingRemoteDisplayChange = rotationChanged && (displayContent.mRemoteDisplayChangeController.isWaitingForRemoteDisplayChange() || displayContent.mTransitionController.isCollecting());
                if (!pendingRemoteDisplayChange) {
                    if (forceRelayout) {
                        displayContent.setLayoutNeeded();
                        layoutNeeded = true;
                    }
                    if (rotationChanged || alwaysSendConfiguration) {
                        displayContent.sendNewConfiguration();
                    }
                }
                if (layoutNeeded) {
                    Trace.traceBegin(32L, "updateRotation: performSurfacePlacement");
                    displayContent.mWmService.mWindowPlacerLocked.performSurfacePlacement();
                    Trace.traceEnd(32L);
                }
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public void checkWindowHiddenWhenFixedRotation(WindowManagerService service, DisplayContent displayContent, WindowState windowState) {
        if (displayContent.hasTopFixedRotationLaunchingApp()) {
            WindowContainer orientationSource = displayContent.getLastOrientationSource();
            ActivityRecord r = orientationSource != null ? orientationSource.asActivityRecord() : null;
            if (r != null) {
                ActivityRecord topCandidate = !r.isVisibleRequested() ? displayContent.topRunningActivity() : r;
                if (topCandidate != null && topCandidate.hasFixedRotationTransform() && windowState != null && windowState.mAttrs != null && (windowState.mAttrs.extraFlags & 536870912) != 0) {
                    displayContent.getPendingTransaction().hide(windowState.mSurfaceControl);
                    this.mHiddenWindowList.add(windowState);
                }
            }
        }
    }

    public void restoreHiddenWindow(DisplayContent displayContent, SurfaceControl.Transaction transaction) {
        if (this.mHiddenWindowList.isEmpty()) {
            return;
        }
        Iterator<WindowState> it = this.mHiddenWindowList.iterator();
        while (it.hasNext()) {
            WindowState w = it.next();
            transaction.show(w.mSurfaceControl);
        }
        this.mHiddenWindowList.clear();
    }
}
