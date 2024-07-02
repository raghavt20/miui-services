package com.android.server.am;

import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.FileUtils;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastPrintWriter;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.AnrEvent;
import miui.os.DeviceFeature;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ProcessRecordImpl implements ProcessRecordStub {
    private static final long MEM_THRESHOLD_IN_WHITE_LIST = 71680;
    private static final String SYSTEM_SERVER = "system_server";
    private static final String TAG = "ProcessRecordInjector";
    private static final Object sLock = new Object();
    private static volatile ProcessManagerInternal sProcessManagerInternal = null;
    private static final SparseArray<Map<String, AppPss>> sAppPssUserMap = new SparseArray<>();
    private final String LOG_DIR = "/data/miuilog/stability/scout";
    private final String APP_LOG_DIR = "/data/miuilog/stability/scout/app";
    private final String SYSTEM_LOG_DIR = "/data/miuilog/stability/scout/sys";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessRecordImpl> {

        /* compiled from: ProcessRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessRecordImpl INSTANCE = new ProcessRecordImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ProcessRecordImpl m668provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ProcessRecordImpl m667provideNewInstance() {
            return new ProcessRecordImpl();
        }
    }

    ProcessRecordImpl() {
    }

    private static ProcessManagerInternal getProcessManagerInternal() {
        if (sProcessManagerInternal == null) {
            synchronized (ProcessRecordImpl.class) {
                if (sProcessManagerInternal == null) {
                    sProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
                }
            }
        }
        return sProcessManagerInternal;
    }

    public static void addAppPssIfNeeded(ProcessManagerInternal pms, ProcessRecord app) {
        PackageInfo pi;
        String pkn = app.info.packageName;
        if ("android".equals(pkn)) {
            return;
        }
        long pss = ProcessUtils.getPackageLastPss(pkn, app.userId);
        synchronized (sLock) {
            Map<String, AppPss> appPssMap = sAppPssUserMap.get(app.userId);
            if (pss >= MEM_THRESHOLD_IN_WHITE_LIST && (appPssMap == null || appPssMap.get(pkn) == null)) {
                try {
                    PackageInfo pi2 = AppGlobals.getPackageManager().getPackageInfo(pkn, 0L, app.userId);
                    pi = pi2;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    pi = null;
                }
                String version = (pi == null || pi.versionName == null) ? "unknown" : pi.versionName;
                AppPss appPss = new AppPss(pkn, pss, version, app.userId);
                if (appPssMap == null) {
                    appPssMap = new HashMap();
                    sAppPssUserMap.put(app.userId, appPssMap);
                }
                appPssMap.put(pkn, appPss);
            }
        }
    }

    public void reportAppPss() {
        final Map<String, AppPss> map = new HashMap<>();
        synchronized (sLock) {
            SparseArray<Map<String, AppPss>> sparseArray = sAppPssUserMap;
            if (sparseArray.size() > 0) {
                int size = sparseArray.size();
                for (int i = 0; i < size; i++) {
                    Map<? extends String, ? extends AppPss> valueAt = sAppPssUserMap.valueAt(i);
                    if (valueAt != null) {
                        map.putAll(valueAt);
                    }
                }
                sAppPssUserMap.clear();
            }
        }
        if (map.size() == 0) {
            return;
        }
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessRecordImpl.1
            @Override // java.lang.Runnable
            public void run() {
                List<String> jsons = new ArrayList<>();
                Set<String> pkns = map.keySet();
                for (String pkn : pkns) {
                    AppPss appPss = (AppPss) map.get(pkn);
                    if (appPss != null) {
                        JSONObject object = new JSONObject();
                        try {
                            object.put("packageName", appPss.pkn);
                            object.put("totalPss", appPss.pss);
                            object.put("versionName", appPss.version);
                            object.put("model", Build.MODEL);
                            object.put("userId", appPss.user);
                            jsons.add(object.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (jsons.size() > 0) {
                    MQSEventManagerDelegate.getInstance().reportEventsV2("appPss", jsons, "mqs_whiteapp_lowmem_monitor_63691000", false);
                }
            }
        });
    }

    /* loaded from: classes.dex */
    static final class AppPss {
        static final String MODEL = "model";
        static final String PACKAGE_NAME = "packageName";
        static final String TOTAL_PSS = "totalPss";
        static final String USER_ID = "userId";
        static final String VERSION_NAME = "versionName";
        String pkn;
        String pss;
        String user;
        String version;

        AppPss(String pkn, long pss, String version, int user) {
            this.pkn = pkn;
            this.pss = String.valueOf(pss);
            this.version = version;
            this.user = String.valueOf(user);
        }
    }

    public void onANR(ActivityManagerService ams, ProcessRecord process, String activityShortComponentName, String parentShortCompontnName, String subject, String report, File logFile, ApplicationErrorReport.CrashInfo crashInfo, String headline, ScoutAnrInfo anrInfo) {
        if (anrInfo != null) {
            anrInfo.setBinderTransInfo(anrInfo.getBinderTransInfo());
            reportANR(ams, IProcessPolicy.REASON_ANR, process, process.processName, activityShortComponentName, parentShortCompontnName, subject, report, logFile, crashInfo, headline, anrInfo);
        }
    }

    private void reportANR(ActivityManagerService ams, String eventType, ProcessRecord process, String processName, String activityShortComponentName, String parentShortCompontnName, String subject, String report, File logFile, ApplicationErrorReport.CrashInfo crashInfo, String headline, ScoutAnrInfo anrInfo) {
        AnrEvent event = new AnrEvent();
        event.setPid(anrInfo.getpid());
        event.setUid(anrInfo.getuid());
        event.setProcessName(process.mPid == ActivityManagerService.MY_PID ? SYSTEM_SERVER : processName);
        if (SYSTEM_SERVER.equals(event.getProcessName())) {
            event.setPackageName(SYSTEM_SERVER);
        } else {
            event.setPackageName(TextUtils.isEmpty(process.info.packageName) ? processName : process.info.packageName);
        }
        event.setTimeStamp(anrInfo.getTimeStamp());
        event.setReason(report);
        event.setCpuInfo(subject);
        event.setBgAnr(anrInfo.getBgAnr());
        event.setBlockSystemState(anrInfo.getBlockSystemState());
        event.setBinderTransactionInfo(anrInfo.getBinderTransInfo());
        if (logFile != null && logFile.exists()) {
            event.setLogName(logFile.getAbsolutePath());
        }
        File lastAnrStateFile = createLastAnrFile(event, logFile);
        if (lastAnrStateFile != null && lastAnrStateFile.exists()) {
            saveLastAnrState(ams, lastAnrStateFile);
            List<String> filesNames = new ArrayList<>();
            filesNames.add(lastAnrStateFile.getAbsolutePath());
            Slog.i(TAG, " add extra file: " + lastAnrStateFile.getAbsolutePath());
            event.setExtraFiles(filesNames);
        }
        if (activityShortComponentName != null) {
            event.setTargetActivity(activityShortComponentName);
        }
        if (parentShortCompontnName != null) {
            event.setParent(parentShortCompontnName);
        }
        MQSEventManagerDelegate.getInstance().reportAnrEvent(event);
    }

    private File createLastAnrFile(AnrEvent event, File logFile) {
        if (logFile == null) {
            return null;
        }
        File lastAnrStateFile = null;
        try {
            String lastAnrFileName = logFile.getName() + "-" + event.getProcessName() + "-" + event.getPid() + "-lastAnrState.txt";
            if (SYSTEM_SERVER.equals(event.getProcessName())) {
                lastAnrStateFile = new File("/data/miuilog/stability/scout/sys", lastAnrFileName);
            } else {
                lastAnrStateFile = new File("/data/miuilog/stability/scout/app", lastAnrFileName);
            }
            if (lastAnrStateFile.createNewFile()) {
                FileUtils.setPermissions(lastAnrStateFile.getAbsolutePath(), 509, -1, -1);
            }
        } catch (IOException e) {
            Slog.w(TAG, "Exception creating lastAnrState dump file" + e.toString());
        }
        return lastAnrStateFile;
    }

    private void saveLastAnrState(ActivityManagerService ams, File file) {
        if (ams == null || !file.exists()) {
            return;
        }
        try {
            FileOutputStream fout = new FileOutputStream(file, true);
            try {
                FastPrintWriter pw = new FastPrintWriter(fout);
                pw.println("\n");
                ams.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
                pw.println("\n");
                ams.mWindowManager.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
                pw.flush();
                fout.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "saveLastAnrState error ", e);
        }
    }

    public boolean skipAppErrorDialog(ProcessRecord app) {
        return (app == null || app.info == null || app.getPid() == Process.myPid() || ((!DeviceFeature.IS_SUBSCREEN_DEVICE || !"com.xiaomi.misubscreenui".equals(app.info.packageName)) && !ScoutUtils.isLibraryTest())) ? false : true;
    }

    public void scoutAppUpdateAnrInfo(String tag, ProcessRecord process, ScoutAnrInfo anrInfo) {
        anrInfo.setPid(process.getPid());
        anrInfo.setuid(process.getStartUid());
        anrInfo.setTimeStamp(System.currentTimeMillis());
    }

    public void scoutAppAddBinderCallChainNativePids(String tag, ArrayList<Integer> nativePids, ArrayList<Integer> binderCallChainNativePids) {
        if (binderCallChainNativePids.size() > 0) {
            Iterator<Integer> it = binderCallChainNativePids.iterator();
            while (it.hasNext()) {
                int nativePid = it.next().intValue();
                if (!nativePids.contains(Integer.valueOf(nativePid))) {
                    nativePids.add(Integer.valueOf(nativePid));
                }
            }
        }
    }

    public void scoutAppCheckBinderCallChain(String tag, int Pid, int systemPid, String annotation, ArrayList<Integer> firstPids, ArrayList<Integer> nativePids, ScoutAnrInfo anrInfo) {
        ArrayList<Integer> scoutJavaPids = new ArrayList<>(5);
        ArrayList<Integer> scoutNativePids = new ArrayList<>(5);
        boolean z = true;
        ScoutHelper.ScoutBinderInfo scoutBinderInfo = new ScoutHelper.ScoutBinderInfo(Pid, systemPid, 1, "MIUIScout ANR");
        scoutJavaPids.add(Integer.valueOf(Pid));
        boolean isBlockSystem = ScoutHelper.checkBinderCallPidList(Pid, scoutBinderInfo, scoutJavaPids, scoutNativePids);
        if (!scoutJavaPids.contains(Integer.valueOf(systemPid)) && annotation.contains("Broadcast")) {
            if (!ScoutHelper.checkAsyncBinderCallPidList(Pid, systemPid, scoutBinderInfo, scoutJavaPids, scoutNativePids) && !isBlockSystem) {
                z = false;
            }
            isBlockSystem = z;
        }
        ScoutHelper.printfProcBinderInfo(Pid, "MIUIScout ANR");
        ScoutHelper.printfProcBinderInfo(systemPid, "MIUIScout ANR");
        anrInfo.setBinderTransInfo(scoutBinderInfo.getBinderTransInfo() + "\n" + scoutBinderInfo.getProcInfo());
        anrInfo.setDThreadState(scoutBinderInfo.getDThreadState());
        if (scoutJavaPids.size() > 0) {
            Iterator<Integer> it = scoutJavaPids.iterator();
            while (it.hasNext()) {
                int javaPid = it.next().intValue();
                if (!firstPids.contains(Integer.valueOf(javaPid))) {
                    firstPids.add(Integer.valueOf(javaPid));
                    Slog.d(tag, "Dump Trace: add java proc " + javaPid);
                }
            }
        }
        if (scoutNativePids.size() > 0) {
            Iterator<Integer> it2 = scoutNativePids.iterator();
            while (it2.hasNext()) {
                int nativePid = it2.next().intValue();
                if (!nativePids.contains(Integer.valueOf(nativePid))) {
                    nativePids.add(Integer.valueOf(nativePid));
                    Slog.d(tag, "Dump Trace: add java proc " + nativePid);
                }
            }
        }
        anrInfo.setBlockSystemState(isBlockSystem);
    }

    public void scoutAppCheckDumpKernelTrace(String tag, ScoutAnrInfo anrInfo) {
        if (ScoutHelper.SYSRQ_ANR_D_THREAD && anrInfo.getDThreadState()) {
            ScoutHelper.doSysRqInterface('w');
            ScoutHelper.doSysRqInterface('l');
            if (ScoutHelper.PANIC_ANR_D_THREAD && ScoutHelper.isDebugpolicyed(tag)) {
                SystemClock.sleep(3000L);
                Slog.e(tag, "Trigge Panic Crash when anr Process has D state thread");
                ScoutHelper.doSysRqInterface('c');
            }
        }
    }

    public void dumpPeriodHistoryMessage(ProcessRecord process, long anrTime, int duration, boolean isSystemInputAnr) {
        try {
            if (process.getThread() == null) {
                Slog.w(TAG, "Can't dumpPeriodHistoryMessage because of null IApplicationThread");
            } else if (!isSystemInputAnr) {
                process.getThread().dumpPeriodHistoryMessage(anrTime, duration);
            } else {
                ScoutHelper.dumpUithreadPeriodHistoryMessage(anrTime, duration);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to dumpPeriodHistoryMessage after ANR", e);
        }
    }
}
