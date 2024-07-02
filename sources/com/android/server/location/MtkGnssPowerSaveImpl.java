package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.location.mnlutils.MnlConfigUtils;
import com.android.server.location.mnlutils.bean.MnlConfig;
import com.android.server.location.mnlutils.bean.MnlConfigFeature;
import com.miui.base.MiuiStubRegistry;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class MtkGnssPowerSaveImpl implements MtkGnssPowerSaveStub {
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_POWER_SAVE_MODE_CHANGED = "miui.intent.action.POWER_SAVE_MODE_CHANGED";
    private static final String CLOUD_KEY_MTK_DISABLE_L5 = "disableL5";
    private static final String CLOUD_KEY_MTK_DISABLE_SATELLITE = "disableSatellite";
    private static final String CLOUD_MODULE_MTK_GNSS_CONFIG = "mtkGnssConfig";
    private static final String DISABLE_L5_MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
    private static final int GNSS_BD_GPS_QZSS = 1;
    private static final int GNSS_MODE_BD_GPS_GA_GL_QZSS = 6;
    private static final String KEY_POWER_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    private static final String MI_MNL_CONFIG_KEY_L1_ONLY_ENABLE = "l1OnlyEnable";
    private static final String MI_MNL_CONFIG_KEY_MNL_VERSION = "mnlVersion";
    private static final String MI_MNL_CONFIG_KEY_SMART_SWITCH_ENABLE = "smartSwitchEnable";
    private static final int MSG_DISABLE_L5_AND_SATELLITE = 2;
    private static final int MSG_RECOVERY_SATELLITE = 0;
    private static final int MSG_SWITCH_SATELLITE_FOR_L1_ONLY_DEVICE = 1;
    private static final String MTK_GNSS_CONFIG_SUPPORT_DISABLE_L5_PROP = "persist.sys.gps.support_disable_l5_config";
    private static final String MTK_GNSS_CONFIG_SUPPORT_DISABLE_SATELLITE_PROP = "persist.sys.gps.support_disable_satellite_config";
    private static final String MTK_GNSS_CONFIG_SUPPORT_L5_PROP = "vendor.debug.gps.support.l5";
    private static final String MTK_GNSS_CONFIG_SUPPORT_PROP = "persist.sys.gps.support_gnss_config";
    private static final String XM_MTK_GNSS_CONF_DISABLE_L5_CONF = "xiaomi_gnss_config_disable_l5";
    private static final int XM_MTK_GNSS_CONF_DISABLE_L5_OFF = 2;
    private static final int XM_MTK_GNSS_CONF_DISABLE_L5_ON = 1;
    private static final String XM_MTK_GNSS_CONF_SMART_SWITCH_CONF = "xiaomi_gnss_smart_switch";
    private static final int XM_MTK_GNSS_CONF_SMART_SWITCH_OFF = 2;
    private static final int XM_MTK_GNSS_CONF_SMART_SWITCH_ON = 1;
    private BroadcastReceiver bootCompletedReceiver;
    private LocationManager locationManager;
    private Context mContext;
    private Handler mHandler;
    private MnlConfigUtils mnlConfigUtils;
    private ContentObserver mtkGnssDisableL5Obs;
    private ContentObserver mtkGnssSmartSwitchObs;
    private BroadcastReceiver powerSaveReceiver;
    private String TAG = "Glp-MtkGnssPowerSaveImpl";
    private final String MNL_L1_ONLY_FEATURE_NAME = "L1Only";
    private final String MNL_L1_ONLY_FORMAT = "L1 only";
    private final String MNL_GLP_FEATURE_NAME = "GLP";
    private final String MNL_GNSS_MODE_FEATURE_NAME = "GnssMode";
    private final String MNL_DGPSMode_FEATURE_NAME = "DGPSMode";
    private boolean startControllerListener = false;
    private boolean isSmartSwitchEnableFlag = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MtkGnssPowerSaveImpl> {

        /* compiled from: MtkGnssPowerSaveImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MtkGnssPowerSaveImpl INSTANCE = new MtkGnssPowerSaveImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MtkGnssPowerSaveImpl m1800provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MtkGnssPowerSaveImpl m1799provideNewInstance() {
            return new MtkGnssPowerSaveImpl();
        }
    }

    public boolean supportMtkGnssConfig() {
        registerControlListener();
        return isCnVersion() && SystemProperties.getBoolean(MTK_GNSS_CONFIG_SUPPORT_PROP, false);
    }

    private boolean isCnVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get("ro.miui.region"));
    }

    private synchronized void registerControlListener() {
        if (this.startControllerListener) {
            return;
        }
        Context context = this.mContext;
        if (context == null) {
            Log.e(this.TAG, "no context");
            return;
        }
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(null) { // from class: com.android.server.location.MtkGnssPowerSaveImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MtkGnssPowerSaveImpl.this.updateDisableL5CloudConfig();
                MtkGnssPowerSaveImpl.this.updateDisableSatelliteCloudConfig();
            }
        });
        Log.i(this.TAG, "register cloud controller listener");
        this.startControllerListener = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisableL5CloudConfig() {
        boolean settingsNow = SystemProperties.getBoolean(MTK_GNSS_CONFIG_SUPPORT_DISABLE_L5_PROP, false);
        String newSettings = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUD_MODULE_MTK_GNSS_CONFIG, CLOUD_KEY_MTK_DISABLE_L5, (String) null);
        if (newSettings == null) {
            return;
        }
        Log.i(this.TAG, "receive l5 config, new value: " + newSettings + ", settingsNow: " + settingsNow);
        boolean newSettingsBool = Boolean.parseBoolean(newSettings);
        if (newSettingsBool == settingsNow) {
            return;
        }
        SystemProperties.set(MTK_GNSS_CONFIG_SUPPORT_DISABLE_L5_PROP, String.valueOf(newSettingsBool));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisableSatelliteCloudConfig() {
        boolean settingsNow = SystemProperties.getBoolean(MTK_GNSS_CONFIG_SUPPORT_DISABLE_SATELLITE_PROP, false);
        String newSettings = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUD_MODULE_MTK_GNSS_CONFIG, CLOUD_KEY_MTK_DISABLE_SATELLITE, (String) null);
        if (newSettings == null) {
            return;
        }
        Log.i(this.TAG, "receive constellation config, new value: " + newSettings + ", settingsNow: " + settingsNow);
        boolean newSettingsBool = Boolean.parseBoolean(newSettings);
        if (newSettingsBool == settingsNow) {
            return;
        }
        SystemProperties.set(MTK_GNSS_CONFIG_SUPPORT_DISABLE_SATELLITE_PROP, String.valueOf(newSettingsBool));
    }

    public void startPowerSaveListener(Context context) {
        this.mContext = context;
        this.mHandler = new SmartSatelliteHandler(Looper.myLooper());
        if (this.bootCompletedReceiver == null) {
            this.bootCompletedReceiver = new BootCompletedReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_BOOT_COMPLETED);
            context.registerReceiver(this.bootCompletedReceiver, intentFilter);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public synchronized String getMtkGnssConfig(String name) {
        char c;
        try {
            if (this.mnlConfigUtils == null) {
                this.mnlConfigUtils = new MnlConfigUtils();
            }
            MnlConfig mnlConfig = this.mnlConfigUtils.getMnlConfig();
            if (mnlConfig == null) {
                return null;
            }
            switch (name.hashCode()) {
                case 64585581:
                    if (name.equals(MI_MNL_CONFIG_KEY_MNL_VERSION)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 647272308:
                    if (name.equals(MI_MNL_CONFIG_KEY_L1_ONLY_ENABLE)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 927995456:
                    if (name.equals(MI_MNL_CONFIG_KEY_SMART_SWITCH_ENABLE)) {
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
                    return mnlConfig.getVersion();
                case 1:
                    return String.valueOf(isL1OnlyEnable(mnlConfig));
                case 2:
                    return String.valueOf(isSmartSwitchEnable(mnlConfig));
                default:
                    return null;
            }
        } catch (Exception e) {
            Log.d(this.TAG, e.getMessage());
            return null;
        }
    }

    public synchronized void activeSmartSwitch(int gnssMode) {
        try {
            if (this.mnlConfigUtils == null) {
                this.mnlConfigUtils = new MnlConfigUtils();
            }
            MnlConfig mnlConfig = this.mnlConfigUtils.getMnlConfig();
            if (mnlConfig != null) {
                MnlConfigFeature SmartSwitchFeature = mnlConfig.getFeature("GnssMode");
                setSmartSwitchFeature(SmartSwitchFeature, true, gnssMode);
                if (!this.mnlConfigUtils.saveMnlConfig(mnlConfig)) {
                    Log.e(this.TAG, "save mnl config fail");
                }
                restartGps();
            }
        } catch (Exception e) {
            Log.e(this.TAG, Log.getStackTraceString(e));
        }
    }

    public synchronized void disableL5AndDisableGlp() {
        try {
            if (this.mnlConfigUtils == null) {
                this.mnlConfigUtils = new MnlConfigUtils();
            }
            MnlConfig mnlConfig = this.mnlConfigUtils.getMnlConfig();
            if (mnlConfig != null) {
                MnlConfigFeature l1OnlyFeature = mnlConfig.getFeature("L1Only");
                MnlConfigFeature glpFeature = mnlConfig.getFeature("GLP");
                if (l1OnlyFeature != null && glpFeature != null) {
                    setL1OnlyFeature(l1OnlyFeature, true);
                    setGlpFeature(glpFeature, false);
                    if (this.mnlConfigUtils.saveMnlConfig(mnlConfig)) {
                        Log.d(this.TAG, "save mnl config success");
                        restartGps();
                    }
                }
                Log.d(this.TAG, "l1Onlyfeature or glpFeature is null");
            }
        } catch (Exception e) {
            Log.e(this.TAG, Log.getStackTraceString(e));
        }
    }

    public synchronized void enableL5AndEnableGlp() {
        try {
            if (this.mnlConfigUtils == null) {
                this.mnlConfigUtils = new MnlConfigUtils();
            }
            MnlConfig mnlConfig = this.mnlConfigUtils.getMnlConfig();
            MnlConfig backupMnlConfig = this.mnlConfigUtils.getBackUpMnlConfig();
            if (mnlConfig != null) {
                MnlConfigFeature l1OnlyFeature = mnlConfig.getFeature("L1Only");
                MnlConfigFeature glpFeature = mnlConfig.getFeature("GLP");
                if (l1OnlyFeature != null && glpFeature != null) {
                    if (backupMnlConfig != null) {
                        resetWithBackupConfig(backupMnlConfig, l1OnlyFeature, glpFeature);
                    } else {
                        setL1OnlyFeature(l1OnlyFeature, false);
                        setGlpFeature(glpFeature, true);
                    }
                    if (this.mnlConfigUtils.saveMnlConfig(mnlConfig)) {
                        Log.d(this.TAG, "save mnl config success");
                        restartGps();
                    }
                }
                Log.d(this.TAG, "l1OnlyFeature, smartSwitchFeature or glpFeature is null");
            }
        } catch (Exception e) {
            Log.e(this.TAG, Log.getStackTraceString(e));
        }
    }

    private void resetWithBackupConfig(MnlConfig backupMnlConfig, MnlConfigFeature l1OnlyFeature, MnlConfigFeature glpFeature) {
        resetWithBackupConfig(backupMnlConfig, l1OnlyFeature, glpFeature, null);
    }

    private void resetWithBackupConfig(MnlConfig backupMnlConfig, MnlConfigFeature l1OnlyFeature, MnlConfigFeature glpFeature, MnlConfigFeature smartSwitchFeature) {
        MnlConfigFeature backupSmartSwitch;
        MnlConfigFeature backupL1OnlyFeature = backupMnlConfig.getFeature("L1Only");
        if (backupL1OnlyFeature != null) {
            l1OnlyFeature.setFormatSettings(backupL1OnlyFeature.getFormatSettings());
            l1OnlyFeature.setConfig(backupL1OnlyFeature.getConfig());
        }
        MnlConfigFeature backupGlpFeature = backupMnlConfig.getFeature("GLP");
        if (backupGlpFeature != null) {
            glpFeature.setFormatSettings(backupGlpFeature.getFormatSettings());
            glpFeature.setConfig(backupGlpFeature.getConfig());
        }
        if (smartSwitchFeature != null && (backupSmartSwitch = backupMnlConfig.getFeature("GnssMode")) != null) {
            smartSwitchFeature.setFormatSettings(backupSmartSwitch.getFormatSettings());
            smartSwitchFeature.setConfig(backupSmartSwitch.getConfig());
        }
    }

    public synchronized void recoveryAllSatellite() {
        try {
            if (this.mnlConfigUtils == null) {
                this.mnlConfigUtils = new MnlConfigUtils();
            }
            MnlConfig mnlConfig = this.mnlConfigUtils.getMnlConfig();
            MnlConfig backupMnlConfig = this.mnlConfigUtils.getBackUpMnlConfig();
            if (mnlConfig != null) {
                MnlConfigFeature smartSwitchFeature = mnlConfig.getFeature("GnssMode");
                if (smartSwitchFeature == null) {
                    Log.d(this.TAG, "smartSwitchFeature or glpFeature is null");
                    return;
                }
                if (backupMnlConfig != null) {
                    resetWithBackupConfig(backupMnlConfig, smartSwitchFeature);
                } else {
                    setSmartSwitchFeature(smartSwitchFeature, false, 6);
                }
                if (this.mnlConfigUtils.saveMnlConfig(mnlConfig)) {
                    Log.d(this.TAG, "recovery mnl config success");
                    restartGps();
                }
            }
        } catch (Exception e) {
            Log.e(this.TAG, Log.getStackTraceString(e));
        }
    }

    private void resetWithBackupConfig(MnlConfig backupMnlConfig, MnlConfigFeature smartSwitchFeature) {
        MnlConfigFeature backupSmartSwitchFeature = backupMnlConfig.getFeature("GnssMode");
        if (backupSmartSwitchFeature != null) {
            smartSwitchFeature.setFormatSettings(backupSmartSwitchFeature.getFormatSettings());
            smartSwitchFeature.setConfig(backupSmartSwitchFeature.getConfig());
        }
    }

    private boolean isL1OnlyEnable(MnlConfig mnlConfig) {
        MnlConfigFeature l1OnlyFeature = mnlConfig.getFeature("L1Only");
        if (l1OnlyFeature == null) {
            return false;
        }
        String l1OnlyConfig = l1OnlyFeature.getConfig();
        String l1OnlyFormatSetting = "";
        Iterator<Map.Entry<String, String>> it = l1OnlyFeature.getFormatSettings().entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().toLowerCase().contains("L1 only".toLowerCase())) {
                String l1OnlyFormatSetting2 = entry.getValue();
                l1OnlyFormatSetting = l1OnlyFormatSetting2;
                break;
            }
        }
        return "1".equals(l1OnlyConfig) && "1".equals(l1OnlyFormatSetting);
    }

    private boolean isSmartSwitchEnable(MnlConfig mnlConfig) {
        MnlConfigFeature smartSwitchFeature = mnlConfig.getFeature("GnssMode");
        boolean z = false;
        if (smartSwitchFeature == null) {
            return false;
        }
        String smartSwitchConfig = smartSwitchFeature.getConfig();
        String smartFormatSetting = "";
        Iterator<Map.Entry<String, String>> it = smartSwitchFeature.getFormatSettings().entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().toLowerCase().contains("GP+".toLowerCase())) {
                String smartFormatSetting2 = entry.getValue();
                smartFormatSetting = smartFormatSetting2;
                break;
            }
        }
        if ("1".equals(smartSwitchConfig) && String.valueOf(1).equals(smartFormatSetting)) {
            z = true;
        }
        this.isSmartSwitchEnableFlag = z;
        return z;
    }

    private void setL1OnlyFeature(MnlConfigFeature l1OnlyFeature, boolean enable) {
        if (l1OnlyFeature == null) {
            Log.d(this.TAG, "l1OnlyFeature or glpFeature is null");
            return;
        }
        String l1OnlySettingKey = "";
        for (String key : l1OnlyFeature.getFormatSettings().keySet()) {
            if (key.toLowerCase().contains("L1 only".toLowerCase())) {
                l1OnlySettingKey = key;
            }
        }
        if (l1OnlySettingKey.equals("")) {
            return;
        }
        l1OnlyFeature.setConfig(enable ? "1" : "0");
        l1OnlyFeature.getFormatSettings().put(l1OnlySettingKey, enable ? "1" : "0");
        Log.d(this.TAG, "setL1OnlyFeature done");
    }

    private void setSmartSwitchFeature(MnlConfigFeature smartSwitchFeature, boolean enable, int gnssModeType) {
        if (smartSwitchFeature == null) {
            Log.d(this.TAG, "smartSwitchFeature is null");
            return;
        }
        String gnssModeSettingKey = "";
        for (String key : smartSwitchFeature.getFormatSettings().keySet()) {
            if (key.toLowerCase().contains("GP+".toLowerCase())) {
                gnssModeSettingKey = key;
            }
        }
        if (gnssModeSettingKey.equals("")) {
            return;
        }
        smartSwitchFeature.setConfig(enable ? "1" : "0");
        smartSwitchFeature.getFormatSettings().put(gnssModeSettingKey, String.valueOf(gnssModeType));
    }

    private void setGlpFeature(MnlConfigFeature glpFeature, boolean enable) {
        glpFeature.setConfig(enable ? "1" : "0");
    }

    private void restartGps() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        try {
            if (this.locationManager == null) {
                this.locationManager = (LocationManager) context.getSystemService("location");
            }
            boolean gpsEnable = this.locationManager.isProviderEnabled("gps");
            if (!gpsEnable) {
                Log.d(this.TAG, "gnss is close, don't need restart");
                return;
            }
            Log.d(this.TAG, "restart gnss");
            this.locationManager.setLocationEnabledForUser(false, UserHandle.of(UserHandle.myUserId()));
            this.locationManager.setLocationEnabledForUser(true, UserHandle.of(UserHandle.myUserId()));
        } catch (Exception e) {
            Log.e(this.TAG, "exception in restart gps : " + Log.getStackTraceString(e));
        }
    }

    /* loaded from: classes.dex */
    private class BootCompletedReceiver extends BroadcastReceiver {
        private BootCompletedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!MtkGnssPowerSaveImpl.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                return;
            }
            Log.d(MtkGnssPowerSaveImpl.this.TAG, "receiver boot completed, start init listener");
            MtkGnssPowerSaveImpl.this.initListenerAndRegisterIEmd();
            MtkGnssPowerSaveImpl.this.mContext.unregisterReceiver(MtkGnssPowerSaveImpl.this.bootCompletedReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initListenerAndRegisterIEmd() {
        if (!supportMtkGnssConfig()) {
            Log.d(this.TAG, "not support mtk gnss config feature");
            return;
        }
        if (this.mtkGnssDisableL5Obs == null) {
            this.mtkGnssDisableL5Obs = new GnssPowerSaveObs(this.mContext, null);
        }
        if (this.mtkGnssSmartSwitchObs == null) {
            this.mtkGnssSmartSwitchObs = new GnssSmartSwitchObs(this.mContext, null);
        }
        try {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(XM_MTK_GNSS_CONF_DISABLE_L5_CONF), false, this.mtkGnssDisableL5Obs);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(XM_MTK_GNSS_CONF_SMART_SWITCH_CONF), false, this.mtkGnssSmartSwitchObs);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_POWER_SAVE_MODE_CHANGED);
            if (this.powerSaveReceiver == null) {
                this.powerSaveReceiver = new PowerSaveReceiver();
            }
            this.mContext.registerReceiver(this.powerSaveReceiver, intentFilter);
        } catch (Exception e) {
            Log.e(this.TAG, "init exception:", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean l5Device() {
        return 1 == SystemProperties.getInt(MTK_GNSS_CONFIG_SUPPORT_L5_PROP, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PowerSaveReceiver extends BroadcastReceiver {
        private PowerSaveReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || TextUtils.isEmpty(action)) {
                return;
            }
            Message msg = new Message();
            msg.what = 0;
            boolean powerSaveStatus = intent.getBooleanExtra(MtkGnssPowerSaveImpl.KEY_POWER_MODE_OPEN, false);
            boolean isL5Device = MtkGnssPowerSaveImpl.this.l5Device();
            if (MtkGnssPowerSaveImpl.ACTION_POWER_SAVE_MODE_CHANGED.equals(action)) {
                if (isL5Device) {
                    msg.what = 2;
                } else if (powerSaveStatus) {
                    msg.what = 1;
                } else {
                    msg.what = 0;
                }
                msg.obj = Boolean.valueOf(powerSaveStatus);
                MtkGnssPowerSaveImpl.this.mHandler.sendMessage(msg);
            }
        }
    }

    private void sendModeChangedBroadcast() {
        Intent intent = new Intent(DISABLE_L5_MODE_CHANGED_ACTION);
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssPowerSaveObs extends ContentObserver {
        private Context mContext;

        public GnssPowerSaveObs(Context mContext, Handler handler) {
            super(handler);
            this.mContext = mContext;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!SystemProperties.getBoolean(MtkGnssPowerSaveImpl.MTK_GNSS_CONFIG_SUPPORT_DISABLE_L5_PROP, false)) {
                return;
            }
            int state = Settings.Secure.getInt(this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_DISABLE_L5_CONF, 2);
            Log.d(MtkGnssPowerSaveImpl.this.TAG, "xiaomi_mtk_gnss_power_save_feature had set to " + state);
            if (state == 1) {
                MtkGnssPowerSaveImpl.this.disableL5AndDisableGlp();
            } else {
                MtkGnssPowerSaveImpl.this.enableL5AndEnableGlp();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssSmartSwitchObs extends ContentObserver {
        private Context mContext;

        public GnssSmartSwitchObs(Context mContext, Handler handler) {
            super(handler);
            this.mContext = mContext;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!SystemProperties.getBoolean(MtkGnssPowerSaveImpl.MTK_GNSS_CONFIG_SUPPORT_DISABLE_SATELLITE_PROP, false)) {
                return;
            }
            int state = Settings.Secure.getInt(this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_SMART_SWITCH_CONF, 2);
            Log.d(MtkGnssPowerSaveImpl.this.TAG, "xiaomi_mtk_gnss_smart_switch_feature had set to " + state);
            if (state == 1) {
                MtkGnssPowerSaveImpl.this.activeSmartSwitch(1);
                Log.d(MtkGnssPowerSaveImpl.this.TAG, "start smart switch");
                MtkGnssPowerSaveImpl.this.isSmartSwitchEnableFlag = true;
            } else {
                if (!MtkGnssPowerSaveImpl.this.isSmartSwitchEnableFlag) {
                    Log.d(MtkGnssPowerSaveImpl.this.TAG, "smartSatellite is not actived,no need to modify");
                    return;
                }
                MtkGnssPowerSaveImpl.this.recoveryAllSatellite();
                MtkGnssPowerSaveImpl.this.isSmartSwitchEnableFlag = false;
                Log.d(MtkGnssPowerSaveImpl.this.TAG, "start recovery all satellite system");
            }
        }
    }

    /* loaded from: classes.dex */
    private class SmartSatelliteHandler extends Handler {
        public SmartSatelliteHandler(Looper looper) {
            super(looper);
        }

        public SmartSatelliteHandler(Looper looper, Handler.Callback callback) {
            super(looper, callback);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.obj == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_SMART_SWITCH_CONF, 2);
                    return;
                case 1:
                    Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_SMART_SWITCH_CONF, 1);
                    return;
                case 2:
                    boolean powerSaveMode = Boolean.valueOf(msg.obj.toString()).booleanValue();
                    if (powerSaveMode) {
                        Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_DISABLE_L5_CONF, 1);
                        Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_SMART_SWITCH_CONF, 1);
                        return;
                    } else {
                        Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_DISABLE_L5_CONF, 2);
                        Settings.Secure.putInt(MtkGnssPowerSaveImpl.this.mContext.getContentResolver(), MtkGnssPowerSaveImpl.XM_MTK_GNSS_CONF_SMART_SWITCH_CONF, 2);
                        return;
                    }
                default:
                    return;
            }
        }
    }
}
