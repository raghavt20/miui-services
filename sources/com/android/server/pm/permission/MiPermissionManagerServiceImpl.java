package com.android.server.pm.permission;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.miui.AppOpsUtils;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import java.util.HashSet;
import java.util.Set;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiPermissionManagerServiceImpl extends PermissionManagerServiceStub {
    private static final String TAG = "PackageManager";
    private static final boolean isRestrictImei;
    private static final Set<String> sAllowedList;
    private static final Set<String> sGetImeiMessage;
    private boolean mIsAndroidPermission;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiPermissionManagerServiceImpl> {

        /* compiled from: MiPermissionManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiPermissionManagerServiceImpl INSTANCE = new MiPermissionManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiPermissionManagerServiceImpl m2150provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiPermissionManagerServiceImpl m2149provideNewInstance() {
            return new MiPermissionManagerServiceImpl();
        }
    }

    static {
        isRestrictImei = !Build.IS_INTERNATIONAL_BUILD && "1".equals(SystemProperties.get("ro.miui.restrict_imei"));
        HashSet hashSet = new HashSet();
        sAllowedList = hashSet;
        hashSet.add("android");
        hashSet.add(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
        hashSet.add("com.miui.cit");
        hashSet.add("com.xiaomi.finddevice");
        hashSet.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        hashSet.add("com.android.settings");
        hashSet.add("com.android.vending");
        hashSet.add("com.google.android.gms");
        hashSet.add("com.xiaomi.factory.mmi");
        hashSet.add("com.miui.qr");
        hashSet.add("com.android.contacts");
        hashSet.add("com.xiaomi.registration");
        hashSet.add("com.miui.tsmclient");
        hashSet.add("com.miui.sekeytool");
        hashSet.add("com.android.updater");
        hashSet.add("com.miui.vipservice");
        hashSet.add("com.xiaomi.uatalive");
        hashSet.add("com.android.phone");
        hashSet.add("com.xiaomi.ab");
        if ("cn_chinamobile".equals(SystemProperties.get("ro.miui.cust_variant")) || "cn_chinatelecom".equals(SystemProperties.get("ro.miui.cust_variant"))) {
            hashSet.add("com.mobiletools.systemhelper");
            hashSet.add("com.miui.dmregservice");
        }
        HashSet hashSet2 = new HashSet();
        sGetImeiMessage = hashSet2;
        hashSet2.add("getImeiForSlot");
        hashSet2.add("getDeviceId");
        hashSet2.add("getMeidForSlot");
        hashSet2.add("getSmallDeviceId");
        hashSet2.add("getDeviceIdList");
        hashSet2.add("getSortedImeiList");
        hashSet2.add("getSortedMeidList");
        hashSet2.add("getMeid");
        hashSet2.add("getImei");
    }

    public boolean isAllowedAccessDeviceIdentifiers(String callingPackage, String message) {
        if (!isRestrictImei || AppOpsUtils.isXOptMode() || !sGetImeiMessage.contains(message) || TextUtils.isEmpty(callingPackage) || sAllowedList.contains(callingPackage)) {
            return true;
        }
        Slog.d(TAG, "MIUILOG- Permission Denied Get Telephony identifier :  pkg : " + callingPackage + " action : " + message);
        return false;
    }

    public boolean isNeedUpdatePermissions(PackageManagerInternal pm, String packageName, int[] allUsers, int[] affectedUsers) {
        PackageStateInternal ps;
        if (pm == null || allUsers == null || affectedUsers == null || (ps = pm.getPackageStateInternal(packageName)) == null) {
            return true;
        }
        for (int userId : allUsers) {
            PackageUserStateInternal state = ps.getUserStateOrDefault(userId);
            if (state != null && state.isInstalled() && !state.isHidden() && !ArrayUtils.contains(affectedUsers, userId)) {
                return false;
            }
        }
        return true;
    }

    public boolean isFlagPermission(Permission permission) {
        return permission != null && "android".equals(permission.getPackageName());
    }

    public int checkPhoneNumberAccess(Context context, String packageName, String message, String callingFeatureId, int pid, int uid) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
        if (appOpsManager.noteOpNoThrow(10032, uid, packageName, callingFeatureId, message) != 0) {
            return 1;
        }
        return 0;
    }
}
