package com.miui.server.stability;

import android.os.FileUtils;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.server.ScoutHelper;
import com.miui.server.AccessController;
import com.xiaomi.abtest.d.d;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToLongFunction;
import libcore.io.IoUtils;
import miui.mqsas.scout.ScoutUtils;

/* loaded from: classes.dex */
public class ScoutMemoryUtils {
    public static final int DIR_DMABUF_ID = 1;
    public static final int DIR_GPU_MEMORY_ID = 2;
    public static final int DIR_PROCS_MEMORY_ID = 3;
    private static final String FILE_DUMP_SUFFIX = "_memory_info";
    public static final String FILE_KGSL = "/sys/class/kgsl/kgsl/page_alloc";
    public static final String FILE_MALI = "/proc/mtk_mali/gpu_memory";
    public static final int GPU_TYPE_KGSL = 1;
    public static final int GPU_TYPE_MTK_MALI = 2;
    public static final int GPU_TYPE_UNKNOW = 0;
    private static final int MAX_MEMLEAK_FILE = 2;
    private static final String MEMLEAK = "memleak";
    private static final String PROC_MEMINFO = "/proc/meminfo";
    private static final String PROC_SLABINFO = "/proc/slabinfo";
    private static final String PROC_VMALLOCINFO = "/proc/vmallocinfo";
    private static final String PROC_VMSTAT = "/proc/vmstat";
    private static final String PROC_ZONEINFO = "/proc/zoneinfo";
    private static final String TAG = "ScoutMemoryUtils";
    public static final HashMap<Integer, String> dirMap = new HashMap<Integer, String>() { // from class: com.miui.server.stability.ScoutMemoryUtils.1
        {
            put(1, "dmabuf");
            put(2, "gpu");
            put(3, "procs");
        }
    };
    public static final Set<String> skipIonProcList = new HashSet(Arrays.asList("/system/bin/surfaceflinger", "/system/bin/cameraserver", AccessController.PACKAGE_CAMERA, "/vendor/bin/hw/vendor.qti.hardware.display.composer-service", "/vendor/bin/hw/android.hardware.graphics.composer@2.3-service", "/vendor/bin/hw/vendor.qti.camera.provider@2.7-service_64", "/vendor/bin/hw/android.hardware.camera.provider@2.4-service_64", "/vendor/bin/hw/camerahalserver", "/vendor/bin/hw/vendor.qti.camera.provider-service_64", "/vendor/bin/hw/android.hardware.graphics.composer@2.1-service"));
    public static final Set<String> gpuMemoryWhiteList = new HashSet(Arrays.asList("com.antutu.benchmark.full:era", "com.antutu.benchmark.full:unity"));
    private static Method sGetTagMethod = null;
    private static Method sGetTagTypeMethod = null;
    private static Method sGetTagValueMethod = null;

    private static File getMemLeakDir(int dirId) {
        HashMap<Integer, String> hashMap = dirMap;
        if (!hashMap.containsKey(Integer.valueOf(dirId))) {
            Slog.e(TAG, "invalid dir id : " + dirId);
            return null;
        }
        File stDir = new File(ScoutHelper.FILE_DIR_STABILITY);
        if (!stDir.exists()) {
            Slog.e(TAG, "stability log dir isn't exists");
            return null;
        }
        File memleakDir = new File(stDir, MEMLEAK);
        if (!memleakDir.exists()) {
            if (!memleakDir.mkdirs()) {
                Slog.e(TAG, "Cannot create memleak dir");
                return null;
            }
            FileUtils.setPermissions(memleakDir.toString(), 511, -1, -1);
            Slog.e(TAG, "mkdir memleak dir");
        }
        File exDir = new File(memleakDir, hashMap.get(Integer.valueOf(dirId)));
        if (!exDir.exists()) {
            if (!exDir.mkdirs()) {
                Slog.e(TAG, "Cannot create memleak dir " + hashMap.get(Integer.valueOf(dirId)));
                return null;
            }
            FileUtils.setPermissions(exDir.toString(), 511, -1, -1);
        }
        return exDir;
    }

