package com.android.server.am;

import android.content.pm.ApplicationInfo;
import android.os.Debug;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.camera.CameraOpt;
import com.android.server.net.NetworkManagementServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.greeze.GreezeManagerInternal;
import com.miui.server.process.ProcessManagerInternal;
import com.xiaomi.abtest.d.d;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import miui.mqsas.scout.ScoutUtils;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ProcessListStubImpl implements ProcessListStub {
    private static final String APP_START_TIMEOUT_DUMP_DIR = "/data/miuilog/stability/scout/app/";
    private static final String APP_START_TIMEOUT_DUMP_PREFIX = "app_start_timeout_";
    private static final String KILL_REASON_START_TIMEOUT = "start timeout";
    private static final String TAG = "ProcessListStubImpl";
    private static final boolean ENABLE_MI_EXTRA_FREE = SystemProperties.getBoolean("persist.sys.spc.extra_free_enable", false);
    private static final int DEF_MIN_EXTRA_FREE_KB = SystemProperties.getInt("persist.sys.spc.extra_free_kbytes", 45375);
    private static final boolean ENABLE_MI_SIZE_EXTRA_FREE_GAME_ONLY = SystemProperties.getBoolean("persist.sys.spc.mi_extra_free_game_only", false);
    private static final boolean ENABLE_MI_SIZE_EXTRA_FREE = SystemProperties.getBoolean("persist.sys.spc.mi_extra_free_enable", false);
    ProcessManagerInternal mPmi = null;
    PeriodicCleanerInternalStub mPeriodicCleaner = null;
    private int mLastDisplayWidth = 0;
    private int mLastDisplayHeight = 0;
    private boolean mGameMode = false;
    private int mSystemRenderThreadTid = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessListStubImpl> {

        /* compiled from: ProcessListStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessListStubImpl INSTANCE = new ProcessListStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ProcessListStubImpl m604provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ProcessListStubImpl m603provideNewInstance() {
            return new ProcessListStubImpl();
        }
    }

    public static ProcessListStubImpl getInstance() {
        return (ProcessListStubImpl) MiuiStubUtil.getImpl(ProcessListStub.class);
    }

    public void notifyProcessStarted(ProcessRecord app, int pid) {
        MiuiProcessPolicyManager.getInstance().promoteImportantProcAdj(app);
        ProcessProphetStub.getInstance().reportProcStarted(app, pid);
        CameraOpt.callMethod("notifyProcessStarted", Integer.valueOf(pid), Integer.valueOf(app.info.uid), app.info.packageName, app.processName);
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyProcessStarted(app);
        NetworkManagementServiceStub.getInstance().setPidForPackage(app.info.packageName, pid, app.uid);
        if (this.mPeriodicCleaner == null) {
            PeriodicCleanerInternalStub periodicCleanerInternalStub = (PeriodicCleanerInternalStub) LocalServices.getService(PeriodicCleanerInternalStub.class);
            this.mPeriodicCleaner = periodicCleanerInternalStub;
            if (periodicCleanerInternalStub == null) {
                return;
            }
        }
        this.mPeriodicCleaner.reportStartProcess(app.processName, app.getHostingRecord().getType());
    }

    public void notifyAmsProcessKill(ProcessRecord app, String reason) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyAmsProcessKill(app, reason);
    }

    public void dumpWhenAppStartTimeout(ProcessRecord app) {
        if (!ScoutUtils.isLibraryTest()) {
            return;
        }
        Log.d(TAG, "pid " + app.mPid + " " + app.processName + " stated timeout and its state is " + getProcState(app.mPid));
        String dumpFile = "/data/miuilog/stability/scout/app/app_start_timeout_" + app.processName + d.h + app.mPid + ".trace";
        if (Debug.dumpJavaBacktraceToFileTimeout(app.mPid, dumpFile, 1)) {
            Log.d(TAG, "succeed to dump java trace to " + dumpFile + " because of app started timeout");
            return;
        }
        Log.e(TAG, "fail to dump java trace for " + app.processName + " when it started timeout, try to dump native trace");
        String dumpFile2 = "/data/miuilog/stability/scout/app/app_start_timeout_" + app.processName + d.h + app.mPid + ".native.trace";
        if (Debug.dumpNativeBacktraceToFileTimeout(app.mPid, dumpFile2, 1)) {
            Log.d(TAG, "succeed to dump native trace to " + dumpFile2 + " because of app started timeout");
        } else {
            Log.e(TAG, "fail to dump native trace for " + app.processName);
        }
    }

    public static char getProcState(int pid) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/" + pid + "/stat"));
            try {
                String stat = bufferedReader.readLine();
                char charAt = stat.charAt(stat.indexOf(")") + 2);
                bufferedReader.close();
                return charAt;
            } finally {
            }
        } catch (IOException e) {
            Log.e(TAG, "fail to get proc state of pid " + pid, e);
            return (char) 0;
        }
    }

    public void notifyLmkProcessKill(int uid, int oomScore, long rssInBytes, int freeMemKb, int freeSwapKb, int killReason, int thrashing, String processName) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyLmkProcessKill(uid, oomScore, rssInBytes, freeMemKb, freeSwapKb, killReason, thrashing, processName);
    }

    public void notifyProcessDied(ProcessRecord app) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyProcessDied(app);
    }

    public void setSystemRenderThreadTidLocked(int tid) {
        this.mSystemRenderThreadTid = tid;
    }

    public int getSystemRenderThreadTid() {
        return this.mSystemRenderThreadTid;
    }

    public boolean isNeedTraceProcess(ProcessRecord app) {
        return MiuiProcessPolicyManager.getInstance().isNeedTraceProcess(app);
    }

    public boolean isAllowRestartProcessLock(String processName, int flag, int uid, String pkgName, String callerPackage, HostingRecord hostingRecord) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return true;
            }
        }
        return this.mPmi.isAllowRestartProcessLock(processName, flag, uid, pkgName, callerPackage, hostingRecord);
    }

    public boolean restartDiedAppOrNot(ProcessRecord app, boolean isHomeApp, boolean allowRestart, boolean fromBinderDied) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return allowRestart;
            }
        }
        return this.mPmi.restartDiedAppOrNot(app, isHomeApp, allowRestart, fromBinderDied);
    }

    public int computeExtraFreeKbytes(long totalMemMb, int oldReserve) {
        if (ENABLE_MI_EXTRA_FREE && totalMemMb > 6144) {
            return Math.max(DEF_MIN_EXTRA_FREE_KB, oldReserve);
        }
        return oldReserve;
    }

    public void writePerfEvent(int uid, String procName, int minOomScore, int oomScore, int killReason) {
        File file = new File("/dev/mi_exception_log");
        if (file.exists()) {
            JSONObject perfjson = new JSONObject();
            try {
                perfjson.put("EventID", 902003001);
                perfjson.put("ProcessName", procName);
                perfjson.put("UID", uid);
                perfjson.put("MinOomAdj", minOomScore);
                perfjson.put("OomAdj", oomScore);
                perfjson.put("Reason", killReason);
            } catch (JSONException e) {
                Slog.w(TAG, "error put event");
            }
            FileOutputStream fos = null;
            try {
                try {
                    try {
                        fos = new FileOutputStream(file);
                        fos.write(perfjson.toString().getBytes());
                        fos.close();
                        fos.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        if (fos != null) {
                            fos.close();
                        }
                    }
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e4.printStackTrace();
            }
        }
    }

    public void hookAppZygoteStart(ApplicationInfo info) {
        GreezeManagerInternal.getInstance().handleAppZygoteStart(info, true);
    }

    public boolean needSetMiSizeExtraFree() {
        return ENABLE_MI_SIZE_EXTRA_FREE || (ENABLE_MI_SIZE_EXTRA_FREE_GAME_ONLY && this.mGameMode);
    }

    public boolean needSetMiSizeExtraFreeForGameMode(boolean gameMode) {
        this.mGameMode = gameMode;
        return !ENABLE_MI_SIZE_EXTRA_FREE && ENABLE_MI_SIZE_EXTRA_FREE_GAME_ONLY;
    }

    public void setLastDisplayWidthAndHeight(int lastDisplayWidth, int lastDisplayHeight) {
        this.mLastDisplayWidth = lastDisplayWidth;
        this.mLastDisplayHeight = lastDisplayHeight;
    }

    public int getLastDisplayHeight() {
        return this.mLastDisplayHeight;
    }

    public int getLastDisplayWidth() {
        return this.mLastDisplayWidth;
    }

    public boolean isGameMode() {
        return this.mGameMode;
    }
}
