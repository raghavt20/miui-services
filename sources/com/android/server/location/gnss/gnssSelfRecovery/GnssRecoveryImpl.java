package com.android.server.location.gnss.gnssSelfRecovery;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class GnssRecoveryImpl implements IGnssRecovery {
    private static final String TAG = "GnssRecoveryImpl";
    private boolean enablefulltracking;
    private Context mContext;
    private Field mFieldEnableCorrVecOutputs;
    private Field mFieldEnableFullTracking;
    private Field mFieldIntervalMillis;
    private Field mFieldRegisterGnssMeasurement;
    private final File mFile = new File(new File(Environment.getDataDirectory(), "system"), "gnss_properties.xml");
    private Object mGnssHal;
    private GnssNative mGnssNative;
    private Method startMeasurementMethod;
    private Method startMethod;
    private Method stopMethod;

    public GnssRecoveryImpl(Context mContext, GnssNative gnssNative) {
        this.mContext = mContext;
        this.mGnssNative = gnssNative;
    }

    public GnssRecoveryImpl(Context mContext) {
        this.mContext = mContext;
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.IGnssRecovery
    public void restartGnss() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService("location");
            boolean gpsEnable = locationManager.isProviderEnabled("gps");
            if (!gpsEnable) {
                Log.d(TAG, "gnss is close, don't need restart");
                return;
            }
            Log.d(TAG, "restart gnss");
            locationManager.setLocationEnabledForUser(false, Process.myUserHandle());
            Thread.sleep(100L);
            locationManager.setLocationEnabledForUser(true, Process.myUserHandle());
        } catch (Exception e) {
            Log.e(TAG, "exception in restart gps : " + Log.getStackTraceString(e));
        }
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.IGnssRecovery
    public boolean setFullTracking(boolean enablefulltracking) {
        if (this.mGnssNative == null) {
            return false;
        }
        Log.d(TAG, "fulltracking: " + enablefulltracking);
        this.mGnssNative.startMeasurementCollection(enablefulltracking, false, Integer.MAX_VALUE);
        return true;
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.IGnssRecovery
    public boolean removeMockLocation() {
        Log.d(TAG, "removeMockLocation()");
        if (!checkMockOpPermission()) {
            grantMockOpPermission();
        }
        final LocationManager mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        String[] allProviders = {"gps", "network", "fused"};
        for (String provider : allProviders) {
            try {
                mLocationManager.removeTestProvider(provider);
            } catch (Exception e) {
                Log.d(TAG, "exception in remove mock providers : " + Log.getStackTraceString(e));
            }
        }
        removeMockAppPkgName();
        boolean gpsEnable = mLocationManager.isProviderEnabled("gps");
        if (!gpsEnable) {
            new Handler(Looper.myLooper()).postDelayed(new Runnable() { // from class: com.android.server.location.gnss.gnssSelfRecovery.GnssRecoveryImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GnssRecoveryImpl.lambda$removeMockLocation$0(mLocationManager);
                }
            }, 200L);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeMockLocation$0(LocationManager mLocationManager) {
        mLocationManager.setLocationEnabledForUser(false, UserHandle.SYSTEM);
        mLocationManager.setLocationEnabledForUser(true, UserHandle.SYSTEM);
    }

    private void removeMockAppPkgName() {
        try {
            SharedPreferences.Editor editor = this.mContext.getSharedPreferences(this.mFile, 0).edit();
            editor.putString(GnssDiagnosticsBase.LAST_MOCK_APP_PKG_NAME, "");
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void grantMockOpPermission() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
        try {
            appOpsManager.setMode(58, 1000, "android", 0);
        } catch (Exception e) {
            Log.d(TAG, "grant mockLocationPermission failed, cause:" + Log.getStackTraceString(e));
        }
    }

    private boolean checkMockOpPermission() {
        return false;
    }

    private void getGnssHal() throws Exception {
        Class<?> innerClazz = Class.forName("com.android.server.location.gnss.hal.GnssNative$GnssHal");
        this.mGnssHal = innerClazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        this.startMethod = innerClazz.getDeclaredMethod("start", new Class[0]);
        this.stopMethod = innerClazz.getDeclaredMethod("stop", new Class[0]);
        this.startMeasurementMethod = innerClazz.getDeclaredMethod("startMeasurementCollection", Boolean.TYPE, Boolean.TYPE, Integer.TYPE);
        this.mFieldRegisterGnssMeasurement = innerClazz.getDeclaredField("mRegisterGnssMeasurement");
        this.mFieldEnableFullTracking = innerClazz.getDeclaredField("mEnableFullTracking");
        this.mFieldEnableCorrVecOutputs = innerClazz.getDeclaredField("mEnableCorrVecOutputs");
        this.mFieldIntervalMillis = innerClazz.getDeclaredField("mIntervalMillis");
    }

    private void doStartMeasurementByInstance() throws IllegalAccessException, InvocationTargetException {
        Field field = this.mFieldRegisterGnssMeasurement;
        if (field != null && field.getBoolean(this.mGnssHal)) {
            Log.v(TAG, "doStartMeasurementByInstance");
            Method method = this.startMeasurementMethod;
            Object obj = this.mGnssHal;
            method.invoke(obj, Boolean.valueOf(this.mFieldEnableFullTracking.getBoolean(obj)), Boolean.valueOf(this.mFieldEnableCorrVecOutputs.getBoolean(this.mGnssHal)), Integer.valueOf(this.mFieldIntervalMillis.getInt(this.mGnssHal)));
            this.mFieldRegisterGnssMeasurement.setBoolean(this.mGnssHal, false);
            Log.v(TAG, "mGnssHal.startMeasurementCollection()");
        }
    }
}
