package com.android.server.pm;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.content.F2fsUtils;
import com.android.server.pm.pkg.AndroidPackage;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class MiuiPreinstallCompressImpl extends MiuiPreinstallCompressStub {
    private static final String MIUI_APP_PREDIR = "/product/data-app";
    private static final String PARTNER_DIR_PREFIX = "partner";
    private static final String RANDOM_DIR_PREFIX = "~~";
    private static final String TAG = "MiuiPreinstallComprImpl";
    private static Handler mHandler = null;
    private static boolean mIsDeviceUpgrading = false;
    private static boolean mIsFirstBoot = false;
    private static final String preinstallPackageList = "/data/app/preinstall_package_path";
    private MiuiPreinstallHelper mMiuiPreinstallHelper;
    private PackageManagerService mPms;
    private static final boolean REGION_IS_CN = SystemProperties.get("ro.miui.region", "unknown").equalsIgnoreCase("cn");
    private static final boolean OTA_COMPR_ENABLE = SystemProperties.getBoolean("ro.miui.ota_compr_enable", false);
    private static final boolean F2FS_COMPR_SUPPORT = F2fsUtils.isCompressSupport();
    private static List<String> mPackageList = getPackageList();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiPreinstallCompressImpl> {

        /* compiled from: MiuiPreinstallCompressImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiPreinstallCompressImpl INSTANCE = new MiuiPreinstallCompressImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiPreinstallCompressImpl m2104provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiPreinstallCompressImpl m2103provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.MiuiPreinstallCompressImpl is marked as singleton");
        }
    }

    public void init(PackageManagerService pms) {
        Slog.d(TAG, "init Miui Preinstall Compress Stub.");
        this.mPms = pms;
        mIsFirstBoot = pms.isFirstBoot();
        mIsDeviceUpgrading = this.mPms.isDeviceUpgrading();
        this.mMiuiPreinstallHelper = MiuiPreinstallHelper.getInstance();
        HandlerThread handlerThread = new HandlerThread("Compress");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    public void compressPreinstallAppFirstBootOrUpgrade(AndroidPackage pkg, int scanFlags) {
        if (this.mMiuiPreinstallHelper.isSupportNewFrame()) {
            if (isCompressApkFirstBootOrUpgradeNewFrame(pkg.getNativeLibraryDir(), scanFlags)) {
                compressLib(pkg.getNativeLibraryDir());
            }
        } else if (isCompressApkFirstBootOrUpgradeOldFrame(pkg.getPackageName(), pkg.getNativeLibraryDir())) {
            compressLib(pkg.getNativeLibraryDir());
        }
    }

    private boolean isCompressApkFirstBootOrUpgradeNewFrame(String rootLibPath, int scanFlags) {
        if ((1073741824 & scanFlags) != 0 && REGION_IS_CN && F2FS_COMPR_SUPPORT && mIsFirstBoot) {
            return true;
        }
        return false;
    }

    private boolean isCompressApkFirstBootOrUpgradeOldFrame(String pkgName, String rootLibPath) {
        List<String> list;
        if (OTA_COMPR_ENABLE && (list = mPackageList) != null && list.contains(pkgName) && !rootLibPath.contains(RANDOM_DIR_PREFIX)) {
            return true;
        }
        return false;
    }

    public void compressPreinstallAppInstall(AndroidPackage pkg) {
        if (isCompressApkInstallNewFrame(pkg)) {
            compressLib(pkg.getNativeLibraryDir());
        } else if (isCompressApkInstallOldFrame(pkg.getPackageName())) {
            compressLib(pkg.getNativeLibraryDir());
        }
    }

    private boolean isCompressApkInstallNewFrame(AndroidPackage pkg) {
        if (this.mMiuiPreinstallHelper.isMiuiPreinstallApp(pkg.getPackageName()) && REGION_IS_CN && F2FS_COMPR_SUPPORT) {
            return true;
        }
        return false;
    }

    private boolean isCompressApkInstallOldFrame(String pkgName) {
        List<String> list;
        if (OTA_COMPR_ENABLE && (list = mPackageList) != null && list.contains(pkgName)) {
            return true;
        }
        return false;
    }

    private void compressLib(final String libDir) {
        mHandler.post(new Runnable() { // from class: com.android.server.pm.MiuiPreinstallCompressImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                F2fsUtils.compressAndReleaseBlocks(new File(libDir));
            }
        });
    }

    private static List<String> getPackageList() {
        if (!OTA_COMPR_ENABLE) {
            return null;
        }
        File file = new File(preinstallPackageList);
        if (!file.exists()) {
            Slog.d(TAG, "Preinstall file is not exist.");
            return null;
        }
        List<String> packageList = new ArrayList<>();
        InputStreamReader read = null;
        BufferedReader buffer = null;
        try {
            try {
                try {
                    InputStream inputStream = new FileInputStream(preinstallPackageList);
                    read = new InputStreamReader(inputStream, "utf-8");
                    buffer = new BufferedReader(read);
                    while (true) {
                        String line = buffer.readLine();
                        if (line == null) {
                            try {
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (line.contains(MIUI_APP_PREDIR)) {
                            String name = line.substring(0, line.indexOf(":"));
                            packageList.add(name);
                        }
                    }
                    read.close();
                    try {
                        buffer.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                } finally {
                }
            } catch (FileNotFoundException e3) {
                e3.printStackTrace();
                if (read != null) {
                    try {
                        read.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
                if (buffer != null) {
                    buffer.close();
                }
            } catch (UnsupportedEncodingException e5) {
                e5.printStackTrace();
                if (read != null) {
                    try {
                        read.close();
                    } catch (IOException e6) {
                        e6.printStackTrace();
                    }
                }
                if (buffer != null) {
                    buffer.close();
                }
            } catch (IOException e7) {
                e7.printStackTrace();
                if (read != null) {
                    try {
                        read.close();
                    } catch (IOException e8) {
                        e8.printStackTrace();
                    }
                }
                if (buffer != null) {
                    buffer.close();
                }
            }
        } catch (IOException e9) {
            e9.printStackTrace();
        }
        return packageList;
    }
}
