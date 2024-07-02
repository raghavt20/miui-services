package com.xiaomi.NetworkBoost.NetworkSDK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SlaveWifiManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.WlanLinkLayerQoE;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.xiaomi.NetworkBoost.DualCelluarDataService.DualCelluarDataService;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback;
import com.xiaomi.NetworkBoost.NetLinkLayerQoE;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import com.xiaomi.NetworkBoost.StatusManager;
import com.xiaomi.NetworkBoost.slaservice.SLAService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.android.animation.controller.AnimState;
import miui.process.ForegroundInfo;
import miui.securitycenter.net.MiuiNetworkSessionStats;
import miui.util.DeviceLevel;
import vendor.xiaomi.hidl.minet.V1_0.IMiNetService;

/* loaded from: classes.dex */
public class NetworkSDKService {
    private static final String ACTION_MSIM_VOICE_CAPABILITY_CHANGED = "org.codeaurora.intent.action.MSIM_VOICE_CAPABILITY_CHANGED";
    private static final String ACTION_VIDEO_APPS_POLICY_NOTIFY = "com.android.phone.action.VIDEO_APPS_POLICY_NOTIFY";
    public static final int ADD_WHITELIST_DUAL_WIFI_TURNED_ON = 7;
    private static final long APP_WIFI_SELECTION_MONITOR_MILLIS = 10000;
    public static final int BASE = 1000;
    private static final int CALLBACK_NETWORK_PRIORITY_CHANGED = 1015;
    private static final int CALLBACK_ON_DSDA_STATE_CHANGED = 1012;
    private static final int CALLBACK_ON_NETWORK_ADDED = 1007;
    private static final int CALLBACK_ON_NETWORK_REMOVEDE = 1008;
    private static final int CALLBACK_ON_SCAN_SUCCESS = 0;
    private static final int CALLBACK_ON_SET_SLAVE_WIFI_RES = 1;
    private static final int CALLBACK_ON_SLAVE_CONNECTED = 2;
    private static final int CALLBACK_ON_SLAVE_DISCONNECTED = 3;
    private static final int CALLBACK_ON_SLAVE_WIFI_ENABLE = 1011;
    private static final int CALLBACK_ON_SLAVE_WIFI_ENABLE_V1 = 1016;
    private static final int CALLBACK_SET_SCREEN_STATUS = 1014;
    private static final int CALLBACK_START_WIFI_LINKSTATS = 1009;
    private static final int CALLBACK_VIDEO_POLICY_CHANGED = 1013;
    private static final String CLOUD_QEE_ENABLE = "cloud_qee_enable";
    private static final long DUAL_WIFI_BACKGROUND_MONITOR_MILLIS = 30000;
    public static final int DUAL_WIFI_TURNED_OFF_SCREEN_OFF = 3;
    public static final int DUAL_WIFI_TURNED_ON_SCREEN_ON = 4;
    private static final int EVENT_BACKGROUND_MONITOR = 107;
    private static final int EVENT_BACKGROUND_MONITOR_RESTART = 108;
    private static final int EVENT_BACKGROUND_MONITOR_START = 109;
    private static final int EVENT_DSDA_STATE_CHANGED = 104;
    private static final int EVENT_GET_HAL = 103;
    private static final int EVENT_IS_EVER_OPENED_DUAL_WIFI = 106;
    private static final int EVENT_RELEASE_DUAL_WIFI = 105;
    private static final int EVENT_SLAVE_WIFI_CONNECT = 101;
    private static final int EVENT_SLAVE_WIFI_DISCONNECT = 102;
    private static final int EVENT_SLAVE_WIFI_ENABLE = 100;
    private static final int GET_SERVICE_DELAY_MILLIS = 4000;
    public static final int HANDLE_ADD_TO_WHITELIST = 2;
    public static final int HANDLE_REMOVE_FROM_WHITELIST = 1;
    public static final int IS_DUAL_WIFI_ENABLED = 0;
    public static final String IS_OPENED_DUAL_WIFI = "is_opened_dual_wifi";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_TYPE = "type";
    private static final int LINK_POLL_SEND_DEVIATION = 33;
    private static final int MAX_COUNT = 18;
    private static final int MAX_DUAL_WIFI_BACKGROUND_COUNT = 10;
    private static final String MIBRIDGE_WHITELIST = "mibridge_authorized_pkg_list";
    public static final int MINETD_CMD_SETSOCKPRIO = 1001;
    public static final int MINETD_CMD_SETTCPCONGESTION = 1002;
    public static final int MINETD_CMD_SET_TRAFFIC_INTERFACE = 1006;
    private static final int MSG_START_APP_WIFI_SELECTION_MONITOR = 1010;
    private static final String NETWORK_NETWORKBOOST_WHITELIST = "cloud_networkboost_whitelist";
    private static final int QCOM_STATE_DSDA = 3;
    private static final String QEE_PROP = "sys.network.cronet.qee";
    public static final int QUERY_IS_IN_WHITELIST = 3;
    public static final int REMOVE_WHITELIST_DUAL_WIFI_TURNED_ON = 8;
    public static final int SDK_TURNS_OFF_DUAL_WIFI = 6;
    public static final int SDK_TURNS_OFF_DUAL_WIFI_BY_BG = 10;
    public static final int SDK_TURNS_ON_DUAL_WIFI = 5;
    public static final int SDK_TURNS_ON_DUAL_WIFI_BY_BG = 9;
    private static final long SLAVE_WIFI_SCREEN_OFF_LONG = 30000;
    private static final String TAG = "NetworkSDKService";
    private static final int TYPE_PRELOAD = 1;
    private static final int TYPE_RESOLUTION_ADJUST = 2;
    public static final int USER_TURNS_OFF_DUAL_WIFI = 2;
    public static final int USER_TURNS_ON_DUAL_WIFI = 1;
    private static final String WIFI_INTERFACE_1 = "wlan1";
    private static IMiNetService mMiNetService;
    public static volatile NetworkSDKService sInstance;
    private boolean isConnectSlaveAp;
    private Handler mCallbackHandler;
    private RemoteCallbackList<IAIDLMiuiNetworkCallback> mCallbacks;
    private Set<String> mCellularNetworkSDKPN;
    private Set<String> mCellularNetworkSDKUid;
    private Context mContext;
    private DualCelluarDataService mDualCelluarDataService;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private MiuiWifiManager mMiuiWifiManager;
    private RemoteCallbackList<IAIDLMiuiNetSelectCallback> mNetSelectCallbacks;
    private Set<String> mNetworkSDKPN;
    private Set<String> mNetworkSDKUid;
    private HashMap<String, String> mPermission;
    private WifiScanner mScanner;
    private NetworkBoostSimCardHelper mSimCardHelper;
    private SLAService mSlaService;
    private SlaveWifiManager mSlaveWifiManager;
    private MiuiNetworkSessionStats mStatsSession;
    private StatusManager mStatusManager;
    private WifiManager mWifiManager;
    private RemoteCallbackList<IAIDLMiuiNetQoECallback> mWlanQoECallbacks;
    private static final Object mListLock = new Object();
    private static final Object mCellularListLock = new Object();
    private long mWifiLinkStatsPollMillis = -1;
    private Set<Integer> mWifiSelectionApps = new HashSet();
    private Map<IBinder, Long> mAppStatsPollMillis = new HashMap();
    private Map<IBinder, Long> mAppStatsPollInterval = new HashMap();
    private Map<IBinder, Integer> mAppCallBackMapToUid = new HashMap();
    private WlanLinkLayerQoE mMiuiWlanLayerQoE = null;
    private NetLinkLayerQoE mMasterNetLayerQoE = null;
    private NetLinkLayerQoE mSlaveNetLayerQoE = null;
    private boolean mIsScreenON = false;
    private long[] mTxAndRxStatistics = new long[3];
    private int DECIMAL_CONVERSION = AnimState.VIEW_SIZE;
    private boolean mDsdaCapability = false;
    private Object mLock = new Object();
    private boolean mIsOpenWifiLinkPoll = false;
    private boolean mIsCloseRoaming = false;
    private List<String> mAvailableIfaces = new ArrayList();
    private WifiScanner.ScanSettings mScanSettings = new WifiScanner.ScanSettings();
    private int mOffScreenCount = 0;
    private int mMarketUid = -1;
    private int mDownloadsUid = -1;
    private int mDualWifiOriginStatus = -1;
    private boolean mIsDualWifiSDKChanged = false;
    private Set<Integer> mDualWifiApps = null;
    private Map<Integer, String> mAppUidToPackName = new HashMap();
    private long mScreenOffSystemTime = -1;
    private int mDualWifiBackgroundCount = 0;
    private boolean mIsEverClosedByBackground = false;
    private boolean mIsStartMonitorByBackground = false;
    private ContentObserver mQEEObserver = null;
    private StatusManager.IAppStatusListener mIAppStatusListenr = new StatusManager.IAppStatusListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.1
        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            if (!NetworkSDKService.this.setOrGetEverOpenedDualWifi(false, false)) {
                return;
            }
            if (NetworkSDKService.this.mDualWifiApps != null && !NetworkSDKService.this.mDualWifiApps.contains(Integer.valueOf(NetworkSDKService.this.mStatusManager.getForegroundUid())) && !NetworkSDKService.this.mIsEverClosedByBackground) {
                NetworkSDKService.this.mHandler.sendMessageDelayed(NetworkSDKService.this.mHandler.obtainMessage(109), 30000L);
            }
            if (NetworkSDKService.this.mDualWifiApps != null && NetworkSDKService.this.mDualWifiApps.contains(Integer.valueOf(NetworkSDKService.this.mStatusManager.getForegroundUid()))) {
                NetworkSDKService.this.mHandler.removeMessages(107);
                NetworkSDKService.this.mHandler.sendMessage(NetworkSDKService.this.mHandler.obtainMessage(108));
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onUidGone(int goneUid, boolean disabled) {
            if (NetworkSDKService.this.mDualWifiApps != null && NetworkSDKService.this.mDualWifiApps.contains(Integer.valueOf(goneUid))) {
                NetworkSDKService.this.mHandler.sendMessage(NetworkSDKService.this.mHandler.obtainMessage(105, Integer.valueOf(goneUid)));
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onAudioChanged(int mode) {
        }
    };
    private StatusManager.IScreenStatusListener mIScreenStatusListener = new StatusManager.IScreenStatusListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.2
        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenON() {
            NetworkSDKService.this.mCallbackHandler.sendMessage(NetworkSDKService.this.mCallbackHandler.obtainMessage(NetworkSDKService.CALLBACK_SET_SCREEN_STATUS, true));
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenOFF() {
            NetworkSDKService.this.mCallbackHandler.sendMessage(NetworkSDKService.this.mCallbackHandler.obtainMessage(NetworkSDKService.CALLBACK_SET_SCREEN_STATUS, false));
        }
    };
    private StatusManager.INetworkPriorityListener mNetworkPriorityListener = new StatusManager.INetworkPriorityListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.3
        @Override // com.xiaomi.NetworkBoost.StatusManager.INetworkPriorityListener
        public void onNetworkPriorityChanged(int priorityMode, int trafficPolicy, int thermalLevel) {
            NetworkSDKService.this.mCallbackHandler.sendMessage(NetworkSDKService.this.mCallbackHandler.obtainMessage(NetworkSDKService.CALLBACK_NETWORK_PRIORITY_CHANGED, priorityMode, trafficPolicy, Integer.valueOf(thermalLevel)));
        }
    };
    private final WifiScanner.ScanListener mScanListener = new WifiScanner.ScanListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.4
        public void onPeriodChanged(int periodInMs) {
            Log.e(NetworkSDKService.TAG, "ScanListener onPeriodChanged: " + periodInMs);
        }

        public void onResults(WifiScanner.ScanData[] results) {
            Log.e(NetworkSDKService.TAG, "ScanListener onResults: " + results);
        }

        public void onFullResult(ScanResult fullScanResult) {
            Log.e(NetworkSDKService.TAG, "ScanListener onFullResult: " + fullScanResult);
        }

        public void onSuccess() {
            Log.e(NetworkSDKService.TAG, "ScanListener onSuccess: ");
        }

        public void onFailure(int reason, String description) {
            Log.e(NetworkSDKService.TAG, "ScanListener onFailure: " + reason + ": " + description);
        }
    };
    private StatusManager.INetworkInterfaceListener networkInterfaceListener = new StatusManager.INetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.5
        @Override // com.xiaomi.NetworkBoost.StatusManager.INetworkInterfaceListener
        public void onNetwrokInterfaceChange(String ifacename, int ifacenum, boolean wifiready, boolean slavewifiready, boolean dataready, boolean slaveDataReady, String status) {
            List<String> interfaceList = new ArrayList<>();
            String[] interfaces = ifacename.split(",");
            for (String str : interfaces) {
                interfaceList.add(str);
            }
            NetworkSDKService.this.mAvailableIfaces = interfaceList;
            if (status.equals(StatusManager.ON_AVAILABLE)) {
                NetworkSDKService networkSDKService = NetworkSDKService.this;
                networkSDKService.ifaceAddedCallBackNotice(networkSDKService.mAvailableIfaces);
                if (ifacename.indexOf(StatusManager.WLAN_IFACE_0) != -1) {
                    NetworkSDKService.this.broadcastPrimaryConnected();
                }
            }
            if (status.equals(StatusManager.ON_LOST)) {
                NetworkSDKService networkSDKService2 = NetworkSDKService.this;
                networkSDKService2.ifaceRemovedCallBackNotice(networkSDKService2.mAvailableIfaces);
            }
            if (ifacename != null && ifacename.indexOf("wlan") != -1 && ifacename.indexOf("wlan1") != -1) {
                NetworkSDKService.this.onSlaveWifiConnected(true);
            }
        }
    };
    private IHwBinder.DeathRecipient mDeathRecipient = new MiNetServiceDeathRecipient();
    private boolean mIsDualWifiScreenOffDisabled = false;
    private Map<String, String> mBssidToNetworkId = null;

    public static NetworkSDKService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkSDKService.class) {
                if (sInstance == null) {
                    sInstance = new NetworkSDKService(context);
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
            synchronized (NetworkSDKService.class) {
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

    private NetworkSDKService(Context context) {
        this.mContext = null;
        this.mContext = context.getApplicationContext();
    }

    public void onCreate() {
        Log.i(TAG, "onCreate ");
        HandlerThread handlerThread = new HandlerThread("NetworkSDKServiceHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        this.mCallbackHandler = new CallbackHandler(this.mHandlerThread.getLooper());
        this.mHandler.sendEmptyMessage(103);
        try {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            this.mSlaveWifiManager = (SlaveWifiManager) this.mContext.getSystemService("SlaveWifiService");
            this.mMiuiWifiManager = (MiuiWifiManager) this.mContext.getSystemService("MiuiWifiService");
            if (!DeviceLevel.IS_MIUI_MIDDLE_VERSION) {
                Log.d(TAG, "is not MIUI Middle or is not port MIUI Middle, start DualCelluarDataService");
                this.mDualCelluarDataService = DualCelluarDataService.getInstance(this.mContext);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSystemService Exception:" + e);
        }
        try {
            this.mScanner = (WifiScanner) Objects.requireNonNull((WifiScanner) this.mContext.getSystemService(WifiScanner.class), "Got a null instance of WifiScanner!");
        } catch (Exception e2) {
            Log.e(TAG, "Got a null instance of WifiScanner!" + e2);
        }
        this.isConnectSlaveAp = false;
        this.mDsdaCapability = getCurrentDSDAState();
        this.mCallbacks = new RemoteCallbackList<>();
        this.mWlanQoECallbacks = new RemoteCallbackList<>();
        this.mNetSelectCallbacks = new RemoteCallbackList<>();
        StatusManager statusManager = StatusManager.getInstance(this.mContext);
        this.mStatusManager = statusManager;
        this.mAvailableIfaces = statusManager.getAvailableIfaces();
        this.mStatusManager.registerNetworkInterfaceListener(this.networkInterfaceListener);
        this.mStatusManager.registerScreenStatusListener(this.mIScreenStatusListener);
        this.mStatusManager.registerNetworkPriorityListener(this.mNetworkPriorityListener);
        this.mStatusManager.registerAppStatusListener(this.mIAppStatusListenr);
        registDualWifiStateBroadcastReceiver();
        initWhiteList();
        initCellularWhiteList();
        initNetworkSDKCloudObserver();
        initMibridgeCloudObserver();
        initCellularNetworkSDKCloudObserver();
        initBroadcastReceiver();
        initDualDataBroadcastReceiver();
        initPermission();
        initAppTrafficSessionAndSimCardHelper();
        this.mHandler.sendEmptyMessage(106);
        registerQEEChangeObserver();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        IMiNetService iMiNetService = mMiNetService;
        if (iMiNetService != null) {
            try {
                iMiNetService.stopMiNetd();
                this.mCallbacks.kill();
                this.mWlanQoECallbacks.kill();
                this.mNetSelectCallbacks.kill();
                mMiNetService.unlinkToDeath(this.mDeathRecipient);
                clearAppWifiSelectionMonitor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.mDualCelluarDataService != null) {
            DualCelluarDataService.destroyInstance();
            this.mDualCelluarDataService = null;
        }
        closeAppTrafficSession();
        this.mHandlerThread.quit();
        this.mStatusManager.unregisterScreenStatusListener(this.mIScreenStatusListener);
        this.mStatusManager.unregisterNetworkInterfaceListener(this.networkInterfaceListener);
        this.mStatusManager.unregisterNetworkPriorityListener(this.mNetworkPriorityListener);
        unregisterQEEChangeObserver();
    }

    /* loaded from: classes.dex */
    class MiNetServiceDeathRecipient implements IHwBinder.DeathRecipient {
        MiNetServiceDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Log.e(NetworkSDKService.TAG, "HAL service died");
            NetworkSDKService.this.mHandler.sendMessageDelayed(NetworkSDKService.this.mHandler.obtainMessage(103, 0), 4000L);
        }
    }

    public boolean setSockPrio(int fd, int prio) {
        Log.i(TAG, "setSockPrio " + fd + "," + prio);
        if (!networkSDKPermissionCheck("setSockPrio")) {
            return false;
        }
        try {
            mMiNetService.setCommon(1001, fd + "," + prio);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return true;
        }
    }

    public boolean setTCPCongestion(int fd, String cc) {
        Log.i(TAG, "setTCPCongestion " + fd + "," + cc);
        if (!networkSDKPermissionCheck("setTCPCongestion")) {
            return false;
        }
        try {
            mMiNetService.setCommon(1002, fd + "," + cc);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return true;
        }
    }

    public boolean isSupportDualWifi() {
        SlaveWifiManager slaveWifiManager;
        Log.i(TAG, "isSupportDualWifi");
        if (networkSDKPermissionCheck("isSupportDualWifi") && (slaveWifiManager = this.mSlaveWifiManager) != null) {
            return slaveWifiManager.supportDualWifi();
        }
        return false;
    }

    public boolean setSlaveWifiEnabled(boolean enable) {
        Log.i(TAG, "setSlaveWifiEnabled: " + enable);
        if (!networkSDKPermissionCheck("setSlaveWifiEnabled")) {
            return false;
        }
        Message msg = Message.obtain();
        msg.what = 100;
        msg.obj = Boolean.valueOf(enable);
        msg.arg1 = Binder.getCallingUid();
        this.mHandler.sendMessage(msg);
        return true;
    }

    public boolean connectSlaveWifi(int networkId) {
        Log.i(TAG, "connectSlaveWifi: " + networkId);
        if (!networkSDKPermissionCheck("connectSlaveWifi")) {
            return false;
        }
        Message msg = Message.obtain();
        msg.what = 101;
        msg.obj = Integer.valueOf(networkId);
        this.mHandler.sendMessage(msg);
        return true;
    }

    public boolean disconnectSlaveWifi() {
        Log.i(TAG, "disconnectSlaveWifi: ");
        if (!networkSDKPermissionCheck("disconnectSlaveWifi")) {
            return false;
        }
        this.mHandler.sendEmptyMessage(102);
        return true;
    }

    public Map<String, String> getAvailableIfaces() {
        Map<String, String> result = new HashMap<>();
        if (!networkSDKPermissionCheck("getAvailableIfaces")) {
            result.put(ResultInfoConstants.CODE, ResultInfoConstants.ERROR_STR_CODE);
            result.put(ResultInfoConstants.MESSAGE, ResultInfoConstants.PERMISSION_ERROR);
            return result;
        }
        List<String> ifacesList = this.mStatusManager.getAvailableIfaces();
        String ifaces = "";
        for (int i = 0; i < ifacesList.size(); i++) {
            ifaces = ifaces + ifacesList.get(i);
            if (i != ifacesList.size() - 1) {
                ifaces = ifaces + ",";
            }
        }
        result.put(ResultInfoConstants.CODE, ResultInfoConstants.SUCCESS_STR_CODE);
        result.put(ResultInfoConstants.RESULT, ifaces);
        return result;
    }

    public boolean setTrafficTransInterface(int fd, String bindInterface) {
        Log.i(TAG, "setTrafficTransInterface " + fd + "," + bindInterface);
        if (!networkSDKPermissionCheck("setTrafficTransInterface")) {
            return false;
        }
        try {
            mMiNetService.setCommon(MINETD_CMD_SET_TRAFFIC_INTERFACE, fd + "," + bindInterface);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            return true;
        }
    }

    public boolean registerCallback(IAIDLMiuiNetworkCallback cb) {
        if (cb != null) {
            this.mCallbacks.register(cb);
            try {
                StatusManager.NetworkPriorityInfo info = this.mStatusManager.getNetworkPriorityInfo();
                cb.onNetworkPriorityChanged(info.priorityMode, info.trafficPolicy, info.thermalLevel);
                return true;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    public boolean unregisterCallback(IAIDLMiuiNetworkCallback cb) {
        if (cb != null) {
            this.mCallbacks.unregister(cb);
            return true;
        }
        return false;
    }

    public boolean activeScan(int[] channelList) {
        Log.i(TAG, "activeScan: " + channelList);
        this.mScanSettings.type = 2;
        this.mScanSettings.band = 0;
        this.mScanSettings.channels = channelsToSpec(channelList);
        this.mScanSettings.periodInMs = 10000;
        this.mScanSettings.numBssidsPerScan = 20;
        this.mScanSettings.maxScansToCache = 0;
        this.mScanSettings.reportEvents = 3;
        this.mScanner.startScan(this.mScanSettings, this.mScanListener);
        return true;
    }

    private static WifiScanner.ChannelSpec[] channelsToSpec(int[] channelList) {
        WifiScanner.ChannelSpec[] channelSpecs = new WifiScanner.ChannelSpec[channelList.length];
        for (int i = 0; i < channelList.length; i++) {
            channelSpecs[i] = new WifiScanner.ChannelSpec(channelList[i]);
        }
        return channelSpecs;
    }

    public boolean abortScan() {
        Log.i(TAG, "abortScan: ");
        this.mScanner.stopScan(this.mScanListener);
        return true;
    }

    public boolean suspendBackgroundScan() {
        MiuiWifiManager miuiWifiManager;
        Log.i(TAG, "suspendBackgroundScan");
        if (!networkSDKPermissionCheck("suspendBackgroundScan") || (miuiWifiManager = this.mMiuiWifiManager) == null) {
            return false;
        }
        miuiWifiManager.setLatencyLevel(4);
        return true;
    }

    public boolean resumeBackgroundScan() {
        MiuiWifiManager miuiWifiManager;
        Log.i(TAG, "resumeBackgroundScan");
        if (!networkSDKPermissionCheck("resumeBackgroundScan") || (miuiWifiManager = this.mMiuiWifiManager) == null) {
            return false;
        }
        miuiWifiManager.setLatencyLevel(1);
        return true;
    }

    public boolean suspendWifiPowerSave() {
        MiuiWifiManager miuiWifiManager;
        Log.i(TAG, "suspendWifiPowerSave");
        if (!networkSDKPermissionCheck("suspendWifiPowerSave") || (miuiWifiManager = this.mMiuiWifiManager) == null) {
            return false;
        }
        miuiWifiManager.enablePowerSave(false);
        return true;
    }

    public boolean resumeWifiPowerSave() {
        MiuiWifiManager miuiWifiManager;
        Log.i(TAG, "resumeWifiPowerSave");
        if (!networkSDKPermissionCheck("resumeWifiPowerSave") || (miuiWifiManager = this.mMiuiWifiManager) == null) {
            return false;
        }
        miuiWifiManager.enablePowerSave(true);
        return true;
    }

    public boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback cb, int interval) {
        int uid = Binder.getCallingUid();
        return registerNetLinkCallback(cb, interval, uid);
    }

    public boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback cb, int interval, int uid) {
        if (interval < 0 || interval > 5 || !networkSDKPermissionCheck("registerNetLinkCallback")) {
            return false;
        }
        int interval2 = NetworkSDKUtils.intervalTransformation(interval);
        if (cb == null) {
            return false;
        }
        synchronized (this.mLock) {
            IBinder key = cb.asBinder();
            if (this.mAppStatsPollMillis.containsKey(key)) {
                return true;
            }
            long j = interval2;
            long j2 = this.mWifiLinkStatsPollMillis;
            if (j <= j2 || j2 == -1) {
                this.mWifiLinkStatsPollMillis = interval2;
            }
            int count = this.mWlanQoECallbacks.getRegisteredCallbackCount();
            long curSystemTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                IAIDLMiuiNetQoECallback callBack = this.mWlanQoECallbacks.getRegisteredCallbackItem(i);
                IBinder callBackKey = callBack.asBinder();
                this.mAppStatsPollMillis.put(callBackKey, Long.valueOf(curSystemTime));
                Log.d(TAG, "registerNetLinkCallback: Reset callBacks millis");
            }
            this.mWlanQoECallbacks.register(cb);
            this.mAppStatsPollInterval.put(key, Long.valueOf(interval2));
            this.mAppStatsPollMillis.put(key, Long.valueOf(curSystemTime));
            this.mAppCallBackMapToUid.put(key, Integer.valueOf(uid));
            if (!this.mIsOpenWifiLinkPoll) {
                this.mCallbackHandler.removeMessages(CALLBACK_START_WIFI_LINKSTATS);
                Handler handler = this.mCallbackHandler;
                handler.sendMessageDelayed(handler.obtainMessage(CALLBACK_START_WIFI_LINKSTATS), this.mWifiLinkStatsPollMillis);
            }
            this.mIsOpenWifiLinkPoll = true;
            return true;
        }
    }

    public boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback cb) {
        int uid = Binder.getCallingUid();
        return unregisterNetLinkCallback(cb, uid);
    }

    public boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback cb, int uid) {
        if (networkSDKPermissionCheck("unregisterNetLinkCallback") && cb != null) {
            synchronized (this.mLock) {
                IBinder key = cb.asBinder();
                if (!this.mAppStatsPollMillis.containsKey(key)) {
                    return true;
                }
                this.mWlanQoECallbacks.unregister(cb);
                this.mAppStatsPollInterval.remove(key);
                this.mAppStatsPollMillis.remove(key);
                this.mAppCallBackMapToUid.remove(key);
                int count = this.mWlanQoECallbacks.getRegisteredCallbackCount();
                long curSystemTime = System.currentTimeMillis();
                long minInterval = -1;
                for (int i = 0; i < count; i++) {
                    IAIDLMiuiNetQoECallback callBack = this.mWlanQoECallbacks.getRegisteredCallbackItem(i);
                    IBinder callBackKey = callBack.asBinder();
                    this.mAppStatsPollMillis.put(callBackKey, Long.valueOf(curSystemTime));
                    if (minInterval == -1) {
                        minInterval = this.mAppStatsPollInterval.get(callBackKey).longValue();
                    } else if (this.mAppStatsPollInterval.get(callBackKey).longValue() < minInterval) {
                        minInterval = this.mAppStatsPollInterval.get(callBackKey).longValue();
                    }
                }
                if (minInterval != -1) {
                    this.mWifiLinkStatsPollMillis = minInterval;
                    Log.d(TAG, "unregisterNetLinkCallback: reset interval = " + this.mWifiLinkStatsPollMillis);
                }
                if (count == 0) {
                    this.mIsOpenWifiLinkPoll = false;
                    this.mWifiLinkStatsPollMillis = -1L;
                }
                return true;
            }
        }
        return false;
    }

    public Map<String, String> requestAppTrafficStatistics(int type, long startTime, long endTime) {
        int uid = Binder.getCallingUid();
        return requestAppTrafficStatistics(type, startTime, endTime, uid);
    }

    public Map<String, String> requestAppTrafficStatistics(int type, long startTime, long endTime, int uid) {
        if (!networkSDKPermissionCheck("requestAppTrafficStatistics")) {
            Map<String, String> appTrafficStatistics = new HashMap<>();
            appTrafficStatistics.put(ResultInfoConstants.CODE, ResultInfoConstants.ERROR_STR_PERMISSION_CODE);
            appTrafficStatistics.put(ResultInfoConstants.MESSAGE, ResultInfoConstants.PERMISSION_ERROR);
            return appTrafficStatistics;
        }
        Map<String, String> appTrafficStatistics2 = buildAppTrafficStatistics(type, startTime, endTime, uid);
        if (appTrafficStatistics2 == null) {
            Map<String, String> appTrafficStatistics3 = new HashMap<>();
            appTrafficStatistics3.put(ResultInfoConstants.CODE, ResultInfoConstants.ERROR_STR_CODE);
            return appTrafficStatistics3;
        }
        appTrafficStatistics2.put(ResultInfoConstants.CODE, ResultInfoConstants.SUCCESS_STR_CODE);
        return appTrafficStatistics2;
    }

    private void initAppTrafficSessionAndSimCardHelper() {
        this.mSimCardHelper = NetworkBoostSimCardHelper.getInstance(this.mContext);
        MiuiNetworkSessionStats miuiNetworkSessionStats = this.mStatsSession;
        if (miuiNetworkSessionStats != null) {
            miuiNetworkSessionStats.openSession();
            return;
        }
        MiuiNetworkSessionStats miuiNetworkSessionStats2 = new MiuiNetworkSessionStats();
        this.mStatsSession = miuiNetworkSessionStats2;
        miuiNetworkSessionStats2.openSession();
    }

    private void closeAppTrafficSession() {
        MiuiNetworkSessionStats miuiNetworkSessionStats = this.mStatsSession;
        if (miuiNetworkSessionStats != null) {
            miuiNetworkSessionStats.closeSession();
            this.mStatsSession = null;
        }
    }

    private Map<String, String> buildAppTrafficStatistics(int type, long startTime, long endTime, int uid) {
        switch (type) {
            case 0:
                Map<String, String> buildAppTrafficStatistics = buildAppMobileDataUsage(uid, startTime, endTime);
                return buildAppTrafficStatistics;
            case 1:
                Map<String, String> buildAppTrafficStatistics2 = buildAppWifiDataUsage(uid, startTime, endTime);
                return buildAppTrafficStatistics2;
            case 2:
                Map<String, String> mobileTrafficStatistics = buildAppMobileDataUsage(uid, startTime, endTime);
                Map<String, String> wifiTrafficStatistics = buildAppWifiDataUsage(uid, startTime, endTime);
                if (mobileTrafficStatistics != null) {
                    mobileTrafficStatistics.putAll(wifiTrafficStatistics);
                    return mobileTrafficStatistics;
                }
                if (wifiTrafficStatistics != null) {
                    return wifiTrafficStatistics;
                }
                return null;
            default:
                return null;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0099  */
    /* JADX WARN: Removed duplicated region for block: B:30:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.util.Map<java.lang.String, java.lang.String> buildAppMobileDataUsage(int r24, long r25, long r27) {
        /*
            r23 = this;
            r1 = r23
            com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper r0 = r1.mSimCardHelper
            r2 = 0
            java.lang.String r3 = "NetworkSDKService"
            if (r0 != 0) goto Lf
            java.lang.String r0 = "mSimCardHelper null"
            android.util.Log.e(r3, r0)
            return r2
        Lf:
            int r4 = r0.getCurrentMobileSlotNum()
            com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper r0 = r1.mSimCardHelper
            java.lang.String r11 = r0.getSimImsi(r4)
            miui.securitycenter.net.MiuiNetworkSessionStats r5 = r1.mStatsSession
            r6 = r11
            r7 = r25
            r9 = r27
            android.util.SparseArray r5 = r5.getMobileSummaryForAllUid(r6, r7, r9)
            if (r5 != 0) goto L2c
            java.lang.String r0 = "buildAppMobileDataUsage networkStats null"
            android.util.Log.e(r3, r0)
            return r2
        L2c:
            int r6 = r5.size()
            r0 = 0
            r7 = 0
            r22 = r7
            r7 = r0
            r0 = r22
        L37:
            if (r0 >= r6) goto L93
            int r8 = r5.keyAt(r0)     // Catch: java.lang.Exception -> L8a
            r9 = r24
            if (r9 != r8) goto L87
            java.lang.Object r10 = r5.get(r8)     // Catch: java.lang.Exception -> L85
            java.util.Map r10 = (java.util.Map) r10     // Catch: java.lang.Exception -> L85
            if (r10 == 0) goto L87
            com.xiaomi.NetworkBoost.NetworkSDK.AppDataUsage r21 = new com.xiaomi.NetworkBoost.NetworkSDK.AppDataUsage     // Catch: java.lang.Exception -> L85
            java.lang.String r12 = "rxBytes"
            java.lang.Object r12 = r10.get(r12)     // Catch: java.lang.Exception -> L85
            java.lang.Long r12 = (java.lang.Long) r12     // Catch: java.lang.Exception -> L85
            long r13 = r12.longValue()     // Catch: java.lang.Exception -> L85
            java.lang.String r12 = "txBytes"
            java.lang.Object r12 = r10.get(r12)     // Catch: java.lang.Exception -> L85
            java.lang.Long r12 = (java.lang.Long) r12     // Catch: java.lang.Exception -> L85
            long r15 = r12.longValue()     // Catch: java.lang.Exception -> L85
            java.lang.String r12 = "txForegroundBytes"
            java.lang.Object r12 = r10.get(r12)     // Catch: java.lang.Exception -> L85
            java.lang.Long r12 = (java.lang.Long) r12     // Catch: java.lang.Exception -> L85
            long r17 = r12.longValue()     // Catch: java.lang.Exception -> L85
            java.lang.String r12 = "rxForegroundBytes"
            java.lang.Object r12 = r10.get(r12)     // Catch: java.lang.Exception -> L85
            java.lang.Long r12 = (java.lang.Long) r12     // Catch: java.lang.Exception -> L85
            long r19 = r12.longValue()     // Catch: java.lang.Exception -> L85
            r12 = r21
            r12.<init>(r13, r15, r17, r19)     // Catch: java.lang.Exception -> L85
            r7 = r21
            goto L87
        L85:
            r0 = move-exception
            goto L8d
        L87:
            int r0 = r0 + 1
            goto L37
        L8a:
            r0 = move-exception
            r9 = r24
        L8d:
            java.lang.String r8 = "buildAppMobileDataUsage error"
            android.util.Log.e(r3, r8)
            goto L96
        L93:
            r9 = r24
        L96:
            if (r7 != 0) goto L99
            goto L9e
        L99:
            r0 = 1
            java.util.Map r2 = r7.toMap(r0)
        L9e:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.buildAppMobileDataUsage(int, long, long):java.util.Map");
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x0087  */
    /* JADX WARN: Removed duplicated region for block: B:26:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.util.Map<java.lang.String, java.lang.String> buildAppWifiDataUsage(int r26, long r27, long r29) {
        /*
            r25 = this;
            r1 = r25
            miui.securitycenter.net.MiuiNetworkSessionStats r0 = r1.mStatsSession
            r2 = r27
            r4 = r29
            android.util.SparseArray r6 = r0.getWifiSummaryForAllUid(r2, r4)
            r7 = 0
            java.lang.String r8 = "NetworkSDKService"
            if (r6 != 0) goto L17
            java.lang.String r0 = "buildAppWifiDataUsage networkStats null"
            android.util.Log.e(r8, r0)
            return r7
        L17:
            int r9 = r6.size()
            r0 = 0
            r10 = 0
            r24 = r10
            r10 = r0
            r0 = r24
        L22:
            if (r0 >= r9) goto L81
            int r11 = r6.keyAt(r0)     // Catch: java.lang.Exception -> L75
            r12 = r26
            if (r12 != r11) goto L72
            java.lang.Object r13 = r6.get(r11)     // Catch: java.lang.Exception -> L70
            java.util.Map r13 = (java.util.Map) r13     // Catch: java.lang.Exception -> L70
            if (r13 == 0) goto L72
            com.xiaomi.NetworkBoost.NetworkSDK.AppDataUsage r23 = new com.xiaomi.NetworkBoost.NetworkSDK.AppDataUsage     // Catch: java.lang.Exception -> L70
            java.lang.String r14 = "rxBytes"
            java.lang.Object r14 = r13.get(r14)     // Catch: java.lang.Exception -> L70
            java.lang.Long r14 = (java.lang.Long) r14     // Catch: java.lang.Exception -> L70
            long r15 = r14.longValue()     // Catch: java.lang.Exception -> L70
            java.lang.String r14 = "txBytes"
            java.lang.Object r14 = r13.get(r14)     // Catch: java.lang.Exception -> L70
            java.lang.Long r14 = (java.lang.Long) r14     // Catch: java.lang.Exception -> L70
            long r17 = r14.longValue()     // Catch: java.lang.Exception -> L70
            java.lang.String r14 = "txForegroundBytes"
            java.lang.Object r14 = r13.get(r14)     // Catch: java.lang.Exception -> L70
            java.lang.Long r14 = (java.lang.Long) r14     // Catch: java.lang.Exception -> L70
            long r19 = r14.longValue()     // Catch: java.lang.Exception -> L70
            java.lang.String r14 = "rxForegroundBytes"
            java.lang.Object r14 = r13.get(r14)     // Catch: java.lang.Exception -> L70
            java.lang.Long r14 = (java.lang.Long) r14     // Catch: java.lang.Exception -> L70
            long r21 = r14.longValue()     // Catch: java.lang.Exception -> L70
            r14 = r23
            r14.<init>(r15, r17, r19, r21)     // Catch: java.lang.Exception -> L70
            r10 = r23
            goto L72
        L70:
            r0 = move-exception
            goto L78
        L72:
            int r0 = r0 + 1
            goto L22
        L75:
            r0 = move-exception
            r12 = r26
        L78:
            r0.printStackTrace()
            java.lang.String r11 = "buildAppWifiDataUsage error"
            android.util.Log.e(r8, r11)
            goto L84
        L81:
            r12 = r26
        L84:
            if (r10 != 0) goto L87
            goto L8c
        L87:
            r0 = 0
            java.util.Map r7 = r10.toMap(r0)
        L8c:
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.buildAppWifiDataUsage(int, long, long):java.util.Map");
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
                    boolean enable = ((Boolean) msg.obj).booleanValue();
                    int uid = msg.arg1;
                    if (NetworkSDKService.this.mSlaveWifiManager != null) {
                        NetworkSDKService.this.handleMultiAppsDualWifi(enable, uid);
                        return;
                    }
                    return;
                case 101:
                case 102:
                default:
                    return;
                case 103:
                    try {
                        NetworkSDKService.mMiNetService = IMiNetService.getService();
                        if (NetworkSDKService.mMiNetService == null) {
                            Log.e(NetworkSDKService.TAG, "HAL service is null");
                            NetworkSDKService.this.mHandler.sendMessageDelayed(NetworkSDKService.this.mHandler.obtainMessage(103, 0), 4000L);
                        } else {
                            Log.d(NetworkSDKService.TAG, "HAL service get success");
                            NetworkSDKService.mMiNetService.linkToDeath(NetworkSDKService.this.mDeathRecipient, 0L);
                            NetworkSDKService.mMiNetService.startMiNetd();
                        }
                        return;
                    } catch (Exception e) {
                        Log.e(NetworkSDKService.TAG, "Exception:" + e);
                        return;
                    }
                case 104:
                    NetworkSDKService networkSDKService = NetworkSDKService.this;
                    networkSDKService.onDsdaStateChanged(networkSDKService.mDsdaCapability);
                    return;
                case 105:
                    int releaseUid = ((Integer) msg.obj).intValue();
                    Log.d(NetworkSDKService.TAG, "EVENT_RELEASE_DUAL_WIFI releaseUid = " + releaseUid);
                    if (NetworkSDKService.this.mSlaveWifiManager != null) {
                        NetworkSDKService.this.handleMultiAppsDualWifi(false, releaseUid);
                        return;
                    }
                    return;
                case 106:
                    synchronized (NetworkSDKService.this.mLock) {
                        try {
                            boolean isEverOpened = NetworkSDKService.this.setOrGetEverOpenedDualWifi(false, false);
                            if (isEverOpened && NetworkSDKService.this.mSlaveWifiManager != null) {
                                NetworkSDKService.this.mSlaveWifiManager.setWifiSlaveEnabled(false);
                                NetworkSDKService.this.setOrGetEverOpenedDualWifi(true, false);
                            }
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                    return;
                case 107:
                    NetworkSDKService.this.dualWifiBackgroundMonitor();
                    return;
                case 108:
                    synchronized (NetworkSDKService.this.mLock) {
                        NetworkSDKService.this.mDualWifiBackgroundCount = 0;
                        NetworkSDKService.this.mIsStartMonitorByBackground = false;
                    }
                    if (NetworkSDKService.this.mIsEverClosedByBackground) {
                        NetworkSDKService.this.dualWifiBackgroundMonitorRestart();
                        return;
                    }
                    return;
                case 109:
                    synchronized (NetworkSDKService.this.mLock) {
                        if (!NetworkSDKService.this.mIsStartMonitorByBackground) {
                            NetworkSDKService.this.mDualWifiBackgroundCount = 0;
                            NetworkSDKService.this.mIsStartMonitorByBackground = true;
                            NetworkSDKService.this.dualWifiBackgroundMonitor();
                        }
                    }
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0245  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x027c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void handleMultiAppsDualWifi(boolean r12, int r13) {
        /*
            Method dump skipped, instructions count: 678
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.handleMultiAppsDualWifi(boolean, int):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setOrGetEverOpenedDualWifi(boolean z, boolean z2) {
        Log.d(TAG, "handleMultiAppsDualWifi isSet = " + z + " isEverOpened = " + z2);
        if (!z) {
            return Settings.System.getInt(this.mContext.getContentResolver(), IS_OPENED_DUAL_WIFI, 0) == 1;
        }
        Settings.System.putInt(this.mContext.getContentResolver(), IS_OPENED_DUAL_WIFI, z2 ? 1 : 0);
        return true;
    }

    private void clearDualWifiStatus() {
        Log.d(TAG, "clearDualWifiStatus");
        synchronized (this.mLock) {
            this.mDualWifiOriginStatus = -1;
            this.mDualWifiApps = null;
        }
    }

    private void registDualWifiStateBroadcastReceiver() {
        Log.d(TAG, "registDualWifiStateBroadcastReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_SLAVE_STATE_CHANGED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                synchronized (NetworkSDKService.this.mLock) {
                    String action = intent.getAction();
                    if ("android.net.wifi.WIFI_SLAVE_STATE_CHANGED".equals(action)) {
                        NetworkSDKService.this.handleDualWifiStatusChanged(intent.getIntExtra("wifi_state", 18));
                    }
                }
            }
        }, new IntentFilter(filter), 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Failed to find 'out' block for switch in B:16:0x0059. Please report as an issue. */
    public void handleDualWifiStatusChanged(int state) {
        synchronized (this.mLock) {
            Log.d(TAG, "mIsDualWifiSDKChanged = " + this.mIsDualWifiSDKChanged + " state = " + state + " mDualWifiOriginStatus = " + this.mDualWifiOriginStatus);
            if (this.mIsDualWifiSDKChanged) {
                if (state == 17) {
                    onSlaveWifiEnableV1(5);
                    this.mIsDualWifiSDKChanged = false;
                    this.mIsEverClosedByBackground = false;
                    this.mIsStartMonitorByBackground = false;
                }
                if (state == 15) {
                    onSlaveWifiEnableV1(6);
                    this.mIsDualWifiSDKChanged = false;
                    this.mIsEverClosedByBackground = false;
                    this.mIsStartMonitorByBackground = false;
                }
                return;
            }
            this.mIsDualWifiSDKChanged = false;
            switch (state) {
                case 15:
                    if (!this.mIsScreenON && System.currentTimeMillis() - this.mScreenOffSystemTime > 30000) {
                        onSlaveWifiEnableV1(4);
                        this.mIsDualWifiScreenOffDisabled = true;
                    } else if (this.mIsEverClosedByBackground) {
                        onSlaveWifiEnableV1(10);
                    } else {
                        try {
                            SLAService sLAService = this.mSlaService;
                            if (sLAService != null) {
                                sLAService.setDoubleWifiWhiteList(5, "", "", false);
                            }
                            setOrGetEverOpenedDualWifi(true, false);
                            MiuiWifiManager miuiWifiManager = this.mMiuiWifiManager;
                            if (miuiWifiManager != null) {
                                miuiWifiManager.setSlaveCandidatesRssiThreshold(false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        clearDualWifiStatus();
                        onSlaveWifiEnableV1(2);
                        this.mIsDualWifiScreenOffDisabled = false;
                    }
                    return;
                case 16:
                default:
                    return;
                case 17:
                    if (this.mIsDualWifiScreenOffDisabled) {
                        this.mIsDualWifiScreenOffDisabled = false;
                        onSlaveWifiEnableV1(3);
                    } else if (this.mIsEverClosedByBackground) {
                        this.mIsEverClosedByBackground = false;
                        this.mIsStartMonitorByBackground = false;
                        onSlaveWifiEnableV1(9);
                    } else {
                        this.mIsDualWifiScreenOffDisabled = false;
                        onSlaveWifiEnableV1(1);
                    }
                    return;
            }
        }
    }

    private void onSlaveWifiEnableV1(int type) {
        Message msg = new Message();
        msg.what = CALLBACK_ON_SLAVE_WIFI_ENABLE_V1;
        msg.obj = Integer.valueOf(type);
        this.mCallbackHandler.sendMessage(msg);
    }

    private void onScanSuccussed(int result) {
        Message msg = new Message();
        msg.what = 0;
        msg.obj = Integer.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    void onSetSlaveWifiResult(boolean result) {
        Message msg = new Message();
        msg.what = 1;
        msg.obj = Boolean.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    void onSlaveWifiConnected(boolean result) {
        Message msg = new Message();
        msg.what = 2;
        msg.obj = Boolean.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    void onSlaveWifiDisconnected(boolean result) {
        Message msg = new Message();
        msg.what = 3;
        msg.obj = Boolean.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    void onSlaveWifiEnable(boolean result) {
        Message msg = new Message();
        msg.what = CALLBACK_ON_SLAVE_WIFI_ENABLE;
        msg.obj = Boolean.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    void onDsdaStateChanged(boolean result) {
        Message msg = new Message();
        msg.what = CALLBACK_ON_DSDA_STATE_CHANGED;
        msg.obj = Boolean.valueOf(result);
        this.mCallbackHandler.sendMessage(msg);
    }

    public void ifaceAddedCallBackNotice(List<String> availableIfaces) {
        if (availableIfaces == null) {
            return;
        }
        Handler handler = this.mCallbackHandler;
        handler.sendMessage(handler.obtainMessage(CALLBACK_ON_NETWORK_ADDED, availableIfaces));
    }

    public void ifaceRemovedCallBackNotice(List<String> availableIfaces) {
        if (availableIfaces == null) {
            return;
        }
        Handler handler = this.mCallbackHandler;
        handler.sendMessage(handler.obtainMessage(CALLBACK_ON_NETWORK_REMOVEDE, availableIfaces));
    }

    public void videoPolicyCallBackNotice(int type, int duration, int length) {
        if (type != 1 && type != 2) {
            return;
        }
        Handler handler = this.mCallbackHandler;
        handler.sendMessage(handler.obtainMessage(CALLBACK_VIDEO_POLICY_CHANGED, type, duration, Integer.valueOf(length)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenStatus(boolean isScreenON) {
        synchronized (this.mLock) {
            this.mIsScreenON = isScreenON;
            if (!isScreenON) {
                this.mScreenOffSystemTime = System.currentTimeMillis();
            } else {
                this.mScreenOffSystemTime = -1L;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ifaceAddCallBackSend(Message msg) {
        List<String> res = (List) msg.obj;
        int N = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                this.mCallbacks.getBroadcastItem(i).ifaceAdded(res);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException at ifaceAddCallBackSend()");
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ifaceRemovedCallBackSend(Message msg) {
        List<String> res = (List) msg.obj;
        int N = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                this.mCallbacks.getBroadcastItem(i).ifaceRemoved(res);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException at ifaceRemovedCallBackSend()");
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dsdaStateChangedCallBackSend(Message msg) {
        boolean res = ((Boolean) msg.obj).booleanValue();
        int N = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                this.mCallbacks.getBroadcastItem(i).dsdaStateChanged(res);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException at dsdaStateChangedCallBackSend()");
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void networkPriorityCallBackSend(Message msg) {
        int priorityMode = msg.arg1;
        int trafficPolicy = msg.arg2;
        int thermalLevel = ((Integer) msg.obj).intValue();
        Log.i(TAG, "networkPriorityCallBackNotice: " + priorityMode + " " + trafficPolicy + " " + thermalLevel);
        int N = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                this.mCallbacks.getBroadcastItem(i).onNetworkPriorityChanged(priorityMode, trafficPolicy, thermalLevel);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException at networkPriorityCallBackSend()");
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void mediaPlayerPolicyCallBackSend(Message msg) {
        int type = msg.arg1;
        int duration = msg.arg2;
        int length = ((Integer) msg.obj).intValue();
        int N = this.mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                this.mCallbacks.getBroadcastItem(i).mediaPlayerPolicyNotify(type, duration, length);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException at mediaPlayerPolicyCallBackSend()");
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    @Deprecated
    public Map<String, String> getQoEByAvailableIfaceName(String ifaceName) {
        return null;
    }

    public NetLinkLayerQoE getQoEByAvailableIfaceNameV1(String ifaceName) {
        NetLinkLayerQoE netLayerQoE;
        if (!networkSDKPermissionCheck("getQoEByAvailableIfaceName")) {
            Log.e(TAG, "getQoEByAvailableIfaceNameV1 No permission");
            return null;
        }
        if (!this.mIsScreenON) {
            return null;
        }
        if (StatusManager.WLAN_IFACE_0.equals(ifaceName)) {
            netLayerQoE = this.mMasterNetLayerQoE;
        } else {
            if (!"wlan1".equals(ifaceName)) {
                return null;
            }
            netLayerQoE = this.mSlaveNetLayerQoE;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (this.mLock) {
                WlanLinkLayerQoE availableIfaceCheckAndQoEPull = availableIfaceCheckAndQoEPull(ifaceName);
                this.mMiuiWlanLayerQoE = availableIfaceCheckAndQoEPull;
                if (availableIfaceCheckAndQoEPull == null) {
                    return null;
                }
                netLayerQoE = NetworkSDKUtils.copyFromNetWlanLinkLayerQoE(availableIfaceCheckAndQoEPull, netLayerQoE);
                calculateLostAndRetryRatio(netLayerQoE);
                return netLayerQoE;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private WlanLinkLayerQoE availableIfaceCheckAndQoEPull(String ifaceName) {
        MiuiWifiManager miuiWifiManager;
        if (!this.mStatusManager.getAvailableIfaces().contains(ifaceName) || (miuiWifiManager = this.mMiuiWifiManager) == null) {
            return null;
        }
        WlanLinkLayerQoE mWlanLinkLayerQoE = miuiWifiManager.getQoEByAvailableIfaceName(ifaceName);
        if (mWlanLinkLayerQoE == null) {
            Log.e(TAG, "mMiuiWlanLayerQoE == null");
            return null;
        }
        return mWlanLinkLayerQoE;
    }

    private void calculateLostAndRetryRatio(NetLinkLayerQoE mNetLayerQoE) {
        calculateAllPackages(mNetLayerQoE);
        long j = this.mTxAndRxStatistics[2];
        if (j == 0) {
            return;
        }
        double curLostRatio = r0[0] / j;
        mNetLayerQoE.setMpduLostRatio(Math.round(this.DECIMAL_CONVERSION * curLostRatio) / this.DECIMAL_CONVERSION);
        long[] jArr = this.mTxAndRxStatistics;
        double curRetryRatio = jArr[1] / jArr[2];
        mNetLayerQoE.setRetriesRatio(Math.round(this.DECIMAL_CONVERSION * curRetryRatio) / this.DECIMAL_CONVERSION);
    }

    private void calculateAllPackages(NetLinkLayerQoE mNetLayerQoE) {
        this.mTxAndRxStatistics[0] = mNetLayerQoE.getLostmpdu_be() + mNetLayerQoE.getLostmpdu_bk() + mNetLayerQoE.getLostmpdu_vi() + mNetLayerQoE.getLostmpdu_vo();
        this.mTxAndRxStatistics[1] = mNetLayerQoE.getRetries_be() + mNetLayerQoE.getRetries_bk() + mNetLayerQoE.getRetries_vi() + mNetLayerQoE.getRetries_vo();
        long[] jArr = this.mTxAndRxStatistics;
        long txmpdu_be = mNetLayerQoE.getTxmpdu_be() + mNetLayerQoE.getTxmpdu_bk() + mNetLayerQoE.getTxmpdu_vi() + mNetLayerQoE.getTxmpdu_vo() + mNetLayerQoE.getRxmpdu_be() + mNetLayerQoE.getRxmpdu_bk() + mNetLayerQoE.getRxmpdu_vi() + mNetLayerQoE.getRxmpdu_vo();
        long[] jArr2 = this.mTxAndRxStatistics;
        jArr[2] = txmpdu_be + jArr2[0] + jArr2[1];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wifiLinkLayerStatsPoll() {
        synchronized (this.mLock) {
            RemoteCallbackList<IAIDLMiuiNetQoECallback> remoteCallbackList = this.mWlanQoECallbacks;
            if (remoteCallbackList != null && this.mAppStatsPollInterval != null) {
                if (!this.mIsOpenWifiLinkPoll) {
                    Log.d(TAG, "wifiLinkLayerStatsPoll polling normal exit");
                    return;
                }
                int count = remoteCallbackList.getRegisteredCallbackCount();
                if (count == 0) {
                    Log.d(TAG, "wifiLinkLayerStatsPoll polling Self-monitoring exit");
                    return;
                }
                if (count != this.mAppStatsPollInterval.size()) {
                    Log.e(TAG, "wifiLinkLayerStatsPoll netLinkQoE callBack unregister Error polling exit");
                    netLinkCallBackUnRegisterError();
                }
                wifiLinkStatsCallbackSend();
                Handler handler = this.mCallbackHandler;
                handler.sendMessageDelayed(handler.obtainMessage(CALLBACK_START_WIFI_LINKSTATS), this.mWifiLinkStatsPollMillis);
            }
        }
    }

    private void netLinkCallBackUnRegisterError() {
        synchronized (this.mLock) {
            Map<IBinder, Long> appStatsPollMillis = new HashMap<>();
            Map<IBinder, Long> appStatsPollInterval = new HashMap<>();
            Map<IBinder, Integer> appCallBackMapToUid = new HashMap<>();
            int count = this.mWlanQoECallbacks.getRegisteredCallbackCount();
            long curSystemTime = System.currentTimeMillis();
            long minInterval = -1;
            for (int i = 0; i < count; i++) {
                IAIDLMiuiNetQoECallback callBack = this.mWlanQoECallbacks.getRegisteredCallbackItem(i);
                IBinder callBackKey = callBack.asBinder();
                appStatsPollMillis.put(callBackKey, Long.valueOf(curSystemTime));
                appStatsPollInterval.put(callBackKey, this.mAppStatsPollInterval.get(callBackKey));
                appCallBackMapToUid.put(callBackKey, this.mAppCallBackMapToUid.get(callBackKey));
                if (minInterval == -1) {
                    minInterval = this.mAppStatsPollInterval.get(callBackKey).longValue();
                } else if (this.mAppStatsPollInterval.get(callBackKey).longValue() < minInterval) {
                    minInterval = this.mAppStatsPollInterval.get(callBackKey).longValue();
                }
            }
            if (minInterval != -1) {
                this.mWifiLinkStatsPollMillis = minInterval;
                Log.e(TAG, "netLinkCallBackUnRegisterError interval = " + this.mWifiLinkStatsPollMillis);
            }
            if (count == 0) {
                Log.e(TAG, "netLinkCallBackUnRegisterError interval = -1");
                this.mIsOpenWifiLinkPoll = false;
                this.mWifiLinkStatsPollMillis = -1L;
            }
            this.mAppStatsPollMillis = appStatsPollMillis;
            this.mAppStatsPollInterval = appStatsPollInterval;
            this.mAppCallBackMapToUid = appCallBackMapToUid;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void appWifiSelectionMonitor() {
        if (!checkAppForegroundStatus()) {
            return;
        }
        Handler handler = this.mCallbackHandler;
        handler.sendMessageDelayed(handler.obtainMessage(MSG_START_APP_WIFI_SELECTION_MONITOR), 10000L);
    }

    private boolean checkAppForegroundStatus() {
        Log.d(TAG, "mOffScreenCount = " + this.mOffScreenCount);
        synchronized (this.mLock) {
            if (!this.mIsCloseRoaming) {
                return false;
            }
            if (!this.mWifiSelectionApps.contains(Integer.valueOf(this.mStatusManager.getForegroundUid()))) {
                this.mOffScreenCount++;
            } else {
                this.mOffScreenCount = 0;
            }
            if (this.mOffScreenCount != 18) {
                return true;
            }
            clearAppWifiSelectionMonitor();
            return false;
        }
    }

    private void clearAppWifiSelectionMonitor() {
        forceOpenFrameworkRoaming();
        this.mOffScreenCount = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dualWifiBackgroundMonitor() {
        if (!checkAppDualWifiBackgroundStatus()) {
            return;
        }
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(107), 30000L);
    }

    private boolean checkAppDualWifiBackgroundStatus() {
        Log.d(TAG, "dual-wifi background monitor count = " + this.mDualWifiBackgroundCount);
        synchronized (this.mLock) {
            if (this.mIsScreenON && this.mDualWifiApps != null && setOrGetEverOpenedDualWifi(false, false) && isSlaveWifiEnabledAndOthersOpt(0) != 0) {
                if (!this.mDualWifiApps.contains(Integer.valueOf(this.mStatusManager.getForegroundUid()))) {
                    int i = this.mDualWifiBackgroundCount + 1;
                    this.mDualWifiBackgroundCount = i;
                    if (i != 10) {
                        return true;
                    }
                    closeDualWifiTemporarilyByBackground();
                    return false;
                }
                resetMonitorStatus();
                return false;
            }
            resetMonitorStatus();
            return false;
        }
    }

    private void closeDualWifiTemporarilyByBackground() {
        resetMonitorStatus();
        SlaveWifiManager slaveWifiManager = this.mSlaveWifiManager;
        if (slaveWifiManager == null) {
            return;
        }
        if (slaveWifiManager.isBusySlaveWifi()) {
            Log.d(TAG, "dual-wifi background monitor slave wifi is busy");
            synchronized (this.mLock) {
                this.mDualWifiBackgroundCount = 9;
            }
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(107), 30000L);
            return;
        }
        synchronized (this.mLock) {
            if (this.mSlaveWifiManager.setWifiSlaveEnabled(false)) {
                this.mIsEverClosedByBackground = true;
            }
        }
        Log.d(TAG, "dual-wifi background monitor close dual wifi");
    }

    private void resetMonitorStatus() {
        synchronized (this.mLock) {
            this.mDualWifiBackgroundCount = 0;
            this.mIsStartMonitorByBackground = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dualWifiBackgroundMonitorRestart() {
        SlaveWifiManager slaveWifiManager;
        synchronized (this.mLock) {
            if (!setOrGetEverOpenedDualWifi(false, false)) {
                this.mIsEverClosedByBackground = false;
                return;
            }
            if (this.mDualWifiApps == null) {
                this.mIsEverClosedByBackground = false;
                return;
            }
            if (this.mIsEverClosedByBackground && (slaveWifiManager = this.mSlaveWifiManager) != null) {
                slaveWifiManager.setWifiSlaveEnabled(true);
            }
            Log.d(TAG, "dual-wifi background monitor restart dual wifi");
        }
    }

    private void wifiLinkStatsCallbackSend() {
        NetLinkLayerQoE statsWlan0;
        NetLinkLayerQoE statsWlan02 = getQoEByAvailableIfaceNameV1(StatusManager.WLAN_IFACE_0);
        NetLinkLayerQoE statsWlan1 = getQoEByAvailableIfaceNameV1("wlan1");
        synchronized (this.mLock) {
            try {
                try {
                    int N = this.mWlanQoECallbacks.beginBroadcast();
                    int i = 0;
                    while (i < N) {
                        try {
                            IAIDLMiuiNetQoECallback callBack = this.mWlanQoECallbacks.getBroadcastItem(i);
                            IBinder callBackKey = callBack.asBinder();
                            long appQoESendInterval = this.mAppStatsPollInterval.get(callBackKey).longValue();
                            long curSystemTime = System.currentTimeMillis();
                            long interval = curSystemTime - this.mAppStatsPollMillis.get(callBackKey).longValue();
                            long deviation = ((appQoESendInterval / 100) * 33) + 50;
                            if (interval < appQoESendInterval || interval > appQoESendInterval + deviation) {
                                statsWlan0 = statsWlan02;
                                if (interval > appQoESendInterval + deviation) {
                                    resetQoECallback(callBackKey, curSystemTime);
                                }
                            } else {
                                callBack.masterQoECallBack(statsWlan02);
                                callBack.slaveQoECallBack(statsWlan1);
                                statsWlan0 = statsWlan02;
                                try {
                                    this.mAppStatsPollMillis.put(callBackKey, Long.valueOf(curSystemTime));
                                } catch (RemoteException e) {
                                    e = e;
                                    Log.e(TAG, "RemoteException at wifiLinkStatsCallbackSend()");
                                    e.printStackTrace();
                                    i++;
                                    statsWlan02 = statsWlan0;
                                }
                            }
                        } catch (RemoteException e2) {
                            e = e2;
                            statsWlan0 = statsWlan02;
                        }
                        i++;
                        statsWlan02 = statsWlan0;
                    }
                    this.mWlanQoECallbacks.finishBroadcast();
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void resetQoECallback(IBinder callBackKey, long curSystemTime) {
        this.mAppStatsPollMillis.put(callBackKey, Long.valueOf(curSystemTime));
        Log.d(TAG, "wifiLinkStatsCallbackSend: resetOneQoECallback");
    }

    public boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb, int type) {
        int uid = Binder.getCallingUid();
        return enableWifiSelectionOpt(cb, type, uid);
    }

    public boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb, int type, int uid) {
        boolean ret;
        if (type < 0 || type > 3) {
            return false;
        }
        if (!networkSDKPermissionCheck("enableWifiSelectionOpt")) {
            Log.e(TAG, "enableWifiSelectionOpt No permission");
            return false;
        }
        synchronized (this.mLock) {
            this.mNetSelectCallbacks.register(cb);
            ret = setFrameworkAndDriverRoaming(false, type, uid);
        }
        return ret;
    }

    public void triggerWifiSelection() {
        RemoteCallbackList<IAIDLMiuiNetSelectCallback> remoteCallbackList;
        if (!networkSDKPermissionCheck("triggerWifiSelection")) {
            Log.e(TAG, "triggerWifiSelection No permission");
            return;
        }
        Map<String, String> map = this.mBssidToNetworkId;
        if (map == null) {
            this.mBssidToNetworkId = new HashMap();
        } else {
            map.clear();
        }
        List<String> netAndBssidLists = new ArrayList<>();
        MiuiWifiManager miuiWifiManager = this.mMiuiWifiManager;
        if (miuiWifiManager != null && (netAndBssidLists = miuiWifiManager.netSDKGetAvailableNetworkIdAndBssid()) == null) {
            netAndBssidLists = new ArrayList<>();
        }
        List<String> retBssidLists = new ArrayList<>();
        for (int i = 0; i < netAndBssidLists.size(); i++) {
            String[] netwrokIdWithBssid = netAndBssidLists.get(i).split(",");
            if (netwrokIdWithBssid.length == 2) {
                this.mBssidToNetworkId.put(netwrokIdWithBssid[1], netwrokIdWithBssid[0]);
                retBssidLists.add(netwrokIdWithBssid[1]);
            }
        }
        synchronized (this.mLock) {
            int N = this.mNetSelectCallbacks.beginBroadcast();
            for (int i2 = 0; i2 < N; i2++) {
                try {
                    try {
                        try {
                            this.mNetSelectCallbacks.getBroadcastItem(i2).avaliableBssidCb(retBssidLists);
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException at triggerWifiSelection()");
                        }
                    } catch (Exception e2) {
                        Log.d(TAG, e2.toString());
                        e2.printStackTrace();
                        remoteCallbackList = this.mNetSelectCallbacks;
                    }
                } catch (Throwable th) {
                    this.mNetSelectCallbacks.finishBroadcast();
                    throw th;
                }
            }
            remoteCallbackList = this.mNetSelectCallbacks;
            remoteCallbackList.finishBroadcast();
        }
    }

    public void reportBssidScore(Map<String, String> bssidScores) {
        if (!networkSDKPermissionCheck("reportBssidScore")) {
            Log.e(TAG, "reportBssidScore No permission");
            return;
        }
        if (bssidScores == null || bssidScores.size() <= 0) {
            return;
        }
        String objBssid = "";
        int score = -1;
        for (Map.Entry entry : bssidScores.entrySet()) {
            int tmpScore = Integer.valueOf(entry.getValue()).intValue();
            if (tmpScore >= 0 && tmpScore > score) {
                score = tmpScore;
                String objBssid2 = entry.getKey();
                objBssid = objBssid2;
            }
        }
        if (score >= 0 && !connectionWithBssid(objBssid)) {
            broadcastPrimaryConnectingFailed();
        }
    }

    private boolean connectionWithBssid(String bssid) {
        MiuiWifiManager miuiWifiManager;
        RemoteCallbackList<IAIDLMiuiNetSelectCallback> remoteCallbackList;
        Map<String, String> map = this.mBssidToNetworkId;
        if (map == null || (miuiWifiManager = this.mMiuiWifiManager) == null || !miuiWifiManager.netSDKConnectPrimaryWithBssid(map.get(bssid), bssid)) {
            return false;
        }
        synchronized (this.mLock) {
            int N = this.mNetSelectCallbacks.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    try {
                        try {
                            this.mNetSelectCallbacks.getBroadcastItem(i).connectionStatusCb(1);
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException at connectionWithBssid()");
                        }
                    } catch (Exception e2) {
                        Log.d(TAG, e2.toString());
                        e2.printStackTrace();
                        remoteCallbackList = this.mNetSelectCallbacks;
                    }
                } catch (Throwable th) {
                    this.mNetSelectCallbacks.finishBroadcast();
                    throw th;
                }
            }
            remoteCallbackList = this.mNetSelectCallbacks;
            remoteCallbackList.finishBroadcast();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastPrimaryConnected() {
        RemoteCallbackList<IAIDLMiuiNetSelectCallback> remoteCallbackList;
        synchronized (this.mLock) {
            if (this.mIsCloseRoaming) {
                int N = this.mNetSelectCallbacks.beginBroadcast();
                for (int i = 0; i < N; i++) {
                    try {
                        try {
                            try {
                                this.mNetSelectCallbacks.getBroadcastItem(i).connectionStatusCb(2);
                            } catch (RemoteException e) {
                                Log.e(TAG, "RemoteException at broadcastPrimaryConnected()");
                            }
                        } catch (Exception e2) {
                            Log.d(TAG, e2.toString());
                            e2.printStackTrace();
                            remoteCallbackList = this.mNetSelectCallbacks;
                        }
                    } catch (Throwable th) {
                        this.mNetSelectCallbacks.finishBroadcast();
                        throw th;
                    }
                }
                remoteCallbackList = this.mNetSelectCallbacks;
                remoteCallbackList.finishBroadcast();
            }
        }
    }

    private void broadcastPrimaryConnectingFailed() {
        RemoteCallbackList<IAIDLMiuiNetSelectCallback> remoteCallbackList;
        synchronized (this.mLock) {
            if (this.mIsCloseRoaming) {
                int N = this.mNetSelectCallbacks.beginBroadcast();
                for (int i = 0; i < N; i++) {
                    try {
                        try {
                            try {
                                this.mNetSelectCallbacks.getBroadcastItem(i).connectionStatusCb(3);
                            } catch (RemoteException e) {
                                Log.e(TAG, "RemoteException at broadcastPrimaryConnectingFailed()");
                            }
                        } catch (Exception e2) {
                            Log.d(TAG, e2.toString());
                            e2.printStackTrace();
                            remoteCallbackList = this.mNetSelectCallbacks;
                        }
                    } catch (Throwable th) {
                        this.mNetSelectCallbacks.finishBroadcast();
                        throw th;
                    }
                }
                remoteCallbackList = this.mNetSelectCallbacks;
                remoteCallbackList.finishBroadcast();
            }
        }
    }

    public boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb) {
        int uid = Binder.getCallingUid();
        return disableWifiSelectionOpt(cb, uid);
    }

    public boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb, int uid) {
        boolean ret;
        if (!networkSDKPermissionCheck("disableWifiSelectionOpt")) {
            Log.e(TAG, "disableWifiSelectionOpt No permission");
            return false;
        }
        synchronized (this.mLock) {
            this.mNetSelectCallbacks.unregister(cb);
            ret = setFrameworkAndDriverRoaming(true, -1, uid);
        }
        return ret;
    }

    private boolean setFrameworkAndDriverRoaming(boolean isOpen, int type, int uid) {
        boolean ret;
        synchronized (this.mLock) {
            if (!isOpen) {
                this.mWifiSelectionApps.add(Integer.valueOf(uid));
                if (this.mIsCloseRoaming) {
                    return true;
                }
                MiuiWifiManager miuiWifiManager = this.mMiuiWifiManager;
                if (miuiWifiManager != null && type != 3) {
                    ret = miuiWifiManager.netSDKSetIsDisableRoam(isOpen, type);
                } else {
                    ret = true;
                }
                this.mIsCloseRoaming = true;
                Handler handler = this.mCallbackHandler;
                handler.sendMessage(handler.obtainMessage(MSG_START_APP_WIFI_SELECTION_MONITOR));
            } else {
                this.mWifiSelectionApps.remove(Integer.valueOf(uid));
                if (this.mMiuiWifiManager == null || this.mWifiSelectionApps.size() != 0) {
                    return true;
                }
                this.mIsCloseRoaming = false;
                ret = this.mMiuiWifiManager.netSDKSetIsDisableRoam(isOpen, -1);
            }
            return ret;
        }
    }

    private void forceOpenFrameworkRoaming() {
        RemoteCallbackList<IAIDLMiuiNetSelectCallback> remoteCallbackList;
        synchronized (this.mLock) {
            if (this.mIsCloseRoaming) {
                int N = this.mNetSelectCallbacks.beginBroadcast();
                for (int i = 0; i < N; i++) {
                    try {
                        try {
                            try {
                                this.mNetSelectCallbacks.getBroadcastItem(i).connectionStatusCb(4);
                            } catch (RemoteException e) {
                                Log.e(TAG, "RemoteException at forceOpenFrameworkRoaming()");
                            }
                        } catch (Exception e2) {
                            Log.d(TAG, e2.toString());
                            e2.printStackTrace();
                            remoteCallbackList = this.mNetSelectCallbacks;
                        }
                    } catch (Throwable th) {
                        this.mNetSelectCallbacks.finishBroadcast();
                        throw th;
                    }
                }
                remoteCallbackList = this.mNetSelectCallbacks;
                remoteCallbackList.finishBroadcast();
                int count = this.mNetSelectCallbacks.getRegisteredCallbackCount();
                List<IAIDLMiuiNetSelectCallback> callbacks = new ArrayList<>();
                for (int i2 = 0; i2 < count; i2++) {
                    callbacks.add(this.mNetSelectCallbacks.getRegisteredCallbackItem(i2));
                }
                for (IAIDLMiuiNetSelectCallback cb : callbacks) {
                    this.mNetSelectCallbacks.unregister(cb);
                }
                MiuiWifiManager miuiWifiManager = this.mMiuiWifiManager;
                if (miuiWifiManager != null) {
                    miuiWifiManager.netSDKSetIsDisableRoam(true, -1);
                }
                this.mIsCloseRoaming = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CallbackHandler extends Handler {
        public CallbackHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int result = ((Integer) msg.obj).intValue();
                    int N = NetworkSDKService.this.mCallbacks.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        try {
                            ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i)).onScanSuccussed(result);
                        } catch (RemoteException e) {
                            NetworkSDKService networkSDKService = NetworkSDKService.this;
                            networkSDKService.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService.mCallbacks.getBroadcastItem(i));
                        }
                    }
                    NetworkSDKService.this.mCallbacks.finishBroadcast();
                    return;
                case 1:
                    boolean res = ((Boolean) msg.obj).booleanValue();
                    int N2 = NetworkSDKService.this.mCallbacks.beginBroadcast();
                    for (int i2 = 0; i2 < N2; i2++) {
                        try {
                            ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i2)).onSetSlaveWifiResult(res);
                        } catch (RemoteException e2) {
                            NetworkSDKService networkSDKService2 = NetworkSDKService.this;
                            networkSDKService2.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService2.mCallbacks.getBroadcastItem(i2));
                        }
                    }
                    NetworkSDKService.this.mCallbacks.finishBroadcast();
                    return;
                case 2:
                    boolean res2 = ((Boolean) msg.obj).booleanValue();
                    int N3 = NetworkSDKService.this.mCallbacks.beginBroadcast();
                    for (int i3 = 0; i3 < N3; i3++) {
                        try {
                            ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i3)).onSlaveWifiConnected(res2);
                        } catch (RemoteException e3) {
                            NetworkSDKService networkSDKService3 = NetworkSDKService.this;
                            networkSDKService3.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService3.mCallbacks.getBroadcastItem(i3));
                        }
                    }
                    NetworkSDKService.this.mCallbacks.finishBroadcast();
                    return;
                case 3:
                    boolean res3 = ((Boolean) msg.obj).booleanValue();
                    int N4 = NetworkSDKService.this.mCallbacks.beginBroadcast();
                    for (int i4 = 0; i4 < N4; i4++) {
                        try {
                            ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i4)).onSlaveWifiDisconnected(res3);
                        } catch (RemoteException e4) {
                            NetworkSDKService networkSDKService4 = NetworkSDKService.this;
                            networkSDKService4.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService4.mCallbacks.getBroadcastItem(i4));
                        }
                    }
                    NetworkSDKService.this.mCallbacks.finishBroadcast();
                    return;
                case NetworkSDKService.CALLBACK_ON_NETWORK_ADDED /* 1007 */:
                    NetworkSDKService.this.ifaceAddCallBackSend(msg);
                    return;
                case NetworkSDKService.CALLBACK_ON_NETWORK_REMOVEDE /* 1008 */:
                    NetworkSDKService.this.ifaceRemovedCallBackSend(msg);
                    return;
                case NetworkSDKService.CALLBACK_START_WIFI_LINKSTATS /* 1009 */:
                    NetworkSDKService.this.wifiLinkLayerStatsPoll();
                    return;
                case NetworkSDKService.MSG_START_APP_WIFI_SELECTION_MONITOR /* 1010 */:
                    try {
                        NetworkSDKService.this.appWifiSelectionMonitor();
                        return;
                    } catch (Exception e5) {
                        e5.printStackTrace();
                        return;
                    }
                case NetworkSDKService.CALLBACK_ON_SLAVE_WIFI_ENABLE /* 1011 */:
                    boolean res4 = ((Boolean) msg.obj).booleanValue();
                    int N5 = NetworkSDKService.this.mCallbacks.beginBroadcast();
                    for (int i5 = 0; i5 < N5; i5++) {
                        try {
                            ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i5)).onSlaveWifiEnable(res4);
                        } catch (RemoteException e6) {
                            NetworkSDKService networkSDKService5 = NetworkSDKService.this;
                            networkSDKService5.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService5.mCallbacks.getBroadcastItem(i5));
                        }
                    }
                    NetworkSDKService.this.mCallbacks.finishBroadcast();
                    return;
                case NetworkSDKService.CALLBACK_ON_DSDA_STATE_CHANGED /* 1012 */:
                    NetworkSDKService.this.dsdaStateChangedCallBackSend(msg);
                    return;
                case NetworkSDKService.CALLBACK_VIDEO_POLICY_CHANGED /* 1013 */:
                    NetworkSDKService.this.mediaPlayerPolicyCallBackSend(msg);
                    return;
                case NetworkSDKService.CALLBACK_SET_SCREEN_STATUS /* 1014 */:
                    boolean screenStatus = ((Boolean) msg.obj).booleanValue();
                    NetworkSDKService.this.setScreenStatus(screenStatus);
                    return;
                case NetworkSDKService.CALLBACK_NETWORK_PRIORITY_CHANGED /* 1015 */:
                    NetworkSDKService.this.networkPriorityCallBackSend(msg);
                    break;
                case NetworkSDKService.CALLBACK_ON_SLAVE_WIFI_ENABLE_V1 /* 1016 */:
                    break;
                default:
                    return;
            }
            int status = ((Integer) msg.obj).intValue();
            int N6 = NetworkSDKService.this.mCallbacks.beginBroadcast();
            for (int i6 = 0; i6 < N6; i6++) {
                try {
                    ((IAIDLMiuiNetworkCallback) NetworkSDKService.this.mCallbacks.getBroadcastItem(i6)).onSlaveWifiEnableV1(status);
                } catch (RemoteException e7) {
                    NetworkSDKService networkSDKService6 = NetworkSDKService.this;
                    networkSDKService6.unregisterCallback((IAIDLMiuiNetworkCallback) networkSDKService6.mCallbacks.getBroadcastItem(i6));
                }
            }
            NetworkSDKService.this.mCallbacks.finishBroadcast();
        }
    }

    public boolean networkSDKPermissionCheck(String callingName) {
        int uid = Binder.getCallingUid();
        if (isSystemProcess(uid)) {
            return true;
        }
        synchronized (mListLock) {
            if (this.mNetworkSDKUid.contains(Integer.toString(uid))) {
                Log.d(TAG, "WhiteList passed: " + uid);
                String permission = this.mPermission.get(callingName);
                if (permission == null) {
                    return true;
                }
                int pid = Binder.getCallingPid();
                int checkResult = this.mContext.checkPermission(permission, pid, uid);
                if (checkResult == 0) {
                    Log.d(TAG, "permission granted: " + callingName + " " + permission);
                    return true;
                }
                Log.d(TAG, "permission denied: " + callingName + " " + permission);
                return false;
            }
            Log.d(TAG, "WhiteList denied: " + uid);
            return false;
        }
    }

    public boolean cellularNetworkSDKPermissionCheck(String callingName) {
        int uid = Binder.getCallingUid();
        if (isSystemProcess(uid)) {
            return true;
        }
        synchronized (mCellularListLock) {
            if (this.mCellularNetworkSDKUid.contains(Integer.toString(uid))) {
                Log.d(TAG, "WhiteList passed(cellular): " + uid);
                String permission = this.mPermission.get(callingName);
                if (permission == null) {
                    return true;
                }
                int pid = Binder.getCallingPid();
                int checkResult = this.mContext.checkPermission(permission, pid, uid);
                if (checkResult == 0) {
                    Log.d(TAG, "permission granted(cellular): " + callingName + " " + permission);
                    return true;
                }
                Log.d(TAG, "permission denied(cellular): " + callingName + " " + permission);
                return false;
            }
            Log.d(TAG, "WhiteList denied(cellular): " + uid);
            return false;
        }
    }

    private boolean isSystemProcess(int uid) {
        return uid < 10000;
    }

    private void initPermission() {
        HashMap<String, String> hashMap = new HashMap<>();
        this.mPermission = hashMap;
        hashMap.put("setSockPrio", "android.permission.CHANGE_NETWORK_STATE");
        this.mPermission.put("setTCPCongestion", "android.permission.CHANGE_NETWORK_STATE");
        this.mPermission.put("isSupportDualWifi", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("setSlaveWifiEnabled", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("connectSlaveWifi", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("disconnectSlaveWifi", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("suspendBackgroundScan", "android.permission.CHANGE_WIFI_STATE");
        this.mPermission.put("resumeBackgroundScan", "android.permission.CHANGE_WIFI_STATE");
        this.mPermission.put("suspendWifiPowerSave", "android.permission.CHANGE_WIFI_STATE");
        this.mPermission.put("resumeWifiPowerSave", "android.permission.CHANGE_WIFI_STATE");
        this.mPermission.put("registerWifiLinkCallback", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("unregisterWifiLinkCallback", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("getAvailableIfaces", "android.permission.ACCESS_NETWORK_STATE");
        this.mPermission.put("getQoEByAvailableIfaceName", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("setTrafficTransInterface", "android.permission.CHANGE_NETWORK_STATE");
        this.mPermission.put("requestAppTrafficStatistics", "android.permission.ACCESS_NETWORK_STATE");
        this.mPermission.put("registerNetLinkCallback", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("unregisterNetLinkCallback", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("enableWifiSelectionOpt", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("triggerWifiSelection", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("reportBssidScore", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("disableWifiSelectionOpt", "android.permission.ACCESS_WIFI_STATE");
        this.mPermission.put("isSlaveWifiEnabledAndOthersOpt", "android.permission.ACCESS_WIFI_STATE");
    }

    private void initNetworkSDKCloudObserver() {
        final ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                NetworkSDKService.this.upDateCloudWhiteList();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(NETWORK_NETWORKBOOST_WHITELIST), false, observer);
        new Thread(new Runnable() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                observer.onChange(false);
            }
        }).start();
    }

    private void initMibridgeCloudObserver() {
        final ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                NetworkSDKService.this.upDateCloudWhiteList();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MIBRIDGE_WHITELIST), false, observer);
        new Thread(new Runnable() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                observer.onChange(false);
            }
        }).start();
    }

    private void initCellularNetworkSDKCloudObserver() {
        final ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                NetworkSDKService.this.updataCellularSdkList();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("cellular_networkboost_sdk_white_list_pkg_name"), false, observer);
        new Thread(new Runnable() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                observer.onChange(false);
            }
        }).start();
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addDataScheme("package");
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.10
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int uid;
                String action = intent.getAction();
                String packageName = intent.getData().getSchemeSpecificPart();
                if (TextUtils.isEmpty(packageName) || (uid = intent.getIntExtra("android.intent.extra.UID", -1)) == -1) {
                    return;
                }
                if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (NetworkSDKService.this.mMarketUid != -1 && packageName.equals("com.xiaomi.market")) {
                        NetworkSDKService.this.mMarketUid = uid;
                    }
                    if (NetworkSDKService.this.mDownloadsUid == -1 && packageName.equals("com.android.providers.downloads")) {
                        NetworkSDKService.this.mDownloadsUid = uid;
                    }
                }
                if (NetworkSDKService.this.mNetworkSDKPN.contains(packageName)) {
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        synchronized (NetworkSDKService.mListLock) {
                            NetworkSDKService.this.mNetworkSDKUid.add(Integer.toString(uid));
                            NetworkSDKService.this.mAppUidToPackName.put(Integer.valueOf(uid), packageName);
                        }
                        Log.i(NetworkSDKService.TAG, "ACTION_PACKAGE_ADDED uid = " + uid + packageName);
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        synchronized (NetworkSDKService.mListLock) {
                            NetworkSDKService.this.mNetworkSDKUid.remove(Integer.toString(uid));
                            NetworkSDKService.this.mAppUidToPackName.remove(Integer.valueOf(uid));
                        }
                        Log.i(NetworkSDKService.TAG, "ACTION_PACKAGE_REMOVED uid = " + uid + packageName);
                    }
                }
                if (NetworkSDKService.this.mCellularNetworkSDKPN.contains(packageName)) {
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        synchronized (NetworkSDKService.mCellularListLock) {
                            NetworkSDKService.this.mCellularNetworkSDKUid.add(Integer.toString(uid));
                        }
                        Log.i(NetworkSDKService.TAG, "Cellular ACTION_PACKAGE_ADDED uid = " + uid + packageName);
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        synchronized (NetworkSDKService.mCellularListLock) {
                            NetworkSDKService.this.mCellularNetworkSDKUid.remove(Integer.toString(uid));
                        }
                        Log.i(NetworkSDKService.TAG, "Cellular ACTION_PACKAGE_REMOVED uid = " + uid + packageName);
                    }
                }
            }
        };
        this.mContext.registerReceiver(receiver, intentFilter, 4);
    }

    private void initDualDataBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_VIDEO_APPS_POLICY_NOTIFY);
        intentFilter.addAction(ACTION_MSIM_VOICE_CAPABILITY_CHANGED);
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.11
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i(NetworkSDKService.TAG, "receive action = " + action);
                if (NetworkSDKService.ACTION_VIDEO_APPS_POLICY_NOTIFY.equals(action)) {
                    int type = intent.getIntExtra("type", -1);
                    int duration = intent.getIntExtra(NetworkSDKService.KEY_DURATION, -1);
                    int length = intent.getIntExtra(NetworkSDKService.KEY_LENGTH, -1);
                    NetworkSDKService.this.videoPolicyCallBackNotice(type, duration, length);
                    Log.i(NetworkSDKService.TAG, "ACTION_VIDEO_APPS_POLICY_NOTIFY, type=" + type + ", duration=" + duration + ", length=" + length);
                    return;
                }
                if (NetworkSDKService.ACTION_MSIM_VOICE_CAPABILITY_CHANGED.equals(action)) {
                    NetworkSDKService networkSDKService = NetworkSDKService.this;
                    networkSDKService.mDsdaCapability = networkSDKService.getCurrentDSDAState();
                    NetworkSDKService.this.mHandler.obtainMessage(104).sendToTarget();
                    Log.i(NetworkSDKService.TAG, "ACTION_MSIM_VOICE_CAPABILITY_CHANGED dsdaStateChanged mDsdaCapability:" + NetworkSDKService.this.mDsdaCapability);
                }
            }
        };
        this.mContext.registerReceiver(receiver, intentFilter);
    }

    private void initWhiteList() {
        this.mNetworkSDKPN = ConcurrentHashMap.newKeySet();
        this.mNetworkSDKUid = new HashSet();
        this.mNetworkSDKPN.add("com.android.settings");
        setNetworkSDKUid();
        Log.d(TAG, "white list init");
    }

    private void initCellularWhiteList() {
        this.mCellularNetworkSDKPN = new HashSet();
        this.mCellularNetworkSDKUid = new HashSet();
        this.mCellularNetworkSDKPN.add("com.miui.vpnsdkmanager");
        this.mCellularNetworkSDKPN.add("com.miui.securityadd");
        setCellularNetworkSDKUid();
        Log.d(TAG, "Cellular white list init");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void upDateCloudWhiteList() {
        this.mNetworkSDKPN.clear();
        this.mAppUidToPackName.clear();
        this.mNetworkSDKPN.add("com.android.settings");
        synchronized (mListLock) {
            this.mNetworkSDKUid.clear();
        }
        setNetworkSDKUid();
        Log.d(TAG, "cloud white list changed");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updataCellularSdkList() {
        this.mCellularNetworkSDKPN.clear();
        this.mCellularNetworkSDKPN.add("com.miui.vpnsdkmanager");
        this.mCellularNetworkSDKPN.add("com.miui.securityadd");
        synchronized (mCellularListLock) {
            this.mCellularNetworkSDKUid.clear();
        }
        setCellularNetworkSDKUid();
        Log.d(TAG, "cloud Cellular white list changed");
    }

    private void setNetworkSDKUid() {
        String[] packages;
        String[] packages2;
        String whiteString_netSDK = "";
        try {
            whiteString_netSDK = Settings.System.getString(this.mContext.getContentResolver(), NETWORK_NETWORKBOOST_WHITELIST);
        } catch (Exception e) {
            Log.e(TAG, "get cloud whitelist error " + e);
        }
        Log.d(TAG, "Cloud whiteString_netSDK networkSDKApp:" + whiteString_netSDK);
        if (!TextUtils.isEmpty(whiteString_netSDK) && (packages2 = whiteString_netSDK.split(",")) != null) {
            for (String str : packages2) {
                this.mNetworkSDKPN.add(str);
            }
        }
        String whiteString_mibridge = mibridgeWhiteList();
        Log.d(TAG, "Cloud whiteString_mibridge networkSDKApp:" + whiteString_mibridge);
        if (!TextUtils.isEmpty(whiteString_mibridge) && (packages = whiteString_mibridge.split(",")) != null) {
            for (String str2 : packages) {
                this.mNetworkSDKPN.add(str2);
            }
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            List<PackageInfo> apps = pm.getInstalledPackages(1);
            for (PackageInfo app : apps) {
                if (app.packageName != null && app.applicationInfo != null && this.mNetworkSDKPN.contains(app.packageName)) {
                    int uid = app.applicationInfo.uid;
                    synchronized (mListLock) {
                        this.mNetworkSDKUid.add(Integer.toString(uid));
                    }
                }
                int uid2 = this.mMarketUid;
                if (uid2 == -1 && app.packageName.equals("com.xiaomi.market")) {
                    this.mMarketUid = app.applicationInfo.uid;
                }
                if (this.mDownloadsUid == -1 && app.packageName.equals("com.android.providers.downloads")) {
                    this.mDownloadsUid = app.applicationInfo.uid;
                }
                if (app != null && app.packageName != null && app.applicationInfo != null) {
                    this.mAppUidToPackName.put(Integer.valueOf(app.applicationInfo.uid), app.packageName);
                }
            }
        } catch (Exception e2) {
            Log.e(TAG, "setNetworkSDKUid error " + e2);
        }
        String white_list = "";
        for (String pn : this.mNetworkSDKPN) {
            white_list = (white_list + " ") + pn;
        }
        synchronized (mListLock) {
            for (String uid3 : this.mNetworkSDKUid) {
                white_list = (white_list + " ") + uid3;
            }
        }
        Log.d(TAG, "white list pn&uid : " + white_list);
    }

    private String mibridgeWhiteList() {
        return Settings.System.getString(this.mContext.getContentResolver(), MIBRIDGE_WHITELIST);
    }

    private void setCellularNetworkSDKUid() {
        String[] packages;
        String cellularWhiteString_netSDK = "";
        try {
            cellularWhiteString_netSDK = Settings.Global.getString(this.mContext.getContentResolver(), "cellular_networkboost_sdk_white_list_pkg_name");
        } catch (Exception e) {
            Log.e(TAG, "get Cellular cloud whitelist error " + e);
        }
        Log.d(TAG, "Cloud CellularNetworkSDKApp:" + cellularWhiteString_netSDK);
        if (!TextUtils.isEmpty(cellularWhiteString_netSDK) && (packages = cellularWhiteString_netSDK.split(",")) != null) {
            for (int i = 0; i < packages.length; i++) {
                this.mCellularNetworkSDKPN.add(packages[i]);
                Log.d(TAG, "setCellularNetworkSDKUid packages:" + packages[i]);
            }
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            List<PackageInfo> apps = pm.getInstalledPackages(1);
            for (PackageInfo app : apps) {
                if (app.packageName != null && app.applicationInfo != null && this.mCellularNetworkSDKPN.contains(app.packageName)) {
                    int uid = app.applicationInfo.uid;
                    synchronized (mCellularListLock) {
                        this.mCellularNetworkSDKUid.add(Integer.toString(uid));
                        Log.d(TAG, "setCellularNetworkSDKUid uid :" + uid);
                    }
                }
            }
        } catch (Exception e2) {
            Log.e(TAG, "setCellularNetworkSDKUid error " + e2);
        }
        String white_list = "";
        for (String pn : this.mCellularNetworkSDKPN) {
            white_list = (white_list + " ") + pn;
        }
        synchronized (mCellularListLock) {
            Iterator<String> it = this.mCellularNetworkSDKUid.iterator();
            while (it.hasNext()) {
                white_list = (white_list + " ") + it.next();
            }
        }
        Log.d(TAG, "setCellularNetworkSDKUid :white list pn&uid : " + white_list);
    }

    public boolean isSupportDualCelluarData() {
        DualCelluarDataService dualCelluarDataService;
        Log.i(TAG, "isSupportDualCelluarData");
        if (cellularNetworkSDKPermissionCheck("isSupportDualCelluarData") && (dualCelluarDataService = this.mDualCelluarDataService) != null) {
            return dualCelluarDataService.supportDualCelluarData();
        }
        return false;
    }

    public boolean isSupportMediaPlayerPolicy() {
        DualCelluarDataService dualCelluarDataService;
        Log.i(TAG, "isSupportMediaPlayerPolicy");
        if (cellularNetworkSDKPermissionCheck("isSupportMediaPlayerPolicy") && (dualCelluarDataService = this.mDualCelluarDataService) != null) {
            return dualCelluarDataService.supportMediaPlayerPolicy();
        }
        return false;
    }

    public boolean isCelluarDSDAState() {
        Log.i(TAG, "isCelluarDSDAState");
        if (!cellularNetworkSDKPermissionCheck("isCelluarDSDAState")) {
            return false;
        }
        return this.mDsdaCapability;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getCurrentDSDAState() {
        return 3 == SystemProperties.getInt("ril.multisim.voice_capability", 0);
    }

    public boolean setDualCelluarDataEnable(boolean enable) {
        Log.i(TAG, "setDualCelluarDataEnable");
        boolean result = false;
        if (!cellularNetworkSDKPermissionCheck("setDualCelluarDataEnable")) {
            return false;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
                DualCelluarDataService dualCelluarDataService = this.mDualCelluarDataService;
                if (dualCelluarDataService != null) {
                    result = dualCelluarDataService.setDualCelluarDataEnable(enable);
                }
            } catch (Exception e) {
                Log.e(TAG, "setDualCelluarDataEnable error " + e);
            }
            return result;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int isSlaveWifiEnabledAndOthersOpt(int type) {
        int uid = Binder.getCallingUid();
        return isSlaveWifiEnabledAndOthersOpt(type, uid);
    }

    public int isSlaveWifiEnabledAndOthersOpt(int type, int uid) {
        if (!networkSDKPermissionCheck("isSlaveWifiEnabledAndOthersOpt")) {
            Log.e(TAG, "isSlaveWifiEnabledAndOthersOpt No permission");
            return -1;
        }
        if (this.mSlaveWifiManager == null) {
            return -1;
        }
        boolean isMarket = false;
        int i = this.mMarketUid;
        if (i != -1 && i == uid) {
            isMarket = true;
        }
        if (this.mSlaService == null) {
            this.mSlaService = SLAService.getInstance(this.mContext);
        }
        String packName = null;
        Map<Integer, String> map = this.mAppUidToPackName;
        if (map != null) {
            String packName2 = map.get(Integer.valueOf(uid));
            packName = packName2;
        }
        if (packName == null && type != 0) {
            return -1;
        }
        String uidStr = String.valueOf(uid);
        if (isMarket) {
            packName = packName + ",com.android.providers.downloads";
            uidStr = uidStr + "," + this.mDownloadsUid;
        }
        Log.d(TAG, "isSlaveWifiEnabledAndOthersOpt packName = " + packName);
        long callingUid = Binder.clearCallingIdentity();
        try {
            switch (type) {
                case 0:
                    return this.mSlaveWifiManager.isSlaveWifiEnabled() ? 1 : 0;
                case 1:
                    if (!this.mSlaService.setDoubleWifiWhiteList(2, packName, uidStr, true)) {
                        return -1;
                    }
                    this.mSlaService.setDWUidToSlad();
                    return 1;
                case 2:
                    if (!this.mSlaService.setDoubleWifiWhiteList(1, packName, uidStr, true)) {
                        return -1;
                    }
                    this.mSlaService.setDWUidToSlad();
                    return 1;
                case 3:
                    return this.mSlaService.setDoubleWifiWhiteList(4, packName, uidStr, false) ? 1 : 0;
                default:
                    return -1;
            }
        } catch (Exception e) {
            Log.d(TAG, "isSlaveWifiEnabledAndOthersOpt clearCallingIdentity uid = " + callingUid);
            e.printStackTrace();
            return -1;
        } finally {
            Binder.restoreCallingIdentity(callingUid);
        }
    }

    public void dumpModule(PrintWriter writer) {
        try {
            writer.println("NetworkSDKService begin:");
            writer.println("    isConnectSlaveAp:" + this.isConnectSlaveAp);
            writer.println("NetworkSDKService end.\n");
        } catch (Exception ex) {
            Log.e(TAG, "dump failed!", ex);
            try {
                writer.println("Dump of NetworkSDKService failed:" + ex);
                writer.println("NetworkSDKService end.\n");
            } catch (Exception e) {
                Log.e(TAG, "dump failure failed:" + e);
            }
        }
    }

    private void registerQEEChangeObserver() {
        Log.d(TAG, "QEE cloud observer register");
        this.mQEEObserver = new ContentObserver(this.mHandler) { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.12
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean isQEENeedOff = "off".equals(Settings.System.getString(NetworkSDKService.this.mContext.getContentResolver(), NetworkSDKService.CLOUD_QEE_ENABLE));
                Log.d(NetworkSDKService.TAG, "QEE cloud change, isQEENeedOff: " + isQEENeedOff);
                try {
                    if (!isQEENeedOff) {
                        SystemProperties.set(NetworkSDKService.QEE_PROP, "on");
                    } else {
                        SystemProperties.set(NetworkSDKService.QEE_PROP, "off");
                    }
                } catch (Exception e) {
                    Log.d(NetworkSDKService.TAG, "QEE setprop faild");
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_QEE_ENABLE), false, this.mQEEObserver, 0);
        this.mHandler.post(new Runnable() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService.13
            @Override // java.lang.Runnable
            public void run() {
                NetworkSDKService.this.mQEEObserver.onChange(false);
            }
        });
    }

    private void unregisterQEEChangeObserver() {
        if (this.mQEEObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mQEEObserver);
            this.mQEEObserver = null;
        }
    }
}
