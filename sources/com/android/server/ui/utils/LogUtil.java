package com.android.server.ui.utils;

import android.util.Log;

/* loaded from: classes.dex */
public class LogUtil {
    private static boolean DEBUG_ALL = true;

    public static void logD(String tag, String msg) {
        if (DEBUG_ALL) {
            Log.d(tag, msg);
        }
    }

    public static void logI(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void logW(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void logE(String tag, String msg) {
        Log.e(tag, msg);
    }
}
