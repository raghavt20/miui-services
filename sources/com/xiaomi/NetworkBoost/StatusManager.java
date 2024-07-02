package com.xiaomi.NetworkBoost;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.SlaveWifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.telephony.SubscriptionInfo;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManager;

/* loaded from: classes.dex */
public class StatusManager {
    public static final int CALLBACK_DEFAULT_NETWORK_INTERFACE = 111;
    public static final int CALLBACK_NETWORK_INTERFACE = 107;
    public static final int CALLBACK_NETWORK_PRIORITY_ADD = 110;
    public static final int CALLBACK_NETWORK_PRIORITY_CHANGED = 109;
    public static final int CALLBACK_SCREEN_STATUS = 108;
    public static final int CALLBACK_VPN_STATUS = 112;
    public static final int DATA_READY = 3;
    private static final int DELAY_TIME = 10000;
    private static final int EVENT_CHECK_SENSOR_STATUS = 106;
    private static final int EVENT_DEFAULT_INTERFACE_ONAVAILABLE = 102;
    private static final int EVENT_DELAY_SET_CELLULAR_CALLBACK = 103;
    private static final int EVENT_GET_MODEM_SIGNAL_STRENGTH = 105;
    private static final int EVENT_INTERFACE_ONAVAILABLE = 100;
    private static final int EVENT_INTERFACE_ONLOST = 101;
    private static final int EVENT_SIM_STATE_CHANGED = 104;
    private static final int LTE_RSRP_LOWER_LIMIT = -140;
    private static final int LTE_RSRP_UPPER_LIMIT = -43;
    private static final int MOVEMENT_SENSOR_TYPE = 33171070;
    private static final int NA_RAT = -1;
    private static final int NETWORK_TYPE_LTE = 13;
    private static final int NETWORK_TYPE_LTE_CA = 19;
    private static final int NETWORK_TYPE_NR = 20;
    private static final int NETWORK_TYPE_NR_CA = 30;
    private static final int NR_RSRP_LOWER_LIMIT = -140;
    private static final int NR_RSRP_UPPER_LIMIT = -44;
    public static final String ON_AVAILABLE = "ON_AVAILABLE";
    public static final String ON_LOST = "ON_LOST";
    public static final int PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
    private static final int RSRP_POOR_THRESHOLD = -105;
    private static final int SENSOR_DELAY_TIME = 5000;
    private static final int SIGNAL_STRENGTH_DELAY_MIILLIS = 30000;
    private static final int SIGNAL_STRENGTH_GOOD = 0;
    private static final int SIGNAL_STRENGTH_POOR = 1;
    public static final int SLAVE_DATA_READY = 4;
    public static final int SLAVE_WIFI_READY = 2;
    private static final String TAG = "NetworkBoostStatusManager";
    private static final int UNAVAILABLE = Integer.MAX_VALUE;
    private static final boolean VPN_STATUS_DISABLE = false;
    private static final boolean VPN_STATUS_ENABLE = true;
    private static final int WIFI_BAND_5_GHZ_HBS = 258;
    public static final int WIFI_READY = 1;
    public static final String WLAN_IFACE_0 = "wlan0";
    public static final String WLAN_IFACE_1 = "wlan1";
    private static ConnectivityManager mConnectivityManager;
    private static Method mReflectScreenState;
    private static SlaveWifiManager mSlaveWifiManager;
    private static WifiManager mWifiManager;
    public static volatile StatusManager sInstance;
    private IActivityManager mActivityManager;
    private AudioManager mAudioManager;
    private AudioManager.OnModeChangedListener mAudioModeListener;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private NetworkBoostProcessMonitor mNetworkBoostProcessMonitor;
    private PowerManager mPowerManager;
    private BroadcastReceiver mScreenStatusReceiver;
    private android.telephony.TelephonyManager mTelephonyManager;
    private BroadcastReceiver mTelephonyStatusReceiver;
    private HandlerThread mWacthdogHandlerThread;
    private String mInterface = "";
    private int mIfaceNumber = 0;
    private boolean mWifiReady = false;
    private boolean mSlaveWifiReady = false;
    private boolean mDataReady = false;
    private boolean mSlaveDataReady = false;
    private int mFUid = -1;
    private int mDefaultNetid = 0;
    private String mDefaultIface = "";
    private int mDefaultIfaceStatus = 0;
    private boolean mVPNstatus = false;
    private SensorManager mSensorManager = null;
    private SensorEvent mCurrentSensorevent = null;
    private boolean mSensorFlag = false;
    private int mPriorityMode = 0;
    private int mTrafficPolicy = 0;
    private int mThermalLevel = 0;
    private List<IAppStatusListener> mAppStatusListenerList = new ArrayList();
    private List<INetworkInterfaceListener> mNetworkInterfaceListenerList = new ArrayList();
    private List<IDefaultNetworkInterfaceListener> mDefaultNetworkInterfaceListenerList = new ArrayList();
    private List<IScreenStatusListener> mScreenStatusListenerList = new ArrayList();
    private List<INetworkPriorityListener> mNetworkPriorityListenerList = new ArrayList();
    private List<IModemSignalStrengthListener> mModemSignalStrengthListener = new ArrayList();
    private List<IMovementSensorStatusListener> mMovementSensorStatusListenerList = new ArrayList();
    private List<IVPNListener> mVPNListener = new ArrayList();
    private IForegroundInfoListener.Stub mForegroundInfoListener = new IForegroundInfoListener.Stub() { // from class: com.xiaomi.NetworkBoost.StatusManager.1
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) throws RemoteException {
            Log.i(StatusManager.TAG, "onForegroundInfoChanged uid:" + Integer.toString(foregroundInfo.mForegroundUid) + ", isColdStart:" + foregroundInfo.isColdStart());
            synchronized (StatusManager.this.mAppStatusListenerList) {
                StatusManager.this.mFUid = foregroundInfo.mForegroundUid;
                for (IAppStatusListener listener : StatusManager.this.mAppStatusListenerList) {
                    listener.onForegroundInfoChanged(foregroundInfo);
                }
            }
        }
    };
    private final IUidObserver mUidObserver = new IUidObserver.Stub() { // from class: com.xiaomi.NetworkBoost.StatusManager.2
        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            Log.i(StatusManager.TAG, "onUidGone uid: " + uid);
            synchronized (StatusManager.this.mAppStatusListenerList) {
                for (IAppStatusListener listener : StatusManager.this.mAppStatusListenerList) {
                    listener.onUidGone(uid, disabled);
                }
            }
        }

        public void onUidActive(int uid) throws RemoteException {
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid, int adj) {
        }
    };
    private final HashMap<Integer, NetworkCallbackInterface> mNetworkMap = new HashMap<>();
    private ConnectivityManager.NetworkCallback mWifiNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.xiaomi.NetworkBoost.StatusManager.4
        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            StatusManager.this.mHandler.sendMessage(StatusManager.this.mHandler.obtainMessage(100, network));
            StatusManager.this.setSmartDNSInterface(network);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            StatusManager.this.mHandler.sendMessage(StatusManager.this.mHandler.obtainMessage(101, network));
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }
    };
    private ConnectivityManager.NetworkCallback mCellularNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.xiaomi.NetworkBoost.StatusManager.5
        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            StatusManager.this.mHandler.sendMessage(StatusManager.this.mHandler.obtainMessage(100, network));
            StatusManager.this.setSmartDNSInterface(network);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            StatusManager.this.mHandler.sendMessage(StatusManager.this.mHandler.obtainMessage(101, network));
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }
    };
    private ConnectivityManager.NetworkCallback mDefaultNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.xiaomi.NetworkBoost.StatusManager.6
        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            Log.i(StatusManager.TAG, "Default Network Callback onAvailable");
            StatusManager.this.mHandler.sendMessage(StatusManager.this.mHandler.obtainMessage(102, network));
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }
    };
    private ConnectivityManager.NetworkCallback mVPNCallback = new ConnectivityManager.NetworkCallback() { // from class: com.xiaomi.NetworkBoost.StatusManager.7
        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            Log.i(StatusManager.TAG, "VPN Callback onAvailable");
            synchronized (StatusManager.this.mVPNListener) {
                StatusManager.this.mVPNstatus = true;
                for (IVPNListener listener : StatusManager.this.mVPNListener) {
                    listener.onVPNStatusChange(StatusManager.this.mVPNstatus);
                }
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            Log.i(StatusManager.TAG, "VPN Callback onLost");
            synchronized (StatusManager.this.mVPNListener) {
                StatusManager.this.mVPNstatus = false;
                for (IVPNListener listener : StatusManager.this.mVPNListener) {
                    listener.onVPNStatusChange(StatusManager.this.mVPNstatus);
                }
            }
        }
    };
    private final SensorEventListener mSensorListener = new SensorEventListener() { // from class: com.xiaomi.NetworkBoost.StatusManager.10
        @Override // android.hardware.SensorEventListener
        public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override // android.hardware.SensorEventListener
        public final void onSensorChanged(SensorEvent event) {
            synchronized (StatusManager.this.mMovementSensorStatusListenerList) {
                StatusManager.this.mCurrentSensorevent = event;
                for (IMovementSensorStatusListener listener : StatusManager.this.mMovementSensorStatusListenerList) {
                    listener.onSensorChanged(event);
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public interface IAppStatusListener {
        void onAudioChanged(int i);

        void onForegroundInfoChanged(ForegroundInfo foregroundInfo);

        void onUidGone(int i, boolean z);
    }

    /* loaded from: classes.dex */
    public interface IDefaultNetworkInterfaceListener {
        void onDefaultNetwrokChange(String str, int i);
    }

    /* loaded from: classes.dex */
    public interface IModemSignalStrengthListener {
        void isModemSingnalStrengthStatus(int i);
    }

    /* loaded from: classes.dex */
    public interface IMovementSensorStatusListener {
        void onSensorChanged(SensorEvent sensorEvent);
    }

    /* loaded from: classes.dex */
    public interface INetworkInterfaceListener {
        void onNetwrokInterfaceChange(String str, int i, boolean z, boolean z2, boolean z3, boolean z4, String str2);
    }

    /* loaded from: classes.dex */
    public interface INetworkPriorityListener {
        void onNetworkPriorityChanged(int i, int i2, int i3);
    }

    /* loaded from: classes.dex */
    public interface IScreenStatusListener {
        void onScreenOFF();

        void onScreenON();
    }

    /* loaded from: classes.dex */
    public interface IVPNListener {
        void onVPNStatusChange(boolean z);
    }

    public static StatusManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (StatusManager.class) {
                if (sInstance == null) {
                    sInstance = new StatusManager(context);
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
            synchronized (StatusManager.class) {
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

    private StatusManager(Context context) {
        this.mContext = null;
        this.mContext = context.getApplicationContext();
    }

    public void onCreate() {
        Log.i(TAG, "onCreate ");
        HandlerThread handlerThread = new HandlerThread("NetoworkBoostWacthdogHandler");
        this.mWacthdogHandlerThread = handlerThread;
        handlerThread.start();
        registerNetworkCallback();
        registerVPNCallback();
        registerDefaultNetworkCallback();
        registerForegroundInfoListener();
        registerUidObserver();
        registerAudioModeChangedListener();
        registerScreenStatusReceiver();
        registerModemSignalStrengthObserver();
        registerTelephonyStatusReceiver();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        this.mWacthdogHandlerThread.quit();
        unregisterNetworkCallback();
        unregisterVPNCallback();
        unregisterDefaultNetworkCallback();
        unregisterForegroundInfoListener();
        unregisterUidObserver();
        unregisterAudioModeChangedListener();
        unregisterScreenStatusReceiver();
        unregisterModemSignalStrengthObserver();
        unregisterTelephonyStatusReceiver();
    }

    public HandlerThread getNetworkBoostWatchdogHandler() {
        return this.mWacthdogHandlerThread;
    }

    private void registerForegroundInfoListener() {
        try {
            NetworkBoostProcessMonitor networkBoostProcessMonitor = new NetworkBoostProcessMonitor(this.mContext);
            this.mNetworkBoostProcessMonitor = networkBoostProcessMonitor;
            networkBoostProcessMonitor.registerForegroundInfoListener(this.mForegroundInfoListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterForegroundInfoListener() {
        try {
            NetworkBoostProcessMonitor networkBoostProcessMonitor = new NetworkBoostProcessMonitor(this.mContext);
            this.mNetworkBoostProcessMonitor = networkBoostProcessMonitor;
            networkBoostProcessMonitor.unregisterForegroundInfoListener(this.mForegroundInfoListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    public int getForegroundUid() {
        int i;
        synchronized (this.mAppStatusListenerList) {
            i = this.mFUid;
        }
        return i;
    }

    private void registerUidObserver() {
        try {
            IActivityManager iActivityManager = ActivityManagerNative.getDefault();
            this.mActivityManager = iActivityManager;
            iActivityManager.registerUidObserver(this.mUidObserver, 2, -1, (String) null);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterUidObserver() {
        try {
            this.mActivityManager.unregisterUidObserver(this.mUidObserver);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void registerAudioModeChangedListener() {
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mAudioModeListener = new AudioManager.OnModeChangedListener() { // from class: com.xiaomi.NetworkBoost.StatusManager.3
            @Override // android.media.AudioManager.OnModeChangedListener
            public void onModeChanged(int mode) {
                Log.i(StatusManager.TAG, "audio Broadcast Receiver mode = " + mode);
                synchronized (StatusManager.this.mAppStatusListenerList) {
                    for (IAppStatusListener listener : StatusManager.this.mAppStatusListenerList) {
                        listener.onAudioChanged(mode);
                    }
                }
            }
        };
        try {
            this.mAudioManager.addOnModeChangedListener(Executors.newSingleThreadExecutor(), this.mAudioModeListener);
        } catch (Exception e) {
        }
    }

    private void unregisterAudioModeChangedListener() {
        try {
            this.mAudioManager.removeOnModeChangedListener(this.mAudioModeListener);
        } catch (Exception e) {
        }
    }

    public void registerAppStatusListener(IAppStatusListener listener) {
        synchronized (this.mAppStatusListenerList) {
            this.mAppStatusListenerList.add(listener);
        }
    }

    public void unregisterAppStatusListener(IAppStatusListener listener) {
        synchronized (this.mAppStatusListenerList) {
            this.mAppStatusListenerList.remove(listener);
        }
    }

    public void onNetworkPriorityChanged(int priorityMode, int trafficPolicy, int thermalLevel) {
        Log.i(TAG, "onNetworkPriorityChanged: " + priorityMode + " " + trafficPolicy + " " + thermalLevel);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(CALLBACK_NETWORK_PRIORITY_CHANGED, new NetworkPriorityInfo(priorityMode, trafficPolicy, thermalLevel)));
    }

    public void registerNetworkPriorityListener(INetworkPriorityListener listener) {
        synchronized (this.mNetworkPriorityListenerList) {
            this.mNetworkPriorityListenerList.add(listener);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(110, listener));
        }
    }

    public void unregisterNetworkPriorityListener(INetworkPriorityListener listener) {
        synchronized (this.mNetworkPriorityListenerList) {
            this.mNetworkPriorityListenerList.remove(listener);
        }
    }

    public NetworkPriorityInfo getNetworkPriorityInfo() {
        NetworkPriorityInfo networkPriorityInfo;
        synchronized (this.mNetworkPriorityListenerList) {
            networkPriorityInfo = new NetworkPriorityInfo(this.mPriorityMode, this.mTrafficPolicy, this.mThermalLevel);
        }
        return networkPriorityInfo;
    }

    /* loaded from: classes.dex */
    public static class NetworkPriorityInfo {
        public int priorityMode;
        public int thermalLevel;
        public int trafficPolicy;

        public NetworkPriorityInfo(int _priorityMode, int _trafficPolicy, int _thermalLevel) {
            this.priorityMode = _priorityMode;
            this.trafficPolicy = _trafficPolicy;
            this.thermalLevel = _thermalLevel;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNetworkPriorityChanged(NetworkPriorityInfo info) {
        synchronized (this.mNetworkPriorityListenerList) {
            Log.i(TAG, "CALLBACK_NETWORK_PRIORITY_CHANGED enter");
            if (this.mPriorityMode == info.priorityMode && this.mTrafficPolicy == info.trafficPolicy && this.mThermalLevel == info.thermalLevel) {
                return;
            }
            this.mPriorityMode = info.priorityMode;
            this.mTrafficPolicy = info.trafficPolicy;
            this.mThermalLevel = info.thermalLevel;
            for (INetworkPriorityListener listener : this.mNetworkPriorityListenerList) {
                listener.onNetworkPriorityChanged(this.mPriorityMode, this.mTrafficPolicy, this.mThermalLevel);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class NetworkCallbackInterface {
        private String interfaceName;
        private int status;

        public NetworkCallbackInterface(String interfaceName, int status) {
            this.interfaceName = interfaceName;
            this.status = status;
        }

        public String getInterfaceName() {
            return this.interfaceName;
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
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
            Log.i(StatusManager.TAG, "handle: " + msg.what);
            switch (msg.what) {
                case 100:
                    Network onAvailableNetwork = (Network) msg.obj;
                    StatusManager.this.interfaceOnAvailable(onAvailableNetwork);
                    return;
                case 101:
                    Network onLostNetwork = (Network) msg.obj;
                    StatusManager.this.interfaceOnLost(onLostNetwork);
                    return;
                case 102:
                    Network defaultNetwork = (Network) msg.obj;
                    StatusManager.this.mDefaultNetid = defaultNetwork.getNetId();
                    StatusManager.this.callbackDefaultNetworkInterface();
                    return;
                case 103:
                    Log.d(StatusManager.TAG, "receive EVENT_DELAY_SET_CELLULAR_CALLBACK");
                    StatusManager.this.processRegisterCellularCallback();
                    return;
                case 104:
                    StatusManager.this.processSimStateChanged((Intent) msg.obj);
                    return;
                case 105:
                    if (StatusManager.this.mPowerManager != null) {
                        boolean isScreenOn = StatusManager.this.mPowerManager.isScreenOn();
                        if (isScreenOn) {
                            StatusManager.this.isModemSingnalStrengthStatus();
                        }
                    }
                    StatusManager.this.mHandler.sendMessageDelayed(StatusManager.this.mHandler.obtainMessage(105), 30000L);
                    return;
                case 106:
                    if (StatusManager.this.mPowerManager != null) {
                        boolean isScreenOn2 = StatusManager.this.mPowerManager.isScreenOn();
                        if (isScreenOn2 && StatusManager.this.mIfaceNumber > 0) {
                            StatusManager.this.registerMovementSensorListener();
                            return;
                        } else {
                            StatusManager.this.unregisterMovementSensorListener();
                            return;
                        }
                    }
                    return;
                case 107:
                    synchronized (StatusManager.this.mNetworkInterfaceListenerList) {
                        Log.i(StatusManager.TAG, "CALLBACK_NETWORK_INTERFACE enter");
                        INetworkInterfaceListener networkListener = (INetworkInterfaceListener) msg.obj;
                        networkListener.onNetwrokInterfaceChange(StatusManager.this.mInterface, StatusManager.this.mIfaceNumber, StatusManager.this.mWifiReady, StatusManager.this.mSlaveWifiReady, StatusManager.this.mDataReady, StatusManager.this.mSlaveDataReady, StatusManager.ON_AVAILABLE);
                    }
                    return;
                case StatusManager.CALLBACK_SCREEN_STATUS /* 108 */:
                    synchronized (StatusManager.this.mScreenStatusListenerList) {
                        Log.i(StatusManager.TAG, "CALLBACK_SCREEN_STATUS enter");
                        IScreenStatusListener screenListener = (IScreenStatusListener) msg.obj;
                        if (StatusManager.this.mPowerManager != null) {
                            boolean isScreenOn3 = StatusManager.this.mPowerManager.isScreenOn();
                            if (isScreenOn3) {
                                screenListener.onScreenON();
                            } else {
                                screenListener.onScreenOFF();
                            }
                        }
                    }
                    return;
                case StatusManager.CALLBACK_NETWORK_PRIORITY_CHANGED /* 109 */:
                    StatusManager.this.onNetworkPriorityChanged((NetworkPriorityInfo) msg.obj);
                    return;
                case 110:
                    synchronized (StatusManager.this.mNetworkPriorityListenerList) {
                        Log.i(StatusManager.TAG, "CALLBACK_NETWORK_PRIORITY_ADD enter");
                        INetworkPriorityListener networkListener2 = (INetworkPriorityListener) msg.obj;
                        networkListener2.onNetworkPriorityChanged(StatusManager.this.mPriorityMode, StatusManager.this.mTrafficPolicy, StatusManager.this.mThermalLevel);
                    }
                    return;
                case StatusManager.CALLBACK_DEFAULT_NETWORK_INTERFACE /* 111 */:
                    synchronized (StatusManager.this.mDefaultNetworkInterfaceListenerList) {
                        Log.i(StatusManager.TAG, "CALLBACK_NETWORK_PRIORITY_ADD enter");
                        IDefaultNetworkInterfaceListener listener = (IDefaultNetworkInterfaceListener) msg.obj;
                        listener.onDefaultNetwrokChange(StatusManager.this.mDefaultIface, StatusManager.this.mDefaultIfaceStatus);
                    }
                    return;
                case StatusManager.CALLBACK_VPN_STATUS /* 112 */:
                    synchronized (StatusManager.this.mVPNListener) {
                        Log.i(StatusManager.TAG, "CALLBACK_VPN_STATUS enter");
                        IVPNListener listener2 = (IVPNListener) msg.obj;
                        listener2.onVPNStatusChange(StatusManager.this.mVPNstatus);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private void registerNetworkCallback() {
        HandlerThread handlerThread = new HandlerThread("NetworkInterfaceHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        mSlaveWifiManager = (SlaveWifiManager) this.mContext.getSystemService("SlaveWifiService");
        if (isDualDataSupported()) {
            Log.d(TAG, "8550 platform version");
            if (!this.mHandler.hasMessages(103)) {
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(handler.obtainMessage(103), 10000L);
            }
        } else {
            Log.d(TAG, "others platform version");
            NetworkRequest networkRequestCellular = new NetworkRequest.Builder().addTransportType(0).removeCapability(4).addCapability(16).addCapability(12).build();
            mConnectivityManager.registerNetworkCallback(networkRequestCellular, this.mCellularNetworkCallback);
        }
        WifiNetworkSpecifier.Builder specifierBuilderWifi6G = new WifiNetworkSpecifier.Builder();
        specifierBuilderWifi6G.setBand(8);
        NetworkRequest networkRequestWifi6G = new NetworkRequest.Builder().addTransportType(1).addCapability(12).addCapability(16).removeCapability(17).removeCapability(24).setNetworkSpecifier(specifierBuilderWifi6G.build()).build();
        mConnectivityManager.registerNetworkCallback(networkRequestWifi6G, this.mWifiNetworkCallback);
        WifiNetworkSpecifier.Builder specifierBuilderWifi5G = new WifiNetworkSpecifier.Builder();
        specifierBuilderWifi5G.setBand(2);
        NetworkRequest networkRequestWifi5G = new NetworkRequest.Builder().addTransportType(1).addCapability(12).addCapability(16).removeCapability(17).removeCapability(24).setNetworkSpecifier(specifierBuilderWifi5G.build()).build();
        mConnectivityManager.registerNetworkCallback(networkRequestWifi5G, this.mWifiNetworkCallback);
        if (mSlaveWifiManager != null) {
            WifiNetworkSpecifier.Builder specifierBuilderWifiHbs5G = new WifiNetworkSpecifier.Builder();
            specifierBuilderWifiHbs5G.setBand(WIFI_BAND_5_GHZ_HBS);
            NetworkRequest networkRequestWifiHbs5G = new NetworkRequest.Builder().addTransportType(1).addCapability(12).addCapability(16).removeCapability(17).removeCapability(24).setNetworkSpecifier(specifierBuilderWifiHbs5G.build()).build();
            mConnectivityManager.registerNetworkCallback(networkRequestWifiHbs5G, this.mWifiNetworkCallback);
        }
        WifiNetworkSpecifier.Builder specifierBuilderWifi24G = new WifiNetworkSpecifier.Builder();
        specifierBuilderWifi24G.setBand(1);
        NetworkRequest networkRequestWifi24G = new NetworkRequest.Builder().addTransportType(1).addCapability(12).addCapability(16).removeCapability(17).removeCapability(24).setNetworkSpecifier(specifierBuilderWifi24G.build()).build();
        mConnectivityManager.registerNetworkCallback(networkRequestWifi24G, this.mWifiNetworkCallback);
    }

    private void unregisterNetworkCallback() {
        this.mHandlerThread.quit();
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager == null) {
            return;
        }
        try {
            ConnectivityManager.NetworkCallback networkCallback = this.mWifiNetworkCallback;
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "unregisterWifiNetworkCallback");
            }
            ConnectivityManager.NetworkCallback networkCallback2 = this.mCellularNetworkCallback;
            if (networkCallback2 != null) {
                mConnectivityManager.unregisterNetworkCallback(networkCallback2);
                Log.d(TAG, "unregisterCellularNetworkCallback");
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Unregister network callback exception" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interfaceOnAvailable(Network network) {
        NetworkCapabilities networkCapabilities;
        Network network2 = network;
        if (network2 == null) {
            Log.e(TAG, "interfaceOnAvailable network is null.");
            return;
        }
        LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network2);
        NetworkCapabilities networkCapabilities2 = mConnectivityManager.getNetworkCapabilities(network2);
        NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network2);
        if (linkProperties == null || networkCapabilities2 == null) {
            return;
        }
        if (networkInfo == null) {
            return;
        }
        for (InetAddress inetAddress : linkProperties.getAddresses()) {
            if (!(inetAddress instanceof Inet4Address)) {
                networkCapabilities = networkCapabilities2;
            } else {
                Log.i(TAG, "onAvailable interface name:" + linkProperties.getInterfaceName());
                if (this.mInterface.indexOf(linkProperties.getInterfaceName()) != -1) {
                    networkCapabilities = networkCapabilities2;
                } else {
                    this.mInterface += linkProperties.getInterfaceName() + ",";
                    this.mIfaceNumber++;
                    if ("WIFI".equals(networkInfo.getTypeName())) {
                        Network currentNetwork = mWifiManager.getCurrentNetwork();
                        if (currentNetwork != null && currentNetwork.equals(network2)) {
                            Log.i(TAG, "onAvailable WIFI master.");
                            this.mWifiReady = true;
                            NetworkCallbackInterface intf = new NetworkCallbackInterface(linkProperties.getInterfaceName(), 1);
                            this.mNetworkMap.put(Integer.valueOf(network.getNetId()), intf);
                        } else {
                            Log.i(TAG, "onAvailable WIFI slave.");
                            this.mSlaveWifiReady = true;
                            NetworkCallbackInterface intf2 = new NetworkCallbackInterface(linkProperties.getInterfaceName(), 2);
                            this.mNetworkMap.put(Integer.valueOf(network.getNetId()), intf2);
                        }
                        networkCapabilities = networkCapabilities2;
                    } else if (!"MOBILE".equals(networkInfo.getTypeName())) {
                        networkCapabilities = networkCapabilities2;
                    } else {
                        int defaultDataSlotId = getDefaultDataSlotId();
                        int DataSubId = -1;
                        if (isValidatePhoneId(defaultDataSlotId) && isSlotActivated(defaultDataSlotId)) {
                            DataSubId = SubscriptionManager.getDefault().getSubscriptionIdForSlot(defaultDataSlotId);
                        }
                        int viceDataSlotId = getViceDataSlotId();
                        int SlaveDataSubId = -1;
                        if (isValidatePhoneId(viceDataSlotId) && isSlotActivated(viceDataSlotId)) {
                            SlaveDataSubId = SubscriptionManager.getDefault().getSubscriptionIdForSlot(viceDataSlotId);
                        }
                        Set<Integer> ids = networkCapabilities2.getSubscriptionIds();
                        int subId = ((Integer) ids.toArray()[0]).intValue();
                        if (subId == DataSubId) {
                            Log.i(TAG, "onAvailable MOBILE master.");
                            for (NetworkCallbackInterface intf3 : this.mNetworkMap.values()) {
                                NetworkCapabilities networkCapabilities3 = networkCapabilities2;
                                if (intf3.getStatus() == 3) {
                                    intf3.setStatus(4);
                                    this.mSlaveDataReady = true;
                                    this.mDataReady = false;
                                }
                                networkCapabilities2 = networkCapabilities3;
                            }
                            networkCapabilities = networkCapabilities2;
                            this.mDataReady = true;
                            NetworkCallbackInterface intf4 = new NetworkCallbackInterface(linkProperties.getInterfaceName(), 3);
                            this.mNetworkMap.put(Integer.valueOf(network.getNetId()), intf4);
                        } else {
                            networkCapabilities = networkCapabilities2;
                            if (subId == SlaveDataSubId) {
                                Log.i(TAG, "onAvailable MOBILE slave.");
                                this.mSlaveDataReady = true;
                                NetworkCallbackInterface intf5 = new NetworkCallbackInterface(linkProperties.getInterfaceName(), 4);
                                this.mNetworkMap.put(Integer.valueOf(network.getNetId()), intf5);
                            }
                        }
                    }
                }
            }
            network2 = network;
            networkCapabilities2 = networkCapabilities;
        }
        callbackNetworkInterface();
        callbackDefaultNetworkInterface();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interfaceOnLost(Network network) {
        if (network == null) {
            Log.e(TAG, "interfaceOnLost network is null.");
            return;
        }
        Log.i(TAG, "onLost interface Network:" + network);
        if (this.mNetworkMap.containsKey(Integer.valueOf(network.getNetId())) && this.mInterface.indexOf(this.mNetworkMap.get(Integer.valueOf(network.getNetId())).getInterfaceName()) != -1) {
            this.mInterface = this.mInterface.replaceAll(this.mNetworkMap.get(Integer.valueOf(network.getNetId())).getInterfaceName() + ",", "");
            this.mIfaceNumber--;
            switch (this.mNetworkMap.get(Integer.valueOf(network.getNetId())).getStatus()) {
                case 1:
                    this.mWifiReady = false;
                    break;
                case 2:
                    this.mSlaveWifiReady = false;
                    break;
                case 3:
                    this.mDataReady = false;
                    for (NetworkCallbackInterface intf : this.mNetworkMap.values()) {
                        if (intf.getStatus() == 4) {
                            intf.setStatus(3);
                            this.mSlaveDataReady = false;
                            this.mDataReady = true;
                        }
                    }
                    break;
                case 4:
                    this.mSlaveDataReady = false;
                    break;
            }
            this.mNetworkMap.remove(Integer.valueOf(network.getNetId()));
        }
        callbackNetworkInterface();
    }

    private void callbackNetworkInterface() {
        Log.i(TAG, "interface name:" + this.mInterface + " interface number:" + this.mIfaceNumber + " WifiReady:" + this.mWifiReady + " SlaveWifiReady:" + this.mSlaveWifiReady + " DataReady:" + this.mDataReady + " SlaveDataReady:" + this.mSlaveDataReady);
        synchronized (this.mNetworkInterfaceListenerList) {
            for (INetworkInterfaceListener listener : this.mNetworkInterfaceListenerList) {
                listener.onNetwrokInterfaceChange(this.mInterface, this.mIfaceNumber, this.mWifiReady, this.mSlaveWifiReady, this.mDataReady, this.mSlaveDataReady, ON_LOST);
            }
        }
        checkSensorStatus();
    }

    public List<String> getAvailableIfaces() {
        List<String> availableIfaces = new ArrayList<>();
        String[] interfaces = this.mInterface.split(",");
        for (String str : interfaces) {
            availableIfaces.add(str);
        }
        return availableIfaces;
    }

    public static boolean isTetheredIfaces(String iface) {
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager == null) {
            return false;
        }
        Method[] teMethods = connectivityManager.getClass().getDeclaredMethods();
        for (Method method : teMethods) {
            if (method.getName().equals("getTetheredIfaces")) {
                try {
                    String[] allTethered = (String[]) method.invoke(mConnectivityManager, new Object[0]);
                    if (allTethered != null) {
                        for (String tethered : allTethered) {
                            if (iface.equals(tethered)) {
                                Log.d(TAG, iface + " is TetheredIfaces ");
                                return true;
                            }
                        }
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception:" + e);
                    return false;
                }
            }
        }
        return false;
    }

    public void registerNetworkInterfaceListener(INetworkInterfaceListener listener) {
        synchronized (this.mNetworkInterfaceListenerList) {
            this.mNetworkInterfaceListenerList.add(listener);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(107, listener));
        }
    }

    public void unregisterNetworkInterfaceListener(INetworkInterfaceListener listener) {
        synchronized (this.mNetworkInterfaceListenerList) {
            this.mNetworkInterfaceListenerList.remove(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void callbackDefaultNetworkInterface() {
        int i = this.mDefaultNetid;
        if (i != 0 && this.mNetworkMap.containsKey(Integer.valueOf(i))) {
            NetworkCallbackInterface intf = this.mNetworkMap.get(Integer.valueOf(this.mDefaultNetid));
            this.mDefaultIface = intf.getInterfaceName();
            this.mDefaultIfaceStatus = intf.getStatus();
            Log.i(TAG, "callbackDefaultNetworkInterface ifacename:" + this.mDefaultIface + " ifacestatus:" + this.mDefaultIfaceStatus);
            synchronized (this.mDefaultNetworkInterfaceListenerList) {
                for (IDefaultNetworkInterfaceListener listener : this.mDefaultNetworkInterfaceListenerList) {
                    listener.onDefaultNetwrokChange(this.mDefaultIface, this.mDefaultIfaceStatus);
                }
            }
        }
    }

    private void registerDefaultNetworkCallback() {
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager == null) {
            return;
        }
        connectivityManager.registerDefaultNetworkCallback(this.mDefaultNetworkCallback);
    }

    private void unregisterDefaultNetworkCallback() {
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager == null) {
            return;
        }
        try {
            ConnectivityManager.NetworkCallback networkCallback = this.mDefaultNetworkCallback;
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "unregisterefaultNetworkCallback");
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Unregister default network callback exception" + e);
        }
    }

    public void registerDefaultNetworkInterfaceListener(IDefaultNetworkInterfaceListener listener) {
        synchronized (this.mDefaultNetworkInterfaceListenerList) {
            this.mDefaultNetworkInterfaceListenerList.add(listener);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(CALLBACK_DEFAULT_NETWORK_INTERFACE, listener));
        }
    }

    public void unregisterDefaultNetworkInterfaceListener(IDefaultNetworkInterfaceListener listener) {
        synchronized (this.mDefaultNetworkInterfaceListenerList) {
            this.mDefaultNetworkInterfaceListenerList.remove(listener);
        }
    }

    void registerVPNCallback() {
        if (mConnectivityManager == null) {
            return;
        }
        NetworkRequest vpnRequest = new NetworkRequest.Builder().addTransportType(4).removeCapability(15).removeCapability(12).build();
        mConnectivityManager.registerNetworkCallback(vpnRequest, this.mVPNCallback);
    }

    void unregisterVPNCallback() {
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager == null) {
            return;
        }
        connectivityManager.unregisterNetworkCallback(this.mVPNCallback);
    }

    public void registerVPNListener(IVPNListener listener) {
        synchronized (this.mVPNListener) {
            this.mVPNListener.add(listener);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(CALLBACK_VPN_STATUS, listener));
        }
    }

    public void unregisterVPNListener(IVPNListener listener) {
        synchronized (this.mVPNListener) {
            this.mVPNListener.remove(listener);
        }
    }

    private void registerScreenStatusReceiver() {
        this.mScreenStatusReceiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.StatusManager.8
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                synchronized (StatusManager.this.mScreenStatusListenerList) {
                    if ("android.intent.action.SCREEN_ON".equals(action)) {
                        Log.d(StatusManager.TAG, "ACTION_SCREEN_ON");
                        for (IScreenStatusListener listener : StatusManager.this.mScreenStatusListenerList) {
                            listener.onScreenON();
                        }
                    } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                        Log.d(StatusManager.TAG, "ACTION_SCREEN_OFF");
                        for (IScreenStatusListener listener2 : StatusManager.this.mScreenStatusListenerList) {
                            listener2.onScreenOFF();
                        }
                    }
                }
                StatusManager.this.checkSensorStatus();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mScreenStatusReceiver, filter, 4);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
    }

    private void unregisterScreenStatusReceiver() {
        BroadcastReceiver broadcastReceiver = this.mScreenStatusReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "unregisterScreenStatusReceiver");
        }
    }

    public void registerScreenStatusListener(IScreenStatusListener listener) {
        synchronized (this.mScreenStatusListenerList) {
            this.mScreenStatusListenerList.add(listener);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(CALLBACK_SCREEN_STATUS, listener));
        }
    }

    public void unregisterScreenStatusListener(IScreenStatusListener listener) {
        synchronized (this.mScreenStatusListenerList) {
            this.mScreenStatusListenerList.remove(listener);
        }
    }

    public void registerModemSignalStrengthListener(IModemSignalStrengthListener listener) {
        synchronized (this.mModemSignalStrengthListener) {
            this.mModemSignalStrengthListener.add(listener);
        }
    }

    public void unregisterModemSignalStrengthListener(IModemSignalStrengthListener listener) {
        synchronized (this.mModemSignalStrengthListener) {
            this.mModemSignalStrengthListener.remove(listener);
        }
    }

    private void registerModemSignalStrengthObserver() {
        this.mTelephonyManager = (android.telephony.TelephonyManager) this.mContext.getSystemService("phone");
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(105), 30000L);
    }

    private void unregisterModemSignalStrengthObserver() {
        this.mHandler.removeMessages(105);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void isModemSingnalStrengthStatus() {
        android.telephony.TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManager is null");
            return;
        }
        int isStatus = 0;
        try {
            SignalStrength signalStrength = telephonyManager.getSignalStrength();
            ServiceState serviceState = this.mTelephonyManager.getServiceState();
            if (signalStrength != null && serviceState != null) {
                int rat = serviceState.getDataNetworkType();
                if (!isLteNrNwType(rat)) {
                    return;
                }
                List<CellSignalStrength> cellSignalStrengths = signalStrength.getCellSignalStrengths();
                for (CellSignalStrength cellSignal : cellSignalStrengths) {
                    if (cellSignal instanceof CellSignalStrengthLte) {
                        CellSignalStrengthLte cellLteSignal = (CellSignalStrengthLte) cellSignal;
                        int cellLteSignalRsrp = inRangeOrUnavailable(cellLteSignal.getRsrp(), -140, LTE_RSRP_UPPER_LIMIT);
                        if (isPoorSignal(cellLteSignalRsrp)) {
                            Log.i(TAG, "isModemSingnalStrengthStatus cellLteSignalRsrp = " + cellLteSignalRsrp);
                            isStatus = 1;
                        }
                    } else if (cellSignal instanceof CellSignalStrengthNr) {
                        CellSignalStrengthNr cellNrSignal = (CellSignalStrengthNr) cellSignal;
                        int cellNrSignalRsrp = inRangeOrUnavailable(cellNrSignal.getSsRsrp(), -140, NR_RSRP_UPPER_LIMIT);
                        if (isPoorSignal(cellNrSignalRsrp)) {
                            Log.i(TAG, "isModemSingnalStrengthStatus cellNrSignalRsrp = " + cellNrSignalRsrp);
                            isStatus = 1;
                        }
                    }
                }
                synchronized (this.mModemSignalStrengthListener) {
                    for (IModemSignalStrengthListener listener : this.mModemSignalStrengthListener) {
                        listener.isModemSingnalStrengthStatus(isStatus);
                    }
                }
                return;
            }
            Log.d(TAG, "isModemSingnalStrengthStatus signalStrength or serviceState is null");
        } catch (Exception e) {
            Log.e(TAG, "isModemSingnalStrengthStatus found an error: " + e);
        }
    }

    private int inRangeOrUnavailable(int value, int rangeMin, int rangeMax) {
        if (value < rangeMin || value > rangeMax) {
            return UNAVAILABLE;
        }
        return value;
    }

    public boolean isPoorSignal(int curRsrp) {
        if (curRsrp == UNAVAILABLE || curRsrp > RSRP_POOR_THRESHOLD) {
            return false;
        }
        return true;
    }

    private boolean isLteNwType(int rat) {
        return rat == 13 || rat == 19;
    }

    private boolean isNrNwType(int rat) {
        return rat == 20 || rat == 30;
    }

    private boolean isLteNrNwType(int rat) {
        return isLteNwType(rat) || isNrNwType(rat);
    }

    private void registerTelephonyStatusReceiver() {
        if (isDualDataSupported()) {
            this.mTelephonyStatusReceiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.StatusManager.9
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                        Log.i(StatusManager.TAG, "receive sim state change");
                        StatusManager.this.mHandler.obtainMessage(104, intent).sendToTarget();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SIM_STATE_CHANGED");
            this.mContext.registerReceiver(this.mTelephonyStatusReceiver, filter);
        }
    }

    private void unregisterTelephonyStatusReceiver() {
        BroadcastReceiver broadcastReceiver = this.mTelephonyStatusReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "unregisterTelephonyStatusReceiver");
        }
    }

    private boolean isDualDataSupported() {
        boolean is_dual_data_support = this.mContext.getResources().getBoolean(285540414);
        Log.d(TAG, "isDualDataSupported = " + is_dual_data_support);
        return is_dual_data_support;
    }

    private int getDefaultDataSlotId() {
        return SubscriptionManager.getDefault().getDefaultDataSlotId();
    }

    private int getViceDataSlotId() {
        for (int slotId = 0; slotId < PHONE_COUNT; slotId++) {
            if (slotId != getDefaultDataSlotId()) {
                return slotId;
            }
        }
        return -1;
    }

    private boolean isSlotActivated(int slotId) {
        SubscriptionInfo subscriptionInfo = SubscriptionManager.getDefault().getSubscriptionInfoForSlot(slotId);
        if (subscriptionInfo != null) {
            return subscriptionInfo != null && subscriptionInfo.isActivated();
        }
        Log.d(TAG, "subscriptionInfo == null");
        return false;
    }

    private boolean isValidatePhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < PHONE_COUNT;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processRegisterCellularCallback() {
        ConnectivityManager.NetworkCallback networkCallback;
        ConnectivityManager connectivityManager = mConnectivityManager;
        if (connectivityManager != null && (networkCallback = this.mCellularNetworkCallback) != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Unregister network callback exception" + e);
            }
        }
        int defaultDataSlotId = getDefaultDataSlotId();
        int viceDataSlotId = getViceDataSlotId();
        Log.d(TAG, "defaultDataSlotId = " + defaultDataSlotId + ", viceDataSlotId = " + viceDataSlotId);
        if (isValidatePhoneId(defaultDataSlotId) && isSlotActivated(defaultDataSlotId) && mConnectivityManager != null) {
            int DataSubId = SubscriptionManager.getDefault().getSubscriptionIdForSlot(defaultDataSlotId);
            Log.d(TAG, "register default slot callback defaultslot = " + defaultDataSlotId + ", subId = " + DataSubId);
            NetworkRequest networkRequestCellular_0 = new NetworkRequest.Builder().addTransportType(0).addCapability(16).addCapability(12).removeCapability(4).setNetworkSpecifier(String.valueOf(SubscriptionManager.getDefault().getSubscriptionIdForSlot(defaultDataSlotId))).build();
            mConnectivityManager.registerNetworkCallback(networkRequestCellular_0, this.mCellularNetworkCallback);
        }
        if (isValidatePhoneId(viceDataSlotId) && isSlotActivated(viceDataSlotId) && mConnectivityManager != null) {
            int SlaveDataSubId = SubscriptionManager.getDefault().getSubscriptionIdForSlot(viceDataSlotId);
            Log.d(TAG, "register vice slot callback viceslot = " + viceDataSlotId + ", subId = " + SlaveDataSubId);
            NetworkRequest networkRequestCellular_1 = new NetworkRequest.Builder().addTransportType(0).addCapability(16).addCapability(12).removeCapability(4).setNetworkSpecifier(String.valueOf(SubscriptionManager.getDefault().getSubscriptionIdForSlot(viceDataSlotId))).build();
            mConnectivityManager.registerNetworkCallback(networkRequestCellular_1, this.mCellularNetworkCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processSimStateChanged(Intent intent) {
        getDefaultDataSlotId();
        getViceDataSlotId();
        if (!this.mHandler.hasMessages(103)) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(103), 10000L);
        }
    }

    private boolean isMobileDataSettingOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), getMobileDataSettingName(), 0) != 0;
    }

    private String getMobileDataSettingName() {
        return PHONE_COUNT > 1 ? "mobile_data0" : "mobile_data";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSmartDNSInterface(Network network) {
        CdmaCellLocation cdmaCellLocation;
        WifiInfo wifiInfo = null;
        if (network == null) {
            Log.e(TAG, "SmartDNS onAvailable parameter is null.");
            return;
        }
        NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
        if (networkInfo == null) {
            Log.e(TAG, "SmartDNS onAvailable getNetworkInfo error.");
            return;
        }
        NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(network);
        if (networkCapabilities != null && networkCapabilities.hasTransport(4)) {
            Log.i(TAG, "SmartDNS " + networkInfo.getTypeName() + " onAvailable vpn.");
            return;
        }
        if (networkInfo.getTypeName().equals("WIFI")) {
            Network currentNetwork = mWifiManager.getCurrentNetwork();
            if (currentNetwork == null || !currentNetwork.equals(network)) {
                Log.i(TAG, "SmartDNS onAvailable WIFI slave");
                SlaveWifiManager slaveWifiManager = mSlaveWifiManager;
                if (slaveWifiManager != null) {
                    wifiInfo = slaveWifiManager.getWifiSlaveConnectionInfo();
                }
            } else {
                Log.i(TAG, "SmartDNS onAvailable WIFI master.");
                wifiInfo = mWifiManager.getConnectionInfo();
            }
            if (wifiInfo != null && wifiInfo.getBSSID() != null) {
                Log.i(TAG, "SmartDNS push DnsReslover a relationship, netid " + network.getNetId() + " name " + wifiInfo.getSSID());
                setResloverInterfaceIdAndNetid(network.getNetId(), wifiInfo.getSSID());
                return;
            }
            return;
        }
        android.telephony.TelephonyManager mTelephonyManager = (android.telephony.TelephonyManager) this.mContext.getSystemService("phone");
        CellLocation location = mTelephonyManager.getCellLocation();
        if (location instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
            if (gsmCellLocation != null) {
                long lac = gsmCellLocation.getLac();
                gsmCellLocation.getCid();
                if (lac > 0) {
                    setResloverInterfaceIdAndNetid(network.getNetId(), Long.toString(lac));
                    Log.i(TAG, "SmartDNS \t LAC = " + lac);
                    return;
                }
                return;
            }
            return;
        }
        if ((location instanceof CdmaCellLocation) && (cdmaCellLocation = (CdmaCellLocation) location) != null) {
            long lac2 = cdmaCellLocation.getNetworkId();
            cdmaCellLocation.getBaseStationId();
            if (lac2 > 0) {
                setResloverInterfaceIdAndNetid(network.getNetId(), Long.toString(lac2));
                Log.i(TAG, "SmartDNS \t LAC = " + lac2);
            }
        }
    }

    private void setResloverInterfaceIdAndNetid(int netId, String interfaceId) {
        try {
            Class<?> ConnectivityManagerClass = Class.forName(mConnectivityManager.getClass().getName());
            Method setResloverInterfaceIdAndNetidMethod = ConnectivityManagerClass.getDeclaredMethod("setResloverInterfaceIdAndNetid", Integer.TYPE, String.class);
            setResloverInterfaceIdAndNetidMethod.setAccessible(true);
            setResloverInterfaceIdAndNetidMethod.invoke(mConnectivityManager, Integer.valueOf(netId), interfaceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerMovementSensorListener() {
        if (this.mSensorFlag) {
            Log.e(TAG, "other registerMovementSensorListener succeed!");
            return;
        }
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }
        Sensor movementSensor = this.mSensorManager.getDefaultSensor(MOVEMENT_SENSOR_TYPE, false);
        if (movementSensor == null) {
            Log.e(TAG, "registerMovementSensorListener is fail!");
        } else {
            this.mSensorFlag = this.mSensorManager.registerListener(this.mSensorListener, movementSensor, 3);
            Log.i(TAG, "registerMovementSensorListener sensorFlag:" + this.mSensorFlag);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterMovementSensorListener() {
        if (!this.mSensorFlag) {
            Log.e(TAG, "other unregisterMovementSensorListener succeed!");
            return;
        }
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            Log.e(TAG, "unregisterMovementSensorListener is fail!");
            return;
        }
        sensorManager.unregisterListener(this.mSensorListener);
        this.mSensorFlag = false;
        Log.i(TAG, "unregisterMovementSensorListener");
        Sensor movementSensor = this.mSensorManager.getDefaultSensor(MOVEMENT_SENSOR_TYPE, false);
        SensorEvent event = new SensorEvent(movementSensor, 0, 0L, new float[]{MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X});
        synchronized (this.mMovementSensorStatusListenerList) {
            for (IMovementSensorStatusListener listener : this.mMovementSensorStatusListenerList) {
                listener.onSensorChanged(event);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkSensorStatus() {
        this.mHandler.removeMessages(106);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(106), 5000L);
    }

    public void registerMovementSensorStatusListener(IMovementSensorStatusListener listener) {
        synchronized (this.mMovementSensorStatusListenerList) {
            this.mMovementSensorStatusListenerList.add(listener);
            SensorEvent sensorEvent = this.mCurrentSensorevent;
            if (sensorEvent != null) {
                listener.onSensorChanged(sensorEvent);
            }
        }
    }

    public void unregisterMovementSensorStatusListener(IMovementSensorStatusListener listener) {
        synchronized (this.mMovementSensorStatusListenerList) {
            this.mMovementSensorStatusListenerList.remove(listener);
        }
    }

    public void dumpModule(PrintWriter writer) {
        try {
            writer.println("NetworkBoostStatusManager begin:");
            writer.println("    WifiReady:" + this.mWifiReady);
            writer.println("    SlaveWifiReady:" + this.mSlaveWifiReady);
            writer.println("    DataReady:" + this.mDataReady);
            writer.println("    SlaveDataReady:" + this.mSlaveDataReady);
            writer.println("    mInterface:" + this.mInterface);
            writer.println("    mIfaceNumber:" + this.mIfaceNumber);
            writer.println("    mPriorityMode:" + this.mPriorityMode);
            writer.println("    mTrafficPolicy:" + this.mTrafficPolicy);
            writer.println("    mThermalLevel:" + this.mThermalLevel);
            writer.println("    mAppStatusListenerList:" + this.mAppStatusListenerList.size());
            writer.println("    mNetworkInterfaceListenerList:" + this.mNetworkInterfaceListenerList.size());
            writer.println("    mScreenStatusListenerList:" + this.mScreenStatusListenerList.size());
            writer.println("    mNetworkPriorityListenerList:" + this.mNetworkPriorityListenerList.size());
            writer.println("NetworkBoostStatusManager end.\n");
        } catch (Exception ex) {
            Log.e(TAG, "dump failed!", ex);
            try {
                writer.println("Dump of NetworkBoostStatusManager failed:" + ex);
                writer.println("NetworkBoostStatusManager end.\n");
            } catch (Exception e) {
                Log.e(TAG, "dump failure failed:" + e);
            }
        }
    }
}
