package com.android.server.wm;

import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.joyose.IJoyoseInterface;

/* loaded from: classes.dex */
public class MiGameTypeIdentificationImpl implements MiGameTypeIdentificationStub {
    private static final String JOYOSE_NAME = "com.xiaomi.joyose";
    private static final String SERVICE_NAME = "xiaomi.joyose";
    private static final String TAG = "MiGgameTypeIdentificationImpl";
    private static IJoyoseInterface mJoyose = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiGameTypeIdentificationImpl> {

        /* compiled from: MiGameTypeIdentificationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiGameTypeIdentificationImpl INSTANCE = new MiGameTypeIdentificationImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiGameTypeIdentificationImpl m2503provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiGameTypeIdentificationImpl m2502provideNewInstance() {
            return new MiGameTypeIdentificationImpl();
        }
    }

    public boolean checkIfGameBeSupported(String app) {
        Log.d(TAG, "checkIfGameBeSupported app:" + app);
        return false;
    }
}
