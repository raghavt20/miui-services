package com.android.server.pm;

import android.R;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParserStub;
import android.content.pm.ResolveInfo;
import android.content.pm.SigningDetails;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.PinnerService;
import com.android.server.SystemConfig;
import com.android.server.am.ProcessUtils;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.pm.PackageEventRecorder;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackageInternal;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.permission.LegacyPermission;
import com.android.server.pm.permission.LegacyPermissionSettings;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceImpl;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.verify.domain.DomainVerificationService;
import com.android.server.wifi.WifiDrvUEventObserver;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.enterprise.ApplicationHelper;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.enterprise.signature.EnterpriseVerifier;
import com.miui.hybrid.hook.HookClient;
import com.miui.server.GreenGuardManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import libcore.io.IoUtils;
import libcore.util.HexEncoding;
import miui.content.pm.PreloadedAppPolicy;
import miui.enterprise.ApplicationHelperStub;
import miui.enterprise.EnterpriseManagerStub;
import miui.os.Build;
import miui.util.DeviceLevel;
import miui.util.FeatureParser;
import miui.util.ReflectionUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

@MiuiStubHead(manifestName = "com.android.server.pm.PackageManagerServiceStub$$")
/* loaded from: classes.dex */
public class PackageManagerServiceImpl extends PackageManagerServiceStub {
    private static final String ANDROID_INSTALLER_PACKAGE = "com.android.packageinstaller";
    private static final String APP_LIST_FILE = "/system/etc/install_app_filter.xml";
    private static final String CARRIER_SYS_APPS_LIST = "/product/opcust/common/carrier_sys_apps_list.xml";
    private static final String CUSTOMIZED_REGION;
    static final int DELETE_FAILED_FORBIDED_BY_MIUI = -1000;
    static final String[] EP_INSTALLER_PKG_WHITELIST;
    private static final String GOOGLE_INSTALLER_PACKAGE = "com.google.android.packageinstaller";
    private static final String GOOGLE_WEB_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
    public static final int INSTALL_FULL_APP = 16384;
    public static final int INSTALL_REASON_USER = 4;
    private static final boolean IS_GLOBAL_REGION_BUILD;
    private static boolean IS_INTERNATIONAL_BUILD = false;
    private static final String MANAGED_PROVISION = "com.android.managedprovisioning";
    static final String[] MIUI_CORE_APPS;
    private static final String MIUI_INSTALLER_PACKAGE = "com.miui.packageinstaller";
    private static final String MIUI_MARKET_PACKAGE = "com.xiaomi.market";
    static final int MIUI_VERIFICATION_TIMEOUT = -100;
    private static final File PACKAGE_EVENT_DIR;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String PACKAGE_WEBVIEW = "com.google.android.webview";
    private static final String PKMS_ATEST_NAME = "com.android.server.pm.test.service.server";
    private static final String SHIM_PKG = "com.android.cts.priv.ctsshim";
    private static final boolean SUPPORT_DEL_COTA_APP;
    static final String TAG = "PKMSImpl";
    private static final String TAG_ADD_APPS = "add_apps";
    private static final String TAG_APP = "app";
    private static final String TAG_IGNORE_APPS = "ignore_apps";
    static final ArrayList<String> sAllowPackage;
    private static final Set<String> sClearPermissionSet;
    private static volatile Handler sFirstUseHandler;
    private static Object sFirstUseLock;
    private static HandlerThread sFirstUseThread;
    private static final Set<String> sGLPhoneAppsSet;
    public static final Set<String> sInstallerSet;
    public static final boolean sIsBootLoaderLocked = "locked".equals(SystemProperties.get("ro.boot.vbmeta.device_state"));
    public static final boolean sIsReleaseRom = Build.TAGS.contains("release-key");
    static final ArrayList<String> sNoVerifyAllowPackage;
    private static final Set<String> sNotSupportUpdateSystemApps;
    static ArrayList<String> sShellCheckPermissions;
    private static final Set<String> sSilentlyUninstallPackages;
    private static final Set<String> sTHPhoneAppsSet;
    private Context mContext;
    private String mCurrentPackageInstaller;
    private DexManager mDexManager;
    private DexOptHelper mDexOptHelper;
    private DexoptServiceThread mDexoptServiceThread;
    private DomainVerificationService mDomainVerificationService;
    private MiuiDexopt mMiuiDexopt;
    private AndroidPackage mMiuiInstallerPackage;
    private PackageSetting mMiuiInstallerPackageSetting;
    private MiuiPreinstallHelper mMiuiPreinstallHelper;
    private PackageManagerService.IPackageManagerImpl mPM;
    private PackageManagerCloudHelper mPackageManagerCloudHelper;
    private PackageDexOptimizer mPdo;
    private Settings mPkgSettings;
    private PackageManagerService mPms;
    private final Set<String> mIgnoreApks = new HashSet();
    private final Set<String> mIgnorePackages = new HashSet();
    private int thirdAppCount = 0;
    private int systemAppCount = 0;
    private int persistAppCount = 0;
    private String mRsaFeature = null;
    private boolean mIsGlobalCrbtSupported = false;
    private HashMap<String, String> mCotaApps = new HashMap<>();
    private List<String> mIsSupportAttention = new ArrayList();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PackageManagerServiceImpl> {

        /* compiled from: PackageManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PackageManagerServiceImpl INSTANCE = new PackageManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PackageManagerServiceImpl m2135provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PackageManagerServiceImpl m2134provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.PackageManagerServiceImpl is marked as singleton");
        }
    }

    static {
        ArraySet arraySet = new ArraySet();
        sInstallerSet = arraySet;
        arraySet.add(MIUI_INSTALLER_PACKAGE);
        arraySet.add(GOOGLE_INSTALLER_PACKAGE);
        arraySet.add(ANDROID_INSTALLER_PACKAGE);
        HashSet hashSet = new HashSet();
        sNotSupportUpdateSystemApps = hashSet;
        hashSet.add("com.android.bluetooth");
        hashSet.add("com.miui.powerkeeper");
        HashSet hashSet2 = new HashSet();
        sTHPhoneAppsSet = hashSet2;
        HashSet hashSet3 = new HashSet();
        sGLPhoneAppsSet = hashSet3;
        hashSet2.add("com.android.contacts");
        hashSet2.add("com.android.incallui");
        hashSet3.add("com.google.android.contacts");
        hashSet3.add("com.google.android.dialer");
        String str = SystemProperties.get("ro.miui.customized.region", "");
        CUSTOMIZED_REGION = str;
        SUPPORT_DEL_COTA_APP = SystemProperties.getBoolean("ro.miui.cota_app_del_support", false);
        IS_GLOBAL_REGION_BUILD = "global".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"));
        IS_INTERNATIONAL_BUILD = SystemProperties.get("ro.product.mod_device", "").contains("_global");
        PACKAGE_EVENT_DIR = new File("data/system/package-event");
        HashSet hashSet4 = new HashSet();
        sClearPermissionSet = hashSet4;
        hashSet4.add(MIUI_INSTALLER_PACKAGE);
        hashSet4.add(GOOGLE_INSTALLER_PACKAGE);
        hashSet4.add(ANDROID_INSTALLER_PACKAGE);
        hashSet4.add("com.miui.cleanmaster");
        hashSet4.add("com.miui.thirdappassistant");
        hashSet4.add("com.miui.screenrecorder");
        EP_INSTALLER_PKG_WHITELIST = new String[]{ANDROID_INSTALLER_PACKAGE, MIUI_INSTALLER_PACKAGE};
        MIUI_CORE_APPS = new String[]{"com.lbe.security.miui", ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.android.updater", MIUI_MARKET_PACKAGE, "com.xiaomi.finddevice", "com.miui.home"};
        HashSet hashSet5 = new HashSet();
        sSilentlyUninstallPackages = hashSet5;
        hashSet5.add(SystemProperties.get("ro.miui.product.home", "com.miui.home"));
        hashSet5.add(MIUI_MARKET_PACKAGE);
        hashSet5.add("com.xiaomi.mipicks");
        hashSet5.add("com.xiaomi.discover");
        hashSet5.add("com.xiaomi.gamecenter");
        hashSet5.add("com.xiaomi.gamecenter.pad");
        hashSet5.add("com.miui.global.packageinstaller");
        hashSet5.add(GOOGLE_INSTALLER_PACKAGE);
        hashSet5.add(GreenGuardManagerService.GREEN_KID_AGENT_PKG_NAME);
        hashSet5.add("com.miui.cleaner");
        hashSet5.add("com.miui.cloudbackup");
        if (IS_INTERNATIONAL_BUILD) {
            hashSet5.add("de.telekom.tsc");
            hashSet5.add("com.sfr.android.sfrjeux");
            hashSet5.add("com.altice.android.myapps");
            if ("jp_kd".equals(str)) {
                hashSet5.add("com.facebook.system");
            }
        }
        ArrayList<String> arrayList = new ArrayList<>();
        sShellCheckPermissions = arrayList;
        arrayList.add("android.permission.SEND_SMS");
        sShellCheckPermissions.add("android.permission.CALL_PHONE");
        sShellCheckPermissions.add("android.permission.READ_CONTACTS");
        sShellCheckPermissions.add("android.permission.WRITE_CONTACTS");
        sShellCheckPermissions.add("android.permission.CLEAR_APP_USER_DATA");
        sShellCheckPermissions.add("android.permission.WRITE_SECURE_SETTINGS");
        sShellCheckPermissions.add("android.permission.WRITE_SETTINGS");
        sShellCheckPermissions.add("android.permission.MANAGE_DEVICE_ADMINS");
        sShellCheckPermissions.add("android.permission.UPDATE_APP_OPS_STATS");
        sShellCheckPermissions.add("android.permission.INJECT_EVENTS");
        sShellCheckPermissions.add("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.GRANT_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.REVOKE_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.SET_PREFERRED_APPLICATIONS");
        if ((Build.IS_DEBUGGABLE && (SystemProperties.getInt("ro.secureboot.devicelock", 0) == 0 || "unlocked".equals(SystemProperties.get("ro.secureboot.lockstate")))) || Build.IS_TABLET) {
            sShellCheckPermissions.clear();
        }
        sAllowPackage = new ArrayList<>();
        ArrayList<String> arrayList2 = new ArrayList<>();
        sNoVerifyAllowPackage = arrayList2;
        arrayList2.add("android");
        arrayList2.add("com.android.provision");
        arrayList2.add("com.miui.securitycore");
        arrayList2.add("com.android.vending");
        arrayList2.add(MIUI_MARKET_PACKAGE);
        arrayList2.add("com.xiaomi.gamecenter");
        arrayList2.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList2.add("com.android.updater");
        arrayList2.add("com.amazon.venezia");
        if (EnterpriseManagerStub.ENTERPRISE_ACTIVATED) {
            arrayList2.add("com.xiaomi.ep");
        }
        if (IS_INTERNATIONAL_BUILD) {
            arrayList2.add("com.miui.cotaservice");
            arrayList2.add("com.xiaomi.discover");
            arrayList2.add("com.xiaomi.mipicks");
        }
        if (Build.IS_DEBUGGABLE) {
            arrayList2.add(PKMS_ATEST_NAME);
        }
        sFirstUseLock = new Object();
    }

    public static PackageManagerServiceImpl get() {
        return (PackageManagerServiceImpl) PackageManagerServiceStub.get();
    }

    void init(PackageManagerService pms, Settings pkgSettings, Context context) {
        this.mPms = pms;
        this.mPdo = pms.mInjector.getPackageDexOptimizer();
        this.mContext = context;
        this.mPkgSettings = pkgSettings;
        this.mDexOptHelper = new DexOptHelper(pms);
        this.mDexManager = pms.mInjector.getDexManager();
        this.mMiuiPreinstallHelper = MiuiPreinstallHelper.getInstance();
        this.mMiuiDexopt = new MiuiDexopt(pms, this.mDexOptHelper, context);
        this.mPackageManagerCloudHelper = new PackageManagerCloudHelper(this.mPms, this.mContext);
        this.mIsSupportAttention = Arrays.asList(this.mContext.getResources().getStringArray(285409364));
        if (SystemProperties.getBoolean("pm.dexopt.async.enabled", true)) {
            DexoptServiceThread dexoptServiceThread = new DexoptServiceThread(this.mPms, this.mPdo);
            this.mDexoptServiceThread = dexoptServiceThread;
            dexoptServiceThread.start();
        }
        LocalServices.addService(PackageEventRecorderInternal.class, new PackageEventRecorder(PACKAGE_EVENT_DIR, new Function() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return PackageManagerServiceImpl.lambda$init$0((PackageEventRecorder) obj);
            }
        }, true));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageEventRecorder.RecorderHandler lambda$init$0(PackageEventRecorder recorder) {
        return new PackageEventRecorder.RecorderHandler(recorder, IoThread.get().getLooper());
    }

    void initIPackageManagerImpl(PackageManagerService.IPackageManagerImpl pm) {
        this.mPM = pm;
    }

    void addMiuiSharedUids() {
        this.mPkgSettings.addSharedUserLPw("android.uid.theme", 6101, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.backup", 6100, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.updater", 6102, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.finddevice", 6110, 1, 8);
    }

    public void beforeSystemReady() {
        PreinstallApp.exemptPermissionRestrictions();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, new ContentObserver(this.mPms.mHandler) { // from class: com.android.server.pm.PackageManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PackageManagerServiceImpl.this.mDomainVerificationService.verifyPackages((List) null, false);
            }
        });
        this.mPackageManagerCloudHelper.registerCloudDataObserver();
        this.mMiuiDexopt.preConfigMiuiDexopt(this.mContext);
        final MiuiPreinstallHelper miuiPreinstallHelper = MiuiPreinstallHelper.getInstance();
        if (miuiPreinstallHelper.isSupportNewFrame()) {
            this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.PackageManagerServiceImpl.2
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    miuiPreinstallHelper.scheduleJob();
                }
            }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        }
    }

    private boolean isProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 1;
    }

    void performPreinstallApp() {
        PreinstallApp.copyPreinstallApps(this.mPms, this.mPkgSettings);
    }

    public void initBeforeScanNonSystemApps() {
        CloudControlPreinstallService.startCloudControlService();
        updateDefaultPkgInstallerLocked();
        checkGTSSpecAppOptMode();
    }

    boolean isPreinstallApp(String packageName) {
        if (this.mMiuiPreinstallHelper.isSupportNewFrame()) {
            return this.mMiuiPreinstallHelper.isMiuiPreinstallApp(packageName);
        }
        return PreinstallApp.isPreinstallApp(packageName);
    }

    boolean isVerificationEnabled(int installerUid) {
        if (!isProvisioned()) {
            return false;
        }
        if (isCTS()) {
            return true;
        }
        return IS_INTERNATIONAL_BUILD && installerUid != 1000;
    }

    public void initIgnoreApps() {
        if (!FeatureParser.getBoolean("support_fm", true)) {
            this.mIgnoreApks.add("/system/app/FM.apk");
            this.mIgnoreApks.add("/system/app/FM");
        }
        readIgnoreApks();
        this.mIgnorePackages.add("com.sogou.inputmethod.mi");
        boolean isCmccCooperationDevice = this.mContext.getResources().getBoolean(285540411);
        this.mIsGlobalCrbtSupported = this.mContext.getResources().getBoolean(285540416);
        if (IS_INTERNATIONAL_BUILD || (!Build.IS_CM_CUSTOMIZATION && (!Build.IS_CT_CUSTOMIZATION || !isCmccCooperationDevice))) {
            this.mIgnorePackages.add("com.miui.dmregservice");
        }
        try {
            addIgnoreApks("ignoredAppsForPackages", this.mIgnorePackages);
            addIgnoreApks("ignoredAppsForApkPath", this.mIgnoreApks);
        } catch (NumberFormatException e) {
            Slog.e(TAG, e.toString());
        }
        String productPath = Environment.getProductDirectory().getPath();
        String MiuiHomePath = productPath + "/priv-app/MiuiHome";
        String MiLauncherGlobalPath = productPath + "/priv-app/MiLauncherGlobal";
        if (new File(MiuiHomePath).exists() && new File(MiLauncherGlobalPath).exists()) {
            if ("POCO".equals(Build.BRAND)) {
                this.mIgnoreApks.add(MiuiHomePath);
            } else {
                this.mIgnoreApks.add(MiLauncherGlobalPath);
            }
        }
        String MITSMClientNoneNfcPath = productPath + "/app/MITSMClientNoneNfc";
        String MITSMClient = productPath + "/app/MITSMClient";
        boolean isSupportNFC = this.mPms.hasSystemFeature("android.hardware.nfc", 0);
        if (new File(MITSMClientNoneNfcPath).exists() && new File(MITSMClient).exists()) {
            if (isSupportNFC) {
                this.mIgnoreApks.add(MITSMClientNoneNfcPath);
            } else {
                this.mIgnoreApks.add(MITSMClient);
            }
        }
        if (!IS_INTERNATIONAL_BUILD) {
            if (new File("/product/priv-app/GmsCore").exists()) {
                this.mIgnorePackages.add("android.ext.shared");
                this.mIgnorePackages.add("com.android.printservice.recommendation");
            } else {
                this.mIgnorePackages.add("com.google.android.gsf");
            }
        }
        initCotaApps();
    }

    private static void addIgnoreApks(String tag, Set<String> set) {
        String[] whiteList = FeatureParser.getStringArray(tag);
        if (whiteList != null && whiteList.length > 0) {
            for (String str : whiteList) {
                String[] item = TextUtils.split(str, ",");
                if (DeviceLevel.TOTAL_RAM <= Integer.parseInt(item[0])) {
                    set.add(item[1]);
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:11:0x0034. Please report as an issue. */
    private void readIgnoreApks() {
        String custVariant = Build.getCustVariant();
        if (TextUtils.isEmpty(custVariant)) {
            return;
        }
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(new File(APP_LIST_FILE));
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                String appPath = null;
                boolean is_add_apps = false;
                for (int type = parser.getEventType(); 1 != type; type = parser.next()) {
                    switch (type) {
                        case 2:
                            String tagName = parser.getName();
                            if (parser.getAttributeCount() > 0) {
                                appPath = parser.getAttributeValue(0);
                            }
                            if (TAG_ADD_APPS.equals(tagName)) {
                                is_add_apps = true;
                            } else if (TAG_IGNORE_APPS.equals(tagName)) {
                                is_add_apps = false;
                            } else if ("app".equals(tagName)) {
                                String[] ss = parser.nextText().split(" ");
                                boolean is_current_cust = false;
                                int i = 0;
                                while (true) {
                                    if (i < ss.length) {
                                        if (!ss[i].equals(custVariant)) {
                                            i++;
                                        } else {
                                            is_current_cust = true;
                                        }
                                    }
                                }
                                if ((is_add_apps && !is_current_cust) || (!is_add_apps && is_current_cust)) {
                                    this.mIgnoreApks.add(appPath);
                                } else if (is_add_apps && is_current_cust && this.mIgnoreApks.contains(appPath)) {
                                    this.mIgnoreApks.remove(appPath);
                                }
                            }
                        case 3:
                            String end_tag_name = parser.getName();
                            if (TAG_ADD_APPS.equals(end_tag_name)) {
                                is_add_apps = false;
                            } else if (TAG_IGNORE_APPS.equals(end_tag_name)) {
                                is_add_apps = true;
                            }
                        default:
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    boolean shouldIgnoreApp(String packageName, String codePath, String reason) {
        boolean ignored = this.mIgnorePackages.contains(packageName) || this.mIgnoreApks.contains(codePath);
        if (ignored) {
            Slog.i(TAG, "Skip scanning package: " + packageName + ", path=" + codePath + ", reason: " + reason);
            int[] userIds = this.mPms.mUserManager.getUserIds();
            PackageSetting ps = this.mPms.mSettings.getPackageLPr(packageName);
            if (ps != null && ps.getPath() != null && ps.getPath().equals(codePath)) {
                RemovePackageHelper removePackageHelper = new RemovePackageHelper(this.mPms);
                removePackageHelper.removePackageDataLIF(ps, userIds, (PackageRemovedInfo) null, 0, false);
            }
        }
        return ignored;
    }

    void clearNoHistoryFlagIfNeed(List<ResolveInfo> resolveInfos, Intent intent) {
        if (!IS_INTERNATIONAL_BUILD || resolveInfos == null) {
            return;
        }
        for (ResolveInfo resolveInfo : resolveInfos) {
            Bundle bundle = resolveInfo.activityInfo.metaData;
            if (bundle != null) {
                try {
                    if (bundle.getBoolean("mi_use_custom_resolver") && resolveInfo.activityInfo.enabled) {
                        Log.i(TAG, "Removing FLAG_ACTIVITY_NO_HISTORY flag for Intent {" + intent.toShortString(true, true, true, false) + "}");
                        intent.removeFlags(1073741824);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        }
    }

    static boolean isTrustedEnterpriseInstaller(Context context, int callingUid, String installerPkg) {
        return !EnterpriseSettings.ENTERPRISE_ACTIVATED || !ApplicationHelper.isTrustedAppStoresEnabled(context, UserHandle.getUserId(callingUid)) || ArrayUtils.contains(EP_INSTALLER_PKG_WHITELIST, installerPkg) || ApplicationHelper.getTrustedAppStores(context, UserHandle.getUserId(callingUid)).contains(installerPkg);
    }

    boolean checkEnterpriseRestriction(ParsedPackage pkg) {
        if (ApplicationHelperStub.getInstance().isEnterpriseInstallRestriction(pkg.getPackageName(), pkg.getRequestedPermissions(), pkg.getBaseApkPath())) {
            return true;
        }
        if (pkg.getRequestedPermissions().contains("com.miui.enterprise.permission.ACCESS_ENTERPRISE_API") && !EnterpriseVerifier.verify(this.mContext, pkg.getBaseApkPath(), pkg.getPackageName())) {
            Slog.d(TAG, "Verify enterprise signature of package " + pkg.getPackageName() + " failed");
            return true;
        }
        if (!EnterpriseSettings.ENTERPRISE_ACTIVATED || !ApplicationHelper.checkEnterprisePackageRestriction(this.mContext, pkg.getPackageName())) {
            return false;
        }
        Slog.d(TAG, "Installation of package " + pkg.getPackageName() + " is restricted");
        return true;
    }

    PackageInfo hookPkgInfo(PackageInfo origPkgInfo, String packageName, long flags) {
        return HookClient.hookPkgInfo(origPkgInfo, packageName, flags);
    }

    boolean isPackageDeviceAdminEnabled() {
        return IS_INTERNATIONAL_BUILD;
    }

    void canBeDisabled(String packageName, int newState) {
        int callingUid = Binder.getCallingUid();
        if (newState == 0 || newState == 1) {
            return;
        }
        try {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "maintenance_mode_user_id") == 110) {
                return;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (callingUid == 2000) {
            if (SHIM_PKG.equals(packageName)) {
                return;
            }
            PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(packageName);
            if (ps != null && ps.isSystem() && newState == 3) {
                throw new SecurityException("Cannot disable system packages.");
            }
        }
        if (ArrayUtils.contains(MIUI_CORE_APPS, packageName)) {
            throw new SecurityException("Cannot disable miui core packages.");
        }
        if ("co.sitic.pp".equals(packageName) && shouldLockAppRegion()) {
            throw new SecurityException("Cannot disable carrier core packages.");
        }
    }

    static boolean shouldLockAppRegion() {
        String str = CUSTOMIZED_REGION;
        return "lm_cr".equals(str) || "mx_telcel".equals(str);
    }

    boolean isCallerAllowedToSilentlyUninstall(int callingUid) {
        AndroidPackage pkgSetting;
        synchronized (this.mPms.mLock) {
            for (String s : this.mPms.snapshotComputer().getPackagesForUid(callingUid)) {
                if (sSilentlyUninstallPackages.contains(s) && (pkgSetting = (AndroidPackage) this.mPms.mPackages.get(s)) != null && UserHandle.getAppId(callingUid) == pkgSetting.getUid()) {
                    Slog.i(TAG, "Allowed silently uninstall from callinguid:" + callingUid);
                    return true;
                }
            }
            return false;
        }
    }

    boolean isMiuiStubPackage(String packageName) {
        PackageInfo packageInfo;
        Bundle meta;
        PackageSetting pkgSetting = null;
        synchronized (this.mPms.mLock) {
            AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(packageName);
            if (pkg != null) {
                pkgSetting = this.mPms.mSettings.getPackageLPr(packageName);
            }
        }
        return (pkgSetting == null || !pkgSetting.getPkgState().isUpdatedSystemApp() || (packageInfo = this.mPms.snapshotComputer().getPackageInfo(packageName, 2097280L, 0)) == null || packageInfo.applicationInfo == null || (meta = packageInfo.applicationInfo.metaData) == null || !meta.getBoolean("com.miui.stub.install", false)) ? false : true;
    }

    boolean protectAppFromDeleting(final String packageName, final IPackageDeleteObserver2 observer, final int callingUid, final int userId, int deleteFlags) {
        int appId;
        boolean isReject;
        PackageStateInternal ps = this.mPms.snapshotComputer().getPackageStateInternal(packageName);
        if (ps != null && ps.getUserStateOrDefault(userId).isInstalled()) {
            int callingPid = Binder.getCallingPid();
            String msg = "Uninstall pkg: " + packageName + " u" + userId + " flags:" + deleteFlags + " from u" + callingUid + "/" + callingPid + " of " + ProcessUtils.getPackageNameByPid(callingPid);
            PackageManagerServiceUtilsStub.get().logMiuiCriticalInfo(3, msg);
        }
        if (isMiuiStubPackage(packageName) && !MANAGED_PROVISION.equals(this.mPms.snapshotComputer().getNameForUid(callingUid))) {
            boolean deleteSystem = (deleteFlags & 4) != 0;
            int removeUser = (deleteFlags & 2) != 0 ? -1 : userId;
            boolean fullRemove = removeUser == -1 || removeUser == 0;
            if (!deleteSystem || fullRemove) {
                final boolean z = deleteSystem;
                this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerServiceImpl.lambda$protectAppFromDeleting$1(packageName, callingUid, userId, z, observer);
                    }
                });
                return true;
            }
        }
        if (ApplicationHelper.protectedFromDelete(this.mContext, packageName, UserHandle.getUserId(callingUid)) || ApplicationHelperStub.getInstance().isPreventUninstallation(packageName)) {
            Slog.d(TAG, "Device is in enterprise mode, " + packageName + " uninstallation is restricted by enterprise!");
            this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    observer.onPackageDeleted(packageName, -1000, (String) null);
                }
            });
            return true;
        }
        if (ps != null && !ps.isSystem() && PreloadedAppPolicy.isProtectedDataApp(this.mContext, packageName, 0) && (appId = UserHandle.getAppId(callingUid)) != 0 && appId != 1000) {
            Iterator it = PreloadedAppPolicy.getAllowDeleteSourceApps().iterator();
            while (true) {
                if (!it.hasNext()) {
                    isReject = true;
                    break;
                }
                String allowPkg = (String) it.next();
                if (appId == this.mPms.snapshotComputer().getPackageUid(allowPkg, 0L, 0)) {
                    isReject = false;
                    break;
                }
            }
            if (isReject) {
                try {
                    Slog.d(TAG, "MIUILOG- can't uninstall pkg : " + packageName + " callingUid : " + callingUid);
                    if (observer != null && (observer instanceof IPackageDeleteObserver2)) {
                        observer.onPackageDeleted(packageName, -1000, (String) null);
                        return true;
                    }
                    return true;
                } catch (RemoteException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$protectAppFromDeleting$1(String packageName, int callingUid, int userId, boolean deleteSystem, IPackageDeleteObserver2 observer) {
        Slog.d(TAG, "Can't uninstall pkg: " + packageName + " from uid: " + callingUid + " with user: " + userId + " deleteSystem: " + deleteSystem + " reason: shell preinstall");
        try {
            observer.onPackageDeleted(packageName, -1000, (String) null);
        } catch (RemoteException e) {
        }
    }

    public int preCheckUidPermission(String permName, int uid) {
        if (UserHandle.getAppId(uid) != 2000 || !sShellCheckPermissions.contains(permName) || SystemProperties.getBoolean("persist.security.adbinput", false)) {
            return 0;
        }
        if ("android.permission.CALL_PHONE".equals(permName) && 110 == ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId()) {
            Slog.d(TAG, "preCheckUidPermission: permission granted call phone");
            return 0;
        }
        Slog.d(TAG, "preCheckUidPermission: permission\u3000denied, perm=" + permName);
        return -1;
    }

    protected static void initAllowPackageList(Context context) {
        ArrayList<String> arrayList = sAllowPackage;
        arrayList.clear();
        try {
            String[] stringArray = context.getResources().getStringArray(285409291);
            arrayList.addAll(Arrays.asList(stringArray));
            Slog.i(TAG, "add " + stringArray.length + " common packages into sAllowPackage list");
            for (String pkg : stringArray) {
                Slog.i(TAG, pkg);
            }
            if (!IS_INTERNATIONAL_BUILD) {
                return;
            }
            String[] stringArray2 = context.getResources().getStringArray(285409292);
            sAllowPackage.addAll(Arrays.asList(stringArray2));
            Slog.i(TAG, "add " + stringArray2.length + " international package into sAllowPackage list");
            for (String pkg2 : stringArray2) {
                Slog.i(TAG, pkg2);
            }
            String[] stringArray3 = context.getResources().getStringArray(285409293);
            sAllowPackage.addAll(Arrays.asList(stringArray3));
            Slog.i(TAG, "add " + stringArray3.length + " operator packages into sAllowPackage list");
            for (String pkg3 : stringArray3) {
                Slog.i(TAG, pkg3);
            }
            if (!sIsReleaseRom || !sIsBootLoaderLocked) {
                String[] stringArray4 = context.getResources().getStringArray(285409294);
                Slog.i(TAG, "add " + stringArray4.length + " unlocked packages into sAllowPackage list");
                sAllowPackage.addAll(Arrays.asList(stringArray4));
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void fatalIf(boolean condition, int err, String msg, String logPrefix) throws PackageManagerException {
        if (condition) {
            Slog.e(TAG, logPrefix + ", msg=" + msg);
            throw new PackageManagerException(err, msg);
        }
    }

    void assertValidApkAndInstaller(String packageName, SigningDetails signingDetails, int callingUid, String callingPackage, boolean isManuallyAccepted, int sessionId) throws PackageManagerException {
        if (isCTS()) {
            return;
        }
        String log = "MIUILOG- assertCallerAndPackage: uid=" + callingUid + ", installerPkg=" + callingPackage;
        int callingAppId = UserHandle.getAppId(callingUid);
        switch (callingAppId) {
            case 0:
                return;
            case 2000:
                verifyInstallFromShell(this.mContext, sessionId, log);
                return;
            default:
                if (sNoVerifyAllowPackage.contains(callingPackage)) {
                    return;
                }
                fatalIf(!isTrustedEnterpriseInstaller(this.mContext, callingUid, callingPackage) || ApplicationHelperStub.getInstance().isTrustedAppStoresDisable(callingPackage), -22, "FAILED_VERIFICATION_FAILURE ENTERPRISE", log);
                if (!verifyPackageForRelease(this.mContext, packageName, signingDetails, callingUid, callingPackage, log)) {
                    return;
                }
                notifyGlobalPackageInstaller(this.mContext, callingPackage);
                ArrayList<String> arrayList = sAllowPackage;
                if (arrayList.isEmpty()) {
                    initAllowPackageList(this.mContext);
                }
                if (isManuallyAccepted || arrayList.contains(callingPackage) || !sIsReleaseRom) {
                    return;
                }
                fatalIf(true, -115, "Permission denied", log);
                return;
        }
    }

    private static boolean verifyPackageForRelease(Context context, String packageName, SigningDetails signingDetails, int callingUid, String callingPackage, String log) throws PackageManagerException {
        if (sIsReleaseRom && sIsBootLoaderLocked) {
            fatalIf(PACKAGE_WEBVIEW.equals(packageName) && sInstallerSet.contains(callingPackage), -22, "FAILED_VERIFICATION_FAILURE MIUI WEBVIEW", log);
            if (!PreloadedAppPolicy.isProtectedDataApp(packageName) || context.getPackageManager().isPackageAvailable(packageName)) {
                return true;
            }
            String signSha256 = PreloadedAppPolicy.getProtectedDataAppSign(packageName);
            if (TextUtils.isEmpty(signSha256) || signingDetails.hasSha256Certificate(HexEncoding.decode(signSha256.replace(":", "").toLowerCase(), false))) {
                return true;
            }
            fatalIf(true, -22, "FAILED_VERIFICATION_FAILURE SIGNATURE FAIL", log);
        }
        return true;
    }

    private static void verifyInstallFromShell(Context context, int sessionId, String log) throws PackageManagerException {
        int result = -1;
        try {
            if (isSecondUserlocked(context)) {
                result = 2;
            } else {
                result = PmInjector.installVerify(sessionId);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error", e);
        }
        fatalIf(result != 2, -111, PmInjector.statusToString(result), log);
    }

    private static void notifyGlobalPackageInstaller(final Context context, final String callingPackage) {
        if (IS_INTERNATIONAL_BUILD) {
            if ("com.android.vending".equals(callingPackage) || GOOGLE_INSTALLER_PACKAGE.equals(callingPackage)) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerServiceImpl.lambda$notifyGlobalPackageInstaller$3(callingPackage, context);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyGlobalPackageInstaller$3(String callingPackage, Context context) {
        Intent intent = new Intent("com.miui.global.packageinstaller.action.verifypackage");
        intent.putExtra("installing", callingPackage);
        context.sendBroadcast(intent, "com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER");
    }

    private static boolean isSecondUserlocked(Context context) {
        boolean iscts = isCTS();
        int userid = PmInjector.getDefaultUserId();
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (iscts && userid != 0 && !userManager.isUserUnlocked(userid)) {
            return true;
        }
        return false;
    }

    ResolveInfo getMarketResolveInfo(List<ResolveInfo> riList) {
        for (ResolveInfo ri : riList) {
            if (MIUI_MARKET_PACKAGE.equals(ri.activityInfo.packageName) && ri.system) {
                return ri;
            }
        }
        return null;
    }

    ResolveInfo getGoogleWebSearchResolveInfo(List<ResolveInfo> riList) {
        if (riList == null || riList.size() == 0) {
            return null;
        }
        for (ResolveInfo ri : riList) {
            if (ri != null && ri.activityInfo != null && GOOGLE_WEB_SEARCH_PACKAGE.equals(ri.activityInfo.packageName)) {
                return ri;
            }
        }
        return null;
    }

    ResolveInfo hookChooseBestActivity(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, int userId, ResolveInfo defaultValue) {
        ResolveInfo ri;
        String realPkgName;
        if (intent == null) {
            return defaultValue;
        }
        String scheme = intent.getScheme();
        String host = intent.getData() != null ? intent.getData().getHost() : null;
        if (!IS_INTERNATIONAL_BUILD && scheme != null && ((scheme.equals("mimarket") || scheme.equals("market")) && "android.intent.action.VIEW".equals(intent.getAction()) && host != null && (host.equals("details") || host.equals("search")))) {
            ResolveInfo ri2 = getMarketResolveInfo(query);
            if (ri2 != null) {
                return ri2;
            }
        } else {
            if (!IS_INTERNATIONAL_BUILD && PACKAGE_MIME_TYPE.equals(intent.getType()) && "android.intent.action.VIEW".equals(intent.getAction())) {
                if (isCTS()) {
                    String realPkgName2 = this.mCurrentPackageInstaller;
                    if (this.mPms.snapshotComputer().getRenamedPackage(this.mCurrentPackageInstaller) != null) {
                        String realPkgName3 = this.mPms.snapshotComputer().getRenamedPackage(this.mCurrentPackageInstaller);
                        realPkgName = realPkgName3;
                    } else {
                        realPkgName = realPkgName2;
                    }
                } else {
                    realPkgName = MIUI_INSTALLER_PACKAGE;
                }
                intent.setPackage(realPkgName);
                return this.mPM.resolveIntent(intent, resolvedType, flags, userId);
            }
            if (IS_INTERNATIONAL_BUILD && "android.intent.action.WEB_SEARCH".equals(intent.getAction()) && isRsa4() && (ri = getGoogleWebSearchResolveInfo(query)) != null) {
                return ri;
            }
        }
        return defaultValue;
    }

    private boolean isRsa4() {
        if (TextUtils.isEmpty(this.mRsaFeature)) {
            this.mRsaFeature = SystemProperties.get("ro.com.miui.rsa.feature", "");
        }
        return !TextUtils.isEmpty(this.mRsaFeature);
    }

    private boolean updateDefaultPkgInstallerLocked() {
        if (!IS_INTERNATIONAL_BUILD) {
            Log.i(TAG, "updateDefaultPkgInstallerLocked");
            PackageSetting miuiInstaller = this.mMiuiInstallerPackageSetting;
            if (miuiInstaller == null) {
                miuiInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(MIUI_INSTALLER_PACKAGE);
            }
            if (miuiInstaller != null) {
                Log.i(TAG, "found miui installer");
            }
            PackageSetting googleInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(GOOGLE_INSTALLER_PACKAGE);
            if (googleInstaller != null) {
                Log.i(TAG, "found google installer");
            }
            PackageSetting androidInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(ANDROID_INSTALLER_PACKAGE);
            if (androidInstaller != null) {
                Log.i(TAG, "found android installer");
            }
            boolean isUseGooglePackageInstaller = isCTS() || miuiInstaller == null || !miuiInstaller.isSystem();
            if (!isUseGooglePackageInstaller) {
                if (!MIUI_INSTALLER_PACKAGE.equals(this.mCurrentPackageInstaller)) {
                    PackageSetting packageSetting = this.mMiuiInstallerPackageSetting;
                    if (packageSetting != null) {
                        packageSetting.setInstalled(true, 0);
                        this.mPkgSettings.mPackages.put(MIUI_INSTALLER_PACKAGE, this.mMiuiInstallerPackageSetting);
                        if ((this.mMiuiInstallerPackageSetting.getFlags() & 128) != 0) {
                            this.mPkgSettings.disableSystemPackageLPw(MIUI_INSTALLER_PACKAGE, true);
                        }
                    }
                    if (this.mMiuiInstallerPackage != null) {
                        this.mPms.mPackages.put(MIUI_INSTALLER_PACKAGE, this.mMiuiInstallerPackage);
                    }
                    this.mCurrentPackageInstaller = MIUI_INSTALLER_PACKAGE;
                    if (googleInstaller != null) {
                        googleInstaller.setInstalled(false, 0);
                    }
                    if (androidInstaller != null) {
                        androidInstaller.setInstalled(false, 0);
                    }
                    return true;
                }
            } else if (!GOOGLE_INSTALLER_PACKAGE.equals(this.mCurrentPackageInstaller) && !ANDROID_INSTALLER_PACKAGE.equals(this.mCurrentPackageInstaller)) {
                if (miuiInstaller != null) {
                    miuiInstaller.setInstalled(false, 0);
                }
                this.mMiuiInstallerPackageSetting = (PackageSetting) this.mPkgSettings.mPackages.get(MIUI_INSTALLER_PACKAGE);
                this.mMiuiInstallerPackage = (AndroidPackage) this.mPms.mPackages.get(MIUI_INSTALLER_PACKAGE);
                this.mPkgSettings.mPackages.remove(MIUI_INSTALLER_PACKAGE);
                if (this.mPkgSettings.isDisabledSystemPackageLPr(MIUI_INSTALLER_PACKAGE)) {
                    this.mPkgSettings.removeDisabledSystemPackageLPw(MIUI_INSTALLER_PACKAGE);
                }
                this.mPms.mPackages.remove(MIUI_INSTALLER_PACKAGE);
                if (googleInstaller != null) {
                    googleInstaller.setInstalled(true, 0);
                    this.mCurrentPackageInstaller = GOOGLE_INSTALLER_PACKAGE;
                } else if (androidInstaller != null) {
                    androidInstaller.setInstalled(true, 0);
                    this.mCurrentPackageInstaller = ANDROID_INSTALLER_PACKAGE;
                }
                return true;
            }
            Log.i(TAG, "set default package install as" + this.mCurrentPackageInstaller);
        }
        return false;
    }

    private boolean isAllowedToGetInstalledApps(int callingUid, String callingPackage, String where) {
        if (IS_INTERNATIONAL_BUILD) {
            return true;
        }
        int callingAppId = UserHandle.getAppId(callingUid);
        if (callingAppId < 10000 || TextUtils.isEmpty(callingPackage)) {
            return true;
        }
        if (Build.IS_DEBUGGABLE && PKMS_ATEST_NAME.equals(callingPackage)) {
            return true;
        }
        AppOpsManager appOpsManager = (AppOpsManager) ActivityThread.currentApplication().getSystemService(AppOpsManager.class);
        if (appOpsManager.noteOpNoThrow(10022, callingUid, callingPackage, (String) null, (String) null) == 0) {
            return true;
        }
        Slog.e(TAG, "MIUILOG- Permission Denied " + where + ". pkg : " + callingPackage + " uid : " + callingUid);
        return false;
    }

    public void switchPackageInstaller() {
        String ctsInstallerPackageName;
        try {
            if (isCTS()) {
                PackageSetting googleInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(GOOGLE_INSTALLER_PACKAGE);
                PackageSetting androidInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(ANDROID_INSTALLER_PACKAGE);
                if (androidInstaller != null) {
                    ctsInstallerPackageName = ANDROID_INSTALLER_PACKAGE;
                } else if (googleInstaller == null) {
                    ctsInstallerPackageName = null;
                } else {
                    ctsInstallerPackageName = GOOGLE_INSTALLER_PACKAGE;
                }
                if (ctsInstallerPackageName != null) {
                    this.mPM.installExistingPackageAsUser(ctsInstallerPackageName, 0, 16384, 4, (List) null);
                }
            }
            synchronized (this.mPms.mLock) {
                if (updateDefaultPkgInstallerLocked()) {
                    this.mPms.mRequiredInstallerPackage = this.mCurrentPackageInstaller;
                    this.mPms.mRequiredUninstallerPackage = this.mCurrentPackageInstaller;
                    AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(this.mPms.mRequiredInstallerPackage);
                    PermissionManagerService pms = ServiceManager.getService("permissionmgr");
                    PermissionManagerServiceImpl impl = (PermissionManagerServiceImpl) ReflectionUtils.getObjectField(pms, "mPermissionManagerServiceImpl", PermissionManagerServiceImpl.class);
                    ReflectionUtils.callMethod(impl, "updatePermissions", Void.class, new Object[]{this.mCurrentPackageInstaller, pkg});
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void checkGTSSpecAppOptMode() {
        String[] pkgs;
        if (IS_INTERNATIONAL_BUILD) {
            pkgs = new String[]{"com.miui.screenrecorder"};
        } else {
            pkgs = new String[]{"com.miui.cleanmaster", "com.xiaomi.drivemode", "com.xiaomi.aiasst.service", "com.miui.thirdappassistant", "com.miui.screenrecorder"};
        }
        synchronized (this.mPms.mLock) {
            Settings mPkgSettings = this.mPms.mSettings;
            boolean isCtsBuild = isCTS();
            for (String pkg : pkgs) {
                PackageSetting uninstallPkg = (PackageSetting) mPkgSettings.mPackages.get(pkg);
                if ("com.miui.cleanmaster".equals(pkg) && uninstallPkg == null) {
                    checkAndClearResiduePermissions(mPkgSettings.mPermissions, pkg, "com.miui.cleanmaster.permission.Clean_Master");
                }
                if ("com.miui.screenrecorder".equals(pkg) && uninstallPkg == null) {
                    checkAndClearResiduePermissions(mPkgSettings.mPermissions, pkg, "com.miui.screenrecorder.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
                }
                if (isCtsBuild && uninstallPkg != null && !uninstallPkg.isSystem()) {
                    uninstallPkg.setInstalled(false, 0);
                    mPkgSettings.mPackages.remove(pkg);
                    if (mPkgSettings.isDisabledSystemPackageLPr(pkg)) {
                        mPkgSettings.removeDisabledSystemPackageLPw(pkg);
                    }
                    this.mPms.mPackages.remove(pkg);
                    if ("com.miui.cleanmaster".equals(pkg) || "com.miui.thirdappassistant".equals(pkg)) {
                        clearPermissions(pkg);
                    }
                }
            }
        }
    }

    private static void clearPermissions(String packageName) {
        if (!sClearPermissionSet.contains(packageName)) {
            return;
        }
        try {
            PermissionManagerService pms = ServiceManager.getService("permissionmgr");
            PermissionManagerServiceImpl impl = (PermissionManagerServiceImpl) ReflectionUtils.getObjectField(pms, "mPermissionManagerServiceImpl", PermissionManagerServiceImpl.class);
            ReflectionUtils.callMethod(impl, "updatePermissions", Void.class, new Object[]{packageName, null});
            Slog.i(TAG, "clear residue permission finish");
        } catch (Exception e) {
            Slog.e(TAG, "clear residue permission error" + e.getLocalizedMessage());
        }
    }

    private static void checkAndClearResiduePermissions(LegacyPermissionSettings settings, String packageName, String perName) {
        if (settings == null || TextUtils.isEmpty(packageName) || TextUtils.isEmpty(perName)) {
            return;
        }
        List<LegacyPermission> permissions = settings.getPermissions();
        if (permissions.size() > 0) {
            Slog.i(TAG, "find residue permission");
            clearPermissions(packageName);
        }
    }

    public static boolean isCTS() {
        return AppOpsUtils.isXOptMode();
    }

    void setCallingPackage(PackageInstallerSession session, String callingPackageName) {
        if (!TextUtils.isEmpty(callingPackageName)) {
            session.setCallingPackage(callingPackageName);
        } else {
            String realPkg = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
            session.setCallingPackage(TextUtils.isEmpty(realPkg) ? session.getInstallerPackageName() : realPkg);
        }
    }

    public static Handler getFirstUseHandler() {
        if (sFirstUseHandler == null) {
            synchronized (sFirstUseLock) {
                if (sFirstUseHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("first_use_thread");
                    sFirstUseThread = handlerThread;
                    handlerThread.start();
                    sFirstUseHandler = new Handler(sFirstUseThread.getLooper());
                }
            }
        }
        return sFirstUseHandler;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFirstUseActivity(String packageName) {
        boolean isDeviceProvisioned = true;
        if (this.mContext != null) {
            try {
                isDeviceProvisioned = isProvisioned();
            } catch (IllegalStateException e) {
                isDeviceProvisioned = false;
            }
        }
        if (!isDeviceProvisioned) {
            Slog.w(TAG, "Skip dexopt, device is not provisioned.");
            return;
        }
        if ("android".equals(packageName)) {
            Slog.w(TAG, "Skip dexopt, cannot dexopt the system server.");
            return;
        }
        boolean success = this.mDexOptHelper.performDexOpt(new DexoptOptions(packageName, 14, WifiDrvUEventObserver.MTK_DATASTALL_PM_CHANGE_FAIL));
        Slog.d(TAG, "FirstUseActivity packageName = " + packageName + " success = " + success);
        PinnerService pinnerService = (PinnerService) LocalServices.getService(PinnerService.class);
        if (pinnerService != null) {
            Log.i(TAG, "FirstUseActivity Pinning optimized code " + packageName);
            ArraySet<String> packages = new ArraySet<>();
            packages.add(packageName);
            pinnerService.update(packages, false);
        }
    }

    public void processFirstUseActivity(final String packageName) {
        Runnable task = new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                PackageManagerServiceImpl.this.handleFirstUseActivity(packageName);
            }
        };
        getFirstUseHandler().postDelayed(task, 9000L);
    }

    public void dumpSingleDexoptState(IndentingPrintWriter pw, PackageStateInternal pkgSetting, String packageName) {
        try {
            AndroidPackageInternal pkg = pkgSetting.getPkg();
            if (pkg == null) {
                pw.println("Unable to find AndroidPackage: " + packageName);
                return;
            }
            pw.increaseIndent();
            pw.println("base APK odex file size: " + PackageParserStub.get().getDexFileSize(pkg.getBaseApkPath()));
            pw.decreaseIndent();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to dumpSingleDexoptState", e);
        }
    }

    public void performDexOptAsyncTask(DexoptOptions options) {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            dexoptServiceThread.performDexOptAsyncTask(options);
        }
    }

    public int getDexOptResult() {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            return dexoptServiceThread.getDexOptResult();
        }
        return 0;
    }

    public void performDexOptSecondary(ApplicationInfo info, String path, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            dexoptServiceThread.performDexOptSecondary(info, path, dexUseInfo, options);
        }
    }

    public int getDexoptSecondaryResult() {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            return dexoptServiceThread.getDexoptSecondaryResult();
        }
        return 0;
    }

    private void disableSystemApp(int userId, String pkg) {
        PackageManagerServiceUtils.logCriticalInfo(4, "Disable " + pkg + " for user " + userId);
        try {
            this.mPM.setApplicationEnabledSetting(pkg, 3, 0, userId, "COTA");
            synchronized (this.mPms.mLock) {
                PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
                if (pkgSetting != null) {
                    updatedefaultState(pkgSetting, "cota-disabled", userId);
                    this.mPms.scheduleWritePackageRestrictions(userId);
                }
            }
        } catch (Exception e) {
            PackageManagerServiceUtils.logCriticalInfo(6, "Failed to disable " + pkg + ", msg=" + e.getMessage());
        }
    }

    private void enableSystemApp(int userId, String pkg) {
        PackageManagerServiceUtils.logCriticalInfo(4, "Enable " + pkg + " for user " + userId);
        try {
            this.mPM.setApplicationEnabledSetting(pkg, 1, 0, userId, "COTA");
            synchronized (this.mPms.mLock) {
                PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
                if (pkgSetting != null) {
                    updatedefaultState(pkgSetting, null, userId);
                    this.mPms.scheduleWritePackageRestrictions(userId);
                }
            }
        } catch (Exception e) {
            PackageManagerServiceUtils.logCriticalInfo(6, "Failed to enable " + pkg + ", msg=" + e.getMessage());
        }
    }

    private void updateSystemAppState(int userId, boolean isCTS, String pkg) {
        synchronized (this.mPms.mLock) {
            PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
            if (pkgSetting != null && pkgSetting.isSystem()) {
                boolean alreadyDisabled = true;
                boolean updatedSystemApp = (pkgSetting.getFlags() & 128) != 0 && pkgSetting.getInstalled(userId);
                boolean untouchedYet = getdatedefaultState(pkgSetting, userId) == null;
                int state = pkgSetting.getEnabled(userId);
                if (state != 2 && state != 3) {
                    alreadyDisabled = false;
                }
                boolean wasDisabledByUs = "COTA".equals(pkgSetting.readUserState(userId).getLastDisableAppCaller());
                if (updatedSystemApp) {
                    return;
                }
                if (isCTS) {
                    if (!alreadyDisabled || !wasDisabledByUs) {
                        return;
                    }
                    enableSystemApp(userId, pkg);
                    return;
                }
                if (!alreadyDisabled && untouchedYet) {
                    disableSystemApp(userId, pkg);
                }
            }
        }
    }

    private String getdatedefaultState(PackageSetting ps, int userId) {
        return ps.readUserState(userId).getDefaultState();
    }

    private void updatedefaultState(PackageSetting ps, String value, int userId) {
        ps.modifyUserState(userId).setDefaultState(value);
    }

    void updateSystemAppDefaultStateForUser(int userId) {
        boolean isCTS = isCTS();
        ArrayMap<String, Boolean> defaultPkgState = SystemConfig.getInstance().getPackageDefaultState();
        if (defaultPkgState.isEmpty()) {
            return;
        }
        int size = defaultPkgState.size();
        for (int i = 0; i < size; i++) {
            String pkg = defaultPkgState.keyAt(i);
            boolean disableByDefault = !defaultPkgState.getOrDefault(pkg, false).booleanValue();
            if (disableByDefault) {
                updateSystemAppState(userId, isCTS, pkg);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doUpdateSystemAppDefaultUserStateForAllUsers() {
        UserManagerInternal mUserManager = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        int[] allUsers = mUserManager.getUserIds();
        for (int userId : allUsers) {
            updateSystemAppDefaultStateForUser(userId);
        }
    }

    void updateSystemAppDefaultStateForAllUsers() {
        ArrayMap<String, Boolean> defaultPkgState = SystemConfig.getInstance().getPackageDefaultState();
        if (defaultPkgState.isEmpty()) {
            return;
        }
        doUpdateSystemAppDefaultUserStateForAllUsers();
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(DisplayModeDirectorImpl.MIUI_OPTIMIZATION), false, new ContentObserver(this.mPms.mHandler) { // from class: com.android.server.pm.PackageManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                PackageManagerServiceImpl.this.doUpdateSystemAppDefaultUserStateForAllUsers();
            }
        }, -1);
    }

    void removePackageFromSharedUser(PackageSetting ps) {
        SharedUserSetting sharedUserSetting = this.mPms.mSettings.getSharedUserSettingLPr(ps);
        if (sharedUserSetting != null) {
            sharedUserSetting.removePackage(ps);
        }
    }

    public PackageManagerService getService() {
        return this.mPms;
    }

    List<ApplicationInfo> getPersistentAppsForOtherUser(final boolean safeMode, final int flags, final int userId) {
        final ArrayList<ApplicationInfo> finalList = new ArrayList<>();
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService != null) {
            packageManagerService.forEachPackageState(packageManagerService.snapshotComputer(), new Consumer() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PackageManagerServiceImpl.lambda$getPersistentAppsForOtherUser$4(safeMode, flags, userId, finalList, (PackageStateInternal) obj);
                }
            });
        }
        return finalList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getPersistentAppsForOtherUser$4(boolean safeMode, int flags, int userId, ArrayList finalList, PackageStateInternal packageState) {
        if (packageState.getPkg().isPersistent()) {
            if (!safeMode || packageState.isSystem()) {
                ApplicationInfo ai = PackageInfoUtils.generateApplicationInfo(packageState.getAndroidPackage(), flags, packageState.getUserStateOrDefault(userId), userId, packageState);
                switch (userId) {
                    case 110:
                        addPersistentPackages(ai, finalList);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static void addPersistentPackages(ApplicationInfo ai, ArrayList<ApplicationInfo> finalList) {
        char c;
        if (ai == null) {
            Slog.w(TAG, "ai is null!");
            return;
        }
        if (ai.processName == null) {
            Slog.w(TAG, "processName is null!");
            return;
        }
        String str = ai.processName;
        switch (str.hashCode()) {
            case -1977039313:
                if (str.equals("com.goodix.fingerprint")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -966434795:
                if (str.equals("com.miui.daemon")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -695600961:
                if (str.equals("com.android.nfc")) {
                    c = 0;
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
            case 1:
            case 2:
                finalList.add(ai);
                return;
            default:
                return;
        }
    }

    public boolean exemptApplink(Intent intent, List<ResolveInfo> candidates, List<ResolveInfo> result) {
        if (intent.isWebIntent() && intent.getData() != null && "digitalkeypairing.org".equals(intent.getData().getHost())) {
            int size = candidates.size();
            int i = 0;
            while (true) {
                if (i < size) {
                    ResolveInfo resolveInfo = candidates.get(i);
                    if (resolveInfo == null || resolveInfo.activityInfo == null || !"com.miui.tsmclient".equals(resolveInfo.activityInfo.packageName)) {
                        i++;
                    } else {
                        result.add(resolveInfo);
                        break;
                    }
                } else {
                    break;
                }
            }
            int i2 = result.size();
            if (i2 > 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void asyncDexMetadataDexopt(AndroidPackage pkg, int[] userId) {
        this.mMiuiDexopt.asyncDexMetadataDexopt(pkg, userId);
    }

    public boolean setBaselineDisabled(String packageName, boolean disabled) {
        return this.mMiuiDexopt.setBaselineDisabled(packageName, disabled);
    }

    public boolean isBaselineDisabled(String packageName) {
        return this.mMiuiDexopt.isBaselineDisabled(packageName);
    }

    public List<PackageInfo> getPackageInfoBySelf(int uid, int pid, final long flags, final int userId) {
        final AndroidPackage pkg = this.mPms.snapshotComputer().getPackage(uid);
        if (pkg == null) {
            return null;
        }
        boolean allowed = isAllowedToGetInstalledApps(uid, pkg.getPackageName(), "getInstalledPackages");
        if (allowed) {
            return null;
        }
        return (List) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda4
            public final Object getOrThrow() {
                List lambda$getPackageInfoBySelf$5;
                lambda$getPackageInfoBySelf$5 = PackageManagerServiceImpl.this.lambda$getPackageInfoBySelf$5(pkg, flags, userId);
                return lambda$getPackageInfoBySelf$5;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ List lambda$getPackageInfoBySelf$5(AndroidPackage pkg, long flags, int userId) throws Exception {
        PackageInfo packageInfo = this.mPms.snapshotComputer().getPackageInfo(pkg.getPackageName(), flags, userId);
        if (packageInfo != null) {
            ArrayList<PackageInfo> list = new ArrayList<>();
            list.add(packageInfo);
            return list;
        }
        return null;
    }

    public List<ApplicationInfo> getApplicationInfoBySelf(int uid, int pid, final long flags, final int userId) {
        final AndroidPackage pkg = this.mPms.snapshotComputer().getPackage(uid);
        if (pkg == null) {
            return null;
        }
        boolean allowed = isAllowedToGetInstalledApps(uid, pkg.getPackageName(), "getInstalledApplications");
        if (allowed) {
            return null;
        }
        return (List) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda1
            public final Object getOrThrow() {
                List lambda$getApplicationInfoBySelf$6;
                lambda$getApplicationInfoBySelf$6 = PackageManagerServiceImpl.this.lambda$getApplicationInfoBySelf$6(pkg, flags, userId);
                return lambda$getApplicationInfoBySelf$6;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ List lambda$getApplicationInfoBySelf$6(AndroidPackage pkg, long flags, int userId) throws Exception {
        ApplicationInfo applicationInfo = this.mPms.snapshotComputer().getApplicationInfo(pkg.getPackageName(), flags, userId);
        if (applicationInfo != null) {
            ArrayList<ApplicationInfo> list = new ArrayList<>();
            list.add(applicationInfo);
            return list;
        }
        return null;
    }

    public void recordPackageUpdate(int[] userIds, String pkgName, String installer, Bundle extras) {
        ((PackageEventRecorderInternal) LocalServices.getService(PackageEventRecorderInternal.class)).recordPackageUpdate(userIds, pkgName, installer, extras);
    }

    public void recordPackageRemove(int[] userIds, String pkgName, String installer, boolean isRemovedFully, Bundle extras) {
        ((PackageEventRecorderInternal) LocalServices.getService(PackageEventRecorderInternal.class)).recordPackageRemove(userIds, pkgName, installer, isRemovedFully, extras);
    }

    public void recordPackageActivate(final String activatedPkg, final int userId, final String sourcePkg) {
        if (!PackageEventRecorder.shouldRecordPackageActivate(activatedPkg, sourcePkg, userId, this.mPms.snapshotComputer().getPackageStateInternal(activatedPkg))) {
            return;
        }
        this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ((PackageEventRecorderInternal) LocalServices.getService(PackageEventRecorderInternal.class)).recordPackageActivate(activatedPkg, userId, sourcePkg);
            }
        });
    }

    public Bundle getPackageActivatedBundle(String activatedPackage, int userId, boolean clear) {
        return ((PackageEventRecorderInternal) LocalServices.getService(PackageEventRecorderInternal.class)).getSourcePackage(activatedPackage, userId, clear);
    }

    public boolean checkReplaceSetting(int appId, SettingBase setting) {
        if (appId == 1000 && (setting instanceof PackageSetting)) {
            PackageSetting ps = (PackageSetting) setting;
            PackageManagerService.reportSettingsProblem(6, "Package " + ps.getPackageName() + " with user id " + appId + " cannot replace SYSTEM_UID");
            return true;
        }
        return false;
    }

    public void setDomainVerificationService(DomainVerificationService service) {
        this.mDomainVerificationService = service;
    }

    public boolean hasDomainVerificationRestriction() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return false;
        }
        boolean isDeviceProvisioned = isProvisioned();
        return !isDeviceProvisioned;
    }

    public boolean useMiuiDefaultCrossProfileIntentFilter() {
        return !(hasGoogleContacts() && hasXiaomiContacts()) && hasXiaomiContacts();
    }

    private boolean hasXiaomiContacts() {
        return new File("/product/priv-app/MIUIContactsT").exists() || new File("/product/priv-app/MIUIContactsFold").exists() || new File("/product/priv-app/MIUIContactsCetus").exists() || new File("/product/priv-app/MIUIContactsTGlobal").exists() || new File("/product/priv-app/MIUIContactsPadU").exists() || new File("/product/priv-app/MIUIContactsFold").exists() || new File("/product/priv-app/MIUIContactsUGlobal").exists() || new File("/product/priv-app/MIUIContactsU").exists();
    }

    private boolean hasGoogleContacts() {
        return new File("/product/app/GoogleContacts").exists();
    }

    public boolean shouldSkipInstallForNewUser(String pkgName, int userId) {
        if (userId == 0) {
            return false;
        }
        return shouldSkipInstall(pkgName) || shouldSkipInstallCotaApp(pkgName);
    }

    public boolean shouldSkipInstallForUserType(String pkgName, String userType) {
        if (userType == "android.os.usertype.full.SYSTEM") {
            return false;
        }
        return shouldSkipInstall(pkgName) || shouldSkipInstallCotaApp(pkgName);
    }

    private boolean shouldSkipInstall(String pkgName) {
        if (!IS_GLOBAL_REGION_BUILD || !this.mIsGlobalCrbtSupported || Build.VERSION.DEVICE_INITIAL_SDK_INT < 33) {
            return false;
        }
        boolean isTHPhoneApp = sTHPhoneAppsSet.contains(pkgName);
        boolean isGLPhoneApp = sGLPhoneAppsSet.contains(pkgName);
        if (!isGLPhoneApp && !isTHPhoneApp) {
            return false;
        }
        String region = SystemProperties.get("ro.miui.region");
        if (TextUtils.isEmpty(region)) {
            return false;
        }
        return region.equals("TH") ? isGLPhoneApp : isTHPhoneApp;
    }

    private boolean shouldSkipInstallCotaApp(String pkgName) {
        if (!SUPPORT_DEL_COTA_APP) {
            return false;
        }
        String cotaCarrier = SystemProperties.get("persist.sys.cota.carrier", "").toLowerCase();
        return (!this.mCotaApps.containsKey(pkgName) || TextUtils.isEmpty(cotaCarrier) || cotaCarrier.equals(this.mCotaApps.get(pkgName))) ? false : true;
    }

    public void addPermierFlagIfNeedForGlobalROM(ArrayMap<String, FeatureInfo> availableFeatures) {
        if (availableFeatures != null && availableFeatures.get("com.google.android.feature.PREMIER_TIER") == null && "tier1".equals(SystemProperties.get("ro.com.miui.rsa", "")) && "global".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region", "")) && "".equals(SystemProperties.get("ro.miui.customized.region", ""))) {
            FeatureInfo fi = new FeatureInfo();
            fi.name = "com.google.android.feature.PREMIER_TIER";
            availableFeatures.put("com.google.android.feature.PREMIER_TIER", fi);
        }
    }

    public void canBeUpdate(String packageName) throws PrepareFailure {
        boolean isNotSupportUpdate;
        if (this.mPackageManagerCloudHelper.getCloudNotSupportUpdateSystemApps().isEmpty()) {
            isNotSupportUpdate = sNotSupportUpdateSystemApps.contains(packageName);
        } else {
            isNotSupportUpdate = this.mPackageManagerCloudHelper.getCloudNotSupportUpdateSystemApps().contains(packageName);
        }
        if (isNotSupportUpdate) {
            throw new PrepareFailure(-2, "Package " + packageName + " are not updateable.");
        }
    }

    public boolean needSkipDomainVerifier(String packageName) {
        if (!IS_INTERNATIONAL_BUILD && this.mPms.isFirstBoot() && "com.google.android.gms".equals(packageName)) {
            return true;
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:6:0x001f. Please report as an issue. */
    private void initCotaApps() {
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(CARRIER_SYS_APPS_LIST);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                for (int event = parser.getEventType(); event != 1; event = parser.next()) {
                    switch (event) {
                        case 0:
                        case 1:
                        default:
                        case 2:
                            String name = parser.getName();
                            if ("package".equals(name)) {
                                String pkgName = parser.getAttributeValue(null, "name");
                                String carrier = parser.getAttributeValue(null, "carrier");
                                if (TextUtils.isEmpty(pkgName)) {
                                    Slog.e(TAG, "initCotaApps pkgName is null, skip parse this tag");
                                } else if (TextUtils.isEmpty(carrier)) {
                                    Slog.e(TAG, "initCotaApps carrier is null, skip parse this tag");
                                } else {
                                    this.mCotaApps.put(pkgName, carrier.toLowerCase());
                                }
                            }
                        case 3:
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "initCotaApps fail: " + e.getMessage());
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    public String getSupportAonServicePackageName() {
        if (!this.mIsSupportAttention.contains(miui.os.Build.DEVICE)) {
            return "";
        }
        PackageManagerService packageManagerService = this.mPms;
        String supportAonServicePackageName = packageManagerService.ensureSystemPackageName(packageManagerService.snapshotComputer(), this.mPms.getPackageFromComponentString(R.string.config_wifi_tether_enable));
        return supportAonServicePackageName;
    }
}
