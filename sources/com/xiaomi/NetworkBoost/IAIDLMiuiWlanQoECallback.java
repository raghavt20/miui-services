package com.xiaomi.NetworkBoost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/* loaded from: classes.dex */
public interface IAIDLMiuiWlanQoECallback extends IInterface {

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAIDLMiuiWlanQoECallback {

        /* loaded from: classes.dex */
        public static class a implements IAIDLMiuiWlanQoECallback {
            public static IAIDLMiuiWlanQoECallback b;
            public IBinder a;

            public a(IBinder iBinder) {
                this.a = iBinder;
            }

            public static /* synthetic */ void a(Parcel parcel, String str, String str2) {
                parcel.writeString(str);
                parcel.writeString(str2);
            }

            public static /* synthetic */ void b(Parcel parcel, String str, String str2) {
                parcel.writeString(str);
                parcel.writeString(str2);
            }

            @Override // android.os.IInterface
            public final IBinder asBinder() {
                return this.a;
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback
            public final void wlanQoEReportUpdateMaster(Map<String, String> map) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
                    if (map == null) {
                        obtain.writeInt(-1);
                    } else {
                        obtain.writeInt(map.size());
                        map.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback$Stub$a$$ExternalSyntheticLambda0
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                IAIDLMiuiWlanQoECallback.Stub.a.a(obtain, (String) obj, (String) obj2);
                            }
                        });
                    }
                    if (!this.a.transact(1, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().wlanQoEReportUpdateMaster(map);
                    } else {
                        obtain2.readException();
                    }
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback
            public final void wlanQoEReportUpdateSlave(Map<String, String> map) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
                    if (map == null) {
                        obtain.writeInt(-1);
                    } else {
                        obtain.writeInt(map.size());
                        map.forEach(new BiConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback$Stub$a$$ExternalSyntheticLambda1
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                IAIDLMiuiWlanQoECallback.Stub.a.b(obtain, (String) obj, (String) obj2);
                            }
                        });
                    }
                    if (!this.a.transact(2, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().wlanQoEReportUpdateSlave(map);
                    } else {
                        obtain2.readException();
                    }
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
        }

        public static IAIDLMiuiWlanQoECallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IAIDLMiuiWlanQoECallback)) {
                return (IAIDLMiuiWlanQoECallback) queryLocalInterface;
            }
            return new a(iBinder);
        }

        public static IAIDLMiuiWlanQoECallback getDefaultImpl() {
            return a.b;
        }

        public static boolean setDefaultImpl(IAIDLMiuiWlanQoECallback iAIDLMiuiWlanQoECallback) {
            if (a.b != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iAIDLMiuiWlanQoECallback == null) {
                return false;
            }
            a.b = iAIDLMiuiWlanQoECallback;
            return true;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, final Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            final HashMap hashMap;
            if (i == 1) {
                parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
                int readInt = parcel.readInt();
                hashMap = readInt >= 0 ? new HashMap() : null;
                IntStream.range(0, readInt).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback$Stub$$ExternalSyntheticLambda1
                    @Override // java.util.function.IntConsumer
                    public final void accept(int i3) {
                        hashMap.put(r0.readString(), parcel.readString());
                    }
                });
                wlanQoEReportUpdateMaster(hashMap);
                parcel2.writeNoException();
                return true;
            }
            if (i != 2) {
                if (i != 1598968902) {
                    return super.onTransact(i, parcel, parcel2, i2);
                }
                parcel2.writeString("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
                return true;
            }
            parcel.enforceInterface("com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback");
            int readInt2 = parcel.readInt();
            hashMap = readInt2 >= 0 ? new HashMap() : null;
            IntStream.range(0, readInt2).forEach(new IntConsumer() { // from class: com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback$Stub$$ExternalSyntheticLambda0
                @Override // java.util.function.IntConsumer
                public final void accept(int i3) {
                    hashMap.put(r0.readString(), parcel.readString());
                }
            });
            wlanQoEReportUpdateSlave(hashMap);
            parcel2.writeNoException();
            return true;
        }
    }

    void wlanQoEReportUpdateMaster(Map<String, String> map) throws RemoteException;

    void wlanQoEReportUpdateSlave(Map<String, String> map) throws RemoteException;
}
