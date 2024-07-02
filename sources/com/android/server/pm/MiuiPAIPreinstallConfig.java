package com.android.server.pm;

import android.os.FileUtils;
import android.os.SELinux;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.JournaledFile;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class MiuiPAIPreinstallConfig {
    private static final String OLD_PREINSTALL_PACKAGE_PAI_TRACKING_FILE = "/cust/etc/pre_install.appsflyer";
    private static final String OPERATOR_PREINSTALL_PACKAGE_TRACKING_FILE = "/mi_ext/product/etc/pre_install.appsflyer";
    private static final String PREINSTALL_PACKAGE_MIUI_TRACKING_DIR = "/data/miui/pai/";
    private static final String PREINSTALL_PACKAGE_MIUI_TRACKING_DIR_CONTEXT = "u:object_r:miui_pai_file:s0";
    private static final String PREINSTALL_PACKAGE_PAI_LIST = "/data/system/preinstallPAI.list";
    private static final String PREINSTALL_PACKAGE_PAI_TRACKING_FILE = "/data/miui/pai/pre_install.appsflyer";
    private static final String TYPE_TRACKING_APPSFLYER = "appsflyer";
    private static final String TYPE_TRACKING_MIUI = "xiaomi";
    private static final String TAG = MiuiPAIPreinstallConfig.class.getSimpleName();
    private static List<String> sPackagePAIList = new ArrayList();
    private static List<String> sTraditionalTrackContentList = new ArrayList();
    private static List<String> sNewTrackContentList = new ArrayList();

    public static void init() {
        readPackagePAIList();
        copyTraditionalTrackFileToNewLocationIfNeed();
    }

    private static void readPackagePAIList() {
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(PREINSTALL_PACKAGE_PAI_LIST));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    } else {
                        sPackagePAIList.add(line);
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs while read preinstalled PAI packages " + e);
                if (reader == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(reader);
        } catch (Throwable th) {
            if (reader != null) {
                IoUtils.closeQuietly(reader);
            }
            throw th;
        }
    }

    private static void copyTraditionalTrackFileToNewLocationIfNeed() {
        readNewPAITrackFileIfNeed();
        String appsflyerPath = SystemProperties.get("ro.appsflyer.preinstall.path", "");
        if (TextUtils.isEmpty(appsflyerPath)) {
            Slog.e(TAG, "no system property ro.appsflyer.preinstall.path");
            return;
        }
        File paiTrackingPath = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (TextUtils.equals(appsflyerPath, PREINSTALL_PACKAGE_PAI_TRACKING_FILE) && !paiTrackingPath.exists()) {
            try {
                Slog.d(TAG, "create new appsflyer tracking file：" + paiTrackingPath.getAbsolutePath());
                if (!paiTrackingPath.getParentFile().exists()) {
                    paiTrackingPath.getParentFile().mkdir();
                    FileUtils.setPermissions(paiTrackingPath.getParentFile(), 509, -1, -1);
                }
                paiTrackingPath.createNewFile();
                FileUtils.setPermissions(paiTrackingPath, 436, -1, -1);
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to create new appsflyer tracking" + e);
            }
            readTraditionalPAITrackFile();
            readOperatorTrackFile();
            writeNewPAITrackFile();
        }
        restoreconPreinstallDir();
    }

    private static void readTraditionalPAITrackFile() {
        Slog.i(TAG, "read traditional track file content from /cust/etc/pre_install.appsflyer");
        File file = new File(OLD_PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (file.exists()) {
            sTraditionalTrackContentList.clear();
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        } else {
                            sTraditionalTrackContentList.add(line);
                        }
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Error occurs while read traditional track file content" + e);
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        }
    }

    private static void readOperatorTrackFile() {
        Slog.i(TAG, "read Operator track file content from /mi_ext/product/etc/pre_install.appsflyer");
        File file = new File(OPERATOR_PREINSTALL_PACKAGE_TRACKING_FILE);
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (true) {
                    try {
                        String line = reader.readLine();
                        if (line != null) {
                            sTraditionalTrackContentList.add(line);
                        } else {
                            reader.close();
                            return;
                        }
                    } finally {
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs while read Operator track file content" + e);
            }
        }
    }

    private static void writeNewPAITrackFile() {
        String str = TAG;
        Slog.i(str, "Write old track file content to  /data/miui/pai/pre_install.appsflyer");
        List<String> list = sTraditionalTrackContentList;
        if (list == null || list.isEmpty()) {
            Slog.e(str, "no content write to new appsflyer file");
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                File newFile = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                    FileUtils.setPermissions(newFile.getParentFile(), 509, -1, -1);
                }
                bufferWriter = new BufferedWriter(new FileWriter(newFile, true));
                FileUtils.setPermissions(newFile, 436, -1, -1);
                synchronized (sTraditionalTrackContentList) {
                    for (int i = 0; i < sTraditionalTrackContentList.size(); i++) {
                        String content = sTraditionalTrackContentList.get(i);
                        if (!sNewTrackContentList.contains(content)) {
                            sNewTrackContentList.add(content);
                            bufferWriter.write(content);
                            bufferWriter.write("\n");
                        }
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to write track file from /cust/etc/pre_install.appsflyer to /data/miui/pai/pre_install.appsflyer");
            }
        } finally {
            IoUtils.closeQuietly(bufferWriter);
        }
    }

    private static void readNewPAITrackFileIfNeed() {
        Slog.i(TAG, "read new track file content from /data/miui/pai/pre_install.appsflyer");
        File file = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        } else if (!sNewTrackContentList.contains(line)) {
                            sNewTrackContentList.add(line);
                        }
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Error occurs while read new track file content on pkms start" + e);
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        }
    }

    private static void restoreconPreinstallDir() {
        File file = new File(PREINSTALL_PACKAGE_MIUI_TRACKING_DIR);
        String fileContext = SELinux.getFileContext(PREINSTALL_PACKAGE_MIUI_TRACKING_DIR);
        if (file.exists() && !TextUtils.equals(fileContext, PREINSTALL_PACKAGE_MIUI_TRACKING_DIR_CONTEXT)) {
            SELinux.restoreconRecursive(file);
        }
    }

    public static void writePreinstallPAIPackage(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                bufferWriter = new BufferedWriter(new FileWriter(PREINSTALL_PACKAGE_PAI_LIST, true));
                synchronized (sPackagePAIList) {
                    sPackagePAIList.add(pkg);
                    bufferWriter.write(pkg);
                    bufferWriter.write("\n");
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to write PAI package name.");
            }
        } finally {
            IoUtils.closeQuietly(bufferWriter);
        }
    }

    public static boolean isPreinstalledPAIPackage(String pkg) {
        boolean contains;
        synchronized (sPackagePAIList) {
            contains = sPackagePAIList.contains(pkg);
        }
        return contains;
    }

    public static void removeFromPreinstallPAIList(String pkg) {
        synchronized (sPackagePAIList) {
            if (sPackagePAIList.contains(pkg)) {
                sPackagePAIList.remove(pkg);
                File tempFile = new File("/data/system/preinstallPAI.list.tmp");
                JournaledFile journal = new JournaledFile(new File(PREINSTALL_PACKAGE_PAI_LIST), tempFile);
                File writeTarget = journal.chooseForWrite();
                FileOutputStream fstr = null;
                BufferedOutputStream str = null;
                try {
                    try {
                        fstr = new FileOutputStream(writeTarget);
                        str = new BufferedOutputStream(fstr);
                        FileUtils.setPermissions(fstr.getFD(), 416, 1000, 1032);
                        StringBuilder sb = new StringBuilder();
                        synchronized (sPackagePAIList) {
                            for (int i = 0; i < sPackagePAIList.size(); i++) {
                                sb.setLength(0);
                                sb.append(sPackagePAIList.get(i));
                                sb.append("\n");
                                str.write(sb.toString().getBytes());
                            }
                        }
                        str.flush();
                        FileUtils.sync(fstr);
                        journal.commit();
                    } catch (Exception e) {
                        Slog.wtf(TAG, "Failed to delete preinstallPAI.list + ", e);
                        journal.rollback();
                    }
                    IoUtils.closeQuietly(fstr);
                    IoUtils.closeQuietly(str);
                    Slog.d(TAG, "Delete package:" + pkg + " from preinstallPAI.list");
                } catch (Throwable th) {
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    throw th;
                }
            }
        }
    }

    public static void copyPreinstallPAITrackingFile(String type, String fileName, String content) {
        String filePath;
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(content)) {
            return;
        }
        boolean isAppsflyer = false;
        if (TextUtils.equals(TYPE_TRACKING_APPSFLYER, type)) {
            filePath = PREINSTALL_PACKAGE_PAI_TRACKING_FILE;
            isAppsflyer = true;
        } else if (TextUtils.equals(TYPE_TRACKING_MIUI, type)) {
            filePath = PREINSTALL_PACKAGE_MIUI_TRACKING_DIR + fileName;
        } else {
            Slog.e(TAG, "Used invalid pai tracking type =" + type + "! you can use type:appsflyer or xiaomi");
            return;
        }
        if (isAppsflyer && sNewTrackContentList.contains(content)) {
            Slog.i(TAG, "Content duplication dose not need to be written again! content is :" + content);
            return;
        }
        String str = TAG;
        Slog.i(str, "use " + type + " tracking method!");
        BufferedWriter bw = null;
        try {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    Slog.d(str, "create tracking file：" + file.getAbsolutePath());
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdir();
                        FileUtils.setPermissions(file.getParentFile(), 509, -1, -1);
                    }
                    file.createNewFile();
                }
                FileUtils.setPermissions(file, 436, -1, -1);
                bw = new BufferedWriter(new FileWriter(file, isAppsflyer));
                if (isAppsflyer) {
                    sNewTrackContentList.add(content);
                }
                bw.write(content);
                bw.write("\n");
                bw.flush();
                Slog.i(str, "Copy PAI tracking content Success!");
                IoUtils.closeQuietly(bw);
                restoreconPreinstallDir();
            } catch (IOException e) {
                if (TextUtils.equals(TYPE_TRACKING_APPSFLYER, type)) {
                    Slog.e(TAG, "Error occurs when to copy PAI tracking content(" + content + ") into " + filePath + ":" + e);
                } else if (TextUtils.equals(TYPE_TRACKING_MIUI, type)) {
                    Slog.e(TAG, "Error occurs when to create " + fileName + " PAI tracking file into " + filePath + ":" + e);
                }
                IoUtils.closeQuietly(bw);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(bw);
            throw th;
        }
    }
}
