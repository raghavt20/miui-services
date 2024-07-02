package com.android.server;

import android.R;
import android.app.ActivityThread;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.pm.CloudControlPreinstallService;
import com.miui.app.MiuiFboServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.security.AccessControlImpl;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import miui.os.Build;
import miui.util.IMiCharge;
import miuix.appcompat.app.AlertDialog;

@MiuiStubHead(manifestName = "com.android.server.MiuiBatteryServiceStub$$")
/* loaded from: classes.dex */
public class MiuiBatteryServiceImpl extends MiuiBatteryServiceStub {
    private static final String AES_DECRYPTED_TEXT = "0123456789abcd";
    private static final String AES_KEY = "123456789abcdefa";
    private static final int HANDLE_PRODUCT_ID = 20611;
    private static final int HANDLE_VENDOR_ID = 10007;
    public static final int HIGH_TEMP_STOP_CHARGE_STATE = 5;
    private static final String KEY_BATTERY_TEMP_ALLOW_KILL = "allowed_kill_battery_temp_threshhold";
    public static final String KEY_FAST_CHARGE_ENABLED = "key_fast_charge_enabled";
    private static final String KEY_SETTING_TEMP_STATE = "thermal_temp_state_value";
    private static final int SHUTDOWN_TMEIOUT = 300000;
    private static final int WAKELOCK_ACQUIRE = 360000;
    private volatile BluetoothA2dp mA2dp;
    private ContentObserver mBatteryTempThreshholdObserver;
    private Context mContext;
    ContentObserver mFastChargeObserver;
    private BatteryHandler mHandler;
    private volatile BluetoothHeadset mHeadset;
    private ContentObserver mTempStateObserver;
    private MiuiFboServiceInternal miuiFboService;
    private static boolean isSystemReady = false;
    private static final String[] TEST_APP_PACKAGENAME_KDDI = {"com.kddi.hirakuto", "com.kddi.hirakuto.debug"};
    private static final String[] TEST_APP_PACKAGENAME_SB = {"com.xiaomi.mihomemanager"};
    private final String TAG = "MiuiBatteryServiceImpl";
    private final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug_impl", false);
    private boolean mIsStopCharge = false;
    private boolean mIsSatisfyTempSocCondition = false;
    private boolean mIsSatisfyTimeRegionCondition = false;
    private boolean mIsSatisfyTempLevelCondition = false;
    private boolean mIsTestMode = false;
    private int BtConnectedCount = 0;
    private IMiCharge mMiCharge = IMiCharge.getInstance();
    public final String[] SUPPORT_COUNTRY = {"IT", "FR", "ES", "DE", "PL", "GB"};
    private int mLastPhoneBatteryLevel = -1;
    private boolean isDisCharging = false;
    private int mBtConnectState = 0;
    private PowerManager pm = null;
    private PowerManager.WakeLock wakeLock = null;
    private BroadcastReceiver mChargingStateReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean z;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1538406691:
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        z = false;
                        break;
                    }
                default:
                    z = -1;
                    break;
            }
            switch (z) {
                case false:
                    int status = intent.getIntExtra("status", -1);
                    MiuiBatteryServiceImpl.this.isDisCharging = status == 3;
                    Slog.d("MiuiBatteryServiceImpl", "mChargingStateReceiver:isDisCharging = " + MiuiBatteryServiceImpl.this.isDisCharging);
                    if (MiuiBatteryServiceImpl.this.isDisCharging && MiuiBatteryServiceImpl.this.mBtConnectState == 0 && !MiuiBatteryServiceImpl.this.mHandler.hasCallbacks(MiuiBatteryServiceImpl.this.shutDownRnuable)) {
                        MiuiBatteryServiceImpl.this.wakeLock.acquire(360000L);
                        MiuiBatteryServiceImpl.this.mHandler.postDelayed(MiuiBatteryServiceImpl.this.shutDownRnuable, 300000L);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                MiuiBatteryServiceImpl.this.mBtConnectState = 2;
                MiuiBatteryServiceImpl.this.mHandler.removeCallbacks(MiuiBatteryServiceImpl.this.shutDownRnuable);
            } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                MiuiBatteryServiceImpl.this.mBtConnectState = 0;
                if (MiuiBatteryServiceImpl.this.isDisCharging) {
                    MiuiBatteryServiceImpl.this.wakeLock.acquire(360000L);
                    MiuiBatteryServiceImpl.this.mHandler.removeCallbacks(MiuiBatteryServiceImpl.this.shutDownRnuable);
                    MiuiBatteryServiceImpl.this.mHandler.postDelayed(MiuiBatteryServiceImpl.this.shutDownRnuable, 300000L);
                }
            }
        }
    };
    private Runnable shutDownRnuable = new Runnable() { // from class: com.android.server.MiuiBatteryServiceImpl.3
        @Override // java.lang.Runnable
        public void run() {
            if (MiuiBatteryServiceImpl.this.isDisCharging) {
                if (MiuiBatteryServiceImpl.this.mBtConnectState == 0 || MiuiBatteryServiceImpl.this.mBtConnectState == 3) {
                    MiuiBatteryServiceImpl.this.pm.shutdown(false, "bt_disconnect_5min", false);
                }
            }
        }
    };
    private BroadcastReceiver mUsbStateReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.4
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2114103349:
                    if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1608292967:
                    if (action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1538406691:
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 798292259:
                    if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    UsbDevice attachedDevice = (UsbDevice) intent.getParcelableExtra(CloudControlPreinstallService.ConnectEntity.DEVICE);
                    if (attachedDevice != null && MiuiBatteryServiceImpl.HANDLE_VENDOR_ID == attachedDevice.getVendorId() && MiuiBatteryServiceImpl.HANDLE_PRODUCT_ID == attachedDevice.getProductId()) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(23, attachedDevice, 0L);
                        return;
                    }
                    return;
                case 1:
                    UsbDevice detachedDevice = (UsbDevice) intent.getParcelableExtra(CloudControlPreinstallService.ConnectEntity.DEVICE);
                    if (detachedDevice != null && MiuiBatteryServiceImpl.HANDLE_VENDOR_ID == detachedDevice.getVendorId() && MiuiBatteryServiceImpl.HANDLE_PRODUCT_ID == detachedDevice.getProductId()) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(24, detachedDevice, 0L);
                        return;
                    }
                    return;
                case 2:
                    MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(25, 0L);
                    return;
                case 3:
                    int phoneBatteryLevel = intent.getIntExtra("level", -1);
                    if (MiuiBatteryServiceImpl.this.mHandler.isHandleConnect() && MiuiBatteryServiceImpl.this.mLastPhoneBatteryLevel != phoneBatteryLevel) {
                        MiuiBatteryServiceImpl.this.mLastPhoneBatteryLevel = phoneBatteryLevel;
                        MiuiBatteryServiceImpl.this.mMiCharge.setTypeCCommonInfo("PhoneBatteryChanged", Integer.toString(MiuiBatteryServiceImpl.this.mLastPhoneBatteryLevel));
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() { // from class: com.android.server.MiuiBatteryServiceImpl.11
        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            switch (profile) {
                case 1:
                    MiuiBatteryServiceImpl.this.mHeadset = (BluetoothHeadset) proxy;
                    return;
                case 2:
                    MiuiBatteryServiceImpl.this.mA2dp = (BluetoothA2dp) proxy;
                    return;
                default:
                    return;
            }
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int profile) {
            switch (profile) {
                case 1:
                    MiuiBatteryServiceImpl.this.mHeadset = null;
                    return;
                case 2:
                    MiuiBatteryServiceImpl.this.mA2dp = null;
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mSupportWirelessCharge = this.mMiCharge.isWirelessChargingSupported();
    private boolean mSupportSB = this.mMiCharge.isFunctionSupported("smart_batt");
    private boolean mSupportHighTempStopCharge = SystemProperties.getBoolean("persist.vendor.high_temp_stop_charge", false);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBatteryServiceImpl> {

        /* compiled from: MiuiBatteryServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBatteryServiceImpl INSTANCE = new MiuiBatteryServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiBatteryServiceImpl m155provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiBatteryServiceImpl m154provideNewInstance() {
            return new MiuiBatteryServiceImpl();
        }
    }

    private void registerChargingStateBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(this.mChargingStateReceiver, intentFilter, 2);
    }

    private void registerBluetoothStateBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        this.mContext.registerReceiver(this.mBluetoothStateReceiver, intentFilter, 2);
    }

    public void init(Context context) {
        this.mContext = context;
        this.mHandler = new BatteryHandler(MiuiFgThread.get().getLooper());
        this.miuiFboService = (MiuiFboServiceInternal) LocalServices.getService(MiuiFboServiceInternal.class);
        isSystemReady = true;
        this.mFastChargeObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiBatteryServiceImpl.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                String socDecimal = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimal();
                String socDecimalRate = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimalRate();
                if (!TextUtils.isEmpty(socDecimal) && !TextUtils.isEmpty(socDecimalRate)) {
                    int socDecimalValue = MiuiBatteryServiceImpl.this.mHandler.parseInt(socDecimal);
                    int socDecimalRateVaule = MiuiBatteryServiceImpl.this.mHandler.parseInt(socDecimalRate);
                    MiuiBatteryServiceImpl.this.mHandler.sendMessage(4, socDecimalValue, socDecimalRateVaule);
                }
            }
        };
        this.mTempStateObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiBatteryServiceImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(29, 0L);
            }
        };
        this.mBatteryTempThreshholdObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiBatteryServiceImpl.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(29, 0L);
            }
        };
        BroadcastReceiver stateChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.8
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (MiuiBatteryServiceImpl.this.DEBUG) {
                    Slog.d("MiuiBatteryServiceImpl", "action = " + action);
                }
                if (!"android.intent.action.BATTERY_CHANGED".equals(action)) {
                    if ("android.intent.action.USER_PRESENT".equals(action)) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(18, 0L);
                        return;
                    } else {
                        if ("android.intent.action.SCREEN_OFF".equals(action)) {
                            MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(19, 0L);
                            return;
                        }
                        return;
                    }
                }
                int plugType = intent.getIntExtra("plugged", -1);
                int batteryLevel = intent.getIntExtra("level", -1);
                boolean plugged = plugType != 0;
                if (MiuiBatteryServiceImpl.this.mIsTestMode && (batteryLevel >= 75 || batteryLevel <= 40)) {
                    MiuiBatteryServiceImpl.this.mHandler.sendMessage(26, batteryLevel);
                }
                if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && !plugged && MiuiBatteryServiceImpl.this.mMiCharge.getWirelessChargingStatus() == 0) {
                    MiuiBatteryServiceImpl.this.mHandler.sendMessage(0, batteryLevel);
                }
            }
        };
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.BATTERY_CHANGED");
        if (SystemProperties.get("ro.product.device", "").startsWith("yudi")) {
            intentfilter.addAction("android.intent.action.SCREEN_OFF");
            intentfilter.addAction("android.intent.action.USER_PRESENT");
        }
        this.mContext.registerReceiver(stateChangedReceiver, intentfilter);
        String chargeType = this.mMiCharge.getBatteryChargeType();
        if (chargeType != null && chargeType.length() > 0 && !SystemProperties.getBoolean("persist.vendor.charge.oneTrack", false)) {
            MiuiBatteryStatsService.getInstance(context);
        }
        if ((SystemProperties.get("ro.product.device", "").startsWith("mona") || SystemProperties.get("ro.product.device", "").startsWith("thor")) && !Build.IS_INTERNATIONAL_BUILD) {
            MiuiBatteryAuthentic.getInstance(context);
        }
        if (SystemProperties.get("ro.miui.customized.region", "").startsWith("jp_kd")) {
            this.mIsTestMode = isTestMUTForJapan(TEST_APP_PACKAGENAME_KDDI);
        }
        if (SystemProperties.get("ro.miui.customized.region", "").startsWith("jp_sb")) {
            this.mIsTestMode = isTestMUTForJapan(TEST_APP_PACKAGENAME_SB);
        }
        if (SystemProperties.getInt("persist.vendor.smartchg", 0) > 0) {
            MiuiBatteryIntelligence.getInstance(context);
        }
        if (SystemProperties.get("ro.product.mod_device", "").startsWith("star") || SystemProperties.get("ro.product.mod_device", "").startsWith("mars")) {
            int initBtConnectCount = initBtConnectCount();
            this.BtConnectedCount = initBtConnectCount;
            if (initBtConnectCount > 0) {
                this.mHandler.sendMessageDelayed(10, true, 1000L);
            }
            BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.9
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                        int value = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                        if (value == 15 || value == 10) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount = 0;
                            MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, false);
                            return;
                        }
                        return;
                    }
                    if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action) || "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                        int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                        int preState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
                        if (state == 2) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount++;
                            MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, true);
                        } else if (state == 0 && preState != 1) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount--;
                            if (MiuiBatteryServiceImpl.this.BtConnectedCount <= 0) {
                                MiuiBatteryServiceImpl.this.BtConnectedCount = 0;
                                MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, false);
                            }
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
            this.mContext.registerReceiver(bluetoothReceiver, filter, 2);
        }
        if (this.mSupportSB) {
            BroadcastReceiver updateBattVolIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.10
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if (MiuiBatteryServiceImpl.this.DEBUG) {
                        Slog.d("MiuiBatteryServiceImpl", "action = " + action);
                    }
                    if ("android.intent.action.BOOT_COMPLETED".equals(action) || MiuiBatteryStatsService.UPDATE_BATTERY_DATA.equals(action)) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(15, 0L);
                    } else if (MiuiBatteryStatsService.ADJUST_VOLTAGE.equals(action)) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(20, intent, 0L);
                    }
                }
            };
            IntentFilter filter2 = new IntentFilter(MiuiBatteryStatsService.UPDATE_BATTERY_DATA);
            filter2.addAction("android.intent.action.BOOT_COMPLETED");
            filter2.addAction(MiuiBatteryStatsService.ADJUST_VOLTAGE);
            this.mContext.registerReceiver(updateBattVolIntentReceiver, filter2, 2);
        }
        if (this.mSupportHighTempStopCharge) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_SETTING_TEMP_STATE), false, this.mTempStateObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_BATTERY_TEMP_ALLOW_KILL), false, this.mBatteryTempThreshholdObserver);
        }
        if (SystemProperties.get("ro.product.device", "").startsWith("aurora")) {
            registerUsbStateBroadcastReceiver();
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_FAST_CHARGE_ENABLED), false, this.mFastChargeObserver);
        if (SystemProperties.get("ro.product.device", "").startsWith("zhuque")) {
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
            this.pm = powerManager;
            this.wakeLock = powerManager.newWakeLock(1, "Screenoffshutdown");
            registerChargingStateBroadcastReceiver();
            registerBluetoothStateBroadcastReceiver();
            this.wakeLock.acquire(360000L);
            this.mHandler.postDelayed(this.shutDownRnuable, 300000L);
        }
    }

    public void setBatteryStatusWithFbo(int batteryStatus, int batteryLevel, int batteryTemperature) {
        if (!isSystemReady) {
            return;
        }
        this.miuiFboService.setBatteryInfos(batteryStatus, batteryLevel, batteryTemperature);
        if (this.miuiFboService.getGlobalSwitch() && batteryTemperature >= 500) {
            this.miuiFboService.deliverMessage("stop", 3, 0L);
            return;
        }
        if (this.miuiFboService.getNativeIsRunning() && this.miuiFboService.getGlobalSwitch() && batteryTemperature > 450) {
            this.miuiFboService.deliverMessage("stopDueTobatteryTemperature", 6, 0L);
        } else if (!this.miuiFboService.getNativeIsRunning() && this.miuiFboService.getGlobalSwitch() && !this.miuiFboService.getDueToScreenWait() && batteryTemperature < 400) {
            this.miuiFboService.deliverMessage("continue", 2, 0L);
        }
    }

    private int initBtConnectCount() {
        int a2dpCount = 0;
        int headsetCount = 0;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            btAdapter.getProfileProxy(this.mContext, this.mProfileServiceListener, 2);
            btAdapter.getProfileProxy(this.mContext, this.mProfileServiceListener, 1);
        }
        if (this.mA2dp != null) {
            a2dpCount = this.mA2dp.getConnectedDevices().size();
        }
        if (this.mHeadset != null) {
            headsetCount = this.mHeadset.getConnectedDevices().size();
        }
        return a2dpCount + headsetCount;
    }

    private void registerUsbStateBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mUsbStateReceiver, intentFilter, 2);
    }

    private boolean isTestMUTForJapan(String[] appPackage) {
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackages(0);
        List<String> appPackagesList = Arrays.asList(appPackage);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if (appPackagesList.contains(packageInfo.packageName)) {
                Slog.d("MiuiBatteryServiceImpl", "TEST MODE");
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class BatteryHandler extends Handler {
        private static final String ACTION_HANDLE_BATTERY_STATE_CHANGED = "miui.intent.action.ACTION_HANDLE_BATTERY_STATE_CHANGED";
        private static final String ACTION_HANDLE_STATE_CHANGED = "miui.intent.action.ACTION_HANDLE_STATE_CHANGED";
        private static final String ACTION_MIUI_PC_BATTERY_CHANGED = "miui.intent.action.MIUI_PC_BATTERY_CHANGED";
        private static final String ACTION_MOISTURE_DET = "miui.intent.action.ACTION_MOISTURE_DET";
        private static final String ACTION_POGO_CONNECTED_STATE = "miui.intent.action.ACTION_POGO_CONNECTED_STATE";
        private static final String ACTION_REVERSE_PEN_CHARGE_STATE = "miui.intent.action.ACTION_PEN_REVERSE_CHARGE_STATE";
        private static final String ACTION_RX_OFFSET = "miui.intent.action.ACTION_RX_OFFSET";
        private static final String ACTION_TYPE_C_HIGH_TEMP = "miui.intent.action.ACTION_TYPE_C_HIGH_TEMP";
        private static final String ACTION_WIRELESS_CHARGING = "miui.intent.action.ACTION_WIRELESS_CHARGING";
        private static final String ACTION_WIRELESS_CHG_WARNING_ACTIVITY = "miui.intent.action.ACTIVITY_WIRELESS_CHG_WARNING";
        private static final String ACTION_WIRELESS_FW_UPDATE = "miui.intent.action.ACTION_WIRELESS_FW_UPDATE";
        private static final String CAR_APP_STATE_EVENT = "POWER_SUPPLY_CAR_APP_STATE";
        private static final String CONNECTOR_TEMP_EVENT = "POWER_SUPPLY_CONNECTOR_TEMP";
        private static final String EXTRA_CAR_CHG = "miui.intent.extra.CAR_CHARGE";
        private static final String EXTRA_HANDLE_CONNECT_STATE = "miui.intent.extra.EXTRA_HANDLE_CONNECT_STATE";
        private static final String EXTRA_HANDLE_VERSION = "miui.intent.extra.EXTRA_HANDLE_VERSION";
        private static final String EXTRA_LOW_TX_OFFESET_STATE = "miui.intent.extra.EXTRA_LOW_TX_OFFSET_STATE";
        private static final String EXTRA_MOISTURE_DET = "miui.intent.extra.EXTRA_MOISTURE_DET";
        private static final String EXTRA_NTC_ALARM = "miui.intent.extra.EXTRA_NTC_ALARM";
        private static final String EXTRA_POGO_CONNECTED_STATE = "miui.intent.extra.EXTRA_POGO_CONNECTED_STATE";
        private static final String EXTRA_POWER_MAX = "miui.intent.extra.POWER_MAX";
        private static final String EXTRA_REVERSE_PEN_CHARGE_STATE = "miui.intent.extra.ACTION_PEN_REVERSE_CHARGE_STATE";
        private static final String EXTRA_REVERSE_PEN_SOC = "miui.intent.extra.REVERSE_PEN_SOC";
        private static final String EXTRA_RX_OFFSET = "miui.intent.extra.EXTRA_RX_OFFSET";
        private static final String EXTRA_TYPE_C_HIGH_TEMP = "miui.intent.extra.EXTRA_TYPE_C_HIGH_TEMP";
        private static final String EXTRA_WIRELESS_CHARGING = "miui.intent.extra.WIRELESS_CHARGING";
        private static final String EXTRA_WIRELESS_FW_UPDATE = "miui.intent.extra.EXTRA_WIRELESS_FW_UPDATE";
        private static final String HANDLE_BATTERY_CHANGED = "POWER_SUPPLY_NAME";
        private static final int HANDLE_STATE_CONNECTED = 1;
        private static final int HANDLE_STATE_DISCONNECTED = 0;
        private static final String HAPTIC_STATE = "haptic_feedback_disable";
        private static final String HVDCP3_TYPE_EVENT = "POWER_SUPPLY_HVDCP3_TYPE";
        private static final String LOW_INDUCTANCE_OFFSET_EVENT = "POWER_SUPPLY_LOW_INDUCTANCE_OFFSET";
        private static final String MOISTURE_DET = "POWER_SUPPLY_MOISTURE_DET_STS";
        static final int MSG_ADJUST_VOLTAGE = 20;
        static final int MSG_BATTERY_CHANGED = 0;
        static final int MSG_BLUETOOTH_CHANGED = 10;
        static final int MSG_CAR_APP_STATE = 22;
        static final int MSG_CHARGE_LIMIT = 26;
        static final int MSG_CHECK_TIME_REGION = 15;
        static final int MSG_CONNECTOR_TEMP = 16;
        static final int MSG_HANDLE_ATTACHED = 23;
        static final int MSG_HANDLE_DETACHED = 24;
        static final int MSG_HANDLE_LOW_TX_OFFSET = 27;
        static final int MSG_HANDLE_REBOOT = 25;
        static final int MSG_HANDLE_TEMP_STATE = 29;
        static final int MSG_HVDCP3_DETECT = 2;
        static final int MSG_MOISTURE_DET = 21;
        static final int MSG_NFC_DISABLED = 9;
        static final int MSG_NFC_ENABLED = 8;
        static final int MSG_NTC_ALARM = 28;
        static final int MSG_POWER_OFF = 14;
        static final int MSG_QUICKCHARGE_DETECT = 3;
        static final int MSG_REVERSE_PEN_CHG_STATE = 12;
        static final int MSG_RX_OFFSET = 17;
        static final int MSG_SCREEN_OFF = 19;
        static final int MSG_SCREEN_UNLOCK = 18;
        static final int MSG_SHUTDOWN_DELAY = 7;
        static final int MSG_SHUTDOWN_DELAY_WARNING = 13;
        static final int MSG_SOC_DECIMAL = 4;
        static final int MSG_WIRELESS_CHARGE_CLOSE = 5;
        static final int MSG_WIRELESS_CHARGE_OPEN = 6;
        static final int MSG_WIRELESS_FW_STATE = 11;
        static final int MSG_WIRELESS_TX = 1;
        private static final String NFC_CLOED = "nfc_closd_from_wirelss";
        private static final String NTC_ALARM_EVENT = "POWER_SUPPLY_NTC_ALARM";
        private static final String POWER_COMMON_RECEIVER_PERMISSION = "com.miui.securitycenter.POWER_CENTER_COMMON_PERMISSION";
        private static final String POWER_SUPPLY_CAPACITY = "POWER_SUPPLY_CAPACITY";
        private static final String POWER_SUPPLY_STATUS = "POWER_SUPPLY_STATUS";
        private static final String QUICK_CHARGE_TYPE_EVENT = "POWER_SUPPLY_QUICK_CHARGE_TYPE";
        private static final int REDUCE_FULL_CHARGE_VBATT_10 = 10;
        private static final int REDUCE_FULL_CHARGE_VBATT_15 = 15;
        private static final int REDUCE_FULL_CHARGE_VBATT_20 = 20;
        private static final int RESET_FULL_CHARGE_VBATT = 0;
        private static final int RETRY_UPDATE_DELAY = 60000;
        private static final String REVERSE_CHG_MODE_EVENT = "POWER_SUPPLY_REVERSE_CHG_MODE";
        private static final String REVERSE_CHG_STATE_EVENT = "POWER_SUPPLY_REVERSE_CHG_STATE";
        private static final String REVERSE_PEN_CHG_STATE_EVENT = "POWER_SUPPLY_REVERSE_PEN_CHG_STATE";
        private static final String REVERSE_PEN_MAC_EVENT = "POWER_SUPPLY_PEN_MAC";
        private static final String REVERSE_PEN_PLACE_ERR_EVENT = "POWER_SUPPLY_PEN_PLACE_ERR";
        private static final String REVERSE_PEN_SOC_EVENT = "POWER_SUPPLY_REVERSE_PEN_SOC";
        private static final String RX_OFFSET_EVENT = "POWER_SUPPLY_RX_OFFSET";
        private static final String SHUTDOWN_DELAY_EVENT = "POWER_SUPPLY_SHUTDOWN_DELAY";
        private static final String SOC_DECIMAL_EVENT = "POWER_SUPPLY_SOC_DECIMAL";
        private static final String SOC_DECIMAL_RATE_EVENT = "POWER_SUPPLY_SOC_DECIMAL_RATE";
        private static final int UPDATE_DELAY = 1000;
        private static final int WIRELESS_AUTO_CLOSED_STATE = 1;
        private static final int WIRELESS_CHG_ERROR_STATE = 2;
        private static final int WIRELESS_LOW_BATTERY_LEVEL_STATE = 4;
        private static final int WIRELESS_NO_ERROR_STATE = 0;
        private static final int WIRELESS_OTHER_WIRELESS_CHG_STATE = 3;
        private static final String WIRELESS_REVERSE_CHARGING = "wireless_reverse_charging";
        private static final String WIRELESS_TX_TYPE_EVENT = "POWER_SUPPLY_TX_ADAPTER";
        private static final String WLS_FW_STATE_EVENT = "POWER_SUPPLY_WLS_FW_STATE";
        private int mChargingNotificationId;
        private boolean mClosedNfcFromCharging;
        private final ContentResolver mContentResolver;
        private int mCount;
        private Date mEndHighTempDate;
        private HandleInfo mHandleInfo;
        private boolean mHapticState;
        private boolean mIsReverseWirelessCharge;
        private int mLastCloseReason;
        private int mLastConnectorTemp;
        private int mLastHvdcpType;
        private int mLastMoistureDet;
        private int mLastNtcAlarm;
        private int mLastOffsetState;
        private int mLastOpenStatus;
        private int mLastPenReverseChargeState;
        private String mLastPenReverseMac;
        private String mLastPenReversePLaceErr;
        private int mLastPenReverseSoc;
        private int mLastPogoConnectedState;
        private int mLastQuickChargeType;
        private int mLastRxOffset;
        private int mLastShutdownDelay;
        private int mLastWirelessFwState;
        private int mLastWirelessTxType;
        private NfcAdapter mNfcAdapter;
        private boolean mRetryAfterOneMin;
        private boolean mShowDisableNfc;
        private boolean mShowEnableNfc;
        private Date mStartHithTempDate;
        private final UEventObserver mUEventObserver;
        private boolean mUpdateSocDecimal;

        public BatteryHandler(Looper looper) {
            super(looper);
            this.mLastOffsetState = 0;
            this.mLastNtcAlarm = 0;
            this.mShowEnableNfc = false;
            this.mShowDisableNfc = false;
            this.mIsReverseWirelessCharge = false;
            this.mRetryAfterOneMin = false;
            this.mHandleInfo = null;
            this.mCount = 0;
            initChargeStatus();
            initBatteryAuthentic();
            this.mContentResolver = MiuiBatteryServiceImpl.this.mContext.getContentResolver();
            BatteryUEventObserver batteryUEventObserver = new BatteryUEventObserver();
            this.mUEventObserver = batteryUEventObserver;
            batteryUEventObserver.startObserving(WIRELESS_TX_TYPE_EVENT);
            batteryUEventObserver.startObserving(HVDCP3_TYPE_EVENT);
            batteryUEventObserver.startObserving(QUICK_CHARGE_TYPE_EVENT);
            batteryUEventObserver.startObserving(SOC_DECIMAL_EVENT);
            batteryUEventObserver.startObserving(REVERSE_CHG_STATE_EVENT);
            batteryUEventObserver.startObserving(REVERSE_CHG_MODE_EVENT);
            batteryUEventObserver.startObserving(SHUTDOWN_DELAY_EVENT);
            batteryUEventObserver.startObserving(WLS_FW_STATE_EVENT);
            batteryUEventObserver.startObserving(REVERSE_PEN_CHG_STATE_EVENT);
            batteryUEventObserver.startObserving(REVERSE_PEN_SOC_EVENT);
            batteryUEventObserver.startObserving(REVERSE_PEN_MAC_EVENT);
            batteryUEventObserver.startObserving(REVERSE_PEN_PLACE_ERR_EVENT);
            batteryUEventObserver.startObserving(CONNECTOR_TEMP_EVENT);
            batteryUEventObserver.startObserving(RX_OFFSET_EVENT);
            batteryUEventObserver.startObserving(MOISTURE_DET);
            batteryUEventObserver.startObserving(CAR_APP_STATE_EVENT);
            batteryUEventObserver.startObserving(HANDLE_BATTERY_CHANGED);
            batteryUEventObserver.startObserving(LOW_INDUCTANCE_OFFSET_EVENT);
            batteryUEventObserver.startObserving(NTC_ALARM_EVENT);
        }

        private void sendSocDecimaBroadcast() {
            String socDecimal = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimal();
            String socDecimalRate = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimalRate();
            if (socDecimal != null && socDecimal.length() != 0 && socDecimalRate != null && socDecimalRate.length() != 0) {
                int socDecimalValue = parseInt(socDecimal);
                int socDecimalRateVaule = parseInt(socDecimalRate);
                sendMessage(4, socDecimalValue, socDecimalRateVaule);
            }
        }

        private void initChargeStatus() {
            int txAdapterValue;
            int quickChargeValue;
            String quickChargeType = MiuiBatteryServiceImpl.this.mMiCharge.getQuickChargeType();
            String txAdapter = MiuiBatteryServiceImpl.this.mMiCharge.getTxAdapt();
            Slog.d("MiuiBatteryServiceImpl", "quickChargeType = " + quickChargeType + " txAdapter = " + txAdapter);
            if (quickChargeType != null && quickChargeType.length() != 0 && (quickChargeValue = parseInt(quickChargeType)) > 0) {
                this.mLastQuickChargeType = quickChargeValue;
                sendMessage(3, quickChargeValue);
                if (quickChargeValue >= 3) {
                    sendSocDecimaBroadcast();
                }
            }
            if (txAdapter != null && txAdapter.length() != 0 && (txAdapterValue = parseInt(txAdapter)) > 0) {
                this.mLastWirelessTxType = txAdapterValue;
                sendMessage(1, txAdapterValue);
            }
        }

        private void initBatteryAuthentic() {
            String batteryAuthentic;
            if ((SystemProperties.get("ro.product.name", "").startsWith("nabu") || SystemProperties.get("ro.product.name", "").startsWith("pipa")) && (batteryAuthentic = MiuiBatteryServiceImpl.this.mMiCharge.getBatteryAuthentic()) != null && batteryAuthentic.length() != 0) {
                int batteryAuthenticValue = parseInt(batteryAuthentic);
                if (batteryAuthenticValue == 0) {
                    sendMessageDelayed(13, 30000L);
                }
            }
        }

        private boolean isSupportControlHaptic() {
            return SystemProperties.get("ro.product.device", "").startsWith("mayfly") || SystemProperties.getBoolean("persist.vendor.revchg.shutmotor", false);
        }

        public void sendMessage(int i, boolean z) {
            removeMessages(i);
            Message obtain = Message.obtain(this, i);
            obtain.arg1 = z ? 1 : 0;
            sendMessage(obtain);
        }

        public void sendMessage(int what, int arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            sendMessage(m);
        }

        public void sendMessage(int what, int arg1, int arg2) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            m.arg2 = arg2;
            sendMessage(m);
        }

        public void sendMessageDelayed(int what, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            sendMessageDelayed(m, delayMillis);
        }

        public void sendMessageDelayed(int i, boolean z, long j) {
            removeMessages(i);
            Message obtain = Message.obtain(this, i);
            obtain.arg1 = z ? 1 : 0;
            sendMessageDelayed(obtain, j);
        }

        public void sendMessageDelayed(int what, Object arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            sendMessageDelayed(m, delayMillis);
        }

        public int parseInt(String argument) {
            try {
                return Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                Slog.e("MiuiBatteryServiceImpl", "Invalid integer argument " + argument);
                return -1;
            }
        }

        private String getCurrentDate() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
            Date date = new Date(System.currentTimeMillis());
            String today = simpleDateFormat.format(date);
            return today;
        }

        private Date parseDate(String date) throws ParseException {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
            return simpleDateFormat.parse(date);
        }

        /* loaded from: classes.dex */
        private final class BatteryUEventObserver extends UEventObserver {
            private BatteryUEventObserver() {
            }

            public void onUEvent(UEventObserver.UEvent event) {
                int ntcAlarm;
                int offsetState;
                String info;
                int pogoConnectedState;
                int moistureDet;
                int rxOffset;
                int connectorTemp;
                int penReverseSoc;
                int penReverseChargeState;
                int wirelessFwState;
                int shutdownDelay;
                int quickChargeType;
                int hvdcpType;
                int wirelessTxType;
                if (event.get(BatteryHandler.WIRELESS_TX_TYPE_EVENT) != null && (wirelessTxType = BatteryHandler.this.parseInt(event.get(BatteryHandler.WIRELESS_TX_TYPE_EVENT))) != BatteryHandler.this.mLastWirelessTxType) {
                    Slog.d("MiuiBatteryServiceImpl", "Wireless_tx_type = " + wirelessTxType + " mLastWireless_tx_type = " + BatteryHandler.this.mLastWirelessTxType);
                    BatteryHandler.this.mLastWirelessTxType = wirelessTxType;
                    BatteryHandler.this.sendMessage(1, wirelessTxType);
                }
                if (event.get(BatteryHandler.HVDCP3_TYPE_EVENT) != null && (hvdcpType = BatteryHandler.this.parseInt(event.get(BatteryHandler.HVDCP3_TYPE_EVENT))) != BatteryHandler.this.mLastHvdcpType) {
                    Slog.d("MiuiBatteryServiceImpl", "HVDCP type = " + hvdcpType + " Last HVDCP type = " + BatteryHandler.this.mLastHvdcpType);
                    BatteryHandler.this.mLastHvdcpType = hvdcpType;
                    BatteryHandler.this.sendMessage(2, hvdcpType);
                }
                if (event.get(BatteryHandler.QUICK_CHARGE_TYPE_EVENT) != null && (quickChargeType = BatteryHandler.this.parseInt(event.get(BatteryHandler.QUICK_CHARGE_TYPE_EVENT))) != BatteryHandler.this.mLastQuickChargeType) {
                    Slog.i("MiuiBatteryServiceImpl", "Quick Charge type = " + quickChargeType + " Last Quick Charge type = " + BatteryHandler.this.mLastQuickChargeType);
                    BatteryHandler.this.mLastQuickChargeType = quickChargeType;
                    BatteryHandler.this.sendMessage(3, quickChargeType);
                    BatteryHandler.this.mUpdateSocDecimal = true;
                }
                if (event.get(BatteryHandler.SOC_DECIMAL_EVENT) != null && BatteryHandler.this.mLastQuickChargeType >= 3 && BatteryHandler.this.mUpdateSocDecimal) {
                    int socDecimal = BatteryHandler.this.parseInt(event.get(BatteryHandler.SOC_DECIMAL_EVENT));
                    int socDecimalRate = BatteryHandler.this.parseInt(event.get(BatteryHandler.SOC_DECIMAL_RATE_EVENT));
                    Slog.i("MiuiBatteryServiceImpl", "socDecimal = " + socDecimal + " socDecimalRate = " + socDecimalRate);
                    BatteryHandler.this.sendMessage(4, socDecimal, socDecimalRate);
                    BatteryHandler.this.mUpdateSocDecimal = false;
                }
                if (event.get(BatteryHandler.REVERSE_CHG_STATE_EVENT) != null) {
                    int closeReason = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_CHG_STATE_EVENT));
                    if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && closeReason != BatteryHandler.this.mLastCloseReason) {
                        Slog.d("MiuiBatteryServiceImpl", "Wireless Reverse Charging Closed Reason  = " + closeReason + " Last Wireless Reverse charging closed reason = " + BatteryHandler.this.mLastCloseReason);
                        BatteryHandler.this.mLastCloseReason = closeReason;
                        BatteryHandler.this.sendMessage(5, closeReason);
                    }
                }
                if (event.get(BatteryHandler.REVERSE_CHG_MODE_EVENT) != null) {
                    int openStatus = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_CHG_MODE_EVENT));
                    if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && openStatus != BatteryHandler.this.mLastOpenStatus) {
                        Slog.d("MiuiBatteryServiceImpl", "Wireless Reverse Charing status  = " + openStatus + " Last Wireless Reverse Charing status = " + BatteryHandler.this.mLastOpenStatus);
                        BatteryHandler.this.mLastOpenStatus = openStatus;
                        BatteryHandler.this.sendMessage(6, openStatus);
                    }
                }
                if (event.get(BatteryHandler.SHUTDOWN_DELAY_EVENT) != null && (shutdownDelay = BatteryHandler.this.parseInt(event.get(BatteryHandler.SHUTDOWN_DELAY_EVENT))) != BatteryHandler.this.mLastShutdownDelay) {
                    Slog.d("MiuiBatteryServiceImpl", "shutdown delay status  = " + shutdownDelay + " Last shutdown delay status = " + BatteryHandler.this.mLastShutdownDelay);
                    BatteryHandler.this.mLastShutdownDelay = shutdownDelay;
                    BatteryHandler.this.sendMessage(7, shutdownDelay);
                }
                if (event.get(BatteryHandler.WLS_FW_STATE_EVENT) != null && (wirelessFwState = BatteryHandler.this.parseInt(event.get(BatteryHandler.WLS_FW_STATE_EVENT))) != BatteryHandler.this.mLastWirelessFwState) {
                    Slog.d("MiuiBatteryServiceImpl", "wireless fw update status  = " + wirelessFwState + " Last wireless fw update status = " + BatteryHandler.this.mLastWirelessFwState);
                    BatteryHandler.this.mLastWirelessFwState = wirelessFwState;
                    BatteryHandler.this.sendMessage(11, wirelessFwState);
                }
                if (event.get(BatteryHandler.REVERSE_PEN_CHG_STATE_EVENT) != null && (penReverseChargeState = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_PEN_CHG_STATE_EVENT))) != BatteryHandler.this.mLastPenReverseChargeState) {
                    Slog.d("MiuiBatteryServiceImpl", "current pen reverse charge state = " + penReverseChargeState + " Last pen reverse charge state = " + BatteryHandler.this.mLastPenReverseChargeState);
                    BatteryHandler.this.mLastPenReverseChargeState = penReverseChargeState;
                    BatteryHandler.this.sendMessage(12, penReverseChargeState);
                }
                if (event.get(BatteryHandler.REVERSE_PEN_SOC_EVENT) != null && (penReverseSoc = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_PEN_SOC_EVENT))) != BatteryHandler.this.mLastPenReverseSoc) {
                    Slog.d("MiuiBatteryServiceImpl", "current pen reverse soc = " + penReverseSoc + " Last pen reverse soc = " + BatteryHandler.this.mLastPenReverseSoc);
                    BatteryHandler.this.mLastPenReverseSoc = penReverseSoc;
                }
                if (event.get(BatteryHandler.REVERSE_PEN_MAC_EVENT) != null) {
                    String penReverseMac = event.get(BatteryHandler.REVERSE_PEN_MAC_EVENT);
                    if (!penReverseMac.equals(BatteryHandler.this.mLastPenReverseMac)) {
                        Slog.d("MiuiBatteryServiceImpl", "current pen reverse mac = " + penReverseMac + " Last pen reverse mac = " + BatteryHandler.this.mLastPenReverseMac);
                        BatteryHandler.this.mLastPenReverseMac = penReverseMac;
                    }
                }
                if (event.get(BatteryHandler.REVERSE_PEN_PLACE_ERR_EVENT) != null) {
                    String penReversePlaceErr = event.get(BatteryHandler.REVERSE_PEN_PLACE_ERR_EVENT);
                    if (!penReversePlaceErr.equals(BatteryHandler.this.mLastPenReversePLaceErr)) {
                        Slog.d("MiuiBatteryServiceImpl", "current pen place error = " + penReversePlaceErr + " Last pen place error = " + BatteryHandler.this.mLastPenReversePLaceErr);
                        BatteryHandler.this.mLastPenReversePLaceErr = penReversePlaceErr;
                    }
                }
                if (event.get(BatteryHandler.CONNECTOR_TEMP_EVENT) != null && (connectorTemp = BatteryHandler.this.parseInt(event.get(BatteryHandler.CONNECTOR_TEMP_EVENT))) != BatteryHandler.this.mLastConnectorTemp) {
                    Slog.d("MiuiBatteryServiceImpl", "currenet connector temp = " + connectorTemp + " Last currenet connector temp = " + BatteryHandler.this.mLastConnectorTemp);
                    BatteryHandler.this.mLastConnectorTemp = connectorTemp;
                    if (connectorTemp > 650) {
                        BatteryHandler.this.sendMessage(16, connectorTemp);
                    }
                }
                if (event.get(BatteryHandler.RX_OFFSET_EVENT) != null && (rxOffset = BatteryHandler.this.parseInt(event.get(BatteryHandler.RX_OFFSET_EVENT))) != BatteryHandler.this.mLastRxOffset) {
                    Slog.d("MiuiBatteryServiceImpl", "current rx offset = " + rxOffset + " last rx offset = " + BatteryHandler.this.mLastRxOffset);
                    BatteryHandler.this.mLastRxOffset = rxOffset;
                    BatteryHandler.this.sendMessage(17, rxOffset);
                }
                if (event.get(BatteryHandler.MOISTURE_DET) != null && (moistureDet = BatteryHandler.this.parseInt(event.get(BatteryHandler.MOISTURE_DET))) != BatteryHandler.this.mLastMoistureDet) {
                    Slog.d("MiuiBatteryServiceImpl", "current moistureDet = " + moistureDet + " last moistureDet = " + BatteryHandler.this.mLastMoistureDet);
                    BatteryHandler.this.mLastMoistureDet = moistureDet;
                    BatteryHandler.this.sendMessage(21, moistureDet);
                }
                if (event.get(BatteryHandler.CAR_APP_STATE_EVENT) != null && (pogoConnectedState = BatteryHandler.this.parseInt(event.get(BatteryHandler.CAR_APP_STATE_EVENT))) != BatteryHandler.this.mLastPogoConnectedState) {
                    Slog.d("MiuiBatteryServiceImpl", "pogo charging connected sate = " + pogoConnectedState + " last pog charging connected state = " + BatteryHandler.this.mLastPogoConnectedState);
                    BatteryHandler.this.mLastPogoConnectedState = pogoConnectedState;
                    BatteryHandler.this.sendMessage(22, pogoConnectedState);
                }
                if (event.get(BatteryHandler.HANDLE_BATTERY_CHANGED) != null && BatteryHandler.this.mHandleInfo != null && (info = event.get(BatteryHandler.HANDLE_BATTERY_CHANGED)) != null && info.equals(BatteryHandler.this.mHandleInfo.mHidName) && event.get(BatteryHandler.POWER_SUPPLY_CAPACITY) != null) {
                    int level = BatteryHandler.this.parseInt(event.get(BatteryHandler.POWER_SUPPLY_CAPACITY));
                    String state = event.get(BatteryHandler.POWER_SUPPLY_STATUS);
                    if (BatteryHandler.this.mHandleInfo.mBatteryLevel != level || (state != null && !state.equals(BatteryHandler.this.mHandleInfo.mBatteryStats))) {
                        Slog.d("MiuiBatteryServiceImpl", "handle battery changed, state = " + state + " and level = " + level);
                        BatteryHandler.this.mHandleInfo.setBatteryLevel(level);
                        BatteryHandler.this.mHandleInfo.setBatteryStats(state);
                        BatteryHandler.this.sendHandleBatteryStatsChangeBroadcast();
                    }
                }
                if (event.get(BatteryHandler.LOW_INDUCTANCE_OFFSET_EVENT) != null && (offsetState = BatteryHandler.this.parseInt(event.get(BatteryHandler.LOW_INDUCTANCE_OFFSET_EVENT))) != BatteryHandler.this.mLastOffsetState) {
                    Slog.d("MiuiBatteryServiceImpl", "Low Inductance offset state = " + offsetState + " Last Low Inductance offset state = " + BatteryHandler.this.mLastOffsetState);
                    BatteryHandler.this.mLastOffsetState = offsetState;
                    BatteryHandler.this.sendEmptyMessage(27);
                }
                if (event.get(BatteryHandler.NTC_ALARM_EVENT) != null && (ntcAlarm = BatteryHandler.this.parseInt(event.get(BatteryHandler.NTC_ALARM_EVENT))) != BatteryHandler.this.mLastNtcAlarm) {
                    Slog.d("MiuiBatteryServiceImpl", "current ntc alarm = " + ntcAlarm + " last ntc alarm = " + BatteryHandler.this.mLastNtcAlarm);
                    BatteryHandler.this.mLastNtcAlarm = ntcAlarm;
                    BatteryHandler.this.sendEmptyMessage(28);
                }
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            NfcAdapter nfcAdapter;
            switch (msg.what) {
                case 0:
                    int wirelessTxType = msg.arg1;
                    shouldCloseWirelessReverseCharging(wirelessTxType);
                    return;
                case 1:
                    int wirelessTxType2 = msg.arg1;
                    Intent wirelessTxTypeIntent = new Intent("miui.intent.action.ACTION_WIRELESS_TX_TYPE");
                    wirelessTxTypeIntent.putExtra("miui.intent.extra.wireless_tx_type", wirelessTxType2);
                    sendStickyBroadcast(wirelessTxTypeIntent);
                    this.mIsReverseWirelessCharge = false;
                    this.mRetryAfterOneMin = false;
                    return;
                case 2:
                    Intent hvdcpTypeIntent = new Intent("miui.intent.action.ACTION_HVDCP_TYPE");
                    hvdcpTypeIntent.putExtra("miui.intent.extra.hvdcp_type", msg.arg1);
                    sendStickyBroadcast(hvdcpTypeIntent);
                    return;
                case 3:
                    Intent quickChargeTypeIntent = new Intent("miui.intent.action.ACTION_QUICK_CHARGE_TYPE");
                    quickChargeTypeIntent.putExtra("miui.intent.extra.quick_charge_type", msg.arg1);
                    quickChargeTypeIntent.putExtra(EXTRA_POWER_MAX, getChargingPowerMax());
                    quickChargeTypeIntent.putExtra(EXTRA_CAR_CHG, getCarChargingType());
                    sendStickyBroadcast(quickChargeTypeIntent);
                    if (msg.arg1 >= 3) {
                        sendSocDecimaBroadcast();
                        return;
                    }
                    return;
                case 4:
                    Intent socDecimalIntent = new Intent("miui.intent.action.ACTION_SOC_DECIMAL");
                    socDecimalIntent.putExtra("miui.intent.extra.soc_decimal", msg.arg1);
                    socDecimalIntent.putExtra("miui.intent.extra.soc_decimal_rate", msg.arg2);
                    sendStickyBroadcast(socDecimalIntent);
                    return;
                case 5:
                    int openStatus = msg.arg1;
                    if (openStatus == 1) {
                        updateWirelessReverseChargingNotification(1);
                        return;
                    } else if (openStatus == 2) {
                        updateWirelessReverseChargingNotification(2);
                        return;
                    } else {
                        if (openStatus == 3) {
                            showWirelessCharingWarningDialog();
                            return;
                        }
                        return;
                    }
                case 6:
                    int openStatus2 = msg.arg1;
                    if (openStatus2 > 0) {
                        updateWirelessReverseChargingNotification(0);
                    }
                    this.mIsReverseWirelessCharge = true;
                    sendMessage(openStatus2 > 0 ? 9 : 8, 0);
                    return;
                case 7:
                    Intent shutdownDelayIntent = new Intent("miui.intent.action.ACTION_SHUTDOWN_DELAY");
                    shutdownDelayIntent.putExtra("miui.intent.extra.shutdown_delay", msg.arg1);
                    sendStickyBroadcast(shutdownDelayIntent);
                    return;
                case 8:
                    int txType = msg.arg1;
                    this.mClosedNfcFromCharging = Settings.Global.getInt(this.mContentResolver, NFC_CLOED, 0) > 0;
                    try {
                        this.mNfcAdapter = NfcAdapter.getNfcAdapter(MiuiBatteryServiceImpl.this.mContext);
                    } catch (UnsupportedOperationException e) {
                        Slog.e("MiuiBatteryServiceImpl", "Get NFC failed");
                    }
                    if (txType == 0 && this.mClosedNfcFromCharging && (nfcAdapter = this.mNfcAdapter) != null && !nfcAdapter.isEnabled()) {
                        if (this.mNfcAdapter.enable()) {
                            Slog.d("MiuiBatteryServiceImpl", "try to open NFC " + this.mCount + " times success");
                            this.mClosedNfcFromCharging = false;
                            this.mShowEnableNfc = true;
                            this.mCount = 0;
                            Settings.Global.putInt(this.mContentResolver, NFC_CLOED, 0);
                        } else {
                            int i = this.mCount;
                            if (i < 3) {
                                this.mCount = i + 1;
                                sendMessageDelayed(8, 1000L);
                            } else {
                                Slog.d("MiuiBatteryServiceImpl", "open NFC failed");
                                this.mCount = 0;
                                if (!this.mRetryAfterOneMin) {
                                    sendMessageDelayed(8, AccessControlImpl.LOCK_TIME_OUT);
                                    this.mRetryAfterOneMin = true;
                                }
                            }
                        }
                    }
                    if (!this.mIsReverseWirelessCharge) {
                        if (this.mShowEnableNfc) {
                            Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196193, 0).show();
                            this.mShowEnableNfc = false;
                            return;
                        }
                        return;
                    }
                    if (isSupportControlHaptic()) {
                        boolean z = Settings.System.getIntForUser(this.mContentResolver, HAPTIC_STATE, 0, 0) > 0;
                        this.mHapticState = z;
                        if (z) {
                            Slog.d("MiuiBatteryServiceImpl", "open haptic when wireless reverse charge");
                            Settings.System.putIntForUser(this.mContentResolver, HAPTIC_STATE, 0, 0);
                        }
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196194, 0).show();
                        return;
                    }
                    if (this.mShowEnableNfc) {
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196195, 0).show();
                        this.mShowEnableNfc = false;
                        return;
                    }
                    return;
                case 9:
                    try {
                        this.mNfcAdapter = NfcAdapter.getNfcAdapter(MiuiBatteryServiceImpl.this.mContext);
                    } catch (UnsupportedOperationException e2) {
                        Slog.e("MiuiBatteryServiceImpl", "Get NFC failed");
                    }
                    NfcAdapter nfcAdapter2 = this.mNfcAdapter;
                    if (nfcAdapter2 != null && nfcAdapter2.isEnabled()) {
                        if (this.mNfcAdapter.disable()) {
                            Settings.Global.putInt(this.mContentResolver, NFC_CLOED, 1);
                            this.mClosedNfcFromCharging = true;
                            this.mShowDisableNfc = true;
                        } else {
                            Slog.e("MiuiBatteryServiceImpl", "close NFC failed");
                        }
                    }
                    if (!this.mIsReverseWirelessCharge) {
                        if (this.mShowDisableNfc) {
                            Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196175, 0).show();
                            this.mShowDisableNfc = false;
                            return;
                        }
                        return;
                    }
                    if (isSupportControlHaptic()) {
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196176, 0).show();
                        return;
                    } else {
                        if (this.mShowDisableNfc) {
                            Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196177, 0).show();
                            this.mShowDisableNfc = false;
                            return;
                        }
                        return;
                    }
                case 10:
                    int bluetoothState = msg.arg1;
                    MiuiBatteryServiceImpl.this.mMiCharge.setBtTransferStartState(bluetoothState);
                    return;
                case 11:
                    Intent wirelessFwIntent = new Intent(ACTION_WIRELESS_FW_UPDATE);
                    wirelessFwIntent.putExtra(EXTRA_WIRELESS_FW_UPDATE, msg.arg1);
                    sendStickyBroadcast(wirelessFwIntent);
                    return;
                case 12:
                    Intent penReverseChgStateIntent = new Intent(ACTION_REVERSE_PEN_CHARGE_STATE);
                    penReverseChgStateIntent.putExtra(EXTRA_REVERSE_PEN_CHARGE_STATE, msg.arg1);
                    penReverseChgStateIntent.putExtra(EXTRA_REVERSE_PEN_SOC, getPSValue());
                    sendStickyBroadcast(penReverseChgStateIntent);
                    return;
                case 13:
                    showPowerOffWarningDialog();
                    sendMessageDelayed(14, 30000L);
                    return;
                case 14:
                    Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                    intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                    intent.setFlags(268435456);
                    MiuiBatteryServiceImpl.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return;
                case 15:
                    try {
                        adjustVoltageFromTimeRegion();
                        return;
                    } catch (ParseException e3) {
                        e3.printStackTrace();
                        return;
                    }
                case 16:
                    Intent connectorTempIntent = new Intent(ACTION_TYPE_C_HIGH_TEMP);
                    connectorTempIntent.putExtra(EXTRA_TYPE_C_HIGH_TEMP, msg.arg1);
                    sendStickyBroadcast(connectorTempIntent);
                    return;
                case 17:
                    Intent rxOffsetIntent = new Intent(ACTION_RX_OFFSET);
                    rxOffsetIntent.putExtra(EXTRA_RX_OFFSET, msg.arg1);
                    sendStickyBroadcast(rxOffsetIntent);
                    return;
                case 18:
                    MiuiBatteryServiceImpl.this.mMiCharge.setMiChargePath("screen_unlock", "1");
                    return;
                case 19:
                    MiuiBatteryServiceImpl.this.mMiCharge.setMiChargePath("screen_unlock", "0");
                    return;
                case 20:
                    Intent j = (Intent) msg.obj;
                    adjustVoltageFromStatsBroadcast(j);
                    return;
                case 21:
                    Intent moistureDetIntent = new Intent(ACTION_MOISTURE_DET);
                    moistureDetIntent.putExtra(EXTRA_MOISTURE_DET, msg.arg1);
                    sendStickyBroadcast(moistureDetIntent);
                    return;
                case 22:
                    Intent pogoConnectedIntent = new Intent(ACTION_POGO_CONNECTED_STATE);
                    pogoConnectedIntent.putExtra(EXTRA_POGO_CONNECTED_STATE, msg.arg1);
                    sendStickyBroadcast(pogoConnectedIntent);
                    return;
                case 23:
                    UsbDevice au = (UsbDevice) msg.obj;
                    checkSpecialHandle(au);
                    return;
                case 24:
                    HandleInfo handleInfo = this.mHandleInfo;
                    if (handleInfo != null) {
                        handleInfo.setConnectState(0);
                        sendHandleStateChangeBroadcast();
                        sendHandleBatteryStatsChangeBroadcast();
                    }
                    MiuiBatteryServiceImpl.this.mLastPhoneBatteryLevel = -1;
                    this.mHandleInfo = null;
                    return;
                case 25:
                    handleConnectReboot();
                    return;
                case 26:
                    int batteryLevel = msg.arg1;
                    if (batteryLevel >= 75) {
                        MiuiBatteryServiceImpl.this.mMiCharge.setInputSuspendState("1");
                        return;
                    } else {
                        if (batteryLevel <= 40) {
                            MiuiBatteryServiceImpl.this.mMiCharge.setInputSuspendState("0");
                            return;
                        }
                        return;
                    }
                case 27:
                case 28:
                    updateMiuiPCBatteryChanged();
                    return;
                case 29:
                    handleTempState();
                    return;
                default:
                    Slog.d("MiuiBatteryServiceImpl", "NO Message");
                    return;
            }
        }

        private int getChargingPowerMax() {
            String powerMax = MiuiBatteryServiceImpl.this.mMiCharge.getChargingPowerMax();
            if (powerMax != null && powerMax.length() != 0) {
                return parseInt(powerMax);
            }
            return -1;
        }

        private int getCarChargingType() {
            String carCharging = MiuiBatteryServiceImpl.this.mMiCharge.getCarChargingType();
            if (carCharging != null && carCharging.length() != 0) {
                return parseInt(carCharging);
            }
            return -1;
        }

        private int getPSValue() {
            String penSoc = MiuiBatteryServiceImpl.this.mMiCharge.getPSValue();
            if (penSoc != null && penSoc.length() != 0) {
                return parseInt(penSoc);
            }
            return -1;
        }

        private int getSBState() {
            String smartBatt = MiuiBatteryServiceImpl.this.mMiCharge.getSBState();
            if (smartBatt != null && smartBatt.length() != 0) {
                return parseInt(smartBatt);
            }
            return -1;
        }

        private void sendStickyBroadcast(Intent intent) {
            intent.addFlags(822083584);
            MiuiBatteryServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendBroadcast(Intent intent) {
            intent.addFlags(822083584);
            MiuiBatteryServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateMiuiPCBatteryChanged() {
            Intent intent = new Intent(ACTION_MIUI_PC_BATTERY_CHANGED);
            intent.putExtra(EXTRA_LOW_TX_OFFESET_STATE, this.mLastOffsetState);
            intent.putExtra(EXTRA_NTC_ALARM, this.mLastNtcAlarm);
            intent.addFlags(822083584);
            MiuiBatteryServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, POWER_COMMON_RECEIVER_PERMISSION);
        }

        private void showWirelessCharingWarningDialog() {
            Intent dialogIntent = new Intent(ACTION_WIRELESS_CHG_WARNING_ACTIVITY);
            dialogIntent.addFlags(268435456);
            dialogIntent.putExtra("plugstatus", 4);
            MiuiBatteryServiceImpl.this.mContext.startActivity(dialogIntent);
            sendUpdateStatusBroadCast(1);
        }

        private void sendUpdateStatusBroadCast(int status) {
            Intent intent = new Intent(ACTION_WIRELESS_CHARGING);
            intent.addFlags(822083584);
            intent.putExtra(EXTRA_WIRELESS_CHARGING, status);
            MiuiBatteryServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateWirelessReverseChargingNotification(int closedReason) {
            int messageRes = 0;
            Resources r = MiuiBatteryServiceImpl.this.mContext.getResources();
            CharSequence title = r.getText(286196760);
            NotificationManager notificationManager = (NotificationManager) MiuiBatteryServiceImpl.this.mContext.getSystemService("notification");
            if (notificationManager == null) {
                Slog.d("MiuiBatteryServiceImpl", "get notification service failed");
                return;
            }
            if (closedReason == 1) {
                messageRes = 286196757;
            } else if (closedReason == 4) {
                messageRes = 286196758;
            } else if (closedReason == 2) {
                messageRes = 286196759;
            }
            int i = this.mChargingNotificationId;
            if (i != 0) {
                notificationManager.cancelAsUser(null, i, UserHandle.ALL);
                Slog.d("MiuiBatteryServiceImpl", "Clear notification");
                this.mChargingNotificationId = 0;
            }
            if (messageRes != 0) {
                Notification.Builder builder = new Notification.Builder(MiuiBatteryServiceImpl.this.mContext, SystemNotificationChannels.USB).setSmallIcon(R.drawable.tab_selected_pressed_holo).setWhen(0L).setOngoing(false).setTicker(title).setDefaults(0).setColor(MiuiBatteryServiceImpl.this.mContext.getColor(R.color.system_notification_accent_color)).setContentTitle(title).setVisibility(1);
                if (closedReason == 4) {
                    String messageString = r.getString(messageRes, NumberFormat.getPercentInstance().format(Settings.Global.getInt(this.mContentResolver, WIRELESS_REVERSE_CHARGING, 30) / 100.0f));
                    builder.setContentText(messageString);
                } else {
                    CharSequence message = r.getText(messageRes);
                    builder.setContentText(message);
                }
                Notification notification = builder.build();
                notificationManager.notifyAsUser(null, messageRes, notification, UserHandle.ALL);
                Slog.d("MiuiBatteryServiceImpl", "push notification:" + ((Object) title));
                this.mChargingNotificationId = messageRes;
            }
            if (closedReason == 4) {
                MiuiBatteryServiceImpl.this.mMiCharge.setWirelessChargingEnabled(false);
            } else if (closedReason == 0) {
                return;
            }
            sendUpdateStatusBroadCast(1);
        }

        private void shouldCloseWirelessReverseCharging(int batteryLevel) {
            if (batteryLevel < Settings.Global.getInt(this.mContentResolver, WIRELESS_REVERSE_CHARGING, 30)) {
                updateWirelessReverseChargingNotification(4);
            }
        }

        private void showPowerOffWarningDialog() {
            AlertDialog powerOffDialog = new AlertDialog.Builder(ActivityThread.currentActivityThread().getSystemUiContext(), 1712390150).setCancelable(false).setTitle(286195824).setMessage(MiuiBatteryServiceImpl.this.mContext.getResources().getQuantityString(286064640, 30, 30)).setPositiveButton(286195823, new DialogInterface.OnClickListener() { // from class: com.android.server.MiuiBatteryServiceImpl.BatteryHandler.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create();
            powerOffDialog.getWindow().setType(2010);
            powerOffDialog.show();
        }

        private void adjustVoltageFromTimeRegion() throws ParseException {
            int smartBatt = getSBState();
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "smartBatt = " + smartBatt);
            }
            if (isDateOfHighTemp() && isInTragetCountry() && smartBatt == 0) {
                MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition = true;
                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(10);
            } else if ((!isDateOfHighTemp() || !isInTragetCountry()) && smartBatt == 10) {
                MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition = false;
                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(0);
            }
        }

        private void adjustVoltageFromStatsBroadcast(Intent intent) {
            int smartBatt = MiuiBatteryServiceImpl.this.mHandler.getSBState();
            MiuiBatteryServiceImpl.this.mIsSatisfyTempSocCondition = intent.getBooleanExtra(MiuiBatteryStatsService.ADJUST_VOLTAGE_TS_EXTRA, false);
            MiuiBatteryServiceImpl.this.mIsSatisfyTempLevelCondition = intent.getBooleanExtra(MiuiBatteryStatsService.ADJUST_VOLTAGE_TL_EXTRA, false);
            if (MiuiBatteryServiceImpl.this.mIsSatisfyTempLevelCondition) {
                if (smartBatt != 20) {
                    MiuiBatteryServiceImpl.this.mMiCharge.setSBState(20);
                }
            } else if (MiuiBatteryServiceImpl.this.mIsSatisfyTempSocCondition) {
                if (smartBatt != 15) {
                    MiuiBatteryServiceImpl.this.mMiCharge.setSBState(15);
                }
            } else if (MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition) {
                if (smartBatt != 10) {
                    MiuiBatteryServiceImpl.this.mMiCharge.setSBState(10);
                }
            } else if (smartBatt != 0) {
                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(0);
            }
        }

        private boolean isDateOfHighTemp() throws ParseException {
            this.mStartHithTempDate = parseDate("06-15");
            this.mEndHighTempDate = parseDate("09-15");
            Date currentDate = parseDate(getCurrentDate());
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "currentDate = " + currentDate);
            }
            if (currentDate.getTime() >= this.mStartHithTempDate.getTime() && currentDate.getTime() <= this.mEndHighTempDate.getTime()) {
                return true;
            }
            return false;
        }

        private boolean isInChina() {
            TelephonyManager tel = (TelephonyManager) MiuiBatteryServiceImpl.this.mContext.getSystemService("phone");
            String networkOperator = tel.getNetworkOperator();
            if (!TextUtils.isEmpty(networkOperator)) {
                if (MiuiBatteryServiceImpl.this.DEBUG) {
                    Slog.d("MiuiBatteryServiceImpl", "networkOperator = " + networkOperator);
                }
                return networkOperator.startsWith("460");
            }
            return false;
        }

        private boolean isInTragetCountry() {
            if (isInChina()) {
                return true;
            }
            if (!SystemProperties.get("ro.product.mod_device", "").startsWith("taoyao") && !SystemProperties.getBoolean("persist.vendor.domain.charge", false)) {
                return false;
            }
            String value = SystemProperties.get("ro.miui.region", "");
            return Arrays.asList(MiuiBatteryServiceImpl.this.SUPPORT_COUNTRY).contains(value);
        }

        private void checkSpecialHandle(UsbDevice usbDevice) {
            if (usbDevice == null) {
                return;
            }
            int num = usbDevice.getConfiguration(0).getInterface(0).getInterfaceProtocol();
            String address = getDecrpytedAddress(num);
            String encryptedText = usbDevice.getSerialNumber();
            if (TextUtils.isEmpty(encryptedText)) {
                Slog.d("MiuiBatteryServiceImpl", "getSerialNumber Failed");
                return;
            }
            try {
                byte[] key = MiuiBatteryServiceImpl.AES_KEY.getBytes("US-ASCII");
                Cipher ciper = Cipher.getInstance("AES/ECB/NoPadding");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
                ciper.init(2, keySpec);
                byte[] encrypted = hexStringToByteArray(encryptedText);
                byte[] decrypted = ciper.doFinal(encrypted);
                if ((MiuiBatteryServiceImpl.AES_DECRYPTED_TEXT + address).equals(new String(decrypted, StandardCharsets.UTF_8))) {
                    this.mHandleInfo = new HandleInfo(usbDevice);
                    Slog.d("MiuiBatteryServiceImpl", "Decryption successful");
                    this.mHandleInfo.setColorNumber(MiuiBatteryServiceImpl.this.mMiCharge.getTypeCCommonInfo("getHandleColor"));
                    this.mHandleInfo.setConnectState(1);
                    sendHandleStateChangeBroadcast();
                }
            } catch (Exception e) {
                Slog.e("MiuiBatteryServiceImpl", "decrypted exception = " + e);
            }
        }

        private void handleConnectReboot() {
            UsbManager usbManager = (UsbManager) MiuiBatteryServiceImpl.this.mContext.getSystemService("usb");
            if (usbManager.getDeviceList() == null) {
                return;
            }
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                if (device.getProductId() == MiuiBatteryServiceImpl.HANDLE_PRODUCT_ID && device.getVendorId() == MiuiBatteryServiceImpl.HANDLE_VENDOR_ID) {
                    Slog.d("MiuiBatteryServiceImpl", "miHandle Attached when reboot");
                    checkSpecialHandle(device);
                    return;
                }
            }
        }

        private void sendHandleStateChangeBroadcast() {
            if (this.mHandleInfo == null) {
                Slog.d("MiuiBatteryServiceImpl", "faild to get handleInfo");
                return;
            }
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "connect changed info = " + this.mHandleInfo);
            }
            Intent handleBroadcast = new Intent(ACTION_HANDLE_STATE_CHANGED);
            handleBroadcast.putExtra(EXTRA_HANDLE_CONNECT_STATE, this.mHandleInfo.mConnectState);
            handleBroadcast.putExtra(EXTRA_HANDLE_VERSION, this.mHandleInfo.mFwVersion);
            handleBroadcast.putExtra("vid", this.mHandleInfo.mVid);
            handleBroadcast.putExtra("pid", this.mHandleInfo.mPid);
            handleBroadcast.putExtra("ColorNumber", this.mHandleInfo.mColorNumber);
            sendBroadcast(handleBroadcast);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void sendHandleBatteryStatsChangeBroadcast() {
            if (this.mHandleInfo == null) {
                Slog.d("MiuiBatteryServiceImpl", "faild to get handleInfo");
                return;
            }
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "batteryInfo changed info = " + this.mHandleInfo);
            }
            Intent handleBatteryChangeBroadcast = new Intent(ACTION_HANDLE_BATTERY_STATE_CHANGED);
            handleBatteryChangeBroadcast.putExtra(EXTRA_HANDLE_CONNECT_STATE, this.mHandleInfo.mConnectState);
            handleBatteryChangeBroadcast.putExtra("batteryLevel", this.mHandleInfo.mBatteryLevel);
            handleBatteryChangeBroadcast.putExtra("batteryStats", this.mHandleInfo.mBatteryStats);
            sendStickyBroadcast(handleBatteryChangeBroadcast);
        }

        public byte[] hexStringToByteArray(String s) {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                try {
                    data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid hex string: " + s);
                    return null;
                }
            }
            return data;
        }

        private String getDecrpytedAddress(int num) {
            if (num < 10) {
                return Integer.toString(num * 10);
            }
            String address = new StringBuilder(Integer.toString(num)).reverse().substring(0, 2).toString();
            return address;
        }

        boolean isHandleConnect() {
            HandleInfo handleInfo = this.mHandleInfo;
            return handleInfo != null && handleInfo.mConnectState == 1;
        }

        private int getOneHundredThousandDigits() {
            int tempState = Settings.Secure.getIntForUser(MiuiBatteryServiceImpl.this.mContext.getContentResolver(), MiuiBatteryServiceImpl.KEY_SETTING_TEMP_STATE, 0, -2);
            return (tempState / 100000) % 10;
        }

        private boolean isBatteryHighTemp() {
            return Settings.Secure.getIntForUser(this.mContentResolver, MiuiBatteryServiceImpl.KEY_BATTERY_TEMP_ALLOW_KILL, 0, 0) == 1;
        }

        private void handleTempState() {
            int tempStateValue = getOneHundredThousandDigits();
            boolean isBatteryHighTemp = isBatteryHighTemp();
            if (isBatteryHighTemp() && tempStateValue == 5) {
                if (!MiuiBatteryServiceImpl.this.mIsStopCharge) {
                    MiuiBatteryServiceImpl.this.mMiCharge.setMiChargePath("set_rx_sleep", "1");
                }
                MiuiBatteryServiceImpl.this.mIsStopCharge = true;
            } else if (tempStateValue != 5) {
                if (MiuiBatteryServiceImpl.this.mIsStopCharge) {
                    MiuiBatteryServiceImpl.this.mMiCharge.setMiChargePath("set_rx_sleep", "0");
                }
                MiuiBatteryServiceImpl.this.mIsStopCharge = false;
            }
            Slog.d("MiuiBatteryServiceImpl", "tempStateValue = " + tempStateValue + ", isBatteryHighTemp = " + isBatteryHighTemp);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class HandleInfo {
        int mBatteryLevel;
        String mBatteryStats;
        String mColorNumber;
        int mConnectState;
        String mFwVersion;
        String mHidName;
        int mPid;
        int mVid;

        public HandleInfo(UsbDevice usbDevice) {
            this.mPid = usbDevice.getProductId();
            this.mVid = usbDevice.getVendorId();
            this.mFwVersion = usbDevice.getVersion();
            this.mHidName = "hid-" + usbDevice.getSerialNumber() + "-battery";
        }

        void setConnectState(int connectState) {
            this.mConnectState = connectState;
        }

        void setBatteryLevel(int level) {
            this.mBatteryLevel = level;
        }

        void setBatteryStats(String batteryStats) {
            this.mBatteryStats = batteryStats;
        }

        void setColorNumber(String color) {
            this.mColorNumber = color;
        }

        public String toString() {
            return "HandleInfo{mConnectState=" + this.mConnectState + ", mPid=" + this.mPid + ", mVid=" + this.mVid + ", mBatteryLevel=" + this.mBatteryLevel + ", mHidName='" + this.mHidName + "', mBatteryStats='" + this.mBatteryStats + "', mFwVersion='" + this.mFwVersion + "', mColorNumber='" + this.mColorNumber + "'}";
        }
    }
}
