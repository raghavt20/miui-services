package com.miui.server.stability;

import android.os.FileUtils;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import com.android.server.ScoutHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import miui.mqsas.IMQSNative;

/* loaded from: classes.dex */
public class DumpSysInfoUtil {
    public static final String ACTIVITY = "activity activities";
    public static final String DIR_NAME = "dumpsys_by_power_";
    public static final String DUMPSYS = "dumpsys";
    public static final String DUMPSYS_FILE_NAME = "dumpsys.txt";
    public static final String FILE_DIR_HANGLOG = "hanglog";
    public static final String FILE_DIR_STABILITY = "/data/miuilog/stability/";
    public static final String INPUT = "input";
    public static final String LOGCAT = "logcat";
    public static final String LOGCAT_FILE_NAME = "logcat.txt";
    private static final int MAX_FREEZE_SCREEN_STUCK_FILE = 3;
    private static final String MQSASD = "miui.mqsas.IMQSNative";
    public static final String SURFACEFLINGER = "SurfaceFlinger";
    private static final String TAG = "DumpSysInfoUtil";
    public static final String WINDOW = "window";
    public static final String ZIP_NAME = "Freeze_Screen_Stuck";
    private static IMQSNative mDaemon;
    private static File temporaryDir;

    /* renamed from: -$$Nest$smgetmDaemon, reason: not valid java name */
    static /* bridge */ /* synthetic */ IMQSNative m3440$$Nest$smgetmDaemon() {
        return getmDaemon();
    }

    private static IMQSNative getmDaemon() {
        if (mDaemon == null) {
            IMQSNative asInterface = IMQSNative.Stub.asInterface(ServiceManager.getService(MQSASD));
            mDaemon = asInterface;
            if (asInterface == null) {
                Slog.e(TAG, "mqsasd not available!");
            }
        }
        return mDaemon;
    }

