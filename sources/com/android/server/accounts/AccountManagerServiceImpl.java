package com.android.server.accounts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.accounts.AccountManagerServiceStub$$")
/* loaded from: classes.dex */
public class AccountManagerServiceImpl extends AccountManagerServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AccountManagerServiceImpl> {

        /* compiled from: AccountManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AccountManagerServiceImpl INSTANCE = new AccountManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AccountManagerServiceImpl m284provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AccountManagerServiceImpl m283provideNewInstance() {
            return new AccountManagerServiceImpl();
        }
    }

    public boolean isForceRemove(boolean removalAllowed) {
        return AccountManagerServiceInjector.isForceRemove(removalAllowed);
    }

    public boolean isTrustedAccountSignature(PackageManager pm, String accountType, int serviceUid, int callingUid) {
        return AccountManagerServiceInjector.isTrustedAccountSignature(pm, accountType, serviceUid, callingUid);
    }

    public boolean canBindService(Context context, Intent service, int userId) {
        return AccountManagerServiceInjector.canBindService(context, service, userId);
    }
}
