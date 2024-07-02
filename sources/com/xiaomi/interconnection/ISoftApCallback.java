package com.xiaomi.interconnection;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface ISoftApCallback extends IInterface {
    public static final String DESCRIPTOR = "com.xiaomi.interconnection.ISoftApCallback";

    void onIfaceInfoChanged(String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISoftApCallback {
        @Override // com.xiaomi.interconnection.ISoftApCallback
        public void onIfaceInfoChanged(String softApIfaceName) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISoftApCallback {
        static final int TRANSACTION_onIfaceInfoChanged = 1;

        public Stub() {
            attachInterface(this, ISoftApCallback.DESCRIPTOR);
        }

        public static ISoftApCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ISoftApCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof ISoftApCallback)) {
                return (ISoftApCallback) iin;
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
                    return "onIfaceInfoChanged";
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
                data.enforceInterface(ISoftApCallback.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(ISoftApCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            onIfaceInfoChanged(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ISoftApCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ISoftApCallback.DESCRIPTOR;
            }

            @Override // com.xiaomi.interconnection.ISoftApCallback
            public void onIfaceInfoChanged(String softApIfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                try {
                    _data.writeInterfaceToken(ISoftApCallback.DESCRIPTOR);
                    _data.writeString(softApIfaceName);
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
