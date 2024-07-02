package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class LaunchParamsControllerImpl implements LaunchParamsControllerStub {
    public void registerDefaultModifiers(LaunchParamsController controller) {
        if (MiuiDesktopModeUtils.isEnabled()) {
            controller.registerModifier(new MiuiDesktopModeLaunchParamsModifier());
        }
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LaunchParamsControllerImpl> {

        /* compiled from: LaunchParamsControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LaunchParamsControllerImpl INSTANCE = new LaunchParamsControllerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LaunchParamsControllerImpl m2485provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LaunchParamsControllerImpl m2484provideNewInstance() {
            return new LaunchParamsControllerImpl();
        }
    }
}
