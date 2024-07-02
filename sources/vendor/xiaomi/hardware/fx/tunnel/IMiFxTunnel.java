package vendor.xiaomi.hardware.fx.tunnel;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnelCallback;

/* loaded from: classes.dex */
public interface IMiFxTunnel extends IInterface {
    public static final String DESCRIPTOR = "vendor$xiaomi$hardware$fx$tunnel$IMiFxTunnel".replace('$', '.');
    public static final String HASH = "cac0cec9bbd7ce7545b32873c89cd67844627700";
    public static final int VERSION = 1;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    IMiFxTunnelCommandResult invokeCommand(int i, byte[] bArr) throws RemoteException;

    void setNotify(IMiFxTunnelCallback iMiFxTunnelCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IMiFxTunnel {
        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
        public IMiFxTunnelCommandResult invokeCommand(int cmdId, byte[] param) throws RemoteException {
            return null;
        }

        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
        public void setNotify(IMiFxTunnelCallback callback) throws RemoteException {
        }

        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IMiFxTunnel {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_invokeCommand = 1;
        static final int TRANSACTION_setNotify = 2;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IMiFxTunnel asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMiFxTunnel)) {
                return (IMiFxTunnel) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= TRANSACTION_getInterfaceVersion) {
                data.enforceInterface(descriptor);
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
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            byte[] _arg1 = data.createByteArray();
                            data.enforceNoDataAvail();
                            IMiFxTunnelCommandResult _result = invokeCommand(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            return true;
                        case 2:
                            IMiFxTunnelCallback _arg02 = IMiFxTunnelCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setNotify(_arg02);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IMiFxTunnel {
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
                return DESCRIPTOR;
            }

            @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
            public IMiFxTunnelCommandResult invokeCommand(int cmdId, byte[] param) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(cmdId);
                    _data.writeByteArray(param);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method invokeCommand is unimplemented.");
                    }
                    _reply.readException();
                    IMiFxTunnelCommandResult _result = (IMiFxTunnelCommandResult) _reply.readTypedObject(IMiFxTunnelCommandResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
            public void setNotify(IMiFxTunnelCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setNotify is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
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

            @Override // vendor.xiaomi.hardware.fx.tunnel.IMiFxTunnel
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                        reply.readException();
                        this.mCachedHash = reply.readString();
                        reply.recycle();
                        data.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                        throw th;
                    }
                }
                return this.mCachedHash;
            }
        }
    }
}
