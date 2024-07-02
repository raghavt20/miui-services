package com.android.server.notification;

import android.content.ComponentName;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class NotificationManagerServiceProxy {

    @MethodArguments(cls = {int.class, ComponentName.class})
    public static RefMethod allowNotificationListener;

    @MethodArguments
    public static RefMethod checkCallerIsSystem;

    static {
        RefClass.attach(NotificationManagerServiceProxy.class, "com.android.server.notification.NotificationManagerService");
    }
}
