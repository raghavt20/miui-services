package com.android.server.wm;

import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefObject;
import com.xiaomi.reflect.annotation.FieldArguments;

/* loaded from: classes.dex */
public class ActivityRecordProxy {

    @FieldArguments
    public static RefObject<Boolean> mOccludesParent;

    static {
        RefClass.attach(ActivityRecordProxy.class, "com.android.server.wm.ActivityRecord");
    }
}
