package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Process;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.notification.BarrageListenerServiceStub;
import com.android.server.wm.WindowProcessUtils;
import com.miui.app.AppOpsServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import miui.content.pm.PreloadedAppPolicy;
import miui.io.IoUtils;
import miui.process.ProcessManagerNative;
import miui.security.SecurityManagerInternal;

/* loaded from: classes.dex */
public class AutoStartManagerServiceStubImpl implements AutoStartManagerServiceStub {
    private static final boolean ENABLE_SIGSTOP_KILL = SystemProperties.getBoolean("persist.proc.enable_sigstop", true);
    private static final String TAG = "AutoStartManagerServiceStubImpl";
    private static HashMap<String, String> startServiceWhiteList;
    private SecurityManagerInternal mSecurityInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AutoStartManagerServiceStubImpl> {

        /* compiled from: AutoStartManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AutoStartManagerServiceStubImpl INSTANCE = new AutoStartManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AutoStartManagerServiceStubImpl m459provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AutoStartManagerServiceStubImpl m458provideNewInstance() {
            return new AutoStartManagerServiceStubImpl();
        }
    }

    static {
        HashMap<String, String> hashMap = new HashMap<>();
        startServiceWhiteList = hashMap;
        hashMap.put("com.miui.huanji", "com.miui.huanji.parsebak.RestoreXSpaceService");
    }

    public void signalStopProcessesLocked(ArrayList<Pair<ProcessRecord, Boolean>> procs, boolean allowRestart, final String packageName, int uid) {
        if (ENABLE_SIGSTOP_KILL) {
            ActivityManagerService ams = ActivityManager.getService();
            if (allowRestart && canRestartServiceLocked(packageName, uid, "AutoStartManagerServiceStubImpl#signalStopProcessesLocked")) {
                return;
            }
            final ArrayList<KillProcessInfo> tmpProcs = new ArrayList<>();
            Iterator<Pair<ProcessRecord, Boolean>> it = procs.iterator();
            while (it.hasNext()) {
                Pair<ProcessRecord, Boolean> proc = it.next();
                tmpProcs.add(new KillProcessInfo((ProcessRecord) proc.first));
            }
            sendSignalToProcessLocked(tmpProcs, packageName, 19, false);
            ams.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.AutoStartManagerServiceStubImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    AutoStartManagerServiceStubImpl.this.sendSignalToProcessLocked(tmpProcs, packageName, 18, true);
                }
            }, 500L);
        }
    }

    void sendSignalToProcessLocked(List<KillProcessInfo> procs, String packageName, int signal, boolean needKillAgain) {
        HashMap<Integer, Pair<Integer, String>> allUncoverdPids = searchAllUncoverdProcsPid(procs);
        for (KillProcessInfo proc : procs) {
            Slog.d(TAG, "prepare force stop p:" + proc.pid + " s: " + signal);
            Process.sendSignal(proc.pid, signal);
            allUncoverdPids.remove(Integer.valueOf(proc.pid));
        }
        for (Integer pid : allUncoverdPids.keySet()) {
            Slog.d(TAG, "prepare force stop native p:" + pid + " s: " + signal);
            Process.sendSignal(pid.intValue(), signal);
            if (!needKillAgain) {
                trackAppBehavior(34, packageName, (String) allUncoverdPids.get(pid).second);
            }
        }
        if (needKillAgain) {
            for (KillProcessInfo p : procs) {
                int pid2 = p.pid;
                int uid = p.uid;
                if (pid2 > 0 && !ProcessUtils.isDiedProcess(uid, pid2)) {
                    EventLog.writeEvent(30023, Integer.valueOf(uid), Integer.valueOf(pid2), p.processName, 1001, "Kill Again");
                    Process.killProcessQuiet(pid2);
                    Process.killProcessGroup(uid, pid2);
                }
            }
        }
    }

    private HashMap<Integer, Pair<Integer, String>> searchAllUncoverdProcsPid(List<KillProcessInfo> procs) {
        HashMap<Integer, Pair<Integer, String>> resPids = new HashMap<>();
        for (KillProcessInfo p : procs) {
            Pair<Integer, String> procInfo = new Pair<>(Integer.valueOf(p.uid), p.processName);
            List<Integer> natiiveProcs = searchNativeProc(p.uid, p.pid);
            for (Integer nPid : natiiveProcs) {
                resPids.put(nPid, procInfo);
            }
        }
        return resPids;
    }

    public static List<Integer> searchNativeProc(int uid, int pid) {
        BufferedReader reader = null;
        List<Integer> nativeProcs = new ArrayList<>();
        try {
            try {
                String fileName = ProcessManagerNative.getCgroupFilePath(uid, pid);
                reader = new BufferedReader(new FileReader(new File(fileName)));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    int proc = Integer.parseInt(line);
                    if (proc > 0 && pid != proc) {
                        nativeProcs.add(Integer.valueOf(proc));
                    }
                }
            } catch (IOException e) {
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return nativeProcs;
        } finally {
            IoUtils.closeQuietly(reader);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class KillProcessInfo {
        int pid;
        ProcessRecord proc;
        String processName;
        int uid;

        KillProcessInfo(ProcessRecord proc) {
            this.uid = proc.uid;
            this.pid = proc.mPid;
            this.processName = proc.processName;
        }
    }

    private void trackAppBehavior(int behavior, String pkg, String info) {
        if (this.mSecurityInternal == null) {
            this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        SecurityManagerInternal securityManagerInternal = this.mSecurityInternal;
        if (securityManagerInternal != null) {
            securityManagerInternal.recordAppBehaviorAsync(behavior, pkg, 1L, info);
        }
    }

    public boolean isAllowStartService(Context context, Intent service, int userId) {
        try {
            String packageName = service.getComponent().getPackageName();
            boolean isAllow = BarrageListenerServiceStub.getInstance().isAllowStartBarrage(packageName);
            if (!isAllow) {
                Log.d(TAG, "MiBarrage: Disallowing to start service {" + packageName + "}");
                return false;
            }
            IPackageManager packageManager = AppGlobals.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0L, userId);
            if (applicationInfo == null) {
                return true;
            }
            int uid = applicationInfo.uid;
            return isAllowStartService(context, service, userId, uid);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean isAllowStartService(Context context, Intent service, int userId, int uid) {
        try {
            AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
            if (aom == null) {
                return true;
            }
            ActivityManagerService ams = ActivityManager.getService();
            ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, (String) null, FormatBytesUtil.KB, userId);
            ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
            if (sInfo != null && (rInfo.serviceInfo.applicationInfo.flags & 1) == 0 && !PreloadedAppPolicy.isProtectedDataApp(context, rInfo.serviceInfo.applicationInfo.packageName, 0)) {
                String packageName = service.getComponent().getPackageName();
                boolean isRunning = WindowProcessUtils.isPackageRunning(ams.mActivityTaskManager, packageName, sInfo.processName, uid);
                if (!isRunning) {
                    int mode = aom.noteOpNoThrow(10008, uid, packageName, (String) null, "AutoStartManagerServiceStubImpl#isAllowStartService");
                    try {
                        trackAppBehavior(6, packageName, service.getComponent().flattenToShortString());
                        if (mode == 0) {
                            return true;
                        }
                        try {
                        } catch (Exception e) {
                            e = e;
                            e.printStackTrace();
                            return true;
                        }
                        try {
                            Slog.i(TAG, "MIUILOG- Reject service :" + service + " userId : " + userId + " uid : " + uid);
                            return false;
                        } catch (Exception e2) {
                            e = e2;
                            e.printStackTrace();
                            return true;
                        }
                    } catch (Exception e3) {
                        e = e3;
                        e.printStackTrace();
                        return true;
                    }
                }
                return true;
            }
            return true;
        } catch (Exception e4) {
            e = e4;
        }
    }

    public boolean canRestartServiceLocked(String packageName, int uid, String message) {
        return canRestartServiceLocked(packageName, uid, message, null, false);
    }

    public boolean canRestartServiceLocked(String packageName, int uid, String message, ComponentName component, boolean isNote) {
        int mode;
        if (((AppOpsServiceInternal) LocalServices.getService(AppOpsServiceInternal.class)).isTestSuitSpecialIgnore(packageName)) {
            return true;
        }
        if (component != null && startServiceWhiteList.containsKey(packageName) && startServiceWhiteList.get(packageName).equals(component.getClassName())) {
            return true;
        }
        AppOpsManager appOpsManager = (AppOpsManager) ActivityThread.currentApplication().getSystemService(AppOpsManager.class);
        if (isNote) {
            mode = appOpsManager.noteOpNoThrow(10008, uid, packageName, (String) null, message);
            if (component != null) {
                trackAppBehavior(6, packageName, component.flattenToShortString());
            }
        } else {
            mode = appOpsManager.checkOpNoThrow(10008, uid, packageName);
        }
        if (mode == 0 || ProcessManagerInternal.checkCtsProcess(packageName)) {
            return true;
        }
        if (isNote && component != null) {
            Slog.i(TAG, "MIUILOG- Reject RestartService service :" + component + " uid : " + uid);
            return false;
        }
        return false;
    }

    private static boolean isAllowStartServiceByUid(Context context, Intent service, int uid) {
        AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
        if (aom == null) {
            return true;
        }
        int mode = aom.checkOpNoThrow(10008, uid, service.getComponent().getPackageName());
        if (mode == 0) {
            return true;
        }
        return false;
    }
}
