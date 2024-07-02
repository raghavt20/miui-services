package com.android.server.display;

import android.os.Build;
import com.miui.base.MiuiStubRegistry;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class LocalDisplayAdapterImpl extends LocalDisplayAdapterStub {
    private static final boolean mIsFoldDevice = MiuiMultiDisplayTypeInfo.isFoldDeviceInside();
    private static final boolean mIsFlipDevice = MiuiMultiDisplayTypeInfo.isFlipDevice();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocalDisplayAdapterImpl> {

        /* compiled from: LocalDisplayAdapterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocalDisplayAdapterImpl INSTANCE = new LocalDisplayAdapterImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LocalDisplayAdapterImpl m1114provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LocalDisplayAdapterImpl m1113provideNewInstance() {
            return new LocalDisplayAdapterImpl();
        }
    }

    public void setDeviceInfoFlagIfNeeded(DisplayDeviceInfo deviceInfo, boolean isFirstDisplay) {
        if (mIsFoldDevice || mIsFlipDevice) {
            deviceInfo.flags |= 128;
        }
        if (!isFirstDisplay && "star".equals(Build.DEVICE)) {
            deviceInfo.flags |= 16384;
            deviceInfo.flags |= 128;
        }
    }

    public void updateScreenEffectIfNeeded(int state, boolean isFirstDisplay) {
        if (!"star".equals(Build.DEVICE) || isFirstDisplay) {
            DisplayFeatureManagerServiceStub.getInstance().updateScreenEffect(state);
        }
    }
}
