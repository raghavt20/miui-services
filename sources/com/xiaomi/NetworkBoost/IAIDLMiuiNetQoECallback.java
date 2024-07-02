package com.xiaomi.NetworkBoost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IAIDLMiuiNetQoECallback extends IInterface {

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLMiuiNetQoECallback {

        /* loaded from: classes.dex */
        public static class a implements IAIDLMiuiNetQoECallback {
            public static IAIDLMiuiNetQoECallback b;
            public IBinder a;

            public a(IBinder iBinder) {
                this.a = iBinder;
            }

            @Override // android.os.IInterface
            public final IBinder asBinder() {
                return this.a;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback
            public final void masterQoECallBack(NetLinkLayerQoE netLinkLayerQoE) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
                    if (netLinkLayerQoE != null) {
                        obtain.writeInt(1);
                        netLinkLayerQoE.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (!this.a.transact(1, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().masterQoECallBack(netLinkLayerQoE);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback
            public final void slaveQoECallBack(NetLinkLayerQoE netLinkLayerQoE) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
                    if (netLinkLayerQoE != null) {
                        obtain.writeInt(1);
                        netLinkLayerQoE.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (!this.a.transact(2, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().slaveQoECallBack(netLinkLayerQoE);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
        }

        public static IAIDLMiuiNetQoECallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IAIDLMiuiNetQoECallback)) {
                return (IAIDLMiuiNetQoECallback) queryLocalInterface;
            }
            return new a(iBinder);
        }

        public static IAIDLMiuiNetQoECallback getDefaultImpl() {
            return a.b;
        }

        public static boolean setDefaultImpl(IAIDLMiuiNetQoECallback iAIDLMiuiNetQoECallback) {
            if (a.b != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iAIDLMiuiNetQoECallback == null) {
                return false;
            }
            a.b = iAIDLMiuiNetQoECallback;
            return true;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            NetLinkLayerQoE netLinkLayerQoE = null;
            if (i == 1) {
                parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
                if (parcel.readInt() != 0) {
                    netLinkLayerQoE = NetLinkLayerQoE.CREATOR.createFromParcel(parcel);
                }
                masterQoECallBack(netLinkLayerQoE);
                return true;
            }
            if (i != 2) {
                if (i != 1598968902) {
                    return super.onTransact(i, parcel, parcel2, i2);
                }
                parcel2.writeString("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
                return true;
            }
            parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback");
            if (parcel.readInt() != 0) {
                netLinkLayerQoE = NetLinkLayerQoE.CREATOR.createFromParcel(parcel);
            }
            slaveQoECallBack(netLinkLayerQoE);
            return true;
        }
    }

    void masterQoECallBack(NetLinkLayerQoE netLinkLayerQoE) throws RemoteException;

    void slaveQoECallBack(NetLinkLayerQoE netLinkLayerQoE) throws RemoteException;
}
