package com.android.server.location.gnss.datacollect;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.location.ThreadPoolUtil;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.gnss.GnssLocationProviderStub;
import com.android.server.location.gnss.hal.GnssPowerOptimizeStub;
import com.android.server.location.gnss.hal.GpoUtil;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class GnssEventTrackingImpl implements GnssEventTrackingStub {
    private static final String ACTION_UPLOAD_DATA = "action upload data";
    private static final String APP_REQUEST_GLP_CNT_AFT_GPO = "AppRequestGlpCntAftGpo";
    private static final String APP_REQUEST_GLP_CNT_BEF_GPO = "AppRequestGlpCntBefGpo";
    private static final String EVENT_APP_REQUEST_GLP_CNT = "AppRequestGlpCnt";
    private static final String EVENT_BLOCK_LIST_USAGE = "GNSS_BLOCK_LIST_USAGE";
    private static final String EVENT_GNSS_ENGINE_USAGE = "GNSS_ENGINE_USAGE";
    private static final String EVENT_GPS_USE_APP = "GPS_USE_APP";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String GMO_POSITION_INTERVAL = "gmo1_during";
    private static final String GMO_POSITION_TIMES = "count";
    private static final String GNSS_BACKGROUND_OPT = "GNSS_BACKGROUND_OPT";
    private static final String GNSS_MOCK_LOCATION_OPT = "GNSS_MOCK_LOCATION_OPT";
    private static final String GNSS_SATELLITE_CALL_OPT = "GNSS_SATELLITE_CALL_OPT";
    private static final String GPO3_CTRL_TYPE_ALL = "gpo3CtrlTypeAll";
    private static final String GPO3_CTRL_TYPE_NONE = "gpo3CtrlTypeNone";
    private static final String GPO3_CTRL_TYPE_PART = "gpo3CtrlTypePart";
    private static final String GPO3_TIME_AFT = "gpo3TimeAft";
    private static final String GPO3_TIME_BEF = "gpo3TimeBef";
    private static final String NAV_APP_TIME = "NavAppTime";
    private static final String PACKAGE_NAME = "packageName";
    private static final int REQUEST_CODE_GNSS_ENGINE_USAGE = 3;
    private static final int REQUEST_CODE_SATELLITE_CALL_OPT = 4;
    private static final int REQUEST_CODE_USE_APP = 0;
    private static final String TAG = "GnssSavePoint";
    private static final int TYPE_GNSS_ENGINE_CONTROL_ALL = 0;
    private static final int TYPE_GNSS_ENGINE_CONTROL_NONE = 2;
    private static final int TYPE_GNSS_ENGINE_CONTROL_PART = 1;
    private final boolean D;
    private int START_INTERVAL;
    private int UPLOAD_REPEAT_TIME;
    private boolean hasStartUploadData;
    private final boolean isSavePoint;
    private final Map<String, AppRequestCtl> mAppRequestCtlMap;
    private AtomicInteger mBackgroundOpt2Cnt;
    private AtomicInteger mBackgroundOpt3Cnt;
    private final List<BlocklistControlBean> mBlocklistControlBeanList;
    private long mEngineBlockTime;
    private long mEngineControlTime;
    private long mEngineStartTime;
    private long mEngineStopTime;
    private long mEngineTimeAftGpo3;
    private long mEngineTimeBefGpo3;
    private long mGlpBackTime;
    private Context mGlpContext;
    private final Map<String, Long> mGlpDuringBackground;
    private final Map<String, Long> mGlpDuringMap;
    private long mGlpForeTime;
    private AtomicLong mGnssMockLocationInterval;
    private AtomicLong mGnssMockLocationTimes;
    private boolean mIsGnssPowerRecord;
    private int mLastEngineStatus;
    private final List<GnssEngineUsage> mListEngineUsage;
    private final List<UseGnssAppBean> mListUseApp;
    private final Map<String, Long> mNavAppTimeMap;
    private final BroadcastReceiver mReceiver;
    private final Map<Integer, UseGnssAppBean> mRequestMap;
    private AtomicInteger mSatelliteCallCnt;
    private long mSatelliteCallDuring;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssEventTrackingImpl> {

        /* compiled from: GnssEventTrackingImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssEventTrackingImpl INSTANCE = new GnssEventTrackingImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssEventTrackingImpl m1807provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssEventTrackingImpl m1806provideNewInstance() {
            return new GnssEventTrackingImpl();
        }
    }

    GnssEventTrackingImpl() {
        boolean z = SystemProperties.getBoolean("persist.sys.miui_gnss_dc", false);
        this.isSavePoint = z;
        boolean z2 = SystemProperties.getBoolean("persist.sys.gnss_dc.test", false);
        this.D = z2;
        this.UPLOAD_REPEAT_TIME = 86400000;
        this.START_INTERVAL = 300000;
        this.mIsGnssPowerRecord = true;
        this.mLastEngineStatus = 4;
        this.mListUseApp = new CopyOnWriteArrayList();
        this.mRequestMap = new ConcurrentHashMap();
        this.mGlpDuringMap = new ConcurrentHashMap();
        this.mGlpDuringBackground = new ConcurrentHashMap();
        this.mListEngineUsage = new CopyOnWriteArrayList();
        this.mAppRequestCtlMap = new ConcurrentHashMap();
        this.mNavAppTimeMap = new ConcurrentHashMap();
        this.mBlocklistControlBeanList = new CopyOnWriteArrayList();
        this.mBackgroundOpt2Cnt = new AtomicInteger(0);
        this.mBackgroundOpt3Cnt = new AtomicInteger(0);
        this.mSatelliteCallCnt = new AtomicInteger(0);
        this.mGnssMockLocationTimes = new AtomicLong(0L);
        this.mGnssMockLocationInterval = new AtomicLong(0L);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.location.gnss.datacollect.GnssEventTrackingImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (GnssEventTrackingImpl.ACTION_UPLOAD_DATA.equals(intent.getAction())) {
                    GnssEventTrackingImpl.this.startUploadGnssData(context);
                }
            }
        };
        if (z2) {
            this.UPLOAD_REPEAT_TIME = 3000;
            this.START_INTERVAL = 3000;
        }
        Log.d(TAG, "Is specified platform:" + z + "  upload repeat time:" + this.UPLOAD_REPEAT_TIME + "  start interval:" + this.START_INTERVAL);
    }

    public void init(Context context) {
        this.mGlpContext = context;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startUploadGnssData(final Context context) {
        ThreadPoolUtil.getInstance().execute(new Runnable() { // from class: com.android.server.location.gnss.datacollect.GnssEventTrackingImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GnssEventTrackingImpl.this.lambda$startUploadGnssData$0(context);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startUploadGnssData$0(Context context) {
        startUploadUseApp(context);
        startUploadAppRequest(context);
        startUploadGnssEngineUsage(context);
        startUploadBlockListUsage(context);
        startUploadBackgroundOpt(context);
        startUploadGnssMockLocationOpt(context);
        startUploadGnssSatelliteCallOpt(context);
    }

    public void recordGnssSatelliteCallOptCnt() {
        Context context;
        int cnt = this.mSatelliteCallCnt.incrementAndGet();
        if (this.D) {
            Log.d(TAG, "recordGnssSatelliteCallOptCnt:" + cnt);
        }
        if (!this.hasStartUploadData && (context = this.mGlpContext) != null) {
            this.hasStartUploadData = true;
            setAlarm(context, ACTION_UPLOAD_DATA, 4);
        }
    }

    public void recordGnssSatelliteCallOptDuring(long during) {
        this.mSatelliteCallDuring += during;
        if (this.D) {
            Log.d(TAG, "recordGnssSatelliteCallOptDuring:" + this.mSatelliteCallDuring);
        }
    }

    private void startUploadGnssSatelliteCallOpt(Context context) {
        synchronized (GnssEventTrackingImpl.class) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("EVENT_NAME", GNSS_SATELLITE_CALL_OPT);
                jsonObject.put(GMO_POSITION_TIMES, this.mSatelliteCallCnt.get());
                jsonObject.put("during", this.mSatelliteCallDuring);
                GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                instance.init(context);
                instance.track(jsonObject);
            } catch (RemoteException e) {
                Log.e(TAG, "startUploadGnssSatelliteCallOpt RemoteException!");
            } catch (JSONException e2) {
                Log.e(TAG, "startUploadGnssSatelliteCallOpt JSONException!");
            }
            this.mSatelliteCallCnt.set(0);
            this.mSatelliteCallDuring = 0L;
        }
    }

    private void recordCallerGnssApp(Context context, String packageName, String interval, long glpDuring, long glpBackgroundDuring) {
        if (context == null || packageName == null || interval == null) {
            return;
        }
        if (!this.isSavePoint) {
            if (this.D) {
                Log.d(TAG, "record Use Gnss App----> Not the specified platform");
                return;
            }
            return;
        }
        if (this.mListUseApp.size() >= Integer.MAX_VALUE) {
            Log.v(TAG, "use app list.size() >= Integer.MAX_VALUE----> return");
            return;
        }
        UseGnssAppBean bean = new UseGnssAppBean();
        bean.packageName = packageName;
        bean.reportInterval = interval;
        Map<String, Long> map = this.mGlpDuringMap;
        map.put(packageName, Long.valueOf(map.getOrDefault(packageName, 0L).longValue() + glpDuring));
        Map<String, Long> map2 = this.mGlpDuringBackground;
        map2.put(packageName, Long.valueOf(map2.getOrDefault(packageName, 0L).longValue() + glpBackgroundDuring));
        this.mListUseApp.add(bean);
        if (this.D) {
            Log.d(TAG, "record Use Gnss App----> " + packageName);
        }
        if (!this.hasStartUploadData) {
            this.hasStartUploadData = true;
            setAlarm(context, ACTION_UPLOAD_DATA, 0);
        }
    }

    public void recordRequest(String provider, int callingIdentityHashCode, String pkgName, long intervalMs) {
        if ("gps".equals(provider)) {
            UseGnssAppBean bean = new UseGnssAppBean();
            bean.requestTime = SystemClock.elapsedRealtime();
            bean.packageName = pkgName;
            bean.reportInterval = String.valueOf(intervalMs);
            bean.changeToBackTime = SystemClock.elapsedRealtime();
            bean.changeToforeTime = bean.changeToBackTime;
            this.mRequestMap.put(Integer.valueOf(callingIdentityHashCode), bean);
            putIntoAppRequestCtlMap(pkgName);
            if (this.D) {
                Log.d(TAG, provider + " recordRequest callingIdentity:" + callingIdentityHashCode);
            }
        }
    }

    public void recordChangeToBackground(String provider, int callingIdentityHashCode, String pkgName, boolean foreground, boolean hasLocationPermissions) {
        UseGnssAppBean bean;
        if (!hasLocationPermissions || !"gps".equals(provider) || (bean = this.mRequestMap.get(Integer.valueOf(callingIdentityHashCode))) == null || bean.requestTime == 0) {
            return;
        }
        if (foreground) {
            bean.changeToforeTime = SystemClock.elapsedRealtime();
            this.mGlpBackTime += bean.changeToforeTime - bean.changeToBackTime;
        } else {
            bean.changeToBackTime = SystemClock.elapsedRealtime();
            this.mGlpForeTime += bean.changeToBackTime - bean.changeToforeTime;
        }
        if (this.D) {
            Log.d(TAG, provider + " recordChangeToBackground callingIdentity:" + callingIdentityHashCode + "pkgName:" + pkgName + (foreground ? " foreground" : " background") + " GlpForeTime:" + this.mGlpForeTime + " GlpBackTime:" + this.mGlpBackTime);
        }
        this.mRequestMap.put(Integer.valueOf(callingIdentityHashCode), bean);
    }

    public void recordRemove(String provider, int callingIdentityHashCode, boolean foreground, boolean hasLocationPermissions) {
        if ("gps".equals(provider)) {
            UseGnssAppBean bean = this.mRequestMap.get(Integer.valueOf(callingIdentityHashCode));
            recordAppRemove(bean);
            if (bean != null && bean.requestTime != 0) {
                long glpDuring = SystemClock.elapsedRealtime() - bean.requestTime;
                if (!hasLocationPermissions) {
                    Log.d(TAG, "do not have Location Permissions, do not need record background time...");
                } else if (bean.changeToforeTime < bean.changeToBackTime) {
                    this.mGlpBackTime = glpDuring - this.mGlpForeTime;
                    if (this.D) {
                        Log.d(TAG, "remove on the background and true BackTime is " + this.mGlpBackTime);
                    }
                } else if (bean.changeToforeTime == bean.changeToBackTime && !foreground) {
                    this.mGlpBackTime = glpDuring;
                }
                recordCallerGnssApp(this.mGlpContext, bean.packageName, bean.reportInterval, glpDuring, this.mGlpBackTime);
                if (this.D) {
                    Log.d(TAG, "packageName:" + bean.packageName + " glpDuring:" + glpDuring + " GlpBackTime:" + this.mGlpBackTime);
                }
            }
            this.mRequestMap.remove(Integer.valueOf(callingIdentityHashCode));
            this.mGlpBackTime = 0L;
            this.mGlpForeTime = 0L;
        }
    }

    private void putIntoAppRequestCtlMap(String pkgName) {
        if (this.mAppRequestCtlMap.containsKey(pkgName)) {
            this.mAppRequestCtlMap.get(pkgName).addRequestCnt(1, 1);
        } else {
            this.mAppRequestCtlMap.put(pkgName, new AppRequestCtl());
        }
    }

    private void recordAppRemove(UseGnssAppBean bean) {
        AppRequestCtl appRequestCtl;
        if (bean != null && (appRequestCtl = this.mAppRequestCtlMap.get(bean.packageName)) != null && this.mLastEngineStatus == 1) {
            appRequestCtl.addRequestCnt(0, -1);
        }
    }

    public void recordEngineUsage(int type, long milliseconds) {
        synchronized (GnssEventTrackingImpl.class) {
            if (this.mLastEngineStatus == type) {
                return;
            }
            this.mLastEngineStatus = type;
            if (this.D) {
                Log.d(TAG, "recordEngineUsage, type=" + type + ", currentTime=" + milliseconds);
            }
            switch (type) {
                case 1:
                    this.mEngineBlockTime = milliseconds;
                    break;
                case 2:
                    long j = this.mEngineStartTime;
                    if (j != 0) {
                        long j2 = this.mEngineControlTime;
                        if (j2 != 0) {
                            this.mEngineTimeAftGpo3 += j2 - j;
                            this.mEngineControlTime = 0L;
                        }
                    }
                    this.mEngineStartTime = milliseconds;
                    if (this.mEngineBlockTime == 0) {
                        this.mEngineBlockTime = milliseconds;
                        break;
                    }
                    break;
                case 3:
                    this.mEngineControlTime = milliseconds;
                    break;
                case 4:
                    calCurEngineUsage(milliseconds);
                    break;
                default:
                    resetGpo3Var();
                    break;
            }
            if (!this.hasStartUploadData) {
                this.hasStartUploadData = true;
                setAlarm(this.mGlpContext, ACTION_UPLOAD_DATA, 3);
            }
        }
    }

    public void recordNavAppTime(String pkn, long time) {
        if (pkn == null || time <= 0) {
            return;
        }
        Map<String, Long> map = this.mNavAppTimeMap;
        map.put(pkn, Long.valueOf(map.getOrDefault(pkn, 0L).longValue() + time));
    }

    private void calCurEngineUsage(long milliseconds) {
        synchronized (GnssEventTrackingImpl.class) {
            this.mEngineStopTime = milliseconds;
            if (this.mEngineControlTime == 0) {
                this.mEngineControlTime = milliseconds;
            }
            long j = this.mEngineStartTime;
            if (j == 0) {
                this.mEngineControlTime = j;
            }
            this.mEngineTimeBefGpo3 = milliseconds - this.mEngineBlockTime;
            this.mEngineTimeAftGpo3 += this.mEngineControlTime - j;
            String dumpInfo = "recordEngineUsage, mEngineTimeBefGpo3=" + this.mEngineTimeBefGpo3 + ", mEngineTimeAftGpo3=" + this.mEngineTimeAftGpo3 + ", saved milliseconds is " + (this.mEngineTimeBefGpo3 - this.mEngineTimeAftGpo3);
            if (this.D) {
                Log.d(TAG, dumpInfo);
            }
            long j2 = this.mEngineTimeBefGpo3;
            long j3 = this.mEngineTimeAftGpo3;
            if (j2 >= j3 && j2 > 0) {
                GnssEngineUsage gnssEngineUsage = new GnssEngineUsage(j2, j3);
                this.mListEngineUsage.add(gnssEngineUsage);
                GnssLocationProviderStub.getInstance().writeLocationInformation(dumpInfo);
            }
            resetGpo3Var();
        }
    }

    public void recordSatelliteBlockListChanged(long mTotalNaviTime, long mEffectiveTime, String mPkn) {
        if (mTotalNaviTime > 0 && mEffectiveTime > 0) {
            BlocklistControlBean mBlocklistControlBean = new BlocklistControlBean(mTotalNaviTime, mEffectiveTime, mPkn);
            this.mBlocklistControlBeanList.add(mBlocklistControlBean);
            GnssLocationProviderStub.getInstance().writeLocationInformation(mBlocklistControlBean.toString());
            if (this.D) {
                Log.d("TAG", mBlocklistControlBean.toString());
            }
        }
    }

    public void recordGnssBackgroundOpt2Time() {
        int cnt = this.mBackgroundOpt2Cnt.incrementAndGet();
        if (this.D) {
            Log.d(TAG, "recordGnssBackgroundOptTime:" + cnt);
        }
    }

    public void recordGnssBackgroundOpt3Time() {
        int cnt = this.mBackgroundOpt3Cnt.incrementAndGet();
        if (this.D) {
            Log.d(TAG, "recordGnssBackgroundOptTime:" + cnt);
        }
    }

    private void resetGpo3Var() {
        synchronized (GnssEventTrackingImpl.class) {
            this.mEngineBlockTime = 0L;
            this.mEngineStartTime = 0L;
            this.mEngineStopTime = 0L;
            this.mEngineControlTime = 0L;
            this.mEngineTimeBefGpo3 = 0L;
            this.mEngineTimeAftGpo3 = 0L;
        }
    }

    private void startUploadBackgroundOpt(Context context) {
        synchronized (GnssEventTrackingImpl.class) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("EVENT_NAME", GNSS_BACKGROUND_OPT);
                jsonObject.put("count2", this.mBackgroundOpt2Cnt.get());
                jsonObject.put("count3", this.mBackgroundOpt3Cnt.get());
                GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                instance.init(context);
                instance.track(jsonObject);
            } catch (RemoteException e) {
                Log.e(TAG, "startUploadBackgroundOpt RemoteException!");
            } catch (JSONException e2) {
                Log.e(TAG, "startUploadBackgroundOpt JSONException!");
            }
            this.mBackgroundOpt2Cnt.set(0);
            this.mBackgroundOpt3Cnt.set(0);
        }
    }

    private void startUploadGnssEngineUsage(Context context) {
        synchronized (GnssEventTrackingImpl.class) {
            try {
                try {
                    if (this.D) {
                        Log.d(TAG, "startUploadGnssEngineUsage");
                    }
                    long timeBef = 0;
                    long timeAft = 0;
                    long typeCtrlAll = 0;
                    long typeCtrlPart = 0;
                    long typeCtrlNone = 0;
                    try {
                        for (GnssEngineUsage usage : this.mListEngineUsage) {
                            timeBef += usage.getGnssEngineShouldUseTime();
                            timeAft += usage.getGnssEngineActuallyUseTime();
                            switch (usage.getGnssEngineControlState()) {
                                case 0:
                                    typeCtrlAll++;
                                    break;
                                case 1:
                                default:
                                    typeCtrlPart++;
                                    break;
                                case 2:
                                    typeCtrlNone++;
                                    break;
                            }
                        }
                    } catch (RemoteException e) {
                    } catch (JSONException e2) {
                    }
                    if (timeBef == 0) {
                        if (this.D) {
                            Log.d(TAG, "No gnss data, do not upload GnssEngineUsage");
                        }
                        return;
                    }
                    try {
                    } catch (RemoteException e3) {
                        Log.e(TAG, "startUploadGnssEngineUsage track RemoteException");
                        this.mListEngineUsage.clear();
                        return;
                    } catch (JSONException e4) {
                        Log.e(TAG, "startUploadGnssEngineUsage JSONException!");
                        this.mListEngineUsage.clear();
                        return;
                    }
                    if (GpoUtil.getInstance().getGpoVersion() >= 3 && !GpoUtil.getInstance().checkHeavyUser()) {
                        GnssPowerOptimizeStub.getInstance().recordEngineUsageDaily(timeAft);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("EVENT_NAME", EVENT_GNSS_ENGINE_USAGE);
                        jsonObject.put(GPO3_TIME_BEF, timeBef);
                        jsonObject.put(GPO3_TIME_AFT, timeAft);
                        jsonObject.put(GPO3_CTRL_TYPE_ALL, typeCtrlAll);
                        jsonObject.put(GPO3_CTRL_TYPE_PART, typeCtrlPart);
                        jsonObject.put(GPO3_CTRL_TYPE_NONE, typeCtrlNone);
                        GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                        instance.init(context);
                        instance.track(jsonObject);
                        if (this.D) {
                            Log.d(TAG, "startUploadGnssEngineUsage track success");
                        }
                        this.mListEngineUsage.clear();
                        return;
                    }
                    if (this.D) {
                        Log.d(TAG, "Not enable gpo or isHeavyUser");
                    }
                    GnssPowerOptimizeStub.getInstance().recordEngineUsageDaily(timeAft);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void startUploadAppRequest(Context context) {
        if (this.D) {
            Log.d(TAG, "startUploadAppRequest schedule current thread---->" + Thread.currentThread().getName());
        }
        if (this.mAppRequestCtlMap.isEmpty()) {
            if (this.D) {
                Log.d(TAG, "No App Request Data, skip upload.");
                return;
            }
            return;
        }
        if (this.mLastEngineStatus != 4) {
            if (this.D) {
                Log.d(TAG, "Gnss engine is working, skip upload.");
                return;
            }
            return;
        }
        synchronized (GnssEventTrackingImpl.class) {
            try {
                try {
                    List<String> dataList = new ArrayList<>();
                    for (Map.Entry<String, AppRequestCtl> entry : this.mAppRequestCtlMap.entrySet()) {
                        String pkg = entry.getKey();
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("EVENT_NAME", EVENT_APP_REQUEST_GLP_CNT);
                        jsonObject.put("packageName", pkg);
                        jsonObject.put(APP_REQUEST_GLP_CNT_BEF_GPO, this.mAppRequestCtlMap.get(pkg).getBefCtlCnt());
                        jsonObject.put(APP_REQUEST_GLP_CNT_AFT_GPO, this.mAppRequestCtlMap.get(pkg).getAftCtlCnt());
                        if (this.mNavAppTimeMap.containsKey(pkg)) {
                            jsonObject.put(NAV_APP_TIME, this.mNavAppTimeMap.get(pkg));
                        }
                        dataList.add(jsonObject.toString());
                    }
                    GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                    instance.init(context);
                    instance.track(dataList);
                    if (this.D) {
                        Log.d(TAG, "startUploadAppRequest track success");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "startUploadAppRequest JSONException!");
                }
            } catch (RemoteException e2) {
                Log.e(TAG, "startUploadAppRequest track RemoteException");
            }
            this.mAppRequestCtlMap.clear();
            this.mNavAppTimeMap.clear();
        }
    }

    private void startUploadUseApp(Context context) {
        if (this.D) {
            Log.d(TAG, "startUploadUseAppTimer schedule current thread---->" + Thread.currentThread().getName());
        }
        List<String> dataList = new ArrayList<>();
        Map<UseGnssAppBean, Integer> map = new HashMap<>();
        synchronized (GnssEventTrackingImpl.class) {
            for (UseGnssAppBean bean : this.mListUseApp) {
                map.put(bean, Integer.valueOf(map.getOrDefault(bean, 0).intValue() + 1));
            }
            try {
                for (Map.Entry<UseGnssAppBean, Integer> entry : map.entrySet()) {
                    String pkg = entry.getKey().packageName;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("EVENT_NAME", EVENT_GPS_USE_APP);
                    jsonObject.put("packageName", pkg);
                    jsonObject.put("report_interval", entry.getKey().reportInterval);
                    jsonObject.put("glp_during", this.mGlpDuringMap.get(pkg));
                    jsonObject.put("glp_during_background", this.mGlpDuringBackground.get(pkg));
                    jsonObject.put(GMO_POSITION_TIMES, "" + entry.getValue());
                    dataList.add(jsonObject.toString());
                }
                GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                instance.init(context);
                instance.track(dataList);
                if (this.D) {
                    Log.d(TAG, "startUploadUseApp track success");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "startUploadUseApp track RemoteException");
            } catch (JSONException e2) {
                Log.e(TAG, "recordUseGnssApp JSONException!");
            }
            this.mListUseApp.clear();
            this.mGlpDuringMap.clear();
            this.mGlpDuringBackground.clear();
        }
    }

    private void startUploadBlockListUsage(Context context) {
        if (this.D) {
            Log.d(TAG, "startUploadBlockListUsage schedule current thread---->" + Thread.currentThread().getName());
        }
        List<String> dataList = new ArrayList<>();
        int mCount = this.mBlocklistControlBeanList.size();
        if (mCount == 0) {
            return;
        }
        synchronized (this) {
            long mTotalTime = 0;
            long mEffectiveTime = 0;
            try {
                try {
                    for (BlocklistControlBean mBLControlBean : this.mBlocklistControlBeanList) {
                        mTotalTime += mBLControlBean.getTotalNaviTime();
                        mEffectiveTime = mBLControlBean.getBlockedTime();
                    }
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("EVENT_NAME", EVENT_BLOCK_LIST_USAGE);
                    jSONObject.put("navi_times", mCount);
                    jSONObject.put("total_navi_time", mTotalTime);
                    jSONObject.put("effective_time", mEffectiveTime);
                    dataList.add(jSONObject.toString());
                    GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                    instance.init(context);
                    instance.track(dataList);
                    if (this.D) {
                        Log.d(TAG, "startUploadBlockListUsage track success");
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "startUploadUseApp track RemoteException");
                }
            } catch (JSONException e2) {
                Log.e(TAG, "recordUseGnssApp JSONException!");
            }
            this.mBlocklistControlBeanList.clear();
        }
    }

    private void setAlarm(Context context, String action, int requestCode) {
        if (context != null && action != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(action);
            context.registerReceiver(this.mReceiver, filter);
            long token = Binder.clearCallingIdentity();
            try {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
                Intent intent = new Intent(action);
                try {
                    PendingIntent p = PendingIntent.getBroadcast(context, requestCode, intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    int i = this.UPLOAD_REPEAT_TIME;
                    alarmManager.setRepeating(2, elapsedRealtime + i, i, p);
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssEngineUsage {
        private long gnssEngineActuallyUseTime;
        private long gnssEngineShouldUseTime;

        private GnssEngineUsage(long gnssEngineShouldUseTime, long gnssEngineActuallyUseTime) {
            this.gnssEngineShouldUseTime = gnssEngineShouldUseTime;
            this.gnssEngineActuallyUseTime = gnssEngineActuallyUseTime;
        }

        public long getGnssEngineShouldUseTime() {
            return this.gnssEngineShouldUseTime;
        }

        public long getGnssEngineActuallyUseTime() {
            return this.gnssEngineActuallyUseTime;
        }

        public int getGnssEngineControlState() {
            long j = this.gnssEngineActuallyUseTime;
            if (j == 0) {
                return 0;
            }
            if (this.gnssEngineShouldUseTime == j) {
                return 2;
            }
            return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppRequestCtl {
        private int aftCtl;
        private int befCtl;

        private AppRequestCtl() {
            this.befCtl = 1;
            this.aftCtl = 1;
        }

        public void addRequestCnt(int befDeltaAdd, int aftDeltaAdd) {
            this.befCtl += befDeltaAdd;
            this.aftCtl += aftDeltaAdd;
        }

        public int getBefCtlCnt() {
            return this.befCtl;
        }

        public int getAftCtlCnt() {
            return this.aftCtl;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BlocklistControlBean {
        private long blockedTime;
        private String pkg;
        private long totalNaviTime;

        public BlocklistControlBean(long totalNaviTime, long blockedTime) {
            this.totalNaviTime = totalNaviTime;
            this.blockedTime = blockedTime;
        }

        public BlocklistControlBean(long totalNaviTime, long blockedTime, String pkg) {
            this.totalNaviTime = totalNaviTime;
            this.blockedTime = blockedTime;
            this.pkg = pkg;
        }

        public long getTotalNaviTime() {
            return this.totalNaviTime;
        }

        public void setTotalNaviTime(long totalNaviTime) {
            this.totalNaviTime = totalNaviTime;
        }

        public long getBlockedTime() {
            return this.blockedTime;
        }

        public void setBlockedTime(long blockedTime) {
            this.blockedTime = blockedTime;
        }

        public String getPkg() {
            return this.pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public String toString() {
            return "recordGnssBlocklistUsage,totalNaviTime=" + this.totalNaviTime + ", blockedTime=" + this.blockedTime + ", pkg=" + this.pkg;
        }
    }

    public void usingBatInMockModeTimes(long times) {
        long cnt = this.mGnssMockLocationTimes.addAndGet(times);
        if (this.D) {
            Log.d(TAG, "usingBatInMockModeTimes: " + cnt);
        }
    }

    public void usingBatInMockModeInterval(long interval) {
        long cnt = this.mGnssMockLocationInterval.addAndGet(interval);
        if (this.D) {
            Log.d(TAG, "usingBatInMockModeInterval: " + cnt);
        }
    }

    private void startUploadGnssMockLocationOpt(Context context) {
        synchronized (GnssEventTrackingImpl.class) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("EVENT_NAME", GNSS_MOCK_LOCATION_OPT);
                jsonObject.put(GMO_POSITION_TIMES, this.mGnssMockLocationTimes);
                jsonObject.put(GMO_POSITION_INTERVAL, this.mGnssMockLocationInterval);
                GnssOneTrackManager instance = GnssOneTrackManager.getInstance();
                instance.init(context);
                instance.track(jsonObject);
            } catch (RemoteException e) {
                Log.e(TAG, "startUploadGnssMockLocationOpt RemoteException!");
            } catch (JSONException e2) {
                Log.e(TAG, "startUploadGnssMockLocationOpt JSONException!");
            }
            this.mGnssMockLocationTimes.set(0L);
            this.mGnssMockLocationInterval.set(0L);
        }
    }
}
