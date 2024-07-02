package com.android.server.am;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MutableActiveServicesStubImpl implements MutableActiveServicesStub {
    private boolean mIsCallerSystem;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MutableActiveServicesStubImpl> {

        /* compiled from: MutableActiveServicesStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MutableActiveServicesStubImpl INSTANCE = new MutableActiveServicesStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MutableActiveServicesStubImpl m544provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MutableActiveServicesStubImpl m543provideNewInstance() {
            return new MutableActiveServicesStubImpl();
        }
    }

    public boolean getIsCallerSystem() {
        return this.mIsCallerSystem;
    }

    public void setIsCallerSystem(ProcessRecord callerApp) {
        this.mIsCallerSystem = callerApp.info.uid == 1000;
    }

    public void setIsCallerSystem() {
        this.mIsCallerSystem = false;
    }
}
