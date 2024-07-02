package com.miui.server.security;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import com.android.server.LocalServices;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.server.security.model.PackageBehaviorBean;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.mqsas.scout.ScoutUtils;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;
import miui.security.AppBehavior;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AppBehaviorService {
    private static final Uri ACCESSIBILITY_URI = Settings.Secure.getUriFor("enabled_accessibility_services");
    private static final String COMPONENT_NAME_SEPARATOR = ":";
    public static final boolean DEBUG = false;
    private static final int MAX_CACHE_COUNT = 200;
    private static final int MSG_BIND_SEC = 3605;
    private static final int MSG_DURATION_TOO_LONG_CHECK = 3602;
    private static final int MSG_FOREGROUND_CHECK = 3600;
    private static final int MSG_TRANSPORT_END = 3604;
    private static final int MSG_TRANSPORT_INFO = 3603;
    private static final String TAG = "AppBehaviorService";
    private final SparseBooleanArray mAlwaysRecordBehavior;
    private AppDurationService mAppBehaviorDuration;
    private final AppOpsManager mAppOps;
    private final SparseLongArray mBehaviorThreshold;
    private final Context mContext;
    private final List<String> mLastEnableAccessibility;
    private final ArrayMap<String, Set<Integer>> mPackageRecordBehavior;
    private Messenger mPersistMessenger;
    private int mRecordSize;
    private final ContentObserver mSettingsObserver;
    private final Handler mWorkHandler;
    private final Object mUidBehaviorLock = new Object();
    private final Object mRecordEnableLock = new Object();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean mRegisterForeground = false;
    private boolean mRegisterAccessibility = false;
    private final IForegroundInfoListener mForegroundInfoListener = new IForegroundInfoListener.Stub() { // from class: com.miui.server.security.AppBehaviorService.1
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            if (foregroundInfo == null || TextUtils.equals(foregroundInfo.mForegroundPackageName, foregroundInfo.mLastForegroundPackageName) || UserHandle.getAppId(foregroundInfo.mForegroundUid) < 10000 || AppBehaviorService.isSystem(AppBehaviorService.this.getApplicationInfo(foregroundInfo.mForegroundPackageName, UserHandle.getUserId(foregroundInfo.mForegroundUid))) || AppBehaviorService.this.mAppOps.checkOpNoThrow(10021, foregroundInfo.mForegroundUid, foregroundInfo.mForegroundPackageName) == 0) {
                return;
            }
            synchronized (AppBehaviorService.this.mForegroundRecorder) {
                AppBehaviorService.this.mForegroundRecorder.put(foregroundInfo.mForegroundPackageName, Integer.valueOf(((Integer) AppBehaviorService.this.mForegroundRecorder.getOrDefault(foregroundInfo.mForegroundPackageName, 0)).intValue() + 1));
            }
        }
    };
    protected final PackageManagerInternal mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    private final ArrayMap<String, Integer> mForegroundRecorder = new ArrayMap<>();
    private final ArrayMap<String, PackageBehaviorBean> mPackageBehavior = new ArrayMap<>();

    public AppBehaviorService(Context context, HandlerThread thread) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        SparseLongArray sparseLongArray = new SparseLongArray();
        this.mBehaviorThreshold = sparseLongArray;
        this.mLastEnableAccessibility = new ArrayList();
        this.mAlwaysRecordBehavior = new SparseBooleanArray();
        this.mPackageRecordBehavior = new ArrayMap<>();
        Handler handler = new Handler(thread.getLooper()) { // from class: com.miui.server.security.AppBehaviorService.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == AppBehaviorService.MSG_FOREGROUND_CHECK) {
                    AppBehaviorService.this.checkForegroundCount();
                } else if (msg.what == AppBehaviorService.MSG_DURATION_TOO_LONG_CHECK) {
                    AppBehaviorService.this.scanDurationTooLong();
                }
            }
        };
        this.mWorkHandler = handler;
        this.mSettingsObserver = new ContentObserver(handler) { // from class: com.miui.server.security.AppBehaviorService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (AppBehaviorService.ACCESSIBILITY_URI.equals(uri)) {
                    AppBehaviorService.this.checkNewEnableAccessibility();
                }
            }
        };
        sparseLongArray.put(35, 20L);
        sparseLongArray.put(37, 10L);
    }

    public boolean hasAppBehaviorWatching() {
        boolean z;
        synchronized (this.mRecordEnableLock) {
            z = this.mAlwaysRecordBehavior.size() > 0 || this.mPackageRecordBehavior.size() > 0;
        }
        return z;
    }

    public void updateBehaviorThreshold(int behavior, long threshold) {
        this.mBehaviorThreshold.put(behavior, threshold);
    }

    public long getThreshold(int behavior) {
        return this.mBehaviorThreshold.get(behavior, 0L);
    }

    public boolean moreThanThreshold(int behavior, long curCount) {
        return curCount >= getThreshold(behavior);
    }

    public void startWatchingAppBehavior(int behavior, boolean includeSystem) {
        synchronized (this.mRecordEnableLock) {
            this.mAlwaysRecordBehavior.put(behavior, includeSystem);
            registerListenerForBehavior(behavior, true);
            Slog.i(TAG, "startWatching:" + behavior + ",includeSystem:" + includeSystem);
        }
    }

    public void stopWatchingAppBehavior(int behavior) {
        synchronized (this.mRecordEnableLock) {
            if (behavior == 1) {
                this.mAlwaysRecordBehavior.clear();
                this.mPackageRecordBehavior.clear();
            } else {
                this.mAlwaysRecordBehavior.delete(behavior);
                Iterator<Map.Entry<String, Set<Integer>>> it = this.mPackageRecordBehavior.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Set<Integer>> item = it.next();
                    Set<Integer> itemWatching = item.getValue();
                    itemWatching.remove(Integer.valueOf(behavior));
                    if (itemWatching.isEmpty()) {
                        it.remove();
                    }
                }
            }
            registerListenerForBehavior(behavior, false);
            Slog.i(TAG, "stopWatching for all:" + behavior);
        }
    }

    public void startWatchingAppBehavior(int behavior, String[] pkgNames, boolean includeSystem) {
        synchronized (this.mRecordEnableLock) {
            for (String tmpPkgName : pkgNames) {
                ApplicationInfo applicationInfo = getApplicationInfo(tmpPkgName, 0);
                if (applicationInfo != null && (includeSystem || !isSystem(applicationInfo))) {
                    Set<Integer> behaviorWatching = this.mPackageRecordBehavior.get(tmpPkgName);
                    if (behaviorWatching == null) {
                        behaviorWatching = new ArraySet();
                        this.mPackageRecordBehavior.put(tmpPkgName, behaviorWatching);
                        Slog.i(TAG, "startWatching:" + behavior + ",pkg:" + tmpPkgName);
                    }
                    behaviorWatching.add(Integer.valueOf(behavior));
                }
            }
            registerListenerForBehavior(behavior, true);
        }
    }

    public void stopWatchingAppBehavior(int behavior, String[] pkgNames) {
        ApplicationInfo applicationInfo;
        synchronized (this.mRecordEnableLock) {
            for (String tmpPkgName : pkgNames) {
                Set<Integer> watchingBehavior = this.mPackageRecordBehavior.get(tmpPkgName);
                Slog.i(TAG, "stopWatching" + behavior + ",pkg:" + tmpPkgName);
                if (watchingBehavior != null) {
                    watchingBehavior.remove(Integer.valueOf(behavior));
                    if (watchingBehavior.isEmpty()) {
                        this.mPackageRecordBehavior.remove(tmpPkgName);
                    }
                }
                if (this.mAppBehaviorDuration != null && (applicationInfo = getApplicationInfo(tmpPkgName, 0)) != null) {
                    this.mAppBehaviorDuration.removeDurationEvent(behavior, applicationInfo.uid, applicationInfo.packageName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean canRecordBehavior(int behavior, String pkgName) {
        synchronized (this.mRecordEnableLock) {
            if (this.mAlwaysRecordBehavior.indexOfKey(behavior) >= 0) {
                if (this.mAlwaysRecordBehavior.get(behavior)) {
                    return true;
                }
                return isSystem(getApplicationInfo(pkgName, 0)) ? false : true;
            }
            ApplicationInfo applicationInfo = getApplicationInfo(pkgName, 0);
            if (applicationInfo == null) {
                return false;
            }
            Set<Integer> behaviorWatching = this.mPackageRecordBehavior.get(pkgName);
            if (behaviorWatching != null && !behaviorWatching.isEmpty()) {
                return behaviorWatching.contains(Integer.valueOf(behavior));
            }
            return false;
        }
    }

    public void persistBehaviorRecord() {
        if (this.mPersistMessenger == null) {
            if (this.mWorkHandler.hasMessages(MSG_BIND_SEC)) {
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.miui.permcenter.monitor.AppUsagePersistService"));
            Slog.i(TAG, "start persistBehaviorRecord, try bind service.");
            this.mContext.bindService(intent, new ServiceConnection() { // from class: com.miui.server.security.AppBehaviorService.4
                @Override // android.content.ServiceConnection
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Slog.i(AppBehaviorService.TAG, "on persistBehaviorRecord ServiceConnected");
                    AppBehaviorService.this.mPersistMessenger = new Messenger(service);
                    AppBehaviorService.this.schedulePersist();
                    AppBehaviorService.this.mWorkHandler.removeMessages(AppBehaviorService.MSG_BIND_SEC);
                }

                @Override // android.content.ServiceConnection
                public void onServiceDisconnected(ComponentName name) {
                    AppBehaviorService.this.mPersistMessenger = null;
                }
            }, 1);
            this.mWorkHandler.sendEmptyMessageDelayed(MSG_BIND_SEC, 10000L);
            return;
        }
        schedulePersist();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void schedulePersist() {
        Slog.i(TAG, "start schedulePersist, size may be:" + this.mRecordSize);
        if (this.mPersistMessenger != null) {
            synchronized (this.mUidBehaviorLock) {
                String fileName = this.mDateFormat.format(new Date());
                for (int i = 0; i < this.mPackageBehavior.size(); i++) {
                    String pkgName = this.mPackageBehavior.keyAt(i);
                    PackageBehaviorBean packageBehaviorBean = this.mPackageBehavior.get(pkgName);
                    if (packageBehaviorBean != null) {
                        try {
                            JSONObject record = packageBehaviorBean.toJson();
                            int init = this.mRecordSize - packageBehaviorBean.init();
                            this.mRecordSize = init;
                            this.mRecordSize = Math.max(init, 0);
                            if (record != null && record.length() > 0) {
                                JSONObject packageRecord = new JSONObject();
                                packageRecord.put(pkgName, record);
                                Message message = Message.obtain();
                                message.what = MSG_TRANSPORT_INFO;
                                Bundle bundle = new Bundle();
                                bundle.putString("android.intent.extra.TITLE", fileName);
                                bundle.putString("android.intent.extra.RETURN_RESULT", packageRecord.toString());
                                message.setData(bundle);
                                this.mPersistMessenger.send(message);
                            }
                        } catch (Exception e) {
                            Slog.e(TAG, "persist exception!", e);
                            if (this.mPersistMessenger == null) {
                                persistBehaviorRecord();
                                return;
                            }
                        }
                    }
                }
                Message message2 = Message.obtain();
                message2.what = MSG_TRANSPORT_END;
                try {
                    this.mPersistMessenger.send(message2);
                } catch (Exception e2) {
                }
            }
        }
    }

    public void recordAppBehaviorAsync(final AppBehavior appBehavior) {
        if (ScoutUtils.isLibraryTest()) {
            return;
        }
        this.mWorkHandler.post(new Runnable() { // from class: com.miui.server.security.AppBehaviorService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppBehaviorService.this.lambda$recordAppBehaviorAsync$0(appBehavior);
            }
        });
    }

    /* renamed from: recordAppBehavior, reason: merged with bridge method [inline-methods] */
    public void lambda$recordAppBehaviorAsync$0(AppBehavior appBehavior) {
        ApplicationInfo callerPkg;
        int behaviorType;
        if (appBehavior == null || !appBehavior.isValid() || (callerPkg = getApplicationInfo(appBehavior.getCallerPkg(), 0)) == null || (behaviorType = AppBehavior.getBehaviorType(appBehavior.getBehavior())) == 0) {
            return;
        }
        if (behaviorType == 2) {
            ApplicationInfo calleePkg = getApplicationInfo(appBehavior.getCalleePkg(), 0);
            if (calleePkg != null && appBehavior.getBehavior() == 2) {
                boolean callerSystem = isSystem(callerPkg);
                boolean calleeSystem = isSystem(calleePkg);
                if (callerSystem && calleeSystem) {
                    return;
                }
                if (callerSystem != calleeSystem) {
                    String willBeCheckPkg = callerSystem ? appBehavior.getCalleePkg() : appBehavior.getCallerPkg();
                    if (canRecordBehavior(28, willBeCheckPkg)) {
                        scheduleRecord(28, appBehavior.getCallerPkg(), appBehavior.getChain(), appBehavior.getCount());
                        return;
                    }
                }
                if (canRecordBehavior(4, appBehavior.getCallerPkg()) || canRecordBehavior(3, appBehavior.getCalleePkg())) {
                    scheduleRecord(4, appBehavior.getCallerPkg(), appBehavior.getChain(), appBehavior.getCount());
                    return;
                }
                return;
            }
            return;
        }
        if ((behaviorType == 1 || behaviorType == 3 || behaviorType == 4) && canRecordBehavior(appBehavior.getBehavior(), appBehavior.getCallerPkg())) {
            scheduleRecord(appBehavior.getBehavior(), appBehavior.getCallerPkg(), appBehavior.getExtra(), appBehavior.getCount());
        }
    }

    private void scheduleRecord(int behavior, String willBeRecordPackageName, String willBeRecordData, long willBeRecordCount) {
        synchronized (this.mUidBehaviorLock) {
            PackageBehaviorBean packageBehaviorBean = this.mPackageBehavior.get(willBeRecordPackageName);
            if (packageBehaviorBean == null) {
                packageBehaviorBean = new PackageBehaviorBean();
                this.mPackageBehavior.put(willBeRecordPackageName, packageBehaviorBean);
            }
            boolean addCount = packageBehaviorBean.recordAppBehavior(behavior, willBeRecordData, willBeRecordCount);
            if (addCount) {
                this.mRecordSize++;
            }
            if (this.mRecordSize >= MAX_CACHE_COUNT) {
                persistBehaviorRecord();
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("==================Dump AppBehaviorService start==================");
        synchronized (this.mRecordEnableLock) {
            pw.println("AlwaysRecordBehavior :");
            for (int i = 0; i < this.mAlwaysRecordBehavior.size(); i++) {
                pw.println("    behavior:" + AppBehavior.behaviorToName(this.mAlwaysRecordBehavior.keyAt(i)) + " include system:" + this.mAlwaysRecordBehavior.valueAt(i));
            }
            pw.println("PackageRecordBehavior :");
            for (int i2 = 0; i2 < this.mPackageRecordBehavior.size(); i2++) {
                Set<Integer> watchingBehavior = this.mPackageRecordBehavior.valueAt(i2);
                if (watchingBehavior != null && watchingBehavior.size() > 0) {
                    Iterator<Integer> it = watchingBehavior.iterator();
                    while (it.hasNext()) {
                        int behavior = it.next().intValue();
                        pw.println("    package:" + this.mPackageRecordBehavior.keyAt(i2) + " behavior:" + AppBehavior.behaviorToName(behavior));
                    }
                }
            }
        }
        synchronized (this.mUidBehaviorLock) {
            pw.println("dump current cache size:" + this.mRecordSize);
            for (int i3 = 0; i3 < this.mPackageBehavior.size(); i3++) {
                pw.println("dump pkg:" + this.mPackageBehavior.keyAt(i3));
                ArrayMap<String, PackageBehaviorBean> arrayMap = this.mPackageBehavior;
                PackageBehaviorBean packageBehaviorBean = arrayMap.get(arrayMap.keyAt(i3));
                if (packageBehaviorBean != null) {
                    packageBehaviorBean.dump(pw);
                }
            }
            pw.println("==================Dump AppBehaviorService end==================");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ApplicationInfo getApplicationInfo(String pkgName, int userId) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            ApplicationInfo info = this.mPackageManagerInt.getApplicationInfo(pkgName, 0L, 1000, userId);
            return info;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public static boolean isSystem(ApplicationInfo applicationInfo) {
        return applicationInfo != null && (applicationInfo.isSystemApp() || UserHandle.getAppId(applicationInfo.uid) < 10000);
    }

    public void setAppBehaviorDuration(AppDurationService appDurationService) {
        this.mAppBehaviorDuration = appDurationService;
    }

    private void registerListenerForBehavior(int behavior, boolean register) {
        if (35 == behavior) {
            registerForegroundRecordTask(register);
            return;
        }
        if (29 == behavior) {
            registerAccessibilityListener(register);
        } else if (32 == behavior || 22 == behavior || 23 == behavior || 25 == behavior) {
            registerScanDurationTooLong(register);
        }
    }

    private void registerForegroundRecordTask(boolean register) {
        if (register) {
            if (!this.mRegisterForeground) {
                this.mWorkHandler.sendEmptyMessageDelayed(MSG_FOREGROUND_CHECK, SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED);
                ProcessManager.registerForegroundInfoListener(this.mForegroundInfoListener);
                this.mRegisterForeground = true;
                return;
            }
            return;
        }
        if (this.mRegisterForeground) {
            this.mForegroundRecorder.clear();
            this.mWorkHandler.removeMessages(MSG_FOREGROUND_CHECK);
            ProcessManager.unregisterForegroundInfoListener(this.mForegroundInfoListener);
            this.mRegisterForeground = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkForegroundCount() {
        Slog.i(TAG, "start checkForegroundPackageRecord with threshold");
        synchronized (this.mForegroundRecorder) {
            for (int i = 0; i < this.mForegroundRecorder.size(); i++) {
                if (moreThanThreshold(35, this.mForegroundRecorder.valueAt(i).intValue())) {
                    recordAppBehaviorAsync(AppBehavior.buildCountEvent(35, this.mForegroundRecorder.keyAt(i), 1L, (String) null));
                }
            }
            this.mForegroundRecorder.clear();
        }
        this.mWorkHandler.sendEmptyMessageDelayed(MSG_FOREGROUND_CHECK, SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED);
    }

    private void registerAccessibilityListener(boolean register) {
        if (register && !this.mRegisterAccessibility) {
            this.mRegisterAccessibility = true;
            this.mContext.getContentResolver().registerContentObserver(ACCESSIBILITY_URI, false, this.mSettingsObserver);
            checkNewEnableAccessibility();
        } else if (this.mRegisterAccessibility && !register) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            this.mLastEnableAccessibility.clear();
            this.mRegisterAccessibility = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkNewEnableAccessibility() {
        ComponentName service;
        String components = Settings.Secure.getString(this.mContext.getContentResolver(), "enabled_accessibility_services");
        if (!TextUtils.isEmpty(components)) {
            String[] componentsArray = components.split(COMPONENT_NAME_SEPARATOR);
            for (String component : componentsArray) {
                boolean alreadyHave = this.mLastEnableAccessibility.contains(component);
                if (!alreadyHave && (service = ComponentName.unflattenFromString(component)) != null) {
                    recordAppBehaviorAsync(AppBehavior.buildCountEvent(29, service.getPackageName(), 1L, service.flattenToShortString()));
                }
            }
            this.mLastEnableAccessibility.clear();
            this.mLastEnableAccessibility.addAll(Arrays.asList(componentsArray));
            return;
        }
        this.mLastEnableAccessibility.clear();
    }

    private void registerScanDurationTooLong(boolean register) {
        if (register && !this.mWorkHandler.hasMessages(MSG_DURATION_TOO_LONG_CHECK)) {
            this.mWorkHandler.sendEmptyMessageDelayed(MSG_DURATION_TOO_LONG_CHECK, 43200000L);
        } else if (!register) {
            this.mWorkHandler.removeMessages(MSG_DURATION_TOO_LONG_CHECK);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanDurationTooLong() {
        this.mWorkHandler.removeMessages(MSG_DURATION_TOO_LONG_CHECK);
        this.mWorkHandler.sendEmptyMessageDelayed(MSG_DURATION_TOO_LONG_CHECK, 43200000L);
        this.mAppBehaviorDuration.checkIfTooLongDuration();
    }
}
