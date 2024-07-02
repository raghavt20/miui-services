package com.miui.server.greeze;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import com.miui.server.greeze.GreezeManagerService;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class AurogonDownloadFilter {
    private static final int DOWNLOAD_SPEED_COMMEN_LIMIT = 500;
    private static final int DOWNLOAD_SPEED_HIGH_LIMIT = 1000;
    private static final int DOWNLOAD_SPEED_LOW_LIMIT = 100;
    private static final int MSG_AUROGON_DOWNLOAD_SPEED_UPDATE = 1001;
    private static final int TIME_NET_DETECT_CYCLE = 5000;
    public static List<String> mImportantDownloadApp = new ArrayList(Arrays.asList("com.tencent.tmgp.pubgmhd", "com.tencent.tmgp.sgame", "com.miHoYo.ys.mi", "com.miHoYo.Yuanshen", "com.miHoYo.ys.bilibili", "com.tencent.lolm", "com.tencent.jkchess", "com.tencent.android.qqdownloader", "com.hunantv.imgo.activity", "com.youku.phone", "com.cmcc.cmvideo", "com.letv.android.client"));
    public static final AurogonDownloadFilter sInstance = new AurogonDownloadFilter();
    private List<AppNetStatus> mAppNetCheckList;
    private DownloadHandler mHandler;
    private int mLastLaunchedAppUid = -1;

    private AurogonDownloadFilter() {
        this.mHandler = null;
        this.mAppNetCheckList = null;
        this.mAppNetCheckList = new ArrayList();
        this.mHandler = new DownloadHandler(GreezeManagerService.GreezeThread.getInstance().getLooper());
    }

    public static AurogonDownloadFilter getInstance() {
        return sInstance;
    }

    public long getUidTxBytes(int uid) {
        return TrafficStats.getUidTxBytes(uid);
    }

    public long getUidRxBytes(int uid) {
        return TrafficStats.getUidRxBytes(uid);
    }

    public long getTotalTxBytes() {
        return TrafficStats.getTotalTxBytes();
    }

    public long getTotalRxBytes() {
        return TrafficStats.getTotalRxBytes();
    }

    public void updateAppSpeed() {
        List<AppNetStatus> removeList = new ArrayList<>();
        synchronized (this.mAppNetCheckList) {
            for (AppNetStatus app : this.mAppNetCheckList) {
                calcAppSPeed(app);
                if (app.downloadSpeed > 1000.0f) {
                    if (GreezeManagerDebugConfig.DEBUG) {
                        Slog.d("Aurogon isDownload ", app.toString());
                    }
                    app.isDownload = true;
                } else if (app.downloadSpeed > 500.0f) {
                    app.isDownload = false;
                } else if (app.downloadSpeed < 100.0f) {
                    app.isDownload = false;
                    if (app.mUid != this.mLastLaunchedAppUid) {
                        removeList.add(app);
                    }
                }
            }
        }
        for (AppNetStatus removeapp : removeList) {
            removeAppNetCheckList(removeapp);
        }
        if (this.mAppNetCheckList.size() != 0) {
            this.mHandler.sendEmptyMessageDelayed(1001, 5000L);
        }
    }

    public void calcAppSPeed(AppNetStatus app) {
        long txBytes = getUidTxBytes(app.mUid);
        long rxBytes = getUidRxBytes(app.mUid);
        long totalBytes = txBytes + rxBytes;
        long diffTotalBytes = totalBytes - (app.mLastRxBytes + app.mLastTxBytes);
        long now = SystemClock.uptimeMillis();
        long time = (now - app.time) / 1000;
        if (time != 0) {
            app.downloadSpeed = (float) ((diffTotalBytes / FormatBytesUtil.KB) / time);
        }
        app.mLastRxBytes = rxBytes;
        app.mLastTxBytes = txBytes;
        app.time = now;
    }

    public boolean isDownloadApp(int uid) {
        AppNetStatus app = getAppNetStatus(uid);
        if (app != null) {
            return app.isDownload;
        }
        return false;
    }

    public boolean isDownloadApp(String pacakgeName) {
        AppNetStatus app = getAppNetStatus(pacakgeName);
        if (app != null) {
            return app.isDownload;
        }
        return false;
    }

    public void addAppNetCheckList(int uid, String packageName) {
        if (getAppNetStatus(uid) != null) {
            return;
        }
        if (!this.mHandler.hasMessages(1001)) {
            this.mHandler.sendEmptyMessageDelayed(1001, 5000L);
        }
        AppNetStatus app = new AppNetStatus(uid, packageName);
        app.mLastTxBytes = getUidTxBytes(uid);
        app.mLastRxBytes = getUidRxBytes(uid);
        app.time = SystemClock.uptimeMillis();
        synchronized (this.mAppNetCheckList) {
            this.mAppNetCheckList.add(app);
        }
    }

    public void setMoveToFgApp(int uid) {
        this.mLastLaunchedAppUid = uid;
    }

    public void removeAppNetCheckList(String packageName) {
        AppNetStatus app = getAppNetStatus(packageName);
        removeAppNetCheckList(app);
    }

    public void removeAppNetCheckList(int uid) {
        AppNetStatus app = getAppNetStatus(uid);
        removeAppNetCheckList(app);
    }

    public void removeAppNetCheckList(AppNetStatus app) {
        synchronized (this.mAppNetCheckList) {
            this.mAppNetCheckList.remove(app);
        }
    }

    private AppNetStatus getAppNetStatus(String packageName) {
        synchronized (this.mAppNetCheckList) {
            for (AppNetStatus app : this.mAppNetCheckList) {
                if (packageName.equals(app.mPacakgeName)) {
                    return app;
                }
            }
            return null;
        }
    }

    private AppNetStatus getAppNetStatus(int uid) {
        synchronized (this.mAppNetCheckList) {
            for (AppNetStatus app : this.mAppNetCheckList) {
                if (app.mUid == uid) {
                    return app;
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DownloadHandler extends Handler {
        private DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 1001:
                    AurogonDownloadFilter.this.updateAppSpeed();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AppNetStatus {
        float downloadSpeed;
        boolean isDownload = false;
        long mLastRxBytes;
        long mLastTxBytes;
        String mPacakgeName;
        int mUid;
        long time;

        AppNetStatus(int uid, String pacakgeName) {
            this.mUid = uid;
            this.mPacakgeName = pacakgeName;
        }

        public String toString() {
            return " AppNetStatus mPacakgeName = " + this.mPacakgeName + " uid = " + this.mUid + " downloadSpeed = " + this.downloadSpeed + " kb/s";
        }
    }
}
