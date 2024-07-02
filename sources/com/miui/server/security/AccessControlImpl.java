package com.miui.server.security;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.am.ProcessUtils;
import com.android.server.wm.WindowProcessUtils;
import com.miui.server.AccessController;
import com.miui.server.SecurityManagerService;
import com.xiaomi.abtest.d.d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import miui.security.SecurityManager;
import miui.security.SecurityManagerCompat;
import miui.securityspace.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AccessControlImpl {
    private static final String APPLOCK_MASK_NOTIFY = "applock_mask_notify";
    public static final long LOCK_TIME_OUT = 60000;
    private static final String TAG = "AccessControlImpl";
    private final Context mContext;
    private final Handler mHandler;
    private final SecurityManagerService mService;

    public AccessControlImpl(SecurityManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mHandler = service.mSecurityWriteHandler;
    }

    public void removeAccessControlPassAsUser(String packageName, int userId) {
        HashMap<String, Object> topActivity;
        String pkgName = null;
        IBinder token = null;
        Integer activityUserId = 0;
        boolean checkAccessControlPass = false;
        if (userId != -1) {
            topActivity = null;
        } else {
            HashMap<String, Object> topActivity2 = WindowProcessUtils.getTopRunningActivityInfo();
            topActivity = topActivity2;
        }
        synchronized (this.mService.mUserStateLock) {
            if (userId == -1) {
                int size = this.mService.mUserStates.size();
                for (int i = 0; i < size; i++) {
                    SecurityUserState userState = this.mService.mUserStates.valueAt(i);
                    removeAccessControlPassLocked(userState, packageName);
                }
                int currentUserId = ProcessUtils.getCurrentUserId();
                SecurityUserState userState2 = this.mService.getUserStateLocked(currentUserId);
                boolean enabled = getAccessControlEnabledLocked(userState2);
                if (!enabled) {
                    return;
                }
                if (topActivity != null) {
                    pkgName = (String) topActivity.get("packageName");
                    token = (IBinder) topActivity.get("token");
                    activityUserId = (Integer) topActivity.get("userId");
                    checkAccessControlPass = checkAccessControlPassLocked(pkgName, null, activityUserId.intValue());
                }
            } else {
                SecurityUserState userState3 = this.mService.getUserStateLocked(userId);
                removeAccessControlPassLocked(userState3, packageName);
            }
            if (userId == -1 && topActivity != null && !checkAccessControlPass) {
                try {
                    Intent intent = SecurityManager.getCheckAccessIntent(true, pkgName, (Intent) null, -1, true, activityUserId.intValue(), (Bundle) null);
                    intent.putExtra("miui.KEYGUARD_LOCKED", true);
                    SecurityManagerCompat.startActvityAsUser(this.mContext, (IApplicationThread) null, token, (String) null, intent, activityUserId.intValue());
                } catch (Exception e) {
                    Log.e(TAG, "removeAccessControlPassAsUser startActivityAsUser error ", e);
                }
            }
        }
    }

    public int activityResume(Intent intent) {
        ComponentName componentName;
        String packageName;
        if (intent == null || (componentName = intent.getComponent()) == null || (packageName = componentName.getPackageName()) == null) {
            return 0;
        }
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            boolean enabled = getAccessControlEnabledLocked(userState);
            if (!enabled) {
                return 0;
            }
            int packageUid = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(packageName, 0L, userId);
            if (callingUid != packageUid) {
                return 0;
            }
            int lockMode = getAccessControlLockMode(userState);
            String oldResumePackage = userState.mLastResumePackage;
            userState.mLastResumePackage = packageName;
            HashSet<String> passPackages = userState.mAccessControlPassPackages;
            if (lockMode == 2 && oldResumePackage != null && passPackages.contains(oldResumePackage)) {
                userState.mAccessControlLastCheck.put(oldResumePackage, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userId);
            }
            SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, packageName);
            if (ps.accessControl) {
                int result = 1 | 2;
                if (passPackages.contains(packageName)) {
                    if (lockMode == 2) {
                        Long lastTime = userState.mAccessControlLastCheck.get(packageName);
                        if (lastTime != null) {
                            long realtime = SystemClock.elapsedRealtime();
                            if (realtime - lastTime.longValue() < LOCK_TIME_OUT) {
                                return result | 4;
                            }
                        }
                        passPackages.remove(packageName);
                        updateMaskObserverValues();
                    } else {
                        int result2 = result | 4;
                        if (lockMode == 0) {
                            clearPassPackages(userId);
                            passPackages.add(packageName);
                            updateMaskObserverValues();
                        }
                        return result2;
                    }
                }
                if (lockMode == 0) {
                    clearPassPackages(userId);
                }
                if (userState.mAccessControlCanceled.contains(packageName)) {
                    return result | 8;
                }
                if ((lockMode == 1 && getAccessControlLockConvenient(userState) && isPackageAccessControlPass(userState)) || this.mService.mAccessController.skipActivity(intent, packageName) || this.mService.mAccessController.filterIntentLocked(true, packageName, intent)) {
                    result |= 4;
                }
                return result;
            }
            if (lockMode == 0) {
                clearPassPackages(userId);
            }
            return 1;
        }
    }

    public void addAccessControlPassForUser(String packageName, int userId) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            int lockMode = getAccessControlLockMode(userState);
            if (lockMode == 2) {
                userState.mAccessControlLastCheck.put(packageName, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userId);
            } else {
                updateMaskObserverValues();
            }
            userState.mAccessControlPassPackages.add(packageName);
        }
    }

    public boolean getApplicationAccessControlEnabledLocked(String packageName, int userId) {
        boolean z;
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, packageName);
                z = ps.accessControl;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setApplicationAccessControlEnabledForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userStateLocked = this.mService.getUserStateLocked(userId);
            SecurityPackageSettings ps = this.mService.getPackageSetting(userStateLocked.mPackages, packageName);
            ps.accessControl = enabled;
            this.mService.scheduleWriteSettings();
            updateMaskObserverValues();
        }
    }

    public boolean checkAccessControlPassLocked(String packageName, Intent intent, int userId) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, packageName);
            if (!ps.accessControl) {
                return true;
            }
            return checkAccessControlPassLockedCore(userState, packageName, intent);
        }
    }

    public String getShouldMaskApps() {
        String jSONArray;
        synchronized (this.mService.mUserStateLock) {
            try {
                try {
                    JSONArray jSONArray2 = new JSONArray();
                    for (int i = 0; i < this.mService.mUserStates.size(); i++) {
                        SecurityUserState userState = this.mService.mUserStates.valueAt(i);
                        JSONObject userStateObj = new JSONObject();
                        userStateObj.put("userId", userState.userHandle);
                        JSONArray itemArray = new JSONArray();
                        boolean enabled = userState.mAccessControlEnabled;
                        if (userState.userHandle == 999) {
                            enabled = this.mService.getUserStateLocked(0).mAccessControlEnabled;
                        }
                        if (enabled) {
                            for (SecurityPackageSettings ps : userState.mPackages.values()) {
                                if (!TextUtils.isEmpty(ps.name) && ps.accessControl && ps.maskNotification && !checkAccessControlPassLockedCore(userState, ps.name, null)) {
                                    itemArray.put(ps.name);
                                }
                            }
                        }
                        userStateObj.put("shouldMaskApps", itemArray);
                        jSONArray2.put(userStateObj);
                    }
                    jSONArray = jSONArray2.toString();
                } catch (Exception e) {
                    Log.e(TAG, "getShouldMaskApps failed. ", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return jSONArray;
    }

    public void updateMaskObserverValues() {
        long origId = Binder.clearCallingIdentity();
        try {
            int oldValue = Settings.Secure.getInt(this.mContext.getContentResolver(), APPLOCK_MASK_NOTIFY, 0);
            Settings.Secure.putInt(this.mContext.getContentResolver(), APPLOCK_MASK_NOTIFY, oldValue ^ 1);
        } catch (Exception e) {
            Log.e(TAG, "write setting secure failed.", e);
        }
        Binder.restoreCallingIdentity(origId);
    }

    public boolean getApplicationMaskNotificationEnabledLocked(String packageName, int userId) {
        boolean z;
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, packageName);
                z = ps.maskNotification;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setApplicationMaskNotificationEnabledForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userStateLocked = this.mService.getUserStateLocked(userId);
            SecurityPackageSettings ps = this.mService.getPackageSetting(userStateLocked.mPackages, packageName);
            ps.maskNotification = enabled;
            this.mService.scheduleWriteSettings();
            updateMaskObserverValues();
        }
    }

    public boolean needFinishAccessControl(IBinder token) {
        Intent intent;
        ComponentName component;
        ArrayList<Intent> taskIntent = WindowProcessUtils.getTaskIntentForToken(token);
        if (taskIntent != null && taskIntent.size() > 1 && (component = (intent = taskIntent.get(1)).getComponent()) != null) {
            return this.mService.mAccessController.filterIntentLocked(true, component.getPackageName(), intent);
        }
        return false;
    }

    public void finishAccessControl(String packageName, int userId) {
        if (packageName == null) {
            return;
        }
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            userState.mAccessControlCanceled.add(packageName);
            Message msg = this.mHandler.obtainMessage(4);
            msg.arg1 = userId;
            msg.obj = packageName;
            this.mHandler.sendMessageDelayed(msg, 500L);
        }
    }

    public boolean getAccessControlEnabledLocked(SecurityUserState userState) {
        SecurityUserState transferUserState = changeUserState(userState);
        this.mService.mSettingsObserver.initAccessControlSettingsLocked(transferUserState);
        return transferUserState.mAccessControlEnabled;
    }

    private boolean checkAccessControlPassLockedCore(SecurityUserState userState, String packageName, Intent intent) {
        int lockMode = getAccessControlLockMode(userState);
        boolean pass = userState.mAccessControlPassPackages.contains(packageName);
        if (pass && lockMode == 2) {
            Long lastTime = userState.mAccessControlLastCheck.get(packageName);
            if (lastTime != null) {
                long realtime = SystemClock.elapsedRealtime();
                if (realtime - lastTime.longValue() > LOCK_TIME_OUT) {
                    pass = false;
                }
            }
            if (pass && !AccessController.PACKAGE_SYSTEMUI.equals(ProcessUtils.getPackageNameByPid(Binder.getCallingPid()))) {
                userState.mAccessControlLastCheck.put(packageName, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userState.userHandle);
            }
        }
        if (!pass && lockMode == 1 && getAccessControlLockConvenient(userState) && isPackageAccessControlPass(userState)) {
            pass = true;
        }
        if (!pass && this.mService.mAccessController.skipActivity(intent, ProcessUtils.getPackageNameByPid(Binder.getCallingPid()))) {
            pass = true;
        }
        if (!pass && this.mService.mAccessController.filterIntentLocked(true, packageName, intent)) {
            return true;
        }
        return pass;
    }

    private boolean isPackageAccessControlPass(SecurityUserState userState) {
        if (!ConfigUtils.isSupportXSpace() || (userState.userHandle != 999 && userState.userHandle != 0)) {
            return userState.mAccessControlPassPackages.size() > 0;
        }
        SecurityUserState userStateOwner = this.mService.getUserStateLocked(0);
        SecurityUserState userStateXSpace = this.mService.getUserStateLocked(999);
        return userStateOwner.mAccessControlPassPackages.size() + userStateXSpace.mAccessControlPassPackages.size() > 0;
    }

    private void removeAccessControlPassLocked(SecurityUserState userState, String packageName) {
        if ("*".equals(packageName)) {
            userState.mAccessControlPassPackages.clear();
            userState.mAccessControlLastCheck.clear();
        } else {
            userState.mAccessControlPassPackages.remove(packageName);
        }
        updateMaskObserverValues();
    }

    private int getAccessControlLockMode(SecurityUserState userState) {
        SecurityUserState transferUserState = changeUserState(userState);
        this.mService.mSettingsObserver.initAccessControlSettingsLocked(transferUserState);
        return transferUserState.mAccessControlLockMode;
    }

    private boolean getAccessControlLockConvenient(SecurityUserState userState) {
        SecurityUserState transferUserState = changeUserState(userState);
        this.mService.mSettingsObserver.initAccessControlSettingsLocked(transferUserState);
        return transferUserState.mAccessControlLockConvenient;
    }

    private SecurityUserState changeUserState(SecurityUserState userState) {
        int userId = SecurityManager.getUserHandle(userState.userHandle);
        return this.mService.getUserStateLocked(userId);
    }

    private void scheduleForMaskObserver(String pkg, int userHandle) {
        String token = pkg + d.h + userHandle;
        this.mHandler.removeCallbacksAndEqualMessages(token);
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.security.AccessControlImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AccessControlImpl.this.updateMaskObserverValues();
            }
        }, token, LOCK_TIME_OUT);
    }

    private void clearPassPackages(int userId) {
        if (ConfigUtils.isSupportXSpace() && (userId == 0 || 999 == userId)) {
            SecurityUserState userStateOwner = this.mService.getUserStateLocked(0);
            SecurityUserState userStateXSpace = this.mService.getUserStateLocked(999);
            HashSet<String> passPackagesOwner = userStateOwner.mAccessControlPassPackages;
            HashSet<String> passPackagesXSpace = userStateXSpace.mAccessControlPassPackages;
            passPackagesOwner.clear();
            passPackagesXSpace.clear();
        } else {
            HashSet<String> passPackages = this.mService.getUserStateLocked(userId).mAccessControlPassPackages;
            passPackages.clear();
        }
        updateMaskObserverValues();
    }
}
