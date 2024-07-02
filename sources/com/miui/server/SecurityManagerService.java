package com.miui.server;

import android.app.AppOpsManagerInternal;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.SurfaceControl;
import com.android.internal.app.ILocationBlurry;
import com.android.internal.app.IWakePathCallback;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.AutoStartManagerServiceStub;
import com.android.server.am.PendingIntentRecordStub;
import com.android.server.am.ProcessRecord;
import com.android.server.am.ProcessUtils;
import com.android.server.location.MiuiBlurLocationManagerStub;
import com.android.server.net.NetworkManagementServiceStub;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.FoldablePackagePolicy;
import com.android.server.wm.MiuiSizeCompatService;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowProcessUtils;
import com.miui.server.security.AccessControlImpl;
import com.miui.server.security.AppBehaviorService;
import com.miui.server.security.AppDurationService;
import com.miui.server.security.DefaultBrowserImpl;
import com.miui.server.security.GameBoosterImpl;
import com.miui.server.security.SafetyDetectManagerStub;
import com.miui.server.security.SecurityPackageSettings;
import com.miui.server.security.SecuritySettingsObserver;
import com.miui.server.security.SecurityUserState;
import com.miui.server.security.SecurityWriteHandler;
import com.miui.server.security.WakeUpTimeImpl;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import miui.app.StorageRestrictedPathManager;
import miui.content.pm.PreloadedAppPolicy;
import miui.os.Build;
import miui.security.AppBehavior;
import miui.security.ISecurityManager;
import miui.security.SecurityManager;
import miui.security.SecurityManagerInternal;
import miui.security.WakePathChecker;
import miui.securityspace.ConfigUtils;
import miui.securityspace.XSpaceUserHandle;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public class SecurityManagerService extends ISecurityManager.Stub {
    private static final String PACKAGE_PERMISSIONCENTER = "com.lbe.security.miui";
    private static final String PACKAGE_SECURITYCENTER = "com.miui.securitycenter";
    private static final String PLATFORM_VAID_PERMISSION = "com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE";
    private static final String READ_AND_WRITE_PERMISSION_MANAGER = "miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER";
    private static final String TAG = "SecurityManagerService";
    private static final String UPDATE_VERSION = "1.0";
    private static final String VAID_PLATFORM_CACHE_PATH = "/data/system/vaid_persistence_platform";
    private static final int WRITE_SETTINGS_DELAY = 1000;
    public final AccessControlImpl mAccessControlImpl;
    public final AccessController mAccessController;
    public boolean mAllowedDeviceProvision;
    private final AppBehaviorService mAppBehavior;
    private final AppDurationService mAppDuration;
    private AppOpsManagerInternal mAppOpsInternal;
    private final AppRunningControlService mAppRunningControlService;
    public final Context mContext;
    public final DefaultBrowserImpl mDefaultBrowserImpl;
    public final GameBoosterImpl mGameBoosterImpl;
    private INotificationManager mINotificationManager;
    private final SecurityManagerInternal mInternal;
    private boolean mIsUpdated;
    private String mPlatformVAID;
    private final SecuritySmsHandler mSecuritySmsHandler;
    public final SecurityWriteHandler mSecurityWriteHandler;
    public SecuritySettingsObserver mSettingsObserver;
    public final WakeUpTimeImpl mWakeUpTimeImpl;
    public final AtomicFile mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "miui-packages.xml"));
    public final Object mUserStateLock = new Object();
    public final SparseArray<SecurityUserState> mUserStates = new SparseArray<>(3);
    private final ArrayList<String> mIncompatibleAppList = new ArrayList<>();
    private final List<String> mPrivacyVirtualDisplay = new ArrayList();
    private final List<String> mPrivacyDisplayNameList = new ArrayList();
    private LinkedHashMap<String, List<String>> restrictChainMaps = new LinkedHashMap<>();
    private final UserManagerService mUserManager = UserManagerService.getInstance();
    private final PermissionManagerService mPermissionManagerService = ServiceManager.getService("permissionmgr");

    protected void dump(FileDescriptor fd, PrintWriter out, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            out.println("Permission Denial: can't dump SecurityManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        WakePathChecker.getInstance().dump(out);
        NetworkManagementServiceStub.getInstance().dump(out);
        dumpPrivacyVirtualDisplay(out);
        this.mAppBehavior.dump(out);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class MyPackageObserver implements PackageManagerInternal.PackageListObserver {
        private final Context mContext;
        private final DefaultBrowserImpl mDefaultBrowserImpl;

        MyPackageObserver(Context context, DefaultBrowserImpl defaultBrowserImpl) {
            this.mContext = context;
            this.mDefaultBrowserImpl = defaultBrowserImpl;
        }

        public void onPackageAdded(String packageName, int uid) {
            WakePathChecker.getInstance().onPackageAdded(this.mContext);
            this.mDefaultBrowserImpl.checkDefaultBrowser(packageName);
        }

        public void onPackageRemoved(String packageName, int uid) {
            this.mDefaultBrowserImpl.checkDefaultBrowser(packageName);
        }

        public void onPackageChanged(String packageName, int uid) {
            this.mDefaultBrowserImpl.checkDefaultBrowser(packageName);
        }
    }

    public SecurityManagerService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("SecurityHandlerThread");
        handlerThread.start();
        SecurityWriteHandler securityWriteHandler = new SecurityWriteHandler(this, handlerThread.getLooper());
        this.mSecurityWriteHandler = securityWriteHandler;
        this.mSecuritySmsHandler = new SecuritySmsHandler(context, securityWriteHandler);
        this.mAccessController = new AccessController(context, handlerThread.getLooper());
        this.mAccessControlImpl = new AccessControlImpl(this);
        this.mWakeUpTimeImpl = new WakeUpTimeImpl(this);
        this.mGameBoosterImpl = new GameBoosterImpl(this);
        this.mDefaultBrowserImpl = new DefaultBrowserImpl(this);
        this.mAppRunningControlService = new AppRunningControlService(context);
        this.mSettingsObserver = new SecuritySettingsObserver(this, securityWriteHandler);
        AppBehaviorService appBehaviorService = new AppBehaviorService(context, handlerThread);
        this.mAppBehavior = appBehaviorService;
        AppDurationService appDurationService = new AppDurationService(appBehaviorService);
        this.mAppDuration = appDurationService;
        appBehaviorService.setAppBehaviorDuration(appDurationService);
        LocalService localService = new LocalService();
        this.mInternal = localService;
        LocalServices.addService(SecurityManagerInternal.class, localService);
        securityWriteHandler.post(new Runnable() { // from class: com.miui.server.SecurityManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SecurityManagerService.this.loadData();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadData() {
        readSettings();
        updateXSpaceSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initWhenBootCompleted() {
        this.mWakeUpTimeImpl.readWakeUpTime();
        synchronized (this.mUserStateLock) {
            SecurityUserState userState = getUserStateLocked(0);
            this.mSettingsObserver.initAccessControlSettingsLocked(userState);
        }
        this.mSettingsObserver.registerForSettingsChanged();
        WakePathChecker.getInstance().init(this.mContext);
        StorageRestrictedPathManager.getInstance().init(this.mContext);
        RestrictAppNetManager.init(this.mContext);
        this.mAccessController.updatePasswordTypeForPattern(UserHandle.myUserId());
        if (!Build.IS_INTERNATIONAL_BUILD) {
            this.mDefaultBrowserImpl.resetDefaultBrowser();
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            pmi.getPackageList(new MyPackageObserver(this.mContext, this.mDefaultBrowserImpl));
        }
    }

    public boolean checkSmsBlocked(Intent intent) {
        checkPermissionByUid(1000, 1001);
        return this.mSecuritySmsHandler.checkSmsBlocked(intent);
    }

    public boolean startInterceptSmsBySender(String pkgName, String sender, int count) {
        checkPermissionByUid(1000);
        return this.mSecuritySmsHandler.startInterceptSmsBySender(pkgName, sender, count);
    }

    public boolean stopInterceptSmsBySender() {
        checkPermissionByUid(1000);
        return this.mSecuritySmsHandler.stopInterceptSmsBySender();
    }

    public boolean addMiuiFirewallSharedUid(int uid) {
        checkPermissionByUid(1000);
        return NetworkManagementServiceStub.getInstance().addMiuiFirewallSharedUid(uid);
    }

    public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) {
        checkPermissionByUid(1000);
        return NetworkManagementServiceStub.getInstance().setMiuiFirewallRule(packageName, uid, rule, type);
    }

    public boolean setCurrentNetworkState(int state) {
        checkPermissionByUid(1000);
        return NetworkManagementServiceStub.getInstance().setCurrentNetworkState(state);
    }

    public void setGameBoosterIBinder(IBinder gameBooster, int userId, boolean isGameMode) {
        checkPermission();
        this.mGameBoosterImpl.setGameBoosterIBinder(gameBooster, userId, isGameMode);
    }

    public boolean getGameMode(int userId) {
        return this.mGameBoosterImpl.getGameMode(userId);
    }

    public void setGameStorageApp(String packageName, int userId, boolean isStorage) {
        checkPermission();
        this.mGameBoosterImpl.setGameStorageApp(packageName, userId, isStorage);
    }

    public boolean isGameStorageApp(String packageName, int userId) {
        checkPermission();
        return this.mGameBoosterImpl.isGameStorageApp(packageName, userId);
    }

    public List<String> getAllGameStorageApps(int userId) {
        checkPermission();
        return this.mGameBoosterImpl.getAllGameStorageApps(userId);
    }

    public boolean getAppDarkMode(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppDarkModeForUser(packageName, userId);
    }

    public boolean getAppDarkModeForUser(String packageName, int userId) {
        return this.mInternal.getAppDarkModeForUser(packageName, userId);
    }

    public void setAppDarkModeForUser(String packageName, boolean enabled, int userId) {
        this.mInternal.setAppDarkModeForUser(packageName, enabled, userId);
    }

    public boolean getAppRemindForRelaunch(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppRemindForRelaunchForUser(packageName, userId);
    }

    public boolean getAppRemindForRelaunchForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            SecurityUserState userState = getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isRemindForRelaunch;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setAppRemindForRelaunchForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mUserStateLock) {
            SecurityUserState userStateLocked = getUserStateLocked(userId);
            SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isRemindForRelaunch = enabled;
            scheduleWriteSettings();
        }
    }

    public boolean getAppRelaunchModeAfterFolded(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppRelaunchModeAfterFoldedForUser(packageName, userId);
    }

    public boolean getAppRelaunchModeAfterFoldedForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            SecurityUserState userState = getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isRelaunchWhenFolded;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setAppRelaunchModeAfterFoldedForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mUserStateLock) {
            SecurityUserState userStateLocked = getUserStateLocked(userId);
            SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isRelaunchWhenFolded = enabled;
            scheduleWriteSettings();
        }
    }

    public boolean isScRelaunchNeedConfirm(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return isScRelaunchNeedConfirmForUser(packageName, userId);
    }

    public boolean isScRelaunchNeedConfirmForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            SecurityUserState userState = getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isScRelaunchConfirm;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setScRelaunchNeedConfirmForUser(String packageName, boolean confirm, int userId) {
        synchronized (this.mUserStateLock) {
            SecurityUserState userStateLocked = getUserStateLocked(userId);
            SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isScRelaunchConfirm = confirm;
            scheduleWriteSettings();
        }
    }

    public void addAccessControlPass(String packageName) {
        checkPermissionByUid(1000);
        addAccessControlPassForUser(packageName, UserHandle.getCallingUserId());
    }

    public void addAccessControlPassForUser(String packageName, int userId) {
        checkPermission();
        this.mAccessControlImpl.addAccessControlPassForUser(packageName, userId);
    }

    public void removeAccessControlPass(String packageName) {
        checkPermission();
        int callingUserId = UserHandle.getCallingUserId();
        this.mAccessControlImpl.removeAccessControlPassAsUser(packageName, callingUserId);
    }

    public boolean checkAccessControlPass(String packageName, Intent intent) {
        int callingUserId = UserHandle.getCallingUserId();
        return this.mAccessControlImpl.checkAccessControlPassLocked(packageName, intent, callingUserId);
    }

    public boolean checkAccessControlPassAsUser(String packageName, Intent intent, int userId) {
        return this.mAccessControlImpl.checkAccessControlPassLocked(packageName, intent, userId);
    }

    public boolean getApplicationAccessControlEnabledAsUser(String packageName, int userId) {
        return this.mAccessControlImpl.getApplicationAccessControlEnabledLocked(packageName, userId);
    }

    public boolean getApplicationMaskNotificationEnabledAsUser(String packageName, int userId) {
        return this.mAccessControlImpl.getApplicationMaskNotificationEnabledLocked(packageName, userId);
    }

    public boolean getApplicationAccessControlEnabled(String packageName) {
        return this.mAccessControlImpl.getApplicationAccessControlEnabledLocked(packageName, UserHandle.getCallingUserId());
    }

    public void setApplicationAccessControlEnabled(String packageName, boolean enabled) {
        checkPermission();
        setApplicationAccessControlEnabledForUser(packageName, enabled, UserHandle.getCallingUserId());
    }

    public void setApplicationAccessControlEnabledForUser(String packageName, boolean enabled, int userId) {
        checkPermission();
        this.mAccessControlImpl.setApplicationAccessControlEnabledForUser(packageName, enabled, userId);
    }

    public void setApplicationMaskNotificationEnabledForUser(String packageName, boolean enabled, int userId) {
        checkPermission();
        this.mAccessControlImpl.setApplicationMaskNotificationEnabledForUser(packageName, enabled, userId);
    }

    public void removeAccessControlPassAsUser(String packageName, int userId) {
        checkPermission();
        this.mAccessControlImpl.removeAccessControlPassAsUser(packageName, userId);
    }

    public void setAccessControlPassword(String passwordType, String password, int userId) {
        checkPermission();
        this.mAccessController.setAccessControlPassword(passwordType, password, SecurityManager.getUserHandle(userId));
    }

    public boolean checkAccessControlPassword(String passwordType, String password, int userId) {
        checkPermission();
        return this.mAccessController.checkAccessControlPassword(passwordType, password, SecurityManager.getUserHandle(userId));
    }

    public boolean haveAccessControlPassword(int userId) {
        return this.mAccessController.haveAccessControlPassword(SecurityManager.getUserHandle(userId));
    }

    public String getAccessControlPasswordType(int userId) {
        checkPermission();
        return this.mAccessController.getAccessControlPasswordType(SecurityManager.getUserHandle(userId));
    }

    public boolean needFinishAccessControl(IBinder token) {
        checkPermission();
        return this.mAccessControlImpl.needFinishAccessControl(token);
    }

    public void finishAccessControl(String packageName, int userId) {
        checkPermission();
        this.mAccessControlImpl.finishAccessControl(packageName, userId);
    }

    public int activityResume(Intent intent) {
        return this.mAccessControlImpl.activityResume(intent);
    }

    public String getShouldMaskApps() {
        checkPermissionByUid(1000);
        return this.mAccessControlImpl.getShouldMaskApps();
    }

    public boolean getApplicationChildrenControlEnabled(String packageName) {
        boolean z;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUserStateLock) {
            try {
                try {
                    SecurityUserState userStateLocked = getUserStateLocked(callingUserId);
                    SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
                    z = ps.childrenControl;
                } catch (Exception e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    public void setApplicationChildrenControlEnabled(String packageName, boolean enabled) {
        checkPermission();
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUserStateLock) {
            SecurityUserState userStateLocked = getUserStateLocked(callingUserId);
            SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.childrenControl = enabled;
            scheduleWriteSettings();
        }
    }

    public IBinder getAppRunningControlIBinder() {
        return this.mAppRunningControlService.asBinder();
    }

    public void watchGreenGuardProcess() {
        GreenGuardManagerService.startWatchGreenGuardProcess(this.mContext);
    }

    public void setWakeUpTime(String componentName, long timeInSeconds) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_BOOT_TIME", TAG);
        this.mWakeUpTimeImpl.setWakeUpTime(componentName, timeInSeconds);
    }

    public long getWakeUpTime(String componentName) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_BOOT_TIME", TAG);
        return this.mWakeUpTimeImpl.getBootTimeFromMap(componentName);
    }

    private void checkWakePathPermission() {
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    public void pushUpdatePkgsData(List<String> updatePkgsList, boolean enable) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushUpdatePkgsData(updatePkgsList, enable);
    }

    public void pushWakePathData(int wakeType, ParceledListSlice wakePathRuleInfos, int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathRuleInfos(wakeType, wakePathRuleInfos.getList(), userId);
    }

    public void pushWakePathWhiteList(List<String> wakePathWhiteList, int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathWhiteList(wakePathWhiteList, userId);
    }

    public void pushWakePathConfirmDialogWhiteList(int type, List<String> whiteList) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathConfirmDialogWhiteList(type, whiteList);
    }

    public void removeWakePathData(int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().removeWakePathData(userId);
    }

    public void setTrackWakePathCallListLogEnabled(boolean enabled) {
        checkWakePathPermission();
        WakePathChecker.getInstance().setTrackWakePathCallListLogEnabled(enabled);
    }

    public ParceledListSlice getWakePathCallListLog() {
        checkWakePathPermission();
        return WakePathChecker.getInstance().getWakePathCallListLog();
    }

    public void registerWakePathCallback(IWakePathCallback callback) {
        checkWakePathPermission();
        WakePathChecker.getInstance().registerWakePathCallback(callback);
    }

    public void updateLauncherPackageNames() {
        WakePathChecker.getInstance().init(this.mContext);
    }

    public int getSecondSpaceId() {
        long callingId = Binder.clearCallingIdentity();
        try {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "second_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int getCurrentUserId() {
        return ProcessUtils.getCurrentUserId();
    }

    private void updateXSpaceSettings() {
        synchronized (this.mUserStateLock) {
            if (ConfigUtils.isSupportXSpace() && !this.mIsUpdated) {
                SecurityUserState userState = getUserStateLocked(0);
                SecurityUserState userStateXSpace = getUserStateLocked(999);
                Set<Map.Entry<String, SecurityPackageSettings>> packagesSet = userState.mPackages.entrySet();
                for (Map.Entry<String, SecurityPackageSettings> entrySet : packagesSet) {
                    String name = entrySet.getKey();
                    if (!TextUtils.isEmpty(name) && XSpaceUserHandle.isAppInXSpace(this.mContext, name)) {
                        SecurityPackageSettings value = entrySet.getValue();
                        SecurityPackageSettings psXSpace = new SecurityPackageSettings(name);
                        psXSpace.accessControl = value.accessControl;
                        psXSpace.childrenControl = value.childrenControl;
                        userStateXSpace.mPackages.put(name, psXSpace);
                    }
                }
                scheduleWriteSettings();
                this.mAccessControlImpl.updateMaskObserverValues();
            }
        }
    }

    public void setIncompatibleAppList(List<String> list) {
        checkPermission();
        if (list == null) {
            throw new NullPointerException("List is null");
        }
        synchronized (this.mIncompatibleAppList) {
            this.mIncompatibleAppList.clear();
            this.mIncompatibleAppList.addAll(list);
        }
    }

    public List<String> getIncompatibleAppList() {
        return this.mIncompatibleAppList;
    }

    private void checkWriteSecurePermission() {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Permission Denial: attempt to change application privacy revoke state from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
    }

    public void setAppPrivacyStatus(final String packageName, final boolean isOpen) {
        if (TextUtils.isEmpty(packageName)) {
            throw new RuntimeException("packageName can not be null or empty");
        }
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            String callingPackage = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
            if (!packageName.equals(callingPackage)) {
                checkWriteSecurePermission();
            }
        }
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.miui.server.SecurityManagerService$$ExternalSyntheticLambda3
            public final Object getOrThrow() {
                Boolean lambda$setAppPrivacyStatus$0;
                lambda$setAppPrivacyStatus$0 = SecurityManagerService.this.lambda$setAppPrivacyStatus$0(packageName, isOpen);
                return lambda$setAppPrivacyStatus$0;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Boolean lambda$setAppPrivacyStatus$0(String str, boolean z) throws Exception {
        return Boolean.valueOf(Settings.Secure.putInt(this.mContext.getContentResolver(), "privacy_status_" + str, z ? 1 : 0));
    }

    public boolean isAppPrivacyEnabled(final String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            throw new RuntimeException("packageName can not be null or empty");
        }
        return Boolean.TRUE.equals(Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.miui.server.SecurityManagerService$$ExternalSyntheticLambda2
            public final Object getOrThrow() {
                Boolean lambda$isAppPrivacyEnabled$1;
                lambda$isAppPrivacyEnabled$1 = SecurityManagerService.this.lambda$isAppPrivacyEnabled$1(packageName);
                return lambda$isAppPrivacyEnabled$1;
            }
        }));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Boolean lambda$isAppPrivacyEnabled$1(String packageName) throws Exception {
        return Boolean.valueOf(Settings.Secure.getInt(this.mContext.getContentResolver(), new StringBuilder().append("privacy_status_").append(packageName).toString(), 1) != 0);
    }

    public void setPrivacyApp(String packageName, int userId, boolean isPrivacy) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            SecurityUserState userStateLocked = getUserStateLocked(userId);
            SecurityPackageSettings ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isPrivacyApp = isPrivacy;
            scheduleWriteSettings();
        }
    }

    public boolean isPrivacyApp(String packageName, int userId) {
        boolean z;
        checkPermission();
        synchronized (this.mUserStateLock) {
            SecurityUserState userState = getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isPrivacyApp;
            } catch (Exception e) {
                Log.e(TAG, "isPrivacyApp error", e);
                return false;
            }
        }
        return z;
    }

    public List<String> getAllPrivacyApps(int userId) {
        List<String> privacyAppsList;
        checkPermission();
        synchronized (this.mUserStateLock) {
            privacyAppsList = new ArrayList<>();
            SecurityUserState userState = getUserStateLocked(userId);
            HashMap<String, SecurityPackageSettings> packages = userState.mPackages;
            Set<String> pkgNames = packages.keySet();
            for (String pkgName : pkgNames) {
                try {
                    SecurityPackageSettings ps = getPackageSetting(userState.mPackages, pkgName);
                    if (ps.isPrivacyApp) {
                        privacyAppsList.add(pkgName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getAllPrivacyApps error", e);
                }
            }
        }
        return privacyAppsList;
    }

    private void checkPermissionByUid(int... uids) {
        int callingUid = Binder.getCallingUid();
        boolean hasPermission = false;
        for (int i : uids) {
            if (UserHandle.getAppId(callingUid) == i) {
                hasPermission = true;
            }
        }
        if (!hasPermission) {
            throw new SecurityException("no permission to read file for UID:" + callingUid);
        }
    }

    private void checkGrantPermissionPkg(String allowPackage) {
        String callingPackageName = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (!allowPackage.equals(callingPackageName)) {
            throw new SecurityException("Permission Denial: attempt to grant/revoke permission from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", pkg=" + callingPackageName);
        }
    }

    private void checkPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 2000) {
            throw new SecurityException("no permission for UID:" + callingUid);
        }
        int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
        if (permission == 0) {
            return;
        }
        int managePermission = this.mContext.checkCallingOrSelfPermission(READ_AND_WRITE_PERMISSION_MANAGER);
        if (managePermission != 0) {
            throw new SecurityException("Permission Denial: attempt to change application state from pid=" + Binder.getCallingPid() + ", uid=" + callingUid);
        }
    }

    public boolean areNotificationsEnabledForPackage(String packageName, int uid) throws RemoteException {
        checkPermission();
        if (this.mINotificationManager == null) {
            this.mINotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mINotificationManager.areNotificationsEnabledForPackage(packageName, uid);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setNotificationsEnabledForPackage(String packageName, int uid, boolean enabled) throws RemoteException {
        checkPermission();
        if (this.mINotificationManager == null) {
            this.mINotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mINotificationManager.setNotificationsEnabledForPackage(packageName, uid, enabled);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void grantRuntimePermissionAsUser(String packageName, String permName, int userId) {
        checkGrantPermissionPkg(PACKAGE_PERMISSIONCENTER);
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) == 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.grantRuntimePermission(packageName, permName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void revokeRuntimePermissionAsUser(String packageName, String permName, int userId) {
        checkGrantPermissionPkg(PACKAGE_PERMISSIONCENTER);
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) != 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.revokeRuntimePermission(packageName, permName, userId, (String) null);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void revokeRuntimePermissionAsUserNotKill(String packageName, String permName, int userId) {
        checkGrantPermissionPkg(PACKAGE_PERMISSIONCENTER);
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) != 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.revokeRuntimePermission(packageName, permName, userId, "not kill");
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getPermissionFlagsAsUser(String permName, String packageName, int userId) {
        checkGrantPermissionPkg(PACKAGE_PERMISSIONCENTER);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mPermissionManagerService.getPermissionFlags(packageName, permName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updatePermissionFlagsAsUser(String permissionName, String packageName, int flagMask, int flagValues, int userId) {
        checkGrantPermissionPkg(PACKAGE_PERMISSIONCENTER);
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                this.mPermissionManagerService.updatePermissionFlags(packageName, permissionName, flagMask, flagValues, true, userId);
            } catch (Exception e) {
                Log.e(TAG, "updatePermissionFlagsAsUser failed: perm=" + permissionName + ", pkg=" + packageName + ", mask=" + flagMask + ", value=" + flagValues, e);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void registerLocationBlurryManager(ILocationBlurry manager) {
        checkWakePathPermission();
        MiuiBlurLocationManagerStub.get().registerLocationBlurryManager(manager);
    }

    public CellIdentity getBlurryCellLocation(CellIdentity location) {
        checkBlurLocationPermission();
        return MiuiBlurLocationManagerStub.get().getBlurryCellLocation(location);
    }

    public List<CellInfo> getBlurryCellInfos(List<CellInfo> location) {
        checkBlurLocationPermission();
        return MiuiBlurLocationManagerStub.get().getBlurryCellInfos(location);
    }

    private void checkBlurLocationPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) >= 10000) {
            throw new SecurityException("Uid " + callingUid + " can't get blur location info");
        }
    }

    public String getPlatformVAID() {
        if (UserHandle.getAppId(Binder.getCallingUid()) > 10000) {
            this.mContext.enforceCallingOrSelfPermission(PLATFORM_VAID_PERMISSION, "Not allowed get platform vaid from other!");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (TextUtils.isEmpty(this.mPlatformVAID)) {
                this.mPlatformVAID = readSystemDataStringFile(VAID_PLATFORM_CACHE_PATH);
            }
            Binder.restoreCallingIdentity(identity);
            return this.mPlatformVAID;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void pushPrivacyVirtualDisplayList(List<String> privacyList) {
        checkPermission();
        this.mPrivacyVirtualDisplay.clear();
        this.mPrivacyVirtualDisplay.addAll(privacyList);
        Log.i(TAG, "privacy virtual display updated! size=" + this.mPrivacyVirtualDisplay.size());
    }

    private void dumpPrivacyVirtualDisplay(PrintWriter out) {
        out.println("===================================SCREEN SHARE PROTECTION DUMP BEGIN========================================");
        if (this.mPrivacyVirtualDisplay.size() == 0) {
            out.println("Don't have any privacy virtual display package!");
        } else {
            for (String packageName : this.mPrivacyVirtualDisplay) {
                out.println("packageName: " + packageName);
            }
        }
        if (this.mPrivacyDisplayNameList.size() == 0) {
            out.println("Don't have any privacy virtual display name!");
        } else {
            for (String name : this.mPrivacyDisplayNameList) {
                out.println("virtual display: " + name);
            }
        }
        out.println("====================================SCREEN SHARE PROTECTION DUMP END========================================");
    }

    public String getPackageNameByPid(int pid) {
        ProcessRecord record;
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && UserHandle.getAppId(callingUid) != 1000 && ((record = ProcessUtils.getProcessRecordByPid(Binder.getCallingPid())) == null || record.getApplicationInfo() == null || !record.getApplicationInfo().isSystemApp())) {
            return null;
        }
        return ProcessUtils.getPackageNameByPid(pid);
    }

    public boolean isAllowStartService(Intent service, int userId) {
        checkPermission();
        return AutoStartManagerServiceStub.getInstance().isAllowStartService(this.mContext, service, userId);
    }

    public IBinder getTopActivity() {
        checkPermission();
        HashMap<String, Object> topActivity = WindowProcessUtils.getTopRunningActivityInfo();
        if (topActivity != null) {
            Intent intent = (Intent) topActivity.get("intent");
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                String clsName = componentName.getClassName();
                String pkgName = componentName.getPackageName();
                if ("com.google.android.packageinstaller".equals(pkgName) && !"com.android.packageinstaller.PackageInstallerActivity".equals(clsName)) {
                    return (IBinder) topActivity.get("token");
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public void exemptTemporarily(String packageName) {
        checkPermission();
        PendingIntentRecordStub.get().exemptTemporarily(packageName);
    }

    public boolean putSystemDataStringFile(String path, String value) {
        checkPermissionByUid(1000);
        File file = new File(path);
        RandomAccessFile raf = null;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            try {
                raf = new RandomAccessFile(file, "rw");
                raf.setLength(0L);
                raf.writeUTF(value);
                try {
                    raf.close();
                    return true;
                } catch (IOException e2) {
                    e2.printStackTrace();
                    return true;
                }
            } catch (IOException e3) {
                e3.printStackTrace();
                if (raf != null) {
                    try {
                        raf.close();
                        return false;
                    } catch (IOException e4) {
                        e4.printStackTrace();
                        return false;
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:27:0x0029 -> B:10:0x0044). Please report as a decompilation issue!!! */
    public String readSystemDataStringFile(String path) {
        checkPermissionByUid(1000);
        File file = new File(path);
        RandomAccessFile raf = null;
        String result = null;
        try {
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (file.exists()) {
            try {
                try {
                    raf = new RandomAccessFile(file, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                    result = raf.readUTF();
                    raf.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                    if (raf != null) {
                        raf.close();
                    }
                }
            } catch (Throwable th) {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        }
        return result;
    }

    public SecurityUserState getUserStateLocked(int userHandle) {
        SecurityUserState userState = this.mUserStates.get(userHandle);
        if (userState == null) {
            SecurityUserState userState2 = new SecurityUserState();
            userState2.userHandle = userHandle;
            this.mUserStates.put(userHandle, userState2);
            return userState2;
        }
        return userState;
    }

    public SecurityPackageSettings getPackageSetting(HashMap<String, SecurityPackageSettings> packages, String packageName) {
        SecurityPackageSettings ps = packages.get(packageName);
        if (ps == null) {
            SecurityPackageSettings ps2 = new SecurityPackageSettings(packageName);
            packages.put(packageName, ps2);
            return ps2;
        }
        return ps;
    }

    public void scheduleWriteSettings() {
        if (this.mSecurityWriteHandler.hasMessages(1)) {
            return;
        }
        this.mSecurityWriteHandler.sendEmptyMessageDelayed(1, 1000L);
    }

    private void readSettings() {
        if (!this.mSettingsFile.getBaseFile().exists()) {
            return;
        }
        try {
            FileInputStream fis = this.mSettingsFile.openRead();
            try {
                readPackagesSettings(fis);
                if (fis != null) {
                    fis.close();
                }
            } finally {
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading package settings", e);
        }
    }

    private void readPackagesSettings(FileInputStream fis) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fis, null);
        for (int eventType = parser.getEventType(); eventType != 2 && eventType != 1; eventType = parser.next()) {
        }
        String tagName = parser.getName();
        if ("packages".equals(tagName)) {
            String updateVersion = parser.getAttributeValue(null, "updateVersion");
            if (!TextUtils.isEmpty(updateVersion) && "1.0".equals(updateVersion)) {
                this.mIsUpdated = true;
            }
            int eventType2 = parser.next();
            do {
                if (eventType2 == 2 && parser.getDepth() == 2) {
                    String tagName2 = parser.getName();
                    if ("package".equals(tagName2)) {
                        String name = parser.getAttributeValue(null, "name");
                        if (TextUtils.isEmpty(name)) {
                            Slog.i(TAG, "read current package name is empty, skip");
                        } else {
                            SecurityPackageSettings ps = new SecurityPackageSettings(name);
                            int userHandle = 0;
                            String userHandleStr = parser.getAttributeValue(null, "u");
                            if (!TextUtils.isEmpty(userHandleStr)) {
                                userHandle = Integer.parseInt(userHandleStr);
                            }
                            ps.accessControl = Boolean.parseBoolean(parser.getAttributeValue(null, "accessControl"));
                            ps.childrenControl = Boolean.parseBoolean(parser.getAttributeValue(null, "childrenControl"));
                            ps.maskNotification = Boolean.parseBoolean(parser.getAttributeValue(null, "maskNotification"));
                            ps.isPrivacyApp = Boolean.parseBoolean(parser.getAttributeValue(null, "isPrivacyApp"));
                            ps.isDarkModeChecked = Boolean.parseBoolean(parser.getAttributeValue(null, "isDarkModeChecked"));
                            ps.isGameStorageApp = Boolean.parseBoolean(parser.getAttributeValue(null, "isGameStorageApp"));
                            ps.isRemindForRelaunch = Boolean.parseBoolean(parser.getAttributeValue(null, "isRemindForRelaunch"));
                            ps.isRelaunchWhenFolded = Boolean.parseBoolean(parser.getAttributeValue(null, "isRelaunchWhenFolded"));
                            ps.isScRelaunchConfirm = Boolean.parseBoolean(parser.getAttributeValue(null, "isScRelaunchConfirm"));
                            synchronized (this.mUserStateLock) {
                                SecurityUserState userState = getUserStateLocked(userHandle);
                                userState.mPackages.put(name, ps);
                            }
                        }
                    }
                }
                eventType2 = parser.next();
            } while (eventType2 != 1);
        }
    }

    public void writeSettings() {
        try {
            ArrayList<SecurityUserState> userStates = new ArrayList<>();
            synchronized (this.mUserStateLock) {
                int size = this.mUserStates.size();
                for (int i = 0; i < size; i++) {
                    SecurityUserState state = this.mUserStates.valueAt(i);
                    SecurityUserState userState = new SecurityUserState();
                    userState.userHandle = state.userHandle;
                    userState.mPackages.putAll(new HashMap(state.mPackages));
                    userStates.add(userState);
                }
            }
            FileOutputStream fos = this.mSettingsFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, "packages");
            fastXmlSerializer.attribute(null, "updateVersion", "1.0");
            Iterator<SecurityUserState> it = userStates.iterator();
            while (it.hasNext()) {
                SecurityUserState userState2 = it.next();
                for (SecurityPackageSettings ps : userState2.mPackages.values()) {
                    if (TextUtils.isEmpty(ps.name)) {
                        Slog.i(TAG, "write current package name is empty, skip");
                    } else {
                        fastXmlSerializer.startTag(null, "package");
                        fastXmlSerializer.attribute(null, "name", ps.name);
                        fastXmlSerializer.attribute(null, "accessControl", String.valueOf(ps.accessControl));
                        fastXmlSerializer.attribute(null, "childrenControl", String.valueOf(ps.childrenControl));
                        fastXmlSerializer.attribute(null, "maskNotification", String.valueOf(ps.maskNotification));
                        fastXmlSerializer.attribute(null, "isPrivacyApp", String.valueOf(ps.isPrivacyApp));
                        fastXmlSerializer.attribute(null, "isDarkModeChecked", String.valueOf(ps.isDarkModeChecked));
                        fastXmlSerializer.attribute(null, "isGameStorageApp", String.valueOf(ps.isGameStorageApp));
                        fastXmlSerializer.attribute(null, "isRemindForRelaunch", String.valueOf(ps.isRemindForRelaunch));
                        fastXmlSerializer.attribute(null, "isRelaunchWhenFolded", String.valueOf(ps.isRelaunchWhenFolded));
                        fastXmlSerializer.attribute(null, "isScRelaunchConfirm", String.valueOf(ps.isScRelaunchConfirm));
                        fastXmlSerializer.attribute(null, "u", String.valueOf(userState2.userHandle));
                        fastXmlSerializer.endTag(null, "package");
                    }
                }
            }
            fastXmlSerializer.endTag(null, "packages");
            fastXmlSerializer.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (Exception e1) {
            Log.e(TAG, "Error writing package settings file", e1);
            if (0 != 0) {
                this.mSettingsFile.failWrite(null);
            }
        }
    }

    public void startWatchingAppBehavior(int behavior, boolean includeSystem) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.startWatchingAppBehavior(behavior, includeSystem);
    }

    public void startWatchingAppBehaviors(int behavior, String[] pkgNames, boolean includeSystem) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.startWatchingAppBehavior(behavior, pkgNames, includeSystem);
    }

    public void stopWatchingAppBehavior(int behavior) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.stopWatchingAppBehavior(behavior);
    }

    public void stopWatchingAppBehaviors(int behavior, String[] pkgNames) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.stopWatchingAppBehavior(behavior, pkgNames);
    }

    public void recordAppBehavior(AppBehavior appBehavior) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        if (this.mAppBehavior.hasAppBehaviorWatching()) {
            this.mAppBehavior.recordAppBehaviorAsync(appBehavior);
        }
    }

    public void persistAppBehavior() {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.persistBehaviorRecord();
    }

    public void updateBehaviorThreshold(int behavior, long threshold) {
        checkGrantPermissionPkg("com.miui.securitycenter");
        this.mAppBehavior.updateBehaviorThreshold(behavior, threshold);
    }

    public boolean hasAppBehaviorWatching() {
        checkGrantPermissionPkg("com.miui.securitycenter");
        return this.mAppBehavior.hasAppBehaviorWatching();
    }

    public void addRestrictChainMaps(String caller, String callee) {
        if (TextUtils.isEmpty(callee) || TextUtils.isEmpty(caller)) {
            return;
        }
        checkPermissionByUid(1000);
        if (!TextUtils.equals("com.tencent.mm", callee) && !TextUtils.equals(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, callee)) {
            List<String> callees = this.restrictChainMaps.get(caller);
            if (callees != null) {
                callees.add(callee);
            } else {
                ArrayList<String> strings = new ArrayList<>();
                strings.add(callee);
                this.restrictChainMaps.put(caller, strings);
            }
        }
        List<String> callers = this.restrictChainMaps.get(callee);
        if (callers != null) {
            callers.add(caller);
        } else {
            ArrayList<String> strings2 = new ArrayList<>();
            strings2.add(caller);
            this.restrictChainMaps.put(callee, strings2);
        }
        if (this.restrictChainMaps.size() > 100) {
            try {
                LinkedHashMap<String, List<String>> linkedHashMap = this.restrictChainMaps;
                linkedHashMap.remove(linkedHashMap.entrySet().iterator().next().getKey());
            } catch (Exception e) {
            }
        }
    }

    public void removeCalleeRestrictChain(String callee) {
        if (TextUtils.isEmpty(callee)) {
            return;
        }
        for (Map.Entry<String, List<String>> next : this.restrictChainMaps.entrySet()) {
            List<String> callees = next.getValue();
            int i = 0;
            while (true) {
                if (i >= callees.size()) {
                    break;
                }
                if (!callee.equals(callees.get(i))) {
                    i++;
                } else {
                    callees.remove(callee);
                    break;
                }
            }
        }
    }

    public boolean exemptByRestrictChain(String caller, String callee) {
        List<String> callees;
        return this.restrictChainMaps.containsKey(caller) && (callees = this.restrictChainMaps.get(caller)) != null && callees.contains(callee);
    }

    public Map<String, String> getSimulatedTouchInfo() {
        checkPermissionByUid(1000);
        return SafetyDetectManagerStub.getInstance().getSimulatedTouchInfo();
    }

    public void switchSimulatedTouchDetect(Map<String, String> config) {
        checkPermissionByUid(1000);
        SafetyDetectManagerStub.getInstance().switchSimulatedTouchDetect(config);
    }

    public void updateAppWidgetVisibility(String packageName, int uid, boolean visible, List<String> permissions, String reason) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_APPWIDGET", reason);
        if (permissions == null || permissions.contains("android.permission.RECORD_AUDIO") || permissions.contains("android.permission.CAMERA") || !permissions.contains("android.permission.ACCESS_FINE_LOCATION")) {
            return;
        }
        if (this.mAppOpsInternal == null) {
            this.mAppOpsInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
        }
        final SparseArray<String> uidPackageNames = new SparseArray<>();
        uidPackageNames.put(uid, packageName);
        this.mAppOpsInternal.updateAppWidgetVisibility(uidPackageNames, visible);
        Log.i(TAG, "update appwidget of uid: " + uid + " package: " + packageName + " visible: " + visible + " reason: " + reason);
        int what = (uid + packageName).hashCode();
        if (this.mSecurityWriteHandler.hasMessages(what)) {
            this.mSecurityWriteHandler.removeMessages(what);
        }
        if (visible) {
            Message msg = Message.obtain(this.mSecurityWriteHandler, what);
            msg.setCallback(new Runnable() { // from class: com.miui.server.SecurityManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SecurityManagerService.this.lambda$updateAppWidgetVisibility$2(uidPackageNames);
                }
            });
            this.mSecurityWriteHandler.sendMessageDelayed(msg, 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAppWidgetVisibility$2(SparseArray uidPackageNames) {
        this.mAppOpsInternal.updateAppWidgetVisibility(uidPackageNames, false);
    }

    /* loaded from: classes.dex */
    private final class LocalService extends SecurityManagerInternal {
        public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
        public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
        public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;

        private LocalService() {
        }

        public void onDisplayDeviceEvent(String packageName, String name, IBinder token, int event) {
            if (SecurityManagerService.this.mPrivacyVirtualDisplay.contains(packageName) && 1 == event) {
                SecurityManagerService.this.mPrivacyDisplayNameList.add(name);
                SurfaceControl.setMiSecurityDisplay(token, true);
            }
            if (3 == event && SecurityManagerService.this.mPrivacyDisplayNameList.remove(name)) {
                SurfaceControl.setMiSecurityDisplay(token, false);
            }
        }

        public boolean checkGameBoosterPayPassAsUser(String packageName, Intent intent, int userId) {
            return (SecurityManagerService.this.mGameBoosterImpl.isVtbMode(SecurityManagerService.this.mContext) || SecurityManagerService.this.getGameMode(userId)) && SecurityManagerService.this.mAccessController.filterIntentLocked(false, true, packageName, intent);
        }

        public boolean isForceLaunchNewTask(String rawPackageName, String className) {
            return SecurityManagerService.this.mAccessController.getForceLaunchList().contains(className);
        }

        public boolean isApplicationLockActivity(String className) {
            return TextUtils.equals(AccessController.APP_LOCK_CLASSNAME, className);
        }

        public boolean checkAllowStartActivity(String callerPkgName, String calleePkgName, Intent intent, int callerUid, int calleeUid) {
            if (!SecurityManagerService.this.mUserManager.exists(UserHandle.getUserId(calleeUid))) {
                return true;
            }
            boolean ret = SecurityManagerService.this.mAccessController.filterIntentLocked(true, calleePkgName, intent);
            if (PreloadedAppPolicy.isProtectedDataApp(SecurityManagerService.this.mContext, callerPkgName, 0) || PreloadedAppPolicy.isProtectedDataApp(SecurityManagerService.this.mContext, calleePkgName, 0)) {
                return true;
            }
            if (!ret) {
                return WakePathChecker.getInstance().checkAllowStartActivity(callerPkgName, calleePkgName, callerUid, calleeUid);
            }
            return ret;
        }

        public boolean checkGameBoosterAntiMsgPassAsUser(String packageName, Intent intent) {
            return !SecurityManagerService.this.mAccessController.filterIntentLocked(false, packageName, intent);
        }

        public boolean isGameBoosterActive(int userId) {
            return SecurityManagerService.this.getGameMode(userId);
        }

        public boolean checkAccessControlPassAsUser(String packageName, Intent intent, int userId) {
            return SecurityManagerService.this.checkAccessControlPassAsUser(packageName, intent, userId);
        }

        public boolean isAccessControlActive(int userId) {
            SecurityUserState userState = SecurityManagerService.this.getUserStateLocked(userId);
            return SecurityManagerService.this.mAccessControlImpl.getAccessControlEnabledLocked(userState);
        }

        public boolean isBlockActivity(Intent intent) {
            return SecurityManagerService.this.mAppRunningControlService.isBlockActivity(intent);
        }

        public void recordDurationInfo(int behavior, IBinder binder, int uid, String pkgName, boolean start) {
            SecurityManagerService.this.mAppDuration.onDurationEvent(behavior, binder, uid, pkgName, start);
        }

        public long getThreshold(int behavior) {
            return SecurityManagerService.this.mAppBehavior.getThreshold(behavior);
        }

        public void recordAppChainAsync(int behavior, String upPkg, String downPkg, String callData) {
            if (SecurityManagerService.this.mAppBehavior.hasAppBehaviorWatching()) {
                SecurityManagerService.this.mAppBehavior.recordAppBehaviorAsync(AppBehavior.buildChainEvent(behavior, upPkg, downPkg, callData));
            }
        }

        public void setAppDarkModeForUser(String packageName, boolean enabled, int userId) {
            synchronized (SecurityManagerService.this.mUserStateLock) {
                SecurityUserState userStateLocked = SecurityManagerService.this.getUserStateLocked(userId);
                SecurityPackageSettings ps = SecurityManagerService.this.getPackageSetting(userStateLocked.mPackages, packageName);
                ps.isDarkModeChecked = enabled;
                SecurityManagerService.this.scheduleWriteSettings();
            }
        }

        public boolean getAppDarkModeForUser(String packageName, int userId) {
            boolean z;
            synchronized (SecurityManagerService.this.mUserStateLock) {
                SecurityUserState userState = SecurityManagerService.this.getUserStateLocked(userId);
                try {
                    SecurityPackageSettings ps = SecurityManagerService.this.getPackageSetting(userState.mPackages, packageName);
                    z = ps.isDarkModeChecked;
                } catch (Exception e) {
                    return false;
                }
            }
            return z;
        }

        public void recordAppBehaviorAsync(int behavior, String pkgName, long count, String extra) {
            if (SecurityManagerService.this.mAppBehavior.hasAppBehaviorWatching()) {
                SecurityManagerService.this.mAppBehavior.recordAppBehaviorAsync(AppBehavior.buildCountEvent(behavior, pkgName, count, extra));
            }
        }

        public boolean exemptByRestrictChain(String callingPackage, String packageName) {
            return SecurityManagerService.this.exemptByRestrictChain(callingPackage, packageName);
        }

        public void removeCalleeRestrictChain(String callee) {
            SecurityManagerService.this.removeCalleeRestrictChain(callee);
        }

        public boolean isAllowedDeviceProvision() {
            return SecurityManagerService.this.mAllowedDeviceProvision;
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final SecurityManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new SecurityManagerService(context);
        }

        public void onStart() {
            publishBinderService("security", this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 1000) {
                SecurityWriteHandler securityWriteHandler = this.mService.mSecurityWriteHandler;
                final SecurityManagerService securityManagerService = this.mService;
                Objects.requireNonNull(securityManagerService);
                securityWriteHandler.post(new Runnable() { // from class: com.miui.server.SecurityManagerService$Lifecycle$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        SecurityManagerService.this.initWhenBootCompleted();
                    }
                });
            }
        }
    }
}
