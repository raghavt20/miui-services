package com.miui.app.am;

import android.content.pm.ApplicationInfo;
import android.util.ArraySet;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.RefObject;
import com.xiaomi.reflect.annotation.FieldArguments;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class ProcessRecordProxy {

    @MethodArguments
    public static RefMethod<Integer> getPid;

    @MethodArguments
    public static RefMethod<ArraySet<String>> getPkgDeps;

    @FieldArguments
    public static RefObject<ApplicationInfo> info;

    static {
        RefClass.attach(ProcessRecordProxy.class, "com.android.server.am.ProcessRecord");
    }
}
