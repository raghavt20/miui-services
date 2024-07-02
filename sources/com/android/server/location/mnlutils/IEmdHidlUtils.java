package com.android.server.location.mnlutils;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd;
import com.android.server.location.hardware.mtk.engineermode.aidl.IEmds;
import java.util.function.Supplier;

/* loaded from: classes.dex */
class IEmdHidlUtils {
    private static final String TAG = "Glp-IEmdHidlUtils";
    public static IEmd mEmHIDLService = null;
    private static Supplier<IEmds> mVintfEmService;

    private IEmdHidlUtils() {
    }

    public static synchronized IEmd getEmHidlService() throws RemoteException {
        IEmd iEmd;
        synchronized (IEmdHidlUtils.class) {
            Log.d(TAG, "getEmHidlService");
            if (mEmHIDLService == null) {
                Log.v(TAG, "getEmHidlService init...");
                try {
                    mEmHIDLService = IEmd.getService("EmHidlServer", true);
                } catch (RemoteException e) {
                    Log.e(TAG, "EmHIDLConnection Exception");
                    Log.e(TAG, Log.getStackTraceString(e));
                    throw e;
                }
            }
            iEmd = mEmHIDLService;
        }
        return iEmd;
    }

    public static IEmds getEmAidlService() {
        Log.d(TAG, "getEmAidlService ...");
        if (mVintfEmService == null) {
            mVintfEmService = new VintfHalCache();
        }
        return mVintfEmService.get();
    }

    /* loaded from: classes.dex */
    private static class VintfHalCache implements Supplier<IEmds>, IBinder.DeathRecipient {
        private IEmds mInstance;

        private VintfHalCache() {
            this.mInstance = null;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.function.Supplier
        public synchronized IEmds get() {
            if (this.mInstance == null) {
                try {
                    IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService("vendor.mediatek.hardware.engineermode.IEmds/default"));
                    if (binder != null) {
                        this.mInstance = IEmds.Stub.asInterface(binder);
                        binder.linkToDeath(this, 0);
                    }
                } catch (RemoteException | RuntimeException e) {
                    this.mInstance = null;
                    Log.e(IEmdHidlUtils.TAG, "Unable to get EM AIDL Service: " + e);
                }
            }
            return this.mInstance;
        }

        @Override // android.os.IBinder.DeathRecipient
        public synchronized void binderDied() {
            this.mInstance = null;
        }
    }
}
