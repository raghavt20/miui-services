package com.miui.server.smartpower;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.ProcessUtils;
import com.android.server.power.PowerManagerServiceStub;
import com.miui.app.smartpower.SmartPowerSettings;
import com.xiaomi.abtest.BuildConfig;
import java.util.ArrayList;
import java.util.Iterator;
import miui.security.SecurityManagerInternal;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AppWakelockResource extends AppPowerResource {
    private static final int BLOOEAN_DISABLE = 0;
    private static final int BLOOEAN_ENABLE = 1;
    private static final int MSG_SET_WAKELOCK_STATE = 1;
    private Handler mHandler;
    private PackageManagerInternal mPackageManagerInt;
    private SecurityManagerInternal mSecurityManagerInt;
    private final Object mLock = new Object();
    private final ArrayList<WakeLock> mWakeLocks = new ArrayList<>();

    public AppWakelockResource(Context context, Looper looper) {
        this.mHandler = new MyHandler(looper);
        this.mType = 4;
        this.mSecurityManagerInt = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList getActiveUids() {
        ArrayList<Integer> uids = new ArrayList<>();
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                uids.add(Integer.valueOf(wakeLock.mOwnerUid));
            }
        }
        return uids;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                if (wakeLock.mOwnerUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                if (wakeLock.mOwnerPid == pid) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
        if (UserHandle.isApp(uid)) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.arg1 = uid;
            msg.arg2 = 0;
            this.mHandler.sendMessage(msg);
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
        if (UserHandle.isApp(uid)) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.arg1 = uid;
            msg.arg2 = 1;
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setWakeLockState(int uid, boolean enable) {
        try {
            ArraySet<String> targetTags = new ArraySet<>();
            synchronized (this.mLock) {
                Iterator<WakeLock> it = this.mWakeLocks.iterator();
                while (it.hasNext()) {
                    WakeLock wakelock = it.next();
                    if (wakelock.mOwnerUid == uid) {
                        targetTags.add(wakelock.mTag);
                    }
                }
            }
            Iterator<String> it2 = targetTags.iterator();
            while (it2.hasNext()) {
                String tag = it2.next();
                PowerManagerServiceStub.get().setUidPartialWakeLockDisabledState(uid, tag, !enable);
            }
            String reason = enable ? "resume" : BuildConfig.BUILD_TYPE;
            EventLog.writeEvent(SmartPowerSettings.EVENT_TAGS, "wakelock u:" + uid + " s:" + reason);
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "setWakeLockState " + uid + " s:" + reason);
            }
        } catch (Exception e) {
            Slog.e(AppPowerResourceManager.TAG, "releaseAppPowerResource", e);
        }
    }

    public void acquireWakelock(final IBinder lock, int flags, String tag, final int ownerUid, int ownerPid) {
        WakeLock wakeLock;
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index >= 0) {
                wakeLock = this.mWakeLocks.get(index);
                wakeLock.updateProperties(flags, tag);
            } else {
                wakeLock = new WakeLock(lock, flags, tag, ownerUid, ownerPid);
                try {
                    lock.linkToDeath(wakeLock, 0);
                    this.mWakeLocks.add(wakeLock);
                } catch (RemoteException e) {
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "onAcquireWakelock: " + wakeLock.toString());
            }
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.AppWakelockResource$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppWakelockResource.this.lambda$acquireWakelock$0(lock, ownerUid);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$acquireWakelock$0(IBinder lock, int ownerUid) {
        onAppBehaviorEvent(lock, ownerUid, true);
    }

    public void releaseWakelock(IBinder lock, int flags) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index < 0) {
                return;
            }
            WakeLock wakeLock = this.mWakeLocks.get(index);
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "onReleaseWakelock: " + wakeLock.toString());
            }
            try {
                wakeLock.mLock.unlinkToDeath(wakeLock, 0);
            } catch (Exception e) {
                Slog.w(AppPowerResourceManager.TAG, "unlinkToDeath failed wakelock=" + wakeLock.toString() + " lock=" + lock, e);
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private int findWakeLockIndexLocked(IBinder lock) {
        int count = this.mWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (this.mWakeLocks.get(i).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private void removeWakeLockLocked(final WakeLock wakeLock, int index) {
        this.mWakeLocks.remove(index);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.AppWakelockResource$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppWakelockResource.this.lambda$removeWakeLockLocked$1(wakeLock);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeWakeLockLocked$1(WakeLock wakeLock) {
        onAppBehaviorEvent(wakeLock.mLock, wakeLock.mOwnerUid, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class WakeLock implements IBinder.DeathRecipient {
        public int mFlags;
        public final IBinder mLock;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public String mTag;

        public WakeLock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
            this.mLock = lock;
            this.mFlags = flags;
            this.mTag = tag;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
        }

        public void updateProperties(int flags, String tag) {
            this.mFlags = flags;
            this.mTag = tag;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppWakelockResource.this.handleWakeLockDeath(this);
        }

        private String getLockLevelString() {
            switch (this.mFlags & 65535) {
                case 1:
                    return "PARTIAL_WAKE_LOCK             ";
                case 6:
                    return "SCREEN_DIM_WAKE_LOCK          ";
                case 10:
                    return "SCREEN_BRIGHT_WAKE_LOCK       ";
                case 26:
                    return "FULL_WAKE_LOCK                ";
                case 32:
                    return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                case 64:
                    return "DOZE_WAKE_LOCK                ";
                case 128:
                    return "DRAW_WAKE_LOCK                ";
                default:
                    return "???                           ";
            }
        }

        private String getLockFlagsString() {
            String result = (this.mFlags & 268435456) != 0 ? " ACQUIRE_CAUSES_WAKEUP" : "";
            if ((this.mFlags & 536870912) != 0) {
                return result + " ON_AFTER_RELEASE";
            }
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getLockLevelString());
            sb.append(" '");
            sb.append(this.mTag);
            sb.append("'");
            sb.append(getLockFlagsString());
            sb.append(" (uid=");
            sb.append(this.mOwnerUid);
            if (this.mOwnerPid != 0) {
                sb.append(" pid=");
                sb.append(this.mOwnerPid);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    /* loaded from: classes.dex */
    private class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    AppWakelockResource.this.setWakeLockState(msg.arg1, msg.arg2 != 0);
                    return;
                default:
                    return;
            }
        }
    }

    public void onAppBehaviorEvent(IBinder binder, int uid, boolean state) {
        if (UserHandle.getAppId(uid) < 10000) {
            return;
        }
        String packageName = this.mPackageManagerInt.getNameForUid(uid);
        ApplicationInfo applicationInfo = this.mPackageManagerInt.getApplicationInfo(packageName, 0L, 1000, UserHandle.getUserId(uid));
        if (applicationInfo == null || ProcessUtils.isSystem(applicationInfo)) {
            return;
        }
        this.mSecurityManagerInt.recordDurationInfo(26, binder, uid, applicationInfo.packageName, state);
    }
}
