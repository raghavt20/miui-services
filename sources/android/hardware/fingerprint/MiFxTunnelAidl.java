package android.hardware.fingerprint;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.fingerprint.FodFingerprintServiceStub;
import com.android.server.biometrics.sensors.fingerprint.HeartRateFingerprintServiceStubImpl;
import java.util.ArrayList;
import vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel;
import vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCallback;
import vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCommandResult;

/* loaded from: classes.dex */
public class MiFxTunnelAidl {
    private static final String DEFAULT = "default";
    private static final String IMIFXTUNNEL_AIDL_INTERFACE = "vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel/default";
    private static final String SERVICE_NAME_V1 = "vendor.xiaomi.hardware.fx.tunnel@1.0::IMiFxTunnel";
    private static final String TAG = "FingerprintServiceAidl";
    private static volatile MiFxTunnelAidl sInstance;
    private static IMiFxTunnel xFxAJ;
    private Handler mHandler;
    private IBinder.DeathRecipient mDeathRecipientAidl = new IBinder.DeathRecipient() { // from class: android.hardware.fingerprint.MiFxTunnelAidl.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.e(MiFxTunnelAidl.TAG, "xFxAJ Died");
            if (MiFxTunnelAidl.xFxAJ == null) {
                return;
            }
            MiFxTunnelAidl.xFxAJ.asBinder().unlinkToDeath(MiFxTunnelAidl.this.mDeathRecipientAidl, 0);
            MiFxTunnelAidl.xFxAJ = null;
            MiFxTunnelAidl.xFxAJ = MiFxTunnelAidl.this.getMiFxTunnelAidl();
        }
    };
    private IMiFxTunnelCallback mMiFxTunnelCallback = new IMiFxTunnelCallback.Stub() { // from class: android.hardware.fingerprint.MiFxTunnelAidl.2
        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCallback
        public void onDaemonMessage(long devId, int msgId, int cmdId, byte[] data) throws RemoteException {
            if (devId == -1 && msgId == -1 && cmdId == -1) {
                Slog.d(MiFxTunnelAidl.TAG, "onDaemonMessage: callback has been replaced!");
            } else {
                HeartRateFingerprintServiceStubImpl.heartRateDataCallback(msgId, cmdId, data);
            }
        }

        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCallback
        public int getInterfaceVersion() {
            return 1;
        }

        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCallback
        public String getInterfaceHash() {
            return "cac0cec9bbd7ce7545b32873c89cd67844627700";
        }
    };

    private MiFxTunnelAidl() {
    }

    public static MiFxTunnelAidl getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (MiFxTunnelAidl.class) {
                if (sInstance == null) {
                    sInstance = new MiFxTunnelAidl();
                }
            }
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IMiFxTunnel getMiFxTunnelAidl() {
        if (xFxAJ == null) {
            IBinder binder = ServiceManager.getService(IMIFXTUNNEL_AIDL_INTERFACE);
            if (binder == null) {
                Slog.e(TAG, "[JAVA] Getting vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel/default service daemon binder failed!");
            } else {
                try {
                    IMiFxTunnel asInterface = IMiFxTunnel.Stub.asInterface(binder);
                    xFxAJ = asInterface;
                    if (asInterface == null) {
                        Slog.e(TAG, "[JAVA] Getting IMiFxTunnel AIDL daemon interface failed!");
                    } else {
                        asInterface.asBinder().linkToDeath(this.mDeathRecipientAidl, 0);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "[JAVA] linkToDeath failed. " + e);
                }
            }
        }
        return xFxAJ;
    }

    public synchronized HeartRateCmdResult sendCommand(int cmdId, byte[] params) {
        HeartRateCmdResult result = new HeartRateCmdResult();
        if (getMiFxTunnelAidl() == null) {
            Slog.e(TAG, "[JAVA] aidl daemon not found");
            return result;
        }
        Slog.i(TAG, "HeartRateCmdResult HeartRateCmdResult: cmdId=" + cmdId + ", params=" + params);
        try {
            IMiFxTunnel iMiFxTunnel = xFxAJ;
            if (iMiFxTunnel != null) {
                IMiFxTunnelCommandResult _result = iMiFxTunnel.invokeCommand(cmdId, params);
                if (_result.errCode == 0) {
                    FodFingerprintServiceStub.getInstance().fodCallBack((Context) null, cmdId, 0, "com.mi.health", (BaseClientMonitor) null);
                    Message msg = this.mHandler.obtainMessage(cmdId);
                    msg.arg1 = cmdId;
                    msg.sendToTarget();
                    result.mResultCode = _result.errCode;
                    result.mResultData = _result.data;
                } else {
                    result.mResultCode = _result.errCode;
                    result.mResultData = null;
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "[JAVA] transact failed. " + e);
        }
        return result;
    }

    public void getAIDLHalDataInt(byte[] data) {
        int currentIndex = 0;
        while (currentIndex < data.length && data.length >= currentIndex + 4) {
            int currentIndex2 = currentIndex + 1;
            int currentIndex3 = currentIndex2 + 1;
            int i = (data[currentIndex] & 255) | ((data[currentIndex2] << 8) & 65280);
            int currentIndex4 = currentIndex3 + 1;
            int val = i | ((data[currentIndex3] << 16) & 16711680) | ((data[currentIndex4] << 24) & (-16777216));
            Slog.i(TAG, "getAIDLHalDataInt: " + val);
            currentIndex = currentIndex4 + 1;
        }
    }

    public synchronized HalDataCmdResult getHalData(int cmdId, byte[] params) {
        HalDataCmdResult result = new HalDataCmdResult();
        if (getMiFxTunnelAidl() == null) {
            Slog.e(TAG, "[JAVA] aidl daemon not found");
            return result;
        }
        try {
            IMiFxTunnel iMiFxTunnel = xFxAJ;
            if (iMiFxTunnel != null) {
                byte[] params2 = {1};
                IMiFxTunnelCommandResult _result = iMiFxTunnel.invokeCommand(cmdId, params2);
                if (_result.errCode == 0) {
                    result.mResultCode = _result.errCode;
                    result.mResultData = _result.data;
                    getAIDLHalDataInt(result.mResultData);
                } else {
                    result.mResultCode = _result.errCode;
                    result.mResultData = null;
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "get HalData failed. " + e);
        }
        return result;
    }

    public void registerCallback(Handler handler) {
        try {
            this.mHandler = handler;
            if (getMiFxTunnelAidl() != null) {
                xFxAJ.setNotify(this.mMiFxTunnelCallback);
            }
        } catch (Exception e) {
            Slog.d(TAG, "registerCallback err");
        }
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] array) {
        ArrayList<Byte> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (byte b : array) {
            Byte b2 = Byte.valueOf(b);
            list.add(b2);
        }
        return list;
    }

    public static byte[] arrayListToByteArray(ArrayList<Byte> list) {
        Byte[] byteArray = (Byte[]) list.toArray(new Byte[list.size()]);
        int i = 0;
        byte[] result = new byte[list.size()];
        int length = byteArray.length;
        int i2 = 0;
        while (i2 < length) {
            Byte b = byteArray[i2];
            result[i] = b.byteValue();
            i2++;
            i++;
        }
        return result;
    }
}
