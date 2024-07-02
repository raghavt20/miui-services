package com.miui.server.smartpower;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.net.NetworkManagementServiceStub;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.smartpower.AppPowerResource;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.util.ArrayList;
import java.util.HashMap;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AppNetworkResource extends AppPowerResource {
    private static final int MSG_NETWORK_CHECK = 1;
    private static final int NET_DOWNLOAD_SCENE_THRESHOLD = 4;
    private static final int NET_KB = 1024;
    private Handler mHandler;
    private final HashMap<Integer, NetworkMonitor> mNetworkMonitorMap = new HashMap<>();
    private NetworkStatsManager mNetworkStatsManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppNetworkResource(Context context, Looper looper) {
        this.mHandler = new MyHandler(looper);
        this.mType = 2;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList getActiveUids() {
        return null;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        return false;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        return false;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
        String event = "network u:" + uid + " s:release";
        Trace.traceBegin(131072L, event);
        updateNetworkRule(uid, true);
        EventLog.writeEvent(SmartPowerSettings.EVENT_TAGS, event);
        Trace.traceEnd(131072L);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
        String event = "network u:" + uid + " s:resume";
        updateNetworkRule(uid, false);
        EventLog.writeEvent(SmartPowerSettings.EVENT_TAGS, event);
        Trace.traceEnd(131072L);
    }

    private void updateNetworkRule(int uid, boolean allow) {
        try {
            NetworkManagementServiceStub.getInstance().updateAurogonUidRule(uid, allow);
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "setFirewall received: " + uid);
            }
        } catch (Exception e) {
            Slog.i(AppPowerResourceManager.TAG, "setFirewall failed " + uid);
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void registerCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        super.registerCallback(callback, uid);
        synchronized (this.mNetworkMonitorMap) {
            if (this.mNetworkMonitorMap.get(Integer.valueOf(uid)) == null) {
                this.mNetworkMonitorMap.put(Integer.valueOf(uid), new NetworkMonitor(uid));
            }
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void unRegisterCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        super.unRegisterCallback(callback, uid);
        synchronized (this.mNetworkMonitorMap) {
            if (this.mResourceCallbacksByUid.get(uid) == null) {
                this.mNetworkMonitorMap.remove(Integer.valueOf(uid));
            }
        }
    }

    /* loaded from: classes.dex */
    class NetworkMonitor {
        int mUid;
        int mActiveSeconds = 0;
        int mInactiveSeconds = 0;
        boolean mIsActive = false;
        long mLastTimeStamp = System.currentTimeMillis();
        long mlastTotalKiloBytes = getUidTxBytes() / FormatBytesUtil.KB;

        NetworkMonitor(int uid) {
            this.mUid = uid;
            Message nextMsg = AppNetworkResource.this.mHandler.obtainMessage(1, this);
            AppNetworkResource.this.mHandler.sendMessageDelayed(nextMsg, SmartPowerSettings.DEF_RES_NET_MONITOR_PERIOD);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateNetworkStatus() {
            long currentTotalKiloBytes = getUidTxBytes() / FormatBytesUtil.KB;
            long now = System.currentTimeMillis();
            long duration = now - this.mLastTimeStamp;
            if (duration <= 0) {
                return;
            }
            long speed = ((currentTotalKiloBytes - this.mlastTotalKiloBytes) * 1000) / duration;
            if (AppPowerResource.DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, this.mUid + " speed " + speed + "kb/s");
            }
            if (speed > SmartPowerSettings.DEF_RES_NET_ACTIVE_SPEED) {
                int i = this.mActiveSeconds + 1;
                this.mActiveSeconds = i;
                this.mInactiveSeconds = 0;
                this.mActiveSeconds = i + 1;
                if (i > 4) {
                    this.mIsActive = true;
                    AppNetworkResource.this.reportResourceStatus(this.mUid, true, 128);
                }
            } else {
                int i2 = this.mInactiveSeconds + 1;
                this.mInactiveSeconds = i2;
                this.mActiveSeconds = 0;
                this.mInactiveSeconds = i2 + 1;
                if (i2 > 4) {
                    this.mIsActive = false;
                    AppNetworkResource.this.reportResourceStatus(this.mUid, false, 128);
                }
            }
            this.mlastTotalKiloBytes = currentTotalKiloBytes;
            this.mLastTimeStamp = now;
        }

        private long getUidTxBytes() {
            return TrafficStats.getUidTxBytes(this.mUid) + TrafficStats.getUidRxBytes(this.mUid);
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
            if (msg.what == 1) {
                NetworkMonitor monitor = (NetworkMonitor) msg.obj;
                synchronized (AppNetworkResource.this.mNetworkMonitorMap) {
                    if (AppNetworkResource.this.mNetworkMonitorMap.containsValue(monitor)) {
                        monitor.updateNetworkStatus();
                        Message message = AppNetworkResource.this.mHandler.obtainMessage(1, monitor);
                        AppNetworkResource.this.mHandler.sendMessageDelayed(message, 1000L);
                    }
                }
            }
        }
    }
}
