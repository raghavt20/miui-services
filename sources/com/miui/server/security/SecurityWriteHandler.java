package com.miui.server.security;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import com.miui.server.SecurityManagerService;
import miui.security.SecurityManagerCompat;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class SecurityWriteHandler extends Handler {
    public static final int REMOVE_AC_PACKAGE = 4;
    private static final String TAG = "SecurityWriteHandler";
    public static final int WRITE_BOOT_TIME = 3;
    public static final int WRITE_SETTINGS = 1;
    public static final int WRITE_WAKE_UP_TIME = 2;
    private final Context mContext;
    private final SecurityManagerService mService;

    public SecurityWriteHandler(SecurityManagerService service, Looper looper) {
        super(looper);
        this.mService = service;
        this.mContext = service.mContext;
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                Process.setThreadPriority(0);
                synchronized (this.mService.mSettingsFile) {
                    removeMessages(1);
                    this.mService.writeSettings();
                }
                Process.setThreadPriority(10);
                return;
            case 2:
                Process.setThreadPriority(0);
                removeMessages(2);
                this.mService.mWakeUpTimeImpl.writeWakeUpTime();
                Process.setThreadPriority(10);
                return;
            case 3:
                Process.setThreadPriority(0);
                removeMessages(3);
                if (FeatureParser.hasFeature("vendor", 3)) {
                    String vendor2 = FeatureParser.getString("vendor");
                    long wakeTime = ((Long) msg.obj).longValue();
                    SecurityManagerCompat.writeBootTime(this.mContext, vendor2, wakeTime);
                    Log.d(TAG, "Wake up time updated " + wakeTime);
                } else {
                    Log.w(TAG, "There is no corresponding feature!");
                }
                Process.setThreadPriority(10);
                return;
            case 4:
                synchronized (this.mService.mUserStateLock) {
                    int userId = msg.arg1;
                    String packageName = (String) msg.obj;
                    SecurityUserState userState = this.mService.getUserStateLocked(userId);
                    userState.mAccessControlCanceled.remove(packageName);
                }
                return;
            default:
                return;
        }
    }
}
