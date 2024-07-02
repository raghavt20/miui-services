package com.android.server.location.gnss.exp;

import android.app.ActivityManager;
import android.app.IProcessObserver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.ILocationListener;
import android.location.LocationRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MiuiStubHead(manifestName = "com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub$$")
/* loaded from: classes.dex */
public class GnssBackgroundUsageOptImpl extends GnssBackgroundUsageOptStub {
    private static final int BACKGROUND_USAGE_OPT_REMOVE_IMMEDIATELY = 0;
    private static final int BACKGROUND_USAGE_OPT_TIME = 10000;
    private static final int DEF_UID = -1;
    private static final String REQ_KEY_SUFFIX_DUP = "-dupReq";
    private static final String TAG = "GnssBackgroundUsageOpt";
    private static Context mContext;
    private boolean mAlreadyLoadDataFromSP;
    private boolean mCurrentForeground;
    private IProcessObserver.Stub mIProcessObserverStub;
    private GnssBackgroundUsageOptStub.IRemoveRequest mIRemoveRequest;
    private GnssBackgroundUsageOptStub.IRestoreRequest mIRestoreRequest;
    private volatile boolean mIsBackFromGsco;
    private volatile boolean mIsUseUidCtl;
    private boolean mOldForeground;
    private int mOldUid;
    private int mRemovedUid;
    private long mScStartTime;
    private final AtomicBoolean mIsSpecifiedDevice = new AtomicBoolean(false);
    private final AtomicBoolean mIsInSatelliteCallMode = new AtomicBoolean(false);
    private final Map<Integer, Boolean> mUidForMap = new ConcurrentHashMap();
    private final File mCloudSpFile = new File(new File(Environment.getDataDirectory(), "system"), "IsSpecifiedDevice.xml");
    private final boolean mDefaultFeatureStatus = SystemProperties.getBoolean("persist.sys.gnss_back.opt", false);
    private final boolean D = true;
    private final Map<String, GnssRequestBean> mRequestMap = new ConcurrentHashMap();
    private final Map<String, Thread> mRemoveThreadMap = new ConcurrentHashMap();
    private final Map<Integer, Long> mBackOpt3Map = new ConcurrentHashMap();
    private HashSet<Integer> mSatelliteCallAppUidSet = new HashSet<>();
    private HashSet<String> mSatelliteCallPkgSet = new HashSet<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssBackgroundUsageOptImpl> {

        /* compiled from: GnssBackgroundUsageOptImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssBackgroundUsageOptImpl INSTANCE = new GnssBackgroundUsageOptImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssBackgroundUsageOptImpl m1816provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssBackgroundUsageOptImpl m1815provideNewInstance() {
            return new GnssBackgroundUsageOptImpl();
        }
    }

    GnssBackgroundUsageOptImpl() {
    }

    public boolean request(Context context, String provider, CallerIdentity identity, LocationRequest locationRequest, boolean foreground, int permissionLevel, boolean hasLocationPermissions, Object callbackType) {
        mContext = context;
        if (!this.mAlreadyLoadDataFromSP) {
            loadFeatureSwitch();
        }
        String requestKey = createReqKey(identity.getUid(), identity.getPid(), provider, identity.getListenerId());
        putIntoRequestMap(requestKey, putGnssRequestBean(identity, provider, locationRequest, foreground, permissionLevel, callbackType), false);
        Log.d(TAG, "request: mRequestMap add: " + requestKey + "\nNow" + locationRequest + " callType:" + callbackType + " identity:" + identity);
        if (!this.mIsSpecifiedDevice.get() || !"gps".equals(provider)) {
            if (this.mIsInSatelliteCallMode.get()) {
                if (this.mIsUseUidCtl) {
                    if (!this.mSatelliteCallAppUidSet.contains(Integer.valueOf(identity.getUid()))) {
                        Log.i(TAG, "request location by uid:" + requestKey + " need to return...");
                        return true;
                    }
                } else if (!this.mSatelliteCallPkgSet.contains(identity.getPackageName())) {
                    Log.i(TAG, "request location by pkg:" + requestKey + " need to return...");
                    return true;
                }
            } else {
                Log.i(TAG, "request provider:" + provider + " key:" + requestKey);
            }
        } else {
            Long backTime = this.mBackOpt3Map.get(Integer.valueOf(identity.getUid()));
            if (!foreground && backTime != null && SystemClock.elapsedRealtime() - backTime.longValue() > 10000) {
                Log.d(TAG, "remove by back opt 3.0:" + requestKey);
                GnssEventTrackingStub.getInstance().recordGnssBackgroundOpt3Time();
                this.mRequestMap.remove(requestKey);
                return true;
            }
            if (foreground) {
                this.mBackOpt3Map.remove(Integer.valueOf(identity.getUid()));
            }
            Log.i(TAG, "normal request location key:" + requestKey);
            if (!foreground) {
                remove(provider, requestKey, 10000);
            }
        }
        return false;
    }

    public void onAppForegroundChanged(int uid, boolean foreground) {
        GnssRequestBean bean;
        if (!foreground) {
            this.mBackOpt3Map.put(Integer.valueOf(uid), Long.valueOf(SystemClock.elapsedRealtime()));
        } else {
            this.mBackOpt3Map.remove(Integer.valueOf(uid));
        }
        if (this.mOldUid == uid && this.mOldForeground == foreground) {
            return;
        }
        this.mUidForMap.put(Integer.valueOf(uid), Boolean.valueOf(foreground));
        this.mOldUid = uid;
        this.mOldForeground = foreground;
        if (!this.mIsSpecifiedDevice.get() || this.mIsInSatelliteCallMode.get() || this.mIsBackFromGsco) {
            return;
        }
        Map<String, GnssRequestBean> reqMap = new HashMap<>(this.mRequestMap);
        for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
            if (uid == map.getValue().identity.getUid() && (bean = map.getValue()) != null && "gps".equals(bean.provider)) {
                Thread removeThread = this.mRemoveThreadMap.get(map.getKey());
                if (!foreground) {
                    remove(bean.provider, map.getKey(), 10000);
                } else if (bean.removeByOpt) {
                    Log.d(TAG, "change to foreground remove by opt and now restore...");
                    restore(map.getKey(), bean.provider, bean.locationRequest, bean.identity, bean.permissionLevel, bean.callbackType);
                } else if (removeThread != null) {
                    removeThread.interrupt();
                    Log.d(TAG, "remove Thread not null, interrupt it...");
                }
            }
        }
    }

    public void removeByLmsUser(int uid, int pid, Object callbackType) {
        Log.d(TAG, "removeByLmsUser" + Integer.toString(uid) + Integer.toString(pid));
        if (!this.mIsInSatelliteCallMode.get() || callbackType == null) {
            return;
        }
        Map<String, GnssRequestBean> reqMap = new HashMap<>(this.mRequestMap);
        for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
            if (((map.getValue().callbackType instanceof ILocationListener) && (callbackType instanceof ILocationListener) && ((ILocationListener) callbackType).asBinder() == ((ILocationListener) map.getValue().callbackType).asBinder()) || callbackType.equals(map.getValue().callbackType)) {
                if (map.getValue().identity.getUid() == uid && map.getValue().identity.getPid() == pid) {
                    this.mRequestMap.remove(map.getKey());
                    Thread thread = this.mRemoveThreadMap.get(map.getKey());
                    if (thread != null) {
                        thread.interrupt();
                    }
                    Log.d(TAG, "removeByLmsUser: key:" + map.getKey());
                } else {
                    Log.e(TAG, "callbackType seem but not true uid pid..");
                }
            }
        }
    }

    public void remove(int uid, int pid, String provider, String listenerId) {
        String removeKey = createReqKey(uid, pid, provider, listenerId);
        if (this.mRequestMap.get(removeKey + REQ_KEY_SUFFIX_DUP) != null) {
            removeKey = removeKey + REQ_KEY_SUFFIX_DUP;
            Log.i(TAG, "same req, return dup first...");
        } else {
            GnssRequestBean bean = this.mRequestMap.get(removeKey);
            if (bean != null) {
                Log.d(TAG, "remove normal: key:" + removeKey + " " + bean.callbackType);
            }
        }
        this.mRequestMap.remove(removeKey);
        Thread thread = this.mRemoveThreadMap.get(removeKey);
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void registerSatelliteCallMode(final HashSet<String> pkgSet, final String key) {
        new Thread(new Runnable() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GnssBackgroundUsageOptImpl.this.lambda$registerSatelliteCallMode$0(pkgSet, key);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerSatelliteCallMode$0(HashSet pkgSet, String key) {
        synchronized (GnssBackgroundUsageOptImpl.class) {
            Log.d(TAG, "=================registerSatelliteCallMode, Start =================");
            if (this.mIsInSatelliteCallMode.get()) {
                Log.d(TAG, "is in Satellite Call Mode do not need register...");
                return;
            }
            Log.i(TAG, "removeAndBlockAllRequestExPkg，pkg:" + pkgSet.toString() + ",key:" + key);
            if (this.KEY_API_USE != key) {
                Log.e(TAG, "removeAndBlockAllRequestExPkg key is invalid...");
                return;
            }
            this.mIsSpecifiedDevice.set(false);
            this.mIsInSatelliteCallMode.set(true);
            this.mScStartTime = SystemClock.elapsedRealtime();
            removeCurrentAllRequestExPkgSet(pkgSet);
        }
    }

    public void unRegisterSatelliteCallMode() {
        new Thread(new Runnable() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                GnssBackgroundUsageOptImpl.this.lambda$unRegisterSatelliteCallMode$1();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$unRegisterSatelliteCallMode$1() {
        synchronized (GnssBackgroundUsageOptImpl.class) {
            if (!this.mIsInSatelliteCallMode.get()) {
                Log.d(TAG, "is not in Satellite Call Mode do not need unRegister...");
                return;
            }
            this.mIsBackFromGsco = true;
            loadFeatureSwitch();
            this.mIsInSatelliteCallMode.set(false);
            if (this.mScStartTime != 0) {
                GnssEventTrackingStub.getInstance().recordGnssSatelliteCallOptDuring(SystemClock.elapsedRealtime() - this.mScStartTime);
            }
            restoreGsco();
            this.mIsBackFromGsco = false;
            Log.d(TAG, "=================unRegisterSatelliteCallMode, End =================");
        }
    }

    public void registerRequestCallback(GnssBackgroundUsageOptStub.IRemoveRequest iRemoveRequest) {
        this.mIRemoveRequest = iRemoveRequest;
    }

    public void registerRestoreCallback(GnssBackgroundUsageOptStub.IRestoreRequest iRestoreRequest) {
        this.mIRestoreRequest = iRestoreRequest;
    }

    public void setBackgroundOptStatus(boolean status) {
        if (!this.mIsSpecifiedDevice.get() && status) {
            registerProcessObserver();
            Log.d(TAG, "Has Register Process Observer by cloud...");
            this.mIsSpecifiedDevice.set(true);
            saveCloudDataToSP(this.mIsSpecifiedDevice.get());
            return;
        }
        if (this.mIsSpecifiedDevice.get() && !status) {
            unRegisterProcessObserver();
            Log.d(TAG, "Has unRegister Process Observer by cloud...");
            this.mIsSpecifiedDevice.set(false);
            saveCloudDataToSP(this.mIsSpecifiedDevice.get());
        }
    }

    public boolean getSatelliteCallMode() {
        return this.mIsInSatelliteCallMode.get();
    }

    private void remove(final String providerName, final String key, final int delayTime) {
        Log.d(TAG, "remove key: " + key);
        if (this.mRemoveThreadMap.get(key) == null) {
            Thread thread = new Thread(new Runnable() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GnssBackgroundUsageOptImpl.this.lambda$remove$2(key, delayTime, providerName);
                }
            });
            thread.start();
            this.mRemoveThreadMap.put(key, thread);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$remove$2(String key, int delayTime, String providerName) {
        GnssRequestBean bean = this.mRequestMap.get(key);
        if (bean != null) {
            try {
                Log.d(TAG, "remove key sleep before: " + key + " delay:" + delayTime);
                if (delayTime != 0) {
                    Thread.sleep(delayTime);
                }
                Log.d(TAG, "remove key sleep after: " + key + " delay:" + delayTime);
                bean.isForegroundService = isForegroundService(bean.identity.getUid(), bean.identity.getPid());
                this.mIRemoveRequest.onRemoveListener(providerName, bean.callbackType);
                bean.removeByOpt = true;
                putIntoRequestMap(key, bean, true);
                if (this.mIsInSatelliteCallMode.get()) {
                    Log.d(TAG, "remove by GSCO opt, key:" + key);
                    GnssEventTrackingStub.getInstance().recordGnssSatelliteCallOptCnt();
                } else {
                    Log.d(TAG, "remove by GBO opt, key:" + key);
                    GnssEventTrackingStub.getInstance().recordGnssBackgroundOpt2Time();
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Log.e(TAG, "current remove thread has been interrupted...");
                    this.mRemoveThreadMap.remove(key);
                    return;
                }
            }
        } else {
            Log.d(TAG, "remove by opt interrupt, key:" + key);
        }
        this.mRemoveThreadMap.remove(key);
    }

    private void restore(String key, String providerName, LocationRequest request, CallerIdentity identity, int permissionLevel, Object callbackType) {
        try {
            this.mRequestMap.remove(key);
            GnssBackgroundUsageOptStub.IRestoreRequest iRestoreRequest = this.mIRestoreRequest;
            if (iRestoreRequest != null) {
                iRestoreRequest.onRestore(providerName, request, identity, permissionLevel, callbackType);
            }
            Log.d(TAG, "restore by opt,uid:" + identity.getUid() + " pkg:" + identity.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "restore exception-->" + e);
        }
    }

    private void registerProcessObserver() {
        this.mIProcessObserverStub = new IProcessObserver.Stub() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl.1
            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) throws RemoteException {
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) throws RemoteException {
            }

            public void onProcessDied(int pid, int uid) throws RemoteException {
                Map<String, GnssRequestBean> reqMap = new HashMap<>((Map<? extends String, ? extends GnssRequestBean>) GnssBackgroundUsageOptImpl.this.mRequestMap);
                for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
                    if (uid == map.getValue().identity.getUid() && pid == map.getValue().identity.getPid()) {
                        if (GnssBackgroundUsageOptImpl.this.isProcessAlive(pid)) {
                            Log.i(GnssBackgroundUsageOptImpl.TAG, "pid:" + pid + " is still alive, do not need clear map");
                            return;
                        }
                        GnssBackgroundUsageOptImpl.this.mRequestMap.remove(map.getKey() + GnssBackgroundUsageOptImpl.REQ_KEY_SUFFIX_DUP);
                        GnssBackgroundUsageOptImpl.this.mRequestMap.remove(map.getKey());
                        Thread removeThread = (Thread) GnssBackgroundUsageOptImpl.this.mRemoveThreadMap.get(map.getKey());
                        if (removeThread != null) {
                            removeThread.interrupt();
                            Log.d(GnssBackgroundUsageOptImpl.TAG, "onProcessDied, remove Thread not null, interrupt it...");
                        }
                        Log.d(GnssBackgroundUsageOptImpl.TAG, "onProcessDied remove " + map.getKey());
                    }
                }
            }
        };
        try {
            ActivityManager.getService().registerProcessObserver(this.mIProcessObserverStub);
        } catch (RemoteException e) {
            Log.e(TAG, "ActivityManager registerProcessObserver RemoteException...:" + e);
        }
    }

    private void unRegisterProcessObserver() {
        if (this.mIProcessObserverStub != null) {
            try {
                ActivityManager.getService().unregisterProcessObserver(this.mIProcessObserverStub);
                Log.d(TAG, "unRegisterProcessObserver...");
            } catch (RemoteException e) {
                Log.e(TAG, "ActivityManager unRegisterProcessObserver RemoteException...:" + e);
            }
        }
    }

    private void saveCloudDataToSP(boolean status) {
        Log.d(TAG, "Save mIsSpecifiedDevice running...");
        try {
            Context context = mContext;
            if (context == null) {
                return;
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(this.mCloudSpFile, 0).edit();
            editor.putBoolean("mIsSpecifiedDevice", status);
            editor.apply();
            Log.d(TAG, "Success to save mIsSpecifiedDevice...");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save mIsSpecifiedDevice..., " + e.toString());
        }
    }

    private boolean loadCloudDataFromSP() {
        Log.d(TAG, "load mIsSpecifiedDevice running...");
        try {
            Context context = mContext;
            if (context == null) {
                return this.mDefaultFeatureStatus;
            }
            Context directBootContext = context.createDeviceProtectedStorageContext();
            SharedPreferences editor = directBootContext.getSharedPreferences(this.mCloudSpFile, 0);
            boolean status = editor.getBoolean("mIsSpecifiedDevice", this.mDefaultFeatureStatus);
            this.mAlreadyLoadDataFromSP = true;
            Log.d(TAG, "Success to load mIsSpecifiedDevice...");
            return status;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load mIsSpecifiedDevice..., " + e.toString());
            return this.mDefaultFeatureStatus;
        }
    }

    private int getUidFromKey(String key) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(key);
        if (!matcher.find()) {
            return -1;
        }
        try {
            int uid = Integer.parseInt(matcher.group());
            return uid;
        } catch (NumberFormatException e) {
            Log.e(TAG, "uid parseInt NumberFormatException...");
            return -1;
        }
    }

    private void loadFeatureSwitch() {
        this.mIsSpecifiedDevice.set(loadCloudDataFromSP());
        if (this.mAlreadyLoadDataFromSP && this.mIsSpecifiedDevice.get()) {
            registerProcessObserver();
            Log.d(TAG, "Has Register Process Observer...");
        }
        Log.d(TAG, "Is Specified Device:" + this.mIsSpecifiedDevice);
    }

    private int getForegroundProcessUid() {
        Context context = mContext;
        if (context == null) {
            return -1;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (processInfo.importance == 100) {
                return processInfo.uid;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isProcessAlive(int pid) {
        Context context = mContext;
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses();
        if (processList != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : processList) {
                if (processInfo.pid == pid) {
                    return true;
                }
            }
        }
        return false;
    }

    private GnssRequestBean putGnssRequestBean(CallerIdentity identity, String provider, LocationRequest locationRequest, boolean foreground, int permissionLevel, Object callbackType) {
        GnssRequestBean requestBean = new GnssRequestBean();
        requestBean.identity = identity;
        requestBean.callbackType = callbackType;
        requestBean.provider = provider;
        requestBean.removeByOpt = false;
        requestBean.locationRequest = locationRequest;
        requestBean.permissionLevel = permissionLevel;
        requestBean.isForegroundService = isForegroundService(identity.getUid(), identity.getPid());
        this.mOldForeground = foreground;
        return requestBean;
    }

    private void removeCurrentAllRequestExPkgSet(HashSet<String> pkgSet) {
        if (pkgSet == null || pkgSet.isEmpty()) {
            Log.e(TAG, "removeCurrentAllRequestExPkg pkg is invalid...");
            return;
        }
        this.mSatelliteCallAppUidSet.clear();
        this.mSatelliteCallPkgSet = pkgSet;
        boolean isException = false;
        Iterator<String> it = pkgSet.iterator();
        while (it.hasNext()) {
            String p = it.next();
            try {
                Context context = mContext;
                if (context != null) {
                    this.mSatelliteCallAppUidSet.add(Integer.valueOf(context.getPackageManager().getPackageUid(p, 0)));
                }
            } catch (Exception e) {
                isException = true;
                Log.e(TAG, "getPackageManager Exception:" + e);
            }
        }
        if (isException) {
            this.mIsUseUidCtl = false;
            removeCurrentAllRequestExPkgByPkgSet(pkgSet);
        } else if (!this.mSatelliteCallAppUidSet.isEmpty()) {
            this.mIsUseUidCtl = true;
            removeCurrentAllRequestExPkgByUidSet(this.mSatelliteCallAppUidSet);
        }
    }

    private void removeCurrentAllRequestExPkgByPkgSet(HashSet<String> pkgSet) {
        if (pkgSet == null) {
            return;
        }
        Map<String, GnssRequestBean> reqMap = new HashMap<>(this.mRequestMap);
        for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
            if (!pkgSet.contains(map.getValue().identity.getPackageName())) {
                remove(map.getValue().provider, map.getKey(), 0);
                Log.i(TAG, "removeCurrentAllRequestExPkgByPkg, uid:" + map.getValue().identity.getUid() + ", pkg:" + map.getValue().identity.getPackageName() + "\nNow :" + map.getValue().locationRequest);
            }
        }
    }

    private void removeCurrentAllRequestExPkgByUidSet(HashSet<Integer> uidSet) {
        if (uidSet == null) {
            return;
        }
        Map<String, GnssRequestBean> reqMap = new HashMap<>(this.mRequestMap);
        for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
            if (!uidSet.contains(Integer.valueOf(map.getValue().identity.getUid()))) {
                remove(map.getValue().provider, map.getKey(), 0);
                Log.i(TAG, "removeCurrentAllRequestExPkgByUid, key:" + map.getKey() + ", pkg:" + map.getValue().identity.getPackageName() + "\n Now mRequestMap:" + map.getValue().locationRequest);
            }
        }
    }

    private boolean isForegroundService(int uid, int pid) {
        Context context = mContext;
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.uid == uid && service.pid == pid && service.foreground) {
                return true;
            }
        }
        return false;
    }

    private void putIntoRequestMap(String key, GnssRequestBean value, boolean isModify) {
        if (isModify) {
            this.mRequestMap.put(key, value);
            return;
        }
        GnssRequestBean bean = this.mRequestMap.get(key);
        if (bean != null && (((value.callbackType instanceof ILocationListener) && ((ILocationListener) value.callbackType).asBinder() == ((ILocationListener) bean.callbackType).asBinder()) || value.callbackType.equals(bean.callbackType))) {
            this.mRequestMap.put(key + REQ_KEY_SUFFIX_DUP, value);
        } else {
            this.mRequestMap.put(key, value);
        }
    }

    private String createReqKey(int uid, int pid, String providerName, String listenerId) {
        return Integer.toString(uid) + Integer.toString(pid) + providerName + listenerId;
    }

    private void restoreGsco() {
        Map<String, GnssRequestBean> reqMap = new HashMap<>(this.mRequestMap);
        for (Map.Entry<String, GnssRequestBean> map : reqMap.entrySet()) {
            GnssRequestBean bean = map.getValue();
            if (bean != null) {
                if (bean.isForegroundService) {
                    Log.d(TAG, "key:" + map.getKey() + " pkg:" + bean.identity.getPackageName() + " is isForegroundService");
                    restore(map.getKey(), bean.provider, bean.locationRequest, bean.identity, bean.permissionLevel, bean.callbackType);
                } else {
                    Log.d(TAG, "key:" + map.getKey() + " pkg:" + bean.identity.getPackageName() + " is not isForegroundService Foreground:" + this.mUidForMap.get(Integer.valueOf(bean.identity.getUid())) + " mUidForMap.containsKey:" + this.mUidForMap.containsKey(Integer.valueOf(bean.identity.getUid())));
                    Log.d(TAG, "!mSatelliteCallAppUidSet.contains(bean.identity.getUid()):" + (!this.mSatelliteCallAppUidSet.contains(Integer.valueOf(bean.identity.getUid()))) + " bean.identity.getUid():" + bean.identity.getUid());
                    if (!this.mSatelliteCallAppUidSet.contains(Integer.valueOf(bean.identity.getUid())) && this.mUidForMap.containsKey(Integer.valueOf(bean.identity.getUid())) && this.mUidForMap.get(Integer.valueOf(bean.identity.getUid())).booleanValue()) {
                        Log.d(TAG, "change to foreground remove by GSCO opt and now restore key:" + map.getKey() + " pkg:" + bean.identity.getPackageName() + " isForegroundService:" + bean.isForegroundService);
                        restore(map.getKey(), bean.provider, bean.locationRequest, bean.identity, bean.permissionLevel, bean.callbackType);
                    }
                }
            }
        }
    }
}
