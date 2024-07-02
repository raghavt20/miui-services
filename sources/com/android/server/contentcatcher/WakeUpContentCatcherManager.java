package com.android.server.contentcatcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.contentcatcher.WakeUpContentCatcherManager;
import com.android.server.input.MiuiInputThread;

/* loaded from: classes.dex */
public class WakeUpContentCatcherManager {
    private static final String CONTENTCATCHER_COMPONENT = "com.miui.contentcatcher/com.miui.contentcatcher.service.StartContentCatcherService";
    private static final String PACKAGE_CONTENTCATCHER = "com.miui.contentcatcher";
    private static final String PACKAGE_CONTENTEXTENSION = "com.miui.contentextension";
    private static final String SUPPORT_CLIPBOARD_MODE = "support_clipboard_mode";
    private static final String SUPPORT_DOUBLE_PRESS_MODE = "support_double_press_mode";
    private static final String SUPPORT_EXTENSION_PRESS_MODE = "content_catcher_network_enabled_content_extension";
    private static final String TAG = "WakeUpContentCatcherManager";
    private static final String TAPLUS_CLIPBOARD_MODE = "taplus_clipboard_mode";
    private Context mContext;
    private boolean mIsExistCatcherProcess = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() { // from class: com.android.server.contentcatcher.WakeUpContentCatcherManager.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(WakeUpContentCatcherManager.TAG, "service connected");
            WakeUpContentCatcherManager.this.mIsExistCatcherProcess = true;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(WakeUpContentCatcherManager.TAG, "service disConnected, so keep alive");
            WakeUpContentCatcherManager.this.mIsExistCatcherProcess = false;
            WakeUpContentCatcherManager.this.startKeepAlive();
        }
    };
    BroadcastReceiver mBootCompleteReceiver = new AnonymousClass2();
    BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() { // from class: com.android.server.contentcatcher.WakeUpContentCatcherManager.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            WakeUpContentCatcherManager.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
        }
    };
    BroadcastReceiver mAppChangedReceiver = new AnonymousClass4();
    private int mCurrentUserId = UserHandle.myUserId();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.contentcatcher.WakeUpContentCatcherManager$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.contentcatcher.WakeUpContentCatcherManager$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WakeUpContentCatcherManager.AnonymousClass2.this.lambda$onReceive$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            if (!WakeUpContentCatcherManager.this.functionEnable()) {
                return;
            }
            Slog.i(WakeUpContentCatcherManager.TAG, "catcher function has been enable");
            WakeUpContentCatcherManager.this.startContentCatcherService();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.contentcatcher.WakeUpContentCatcherManager$4, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass4 extends BroadcastReceiver {
        AnonymousClass4() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            Uri data = intent.getData();
            if (data == null) {
                return;
            }
            String packageName = data.getSchemeSpecificPart();
            if (!WakeUpContentCatcherManager.PACKAGE_CONTENTCATCHER.equals(packageName)) {
                return;
            }
            switch (action.hashCode()) {
                case -810471698:
                    if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 172491798:
                    if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1544582882:
                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
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
                    Slog.i(WakeUpContentCatcherManager.TAG, "contentcatcher apk has remove， due to install");
                    break;
                case 1:
                case 2:
                case 3:
                    break;
                default:
                    return;
            }
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.contentcatcher.WakeUpContentCatcherManager$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WakeUpContentCatcherManager.AnonymousClass4.this.lambda$onReceive$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            WakeUpContentCatcherManager.this.startContentCatcherService();
        }
    }

    public WakeUpContentCatcherManager(Context context) {
        this.mContext = context;
        Settings.System.putIntForUser(this.mContext.getContentResolver(), SUPPORT_CLIPBOARD_MODE, 1, this.mCurrentUserId);
        Settings.System.putIntForUser(context.getContentResolver(), SUPPORT_DOUBLE_PRESS_MODE, 1, this.mCurrentUserId);
        MiuiSettingsObserver settingsObserver = new MiuiSettingsObserver(MiuiInputThread.getHandler());
        settingsObserver.observe();
        registerBootCompletedReceiver();
        registerUserSwitchReceiver(this.mContext);
        registerPackageInstallCompletedReceiver();
    }

    private void registerBootCompletedReceiver() {
        IntentFilter bootCompletedFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBootCompleteReceiver, bootCompletedFilter);
    }

    private void registerUserSwitchReceiver(Context context) {
        IntentFilter userSwitchFiler = new IntentFilter();
        userSwitchFiler.addAction("android.intent.action.USER_SWITCHED");
        context.registerReceiverForAllUsers(this.mUserSwitchReceiver, userSwitchFiler, null, null);
    }

    private void registerPackageInstallCompletedReceiver() {
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        appChangedFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        appChangedFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        appChangedFilter.addAction("android.intent.action.PACKAGE_ADDED");
        appChangedFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mAppChangedReceiver, appChangedFilter);
    }

    private boolean isExistContentExtension() {
        PackageInfo packageInfo;
        try {
            packageInfo = this.mContext.getPackageManager().getPackageInfo(PACKAGE_CONTENTEXTENSION, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, e.getMessage(), e);
            packageInfo = null;
        }
        return packageInfo != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startContentCatcherService() {
        if (!isExistContentExtension()) {
            Slog.i(TAG, "device not support, so return");
        } else {
            Slog.i(TAG, "startContentCatcherService");
            startKeepAlive();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startKeepAlive() {
        Slog.i(TAG, "start contentcatcher process keep alive");
        ComponentName serviceComponent = ComponentName.unflattenFromString(CONTENTCATCHER_COMPONENT);
        Intent intent = new Intent();
        intent.setComponent(serviceComponent);
        this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean functionEnable() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), TAPLUS_CLIPBOARD_MODE, 0, this.mCurrentUserId) == 1 || Settings.System.getIntForUser(this.mContext.getContentResolver(), SUPPORT_EXTENSION_PRESS_MODE, 0, this.mCurrentUserId) == 1;
    }

    /* loaded from: classes.dex */
    private class MiuiSettingsObserver extends ContentObserver {
        public MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = WakeUpContentCatcherManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(WakeUpContentCatcherManager.TAPLUS_CLIPBOARD_MODE), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(WakeUpContentCatcherManager.SUPPORT_EXTENSION_PRESS_MODE), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            boolean enable = WakeUpContentCatcherManager.this.functionEnable();
            if (!WakeUpContentCatcherManager.this.mIsExistCatcherProcess && enable) {
                WakeUpContentCatcherManager.this.startContentCatcherService();
            }
        }
    }
}
