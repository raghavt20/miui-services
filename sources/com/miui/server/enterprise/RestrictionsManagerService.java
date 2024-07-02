package com.miui.server.enterprise;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.net.IVpnManager;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.LocalePicker;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.server.content.MiSyncConstants;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.wm.WindowManagerService;
import com.miui.enterprise.IRestrictionsManager;
import com.miui.enterprise.RestrictionsHelper;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.stability.DumpSysInfoUtil;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import miui.securityspace.CrossUserUtils;

/* loaded from: classes.dex */
public class RestrictionsManagerService extends IRestrictionsManager.Stub {
    private static final String TAG = "Enterprise-restric";
    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private ComponentName mDeviceOwner;
    private IDevicePolicyManager mDevicePolicyManager;
    private Handler mHandler;
    private WindowManagerService mWindowManagerService = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
    private PackageManagerService.IPackageManagerImpl mPMS = ServiceManager.getService("package");
    private UserManagerService mUserManager = ServiceManager.getService("user");

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public void bootComplete() {
        Slog.d(TAG, "Restriction init");
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        List<UserInfo> users = userManager.getUsers();
        for (UserInfo user : users) {
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_screencapture", user.id)) {
                RestrictionManagerServiceProxy.setScreenCaptureDisabled(this.mWindowManagerService, this.mContext, user.id, true);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_vpn", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(47, true, this, null, user.id);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_fingerprint", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(55, true, this, null, user.id);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_imeiread", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(51, true, this, null, user.id);
            }
        }
        startWatchLocationRestriction();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RestrictionsManagerService(Context context) {
        this.mContext = context;
        IDevicePolicyManager asInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        this.mDevicePolicyManager = asInterface;
        try {
            this.mDeviceOwner = asInterface.getDeviceOwnerComponent(true);
        } catch (RemoteException e) {
        }
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHandler = new Handler();
    }

    private void startWatchLocationRestriction() {
        Uri uri = Settings.Secure.getUriFor("location_providers_allowed");
        this.mContext.getContentResolver().registerContentObserver(uri, true, new ContentObserver(this.mHandler) { // from class: com.miui.server.enterprise.RestrictionsManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                int currentUserId = CrossUserUtils.getCurrentUserId();
                int mode = EnterpriseSettings.getInt(RestrictionsManagerService.this.mContext, "gps_state", 1, currentUserId);
                if (mode == 4) {
                    Slog.d(RestrictionsManagerService.TAG, "FORCE_OPEN GPS");
                    Settings.Secure.putIntForUser(RestrictionsManagerService.this.mContext.getContentResolver(), "location_mode", 3, currentUserId);
                } else if (mode == 0) {
                    Slog.d(RestrictionsManagerService.TAG, "Close GPS");
                    Settings.Secure.putIntForUser(RestrictionsManagerService.this.mContext.getContentResolver(), "location_mode", 0, currentUserId);
                }
            }
        }, -1);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x0034, code lost:
    
