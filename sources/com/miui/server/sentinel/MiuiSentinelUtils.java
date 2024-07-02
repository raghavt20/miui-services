package com.miui.server.sentinel;

import android.os.Debug;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.miui.server.stability.ScoutMemoryUtils;
import com.xiaomi.abtest.d.d;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class MiuiSentinelUtils {
    private static final int MAX_FD_AMOUNT = 1000;
    private static final int MAX_THREAD_AMOUNT = 500;
    private static final String MIUI_SENTINEL_DIR = "/data/miuilog/stability/memleak/sentinel";
    private static boolean MTBF_MIUI_TEST = false;
    private static final String PROPERTIES_BUILD_FINGERPRINT = "ro.build.fingerprint";
    private static final String PROPERTIES_MTBF_MIUI_TEST = "ro.miui.mtbftest";
    private static final String PROPERTIES_MTBF_TEST = "persist.mtbf.test";
    private static final String PROPERTIES_OMNI_TEST = "persist.omni.test";
    private static final String SYSPROP_ENABLE_TRACK_MALLOC = "persist.track.malloc.enable";
    private static final int SYSTEM_SERVER_MAX_JAVAHEAP = 409600;
    private static final int SYSTEM_SERVER_MAX_NATIVEHEAP = 358400;
    private static final String TAG = "MiuiSentinelUtils";
    private static final String PROPERTIES_MTBF_COREDUMP = "persist.reboot.coredump";
    private static boolean REBOOT_COREDUMP = SystemProperties.getBoolean(PROPERTIES_MTBF_COREDUMP, false);

    static {
        MTBF_MIUI_TEST = SystemProperties.getInt(PROPERTIES_MTBF_MIUI_TEST, 0) == 1;
    }

    private static boolean isOmniTest() {
        return SystemProperties.getInt(PROPERTIES_OMNI_TEST, 0) == 1;
    }

    private static boolean isMtbfTest() {
        return SystemProperties.getBoolean(PROPERTIES_MTBF_TEST, false) || REBOOT_COREDUMP || MTBF_MIUI_TEST;
    }

    public static boolean isLaboratoryTest() {
        return isMtbfTest() || isOmniTest();
    }

    public static boolean isEnaleTrack() {
        if (SystemProperties.get(SYSPROP_ENABLE_TRACK_MALLOC, "") != "") {
            return true;
        }
        return false;
    }

    public static long getTotalRss() {
        Debug.MemoryInfo mi = new Debug.MemoryInfo();
        Slog.d(TAG, "total RSS :" + mi.getTotalRss());
        return mi.getTotalRss();
    }

    public static String getFormatDateTime(long timeMillis) {
        Date date = new Date(timeMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return dateFormat.format(date);
    }

    public static String getProcessCmdline(int pid) {
        String cmdline = "unknown";
        if (pid == 0) {
            return "unknown";
        }
        try {
            BufferedReader cmdlineReader = new BufferedReader(new FileReader("proc/" + pid + "/cmdline"));
            try {
                cmdline = cmdlineReader.readLine().trim();
                cmdlineReader.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.w(TAG, "Read process(" + pid + ") cmdline Error");
        }
        return cmdline;
    }

    public static long getProcessThreshold(String processname, int type) {
        switch (type) {
            case 17:
                if (MiuiSentinelService.getAppJavaheapWhiteList().get(processname) != null) {
                    long threshold = MiuiSentinelService.getAppJavaheapWhiteList().get(processname).intValue();
                    return threshold;
                }
                if (!"system_server".equals(processname)) {
                    return 0L;
                }
                return 409600L;
            case 18:
                if (MiuiSentinelService.getAppNativeheapWhiteList().get(processname) != null) {
                    long threshold2 = MiuiSentinelService.getAppNativeheapWhiteList().get(processname).intValue();
                    return threshold2;
                }
                if (!"system_server".equals(processname)) {
                    return 0L;
                }
                return 358400L;
            default:
                Slog.d(TAG, "The process threshold cannot be obtained and may not be in the monitoring whitelist");
                return -1L;
        }
    }

    public static void dumpThreadInfo(int pid, String cmdline, File file) {
        String taskName;
        String path = "/proc/" + pid + "/task";
        File taskDir = new File(path);
        if (!taskDir.exists() || !taskDir.isDirectory()) {
            Slog.d(TAG, "failed to open proc/" + pid + "/task");
            return;
        }
        File[] taskFiles = taskDir.listFiles();
        StringBuilder sb = new StringBuilder();
        sb.append("Sentinel Monitor Reason: ThreadLeak\n");
        sb.append("Build fingerprint:" + SystemProperties.get(PROPERTIES_BUILD_FINGERPRINT, "unknown") + "\n");
        sb.append("TimeStamp: " + getFormatDateTime(System.currentTimeMillis()) + "\n");
        sb.append("Cmdline： " + cmdline + "\n");
        sb.append("pid: " + String.valueOf(pid) + "\n");
        sb.append("Thread Info:\n");
        sb.append("---Dump thread info pid: " + pid + " ---\n");
        if (taskFiles != null) {
            for (File taskFile : taskFiles) {
                if (taskFile.isDirectory()) {
                    String tid = taskFile.getName();
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(new File(taskFile, "comm")));
                        try {
                            String taskName2 = reader.readLine();
                            if (taskName2 != null && !taskName2.isEmpty()) {
                                taskName = taskName2.replaceAll("\n", "");
                            } else {
                                taskName = "<UNNAMED THREAD>";
                            }
                            sb.append("sysTid = " + tid + "  " + taskName + "\n");
                            reader.close();
                        } catch (Throwable th) {
                            try {
                                reader.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        sb.append("---Dump thread info End---");
        ScoutMemoryUtils.dumpInfoToFile(sb.toString(), file);
    }

    public static String getFdInfo(int pid, File fdPath) {
        String path = "/proc/" + String.valueOf(pid) + "/fd/" + fdPath.getName();
        StringBuilder sb = new StringBuilder();
        sb.append("fdname --> ");
        try {
            Path paths = Paths.get(path, new String[0]);
            Path targetPaths = Files.readSymbolicLink(paths);
            sb.append(targetPaths.toString());
            sb.append(" <--");
            return sb.toString();
        } catch (Exception e) {
            Slog.w(TAG, "getFdInfo: " + fdPath, e);
            return " <--(unknow fd)";
        }
    }

    public static void dumpFdInfo(int pid, String cmdline, File file) {
        File fdPath = new File("/proc/" + String.valueOf(pid) + "/fd");
        File[] fdList = fdPath.listFiles();
        if (ArrayUtils.isEmpty(fdList)) {
            Slog.w(TAG, "failed to read fdList path=/proc/" + String.valueOf(pid) + "/fdinfo");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Sentinel Monitor Reason: FdLeak\n");
        sb.append("Build fingerprint:" + SystemProperties.get(PROPERTIES_BUILD_FINGERPRINT, "unknown") + "\n");
        sb.append("TimeStamp: " + getFormatDateTime(System.currentTimeMillis()) + "\n");
        sb.append("Cmdline： " + cmdline + "\n");
        sb.append("pid: " + String.valueOf(pid) + "\n");
        sb.append("Fd Info:\n");
        sb.append("---Dump Fd info pid: " + pid + " ---\n");
        for (File fd : fdList) {
            fd.getAbsolutePath();
            String fdinfo = getFdInfo(pid, fd);
            sb.append(fdinfo);
        }
        sb.append("---Dump Fd info END---\n");
        ScoutMemoryUtils.dumpInfoToFile(sb.toString(), file);
    }

    public static String dumpProcSmaps(int pid) {
        String path = "/proc/" + String.valueOf(pid) + "/smaps";
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line + "\n");
                } finally {
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Slog.d(TAG, "read failed: " + path + e);
        }
        return sb.toString();
    }

    public static void dumpRssInfo(int pid, long rssszie, String cmdline, File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sentinel Monitor Reason: RssLeak\n");
        sb.append("Build fingerprint:" + SystemProperties.get(PROPERTIES_BUILD_FINGERPRINT, "unknown") + "\n");
        sb.append("TimeStamp: " + getFormatDateTime(System.currentTimeMillis()) + "\n");
        sb.append("Cmdline： " + cmdline + "\n");
        sb.append("pid: " + String.valueOf(pid) + "\n");
        sb.append("RSS: " + rssszie + "\n");
        sb.append("---Dump smaps info pid" + pid + " ---\n");
        sb.append(dumpProcSmaps(pid));
        ScoutMemoryUtils.dumpInfoToFile(sb.toString(), file);
    }

    public static File getExceptionPath(int pid, String cmdline, String type) {
        try {
            File sentinelDir = new File(MIUI_SENTINEL_DIR);
            if (!sentinelDir.exists()) {
                if (!sentinelDir.mkdirs()) {
                    Slog.e(TAG, "cannot create sentinel Dir", new Throwable());
                }
                FileUtils.setPermissions(sentinelDir, 508, -1, -1);
            }
            String dirSuffix = getFormatDateTime(System.currentTimeMillis());
            File exceptionDir = new File(sentinelDir, type);
            if (!exceptionDir.exists()) {
                if (!exceptionDir.mkdirs()) {
                    Slog.e(TAG, "cannot create exceptionDir", new Throwable());
                }
                FileUtils.setPermissions(exceptionDir, 508, -1, -1);
            }
            String filename = type + d.h + cmdline + d.h + String.valueOf(pid) + d.h + dirSuffix + ".txt";
            File leakfile = new File(exceptionDir, filename);
            if (!leakfile.exists()) {
                if (!leakfile.createNewFile()) {
                    Slog.e(TAG, "cannot create leakfile", new Throwable());
                }
                FileUtils.setPermissions(leakfile.getAbsolutePath(), 508, -1, -1);
            }
            return leakfile;
        } catch (IOException e) {
            Slog.w(TAG, "Unable get leakfile : ", e);
            return null;
        }
    }
}
