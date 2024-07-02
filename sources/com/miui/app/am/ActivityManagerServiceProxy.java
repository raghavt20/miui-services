package com.miui.app.am;

import com.android.server.am.ProcessRecord;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.RefObject;
import com.xiaomi.reflect.annotation.FieldArguments;
import com.xiaomi.reflect.annotation.MethodArguments;
import java.io.PrintWriter;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class ActivityManagerServiceProxy {

    @MethodArguments(cls = {PrintWriter.class, int.class, boolean.class, String[].class})
    public static RefMethod<ArrayList<ProcessRecord>> collectProcesses;

    @FieldArguments
    public static RefObject mProcLock;

    static {
        RefClass.attach(ActivityManagerServiceProxy.class, "com.android.server.am.ActivityManagerService");
    }
}
