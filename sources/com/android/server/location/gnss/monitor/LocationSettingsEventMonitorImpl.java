package com.android.server.location.gnss.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.location.LocationDumpLogStub;
import com.miui.base.MiuiStubRegistry;
import java.util.List;

/* loaded from: classes.dex */
public class LocationSettingsEventMonitorImpl implements LocationSettingsEventMonitorStub {
    private static final String DISABLED_BY_ANDROID = "android";
    private static final String DISABLED_BY_SYSTEM = "system";
    private static final String TAG = "LocationSettingsEventMonitor";
    private final boolean D = SystemProperties.getBoolean("persist.sys.gnss_dc.test", false);
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocationSettingsEventMonitorImpl> {

        /* compiled from: LocationSettingsEventMonitorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocationSettingsEventMonitorImpl INSTANCE = new LocationSettingsEventMonitorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LocationSettingsEventMonitorImpl m1854provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LocationSettingsEventMonitorImpl m1853provideNewInstance() {
            return new LocationSettingsEventMonitorImpl();
        }
    }

    LocationSettingsEventMonitorImpl() {
    }

    public void initMonitor(Context context) {
        this.mContext = context;
        Log.d(TAG, "init LocationSettingsEventMonitor done");
    }

    public void dumpCallingInfo(int userId, int pid, boolean enabled) {
        String callingPackageName = getCallingPackageName(pid);
        if (!DISABLED_BY_ANDROID.equals(callingPackageName) && !DISABLED_BY_SYSTEM.equals(callingPackageName) && callingPackageName != null) {
            String debugInfo = "[u" + userId + "] location enabled = " + enabled + ", because:" + callingPackageName;
            if (this.D) {
                Log.d(TAG, debugInfo);
            }
            LocationDumpLogStub.getInstance().addToBugreport(1, debugInfo);
        }
    }

    private String getCallingPackageName(int pid) {
        Context context = this.mContext;
        if (context == null) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfos) {
            if (runningAppProcessInfo.pid == pid) {
                return runningAppProcessInfo.processName;
            }
        }
        return null;
    }
}
