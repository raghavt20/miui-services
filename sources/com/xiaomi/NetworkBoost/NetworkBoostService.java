package com.xiaomi.NetworkBoost;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost;
import com.xiaomi.NetworkBoost.MscsService.MscsService;
import com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService;
import com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService;
import com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService;
import com.xiaomi.NetworkBoost.slaservice.SLAApp;
import com.xiaomi.NetworkBoost.slaservice.SLAService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.util.DeviceLevel;
import vendor.xiaomi.hidl.minet.V1_0.IMiNetCallback;

/* loaded from: classes.dex */
public class NetworkBoostService extends Service {
    public static final String ACTION_START_SERVICE = "ACTION_SLA_JAVA_SERVICE_START";
    public static final int BASE = 1000;
    public static final int MINETD_CMD_FLOWSTATGET = 1005;
    public static final int MINETD_CMD_FLOWSTATSTART = 1003;
    public static final int MINETD_CMD_FLOWSTATSTOP = 1004;
    public static final int MINETD_CMD_SETSOCKPRIO = 1001;
    public static final int MINETD_CMD_SETTCPCONGESTION = 1002;
    public static final int SERVICE_VERSION = 6;
    private static final String TAG = "NetworkBoostService";
    private static Context mContext;
    private static boolean mJniLoaded = false;
    private static volatile NetworkBoostService sInstance = null;
    private SLAService mSLAService = null;
    private NetworkSDKService mNetworkSDKService = null;
    private StatusManager mStatusManager = null;
    private NetworkAccelerateSwitchService mNetworkAccelerateSwitchService = null;
    private MscsService mMscsService = null;
    private final NetworkBoostServiceManager mAIDLMiuiNetworkBoost = new NetworkBoostServiceManager();
    private final SLAServiceBinder mAIDLSLAServiceBinder = new SLAServiceBinder();
    private final IMiNetCallback.Stub mMiNetCallback = new IMiNetCallback.Stub() { // from class: com.xiaomi.NetworkBoost.NetworkBoostService.1
        @Override // vendor.xiaomi.hidl.minet.V1_0.IMiNetCallback
        public void notifyCommon(int cmd, String attr) throws RemoteException {
            Log.i(NetworkBoostService.TAG, cmd + "," + attr);
            switch (cmd) {
                case 1005:
                    onFlowStatGet(attr);
                    return;
                default:
                    Log.i(NetworkBoostService.TAG, "unknow cmd:" + cmd);
                    return;
            }
        }

        public void onFlowStatGet(String attr) {
            Log.i(NetworkBoostService.TAG, "onFlowStatGet" + attr);
            if (NetworkBoostService.this.mNetworkAccelerateSwitchService != null) {
                NetworkBoostService.this.mNetworkAccelerateSwitchService.notifyCollectIpAddress(attr);
            }
        }
    };

    /* loaded from: classes.dex */
    public class NetworkBoostServiceManager extends IAIDLMiuiNetworkBoost.Stub {
        public static final String SERVICE_NAME = "xiaomi.NetworkBoostServiceManager";

