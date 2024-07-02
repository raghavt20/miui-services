package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.miui.AppOpsUtils;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IMessenger;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;

/* loaded from: classes.dex */
public final class PmInjector {
    private static final String PM = "Pm";
    public static final int RESULT_NO_RESPONSE = 10;
    public static final int STATUS_INVALID_APK = 3;
    public static final int STATUS_REJECT = -1;
    public static final int STATUS_SUCESS = 2;

    public static String statusToString(int status) {
        switch (status) {
            case -1:
                return "Install canceled by user";
            case 0:
            case 1:
            default:
                return "";
            case 2:
                return "Sucess";
            case 3:
                return "Invalid apk";
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v2, types: [java.lang.Object, com.android.server.pm.PmInjector$InstallObserver, android.os.IBinder] */
    public static int installVerify(int sessionId) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClassName(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.miui.permcenter.install.AdbInstallActivity");
        ?? installObserver = new InstallObserver();
        intent.putExtra("observer", (IBinder) installObserver);
        intent.putExtra("android.content.pm.extra.SESSION_ID", sessionId);
        intent.addFlags(402653184);
        try {
            IActivityManager am = ActivityManager.getService();
            int res = am.startActivity((IApplicationThread) null, "pm", intent, (String) null, (IBinder) null, (String) null, 0, 0, (ProfilerInfo) null, (Bundle) null);
            if (res != 0) {
                Log.e(PM, "start PackageInstallerActivity failed [" + res + "]");
                if (PackageManagerServiceImpl.sIsReleaseRom && PackageManagerServiceImpl.sIsBootLoaderLocked) {
                    if (isSecurityCenterExist()) {
                        return -1;
                    }
                }
                return 2;
            }
            synchronized (installObserver) {
                while (!installObserver.finished) {
                    try {
                        installObserver.wait(5000L);
                    } catch (InterruptedException e) {
                    }
                    if (installObserver.result == 10 && (!PackageManagerServiceImpl.sIsReleaseRom || !PackageManagerServiceImpl.sIsBootLoaderLocked)) {
                        return 2;
                    }
                    if (installObserver.result != 10) {
                        installObserver.finished = true;
                    } else {
                        installObserver.wait(ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION);
                        installObserver.finished = true;
                    }
                }
                if (installObserver.result == -1) {
                    return 2;
                }
                String msg = installObserver.msg;
                if (msg == null) {
                    msg = "Failure [INSTALL_CANCELED_BY_USER]";
                }
                Log.e(PM, "install msg : " + msg);
                return msg.contains("Invalid apk") ? !isSecurityCenterExist() ? 2 : 3 : !isSecurityCenterExist() ? 2 : -1;
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
            Log.e(PM, "start PackageInstallerActivity RemoteException");
            return (PackageManagerServiceImpl.sIsReleaseRom && PackageManagerServiceImpl.sIsBootLoaderLocked && isSecurityCenterExist()) ? -1 : 2;
        }
    }

    /* loaded from: classes.dex */
    public static class InstallObserver extends IMessenger.Stub {
        boolean finished;
        String msg;
        int result = 10;

        public void send(Message message) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = message.what;
                Bundle data = message.getData();
                if (data != null) {
                    this.msg = data.getString("msg");
                }
                notifyAll();
            }
        }
    }

    public static int getDefaultUserId() {
        return -1;
    }

    public static boolean isSecurityCenterExist() {
        if (!AppOpsUtils.isXOptMode()) {
            return true;
        }
        try {
            PackageInfo pi = AppGlobals.getPackageManager().getPackageInfo(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, 1L, UserHandle.myUserId());
            Log.d(PM, "checkSecurityCenterInstalled:getPackageInfo:true");
            if (pi != null) {
                return true;
            }
        } catch (Exception e) {
            Log.d(PM, "getPackageInfo error:" + e.toString());
        }
        Log.d(PM, "checkSecurityCenterInstalled:Exception:false");
        return false;
    }
}
