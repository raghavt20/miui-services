package com.android.server.wm;

import android.app.WindowConfiguration;
import android.app.servertransaction.BoundsCompat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.util.MiuiAppSizeCompatModeStub;
import android.view.DisplayCutout;
import android.view.DisplayCutoutStub;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.WindowInsets;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.xiaomi.NetworkBoost.StatusManager;

/* loaded from: classes.dex */
public class WindowStateStubImpl extends WindowStateStub {
    private static final float DEFAULT_COMPAT_SCALE_VALUE = 1.0f;
    private boolean mIsWallpaperOffsetUpdateCompleted = true;
    private static final String TAG = WindowStateStubImpl.class.getSimpleName();
    private static float FLIP_COMPAT_SCALE_SCALE_VALUE = 0.8f;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowStateStubImpl> {

        /* compiled from: WindowStateStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowStateStubImpl INSTANCE = new WindowStateStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WindowStateStubImpl m2809provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WindowStateStubImpl m2808provideNewInstance() {
            return new WindowStateStubImpl();
        }
    }

    public boolean isMiuiLayoutInCutoutAlways(WindowManager.LayoutParams attrs) {
        return false;
    }

    public boolean notifyNonAppSurfaceVisibilityChanged(String pkgName, int type) {
        return "com.miui.home".equals(pkgName) && type == 4;
    }

    public boolean isStatusBarForceBlackWindow(WindowManager.LayoutParams attrs) {
        return "ForceBlack".equals(attrs.getTitle()) && AccessController.PACKAGE_SYSTEMUI.equals(attrs.packageName);
    }

    public static void adjuestScaleAndFrame(WindowState win, Task task) {
    }

    public static void adjuestFrameForChild(WindowState win) {
        Task task;
        WindowConfiguration windowConfiguration;
        WindowConfiguration windowConfiguration2;
        if (!win.isChildWindow() && (task = win.getTask()) != null) {
            Rect rect = task.getBounds();
            if (rect.left < win.mWindowFrames.mFrame.left) {
                int relativeLeft = win.mWindowFrames.mFrame.left - rect.left;
                int weightOffset = relativeLeft - ((int) (relativeLeft * win.mGlobalScale));
                if (win.inFreeformWindowingMode() && win.mDownscaleScale > 1.0f && (windowConfiguration2 = win.getWindowConfiguration()) != null && windowConfiguration2.getBounds() != null) {
                    Rect bounds = windowConfiguration2.getBounds();
                    int targetLeft = (int) (bounds.left + (((bounds.width() - (win.mRequestedWidth * win.mDownscaleScale)) * win.mGlobalScale) / 2.0f));
                    weightOffset = win.mWindowFrames.mFrame.left - targetLeft;
                }
                win.mWindowFrames.mFrame.left -= weightOffset;
                win.mWindowFrames.mFrame.right -= weightOffset;
            }
            if (rect.top < win.mWindowFrames.mFrame.top) {
                int relativeTop = win.mWindowFrames.mFrame.top - rect.top;
                int heightOffset = relativeTop - ((int) (relativeTop * win.mGlobalScale));
                if (win.inFreeformWindowingMode() && win.mDownscaleScale > 1.0f && (windowConfiguration = win.getWindowConfiguration()) != null && windowConfiguration.getBounds() != null) {
                    Rect bounds2 = windowConfiguration.getBounds();
                    int targetTop = (int) (bounds2.top + (((bounds2.height() - (win.mRequestedHeight * win.mDownscaleScale)) * win.mGlobalScale) / 2.0f));
                    heightOffset = win.mWindowFrames.mFrame.top - targetTop;
                }
                win.mWindowFrames.mFrame.top -= heightOffset;
                win.mWindowFrames.mFrame.bottom -= heightOffset;
            }
        }
    }

    public static void adjuestFreeFormTouchRegion(WindowState win, Region outRegion) {
    }

    public static WindowState getWinStateFromInputWinMap(WindowManagerService windowManagerService, IBinder token) {
        WindowState windowState;
        synchronized (windowManagerService.mGlobalLock) {
            windowState = (WindowState) windowManagerService.mInputToWindowMap.get(token);
        }
        return windowState;
    }

    public IMiuiWindowStateEx getMiuiWindowStateEx(WindowManagerService service, Object w) {
        return new MiuiWindowStateEx(service, w);
    }

    public InsetsState adjustInsetsForSplit(WindowState ws, InsetsState insets) {
        MiuiEmbeddingWindowServiceStub.get().adjustInsetsForEmbedding(ws, insets);
        if (ws == null || insets == null || !ActivityTaskManagerServiceImpl.getInstance().isVerticalSplit()) {
            return insets;
        }
        if (ws.inSplitScreenWindowingMode() && ws.mActivityRecord != null && !ws.mActivityRecord.mImeInsetsFrozenUntilStartInput) {
            boolean removeIme = false;
            InsetsControlTarget imeTarget = ws.getDisplayContent().getImeTarget(0);
            if (imeTarget != null && imeTarget.getWindow() != null && ws.getTask() != imeTarget.getWindow().getTask()) {
                removeIme = true;
            }
            if (removeIme) {
                insets.removeSource(InsetsSource.ID_IME);
            }
        }
        return insets;
    }

    public void updateSizeCompatModeWindowPosition(Point mSurfacePosition, WindowState windowState) {
        if (needUpdateWindowEnable(windowState)) {
            mSurfacePosition.x = (int) ((mSurfacePosition.x * windowState.mGlobalScale) + 0.5f);
            if (needNavBarOffset(windowState.getAttrs().gravity)) {
                mSurfacePosition.y = ((int) ((mSurfacePosition.y * windowState.mGlobalScale) + 0.5f)) - getNavBarHeight(windowState);
            } else {
                mSurfacePosition.y = (int) ((mSurfacePosition.y * windowState.mGlobalScale) + 0.5f);
            }
        }
    }

    public boolean updateSizeCompatModeWindowInsetsPosition(WindowState windowState, Point outPos, Rect surfaceInsets) {
        if (needUpdateWindowEnable(windowState)) {
            outPos.x = surfaceInsets.left;
            outPos.y = surfaceInsets.top;
            return true;
        }
        return false;
    }

    private int getNavBarHeight(WindowState windowState) {
        return windowState.getInsetsState().calculateInsets(windowState.getInsetsState().getDisplayFrame(), WindowInsets.Type.navigationBars(), true).bottom;
    }

    private boolean needUpdateWindowEnable(WindowState windowState) {
        if (windowState.mActivityRecord == null || windowState.mGlobalScale == 1.0f || !switchWindowType(windowState.getAttrs())) {
            return false;
        }
        return isinMiuiSizeCompatModeEnable(windowState) || isFixedOrizationScaleEnable(windowState);
    }

    private boolean isFixedOrizationScaleEnable(WindowState windowState) {
        return windowState.getDisplayContent() != null && windowState.getDisplayContent().getIgnoreOrientationRequest();
    }

    private boolean isinMiuiSizeCompatModeEnable(WindowState windowState) {
        return MiuiAppSizeCompatModeStub.get().isEnabled() && windowState.mActivityRecord.inMiuiSizeCompatMode();
    }

    private boolean switchWindowType(WindowManager.LayoutParams Attrs) {
        switch (Attrs.type) {
            case 2:
            case 1000:
                return true;
            default:
                return false;
        }
    }

    private boolean needNavBarOffset(int gravity) {
        return (gravity & StatusManager.CALLBACK_VPN_STATUS) == 80;
    }

    public void adjustFrameForDownscale(WindowState ws, Rect frame, WindowConfiguration config) {
        if (ws.inFreeformWindowingMode() && ws.mDownscaleScale > 1.0f) {
            Rect bounds = config.getBounds();
            int height = (int) ((bounds.height() * (1.0f / ws.mDownscaleScale)) + 0.5f);
            int width = (int) ((bounds.width() * (1.0f / ws.mDownscaleScale)) + 0.5f);
            frame.right = frame.left + width;
            frame.bottom = frame.top + height;
        }
    }

    public void updateClientHoverState(WindowState windowState, boolean hoverMode, boolean supportTouch) {
        try {
            windowState.mClient.updateClientHoverState(hoverMode, supportTouch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateHoverGuidePanel(WindowState windowState, boolean add) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.updateHoverGuidePanel(windowState, add);
        }
    }

    public void updateWallpaperOffsetUpdateState() {
        if (this.mIsWallpaperOffsetUpdateCompleted) {
            this.mIsWallpaperOffsetUpdateCompleted = false;
        } else {
            this.mIsWallpaperOffsetUpdateCompleted = true;
        }
    }

    public void updateWallpaperOffsetUpdateState(WindowState windowState) {
        if (isImageWallpaper(windowState) && !this.mIsWallpaperOffsetUpdateCompleted) {
            this.mIsWallpaperOffsetUpdateCompleted = true;
        }
    }

    public boolean isWallpaperOffsetUpdateCompleted(WindowState windowState) {
        if (isImageWallpaper(windowState)) {
            return this.mIsWallpaperOffsetUpdateCompleted;
        }
        return true;
    }

    private boolean isImageWallpaper(WindowState windowState) {
        return windowState.getName().contains("ImageWallpaper");
    }

    public float getScaleCurrentDuration(DisplayContent displayContent, float windowAnimationScale) {
        if (displayContent == null) {
            return windowAnimationScale;
        }
        return displayContent.mDisplayContentStub.isNoAnimation() ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : windowAnimationScale;
    }

    public boolean updateSyncStateReady(int viewVisibility, int syncState) {
        return viewVisibility == 8 && syncState == 1;
    }

    public void updateSizeCompatModeWindowPositionForFlip(Point mSurfacePosition, WindowState windowState) {
        WindowToken token = windowState.mToken;
        if (needUpdateWindowEnableForFlip(windowState)) {
            if (windowState.getWindowType() != 2005) {
                windowState.mGlobalScale = FLIP_COMPAT_SCALE_SCALE_VALUE;
            }
            if (windowState.mDisplayContent.inTransition() && windowState.getWindowType() == 2005) {
                windowState.hide(false, false);
            }
            Rect rect = BoundsCompat.getInstance().computeCompatBounds(1.7206428f, new Point(token.getBounds().width(), token.getBounds().height()), getmGravity(windowState.getWindowConfiguration(), 17), windowState.getWindowConfiguration().getRotation(), windowState.getWindowConfiguration().getRotation(), windowState.mGlobalScale);
            windowState.setBounds(rect);
            if (windowState.getWindowConfiguration().getDisplayRotation() == 2) {
                mSurfacePosition.x = (int) ((mSurfacePosition.x * windowState.mGlobalScale) + 0.5d);
            }
            mSurfacePosition.y = (int) ((mSurfacePosition.y * windowState.mGlobalScale) + 0.5d);
            return;
        }
        if (MiuiAppSizeCompatModeStub.get().isFlip() && !MiuiAppSizeCompatModeStub.get().isFlipFolded() && windowState.getWindowType() == 2031) {
            windowState.mGlobalScale = 1.0f;
            windowState.setBounds(token.getBounds());
        }
    }

    private static int getmGravity(WindowConfiguration windowconfiguration, int mGravity) {
        if (windowconfiguration.getDisplayRotation() == 0) {
            return 5;
        }
        if (windowconfiguration.getDisplayRotation() == 2) {
            return 3;
        }
        return mGravity;
    }

    private boolean needUpdateWindowEnableForFlip(WindowState windowState) {
        if (!MiuiAppSizeCompatModeStub.get().isFlipFolded() || ActivityTaskManagerServiceStub.get().shouldNotApplyAspectRatio(windowState.mAttrs.packageName)) {
            return false;
        }
        switch (windowState.getWindowType()) {
            case 2005:
            case 2031:
                return true;
            default:
                return false;
        }
    }

    public static boolean isFlipScale(WindowState windowState) {
        return windowState.mGlobalScale == FLIP_COMPAT_SCALE_SCALE_VALUE;
    }

    public InsetsState adjustInsetsForFlipFolded(WindowState ws, InsetsState insets) {
        if (ws == null || insets == null || ws.mActivityRecord == null || !DisplayCutoutStub.get().isFlipFolded() || !MiuiAppSizeCompatModeStub.get().isEnabled() || ws.getActivityType() == 2) {
            return insets;
        }
        if (ws.mActivityRecord.inMiuiSizeCompatMode() || (ws.mAttrs.accessibilityTitle != null && ws.mAttrs.accessibilityTitle.equals("MiuixImmersionDialog"))) {
            insets.setDisplayCutout(DisplayCutout.NO_CUTOUT);
        }
        return insets;
    }
}
