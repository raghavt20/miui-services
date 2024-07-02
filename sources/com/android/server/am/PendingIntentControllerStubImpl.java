package com.android.server.am;

import android.os.Binder;
import android.os.Process;
import android.util.Slog;
import android.util.SparseIntArray;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class PendingIntentControllerStubImpl implements PendingIntentControllerStub {
    private static final int DEFAULT_PENDINGINTENT_ERROR_THRESHOLD = 10000;
    private static final String TAG = "PendingIntentControllerStubImpl";
    public int PENDINGINTENT_ERROR_THRESHOLD = 10000;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PendingIntentControllerStubImpl> {

        /* compiled from: PendingIntentControllerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PendingIntentControllerStubImpl INSTANCE = new PendingIntentControllerStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PendingIntentControllerStubImpl m550provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PendingIntentControllerStubImpl m549provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.am.PendingIntentControllerStubImpl is marked as singleton");
        }
    }

    public void checkIsTooManyPendingIntent(int newCount, int uid, PendingIntentRecord pir, SparseIntArray intentsPerUid) {
        if (newCount >= this.PENDINGINTENT_ERROR_THRESHOLD) {
            String msg = "Too many PendingIntent created for uid " + uid + ", aborting " + pir.key.toString();
            Slog.w(TAG, msg);
            if (uid == 1000 && Binder.getCallingPid() == Process.myPid()) {
                Slog.wtf(TAG, "Too many PendingIntent created for system_server");
            } else {
                intentsPerUid.put(uid, newCount - 1);
                throw new SecurityException(msg);
            }
        }
    }
}
