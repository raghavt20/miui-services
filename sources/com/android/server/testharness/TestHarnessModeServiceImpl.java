package com.android.server.testharness;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.PrintWriter;

@MiuiStubHead(manifestName = "com.android.server.testharness.TestHarnessModeServiceStub$$")
/* loaded from: classes.dex */
public class TestHarnessModeServiceImpl extends TestHarnessModeServiceStub {
    private static final String CMD_DISABLE_MITS = "disable_mits";
    private static final String CMD_ENABLE_MITS = "enable_mits";
    private static final String CMD_HAS_MITS = "has_mits";
    private static final String MITS_ENABLED = "MITS_ENABLED";
    private static final String TAG = TestHarnessModeServiceImpl.class.getSimpleName();
    private PersistentDataBlockManagerInternal mPersistentDataBlockManagerInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TestHarnessModeServiceImpl> {

        /* compiled from: TestHarnessModeServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TestHarnessModeServiceImpl INSTANCE = new TestHarnessModeServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public TestHarnessModeServiceImpl m2325provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public TestHarnessModeServiceImpl m2324provideNewInstance() {
            return new TestHarnessModeServiceImpl();
        }
    }

    private PersistentDataBlockManagerInternal getPersistentDataBlock() {
        if (this.mPersistentDataBlockManagerInternal == null) {
            Slog.d(TAG, "Getting PersistentDataBlockManagerInternal from LocalServices");
            this.mPersistentDataBlockManagerInternal = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        }
        return this.mPersistentDataBlockManagerInternal;
    }

    private boolean isDeviceProvisioned(Context context) {
        ContentResolver cr = context.getContentResolver();
        try {
            boolean provisioned = Settings.Global.getInt(cr, "device_provisioned") == 1;
            return provisioned;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

    private void checkPermissions(Context context) {
        context.enforceCallingPermission("android.permission.ENABLE_TEST_HARNESS_MODE", "You must hold android.permission.ENABLE_TEST_HARNESS_MODE to enable Test Harness Mode");
    }

    public boolean isMITSModeEnabled() {
        PersistentDataBlockManagerInternal blockManager = getPersistentDataBlock();
        if (blockManager == null) {
            Slog.e(TAG, "Failed to get MITS Mode; no implementation of PersistentDataBlockManagerInternal was bound!");
            return false;
        }
        return MITS_ENABLED.equals(new String(blockManager.getMITSModeData()));
    }

    public void setUpMITSMode(Context context) {
        if (!isDeviceProvisioned(context) && isMITSModeEnabled()) {
            ContentResolver cr = context.getContentResolver();
            Settings.Global.putInt(cr, "adb_enabled", 1);
        }
    }

    public boolean isMitsCommand(String cmd) {
        return CMD_ENABLE_MITS.equals(cmd) || CMD_DISABLE_MITS.equals(cmd) || CMD_HAS_MITS.equals(cmd);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onMitsCommand(String cmd, PrintWriter printWriter, Context context) {
        char c;
        switch (cmd.hashCode()) {
            case -1429032361:
                if (cmd.equals(CMD_ENABLE_MITS)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -986100718:
                if (cmd.equals(CMD_DISABLE_MITS)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 140630944:
                if (cmd.equals(CMD_HAS_MITS)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                if (SystemProperties.getInt("ro.debuggable", 0) != 1) {
                    printWriter.println("Forbid to enable Mits Mode.");
                    return 2;
                }
                checkPermissions(context);
                if (!isMITSModeEnabled()) {
                    getPersistentDataBlock().setMITSModeData(MITS_ENABLED.getBytes());
                    printWriter.println("MITS enabled");
                    return 0;
                }
                printWriter.println("MITS already enabled");
                return 1;
            case 1:
                if (isMITSModeEnabled()) {
                    getPersistentDataBlock().clearMITSModeData();
                    printWriter.println("MITS disabled");
                    return 0;
                }
                printWriter.println("MITS already disabled");
                return 1;
            case 2:
                if (isMITSModeEnabled()) {
                    printWriter.println("yes");
                    return 0;
                }
                printWriter.println("no");
                return 1;
            default:
                return 0;
        }
    }
}
