package com.android.server.accounts;

import android.accounts.Account;
import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.miui.AppOpsUtils;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.am.AutoStartManagerServiceStub;
import com.android.server.am.BroadcastQueueModernStubImpl;
import miui.content.pm.ExtraPackageManager;

/* loaded from: classes.dex */
public class AccountManagerServiceInjector {
    public static final String ACCOUNT_CHANGED_ACTION_ADDED = "added";
    public static final String ACCOUNT_CHANGED_ACTION_ALTERED = "altered";
    public static final String ACCOUNT_CHANGED_ACTION_REMOVED = "removed";
    public static final String KEY_ACCOUNT_CHANGED_ACTION = "account_changed_action";
    public static final String LOGIN_ACCOUNTS_CHANGED_SYS_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED_SYS";
    private static final String TAG = "AccountManagerServiceInjector";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTrustedAccountSignature(PackageManager pm, String accountType, int serviceUid, int callingUid) {
        long identityToken = Binder.clearCallingIdentity();
        try {
            return ExtraPackageManager.isTrustedAccountSignature(pm, accountType, serviceUid, callingUid);
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isForceRemove(boolean removalAllowed) {
        ApplicationInfo info;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
            if (packages != null && packages.length > 0 && (info = pm.getApplicationInfo(packages[0], 0L, UserHandle.getCallingUserId())) != null && (info.flags & 1) == 0) {
                if (!removalAllowed) {
                    Slog.d(TAG, "force remove account");
                }
                return !AppOpsUtils.isXOptMode();
            }
        } catch (Exception e) {
            Log.e(TAG, "isForceRemove", e);
        }
        return false;
    }

    static void sendAccountsChangedSysBroadcast(Context ctx, int userId, String action, Account[] accounts) {
        Intent i = new Intent(LOGIN_ACCOUNTS_CHANGED_SYS_ACTION);
        i.putExtra(KEY_ACCOUNT_CHANGED_ACTION, action);
        i.putExtra("accounts", accounts);
        i.addFlags(BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        ctx.sendBroadcastAsUser(i, new UserHandle(userId), "android.permission.GET_ACCOUNTS");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean canBindService(Context context, Intent service, int userId) {
        return AutoStartManagerServiceStub.getInstance().isAllowStartService(context, service, userId);
    }
}