    public static void captureDumpLog() {
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.stability.DumpSysInfoUtil$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DumpSysInfoUtil.lambda$captureDumpLog$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$captureDumpLog$0() {
        try {
            deleteDumpSysFile(ZIP_NAME);
            ScoutHelper.Action action = new ScoutHelper.Action();
            action.addActionAndParam("dumpsys", SURFACEFLINGER);
            action.addActionAndParam("dumpsys", ACTIVITY);
            action.addActionAndParam("dumpsys", WINDOW);
            action.addActionAndParam("dumpsys", INPUT);
            File dumpsysLogPath = getDumpSysFilePath();
            if (dumpsysLogPath != null) {
                ScoutHelper.dumpOfflineLog(ZIP_NAME, action, FILE_DIR_HANGLOG, dumpsysLogPath.getAbsolutePath() + "/");
            }
        } catch (Exception e) {
            Slog.e(TAG, "crash in the captureDumpLog()", e);
        }
    }

    public static void crawlLogsByPower() {
        deleteDumpSysFile(DIR_NAME);
        Thread crawlDumpsysInfo = new Thread(new Runnable() { // from class: com.miui.server.stability.DumpSysInfoUtil.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    String path = DumpSysInfoUtil.createFile(DumpSysInfoUtil.DUMPSYS_FILE_NAME);
                    IMQSNative mClient = DumpSysInfoUtil.m3440$$Nest$smgetmDaemon();
                    if (mClient != null && path != null) {
                        List<String> actions = Arrays.asList("dumpsys", "dumpsys", "dumpsys", "dumpsys");
                        List<String> params = Arrays.asList(DumpSysInfoUtil.SURFACEFLINGER, DumpSysInfoUtil.ACTIVITY, DumpSysInfoUtil.WINDOW, DumpSysInfoUtil.INPUT);
                        mClient.captureLogByRunCommand(actions, params, path, 3);
                        DumpSysInfoUtil.deleteMissFetchByPower(DumpSysInfoUtil.temporaryDir);
                    }
                } catch (Exception e) {
                    Slog.e(DumpSysInfoUtil.TAG, "crash in the crawlDumpsysLogs()", e);
                }
            }
        });
        crawlDumpsysInfo.start();
    }

    private static void crawlLogcat() {
        Thread crawlLocat = new Thread(new Runnable() { // from class: com.miui.server.stability.DumpSysInfoUtil.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    String path = DumpSysInfoUtil.createFile(DumpSysInfoUtil.LOGCAT_FILE_NAME);
                    IMQSNative mClient = DumpSysInfoUtil.m3440$$Nest$smgetmDaemon();
                    if (mClient != null && path != null) {
                        List<String> actions = Arrays.asList("logcat", "logcat");
                        List<String> params = Arrays.asList("-v threadtime -d *:v", "-v threadtime -b events -d *:v");
                        mClient.captureLogByRunCommand(actions, params, path, 3);
                    }
                } catch (Exception e) {
                    Slog.e(DumpSysInfoUtil.TAG, "crash in the crawlLogcat()", e);
                }
            }
        });
        crawlLocat.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void deleteMissFetchByPower(final File filePath) {
        MiuiBgThread.getHandler().postDelayed(new Runnable() { // from class: com.miui.server.stability.DumpSysInfoUtil$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DumpSysInfoUtil.lambda$deleteMissFetchByPower$1(filePath);
            }
        }, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$deleteMissFetchByPower$1(File filePath) {
        if (filePath != null) {
            try {
                if (filePath.exists()) {
                    for (File file : filePath.listFiles()) {
                        file.delete();
                    }
                    filePath.delete();
                }
            } catch (Exception e) {
                Slog.e(TAG, "crash in the deleteMissFetchByPower()", e);
            }
        }
    }

    private static void deleteDumpSysFile(String fileName) {
        try {
            File hanglog = getDumpSysFilePath();
            TreeSet<File> existinglog = new TreeSet<>();
            if (hanglog != null && hanglog.exists()) {
                for (File file : hanglog.listFiles()) {
                    if (file.getName().contains(fileName)) {
                        existinglog.add(file);
                    }
                }
                if (existinglog.size() >= 3) {
                    File deleteFile = existinglog.pollFirst();
                    if (!deleteFile.getName().contains("zip")) {
                        for (File file2 : deleteFile.listFiles()) {
                            file2.delete();
                        }
                    }
                    deleteFile.delete();
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "crash in the deleteDumpSysFile()", e);
        }
    }

    private static File getDumpSysFilePath() {
        try {
            File stabilityLog = new File(FILE_DIR_STABILITY);
            File dumpsyslog = new File(stabilityLog, FILE_DIR_HANGLOG);
            if (!dumpsyslog.exists()) {
                if (!dumpsyslog.mkdirs()) {
                    Slog.e(TAG, "Cannot create dumpsyslog dir");
                    return null;
                }
                FileUtils.setPermissions(dumpsyslog.toString(), 511, -1, -1);
                Slog.d(TAG, "mkdir dumpsyslog dir");
            }
            return dumpsyslog;
        } catch (Exception e) {
            Slog.e(TAG, "crash in the getDumpSysFilePath()", e);
            return null;
        }
    }

    private static synchronized File createDumpsysByPowerPath() {
        synchronized (DumpSysInfoUtil.class) {
            try {
                File file = getDumpSysFilePath();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                File dumpsysPath = new File(file, DIR_NAME + dateFormat.format(new Date()));
                if (file != null && (dumpsysPath.exists() || dumpsysPath.mkdirs())) {
                    Slog.d(TAG, "mkdir dumpsysPath dir");
                    FileUtils.setPermissions(dumpsysPath, 508, -1, -1);
                    temporaryDir = dumpsysPath;
                    return dumpsysPath;
                }
                Slog.e(TAG, "Cannot create dumpsysPath dir");
                temporaryDir = null;
                return null;
            } catch (Exception e) {
                Slog.e(TAG, "crash in the createDumpsysByPowerPath()", e);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String createFile(String mFileName) {
        try {
            File dumpsysLogPath = createDumpsysByPowerPath();
            if (dumpsysLogPath != null) {
                File file = new File(dumpsysLogPath.getAbsolutePath() + "/" + mFileName);
                if (file.createNewFile()) {
                    FileUtils.setPermissions(file, 508, -1, -1);
                    Slog.d(TAG, "create file success");
                    return file.getAbsolutePath();
                }
            }
            Slog.e(TAG, "create file fail");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "crash in the createFile()", e);
            return null;
        }
    }
}
