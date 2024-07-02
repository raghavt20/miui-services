package com.android.server.tof;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.os.BackgroundThread;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class AppStatusManager {
    private static final String GESTURE_DISABLED_APPS_KEY = "gesture_disabled_apps_";
    private static final String GESTURE_ENABLED_APPS_KEY = "gesture_enabled_apps_";
    private static HashMap<String, AppStatusManager> instances = new HashMap<>();
    private Set<String> mCachedDisabledApps;
    private Set<String> mCachedEnabledApps;
    private Context mContext;
    private String mUserId;

    private AppStatusManager(String userId, Context context) {
        this.mUserId = userId;
        this.mContext = context;
        init();
    }

    public synchronized void addDisabledApp(String appPackageName) {
        if (!TextUtils.isEmpty(appPackageName) && this.mCachedDisabledApps.add(appPackageName)) {
            saveDisabledAppsAsync();
        }
    }

    public synchronized void removeDisabledApp(String appPackageName) {
        if (!TextUtils.isEmpty(appPackageName) && this.mCachedDisabledApps.remove(appPackageName)) {
            saveDisabledAppsAsync();
        }
    }

    public synchronized void addEnabledApp(String appPackageName) {
        if (!TextUtils.isEmpty(appPackageName) && this.mCachedEnabledApps.add(appPackageName)) {
            saveEnabledAppsAsync();
        }
    }

    public synchronized void removeEnabledApp(String appPackageName) {
        if (!TextUtils.isEmpty(appPackageName) && this.mCachedEnabledApps.remove(appPackageName)) {
            saveEnabledAppsAsync();
        }
    }

    public void setEnable(String pkg, boolean enable) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        if (enable) {
            removeDisabledApp(pkg);
            addEnabledApp(pkg);
        } else {
            addDisabledApp(pkg);
            removeEnabledApp(pkg);
        }
    }

    public boolean isDisabled(String pkg) {
        return !TextUtils.isEmpty(pkg) && this.mCachedDisabledApps.contains(pkg);
    }

    public boolean isEnabled(String pkg) {
        return !TextUtils.isEmpty(pkg) && this.mCachedEnabledApps.contains(pkg);
    }

    private void init() {
        this.mCachedDisabledApps = new HashSet();
        this.mCachedEnabledApps = new HashSet();
        String disableApps = Settings.Secure.getString(this.mContext.getContentResolver(), GESTURE_DISABLED_APPS_KEY + this.mUserId);
        if (!TextUtils.isEmpty(disableApps)) {
            this.mCachedDisabledApps.addAll(Arrays.asList(TextUtils.split(disableApps, ",")));
        }
        String enableApps = Settings.Secure.getString(this.mContext.getContentResolver(), GESTURE_ENABLED_APPS_KEY + this.mUserId);
        if (!TextUtils.isEmpty(enableApps)) {
            this.mCachedEnabledApps.addAll(Arrays.asList(TextUtils.split(enableApps, ",")));
        }
    }

    private void saveDisabledAppsAsync() {
        BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.tof.AppStatusManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppStatusManager.this.lambda$saveDisabledAppsAsync$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$saveDisabledAppsAsync$0() {
        String appsString = TextUtils.join(",", this.mCachedDisabledApps);
        Settings.Secure.putString(this.mContext.getContentResolver(), GESTURE_DISABLED_APPS_KEY + this.mUserId, appsString);
    }

    private void saveEnabledAppsAsync() {
        BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.tof.AppStatusManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppStatusManager.this.lambda$saveEnabledAppsAsync$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$saveEnabledAppsAsync$1() {
        String appsString = TextUtils.join(",", this.mCachedEnabledApps);
        Settings.Secure.putString(this.mContext.getContentResolver(), GESTURE_ENABLED_APPS_KEY + this.mUserId, appsString);
    }

    public static boolean isAppDisabled(Context context, String pkg) {
        if (context == null) {
            return false;
        }
        AppStatusManager appStatusManager = getOrCreateInstance(context);
        return appStatusManager.isDisabled(pkg);
    }

    public static boolean isAppEnabled(Context context, String pkg) {
        if (context == null) {
            return false;
        }
        AppStatusManager appStatusManager = getOrCreateInstance(context);
        return appStatusManager.isEnabled(pkg);
    }

    public static boolean statusHasChanged(Context context, String pkg) {
        if (context == null) {
            return false;
        }
        return isAppDisabled(context, pkg) || isAppEnabled(context, pkg);
    }

    public static void setAppEnable(Context context, String pkg, boolean enable) {
        if (context == null || TextUtils.isEmpty(pkg)) {
            return;
        }
        AppStatusManager appStatusManager = getOrCreateInstance(context);
        appStatusManager.setEnable(pkg, enable);
    }

    private static AppStatusManager getOrCreateInstance(Context context) {
        String userId = String.valueOf(ActivityManager.getCurrentUser());
        if (instances.containsKey(userId + "")) {
            return instances.get(userId);
        }
        AppStatusManager appManager = new AppStatusManager(userId, context);
        instances.put(userId, appManager);
        return appManager;
    }
}
