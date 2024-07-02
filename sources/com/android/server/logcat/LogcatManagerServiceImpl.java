package com.android.server.logcat;

import com.android.server.LocalServices;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.ArrayList;
import java.util.List;

@MiuiStubHead(manifestName = "com.android.server.logcat.LogcatManagerServiceStub$$")
/* loaded from: classes.dex */
public class LogcatManagerServiceImpl extends LogcatManagerServiceStub {
    private static final List<String> sAutoApprovedList;
    private PermissionManagerServiceInternal mPmi;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LogcatManagerServiceImpl> {

        /* compiled from: LogcatManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LogcatManagerServiceImpl INSTANCE = new LogcatManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LogcatManagerServiceImpl m1869provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LogcatManagerServiceImpl m1868provideNewInstance() {
            return new LogcatManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        sAutoApprovedList = arrayList;
        arrayList.add("com.xiaomi.vipaccount");
    }

    public boolean isAutoApproved(int uid, int pid, String packageName) {
        if (!sAutoApprovedList.contains(packageName)) {
            return false;
        }
        if (this.mPmi == null) {
            this.mPmi = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        }
        return this.mPmi.checkUidPermission(uid, "android.permission.READ_LOGS") == 0;
    }
}
