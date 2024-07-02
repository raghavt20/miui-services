package com.android.server.location;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.WorkSource;
import android.util.SparseArray;

/* loaded from: classes.dex */
public class LocationOpHandler extends Handler {
    private static final int MSG_DELAYED_LOCATION_OP = 1;
    private static final String TAG = "LocationOpHanlder";
    private final Context mContext;
    private SparseArray<LocationOpRecord> mLastLocationOps;
    private final Object mLock;
    private final WifiManager mWifiManager;

    public LocationOpHandler(Context context, Looper looper) {
        super(looper);
        this.mLock = new Object();
        this.mLastLocationOps = new SparseArray<>();
        this.mContext = context;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                if (msg.arg1 == 2) {
                    postWifiScanRequest(msg.arg2);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void postWifiScanRequest(int uid) {
        this.mWifiManager.startScan(new WorkSource(uid));
    }

    public boolean isFrequenctlyOp(int uid, int op, long optime, int minInterval) {
        if (op != 2 && op != 3) {
            return false;
        }
        boolean isFrequenctlyOp = false;
        synchronized (this.mLock) {
            LocationOpRecord lastOp = this.mLastLocationOps.get(op, null);
            if (lastOp != null && op == 2 && optime > lastOp.timestamp && optime < lastOp.timestamp + minInterval) {
                isFrequenctlyOp = true;
            }
        }
        return isFrequenctlyOp;
    }

    public void setFollowupAction(int uid, int op, long optime, int minInterval) {
        synchronized (this.mLock) {
            if (op == 2) {
                long delay = minInterval;
                LocationOpRecord lastOp = this.mLastLocationOps.get(op, null);
                if (lastOp != null && optime > lastOp.timestamp) {
                    delay -= optime - lastOp.timestamp;
                }
                Message msg = Message.obtain(this, 1, op, uid);
                sendMessageDelayed(msg, delay);
            }
        }
    }

    public void updateLastLocationOp(int uid, int op, long optime) {
        if (op != 2 && op != 3) {
            return;
        }
        synchronized (this.mLock) {
            LocationOpRecord opRecord = new LocationOpRecord(uid, op, optime);
            this.mLastLocationOps.put(op, opRecord);
            if (op == 2) {
                removeMessages(1);
            }
        }
    }

    /* loaded from: classes.dex */
    public class LocationOpRecord {
        int locationOp;
        long timestamp;
        int uid;

        public LocationOpRecord(int uid, int locationOp, long timestamp) {
            this.uid = uid;
            this.locationOp = locationOp;
            this.timestamp = timestamp;
        }
    }
}
