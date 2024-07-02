package com.android.server.print;

import android.content.Intent;
import android.print.PrintJobInfo;
import android.text.TextUtils;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.print.UserState$$")
/* loaded from: classes.dex */
public class MiuiPrintManager extends MiuiPrintManagerStub {
    public static final String MIUI_ACTION_PRINT_DIALOG = "miui.print.PRINT_DIALOG";
    public static final String MIUI_PREFIX = "MIUI:";
    public static final String TAG = "MiuiPrintManager";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiPrintManager> {

        /* compiled from: MiuiPrintManager$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiPrintManager INSTANCE = new MiuiPrintManager();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiPrintManager m2312provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiPrintManager m2311provideNewInstance() {
            return new MiuiPrintManager();
        }
    }

    public boolean ensureInjected(PrintJobInfo info) {
        String jobName = info.getLabel();
        if (!TextUtils.isEmpty(jobName) && jobName.startsWith(MIUI_PREFIX)) {
            info.setLabel(jobName.substring(MIUI_PREFIX.length()));
            return true;
        }
        return false;
    }

    public void printInject(boolean injected, Intent intent) {
        if (injected) {
            intent.setAction(MIUI_ACTION_PRINT_DIALOG);
            Log.d(TAG, "printInject....will handle for MIUI target.");
        }
    }
}
