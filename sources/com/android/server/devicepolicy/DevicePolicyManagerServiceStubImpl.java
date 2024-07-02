package com.android.server.devicepolicy;

import android.content.Context;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.lang.reflect.Method;

@MiuiStubHead(manifestName = "com.android.server.devicepolicy.DevicePolicyManagerServiceStub$$")
/* loaded from: classes.dex */
public class DevicePolicyManagerServiceStubImpl extends DevicePolicyManagerServiceStub {
    private static final String TAG = "DevicePolicyManagerServiceStubImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DevicePolicyManagerServiceStubImpl> {

        /* compiled from: DevicePolicyManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DevicePolicyManagerServiceStubImpl INSTANCE = new DevicePolicyManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DevicePolicyManagerServiceStubImpl m965provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DevicePolicyManagerServiceStubImpl m964provideNewInstance() {
            return new DevicePolicyManagerServiceStubImpl();
        }
    }

    public void checkFactoryResetRecoveryCondition(String key, boolean enabledFromThisOwner, Context mContext) {
        try {
            Class<?> recoverySystem = Class.forName("android.os.RecoverySystem");
            Method method_disableFactoryReset = recoverySystem.getDeclaredMethod("disableFactoryReset", Context.class, Boolean.TYPE);
            if (key.equals("no_factory_reset")) {
                if (enabledFromThisOwner) {
                    method_disableFactoryReset.setAccessible(true);
                    method_disableFactoryReset.invoke(recoverySystem, mContext, true);
                    Slog.w(TAG, "Restrict factory reset by recovery system");
                } else {
                    method_disableFactoryReset.setAccessible(true);
                    method_disableFactoryReset.invoke(recoverySystem, mContext, false);
                    Slog.w(TAG, "Launch factory reset by recovery system");
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get recoverySystem", e);
        }
    }
}
