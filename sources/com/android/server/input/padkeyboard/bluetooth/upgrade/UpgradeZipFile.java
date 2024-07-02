package com.android.server.input.padkeyboard.bluetooth.upgrade;

import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* loaded from: classes.dex */
public class UpgradeZipFile {
    public static String OTA_FLIE_PATH = "odm/etc/Redmi_Keyboard_OTA.zip";
    private static final String TAG = "UpgradeZipFile";
    private String mPath;
    private boolean mValid;
    private String mVersion;
    private byte[] mFileBufJson = null;
    private byte[] mFileBufDat = null;
    private byte[] mFileBufBin = null;

    public UpgradeZipFile(String path) {
        if (path == null) {
            Slog.i(TAG, "UpgradeOta file Path is null");
            return;
        }
        this.mPath = path;
        Slog.i(TAG, "UpgradeOta file Path:" + this.mPath);
        readFileToBuf(this.mPath);
        if (this.mFileBufDat == null || this.mFileBufBin == null) {
            Slog.i(TAG, "UpgradeOta file buff is null or length low than 6000");
            return;
        }
        this.mValid = true;
        this.mVersion = String.format("%02x", Byte.valueOf(this.mFileBufBin[65])) + String.format("%02x", Byte.valueOf(this.mFileBufBin[64]));
        Slog.i(TAG, "Bin version is " + this.mVersion);
    }

    public boolean isValidFile() {
        if (this.mFileBufDat != null && this.mFileBufBin != null) {
            return true;
        }
        return false;
    }

    public String getVersion() {
        return this.mVersion;
    }

    public void setVersion(String version) {
        this.mVersion = version;
    }

    private void readFileToBuf(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            Slog.i(TAG, "The Upgrade file does not exist.");
            return;
        }
        try {
            InputStream fIn = new FileInputStream(fileName);
            try {
                if (fIn.available() > 10485760) {
                    Slog.i(TAG, "File too large: " + fIn.available() + " bytes (max 10 MB)");
                    fIn.close();
                    return;
                }
                ZipInputStream zipIn = new ZipInputStream(fIn);
                try {
                    byte[] buffer = new byte[1024];
                    for (ZipEntry zipEntry = zipIn.getNextEntry(); zipEntry != null; zipEntry = zipIn.getNextEntry()) {
                        if (!zipEntry.isDirectory()) {
                            String zipFileName = zipEntry.getName();
                            int leng = (int) zipEntry.getSize();
                            Slog.i(TAG, "ZipFileName:" + zipFileName + "/" + leng);
                            if (zipFileName.contains(".json")) {
                                byte[] bArr = new byte[leng];
                                this.mFileBufJson = bArr;
                                zipIn.read(bArr);
                            }
                            if (zipFileName.contains(".dat")) {
                                byte[] bArr2 = new byte[leng];
                                this.mFileBufDat = bArr2;
                                zipIn.read(bArr2);
                            }
                            if (zipFileName.contains(".bin")) {
                                this.mFileBufBin = new byte[leng];
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                while (true) {
                                    int count = zipIn.read(buffer);
                                    if (count == -1) {
                                        break;
                                    } else {
                                        baos.write(buffer, 0, count);
                                    }
                                }
                                this.mFileBufBin = baos.toByteArray();
                            }
                        }
                    }
                    zipIn.close();
                    fIn.close();
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "readFileToBuf exception: " + e.getMessage());
        }
    }

    public byte[] getFileBufDat() {
        return this.mFileBufDat;
    }

    public byte[] getFileBufBin() {
        return this.mFileBufBin;
    }

    public double getPercent(int packetIndex, int packetLength) {
        if (this.mFileBufBin.length > 64) {
            float progress = Math.round(((packetIndex * packetLength) * 100) / r0.length);
            DecimalFormat df = new DecimalFormat("#.00");
            String percent = df.format(progress);
            return Double.parseDouble(percent);
        }
        return 0.0d;
    }

    public int getFileBufDatCrc() {
        byte[] bArr = this.mFileBufDat;
        if (bArr != null && bArr.length > 0) {
            CRC32 crc32 = new CRC32();
            crc32.update(this.mFileBufDat);
            return (int) (crc32.getValue() & 4294967295L);
        }
        return 0;
    }

    public int getFileBufBinCrc() {
        byte[] bArr = this.mFileBufBin;
        if (bArr != null && bArr.length > 0) {
            CRC32 crc32 = new CRC32();
            crc32.update(this.mFileBufBin);
            return (int) (crc32.getValue() & 4294967295L);
        }
        return 0;
    }
}
