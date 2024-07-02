package com.android.server.pm;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.io.IoUtils;
import miui.util.CustomizeUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
public class MiuiOperatorPreinstallConfig extends MiuiPreinstallConfig {
    private static final String ALLOW_INSTALL_THIRD_APP_LIST = "allow_install_third_app.list";
    private static final String DEL_APPS_LIST = "/product/opcust/common/del_apps_list.xml";
    private static final String DISALLOW_INSTALL_THIRD_APP_LIST = "disallow_install_third_app.list";
    private static final String O2SPACE_PACKAGE_NAME = "uk.co.o2.android.o2space";
    private static final String OPERATOR_PREINSTALL_INFO_DIR = "/mi_ext/product/etc/";
    private static final String OPERATOR_PRODUCT_DATA_APP_LIST = "operator_product_data_app.list";
    private static final String PRODUCT_CARRIER_DIR = "/product/opcust";
    private static final String TAG = MiuiOperatorPreinstallConfig.class.getSimpleName();
    private static final String MIUI_OPERATOR_PREINALL_PATH = "/product/opcust/data-app";
    private static final File PRODUCT_CARRIER_APP_DIR = new File(MIUI_OPERATOR_PREINALL_PATH);
    private static final File PRODUCT_DATA_APP_DIR = new File("/product/data-app");
    private static final String sCustomizedRegion = SystemProperties.get("ro.miui.customized.region", "");
    private final Set<String> sAllowOnlyInstallThirdApps = new HashSet();
    private final Set<String> sDisallowInstallThirdApps = new HashSet();
    private final Set<String> sCotaDelApps = new HashSet();

