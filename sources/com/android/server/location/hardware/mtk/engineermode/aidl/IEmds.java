package com.android.server.location.hardware.mtk.engineermode.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.server.location.hardware.mtk.engineermode.aidl.IEmCallbacks;
import com.android.server.location.hardware.mtk.engineermode.aidl.IEmResponses;

/* loaded from: classes.dex */
public interface IEmds extends IInterface {
    public static final String DESCRIPTOR = "vendor.mediatek.hardware.engineermode.IEmds";
    public static final String HASH = "de2f01ae4c46a25b30928e5a2edf9a1df3132225";
    public static final int VERSION = 1;

    int btDoTest(int i, int i2, int i3, int i4, int i5, int i6, int i7) throws RemoteException;

    int[] btEndNoSigRxTest() throws RemoteException;

    byte[] btHciCommandRun(byte[] bArr) throws RemoteException;

    int btInit() throws RemoteException;

    boolean btIsBLEEnhancedSupport() throws RemoteException;

    int btIsBLESupport() throws RemoteException;

    int btIsComboSupport() throws RemoteException;

    int btIsEmSupport() throws RemoteException;

    int btPollingStart() throws RemoteException;

    int btPollingStop() throws RemoteException;

    boolean btStartNoSigRxTest(int i, int i2, int i3, int i4) throws RemoteException;

    int btStartRelayer(int i, int i2) throws RemoteException;

    int btStopRelayer() throws RemoteException;

    int btUninit() throws RemoteException;

    boolean connect() throws RemoteException;

    void disconnect() throws RemoteException;

    boolean getFilePathListWithCallBack(String str, IEmCallbacks iEmCallbacks) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    int isGauge30Support() throws RemoteException;

    int isNfcSupport() throws RemoteException;

    boolean readFileConfigWithCallBack(String str, IEmCallbacks iEmCallbacks) throws RemoteException;

    byte[] readMnlConfigFile(int[] iArr) throws RemoteException;

    void sendNfcRequest(int i, byte[] bArr) throws RemoteException;

    boolean sendToServer(String str) throws RemoteException;

    boolean sendToServerWithCallBack(String str, IEmCallbacks iEmCallbacks) throws RemoteException;

    boolean setBypassDis(String str) throws RemoteException;

    boolean setBypassEn(String str) throws RemoteException;

    boolean setBypassService(String str) throws RemoteException;

    void setCallback(IEmCallbacks iEmCallbacks) throws RemoteException;

    boolean setCtIREngMode(String str) throws RemoteException;

    boolean setDisableC2kCap(String str) throws RemoteException;

    boolean setDsbpSupport(String str) throws RemoteException;

    boolean setEmConfigure(String str, String str2) throws RemoteException;

    boolean setEmUsbType(String str) throws RemoteException;

    boolean setEmUsbValue(String str) throws RemoteException;

    boolean setImsTestMode(String str) throws RemoteException;

    boolean setMdResetDelay(String str) throws RemoteException;

    boolean setModemWarningEnable(String str) throws RemoteException;

    boolean setMoms(String str) throws RemoteException;

    void setNfcResponseFunction(IEmResponses iEmResponses) throws RemoteException;

    boolean setPreferGprsMode(String str) throws RemoteException;

    boolean setRadioCapabilitySwitchEnable(String str) throws RemoteException;

    boolean setSmsFormat(String str) throws RemoteException;

    boolean setTestSimCardType(String str) throws RemoteException;

    boolean setUsbOtgSwitch(String str) throws RemoteException;

    boolean setUsbPort(String str) throws RemoteException;

    boolean setVolteMalPctid(String str) throws RemoteException;

    boolean setWcnCoreDump(String str) throws RemoteException;

    boolean writeFileConfig(String str, String str2) throws RemoteException;

    boolean writeMnlConfigFile(byte[] bArr, int[] iArr) throws RemoteException;

