package com.android.server;

import android.os.Handler;
import miui.os.Build;

/* loaded from: classes.dex */
public final class MiuiFgThread extends ServiceThread {
    private static final boolean IS_STABLE_VERSION = Build.IS_STABLE_VERSION;
    private static final long WATCHDOG_TIMEOUT = 120000;
    private static Handler sHandler;
    private static MiuiFgThread sInstance;

    private MiuiFgThread() {
        super("miui.fg", 0, true);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            MiuiFgThread miuiFgThread = new MiuiFgThread();
            sInstance = miuiFgThread;
            miuiFgThread.start();
            sHandler = new Handler(sInstance.getLooper());
            Watchdog.getInstance().addThread(sHandler, IS_STABLE_VERSION ? 600000L : WATCHDOG_TIMEOUT);
        }
    }

    public static MiuiFgThread get() {
        MiuiFgThread miuiFgThread;
        synchronized (MiuiFgThread.class) {
            ensureThreadLocked();
            miuiFgThread = sInstance;
        }
        return miuiFgThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (MiuiFgThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static void initialMiuiFgThread() {
        synchronized (MiuiFgThread.class) {
            ensureThreadLocked();
        }
    }
}
