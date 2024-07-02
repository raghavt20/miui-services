package com.android.server;

import android.os.FileUtils;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Slog;
import com.android.internal.logging.EventLogTags;
import com.android.server.pm.PackageManagerService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.GeneralExceptionEvent;

/* loaded from: classes.dex */
public class MiuiFallbackHelper extends MiuiFallbackHelperStub {
    private static final String FALLBACK_FILE_SUFFIX = ".fallback";
    private static final String FALLBACK_TEMP_FILE_SUFFIX = ".fallbacktemp";
    private static final String TAG = MiuiFallbackHelper.class.getSimpleName();
    private static final String XATTR_MD5 = "user.md5";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFallbackHelper> {

        /* compiled from: MiuiFallbackHelper$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFallbackHelper INSTANCE = new MiuiFallbackHelper();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiFallbackHelper m247provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiFallbackHelper m246provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.MiuiFallbackHelper is marked as singleton");
        }
    }

    public void snapshotFile(String filePath, long periodTime) {
        if (periodTime < 0) {
            throw new IllegalArgumentException("snapshotFile periodTime must >= 0");
        }
        File originalFile = new File(filePath);
        if (originalFile.exists()) {
            if (periodTime == 0 || checkNeedCopy(filePath, periodTime)) {
                long startTime = SystemClock.uptimeMillis();
                File fallbackFile = getFallbackFile(filePath);
                File fallbackTempFile = getFallbackTempFile(filePath);
                if (fallbackFile.exists()) {
                    if (fallbackTempFile.exists()) {
                        fallbackFile.delete();
                        Slog.w(TAG, "Preserving older fallback file backup");
                    } else if (!fallbackFile.renameTo(fallbackTempFile)) {
                        Slog.w(TAG, "Unable to backup " + filePath);
                        return;
                    }
                }
                try {
                    FileInputStream fis = new FileInputStream(originalFile);
                    try {
                        FileOutputStream fos = new FileOutputStream(fallbackFile);
                        try {
                            FileUtils.copy(fis, fos);
                            fos.flush();
                            calculateMD5(fallbackFile, true);
                            FileUtils.sync(fos);
                            if (fallbackTempFile.exists()) {
                                fallbackTempFile.delete();
                            }
                            FileUtils.setPermissions(fallbackFile.toString(), 288, -1, -1);
                            fos.close();
                            fis.close();
                        } finally {
                        }
                    } finally {
                    }
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to write fallback file for: " + filePath);
                }
                EventLogTags.writeCommitSysConfigFile("snapshotFile: " + filePath, SystemClock.uptimeMillis() - startTime);
            }
        }
    }

    public boolean restoreFile(String filePath) {
        File fallbackFile = getFallbackFile(filePath);
        File fallbackTempFile = getFallbackTempFile(filePath);
        File originalFile = new File(filePath);
        if (fallbackFile.exists() && fallbackTempFile.exists()) {
            if (fallbackTempFile.renameTo(fallbackFile)) {
                Slog.w(TAG, "fallback file maybe saved abnormally last time. trust temp file");
            } else {
                Slog.w(TAG, "fallback temp file rename failed: " + fallbackTempFile.getAbsolutePath());
                return false;
            }
        }
        if (!fallbackFile.exists()) {
            return false;
        }
        byte[] md5sum = calculateMD5(fallbackFile, false);
        byte[] xattr = null;
        try {
            xattr = Os.getxattr(fallbackFile.getAbsolutePath(), XATTR_MD5);
        } catch (ErrnoException e) {
            Slog.d(TAG, "restoreFile: " + e);
        }
        if (md5sum == null || !Arrays.equals(md5sum, xattr)) {
            Slog.w(TAG, "restoreFile: " + filePath + ". but MD5 is not equal!");
            return false;
        }
        String msg = filePath + " exists but is corrupted. Fallback file will be used. lastModified: " + fallbackFile.lastModified();
        String timeStamp = String.valueOf(fallbackFile.lastModified());
        PackageManagerService.reportSettingsProblem(3, msg);
        if (!fallbackFile.renameTo(originalFile)) {
            return false;
        }
        String restoreFilePath = originalFile.getPath();
        reportFileCorruptionForMqs(restoreFilePath, timeStamp);
        return true;
    }

    private boolean checkNeedCopy(String filePath, long periodTime) {
        File fallbackFile = getFallbackFile(filePath);
        return !fallbackFile.exists() || System.currentTimeMillis() - fallbackFile.lastModified() > periodTime;
    }

    private byte[] calculateMD5(File file, boolean setXattr) {
        byte[] md5sum = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] buffer = new byte[8192];
                while (true) {
                    int read = fis.read(buffer);
                    if (read <= 0) {
                        break;
                    }
                    digest.update(buffer, 0, read);
                }
                md5sum = digest.digest();
                if (setXattr) {
                    Os.setxattr(file.getAbsolutePath(), XATTR_MD5, md5sum, 0);
                }
                fis.close();
            } catch (Throwable th) {
                try {
                    fis.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (ErrnoException e) {
            Slog.d(TAG, "calculateMD5 ErrnoException: " + e);
        } catch (IOException e2) {
            Slog.d(TAG, "calculateMD5 IOException: " + e2);
        } catch (NoSuchAlgorithmException e3) {
            Slog.d(TAG, "calculateMD5 NoSuchAlgorithmException: " + e3);
        }
        return md5sum;
    }

    private File getFallbackFile(String filePath) {
        return new File(filePath + FALLBACK_FILE_SUFFIX);
    }

    private File getFallbackTempFile(String filePath) {
        return new File(filePath + FALLBACK_TEMP_FILE_SUFFIX);
    }

    private void reportFileCorruptionForMqs(String filePath, String timeStamp) {
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        event.setType(439);
        event.setSystem(true);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(filePath);
        event.setPackageName("android");
        event.setDetails(timeStamp);
        event.setUpload(true);
        event.setEnsureReport(true);
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
    }
}
