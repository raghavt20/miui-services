package com.android.server.input.padkeyboard.iic;

import android.content.Context;
import android.util.Slog;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class TouchPadUpgradeUtil {
    private static final int START_INDEX_FOR_176 = 0;
    private static final int START_INDEX_FOR_179 = 4032;
    private static final String TAG = "TouchPadKeyboardUpgradeUtil";
    public int mBinCheckSum;
    public int mBinLength;
    private String mBinStartFlag;
    public String mBinVersionStr;
    private byte[] mFileBuf;
    private boolean mIsBleDevice;
    public String mPath;
    public boolean mValid;
    public byte[] mBinLengthByte = new byte[4];
    public byte[] mBinCheckSumByte = new byte[4];
    public byte[] mBinVersion = new byte[2];
    public byte[] mBinStartAddressByte = new byte[4];

    public TouchPadUpgradeUtil(Context context, String path, byte type) {
        if (path == null) {
            Slog.i(TAG, "UpgradeOta file Path is null");
            return;
        }
        this.mPath = path;
        this.mIsBleDevice = type == 2;
        Slog.i(TAG, "TouchPad UpgradeOta file Path:" + this.mPath);
        byte[] ReadUpgradeFile = ReadUpgradeFile(this.mPath);
        this.mFileBuf = ReadUpgradeFile;
        if (ReadUpgradeFile == null) {
            Slog.i(TAG, "UpgradeOta file buff is null or length low than 6000");
        } else {
            this.mValid = parseOtaFile();
        }
    }

    private byte[] ReadUpgradeFile(String filePath) {
        File mFile = new File(filePath);
        if (mFile.exists()) {
            byte[] fileBuf = readFileToBuf(filePath);
            return fileBuf;
        }
        Slog.i(TAG, "=== The Upgrade bin file does not exist.");
        return null;
    }

    public boolean isValidFile() {
        return this.mValid;
    }

    private byte[] readFileToBuf(String fileName) {
        byte[] fileBuf = null;
        try {
            InputStream fileStream = new FileInputStream(fileName);
            try {
                int fileLength = fileStream.available();
                fileBuf = new byte[fileLength];
                fileStream.read(fileBuf);
                fileStream.close();
            } finally {
            }
        } catch (IOException e) {
            Slog.i(TAG, "File is not exit!" + fileName);
        }
        return fileBuf;
    }

    public boolean checkVersion(String version) {
        Slog.i(TAG, "CheckVersion:" + version + "/" + this.mBinVersionStr);
        String str = this.mBinVersionStr;
        return (str == null || version == null || str.startsWith(version) || this.mBinVersionStr.compareTo(version) <= 0) ? false : true;
    }

    private boolean parseOtaFile() {
        int startIndex = this.mIsBleDevice ? START_INDEX_FOR_179 : 0;
        byte[] bArr = this.mFileBuf;
        if (bArr != null && bArr.length > 64) {
            int fileHeadBaseAddr = startIndex;
            byte[] binStartFlagByte = new byte[8];
            System.arraycopy(bArr, fileHeadBaseAddr, binStartFlagByte, 0, binStartFlagByte.length);
            this.mBinStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
            Slog.i(TAG, "Bin Start Flag: " + this.mBinStartFlag);
            int fileHeadBaseAddr2 = fileHeadBaseAddr + 8;
            byte[] bArr2 = this.mFileBuf;
            byte[] bArr3 = this.mBinLengthByte;
            System.arraycopy(bArr2, fileHeadBaseAddr2, bArr3, 0, bArr3.length);
            byte[] bArr4 = this.mBinLengthByte;
            this.mBinLength = ((bArr4[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr4[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr4[1] << 8) & 65280) + (bArr4[0] & 255);
            Slog.i(TAG, "Bin Lenght: " + this.mBinLength);
            int fileHeadBaseAddr3 = fileHeadBaseAddr2 + 4;
            byte[] bArr5 = this.mFileBuf;
            byte[] bArr6 = this.mBinCheckSumByte;
            System.arraycopy(bArr5, fileHeadBaseAddr3, bArr6, 0, bArr6.length);
            byte[] bArr7 = this.mBinCheckSumByte;
            this.mBinCheckSum = ((bArr7[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr7[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr7[1] << 8) & 65280) + (bArr7[0] & 255);
            Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
            byte[] bArr8 = new byte[50];
            this.mBinVersion = bArr8;
            System.arraycopy(this.mFileBuf, fileHeadBaseAddr3 + 4, bArr8, 0, bArr8.length);
            this.mBinVersionStr = String.format("%02x", Byte.valueOf(this.mBinVersion[0])) + String.format("%02x", Byte.valueOf(this.mBinVersion[1]));
            Slog.i(TAG, "Bin Version : " + this.mBinVersionStr);
            byte[] bArr9 = this.mFileBuf;
            int sum1 = getSumInt(bArr9, startIndex + 64, (bArr9.length - startIndex) - 64);
            if (sum1 != this.mBinCheckSum) {
                Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
                return false;
            }
            byte sum2 = getSum(this.mFileBuf, startIndex, 63);
            if (sum2 != this.mFileBuf[startIndex + 63]) {
                Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[startIndex + 63]));
                return false;
            }
            byte[] bArr10 = this.mBinStartAddressByte;
            bArr10[0] = 0;
            bArr10[1] = CommunicationUtil.PAD_ADDRESS;
            bArr10[2] = 6;
            bArr10[3] = 0;
            return true;
        }
        Slog.i(TAG, "TouchPad Parse OtaFile err");
        return false;
    }

    public byte[] getUpgradeInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 2;
        bytes[9] = 19;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinVersion, 0, bytes, 22, 2);
        bytes[24] = 43;
        bytes[25] = CommunicationUtil.COMMAND_MCU_BOOT;
        bytes[26] = 32;
        bytes[27] = 3;
        bytes[29] = getSum(bytes, 4, 25);
        return bytes;
    }

    public byte[] getBinPackInfo(byte target, int offset) {
        byte[] bytes = new byte[68];
        int binlength = 52;
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
        bytes[9] = CommunicationUtil.KEYBOARD_ADDRESS;
        byte[] offsetB = MiuiKeyboardUtil.int2Bytes(offset);
        bytes[10] = offsetB[3];
        bytes[11] = offsetB[2];
        bytes[12] = offsetB[1];
        bytes[13] = CommunicationUtil.COMMAND_AUTH_52;
        if (this.mIsBleDevice) {
            byte[] bArr = this.mFileBuf;
            if (bArr.length < offset + 4096 + 52) {
                binlength = bArr.length - (offset + 4096);
            }
            System.arraycopy(bArr, offset + 4096, bytes, 14, binlength);
        } else {
            byte[] bArr2 = this.mFileBuf;
            if (bArr2.length < offset + 52) {
                binlength = bArr2.length - offset;
            }
            System.arraycopy(bArr2, offset, bytes, 14, binlength);
        }
        bytes[66] = getSum(bytes, 4, 62);
        return bytes;
    }

    public byte[] getUpEndInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 4;
        bytes[9] = 14;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinVersion, 0, bytes, 22, 2);
        bytes[24] = getSum(bytes, 4, 20);
        return bytes;
    }

    public byte[] getUpFlashInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 6;
        bytes[9] = 4;
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 10, 4);
        bytes[14] = getSum(bytes, 4, 10);
        return bytes;
    }

    public int getBinPacketTotal(int length) {
        int binLength = this.mIsBleDevice ? this.mBinLength : this.mBinLength + 64;
        switch (length) {
            case 32:
                int totle = binLength / 20;
                if (this.mBinLength % 20 != 0) {
                    return totle + 1;
                }
                return totle;
            case 64:
                int totle2 = binLength / 52;
                if (this.mBinLength % 52 != 0) {
                    return totle2 + 1;
                }
                return totle2;
            default:
                return 0;
        }
    }

    private byte getSum(byte[] data, int start, int length) {
        byte sum = 0;
        for (int i = start; i < start + length; i++) {
            sum = (byte) ((data[i] & 255) + sum);
        }
        return sum;
    }

    private int getSumInt(byte[] data, int start, int length) {
        int sum = 0;
        for (int i = start; i < start + length; i++) {
            sum += data[i] & 255;
        }
        return sum;
    }
}
