package com.xiaomi.interconnection;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IWifiP2pCallback extends IInterface {
    public static final String DESCRIPTOR = "com.xiaomi.interconnection.IWifiP2pCallback";

    void onDevicesInfoChanged(P2pDevicesInfo p2pDevicesInfo) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IWifiP2pCallback {
        @Override // com.xiaomi.interconnection.IWifiP2pCallback
        public void onDevicesInfoChanged(P2pDevicesInfo info) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IWifiP2pCallback {
        static final int TRANSACTION_onDevicesInfoChanged = 1;

        public Stub() {
            attachInterface(this, IWifiP2pCallback.DESCRIPTOR);
        }

        public static IWifiP2pCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IWifiP2pCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IWifiP2pCallback)) {
                return (IWifiP2pCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "onDevicesInfoChanged";
                default:
                    return null;
            }
        }

        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IWifiP2pCallback.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IWifiP2pCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            P2pDevicesInfo _arg0 = (P2pDevicesInfo) data.readTypedObject(P2pDevicesInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onDevicesInfoChanged(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IWifiP2pCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IWifiP2pCallback.DESCRIPTOR;
            }

            @Override // com.xiaomi.interconnection.IWifiP2pCallback
            public void onDevicesInfoChanged(P2pDevicesInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                try {
                    _data.writeInterfaceToken(IWifiP2pCallback.DESCRIPTOR);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public int getMaxTransactionId() {
            return 0;
        }
    }
}
