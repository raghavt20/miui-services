package com.miui.server.enterprise;

import android.content.Context;
import android.net.ConnectivityManager;
import com.android.server.wm.WindowManagerService;

/* loaded from: classes.dex */
public class RestrictionManagerServiceProxy {
    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setScreenCaptureDisabled(WindowManagerService service, Context context, int userId, boolean enabled) {
        service.refreshScreenCaptureDisabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setWifiApEnabled(Context context, boolean enabled) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        connectivityManager.stopTethering(0);
        connectivityManager.stopTethering(1);
        connectivityManager.stopTethering(2);
    }
}
