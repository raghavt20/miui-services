package com.android.server.input.padkeyboard.iic;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class CommunicationUtil {
    public static final int BUFFER_SIZE = 1024;
    public static final byte COMMAND_AUTH_3 = 50;
    public static final byte COMMAND_AUTH_51 = 51;
    public static final byte COMMAND_AUTH_52 = 52;
    public static final byte COMMAND_AUTH_7 = 53;
    public static final byte COMMAND_AUTH_START = 49;
    public static final byte COMMAND_CHECK_MCU_STATUS = -95;
    public static final byte COMMAND_CUSTOM_DATA_KB = 105;
    public static final byte COMMAND_DATA_PKG = -111;
    public static final byte COMMAND_GET_VERSION = 1;
    public static final byte COMMAND_G_SENSOR = 100;
    public static final byte COMMAND_KB_BACKLIGHT = 35;
    public static final byte COMMAND_KB_BATTERY_LEVEL = 41;
    public static final byte COMMAND_KB_FEATURE_CAPS_LOCK = 38;
    public static final byte COMMAND_KB_FEATURE_POWER = 37;
    public static final byte COMMAND_KB_FEATURE_RECOVER = 32;
    public static final byte COMMAND_KB_FEATURE_SLEEP = 40;
    public static final byte COMMAND_KB_NFC = 54;
    public static final byte COMMAND_KB_RE_AUTH = 36;
    public static final byte COMMAND_KB_STATUS = 34;
    public static final byte COMMAND_KEYBOARD_RESPONSE_STATUS = 48;
    public static final byte COMMAND_KEYBOARD_STATUS = -16;
    public static final byte COMMAND_KEYBOARD_UPGRADE_STATUS = 117;
    public static final byte COMMAND_MCU_BOOT = 18;
    public static final byte COMMAND_MCU_RESET = 3;
    public static final byte COMMAND_READ_KB_STATUS = 82;
    public static final byte COMMAND_RESPONSE_MCU_STATUS = -94;
    public static final byte COMMAND_SUCCESS_RUN = 0;
    public static final byte COMMAND_UPGRADE = 2;
    public static final byte COMMAND_UPGRADE_BUSY = 7;
    public static final byte COMMAND_UPGRADE_FINISHED = 4;
    public static final byte COMMAND_UPGRADE_FLASH = 6;
    public static final byte COMMAN_KEYBOARD_EXTERNAL_FLAG = 105;
    public static final byte COMMAN_PAD_BLUETOOTH = 82;
    public static final byte COMMAN_PAD_EXTERNAL_FLAG = 104;
    public static final byte FEATURE_DISABLE = 0;
    public static final byte FEATURE_ENABLE = 1;
    public static final byte KEYBOARD_ADDRESS = 56;
    public static final byte KEYBOARD_COLOR_BLACK = 65;
    public static final byte KEYBOARD_COLOR_WHITE = 66;
    public static final byte KEYBOARD_FLASH_ADDRESS = 57;
    public static final byte L81A_VERSION_LENGTH = 5;
    public static final byte MCU_ADDRESS = 24;
    public static final byte OTA_FAIL_NOT_MATCH = 7;
    public static final byte PAD_ADDRESS = Byte.MIN_VALUE;
    public static final byte REPLENISH_PROTOCOL_COMMAND = 49;
    public static final byte RESPONSE_BLE_DEVICE_ID = 49;
    public static final byte RESPONSE_TYPE = 87;
    public static final byte RESPONSE_VENDOR_ONE_LONG_REPORT_ID = 36;
    public static final byte RESPONSE_VENDOR_ONE_SHORT_REPORT_ID = 35;
    public static final byte RESPONSE_VENDOR_TWO_REPORT_ID = 38;
    public static final byte RESPONSE_VENDOR_TWO_SHORT_REPORT_ID = 34;
    public static final int SEND_COMMAND_BYTE_LONG = 68;
    public static final int SEND_COMMAND_BYTE_SHORT = 34;
    public static final byte SEND_EMPTY_DATA = 0;
    public static final byte SEND_REPORT_ID_LONG_DATA = 79;
    public static final byte SEND_REPORT_ID_SHORT_DATA = 78;
    public static final byte SEND_RESTORE_COMMAND = 29;
    public static final byte SEND_SERIAL_NUMBER = 0;
    public static final byte SEND_TYPE = 50;
    public static final byte SEND_UPGRADE_PACKAGE_COMMAND = 17;
    public static final String TAG = "IIC_CommunicationUtil";
    public static final byte TOUCHPAD_ADDRESS = 64;
    public static final byte UPGRADE_PROTOCOL_COMMAND = 48;
    private static volatile CommunicationUtil sCommunicationUtil;
    private KeyboardNanoAppManager mKeyboardNanoAppManager;
    private final Object mLock = new Object();
    private int mNFCDataPkgSize;
    private NanoSocketCallback mNanoSocketCallback;
    private ReadSocketHandler mReadSocketHandler;
    private SocketCallBack mSocketCallBack;

    /* loaded from: classes.dex */
    public interface SocketCallBack {
        void responseFromKeyboard(byte[] bArr);

        void responseFromMCU(byte[] bArr);
    }

    /* loaded from: classes.dex */
    public enum KB_FEATURE {
        KB_ENABLE((byte) 34, 1),
        KB_POWER(CommunicationUtil.COMMAND_KB_FEATURE_POWER, 2),
        KB_CAPS_KEY((byte) 38, 3),
        KB_WRITE_CMD_ACK((byte) 48, 4),
        KB_FIRMWARE_RECOVER((byte) 32, 5),
        KB_SLEEP_STATUS(CommunicationUtil.COMMAND_KB_FEATURE_SLEEP, 6),
        KB_G_SENSOR((byte) 39, 7),
        KB_BACKLIGHT((byte) 35, 8),
        KB_MIC_MUTE((byte) 38, 9),
        KB_CAPS_KEY_NEW((byte) 46, 10),
        KB_MIC_MUTE_NEW((byte) 46, 11);

        private final byte mCommand;
        private final int mIndex;

        KB_FEATURE(byte commandId, int index) {
            this.mCommand = commandId;
            this.mIndex = index;
        }

        public byte getCommand() {
            return this.mCommand;
        }

        public int getIndex() {
            return this.mIndex;
        }
    }

    /* loaded from: classes.dex */
    public enum AUTH_COMMAND {
        AUTH_START((byte) 49, (byte) 6),
        AUTH_STEP3((byte) 50, (byte) 36),
        AUTH_STEP5_1(CommunicationUtil.COMMAND_AUTH_51, MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH),
        AUTH_STEP5_2(CommunicationUtil.COMMAND_AUTH_52, CommunicationUtil.COMMAND_AUTH_52),
        AUTH_STEP7(CommunicationUtil.COMMAND_AUTH_7, (byte) 2);

        private final byte mCommand;
        private final byte mSize;

        AUTH_COMMAND(byte command, byte size) {
            this.mCommand = command;
            this.mSize = size;
        }

        public byte getCommand() {
            return this.mCommand;
        }

        public byte getSize() {
            return this.mSize;
        }
    }

    public static CommunicationUtil getInstance() {
        if (sCommunicationUtil == null) {
            synchronized (CommunicationUtil.class) {
                if (sCommunicationUtil == null) {
                    sCommunicationUtil = new CommunicationUtil();
                }
            }
        }
        return sCommunicationUtil;
    }

    private CommunicationUtil() {
        initSocketClient();
    }

    private void initSocketClient() {
        if (this.mReadSocketHandler == null) {
            HandlerThread readThread = new HandlerThread("IIC_Read_Socket");
            readThread.start();
            this.mReadSocketHandler = new ReadSocketHandler(readThread.getLooper());
        }
        KeyboardNanoAppManager keyboardNanoAppManager = KeyboardNanoAppManager.getInstance();
        this.mKeyboardNanoAppManager = keyboardNanoAppManager;
        keyboardNanoAppManager.initCallback();
        this.mKeyboardNanoAppManager.setCommunicationUtil(this);
    }

    public void receiverNanoAppData(byte[] responseData) {
        Slog.i(TAG, "get keyboard data:" + MiuiKeyboardUtil.Bytes2Hex(responseData, responseData.length));
        if (this.mReadSocketHandler == null) {
            HandlerThread readThread = new HandlerThread("IIC_Read_Socket");
            readThread.start();
            this.mReadSocketHandler = new ReadSocketHandler(readThread.getLooper());
        }
        Message msg = this.mReadSocketHandler.obtainMessage(1, responseData);
        this.mReadSocketHandler.sendMessage(msg);
    }

    public void registerSocketCallback(SocketCallBack socketCallBack) {
        this.mSocketCallBack = socketCallBack;
    }

    public void setOtaCallBack(NanoSocketCallback callBack) {
        this.mNanoSocketCallback = callBack;
    }

    public byte[] getVersionCommand(byte targetAddress) {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = SEND_REPORT_ID_SHORT_DATA;
        temp[5] = 48;
        temp[6] = PAD_ADDRESS;
        temp[7] = targetAddress;
        temp[8] = 1;
        temp[9] = 1;
        temp[10] = 0;
        temp[11] = getSum(temp, 4, 7);
        return temp;
    }

    public void sendRestoreMcuCommand() {
        byte[] temp = new byte[68];
        temp[0] = 50;
        temp[1] = 0;
        temp[2] = SEND_REPORT_ID_LONG_DATA;
        temp[3] = 48;
        temp[4] = PAD_ADDRESS;
        temp[5] = MCU_ADDRESS;
        temp[6] = SEND_RESTORE_COMMAND;
        temp[7] = 13;
        temp[8] = -116;
        temp[9] = 63;
        temp[10] = 101;
        temp[11] = -127;
        temp[12] = -110;
        temp[13] = -50;
        temp[14] = 0;
        temp[15] = -108;
        temp[16] = -88;
        temp[17] = -126;
        temp[18] = 45;
        temp[19] = 91;
        temp[20] = 126;
        temp[21] = getSum(temp, 2, 19);
        writeSocketCmd(temp);
    }

    public boolean writeSocketCmd(byte[] buf) {
        Byte[] temp = new Byte[buf.length];
        ArrayList<Byte> result = new ArrayList<>();
        for (int i = 0; i < buf.length; i++) {
            temp[i] = Byte.valueOf(buf[i]);
        }
        Collections.addAll(result, temp);
        Slog.i(TAG, "write to IIC:" + MiuiKeyboardUtil.Bytes2Hex(buf, buf.length));
        this.mKeyboardNanoAppManager.sendCommandToNano(result);
        return false;
    }

    public byte[] getLongRawCommand() {
        return new byte[68];
    }

    public void setCommandHead(byte[] bytes) {
        bytes[0] = -86;
        bytes[1] = KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
    }

    public void setSetKeyboardStatusCommand(byte[] bytes, byte commandId, byte data) {
        bytes[4] = SEND_REPORT_ID_SHORT_DATA;
        bytes[5] = 49;
        bytes[6] = PAD_ADDRESS;
        bytes[7] = KEYBOARD_ADDRESS;
        bytes[8] = commandId;
        bytes[9] = 1;
        bytes[10] = data;
        bytes[11] = getSum(bytes, 4, 7);
    }

    public void setReadKeyboardCommand(byte[] bytes, byte reportId, byte version, byte sourceAdd, byte targetAdd, byte feature) {
        bytes[4] = reportId;
        bytes[5] = version;
        bytes[6] = sourceAdd;
        bytes[7] = targetAdd;
        bytes[8] = feature;
        bytes[9] = 0;
        bytes[10] = getSum(bytes, 4, 7);
    }

    public void setLocalAddress2KeyboardCommand(byte[] bytes, byte reportId, byte version, byte sourceAdd, byte targetAdd, byte feature) {
        String localAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        Slog.i(TAG, "get pad Address:" + localAddress);
        bytes[4] = reportId;
        bytes[5] = version;
        bytes[6] = sourceAdd;
        bytes[7] = targetAdd;
        bytes[8] = feature;
        bytes[9] = 6;
        int i = 10;
        int i2 = 0;
        if (!TextUtils.isEmpty(localAddress)) {
            String[] split = localAddress.split(":");
            int length = split.length;
            while (i2 < length) {
                String retval = split[i2];
                bytes[i] = Integer.valueOf(retval, 16).byteValue();
                i2++;
                i++;
            }
        } else {
            for (int i3 = 10; i3 < 16; i3 = i3 + 1 + 1) {
                bytes[i3] = 0;
            }
        }
        bytes[16] = getSum(bytes, 4, 13);
    }

    public void setCheckKeyboardResponseCommand(byte[] bytes, byte reportId, byte version, byte sourceAdd, byte targetAdd, byte lastCommandId) {
        bytes[4] = reportId;
        bytes[5] = version;
        bytes[6] = sourceAdd;
        bytes[7] = targetAdd;
        bytes[8] = 48;
        bytes[9] = 1;
        bytes[10] = lastCommandId;
        bytes[11] = getSum(bytes, 4, 7);
    }

    public void setCheckMCUStatusCommand(byte[] data, byte command) {
        setCommandHead(data);
        data[4] = SEND_REPORT_ID_SHORT_DATA;
        data[5] = 49;
        data[6] = PAD_ADDRESS;
        data[7] = KEYBOARD_ADDRESS;
        data[8] = command;
        data[9] = 1;
        data[10] = 1;
        data[11] = getSum(data, 4, 7);
    }

    public void setGetHallStatusCommand(byte[] data) {
        setCommandHead(data);
        data[4] = SEND_REPORT_ID_LONG_DATA;
        data[5] = 32;
        data[6] = PAD_ADDRESS;
        data[7] = PAD_ADDRESS;
        data[8] = -31;
        data[9] = 1;
        data[10] = 0;
        data[11] = getSum(data, 4, 7);
    }

    public static synchronized byte getSum(byte[] command, int start, int length) {
        byte sum;
        synchronized (CommunicationUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum = (byte) ((command[i] & 255) + sum);
            }
        }
        return sum;
    }

    public static synchronized int getSumInt(byte[] data, int start, int length) {
        int sum;
        synchronized (CommunicationUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum += data[i] & 255;
            }
        }
        return sum;
    }

    public void sendNFC(byte[] data_byte) {
        List<byte[]> nfcData = getNfcData(data_byte);
        this.mNFCDataPkgSize = nfcData.size();
        for (int i = 0; i < this.mNFCDataPkgSize; i++) {
            byte[] temp = new byte[68];
            temp[0] = -86;
            temp[1] = KEYBOARD_COLOR_WHITE;
            temp[2] = 50;
            temp[3] = 0;
            temp[4] = SEND_REPORT_ID_LONG_DATA;
            temp[5] = 49;
            temp[6] = PAD_ADDRESS;
            temp[7] = KEYBOARD_ADDRESS;
            temp[8] = COMMAND_KB_NFC;
            temp[9] = (byte) (nfcData.get(i).length + 2);
            temp[10] = (byte) nfcData.size();
            temp[11] = (byte) (i + 1);
            System.arraycopy(nfcData.get(i), 0, temp, 12, nfcData.get(i).length);
            temp[nfcData.get(i).length + 12] = getSum(temp, 4, nfcData.get(i).length + 12);
            writeSocketCmd(temp);
        }
    }

    private List<byte[]> getNfcData(byte[] nfc) {
        List<byte[]> nfc_data = new ArrayList<>();
        byte[] all_nfc = new byte[nfc.length + 8];
        all_nfc[0] = SEND_REPORT_ID_SHORT_DATA;
        all_nfc[1] = 70;
        all_nfc[2] = 67;
        all_nfc[3] = (byte) ((nfc.length + 1) & 255);
        all_nfc[4] = (byte) (((nfc.length + 1) >> 8) & 4094);
        all_nfc[5] = 0;
        all_nfc[6] = 0;
        all_nfc[7] = 84;
        System.arraycopy(nfc, 0, all_nfc, 8, nfc.length);
        int sumInt = getSumInt(all_nfc, 7, nfc.length + 1);
        all_nfc[5] = (byte) (sumInt & 255);
        all_nfc[6] = (byte) ((sumInt >> 8) & 255);
        if (all_nfc.length > 52) {
            int pac_numbers = (all_nfc.length + 51) / 52;
            for (int i = 0; i < pac_numbers; i++) {
                int length = i + 1 == pac_numbers ? all_nfc.length - (i * 52) : 52;
                byte[] temp = new byte[length];
                System.arraycopy(all_nfc, i * 52, temp, 0, temp.length);
                nfc_data.add(temp);
            }
        } else {
            nfc_data.add(all_nfc);
        }
        return nfc_data;
    }

    public int getNFCDataPkgSize() {
        return this.mNFCDataPkgSize;
    }

    public boolean checkSum(byte[] data, int start, int length, byte sum) {
        byte dataSum = getSum(data, start, length);
        byte[] bytes = {dataSum};
        if (bytes[0] == sum) {
            return true;
        }
        return false;
    }

    public byte getLedStatusValue(int type, boolean enable, byte keyType) {
        if (keyType == 33 || !MiuiKeyboardUtil.isXM2022MCU()) {
            if (type == KB_FEATURE.KB_CAPS_KEY.getIndex() || type == KB_FEATURE.KB_CAPS_KEY_NEW.getIndex()) {
                return enable ? (byte) -3 : (byte) -4;
            }
            if (type == KB_FEATURE.KB_MIC_MUTE.getIndex() || type == KB_FEATURE.KB_MIC_MUTE_NEW.getIndex()) {
                return enable ? (byte) -9 : (byte) -13;
            }
            return (byte) 0;
        }
        return (byte) 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ReadSocketHandler extends Handler {
        public static final int MSG_READ = 1;

        ReadSocketHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                byte[] bArr = (byte[]) msg.obj;
                int position = 0;
                if (bArr == 0) {
                    return;
                }
                while (position < bArr.length) {
                    if (bArr[position] == -86) {
                        byte[] temp = new byte[bArr[position + 1]];
                        if (position + 2 + temp.length <= bArr.length) {
                            System.arraycopy(bArr, position + 2, temp, 0, temp.length);
                            dealReadSocketPackage(temp);
                            position += temp.length + 2;
                        } else {
                            Slog.e(CommunicationUtil.TAG, "Drop not complete Data!");
                            return;
                        }
                    } else if (bArr[0] == 35 && (bArr[1] == 49 || bArr[1] == 48)) {
                        dealReadSocketPackage(bArr);
                        return;
                    } else {
                        Slog.e(CommunicationUtil.TAG, "Receiver Data is too old!");
                        return;
                    }
                }
            }
        }

        private void dealReadSocketPackage(byte[] data) {
            if (CommunicationUtil.this.mNanoSocketCallback == null || CommunicationUtil.this.mSocketCallBack == null) {
                Slog.e(CommunicationUtil.TAG, "Miui Keyboard Manager is not ready,Abandon this socket package.");
                return;
            }
            if (data[0] == Byte.MIN_VALUE) {
                CommunicationUtil.this.mNanoSocketCallback.onWriteSocketErrorInfo(NanoSocketCallback.OTA_ERROR_REASON_WRITE_SOCKET_EXCEPTION);
                Slog.d(CommunicationUtil.TAG, "Exception socket command is:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
            }
            if ((data[0] == 36 || data[0] == 35 || data[0] == 38 || data[0] == 34) && data[3] == Byte.MIN_VALUE) {
                if (data[1] == 48) {
                    if (data[2] == 24) {
                        CommunicationUtil.this.mSocketCallBack.responseFromMCU(data);
                        return;
                    } else {
                        if (data[2] == 56 || data[2] == 57) {
                            CommunicationUtil.this.mSocketCallBack.responseFromKeyboard(data);
                            return;
                        }
                        return;
                    }
                }
                if (data[1] == 49 && data[2] == 56) {
                    CommunicationUtil.this.mSocketCallBack.responseFromKeyboard(data);
                } else if (data[1] == 32 && data[2] == Byte.MIN_VALUE && data[4] == -31 && data[5] == 1) {
                    CommunicationUtil.this.mNanoSocketCallback.onHallStatusChanged(data[6]);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class BleDeviceUtil {
        public static final byte[] CMD_KB_STATUS = {48, 1, 1, 0, 50};

        public static byte[] getKeyboardFeatureCommand(byte command, byte data) {
            byte[] result = {49, command, 1, data, CommunicationUtil.getSum(result, 0, 4)};
            return result;
        }
    }
}
