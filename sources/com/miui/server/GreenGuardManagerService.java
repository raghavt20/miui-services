package com.miui.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.ProcessUtils;
import com.miui.server.GreenGuardManagerService;

/* loaded from: classes.dex */
public class GreenGuardManagerService {
    public static final String GREEN_KID_AGENT_PKG_NAME = "com.miui.greenguard";
    public static final String GREEN_KID_SERVICE = "com.miui.greenguard.service.GreenKidService";
    private static final String TAG = "GreenKidManagerService";
    private static ServiceConnection mGreenGuardServiceConnection;

    public static void init(Context context) {
        if (!isGreenKidActive(context) && !isGreenKidNeedWipe(context)) {
            disableAgentProcess(context);
        } else {
            mGreenGuardServiceConnection = new GreenGuardServiceConn(context);
        }
    }

    private static void disableAgentProcess(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            if (packageManager.getApplicationInfo(GREEN_KID_AGENT_PKG_NAME, 8192) != null) {
                packageManager.setApplicationEnabledSetting(GREEN_KID_AGENT_PKG_NAME, 2, 0);
                Slog.i(TAG, "Disable GreenGuard agent : [ com.miui.greenguard] .");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Disable greenGuard agent : [ com.miui.greenguard] failed , package not install", e);
        }
    }

    private static boolean isGreenKidActive(Context context) {
        return MiuiSettings.Secure.isGreenKidActive(context.getContentResolver());
    }

    private static boolean isGreenKidNeedWipe(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_url_green_guard_sdk_need_clear_data", 0) == 1;
    }

    public static void startWatchGreenGuardProcess(Context context) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        String callingPackageName = ProcessUtils.getPackageNameByPid(callingPid);
        if (UserHandle.getAppId(callingUid) != 1000 && !GREEN_KID_AGENT_PKG_NAME.equals(callingPackageName)) {
            String msg = "Permission Denial from pid=" + callingPid + ", uid=" + callingUid + "callingPkg:" + (callingPackageName == null ? "" : callingPackageName);
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        startWatchGreenGuardProcessInner(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized void startWatchGreenGuardProcessInner(Context context) {
        synchronized (GreenGuardManagerService.class) {
            Log.d(TAG, "startWatchGreenGuardProcess");
            Intent intent = new Intent();
            intent.setClassName(GREEN_KID_AGENT_PKG_NAME, GREEN_KID_SERVICE);
            context.bindService(intent, mGreenGuardServiceConnection, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GreenGuardServiceConn implements ServiceConnection {
        private final Context mContext;

        public GreenGuardServiceConn(Context context) {
            this.mContext = context;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(GreenGuardManagerService.TAG, "On GreenKidService Connected");
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.d(GreenGuardManagerService.TAG, "On GreenKidService Disconnected , schedule restart it in 10s.");
            BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.miui.server.GreenGuardManagerService$GreenGuardServiceConn$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GreenGuardManagerService.GreenGuardServiceConn.this.lambda$onServiceDisconnected$0();
                }
            }, 10000L);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceDisconnected$0() {
            GreenGuardManagerService.startWatchGreenGuardProcessInner(this.mContext);
        }
    }
}
