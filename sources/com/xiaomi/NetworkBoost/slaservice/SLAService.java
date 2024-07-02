package com.xiaomi.NetworkBoost.slaservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.DhcpInfo;
import android.net.MacAddress;
import android.net.NetworkUtils;
import android.net.wifi.SlaveWifiManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.xiaomi.NetworkBoost.StatusManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import vendor.qti.sla.service.V1_0.ISlaService;

/* loaded from: classes.dex */
public class SLAService {
    public static final String ACTION_START_SERVICE = "ACTION_SLA_JAVA_SERVICE_START";
    private static final String CHANNEL_ID = "notification_channel";
    private static final String CLOUD_DOUBLE_WIFI_MONITOR = "cloud_double_wifi_monitor_controll";
    private static final String CLOUD_MIWILL_CONTROLLER_LIST = "cloud_miwill_controller_list";
    private static final String CLOUD_MIWILL_DUAL_WIFI_OFF = "miwill_dual_wifi_off";
    private static final String CLOUD_MIWILL_DUAL_WIFI_ON = "miwill_dual_wifi_on";
    private static final String CLOUD_MIWILL_OFF = "miwill_off";
    private static final String CLOUD_MIWILL_ON = "miwill_on";
    private static final String COEXISTENSE = "coexistense";
    private static final int EVENT_DISABLE_SLM = 105;
    private static final int EVENT_ENABLE_SLM = 104;
    private static final int EVENT_GET_HAL = 103;
    private static final int EVENT_GET_THERMAL = 110;
    private static final int EVENT_SCREEN_OFF = 107;
    private static final int EVENT_SCREEN_ON = 106;
    private static final int EVENT_TRAFFIC_CHECK = 108;
    private static final int EVENT_UPDATE_SLAD_STATE = 100;
    private static final int EVENT_WATCH_DOG_TIMEOUT = 100;
    private static final int GET_SERVICE_DELAY_MILLIS = 4000;
    private static final int GET_THERMAL_DELAY_MILIS = 10000;
    private static final String NOTIFACATION_RECEIVER_PACKAGE = "com.android.phone";
    private static final int NOTIFICATION_ID_1 = 1;
    private static final int NOTIFICATION_ID_2 = 2;
    private static final String ON_AVAILABLE = "ON_AVAILABLE";
    private static final String ON_DISABLE_EVENT = "ON_DISABLE_EVENT";
    private static final String ON_LOST = "ON_LOST";
    private static final String ON_USER = "ON_USER";
    private static final String PROP_MIWILL_DUAL_WIFI_ENABLE = "persist.log.tag.miwill_dual_wifi_enable";
    public static final int REMIND_OFF = 0;
    public static final int REMIND_ON = 1;
    private static final int SCREEN_OFF_DELAY_MIILLIS = 180000;
    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;
    private static final int SIGNAL_STRENGTH_GOOD = 0;
    private static final int SIGNAL_STRENGTH_POOR = 1;
    private static final String SLAD_STOP = "0";
    private static final String SLA_NOTIFICATION_STATE = "sla_notification_state";
    private static final String SLM_FEATURE_STATUS = "com.android.phone.intent.action.SLM_STATUS";
    private static final String TAG = "SLM-SRV-SLAService";
    private static final int THERMAL_CNT = 6;
    private static final long THERMAL_DISABLE = 45500;
    private static final long THERMAL_ENABLE = 41000;
    private static final String THERMAL_MODE_FILE = "/sys/class/thermal/thermal_message/balance_mode";
    private static final int THERMAL_PERFORMANCE_MODE = 1;
    private static final String THERMAL_STATE_FILE = "/sys/class/thermal/thermal_message/board_sensor_temp";
    private static final int TRAFFIC_LIMIT = 300;
    private static final long TRAFFIC_TIMER = 10000;
    private static final int WATCH_DOG_TIMEOUT = 120000;
    private static Context mContext;
    private static SLAAppLib mSLAAppLib;
    private static SLATrack mSLATrack;
    private static SLSAppLib mSLSAppLib;
    private static ISlaService mSlaService;
    private static SLAToast mSlaToast;
    private static volatile SLAService sInstance;
    private BroadcastReceiver mConnectivityChangedReceiver;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Handler mWacthdogHandler;
    private HandlerThread mWacthdogHandlerThread;
    private static boolean sladEnabled = false;
    public static final Object mSlaLock = new Object();
    private static boolean mLinkTurboSwitch = false;
    private static final Semaphore mSemaphore = new Semaphore(1);
    private static boolean mWifiReady = false;
    private static boolean mSlaveWifiReady = false;
    private static boolean mDataReady = false;
    private static boolean mSlaveDataReady = false;
    private static String mInterface = "";
    private static int mIfaceNumber = 0;
    private final String LINK_TURBO_ENABLE_PREF = "linkturbo_is_enable";
    private List<SLAApp> mSLAApp = new ArrayList();
    private MiWillManager mMiWillManager = null;
    private ContentObserver mCloudObserver = null;
    private int disableSLMCnt = 0;
    private int mlastWlanWwanCoexistanceStatus = -1;
    private StatusManager mStatusManager = null;
    private WifiManager mWifiManager = null;
    private SlaveWifiManager mSlaveWifiManager = null;
    public NotificationManager mNotificationManager = null;
    private boolean misEnableDWMonitor = false;
    private IHwBinder.DeathRecipient mDeathRecipient = new SlaServiceDeathRecipient();
    private StatusManager.INetworkInterfaceListener mNetworkInterfaceListener = new StatusManager.INetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.2
        @Override // com.xiaomi.NetworkBoost.StatusManager.INetworkInterfaceListener
        public void onNetwrokInterfaceChange(String ifacename, int ifacenum, boolean wifiready, boolean slavewifiready, boolean dataready, boolean slaveDataReady, String status) {
            SLAService.mInterface = ifacename;
            SLAService.mIfaceNumber = ifacenum;
            SLAService.mWifiReady = wifiready;
            SLAService.mSlaveWifiReady = slavewifiready;
            SLAService.mDataReady = dataready;
            SLAService.mSlaveDataReady = slaveDataReady;
            Log.i(SLAService.TAG, "interface name:" + SLAService.mInterface + " interface number:" + SLAService.mIfaceNumber + " WifiReady:" + SLAService.mWifiReady + " SlaveWifiReady:" + SLAService.mSlaveWifiReady + " DataReady:" + SLAService.mDataReady + " SlaveDataReady:" + SLAService.mSlaveDataReady);
            if (SLAService.this.mMiWillManager != null) {
                SLAService.this.mMiWillManager.setDualWifiReady(SLAService.mWifiReady && SLAService.mSlaveWifiReady);
            }
            SLAService.this.processNetworkCallback(status);
        }
    };
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.STATE_CHANGE".equals(action) && SLAService.mWifiReady && SLAService.mSlaveWifiReady && SLAService.this.mMiWillManager != null) {
                Log.i(SLAService.TAG, "Dual wifi roaming,check miwill status");
                SLAService.this.mMiWillManager.setDualWifiReady(SLAService.mWifiReady && SLAService.mSlaveWifiReady);
            }
        }
    };
    private StatusManager.IDefaultNetworkInterfaceListener mDefaultNetworkInterfaceListener = new StatusManager.IDefaultNetworkInterfaceListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.4
        @Override // com.xiaomi.NetworkBoost.StatusManager.IDefaultNetworkInterfaceListener
        public void onDefaultNetwrokChange(String ifacename, int ifacestatus) {
            boolean z = false;
            if (SLAService.mWifiReady && ifacestatus != 1) {
                SLAService.mWifiReady = false;
                SLAService.mInterface = SLAService.mInterface.replaceAll("wlan0,", "");
                SLAService.mIfaceNumber--;
            } else if (!SLAService.mWifiReady && ifacestatus == 1) {
                SLAService.mWifiReady = true;
                SLAService.mInterface += ifacename + ",";
                SLAService.mIfaceNumber++;
            } else {
                return;
            }
            Log.i(SLAService.TAG, "Default Netwrok Change mInterface:" + SLAService.mInterface + " mWifiReady:" + SLAService.mWifiReady);
            if (SLAService.this.mMiWillManager != null) {
                MiWillManager miWillManager = SLAService.this.mMiWillManager;
                if (SLAService.mWifiReady && SLAService.mSlaveWifiReady) {
                    z = true;
                }
                miWillManager.setDualWifiReady(z);
            }
            SLAService.this.processNetworkCallback("Default Netwrok Change");
        }
    };
    private StatusManager.IVPNListener mVPNListener = new StatusManager.IVPNListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.5
        @Override // com.xiaomi.NetworkBoost.StatusManager.IVPNListener
        public void onVPNStatusChange(boolean vpnEnable) {
            if (vpnEnable) {
                Log.d(SLAService.TAG, "VPN CONNECTED");
                SLAService.this.disableSLM();
            } else {
                Log.d(SLAService.TAG, "VPN DISCONNECTED");
                SLAService.this.enableSLM();
            }
        }
    };
    private StatusManager.IScreenStatusListener mScreenStatusListener = new StatusManager.IScreenStatusListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.6
        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenON() {
            SLAService.this.handleScreenOn();
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IScreenStatusListener
        public void onScreenOFF() {
            SLAService.this.handleScreenOff();
        }
    };
    private boolean thermal_enable_slm = true;
    private ArrayList<Long> mThermalList = new ArrayList<>();
    private boolean mSingnalPoor = false;
    private StatusManager.IModemSignalStrengthListener mModemSignalStrengthListener = new StatusManager.IModemSignalStrengthListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.7
        @Override // com.xiaomi.NetworkBoost.StatusManager.IModemSignalStrengthListener
        public void isModemSingnalStrengthStatus(int status) {
            Log.d(SLAService.TAG, "isModemSingnalStrengthStatus = " + status);
            if (status == 1 && !SLAService.this.mSingnalPoor) {
                Log.i(SLAService.TAG, "isModemSingnalStrengthStatus setMobileDataAlwaysOff");
                SLAService.this.mSingnalPoor = true;
                SLAService.this.setMobileDataAlwaysOff();
            } else if (status == 0 && SLAService.mLinkTurboSwitch && SLAService.this.disableSLMCnt == 0 && !SLAService.mDataReady && SLAService.this.mSingnalPoor) {
                Log.i(SLAService.TAG, "isModemSingnalStrengthStatus setMobileDataAlwaysOn");
                SLAService.this.mSingnalPoor = false;
                SLAService.this.setMobileDataAlwaysOn();
            }
        }
    };

    public boolean addUidToLinkTurboWhiteList(String uid) {
        return mSLAAppLib.sendMsgAddSLAUid(uid);
    }

    public boolean removeUidInLinkTurboWhiteList(String uid) {
        return mSLAAppLib.sendMsgDelSLAUid(uid);
    }

    public String getLinkTurboWhiteList() {
        return mSLAAppLib.getLinkTurboWhiteList();
    }

    public List<String> getLinkTurboDefaultPn() {
        return mSLAAppLib.getLinkTurboDefaultPn();
    }

    public List<SLAApp> getLinkTurboAppsTraffic() {
        return mSLAAppLib.getLinkTurboAppsTraffic();
    }

    public boolean setLinkTurboEnable(boolean z) {
        if (mLinkTurboSwitch == z) {
            return true;
        }
        if (z) {
            setMobileDataAlwaysOn();
        } else {
            setMobileDataAlwaysOff();
        }
        Log.i(TAG, "SLM Enable:" + z);
        if (!z) {
            this.mHandler.removeMessages(108);
        } else {
            Settings.System.putInt(mContext.getContentResolver(), SLA_NOTIFICATION_STATE, 1);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(108), 10000L);
        }
        mLinkTurboSwitch = z;
        Settings.System.putInt(mContext.getContentResolver(), "linkturbo_is_enable", z ? 1 : 0);
        Message obtain = Message.obtain();
        obtain.what = 100;
        this.mHandler.sendMessage(obtain);
        return true;
    }

    public static SLAService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SLAService.class) {
                if (sInstance == null) {
                    sInstance = new SLAService(context);
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
            synchronized (MiWillManager.class) {
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

    private SLAService(Context context) {
        mContext = context.getApplicationContext();
    }

    public void onCreate() {
        Log.i(TAG, "onCreate");
        HandlerThread handlerThread = new HandlerThread("SLAServiceHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        InternalHandler internalHandler = new InternalHandler(this.mHandlerThread.getLooper());
        this.mHandler = internalHandler;
        internalHandler.sendMessage(internalHandler.obtainMessage(103, 0));
        registerNetworkBoostWatchdog();
        mSlaToast = new SLAToast(mContext);
        mSLAAppLib = SLAAppLib.get(mContext);
        mSLSAppLib = new SLSAppLib(mContext, this);
        mSLATrack = SLATrack.getSLATrack(mContext);
        this.mWifiManager = (WifiManager) mContext.getSystemService("wifi");
        this.mSlaveWifiManager = (SlaveWifiManager) mContext.getSystemService("SlaveWifiService");
        initCloudObserver();
        if (checkMiwillCloudController()) {
            this.mMiWillManager = MiWillManager.getInstance(mContext);
        }
        isEnableDWMonitor();
        createSLANotificationChannel();
        registerVPNChangedCallback();
        registerScreenStatusListener();
        registerModemSignalStrengthListener();
        mLinkTurboSwitch = Settings.System.getInt(mContext.getContentResolver(), "linkturbo_is_enable", 0) == 1;
        registerNetworkCallback();
        registerDefaultNetworkCallback();
        registerNetworkStateBroadcastReceiver();
        this.mHandler.sendEmptyMessage(110);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        ISlaService iSlaService = mSlaService;
        if (iSlaService != null) {
            try {
                iSlaService.unlinkToDeath(this.mDeathRecipient);
            } catch (Exception e) {
            }
        }
        this.mHandlerThread.quit();
        mSlaToast.getSLAToastHandlerThread().quit();
        unregisterVPNChangedCallback();
        unregisterScreenStatusListener();
        unregisterModemSignalStrengthListener();
        unregisterNetworkCallback();
        unregisterDefaultNetworkCallback();
        unregisterNetworkStateBroadcastReceiver();
        deinitCloudObserver();
        if (this.mMiWillManager != null) {
            MiWillManager.destroyInstance();
            this.mMiWillManager = null;
        }
    }

    /* loaded from: classes.dex */
    class SlaServiceDeathRecipient implements IHwBinder.DeathRecipient {
        SlaServiceDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Log.e(SLAService.TAG, "HAL service died");
            SLAService.this.mHandler.sendMessageDelayed(SLAService.this.mHandler.obtainMessage(103, 1), 4000L);
        }
    }

    private static MacAddress getMacAddress(byte[] linkLayerAddress) {
        if (linkLayerAddress != null) {
            try {
                return MacAddress.fromBytes(linkLayerAddress);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to parse link-layer address: " + linkLayerAddress);
                return null;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMobileDataAlwaysOn() {
        int mobileDataAlwaysOnMode = Settings.Global.getInt(mContext.getContentResolver(), "mobile_data_always_on", 0);
        if (mobileDataAlwaysOnMode == 0 && !this.mSingnalPoor) {
            Settings.Global.putInt(mContext.getContentResolver(), "mobile_data_always_on", 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMobileDataAlwaysOff() {
        int mobileDataAlwaysOnMode = Settings.Global.getInt(mContext.getContentResolver(), "mobile_data_always_on", 0);
        if (mobileDataAlwaysOnMode == 1) {
            Settings.Global.putInt(mContext.getContentResolver(), "mobile_data_always_on", 0);
        }
    }

    public void enableSLM() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessage(104);
        } else {
            Log.e(TAG, "enableSLM handler is null");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleEnableSLM() {
        int i = this.disableSLMCnt;
        if (i > 0) {
            this.disableSLMCnt = i - 1;
        }
        if (isSLMSwitchON() && this.disableSLMCnt == 0) {
            setMobileDataAlwaysOn();
            processNetworkCallback(ON_DISABLE_EVENT);
        }
    }

    public void disableSLM() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessage(105);
        } else {
            Log.e(TAG, "disableSLM handler is null");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDisableSLM() {
        this.disableSLMCnt++;
        if (isSLMSwitchON() && this.disableSLMCnt == 1) {
            stopSLAD();
            checkWlanWwanCoexistense();
            setMobileDataAlwaysOff();
        }
    }

    public boolean setMiWillGameStart(String uid) {
        MiWillManager miWillManager = this.mMiWillManager;
        if (miWillManager != null) {
            return miWillManager.setMiWillGameStart(uid);
        }
        Log.e(TAG, "setMiWillGameStart manager is null: " + uid);
        return false;
    }

    public boolean setMiWillGameStop(String uid) {
        MiWillManager miWillManager = this.mMiWillManager;
        if (miWillManager != null) {
            return miWillManager.setMiWillGameStop(uid);
        }
        Log.e(TAG, "setMiWillGameStop manager is null: " + uid);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkMiwillCloudController() {
        String[] controllers;
        boolean isMiwillOn = true;
        try {
            String miwillControllerList = Settings.System.getString(mContext.getContentResolver(), CLOUD_MIWILL_CONTROLLER_LIST);
            if (!TextUtils.isEmpty(miwillControllerList) && (controllers = miwillControllerList.split(",")) != null) {
                for (int i = 0; i < controllers.length; i++) {
                    if (controllers[i].equals(CLOUD_MIWILL_ON)) {
                        isMiwillOn = true;
                    } else if (controllers[i].equals(CLOUD_MIWILL_OFF)) {
                        Log.i(TAG, "checkMiwillCloudController miwill dual wifi off because miwill off");
                        SystemProperties.set(PROP_MIWILL_DUAL_WIFI_ENABLE, "false");
                        isMiwillOn = false;
                    } else if (controllers[i].equals(CLOUD_MIWILL_DUAL_WIFI_ON)) {
                        Log.i(TAG, "checkMiwillCloudController miwill dual wifi on");
                        SystemProperties.set(PROP_MIWILL_DUAL_WIFI_ENABLE, "true");
                    } else if (controllers[i].equals(CLOUD_MIWILL_DUAL_WIFI_OFF)) {
                        Log.i(TAG, "checkMiwillCloudController miwill dual wifi off");
                        SystemProperties.set(PROP_MIWILL_DUAL_WIFI_ENABLE, "false");
                    }
                }
            }
            return isMiwillOn;
        } catch (Exception e) {
            Log.e(TAG, "cloud observer catch:", e);
            return false;
        }
    }

    private void initCloudObserver() {
        if (this.mCloudObserver != null) {
            Log.e(TAG, "initCloudObserver rejected! observer has already registered! check your code.");
        }
        if (mContext != null) {
            this.mCloudObserver = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAService.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    if (SLAService.this.checkMiwillCloudController() && SLAService.this.mMiWillManager == null) {
                        Log.i(SLAService.TAG, "cloud controller enable miwill.");
                        SLAService.this.mMiWillManager = MiWillManager.getInstance(SLAService.mContext);
                    } else if (!SLAService.this.checkMiwillCloudController() && SLAService.this.mMiWillManager != null) {
                        Log.i(SLAService.TAG, "cloud controller disable miwill.");
                        MiWillManager.destroyInstance();
                        SLAService.this.mMiWillManager = null;
                    }
                    SLAService.this.isEnableDWMonitor();
                }
            };
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MIWILL_CONTROLLER_LIST), false, this.mCloudObserver);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_DOUBLE_WIFI_MONITOR), false, this.mCloudObserver);
            return;
        }
        Log.e(TAG, "initCloudObserver failed! mContext should be initialized in onCreate");
    }

    private void deinitCloudObserver() {
        Context context;
        if (this.mCloudObserver != null && (context = mContext) != null) {
            context.getContentResolver().unregisterContentObserver(this.mCloudObserver);
            this.mCloudObserver = null;
        } else {
            Log.w(TAG, "deinitCloudObserver ignore because: observer: " + this.mCloudObserver + " context: " + mContext);
        }
    }

    private boolean isSLAReady() {
        if (mLinkTurboSwitch) {
            return (mDataReady || mSlaveDataReady) && mIfaceNumber > 1 && this.disableSLMCnt == 0;
        }
        return false;
    }

    private boolean isSLSReady() {
        return isSLAReady() || isDWReady();
    }

    private boolean isDWReady() {
        return mWifiReady && mSlaveWifiReady && mIfaceNumber > 1 && this.disableSLMCnt == 0;
    }

    private boolean isDDReady() {
        return mDataReady && mSlaveDataReady && mWifiReady == mSlaveWifiReady && mIfaceNumber > 1 && this.disableSLMCnt == 0;
    }

    private boolean isSLMSwitchON() {
        if ((mWifiReady && mSlaveWifiReady) || mLinkTurboSwitch) {
            return true;
        }
        return false;
    }

    private void updateSLAD() {
        Log.i(TAG, "updateSLAD");
        Handler handler = this.mWacthdogHandler;
        handler.sendMessageDelayed(handler.obtainMessage(100), 120000L);
        synchronized (mSlaLock) {
            try {
                ISlaService iSlaService = mSlaService;
                if (iSlaService == null) {
                    Log.e(TAG, "HAL service is null, get");
                    Handler handler2 = this.mHandler;
                    handler2.sendMessage(handler2.obtainMessage(103, 0));
                } else {
                    mSLAAppLib.setSlaService(iSlaService);
                    mSLSAppLib.setSlaService(mSlaService);
                    Log.i(TAG, "isSLAReady:" + isSLAReady() + ", isDWReady:" + isDWReady() + ", isDDReady:" + isDDReady());
                    if (!isSLAReady() && !isDWReady() && !isDDReady()) {
                        if (sladEnabled) {
                            Log.i(TAG, "stopSlad");
                            try {
                                mSlaService.stopSlad();
                                sladEnabled = false;
                            } catch (Exception e) {
                                Log.e(TAG, "updateSLAD SlaService stopSlad failed " + e);
                            }
                            mSLAAppLib.stopSladHandle();
                        } else {
                            Log.d(TAG, "slad is already disabled");
                        }
                    }
                    if (!sladEnabled) {
                        Log.i(TAG, "startSlad ");
                        try {
                            mSlaService.startSlad();
                            sladEnabled = true;
                        } catch (Exception e2) {
                            Log.e(TAG, "updateSLAD SlaService startSlad failed " + e2);
                        }
                    } else {
                        Log.d(TAG, "slad is already enabled");
                    }
                    setInterface();
                    if (isSLAReady()) {
                        mSLAAppLib.startSladHandle();
                    }
                    if (isSLSReady()) {
                        mSLSAppLib.setWifiBSSID();
                    }
                    if (isDWReady()) {
                        mSLAAppLib.sendMsgDoubleWifiUid();
                    }
                    if (isDDReady()) {
                        mSLAAppLib.sendMsgDoubleDataUid();
                        mSLAAppLib.setDDSLAMode();
                    }
                    setSLAWifiGateway();
                    setDWMonitorEnable();
                }
            } catch (Exception e3) {
                Log.e(TAG, "Exception:" + e3);
            }
            mSlaToast.setLinkTurboStatus(isSLAReady());
            mSLSAppLib.setSLSEnableStatus(isSLSReady());
            SLAToast.setLinkTurboUidList(mSLAAppLib.getLinkTurboWhiteList());
            SLSAppLib.setSLAAppWhiteList(mSLAAppLib.getLinkTurboWhiteList());
            if (isSLAReady()) {
                if (isDWReady()) {
                    SLATrack.sendMsgDoubleWifiAndSLAStart();
                } else {
                    SLATrack.sendMsgSlaStart();
                }
            }
        }
        this.mWacthdogHandler.removeMessages(100);
    }

    private void stopSLAD() {
        Log.i(TAG, "stopSLAD");
        synchronized (mSlaLock) {
            try {
                if (sladEnabled) {
                    Log.i(TAG, "stopSlad");
                    mSlaService.stopSlad();
                    sladEnabled = false;
                    mSLAAppLib.stopSladHandle();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
            }
        }
        mSLSAppLib.setSLSEnableStatus(isSLSReady());
        SLATrack.sendMsgSlsStop();
        SLATrack.sendMsgDoubleWifiAndSLAStop();
        SLATrack.sendMsgSlaStop();
    }

    private void acquireSlaServiceLock(String func) {
        Log.d(TAG, func + " Lock");
        try {
            mSemaphore.acquire();
            Log.d(TAG, func + " Lock success");
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException:" + e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean setDoubleWifiWhiteList(int type, String packageName, String uid, boolean isOperateBlacklist) {
        SLAAppLib sLAAppLib = mSLAAppLib;
        if (sLAAppLib == null) {
            return false;
        }
        return sLAAppLib.setDoubleWifiWhiteList(type, packageName, uid, isOperateBlacklist);
    }

    public void setDWUidToSlad() {
        SLAAppLib sLAAppLib;
        if (isDWReady() && (sLAAppLib = mSLAAppLib) != null) {
            sLAAppLib.sendMsgDoubleWifiUid();
        } else {
            Log.d("NetworkSDKService", "setDWUidToSlad isDWReady = " + isDWReady());
        }
    }

    private void releaseSlaServiceLock(String func) {
        Semaphore semaphore = mSemaphore;
        semaphore.drainPermits();
        semaphore.release();
        Log.d(TAG, func + " Unlock");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processNetworkCallback(String status) {
        Log.i(TAG, "processNetworkCallback " + status);
        checkWlanWwanCoexistense();
        updateSLAD();
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
                    SLAService.this.processNetworkCallback(SLAService.ON_USER);
                    return;
                case 101:
                case 102:
                case StatusManager.CALLBACK_NETWORK_PRIORITY_CHANGED /* 109 */:
                default:
                    return;
                case 103:
                    try {
                        SLAService.mSlaService = ISlaService.getService();
                        if (SLAService.mSlaService == null) {
                            Log.e(SLAService.TAG, "HAL service is null");
                            SLAService.this.mHandler.sendMessageDelayed(SLAService.this.mHandler.obtainMessage(103, msg.obj), 4000L);
                        } else {
                            Log.d(SLAService.TAG, "HAL service get success");
                            SLAService.mSlaService.linkToDeath(SLAService.this.mDeathRecipient, 0L);
                        }
                        if (SLAService.mSlaService != null && SLAService.mSLAAppLib != null) {
                            SLAService.mSLAAppLib.setSlaService(SLAService.mSlaService);
                        }
                        if (SLAService.mSlaService != null && SLAService.mSLSAppLib != null) {
                            SLAService.mSLSAppLib.setSlaService(SLAService.mSlaService);
                        }
                        if (((Integer) msg.obj).intValue() == 1) {
                            sendMessage(obtainMessage(100));
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        Log.e(SLAService.TAG, "Exception:" + e);
                        return;
                    }
                case 104:
                    SLAService.this.handleEnableSLM();
                    return;
                case 105:
                    SLAService.this.handleDisableSLM();
                    return;
                case 106:
                    if (SLAService.mSLSAppLib != null) {
                        SLAService.mSLSAppLib.setPowerMgrScreenInfo(true);
                        return;
                    } else {
                        Log.e(SLAService.TAG, "EVENT_SCREEN_ON mSLSAppLib is null");
                        return;
                    }
                case 107:
                    if (SLAService.mSLSAppLib != null) {
                        SLAService.mSLSAppLib.setPowerMgrScreenInfo(false);
                        return;
                    } else {
                        Log.e(SLAService.TAG, "EVENT_SCREEN_OFF mSLSAppLib is null");
                        return;
                    }
                case 108:
                    if (SLAService.this.isSlaRemind() && SLAService.this.getLinkTurboAppsTotalDayTraffic() > 300) {
                        try {
                            SLAService.this.postTrafficNotification();
                        } catch (Exception e2) {
                            Log.e(SLAService.TAG, "Exception:" + e2);
                        }
                    }
                    SLAService.this.mHandler.sendMessageDelayed(SLAService.this.mHandler.obtainMessage(108), 10000L);
                    return;
                case 110:
                    SLAService.this.checktemp(SLAService.THERMAL_STATE_FILE);
                    SLAService.this.mHandler.sendEmptyMessageDelayed(110, 10000L);
                    return;
            }
        }
    }

    private void registerNetworkCallback() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            statusManager.registerNetworkInterfaceListener(this.mNetworkInterfaceListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterNetworkCallback() {
        try {
            if (this.mNetworkInterfaceListener != null) {
                StatusManager statusManager = StatusManager.getInstance(mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterNetworkInterfaceListener(this.mNetworkInterfaceListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void registerNetworkStateBroadcastReceiver() {
        IntentFilter wifiStateFilter = new IntentFilter();
        wifiStateFilter.addAction("android.net.wifi.STATE_CHANGE");
        mContext.registerReceiver(this.mWifiStateReceiver, wifiStateFilter);
    }

    private void unregisterNetworkStateBroadcastReceiver() {
        mContext.unregisterReceiver(this.mWifiStateReceiver);
    }

    private void setInterface() {
        Log.i(TAG, "setInterface");
        if (mSlaService != null) {
            try {
                Log.i(TAG, "setInterface:" + mInterface);
                mSlaService.setInterface(mInterface);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return;
            }
        }
        Log.d(TAG, "mSlaService is null");
    }

    private void registerDefaultNetworkCallback() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            statusManager.registerDefaultNetworkInterfaceListener(this.mDefaultNetworkInterfaceListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterDefaultNetworkCallback() {
        try {
            if (this.mDefaultNetworkInterfaceListener != null) {
                StatusManager statusManager = StatusManager.getInstance(mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterDefaultNetworkInterfaceListener(this.mDefaultNetworkInterfaceListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void registerVPNChangedCallback() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            statusManager.registerVPNListener(this.mVPNListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterVPNChangedCallback() {
        try {
            if (this.mVPNListener != null) {
                StatusManager statusManager = StatusManager.getInstance(mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterVPNListener(this.mVPNListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void registerScreenStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            statusManager.registerScreenStatusListener(this.mScreenStatusListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterScreenStatusListener() {
        try {
            if (this.mScreenStatusListener != null) {
                StatusManager statusManager = StatusManager.getInstance(mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterScreenStatusListener(this.mScreenStatusListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOn() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(107);
            this.mHandler.sendEmptyMessage(106);
        } else {
            Log.e(TAG, "handleScreenOn handler is null");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOff() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessageDelayed(107, 180000L);
        } else {
            Log.e(TAG, "handleScreenOff handler is null");
        }
    }

    private void checkWlanWwanCoexistense() {
        int wlanwwanCoexistanceStatus;
        if (isSLAReady()) {
            wlanwwanCoexistanceStatus = 1;
        } else {
            wlanwwanCoexistanceStatus = 0;
        }
        if (wlanwwanCoexistanceStatus != this.mlastWlanWwanCoexistanceStatus) {
            this.mlastWlanWwanCoexistanceStatus = wlanwwanCoexistanceStatus;
            notifyWlanWwanCoexistenseStatus(wlanwwanCoexistanceStatus);
        }
    }

    private void notifyWlanWwanCoexistenseStatus(int status) {
        Log.d(TAG, "notifyWlanWwanCoexistenseStatus " + status);
        Intent intent = new Intent();
        intent.setAction(SLM_FEATURE_STATUS);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(COEXISTENSE, status);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void setSLAWifiGateway() {
        DhcpInfo dhcpInfo = null;
        DhcpInfo slaveDhcpInfo = null;
        StringBuffer gateway = new StringBuffer();
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            dhcpInfo = wifiManager.getDhcpInfo();
        }
        SlaveWifiManager slaveWifiManager = this.mSlaveWifiManager;
        if (slaveWifiManager != null) {
            slaveDhcpInfo = slaveWifiManager.getSlaveDhcpInfo();
        }
        if (dhcpInfo != null) {
            String address = NetworkUtils.intToInetAddress(dhcpInfo.gateway).getHostAddress();
            Log.e(TAG, "Gateway address:" + address);
            gateway.append(address);
            gateway.append(",");
        }
        if (slaveDhcpInfo != null) {
            String address2 = NetworkUtils.intToInetAddress(slaveDhcpInfo.gateway).getHostAddress();
            Log.e(TAG, "Slave Gateway address:" + address2);
            gateway.append(address2);
            gateway.append(",");
        }
        if (gateway.length() > 0) {
            try {
                mSlaService.setWifiGateway(gateway.toString());
            } catch (Exception ex) {
                Log.e(TAG, "setSLAWifiGateway catch ex:" + ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void isEnableDWMonitor() {
        String cvalue = Settings.System.getStringForUser(mContext.getContentResolver(), CLOUD_DOUBLE_WIFI_MONITOR, -2);
        Log.d(TAG, "isEnableDWMonitor =" + cvalue);
        this.misEnableDWMonitor = cvalue != null && "v1".equals(cvalue);
    }

    private void setDWMonitorEnable() {
        Log.d(TAG, "setDWMonitorEnable enter");
        if (!this.misEnableDWMonitor) {
            return;
        }
        if (isDWReady() && !isSLAReady()) {
            try {
                mSlaService.setDWMonitorEnable("1".toString());
            } catch (Exception ex) {
                Log.e(TAG, "setDWMonitorEnable catch ex:" + ex);
            }
        } else {
            try {
                mSlaService.setDWMonitorEnable(SLAD_STOP.toString());
            } catch (Exception ex2) {
                Log.e(TAG, "setDWMonitorEnable catch ex:" + ex2);
            }
        }
        Log.d(TAG, "setDWMonitorEnable exit");
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:11:0x0019 -> B:6:0x0032). Please report as a decompilation issue!!! */
    private String readLine(String path) {
        BufferedReader reader = null;
        String line = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new FileReader(path), 1024);
                    line = reader.readLine();
                    reader.close();
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (Exception e3) {
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        return line;
    }

    private boolean isPerformanceMode() {
        try {
            String line = readLine(THERMAL_MODE_FILE);
            if (line != null) {
                String str_temp = line.trim();
                int mode = Integer.parseInt(str_temp);
                if (mode == 1) {
                    return true;
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checktemp(String path) {
        try {
            String line = readLine(THERMAL_STATE_FILE);
            if (line != null) {
                String str_temp = line.trim();
                long temperature = Long.parseLong(str_temp);
                this.mThermalList.add(Long.valueOf(temperature));
                if (this.mThermalList.size() > 6) {
                    this.mThermalList.remove(0);
                }
                long temperature_average = 0;
                Iterator<Long> it = this.mThermalList.iterator();
                while (it.hasNext()) {
                    long tmp = it.next().longValue();
                    temperature_average += tmp;
                }
                long temperature_average2 = temperature_average / this.mThermalList.size();
                Log.d(TAG, "checktemp temperature = " + temperature + " temperature_average = " + temperature_average2 + " isPerformanceMode = " + isPerformanceMode() + " thermal_enable_slm = " + this.thermal_enable_slm);
                if (this.thermal_enable_slm && !isPerformanceMode() && temperature_average2 > THERMAL_DISABLE) {
                    Log.i(TAG, "Thermal management disableSLM - " + temperature_average2);
                    this.thermal_enable_slm = false;
                    disableSLM();
                } else {
                    if (this.thermal_enable_slm) {
                        return;
                    }
                    if (temperature_average2 < THERMAL_ENABLE || isPerformanceMode()) {
                        Log.i(TAG, "Thermal management enableSLM - " + temperature_average2);
                        this.thermal_enable_slm = true;
                        enableSLM();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "checktemp:", e);
        }
    }

    private void registerModemSignalStrengthListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            statusManager.registerModemSignalStrengthListener(this.mModemSignalStrengthListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void unregisterModemSignalStrengthListener() {
        try {
            if (this.mModemSignalStrengthListener != null) {
                StatusManager statusManager = StatusManager.getInstance(mContext);
                this.mStatusManager = statusManager;
                statusManager.unregisterModemSignalStrengthListener(this.mModemSignalStrengthListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void registerNetworkBoostWatchdog() {
        try {
            StatusManager statusManager = StatusManager.getInstance(mContext);
            this.mStatusManager = statusManager;
            this.mWacthdogHandlerThread = statusManager.getNetworkBoostWatchdogHandler();
            this.mWacthdogHandler = new WatchdogHandler(this.mWacthdogHandlerThread.getLooper());
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WatchdogHandler extends Handler {
        public WatchdogHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Log.i(SLAService.TAG, "WatchdogHandler: " + msg.what);
            switch (msg.what) {
                case 100:
                    SystemProperties.set("sys.sla.enabled", SLAService.SLAD_STOP);
                    return;
                default:
                    return;
            }
        }
    }

    public void dumpModule(PrintWriter writer) {
        try {
            writer.println("SLAService begin:");
            writer.println("    WifiReady:" + mWifiReady);
            writer.println("    SlaveWifiReady:" + mSlaveWifiReady);
            writer.println("    DataReady:" + mDataReady);
            writer.println("    Interface:" + mInterface);
            writer.println("    IfaceNumber:" + mIfaceNumber);
            writer.println("    disableSLMCnt:" + this.disableSLMCnt);
            writer.println("    mlastWlanWwanCoexistanceStatus:" + this.mlastWlanWwanCoexistanceStatus);
            writer.println("    thermal_enable_slm:" + this.thermal_enable_slm);
            writer.println("    mThermalList:" + this.mThermalList.size() + " {");
            String temperatureStr = "";
            Iterator<Long> it = this.mThermalList.iterator();
            while (it.hasNext()) {
                Long temperature = it.next();
                if (1 != 0) {
                    temperatureStr = temperatureStr + temperature;
                } else {
                    temperatureStr = temperatureStr + ", " + temperature;
                }
            }
            writer.println("        " + temperatureStr);
            writer.println("    }");
            writer.println("SLAService end.\n");
        } catch (Exception ex) {
            Log.e(TAG, "dump failed!", ex);
            try {
                writer.println("Dump of SLAService failed:" + ex);
                writer.println("SLAService end.\n");
            } catch (Exception e) {
                Log.e(TAG, "dump failure failed:" + e);
            }
        }
    }

    private void createSLANotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "xiaomi.NetworkBoost", 4);
        channel.setDescription("xiaomi.NetworkBoost.channel");
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(NotificationManager.class);
        this.mNotificationManager = notificationManager;
        notificationManager.createNotificationChannel(channel);
    }

    private void postNotification() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSlaRemind() {
        return 1 == Settings.System.getInt(mContext.getContentResolver(), SLA_NOTIFICATION_STATE, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postTrafficNotification() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getLinkTurboAppsTotalDayTraffic() {
        long traffic = 0;
        List<SLAApp> linkTurboAppsTraffic = getLinkTurboAppsTraffic();
        this.mSLAApp = linkTurboAppsTraffic;
        if (linkTurboAppsTraffic == null || linkTurboAppsTraffic.size() < 1) {
            return 0L;
        }
        for (int i = 0; i < this.mSLAApp.size(); i++) {
            traffic += this.mSLAApp.get(i).getDayTraffic();
        }
        Log.d(TAG, "TotalDayTraffic: " + traffic);
        return traffic / FormatBytesUtil.MB;
    }
}
