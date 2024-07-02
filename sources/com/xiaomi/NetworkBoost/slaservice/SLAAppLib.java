package com.xiaomi.NetworkBoost.slaservice;

import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda3;
import com.android.server.wm.ActivityStarterImpl;
import com.xiaomi.NetworkBoost.NetworkAccelerateSwitch.NetworkAccelerateSwitchService;
import com.xiaomi.NetworkBoost.NetworkSDK.NetworkSDKService;
import database.SlaDbSchema.SlaBaseHelper;
import database.SlaDbSchema.SlaCursorWrapper;
import database.SlaDbSchema.SlaDbSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import miui.android.animation.internal.AnimTask;
import vendor.qti.sla.service.V1_0.ISlaService;

/* loaded from: classes.dex */
public class SLAAppLib {
    private static final String ACTION_DUAL_DATA_CONCURRENT_LIMITED_WHITE_LIST_DONE = "com.android.phone.intent.action.DUAL_DATA_CONCURRENT_LIMITED_WHITE_LIST_DONE";
    private static final String ACTION_DUAL_DATA_CONCURRENT_MODE_WHITE_LIST_DONE = "com.android.phone.intent.action.DUAL_DATA_CONCURRENT_MODE_WHITE_LIST_DONE";
    public static final int ADD_TO_WHITELIST = 1;
    private static final String CLOUD_DOUBLEDATA_LIMITED_WHITELIST = "dual_data_concurrent_limited_white_list_pkg_name";
    private static final String CLOUD_DOUBLEDATA_MODE_WHITELIST = "dual_data_concurrent_mode_white_list_pkg_name";
    private static final String CLOUD_DOUBLEWIFI_WHITELIST = "cloud_double_wifi_uidlist";
    private static final String CLOUD_SLA_WHITELIST = "cloud_sla_whitelist";
    private static final String CLOUD_SLS_GAMING_WHITELIST = "cloud_sls_whitelist";
    private static final boolean DEBUG = false;
    private static final int IS_DAY_CHANGE = 1;
    public static final int IS_IN_WHITELIST = 4;
    private static final int IS_MONTH_CHANGE = 2;
    private static final int IS_TODAY = 0;
    private static final String LINKTURBO_UID_TO_SLA = "update_uidlist_to_sla";
    public static final String LINK_TURBO_MODE = "link_turbo_mode";
    private static final String LINK_TURBO_OPTION = "link_turbo_option";
    private static final int MSG_DB_SLAAPP_ADD = 100;
    private static final int MSG_DB_SLAAPP_DEL = 101;
    private static final int MSG_DB_SLAAPP_UPDATE = 102;
    private static final int MSG_DELAY_TIME = 300;
    private static final int MSG_SET_DDAPP_LIST = 105;
    private static final int MSG_SET_DWAPP_LIST = 103;
    private static final int MSG_SET_SLAAPP_LIST = 104;
    public static final int REMOVE_FROM_WHITELIST = 2;
    public static final int RESET_WHITELIST = 3;
    public static final int RESTORE_RESET_WHITELIST = 5;
    private static final String SLA_MODE_CONCURRENT = "0";
    private static final String SLA_MODE_SLAVE = "1";
    private static final int START_CALC_TRAFFIC_STAT = 201;
    private static final int STOP_CALC_TRAFFIC_STAT = 203;
    static final String TAG = "SLM-SRV-SLAAppLib";
    public static final int TYPE_MOBILE = 0;
    private static final int UPDATE_CALC_TRAFFIC_STAT = 202;
    private static final int UPDATE_DB_TRAFFIC_STAT = 204;
    private static boolean defaultSlaEnable = false;
    private static boolean mIsSladRunning = false;
    private static boolean mIsUidChangeReboot = false;
    private static NetworkStatsManager mNetworkStatsManager = null;
    private static final String mSLSVoIPAppPN = "com.tencent.mm";
    private static ISlaService mSlaService;
    private static SLAAppLib sSLAAppLib;
    private Context mContext;
    private Handler mDataBaseHandler;
    private HandlerThread mDataBaseThread;
    private SQLiteDatabase mDatabase;
    private int mDay;
    private Set<String> mDoubleDataAppPN;
    private Set<String> mDoubleWifiAppPN;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int mMonth;
    private Set<String> mSDKDoubleWifiAppPN;
    private Set<String> mSDKDoubleWifiBlackUidSets;
    private Set<String> mSLAAppDefaultPN;
    private Set<String> mSLSGameAppPN;
    private SlaBaseHelper slaBaseHelper;
    private static ArrayList<SLAApp> mAppLists = new ArrayList<>();
    private static HashMap<String, Long> mTraffic = new HashMap<>();
    private static int mLinkTurboMode = -2;
    private static final Object mSLAAppLibLock = new Object();
    private static HashMap<String, Long> mSLAAppUpgradeList = new HashMap<>();
    private int mCalcTrafficInterval = AnimTask.MAX_SINGLE_TASK_SIZE;
    private int mCount = 0;
    private final Semaphore mSemaphore = new Semaphore(1);
    private String mDoubleWifiUidList = null;
    private String mSDKDoubleWifiUidList = null;
    private String mDoubleDataUidList = null;
    private String mSLSGameUidList = null;
    private int mSLSVoIPUid = 0;

    public static SLAAppLib get(Context context) {
        if (sSLAAppLib == null) {
            sSLAAppLib = new SLAAppLib(context);
        }
        return sSLAAppLib;
    }

