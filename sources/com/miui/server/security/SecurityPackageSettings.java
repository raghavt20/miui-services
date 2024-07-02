package com.miui.server.security;

import com.android.server.MiuiUiModeManagerServiceStub;

/* loaded from: classes.dex */
public class SecurityPackageSettings {
    public boolean isDarkModeChecked;
    public String name;
    public boolean accessControl = false;
    public boolean childrenControl = false;
    public boolean maskNotification = false;
    public boolean isPrivacyApp = false;
    public boolean isGameStorageApp = false;
    public boolean isRemindForRelaunch = true;
    public boolean isRelaunchWhenFolded = false;
    public boolean isScRelaunchConfirm = true;

    public SecurityPackageSettings(String name) {
        this.name = name;
        this.isDarkModeChecked = MiuiUiModeManagerServiceStub.getInstance().getForceDarkAppDefaultEnable(name);
    }
}
