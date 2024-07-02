package com.android.server.recoverysystem;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import miui.mqsas.IMQSNative;

/* loaded from: classes.dex */
public class RecoverySystemServiceImpl extends RecoverySystemServiceStub {
    private static final String MQSASD = "miui.mqsas.IMQSNative";
    private static final String TAG = "RecoverySystemServiceStub";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<RecoverySystemServiceImpl> {

        /* compiled from: RecoverySystemServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final RecoverySystemServiceImpl INSTANCE = new RecoverySystemServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public RecoverySystemServiceImpl m2314provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public RecoverySystemServiceImpl m2313provideNewInstance() {
            return new RecoverySystemServiceImpl();
        }
    }

    public void doWipeRescuePartition() {
        IMQSNative daemon = IMQSNative.Stub.asInterface(ServiceManager.getService(MQSASD));
        if (daemon == null) {
            Slog.e(TAG, "mqsasd not available!");
            return;
        }
        Slog.w(TAG, "Trigger wipe rescue partition!");
        try {
            daemon.FactoryResetWipeRescuePartition();
        } catch (RemoteException e) {
            Slog.e(TAG, "Trigger wipe rescue partition failed!", e);
        }
    }
}
