package com.xiaomi.NetworkBoost.MscsService;

import android.content.Context;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.SlaveWifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.server.audio.AudioServiceStubImpl;
import com.miui.server.security.AccessControlImpl;
import com.xiaomi.NetworkBoost.StatusManager;

/* loaded from: classes.dex */
public class MscsService {
    private static final String CLOUD_MSCS_ENABLE = "cloud_mscs_enable";
    private static final String CLOUD_WIFI7_MSCS_ENABLE = "cloud_wifi7_mscs_enable";
    private static final String MSCS_FRAME_CLASSIFIER = "0401040000000000000000000000000000";
    private static final int MSCS_STREAM_TIMEOUT = 60000;
    private static final String MSCS_UP_BITMAP = "f0";
    private static final String MSCS_UP_LIMIT = "7";
    private static final int MSG_REMOVE_REQ = 2;
    private static final int MSG_SEND_REQ = 1;
    private static final int REMOVE_DELAY = 60000;
    private static final int SEND_DELAY = 50000;
    private static final String TAG = "MscsService";
    private static final String WIFI_INTERFACE_0 = "wlan0";
    private static final String WIFI_INTERFACE_1 = "wlan1";
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private static volatile MscsService sInstance = null;
    private static int RESULT_CODE_NO_EFFECT = -1;
    private static int RESULT_CODE_SUCCESS = 0;
    private StatusManager mStatusManager = null;
    private MiuiWifiManager mMiuiWifiManager = null;
    private ConnectivityManager mConnectivityManager = null;
    private Network mNetwork = null;
    private boolean mMasterWifiSupport = true;
    private boolean mSlaveWifiSupport = true;
    private boolean mMasterWifiUsed = false;
    private boolean mSlaveWifiUsed = false;
    private boolean mIsScreenOn = false;
    private boolean mMSCSEnable = false;
    private boolean mWifi7MSCSEnable = false;
    private WifiManager mWifiManager = null;
    private SlaveWifiManager mSlaveWifiManager = null;
    private StatusManager.INetworkInterfaceListener mNetworkInterfaceListener = new StatusManager.INetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.MscsService.MscsService.3
        @Override // com.xiaomi.NetworkBoost.StatusManager.INetworkInterfaceListener
        public void onNetwrokInterfaceChange(String ifacename, int ifacenum, boolean wifiready, boolean slavewifiready, boolean dataready, boolean slaveDataReady, String status) {
            if (ifacename == null) {
                MscsService.this.mHandler.removeMessages(1);
                return;
            }
            if (ifacename.indexOf("wlan") == -1) {
                MscsService.this.mHandler.removeMessages(1);
                return;
            }
            Log.d(MscsService.TAG, "onNetwrokInterfaceChange ifacename:" + ifacename);
            boolean z = false;
            MscsService.this.mMasterWifiUsed = ifacename.indexOf("wlan0") != -1;
            MscsService mscsService = MscsService.this;
            mscsService.mMasterWifiSupport = mscsService.mMasterWifiUsed;
            MscsService mscsService2 = MscsService.this;
            if (mscsService2.isWifi7MLO(mscsService2.mWifiManager.getConnectionInfo())) {
                MscsService mscsService3 = MscsService.this;
                mscsService3.mMasterWifiSupport = mscsService3.mMasterWifiSupport && MscsService.this.mWifi7MSCSEnable;
            }
            MscsService.this.mSlaveWifiUsed = ifacename.indexOf("wlan1") != -1;
            MscsService mscsService4 = MscsService.this;
            mscsService4.mSlaveWifiSupport = mscsService4.mSlaveWifiUsed;
            if (MscsService.this.mSlaveWifiManager != null) {
                MscsService mscsService5 = MscsService.this;
                if (mscsService5.isWifi7MLO(mscsService5.mSlaveWifiManager.getWifiSlaveConnectionInfo())) {
                    MscsService mscsService6 = MscsService.this;
                    if (mscsService6.mSlaveWifiSupport && MscsService.this.mWifi7MSCSEnable) {
                        z = true;
                    }
                    mscsService6.mSlaveWifiSupport = z;
                }
            }
            Log.d(MscsService.TAG, "onNetwrokInterfaceChange mMasterWifiUsed:" + MscsService.this.mMasterWifiUsed + " mSlaveWifiUsed:" + MscsService.this.mSlaveWifiUsed + " mMasterWifiSupport:" + MscsService.this.mMasterWifiSupport + " mSlaveWifiSupport" + MscsService.this.mSlaveWifiSupport + " mIsScreenOn:" + MscsService.this.mIsScreenOn);
            MscsService.this.mHandler.removeMessages(1);
            if (MscsService.this.mIsScreenOn) {
                MscsService.this.mHandler.sendEmptyMessage(1);
            }
        }
    };
    private StatusManager.IScreenStatusListener mScreenStatusListener = new StatusManager.IScreenStatusListener() { // from class: com.xiaomi.NetworkBoost.MscsService.MscsService.4
        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenON() {
            Log.d(MscsService.TAG, "onScreenON");
            MscsService.this.mIsScreenOn = true;
            if ((MscsService.this.mMasterWifiUsed && MscsService.this.mMasterWifiSupport) || (MscsService.this.mSlaveWifiUsed && MscsService.this.mSlaveWifiSupport)) {
                MscsService.this.mHandler.removeMessages(2);
                MscsService.this.mHandler.removeMessages(1);
                MscsService.this.mHandler.sendEmptyMessage(1);
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenOFF() {
            Log.d(MscsService.TAG, "onScreenOFF");
            MscsService.this.mIsScreenOn = false;
            MscsService.this.mHandler.removeMessages(2);
            MscsService.this.mHandler.sendEmptyMessageDelayed(2, AccessControlImpl.LOCK_TIME_OUT);
        }
    };

    private MscsService(Context context) {
        this.mContext = null;
        this.mContext = context.getApplicationContext();
    }

    public static MscsService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MscsService.class) {
                if (sInstance == null) {
                    sInstance = new MscsService(context);
                    try {
                        sInstance.onCreate();
                    } catch (Exception e) {
                        Log.e(TAG, "getInstance onCreate catch:", e);
                    }
                }
            }
        }
        return sInstance;
    }

    public static void destroyInstance() {
        if (sInstance != null) {
            synchronized (MscsService.class) {
                if (sInstance != null) {
                    try {
                        sInstance.onDestroy();
                    } catch (Exception e) {
                        Log.e(TAG, "destroyInstance onDestroy catch:", e);
                    }
                    sInstance = null;
                }
            }
        }
    }

    public void onCreate() {
        Log.i(TAG, "onCreate ");
        try {
            this.mMiuiWifiManager = (MiuiWifiManager) this.mContext.getSystemService("MiuiWifiService");
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            this.mSlaveWifiManager = (SlaveWifiManager) this.mContext.getSystemService("SlaveWifiService");
        } catch (Exception e) {
            Log.e(TAG, "getSystemService Exception:" + e);
        }
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        registerMscsChangeObserver();
        this.mMSCSEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOUD_MSCS_ENABLE, 0, -2) == 1;
        this.mWifi7MSCSEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOUD_WIFI7_MSCS_ENABLE, 0, -2) == 1;
        registerNetworkCallback();
        registerScreenStatusListener();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        unregisterNetworkCallback();
        unregisterScreenStatusListener();
    }

    private void registerMscsChangeObserver() {
        Log.d(TAG, "MSCS cloud observer register");
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.xiaomi.NetworkBoost.MscsService.MscsService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MscsService mscsService = MscsService.this;
                mscsService.mMSCSEnable = Settings.System.getIntForUser(mscsService.mContext.getContentResolver(), MscsService.CLOUD_MSCS_ENABLE, 0, -2) == 1;
                MscsService mscsService2 = MscsService.this;
                mscsService2.mWifi7MSCSEnable = Settings.System.getIntForUser(mscsService2.mContext.getContentResolver(), MscsService.CLOUD_WIFI7_MSCS_ENABLE, 0, -2) == 1;
                Log.d(MscsService.TAG, "MSCS cloud change, mMSCSEnable: " + MscsService.this.mMSCSEnable);
                Log.d(MscsService.TAG, "WIFI7 MSCS cloud change, mWifi7MSCSEnable: " + MscsService.this.mWifi7MSCSEnable);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MSCS_ENABLE), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_WIFI7_MSCS_ENABLE), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.xiaomi.NetworkBoost.MscsService.MscsService.2
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int enableMSCS(String interfaceName) {
        Log.d(TAG, "enableMSCS mMSCSEnable: " + this.mMSCSEnable);
        int ret = RESULT_CODE_NO_EFFECT;
        if (!this.mMSCSEnable) {
            return ret;
        }
        String stream_timeout = String.valueOf(AudioServiceStubImpl.MUSIC_ACTIVE_CONTINUOUS_POLL_PERIOD_MS);
        Log.d(TAG, "enableMSCS interfaceName:" + interfaceName + " up_bitmap:" + MSCS_UP_BITMAP + " up_limit:" + MSCS_UP_LIMIT + " stream_timeout:" + stream_timeout + " frame_classifier:" + MSCS_FRAME_CLASSIFIER);
        MiuiWifiManager miuiWifiManager = this.mMiuiWifiManager;
        if (miuiWifiManager != null) {
            int ret2 = miuiWifiManager.changeMSCS(interfaceName, MSCS_UP_BITMAP, MSCS_UP_LIMIT, stream_timeout, MSCS_FRAME_CLASSIFIER);
            if (ret2 == RESULT_CODE_SUCCESS) {
                return ret2;
            }
            if (ret2 == RESULT_CODE_NO_EFFECT) {
                return this.mMiuiWifiManager.addMSCS(interfaceName, MSCS_UP_BITMAP, MSCS_UP_LIMIT, stream_timeout, MSCS_FRAME_CLASSIFIER);
            }
            return ret2;
        }
        return ret;
    }

    private int disableMSCS() {
        Log.d(TAG, "disableMSCS mMSCSEnable: " + this.mMSCSEnable);
        int ret = RESULT_CODE_NO_EFFECT;
        return ret;
    }

    private void registerNetworkCallback() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerNetworkInterfaceListener(this.mNetworkInterfaceListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterNetworkCallback() {
        try {
            if (this.mNetworkInterfaceListener != null) {
                StatusManager statusManager = StatusManager.getInstance(this.mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterNetworkInterfaceListener(this.mNetworkInterfaceListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int ret0 = MscsService.RESULT_CODE_NO_EFFECT;
                    int ret1 = MscsService.RESULT_CODE_NO_EFFECT;
                    if (MscsService.this.mMasterWifiUsed && MscsService.this.mMasterWifiSupport) {
                        ret0 = MscsService.this.enableMSCS("wlan0");
                    }
                    if (MscsService.this.mSlaveWifiUsed && MscsService.this.mSlaveWifiSupport) {
                        ret1 = MscsService.this.enableMSCS("wlan1");
                    }
                    if (ret0 == MscsService.RESULT_CODE_SUCCESS || ret1 == MscsService.RESULT_CODE_SUCCESS) {
                        MscsService.this.mHandler.sendEmptyMessageDelayed(1, 50000L);
                    }
                    MscsService.this.mMasterWifiSupport = ret0 == MscsService.RESULT_CODE_SUCCESS;
                    MscsService.this.mSlaveWifiSupport = ret1 == MscsService.RESULT_CODE_SUCCESS;
                    Log.d(MscsService.TAG, "mMasterWifiUsed:" + MscsService.this.mMasterWifiUsed + " mMasterWifiSupport:" + MscsService.this.mMasterWifiSupport + " mSlaveWifiUsed:" + MscsService.this.mSlaveWifiUsed + " mSlaveWifiSupport:" + MscsService.this.mSlaveWifiSupport);
                    Log.d(MscsService.TAG, "ret0:" + ret0 + " ret1:" + ret1);
                    return;
                case 2:
                    Log.d(MscsService.TAG, "removeMessages");
                    MscsService.this.mHandler.removeMessages(1);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWifi7MLO(WifiInfo wifiInfo) {
        if (wifiInfo != null && wifiInfo.getApMldMacAddress() != null) {
            return !wifiInfo.getApMldMacAddress().toString().equals("00:00:00:00:00:00");
        }
        return false;
    }

    private void registerScreenStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerScreenStatusListener(this.mScreenStatusListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterScreenStatusListener() {
        try {
            if (this.mScreenStatusListener != null) {
                StatusManager statusManager = StatusManager.getInstance(this.mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterScreenStatusListener(this.mScreenStatusListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }
}
