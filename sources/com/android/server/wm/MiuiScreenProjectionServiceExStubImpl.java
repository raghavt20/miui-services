package com.android.server.wm;

import android.view.WindowManager;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiScreenProjectionServiceExStubImpl implements IMiuiScreenProjectionServiceExStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiScreenProjectionServiceExStubImpl> {

        /* compiled from: MiuiScreenProjectionServiceExStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiScreenProjectionServiceExStubImpl INSTANCE = new MiuiScreenProjectionServiceExStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiScreenProjectionServiceExStubImpl m2653provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiScreenProjectionServiceExStubImpl m2652provideNewInstance() {
            return new MiuiScreenProjectionServiceExStubImpl();
        }
    }

    public void onCastActivityResumed(ActivityRecord ar) {
        ActivityTaskManagerServiceImpl.get().getMiuiActivityController().activityResumed(ar);
        ActivityTaskManagerServiceImpl.get().onForegroundActivityChangedLocked(ar);
    }

    public void setAlertWindowTitle(WindowManager.LayoutParams attrs) {
        WindowManagerServiceImpl.setAlertWindowTitle(attrs);
    }
}
