package com.android.server;

import android.appcompat.ApplicationCompatUtilsStub;
import android.content.Context;
import android.hovermode.MiuiHoverModeManager;
import android.magicpointer.MiuiMagicPointerStub;
import android.os.FileUtils;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Log;
import android.util.MiuiAppSizeCompatModeImpl;
import android.util.Slog;
import com.android.server.am.MimdManagerServiceStub;
import com.android.server.am.PeriodicCleanerInternalStub;
import com.android.server.lights.LightsService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerServiceStub;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.tof.ContactlessGestureService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.AppContinuityRouterStub;
import com.android.server.wm.ApplicationCompatRouterStub;
import com.android.server.wm.MiuiEmbeddingWindowServiceStubHead;
import com.miui.autoui.MiuiAutoUIManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.car.MiuiCarServiceStub;
import com.miui.whetstone.server.WhetstoneActivityManagerService;
import com.xiaomi.abtest.d.d;
import com.xiaomi.interconnection.InterconnectionService;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.TreeSet;
import miui.enterprise.EnterpriseManagerStub;
import miui.hardware.shoulderkey.ShoulderKeyManager;
import miui.mqsas.sdk.BootEventManager;
import miui.os.Build;
import miui.util.FeatureParser;

@MiuiStubHead(manifestName = "com.android.server.MiuiStubImplManifest$$")
/* loaded from: classes.dex */
public class SystemServerImpl extends SystemServerStub {
    private static final String AML_MIUI_WIFI_SERVICE = "AmlMiuiWifiService";
    private static final String AML_MIUI_WIFI_SERVICE_CLASS = "com.android.server.wifi.AmlMiuiWifiService";
    private static final String AML_SLAVE_WIFI_SERVICE = "AmlSlaveWifiService";
    private static final String AML_SLAVE_WIFI_SERVICE_CLASS = "com.android.server.wifi.AmlSlaveWifiService";
    private static final boolean DEBUG = true;
    private static final File DEFAULT_HEAP_DUMP_FILE = new File("/data/system/heapdump/");
    private static final int MAX_HEAP_DUMPS = 2;
    private static final String MIUI_HEAP_DUMP_PATH = "/data/miuilog/stability/resleak/fdtrack";
    private static final String MIUI_SLAVE_WIFI_SERVICE = "SlaveWifiService";
    private static final String MIUI_WIFI_SERVICE = "MiuiWifiService";
    private static final String TAG = "SystemServerI";
    private static final String WIFI_APEX_SERVICE_JAR_PATH = "/apex/com.android.wifi/javalib/service-wifi.jar";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SystemServerImpl> {

        /* compiled from: SystemServerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SystemServerImpl INSTANCE = new SystemServerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SystemServerImpl m276provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SystemServerImpl m275provideNewInstance() {
            return new SystemServerImpl();
        }
    }

    static {
        PathClassLoader pathClassLoader = (PathClassLoader) SystemServerImpl.class.getClassLoader();
        pathClassLoader.addDexPath("/system_ext/framework/miuix.jar");
        pathClassLoader.addDexPath("/system_ext/framework/miui-cameraopt.jar");
        try {
            Log.i(TAG, "Load libmiui_service");
            System.loadLibrary("miui_service");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "can't loadLibrary libmiui_service", e);
        }
    }

    public SystemServerImpl() {
        PackageManagerServiceStub.get();
        if ("file".equals(SystemProperties.get("ro.crypto.type")) || "trigger_restart_framework".equals(SystemProperties.get("vold.decrypt"))) {
            enforceVersionPolicy();
        }
    }

    PhoneWindowManager createPhoneWindowManager() {
        PhoneWindowManager phoneWindowManager = new PhoneWindowManager();
        try {
            PhoneWindowManager phoneWindowManager2 = (PhoneWindowManager) Class.forName("com.android.server.policy.MiuiPhoneWindowManager").newInstance();
            return phoneWindowManager2;
        } catch (Exception e) {
            e.printStackTrace();
            return phoneWindowManager;
        }
    }

    Class createLightsServices() {
        try {
            Class<?> clazz = Class.forName("com.android.server.lights.MiuiLightsService");
            return clazz;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to find MiuiLightsService", e);
            return LightsService.class;
        }
    }

    private static void startService(String className) {
        SystemServiceManager manager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
        try {
            manager.startService(className);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to start " + className, e);
        }
    }

    final void addExtraServices(Context context, boolean onlyCore) {
        if (ShoulderKeyManager.SUPPORT_SHOULDERKEY || ShoulderKeyManager.SUPPORT_MIGAMEMACRO) {
            startService("com.android.server.input.shoulderkey.ShoulderKeyManagerService$Lifecycle");
        }
        AppContinuityRouterStub.get().initContinuityManagerService();
        startService("com.miui.server.SecurityManagerService$Lifecycle");
        startService("com.miui.server.MiuiWebViewManagerService$Lifecycle");
        startService("com.miui.server.MiuiInitServer$Lifecycle");
        startService("com.miui.server.BackupManagerService$Lifecycle");
        startService("com.miui.server.stepcounter.StepCounterManagerService");
        startService("com.android.server.location.LocationPolicyManagerService$Lifecycle");
        startService("com.miui.server.PerfShielderService$Lifecycle");
        startService("com.miui.server.greeze.GreezeManagerService$Lifecycle");
        if (FeatureParser.getBoolean(ContactlessGestureService.FEATURE_TOF_PROXIMITY_SUPPORT, false) || FeatureParser.getBoolean(ContactlessGestureService.FEATURE_TOF_GESTURE_SUPPORT, false) || FeatureParser.getBoolean("config_aon_gesture_available", false)) {
            startService("com.android.server.tof.ContactlessGestureService");
        }
        MiuiCarServiceStub.get().publishCarService();
        startService("com.miui.server.turbosched.TurboSchedManagerService$Lifecycle");
        startService("com.android.server.am.ProcessManagerService$Lifecycle");
        startService("com.miui.server.rtboost.SchedBoostService$Lifecycle");
        startService("com.miui.server.MiuiCldService$Lifecycle");
        startService("com.miui.server.MiuiFboService$Lifecycle");
        startService("com.miui.server.stability.StabilityLocalService$Lifecycle");
        if (SystemProperties.getBoolean("persist.sys.stability.swapEnable", false)) {
            startService("com.miui.server.MiuiSwapService$Lifecycle");
        }
        startService("com.android.server.am.MiuiMemoryService$Lifecycle");
        startService("com.miui.server.MiuiDfcService$Lifecycle");
        startService("com.miui.server.sentinel.MiuiSentinelService$Lifecycle");
        ServiceManager.addService("whetstone.activity", new WhetstoneActivityManagerService(context));
        if (MiuiAppSizeCompatModeImpl.getInstance().isEnabled()) {
            startService("com.android.server.wm.MiuiSizeCompatService$Lifecycle");
        }
        if (MiuiAutoUIManager.IS_AUTO_UI_ENABLED) {
            startService("com.android.server.autoui.MiuiAutoUIManagerService");
        }
        startService("com.android.server.input.MiuiInputManagerService$Lifecycle");
        if (MiuiHoverModeManager.IS_HOVERMODE_ENABLED) {
            Slog.i(TAG, "start MiuiHoverModeService");
            startService("com.android.server.wm.MiuiHoverModeService$Lifecycle");
        }
        if (MiuiSettings.ScreenEffect.SCREEN_EFFECT_SUPPORTED != 0) {
            startService("com.android.server.display.DisplayFeatureManagerService");
        }
        if (SystemProperties.getBoolean("ro.vendor.display.uiservice.enable", false)) {
            startService("com.android.server.ui.UIService$Lifecycle");
        }
        MiuiFgThread.initialMiuiFgThread();
        if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
            startService("com.miui.server.enterprise.EnterpriseManagerService$Lifecycle");
        }
        if (EnterpriseManagerStub.ENTERPRISE_ACTIVATED) {
            EnterpriseManagerStub.getInstance().startService(context, getClass().getClassLoader());
        }
        startService("com.android.server.am.SmartPowerService$Lifecycle");
        startService("com.miui.server.migard.MiGardService$Lifecycle");
        startService("com.miui.server.blackmask.MiBlackMaskService$Lifecycle");
        startService("com.xiaomi.mirror.service.MirrorService$Lifecycle");
        if (MiuiMagicPointerStub.isSupportMagicPointer()) {
            startService("com.android.server.input.MiuiMagicPointerService$Lifecycle");
        }
        MiuiCommonCloudServiceStub.getInstance().init(context);
        startService("com.miui.server.multisence.MultiSenceService");
        if (TextUtils.equals(SystemProperties.get("ro.miui.build.region", ""), "cn")) {
            startService("com.miui.server.rescue.BrokenScreenRescueService");
        }
        if (ApplicationCompatUtilsStub.get().isAppCompatEnabled()) {
            ApplicationCompatRouterStub.get();
            if (ApplicationCompatUtilsStub.get().isContinuityEnabled()) {
                AppContinuityRouterStub.get();
            }
        }
        startService("com.miui.server.MiuiFreeDragService$Lifecycle");
        startService("com.android.server.powerconsumpiton.PowerConsumptionService");
        startService("com.android.server.aiinput.AIInputTextManagerService$Lifecycle");
        startService("com.miui.cameraopt.CameraOptManagerService$Lifecycle");
        ServiceManager.addService(InterconnectionService.SERVICE_NAME, new InterconnectionService(context));
        startService("com.xiaomi.vkmode.service.MiuiForceVkService$Lifecycle");
        MimdManagerServiceStub.get().systemReady(context);
    }

    void markSystemRun(long time) {
        long now = SystemClock.uptimeMillis();
        BootEventManager.getInstance().setZygotePreload(now - time);
        BootEventManager.getInstance().setSystemRun(time);
        if (MiuiEmbeddingWindowServiceStubHead.isActivityEmbeddingEnable()) {
            MiuiEmbeddingWindowServiceStubHead.get();
        }
    }

    private static void rebootIntoRecovery() {
        BcbUtil.setupBcb("--show_version_mismatch\n");
        SystemProperties.set("sys.powerctl", "reboot,recovery");
    }

    private static boolean isGlobalHaredware() {
        String country = SystemProperties.get("ro.boot.hwc");
        if ("CN".equals(country)) {
            return false;
        }
        if (country != null && country.startsWith("CN_")) {
            return false;
        }
        return true;
    }

    private static void enforceVersionPolicy() {
        String country;
        String product = SystemProperties.get("ro.product.name");
        boolean isIndiaBuild = SystemProperties.get("ro.product.mod_device", "").contains("_in");
        String[] DPC_DLC_WHITE_LIST = {"chenfeng", "chenfeng_"};
        if (!"locked".equals(SystemProperties.get("ro.secureboot.lockstate"))) {
            Slog.d(TAG, "enforceVersionPolicy: device unlocked");
            return;
        }
        for (String p : DPC_DLC_WHITE_LIST) {
            if ((p.equals(product) || (product != null && p.endsWith(d.h) && product.startsWith(p))) && (country = SystemProperties.get("ro.boot.hwc")) != null && (country.startsWith("India") || country.startsWith("IN"))) {
                if (!isIndiaBuild) {
                    Slog.e(TAG, "DPC/DLC devices IN hardware can't run Global build; reboot into recovery!!!");
                    rebootIntoRecovery();
                } else {
                    Slog.d(TAG, "enforceVersionPolicy:no in device");
                    return;
                }
            }
        }
        if (isGlobalHaredware()) {
            Slog.d(TAG, "enforceVersionPolicy: global device");
        } else if (Build.IS_INTERNATIONAL_BUILD) {
            Slog.e(TAG, "CN hardware can't run Global build; reboot into recovery!!!");
            rebootIntoRecovery();
        }
    }

    void markPmsScan(long startTime, long endTime) {
        BootEventManager.getInstance().setPmsScanStart(startTime);
        BootEventManager.getInstance().setPmsScanEnd(endTime);
    }

    void markBootDexopt(long startTime, long endTime) {
        BootEventManager.getInstance().setBootDexopt(endTime - startTime);
    }

    public String getMiuilibpath() {
        return ":/system_ext/framework/miui-wifi-service.jar";
    }

    void addMiuiRestoreManagerService(Context context, Installer installer) {
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            Class<?> cls = Class.forName("com.android.server.MiuiRestoreManagerService$Lifecycle", true, classLoader);
            String name = cls.getName();
            Slog.i(TAG, "Starting " + name);
            Trace.traceBegin(524288L, "StartService " + name);
            if (!SystemService.class.isAssignableFrom(cls)) {
                throw new RuntimeException("Failed to create " + name + ": service must extend " + SystemService.class.getName());
            }
            try {
                SystemService service = (SystemService) cls.getConstructor(Context.class, Installer.class).newInstance(context, installer);
                SystemServiceManager manager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
                try {
                    manager.startService(service);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to start " + service, e);
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex);
            } catch (InstantiationException ex2) {
                throw new RuntimeException("Failed to create service " + name + ": service could not be instantiated", ex2);
            } catch (NoSuchMethodException ex3) {
                throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex3);
            } catch (InvocationTargetException ex4) {
                throw new RuntimeException("Failed to create service " + name + ": service constructor threw an exception", ex4);
            }
        } catch (ClassNotFoundException ex5) {
            throw new RuntimeException("Failed to create service com.android.server.MiuiRestoreManagerService$Lifecycle from class loader " + classLoader.toString() + ": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it. Also ensure that the correct path for the classloader is supplied, if applicable.", ex5);
        }
    }

    public String getConnectivitylibpath() {
        return ":/system_ext/framework/miui-connectivity-service.jar";
    }

    void addCameraCoveredManagerService(Context context) {
        try {
            Slog.d(TAG, "add CameraCoveredManagerService");
            startService("com.android.server.cameracovered.MiuiCameraCoveredManagerService$Lifecycle");
        } catch (Throwable e) {
            Slog.d(TAG, "add CameraCoveredManagerService fail " + e);
        }
    }

    void addMiuiPeriodicCleanerService(ActivityTaskManagerService atm) {
        if (SystemProperties.getBoolean("persist.sys.periodic.u.enable", false)) {
            try {
                Slog.d(TAG, "start PeriodicCleanerService");
                startService("com.android.server.am.PeriodicCleanerService");
                atm.setPeriodicCleaner((PeriodicCleanerInternalStub) LocalServices.getService(PeriodicCleanerInternalStub.class));
            } catch (Throwable e) {
                Slog.d(TAG, "add MiuiPeriodicCleanerService fail " + e);
            }
        }
    }

    public File getHeapDumpDir() {
        File fdtrackDir = new File(MIUI_HEAP_DUMP_PATH);
        if (!fdtrackDir.exists() && !fdtrackDir.mkdirs()) {
            return DEFAULT_HEAP_DUMP_FILE;
        }
        if (FileUtils.setPermissions(fdtrackDir, 493, Process.myUid(), Process.myUid()) != 0) {
            return DEFAULT_HEAP_DUMP_FILE;
        }
        return fdtrackDir;
    }

    public void keepDumpSize(TreeSet<File> fileTreeSet) {
        if (fileTreeSet.size() >= 2) {
            for (int i = 0; i < 1; i++) {
                fileTreeSet.pollLast();
            }
            Iterator<File> it = fileTreeSet.iterator();
            while (it.hasNext()) {
                File file = it.next();
                if (!file.delete()) {
                    Slog.w("System", "Failed to clean up fdtrack " + file);
                }
            }
        }
    }

    public void startAmlMiuiWifiService(TimingsTraceAndSlog t, Context context) {
        if (context.getSystemService(MIUI_WIFI_SERVICE) == null) {
            SystemServiceManager manager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            t.traceBegin(AML_MIUI_WIFI_SERVICE);
            manager.startServiceFromJar(AML_MIUI_WIFI_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
            t.traceEnd();
        }
    }

    public void startAmlSlaveWifiService(TimingsTraceAndSlog t, Context context) {
        if (context.getSystemService(MIUI_SLAVE_WIFI_SERVICE) == null) {
            SystemServiceManager manager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            t.traceBegin(AML_SLAVE_WIFI_SERVICE);
            manager.startServiceFromJar(AML_SLAVE_WIFI_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
            t.traceEnd();
        }
    }
}
