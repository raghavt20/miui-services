package com.miui.server.greeze;

import android.app.ActivityManager;
import java.util.Arrays;
import miui.process.RunningProcessInfo;

/* loaded from: classes.dex */
public class RunningProcess {
    int adj;
    boolean hasForegroundActivities;
    boolean hasForegroundServices;
    int pid;
    String[] pkgList;
    int procState;
    String processName;
    int uid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunningProcess(RunningProcessInfo info) {
        this.pid = info.mPid;
        this.uid = info.mUid;
        this.processName = info.mProcessName;
        this.pkgList = info.mPkgList != null ? info.mPkgList : new String[0];
        this.adj = info.mAdj;
        this.procState = info.mProcState;
        this.hasForegroundActivities = info.mHasForegroundActivities;
        this.hasForegroundServices = info.mHasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunningProcess(ActivityManager.RunningAppProcessInfo info) {
        this.pid = info.pid;
        this.uid = info.uid;
        this.processName = info.processName;
        this.pkgList = info.pkgList;
    }

    public String toString() {
        return this.uid + " " + this.pid + " " + this.processName + " " + Arrays.toString(this.pkgList) + " " + this.adj + " " + this.procState + (this.hasForegroundActivities ? " FA" : "") + (this.hasForegroundServices ? " FS" : "");
    }
}
