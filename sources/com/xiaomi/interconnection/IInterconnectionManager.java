package com.xiaomi.interconnection;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.xiaomi.interconnection.ISoftApCallback;
import com.xiaomi.interconnection.IWifiP2pCallback;

/* loaded from: classes.dex */
public interface IInterconnectionManager extends IInterface {
    public static final String DESCRIPTOR = "com.xiaomi.interconnection.IInterconnectionManager";

    String getWifiChipModel() throws RemoteException;

    void notifyConcurrentNetworkState(boolean z) throws RemoteException;

    void registerSoftApCallback(ISoftApCallback iSoftApCallback) throws RemoteException;

    void registerWifiP2pCallback(IWifiP2pCallback iWifiP2pCallback) throws RemoteException;

    int supportDbs() throws RemoteException;

    boolean supportHbs() throws RemoteException;

    boolean supportP2p160Mode() throws RemoteException;

    boolean supportP2pChannel165() throws RemoteException;

    void unregisterSoftApCallback(ISoftApCallback iSoftApCallback) throws RemoteException;

    void unregisterWifiP2pCallback(IWifiP2pCallback iWifiP2pCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IInterconnectionManager {
        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public String getWifiChipModel() throws RemoteException {
            return null;
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public boolean supportP2pChannel165() throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public boolean supportP2p160Mode() throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public boolean supportHbs() throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public int supportDbs() throws RemoteException {
            return 0;
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public void registerSoftApCallback(ISoftApCallback cb) throws RemoteException {
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public void unregisterSoftApCallback(ISoftApCallback cb) throws RemoteException {
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public void registerWifiP2pCallback(IWifiP2pCallback cb) throws RemoteException {
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public void unregisterWifiP2pCallback(IWifiP2pCallback cb) throws RemoteException {
        }

        @Override // com.xiaomi.interconnection.IInterconnectionManager
        public void notifyConcurrentNetworkState(boolean mcc) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IInterconnectionManager {
        static final int TRANSACTION_getWifiChipModel = 1;
        static final int TRANSACTION_notifyConcurrentNetworkState = 10;
        static final int TRANSACTION_registerSoftApCallback = 6;
        static final int TRANSACTION_registerWifiP2pCallback = 8;
        static final int TRANSACTION_supportDbs = 5;
        static final int TRANSACTION_supportHbs = 4;
        static final int TRANSACTION_supportP2p160Mode = 3;
        static final int TRANSACTION_supportP2pChannel165 = 2;
        static final int TRANSACTION_unregisterSoftApCallback = 7;
        static final int TRANSACTION_unregisterWifiP2pCallback = 9;

        public Stub() {
            attachInterface(this, IInterconnectionManager.DESCRIPTOR);
        }

        public static IInterconnectionManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IInterconnectionManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IInterconnectionManager)) {
                return (IInterconnectionManager) iin;
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
                    return "getWifiChipModel";
                case 2:
                    return "supportP2pChannel165";
                case 3:
                    return "supportP2p160Mode";
                case 4:
                    return "supportHbs";
                case 5:
                    return "supportDbs";
                case 6:
                    return "registerSoftApCallback";
                case 7:
                    return "unregisterSoftApCallback";
                case 8:
                    return "registerWifiP2pCallback";
                case 9:
                    return "unregisterWifiP2pCallback";
                case 10:
                    return "notifyConcurrentNetworkState";
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
                data.enforceInterface(IInterconnectionManager.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IInterconnectionManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _result = getWifiChipModel();
                            reply.writeNoException();
                            reply.writeString(_result);
                            return true;
                        case 2:
                            boolean _result2 = supportP2pChannel165();
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            return true;
                        case 3:
                            boolean _result3 = supportP2p160Mode();
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            return true;
                        case 4:
                            boolean _result4 = supportHbs();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            return true;
                        case 5:
                            int _result5 = supportDbs();
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            return true;
                        case 6:
                            ISoftApCallback _arg0 = ISoftApCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerSoftApCallback(_arg0);
                            reply.writeNoException();
                            return true;
                        case 7:
                            ISoftApCallback _arg02 = ISoftApCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterSoftApCallback(_arg02);
                            reply.writeNoException();
                            return true;
                        case 8:
                            IWifiP2pCallback _arg03 = IWifiP2pCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerWifiP2pCallback(_arg03);
                            reply.writeNoException();
                            return true;
                        case 9:
                            IWifiP2pCallback _arg04 = IWifiP2pCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterWifiP2pCallback(_arg04);
                            reply.writeNoException();
                            return true;
                        case 10:
                            boolean _arg05 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyConcurrentNetworkState(_arg05);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IInterconnectionManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IInterconnectionManager.DESCRIPTOR;
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public String getWifiChipModel() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public boolean supportP2pChannel165() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public boolean supportP2p160Mode() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public boolean supportHbs() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public int supportDbs() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public void registerSoftApCallback(ISoftApCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public void unregisterSoftApCallback(ISoftApCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public void registerWifiP2pCallback(IWifiP2pCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public void unregisterWifiP2pCallback(IWifiP2pCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.interconnection.IInterconnectionManager
            public void notifyConcurrentNetworkState(boolean mcc) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IInterconnectionManager.DESCRIPTOR);
                    _data.writeBoolean(mcc);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public int getMaxTransactionId() {
            return 9;
        }
    }
}
