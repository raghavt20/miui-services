package com.android.server.pm;

/* loaded from: classes.dex */
public class MiuiPreinstallApp {
    private String apkPath;
    private String packageName;
    private long versionCode;

    public MiuiPreinstallApp(String packageName, long versionCode, String apkPath) {
        this.packageName = packageName;
        this.versionCode = versionCode;
        this.apkPath = apkPath;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public long getVersionCode() {
        return this.versionCode;
    }

    public String getApkPath() {
        return this.apkPath;
    }
}
