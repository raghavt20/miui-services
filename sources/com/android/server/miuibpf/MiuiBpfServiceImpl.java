package com.android.server.miuibpf;

import android.util.Log;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiBpfServiceImpl extends MiuiBpfServiceStub {
    private static final String TAG = "MiuiBpfService";

    private static native boolean runMiuiBpfMain();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBpfServiceImpl> {

        /* compiled from: MiuiBpfServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBpfServiceImpl INSTANCE = new MiuiBpfServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiBpfServiceImpl m1875provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiBpfServiceImpl m1874provideNewInstance() {
            return new MiuiBpfServiceImpl();
        }
    }

    public void start() {
        Log.e(TAG, "Here we starting MiuiBpfService...");
        runMiuiBpfMain();
    }
}
