package com.xiaomi.NetworkBoost;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost;
import com.xiaomi.NetworkBoost.NetworkBoostService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class NetworkBoostManager {
    public IAIDLMiuiNetworkBoost a;
    public final Context c;
    public ServiceCallback e;
    public Object d = new Object();
    public a b = new a();

    /* loaded from: classes.dex */
    public class a implements ServiceConnection {
        public a() {
        }

        @Override // android.content.ServiceConnection
        public final void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (NetworkBoostManager.this.d) {
                NetworkBoostManager.this.a = IAIDLMiuiNetworkBoost.Stub.asInterface(iBinder);
                try {
                    Version.setServiceVersion(NetworkBoostManager.this.a.getServiceVersion());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                NetworkBoostManager.this.d.notifyAll();
            }
            NetworkBoostManager.this.e.onServiceConnected();
        }

        @Override // android.content.ServiceConnection
        public final void onServiceDisconnected(ComponentName componentName) {
            synchronized (NetworkBoostManager.this.d) {
                NetworkBoostManager networkBoostManager = NetworkBoostManager.this;
                networkBoostManager.a = null;
                networkBoostManager.d.notifyAll();
            }
            NetworkBoostManager.this.e.onServiceDisconnected();
        }
    }

    public NetworkBoostManager(Context context, ServiceCallback serviceCallback) {
        this.c = context;
        this.e = serviceCallback;
    }

    public static int a(FileDescriptor fileDescriptor) throws IOException {
        try {
            if (!fileDescriptor.valid()) {
                return -1;
            }
            Field declaredField = FileDescriptor.class.getDeclaredField("fd");
            declaredField.setAccessible(true);
            long j = declaredField.getInt(fileDescriptor);
            declaredField.setAccessible(false);
            return (int) j;
        } catch (IllegalAccessException e) {
            throw new IOException("unable to access handle/fd fields in FileDescriptor", e);
        } catch (NoSuchFieldException e2) {
            throw new IOException("FileDescriptor in this JVM lacks handle/fd fields", e2);
        }
    }

    public static int getSDKVersion() {
        return 6;
    }

    public static int getServiceVersion() {
        return Version.getServiceVersion();
    }

    public boolean abortScan() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.abortScan();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean activeScan(int[] iArr) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.activeScan(iArr);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean bindService() {
        try {
            this.a = IAIDLMiuiNetworkBoost.Stub.asInterface((IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, NetworkBoostService.NetworkBoostServiceManager.SERVICE_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                Version.setServiceVersion(iAIDLMiuiNetworkBoost.getServiceVersion());
                this.e.onServiceConnected();
            } catch (RemoteException e2) {
                e2.printStackTrace();
                this.e.onServiceDisconnected();
            }
        } else {
            Intent intent = new Intent(IAIDLMiuiNetworkBoost.class.getName());
            intent.setClassName("com.xiaomi.NetworkBoost", "com.xiaomi.NetworkBoost.NetworkBoostService");
            this.c.bindService(intent, this.b, 1);
        }
        return this.a != null;
    }

    public boolean connectSlaveWifi(int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(Integer.MAX_VALUE) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.connectSlaveWifi(i);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.disableWifiSelectionOpt(iAIDLMiuiNetSelectCallback);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean disableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.disableWifiSelectionOptByUid(iAIDLMiuiNetSelectCallback, i);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean disconnectSlaveWifi() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(Integer.MAX_VALUE) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.disconnectSlaveWifi();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.enableWifiSelectionOpt(iAIDLMiuiNetSelectCallback, i);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean enableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i, int i2) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.enableWifiSelectionOptByUid(iAIDLMiuiNetSelectCallback, i, i2);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public Map<String, String> getAvailableIfaces() {
        if (!Version.isSupport(3)) {
            return new HashMap();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                return iAIDLMiuiNetworkBoost.getAvailableIfaces();
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return new HashMap();
    }

    @Deprecated
    public Map<String, String> getQoEByAvailableIfaceName(String str) {
        if (!Version.isSupport(Integer.MAX_VALUE)) {
            return new HashMap();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                return iAIDLMiuiNetworkBoost.getQoEByAvailableIfaceName(str);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return new HashMap();
    }

    public NetLinkLayerQoE getQoEByAvailableIfaceNameV1(String str) {
        if (!Version.isSupport(4)) {
            return new NetLinkLayerQoE();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                return iAIDLMiuiNetworkBoost.getQoEByAvailableIfaceNameV1(str);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return new NetLinkLayerQoE();
    }

    public boolean isCelluarDSDAState() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isCelluarDSDAState();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public int isSlaveWifiEnabledAndOthersOpt(int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(5) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isSlaveWifiEnabledAndOthersOpt(i);
            } catch (Exception e) {
                e.toString();
            }
        }
        return -1;
    }

    public int isSlaveWifiEnabledAndOthersOptByUid(int i, int i2) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(6) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isSlaveWifiEnabledAndOthersOptByUid(i, i2);
            } catch (Exception e) {
                e.toString();
            }
        }
        return -1;
    }

    public boolean isSupportDualCelluarData() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isSupportDualCelluarData();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean isSupportDualWifi() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isSupportDualWifi();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean isSupportMediaPlayerPolicy() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.isSupportMediaPlayerPolicy();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean registerCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(1) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.registerCallback(iAIDLMiuiNetworkCallback);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.registerNetLinkCallback(iAIDLMiuiNetQoECallback, i);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean registerNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i, int i2) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.registerNetLinkCallbackByUid(iAIDLMiuiNetQoECallback, i, i2);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    @Deprecated
    public boolean registerWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.registerWifiLinkCallback(iAIDLMiuiWlanQoECallback);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public void reportBssidScore(Map<String, String> map) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                iAIDLMiuiNetworkBoost.reportBssidScore(map);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
    }

    public Map<String, String> requestAppTrafficStatistics(int i, long j, long j2) {
        if (!Version.isSupport(3)) {
            return new HashMap();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                return iAIDLMiuiNetworkBoost.requestAppTrafficStatistics(i, j, j2);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return new HashMap();
    }

    public Map<String, String> requestAppTrafficStatisticsByUid(int i, long j, long j2, int i2) {
        if (!Version.isSupport(4)) {
            return new HashMap();
        }
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost = this.a;
        if (iAIDLMiuiNetworkBoost != null) {
            try {
                return iAIDLMiuiNetworkBoost.requestAppTrafficStatisticsByUid(i, j, j2, i2);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return new HashMap();
    }

    public boolean resumeBackgroundScan() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.resumeBackgroundScan();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean resumeWifiPowerSave() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.resumeWifiPowerSave();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setDualCelluarDataEnable(boolean z) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.setDualCelluarDataEnable(z);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setSlaveWifiEnable(boolean z) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.setSlaveWifiEnable(z);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setSockPrio(int i, int i2) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.setSockPrio(i, i2);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setTCPCongestion(int i, String str) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.setTCPCongestion(i, str);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setTrafficTransInterface(int i, String str) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.setTrafficTransInterface(i, str);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean suspendBackgroundScan() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.suspendBackgroundScan();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean suspendWifiPowerSave() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.suspendWifiPowerSave();
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public void triggerWifiSelection() {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                iAIDLMiuiNetworkBoost.triggerWifiSelection();
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
    }

    public void unbindService() {
        Context context = this.c;
        if (context != null) {
            try {
                context.unbindService(this.b);
            } catch (IllegalArgumentException e) {
            }
        }
        synchronized (this.d) {
            this.a = null;
            this.d.notifyAll();
        }
    }

    public boolean unregisterCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(1) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.unregisterCallback(iAIDLMiuiNetworkCallback);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.unregisterNetLinkCallback(iAIDLMiuiNetQoECallback);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean unregisterNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(4) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.unregisterNetLinkCallbackByUid(iAIDLMiuiNetQoECallback, i);
            } catch (Exception e) {
                e.toString();
                e.printStackTrace();
            }
        }
        return false;
    }

    @Deprecated
    public boolean unregisterWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) {
        IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost;
        if (Version.isSupport(3) && (iAIDLMiuiNetworkBoost = this.a) != null) {
            try {
                return iAIDLMiuiNetworkBoost.unregisterWifiLinkCallback(iAIDLMiuiWlanQoECallback);
            } catch (Exception e) {
                e.toString();
            }
        }
        return false;
    }

    public boolean setSockPrio(FileDescriptor fileDescriptor, int i) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setSockPrio(a(fileDescriptor), i);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }

    public boolean setTCPCongestion(FileDescriptor fileDescriptor, String str) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setTCPCongestion(a(fileDescriptor), str);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }

    public boolean setTrafficTransInterface(FileDescriptor fileDescriptor, String str) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setTrafficTransInterface(a(fileDescriptor), str);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }

    public boolean setSockPrio(Socket socket, int i) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setSockPrio(a(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor()), i);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }

    public boolean setTCPCongestion(Socket socket, String str) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setTCPCongestion(a(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor()), str);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }

    public boolean setTrafficTransInterface(Socket socket, String str) {
        if (!Version.isSupport(3)) {
            return false;
        }
        try {
            return setTrafficTransInterface(a(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor()), str);
        } catch (IOException e) {
            e.toString();
            return false;
        }
    }
}
