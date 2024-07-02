package com.android.server.pm;

import android.os.Build;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class MiuiPlatformPreinstallConfig extends MiuiPreinstallConfig {
    private static final String MIUI_PLATFORM_PREINSTALL_PATH = "/product/data-app";
    private Set<String> mNeedIgnoreSet = new HashSet();

    /* JADX INFO: Access modifiers changed from: protected */
    public MiuiPlatformPreinstallConfig() {
        initIgnoreSet();
    }

    private void initIgnoreSet() {
        if ("POCO".equals(Build.BRAND)) {
            this.mNeedIgnoreSet.add("com.mi.global.bbs");
            this.mNeedIgnoreSet.add("com.mi.global.shop");
        } else {
            this.mNeedIgnoreSet.add("com.mi.global.pocobbs");
            this.mNeedIgnoreSet.add("com.mi.global.pocostore");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<String> getLegacyPreinstallList(boolean isFirstBoot, boolean isDeviceUpgrading) {
        List<String> legacyPreinstallList = new ArrayList<>();
        return legacyPreinstallList;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public boolean needIgnore(String apkPath, String packageName) {
        if (!this.mNeedIgnoreSet.isEmpty()) {
            return this.mNeedIgnoreSet.contains(packageName);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.MiuiPreinstallConfig
    public List<File> getPreinstallDirs() {
        List<File> preinstallDirs = new ArrayList<>();
        preinstallDirs.add(new File(MIUI_PLATFORM_PREINSTALL_PATH));
        return preinstallDirs;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    protected List<File> getVanwardAppList() {
        return null;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    protected List<File> getCustAppList() {
        return null;
    }

    @Override // com.android.server.pm.MiuiPreinstallConfig
    protected boolean needLegacyPreinstall(String apkPath, String pkgName) {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isPlatformPreinstall(String path) {
        return path.startsWith(MIUI_PLATFORM_PREINSTALL_PATH);
    }
}
