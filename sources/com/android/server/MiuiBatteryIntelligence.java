package com.android.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.am.BroadcastQueueModernStubImpl;
import java.util.ArrayList;
import java.util.List;
import miui.os.Build;
import miui.securityspace.CrossUserUtils;
import miui.util.IMiCharge;
import org.json.JSONArray;

/* loaded from: classes.dex */
public class MiuiBatteryIntelligence {
    public static final String ACTION_NEED_SCAN_WIFI = "miui.intent.action.NEED_SCAN_WIFI";
    private static final String CHARGE_CLOUD_MODULE_NAME = "ChargeFwCloudControl";
    public static final int FUNCTION_LOW_BATTERY_FAST_CHARGE = 8;
    public static final int FUNCTION_NAVIGATION_CHARGE = 2;
    public static final int FUNCTION_OUT_DOOR_CHARGE = 4;
    private static final long INTERVAL = 300000;
    private static final String MIUI_SECURITYCENTER_APP = "com.miui.securitycenter";
    private static final String NAVIGATION_APP_WHILTE_LIST = "NavigationWhiteList";
    private static final String PERMISSON_MIUIBATTERY_BROADCAST = "com.xiaomi.permission.miuibatteryintelligence";
    public Context mContext;
    private BatteryInelligenceHandler mHandler;
    private boolean mPlugged;
    private int mWifiState;
    public int mfunctions;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug_stats", false);
    private static volatile MiuiBatteryIntelligence INSTANCE = null;
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private final String TAG = "MiuiBatteryIntelligence";
    private ArrayList<String> mMapApplist = new ArrayList<>();
    private IMiCharge mMiCharge = IMiCharge.getInstance();
    private String[] MAP_APP = {"com.autonavi.minimap", "com.tencent.map", "com.baidu.BaiduMap"};
    private BroadcastReceiver mStateChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryIntelligence.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int plugType = intent.getIntExtra("plugged", -1);
                boolean plugged = plugType != 0;
                if (plugged != MiuiBatteryIntelligence.this.mPlugged) {
                    MiuiBatteryIntelligence.this.mPlugged = plugged;
                    MiuiBatteryIntelligence.this.mHandler.sendMessageDelayed(1, 0L);
                    return;
                }
                return;
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                MiuiBatteryIntelligence.this.mHandler.sendMessageDelayed(2, 0L);
                return;
            }
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                int wifiState = intent.getIntExtra("wifi_state", 0);
                if (wifiState != MiuiBatteryIntelligence.this.mWifiState) {
                    MiuiBatteryIntelligence.this.mWifiState = wifiState;
                    if (wifiState == 1 && MiuiBatteryIntelligence.this.mPlugged) {
                        MiuiBatteryIntelligence.this.mHandler.sendMessageDelayed(4, 0L);
                        return;
                    }
                    return;
                }
                return;
            }
            if (MiuiBatteryIntelligence.ACTION_NEED_SCAN_WIFI.equals(action)) {
                MiuiBatteryIntelligence.this.mHandler.sendMessageDelayed(3, 0L);
            }
        }
    };

    public static MiuiBatteryIntelligence getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MiuiBatteryIntelligence.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiuiBatteryIntelligence(context);
                }
            }
        }
        return INSTANCE;
    }

    private MiuiBatteryIntelligence(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mfunctions = SystemProperties.getInt("persist.vendor.smartchg", 0);
        this.mHandler = new BatteryInelligenceHandler(MiuiBgThread.get().getLooper());
        initMapList(this.MAP_APP);
        registerCloudControlObserver(this.mContext.getContentResolver(), NAVIGATION_APP_WHILTE_LIST);
        registerChangeStateReceiver();
    }

    private void registerChangeStateReceiver() {
        IntentFilter i = new IntentFilter();
        i.addAction("android.intent.action.BATTERY_CHANGED");
        i.addAction("android.intent.action.BOOT_COMPLETED");
        if (isSupportOutDoorCharge()) {
            i.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            i.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            i.addAction(ACTION_NEED_SCAN_WIFI);
        }
        this.mContext.registerReceiver(this.mStateChangedReceiver, i);
    }

    private void initMapList(String[] maplist) {
        for (String str : maplist) {
            this.mMapApplist.add(str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readLocalCloudControlData(ContentResolver contentResolver, String moduleName) {
        if (TextUtils.isEmpty(moduleName)) {
            Slog.e("MiuiBatteryIntelligence", "moduleName can not be null");
            return;
        }
        try {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(contentResolver, moduleName, NAVIGATION_APP_WHILTE_LIST, (String) null);
            Slog.d("MiuiBatteryIntelligence", "on cloud read and data = " + data);
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                this.mMapApplist.clear();
                for (int i = 0; i < apps.length(); i++) {
                    this.mMapApplist.add(apps.getString(i));
                }
                Slog.i("MiuiBatteryIntelligence", "cloud data for listNavigation " + this.mMapApplist.toString());
            }
        } catch (Exception e) {
            Slog.e("MiuiBatteryIntelligence", "Exception when readLocalCloudControlData  :", e);
        }
    }

    private void registerCloudControlObserver(final ContentResolver contentResolver, final String moduleName) {
        contentResolver.registerContentObserver(URI_CLOUD_ALL_DATA_NOTIFY, true, new ContentObserver(null) { // from class: com.android.server.MiuiBatteryIntelligence.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                Slog.i("MiuiBatteryIntelligence", "cloud data has update");
                MiuiBatteryIntelligence.this.readLocalCloudControlData(contentResolver, moduleName);
            }
        });
    }

    public boolean isSupportNavigationCharge() {
        return (this.mfunctions & 2) > 0 && !Build.IS_INTERNATIONAL_BUILD;
    }

    public boolean isSupportOutDoorCharge() {
        return (this.mfunctions & 4) > 0;
    }

    public boolean isSupportLowBatteryFastCharge() {
        return (this.mfunctions & 8) > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BatteryInelligenceHandler extends Handler {
        public static final String CLOSE_LOW_BATTERY_FAST_CHARGE_FUNCTION = "8";
        public static final String KEY_LOW_BATTERY_FAST_CHARGE = "pc_low_battery_fast_charge";
        static final int MSG_BOOT_COMPLETED = 2;
        static final int MSG_DELAY_JUDGMEMNT = 5;
        static final int MSG_NEED_SCAN_WIFI = 3;
        static final int MSG_PLUGGED_CHANGED = 1;
        static final int MSG_WIFI_STATE_CHANGE = 4;
        public static final String SMART_CHARGER_NODE = "smart_chg";
        public static final String START_LOW_BATTERY_FAST_CHARGE_FUNCTION = "9";
        public static final String START_OUT_DOOR_CHARGE_FUNCTION = "5";
        public static final String STOP_OUT_DOOR_CHARGE_FUNCTION = "4";
        private AlarmManager mAlarmManager;
        private BatteryNotificationListernerService mBatteryNotificationListerner;
        private final ContentResolver mContentResolver;
        private boolean mIsOutDoor;
        private PendingIntent mNeedScanWifi;
        private List<WifiConfiguration> mSavedApInfo;
        private ContentObserver mSettingsObserver;
        private WifiManager mWifiManager;

        public BatteryInelligenceHandler(Looper looper) {
            super(looper);
            this.mBatteryNotificationListerner = new BatteryNotificationListernerService();
            this.mSettingsObserver = new ContentObserver(MiuiBatteryIntelligence.this.mHandler) { // from class: com.android.server.MiuiBatteryIntelligence.BatteryInelligenceHandler.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    int state = Settings.Secure.getInt(BatteryInelligenceHandler.this.mContentResolver, BatteryInelligenceHandler.KEY_LOW_BATTERY_FAST_CHARGE, -1);
                    if (state == -1) {
                        Slog.e("MiuiBatteryIntelligence", "Failed to get settings");
                        return;
                    }
                    Slog.d("MiuiBatteryIntelligence", "Settings onChange and low-Battery-Charge State = " + state);
                    if (state == 1) {
                        BatteryInelligenceHandler.this.setSmartChargeFunction(BatteryInelligenceHandler.START_LOW_BATTERY_FAST_CHARGE_FUNCTION);
                    } else if (state == 0) {
                        BatteryInelligenceHandler.this.setSmartChargeFunction(BatteryInelligenceHandler.CLOSE_LOW_BATTERY_FAST_CHARGE_FUNCTION);
                    }
                }
            };
            this.mContentResolver = MiuiBatteryIntelligence.this.mContext.getContentResolver();
            this.mWifiManager = (WifiManager) MiuiBatteryIntelligence.this.mContext.getSystemService("wifi");
            this.mAlarmManager = (AlarmManager) MiuiBatteryIntelligence.this.mContext.getSystemService("alarm");
            if (MiuiBatteryIntelligence.this.isSupportLowBatteryFastCharge()) {
                registerSettingsObserver();
            }
        }

        private void registerSettingsObserver() {
            Uri u = Settings.Secure.getUriFor(KEY_LOW_BATTERY_FAST_CHARGE);
            this.mContentResolver.registerContentObserver(u, true, this.mSettingsObserver);
        }

        public void sendMessageDelayed(int what, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            sendMessageDelayed(m, delayMillis);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (MiuiBatteryIntelligence.this.isSupportNavigationCharge()) {
                        changeListenerStatus();
                    }
                    if (MiuiBatteryIntelligence.this.isSupportOutDoorCharge()) {
                        changeOutDoorState();
                        return;
                    }
                    return;
                case 2:
                    if (MiuiBatteryIntelligence.this.isSupportNavigationCharge()) {
                        MiuiBatteryIntelligence miuiBatteryIntelligence = MiuiBatteryIntelligence.this;
                        miuiBatteryIntelligence.readLocalCloudControlData(miuiBatteryIntelligence.mContext.getContentResolver(), MiuiBatteryIntelligence.NAVIGATION_APP_WHILTE_LIST);
                    }
                    if (MiuiBatteryIntelligence.this.isSupportLowBatteryFastCharge()) {
                        resetLowBatteryFastChargeState();
                        return;
                    }
                    return;
                case 3:
                    scanConfirmSavaStatus();
                    return;
                case 4:
                    String type = MiuiBatteryIntelligence.this.mMiCharge.getBatteryChargeType();
                    if ("DCP".equals(type)) {
                        handlerWifiChangedDisable();
                        return;
                    }
                    return;
                case 5:
                    outDoorJudgment();
                    return;
                default:
                    Slog.d("MiuiBatteryIntelligence", "no msg need to handle");
                    return;
            }
        }

        private void changeListenerStatus() {
            if (MiuiBatteryIntelligence.this.mPlugged) {
                try {
                    this.mBatteryNotificationListerner.registerAsSystemService(MiuiBatteryIntelligence.this.mContext, new ComponentName(MiuiBatteryIntelligence.this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
                } catch (RemoteException e) {
                    Slog.e("MiuiBatteryIntelligence", "Cannot register listener");
                }
            } else {
                try {
                    this.mBatteryNotificationListerner.clearStartAppList();
                    this.mBatteryNotificationListerner.unregisterAsSystemService();
                } catch (RemoteException e2) {
                    Slog.e("MiuiBatteryIntelligence", "Cannot unregister listener");
                }
            }
        }

        private void changeOutDoorState() {
            if (MiuiBatteryIntelligence.this.mPlugged) {
                sendMessageDelayed(5, 1000L);
            } else {
                turnOffMonitorIfNeeded();
            }
        }

        private void handlerWifiChangedDisable() {
            cancelScanAlarm();
            confrimOutDoorScene();
        }

        private void resetLowBatteryFastChargeState() {
            int state = Settings.Secure.getInt(this.mContentResolver, KEY_LOW_BATTERY_FAST_CHARGE, -1);
            if (state == -1) {
                Slog.e("MiuiBatteryIntelligence", "Failed to get settings");
                return;
            }
            Slog.d("MiuiBatteryIntelligence", "Boot Completed reset low-Battery-Charge State = " + state);
            if (state == 1) {
                setSmartChargeFunction(START_LOW_BATTERY_FAST_CHARGE_FUNCTION);
            } else if (state == 0) {
                setSmartChargeFunction(CLOSE_LOW_BATTERY_FAST_CHARGE_FUNCTION);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setSmartChargeFunction(String function) {
            try {
                MiuiBatteryIntelligence.this.mMiCharge.setMiChargePath(SMART_CHARGER_NODE, function);
                Slog.d("MiuiBatteryIntelligence", "set smart charge = " + function);
            } catch (Exception e) {
                Slog.e("MiuiBatteryIntelligence", "failed to set function");
            }
        }

        private void outDoorJudgment() {
            String chargeType = MiuiBatteryIntelligence.this.mMiCharge.getBatteryChargeType();
            Slog.d("MiuiBatteryIntelligence", "TYPE = " + chargeType);
            if (!"DCP".equals(chargeType)) {
                return;
            }
            if (MiuiBatteryIntelligence.this.mWifiState == 1) {
                confrimOutDoorScene();
                return;
            }
            if (MiuiBatteryIntelligence.this.mWifiState == 3) {
                getSavedApInfo();
                if (this.mSavedApInfo == null) {
                    Slog.d("MiuiBatteryIntelligence", "there is no saved AP info");
                    confrimOutDoorScene();
                } else {
                    scanConfirmSavaStatus();
                }
            }
        }

        private void turnOffMonitorIfNeeded() {
            if (this.mIsOutDoor) {
                setSmartChargeFunction("4");
            }
            this.mIsOutDoor = false;
            removeMessages(5);
            cancelScanAlarm();
        }

        private void scanConfirmSavaStatus() {
            List<ScanResult> scanResults = sacnApList();
            if (scanResults == null || this.mSavedApInfo == null) {
                Slog.e("MiuiBatteryIntelligence", "Faild to get current AP list");
                return;
            }
            if (!compareSavedList(scanResults)) {
                confrimOutDoorScene();
                return;
            }
            Intent needScan = new Intent(MiuiBatteryIntelligence.ACTION_NEED_SCAN_WIFI);
            this.mNeedScanWifi = PendingIntent.getBroadcast(MiuiBatteryIntelligence.this.mContext, 0, needScan, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
            this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + MiuiBatteryIntelligence.INTERVAL, this.mNeedScanWifi);
            Slog.d("MiuiBatteryIntelligence", "has saved AP,wait 5 mins");
        }

        private boolean compareSavedList(List<ScanResult> scanResults) {
            for (ScanResult scanResult : scanResults) {
                String ssid = scanResult.SSID;
                if (MiuiBatteryIntelligence.DEBUG) {
                    Slog.d("MiuiBatteryIntelligence", "scanResult ssid = " + ssid);
                }
                for (WifiConfiguration wifiConfiguration : this.mSavedApInfo) {
                    if (("\"" + ssid + "\"").equals(wifiConfiguration.SSID)) {
                        Slog.d("MiuiBatteryIntelligence", "already has saved");
                        return true;
                    }
                }
            }
            return false;
        }

        private void confrimOutDoorScene() {
            if (this.mIsOutDoor) {
                return;
            }
            this.mIsOutDoor = true;
            Slog.d("MiuiBatteryIntelligence", "Now we get outDoorScene");
            setSmartChargeFunction(START_OUT_DOOR_CHARGE_FUNCTION);
        }

        private void cancelScanAlarm() {
            PendingIntent pendingIntent = this.mNeedScanWifi;
            if (pendingIntent != null) {
                this.mAlarmManager.cancel(pendingIntent);
                this.mNeedScanWifi = null;
                Slog.d("MiuiBatteryIntelligence", "cancel alarm");
            }
        }

        private List<ScanResult> sacnApList() {
            return this.mWifiManager.getScanResults();
        }

        private void getSavedApInfo() {
            this.mSavedApInfo = this.mWifiManager.getConfiguredNetworks();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BatteryNotificationListernerService extends NotificationListenerService {
        private static final String ACTION_FULL_CHARGER_NAVIGATION = "miui.intent.action.ACTION_FULL_CHARGE_NAVIGATION";
        private static final String EXTRA_FULL_CHARGER_NAVIGATION = "miui.intent.extra.FULL_CHARGER_NAVIGATION";
        private ArrayList<String> mAlreadyStartApp;
        private ArrayList<String> mAlreadyStartAppXSpace;
        public boolean mIsNavigation;

        private BatteryNotificationListernerService() {
            this.mAlreadyStartApp = new ArrayList<>();
            this.mAlreadyStartAppXSpace = new ArrayList<>();
        }

        @Override // android.service.notification.NotificationListenerService
        public void onListenerConnected() {
            StatusBarNotification[] postedNotifications = null;
            try {
                postedNotifications = getActiveNotifications();
            } catch (Exception e) {
                Slog.e("MiuiBatteryIntelligence", "Get Notification Faild");
            }
            if (postedNotifications != null) {
                for (StatusBarNotification sbn : postedNotifications) {
                    checkNavigationEntry(sbn);
                }
            }
        }

        @Override // android.service.notification.NotificationListenerService
        public void onListenerDisconnected() {
            Slog.d("MiuiBatteryIntelligence", "destory?");
            this.mIsNavigation = false;
            sendNavigationBroadcast();
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationPosted(StatusBarNotification sbn) {
            checkNavigationEntry(sbn);
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationRemoved(StatusBarNotification sbn) {
            checkNavigationEnd(sbn);
        }

        private void checkNavigationEntry(StatusBarNotification sbn) {
            if (isNavigationStatus(sbn)) {
                String packageName = sbn.getPackageName();
                int appUserId = sbn.getUserId();
                if (appUserId == 0 && !this.mAlreadyStartApp.contains(packageName)) {
                    this.mAlreadyStartApp.add(packageName);
                } else if (appUserId == 999 && !this.mAlreadyStartAppXSpace.contains(packageName)) {
                    this.mAlreadyStartAppXSpace.add(packageName);
                }
                Slog.d("MiuiBatteryIntelligence", "Owner Space Start APP list = " + this.mAlreadyStartApp + "  XSpace Start APP list = " + this.mAlreadyStartAppXSpace);
                processNavigation();
            }
        }

        private void checkNavigationEnd(StatusBarNotification sbn) {
            if (isNavigationStatus(sbn)) {
                String packageName = sbn.getPackageName();
                int appUserId = sbn.getUserId();
                if (appUserId == 0 && this.mAlreadyStartApp.contains(packageName)) {
                    this.mAlreadyStartApp.remove(packageName);
                } else if (appUserId == 999 && this.mAlreadyStartAppXSpace.contains(packageName)) {
                    this.mAlreadyStartAppXSpace.remove(packageName);
                }
                Slog.d("MiuiBatteryIntelligence", "Owner Space Start APP list = " + this.mAlreadyStartApp + "  XSpace Start APP list = " + this.mAlreadyStartAppXSpace);
                if (this.mAlreadyStartApp.isEmpty() && this.mAlreadyStartAppXSpace.isEmpty()) {
                    this.mIsNavigation = false;
                    sendNavigationBroadcast();
                }
            }
        }

        private boolean isNavigationStatus(StatusBarNotification sbn) {
            if (sbn == null || !isOnwerUser()) {
                return false;
            }
            String packName = sbn.getPackageName();
            boolean isClearable = sbn.isClearable();
            if (isClearable || !MiuiBatteryIntelligence.this.mMapApplist.contains(packName)) {
                return false;
            }
            return true;
        }

        private void processNavigation() {
            this.mIsNavigation = true;
            sendNavigationBroadcast();
        }

        private void sendNavigationBroadcast() {
            Intent intent = new Intent(ACTION_FULL_CHARGER_NAVIGATION);
            intent.putExtra(EXTRA_FULL_CHARGER_NAVIGATION, isTargetEnvironment());
            intent.setPackage("com.miui.securitycenter");
            MiuiBatteryIntelligence.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, MiuiBatteryIntelligence.PERMISSON_MIUIBATTERY_BROADCAST);
        }

        private boolean isTargetEnvironment() {
            Slog.d("MiuiBatteryIntelligence", "Plugged? " + MiuiBatteryIntelligence.this.mPlugged + " isNavigation? " + this.mIsNavigation);
            return MiuiBatteryIntelligence.this.mPlugged && this.mIsNavigation;
        }

        private boolean isOnwerUser() {
            return CrossUserUtils.getCurrentUserId() == 0;
        }

        public void clearStartAppList() {
            this.mAlreadyStartApp.clear();
            this.mAlreadyStartAppXSpace.clear();
        }
    }
}
