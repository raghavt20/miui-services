package com.android.server.location.hardware.mtk.engineermode.V1_3;

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
import com.android.server.input.padkeyboard.iic.NanoSocketCallback;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd;
import com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd;
import com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd;
import com.android.server.wifi.ArpPacket;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import miui.android.services.internal.hidl.base.V1_0.IBase;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;

/* loaded from: classes.dex */
public interface IEmd extends com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd {
    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    IHwBinder asBinder();

    boolean connect() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void disconnect() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    String interfaceDescriptor() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    void notifySyspropsChanged() throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    void ping() throws RemoteException;

    ArrayList<Byte> readMnlConfigFile(ArrayList<Integer> arrayList) throws RemoteException;

    void sendNfcRequest(int i, ArrayList<Byte> arrayList) throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    void setHALInstrumentation() throws RemoteException;

    void setNfcResponseFunction(IEmNfcResponse iEmNfcResponse) throws RemoteException;

    @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    boolean writeMnlConfigFile(ArrayList<Byte> arrayList, ArrayList<Integer> arrayList2) throws RemoteException;

    /* loaded from: classes.dex */
    public static final class Proxy implements IEmd {
        public static final String INTF_EMD_1_3 = "vendor.mediatek.hardware.engineermode@1.3::IEmd";
        private IHwBinder mRemote;

