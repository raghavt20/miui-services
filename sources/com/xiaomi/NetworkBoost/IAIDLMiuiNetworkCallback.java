package com.xiaomi.NetworkBoost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

/* loaded from: classes.dex */
public interface IAIDLMiuiNetworkCallback extends IInterface {

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLMiuiNetworkCallback {

        /* loaded from: classes.dex */
        public static class a implements IAIDLMiuiNetworkCallback {
            public static IAIDLMiuiNetworkCallback b;
            public IBinder a;

            public a(IBinder iBinder) {
                this.a = iBinder;
            }

            @Override // android.os.IInterface
            public final IBinder asBinder() {
                return this.a;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void dsdaStateChanged(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(8, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().dsdaStateChanged(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void ifaceAdded(List<String> list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeStringList(list);
                    if (!this.a.transact(5, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().ifaceAdded(list);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void ifaceRemoved(List<String> list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeStringList(list);
                    if (!this.a.transact(6, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().ifaceRemoved(list);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void mediaPlayerPolicyNotify(int i, int i2, int i3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    if (!this.a.transact(9, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().mediaPlayerPolicyNotify(i, i2, i3);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onNetworkPriorityChanged(int i, int i2, int i3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    if (!this.a.transact(10, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onNetworkPriorityChanged(i, i2, i3);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onScanSuccussed(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(i);
                    if (!this.a.transact(1, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onScanSuccussed(i);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onSetSlaveWifiResult(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(2, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSetSlaveWifiResult(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onSlaveWifiConnected(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(3, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSlaveWifiConnected(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onSlaveWifiDisconnected(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(4, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSlaveWifiDisconnected(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onSlaveWifiEnable(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(z ? 1 : 0);
                    if (!this.a.transact(7, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSlaveWifiEnable(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
            public final void onSlaveWifiEnableV1(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                    obtain.writeInt(i);
                    if (!this.a.transact(11, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSlaveWifiEnableV1(i);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
        }

        public static IAIDLMiuiNetworkCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IAIDLMiuiNetworkCallback)) {
                return (IAIDLMiuiNetworkCallback) queryLocalInterface;
            }
            return new a(iBinder);
        }

        public static IAIDLMiuiNetworkCallback getDefaultImpl() {
            return a.b;
        }

        public static boolean setDefaultImpl(IAIDLMiuiNetworkCallback iAIDLMiuiNetworkCallback) {
            if (a.b != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iAIDLMiuiNetworkCallback == null) {
                return false;
            }
            a.b = iAIDLMiuiNetworkCallback;
            return true;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onScanSuccussed(parcel.readInt());
                        return true;
                    case 2:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onSetSlaveWifiResult(parcel.readInt() != 0);
                        return true;
                    case 3:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onSlaveWifiConnected(parcel.readInt() != 0);
                        return true;
                    case 4:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onSlaveWifiDisconnected(parcel.readInt() != 0);
                        return true;
                    case 5:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        ifaceAdded(parcel.createStringArrayList());
                        return true;
                    case 6:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        ifaceRemoved(parcel.createStringArrayList());
                        return true;
                    case 7:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onSlaveWifiEnable(parcel.readInt() != 0);
                        return true;
                    case 8:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        dsdaStateChanged(parcel.readInt() != 0);
                        return true;
                    case 9:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        mediaPlayerPolicyNotify(parcel.readInt(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 10:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onNetworkPriorityChanged(parcel.readInt(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 11:
                        parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
                        onSlaveWifiEnableV1(parcel.readInt());
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            }
            parcel2.writeString("com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback");
            return true;
        }
    }

    void dsdaStateChanged(boolean z) throws RemoteException;

    void ifaceAdded(List<String> list) throws RemoteException;

    void ifaceRemoved(List<String> list) throws RemoteException;

    void mediaPlayerPolicyNotify(int i, int i2, int i3) throws RemoteException;

    void onNetworkPriorityChanged(int i, int i2, int i3) throws RemoteException;

    void onScanSuccussed(int i) throws RemoteException;

    void onSetSlaveWifiResult(boolean z) throws RemoteException;

    void onSlaveWifiConnected(boolean z) throws RemoteException;

    void onSlaveWifiDisconnected(boolean z) throws RemoteException;

    void onSlaveWifiEnable(boolean z) throws RemoteException;

    void onSlaveWifiEnableV1(int i) throws RemoteException;
}
