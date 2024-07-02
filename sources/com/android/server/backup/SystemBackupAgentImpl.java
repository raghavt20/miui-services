package com.android.server.backup;

import android.app.backup.BackupHelper;
import android.content.Context;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import miui.stepcounter.backup.StepBackupHelper;

@MiuiStubHead(manifestName = "com.android.server.backup.SystemBackupAgentStub$$")
/* loaded from: classes.dex */
public class SystemBackupAgentImpl extends SystemBackupAgentStub {
    private static final String STEP_COUNTER_HELPER = "step_counter";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SystemBackupAgentImpl> {

        /* compiled from: SystemBackupAgentImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SystemBackupAgentImpl INSTANCE = new SystemBackupAgentImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SystemBackupAgentImpl m860provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SystemBackupAgentImpl m859provideNewInstance() {
            return new SystemBackupAgentImpl();
        }
    }

    public String getHelperName() {
        return STEP_COUNTER_HELPER;
    }

    public BackupHelper createBackupHelper(Context context) {
        return new StepBackupHelper(context);
    }
}