    /* JADX INFO: Access modifiers changed from: protected */
    public MiuiOperatorPreinstallConfig() {
        initCotaDelAppsList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<String> getLegacyPreinstallList(boolean isFirstBoot, boolean isDeviceUpgrading) {
        List<String> legacyPreinstallApkList = new ArrayList<>();
        new ArrayList();
        List<File> productCustomizedAppList = new ArrayList<>();
        Set<String> productDataAppSet = new HashSet<>();
        File productCustomizedAppListFile = new File(OPERATOR_PREINSTALL_INFO_DIR, OPERATOR_PRODUCT_DATA_APP_LIST);
        if (isDeviceUpgrading) {
            List<File> custAppList = getCustAppList();
            if (!custAppList.isEmpty()) {
                for (File app : custAppList) {
                    legacyPreinstallApkList.add(app.getPath());
                }
            }
        }
        if (productCustomizedAppListFile.exists()) {
            readLineToSet(productCustomizedAppListFile, productDataAppSet);
            addPreinstallAppToList(productCustomizedAppList, PRODUCT_DATA_APP_DIR, productDataAppSet);
            if (!productCustomizedAppList.isEmpty()) {
                for (File app2 : productCustomizedAppList) {
                    legacyPreinstallApkList.add(app2.getPath());
                }
            }
        }
        return legacyPreinstallApkList;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public boolean needIgnore(String apkPath, String packageName) {
        if (apkPath != null && packageName != null) {
            if (skipInstallByCota(apkPath, packageName)) {
                Slog.i(TAG, "cota skip install cust preinstall data-app, :" + packageName);
                return true;
            }
            if ("es_telefonica".equals(sCustomizedRegion) && skipInstallO2Space(packageName)) {
                Slog.i(TAG, "skip install O2Space");
                return true;
            }
            if (skipInstallDataAppByCota(packageName)) {
                Slog.i(TAG, "skip install app because cota-reject :" + packageName);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    protected List<File> getPreinstallDirs() {
        return null;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    protected List<File> getVanwardAppList() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<File> getCustAppList() {
        List<File> custAppList = new ArrayList<>();
        Set<String> productCarrierAppSet = new HashSet<>();
        File CarrierRegionAppFile = CustomizeUtil.getProductCarrierRegionAppFile();
        readAllowOnlyInstallThirdAppForCarrier();
        if (CarrierRegionAppFile != null && CarrierRegionAppFile.exists()) {
            readLineToSet(CarrierRegionAppFile, productCarrierAppSet);
            addPreinstallAppToList(custAppList, PRODUCT_CARRIER_APP_DIR, productCarrierAppSet);
        }
        return custAppList;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public boolean needLegacyPreinstall(String apkPath, String pkgName) {
        return isOperatorPreinstall(apkPath);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isOperatorPreinstall(String path) {
        return path.startsWith(MIUI_OPERATOR_PREINALL_PATH);
    }

    private void addPreinstallAppToList(List<File> preinstallAppList, File appDir, Set<String> filterSet) {
        int i;
        File apk;
        File[] apps = appDir.listFiles();
        if (apps != null) {
            int length = apps.length;
            while (i < length) {
                File app = apps[i];
                if (app.isDirectory()) {
                    apk = new File(app, app.getName() + ".apk");
                    i = apk.exists() ? 0 : i + 1;
                    if (filterSet != null || filterSet.contains(apk.getName())) {
                        preinstallAppList.add(app);
                    }
                } else {
                    apk = app;
                    if (apk.exists()) {
                        if (!apk.getPath().endsWith(".apk")) {
                        }
                        if (filterSet != null) {
                        }
                        preinstallAppList.add(app);
                    }
                }
            }
        }
    }

    private void readAllowOnlyInstallThirdAppForCarrier() {
        String cotaCarrier = SystemProperties.get("persist.sys.cota.carrier", "");
        if (!TextUtils.isEmpty(cotaCarrier) && !"XM".equals(cotaCarrier)) {
            try {
                String region = SystemProperties.get("ro.miui.region", "cn").toLowerCase();
                File customizedAppSetFile = new File(new File("/product/opcust/" + cotaCarrier, region), ALLOW_INSTALL_THIRD_APP_LIST);
                if (!customizedAppSetFile.exists()) {
                    customizedAppSetFile = new File(new File(PRODUCT_CARRIER_DIR, cotaCarrier), ALLOW_INSTALL_THIRD_APP_LIST);
                }
                if (customizedAppSetFile.exists()) {
                    readLineToSet(customizedAppSetFile, this.sAllowOnlyInstallThirdApps);
                    return;
                }
                File customizedAppSetFile2 = new File(new File("/product/opcust/" + cotaCarrier, region), DISALLOW_INSTALL_THIRD_APP_LIST);
                if (!customizedAppSetFile2.exists()) {
                    customizedAppSetFile2 = new File(new File(PRODUCT_CARRIER_DIR, cotaCarrier), DISALLOW_INSTALL_THIRD_APP_LIST);
                }
                if (customizedAppSetFile2.exists()) {
                    readLineToSet(customizedAppSetFile2, this.sDisallowInstallThirdApps);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error occurs when to read allow install third app list.");
            }
        }
    }

    private boolean skipInstallByCota(String apkFilePath, String pkgName) {
        if (apkFilePath == null || pkgName == null || apkFilePath == null || apkFilePath.contains("/system/data-app") || apkFilePath.contains("/vendor/data-app") || apkFilePath.contains("/product/data-app")) {
            return false;
        }
        return (this.sAllowOnlyInstallThirdApps.size() > 0 && !this.sAllowOnlyInstallThirdApps.contains(pkgName)) || (this.sDisallowInstallThirdApps.size() > 0 && this.sDisallowInstallThirdApps.contains(pkgName));
    }

    private boolean skipInstallO2Space(String pkgName) {
        return ("tef_o2".equals(SystemProperties.get("persist.sys.carrier.subnetwork", "")) || pkgName == null || !O2SPACE_PACKAGE_NAME.equals(pkgName)) ? false : true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:6:0x001d. Please report as an issue. */
    private void initCotaDelAppsList() {
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(DEL_APPS_LIST);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                for (int event = parser.getEventType(); event != 1; event = parser.next()) {
                    switch (event) {
                        case 0:
                        case 1:
                        default:
                        case 2:
                            String name = parser.getName();
                            if ("package".equals(name)) {
                                String pkgName = parser.getAttributeValue(null, "name");
                                if (TextUtils.isEmpty(pkgName)) {
                                    Slog.e(TAG, "initCotaDelApps pkgName is null, skip parse this tag");
                                } else {
                                    this.sCotaDelApps.add(pkgName);
                                }
                            }
                        case 3:
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "initCotaDelApps fail: " + e.getMessage());
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    private boolean skipInstallDataAppByCota(String pkgName) {
        if (this.sCotaDelApps.size() <= 0 || !this.sCotaDelApps.contains(pkgName)) {
            return false;
        }
        String cotaCarrier = SystemProperties.get("persist.sys.cota.carrier", "");
        return (TextUtils.isEmpty(cotaCarrier) || "XM".equals(cotaCarrier)) ? false : true;
    }
}
