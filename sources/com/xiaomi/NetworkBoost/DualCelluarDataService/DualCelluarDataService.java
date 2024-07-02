package com.xiaomi.NetworkBoost.DualCelluarDataService;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;
import miui.os.Build;

/* loaded from: classes.dex */
public class DualCelluarDataService {
    private static final String APPLICATION_DUAL_MOBILE_DATA = "application_dual_mobile_data";
    private static final int DUAL_DATA_STATE_OFF = 0;
    private static final int DUAL_DATA_STATE_ON = 1;
    public static final String DUAL_MOBILE_DATA = "dual_mobile_data";
    private static final int ERROR_CODE_DEVICE_CAPABILITY = 100;
    private static final int ERROR_CODE_DEVICE_NOT_SUPPORT = 101;
    private static final int ERROR_CODE_OTHER_DUPLICATE_SETTING = 200;
    private static final int SUCCESS_CODE = 0;
    private static final String TAG = "DualCelluarDataService";
    private static DualCelluarDataService sInstance = null;
    private Context mContext;
    private IActivityManager mIActivityManager;
    private final IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.xiaomi.NetworkBoost.DualCelluarDataService.DualCelluarDataService.1
        public void onForegroundActivitiesChanged(int pid, int uid, boolean fg) {
        }

        public void onProcessDied(int pid, int uid) {
            DualCelluarDataService.this.processWhiteListAppDied(uid);
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    private Set<String> mRedundantModeApplicationPN;

    private DualCelluarDataService(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static DualCelluarDataService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DualCelluarDataService.class) {
                if (sInstance == null) {
                    DualCelluarDataService dualCelluarDataService = new DualCelluarDataService(context);
                    sInstance = dualCelluarDataService;
                    try {
                        dualCelluarDataService.onCreate();
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
            synchronized (DualCelluarDataService.class) {
                DualCelluarDataService dualCelluarDataService = sInstance;
                if (dualCelluarDataService != null) {
                    try {
                        dualCelluarDataService.onDestroy();
                    } catch (Exception e) {
                        Log.e(TAG, "destroyInstance onDestroy catch:", e);
                    }
                    sInstance = null;
                }
            }
        }
    }

    public void onCreate() {
        log("DualCelluarDataService onCreate");
        setDualDataEnable(false);
        initRedundantModeApplicationPN();
        IActivityManager service = ActivityManager.getService();
        this.mIActivityManager = service;
        try {
            service.registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            log(" could not get IActivityManager");
        }
    }

    public void onDestroy() {
        log("DualCelluarDataService onDestroy");
        IActivityManager service = ActivityManager.getService();
        this.mIActivityManager = service;
        try {
            service.unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            log(" could not get IActivityManager");
        }
    }

    void initRedundantModeApplicationPN() {
        HashSet hashSet = new HashSet();
        this.mRedundantModeApplicationPN = hashSet;
        hashSet.add("com.miui.vpnsdkmanager");
        this.mRedundantModeApplicationPN.add("com.miui.securityadd");
        this.mRedundantModeApplicationPN.add("cn.wsds.gamemaster");
    }

    public boolean supportDualCelluarData() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return false;
        }
        try {
            boolean support = this.mContext.getResources().getBoolean(285540414);
            log("supportDualCelluarData: " + support);
            return support;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean supportMediaPlayerPolicy() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return false;
        }
        try {
            boolean support = this.mContext.getResources().getBoolean(285540443);
            log("supportMediaPlayerPolicy: " + support);
            return support;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setDualCelluarDataEnable(boolean enable) {
        int errorCode = 0;
        if (!supportDualCelluarData()) {
            errorCode = 101;
        }
        if (enable == getDualDataEnable()) {
            errorCode = ERROR_CODE_OTHER_DUPLICATE_SETTING;
        }
        log("setDualCelluarDataEnable: " + errorCode);
        if (errorCode == 0) {
            setDualDataEnable(enable);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processWhiteListAppDied(int uid) {
        String[] pkgNames = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (pkgNames != null && pkgNames.length > 0 && this.mRedundantModeApplicationPN != null) {
            for (String packageName : pkgNames) {
                if (this.mRedundantModeApplicationPN.contains(packageName)) {
                    log(" processWhiteListAppDied packageName: " + packageName);
                    setDualDataEnable(false);
                }
            }
        }
    }

    private void setDualDataEnable(boolean z) {
        log(" setDualDataEnable" + z);
        Settings.Global.putInt(this.mContext.getContentResolver(), APPLICATION_DUAL_MOBILE_DATA, z ? 1 : 0);
    }

    private boolean getDualDataEnable() {
        int enable = Settings.Global.getInt(this.mContext.getContentResolver(), APPLICATION_DUAL_MOBILE_DATA, -1);
        return enable == 1;
    }

    private void log(String s) {
        Log.d(TAG, s);
    }
}
