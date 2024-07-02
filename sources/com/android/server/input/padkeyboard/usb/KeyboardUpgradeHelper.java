package com.android.server.input.padkeyboard.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Slog;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class KeyboardUpgradeHelper extends UpgradeHelper {
    public static String FIRST_LOW_KB_TYPE;
    public static String KB_BIN_PATH;
    public static String KB_H_BIN_PATH;
    public static Map<String, String> KB_L_BIN_PATH_MAP;
    public static String WL_BIN_PATH;
    protected byte[] mCheckSumByte;
    protected byte[] mLengthByte;
    protected byte[] mStartAddressByte;
    protected byte[] mVersionByte;

    static {
        HashMap hashMap = new HashMap();
        KB_L_BIN_PATH_MAP = hashMap;
        FIRST_LOW_KB_TYPE = "a";
        KB_BIN_PATH = "/vendor/etc/XM_KB.bin";
        KB_H_BIN_PATH = "/vendor/etc/XM_KB_H.bin";
        WL_BIN_PATH = "/vendor/etc/XM_KB_WL.bin";
        hashMap.put("20", "/vendor/etc/XM_KB_L.bin");
        KB_L_BIN_PATH_MAP.put("4a", "/vendor/etc/XM_KB_L_A.bin");
        KB_L_BIN_PATH_MAP.put("4b", "/vendor/etc/XM_KB_L_B.bin");
        KB_L_BIN_PATH_MAP.put("4c", "/vendor/etc/XM_KB_L_C.bin");
    }

    public KeyboardUpgradeHelper(Context context, String binPath) {
        super(context, binPath);
        this.mLengthByte = new byte[4];
        this.mStartAddressByte = new byte[4];
        this.mCheckSumByte = new byte[1];
        this.mVersionByte = new byte[2];
        parseUpgradeFileHead();
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    protected void parseUpgradeFileHead() {
        if (this.mFileBuf != null && this.mFileBuf.length > 64) {
            byte[] bArr = this.mFileBuf;
            byte[] bArr2 = this.mLengthByte;
            System.arraycopy(bArr, 0, bArr2, 0, bArr2.length);
            byte[] bArr3 = this.mLengthByte;
            this.mBinLength = ((bArr3[1] << 8) & 65280) + (bArr3[0] & 255);
            int headAddress = 0 + this.mLengthByte.length;
            byte[] bArr4 = this.mFileBuf;
            byte[] bArr5 = this.mStartAddressByte;
            System.arraycopy(bArr4, headAddress, bArr5, 0, bArr5.length);
            int headAddress2 = headAddress + this.mStartAddressByte.length;
            byte[] bArr6 = this.mFileBuf;
            byte[] bArr7 = this.mCheckSumByte;
            System.arraycopy(bArr6, headAddress2, bArr7, 0, bArr7.length);
            byte[] bArr8 = this.mCheckSumByte;
            if ((bArr8[0] & CommunicationUtil.PAD_ADDRESS) != 0) {
                StringBuilder append = new StringBuilder().append("FFFFFF");
                byte[] bArr9 = this.mCheckSumByte;
                this.mCheckSum = new BigInteger(append.append(MiuiKeyboardUtil.Bytes2HexString(bArr9, bArr9.length, "")).toString(), 16).intValue();
            } else {
                this.mCheckSum = Integer.parseInt(MiuiKeyboardUtil.Bytes2HexString(bArr8, bArr8.length, ""), 16);
            }
            int headAddress3 = headAddress2 + this.mCheckSumByte.length;
            byte[] bArr10 = this.mFileBuf;
            byte[] bArr11 = this.mVersionByte;
            System.arraycopy(bArr10, headAddress3, bArr11, 0, bArr11.length);
            this.mVersion = MiuiKeyboardUtil.Bytes2RevertHexString(this.mVersionByte);
            Slog.i(MiuiPadKeyboardManager.TAG, "Bin Version : " + this.mVersion);
            int sum2 = MiuiKeyboardUtil.getSum(this.mFileBuf, 16, this.mFileBuf.length - 16);
            int sum = (~sum2) + 1;
            String hexSum = Integer.toHexString(sum);
            byte[] bArr12 = this.mCheckSumByte;
            if (!hexSum.contains(MiuiKeyboardUtil.Bytes2HexString(bArr12, bArr12.length, ""))) {
                this.mFileBuf = null;
                Slog.i(MiuiPadKeyboardManager.TAG, "check sum error");
            } else {
                byte[] realData = new byte[this.mFileBuf.length - 16];
                System.arraycopy(this.mFileBuf, 16, realData, 0, realData.length);
                this.mFileBuf = realData;
            }
        }
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public byte[] getUpgradeCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 2;
        bytes[5] = 11;
        System.arraycopy(this.mLengthByte, 0, bytes, 6, 4);
        System.arraycopy(this.mStartAddressByte, 0, bytes, 10, 4);
        System.arraycopy(this.mCheckSumByte, 0, bytes, 14, 1);
        System.arraycopy(this.mVersionByte, 0, bytes, 15, 2);
        bytes[17] = MiuiKeyboardUtil.getSum(bytes, 0, 17);
        return bytes;
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public byte[] getBinPackInfo(int offset, int packetLength) {
        byte[] bytes = new byte[32];
        if (packetLength == 32) {
            bytes = new byte[32];
            int binlength = 20;
            bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
            bytes[1] = 48;
            bytes[2] = CommunicationUtil.PAD_ADDRESS;
            bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
            bytes[4] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
            bytes[5] = CommunicationUtil.SEND_RESTORE_COMMAND;
            byte[] offsetB = MiuiKeyboardUtil.int2Bytes(offset + 4096);
            bytes[6] = offsetB[3];
            bytes[7] = offsetB[2];
            bytes[8] = offsetB[1];
            if (this.mFileBuf.length < offset + 20) {
                int binlength2 = this.mFileBuf.length - offset;
                bytes[9] = MiuiKeyboardUtil.int2Bytes(binlength2)[3];
                binlength = binlength2;
            } else {
                bytes[9] = 20;
            }
            System.arraycopy(this.mFileBuf, offset, bytes, 10, binlength);
            bytes[31] = MiuiKeyboardUtil.getSum(bytes, 0, 31);
        } else if (packetLength == 64) {
            bytes = new byte[64];
            bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
            bytes[1] = 48;
            bytes[2] = CommunicationUtil.PAD_ADDRESS;
            bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
            bytes[4] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
            bytes[5] = CommunicationUtil.KEYBOARD_ADDRESS;
            byte[] offsetB2 = MiuiKeyboardUtil.int2Bytes(offset + 4096);
            bytes[6] = offsetB2[3];
            bytes[7] = offsetB2[2];
            bytes[8] = offsetB2[1];
            if (this.mFileBuf.length < offset + 52) {
                int binlength3 = this.mFileBuf.length - offset;
                bytes[5] = (byte) (binlength3 + 4);
                bytes[9] = (byte) binlength3;
                System.arraycopy(this.mFileBuf, offset, bytes, 10, binlength3);
                bytes[binlength3 + 10] = MiuiKeyboardUtil.getSum(bytes, 0, binlength3 + 10);
            } else {
                bytes[9] = CommunicationUtil.COMMAND_AUTH_52;
                System.arraycopy(this.mFileBuf, offset, bytes, 10, 52);
                bytes[62] = MiuiKeyboardUtil.getSum(bytes, 0, 62);
            }
        }
        return bytes;
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public byte[] getUpEndCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 4;
        bytes[5] = 11;
        System.arraycopy(this.mLengthByte, 0, bytes, 6, 4);
        System.arraycopy(this.mStartAddressByte, 0, bytes, 10, 4);
        System.arraycopy(this.mCheckSumByte, 0, bytes, 14, 1);
        System.arraycopy(this.mVersionByte, 0, bytes, 15, 2);
        bytes[17] = MiuiKeyboardUtil.getSum(bytes, 0, 17);
        return bytes;
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public byte[] getResetCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 6;
        bytes[5] = 4;
        System.arraycopy(this.mStartAddressByte, 0, bytes, 6, 4);
        bytes[10] = MiuiKeyboardUtil.getSum(bytes, 0, 10);
        return bytes;
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public int getBinPacketNum(int length) {
        if (length == 32) {
            int num = (int) Math.ceil(this.mBinLength / 20.0d);
            return num;
        }
        if (length != 64) {
            return 0;
        }
        int num2 = (int) Math.ceil(this.mBinLength / 52.0d);
        return num2;
    }

    @Override // com.android.server.input.padkeyboard.usb.UpgradeHelper
    public boolean startUpgrade(UsbDevice usbDevice, UsbDeviceConnection usbConnection, UsbEndpoint outUsbEndpoint, UsbEndpoint inUsbEndpoint) {
        Slog.i(MiuiPadKeyboardManager.TAG, "start keyboard upgrade");
        if (usbDevice == null || usbConnection == null || outUsbEndpoint == null || inUsbEndpoint == null) {
            Slog.i(MiuiPadKeyboardManager.TAG, "start upgrade failed : device not ready");
            return false;
        }
        this.mUsbDevice = usbDevice;
        this.mUsbConnection = usbConnection;
        this.mOutUsbEndpoint = outUsbEndpoint;
        this.mInUsbEndpoint = inUsbEndpoint;
        if (this.mFileBuf == null || this.mFileBuf.length < 64) {
            Slog.i(MiuiPadKeyboardManager.TAG, "start upgrade failed : invalid bin file");
            return false;
        }
        return sendUpgradeCommand();
    }

    private boolean sendUpgradeCommand() {
        Slog.i(MiuiPadKeyboardManager.TAG, "send upgrade command");
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getUpgradeCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUpgradeCommand()) {
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send upgrade command failed");
            }
            MiuiKeyboardUtil.operationWait(500);
            if (this.mUsbDevice == null) {
                return false;
            }
        }
        return false;
    }

    private boolean parseUpgradeCommand() {
        if (this.mRecBuf[6] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command state error:" + ((int) this.mRecBuf[6]));
            return false;
        }
        if (this.mRecBuf[7] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command mode error:" + ((int) this.mRecBuf[6]));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(this.mRecBuf, 0, 8, this.mRecBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command checkSum error:" + ((int) this.mRecBuf[8]));
            return false;
        }
        return sendBinData();
    }

    private boolean sendBinData() {
        int packetNum = getBinPacketNum(64);
        Slog.i(MiuiPadKeyboardManager.TAG, "send bin data, packet num: " + packetNum);
        for (int num = 0; num < packetNum; num++) {
            if (!sendOnePacket(num)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "send bin data " + num + " failed");
                return false;
            }
        }
        return sendUpEndCommand();
    }

    private boolean sendOnePacket(int num) {
        Arrays.fill(this.mSendBuf, (byte) 0);
        System.arraycopy(getBinPackInfo(num * 52, 64), 0, this.mSendBuf, 0, 64);
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (this.mRecBuf[4] == -111 || this.mRecBuf[4] == 17) {
                        if (this.mRecBuf[6] == 0) {
                            return true;
                        }
                    }
                }
            }
            MiuiKeyboardUtil.operationWait(200);
        }
        return false;
    }

    private boolean sendUpEndCommand() {
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getUpEndCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUpEndCommand()) {
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send up end command failed");
            }
            if (this.mUsbDevice == null) {
                return false;
            }
            MiuiKeyboardUtil.operationWait(500);
        }
        return false;
    }

    private boolean parseUpEndCommand() {
        if (this.mRecBuf[6] == 1) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up end command state need wait");
            return false;
        }
        if (this.mRecBuf[6] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up end command state error:" + ((int) this.mRecBuf[6]));
            return false;
        }
        return sendResetCommand();
    }

    private boolean sendResetCommand() {
        Slog.i(MiuiPadKeyboardManager.TAG, "send reset command");
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getResetCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        boolean resetSent = false;
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                resetSent = true;
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (this.mRecBuf[6] != 0) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command state error:" + ((int) this.mRecBuf[6]));
                    } else if (!MiuiKeyboardUtil.checkSum(this.mRecBuf, 0, 7, this.mRecBuf[7])) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command checkSum error:" + ((int) this.mRecBuf[7]));
                    } else {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command");
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send reset command failed");
            }
            if (this.mUsbDevice == null) {
                return true;
            }
            MiuiKeyboardUtil.operationWait(1000);
        }
        return resetSent;
    }
}
