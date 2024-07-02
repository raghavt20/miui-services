package com.android.server;

import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class SystemServerProxy {

    @MethodArguments(cls = {String.class, Throwable.class})
    public static RefMethod<Void> reportWtf;

    static {
        RefClass.attach(SystemServerProxy.class, "com.android.server.SystemServer");
    }
}
