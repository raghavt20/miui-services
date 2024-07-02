package com.android.server.location;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.location.Order;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.provider.MockLocationProvider;
import com.android.server.location.provider.MockableLocationProvider;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class GnssMockLocationOptImpl implements GnssMockLocationOptStub {
    private static final int ALWAYS_MOCK_MODE = 2;
    private static final int ALWAYS_NONMOCK_MODE = 2;
    private static final String MOCK_REMOVE = "0";
    private static final String MOCK_SETUP = "1";
    private static final int ONLY_MOCK_ONECE = 1;
    private static final String PERSIST_FOR_GMO_VERSION = "persist.sys.gmo.version";
    private static final String TAG = "GnssMockLocationOpt";
    private static Context mContext;
    private static final HashSet<String> sBatList;
    private static boolean sFusedProviderStatus;
    private static boolean sGpsProviderStatus;
    private static boolean sLastFlag;
    private static int sLastMockAppPid;
    private static boolean sMockFlagSetByUser;
    private static boolean sNetworkProviderStatus;
    private boolean mAlreadyLoadControlFlag;
    private CallerIdentity mCallerIdentity;
    private boolean mChangeModeInNaviCondition;
    private long mCountRemoveTimes;
    private MockableLocationProvider mFusedProvider;
    private MockableLocationProvider mGpsProvider;
    private boolean mIsMockMode;
    private boolean mIsValidInterval;
    private String mLatestMockApp;
    private String mName;
    private MockableLocationProvider mNetworkProvider;
    private MockableLocationProvider mProvider;
    private boolean mRemovedBySystem;
    private long mTimeInterval;
    private long mTimeStart;
    private long mTimeStop;
    private boolean mEnableGnssMockLocationOpt = true;
    private Set<String> mBatUsing = ConcurrentHashMap.newKeySet();
    private final boolean D = SystemProperties.getBoolean("persist.sys.gnss_dc.test", false);
    private Map<Long, String> mRecordMockSchedule = new ConcurrentHashMap();
    private boolean mPermittedRunning = true;
    private boolean mEnableGmoControlStatus = true;
    private final File mGmoCloudFlagFile = new File(new File(Environment.getDataDirectory(), "system"), "GmoCloudFlagFile.xml");
    private final AtomicInteger mGmoVersion = new AtomicInteger(SystemProperties.getInt(PERSIST_FOR_GMO_VERSION, 0));

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssMockLocationOptImpl> {

        /* compiled from: GnssMockLocationOptImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssMockLocationOptImpl INSTANCE = new GnssMockLocationOptImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssMockLocationOptImpl m1761provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssMockLocationOptImpl m1760provideNewInstance() {
            return new GnssMockLocationOptImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        sBatList = hashSet;
        hashSet.add("com.baidu.BaiduMap");
        hashSet.add("com.autonavi.minimap");
        hashSet.add("com.tencent.map");
        sLastMockAppPid = 1000;
    }

    GnssMockLocationOptImpl() {
        Order.setOnChangeListener(new Order.OnChangeListener() { // from class: com.android.server.location.GnssMockLocationOptImpl.1
            @Override // com.android.server.location.Order.OnChangeListener
            public void onChange() {
                GnssMockLocationOptImpl.this.mRecordMockSchedule.put(Long.valueOf(SystemClock.elapsedRealtime()), Order.getFlag());
                if (GnssMockLocationOptImpl.this.D) {
                    Log.d(GnssMockLocationOptImpl.TAG, "callback set function");
                }
            }
        });
    }

    public void handleNaviBatRegisteration(Context context, CallerIdentity callerIdentity, String name, MockableLocationProvider provider) {
        boolean isStart;
        if (this.mEnableGnssMockLocationOpt) {
            if (context == null) {
                if (this.D) {
                    Log.d(TAG, "context is null");
                }
            } else {
                mContext = context;
                this.mProvider = provider;
                this.mName = name;
                if (!this.mAlreadyLoadControlFlag) {
                    boolean gmoVersionEnabled = this.mGmoVersion.get() != 0;
                    boolean cloudControlFlag = loadCloudDataFromSP(mContext);
                    this.mEnableGmoControlStatus = cloudControlFlag && gmoVersionEnabled;
                    Log.d(TAG, "GMO version: " + String.valueOf(this.mGmoVersion.get()) + ", cloud control flag: " + String.valueOf(cloudControlFlag));
                }
            }
            String identity = "[" + name + "-" + callerIdentity + "]";
            if (isNaviBat(callerIdentity.getPackageName())) {
                if (this.D) {
                    Log.d(TAG, "handleNaviBatRegisteration, add " + identity + " done");
                }
                if (this.mBatUsing.isEmpty()) {
                    isStart = true;
                } else {
                    isStart = false;
                }
                this.mBatUsing.add(identity);
                if (isStart) {
                    this.mTimeStart = SystemClock.elapsedRealtime();
                    if (sMockFlagSetByUser) {
                        if (this.D) {
                            Log.d(TAG, "axis set begin with mock");
                        }
                        this.mRecordMockSchedule.put(Long.valueOf(this.mTimeStart), "1");
                    } else {
                        if (this.D) {
                            Log.d(TAG, "axis set begin with nonmock");
                        }
                        this.mRecordMockSchedule.put(Long.valueOf(this.mTimeStart), MOCK_REMOVE);
                    }
                    if (this.mEnableGmoControlStatus) {
                        synchronized (GnssMockLocationOptImpl.class) {
                            if (sLastFlag) {
                                this.mCountRemoveTimes = 0L;
                                removeMock();
                                Log.d(TAG, "begin navigation with mock, remove mock process");
                            } else {
                                this.mRemovedBySystem = false;
                                Log.d(TAG, "begin navigation with nonmock, nothing to do");
                            }
                        }
                    }
                }
            }
        }
    }

    public void handleNaviBatUnregisteration(CallerIdentity callerIdentity, String name, MockableLocationProvider provider) {
        if (this.mEnableGnssMockLocationOpt) {
            String identity = "[" + name + "-" + callerIdentity + "]";
            if (isNaviBat(callerIdentity.getPackageName())) {
                if (this.D) {
                    Log.d(TAG, "handleNaviBatUnregisteration, remove " + identity + " done");
                }
                this.mBatUsing.remove(identity);
                if (this.mBatUsing.isEmpty()) {
                    this.mTimeStop = SystemClock.elapsedRealtime();
                    if (sMockFlagSetByUser) {
                        if (this.D) {
                            Log.d(TAG, "axis set end with mock");
                        }
                        this.mRecordMockSchedule.put(Long.valueOf(this.mTimeStop), "1");
                    } else {
                        if (this.D) {
                            Log.d(TAG, "axis set end with nonmock");
                        }
                        this.mRecordMockSchedule.put(Long.valueOf(this.mTimeStop), MOCK_REMOVE);
                    }
                    synchronized (GnssMockLocationOptImpl.class) {
                        if (!this.mEnableGmoControlStatus) {
                            dealingProcess();
                            return;
                        }
                        if (!this.mChangeModeInNaviCondition) {
                            if (this.mRemovedBySystem) {
                                restoreMock();
                                sLastFlag = true;
                                this.mRemovedBySystem = false;
                                Log.d(TAG, "restore begin mock, and no interrupt by third app");
                            } else {
                                sLastFlag = false;
                            }
                        } else {
                            restoreMock();
                            sLastFlag = true;
                            Log.d(TAG, "force restore command");
                        }
                        this.mPermittedRunning = true;
                        dealingProcess();
                    }
                }
            }
        }
    }

    private boolean isNaviBat(String name) {
        if (sBatList.contains(name)) {
            if (this.D) {
                Log.d(TAG, name + " is navi app");
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isMockInfluence(String name, MockableLocationProvider provider) {
        boolean z;
        boolean z2;
        if (this.D) {
            Log.d(TAG, name);
        }
        if ("gps".equalsIgnoreCase(name)) {
            this.mGpsProvider = provider;
            if (provider.isMock()) {
                sGpsProviderStatus = true;
            } else {
                sGpsProviderStatus = false;
            }
        }
        if ("fused".equalsIgnoreCase(name)) {
            this.mFusedProvider = provider;
            if (provider.isMock()) {
                sFusedProviderStatus = true;
            } else {
                sFusedProviderStatus = false;
            }
        }
        if ("network".equalsIgnoreCase(name)) {
            this.mNetworkProvider = provider;
            if (provider.isMock()) {
                sNetworkProviderStatus = true;
            } else {
                sNetworkProviderStatus = false;
            }
        }
        if (sGpsProviderStatus && this.D) {
            Log.d(TAG, "gps is mock");
        }
        if (sFusedProviderStatus && this.D) {
            Log.d(TAG, "fused is mock");
        }
        if (sNetworkProviderStatus && this.D) {
            Log.d(TAG, "network is mock");
        }
        boolean z3 = sGpsProviderStatus;
        return z3 || (z = sFusedProviderStatus) || (z2 = sNetworkProviderStatus) || z3 || z || z2;
    }

    public boolean recordOrderSchedule(boolean flag, String name, MockableLocationProvider provider, MockLocationProvider testProvider) {
        boolean done = sLastFlag == flag;
        if (this.mEnableGmoControlStatus) {
            if (this.D) {
                int pid = Binder.getCallingPid();
                Log.d(TAG, "pid: " + String.valueOf(pid) + ", android os identity: " + String.valueOf(Process.myPid()));
            }
            getLatestMockAppName(mContext);
            if (!flag && testProvider != null && !this.mBatUsing.isEmpty()) {
                if (this.D) {
                    Log.d(TAG, "third app add test provider");
                }
                isMockInfluence(name, provider);
                this.mChangeModeInNaviCondition = true;
                return false;
            }
            if (!flag && testProvider == null && !this.mBatUsing.isEmpty()) {
                this.mChangeModeInNaviCondition = false;
            }
            if (!flag && testProvider != null && this.mBatUsing.isEmpty()) {
                if (this.D) {
                    Log.d(TAG, "recover add test provider");
                }
                return true;
            }
            if (this.D) {
                Log.d(TAG, "flag: " + String.valueOf(flag) + ", provider: " + String.valueOf(testProvider == null));
            }
            if (!flag && testProvider == null) {
                if (this.D) {
                    Log.d(TAG, "system process: " + String.valueOf(this.mLatestMockApp.equalsIgnoreCase("system")) + ", fact process: " + this.mLatestMockApp + ", judge flag: " + String.valueOf(this.mRemovedBySystem));
                }
                synchronized (GnssMockLocationOptImpl.class) {
                    if (this.mRemovedBySystem) {
                        long j = this.mCountRemoveTimes + 1;
                        this.mCountRemoveTimes = j;
                        if (j != 1) {
                            if (this.D) {
                                Log.d(TAG, "interrupt by third app");
                            }
                            this.mRemovedBySystem = false;
                            this.mCountRemoveTimes = 0L;
                        }
                    }
                    if (!this.mRemovedBySystem) {
                        if (this.D) {
                            Log.d(TAG, "reset all provider");
                        }
                        sNetworkProviderStatus = false;
                        sGpsProviderStatus = false;
                        sFusedProviderStatus = false;
                    }
                }
            }
            if (this.D) {
                Log.d(TAG, "bat null: " + String.valueOf(this.mBatUsing.isEmpty()) + ", permitted: " + String.valueOf(this.mPermittedRunning));
            }
            if (this.mBatUsing.isEmpty() && this.mPermittedRunning) {
                if (this.D) {
                    Log.d(TAG, "mock permitted");
                }
            } else {
                if (this.D) {
                    Log.d(TAG, "mock denied");
                }
                return false;
            }
        }
        if (!done) {
            String flage = flag ? "1" : MOCK_REMOVE;
            Order.setFlag(flage);
            Log.d(TAG, "mode switch to " + flage);
            if (flage.equals("1")) {
                sMockFlagSetByUser = true;
                isMockInfluence(name, provider);
            }
            if (flage.equals(MOCK_REMOVE)) {
                sMockFlagSetByUser = false;
            }
        }
        sLastFlag = flag;
        return true;
    }

    private void removeMock() {
        String str;
        String str2;
        synchronized (GnssMockLocationOptImpl.class) {
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    grantMockLocationPermission(mContext);
                    MockableLocationProvider mockableLocationProvider = this.mGpsProvider;
                    if (mockableLocationProvider != null && mockableLocationProvider.isMock()) {
                        Log.d(TAG, "remove gps test provider");
                        this.mGpsProvider.setMockProvider((MockLocationProvider) null);
                    }
                    MockableLocationProvider mockableLocationProvider2 = this.mFusedProvider;
                    if (mockableLocationProvider2 != null && mockableLocationProvider2.isMock()) {
                        Log.d(TAG, "remove fused test provider");
                        this.mFusedProvider.setMockProvider((MockLocationProvider) null);
                    }
                    MockableLocationProvider mockableLocationProvider3 = this.mNetworkProvider;
                    if (mockableLocationProvider3 != null && mockableLocationProvider3.isMock()) {
                        Log.d(TAG, "remove network test provider");
                        this.mNetworkProvider.setMockProvider((MockLocationProvider) null);
                    }
                    Binder.restoreCallingIdentity(identity);
                    str = TAG;
                    str2 = "remove mock provider in system process";
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    Binder.restoreCallingIdentity(identity);
                    str = TAG;
                    str2 = "remove mock provider in system process";
                }
                Log.d(str, str2);
                this.mRemovedBySystem = true;
                this.mCountRemoveTimes = 1L;
                this.mPermittedRunning = false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                Log.d(TAG, "remove mock provider in system process");
                throw th;
            }
        }
    }

    private void restoreMock() {
        long identity;
        String str;
        String str2;
        LocationManager locationManager;
        synchronized (GnssMockLocationOptImpl.class) {
            try {
                try {
                    try {
                        identity = Binder.clearCallingIdentity();
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                try {
                    try {
                        grantMockLocationPermission(mContext);
                        locationManager = (LocationManager) mContext.getSystemService("location");
                        locationManager.addTestProvider("gps", false, false, false, false, true, true, true, 1, 1);
                        Log.d(TAG, "recover gps test provider");
                    } catch (Exception e) {
                        e = e;
                        Log.e(TAG, e.toString());
                        Binder.restoreCallingIdentity(identity);
                        str = TAG;
                        str2 = "restore mock provider in system process";
                        Log.d(str, str2);
                    }
                } catch (Exception e2) {
                    e = e2;
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(identity);
                    Log.d(TAG, "restore mock provider in system process");
                    throw th;
                }
                if (!sFusedProviderStatus && 1 == 0) {
                    Log.d(TAG, "fused flag false");
                    if (!sNetworkProviderStatus && 1 == 0) {
                        Log.d(TAG, "network flag false");
                        Binder.restoreCallingIdentity(identity);
                        str = TAG;
                        str2 = "restore mock provider in system process";
                        Log.d(str, str2);
                    }
                    Log.d(TAG, "recover network test provider");
                    locationManager.addTestProvider("network", false, false, false, false, true, true, true, 1, 1);
                    Binder.restoreCallingIdentity(identity);
                    str = TAG;
                    str2 = "restore mock provider in system process";
                    Log.d(str, str2);
                }
                Log.d(TAG, "recover fused test provider");
                locationManager.addTestProvider("fused", false, false, false, false, true, true, true, 1, 1);
                if (!sNetworkProviderStatus) {
                    Log.d(TAG, "network flag false");
                    Binder.restoreCallingIdentity(identity);
                    str = TAG;
                    str2 = "restore mock provider in system process";
                    Log.d(str, str2);
                }
                Log.d(TAG, "recover network test provider");
                locationManager.addTestProvider("network", false, false, false, false, true, true, true, 1, 1);
                Binder.restoreCallingIdentity(identity);
                str = TAG;
                str2 = "restore mock provider in system process";
                Log.d(str, str2);
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    private void grantMockLocationPermission(Context context) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
        try {
            appOpsManager.setMode(58, 1000, "android", 0);
        } catch (Exception e) {
            Log.e(TAG, "grant mockLocationPermission failed, cause: " + Log.getStackTraceString(e));
        }
    }

    private void dealingProcess() {
        Map<Long, String> preResult = new HashMap<>();
        Iterator<Long> it = this.mRecordMockSchedule.keySet().iterator();
        while (it.hasNext()) {
            long key = it.next().longValue();
            if (key >= this.mTimeStart && key <= this.mTimeStop) {
                preResult.put(Long.valueOf(key), this.mRecordMockSchedule.get(Long.valueOf(key)));
            }
        }
        final Map<Long, String> result = new LinkedHashMap<>();
        preResult.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(new Consumer() { // from class: com.android.server.location.GnssMockLocationOptImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                result.put((Long) r2.getKey(), (String) ((Map.Entry) obj).getValue());
            }
        });
        try {
            if (!"1".equals(result.get(Long.valueOf(this.mTimeStart))) || !"1".equals(result.get(Long.valueOf(this.mTimeStop)))) {
                if ("1".equals(result.get(Long.valueOf(this.mTimeStart))) && MOCK_REMOVE.equals(result.get(Long.valueOf(this.mTimeStop)))) {
                    calculate(result, true);
                    if (this.D) {
                        Log.d(TAG, "mock to nonmock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
                    }
                    GnssEventTrackingStub.getInstance().usingBatInMockModeInterval(this.mTimeInterval);
                    resetCondition();
                    return;
                }
                if (MOCK_REMOVE.equals(result.get(Long.valueOf(this.mTimeStart))) && MOCK_REMOVE.equals(result.get(Long.valueOf(this.mTimeStop)))) {
                    if (result.size() == 2) {
                        this.mTimeInterval = 0L;
                        if (this.D) {
                            Log.d(TAG, "always nonmock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
                        }
                        resetCondition();
                        return;
                    }
                    calculate(result, false);
                    if (this.D) {
                        Log.d(TAG, "nonmock to nonmock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
                    }
                    GnssEventTrackingStub.getInstance().usingBatInMockModeInterval(this.mTimeInterval);
                    resetCondition();
                    return;
                }
                if (MOCK_REMOVE.equals(result.get(Long.valueOf(this.mTimeStart))) && "1".equals(result.get(Long.valueOf(this.mTimeStop)))) {
                    calculate(result, false);
                    if (this.D) {
                        Log.d(TAG, "nonmock to mock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
                    }
                    GnssEventTrackingStub.getInstance().usingBatInMockModeInterval(this.mTimeInterval);
                    resetCondition();
                    return;
                }
                return;
            }
            if (result.size() == 2) {
                this.mTimeInterval = this.mTimeStop - this.mTimeStart;
                if (this.D) {
                    Log.d(TAG, "always mock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
                }
                GnssEventTrackingStub.getInstance().usingBatInMockModeTimes(1L);
                GnssEventTrackingStub.getInstance().usingBatInMockModeInterval(this.mTimeInterval);
                resetCondition();
                return;
            }
            calculate(result, true);
            if (this.D) {
                Log.d(TAG, "mock to mock mode, time interval is " + String.valueOf(this.mTimeInterval) + "ms");
            }
            GnssEventTrackingStub.getInstance().usingBatInMockModeInterval(this.mTimeInterval);
            resetCondition();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void resetCondition() {
        this.mTimeInterval = 0L;
        this.mRecordMockSchedule.clear();
        this.mTimeStart = 0L;
        this.mTimeStop = 0L;
    }

    private void calculate(Map<Long, String> result, boolean flag) {
        int cnt = 0;
        int cntOdd = 0;
        int cntEven = 0;
        ArrayList<Long> oddTimePoint = new ArrayList<>();
        ArrayList<Long> evenTimePoint = new ArrayList<>();
        Iterator<Long> it = result.keySet().iterator();
        while (it.hasNext()) {
            long key = it.next().longValue();
            if (cnt % 2 == 0) {
                evenTimePoint.add(Long.valueOf(key));
                cntEven++;
            } else {
                oddTimePoint.add(Long.valueOf(key));
                cntOdd++;
            }
            cnt++;
        }
        for (int i = 0; i < cntOdd; i++) {
            this.mTimeInterval += oddTimePoint.get(i).longValue() - evenTimePoint.get(i).longValue();
        }
        if (!flag) {
            this.mTimeInterval = (this.mTimeStop - this.mTimeStart) - this.mTimeInterval;
        }
        if (flag) {
            GnssEventTrackingStub.getInstance().usingBatInMockModeTimes(cntOdd);
        } else {
            GnssEventTrackingStub.getInstance().usingBatInMockModeTimes(cntEven - 1);
        }
    }

    private void getLatestMockAppName(Context context) {
        if (context == null) {
            return;
        }
        int pid = Binder.getCallingPid();
        if (pid == sLastMockAppPid) {
            if (this.D) {
                Log.d(TAG, "duplicate, no need to record, " + String.valueOf(pid));
            }
        } else {
            sLastMockAppPid = pid;
            this.mLatestMockApp = getProcessName(context, pid);
            if (this.D) {
                Log.d(TAG, this.mLatestMockApp + " calling");
            }
        }
    }

    private String getProcessName(Context context, int pid) {
        String str = null;
        if (context == null) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List list = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : list) {
            try {
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            if (info.pid == pid) {
                return info.processName;
            }
            continue;
        }
        return str;
    }

    private void saveCloudDataToSP(Context context, boolean status) {
        try {
            if (context == null) {
                Log.d(TAG, "null context object");
                return;
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(this.mGmoCloudFlagFile, 0).edit();
            editor.putBoolean("mEnableGmoControlStatus", status);
            editor.apply();
            Log.d(TAG, "success to save mEnableGmoControlStatus, " + String.valueOf(status));
        } catch (Exception e) {
            Log.e(TAG, "failed to save mEnableGmoControlStatus, " + e.toString());
        }
    }

    private boolean loadCloudDataFromSP(Context context) {
        try {
            if (context == null) {
                Log.d(TAG, "null context object");
                return false;
            }
            Context directBootContext = context.createDeviceProtectedStorageContext();
            SharedPreferences editor = directBootContext.getSharedPreferences(this.mGmoCloudFlagFile, 0);
            boolean status = editor.getBoolean("mEnableGmoControlStatus", true);
            this.mAlreadyLoadControlFlag = true;
            Log.d(TAG, "success to load mEnableGmoControlStatus, " + String.valueOf(status));
            return status;
        } catch (Exception e) {
            Log.e(TAG, "failed to load mEnableGmoControlStatus, " + e.toString());
            return false;
        }
    }

    public void setMockLocationOptStatus(boolean status) {
        if (status) {
            saveCloudDataToSP(mContext, status);
            this.mEnableGmoControlStatus = true;
            Log.d(TAG, "enable GMO by cloud");
        } else {
            saveCloudDataToSP(mContext, status);
            this.mEnableGmoControlStatus = false;
            Log.d(TAG, "disable GMO by cloud");
        }
    }
}
