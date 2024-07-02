package com.miui.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import java.util.ArrayList;
import java.util.List;
import miui.security.IAppRunningControlManager;

/* loaded from: classes.dex */
public class AppRunningControlService extends IAppRunningControlManager.Stub {
    private static final String TAG = "AppRunningControlService";
    private static final ArrayList<String> sNotDisallow;
    private final List<String> mAppsDisallowRunning = new ArrayList();
    private final Context mContext;
    private Intent mDisallowRunningAppIntent;
    private boolean mIsBlackListEnable;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sNotDisallow = arrayList;
        arrayList.add("com.lbe.security.miui");
        arrayList.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList.add("com.android.updater");
        arrayList.add("com.xiaomi.market");
        arrayList.add("com.xiaomi.finddevice");
        arrayList.add("com.miui.home");
    }

    public AppRunningControlService(Context context) {
        this.mContext = context;
    }

    public void setDisallowRunningList(List<String> list, Intent intent) {
        checkPermission();
        if (intent == null) {
            Slog.w(TAG, "setDisallowRunningList intent can't be null");
            return;
        }
        this.mDisallowRunningAppIntent = intent;
        this.mAppsDisallowRunning.clear();
        if (list == null || list.isEmpty()) {
            Slog.d(TAG, "setDisallowRunningList clear list.");
            return;
        }
        for (String pkgName : list) {
            if (!sNotDisallow.contains(pkgName)) {
                this.mAppsDisallowRunning.add(pkgName);
            }
        }
    }

    public void setBlackListEnable(boolean isEnable) {
        checkPermission();
        this.mIsBlackListEnable = isEnable;
    }

    public Intent getBlockActivityIntent(String packageName, Intent intent, boolean fromActivity, int requestCode) {
        if (!this.mIsBlackListEnable) {
            return null;
        }
        if (TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "getBlockActivityIntent packageName can't be null");
            return null;
        }
        if (!this.mAppsDisallowRunning.contains(packageName)) {
            return null;
        }
        Intent result = (Intent) this.mDisallowRunningAppIntent.clone();
        result.putExtra("packageName", packageName);
        if (intent != null) {
            if ((intent.getFlags() & 33554432) != 0) {
                result.addFlags(33554432);
            }
            result.addFlags(BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT);
            if (fromActivity) {
                if (requestCode >= 0) {
                    result.addFlags(33554432);
                }
            } else {
                result.addFlags(268435456);
            }
        }
        return result;
    }

    private boolean matchRuleInner(String pkgName, int wakeType) {
        if (this.mIsBlackListEnable && wakeType != 1) {
            return this.mAppsDisallowRunning.contains(pkgName);
        }
        return false;
    }

    public boolean isBlockActivity(Intent intent) {
        if (intent == null || this.mDisallowRunningAppIntent == null) {
            return false;
        }
        String action = intent.getAction();
        return TextUtils.equals(action, this.mDisallowRunningAppIntent.getAction());
    }

    private void checkPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String msg = "Permission Denial from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    public boolean matchRule(String pkgName, int wakeType) {
        return matchRuleInner(pkgName, wakeType);
    }

    public List<String> getNotDisallowList() {
        return sNotDisallow;
    }
}
