package com.android.server.inputmethod;

import android.content.Context;
import android.content.res.Configuration;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class DeviceUtils {
    private static final int SCREEN_TYPE_EXPAND = 0;
    private static final int SCREEN_TYPE_FOLD = 1;

    public static boolean isFlipTinyScreen(Context context) {
        Configuration configuration;
        return isFlipDevice() && (configuration = context.getResources().getConfiguration()) != null && configuration.getScreenType() == 1;
    }

    public static boolean isFlipDevice() {
        return MiuiMultiDisplayTypeInfo.isFlipDevice();
    }
}
