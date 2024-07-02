package com.android.server.media.projection;

import android.media.projection.MediaProjectionInfo;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.miui.base.MiuiStubRegistry;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class MediaProjectionManagerServiceStubImpl implements MediaProjectionManagerServiceStub {
    private static final String TAG = "MediaProjectionManagerServiceStubImpl";
    private final LinkedList<MediaProjectionManagerService.MediaProjection> mMediaProjectionClients = new LinkedList<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MediaProjectionManagerServiceStubImpl> {

        /* compiled from: MediaProjectionManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MediaProjectionManagerServiceStubImpl INSTANCE = new MediaProjectionManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MediaProjectionManagerServiceStubImpl m1873provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MediaProjectionManagerServiceStubImpl m1872provideNewInstance() {
            return new MediaProjectionManagerServiceStubImpl();
        }
    }

    public boolean isSupportMutilMediaProjection() {
        return true;
    }

    public void handleForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        Log.d(TAG, "handleForegroundServicesChanged");
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            if (projection != null && projection.uid == uid && projection.pid == pid && projection.requiresForegroundService() && (serviceTypes & 32) == 0) {
                if (isUcar(projection.packageName) || isScreenRecorder(projection.packageName)) {
                    Log.d(TAG, "handleForegroundServicesChanged don't stop projection for " + projection.packageName);
                } else {
                    it.remove();
                    Log.d(TAG, "handleForegroundServicesChanged stop projection for " + projection.packageName);
                    projection.stop();
                }
            }
        }
    }

    private boolean isScreenRecorder(String packageName) {
        if (TextUtils.equals(packageName, "com.miui.screenrecorder")) {
            return true;
        }
        return false;
    }

    private boolean isUcar(String packageName) {
        if (TextUtils.equals(packageName, "com.miui.carlink")) {
            return true;
        }
        return false;
    }

    public void addMediaProjection(MediaProjectionManagerService.MediaProjection mp) {
        Log.d(TAG, "addMediaProjection");
        removeMediaProjection(mp);
        this.mMediaProjectionClients.add(0, mp);
    }

    public void removeMediaProjection(MediaProjectionManagerService.MediaProjection mp) {
        Log.d(TAG, "removeMediaProjection");
        this.mMediaProjectionClients.remove(mp);
    }

    public boolean isValidMediaProjection(IBinder token) {
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            if (projection.asBinder() == token) {
                return true;
            }
        }
        return false;
    }

    public MediaProjectionInfo getActiveProjectionInfo() {
        if (this.mMediaProjectionClients.size() == 0) {
            return null;
        }
        return this.mMediaProjectionClients.get(0).getProjectionInfo();
    }

    public void stopActiveProjection() {
        Log.d(TAG, "stopActiveProjection");
        if (this.mMediaProjectionClients.size() > 0) {
            this.mMediaProjectionClients.get(0).stop();
        }
    }

    public void stopProjections() {
        Log.d(TAG, "stopProjections");
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            it.remove();
            if (projection != null) {
                projection.stop();
            }
        }
    }

    public MediaProjectionManagerService.MediaProjection getTopProject() {
        if (this.mMediaProjectionClients.isEmpty()) {
            return null;
        }
        return this.mMediaProjectionClients.get(0);
    }
}
