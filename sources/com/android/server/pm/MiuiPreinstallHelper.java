package com.android.server.pm;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.parsing.ApkLite;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.logging.EventLogTags;
import com.android.internal.util.ArrayUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.wm.MiuiSizeCompatService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class MiuiPreinstallHelper extends MiuiPreinstallHelperStub {
    private static final String DATA_APP_DIR = "/data/app/";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String RANDOM_DIR_PREFIX = "~~";
    private static final String TAG = MiuiPreinstallHelper.class.getSimpleName();
    private MiuiBusinessPreinstallConfig mBusinessPreinstallConfig;
    private boolean mIsDeviceUpgrading;
    private boolean mIsFirstBoot;
    private MiuiOperatorPreinstallConfig mOperatorPreinstallConfig;
    private MiuiPlatformPreinstallConfig mPlatformPreinstallConfig;
    private PackageManagerService mPms;
    private File mPreviousSettingsFilename;
    private File mSettingsFilename;
    private File mSettingsReserveCopyFilename;
    private List<File> mPreinstallDirs = new ArrayList();
    private ArrayMap<String, MiuiPreinstallApp> mMiuiPreinstallApps = new ArrayMap<>();
    private List<String> mLegacyPreinstallAppPaths = new ArrayList();
    private Object mLock = new Object();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiPreinstallHelper> {

        /* compiled from: MiuiPreinstallHelper$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiPreinstallHelper INSTANCE = new MiuiPreinstallHelper();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiPreinstallHelper m2109provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiPreinstallHelper m2108provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.MiuiPreinstallHelper is marked as singleton");
        }
    }

    public static MiuiPreinstallHelper getInstance() {
        return (MiuiPreinstallHelper) MiuiPreinstallHelperStub.getInstance();
    }

    public void init(PackageManagerService pms) {
        Slog.d(TAG, "use Miui Preinstall Frame.");
        this.mPms = pms;
        this.mIsFirstBoot = pms.isFirstBoot();
        this.mIsDeviceUpgrading = this.mPms.isDeviceUpgrading();
        File systemDir = new File(Environment.getDataDirectory(), "system");
        if (!systemDir.exists()) {
            systemDir.mkdirs();
            FileUtils.setPermissions(systemDir.toString(), 509, -1, -1);
        }
        this.mSettingsFilename = new File(systemDir, "preinstall_packages.xml");
        this.mSettingsReserveCopyFilename = new File(systemDir, "preinstall_packages.xml.reservecopy");
        this.mPreviousSettingsFilename = new File(systemDir, "preinstall_packages-backup.xml");
        this.mPlatformPreinstallConfig = new MiuiPlatformPreinstallConfig();
        this.mBusinessPreinstallConfig = new MiuiBusinessPreinstallConfig();
        this.mOperatorPreinstallConfig = new MiuiOperatorPreinstallConfig();
        synchronized (this.mLock) {
            this.mMiuiPreinstallApps.clear();
            readHistory();
        }
        initPreinstallDirs();
        initLegacyPreinstallList();
    }

    public void init(PackageManagerService pms, List<String> legacyPreinstallAppPaths, Map<String, MiuiPreinstallApp> miuiPreinstallApps, MiuiBusinessPreinstallConfig miuiBusinessPreinstallConfig) {
        this.mPms = pms;
        this.mIsDeviceUpgrading = pms.isDeviceUpgrading();
        this.mLegacyPreinstallAppPaths.clear();
        this.mLegacyPreinstallAppPaths = legacyPreinstallAppPaths;
        synchronized (this.mLock) {
            this.mMiuiPreinstallApps.clear();
            this.mMiuiPreinstallApps.putAll(miuiPreinstallApps);
        }
        this.mPlatformPreinstallConfig = new MiuiPlatformPreinstallConfig();
        this.mBusinessPreinstallConfig = miuiBusinessPreinstallConfig;
        this.mOperatorPreinstallConfig = new MiuiOperatorPreinstallConfig();
    }

    public boolean isSupportNewFrame() {
        return Build.VERSION.DEVICE_INITIAL_SDK_INT > 33;
    }

    public List<File> getPreinstallDirs() {
        return this.mPreinstallDirs;
    }

    public boolean shouldIgnoreInstall(ParsedPackage parsedPackage) {
        String pkgName = parsedPackage.getPackageName();
        long versionCode = parsedPackage.getLongVersionCode();
        String apkPath = parsedPackage.getPath();
        boolean z = true;
        if (this.mLegacyPreinstallAppPaths.contains(apkPath)) {
            Slog.d(TAG, "package use legacy preinstall: " + pkgName + " path: " + apkPath);
            return true;
        }
        synchronized (this.mLock) {
            if (this.mMiuiPreinstallApps.containsKey(pkgName)) {
                PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(pkgName);
                if (ps == null) {
                    Slog.d(TAG, "package had beed uninstalled: " + pkgName + " skip!");
                    return true;
                }
                if (ps.getPathString() != null && this.mBusinessPreinstallConfig.isBusinessPreinstall(ps.getPathString()) && (ps.getVersionCode() != versionCode || !apkPath.equals(ps.getPathString()))) {
                    Slog.e(TAG, "business preinstall app must not update: " + pkgName + " do not scan the path: " + apkPath);
                    return true;
                }
                if (ps.getPathString() == null || ps.getPathString().equals(apkPath) || !ps.getPathString().startsWith(Environment.getDataDirectory().getPath() + "/app")) {
                    return false;
                }
                if (this.mIsDeviceUpgrading && !this.mBusinessPreinstallConfig.isBusinessPreinstall(apkPath) && ps.getVersionCode() < versionCode) {
                    ApkLite apkLite = parseApkLite(new File(parsedPackage.getBaseApkPath()));
                    if (apkLite == null) {
                        Slog.w(TAG, "parseApkLite failed: " + parsedPackage.getBaseApkPath());
                    }
                    boolean sameSignatures = PackageManagerServiceUtils.compareSignatures(apkLite != null ? apkLite.getSigningDetails().getSignatures() : null, ps.getSigningDetails().getSignatures()) == 0;
                    if (sameSignatures) {
                        Slog.d(TAG, "package had beed updated: " + pkgName + " versionCode: " + ps.getVersionCode() + ", but current version is bertter: " + versionCode);
                        copyMiuiPreinstallApp(apkPath, ps, parsedPackage);
                    } else {
                        Slog.e(TAG, apkPath + " mismatch signature with " + ps.getPathString() + ", need skip copy!");
                    }
                }
                Slog.d(TAG, "package had beed updated: " + pkgName + " skip!");
                return true;
            }
            if (miui.os.Build.IS_INTERNATIONAL_BUILD) {
                if (!this.mBusinessPreinstallConfig.isBusinessPreinstall(apkPath) && !this.mPlatformPreinstallConfig.needIgnore(apkPath, pkgName) && !this.mOperatorPreinstallConfig.needIgnore(apkPath, pkgName)) {
                    z = false;
                }
                return z;
            }
            if (!this.mPlatformPreinstallConfig.needIgnore(apkPath, pkgName) && !this.mBusinessPreinstallConfig.needIgnore(apkPath, pkgName)) {
                z = false;
            }
            return z;
        }
    }

    public void deleteArtifactsForPreinstall(PackageSetting ps, boolean deleteCodeAndResources, int userId) {
        if (ps == null || ps.getPkg() == null || !ps.getPkg().isMiuiPreinstall() || deleteCodeAndResources) {
            return;
        }
        if (userId != -1) {
            int[] userIds = this.mPms.mInjector.getUserManagerInternal().getUserIds();
            if (userIds.length > 0) {
                for (int nextUserId : userIds) {
                    if (nextUserId != userId && ps.getUserStateOrDefault(nextUserId).isInstalled()) {
                        Slog.d(TAG, "Still installed by other users, don't delete artifacts!");
                        return;
                    }
                }
            }
        }
        Slog.d(TAG, ps.getPackageName() + " is not installed by other users, will delete oat artifacts and app-libs");
        File file = new File(ps.getLegacyNativeLibraryPath());
        NativeLibraryHelper.removeNativeBinariesFromDirLI(file, true);
        PackageManagerService packageManagerService = this.mPms;
        packageManagerService.deleteOatArtifactsOfPackage(packageManagerService.snapshotComputer(), ps.getPackageName());
    }

    public void installVanwardApps() {
        List<File> vanwardAppList = getVanwardAppList();
        if (vanwardAppList.isEmpty()) {
            return;
        }
        List<PackageLite> filterList = new ArrayList<>();
        for (File apkFile : vanwardAppList) {
            PackageLite packageLite = parsedPackageLite(apkFile);
            if (packageLite == null) {
                Slog.w(TAG, "installVanwardApps parsedPackageLite failed:  apkFile");
            } else {
                PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(packageLite.getPackageName());
                if (ps != null) {
                    Slog.w(TAG, "installVanwardApps the app had been installed: " + apkFile.getAbsolutePath() + " keep first: " + ps.getPathString());
                } else if (!needIgnore(packageLite)) {
                    filterList.add(packageLite);
                }
            }
        }
        if (filterList.isEmpty()) {
            return;
        }
        batchInstallApps(filterList);
        writeHistory();
    }

    public MiuiPlatformPreinstallConfig getPlatformPreinstallConfig() {
        if (this.mPlatformPreinstallConfig == null) {
            Slog.w(TAG, "PlatformPreinstallConfig is not init!");
            this.mPlatformPreinstallConfig = new MiuiPlatformPreinstallConfig();
        }
        return this.mPlatformPreinstallConfig;
    }

    public MiuiBusinessPreinstallConfig getBusinessPreinstallConfig() {
        if (this.mBusinessPreinstallConfig == null) {
            Slog.w(TAG, "BusinessPreinstallConfig is not init!");
            this.mBusinessPreinstallConfig = new MiuiBusinessPreinstallConfig();
        }
        return this.mBusinessPreinstallConfig;
    }

    public MiuiOperatorPreinstallConfig getOperatorPreinstallConfig() {
        if (this.mOperatorPreinstallConfig == null) {
            Slog.w(TAG, "OperatorPreinstallConfig is not init!");
            this.mOperatorPreinstallConfig = new MiuiOperatorPreinstallConfig();
        }
        return this.mOperatorPreinstallConfig;
    }

    private List<File> getVanwardAppList() {
        List<File> vanwardAppList = new ArrayList<>();
        if (this.mBusinessPreinstallConfig.getVanwardAppList() != null) {
            vanwardAppList.addAll(this.mBusinessPreinstallConfig.getVanwardAppList());
        }
        return vanwardAppList;
    }

    private List<File> getCustAppList() {
        List<File> custAppList = new ArrayList<>();
        if (this.mBusinessPreinstallConfig.getCustAppList() != null) {
            custAppList.addAll(this.mBusinessPreinstallConfig.getCustAppList());
        }
        if (this.mOperatorPreinstallConfig.getCustAppList() != null) {
            custAppList.addAll(this.mOperatorPreinstallConfig.getCustAppList());
        }
        return custAppList;
    }

    private boolean needLegacyBatchPreinstall(String apkPath, String pkgName) {
        if (this.mBusinessPreinstallConfig.needLegacyPreinstall(apkPath, pkgName) || this.mOperatorPreinstallConfig.needLegacyPreinstall(apkPath, pkgName)) {
            return true;
        }
        return false;
    }

    private void batchInstallApps(List<PackageLite> apkList) {
        ArrayList<PackageLite> installedApps = new ArrayList<>();
        SparseArray<PackageLite> activeSessions = new SparseArray<>(apkList.size());
        HashSet<String> packages = new HashSet<>();
        long waitDuration = 0;
        LocalIntentReceiver receiver = new LocalIntentReceiver(apkList.size());
        Context context = this.mPms.mContext;
        if (context == null) {
            Slog.e(TAG, "batchInstallApps context is null!");
        }
        for (PackageLite packageLite : apkList) {
            if (packages.contains(packageLite.getPackageName())) {
                Slog.e(TAG, "Fail to installApp: " + packageLite.getPackageName() + ", duplicate package name, version: " + packageLite.getVersionCode());
            } else {
                packages.add(packageLite.getPackageName());
                int sessionId = installOne(context, receiver, packageLite);
                if (sessionId > 0) {
                    activeSessions.put(sessionId, packageLite);
                    waitDuration += TimeUnit.MINUTES.toMillis(2L);
                }
            }
        }
        long start = SystemClock.uptimeMillis();
        while (true) {
            int i = 0;
            if (activeSessions.size() <= 0 || SystemClock.uptimeMillis() - start >= waitDuration) {
                break;
            }
            SystemClock.sleep(1000L);
            for (Intent intent : receiver.getResultsNoWait()) {
                int sessionId2 = intent.getIntExtra("android.content.pm.extra.SESSION_ID", i);
                if (activeSessions.indexOfKey(sessionId2) >= 0) {
                    PackageLite pkgLite = (PackageLite) activeSessions.removeReturnOld(sessionId2);
                    HashSet<String> packages2 = packages;
                    int status = intent.getIntExtra("android.content.pm.extra.STATUS", 1);
                    if (status == 0) {
                        installedApps.add(pkgLite);
                        packages = packages2;
                        i = 0;
                    } else {
                        String errorMsg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
                        Slog.d(TAG, "Failed to install " + pkgLite.getPath() + ": error code=" + status + ", msg=" + errorMsg);
                        packages = packages2;
                        waitDuration = waitDuration;
                        receiver = receiver;
                        i = 0;
                    }
                }
            }
        }
        for (int i2 = 0; i2 < activeSessions.size(); i2++) {
            int sessionId3 = activeSessions.keyAt(0);
            Slog.e(TAG, "Failed to install " + activeSessions.get(sessionId3).getPath() + ": timeout, sessionId=" + sessionId3);
        }
        Iterator<PackageLite> it = installedApps.iterator();
        while (it.hasNext()) {
            PackageLite pkgLite2 = it.next();
            addMiuiPreinstallApp(new MiuiPreinstallApp(pkgLite2.getPackageName(), pkgLite2.getLongVersionCode(), pkgLite2.getPath()));
        }
    }

    private int installOne(Context context, LocalIntentReceiver receiver, PackageLite packageLite) {
        try {
            boolean useLegacyPreinstall = needLegacyBatchPreinstall(packageLite.getPath(), packageLite.getPackageName());
            PackageInstaller.SessionParams sessionParams = makeSessionParams(packageLite, useLegacyPreinstall);
            int sessionId = doCreateSession(context, sessionParams);
            if (sessionId <= 0) {
                return -1;
            }
            if (useLegacyPreinstall) {
                for (String splitCodePath : packageLite.getAllApkPaths()) {
                    File splitFile = new File(splitCodePath);
                    if (useLegacyPreinstall && !doWriteSession(context, splitFile.getName(), splitFile, sessionId)) {
                        doAandonSession(context, sessionId);
                        return -1;
                    }
                }
            }
            if (doCommitSession(context, sessionId, receiver.getIntentSender())) {
                return sessionId;
            }
            return -1;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install " + packageLite.getPath(), e);
            return -1;
        }
    }

    private PackageInstaller.SessionParams makeSessionParams(PackageLite pkgLite, boolean useLegacyPreinstall) {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.setInstallAsInstantApp(false);
        sessionParams.setInstallerPackageName("android");
        sessionParams.setAppPackageName(pkgLite.getPackageName());
        sessionParams.setInstallLocation(pkgLite.getInstallLocation());
        if (!useLegacyPreinstall) {
            sessionParams.setIsMiuiPreinstall();
            sessionParams.setApkLocation(pkgLite.getPath());
        }
        try {
            sessionParams.setSize(InstallLocationUtils.calculateInstalledSize(pkgLite, (String) null));
        } catch (IOException e) {
            sessionParams.setSize(new File(pkgLite.getBaseApkPath()).length());
        }
        return sessionParams;
    }

    private static int doCreateSession(Context context, PackageInstaller.SessionParams sessionParams) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        try {
            int sessionId = packageInstaller.createSession(sessionParams);
            return sessionId;
        } catch (IOException e) {
            Slog.e(TAG, "doCreateSession failed: ", e);
            return 0;
        }
    }

    private static void doAandonSession(Context context, int sessionId) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
                session.abandon();
            } catch (IOException e) {
                Slog.e(TAG, "doAandonSession failed: ", e);
            }
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private boolean doWriteSession(Context context, String name, File apkFile, int sessionId) {
        PackageInstaller.Session session;
        Throwable th;
        Exception e;
        ParcelFileDescriptor pfd = null;
        try {
            ParcelFileDescriptor pfd2 = ParcelFileDescriptor.open(apkFile, 268435456);
            try {
                session = context.getPackageManager().getPackageInstaller().openSession(sessionId);
                try {
                    session.write(name, 0L, pfd2.getStatSize(), pfd2);
                    IoUtils.closeQuietly(pfd2);
                    IoUtils.closeQuietly(session);
                    return true;
                } catch (Exception e2) {
                    e = e2;
                    pfd = pfd2;
                    try {
                        Slog.e(TAG, "doWriteSession failed: ", e);
                        IoUtils.closeQuietly(pfd);
                        IoUtils.closeQuietly(session);
                        return false;
                    } catch (Throwable th2) {
                        th = th2;
                        IoUtils.closeQuietly(pfd);
                        IoUtils.closeQuietly(session);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    pfd = pfd2;
                    IoUtils.closeQuietly(pfd);
                    IoUtils.closeQuietly(session);
                    throw th;
                }
            } catch (Exception e3) {
                session = null;
                e = e3;
                pfd = pfd2;
            } catch (Throwable th4) {
                session = null;
                th = th4;
                pfd = pfd2;
            }
        } catch (Exception e4) {
            session = null;
            e = e4;
        } catch (Throwable th5) {
            session = null;
            th = th5;
        }
    }

    private static boolean doCommitSession(Context context, int sessionId, IntentSender target) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
                session.commit(target);
                IoUtils.closeQuietly(session);
                return true;
            } catch (Exception e) {
                Slog.e(TAG, "doCommitSession failed: ", e);
                IoUtils.closeQuietly(session);
                return false;
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(session);
            throw th;
        }
    }

    public void installCustApps() {
        List<File> custAppList = getCustAppList();
        if (custAppList.isEmpty()) {
            return;
        }
        List<PackageLite> filterList = new ArrayList<>();
        for (File apkFile : custAppList) {
            PackageLite packageLite = parsedPackageLite(apkFile);
            if (packageLite == null) {
                Slog.w(TAG, "installCustApps parsedPackageLite failed: " + apkFile);
            } else {
                PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(packageLite.getPackageName());
                synchronized (this.mLock) {
                    if (this.mMiuiPreinstallApps.containsKey(packageLite.getPackageName()) && ps == null) {
                        Slog.w(TAG, "installCustApps package had beed uninstalled: " + packageLite.getPackageName() + " skip!");
                    } else if (ps != null) {
                        Slog.w(TAG, "installCustApps the app had been installed: " + apkFile.getAbsolutePath() + " keep first: " + ps.getPathString());
                    } else if (!needIgnore(packageLite)) {
                        filterList.add(packageLite);
                    }
                }
            }
        }
        if (filterList.isEmpty()) {
            return;
        }
        batchInstallApps(filterList);
        writeHistory();
    }

    private boolean needIgnore(PackageLite packageLite) {
        String apkPath = packageLite.getPath();
        String pkgName = packageLite.getPackageName();
        if (this.mPlatformPreinstallConfig.needIgnore(apkPath, pkgName) || this.mBusinessPreinstallConfig.needIgnore(apkPath, pkgName) || this.mOperatorPreinstallConfig.needIgnore(apkPath, pkgName)) {
            return true;
        }
        return false;
    }

    private void initPreinstallDirs() {
        if (this.mPlatformPreinstallConfig.getPreinstallDirs() != null) {
            this.mPreinstallDirs.addAll(this.mPlatformPreinstallConfig.getPreinstallDirs());
        }
        if (SystemProperties.getInt("ro.miui.product_to_cust", -1) == 1 && this.mBusinessPreinstallConfig.getCustMiuiPreinstallDirs() != null) {
            this.mPreinstallDirs.addAll(this.mBusinessPreinstallConfig.getCustMiuiPreinstallDirs());
        }
        if (this.mBusinessPreinstallConfig.getPreinstallDirs() != null) {
            if (!miui.os.Build.IS_INTERNATIONAL_BUILD || !this.mPms.isFirstBoot()) {
                this.mPreinstallDirs.addAll(this.mBusinessPreinstallConfig.getPreinstallDirs());
            }
        }
    }

    private void initLegacyPreinstallList() {
        this.mLegacyPreinstallAppPaths.clear();
        this.mLegacyPreinstallAppPaths.addAll(this.mPlatformPreinstallConfig.getLegacyPreinstallList(this.mIsFirstBoot, this.mIsDeviceUpgrading));
        this.mLegacyPreinstallAppPaths.addAll(this.mBusinessPreinstallConfig.getLegacyPreinstallList(this.mIsFirstBoot, this.mIsDeviceUpgrading));
        this.mLegacyPreinstallAppPaths.addAll(this.mOperatorPreinstallConfig.getLegacyPreinstallList(this.mIsFirstBoot, this.mIsDeviceUpgrading));
    }

    public void performLegacyCopyPreinstall() {
        String cryptState;
        PackageStateInternal ps;
        String cryptState2 = SystemProperties.get("vold.decrypt");
        if (ENCRYPTING_STATE.equals(cryptState2)) {
            Slog.w(TAG, "Detected encryption in progress - can't copy preinstall apps now!");
            return;
        }
        if ((!this.mIsFirstBoot && !this.mIsDeviceUpgrading) || this.mLegacyPreinstallAppPaths.size() == 0) {
            return;
        }
        DeletePackageHelper deletePackageHelper = new DeletePackageHelper(this.mPms);
        for (String path : this.mLegacyPreinstallAppPaths) {
            File targetAppDir = new File(path);
            if (!targetAppDir.exists()) {
                Slog.w(TAG, "copyLegacyPreisntallApp: " + path + " is not exists!");
            } else {
                File[] files = targetAppDir.listFiles();
                if (ArrayUtils.isEmpty(files)) {
                    Slog.w(TAG, "there is none apk file in path: " + path);
                } else {
                    PackageLite packageLite = parsedPackageLite(targetAppDir);
                    if (packageLite == null) {
                        Slog.w(TAG, "parsedPackageLite failed: " + path);
                    } else {
                        String pkgName = packageLite.getPackageName();
                        if (!needIgnore(packageLite)) {
                            String str = TAG;
                            Slog.d(str, "copyLegacyPreisntallApp: " + packageLite.getPackageName());
                            if (this.mIsFirstBoot) {
                                copyLegacyPreisntallApp(path, null, packageLite);
                                cryptState = cryptState2;
                            } else if (!this.mIsDeviceUpgrading) {
                                cryptState = cryptState2;
                            } else if (this.mMiuiPreinstallApps.containsKey(pkgName)) {
                                PackageStateInternal ps2 = this.mPms.snapshotComputer().getPackageStateInternal(pkgName);
                                if (ps2 != null) {
                                    if (ps2.getVersionCode() < packageLite.getLongVersionCode()) {
                                        ApkLite apkLite = parseApkLite(new File(packageLite.getBaseApkPath()));
                                        if (apkLite == null) {
                                            Slog.w(str, "parseApkLite failed: " + packageLite.getBaseApkPath());
                                        } else {
                                            if (PackageManagerServiceUtils.compareSignatures(apkLite.getSigningDetails().getSignatures(), ps2.getSigningDetails().getSignatures()) == 0) {
                                                cryptState = cryptState2;
                                                ps = ps2;
                                            } else {
                                                Slog.e(str, apkLite.getPath() + " mismatch signature with " + ps2.getPathString() + ", delete it's resources and data before coping");
                                                cryptState = cryptState2;
                                                ps = ps2;
                                                if (deletePackageHelper.deletePackageX(ps2.getPackageName(), ps2.getVersionCode(), 0, 2, true) != 1) {
                                                    Slog.e(str, "Delete mismatch signature app " + pkgName + " failed, skip coping " + packageLite.getPath());
                                                }
                                            }
                                            copyLegacyPreisntallApp(path, ps, packageLite);
                                        }
                                    } else {
                                        Slog.w(str, packageLite.getPath() + " is not newer than " + ps2.getPathString() + "[" + ps2.getVersionCode() + "], skip coping");
                                    }
                                }
                            } else {
                                cryptState = cryptState2;
                                copyLegacyPreisntallApp(path, null, packageLite);
                            }
                            cryptState2 = cryptState;
                        }
                    }
                }
            }
        }
    }

    public void insertPreinstallPackageSetting(PackageSetting ps) {
        MiuiPreinstallApp miuiPreinstallApp = new MiuiPreinstallApp(ps.getPackageName(), ps.getVersionCode(), ps.getPath().getAbsolutePath());
        synchronized (this.mLock) {
            this.mMiuiPreinstallApps.put(ps.getPackageName(), miuiPreinstallApp);
        }
    }

    private void copyLegacyPreisntallApp(String path, PackageStateInternal ps, PackageLite packageLite) {
        File dstCodePath;
        File targetAppDir = new File(path);
        File[] files = targetAppDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            Slog.w(TAG, "there is none apk file in path: " + path);
            return;
        }
        File dstCodePath2 = null;
        if (ps != null && ps.getPathString().startsWith(DATA_APP_DIR)) {
            dstCodePath2 = ps.getPath();
            cleanUpResource(dstCodePath2);
        }
        if (dstCodePath2 != null) {
            dstCodePath = dstCodePath2;
        } else {
            dstCodePath = PackageManagerServiceUtils.getNextCodePath(new File(DATA_APP_DIR), packageLite.getPackageName());
        }
        if (!dstCodePath.exists()) {
            dstCodePath.mkdirs();
        }
        if (dstCodePath.exists()) {
            File codePathParent = dstCodePath.getParentFile();
            if (codePathParent.getName().startsWith(RANDOM_DIR_PREFIX)) {
                FileUtils.setPermissions(codePathParent.getAbsolutePath(), 509, -1, -1);
            }
            FileUtils.setPermissions(dstCodePath, 509, -1, -1);
        }
        boolean res = false;
        for (File file : files) {
            File dstFile = new File(dstCodePath, file.getName());
            try {
                FileUtils.copy(file, dstFile);
                boolean res2 = FileUtils.setPermissions(dstFile, 420, -1, -1) == 0;
                res = res2;
            } catch (IOException e) {
                Slog.d(TAG, "Copy failed from: " + file.getPath() + " to " + dstFile.getPath() + ":" + e.toString());
            }
        }
        if (res) {
            addMiuiPreinstallApp(new MiuiPreinstallApp(packageLite.getPackageName(), packageLite.getLongVersionCode(), packageLite.getPath()));
        }
    }

    private void copyMiuiPreinstallApp(String path, PackageStateInternal ps, ParsedPackage parsedPackage) {
        File dstCodePath;
        File targetAppDir = new File(path);
        File[] files = targetAppDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            Slog.w(TAG, "there is none apk file in path: " + path);
            return;
        }
        if (ps == null) {
            dstCodePath = null;
        } else {
            File dstCodePath2 = ps.getPath();
            cleanUpResource(dstCodePath2);
            dstCodePath = dstCodePath2;
        }
        if (dstCodePath.exists()) {
            File codePathParent = dstCodePath.getParentFile();
            if (codePathParent.getName().startsWith(RANDOM_DIR_PREFIX)) {
                FileUtils.setPermissions(codePathParent.getAbsolutePath(), 509, -1, -1);
            }
            FileUtils.setPermissions(dstCodePath, 509, -1, -1);
        }
        boolean res = false;
        for (File file : files) {
            File dstFile = new File(dstCodePath, file.getName());
            try {
                FileUtils.copy(file, dstFile);
                boolean res2 = FileUtils.setPermissions(dstFile, 420, -1, -1) == 0;
                res = res2;
            } catch (IOException e) {
                Slog.d(TAG, "Copy failed from: " + file.getPath() + " to " + dstFile.getPath() + ":" + e.toString());
            }
        }
        if (res) {
            addMiuiPreinstallApp(new MiuiPreinstallApp(parsedPackage.getPackageName(), parsedPackage.getLongVersionCode(), parsedPackage.getPath()));
        }
    }

    private void cleanUpResource(File dstCodePath) {
        if (dstCodePath != null && dstCodePath.isDirectory()) {
            dstCodePath.listFiles(new FileFilter() { // from class: com.android.server.pm.MiuiPreinstallHelper$$ExternalSyntheticLambda0
                @Override // java.io.FileFilter
                public final boolean accept(File file) {
                    boolean lambda$cleanUpResource$0;
                    lambda$cleanUpResource$0 = MiuiPreinstallHelper.this.lambda$cleanUpResource$0(file);
                    return lambda$cleanUpResource$0;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$cleanUpResource$0(File f) {
        if (f.getName().endsWith(".apk")) {
            Slog.d(TAG, "list and delete " + f.getName());
            f.delete();
            return false;
        }
        if (f.isDirectory()) {
            deleteContentsRecursive(f);
            f.delete();
            return false;
        }
        Slog.d(TAG, "list unknown file: " + f.getName());
        return false;
    }

    private boolean deleteContentsRecursive(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContentsRecursive(file);
                }
                if (!file.delete()) {
                    Slog.w(TAG, "Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }

    private PackageLite parsedPackageLite(File apkFile) {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input.reset(), apkFile, 0);
        if (result.isError()) {
            Slog.e(TAG, "Failed to parsePackageLite: " + apkFile + " error: " + result.getErrorMessage(), result.getException());
            return null;
        }
        return (PackageLite) result.getResult();
    }

    private ApkLite parseApkLite(File apkFile) {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<ApkLite> result = ApkLiteParseUtils.parseApkLite(input.reset(), apkFile, 32);
        if (result.isError()) {
            Slog.e(TAG, "Failed to parseApkLite: " + apkFile + " error: " + result.getErrorMessage(), result.getException());
            return null;
        }
        return (ApkLite) result.getResult();
    }

    private void readHistory() {
        FileInputStream str;
        int type;
        if (this.mPms.isFirstBoot()) {
            return;
        }
        ResilientAtomicFile atomicFile = getSettingsFile();
        try {
            str = atomicFile.openRead();
        } catch (Exception e) {
        } catch (Throwable th) {
            if (atomicFile != null) {
                try {
                    atomicFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
        if (str != null) {
            TypedXmlPullParser parser = Xml.resolvePullParser(str);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                Slog.wtf(TAG, "No start tag found in preinstall settings");
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type2 = parser.next();
                if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
                if (type2 != 3 && type2 != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("preinstall_package")) {
                        String packageName = parser.getAttributeValue((String) null, "name");
                        long versionCode = parser.getAttributeLong((String) null, AmapExtraCommand.VERSION_KEY, 0L);
                        String apkPath = parser.getAttributeValue((String) null, "path");
                        addMiuiPreinstallApp(new MiuiPreinstallApp(packageName, versionCode, apkPath));
                    }
                }
            }
            if (atomicFile != null) {
                atomicFile.close();
                return;
            }
            return;
        }
        if (atomicFile != null) {
            atomicFile.close();
        }
    }

    public boolean isMiuiPreinstallApp(String pkgName) {
        if (!isSupportNewFrame()) {
            return false;
        }
        synchronized (this.mLock) {
            return this.mMiuiPreinstallApps.containsKey(pkgName);
        }
    }

    public boolean isPreinstalledPackage(String pkgName) {
        if (!isSupportNewFrame()) {
            return false;
        }
        synchronized (this.mLock) {
            return (this.mMiuiPreinstallApps.isEmpty() || !this.mMiuiPreinstallApps.containsKey(pkgName) || this.mBusinessPreinstallConfig.isCloudOfflineApp(pkgName)) ? false : true;
        }
    }

    public int getPreinstallAppVersion(String pkgName) {
        if (!isSupportNewFrame()) {
            return 0;
        }
        synchronized (this.mLock) {
            if (this.mMiuiPreinstallApps.isEmpty() || !this.mMiuiPreinstallApps.containsKey(pkgName) || this.mBusinessPreinstallConfig.isCloudOfflineApp(pkgName)) {
                return 0;
            }
            return (int) this.mMiuiPreinstallApps.get(pkgName).getVersionCode();
        }
    }

    public boolean isMiuiPreinstallAppPath(String apkPath) {
        return isSupportNewFrame() && !TextUtils.isEmpty(apkPath) && (this.mBusinessPreinstallConfig.isBusinessPreinstall(apkPath) || this.mBusinessPreinstallConfig.isCustMiuiPreinstall(apkPath) || this.mPlatformPreinstallConfig.isPlatformPreinstall(apkPath) || this.mOperatorPreinstallConfig.isOperatorPreinstall(apkPath));
    }

    public void writeHistory() {
        if (!isSupportNewFrame()) {
            return;
        }
        synchronized (this.mLock) {
            long startTime = SystemClock.uptimeMillis();
            ResilientAtomicFile atomicFile = getSettingsFile();
            FileOutputStream str = null;
            try {
                try {
                    str = atomicFile.startWrite();
                    TypedXmlSerializer serializer = Xml.resolveSerializer(str);
                    serializer.startDocument((String) null, true);
                    serializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    serializer.startTag((String) null, "preinstall_packages");
                    for (MiuiPreinstallApp miuiPreinstallApp : this.mMiuiPreinstallApps.values()) {
                        serializer.startTag((String) null, "preinstall_package");
                        serializer.attribute((String) null, "name", miuiPreinstallApp.getPackageName());
                        serializer.attributeLong((String) null, AmapExtraCommand.VERSION_KEY, miuiPreinstallApp.getVersionCode());
                        serializer.attribute((String) null, "path", miuiPreinstallApp.getApkPath());
                        serializer.endTag((String) null, "preinstall_package");
                    }
                    serializer.endTag((String) null, "preinstall_packages");
                    serializer.endDocument();
                    atomicFile.finishWrite(str);
                    EventLogTags.writeCommitSysConfigFile("preinstall_package", SystemClock.uptimeMillis() - startTime);
                } catch (Exception e) {
                    Slog.e(TAG, "Unable to write preinstall settings, current changes will be lost at reboot", e);
                    if (str != null) {
                        atomicFile.failWrite(str);
                    }
                }
                if (atomicFile != null) {
                    atomicFile.close();
                }
            } finally {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<MiuiPreinstallApp> getPreinstallApps() {
        List<MiuiPreinstallApp> preinstallAppList = new ArrayList<>();
        synchronized (this.mLock) {
            preinstallAppList.addAll(this.mMiuiPreinstallApps.values());
        }
        return preinstallAppList;
    }

    private void addMiuiPreinstallApp(MiuiPreinstallApp miuiPreinstallApp) {
        Slog.d(TAG, "addMiuiPreinstallApp: " + miuiPreinstallApp.getPackageName() + " versionCode: " + miuiPreinstallApp.getVersionCode() + " apkPath: " + miuiPreinstallApp.getApkPath());
        synchronized (this.mLock) {
            this.mMiuiPreinstallApps.put(miuiPreinstallApp.getPackageName(), miuiPreinstallApp);
        }
    }

    private ResilientAtomicFile getSettingsFile() {
        return new ResilientAtomicFile(this.mSettingsFilename, this.mPreviousSettingsFilename, this.mSettingsReserveCopyFilename, 432, "package manager preinstall settings", this);
    }

    public void scheduleJob() {
        JobInfo jobInfo = new JobInfo.Builder(BackgroundPreinstalloptService.JOBID, new ComponentName("android", BackgroundPreinstalloptService.class.getName())).setPeriodic(TimeUnit.DAYS.toMillis(1L)).setRequiresDeviceIdle(true).setRequiresCharging(true).setRequiresBatteryNotLow(true).build();
        JobScheduler jobScheduler = (JobScheduler) this.mPms.mContext.getSystemService("jobscheduler");
        jobScheduler.schedule(jobInfo);
    }

    public boolean onStartJob(final BackgroundPreinstalloptService jobService, final JobParameters params) {
        if (this.mPms == null) {
            Slog.e(TAG, "clean up faild, reason: pkms is null");
            return false;
        }
        synchronized (this.mLock) {
            if (this.mMiuiPreinstallApps.isEmpty()) {
                Slog.e(TAG, "clean up faild, reason: mMiuiPreinstallApps is empty");
                return false;
            }
            this.mPms.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.pm.MiuiPreinstallHelper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiPreinstallHelper.this.lambda$onStartJob$1(jobService, params);
                }
            });
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onStartJob$1(BackgroundPreinstalloptService jobService, JobParameters params) {
        File appLib;
        synchronized (this.mLock) {
            for (Map.Entry<String, MiuiPreinstallApp> entry : this.mMiuiPreinstallApps.entrySet()) {
                if (shouldCleanUp(entry) && (appLib = deriveAppLib(entry.getValue().getApkPath())) != null) {
                    Slog.d(TAG, "will clean up " + appLib.getAbsolutePath());
                    removeNativeBinariesFromDir(appLib, true);
                }
            }
        }
        jobService.jobFinished(params, false);
    }

    private boolean shouldCleanUp(Map.Entry<String, MiuiPreinstallApp> entry) {
        PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(entry.getKey());
        String apkPath = entry.getValue().getApkPath();
        if (TextUtils.isEmpty(apkPath)) {
            return false;
        }
        if (ps == null) {
            return true;
        }
        return (ps == null || ps.getPathString().equals(apkPath) || !ps.getPathString().startsWith(new StringBuilder().append(Environment.getDataDirectory().getPath()).append("/app").toString())) ? false : true;
    }

    private File deriveAppLib(String apkPath) {
        String trimmedInput = apkPath.startsWith("/") ? apkPath.substring(1) : apkPath;
        String[] parts = trimmedInput.split("/");
        String apkName = parts.length > 0 ? parts[parts.length - 1] : null;
        File applib = new File(ScanPackageUtils.getAppLib32InstallDir(), apkName);
        if (applib.exists()) {
            return applib;
        }
        return null;
    }

    private void removeNativeBinariesFromDir(File nativeLibraryRoot, boolean deleteRootDir) {
        Slog.w(TAG, "Deleting native binaries from: " + nativeLibraryRoot.getPath());
        if (nativeLibraryRoot.exists()) {
            File[] files = nativeLibraryRoot.listFiles();
            if (files != null) {
                for (int nn = 0; nn < files.length; nn++) {
                    String str = TAG;
                    Slog.d(str, "    Deleting " + files[nn].getName());
                    if (files[nn].isDirectory()) {
                        removeNativeBinariesFromDir(files[nn], true);
                    } else if (!files[nn].delete()) {
                        Slog.w(str, "Could not delete native binary: " + files[nn].getPath());
                    }
                }
            }
            if (deleteRootDir && !nativeLibraryRoot.delete()) {
                Slog.w(TAG, "Could not delete native binary directory: " + nativeLibraryRoot.getPath());
            }
        }
    }

    public void logEvent(int priority, String msg) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.pm.MiuiPreinstallHelper.LocalIntentReceiver.1
            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                LocalIntentReceiver.this.mResult.offer(intent);
            }
        };
        private final LinkedBlockingQueue<Intent> mResult;

        public LocalIntentReceiver(int capacity) {
            this.mResult = new LinkedBlockingQueue<>(capacity);
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public List<Intent> getResultsNoWait() {
            ArrayList<Intent> results = new ArrayList<>();
            try {
                this.mResult.drainTo(results);
            } catch (Exception e) {
            }
            return results;
        }

        public Intent getResult() {
            Intent intent = null;
            try {
                intent = this.mResult.poll(30L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Slog.e(MiuiPreinstallHelper.TAG, "LocalIntentReceiver poll timeout in 30 seconds.");
            }
            return intent != null ? intent : new Intent();
        }
    }
}
