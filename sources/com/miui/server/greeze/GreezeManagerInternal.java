package com.miui.server.greeze;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import com.android.server.LocalServices;
import java.util.List;
import miui.greeze.IGreezeCallback;

/* loaded from: classes.dex */
public abstract class GreezeManagerInternal {
    public static final int BINDER_STATE_IN_BUSY = 1;
    public static final int BINDER_STATE_IN_IDLE = 0;
    public static final int BINDER_STATE_IN_TRANSACTION = 4;
    public static final int BINDER_STATE_PROC_IN_BUSY = 3;
    public static final int BINDER_STATE_THREAD_IN_BUSY = 2;
    public static int GREEZER_MODULE_UNKNOWN = 0;
    public static int GREEZER_MODULE_POWER = 1;
    public static int GREEZER_MODULE_PERFORMANCE = 2;
    public static int GREEZER_MODULE_GAME = 3;
    public static int GREEZER_MODULE_PRELOAD = 4;
    public static int GREEZER_MODULE_ALL = 9999;
    private static GreezeManagerInternal sInstance = null;

    public abstract boolean checkAurogonIntentDenyList(String str);

    public abstract void finishLaunchMode(String str, int i);

    public abstract boolean freezePid(int i);

    public abstract boolean freezePid(int i, int i2);

    public abstract List<Integer> freezePids(int[] iArr, long j, int i, String str);

    public abstract List<Integer> freezeUids(int[] iArr, long j, int i, String str, boolean z);

    public abstract int[] getFrozenPids(int i);

    public abstract int[] getFrozenUids(int i);

    public abstract long getLastThawedTime(int i, int i2);

    public abstract void handleAppZygoteStart(ApplicationInfo applicationInfo, boolean z);

    public abstract boolean isNeedCachedBroadcast(Intent intent, int i, String str, boolean z);

    public abstract boolean isRestrictBackgroundAction(String str, int i, String str2, int i2, String str3);

    public abstract boolean isRestrictReceiver(Intent intent, int i, String str, int i2, String str2);

    public abstract boolean isUidFrozen(int i);

    public abstract void notifyBackup(int i, boolean z);

    public abstract void notifyDumpAllInfo();

    public abstract void notifyDumpAppInfo(int i, int i2);

    public abstract void notifyExcuteServices(int i);

    public abstract void notifyFreeformModeFocus(String str, int i);

    public abstract void notifyMovetoFront(int i, boolean z);

    public abstract void notifyMultitaskLaunch(int i, String str);

    public abstract void notifyResumeTopActivity(int i, String str, boolean z);

    public abstract void queryBinderState(int i);

    public abstract boolean registerCallback(IGreezeCallback iGreezeCallback, int i) throws RemoteException;

    public abstract boolean thawPid(int i);

    public abstract boolean thawPid(int i, int i2);

    public abstract List<Integer> thawPids(int[] iArr, int i, String str);

    public abstract boolean thawUid(int i, int i2, String str);

    public abstract List<Integer> thawUids(int[] iArr, int i, String str);

    public abstract void triggerLaunchMode(String str, int i);

    public abstract void updateOrderBCStatus(String str, int i, boolean z, boolean z2);

    public static GreezeManagerInternal getInstance() {
        if (sInstance == null) {
            sInstance = (GreezeManagerInternal) LocalServices.getService(GreezeManagerInternal.class);
        }
        return sInstance;
    }
}
