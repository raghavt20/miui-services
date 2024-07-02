package com.android.server.wm;

import android.content.res.Configuration;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class WallpaperWindowTokenImpl implements WallpaperWindowTokenStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WallpaperWindowTokenImpl> {

        /* compiled from: WallpaperWindowTokenImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WallpaperWindowTokenImpl INSTANCE = new WallpaperWindowTokenImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WallpaperWindowTokenImpl m2797provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WallpaperWindowTokenImpl m2796provideNewInstance() {
            return new WallpaperWindowTokenImpl();
        }
    }

    public void onConfigurationChanged(Configuration newParentConfig, WallpaperWindowToken wallpaperWindowToken) {
        if (!wallpaperWindowToken.mResolvedTmpConfig.equals(wallpaperWindowToken.getResolvedOverrideConfiguration())) {
            for (int i = wallpaperWindowToken.mChildren.size() - 1; i >= 0; i--) {
                WindowProcessController app = wallpaperWindowToken.mWmService.mAtmService.mProcessMap.getProcess(((WindowState) wallpaperWindowToken.mChildren.get(i)).mSession.mPid);
                if (app != null && app.hasThread()) {
                    app.onRequestedOverrideConfigurationChanged(wallpaperWindowToken.getResolvedOverrideConfiguration());
                }
            }
        }
        for (int i2 = wallpaperWindowToken.mChildren.size() - 1; i2 >= 0; i2--) {
            WindowProcessController app2 = wallpaperWindowToken.mWmService.mAtmService.mProcessMap.getProcess(((WindowState) wallpaperWindowToken.mChildren.get(i2)).mSession.mPid);
            if (app2 != null && app2.hasThread()) {
                app2.onMergedOverrideConfigurationChanged(wallpaperWindowToken.getMergedOverrideConfiguration());
            }
        }
    }

    public Transition getWaitingRecentTransition(WindowState wallpaperTarget, TransitionController controller) {
        if (wallpaperTarget == null || wallpaperTarget.mActivityRecord == null) {
            return null;
        }
        for (int i = 0; i < controller.mWaitingTransitions.size(); i++) {
            Transition transition = (Transition) controller.mWaitingTransitions.get(i);
            if (transition.mParallelCollectType == 2 && transition.isInTransition(wallpaperTarget.mActivityRecord)) {
                return transition;
            }
        }
        return null;
    }
}
