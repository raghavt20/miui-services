package com.android.server.wm;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MiuiMultiWindowServiceImpl implements MiuiMultiWindowServiceStub {
    public static final String TAG = MiuiMultiWindowServiceImpl.class.getSimpleName();
    ActivityTaskManagerService mAtms;
    int mNavigationMode = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMultiWindowServiceImpl> {

        /* compiled from: MiuiMultiWindowServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMultiWindowServiceImpl INSTANCE = new MiuiMultiWindowServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiMultiWindowServiceImpl m2595provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiMultiWindowServiceImpl m2594provideNewInstance() {
            return new MiuiMultiWindowServiceImpl();
        }
    }

    public void init(ActivityTaskManagerService service) {
        this.mAtms = service;
    }

    public boolean isMiuiMultiWinChangeSupport() {
        return MiuiMultiWindowUtils.MULTIWIN_SWITCH_ENABLED;
    }

    public boolean isEventXInDotRegion(DisplayContent displayContent, float eventX) {
        if (displayContent == null) {
            return false;
        }
        float halfDotWidth = MiuiMultiWindowUtils.applyDip2Px(62.0f) / 2.0f;
        synchronized (displayContent.mAtmService.mGlobalLock) {
            if (MiuiSoScManagerStub.get().isSoScModeActivated()) {
                if (MiuiSoScManagerStub.get().isSoScMinimizedModeActivated()) {
                    return false;
                }
                Task soscRoot = displayContent.getDefaultTaskDisplayArea().getTask(new Predicate() { // from class: com.android.server.wm.MiuiMultiWindowServiceImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean z;
                        z = ((Task) obj).mSoScRoot;
                        return z;
                    }
                });
                if (soscRoot != null && soscRoot.getChildCount() >= 2) {
                    Rect splitBounds0 = soscRoot.getChildAt(0).getBounds();
                    Rect splitBounds1 = soscRoot.getChildAt(1).getBounds();
                    if (splitBounds0.width() == displayContent.getBounds().width()) {
                        Task topSplitTask = (splitBounds0.top < splitBounds1.top ? soscRoot.getChildAt(0) : soscRoot.getChildAt(1)).getTopMostTask();
                        float centerX = (splitBounds0.left + splitBounds0.right) / 2.0f;
                        if (topSplitTask != null && topSplitTask.hasDot() && eventX >= centerX - halfDotWidth && eventX <= centerX + halfDotWidth) {
                            Slog.d(TAG, "Event x is in dot region, topSplitTask=" + topSplitTask);
                            return true;
                        }
                    } else {
                        Task t0 = soscRoot.getChildAt(0).getTopMostTask();
                        if (t0 != null && t0.hasDot()) {
                            float centerX2 = (splitBounds0.left + splitBounds0.right) / 2.0f;
                            if (eventX >= centerX2 - halfDotWidth && eventX <= centerX2 + halfDotWidth) {
                                Slog.d(TAG, "Event x is in dot region, t0=" + t0);
                                return true;
                            }
                        }
                        Task t1 = soscRoot.getChildAt(1).getTopMostTask();
                        if (t1 != null && t1.hasDot()) {
                            float centerX3 = (splitBounds1.left + splitBounds1.right) / 2.0f;
                            if (eventX >= centerX3 - halfDotWidth && eventX <= centerX3 + halfDotWidth) {
                                Slog.d(TAG, "Event x is in dot region, t1=" + t1);
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
            Task fullscreenTask = displayContent.getRootTask(new Predicate() { // from class: com.android.server.wm.MiuiMultiWindowServiceImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiMultiWindowServiceImpl.lambda$isEventXInDotRegion$1((Task) obj);
                }
            });
            if (fullscreenTask != null) {
                Rect taskBounds = fullscreenTask.getBounds();
                float centerX4 = (taskBounds.left + taskBounds.right) / 2.0f;
                if (eventX >= centerX4 - halfDotWidth && eventX <= centerX4 + halfDotWidth) {
                    Slog.d(TAG, "Event x is in dot region, fullscreenTask=" + fullscreenTask);
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isEventXInDotRegion$1(Task t) {
        return t.hasDot() && t.getWindowingMode() == 1 && t.isVisible();
    }

    public boolean isSupportSoScMode(Context context) {
        if (!this.mAtms.isInSplitScreenWindowingMode()) {
            return false;
        }
        if (!MiuiMultiWindowUtils.isTablet() && !MiuiMultiWindowUtils.isFoldInnerScreen(context)) {
            return false;
        }
        if (this.mNavigationMode == -1) {
            this.mNavigationMode = Settings.Secure.getIntForUser(this.mAtms.mContext.getContentResolver(), "navigation_mode", 0, -2);
            this.mAtms.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("navigation_mode"), false, new ContentObserver(this.mAtms.mUiHandler) { // from class: com.android.server.wm.MiuiMultiWindowServiceImpl.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    MiuiMultiWindowServiceImpl miuiMultiWindowServiceImpl = MiuiMultiWindowServiceImpl.this;
                    miuiMultiWindowServiceImpl.mNavigationMode = Settings.Secure.getIntForUser(miuiMultiWindowServiceImpl.mAtms.mContext.getContentResolver(), "navigation_mode", 0, -2);
                }
            }, -1);
        }
        return this.mNavigationMode != 0;
    }
}
