package com.android.server.wm;

import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class ImmersiveModeConfirmationProxy {

    @MethodArguments(cls = {int.class})
    public static RefMethod<Void> handleShow;

    static {
        RefClass.attach(ImmersiveModeConfirmationProxy.class, "com.android.server.wm.ImmersiveModeConfirmation");
    }
}
