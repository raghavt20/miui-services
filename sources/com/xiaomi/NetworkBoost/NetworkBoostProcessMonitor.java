package com.xiaomi.NetworkBoost;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;

/* loaded from: classes.dex */
public class NetworkBoostProcessMonitor {
    private static String TAG = "NetworkBoostProcessMonitor";
    private Method method_getForegroundInfo;
    private Method method_registerForegroundInfoListener;
    private Method method_unregisterForegroundInfoListener;
    private Class<?> processManager;

    public NetworkBoostProcessMonitor(Context context) {
        Log.i(TAG, "NetworkBoostProcessMonitor");
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> loadClass = classLoader.loadClass("miui.process.ProcessManager");
            this.processManager = loadClass;
            if (loadClass != null) {
                Method method = loadClass.getMethod("getForegroundInfo", null);
                this.method_getForegroundInfo = method;
                method.setAccessible(true);
                this.method_registerForegroundInfoListener = this.processManager.getMethod("registerForegroundInfoListener", IForegroundInfoListener.class);
                this.method_unregisterForegroundInfoListener = this.processManager.getMethod("unregisterForegroundInfoListener", IForegroundInfoListener.class);
                this.method_registerForegroundInfoListener.setAccessible(true);
            } else {
                Log.e(TAG, "processManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "NetworkBoostProcessMonitor don't support ProcessManager" + e);
        }
    }

    public ForegroundInfo getForegroundInfo() {
        try {
            ForegroundInfo foregroundInfo = (ForegroundInfo) this.method_getForegroundInfo.invoke(null, null);
            return foregroundInfo;
        } catch (Exception e) {
            Log.e(TAG, "getForegroundInfo Exception" + e);
            return null;
        }
    }

    public void registerForegroundInfoListener(IForegroundInfoListener iForegroundInfoListener) {
        try {
            this.method_registerForegroundInfoListener.invoke(null, iForegroundInfoListener);
        } catch (Exception e) {
            Log.e(TAG, "registerForegroundInfoListener Exception" + e);
        }
    }

    public void unregisterForegroundInfoListener(IForegroundInfoListener iForegroundInfoListener) {
        try {
            this.method_unregisterForegroundInfoListener.invoke(null, iForegroundInfoListener);
        } catch (Exception e) {
            Log.e(TAG, "unregisterForegroundInfoListener Exception" + e);
        }
    }
}
