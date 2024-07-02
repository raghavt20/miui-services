package com.android.server.pm;

import android.os.SystemClock;
import android.util.Slog;
import com.android.server.pm.parsing.pkg.AndroidPackageInternal;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.miui.base.MiuiStubRegistry;
import java.util.Locale;
import java.util.function.Consumer;
import miui.mqsas.sdk.BootEventManager;

/* loaded from: classes.dex */
public class PerfTimeMonitorImpl extends PerfTimeMonitorStub {
    private static final boolean DEBUG = false;
    private static final String TAG = "PerfTimeMonitorImpl";
    private String pkg = "";
    private String installer = "";
    private int fileCopying = 0;
    private int collectingCerts = 0;
    private int miuiVerification = 0;
    private int googleVerification = 0;
    private int dexopt = 0;
    private int total = 0;
    private long createdAt = 0;
    private long phraseStartedAt = 0;
    private int extractNativeLib = 0;
    private int thirdAppCount = 0;
    private int systemAppCount = 0;
    private int persistAppCount = 0;
    private long extractNativeLibStartedAt = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PerfTimeMonitorImpl> {

        /* compiled from: PerfTimeMonitorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PerfTimeMonitorImpl INSTANCE = new PerfTimeMonitorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PerfTimeMonitorImpl m2139provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PerfTimeMonitorImpl m2138provideNewInstance() {
            return new PerfTimeMonitorImpl();
        }
    }

    public boolean isFinished() {
        return this.total > 0;
    }

    public void setPackageName(String pkg) {
        this.pkg = pkg;
    }

    public void setInstaller(String installer) {
        this.installer = installer;
    }

    public void setStarted(int sessionId) {
        this.createdAt = SystemClock.uptimeMillis();
        Slog.i("InstallationTiming", "beginInstall_sessionId: " + sessionId);
    }

    public void setCollectCertsStarted() {
        this.phraseStartedAt = SystemClock.uptimeMillis();
    }

    void setExtractNativeLibStarted() {
        this.extractNativeLibStartedAt = SystemClock.uptimeMillis();
    }

    public void setMiuiVerificationStarted() {
        this.phraseStartedAt = SystemClock.uptimeMillis();
    }

    public void setGoogleVerificationStarted() {
        this.phraseStartedAt = SystemClock.uptimeMillis();
    }

    public void setDexoptStarted() {
        this.phraseStartedAt = SystemClock.uptimeMillis();
    }

    public void setCollectCertsFinished() {
        this.collectingCerts = (int) (this.collectingCerts + (SystemClock.uptimeMillis() - this.phraseStartedAt));
    }

    void setExtractNativeLibFinished() {
        this.extractNativeLib = (int) (SystemClock.uptimeMillis() - this.extractNativeLibStartedAt);
    }

    public void setFileCopied() {
        this.fileCopying = (int) (SystemClock.uptimeMillis() - this.createdAt);
    }

    public void setMiuiVerificationFinished() {
        this.miuiVerification = (int) (SystemClock.uptimeMillis() - this.phraseStartedAt);
    }

    public void setGoogleVerificationFinished() {
        this.googleVerification = (int) (SystemClock.uptimeMillis() - this.phraseStartedAt);
    }

    public void setDexoptFinished() {
        this.dexopt = (int) (SystemClock.uptimeMillis() - this.phraseStartedAt);
    }

    public void setFinished() {
        this.total = (int) (SystemClock.uptimeMillis() - this.createdAt);
    }

    public int getFileCopying() {
        return this.fileCopying;
    }

    public int getCollectingCerts() {
        return this.collectingCerts;
    }

    public int getMiuiVerification() {
        return this.miuiVerification;
    }

    public int getGoogleVerification() {
        return this.googleVerification;
    }

    public int getDexopt() {
        return this.dexopt;
    }

    public int getTotal() {
        return this.total;
    }

    public void markPmsScanDetail(final PackageManagerService pms) {
        int type;
        synchronized (pms.mLock) {
            final Settings settings = pms.mSettings;
            pms.forEachPackageSetting(new Consumer() { // from class: com.android.server.pm.PerfTimeMonitorImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PerfTimeMonitorImpl.this.lambda$markPmsScanDetail$0(settings, pms, (PackageSetting) obj);
                }
            });
        }
        BootEventManager.getInstance().setSystemAppCount(this.systemAppCount);
        BootEventManager.getInstance().setThirdAppCount(this.thirdAppCount);
        BootEventManager.getInstance().setPersistAppCount(this.persistAppCount);
        if (pms.isFirstBoot()) {
            type = 2;
        } else {
            type = pms.isDeviceUpgrading() ? 3 : 1;
        }
        BootEventManager.getInstance().setBootType(type);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$markPmsScanDetail$0(Settings settings, PackageManagerService pms, PackageSetting ps) {
        if (ps.isSystem()) {
            this.systemAppCount++;
            if (settings.isDisabledSystemPackageLPr(ps.getPackageName())) {
                this.thirdAppCount++;
            }
        } else {
            this.thirdAppCount++;
        }
        AndroidPackageInternal pkg = ps.getPkg();
        if (pkg == null || !pkg.isPersistent()) {
            return;
        }
        if (!pms.getSafeMode() || ps.isSystem()) {
            this.persistAppCount++;
        }
    }

    public void markPackageOptimized(PackageManagerService pms, AndroidPackage pkg) {
        BootEventManager manager = BootEventManager.getInstance();
        PackageStateInternal psi = pms.snapshotComputer().getPackageStateInternal(pkg.getPackageName());
        if (psi.isSystem() && !psi.isUpdatedSystemApp()) {
            manager.setDexoptSystemAppCount(manager.getDexoptSystemAppCount() + 1);
        } else {
            manager.setDexoptThirdAppCount(manager.getDexoptThirdAppCount() + 1);
        }
    }

    public void dump() {
        Slog.i("InstallationTiming", this.pkg + "|" + this.installer + "|" + toString());
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "%d|%d|%d|%d|%d|%d|%d", Integer.valueOf(this.total), Integer.valueOf(this.fileCopying), Integer.valueOf(this.collectingCerts), Integer.valueOf(this.extractNativeLib), Integer.valueOf(this.miuiVerification), Integer.valueOf(this.googleVerification), Integer.valueOf(this.dexopt));
    }
}
