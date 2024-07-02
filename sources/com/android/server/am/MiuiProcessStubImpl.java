package com.android.server.am;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiProcessStubImpl implements MiuiProcessStub {
    public int getSchedModeAnimatorRt() {
        return 1;
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiProcessStubImpl> {

        /* compiled from: MiuiProcessStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiProcessStubImpl INSTANCE = new MiuiProcessStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiProcessStubImpl m540provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiProcessStubImpl m539provideNewInstance() {
            return new MiuiProcessStubImpl();
        }
    }

    public long getLaunchRtSchedDurationMs() {
        return 500L;
    }

    public int getSchedModeNormal() {
        return 0;
    }

    public long getScrollRtSchedDurationMs() {
        return 5000L;
    }

    public long getTouchRtSchedDurationMs() {
        return 2000L;
    }

    public long getLiteAnimRtSchedDurationMs() {
        return 1000L;
    }

    public int getSchedModeTouchRt() {
        return 2;
    }

    public int getSchedModeHomeAnimation() {
        return 4;
    }

    public int getSchedModeUnlock() {
        return 5;
    }

    public int getSchedModeBindBigCore() {
        return 7;
    }
}
