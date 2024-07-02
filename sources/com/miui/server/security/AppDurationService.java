package com.miui.server.security;

import android.app.ActivityManager;
import android.app.IForegroundServiceObserver;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import java.util.Objects;
import miui.security.AppBehavior;

/* loaded from: classes.dex */
public class AppDurationService {
    private static final String TAG = "AppDurationService";
    private final AppBehaviorService appBehavior;
    private final Object mDurationLock = new Object();
    private final ArrayMap<UserPackage, UserPackageBehaviorDuration> userPackageStartTimeAndTokenArrayMap = new ArrayMap<>();

    public AppDurationService(final AppBehaviorService appBehavior) {
        this.appBehavior = appBehavior;
        try {
            IForegroundServiceObserver.Stub foregroundServiceObserver = new IForegroundServiceObserver.Stub() { // from class: com.miui.server.security.AppDurationService.1
                public void onForegroundStateChanged(IBinder serviceToken, String packageName, int userId, boolean isForeground) {
                    int uid = appBehavior.mPackageManagerInt.getPackageUid(packageName, 0L, userId);
                    AppDurationService.this.onDurationEvent(25, serviceToken, uid, packageName, isForeground);
                }
            };
            ActivityManager.getService().registerForegroundServiceObserver(foregroundServiceObserver);
        } catch (RemoteException exception) {
            Slog.e(TAG, "AppDurationService registerForegroundServiceObserver", exception);
        }
    }

    public void onDurationEvent(int behavior, IBinder binder, int uid, String pkgName, boolean start) {
        if (!this.appBehavior.canRecordBehavior(behavior, pkgName)) {
            return;
        }
        UserPackage userPackage = new UserPackage(uid, pkgName);
        synchronized (this.mDurationLock) {
            UserPackageBehaviorDuration userPackageBehaviorDuration = this.userPackageStartTimeAndTokenArrayMap.get(userPackage);
            if (start || userPackageBehaviorDuration != null) {
                if (userPackageBehaviorDuration == null) {
                    userPackageBehaviorDuration = new UserPackageBehaviorDuration();
                    this.userPackageStartTimeAndTokenArrayMap.put(userPackage, userPackageBehaviorDuration);
                }
                if (start) {
                    userPackageBehaviorDuration.recordBehavior(behavior, binder, uid);
                } else {
                    long duration = userPackageBehaviorDuration.stopRecordDuration(behavior, binder, uid);
                    if (duration > 0) {
                        this.appBehavior.recordAppBehaviorAsync(AppBehavior.buildCountEvent(behavior, pkgName, duration, (String) null));
                    }
                }
            }
        }
    }

    public void removeDurationEvent(int behavior, int uid, String pkgName) {
        UserPackage userPackage = new UserPackage(uid, pkgName);
        synchronized (this.mDurationLock) {
            UserPackageBehaviorDuration userPackageBehaviorDuration = this.userPackageStartTimeAndTokenArrayMap.get(userPackage);
            if (userPackageBehaviorDuration == null) {
                return;
            }
            if (userPackageBehaviorDuration.behaviorDurationMap != null) {
                userPackageBehaviorDuration.behaviorDurationMap.remove(behavior);
                if (userPackageBehaviorDuration.behaviorDurationMap.size() == 0) {
                    this.userPackageStartTimeAndTokenArrayMap.remove(userPackage);
                }
            }
        }
    }