    boolean writeOtaFiles(String str, byte[] bArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IEmds {
        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public void setCallback(IEmCallbacks callback) throws RemoteException {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean sendToServer(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean sendToServerWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setCtIREngMode(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setDisableC2kCap(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setDsbpSupport(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setPreferGprsMode(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setRadioCapabilitySwitchEnable(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setSmsFormat(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setTestSimCardType(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setImsTestMode(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setVolteMalPctid(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setEmConfigure(String name, String value) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int isGauge30Support() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btDoTest(int kind, int pattern, int channel, int pocketType, int pocketTypeLen, int freq, int power) throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int[] btEndNoSigRxTest() throws RemoteException {
            return null;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public byte[] btHciCommandRun(byte[] input) throws RemoteException {
            return null;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btInit() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean btIsBLEEnhancedSupport() throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btIsBLESupport() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btIsComboSupport() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btIsEmSupport() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btPollingStart() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btPollingStop() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean btStartNoSigRxTest(int pattern, int pockettype, int freq, int address) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btStartRelayer(int port, int speed) throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btStopRelayer() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int btUninit() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setMdResetDelay(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setModemWarningEnable(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setWcnCoreDump(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setUsbPort(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setUsbOtgSwitch(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setEmUsbType(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setEmUsbValue(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setBypassDis(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setBypassEn(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setBypassService(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean setMoms(String data) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean getFilePathListWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public byte[] readMnlConfigFile(int[] tag) throws RemoteException {
            return null;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean writeMnlConfigFile(byte[] content, int[] tag) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int isNfcSupport() throws RemoteException {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public void setNfcResponseFunction(IEmResponses response) throws RemoteException {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean connect() throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public void disconnect() throws RemoteException {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public void sendNfcRequest(int ins, byte[] detail) throws RemoteException {
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean readFileConfigWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean writeFileConfig(String data, String value) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public boolean writeOtaFiles(String destFile, byte[] content) throws RemoteException {
            return false;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IEmds {
        static final int TRANSACTION_btDoTest = 15;
        static final int TRANSACTION_btEndNoSigRxTest = 16;
        static final int TRANSACTION_btHciCommandRun = 17;
        static final int TRANSACTION_btInit = 18;
        static final int TRANSACTION_btIsBLEEnhancedSupport = 19;
        static final int TRANSACTION_btIsBLESupport = 20;
        static final int TRANSACTION_btIsComboSupport = 21;
        static final int TRANSACTION_btIsEmSupport = 22;
        static final int TRANSACTION_btPollingStart = 23;
        static final int TRANSACTION_btPollingStop = 24;
        static final int TRANSACTION_btStartNoSigRxTest = 25;
        static final int TRANSACTION_btStartRelayer = 26;
        static final int TRANSACTION_btStopRelayer = 27;
        static final int TRANSACTION_btUninit = 28;
        static final int TRANSACTION_connect = 45;
        static final int TRANSACTION_disconnect = 46;
        static final int TRANSACTION_getFilePathListWithCallBack = 40;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_isGauge30Support = 14;
        static final int TRANSACTION_isNfcSupport = 43;
        static final int TRANSACTION_readFileConfigWithCallBack = 48;
        static final int TRANSACTION_readMnlConfigFile = 41;
        static final int TRANSACTION_sendNfcRequest = 47;
        static final int TRANSACTION_sendToServer = 2;
        static final int TRANSACTION_sendToServerWithCallBack = 3;
        static final int TRANSACTION_setBypassDis = 36;
        static final int TRANSACTION_setBypassEn = 37;
        static final int TRANSACTION_setBypassService = 38;
        static final int TRANSACTION_setCallback = 1;
        static final int TRANSACTION_setCtIREngMode = 4;
        static final int TRANSACTION_setDisableC2kCap = 5;
        static final int TRANSACTION_setDsbpSupport = 6;
        static final int TRANSACTION_setEmConfigure = 13;
        static final int TRANSACTION_setEmUsbType = 34;
        static final int TRANSACTION_setEmUsbValue = 35;
        static final int TRANSACTION_setImsTestMode = 11;
        static final int TRANSACTION_setMdResetDelay = 29;
        static final int TRANSACTION_setModemWarningEnable = 30;
        static final int TRANSACTION_setMoms = 39;
        static final int TRANSACTION_setNfcResponseFunction = 44;
        static final int TRANSACTION_setPreferGprsMode = 7;
        static final int TRANSACTION_setRadioCapabilitySwitchEnable = 8;
        static final int TRANSACTION_setSmsFormat = 9;
        static final int TRANSACTION_setTestSimCardType = 10;
        static final int TRANSACTION_setUsbOtgSwitch = 33;
        static final int TRANSACTION_setUsbPort = 32;
        static final int TRANSACTION_setVolteMalPctid = 12;
        static final int TRANSACTION_setWcnCoreDump = 31;
        static final int TRANSACTION_writeFileConfig = 49;
        static final int TRANSACTION_writeMnlConfigFile = 42;
        static final int TRANSACTION_writeOtaFiles = 50;

        public Stub() {
            markVintfStability();
            attachInterface(this, IEmds.DESCRIPTOR);
        }

        public static IEmds asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IEmds.DESCRIPTOR);
            if (iin != null && (iin instanceof IEmds)) {
                return (IEmds) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= TRANSACTION_getInterfaceVersion) {
                data.enforceInterface(IEmds.DESCRIPTOR);
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
                    reply.writeString(IEmds.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IEmCallbacks _arg0 = IEmCallbacks.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setCallback(_arg0);
                            reply.writeNoException();
                            return true;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result = sendToServer(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            return true;
                        case 3:
                            String _arg03 = data.readString();
                            IEmCallbacks _arg1 = IEmCallbacks.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result2 = sendToServerWithCallBack(_arg03, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            return true;
                        case 4:
                            String _arg04 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result3 = setCtIREngMode(_arg04);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            return true;
                        case 5:
                            String _arg05 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result4 = setDisableC2kCap(_arg05);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            return true;
                        case 6:
                            String _arg06 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result5 = setDsbpSupport(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            return true;
                        case 7:
                            String _arg07 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result6 = setPreferGprsMode(_arg07);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            return true;
                        case 8:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result7 = setRadioCapabilitySwitchEnable(_arg08);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            return true;
                        case 9:
                            String _arg09 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result8 = setSmsFormat(_arg09);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            return true;
                        case 10:
                            String _arg010 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result9 = setTestSimCardType(_arg010);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            return true;
                        case 11:
                            String _arg011 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result10 = setImsTestMode(_arg011);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            return true;
                        case 12:
                            String _arg012 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result11 = setVolteMalPctid(_arg012);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            return true;
                        case 13:
                            String _arg013 = data.readString();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result12 = setEmConfigure(_arg013, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            return true;
                        case 14:
                            int _result13 = isGauge30Support();
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            return true;
                        case 15:
                            int _arg014 = data.readInt();
                            int _arg13 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            int _arg4 = data.readInt();
                            int _arg5 = data.readInt();
                            int _arg6 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result14 = btDoTest(_arg014, _arg13, _arg2, _arg3, _arg4, _arg5, _arg6);
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            return true;
                        case 16:
                            int[] _result15 = btEndNoSigRxTest();
                            reply.writeNoException();
                            reply.writeIntArray(_result15);
                            return true;
                        case 17:
                            byte[] _arg015 = data.createByteArray();
                            data.enforceNoDataAvail();
                            byte[] _result16 = btHciCommandRun(_arg015);
                            reply.writeNoException();
                            reply.writeByteArray(_result16);
                            return true;
                        case 18:
                            int _result17 = btInit();
                            reply.writeNoException();
                            reply.writeInt(_result17);
                            return true;
                        case 19:
                            boolean _result18 = btIsBLEEnhancedSupport();
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            return true;
                        case 20:
                            int _result19 = btIsBLESupport();
                            reply.writeNoException();
                            reply.writeInt(_result19);
                            return true;
                        case 21:
                            int _result20 = btIsComboSupport();
                            reply.writeNoException();
                            reply.writeInt(_result20);
                            return true;
                        case 22:
                            int _result21 = btIsEmSupport();
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            return true;
                        case 23:
                            int _result22 = btPollingStart();
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            return true;
                        case 24:
                            int _result23 = btPollingStop();
                            reply.writeNoException();
                            reply.writeInt(_result23);
                            return true;
                        case 25:
                            int _arg016 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result24 = btStartNoSigRxTest(_arg016, _arg14, _arg22, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result24);
                            return true;
                        case 26:
                            int _arg017 = data.readInt();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result25 = btStartRelayer(_arg017, _arg15);
                            reply.writeNoException();
                            reply.writeInt(_result25);
                            return true;
                        case 27:
                            int _result26 = btStopRelayer();
                            reply.writeNoException();
                            reply.writeInt(_result26);
                            return true;
                        case 28:
                            int _result27 = btUninit();
                            reply.writeNoException();
                            reply.writeInt(_result27);
                            return true;
                        case 29:
                            String _arg018 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result28 = setMdResetDelay(_arg018);
                            reply.writeNoException();
                            reply.writeBoolean(_result28);
                            return true;
                        case 30:
                            String _arg019 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result29 = setModemWarningEnable(_arg019);
                            reply.writeNoException();
                            reply.writeBoolean(_result29);
                            return true;
                        case 31:
                            String _arg020 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result30 = setWcnCoreDump(_arg020);
                            reply.writeNoException();
                            reply.writeBoolean(_result30);
                            return true;
                        case 32:
                            String _arg021 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result31 = setUsbPort(_arg021);
                            reply.writeNoException();
                            reply.writeBoolean(_result31);
                            return true;
                        case 33:
                            String _arg022 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result32 = setUsbOtgSwitch(_arg022);
                            reply.writeNoException();
                            reply.writeBoolean(_result32);
                            return true;
                        case 34:
                            String _arg023 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result33 = setEmUsbType(_arg023);
                            reply.writeNoException();
                            reply.writeBoolean(_result33);
                            return true;
                        case 35:
                            String _arg024 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result34 = setEmUsbValue(_arg024);
                            reply.writeNoException();
                            reply.writeBoolean(_result34);
                            return true;
                        case 36:
                            String _arg025 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result35 = setBypassDis(_arg025);
                            reply.writeNoException();
                            reply.writeBoolean(_result35);
                            return true;
                        case 37:
                            String _arg026 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result36 = setBypassEn(_arg026);
                            reply.writeNoException();
                            reply.writeBoolean(_result36);
                            return true;
                        case 38:
                            String _arg027 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result37 = setBypassService(_arg027);
                            reply.writeNoException();
                            reply.writeBoolean(_result37);
                            return true;
                        case TRANSACTION_setMoms /* 39 */:
                            String _arg028 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result38 = setMoms(_arg028);
                            reply.writeNoException();
                            reply.writeBoolean(_result38);
                            return true;
                        case 40:
                            String _arg029 = data.readString();
                            IEmCallbacks _arg16 = IEmCallbacks.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result39 = getFilePathListWithCallBack(_arg029, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result39);
                            return true;
                        case TRANSACTION_readMnlConfigFile /* 41 */:
                            int[] _arg030 = data.createIntArray();
                            data.enforceNoDataAvail();
                            byte[] _result40 = readMnlConfigFile(_arg030);
                            reply.writeNoException();
                            reply.writeByteArray(_result40);
                            return true;
                        case 42:
                            byte[] _arg031 = data.createByteArray();
                            int[] _arg17 = data.createIntArray();
                            data.enforceNoDataAvail();
                            boolean _result41 = writeMnlConfigFile(_arg031, _arg17);
                            reply.writeNoException();
                            reply.writeBoolean(_result41);
                            return true;
                        case TRANSACTION_isNfcSupport /* 43 */:
                            int _result42 = isNfcSupport();
                            reply.writeNoException();
                            reply.writeInt(_result42);
                            return true;
                        case TRANSACTION_setNfcResponseFunction /* 44 */:
                            IEmResponses _arg032 = IEmResponses.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setNfcResponseFunction(_arg032);
                            reply.writeNoException();
                            return true;
                        case TRANSACTION_connect /* 45 */:
                            boolean _result43 = connect();
                            reply.writeNoException();
                            reply.writeBoolean(_result43);
                            return true;
                        case TRANSACTION_disconnect /* 46 */:
                            disconnect();
                            reply.writeNoException();
                            return true;
                        case TRANSACTION_sendNfcRequest /* 47 */:
                            int _arg033 = data.readInt();
                            byte[] _arg18 = data.createByteArray();
                            data.enforceNoDataAvail();
                            sendNfcRequest(_arg033, _arg18);
                            return true;
                        case 48:
                            String _arg034 = data.readString();
                            IEmCallbacks _arg19 = IEmCallbacks.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result44 = readFileConfigWithCallBack(_arg034, _arg19);
                            reply.writeNoException();
                            reply.writeBoolean(_result44);
                            return true;
                        case 49:
                            String _arg035 = data.readString();
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result45 = writeFileConfig(_arg035, _arg110);
                            reply.writeNoException();
                            reply.writeBoolean(_result45);
                            return true;
                        case 50:
                            String _arg036 = data.readString();
                            byte[] _arg111 = data.createByteArray();
                            data.enforceNoDataAvail();
                            boolean _result46 = writeOtaFiles(_arg036, _arg111);
                            reply.writeNoException();
                            reply.writeBoolean(_result46);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IEmds {
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
                return IEmds.DESCRIPTOR;
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public void setCallback(IEmCallbacks callback) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setCallback is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean sendToServer(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method sendToServer is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean sendToServerWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method sendToServerWithCallBack is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setCtIREngMode(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setCtIREngMode is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setDisableC2kCap(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setDisableC2kCap is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setDsbpSupport(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setDsbpSupport is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setPreferGprsMode(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setPreferGprsMode is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setRadioCapabilitySwitchEnable(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setRadioCapabilitySwitchEnable is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setSmsFormat(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setSmsFormat is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setTestSimCardType(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setTestSimCardType is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setImsTestMode(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setImsTestMode is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setVolteMalPctid(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setVolteMalPctid is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setEmConfigure(String name, String value) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(value);
                    boolean _status = this.mRemote.transact(13, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setEmConfigure is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int isGauge30Support() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(14, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method isGauge30Support is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btDoTest(int kind, int pattern, int channel, int pocketType, int pocketTypeLen, int freq, int power) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeInt(kind);
                    _data.writeInt(pattern);
                    _data.writeInt(channel);
                    _data.writeInt(pocketType);
                    _data.writeInt(pocketTypeLen);
                    _data.writeInt(freq);
                    _data.writeInt(power);
                    boolean _status = this.mRemote.transact(15, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btDoTest is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int[] btEndNoSigRxTest() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(16, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btEndNoSigRxTest is unimplemented.");
                    }
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public byte[] btHciCommandRun(byte[] input) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeByteArray(input);
                    boolean _status = this.mRemote.transact(17, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btHciCommandRun is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btInit() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(18, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btInit is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean btIsBLEEnhancedSupport() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(19, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btIsBLEEnhancedSupport is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btIsBLESupport() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(20, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btIsBLESupport is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btIsComboSupport() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(21, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btIsComboSupport is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btIsEmSupport() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(22, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btIsEmSupport is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btPollingStart() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(23, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btPollingStart is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btPollingStop() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(24, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btPollingStop is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean btStartNoSigRxTest(int pattern, int pockettype, int freq, int address) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeInt(pattern);
                    _data.writeInt(pockettype);
                    _data.writeInt(freq);
                    _data.writeInt(address);
                    boolean _status = this.mRemote.transact(25, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btStartNoSigRxTest is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btStartRelayer(int port, int speed) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeInt(port);
                    _data.writeInt(speed);
                    boolean _status = this.mRemote.transact(26, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btStartRelayer is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btStopRelayer() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(27, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btStopRelayer is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int btUninit() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(28, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method btUninit is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setMdResetDelay(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(29, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setMdResetDelay is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setModemWarningEnable(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(30, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setModemWarningEnable is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setWcnCoreDump(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(31, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setWcnCoreDump is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setUsbPort(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(32, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setUsbPort is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setUsbOtgSwitch(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(33, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setUsbOtgSwitch is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setEmUsbType(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(34, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setEmUsbType is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setEmUsbValue(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(35, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setEmUsbValue is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setBypassDis(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(36, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setBypassDis is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setBypassEn(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(37, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setBypassEn is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setBypassService(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(38, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setBypassService is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean setMoms(String data) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_setMoms, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setMoms is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean getFilePathListWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(40, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getFilePathListWithCallBack is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public byte[] readMnlConfigFile(int[] tag) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeIntArray(tag);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_readMnlConfigFile, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method readMnlConfigFile is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean writeMnlConfigFile(byte[] content, int[] tag) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeByteArray(content);
                    _data.writeIntArray(tag);
                    boolean _status = this.mRemote.transact(42, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method writeMnlConfigFile is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int isNfcSupport() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_isNfcSupport, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method isNfcSupport is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public void setNfcResponseFunction(IEmResponses response) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeStrongInterface(response);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_setNfcResponseFunction, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setNfcResponseFunction is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean connect() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_connect, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method connect is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public void disconnect() throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_disconnect, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method disconnect is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public void sendNfcRequest(int ins, byte[] detail) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeInt(ins);
                    _data.writeByteArray(detail);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_sendNfcRequest, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method sendNfcRequest is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean readFileConfigWithCallBack(String data, IEmCallbacks callback) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(48, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method readFileConfigWithCallBack is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean writeFileConfig(String data, String value) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(data);
                    _data.writeString(value);
                    boolean _status = this.mRemote.transact(49, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method writeFileConfig is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public boolean writeOtaFiles(String destFile, byte[] content) throws RemoteException {
                Parcel _data = Parcel.obtain(asBinder());
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    _data.writeString(destFile);
                    _data.writeByteArray(content);
                    boolean _status = this.mRemote.transact(50, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method writeOtaFiles is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(IEmds.DESCRIPTOR);
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

            @Override // com.android.server.location.hardware.mtk.engineermode.aidl.IEmds
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain(asBinder());
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(IEmds.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
