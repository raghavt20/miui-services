package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.app.AppGlobals;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.enterprise.RestrictionsHelper;
import java.util.ArrayList;
import miui.enterprise.RestrictionsHelperStub;

/* loaded from: classes.dex */
public class SyncSecurityStubImpl implements SyncSecurityStub {
    private static final String CLOUD_MANAGER_PERMISSION = "com.xiaomi.permission.CLOUD_MANAGER";
    private static final String TAG = "SyncSecurityImpl";
    private static final String XIAOMI_ACCOUNT_TYPE = "com.xiaomi";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SyncSecurityStubImpl> {

        /* compiled from: SyncSecurityStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SyncSecurityStubImpl INSTANCE = new SyncSecurityStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SyncSecurityStubImpl m954provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SyncSecurityStubImpl m953provideNewInstance() {
            return new SyncSecurityStubImpl();
        }
    }

    public AccountAndUser[] filterOutXiaomiAccount(AccountAndUser[] accountAndUsers, int reason) {
        if (accountAndUsers == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "filterOutXiaomiAccount: null accountAndUsers, abort. ");
                return null;
            }
            return null;
        }
        if (reason < 0) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "filterOutXiaomiAccount: internal request, abort. ");
            }
            return accountAndUsers;
        }
        int appId = UserHandle.getAppId(reason);
        if (appId < 10000) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "filterOutXiaomiAccount: system request, abort. ");
            }
            return accountAndUsers;
        }
        try {
            if (AppGlobals.getPackageManager().checkUidPermission(CLOUD_MANAGER_PERMISSION, appId) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    Slog.i(TAG, "filterOutXiaomiAccount: CLOUD MANAGER, abort. ");
                }
                return accountAndUsers;
            }
        } catch (RemoteException e) {
        }
        if (Log.isLoggable(TAG, 2)) {
            Slog.i(TAG, "filterOutXiaomiAccount: go. ");
        }
        ArrayList<AccountAndUser> filtered = new ArrayList<>();
        for (AccountAndUser au : accountAndUsers) {
            if (au == null || au.account == null || !"com.xiaomi".equals(au.account.type)) {
                filtered.add(au);
            }
        }
        return (AccountAndUser[]) filtered.toArray(new AccountAndUser[0]);
    }

    public boolean permitControlSyncForAccount(Context context, Account account) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        if (RestrictionsHelper.hasRestriction(context, "disallow_auto_sync") || RestrictionsHelperStub.getInstance().isRestriction("disallow_auto_sync")) {
            Slog.d(TAG, "Device is in enterprise mode, sync is restricted by enterprise!");
            return false;
        }
        if (uid < 10000) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "Permit sync control for account " + getAccountType(account) + " by pid " + pid + ". SYSTEM UID. ");
            }
            return true;
        }
        if (account == null || !"com.xiaomi".equals(account.type)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "Permit sync control for account " + getAccountType(account) + " by pid " + pid + ". OTHER ACCOUNT. ");
            }
            return true;
        }
        if (context.checkCallingOrSelfPermission(CLOUD_MANAGER_PERMISSION) == 0) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.i(TAG, "Permit sync control for account " + getAccountType(account) + " by pid " + pid + ". CLOUD MANAGER. ");
            }
            return true;
        }
        if (Log.isLoggable(TAG, 2)) {
            Slog.i(TAG, "Deny sync control for account " + getAccountType(account) + " by pid " + pid + ". ");
        }
        return false;
    }

    private static String getAccountType(Account account) {
        if (account == null) {
            return "[NULL]";
        }
        return "[" + account.type + "]";
    }
}
