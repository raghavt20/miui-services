package com.android.server;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.PackageManagerServiceUtils;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.util.Set;

/* loaded from: classes.dex */
public class RescuePartyImpl extends RescuePartyStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<RescuePartyImpl> {

        /* compiled from: RescuePartyImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final RescuePartyImpl INSTANCE = new RescuePartyImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public RescuePartyImpl m264provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public RescuePartyImpl m263provideNewInstance() {
            return new RescuePartyImpl();
        }
    }

    public boolean isLauncher(Context context, String packageName) {
        return (RescuePartyPlusHelper.checkDisableRescuePartyPlus() || packageName == null || !packageName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) ? false : true;
    }

    public boolean maybeDoResetConfig(final Context context, String failedPackage) {
        if (!TextUtils.isEmpty(failedPackage)) {
            SystemProperties.set("sys.rescue_party_failed_package", failedPackage);
        }
        if (RescuePartyPlusHelper.checkDisableRescuePartyPlus()) {
            return false;
        }
        int mitigationCount = RescuePartyPlusHelper.getMitigationTempCount();
        if (RescuePartyPlusHelper.getConfigResetProcessStatus()) {
            Slog.w("RescuePartyPlus", "Config Reset in progress!");
            SystemProperties.set("sys.powerctl", "reboot,RescueParty");
            return true;
        }
        if (mitigationCount != 5) {
            return false;
        }
        RescuePartyPlusHelper.setLastResetConfigStatus(true);
        RescuePartyPlusHelper.setConfigResetProcessStatus(true);
        Slog.w("RescuePartyPlus", "Start Config Reset!");
        PackageManagerServiceUtils.logCriticalInfo(3, "Finished rescue level CONFIG_RESET for package " + failedPackage);
        Set<String> deleteFileSet = RescuePartyPlusHelper.tryGetCloudControlOrDefaultData();
        for (String filename : deleteFileSet) {
            Slog.w("RescuePartyPlus", "Preparing to delete files: " + filename);
            PackageManagerServiceUtils.logCriticalInfo(3, "Preparing to delete files: " + filename);
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
            }
        }
        if (!RescuePartyPlusHelper.resetTheme(failedPackage)) {
            Slog.e("RescuePartyPlus", "Reset theme failed: " + failedPackage);
        }
        Runnable runnable = new Runnable() { // from class: com.android.server.RescuePartyImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RescuePartyImpl.lambda$maybeDoResetConfig$0(context);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$maybeDoResetConfig$0(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
            if (pm != null) {
                pm.reboot("RescueParty");
            }
        } catch (Throwable t) {
            Slog.e("RescuePartyPlus", "do config reset failed!", t);
        }
    }
}
