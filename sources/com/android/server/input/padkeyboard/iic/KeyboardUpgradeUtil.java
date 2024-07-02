package com.android.server.input.padkeyboard.iic;

import android.content.Context;
import android.util.Slog;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class KeyboardUpgradeUtil {
    private static final int START_INFO_INDEX_FOR_176 = 135104;
    private static final int START_INFO_INDEX_FOR_179 = 24512;
    private static final int START_INFO_INDEX_FOR_8012 = 12224;
    private static final String TAG = "KeyboardUpgradeUtil";
    public int mBinCheckSum;
    public int mBinLength;
    public String mBinVersionStr;
    private byte[] mFileBuf;
    private byte mKeyType;
    private byte mNFCType;
    private int mStartInfoIndex;
    private byte mTouchType;
    public String mUpgradeFilePath;
    public boolean mValid;
    public byte[] mBinLengthByte = new byte[4];
    public byte[] mBinCheckSumByte = new byte[4];
    public byte[] mBinVersion = new byte[2];
    public byte[] mBinStartAddressByte = new byte[4];

    public KeyboardUpgradeUtil(Context context, String path, byte keyType) {
        if (path == null) {
            Slog.i(TAG, "UpgradeOta file Path is null");
            return;
        }
        this.mUpgradeFilePath = path;
        Slog.i(TAG, "UpgradeOta file Path:" + this.mUpgradeFilePath);
        byte[] ReadUpgradeFile = ReadUpgradeFile(this.mUpgradeFilePath);
        this.mFileBuf = ReadUpgradeFile;
        if (ReadUpgradeFile == null) {
            Slog.i(TAG, "UpgradeOta file buff is null or length low than 6000");
        } else {
            this.mValid = parseOtaFile(keyType);
        }
    }

    private boolean parseOtaFile(byte keyType) {
        if (keyType == 33) {
            return parseOtaFileFor179();
        }
        if (!MiuiKeyboardUtil.isXM2022MCU()) {
            return parseOtaFileFor8012();
        }
        return parseOtaFileFor176();
    }

    private byte[] ReadUpgradeFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
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
        Slog.i(TAG, "get verison :" + version + " , bin version:" + this.mBinVersionStr);
        String str = this.mBinVersionStr;
        return (str == null || version == null || str.startsWith(version) || this.mBinVersionStr.compareTo(version) <= 0) ? false : true;
    }

    private boolean parseOtaFileFor176() {
        this.mStartInfoIndex = START_INFO_INDEX_FOR_176;
        byte[] bArr = this.mFileBuf;
        if (bArr == null || bArr.length <= 135168) {
            return false;
        }
        byte[] binStartFlagByte = new byte[8];
        System.arraycopy(bArr, START_INFO_INDEX_FOR_176, binStartFlagByte, 0, binStartFlagByte.length);
        String binStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
        Slog.i(TAG, "Bin Start Flag: " + binStartFlag);
        int fileHeadBaseAddr = START_INFO_INDEX_FOR_176 + 8;
        byte[] bArr2 = this.mFileBuf;
        byte[] bArr3 = this.mBinLengthByte;
        System.arraycopy(bArr2, fileHeadBaseAddr, bArr3, 0, bArr3.length);
        byte[] bArr4 = this.mBinLengthByte;
        this.mBinLength = ((bArr4[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr4[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr4[1] << 8) & 65280) + (bArr4[0] & 255);
        Slog.i(TAG, "Bin Length: " + this.mBinLength);
        int fileHeadBaseAddr2 = fileHeadBaseAddr + 4;
        byte[] bArr5 = this.mFileBuf;
        byte[] bArr6 = this.mBinCheckSumByte;
        System.arraycopy(bArr5, fileHeadBaseAddr2, bArr6, 0, bArr6.length);
        byte[] bArr7 = this.mBinCheckSumByte;
        this.mBinCheckSum = ((bArr7[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr7[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr7[1] << 8) & 65280) + (bArr7[0] & 255);
        Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
        byte[] bArr8 = new byte[2];
        this.mBinVersion = bArr8;
        System.arraycopy(this.mFileBuf, fileHeadBaseAddr2 + 4, bArr8, 0, bArr8.length);
        this.mBinVersionStr = String.format("%02x", Byte.valueOf(this.mBinVersion[1])) + String.format("%02x", Byte.valueOf(this.mBinVersion[0]));
        Slog.i(TAG, "Bin Version : " + this.mBinVersionStr);
        int sum1 = CommunicationUtil.getSumInt(this.mFileBuf, 135168, (r5.length - START_INFO_INDEX_FOR_176) - 64);
        if (sum1 != this.mBinCheckSum) {
            Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
            return false;
        }
        byte sum2 = CommunicationUtil.getSum(this.mFileBuf, START_INFO_INDEX_FOR_176, 63);
        byte[] bArr9 = this.mFileBuf;
        if (sum2 != bArr9[135167]) {
            Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[135167]));
            return false;
        }
        this.mKeyType = bArr9[135368];
        this.mNFCType = (byte) (bArr9[135369] >> 6);
        this.mTouchType = (byte) (bArr9[135370] & 15);
        Slog.i(TAG, "For 176 mKeyType:" + ((int) this.mKeyType) + ", mNFCType:" + ((int) this.mNFCType) + ", mTouchType:" + ((int) this.mTouchType));
        byte[] bArr10 = this.mBinStartAddressByte;
        bArr10[0] = 0;
        bArr10[1] = CommunicationUtil.PAD_ADDRESS;
        bArr10[2] = 5;
        bArr10[3] = 0;
        return true;
    }

    private boolean parseOtaFileFor179() {
        this.mStartInfoIndex = START_INFO_INDEX_FOR_179;
        byte[] bArr = this.mFileBuf;
        if (bArr != null && bArr.length > 4032) {
            byte[] binStartFlagByte = new byte[8];
            System.arraycopy(bArr, START_INFO_INDEX_FOR_179, binStartFlagByte, 0, binStartFlagByte.length);
            String binStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
            Slog.i(TAG, "Bin Start Flag: " + binStartFlag);
            int mFileHeadBaseAddr = START_INFO_INDEX_FOR_179 + 8;
            byte[] bArr2 = this.mFileBuf;
            byte[] bArr3 = this.mBinLengthByte;
            System.arraycopy(bArr2, mFileHeadBaseAddr, bArr3, 0, bArr3.length);
            byte[] bArr4 = this.mBinLengthByte;
            this.mBinLength = ((bArr4[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr4[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr4[1] << 8) & 65280) + (bArr4[0] & 255);
            Slog.i(TAG, "Bin Lenght: " + this.mBinLength);
            int mFileHeadBaseAddr2 = mFileHeadBaseAddr + 4;
            byte[] bArr5 = this.mFileBuf;
            byte[] bArr6 = this.mBinCheckSumByte;
            System.arraycopy(bArr5, mFileHeadBaseAddr2, bArr6, 0, bArr6.length);
            byte[] bArr7 = this.mBinCheckSumByte;
            this.mBinCheckSum = ((bArr7[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr7[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr7[1] << 8) & 65280) + (bArr7[0] & 255);
            Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
            byte[] bArr8 = new byte[2];
            this.mBinVersion = bArr8;
            System.arraycopy(this.mFileBuf, mFileHeadBaseAddr2 + 4, bArr8, 0, bArr8.length);
            this.mBinVersionStr = String.format("%02x", Byte.valueOf(this.mBinVersion[1])) + String.format("%02x", Byte.valueOf(this.mBinVersion[0]));
            Slog.i(TAG, "Bin Version : " + this.mBinVersionStr);
            byte[] bArr9 = this.mFileBuf;
            int i = this.mStartInfoIndex;
            int sum1 = CommunicationUtil.getSumInt(bArr9, i + 64, bArr9.length - (i + 64));
            if (sum1 != this.mBinCheckSum) {
                Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
                return false;
            }
            byte sum2 = CommunicationUtil.getSum(this.mFileBuf, this.mStartInfoIndex, 63);
            byte[] bArr10 = this.mFileBuf;
            if (sum2 != bArr10[this.mStartInfoIndex + 63]) {
                Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[this.mStartInfoIndex + 63]));
                return false;
            }
            this.mKeyType = bArr10[24648];
            this.mNFCType = (byte) (bArr10[24649] >> 5);
            this.mTouchType = (byte) (bArr10[24650] & 15);
            Slog.i(TAG, "For 179 mKeyType:" + ((int) this.mKeyType) + ", mNFCType:" + ((int) this.mNFCType) + ", mTouchType:" + ((int) this.mTouchType));
            byte[] bArr11 = this.mBinStartAddressByte;
            bArr11[0] = 0;
            bArr11[1] = MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH;
            bArr11[2] = 6;
            bArr11[3] = 0;
            return true;
        }
        Slog.i(TAG, "179 Parse OtaFile err");
        return false;
    }

    private boolean parseOtaFileFor8012() {
        this.mStartInfoIndex = START_INFO_INDEX_FOR_8012;
        byte[] bArr = this.mFileBuf;
        if (bArr != null && bArr.length > 4032) {
            byte[] binStartFlagByte = new byte[8];
            System.arraycopy(bArr, START_INFO_INDEX_FOR_8012, binStartFlagByte, 0, binStartFlagByte.length);
            String binStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
            Slog.i(TAG, "Bin Start Flag: " + binStartFlag);
            int mFileHeadBaseAddr = START_INFO_INDEX_FOR_8012 + 8;
            byte[] bArr2 = this.mFileBuf;
            byte[] bArr3 = this.mBinLengthByte;
            System.arraycopy(bArr2, mFileHeadBaseAddr, bArr3, 0, bArr3.length);
            byte[] bArr4 = this.mBinLengthByte;
            this.mBinLength = ((bArr4[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr4[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr4[1] << 8) & 65280) + (bArr4[0] & 255);
            Slog.i(TAG, "Bin Lenght: " + this.mBinLength);
            int mFileHeadBaseAddr2 = mFileHeadBaseAddr + 4;
            byte[] bArr5 = this.mFileBuf;
            byte[] bArr6 = this.mBinCheckSumByte;
            System.arraycopy(bArr5, mFileHeadBaseAddr2, bArr6, 0, bArr6.length);
            byte[] bArr7 = this.mBinCheckSumByte;
            this.mBinCheckSum = ((bArr7[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr7[2] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((bArr7[1] << 8) & 65280) + (bArr7[0] & 255);
            Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
            byte[] bArr8 = new byte[2];
            this.mBinVersion = bArr8;
            System.arraycopy(this.mFileBuf, mFileHeadBaseAddr2 + 4, bArr8, 0, bArr8.length);
            this.mBinVersionStr = String.format("%02x", Byte.valueOf(this.mBinVersion[1])) + String.format("%02x", Byte.valueOf(this.mBinVersion[0]));
            Slog.i(TAG, "Bin Version : " + this.mBinVersionStr);
            byte[] bArr9 = this.mFileBuf;
            int i = this.mStartInfoIndex;
            int sum1 = CommunicationUtil.getSumInt(bArr9, i + 64, bArr9.length - (i + 64));
            if (sum1 != this.mBinCheckSum) {
                Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
                return false;
            }
            byte sum2 = CommunicationUtil.getSum(this.mFileBuf, this.mStartInfoIndex, 63);
            byte[] bArr10 = this.mFileBuf;
            if (sum2 != bArr10[this.mStartInfoIndex + 63]) {
                Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[this.mStartInfoIndex + 63]));
                return false;
            }
            this.mKeyType = bArr10[24536];
            this.mNFCType = (byte) (bArr10[24537] >> 5);
            this.mTouchType = (byte) (bArr10[24538] & 15);
            Slog.i(TAG, "For 8012 mKeyType:" + ((int) this.mKeyType) + ", mNFCType:" + ((int) this.mNFCType) + ", mTouchType:" + ((int) this.mTouchType));
            byte[] bArr11 = this.mBinStartAddressByte;
            bArr11[0] = 0;
            bArr11[1] = 32;
            bArr11[2] = 0;
            bArr11[3] = 0;
            return true;
        }
        Slog.i(TAG, "8012 Parse OtaFile err");
        return false;
    }

    public byte[] getUpgradeInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
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
        bytes[26] = this.mKeyType;
        bytes[27] = this.mTouchType;
        bytes[28] = this.mNFCType;
        bytes[29] = CommunicationUtil.getSum(bytes, 4, 25);
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
        byte[] bArr = this.mFileBuf;
        int length = bArr.length;
        int i = this.mStartInfoIndex;
        if (length < i + 64 + offset + 52) {
            binlength = ((bArr.length - i) - 64) - offset;
        }
        bytes[13] = CommunicationUtil.COMMAND_AUTH_52;
        System.arraycopy(bArr, i + 64 + offset, bytes, 14, binlength);
        bytes[66] = CommunicationUtil.getSum(bytes, 4, 62);
        return bytes;
    }

    public byte[] getUpEndInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 4;
        bytes[9] = 14;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinVersion, 0, bytes, 22, 2);
        bytes[24] = CommunicationUtil.getSum(bytes, 4, 21);
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
        bytes[14] = CommunicationUtil.getSum(bytes, 4, 10);
        return bytes;
    }

    public int getBinPacketTotal(int length) {
        switch (length) {
            case 32:
                int i = this.mBinLength;
                int totle = i / 20;
                if (i % 20 != 0) {
                    return totle + 1;
                }
                return totle;
            case 64:
                int i2 = this.mBinLength;
                int totle2 = i2 / 52;
                if (i2 % 52 != 0) {
                    return totle2 + 1;
                }
                return totle2;
            default:
                return 0;
        }
    }
}
