package vendor.xiaomi.hardware.aon;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import vendor.xiaomi.hardware.aon.IAlwaysOnListener;

/* loaded from: classes.dex */
public interface IAlwaysOn extends IInterface {
    public static final String DESCRIPTOR = "vendor$xiaomi$hardware$aon$IAlwaysOn".replace('$', '.');
    public static final String HASH = "c123bfb04ecd15433b050c8b982ff8550a527b0a";
    public static final int VERSION = 1;

    void aon_update_parameter(int i, float f, int i2, long j) throws RemoteException;

    int getCapability() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    int registerListener(int i, float f, int i2, IAlwaysOnListener iAlwaysOnListener) throws RemoteException;

    int unregisterListener(int i, IAlwaysOnListener iAlwaysOnListener) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAlwaysOn {
        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public int getCapability() throws RemoteException {
            return 0;
        }

        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public int registerListener(int Type, float fps, int timeout, IAlwaysOnListener listener) throws RemoteException {
            return 0;
        }

        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public int unregisterListener(int Type, IAlwaysOnListener listener) throws RemoteException {
            return 0;
        }

        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public void aon_update_parameter(int Type, float fps, int timeout, long data) throws RemoteException {
        }

        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAlwaysOn {
        static final int TRANSACTION_aon_update_parameter = 4;
        static final int TRANSACTION_getCapability = 1;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_registerListener = 2;
        static final int TRANSACTION_unregisterListener = 3;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IAlwaysOn asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAlwaysOn)) {
                return (IAlwaysOn) iin;
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
                            int _result = getCapability();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            return true;
                        case 2:
                            int _arg0 = data.readInt();
                            float _arg1 = data.readFloat();
                            int _arg2 = data.readInt();
                            IAlwaysOnListener _arg3 = IAlwaysOnListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            int _result2 = registerListener(_arg0, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            return true;
                        case 3:
                            int _arg02 = data.readInt();
                            IAlwaysOnListener _arg12 = IAlwaysOnListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            int _result3 = unregisterListener(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            return true;
                        case 4:
                            int _arg03 = data.readInt();
                            float _arg13 = data.readFloat();
                            int _arg22 = data.readInt();
                            long _arg32 = data.readLong();
                            data.enforceNoDataAvail();
                            aon_update_parameter(_arg03, _arg13, _arg22, _arg32);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IAlwaysOn {
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

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
            public int getCapability() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getCapability is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
            public int registerListener(int Type, float fps, int timeout, IAlwaysOnListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(Type);
                    _data.writeFloat(fps);
                    _data.writeInt(timeout);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method registerListener is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
            public int unregisterListener(int Type, IAlwaysOnListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(Type);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method unregisterListener is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
            public void aon_update_parameter(int Type, float fps, int timeout, long data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(Type);
                    _data.writeFloat(fps);
                    _data.writeInt(timeout);
                    _data.writeLong(data);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method aon_update_parameter is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
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

            @Override // vendor.xiaomi.hardware.aon.IAlwaysOn
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
