package com.android.server.pm;

import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import miui.os.CustVerifier;
import miui.util.CustomizeUtil;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiBusinessPreinstallConfig extends MiuiPreinstallConfig {
    private static final String CLOUD_CONTROL_OFFLINE_PACKAGE_LIST = "/data/system/cloud_control_offline_package.list";
    private String deviceName;
    private static final String TAG = MiuiBusinessPreinstallConfig.class.getSimpleName();
    public static ArrayList<String> sCloudControlUninstall = new ArrayList<>();
    private static final File CUSTOMIZED_APP_DIR = CustomizeUtil.getMiuiCustomizedAppDir();
    private static final String BUSINESS_PREINSTALL_IN_DATA_PATH = "/data/miui/app/recommended";
    private static final File RECOMMENDED_APP_DIR = new File(BUSINESS_PREINSTALL_IN_DATA_PATH);
    private static final String MIUI_BUSINESS_PREINALL_PATH = "/cust/app/customized";
    private static final File MIUI_BUSINESS_PREINALL_DIR = new File(MIUI_BUSINESS_PREINALL_PATH);
    private static final String CUST_MIUI_PREINSTALL_PATH = "/cust/data-app";
    private static final File CUST_MIUI_PREINSTALL_DIR = new File(CUST_MIUI_PREINSTALL_PATH);
    private static final File OTA_SKIP_BUSINESS_APP_LIST_FILE = new File("/system/etc/ota_skip_apps");
    private Set<String> mCloudControlOfflinePackages = new HashSet();
    private final Set<String> NOT_OTA_PACKAGE_NAMES = new HashSet();
    private boolean hasLoadGlobalLegacyPreinstall = false;
    List<String> listLegacyApkPath = new ArrayList();

    /* JADX INFO: Access modifiers changed from: protected */
    public MiuiBusinessPreinstallConfig() {
        readCloudControlOfflinePackage();
        MiuiPAIPreinstallConfig.init();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<File> getPreinstallDirs() {
        List<File> preinstallDirs = new ArrayList<>();
        preinstallDirs.add(MIUI_BUSINESS_PREINALL_DIR);
        preinstallDirs.add(RECOMMENDED_APP_DIR);
        return preinstallDirs;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<File> getCustMiuiPreinstallDirs() {
        List<File> custMiuiPreinstallDirs = new ArrayList<>();
        custMiuiPreinstallDirs.add(CUST_MIUI_PREINSTALL_DIR);
        return custMiuiPreinstallDirs;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    public boolean needIgnore(String apkPath, String packageName) {
        if (TextUtils.isEmpty(apkPath)) {
            Slog.i(TAG, "apkPath is null, won't install.");
            return true;
        }
        File apk = new File(apkPath);
        if (!apk.exists()) {
            Slog.i(TAG, "apk is not exist, won't install, apkPath=" + apkPath);
            return true;
        }
        if (sCloudControlUninstall.contains(packageName)) {
            Slog.i(TAG, "CloudControlUninstall apk won't install, packageName=" + packageName);
            return true;
        }
        if (skipOTA(packageName)) {
            Slog.i(TAG, " not support ota, packageName=" + packageName);
            return true;
        }
        if (isUninstallByMccOrMnc(packageName)) {
            Slog.i(TAG, "region or mcc not support,won't install, packageName=" + packageName);
            return true;
        }
        return signCheckFailed(apkPath);
    }

    private boolean signCheckFailed(String apkPath) {
        if (Build.IS_DEBUGGABLE || miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return false;
        }
        boolean custAppSupportSign = supportSignVerifyInCust() && apkPath.contains(MIUI_BUSINESS_PREINALL_PATH);
        String str = TAG;
        Slog.i(str, "Sign support is " + custAppSupportSign);
        if (custAppSupportSign && CustVerifier.getInstance() == null) {
            Slog.i(str, "CustVerifier init error !" + apkPath + " will won't install.");
            return true;
        }
        File baseApkFile = getBaseApkFile(new File(apkPath));
        if (!custAppSupportSign || CustVerifier.getInstance().verifyApkSignatue(baseApkFile.getPath(), 0)) {
            return false;
        }
        Slog.i(str, baseApkFile.getPath() + " verify failed!");
        return true;
    }

    private boolean supportSignVerifyInCust() {
        return FeatureParser.getBoolean("support_sign_verify_in_cust", false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isBusinessPreinstall(String path) {
        return path.startsWith(MIUI_BUSINESS_PREINALL_PATH) || path.startsWith(BUSINESS_PREINSTALL_IN_DATA_PATH);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isCustMiuiPreinstall(String path) {
        return path.startsWith(CUST_MIUI_PREINSTALL_PATH);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<String> getLegacyPreinstallList(boolean isFirstBoot, boolean isDeviceUpgrading) {
        if (isDeviceUpgrading) {
            readLineToSet(OTA_SKIP_BUSINESS_APP_LIST_FILE, this.NOT_OTA_PACKAGE_NAMES);
        }
        List<String> legacyPreinstallApkList = new ArrayList<>();
        Set<String> legacyPreinstallApkSet = new HashSet<>();
        readLineToSet(new File(CustomizeUtil.getMiuiCustomizedDir(), "app_legacy_install_list"), legacyPreinstallApkSet);
        addPreinstallAppPathToList(legacyPreinstallApkList, CUSTOMIZED_APP_DIR, legacyPreinstallApkSet);
        addPreinstallAppPathToList(legacyPreinstallApkList, RECOMMENDED_APP_DIR, legacyPreinstallApkSet);
        return legacyPreinstallApkList;
    }

    private boolean skipOTA(String packageName) {
        try {
            if (TextUtils.isEmpty(this.deviceName)) {
                this.deviceName = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class).invoke(null, "ro.product.name", "");
            }
            if ("sagit".equals(this.deviceName) && "com.xiaomi.youpin".equals(packageName)) {
                Slog.i(TAG, "skipOTA, deviceName is " + this.deviceName);
                return false;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Get exception", e);
        }
        return this.NOT_OTA_PACKAGE_NAMES.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<File> getVanwardAppList() {
        List<File> custAppList = getCustAppList();
        List<File> vanwardAppList = new ArrayList<>();
        Set<String> vanwardCustAppSet = new HashSet<>();
        readLineToSet(new File(CustomizeUtil.getMiuiAppDir(), "vanward_applist"), vanwardCustAppSet);
        if (custAppList.isEmpty() || vanwardCustAppSet.isEmpty()) {
            Slog.w(TAG, "No vanward cust app need to install");
            return vanwardAppList;
        }
        for (File appDir : custAppList) {
            File apkFile = getBaseApkFile(appDir);
            if (vanwardCustAppSet.contains(apkFile.getName())) {
                vanwardAppList.add(appDir);
            }
        }
        return vanwardAppList;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<File> getCustAppList() {
        Slog.w(TAG, "getCustAppList");
        List<File> custAppList = new ArrayList<>();
        File custVariantDir = CustomizeUtil.getMiuiCustVariantDir();
        Set<String> customizedAppSet = new HashSet<>();
        Set<String> recommendedAppSet = new HashSet<>();
        if (custVariantDir != null) {
            readLineToSet(new File(custVariantDir, "customized_applist"), customizedAppSet);
            readLineToSet(new File(custVariantDir, "ota_customized_applist"), customizedAppSet);
            readLineToSet(new File(custVariantDir, "recommended_applist"), recommendedAppSet);
            readLineToSet(new File(custVariantDir, "ota_recommended_applist"), recommendedAppSet);
        }
        addPreinstallAppToList(custAppList, CUSTOMIZED_APP_DIR, customizedAppSet);
        addPreinstallAppToList(custAppList, RECOMMENDED_APP_DIR, recommendedAppSet);
        return custAppList;
    }

    private boolean isValidIme(String locale, Locale curLocale) {
        String[] locales = locale.split(",");
        for (int i = 0; i < locales.length; i++) {
            if (locales[i].startsWith(curLocale.toString()) || locales[i].equals("*") || locales[i].startsWith(curLocale.getLanguage() + "_*")) {
                return true;
            }
        }
        return false;
    }

    private void addPreinstallAppPathToList(List<String> preinstallAppList, File appDir, Set<String> filterSet) {
        File[] apps = appDir.listFiles();
        if (apps != null) {
            for (File app : apps) {
                if (app.isDirectory()) {
                    File apk = getBaseApkFile(app);
                    if (apk.exists() && filterSet != null) {
                        for (String pkgName : filterSet) {
                            if (apk.getName().contains(pkgName)) {
                                preinstallAppList.add(app.getPath());
                            }
                        }
                    }
                }
            }
        }
    }

    private void addPreinstallAppToList(List<File> preinstallAppList, File appDir, Set<String> filterSet) {
        File[] apps = appDir.listFiles();
        if (apps != null) {
            for (File app : apps) {
                if (app.isDirectory()) {
                    File apk = getBaseApkFile(app);
                    if (apk.exists() && (filterSet == null || filterSet.contains(apk.getName()))) {
                        preinstallAppList.add(app);
                    }
                }
            }
        }
    }

    private File getBaseApkFile(File dir) {
        return new File(dir, dir.getName() + ".apk");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public boolean needLegacyPreinstall(String apkPath, String pkgName) {
        if (!this.hasLoadGlobalLegacyPreinstall) {
            this.listLegacyApkPath = getLegacyPreinstallList(false, false);
            this.hasLoadGlobalLegacyPreinstall = true;
        }
        return this.listLegacyApkPath.contains(apkPath);
    }

    public ArrayList<String> getPeinstalledChannelList() {
        ArrayList<String> preinstalledChannelList = new ArrayList<>();
        File custVariantDir = CustomizeUtil.getMiuiCustVariantDir();
        if (custVariantDir != null) {
            String custVariantPath = custVariantDir.getPath();
            File file = CUSTOMIZED_APP_DIR;
            addPreinstallChannelToList(preinstalledChannelList, file, custVariantPath + "/customized_channellist");
            addPreinstallChannelToList(preinstalledChannelList, file, custVariantPath + "/ota_customized_channellist");
            File file2 = RECOMMENDED_APP_DIR;
            addPreinstallChannelToList(preinstalledChannelList, file2, custVariantPath + "/recommended_channellist");
            addPreinstallChannelToList(preinstalledChannelList, file2, custVariantPath + "/ota_recommended_channellist");
        }
        return preinstalledChannelList;
    }

    private void addPreinstallChannelToList(List<String> preinstallChannelList, File channelDir, String channelListFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(channelListFile));
            while (true) {
                String channelName = reader.readLine();
                if (channelName != null) {
                    preinstallChannelList.add(channelDir.getPath() + "/" + channelName);
                } else {
                    reader.close();
                    return;
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs while read preinstalled channels " + e);
        }
    }

    private void readCloudControlOfflinePackage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CLOUD_CONTROL_OFFLINE_PACKAGE_LIST));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line != null) {
                        if (!TextUtils.isEmpty(line)) {
                            this.mCloudControlOfflinePackages.add(line);
                        }
                    } else {
                        reader.close();
                        return;
                    }
                } finally {
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs while offline preinstall packages " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isCloudOfflineApp(String pkg) {
        return !this.mCloudControlOfflinePackages.isEmpty() && this.mCloudControlOfflinePackages.contains(pkg);
    }

    public void removeFromPreinstallList(String pkg) {
        if (!this.mCloudControlOfflinePackages.contains(pkg)) {
            writeOfflinePreinstallPackage(pkg);
        }
    }

    private void writeOfflinePreinstallPackage(String pkg) {
        Slog.i(TAG, "Write offline preinstall package name into /data/system/cloud_control_offline_package.list");
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        try {
            BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(CLOUD_CONTROL_OFFLINE_PACKAGE_LIST, true));
            try {
                synchronized (this.mCloudControlOfflinePackages) {
                    this.mCloudControlOfflinePackages.add(pkg);
                    bufferWriter.write(pkg);
                    bufferWriter.write("\n");
                }
                bufferWriter.close();
            } finally {
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs when to write offline preinstall package name.");
        }
    }

    public Map<String, String> getAdvanceApps() {
        Map<String, String> sAdvanceApps = new HashMap<>();
        List<MiuiPreinstallApp> preinstallApps = MiuiPreinstallHelper.getInstance().getPreinstallApps();
        for (MiuiPreinstallApp app : preinstallApps) {
            if (app.getApkPath() != null && (app.getApkPath().contains(AdvancePreinstallService.ADVANCE_TAG) || app.getApkPath().contains(AdvancePreinstallService.ADVERT_TAG))) {
                sAdvanceApps.put(app.getPackageName(), app.getApkPath());
            }
        }
        return sAdvanceApps;
    }

    private boolean isUninstallByMccOrMnc(String packageName) {
        if (!"com.yandex.searchapp".equals(packageName)) {
            return false;
        }
        String sku = SystemProperties.get("ro.miui.build.region", "");
        if (!TextUtils.equals(sku, "eea") && !TextUtils.equals(sku, "global")) {
            return false;
        }
        boolean isUninstalled = !"ru_operator".equals(SystemProperties.get("persist.sys.carrier.subnetwork", ""));
        return isUninstalled;
    }
}
