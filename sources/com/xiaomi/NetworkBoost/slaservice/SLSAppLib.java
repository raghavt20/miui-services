package com.xiaomi.NetworkBoost.slaservice;

import android.app.IActivityManager;
import android.content.Context;
import android.net.wifi.SlaveWifiManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.xiaomi.NetworkBoost.StatusManager;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import miui.process.ForegroundInfo;
import vendor.qti.sla.service.V1_0.ISlaService;

/* loaded from: classes.dex */
public class SLSAppLib {
    private static final int EVENT_START_VOIP = 100;
    private static final int EVENT_STOP_VOIP = 101;
    static final String TAG = "SLM-SRV-SLSAppLib";
    private static final int VOIP_START_DELAY_TIME = 10000;
    private static final int VOIP_STOP_DELAY_TIME = 5000;
    private static ISlaService mSlaService;
    private boolean isSLSEnable;
    private IActivityManager mActivityManager;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private SLATrack mSLATrack;
    private WeakReference<SLAService> mSLMService;
    private static HashSet<String> mSlaAppList = new HashSet<>();
    private static HashSet<String> mSlsAppList = new HashSet<>();
    private static int mSLSVoIPUid = 0;
    private static int mSLSUid = 0;
    private boolean isSLSGameRunning = false;
    private boolean isSLSVoIPRunning = false;
    private boolean powerMgrGameStatus = false;
    private int powerMgrGameUid = 0;
    private boolean powerMgrVoIPStatus = false;
    private boolean powerMgrSLMStatus = true;
    private boolean powerMgrScreenOn = true;
    private StatusManager mStatusManager = null;
    private StatusManager.IAppStatusListener mAppStatusListener = new StatusManager.IAppStatusListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLSAppLib.1
        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            if (SLSAppLib.mSlaAppList.contains(Integer.toString(foregroundInfo.mForegroundUid))) {
                if (SLSAppLib.mSlsAppList.contains(Integer.toString(foregroundInfo.mForegroundUid))) {
                    SLSAppLib.this.setSLSGameStart(Integer.toString(foregroundInfo.mForegroundUid));
                    SLSAppLib.this.setPowerMgrGameInfo(true, foregroundInfo.mForegroundUid);
                    return;
                }
                return;
            }
            if ((!SLSAppLib.mSlaAppList.contains(Integer.toString(foregroundInfo.mForegroundUid)) || !SLSAppLib.mSlsAppList.contains(Integer.toString(foregroundInfo.mForegroundUid))) && SLSAppLib.mSlaAppList.contains(Integer.toString(foregroundInfo.mLastForegroundUid)) && SLSAppLib.mSlsAppList.contains(Integer.toString(foregroundInfo.mLastForegroundUid))) {
                if (SLSAppLib.mSLSUid == foregroundInfo.mLastForegroundUid) {
                    Log.d(SLSAppLib.TAG, "onForegroundInfoChanged stop game uid:" + foregroundInfo.mLastForegroundUid);
                    SLSAppLib.this.setSLSGameStop(Integer.toString(foregroundInfo.mLastForegroundUid));
                }
                if (SLSAppLib.this.powerMgrGameUid == foregroundInfo.mLastForegroundUid) {
                    Log.d(SLSAppLib.TAG, "onForegroundInfoChanged reset gameinfo uid:" + foregroundInfo.mLastForegroundUid);
                    SLSAppLib.this.setPowerMgrGameInfo(false, foregroundInfo.mLastForegroundUid);
                }
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onUidGone(int uid, boolean disabled) {
            if (SLSAppLib.mSLSUid == uid) {
                Log.d(SLSAppLib.TAG, "onUidGone uid:" + uid);
                SLSAppLib.this.setSLSGameStop(Integer.toString(uid));
            }
            if (SLSAppLib.this.powerMgrGameUid == uid) {
                SLSAppLib.this.setPowerMgrGameInfo(false, uid);
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onAudioChanged(int mode) {
            Log.d(SLSAppLib.TAG, "initBroadcastReceiver mode:" + mode);
            if (mode == 3) {
                SLSAppLib.this.mHandler.removeMessages(100);
                SLSAppLib.this.mHandler.removeMessages(101);
                SLSAppLib.this.mHandler.sendEmptyMessageDelayed(100, 10000L);
            } else if (mode == 0) {
                SLSAppLib.this.mHandler.removeMessages(100);
                SLSAppLib.this.mHandler.removeMessages(101);
                SLSAppLib.this.mHandler.sendEmptyMessageDelayed(101, 5000L);
            }
        }
    };

    public SLSAppLib(Context context, SLAService service) {
        this.mContext = context;
        this.mSLMService = new WeakReference<>(service);
        this.mSLATrack = SLATrack.getSLATrack(context);
        HandlerThread handlerThread = new HandlerThread("SLSAppLibHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        registerAppStatusListener();
    }

    private void checkSLSStatus() {
        boolean z = this.powerMgrGameStatus;
        if (z && this.powerMgrVoIPStatus) {
            return;
        }
        if (z && !this.powerMgrScreenOn) {
            return;
        }
        if (z) {
            setSLSGameStart(Integer.toString(this.powerMgrGameUid));
        } else {
            if (this.powerMgrVoIPStatus) {
                this.mHandler.removeMessages(100);
                this.mHandler.removeMessages(101);
                setSLSVoIPStart();
                return;
            }
            Log.d(TAG, "checkSLSStatus false.");
        }
    }

    private void powerMgrCheck() {
        boolean z = this.powerMgrGameStatus;
        if ((z && this.powerMgrVoIPStatus) || (z && !this.powerMgrScreenOn)) {
            if (this.powerMgrSLMStatus) {
                this.powerMgrSLMStatus = false;
                disableSLM();
                return;
            }
            return;
        }
        if (!this.powerMgrSLMStatus) {
            this.powerMgrSLMStatus = true;
            enableSLM();
            if (this.powerMgrGameStatus) {
                setSLSGameStart(Integer.toString(this.powerMgrGameUid));
            } else {
                if (this.powerMgrVoIPStatus) {
                    this.mHandler.removeMessages(100);
                    this.mHandler.removeMessages(101);
                    setSLSVoIPStart();
                    return;
                }
                Log.e(TAG, "powerMgrCheck error.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPowerMgrGameInfo(boolean status, int uid) {
        this.powerMgrGameStatus = status;
        if (status) {
            this.powerMgrGameUid = uid;
        } else {
            this.powerMgrGameUid = 0;
        }
        powerMgrCheck();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPowerMgrVoIPInfo(boolean status) {
        this.powerMgrVoIPStatus = status;
        powerMgrCheck();
    }

    public void setPowerMgrScreenInfo(boolean status) {
        this.powerMgrScreenOn = status;
        powerMgrCheck();
    }

    private void enableSLM() {
        SLAService service = this.mSLMService.get();
        if (service == null) {
            Log.e(TAG, "enableSLM get SLAService null!");
        } else {
            service.enableSLM();
        }
    }

    private void disableSLM() {
        SLAService service = this.mSLMService.get();
        if (service == null) {
            Log.e(TAG, "enableSLM get SLAService null!");
        } else {
            service.disableSLM();
        }
    }

    public void setSlaService(ISlaService service) {
        mSlaService = service;
    }

    public void setSLSEnableStatus(boolean enable) {
        Log.i(TAG, "setSLSEnableStatus:" + enable);
        this.isSLSEnable = enable;
        if (!enable) {
            if (this.isSLSGameRunning) {
                setSLSGameStop(Integer.toString(mSLSUid));
            }
            if (this.isSLSVoIPRunning) {
                this.mHandler.removeMessages(100);
                this.mHandler.removeMessages(101);
                setSLSVoIPStop();
                return;
            }
            return;
        }
        checkSLSStatus();
    }

    public static void setSLAAppWhiteList(String uidList) {
        Log.i(TAG, "setSLAAppWhiteList:" + uidList);
        if (uidList == null) {
            return;
        }
        mSlaAppList.clear();
        String[] temp = uidList.split(",");
        for (String str : temp) {
            mSlaAppList.add(str);
        }
    }

    public static void setSLSGameUidList(String uidList) {
        Log.i(TAG, "setSLSGameUidList:" + uidList);
        if (uidList == null) {
            return;
        }
        mSlsAppList.clear();
        String[] temp = uidList.split(",");
        for (String str : temp) {
            mSlsAppList.add(str);
        }
    }

    public static void setSLSVoIPUid(int uid) {
        Log.i(TAG, "setSLSVoIPUid:" + uid);
        mSLSVoIPUid = uid;
    }

    private boolean checkHAL(String func) {
        if (mSlaService == null) {
            Log.e(TAG, func + " checkHAL null");
            return false;
        }
        return true;
    }

    public boolean setWifiBSSID() {
        if (!checkHAL("setWifiBSSID")) {
            return false;
        }
        String wifiBSSID = null;
        String slavewifiBSSID = null;
        try {
            WifiManager wifimanager = (WifiManager) this.mContext.getSystemService("wifi");
            if (wifimanager != null) {
                wifiBSSID = wifimanager.getConnectionInfo().getBSSID();
            }
            SlaveWifiManager slavewifimanager = (SlaveWifiManager) this.mContext.getSystemService("SlaveWifiService");
            if (slavewifimanager != null) {
                slavewifiBSSID = slavewifimanager.getWifiSlaveConnectionInfo().getBSSID();
            }
            if (wifiBSSID == null && slavewifiBSSID == null) {
                Log.e(TAG, "setWifiBSSID:null");
                return false;
            }
            String BSSID = wifiBSSID + "," + slavewifiBSSID;
            Log.i(TAG, "setWifiBSSID:" + BSSID);
            mSlaService.setSLSBSSID(BSSID);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return false;
        }
    }

    private boolean setMiWillGameStart(String uid) {
        SLAService service = this.mSLMService.get();
        if (service == null) {
            Log.e(TAG, "setMiWillGameStart get SLAService null!");
            return false;
        }
        return service.setMiWillGameStart(uid);
    }

    private boolean setMiWillGameStop(String uid) {
        SLAService service = this.mSLMService.get();
        if (service == null) {
            Log.e(TAG, "setMiWillGameStop get SLAService null!");
            return false;
        }
        return service.setMiWillGameStop(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setSLSGameStart(String uid) {
        if (!this.isSLSEnable || this.isSLSGameRunning || this.isSLSVoIPRunning) {
            return false;
        }
        if (uid == null) {
            Log.e(TAG, "setSLSGameStart null");
            return false;
        }
        if (!checkHAL("setSLSGameStart")) {
            return false;
        }
        Log.i(TAG, "setSLSGameStart:" + uid);
        if (!setWifiBSSID()) {
            return false;
        }
        this.isSLSGameRunning = true;
        mSLSUid = Integer.valueOf(uid).intValue();
        SLATrack.sendMsgSlsStart();
        try {
            if (!setMiWillGameStart(uid)) {
                mSlaService.setSLSGameStart(uid);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setSLSGameStop(String uid) {
        if (!this.isSLSGameRunning || !checkHAL("setSLSGameStop")) {
            return false;
        }
        Log.i(TAG, "setSLSGameStop:" + uid);
        this.isSLSGameRunning = false;
        mSLSUid = 0;
        SLATrack.sendMsgSlsStop();
        try {
            setMiWillGameStop(uid);
            mSlaService.setSLSGameStop(uid);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return false;
        }
    }

    private void registerAppStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerAppStatusListener(this.mAppStatusListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    /* loaded from: classes.dex */
    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    Log.i(SLSAppLib.TAG, "EVENT_START_VOIP");
                    SLSAppLib.this.setSLSVoIPStart();
                    SLSAppLib.this.setPowerMgrVoIPInfo(true);
                    return;
                case 101:
                    Log.i(SLSAppLib.TAG, "EVENT_STOP_VOIP");
                    SLSAppLib.this.setSLSVoIPStop();
                    SLSAppLib.this.setPowerMgrVoIPInfo(false);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSLSVoIPStart() {
        if (!this.isSLSEnable || this.isSLSGameRunning || this.isSLSVoIPRunning) {
            return;
        }
        if (mSLSVoIPUid <= 0) {
            Log.e(TAG, "setSLSVoIPStart:" + mSLSVoIPUid);
            return;
        }
        if (!checkHAL("setSLSVoIPStart")) {
            return;
        }
        Log.i(TAG, "setSLSVoIPStart:" + mSLSVoIPUid);
        if (setWifiBSSID()) {
            this.isSLSVoIPRunning = true;
            try {
                mSlaService.setSLSVoIPStart(Integer.toString(mSLSVoIPUid));
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSLSVoIPStop() {
        if (!this.isSLSVoIPRunning || !checkHAL("setSLSVoIPStop")) {
            return;
        }
        Log.i(TAG, "setSLSVoIPStop:");
        this.isSLSVoIPRunning = false;
        try {
            mSlaService.setSLSVoIPStart("0");
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }
}
