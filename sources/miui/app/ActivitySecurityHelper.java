package miui.app;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.miui.AppOpsUtils;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import miui.os.Build;
import miui.security.AppRunningControlManager;
import miui.security.SecurityManager;
import miui.security.SecurityManagerInternal;
import miui.security.WakePathChecker;

/* loaded from: classes.dex */
public class ActivitySecurityHelper {
    private static final String PACKAGE_ANDRIOD = "android";
    private static final String PACKAGE_SECURITYCENTER = "com.miui.securitycenter";
    private static final String START_ACTIVITY_CALLEE_PKGNAME = "CalleePkgName";
    private static final String START_ACTIVITY_CALLEE_UID = "calleeUserId";
    private static final String START_ACTIVITY_CALLER_PKGNAME = "CallerPkgName";
    private static final String START_ACTIVITY_CALLER_UID = "callerUserId";
    private static final String START_ACTIVITY_DIALOG_TYPE = "dialogType";
    private static final String START_ACTIVITY_RESTRICT_FOR_CHAIN = "restrictForChain";
    private static final String START_ACTIVITY_USERID = "UserId";
    private static final String START_RECORD_RECENT_TASK = "recentTask";
    private static final String TAG = "ActivitySecurityHelper";
    private AppOpsManager mAppOpsManager;
    private final Context mContext;
    private final PackageManagerInternal mPmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    private final SecurityManagerInternal mSmi = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
    private SecurityManagerInternal mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);

    public ActivitySecurityHelper(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
    }

    public Intent getCheckIntent(String callingPackage, String packageName, Intent intent, boolean fromActivity, int requestCode, boolean calleeAlreadyStarted, int userId, int callingUid, Bundle bOptions) {
        int callingUser = UserHandle.getUserId(callingUid);
        ApplicationInfo callerInfo = getApplicationInfo(callingPackage, callingUser);
        ApplicationInfo calleeInfo = getApplicationInfo(packageName, userId);
        if (callerInfo == null || calleeInfo == null) {
            return null;
        }
        Intent ret = AppRunningControlManager.getBlockActivityIntent(this.mContext, packageName, intent, fromActivity, requestCode);
        if (ret != null) {
            return ret;
        }
        Intent ret2 = getCheckStartActivityIntent(callerInfo, calleeInfo, intent, fromActivity, requestCode, calleeAlreadyStarted, userId, callingUser);
        if (ret2 != null) {
            return ret2;
        }
        Intent ret3 = getCheckGameBoosterAntiMsgIntent(callingPackage, packageName, intent, fromActivity, requestCode, userId);
        if (ret3 != null) {
            return ret3;
        }
        return getCheckAccessControlIntent(packageName, calleeInfo.uid, intent, fromActivity, requestCode, userId, bOptions);
    }

    private ApplicationInfo getApplicationInfo(final String packageName, final int userId) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        return (ApplicationInfo) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: miui.app.ActivitySecurityHelper$$ExternalSyntheticLambda0
            public final Object getOrThrow() {
                ApplicationInfo lambda$getApplicationInfo$0;
                lambda$getApplicationInfo$0 = ActivitySecurityHelper.this.lambda$getApplicationInfo$0(packageName, userId);
                return lambda$getApplicationInfo$0;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ApplicationInfo lambda$getApplicationInfo$0(String packageName, int userId) throws Exception {
        return this.mPmi.getApplicationInfo(packageName, 0L, Process.myUid(), userId);
    }

    public Intent getCheckStartActivityIntent(ApplicationInfo callerInfo, ApplicationInfo calleeInfo, Intent intent, boolean fromActivity, int requestCode, boolean calleeAlreadyStarted, int userId, int callingUser) {
        if (intent == null || AppOpsUtils.isXOptMode() || Build.IS_INTERNATIONAL_BUILD) {
            return null;
        }
        Intent result = checkShareActionForStorageRestricted(callerInfo, calleeInfo, intent, fromActivity, requestCode, userId);
        if (result != null) {
            return result;
        }
        if (UserHandle.getAppId(callerInfo.uid) < 10000 || callerInfo.isSystemApp()) {
            WakePathChecker.getInstance().recordWakePathCall(callerInfo.packageName, calleeInfo.packageName, 1, callingUser, userId, true);
            return null;
        }
        if (UserHandle.getAppId(calleeInfo.uid) < 10000 || calleeInfo.isSystemApp()) {
            WakePathChecker.getInstance().recordWakePathCall(callerInfo.packageName, calleeInfo.packageName, 1, callingUser, userId, true);
            return null;
        }
        if (TextUtils.equals(callerInfo.packageName, calleeInfo.packageName) && (intent.getFlags() & 1048576) != 0) {
            WakePathChecker.getInstance().recordWakePathCall(START_RECORD_RECENT_TASK, calleeInfo.packageName, 1, UserHandle.myUserId(), userId, true);
        }
        boolean exemptByRestrictChain = this.mSecurityManagerInternal.exemptByRestrictChain(callerInfo.packageName, calleeInfo.packageName);
        if (!TextUtils.equals(callerInfo.packageName, calleeInfo.packageName) && restrictForChain(callerInfo) && !exemptByRestrictChain) {
            return buildIntentForRestrictChain(callerInfo, calleeInfo.packageName, calleeInfo.uid, intent, fromActivity, requestCode, userId);
        }
        if (exemptByRestrictChain) {
            return null;
        }
        if (calleeAlreadyStarted) {
            WakePathChecker.getInstance().recordWakePathCall(callerInfo.packageName, calleeInfo.packageName, 1, callingUser, userId, true);
            return null;
        }
        if (TextUtils.equals(callerInfo.packageName, calleeInfo.packageName) || this.mSmi.checkAllowStartActivity(callerInfo.packageName, calleeInfo.packageName, intent, callerInfo.uid, calleeInfo.uid)) {
            WakePathChecker.getInstance().recordWakePathCall(callerInfo.packageName, calleeInfo.packageName, 1, callingUser, userId, true);
            return null;
        }
        return buildStartIntent(callerInfo.packageName, calleeInfo.packageName, userId, callerInfo.uid, calleeInfo.uid, intent, fromActivity, requestCode, -1);
    }

    private boolean restrictForChain(ApplicationInfo callerInfo) {
        try {
            int mode = this.mAppOpsManager.checkOpNoThrow(10049, callerInfo.uid, callerInfo.packageName);
            return mode != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Intent buildIntentForRestrictChain(ApplicationInfo callerAppInfo, String calleePackageName, int calleeUid, Intent intent, boolean fromActivity, int requestCode, int userId) {
        Intent startIntent = buildStartIntent(callerAppInfo.packageName, calleePackageName, userId, callerAppInfo.uid, calleeUid, intent, fromActivity, requestCode, -1);
        startIntent.putExtra(START_ACTIVITY_RESTRICT_FOR_CHAIN, true);
        return startIntent;
    }

    private Intent checkShareActionForStorageRestricted(ApplicationInfo callerInfo, ApplicationInfo calleeInfo, Intent intent, boolean fromActivity, int requestCode, int userId) {
        if (PACKAGE_ANDRIOD.equals(calleeInfo.packageName)) {
            return null;
        }
        String action = intent.getAction();
        if ("android.intent.action.PICK".equals(action)) {
            if (StorageRestrictedPathManager.isDenyAccessGallery(this.mContext, callerInfo.packageName, callerInfo.uid, intent)) {
                return buildStartIntent(callerInfo.packageName, calleeInfo.packageName, userId, callerInfo.uid, calleeInfo.uid, intent, fromActivity, requestCode, 10034);
            }
        } else if ("android.intent.action.SEND".equals(action) && StorageRestrictedPathManager.isDenyAccessGallery(this.mContext, calleeInfo.packageName, calleeInfo.uid, intent)) {
            return buildStartIntent(callerInfo.packageName, calleeInfo.packageName, userId, callerInfo.uid, calleeInfo.uid, intent, fromActivity, requestCode, 10034);
        }
        return null;
    }

    private Intent buildStartIntent(String callerPackage, String calleePackage, int userId, int callerUid, int calleeUid, Intent sourceIntent, boolean fromActivity, int requestCode, int type) {
        Intent result = new Intent("android.app.action.CHECK_ALLOW_START_ACTIVITY");
        result.putExtra(START_ACTIVITY_CALLER_PKGNAME, callerPackage);
        result.putExtra(START_ACTIVITY_CALLEE_PKGNAME, calleePackage);
        result.putExtra(START_ACTIVITY_USERID, userId);
        result.putExtra(START_ACTIVITY_CALLER_UID, callerUid);
        result.putExtra(START_ACTIVITY_CALLEE_UID, calleeUid);
        result.putExtra(START_ACTIVITY_DIALOG_TYPE, type);
        result.addFlags(SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX);
        result.setPackage("com.miui.securitycenter");
        if (sourceIntent != null) {
            if ((sourceIntent.getFlags() & 33554432) != 0) {
                result.addFlags(33554432);
            }
            sourceIntent.addFlags(BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT);
            if (fromActivity) {
                if (requestCode >= 0) {
                    sourceIntent.addFlags(33554432);
                }
            } else {
                sourceIntent.addFlags(268435456);
            }
            result.putExtra("android.intent.extra.INTENT", sourceIntent);
        }
        return result;
    }

    public Intent getCheckGameBoosterAntiMsgIntent(String callPackageName, String packageName, Intent intent, boolean fromActivity, int requestCode, int userId) {
        if ("com.miui.securitycenter".equals(callPackageName) || !this.mSmi.isGameBoosterActive(userId) || this.mSmi.checkGameBoosterAntiMsgPassAsUser(packageName, intent)) {
            return null;
        }
        return SecurityManager.getCheckAccessIntent(false, packageName, intent, requestCode, fromActivity, userId, (Bundle) null);
    }

    public Intent getCheckAccessControlIntent(String packageName, int uid, Intent intent, boolean fromActivity, int requestCode, int userId, Bundle bOptions) {
        if (!this.mSmi.isAccessControlActive(userId)) {
            return null;
        }
        if (intent != null && uid == Binder.getCallingUid()) {
            return null;
        }
        if (!this.mSmi.checkAccessControlPassAsUser(packageName, intent, userId) && (intent == null || (intent.getFlags() & 1048576) == 0)) {
            return SecurityManager.getCheckAccessIntent(true, packageName, intent, requestCode, fromActivity, userId, bOptions);
        }
        return null;
    }
}
