package com.android.server.content;

import android.accounts.Account;
import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.util.ArrayMap;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.PrintWriter;

@MiuiStubHead(manifestName = "com.android.server.content.ContentServiceStub$$")
/* loaded from: classes.dex */
public class ContentServiceStubImpl extends ContentServiceStub {
    private static final int MIUI_OBSERVERS_THRESHOLD = 20000;
    private static final ArrayMap<Long, Integer> sObserverHistogram = new ArrayMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ContentServiceStubImpl> {

        /* compiled from: ContentServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ContentServiceStubImpl INSTANCE = new ContentServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ContentServiceStubImpl m948provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ContentServiceStubImpl m947provideNewInstance() {
            return new ContentServiceStubImpl();
        }
    }

    public void setMiSyncPauseToTime(Context context, ContentService contentService, Account account, long pauseTimeMillis, int uid) {
        contentService.enforceCrossUserPermissionForInjector(uid, "no permission to set the sync status for user " + uid);
        context.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        long identityToken = ContentService.clearCallingIdentity();
        try {
            SyncManager syncManager = contentService.getSyncManagerForInjector();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMiSyncPauseToTime(account, pauseTimeMillis, uid);
                SyncManagerStubImpl.handleSyncPauseChanged(context, syncManager, pauseTimeMillis);
            }
        } finally {
            ContentService.restoreCallingIdentity(identityToken);
        }
    }

    public long getMiSyncPauseToTime(Context context, ContentService contentService, Account account, int uid) {
        contentService.enforceCrossUserPermissionForInjector(uid, "no permission to read the sync settings for user " + uid);
        context.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = ContentService.clearCallingIdentity();
        try {
            SyncManager syncManager = contentService.getSyncManagerForInjector();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getMiSyncPauseToTime(account, uid);
            }
            ContentService.restoreCallingIdentity(identityToken);
            return 0L;
        } finally {
            ContentService.restoreCallingIdentity(identityToken);
        }
    }

    public void setMiSyncStrategy(Context context, ContentService contentService, Account account, int strategy, int uid) {
        contentService.enforceCrossUserPermissionForInjector(uid, "no permission to set the sync status for user " + uid);
        context.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        long identityToken = ContentService.clearCallingIdentity();
        try {
            SyncManager syncManager = contentService.getSyncManagerForInjector();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMiSyncStrategy(account, strategy, uid);
                SyncManagerStubImpl.handleSyncStrategyChanged(context, syncManager);
            }
        } finally {
            ContentService.restoreCallingIdentity(identityToken);
        }
    }

    public int getMiSyncStrategy(Context context, ContentService contentService, Account account, int uid) {
        contentService.enforceCrossUserPermissionForInjector(uid, "no permission to read the sync settings for user " + uid);
        context.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = ContentService.clearCallingIdentity();
        try {
            SyncManager syncManager = contentService.getSyncManagerForInjector();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getMiSyncStrategy(account, uid);
            }
            ContentService.restoreCallingIdentity(identityToken);
            return 0;
        } finally {
            ContentService.restoreCallingIdentity(identityToken);
        }
    }

    public void increasePidObserverCount(int uid, int pid) {
        ArrayMap<Long, Integer> arrayMap = sObserverHistogram;
        synchronized (arrayMap) {
            long uidPid = (uid << 32) | pid;
            int count = arrayMap.getOrDefault(Long.valueOf(uidPid), 0).intValue() + 1;
            arrayMap.put(Long.valueOf(uidPid), Integer.valueOf(count));
            if (count == MIUI_OBSERVERS_THRESHOLD && uid != 1000 && Binder.getCallingPid() != Process.myPid()) {
                throw new SecurityException("uid " + uid + " pid " + pid + " registered too many content observers");
            }
        }
    }

    public void decreasePidObserverCount(int uid, int pid) {
        ArrayMap<Long, Integer> arrayMap = sObserverHistogram;
        synchronized (arrayMap) {
            long uidPid = (uid << 32) | pid;
            int prevCount = arrayMap.getOrDefault(Long.valueOf(uidPid), -1).intValue();
            if (prevCount == -1) {
                return;
            }
            if (prevCount > 0) {
                arrayMap.put(Long.valueOf(uidPid), Integer.valueOf(prevCount - 1));
            } else {
                arrayMap.remove(Long.valueOf(uidPid));
            }
        }
    }

    public void dumpObserverHistogram(PrintWriter pw) {
        pw.println("Observer histogram:");
        ArrayMap<Long, Integer> arrayMap = sObserverHistogram;
        synchronized (arrayMap) {
            int size = arrayMap.size();
            for (int i = 0; i < size; i++) {
                ArrayMap<Long, Integer> arrayMap2 = sObserverHistogram;
                long key = arrayMap2.keyAt(i).longValue();
                int count = arrayMap2.valueAt(i).intValue();
                int uid = (int) (key >> 32);
                int pid = (int) key;
                pw.println("  uid: " + uid + " pid: " + pid + " " + count + " observers");
            }
        }
    }
}
