package com.miui.server.security;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.pm.PackageManagerServiceStub;
import com.android.server.pm.permission.DefaultPermissionGrantPolicyStub;
import com.miui.server.SecurityManagerService;
import miui.os.Build;

/* loaded from: classes.dex */
public class SecuritySettingsObserver extends ContentObserver {
    private final Uri mAccessControlLockConvenientUri;
    private final Uri mAccessControlLockEnabledUri;
    private final Uri mAccessControlLockModeUri;
    private final Uri mAccessMiuiOptimizationUri;
    private final Uri mAllowedDeviceProvisionedUri;
    private final Context mContext;
    private final SecurityManagerService mService;

    public SecuritySettingsObserver(SecurityManagerService service, Handler handler) {
        super(handler);
        this.mAccessControlLockEnabledUri = Settings.Secure.getUriFor("access_control_lock_enabled");
        this.mAccessControlLockModeUri = Settings.Secure.getUriFor("access_control_lock_mode");
        this.mAccessControlLockConvenientUri = Settings.Secure.getUriFor("access_control_lock_convenient");
        this.mAllowedDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
        this.mAccessMiuiOptimizationUri = Settings.Secure.getUriFor(DisplayModeDirectorImpl.MIUI_OPTIMIZATION);
        this.mService = service;
        this.mContext = service.mContext;
    }

    public void registerForSettingsChanged() {
        ContentResolver resolver = this.mService.mContext.getContentResolver();
        resolver.registerContentObserver(this.mAccessControlLockEnabledUri, false, this, -1);
        resolver.registerContentObserver(this.mAccessControlLockModeUri, false, this, -1);
        resolver.registerContentObserver(this.mAccessControlLockConvenientUri, false, this, -1);
        resolver.registerContentObserver(this.mAccessMiuiOptimizationUri, false, this, -1);
        if (Build.IS_INTERNATIONAL_BUILD && !this.mService.mAllowedDeviceProvision) {
            resolver.registerContentObserver(this.mAllowedDeviceProvisionedUri, false, this, 0);
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange, Uri uri, int userId) {
        if (this.mAccessMiuiOptimizationUri.equals(uri)) {
            updateAccessMiuiOptUri();
            return;
        }
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            if (this.mAccessControlLockEnabledUri.equals(uri)) {
                updateAccessControlEnabledLocked(userState);
            } else if (this.mAccessControlLockModeUri.equals(uri)) {
                updateAccessControlLockModeLocked(userState);
            } else if (this.mAccessControlLockConvenientUri.equals(uri)) {
                updateAccessControlLockConvenientLocked(userState);
            } else if (this.mAllowedDeviceProvisionedUri.equals(uri)) {
                updateAllowedDeviceProvisioned();
            }
            this.mService.mAccessControlImpl.updateMaskObserverValues();
        }
    }

    private void updateAccessControlEnabledLocked(SecurityUserState userState) {
        userState.mAccessControlEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_enabled", 0, userState.userHandle) == 1;
    }

    private void updateAccessControlLockModeLocked(SecurityUserState userState) {
        userState.mAccessControlLockMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_mode", 1, userState.userHandle);
    }

    private void updateAccessControlLockConvenientLocked(SecurityUserState userState) {
        userState.mAccessControlLockConvenient = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_convenient", 0, userState.userHandle) == 1;
    }

    private void updateAllowedDeviceProvisioned() {
        this.mService.mAllowedDeviceProvision = Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 1;
    }

    public void initAccessControlSettingsLocked(SecurityUserState userState) {
        if (userState.mAccessControlSettingInit) {
            return;
        }
        updateAccessControlEnabledLocked(userState);
        updateAccessControlLockModeLocked(userState);
        updateAccessControlLockConvenientLocked(userState);
        updateAllowedDeviceProvisioned();
        userState.mAccessControlSettingInit = true;
    }

    private void updateAccessMiuiOptUri() {
        if (AppOpsUtils.isXOptMode()) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                pm.setDefaultBrowserPackageNameAsUser("", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PackageManagerServiceStub.get().switchPackageInstaller();
        if (Build.IS_INTERNATIONAL_BUILD) {
            DefaultPermissionGrantPolicyStub.get().revokeAllPermssions();
        }
        if (!AppOpsUtils.isXOptMode()) {
            DefaultPermissionGrantPolicyStub.get().grantMiuiPackageInstallerPermssions();
            DefaultBrowserImpl.setDefaultBrowser(this.mContext);
        }
    }
}
