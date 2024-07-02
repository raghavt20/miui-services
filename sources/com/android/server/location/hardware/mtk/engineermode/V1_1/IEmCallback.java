package com.android.server.location.hardware.mtk.engineermode.V1_1;

import android.hidl.base.V1_0.DebugInfo;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import miui.android.services.internal.hidl.base.V1_0.IBase;

/* loaded from: classes.dex */
public interface IEmCallback extends com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback {
    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    IHwBinder asBinder();

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    String interfaceDescriptor() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    void notifySyspropsChanged() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    void ping() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    void setHALInstrumentation() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    /* loaded from: classes.dex */
    public static final class Proxy implements IEmCallback {
        public static final String INTF_EM_CALLBACK_1_1 = "vendor.mediatek.hardware.engineermode@1.1::IEmCallback";
        private IHwBinder mRemote;

        public Proxy(IHwBinder iHwBinder) {
            Objects.requireNonNull(iHwBinder);
            this.mRemote = iHwBinder;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public boolean callbackToClient(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmCallback.Proxy.INTF_EM_CALLBACK_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public String interfaceDescriptor() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException {
            return this.mRemote.linkToDeath(deathRecipient, j);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257120595, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public void ping() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256921159, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public void setHALInstrumentation() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
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
                return "[class or subclass of vendor.mediatek.hardware.engineermode@1.1::IEmCallback]@Proxy";
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(deathRecipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IEmCallback {
        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public IHwBinder asBinder() {
            return this;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{14, 83, 28, -4, CommunicationUtil.COMMAN_PAD_EXTERNAL_FLAG, 98, -79, -83, 71, 92, -81, 120, CommunicationUtil.TOUCHPAD_ADDRESS, 84, 28, 92, 58, 73, -62, -33, -63, 120, CommunicationUtil.TOUCHPAD_ADDRESS, 9, -60, 101, -42, 36, 34, 68, -3, -34}, new byte[]{-27, 112, 36, 113, -82, 20, -75, -113, 38, -39, 101, -23, CommunicationUtil.PAD_ADDRESS, -21, 119, CommunicationUtil.COMMAND_KEYBOARD_UPGRADE_STATUS, -10, CommunicationUtil.COMMAND_KB_FEATURE_SLEEP, -108, 42, CommunicationUtil.SEND_RESTORE_COMMAND, -83, 7, -109, CommunicationUtil.COMMAND_G_SENSOR, 101, -90, CommunicationUtil.COMMAND_G_SENSOR, -55, 73, 93, -105}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, CommunicationUtil.RESPONSE_TYPE, 19, -109, 36, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(Proxy.INTF_EM_CALLBACK_1_1, IEmCallback.Proxy.INTF_EM_CALLBACK_1_0, IBase.kInterfaceName));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final String interfaceDescriptor() {
            return Proxy.INTF_EM_CALLBACK_1_1;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public void onTransact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    hwParcel.enforceInterface(IEmCallback.Proxy.INTF_EM_CALLBACK_1_0);
                    boolean callbackToClient = callbackToClient(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(callbackToClient);
                    hwParcel2.send();
                    return;
                case 256067662:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    ArrayList<String> interfaceChain = interfaceChain();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeStringVector(interfaceChain);
                    hwParcel2.send();
                    return;
                case 256131655:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    debug(hwParcel.readNativeHandle(), hwParcel.readStringVector());
                    hwParcel2.writeStatus(0);
                    hwParcel2.send();
                    return;
                case 256136003:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    String interfaceDescriptor = interfaceDescriptor();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeString(interfaceDescriptor);
                    hwParcel2.send();
                    return;
                case 256398152:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
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
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    setHALInstrumentation();
                    return;
                case 256921159:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    ping();
                    hwParcel2.writeStatus(0);
                    hwParcel2.send();
                    return;
                case 257049926:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    DebugInfo debugInfo = getDebugInfo();
                    hwParcel2.writeStatus(0);
                    debugInfo.writeToParcel(hwParcel2);
                    hwParcel2.send();
                    return;
                case 257120595:
                    hwParcel.enforceInterface(IBase.kInterfaceName);
                    notifySyspropsChanged();
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final void ping() {
        }

        public IHwInterface queryLocalInterface(String str) {
            if (Proxy.INTF_EM_CALLBACK_1_1.equals(str)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String str) throws RemoteException {
            registerService(str);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final void setHALInstrumentation() {
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmCallback, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }
    }

    static IEmCallback asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IEmCallback queryLocalInterface = iHwBinder.queryLocalInterface(Proxy.INTF_EM_CALLBACK_1_1);
        if (queryLocalInterface != null && (queryLocalInterface instanceof IEmCallback)) {
            return queryLocalInterface;
        }
        Proxy proxy = new Proxy(iHwBinder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(IBase.kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    static IEmCallback castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IEmCallback getService() throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
    }

    static IEmCallback getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(Proxy.INTF_EM_CALLBACK_1_1, str));
    }

    static IEmCallback getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(Proxy.INTF_EM_CALLBACK_1_1, str, z));
    }

    static IEmCallback getService(boolean z) throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, z);
    }
}
