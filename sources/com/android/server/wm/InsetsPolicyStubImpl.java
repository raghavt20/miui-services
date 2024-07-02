package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class InsetsPolicyStubImpl extends InsetsPolicyStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InsetsPolicyStubImpl> {

        /* compiled from: InsetsPolicyStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InsetsPolicyStubImpl INSTANCE = new InsetsPolicyStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public InsetsPolicyStubImpl m2483provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public InsetsPolicyStubImpl m2482provideNewInstance() {
            return new InsetsPolicyStubImpl();
        }
    }

    public boolean useFocusedWindowForStatusControl(WindowState focusedWin, boolean forceShowsSystemBarsForWindowingMode) {
        return (forceShowsSystemBarsForWindowingMode || focusedWin == null || focusedWin.getWindowTag() == null || !focusedWin.getWindowTag().equals("control_center")) ? false : true;
    }
}
