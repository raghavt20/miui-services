package com.android.server.location.gnss;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationRequest;
import android.location.provider.ProviderRequest;
import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.location.GnssEventHandler;
import com.android.server.location.GnssSmartSatelliteSwitchStub;
import com.android.server.location.LocationDumpLogStub;
import com.android.server.location.LocationExtCooperateStub;
import com.android.server.location.MiuiBlurLocationManagerImpl;
import com.android.server.location.NewGnssEventHandler;
import com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecoveryStub;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.gnss.hal.GnssPowerOptimizeStub;
import com.android.server.location.provider.AmapCustomStub;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;

/* loaded from: classes.dex */
public class GnssLocationProviderImpl implements GnssLocationProviderStub {
    private static final String ACTION_MODEM_LOCATION = "NFW GPS LOCATION FROM Modem";
    private static final String CALLER_PACKAGE_NAME_ACTION = "com.xiaomi.bsp.gps.nps.callerName";
    private static final String CLASS_PACKAGE_CONNECTIVITY_NPI = "com.xiaomi.bsp.gps.nps";
    private static boolean ENABLE_FULL_TRACKING = false;
    private static final String EXTRA_NPS_NEW_EVENT = "com.xiaomi.bsp.gps.nps.NewEvent";
    private static final String EXTRA_NPS_PACKAGE_NAME = "com.xiaomi.bsp.gps.nps.pkgNname";
    private static final String GET_EVENT_ACTION = "com.xiaomi.bsp.gps.nps.GetEvent";
    private static final String MIUI_NFW_PROXY_APP = "com.lbe.security.miui";
    private static final String RECEIVER_GNSS_CALLER_NAME_EVENT = "com.xiaomi.bsp.gps.nps.GnssCallerNameEventReceiver";
    private static final String RECEIVER_GNSS_EVENT = "com.xiaomi.bsp.gps.nps.GnssEventReceiver";
    private static final String STR_LOCATIONFILE = "locationinformation.txt";
    private static final String XM_HP_LOCATION = "xiaomi_high_precise_location";
    private static final int XM_HP_LOCATION_OFF = 2;
    private static final File mLocationFile;
    private static final Object mLocationInformationLock;
    private Stack<BlurLocationItem> blurLocationCollector;
    private BlurLocationItem blurLocationItem;
    private MiuiBlurLocationManagerImpl mBlurLocation;
    private String mBlurPackageName;
    private int mBlurUid;
    private Context mContext;
    private BlurLocationItem popBlurLocationItem;
    private static final String TAG = "GnssLocationProviderImpl";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private boolean mEnableSendingState = false;
    private boolean mEdgnssUiSwitch = false;
    private boolean mNoise = false;
    private GnssEventHandler mGnssEventHandler = null;
    private NewGnssEventHandler newGnssEventHandler = null;
    private int mBlurState = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssLocationProviderImpl> {

        /* compiled from: GnssLocationProviderImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssLocationProviderImpl INSTANCE = new GnssLocationProviderImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssLocationProviderImpl m1804provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssLocationProviderImpl m1803provideNewInstance() {
            return new GnssLocationProviderImpl();
        }
    }

    static {
        Object obj = new Object();
        mLocationInformationLock = obj;
        ENABLE_FULL_TRACKING = false;
        synchronized (obj) {
            File file = new File(getSystemDir(), STR_LOCATIONFILE);
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                loge("IO error occurred!");
            }
            mLocationFile = new File(getSystemDir(), STR_LOCATIONFILE);
        }
    }

    public void handleInitialize(Context context) {
        this.mContext = context;
        GnssPowerOptimizeStub.getInstance().init(context);
        GnssEventTrackingStub.getInstance().init(context);
        LocationExtCooperateStub.getInstance().init(context);
        LocationDumpLogStub.getInstance().addToBugreport(2, "initialize glp");
        GnssCollectDataStub.getInstance().savePoint(0, (String) null, context);
        writeLocationInformation("initialize glp");
    }

    public boolean getSendingSwitch() {
        return this.mEnableSendingState;
    }

    public void setSendingSwitch(boolean newValue) {
        this.mEnableSendingState = newValue;
    }

    public void reloadGpsProperties(GnssConfiguration gnssConfiguration) {
        LocationDumpLogStub.getInstance().setLength(4, Integer.parseInt(LocationDumpLogStub.getInstance().getConfig(gnssConfiguration.getProperties(), "NMEA_LEN", "20000")));
        setSendingSwitch(Boolean.parseBoolean(getConfig(gnssConfiguration.getProperties(), "ENABLE_NOTIFY", "false")));
    }

    public LocationRequest.Builder handleRequestLocation(boolean canBypass, LocationRequest.Builder locationRequest, String provider) {
        LocationDumpLogStub.getInstance().addToBugreport(2, "request location from HAL using provider " + provider);
        if (canBypass) {
            locationRequest.setLocationSettingsIgnored(true);
            locationRequest.setMaxUpdates(2);
            Log.i(TAG, "Bypass All Modem request.");
            LocationDumpLogStub.getInstance().addToBugreport(2, "Bypass All Modem request.");
        }
        return locationRequest;
    }

    public String getConfig(Properties properties, String config, String defaultConfig) {
        return properties.getProperty(config, defaultConfig);
    }

    public void notifyState(int event) {
        logd("gps now on " + event);
        deliverIntent(this.mContext, event);
    }

    public void notifyStateWithPackageName(String packageName, int event) {
        logd("notify state, event:" + event + ", " + packageName);
        deliverIntentWithPackageName(this.mContext, packageName, event);
    }

    public void handleEnable() {
        LocationDumpLogStub.getInstance().addToBugreport(2, "enable gnss");
        writeLocationInformation("enable gnss");
    }

    public void handleDisable() {
        LocationDumpLogStub.getInstance().clearData();
        LocationDumpLogStub.getInstance().addToBugreport(2, "disable gnss");
        GnssPowerOptimizeStub.getInstance().disableGnssSwitch();
        writeLocationInformation("disable gnss");
    }

    public void handleReportSvStatus(GnssStatus gnssStatus, long lastFixTime, ProviderRequest mProviderRequest, GnssNative mGnssNative) {
        int svCount = gnssStatus.getSatelliteCount();
        float[] cn0s = new float[svCount];
        float[] svCarrierFre = new float[svCount];
        float[] svConstellation = new float[svCount];
        for (int i = 0; i < svCount; i++) {
            cn0s[i] = gnssStatus.getCn0DbHz(i);
            svCarrierFre[i] = gnssStatus.getCarrierFrequencyHz(i);
            if (gnssStatus.usedInFix(i)) {
                svConstellation[i] = gnssStatus.getConstellationType(i);
            }
        }
        GnssCollectDataStub.getInstance().savePoint(10, cn0s, svCount, svCarrierFre, svConstellation);
        AmapCustomStub.getInstance().setLastSvStatus(gnssStatus);
        if (lastFixTime != 0 && SystemClock.elapsedRealtime() - lastFixTime > 3000 && LocationDumpLogStub.getInstance().getRecordLoseLocation()) {
            LocationDumpLogStub.getInstance().addToBugreport(2, "lose location, record the latest SV status ");
            StringBuilder statusInfos = new StringBuilder();
            for (int i2 = 0; i2 < gnssStatus.getSatelliteCount(); i2++) {
                statusInfos.append(gnssStatus.getSvid(i2)).append(",").append(gnssStatus.getCn0DbHz(i2)).append(",");
            }
            LocationDumpLogStub.getInstance().addToBugreport(3, statusInfos.toString());
            if (!ENABLE_FULL_TRACKING && GnssSelfRecoveryStub.getInstance().startDiagnostic(mProviderRequest.getWorkSource(), mGnssNative)) {
                ENABLE_FULL_TRACKING = true;
            }
            LocationDumpLogStub.getInstance().setRecordLoseLocation(false);
            writeLocationInformation("lose location");
            GnssCollectDataStub.getInstance().savePoint(4, (String) null);
        }
        GnssSmartSatelliteSwitchStub.getInstance().smartSatelliteSwitchMonitor(gnssStatus);
    }

    public void dump(PrintWriter pw) {
        pw.append((CharSequence) loadLocationInformation());
        pw.println("GLP information:");
        LocationDumpLogStub.getInstance().dump(2, pw);
        pw.println("NMEA information:");
        LocationDumpLogStub.getInstance().dump(4, pw);
    }

    public void notifyCallerName(String pkgName) {
        logd("caller name: " + pkgName);
        Context context = this.mContext;
        if (context != null) {
            deliverCallerNameIntent(context, pkgName);
            this.mEdgnssUiSwitch = isEdgnssSwitchOn(this.mContext);
            logd("Edgnss Switch now is " + this.mEdgnssUiSwitch);
        }
    }

    public void notifyCallerName(String pkgName, String eventType) {
        logd("caller name: " + pkgName + ", eventType" + eventType);
        if (eventType == null) {
            notifyCallerName(pkgName);
            return;
        }
        Context context = this.mContext;
        if (context != null) {
            try {
                if (this.mGnssEventHandler == null) {
                    this.mGnssEventHandler = GnssEventHandler.getInstance(context);
                }
                if (this.blurLocationCollector == null) {
                    this.blurLocationCollector = new Stack<>();
                }
                if (eventType.equals("blurLocation_notify is on")) {
                    showBlurNotification(this.mContext.getPackageManager().getApplicationInfo(pkgName, 0).uid, pkgName);
                } else if (eventType.equals("blurLocation_notify is off")) {
                    removeBlurNotification(this.mContext.getPackageManager().getApplicationInfo(pkgName, 0).uid, pkgName);
                }
            } catch (Exception e) {
                loge("exception");
            }
        }
    }

    public void handleReportLocation(int ttff, Location location) {
        LocationDumpLogStub.getInstance().addToBugreport(2, "TTFF is " + ttff);
        if (ENABLE_FULL_TRACKING) {
            ENABLE_FULL_TRACKING = false;
        }
        LocationDumpLogStub.getInstance().addToBugreport(3, "the first location is " + location);
        writeLocationInformation("fix location");
        GnssCollectDataStub.getInstance().savePoint(2, (String) null);
    }

    private boolean isChineseLanguage() {
        String language = Locale.getDefault().toString();
        return language.endsWith("zh_CN");
    }

    private void packageOnReceive(String packageName) {
        List<String> mIgnoreNotifyPackage = new ArrayList<>(Arrays.asList("android", "com.xiaomi.location.fused"));
        if (this.mGnssEventHandler == null) {
            this.mGnssEventHandler = GnssEventHandler.getInstance(this.mContext);
        }
        Log.d(TAG, "receive caller pkg name =" + packageName);
        if (TextUtils.isEmpty(packageName) || mIgnoreNotifyPackage.contains(packageName)) {
            return;
        }
        this.mGnssEventHandler.handleCallerName(packageName);
    }

    private void packageEventOnReceive(int event, String packageName) {
        if (this.mGnssEventHandler == null) {
            this.mGnssEventHandler = GnssEventHandler.getInstance(this.mContext);
        }
        if (this.newGnssEventHandler == null) {
            this.newGnssEventHandler = NewGnssEventHandler.getInstance(this.mContext);
        }
        if (event == 0) {
            return;
        }
        Log.d(TAG, "receive event " + event + (packageName == null ? "" : "," + packageName));
        if (packageName != null && !packageName.trim().equals("")) {
            switch (event) {
                case 3:
                case 5:
                    this.newGnssEventHandler.handlerUpdateFixStatus(true);
                    return;
                case 4:
                    this.newGnssEventHandler.handlerUpdateFixStatus(false);
                    return;
                case 6:
                    this.newGnssEventHandler.handleStart(packageName);
                    return;
                case 7:
                    this.newGnssEventHandler.handleStop(packageName);
                    return;
                case 8:
                    this.newGnssEventHandler.handleUpdateGnssStatus();
                    return;
                default:
                    return;
            }
        }
        switch (event) {
            case 1:
            case 3:
            case 4:
            case 5:
            default:
                return;
            case 2:
                this.mGnssEventHandler.handleStop();
                return;
        }
    }

    private void deliverIntent(Context context, int event) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageEventOnReceive(event, null);
    }

    private void deliverIntentWithPackageName(Context context, String packageName, int event) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageEventOnReceive(event, packageName);
    }

    private void deliverCallerNameIntent(Context context, String packageName) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageOnReceive(packageName);
    }

    public void writeLocationInformation(String event) {
        logd("writeLocationInformation:" + event);
        synchronized (mLocationInformationLock) {
            String info = getCurrentTime() + ": " + event + "\n";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(getLocationFilePath(), true));
                try {
                    writer.write(info);
                    writer.close();
                } catch (Throwable th) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                loge("IO exception");
            }
        }
    }

    public StringBuilder loadLocationInformation() {
        StringBuilder stirngBuidler;
        String readLine;
        logd("loadLocationInformation");
        synchronized (mLocationInformationLock) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(mLocationFile));
                try {
                    stirngBuidler = new StringBuilder();
                    String line = "LocationInformation:";
                    do {
                        stirngBuidler.append(line).append("\n");
                        readLine = reader.readLine();
                        line = readLine;
                    } while (readLine != null);
                    reader.close();
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                loge("IO exception");
                return new StringBuilder();
            }
        }
        return stirngBuidler;
    }

    private String getLocationFilePath() {
        String absolutePath;
        synchronized (mLocationInformationLock) {
            absolutePath = mLocationFile.getAbsolutePath();
        }
        return absolutePath;
    }

    private String getCurrentTime() {
        long mNow = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mNow);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
        return sb.toString();
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public void setNfwProxyAppConfig(Properties properties, String config) {
        String country = SystemProperties.get("ro.boot.hwc");
        if (!"CN".equals(country)) {
            properties.setProperty(config, MIUI_NFW_PROXY_APP);
            Log.d(TAG, "NFW app is com.lbe.security.miui");
        }
    }

    public boolean hasLocationPermission(PackageManager packageManager, String pkgName, Context context) {
        int flags = packageManager.getPermissionFlags("com.miui.securitycenter.permission.modem_location", pkgName, context.getUser());
        loge("flags in nfw app is " + flags);
        return packageManager.checkPermission("com.miui.securitycenter.permission.modem_location", pkgName) == 0 || (flags & 2) == 0;
    }

    public void sendNfwbroadcast(Context context) {
        Intent intent = new Intent(ACTION_MODEM_LOCATION);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isEdgnssSwitchOn(Context context) {
        return (context == null || Settings.Secure.getInt(context.getContentResolver(), XM_HP_LOCATION, 2) == 2) ? false : true;
    }

    public void ifNoiseEnvironment(String nmea) {
        if (!nmea.contains("PQWM1")) {
            return;
        }
        String[] nmeaSplit = nmea.split(",");
        if (nmeaSplit[0].indexOf("PQWM1") == -1 || nmeaSplit.length < 3 || TextUtils.isEmpty(nmeaSplit[9])) {
            return;
        }
        int value = Integer.parseInt(nmeaSplit[9]);
        if ((value != 18 && value != -12) || this.mNoise) {
            if (this.mNoise && value != 18 && value != -12) {
                this.mNoise = false;
                notifyCallerName("normal_environment");
                return;
            }
            return;
        }
        notifyCallerName("noise_environment");
        this.mNoise = true;
    }

    public String precisionProcessByType(String nmea) {
        ifNoiseEnvironment(nmea);
        if (!this.mEdgnssUiSwitch) {
            return nmea;
        }
        String[] nmeaSplit = nmea.split(",");
        if (nmeaSplit[0].indexOf("GGA") != -1 && !TextUtils.isEmpty(nmeaSplit[2])) {
            nmeaSplit[2] = precisionProcess(nmeaSplit[2]);
            nmeaSplit[4] = precisionProcess(nmeaSplit[4]);
        }
        if (nmeaSplit[0].indexOf("RMC") != -1 && !TextUtils.isEmpty(nmeaSplit[3])) {
            nmeaSplit[3] = precisionProcess(nmeaSplit[3]);
            nmeaSplit[5] = precisionProcess(nmeaSplit[5]);
        }
        if (nmeaSplit[0].indexOf("GLL") != -1 && !TextUtils.isEmpty(nmeaSplit[1])) {
            nmeaSplit[1] = precisionProcess(nmeaSplit[1]);
            nmeaSplit[3] = precisionProcess(nmeaSplit[3]);
        }
        String nmeaEnd = String.join(",", nmeaSplit);
        return nmeaEnd;
    }

    private String precisionProcess(String data) {
        int leng = data.lastIndexOf(46);
        if (leng == -1) {
            return data;
        }
        String dataNeedProcessed = data.substring(leng - 2);
        StringBuilder sf = new StringBuilder(2);
        String dataInvariant = data.substring(0, leng - 2);
        sf.append(dataInvariant);
        if (dataNeedProcessed.substring(0, 1).equals("0")) {
            sf.append("0");
        }
        BigDecimal bigDecimal = BigDecimal.valueOf(Double.valueOf(dataNeedProcessed).doubleValue() / 60.0d).setScale(5, 1);
        double dataProcessed = bigDecimal.multiply(new BigDecimal(60)).doubleValue();
        sf.append(dataProcessed);
        return sf.toString();
    }

    public Location getLowerAccLocation(Location location) {
        if (!this.mEdgnssUiSwitch) {
            return location;
        }
        location.setLatitude(dataFormat(location.getLatitude()));
        location.setLongitude(dataFormat(location.getLongitude()));
        return location;
    }

    public void showBlurNotification(int uid, String packageName) {
        int i;
        String str;
        if (!packageName.equals(this.mBlurPackageName) && (i = this.mBlurState) != 0 && (str = this.mBlurPackageName) != null) {
            BlurLocationItem blurLocationItem = new BlurLocationItem(i, str, this.mBlurUid);
            this.blurLocationItem = blurLocationItem;
            this.blurLocationCollector.push(blurLocationItem);
            this.mBlurState = 1;
            if (this.mBlurLocation == null) {
                this.mBlurLocation = new MiuiBlurLocationManagerImpl();
            }
            if (!MiuiBlurLocationManagerImpl.get().isBlurLocationMode(this.mBlurUid, this.mBlurPackageName)) {
                this.mBlurState = 1;
                this.blurLocationCollector.clear();
            }
        } else {
            this.mBlurState++;
        }
        this.mBlurUid = uid;
        this.mBlurPackageName = packageName;
        this.mGnssEventHandler.handleCallerName(packageName, "blurLocation_notify is on");
    }

    public void removeBlurNotification(int uid, String packageName) {
        if (!packageName.equals(this.mBlurPackageName)) {
            this.mBlurState = 0;
            this.mBlurPackageName = null;
            this.blurLocationCollector.clear();
            this.mGnssEventHandler.handleCallerName(packageName, "blurLocation_notify is off");
            return;
        }
        int i = this.mBlurState - 1;
        this.mBlurState = i;
        if (i == 0 && !this.blurLocationCollector.empty()) {
            BlurLocationItem pop = this.blurLocationCollector.pop();
            this.popBlurLocationItem = pop;
            this.mBlurState = pop.getState();
            this.mBlurUid = this.popBlurLocationItem.getUid();
            String packageName2 = this.popBlurLocationItem.getPackageName();
            this.mBlurPackageName = packageName2;
            this.mGnssEventHandler.handleCallerName(packageName2, "blurLocation_notify is on");
            return;
        }
        if (this.mBlurState == 0 && this.blurLocationCollector.empty()) {
            this.mGnssEventHandler.handleCallerName(packageName, "blurLocation_notify is off");
        }
    }

    private double dataFormat(double data) {
        try {
            DecimalFormat df = new DecimalFormat("0.00000");
            String doubleNumAsString = df.format(data);
            return Double.valueOf(doubleNumAsString.replace(",", ".")).doubleValue();
        } catch (Exception e) {
            loge("Exception here, print locale: " + Locale.getDefault());
            return data;
        }
    }

    private static void loge(String string) {
        Log.e(TAG, string);
    }

    private void logd(String string) {
        if (DEBUG) {
            Log.d(TAG, string);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BlurLocationItem {
        private String blurPackageName;
        private int blurState;
        private int blurUid;

        private BlurLocationItem(int state, String packageName, int uid) {
            this.blurState = state;
            this.blurPackageName = packageName;
            this.blurUid = uid;
        }

        public int getState() {
            return this.blurState;
        }

        public String getPackageName() {
            return this.blurPackageName;
        }

        public int getUid() {
            return this.blurUid;
        }
    }
}
