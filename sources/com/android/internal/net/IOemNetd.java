package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.net.IOemNetdUnsolicitedEventListener;

/* loaded from: classes.dex */
public interface IOemNetd extends IInterface {
    public static final String DESCRIPTOR = "com$android$internal$net$IOemNetd".replace('$', '.');
    public static final int SLAVEP2P_LOCAL_NET_ID = 98;

    boolean addMiuiFirewallSharedUid(int i) throws RemoteException;

    boolean attachXdpProg(boolean z, String str) throws RemoteException;

    int bindPort(ParcelFileDescriptor parcelFileDescriptor, int i, String str) throws RemoteException;

    boolean clearAllMap() throws RemoteException;

    void closeUnreachedPortFilter() throws RemoteException;

    boolean detachXdpProg(String str) throws RemoteException;

    int enableAutoForward(String str, int i, boolean z) throws RemoteException;

    boolean enableIptablesRestore(boolean z) throws RemoteException;

    boolean enableLimitter(boolean z) throws RemoteException;

    boolean enableMobileTrafficLimit(boolean z, String str) throws RemoteException;

    boolean enableQos(boolean z) throws RemoteException;

    boolean enableRps(String str, boolean z) throws RemoteException;

    boolean enableWmmer(boolean z) throws RemoteException;

    long getMiuiSlmVoipUdpAddress(int i) throws RemoteException;

    int getMiuiSlmVoipUdpPort(int i) throws RemoteException;

    long getShareStats(int i) throws RemoteException;

    boolean isAlive() throws RemoteException;

    boolean listenUidDataActivity(int i, int i2, int i3, int i4, boolean z) throws RemoteException;

    int lookupPort(int i, int i2) throws RemoteException;

    void notifyFirewallBlocked(int i, String str) throws RemoteException;

    void notifyUnreachedPort(int i, int i2, String str) throws RemoteException;

    void openUnreachedPortFilter() throws RemoteException;

