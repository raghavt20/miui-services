package com.xiaomi.NetworkBoost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback;
import com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;

/* loaded from: classes.dex */
public interface IAIDLMiuiNetworkBoost extends IInterface {
    boolean abortScan() throws RemoteException;

    boolean activeScan(int[] iArr) throws RemoteException;

    boolean connectSlaveWifi(int i) throws RemoteException;

    boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback) throws RemoteException;

    boolean disableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) throws RemoteException;

    boolean disconnectSlaveWifi() throws RemoteException;

    boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) throws RemoteException;

    boolean enableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i, int i2) throws RemoteException;

    Map<String, String> getAvailableIfaces() throws RemoteException;

    Map<String, String> getQoEByAvailableIfaceName(String str) throws RemoteException;

    NetLinkLayerQoE getQoEByAvailableIfaceNameV1(String str) throws RemoteException;

    int getServiceVersion() throws RemoteException;

    boolean isCelluarDSDAState() throws RemoteException;

    int isSlaveWifiEnabledAndOthersOpt(int i) throws RemoteException;

    int isSlaveWifiEnabledAndOthersOptByUid(int i, int i2) throws RemoteException;

    boolean isSupportDualCelluarData() throws RemoteException;

    boolean isSupportDualWifi() throws RemoteException;

    boolean isSupportMediaPlayerPolicy() throws RemoteException;

    boolean registerCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) throws RemoteException;

    boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) throws RemoteException;

    boolean registerNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i, int i2) throws RemoteException;

    boolean registerWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) throws RemoteException;

    void reportBssidScore(Map<String, String> map) throws RemoteException;

    Map<String, String> requestAppTrafficStatistics(int i, long j, long j2) throws RemoteException;

    Map<String, String> requestAppTrafficStatisticsByUid(int i, long j, long j2, int i2) throws RemoteException;

    boolean resumeBackgroundScan() throws RemoteException;

    boolean resumeWifiPowerSave() throws RemoteException;

    boolean setDualCelluarDataEnable(boolean z) throws RemoteException;

    boolean setSlaveWifiEnable(boolean z) throws RemoteException;

    boolean setSockPrio(int i, int i2) throws RemoteException;

    boolean setTCPCongestion(int i, String str) throws RemoteException;

    boolean setTrafficTransInterface(int i, String str) throws RemoteException;

    boolean suspendBackgroundScan() throws RemoteException;

    boolean suspendWifiPowerSave() throws RemoteException;

    void triggerWifiSelection() throws RemoteException;

    boolean unregisterCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) throws RemoteException;

    boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback) throws RemoteException;

    boolean unregisterNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) throws RemoteException;

    boolean unregisterWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLMiuiNetworkBoost {
        public Stub() {
            attachInterface(this, "com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
        }

        public static /* synthetic */ void a(Parcel parcel, String str, String str2) {
            parcel.writeString(str);
            parcel.writeString(str2);
        }

        public static IAIDLMiuiNetworkBoost asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IAIDLMiuiNetworkBoost)) {
                return (IAIDLMiuiNetworkBoost) queryLocalInterface;
            }
            return new a(iBinder);
        }

        public static /* synthetic */ void b(Parcel parcel, String str, String str2) {
            parcel.writeString(str);
            parcel.writeString(str2);
        }

        public static /* synthetic */ void c(Parcel parcel, String str, String str2) {
            parcel.writeString(str);
            parcel.writeString(str2);
        }

        public static /* synthetic */ void d(Parcel parcel, String str, String str2) {
            parcel.writeString(str);
            parcel.writeString(str2);
        }

        public static IAIDLMiuiNetworkBoost getDefaultImpl() {
            return a.b;
        }

        public static boolean setDefaultImpl(IAIDLMiuiNetworkBoost iAIDLMiuiNetworkBoost) {
            if (a.b != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iAIDLMiuiNetworkBoost == null) {
                return false;
            }
            a.b = iAIDLMiuiNetworkBoost;
            return true;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, final Parcel parcel, final Parcel parcel2, int i2) throws RemoteException {
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean sockPrio = setSockPrio(parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(sockPrio ? 1 : 0);
                        return true;
                    case 2:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean tCPCongestion = setTCPCongestion(parcel.readInt(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(tCPCongestion ? 1 : 0);
                        return true;
                    case 3:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean isSupportDualWifi = isSupportDualWifi();
                        parcel2.writeNoException();
                        parcel2.writeInt(isSupportDualWifi ? 1 : 0);
                        return true;
                    case 4:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean slaveWifiEnable = setSlaveWifiEnable(parcel.readInt() != 0);
                        parcel2.writeNoException();
                        parcel2.writeInt(slaveWifiEnable ? 1 : 0);
                        return true;
                    case 5:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean connectSlaveWifi = connectSlaveWifi(parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(connectSlaveWifi ? 1 : 0);
                        return true;
                    case 6:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean disconnectSlaveWifi = disconnectSlaveWifi();
                        parcel2.writeNoException();
                        parcel2.writeInt(disconnectSlaveWifi ? 1 : 0);
                        return true;
                    case 7:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        Map<String, String> availableIfaces = getAvailableIfaces();
                        parcel2.writeNoException();
                        if (availableIfaces == null) {
                            parcel2.writeInt(-1);
                        } else {
                            parcel2.writeInt(availableIfaces.size());
                            availableIfaces.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$$ExternalSyntheticLambda4
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    IAIDLMiuiNetworkBoost.Stub.a(parcel2, (String) obj, (String) obj2);
                                }
                            });
                        }
                        return true;
                    case 8:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        Map<String, String> requestAppTrafficStatistics = requestAppTrafficStatistics(parcel.readInt(), parcel.readLong(), parcel.readLong());
                        parcel2.writeNoException();
                        if (requestAppTrafficStatistics == null) {
                            parcel2.writeInt(-1);
                        } else {
                            parcel2.writeInt(requestAppTrafficStatistics.size());
                            requestAppTrafficStatistics.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$$ExternalSyntheticLambda3
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    IAIDLMiuiNetworkBoost.Stub.b(parcel2, (String) obj, (String) obj2);
                                }
                            });
                        }
                        return true;
                    case 9:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean registerCallback = registerCallback(IAIDLMiuiNetworkCallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(registerCallback ? 1 : 0);
                        return true;
                    case 10:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean unregisterCallback = unregisterCallback(IAIDLMiuiNetworkCallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(unregisterCallback ? 1 : 0);
                        return true;
                    case 11:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean activeScan = activeScan(parcel.createIntArray());
                        parcel2.writeNoException();
                        parcel2.writeInt(activeScan ? 1 : 0);
                        return true;
                    case 12:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean abortScan = abortScan();
                        parcel2.writeNoException();
                        parcel2.writeInt(abortScan ? 1 : 0);
                        return true;
                    case 13:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean suspendBackgroundScan = suspendBackgroundScan();
                        parcel2.writeNoException();
                        parcel2.writeInt(suspendBackgroundScan ? 1 : 0);
                        return true;
                    case 14:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean resumeBackgroundScan = resumeBackgroundScan();
                        parcel2.writeNoException();
                        parcel2.writeInt(resumeBackgroundScan ? 1 : 0);
                        return true;
                    case 15:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean suspendWifiPowerSave = suspendWifiPowerSave();
                        parcel2.writeNoException();
                        parcel2.writeInt(suspendWifiPowerSave ? 1 : 0);
                        return true;
                    case 16:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean resumeWifiPowerSave = resumeWifiPowerSave();
                        parcel2.writeNoException();
                        parcel2.writeInt(resumeWifiPowerSave ? 1 : 0);
                        return true;
                    case 17:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean registerWifiLinkCallback = registerWifiLinkCallback(IAIDLMiuiWlanQoECallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(registerWifiLinkCallback ? 1 : 0);
                        return true;
                    case 18:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean unregisterWifiLinkCallback = unregisterWifiLinkCallback(IAIDLMiuiWlanQoECallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(unregisterWifiLinkCallback ? 1 : 0);
                        return true;
                    case 19:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        Map<String, String> qoEByAvailableIfaceName = getQoEByAvailableIfaceName(parcel.readString());
                        parcel2.writeNoException();
                        if (qoEByAvailableIfaceName == null) {
                            parcel2.writeInt(-1);
                        } else {
                            parcel2.writeInt(qoEByAvailableIfaceName.size());
                            qoEByAvailableIfaceName.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$$ExternalSyntheticLambda2
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    IAIDLMiuiNetworkBoost.Stub.c(parcel2, (String) obj, (String) obj2);
                                }
                            });
                        }
                        return true;
                    case 20:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean trafficTransInterface = setTrafficTransInterface(parcel.readInt(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(trafficTransInterface ? 1 : 0);
                        return true;
                    case 21:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        int serviceVersion = getServiceVersion();
                        parcel2.writeNoException();
                        parcel2.writeInt(serviceVersion);
                        return true;
                    case 22:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        NetLinkLayerQoE qoEByAvailableIfaceNameV1 = getQoEByAvailableIfaceNameV1(parcel.readString());
                        parcel2.writeNoException();
                        if (qoEByAvailableIfaceNameV1 != null) {
                            parcel2.writeInt(1);
                            qoEByAvailableIfaceNameV1.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 23:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean registerNetLinkCallback = registerNetLinkCallback(IAIDLMiuiNetQoECallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(registerNetLinkCallback ? 1 : 0);
                        return true;
                    case 24:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean unregisterNetLinkCallback = unregisterNetLinkCallback(IAIDLMiuiNetQoECallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(unregisterNetLinkCallback ? 1 : 0);
                        return true;
                    case 25:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean registerNetLinkCallbackByUid = registerNetLinkCallbackByUid(IAIDLMiuiNetQoECallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(registerNetLinkCallbackByUid ? 1 : 0);
                        return true;
                    case 26:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean unregisterNetLinkCallbackByUid = unregisterNetLinkCallbackByUid(IAIDLMiuiNetQoECallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(unregisterNetLinkCallbackByUid ? 1 : 0);
                        return true;
                    case 27:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        Map<String, String> requestAppTrafficStatisticsByUid = requestAppTrafficStatisticsByUid(parcel.readInt(), parcel.readLong(), parcel.readLong(), parcel.readInt());
                        parcel2.writeNoException();
                        if (requestAppTrafficStatisticsByUid == null) {
                            parcel2.writeInt(-1);
                        } else {
                            parcel2.writeInt(requestAppTrafficStatisticsByUid.size());
                            requestAppTrafficStatisticsByUid.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$$ExternalSyntheticLambda1
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    IAIDLMiuiNetworkBoost.Stub.d(parcel2, (String) obj, (String) obj2);
                                }
                            });
                        }
                        return true;
                    case 28:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean enableWifiSelectionOptByUid = enableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(enableWifiSelectionOptByUid ? 1 : 0);
                        return true;
                    case IResultValue.MISYS_ESPIPE /* 29 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean disableWifiSelectionOptByUid = disableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(disableWifiSelectionOptByUid ? 1 : 0);
                        return true;
                    case 30:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean enableWifiSelectionOpt = enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(enableWifiSelectionOpt ? 1 : 0);
                        return true;
                    case IResultValue.MISYS_EMLINK /* 31 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        triggerWifiSelection();
                        parcel2.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        int readInt = parcel.readInt();
                        final HashMap hashMap = readInt < 0 ? null : new HashMap();
                        IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$$ExternalSyntheticLambda0
                            @Override // java.util.function.IntConsumer
                            public final void accept(int i3) {
                                hashMap.put(r0.readString(), parcel.readString());
                            }
                        });
                        reportBssidScore(hashMap);
                        parcel2.writeNoException();
                        return true;
                    case UsbKeyboardUtil.COMMAND_TOUCH_PAD_ENABLE /* 33 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean disableWifiSelectionOpt = disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback.Stub.asInterface(parcel.readStrongBinder()));
                        parcel2.writeNoException();
                        parcel2.writeInt(disableWifiSelectionOpt ? 1 : 0);
                        return true;
                    case 34:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean isSupportDualCelluarData = isSupportDualCelluarData();
                        parcel2.writeNoException();
                        parcel2.writeInt(isSupportDualCelluarData ? 1 : 0);
                        return true;
                    case UsbKeyboardUtil.COMMAND_BACK_LIGHT_ENABLE /* 35 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean isCelluarDSDAState = isCelluarDSDAState();
                        parcel2.writeNoException();
                        parcel2.writeInt(isCelluarDSDAState ? 1 : 0);
                        return true;
                    case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean dualCelluarDataEnable = setDualCelluarDataEnable(parcel.readInt() != 0);
                        parcel2.writeNoException();
                        parcel2.writeInt(dualCelluarDataEnable ? 1 : 0);
                        return true;
                    case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        boolean isSupportMediaPlayerPolicy = isSupportMediaPlayerPolicy();
                        parcel2.writeNoException();
                        parcel2.writeInt(isSupportMediaPlayerPolicy ? 1 : 0);
                        return true;
                    case 38:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        int isSlaveWifiEnabledAndOthersOpt = isSlaveWifiEnabledAndOthersOpt(parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(isSlaveWifiEnabledAndOthersOpt);
                        return true;
                    case 39:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                        int isSlaveWifiEnabledAndOthersOptByUid = isSlaveWifiEnabledAndOthersOptByUid(parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(isSlaveWifiEnabledAndOthersOptByUid);
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            }
            parcel2.writeString("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
            return true;
        }

        /* loaded from: classes.dex */
        public static class a implements IAIDLMiuiNetworkBoost {
            public static IAIDLMiuiNetworkBoost b;
            public IBinder a;

            public a(IBinder iBinder) {
                this.a = iBinder;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean abortScan() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(12, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().abortScan();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean activeScan(int[] iArr) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeIntArray(iArr);
                    if (!this.a.transact(11, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().activeScan(iArr);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // android.os.IInterface
            public final IBinder asBinder() {
                return this.a;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean connectSlaveWifi(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    if (!this.a.transact(5, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().connectSlaveWifi(i);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetSelectCallback != null ? iAIDLMiuiNetSelectCallback.asBinder() : null);
                    if (!this.a.transact(33, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().disableWifiSelectionOpt(iAIDLMiuiNetSelectCallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean disableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetSelectCallback != null ? iAIDLMiuiNetSelectCallback.asBinder() : null);
                    obtain.writeInt(i);
                    if (!this.a.transact(29, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().disableWifiSelectionOptByUid(iAIDLMiuiNetSelectCallback, i);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean disconnectSlaveWifi() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(6, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().disconnectSlaveWifi();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetSelectCallback != null ? iAIDLMiuiNetSelectCallback.asBinder() : null);
                    obtain.writeInt(i);
                    if (!this.a.transact(30, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().enableWifiSelectionOpt(iAIDLMiuiNetSelectCallback, i);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean enableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback, int i, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetSelectCallback != null ? iAIDLMiuiNetSelectCallback.asBinder() : null);
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (!this.a.transact(28, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().enableWifiSelectionOptByUid(iAIDLMiuiNetSelectCallback, i, i2);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final Map<String, String> getAvailableIfaces() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(7, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAvailableIfaces();
                    }
                    obtain2.readException();
                    int readInt = obtain2.readInt();
                    final HashMap hashMap = readInt < 0 ? null : new HashMap();
                    IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$a$$ExternalSyntheticLambda2
                        @Override // java.util.function.IntConsumer
                        public final void accept(int i) {
                            hashMap.put(r0.readString(), obtain2.readString());
                        }
                    });
                    return hashMap;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final Map<String, String> getQoEByAvailableIfaceName(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeString(str);
                    if (!this.a.transact(19, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getQoEByAvailableIfaceName(str);
                    }
                    obtain2.readException();
                    int readInt = obtain2.readInt();
                    final HashMap hashMap = readInt < 0 ? null : new HashMap();
                    IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$a$$ExternalSyntheticLambda0
                        @Override // java.util.function.IntConsumer
                        public final void accept(int i) {
                            hashMap.put(r0.readString(), obtain2.readString());
                        }
                    });
                    return hashMap;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final NetLinkLayerQoE getQoEByAvailableIfaceNameV1(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeString(str);
                    if (!this.a.transact(22, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getQoEByAvailableIfaceNameV1(str);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0 ? NetLinkLayerQoE.CREATOR.createFromParcel(obtain2) : null;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final int getServiceVersion() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(21, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getServiceVersion();
                    }
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean isCelluarDSDAState() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(35, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isCelluarDSDAState();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final int isSlaveWifiEnabledAndOthersOpt(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    if (!this.a.transact(38, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSlaveWifiEnabledAndOthersOpt(i);
                    }
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final int isSlaveWifiEnabledAndOthersOptByUid(int i, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (!this.a.transact(39, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSlaveWifiEnabledAndOthersOptByUid(i, i2);
                    }
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean isSupportDualCelluarData() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(34, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSupportDualCelluarData();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean isSupportDualWifi() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(3, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSupportDualWifi();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean isSupportMediaPlayerPolicy() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(37, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSupportMediaPlayerPolicy();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean registerCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetworkCallback != null ? iAIDLMiuiNetworkCallback.asBinder() : null);
                    if (!this.a.transact(9, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerCallback(iAIDLMiuiNetworkCallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetQoECallback != null ? iAIDLMiuiNetQoECallback.asBinder() : null);
                    obtain.writeInt(i);
                    if (!this.a.transact(23, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerNetLinkCallback(iAIDLMiuiNetQoECallback, i);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean registerNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetQoECallback != null ? iAIDLMiuiNetQoECallback.asBinder() : null);
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (!this.a.transact(25, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerNetLinkCallbackByUid(iAIDLMiuiNetQoECallback, i, i2);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean registerWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiWlanQoECallback != null ? iAIDLMiuiWlanQoECallback.asBinder() : null);
                    if (!this.a.transact(17, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().registerWifiLinkCallback(iAIDLMiuiWlanQoECallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final void reportBssidScore(Map<String, String> map) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (map == null) {
                        obtain.writeInt(-1);
                    } else {
                        obtain.writeInt(map.size());
                        map.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$a$$ExternalSyntheticLambda4
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                IAIDLMiuiNetworkBoost.Stub.a.a(obtain, (String) obj, (String) obj2);
                            }
                        });
                    }
                    if (!this.a.transact(32, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().reportBssidScore(map);
                    } else {
                        obtain2.readException();
                    }
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final Map<String, String> requestAppTrafficStatistics(int i, long j, long j2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    if (!this.a.transact(8, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().requestAppTrafficStatistics(i, j, j2);
                    }
                    obtain2.readException();
                    int readInt = obtain2.readInt();
                    final HashMap hashMap = readInt < 0 ? null : new HashMap();
                    IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$a$$ExternalSyntheticLambda1
                        @Override // java.util.function.IntConsumer
                        public final void accept(int i2) {
                            hashMap.put(r0.readString(), obtain2.readString());
                        }
                    });
                    return hashMap;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final Map<String, String> requestAppTrafficStatisticsByUid(int i, long j, long j2, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    obtain.writeInt(i2);
                    if (!this.a.transact(27, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().requestAppTrafficStatisticsByUid(i, j, j2, i2);
                    }
                    obtain2.readException();
                    int readInt = obtain2.readInt();
                    final HashMap hashMap = readInt < 0 ? null : new HashMap();
                    IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost$Stub$a$$ExternalSyntheticLambda3
                        @Override // java.util.function.IntConsumer
                        public final void accept(int i3) {
                            hashMap.put(r0.readString(), obtain2.readString());
                        }
                    });
                    return hashMap;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean resumeBackgroundScan() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(14, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().resumeBackgroundScan();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean resumeWifiPowerSave() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(16, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().resumeWifiPowerSave();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean setDualCelluarDataEnable(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(36, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setDualCelluarDataEnable(z);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean setSlaveWifiEnable(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(4, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setSlaveWifiEnable(z);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean setSockPrio(int i, int i2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    if (!this.a.transact(1, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setSockPrio(i, i2);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean setTCPCongestion(int i, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    if (!this.a.transact(2, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setTCPCongestion(i, str);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean setTrafficTransInterface(int i, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    if (!this.a.transact(20, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setTrafficTransInterface(i, str);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean suspendBackgroundScan() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(13, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().suspendBackgroundScan();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean suspendWifiPowerSave() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(15, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().suspendWifiPowerSave();
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final void triggerWifiSelection() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    if (!this.a.transact(31, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().triggerWifiSelection();
                    } else {
                        obtain2.readException();
                    }
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean unregisterCallback(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetworkCallback != null ? iAIDLMiuiNetworkCallback.asBinder() : null);
                    if (!this.a.transact(10, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterCallback(iAIDLMiuiNetworkCallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetQoECallback != null ? iAIDLMiuiNetQoECallback.asBinder() : null);
                    if (!this.a.transact(24, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterNetLinkCallback(iAIDLMiuiNetQoECallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean unregisterNetLinkCallbackByUid(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiNetQoECallback != null ? iAIDLMiuiNetQoECallback.asBinder() : null);
                    obtain.writeInt(i);
                    if (!this.a.transact(26, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterNetLinkCallbackByUid(iAIDLMiuiNetQoECallback, i);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
            public final boolean unregisterWifiLinkCallback(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost");
                    obtain.writeStrongBinder(iAIDLMiuiWlanQoECallback != null ? iAIDLMiuiWlanQoECallback.asBinder() : null);
                    if (!this.a.transact(18, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().unregisterWifiLinkCallback(iAIDLMiuiWlanQoECallback);
                    }
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            public static /* synthetic */ void a(Parcel parcel, String str, String str2) {
                parcel.writeString(str);
                parcel.writeString(str2);
            }
        }
    }
}