    private SLAAppLib(Context context) {
        this.mDay = -1;
        this.mMonth = 1;
        this.mContext = context.getApplicationContext();
        SlaBaseHelper slaBaseHelper = new SlaBaseHelper(this.mContext);
        this.slaBaseHelper = slaBaseHelper;
        this.mDatabase = slaBaseHelper.getWritableDatabase();
        this.mSLSGameAppPN = new HashSet();
        this.mSLAAppDefaultPN = new HashSet();
        this.mDoubleWifiAppPN = new HashSet();
        this.mDoubleDataAppPN = new HashSet();
        mIsUidChangeReboot = false;
        syncSLAAppFromDB();
        initSLAUIObserver();
        initBroadcastReceiver();
        if (1 == Settings.System.getInt(this.mContext.getContentResolver(), LINK_TURBO_MODE, 0)) {
            mLinkTurboMode = 1;
        } else {
            mLinkTurboMode = 0;
        }
        if (mAppLists.size() > 0) {
            for (int i = 0; i < mAppLists.size(); i++) {
                mTraffic.put(mAppLists.get(i).getUid(), 0L);
            }
        }
        if (mAppLists.size() > 0) {
            this.mDay = mAppLists.get(0).getDay();
            this.mMonth = mAppLists.get(0).getDay();
        }
        mNetworkStatsManager = (NetworkStatsManager) this.mContext.getSystemService("netstats");
        HandlerThread handlerThread = new HandlerThread("SlaUidHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SLAAppLib.START_CALC_TRAFFIC_STAT /* 201 */:
                        SLAAppLib.this.calcAppsTrafficStat();
                        return;
                    case SLAAppLib.UPDATE_CALC_TRAFFIC_STAT /* 202 */:
                    default:
                        return;
                    case SLAAppLib.STOP_CALC_TRAFFIC_STAT /* 203 */:
                        SLAAppLib.this.restoreAppsTrafficStat();
                        return;
                    case SLAAppLib.UPDATE_DB_TRAFFIC_STAT /* 204 */:
                        SLAAppLib.this.restoreAppsTrafficStat();
                        return;
                }
            }
        };
        HandlerThread handlerThread2 = new HandlerThread("DatabaseHandler");
        this.mDataBaseThread = handlerThread2;
        handlerThread2.start();
        this.mDataBaseHandler = new Handler(this.mDataBaseThread.getLooper()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                SLAApp app = (SLAApp) msg.obj;
                switch (msg.what) {
                    case 100:
                        SLAAppLib.this.dbAddSLAApp(app);
                        break;
                    case 101:
                        SLAAppLib.this.dbUpdateSLAApp(app);
                        break;
                    case 102:
                        SLAAppLib.this.dbUpdateSLAApp(app);
                        break;
                    case 103:
                        SLAAppLib.this.setDoubleWifiUidToSlad();
                        break;
                    case 105:
                        SLAAppLib.this.setDoubleDataUidToSlad();
                        break;
                }
                SLAAppLib.mIsUidChangeReboot = true;
                SLAAppLib.this.setUidWhiteListToSlad();
                SLAToast.setLinkTurboUidList(SLAAppLib.this.getLinkTurboWhiteList());
                SLSAppLib.setSLAAppWhiteList(SLAAppLib.this.getLinkTurboWhiteList());
                SLAAppLib.mIsUidChangeReboot = false;
            }
        };
        initSLMCloudObserver();
        initSLACloudObserver();
        initSLAAppDefault();
        initDoubleDataCloudObserver();
    }

    public void setSlaService(ISlaService service) {
        mSlaService = service;
    }

    private void sendMsgStartCalc() {
        Log.d(TAG, "sendMsgStartCalc");
        Message msg = Message.obtain();
        msg.what = START_CALC_TRAFFIC_STAT;
        this.mHandler.sendMessage(msg);
    }

    private void sendMsgStopCalc() {
        Log.d(TAG, "sendMsgStopCalc");
        Message msg = Message.obtain();
        msg.what = STOP_CALC_TRAFFIC_STAT;
        this.mHandler.sendMessage(msg);
    }

    private void postCalcAppsTrafficStat(long delay) {
        if (this.mCount == 15) {
            restoreAppsTrafficStat();
            this.mCount = 0;
        }
        this.mCount++;
        this.mHandler.removeMessages(START_CALC_TRAFFIC_STAT);
        this.mHandler.sendEmptyMessageDelayed(START_CALC_TRAFFIC_STAT, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void calcAppsTrafficStat() {
        int change = isDateChanged();
        List<SLAApp> tmpLists = cloneSLAApp();
        int i = 1;
        if (tmpLists.size() < 1) {
            postCalcAppsTrafficStat(this.mCalcTrafficInterval);
            return;
        }
        int i2 = 0;
        while (i2 < tmpLists.size()) {
            long curtraffic = 0;
            SLAApp app = tmpLists.get(i2);
            int uid = Integer.parseInt(app.getUid());
            boolean state = app.getState();
            if (!state) {
                if (change == 0) {
                    i2++;
                    i = 1;
                } else if (change == i) {
                    Log.d(TAG, "IS_DAY_CHANGE state = " + state);
                    app.setDayTraffic(0L);
                    app.setDay(this.mDay);
                }
            }
            long traffic = getDataConsumedForUid(uid);
            Long lCurTrafficLong = mTraffic.get(app.getUid());
            if (lCurTrafficLong != null) {
                curtraffic = lCurTrafficLong.longValue();
            }
            if (curtraffic == 0) {
                curtraffic = traffic;
            }
            if (curtraffic > traffic) {
                traffic = curtraffic;
            }
            if (change == 0) {
                app.setDayTraffic(app.getDayTraffic() + (traffic - curtraffic));
                app.setMonthTraffic(app.getMonthTraffic() + (traffic - curtraffic));
            } else if (change == 2) {
                Log.d(TAG, "IS_MONTH_CHANGE");
                app.setDayTraffic(0L);
                app.setMonthTraffic(0L);
                app.setDay(this.mDay);
                app.setMonth(this.mMonth);
            } else {
                Log.d(TAG, "IS_DAY_CHANGE");
                app.setDayTraffic(0L);
                app.setMonthTraffic(app.getMonthTraffic() + (traffic - curtraffic));
                app.setDay(this.mDay);
            }
            long curtraffic2 = traffic;
            mTraffic.put(app.getUid(), Long.valueOf(curtraffic2));
            app.mCurTraffic = curtraffic2;
            i2++;
            i = 1;
        }
        if (change == 2) {
            UpdateSLAAppList(cloneSLAApp(), true);
        }
        if (mIsSladRunning) {
            postCalcAppsTrafficStat(this.mCalcTrafficInterval);
        }
    }

    private static void printTraffic(SLAApp app) {
        Log.d(TAG, "+++++++++++++++++++app uid: " + app.getUid());
        Log.d(TAG, "app day traffic: " + FormatBytesUtil.formatBytes(app.getDayTraffic()));
        Log.d(TAG, "app month traffic: " + FormatBytesUtil.formatBytes(app.getMonthTraffic()));
        Log.d(TAG, "current traffic: " + FormatBytesUtil.formatBytes(app.mCurTraffic));
        Log.d(TAG, "app state: " + app.getState());
        Log.d(TAG, "app day: " + app.getDay());
        Log.d(TAG, "app month: " + app.getMonth());
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x0038, code lost:
    
        if (r0 != null) goto L14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x003a, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x004b, code lost:
    
        return r10 + r12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0046, code lost:
    
        if (r0 == null) goto L22;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static long getDataConsumedForUid(int r14) {
        /*
            android.app.usage.NetworkStatsManager r0 = com.xiaomi.NetworkBoost.slaservice.SLAAppLib.mNetworkStatsManager
            if (r0 == 0) goto L4c
            r0 = 0
            long r8 = java.lang.System.currentTimeMillis()
            r10 = 0
            r12 = 0
            android.app.usage.NetworkStatsManager r1 = com.xiaomi.NetworkBoost.slaservice.SLAAppLib.mNetworkStatsManager     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            r2 = 0
            r3 = 0
            r4 = 0
            r6 = r8
            android.app.usage.NetworkStats r1 = r1.querySummary(r2, r3, r4, r6)     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            r0 = r1
            android.app.usage.NetworkStats$Bucket r1 = new android.app.usage.NetworkStats$Bucket     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            r1.<init>()     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
        L1e:
            boolean r2 = r0.hasNextBucket()     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            if (r2 == 0) goto L38
            r0.getNextBucket(r1)     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            int r2 = r1.getUid()     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            if (r2 != r14) goto L37
            long r3 = r1.getTxBytes()     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            long r10 = r10 + r3
            long r3 = r1.getRxBytes()     // Catch: java.lang.Throwable -> L3e java.lang.Exception -> L45
            long r12 = r12 + r3
        L37:
            goto L1e
        L38:
            if (r0 == 0) goto L49
        L3a:
            r0.close()
            goto L49
        L3e:
            r1 = move-exception
            if (r0 == 0) goto L44
            r0.close()
        L44:
            throw r1
        L45:
            r1 = move-exception
            if (r0 == 0) goto L49
            goto L3a
        L49:
            long r1 = r10 + r12
            return r1
        L4c:
            r0 = 0
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.getDataConsumedForUid(int):long");
    }

    private int isDateChanged() {
        List<SLAApp> tmpLists = cloneSLAApp();
        synchronized (mSLAAppLibLock) {
            if (tmpLists.size() < 1) {
                return 0;
            }
            SLAApp app = tmpLists.get(0);
            int day = app.getDay();
            int month = app.getMonth();
            Calendar c = Calendar.getInstance();
            int localmonth = c.get(2) + 1;
            int localday = c.get(5);
            if (month != localmonth && month > 0) {
                this.mDay = localday;
                this.mMonth = localmonth;
                return 2;
            }
            if (day == localday || day <= 0) {
                return 0;
            }
            this.mDay = localday;
            return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreAppsTrafficStat() {
        List<SLAApp> tmpLists;
        synchronized (mSLAAppLibLock) {
            tmpLists = cloneSLAApp();
        }
        for (int i = 0; i < tmpLists.size(); i++) {
            dbUpdateSLAApp(tmpLists.get(i));
        }
    }

    public String getLinkTurboWhiteList() {
        String uidlist = "";
        List<SLAApp> tmpLists = cloneSLAApp();
        if (tmpLists.size() == 0) {
            return null;
        }
        List<String> uids = new ArrayList<>();
        for (int i = 0; i < tmpLists.size(); i++) {
            if (tmpLists.get(i).getState()) {
                uids.add(tmpLists.get(i).getUid());
            }
        }
        if (uids.size() == 0) {
            return null;
        }
        for (int i2 = 0; i2 < uids.size(); i2++) {
            uidlist = uidlist + uids.get(i2) + ",";
        }
        Log.d(TAG, "getLinkTurboWhiteList:" + uidlist);
        return uidlist;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getLinkTurboDefaultPn() {
        List<String> defaultPn = new ArrayList<>();
        if (!this.mSLAAppDefaultPN.isEmpty()) {
            defaultPn.addAll(this.mSLAAppDefaultPN);
        }
        if (!this.mSLSGameAppPN.isEmpty()) {
            defaultPn.addAll(this.mSLSGameAppPN);
        }
        if (defaultPn.isEmpty()) {
            Log.e(TAG, "LinkTurboDefaultPn is empty");
        }
        return defaultPn;
    }

    public List<SLAApp> getLinkTurboAppsTraffic() {
        List<SLAApp> tmpLists = cloneSLAApp();
        if (tmpLists.size() == 0) {
            return null;
        }
        List<SLAApp> apps = new ArrayList<>();
        for (int i = 0; i < tmpLists.size(); i++) {
            apps.add(tmpLists.get(i));
        }
        sendMsgUpdateTrafficStat();
        return apps;
    }

    public void startSladHandle() {
        mIsSladRunning = true;
        sendMsgStartCalc();
        setUidWhiteListToSlad();
        setSLAMode();
    }

    public void stopSladHandle() {
        mIsSladRunning = false;
        sendMsgStopCalc();
    }

    public boolean sendMsgAddSLAUid(String uid) {
        SLAApp app;
        Message msg = Message.obtain();
        int ret = addSLAApp(uid);
        if (ret == 1) {
            msg.what = 100;
            Log.d(TAG, "MSG_DB_SLAAPP_ADD:" + uid);
        } else if (ret == 0) {
            msg.what = 102;
            Log.d(TAG, "MSG_DB_SLAAPP_UPDATE:" + uid);
        }
        synchronized (mSLAAppLibLock) {
            app = getSLAAppByUid(uid);
        }
        msg.obj = app;
        this.mDataBaseHandler.sendMessage(msg);
        Settings.System.putString(this.mContext.getContentResolver(), LINKTURBO_UID_TO_SLA, getLinkTurboWhiteList());
        return true;
    }

    public boolean sendMsgDelSLAUid(String uid) {
        SLAApp app;
        Message msg = Message.obtain();
        int ret = deleteSLAApp(uid);
        if (ret == 1) {
            msg.what = 101;
            Log.d(TAG, "MSG_DB_SLAAPP_DEL:" + uid);
            synchronized (mSLAAppLibLock) {
                app = getSLAAppByUid(uid);
            }
            msg.obj = app;
            this.mDataBaseHandler.sendMessage(msg);
            Settings.System.putString(this.mContext.getContentResolver(), LINKTURBO_UID_TO_SLA, getLinkTurboWhiteList());
            return true;
        }
        return false;
    }

    public boolean sendMsgDoubleWifiUid() {
        Message msg = Message.obtain();
        msg.what = 103;
        Log.d(TAG, "sendMsgDoubleWifiUid");
        this.mDataBaseHandler.sendMessageAtTime(msg, 300L);
        return true;
    }

    public boolean sendMsgDoubleDataUid() {
        Message msg = Message.obtain();
        msg.what = 105;
        Log.d(TAG, "sendMsgDoubleDataUid");
        this.mDataBaseHandler.sendMessageAtTime(msg, 300L);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean sendMsgSetSLAAppList() {
        Message msg = Message.obtain();
        msg.what = 104;
        Log.d(TAG, "sendMsgSetSLAAppList");
        this.mDataBaseHandler.sendMessage(msg);
        return true;
    }

    private List<SLAApp> cloneSLAApp() {
        List<SLAApp> tmpLists = new ArrayList<>();
        synchronized (mSLAAppLibLock) {
            tmpLists.addAll(mAppLists);
        }
        return tmpLists;
    }

    private void UpdateSLAAppList(List<SLAApp> src, boolean change) {
        if (src == null) {
            return;
        }
        synchronized (mSLAAppLibLock) {
            for (int i = 0; i < src.size(); i++) {
                SLAApp tmpapp = src.get(i);
                SLAApp app = getSLAAppByUid(tmpapp.getUid());
                if (change && !src.get(i).getState()) {
                    mAppLists.remove(app);
                    dbDeleteSLAApp(app);
                } else if (app != null) {
                    app.setDay(this.mDay);
                    app.setMonth(this.mMonth);
                    app.setDayTraffic(0L);
                    app.setMonthTraffic(0L);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SLAApp getSLAAppByUid(String uid) {
        for (int i = 0; i < mAppLists.size(); i++) {
            if (mAppLists.get(i).getUid().equals(uid)) {
                return mAppLists.get(i);
            }
        }
        return null;
    }

    private void printUidLists() {
        String activeuidlist = "";
        String deactiveuidlist = "";
        List<SLAApp> tmpLists = cloneSLAApp();
        if (tmpLists.size() < 1) {
            return;
        }
        for (int i = 0; i < tmpLists.size(); i++) {
            if (tmpLists.get(i).getState()) {
                activeuidlist = activeuidlist + tmpLists.get(i).getUid() + ",";
            } else {
                deactiveuidlist = deactiveuidlist + tmpLists.get(i).getUid() + ",";
            }
        }
        Log.d(TAG, "activeuidlist:" + activeuidlist);
        Log.d(TAG, "deactiveuidlist:" + deactiveuidlist);
    }

    public boolean setDoubleWifiWhiteList(int type, String packageName, String uidList, boolean isOperateBlacklist) {
        boolean isInBlackSet;
        Log.d(TAG, "setDoubleWifiWhiteList type = " + type + " packageName = " + packageName + " uidList = " + uidList);
        Log.d(TAG, "pre setDoubleWifiWhiteList mSDKDoubleWifiUidList = " + this.mSDKDoubleWifiUidList + " mSDKDoubleWifiAppPN = " + this.mSDKDoubleWifiAppPN + " mSDKDoubleWifiBlackUidSets = " + this.mSDKDoubleWifiBlackUidSets);
        if (packageName == null || uidList == null) {
            return false;
        }
        synchronized (mSLAAppLibLock) {
            if (this.mSDKDoubleWifiUidList == null) {
                this.mSDKDoubleWifiUidList = "";
            }
            if (this.mSDKDoubleWifiAppPN == null) {
                this.mSDKDoubleWifiAppPN = new HashSet();
            }
            List<String> whiteUidLists = (List) Arrays.stream(this.mSDKDoubleWifiUidList.split(",")).map(new SLAAppLib$$ExternalSyntheticLambda0()).collect(Collectors.toCollection(new AudioServiceStubImpl$$ExternalSyntheticLambda3()));
            switch (type) {
                case 1:
                    addOrRemoveWhiteList(whiteUidLists, false, packageName, uidList, isOperateBlacklist);
                    break;
                case 2:
                    addOrRemoveWhiteList(whiteUidLists, true, packageName, uidList, isOperateBlacklist);
                    break;
                case 3:
                    Set<String> packagesSet = (Set) Arrays.stream(packageName.split(",")).map(new SLAAppLib$$ExternalSyntheticLambda0()).collect(Collectors.toCollection(new Supplier() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib$$ExternalSyntheticLambda1
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            return new HashSet();
                        }
                    }));
                    this.mSDKDoubleWifiAppPN = packagesSet;
                    this.mSDKDoubleWifiUidList = uidList;
                    break;
                case 4:
                    String[] uids = uidList.split(",");
                    for (String str : uids) {
                        Set<String> set = this.mSDKDoubleWifiBlackUidSets;
                        if (set == null || !set.contains(str)) {
                            isInBlackSet = false;
                        } else {
                            isInBlackSet = true;
                        }
                        if (isInBlackSet) {
                            return false;
                        }
                    }
                    return true;
                case 5:
                    this.mSDKDoubleWifiUidList = null;
                    this.mSDKDoubleWifiAppPN = null;
                    break;
            }
            Log.d(TAG, "after setDoubleWifiWhiteList mSDKDoubleWifiUidList = " + this.mSDKDoubleWifiUidList + " mSDKDoubleWifiAppPN = " + this.mSDKDoubleWifiAppPN + " mSDKDoubleWifiBlackUidSets = " + this.mSDKDoubleWifiBlackUidSets);
            return true;
        }
    }

    private void addOrRemoveWhiteList(List<String> whiteUidLists, boolean remove, String packageName, String uidList, boolean isOperateBlacklist) {
        synchronized (mSLAAppLibLock) {
            if (whiteUidLists != null) {
                String[] packages = packageName.split(",");
                if (this.mSDKDoubleWifiBlackUidSets == null) {
                    this.mSDKDoubleWifiBlackUidSets = new HashSet();
                }
                for (int i = 0; i < packages.length; i++) {
                    if (remove && this.mSDKDoubleWifiAppPN.contains(packages[i])) {
                        if (!isOperateBlacklist) {
                            this.mSDKDoubleWifiAppPN.remove(packages[i]);
                        }
                    } else if (!this.mSDKDoubleWifiAppPN.contains(packages[i]) && !isOperateBlacklist) {
                        this.mSDKDoubleWifiAppPN.add(packages[i]);
                    }
                }
                String[] uids = uidList.split(",");
                for (int i2 = 0; i2 < uids.length; i2++) {
                    if (remove) {
                        if (isOperateBlacklist && !this.mSDKDoubleWifiBlackUidSets.contains(uids[i2])) {
                            this.mSDKDoubleWifiBlackUidSets.add(uids[i2]);
                        }
                        if (!isOperateBlacklist && whiteUidLists.contains(uids[i2])) {
                            whiteUidLists.remove(uids[i2]);
                        }
                    } else {
                        if (isOperateBlacklist && this.mSDKDoubleWifiBlackUidSets.contains(uids[i2])) {
                            this.mSDKDoubleWifiBlackUidSets.remove(uids[i2]);
                        }
                        if (!isOperateBlacklist && !whiteUidLists.contains(uids[i2])) {
                            whiteUidLists.add(uids[i2]);
                        }
                    }
                }
                int i3 = whiteUidLists.size();
                if (i3 > 0) {
                    String curWhites = String.join(",", whiteUidLists);
                    this.mSDKDoubleWifiUidList = curWhites;
                } else {
                    this.mSDKDoubleWifiUidList = null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void setSLAMode() {
        Log.i(TAG, "setSLAMode");
        if (mSlaService != null) {
            try {
                if (mLinkTurboMode == 0) {
                    Log.i(TAG, "setSLAMode:1");
                    mSlaService.setSLAMode("1");
                } else {
                    Log.i(TAG, "setSLAMode:0");
                    mSlaService.setSLAMode(SLA_MODE_CONCURRENT);
                }
                return;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return;
            }
        }
        Log.d(TAG, "mSlaService is null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUidWhiteListToSlad() {
        Log.i(TAG, "setUidWhiteListToSlad");
        String uidlist = getLinkTurboWhiteList();
        ISlaService iSlaService = mSlaService;
        if (iSlaService != null) {
            try {
                if (uidlist != null) {
                    PackageManager pm = this.mContext.getPackageManager();
                    int downloadui_uid = pm.getApplicationInfo("com.android.providers.downloads.ui", 0).uid;
                    int download_uid = pm.getApplicationInfo("com.android.providers.downloads", 0).uid;
                    if (downloadui_uid != 0 && download_uid != 0 && uidlist.indexOf(Integer.toString(download_uid)) == -1 && uidlist.indexOf(Integer.toString(downloadui_uid)) != -1) {
                        uidlist = uidlist + Integer.toString(download_uid) + ",";
                    } else if (downloadui_uid != 0 && download_uid != 0 && uidlist.indexOf(Integer.toString(download_uid)) != -1 && uidlist.indexOf(Integer.toString(downloadui_uid)) == -1) {
                        uidlist = uidlist.replaceFirst(Integer.toString(download_uid) + ",", "");
                    }
                    if (uidlist.length() == 0) {
                        mSlaService.setSLAUidList("NULL");
                        return;
                    } else {
                        Log.i(TAG, "setSLAUidList:" + uidlist);
                        mSlaService.setSLAUidList(uidlist);
                        return;
                    }
                }
                iSlaService.setSLAUidList("NULL");
                return;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return;
            }
        }
        Log.d(TAG, "mSlaService is null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDoubleWifiUidToSlad() {
        List<String> uidLists;
        Log.i(TAG, "setDoubleWifiUidToSlad");
        if (mSlaService != null) {
            try {
                boolean isSDKOpended = Settings.System.getInt(this.mContext.getContentResolver(), NetworkSDKService.IS_OPENED_DUAL_WIFI, 0) == 1;
                if (isSDKOpended) {
                    Log.i(TAG, "setDWUidList isSDKOpend = true DoubleWifiUidList = " + this.mSDKDoubleWifiUidList + " mSDKDoubleWifiBlackUidSets = " + this.mSDKDoubleWifiBlackUidSets);
                    if (!TextUtils.isEmpty(this.mSDKDoubleWifiUidList)) {
                        List<String> uidLists2 = (List) Arrays.stream(this.mSDKDoubleWifiUidList.split(",")).map(new SLAAppLib$$ExternalSyntheticLambda0()).collect(Collectors.toCollection(new AudioServiceStubImpl$$ExternalSyntheticLambda3()));
                        String finalUids = generateFinalWhiteList(uidLists2);
                        Log.i(TAG, "setDWUidList isSDKOpend= true final DoubleWifiUidList = " + finalUids);
                        if (!TextUtils.isEmpty(finalUids)) {
                            mSlaService.setDWUidList(finalUids);
                        } else {
                            mSlaService.setDWUidList("NULL");
                        }
                        return;
                    }
                    mSlaService.setDWUidList("NULL");
                    return;
                }
                Log.i(TAG, "setDWUidList isSDKOpend= false mDoubleWifiUidList = " + this.mDoubleWifiUidList + " mSDKDoubleWifiUidList = " + this.mSDKDoubleWifiUidList + " mSDKDoubleWifiBlackUidSets = " + this.mSDKDoubleWifiBlackUidSets);
                if (!TextUtils.isEmpty(this.mDoubleWifiUidList)) {
                    uidLists = (List) Arrays.stream(this.mDoubleWifiUidList.split(",")).map(new SLAAppLib$$ExternalSyntheticLambda0()).collect(Collectors.toCollection(new AudioServiceStubImpl$$ExternalSyntheticLambda3()));
                } else {
                    uidLists = new ArrayList();
                }
                String str = this.mSDKDoubleWifiUidList;
                if (str != null) {
                    String[] sdkUids = str.split(",");
                    for (String sdkUid : sdkUids) {
                        if (!uidLists.contains(sdkUid)) {
                            uidLists.add(sdkUid);
                        }
                    }
                }
                String finalUids2 = generateFinalWhiteList(uidLists);
                Log.i(TAG, "setDWUidList isSDKOpend= false final DoubleWifiUidList = " + finalUids2);
                if (!TextUtils.isEmpty(finalUids2)) {
                    mSlaService.setDWUidList(finalUids2);
                    return;
                } else {
                    mSlaService.setDWUidList("NULL");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return;
            }
        }
        Log.d(TAG, "mSlaService is null");
    }

    private String generateFinalWhiteList(List<String> uidLists) {
        if (uidLists == null) {
            return "";
        }
        synchronized (mSLAAppLibLock) {
            Set<String> set = this.mSDKDoubleWifiBlackUidSets;
            if (set != null && set.size() > 0) {
                for (String blackUid : this.mSDKDoubleWifiBlackUidSets) {
                    if (uidLists.contains(blackUid)) {
                        uidLists.remove(blackUid);
                    }
                }
            }
        }
        String finalUids = String.join(",", uidLists);
        return finalUids;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDoubleDataUidToSlad() {
        Log.i(TAG, "setDoubleDataUidToSlad");
        if (mSlaService != null) {
            try {
                Log.i(TAG, "setDDUidList:" + this.mDoubleDataUidList);
                String str = this.mDoubleDataUidList;
                if (str != null) {
                    mSlaService.setDDUidList(str);
                } else {
                    mSlaService.setDDUidList("NULL");
                }
                return;
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
                return;
            }
        }
        Log.d(TAG, "mSlaService is null");
    }

    private int dbAcquirePermit(String action) {
        Log.d(TAG, "action:" + action + " acquire a permit");
        try {
            this.mSemaphore.acquire();
            Log.d(TAG, "action:" + action + " acquire a permit success");
            return 0;
        } catch (InterruptedException e) {
            Log.e(TAG, "an InterruptedException hanppened! action:" + action);
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private void dbReleasePermit(String action) {
        this.mSemaphore.drainPermits();
        this.mSemaphore.release();
        Log.d(TAG, "action:" + action + " releases a permit");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dbAddSLAApp(SLAApp app) {
        if (app == null) {
            return;
        }
        Log.d(TAG, "dbAddSLAApp uid = " + app.getUid());
        ContentValues values = getContentValues(app);
        dbAcquirePermit("add");
        this.mDatabase.insert(SlaDbSchema.SlaTable.NAME, null, values);
        dbReleasePermit("add");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dbUpdateSLAApp(SLAApp app) {
        if (app == null) {
            return;
        }
        Log.d(TAG, "dbUpdateSLAApp uid = " + app.getUid());
        ContentValues values = getContentValues(app);
        dbAcquirePermit("update");
        this.mDatabase.update(SlaDbSchema.SlaTable.NAME, values, "uid = ?", new String[]{app.getUid()});
        dbReleasePermit("update");
    }

    private void dbDeleteSLAApp(SLAApp app) {
        if (app == null) {
            return;
        }
        Log.d(TAG, "dbDeleteSLAApp uid = " + app.getUid());
        getContentValues(app);
        dbAcquirePermit("del");
        this.mDatabase.delete(SlaDbSchema.SlaTable.NAME, "uid = ?", new String[]{app.getUid()});
        dbReleasePermit("del");
    }

    private SlaCursorWrapper dbGetCursorSLAApp(String whereClaue, String[] whereArgs) {
        Cursor cursor = this.mDatabase.query(SlaDbSchema.SlaTable.NAME, null, whereClaue, whereArgs, null, null, null);
        return new SlaCursorWrapper(cursor);
    }

    private void syncSLAAppFromDB() {
        List<SLAApp> tmpLists = new ArrayList<>();
        SlaCursorWrapper cursor = dbGetCursorSLAApp(null, null);
        try {
            tmpLists.clear();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                tmpLists.add(cursor.getSLAApp());
                cursor.moveToNext();
            }
            synchronized (mSLAAppLibLock) {
                mAppLists.clear();
                mAppLists.addAll(tmpLists);
            }
            printUidLists();
            cursor.close();
        } catch (Throwable th) {
            synchronized (mSLAAppLibLock) {
                mAppLists.clear();
                mAppLists.addAll(tmpLists);
                printUidLists();
                cursor.close();
                throw th;
            }
        }
    }

    private static ContentValues getContentValues(SLAApp sLAApp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SlaDbSchema.SlaTable.Uidlist.UID, sLAApp.getUid());
        contentValues.put(SlaDbSchema.SlaTable.Uidlist.DAYTRAFFIC, Long.valueOf(sLAApp.getDayTraffic()));
        contentValues.put(SlaDbSchema.SlaTable.Uidlist.MONTHTRAFFIC, Long.valueOf(sLAApp.getMonthTraffic()));
        contentValues.put("state", Integer.valueOf(sLAApp.getState() ? 1 : 0));
        contentValues.put(SlaDbSchema.SlaTable.Uidlist.DAY, Integer.valueOf(sLAApp.getDay()));
        contentValues.put(SlaDbSchema.SlaTable.Uidlist.MONTH, Integer.valueOf(sLAApp.getMonth()));
        return contentValues;
    }

    private void initSLACloudObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                try {
                    PackageManager pm = SLAAppLib.this.mContext.getPackageManager();
                    List<PackageInfo> apps = pm.getInstalledPackages(1);
                    Build.DEVICE.toLowerCase();
                    SLAAppLib.this.mSLAAppDefaultPN.clear();
                    if (SLAAppLib.mAppLists != null) {
                        synchronized (SLAAppLib.mSLAAppLibLock) {
                            for (int i = 0; i < SLAAppLib.mAppLists.size(); i++) {
                                ((SLAApp) SLAAppLib.mAppLists.get(i)).setState(false);
                                SLAAppLib.this.dbUpdateSLAApp((SLAApp) SLAAppLib.mAppLists.get(i));
                            }
                        }
                    }
                    SLAAppLib.this.initSLAAppDefault();
                    for (PackageInfo app : apps) {
                        if (app.packageName != null && app.applicationInfo != null && SLAAppLib.this.mSLAAppDefaultPN.contains(app.packageName)) {
                            int uid = app.applicationInfo.uid;
                            SLAAppLib.this.addSLAAppDefault(uid);
                        }
                    }
                } catch (Exception e) {
                    Log.e(SLAAppLib.TAG, "initSLACloudObserver onChange error " + e);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_SLA_WHITELIST), false, observer);
    }

    private void initSLMCloudObserver() {
        final ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                String[] packages;
                PackageManager pm;
                String[] packages2;
                String[] packages3;
                String[] packages4;
                try {
                    PackageManager pm2 = SLAAppLib.this.mContext.getPackageManager();
                    List<PackageInfo> apps = pm2.getInstalledPackages(1);
                    Build.DEVICE.toLowerCase();
                    SLAAppLib.this.mSLSGameAppPN.clear();
                    SLAAppLib.this.mDoubleWifiAppPN.clear();
                    SLAAppLib.this.mSLSGameUidList = null;
                    SLAAppLib.this.mDoubleWifiUidList = null;
                    String whiteString_sls = Settings.System.getString(SLAAppLib.this.mContext.getContentResolver(), SLAAppLib.CLOUD_SLS_GAMING_WHITELIST);
                    Log.d(SLAAppLib.TAG, "Cloud SLSApp: " + whiteString_sls);
                    if (!TextUtils.isEmpty(whiteString_sls) && (packages4 = whiteString_sls.split(",")) != null) {
                        for (int i = 0; i < packages4.length; i++) {
                            if (!packages4[i].startsWith("miwill")) {
                                SLAAppLib.this.mSLSGameAppPN.add(packages4[i]);
                            }
                        }
                    }
                    String whiteString_doubleWifi = Settings.System.getString(SLAAppLib.this.mContext.getContentResolver(), SLAAppLib.CLOUD_DOUBLEWIFI_WHITELIST);
                    Log.d(SLAAppLib.TAG, "Cloud DWApp: " + whiteString_doubleWifi);
                    if (!TextUtils.isEmpty(whiteString_doubleWifi) && (packages3 = whiteString_doubleWifi.split(",")) != null) {
                        for (String str : packages3) {
                            SLAAppLib.this.mDoubleWifiAppPN.add(str);
                        }
                    }
                    if (SLAAppLib.this.mSLSGameAppPN.isEmpty()) {
                        if ("CN".equalsIgnoreCase(SystemProperties.get("ro.boot.hwc"))) {
                            packages2 = new String[]{"com.tencent.tmgp.pubgmhd", "com.tencent.tmgp.sgame", "com.tencent.tmgp.speedmobile", SLAAppLib.mSLSVoIPAppPN};
                        } else {
                            packages2 = new String[]{"com.test"};
                        }
                        for (String str2 : packages2) {
                            SLAAppLib.this.mSLSGameAppPN.add(str2);
                        }
                    }
                    Log.d(SLAAppLib.TAG, "set SLSApp: " + SLAAppLib.this.mSLSGameAppPN.toString());
                    for (PackageInfo app : apps) {
                        if (app.packageName == null || app.applicationInfo == null) {
                            pm = pm2;
                        } else if (!SLAAppLib.this.mSLSGameAppPN.contains(app.packageName)) {
                            pm = pm2;
                        } else {
                            int uid = app.applicationInfo.uid;
                            SLAAppLib.this.addSLAAppDefault(uid);
                            if (SLAAppLib.this.mSLSGameUidList == null) {
                                pm = pm2;
                                SLAAppLib.this.mSLSGameUidList = Integer.toString(uid) + ",";
                            } else {
                                pm = pm2;
                                SLAAppLib.this.mSLSGameUidList += Integer.toString(uid) + ",";
                            }
                        }
                        if (app.packageName != null && app.packageName.equals(SLAAppLib.mSLSVoIPAppPN)) {
                            if (SLAAppLib.this.mSLSGameAppPN.contains(SLAAppLib.mSLSVoIPAppPN)) {
                                SLAAppLib.this.mSLSVoIPUid = app.applicationInfo.uid;
                            } else {
                                SLAAppLib.this.mSLSVoIPUid = 0;
                            }
                        }
                        pm2 = pm;
                    }
                    if (SLAAppLib.this.mSLSVoIPUid != 0) {
                        SLAAppLib sLAAppLib = SLAAppLib.this;
                        sLAAppLib.mSLSGameUidList = sLAAppLib.mSLSGameUidList.replace(Integer.toString(SLAAppLib.this.mSLSVoIPUid), "");
                        SLAAppLib sLAAppLib2 = SLAAppLib.this;
                        sLAAppLib2.mSLSGameUidList = sLAAppLib2.mSLSGameUidList.replace(",,", ",");
                    }
                    SLSAppLib.setSLSGameUidList(SLAAppLib.this.mSLSGameUidList);
                    SLSAppLib.setSLSVoIPUid(SLAAppLib.this.mSLSVoIPUid);
                    NetworkAccelerateSwitchService.setNetworkAccelerateSwitchUidList(SLAAppLib.this.mSLSGameUidList);
                    NetworkAccelerateSwitchService.setNetworkAccelerateSwitchVoIPUid(SLAAppLib.this.mSLSVoIPUid);
                    if (SLAAppLib.this.mDoubleWifiAppPN.isEmpty()) {
                        if ("CN".equalsIgnoreCase(SystemProperties.get("ro.boot.hwc"))) {
                            packages = new String[]{"air.tv.douyu.android", "com.duowan.kiwi", "com.ss.android.ugc.aweme", "com.taobao.taobao", "com.tmall.wireless", "com.jingdong.app.mall", "com.youku.phone", "com.qiyi.video", "com.android.providers.downloads.ui", "com.android.providers.downloads", "com.xiaomi.market", "tv.danmaku.bili", "org.zwanoo.android.speedtest"};
                        } else {
                            packages = new String[]{"com.spotify.music", "com.ebay.mobile", "com.amazon.kindle", "com.instagram.android", "com.melodis.midomiMusicIdentifier.freemium", "com.google.android.youtube"};
                        }
                        for (String str3 : packages) {
                            SLAAppLib.this.mDoubleWifiAppPN.add(str3);
                        }
                    }
                    Log.d(SLAAppLib.TAG, "set DWApp: " + SLAAppLib.this.mDoubleWifiAppPN.toString());
                    for (PackageInfo app2 : apps) {
                        if (app2.packageName != null && app2.applicationInfo != null && SLAAppLib.this.mDoubleWifiAppPN.contains(app2.packageName)) {
                            int uid2 = app2.applicationInfo.uid;
                            if (SLAAppLib.this.mDoubleWifiUidList != null) {
                                SLAAppLib.this.mDoubleWifiUidList += Integer.toString(uid2) + ",";
                            } else {
                                SLAAppLib.this.mDoubleWifiUidList = Integer.toString(uid2) + ",";
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(SLAAppLib.TAG, "initSLMCloudObserver onChange error " + e);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_SLS_GAMING_WHITELIST), false, observer);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_DOUBLEWIFI_WHITELIST), false, observer);
        new Thread(new Runnable() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                observer.onChange(false);
            }
        }).start();
    }

    private void initSLAUIObserver() {
        ContentObserver mContentObserver = new ContentObserver(this.mHandler) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                int linkTurboMode = Settings.System.getInt(SLAAppLib.this.mContext.getContentResolver(), SLAAppLib.LINK_TURBO_MODE, 0);
                if (SLAAppLib.mLinkTurboMode != linkTurboMode) {
                    SLAAppLib.mLinkTurboMode = linkTurboMode;
                    SLAAppLib.setSLAMode();
                }
                super.onChange(selfChange);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(LINK_TURBO_MODE), false, mContentObserver);
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction(ACTION_DUAL_DATA_CONCURRENT_MODE_WHITE_LIST_DONE);
        intentFilter.addAction(ACTION_DUAL_DATA_CONCURRENT_LIMITED_WHITE_LIST_DONE);
        intentFilter.addDataScheme("package");
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int uid;
                String action = intent.getAction();
                String packageName = intent.getData().getSchemeSpecificPart();
                if (TextUtils.isEmpty(packageName) || (uid = intent.getIntExtra("android.intent.extra.UID", -1)) == -1) {
                    return;
                }
                Log.e(SLAAppLib.TAG, "initBroadcastReceiver" + packageName);
                if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    Log.i(SLAAppLib.TAG, "ACTION_PACKAGE_ADDED uid = " + uid + packageName);
                    if (SLAAppLib.this.mSLSGameAppPN.contains(packageName)) {
                        SLAAppLib.this.addSLAAppDefault(uid);
                        if (SLAAppLib.this.mSLSGameUidList == null) {
                            SLAAppLib.this.mSLSGameUidList = Integer.toString(uid) + ",";
                        } else {
                            SLAAppLib.this.mSLSGameUidList += Integer.toString(uid) + ",";
                        }
                    } else if (SLAAppLib.this.mSLAAppDefaultPN.contains(packageName)) {
                        SLAAppLib.this.addSLAAppDefault(uid);
                    }
                    if (SLAAppLib.this.mDoubleWifiAppPN.contains(packageName)) {
                        if (SLAAppLib.this.mDoubleWifiUidList == null) {
                            SLAAppLib.this.mDoubleWifiUidList = Integer.toString(uid) + ",";
                        } else {
                            SLAAppLib.this.mDoubleWifiUidList += Integer.toString(uid) + ",";
                        }
                    }
                    if (SLAAppLib.this.mDoubleDataAppPN.contains(packageName)) {
                        if (SLAAppLib.this.mDoubleDataUidList == null) {
                            SLAAppLib.this.mDoubleDataUidList = Integer.toString(uid) + ",";
                        } else {
                            SLAAppLib.this.mDoubleDataUidList += Integer.toString(uid) + ",";
                        }
                    }
                    if (packageName.equals(SLAAppLib.mSLSVoIPAppPN)) {
                        if (SLAAppLib.this.mSLSGameAppPN.contains(SLAAppLib.mSLSVoIPAppPN)) {
                            SLAAppLib.this.mSLSVoIPUid = uid;
                        } else {
                            SLAAppLib.this.mSLSVoIPUid = 0;
                        }
                    }
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    Log.i(SLAAppLib.TAG, "ACTION_PACKAGE_REMOVED uid = " + uid + packageName);
                    synchronized (SLAAppLib.mSLAAppLibLock) {
                        if (SLAAppLib.this.getSLAAppByUid(Integer.toString(uid)) != null) {
                            Log.i(SLAAppLib.TAG, "package upgrade store, uid = " + uid + packageName);
                            SLAAppLib.this.storeSLAAppUpgrade(packageName);
                        }
                    }
                    SLAAppLib.this.clearSLAApp(Integer.toString(uid));
                    SLAAppLib.this.deleteSLAAppDefault(uid);
                    if (SLAAppLib.this.mSLSGameUidList != null) {
                        SLAAppLib sLAAppLib = SLAAppLib.this;
                        sLAAppLib.mSLSGameUidList = sLAAppLib.mSLSGameUidList.replaceAll(Integer.toString(uid) + ",", "");
                    }
                    if (SLAAppLib.this.mDoubleWifiUidList != null) {
                        SLAAppLib sLAAppLib2 = SLAAppLib.this;
                        sLAAppLib2.mDoubleWifiUidList = sLAAppLib2.mDoubleWifiUidList.replaceAll(Integer.toString(uid) + ",", "");
                    }
                    if (SLAAppLib.this.mDoubleDataUidList != null) {
                        SLAAppLib sLAAppLib3 = SLAAppLib.this;
                        sLAAppLib3.mDoubleDataUidList = sLAAppLib3.mDoubleDataUidList.replaceAll(Integer.toString(uid) + ",", "");
                    }
                    if (packageName.equals(SLAAppLib.mSLSVoIPAppPN)) {
                        SLAAppLib.this.mSLSVoIPUid = 0;
                    }
                } else if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
                    Log.i(SLAAppLib.TAG, "ACTION_PACKAGE_REPLACED uid = " + uid + packageName);
                    if (SLAAppLib.this.restoreSLAAppUpgrade(packageName)) {
                        Log.i(SLAAppLib.TAG, "package upgrade restore, uid = " + uid + packageName);
                        SLAAppLib.this.sendMsgAddSLAUid(Integer.toString(uid));
                    }
                } else if (SLAAppLib.ACTION_DUAL_DATA_CONCURRENT_MODE_WHITE_LIST_DONE.equals(action) || SLAAppLib.ACTION_DUAL_DATA_CONCURRENT_LIMITED_WHITE_LIST_DONE.equals(action)) {
                    Log.i(SLAAppLib.TAG, "DUAL_DATA_COCURRENT_WHITE_LIST CHANGE");
                    SLAAppLib.this.fetchDoubleDataWhiteListApp();
                }
                SLAAppLib.this.sendMsgSetSLAAppList();
                if (SLAAppLib.this.mSLSVoIPUid != 0) {
                    SLAAppLib sLAAppLib4 = SLAAppLib.this;
                    sLAAppLib4.mSLSGameUidList = sLAAppLib4.mSLSGameUidList.replace(Integer.toString(SLAAppLib.this.mSLSVoIPUid), "");
                    SLAAppLib sLAAppLib5 = SLAAppLib.this;
                    sLAAppLib5.mSLSGameUidList = sLAAppLib5.mSLSGameUidList.replace(",,", ",");
                }
                SLSAppLib.setSLSGameUidList(SLAAppLib.this.mSLSGameUidList);
                SLSAppLib.setSLSVoIPUid(SLAAppLib.this.mSLSVoIPUid);
                NetworkAccelerateSwitchService.setNetworkAccelerateSwitchUidList(SLAAppLib.this.mSLSGameUidList);
                NetworkAccelerateSwitchService.setNetworkAccelerateSwitchVoIPUid(SLAAppLib.this.mSLSVoIPUid);
                Log.i(SLAAppLib.TAG, "mDoubleWifiUidList = " + SLAAppLib.this.mDoubleWifiUidList);
                Log.i(SLAAppLib.TAG, "mDoubleDataUidList = " + SLAAppLib.this.mDoubleDataUidList);
            }
        };
        this.mContext.registerReceiver(receiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initSLAAppDefault() {
        String[] packages;
        String[] packages2;
        if (!this.mSLAAppDefaultPN.isEmpty()) {
            return;
        }
        String whiteString_sla = Settings.System.getString(this.mContext.getContentResolver(), CLOUD_SLA_WHITELIST);
        Log.d(TAG, "Cloud SLAAppDefault: " + whiteString_sla);
        if (!TextUtils.isEmpty(whiteString_sla) && (packages2 = whiteString_sla.split(",")) != null) {
            for (String str : packages2) {
                this.mSLAAppDefaultPN.add(str);
            }
        }
        if (this.mSLAAppDefaultPN.isEmpty()) {
            if ("CN".equalsIgnoreCase(SystemProperties.get("ro.boot.hwc"))) {
                packages = new String[]{mSLSVoIPAppPN, "com.tencent.mobileqq", ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.taobao.taobao", "com.tmall.wireless", "com.jingdong.app.mall", "com.xiaomi.youpin", "com.tencent.mtt", "com.hupu.games", "com.zhihu.android", "com.dianping.v1", "com.tencent.qqmusic", "com.netease.cloudmusic", "com.UCMobile", "com.ss.android.article.news", "com.smile.gifmaker", "com.ss.android.ugc.aweme", "tv.danmaku.bili", "org.zwanoo.android.speedtest"};
            } else {
                packages = new String[]{"com.spotify.music", "com.ebay.mobile", "com.amazon.kindle", "com.instagram.android", "com.melodis.midomiMusicIdentifier.freemium", "com.google.android.youtube"};
            }
            for (String str2 : packages) {
                this.mSLAAppDefaultPN.add(str2);
            }
        }
        Log.d(TAG, "set SLAAppDefault: " + this.mSLAAppDefaultPN.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addSLAAppDefault(int uid) {
        SLAApp slaDefUid = getSLAAppByUid(Integer.toString(uid));
        if (slaDefUid == null) {
            SLAApp slaDefUid2 = new SLAApp(Integer.toString(uid));
            mAppLists.add(slaDefUid2);
            mTraffic.put(Integer.toString(uid), 0L);
            dbAddSLAApp(slaDefUid2);
            return;
        }
        dbUpdateSLAApp(slaDefUid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteSLAAppDefault(int uid) {
        SLAApp slaDefUid = getSLAAppByUid(Integer.toString(uid));
        if (slaDefUid != null) {
            mAppLists.remove(slaDefUid);
            dbDeleteSLAApp(slaDefUid);
        }
    }

    private int addSLAApp(String uid) {
        int ret;
        synchronized (mSLAAppLibLock) {
            SLAApp app = getSLAAppByUid(uid);
            if (app == null) {
                mAppLists.add(new SLAApp(uid));
                mTraffic.put(uid, 0L);
                ret = 1;
            } else {
                app.setState(true);
                ret = 0;
            }
        }
        return ret;
    }

    private int deleteSLAApp(String uid) {
        int ret = 0;
        synchronized (mSLAAppLibLock) {
            SLAApp app = getSLAAppByUid(uid);
            if (app != null) {
                app.setState(false);
                ret = 1;
            }
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearSLAApp(String uid) {
        SLAApp app;
        Log.i(TAG, "clearSLAApp uid = " + uid);
        synchronized (mSLAAppLibLock) {
            app = getSLAAppByUid(uid);
            if (app != null) {
                mAppLists.remove(app);
            }
        }
        if (app != null) {
            dbDeleteSLAApp(app);
            Settings.System.putString(this.mContext.getContentResolver(), LINKTURBO_UID_TO_SLA, getLinkTurboWhiteList());
        }
    }

    private void refreshSLAAppUpgradeList() {
        long now = Calendar.getInstance().getTimeInMillis();
        Iterator<Map.Entry<String, Long>> it = mSLAAppUpgradeList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> item = it.next();
            if (item.getValue().longValue() + 300000 < now) {
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void storeSLAAppUpgrade(String pkg) {
        long now = Calendar.getInstance().getTimeInMillis();
        refreshSLAAppUpgradeList();
        if (mSLAAppUpgradeList.containsKey(pkg)) {
            mSLAAppUpgradeList.replace(pkg, Long.valueOf(now));
        } else {
            mSLAAppUpgradeList.put(pkg, Long.valueOf(now));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean restoreSLAAppUpgrade(String pkg) {
        refreshSLAAppUpgradeList();
        if (!mSLAAppUpgradeList.containsKey(pkg)) {
            return false;
        }
        mSLAAppUpgradeList.remove(pkg);
        return true;
    }

    private void initDoubleDataCloudObserver() {
        Log.d(TAG, "initDoubleDataWhiteListApp");
        fetchDoubleDataWhiteListApp();
        ContentObserver observer = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAAppLib.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                try {
                    SLAAppLib.this.fetchDoubleDataWhiteListApp();
                } catch (Exception e) {
                    Log.e(SLAAppLib.TAG, "initDoubleDataCloudObserver onChange error " + e);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(CLOUD_DOUBLEDATA_MODE_WHITELIST), false, observer);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(CLOUD_DOUBLEDATA_LIMITED_WHITELIST), false, observer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchDoubleDataWhiteListApp() {
        String[] packages;
        Log.d(TAG, "fetchDoubleDataWhiteListApp: mDoubleDataUidList = " + this.mDoubleDataUidList + "mDoubleDataAppPN size = " + this.mDoubleDataAppPN.size());
        if (this.mDoubleDataUidList != null || this.mDoubleDataAppPN.size() > 0) {
            Log.d(TAG, "reset mDoubleDataAppPN and mDoubleDataUidList");
            this.mDoubleDataAppPN.clear();
            this.mDoubleDataUidList = null;
        }
        String tput_data = Settings.Global.getString(this.mContext.getContentResolver(), CLOUD_DOUBLEDATA_MODE_WHITELIST);
        String limited_data = Settings.Global.getString(this.mContext.getContentResolver(), CLOUD_DOUBLEDATA_LIMITED_WHITELIST);
        String whiteString_doubleData = tput_data + "," + limited_data;
        Log.d(TAG, "Cloud DDApp: " + whiteString_doubleData);
        if (!TextUtils.isEmpty(whiteString_doubleData) && (packages = whiteString_doubleData.split(",")) != null) {
            for (String str : packages) {
                this.mDoubleDataAppPN.add(str);
            }
        }
        PackageManager pm = this.mContext.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(1);
        for (PackageInfo app : apps) {
            if (app.packageName != null && app.applicationInfo != null && this.mDoubleDataAppPN.contains(app.packageName)) {
                int uid = app.applicationInfo.uid;
                if (this.mDoubleDataUidList == null) {
                    this.mDoubleDataUidList = Integer.toString(uid) + ",";
                } else {
                    this.mDoubleDataUidList += Integer.toString(uid) + ",";
                }
            }
        }
        Log.i(TAG, "mDoubleDataUidList = " + this.mDoubleDataUidList);
    }

    public void setDDSLAMode() {
        try {
            Log.i(TAG, "setSLAMode: SLA_MODE_CONCURRENT");
            mSlaService.setSLAMode(SLA_MODE_CONCURRENT);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void sendMsgUpdateTrafficStat() {
        Message msg = Message.obtain();
        msg.what = STOP_CALC_TRAFFIC_STAT;
        this.mHandler.sendMessage(msg);
    }
}