        if (r8.equals("airplane_state") != false) goto L30;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setControlStatus(java.lang.String r8, int r9, int r10) {
        /*
            Method dump skipped, instructions count: 310
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.enterprise.RestrictionsManagerService.setControlStatus(java.lang.String, int, int):void");
    }

    private boolean shouldOpen(int state) {
        return state == 2 || state == 4;
    }

    private boolean shouldClose(int state) {
        return state == 0 || state == 3;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Multi-variable type inference failed */
    public void setRestriction(String str, boolean z, int i) {
        char c;
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, str, z ? 1 : 0, i);
        switch (str.hashCode()) {
            case -1915200762:
                if (str.equals("disallow_backup")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case -1886279575:
                if (str.equals("disallow_camera")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1859967211:
                if (str.equals("disallow_system_update")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -1552410727:
                if (str.equals("disallow_landscape_statusbar")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case -1425744347:
                if (str.equals("disallow_sdcard")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1395678890:
                if (str.equals("disallow_tether")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1094316185:
                if (str.equals("disallow_auto_sync")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -831735134:
                if (str.equals("disallow_imeiread")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -453687405:
                if (str.equals("disallow_usbdebug")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -429823771:
                if (str.equals("disallow_mtp")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -429821858:
                if (str.equals("disallow_otg")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -429815248:
                if (str.equals("disallow_vpn")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -208396879:
                if (str.equals("disallow_timeset")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -170808536:
                if (str.equals("disable_usb_device")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 82961609:
                if (str.equals("disallow_factoryreset")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 408143697:
                if (str.equals("disallow_safe_mode")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 411690763:
                if (str.equals("disallow_key_back")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 411883267:
                if (str.equals("disallow_key_home")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 412022659:
                if (str.equals("disallow_key_menu")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case 470637462:
                if (str.equals("disallow_screencapture")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 480222787:
                if (str.equals("disallow_change_language")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 487960096:
                if (str.equals("disallow_fingerprint")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 724096394:
                if (str.equals("disallow_status_bar")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 907380174:
                if (str.equals("disallow_mi_account")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 1157197624:
                if (str.equals("disable_accelerometer")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 1846688878:
                if (str.equals("disallow_microphone")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                boolean hasUserRestriction = this.mUserManager.hasUserRestriction("no_config_vpn", i);
                if (z && CrossUserUtils.getCurrentUserId() == i && !hasUserRestriction) {
                    try {
                        IVpnManager asInterface = IVpnManager.Stub.asInterface(ServiceManager.getService("vpn_management"));
                        VpnConfig vpnConfig = asInterface.getVpnConfig(UserHandle.myUserId());
                        if (vpnConfig != null) {
                            vpnConfig.configureIntent.send();
                        }
                        LegacyVpnInfo legacyVpnInfo = asInterface.getLegacyVpnInfo(UserHandle.myUserId());
                        if (legacyVpnInfo != null) {
                            legacyVpnInfo.intent.send();
                        }
                    } catch (Exception e) {
                        Slog.d(TAG, "Something wrong while close vpn: " + e);
                    }
                }
                this.mUserManager.setUserRestriction("no_config_vpn", z, i);
                this.mAppOpsManager.setUserRestrictionForUser(47, z, this, null, i);
                return;
            case 1:
                boolean hasUserRestriction2 = this.mUserManager.hasUserRestriction("no_config_tethering", i);
                if (z && i == CrossUserUtils.getCurrentUserId() && !hasUserRestriction2) {
                    RestrictionManagerServiceProxy.setWifiApEnabled(this.mContext, false);
                }
                this.mUserManager.setUserRestriction("no_config_tethering", z, i);
                return;
            case 2:
                this.mUserManager.setUserRestriction("no_camera", z, i);
                return;
            case 3:
                this.mUserManager.setUserRestriction("no_record_audio", z, i);
                return;
            case 4:
                RestrictionManagerServiceProxy.setScreenCaptureDisabled(this.mWindowManagerService, this.mContext, i, z);
                return;
            case 5:
                if (z && CrossUserUtils.getCurrentUserId() == i) {
                    unmountPublicVolume(4);
                    return;
                }
                return;
            case 6:
                UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
                this.mUserManager.setUserRestriction("no_usb_file_transfer", z, i);
                setUsbFunction(usbManager, "none");
                return;
            case 7:
            case 14:
            case 19:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
                return;
            case '\b':
                if (z && CrossUserUtils.getCurrentUserId() == i) {
                    unmountPublicVolume(8);
                    return;
                }
                return;
            case '\t':
                if (CrossUserUtils.getCurrentUserId() == i) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
                }
                this.mUserManager.setUserRestriction("no_debugging_features", z, i);
                return;
            case '\n':
                this.mAppOpsManager.setUserRestrictionForUser(55, z, this, null, i);
                return;
            case 11:
                this.mUserManager.setUserRestriction("no_factory_reset", z, i);
                return;
            case '\f':
                if (CrossUserUtils.getCurrentUserId() == i) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "auto_time", z ? 1 : 0);
                }
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "time_change_disallow", z ? 1 : 0, i);
                return;
            case '\r':
                this.mAppOpsManager.setUserRestrictionForUser(51, z, this, null, i);
                return;
            case 15:
                if (z) {
                    this.mPMS.setApplicationEnabledSetting("com.miui.backup", 2, 0, i, this.mContext.getPackageName());
                    return;
                } else {
                    this.mPMS.setApplicationEnabledSetting("com.miui.backup", 1, 0, i, this.mContext.getPackageName());
                    return;
                }
            case 16:
                if (z) {
                    ContentResolver.setMasterSyncAutomaticallyAsUser(false, i);
                    closeCloudBackup(i);
                    return;
                }
                return;
            case 17:
                this.mUserManager.setUserRestriction("no_safe_boot", z, i);
                return;
            case 18:
                LocalePicker.updateLocale(Locale.CHINA);
                return;
            case 20:
                StatusBarManager statusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
                if (statusBarManager == null) {
                    Log.e(TAG, "statusBarManager is null!");
                    return;
                } else {
                    statusBarManager.disable(z ? 65536 : 0);
                    return;
                }
            default:
                throw new IllegalArgumentException("Unknown restriction item: " + str);
        }
    }

    public int getControlStatus(String key, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, key, 1, userId);
    }

    public boolean hasRestriction(String key, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, key, 0, userId) == 1;
    }

    private void unmountPublicVolume(int volFlag) {
        final StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        VolumeInfo usbVol = null;
        Iterator it = storageManager.getVolumes().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            VolumeInfo vol = (VolumeInfo) it.next();
            if (vol.getType() == 0 && (vol.getDisk().flags & volFlag) == volFlag) {
                usbVol = vol;
                break;
            }
        }
        if (usbVol != null && usbVol.getState() == 2) {
            final String volId = usbVol.getId();
            new Thread(new Runnable() { // from class: com.miui.server.enterprise.RestrictionsManagerService.2
                @Override // java.lang.Runnable
                public void run() {
                    storageManager.unmount(volId);
                }
            }).start();
        }
    }

    private void closeCloudBackup(int userId) {
        Account account = null;
        AccountManager am = AccountManager.get(this.mContext);
        Account[] accounts = am.getAccountsByTypeAsUser(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE, new UserHandle(userId));
        if (accounts.length > 0) {
            account = accounts[0];
        }
        if (account != null) {
            ContentValues values = new ContentValues();
            values.put("account_name", account.name);
            values.put("is_open", (Boolean) false);
            Uri cloudBackupInfoUri = Uri.parse("content://com.miui.micloud").buildUpon().appendPath("cloud_backup_info").build();
            this.mContext.getContentResolver().update(ContentProvider.maybeAddUserId(cloudBackupInfoUri, userId), values, null, null);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.cloudbackup", "com.miui.cloudbackup.service.CloudBackupService"));
            intent.setAction("close_cloud_back_up");
            this.mContext.startServiceAsUser(intent, new UserHandle(userId));
        }
    }

    private void setUsbFunction(UsbManager usbManager, String function) {
        try {
            Method method = UsbManager.class.getDeclaredMethod("setCurrentFunction", String.class);
            method.setAccessible(true);
            method.invoke(usbManager, function);
        } catch (Exception e) {
            try {
                Method method2 = UsbManager.class.getDeclaredMethod("setCurrentFunction", String.class, Boolean.TYPE);
                method2.setAccessible(true);
                method2.invoke(usbManager, function, false);
            } catch (Exception e1) {
                Slog.d(TAG, "Failed to set usb function", e1);
            }
        }
    }
}