    public static File getExceptionFile(String reason, int dirId) {
        File memleakDir = getMemLeakDir(dirId);
        if (memleakDir == null) {
            return null;
        }
        SimpleDateFormat memleakFileDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        String formattedDate = memleakFileDateFormat.format(new Date());
        File fname = new File(memleakDir, reason + FILE_DUMP_SUFFIX + d.h + formattedDate + ".txt");
        return fname;
    }

    public static void deleteOldKgslDir() {
        File stDir = new File(ScoutHelper.FILE_DIR_STABILITY);
        if (!stDir.exists()) {
            Slog.e(TAG, "stability log dir isn't exists");
            return;
        }
        File memleakDir = new File(stDir, MEMLEAK);
        if (!memleakDir.exists()) {
            Slog.e(TAG, "memleak dir isn't exists");
            return;
        }
        File kgslDir = new File(memleakDir, "kgsl");
        if (kgslDir.exists() && kgslDir.isDirectory()) {
            File[] listFiles = kgslDir.listFiles();
            for (File file : listFiles) {
                file.delete();
            }
            kgslDir.delete();
            Slog.d(TAG, "delete kgsl dir");
        }
    }

    public static void deleteMemLeakFile(int dirId) {
        TreeSet<File> existinglog = new TreeSet<>();
        File memleakDir = getMemLeakDir(dirId);
        if (memleakDir == null) {
            return;
        }
        for (File file : memleakDir.listFiles()) {
            if (file.getName().endsWith(".txt")) {
                if (!file.delete()) {
                    Slog.w(TAG, "Failed to clean up memleak txt file:" + file);
                }
            } else {
                existinglog.add(file);
            }
        }
        if (existinglog.size() >= 2) {
            for (int i = 0; i < 1; i++) {
                existinglog.pollLast();
            }
            Iterator<File> it = existinglog.iterator();
            while (it.hasNext()) {
                File file2 = it.next();
                if (!file2.delete()) {
                    Slog.w(TAG, "Failed to clean up memleak log:" + file2);
                }
            }
        }
    }

