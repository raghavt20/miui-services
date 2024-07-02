package com.xiaomi.NetworkBoost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

/* loaded from: classes.dex */
public interface IAIDLMiuiNetSelectCallback extends IInterface {

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLMiuiNetSelectCallback {

        /* loaded from: classes.dex */
        public static class a implements IAIDLMiuiNetSelectCallback {
            public static IAIDLMiuiNetSelectCallback b;
            public IBinder a;

            public a(IBinder iBinder) {
                this.a = iBinder;
            }

            @Override // android.os.IInterface
            public final IBinder asBinder() {
                return this.a;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback
            public final void avaliableBssidCb(List<String> list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
                    obtain.writeStringList(list);
                    if (!this.a.transact(1, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().avaliableBssidCb(list);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback
            public final void connectionStatusCb(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
                    obtain.writeInt(i);
                    if (!this.a.transact(2, obtain, null, 1) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().connectionStatusCb(i);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
        }

        public static IAIDLMiuiNetSelectCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IAIDLMiuiNetSelectCallback)) {
                return (IAIDLMiuiNetSelectCallback) queryLocalInterface;
            }
            return new a(iBinder);
        }

        public static IAIDLMiuiNetSelectCallback getDefaultImpl() {
            return a.b;
        }

        public static boolean setDefaultImpl(IAIDLMiuiNetSelectCallback iAIDLMiuiNetSelectCallback) {
            if (a.b != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iAIDLMiuiNetSelectCallback == null) {
                return false;
            }
            a.b = iAIDLMiuiNetSelectCallback;
            return true;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1) {
                parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
                avaliableBssidCb(parcel.createStringArrayList());
                return true;
            }
            if (i == 2) {
                parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
                connectionStatusCb(parcel.readInt());
                return true;
            }
            if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel2.writeString("com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback");
            return true;
        }
    }

    void avaliableBssidCb(List<String> list) throws RemoteException;

    void connectionStatusCb(int i) throws RemoteException;
}
