package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class DimmerImpl implements DimmerStub {
    private boolean mAnimateEnter = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DimmerImpl> {

        /* compiled from: DimmerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DimmerImpl INSTANCE = new DimmerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DimmerImpl m2458provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DimmerImpl m2457provideNewInstance() {
            return new DimmerImpl();
        }
    }

    public void setAnimateEnter(boolean animateEnter) {
        this.mAnimateEnter = animateEnter;
    }

    public boolean isAnimateEnter() {
        return this.mAnimateEnter;
    }
}
