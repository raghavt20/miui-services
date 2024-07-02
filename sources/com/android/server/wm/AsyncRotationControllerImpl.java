package com.android.server.wm;

import android.util.Slog;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public class AsyncRotationControllerImpl implements AsyncRotationControllerStub {
    private static final String TAG = "AsyncRotationControllerImpl";
    private final HashSet<WindowState> mHiddenWindowList = new HashSet<>();
    private Runnable mTimeoutRunnable;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AsyncRotationControllerImpl> {

        /* compiled from: AsyncRotationControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AsyncRotationControllerImpl INSTANCE = new AsyncRotationControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AsyncRotationControllerImpl m2454provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AsyncRotationControllerImpl m2453provideNewInstance() {
            return new AsyncRotationControllerImpl();
        }
    }

    public boolean excludeFromFadeRotationAnimation(WindowState windowState, boolean isFade) {
        if (windowState == null || windowState.mAttrs == null || !isFade || ((windowState.getWindowType() != 2017 || !windowState.getWindowTag().equals("control_center")) && (windowState.mAttrs.extraFlags & 268435456) == 0)) {
            return false;
        }
        return true;
    }

    public void clearHiddenWindow(WindowManagerService service) {
        if (this.mHiddenWindowList.isEmpty()) {
            return;
        }
        synchronized (service.mGlobalLock) {
            Slog.i(TAG, "Clear HiddenWindowList");
            this.mHiddenWindowList.clear();
        }
    }

    public void updateRoundedCornerAlpha(WindowManagerService service) {
        if (this.mHiddenWindowList.isEmpty()) {
            return;
        }
        Iterator<WindowState> it = this.mHiddenWindowList.iterator();
        while (it.hasNext()) {
            WindowState w = it.next();
            if (isValid(w)) {
                w.mToken.getPendingTransaction().setAlpha(w.mToken.mSurfaceControl, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            }
        }
        service.mH.removeCallbacks(this.mTimeoutRunnable);
        scheduleRestoreHiddenWindowTimeout(service);
    }

    public void updateRoundedCornerAlpha(WindowManagerService service, WindowState w) {
        synchronized (service.mGlobalLock) {
            if (w.mToken.mRoundedCornerOverlay && isValid(w)) {
                ((SurfaceControl.Transaction) service.mTransactionFactory.get()).setAlpha(w.mToken.mSurfaceControl, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X).apply();
                this.mHiddenWindowList.add(w);
            }
        }
    }

    public void updateRoundedCornerAlpha(WindowManagerService service, WindowState w, float alpha, SurfaceControl.Transaction t) {
        if (this.mHiddenWindowList.isEmpty() || alpha != 1.0f) {
            return;
        }
        synchronized (service.mGlobalLock) {
            if (service.getDefaultDisplayContentLocked().hasTopFixedRotationLaunchingApp() && w.mToken.mRoundedCornerOverlay && isValid(w)) {
                t.setAlpha(w.mToken.mSurfaceControl, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            }
        }
    }

    public void scheduleRestoreHiddenWindowTimeout(final WindowManagerService service) {
        if (this.mHiddenWindowList.isEmpty()) {
            return;
        }
        if (this.mTimeoutRunnable == null) {
            this.mTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.AsyncRotationControllerImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AsyncRotationControllerImpl.this.lambda$scheduleRestoreHiddenWindowTimeout$0(service);
                }
            };
        }
        service.mH.postDelayed(this.mTimeoutRunnable, 2000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleRestoreHiddenWindowTimeout$0(WindowManagerService service) {
        synchronized (service.mGlobalLock) {
            if (service.getDefaultDisplayContentLocked().hasTopFixedRotationLaunchingApp()) {
                return;
            }
            Slog.i(TAG, "Restore hidden window timeout");
            Iterator<WindowState> it = this.mHiddenWindowList.iterator();
            while (it.hasNext()) {
                WindowState w = it.next();
                if (isValid(w)) {
                    w.mToken.getPendingTransaction().setAlpha(w.mToken.mSurfaceControl, 1.0f);
                }
            }
            this.mHiddenWindowList.clear();
        }
    }

    public void removeHiddenWindowTimeoutCallbacks(WindowManagerService service) {
        service.mH.removeCallbacks(this.mTimeoutRunnable);
    }

    private boolean isValid(WindowState w) {
        if (w != null && w.mToken.mSurfaceControl != null && w.mToken.mSurfaceControl.isValid()) {
            return true;
        }
        return false;
    }
}
