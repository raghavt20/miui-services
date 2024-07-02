package com.android.server;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.PrivacyTestModeStub;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerStub;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.MiuiBinderProxy;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.app.IAppOpsService;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.ProcessUtils;
import com.android.server.appop.AppOpsService;
import com.android.server.appop.AppOpsServiceStub;
import com.android.server.biometrics.sensors.face.MiuiFaceHidl;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.wifi.ArpPacket;
import com.miui.app.AppOpsServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.security.AppBehavior;
import miui.security.SecurityManagerInternal;

@MiuiStubHead(manifestName = "com.android.server.appop.AppOpsServiceStub$$")
/* loaded from: classes.dex */
public class AppOpsServiceStubImpl extends AppOpsServiceStub implements AppOpsServiceInternal {
    public static final boolean DEBUG = false;
    private static final String KEY_APP_BEHAVIOR_RECORD_ENABLE = "key_app_behavior_record_enable";
    private static final Set<String> MESSAGE_NOT_RECORD;
    private static final String MESSAGE_NO_ASK = "noteNoAsk";
    private static final String MESSAGE_NO_ASK_NO_RECORD = "NoAskNoRecord";
    private static final ArrayMap<String, String> PLATFORM_PERMISSIONS;
    private static final String TAG = "AppOpsServiceStubImpl";
    private static final String WIFI_MESSAGE_STARTWITH = "WifiPermissionsUtil#noteAppOpAllowed";
    private static final Set<String> sCtsIgnore;
    private static final Map<Integer, Integer> supportVirtualGrant;
    private ActivityManagerInternal mActivityManagerInternal;
    private AppOpsService mAppOpsService;
    private Context mContext;
    private PackageManagerInternal mPackageManagerInternal;
    private SecurityManagerInternal mSecurityManagerInternal;
    final SparseArray<UserState> mUidStates = new SparseArray<>();
    private boolean mRecordEnable = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppOpsServiceStubImpl> {

        /* compiled from: AppOpsServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppOpsServiceStubImpl INSTANCE = new AppOpsServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AppOpsServiceStubImpl m12provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AppOpsServiceStubImpl m11provideNewInstance() {
            return new AppOpsServiceStubImpl();
        }
    }

    static {
        HashSet hashSet = new HashSet();
        sCtsIgnore = hashSet;
        HashSet hashSet2 = new HashSet();
        MESSAGE_NOT_RECORD = hashSet2;
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        PLATFORM_PERMISSIONS = arrayMap;
        arrayMap.put("android.permission.SEND_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.READ_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_MMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_WAP_PUSH", "android.permission-group.SMS");
        arrayMap.put("android.permission.READ_CELL_BROADCASTS", "android.permission-group.SMS");
        hashSet.add("android.app.usage.cts");
        hashSet.add("com.android.cts.usepermission");
        hashSet.add("com.android.cts.permission");
        hashSet.add("com.android.cts.netlegacy22.permission");
        hashSet.add("android.netlegacy22.permission.cts");
        hashSet.add("android.provider.cts");
        hashSet.add("android.telephony2.cts");
        hashSet.add("android.permission.cts");
        hashSet.add("com.android.cts.writeexternalstorageapp");
        hashSet.add("com.android.cts.readexternalstorageapp");
        hashSet.add("com.android.cts.externalstorageapp");
        hashSet.add("android.server.alertwindowapp");
        hashSet.add("android.server.alertwindowappsdk25");
        hashSet.add("com.android.app2");
        hashSet.add("com.android.cts.appbinding.app");
        hashSet.add("com.android.cts.launcherapps.simplepremapp");
        hashSet2.add("AutoStartManagerService#signalStopProcessesLocked");
        hashSet2.add("ContentProvider#enforceReadPermission");
        hashSet2.add("ContentProvider#enforceWritePermission");
        hashSet2.add("AppOpsHelper#checkLocationAccess");
        hashSet2.add("BroadcastQueueImpl#specifyBroadcast");
        hashSet2.add(WIFI_MESSAGE_STARTWITH);
        HashMap hashMap = new HashMap();
        supportVirtualGrant = hashMap;
        hashMap.put(14, 10028);
        hashMap.put(4, 10029);
        hashMap.put(8, 10030);
        hashMap.put(6, 10031);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class UserState {
        Callback mCallback;
        MiuiBinderProxy mCallbackBinder;

        private UserState() {
        }
    }

    public void init(Context context) {
        this.mContext = context;
        LocalServices.addService(AppOpsServiceInternal.class, this);
    }

    public static boolean isCtsIgnore(String packageName) {
        return sCtsIgnore.contains(packageName);
    }

    @Override // com.miui.app.AppOpsServiceInternal
    public boolean isTestSuitSpecialIgnore(String packageName) {
        return sCtsIgnore.contains(packageName);
    }

    private synchronized UserState getUidState(int userHandle, boolean edit) {
        UserState userState = this.mUidStates.get(userHandle);
        if (userState == null) {
            if (!edit) {
                return null;
            }
            userState = new UserState();
            this.mUidStates.put(userHandle, userState);
        }
        return userState;
    }

    public synchronized void removeUser(int userHandle) {
        this.mUidStates.remove(userHandle);
    }

    public void systemReady() {
        Uri enableUri = Settings.Global.getUriFor(KEY_APP_BEHAVIOR_RECORD_ENABLE);
        ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.AppOpsServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                AppOpsServiceStubImpl.this.updateRecordState();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(enableUri, false, observer);
        updateRecordState();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRecordState() {
        this.mRecordEnable = Settings.Global.getInt(this.mContext.getContentResolver(), KEY_APP_BEHAVIOR_RECORD_ENABLE, 1) == 1;
        Slog.d(TAG, "updateRecordState: " + this.mRecordEnable);
    }

    public int askOperationLocked(int code, int uid, String packageName, String message) {
        if (MESSAGE_NO_ASK.equals(message) || MESSAGE_NO_ASK_NO_RECORD.equals(message)) {
            return 1;
        }
        int userId = UserHandle.getUserId(uid);
        if (userId == 999) {
            uid = UserHandle.getUid(0, UserHandle.getAppId(uid));
        }
        UserState uidState = getUidState(UserHandle.getUserId(uid), false);
        if (uidState == null || uidState.mCallbackBinder == null) {
            return 1;
        }
        int result = uidState.mCallbackBinder.callNonOneWayTransact(1, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(code)});
        return result;
    }

    private ActivityManagerInternal getActivityManagerInternal() {
        if (this.mActivityManagerInternal == null) {
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
        return this.mActivityManagerInternal;
    }

    private PackageManagerInternal getPackageManagerInternal() {
        if (this.mPackageManagerInternal == null) {
            this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManagerInternal;
    }

    public void onAppApplyOperation(int uid, String packageName, int op, int mode, int operationType, int processState, int capability, boolean appWidgetVisible, int flags, String message) {
        if ((flags & 32) == 0 && !MESSAGE_NOT_RECORD.contains(message)) {
            if ((!TextUtils.isEmpty(message) && message.startsWith(WIFI_MESSAGE_STARTWITH)) || MESSAGE_NO_ASK_NO_RECORD.equals(message)) {
                return;
            }
            onAppApplyOperation(uid, packageName, op, mode, operationType, processState, capability, appWidgetVisible);
        }
    }

    public void onAppApplyOperation(int uid, String packageName, int op, int mode, int operationType, int processState, int capability, boolean appWidgetVisible) {
        int mode2;
        int processState2;
        UserState uidState;
        int mode3 = mode;
        int processState3 = processState;
        if (UserHandle.getAppId(uid) <= 2000 || !this.mRecordEnable) {
            return;
        }
        if (op == 10032 && mode3 == 0) {
            return;
        }
        if ((op != 51 && op != 65) || mode3 != 0 || getAppOpsService().checkOperation(10032, uid, packageName) == 0) {
            PrivacyTestModeStub.get().collectPrivacyTestModeInfo(op, uid, this.mContext, packageName);
            if ((mode3 == 4 || mode3 == 0) && processState3 > 200) {
                if (appWidgetVisible || (getActivityManagerInternal() != null && getActivityManagerInternal().isPendingTopUid(uid))) {
                    mode2 = 0;
                    processState2 = 200;
                } else if (processState3 <= AppOpsManager.resolveFirstUnrestrictedUidState(op)) {
                    switch (op) {
                        case 0:
                        case 1:
                        case 41:
                        case ArpPacket.ARP_ETHER_IPV4_LEN /* 42 */:
                            if ((capability & 1) != 0) {
                                processState3 = 200;
                                mode3 = 0;
                                break;
                            }
                            break;
                        case 26:
                            if ((capability & 2) != 0) {
                                processState3 = 200;
                                mode3 = 0;
                                break;
                            }
                            break;
                        case 27:
                            if ((capability & 4) != 0) {
                                processState3 = 200;
                                mode3 = 0;
                                break;
                            }
                            break;
                        default:
                            processState3 = 200;
                            mode3 = 0;
                            break;
                    }
                    if (mode3 == 0) {
                        mode2 = mode3;
                        processState2 = processState3;
                    } else {
                        mode2 = 1;
                        processState2 = processState3;
                    }
                }
                uidState = getUidState(UserHandle.getUserId(uid), false);
                if (uidState != null && uidState.mCallbackBinder != null) {
                    uidState.mCallbackBinder.callOneWayTransact(4, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(op), Integer.valueOf(mode2), Integer.valueOf(operationType), Integer.valueOf(processState2)});
                }
                if (AppBehavior.switchOpToBehavior(op) == 0 && getSecurityManager() != null) {
                    getSecurityManager().recordAppBehaviorAsync(AppBehavior.switchOpToBehavior(op), packageName, 1L, (String) null);
                    return;
                }
            }
            mode2 = mode3;
            processState2 = processState3;
            uidState = getUidState(UserHandle.getUserId(uid), false);
            if (uidState != null) {
                uidState.mCallbackBinder.callOneWayTransact(4, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(op), Integer.valueOf(mode2), Integer.valueOf(operationType), Integer.valueOf(processState2)});
            }
            if (AppBehavior.switchOpToBehavior(op) == 0) {
            }
        }
    }

    public void updateProcessState(int uid, int procState) {
        AndroidPackage pkg;
        if (UserHandle.getAppId(uid) < 10000) {
            return;
        }
        int userId = UserHandle.getUserId(uid);
        UserState uidState = getUidState(userId, false);
        if (uidState != null && uidState.mCallbackBinder != null) {
            uidState.mCallbackBinder.callOneWayTransact(5, new Object[]{Integer.valueOf(uid), Integer.valueOf(procState)});
        }
        if (getSecurityManager() != null && (pkg = getPackageManagerInternal().getPackage(uid)) != null && !ProcessUtils.isSystem(getPackageManagerInternal().getApplicationInfo(pkg.getPackageName(), 0L, 1000, userId))) {
            boolean fgState = false;
            boolean bgState = false;
            if (procState < 20) {
                if (procState < 7) {
                    fgState = true;
                } else {
                    bgState = true;
                }
            } else {
                getSecurityManager().removeCalleeRestrictChain(pkg.getPackageName());
            }
            getSecurityManager().recordDurationInfo(22, (IBinder) null, uid, pkg.getPackageName(), fgState);
            getSecurityManager().recordDurationInfo(23, (IBinder) null, uid, pkg.getPackageName(), bgState);
        }
    }

    public int registerCallback(IBinder callback) {
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        if (callback == null) {
            return -1;
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == 0) {
            registerCallback(callback, 999);
        }
        return registerCallback(callback, callingUserId);
    }

    public int registerCallback(IBinder callback, int userId) {
        UserState uidState;
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        if (callback == null || (uidState = getUidState(userId, true)) == null) {
            return -1;
        }
        uidState.mCallbackBinder = new MiuiBinderProxy(callback, "com.android.internal.app.IOpsCallback");
        if (uidState.mCallback != null) {
            uidState.mCallback.unlinkToDeath();
        }
        uidState.mCallback = new Callback(callback, userId);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startService(final int userId) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.android.server.AppOpsServiceStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppOpsServiceStubImpl.this.lambda$startService$0(userId);
            }
        }, 1300L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startService$0(int userId) {
        try {
            Intent intent = new Intent("com.miui.permission.Action.SecurityService");
            intent.setPackage("com.lbe.security.miui");
            this.mContext.startServiceAsUser(intent, new UserHandle(userId));
        } catch (Exception e) {
            Slog.e(TAG, "Start Error", e);
        }
    }

    /* loaded from: classes.dex */
    public final class Callback implements IBinder.DeathRecipient {
        final IBinder mCallback;
        volatile boolean mUnLink;
        final int mUserId;

        public Callback(IBinder callback, int userId) {
            this.mCallback = callback;
            this.mUserId = userId;
            try {
                callback.linkToDeath(this, 0);
                Slog.d(AppOpsServiceStubImpl.TAG, "linkToDeath");
            } catch (RemoteException e) {
                Slog.e(AppOpsServiceStubImpl.TAG, "linkToDeath failed!", e);
            }
        }

        public void unlinkToDeath() {
            if (this.mUnLink) {
                return;
            }
            try {
                this.mUnLink = true;
                this.mCallback.unlinkToDeath(this, 0);
            } catch (Exception e) {
                Slog.e(AppOpsServiceStubImpl.TAG, "unlinkToDeath failed!", e);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            unlinkToDeath();
            AppOpsServiceStubImpl.this.startService(this.mUserId);
            Slog.d(AppOpsServiceStubImpl.TAG, "binderDied mUserId : " + this.mUserId);
        }
    }

    public boolean assessVirtualEnable(int code, int uid, int pid) {
        if (code > 10000 || code == 29 || code == 30 || UserHandle.getAppId(uid) < 10000) {
            return true;
        }
        AndroidPackage pkg = getPackageManagerInternal().getPackage(uid);
        return pkg != null && ProcessUtils.isSystem(getPackageManagerInternal().getApplicationInfo(pkg.getPackageName(), 0L, 1000, UserHandle.getUserId(uid)));
    }

    public boolean isSupportVirtualGrant(int code) {
        return !Build.IS_INTERNATIONAL_BUILD && supportVirtualGrant.containsKey(Integer.valueOf(code));
    }

    public int convertVirtualOp(int code) {
        return supportVirtualGrant.getOrDefault(Integer.valueOf(code), Integer.valueOf(code)).intValue();
    }

    public boolean supportForegroundByMiui(String permission) {
        return "android.permission.READ_EXTERNAL_STORAGE".equals(permission) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(permission) || "android.permission.READ_CALL_LOG".equals(permission) || "android.permission.WRITE_CALL_LOG".equals(permission) || "android.permission.READ_CALENDAR".equals(permission) || "android.permission.WRITE_CALENDAR".equals(permission) || "android.permission.READ_CONTACTS".equals(permission) || "android.permission.WRITE_CONTACTS".equals(permission) || "android.permission.GET_ACCOUNTS".equals(permission);
    }

    public boolean isRevokedCompatByMiui(String permission, int mode) {
        return supportForegroundByMiui(permission) ? (mode == 0 || mode == 4) ? false : true : mode != 0;
    }

    public boolean skipSyncAppOpsWithRuntime(int code, int mode, int oldMode) {
        if (PackageManagerStub.get().isOptimizationMode()) {
            String permission = AppOpsManager.opToPermission(code);
            if (oldMode == 4 && supportForegroundByMiui(permission)) {
                return true;
            }
            return TextUtils.equals(PLATFORM_PERMISSIONS.get(permission), "android.permission-group.SMS");
        }
        return false;
    }

    public boolean skipNotificationRequest(ActivityInfo activityInfo) {
        return (activityInfo == null || activityInfo.name == null || !activityInfo.name.contains("SplashActivity")) ? false : true;
    }

    public int getNotificationRequestDelay(ActivityInfo activityInfo) {
        if (activityInfo == null) {
            return 0;
        }
        if (this.mContext.getResources().getConfiguration().orientation == 1) {
            switch (activityInfo.screenOrientation) {
                case 0:
                case 6:
                case 8:
                case 11:
                    return 1000;
                default:
                    return 0;
            }
        }
        switch (activityInfo.screenOrientation) {
            case 1:
            case 7:
            case 9:
            case 12:
                return 1000;
            default:
                return 0;
        }
    }

    public void initSwitchedOp(SparseArray<int[]> switchedOps) {
        for (int miuiCode = MiuiFaceHidl.MSG_GET_IMG_FRAME; miuiCode < 10051; miuiCode++) {
            int switchCode = AppOpsManager.opToSwitch(miuiCode);
            switchedOps.put(switchCode, ArrayUtils.appendInt(switchedOps.get(switchCode), miuiCode));
        }
    }

    private SecurityManagerInternal getSecurityManager() {
        if (this.mSecurityManagerInternal == null) {
            this.mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        return this.mSecurityManagerInternal;
    }

    public int checkPermission(PackageManager packageManager, String permission, String packageName, int uid) {
        return PermissionManager.checkPackageNamePermission(permission, packageName, UserHandle.getUserId(uid));
    }

    public boolean isMiuiOp(int op) {
        return op > 10000 && op < 10051;
    }

    private AppOpsService getAppOpsService() {
        if (this.mAppOpsService == null) {
            IBinder b = ServiceManager.getService("appops");
            this.mAppOpsService = IAppOpsService.Stub.asInterface(b);
        }
        return this.mAppOpsService;
    }
}