        public NetworkBoostServiceManager() {
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean setSockPrio(int fd, int prio) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.setSockPrio(fd, prio);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean setTCPCongestion(int fd, String cc) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.setTCPCongestion(fd, cc);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean isSupportDualWifi() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.isSupportDualWifi();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean setSlaveWifiEnable(boolean enable) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.setSlaveWifiEnabled(enable);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean connectSlaveWifi(int networkId) {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean disconnectSlaveWifi() {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean registerCallback(IAIDLMiuiNetworkCallback cb) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.registerCallback(cb);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean unregisterCallback(IAIDLMiuiNetworkCallback cb) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.unregisterCallback(cb);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public Map<String, String> getQoEByAvailableIfaceName(String ifaceName) {
            return null;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean setTrafficTransInterface(int fd, String bindInterface) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.setTrafficTransInterface(fd, bindInterface);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean activeScan(int[] channelList) {
            Log.i(NetworkBoostService.TAG, "activeScan:" + channelList);
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.activeScan(channelList);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean abortScan() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.abortScan();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean registerWifiLinkCallback(IAIDLMiuiWlanQoECallback cb) {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean suspendBackgroundScan() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.suspendBackgroundScan();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean unregisterWifiLinkCallback(IAIDLMiuiWlanQoECallback cb) {
            return false;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean resumeBackgroundScan() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.resumeBackgroundScan();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean suspendWifiPowerSave() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.suspendWifiPowerSave();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean resumeWifiPowerSave() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.resumeWifiPowerSave();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public Map<String, String> getAvailableIfaces() {
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    return NetworkBoostService.this.mNetworkSDKService.getAvailableIfaces();
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " getAvailableIfaces:" + e);
                    e.printStackTrace();
                }
            }
            return new HashMap();
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public Map<String, String> requestAppTrafficStatistics(int type, long startTime, long endTime) {
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    return NetworkBoostService.this.mNetworkSDKService.requestAppTrafficStatistics(type, startTime, endTime);
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " requestAppTrafficStatistics:" + e);
                    e.printStackTrace();
                }
            }
            return new HashMap();
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public int getServiceVersion() {
            Log.d(NetworkBoostService.TAG, "Service Version:6");
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                return 6;
            }
            return 100;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public NetLinkLayerQoE getQoEByAvailableIfaceNameV1(String ifaceName) {
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    return NetworkBoostService.this.mNetworkSDKService.getQoEByAvailableIfaceNameV1(ifaceName);
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " getQoEByAvailableIfaceNameV1:" + e);
                    e.printStackTrace();
                }
            }
            return new NetLinkLayerQoE();
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean registerNetLinkCallback(IAIDLMiuiNetQoECallback cb, int interval) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.registerNetLinkCallback(cb, interval);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " registerNetLinkCallback:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean unregisterNetLinkCallback(IAIDLMiuiNetQoECallback cb) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.unregisterNetLinkCallback(cb);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " unregisterNetLinkCallback:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean registerNetLinkCallbackByUid(IAIDLMiuiNetQoECallback cb, int interval, int uid) {
            if (!NetworkBoostService.this.isSystemProcess() || NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.registerNetLinkCallback(cb, interval, uid);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " registerNetLinkCallback:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean unregisterNetLinkCallbackByUid(IAIDLMiuiNetQoECallback cb, int uid) {
            if (!NetworkBoostService.this.isSystemProcess() || NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.unregisterNetLinkCallback(cb, uid);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " unregisterNetLinkCallback:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public Map<String, String> requestAppTrafficStatisticsByUid(int type, long startTime, long endTime, int uid) {
            if (!NetworkBoostService.this.isSystemProcess()) {
                return new HashMap();
            }
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    return NetworkBoostService.this.mNetworkSDKService.requestAppTrafficStatistics(type, startTime, endTime, uid);
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " requestAppTrafficStatistics:" + e);
                    e.printStackTrace();
                }
            }
            return new HashMap();
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean enableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback cb, int type, int uid) {
            if (!NetworkBoostService.this.isSystemProcess() || NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.enableWifiSelectionOpt(cb, type, uid);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " enableWifiSelectionOpt:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean disableWifiSelectionOptByUid(IAIDLMiuiNetSelectCallback cb, int uid) {
            if (!NetworkBoostService.this.isSystemProcess() || NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.disableWifiSelectionOpt(cb, uid);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " disableWifiSelectionOpt:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean enableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb, int type) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.enableWifiSelectionOpt(cb, type);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " enableWifiSelectionOpt:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public void triggerWifiSelection() {
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    NetworkBoostService.this.mNetworkSDKService.triggerWifiSelection();
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " triggerWifiSelection:" + e);
                    e.printStackTrace();
                }
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public void reportBssidScore(Map<String, String> bssidScores) {
            if (NetworkBoostService.this.mNetworkSDKService != null) {
                try {
                    NetworkBoostService.this.mNetworkSDKService.reportBssidScore(bssidScores);
                } catch (Exception e) {
                    Log.d(NetworkBoostService.TAG, " reportBssidScore:" + e);
                    e.printStackTrace();
                }
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean disableWifiSelectionOpt(IAIDLMiuiNetSelectCallback cb) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            try {
                boolean ret = NetworkBoostService.this.mNetworkSDKService.disableWifiSelectionOpt(cb);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " disableWifiSelectionOpt:" + e);
                e.printStackTrace();
                return false;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean isSupportDualCelluarData() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.isSupportDualCelluarData();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean isSupportMediaPlayerPolicy() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.isSupportMediaPlayerPolicy();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean isCelluarDSDAState() {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.isCelluarDSDAState();
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public boolean setDualCelluarDataEnable(boolean enable) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mNetworkSDKService.setDualCelluarDataEnable(enable);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public int isSlaveWifiEnabledAndOthersOpt(int type) {
            if (NetworkBoostService.this.mNetworkSDKService == null) {
                return -1;
            }
            try {
                int ret = NetworkBoostService.this.mNetworkSDKService.isSlaveWifiEnabledAndOthersOpt(type);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " isSlaveWifiEnabledAndOthersOpt:" + e);
                e.printStackTrace();
                return -1;
            }
        }

        @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkBoost
        public int isSlaveWifiEnabledAndOthersOptByUid(int type, int uid) {
            if (!NetworkBoostService.this.isSystemProcess() || NetworkBoostService.this.mNetworkSDKService == null) {
                return -1;
            }
            try {
                int ret = NetworkBoostService.this.mNetworkSDKService.isSlaveWifiEnabledAndOthersOpt(type, uid);
                return ret;
            } catch (Exception e) {
                Log.d(NetworkBoostService.TAG, " isSlaveWifiEnabledAndOthersOptByUid:" + e);
                e.printStackTrace();
                return -1;
            }
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            if (NetworkBoostService.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                writer.println("Permission Denial: can't dump xiaomi.NetworkBoostServiceManager from pid=" + getCallingPid() + ", uid=" + getCallingUid() + " due to missing android.permission.DUMP permission");
            } else {
                NetworkBoostService.dumpModule(fd, writer, args);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSystemProcess() {
        int uid = Binder.getCallingUid();
        return uid < 10000;
    }

    /* loaded from: classes.dex */
    public class SLAServiceBinder extends IAIDLSLAService.Stub {
        public static final String SERVICE_NAME = "xiaomi.SLAService";

        public SLAServiceBinder() {
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean addUidToLinkTurboWhiteList(String uid) {
            if (NetworkBoostService.this.mSLAService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mSLAService.addUidToLinkTurboWhiteList(uid);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean removeUidInLinkTurboWhiteList(String uid) {
            if (NetworkBoostService.this.mSLAService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mSLAService.removeUidInLinkTurboWhiteList(uid);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public String getLinkTurboWhiteList() {
            if (NetworkBoostService.this.mSLAService == null) {
                return null;
            }
            String tmpLists = NetworkBoostService.this.mSLAService.getLinkTurboWhiteList();
            return tmpLists;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public List<SLAApp> getLinkTurboAppsTraffic() {
            if (NetworkBoostService.this.mSLAService == null) {
                return null;
            }
            List<SLAApp> tmpLists = NetworkBoostService.this.mSLAService.getLinkTurboAppsTraffic();
            return tmpLists;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public boolean setLinkTurboEnable(boolean enable) {
            if (NetworkBoostService.this.mSLAService == null) {
                return false;
            }
            boolean ret = NetworkBoostService.this.mSLAService.setLinkTurboEnable(enable);
            return ret;
        }

        @Override // com.xiaomi.NetworkBoost.slaservice.IAIDLSLAService
        public List<String> getLinkTurboDefaultPn() {
            if (NetworkBoostService.this.mSLAService == null) {
                return null;
            }
            List<String> ret = NetworkBoostService.this.mSLAService.getLinkTurboDefaultPn();
            return ret;
        }
    }

    public static NetworkBoostService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkBoostService.class) {
                if (sInstance == null) {
                    sInstance = new NetworkBoostService(context);
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
            synchronized (NetworkBoostService.class) {
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

    public static void dumpModule(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (sInstance != null) {
            synchronized (NetworkBoostService.class) {
                if (sInstance != null) {
                    try {
                        sInstance.dump(fd, writer, args);
                    } catch (Exception e) {
                        Log.e(TAG, "dumpModule dump catch:", e);
                    }
                } else {
                    Log.w(TAG, "dumpModule: instance is uninitialized.");
                }
            }
            return;
        }
        Log.w(TAG, "dumpModule: instance is uninitialized.");
    }

    private NetworkBoostService(Context context) {
        mContext = context;
    }

    public void onNetworkPriorityChanged(int priorityMode, int trafficPolicy, int thermalLevel) {
        StatusManager statusManager = this.mStatusManager;
        if (statusManager != null) {
            statusManager.onNetworkPriorityChanged(priorityMode, trafficPolicy, thermalLevel);
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        Log.i(TAG, " onCreate ");
        if (!mJniLoaded) {
            try {
                System.loadLibrary("networkboostjni");
                mJniLoaded = true;
                Log.i(TAG, "load libnetworkboostjni.so");
            } catch (Exception e) {
                Log.e(TAG, "libnetworkboostjni.so load failed:" + e);
            } catch (UnsatisfiedLinkError e2) {
                Log.e(TAG, "load libnetworkboostjni.so failed", e2);
            }
        }
        this.mStatusManager = StatusManager.getInstance(mContext);
        this.mSLAService = SLAService.getInstance(mContext);
        this.mNetworkSDKService = NetworkSDKService.getInstance(mContext);
        if (mJniLoaded) {
            this.mNetworkAccelerateSwitchService = NetworkAccelerateSwitchService.getInstance(mContext, this.mMiNetCallback);
        } else {
            Log.e(TAG, "WlanRamdumpCollector required libnetworkboostjni.so");
        }
        if (!DeviceLevel.IS_MIUI_MIDDLE_VERSION) {
            Log.d(TAG, "is not MIUI Middle or is not port MIUI Middle, start MscsService");
            this.mMscsService = MscsService.getInstance(mContext);
        }
        Log.d(TAG, "addService >>>");
        try {
            ServiceManager.addService(NetworkBoostServiceManager.SERVICE_NAME, this.mAIDLMiuiNetworkBoost);
            Log.d(TAG, "addService NetworkBoost success");
        } catch (Exception e3) {
            Log.e(TAG, "addService NetworkBoost failed:" + e3);
        }
        try {
            ServiceManager.addService(SLAServiceBinder.SERVICE_NAME, this.mAIDLSLAServiceBinder);
            Log.d(TAG, "addService SLAService success");
        } catch (Exception e4) {
            Log.e(TAG, "addService SLAService failed:" + e4);
        }
        Log.d(TAG, "addService <<<");
        super.onCreate();
    }

    @Override // android.app.Service
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, " onStart ");
        if (intent != null && intent.getExtras() != null) {
            Log.d(TAG, "onStart, get intent:");
        }
        super.onStart(intent, startId);
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.i(TAG, " onBind ");
        if (IAIDLMiuiNetworkBoost.class.getName().equals(intent.getAction())) {
            return this.mAIDLMiuiNetworkBoost;
        }
        if (IAIDLSLAService.class.getName().equals(intent.getAction())) {
            return this.mAIDLSLAServiceBinder;
        }
        return null;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, " onUnbind ");
        return super.onUnbind(intent);
    }

    @Override // android.app.Service
    public void onDestroy() {
        Log.i(TAG, " onDestroy ");
        if (this.mStatusManager != null) {
            StatusManager.destroyInstance();
            this.mStatusManager = null;
        }
        if (this.mSLAService != null) {
            SLAService.destroyInstance();
            this.mSLAService = null;
        }
        if (this.mNetworkSDKService != null) {
            NetworkSDKService.destroyInstance();
            this.mNetworkSDKService = null;
        }
        if (this.mNetworkAccelerateSwitchService != null) {
            NetworkAccelerateSwitchService.destroyInstance();
            this.mNetworkAccelerateSwitchService = null;
        }
        if (this.mMscsService != null) {
            MscsService.destroyInstance();
            this.mMscsService = null;
        }
        super.onDestroy();
    }

    @Override // android.app.Service
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        try {
            writer.println("Dump of NetworkBoostService begin:");
            StatusManager statusManager = this.mStatusManager;
            if (statusManager != null) {
                statusManager.dumpModule(writer);
            } else {
                writer.println("mStatusManager is null");
            }
            SLAService sLAService = this.mSLAService;
            if (sLAService != null) {
                sLAService.dumpModule(writer);
            } else {
                writer.println("mSLAService is null");
            }
            NetworkSDKService networkSDKService = this.mNetworkSDKService;
            if (networkSDKService != null) {
                networkSDKService.dumpModule(writer);
            } else {
                writer.println("mNetworkSDKService is null");
            }
            NetworkAccelerateSwitchService networkAccelerateSwitchService = this.mNetworkAccelerateSwitchService;
            if (networkAccelerateSwitchService != null) {
                networkAccelerateSwitchService.dumpModule(writer);
            } else {
                writer.println("mNetworkAccelerateSwitchService is null");
            }
            writer.println("Dump of NetworkBoostService end");
        } catch (Exception ex) {
            Log.e(TAG, "dump failed!", ex);
            try {
                writer.println("Dump of NetworkBoostService failed:" + ex);
                writer.println("Dump of NetworkBoostService end");
            } catch (Exception e) {
                Log.e(TAG, "dump failure failed:" + e);
            }
        }
    }
}
