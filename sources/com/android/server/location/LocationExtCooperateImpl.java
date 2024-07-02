package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub;
import com.android.server.wm.FoldablePackagePolicy;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/* loaded from: classes.dex */
public class LocationExtCooperateImpl implements LocationExtCooperateStub {
    private static final String ACTION_SATELLITE_STATE_CHANGE = "com.android.app.action.SATELLITE_STATE_CHANGE";
    private static final String DEF_GSCO_PKG = "gscoPkg invalid";
    private static final String FLAG_GSCO_APPEND_PKG = "1";
    private static final String FLAG_GSCO_COVER_PKG = "0";
    private static final String KEY_API_USE = "MI_GNSS";
    private static final String KEY_GSCO_PKG = "gscoPkg";
    private static final String KEY_GSCO_STATUS = "gscoStatus";
    private static final String KEY_SATELLITE_STATE_CHANGE_IS_ENABLE = "is_enable";
    private static final String KEY_SATELLITE_STATE_CHANGE_PHONE_ID = "phone_id";
    private static final String SATELLITE_STATE = "satellite_state";
    private static final String TAG = "LocationExtCooperate";
    private static final HashSet<String> sDevices;
    private final File mCloudSpFile = new File(new File(Environment.getDataDirectory(), "system"), "satelliteCallOpt.xml");
    private Context mContext;
    private final boolean mDefaultFeatureStatus;
    private boolean mIsSpecifiedDevice;
    private final BroadcastReceiver mReceiver;
    private SettingsObserver mSettingsObserver;
    private final HashSet<String> sModule;
    private HashSet<String> sPkg;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocationExtCooperateImpl> {

        /* compiled from: LocationExtCooperateImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocationExtCooperateImpl INSTANCE = new LocationExtCooperateImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LocationExtCooperateImpl m1780provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LocationExtCooperateImpl m1779provideNewInstance() {
            return new LocationExtCooperateImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        sDevices = hashSet;
        hashSet.add("aurora");
    }

    LocationExtCooperateImpl() {
        HashSet<String> hashSet = new HashSet<>();
        this.sModule = hashSet;
        this.sPkg = new HashSet<>();
        this.mDefaultFeatureStatus = SystemProperties.getBoolean("persist.sys.gnss_gsco", false);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.location.LocationExtCooperateImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if ("com.android.app.action.SATELLITE_STATE_CHANGE".equals(intent.getAction())) {
                    boolean satelliteCallSwitcher = intent.getBooleanExtra(LocationExtCooperateImpl.KEY_SATELLITE_STATE_CHANGE_IS_ENABLE, false);
                    int phoneId = intent.getIntExtra(LocationExtCooperateImpl.KEY_SATELLITE_STATE_CHANGE_PHONE_ID, -1);
                    Log.i(LocationExtCooperateImpl.TAG, "Satellite call is enable:" + satelliteCallSwitcher + ", phoneId:" + phoneId);
                    if (satelliteCallSwitcher) {
                        GnssBackgroundUsageOptStub.getInstance().registerSatelliteCallMode(LocationExtCooperateImpl.this.sPkg, LocationExtCooperateImpl.KEY_API_USE);
                    } else {
                        GnssBackgroundUsageOptStub.getInstance().unRegisterSatelliteCallMode();
                    }
                }
            }
        };
        hashSet.add("power");
        this.sPkg.add("com.android.mms");
        this.sPkg.add("com.android.incallui");
        setPkgFromCloud(loadCloudDataGscoPkgFromSP());
        this.mSettingsObserver = new SettingsObserver(new Handler());
    }

    public void init(Context context) {
        this.mContext = context;
        boolean loadCloudDataGscoStatusFromSP = loadCloudDataGscoStatusFromSP();
        this.mIsSpecifiedDevice = loadCloudDataGscoStatusFromSP;
        if (loadCloudDataGscoStatusFromSP) {
            registerReceiver();
        }
        if (this.mSettingsObserver != null) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_GSCO_STATUS), false, this.mSettingsObserver, -2);
        }
    }

    public void setSatelliteCallOptStatus(boolean status) {
        boolean z = this.mIsSpecifiedDevice;
        if (!z && status) {
            Log.d(TAG, "Has Register Process Observer by cloud...");
            this.mIsSpecifiedDevice = true;
            saveCloudDataToSP(KEY_GSCO_STATUS, true);
            registerReceiver();
            return;
        }
        if (z && !status) {
            Log.d(TAG, "Has unRegister Process Observer by cloud...");
            this.mIsSpecifiedDevice = false;
            saveCloudDataToSP(KEY_GSCO_STATUS, false);
            unRegisterReceiver();
        }
    }

    public void setSatelliteCallOptPkg(String pkgs) {
        saveCloudDataToSP(KEY_GSCO_PKG, pkgs);
        setPkgFromCloud(pkgs);
    }

    private void setPkgFromCloud(String pkgs) {
        Log.i(TAG, "set Pkg From Cloud:" + pkgs);
        if (pkgs == null || pkgs.isEmpty()) {
            return;
        }
        String[] parts = pkgs.split(",");
        String flag = parts[0];
        Set<String> pkgSet = new HashSet<>();
        for (int i = 1; i < parts.length; i++) {
            pkgSet.add(parts[i]);
        }
        if (FLAG_GSCO_COVER_PKG.equals(flag)) {
            this.sPkg = new HashSet<>(pkgSet);
        } else if ("1".equals(flag)) {
            this.sPkg.addAll(pkgSet);
        } else {
            Log.e(TAG, "receive cloud pkg data not valid:" + pkgSet);
        }
        Log.i(TAG, "set pkg success:" + this.sPkg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unRegisterReceiver() {
        synchronized (LocationExtCooperateImpl.class) {
            Context context = this.mContext;
            if (context == null) {
                Log.e(TAG, "unRegisterReceiver fail due to context == null.");
                return;
            }
            context.unregisterReceiver(this.mReceiver);
            GnssBackgroundUsageOptStub.getInstance().unRegisterSatelliteCallMode();
            if (this.mSettingsObserver != null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
                this.mSettingsObserver = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerReceiver() {
        synchronized (LocationExtCooperateImpl.class) {
            if (this.mContext == null) {
                Log.e(TAG, "registerReceiver fail due to context == null.");
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.app.action.SATELLITE_STATE_CHANGE");
            this.mContext.registerReceiver(this.mReceiver, filter);
            if (this.mSettingsObserver != null) {
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("satellite_state"), false, this.mSettingsObserver, -2);
            } else {
                this.mSettingsObserver = new SettingsObserver(new Handler());
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("satellite_state"), false, this.mSettingsObserver, -2);
            }
        }
    }

    private String loadCloudDataGscoPkgFromSP() {
        Log.d(TAG, "load Cloud Data Gsco Status From SP running...");
        try {
            Context context = this.mContext;
            if (context == null) {
                return DEF_GSCO_PKG;
            }
            Context directBootContext = context.createDeviceProtectedStorageContext();
            SharedPreferences editor = directBootContext.getSharedPreferences(this.mCloudSpFile, 0);
            String pkg = editor.getString(KEY_GSCO_PKG, DEF_GSCO_PKG);
            Log.d(TAG, "Success to load GscoPkg:" + pkg);
            return pkg;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load GscoPkg..., " + e);
            return DEF_GSCO_PKG;
        }
    }

    private boolean loadCloudDataGscoStatusFromSP() {
        boolean defVal = this.mDefaultFeatureStatus || sDevices.contains(Build.DEVICE.toLowerCase());
        Log.d(TAG, "load Cloud Data Gsco Status From SP running...");
        try {
            Context context = this.mContext;
            if (context == null) {
                return defVal;
            }
            Context directBootContext = context.createDeviceProtectedStorageContext();
            SharedPreferences editor = directBootContext.getSharedPreferences(this.mCloudSpFile, 0);
            boolean status = editor.getBoolean(KEY_GSCO_STATUS, defVal);
            Log.d(TAG, "Success to load GscoStatus:" + status);
            return status;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load GscoStatus..., " + e);
            return defVal;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveCloudDataToSP(String key, Object value) {
        try {
            Context context = this.mContext;
            if (context == null) {
                return;
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(this.mCloudSpFile, 0).edit();
            if (value instanceof Boolean) {
                editor.putBoolean(key, ((Boolean) value).booleanValue());
            } else {
                if (!(value instanceof String)) {
                    Log.e(TAG, "save cloud data to sp value is not valid...");
                    return;
                }
                editor.putString(key, (String) value);
            }
            editor.apply();
            Log.d(TAG, "Success to save data to sp, data:" + value);
        } catch (Exception e) {
            Log.e(TAG, "Save data to sp Exception:" + e);
        }
    }

    public void registerSatelliteCallMode(String module, String pkg) {
        HashSet<String> pkgSet = new HashSet<>(Arrays.asList(pkg.split(",")));
        Iterator<String> it = pkgSet.iterator();
        while (it.hasNext()) {
            String p = it.next();
            if (!this.sPkg.contains(p)) {
                pkgSet.remove(p);
                Log.e(TAG, p + FoldablePackagePolicy.POLICY_VALUE_INTERCEPT_LIST);
            }
        }
        Log.i(TAG, "setSingleAppPosMode module:" + module + ",pkg:" + pkg + ", uid:" + Binder.getCallingUid());
        if (!this.sModule.contains(module) || pkgSet.isEmpty()) {
            Log.e(TAG, "setSingleAppPosMode not specify module or pkg...");
        } else {
            GnssBackgroundUsageOptStub.getInstance().registerSatelliteCallMode(pkgSet, KEY_API_USE);
        }
    }

    public void unRegisterSatelliteCallMode(String module) {
        Log.i(TAG, "removeSingleAppPosMode module:" + module + ", uid:" + Binder.getCallingUid());
        if (!this.sModule.contains(module)) {
            Log.e(TAG, "removeSingleAppPosMode not specify module...");
        } else {
            GnssBackgroundUsageOptStub.getInstance().unRegisterSatelliteCallMode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (uri.equals(Settings.System.getUriFor("satellite_state"))) {
                boolean state = Settings.System.getInt(LocationExtCooperateImpl.this.mContext.getContentResolver(), "satellite_state", -1) == 1;
                Log.i(LocationExtCooperateImpl.TAG, "Database SATELLITE_STATE changed:" + state);
                if (state) {
                    GnssBackgroundUsageOptStub.getInstance().registerSatelliteCallMode(LocationExtCooperateImpl.this.sPkg, LocationExtCooperateImpl.KEY_API_USE);
                    return;
                } else {
                    GnssBackgroundUsageOptStub.getInstance().unRegisterSatelliteCallMode();
                    return;
                }
            }
            if (uri.equals(Settings.System.getUriFor(LocationExtCooperateImpl.KEY_GSCO_STATUS))) {
                boolean state2 = Settings.System.getInt(LocationExtCooperateImpl.this.mContext.getContentResolver(), LocationExtCooperateImpl.KEY_GSCO_STATUS, -1) == 1;
                Log.d(LocationExtCooperateImpl.TAG, "receive gscoStatus database change:" + state2);
                if (!LocationExtCooperateImpl.this.mIsSpecifiedDevice && state2) {
                    Log.d(LocationExtCooperateImpl.TAG, "Has Register Process Observer by database...");
                    LocationExtCooperateImpl.this.mIsSpecifiedDevice = true;
                    LocationExtCooperateImpl.this.saveCloudDataToSP(LocationExtCooperateImpl.KEY_GSCO_STATUS, true);
                    LocationExtCooperateImpl.this.registerReceiver();
                    return;
                }
                if (LocationExtCooperateImpl.this.mIsSpecifiedDevice && !state2) {
                    Log.d(LocationExtCooperateImpl.TAG, "Has unRegister Process Observer by database...");
                    LocationExtCooperateImpl.this.mIsSpecifiedDevice = false;
                    LocationExtCooperateImpl.this.saveCloudDataToSP(LocationExtCooperateImpl.KEY_GSCO_STATUS, false);
                    LocationExtCooperateImpl.this.unRegisterReceiver();
                }
            }
        }
    }
}
