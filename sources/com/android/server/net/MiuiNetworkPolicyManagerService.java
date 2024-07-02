package com.android.server.net;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.telephony.PhoneConstants;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.net.MiuiNetworkManagementService;
import com.xiaomi.NetworkBoost.NetworkBoostService;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import com.xiaomi.NetworkBoost.StatusManager;
import com.xiaomi.abtest.d.i;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import miui.os.Build;
import miui.util.DeviceLevel;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiNetworkPolicyManagerService {
    private static final String ACTION_NETWORK_CONDITIONS_MEASURED = "android.net.conn.NETWORK_CONDITIONS_MEASURED";
    private static final String ACTION_SLEEP_MODE_CHANGED = "com.miui.powerkeeper_sleep_changed";
    private static final long BG_MIN_BANDWIDTH = 100000;
    private static final String CLOUD_AUTO_FORWARD_ENABLED = "cloud_auto_forward_enabled";
    private static final String CLOUD_DNS_HAPPY_EYEBALLS_ENABLED = "cloud_dns_happy_eyeballs_priority_enabled";
    private static final String CLOUD_DNS_HAPPY_EYEBALLS_PRIORITY_TIME = "cloud_dns_happy_eyeballs_priority_time";
    private static final String CLOUD_LOW_LATENCY_APPLIST_FOR_MOBILE = "cloud_block_scan_applist_for_mobile";
    private static final String CLOUD_LOW_LATENCY_WHITELIST = "cloud_lowlatency_whitelist";
    private static final String CLOUD_MTK_WIFI_TRAFFIC_PRIORITY_MODE = "cloud_mtk_wifi_traffic_priority_mode";
    private static final String CLOUD_MTK_WMMER_ENABLED = "cloud_mtk_wmmer_enabled";
    private static final String CLOUD_NETWORK_PRIORITY_ENABLED = "cloud_network_priority_enabled";
    private static final String CLOUD_NETWORK_TRUST_MIUI_ADD_DNS = "cloud_trust_miui_add_dns";
    private static final String CLOUD_P2PHC_ENABLED = "cloud_p2phc_enabled";
    private static final String CLOUD_RESTRICT_WIFI_POWERSAVE_APPLIST = "cloud_block_scan_applist";
    public static final String CLOUD_SLEEPMODE_NETWORKPOLICY_ENABLED = "cloud_sleepmode_networkpolicy_enabled";
    private static final String CLOUD_SMARTDNS_ENABLED = "cloud_smartdns_enabled";
    private static final String CLOUD_WMMER_ENABLED = "cloud_wmmer_enabled";
    private static final String CLOUD_WMMER_ROUTER_WHITELIST = "cloud_wmmer_router_whitelist";
    private static final boolean DEBUG = true;
    private static final int DISABLE_LIMIT_TIMEOUT_LEVEL_1 = 15000;
    private static final int DISABLE_LIMIT_TIMEOUT_LEVEL_2 = 7500;
    private static final int DISABLE_LIMIT_TIMEOUT_LEVEL_3 = 5000;
    private static final int DISABLE_POWER_SAVE_TIMEOUT = 5000;
    public static final String EARTHQUAKE_WARNING_FLAG = "key_open_earthquake_warning";
    private static final int EARTH_QUACK_ON = 1;
    private static final int EID_VSA = 221;
    private static final int ENABLE_LIMIT_TIMEOUT_LEVEL_1 = 15000;
    private static final int ENABLE_LIMIT_TIMEOUT_LEVEL_2 = 22500;
    private static final int ENABLE_LIMIT_TIMEOUT_LEVEL_3 = 25000;
    private static final String EXTRA_IS_CAPTIVE_PORTAL = "extra_is_captive_portal";
    private static final String EXTRA_STATE = "state";
    private static final long FG_MAX_BANDWIDTH = 1000000;
    private static final long HISTORY_BANDWIDTH_MIN = 200000;
    private static final int HISTORY_BANDWIDTH_SIZE = 10;
    private static final String LATENCY_ACTION_CHANGE_LEVEL = "com.android.phone.intent.action.CHANGE_LEVEL";
    private static final int LATENCY_DEFAULT = -1;
    private static final String LATENCY_KEY_LEVEL_DL = "Level_DL";
    private static final String LATENCY_KEY_LEVEL_UL = "Level_UL";
    private static final String LATENCY_KEY_RAT_TYPE = "Rat_type";
    private static final int LATENCY_OFF = 0;
    private static final int LATENCY_ON = 1;
    private static final long LATENCY_VALUE_L1 = 1;
    private static final long LATENCY_VALUE_L2 = 2;
    private static final long LATENCY_VALUE_L3 = 3;
    private static final long LATENCY_VALUE_L4 = 4;
    private static final long LATENCY_VALUE_WLAN = 1;
    private static final long LATENCY_VALUE_WWAN = 0;
    private static final long MAX_ROUTER_DETECT_TIME = 3000000;
    private static final int MIN_THIRDAPP_UID = 10000;
    private static final int MSG_BANDWIDTH_POLL = 6;
    private static final int MSG_CHECK_ROUTER_MTK = 11;
    private static final int MSG_DISABLE_LIMIT_TIMEOUT = 5;
    private static final int MSG_DISABLE_POWER_SAVE_TIMEOUT = 8;
    private static final int MSG_ENABLE_LIMIT_TIMEOUT = 4;
    private static final int MSG_MOBILE_LATENCY_CHANGED = 9;
    public static final int MSG_SET_LIMIT_FOR_MOBILE = 13;
    private static final int MSG_SET_RPS_STATS = 10;
    public static final int MSG_SET_THERMAL_POLICY = 14;
    private static final int MSG_SET_TRAFFIC_POLICY = 7;
    private static final int MSG_UID_DATA_ACTIVITY_CHANGED = 3;
    public static final int MSG_UID_STATE_CHANGED = 1;
    public static final int MSG_UID_STATE_GONED = 2;
    public static final int MSG_UPDATE_RULE_FOR_MOBILE_TRAFFIC = 12;
    private static final int NETWORK_PRIORITY_MODE_CLOSED = 255;
    private static final int NETWORK_PRIORITY_MODE_FAST = 2;
    private static final int NETWORK_PRIORITY_MODE_NORMAL = 1;
    private static final int NETWORK_PRIORITY_MODE_WMM = 0;
    private static final String NETWORK_PRIORITY_WHITELIST = "cloud_network_priority_whitelist";
    private static final String NOTIFACATION_RECEIVER_PACKAGE = "com.android.phone";
    private static final String PERSIST_DEVICE_CONFIG_NETD_ENABLED_VALUE = "persist.device_config.netd_native.happy_eyeballs_enable";
    private static final String PERSIST_DEVICE_CONFIG_NETD_PRIORITY_TIME = "persist.device_config.netd_native.happy_eyeballs_master_server_priority_time";
    private static final String PERSIST_DEVICE_CONFIG_NETD_SMARTDNS_ENABLED_VALUE = "persist.device_config.netd_native.smartdns_enable";
    private static final String PERSIST_DEVICE_CONFIG_NETD_VALUE = "persist.device_config.netd_native.trust_miui_add_dns";
    private static final String PERSIST_WPA_SUPPLICANT_P2PHC_ENABLE = "vendor.miui.wifi.p2phc";
    private static final int POLL_BANDWIDTH_INTERVAL_SECS = 3;
    private static final int POWER_SAVE_IDLETIMER_LABEL = 118;
    private static final int POWER_SAVE_IDLETIMER_TIMEOUT = 2;
    private static final String SLEEPMODE_TAG = "MiuiNetworkPolicySleepMode";
    private static final int SLEEP_MODE_ON = 1;
    private static final String TAG = "MiuiNetworkPolicy";
    private static final int THERMAL_FAST_LEVEL_1 = 1;
    private static final int THERMAL_FAST_LEVEL_2 = 2;
    private static final int THERMAL_FAST_LEVEL_3 = 3;
    private static final int THERMAL_FIRST_MODE = 0;
    private static final int THERMAL_LAST_MODE = 3;
    private static final int THERMAL_NORMAL_MODE = 0;
    private static final String THERMAL_STATE_FILE = "/sys/class/thermal/thermal_message/wifi_limit";
    private static final int WMM_AC_BE = 0;
    private static final int WMM_AC_VI = 1;
    private static final int WMM_AC_VO = 2;
    private static MiuiNetworkPolicyManagerService sSelf;
    private ConnectivityManager cm;
    private MiuiNetworkPolicyAppBuckets mAppBuckets;
    private boolean mCloudWmmerEnable;
    private String mConnectedApModel;
    private ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private Network mCurrentNetwork;
    private final Handler mHandler;
    private Deque<Long> mHistoryBandWidth;
    private String mInterfaceName;
    private boolean mIsCaptivePortal;
    private boolean mIsMtkRouter;
    private String mLastIpAddress;
    private int mLastNetId;
    private long mLastRxBytes;
    private boolean mLimitEnabled;
    private Set<Integer> mLowLatencyApps;
    private Set<String> mLowLatencyAppsPN;
    private MiuiWifiManager mMiuiWifiManager;
    private Set<Integer> mMobileLowLatencyApps;
    private Set<String> mMobileLowLatencyAppsPN;
    private MiuiNetworkPolicyTrafficLimit mMobileTcUtils;
    private Set<Integer> mNeedRestrictPowerSaveApps;
    private Set<String> mNeedRestrictPowerSaveAppsPN;
    private MiuiNetworkManagementService mNetworkManager;
    private int mNetworkPriorityMode;
    private MiuiNetworkPolicyQosUtils mQosUtils;
    private Network mSlaveWifiNetwork;
    private Set<Integer> mSleepModeWhitelistUids;
    private MiuiNetworkPolicyServiceSupport mSupport;
    private ThermalStateListener mThermalStateListener;
    private int mTrafficPolicyMode;
    private Set<Integer> mUnRestrictApps;
    private Set<String> mUnRestrictAppsPN;
    private UserManager mUserManager;
    private ConnectivityManager.NetworkCallback mVPNNetworkCallback;
    private NetworkRequest mVPNNetworkRequest;
    private Network mVpnNetwork;
    private boolean mWifiConnected;
    private ConnectivityManager.NetworkCallback mWifiNetworkCallback;
    private NetworkRequest mWifiNetworkRequest;
    private boolean mWmmerEnable;
    private static final String[] LOCAL_NETWORK_PRIORITY_WHITELIST = {"com.tencent.mm", "com.tencent.mobileqq", "com.xiaomi.xmsf", "com.google.android.gms", "com.google.android.dialer", "com.whatsapp", "com.miui.vpnsdkmanager"};
    private static final byte[] AVOID_CHANNEL_IE = {0, -96, -58, 0};
    private static final byte[] WPA_AKM_EAP_IE = {0, 80, -14, 1};
    private static final byte[] WPA_AKM_PSK_IE = {0, 80, -14, 2};
    private static final byte[] WPA2_AKM_PSK_IE = {0, 80, -14, 4};
    private static final byte[] MTK_OUI1 = {0, 12, -25};
    private static final byte[] MTK_OUI2 = {0, 10, 0};
    private static final byte[] MTK_OUI3 = {0, 12, 67};
    private static final byte[] MTK_OUI4 = {0, 23, -91};
    private static final byte[] MODEL_NAME = {MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH, 35};
    private static int mMobileLatencyState = -1;
    private static final boolean IS_QCOM = "qcom".equals(FeatureParser.getString("vendor"));
    private HashSet<String> mWmmerRouterWhitelist = new HashSet<>();
    private long ROUTER_DETECT_TIME = 10000;
    private String mDefaultInputMethod = "";
    private boolean mPowerSaveEnabled = true;
    private boolean mRpsEnabled = false;
    final SparseIntArray mUidState = new SparseIntArray();
    private PhoneConstants.DataState mConnectState = PhoneConstants.DataState.DISCONNECTED;
    private NetworkBoostService mNetworkBoostService = null;
    private boolean mHasAutoForward = false;
    private boolean mHasListenWifiNetwork = false;
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.14
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(MiuiNetworkPolicyManagerService.TAG, "mWifiStateReceiver onReceive: " + action);
            boolean z = false;
            if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                boolean wasConnected = MiuiNetworkPolicyManagerService.this.mWifiConnected;
                MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                if (netInfo != null && netInfo.isConnected()) {
                    z = true;
                }
                miuiNetworkPolicyManagerService.mWifiConnected = z;
                Log.i(MiuiNetworkPolicyManagerService.TAG, "wasConnected = " + wasConnected + " mWifiConnected = " + MiuiNetworkPolicyManagerService.this.mWifiConnected + " mNetworkPriorityMode =" + MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode);
                if (MiuiNetworkPolicyManagerService.this.mWifiConnected != wasConnected) {
                    MiuiNetworkPolicyManagerService.this.enablePowerSave(true);
                    if (MiuiNetworkPolicyManagerService.this.mWifiConnected) {
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService2 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService2.mInterfaceName = miuiNetworkPolicyManagerService2.mSupport.updateIface(MiuiNetworkPolicyManagerService.this.mInterfaceName);
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService3 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService3.mIsMtkRouter = miuiNetworkPolicyManagerService3.checkRouterMTK();
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService4 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService4.mConnectedApModel = miuiNetworkPolicyManagerService4.getRouterModel();
                        Log.d(MiuiNetworkPolicyManagerService.TAG, "router is mtk: " + MiuiNetworkPolicyManagerService.this.mIsMtkRouter);
                    }
                    MiuiNetworkPolicyManagerService.this.enableWmmer();
                    int networkPriority = MiuiNetworkPolicyManagerService.this.networkPriorityMode();
                    if (MiuiNetworkPolicyManagerService.this.isLimitterEnabled(networkPriority)) {
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService5 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService5.enableNetworkPriority(miuiNetworkPolicyManagerService5.mWifiConnected ? networkPriority : MiuiNetworkPolicyManagerService.NETWORK_PRIORITY_MODE_CLOSED);
                        return;
                    }
                    return;
                }
                return;
            }
            if (MiuiNetworkPolicyManagerService.ACTION_NETWORK_CONDITIONS_MEASURED.equals(action)) {
                boolean wasCaptivePortal = MiuiNetworkPolicyManagerService.this.mIsCaptivePortal;
                MiuiNetworkPolicyManagerService.this.mIsCaptivePortal = intent.getBooleanExtra(MiuiNetworkPolicyManagerService.EXTRA_IS_CAPTIVE_PORTAL, false);
                Log.i(MiuiNetworkPolicyManagerService.TAG, "network was: " + wasCaptivePortal + " captive portal, and now is " + MiuiNetworkPolicyManagerService.this.mIsCaptivePortal);
                if (wasCaptivePortal != MiuiNetworkPolicyManagerService.this.mIsCaptivePortal && MiuiNetworkPolicyManagerService.this.mWmmerEnable) {
                    MiuiNetworkPolicyManagerService.this.updateRuleGlobal();
                }
            }
        }
    };
    private Handler.Callback mHandlerCallback = new Handler.Callback() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.24
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiNetworkPolicyManagerService.this.updateUidState(msg.arg1, msg.arg2);
                    return false;
                case 2:
                    MiuiNetworkPolicyManagerService.this.removeUidState(msg.arg1);
                    return false;
                case 3:
                    String label = (String) msg.obj;
                    int uid = msg.arg1;
                    boolean isActive = msg.arg2 == 1;
                    if (TextUtils.equals(MiuiNetworkPolicyManagerService.this.mInterfaceName, label)) {
                        if (isActive && !MiuiNetworkPolicyManagerService.this.mLimitEnabled) {
                            MiuiNetworkPolicyManagerService.this.mHandler.removeMessages(5);
                            MiuiNetworkPolicyManagerService.this.mHandler.sendEmptyMessageDelayed(4, MiuiNetworkPolicyManagerService.this.getEnableLimitTimeout());
                            MiuiNetworkPolicyManagerService.this.updateLimit(true);
                        } else if (!isActive) {
                            MiuiNetworkPolicyManagerService.this.mHandler.removeMessages(5);
                            MiuiNetworkPolicyManagerService.this.mHandler.removeMessages(4);
                            if (MiuiNetworkPolicyManagerService.this.mLimitEnabled) {
                                MiuiNetworkPolicyManagerService.this.updateLimit(false);
                            }
                        }
                    } else if (TextUtils.equals(String.valueOf(MiuiNetworkPolicyManagerService.POWER_SAVE_IDLETIMER_LABEL), label)) {
                        if (isActive && MiuiNetworkPolicyManagerService.this.mPowerSaveEnabled) {
                            MiuiNetworkPolicyManagerService.this.mHandler.removeMessages(8);
                            MiuiNetworkPolicyManagerService.this.mHandler.sendMessageDelayed(MiuiNetworkPolicyManagerService.this.mHandler.obtainMessage(8, uid, msg.arg2), 5000L);
                        } else if (!isActive) {
                            MiuiNetworkPolicyManagerService.this.mHandler.removeMessages(8);
                            if (!MiuiNetworkPolicyManagerService.this.mPowerSaveEnabled) {
                                MiuiNetworkPolicyManagerService.this.updatePowerSaveForUidDataActivityChanged(uid, isActive);
                            }
                        }
                    }
                    return true;
                case 4:
                    if (MiuiNetworkPolicyManagerService.this.mLimitEnabled) {
                        MiuiNetworkPolicyManagerService.this.updateLimit(false);
                        MiuiNetworkPolicyManagerService.this.mHandler.sendEmptyMessageDelayed(5, MiuiNetworkPolicyManagerService.this.getDisableLimitTimeout());
                    }
                    return true;
                case 5:
                    if (!MiuiNetworkPolicyManagerService.this.mLimitEnabled) {
                        MiuiNetworkPolicyManagerService.this.updateLimit(true);
                        MiuiNetworkPolicyManagerService.this.mHandler.sendEmptyMessageDelayed(4, MiuiNetworkPolicyManagerService.this.getEnableLimitTimeout());
                    }
                    return true;
                case 6:
                    MiuiNetworkPolicyManagerService.this.calculateBandWidth();
                    MiuiNetworkPolicyManagerService.this.mHandler.sendEmptyMessageDelayed(6, 3000L);
                    return true;
                case 7:
                    int lastTrafficPolicyMode = MiuiNetworkPolicyManagerService.this.mTrafficPolicyMode;
                    MiuiNetworkPolicyManagerService.this.mTrafficPolicyMode = msg.arg1;
                    int networkPriority = MiuiNetworkPolicyManagerService.this.networkPriorityMode();
                    Log.i(MiuiNetworkPolicyManagerService.TAG, "networkPriorityMode = " + networkPriority + " mNetworkPriorityMode =" + MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode + " mWifiConnected=" + MiuiNetworkPolicyManagerService.this.mWifiConnected);
                    if (MiuiNetworkPolicyManagerService.this.mWifiConnected && networkPriority != MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode) {
                        MiuiNetworkPolicyManagerService.this.enableNetworkPriority(networkPriority);
                    } else if (lastTrafficPolicyMode != MiuiNetworkPolicyManagerService.this.mTrafficPolicyMode && MiuiNetworkPolicyManagerService.this.mNetworkBoostService != null) {
                        MiuiNetworkPolicyManagerService.this.mNetworkBoostService.onNetworkPriorityChanged(MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode, MiuiNetworkPolicyManagerService.this.mTrafficPolicyMode, MiuiNetworkPolicyManagerService.this.mThermalForceMode);
                    }
                    return true;
                case 8:
                    MiuiNetworkPolicyManagerService.this.updatePowerSaveForUidDataActivityChanged(msg.arg1, msg.arg2 == 1);
                    return true;
                case 9:
                    MiuiNetworkPolicyManagerService.this.updateMobileLatency();
                    return true;
                case 10:
                    if (msg.obj instanceof Boolean) {
                        MiuiNetworkPolicyManagerService.this.mRpsEnabled = ((Boolean) msg.obj).booleanValue();
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService.enableRps(miuiNetworkPolicyManagerService.mRpsEnabled);
                    }
                    return true;
                case 11:
                    if (MiuiNetworkPolicyManagerService.this.mWifiConnected) {
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService2 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService2.mIsMtkRouter = miuiNetworkPolicyManagerService2.checkRouterMTK();
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService3 = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService3.mConnectedApModel = miuiNetworkPolicyManagerService3.getRouterModel();
                        Log.d(MiuiNetworkPolicyManagerService.TAG, "detect again, router is mtk:" + MiuiNetworkPolicyManagerService.this.mIsMtkRouter);
                        MiuiNetworkPolicyManagerService.this.enableWmmer();
                    }
                    return true;
                case 12:
                    MiuiNetworkPolicyManagerService.this.updateRuleForMobileTc();
                    return true;
                case 13:
                    MiuiNetworkPolicyManagerService.this.setTrafficControllerForMobile(msg.arg1 == 1, msg.arg2);
                    return true;
                case 14:
                    int lastThermalForceMode = MiuiNetworkPolicyManagerService.this.mThermalForceMode;
                    MiuiNetworkPolicyManagerService.this.mThermalForceMode = msg.arg1;
                    int _networkPriority = MiuiNetworkPolicyManagerService.this.networkPriorityMode();
                    Log.i(MiuiNetworkPolicyManagerService.TAG, "thermal networkPriorityMode = " + _networkPriority + " mNetworkPriorityMode =" + MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode + " mWifiConnected=" + MiuiNetworkPolicyManagerService.this.mWifiConnected + " mThermalForceMode=" + MiuiNetworkPolicyManagerService.this.mThermalForceMode);
                    if (MiuiNetworkPolicyManagerService.this.mWifiConnected && _networkPriority != MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode) {
                        MiuiNetworkPolicyManagerService.this.enableNetworkPriority(_networkPriority);
                    } else if (lastThermalForceMode != MiuiNetworkPolicyManagerService.this.mThermalForceMode && MiuiNetworkPolicyManagerService.this.mNetworkBoostService != null) {
                        MiuiNetworkPolicyManagerService.this.mNetworkBoostService.onNetworkPriorityChanged(MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode, MiuiNetworkPolicyManagerService.this.mTrafficPolicyMode, MiuiNetworkPolicyManagerService.this.mThermalForceMode);
                    }
                    return true;
                default:
                    return false;
            }
        }
    };
    private MiuiNetworkManagementService.NetworkEventObserver mUidDataActivityObserver = new MiuiNetworkManagementService.NetworkEventObserver() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.25
        @Override // com.android.server.net.MiuiNetworkManagementService.NetworkEventObserver
        public void uidDataActivityChanged(String str, int i, boolean z, long j) {
            Log.i(MiuiNetworkPolicyManagerService.TAG, "label " + str + ", uid " + i + ", active " + z + ", tsNanos " + j);
            MiuiNetworkPolicyManagerService.this.mHandler.sendMessage(MiuiNetworkPolicyManagerService.this.mHandler.obtainMessage(3, i, z ? 1 : 0, str));
        }
    };
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.26
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int uid;
            String action = intent.getAction();
            Uri data = intent.getData();
            if (data == null) {
                return;
            }
            String packageName = data.getSchemeSpecificPart();
            if (TextUtils.isEmpty(packageName) || (uid = intent.getIntExtra("android.intent.extra.UID", -1)) == -1) {
                return;
            }
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                Log.i(MiuiNetworkPolicyManagerService.TAG, "ACTION_PACKAGE_ADDED uid = " + uid);
                if (MiuiNetworkPolicyManagerService.this.mUnRestrictAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mUnRestrictApps.add(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mLowLatencyAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mLowLatencyApps.add(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveApps.add(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.m1989$$Nest$smisMobileLatencyAllowed() && MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN != null && MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mMobileLowLatencyApps.add(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mQosUtils != null) {
                    MiuiNetworkPolicyManagerService.this.mQosUtils.updateAppPN(packageName, uid, true);
                }
                if (MiuiNetworkPolicyManagerService.this.mAppBuckets != null) {
                    MiuiNetworkPolicyManagerService.this.mAppBuckets.updateAppPN(packageName, uid, true);
                }
                if (MiuiNetworkPolicyManagerService.this.mMobileTcUtils != null) {
                    MiuiNetworkPolicyManagerService.this.mMobileTcUtils.updateAppPN(packageName, uid, true);
                    return;
                }
                return;
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                Log.i(MiuiNetworkPolicyManagerService.TAG, "ACTION_PACKAGE_REMOVED uid = " + uid);
                if (MiuiNetworkPolicyManagerService.this.mUnRestrictAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mUnRestrictApps.remove(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mLowLatencyAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mLowLatencyApps.remove(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveApps.remove(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.m1989$$Nest$smisMobileLatencyAllowed() && MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN != null && MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN.contains(packageName)) {
                    MiuiNetworkPolicyManagerService.this.mMobileLowLatencyApps.remove(Integer.valueOf(uid));
                }
                if (MiuiNetworkPolicyManagerService.this.mQosUtils != null) {
                    MiuiNetworkPolicyManagerService.this.mQosUtils.updateAppPN(packageName, uid, false);
                }
                if (MiuiNetworkPolicyManagerService.this.mAppBuckets != null) {
                    MiuiNetworkPolicyManagerService.this.mAppBuckets.updateAppPN(packageName, uid, false);
                }
                if (MiuiNetworkPolicyManagerService.this.mMobileTcUtils != null) {
                    MiuiNetworkPolicyManagerService.this.mMobileTcUtils.updateAppPN(packageName, uid, false);
                }
            }
        }
    };
    private final BroadcastReceiver mMobileNwReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.29
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            PhoneConstants.DataState state;
            String apnType = intent.getStringExtra("apnType");
            if (!NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI.equals(apnType) || MiuiNetworkPolicyManagerService.this.mConnectState == (state = Enum.valueOf(PhoneConstants.DataState.class, intent.getStringExtra("state")))) {
                return;
            }
            MiuiNetworkPolicyManagerService.this.mConnectState = state;
            Log.i(MiuiNetworkPolicyManagerService.TAG, "mMobileNwReceiver mConnectState = " + MiuiNetworkPolicyManagerService.this.mConnectState);
            if ((state == PhoneConstants.DataState.DISCONNECTED && MiuiNetworkPolicyManagerService.mMobileLatencyState == 1) || (state == PhoneConstants.DataState.CONNECTED && MiuiNetworkPolicyManagerService.mMobileLatencyState == 0)) {
                MiuiNetworkPolicyManagerService.this.mHandler.sendEmptyMessage(9);
            }
        }
    };
    private boolean mSleepModeEnter = false;
    private boolean mSleepModeEnable = false;
    private final BroadcastReceiver mSleepModeReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.32
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.miui.powerkeeper_sleep_changed".equals(action)) {
                int state = intent.getIntExtra("state", -1);
                Log.d(MiuiNetworkPolicyManagerService.SLEEPMODE_TAG, "broadcastReceiver state is " + state);
                if (state == 1 && !MiuiNetworkPolicyManagerService.this.mSleepModeEnter && !MiuiNetworkPolicyManagerService.this.mSleepModeWhitelistUids.isEmpty()) {
                    MiuiNetworkPolicyManagerService.this.mSleepModeEnter = true;
                    MiuiNetworkPolicyManagerService.this.setSleepModeWhitelistUidRules();
                    MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                    miuiNetworkPolicyManagerService.enableSleepModeChain(miuiNetworkPolicyManagerService.mSleepModeEnter);
                    return;
                }
                if (state != 1 && MiuiNetworkPolicyManagerService.this.mSleepModeEnter) {
                    MiuiNetworkPolicyManagerService.this.mSleepModeEnter = false;
                    MiuiNetworkPolicyManagerService.this.clearSleepModeWhitelistUidRules();
                    MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService2 = MiuiNetworkPolicyManagerService.this;
                    miuiNetworkPolicyManagerService2.enableSleepModeChain(miuiNetworkPolicyManagerService2.mSleepModeEnter);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                Log.d(MiuiNetworkPolicyManagerService.SLEEPMODE_TAG, "ACTION_SCREEN_ON");
                if (MiuiNetworkPolicyManagerService.this.mSleepModeEnter) {
                    MiuiNetworkPolicyManagerService.this.mSleepModeEnter = false;
                    MiuiNetworkPolicyManagerService.this.clearSleepModeWhitelistUidRules();
                    MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService3 = MiuiNetworkPolicyManagerService.this;
                    miuiNetworkPolicyManagerService3.enableSleepModeChain(miuiNetworkPolicyManagerService3.mSleepModeEnter);
                }
            }
        }
    };
    private int mThermalForceMode = 0;

    /* renamed from: -$$Nest$smisMobileLatencyAllowed, reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m1989$$Nest$smisMobileLatencyAllowed() {
        return isMobileLatencyAllowed();
    }

    public static MiuiNetworkPolicyManagerService make(Context context) {
        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = new MiuiNetworkPolicyManagerService(context);
        sSelf = miuiNetworkPolicyManagerService;
        return miuiNetworkPolicyManagerService;
    }

    public static MiuiNetworkPolicyManagerService get() {
        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = sSelf;
        if (miuiNetworkPolicyManagerService != null) {
            return miuiNetworkPolicyManagerService;
        }
        throw new RuntimeException("MiuiNetworkPolicyManagerService has not been initialized ");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableAutoForward(boolean enable) {
        if (!enable) {
            this.mNetworkManager.enableAutoForward(this.mLastIpAddress, this.mLastNetId, false);
            this.mHasAutoForward = enable;
            return;
        }
        int netId = this.mCurrentNetwork.getNetId();
        LinkProperties linkProperties = this.mConnectivityManager.getLinkProperties(this.mCurrentNetwork);
        if (linkProperties == null) {
            Log.d(TAG, "link properties is null");
            return;
        }
        List<LinkAddress> curAddr = linkProperties.getLinkAddresses();
        String iface = linkProperties.getInterfaceName();
        if (StatusManager.WLAN_IFACE_0.equals(iface) && curAddr != null && curAddr.size() > 0) {
            for (LinkAddress address : curAddr) {
                if (address.getAddress() instanceof Inet4Address) {
                    String inet4Addr = address.getAddress().getHostAddress();
                    Log.d(TAG, "enable autoforward, current address: " + inet4Addr + ", netid = " + netId);
                    this.mNetworkManager.enableAutoForward(inet4Addr, netId, true);
                    this.mLastIpAddress = inet4Addr;
                    this.mLastNetId = netId;
                    this.mHasAutoForward = enable;
                }
            }
        }
    }

    private MiuiNetworkPolicyManagerService(Context context) {
        this.mThermalStateListener = null;
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        Handler handler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mHandler = handler;
        this.mUnRestrictApps = new HashSet();
        this.mLowLatencyApps = new HashSet();
        this.mNeedRestrictPowerSaveApps = new HashSet();
        this.mMobileLowLatencyApps = new HashSet();
        this.mHistoryBandWidth = new LinkedList();
        this.mNetworkPriorityMode = NETWORK_PRIORITY_MODE_CLOSED;
        this.mTrafficPolicyMode = 0;
        this.mSupport = new MiuiNetworkPolicyServiceSupport(context, handler);
        this.mWifiNetworkRequest = new NetworkRequest.Builder().addTransportType(1).addTransportType(4).removeCapability(15).build();
        this.mVPNNetworkRequest = new NetworkRequest.Builder().addTransportType(4).removeCapability(15).build();
        this.mWifiNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                super.onAvailable(network);
                NetworkCapabilities networkCapabilities = MiuiNetworkPolicyManagerService.this.mConnectivityManager.getNetworkCapabilities(network);
                if (networkCapabilities == null) {
                    return;
                }
                boolean isCompatible = false;
                boolean isVpn = networkCapabilities.hasTransport(4);
                if (isVpn) {
                    MiuiNetworkPolicyManagerService.this.mVpnNetwork = network;
                    isCompatible = true;
                } else {
                    WifiInfo wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo();
                    if (wifiInfo != null && !wifiInfo.isPrimary()) {
                        MiuiNetworkPolicyManagerService.this.mSlaveWifiNetwork = network;
                        isCompatible = true;
                    }
                }
                if (isCompatible) {
                    if (MiuiNetworkPolicyManagerService.this.mHasAutoForward) {
                        MiuiNetworkPolicyManagerService.this.enableAutoForward(false);
                        MiuiNetworkPolicyManagerService.this.mHasAutoForward = false;
                    }
                    Log.w(MiuiNetworkPolicyManagerService.TAG, "Incompatible network, disable auto forward");
                    return;
                }
                MiuiNetworkPolicyManagerService.this.checkEnableAutoForward(networkCapabilities, network);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                MiuiNetworkPolicyManagerService.this.checkEnableAutoForward(networkCapabilities, network);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                super.onLost(network);
                if (network.equals(MiuiNetworkPolicyManagerService.this.mSlaveWifiNetwork)) {
                    MiuiNetworkPolicyManagerService.this.mSlaveWifiNetwork = null;
                    Log.v(MiuiNetworkPolicyManagerService.TAG, "Slave wifi Network is lost");
                } else if (network.equals(MiuiNetworkPolicyManagerService.this.mVpnNetwork)) {
                    MiuiNetworkPolicyManagerService.this.mVpnNetwork = null;
                    Log.v(MiuiNetworkPolicyManagerService.TAG, "vpn Network is lost");
                } else if (network.equals(MiuiNetworkPolicyManagerService.this.mCurrentNetwork) && MiuiNetworkPolicyManagerService.this.mHasAutoForward) {
                    Log.v(MiuiNetworkPolicyManagerService.TAG, "network is lost, remove autoforward rule");
                    MiuiNetworkPolicyManagerService.this.enableAutoForward(false);
                }
            }
        };
        this.mVPNNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.2
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                super.onAvailable(network);
                MiuiNetworkPolicyManagerService.this.onVpnNetworkChanged(network, true);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                super.onLost(network);
                MiuiNetworkPolicyManagerService.this.onVpnNetworkChanged(network, false);
            }
        };
        if (isQosFeatureAllowed()) {
            this.mQosUtils = new MiuiNetworkPolicyQosUtils(context, handler);
        }
        this.mAppBuckets = new MiuiNetworkPolicyAppBuckets(context, handler);
        if (isMobileTcFeatureAllowed()) {
            this.mMobileTcUtils = new MiuiNetworkPolicyTrafficLimit(context, handler);
        }
        try {
            ThermalStateListener thermalStateListener = new ThermalStateListener(THERMAL_STATE_FILE);
            this.mThermalStateListener = thermalStateListener;
            thermalStateListener.startWatching();
        } catch (Exception e) {
            Log.e(TAG, "Thremal state listener init failed:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVpnNetworkChanged(Network network, boolean add) {
        NetworkCapabilities networkCapabilities = this.mConnectivityManager.getNetworkCapabilities(network);
        if (networkCapabilities == null) {
            return;
        }
        int[] uids = networkCapabilities.getAdministratorUids();
        if (add) {
            for (int uid : uids) {
                Log.d(TAG, "listened vpn network: " + uid);
                if (uid != -1 && uid > 10000 && !this.mUnRestrictApps.contains(Integer.valueOf(uid))) {
                    this.mUnRestrictApps.add(Integer.valueOf(uid));
                    this.mNetworkManager.whiteListUid(uid, true);
                }
            }
            return;
        }
        for (int uid2 : uids) {
            Log.d(TAG, "lost vpn network: " + uid2);
            if (uid2 != -1 && uid2 > 10000 && this.mUnRestrictApps.contains(Integer.valueOf(uid2))) {
                this.mUnRestrictApps.remove(Integer.valueOf(uid2));
                this.mNetworkManager.whiteListUid(uid2, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkEnableAutoForward(NetworkCapabilities networkCapabilities, Network network) {
        boolean isCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
        if (networkCapabilities == null || isCtsMode || this.mSlaveWifiNetwork != null || this.mVpnNetwork != null) {
            return;
        }
        this.mCurrentNetwork = network;
        boolean isValidated = networkCapabilities.hasCapability(12) && networkCapabilities.hasCapability(16);
        if (!isValidated && !this.mHasAutoForward) {
            Log.d(TAG, "add auto forward rule ");
            enableAutoForward(true);
        } else if (isValidated && this.mHasAutoForward) {
            Log.d(TAG, "validation pass,  remove auto forward rule");
            enableAutoForward(false);
        }
    }

    public void systemReady() {
        this.mInterfaceName = SystemProperties.get("wifi.interface", StatusManager.WLAN_IFACE_0);
        MiuiNetworkManagementService miuiNetworkManagementService = MiuiNetworkManagementService.getInstance();
        this.mNetworkManager = miuiNetworkManagementService;
        miuiNetworkManagementService.setNetworkEventObserver(this.mUidDataActivityObserver);
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mNetworkManager.enableWmmer(false);
        this.mNetworkManager.enableLimitter(false);
        NetworkStatsReporter.make(this.mContext);
        IntentFilter wifiStateFilter = new IntentFilter("android.net.wifi.STATE_CHANGE");
        wifiStateFilter.addAction(ACTION_NETWORK_CONDITIONS_MEASURED);
        this.mContext.registerReceiver(this.mWifiStateReceiver, wifiStateFilter, null, this.mHandler, 2);
        registerWmmerEnableChangedObserver();
        registerNetworkProrityModeChangedObserver();
        networkPriorityCloudControl();
        this.mDefaultInputMethod = getDefaultInputMethod();
        registerUnRestirctAppsChangedObserver();
        registerLowLatencyAppsChangedObserver();
        registerWmmerRouterWhitelistChangedObserver();
        registerDefaultInputMethodChangedObserver();
        registerRestrictPowerSaveAppsChangedObserver();
        registerTrustMiuiAddDnsObserver();
        registerHappyEyeballsChangeObserver();
        registerP2PHCChangeObserver();
        registerSmartDnsChangeObserver();
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageReceiver, packageFilter, null, this.mHandler, 4);
        this.mSupport.registerUidObserver();
        registerWifiNetworkListener();
        if (isMobileLatencyAllowed()) {
            registerMobileLatencyAppsChangedObserver();
            IntentFilter mobileNwFilter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
            this.mContext.registerReceiver(this.mMobileNwReceiver, mobileNwFilter, null, this.mHandler, 4);
        }
        MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = this.mQosUtils;
        if (miuiNetworkPolicyQosUtils != null) {
            miuiNetworkPolicyQosUtils.systemReady(this.mNetworkManager);
        }
        MiuiNetworkPolicyAppBuckets miuiNetworkPolicyAppBuckets = this.mAppBuckets;
        if (miuiNetworkPolicyAppBuckets != null) {
            miuiNetworkPolicyAppBuckets.systemReady();
        }
        MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = this.mMobileTcUtils;
        if (miuiNetworkPolicyTrafficLimit != null) {
            miuiNetworkPolicyTrafficLimit.systemReady(this.mNetworkManager);
        }
        if (!DeviceLevel.IS_MIUI_LITE_VERSION && !DeviceLevel.IS_MIUI_GO_VERSION) {
            Log.d(TAG, "is MIUI or is not apply getMiuiLiteVersion");
            NetworkBoostService networkBoostService = NetworkBoostService.getInstance(this.mContext);
            this.mNetworkBoostService = networkBoostService;
            if (networkBoostService != null) {
                networkBoostService.onNetworkPriorityChanged(this.mNetworkPriorityMode, this.mTrafficPolicyMode, this.mThermalForceMode);
            }
        }
        registerPowerKeeperSleep();
    }

    private int getWmmForUidState(int uid, int state) {
        if (isStateWmmed(state)) {
            if (this.mLowLatencyApps.contains(Integer.valueOf(uid)) && !this.mIsCaptivePortal) {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    private boolean isStateWmmed(int state) {
        return state >= 0 && state <= 2;
    }

    private boolean isStateUnRestrictedForUid(int uid, int state) {
        return state >= 0 && (state <= 4 || (state < 19 && this.mUnRestrictApps.contains(Integer.valueOf(uid))));
    }

    private static boolean isUidValidForRules(int uid) {
        if (uid == 1013 || uid == 1019 || UserHandle.isApp(uid)) {
            return true;
        }
        return false;
    }

    private void updateWmmForUidState(int uid, int state) {
        if (this.mWmmerEnable) {
            Log.i(TAG, "updateWmmForUidState uid: " + uid + " state: " + state + " wmm: " + getWmmForUidState(uid, state));
            int market_uid = 0;
            int download_uid = 0;
            int downloadui_uid = 0;
            try {
                PackageManager pm = this.mContext.getPackageManager();
                market_uid = pm.getApplicationInfo("com.xiaomi.market", 0).uid;
                download_uid = pm.getApplicationInfo("com.android.providers.downloads", 0).uid;
                downloadui_uid = pm.getApplicationInfo("com.android.providers.downloads.ui", 0).uid;
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.mNetworkManager.updateWmm(uid, getWmmForUidState(uid, state));
            if (uid == market_uid && market_uid != 0) {
                this.mNetworkManager.updateWmm(download_uid, getWmmForUidState(uid, state));
                this.mNetworkManager.updateWmm(downloadui_uid, getWmmForUidState(uid, state));
            }
        }
    }

    private void updateLimitterForUidState(int uid, int state) {
        if (isLimitterEnabled()) {
            this.mNetworkManager.whiteListUid(uid, isStateUnRestrictedForUid(uid, state));
        }
    }

    private void updateRulesForUidStateChange(int uid, int oldUidState, int newUidState) {
        if (isUidValidForRules(uid)) {
            if (getWmmForUidState(uid, oldUidState) != getWmmForUidState(uid, newUidState)) {
                updateWmmForUidState(uid, newUidState);
            }
            if (isStateUnRestrictedForUid(uid, oldUidState) != isStateUnRestrictedForUid(uid, newUidState)) {
                updateLimitterForUidState(uid, newUidState);
            }
            if (isPowerSaveRestrictedForUid(uid, oldUidState) != isPowerSaveRestrictedForUid(uid, newUidState)) {
                updatePowerSaveStateForUidState(uid, newUidState);
            }
        }
    }

    private boolean isPowerSaveRestrictedForUid(int uid, int state) {
        return state == 2 && this.mWifiConnected && this.mNeedRestrictPowerSaveApps.contains(Integer.valueOf(uid));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerSaveForUidDataActivityChanged(int uid, boolean active) {
        int state = this.mUidState.get(uid, 19);
        boolean restrict = isPowerSaveRestrictedForUid(uid, state);
        Log.i(TAG, "update ps for data activity, uid = " + uid + ", state= " + state + ", restrict = " + restrict + ", active = " + active + ", mPS = " + this.mPowerSaveEnabled);
        if (active && restrict && this.mPowerSaveEnabled) {
            enablePowerSave(false);
        } else if (!active && !this.mPowerSaveEnabled) {
            enablePowerSave(true);
        }
    }

    private void updatePowerSaveStateForUidState(int uid, int state) {
        boolean restrict = isPowerSaveRestrictedForUid(uid, state);
        this.mNetworkManager.listenUidDataActivity(OsConstants.IPPROTO_UDP, uid, POWER_SAVE_IDLETIMER_LABEL, 2, restrict);
        enablePowerSave(!restrict);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enablePowerSave(boolean enable) {
        Log.i(TAG, "enable ps, mPS = " + this.mPowerSaveEnabled + ", enable = " + enable);
        if (this.mPowerSaveEnabled != enable) {
            this.mPowerSaveEnabled = enable;
            this.mSupport.enablePowerSave(enable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRuleGlobal() {
        int size = this.mUidState.size();
        for (int i = 0; i < size; i++) {
            int uid = this.mUidState.keyAt(i);
            int state = this.mUidState.get(uid, 19);
            Log.i(TAG, "updateRuleGlobal uid = " + uid + ", state = " + state);
            updateRulesForUidStateChange(uid, 19, state);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void calculateBandWidth() {
        long rxBytes = TrafficStats.getRxBytes(this.mInterfaceName);
        if (rxBytes < 0 || this.mLastRxBytes > rxBytes) {
            Log.i(TAG, "rxByte: " + rxBytes + ", mLastRxBytes: " + this.mLastRxBytes);
            this.mLastRxBytes = 0L;
        }
        long j = this.mLastRxBytes;
        if (j == 0 && rxBytes >= 0) {
            this.mLastRxBytes = rxBytes;
            return;
        }
        long bwBps = (rxBytes - j) / LATENCY_VALUE_L3;
        if (bwBps >= HISTORY_BANDWIDTH_MIN) {
            addHistoryBandWidth(bwBps);
        }
        Log.i(TAG, "bandwidth: " + (bwBps / 1000) + " KB/s, Max bandwidth: " + (((Long) Collections.max(this.mHistoryBandWidth)).longValue() / 1000) + " KB/s");
        this.mLastRxBytes = rxBytes;
    }

    private void addHistoryBandWidth(long bwBps) {
        if (this.mHistoryBandWidth.size() >= 10) {
            this.mHistoryBandWidth.removeLast();
        }
        this.mHistoryBandWidth.addFirst(Long.valueOf(bwBps));
    }

    private void enableBandwidthPoll(boolean enabled) {
        this.mHandler.removeMessages(6);
        this.mHistoryBandWidth.clear();
        if (enabled) {
            this.mHandler.sendEmptyMessage(6);
            addHistoryBandWidth(HISTORY_BANDWIDTH_MIN);
        }
    }

    private boolean isNetworkPrioritySupport() {
        boolean support;
        try {
            if ("mediatek".equals(FeatureParser.getString("vendor"))) {
                if (Build.IS_INTERNATIONAL_BUILD) {
                    support = this.mContext.getResources().getBoolean(this.mContext.getResources().getIdentifier("config_network_priority_mtk_global_enabled", "bool", "android.miui"));
                } else {
                    support = this.mContext.getResources().getBoolean(this.mContext.getResources().getIdentifier("config_network_priority_mtk_enabled", "bool", "android.miui"));
                }
            } else if (Build.IS_INTERNATIONAL_BUILD) {
                support = this.mContext.getResources().getBoolean(this.mContext.getResources().getIdentifier("config_network_priority_global_enabled", "bool", "android.miui"));
            } else {
                support = this.mContext.getResources().getBoolean(this.mContext.getResources().getIdentifier("config_network_priority_enabled", "bool", "android.miui"));
            }
            return support;
        } catch (Exception e) {
            Log.e(TAG, "get network priority config fail");
            return false;
        }
    }

    private String getNetworkPriorityCloudValue() {
        String cvalue;
        if ("mediatek".equals(FeatureParser.getString("vendor"))) {
            cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MTK_WIFI_TRAFFIC_PRIORITY_MODE, -2);
        } else {
            cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_NETWORK_PRIORITY_ENABLED, -2);
        }
        if (cvalue == null) {
            return "";
        }
        return cvalue;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void networkPriorityCloudControl() {
        String cvalue = getNetworkPriorityCloudValue();
        if (!isNetworkPrioritySupport()) {
            cvalue = "off";
        }
        try {
            SystemProperties.set("sys.net.support.netprio", TextUtils.equals("off", cvalue) ? "false" : "true");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set network priority support config", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int networkPriorityMode() {
        if (!isNetworkPrioritySupport()) {
            return NETWORK_PRIORITY_MODE_CLOSED;
        }
        String cvalue = getNetworkPriorityCloudValue();
        boolean isCloudUnForceClosed = !"off".equals(cvalue);
        if (!isCloudUnForceClosed) {
            return NETWORK_PRIORITY_MODE_CLOSED;
        }
        if (this.mThermalForceMode != 0) {
            Log.i(TAG, "force mThermalForceMode = " + this.mThermalForceMode);
            return 2;
        }
        int i = this.mTrafficPolicyMode;
        if (i != 0) {
            return i;
        }
        if ("on".equals(cvalue)) {
            return 1;
        }
        if (!"fast".equals(cvalue)) {
            return 0;
        }
        return 2;
    }

    private boolean isLimitterEnabled() {
        return isLimitterEnabled(this.mNetworkPriorityMode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isLimitterEnabled(int mode) {
        return mode == 1 || mode == 2;
    }

    private boolean isWmmerEnabled() {
        return this.mCloudWmmerEnable && this.mWifiConnected && (!this.mIsMtkRouter || (!TextUtils.isEmpty(this.mConnectedApModel) && this.mWmmerRouterWhitelist.contains(this.mConnectedApModel)));
    }

    private boolean validatePriorityMode(int mode) {
        return mode == 0 || mode == 1 || mode == 2 || mode == NETWORK_PRIORITY_MODE_CLOSED;
    }

    public boolean setNetworkTrafficPolicy(int mode) {
        if (!validatePriorityMode(mode)) {
            return false;
        }
        this.mHandler.removeMessages(7);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(7, mode, 0));
        return true;
    }

    public boolean setRpsStatus(boolean enable) {
        Log.d(TAG, "setRpsStatus/in [" + enable + "]");
        if (this.mRpsEnabled != enable) {
            this.mHandler.removeMessages(10);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(10, Boolean.valueOf(enable)));
            Log.d(TAG, "setRpsStatus/out [ true ]");
            return true;
        }
        Log.d(TAG, "setRpsStatus/out [ false]");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableNetworkPriority(int mode) {
        boolean isNeedUpdate = false;
        boolean wasLimitterEnabled = isLimitterEnabled();
        boolean isLimitterEnabled = isLimitterEnabled(mode);
        this.mNetworkPriorityMode = mode;
        if (mode == 1) {
            enableBandwidthPoll(true);
        } else {
            enableBandwidthPoll(false);
        }
        if (wasLimitterEnabled && !isLimitterEnabled) {
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(3, 0, 0));
            this.mNetworkManager.enableLimitter(false);
            return;
        }
        if (!wasLimitterEnabled && isLimitterEnabled) {
            this.mNetworkManager.enableLimitter(true);
            isNeedUpdate = true;
            this.mHandler.sendEmptyMessageDelayed(5, 0L);
        } else if (this.mLimitEnabled) {
            this.mHandler.removeMessages(4);
            this.mHandler.sendEmptyMessage(4);
        }
        if (isNeedUpdate) {
            updateRuleGlobal();
        }
        NetworkBoostService networkBoostService = this.mNetworkBoostService;
        if (networkBoostService != null) {
            networkBoostService.onNetworkPriorityChanged(this.mNetworkPriorityMode, this.mTrafficPolicyMode, this.mThermalForceMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableWmmer() {
        enableWmmer(isWmmerEnabled());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableWmmer(boolean enable) {
        if (this.mWmmerEnable != enable) {
            this.mNetworkManager.enableWmmer(enable);
            if (enable) {
                updateRuleGlobal();
            }
            this.mWmmerEnable = enable;
        }
    }

    private boolean isAutoForwardEnabled() {
        String cvalue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_AUTO_FORWARD_ENABLED, -2);
        return cvalue != null && "on".equals(cvalue);
    }

    private void registerWmmerEnableChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                String value;
                if ("mediatek".equals(FeatureParser.getString("vendor"))) {
                    value = Settings.System.getStringForUser(MiuiNetworkPolicyManagerService.this.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_MTK_WMMER_ENABLED, -2);
                } else {
                    value = Settings.System.getStringForUser(MiuiNetworkPolicyManagerService.this.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_WMMER_ENABLED, -2);
                }
                MiuiNetworkPolicyManagerService.this.mCloudWmmerEnable = value != null && "on".equals(value);
                Log.d(MiuiNetworkPolicyManagerService.TAG, " wmmer value:" + value + " mCloudWmmerEnable:" + MiuiNetworkPolicyManagerService.this.mCloudWmmerEnable);
                MiuiNetworkPolicyManagerService.this.enableWmmer();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_WMMER_ENABLED), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MTK_WMMER_ENABLED), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerWifiNetworkListener() {
        if (isAutoForwardEnabled() && !this.mHasListenWifiNetwork) {
            this.mConnectivityManager.registerNetworkCallback(this.mWifiNetworkRequest, this.mWifiNetworkCallback);
            this.mHasListenWifiNetwork = true;
        } else if (!isAutoForwardEnabled() && this.mHasListenWifiNetwork) {
            this.mConnectivityManager.unregisterNetworkCallback(this.mWifiNetworkCallback);
            if (this.mHasAutoForward) {
                Log.d(TAG, "cloud control is disabled, remove autoforward rule");
                enableAutoForward(false);
            }
            this.mHasListenWifiNetwork = false;
        }
    }

    private void registerTrustMiuiAddDnsObserver() {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MiuiNetworkPolicyManagerService.CLOUD_NETWORK_TRUST_MIUI_ADD_DNS))) {
                    String trustMiuiAddDns = Settings.System.getString(MiuiNetworkPolicyManagerService.this.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_NETWORK_TRUST_MIUI_ADD_DNS);
                    String sysProVal = "v1".equals(trustMiuiAddDns) ? "1" : "0";
                    try {
                        SystemProperties.set(MiuiNetworkPolicyManagerService.PERSIST_DEVICE_CONFIG_NETD_VALUE, sysProVal);
                        Log.d(MiuiNetworkPolicyManagerService.TAG, "trust miui add dns: " + sysProVal);
                    } catch (Exception e) {
                        Log.e(MiuiNetworkPolicyManagerService.TAG, "set system properties error:" + e);
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_NETWORK_TRUST_MIUI_ADD_DNS), false, observer, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void happyEyeballsEnableCloudControl() {
        String enableValue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_DNS_HAPPY_EYEBALLS_ENABLED, -2);
        try {
            SystemProperties.set(PERSIST_DEVICE_CONFIG_NETD_ENABLED_VALUE, TextUtils.equals("on", enableValue) ? "1" : "0");
            Log.d(TAG, "happy eyeballs enabled = " + enableValue);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set happy eyeballs cloud support config", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void happyEyeballsMasterServerPriorityTimeCloudControl() {
        String timeValue = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_DNS_HAPPY_EYEBALLS_PRIORITY_TIME, -2);
        if (timeValue != null && timeValue != "") {
            try {
                SystemProperties.set(PERSIST_DEVICE_CONFIG_NETD_PRIORITY_TIME, timeValue);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set happy eyeballs master server time", e);
                return;
            }
        }
        Log.d(TAG, "happy eyeballs master server time = " + timeValue);
    }

    private void registerHappyEyeballsChangeObserver() {
        Log.d(TAG, "happy eyeballs observer register");
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiNetworkPolicyManagerService.this.happyEyeballsEnableCloudControl();
                MiuiNetworkPolicyManagerService.this.happyEyeballsMasterServerPriorityTimeCloudControl();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_DNS_HAPPY_EYEBALLS_ENABLED), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_DNS_HAPPY_EYEBALLS_PRIORITY_TIME), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.7
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    private void registerSmartDnsChangeObserver() {
        Log.d(TAG, "SmartDns observer register");
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                String enableValue = Settings.System.getStringForUser(MiuiNetworkPolicyManagerService.this.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_SMARTDNS_ENABLED, -2);
                try {
                    SystemProperties.set(MiuiNetworkPolicyManagerService.PERSIST_DEVICE_CONFIG_NETD_SMARTDNS_ENABLED_VALUE, TextUtils.equals("on", enableValue) ? "1" : "0");
                    Log.d(MiuiNetworkPolicyManagerService.TAG, "smartdns enabled = " + enableValue);
                } catch (Exception e) {
                    Log.e(MiuiNetworkPolicyManagerService.TAG, "Failed to set smartdns support config", e);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_SMARTDNS_ENABLED), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.9
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    private void registerP2PHCChangeObserver() {
        Log.d(TAG, "p2phc observer register");
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.10
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                String stringForUser = Settings.System.getStringForUser(MiuiNetworkPolicyManagerService.this.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_P2PHC_ENABLED, -2);
                try {
                    MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                    miuiNetworkPolicyManagerService.mMiuiWifiManager = (MiuiWifiManager) miuiNetworkPolicyManagerService.mContext.getSystemService("MiuiWifiService");
                } catch (Exception e) {
                    Log.e(MiuiNetworkPolicyManagerService.TAG, "Failed to set p2phc cloud support config", e);
                }
                if (MiuiNetworkPolicyManagerService.this.mMiuiWifiManager != null) {
                    MiuiNetworkPolicyManagerService.this.mMiuiWifiManager.setP2PHCEnable(TextUtils.equals("on", stringForUser) ? 1 : 0);
                    try {
                        SystemProperties.set(MiuiNetworkPolicyManagerService.PERSIST_WPA_SUPPLICANT_P2PHC_ENABLE, TextUtils.equals("on", stringForUser) ? "1" : "0");
                    } catch (Exception e2) {
                        Log.e(MiuiNetworkPolicyManagerService.TAG, "Failed to set p2phc_enable config", e2);
                    }
                    Log.d(MiuiNetworkPolicyManagerService.TAG, "p2phc enabled = " + stringForUser);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_P2PHC_ENABLED), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.11
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    private void registerNetworkProrityModeChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.12
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && (uri.equals(Settings.System.getUriFor(MiuiNetworkPolicyManagerService.CLOUD_NETWORK_PRIORITY_ENABLED)) || uri.equals(Settings.System.getUriFor(MiuiNetworkPolicyManagerService.CLOUD_MTK_WIFI_TRAFFIC_PRIORITY_MODE)))) {
                    MiuiNetworkPolicyManagerService.this.networkPriorityCloudControl();
                }
                int networkPriority = MiuiNetworkPolicyManagerService.this.networkPriorityMode();
                Log.i(MiuiNetworkPolicyManagerService.TAG, "networkPriorityMode = " + networkPriority + " mNetworkPriorityMode =" + MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode + " mWifiConnected=" + MiuiNetworkPolicyManagerService.this.mWifiConnected);
                if (networkPriority != MiuiNetworkPolicyManagerService.this.mNetworkPriorityMode && MiuiNetworkPolicyManagerService.this.mWifiConnected) {
                    MiuiNetworkPolicyManagerService.this.enableNetworkPriority(networkPriority);
                }
                MiuiNetworkPolicyManagerService.this.registerWifiNetworkListener();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_NETWORK_PRIORITY_ENABLED), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MTK_WIFI_TRAFFIC_PRIORITY_MODE), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_AUTO_FORWARD_ENABLED), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.13
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
        this.mConnectivityManager.registerNetworkCallback(this.mVPNNetworkRequest, this.mVPNNetworkCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkRouterMTK() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String bssid = wifiInfo.getBSSID();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        if (bssid == null || scanResults.isEmpty()) {
            long j = this.ROUTER_DETECT_TIME;
            if (j < MAX_ROUTER_DETECT_TIME) {
                this.mHandler.sendEmptyMessageDelayed(11, j);
                this.ROUTER_DETECT_TIME *= 2;
                return true;
            }
        }
        this.ROUTER_DETECT_TIME = 10000L;
        for (ScanResult scanResult : scanResults) {
            if (TextUtils.equals(bssid, scanResult.BSSID) && checkIEMTK(scanResult.getInformationElements())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIEMTK(List<ScanResult.InformationElement> infoElements) {
        if (infoElements == null || infoElements.size() == 0) {
            return true;
        }
        Iterator<ScanResult.InformationElement> it = infoElements.iterator();
        while (true) {
            if (!it.hasNext()) {
                return false;
            }
            ScanResult.InformationElement ie = it.next();
            if (ie.getId() == EID_VSA) {
                byte[] oui = new byte[3];
                try {
                    ie.getBytes().get(oui, 0, 3);
                    boolean isMtk = Arrays.equals(oui, MTK_OUI1) || Arrays.equals(oui, MTK_OUI2) || Arrays.equals(oui, MTK_OUI3) || Arrays.equals(oui, MTK_OUI4);
                    if (isMtk) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set network priority support config", e);
                    return false;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUidState(int uid, int uidState) {
        Log.i(TAG, "updateUidState uid = " + uid + ", uidState = " + uidState);
        int oldUidState = this.mUidState.get(uid, 19);
        if (oldUidState != uidState) {
            this.mUidState.put(uid, uidState);
            updateRulesForUidStateChange(uid, oldUidState, uidState);
            if (isMobileLatencyAllowed()) {
                updateMobileLatencyForUidStateChange(uid, oldUidState, uidState);
            }
            MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = this.mQosUtils;
            if (miuiNetworkPolicyQosUtils != null) {
                miuiNetworkPolicyQosUtils.updateQosForUidStateChange(uid, oldUidState, uidState);
            }
            MiuiNetworkPolicyAppBuckets miuiNetworkPolicyAppBuckets = this.mAppBuckets;
            if (miuiNetworkPolicyAppBuckets != null) {
                miuiNetworkPolicyAppBuckets.updateAppBucketsForUidStateChange(uid, oldUidState, uidState);
            }
            MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = this.mMobileTcUtils;
            if (miuiNetworkPolicyTrafficLimit != null) {
                miuiNetworkPolicyTrafficLimit.updateRulesForUidStateChange(uid, oldUidState, uidState);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeUidState(int uid) {
        Log.i(TAG, "removeUidState uid = " + uid);
        int index = this.mUidState.indexOfKey(uid);
        if (index >= 0) {
            int oldUidState = this.mUidState.valueAt(index);
            this.mUidState.removeAt(index);
            if (oldUidState != 19) {
                updateRulesForUidStateChange(uid, oldUidState, 19);
                if (isMobileLatencyAllowed()) {
                    updateMobileLatencyForUidStateChange(uid, oldUidState, 19);
                }
                MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = this.mQosUtils;
                if (miuiNetworkPolicyQosUtils != null) {
                    miuiNetworkPolicyQosUtils.updateQosForUidStateChange(uid, oldUidState, 19);
                }
                MiuiNetworkPolicyAppBuckets miuiNetworkPolicyAppBuckets = this.mAppBuckets;
                if (miuiNetworkPolicyAppBuckets != null) {
                    miuiNetworkPolicyAppBuckets.updateAppBucketsForUidStateChange(uid, oldUidState, 19);
                }
                MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = this.mMobileTcUtils;
                if (miuiNetworkPolicyTrafficLimit != null) {
                    miuiNetworkPolicyTrafficLimit.updateRulesForUidStateChange(uid, oldUidState, 19);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Set<String> getUnRestrictedApps(Context context) {
        String whiteString = Settings.System.getStringForUser(context.getContentResolver(), NETWORK_PRIORITY_WHITELIST, -2);
        Set<String> whiteList = new HashSet<>();
        if (!TextUtils.isEmpty(whiteString)) {
            String[] packages = whiteString.split(",");
            if (packages != null) {
                for (String str : packages) {
                    whiteList.add(str);
                }
            }
        } else {
            int i = 0;
            while (true) {
                String[] strArr = LOCAL_NETWORK_PRIORITY_WHITELIST;
                if (i >= strArr.length) {
                    break;
                }
                whiteList.add(strArr[i]);
                i++;
            }
        }
        return whiteList;
    }

    private void registerRestrictPowerSaveAppsChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.15
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                UserManager um = (UserManager) MiuiNetworkPolicyManagerService.this.mContext.getSystemService("user");
                PackageManager pm = MiuiNetworkPolicyManagerService.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                miuiNetworkPolicyManagerService.mNeedRestrictPowerSaveAppsPN = miuiNetworkPolicyManagerService.getNeedRestrictPowerSaveApps(miuiNetworkPolicyManagerService.mContext);
                MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveApps.clear();
                if (!MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveAppsPN.isEmpty()) {
                    for (UserInfo user : users) {
                        List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                        for (PackageInfo app : apps) {
                            if (app.packageName != null && app.applicationInfo != null && MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveAppsPN.contains(app.packageName)) {
                                int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                MiuiNetworkPolicyManagerService.this.mNeedRestrictPowerSaveApps.add(Integer.valueOf(uid));
                            }
                        }
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_RESTRICT_WIFI_POWERSAVE_APPLIST), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.16
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Set<String> getNeedRestrictPowerSaveApps(Context context) {
        String[] packages;
        String appString = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_RESTRICT_WIFI_POWERSAVE_APPLIST, -2);
        Set<String> appList = new HashSet<>();
        if (!TextUtils.isEmpty(appString) && (packages = appString.split(",")) != null) {
            for (String str : packages) {
                appList.add(str);
            }
        }
        return appList;
    }

    private void registerUnRestirctAppsChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.17
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                UserManager um = (UserManager) MiuiNetworkPolicyManagerService.this.mContext.getSystemService("user");
                PackageManager pm = MiuiNetworkPolicyManagerService.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                miuiNetworkPolicyManagerService.mUnRestrictAppsPN = miuiNetworkPolicyManagerService.getUnRestrictedApps(miuiNetworkPolicyManagerService.mContext);
                MiuiNetworkPolicyManagerService.this.mUnRestrictApps.clear();
                if (!MiuiNetworkPolicyManagerService.this.mDefaultInputMethod.equals("")) {
                    MiuiNetworkPolicyManagerService.this.mUnRestrictAppsPN.add(MiuiNetworkPolicyManagerService.this.mDefaultInputMethod);
                }
                if (!MiuiNetworkPolicyManagerService.this.mUnRestrictAppsPN.isEmpty()) {
                    for (UserInfo user : users) {
                        List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                        for (PackageInfo app : apps) {
                            if (app.packageName != null && app.applicationInfo != null && MiuiNetworkPolicyManagerService.this.mUnRestrictAppsPN.contains(app.packageName)) {
                                int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                MiuiNetworkPolicyManagerService.this.mUnRestrictApps.add(Integer.valueOf(uid));
                            }
                        }
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(NETWORK_PRIORITY_WHITELIST), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.18
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDefaultInputMethod() {
        ComponentName inputMethodCptName;
        String inputMethodPkgName;
        String inputMethod = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        return (inputMethod == null || (inputMethodCptName = ComponentName.unflattenFromString(inputMethod)) == null || (inputMethodPkgName = inputMethodCptName.getPackageName()) == null) ? "" : inputMethodPkgName;
    }

    private void registerDefaultInputMethodChangedObserver() {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.19
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                UserManager um = (UserManager) MiuiNetworkPolicyManagerService.this.mContext.getSystemService("user");
                PackageManager pm = MiuiNetworkPolicyManagerService.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                String inputMethodPkgName = MiuiNetworkPolicyManagerService.this.getDefaultInputMethod();
                if (!inputMethodPkgName.equals("") && !inputMethodPkgName.equals(MiuiNetworkPolicyManagerService.this.mDefaultInputMethod)) {
                    for (UserInfo user : users) {
                        List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                        for (PackageInfo app : apps) {
                            if (app.packageName != null && app.applicationInfo != null) {
                                int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                if (app.packageName.equals(MiuiNetworkPolicyManagerService.this.mDefaultInputMethod)) {
                                    MiuiNetworkPolicyManagerService.this.mUnRestrictApps.remove(Integer.valueOf(uid));
                                    MiuiNetworkPolicyManagerService.this.mNetworkManager.whiteListUid(uid, false);
                                } else if (app.packageName.equals(inputMethodPkgName)) {
                                    MiuiNetworkPolicyManagerService.this.mUnRestrictApps.add(Integer.valueOf(uid));
                                    MiuiNetworkPolicyManagerService.this.mNetworkManager.whiteListUid(uid, true);
                                }
                            }
                        }
                    }
                    MiuiNetworkPolicyManagerService.this.mDefaultInputMethod = inputMethodPkgName;
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, observer, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Set<String> getLowLatencyApps(Context context) {
        String[] packages;
        String whiteString = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_LOW_LATENCY_WHITELIST, -2);
        Set<String> whiteList = new HashSet<>();
        if (!TextUtils.isEmpty(whiteString) && (packages = whiteString.split(",")) != null) {
            for (String str : packages) {
                whiteList.add(str);
            }
        }
        return whiteList;
    }

    private void registerLowLatencyAppsChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.20
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                UserManager um = (UserManager) MiuiNetworkPolicyManagerService.this.mContext.getSystemService("user");
                PackageManager pm = MiuiNetworkPolicyManagerService.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                miuiNetworkPolicyManagerService.mLowLatencyAppsPN = miuiNetworkPolicyManagerService.getLowLatencyApps(miuiNetworkPolicyManagerService.mContext);
                MiuiNetworkPolicyManagerService.this.mLowLatencyApps.clear();
                if (!MiuiNetworkPolicyManagerService.this.mLowLatencyAppsPN.isEmpty()) {
                    for (UserInfo user : users) {
                        List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                        for (PackageInfo app : apps) {
                            if (app.packageName != null && app.applicationInfo != null && MiuiNetworkPolicyManagerService.this.mLowLatencyAppsPN.contains(app.packageName)) {
                                int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                MiuiNetworkPolicyManagerService.this.mLowLatencyApps.add(Integer.valueOf(uid));
                            }
                        }
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_LOW_LATENCY_WHITELIST), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.21
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    private void registerMiuiOptimizationChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.22
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean isCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
                int networkPriority = MiuiNetworkPolicyManagerService.this.networkPriorityMode();
                Log.i(MiuiNetworkPolicyManagerService.TAG, "miui optimization mode changed: " + isCtsMode + ", current network priority: " + networkPriority);
                if (!isCtsMode) {
                    MiuiNetworkPolicyManagerService.this.mNetworkManager.enableIptablesRestore(true);
                    MiuiNetworkPolicyManagerService.this.enableWmmer();
                    if (MiuiNetworkPolicyManagerService.this.mWifiConnected && networkPriority != MiuiNetworkPolicyManagerService.NETWORK_PRIORITY_MODE_CLOSED) {
                        MiuiNetworkPolicyManagerService.this.enableNetworkPriority(networkPriority);
                        return;
                    }
                    return;
                }
                MiuiNetworkPolicyManagerService.this.enableNetworkPriority(MiuiNetworkPolicyManagerService.NETWORK_PRIORITY_MODE_CLOSED);
                MiuiNetworkPolicyManagerService.this.enableWmmer(false);
                MiuiNetworkPolicyManagerService.this.mNetworkManager.enableIptablesRestore(false);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.23
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLimit(boolean enabled) {
        Log.d(TAG, "updateLimit mLimitEnabled=" + this.mLimitEnabled + ",enabled=" + enabled + ",mNetworkPriorityMode=" + this.mNetworkPriorityMode + ",mThermalForceMode=" + this.mThermalForceMode);
        if (this.mLimitEnabled != enabled) {
            long bwBps = 0;
            if (enabled && this.mNetworkPriorityMode == 1) {
                long bwBps2 = HISTORY_BANDWIDTH_MIN;
                if (this.mHistoryBandWidth.size() > 0) {
                    bwBps2 = ((Long) Collections.max(this.mHistoryBandWidth)).longValue();
                }
                long fgBw = Math.min((80 * bwBps2) / 100, FG_MAX_BANDWIDTH);
                bwBps = Math.max(bwBps2 - fgBw, BG_MIN_BANDWIDTH);
            }
            this.mNetworkManager.setLimit(enabled, bwBps);
            this.mLimitEnabled = enabled;
        }
    }

    private void registerMobileLatencyAppsChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.27
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                UserManager um = (UserManager) MiuiNetworkPolicyManagerService.this.mContext.getSystemService("user");
                PackageManager pm = MiuiNetworkPolicyManagerService.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                miuiNetworkPolicyManagerService.mMobileLowLatencyAppsPN = miuiNetworkPolicyManagerService.getMobileLowLatencyApps(miuiNetworkPolicyManagerService.mContext);
                MiuiNetworkPolicyManagerService.this.mMobileLowLatencyApps.clear();
                if (!MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN.isEmpty()) {
                    for (UserInfo user : users) {
                        List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                        for (PackageInfo app : apps) {
                            if (app.packageName != null && app.applicationInfo != null && MiuiNetworkPolicyManagerService.this.mMobileLowLatencyAppsPN.contains(app.packageName)) {
                                int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                MiuiNetworkPolicyManagerService.this.mMobileLowLatencyApps.add(Integer.valueOf(uid));
                            }
                        }
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_LOW_LATENCY_APPLIST_FOR_MOBILE), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.28
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Set<String> getMobileLowLatencyApps(Context context) {
        String[] packages;
        String appString = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_LOW_LATENCY_APPLIST_FOR_MOBILE, -2);
        Log.i(TAG, "getMobileLowLatencyApps appString=" + appString);
        Set<String> appList = new HashSet<>();
        if (!TextUtils.isEmpty(appString) && (packages = appString.split(",")) != null) {
            for (String str : packages) {
                appList.add(str);
            }
        }
        return appList;
    }

    private boolean isMobileLatencyEnabledForUid(int uid, int state) {
        Log.i(TAG, "isMLEnabled state:" + state + ",uid:" + uid + ",connect:" + this.mConnectState);
        return state == 2 && this.mConnectState == PhoneConstants.DataState.CONNECTED && this.mMobileLowLatencyApps.contains(Integer.valueOf(uid));
    }

    private void updateMobileLatencyStateForUidState(int uid, int state) {
        boolean enabled = isMobileLatencyEnabledForUid(uid, state);
        Log.i(TAG, "updateMobileLowLatencyStateForUidState enabled = " + enabled);
        enableMobileLowLatency(enabled);
    }

    private void enableMobileLowLatency(boolean z) {
        Log.i(TAG, "enableMobileLowLatency enable = " + z + ",mMobileLatencyState=" + mMobileLatencyState);
        if (mMobileLatencyState != z) {
            mMobileLatencyState = z ? 1 : 0;
            Intent intent = new Intent();
            intent.setAction(LATENCY_ACTION_CHANGE_LEVEL);
            intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
            intent.putExtra(LATENCY_KEY_RAT_TYPE, 0L);
            intent.putExtra(LATENCY_KEY_LEVEL_UL, z ? 4L : 1L);
            intent.putExtra(LATENCY_KEY_LEVEL_DL, z ? 4L : 1L);
            this.mContext.sendBroadcast(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMobileLatency() {
        int size = this.mUidState.size();
        for (int i = 0; i < size; i++) {
            int uid = this.mUidState.keyAt(i);
            int state = this.mUidState.get(uid, 19);
            Log.i(TAG, "updateMobileLatency uid = " + uid + ", state = " + state);
            if (isUidValidForRules(uid) && state == 2 && this.mMobileLowLatencyApps.contains(Integer.valueOf(uid))) {
                updateMobileLatencyStateForUidState(uid, state);
            }
        }
    }

    private void updateMobileLatencyForUidStateChange(int uid, int oldUidState, int newUidState) {
        Log.i(TAG, "updateMLUid uid:" + uid + ",old:" + oldUidState + ",new:" + newUidState);
        if (isUidValidForRules(uid) && isMobileLatencyEnabledForUid(uid, oldUidState) != isMobileLatencyEnabledForUid(uid, newUidState)) {
            updateMobileLatencyStateForUidState(uid, newUidState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableRps(boolean enable) {
        String iface = this.mSupport.updateIface(this.mInterfaceName);
        Log.d(TAG, "enableRps interface = " + iface + "enable = " + enable);
        if (iface != null) {
            this.mNetworkManager.enableRps(this.mInterfaceName, enable);
        }
    }

    private static boolean isMobileLatencyAllowed() {
        return false;
    }

    private static boolean isQosFeatureAllowed() {
        boolean rst = (Build.IS_INTERNATIONAL_BUILD || "andromeda".equals(android.os.Build.DEVICE)) ? false : true;
        Log.d(TAG, "isQosFeatureAllowed rst=" + rst);
        return rst;
    }

    public static boolean isMobileTcFeatureAllowed() {
        return !Build.IS_INTERNATIONAL_BUILD;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRuleForMobileTc() {
        int size = this.mUidState.size();
        for (int i = 0; i < size; i++) {
            int uid = this.mUidState.keyAt(i);
            int state = this.mUidState.get(uid, 19);
            Log.i(TAG, "updateRuleForMobileTc uid = " + uid + ", state = " + state);
            MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = this.mMobileTcUtils;
            if (miuiNetworkPolicyTrafficLimit != null) {
                miuiNetworkPolicyTrafficLimit.updateRulesForUidStateChange(uid, 19, state);
            }
        }
    }

    public int setTrafficControllerForMobile(boolean enable, long rate) {
        Log.d(TAG, "setTrafficControllerForMobile enable=" + enable + "rate = " + rate);
        MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = this.mMobileTcUtils;
        if (miuiNetworkPolicyTrafficLimit == null) {
            Log.d(TAG, "setTrafficControllerForMobile unsupport device!!!");
            return -1;
        }
        int rst = miuiNetworkPolicyTrafficLimit.setMobileTrafficLimitForGame(enable, rate);
        Log.d(TAG, "setTrafficControllerForMobile rst = " + rst);
        return rst;
    }

    public boolean setMiuiSlmBpfUid(int uid) {
        if (uid != 0) {
            this.mNetworkManager.setMiuiSlmBpfUid(uid);
            Log.d(TAG, "setMiuiSlmBpfUid uid = " + uid);
            return true;
        }
        return false;
    }

    public long getMiuiSlmVoipUdpAddress(int uid) {
        if (uid != 0) {
            long address = this.mNetworkManager.getMiuiSlmVoipUdpAddress(uid);
            Log.d(TAG, "getMiuiSlmVoipUdpAddress uid = " + uid + " address = " + address);
            return address;
        }
        return 0L;
    }

    public int getMiuiSlmVoipUdpPort(int uid) {
        if (uid != 0) {
            int ports = this.mNetworkManager.getMiuiSlmVoipUdpPort(uid);
            Log.d(TAG, "getMiuiSlmVoipUdpPort uid = " + uid + " ports = " + ports);
            return ports;
        }
        return 0;
    }

    public long getShareStats(int type) {
        return this.mNetworkManager.getShareStats(type);
    }

    private void registerWmmerRouterWhitelistChangedObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.30
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                try {
                    MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                    miuiNetworkPolicyManagerService.updateRouterWhitelist(Settings.System.getStringForUser(miuiNetworkPolicyManagerService.mContext.getContentResolver(), MiuiNetworkPolicyManagerService.CLOUD_WMMER_ROUTER_WHITELIST, -2));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_WMMER_ROUTER_WHITELIST), false, observer, -2);
    }

    void updateRouterWhitelist(String whitelist) {
        try {
            if (TextUtils.isEmpty(whitelist)) {
                Log.e(TAG, "RT whitelist is empty.");
                return;
            }
            String[] temp = whitelist.split(",");
            this.mWmmerRouterWhitelist.clear();
            for (int i = 0; i < temp.length; i++) {
                this.mWmmerRouterWhitelist.add(temp[i]);
                Log.i(TAG, "update RT " + temp[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0057, code lost:
    
        r5 = new byte[r8.getBytes().remaining()];
        r8.getBytes().get(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x006c, code lost:
    
        r3 = parseModelName(r5);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.String getRouterModel() {
        /*
            r11 = this;
            android.content.Context r0 = r11.mContext
            java.lang.String r1 = "wifi"
            java.lang.Object r0 = r0.getSystemService(r1)
            android.net.wifi.WifiManager r0 = (android.net.wifi.WifiManager) r0
            android.net.wifi.WifiInfo r1 = r0.getConnectionInfo()
            java.util.List r2 = r1.getInformationElements()
            java.lang.String r3 = ""
            java.lang.String r4 = "MiuiNetworkPolicy"
            if (r2 == 0) goto L8e
            int r5 = r2.size()
            if (r5 != 0) goto L20
            goto L8e
        L20:
            r5 = 4
            byte[] r6 = new byte[r5]     // Catch: java.lang.Exception -> L87
            java.util.Iterator r7 = r2.iterator()     // Catch: java.lang.Exception -> L87
        L27:
            boolean r8 = r7.hasNext()     // Catch: java.lang.Exception -> L87
            if (r8 == 0) goto L6f
            java.lang.Object r8 = r7.next()     // Catch: java.lang.Exception -> L87
            android.net.wifi.ScanResult$InformationElement r8 = (android.net.wifi.ScanResult.InformationElement) r8     // Catch: java.lang.Exception -> L87
            if (r8 == 0) goto L6e
            int r9 = r8.getId()     // Catch: java.lang.Exception -> L87
            r10 = 221(0xdd, float:3.1E-43)
            if (r9 != r10) goto L6e
            java.nio.ByteBuffer r9 = r8.getBytes()     // Catch: java.lang.Exception -> L87
            r10 = 0
            r9.get(r6, r10, r5)     // Catch: java.lang.Exception -> L87
            byte[] r9 = com.android.server.net.MiuiNetworkPolicyManagerService.WPA2_AKM_PSK_IE     // Catch: java.lang.Exception -> L87
            boolean r9 = java.util.Arrays.equals(r6, r9)     // Catch: java.lang.Exception -> L87
            if (r9 == 0) goto L6e
            java.nio.ByteBuffer r9 = r8.getBytes()     // Catch: java.lang.Exception -> L87
            boolean r9 = r9.hasRemaining()     // Catch: java.lang.Exception -> L87
            if (r9 == 0) goto L6e
            java.nio.ByteBuffer r5 = r8.getBytes()     // Catch: java.lang.Exception -> L87
            int r5 = r5.remaining()     // Catch: java.lang.Exception -> L87
            byte[] r5 = new byte[r5]     // Catch: java.lang.Exception -> L87
            java.nio.ByteBuffer r7 = r8.getBytes()     // Catch: java.lang.Exception -> L87
            r7.get(r5)     // Catch: java.lang.Exception -> L87
            java.lang.String r7 = r11.parseModelName(r5)     // Catch: java.lang.Exception -> L87
            r3 = r7
            goto L6f
        L6e:
            goto L27
        L6f:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "getRouterModel = "
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.StringBuilder r5 = r5.append(r3)
            java.lang.String r5 = r5.toString()
            android.util.Log.i(r4, r5)
            return r3
        L87:
            r5 = move-exception
            java.lang.String r6 = "getRouterName Exception"
            android.util.Log.e(r4, r6)
            return r3
        L8e:
            java.lang.String r5 = "getRouterModel InformationElement empty"
            android.util.Log.e(r4, r5)
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.MiuiNetworkPolicyManagerService.getRouterModel():java.lang.String");
    }

    private String parseModelName(byte[] bytes) {
        try {
            ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            byte[] flag = new byte[4];
            data.get(flag);
            byte[] id = new byte[2];
            byte[] len = new byte[2];
            while (data.remaining() > 4) {
                data.get(id);
                data.get(len);
                byte[] name = new byte[((len[0] & NETWORK_PRIORITY_MODE_CLOSED) << 8) | (len[1] & NETWORK_PRIORITY_MODE_CLOSED)];
                data.get(name);
                if (Arrays.equals(id, MODEL_NAME)) {
                    String modelName = bytesToString(name);
                    return modelName;
                }
            }
            return "";
        } catch (Exception e) {
            Log.e(TAG, "parseModelName Exception");
            return "";
        }
    }

    private String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes);
    }

    public boolean onSleepModeWhitelistChange(final int appId, boolean added) {
        Log.i(SLEEPMODE_TAG, "onSleepModeWhitelistChange appId:" + appId);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.31
            @Override // java.lang.Runnable
            public void run() {
                List<UserInfo> users = MiuiNetworkPolicyManagerService.this.mUserManager.getUsers();
                for (int ui = users.size() - 1; ui >= 0; ui--) {
                    UserInfo user = users.get(ui);
                    int uid = UserHandle.getUid(user.id, appId);
                    MiuiNetworkPolicyManagerService.this.mSleepModeWhitelistUids.add(Integer.valueOf(uid));
                }
            }
        });
        return true;
    }

    private void registerPowerKeeperSleep() {
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mSleepModeWhitelistUids = new HashSet();
        registerSleepModeEnableControlObserver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerSleepModeReceiver() {
        IntentFilter sleepModeFilter = new IntentFilter();
        sleepModeFilter.addAction("com.miui.powerkeeper_sleep_changed");
        sleepModeFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.mSleepModeReceiver, sleepModeFilter, null, this.mHandler, 2);
        Log.d(SLEEPMODE_TAG, "registerSleepModeReceiver");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterSleepModeReceiver() {
        BroadcastReceiver broadcastReceiver = this.mSleepModeReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            Log.d(SLEEPMODE_TAG, "unregisterSleepModeReceiver");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean sleepModeEnableControl() {
        int earthQuack = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), EARTHQUAKE_WARNING_FLAG, 0, -2);
        String cloudEnable = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_SLEEPMODE_NETWORKPOLICY_ENABLED, 0);
        Log.d(SLEEPMODE_TAG, "sleepModeEnableControl earthQuack:" + earthQuack + " cloudEnable:" + cloudEnable);
        return TextUtils.equals("on", cloudEnable) && earthQuack == 1;
    }

    private void registerSleepModeEnableControlObserver() {
        Log.d(SLEEPMODE_TAG, "registerSleepModeEnableControlObserver");
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.33
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                if (MiuiNetworkPolicyManagerService.this.sleepModeEnableControl() && !MiuiNetworkPolicyManagerService.this.mSleepModeEnable) {
                    MiuiNetworkPolicyManagerService.this.registerSleepModeReceiver();
                    MiuiNetworkPolicyManagerService.this.mSleepModeEnable = true;
                    Log.d(MiuiNetworkPolicyManagerService.SLEEPMODE_TAG, "Sleep mode receiver register");
                } else if (MiuiNetworkPolicyManagerService.this.mSleepModeEnable) {
                    MiuiNetworkPolicyManagerService.this.unregisterSleepModeReceiver();
                    MiuiNetworkPolicyManagerService.this.mSleepModeEnable = false;
                    if (MiuiNetworkPolicyManagerService.this.mSleepModeEnter) {
                        MiuiNetworkPolicyManagerService.this.mSleepModeEnter = false;
                        MiuiNetworkPolicyManagerService.this.clearSleepModeWhitelistUidRules();
                        MiuiNetworkPolicyManagerService miuiNetworkPolicyManagerService = MiuiNetworkPolicyManagerService.this;
                        miuiNetworkPolicyManagerService.enableSleepModeChain(miuiNetworkPolicyManagerService.mSleepModeEnter);
                    }
                    Log.d(MiuiNetworkPolicyManagerService.SLEEPMODE_TAG, "Sleep mode receiver unregister");
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(EARTHQUAKE_WARNING_FLAG), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_SLEEPMODE_NETWORKPOLICY_ENABLED), false, observer, -1);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyManagerService.34
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSleepModeWhitelistUidRules() {
        if (!this.mSleepModeWhitelistUids.isEmpty()) {
            Iterator<Integer> it = this.mSleepModeWhitelistUids.iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                updateSleepModeWhitelistUidRules(uid, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearSleepModeWhitelistUidRules() {
        if (!this.mSleepModeWhitelistUids.isEmpty()) {
            Iterator<Integer> it = this.mSleepModeWhitelistUids.iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                updateSleepModeWhitelistUidRules(uid, false);
            }
            this.mSleepModeWhitelistUids.clear();
        }
    }

    private void updateSleepModeWhitelistUidRules(int uid, boolean added) {
        try {
            if (this.cm != null) {
                Log.i(SLEEPMODE_TAG, "updateSleepModeWhitelistUidRules appId:" + uid + " added:" + added);
                Class clazz = Class.forName("android.net.ConnectivityManager");
                Method method = clazz.getDeclaredMethod("updateSleepModeUidRule", Integer.TYPE, Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(this.cm, Integer.valueOf(uid), Boolean.valueOf(added));
            }
        } catch (Exception e) {
            Log.e(SLEEPMODE_TAG, "updateSleepModeWhitelistUidRules error ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableSleepModeChain(boolean enable) {
        try {
            if (this.cm != null) {
                Class clazz = Class.forName("android.net.ConnectivityManager");
                Method method = clazz.getDeclaredMethod("enableSleepModeChain", Boolean.TYPE);
                method.setAccessible(true);
                Object result = method.invoke(this.cm, Boolean.valueOf(enable));
                Log.i(SLEEPMODE_TAG, "enableSleepModeChain enable:" + enable + " result:" + result);
            }
        } catch (Exception e) {
            Log.e(SLEEPMODE_TAG, "enableSleepModeChain error!", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getEnableLimitTimeout() {
        if (this.mTrafficPolicyMode == 2) {
            return ENABLE_LIMIT_TIMEOUT_LEVEL_3;
        }
        switch (this.mThermalForceMode) {
            case 1:
                return i.b;
            case 2:
                return ENABLE_LIMIT_TIMEOUT_LEVEL_2;
            case 3:
                return ENABLE_LIMIT_TIMEOUT_LEVEL_3;
            default:
                Log.i(TAG, "getEnableLimitTimeout when thermal mode is:" + this.mThermalForceMode);
                return ENABLE_LIMIT_TIMEOUT_LEVEL_3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getDisableLimitTimeout() {
        if (this.mTrafficPolicyMode == 2) {
            return 5000;
        }
        switch (this.mThermalForceMode) {
            case 1:
                return i.b;
            case 2:
                return DISABLE_LIMIT_TIMEOUT_LEVEL_2;
            case 3:
                return 5000;
            default:
                Log.i(TAG, "getDisableLimitTimeout when thermal mode is:" + this.mThermalForceMode);
                return 5000;
        }
    }

    /* loaded from: classes.dex */
    private class ThermalStateListener extends FileObserver {
        public ThermalStateListener(String path) {
            super(path);
            MiuiNetworkPolicyManagerService.this.checkWifiLimit(path);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            switch (event) {
                case 2:
                    MiuiNetworkPolicyManagerService.this.checkWifiLimit(path);
                    return;
                default:
                    return;
            }
        }
    }

    private String readLine(String path) {
        StringBuilder sb;
        BufferedReader reader = null;
        String line = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(path), 1024);
                line = reader.readLine();
                try {
                    reader.close();
                } catch (IOException e) {
                    ioe2 = e;
                    sb = new StringBuilder();
                    Log.e(TAG, sb.append("close failed:").append(ioe2).toString());
                    return line;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe2) {
                        Log.e(TAG, "close failed:" + ioe2);
                    }
                }
                throw th;
            }
        } catch (IOException ioe) {
            Log.e(TAG, "readLine:", ioe);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                    ioe2 = e2;
                    sb = new StringBuilder();
                    Log.e(TAG, sb.append("close failed:").append(ioe2).toString());
                    return line;
                }
            }
        } catch (Exception e3) {
            Log.e(TAG, "readLine:", e3);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                    ioe2 = e4;
                    sb = new StringBuilder();
                    Log.e(TAG, sb.append("close failed:").append(ioe2).toString());
                    return line;
                }
            }
        }
        return line;
    }

    private boolean validateThermalMode(int mode) {
        return mode >= 0 && mode <= 3;
    }

    private boolean setThermalPolicy(int mode) {
        if (!validateThermalMode(mode)) {
            return false;
        }
        this.mHandler.removeMessages(14);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(14, mode, 0));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkWifiLimit(String path) {
        try {
            String line = readLine(THERMAL_STATE_FILE);
            if (line != null) {
                String str_temp = line.trim();
                int wifi_limit = Integer.parseInt(str_temp);
                int thermalMode = wifi_limit;
                if (wifi_limit > 10) {
                    thermalMode = getThermalWifiLimitMode(wifi_limit);
                }
                if (!setThermalPolicy(thermalMode)) {
                    Log.w(TAG, "setThermalPolicy failed: " + thermalMode + " path: " + path);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "checkWifiLimit:", e);
        }
    }

    private int getThermalWifiLimitMode(int wifi_limit) {
        return (wifi_limit / 10000) % 10;
    }
}
