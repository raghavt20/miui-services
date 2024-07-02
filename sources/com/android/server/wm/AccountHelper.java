package com.android.server.wm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IMiuiActivityObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.content.MiSyncConstants;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class AccountHelper {
    private static final int LISTEN_MODE_ACCOUNT = 1;
    private static final int LISTEN_MODE_NONE = 0;
    private static final int LISTEN_MODE_WIFI = 2;
    private static final String TAG = "MiuiPermision";
    private static final String mAccountLoginActivity;
    private static final ComponentName mCTAActivity;
    private static AccountCallback mCallBack;
    private static Context mContext;
    private static final ComponentName mGrantPermissionsActivity;
    private static final String mNotificationActivity;
    private static final String mPermissionActivity;
    private static final String mWifiSettingActivity;
    private static ArrayList<ComponentName> sAccessActivities;
    private static ArrayList<String> sAccessPackage;
    private static volatile AccountHelper sAccountHelper = null;
    private static boolean mListeningActivity = false;
    private static boolean mInIMEIWhiteList = false;
    private static boolean DEBUG = true;
    private int mListenMode = 0;
    IMiuiActivityObserver mActivityStateObserver = new IMiuiActivityObserver.Stub() { // from class: com.android.server.wm.AccountHelper.2
        public void activityIdle(Intent intent) throws RemoteException {
        }

        public void activityResumed(Intent intent) throws RemoteException {
            if (ActivityTaskManagerServiceStub.get().isControllerAMonkey()) {
                if (AccountHelper.DEBUG) {
                    Log.v(AccountHelper.TAG, "skip account check !");
                    return;
                }
                return;
            }
            intent.getComponent().getClassName();
            String packageName = intent.getComponent().getPackageName();
            if (AccountHelper.DEBUG) {
                Log.i(AccountHelper.TAG, "resume packageName:" + packageName + "mListenMode :" + AccountHelper.this.mListenMode);
            }
            AccountManager.get(AccountHelper.mContext).getAccountsByType(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE);
            if (!AccountHelper.sAccessPackage.contains(packageName) && !AccountHelper.sAccessActivities.contains(intent.getComponent())) {
                if ((AccountHelper.this.mListenMode & 2) != 0) {
                    AccountHelper.mCallBack.onWifiSettingFinish();
                } else if ((AccountHelper.this.mListenMode & 1) != 0) {
                    AccountHelper.this.addAccount(AccountHelper.mContext);
                }
            }
        }

        public void activityPaused(Intent intent) throws RemoteException {
        }

        public void activityStopped(Intent intent) throws RemoteException {
        }

        public void activityDestroyed(Intent intent) throws RemoteException {
        }

        /* JADX WARN: Multi-variable type inference failed */
        public IBinder asBinder() {
            return this;
        }
    };

    /* loaded from: classes.dex */
    public interface AccountCallback {
        void onWifiSettingFinish();

        void onXiaomiAccountLogin();

        void onXiaomiAccountLogout();
    }

    static {
        String str = new String("com.xiaomi.account");
        mAccountLoginActivity = str;
        String str2 = new String("com.xiaomi.passport");
        mNotificationActivity = str2;
        String str3 = new String("com.android.settings");
        mWifiSettingActivity = str3;
        String str4 = new String("com.google.android.permissioncontroller");
        mPermissionActivity = str4;
        ComponentName componentName = new ComponentName(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.miui.permcenter.permissions.SystemAppPermissionDialogActivity");
        mCTAActivity = componentName;
        ComponentName componentName2 = new ComponentName("com.lbe.security.miui", ActivityRecordImpl.PERMISSION_ACTIVITY);
        mGrantPermissionsActivity = componentName2;
        sAccessPackage = new ArrayList<>();
        sAccessActivities = new ArrayList<>();
        sAccessPackage.add(str);
        sAccessPackage.add(str2);
        sAccessPackage.add(str3);
        sAccessPackage.add(str4);
        sAccessActivities.add(componentName);
        sAccessActivities.add(componentName2);
    }

    private AccountHelper() {
    }

    public static AccountHelper getInstance() {
        if (sAccountHelper == null) {
            synchronized (AccountHelper.class) {
                if (sAccountHelper == null) {
                    sAccountHelper = new AccountHelper();
                }
            }
        }
        return sAccountHelper;
    }

    public void registerAccountListener(Context context, AccountCallback callBack) {
        mContext = context;
        mCallBack = callBack;
        context.registerReceiver(new AccountBroadcastReceiver(), new IntentFilter("android.accounts.LOGIN_ACCOUNTS_POST_CHANGED"));
    }

    public Account getXiaomiAccount(Context context) {
        Account account = null;
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE);
        if (accounts.length > 0) {
            account = accounts[0];
        }
        if (account == null) {
            Log.i(TAG, "xiaomi account is null");
        }
        return account;
    }

    public void onXiaomiAccountLogin(Context context, Account account) {
        mCallBack.onXiaomiAccountLogin();
    }

    public void onXiaomiAccountLogout(Context context, Account account) {
        mCallBack.onXiaomiAccountLogout();
    }

    /* loaded from: classes.dex */
    private class AccountBroadcastReceiver extends BroadcastReceiver {
        private AccountBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (TextUtils.equals(action, "android.accounts.LOGIN_ACCOUNTS_POST_CHANGED")) {
                int type = intent.getIntExtra("extra_update_type", -1);
                Account account = (Account) intent.getParcelableExtra("extra_account");
                if (account == null || !TextUtils.equals(account.type, MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE)) {
                    Log.i(AccountHelper.TAG, "It isn't a xiaomi account changed.");
                    return;
                }
                Context appContext = context.getApplicationContext();
                if (type == 1) {
                    AccountHelper.this.onXiaomiAccountLogout(appContext, account);
                } else if (type != 2) {
                    Log.w(AccountHelper.TAG, String.format("Xiaomi account changed, but unknown type: %s.", Integer.valueOf(type)));
                } else {
                    AccountHelper.this.onXiaomiAccountLogin(appContext, account);
                }
            }
        }
    }

    public void ListenAccount(int mode) {
        registerAccountActivityObserver();
        this.mListenMode |= mode;
        if (DEBUG) {
            Log.i(TAG, "ListenAccount mode: " + mode + " mListenMode: " + this.mListenMode);
        }
    }

    public void UnListenAccount(int mode) {
        int i = this.mListenMode ^ mode;
        this.mListenMode = i;
        if (i == 0) {
            unRegisterAccountActivityObserver();
        }
        if (DEBUG) {
            Log.i(TAG, "UnListenAccount mode: " + mode + " mListenMode: " + this.mListenMode);
        }
    }

    public void registerAccountActivityObserver() {
        if (mListeningActivity) {
            return;
        }
        mListeningActivity = true;
        Intent intent = new Intent();
        IActivityManager activityManager = ActivityManager.getService();
        if (activityManager == null) {
            return;
        }
        ActivityTaskManagerServiceImpl.getInstance().getMiuiActivityController().registerActivityObserver(this.mActivityStateObserver, intent);
    }

    public void unRegisterAccountActivityObserver() {
        if (!mListeningActivity) {
            return;
        }
        mListeningActivity = false;
        new Intent();
        IActivityManager activityManager = ActivityManager.getService();
        if (activityManager == null) {
            return;
        }
        ActivityTaskManagerServiceImpl.getInstance().getMiuiActivityController().unregisterActivityObserver(this.mActivityStateObserver);
    }

    public void addAccount(final Context context) {
        if (DEBUG) {
            Log.i(TAG, "addAccount");
        }
        new Bundle();
        AccountManager.get(context).addAccount(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE, "passportapi", null, null, null, new AccountManagerCallback<Bundle>() { // from class: com.android.server.wm.AccountHelper.1
            @Override // android.accounts.AccountManagerCallback
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Intent intent = (Intent) future.getResult().getParcelable("intent");
                    intent.addFlags(268435456);
                    if (intent != null) {
                        context.startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.i(AccountHelper.TAG, "addAccount");
                    e.printStackTrace();
                }
            }
        }, null);
    }
}