    public void checkIfTooLongDuration() {
        UserPackageBehaviorDuration userPackageBehaviorDuration;
        UserPackageBehaviorDuration userPackageBehaviorDuration2;
        Slog.i(TAG, "checkIfTooLongDuration");
        synchronized (this.mDurationLock) {
            long current = SystemClock.elapsedRealtime();
            for (int i = 0; i < this.userPackageStartTimeAndTokenArrayMap.size(); i++) {
                String pkgName = this.userPackageStartTimeAndTokenArrayMap.keyAt(i).pkgName;
                UserPackageBehaviorDuration userPackageBehaviorDuration3 = this.userPackageStartTimeAndTokenArrayMap.valueAt(i);
                int j = 0;
                while (j < userPackageBehaviorDuration3.behaviorDurationMap.size()) {
                    int curBehavior = userPackageBehaviorDuration3.behaviorDurationMap.keyAt(j);
                    TokenAndStartTime tokenAndStartTime = (TokenAndStartTime) userPackageBehaviorDuration3.behaviorDurationMap.valueAt(j);
                    if (tokenAndStartTime.binderStartTimeMap == null || tokenAndStartTime.binderStartTimeMap.size() <= 0) {
                        userPackageBehaviorDuration = userPackageBehaviorDuration3;
                    } else {
                        int k = 0;
                        while (k < tokenAndStartTime.binderStartTimeMap.size()) {
                            long duration = current - ((Long) tokenAndStartTime.binderStartTimeMap.valueAt(k)).longValue();
                            boolean moreThanThreshold = this.appBehavior.moreThanThreshold(curBehavior, duration);
                            if (!moreThanThreshold) {
                                userPackageBehaviorDuration2 = userPackageBehaviorDuration3;
                            } else {
                                userPackageBehaviorDuration2 = userPackageBehaviorDuration3;
                                Slog.i(TAG, pkgName + ",behavior:" + curBehavior + " has running too long. record and restart");
                                tokenAndStartTime.binderStartTimeMap.setValueAt(k, Long.valueOf(current));
                                this.appBehavior.recordAppBehaviorAsync(AppBehavior.buildCountEvent(curBehavior, pkgName, duration, (String) null));
                            }
                            k++;
                            userPackageBehaviorDuration3 = userPackageBehaviorDuration2;
                        }
                        userPackageBehaviorDuration = userPackageBehaviorDuration3;
                    }
                    if (tokenAndStartTime.uidStartTimeMap != null && tokenAndStartTime.uidStartTimeMap.size() > 0) {
                        for (int k2 = 0; k2 < tokenAndStartTime.uidStartTimeMap.size(); k2++) {
                            long duration2 = current - ((Long) tokenAndStartTime.uidStartTimeMap.valueAt(k2)).longValue();
                            boolean moreThanThreshold2 = this.appBehavior.moreThanThreshold(curBehavior, duration2);
                            if (moreThanThreshold2) {
                                Slog.i(TAG, pkgName + ",behavior:" + curBehavior + " has running too long. record and restart");
                                tokenAndStartTime.uidStartTimeMap.setValueAt(k2, Long.valueOf(current));
                                this.appBehavior.recordAppBehaviorAsync(AppBehavior.buildCountEvent(curBehavior, pkgName, duration2, (String) null));
                            }
                        }
                    }
                    j++;
                    userPackageBehaviorDuration3 = userPackageBehaviorDuration;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UserPackage {
        private final String pkgName;
        private final int uid;

        public UserPackage(int uid, String pkgName) {
            this.uid = uid;
            this.pkgName = pkgName;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserPackage that = (UserPackage) o;
            if (this.uid == that.uid && Objects.equals(this.pkgName, that.pkgName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.uid), this.pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UserPackageBehaviorDuration {
        private SparseArray<TokenAndStartTime> behaviorDurationMap;

        private UserPackageBehaviorDuration() {
        }

        public boolean recordBehavior(int behavior, IBinder binder, int uid) {
            if (this.behaviorDurationMap == null) {
                this.behaviorDurationMap = new SparseArray<>();
            }
            TokenAndStartTime tokenAndStartTime = this.behaviorDurationMap.get(behavior);
            if (tokenAndStartTime == null) {
                tokenAndStartTime = new TokenAndStartTime();
                this.behaviorDurationMap.put(behavior, tokenAndStartTime);
            }
            if (binder != null) {
                return tokenAndStartTime.recordBinderTime(binder);
            }
            return tokenAndStartTime.recordUidTime(uid);
        }

        public long stopRecordDuration(int behavior, IBinder binder, int uid) {
            TokenAndStartTime tokenAndStartTime;
            SparseArray<TokenAndStartTime> sparseArray = this.behaviorDurationMap;
            if (sparseArray == null || sparseArray.size() == 0 || (tokenAndStartTime = this.behaviorDurationMap.get(behavior)) == null) {
                return 0L;
            }
            return binder != null ? tokenAndStartTime.stopBinderTime(binder) : tokenAndStartTime.stopUidTime(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TokenAndStartTime {
        private ArrayMap<IBinder, Long> binderStartTimeMap;
        private SparseArray<Long> uidStartTimeMap;

        private TokenAndStartTime() {
        }

        public boolean recordBinderTime(IBinder binder) {
            if (this.binderStartTimeMap == null) {
                this.binderStartTimeMap = new ArrayMap<>();
            }
            if (!this.binderStartTimeMap.containsKey(binder)) {
                this.binderStartTimeMap.put(binder, Long.valueOf(SystemClock.elapsedRealtime()));
                return true;
            }
            return false;
        }

        public long stopBinderTime(IBinder binder) {
            ArrayMap<IBinder, Long> arrayMap;
            if (binder == null || (arrayMap = this.binderStartTimeMap) == null || arrayMap.size() == 0) {
                return 0L;
            }
            long curTime = SystemClock.elapsedRealtime();
            long startTime = this.binderStartTimeMap.getOrDefault(binder, Long.valueOf(curTime)).longValue();
            this.binderStartTimeMap.remove(binder);
            return curTime - startTime;
        }

        public boolean recordUidTime(int uid) {
            if (this.uidStartTimeMap == null) {
                this.uidStartTimeMap = new SparseArray<>();
            }
            if (!this.uidStartTimeMap.contains(uid)) {
                this.uidStartTimeMap.put(uid, Long.valueOf(SystemClock.elapsedRealtime()));
                return true;
            }
            return false;
        }

        public long stopUidTime(int uid) {
            SparseArray<Long> sparseArray = this.uidStartTimeMap;
            if (sparseArray == null || sparseArray.size() == 0) {
                return 0L;
            }
            long curTime = SystemClock.elapsedRealtime();
            long startTime = this.uidStartTimeMap.get(uid, Long.valueOf(curTime)).longValue();
            this.uidStartTimeMap.remove(uid);
            return curTime - startTime;
        }
    }
}
