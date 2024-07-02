package com.xiaomi.abtest.d;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class c {
    private static ThreadPoolExecutor a;
    private static int b = Runtime.getRuntime().availableProcessors() + 1;

    static {
        int i = b;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(i, i, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        a = threadPoolExecutor;
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public static void a(Runnable runnable) {
        try {
            a.execute(runnable);
        } catch (Throwable th) {
        }
    }
}
