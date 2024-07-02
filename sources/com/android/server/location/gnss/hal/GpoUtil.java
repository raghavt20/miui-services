package com.android.server.location.gnss.hal;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.location.LocationDumpLogStub;
import com.android.server.location.gnss.GnssLocationProviderStub;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public class GpoUtil {
    private static final long MAX_ENGINE_TIME = 3600000;
    private static final int MAX_RECORD_ENGINE_SIZE = 5;
    private static final String PERSIST_FOR_GPO_VERSION = "persist.sys.gpo.version";
    private static final String SP_INDEX = "index";
    private static final String TAG = "GpoUtil";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static volatile GpoUtil instance;
    private DataEventAdapter mAdapter;
    private AppOpsManager mAppOps;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private Object mGnssHal;
    private Method startMethod;
    private Method stopMethod;
    private final AtomicInteger mGnssEngineStatus = new AtomicInteger(4);
    private final AtomicInteger mDefaultNetwork = new AtomicInteger(-1);
    private final AtomicInteger mGpoVersion = new AtomicInteger(SystemProperties.getInt(PERSIST_FOR_GPO_VERSION, 0));
    private final AtomicBoolean isHeavyUser = new AtomicBoolean(true);
    private final AtomicBoolean mGnssEngineStoppedByInstance = new AtomicBoolean(false);
    private final File mSpFileGnssEngineUsageTime = new File(new File(Environment.getDataDirectory(), "system"), "GnssEngineUsageTime.xml");
    private boolean isUserUnlocked = false;
    private int mUserBehavReport = 0;
    private final BroadcastReceiver mUserUnlockReceiver = new BroadcastReceiver() { // from class: com.android.server.location.gnss.hal.GpoUtil.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            GpoUtil.this.isUserUnlocked = true;
            GpoUtil.this.readEngineUsageData();
            GpoUtil.this.mContext.unregisterReceiver(GpoUtil.this.mUserUnlockReceiver);
        }
    };
    private final BroadcastReceiver mScreenUnlockReceiver = new BroadcastReceiver() { // from class: com.android.server.location.gnss.hal.GpoUtil.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            GpoUtil.this.mAdapter.notifyScreenState("android.intent.action.SCREEN_ON".equalsIgnoreCase(intent.getAction()));
        }
    };

    /* loaded from: classes.dex */
    public interface IDataEvent {
        void updateDefaultNetwork(int i);

        void updateFeatureSwitch(int i);

        void updateGnssStatus(int i);

        void updateScreenState(boolean z);
    }

    public static GpoUtil getInstance() {
        if (instance == null) {
            synchronized (GpoUtil.class) {
                if (instance == null) {
                    instance = new GpoUtil();
                }
            }
        }
        return instance;
    }

    private GpoUtil() {
    }

    public void initOnce(Context context, IDataEvent iDataEvent) {
        if (this.mContext == null) {
            this.mContext = context;
            this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            getGnssHal();
            registerBootCompletedRecv();
            registerScreenUnlockRecv();
            DataEventAdapter dataEventAdapter = new DataEventAdapter();
            this.mAdapter = dataEventAdapter;
            dataEventAdapter.attachEventListener(iDataEvent);
            this.mConnectivityManager = (ConnectivityManager) Preconditions.checkNotNull((ConnectivityManager) this.mContext.getSystemService("connectivity"));
            registerNetworkListener();
        }
    }

    private void registerNetworkListener() {
        this.mConnectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.gnss.hal.GpoUtil.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                GpoUtil.this.getDefaultNetwork();
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                GpoUtil.this.getDefaultNetwork();
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            }
        });
    }

    public int getDefaultNetwork() {
        int curNetwork;
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            curNetwork = -1;
        } else {
            curNetwork = networkInfo.getType();
        }
        if (this.mDefaultNetwork.get() != curNetwork) {
            logi(TAG, "Default Network: " + curNetwork, true);
            this.mAdapter.notifyDefaultNetwork(curNetwork);
            this.mDefaultNetwork.set(curNetwork);
        }
        return this.mDefaultNetwork.get();
    }

    public boolean isWifiEnabled() {
        return this.mDefaultNetwork.get() == 1;
    }

    public boolean isNetworkConnected() {
        return this.mDefaultNetwork.get() != -1;
    }

    private void registerBootCompletedRecv() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mUserUnlockReceiver, filter);
    }

    private void registerScreenUnlockRecv() {
        logv(TAG, "registerScreenUnlockRecv");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mScreenUnlockReceiver, filter);
    }

    public int getGpoVersion() {
        return this.mGpoVersion.get();
    }

    public int getEngineStatus() {
        return this.mGnssEngineStatus.get();
    }

    public void setEngineStatus(int status) {
        this.mGnssEngineStatus.set(status);
        logi(TAG, "gnss engine status: " + this.mGnssEngineStatus.get(), true);
        this.mAdapter.notifyGnssStatus(status);
    }

    public void setGpoVersionValue(int newValue) {
        if (this.mGpoVersion.get() != newValue) {
            SystemProperties.set(PERSIST_FOR_GPO_VERSION, String.valueOf(newValue));
            logi(TAG, "setGpoVersionValue: " + newValue, true);
            this.mGpoVersion.set(newValue);
            this.mAdapter.notifyPersistUpdates(this.mGpoVersion.get());
        }
    }

    public void recordEngineUsageDaily(long time) {
        if (this.isUserUnlocked) {
            logv(TAG, "recordEngineUsageDaily: " + time);
            SharedPreferences perf = this.mContext.getSharedPreferences(this.mSpFileGnssEngineUsageTime, 0);
            SharedPreferences.Editor editor = perf.edit();
            for (int i = 4; i > 0; i--) {
                editor.putInt(SP_INDEX + i, perf.getInt(SP_INDEX + (i - 1), 0));
            }
            editor.putInt("index0", time >= 3600000 ? 1 : 0);
            editor.apply();
            readEngineUsageData();
        }
    }

    public void readEngineUsageData() {
        if (this.isUserUnlocked) {
            logv(TAG, "readEngineUsageData");
            SharedPreferences pref = this.mContext.getSharedPreferences(this.mSpFileGnssEngineUsageTime, 0);
            int sum = 0;
            for (int i = 0; i < 5; i++) {
                int j = pref.getInt(SP_INDEX + i, 0);
                logv(TAG, SP_INDEX + i + " is " + j);
                sum += j;
            }
            boolean newValue = sum >= 3;
            if (this.isHeavyUser.get() != newValue) {
                this.isHeavyUser.set(newValue);
            }
            logi(TAG, "is heavy user ? " + this.isHeavyUser.get(), true);
        }
    }

    public boolean checkHeavyUser() {
        return this.isHeavyUser.get();
    }

    private void getGnssHal() {
        try {
            Class<?> innerClazz = Class.forName("com.android.server.location.gnss.hal.GnssNative$GnssHal");
            this.mGnssHal = innerClazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            this.startMethod = innerClazz.getDeclaredMethod("start", new Class[0]);
            this.stopMethod = innerClazz.getDeclaredMethod("stop", new Class[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateLocationIcon(boolean displayLocationIcon, int uid, String pkg) {
        AppOpsManager appOpsManager = this.mAppOps;
        if (appOpsManager == null) {
            return;
        }
        if (displayLocationIcon) {
            if (appOpsManager.startOpNoThrow(42, uid, pkg) != 0) {
                this.mAppOps.finishOp(42, uid, pkg);
            }
        } else {
            appOpsManager.finishOp(42, uid, pkg);
        }
        logi(TAG, "update Location Icon to " + displayLocationIcon + " for (" + uid + "," + pkg + ")", true);
    }

    public boolean doStartEngineByInstance() {
        if (this.mGnssEngineStatus.get() == 2) {
            return true;
        }
        if (this.mGnssEngineStatus.get() == 4) {
            return false;
        }
        try {
            logi(TAG, "doStartEngineByInstance()", true);
            this.startMethod.invoke(this.mGnssHal, new Object[0]);
            logv(TAG, "mGnssHal.start()");
            return true;
        } catch (Exception e) {
            loge(TAG, "start engine failed.");
            return false;
        }
    }

    public void doStopEngineByInstance() {
        try {
            this.mGnssEngineStoppedByInstance.set(true);
            logi(TAG, "doStopEngineByInstance()", true);
            this.stopMethod.invoke(this.mGnssHal, new Object[0]);
        } catch (Exception e) {
            loge(TAG, "stop engine failed.");
        }
        this.mGnssEngineStoppedByInstance.set(false);
        logv(TAG, "mGnssHal.stop()");
    }

    public boolean engineStoppedByGpo() {
        return this.mGnssEngineStoppedByInstance.get();
    }

    public Location convertNlp2Glp(Location nLocation) {
        Location gLocation = new Location("gps");
        gLocation.setLatitude(nLocation.getLatitude());
        gLocation.setLongitude(nLocation.getLongitude());
        gLocation.setAccuracy(nLocation.getAccuracy());
        gLocation.setAltitude(0.0d);
        gLocation.setSpeed(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        gLocation.setTime(System.currentTimeMillis());
        gLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return gLocation;
    }

    public void setUserBehav(int event) {
        this.mUserBehavReport = event;
    }

    public int getCurUserBehav() {
        logv(TAG, "Current user behav is " + this.mUserBehavReport);
        return this.mUserBehavReport;
    }

    public void logv(String tag, String info) {
        if (VERBOSE) {
            Log.v(tag, info);
        }
    }

    public void loge(String tag, String info) {
        Log.e(tag, info);
    }

    public void logi(String tag, String info, boolean write2File) {
        Log.i(tag, info);
        if (write2File) {
            GnssLocationProviderStub.getInstance().writeLocationInformation(info);
        }
    }

    public void logEn(String info) {
        LocationDumpLogStub.getInstance().addToBugreport(3, info);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DataEventAdapter {
        private IDataEvent iDataEvent;

        private DataEventAdapter() {
        }

        public void attachEventListener(IDataEvent listener) {
            this.iDataEvent = listener;
        }

        public void notifyPersistUpdates(int version) {
            IDataEvent iDataEvent = this.iDataEvent;
            if (iDataEvent != null) {
                iDataEvent.updateFeatureSwitch(version);
            }
        }

        public void notifyScreenState(boolean on) {
            IDataEvent iDataEvent = this.iDataEvent;
            if (iDataEvent != null) {
                iDataEvent.updateScreenState(on);
            }
        }

        public void notifyDefaultNetwork(int type) {
            IDataEvent iDataEvent = this.iDataEvent;
            if (iDataEvent != null) {
                iDataEvent.updateDefaultNetwork(type);
            }
        }

        public void notifyGnssStatus(int status) {
            IDataEvent iDataEvent = this.iDataEvent;
            if (iDataEvent != null) {
                iDataEvent.updateGnssStatus(status);
            }
        }
    }
}
