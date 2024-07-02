package com.android.server.pm;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import com.android.internal.util.FastPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.pm.pkg.PackageStateInternal;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class PackageManagerServiceUtilsImpl extends PackageManagerServiceUtilsStub {
    private static final long MAX_CRITICAL_INFO_DUMP_SIZE = 3000000;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PackageManagerServiceUtilsImpl> {

        /* compiled from: PackageManagerServiceUtilsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PackageManagerServiceUtilsImpl INSTANCE = new PackageManagerServiceUtilsImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PackageManagerServiceUtilsImpl m2137provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PackageManagerServiceUtilsImpl m2136provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.PackageManagerServiceUtilsImpl is marked as singleton");
        }
    }

    public void truncateSettingsProblemFileIfNeeded() {
        truncateSettingsProblemFileIfNeeded(getSettingsProblemFile(), getBackupSettingsProblemFile());
        truncateSettingsProblemFileIfNeeded(getMiuiPackageProblemFile(), getBackupMiuiPackageProblemFile());
    }

    public void logMiuiCriticalInfo(int priority, String msg) {
        Slog.println(priority, "PackageManager", msg);
        EventLogTags.writePmCriticalInfo(msg);
        try {
            File fname = getMiuiPackageProblemFile();
            FileOutputStream out = new FileOutputStream(fname, true);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(out);
            SimpleDateFormat formatter = new SimpleDateFormat();
            String dateString = formatter.format(new Date(System.currentTimeMillis()));
            fastPrintWriter.println(dateString + ": " + msg);
            fastPrintWriter.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    public void dumpCriticalInfo(PrintWriter pw, String msg) {
        File file = getMiuiPackageProblemFile();
        long skipSize = file.length() - MAX_CRITICAL_INFO_DUMP_SIZE;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            if (skipSize > 0) {
                try {
                    in.skip(skipSize);
                } finally {
                }
            }
            while (true) {
                String line = in.readLine();
                if (line != null) {
                    if (!line.contains("ignored: updated version")) {
                        if (msg != null) {
                            pw.print(msg);
                        }
                        pw.println(line);
                    }
                } else {
                    in.close();
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    public void logDisableComponent(PackageManager.ComponentEnabledSetting setting, boolean isSystem, String callingPackage, int callingUid, int callingPid) {
        String str;
        StringBuilder append = new StringBuilder().append("set app");
        if (setting.isComponent()) {
            str = " component=" + setting.getComponentName();
        } else {
            str = " package: " + setting.getPackageName();
        }
        String msg = append.append(str).append(" enabled state: ").append(setting.getEnabledState()).append(" from ").append(callingPackage).append(",uid=").append(callingUid).append(",pid=").append(callingPid).toString();
        if (!setting.isComponent() && isSystem && setting.getEnabledState() == 3) {
            logMiuiCriticalInfo(3, msg);
        } else {
            Slog.d("PackageManager", msg);
        }
    }

    public void logSuspendApp(String packageName, Computer snapshot, boolean suspended, String callingPackage, int callingUid) {
        PackageStateInternal ps = snapshot.getPackageStateInternal(packageName);
        if (ps != null && ps.isSystem() && suspended) {
            String msg = "set package: " + packageName + " suspended from " + callingPackage + ",uid=" + callingUid;
            PackageManagerServiceUtilsStub.get().logMiuiCriticalInfo(3, msg);
        }
    }

    private void truncateSettingsProblemFileIfNeeded(File srcFile, File backupFile) {
        ParcelFileDescriptor srcPfd = null;
        ParcelFileDescriptor backupPfd = null;
        try {
        } catch (ErrnoException | IOException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(srcPfd);
            IoUtils.closeQuietly(backupPfd);
            throw th;
        }
        if (srcFile.exists() && srcFile.length() >= 524288) {
            srcPfd = ParcelFileDescriptor.open(srcFile, 268435456);
            backupPfd = ParcelFileDescriptor.open(backupFile, 939524096);
            if (srcPfd != null && backupPfd != null) {
                Os.lseek(srcPfd.getFileDescriptor(), srcFile.length() / 2, OsConstants.SEEK_SET);
                FileUtils.copy(srcPfd.getFileDescriptor(), backupPfd.getFileDescriptor());
                backupFile.renameTo(srcFile);
                FileUtils.setPermissions(srcFile.toString(), 508, -1, -1);
                IoUtils.closeQuietly(srcPfd);
                IoUtils.closeQuietly(backupPfd);
                return;
            }
            IoUtils.closeQuietly(srcPfd);
            IoUtils.closeQuietly(backupPfd);
            return;
        }
        IoUtils.closeQuietly((AutoCloseable) null);
        IoUtils.closeQuietly((AutoCloseable) null);
    }

    private File getSettingsProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, "uiderrors.txt");
        return fname;
    }

    private File getBackupSettingsProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, "uiderrors_backup.txt");
        return fname;
    }

    private File getMiuiPackageProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, "pkg_history.txt");
        if (!fname.exists()) {
            try {
                fname.createNewFile();
            } catch (IOException e) {
            }
        }
        return fname;
    }

    private File getBackupMiuiPackageProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, "pkg_history_backup.txt");
        return fname;
    }
}
