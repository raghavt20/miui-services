package com.miui.server.process;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import com.android.server.LocalServices;
import com.android.server.am.HostingRecord;
import com.android.server.am.IProcessPolicy;
import com.android.server.am.ProcessRecord;
import com.android.server.wm.FgActivityChangedInfo;
import com.android.server.wm.FgWindowChangedInfo;
import java.util.List;
import miui.process.ForegroundInfo;
import miui.process.IMiuiApplicationThread;
import miui.process.RunningProcessInfo;

/* loaded from: classes.dex */
public abstract class ProcessManagerInternal {
    private static ProcessManagerInternal sInstance;

    public abstract boolean checkAppFgServices(int i);

    public abstract void forceStopPackage(ProcessRecord processRecord, String str, boolean z);

    public abstract void forceStopPackage(String str, int i, String str2);

    public abstract List<RunningProcessInfo> getAllRunningProcessInfo();

    public abstract ForegroundInfo getForegroundInfo() throws RemoteException;

    public abstract IMiuiApplicationThread getMiuiApplicationThread(int i);

    public abstract ApplicationInfo getMultiWindowForegroundAppInfoLocked();

    public abstract IProcessPolicy getProcessPolicy();

    public abstract ProcessRecord getProcessRecord(String str);

    public abstract ProcessRecord getProcessRecord(String str, int i);

    public abstract ProcessRecord getProcessRecordByPid(int i);

    public abstract boolean isAllowRestartProcessLock(String str, int i, int i2, String str2, String str3, HostingRecord hostingRecord);

    public abstract boolean isForegroundApp(String str, int i);

    public abstract boolean isInWhiteList(ProcessRecord processRecord, int i, int i2);

    public abstract boolean isLockedApplication(String str, int i) throws RemoteException;

    public abstract boolean isPackageFastBootEnable(String str, int i, boolean z);

    public abstract boolean isTrimMemoryEnable(String str);

    public abstract void killApplication(ProcessRecord processRecord, String str, boolean z);

    public abstract void notifyActivityChanged(ComponentName componentName);

    public abstract void notifyAmsProcessKill(ProcessRecord processRecord, String str);

    public abstract void notifyForegroundInfoChanged(FgActivityChangedInfo fgActivityChangedInfo);

    public abstract void notifyForegroundWindowChanged(FgWindowChangedInfo fgWindowChangedInfo);

    public abstract void notifyLmkProcessKill(int i, int i2, long j, int i3, int i4, int i5, int i6, String str);

    public abstract void notifyProcessDied(ProcessRecord processRecord);

    public abstract void notifyProcessStarted(ProcessRecord processRecord);

    public abstract boolean restartDiedAppOrNot(ProcessRecord processRecord, boolean z, boolean z2, boolean z3);

    public abstract void setProcessMaxAdjLock(int i, ProcessRecord processRecord, int i2, int i3);

    public abstract void setSpeedTestState(boolean z);

    public abstract void trimMemory(ProcessRecord processRecord, boolean z);

    public abstract void updateEnterpriseWhiteList(String str, boolean z);

    public static ProcessManagerInternal getInstance() {
        if (sInstance == null) {
            sInstance = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        }
        return sInstance;
    }

    public static boolean checkCtsProcess(String processName) {
        return processName.startsWith("com.android.cts.") || processName.startsWith("android.app.cts.") || processName.startsWith("com.android.server.cts.") || processName.startsWith("com.android.RemoteDPC") || processName.startsWith("android.camera.cts") || processName.startsWith("android.permission.cts") || processName.startsWith("android.jobscheduler.cts") || processName.startsWith("android.voiceinteraction.cts") || processName.startsWith("android.telecom.cts") || processName.startsWith("com.android.app1") || processName.startsWith("android.app.stubs") || processName.startsWith("android.cts.backup.successnotificationapp") || processName.startsWith("com.android.Delegate") || processName.startsWith("com.android.bedstead.testapp") || processName.startsWith("com.android.RemoteAccountAuthenticator");
    }
}