    public static void deleteOldFiles(int dirId, int limit) {
        File memleakDir = getMemLeakDir(dirId);
        if (memleakDir == null) {
            return;
        }
        File[] files = memleakDir.listFiles();
        if (files.length <= limit) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(new ToLongFunction() { // from class: com.miui.server.stability.ScoutMemoryUtils$$ExternalSyntheticLambda0
            @Override // java.util.function.ToLongFunction
            public final long applyAsLong(Object obj) {
                return ((File) obj).lastModified();
            }
        }));
        for (int i = 0; i < files.length - limit; i++) {
            if (!files[i].delete()) {
                Slog.w(TAG, "Failed to delete log file: " + files[i]);
            }
        }
    }

    public static void doFsyncZipFile(int dirId) {
        TreeSet<File> existinglog = new TreeSet<>();
        File memleakDir = getMemLeakDir(dirId);
        if (memleakDir == null) {
            return;
        }
        for (File file : memleakDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                existinglog.add(file);
            }
        }
        File fsyncFile = existinglog.pollLast();
        FileDescriptor fd = null;
        try {
            try {
                fd = Os.open(fsyncFile.getAbsolutePath(), OsConstants.O_RDONLY, 0);
                Os.fsync(fd);
                Os.close(fd);
                Slog.w(TAG, "finish fsync file: " + fsyncFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            IoUtils.closeQuietly(fd);
        }
    }

    public static boolean dumpInfoToFile(String info, File file) {
        if (file == null) {
            return false;
        }
        FileDescriptor fd = null;
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(out);
            fastPrintWriter.println(info);
            fastPrintWriter.close();
            FileUtils.setPermissions(file.toString(), 508, -1, -1);
            fd = Os.open(file.getAbsolutePath(), OsConstants.O_RDONLY, 0);
            Os.fsync(fd);
            Os.close(fd);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            IoUtils.closeQuietly(fd);
        }
    }

    public static void dumpInfo(String path, File file) {
        StringBuilder builder = new StringBuilder();
        File readerFile = new File(path);
        builder.append("---Dump " + path + " START---\n");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(readerFile));
            while (true) {
                try {
                    String readInfo = reader.readLine();
                    if (readInfo == null) {
                        break;
                    } else {
                        builder.append(readInfo + "\n");
                    }
                } finally {
                }
            }
            reader.close();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to read thread stat", e);
        }
        builder.append("---Dump " + path + " END---\n\n");
        dumpInfoToFile(builder.toString(), file);
    }

    public static void dumpProcInfo(String info, File file, String reason) {
        StringBuilder builder = new StringBuilder();
        builder.append("---Dump " + reason + " usage info START---\n");
        builder.append(info);
        builder.append("---Dump " + reason + " usage info END---\n\n");
        dumpInfoToFile(builder.toString(), file);
    }

    public static void dumpMemoryInfo(File file) {
        dumpInfo(PROC_MEMINFO, file);
    }

    public static void dumpGpuMemoryInfo(File file, int type) {
        if (type == 1) {
            dumpInfo(FILE_KGSL, file);
        } else if (type == 2) {
            dumpInfo(FILE_MALI, file);
        }
    }

    private static String getFdOwnerImpl(FileDescriptor fd) throws Exception {
        libcore.io.Os os = libcore.io.Os.getDefault();
        if (sGetTagMethod == null) {
            sGetTagMethod = libcore.io.Os.class.getDeclaredMethod("android_fdsan_get_owner_tag", FileDescriptor.class);
            sGetTagTypeMethod = libcore.io.Os.class.getDeclaredMethod("android_fdsan_get_tag_type", Long.TYPE);
            sGetTagValueMethod = libcore.io.Os.class.getDeclaredMethod("android_fdsan_get_tag_value", Long.TYPE);
        }
        long tag = ((Long) sGetTagMethod.invoke(os, fd)).longValue();
        if (tag == 0) {
            return "unowned";
        }
        String type = (String) sGetTagTypeMethod.invoke(os, Long.valueOf(tag));
        long value = ((Long) sGetTagValueMethod.invoke(os, Long.valueOf(tag))).longValue();
        return "owned by " + type + " 0x" + Long.toHexString(value);
    }

    public static String getFdOwner(File fdPath) {
        try {
            int rawFd = Integer.valueOf(fdPath.getName()).intValue();
            try {
                FileDescriptor fd = new FileDescriptor();
                fd.setInt$(rawFd);
                return " (" + getFdOwnerImpl(fd) + ") ";
            } catch (Exception e) {
                Slog.w(TAG, "getFdOwner: " + fdPath, e);
                return "";
            }
        } catch (Exception e2) {
            Slog.w(TAG, "getFdOwner: " + fdPath + " is not a valid fd path", e2);
            return "";
        }
    }

    public static String getFdIno(int pid, File fdPath) {
        String name = "/proc/" + String.valueOf(pid) + "/fdinfo/" + fdPath.getName();
        try {
            BufferedReader fdinfoReader = new BufferedReader(new FileReader(name));
            try {
                StringBuilder builder = new StringBuilder();
                builder.append(" ( fdinfo -->");
                while (true) {
                    String fdinfo = fdinfoReader.readLine();
                    if (fdinfo != null) {
                        builder.append(" " + fdinfo);
                    } else {
                        builder.append(" <--)");
                        String sb = builder.toString();
                        fdinfoReader.close();
                        return sb;
                    }
                }
            } finally {
            }
        } catch (Exception e) {
            Slog.w(TAG, "getFdIno: " + fdPath, e);
            return "(unknow ino: " + name + ")";
        }
    }

    public static void dumpOpenFds(int pid, File file) {
        File fdPath = new File("/proc/" + String.valueOf(pid) + "/fdinfo");
        File[] fds = fdPath.listFiles();
        if (ArrayUtils.isEmpty(fds)) {
            Slog.w(TAG, "failed to read fds! path=/proc/" + String.valueOf(pid) + "/fdinfo");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("---Dump fd info pid: " + pid + ", size: " + fds.length + " START---\n");
        for (File fd : fds) {
            String fd_path = fd.getAbsolutePath();
            String fdinfo = getFdIno(pid, fd);
            if (fdinfo.contains("exp_name")) {
                builder.append(fd_path + " ----> " + getFdOwner(fd) + fdinfo + "\n");
            }
        }
        builder.append("---Dump fd info END---\n\n");
        dumpInfoToFile(builder.toString(), file);
    }

    public static void triggerCrash() {
        ScoutHelper.doSysRqInterface('c');
    }

    public static String captureIonLeakLog(String dmabufInfo, String reason, HashSet<Integer> fdPids) {
        deleteMemLeakFile(1);
        File exectionFile = getExceptionFile(reason, 1);
        dumpProcInfo(dmabufInfo, exectionFile, reason);
        if (fdPids.size() > 0) {
            Iterator<Integer> it = fdPids.iterator();
            while (it.hasNext()) {
                int fdPid = it.next().intValue();
                Slog.w(TAG, "captureIonLeakLog: " + fdPid);
                dumpOpenFds(fdPid, exectionFile);
            }
        }
        ScoutHelper.Action action = new ScoutHelper.Action();
        action.addActionAndParam("dumpsys", DumpSysInfoUtil.SURFACEFLINGER);
        action.addActionAndParam("dumpsys", "gfxinfo");
        action.addActionAndParam("dumpsys", "activity");
        action.addActionAndParam("dumpsys", DumpSysInfoUtil.WINDOW);
        action.addActionAndParam("dumpsys", "meminfo");
        if (ScoutUtils.isLibraryTest()) {
            action.addActionAndParam("logcat", "-b main,system,crash,events");
        }
        if (exectionFile != null) {
            action.addIncludeFile(exectionFile.getAbsolutePath());
        }
        File memleakDir = getMemLeakDir(1);
        if (memleakDir == null) {
            return "";
        }
        String zipPath = ScoutHelper.dumpOfflineLog(reason, action, MEMLEAK, memleakDir.getAbsolutePath() + "/");
        return zipPath;
    }

    public static String captureGpuMemoryLeakLog(String gpuMemoryInfo, String reason, int type) {
        deleteOldKgslDir();
        deleteMemLeakFile(2);
        File exectionFile = getExceptionFile(reason, 2);
        dumpProcInfo(gpuMemoryInfo, exectionFile, reason);
        dumpGpuMemoryInfo(exectionFile, type);
        dumpMemoryInfo(exectionFile);
        ScoutHelper.Action action = new ScoutHelper.Action();
        action.addActionAndParam("dumpsys", "meminfo");
        action.addActionAndParam("dumpsys", DumpSysInfoUtil.SURFACEFLINGER);
        action.addActionAndParam("dumpsys", "gfxinfo");
        if (ScoutUtils.isLibraryTest()) {
            action.addActionAndParam("logcat", "-b main,system,crash,events");
        }
        if (exectionFile != null) {
            action.addIncludeFile(exectionFile.getAbsolutePath());
        }
        File memleakDir = getMemLeakDir(2);
        if (memleakDir == null) {
            return "";
        }
        String zipPath = ScoutHelper.dumpOfflineLog(reason, action, MEMLEAK, memleakDir.getAbsolutePath() + "/");
        return zipPath;
    }
}
