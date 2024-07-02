package com.android.server.wm;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import android.view.SurfaceControl;
import android.window.TransitionInfo;
import com.android.server.wm.WallpaperController;
import com.miui.base.MiuiStubRegistry;
import java.util.List;
import miui.os.Build;
import miui.os.DeviceFeature;

/* loaded from: classes.dex */
public class WallpaperControllerImpl implements WallpaperControllerStub {
    private static final boolean DEBUG = false;
    private static final String KEYGUARD_DESCRIPTOR = "miui.systemui.keyguard.Wallpaper";
    private static final String SCROLL_DESKTOP_WALLPAPER = "pref_key_wallpaper_screen_scrolled_span";
    private static final String TAG = "WallpaperControllerImpl";
    private static final float WALLPAPER_OFFSET_CENTER = 0.5f;
    private static final float WALLPAPER_OFFSET_DEFAULT = -1.0f;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WallpaperControllerImpl> {

        /* compiled from: WallpaperControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WallpaperControllerImpl INSTANCE = new WallpaperControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WallpaperControllerImpl m2795provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WallpaperControllerImpl m2794provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.wm.WallpaperControllerImpl is marked as singleton");
        }
    }

    public float getLastWallpaperX(Context context) {
        if (context != null) {
            if (!Build.IS_TABLET || Settings.Secure.getInt(context.getContentResolver(), SCROLL_DESKTOP_WALLPAPER, -1) == 1) {
                return -1.0f;
            }
            return 0.5f;
        }
        Slog.e(TAG, "getLastWallpaperX: fail, context null");
        return -1.0f;
    }

    public void findWallpapers(WindowState w, WallpaperController.FindWallpaperTargetResult findResult) {
        if (w.mAttrs.type == 2013) {
            WallpaperWindowToken token = w.mToken.asWallpaperToken();
            if (token.canShowWhenLocked() && !findResult.hasTopShowWhenLockedWallpaper()) {
                findResult.setTopShowWhenLockedWallpaper(w);
                return;
            }
            if (!token.canShowWhenLocked()) {
                try {
                    if (KEYGUARD_DESCRIPTOR.equals(w.mToken.token.getInterfaceDescriptor())) {
                        findResult.setTopShowWhenLockedWallpaper(w);
                    } else if (!findResult.hasTopHideWhenLockedWallpaper()) {
                        findResult.setTopHideWhenLockedWallpaper(w);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void showWallpaperIfNeeded(TransitionInfo info, DisplayContent displayContent, SurfaceControl.Transaction t) {
        List<WindowState> wallpapers;
        if (info.getType() != 2 || DeviceFeature.IS_FOLD_DEVICE || Build.IS_TABLET) {
            return;
        }
        boolean hasHomeOpen = false;
        boolean hasAppClose = false;
        boolean hasWallpaperOpen = false;
        for (int i = 0; i < info.getChanges().size(); i++) {
            TransitionInfo.Change change = (TransitionInfo.Change) info.getChanges().get(i);
            if ((change.getMode() == 2 || change.getMode() == 4) && TransitionInfo.isIndependent(change, info) && change.getTaskInfo() != null && change.getTaskInfo().getActivityType() == 1) {
                hasAppClose = true;
            }
            if ((change.getMode() == 1 || change.getMode() == 3) && TransitionInfo.isIndependent(change, info) && change.getTaskInfo() != null && change.getTaskInfo().getActivityType() == 2) {
                hasHomeOpen = true;
            }
            if ((change.getMode() == 1 || change.getMode() == 3) && change.hasFlags(2)) {
                hasWallpaperOpen = true;
            }
        }
        if (!hasAppClose && hasHomeOpen && hasWallpaperOpen && (wallpapers = displayContent.mWallpaperController.getAllTopWallpapers()) != null) {
            for (WindowState wallpaper : wallpapers) {
                WindowStateAnimator wsa = wallpaper.mWinAnimator;
                if (wsa != null && wsa.mDrawState == 1 && wsa.mShownAlpha == 1.0f) {
                    wsa.mSurfaceController.showRobustly(t);
                }
            }
        }
    }
}
