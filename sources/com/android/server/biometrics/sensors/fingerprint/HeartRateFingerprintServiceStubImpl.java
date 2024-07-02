package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.HeartRateCmdResult;
import android.hardware.fingerprint.MiFxTunnelAidl;
import android.hardware.fingerprint.MiFxTunnelHidl;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.policy.DisplayTurnoverManager;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class HeartRateFingerprintServiceStubImpl implements HeartRateFingerprintServiceStub {
    private static final boolean DEBUG = true;
    private static final String TAG = "HeartRateFingerprintServiceStubImpl";
    private static IBinder mHeartRateBinder = null;
    private Handler mHandler;
    private MiuiFingerprintCloudController mMiuiFingerprintCloudController = null;
    private FingerprintService mService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<HeartRateFingerprintServiceStubImpl> {

        /* compiled from: HeartRateFingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final HeartRateFingerprintServiceStubImpl INSTANCE = new HeartRateFingerprintServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public HeartRateFingerprintServiceStubImpl m881provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public HeartRateFingerprintServiceStubImpl m880provideNewInstance() {
            return new HeartRateFingerprintServiceStubImpl();
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        try {
            switch (code) {
                case 16777213:
                    mHeartRateBinder = null;
                    reply.writeNoException();
                    return true;
                case 16777214:
                    data.enforceInterface("com.android.app.HeartRate");
                    mHeartRateBinder = data.readStrongBinder();
                    registerCallback();
                    reply.writeNoException();
                    return true;
                case 16777215:
                    data.enforceInterface("com.android.app.HeartRate");
                    int cmd = data.readInt();
                    int size = data.readInt();
                    byte[] val = null;
                    if (size >= 0) {
                        val = new byte[size];
                        data.readByteArray(val);
                    }
                    HeartRateCmdResult result = doSendCommand(cmd, val);
                    reply.writeParcelable(result, 1);
                    return true;
                default:
                    return true;
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "onTra : ", e);
            return false;
        }
    }

    public int cloudCmd(Looper looper, Context context, int cmd, int param) {
        switch (cmd) {
            case 0:
                this.mMiuiFingerprintCloudController = null;
                return 0;
            case 1:
                if (this.mMiuiFingerprintCloudController == null) {
                    this.mMiuiFingerprintCloudController = new MiuiFingerprintCloudController(looper, context);
                    return 0;
                }
                return 0;
            default:
                return 0;
        }
    }

    public HeartRateCmdResult doSendCommand(int cmdId, byte[] params) throws RemoteException {
        if (MiFxTunnelAidl.getInstance() != null && FingerprintServiceStub.getInstance().getSupportInterfaceVersion() == 2) {
            return MiFxTunnelAidl.getInstance().sendCommand(cmdId, params);
        }
        if (MiFxTunnelHidl.getInstance() != null) {
            return MiFxTunnelHidl.getInstance().sendCommand(cmdId, params);
        }
        Slog.d(TAG, "get null");
        return new HeartRateCmdResult();
    }

    private void registerCallback() throws RemoteException {
        Slog.d(TAG, "reg callback");
        if (MiFxTunnelAidl.getInstance() != null && FingerprintServiceStub.getInstance().getSupportInterfaceVersion() == 2) {
            MiFxTunnelAidl.getInstance().registerCallback(this.mHandler);
        } else if (MiFxTunnelHidl.getInstance() != null) {
            Slog.d(TAG, "get reg");
            MiFxTunnelHidl.getInstance().registerCallback(this.mHandler);
        } else {
            Slog.d(TAG, "get null");
        }
    }

    public static boolean heartRateDataCallback(int msgId, int cmdId, byte[] msg_data) {
        Log.d(TAG, "heartRateDataCallback: msgId: " + msgId + " cmdId: " + cmdId + " msg_data: " + msg_data + " mHeartRateBinder: " + mHeartRateBinder);
        if (mHeartRateBinder == null) {
            return false;
        }
        Parcel request = Parcel.obtain();
        try {
            request.writeInterfaceToken("com.android.app.HeartRate");
            request.writeInt(msgId);
            request.writeInt(cmdId);
            request.writeByteArray(msg_data);
            mHeartRateBinder.transact(DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY, request, null, 1);
            return true;
        } catch (Exception e) {
            mHeartRateBinder = null;
            e.printStackTrace();
            return false;
        } finally {
            request.recycle();
        }
    }
}