    void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener iOemNetdUnsolicitedEventListener) throws RemoteException;

    boolean setCurrentNetworkState(int i) throws RemoteException;

    void setEthernetVlan() throws RemoteException;

    boolean setLimit(boolean z, long j) throws RemoteException;

    boolean setMiuiFirewallRule(String str, int i, int i2, int i3) throws RemoteException;

    boolean setMiuiSlmBpfUid(int i) throws RemoteException;

    boolean setMobileTrafficLimit(boolean z, long j) throws RemoteException;

    void setPidForPackage(String str, int i, int i2) throws RemoteException;

    boolean setQos(int i, int i2, int i3, boolean z) throws RemoteException;

    boolean unbindPort(int i) throws RemoteException;

    void unregisterOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener iOemNetdUnsolicitedEventListener) throws RemoteException;

    boolean updateIface(String str) throws RemoteException;

    boolean updateWmm(int i, int i2) throws RemoteException;

    boolean whiteListUid(int i, boolean z) throws RemoteException;

    boolean whiteListUidForMobileTraffic(int i, boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetd {
        @Override // com.android.internal.net.IOemNetd
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableWmmer(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableLimitter(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean updateWmm(int uid, int wmm) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean whiteListUid(int uid, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setLimit(boolean enabled, long rate) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableIptablesRestore(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean listenUidDataActivity(int protocol, int uid, int label, int timeout, boolean listen) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean updateIface(String iface) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean addMiuiFirewallSharedUid(int uid) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setCurrentNetworkState(int state) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableRps(String iface, boolean enable) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void notifyFirewallBlocked(int code, String packageName) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableQos(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setQos(int protocol, int uid, int tos, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void setPidForPackage(String packageName, int pid, int uid) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableMobileTrafficLimit(boolean enabled, String iface) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMobileTrafficLimit(boolean enabled, long rate) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean whiteListUidForMobileTraffic(int uid, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMiuiSlmBpfUid(int uid) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public long getMiuiSlmVoipUdpAddress(int uid) throws RemoteException {
            return 0L;
        }

        @Override // com.android.internal.net.IOemNetd
        public int getMiuiSlmVoipUdpPort(int uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int enableAutoForward(String addr, int fwmark, boolean enabled) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public void notifyUnreachedPort(int port, int ip, String interfaceName) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void unregisterOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void openUnreachedPortFilter() throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void closeUnreachedPortFilter() throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean attachXdpProg(boolean isNative, String ifaceName) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean detachXdpProg(String ifaceName) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public int bindPort(ParcelFileDescriptor sockFd, int port, String localMac) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean unbindPort(int port) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public int lookupPort(int type, int value) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean clearAllMap() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void setEthernetVlan() throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public long getShareStats(int type) throws RemoteException {
            return 0L;
        }

        @Override // com.android.internal.net.IOemNetd
        public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetd {
        static final int TRANSACTION_addMiuiFirewallSharedUid = 10;
        static final int TRANSACTION_attachXdpProg = 29;
        static final int TRANSACTION_bindPort = 31;
        static final int TRANSACTION_clearAllMap = 34;
        static final int TRANSACTION_closeUnreachedPortFilter = 28;
        static final int TRANSACTION_detachXdpProg = 30;
        static final int TRANSACTION_enableAutoForward = 24;
        static final int TRANSACTION_enableIptablesRestore = 7;
        static final int TRANSACTION_enableLimitter = 3;
        static final int TRANSACTION_enableMobileTrafficLimit = 18;
        static final int TRANSACTION_enableQos = 15;
        static final int TRANSACTION_enableRps = 13;
        static final int TRANSACTION_enableWmmer = 2;
        static final int TRANSACTION_getMiuiSlmVoipUdpAddress = 22;
        static final int TRANSACTION_getMiuiSlmVoipUdpPort = 23;
        static final int TRANSACTION_getShareStats = 36;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_listenUidDataActivity = 8;
        static final int TRANSACTION_lookupPort = 33;
        static final int TRANSACTION_notifyFirewallBlocked = 14;
        static final int TRANSACTION_notifyUnreachedPort = 25;
        static final int TRANSACTION_openUnreachedPortFilter = 27;
        static final int TRANSACTION_registerOemUnsolicitedEventListener = 37;
        static final int TRANSACTION_setCurrentNetworkState = 12;
        static final int TRANSACTION_setEthernetVlan = 35;
        static final int TRANSACTION_setLimit = 6;
        static final int TRANSACTION_setMiuiFirewallRule = 11;
        static final int TRANSACTION_setMiuiSlmBpfUid = 21;
        static final int TRANSACTION_setMobileTrafficLimit = 19;
        static final int TRANSACTION_setPidForPackage = 17;
        static final int TRANSACTION_setQos = 16;
        static final int TRANSACTION_unbindPort = 32;
        static final int TRANSACTION_unregisterOemUnsolicitedEventListener = 26;
        static final int TRANSACTION_updateIface = 9;
        static final int TRANSACTION_updateWmm = 4;
        static final int TRANSACTION_whiteListUid = 5;
        static final int TRANSACTION_whiteListUidForMobileTraffic = 20;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetd)) {
                return (IOemNetd) iin;
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
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _result = isAlive();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            return true;
                        case 2:
                            boolean _arg0 = data.readBoolean();
                            boolean _result2 = enableWmmer(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            return true;
                        case 3:
                            boolean _arg02 = data.readBoolean();
                            boolean _result3 = enableLimitter(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            return true;
                        case 4:
                            int _arg03 = data.readInt();
                            int _arg1 = data.readInt();
                            boolean _result4 = updateWmm(_arg03, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            return true;
                        case 5:
                            int _arg04 = data.readInt();
                            boolean _arg12 = data.readBoolean();
                            boolean _result5 = whiteListUid(_arg04, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            return true;
                        case 6:
                            boolean _arg05 = data.readBoolean();
                            long _arg13 = data.readLong();
                            boolean _result6 = setLimit(_arg05, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            return true;
                        case 7:
                            boolean _arg06 = data.readBoolean();
                            boolean _result7 = enableIptablesRestore(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            return true;
                        case 8:
                            int _arg07 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            boolean _arg4 = data.readBoolean();
                            boolean _result8 = listenUidDataActivity(_arg07, _arg14, _arg2, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            return true;
                        case 9:
                            String _arg08 = data.readString();
                            boolean _result9 = updateIface(_arg08);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            return true;
                        case 10:
                            int _arg09 = data.readInt();
                            boolean _result10 = addMiuiFirewallSharedUid(_arg09);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            return true;
                        case 11:
                            String _arg010 = data.readString();
                            int _arg15 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            boolean _result11 = setMiuiFirewallRule(_arg010, _arg15, _arg22, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            return true;
                        case 12:
                            int _arg011 = data.readInt();
                            boolean _result12 = setCurrentNetworkState(_arg011);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            return true;
                        case 13:
                            String _arg012 = data.readString();
                            boolean _arg16 = data.readBoolean();
                            boolean _result13 = enableRps(_arg012, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            return true;
                        case 14:
                            int _arg013 = data.readInt();
                            String _arg17 = data.readString();
                            notifyFirewallBlocked(_arg013, _arg17);
                            reply.writeNoException();
                            return true;
                        case 15:
                            boolean _arg014 = data.readBoolean();
                            boolean _result14 = enableQos(_arg014);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            return true;
                        case 16:
                            int _arg015 = data.readInt();
                            int _arg18 = data.readInt();
                            int _arg23 = data.readInt();
                            boolean _arg33 = data.readBoolean();
                            boolean _result15 = setQos(_arg015, _arg18, _arg23, _arg33);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            return true;
                        case 17:
                            String _arg016 = data.readString();
                            int _arg19 = data.readInt();
                            int _arg24 = data.readInt();
                            setPidForPackage(_arg016, _arg19, _arg24);
                            reply.writeNoException();
                            return true;
                        case 18:
                            boolean _arg017 = data.readBoolean();
                            String _arg110 = data.readString();
                            boolean _result16 = enableMobileTrafficLimit(_arg017, _arg110);
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            return true;
                        case 19:
                            boolean _arg018 = data.readBoolean();
                            long _arg111 = data.readLong();
                            boolean _result17 = setMobileTrafficLimit(_arg018, _arg111);
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            return true;
                        case 20:
                            int _arg019 = data.readInt();
                            boolean _arg112 = data.readBoolean();
                            boolean _result18 = whiteListUidForMobileTraffic(_arg019, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            return true;
                        case 21:
                            int _arg020 = data.readInt();
                            boolean _result19 = setMiuiSlmBpfUid(_arg020);
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            return true;
                        case 22:
                            int _arg021 = data.readInt();
                            long _result20 = getMiuiSlmVoipUdpAddress(_arg021);
                            reply.writeNoException();
                            reply.writeLong(_result20);
                            return true;
                        case 23:
                            int _arg022 = data.readInt();
                            int _result21 = getMiuiSlmVoipUdpPort(_arg022);
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            return true;
                        case 24:
                            String _arg023 = data.readString();
                            int _arg113 = data.readInt();
                            boolean _arg25 = data.readBoolean();
                            int _result22 = enableAutoForward(_arg023, _arg113, _arg25);
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            return true;
                        case 25:
                            int _arg024 = data.readInt();
                            int _arg114 = data.readInt();
                            String _arg26 = data.readString();
                            notifyUnreachedPort(_arg024, _arg114, _arg26);
                            reply.writeNoException();
                            return true;
                        case 26:
                            IOemNetdUnsolicitedEventListener _arg025 = IOemNetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                            unregisterOemUnsolicitedEventListener(_arg025);
                            reply.writeNoException();
                            return true;
                        case 27:
                            openUnreachedPortFilter();
                            reply.writeNoException();
                            return true;
                        case 28:
                            closeUnreachedPortFilter();
                            reply.writeNoException();
                            return true;
                        case 29:
                            boolean _arg026 = data.readBoolean();
                            String _arg115 = data.readString();
                            boolean _result23 = attachXdpProg(_arg026, _arg115);
                            reply.writeNoException();
                            reply.writeBoolean(_result23);
                            return true;
                        case 30:
                            String _arg027 = data.readString();
                            boolean _result24 = detachXdpProg(_arg027);
                            reply.writeNoException();
                            reply.writeBoolean(_result24);
                            return true;
                        case 31:
                            ParcelFileDescriptor _arg028 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            int _arg116 = data.readInt();
                            String _arg27 = data.readString();
                            int _result25 = bindPort(_arg028, _arg116, _arg27);
                            reply.writeNoException();
                            reply.writeInt(_result25);
                            return true;
                        case 32:
                            int _arg029 = data.readInt();
                            boolean _result26 = unbindPort(_arg029);
                            reply.writeNoException();
                            reply.writeBoolean(_result26);
                            return true;
                        case 33:
                            int _arg030 = data.readInt();
                            int _arg117 = data.readInt();
                            int _result27 = lookupPort(_arg030, _arg117);
                            reply.writeNoException();
                            reply.writeInt(_result27);
                            return true;
                        case 34:
                            boolean _result28 = clearAllMap();
                            reply.writeNoException();
                            reply.writeBoolean(_result28);
                            return true;
                        case 35:
                            setEthernetVlan();
                            reply.writeNoException();
                            return true;
                        case 36:
                            int _arg031 = data.readInt();
                            long _result29 = getShareStats(_arg031);
                            reply.writeNoException();
                            reply.writeLong(_result29);
                            return true;
                        case 37:
                            IOemNetdUnsolicitedEventListener _arg032 = IOemNetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                            registerOemUnsolicitedEventListener(_arg032);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IOemNetd {
            private IBinder mRemote;

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

            @Override // com.android.internal.net.IOemNetd
            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableWmmer(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableLimitter(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean updateWmm(int uid, int wmm) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(wmm);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean whiteListUid(int uid, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeBoolean(add);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setLimit(boolean enabled, long rate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeLong(rate);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableIptablesRestore(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean listenUidDataActivity(int protocol, int uid, int label, int timeout, boolean listen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(protocol);
                    _data.writeInt(uid);
                    _data.writeInt(label);
                    _data.writeInt(timeout);
                    _data.writeBoolean(listen);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean updateIface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean addMiuiFirewallSharedUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(uid);
                    _data.writeInt(rule);
                    _data.writeInt(type);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setCurrentNetworkState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableRps(String iface, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void notifyFirewallBlocked(int code, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(packageName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableQos(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setQos(int protocol, int uid, int tos, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(protocol);
                    _data.writeInt(uid);
                    _data.writeInt(tos);
                    _data.writeBoolean(add);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setPidForPackage(String packageName, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableMobileTrafficLimit(boolean enabled, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeString(iface);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMobileTrafficLimit(boolean enabled, long rate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeLong(rate);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean whiteListUidForMobileTraffic(int uid, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeBoolean(add);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMiuiSlmBpfUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public long getMiuiSlmVoipUdpAddress(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int getMiuiSlmVoipUdpPort(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int enableAutoForward(String addr, int fwmark, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeInt(fwmark);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void notifyUnreachedPort(int port, int ip, String interfaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(port);
                    _data.writeInt(ip);
                    _data.writeString(interfaceName);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void unregisterOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void openUnreachedPortFilter() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void closeUnreachedPortFilter() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean attachXdpProg(boolean isNative, String ifaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(isNative);
                    _data.writeString(ifaceName);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean detachXdpProg(String ifaceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifaceName);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int bindPort(ParcelFileDescriptor sockFd, int port, String localMac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(sockFd, 0);
                    _data.writeInt(port);
                    _data.writeString(localMac);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean unbindPort(int port) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(port);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int lookupPort(int type, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(value);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean clearAllMap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setEthernetVlan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public long getShareStats(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
