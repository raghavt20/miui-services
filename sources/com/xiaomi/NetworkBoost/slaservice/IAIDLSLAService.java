package com.xiaomi.NetworkBoost.slaservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

/* loaded from: classes.dex */
public interface IAIDLSLAService extends IInterface {
    public static final String DESCRIPTOR = "com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService";

    boolean addUidToLinkTurboWhiteList(String str) throws RemoteException;

    List<SLAApp> getLinkTurboAppsTraffic() throws RemoteException;

    List<String> getLinkTurboDefaultPn() throws RemoteException;

    String getLinkTurboWhiteList() throws RemoteException;

    boolean removeUidInLinkTurboWhiteList(String str) throws RemoteException;

    boolean setLinkTurboEnable(boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAIDLSLAService {
        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean addUidToLinkTurboWhiteList(String uid) throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean removeUidInLinkTurboWhiteList(String uid) throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public String getLinkTurboWhiteList() throws RemoteException {
            return null;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public List<SLAApp> getLinkTurboAppsTraffic() throws RemoteException {
            return null;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean setLinkTurboEnable(boolean enable) throws RemoteException {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public List<String> getLinkTurboDefaultPn() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLSLAService {
        static final int TRANSACTION_addUidToLinkTurboWhiteList = 1;
        static final int TRANSACTION_getLinkTurboAppsTraffic = 4;
        static final int TRANSACTION_getLinkTurboDefaultPn = 6;
        static final int TRANSACTION_getLinkTurboWhiteList = 3;
        static final int TRANSACTION_removeUidInLinkTurboWhiteList = 2;
        static final int TRANSACTION_setLinkTurboEnable = 5;

        public Stub() {
            attachInterface(this, IAIDLSLAService.DESCRIPTOR);
        }

        public static IAIDLSLAService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAIDLSLAService.DESCRIPTOR);
            if (iin != null && (iin instanceof IAIDLSLAService)) {
                return (IAIDLSLAService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IAIDLSLAService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IAIDLSLAService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result = addUidToLinkTurboWhiteList(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            return true;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = removeUidInLinkTurboWhiteList(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            return true;
                        case 3:
                            String _result3 = getLinkTurboWhiteList();
                            reply.writeNoException();
                            reply.writeString(_result3);
                            return true;
                        case 4:
                            List<SLAApp> _result4 = getLinkTurboAppsTraffic();
                            reply.writeNoException();
                            reply.writeTypedList(_result4, 1);
                            return true;
                        case 5:
                            boolean _arg03 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result5 = setLinkTurboEnable(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            return true;
                        case 6:
                            List<String> _result6 = getLinkTurboDefaultPn();
                            reply.writeNoException();
                            reply.writeStringList(_result6);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IAIDLSLAService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAIDLSLAService.DESCRIPTOR;
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public boolean addUidToLinkTurboWhiteList(String uid) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    _data.writeString(uid);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public boolean removeUidInLinkTurboWhiteList(String uid) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    _data.writeString(uid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public String getLinkTurboWhiteList() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public List<SLAApp> getLinkTurboAppsTraffic() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<SLAApp> _result = _reply.createTypedArrayList(SLAApp.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public boolean setLinkTurboEnable(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
            public List<String> getLinkTurboDefaultPn() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAIDLSLAService.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
