package com.xiaomi.NetworkBoost.NetworkAccelerateSwitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.SensorEvent;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.xiaomi.NetworkBoost.NetLinkLayerQoE;
import com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService;
import com.xiaomi.NetworkBoost.StatusManager;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import miui.process.ForegroundInfo;
import vendor.xiaomi.hidl.minet.V1_0.IMiNetCallback;
import vendor.xiaomi.hidl.minet.V1_0.IMiNetService;

/* loaded from: classes.dex */
public class NetworkAccelerateSwitchService {
    private static final int BAND_24_GHZ_END_FREQ_MHZ = 2484;
    private static final int BAND_24_GHZ_START_FREQ_MHZ = 2412;
    public static final int BASE = 1000;
    private static final String CLOUD_WEAK_NETWORK_SWITCH_ENABLED = "cloud_weak_network_switch_enabled";
    private static final String CLOUD_WEAK_NETWORK_SWITCH_HIGH_SPEED_MODE = "cloud_weak_network_switch_high_speed_mode";
    private static final int DEFAULT_FOREGROUND_UID = 0;
    private static final int DISABLE_NETWORK_SWITCH = 0;
    private static final int DUMP_MAX_COLUMN = 5;
    private static final int DUMP_MAX_ROW = 10;
    private static final int DUMP_SIZE_UPBOUND = 50;
    private static final int EFFECT_APP_STATUS_CHANGE_DELAY_MIILLIS = 5000;
    private static final String ELEVATOR_STATE = "elevatorState";
    private static final String ELEVATOR_STATE_SCENE = "com.android.phone.intent.action.ELEVATOR_STATE_SCENE";
    private static final int ENABLE_NETWORK_SWITCH = 1;
    private static final int ENTER_ELECATOR = 2;
    private static final int EVENT_CHECK_INTERFACE = 104;
    private static final int EVENT_EFFECT_APP_START = 105;
    private static final int EVENT_EFFECT_APP_STOP = 106;
    private static final int EVENT_GET_HAL = 100;
    private static final int EVENT_MOVEMENT = 109;
    private static final int EVENT_RSSI_POLL = 103;
    private static final int EVENT_SCREEN_OFF = 108;
    private static final int EVENT_SCREEN_ON = 107;
    private static final int EVENT_START_RSSI_POLL = 101;
    private static final int EVENT_STATIONARY = 110;
    private static final int EVENT_STOP_RSSI_POLL = 102;
    private static final int EXIT_ELECATOR = 1;
    private static final String FEATURE_STATUS = "com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.READY";
    private static final int GET_SERVICE_DELAY_MILLIS = 4000;
    private static final int GET_WIFI_RSSI_DELAY_MILLIS = 3000;
    private static final String LINKTURBO_IS_ENABLE = "linkturbo_is_enable";
    private static final int LONG_RSSI_QUEUE_SIZE = 7;
    private static final int LONG_RTT_QUEUE_SIZE = 7;
    public static final int MINETD_CMD_FLOWSTATGET = 1005;
    public static final int MINETD_CMD_FLOWSTATSTART = 1003;
    public static final int MINETD_CMD_FLOWSTATSTOP = 1004;
    public static final int MINETD_CMD_SETSOCKPRIO = 1001;
    public static final int MINETD_CMD_SETTCPCONGESTION = 1002;
    private static final int MOVEMENT_CHECK_TIME_DELAY = 3000;
    private static final int MOVEMENT_EXIT_TIME_DELAY = 30000;
    private static final int MOVEMENT_THRESHOLD_24GHZ = 6;
    private static final int MOVEMENT_THRESHOLD_5GHZ = 10;
    private static final String NETWORKBOOST_ACCELERATE_RSSI_INCREASE = "networkboot_rssi_increase";
    private static final String NETWORKBOOST_ACCELERATE_SCORE = "networkboost_sorce";
    private static final int NETWORK_CACL_RTT_START = 71;
    private static final int NETWORK_CACL_RTT_STOP = 67;
    private static final String NETWORK_SWITCH = "com.xiaomi.NetworkBoost.NetworkAccelerateSwitchService.action.enableNetworkSwitch";
    private static final int QOE_LOST_RATIO_QUEUE_SIZE = 4;
    private static final int SCREEN_OFF_DELAY_MIILLIS = 50000;
    private static final int SCREEN_ON_DELAY_MIILLIS = 10000;
    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;
    private static final int SHOART_RSSI_QUEUE_SIZE = 3;
    private static final int SHOART_RTT_QUEUE_SIZE = 3;
    private static final boolean START_COLLECT_IPADDRESS = true;
    private static final int START_NETWORK_SWITCH_CONDITIONS = 9000;
    private static final boolean STOP_COLLECT_IPADDRESS = false;
    private static final int STOP_NETWORK_SWITCH_CONDITIONS = 5000;
    private static final String TAG = "NetworkAccelerateSwitchService";
    private static final String TCP_BLACK_LIST = "8081";
    private static final int TCP_TARTGET_INDEX = 0;
    private static final String UDP_BLACK_LIST = "53";
    private static final int UDP_TARTGET_INDEX = 1;
    private static final int VOIP_RSSI_DEFAULT = 0;
    private static final int VOIP_RSSI_INCREASE = 3;
    private static final String WIFI_ASSISTANT = "wifi_assistant";
    private static final int WIFI_ASSISTANT_DEFAULT = 1;
    public static volatile NetworkAccelerateSwitchService sInstance;
    private Context mContext;
    private BroadcastReceiver mElevatorStatusReceiver;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private static boolean mWifiReady = false;
    private static boolean mSlaveWifiReady = false;
    private static boolean mRssiInRange = false;
    private static boolean mRssiPoll = false;
    private static String mInterface = "";
    private static int mIfaceNumber = 0;
    private static boolean mScreenon = false;
    private static IMiNetService mMiNetService = null;
    private static IMiNetCallback mMiNetCallback = null;
    private static int mEffectVoIPUid = 0;
    private static int mEffectForegroundUid = 0;
    private static HashSet<String> mEffectAppUidList = new HashSet<>();
    private WifiManager mWifiManager = null;
    private MiuiWifiManager mMiuiWifiManager = null;
    private ContentObserver mObserver = null;
    private volatile boolean mSwithInWeakNet = true;
    private boolean mSendBroadcast = false;
    private volatile boolean mMobileDataAlwaysonEnable = false;
    private StatusManager mStatusManager = null;
    private NetworkSDKService mNetworkSDKService = null;
    private int mAverageRssi = 0;
    Queue<Integer> mShortRssiQueue = new LinkedList();
    Queue<Integer> mLongRssiQueue = new LinkedList();
    Queue<Integer> mShortUnmeteredRttQueue = new LinkedList();
    Queue<Integer> mLongUnmeteredRttQueue = new LinkedList();
    Queue<Double> mQoeLostRatioQueue = new LinkedList();
    private boolean misEvevator = false;
    private boolean misHighRtt = false;
    private ContentObserver mMovementObserver = null;
    private int mMovement24GHzThreshold = 6;
    private int mMovement5GHzThreshold = 10;
    private boolean mis24GHz = false;
    private IHwBinder.DeathRecipient mDeathRecipient = new MiNetServiceDeathRecipient();
    private StatusManager.IAppStatusListener mAppStatusListener = new StatusManager.IAppStatusListener() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.2
        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            if (NetworkAccelerateSwitchService.mEffectForegroundUid != NetworkAccelerateSwitchService.mEffectVoIPUid && NetworkAccelerateSwitchService.mEffectForegroundUid == foregroundInfo.mLastForegroundUid) {
                NetworkAccelerateSwitchService.this.setNetworkAccelerateSwitchAppStop(foregroundInfo.mLastForegroundUid);
            }
            if (NetworkAccelerateSwitchService.mEffectAppUidList.contains(Integer.toString(foregroundInfo.mForegroundUid))) {
                NetworkAccelerateSwitchService.this.setNetworkAccelerateSwitchAppStart(foregroundInfo.mForegroundUid);
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onUidGone(int uid, boolean disabled) {
            if (NetworkAccelerateSwitchService.mEffectForegroundUid == uid) {
                NetworkAccelerateSwitchService.this.setNetworkAccelerateSwitchAppStop(uid);
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onAudioChanged(int mode) {
            if (mode == 3 && NetworkAccelerateSwitchService.mEffectVoIPUid != 0 && NetworkAccelerateSwitchService.mEffectForegroundUid == 0) {
                Log.i(NetworkAccelerateSwitchService.TAG, "mEffectVoIPUid:" + NetworkAccelerateSwitchService.mEffectVoIPUid + " mEffectForegroundUid " + NetworkAccelerateSwitchService.mEffectForegroundUid);
                NetworkAccelerateSwitchService.this.setNetworkAccelerateSwitchAppStart(NetworkAccelerateSwitchService.mEffectVoIPUid);
            } else if (mode == 0 && NetworkAccelerateSwitchService.mEffectVoIPUid != 0 && NetworkAccelerateSwitchService.mEffectForegroundUid == NetworkAccelerateSwitchService.mEffectVoIPUid) {
                Log.i(NetworkAccelerateSwitchService.TAG, "mEffectVoIPUid:" + NetworkAccelerateSwitchService.mEffectVoIPUid + " mEffectForegroundUid " + NetworkAccelerateSwitchService.mEffectForegroundUid);
                NetworkAccelerateSwitchService.this.setNetworkAccelerateSwitchAppStop(NetworkAccelerateSwitchService.mEffectVoIPUid);
            }
        }
    };
    private StatusManager.INetworkInterfaceListener mNetworkInterfaceListener = new StatusManager.INetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.3
        @Override // com.xiaomi.NetworkBoost.StatusManager.INetworkInterfaceListener
        public void onNetwrokInterfaceChange(String ifacename, int ifacenum, boolean wifiready, boolean slavewifiready, boolean dataready, boolean slaveDataReady, String status) {
            String[] arr = ifacename.split(",", ifacenum);
            String newstring = "";
            int newcnt = 0;
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].indexOf("rmnet_data") < 0 && arr[i].indexOf("ccmni") < 0 && arr[i].indexOf("wlan") >= 0) {
                    arr[i] = arr[i].replaceAll(",", "");
                    newstring = newstring + arr[i] + ",";
                    newcnt++;
                }
            }
            NetworkAccelerateSwitchService.mInterface = newstring;
            NetworkAccelerateSwitchService.mIfaceNumber = newcnt;
            NetworkAccelerateSwitchService.mWifiReady = wifiready;
            NetworkAccelerateSwitchService.mSlaveWifiReady = slavewifiready;
            NetworkAccelerateSwitchService.this.checkRssiPollStartConditions();
            NetworkAccelerateSwitchService.this.checkRttPollStartConditions();
            NetworkAccelerateSwitchService.this.checkWifiBand();
        }
    };
    private StatusManager.IDefaultNetworkInterfaceListener mDefaultNetworkInterfaceListener = new StatusManager.IDefaultNetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.4
        @Override // com.xiaomi.NetworkBoost.StatusManager.IDefaultNetworkInterfaceListener
        public void onDefaultNetwrokChange(String ifacename, int ifacestatus) {
            if (NetworkAccelerateSwitchService.mWifiReady && ifacestatus != 1) {
                NetworkAccelerateSwitchService.mWifiReady = false;
                NetworkAccelerateSwitchService.mInterface = NetworkAccelerateSwitchService.mInterface.replaceAll("wlan0,", "");
                NetworkAccelerateSwitchService.mIfaceNumber--;
            } else if (!NetworkAccelerateSwitchService.mWifiReady && ifacestatus == 1) {
                NetworkAccelerateSwitchService.mWifiReady = true;
                NetworkAccelerateSwitchService.mInterface += ifacename + ",";
                NetworkAccelerateSwitchService.mIfaceNumber++;
            } else {
                return;
            }
            Log.i(NetworkAccelerateSwitchService.TAG, "Default Netwrok Change mInterface:" + NetworkAccelerateSwitchService.mInterface + " mWifiReady:" + NetworkAccelerateSwitchService.mWifiReady);
            NetworkAccelerateSwitchService.this.checkRssiPollStartConditions();
            NetworkAccelerateSwitchService.this.checkRttPollStartConditions();
        }
    };
    private StatusManager.IScreenStatusListener mScreenStatusListener = new StatusManager.IScreenStatusListener() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.5
        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenON() {
            NetworkAccelerateSwitchService.this.mHandler.removeMessages(107);
            NetworkAccelerateSwitchService.this.mHandler.removeMessages(108);
            NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessageDelayed(107, 10000L);
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenOFF() {
            NetworkAccelerateSwitchService.this.mHandler.removeMessages(107);
            NetworkAccelerateSwitchService.this.mHandler.removeMessages(108);
            NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessageDelayed(108, 50000L);
        }
    };
    private boolean mMovement = false;
    private int mMovementState = 0;
    private StatusManager.IMovementSensorStatusListener mMovementSensorStatusListener = new StatusManager.IMovementSensorStatusListener() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.8
        @Override // com.xiaomi.NetworkBoost.StatusManager.IMovementSensorStatusListener
        public void onSensorChanged(SensorEvent event) {
            int type = event.sensor.getType();
            NetworkAccelerateSwitchService.this.mMovementState = (int) event.values[0];
            Log.i(NetworkAccelerateSwitchService.TAG, "onMovementSensorChanged:" + type + " State:" + NetworkAccelerateSwitchService.this.mMovementState);
        }
    };

    private native int deinitNetworkAccelerateSwitch();

    private native int initNetworkAccelerateSwitch();

    private native String nativeDump();

    private native int setRttTarget(String str, int i, String str2, int i2, int i3);

    private native int startRttCollection(String str);

    private native int stopRttCollection();

    public static NetworkAccelerateSwitchService getInstance(Context context, IMiNetCallback minetcallback) {
        if (sInstance == null) {
            synchronized (NetworkAccelerateSwitchService.class) {
                if (sInstance == null) {
                    sInstance = new NetworkAccelerateSwitchService(context, minetcallback);
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
            synchronized (NetworkAccelerateSwitchService.class) {
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

    private NetworkAccelerateSwitchService(Context context, IMiNetCallback minetcallback) {
        this.mContext = null;
        this.mContext = context.getApplicationContext();
        mMiNetCallback = minetcallback;
    }

    public void onCreate() {
        Log.i(TAG, "onCreate ");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        HandlerThread handlerThread = new HandlerThread("NetworkAccelerateSwitchHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        InternalHandler internalHandler = new InternalHandler(this.mHandlerThread.getLooper());
        this.mHandler = internalHandler;
        internalHandler.sendEmptyMessage(100);
        initNetworkAccelerateSwitch();
        registerNetworkSDK();
        registerNetworkSwitchModeChangedObserver();
        registerAppStatusListener();
        registerNetworkCallback();
        registerDefaultNetworkCallback();
        registerScreenStatusListener();
        registerHighSpeedModeChangedObserver();
        registerElevatorStatusReceiver();
        registerMovementSensorStatusListener();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        unregisterNetworkSwitchModeChangedObserver();
        unregisterAppStatusListener();
        unregisterNetworkCallback();
        unregisterDefaultNetworkCallback();
        unregisterScreenStatusListener();
        unregisterHighSpeedModeChangedObserver();
        unregisterElevatorStatusReceiver();
        unregisterMovementSensorStatusListener();
        IMiNetService iMiNetService = mMiNetService;
        if (iMiNetService != null) {
            try {
                iMiNetService.unregisterCallback(mMiNetCallback);
                mMiNetService.stopMiNetd();
                mMiNetService.unlinkToDeath(this.mDeathRecipient);
            } catch (Exception e) {
            }
        }
        HandlerThread handlerThread = this.mHandlerThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        this.mHandler = null;
        deinitNetworkAccelerateSwitch();
    }

    /* loaded from: classes.dex */
    class MiNetServiceDeathRecipient implements IHwBinder.DeathRecipient {
        MiNetServiceDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Log.e(NetworkAccelerateSwitchService.TAG, "HAL service died");
            NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessageDelayed(100, 4000L);
        }
    }

    private void registerNetworkSDK() {
        try {
            this.mNetworkSDKService = NetworkSDKService.getInstance(this.mContext);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private boolean startCollectIpAddress(int uid) {
        Log.i(TAG, "startCollectIpAddress " + uid);
        IMiNetService iMiNetService = mMiNetService;
        if (iMiNetService != null && mMiNetCallback != null) {
            try {
                iMiNetService.setCommon(1003, Integer.toString(uid));
                mMiNetService.setCommon(1005, Integer.toString(uid));
                mMiNetService.registerCallback(mMiNetCallback);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return true;
            }
        }
        return true;
    }

    private boolean stopCollectIpAddress(int uid) {
        Log.i(TAG, "stopCollectIpAddress " + uid);
        IMiNetService iMiNetService = mMiNetService;
        if (iMiNetService != null) {
            try {
                iMiNetService.setCommon(1004, Integer.toString(uid));
                mMiNetService.unregisterCallback(mMiNetCallback);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return true;
            }
        }
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x00e7 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:39:0x00f8 A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean notifyCollectIpAddress(java.lang.String r17) {
        /*
            Method dump skipped, instructions count: 262
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.notifyCollectIpAddress(java.lang.String):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEnableAccelerationSwitch() {
        boolean isWifiAssistEnable = Settings.System.getInt(this.mContext.getContentResolver(), WIFI_ASSISTANT, 1) == 1;
        if (!isWifiAssistEnable) {
            return false;
        }
        String cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_WEAK_NETWORK_SWITCH_ENABLED, -2);
        boolean enable = Settings.System.getIntForUser(this.mContext.getContentResolver(), LINKTURBO_IS_ENABLE, 0, -2) == 1;
        if (cvalue != null && AmapExtraCommand.VERSION_NAME.equals(cvalue) && !enable) {
            boolean z = mSlaveWifiReady;
            if (!z && mWifiReady) {
                return true;
            }
            if (z && !mWifiReady) {
                return true;
            }
        }
        return false;
    }

    private void registerNetworkSwitchModeChangedObserver() {
        this.mObserver = new ContentObserver(this.mHandler) { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                NetworkAccelerateSwitchService.this.checkRssiPollStartConditions();
                NetworkAccelerateSwitchService.this.checkRttPollStartConditions();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_WEAK_NETWORK_SWITCH_ENABLED), false, this.mObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(WIFI_ASSISTANT), false, this.mObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(LINKTURBO_IS_ENABLE), false, this.mObserver, -2);
        this.mObserver.onChange(false);
    }

    private void unregisterNetworkSwitchModeChangedObserver() {
        Context context;
        if (this.mObserver != null && (context = this.mContext) != null) {
            context.getContentResolver().unregisterContentObserver(this.mObserver);
            this.mObserver = null;
        } else {
            Log.w(TAG, "deinitCloudObserver ignore because: observer: " + this.mObserver + " context: " + this.mContext);
        }
    }

    public static void setNetworkAccelerateSwitchUidList(String uidList) {
        Log.i(TAG, "setNetworkAccelerateSwitchUidList:" + uidList);
        if (uidList == null) {
            return;
        }
        mEffectAppUidList.clear();
        String[] temp = uidList.split(",");
        for (String str : temp) {
            mEffectAppUidList.add(str);
        }
    }

    public static void setNetworkAccelerateSwitchVoIPUid(int uid) {
        Log.i(TAG, "setNetworkAccelerateSwitchVoIPUid:" + uid);
        mEffectVoIPUid = uid;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNetworkAccelerateSwitchAppStart(int uid) {
        this.mHandler.removeMessages(105);
        this.mHandler.removeMessages(106);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(105, Integer.valueOf(uid)), 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNetworkAccelerateSwitchAppStop(int uid) {
        this.mHandler.removeMessages(105);
        this.mHandler.removeMessages(106);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(106, Integer.valueOf(uid)), 5000L);
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

    private void unregisterAppStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            StatusManager.IAppStatusListener iAppStatusListener = this.mAppStatusListener;
            if (iAppStatusListener != null) {
                statusManager.unregisterAppStatusListener(iAppStatusListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
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

    private void registerDefaultNetworkCallback() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerDefaultNetworkInterfaceListener(this.mDefaultNetworkInterfaceListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterDefaultNetworkCallback() {
        try {
            if (this.mDefaultNetworkInterfaceListener != null) {
                StatusManager statusManager = StatusManager.getInstance(this.mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterDefaultNetworkInterfaceListener(this.mDefaultNetworkInterfaceListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
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

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    try {
                        NetworkAccelerateSwitchService.mMiNetService = IMiNetService.getService();
                        if (NetworkAccelerateSwitchService.mMiNetService == null) {
                            Log.e(NetworkAccelerateSwitchService.TAG, "HAL service is null");
                            NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessageDelayed(100, 4000L);
                        } else {
                            Log.d(NetworkAccelerateSwitchService.TAG, "HAL service get success");
                            NetworkAccelerateSwitchService.mMiNetService.linkToDeath(NetworkAccelerateSwitchService.this.mDeathRecipient, 0L);
                            NetworkAccelerateSwitchService.mMiNetService.startMiNetd();
                        }
                        return;
                    } catch (Exception e) {
                        Log.e(NetworkAccelerateSwitchService.TAG, "Exception:" + e);
                        return;
                    }
                case 101:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_START_RSSI_POLL");
                    NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessage(103);
                    return;
                case 102:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_STOP_RSSI_POLL");
                    NetworkAccelerateSwitchService.this.restoreMovementState();
                    return;
                case 103:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_RSSI_POLL. mRssiPoll=" + NetworkAccelerateSwitchService.mRssiPoll);
                    int rssi = NetworkAccelerateSwitchService.this.getAverageWifiRssi();
                    NetworkAccelerateSwitchService.this.checkRssiInRange(rssi);
                    NetworkAccelerateSwitchService.this.checkRssiChange();
                    if (NetworkAccelerateSwitchService.mRssiPoll) {
                        NetworkAccelerateSwitchService.this.mHandler.sendEmptyMessageDelayed(103, NetworkAccelerateSwitchService.this.mMovement ? 1000L : 3000L);
                        return;
                    }
                    return;
                case 104:
                default:
                    return;
                case 105:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_EFFECT_APP_START");
                    NetworkAccelerateSwitchService.this.checkCollectIpAddress(((Integer) msg.obj).intValue(), true);
                    return;
                case 106:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_EFFECT_APP_STOP");
                    NetworkAccelerateSwitchService.this.checkCollectIpAddress(((Integer) msg.obj).intValue(), false);
                    return;
                case 107:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_SCREEN_ON");
                    NetworkAccelerateSwitchService.mScreenon = true;
                    NetworkAccelerateSwitchService.this.checkRssiPollStartConditions();
                    NetworkAccelerateSwitchService.this.checkRttPollStartConditions();
                    return;
                case 108:
                    Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_SCREEN_OFF");
                    NetworkAccelerateSwitchService.mScreenon = false;
                    NetworkAccelerateSwitchService.this.checkRssiPollStartConditions();
                    NetworkAccelerateSwitchService.this.checkRttPollStartConditions();
                    return;
                case 109:
                    if (NetworkAccelerateSwitchService.this.mMiuiWifiManager != null) {
                        Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_MOVEMENT movement set Poll Rssi Interval 1000ms");
                        NetworkAccelerateSwitchService.this.mHandler.removeMessages(109);
                        NetworkAccelerateSwitchService.this.mMiuiWifiManager.setPollRssiIntervalMillis(1000);
                        NetworkAccelerateSwitchService.this.mMovement = true;
                        return;
                    }
                    return;
                case 110:
                    if (NetworkAccelerateSwitchService.this.mMiuiWifiManager != null) {
                        Log.i(NetworkAccelerateSwitchService.TAG, "EVENT_STATIONARY not movement set Poll Rssi Interval 3000ms");
                        NetworkAccelerateSwitchService.this.mHandler.removeMessages(110);
                        NetworkAccelerateSwitchService.this.mMiuiWifiManager.setPollRssiIntervalMillis(3000);
                        NetworkAccelerateSwitchService.this.mMovement = false;
                        return;
                    }
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getAverageWifiRssi() {
        int rssi = 0;
        int longSum = 0;
        int sortSum = 0;
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            rssi = Math.abs(wifiInfo.getRssi());
        }
        if (this.mShortRssiQueue.size() >= 3) {
            this.mShortRssiQueue.poll();
        }
        this.mShortRssiQueue.offer(Integer.valueOf(rssi));
        if (this.mLongRssiQueue.size() >= 7) {
            this.mLongRssiQueue.poll();
        }
        this.mLongRssiQueue.offer(Integer.valueOf(rssi));
        Iterator<Integer> it = this.mShortRssiQueue.iterator();
        while (it.hasNext()) {
            int a = it.next().intValue();
            sortSum += a;
        }
        Iterator<Integer> it2 = this.mLongRssiQueue.iterator();
        while (it2.hasNext()) {
            int a2 = it2.next().intValue();
            longSum += a2;
        }
        int averageRssi = (((sortSum / this.mShortRssiQueue.size()) * 75) + ((longSum / this.mLongRssiQueue.size()) * 25)) / 100;
        Log.i(TAG, "getAverageWifiRssi:" + averageRssi);
        this.mAverageRssi = averageRssi;
        return averageRssi;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRssiPollStartConditions() {
        Log.i(TAG, "checkRssiPollStartConditions  mWifiReady:" + mWifiReady + " mRssiPoll:" + mRssiPoll + " checkScreenStatus():" + checkScreenStatus() + " isEnableAccelerationSwitch():" + isEnableAccelerationSwitch());
        if (this.mHandler == null) {
            Log.e(TAG, "checkRssiPollStartConditions handler is null");
            return;
        }
        if (!mRssiPoll && mWifiReady && checkScreenStatus() && isEnableAccelerationSwitch()) {
            this.mHandler.sendEmptyMessage(101);
            mRssiPoll = true;
        } else if (mRssiPoll) {
            if (!mWifiReady || !checkScreenStatus() || !isEnableAccelerationSwitch()) {
                this.mHandler.removeMessages(103);
                this.mHandler.sendEmptyMessage(102);
                mRssiPoll = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRssiInRange(int rssi) {
        boolean rssiinrange = mRssiInRange;
        if (rssi > NETWORK_CACL_RTT_START) {
            mRssiInRange = true;
        } else if (rssi < NETWORK_CACL_RTT_STOP) {
            mRssiInRange = false;
        }
        if (rssiinrange != mRssiInRange) {
            checkRttPollStartConditions();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCollectIpAddress(int uid, boolean opt) {
        boolean chagne = false;
        if (opt && uid != 0 && uid != mEffectForegroundUid && isEnableAccelerationSwitch()) {
            startCollectIpAddress(uid);
            mEffectForegroundUid = uid;
            chagne = true;
        } else if (!opt && mEffectForegroundUid != 0) {
            stopCollectIpAddress(uid);
            mEffectForegroundUid = 0;
            chagne = true;
        }
        if (chagne) {
            checkRttPollStartConditions();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkScreenStatus() {
        int i = mEffectForegroundUid;
        int i2 = mEffectVoIPUid;
        if (i == i2 && i2 != 0) {
            return true;
        }
        return mScreenon;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkWifiBand() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (is24GHz(wifiInfo.getFrequency())) {
                this.mis24GHz = true;
            } else {
                this.mis24GHz = false;
            }
        }
    }

    private static boolean is24GHz(int freqMhz) {
        return freqMhz >= BAND_24_GHZ_START_FREQ_MHZ && freqMhz <= BAND_24_GHZ_END_FREQ_MHZ;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRttPollStartConditions() {
        Log.i(TAG, "checkRttPollStartConditions  mWifiReady:" + mWifiReady + " mRssiPoll:" + mRssiPoll + " mRssiInRange:" + mRssiInRange + " mEffectForegroundUid:" + mEffectForegroundUid + " checkScreenStatus():" + checkScreenStatus() + " isEnableAccelerationSwitch():" + isEnableAccelerationSwitch());
        if (isEnableAccelerationSwitch() && mWifiReady && mRssiPoll && mRssiInRange && mEffectForegroundUid != 0 && checkScreenStatus()) {
            Log.i(TAG, "checkRttPollStartConditions start");
            startRttCollection(mInterface);
        } else {
            this.misHighRtt = false;
            stopRttCollection();
            disabelNetworkSwitch();
            Log.i(TAG, "checkRttPollStartConditions stop");
        }
    }

    /* loaded from: classes.dex */
    public class RttResult {
        public String interfaceName;
        public int rtt;

        public RttResult(int _rtt, String _interfaceName) {
            this.rtt = _rtt;
            this.interfaceName = _interfaceName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" RttResult { ").append(" rtt = " + this.rtt).append(" interfaceName = " + this.interfaceName).append(" }");
            return sb.toString();
        }
    }

    public int notifyRttInfo(RttResult[] results) {
        NetLinkLayerQoE resultQOE;
        int shortunmeterdrtt = 0;
        int longunmeterdrtt = 0;
        int tmp_rtt = 0;
        int tmp_rtt_count = 0;
        for (RttResult rttResult : results) {
            tmp_rtt += rttResult.rtt;
            tmp_rtt_count++;
        }
        if (tmp_rtt_count <= 0) {
            Log.e(TAG, "tmp_rtt /= tmp_rtt_count error!");
            return -1;
        }
        int tmp_rtt2 = tmp_rtt / tmp_rtt_count;
        Log.i(TAG, "tmp_rtt: " + tmp_rtt2);
        if (tmp_rtt2 <= 500 && tmp_rtt2 != 0) {
            for (RttResult ret : results) {
                Log.i(TAG, "ret: " + ret);
                if (ret.interfaceName.startsWith("wlan")) {
                    if (this.mShortUnmeteredRttQueue.size() >= 3) {
                        this.mShortUnmeteredRttQueue.poll();
                    }
                    this.mShortUnmeteredRttQueue.offer(Integer.valueOf(ret.rtt));
                    if (this.mLongUnmeteredRttQueue.size() >= 7) {
                        this.mLongUnmeteredRttQueue.poll();
                    }
                    this.mLongUnmeteredRttQueue.offer(Integer.valueOf(ret.rtt));
                }
            }
        }
        NetworkSDKService networkSDKService = this.mNetworkSDKService;
        if (networkSDKService != null && (resultQOE = networkSDKService.getQoEByAvailableIfaceNameV1(StatusManager.WLAN_IFACE_0)) != null) {
            double lostRatio = resultQOE.getMpduLostRatio() * 100.0d;
            if (this.mQoeLostRatioQueue.size() >= 4) {
                this.mQoeLostRatioQueue.poll();
            }
            this.mQoeLostRatioQueue.offer(Double.valueOf(lostRatio));
            if (this.mQoeLostRatioQueue.size() == 4) {
                double averageLostRatio = 0.0d;
                Iterator<Double> it = this.mQoeLostRatioQueue.iterator();
                while (it.hasNext()) {
                    double a = it.next().doubleValue();
                    averageLostRatio += a;
                }
                double averageLostRatio2 = averageLostRatio / this.mQoeLostRatioQueue.size();
                Log.i(TAG, "averageLostRatio: " + averageLostRatio2);
                if (averageLostRatio2 >= 15.0d && !this.misHighRtt) {
                    Log.i(TAG, "enable net weak! averageLostRatio: " + averageLostRatio2);
                    this.misHighRtt = true;
                    enabelNetworkSwitch();
                    return 0;
                }
            }
        }
        if (this.mShortUnmeteredRttQueue.size() != 0 && this.mLongUnmeteredRttQueue.size() != 0) {
            Iterator<Integer> it2 = this.mShortUnmeteredRttQueue.iterator();
            while (it2.hasNext()) {
                int a2 = it2.next().intValue();
                shortunmeterdrtt += a2;
            }
            Iterator<Integer> it3 = this.mLongUnmeteredRttQueue.iterator();
            while (it3.hasNext()) {
                int a3 = it3.next().intValue();
                longunmeterdrtt += a3;
            }
            int unmeterdaveragertt = ((shortunmeterdrtt / this.mShortUnmeteredRttQueue.size()) + (longunmeterdrtt / this.mLongUnmeteredRttQueue.size())) / 2;
            int calcunmeterdaverage = (unmeterdaveragertt * 100) + (this.mAverageRssi * unmeterdaveragertt);
            Log.i(TAG, "results:end unmeterdaveragertt:" + unmeterdaveragertt);
            Log.i(TAG, "results:end calcunmeterdaverage:" + (calcunmeterdaverage / 100));
            if (calcunmeterdaverage > 9000 && !this.misHighRtt) {
                Log.i(TAG, "enable net weak! averagertt:" + (calcunmeterdaverage / 100));
                this.misHighRtt = true;
                enabelNetworkSwitch();
                return 0;
            }
            if (calcunmeterdaverage < 5000 && this.misHighRtt) {
                Log.i(TAG, "disable net weak! averagertt:" + (calcunmeterdaverage / 100));
                this.misHighRtt = false;
                disabelNetworkSwitch();
                return 0;
            }
        }
        return 0;
    }

    private void registerHighSpeedModeChangedObserver() {
        this.mMovementObserver = new ContentObserver(this.mHandler) { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                NetworkAccelerateSwitchService.this.checkHighSpeedModeCloudController();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_WEAK_NETWORK_SWITCH_HIGH_SPEED_MODE), false, this.mMovementObserver, -2);
        this.mMovementObserver.onChange(false);
    }

    private void unregisterHighSpeedModeChangedObserver() {
        Context context;
        if (this.mMovementObserver != null && (context = this.mContext) != null) {
            context.getContentResolver().unregisterContentObserver(this.mMovementObserver);
            this.mMovementObserver = null;
        } else {
            Log.w(TAG, "deinitCloudObserver ignore because: observer: " + this.mMovementObserver + " context: " + this.mContext);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkHighSpeedModeCloudController() {
        String[] controllers;
        this.mMovement24GHzThreshold = 6;
        this.mMovement5GHzThreshold = 10;
        String cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_WEAK_NETWORK_SWITCH_HIGH_SPEED_MODE, -2);
        if (cvalue == null || cvalue.indexOf("v1") == -1) {
            restoreMovementState();
            return;
        }
        try {
            if (!TextUtils.isEmpty(cvalue) && (controllers = cvalue.split(",")) != null && controllers.length == 3) {
                this.mMovement24GHzThreshold = Integer.parseInt(controllers[1]);
                this.mMovement5GHzThreshold = Integer.parseInt(controllers[2]);
            }
        } catch (Exception e) {
            Log.e(TAG, "cloud observer catch:", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEnableAccelerationSwitchHighSpeedMode() {
        String cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_WEAK_NETWORK_SWITCH_HIGH_SPEED_MODE, -2);
        return (cvalue == null || cvalue.indexOf("v1") == -1) ? false : true;
    }

    private void registerElevatorStatusReceiver() {
        this.mElevatorStatusReceiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (NetworkAccelerateSwitchService.this.isEnableAccelerationSwitchHighSpeedMode() && NetworkAccelerateSwitchService.this.isEnableAccelerationSwitch() && NetworkAccelerateSwitchService.ELEVATOR_STATE_SCENE.equals(action) && NetworkAccelerateSwitchService.mWifiReady && NetworkAccelerateSwitchService.this.checkScreenStatus()) {
                    Bundle bundle = intent.getExtras();
                    int status = bundle.getInt(NetworkAccelerateSwitchService.ELEVATOR_STATE);
                    Log.i(NetworkAccelerateSwitchService.TAG, "elevator status:" + status);
                    if (status == 2 && !NetworkAccelerateSwitchService.this.misEvevator) {
                        NetworkAccelerateSwitchService.this.misEvevator = true;
                        NetworkAccelerateSwitchService.this.enabelNetworkSwitch();
                        return;
                    } else {
                        if (status == 1 && NetworkAccelerateSwitchService.this.misEvevator) {
                            NetworkAccelerateSwitchService.this.misEvevator = false;
                            NetworkAccelerateSwitchService.this.disabelNetworkSwitch();
                            return;
                        }
                        return;
                    }
                }
                NetworkAccelerateSwitchService.this.misEvevator = false;
                NetworkAccelerateSwitchService.this.disabelNetworkSwitch();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ELEVATOR_STATE_SCENE);
        this.mContext.registerReceiver(this.mElevatorStatusReceiver, filter);
    }

    private void unregisterElevatorStatusReceiver() {
        BroadcastReceiver broadcastReceiver = this.mElevatorStatusReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "unregisterElevatorStatusReceiver");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enabelNetworkSwitch() {
        Log.d(TAG, "enabelNetworkSwitch mSendBroadcast:" + this.mSendBroadcast + " misEvevator:" + this.misEvevator + " misHighRtt:" + this.misHighRtt);
        if (this.mSendBroadcast) {
            return;
        }
        if (this.misEvevator || this.misHighRtt) {
            Intent intent = new Intent();
            intent.setAction(NETWORK_SWITCH);
            intent.putExtra(NETWORKBOOST_ACCELERATE_SCORE, 1);
            intent.putExtra(NETWORKBOOST_ACCELERATE_RSSI_INCREASE, this.misHighRtt ? 3 : 0);
            intent.addFlags(268435456);
            this.mContext.sendBroadcast(intent);
            this.mSendBroadcast = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disabelNetworkSwitch() {
        Log.d(TAG, "disabelNetworkSwitch mSendBroadcast:" + this.mSendBroadcast + " misEvevator:" + this.misEvevator + " misHighRtt:" + this.misHighRtt);
        if (this.mSendBroadcast && !this.misEvevator && !this.misHighRtt) {
            Intent intent = new Intent();
            intent.setAction(NETWORK_SWITCH);
            intent.putExtra(NETWORKBOOST_ACCELERATE_SCORE, 0);
            intent.putExtra(NETWORKBOOST_ACCELERATE_RSSI_INCREASE, 0);
            intent.addFlags(268435456);
            this.mContext.sendBroadcast(intent);
            this.mSendBroadcast = false;
        }
    }

    private void registerMovementSensorStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerMovementSensorStatusListener(this.mMovementSensorStatusListener);
            this.mMiuiWifiManager = (MiuiWifiManager) this.mContext.getSystemService("MiuiWifiService");
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterMovementSensorStatusListener() {
        try {
            if (this.mMovementSensorStatusListener != null) {
                StatusManager statusManager = StatusManager.getInstance(this.mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterMovementSensorStatusListener(this.mMovementSensorStatusListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRssiChange() {
        if (!isEnableAccelerationSwitch() || !isEnableAccelerationSwitchHighSpeedMode()) {
            return;
        }
        Log.i(TAG, "checkRssiChange mMovementState:" + this.mMovementState + " mMovement:" + this.mMovement);
        int i = this.mMovementState;
        if (i != 0 && !this.mMovement) {
            if (checkMovement(this.mShortRssiQueue)) {
                this.mHandler.sendEmptyMessage(109);
            }
        } else if (i != 0 && this.mMovement) {
            this.mHandler.removeMessages(110);
        } else if (i == 0 && this.mMovement) {
            this.mHandler.sendEmptyMessageDelayed(110, 30000L);
        }
    }

    public boolean checkMovement(Queue<Integer> rssiQueue) {
        int rssiChange = 0;
        int rssi = 0;
        if (rssiQueue.size() < 3) {
            return false;
        }
        Iterator<Integer> it = rssiQueue.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            int a = it.next().intValue();
            Log.d(TAG, "checkMovement:" + a);
            if (rssi == 0) {
                rssi = a;
            } else {
                if (a < rssi) {
                    rssiChange = 0;
                    break;
                }
                int rssiChange2 = (a - rssi) + rssiChange;
                rssi = a;
                rssiChange = rssiChange2;
            }
        }
        Log.i(TAG, "checkMovement rssiChange:" + rssiChange + " getMovementThreshold:" + getMovementThreshold());
        if (rssiChange <= getMovementThreshold()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreMovementState() {
        if (this.mMovement) {
            this.mHandler.sendEmptyMessage(110);
            this.mMovement = false;
        }
    }

    private int getMovementThreshold() {
        if (this.mis24GHz) {
            return this.mMovement24GHzThreshold;
        }
        return this.mMovement5GHzThreshold;
    }

    private void dumpIntegerQueue(PrintWriter writer, String spacePrefix, String name, Queue<Integer> dumpQueue) {
        int dump_cnt = 0;
        writer.println(spacePrefix + name + ":" + dumpQueue.size());
        writer.print(spacePrefix);
        Iterator<Integer> it = dumpQueue.iterator();
        while (it.hasNext()) {
            int item = it.next().intValue();
            if (dump_cnt % 5 != 0) {
                writer.print(", ");
            }
            writer.print("" + item);
            dump_cnt++;
            if (dump_cnt >= 50) {
                break;
            } else if (dump_cnt % 5 == 0) {
                writer.println(",");
                writer.print(spacePrefix);
            }
        }
        writer.println("");
    }

    public void dumpModule(PrintWriter writer) {
        try {
            writer.println("NetworkAccelerateSwitchService begin:");
            writer.println("    mWifiReady:" + mWifiReady);
            writer.println("    mSlaveWifiReady:" + mSlaveWifiReady);
            writer.println("    mRssiInRange:" + mRssiInRange);
            writer.println("    mInterface:" + mInterface);
            writer.println("    mIfaceNumber:" + mIfaceNumber);
            writer.println("    mScreenon:" + mScreenon);
            writer.println("    DUMP_SIZE_UPBOUND:50");
            int dump_cnt = 0;
            writer.println("    mEffectAppUidList:" + mEffectAppUidList.size());
            writer.print("    ");
            Iterator<String> it = mEffectAppUidList.iterator();
            while (it.hasNext()) {
                String str_uid = it.next();
                if (dump_cnt % 5 != 0) {
                    writer.print(", ");
                }
                writer.print(str_uid);
                dump_cnt++;
                if (dump_cnt >= 50) {
                    break;
                } else if (dump_cnt % 5 == 0) {
                    writer.println(",");
                    writer.print("    ");
                }
            }
            writer.println("");
            writer.println("    mEffectVoIPUid:" + mEffectVoIPUid);
            writer.println("    mEffectForegroundUid:" + mEffectForegroundUid);
            dumpIntegerQueue(writer, "    ", "mShortRssiQueue", this.mShortRssiQueue);
            dumpIntegerQueue(writer, "    ", "mLongRssiQueue", this.mLongRssiQueue);
            dumpIntegerQueue(writer, "    ", "mShortUnmeteredRttQueue", this.mShortUnmeteredRttQueue);
            dumpIntegerQueue(writer, "    ", "mLongUnmeteredRttQueue", this.mLongUnmeteredRttQueue);
            writer.println("    mAverageRssi:" + this.mAverageRssi);
            writer.println("    UDP_BLACK_LIST:" + UDP_BLACK_LIST);
            writer.println("    TCP_BLACK_LIST:" + TCP_BLACK_LIST);
            String nativeDumpStr = nativeDump();
            writer.println(nativeDumpStr != null ? nativeDumpStr : "    native dump is null");
            writer.println("NetworkAccelerateSwitchService end.\n");
        } catch (Exception ex) {
            Log.e(TAG, "dump failed!", ex);
            try {
                writer.println("Dump of NetworkAccelerateSwitchService failed:" + ex);
                writer.println("NetworkAccelerateSwitchService end.\n");
            } catch (Exception e) {
                Log.e(TAG, "dump failure failed:" + e);
            }
        }
    }
}