        public Proxy(IHwBinder iHwBinder) {
            Objects.requireNonNull(iHwBinder);
            this.mRemote = iHwBinder;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btDoTest(int i, int i2, int i3, int i4, int i5, int i6, int i7) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32(i4);
            hwParcel.writeInt32(i5);
            hwParcel.writeInt32(i6);
            hwParcel.writeInt32(i7);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public ArrayList<Integer> btEndNoSigRxTest() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32Vector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public ArrayList<Byte> btHciCommandRun(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt8Vector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btInit() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean btIsBLEEnhancedSupport() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btIsBLESupport() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btIsComboSupport() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd
        public int btIsEmSupport() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btPollingStart() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btPollingStop() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean btStartNoSigRxTest(int i, int i2, int i3, int i4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32(i4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btStartRelayer(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btStopRelayer() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public int btUninit() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean clearItemsforRsc() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public boolean connect() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(53, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public void disconnect() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean genMdLogFilter(String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean getFilePathListWithCallBack(String str, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback iEmCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            hwParcel.writeStrongBinder(iEmCallback == null ? null : iEmCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd
        public int isGauge30Support() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd
        public int isNfcSupport() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException {
            return this.mRemote.linkToDeath(deathRecipient, j);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public ArrayList<Byte> readMnlConfigFile(ArrayList<Integer> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            hwParcel.writeInt32Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt8Vector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public void sendNfcRequest(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean sendToServer(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean sendToServerWithCallBack(String str, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback iEmCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            hwParcel.writeStrongBinder(iEmCallback == null ? null : iEmCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setBypassDis(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setBypassEn(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setBypassService(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public void setCallback(com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback iEmCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeStrongBinder(iEmCallback == null ? null : iEmCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setCtIREngMode(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setDisableC2kCap(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setDsbpSupport(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd
        public boolean setEmConfigure(String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_1);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setEmUsbType(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setEmUsbValue(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
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

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setImsTestMode(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setMdResetDelay(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setModemWarningEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setMoms(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public void setNfcResponseFunction(IEmNfcResponse iEmNfcResponse) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            hwParcel.writeStrongBinder(iEmNfcResponse == null ? null : iEmNfcResponse.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(52, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setOmxCoreLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setOmxVdecLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setOmxVencLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setPreferGprsMode(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setRadioCapabilitySwitchEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setSmsFormat(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setSvpLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setTestSimCardType(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setUsbOtgSwitch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setUsbPort(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setVdecDriverLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setVencDriverLogEnable(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setVolteMalPctid(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean setWcnCoreDump(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IEmd.Proxy.INTF_EMD_1_0);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.mediatek.hardware.engineermode@1.3::IEmd]@Proxy";
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(deathRecipient);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd
        public boolean writeMnlConfigFile(ArrayList<Byte> arrayList, ArrayList<Integer> arrayList2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(INTF_EMD_1_3);
            hwParcel.writeInt8Vector(arrayList);
            hwParcel.writeInt32Vector(arrayList2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IEmd {
        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public IHwBinder asBinder() {
            return this;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-22, 12, 88, CommunicationUtil.COMMAND_AUTH_52, -116, -101, -35, -76, -64, 82, -107, 125, -61, 84, -32, 119, -69, -36, -2, -56, 110, 107, -114, -117, -7, -34, 60, -36, 63, -60, 14, -109}, new byte[]{-27, 21, -43, -90, 81, 55, -122, 15, -38, CommunicationUtil.COMMAND_KB_BATTERY_LEVEL, 82, 89, 19, 22, -11, -38, 116, -21, -72, 107, 108, -20, 82, CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND, -40, -117, CommunicationUtil.SEND_RESTORE_COMMAND, 113, -32, -38, 55, -26}, new byte[]{99, -93, 35, -85, -5, -25, 48, 13, -112, -127, 102, -26, 71, 112, 32, -43, 73, 58, 120, -17, -118, -84, 123, -14, -27, -42, -96, 67, -65, -69, -92, -113}, new byte[]{-66, -27, CommunicationUtil.COMMAND_MCU_BOOT, -107, 121, -115, -5, -113, -11, 28, 119, -105, 121, -100, 105, -29, 86, 105, -79, 32, -9, 25, CommunicationUtil.COMMAND_KB_NFC, -117, -93, -97, -29, -30, -10, -48, CommunicationUtil.SEND_REPORT_ID_LONG_DATA, -100}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, CommunicationUtil.RESPONSE_TYPE, 19, -109, 36, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(Proxy.INTF_EMD_1_3, IEmd.Proxy.INTF_EMD_1_2, IEmd.Proxy.INTF_EMD_1_1, IEmd.Proxy.INTF_EMD_1_0, IBase.kInterfaceName));
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final String interfaceDescriptor() {
            return Proxy.INTF_EMD_1_3;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public void onTransact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    setCallback(com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback.asInterface(hwParcel.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    hwParcel2.send();
                    return;
                case 2:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean sendToServer = sendToServer(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(sendToServer);
                    hwParcel2.send();
                    return;
                case 3:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean sendToServerWithCallBack = sendToServerWithCallBack(hwParcel.readString(), com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback.asInterface(hwParcel.readStrongBinder()));
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(sendToServerWithCallBack);
                    hwParcel2.send();
                    return;
                case 4:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean smsFormat = setSmsFormat(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(smsFormat);
                    hwParcel2.send();
                    return;
                case 5:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean ctIREngMode = setCtIREngMode(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(ctIREngMode);
                    hwParcel2.send();
                    return;
                case 6:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean testSimCardType = setTestSimCardType(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(testSimCardType);
                    hwParcel2.send();
                    return;
                case 7:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean preferGprsMode = setPreferGprsMode(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(preferGprsMode);
                    hwParcel2.send();
                    return;
                case 8:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean radioCapabilitySwitchEnable = setRadioCapabilitySwitchEnable(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(radioCapabilitySwitchEnable);
                    hwParcel2.send();
                    return;
                case 9:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean disableC2kCap = setDisableC2kCap(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(disableC2kCap);
                    hwParcel2.send();
                    return;
                case 10:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean imsTestMode = setImsTestMode(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(imsTestMode);
                    hwParcel2.send();
                    return;
                case 11:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean dsbpSupport = setDsbpSupport(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(dsbpSupport);
                    hwParcel2.send();
                    return;
                case 12:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean volteMalPctid = setVolteMalPctid(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(volteMalPctid);
                    hwParcel2.send();
                    return;
                case 13:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btStartRelayer = btStartRelayer(hwParcel.readInt32(), hwParcel.readInt32());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btStartRelayer);
                    hwParcel2.send();
                    return;
                case 14:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btStopRelayer = btStopRelayer();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btStopRelayer);
                    hwParcel2.send();
                    return;
                case 15:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btIsBLESupport = btIsBLESupport();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btIsBLESupport);
                    hwParcel2.send();
                    return;
                case 16:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean btIsBLEEnhancedSupport = btIsBLEEnhancedSupport();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(btIsBLEEnhancedSupport);
                    hwParcel2.send();
                    return;
                case 17:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btInit = btInit();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btInit);
                    hwParcel2.send();
                    return;
                case 18:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btUninit = btUninit();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btUninit);
                    hwParcel2.send();
                    return;
                case 19:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btDoTest = btDoTest(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btDoTest);
                    hwParcel2.send();
                    return;
                case 20:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    ArrayList<Byte> btHciCommandRun = btHciCommandRun(hwParcel.readInt8Vector());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt8Vector(btHciCommandRun);
                    hwParcel2.send();
                    return;
                case 21:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean btStartNoSigRxTest = btStartNoSigRxTest(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(btStartNoSigRxTest);
                    hwParcel2.send();
                    return;
                case 22:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    ArrayList<Integer> btEndNoSigRxTest = btEndNoSigRxTest();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32Vector(btEndNoSigRxTest);
                    hwParcel2.send();
                    return;
                case 23:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btIsComboSupport = btIsComboSupport();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btIsComboSupport);
                    hwParcel2.send();
                    return;
                case 24:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btPollingStart = btPollingStart();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btPollingStart);
                    hwParcel2.send();
                    return;
                case 25:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    int btPollingStop = btPollingStop();
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeInt32(btPollingStop);
                    hwParcel2.send();
                    return;
                case 26:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean mdResetDelay = setMdResetDelay(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(mdResetDelay);
                    hwParcel2.send();
                    return;
                case 27:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean wcnCoreDump = setWcnCoreDump(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(wcnCoreDump);
                    hwParcel2.send();
                    return;
                case 28:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean omxVencLogEnable = setOmxVencLogEnable(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(omxVencLogEnable);
                    hwParcel2.send();
                    return;
                case IResultValue.MISYS_ESPIPE /* 29 */:
                    hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                    boolean omxVdecLogEnable = setOmxVdecLogEnable(hwParcel.readString());
                    hwParcel2.writeStatus(0);
                    hwParcel2.writeBool(omxVdecLogEnable);
                    hwParcel2.send();
                    return;
                default:
                    switch (i) {
                        case 30:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean vdecDriverLogEnable = setVdecDriverLogEnable(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(vdecDriverLogEnable);
                            hwParcel2.send();
                            return;
                        case IResultValue.MISYS_EMLINK /* 31 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean svpLogEnable = setSvpLogEnable(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(svpLogEnable);
                            hwParcel2.send();
                            return;
                        case 32:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean omxCoreLogEnable = setOmxCoreLogEnable(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(omxCoreLogEnable);
                            hwParcel2.send();
                            return;
                        case UsbKeyboardUtil.COMMAND_TOUCH_PAD_ENABLE /* 33 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean vencDriverLogEnable = setVencDriverLogEnable(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(vencDriverLogEnable);
                            hwParcel2.send();
                            return;
                        case 34:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean modemWarningEnable = setModemWarningEnable(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(modemWarningEnable);
                            hwParcel2.send();
                            return;
                        case UsbKeyboardUtil.COMMAND_BACK_LIGHT_ENABLE /* 35 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean genMdLogFilter = genMdLogFilter(hwParcel.readString(), hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(genMdLogFilter);
                            hwParcel2.send();
                            return;
                        case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean usbPort = setUsbPort(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(usbPort);
                            hwParcel2.send();
                            return;
                        case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean usbOtgSwitch = setUsbOtgSwitch(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(usbOtgSwitch);
                            hwParcel2.send();
                            return;
                        case 38:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean emUsbValue = setEmUsbValue(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(emUsbValue);
                            hwParcel2.send();
                            return;
                        case 39:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean emUsbType = setEmUsbType(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(emUsbType);
                            hwParcel2.send();
                            return;
                        case NanoSocketCallback.CALLBACK_TYPE_TOUCHPAD /* 40 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean bypassEn = setBypassEn(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(bypassEn);
                            hwParcel2.send();
                            return;
                        case 41:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean bypassDis = setBypassDis(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(bypassDis);
                            hwParcel2.send();
                            return;
                        case ArpPacket.ARP_ETHER_IPV4_LEN /* 42 */:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean bypassService = setBypassService(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(bypassService);
                            hwParcel2.send();
                            return;
                        case 43:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean moms = setMoms(hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(moms);
                            hwParcel2.send();
                            return;
                        case 44:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean clearItemsforRsc = clearItemsforRsc();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(clearItemsforRsc);
                            hwParcel2.send();
                            return;
                        case 45:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_0);
                            boolean filePathListWithCallBack = getFilePathListWithCallBack(hwParcel.readString(), com.android.server.location.hardware.mtk.engineermode.V1_0.IEmCallback.asInterface(hwParcel.readStrongBinder()));
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(filePathListWithCallBack);
                            hwParcel2.send();
                            return;
                        case 46:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_1);
                            boolean emConfigure = setEmConfigure(hwParcel.readString(), hwParcel.readString());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(emConfigure);
                            hwParcel2.send();
                            return;
                        case 47:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_2);
                            int btIsEmSupport = btIsEmSupport();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeInt32(btIsEmSupport);
                            hwParcel2.send();
                            return;
                        case 48:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_2);
                            int isNfcSupport = isNfcSupport();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeInt32(isNfcSupport);
                            hwParcel2.send();
                            return;
                        case 49:
                            hwParcel.enforceInterface(IEmd.Proxy.INTF_EMD_1_2);
                            int isGauge30Support = isGauge30Support();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeInt32(isGauge30Support);
                            hwParcel2.send();
                            return;
                        case UsbKeyboardUtil.COMMAND_MIAUTH_STEP3_TYPE1 /* 50 */:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            ArrayList<Byte> readMnlConfigFile = readMnlConfigFile(hwParcel.readInt32Vector());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeInt8Vector(readMnlConfigFile);
                            hwParcel2.send();
                            return;
                        case 51:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            boolean writeMnlConfigFile = writeMnlConfigFile(hwParcel.readInt8Vector(), hwParcel.readInt32Vector());
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(writeMnlConfigFile);
                            hwParcel2.send();
                            return;
                        case 52:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            setNfcResponseFunction(IEmNfcResponse.asInterface(hwParcel.readStrongBinder()));
                            hwParcel2.writeStatus(0);
                            hwParcel2.send();
                            return;
                        case 53:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            boolean connect = connect();
                            hwParcel2.writeStatus(0);
                            hwParcel2.writeBool(connect);
                            hwParcel2.send();
                            return;
                        case 54:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            disconnect();
                            hwParcel2.writeStatus(0);
                            hwParcel2.send();
                            return;
                        case 55:
                            hwParcel.enforceInterface(Proxy.INTF_EMD_1_3);
                            sendNfcRequest(hwParcel.readInt32(), hwParcel.readInt8Vector());
                            return;
                        default:
                            switch (i) {
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
            }
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final void ping() {
        }

        public IHwInterface queryLocalInterface(String str) {
            if (Proxy.INTF_EMD_1_3.equals(str)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String str) throws RemoteException {
            registerService(str);
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final void setHALInstrumentation() {
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_2.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_1.IEmd, com.android.server.location.hardware.mtk.engineermode.V1_0.IEmd
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }
    }

    static IEmd asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IEmd queryLocalInterface = iHwBinder.queryLocalInterface(Proxy.INTF_EMD_1_3);
        if (queryLocalInterface != null && (queryLocalInterface instanceof IEmd)) {
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

    static IEmd castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IEmd getService() throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
    }

    static IEmd getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(Proxy.INTF_EMD_1_3, str));
    }

    static IEmd getService(String str, boolean z) throws RemoteException {
        IHwBinder service = HwBinder.getService(Proxy.INTF_EMD_1_3, str, z);
        return asInterface(service);
    }

    static IEmd getService(boolean z) throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, z);
    }
}
