package com.android.server.pm;

import android.app.AppGlobals;
import android.app.AppOpsManagerInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.miui.AppOpsUtils;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.app.IAppOpsCallback;
import com.android.server.LocalServices;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.pm.permission.DefaultPermissionGrantPolicyStub;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import miui.content.pm.ExtraPackageManager;

/* loaded from: classes.dex */
public class MiuiDefaultPermissionGrantPolicy extends DefaultPermissionGrantPolicyStub {
    private static final int DEFAULT_PACKAGE_INFO_QUERY_FLAGS = 536916096;
    private static final String GRANT_RUNTIME_VERSION = "persist.sys.grant_version";
    private static final String INCALL_UI = "com.android.incallui";
    private static final String REQUIRED_PERMISSIONS = "required_permissions";
    private static final String RUNTIME_PERMSSION_PROPTERY = "persist.sys.runtime_perm";
    private static final int STATE_DEF = -1;
    private static final int STATE_GRANT = 0;
    private static final int STATE_REVOKE = 1;
    private static final String TAG = "DefaultPermGrantPolicyI";
    private static final Set<String> RUNTIME_PERMISSIONS = new ArraySet();
    public static final String[] sAllowAutoStartForOTAPkgs = {"com.xiaomi.finddevice"};
    public static final String[] MIUI_SYSTEM_APPS = ExtraPackageManager.MIUI_SYSTEM_APPS;
    private static final String[] MIUI_GLOBAL_APPS = {"co.sitic.pp", "com.miui.backup", "com.xiaomi.finddevice"};
    private static final ArrayMap<String, ArrayList<String>> sMiuiAppDefaultGrantedPermissions = new ArrayMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDefaultPermissionGrantPolicy> {

        /* compiled from: MiuiDefaultPermissionGrantPolicy$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDefaultPermissionGrantPolicy INSTANCE = new MiuiDefaultPermissionGrantPolicy();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiDefaultPermissionGrantPolicy m2099provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiDefaultPermissionGrantPolicy m2098provideNewInstance() {
            return new MiuiDefaultPermissionGrantPolicy();
        }
    }

    private static boolean isDangerousPermission(String permission) {
        ensureDangerousSetInit();
        return RUNTIME_PERMISSIONS.contains(permission);
    }

    private static synchronized void ensureDangerousSetInit() {
        synchronized (MiuiDefaultPermissionGrantPolicy.class) {
            if (RUNTIME_PERMISSIONS.size() > 0) {
                return;
            }
            PermissionManagerServiceInternal pm = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            List<PermissionInfo> dangerous = pm.getAllPermissionsWithProtection(1);
            for (PermissionInfo permissionInfo : dangerous) {
                RUNTIME_PERMISSIONS.add(permissionInfo.name);
            }
        }
    }

    private static void grantPermissionsForCTS(int userId) {
        String miuiCustomizedRegion = SystemProperties.get("ro.miui.customized.region", "");
        if ("lm_cr".equals(miuiCustomizedRegion) || "mx_telcel".equals(miuiCustomizedRegion)) {
            PermissionManagerService permissionService = AppGlobals.getPermissionManager();
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.READ_PHONE_STATE", 2, 2, true, userId);
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.RECEIVE_SMS", 2, 2, true, userId);
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.CALL_PHONE", 2, 2, true, userId);
            Log.i(TAG, "grant permissions for Sysdll because of CTS");
        }
    }

    public void grantDefaultPermissions(int userId) {
        if (AppOpsUtils.isXOptMode() || (userId == 0 && TextUtils.equals(Build.VERSION.INCREMENTAL, SystemProperties.get(GRANT_RUNTIME_VERSION)))) {
            Log.i(TAG, "Don't need grant default permission to apps");
            if (AppOpsUtils.isXOptMode()) {
                grantPermissionsForCTS(userId);
                return;
            }
            return;
        }
        long startTime = System.currentTimeMillis();
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (miui.os.Build.IS_INTERNATIONAL_BUILD) {
            for (String miuiGlobalApp : MIUI_GLOBAL_APPS) {
                grantRuntimePermissionsLPw(service, miuiGlobalApp, false, userId);
            }
            Log.i(TAG, "grant permissions for miui global apps");
            if (SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, true)) {
                Log.i(TAG, "grant permissions for miui global apps: com.android.incallui");
                grantIncallUiPermission(service, userId);
            }
        } else {
            realGrantDefaultPermissions(service, userId);
        }
        SystemProperties.set(GRANT_RUNTIME_VERSION, Build.VERSION.INCREMENTAL);
        Log.i(TAG, "grantDefaultPermissions cost " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private static void realGrantDefaultPermissions(PackageManagerService service, int userId) {
        for (String miuiSystemApp : MIUI_SYSTEM_APPS) {
            grantRuntimePermissionsLPw(service, miuiSystemApp, true, userId);
        }
        AppOpsManagerInternal appOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
        for (String sAllowAutoStartForOTAPkg : sAllowAutoStartForOTAPkgs) {
            PackageInfo pkg = service.snapshotComputer().getPackageInfo(sAllowAutoStartForOTAPkg, 536916096L, userId);
            if (pkg != null) {
                appOpsManagerInternal.setModeFromPermissionPolicy(10008, pkg.applicationInfo.uid, sAllowAutoStartForOTAPkg, 0, (IAppOpsCallback) null);
            }
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageInfo pkg) {
        return pkg.applicationInfo.targetSdkVersion > 22;
    }

    /* JADX WARN: Code restructure failed: missing block: B:74:0x01c1, code lost:
    
        if ("android.permission.POST_NOTIFICATIONS".equals(r7) == false) goto L80;
     */
    /* JADX WARN: Removed duplicated region for block: B:78:0x01ee  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x020d  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x0203  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void grantRuntimePermissionsLPw(com.android.server.pm.PackageManagerService r24, java.lang.String r25, boolean r26, int r27) {
        /*
            Method dump skipped, instructions count: 589
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.MiuiDefaultPermissionGrantPolicy.grantRuntimePermissionsLPw(com.android.server.pm.PackageManagerService, java.lang.String, boolean, int):void");
    }

    private static boolean isUserChanged(int flags) {
        return (flags & 3) != 0;
    }

    private static boolean isOTAUpdated(int flags) {
        return ((flags & 2) == 0 || (flags & 32) == 0) ? false : true;
    }

    public static void setCoreRuntimePermissionEnabled(boolean grant, int flags, int userId) {
        if (userId != 0) {
            return;
        }
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (!grant) {
            SystemProperties.set(RUNTIME_PERMSSION_PROPTERY, String.valueOf(1));
        } else {
            realGrantDefaultPermissions(service, userId);
            SystemProperties.set(RUNTIME_PERMSSION_PROPTERY, String.valueOf(0));
        }
    }

    public static void grantRuntimePermission(String packageName, int userId) {
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (INCALL_UI.equals(packageName)) {
            grantIncallUiPermission(service, userId);
        }
    }

    private static void grantIncallUiPermission(PackageManagerService service, int userId) {
        ArrayList<String> perms = new ArrayList<>();
        perms.add("android.permission.READ_CONTACTS");
        perms.add("android.permission.POST_NOTIFICATIONS");
        PermissionManagerService permissionService = AppGlobals.getPermissionManager();
        Iterator<String> it = perms.iterator();
        while (it.hasNext()) {
            String p = it.next();
            int result = service.checkPermission(p, INCALL_UI, userId);
            if (result == -1) {
                permissionService.grantRuntimePermission(INCALL_UI, p, userId);
            }
        }
    }

    public void revokeAllPermssions() {
        if (!AppOpsUtils.isXOptMode()) {
            return;
        }
        SystemProperties.set(GRANT_RUNTIME_VERSION, "");
        try {
            PermissionManagerService permissionManagerService = AppGlobals.getPermissionManager();
            for (String pkg : sMiuiAppDefaultGrantedPermissions.keySet()) {
                ArrayList<String> permissions = sMiuiAppDefaultGrantedPermissions.get(pkg);
                if (!"com.google.android.packageinstaller".equals(pkg) && permissions != null) {
                    Iterator<String> it = permissions.iterator();
                    while (it.hasNext()) {
                        String p = it.next();
                        if (!"com.google.android.gms".equals(pkg) || (!"android.permission.RECORD_AUDIO".equals(p) && !"android.permission.ACCESS_FINE_LOCATION".equals(p))) {
                            try {
                                permissionManagerService.revokeRuntimePermission(pkg, p, 0, "revokeMiuiOpt");
                            } catch (Exception e) {
                                Log.d(TAG, "revokeAllPermssions error:" + e.toString());
                            }
                        }
                    }
                }
            }
            ArrayList<String> permissionList = new ArrayList<>();
            permissionList.add("android.permission.READ_EXTERNAL_STORAGE");
            permissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
            Iterator<String> it2 = permissionList.iterator();
            while (it2.hasNext()) {
                String permItem = it2.next();
                try {
                    permissionManagerService.revokeRuntimePermission("com.miui.packageinstaller", permItem, 0, "revokeMiuiOpt");
                } catch (Exception e2) {
                    Log.d(TAG, "revokeRuntimePermissionInternal error:" + e2.toString());
                }
            }
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    public void grantMiuiPackageInstallerPermssions() {
        List<String> permissionList = new ArrayList<>();
        permissionList.add("android.permission.READ_EXTERNAL_STORAGE");
        permissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        permissionList.add("android.permission.READ_PHONE_STATE");
        for (String permItem : permissionList) {
            try {
                PermissionManagerService permissionService = AppGlobals.getPermissionManager();
                permissionService.grantRuntimePermission("com.miui.packageinstaller", permItem, 0);
            } catch (Exception e) {
                Log.d(TAG, "grantMiuiPackageInstallerPermssions error:" + e.toString());
            }
        }
    }

    public boolean isSpecialUidNeedDefaultGrant(PackageInfo info) {
        PackageInfo sharedMetaInfo;
        if (UserHandle.getAppId(info.applicationInfo.uid) <= 2000 || UserHandle.getAppId(info.applicationInfo.uid) >= 10000) {
            return true;
        }
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        PackageInfo metaInfo = service.snapshotComputer().getPackageInfo(info.packageName, 128L, userId);
        if (metaInfo == null) {
            return true;
        }
        if (TextUtils.isEmpty(metaInfo.sharedUserId)) {
            return isAdaptedRequiredPermissions(metaInfo);
        }
        String[] sharedPackages = service.snapshotComputer().getPackagesForUid(info.applicationInfo.uid);
        if (sharedPackages == null) {
            return true;
        }
        for (String sharedPackage : sharedPackages) {
            if (TextUtils.equals(info.packageName, sharedPackage)) {
                sharedMetaInfo = metaInfo;
            } else {
                sharedMetaInfo = service.snapshotComputer().getPackageInfo(sharedPackage, 128L, userId);
            }
            if (!isAdaptedRequiredPermissions(sharedMetaInfo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdaptedRequiredPermissions(PackageInfo pi) {
        if (pi == null || pi.applicationInfo == null || pi.applicationInfo.metaData == null) {
            return false;
        }
        String permission = pi.applicationInfo.metaData.getString(REQUIRED_PERMISSIONS);
        return !TextUtils.isEmpty(permission);
    }

    public void miReadOutPermissionsInExt(ArrayList<File> ret) {
        File dir = new File("mi_ext/product", "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
    }
}
