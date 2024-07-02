package com.android.server.am;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import miui.process.IMiuiApplicationThread;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MiuiApplicationThreadManager {
    private static final String TAG = "ProcessManager";
    private ActivityManagerService mActivityManagerService;
    private SparseArray<IMiuiApplicationThread> mMiuiApplicationThreads = new SparseArray<>();

    public MiuiApplicationThreadManager(ActivityManagerService ams) {
        this.mActivityManagerService = ams;
    }

    public synchronized void addMiuiApplicationThread(IMiuiApplicationThread applicationThread, int pid) {
        this.mMiuiApplicationThreads.put(pid, applicationThread);
        try {
            CallBack callback = new CallBack(pid, applicationThread);
            applicationThread.asBinder().linkToDeath(callback, 0);
        } catch (RemoteException e) {
            Log.w("ProcessManager", "process:" + pid + " is dead");
        }
    }

    public synchronized void removeMiuiApplicationThread(int pid) {
        this.mMiuiApplicationThreads.remove(pid);
    }

    public synchronized IMiuiApplicationThread getMiuiApplicationThread(int pid) {
        return pid != 0 ? this.mMiuiApplicationThreads.get(pid) : null;
    }

    /* loaded from: classes.dex */
    private final class CallBack implements IBinder.DeathRecipient {
        final IMiuiApplicationThread mMiuiApplicationThread;
        final int mPid;

        CallBack(int pid, IMiuiApplicationThread thread) {
            this.mPid = pid;
            this.mMiuiApplicationThread = thread;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiApplicationThreadManager.this.removeMiuiApplicationThread(this.mPid);
        }
    }
}
