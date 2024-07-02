package com.android.server.location.hardware.mtk.engineermode.V1_3;

import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
public interface IEmNfcResponse extends IBase {
    IHwBinder asBinder();

    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    DebugInfo getDebugInfo() throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void response(ArrayList<Byte> arrayList) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    /* loaded from: classes.dex */
    public static final class Proxy implements IEmNfcResponse {
        public static final String INTF_EM_NFC_RESPONSE_1_3 = "vendor.mediatek.hardware.engineermode@1.3::IEmNfcResponse";
        private IHwBinder mRemote;

        public Proxy(IHwBinder iHwBinder) {
            Objects.requireNonNull(iHwBinder);
            this.mRemote = iHwBinder;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            hwParcel.writeNativeHandle(nativeHandle);
            hwParcel.writeStringVector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256131655, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257049926, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                DebugInfo debugInfo = new DebugInfo();
                debugInfo.readFromParcel(hwParcel2);
                return debugInfo;
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256398152, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                ArrayList<byte[]> arrayList = new ArrayList<>();
                HwBlob readBuffer = hwParcel2.readBuffer(16L);
                int int32 = readBuffer.getInt32(8L);
                HwBlob readEmbeddedBuffer = hwParcel2.readEmbeddedBuffer(int32 * 32, readBuffer.handle(), 0L, true);
                arrayList.clear();
                for (int i = 0; i < int32; i++) {
                    byte[] bArr = new byte[32];
                    readEmbeddedBuffer.copyToInt8Array(i * 32, bArr, 32);
                    arrayList.add(bArr);
                }
                return arrayList;
            } finally {
                hwParcel2.release();
            }
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256067662, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readStringVector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public String interfaceDescriptor() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256136003, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readString();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException {
            return this.mRemote.linkToDeath(deathRecipient, j);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257120595, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void ping() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256921159, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void response(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EM_NFC_RESPONSE_1_3);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void setHALInstrumentation() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256462420, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.mediatek.hardware.engineermode@1.3::IEmNfcResponse]@Proxy";
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(deathRecipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IEmNfcResponse {
        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public IHwBinder asBinder() {
            return this;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-85, -50, CommunicationUtil.COMMAND_KB_FEATURE_POWER, 115, CommunicationUtil.COMMAND_AUTH_52, 76, CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 90, -2, 108, 122, -85, -24, -36, 112, 59, 2, -40, -90, 98, -56, -29, 107, 119, -81, CommunicationUtil.COMMAND_RESPONSE_MCU_STATUS, 115, -49, 123, -33, -101, -33}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, CommunicationUtil.RESPONSE_TYPE, 19, -109, 36, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(Proxy.INTF_EM_NFC_RESPONSE_1_3, miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final String interfaceDescriptor() {
            return Proxy.INTF_EM_NFC_RESPONSE_1_3;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public void onTransact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    hwParcel.enforceInterface(Proxy.INTF_EM_NFC_RESPONSE_1_3);
                    response(hwParcel.readInt8Vector());
                    return;
                case 256067662:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    ArrayList<String> interfaceChain = interfaceChain();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeStringVector(interfaceChain);
                    hwParcel2.send();
                    return;
                case 256131655:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    debug(hwParcel.readNativeHandle(), hwParcel.readStringVector());
                    hwParcel2.writeStatus(0);
                    hwParcel2.send();
                    return;
                case 256136003:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    String interfaceDescriptor = interfaceDescriptor();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeString(interfaceDescriptor);
                    hwParcel2.send();
                    return;
                case 256398152:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    ArrayList<byte[]> hashChain = getHashChain();
                    hwParcel2.writeStatus(0);
                    HwBlob hwBlob = new HwBlob(16);
                    int size = hashChain.size();
                    hwBlob.putInt32(8L, size);
                    hwBlob.putBool(12L, false);
                    HwBlob hwBlob2 = new HwBlob(size * 32);
                    for (int i3 = 0; i3 < size; i3++) {
                        long j = i3 * 32;
                        byte[] bArr = hashChain.get(i3);
                        if (bArr == null || bArr.length != 32) {
                            throw new IllegalArgumentException("Array element is not of the expected length");
                        }
                        hwBlob2.putInt8Array(j, bArr);
                    }
                    hwBlob.putBlob(0L, hwBlob2);
                    hwParcel2.writeBuffer(hwBlob);
                    hwParcel2.send();
                    return;
                case 256462420:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    setHALInstrumentation();
                    return;
                case 256921159:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    ping();
                    hwParcel2.writeStatus(0);
                    hwParcel2.send();
                    return;
                case 257049926:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    DebugInfo debugInfo = getDebugInfo();
                    hwParcel2.writeStatus(0);
                    debugInfo.writeToParcel(hwParcel2);
                    hwParcel2.send();
                    return;
                case 257120595:
                    hwParcel.enforceInterface(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName);
                    notifySyspropsChanged();
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final void ping() {
        }

        public IHwInterface queryLocalInterface(String str) {
            if (Proxy.INTF_EM_NFC_RESPONSE_1_3.equals(str)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String str) throws RemoteException {
            registerService(str);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final void setHALInstrumentation() {
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmNfcResponse
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }
    }

    static IEmNfcResponse asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IEmNfcResponse queryLocalInterface = iHwBinder.queryLocalInterface(Proxy.INTF_EM_NFC_RESPONSE_1_3);
        if (queryLocalInterface != null && (queryLocalInterface instanceof IEmNfcResponse)) {
            return queryLocalInterface;
        }
        Proxy proxy = new Proxy(iHwBinder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(miui.android.services.internal.hidl.base.V1_0.IBase.kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    static IEmNfcResponse castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IEmNfcResponse getService() throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
    }

    static IEmNfcResponse getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(Proxy.INTF_EM_NFC_RESPONSE_1_3, str));
    }

    static IEmNfcResponse getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(Proxy.INTF_EM_NFC_RESPONSE_1_3, str, z));
    }

    static IEmNfcResponse getService(boolean z) throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, z);
    }
}
