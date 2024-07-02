package com.android.server.location.hardware.mtk.engineermode.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IEmResponses extends IInterface {
    public static final String DESCRIPTOR = "vendor.mediatek.hardware.engineermode.IEmResponses";
    public static final String HASH = "de2f01ae4c46a25b30928e5a2edf9a1df3132225";
    public static final int VERSION = 1;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void response(byte[] bArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IEmResponses {
        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
        public void response(byte[] message) throws RemoteException {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IEmResponses {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_response = 1;

        public Stub() {
            markVintfStability();
            attachInterface(this, IEmResponses.DESCRIPTOR);
        }

        public static IEmResponses asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IEmResponses.DESCRIPTOR);
            if (iin != null && (iin instanceof IEmResponses)) {
                return (IEmResponses) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= TRANSACTION_getInterfaceVersion) {
                data.enforceInterface(IEmResponses.DESCRIPTOR);
            }
            switch (code) {
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case TRANSACTION_getInterfaceVersion /* 16777215 */:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(IEmResponses.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            byte[] _arg0 = data.createByteArray();
                            data.enforceNoDataAvail();
                            response(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IEmResponses {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IEmResponses.DESCRIPTOR;
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
            public void response(byte[] message) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                try {
                    _data.writeInterfaceToken(IEmResponses.DESCRIPTOR);
                    _data.writeByteArray(message);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method response is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(IEmResponses.DESCRIPTOR);
                        this.mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(IEmResponses.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
