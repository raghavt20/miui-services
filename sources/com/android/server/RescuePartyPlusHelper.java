package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.PackageManagerServiceUtils;
import com.miui.server.AccessController;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import miui.app.StorageRestrictedPathManager;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class RescuePartyPlusHelper {
    private static final String CLOUD_CONTROL_FILE = "/data/mqsas/cloud/rescuePartyPlusCloudControl.json";
    private static final String CONFIG_RESET_PROCESS = "sys.rescuepartyplus.config_reset_process";
    private static final String DEVELOPMENT_PROP = "ro.mi.development";
    private static final String PRODUCT_PROP = "ro.build.product";
    private static final String PROP_BOOTCOMPLETE = "sys.boot_completed";
    private static final String PROP_DISABLE_AUTORESTART_APP_PREFIX = "sys.rescuepartyplus.disable_autorestart.";
    private static final String PROP_READY_SHOW_RESET_CONFIG_UI = "sys.rescuepartyplus.ready_show_reset_config_ui";
    private static final String RESCUEPARTY_DEBUG_PROC = "persist.sys.rescuepartyplus.debug";
    private static final String RESCUEPARTY_LAST_RESET_CONFIG_PROP = "persist.sys.rescuepartyplus.last_reset_config";
    private static final String RESCUEPARTY_PLUS_DISABLE_PROP = "persist.sys.rescuepartyplus.disable";
    private static final String RESCUEPARTY_PLUS_ENABLE_PROP = "persist.sys.rescuepartyplus.enable";
    private static final String RESCUEPARTY_TEMP_MITIGATION_COUNT_PROP = "sys.rescuepartyplus.temp_mitigation_count";
    static final String TAG = "RescuePartyPlus";
    private static final String THEME_DATA = "/data/system/theme";
    private static final String THEME_MAGIC_DATA = "/data/system/theme_magic";
    private static final Set<String> RESET_CONFIG_DEFAULT_SET = new HashSet<String>() { // from class: com.android.server.RescuePartyPlusHelper.1
        {
            add("/data/system/users/0/app_idle_stats.xml");
            add("/data/system/users/0/settings_system.xml");
            add("/data/system/users/0/settings_system.xml.fallback");
            add("/data/system/users/0/settings_secure.xml");
            add("/data/system/users/0/settings_secure.xml.fallback");
        }
    };
    private static final Set<String> CORE_PACKAGE = new HashSet<String>() { // from class: com.android.server.RescuePartyPlusHelper.2
        {
            add("android");
        }
    };
    private static final Set<String> UI_PACKAGE = new HashSet<String>() { // from class: com.android.server.RescuePartyPlusHelper.3
    };
    private static final Set<String> TOP_UI_PACKAGE = new HashSet<String>() { // from class: com.android.server.RescuePartyPlusHelper.4
        {
            add(AccessController.PACKAGE_SYSTEMUI);
        }
    };

    private static String getProductInfo() {
        return SystemProperties.get(PRODUCT_PROP, "None");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getMitigationTempCount() {
        return SystemProperties.getInt(RESCUEPARTY_TEMP_MITIGATION_COUNT_PROP, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setMitigationTempCount(int mitigationTempCount) {
        SystemProperties.set(RESCUEPARTY_TEMP_MITIGATION_COUNT_PROP, String.valueOf(mitigationTempCount));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean getConfigResetProcessStatus() {
        return SystemProperties.getBoolean(CONFIG_RESET_PROCESS, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setConfigResetProcessStatus(boolean status) {
        SystemProperties.set(CONFIG_RESET_PROCESS, status ? "1" : "0");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean getLastResetConfigStatus() {
        return SystemProperties.getBoolean(RESCUEPARTY_LAST_RESET_CONFIG_PROP, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setLastResetConfigStatus(boolean status) {
        SystemProperties.set(RESCUEPARTY_LAST_RESET_CONFIG_PROP, status ? "1" : "0");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean getShowResetConfigUIStatus() {
        return SystemProperties.getBoolean(PROP_READY_SHOW_RESET_CONFIG_UI, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setShowResetConfigUIStatus(boolean status) {
        SystemProperties.set(PROP_READY_SHOW_RESET_CONFIG_UI, status ? "1" : "0");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkBootCompletedStatus() {
        return SystemProperties.getBoolean(PROP_BOOTCOMPLETE, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkPackageIsCore(String packageName) {
        if (packageName == null) {
            return false;
        }
        return CORE_PACKAGE.contains(packageName);
    }

    static boolean checkPackageIsUI(String packageName) {
        if (packageName == null) {
            return false;
        }
        return UI_PACKAGE.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkPackageIsTOPUI(String packageName) {
        if (packageName == null) {
            return false;
        }
        return TOP_UI_PACKAGE.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void disableAppRestart(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            Slog.e(TAG, "Disable app auto restart failed, because package is empty: " + packageName);
            return;
        }
        Slog.w(TAG, "Disable app auto restart: " + packageName);
        SystemProperties.set(PROP_DISABLE_AUTORESTART_APP_PREFIX + packageName, "1");
        PackageManagerServiceUtils.logCriticalInfo(3, "Finished rescue level DISABLE_APP for package " + packageName);
    }

    static boolean enableDebugStatus() {
        return SystemProperties.getBoolean(RESCUEPARTY_DEBUG_PROC, false) || SystemProperties.getBoolean(DEVELOPMENT_PROP, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkDisableRescuePartyPlus() {
        if (SystemProperties.getBoolean(RESCUEPARTY_PLUS_DISABLE_PROP, false)) {
            Slog.w(TAG, "RescueParty Plus is disable!");
            return true;
        }
        if (!SystemProperties.getBoolean(RESCUEPARTY_PLUS_ENABLE_PROP, false)) {
            return false;
        }
        Slog.w(TAG, "This device support and enable RescuePartyPlus! (Via cloud control)");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getLauncherPackageName(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        if (res == null || res.activityInfo == null || res.activityInfo.packageName.equals("android")) {
            return null;
        }
        return res.activityInfo.packageName;
    }

    static boolean delete(File deleteFile, Set<String> saveDir) {
        File[] files;
        boolean result = true;
        if (deleteFile.isFile()) {
            boolean result2 = true & deleteFile.delete();
            return result2;
        }
        if (!deleteFile.isDirectory() || (files = deleteFile.listFiles()) == null) {
            return true;
        }
        for (File deleteInFile : files) {
            if (deleteInFile.isFile()) {
                result &= deleteInFile.delete();
            } else if (deleteInFile.isDirectory()) {
                result &= delete(deleteInFile, saveDir);
            }
        }
        if (!saveDir.contains(deleteFile.getAbsolutePath())) {
            deleteFile.delete();
            return result;
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean resetTheme(String failedPackage) {
        Slog.w(TAG, "Preparing to reset theme: " + failedPackage);
        HashSet<String> saveDir = new HashSet<>();
        boolean result = true;
        saveDir.add(THEME_DATA);
        saveDir.add(THEME_MAGIC_DATA);
        if (!delete(new File(THEME_DATA), saveDir)) {
            Slog.e(TAG, "Delete theme files failed: " + failedPackage);
            result = false;
        }
        if (!delete(new File(THEME_MAGIC_DATA), saveDir)) {
            Slog.e(TAG, "Delete magic theme files failed: " + failedPackage);
            return false;
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Set<String> tryGetCloudControlOrDefaultData() {
        File cloudControlFile = new File(CLOUD_CONTROL_FILE);
        HashSet<String> cloudControlData = new HashSet<>();
        if (cloudControlFile.exists()) {
            Slog.w(TAG, "Use rescueparty plus cloud control data!");
            try {
                FileReader fileReader = new FileReader(cloudControlFile);
                try {
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        while (true) {
                            String str = bufferedReader.readLine();
                            if (str == null) {
                                break;
                            }
                            stringBuilder.append(str);
                        }
                        String originData = stringBuilder.toString();
                        if (!originData.isEmpty()) {
                            JSONObject controlJSONObject = new JSONObject(originData);
                            String controlData = controlJSONObject.optString("controlData");
                            String[] deleteFiles = controlData.split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
                            for (String filePath : deleteFiles) {
                                if (!filePath.isEmpty()) {
                                    cloudControlData.add(filePath);
                                }
                            }
                        } else {
                            Slog.e(TAG, "Cloud control data is empty!");
                        }
                        bufferedReader.close();
                        fileReader.close();
                    } finally {
                    }
                } catch (Throwable th) {
                    try {
                        fileReader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                Slog.e(TAG, "Read cloud control data failed!", e);
            } catch (JSONException e2) {
                Slog.e(TAG, "Parse cloud control data failed!", e2);
            }
        } else {
            cloudControlData.addAll(RESET_CONFIG_DEFAULT_SET);
        }
        return cloudControlData;
    }
}
